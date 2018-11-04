package org.opensha.oaf.aftershockStatistics.util;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.time.Duration;


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
			result.append (String.format(", mag = %.3f", event_mag));
		} else {
			result.append (", mag = " + event_mag);
		}

		if (event_lat > -999.0 && event_lat < 999.0) {
			result.append (String.format(", lat = %.5f", event_lat));
		} else {
			result.append (", lat = " + event_lat);
		}

		if (event_lon > -999.0 && event_lon < 999.0) {
			result.append (String.format(", lon = %.5f", event_lon));
		} else {
			result.append (", lon = " + event_lon);
		}

		if (event_depth > -9999.0 && event_depth < 9999.0) {
			result.append (String.format(", depth = %.3f", event_depth));
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




}
