package org.opensha.oaf.aafs;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestMode;

/**
 * Clock for AAFS server.
 * Author: Michael Barall 03/18/2018.
 *
 * This class provides functions for getting and setting the time.
 * Normally, time is obtained from System.currentTimeMillis().
 * But, with this class it is possible to manipulate the clock for accelerated testing.
 */

// For most purposes, the time is obtained by calling get_time().
// Ordinarily it returns the value of System.currentTimeMillis().
// However, it is possible to "freeze" the clock at any desired time, after which get_time()
// returns the frozen time.  The frozen time can be set programmatically using freeze_time()
// or advance_frozen_time(), or from the command line using the "apptime" property (see TestMode).
//
// For some purposes, it is desireable to use the actual time even when the time is frozen.
// For these purposes, the time is obtained by calling get_true_time(), which ordinarily
// returns the value of System.currentTimeMillis() even when the time is frozen.
// However, it is possible to also "freeze" the true time, which causes get_true_time()
// to always return the frozen true time.  The frozen true time can only be set from the
// command line, using the "testtime" property (see TestMode).  Note that setting the "testtime"
// property also freezes the time returned by get_time(), and has other effects such as generating
// repeatable random seeds and database ids.  The "testtime" property is intended for unit
// testing, where the code attempts to generate results that are exactly repeatable.
//
// The time returned by get_true_time() is currently used in the following places:
// LogSupport.report_action: Time stamps written to the summary log file.
// ServerCmd: Time stamps for progress messages written to stdout.
// ServerCmd: Construct file names for console redirection and the summary log file.
// ServerCmd.cmd_pdl_intake: Filter PDL intake to include only mainshocks occurring
//  within an acceptable time range.
// ServerCmd.cmd_pdl_intake: Time stamps written to the intake log file.
// TaskDispatcher.exec_idle_time: Construct file names to split log files.
// TaskDispatcher.post_shutdown: Submission time for a shutdown command.
// TaskDispatcher.run: Record the dispatcher start time and last-active time.
// TaskDispatcher.run: Task dispatcher restart logic.
// TaskDispatcher.run: Task dispatcher polling loop.

public class ServerClock {

	// The frozen clock time, or 0L for normal clock operation, in milliseconds since the epoch.
	// If not initialized yet, the value is -1L.

	private static long frozen_time = -1L;

	// The frozen clock true time, or 0L for normal clock operation, in milliseconds since the epoch.
	// If not initialized yet, the value is -1L.

	private static long frozen_true_time = -1L;


	// init_time - Initialize the time variables.
	// Note: This should only be called after the caller has checked that
	// either frozen_time or frozen_true_time is < 0L.

	private static void init_time () {

		// In test mode, freeze the true time at the test time

		frozen_true_time = TestMode.get_test_time();

		// If an app time is set, initialize to frozen at that time

		frozen_time = TestMode.get_app_time();
		return;
	}


	/**
	 * get_time - Get the current time, in milliseconds since the epoch..
	 */
	public static synchronized long get_time () {

		// Check for frozen time, initialize if needed

		long the_time = frozen_time;
		if (the_time < 0L) {
			init_time();
			the_time = frozen_time;
		}

		// If not frozen time, check for frozen true time

		if (the_time == 0L) {
			the_time = frozen_true_time;
			if (the_time == 0L) {
				the_time = System.currentTimeMillis();
			}
		}
		return the_time;
	}


	/**
	 * freeze_time - Freeze the clock at the given time.
	 * @param at_time = Time at which to freeze the clock, in milliseconds since the epoch.
	 * A parameter value of 0L will un-freeze the clock.
	 * It is an error if the parameter is < 0L.
	 */
	public static synchronized void freeze_time (long at_time) {
		if (at_time < 0L) {
			throw new IllegalArgumentException ("ServerClock.freeze_time: Invalid parameter at_time = " + at_time);
		}
		if (frozen_time < 0L) {
			init_time();
		}
		frozen_time = at_time;
		return;
	}


	/**
	 * advance_frozen_time - Freeze the clock at the given time,
	 *                       if the given time is later than the current frozen time.
	 * @param at_time = Time at which to freeze the clock, in milliseconds since the epoch.
	 * A parameter value of 0L will not advance the clock.
	 * It is an error if the parameter is < 0L.
	 */
	public static synchronized void advance_frozen_time (long at_time) {
		if (at_time < 0L) {
			throw new IllegalArgumentException ("ServerClock.advance_frozen_time: Invalid parameter at_time = " + at_time);
		}
		if (frozen_time < 0L) {
			init_time();
		}
		if (at_time > frozen_time) {
			frozen_time = at_time;
		}
		return;
	}


	/**
	 * get_true_time - Get the true current time, in milliseconds since the epoch..
	 */
	public static synchronized long get_true_time () {

		// Check for frozen true time, initialize if needed

		long the_time = frozen_true_time;
		if (the_time < 0L) {
			init_time();
			the_time = frozen_true_time;
		}

		// If not frozen true time, use the system time

		if (the_time == 0L) {
			the_time = System.currentTimeMillis();
		}
		return the_time;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ServerClock : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Display the times and try frozen times.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("ServerClock : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Checking clock times");

				System.out.println();

				long true_time = ServerClock.get_true_time();
				long time = ServerClock.get_time();
				System.out.println ("true_time = " + SimpleUtils.time_raw_and_string (true_time));
				System.out.println ("time = " + SimpleUtils.time_raw_and_string (time));

				// Try advancing frozen time by 2 days

				ServerClock.advance_frozen_time (time + 86400000L + 86400000L);

				true_time = ServerClock.get_true_time();
				time = ServerClock.get_time();
				System.out.println ("[+2 days] true_time = " + SimpleUtils.time_raw_and_string (true_time));
				System.out.println ("[+2 days] time = " + SimpleUtils.time_raw_and_string (time));

				// Try backing up frozen time by 1 day, should not change

				ServerClock.advance_frozen_time (time - 86400000L);

				true_time = ServerClock.get_true_time();
				time = ServerClock.get_time();
				System.out.println ("[-1 day, no change expected] true_time = " + SimpleUtils.time_raw_and_string (true_time));
				System.out.println ("[-1 day, no change expected] time = " + SimpleUtils.time_raw_and_string (time));

				// Try advancing frozen time by 3 days

				ServerClock.advance_frozen_time (time + 86400000L + 86400000L + 86400000L);

				true_time = ServerClock.get_true_time();
				time = ServerClock.get_time();
				System.out.println ("[+3 days] true_time = " + SimpleUtils.time_raw_and_string (true_time));
				System.out.println ("[+3 days] time = " + SimpleUtils.time_raw_and_string (time));

				// Try backing up frozen time by 1 day, should work

				ServerClock.freeze_time (time - 86400000L);

				true_time = ServerClock.get_true_time();
				time = ServerClock.get_time();
				System.out.println ("[-1 day] true_time = " + SimpleUtils.time_raw_and_string (true_time));
				System.out.println ("[-1 day] time = " + SimpleUtils.time_raw_and_string (time));

				// Try unfreezing time, should work

				ServerClock.freeze_time (0L);

				true_time = ServerClock.get_true_time();
				time = ServerClock.get_time();
				System.out.println ("[unfreeze] true_time = " + SimpleUtils.time_raw_and_string (true_time));
				System.out.println ("[unfreeze] time = " + SimpleUtils.time_raw_and_string (time));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ServerClock : Unrecognized subcommand : " + args[0]);
		return;

	}

}
