package org.opensha.oaf.util.health;

import org.opensha.oaf.util.SimpleUtils;


// Monitors the health of a subsystem, with simple success and failure counters.
// Author: Michael Barall 12/28/2020.
//
// This object keeps count of the number of successes and failures since it
// was created, or since the last reset.  It alerts if the most recent
// operation was failure.
//
// Threading: This class is thread-safe.

public class SimpleHealthCounter implements HealthMonitor {


	//----- Tracking -----

	// True if the most recent operation was a failure.

	private boolean f_alert;

	// Number of successes.

	private long success_count;

	// Number of failures.

	private long failure_count;


	// Reset the tracking, internal procedure.

	private void do_reset () {
		f_alert = false;
		success_count = 0L;
		failure_count = 0L;
		return;
	}


	// Constructor.

	public SimpleHealthCounter () {
		do_reset();
		return;
	}


	// Retrieve the counts.
	// Parameters:
	//  counts = Array of two (or more) elements:
	//           counts[0] receives the number of successes.
	//           counts[1] receives the number of failures.
	// Returns counts.

	public synchronized long[] get_counts (long[] counts) {
		counts[0] = success_count;
		counts[1] = failure_count;
		return counts;
	}




	// Make a string containing the state.

	@Override
	public synchronized String toString () {
		StringBuilder sb = new StringBuilder();

		sb.append ("SimpleHealthCounter:\n");

		sb.append ("f_alert = " + f_alert + "\n");
		sb.append ("success_count = " + success_count + "\n");
		sb.append ("failure_count = " + failure_count + "\n");
		
		return sb.toString();
	}




	//----- Implementation of HealthMonitor -----


	// Report a success.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// If time_now is zero or omitted, then System.currentTimeMillis() is used.

	@Override
	public synchronized void report_success (long time_now) {
		f_alert = false;
		++success_count;
		return;
	}


	// Report a failure.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// If time_now is zero or omitted, then System.currentTimeMillis() is used.

	@Override
	public synchronized void report_failure (long time_now) {
		f_alert = true;
		++failure_count;
		return;
	}


	// Reset the monitor.

	@Override
	public synchronized void reset_monitor () {
		do_reset();
		return;
	}


	// Check whether the monitor is in alert state, indicating lack of health.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// Returns true if there is an alert.
	// Note: Not all monitors may provide alerts.

	@Override
	public synchronized boolean check_alert (long time_now) {
		return f_alert;
	}




	//----- Testing -----




	// Subroutine to test monitor reports using a sentinel.
	// Parameters:
	//  monitor = The health monitor.
	//  f_success = True to switch sentinel to success state.
	//  f_failure = True to switch sentinel to failure state.
	//  time_now = Operation time, or 0L for now.
	// Note that f_success and f_failure can both be true, in which case the
	// sentinel is first set to success state and then to failure state.  If
	// neither is true, the sentinel is left in neutral state.

	protected static void test_sentinel_report (HealthMonitor monitor, boolean f_success, boolean f_failure, long time_now) {
		try (
			HealthSentinel health_sentinel = new HealthSentinel (monitor);
		){
			if (time_now != 0L) {
				health_sentinel.set_time (time_now);
			}
			if (f_success) {
				health_sentinel.set_success();
			}
			if (f_failure) {
				health_sentinel.set_failure();
			}
		}
		return;
	}


	// Subroutine to test alert indications at given times.
	// Parameters:
	//  monitor = The health monitor.
	//  test_num = Test number to appear at start of line, or -1 if none.
	//  alert_times = List or array of times at which to display alert indications.
	// Returns a string containing test results as a single line (with no \n termination).

	protected static String alert_indications_at_times (HealthMonitor monitor, int test_num, long... alert_times) {
		StringBuilder result = new StringBuilder();
		String sep = "";
		if (test_num >= 0) {
			result.append (test_num + ":");
			sep = " ";
		}
		for (int n = 0; n < alert_times.length; ++n) {
			result.append (sep + monitor.check_alert (alert_times[n]));
			sep = ", ";
		}
		return result.toString();
	}


	// Subroutine to test a series of alert indications.
	// Parameters:
	//  monitor = The health monitor.
	//  num_reports = The number of failure reports to make.
	//  report_time = The time of the first failure report, can be 0L for now.
	//  time_delta = The time delta to add after each report, should be 0L if report_time is 0L.
	//  alert_times = List or array of times at which to display alert indications.
	// Returns a string containing test results as multiple lines.

	protected static String test_alert_indications (HealthMonitor monitor, int num_reports, long report_time, long time_delta, long... alert_times) {
		StringBuilder result = new StringBuilder();
		long time_now = report_time;

		// Show initial alert indications

		result.append (alert_indications_at_times (monitor, 0, alert_times));
		result.append ("\n");

		// Make the requested failure reports, and show alert indications for each

		for (int n = 0; n < num_reports; ++n) {
			boolean f_success = (n % 2 == 1);
			boolean f_failure = true;
			test_sentinel_report (monitor, f_success, f_failure, time_now);
			time_now += time_delta;
			result.append (alert_indications_at_times (monitor, n + 1, alert_times));
			result.append ("\n");
		}

		return result.toString();
	}




	// Display the state of the monitor.

	protected static void show_monitor_state (SimpleHealthCounter monitor) {
		System.out.println (monitor.toString());
		long[] counts = monitor.get_counts (new long[2]);
		System.out.println ("counts[0] = " + counts[0]);
		System.out.println ("counts[1] = " + counts[1]);
		System.out.println ("check_alert = " + monitor.check_alert());
		return;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("SimpleHealthCounter : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Test the counting functions of SimpleHealthCounter.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("SimpleHealthCounter : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Testing counting functions for SimpleHealthCounter");

				// Create a monitor

				SimpleHealthCounter monitor = new SimpleHealthCounter ();

				System.out.println ();
				show_monitor_state (monitor);

				// 5 successes

				for (int k = 0; k < 5; ++k) {
					monitor.report_success();
				}

				System.out.println ();
				System.out.println ("After 5 successes:");
				System.out.println ();
				show_monitor_state (monitor);

				// 3 failures

				for (int k = 0; k < 3; ++k) {
					monitor.report_failure();
				}

				System.out.println ();
				System.out.println ("After 3 failures:");
				System.out.println ();
				show_monitor_state (monitor);

				// 7 successes

				for (int k = 0; k < 7; ++k) {
					monitor.report_success();
				}

				System.out.println ();
				System.out.println ("After 7 successes:");
				System.out.println ();
				show_monitor_state (monitor);

				// Reset

				monitor.reset_monitor();

				System.out.println ();
				System.out.println ("After reset:");
				System.out.println ();
				show_monitor_state (monitor);

				// 9 failures

				for (int k = 0; k < 9; ++k) {
					monitor.report_failure();
				}

				System.out.println ();
				System.out.println ("After 9 failures:");
				System.out.println ();
				show_monitor_state (monitor);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("SimpleHealthCounter : Unrecognized subcommand : " + args[0]);
		return;

	}




}
