package org.opensha.oaf.aafs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayDeque;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;

import org.opensha.oaf.util.EventNotFoundException;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionWorld;
import org.opensha.oaf.util.ObsEqkRupMaxTimeComparator;

import org.opensha.oaf.pdl.PDLCodeChooserOaf;

import org.opensha.oaf.rj.CompactEqkRupList;

import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;
import org.opensha.oaf.comcat.GeoJsonUtils;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.commons.data.comcat.ComcatVisitor;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

import gov.usgs.earthquake.event.JsonEvent;

/**
 * Support functions for cleaning up old forecasts in PDL.
 * Author: Michael Barall 07/03/2019.
 */
public class CleanupSupport extends ServerComponent {




	//----- State variables -----

	// True if cleanup is currently enabled, false if not.

	private boolean f_cleanup_enabled;

	// The wake time at which the next query is to be done, in milliseconds since the epoch.

	private long cleanup_query_wake_time;

	// The wake time at which the next event is to be processed, in milliseconds since the epoch.

	private long cleanup_event_wake_time;

	// The queue of event ids requiring cleanup, or null if none.

	private ArrayDeque<String> cleanup_ids;




	//----- Cleanup subroutines -----




	// Set cleanup to the disabled state.
	// Note: This is to be called from the execution function of a cleanup task.

	public void set_cleanup_disabled () {

		f_cleanup_enabled = false;

		return;
	}




	// Set cleanup to the enabled state.
	// Note: This is to be called from the execution function of a cleanup task.
	// Note: If cleanup is already enabled, the cycle is re-initialized.

	public void set_cleanup_enabled () {

		// Get the current time

		long time_now = sg.task_disp.get_time();

		// Get the action configuration

		ActionConfig action_config = sg.task_disp.get_action_config();

		// Set to perform a new query after a short gap

		cleanup_query_wake_time = time_now + action_config.get_removal_event_gap();
		cleanup_event_wake_time = time_now + action_config.get_removal_event_gap();
		cleanup_ids = null;

		// Set enabled flag

		f_cleanup_enabled = true;

		return;
	}




	// Set cleanup retry following a failed operation.
	// Note: This is to be called from the execution function of a cleanup task, or from idle time code.

	public void set_cleanup_retry () {

		// Get the current time

		long time_now = sg.task_disp.get_time();

		// Get the action configuration

		ActionConfig action_config = sg.task_disp.get_action_config();

		// Set to perform a new query after the retry interval

		cleanup_query_wake_time = time_now + action_config.get_removal_retry_period();
		cleanup_event_wake_time = time_now + action_config.get_removal_retry_period();
		cleanup_ids = null;

		return;
	}




	// Delete all cleanup tasks (the cleanup PDL start/stop tasks).
	// The currently active task is deleted, if it is a cleanup task.

	public void delete_all_existing_cleanup_tasks () {
		sg.task_sup.delete_all_tasks_for_event (EVID_CLEANUP, OPCODE_CLEANUP_PDL_START, OPCODE_CLEANUP_PDL_STOP);
		return;
	}




	// Initialize cleanup to the disabled state.
	// Also deletes all cleanup tasks.
	// Note: This is to be called from the task dispatcher, not during execution of a task.
	// This must be called before the first task is executed from the task queue.

	public void init_cleanup_disabled () {
		delete_all_existing_cleanup_tasks ();

		f_cleanup_enabled = false;
		cleanup_query_wake_time = 0L;
		cleanup_event_wake_time = 0L;
		cleanup_ids = null;

		return;
	}




	// Check if cleanup is needed for an event.
	// Parameters:
	//  time_now = Current time, in milliseconds since the epoch.
	//  comcat_ids = Set of Comcat event ids to check.
	// Returns >= 0L if the OAF products of the event should be checked.
	//  In this case, the return value is the appropriate cutoff time, meaning that
	//  deletion from PDL should occur only if the update time is less than the cutoff time.
	//  (A return value of 0L means that deletion should always occur, hoever this is
	//  currently not used.)
	// Returns -1L if no cleanup is needed at this time.
	// Note: An exception should be considered a database error.
	// Note: This may be called from a task execution function, or from an idle time function.

	public long is_cleanup_needed (long time_now, String... comcat_ids) {

		// Must supply at least one id

		if (comcat_ids == null || comcat_ids.length == 0) {
			throw new IllegalArgumentException ("CleanupSupport.is_cleanup_needed - No Comcat ids supplied");
		}

		// Action configuration

		ActionConfig action_config = new ActionConfig();

		// Get all PDL completion, removal, and foreign relay items for any of the Comcat ids

		List<RelayItem> relits = sg.relay_sup.get_pdl_prem_pfrn_relay_items (comcat_ids);

		// Accumulate the most recent completion, foreign, and removal times

		long completion_time = 0L;
		long foreign_time = 0L;
		long removal_time = 0L;

		for (RelayItem relit : relits) {

			// Unmarshal the payload and accumulate

			switch (RelaySupport.classify_relay_id (relit.get_relay_id())) {

			case RelaySupport.RITYPE_PDL_COMPLETION:
			{
				RiPDLCompletion payload = new RiPDLCompletion();
				payload.unmarshal_relay (relit);

				completion_time = Math.max (completion_time, payload.ripdl_update_time);

			}
			break;

			case RelaySupport.RITYPE_PDL_REMOVAL:
			{
				RiPDLRemoval payload = new RiPDLRemoval();
				payload.unmarshal_relay (relit);

				removal_time = Math.max (removal_time, payload.riprem_remove_time);

			}
			break;

			case RelaySupport.RITYPE_PDL_FOREIGN:
			{
				RiPDLForeign payload = new RiPDLForeign();
				payload.unmarshal_relay (relit);

				foreign_time = Math.max (foreign_time, payload.ripfrn_detect_time);

			}
			break;

			}
		}

		// No cleanup if a known PDL forecast is unexpired and not removed
		// Note: A completion_time does not count if it is strictly less than removal_time.
		// This corresponds to the relationship between update time and cutoff time in
		// PDLCodeChooserOaf.deleteOldOafProducts.  This correspondence is required so that,
		// if removal_time is used as the cutoff time, and deleteOldOafProducts finds an OAF
		// product that does not satisfy the cutoff (and hence is not deleted), then the
		// the completion_time gets updated to a value >= removal_time and hence is not
		// blocked on the next call to this function, avoiding repeated checks of the event.

		if (completion_time > 0L
			&& completion_time >= removal_time
			&& completion_time + action_config.get_removal_forecast_age()
				+ action_config.get_removal_update_skew() >= time_now) {

			return -1L;
		}

		// No cleanup if soon after a foreign forecast was seen

		if (foreign_time > 0L
			&& foreign_time >= removal_time
			&& foreign_time + action_config.get_removal_foreign_block() >= time_now) {

			return -1L;
		}

		// Cutoff time based on expiration

		long cutoff_time = Math.max (1L, time_now - action_config.get_removal_forecast_age());

		// Cutoff time based on removal

		if (removal_time > 0L) {
			cutoff_time = Math.max (cutoff_time, removal_time);
		}

		return cutoff_time;
	}




	// Perform cleanup for an event.
	// Parameters:
	//  query_id = ID to use for querying Comcat.
	//  time_now = Current time, in milliseconds since the epoch.
	// Returns 0L (== PDLCodeChooserOaf.DOOP_DELETED) if one or more products were deleted.
	// Returns > 0L if no product was deleted because the most recent product was
	//  updated at or after the cutoff time.  In this case, the return value is the
	//  time that the most recent product (from our source) was updated.
	// Returns < 0L if no product was deleted for some other reason.  In this case,
	//  the return value is one of the PDLCodeChooserOaf.DOOP_XXXXX return codes.
	// Note: Return values are the same as for PDLCodeChooserOaf.deleteOldOafProducts().
	// Note: The return DOOP_BLOCKED means that existing OAF products were not examined because
	//  there is a relay item which indicates no examination is needed at this time.
	// Throws ComcatException if an error occurs while calling Comcat.
	// Throws DBException if an error occurs while accessing the database.
	// Any other exception can be regarded as a PDL error.
	// Note: This may be called from a task execution function, or from an idle time function.

	public long cleanup_event (String query_id, long time_now) throws Exception {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Action configuration

		ActionConfig action_config = new ActionConfig();

		// Get flag indicating if we should read products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();

		// Get the geojson, reading from Comcat if necessary

		JsonEvent my_geojson;

		try {
		
			// Get the accessor

			ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

			// Try to retrieve the event

			ObsEqkRupture rup = accessor.fetchEvent (query_id, false, true);

			// If not found, nothing to do

			if (rup == null) {
				return PDLCodeChooserOaf.DOOP_NOT_FOUND;
			}

			// Get the geojson from the fetch (must allow for the possibility this is null)

			my_geojson = accessor.get_last_geojson();
		}

		catch (Exception e) {
			throw new ComcatException ("Comcat error while doing cleanup for event: " + query_id, e);
		}

		// If no geojson, nothing to do

		if (my_geojson == null) {
			return PDLCodeChooserOaf.DOOP_NO_GEOJSON;
		}

		// Need to have event net, code, and ids

		String event_net = GeoJsonUtils.getNet (my_geojson);
		String event_code = GeoJsonUtils.getCode (my_geojson);
		String event_ids = GeoJsonUtils.getIds (my_geojson);

		if ( event_net == null || event_net.isEmpty() 
			|| event_code == null || event_code.isEmpty()
			|| event_ids == null || event_ids.isEmpty() ) {
			
			return PDLCodeChooserOaf.DOOP_INCOMPLETE;
		}

		// Get the list of IDs for this event, with the authorative id first

		List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

		// Convert it to an array

		String[] comcat_ids = idlist.toArray (new String[0]);

		// Get the cutoff time, check if cleanup is needed

		long cutoff_time;

		try {
			cutoff_time = is_cleanup_needed (time_now, comcat_ids);
		}

		catch (Exception e) {
			throw new DBException ("Database error while doing cleanup for event: " + query_id, e);
		}

		// If no cutoff time, nothing to do

		if (cutoff_time < 0L) {
			return PDLCodeChooserOaf.DOOP_BLOCKED;
		}

		// Attempt to delete old OAF products

		boolean isReviewed = false;

		long update_time = PDLCodeChooserOaf.deleteOldOafProducts (f_use_prod,
			my_geojson, f_use_prod, query_id, isReviewed, cutoff_time);

		// If nothing deleted due to unexpired OAF product (from our source),
		// write a relay item to defer further checks until its expiration time

		if (update_time > 0L) {
			try {
				sg.relay_sup.submit_pdl_relay_item (
					comcat_ids[0],								// event_id
					time_now,									// relay_time
					true,										// f_force
					RiPDLCompletion.RIPDL_ACT_CLEANUP_FIND,		// ripdl_action
					new ForecastStamp(),						// ripdl_forecast_stamp
					update_time									// ripdl_update_time
				);
			}
			catch (Exception e) {
				throw new DBException ("Database error while doing cleanup for event: " + query_id, e);
			}
		}

		// If there was no OAF product from our source, but a foreign OAF product was found,
		// write a relay item to defer further checks for a while

		else if (update_time == PDLCodeChooserOaf.DOOP_FOREIGN) {
			try {
				sg.relay_sup.submit_pfrn_relay_item (
					comcat_ids[0],								// event_id
					time_now,									// relay_time
					true,										// f_force
					RiPDLForeign.RIPFRN_STAT_FOREIGN,			// ripfrn_status
					time_now									// ripfrn_detect_time
				);
			}
			catch (Exception e) {
				throw new DBException ("Database error while doing cleanup for event: " + query_id, e);
			}
		}

		// Return the update time or result code

		return update_time;
	}




	// Find events needing cleanup.
	// Parameters:
	//  coll = Collection, to receive strings giving event ids.
	//  time_now = Current time, in milliseconds since the epoch.
	//  startTime = Start of time interval to search, in milliseconds since the epoch.
	//  endTime = End time of interval to search, in milliseconds since the epoch.
	//  minMag = Minimum magnitude of events to search for.
	// Returns a two-element array of counters:
	//  count[0] = the number of events needing cleanup that were added to the collection.
	//  count[1] = the number of events with OAF products that were checked.
	// Throws DBException if a database error occurs.  Any other exception is likely
	//  a Comcat error.
	// Note: This may be called from a task execution function, or from an idle time function.

	public int[] find_events_needing_cleanup (final Collection<String> coll, final long time_now,
				long startTime, long endTime, double minMag) {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get flag indicating if we should read products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();
		
		// Get the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

		// Counters for number of events added, and number of events checked

		final int[] count = new int[2];
		count[0] = 0;
		count[1] = 0;

		// Visitor for building the list

		ComcatVisitor visitor = new ComcatVisitor() {
			@Override
			public int visit (ObsEqkRupture rup, JsonEvent geojson) {

				// If no geojson, skip

				if (geojson == null) {
					return 0;
				}

				// Need to have event net, code, and ids

				String event_net = GeoJsonUtils.getNet (geojson);
				String event_code = GeoJsonUtils.getCode (geojson);
				String event_ids = GeoJsonUtils.getIds (geojson);

				if ( event_net == null || event_net.isEmpty() 
					|| event_code == null || event_code.isEmpty()
					|| event_ids == null || event_ids.isEmpty() ) {
			
					return 0;
				}

				// Get the list of IDs for this event, with the authorative id first

				List<String> idlist = ComcatOAFAccessor.idsToList (event_ids, event_net + event_code);

				// Convert it to an array

				String[] comcat_ids = idlist.toArray (new String[0]);

				// Count event checked

				count[1] = count[1] + 1;

				// Get the cutoff time, check if cleanup is needed

				long cutoff_time;

				try {
					cutoff_time = is_cleanup_needed (time_now, comcat_ids);
				}

				catch (Exception e) {
					throw new DBException ("Database error while checking if cleanup is needed for event: " + event_net + event_code, e);
				}

				// If no cutoff time, skip

				if (cutoff_time < 0L) {
					return 0;
				}

				// Add to the collection and count it

				if (coll.add (event_net + event_code)) {
					count[0] = count[0] + 1;
				}

				return 0;
			}
		};

		// Call Comcat

		String rup_event_id = null;
		boolean wrapLon = false;
		boolean extendedInfo = true;

		SphRegionWorld region = new SphRegionWorld ();
		double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

		String productType = server_config.get_pdl_oaf_type();

		int visit_result = accessor.visitEventList (visitor, rup_event_id, startTime, endTime,
				minDepth, maxDepth, region, wrapLon, extendedInfo,
				minMag, productType);

		System.out.println ("Count of events with OAF products = " + count[1] + ", events needing cleanup = " + count[0]);

		// Return number of events added and checked

		return count;
	}




	// Run cleanup operation during task idle time.
	// On entry, these task dispatcher context variables must set up:
	//  dispatcher_time, dispatcher_true_time, dispatcher_action_config
	// This should be run during idle time, not during a MongoDB transaction.
	// Returns true if it did work, false if not.

	public boolean run_cleanup_during_idle (boolean f_verbose) {

		//--- Activity check

		// Get the current time

		long time_now = sg.task_disp.get_time();

		// Get the action configuration

		ActionConfig action_config = sg.task_disp.get_action_config();

		// If cannot execute now (not enabled, or not a primary server) 

		if (!( f_cleanup_enabled && sg.pdl_sup.is_pdl_primary() )) {

			// If interrupted in the middle of processing events ...

			if (cleanup_ids != null) {

				// Set to perform a new query after a short gap

				cleanup_query_wake_time = time_now + action_config.get_removal_event_gap();
				cleanup_event_wake_time = time_now + action_config.get_removal_event_gap();
				cleanup_ids = null;
			}
		
			// Return no work done

			return false;
		}

		//--- Query processing

		// If not processing events ...

		if (cleanup_ids == null || cleanup_ids.isEmpty()) {		// isEmpty clause should be unnecessary

			// If not time to wake up and perform a query, just return

			if (time_now < cleanup_query_wake_time) {

				// Didn't do any work

				return false;
			}

			// Time for next query

			cleanup_query_wake_time = Math.max (
								cleanup_query_wake_time + action_config.get_removal_check_period(),
								time_now + (action_config.get_removal_check_period() / 10L)
							);

			// Time to process first event (if any)

			cleanup_event_wake_time = time_now + action_config.get_removal_event_gap();

			// Say hello

			if (f_verbose) {
				System.out.println (LOG_SEPARATOR_LINE);
				System.out.println ("CLEANUP-QUERY-BEGIN: " + SimpleUtils.time_to_string (time_now));
			}
			sg.log_sup.report_cleanup_query_begin ();

			// Get query parameters

			long query_lookback =  action_config.get_removal_lookback_tmax();

			long startTime = time_now - action_config.get_removal_lookback_tmax();
			long endTime = time_now - action_config.get_removal_lookback_tmin();
			double minMag = action_config.get_removal_lookback_mag();

			if (f_verbose) {
				System.out.println ("CLEANUP-QUERY-INFO: startTime = " + SimpleUtils.time_to_string (startTime)
									+  ", endTime = " + SimpleUtils.time_to_string (endTime)
									+  ", minMag = " + String.format("%.2f", minMag));
			}

			// Perform the query

			int[] count;
			ArrayDeque<String> my_cleanup_ids = new ArrayDeque<String>();

			try {
				count = find_events_needing_cleanup (my_cleanup_ids, time_now, startTime, endTime, minMag);
			}

			// Database exceptions are propagated

			catch (DBException e) {

				// Try again after retry interval

				set_cleanup_retry();

				if (f_verbose) {
					System.out.println ("CLEANUP-QUERY-ERR: Database exception during query");
					System.out.println ("CLEANUP-QUERY-END");
				}
				sg.log_sup.report_cleanup_query_end (cleanup_query_wake_time);

				// Propagate exception

				throw new DBException ("Database error while performing cleanup query", e);
			}

			// Any other exception is considered to be a Comcat error

			catch (Exception e) {

				// Try again after retry interval

				set_cleanup_retry();

				if (f_verbose) {
					System.out.println ("CLEANUP-QUERY-ERR: Comcat exception during query");
					System.out.println ("Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));
					System.out.println ("CLEANUP-QUERY-END");
				}
				sg.log_sup.report_comcat_exception (null, e);
				sg.log_sup.report_cleanup_query_end (cleanup_query_wake_time);

				// Did work

				return true;
			}

			// If we got events, save the list

			if (!( my_cleanup_ids.isEmpty() )) {
				cleanup_ids = my_cleanup_ids;
			}
		
			if (f_verbose) {
				System.out.println ("CLEANUP-QUERY-END");
			}
			sg.log_sup.report_cleanup_query_done (query_lookback, count[1], count[0]);
			sg.log_sup.report_cleanup_query_end (cleanup_query_wake_time);

		}

		//--- Event processing

		// Otherwise, we are processing events ...

		else {

			// If not time to wake up and process an event, just return

			if (time_now < cleanup_event_wake_time) {

				// Didn't do any work

				return false;
			}

			// Time to process next event (if any)

			cleanup_event_wake_time = Math.max (
								cleanup_event_wake_time + action_config.get_removal_event_gap(),
								time_now + (action_config.get_removal_event_gap() / 2L)
							);

			// Get the event to process

			String event_id = cleanup_ids.remove();

			if (cleanup_ids.isEmpty()) {
				cleanup_ids = null;
			}

			// Say hello

			if (f_verbose) {
				System.out.println (LOG_SEPARATOR_LINE);
				System.out.println ("CLEANUP-EVENT-BEGIN: " + SimpleUtils.time_to_string (time_now));
				System.out.println ("event_id = " + event_id);
			}
			sg.log_sup.report_cleanup_event_begin (event_id);

			// Clean up the event

			long doop;

			try {
				doop = cleanup_event (event_id, time_now);
			}

			// Database exceptions are propagated

			catch (DBException e) {

				// Try again after retry interval

				set_cleanup_retry();

				if (f_verbose) {
					System.out.println ("CLEANUP-EVENT-ERR: Database exception during event cleanup, event_id = " + event_id);
					System.out.println ("CLEANUP-EVENT-END");
				}
				sg.log_sup.report_cleanup_event_end (PDLCodeChooserOaf.DOOP_DB_EXCEPTION);

				// Propagate exception

				throw new DBException ("Database error while performing event cleanup, event_id = " + event_id, e);
			}

			// Handle a Comcat error

			catch (ComcatException e) {

				// Try again after retry interval

				set_cleanup_retry();

				if (f_verbose) {
					System.out.println ("CLEANUP-EVENT-ERR: Comcat exception during event cleanup, event_id = " + event_id);
					System.out.println ("Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));
					System.out.println ("CLEANUP-EVENT-END");
				}
				sg.log_sup.report_comcat_exception (event_id, e);
				sg.log_sup.report_cleanup_event_end (PDLCodeChooserOaf.DOOP_COMCAT_EXCEPTION);

				// Did work

				return true;
			}

			// Any other exception is considered to be a PDL error

			catch (Exception e) {

				// Try again after retry interval

				set_cleanup_retry();

				if (f_verbose) {
					System.out.println ("CLEANUP-EVENT-ERR: PDL exception during event cleanup, event_id = " + event_id);
					System.out.println ("Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));
					System.out.println ("CLEANUP-EVENT-END");
				}
				sg.log_sup.report_pdl_delete_exception (event_id, e);
				sg.log_sup.report_cleanup_event_end (PDLCodeChooserOaf.DOOP_PDL_EXCEPTION);

				// Did work

				return true;
			}

			// Log successful deletion, if we deleted something

			if (doop == PDLCodeChooserOaf.DOOP_DELETED) {
				if (f_verbose) {
					System.out.println ("CLEANUP-EVENT-INFO: Deleted product from PDL, event_id = " + event_id);
				}
				sg.log_sup.report_pdl_delete_ok (event_id);
			}

			// Finished event cleanup

			if (f_verbose) {
				System.out.println ("CLEANUP-EVENT-INFO: Result = " + PDLCodeChooserOaf.get_doop_as_string (doop));
				System.out.println ("CLEANUP-EVENT-END");
			}
			sg.log_sup.report_cleanup_event_end (doop);

		}

		//--- Done

		// Did work

		return true;
	}




	//----- Construction -----


	// Default constructor.

	public CleanupSupport () {

		f_cleanup_enabled = false;
		cleanup_query_wake_time = 0L;
		cleanup_event_wake_time = 0L;
		cleanup_ids = null;

	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		f_cleanup_enabled = false;
		cleanup_query_wake_time = 0L;
		cleanup_event_wake_time = 0L;
		cleanup_ids = null;

		return;
	}

}
