package org.opensha.oaf.util.catalog;

import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

import java.time.Instant;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.catalog.EventIDGenerator;

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



// Line formatter for legacy GUI 10-column format.
// Author: Michael Barall 10/11/2021.
//
// Each line has 10 fields, as follows.  Fields are separated with a single tab.
//
//  <Year> <Month> <Day> <Hour> <Minute> <Sec> <Lat> <Lon> <Depth> <Magnitude>
//
// The year has 4 digits.  The month, day, hour, minute, and second each have 2 digits.
// The latitude, longitude, depth, and magnitude originally were cast to float and then
// converted to string using the Java default format.  Here, we format them fixed-point
// to 5, 5, 3, and 2 decimal places, respectively, and remove trailing zeros.
//
// For parse, fields can be separated by any amount of white space, and there
// can be leading and trailing white space (as defined by String.trim).

public class RuptureLineFormatGUILegacy extends RuptureLineFormatBase implements RuptureLineFormatter {

	// Constructor sets up formatting.

	public RuptureLineFormatGUILegacy () {
		setup_time ("yyyy\tMM\tdd\tHH\tmm\tss");
		setup_lat ("f", 5, true, false, false);
		setup_lon ("f", 5, true, false, false);
		setup_depth ("f", 3, true, false, false);
		setup_mag ("f", 2, true, false, false);
	}


	// Set a converter for absolute/relative time and location conversions.

	public RuptureLineFormatGUILegacy set_abs_rel_conv (AbsRelTimeLocConverter converter) {
		abs_rel_conv = converter;
		return this;
	}


	// Display internal formatting information.
		
	@Override
	public String show_format_info () {
		return "RuptureLineFormatGUILegacy:\n" + super.show_format_info(); 
	}


	@Override
	public String toString () {
		return show_format_info();
	}


	// Format a line, taking parameter values from rf.

	@Override
	public String format_line (RuptureFormatter rf) {
		StringBuilder sb = new StringBuilder();
		try {

			// Require absolute time and location

			rf.require_has_absolute (abs_rel_conv);

			// Format parameters

			sb.append (format_time (rf));
			sb.append ("\t");
			sb.append (format_lat (rf));
			sb.append ("\t");
			sb.append (format_lon (rf));
			sb.append ("\t");
			sb.append (format_depth (rf));
			sb.append ("\t");
			sb.append (format_mag (rf));
		}
		catch (Exception e) {
			throw new RuntimeException ("RuptureLineFormatGUILegacy.format_line: Error formatting line", e);
		}
		return sb.toString();
	}


	// Parse a line, storing parameter value into rf.

	@Override
	public void parse_line (RuptureFormatter rf, String line) {
		try {

			// Clear all parameters and prepare to write absolute time and location

			rf.clear_and_prep_absolute_tloc();

			// Split line into words

			set_word_array (SimpleUtils.split_around_trim (line.trim()));

			// Assemble the words that make up the time, tab-separated

			StringBuilder sb_time = new StringBuilder();
			for (int n = 0; n < fti_time.field_words; ++n) {
				if (n > 0) {
					sb_time.append ("\t");
				}
				sb_time.append (get_word_array_next (IDMISS_EXCEPT));
			}
			String date_str = sb_time.toString();

			// Parse parameters

			parse_time (rf, date_str);
			parse_lat (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_lon (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_depth (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_mag (rf, get_word_array_next (IDMISS_EXCEPT));

			// Check that all words were used

			require_word_array_end();

			// Fabricate an event id from the time and magnitude

			parse_event_id (rf, EventIDGenerator.generate_id(), IDMISS_EXCEPT);
		}
		catch (Exception e) {
			throw new RuntimeException ("RuptureLineFormatGUILegacy.parse_line: Error parsing line: line = " + line, e);
		}
		return;
	}

}
