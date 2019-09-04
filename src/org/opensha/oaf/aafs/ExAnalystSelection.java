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

/**
 * Execute task: Analyst selection of options.
 * Author: Michael Barall 08/29/2019.
 */
public class ExAnalystSelection extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_analyst_selection (task);
	}




	// Analyst intervention.

	private int exec_analyst_selection (PendingTask task) {

		// Convert event ID to timeline ID if needed

		int etres = ansel_intake_event_id_to_timeline_id (task);
		if (etres != RESCODE_SUCCESS) {
			return etres;
		}

		//--- Get payload and timeline status

		OpAnalystSelection payload = new OpAnalystSelection();
		TimelineStatus tstatus = new TimelineStatus();

		int rescode = sg.timeline_sup.open_timeline (task, tstatus, payload);

		switch (rescode) {

		case RESCODE_TIMELINE_EXISTS:

			// Note: If the timeline exists, then this task is not allowed to
			// make any calls to Comcat.  This restriction ensures that analyst
			// options sent to an existing timeline ID are processed in the
			// order that they are submitted.  (Comcat calls could lead to
			// Comcat retries, which could re-order tasks.)

			// Note: ansel_intake_event_id_to_timeline_id saved the relay item to
			// the database before switching to the timeline id.  So, the new analyst
			// options are already retrieved by open_timeline.  

			// If request to start generating forecasts ...

			if (payload.ansel_payload.state_change == OpAnalystIntervene.ASREQ_START && tstatus.can_analyst_start()) {

				// Analyst intervention

				tstatus.set_state_analyst_intervention (sg.task_disp.get_time());

				// Update the state
			
				tstatus.set_fc_status (TimelineStatus.FCSTAT_ACTIVE_NORMAL);

				//  // Merge any requested extra forecast
				//  
				//  tstatus.merge_extra_forecast_lag (payload.ansel_payload.analyst_options.extra_forecast_lag);

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_SUCCESS;
			}

			// If request to stop generating forecasts ...

			if (payload.ansel_payload.state_change == OpAnalystIntervene.ASREQ_STOP && tstatus.can_analyst_stop()) {

				// Analyst intervention

				tstatus.set_state_analyst_intervention (sg.task_disp.get_time());

				// Update the state
			
				tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_ANALYST);

				//  // Merge any requested extra forecast
				//  
				//  tstatus.merge_extra_forecast_lag (payload.ansel_payload.analyst_options.extra_forecast_lag);

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_SUCCESS;
			}

			// If request to withdraw timeline ...

			if (payload.ansel_payload.state_change == OpAnalystIntervene.ASREQ_WITHDRAW && tstatus.can_analyst_withdraw()) {

				// Analyst intervention

				tstatus.set_state_analyst_intervention (sg.task_disp.get_time());

				// Update the state
			
				tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_WITHDRAWN);

				//  // Merge any requested extra forecast
				//  
				//  tstatus.merge_extra_forecast_lag (payload.ansel_payload.analyst_options.extra_forecast_lag);

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_SUCCESS;
			}

			// If request to set analyst data with no change in state

			if (tstatus.can_analyst_update()) {

				// Analyst intervention
			
				tstatus.set_state_analyst_intervention (sg.task_disp.get_time());

				//  // Merge any requested extra forecast
				//  
				//  tstatus.merge_extra_forecast_lag (payload.ansel_payload.analyst_options.extra_forecast_lag);

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_SUCCESS;
			}

			return RESCODE_TIMELINE_ANALYST_FAIL;

		case RESCODE_TIMELINE_NOT_FOUND:
			break;

		default:
			return rescode;
		}

		//--- Mainshock data

		// If not requesting timeline creation, just return

		if (!( payload.ansel_payload.f_create_timeline )) {
			return RESCODE_TIMELINE_ANALYST_NONE;
		}

		// Get mainshock parameters

		ForecastMainshock fcmain = new ForecastMainshock();
		int retval;

		try {
			retval = sg.alias_sup.get_mainshock_for_timeline_id (task.get_event_id(), fcmain);
		}

		// An exception here triggers a ComCat retry

		catch (ComcatException e) {
			return sg.timeline_sup.intake_setup_comcat_retry (task, e);
		}

		// If timeline is not found or stopped, just return

		if (retval != RESCODE_SUCCESS) {
			return retval;
		}

		//--- Final steps

		// Set track state
			
		tstatus.set_state_track (
			sg.task_disp.get_time(),
			sg.task_disp.get_action_config(),
			task.get_event_id(),
			fcmain,
			TimelineStatus.FCORIG_ANALYST,
			TimelineStatus.FCSTAT_ACTIVE_NORMAL);

		// If request to stop sending forecasts, create timeline in the stopped state

		if (payload.ansel_payload.state_change == OpAnalystIntervene.ASREQ_STOP) {
			tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_ANALYST);
		}

		// If request to withdraw timeline, create timeline in the withdrawn state

		if (payload.ansel_payload.state_change == OpAnalystIntervene.ASREQ_WITHDRAW) {
			tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_WITHDRAWN);
		}

		// Update analyst options from relay items

		sg.timeline_sup.update_analyst_options_from_relay (tstatus);

		//  // Merge any requested extra forecast
		//  
		//  tstatus.merge_extra_forecast_lag (payload.ansel_payload.analyst_options.extra_forecast_lag);

		// Write the new timeline entry

		sg.timeline_sup.append_timeline (task, tstatus);

		// Log the task

		return RESCODE_SUCCESS;
	}




	// Convert event ID to timeline ID for an intake command.
	// Returns values:
	//  RESCODE_SUCCESS = The task already contains a timeline ID.
	//  RESCODE_STAGE_TIMELINE_ID or RESCODE_STAGE_COMCAT_RETRY = The task is being staged
	//    for retry, either to start over with a timeline ID in place of an event ID, or
	//    to retry a failed Comcat operation.
	//  RESCODE_INTAKE_COMCAT_FAIL = Comcat retries exhausted, the command has failed.
	//  RESCODE_ALIAS_EVENT_NOT_IN_COMCAT = The event ID is not in Comcat.
	//  RESCODE_ANALYST_OPTIONS_STALE = Stopping because the relay item is stale (which implies
	//    that another task with the same relay item has already been processed).
	//  RESCODE_ANALYST_OPTIONS_BAD = Command contains bad parameters.
	// Note: This function is the same as TimelineSupport.intake_event_id_to_timeline_id
	// with the following changes:
	// - On first entry (stage == 0) with a Comcat ID, write the analyst selection relay
	//   item, and stop if the relay item is stale.
	// - Do not log this as a candidate intake event.

	private int ansel_intake_event_id_to_timeline_id (PendingTask task) {

		// If the task already contains a timeline ID, just return

		if (sg.alias_sup.is_timeline_id (task.get_event_id())) {
			return RESCODE_SUCCESS;
		}

		// If this is first entry ...

		if (task.get_stage() == 0) {

			// Get the payload

			OpAnalystSelection payload = new OpAnalystSelection();

			try {
				payload.unmarshal_task (task);
			}

			// Since these commands can be submitted externally, an error here
			// is just logged and not treated as database failure

			catch (Exception e) {
				return RESCODE_ANALYST_OPTIONS_BAD;
			}

			// Display the payload

			System.out.println (payload.toString());

			// If we want to write relay item ...

			if (payload.relay_write_option == OpAnalystSelection.RWOPT_WRITE_NEW) {

				// Attempt to write the relay item into the database

				int anres = sg.relay_sup.submit_ansel_relay_item (payload.ansel_relit, payload.relay_stamp, false,
					payload.ansel_payload, payload.event_id);

				// If not written, then this task is a duplicate

				if (anres <= 0) {
					return RESCODE_ANALYST_OPTIONS_STALE;
				}
			}
		}

		// Get mainshock parameters for an event ID

		ForecastMainshock fcmain = new ForecastMainshock();
		int retval;

		try {
			retval = sg.alias_sup.get_mainshock_for_event_id (task.get_event_id(), fcmain);
		}

		// Handle Comcat exception

		catch (ComcatException e) {
			return sg.timeline_sup.intake_setup_comcat_retry (task, e);
		}

		// If event not in Comcat, then return

		if (retval == RESCODE_ALIAS_EVENT_NOT_IN_COMCAT) {
			return RESCODE_ALIAS_EVENT_NOT_IN_COMCAT;
		}

		//sg.log_sup.report_candidate_event (fcmain);

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




	//----- Construction -----


	// Default constructor.

	public ExAnalystSelection () {}

}
