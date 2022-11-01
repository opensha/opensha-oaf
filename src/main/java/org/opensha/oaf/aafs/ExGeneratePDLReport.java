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

import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Execute task: Generate PDL report retry.
 * Author: Michael Barall 06/25/2018.
 */
public class ExGeneratePDLReport extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_gen_pdl_report (task);
	}




	// Generate PDL report retry.

	private int exec_gen_pdl_report (PendingTask task) {

		//--- Get payload and timeline status

		OpGeneratePDLReport payload = new OpGeneratePDLReport();
		TimelineStatus tstatus = new TimelineStatus();

		int rescode = sg.timeline_sup.open_timeline (task, tstatus, payload);

		switch (rescode) {

		case RESCODE_TIMELINE_EXISTS:
			break;

		case RESCODE_TIMELINE_NOT_FOUND:
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry not found:\n"
				+ "event_id = " + task.get_event_id());
			return rescode;

		default:
			return rescode;
		}

		//--- Cancellation check

		// Check for cancel stage

		if (task.get_stage() == STAGE_CANCEL) {
		
			sg.task_disp.set_display_taskres_log ("TASK-INFO: PDL report command is canceled:\n"
				+ "event_id = " + task.get_event_id() + "\n"
				+ "payload.action_time = " + payload.action_time + "\n"
				+ "payload.last_forecast_stamp = " + payload.last_forecast_stamp.get_friendly_string());

			sg.timeline_sup.next_auto_timeline (tstatus);
			return RESCODE_PDL_CANCELED;
		}

		//--- Timeline state check

		// Check that timeline is sending a PDL report

		if (!( tstatus.is_pdl_retry_state() )) {
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry is not sending a PDL report:\n"
				+ "event_id = " + task.get_event_id() + "\n"
				+ "tstatus.fc_status = " + tstatus.get_fc_status_as_string() + "\n"
				+ "tstatus.pdl_status = " + tstatus.get_pdl_status_as_string());

			sg.timeline_sup.next_auto_timeline (tstatus);
			return RESCODE_TIMELINE_NOT_PDL_PEND;
		}

		// Check state matches the command

		if (!( payload.action_time == tstatus.action_time
			&& payload.last_forecast_stamp.is_equal_to (tstatus.last_forecast_stamp) )) {
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry state does not match task:\n"
				+ "event_id = " + task.get_event_id() + "\n"
				+ "payload.action_time = " + payload.action_time + "\n"
				+ "tstatus.action_time = " + tstatus.action_time + "\n"
				+ "payload.last_forecast_stamp = " + payload.last_forecast_stamp.get_friendly_string() + "\n"
				+ "tstatus.last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

			sg.timeline_sup.next_auto_timeline (tstatus);
			return RESCODE_TIMELINE_TASK_MISMATCH;
		}

		//  // Skip the operation if this is a secondary server
		//  // (This does not create an infinite loop because next_auto_timeline() never produces
		//  // a PDL report command on a secondary server)
		//  
		//  if (!( sg.pdl_sup.is_pdl_primary() )) {
		//  	sg.timeline_sup.next_auto_timeline (tstatus);
		//  	return RESCODE_PDL_SECONDARY;
		//  }


		//--- Find most recent forecast

		TimelineEntry pdl_tentry = null;
		CatalogSnapshot pdl_catsnap = null;
		TimelineStatus pdl_tstatus = null;
		CompactEqkRupList pdl_catalog = null;

		// Scan timeline for most recent forecast

		List<TimelineEntry> list_tentry = TimelineEntry.get_timeline_entry_range (0L, 0L, task.get_event_id(), null, null);

		for (TimelineEntry the_tentry : list_tentry) {
			if (the_tentry.get_actcode() == TimelineStatus.ACTCODE_FORECAST) {
				pdl_tentry = the_tentry;
				break;
			}
		}

		// No forecast found, fail it

		if (pdl_tentry == null) {

			// PDL report failed

			tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_FAILURE);
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: PDL report failed because unable to find forecast in timeline:\n"
				+ "event_id = " + tstatus.event_id + "\n"
				+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

			// Write the new timeline entry

			sg.timeline_sup.append_timeline (task, tstatus);

			// Log the task

			return RESCODE_TIMELINE_PDL_NO_FORECAST;
		}

		// Get the status for the forecast timeline entry

		try {
			pdl_tstatus = new TimelineStatus();
			pdl_tstatus.unmarshal_timeline (pdl_tentry);
		}

		// Invalid forecast timeline entry

		catch (Exception e) {

			// PDL report failed

			tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_FAILURE);
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: PDL report failed because unable to interpret forecast in timeline:\n"
				+ "event_id = " + tstatus.event_id + "\n"
				+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string() + "\n"
				+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

			// Write the new timeline entry

			sg.timeline_sup.append_timeline (task, tstatus);

			// Log the task

			return RESCODE_TIMELINE_PDL_BAD_FORECAST;
		}

		sg.log_sup.report_pdl_forecast_found (pdl_tstatus);

		// If there is a catalog snapshot for this forecast ...

		if (pdl_tstatus.has_catalog_snapshot()) {
		
			// Get the catalog snapshot

			pdl_catsnap = CatalogSnapshot.get_catalog_shapshot_for_key (pdl_tentry.get_record_key());

			// No catalog found, fail it

			if (pdl_catsnap == null) {

				// PDL report failed

				tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_FAILURE);
		
				sg.task_disp.set_display_taskres_log ("TASK-ERR: PDL report failed because unable to find catalog:\n"
					+ "event_id = " + tstatus.event_id + "\n"
					+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_TIMELINE_PDL_NO_CATALOG;
			}

			// Check parameter match

			if (!( pdl_catsnap.get_event_id().equals (pdl_tstatus.event_id)
				&& pdl_catsnap.get_start_time() == pdl_tstatus.forecast_results.catalog_start_time
				&& pdl_catsnap.get_end_time() == pdl_tstatus.forecast_results.catalog_end_time
				&& pdl_catsnap.get_eqk_count() == pdl_tstatus.forecast_results.catalog_eqk_count )) {

				// PDL report failed

				tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_FAILURE);
		
				sg.task_disp.set_display_taskres_log ("TASK-ERR: PDL report failed because of catalog parameter mismatch:\n"
					+ "event_id = " + tstatus.event_id + "\n"
					+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string() + "\n"
					+ "catalog event_id = " + pdl_catsnap.get_event_id() + "\n"
					+ "timeline event_id = " + pdl_tstatus.event_id + "\n"
					+ "catalog start_time = " + pdl_catsnap.get_start_time() + "\n"
					+ "timeline start_time = " + pdl_tstatus.forecast_results.catalog_start_time + "\n"
					+ "catalog end_time = " + pdl_catsnap.get_end_time() + "\n"
					+ "timeline end_time = " + pdl_tstatus.forecast_results.catalog_end_time + "\n"
					+ "catalog eqk_count = " + pdl_catsnap.get_eqk_count() + "\n"
					+ "timeline eqk_count = " + pdl_tstatus.forecast_results.catalog_eqk_count);

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_TIMELINE_PDL_CAT_MISMATCH;
			}

			// Get the catalog from the snapshot

			try {
				pdl_catalog = pdl_catsnap.get_rupture_list();
			}

			// Invalid catalog

			catch (Exception e) {

				// PDL report failed

				tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_FAILURE);
		
				sg.task_disp.set_display_taskres_log ("TASK-ERR: PDL report failed because unable to interpret catalog:\n"
					+ "event_id = " + tstatus.event_id + "\n"
					+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string() + "\n"
					+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_TIMELINE_PDL_BAD_CATALOG;
			}

			sg.log_sup.report_pdl_catalog_found (pdl_catsnap.get_event_id(), pdl_catsnap.get_eqk_count());
		}

		//--- PDL report

		// Check if PDL operation has already been done

		if (sg.timeline_sup.has_pdl_been_confirmed (pdl_tstatus)) {

			// PDL report confirmed

			tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_CONFIRMED);
		
			sg.task_disp.set_display_taskres_log ("TASK-INFO: PDL report has already been sent:\n"
				+ "event_id = " + tstatus.event_id + "\n"
				+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

			sg.log_sup.report_pdl_sent_already (tstatus);

			// Write the new timeline entry

			sg.timeline_sup.append_timeline (task, tstatus);

			// Log the task

			return RESCODE_PDL_CONFIRMED;
		}

		// Skip the operation if this is a secondary server
		// (This does not create an infinite loop because next_auto_timeline() never produces
		// a PDL report command on a secondary server)

		if (!( sg.pdl_sup.is_pdl_primary() )) {
		
			sg.task_disp.set_display_taskres_log ("TASK-INFO: PDL report not sent from secondary server:\n"
				+ "event_id = " + tstatus.event_id + "\n"
				+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

			sg.log_sup.report_pdl_not_sent_secondary (tstatus);

			sg.timeline_sup.next_auto_timeline (tstatus);

			return RESCODE_PDL_SECONDARY;
		}
			
		// Attempt to send the report

		String productCode = null;
		EventSequenceResult evseq_res = new EventSequenceResult();

		try {
			productCode = sg.pdl_sup.send_pdl_report (evseq_res, pdl_tstatus, pdl_catalog);
		}

		// Exception here means PDL report did not succeed

		catch (Exception e) {

			evseq_res.write_log (sg);
			sg.log_sup.report_pdl_send_exception (tstatus, e);

			// Get current PDL lag from the stage

			long new_next_pdl_lag = sg.task_disp.get_action_config().int_to_lag (task.get_stage());

			// Get the next forecast lag, or -1 if none

			long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

			// Get time of PDL retry

			new_next_pdl_lag = sg.timeline_sup.get_next_pdl_lag (tstatus, new_next_forecast_lag, new_next_pdl_lag, payload.base_pdl_time);

			// If there is another retry, stage the task

			if (new_next_pdl_lag >= 0L) {
				sg.task_disp.set_taskres_stage (payload.base_pdl_time + new_next_pdl_lag,
									sg.task_disp.get_action_config().lag_to_int (new_next_pdl_lag));

				return RESCODE_STAGE_PDL_RETRY;
			}

			// PDL report failed

			tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_FAILURE);
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Unable to send forecast report to PDL:\n"
				+ "event_id = " + tstatus.event_id + "\n"
				+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string() + "\n"
				+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));

			// Write the new timeline entry

			sg.timeline_sup.append_timeline (task, tstatus);

			// Log the task

			return RESCODE_TIMELINE_PDL_FAIL;
		}

		evseq_res.write_log (sg);
		if (productCode == null) {
			sg.log_sup.report_pdl_send_conflict (tstatus);
		} else {
			sg.log_sup.report_pdl_send_ok (tstatus, productCode);
		}

		// Write the relay item

		sg.relay_sup.submit_pdl_relay_item (
			pdl_tstatus.forecast_mainshock.get_pdl_relay_id(),		// event_id
			sg.task_disp.get_time(),								// relay_time
			true,													// f_force
			RiPDLCompletion.RIPDL_ACT_FORECAST_PDL,					// ripdl_action
			pdl_tstatus.last_forecast_stamp,						// ripdl_forecast_stamp
			sg.task_disp.get_time()									// ripdl_update_time
		);

		//--- Final steps

		// PDL report succeeded
			
		tstatus.set_state_pdl_update (sg.task_disp.get_time(), TimelineStatus.PDLSTAT_SUCCESS);

		// Write the new timeline entry

		sg.timeline_sup.append_timeline (task, tstatus);

		// Log the task

		return RESCODE_SUCCESS;
	}




	//----- Construction -----


	// Default constructor.

	public ExGeneratePDLReport () {}

}
