package org.opensha.oaf.aafs;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

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
import org.opensha.oaf.comcat.ComcatConflictException;
import org.opensha.oaf.comcat.ComcatRemovedException;
import org.opensha.oaf.comcat.ComcatQueryException;
import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Support functions for timelines.
 * Author: Michael Barall 06/23/2018.
 */
public class TimelineSupport extends ServerComponent {




	//----- Task execution subroutines : Timeline operations -----




	// Delete all waiting tasks with the given event id that are delayed timeline actions.
	// Note: The currently active task is not deleted, even if it would match.

	public void delete_delayed_timeline_tasks (String event_id) {
		sg.task_sup.delete_all_waiting_tasks (event_id, OPCODE_GEN_FORECAST, OPCODE_GEN_PDL_REPORT, OPCODE_GEN_EXPIRE);
		return;
	}




	// Get the most recent entry on the timeline, and extract its status structure.
	// If the task is restarted, this routine either completes the task or rolls back
	// the timeline state so the task can start over.
	// Return values are:
	//  RESCODE_TIMELINE_EXISTS = The timeline exists, and its state is in tstatus.
	//                            If restarted, anything that was previously done is undone.
	//  RESCODE_TIMELINE_NOT_FOUND = The timeline does not exist, and tstatus is indeterminate.
	//                               If restarted, anything that was previously done is undone.
	//  RESCODE_TASK_RETRY_SUCCESS = The task was restarted, and has now been completed successfully.
	//  RESCODE_TIMELINE_CORRUPT, RESCODE_TASK_CORRUPT (or anything else) = Error, operation cannot proceed.

	public int open_timeline (PendingTask task, TimelineStatus tstatus, DBPayload payload) {

		TimelineEntry tentry = null;
		CatalogSnapshot catsnap = null;

		// Get the payload

		try {
			payload.unmarshal_task (task);
		}

		// Invalid task

		catch (Exception e) {

			// If this task is a delayed timeline operation ...

			switch (task.get_opcode()) {
			case OPCODE_GEN_FORECAST:
			case OPCODE_GEN_PDL_REPORT:
			case OPCODE_GEN_EXPIRE:

				// Get the most recent timeline entry for this event

				tentry = TimelineEntry.get_recent_timeline_entry (0L, 0L, task.get_event_id(), null, null);

				if (tentry != null) {

					// Get the status for this timeline entry

					try {
						tstatus.unmarshal_timeline (tentry);
					}

					// Invalid timeline entry

					catch (Exception e2) {

						// Display the error and log the task

						display_timeline_corrupt (tentry, task, e2);
						return RESCODE_TIMELINE_CORRUPT;
					}

					// Issue any new delayed command that is needed to replace the failed one

					next_auto_timeline (tstatus);
				}
				break;
			}

			// Display the error and log the task

			sg.task_sup.display_invalid_task (task, e);
			return RESCODE_TASK_CORRUPT;
		}

		// If task is restarting ...

		if (task.is_restarted()) {

			// If a timeline entry was written for this task, get it

			tentry = TimelineEntry.get_timeline_entry_for_key (task.get_record_key());

			// If a catalog snapshot was written for this task, get it

			catsnap = CatalogSnapshot.get_catalog_shapshot_for_key (task.get_record_key());

			// If we got a timeline entry ...

			if (tentry != null) {

				// Get the status for this timeline entry

				try {
					tstatus.unmarshal_timeline (tentry);
				}

				// Invalid timeline entry

				catch (Exception e) {

					// Display the error and log the task

					display_timeline_corrupt (tentry, task, e);
					return RESCODE_TIMELINE_CORRUPT;
				}

				// If we have any required catalog snapshot, then we can complete the task ...

				if ( (!(tstatus.has_catalog_snapshot())) || catsnap != null ) {

					//--- Complete the pending task

					// Update the analyst options from relay items

					update_analyst_options_from_relay (tstatus);
				
					// Remove any delayed commands

					delete_delayed_timeline_tasks (tstatus.event_id);

					// Issue any new delayed command that is needed

					next_auto_timeline (tstatus);

					// Task is now completed

					return RESCODE_TASK_RETRY_SUCCESS;
				}

				// Unwind the task, by deleting the timeline entry

				sg.log_sup.report_timeline_entry_deleted (task.get_event_id());

				TimelineEntry.delete_timeline_entry (tentry);
			}

			//--- Undo the pending task, so it can start over from the beginning

			sg.log_sup.report_timeline_unwind (task.get_event_id());

			// Delete the catalog entry if we have it

			if (catsnap != null) {
				CatalogSnapshot.delete_catalog_snapshot (catsnap);
			}
				
			// Remove any delayed commands

			delete_delayed_timeline_tasks (tstatus.event_id);

			// Get the most recent timeline entry for this event

			tentry = TimelineEntry.get_recent_timeline_entry (0L, 0L, task.get_event_id(), null, null);

			if (tentry == null) {
				return RESCODE_TIMELINE_NOT_FOUND;
			}

			// Get the status for this timeline entry

			try {
				tstatus.unmarshal_timeline (tentry);
			}

			// Invalid timeline entry

			catch (Exception e) {

				// Display the error and log the task

				display_timeline_corrupt (tentry, task, e);
				return RESCODE_TIMELINE_CORRUPT;
			}

			// Update the analyst options from relay items

			update_analyst_options_from_relay (tstatus);

			// If the current task is not a delayed task, re-issue any delayed task for the current timeline entry

			switch (task.get_opcode()) {
			case OPCODE_GEN_FORECAST:
			case OPCODE_GEN_PDL_REPORT:
			case OPCODE_GEN_EXPIRE:
				break;

			default:
				next_auto_timeline (tstatus);
				break;
			}

			// Found timeline entry

			return RESCODE_TIMELINE_EXISTS;
		}

		//--- Prepare for the pending task

		// Get the most recent timeline entry for this event

		tentry = TimelineEntry.get_recent_timeline_entry (0L, 0L, task.get_event_id(), null, null);

		if (tentry == null) {
			return RESCODE_TIMELINE_NOT_FOUND;
		}

		// Get the status for this timeline entry

		try {
			tstatus.unmarshal_timeline (tentry);
		}

		// Invalid timeline entry

		catch (Exception e) {

			// Display the error and log the task

			display_timeline_corrupt (tentry, task, e);
			return RESCODE_TIMELINE_CORRUPT;
		}

		// Update the analyst options from relay items

		update_analyst_options_from_relay (tstatus);

		// Found timeline entry

		return RESCODE_TIMELINE_EXISTS;
	}




	// Display an exception caused by a corrupt timeline entry, and set the log remark.
	// Also, if the timeline entry is not an error entry, then write an error entry.

	public void display_timeline_corrupt (TimelineEntry tentry, PendingTask task, Exception e) {

		// Display messages
		
		sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry is corrupt:\n"
			+ "event_id = " + task.get_event_id() + "\n"
			+ "Timeline entry synopsis:\n" + tentry.toString() + "\n"
			+ "Timeline entry details dump:\n" + tentry.dump_details() + "\n"
			+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

		sg.log_sup.report_corrupt_timeline_exception (tentry, task, e);

		// If the timeline entry is not marked as error, then add a new timeline entry

		if (tentry.get_actcode() != TimelineStatus.ACTCODE_ERROR) {

			// Delete any pending delayed tasks

			delete_delayed_timeline_tasks (tentry.get_event_id());

			// Write an error entry into the timeline

			String[] comcat_ids = new String[1];
			comcat_ids[0] = EVID_ERROR;

			TimelineEntry.submit_timeline_entry (
				task.get_record_key(),										// key
				Math.max(sg.task_disp.get_time(), tentry.get_action_time() + 1L),	// action_time
				tentry.get_event_id(),										// event_id
				comcat_ids,													// comcat_ids
				TimelineStatus.ACTCODE_ERROR,								// actcode
				null);														// details

			sg.log_sup.report_timeline_error_appended (tentry.get_event_id());
		}

		return;
	}




	// Process an exception caused by a ComCat failure.
	// Display a message, set the log remark, set the timeline to ComCat fail state, and write the timeline entry.

	private void process_timeline_comcat_fail (PendingTask task, TimelineStatus tstatus, Exception e) {

		// Display messages
			
		sg.log_sup.report_comcat_exception (task.get_event_id(), e);
		
		sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline stopped due to ComCat failure:\n"
			+ "opcode = " + get_opcode_as_string (task.get_opcode()) + "\n"
			+ "event_id = " + task.get_event_id() + "\n"
			+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

		// Set to ComCat fail state

		tstatus.set_state_comcat_fail (sg.task_disp.get_time());

		// Write timeline entry

		append_timeline (task, tstatus);
		return;
	}




	// Process an exception caused by an event removed from ComCat.
	// Display a message, set the log remark, set the timeline to withdrawn state, and write the timeline entry.

	private void process_timeline_comcat_removed (PendingTask task, TimelineStatus tstatus, Exception e) {

		// Display messages
			
		sg.log_sup.report_comcat_exception (task.get_event_id(), e);
		
		sg.task_disp.set_display_taskres_log ("TASK-INFO: Timeline stopped due to event deleted or merged in ComCat:\n"
			+ "opcode = " + get_opcode_as_string (task.get_opcode()) + "\n"
			+ "event_id = " + task.get_event_id() + "\n"
			+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

		// Withdraw the timeline

		tstatus.set_state_withdrawn (sg.task_disp.get_time(), null);

		// Write timeline entry

		append_timeline (task, tstatus);
		return;
	}




	// Process a retry caused by a ComCat failure.
	// Stage the task to retry the Comcat operation.
	// If retries are exhausted, then fail the operation.
	// Return values:
	//  RESCODE_STAGE_COMCAT_RETRY or RESCODE_STAGE_COMCAT_QUERY_RETRY = The task is being staged for retry.
	//  RESCODE_TIMELINE_COMCAT_FAIL or RESCODE_TIMELINE_COMCAT_QUERY_FAIL = Retries exhausted, the timeline is stopped in Comcat fail state.
	//  RESCODE_TIMELINE_EVENT_REMOVED = Retries exhausted, event deleted or merged in Comcat, timeline is in withdrawn state.

	public int process_timeline_comcat_retry (PendingTask task, TimelineStatus tstatus, Exception e) {

		// Get the next ComCat retry lag

		long last_comcat_retry_lag = sg.task_disp.get_action_config().int_to_lag (task.get_stage());

		long min_lag = last_comcat_retry_lag + 1L;

		if (e instanceof ComcatRemovedException) {
			if (min_lag < sg.task_disp.get_action_config().get_comcat_retry_missing()) {
				min_lag = sg.task_disp.get_action_config().get_comcat_retry_missing();
			}
		}

		long next_comcat_retry_lag = sg.task_disp.get_action_config().get_next_comcat_retry_lag (min_lag);

		// If there is another retry, stage the task

		if (next_comcat_retry_lag >= 0L) {

			sg.log_sup.report_comcat_exception (task.get_event_id(), e);

			//sg.task_disp.set_taskres_stage (task.get_sched_time() + next_comcat_retry_lag,
			//					sg.task_disp.get_action_config().lag_to_int (next_comcat_retry_lag));

			sg.task_disp.set_taskres_stage (
					Math.max (task.get_sched_time(), sg.task_disp.get_time() - last_comcat_retry_lag) + next_comcat_retry_lag,
					sg.task_disp.get_action_config().lag_to_int (next_comcat_retry_lag));

			return (e instanceof ComcatQueryException) ? RESCODE_STAGE_COMCAT_QUERY_RETRY : RESCODE_STAGE_COMCAT_RETRY;
		}

		// Retries exhausted, process the error and log the task

		if (e instanceof ComcatRemovedException) {
			process_timeline_comcat_removed (task, tstatus, e);
			return RESCODE_TIMELINE_EVENT_REMOVED;
		}

		process_timeline_comcat_fail (task, tstatus, e);
		return (e instanceof ComcatQueryException) ? RESCODE_TIMELINE_COMCAT_QUERY_FAIL : RESCODE_TIMELINE_COMCAT_FAIL;
	}




	// Append an entry to the timeline.
	// The entry is tagged with the key from the current task.
	// Also write a catalog snapshot to the database if necessary.
	// Also issue the next delayed command for the timeline, if necessary.
	// Parameters:
	//  task = Current task.
	//  tstatus = Timeline status.
	//  last_pdl_lag = Lag for the last PDL report attempt, or -1L if none.  Defaults to -1L if omitted.
	// Note: Typically last_pdl_lag == 0L if the caller has already attempted a PDL report, -1L otherwise.

	public void append_timeline (PendingTask task, TimelineStatus tstatus) {

		append_timeline (task, tstatus, -1L);
		return;
	}

	public void append_timeline (PendingTask task, TimelineStatus tstatus, long last_pdl_lag) {

		// If the current task is not a delayed task, delete any delayed task for the current timeline entry

		switch (task.get_opcode()) {
		case OPCODE_GEN_FORECAST:
		case OPCODE_GEN_PDL_REPORT:
		case OPCODE_GEN_EXPIRE:
			break;

		default:
			//delete_delayed_timeline_tasks (task.get_event_id());
			delete_delayed_timeline_tasks (tstatus.event_id);
			break;
		}

		// If there is a catalog snapshot available, write it to the database

		CompactEqkRupList catalog_aftershocks = tstatus.get_catalog_snapshot();

		if (catalog_aftershocks != null) {

			// Write catalog snapshot to database

			CatalogSnapshot.submit_catalog_shapshot (
				task.get_record_key(),							// key
				tstatus.event_id,								// event_id
				tstatus.forecast_results.catalog_start_time,	// start_time
				tstatus.forecast_results.catalog_end_time,		// end_time
				catalog_aftershocks);							// rupture_list

			// Display message
		
			sg.task_disp.display_taskinfo ("TASK-INFO: Catalog snapshot saved:\n"
				+ "event_id = " + tstatus.event_id + "\n"
				+ "catalog_eqk_count = " + tstatus.forecast_results.catalog_eqk_count);

			sg.log_sup.report_catalog_saved (tstatus.event_id, tstatus.forecast_results.catalog_eqk_count);

		}

		// Write timeline entry

		TimelineEntry.submit_timeline_entry (
			task.get_record_key(),				// key
			tstatus.action_time,				// action_time
			tstatus.event_id,					// event_id
			tstatus.comcat_ids,					// comcat_ids
			tstatus.actcode,					// actcode
			tstatus.marshal_timeline());		// details

		// Display message
		
		sg.task_disp.display_taskinfo ("TASK-INFO: Timeline appended:\n"
			+ "event_id = " + tstatus.event_id + "\n"
			+ "actcode = " + tstatus.get_actcode_as_string () + "\n"
			+ "action_time = " + tstatus.action_time + "\n"
			+ "fc_status = " + tstatus.get_fc_status_as_string () + "\n"
			+ "pdl_status = " + tstatus.get_pdl_status_as_string () + "\n"
			+ "fc_result = " + tstatus.get_fc_result_as_string ());

		sg.log_sup.report_timeline_appended (tstatus);

		// Issue any new delayed command that is needed

		next_auto_timeline (tstatus, last_pdl_lag);

		return;
	}




	// Determine the next forecast lag for a timeline entry.
	// Parameters:
	//  tstatus = Timeline status.
	// Returns -1L if there is no next forecast lag.
	// If there is a next forecast lag, the return value is
	// positive and a multiple of the lag unit.
	// Note: Forecast lag is relative to the mainshock origin time.
	// Note: This does not consider the possibility of a PDL report.

	public long get_next_forecast_lag (TimelineStatus tstatus) {
		return get_next_forecast_lag (tstatus, tstatus.get_last_forecast_lag());
	}




	// Determine the next forecast lag for a timeline entry.
	// Parameters:
	//  tstatus = Timeline status.
	//  last_forecast_lag = Lag for prior forecast, or -1L if no prior forecast.
	// Returns -1L if there is no next forecast lag greater than last_forecast_lag.
	// If there is a next forecast lag greater than last_forecast_lag, the return value is
	// positive and a multiple of the lag unit.
	// Note: Forecast lag is relative to the mainshock origin time.
	// Note: This does not consider the possibility of a PDL report.

	public long get_next_forecast_lag (TimelineStatus tstatus, long last_forecast_lag) {

		long next_forecast_lag = -1L;

		// If the timeline state requests a forecast ...

		if (tstatus.is_forecast_state()) {

			long min_lag;				// returned scheduled forecast lag must be >= min_lag
			long min_extra_lag;			// returned extra forecast lag must be >= min_extra_lag

			// Minimum lag is 0L if this is the first forecast,
			// otherwise it is a minimum spacing after the last lag;
			// extra lags are not subject to minimum spacing requirement

			if (last_forecast_lag < 0L) {
				min_lag = 0L;
				min_extra_lag = 0L;
			} else {
				min_lag = last_forecast_lag + sg.task_disp.get_action_config().get_forecast_min_gap();
				min_extra_lag = last_forecast_lag + 1L;
			}

			// Get next forecast lag from configured schedule

			next_forecast_lag = sg.task_disp.get_action_config().get_next_forecast_lag (min_lag, tstatus.analyst_options.max_forecast_lag);

			// If no next forecast lag on the configured schedule ...

			if (next_forecast_lag < 0L) {
			
				// Use the requested extra forecast lag, if any

				if (tstatus.analyst_options.extra_forecast_lag >= min_extra_lag) {

					// Make sure the value is a multiple of the lag unit, and greater than the last lag (if any), and positive,
					// and not less than the supplied extra_forecast_lag (the last condition is required to avoid an infinite loop)

					next_forecast_lag = 
						sg.task_disp.get_action_config().ceil_unit_lag (tstatus.analyst_options.extra_forecast_lag, 0L);
				} else {
					next_forecast_lag = -1L;
				}
			}

			// Otherwise, we have a forecast lag from the schedule ...

			else {
			
				// If there is a requested extra forecast lag ...

				if (tstatus.analyst_options.extra_forecast_lag >= min_extra_lag) {

					// Use the smaller of the scheduled and extra lags

					next_forecast_lag = Math.min (next_forecast_lag,
						sg.task_disp.get_action_config().ceil_unit_lag (tstatus.analyst_options.extra_forecast_lag, 0L));
				}
			}
		}

		// Return next lag

		return next_forecast_lag;
	}




//	// Determine the next forecast lag for a timeline entry.
//	// Parameters:
//	//  tstatus = Timeline status.
//	// Returns -1L if there is no next forecast lag.
//	// If there is a next forecast lag, the return value is
//	// positive and a multiple of the lag unit.
//	// Note: Forecast lag is relative to the mainshock origin time.
//	// Note: This does not consider the possibility of a PDL report.
//
//	public long get_next_forecast_lag (TimelineStatus tstatus) {
//
//		long next_forecast_lag = -1L;
//
//		// If the timeline state requests a forecast ...
//
//		if (tstatus.is_forecast_state()) {
//
//			long min_lag;
//			long min_extra_lag;
//
//			// Minimum lag is 0L if this is the first forecast,
//			// otherwise it is a minimum spacing after the last lag
//
//			if (tstatus.get_last_forecast_lag() < 0L) {
//				min_lag = 0L;
//				min_extra_lag = 0L;
//			} else {
//				min_lag = tstatus.get_last_forecast_lag() + sg.task_disp.get_action_config().get_forecast_min_gap();
//				min_extra_lag = tstatus.get_last_forecast_lag();
//			}
//
//			// Get next forecast lag from configured schedule
//
//			next_forecast_lag = sg.task_disp.get_action_config().get_next_forecast_lag (min_lag, tstatus.analyst_options.max_forecast_lag);
//
//			// If no next forecast lag on the configured schedule ...
//
//			if (next_forecast_lag < 0L) {
//			
//				// Use the requested extra forecast lag, if any
//
//				if (tstatus.analyst_options.extra_forecast_lag >= 0L) {
//
//					// Make sure the value is a multiple of the lag unit, and greater than the last lag
//
//					next_forecast_lag = 
//						sg.task_disp.get_action_config().floor_unit_lag (tstatus.analyst_options.extra_forecast_lag, min_extra_lag);
//				} else {
//					next_forecast_lag = -1L;
//				}
//			}
//
//			// Otherwise, we have a forecast lag from the schedule ...
//
//			else {
//			
//				// If there is a requested extra forecast lag ...
//
//				if (tstatus.analyst_options.extra_forecast_lag >= 0L) {
//
//					// Use the smaller of the scheduled and extra lags
//
//					next_forecast_lag = Math.min (next_forecast_lag,
//						sg.task_disp.get_action_config().floor_unit_lag (tstatus.analyst_options.extra_forecast_lag, min_extra_lag));
//				}
//			}
//		}
//
//		// Return next lag
//
//		return next_forecast_lag;
//	}




	// Determine the next PDL report stage for a timeline entry.
	// Parameters:
	//  tstatus = Timeline status.
	//  next_forecast_lag = Scheduled time of the next forecast, or -1L if none.
	//  last_pdl_lag = Lag for the last PDL report attempt, or -1L if none.
	//	base_pdl_time = Start time for the PDL report sequence.
	// Returns -1L if there is no next PDL report lag.
	// If there is a next PDL report lag, the return value is
	// non-negative, a multiple of the lag unit, and greather than last_pdl_lag.
	// Note: PDL report lag is relative to the start time of the PDL report sequence.
	// Note: If last_pdl_lag == -1L and there is a next PDL report lag, then the return value is 0L.
	// Note: If the return value is not -1L, then it is guaranteed that tstatus.is_pdl_retry_state() == true.

	public long get_next_pdl_lag (TimelineStatus tstatus, long next_forecast_lag, long last_pdl_lag, long base_pdl_time) {

		long next_pdl_lag = -1L;

		// If the timeline state requests a PDL retry ...

		if (tstatus.is_pdl_retry_state()) {

			// Get next PDL lag from the schedule, must be > last_pdl_lag

			if (last_pdl_lag >= 0L) {
				next_pdl_lag = sg.task_disp.get_action_config().get_next_pdl_report_retry_lag (last_pdl_lag + 1L);
			} else {
				next_pdl_lag = 0L;
			}

			// If there is another lag in the schedule ...

			if (next_pdl_lag >= 0L) {

				// This will be the PDL time ceiling (the retry must occur before this time)

				long pdl_time_ceiling = Long.MAX_VALUE;

				// If there is a next forecast, limit to a time shortly before the forecast

				if (next_forecast_lag >= 0L) {
					pdl_time_ceiling = Math.min (pdl_time_ceiling,
						tstatus.forecast_mainshock.mainshock_time + next_forecast_lag
						+ sg.task_disp.get_action_config().get_comcat_clock_skew()
						+ sg.task_disp.get_action_config().get_comcat_origin_skew()
						- sg.task_disp.get_action_config().get_forecast_min_gap());
				}

				// If there is a previous forecast (should always be), limit to a maximum time after it
				// Note: Originally the "else" did not appear on the following line.  With "else" the
				// staleness test is applied only after the last forecast.  Without "else" the staleness
				// test is applied to every forecast.

				else if (tstatus.get_last_forecast_lag() >= 0L) {
					pdl_time_ceiling = Math.min (pdl_time_ceiling,
						tstatus.forecast_mainshock.mainshock_time + tstatus.get_last_forecast_lag() + sg.task_disp.get_action_config().get_forecast_max_delay());
				}

				// Kill the PDL retry if it would not occur before the time ceiling
				// (The max below is the projected execution time of the PDL retry)

				if (Math.max (base_pdl_time + next_pdl_lag, sg.task_disp.get_time()) >= pdl_time_ceiling) {
					next_pdl_lag = -1L;
				}

			} else {
				next_pdl_lag = -1L;
			}
		}

		// Return next stage

		return next_pdl_lag;
	}




	// Post the next automatic database command for a timeline entry.
	// Parameters:
	//  tstatus = Timeline status.
	//  last_pdl_lag = Lag for the last PDL report attempt, or -1L if none.  Defaults to -1L if omitted.
	// Returns true if a task was posted.
	// Note: Typically last_pdl_lag == 0L if the caller has already attempted a PDL report, -1L otherwise.

	public boolean next_auto_timeline (TimelineStatus tstatus) {

		return next_auto_timeline (tstatus, -1L);
	}


	public boolean next_auto_timeline (TimelineStatus tstatus, long last_pdl_lag) {

		// Get the next forecast lag, or -1 if none

		long next_forecast_lag = get_next_forecast_lag (tstatus);

		// Get the next PDL report lag, or -1 if none, assuming the report begins now

		long next_pdl_lag = get_next_pdl_lag (tstatus, next_forecast_lag, last_pdl_lag, sg.task_disp.get_time());

		// If a PDL report is desired, and this is a primary server, submit the task

		if (next_pdl_lag >= 0L && sg.pdl_sup.is_pdl_primary()) {
		
			OpGeneratePDLReport pdl_payload = new OpGeneratePDLReport();
			pdl_payload.setup (
				tstatus.action_time,										// the_action_time
				tstatus.last_forecast_stamp,								// the_last_forecast_stamp
				sg.task_disp.get_time(),									// the_base_pdl_time
				tstatus.forecast_mainshock.mainshock_time);					// the_mainshock_time

			PendingTask.submit_task (
				tstatus.event_id,											// event id
				sg.task_disp.get_time() + next_pdl_lag,						// sched_time
				sg.task_disp.get_time(),									// submit_time
				SUBID_AAFS,													// submit_id
				OPCODE_GEN_PDL_REPORT,										// opcode
				sg.task_disp.get_action_config().lag_to_int (next_pdl_lag),	// stage
				pdl_payload.marshal_task());								// details

			sg.log_sup.report_pdl_report_request (tstatus.event_id, sg.task_disp.get_time() + next_pdl_lag);

			return true;
		}

		// If a forecast is desired, submit the task

		if (next_forecast_lag >= 0L) {

			String[] forecast_pdl_relay_ids;

			if (next_pdl_lag >= 0L) {	// if PDL report desired, only possible on a secondary server with tstatus.is_pdl_retry_state() == true
				forecast_pdl_relay_ids = tstatus.forecast_mainshock.get_confirm_relay_ids();
			} else {
				forecast_pdl_relay_ids = new String[0];
			}
		
			OpGenerateForecast forecast_payload = new OpGenerateForecast();
			forecast_payload.setup (
				tstatus.action_time,											// the_action_time
				tstatus.last_forecast_stamp,									// the_last_forecast_stamp
				next_forecast_lag,												// the_next_forecast_lag
				tstatus.forecast_mainshock.mainshock_time,						// the_mainshock_time
				forecast_pdl_relay_ids);										// the_pdl_relay_ids

			PendingTask.submit_task (
				tstatus.event_id,												// event id
				tstatus.forecast_mainshock.mainshock_time + next_forecast_lag 
				+ sg.task_disp.get_action_config().get_comcat_clock_skew()
				+ sg.task_disp.get_action_config().get_comcat_origin_skew(),	// sched_time
				sg.task_disp.get_time(),										// submit_time
				SUBID_AAFS,														// submit_id
				OPCODE_GEN_FORECAST,											// opcode
				0,																// stage
				forecast_payload.marshal_task());								// details

			sg.log_sup.report_forecast_request (tstatus.event_id,
				tstatus.forecast_mainshock.mainshock_time + next_forecast_lag 
				+ sg.task_disp.get_action_config().get_comcat_clock_skew()
				+ sg.task_disp.get_action_config().get_comcat_origin_skew());

			return true;
		}

		// If timeline state requests an action, submit an expire command

		if (tstatus.is_forecast_state() || tstatus.is_pdl_retry_state()) {

			String[] expire_pdl_relay_ids;
			long expire_sched_time;
			long expire_mainshock_time;

			if (next_pdl_lag >= 0L) {	// if PDL report desired, only possible on a secondary server with tstatus.is_pdl_retry_state() == true
				expire_pdl_relay_ids = tstatus.forecast_mainshock.get_confirm_relay_ids();
				expire_sched_time = tstatus.forecast_mainshock.mainshock_time + tstatus.get_last_forecast_lag() + sg.task_disp.get_action_config().get_forecast_max_delay();
				expire_mainshock_time = tstatus.forecast_mainshock.mainshock_time;
			} else {
				expire_pdl_relay_ids = new String[0];
				expire_sched_time = sg.task_sup.get_prompt_exec_time();		// can expire immediately if no PDL send pending
				expire_mainshock_time = 0L;
			}
		
			OpGenerateExpire expire_payload = new OpGenerateExpire();
			expire_payload.setup (
				tstatus.action_time,									// the_action_time
				tstatus.last_forecast_stamp,							// the_last_forecast_stamp
				expire_mainshock_time,									// the_mainshock_time
				expire_pdl_relay_ids);									// the_pdl_relay_ids

			PendingTask.submit_task (
				tstatus.event_id,										// event id
				expire_sched_time,										// sched_time
				sg.task_disp.get_time(),								// submit_time
				SUBID_AAFS,												// submit_id
				OPCODE_GEN_EXPIRE,										// opcode
				0,														// stage
				expire_payload.marshal_task());							// details

			sg.log_sup.report_expire_request (tstatus.event_id, expire_sched_time);

			return true;
		}

		// No command required

		sg.log_sup.report_timeline_idle (tstatus);

		return false;
	}




	// Set up Comcat retry for an intake command, while processing a Comcat exception.
	// Return values:
	//  RESCODE_STAGE_COMCAT_RETRY or RESCODE_STAGE_COMCAT_QUERY_RETRY = The task is being staged for retry.
	//  RESCODE_INTAKE_COMCAT_FAIL or RESCODE_INTAKE_COMCAT_QUERY_FAIL = Retries exhausted, the task has failed.

	public int intake_setup_comcat_retry (PendingTask task, ComcatException e) {

		sg.log_sup.report_comcat_exception (task.get_event_id(), e);

		// For PDL intake only, delete any other PDL intake commands for this event, so we don't have multiple retries going on
		// Note: The number deleted is limited so that, if PDL floods us with intake commands, we don't delete so many that
		// we exceed the MongoDB oplog size limit.  Any remaining commands cause no harm and are eventually processed or deleted.

		if (task.get_opcode() == OPCODE_INTAKE_PDL) {
			sg.task_sup.delete_all_waiting_tasks_limited (MONGO_DUP_INTAKE_CLEANUP, task.get_event_id(), OPCODE_INTAKE_PDL);
		}

		// Get the next ComCat retry lag

		long next_comcat_intake_lag = sg.task_disp.get_action_config().get_next_comcat_intake_lag (
										sg.task_disp.get_action_config().int_to_lag (task.get_stage()) + 1L );

		// If there is another retry, stage the task

		if (next_comcat_intake_lag >= 0L) {
			sg.task_disp.set_taskres_stage (task.get_sched_time() + next_comcat_intake_lag,
								sg.task_disp.get_action_config().lag_to_int (next_comcat_intake_lag));
			return (e instanceof ComcatQueryException) ? RESCODE_STAGE_COMCAT_QUERY_RETRY : RESCODE_STAGE_COMCAT_RETRY;
		}

		// Retries exhausted, display the error and log the task
		
		sg.task_disp.set_display_taskres_log ("TASK-ERR: Intake failed due to ComCat failure:\n"
			+ "opcode = " + get_opcode_as_string (task.get_opcode()) + "\n"
			+ "event_id = " + task.get_event_id() + "\n"
			+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

		return (e instanceof ComcatQueryException) ? RESCODE_INTAKE_COMCAT_QUERY_FAIL : RESCODE_INTAKE_COMCAT_FAIL;
	}




	// Convert event ID to timeline ID for an intake command.
	// Returns values:
	//  RESCODE_SUCCESS = The task already contains a timeline ID.
	//  RESCODE_STAGE_TIMELINE_ID or RESCODE_STAGE_COMCAT_RETRY or RESCODE_STAGE_COMCAT_QUERY_RETRY = The task is being staged
	//    for retry, either to start over with a timeline ID in place of an event ID, or
	//    to retry a failed Comcat operation.
	//  RESCODE_INTAKE_COMCAT_FAIL or RESCODE_INTAKE_COMCAT_QUERY_FAIL = Comcat retries exhausted, the command has failed.
	//  RESCODE_ALIAS_EVENT_NOT_IN_COMCAT = The event ID is not in Comcat.

	public int intake_event_id_to_timeline_id (PendingTask task) {

		// If the task already contains a timeline ID, just return

		if (sg.alias_sup.is_timeline_id (task.get_event_id())) {
			return RESCODE_SUCCESS;
		}

		// Get mainshock parameters for an event ID

		ForecastMainshock fcmain = new ForecastMainshock();
		int retval;

		try {
			retval = sg.alias_sup.get_mainshock_for_event_id (task.get_event_id(), fcmain);
		}

		// Handle Comcat exception

		catch (ComcatException e) {
			return intake_setup_comcat_retry (task, e);
		}

		// If event not in Comcat, then return

		if (retval == RESCODE_ALIAS_EVENT_NOT_IN_COMCAT) {
			return RESCODE_ALIAS_EVENT_NOT_IN_COMCAT;
		}

		sg.log_sup.report_candidate_event (fcmain);

		// If the event ID has not been seen before, create the alias timeline

		if (retval == RESCODE_ALIAS_NEW_EVENT) {
			sg.alias_sup.write_mainshock_to_new_timeline (fcmain);
		}

		// Stage the task, using the timeline ID in place of the event ID, for immediate execution

		sg.task_disp.set_taskres_stage (sg.task_sup.get_prompt_exec_time(),		// could use EXEC_TIME_MIN_WAITING
										task.get_stage(),
										fcmain.timeline_id);
		return RESCODE_STAGE_TIMELINE_ID;
	}




	// Test if a forecast or other PDL operation has already been sent (presumably by another server).
	// Parameters:
	//  tstatus = Timeline status.
	// Returns true if there is a relay item indicating that a PDL operation
	// has occured at equal or later lag (accounting for analyst options) than tstatus.last_forecast_stamp.
	// Note: This function attempts to be conservative, returning true only if it
	// is certain that a PDL operation has been sent.  It cannot eliminate the
	// possibility that it may return false even if a PDL operation has been sent.

	public boolean has_pdl_been_confirmed (TimelineStatus tstatus) {

		// Only do this check if the timeline is in a state that requires a PDL report
		// (This may be unnecessary because callers have probably already checked is_pdl_retry_state())

		if (tstatus.is_pdl_retry_state() && tstatus.get_last_forecast_lag() >= 0L) {		// the check on last_forecast_lag should be unnecessary

			// If there are relay items that confirm the last forecast, then consider it confirmed

			if (sg.relay_sup.is_pdl_prem_confirmation_of (tstatus.last_forecast_stamp, tstatus.forecast_mainshock.get_confirm_relay_ids())) {
				return true;
			}
		}

		// Not confirmed

		return false;
	}




	// A class that holds a pending task and a time.

	public static class TaskAndTime {
		public PendingTask task;
		public long time;

		public TaskAndTime (PendingTask the_task, long the_time) {
			task = the_task;
			time = the_time;
		}
	}




	// Comparator to sort TaskAndTime into ascending order by time (earliest first).

	public static class TaskAndTimeAscendingComparator implements Comparator<TaskAndTime> {
	
		// Compare two items

		@Override
		public int compare (TaskAndTime tandt1, TaskAndTime tandt2) {
			return Long.compare (tandt1.time, tandt2.time);
		}
	}




	// Comparator to sort TaskAndTime into descending order by time (most recent first).

	public static class TaskAndTimeDescendingComparator implements Comparator<TaskAndTime> {
	
		// Compare two items

		@Override
		public int compare (TaskAndTime tandt1, TaskAndTime tandt2) {
			return Long.compare (tandt2.time, tandt1.time);
		}
	}
	



	// Set the timeline to correct state for a primary server.
	// Returns the number of tasks canceled.
	// Currently, this scans the task queue, looking for any forecast or expire task that was issued
	// while the previous forecast was not yet sent to PDL (which can only happen if this was a
	// secondary server at the time the task was issued).  For each such task, we attempt to confirm
	// that the previous forecast has now been sent to PDL.  If we cannot confirm it, then the task
	// is canceled, which permits a PDL report task to be issued.
	// Note: This function should be called during idle time processing.

	public int set_timeline_to_primary () {

		// List of tasks to cancel

		ArrayList<TaskAndTime> tasks_to_cancel = new ArrayList<TaskAndTime>();

		// Scan the entire task queue

		try (
			RecordIterator<PendingTask> tasks = PendingTask.fetch_task_entry_range (0L, 0L, null, PendingTask.UNSORTED);
		){
			for (PendingTask task : tasks) {

				// Switch on opcode

				switch (task.get_opcode()) {

				// Forecast task

				case OPCODE_GEN_FORECAST:

					// If not already canceled ...

					if (task.get_stage() != STAGE_CANCEL) {

						// Get the payload

						OpGenerateForecast payload = new OpGenerateForecast();
						try {
							payload.unmarshal_task (task);
						}

						// Invalid task, skip it

						catch (Exception e) {
							break;
						}

						// If the last forecast was not sent to PDL ...

						if (payload.get_last_forecast_lag() >= 0L && payload.pdl_relay_ids.length > 0) {

							// If there are no relay items that confirm the last forecast ...

							if (!( sg.relay_sup.is_pdl_prem_confirmation_of (payload.last_forecast_stamp, payload.pdl_relay_ids) )) {

								// Put this task on the list to be canceled

								tasks_to_cancel.add (new TaskAndTime (task, payload.mainshock_time));
							}
						}
					}
					break;

				// Expire task

				case OPCODE_GEN_EXPIRE:

					// If not already canceled ...

					if (task.get_stage() != STAGE_CANCEL) {

						// Get the payload

						OpGenerateExpire payload = new OpGenerateExpire();
						try {
							payload.unmarshal_task (task);
						}

						// Invalid task, skip it

						catch (Exception e) {
							break;
						}

						// If the last forecast was not sent to PDL ...

						if (payload.get_last_forecast_lag() >= 0L && payload.pdl_relay_ids.length > 0) {

							// If there are no relay items that confirm the last forecast ...

							if (!( sg.relay_sup.is_pdl_prem_confirmation_of (payload.last_forecast_stamp, payload.pdl_relay_ids) )) {

								// Put this task on the list to be canceled

								tasks_to_cancel.add (new TaskAndTime (task, payload.mainshock_time));
							}
						}
					}
					break;
				}
			}
		}

		// Sort the tasks that need to be canceled, earliest first

		if (tasks_to_cancel.size() > 1) {
			tasks_to_cancel.sort (new TaskAndTimeDescendingComparator());
		}

		// Increment between cancels

		long cancel_time_increment = 10000L;		// 10 seconds

		// Initial execution time

		long exec_time = sg.task_disp.get_time();

		// Iterate over the tasks to cancel

		for (TaskAndTime task_to_cancel : tasks_to_cancel) {

			// Stage the task

			exec_time += cancel_time_increment;
			long eff_exec_time = Math.min (exec_time, task_to_cancel.task.get_exec_time());

			PendingTask.stage_task (task_to_cancel.task, eff_exec_time, STAGE_CANCEL, null);
		}

		// Return the number of canceled tasks

		sg.log_sup.report_timeline_to_primary (tasks_to_cancel.size());

		return tasks_to_cancel.size();
	}




	// Set the timeline to correct state for a secondary server.
	// Returns the number of tasks canceled.
	// Currently, this scans the task queue, looking for any PDL report task (which can only exist
	// if this was a primary server at the time the task was issued).  The task is canceled,
	// which permits the next forecast or expire command to be issued.
	// Note: This function should be called during idle time processing.

	public int set_timeline_to_secondary () {

		// List of tasks to cancel

		ArrayList<TaskAndTime> tasks_to_cancel = new ArrayList<TaskAndTime>();

		// Scan the entire task queue

		try (
			RecordIterator<PendingTask> tasks = PendingTask.fetch_task_entry_range (0L, 0L, null, PendingTask.UNSORTED);
		){
			for (PendingTask task : tasks) {

				// Switch on opcode

				switch (task.get_opcode()) {

				// PDL report task

				case OPCODE_GEN_PDL_REPORT:

					// If not already canceled ...

					if (task.get_stage() != STAGE_CANCEL) {

						// Get the payload

						OpGeneratePDLReport payload = new OpGeneratePDLReport();
						try {
							payload.unmarshal_task (task);
						}

						// Invalid task, skip it

						catch (Exception e) {
							break;
						}

						// Put this task on the list to be canceled

						tasks_to_cancel.add (new TaskAndTime (task, payload.mainshock_time));
					}
					break;
				}
			}
		}

		// Sort the tasks that need to be canceled, earliest first

		if (tasks_to_cancel.size() > 1) {
			tasks_to_cancel.sort (new TaskAndTimeDescendingComparator());
		}

		// Increment between cancels

		long cancel_time_increment = 10000L;		// 10 seconds

		// Initial execution time

		long exec_time = sg.task_disp.get_time();

		// Iterate over the tasks to cancel

		for (TaskAndTime task_to_cancel : tasks_to_cancel) {

			// Stage the task

			exec_time += cancel_time_increment;
			long eff_exec_time = Math.min (exec_time, task_to_cancel.task.get_exec_time());

			PendingTask.stage_task (task_to_cancel.task, eff_exec_time, STAGE_CANCEL, null);
		}

		// Return the number of canceled tasks

		sg.log_sup.report_timeline_to_secondary (tasks_to_cancel.size());

		return tasks_to_cancel.size();
	}




	// Get analyst options from relay items, for the given timeline id.
	// Returns the analyst options.
	// Returns null if no analyst options are available, which may be because:
	// - The timeline id is unknown.
	// - There are currently no Comcat ids assigned to the timeline.
	// - There is no analyst selection relay item for any of the Comcat ids.
	// - The most recent analyst selection relay item does not contain analyst options.
	// Note: The returned AnalystOptions object is newly allocated.

	public AnalystOptions get_analyst_options_from_relay (String timeline_id) {

		// Get the current list of Comcat ids for this timeline

		String[] comcat_ids = sg.alias_sup.get_comcat_ids_for_timeline_id (timeline_id);

		// If none, then return null

		if (comcat_ids == null || comcat_ids.length == 0) {
			return null;
		}

		// Get the most recent analyst intervention relay item

		RelayItem relit = sg.relay_sup.get_ansel_first_relay_item (comcat_ids);

		// If none, then return null

		if (relit == null) {
			return null;
		}

		// Get the analyst selection payload, if exception then return null

		RiAnalystSelection payload = new RiAnalystSelection();
		try {
			payload.unmarshal_relay (relit);
		} catch (Exception e) {
			return null;
		}

		// Return the analyst options from the payload, which could be null

		return payload.analyst_options;
	}




	// Update analyst options with the latest options in the relay items.
	// Parameters:
	//  tstatus = Timeline status, with timeline id in tstatus.event_id.
	// Returns true if analyst options were changed.
	// Note: tstatus.event_id is set if open_timeline() returns with timeline exists,
	// or if tstatus.set_state_track() has been called.
	// Design note: Replacing this function with a no-operation will make analyst options
	// be strictly inherited within the timeline (which was the original design).
	
	public boolean update_analyst_options_from_relay (TimelineStatus tstatus) {

		// If the timeline has no analyst options (happens only for error state), then no changes

		if (tstatus.analyst_options == null) {
			return false;
		}

		// Get the timeline id

		String timeline_id = tstatus.event_id;

		// Get the analyst options from relay items

		AnalystOptions new_analyst_options = get_analyst_options_from_relay (timeline_id);

		// If none ...

		if (new_analyst_options == null) {

			// If the timeline's analyst options have analyst_time == 0L then no change.  This permits
			// analyst_time == 0L to indicate that analyst options should not be touched by relay code
			// if no relay items exist, meaning that they are inherited within the timeline.

			if (tstatus.analyst_options.analyst_time == 0L) {
				return false;
			}

			// Here, no relay item exists but the timeline contains analyst options that came from
			// a relay item.  (This can only happen in case of a Comcat split or deletion.)
			// We restore system default options.

			new_analyst_options = new AnalystOptions();
		}

		// If the new options are the same as the old, then no action.
		// (We use the function in ForecastStamp to ensure this test is consistent with the
		// matching test used by ForecastStamp.)

		if (ForecastStamp.are_same_options (new_analyst_options, tstatus.analyst_options)) {
			return false;
		}

		// Insert the new analyst options, adjusting extra_forecast_lag if needed

		tstatus.set_analyst_data_2 (new_analyst_options);

		return true;
	}
	
//	public boolean update_analyst_options_from_relay (TimelineStatus tstatus) {
//
//		// If the timeline has no analyst options (happens only for error state), then no changes
//
//		if (tstatus.analyst_options == null) {
//			return false;
//		}
//
//		// Get the timeline id
//
//		String timeline_id = tstatus.event_id;
//
//		// Get the analyst options from relay items
//
//		AnalystOptions new_analyst_options = get_analyst_options_from_relay (timeline_id);
//
//		// If none, use default options
//
//		if (new_analyst_options == null) {
//			new_analyst_options = new AnalystOptions();
//		}
//
//		// If the analyst time is the same, consider it unchanged
//
//		boolean result = true;
//
//		if (new_analyst_options.analyst_time == tstatus.analyst_options.analyst_time) {
//			result = false;
//		}
//
//		// Preserve the extra forecast lag
//
//		new_analyst_options.extra_forecast_lag = tstatus.analyst_options.extra_forecast_lag;
//
//		// Insert the new analyst options
//
//		tstatus.analyst_options = new_analyst_options;
//
//		return result;
//	}




	//----- Construction -----


	// Default constructor.

	public TimelineSupport () {}

}
