package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.RelayItem;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Support functions for server relay.
 * Author: Michael Barall 05/30/2019.
 */
public class RelaySupport extends ServerComponent {

	//----- Constants -----


	// Relay item type codes.
	// Note: These are intended only for internal use and should not be put in the database or any file.

	public static final int RITYPE_UNKNOWN = 0;					// Unknown relay item type
	public static final int RITYPE_SERVER_STATUS = 1;			// Server status relay item
	public static final int RITYPE_PDL_COMPLETION = 2;			// PDL completion relay item
	public static final int RITYPE_PDL_REMOVAL = 3;				// PDL removal relay item
	public static final int RITYPE_PDL_FOREIGN = 4;				// PDL foreign relay item
	public static final int RITYPE_ANALYST_SELECTION = 5;		// Analyst selection relay item

	// Return a string describing a relay item type.

	public String get_ritype_as_string (int x) {
		switch (x) {
		case RITYPE_UNKNOWN: return "RITYPE_UNKNOWN";
		case RITYPE_SERVER_STATUS: return "RITYPE_SERVER_STATUS";
		case RITYPE_PDL_COMPLETION: return "RITYPE_PDL_COMPLETION";
		case RITYPE_PDL_REMOVAL: return "RITYPE_PDL_REMOVAL";
		case RITYPE_PDL_FOREIGN: return "RITYPE_PDL_FOREIGN";
		case RITYPE_ANALYST_SELECTION: return "RITYPE_ANALYST_SELECTION";
		}
		return "RITYPE_INVALID(" + x + ")";
	}




	//----- Relay item namespaces -----


	// The name used for server status relay items.

	public static final String RI_STATUS_ID = "===status===";

	// The prefix used for server status relay items.

	// public static final String RI_STATUS_PREFIX = "=";

	// The prefix used for PDL completion relay items.

	public static final String RI_PDL_PREFIX = "%";

	// The prefix used for PDL removal relay items.

	public static final String RI_PREM_PREFIX = "~";

	// The prefix used for PDL foreign relay items.

	public static final String RI_PFRN_PREFIX = "$";

	// The prefix used for analyst selection relay items.

	public static final String RI_ANALYST_PREFIX = "@";


	// Classify a relay item id

	public static int classify_relay_id (String relay_id) {
		//  if (relay_id.startsWith (RI_STATUS_PREFIX)) {
		//  	return RITYPE_SERVER_STATUS;
		//  }
		if (relay_id.startsWith (RI_PDL_PREFIX)) {
			return RITYPE_PDL_COMPLETION;
		}
		if (relay_id.startsWith (RI_PREM_PREFIX)) {
			return RITYPE_PDL_REMOVAL;
		}
		if (relay_id.startsWith (RI_PFRN_PREFIX)) {
			return RITYPE_PDL_FOREIGN;
		}
		if (relay_id.startsWith (RI_ANALYST_PREFIX)) {
			return RITYPE_ANALYST_SELECTION;
		}
		if (relay_id.equals (RI_STATUS_ID)) {
			return RITYPE_SERVER_STATUS;
		}
		return RITYPE_UNKNOWN;
	}




	//  // Convert a machine name to a server status relay id.
	//  
	//  public static String machine_name_to_status_relay_id (String machine_name) {
	//  	return RI_STATUS_PREFIX + machine_name;
	//  }
	//  
	//  // Convert a server status relay id to a machine name.
	//  
	//  public static String status_relay_id_to_machine_name (String relay_id) {
	//  	if (!( relay_id.startsWith (RI_STATUS_PREFIX) )) {
	//  		throw new IllegalArgumentException("RelaySupport.status_relay_id_to_machine_name: Invalid relay ID supplied: " + relay_id);
	//  	}
	//  
	//  	return relay_id.substring (RI_STATUS_PREFIX.length());
	//  }




	// Convert an event id to a pdl completion relay id.

	public static String event_id_to_pdl_relay_id (String event_id) {
		return RI_PDL_PREFIX + event_id;
	}

	// Convert an array of event ids to an array of pdl completion relay ids.

	public static String[] event_ids_to_pdl_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = RI_PDL_PREFIX + event_ids[i];
		}
		return result;
	}

	// Convert a pdl completion relay id to an event id.

	public static String pdl_relay_id_to_event_id (String relay_id) {
		if (!( relay_id.startsWith (RI_PDL_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.pdl_relay_id_to_event_id: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_PDL_PREFIX.length());
	}




	// Convert an event id to a pdl removal relay id.

	public static String event_id_to_prem_relay_id (String event_id) {
		return RI_PREM_PREFIX + event_id;
	}

	// Convert an array of event ids to an array of pdl removal relay ids.

	public static String[] event_ids_to_prem_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = RI_PREM_PREFIX + event_ids[i];
		}
		return result;
	}

	// Convert a pdl removal relay id to an event id.

	public static String prem_relay_id_to_event_id (String relay_id) {
		if (!( relay_id.startsWith (RI_PREM_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.prem_relay_id_to_event_id: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_PREM_PREFIX.length());
	}




	// Convert an event id to a pdl foreign relay id.

	public static String event_id_to_pfrn_relay_id (String event_id) {
		return RI_PFRN_PREFIX + event_id;
	}

	// Convert an array of event ids to an array of pdl foreign relay ids.

	public static String[] event_ids_to_pfrn_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = RI_PFRN_PREFIX + event_ids[i];
		}
		return result;
	}

	// Convert a pdl foreign relay id to an event id.

	public static String pfrn_relay_id_to_event_id (String relay_id) {
		if (!( relay_id.startsWith (RI_PFRN_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.pfrn_relay_id_to_event_id: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_PFRN_PREFIX.length());
	}




	// Convert an array of event ids to an array of pdl completion and removal relay ids.

	public static String[] event_ids_to_pdl_and_prem_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[2*len];
		for (int i = 0; i < len; ++i) {
			result[2*i] = RI_PDL_PREFIX + event_ids[i];
			result[2*i + 1] = RI_PREM_PREFIX + event_ids[i];
		}
		return result;
	}




	// Convert an array of event ids to an array of pdl completion, removal, and foreign relay ids.

	public static String[] event_ids_to_pdl_prem_pfrn_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[3*len];
		for (int i = 0; i < len; ++i) {
			result[3*i] = RI_PDL_PREFIX + event_ids[i];
			result[3*i + 1] = RI_PREM_PREFIX + event_ids[i];
			result[3*i + 2] = RI_PFRN_PREFIX + event_ids[i];
		}
		return result;
	}




	// Convert an event id to an analyst selection relay id.

	public static String event_id_to_analyst_relay_id (String event_id) {
		return RI_ANALYST_PREFIX + event_id;
	}

	// Convert an array of event ids to an array of analyst selection relay ids.

	public static String[] event_ids_to_analyst_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = RI_ANALYST_PREFIX + event_ids[i];
		}
		return result;
	}

	// Convert an analyst selection relay id to an event id.

	public static String analyst_relay_id_to_event_id (String relay_id) {
		if (!( relay_id.startsWith (RI_ANALYST_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.analyst_relay_id_to_event_id: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_ANALYST_PREFIX.length());
	}




	//----- PDL completion relay item support -----




	// Submit a PDL completion relay item.
	// Parmeters:
	//  event_id = Event id for the pdl operation (this function handles the conversion).
	//  relay_time = Time of this PDL action.
	//  f_force = True to force this item to be written, increasing the relay time if needed.
	//  ripdl_action = Action code, see RIPDL_ACT_XXXXX in class RiPDLCompletion.
	//  ripdl_forecast_stamp = Forecast stamp, which identifies the forecast.
	//  ripdl_update_time = Update time associated with the OAF product.
	// Returns the new RelayItem if it was added to the database.
	// Returns null if not added to the database (because a newer one alreay exists).
	// Remark: The appropriate event id to use is the event id to which the OAF product is
	// associated, that is, the eventsource + eventsourcecode from the PDL send operation.

	public RelayItem submit_pdl_relay_item (String event_id, long relay_time, boolean f_force,
				int ripdl_action, ForecastStamp ripdl_forecast_stamp, long ripdl_update_time) {
		
		RiPDLCompletion ripdl_payload = new RiPDLCompletion();
		ripdl_payload.setup (ripdl_action, ripdl_forecast_stamp, ripdl_update_time);

		RelayItem result = RelayItem.submit_relay_item (
			event_id_to_pdl_relay_id (event_id),			// relay_id
			relay_time,										// relay_time
			ripdl_payload.marshal_relay(),					// details
			f_force,										// f_force
			0L);											// relay_stamp

		int log_op = ((result == null) ? LogSupport.RIOP_STALE : LogSupport.RIOP_SAVE);
		long log_relay_time = ((result == null) ? relay_time : (result.get_relay_time()));
		sg.log_sup.report_pdl_relay_set (log_op, event_id, log_relay_time, ripdl_payload);
	
		return result;
	}



	// Get PDL completion relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the list of matching relay items, sorted newest relay item first.
	// Returns an empty list if no relay item is found, or if no event ids are supplied.

	public List<RelayItem> get_pdl_relay_items (String... event_ids) {
		if (event_ids == null || event_ids.length == 0) {
			return new ArrayList<RelayItem>();
		}
		String[] relay_ids = event_ids_to_pdl_relay_ids (event_ids);
		List<RelayItem> result = RelayItem.get_relay_item_range (RelayItem.DESCENDING, 0L, 0L, relay_ids);
		return result;
	}




	//----- PDL removal relay item support -----




	// Submit a PDL removal relay item.
	// Parmeters:
	//  event_id = Event id for the pdl operation (this function handles the conversion).
	//  relay_time = Time of this PDL action.
	//  f_force = True to force this item to be written, increasing the relay time if needed.
	//  riprem_reason = Reason code, see RIPREM_REAS_XXXXX in class RiPDLRemoval.
	//  riprem_forecast_stamp = Forecast stamp, which identifies the forecast.
	//  riprem_remove_time = Time when it was determined that the OAF product can be deleted.
	// Returns the new RelayItem if it was added to the database.
	// Returns null if not added to the database (because a newer one alreay exists).
	// Remark: The appropriate event id to use is the event id to which the OAF product is
	// associated, that is, the eventsource + eventsourcecode.

	public RelayItem submit_prem_relay_item (String event_id, long relay_time, boolean f_force,
				int riprem_reason, ForecastStamp riprem_forecast_stamp, long riprem_remove_time) {
		
		RiPDLRemoval riprem_payload = new RiPDLRemoval();
		riprem_payload.setup (riprem_reason, riprem_forecast_stamp, riprem_remove_time);

		RelayItem result = RelayItem.submit_relay_item (
			event_id_to_prem_relay_id (event_id),			// relay_id
			relay_time,										// relay_time
			riprem_payload.marshal_relay(),					// details
			f_force,										// f_force
			0L);											// relay_stamp

		int log_op = ((result == null) ? LogSupport.RIOP_STALE : LogSupport.RIOP_SAVE);
		long log_relay_time = ((result == null) ? relay_time : (result.get_relay_time()));
		sg.log_sup.report_prem_relay_set (log_op, event_id, log_relay_time, riprem_payload);
	
		return result;
	}



	// Get PDL removal relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the list of matching relay items, sorted newest relay item first.
	// Returns an empty list if no relay item is found, or if no event ids are supplied.

	public List<RelayItem> get_prem_relay_items (String... event_ids) {
		if (event_ids == null || event_ids.length == 0) {
			return new ArrayList<RelayItem>();
		}
		String[] relay_ids = event_ids_to_prem_relay_ids (event_ids);
		List<RelayItem> result = RelayItem.get_relay_item_range (RelayItem.DESCENDING, 0L, 0L, relay_ids);
		return result;
	}




	//----- PDL foreign relay item support -----




	// Submit a PDL foreign relay item.
	// Parmeters:
	//  event_id = Event id for the pdl operation (this function handles the conversion).
	//  relay_time = Time of this PDL action.
	//  f_force = True to force this item to be written, increasing the relay time if needed.
	//  ripfrn_status = Status code, see RIPFRN_STAT_XXXXX in class RiPDLForeign.
	//  ripfrn_detect_time = Time when the OAF product from another source was detected.
	// Returns the new RelayItem if it was added to the database.
	// Returns null if not added to the database (because a newer one alreay exists).
	// Remark: The appropriate event id to use is the event id to which the OAF product is
	// associated, that is, the eventsource + eventsourcecode.

	public RelayItem submit_pfrn_relay_item (String event_id, long relay_time, boolean f_force,
				int ripfrn_status, long ripfrn_detect_time) {
		
		RiPDLForeign ripfrn_payload = new RiPDLForeign();
		ripfrn_payload.setup (ripfrn_status, ripfrn_detect_time);

		RelayItem result = RelayItem.submit_relay_item (
			event_id_to_pfrn_relay_id (event_id),			// relay_id
			relay_time,										// relay_time
			ripfrn_payload.marshal_relay(),					// details
			f_force,										// f_force
			0L);											// relay_stamp

		int log_op = ((result == null) ? LogSupport.RIOP_STALE : LogSupport.RIOP_SAVE);
		long log_relay_time = ((result == null) ? relay_time : (result.get_relay_time()));
		sg.log_sup.report_pfrn_relay_set (log_op, event_id, log_relay_time, ripfrn_payload);
	
		return result;
	}



	// Get PDL foreign relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the list of matching relay items, sorted newest relay item first.
	// Returns an empty list if no relay item is found, or if no event ids are supplied.

	public List<RelayItem> get_pfrn_relay_items (String... event_ids) {
		if (event_ids == null || event_ids.length == 0) {
			return new ArrayList<RelayItem>();
		}
		String[] relay_ids = event_ids_to_pfrn_relay_ids (event_ids);
		List<RelayItem> result = RelayItem.get_relay_item_range (RelayItem.DESCENDING, 0L, 0L, relay_ids);
		return result;
	}




	//----- PDL combined relay item support -----




	// Get PDL completion and removal relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the list of matching relay items, sorted newest relay item first.
	// Returns an empty list if no relay item is found, or if no event ids are supplied.

	public List<RelayItem> get_pdl_and_prem_relay_items (String... event_ids) {
		if (event_ids == null || event_ids.length == 0) {
			return new ArrayList<RelayItem>();
		}
		String[] relay_ids = event_ids_to_pdl_and_prem_relay_ids (event_ids);
		List<RelayItem> result = RelayItem.get_relay_item_range (RelayItem.DESCENDING, 0L, 0L, relay_ids);
		return result;
	}



	// Get PDL completion, removal, and foreign relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the list of matching relay items, sorted newest relay item first.
	// Returns an empty list if no relay item is found, or if no event ids are supplied.

	public List<RelayItem> get_pdl_prem_pfrn_relay_items (String... event_ids) {
		if (event_ids == null || event_ids.length == 0) {
			return new ArrayList<RelayItem>();
		}
		String[] relay_ids = event_ids_to_pdl_prem_pfrn_relay_ids (event_ids);
		List<RelayItem> result = RelayItem.get_relay_item_range (RelayItem.DESCENDING, 0L, 0L, relay_ids);
		return result;
	}



	// Get the latest forecast lag from any PDL completion relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the largest forecast_lag from any PDL completion relay item
	// for the given event_ids.  Returns -1L if no relay item is found, or if no event ids
	// are supplied, or if no relay item contains a forecast_lag.

	public long get_pdl_forecast_lag (String... event_ids) {

		// Get PDL completion relay items

		List<RelayItem> relits = get_pdl_relay_items (event_ids);

		// Accumulate the most recent forecast lag

		long forecast_lag = -1L;

		for (RelayItem relit : relits) {

			// Unmarshal the payload and accumulate

			switch (RelaySupport.classify_relay_id (relit.get_relay_id())) {

			case RelaySupport.RITYPE_PDL_COMPLETION:
			{
				RiPDLCompletion payload = new RiPDLCompletion();
				payload.unmarshal_relay (relit);

				forecast_lag = Math.max (forecast_lag, payload.ripdl_forecast_stamp.get_forecast_lag());

			}
			break;

			}
		}

		// Return the forecast lag

		return forecast_lag;
	}



	// Get the latest forecast lag from any PDL completion and removal relay items.
	// Parameters:
	//  event_ids = Event ids to search for.
	// Returns the largest forecast_lag from any PDL completion or removal relay item
	// for the given event_ids.  Returns -1L if no relay item is found, or if no event ids
	// are supplied, or if no relay item contains a forecast_lag.

	public long get_pdl_prem_forecast_lag (String... event_ids) {

		// Get PDL completion and removal relay items

		List<RelayItem> relits = get_pdl_and_prem_relay_items (event_ids);

		// Accumulate the most recent forecast lag

		long forecast_lag = -1L;

		for (RelayItem relit : relits) {

			// Unmarshal the payload and accumulate

			switch (RelaySupport.classify_relay_id (relit.get_relay_id())) {

			case RelaySupport.RITYPE_PDL_COMPLETION:
			{
				RiPDLCompletion payload = new RiPDLCompletion();
				payload.unmarshal_relay (relit);

				forecast_lag = Math.max (forecast_lag, payload.ripdl_forecast_stamp.get_forecast_lag());

			}
			break;

			case RelaySupport.RITYPE_PDL_REMOVAL:
			{
				RiPDLRemoval payload = new RiPDLRemoval();
				payload.unmarshal_relay (relit);

				forecast_lag = Math.max (forecast_lag, payload.riprem_forecast_stamp.get_forecast_lag());

			}
			break;

			}
		}

		// Return the forecast lag

		return forecast_lag;
	}



	// Test if any PDL completion or removal relay item confirms that a forecast was issued.
	// Parameters:
	//  forecast_stamp = Forecast stamp which identifies the proposed forecast, must have lag >= 0.
	//  event_ids = Event ids to search for.
	// Examines the PDL completion and removal relay items for the given event_ids,
	// and returns true if any of the relay items contains a forecast stamp which
	// is a confirmation of the given forecast_stamp.
	// Returns false if no relay item is found, or if no event_ids are supplied, or
	// if no relay item contains a forecast stamp with lag >= 0.
	// Note: Roughly speaking, this function returns true if any relay item mentions
	// a forecast which is the same as, or later than, forecast_stamp.

	public boolean is_pdl_prem_confirmation_of (ForecastStamp forecast_stamp, String... event_ids) {

		// Get PDL completion and removal relay items

		List<RelayItem> relits = get_pdl_and_prem_relay_items (event_ids);

		// Scan for confirming item

		for (RelayItem relit : relits) {

			// Unmarshal the payload and check if it confirms the forecast

			switch (RelaySupport.classify_relay_id (relit.get_relay_id())) {

			case RelaySupport.RITYPE_PDL_COMPLETION:
			{
				RiPDLCompletion payload = new RiPDLCompletion();
				payload.unmarshal_relay (relit);

				if (payload.ripdl_forecast_stamp.is_confirmation_of (forecast_stamp)) {
					return true;
				}
			}
			break;

			case RelaySupport.RITYPE_PDL_REMOVAL:
			{
				RiPDLRemoval payload = new RiPDLRemoval();
				payload.unmarshal_relay (relit);

				if (payload.riprem_forecast_stamp.is_confirmation_of (forecast_stamp)) {
					return true;
				}
			}
			break;

			}
		}

		// Return forecast not confirmed

		return false;
	}




	//----- Server status relay item support -----




	// Submit a server status relay item.
	// Parmeters:
	//  relay_time = Time of this server status.
	//  f_force = True to force this item to be written, increasing the relay time if needed.
	//  f_log = True to write log entry.
	//  sstat_payload = Server status payload.
	// Returns the new RelayItem if it was added to the database.
	// Returns null if not added to the database (because a newer one alreay exists).

	public RelayItem submit_sstat_relay_item (long relay_time, boolean f_force, boolean f_log,
				RiServerStatus sstat_payload) {

		RelayItem result = RelayItem.submit_relay_item (
			RelaySupport.RI_STATUS_ID,						// relay_id
			relay_time,										// relay_time
			sstat_payload.marshal_relay(),					// details
			f_force,										// f_force
			0L);											// relay_stamp

		if (f_log) {
			int log_op = ((result == null) ? LogSupport.RIOP_STALE : LogSupport.RIOP_SAVE);
			long log_relay_time = ((result == null) ? relay_time : (result.get_relay_time()));
			sg.log_sup.report_sstat_relay_set (log_op, log_relay_time, sstat_payload);
		}
	
		return result;
	}



	// Get a server status relay item.
	// Parameters:
	//  sstat_payload = Receives the server status payload, can be null if not needed.
	// Returns the server status relay item, or null if none is found.
	// An exception is thrown if the payload is corrupted.

	public RelayItem get_sstat_relay_item (RiServerStatus sstat_payload) {

		RelayItem relit = RelayItem.get_first_relay_item (RelayItem.DESCENDING, 0L, 0L, RelaySupport.RI_STATUS_ID);

		if (relit != null && sstat_payload != null) {
			sstat_payload.unmarshal_relay (relit);
		}

		return relit;
	}




	// Submit a server status relay item -- static version.
	// Parmeters:
	//  relay_time = Time of this server status.
	//  f_force = True to force this item to be written, increasing the relay time if needed.
	//  sstat_payload = Server status payload.
	// Returns the new RelayItem if it was added to the database.
	// Returns null if not added to the database (because a newer one alreay exists).

	public static RelayItem static_submit_sstat_relay_item (long relay_time, boolean f_force,
				RiServerStatus sstat_payload) {

		RelayItem result = RelayItem.submit_relay_item (
			RelaySupport.RI_STATUS_ID,						// relay_id
			relay_time,										// relay_time
			sstat_payload.marshal_relay(),					// details
			f_force,										// f_force
			0L);											// relay_stamp
	
		return result;
	}



	// Get a server status relay item -- static version.
	// Parameters:
	//  sstat_payload = Receives the server status payload, can be null if not needed.
	// Returns the server status relay item, or null if none is found.
	// An exception is thrown if the payload is corrupted.

	public static RelayItem static_get_sstat_relay_item (RiServerStatus sstat_payload) {

		RelayItem relit = RelayItem.get_first_relay_item (RelayItem.DESCENDING, 0L, 0L, RelaySupport.RI_STATUS_ID);

		if (relit != null && sstat_payload != null) {
			sstat_payload.unmarshal_relay (relit);
		}

		return relit;
	}




	// Get a server status relay item from a remote server.
	// Parameters:
	//  db_handle = Database handle, can be null or empty for default database.
	//  sstat_payload = Receives the server status payload, can be null if not needed.
	// Returns the server status relay item, or null if none is found.
	// Exceptions are caught within this function.

	public static RelayItem get_remote_sstat_relay_item (String db_handle, RiServerStatus sstat_payload) {

		RelayItem relit = null;

		// Connect to MongoDB

		int conopt = MongoDBUtil.CONOPT_CONNECT;

		int ddbopt = MongoDBUtil.DDBOPT_SAVE_SET;

		try (
			MongoDBUtil mongo_instance = new MongoDBUtil (conopt, ddbopt, db_handle);
		){

			// Get the relay item

			relit = static_get_sstat_relay_item (sstat_payload);

		// Abnormal return

		} catch (Exception e) {
			relit = null;
			e.printStackTrace();
		} catch (Throwable e) {
			relit = null;
			e.printStackTrace();
		}

		return relit;
	}




	//----- Analyst selection relay item support -----




	// Submit an analyst selection relay item.
	// Parmeters:
	//  event_id = Event id for the analyst selection (this function handles the conversion).
	//  relay_time = Time of this analyst selection.
	//  relay_stamp = Origin stamp for this relay item, >= 0L means send to partner server.
	//  f_force = True to force this item to be written, increasing the relay time if needed.
	//  state_change = State change, see OpAnalystIntervene.ASREQ_XXXX.
	//  f_create_timeline = True to create timeline if it doesn't exist.
	//  analyst_options = Parameters supplied by the analyst, or null if none.
	// Returns the new RelayItem if it was added to the database.
	// Returns null if not added to the database (because a newer one alreay exists).
	// Remark: The appropriate event id to use is the authoritative Comcat id.

	public RelayItem submit_ansel_relay_item (String event_id, long relay_time, long relay_stamp, boolean f_force,
				int state_change, boolean f_create_timeline, AnalystOptions analyst_options) {
		
		RiAnalystSelection ansel_payload = new RiAnalystSelection();
		ansel_payload.setup (state_change, f_create_timeline, analyst_options);

		RelayItem result = RelayItem.submit_relay_item (
			event_id_to_analyst_relay_id (event_id),		// relay_id
			relay_time,										// relay_time
			ansel_payload.marshal_relay(),					// details
			f_force,										// f_force
			relay_stamp);									// relay_stamp

		if (relay_stamp >= 0L || result != null) {
			int log_op = ((relay_stamp >= 0L) ? ((result == null) ? LogSupport.RIOP_STALE : LogSupport.RIOP_SAVE) : LogSupport.RIOP_COPY);
			long log_relay_time = ((result == null) ? relay_time : (result.get_relay_time()));
			sg.log_sup.report_ansel_relay_set (log_op, event_id, log_relay_time, ansel_payload);
		}
	
		return result;
	}



	// Get analyst selection relay items.
	// Parameters:
	//  event_ids = Event ids to search for (this function handles the conversion).
	// Returns the list of matching relay items, sorted newest relay item first.
	// Returns an empty list if no relay item is found, or if no event ids are supplied.

	public List<RelayItem> get_ansel_relay_items (String... event_ids) {
		if (event_ids == null || event_ids.length == 0) {
			return new ArrayList<RelayItem>();
		}
		String[] relay_ids = event_ids_to_analyst_relay_ids (event_ids);
		List<RelayItem> result = RelayItem.get_relay_item_range (RelayItem.DESCENDING, 0L, 0L, relay_ids);
		return result;
	}



	// Get the most recent analyst selection relay item.
	// Parameters:
	//  event_ids = Event ids to search for (this function handles the conversion).
	// Returns the newest matching relay item.
	// Returns null if no relay item is found, or if no event ids are supplied.
	// Implementation note: It is necessary to retrieve the full list and sort or scan it,
	// so a repeatable selection is made among items with the same relay_time.

	public RelayItem get_ansel_first_relay_item (String... event_ids) {
		List<RelayItem> relits = get_ansel_relay_items (event_ids);
		RelayItem result = null;
		for (RelayItem relit : relits) {
			if (result == null || RelayItem.compare (result, relit) < 0) {
				result = relit;
			}
		}
		return result;
	}

	//  public RelayItem get_ansel_first_relay_item (String... event_ids) {
	//  	List<RelayItem> relits = get_ansel_relay_items (event_ids);
	//  	if (relits.isEmpty()) {
	//  		return null;
	//  	}
	//  	relits.sort (new RelayItem.DescendingComparator());
	//  	return relits.get(0);
	//  }

	//  public RelayItem get_ansel_first_relay_item (String... event_ids) {
	//  	if (event_ids == null || event_ids.length == 0) {
	//  		return null;
	//  	}
	//  	String[] relay_ids = event_ids_to_analyst_relay_ids (event_ids);
	//  	RelayItem result = RelayItem.get_first_relay_item (RelayItem.DESCENDING, 0L, 0L, relay_ids);
	//  	return result;
	//  }




	//----- Construction -----


	// Default constructor.

	public RelaySupport () {

	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		return;
	}

}
