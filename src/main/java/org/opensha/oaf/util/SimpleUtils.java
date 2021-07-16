package org.opensha.oaf.util;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;

import org.apache.commons.math3.distribution.UniformRealDistribution;


/**
 * Class to hold some simple utility functions.
 * Author: Michael Barall 05/29/2018.
 *
 * All functions in this class are static.
 */
public class SimpleUtils {




	// Number of milliseconds in a day, hour, minute, second.

	public static final long SECOND_MILLIS = 1000L;
	public static final long MINUTE_MILLIS = 60000L;
	public static final long HOUR_MILLIS = 3600000L;
	public static final long DAY_MILLIS = 86400000L;

	// Number of milliseconds in a day, hour, minute, second, in floating point.

	public static final double SECOND_MILLIS_D = 1000.0;
	public static final double MINUTE_MILLIS_D = 60000.0;
	public static final double HOUR_MILLIS_D = 3600000.0;
	public static final double DAY_MILLIS_D = 86400000.0;




	// Get a stack trace as a string.

	public static String getStackTraceAsString (Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		e.printStackTrace(pw);
		return sw.getBuffer().toString();
	}




	// Convert a time (in milliseconds after the epoch) to a human-readable string.

	public static String time_to_string (long the_time) {
		SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss z");
		fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		return fmt.format (new Date (the_time));
	}




	// Convert a time (in milliseconds after the epoch) to a human-readable string.
	// This version does not have the "UTC" suffix (but the time is still UTC).

	public static String time_to_string_no_z (long the_time) {
		SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
		fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		return fmt.format (new Date (the_time));
	}




	// Given a time (in milliseconds after the epoch), produce a string which
	// is its numerical value followed by the human-readable form in parentheses.

	public static String time_raw_and_string (long the_time) {
		return the_time + " (" + time_to_string(the_time) + ")";
	}




	// Given a time (in milliseconds after the epoch), produce a string which
	// is its numerical value followed by the human-readable form in parentheses.
	// Except that if the_time <= the_cutoff, then include only the numerical value.

	public static String time_raw_and_string_with_cutoff (long the_time, long the_cutoff) {
		if (the_time <= the_cutoff) {
			return Long.toString (the_time);
		}
		return time_raw_and_string (the_time);
	}




	// Convert a time (in milliseconds after the epoch) to a parseable string.
	// The result can be understood by string_to_time().

	public static String time_to_parseable_string (long the_time) {
		//SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ssz");
		//fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		//return fmt.format (new Date (the_time));
		return Instant.ofEpochMilli(the_time).toString();
	}




	// Convert a string in ISO-8601 format to time (in milliseconds after the epoch).
	// An example is 2011-12-03T10:15:30Z.

	public static long string_to_time (String s) {
		return Instant.parse(s).toEpochMilli();
	}




	// Convert a duration (in milliseconds) to a human-readable string in java.time.Duration format.

	public static String duration_to_string (long the_duration) {
		return Duration.ofMillis(the_duration).toString();
	}




	// Given a duration (in milliseconds), produce a string which
	// is its numerical value followed by the human-readable form in parentheses.

	public static String duration_raw_and_string (long the_duration) {
		return the_duration + " (" + duration_to_string(the_duration) + ")";
	}




	// Convert a string in java.time.Duration format to duration (in milliseconds).
	// An example is P3DT11H45M04S.

	public static long string_to_duration (String s) {
		return Duration.parse(s).toMillis();
	}




	// Convert a duration (in milliseconds) to a human-readable string in java.time.Duration format.
	// This version includes a "days" field for durations of 1 day or more.

	public static String duration_to_string_2 (long the_duration) {
		String result;

		// If negative, reverse the sign and prepend a minus sign

		long x = the_duration;
		String sign = "";

		if (x < 0L) {
			x = -x;
			sign = "-";
		}

		// Split

		long days = x / DAY_MILLIS;
		long hours = (x / HOUR_MILLIS) % 24L;
		long minutes = (x / MINUTE_MILLIS) % 60L;
		long seconds = (x / SECOND_MILLIS) % 60L;
		long millis = x % 1000L;

		// Leading field is days

		if (days != 0) {
			if (millis != 0) {
				result = String.format ("%sP%dDT%dH%dM%d.%03dS", sign, days, hours, minutes, seconds, millis);
			} else if (seconds != 0) {
				result = String.format ("%sP%dDT%dH%dM%dS", sign, days, hours, minutes, seconds);
			} else if (minutes != 0) {
				result = String.format ("%sP%dDT%dH%dM", sign, days, hours, minutes);
			} else if (hours != 0) {
				result = String.format ("%sP%dDT%dH", sign, days, hours);
			} else {
				result = String.format ("%sP%dD", sign, days);
			}
		}

		// Leading field is hours

		else if (hours != 0) {
			if (millis != 0) {
				result = String.format ("%sPT%dH%dM%d.%03dS", sign, hours, minutes, seconds, millis);
			} else if (seconds != 0) {
				result = String.format ("%sPT%dH%dM%dS", sign, hours, minutes, seconds);
			} else if (minutes != 0) {
				result = String.format ("%sPT%dH%dM", sign, hours, minutes);
			} else {
				result = String.format ("%sPT%dH", sign, hours);
			}
		}

		// Leading field is minutes

		else if (minutes != 0) {
			if (millis != 0) {
				result = String.format ("%sPT%dM%d.%03dS", sign, minutes, seconds, millis);
			} else if (seconds != 0) {
				result = String.format ("%sPT%dM%dS", sign, minutes, seconds);
			} else {
				result = String.format ("%sPT%dM", sign, minutes);
			}
		}

		// Leading field is seconds

		else {
			if (millis != 0) {
				result = String.format ("%sPT%d.%03dS", sign, seconds, millis);
			} else {
				result = String.format ("%sPT%dS", sign, seconds);
			}
		}

		return result;
	}




	// Given a duration (in milliseconds), produce a string which
	// is its numerical value followed by the human-readable form in parentheses.
	// This version includes a "days" field for durations of 1 day or more.

	public static String duration_raw_and_string_2 (long the_duration) {
		return the_duration + " (" + duration_to_string_2(the_duration) + ")";
	}




	// Given information about an event, produce a one-line summary.

	public static String event_info_one_line (long event_time,
			double event_mag, double event_lat, double event_lon, double event_depth) {

		StringBuilder result = new StringBuilder();

		result.append ("time = " + time_to_string (event_time));

		if (event_mag > -99.0 && event_mag < 99.0) {
			result.append (String.format(Locale.US, ", mag = %.3f", event_mag));
		} else {
			result.append (", mag = " + event_mag);
		}

		if (event_lat > -999.0 && event_lat < 999.0) {
			result.append (String.format(Locale.US, ", lat = %.5f", event_lat));
		} else {
			result.append (", lat = " + event_lat);
		}

		if (event_lon > -999.0 && event_lon < 999.0) {
			result.append (String.format(Locale.US, ", lon = %.5f", event_lon));
		} else {
			result.append (", lon = " + event_lon);
		}

		if (event_depth > -9999.0 && event_depth < 9999.0) {
			result.append (String.format(Locale.US, ", depth = %.3f", event_depth));
		} else {
			result.append (", depth = " + event_depth);
		}

		return result.toString();
	}




	// Given information about an event, produce a one-line summary.
	// This produces an event ID, followed by info in parentheses

	public static String event_id_and_info_one_line (String event_id, long event_time,
			double event_mag, double event_lat, double event_lon, double event_depth) {

		return event_id
				+ " ("
				+ event_info_one_line (event_time, event_mag, event_lat, event_lon, event_depth)
				+ ")";
	}




	// Create a new uniform random number generator.
	// The generator returns values uniformly distributed between 0 and 1.
	// If a test is in progress, the generator is seeded so that results are reproducible.

	public static UniformRealDistribution make_uniform_rangen () {
		UniformRealDistribution rangen = new UniformRealDistribution();

		long ranseed = TestMode.get_test_ranseed();
		if (ranseed > 0L) {
			rangen.reseedRandomGenerator (ranseed);
		}

		return rangen;
	}




	// Get the system time, in milliseconds since the epoch.
	// If a test is in progress, the return value is the test time.
	// Otherwise, the return value is System.currentTimeMillis().

	public static long get_system_time () {
		long the_time = TestMode.get_test_time();
		if (the_time <= 0L) {
			the_time = System.currentTimeMillis();
		}
		return the_time;
	}




//	// Convert a string in ISO-8601 format to time (in milliseconds after the epoch).
//	// An example is 2011-12-03T10:15:30Z.
//	// This function also accepts the following special values:
//	// "now" -- The current system time, or the test time if a test is in progress.
//	// A string of digit with optional minus sign -- The time obtained by converting to long.
//	// Throws an exception if the string cannot be parsed.
//
//	public static long special_string_to_time (String s) {
//		if (s.equals("now")) {
//			return get_system_time();
//		}
//		if (s.matches("-?\\d+")) {
//			return Long.parseLong (s);
//		}
//		return string_to_time(s);
//	}




	// Convert a string in ISO-8601 format to time (in milliseconds after the epoch).
	// An example is 2011-12-03T10:15:30Z.
	// This function also accepts the a string of digits with otional sign,
	// which it treats as a number of milliseconds since the epoch.
	// Throws an exception if the string cannot be parsed.

	public static long string_or_number_to_time (String s) {
		if (s.matches("[+-]?\\d+")) {
			return Long.parseLong(s);
		}
		return string_to_time(s);
	}




	// Convert a string in ISO-8601 format to time (in milliseconds after the epoch).
	// An example is 2011-12-03T10:15:30Z.
	// This function also accepts the a string of digits with otional sign,
	// which it treats as a number of milliseconds since the epoch.
	// This function also accepts "now" to return the current time acording to currentTimeMillis.
	// Throws an exception if the string cannot be parsed.

	public static long string_or_number_or_now_to_time (String s) {
		if (s.equals("now")) {
			return System.currentTimeMillis();
		}
		if (s.matches("[+-]?\\d+")) {
			return Long.parseLong(s);
		}
		return string_to_time(s);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("SimpleUtils : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  string
		// Test the operation of string_or_number_to_time.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("SimpleUtils : Invalid 'test1' subcommand");
				return;
			}

			try {

				String s = args[1];

				// Say hello

				System.out.println ("Converting time using string_or_number_to_time");
				System.out.println ("s = " + s);

				// Convert

				long time = string_or_number_to_time(s);

				// Display result

				System.out.println();
				System.out.println ("time = " + time_raw_and_string(time));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  string
		// Test the operation of string_or_number_or_now_to_time.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("SimpleUtils : Invalid 'test2' subcommand");
				return;
			}

			try {

				String s = args[1];

				// Say hello

				System.out.println ("Converting time using string_or_number_or_now_to_time");
				System.out.println ("s = " + s);

				// Convert

				long time = string_or_number_or_now_to_time(s);

				// Display result

				System.out.println();
				System.out.println ("time = " + time_raw_and_string(time));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("SimpleUtils : Unrecognized subcommand : " + args[0]);
		return;

	}




}
