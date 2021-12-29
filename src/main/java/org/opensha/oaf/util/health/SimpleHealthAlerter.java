package org.opensha.oaf.util.health;

import org.opensha.oaf.util.SimpleUtils;


// Monitors the health of a subsystem, with simple alerting rules.
// Author: Michael Barall 12/27/2020.
//
// This object watches for a run of failures, and tracks the time at which
// the run began and the number of failures in the run.  Any success
// terminates the run.
//
// An alerting rule consists of a timeout and count.  An alert is raised
// if a run of failures is in progress, the time at which the run began is
// at least the timeout before the current time, and the number of failures
// is at least the count.
//
// It is possible to specify multiple alerting rules, in which case an alert
// is raised if any of the rules would raise an alert.
//
// A negative or zero timeout can be used to raise an alert based solely on the
// number of failures.  A negative or zero count is treated as if the count is 1.
//
// Threading: This class is thread-safe.

public class SimpleHealthAlerter implements HealthMonitor {

	// ----- Rules -----

	// The timeouts for alerting rules.
	// Each value is positive or zero.

	private long[] rule_timeout;

	// The counts for alerting rules.
	// Each value is at least 1.
	// The length of the array is the same as rule_timeout.

	private long[] rule_count;


	// Set the alerting rules, internal procedure.
	// Parameters:
	//  rules = An array, of even length, that contains the rules. Even-numbered
	//          elements contain timeouts, and odd-numbered elements contain counts.
	// Negative timeouts are converted to zero, and counts less than 1 are changed to 1.
	// Note that the timeouts and count can be given directly in the function call.

	private void do_set_alerting_rules (long... rules) {
		if (rules.length % 2 != 0) {
			throw new IllegalArgumentException ("SimpleHealthAlerter.do_set_alerting_rules: Invalid rule array length: length = " + rules.length);
		}
		rule_timeout = new long[rules.length / 2];
		rule_count = new long[rules.length / 2];
		for (int n = 0; n < rule_timeout.length; ++n) {
			rule_timeout[n] = Math.max (rules[2*n], 0L);
			rule_count[n] = Math.max (rules[2*n + 1], 1L);
		}
		return;
	}


	// Set the alerting rules.
	// Parameters:
	//  rules = An array, of even length, that contains the rules. Even-numbered
	//          elements contain timeouts, and odd-numbered elements contain counts.
	// Negative timeouts are converted to zero, and counts less than 1 are changed to 1.
	// Note that the timeouts and count can be given directly in the function call.

	public final synchronized void set_alerting_rules (long... rules) {
		do_set_alerting_rules (rules);
		return;
	}


	// Constructor.
	// Parameters:
	//  rules = An array, of even length, that contains the rules. Even-numbered
	//          elements contain timeouts, and odd-numbered elements contain counts.
	// Negative timeouts are converted to zero, and counts less than 1 are changed to 1.
	// Note that the timeouts and count can be given directly in the function call.

	public SimpleHealthAlerter (long... rules) {
		do_reset();
		do_set_alerting_rules (rules);
		return;
	}




	//----- Tracking -----

	// Time at which the current run of failures began.

	private long failure_time;

	// Number of failures in the current run.

	private long failure_count;


	// Reset the tracking, internal procedure.

	private void do_reset () {
		failure_time = 0L;
		failure_count = 0L;
		return;
	}




	// Make a string containing the state.

	@Override
	public synchronized String toString () {
		StringBuilder sb = new StringBuilder();

		sb.append ("SimpleHealthAlerter:\n");
		for (int n = 0; n < rule_timeout.length; ++n) {
			sb.append ("Rule " + n + ": count = " + rule_count[n] + ", timeout = " + SimpleUtils.duration_raw_and_string_2 (rule_timeout[n]) + "\n");
		}
		sb.append ("failure_time = " + SimpleUtils.time_raw_and_string_with_cutoff (failure_time, 0L) + "\n");
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
		do_reset();
		return;
	}


	// Report a failure.
	// Parameters:
	//  time_now = Time of occurrence, in milliseconds since the epoch.
	// If time_now is zero or omitted, then System.currentTimeMillis() is used.

	@Override
	public synchronized void report_failure (long time_now) {
		long report_time = ((time_now == 0L) ? System.currentTimeMillis() : time_now);

		// If start of run ...

		if (failure_count == 0L) {
			failure_count = 1L;
			failure_time = report_time;
		}

		// Otherwise, continue the run ...

		else {
			++failure_count;
			if (report_time < failure_time) {
				failure_time = report_time;	// needed because reports can be received out-of-order
			}
		}

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

		// If in a run of failures, check the alert rules

		if (failure_count > 0L) {
			long check_time = ((time_now == 0L) ? System.currentTimeMillis() : time_now);
			long time_diff = Math.max (0L, check_time - failure_time);
			for (int n = 0; n < rule_timeout.length; ++n) {
				if (time_diff >= rule_timeout[n] && failure_count >= rule_count[n]) {
					return true;
				}
			}
		}

		return false;
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




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("SimpleHealthAlerter : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Test the alerting functions of SimpleHealthAlerter.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("SimpleHealthAlerter : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Testing alerting functions for SimpleHealthAlerter");

				// Number of milliseconds per minute, hour, day

				long minute = 60L * 1000L;
				long hour = 60L * minute;
				long day = 24L * hour;

				// Base time is one day before now

				long base_time = System.currentTimeMillis() - day;

				// Alert time we use

				long[] alert_times = new long[3];
				alert_times[0] = base_time + hour;			// base time plus 1 hour
				alert_times[1] = base_time + (2L * hour);	// base time plus 2 hours
				alert_times[2] = 0L;						// now, which is base time plus 24 hours

				// Create a monitor with alerts: 12 failures in no time, 6 failures in 2 hours, 2 failures in 1 day

				SimpleHealthAlerter monitor = new SimpleHealthAlerter (0L, 12L, 2L * hour, 6L, day, 2L);

				System.out.println ();
				System.out.println (monitor.toString());

				// Test 15 failures at 1-minute intervals, starting at base time

				String result1 = test_alert_indications (monitor, 15, base_time, minute, alert_times);

				System.out.println ("15 failures at 1-minute intervals, starting at base time");
				System.out.println (result1);

				System.out.println (monitor.toString());

				// Success report

				test_sentinel_report (monitor, true, false, base_time + (15L * minute));

				System.out.println ("1 success");
				System.out.println (alert_indications_at_times (monitor, -1, alert_times));

				System.out.println ();
				System.out.println (monitor.toString());

				// Test 15 failures at (-1)-minute intervals, starting at base time + 7 minutes

				String result2 = test_alert_indications (monitor, 15, base_time + (7L * minute), -minute, alert_times);

				System.out.println ("15 failures at (-1)-minute intervals, starting at base time + 7 minutes");
				System.out.println (result2);

				System.out.println (monitor.toString());

				// Reset

				monitor.reset_monitor();

				System.out.println ("reset");
				System.out.println (alert_indications_at_times (monitor, -1, alert_times));

				System.out.println ();
				System.out.println (monitor.toString());

				// Test 15 failures at current time

				String result3 = test_alert_indications (monitor, 15, 0L, 0L, alert_times);

				System.out.println ("15 failures at current time");
				System.out.println (result3);

				System.out.println (monitor.toString());

				// Success report at current time

				test_sentinel_report (monitor, true, false, 0L);

				System.out.println ("1 success at current time");
				System.out.println (alert_indications_at_times (monitor, -1, alert_times));

				System.out.println ();
				System.out.println (monitor.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("SimpleHealthAlerter : Unrecognized subcommand : " + args[0]);
		return;

	}




}
