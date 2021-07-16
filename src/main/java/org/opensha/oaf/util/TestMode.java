package org.opensha.oaf.util;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.time.Duration;


/**
 * Class to hold test mode functions.
 * Author: Michael Barall.
 *
 * All functions in this class are static.
 */
public class TestMode {


	//----- Test Time -----
	//
	// This is used to set the current time, when running a test.
	// The time is set by the command line parameter -Dtesttime=
	// where the value is a long integer giving the time in milliseconds,
	// or a time in ISO-8601 format like 2011-12-03T10:15:30Z.
	// Omitting it, or specifying zero, means not to use a test time.
	// The goal is to generate repeatable results, by having time-dependent
	// code use the test time (e.g., for random seeds and database ids).

	// The test time, or 0L if not set, or -1L if not checked yet.

	private static long test_time = -1L;

	// Get the test time.
	// Returns 0L if no test time is set.
	// If a test time is set, returns a value > 0L which is the time in milliseconds since the epoch.
	// Note: The caller can use either value <= 0L or value == 0L to detect that no test time is set.

	public static synchronized long get_test_time () {

		// If not known ...

		if (test_time < 0L) {
		
			// Read value from command line parameter

			String s = System.getProperty ("testtime");
			if (s == null) {
			
				// Not found

				test_time = 0L;
			}
			else {
			
				// Try conversion to long, any error means not set

				try {
					test_time = SimpleUtils.string_or_number_to_time (s);
				} catch (Exception e) {
					test_time = 0L;
					throw new IllegalArgumentException ("TestMode.get_test_time: Invalid property testtime = " + s, e);
				}

				// Negative values are treated as not set

				if (test_time < 0L) {
					test_time = 0L;
				}
			}
		}
	
		return test_time;
	}




	//----- App Time -----
	//
	// This is used to set the time, as seen by application-level code.
	// In test mode, the app time is the same as the test time.
	// Otherwise, the time is set by the command line parameter -Dapptime=
	// where the value is a long integer giving the time in milliseconds,
	// or a time in ISO-8601 format like 2011-12-03T10:15:30Z.
	// Omitting it, or specifying zero, means not to use an app time.
	// The goal is to test the actions the code would take at different times, but
	// without fixing all time-dependent code (e.g., random seeds and database ids).

	// The app time, or 0L if not set, or -1L if not checked yet.

	private static long app_time = -1L;

	// Get the app time.
	// Returns 0L if no app time is set.
	// If an app time is set, returns a value > 0L which is the time in milliseconds since the epoch.
	// Note: The caller can use either value <= 0L or value == 0L to detect that no app time is set.
	// Note: In test mode, the return value is the same as get_test_time.

	public static synchronized long get_app_time () {

		// If not known ...

		if (app_time < 0L) {

			// Check for test time, use it if it is set

			long t = get_test_time();

			if (t > 0L) {
				app_time = t;
			}

			// Otherwise, check our parameter

			else {
		
				// Read value from command line parameter

				String s = System.getProperty ("apptime");
				if (s == null) {
			
					// Not found

					app_time = 0L;
				}
				else {
			
					// Try conversion to long, any error means not set

					try {
						app_time = SimpleUtils.string_or_number_to_time (s);
					} catch (Exception e) {
						app_time = 0L;
						throw new IllegalArgumentException ("TestMode.get_app_time: Invalid property apptime = " + s, e);
					}

					// Negative values are treated as not set

					if (app_time < 0L) {
						app_time = 0L;
					}
				}
			}
		}
	
		return app_time;
	}




	//----- Test Random Seed -----
	//
	// This is used to generate a random number seed, when running a test.
	// The seed is generated from the test time and a sequence number.

	// The test random seed sequence number.

	private static long test_ranseed_seq = 0L;

	// Get the test random seed sequence number.

	private static synchronized long get_test_ranseed_seq () {
		test_ranseed_seq += 987654321L;
		return test_ranseed_seq;
	}

	// Get the test random seed.
	// A return value <= 0L means no test is in progress.

	public static long get_test_ranseed () {

		// Get the test time

		long ranseed = get_test_time();

		// If no test, just set to zero

		if (ranseed <= 0L) {
			ranseed = 0L;
		}

		// Otherwise, combine with the sequence number

		else {
			ranseed += get_test_ranseed_seq();

			if (ranseed <= 0L) {	// really this should never happen
				ranseed = 123456789L - (ranseed / 2L);
			}
		}

		return ranseed;
	}




	//----- Test Connection Option -----
	//
	// This is used to set the default connection option, when running a test.
	// The time is set by the command line parameter -Dtestconopt=
	// where the value is an integer giving the connection option.
	// Omitting it, or specifying -1, means not to change the default.

	// The test time, or -1 if not set, or -2 if not checked yet.

	private static int test_conopt = -2;

	// Get the test connection option.
	// A return value < 0 means no test connection option is set.

	public static synchronized int get_test_conopt () {

		// If not known ...

		if (test_conopt < -1) {
		
			// Read value from command line parameter

			String s = System.getProperty ("testconopt");
			if (s == null) {
			
				// Not found

				test_conopt = -1;
			}
			else {
			
				// Try conversion to int, any error means not set

				try {
					test_conopt = Integer.parseInt (s);
				} catch (Exception e) {
					test_conopt = -1;
				}

				// Negative values are treated as not set

				if (test_conopt < -1) {
					test_conopt = -1;
				}
			}
		}
	
		return test_conopt;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("TestMode : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Display the times and random seeds.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("TestMode : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Checking test times");

				// Display result

				System.out.println();
				System.out.println ("test_time = " + SimpleUtils.time_raw_and_string (get_test_time()));
				System.out.println ("app_time = " + SimpleUtils.time_raw_and_string (get_app_time()));

				for (int i = 0; i < 10; ++i) {
					System.out.println (i + ": test_ranseed = " + get_test_ranseed());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("TestMode : Unrecognized subcommand : " + args[0]);
		return;

	}


}
