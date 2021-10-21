package org.opensha.oaf.util.catalog;

import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

import java.time.Instant;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.FormatDoubleInfo;
import org.opensha.oaf.util.FormatTimeInfo;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

import static org.opensha.oaf.util.SimpleUtils.TRAILZ_OK;
import static org.opensha.oaf.util.SimpleUtils.TRAILZ_REMOVE;
import static org.opensha.oaf.util.SimpleUtils.TRAILZ_PAD_LEFT;
import static org.opensha.oaf.util.SimpleUtils.TRAILZ_PAD_RIGHT;

import static org.opensha.oaf.util.catalog.RuptureStrings.IDMISS_NULL;
import static org.opensha.oaf.util.catalog.RuptureStrings.IDMISS_EMPTY;
import static org.opensha.oaf.util.catalog.RuptureStrings.IDMISS_EXCEPT;
import static org.opensha.oaf.util.catalog.RuptureStrings.IDMISS_AFFIX;

import static org.opensha.oaf.util.catalog.AbsoluteTimeLocation.LON_RANGE_SPH;
import static org.opensha.oaf.util.catalog.AbsoluteTimeLocation.LON_RANGE_WRAP;
import static org.opensha.oaf.util.catalog.AbsoluteTimeLocation.LON_RANGE_LOC;
import static org.opensha.oaf.util.catalog.AbsoluteTimeLocation.LON_RANGE_FULL;



// Common base implementation class for rupture line formatters.
// Author: Michael Barall 10/11/2021.
//
// This class contains formatters and parsers for parameters,
// plus some convenience functions.
// Note: A line formatter is not required to inherit from this class,
// but doing so may make implementation easier.
// Note: A line formatter that inherits from this class must also
// implement RuptureLineFormatter (or a subinterface thereof).

public abstract class RuptureLineFormatBase {


	//--- Parameter format/parse functions, conversions, and coercing ---
	//
	// Parameters for string and string-array format functions:
	//  rf = Rupture formatter.
	//  idmiss = Action to take if the parameter is not available (null, empty, blank):
	//           IDMISS_NULL = Return null.
	//           IDMISS_EMPTY = Return an empty string or empty array.
	//           IDMISS_EXCEPT = Throw an exception.
	//           IDMISS_AFFIX = Return the concatenation of the prefix and suffix (get_coerce_id_list only).
	//
	// Parameters for string and string-array parse functions:
	//  rf = Rupture formatter.
	//  x = String to parse.
	//  idmiss = Action to take if the x is not available (null, empty, blank):
	//           IDMISS_NULL = Set parameter to null.  (Preferred way to set parameter to "not available").
	//           IDMISS_EMPTY = Set parameter to an empty string or empty array.
	//           IDMISS_EXCEPT = Throw an exception.
	//
	// Parameters for string-array-as-string setup functions (setup_id_list_str):
	//  sep = Separator string to insert between strings when formatting / separator to expect when parsing, must be non-null and non-empty.
	//  prefix = Prefix to insert at start of string when formatting / prefix to expect when parsing, or null if none.
	//  suffix = Suffix to insert at end of string when formatting / suffix to expect when parsing, or null if none.
	//  affix_opt = True if presence of prefix and suffix is optional when parsing.
	//
	// Parameters for double and time format functions:
	//  rf = Rupture formatter.
	//
	// Parameters for double and time parse functions:
	//  rf = Rupture formatter.
	//  x = String to parse.
	//
	// Parameters for double setup functions (see class FormatDoubleInfo):
	//  form = Output form: "f", "e", "E", "g", "G", "s".
	//  prec = Precision.
	//    For "f", "e", or "E", the number of digits after the decimal point.
	//    For "g" or "G", the number of significant digits.
	//    For "s", ignored.
	//  f_trailz = True to remove trailing zeros.
	//  f_plus = True to prefix positive numbers with plus sign (except for "s").
	//  f_pad = True to add padding.
	//    For "f", pad on left with field-specific amount if available;
	//      if trailing zeros are removed then add padding on right.
	//    For "e" or "E", if f_plus is false then insert space before positive values;
	//      if trailing zeros are removed then add padding on right.
	//    For "g" or "G", if f_plus is false then insert space before positive values.
	//    For "s", ignored.
	//
	// Parameters for time setup functions (see class FormatTimeInfo):
	//  form = Output form:
	//    "u" = Numeric output, milliseconds since the epoch (Unix format).
	//    "z" = ISO-8601 format.
	//    Any other value = Format as for SimpleDateFormat.
	//
	// Parameters for longitude range setup function:
	//  wrapLon = Longitude range produced by format, 0 to +360 if true, -180 to +180 if false.
	//  in_range = Longitude range accepted by parse:
	//             LON_RANGE_SPH = Spherical range, -180 to +180
	//             LON_RANGE_WRAP = Wrapped range, 0 to +360
	//             LON_RANGE_LOC = Location range, -180 to +360
	//             LON_RANGE_FULL = Full range, -360 to +360


	// Event id

	protected String format_event_id (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_event_id (idmiss);
	}

	protected void parse_event_id (RuptureFormatter rf, String x, int idmiss) {
		rf.eqk_strs.set_coerce_event_id (x, idmiss);
		return;
	}


	// Network

	protected String format_network (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_network (idmiss);
	}

	protected void parse_network (RuptureFormatter rf, String x, int idmiss) {
		rf.eqk_strs.set_coerce_network (x, idmiss);
		return;
	}


	// Code

	protected String format_code (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_code (idmiss);
	}

	protected void parse_code (RuptureFormatter rf, String x, int idmiss) {
		rf.eqk_strs.set_coerce_code (x, idmiss);
		return;
	}


	// Description

	protected String format_description (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_description (idmiss);
	}

	protected void parse_description (RuptureFormatter rf, String x, int idmiss) {
		rf.eqk_strs.set_coerce_description (x, idmiss);
		return;
	}


	// ID list, as a string

	protected String id_list_str_sep = null;		// separator, must be non-null and non-empty
	protected String id_list_str_prefix = null;		// prefix, can be null
	protected String id_list_str_suffix = null;		// suffix, can be null
	protected boolean id_list_str_affix_opt = true;	// true if prefix/suffix is optional on parse

	protected void setup_id_list_str (String sep, String prefix, String suffix, boolean affix_opt) {
		id_list_str_sep = sep;
		id_list_str_prefix = prefix;
		id_list_str_suffix = suffix;
		id_list_str_affix_opt = affix_opt;
		return;
	}

	protected String format_id_list_str (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_id_list (id_list_str_sep, id_list_str_prefix, id_list_str_suffix, idmiss);
	}

	protected void parse_id_list_str (RuptureFormatter rf, String x, int idmiss) {
		rf.eqk_strs.set_coerce_id_list (x, id_list_str_sep, id_list_str_prefix, id_list_str_suffix, id_list_str_affix_opt, idmiss);
		return;
	}


	// ID list, as an array of strings

	protected String[] format_id_list (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_id_list (idmiss);
	}

	protected void parse_id_list (RuptureFormatter rf, String[] x, int idmiss) {
		rf.eqk_strs.set_coerce_id_list (x, idmiss);
		return;
	}


	// ID list, as an array of strings, with event id
	// Format: If id list is not available, use event id to construct a one-element list.
	// Format: If id list and event id are both available, check event id is the first element in the id list.
	// Parse: Set event id equal to the first element in the id list.

	protected String[] format_id_list_with_event_id (RuptureFormatter rf, int idmiss) {
		return rf.eqk_strs.get_coerce_id_list_with_event_id (idmiss);
	}

	protected void parse_id_list_with_event_id (RuptureFormatter rf, String[] x, int idmiss) {
		rf.eqk_strs.set_coerce_id_list_with_event_id (x, idmiss);
		return;
	}


	// Magnitude

	protected FormatDoubleInfo fdi_mag = null;

	protected void setup_mag (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_mag = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, AbsoluteTimeLocation.get_width_mag (0, f_plus), false);
		return;
	}

	protected String format_mag (RuptureFormatter rf) {
		return fdi_mag.format_double (rf.get_coerce_mag());
	}

	protected void parse_mag (RuptureFormatter rf, String x) {
		rf.set_coerce_mag (fdi_mag.parse_double (x));
		return;
	}


	// Time

	protected FormatTimeInfo fti_time = null;

	protected void setup_time (String form) {
		fti_time = new FormatTimeInfo (form);
		return;
	}

	protected String format_time (RuptureFormatter rf) {
		return fti_time.format_time (rf.abs_tloc.abs_time);
	}

	protected void parse_time (RuptureFormatter rf, String x) {
		rf.abs_tloc.abs_time = fti_time.parse_time (x);
		return;
	}


	// Latitude

	protected FormatDoubleInfo fdi_lat = null;

	protected void setup_lat (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_lat = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, AbsoluteTimeLocation.get_width_lat (0, f_plus), false);
		return;
	}

	protected String format_lat (RuptureFormatter rf) {
		return fdi_lat.format_double (rf.abs_tloc.get_coerce_lat());
	}

	protected void parse_lat (RuptureFormatter rf, String x) {
		rf.abs_tloc.set_coerce_lat (fdi_lat.parse_double (x));
		return;
	}


	// Longitude

	protected FormatDoubleInfo fdi_lon = null;

	protected boolean lon_wrapLon = false;		// longitude range produced by format, 0 to +360 if true, -180 to +180 if false
	protected int lon_in_range = LON_RANGE_FULL;	// longitude range accepted by parse, LON_RANGE_XXXX.

	protected void setup_lon (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_lon = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, AbsoluteTimeLocation.get_width_lon (0, f_plus), false);
		return;
	}

	protected void setup_lon_range (boolean wrapLon, int in_range) {
		lon_wrapLon = wrapLon;
		lon_in_range = in_range;
		return;
	}

	protected String format_lon (RuptureFormatter rf) {
		return fdi_lon.format_double (rf.abs_tloc.get_coerce_lon(lon_wrapLon));
	}

	protected void parse_lon (RuptureFormatter rf, String x) {
		rf.abs_tloc.set_coerce_lon (fdi_lon.parse_double (x), lon_in_range);
		return;
	}


	// Depth

	protected FormatDoubleInfo fdi_depth = null;

	protected void setup_depth (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_depth = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, AbsoluteTimeLocation.get_width_depth (0, f_plus), false);
		return;
	}

	protected String format_depth (RuptureFormatter rf) {
		return fdi_depth.format_double (rf.abs_tloc.get_coerce_depth());
	}

	protected void parse_depth (RuptureFormatter rf, String x) {
		rf.abs_tloc.set_coerce_depth (fdi_depth.parse_double (x));
		return;
	}


	// Relative time

	protected FormatDoubleInfo fdi_t_day = null;

	protected void setup_t_day (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_t_day = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, RelativeTimeLocation.get_width_t_day (0, f_plus), false);
		return;
	}

	protected String format_t_day (RuptureFormatter rf) {
		return fdi_t_day.format_double (rf.rel_tloc.get_coerce_t_day());
	}

	protected void parse_t_day (RuptureFormatter rf, String x) {
		rf.rel_tloc.set_coerce_t_day (fdi_t_day.parse_double (x));
		return;
	}


	// Relative x-coordinate

	protected FormatDoubleInfo fdi_x_km = null;

	protected void setup_x_km (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_x_km = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, RelativeTimeLocation.get_width_x_km (0, f_plus), false);
		return;
	}

	protected String format_x_km (RuptureFormatter rf) {
		return fdi_x_km.format_double (rf.rel_tloc.get_coerce_x_km());
	}

	protected void parse_x_km (RuptureFormatter rf, String x) {
		rf.rel_tloc.set_coerce_x_km (fdi_x_km.parse_double (x));
		return;
	}


	// Relative y-coordinate

	protected FormatDoubleInfo fdi_y_km = null;

	protected void setup_y_km (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_y_km = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, RelativeTimeLocation.get_width_y_km (0, f_plus), false);
		return;
	}

	protected String format_y_km (RuptureFormatter rf) {
		return fdi_y_km.format_double (rf.rel_tloc.get_coerce_y_km());
	}

	protected void parse_y_km (RuptureFormatter rf, String x) {
		rf.rel_tloc.set_coerce_y_km (fdi_y_km.parse_double (x));
		return;
	}


	// Relative depth

	protected FormatDoubleInfo fdi_d_km = null;

	protected void setup_d_km (String form, int prec, boolean f_trailz, boolean f_plus, boolean f_pad) {
		fdi_d_km = new FormatDoubleInfo (form, prec, f_trailz, f_plus, f_pad, RelativeTimeLocation.get_width_d_km (0, f_plus), false);
		return;
	}

	protected String format_d_km (RuptureFormatter rf) {
		return fdi_d_km.format_double (rf.rel_tloc.get_coerce_d_km());
	}

	protected void parse_d_km (RuptureFormatter rf, String x) {
		rf.rel_tloc.set_coerce_d_km (fdi_d_km.parse_double (x));
		return;
	}




	//--- Word array convenience functions ---
	//
	// These functions provide a convenient way for parsers to scan an array of strings.
	// Typically, such an array results from splitting a line into words.

	// The array of words.

	protected String[] word_array = null;

	// Current index into the array of words.

	protected int word_array_index = 0;

	// Set the word array, and set the index to zero.

	protected void set_word_array (String[] words) {
		word_array = words;
		word_array_index = 0;
		return;
	}

	// Get the next word in the array.
	// The idmiss parameter says what to do if there are no more words:
	//   IDMISS_NULL = Return null.
	//   IDMISS_EMPTY = Return an empty string.
	//   IDMISS_EXCEPT = Throw an exception.

	protected String get_word_array_next (int idmiss) {
		if (word_array_index < word_array.length) {
			String s = word_array[word_array_index];
			++word_array_index;
			return s;
		}
		switch (idmiss) {
		case IDMISS_NULL:
			return null;
		case IDMISS_EMPTY:
			return "";
		case IDMISS_EXCEPT:
			throw new RuntimeException ("RuptureLineFormatBase.get_word_array_next: Too few words in list, or too few words on line");
		}
		throw new RuntimeException ("RuptureLineFormatBase.get_word_array_next: Invalid missing word option: idmiss = " + idmiss);
	}

	// Throw an exception if not at end of word array.

	protected void require_word_array_end () {
		if (word_array_index < word_array.length) {
			throw new RuntimeException ("RuptureLineFormatBase.require_word_array_end: Too many words in list, or too many words on line");
		}
		return;
	}

	// Get the number of words in the array.

	protected int get_word_array_count () {
		return word_array.length;
	}

	// Get the number of words remaining in the array.

	protected int get_word_array_remaining () {
		return word_array.length - word_array_index;
	}

	// Return true if there are more words.

	protected boolean word_array_has_next () {
		return word_array_index < word_array.length;
	}




	//--- Absolute/relative conversion support ---

	// The converter for absolute/relative time and location conversions, or null if none supplied.

	protected AbsRelTimeLocConverter abs_rel_conv = null;




	//--- Information display ---

	// Display internal formatting information.

	public String show_format_info () {
		StringBuilder sb = new StringBuilder();

		sb.append ("id_list_str_sep = " + SimpleUtils.disp_string_for_user (id_list_str_sep) + "\n");
		sb.append ("id_list_str_prefix = " + SimpleUtils.disp_string_for_user (id_list_str_prefix) + "\n");
		sb.append ("id_list_str_suffix = " + SimpleUtils.disp_string_for_user (id_list_str_suffix) + "\n");
		sb.append ("id_list_str_affix_opt = " + id_list_str_affix_opt + "\n");
		sb.append ("fdi_mag = " + FormatDoubleInfo.one_line_string (fdi_mag) + "\n");
		sb.append ("fti_time = " + FormatTimeInfo.one_line_string (fti_time) + "\n");
		sb.append ("fdi_lat = " + FormatDoubleInfo.one_line_string (fdi_lat) + "\n");
		sb.append ("fdi_lon = " + FormatDoubleInfo.one_line_string (fdi_lon) + "\n");
		sb.append ("fdi_depth = " + FormatDoubleInfo.one_line_string (fdi_depth) + "\n");
		sb.append ("fdi_t_day = " + FormatDoubleInfo.one_line_string (fdi_t_day) + "\n");
		sb.append ("fdi_x_km = " + FormatDoubleInfo.one_line_string (fdi_x_km) + "\n");
		sb.append ("fdi_y_km = " + FormatDoubleInfo.one_line_string (fdi_y_km) + "\n");
		sb.append ("fdi_d_km = " + FormatDoubleInfo.one_line_string (fdi_d_km) + "\n");

		return sb.toString();
	}

}
