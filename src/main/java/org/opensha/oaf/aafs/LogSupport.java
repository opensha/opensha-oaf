package org.opensha.oaf.aafs;

import java.util.List;

import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TimeSplitOutputStream;

import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.oaf.comcat.ComcatConflictException;
import org.opensha.oaf.comcat.ComcatRemovedException;
import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.pdl.PDLCodeChooserOaf;
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;
import org.opensha.oaf.comcat.PropertiesEventSequence;

import org.opensha.oaf.oetas.env.OEtasLogInfo;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

/**
 * Support functions for logging.
 * Author: Michael Barall 06/23/2018.
 */
public class LogSupport extends ServerComponent {


	//----- Logging state -----


	// The summary log output destination, or null if none.

	private PrintStream summary_log_out = null;




	//----- Destination control -----


	// Set the summary log destination.
	// Destination can be null if no summary log is desired.

	public void set_summary_log_out (OutputStream dest) {

		if (dest == null) {
			summary_log_out = null;
		} else {
			boolean autoFlush = true;
			summary_log_out = new PrintStream (dest, autoFlush);
		}

		return;
	}




	//----- Internal reporting functions -----


	// Report an action.
	// Parameters:
	//  name = Name of action, must be non-null and non-empty
	//  items = List of items describing action, any null or empty items are ignored.

	private void report_action (String name, String... items) {

		// If we have a summary log ...

		if (summary_log_out != null) {

			StringBuilder result = new StringBuilder();

			// Time stamp

			result.append (SimpleUtils.time_to_string_no_z (ServerClock.get_true_time()));

			// Name

			result.append (" " + name);

			// Items

			String sep = ": ";

			for (String item : items) {
				if (!( item == null || item.isEmpty() )) {
					result.append (sep + item);
					sep = ", ";
				}
			}

			// Write the line

			summary_log_out.println (result.toString());
		}

		return;
	}



	// Report an exception.
	// Parameters:
	//  e = Exception, must be non-null.
	// Note: You should call report_action first.

	private void report_exception (Throwable e) {

		// If we have a summary log ...

		if (summary_log_out != null) {

			// Write the stack trace

			summary_log_out.println (SimpleUtils.getStackTraceAsString (e));
		}

		return;
	}



	// Report information.
	// Parameters:
	//  info = Information, can be multiple lines.
	// Note: You should call report_action first.

	private void report_info (String info) {

		// If we have a summary log ...

		if (summary_log_out != null) {

			// Write the information

			if (!( info == null || info.isEmpty() )) {
				summary_log_out.println (info);
			}
		}

		return;
	}




	//----- Reporting functions -----




	// Report an uncaught dispatcher exception.
	// Note: The task can be null.

	public void report_dispatcher_exception (PendingTask task, Throwable e) {
		report_action ("DISPATCHER-EXCEPTION");
		if (task != null) {
			report_info ("Failing task: " + task.toString());
		}
		report_exception (e);
		return;
	}




	// Report invalid task exception.

	public void report_invalid_task_exception (PendingTask task, Exception e) {
		report_action ("INVALID-TASK-EXCEPTION");
		report_info ("Invalid task: " + task.toString());
		report_info ("Dump: " + task.dump_details());
		report_exception (e);
		return;
	}




	// Report task begin.

	public void report_task_begin (PendingTask task) {
		report_action ("TASK-BEGIN",
					get_opcode_as_string (task.get_opcode()),
					"event_id = " + task.get_event_id(),
					"stage = " + task.get_stage());
		return;
	}




	// Report task restart.

	public void report_task_restart (PendingTask task) {
		report_action ("TASK-RESTART",
					get_opcode_as_string (task.get_opcode()),
					"event_id = " + task.get_event_id(),
					"stage = " + task.get_stage());
		return;
	}




	// Report task end.

	public void report_task_end (PendingTask task, int rescode) {
		report_action ("TASK-END",
					get_opcode_as_string (task.get_opcode()),
					get_rescode_as_string (rescode));
		return;
	}




	// Report task delete.

	public void report_task_delete (PendingTask task, int rescode) {
		report_action ("TASK-DELETE",
					get_opcode_as_string (task.get_opcode()),
					get_rescode_as_string (rescode));
		return;
	}




	// Report task stage.
	// Note: event_id can be null if the event ID is not being changed.

	public void report_task_stage (PendingTask task, int rescode, String event_id, int stage, long exec_time) {
		report_action ("TASK-STAGE",
					get_opcode_as_string (task.get_opcode()),
					get_rescode_as_string (rescode),
					(event_id == null) ? ("event_id = " + task.get_event_id()) : ("new_event_id = " + event_id),
					"stage = " + stage,
					"exec_time = " + SimpleUtils.time_to_string (exec_time));
		return;
	}




	// Report catalog snapshot saved.

	public void report_catalog_saved (String event_id, int eqk_count) {
		report_action ("CATALOG-SAVED",
					event_id,
					"eqk_count = " + eqk_count);
		return;
	}




	// Report timeline appended.

	public void report_timeline_appended (TimelineStatus tstatus) {

		if (tstatus.forecast_mainshock != null) {
			if (tstatus.is_first_entry()) {
				report_action ("INTAKE-EVENT",
					SimpleUtils.event_id_and_info_one_line (
						tstatus.forecast_mainshock.mainshock_event_id,
						tstatus.forecast_mainshock.mainshock_time,
						tstatus.forecast_mainshock.mainshock_mag,
						tstatus.forecast_mainshock.mainshock_lat,
						tstatus.forecast_mainshock.mainshock_lon,
						tstatus.forecast_mainshock.mainshock_depth)
				);
			}
			else {
				report_action ("TIMELINE-EVENT",
					SimpleUtils.event_id_and_info_one_line (
						tstatus.forecast_mainshock.mainshock_event_id,
						tstatus.forecast_mainshock.mainshock_time,
						tstatus.forecast_mainshock.mainshock_mag,
						tstatus.forecast_mainshock.mainshock_lat,
						tstatus.forecast_mainshock.mainshock_lon,
						tstatus.forecast_mainshock.mainshock_depth),
					String.format ("lag = %.3f days",
						((double)(tstatus.get_last_forecast_lag()))/ComcatOAFAccessor.day_millis)
				);
			}
		}

		report_action ("TIMELINE-APPENDED",
					tstatus.event_id,
					tstatus.get_actcode_as_string (),
					tstatus.get_fc_status_as_string (),
					(tstatus.has_pdl_product_code())
						? (tstatus.get_pdl_status_as_string () + " (" + tstatus.pdl_product_code + ")")
						: (tstatus.get_pdl_status_as_string ()),
					(tstatus.result_has_shadowing())
						? (tstatus.get_fc_result_as_string () + " (" + tstatus.shadowing_event_id + ")")
						: (tstatus.get_fc_result_as_string ()),
					"action_time = " + SimpleUtils.time_raw_and_string (tstatus.action_time));
		return;
	}




	// Report intake region selection.

	public void report_intake_region (String event_id, IntakeSphRegion intake_region) {
		report_action ("INTAKE-PASSED",
					event_id,
					"intake_region = " + intake_region.get_name());
		return;
	}




	// Report forecast request.

	public void report_forecast_request (String event_id, long sched_time) {
		report_action ("FORECAST-REQUEST",
					event_id,
					"sched_time = " + SimpleUtils.time_raw_and_string (sched_time));
		return;
	}




	// Report PDL report request.

	public void report_pdl_report_request (String event_id, long sched_time) {
		report_action ("PDL-REPORT-REQUEST",
					event_id,
					"sched_time = " + SimpleUtils.time_raw_and_string (sched_time));
		return;
	}




	// Report expire request.

	public void report_expire_request (String event_id, long sched_time) {
		report_action ("EXPIRE-REQUEST",
					event_id,
					"sched_time = " + SimpleUtils.time_raw_and_string (sched_time));
		return;
	}




	// Report timeline idle.

	public void report_timeline_idle (TimelineStatus tstatus) {

		report_action ("TIMELINE-IDLE",
					tstatus.event_id,
					tstatus.get_actcode_as_string (),
					tstatus.get_fc_status_as_string (),
					(tstatus.has_pdl_product_code())
						? (tstatus.get_pdl_status_as_string () + " (" + tstatus.pdl_product_code + ")")
						: (tstatus.get_pdl_status_as_string ()),
					(tstatus.result_has_shadowing())
						? (tstatus.get_fc_result_as_string () + " (" + tstatus.shadowing_event_id + ")")
						: (tstatus.get_fc_result_as_string ()),
					"action_time = " + SimpleUtils.time_raw_and_string (tstatus.action_time));
		return;
	}




	// Report dispatcher start.

	public void report_dispatcher_start () {
		report_info (LOG_SEPARATOR_LINE);
		report_action ("DISPATCHER-START");
		report_info (VersionInfo.get_title());
		return;
	}




	// Report dispatcher shutdown.

	public void report_dispatcher_shutdown () {
		report_action ("DISPATCHER-SHUTDOWN");
		return;
	}




	// Report dispatcher immediate shutdown.

	public void report_dispatcher_immediate_shutdown () {
		report_action ("DISPATCHER-IMMEDIATE-SHUTDOWN");
		return;
	}




	// Report dispatcher restart.

	public void report_dispatcher_restart () {
		report_action ("DISPATCHER-RESTART");
		return;
	}




	// Report timeline entry deleted.

	public void report_timeline_entry_deleted (String event_id) {
		report_action ("TIMELINE-ENTRY-DELETED",
					event_id);
		return;
	}




	// Report timeline task unwind.

	public void report_timeline_unwind (String event_id) {
		report_action ("TIMELINE-TASK-UNWIND",
					event_id);
		return;
	}




	// Report corrupt timeline exception.

	public void report_corrupt_timeline_exception (TimelineEntry tentry, PendingTask task, Exception e) {
		report_action ("CORRUPT-TIMELINE-EXCEPTION",
					task.get_event_id());
		report_info ("Timeline entry synopsis:\n" + tentry.toString());
		report_info ("Timeline entry details dump:\n" + tentry.dump_details());
		report_exception (e);
		return;
	}




	// Report timeline error appended.

	public void report_timeline_error_appended (String event_id) {
		report_action ("TIMELINE-ERROR-APPENDED",
					event_id);
		return;
	}




	// Report Comcat exception.
	// Note: event_id can be null.

	public void report_comcat_exception (String event_id, Exception e) {

		if (e instanceof ComcatRemovedException) {
			report_action ("COMCAT-REMOVED",
						event_id);
			String message = e.getMessage();
			if (!( message == null || message.isEmpty() )) {
				report_info (message);
			}
		}

		else {
			report_action ("COMCAT-EXCEPTION",
						event_id);
			report_exception (e);
		}

		return;
	}




	// Report event shadowed.

	public void report_event_shadowed (String event_id, String shadow_id,
					double event_mag, double shadow_mag, double distance, double interval) {
		report_action ("EVENT-SHADOWED",
					"event_id = " + event_id,
					"shadow_id = " + shadow_id,
					"event_mag = " + String.format("%.2f", event_mag),
					"shadow_mag = " + String.format("%.2f", shadow_mag),
					"distance = " + String.format("%.3f", distance) + " km",
					"interval = " + String.format("%.3f", interval) + " days");
		return;
	}

	public void report_event_shadowed (String event_id, String shadow_id,
					double event_mag, double shadow_mag, double distance, double interval, long shadow_dur) {
		report_action ("EVENT-SHADOWED",
					"event_id = " + event_id,
					"shadow_id = " + shadow_id,
					"event_mag = " + String.format("%.2f", event_mag),
					"shadow_mag = " + String.format("%.2f", shadow_mag),
					"distance = " + String.format("%.3f", distance) + " km",
					"interval = " + String.format("%.3f", interval) + " days",
					"shadow_dur = " + ((shadow_dur <= 0L) ? ("0") : (String.format("%.3f", ((double)(shadow_dur))/SimpleUtils.DAY_MILLIS_D))) + " days");
		return;
	}




	// Report event is a foreshock.

	public void report_event_foreshock (String event_id, String cat_max_id,
					double event_mag, double cat_max_mag) {
		report_action ("EVENT-FORESHOCK",
					"event_id = " + event_id,
					"cat_max_id = " + cat_max_id,
					"event_mag = " + String.format("%.2f", event_mag),
					"cat_max_mag = " + String.format("%.2f", cat_max_mag));
		return;
	}

	public void report_event_foreshock (String event_id, String cat_max_id,
					double event_mag, double cat_max_mag, long shadow_dur) {
		report_action ("EVENT-FORESHOCK",
					"event_id = " + event_id,
					"cat_max_id = " + cat_max_id,
					"event_mag = " + String.format("%.2f", event_mag),
					"cat_max_mag = " + String.format("%.2f", cat_max_mag),
					"shadow_dur = " + ((shadow_dur <= 0L) ? ("0") : (String.format("%.3f", ((double)(shadow_dur))/SimpleUtils.DAY_MILLIS_D))) + " days");
		return;
	}




	// Report successful PDL send.

	public void report_pdl_send_ok (TimelineStatus tstatus, String productCode) {
		report_action ("PDL-SEND-OK",
					"eventID = " + sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id),
					"productCode = " + productCode,
					"eventNetwork = " + tstatus.forecast_mainshock.mainshock_network,
					"eventCode = " + tstatus.forecast_mainshock.mainshock_code);
		return;
	}




	// Report successful PDL send, but not stored due to presence of conflicting forecast.

	public void report_pdl_send_conflict (TimelineStatus tstatus) {
		report_action ("PDL-SEND-CONFLICT",
					"eventID = " + sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id),
					"eventNetwork = " + tstatus.forecast_mainshock.mainshock_network,
					"eventCode = " + tstatus.forecast_mainshock.mainshock_code);
		return;
	}




	// Report PDL send exception.

	public void report_pdl_send_exception (TimelineStatus tstatus, Exception e) {
		report_action ("PDL-SEND-EXCEPTION",
					"eventID = " + sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id),
					"eventNetwork = " + tstatus.forecast_mainshock.mainshock_network,
					"eventCode = " + tstatus.forecast_mainshock.mainshock_code);
		report_exception (e);
		return;
	}




	// Report PDL alreeady sent.

	public void report_pdl_sent_already (TimelineStatus tstatus) {
		report_action ("PDL-SENT-ALREADY",
					"eventID = " + sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id),
					"eventNetwork = " + tstatus.forecast_mainshock.mainshock_network,
					"eventCode = " + tstatus.forecast_mainshock.mainshock_code);
		return;
	}




	// Report PDL not sent on a secondary server.

	public void report_pdl_not_sent_secondary (TimelineStatus tstatus) {
		report_action ("PDL-NOT-SENT-SECONDARY",
					"eventID = " + sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id),
					"eventNetwork = " + tstatus.forecast_mainshock.mainshock_network,
					"eventCode = " + tstatus.forecast_mainshock.mainshock_code);
		return;
	}




	// Report successful PDL send of an event-sequence product.

	public void report_evseq_send_ok (TimelineStatus tstatus, String productCode, long start_time, long end_time) {
		report_action ("EVSEQ-SEND-OK",
					"eventID = " + sg.alias_sup.timeline_id_to_pdl_code (tstatus.event_id),
					"start_time = " + SimpleUtils.time_raw_and_string (start_time),
					"end_time = " + SimpleUtils.time_raw_and_string (end_time),
					"productCode = " + productCode,
					"eventNetwork = " + tstatus.forecast_mainshock.mainshock_network,
					"eventCode = " + tstatus.forecast_mainshock.mainshock_code);
		return;
	}

	public void report_evseq_send_ok (String event_id, PropertiesEventSequence props, String pdl_code) {
		report_action ("EVSEQ-SEND-OK",
					"eventID = " + event_id,
					"start_time = " + props.s_start_time,
					"end_time = " + ((props.has_end_time()) ? props.s_end_time : "unbounded"),
					"event_time = " + ((props.has_event_time()) ? props.s_event_time : "omitted"),
					"productCode = " + pdl_code,
					"eventNetwork = " + props.eventNetwork,
					"eventCode = " + props.eventCode);
		return;
	}




	// Report Comcat poll done.

	public void report_comcat_poll_done (long poll_lookback, int count_no_timeline, int count_withdrawn_timeline) {
		double poll_lookback_days = ((double)poll_lookback)/ComcatOAFAccessor.day_millis;
		report_action ("COMCAT-POLL-DONE",
					"poll_lookback = " + String.format ("%.3f", poll_lookback_days) + " days",
					"count_no_timeline = " + count_no_timeline,
					"count_withdrawn_timeline = " + count_withdrawn_timeline);
		return;
	}




	// Report Comcat poll begin.

	public void report_comcat_poll_begin () {
		report_action ("COMCAT-POLL-BEGIN");
		return;
	}




	// Report Comcat poll end.

	public void report_comcat_poll_end (long next_poll_time) {
		report_action ("COMCAT-POLL-END",
					"next_poll_time = " + SimpleUtils.time_raw_and_string (next_poll_time));
		return;
	}




	// Report Comcat poll cleanup begin.

	public void report_comcat_poll_cleanup_begin () {
		report_action ("COMCAT-POLL-CLEANUP-BEGIN");
		return;
	}




	// Report Comcat poll cleanup end.

	public void report_comcat_poll_cleanup_end () {
		report_action ("COMCAT-POLL-CLEANUP-END");
		return;
	}




	// Report alias family created.

	public void report_alias_family_created (long family_time, String info) {
		report_action ("ALIAS-CREATED",
					"family_time = " + SimpleUtils.time_raw_and_string (family_time));
		report_info (info);
		return;
	}




	// Report alias family updated.

	public void report_alias_family_updated (long family_time, String info) {
		report_action ("ALIAS-UPDATED",
					"family_time = " + SimpleUtils.time_raw_and_string (family_time));
		report_info (info);
		return;
	}




	// Report candidate event.

	public void report_candidate_event (ForecastMainshock fcmain) {
		report_action ("CANDIDATE-EVENT",
			SimpleUtils.event_id_and_info_one_line (
				fcmain.mainshock_event_id,
				fcmain.mainshock_time,
				fcmain.mainshock_mag,
				fcmain.mainshock_lat,
				fcmain.mainshock_lon,
				fcmain.mainshock_depth)
		);
		return;
	}




	// Report PDL event.

	public void report_pdl_event (OpIntakePDL payload, long recv_time) {
		report_action ("PDL-EVENT",
			SimpleUtils.event_id_and_info_one_line (
				payload.event_id,
				payload.mainshock_time,
				payload.mainshock_mag,
				payload.mainshock_lat,
				payload.mainshock_lon,
				payload.mainshock_depth),
			"recv_time = " + SimpleUtils.time_to_string (recv_time)
		);
		return;
	}




	// Report PDL send forecast found.

	public void report_pdl_forecast_found (TimelineStatus tstatus) {
		report_action ("PDL-FORECAST-FOUND",
					tstatus.event_id,
					tstatus.get_actcode_as_string (),
					tstatus.get_fc_status_as_string (),
					(tstatus.has_pdl_product_code())
						? (tstatus.get_pdl_status_as_string () + " (" + tstatus.pdl_product_code + ")")
						: (tstatus.get_pdl_status_as_string ()),
					(tstatus.result_has_shadowing())
						? (tstatus.get_fc_result_as_string () + " (" + tstatus.shadowing_event_id + ")")
						: (tstatus.get_fc_result_as_string ()),
					"action_time = " + SimpleUtils.time_raw_and_string (tstatus.action_time));
		return;
	}




	// Report PDL send catalog found.

	public void report_pdl_catalog_found (String event_id, int eqk_count) {
		report_action ("PDL-CATALOG-FOUND",
					event_id,
					"eqk_count = " + eqk_count);
		return;
	}



	// Relay item operation codes

	public static final int RIOP_SAVE = 1;				// 1 = Locally-generated relay item saved.
	public static final int RIOP_STALE = 2;				// 2 = Locally-generated relay item is stale.
	public static final int RIOP_COPY = 3;				// 3 = Remote relay item saved.
	public static final int RIOP_DELETE = 4;			// 4 = Relay item deleted.
	public static final int RIOP_COPY_TASK = 5;			// 5 = Remote relay item saved into a task.




	// Report PDL completion relay item set.
	// op = Operation code:
	//  RIOP_SAVE = 1 = Locally-generated relay item saved.
	//  RIOP_STALE = 2 = Locally-generated relay item is stale.
	//  RIOP_COPY = 3 = Remote relay item saved.
	//  RIOP_DELETE = 4 = Relay item deleted.

	public void report_pdl_relay_set (int op, String event_id, long relay_time, RiPDLCompletion ripdl) {

		String name = "RELAY-PDL-SET-UNKNOWN";

		switch (op) {
		case RIOP_SAVE: name = "RELAY-PDL-SAVE"; break;
		case RIOP_STALE: name = "RELAY-PDL-STALE"; break;
		case RIOP_COPY: name = "RELAY-PDL-COPY"; break;
		case RIOP_DELETE: name = "RELAY-PDL-DELETE"; break;
		}

		report_action (name,
					event_id,
					ripdl.get_ripdl_action_as_string (),
					"relay_time = " + SimpleUtils.time_raw_and_string (relay_time),
					"forecast_stamp = " + ripdl.get_ripdl_forecast_stamp_as_string(),
					"update_time = " + ripdl.get_ripdl_update_time_as_string()
					);
		return;
	}




	// Report PDL removal relay item set.
	// op = Operation code:
	//  RIOP_SAVE = 1 = Locally-generated relay item saved.
	//  RIOP_STALE = 2 = Locally-generated relay item is stale.
	//  RIOP_COPY = 3 = Remote relay item saved.
	//  RIOP_DELETE = 4 = Relay item deleted.

	public void report_prem_relay_set (int op, String event_id, long relay_time, RiPDLRemoval riprem) {

		String name = "RELAY-PREM-SET-UNKNOWN";

		switch (op) {
		case RIOP_SAVE: name = "RELAY-PREM-SAVE"; break;
		case RIOP_STALE: name = "RELAY-PREM-STALE"; break;
		case RIOP_COPY: name = "RELAY-PREM-COPY"; break;
		case RIOP_DELETE: name = "RELAY-PREM-DELETE"; break;
		}

		report_action (name,
					event_id,
					riprem.get_riprem_reason_as_string (),
					"relay_time = " + SimpleUtils.time_raw_and_string (relay_time),
					"forecast_stamp = " + riprem.get_riprem_forecast_stamp_as_string(),
					"remove_time = " + riprem.get_riprem_remove_time_as_string(),
					"cap_time = " + riprem.get_riprem_cap_time_as_string()
					);
		return;
	}




	// Report PDL foreign relay item set.
	// op = Operation code:
	//  RIOP_SAVE = 1 = Locally-generated relay item saved.
	//  RIOP_STALE = 2 = Locally-generated relay item is stale.
	//  RIOP_COPY = 3 = Remote relay item saved.
	//  RIOP_DELETE = 4 = Relay item deleted.

	public void report_pfrn_relay_set (int op, String event_id, long relay_time, RiPDLForeign ripfrn) {

		String name = "RELAY-PFRN-SET-UNKNOWN";

		switch (op) {
		case RIOP_SAVE: name = "RELAY-PFRN-SAVE"; break;
		case RIOP_STALE: name = "RELAY-PFRN-STALE"; break;
		case RIOP_COPY: name = "RELAY-PFRN-COPY"; break;
		case RIOP_DELETE: name = "RELAY-PFRN-DELETE"; break;
		}

		report_action (name,
					event_id,
					ripfrn.get_ripfrn_status_as_string (),
					"relay_time = " + SimpleUtils.time_raw_and_string (relay_time),
					"detect_time = " + ripfrn.get_ripfrn_detect_time_as_string()
					);
		return;
	}




	// Report analyst selection relay item set.
	// op = Operation code:
	//  RIOP_SAVE = 1 = Locally-generated relay item saved.
	//  RIOP_STALE = 2 = Locally-generated relay item is stale.
	//  RIOP_COPY = 3 = Remote relay item saved.
	//  RIOP_DELETE = 4 = Relay item deleted.
	//  RIOP_COPY_TASK = 5 = Remote relay item saved into a task.

	public void report_ansel_relay_set (int op, String event_id, long relay_time, RiAnalystSelection riansel) {

		String name = "RELAY-ANSEL-SET-UNKNOWN";

		switch (op) {
		case RIOP_SAVE: name = "RELAY-ANSEL-SAVE"; break;
		case RIOP_STALE: name = "RELAY-ANSEL-STALE"; break;
		case RIOP_COPY: name = "RELAY-ANSEL-COPY"; break;
		case RIOP_DELETE: name = "RELAY-ANSEL-DELETE"; break;
		case RIOP_COPY_TASK: name = "RELAY-ANSEL-COPY-TASK"; break;
		}

		report_action (name,
					event_id,
					"relay_time = " + SimpleUtils.time_raw_and_string (relay_time),
					riansel.get_state_change_as_string(),
					"f_create_timeline = " + riansel.f_create_timeline,
					"analyst_time = " + riansel.get_analyst_time_as_string()
					);
		return;
	}




	// Report server status relay item set.
	// op = Operation code:
	//  RIOP_SAVE = 1 = Locally-generated relay item saved.
	//  RIOP_STALE = 2 = Locally-generated relay item is stale.
	//  RIOP_COPY = 3 = Remote relay item saved.
	//  RIOP_DELETE = 4 = Relay item deleted.

	public void report_sstat_relay_set (int op, long relay_time, RiServerStatus sstat) {

		String name = "SRV-STAT-SET-UNKNOWN";

		switch (op) {
		case RIOP_SAVE: name = "SRV-STAT-SAVE"; break;
		case RIOP_STALE: name = "SRV-STAT-STALE"; break;
		case RIOP_COPY: name = "SRV-STAT-COPY"; break;
		case RIOP_DELETE: name = "SRV-STAT-DELETE"; break;
		}

		report_action (name,
					sstat.get_server_number_as_string (),
					sstat.get_relay_mode_as_string (),
					sstat.get_configured_primary_as_string (),
					sstat.get_link_state_as_string (),
					sstat.get_primary_state_as_string (),
					sstat.get_inferred_state_as_string (),
					sstat.get_health_status_as_long_string (),
					"heartbeat = " + sstat.get_heartbeat_time_as_string(),
					"relay_time = " + SimpleUtils.time_raw_and_string (relay_time)
					);
		return;
	}




	// Report relay sync begin.

	public void report_relay_sync_begin (long lookback) {
		report_action ("RELAY-SYNC-BEGIN",
					"lookback = " + SimpleUtils.duration_to_string_2 (lookback)
					);
		return;
	}




	// Report relay sync copy.

	public void report_relay_sync_copy (int item_count) {
		report_action ("RELAY-SYNC-COPY",
					"item_count = " + item_count
					);
		return;
	}




	// Report relay sync end.

	public void report_relay_sync_end (int items_processed, int items_copied, long next_sync_time) {
		report_action ("RELAY-SYNC-END",
					"items_processed = " + items_processed,
					"items_copied = " + items_copied,
					"next_sync_time = " + SimpleUtils.time_raw_and_string (next_sync_time)
					);
		return;
	}




	// Report successful PDL delete of all OAF products for an event.

	public void report_pdl_delete_ok (String event_id) {
		report_action ("PDL-DELETE-OK",
					"eventID = " + event_id);
		return;
	}




	// Report PDL exception while attempting to delete all OAF products for an event.

	public void report_pdl_delete_exception (String event_id, Exception e) {
		report_action ("PDL-DELETE-EXCEPTION",
					"eventID = " + event_id);
		report_exception (e);
		return;
	}




	// Report successful PDL delete of all event-sequence products for an event.

	public void report_evseq_delete_ok (String event_id) {
		report_action ("EVSEQ-DELETE-OK",
					"eventID = " + event_id);
		return;
	}




	// Report successful PDL capping of an event-sequence product for an event.

	public void report_evseq_cap_ok (String event_id, long cap_time) {
		report_action ("EVSEQ-CAP-OK",
					"eventID = " + event_id,
					"cap_time = " + PDLCodeChooserEventSequence.cap_time_raw_and_string (cap_time));
		return;
	}

	public void report_evseq_cap_ok (String event_id) {
		report_action ("EVSEQ-CAP-OK",
					"eventID = " + event_id);
		return;
	}




	// Report cleanup query done.

	public void report_cleanup_query_done (long query_lookback, int count_with_oaf, int count_needing_cleanup) {
		double query_lookback_days = ((double)query_lookback)/ComcatOAFAccessor.day_millis;
		report_action ("CLEANUP-QUERY-DONE",
					"query_lookback = " + String.format ("%.3f", query_lookback_days) + " days",
					"count_with_oaf = " + count_with_oaf,
					"count_needing_cleanup = " + count_needing_cleanup);
		return;
	}




	// Report cleanup query begin.

	public void report_cleanup_query_begin () {
		report_action ("CLEANUP-QUERY-BEGIN");
		return;
	}




	// Report cleanup query end.

	public void report_cleanup_query_end (long next_query_time) {
		report_action ("CLEANUP-QUERY-END",
					"next_query_time = " + SimpleUtils.time_raw_and_string (next_query_time));
		return;
	}




	// Report cleanup event begin.

	public void report_cleanup_event_begin (String event_id) {
		report_action ("CLEANUP-EVENT-BEGIN",
					event_id);
		return;
	}




	// Report cleanup event end.

	public void report_cleanup_event_end (long doop) {
		report_action ("CLEANUP-EVENT-END",
					PDLCodeChooserOaf.get_doop_as_string (doop));
		return;
	}




	// Report timeline converted to primary.

	public void report_timeline_to_primary (int tasks_canceled) {
		report_action ("TIMELINE-TO-PRIMARY",
					"tasks_canceled = " + tasks_canceled);
		return;
	}




	// Report timeline converted to secondary.

	public void report_timeline_to_secondary (int tasks_canceled) {
		report_action ("TIMELINE-TO-SECONDARY",
					"tasks_canceled = " + tasks_canceled);
		return;
	}




	// Report results of an ETAS operation.

	public void report_etas_results (OEtasLogInfo etas_log_info) {

		if (etas_log_info != null && etas_log_info.has_log_entry()) {

			String name = "ETAS-UNKNOWN";

			switch (etas_log_info.etas_logtype) {
			case OEtasLogInfo.ETAS_LOGTYPE_OMIT: name = "ETAS-OMIT"; break;
			case OEtasLogInfo.ETAS_LOGTYPE_OK: name = "ETAS-OK"; break;
			case OEtasLogInfo.ETAS_LOGTYPE_FAIL: name = "ETAS-FAIL"; break;
			case OEtasLogInfo.ETAS_LOGTYPE_SKIP: name = "ETAS-SKIP"; break;
			case OEtasLogInfo.ETAS_LOGTYPE_UNKNOWN: name = "ETAS-UNKNOWN"; break;
			case OEtasLogInfo.ETAS_LOGTYPE_REJECT: name = "ETAS-REJECT"; break;
			}

			report_action (name,
						etas_log_info.etas_log_string
						);

			if (etas_log_info.has_etas_abort_message()) {
				report_info (etas_log_info.etas_abort_message);
			}
		}

		return;
	}




	// Report forecast rate limit triggered, if delay is positive.

	public void report_forecast_rate_limit (long delay) {
		if (delay > 0L) {
			double delay_secs = ((double)delay)/1000.0;
			report_action ("FORECAST-RATE-LIMIT",
						"delay = " + String.format ("%.3f", delay_secs) + " seconds");
		}
		return;
	}




	//----- Construction -----


	// Default constructor.

	public LogSupport () {
		summary_log_out = null;
	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		summary_log_out = null;

		return;
	}

}
