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
import org.opensha.oaf.rj.AftershockStatsShadow;

import org.opensha.oaf.comcat.GeoJsonHolder;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

/**
 * Execute task: Generate forecast.
 * Author: Michael Barall 06/25/2018.
 */
public class ExGenerateForecast extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_gen_forecast (task);
	}




	// Calculate delay to enforce forecast rate limit, if needed.
	// The parameters is the rate limit time and the maximum allowed delay, in milliseconds.
	// The return value is the delay that is required, in milliseconds, or 0L if none..
	// Note: This would work for multiple threads, although the dispatcher is single-threaded.

	private static long forecast_wakeup_time = 0L;

	private static synchronized long enforce_rate_limit (long rate_limit, long max_delay) {
		long delay = 0L;

		// If rate limit is at least 1 second ...

		if (rate_limit >= 1000L && max_delay >= 1000L) {
			long time_now = System.currentTimeMillis();

			// If after the wakeup time, just set the next wakeup time

			if (time_now >= forecast_wakeup_time) {
				forecast_wakeup_time = time_now + rate_limit;
			}

			// Otherwise, delay until wakeup time, but not more than the maximum

			else {
				delay = Math.min (max_delay, forecast_wakeup_time - time_now);
				forecast_wakeup_time += rate_limit;
			}
		}

		return delay;
	}




	// Generate forecast.

	private int exec_gen_forecast (PendingTask task) {

		//--- Get payload and timeline status

		OpGenerateForecast payload = new OpGenerateForecast();
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
		
			sg.task_disp.set_display_taskres_log ("TASK-INFO: Forecast command is canceled:\n"
				+ "event_id = " + task.get_event_id() + "\n"
				+ "payload.action_time = " + payload.action_time + "\n"
				+ "payload.next_forecast_lag = " + payload.next_forecast_lag + "\n"
				+ "payload.last_forecast_stamp = " + payload.last_forecast_stamp.get_friendly_string());

			sg.timeline_sup.next_auto_timeline (tstatus);
			return RESCODE_FORECAST_CANCELED;
		}

		//--- Rate limit

		// Enforce rate limit if needed

		long rate_limit_delay = enforce_rate_limit (
			sg.task_disp.get_action_config().get_forecast_rate_limit(),
			sg.task_disp.get_action_config().get_forecast_max_limit()
		);

		// If requesting at least 1/4 second of delay, do the delay

		if (rate_limit_delay >= 250L) {
			try {
				Thread.sleep (rate_limit_delay);
			} catch (InterruptedException e) {
			}

			sg.log_sup.report_forecast_rate_limit (rate_limit_delay);
		}

		//--- Mainshock

		// Get mainshock parameters

		ForecastMainshock fcmain = new ForecastMainshock();

		try {
			sg.alias_sup.get_mainshock_for_timeline_id_generate (task.get_event_id(), fcmain);
		}

		// An exception here triggers a ComCat retry

		catch (ComcatException e) {
			return sg.timeline_sup.process_timeline_comcat_retry (task, tstatus, e);
		}

		// Update the analyst options from relay items

		sg.timeline_sup.update_analyst_options_from_relay (tstatus);

		//--- Timeline state check

		// Get the expected forecast lag

		long next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

		// Check that timeline is active

		if (next_forecast_lag < 0L) {
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry is not active:\n"
				+ "event_id = " + task.get_event_id() + "\n"
				+ "tstatus.fc_status = " + tstatus.get_fc_status_as_string());

			sg.timeline_sup.next_auto_timeline (tstatus);
			return RESCODE_TIMELINE_NOT_ACTIVE;
		}

		// Check state matches the command

		if (!( payload.action_time == tstatus.action_time
			&& payload.next_forecast_lag == next_forecast_lag
			&& payload.last_forecast_stamp.is_equal_to (tstatus.last_forecast_stamp) )) {
		
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry state does not match task:\n"
				+ "event_id = " + task.get_event_id() + "\n"
				+ "payload.action_time = " + payload.action_time + "\n"
				+ "tstatus.action_time = " + tstatus.action_time + "\n"
				+ "payload.next_forecast_lag = " + payload.next_forecast_lag + "\n"
				+ "next_forecast_lag = " + next_forecast_lag + "\n"
				+ "payload.last_forecast_stamp = " + payload.last_forecast_stamp.get_friendly_string() + "\n"
				+ "tstatus.last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

			sg.timeline_sup.next_auto_timeline (tstatus);
			return RESCODE_TIMELINE_TASK_MISMATCH;
		}

		//--- Premature forecasts

		// Check if it's too soon to do this forecast (might happen if mainshock origin time has changed)

		if (fcmain.mainshock_time + next_forecast_lag
			+ sg.task_disp.get_action_config().get_comcat_clock_skew() > sg.task_disp.get_time()) {
		
			// Stage the task

			sg.task_disp.set_taskres_stage (fcmain.mainshock_time + next_forecast_lag
								+ sg.task_disp.get_action_config().get_comcat_clock_skew()
								+ sg.task_disp.get_action_config().get_comcat_origin_skew(),
								task.get_stage());
			return RESCODE_STAGE_TOO_SOON;
		}

		//--- Stale forecasts

		// If we want to omit stale forecasts ...

		if (sg.task_disp.get_action_config().get_omit_stale_forecasts()) {
			for (;;) {

				// Possible forecast lag

				//long possible_min_lag = next_forecast_lag + sg.task_disp.get_action_config().get_forecast_min_gap();
				//long possible_forecast_lag = sg.task_disp.get_action_config().get_next_forecast_lag (
				//						possible_min_lag, tstatus.analyst_options.max_forecast_lag);

				long possible_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus, next_forecast_lag);

				// Stop if reached end of forecast lags

				if (possible_forecast_lag < 0L) {
					break;
				}

				// Stop if it's too soon to do this possible forecast

				if (fcmain.mainshock_time + possible_forecast_lag
					+ sg.task_disp.get_action_config().get_comcat_clock_skew() > sg.task_disp.get_time()) {
					break;
				}

				// Advance forecast lag

				next_forecast_lag = possible_forecast_lag;
			}
		}

		// If we want to skip stale forecasts ...

		if (sg.task_disp.get_action_config().get_skip_stale_forecasts()) {

			// Possible forecast lag

			//long possible_min_lag = next_forecast_lag + sg.task_disp.get_action_config().get_forecast_min_gap();
			//long possible_forecast_lag = sg.task_disp.get_action_config().get_next_forecast_lag (
			//						possible_min_lag, tstatus.analyst_options.max_forecast_lag);

			long possible_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus, next_forecast_lag);

			// If there is another forecast lag ...

			if (!( possible_forecast_lag < 0L )) {

				// If it's not too soon to do this possible forecast ...

				if (!( fcmain.mainshock_time + possible_forecast_lag
						+ sg.task_disp.get_action_config().get_comcat_clock_skew() > sg.task_disp.get_time() )) {

					// Insert skip into timeline status

					tstatus.set_state_skipped (sg.task_disp.get_time(), sg.task_disp.get_action_config(),
							fcmain, next_forecast_lag, TimelineStatus.FCRES_SKIPPED_STALE, null);

					// Get the next forecast lag, or -1 if none

					long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

					// If no next forecast, mark the timeline expired

					if (new_next_forecast_lag < 0L) {
						tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_EXPIRED);
					}

					// Display message

					sg.task_disp.set_display_taskres_log ("TASK-INFO: Stale forecast skipped:\n"
						+ "timeline_id = " + tstatus.event_id + "\n"
						+ "mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
						+ "mainshock_mag = " + fcmain.mainshock_mag);

					// Write the new timeline entry

					sg.timeline_sup.append_timeline (task, tstatus);

					// Log the task

					return RESCODE_FORECAST_STALE;
				}
			}
		}

		//--- Intake filtering

		// If analyst forces block ...

		if (tstatus.analyst_options.is_intake_blocked()) {

			// Insert skip into timeline status

			tstatus.set_state_skipped (sg.task_disp.get_time(), sg.task_disp.get_action_config(),
					fcmain, next_forecast_lag, TimelineStatus.FCRES_SKIPPED_ANALYST, null);

			// Get the next forecast lag, or -1 if none

			long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

			// If no next forecast, mark the timeline expired

			if (new_next_forecast_lag < 0L) {
				tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_EXPIRED);
			}

			// Display message

			sg.task_disp.set_display_taskres_log ("TASK-INFO: Blocked by analyst option:\n"
				+ "timeline_id = " + tstatus.event_id + "\n"
				+ "mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
				+ "mainshock_mag = " + fcmain.mainshock_mag);

			// Delete OAF products and write relay item

			sg.pdl_sup.delete_oaf_products (fcmain, RiPDLRemoval.RIPREM_REAS_SKIPPED_ANALYST, tstatus.last_forecast_stamp,
						EventSequenceParameters.get_cap_time_for_block (fcmain, tstatus.analyst_options.analyst_params));

			// Write the new timeline entry

			sg.timeline_sup.append_timeline (task, tstatus);

			// Log the task

			return RESCODE_FORECAST_ANALYST_BLOCKED;
		}

		// If analyst requests normal intake filtering (which is default) ...

		if (tstatus.analyst_options.is_intake_normal()) {

			// Search intake regions

			IntakeSphRegion intake_region = sg.task_disp.get_action_config().get_pdl_intake_region_for_min_mag (
				fcmain.mainshock_lat, fcmain.mainshock_lon, fcmain.mainshock_mag);

			// If none, then skip the forecast

			if (intake_region == null) {

				// If the timeline is still in intake state, and it is late enough to withdraw the timeline ...

				if (tstatus.is_intake_state() && next_forecast_lag >= sg.task_disp.get_action_config().get_withdraw_forecast_lag()) {
				
					// Withdraw the timeline

					tstatus.set_state_withdrawn (sg.task_disp.get_time(), fcmain);

					// Display message
		
					sg.task_disp.set_display_taskres_log ("TASK-INFO: Timeline entry withdrawn:\n"
						+ "timeline_id = " + tstatus.event_id + "\n"
						+ "fcmain.mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
						+ "fcmain.mainshock_lat = " + fcmain.mainshock_lat + "\n"
						+ "fcmain.mainshock_lon = " + fcmain.mainshock_lon + "\n"
						+ "fcmain.mainshock_mag = " + fcmain.mainshock_mag);

					// Write the new timeline entry

					sg.timeline_sup.append_timeline (task, tstatus);

					// Log the task

					return RESCODE_TIMELINE_WITHDRAW;
				}

				// Insert skip into timeline status

				tstatus.set_state_skipped (sg.task_disp.get_time(), sg.task_disp.get_action_config(),
						fcmain, next_forecast_lag, TimelineStatus.FCRES_SKIPPED_INTAKE, null);

				// Get the next forecast lag, or -1 if none

				long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

				// If no next forecast, mark the timeline expired

				if (new_next_forecast_lag < 0L) {
					tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_EXPIRED);
				}

				// Display message

				sg.task_disp.set_display_taskres_log ("TASK-INFO: Blocked by intake filter:\n"
					+ "timeline_id = " + tstatus.event_id + "\n"
					+ "fcmain.mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
					+ "fcmain.mainshock_lat = " + fcmain.mainshock_lat + "\n"
					+ "fcmain.mainshock_lon = " + fcmain.mainshock_lon + "\n"
					+ "fcmain.mainshock_mag = " + fcmain.mainshock_mag);

				// Delete OAF products and write relay item

				sg.pdl_sup.delete_oaf_products (fcmain, RiPDLRemoval.RIPREM_REAS_SKIPPED_INTAKE, tstatus.last_forecast_stamp,
							EventSequenceParameters.get_cap_time_for_block (fcmain, tstatus.analyst_options.analyst_params));

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_FORECAST_INTAKE_FILTERED;
			}
		}

//		// If intake need to be re-checked ...
//
//		if (tstatus.is_intake_state()) {
//
//			// Search intake regions
//
//			IntakeSphRegion intake_region = sg.task_disp.get_action_config().get_pdl_intake_region_for_min_mag (
//				fcmain.mainshock_lat, fcmain.mainshock_lon, fcmain.mainshock_mag);
//
//			// If none, then withdraw the timeline
//
//			if (intake_region == null) {
//		
//				sg.task_disp.set_display_taskres_log ("TASK-INFO: Timeline entry withdrawn:\n"
//					+ "timeline_id = " + tstatus.event_id + "\n"
//					+ "fcmain.mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
//					+ "fcmain.mainshock_lat = " + fcmain.mainshock_lat + "\n"
//					+ "fcmain.mainshock_lon = " + fcmain.mainshock_lon + "\n"
//					+ "fcmain.mainshock_mag = " + fcmain.mainshock_mag);
//
//				tstatus.set_state_withdrawn (sg.task_disp.get_time(), fcmain);
//				sg.timeline_sup.append_timeline (task, tstatus);
//
//				return RESCODE_TIMELINE_WITHDRAW;
//			}
//		}

		//--- Shadowing

		// If analyst requests normal shadowing detection (which is default) ...

		if (tstatus.analyst_options.is_shadow_normal()) {

			// Get find_shadow parameters

			ObsEqkRupture rup = fcmain.get_eqk_rupture();
			long time_now = fcmain.mainshock_time + next_forecast_lag;
			double search_radius = sg.task_disp.get_action_config().get_shadow_search_radius();
			long search_time_lo = fcmain.mainshock_time - sg.task_disp.get_action_config().get_shadow_lookback_time();
			long search_time_hi = fcmain.mainshock_time + next_forecast_lag;
			long centroid_rel_time_lo = 0L;
			long centroid_rel_time_hi = DURATION_HUGE;
			double centroid_mag_floor = sg.task_disp.get_action_config().get_shadow_centroid_mag();
			double large_mag = sg.task_disp.get_action_config().get_shadow_large_mag();
			double[] separation = new double[2];
			long[] seq_end_time = new long[2];

			// Run find_shadow

			ObsEqkRupture shadow;

			try {
				shadow = AftershockStatsShadow.find_shadow_v2 (rup, time_now,
					search_radius, search_time_lo, search_time_hi,
					centroid_rel_time_lo, centroid_rel_time_hi,
					centroid_mag_floor, large_mag, separation, seq_end_time);
			}

			// An exception here triggers a ComCat retry

			catch (Exception e) {
				return sg.timeline_sup.process_timeline_comcat_retry (task, tstatus, e);
			}

			// If we are shadowed ...

			if (shadow != null) {

				// Get cap time for any event-sequence product

				long cap_time = EventSequenceParameters.get_cap_time_for_shadow (fcmain, tstatus.analyst_options.analyst_params, seq_end_time[0]);

				// GeoJSON holder and flag for product deletion

				GeoJsonHolder gj_holder = new GeoJsonHolder();
				boolean f_del_evseq = true;

				// If this is a primary server ...

				if (sg.pdl_sup.is_pdl_primary()) {

					// Create a new event-sequence product if needed

					try {
						boolean f_new_only = false;
						f_del_evseq = sg.pdl_sup.send_shadowed_evseq (tstatus, fcmain, next_forecast_lag, cap_time, f_new_only, gj_holder);
					}

					// An exception here triggers a ComCat retry

					catch (Exception e) {
						return sg.timeline_sup.process_timeline_comcat_retry (task, tstatus, e);
					}
				}

				// Insert skip into timeline status

				tstatus.set_state_skipped (sg.task_disp.get_time(), sg.task_disp.get_action_config(),
						fcmain, next_forecast_lag, TimelineStatus.FCRES_SKIPPED_SHADOWED, shadow.getEventId());

				// Get the next forecast lag, or -1 if none

				long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

				// If no next forecast, mark the timeline expired

				if (new_next_forecast_lag < 0L) {
					tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_EXPIRED);
				}

				// Display message

				sg.task_disp.set_display_taskres_log ("TASK-INFO: Shadowing event detected:\n"
					+ "timeline_id = " + tstatus.event_id + "\n"
					+ "mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
					+ "mainshock_mag = " + fcmain.mainshock_mag + "\n"
					+ "mainshock_time = " + fcmain.mainshock_time + " (" + SimpleUtils.time_to_string(fcmain.mainshock_time) + ")" + "\n"
					+ "shadow event_id = " + shadow.getEventId() + "\n"
					+ "shadow mag = " + shadow.getMag() + "\n"
					+ "shadow time = " + shadow.getOriginTime() + " (" + SimpleUtils.time_to_string(shadow.getOriginTime()) + ")" + "\n"
					+ "distance = " + String.format("%.3f", separation[0]) + " km" + "\n"
					+ "interval = " + String.format("%.3f", separation[1]) + " days" + "\n"
					+ "shadow dur = " + ((seq_end_time[1] <= 0L) ? ("0") : (String.format("%.3f", ((double)(seq_end_time[1]))/SimpleUtils.DAY_MILLIS_D))) + " days");

				sg.log_sup.report_event_shadowed (
						fcmain.mainshock_event_id,
						shadow.getEventId(),
						fcmain.mainshock_mag,
						shadow.getMag(),
						separation[0],
						separation[1],
						seq_end_time[1]);

				// Delete OAF products and write relay item

				sg.pdl_sup.delete_oaf_products (fcmain, RiPDLRemoval.RIPREM_REAS_SKIPPED_SHADOWED, tstatus.last_forecast_stamp, cap_time, gj_holder, f_del_evseq);

				// Write the new timeline entry

				sg.timeline_sup.append_timeline (task, tstatus);

				// Log the task

				return RESCODE_FORECAST_SHADOWED;
			}
		}

		//--- Forecast

		// Fetch parameters (model and search parameters), and calculate results

		ForecastParameters forecast_params = new ForecastParameters();
		ForecastResults forecast_results = new ForecastResults();

		try {
			forecast_params.fetch_all_params (next_forecast_lag, fcmain, tstatus.analyst_options.analyst_params);

			forecast_params.next_scheduled_lag = sg.task_disp.get_action_config().get_next_forecast_lag (
				next_forecast_lag + sg.task_disp.get_action_config().get_forecast_min_gap(),	// min_lag
				tstatus.analyst_options.max_forecast_lag										// max_lag
			);

			if (forecast_params.next_scheduled_lag > 0L) {
				forecast_params.next_scheduled_lag += (
					sg.task_disp.get_action_config().get_comcat_clock_skew()
					+ sg.task_disp.get_action_config().get_comcat_origin_skew()
				);
			}

			long advisory_lag;

			if (next_forecast_lag >= sg.task_disp.get_action_config().get_advisory_dur_year()) {
				advisory_lag = ForecastResults.ADVISORY_LAG_YEAR;
			} else if (next_forecast_lag >= sg.task_disp.get_action_config().get_advisory_dur_month()) {
				advisory_lag = ForecastResults.ADVISORY_LAG_MONTH;
			} else if (next_forecast_lag >= sg.task_disp.get_action_config().get_advisory_dur_week()) {
				advisory_lag = ForecastResults.ADVISORY_LAG_WEEK;
			} else {
				advisory_lag = ForecastResults.ADVISORY_LAG_DAY;
			}

			String the_injectable_text = forecast_params.get_eff_injectable_text (
					sg.task_disp.get_action_config().get_def_injectable_text());

			forecast_results.calc_all (
				fcmain.mainshock_time + next_forecast_lag,
				advisory_lag,
				the_injectable_text,
				fcmain,
				forecast_params,
				next_forecast_lag >= sg.task_disp.get_action_config().get_seq_spec_min_lag());
		}

		// An exception here triggers a ComCat retry

		catch (Exception e) {
			return sg.timeline_sup.process_timeline_comcat_retry (task, tstatus, e);
		}

		// Select report for PDL, if any

		forecast_results.pick_pdl_model();

		// If we have an earthquake catalog ...

		if (forecast_results.catalog_result_avail) {

			// If there is an aftershock with larger magnitude than the mainshock ...

			if (forecast_results.catalog_eqk_count > 0 && forecast_results.catalog_max_mag > fcmain.mainshock_mag) {

				// If analyst requests normal shadowing detection (which is default) ...

				if (tstatus.analyst_options.is_shadow_normal()) {

					// Compute the shadow time, which is the time of the earliest aftershock whose magnitude is
					// larger than the mainshock; use the mainshock time if we don't find one

					int shadow_count = 0;
					long shadow_time = fcmain.mainshock_time;

					if (forecast_results.catalog_comcat_aftershocks != null) {	// should always be non-null
						for (ObsEqkRupture rup : forecast_results.catalog_comcat_aftershocks) {
							double rup_mag = rup.getMag();
							long rup_time = rup.getOriginTime();
							if (rup_mag > fcmain.mainshock_mag || (rup_mag == fcmain.mainshock_mag && rup_time > fcmain.mainshock_time)) {
								if (shadow_count == 0 || shadow_time > rup_time) {
									shadow_time = rup_time;
								}
								++shadow_count;
							}
						}
					}

					// Get cap time for any event-sequence product

					long cap_time = EventSequenceParameters.get_cap_time_for_shadow (fcmain, tstatus.analyst_options.analyst_params, shadow_time);

					// GeoJSON holder and flag for product deletion

					GeoJsonHolder gj_holder = new GeoJsonHolder();
					boolean f_del_evseq = true;

					// If this is a primary server ...

					if (sg.pdl_sup.is_pdl_primary()) {

						// Create a new event-sequence product if needed

						try {
							boolean f_new_only = false;
							f_del_evseq = sg.pdl_sup.send_shadowed_evseq (tstatus, fcmain, next_forecast_lag, cap_time, f_new_only, gj_holder);
						}

						// An exception here triggers a ComCat retry

						catch (Exception e) {
							return sg.timeline_sup.process_timeline_comcat_retry (task, tstatus, e);
						}
					}

					// Insert skip into timeline status

					tstatus.set_state_skipped (sg.task_disp.get_time(), sg.task_disp.get_action_config(),
							fcmain, next_forecast_lag, TimelineStatus.FCRES_SKIPPED_FORESHOCK, forecast_results.catalog_max_event_id);

					// Get the next forecast lag, or -1 if none

					long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

					// If no next forecast, mark the timeline expired

					if (new_next_forecast_lag < 0L) {
						tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_EXPIRED);
					}

					// Display message

					long shadow_dur = shadow_time - fcmain.mainshock_time;

					sg.task_disp.set_display_taskres_log ("TASK-INFO: Foreshock detected:\n"
						+ "timeline_id = " + tstatus.event_id + "\n"
						+ "mainshock_event_id = " + fcmain.mainshock_event_id + "\n"
						+ "mainshock_mag = " + fcmain.mainshock_mag + "\n"
						+ "catalog_max_event_id = " + forecast_results.catalog_max_event_id + "\n"
						+ "catalog_max_mag = " + forecast_results.catalog_max_mag + "\n"
						+ "shadow_dur = " + ((shadow_dur <= 0L) ? ("0") : (String.format("%.3f", ((double)(shadow_dur))/SimpleUtils.DAY_MILLIS_D))) + " days");

					sg.log_sup.report_event_foreshock (
							fcmain.mainshock_event_id,
							forecast_results.catalog_max_event_id,
							fcmain.mainshock_mag,
							forecast_results.catalog_max_mag,
							shadow_dur);

					// Delete OAF products and write relay item

					sg.pdl_sup.delete_oaf_products (fcmain, RiPDLRemoval.RIPREM_REAS_SKIPPED_FORESHOCK, tstatus.last_forecast_stamp, cap_time, gj_holder, f_del_evseq);

					// Write the new timeline entry

					sg.timeline_sup.append_timeline (task, tstatus);

					// Log the task

					return RESCODE_FORECAST_FORESHOCK;
				}
			
//				// Set timeline to shadowed state
//
//				tstatus.set_state_shadowed (sg.task_disp.get_time(), TimelineStatus.FCRES_SKIPPED_FORESHOCK, forecast_results.catalog_max_event_id);
//
//				// Display message
//
//				sg.task_disp.set_display_taskres_log ("TASK-INFO: Foreshock detected:\n"
//					+ "event_id = " + tstatus.event_id + "\n"
//					+ "mainshock_mag = " + fcmain.mainshock_mag + "\n"
//					+ "catalog_max_event_id = " + forecast_results.catalog_max_event_id + "\n"
//					+ "catalog_max_mag = " + forecast_results.catalog_max_mag);
//
//				// Write the timeline entry
//
//				sg.timeline_sup.append_timeline (task, tstatus);
//
//				// Log the task
//
//				return RESCODE_TIMELINE_FORESHOCK;
			}
		}

		// Insert forecast into timeline status

		tstatus.set_state_forecast (sg.task_disp.get_time(), sg.task_disp.get_action_config(), fcmain, forecast_params, forecast_results);

		// Get the next forecast lag, or -1 if none

		long new_next_forecast_lag = sg.timeline_sup.get_next_forecast_lag (tstatus);

		// If no next forecast, mark the timeline expired

		if (new_next_forecast_lag < 0L) {
			tstatus.set_fc_status (TimelineStatus.FCSTAT_STOP_EXPIRED);
		}

		//--- PDL report

		// If a PDL report is requested based on state ...

		boolean f_pdl_send_fail = false;

		if (tstatus.is_pdl_retry_state()) {

			// Get the next PDL report lag, or -1 if none, assuming the report begins now

			long new_next_pdl_lag = sg.timeline_sup.get_next_pdl_lag (tstatus, new_next_forecast_lag, -1L, sg.task_disp.get_time());

			// If no PDL report is requested based on timing, mark it bypassed

			if (new_next_pdl_lag < 0L) {
				tstatus.set_pdl_status (TimelineStatus.PDLSTAT_BYPASSED);
			}

			// Otherwise, check if PDL operation has already been done

			else if (sg.timeline_sup.has_pdl_been_confirmed (tstatus)) {
		
				sg.task_disp.display_taskinfo ("TASK-INFO: PDL report has already been sent:\n"
					+ "event_id = " + tstatus.event_id + "\n"
					+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

				sg.log_sup.report_pdl_sent_already (tstatus);

				tstatus.set_pdl_status (TimelineStatus.PDLSTAT_CONFIRMED);
			}

			// Otherwise, check for secondary status

			else if (!( sg.pdl_sup.is_pdl_primary() )) {
		
				sg.task_disp.set_display_taskres_log ("TASK-INFO: PDL report not sent from secondary server:\n"
					+ "event_id = " + tstatus.event_id + "\n"
					+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string());

				sg.log_sup.report_pdl_not_sent_secondary (tstatus);

				tstatus.set_pdl_status (TimelineStatus.PDLSTAT_SECONDARY);
			}

			// Otherwise, we need to send a PDL report ...

			else {
			
				// Attempt to send the report

				String productCode = null;
				EventSequenceResult evseq_res = new EventSequenceResult();

				try {
					productCode = sg.pdl_sup.send_pdl_report (evseq_res, tstatus);
					tstatus.set_pdl_status ((productCode == null) ? TimelineStatus.PDLSTAT_CONFLICT : TimelineStatus.PDLSTAT_SUCCESS);
					new_next_pdl_lag = -1L;
				}

				// Exception here means PDL report did not succeed

				catch (Exception e) {

					f_pdl_send_fail = true;

					evseq_res.write_log (sg);
					sg.log_sup.report_pdl_send_exception (tstatus, e);

					// Get time of PDL retry

					tstatus.set_pdl_status (TimelineStatus.PDLSTAT_PENDING);	// in case it was changed in the try block
					new_next_pdl_lag = sg.timeline_sup.get_next_pdl_lag (tstatus, new_next_forecast_lag, 0L, sg.task_disp.get_time());

					// If no retry, report the failure now

					if (new_next_pdl_lag < 0L) {
						tstatus.set_pdl_status (TimelineStatus.PDLSTAT_FAILURE);
		
						sg.task_disp.display_taskinfo ("TASK-ERR: Unable to send forecast report to PDL:\n"
							+ "event_id = " + tstatus.event_id + "\n"
							+ "last_forecast_stamp = " + tstatus.last_forecast_stamp.get_friendly_string() + "\n"
							+ "Stack trace:\n" + SimpleUtils.getStackTraceAsString(e));
					}
				}

				if (tstatus.is_pdl_send_successful()) {
					evseq_res.write_log (sg);
					if (productCode == null) {
						sg.log_sup.report_pdl_send_conflict (tstatus);
					} else {
						sg.log_sup.report_pdl_send_ok (tstatus, productCode);
					}

					// Write the relay item

					sg.relay_sup.submit_pdl_relay_item (
						tstatus.forecast_mainshock.get_pdl_relay_id(),			// event_id
						sg.task_disp.get_time(),								// relay_time
						true,													// f_force
						RiPDLCompletion.RIPDL_ACT_FORECAST_PDL,					// ripdl_action
						tstatus.last_forecast_stamp,							// ripdl_forecast_stamp
						sg.task_disp.get_time()									// ripdl_update_time
					);
				}
			}
		}

		//--- Final steps

		// Write the new timeline entry

		sg.timeline_sup.append_timeline (task, tstatus, 0L);

		// Log the task

		if (f_pdl_send_fail) {
			return RESCODE_FORECAST_PDL_SEND_FAIL;
		}

		return RESCODE_SUCCESS;
	}




	//----- Construction -----


	// Default constructor.

	public ExGenerateForecast () {}

}
