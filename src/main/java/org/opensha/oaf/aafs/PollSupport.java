package org.opensha.oaf.aafs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;

import org.opensha.oaf.util.EventNotFoundException;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.ObsEqkRupMaxTimeComparator;

import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.commons.data.comcat.ComcatException;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

/**
 * Support functions for polling Comcat.
 * Author: Michael Barall 08/05/2018.
 */
public class PollSupport extends ServerComponent {




	//----- Timing variables -----

	// True to poll using the ExPollComcatRun task, false to poll during idle.

	private boolean f_poll_using_task;

	// True if polling is currently enabled, false if not.

	private boolean f_polling_enabled;

	// True if a poll cleanup operation is needed, false if not (used when polling during idle time).

	private boolean f_poll_cleanup_needed;

	// True to allow the next poll to be long.

	private boolean f_long_poll_ok;

	// Time at which the next short poll can occur, in milliseconds since the epoch.

	private long next_short_poll_time;

	// Time at which the next long poll can occur, in milliseconds since the epoch.

	private long next_long_poll_time;




	//----- Polling subroutines -----




//	// Delete all waiting polling tasks.
//	// The currently active task is not deleted, even if it is a polling task.
//
//	public void delete_all_waiting_polling_tasks () {
//		sg.task_sup.delete_all_waiting_tasks (EVID_POLL, OPCODE_POLL_COMCAT_RUN, OPCODE_POLL_COMCAT_START, OPCODE_POLL_COMCAT_STOP);
//		return;
//	}




	// Delete all polling tasks (the poll Comcat run/start/stop tasks, and delayed poll intake tasks).
	// The currently active task is deleted, if it is a polling task.

	public void delete_all_existing_polling_tasks () {
		sg.task_sup.delete_all_tasks_for_event (EVID_POLL, OPCODE_INTAKE_POLL, OPCODE_POLL_COMCAT_RUN, OPCODE_POLL_COMCAT_START, OPCODE_POLL_COMCAT_STOP);
		return;
	}




	// Delete the poll Comcat run task, and all delayed poll intake tasks, if any.
	// The currently active task is not deleted, even if it would match.

	public void delete_waiting_poll_run_tasks () {
		sg.task_sup.delete_all_waiting_tasks (EVID_POLL, OPCODE_INTAKE_POLL, OPCODE_POLL_COMCAT_RUN);
		return;
	}




	// Delete the poll Comcat run task, and all delayed poll intake tasks, if any.
	// The currently active task is deleted, if it is a match.

	public void delete_all_existing_poll_run_tasks () {
		sg.task_sup.delete_all_tasks_for_event (EVID_POLL, OPCODE_INTAKE_POLL, OPCODE_POLL_COMCAT_RUN);
		return;
	}




	// Set polling to the disabled state.
	// Also deletes the poll Comcat run task, if any.
	// Note: This is to be called from the execution function of a polling task.

	public void set_polling_disabled () {

		if (f_poll_using_task) {
			delete_waiting_poll_run_tasks ();
		}

		f_polling_enabled = false;
		f_poll_cleanup_needed = true;

		return;
	}




	// Set polling to the enabled state.
	// Also deletes the poll Comcat run task, if any, and issues a new one.
	// Note: This is to be called from the execution function of a polling task.
	// Note: If polling is already enabled, the cycle is reset.

	public void set_polling_enabled () {

		if (f_poll_using_task) {
			delete_waiting_poll_run_tasks ();
		}

		f_polling_enabled = true;
		f_poll_cleanup_needed = true;
		f_long_poll_ok = false;
		next_short_poll_time = sg.task_disp.get_time();
		next_long_poll_time = sg.task_disp.get_time();

		// Kick off polling

		if (f_poll_using_task) {
			kick_off_polling (0L);
		}

		return;
	}




	// Initialize polling to the disabled state.
	// Also deletes all polling tasks.
	// Note: This is to be called from the task dispatcher, not during execution of a task.
	// This must be called before the first task is executed from the task queue.

	public void init_polling_disabled () {
		delete_all_existing_polling_tasks ();

		f_polling_enabled = false;
		f_poll_cleanup_needed = false;
		f_long_poll_ok = false;
		next_short_poll_time = 0L;
		next_long_poll_time = 0L;

		return;
	}




	// Issue the poll Comcat run task.
	// The first poll happens no earlier than the given delay after the current time, in milliseconds.

	public void kick_off_polling (long delay) {

		OpPollComcatRun poll_run_payload = new OpPollComcatRun();
		poll_run_payload.setup ();

		PendingTask.submit_task (
			EVID_POLL,										// event id
			Math.max (sg.task_disp.get_time() + delay, get_next_poll_time()),	// sched_time
			sg.task_disp.get_time(),						// submit_time
			SUBID_AAFS,										// submit_id
			OPCODE_POLL_COMCAT_RUN,							// opcode
			0,												// stage
			poll_run_payload.marshal_task());				// details

		return;
	}




	// Check if it is time to perform a poll.
	// Return value indicates which poll:
	//  0 = No poll now.
	//  1 = Do short poll.
	//  2 = Do long poll.

	public int check_if_poll_time () {

		// If polling is enabled ...

		if (f_polling_enabled) {

			// Jitter allowance is 40% of the short poll time

			long jitter = sg.task_disp.get_action_config().get_poll_short_period() * 2L / 5L;

			// Effective time is current time plus jitter allowance

			long eff_time_short = sg.task_disp.get_time();
			long eff_time_long = eff_time_short + jitter;
			if (f_poll_using_task) {
				eff_time_short = eff_time_long;
			}

			// If it's time for a short poll ...

			if (eff_time_short >= next_short_poll_time) {

				// If long poll is allowed ...

				if (f_long_poll_ok) {

					// If it's time for a long poll, return it

					if (eff_time_long >= next_long_poll_time) {
						return 2;
					}
				}

				// Otherwise, return short poll

				return 1;
			}
		}

		// No poll

		return 0;
	}




	// Update the last poll time.
	// Parameters:
	//  which_poll = 1 for short poll, 2 for long poll (must be return value from check_if_poll_time).
	//  total_delay = Delay time consumed by all delayed intake commands, in milliseconds.
	//  short_delay = Delay time consumed by delayed intake commands within range of a short poll, in milliseconds.
	// Note: For a short poll, short_delay should equal total_delay.

	public void update_last_poll_time (int which_poll, long total_delay, long short_delay) {

		// Jitter allowance is 60% of the short poll time
		// (Jitter allowances here and in check_if_poll_time should sum to 100%)

		long jitter = sg.task_disp.get_action_config().get_poll_short_period() * 3L / 5L;

		// End time of delay, requiring at least the jitter allowance after current time

		long delay_end_time = sg.task_disp.get_time() + Math.max (short_delay, jitter);

		// End time of period

		long period_end_time = next_short_poll_time
			+ (sg.task_disp.get_action_config().get_poll_short_period());

		// New time is the later of the two

		next_short_poll_time = Math.max (delay_end_time, period_end_time);

		// Allow long poll

		f_long_poll_ok = true;

		// If we just did a long poll ...

		if (which_poll == 2) {

			// Jitter allowance is 60% of the long poll time

			jitter = sg.task_disp.get_action_config().get_poll_long_period() * 3L / 5L;

			// End time of delay, requiring at least the jitter allowance after current time

			delay_end_time = sg.task_disp.get_time() + Math.max (total_delay, jitter);

			// End time of period

			period_end_time = next_long_poll_time
				+ (sg.task_disp.get_action_config().get_poll_long_period());

			// New time is the later of the two

			next_long_poll_time = Math.max (delay_end_time, period_end_time);

			// Cannot do two long polls in a row

			f_long_poll_ok = false;
		}

		return;
	}




	// Get the recommended execution time of the next poll Comcat run command.
	// The returned value is always at least the current time.

	public long get_next_poll_time () {

		// It's the time of the next short poll, but at least the current time

		return Math.max (next_short_poll_time, sg.task_disp.get_time());
	}




	// Run poll operation during task idle time.
	// On entry, these task dispatcher context variables must set up:
	//  dispatcher_time, dispatcher_true_time, dispatcher_action_config
	// This should be run during idle time, not during a MongoDB transaction.
	// Returns true if it did work, false if not.

	public boolean run_poll_during_idle (boolean f_verbose) {
		boolean result = false;

		if (f_poll_using_task) {
			return result;
		}

		//--- Cleanup operation

		// If cleanup is needed, do it

		if (f_poll_cleanup_needed) {

			// Say hello

			if (f_verbose) {
				System.out.println (LOG_SEPARATOR_LINE);
				System.out.println ("COMCAT-POLL-CLEANUP-BEGIN: " + SimpleUtils.time_to_string (sg.task_disp.get_time()));
			}
			sg.log_sup.report_comcat_poll_cleanup_begin ();

			result = true;

			// Delete all poll run tasks

			delete_all_existing_poll_run_tasks();

			// Clear the flag
		
			f_poll_cleanup_needed = false;

			// Say goodbye

			if (f_verbose) {
				System.out.println ("COMCAT-POLL-CLEANUP-END");
			}
			sg.log_sup.report_comcat_poll_cleanup_end ();
		}

		//--- Polling operation

		// Find which poll to do

		int which_poll = check_if_poll_time();

		// No poll, just return

		if (which_poll == 0) {
			return result;
		}

		// Check for intake blocked

		if ((new ServerConfig()).get_is_poll_intake_blocked()) {
			f_polling_enabled = false;
			f_poll_cleanup_needed = true;
			return result;
		}

		// Say hello

		if (f_verbose) {
			System.out.println (LOG_SEPARATOR_LINE);
			System.out.println ("COMCAT-POLL-BEGIN: " + SimpleUtils.time_to_string (sg.task_disp.get_time()));
		}
		sg.log_sup.report_comcat_poll_begin ();

		result = true;

		// Get lookback depending on whether it is a short or long poll

		long poll_lookback;

		if (which_poll == 2) {
			poll_lookback = sg.task_disp.get_action_config().get_poll_long_lookback();
		} else {
			poll_lookback = sg.task_disp.get_action_config().get_poll_short_lookback();
		}

		// Create the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Search the entire world, for minimum magnitude equal to the lowest in any intake region

		SphRegion search_region = SphRegion.makeWorld ();
		double min_mag = sg.task_disp.get_action_config().get_pdl_intake_region_min_min_mag();

		// Search within the lookback of the current time minus the clock skew

		long search_time_hi = sg.task_disp.get_time() - sg.task_disp.get_action_config().get_comcat_clock_skew();
		long search_time_lo = search_time_hi - poll_lookback;

		// This is the start of the search interval, for a short poll

		long search_time_short = search_time_hi - sg.task_disp.get_action_config().get_poll_short_lookback();

		// Call Comcat to get a list of potential events

		String exclude_id = null;

		double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

		boolean wrapLon = false;
		boolean extendedInfo = false;
		int limit_per_call = 0;
		int max_calls = 0;

		ObsEqkRupList potentials = null;

		try {
			potentials = accessor.fetchEventList (exclude_id,
					search_time_lo, search_time_hi,
					min_depth, max_depth,
					search_region, wrapLon, extendedInfo,
					min_mag, limit_per_call, max_calls);
		}
		catch (Exception e) {
		
			// In case of Comcat failure, just delay to next polling time

			next_short_poll_time = sg.task_disp.get_time()
								+ sg.task_disp.get_action_config().get_poll_short_period();

			// Say goodbye

			if (f_verbose) {
				System.out.println ("Poll failed due to Comcat exception");
				System.out.println ("COMCAT-POLL-END");
			}
			sg.log_sup.report_comcat_exception (null, e);
			sg.log_sup.report_comcat_poll_end (get_next_poll_time());

			return result;
		}

		double poll_lookback_days = ((double)poll_lookback) / ((double)DURATION_DAY);
		System.out.println ("COMCAT-POLL-INFO: Comcat poll found " + potentials.size() + " potential events in " + String.format ("%.3f", poll_lookback_days) + " days");

		// Process potential events in temporal order, most recent first

		if (potentials.size() > 1) {
			Collections.sort (potentials, new ObsEqkRupMaxTimeComparator());
		}

		// Loop over potential events

		int count_no_timeline = 0;
		int count_withdrawn_timeline = 0;

		long total_delay = 0L;
		long short_delay = 0L;
		boolean f_seen_long = false;

		for (ObsEqkRupture potential : potentials) {

			// Get the potential parameters

			String potential_event_id = potential.getEventId();
			long potential_time = potential.getOriginTime();
			double potential_mag = potential.getMag();
			Location potential_hypo = potential.getHypocenterLocation();
			double potential_lat = potential_hypo.getLatitude();
			double potential_lon = potential_hypo.getLongitude();

			// Search intake regions, using the minimum magnitude criterion

			IntakeSphRegion intake_region = sg.task_disp.get_action_config().get_pdl_intake_region_for_min_mag (
				potential_lat, potential_lon, potential_mag);

			// If we passed the intake filter ...

			if (intake_region != null) {

				// Try to identify the timeline for this event

				String timeline_id = sg.alias_sup.get_timeline_id_for_primary_id (potential_event_id);

				// Get the corresponding timeline entry, or null if none

				TimelineEntry tentry = null;

				if (timeline_id != null) {
					tentry = TimelineEntry.get_recent_timeline_entry (0L, 0L, timeline_id, null, null);
				}

				// If no timeline found ...

				if (tentry == null) {

					// For an event, process poll no earlier than the time of first forecast
					// (Intake is delayed as long as possible, because Comcat authoritative event ID
					// changes typically occur within the first 20 minutes after an earthquake, and
					// we attempt to avoid intake until the authoritative ID is fixed.)

					long first_forecast = potential_time
											+ sg.task_disp.get_action_config().get_next_forecast_lag(0L)
											+ sg.task_disp.get_action_config().get_comcat_clock_skew()
											+ sg.task_disp.get_action_config().get_comcat_origin_skew();

					// Submit a poll intake task for the event

					OpIntakePoll intake_payload = new OpIntakePoll();
					intake_payload.setup (potential_event_id);

					PendingTask.submit_task (
						EVID_POLL,									// event id
						Math.max (sg.task_disp.get_time() + total_delay, first_forecast),	// sched_time
						sg.task_disp.get_time(),					// submit_time
						SUBID_AAFS,									// submit_id
						OPCODE_INTAKE_POLL,							// opcode
						0,											// stage
						intake_payload.marshal_task());				// details

					++count_no_timeline;

					// Adjust total and short delay time

					if (which_poll == 2 && potential_time < search_time_short) {
						f_seen_long = true;
					}
					if (f_seen_long) {
						total_delay += sg.task_disp.get_action_config().get_poll_long_intake_gap();
					} else {
						total_delay += sg.task_disp.get_action_config().get_poll_short_intake_gap();
						short_delay = total_delay;
					}
				}

				// Otherwise, found timeline entry ...

				else {

					// If the last action was one that could indicate a withdrawn timeline ...

					if (TimelineStatus.can_actcode_intake_poll_start (tentry.get_actcode())) {

						// Get the status for this timeline entry
					
						TimelineStatus tstatus = new TimelineStatus();
					
						try {
							tstatus.unmarshal_timeline (tentry);
						}
					
						// Invalid timeline entry
					
						catch (Exception e2) {
							tstatus = null;
						}
					
						// If we got the timeline status ...
					
						if (tstatus != null) {
					
							// If it is in a state that can be awakened by a poll ...

							if (tstatus.can_intake_poll_start()) {

								// Submit a poll intake task for the timeline

								OpIntakePoll intake_payload = new OpIntakePoll();
								intake_payload.setup (timeline_id);

								PendingTask.submit_task (
									EVID_POLL,									// event id
									sg.task_disp.get_time() + total_delay,		// sched_time
									sg.task_disp.get_time(),					// submit_time
									SUBID_AAFS,									// submit_id
									OPCODE_INTAKE_POLL,							// opcode
									0,											// stage
									intake_payload.marshal_task());				// details

								++count_withdrawn_timeline;

								// Adjust total and short delay time

								if (which_poll == 2 && potential_time < search_time_short) {
									f_seen_long = true;
								}
								if (f_seen_long) {
									total_delay += sg.task_disp.get_action_config().get_poll_long_intake_gap();
								} else {
									total_delay += sg.task_disp.get_action_config().get_poll_short_intake_gap();
									short_delay = total_delay;
								}
							}
						}
					}
				}
			}
		}

		System.out.println ("COMCAT-POLL-INFO: Comcat poll found " + count_no_timeline + " potential events with no corresponding timeline");
		System.out.println ("COMCAT-POLL-INFO: Comcat poll found " + count_withdrawn_timeline + " potential events with a withdrawn timeline");

		sg.log_sup.report_comcat_poll_done (poll_lookback, count_no_timeline, count_withdrawn_timeline);

		//--- Final steps

		// Update the poll timers

		update_last_poll_time (which_poll, total_delay, short_delay);

		// Say goodbye

		if (f_verbose) {
			System.out.println ("COMCAT-POLL-END");
		}
		sg.log_sup.report_comcat_poll_end (get_next_poll_time());

		return result;	
	}




	//----- Construction -----


	// Default constructor.

	public PollSupport () {

		f_poll_using_task = false;

		f_polling_enabled = false;
		f_poll_cleanup_needed = false;
		f_long_poll_ok = false;
		next_short_poll_time = 0L;
		next_long_poll_time = 0L;

	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		f_poll_using_task = false;

		f_polling_enabled = false;
		f_poll_cleanup_needed = false;
		f_long_poll_ok = false;
		next_short_poll_time = 0L;
		next_long_poll_time = 0L;

		return;
	}

}
