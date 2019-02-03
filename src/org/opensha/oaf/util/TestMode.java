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
	// where the value is a long integer giving the time in milliseconds.
	// Omitting it, or specifying zero, means not to use a test time.

	// The test time, or 0L if not set, or -1L if not checked yet.

	private static long test_time = -1L;

	// Get the test time.
	// A return value <= 0L means no test time is set.

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
					test_time = Long.parseLong (s);
				} catch (Exception e) {
					test_time = 0L;
				}

				// Negative values are treated as not set

				if (test_time < 0L) {
					test_time = 0L;
				}
			}
		}
	
		return test_time;
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


}
