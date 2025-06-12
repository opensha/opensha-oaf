package org.opensha.oaf.aafs;

import java.util.List;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.oaf.rj.CompactEqkRupList;

import org.opensha.oaf.comcat.PropertiesEventSequence;
import org.opensha.oaf.comcat.GeoJsonHolder;
import org.opensha.oaf.comcat.ComcatPDLSendException;

import org.opensha.oaf.pdl.PDLCodeChooserOaf;
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import gov.usgs.earthquake.product.Product;

import org.json.simple.JSONObject;


/**
 * Support functions for PDL reporting.
 * Author: Michael Barall 06/24/2018.
 */
public class PDLSupport extends ServerComponent {




	//----- Task execution subroutines : PDL operations -----




//	// Send a report to PDL.
//	// Throw an exception if the report failed.
//
//	public void send_pdl_report (TimelineStatus tstatus) throws Exception {
//
//		// The JSON file to send
//
//		String jsonText = tstatus.forecast_results.get_pdl_model();
//
//		// The event network and code
//
//		String eventNetwork = tstatus.forecast_mainshock.mainshock_network;
//		String eventCode = tstatus.forecast_mainshock.mainshock_code;
//
//		// The event ID, which for us identifies the timeline
//
//		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);
//
//		// Modification time, 0 means now
//
//		long modifiedTime = 0L;
//
//		// Review status, false means automatically generated
//
//		boolean isReviewed = false;
//
//		// Build the product
//
//		Product product = PDLProductBuilderOaf.createProduct (eventID, eventNetwork, eventCode, isReviewed, jsonText, modifiedTime);
//
//		// Sign the product
//
//		PDLSender.signProduct(product);
//
//		// Send the product, true means it is text
//
//		PDLSender.sendProduct(product, true);
//
//		return;
//	}




	// Send a report to PDL.
	// Return the code used to send to PDL, null if not stored due to conflict with existing forecast.
	// Throw an exception if the report failed.
	// If successful send with no conflict, then set tstatus.pdl_product_code to the product code used. 
	// Use this version if the catalog is in tstatus.forecast_results.

	public String send_pdl_report (TimelineStatus tstatus) throws Exception {

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (tstatus.entry_time, tstatus.forecast_mainshock, tstatus.forecast_params,
							tstatus.forecast_results, tstatus.analyst_options);

		// The event ID, which for us identifies the timeline

		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

		// The suggested product code, either derived from the event ID or saved from the prior Send

		String suggested_code = ((tstatus.has_pdl_product_code()) ? (tstatus.pdl_product_code) : eventID);

		// Review status, false means automatically generated

		boolean isReviewed = false;

		// Build the product

		Product product = forecast_data.make_pdl_product (suggested_code, isReviewed);

		// Stop if conflict

		if (product == null) {
			System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
			return null;
		}

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		// Save the product code that was used in the send

		tstatus.pdl_product_code = ((forecast_data.pdl_event_id.equals (eventID)) ? "" : (forecast_data.pdl_event_id));

		return forecast_data.pdl_event_id;
	}




	// Version that also sends event-sequence products.

	public String send_pdl_report (EventSequenceResult evseq_res, TimelineStatus tstatus) throws Exception {

		evseq_res.clear();

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (tstatus.entry_time, tstatus.forecast_mainshock, tstatus.forecast_params,
							tstatus.forecast_results, tstatus.analyst_options);

		// The event ID, which for us identifies the timeline

		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

		// The suggested product code, either derived from the event ID or saved from the prior Send

		String suggested_code = ((tstatus.has_pdl_product_code()) ? (tstatus.pdl_product_code) : eventID);

		// Review status, false means automatically generated

		boolean isReviewed = false;

		// Build the product

		Product product = forecast_data.make_pdl_product (evseq_res, suggested_code, isReviewed);

		// Stop if conflict

		if (product == null) {
			System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
			return null;
		}

		// Send event-sequence product, if any

		evseq_res.perform_send();

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		// Save the product code that was used in the send

		tstatus.pdl_product_code = ((forecast_data.pdl_event_id.equals (eventID)) ? "" : (forecast_data.pdl_event_id));

		return forecast_data.pdl_event_id;
	}




	// Static version that also sends event-sequence products.
	// Returns the event ID used for PDL.

	public static String static_send_pdl_report (
		boolean isReviewed,
		EventSequenceResult evseq_res,
		ForecastData forecast_data
	) throws Exception {

		evseq_res.clear();

		// Check for availability of event id, network, and code

		ForecastMainshock forecast_mainshock = forecast_data.mainshock;

		if (forecast_mainshock.mainshock_event_id == null || forecast_mainshock.mainshock_event_id.trim().isEmpty()) {
			throw new IllegalArgumentException ("Cannot construct OAF product because the event ID is not available");
		}
		if (forecast_mainshock.mainshock_network == null || forecast_mainshock.mainshock_network.trim().isEmpty()
			|| forecast_mainshock.mainshock_code == null || forecast_mainshock.mainshock_code.trim().isEmpty()) {
			throw new IllegalArgumentException ("Cannot construct OAF product for event " + forecast_mainshock.mainshock_event_id + " because the mainshock network and code are not available");
		}

		// The suggested product code, derived from the event ID

		String suggested_code = forecast_mainshock.mainshock_event_id;

		// Build the product

		Product product = forecast_data.make_pdl_product (evseq_res, suggested_code, isReviewed);

		// Stop if conflict

		if (product == null) {
			throw new RuntimeException ("Cannot construct OAF product for event " + forecast_mainshock.mainshock_event_id + " due to the presence of a conflicting product in PDL");
		}

		// Send event-sequence product, if any

		evseq_res.perform_send();

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		// Save the event ID used for PDL

		return forecast_data.pdl_event_id;
	}




	// Static version that also sends event-sequence products.
	// Returns the event ID used for PDL.

	public static String static_send_pdl_report (
		boolean isReviewed,
		EventSequenceResult evseq_res,
		long creation_time,
		ForecastMainshock forecast_mainshock,
		ForecastParameters forecast_params,
		ForecastResults forecast_results,
		AnalystOptions analyst_options
	) throws Exception {

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (creation_time, forecast_mainshock, forecast_params,
							forecast_results, analyst_options);

		// Finish the send

		return static_send_pdl_report (isReviewed, evseq_res, forecast_data);
	}




	// Send a report to PDL.
	// Return the code used to send to PDL, null if not stored due to conflict with existing forecast.
	// Throw an exception if the report failed.
	// If successful send with no conflict, then set tstatus.pdl_product_code to the product code used. 
	// Use this version to supply the catalog separately.

	public String send_pdl_report (TimelineStatus tstatus, CompactEqkRupList catalog) throws Exception {

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (tstatus.entry_time, tstatus.forecast_mainshock, tstatus.forecast_params,
							tstatus.forecast_results, tstatus.analyst_options, catalog);

		// The event ID, which for us identifies the timeline

		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

		// The suggested product code, either derived from the event ID or saved from the prior Send

		String suggested_code = ((tstatus.has_pdl_product_code()) ? (tstatus.pdl_product_code) : eventID);

		// Review status, false means automatically generated

		boolean isReviewed = false;

		// Build the product

		Product product = forecast_data.make_pdl_product (suggested_code, isReviewed);

		// Stop if conflict

		if (product == null) {
			System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
			return null;
		}

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		// Save the product code that was used in the send

		tstatus.pdl_product_code = ((forecast_data.pdl_event_id.equals (eventID)) ? "" : (forecast_data.pdl_event_id));

		return forecast_data.pdl_event_id;
	}




	// Version that also sends event-sequence products.

	public String send_pdl_report (EventSequenceResult evseq_res, TimelineStatus tstatus, CompactEqkRupList catalog) throws Exception {

		evseq_res.clear();

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (tstatus.entry_time, tstatus.forecast_mainshock, tstatus.forecast_params,
							tstatus.forecast_results, tstatus.analyst_options, catalog);

		// The event ID, which for us identifies the timeline

		String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

		// The suggested product code, either derived from the event ID or saved from the prior Send

		String suggested_code = ((tstatus.has_pdl_product_code()) ? (tstatus.pdl_product_code) : eventID);

		// Review status, false means automatically generated

		boolean isReviewed = false;

		// Build the product

		Product product = forecast_data.make_pdl_product (evseq_res, suggested_code, isReviewed);

		// Stop if conflict

		if (product == null) {
			System.out.println ("ForecastData.make_pdl_product returned null, indicating conflict");
			return null;
		}

		// Send event-sequence product, if any

		evseq_res.perform_send();

		// Sign the product

		PDLSender.signProduct(product);

		// Send the product, true means it is text

		PDLSender.sendProduct(product, true);

		// Save the product code that was used in the send

		tstatus.pdl_product_code = ((forecast_data.pdl_event_id.equals (eventID)) ? "" : (forecast_data.pdl_event_id));

		return forecast_data.pdl_event_id;
	}




	// Static version that also sends event-sequence products.
	// Returns the event ID used for PDL.

	public static String static_send_pdl_report (
		boolean isReviewed,
		EventSequenceResult evseq_res,
		long creation_time,
		ForecastMainshock forecast_mainshock,
		ForecastParameters forecast_params,
		ForecastResults forecast_results,
		AnalystOptions analyst_options,
		CompactEqkRupList catalog
	) throws Exception {

		// Collect the forecast data

		ForecastData forecast_data = new ForecastData();
		forecast_data.set_data (creation_time, forecast_mainshock, forecast_params,
							forecast_results, analyst_options, catalog);

		// Finish the send

		return static_send_pdl_report (isReviewed, evseq_res, forecast_data);
	}




	// Send an event-sequence product for a shadowed event.
	// Parameters:
	//  tstatus = Timeline status.
	//  fcmain = Mainshock info.
	//  forecast_lag = Lag for the forecast being processed.
	//  cap_time = Cap time for the event-sequence product, that is, its end time.
	//   Can also be CAP_TIME_DELETE to request deletion of event-sequence product.
	//  f_new_only = True to send only if there is no existing event-sequence product with a valid code.
	//   True also prevents handing of deletion operations.
	//  gj_used = Returns the GeoJSON used to read back from PDL, or null GeoJSON if no readback was done.
	// Returns false if the requested action was taken, that is, an event-sequence product was sent,
	//  or the deletion operation was done.  Returns true if not.
	// Throws an exception if Comcat or PDL error.
	// Note: If a send occurs, a log entry is written.
	// Note: If no send occurs, then there is no modification of PDL.
	// Note: The return value plus gj_used can be passed to delete_oaf_products to correctly
	//  pass information to a later deletion operation.
	// Note: This should only be called on a primary server.  The caller must check.

	public boolean send_shadowed_evseq (TimelineStatus tstatus, ForecastMainshock fcmain,
			long forecast_lag, long cap_time, boolean f_new_only, GeoJsonHolder gj_used) throws Exception {

		// Default to readback not done

		gj_used.clear();

		//  If event-sequence is enabled, and cap time is requesting delete ...

		if (sg.task_disp.get_action_config().get_is_evseq_enabled()
			&& cap_time == PDLCodeChooserEventSequence.CAP_TIME_DELETE
			&& (!f_new_only)) {

			// The event ID for deletion (cf delete_oaf_products)

			String queryID = fcmain.mainshock_event_id;

			// The GeoJSON, if available

			JSONObject geojson = fcmain.mainshock_geojson;	// it's OK if this is null
			boolean f_gj_prod = true;

			// Do the delete operation

			boolean isReviewed = false;
			boolean f_keep_reviewed = false;
			PDLCodeChooserEventSequence.DeleteEvSeqOp del_op = new PDLCodeChooserEventSequence.DeleteEvSeqOp();

			int doesp = PDLCodeChooserEventSequence.checkDeleteOldEventSequenceProducts (null,
					geojson, f_gj_prod, queryID, isReviewed, cap_time, f_keep_reviewed, del_op, gj_used);

			// Do pending deletes

			if (doesp == PDLCodeChooserEventSequence.DOESP_DELETED) {
				System.out.println ("Deleting shadowed event-sequence product.");
			}

			try {
				del_op.do_delete();
			}
			catch (Exception e) {
				throw new ComcatPDLSendException ("Error deleting shadowed event-sequence product from PDL", e);
			}

			// Log it, if we deleted or capped something

			if (doesp == PDLCodeChooserEventSequence.DOESP_DELETED) {
				sg.log_sup.report_evseq_delete_ok (fcmain.get_pdl_relay_id());
			}
			if (doesp == PDLCodeChooserEventSequence.DOESP_CAPPED) {	// should never happen
				sg.log_sup.report_evseq_cap_ok (fcmain.get_pdl_relay_id(), cap_time);
			}

			// Return that we did the delete operation

			return false;
		}

		// If event-sequence is enabled, and the cap time is a time (not a special value) ...

		if (sg.task_disp.get_action_config().get_is_evseq_enabled()
			&& PDLCodeChooserEventSequence.cap_time_is_time (cap_time)
			&& (cap_time - fcmain.mainshock_time) >= SimpleUtils.HOUR_MILLIS) {	// sanity check

			// The event ID, which for us identifies the timeline

			String eventID = sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id);

			// The suggested product code, either derived from the event ID or saved from the prior Send

			String suggested_code = ((tstatus.has_pdl_product_code()) ? (tstatus.pdl_product_code) : eventID);

			// Review status, false means automatically generated

			boolean isReviewed = false;

			// The event network and code

			String eventNetwork = fcmain.mainshock_network;
			String eventCode = fcmain.mainshock_code;

			// The event query ID

			String queryID = fcmain.mainshock_event_id;

			// The GeoJSON, if available

			JSONObject geojson = fcmain.mainshock_geojson;	// it's OK if this is null
			boolean f_gj_prod = true;

			// Check if we can issue an event-sequence product, and if so get the code to use

			PDLCodeChooserEventSequence.DeleteEvSeqOp del_op = new PDLCodeChooserEventSequence.DeleteEvSeqOp();
			String chosen_code = null;

			if (f_new_only) {
				chosen_code = PDLCodeChooserEventSequence.prepIssueNewEventSequence (null, suggested_code,
					geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed, del_op, gj_used);
			} else {
				boolean f_valid_ok = true;
				long reviewOverwrite = -1L;

				chosen_code = PDLCodeChooserEventSequence.checkChooseEventSequenceCode (suggested_code, f_valid_ok, reviewOverwrite,
					geojson, f_gj_prod, queryID, eventNetwork, eventCode, isReviewed, del_op, gj_used);
			}

			// If we got a code to use and a GeoJSON ...

			if (chosen_code != null && (!(chosen_code.isEmpty())) && gj_used.geojson != null) {

				// The lag we use is the earlier of the supplied forecast lag and the cap time

				long lag = Math.min (forecast_lag, cap_time - fcmain.mainshock_time);

				// Get parameters for the mainshock

				ForecastParameters fcparam = new ForecastParameters();
				fcparam.fetch_all_params (lag, fcmain, tstatus.analyst_options.analyst_params);

				// If we have event-sequence parameters, and we are reporting ...

				if (fcparam.evseq_cfg_avail && fcparam.evseq_cfg_params.get_evseq_cfg_report() == ActionConfigFile.ESREP_REPORT) {

					// Make the event-sequence properties

					PropertiesEventSequence evs_props = new PropertiesEventSequence();
					String evseq_err = ForecastData.make_evseq_properties (evs_props, gj_used.geojson, fcparam, cap_time, false);	// checks for null geojson

					// If error during property build ...

					if (evseq_err != null) {
						System.out.println ("Bypassing shadowed event-sequence product generation: " + evseq_err + ".");
					}

					// Otherwise, we have the event-sequence properties

					else {
						System.out.println ("Created shadowed event-sequence properties.");
						System.out.println (evs_props.toString());

						EventSequenceResult evseq_res = new EventSequenceResult();
						evseq_res.set_for_pending_send (queryID, evs_props, chosen_code, isReviewed);

						try {
							// Do pending deletes

							del_op.do_delete();

							// Do the send

							evseq_res.perform_send();
						}
						catch (Exception e) {
							throw new ComcatPDLSendException ("Error sending shadowed event-sequence product to PDL", e);
						}

						// Write the log entry

						evseq_res.write_log (sg);

						// Return that we did the send

						return false;
					}
				}
			}
		}

		// No send

		return true;
	}




	// Delete the OAF produts for an event.
	// Parameters:
	//  fcmain = Forecast mainshock structure, already filled in.
	//  riprem_reason = Relay item action code, from RiPDLRemoval.RIPREM_REAS_XXXXX.
	//  riprem_forecast_stamp = Forecast stamp for this action, contained forecast lag can be -1L if unknown.
	//  riprem_cap_time = Cap time, or CAP_TIME_XXXXX special value, for deleting event-sequence products.
	//  gj_holder = If non-null, and contains a non-null GeoJSON, it supplies the GeoJSON for reading
	//   back from PDL.  Otherwise, the GeoJSON from fcmain is used.  If omitted, defaults to null.
	//  f_del_evseq = True to also delete event-sequence products, false if not.
	//   If omitted, defaults to true.
	// If a forecast lag is supplied, then the removal time is equal to the mainshock time
	//  plus the forecast lag.  Otherwise, the removel time is the current time.
	// If this is a primary machine, then this function:
	// - Writes a relay item for the given action code.
	// - Deletes or caps all event-sequence products associated with the event, if f_del_evseq is true.
	// - Deletes all OAF products associated with the event.
	// - Writes a log message.
	// If an exception occurs during the deletion of OAF products, this function
	//  absorbs the exception and does not propagate it to hogher levels.
	// If this is a secondary machine, the function does nothing.
	// Note: Exceptions do not trigger any special retry logic.  The relay item is submitted
	//  to the database before attempting the PDL deletion, so following an exception the
	//  cleanup process should eventually finish the deletion.

//	public void delete_oaf_products (ForecastMainshock fcmain, int riprem_reason, ForecastStamp riprem_forecast_stamp) {	// EVSTBD
//
//		// If this is not primary, then do nothing
//
//		if (!( is_pdl_primary() )) {
//			return;
//		}
//
//		// Write the relay item
//
//		String event_id = fcmain.get_pdl_relay_id();
//		long relay_time = sg.task_disp.get_time();
//		boolean f_force = false;
//
//		long riprem_remove_time;
//		if (riprem_forecast_stamp.get_forecast_lag() < 0L) {
//			riprem_remove_time = sg.task_disp.get_time();
//		} else {
//			riprem_remove_time = fcmain.mainshock_time + riprem_forecast_stamp.get_forecast_lag();
//		}
//
//		RelayItem relit = sg.relay_sup.submit_prem_relay_item (event_id, relay_time, f_force, riprem_reason, riprem_forecast_stamp, riprem_remove_time);
//	
//		// Delete the old OAF products
//
//		long delres;
//
//		try {
//			JSONObject geojson = fcmain.mainshock_geojson;	// it's OK if this is null
//			boolean f_gj_prod = true;
//			String queryID = fcmain.mainshock_event_id;
//			boolean isReviewed = false;
//			long cutoff_time = 0L;
//
//			long reviewed_time = 0L;
//			if (riprem_reason == RiPDLRemoval.RIPREM_REAS_SKIPPED_ANALYST) {
//				reviewed_time = 1L;		// for analyst, don't delete reviewed products
//			}
//
//			delres = PDLCodeChooserOaf.deleteOldOafProducts_v2 (null, geojson, f_gj_prod, queryID, isReviewed, cutoff_time, reviewed_time);
//
//		} catch (Exception e) {
//
//			// Run cleanup process after retry interval
//
//			sg.cleanup_sup.set_cleanup_retry();
//
//			// Just log the exception and done
//
//			sg.log_sup.report_pdl_delete_exception (event_id, e);
//			return;
//		}
//
//		// Log successful deletion, if we deleted something
//
//		if (delres == PDLCodeChooserOaf.DOOP_DELETED) {
//			sg.log_sup.report_pdl_delete_ok (event_id);
//		}
//
//		// Done
//
//		return;
//	}


	public void delete_oaf_products (ForecastMainshock fcmain, int riprem_reason, ForecastStamp riprem_forecast_stamp, long riprem_cap_time) {
		
		GeoJsonHolder gj_holder = null;
		boolean f_del_evseq = true;

		delete_oaf_products (fcmain, riprem_reason, riprem_forecast_stamp, riprem_cap_time,
			gj_holder, f_del_evseq);

		return;
	}


	public void delete_oaf_products (ForecastMainshock fcmain, int riprem_reason, ForecastStamp riprem_forecast_stamp, long riprem_cap_time,
			GeoJsonHolder gj_holder, boolean f_del_evseq) {

		// If this is not primary, then do nothing

		if (!( is_pdl_primary() )) {
			return;
		}

		// Write the relay item

		String event_id = fcmain.get_pdl_relay_id();
		long relay_time = sg.task_disp.get_time();
		boolean f_force = false;

		//  long riprem_remove_time;
		//  if (riprem_forecast_stamp.get_forecast_lag() < 0L) {
		//  	riprem_remove_time = sg.task_disp.get_time();
		//  } else {
		//  	riprem_remove_time = fcmain.mainshock_time + riprem_forecast_stamp.get_forecast_lag();
		//  }
		long riprem_remove_time = sg.task_disp.get_time();

		RelayItem relit = sg.relay_sup.submit_prem_relay_item (event_id, relay_time, f_force, riprem_reason, riprem_forecast_stamp, riprem_remove_time, riprem_cap_time);
	
		// Delete the old OAF products

		long delres;

		try {
			JSONObject geojson = fcmain.mainshock_geojson;	// it's OK if this is null
			boolean f_gj_prod = true;

			if (gj_holder != null) {
				if (gj_holder.geojson != null) {
					geojson = gj_holder.geojson;
					f_gj_prod = gj_holder.f_gj_prod;
				}
			}

			String queryID = fcmain.mainshock_event_id;
			boolean isReviewed = false;
			long cutoff_time = 0L;

			long reviewed_time = 0L;
			boolean f_keep_reviewed = false;
			if (riprem_reason == RiPDLRemoval.RIPREM_REAS_SKIPPED_ANALYST) {
				reviewed_time = 1L;		// for analyst, don't delete reviewed products
				f_keep_reviewed = true;	// don't delete reviewed event-sequence products either
			}

			// If event-sequence is enabled, and cap time is not no-operation, delete or cap the event-sequence product

			if (f_del_evseq && sg.task_disp.get_action_config().get_is_evseq_enabled() && riprem_cap_time != PDLCodeChooserEventSequence.CAP_TIME_NOP) {

				GeoJsonHolder gj_used = new GeoJsonHolder (geojson, f_gj_prod);

				int doesp = PDLCodeChooserEventSequence.deleteOldEventSequenceProducts (null,
						geojson, f_gj_prod, queryID, isReviewed, riprem_cap_time, f_keep_reviewed, gj_used);

				geojson = gj_used.geojson;
				f_gj_prod = gj_used.f_gj_prod;

				// Log it, if we deleted or capped something

				if (doesp == PDLCodeChooserEventSequence.DOESP_DELETED) {
					sg.log_sup.report_evseq_delete_ok (event_id);
				}
				if (doesp == PDLCodeChooserEventSequence.DOESP_CAPPED) {
					sg.log_sup.report_evseq_cap_ok (event_id, riprem_cap_time);
				}
			}

			// Now delete the OAF product

			delres = PDLCodeChooserOaf.deleteOldOafProducts_v2 (null, geojson, f_gj_prod, queryID, isReviewed, cutoff_time, reviewed_time);

		} catch (Exception e) {

			// Run cleanup process after retry interval

			sg.cleanup_sup.set_cleanup_retry();

			// Just log the exception and done

			sg.log_sup.report_pdl_delete_exception (event_id, e);
			return;
		}

		// Log successful deletion, if we deleted something

		if (delres == PDLCodeChooserOaf.DOOP_DELETED) {
			sg.log_sup.report_pdl_delete_ok (event_id);
		}

		// Done

		return;
	}




	// Delete the OAF produts for an event, static function.
	// Parameters:
	//  fcmain = Forecast mainshock structure, already filled in.
	//  isReviewed = True if the deletion is reviewed.
	//  cap_time = Cap time, or CAP_TIME_XXXXX special value, for deleting event-sequence products.
	//	  If equal to PDLCodeChooserEventSequence.CAP_TIME_NOP, then no event-sequence products are deleted.
	//    If equal to PDLCodeChooserEventSequence.CAP_TIME_DELETE, then all event-sequence products are deleted.
	//  f_keep_reviewed = True to prevent deletion of reviewed products (oaf and event-sequence).
	//  gj_holder = If non-null, and contains a non-null GeoJSON, it supplies the GeoJSON for reading
	//   back from PDL.  Otherwise, the GeoJSON from fcmain is used.  If omitted, defaults to null.
	// Returns a 3-element array of boolean.
	//   result[0] = True if an oaf product was deleted.
	//   result[1] = True if an event-sequence product was deleted.
	//   result[2] = True if an event-sequence product was capped.
	// Note: At most one of result[1] and result[2] are true;
	// Note: If event-sequence is disabled in ActionConfig.json, then event-sequence is not deleted.

	public static boolean[] static_delete_oaf_products (
		ForecastMainshock fcmain,
		boolean isReviewed,
		long cap_time,
		boolean f_keep_reviewed,
		GeoJsonHolder gj_holder
	) throws Exception {

		// Initialize result to no action

		boolean[] result = new boolean[3];
		result[0] = false;
		result[1] = false;
		result[2] = false;

		// Check for availability of event id

		if (fcmain.mainshock_event_id == null || fcmain.mainshock_event_id.trim().isEmpty()) {
			throw new IllegalArgumentException ("Cannot delete OAF products because the event ID is not available");
		}

		// Delete the old OAF products

		JSONObject geojson = fcmain.mainshock_geojson;	// it's OK if this is null
		boolean f_gj_prod = true;

		if (gj_holder != null) {
			if (gj_holder.geojson != null) {
				geojson = gj_holder.geojson;
				f_gj_prod = gj_holder.f_gj_prod;
			}
		}

		String queryID = fcmain.mainshock_event_id;
		long cutoff_time = 0L;

		long reviewed_time = 0L;
		if (f_keep_reviewed) {
			reviewed_time = 1L;		// don't delete reviewed products
		}

		// If event-sequence is enabled, and cap time is not no-operation, delete or cap the event-sequence product

		if ((new ActionConfig()).get_is_evseq_enabled() && cap_time != PDLCodeChooserEventSequence.CAP_TIME_NOP) {

			GeoJsonHolder gj_used = new GeoJsonHolder (geojson, f_gj_prod);

			int doesp = PDLCodeChooserEventSequence.deleteOldEventSequenceProducts (null,
					geojson, f_gj_prod, queryID, isReviewed, cap_time, f_keep_reviewed, gj_used);

			geojson = gj_used.geojson;
			f_gj_prod = gj_used.f_gj_prod;

			// Indicate in result if we deleted or capped something

			if (doesp == PDLCodeChooserEventSequence.DOESP_DELETED) {
				result[1] = true;
			}
			if (doesp == PDLCodeChooserEventSequence.DOESP_CAPPED) {
				result[2] = true;
			}
		}

		// Now delete the OAF product

		long delres = PDLCodeChooserOaf.deleteOldOafProducts_v2 (null, geojson, f_gj_prod, queryID, isReviewed, cutoff_time, reviewed_time);

		// Indicate in result if we deleted something

		if (delres == PDLCodeChooserOaf.DOOP_DELETED) {
			result[0] = true;
		}

		// Done

		return result;
	}




	//----- Primary/Secondary -----




	// Force primary/secondary mode (used for testing):
	// 0 = normal operation, 1 = force primary mode, 2 = force secondary mode.

	private int force_primary;


	// Set the force primary mode.

	public void set_force_primary (int the_force_primary) {
		force_primary = the_force_primary;
		return;
	}




	// Return true if this machine is primary for sending reports to PDL, false if secondary

	public boolean is_pdl_primary () {

		// Check if mode is forced

		switch (force_primary) {
		case 1:
			return true;
		case 2:
			return false;
		}

		// Get the primary mode from the relay link

		return sg.relay_link.is_primary_state();

		//  // For now, just assume primary
		//  
		//  return true;
	}




	//----- Construction -----


	// Default constructor.

	public PDLSupport () {
	
		force_primary = 0;
	
	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		force_primary = 0;

		return;
	}

}
