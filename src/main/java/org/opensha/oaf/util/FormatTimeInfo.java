package org.opensha.oaf.util;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;


/**
 * Holds information for formatting a time, including options for ISO-8601 and numeric.
 * Author: Michael Barall 10/09/2021.
 */
public class FormatTimeInfo {

	// The time formatter, or null if not used.
	// If null, and f_numeric is false, then use ISO-8601.

	public SimpleDateFormat date_fmt;

	// Flag, true to format as numeric (milliseconds since the epoch).

	public boolean f_numeric;

	// Flag, true to format as ISO-8601.

	public boolean f_iso_8601;

	// Number of words in the formatted time.

	public int field_words;

	// Field width, or 0 if width is variable or unspecified.

	public int field_width;




	// Clear to ISO-8601.

	public void clear () {
		date_fmt = null;
		f_numeric = false;
		f_iso_8601 = true;
		field_words = 1;
		field_width = 20;
		return;
	}

	// Construct with ISO-8601.

	public FormatTimeInfo () {
		clear();
	}




	// Set format info.
	// Parameters:
	//  form = Output form:
	//    "u" = Numeric output, milliseconds since the epoch (Unix format).
	//    "z" = ISO-8601 format.
	//    Any other value = Format as for SimpleDateFormat.

	public void set (String form) {

		// If format as numeric ...

		if (form.equals ("u")) {
			date_fmt = null;
			f_numeric = true;
			f_iso_8601 = false;
			field_words = 1;
			field_width = 0;
		}

		// Otherwise, if format as ISO-8601 ...

		else if (form.equals ("z")) {
			date_fmt = null;
			f_numeric = false;
			f_iso_8601 = true;
			field_words = 1;
			field_width = 20;
		}

		// Otherwise, use SimpleDateFormat ...

		else {

			// Set up the formatter

			try {
				date_fmt = new SimpleDateFormat (form);
				date_fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
			} catch (Exception e) {
				throw new IllegalArgumentException ("FormatTimeInfo.set - Invalid date/time format: form = " + form);
			}

			f_numeric = false;
			f_iso_8601 = false;

			// Do a format to get likely field width and number of words

			long x = 0L;
			String s = date_fmt.format (new Date (x));

			field_width = s.length();
			field_words = SimpleUtils.split_around_trim(s.trim()).length;
		} 

		return;
	}

	// Construct and set format info.

	public FormatTimeInfo (String form) {
		set (form);
	}




	// Format a time using this information.

	public String format_time (long x) {
		String s;

		if (f_numeric) {
			s = String.valueOf (x);
		}

		else if (f_iso_8601) {
			s = SimpleUtils.time_to_parseable_string (x);
		}

		else {
			s = date_fmt.format (new Date (x));
		}

		return s;
	}




	// Parse a time using this information.

	public long parse_time (String s) {
		long x;

		if (f_numeric) {
			try {
				x = Long.parseLong (s);
			} catch (Exception e) {
				throw new RuntimeException ("FormatTimeInfo.parse_time: Error parsing numeric (millisecond) time: s = " + s, e);
			}
		}

		else if (f_iso_8601) {
			try {
				x = SimpleUtils.string_to_time (s);
			} catch (Exception e) {
				throw new RuntimeException ("FormatTimeInfo.parse_time: Error parsing ISO-8601 time: s = " + s, e);
			}
		}

		else {
			try {
				Date date = date_fmt.parse (s);
				x = date.getTime();
			} catch (Exception e) {
				throw new RuntimeException ("FormatTimeInfo.parse_time: Error parsing time: s = " + s, e);
			}
		}

		return x;
	}




	//  // Private function to prepare a string for display.
	//  
	//  private static String disp_string (String s) {
	//  	if (s == null) {
	//  		return "<null>";
	//  	}
	//  	if (s.isEmpty() || s.matches (".*\\s.*")) {
	//  		return "\"" + s.replaceAll ("\t", "\\\\t") + "\"";
	//  	}
	//  	return s;
	//  }




	//  // Private function to prepare a date format for display.
	//  
	//  private static String disp_date_format (SimpleDateFormat df) {
	//  	return ((df == null) ? "<null>" : disp_string (df.toPattern());
	//  }




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("FormatTimeInfo:" + "\n");

		result.append ("date_fmt = " + SimpleUtils.disp_date_format_for_user (date_fmt) + "\n");
		result.append ("f_numeric = " + f_numeric + "\n");
		result.append ("f_iso_8601 = " + f_iso_8601 + "\n");
		result.append ("field_words = " + field_words + "\n");
		result.append ("field_width = " + field_width + "\n");

		return result.toString();
	}

	// One-line string.

	public final String one_line_string () {
		return "["
			+ "date_fmt = " + SimpleUtils.disp_date_format_for_user (date_fmt)
			+ ", f_numeric = " + f_numeric
			+ ", f_iso_8601 = " + f_iso_8601
			+ ", field_words = " + field_words
			+ ", field_width = " + field_width
			+ "]";
	}

	// One-line string for a given object, which can be null.

	public static String one_line_string (FormatTimeInfo fti) {
		return ((fti == null) ? "<null>" : fti.one_line_string());
	}

}
