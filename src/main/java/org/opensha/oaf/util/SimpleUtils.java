package org.opensha.oaf.util;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.io.UnsupportedEncodingException;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

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

	// The number of milliseconds in a week.
	// Note: A week is defined to be 7 days.

	public static final long WEEK_MILLIS = 604800000L;
	public static final double WEEK_MILLIS_D = 604800000.0;

	// The number of milliseconds in a month.
	// Note: A month is defined to be 30 days.

	public static final long MONTH_MILLIS = 2592000000L;
	public static final double MONTH_MILLIS_D = 2592000000.0;

	// The number of milliseconds in a year.
	// Note: A year is defined to be 365 days.

	public static final long YEAR_MILLIS = 31536000000L;
	public static final double YEAR_MILLIS_D = 31536000000.0;




	// Convert milliseconds (long) to days (double).

	public static double millis_to_days (long millis) {
		return ((double)millis) / DAY_MILLIS_D;
	}

	// Convert days (double) to milliseconds (long).

	public static long days_to_millis (double days) {
		return Math.round (DAY_MILLIS_D * days);
	}




	// Clip x to lie between v1 and v2.

	public static double clip_val (double v1, double v2, double x) {
		return ( (v1 < v2) ? (Math.max(v1, Math.min(v2, x))) : (Math.max(v2, Math.min(v1, x))) );
	}

	public static float clip_val_f (float v1, float v2, float x) {
		return ( (v1 < v2) ? (Math.max(v1, Math.min(v2, x))) : (Math.max(v2, Math.min(v1, x))) );
	}

	public static int clip_val_i (int v1, int v2, int x) {
		return ( (v1 < v2) ? (Math.max(v1, Math.min(v2, x))) : (Math.max(v2, Math.min(v1, x))) );
	}

	public static long clip_val_l (long v1, long v2, long x) {
		return ( (v1 < v2) ? (Math.max(v1, Math.min(v2, x))) : (Math.max(v2, Math.min(v1, x))) );
	}


	// Clip x to lie between v1 and v2 assuming v1 <= v2; if v1 > v2 then the return value is v1.

	public static double clip_max_min (double v1, double v2, double x) {
		return Math.max(v1, Math.min(v2, x));
	}

	public static float clip_max_min_f (float v1, float v2, float x) {
		return Math.max(v1, Math.min(v2, x));
	}

	public static int clip_max_min_i (int v1, int v2, int x) {
		return Math.max(v1, Math.min(v2, x));
	}

	public static long clip_max_min_l (long v1, long v2, long x) {
		return Math.max(v1, Math.min(v2, x));
	}


	// Clip x to lie between v1 and v2 assuming v1 <= v2; if v1 > v2 then the return value is v2.

	public static double clip_min_max (double v1, double v2, double x) {
		return Math.min(v2, Math.max(v1, x));
	}

	public static float clip_min_max_f (float v1, float v2, float x) {
		return Math.min(v2, Math.max(v1, x));
	}

	public static int clip_min_max_i (int v1, int v2, int x) {
		return Math.min(v2, Math.max(v1, x));
	}

	public static long clip_min_max_l (long v1, long v2, long x) {
		return Math.min(v2, Math.max(v1, x));
	}




	// Get a stack trace as a string.

	public static String getStackTraceAsString (Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		e.printStackTrace(pw);
		return sw.getBuffer().toString();
	}




	// Convert a time (in milliseconds after the epoch) to a human-readable string.

	private static final TimeZone tz_utc = TimeZone.getTimeZone("UTC");

	private static final SimpleDateFormat time_to_string_fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss z");
	static {
		time_to_string_fmt.setTimeZone (tz_utc);
	}

	public static String time_to_string (long the_time) {
		//SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss z");
		//fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		return time_to_string_fmt.format (new Date (the_time));
	}




	// Convert a time (in milliseconds after the epoch) to a human-readable string.
	// This version does not have the "UTC" suffix (but the time is still UTC).

	private static final SimpleDateFormat time_to_string_no_z_fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
	static {
		time_to_string_no_z_fmt.setTimeZone (tz_utc);
	}

	public static String time_to_string_no_z (long the_time) {
		//SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
		//fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		return time_to_string_no_z_fmt.format (new Date (the_time));
	}




	// Convert a time (in milliseconds after the epoch) to a human-readable string.
	// This version does not have the seconds field or the "UTC" suffix (but the time is still UTC).

	private static final SimpleDateFormat time_to_string_no_sec_fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm");
	static {
		time_to_string_no_sec_fmt.setTimeZone (tz_utc);
	}

	public static String time_to_string_no_sec (long the_time) {
		return time_to_string_no_sec_fmt.format (new Date (the_time));
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




	// Convert a time (in milliseconds after the epoch) to a parseable string (ISO-8601 format).
	// The result can be understood by string_to_time().
	// If (and only if) the milliseconds are non-zero, then the seconds field includes
	// a decimal part with three decimal places.

	public static String time_to_parseable_string (long the_time) {
		//SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ssz");
		//fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		//return fmt.format (new Date (the_time));
		return Instant.ofEpochMilli(the_time).toString();
	}




	// Convert a time (in milliseconds after the epoch) to a parseable string (ISO-8601 format).
	// The result always has a 3-digit millisecond field.
	// The result can be understood by string_to_time().

	private static final SimpleDateFormat time_to_parseable_string_with_millis_fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static {
		time_to_parseable_string_with_millis_fmt.setTimeZone (tz_utc);
	}

	public static String time_to_parseable_string_with_millis (long the_time) {
		return time_to_parseable_string_with_millis_fmt.format (new Date (the_time));
	}




	// Convert a time (in milliseconds after the epoch) to a parseable string (ISO-8601 format).
	// The result never has a millisecond field.
	// The result can be understood by string_to_time().

	private static final SimpleDateFormat time_to_parseable_string_no_millis_fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	static {
		time_to_parseable_string_no_millis_fmt.setTimeZone (tz_utc);
	}

	public static String time_to_parseable_string_no_millis (long the_time) {
		return time_to_parseable_string_no_millis_fmt.format (new Date (the_time));
	}




	// Convert a string in ISO-8601 format to time (in milliseconds after the epoch).
	// An example is 2011-12-03T10:15:30Z.
	// The seconds field may include a decimal part to specify milliseconds.

	public static long string_to_time (String s) {
		return Instant.parse(s).toEpochMilli();
	}




	// Convert a string ito time (in milliseconds after the epoch).
	// This conversion is highly permissive in the time format.
	// Fields are specified in order: year month day hour minute second
	// - Month, day, hour, minute, and second can be one or two digits (year must be 4 digits).
	// - Seconds are optional.
	// - Seconds can optionally have a decimal part, consisting of period/comma followed by any number of digits.
	// - Parts of date can be separated by spaces/tabs, or by dash/slash/period/comma optionally surrounded by spaces/tabs.
	// - Parts of time can be separated by spaces/tabs, or by colon/period/comma optionally surrounded by spaces/tabs.
	// - Date and time can be optionally separated by "T" (case-insensitive), optionally surrounded by spaces/tabs.
	// - Time can optionally be followed by "Z" or "UTC" (case-insensitive), optionally preceded by spaces/tabs.
	// Note that ISO-8601 format is accepted.

	private static final Pattern sttp_pattern = Pattern.compile ("[\\x00-\\x20]*(\\d\\d\\d\\d)(?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[tT][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[:.,][ \\t]*)(\\d\\d?)(?:(?:[ \\t]+|[ \\t]*[:.,][ \\t]*)(\\d\\d?)(?:[.,](\\d*))?)?(?:[ \\t]*(?:[zZ]|[uU][tT][cC]))?[\\x00-\\x20]*");
	
	private static final int sttp_year_capture_group = 1;
	private static final int sttp_month_capture_group = 2;
	private static final int sttp_day_capture_group = 3;

	private static final int sttp_hour_capture_group = 4;
	private static final int sttp_minute_capture_group = 5;
	private static final int sttp_second_capture_group = 6;
	private static final int sttp_millis_capture_group = 7;

	public static long string_to_time_permissive (String s) {

		Matcher matcher = sttp_pattern.matcher (s);
		if (!( matcher.matches() )) {
			throw new IllegalArgumentException ("SimpleUtils.string_to_time_permissive: Invalid date/time format for string: " + s.trim());
		}

		// Get the parts of the string

		String year = matcher.group (sttp_year_capture_group);
		String month = matcher.group (sttp_month_capture_group);
		String day = matcher.group (sttp_day_capture_group);

		String hour = matcher.group (sttp_hour_capture_group);
		String minute = matcher.group (sttp_minute_capture_group);
		String second = matcher.group (sttp_second_capture_group);
		String millis = matcher.group (sttp_millis_capture_group);

		// Convert the date and time, allowing single-digit for all except year

		StringBuilder daytime = new StringBuilder();
		daytime.append (year);
		daytime.append ("-");
		if (month.length() == 1) {
			daytime.append ("0");
		}
		daytime.append (month);
		daytime.append ("-");
		if (day.length() == 1) {
			daytime.append ("0");
		}
		daytime.append (day);
		daytime.append ("T");
		if (hour.length() == 1) {
			daytime.append ("0");
		}
		daytime.append (hour);
		daytime.append (":");
		if (minute.length() == 1) {
			daytime.append ("0");
		}
		daytime.append (minute);

		daytime.append (":");
		if (second == null) {
			daytime.append ("00");
		} else {
			if (second.length() == 1) {
				daytime.append ("0");
			}
			daytime.append (second);
			if (millis != null) {
				daytime.append (".");
				switch (millis.length()) {
				case 0:
					daytime.append ("000");
					break;
				case 1:
					daytime.append (millis);
					daytime.append ("00");
					break;
				case 2:
					daytime.append (millis);
					daytime.append ("0");
					break;
				case 3:
					daytime.append (millis);
					break;
				default:
					daytime.append (millis.substring (0, 3));
					break;
				}
			}
		}

		daytime.append ("Z");

		long time;
		try {
			time = string_to_time (daytime.toString());
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException ("SimpleUtils.string_to_time_permissive: Date/time parse error parsing time for string: " + s.trim(), e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("SimpleUtils.string_to_time_permissive: Error parsing time for string: " + s.trim(), e);
		}

		return time;
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




	// Convert a duration (in milliseconds) to a human-readable string.
	// This version includes a "days" field for durations of 1 day or more.
	// This produces an easier-to-read form, not in java.time.Duration format.
	// Example: 7d13h4m.

	public static String duration_to_string_3 (long the_duration) {
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
				result = String.format ("%s%dd%dh%dm%d.%03ds", sign, days, hours, minutes, seconds, millis);
			} else if (seconds != 0) {
				result = String.format ("%s%dd%dh%dm%ds", sign, days, hours, minutes, seconds);
			} else if (minutes != 0) {
				result = String.format ("%s%dd%dh%dm", sign, days, hours, minutes);
			} else if (hours != 0) {
				result = String.format ("%s%dd%dh", sign, days, hours);
			} else {
				result = String.format ("%s%dd", sign, days);
			}
		}

		// Leading field is hours

		else if (hours != 0) {
			if (millis != 0) {
				result = String.format ("%s%dh%dm%d.%03ds", sign, hours, minutes, seconds, millis);
			} else if (seconds != 0) {
				result = String.format ("%s%dh%dm%ds", sign, hours, minutes, seconds);
			} else if (minutes != 0) {
				result = String.format ("%s%dh%dm", sign, hours, minutes);
			} else {
				result = String.format ("%s%dh", sign, hours);
			}
		}

		// Leading field is minutes

		else if (minutes != 0) {
			if (millis != 0) {
				result = String.format ("%s%dm%d.%03ds", sign, minutes, seconds, millis);
			} else if (seconds != 0) {
				result = String.format ("%s%dm%ds", sign, minutes, seconds);
			} else {
				result = String.format ("%s%dm", sign, minutes);
			}
		}

		// Leading field is seconds

		else {
			if (millis != 0) {
				result = String.format ("%s%d.%03ds", sign, seconds, millis);
			} else {
				result = String.format ("%s%ds", sign, seconds);
			}
		}

		return result;
	}




	// Given a duration (in milliseconds), produce a string which
	// is its numerical value followed by the human-readable form in parentheses.
	// This version includes a "days" field for durations of 1 day or more.
	// This produces an easier-to-read form, not in java.time.Duration format.

	public static String duration_raw_and_string_3 (long the_duration) {
		return the_duration + " (" + duration_to_string_3(the_duration) + ")";
	}




	// Convert a numan-readable string (example: 7d13h4m) to a duration in milliseconds.
	// Parameters:
	//  s = String to convert.
	//  def_unit = A string that gives default units; can be null or blank if none.
	// The string to convert consists of a uptional sign, followed by optional
	// substrings of "nnd", "nnh", "nnm", and "nns" (in that order) which given
	// durations in days, hours, minutes, and seconds.  Each "nn" can be An
	// unsigned integer or fixed-point number.  The unit characters (d,h,m,s)
	// can be lowercase or uppercase.
	// If def_unit is non-null and non-blank, then it must be a unit character
	// (lowercase or uppercase d,h,m,s).  The the string contains no units at all,
	// then it is interpreted as if it were terminated by def_unit.
	// If def_unit is null or blank, then it is an error for the string to not
	// contain a units character.

	// Pattern for a string with no unit.

	//private static final Pattern stodur_nounit_pattern = Pattern.compile ("([+-])?  ( (?:\\d+(?:\\.\\d*)?) | (?:\\.\\d+) )");
	private static final Pattern stodur_nounit_pattern = Pattern.compile ("([+-])?((?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))");

	// Pattern for a string with units.

	//private static final Pattern stodur_unit_pattern = Pattern.compile ("([+-])?  (?:( (?:\\d+(?:\\.\\d*)?) | (?:\\.\\d+) )[dD])?  (?:( (?:\\d+(?:\\.\\d*)?) | (?:\\.\\d+) )[hH])?  (?:( (?:\\d+(?:\\.\\d*)?) | (?:\\.\\d+) )[mM])?  (?:( (?:\\d+(?:\\.\\d*)?) | (?:\\.\\d+) )[sS])?");
	private static final Pattern stodur_unit_pattern = Pattern.compile ("([+-])?(?:((?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))[dD])?(?:((?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))[hH])?(?:((?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))[mM])?(?:((?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))[sS])?");

	public static long string_to_duration_3 (String s, String def_unit) {
		String s_trim = s.trim();

		// Get the number of milliseconds per default unit, or -1.0 if no default unit

		double def_unit_millis = -1.0;

		if (def_unit != null) {
			String def_unit_trim = def_unit.trim();
			if (def_unit_trim.length() > 0) {
				switch (def_unit.trim()) {
					case "d": case "D": def_unit_millis = DAY_MILLIS_D; break;
					case "h": case "H": def_unit_millis = HOUR_MILLIS_D; break;
					case "m": case "M": def_unit_millis = MINUTE_MILLIS_D; break;
					case "s": case "S": def_unit_millis = SECOND_MILLIS_D; break;
					default:
						throw new IllegalArgumentException ("SimpleUtils.string_to_duration_3: Invalid def_unit = " + def_unit);
				}
			}
		}

		// Any exceptions here indicate invalid string

		try {

			// If we match a string with units ...

			Matcher matcher = stodur_unit_pattern.matcher (s_trim);
			if (matcher.matches()) {

				// Get the positive duration in floating point

				boolean f_got_value = false;
				double d_dur = 0.0;

				String g = matcher.group(2);
				if (g != null) {
					f_got_value = true;
					d_dur += (Double.parseDouble(g) * DAY_MILLIS_D);
				}

				g = matcher.group(3);
				if (g != null) {
					f_got_value = true;
					d_dur += (Double.parseDouble(g) * HOUR_MILLIS_D);
				}

				g = matcher.group(4);
				if (g != null) {
					f_got_value = true;
					d_dur += (Double.parseDouble(g) * MINUTE_MILLIS_D);
				}

				g = matcher.group(5);
				if (g != null) {
					f_got_value = true;
					d_dur += (Double.parseDouble(g) * SECOND_MILLIS_D);
				}

				// If we got a value ...

				if (f_got_value) {

					// Convert to integer milliseconds

					long dur = Math.round (d_dur);

					// Apply sign

					g = matcher.group(1);
					if (g != null) {
						if (g.equals ("-")) {
							dur = -dur;
						}
					}

					return dur;
				}
			}

			// Otherwise, if we have a default unit ...

			else if (def_unit_millis > 0.0) {

				// If we match a string without units ...

				matcher = stodur_nounit_pattern.matcher (s_trim);
				if (matcher.matches()) {

					// Get the positive duration in floating point

					boolean f_got_value = false;
					double d_dur = 0.0;

					String g = matcher.group(2);
					if (g != null) {
						f_got_value = true;
						d_dur += (Double.parseDouble(g) * def_unit_millis);
					}

					// If we got a value ...

					if (f_got_value) {

						// Convert to integer milliseconds

						long dur = Math.round (d_dur);

						// Apply sign

						g = matcher.group(1);
						if (g != null) {
							if (g.equals ("-")) {
								dur = -dur;
							}
						}

						return dur;
					}
				}
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException ("SimpleUtil.string_to_duration_3: Invalid string = " + s, e);
		}

		throw new IllegalArgumentException ("SimpleUtil.string_to_duration_3: Invalid string = " + s);
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




	// Convert a double to a string.
	// Parameters:
	//  fmt = Format specification, as for String.format; null for Java default.
	//  x = Double to convert.
	// This function uses the US locale so that the output can be read back in.

	public static String double_to_string (String fmt, double x) {
		if (fmt == null) {
			return Double.toString (x);
		}
		return String.format (Locale.US, fmt, x);
	}




	// Pattern used to find trailing zeros.
	// This recognizes strings that have the form of a fixed-point or floating-point
	// number, possibly with leading or trailing spaces, with trailing zeros in the
	// decimal part of the number.  If there is a match, capture group 1 contains
	// the trailing zeros.  A zero appearing immediately after the decimal point
	// is not considered to be trailing.
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern trailz_pattern = Pattern.compile (" *[+-]?\\d*\\.\\d(?:\\d*[1-9])?(0+)(?:[eE][+-]?\\d+)? *");

	// The capture group for the trailing zeros

	private static final int trailz_capture_group = 1;




	// Constants for selecting options for trailing zeros.

	public static final int TRAILZ_OK = 0;			// OK to have trailing zeros.
	public static final int TRAILZ_REMOVE = 1;		// Remove trailing zeros.
	public static final int TRAILZ_PAD_LEFT = 2;	// Remove trailing zeros and pad with spaces on left.
	public static final int TRAILZ_PAD_RIGHT = 3;	// Remove trailing zeros and pad with spaces on right.




	// Return a string describing a trailing zero option.

	public static String trailz_to_string (int trailz) {
		switch (trailz) {
			case TRAILZ_OK: return "TRAILZ_OK";
			case TRAILZ_REMOVE: return "TRAILZ_REMOVE";
			case TRAILZ_PAD_LEFT: return "TRAILZ_PAD_LEFT";
			case TRAILZ_PAD_RIGHT: return "TRAILZ_PAD_RIGHT";
		}
		return "TRAILZ_INVALID(" + trailz + ")";
	}




	// Remove trailing zeros from the decimal part of a number.
	// Parameters:
	//  s = Number, in the form of a fixed-point or floating-point number.
	//  trailz = Trailing-zero option, TRAILZ_XXXX above.
	// This function returns s if s does not have the expected form, or if
	// there are no trailing zeros to remove.

	public static String remove_trailing_zeros (String s, int trailz) {

		// If not removing trailing zeros, just return

		if (trailz == TRAILZ_OK) {
			return s;
		}

		// Try to match the pattern

		Matcher matcher = trailz_pattern.matcher (s);
		if (!( matcher.matches() )) {
			return s;
		}

		// Start and end offsets of the trailing zeros

		int start = matcher.start (trailz_capture_group);
		int end = matcher.end (trailz_capture_group);

		// String builder to accumulate the result

		StringBuilder builder = new StringBuilder();

		// Switch on trailing zero option

		switch (trailz) {

		// Remove trailing zeros

		case TRAILZ_REMOVE:
			builder.append (s.substring(0, start));
			builder.append (s.substring(end));
			break;

		// Remove trailing zeros and pad on left

		case TRAILZ_PAD_LEFT:
			for (int i = start; i < end; ++i) {
				builder.append (" ");
			}
			builder.append (s.substring(0, start));
			builder.append (s.substring(end));
			break;

		// Remove trailing zeros and pad on right

		case TRAILZ_PAD_RIGHT:
			builder.append (s.substring(0, start));
			builder.append (s.substring(end));
			for (int i = start; i < end; ++i) {
				builder.append (" ");
			}
			break;

		// Invalid option

		default:
			throw new IllegalArgumentException ("SimpleUtils.remove_trailing_zeros - Invalid trailing zero option, trailz = " + trailz);
		}

		// Return result

		return builder.toString();
	}




	// Convert double to string, and remove trailing zeros.
	// Parameters:
	//  fmt = Format specification, as for String.format; null for Java default.
	//  trailz = Trailing-zero option, TRAILZ_XXXX above.
	//  x = Double to convert.

	public static String double_to_string_trailz (String fmt, int trailz, double x) {
		return remove_trailing_zeros (double_to_string (fmt, x), trailz);
	}




	// Round a double by converting to a string and back.
	// Parameters:
	//  fmt = Format specification, as for String.format; null for Java default.
	//  x = Double to convert.
	// This function uses the US locale so that the string is parseable.

	public static double round_double_via_string (String fmt, double x) {
		return Double.parseDouble (double_to_string (fmt, x));
	}




	// Function to format floating-point values for display during testing.
	// This function returns a string, with 12 significant digits, in either
	// fixed or floating point depending on the value, with no trailing zeros.

	public static String rndd (double x) {
		return double_to_string_trailz ("%.12G", TRAILZ_REMOVE, x);
	}




	// Function to format floating-point values for display during testing.
	// This function returns a string, with 6 significant digits, in either
	// fixed or floating point depending on the value, with no trailing zeros.

	public static String rndf (double x) {
		return double_to_string_trailz ("%.6G", TRAILZ_REMOVE, x);
	}




	// Coerce a value to lie within given limits.
	// Parameters:
	//  x = Value to check.
	//  min_value = Minimum allowed value.
	//  min_coerce = Minimum value that is coerced to min_value, must satisfy min_coerce <= min_value.
	//  max_value = Maximum allowed value.
	//  max_coerce = Maximum value that is coerced to max_value, must satisfy max_coerce >= max_value.
	//  min_tiny = Minimum value that is coerced to zero, must satisfy min_tiny <= 0.0.
	//  max_tiny = Maximum value that is coerced to zero, must satisfy max_tiny >= 0.0.
	// Returns the coerced value of x.
	// Throw an exception if:  x < min_coerce  or  x > max_coerce.
	// Coerce x to min_value if:  min_coerce <= x < min_value.
	// Coerce x to max_value if:  max_value < x <= max_coerce.
	// Coerce x to zero if:  min_tiny < x < max_tiny.

	public static double coerce_value (double x, double min_value, double min_coerce, double max_value, double max_coerce, double min_tiny, double max_tiny) {
		if (x < min_value) {
			if (x < min_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_value: Value too small: x = " + x + ", min_value = " + min_value + ", min_coerce = " + min_coerce);
			}
			return min_value;
		}
		if (x > max_value) {
			if (x > max_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_value: Value too large: x = " + x + ", max_value = " + max_value + ", max_coerce = " + max_coerce);
			}
			return max_value;
		}
		if (x > min_tiny && x < max_tiny) {
			return 0.0;
		}
		return x;
	}




	// Coerce a value to lie within given limits.
	// Parameters:
	//  x = Value to check.
	//  min_value = Minimum allowed value.
	//  min_coerce = Minimum value that is coerced to min_value, must satisfy min_coerce <= min_value.
	//  max_value = Maximum allowed value.
	//  max_coerce = Maximum value that is coerced to max_value, must satisfy max_coerce >= max_value.
	// Returns the coerced value of x.
	// Throw an exception if:  x < min_coerce  or  x > max_coerce.
	// Coerce x to min_value if:  min_coerce <= x < min_value.
	// Coerce x to max_value if:  max_value < x <= max_coerce.

	public static double coerce_value (double x, double min_value, double min_coerce, double max_value, double max_coerce) {
		if (x < min_value) {
			if (x < min_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_value: Value too small: x = " + x + ", min_value = " + min_value + ", min_coerce = " + min_coerce);
			}
			return min_value;
		}
		if (x > max_value) {
			if (x > max_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_value: Value too large: x = " + x + ", max_value = " + max_value + ", max_coerce = " + max_coerce);
			}
			return max_value;
		}
		return x;
	}




	// Coerce and wrap a value to lie within given limits.
	// Parameters:
	//  x = Value to check.
	//  wrap_lo = Low end of wrap range.
	//  wrap_hi = High end of wrap range.
	//  min_value = Minimum allowed value, must satisfy  min_value >= wrap_lo - (wrap_hi - wrap_lo).
	//  min_coerce = Minimum value that is coerced to min_value, must satisfy min_coerce <= min_value.
	//  max_value = Maximum allowed value, must satisfy  max_value <= wrap_hi + (wrap_hi - wrap_lo).
	//  max_coerce = Maximum value that is coerced to max_value, must satisfy max_coerce >= max_value.
	//  min_tiny = Minimum value that is coerced to zero, must satisfy min_tiny <= 0.0.
	//  max_tiny = Maximum value that is coerced to zero, must satisfy max_tiny >= 0.0.
	// Returns the coerced value of x.
	// Throw an exception if:  x < min_coerce  or  x > max_coerce.
	// Coerce x to min_value if:  min_coerce <= x < min_value.
	// Coerce x to max_value if:  max_value < x <= max_coerce.
	// Then:  if x > wrap_hi then subtract wrap_hi - wrap_lo;  if x < wrap_lo then add wrap_hi - wrap_lo.
	// Finally:  coerce x to zero if:  min_tiny < x < max_tiny.

	public static double coerce_wrap_value (double x, double wrap_lo, double wrap_hi, double min_value, double min_coerce, double max_value, double max_coerce, double min_tiny, double max_tiny) {
		double y = x;
		if (x < min_value) {
			if (x < min_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_wrap_value: Value too small: x = " + x + ", min_value = " + min_value + ", min_coerce = " + min_coerce);
			}
			y = min_value;
		}
		else if (x > max_value) {
			if (x > max_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_wrap_value: Value too large: x = " + x + ", max_value = " + max_value + ", max_coerce = " + max_coerce);
			}
			y = max_value;
		}
		if (y > wrap_hi) {
			y = (y - wrap_hi) + wrap_lo;
		}
		else if (y < wrap_lo) {
			y = (y - wrap_lo) + wrap_hi;
		}
		if (y > min_tiny && y < max_tiny) {
			y = 0.0;
		}
		return y;
	}




	// Coerce and wrap a value to lie within given limits.
	// Parameters:
	//  x = Value to check.
	//  wrap_lo = Low end of wrap range.
	//  wrap_hi = High end of wrap range.
	//  min_value = Minimum allowed value, must satisfy  min_value >= wrap_lo - (wrap_hi - wrap_lo).
	//  min_coerce = Minimum value that is coerced to min_value, must satisfy min_coerce <= min_value.
	//  max_value = Maximum allowed value, must satisfy  max_value <= wrap_hi + (wrap_hi - wrap_lo).
	//  max_coerce = Maximum value that is coerced to max_value, must satisfy max_coerce >= max_value.
	// Returns the coerced value of x.
	// Throw an exception if:  x < min_coerce  or  x > max_coerce.
	// Coerce x to min_value if:  min_coerce <= x < min_value.
	// Coerce x to max_value if:  max_value < x <= max_coerce.
	// Then:  if x > wrap_hi then subtract wrap_hi - wrap_lo;  if x < wrap_lo then add wrap_hi - wrap_lo.

	public static double coerce_wrap_value (double x, double wrap_lo, double wrap_hi, double min_value, double min_coerce, double max_value, double max_coerce) {
		double y = x;
		if (x < min_value) {
			if (x < min_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_wrap_value: Value too small: x = " + x + ", min_value = " + min_value + ", min_coerce = " + min_coerce);
			}
			y = min_value;
		}
		else if (x > max_value) {
			if (x > max_coerce) {
				throw new IllegalArgumentException ("SimpleUtils.coerce_wrap_value: Value too large: x = " + x + ", max_value = " + max_value + ", max_coerce = " + max_coerce);
			}
			y = max_value;
		}
		if (y > wrap_hi) {
			y = (y - wrap_hi) + wrap_lo;
		}
		else if (y < wrap_lo) {
			y = (y - wrap_lo) + wrap_hi;
		}
		return y;
	}




	// Coerce a tiny value to be exactly zero.
	// Parameters:
	//  x = Value to check.
	//  min_tiny = Minimum value that is coerced to zero, must satisfy min_tiny <= 0.0.
	//  max_tiny = Maximum value that is coerced to zero, must satisfy max_tiny >= 0.0.
	// Returns the coerced value of x.
	// Coerce x to zero if:  min_tiny < x < max_tiny.

	public static double coerce_tiny_to_zero (double x, double min_tiny, double max_tiny) {
		if (x > min_tiny && x < max_tiny) {
			return 0.0;
		}
		return x;
	}




	// Clip a string, to a maximum of the given number of characters.
	// If the string is clipped, then an ellipsis is appended.

	public static String clip_string (String s, int max_chars) {
		if (s.length() > max_chars) {
			return s.substring (0, max_chars) + "...";
		}
		return s;
	}




	// URL encode a string.
	// Parameters:
	//  s = String to encode.
	//  f_except = true to throw exception if error, false to return null if error.
	// Returns the encoded string.

	public static String url_encode (String s, boolean f_except) {
		String result = null;
		try {
			result = URLEncoder.encode (s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			if (f_except) {
				throw new IllegalArgumentException ("SimpleUtils.url_encode: Invalid URL encoding: s = " + clip_string (s, 100), e);
			}
			result = null;
		} catch (Exception e) {
			if (f_except) {
				throw new IllegalArgumentException ("SimpleUtils.url_encode: Error during URL encoding: s = " + clip_string (s, 100), e);
			}
			result = null;
		}
		return result;
	}




	// URL decode a string.
	// Parameters:
	//  s = String to decode.
	//  f_except = true to throw exception if error, false to return null if error.
	// Returns the decoded string.

	public static String url_decode (String s, boolean f_except) {
		String result = null;
		try {
			result = URLDecoder.decode (s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			if (f_except) {
				throw new IllegalArgumentException ("SimpleUtils.url_decode: Invalid URL encoding: s = " + clip_string (s, 100), e);
			}
			result = null;
		} catch (Exception e) {
			if (f_except) {
				throw new IllegalArgumentException ("SimpleUtils.url_decode: Error during URL decoding: s = " + clip_string (s, 100), e);
			}
			result = null;
		}
		return result;
	}




	// Pattern used to normalize spaces.
	// This recognizes any sequence of white space characters (as defined by regex \s), except for a single space.
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern normsp_s_pattern = Pattern.compile (" \\s+|[\\t\\n\\x0B\\f\\r]\\s*");

	// Pattern used to normalize spaces.
	// This recognizes any sequence of white space characters (as defined by String.trim), except for a single space.
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern normsp_trim_pattern = Pattern.compile (" [\\x00-\\x20]+|[\\x00-\\x1F][\\x00-\\x20]*");

	// Pattern used to recognize white space.
	// This recognizes any sequence of white space characters (as defined by regex \s).
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern whitesp_s_pattern = Pattern.compile ("\\s+");

	// Pattern used to recognize white space.
	// This recognizes any sequence of white space characters (as defined by String.trim).
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern whitesp_trim_pattern = Pattern.compile ("[\\x00-\\x20]+");

	// Pattern used to trim trailing white space.
	// This recognizes a string that has trailing white space (as defined by String.trim).
	// If a match, then capture group 1 is the string with white space removed.
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern trailsp_trim_pattern = Pattern.compile ("((?:.*[^\\x00-\\x20])?)[\\x00-\\x20]+");




	// Trim leading and trailing whitespace, and replace any internal whitespace with a single space.
	// If s is null, return null.
	// Whitespace is defined as for String.trim.

	public static String trim_and_normalize (String s) {
		if (s == null) {
			return null;
		}
		return normsp_trim_pattern.matcher(s.trim()).replaceAll(" ");
	}




	// Split a string into words separated by white space, as defined for regex \s.
	// Note: If the string contains leading white space, the first word is empty.

	public static String[] split_around_s (String s) {
		return whitesp_s_pattern.split(s);
	}




	// Split a string into words separated by white space, as defined for String.trim.
	// Note: If the string contains leading white space, the first word is empty.
	// Note: This produces words that would be unchanged by String.trim.

	public static String[] split_around_trim (String s) {
		return whitesp_trim_pattern.split(s);
	}




	// Remove trailing white spaces from a string, as defined for String.trim.
	// Note that leading white space is retained.

	public static String trim_trailing (String s) {
		Matcher matcher = trailsp_trim_pattern.matcher (s);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return s;
	}




	// Prepare a string for display to the user.
	// This is intended for strings free of control characters other than tab.
	// If the string contains any spaces, tabs, or quotes, or is empty, then
	// the string is enclosed in quotes and any tabs and quotes are escaped.

	public static String disp_string_for_user (String s) {
		if (s == null) {
			return "<null>";
		}
		if (s.isEmpty() || s.matches (".*[ \\t\"].*")) {
			return "\"" + s.replaceAll ("\t", "\\\\t").replaceAll ("\"", "\\\\\"") + "\"";
		}
		return s;
	}




	// Prepare a date format for display to the user.

	public static String disp_date_format_for_user (SimpleDateFormat df) {
		return ((df == null) ? "<null>" : disp_string_for_user (df.toPattern()));
	}




	// Create a string containing memory usage status.
	// The resulting string has multiple lines, each terminated by linefeed.

	public static String memory_status_string () {
		StringBuilder sb = new StringBuilder();
		long max_memory = Runtime.getRuntime().maxMemory();
		long total_memory = Runtime.getRuntime().totalMemory();
		long free_memory = Runtime.getRuntime().freeMemory();

		long used_memory = total_memory - free_memory;

		if (max_memory == Long.MAX_VALUE) {
			sb.append ("max_memory = unlimited\n");
		} else {
			sb.append ("max_memory = " + (max_memory / 1048576L) + " M\n");
		}
			
		sb.append ("total_memory = " + (total_memory / 1048576L) + " M\n");
		sb.append ("free_memory = " + (free_memory / 1048576L) + " M\n");
		sb.append ("used_memory = " + (used_memory / 1048576L) + " M\n");

		return sb.toString();
	}




	// Show the current memory status on standard output.
	// If a test is in progress, do nothing because memory usage is not repeatable.

	public static void show_memory_status () {
		long the_time = TestMode.get_test_time();
		if (the_time <= 0L) {
			long max_memory = Runtime.getRuntime().maxMemory();
			long total_memory = Runtime.getRuntime().totalMemory();
			long free_memory = Runtime.getRuntime().freeMemory();

			long used_memory = total_memory - free_memory;

			if (max_memory == Long.MAX_VALUE) {
				System.out.println ("max_memory = unlimited");
			} else {
				System.out.println ("max_memory = " + (max_memory / 1048576L) + " M");
			}
			
			System.out.println ("total_memory = " + (total_memory / 1048576L) + " M");
			System.out.println ("free_memory = " + (free_memory / 1048576L) + " M");
			System.out.println ("used_memory = " + (used_memory / 1048576L) + " M");
		}
		return;
	}




	// Create a one-line string containing memory usage status.
	// The resulting string has a single line, not terminated by linefeed.

	public static String one_line_memory_status_string () {
		StringBuilder sb = new StringBuilder();
		long max_memory = Runtime.getRuntime().maxMemory();
		long total_memory = Runtime.getRuntime().totalMemory();
		long free_memory = Runtime.getRuntime().freeMemory();

		long used_memory = total_memory - free_memory;

		if (max_memory == Long.MAX_VALUE) {
			sb.append ("Memory: max = unlimited");
		} else {
			sb.append ("Memory: max = " + (max_memory / 1048576L) + " M");
		}
			
		sb.append (", total = " + (total_memory / 1048576L) + " M");
		sb.append (", free = " + (free_memory / 1048576L) + " M");
		sb.append (", used = " + (used_memory / 1048576L) + " M");

		return sb.toString();
	}




	// Show one-line memory status on standard output.
	// If a test is in progress, do nothing because memory usage is not repeatable.

	public static void show_one_line_memory_status () {
		long the_time = TestMode.get_test_time();
		if (the_time <= 0L) {
			System.out.println (one_line_memory_status_string());
		}
		return;
	}




	// Create a containing the amount of used memory.
	// The resulting string contains just a numerical value and unit, and is not terminated by linefeed.

	public static String used_memory_string () {
		long total_memory = Runtime.getRuntime().totalMemory();
		long free_memory = Runtime.getRuntime().freeMemory();

		long used_memory = total_memory - free_memory;

		return (used_memory / 1048576L) + " M";
	}




	// Get the amount of used memory, in bytes.

	public static long get_used_memory_bytes () {
		long total_memory = Runtime.getRuntime().totalMemory();
		long free_memory = Runtime.getRuntime().freeMemory();

		long used_memory = total_memory - free_memory;

		return used_memory;
	}




	// Convert an amount of used memory, in bytes, to a string.
	// The resulting string contains just a numerical value and unit, and is not terminated by linefeed.
	// Note: Calling get_used_memory_bytes() followed by used_memory_to_string()
	// performs the same operation as used_memory_string().

	public static String used_memory_to_string (long used_memory) {
		return (used_memory / 1048576L) + " M";
	}




	// Write one or more strings as a file.

	public static void write_string_as_file (String filename, String... s) throws IOException {
		try (
			BufferedWriter buf = new BufferedWriter (new FileWriter (filename));
		) {
			for (int j = 0; j < s.length; ++j) {
				buf.write (s[j]);
			}
		}
		return;
	}




	// Write an array of bytes as a binary file.

	public static void write_bytes_as_file (String filename, byte[] b) throws IOException {
		try (
			BufferedOutputStream buf = new BufferedOutputStream (new FileOutputStream (filename));
		) {
			buf.write (b);
		}
		return;
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

				System.out.println();
				System.out.println ("parseable time = " + time_to_parseable_string(time));

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

				System.out.println();
				System.out.println ("parseable time = " + time_to_parseable_string(time));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  string  trailz
		// Test the operation of remove_trailing_zeros.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 2 additional arguments

			if (!( args.length == 3 )) {
				System.err.println ("SimpleUtils : Invalid 'test3' subcommand");
				return;
			}

			try {

				String s = args[1];
				int trailz = Integer.parseInt(args[2]);

				// Say hello

				System.out.println ("Removing trailing zeros using remove_trailing_zeros");
				System.out.println ("s = \"" + s + "\"");
				System.out.println ("trailz = " + trailz);

				// Remove trailing zeros

				String result = remove_trailing_zeros (s, trailz);

				// Display result

				System.out.println();
				System.out.println ("result = \"" + result + "\"");

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  string
		// Test the operation of url_encode and url_decode.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 1 additional argument

			if (!( args.length == 2 )) {
				System.err.println ("SimpleUtils : Invalid 'test4' subcommand");
				return;
			}

			try {

				String s = args[1];

				// Say hello

				System.out.println ("Test URL encoding and decoding");
				System.out.println ("s = \"" + s + "\"");

				// URL encode

				String s2 = url_encode (s, true);

				System.out.println();
				System.out.println ("encoded = \"" + s2 + "\"");

				// URL decode

				String s3 = url_decode (s2, true);

				System.out.println();
				System.out.println ("decoded = \"" + s3 + "\"");

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Test the operation of url_encode and url_decode.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("SimpleUtils : Invalid 'test5' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Test memory status functions");

				// Memory status

				System.out.println();
				System.out.println ("Memory status:");
				System.out.println (memory_status_string());

				// One-line memory status

				System.out.println ("One-line memory status:");
				System.out.println (one_line_memory_status_string());

				// Used memory

				System.out.println();
				System.out.println ("Used memory:");
				System.out.println (used_memory_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  filename
		// Test the operation of write_string_as_file.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("SimpleUtils : Invalid 'test6' subcommand");
				return;
			}

			try {

				String filename = args[1];

				// Say hello

				System.out.println ("Testing write_string_as_file");
				System.out.println ("filename = " + filename);

				// Make some test strings

				String s1 =
					  "a0 a1 a2 a3 a4 a5 a6 a7 a8 a9\n"
					+ "bcd0 bcd1 bcd2 bcd3 bcd4 bcd5 bcd6 bcd7 bcd8 bcd9\n"
					+ "ef0 ef1 ef2 ef3 ef4 ef5 ef6 ef7 ef8 ef9\n"
					+ "g0 g1 g2 g3 g4 g5 g6 g7 g8 g9\n"
					+ "hij0 hij1 hij2 hij3 hij4 hij5 hij6 hij7 hij8 hij9\n"
					+ "k0 k1 k2 k3 k4 k5 k6 k7 k8 k9\n"
				;

				String s2 =
					  "lmn0 lmn1 lmn2 lmn3 lmn4 lmn5 lmn6 lmn7 lmn8 lmn9\n"
					+ "op0 op1 op2 op3 op4 op5 op6 op7 op8 op9\n"
					+ "qr0 qr1 qr2 qr3 qr4 qr5 qr6 qr7 qr8 qr9\n"
					+ "s0 s1 s2 s3 s4 s5 s6 s7 s8 s9\n"
				;

				// Write it

				write_string_as_file (filename, s1, s2);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  string  def_unit
		// Test the operation of string_to_duration_3.
		// The value of def_unit can be "null" or "empty" to produce a null or empty string.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 2 additional arguments

			if (!( args.length == 3 )) {
				System.err.println ("SimpleUtils : Invalid 'test7' subcommand");
				return;
			}

			try {

				String s = args[1];
				String def_unit = args[2];

				// Say hello

				System.out.println ("Removing trailing zeros using remove_trailing_zeros");
				System.out.println ("s = \"" + s + "\"");
				System.out.println ("def_unit = " + def_unit);

				if (def_unit.equals ("null")) {
					def_unit = null;
				}
				else if (def_unit.equals ("empty")) {
					def_unit = "";
				}

				// Convert to duration in milliseconds

				long duration_millis = string_to_duration_3 (s, def_unit);

				// Display result

				System.out.println();
				System.out.println ("duration_millis = " + duration_millis);

				System.out.println();
				System.out.println ("duration_to_string_2 = " + duration_to_string_2 (duration_millis));

				System.out.println();
				System.out.println ("duration_to_string_3 = " + duration_to_string_3 (duration_millis));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  string
		// Test the operation of string_to_time and varios formatting functions.

		if (args[0].equalsIgnoreCase ("test8")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("SimpleUtils : Invalid 'test8' subcommand");
				return;
			}

			try {

				String s = args[1];

				// Say hello

				System.out.println ("Converting time using string_to_time");
				System.out.println ("s = " + s);

				// Convert

				long time = string_to_time(s);

				// Display result

				System.out.println();
				System.out.println ("time = " + time);

				System.out.println();
				System.out.println ("time_to_string = " + time_to_string(time));

				System.out.println();
				System.out.println ("time_to_string_no_z = " + time_to_string_no_z(time));

				System.out.println();
				System.out.println ("time_raw_and_string = " + time_raw_and_string(time));

				System.out.println();
				System.out.println ("time_to_parseable_string = " + time_to_parseable_string(time));

				System.out.println();
				System.out.println ("time_to_parseable_string_with_millis = " + time_to_parseable_string_with_millis(time));

				System.out.println();
				System.out.println ("time_to_parseable_string_no_millis = " + time_to_parseable_string_no_millis(time));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  string
		// Test the operation of string_to_time_permissive and varios formatting functions.

		if (args[0].equalsIgnoreCase ("test9")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("SimpleUtils : Invalid 'test9' subcommand");
				return;
			}

			try {

				String s = args[1];

				// Say hello

				System.out.println ("Converting time using string_to_time_permissive");
				System.out.println ("s = " + s);

				// Convert

				long time = string_to_time_permissive(s);

				// Display result

				System.out.println();
				System.out.println ("time = " + time);

				System.out.println();
				System.out.println ("time_to_string = " + time_to_string(time));

				System.out.println();
				System.out.println ("time_to_string_no_z = " + time_to_string_no_z(time));

				System.out.println();
				System.out.println ("time_raw_and_string = " + time_raw_and_string(time));

				System.out.println();
				System.out.println ("time_to_parseable_string = " + time_to_parseable_string(time));

				System.out.println();
				System.out.println ("time_to_parseable_string_with_millis = " + time_to_parseable_string_with_millis(time));

				System.out.println();
				System.out.println ("time_to_parseable_string_no_millis = " + time_to_parseable_string_no_millis(time));

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
