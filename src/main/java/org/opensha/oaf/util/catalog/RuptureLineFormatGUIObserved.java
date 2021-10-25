package org.opensha.oaf.util.catalog;

import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

import java.time.Instant;

import org.opensha.oaf.util.SimpleUtils;

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



// Line formatter for GUI observed catalog, 7-column format.
// Author: Michael Barall 10/11/2021.
//
// Each line has 7 fields, as follows.  Fields are separated with a two spaces.
// (It is hard for a user to read the file if only a single space is used.)
//
//  <YYYY-MM-DD> <HH:MM:SS> <Lat> <Lon> <Depth> <Magnitude> <Event ID>
//
// The year has 4 digits.  The month, day, hour, minute, and second each have 2 digits.
// The latitude, longitude, depth, and magnitude are formatted as fixed-point,
// to 5, 5, 3, and 3 decimal places, respectively.  Trailing zeros are removed,
// and padding is added to align data in columns.
//
// For parse, fields can be separated by any amount of white space, and there
// can be leading and trailing white space (as defined by String.trim).
// The event ID is optional, and if not included then an event ID is generated.

public class RuptureLineFormatGUIObserved extends RuptureLineFormatBase implements RuptureLineFormatter {

	// Separator between fields, it must be entirely spaces and tabs.

	private String separator;

	// Constructor sets up formatting.

	public RuptureLineFormatGUIObserved () {
		separator = "  ";
		setup_time ("yyyy-MM-dd" + separator + "HH:mm:ss");
		setup_lat ("f", 5, true, false, true);
		setup_lon ("f", 5, true, false, true);
		setup_depth ("f", 3, true, false, true);
		setup_mag ("f", 3, true, false, true);
	}


	// Set a converter for absolute/relative time and location conversions.

	public RuptureLineFormatGUIObserved set_abs_rel_conv (AbsRelTimeLocConverter converter) {
		abs_rel_conv = converter;
		return this;
	}


	// Display internal formatting information.
		
	@Override
	public String show_format_info () {
		return "RuptureLineFormatGUIObserved:\n" + super.show_format_info(); 
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
			sb.append (separator);
			sb.append (format_lat (rf));
			sb.append (separator);
			sb.append (format_lon (rf));
			sb.append (separator);
			sb.append (format_depth (rf));
			sb.append (separator);

			String event_id = format_event_id (rf, IDMISS_NULL);
			if (event_id != null) {
				sb.append (format_mag (rf));
				sb.append (separator);
				sb.append (event_id);
			} else {
				sb.append (SimpleUtils.trim_trailing (format_mag (rf)));
			}
		}
		catch (Exception e) {
			throw new RuntimeException ("RuptureLineFormatGUIObserved.format_line: Error formatting line", e);
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
					sb_time.append (separator);
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

			if (word_array_has_next()) {
				parse_event_id (rf, get_word_array_next (IDMISS_EXCEPT), IDMISS_EXCEPT);
			} else {
				parse_event_id (rf, "T" + rf.abs_tloc.abs_time + "_M" + String.format (Locale.US, "%.4f", rf.get_eqk_mag()), IDMISS_EXCEPT);
			}

			// Check that all words were used

			require_word_array_end();
		}
		catch (Exception e) {
			throw new RuntimeException ("RuptureLineFormatGUIObserved.parse_line: Error parsing line: line = " + line, e);
		}
		return;
	}


	// Get a comment block that describes the contents of each line.
	// Parameters:
	//  comments = Collection (usually a List) to which comments are added.
	//  prefix_len = Length of prefix that will be pre-pended to each comment.
	// This function adds the comment lines to the collection.  If there are no
	// comment lines, then this function performs no operation and leaves the
	// collection unmodified.
	// If prefix_len is positive, then it is assumed that each comment line
	// will have a prefix of that length pre-pended.  The function may want to
	// make each comment start with a space to provide separation from the prefix.

	public void get_comment_block (Collection<String> comments, int prefix_len) {
		StringBuilder sb = new StringBuilder();

		append_column_heading (sb, "Date", 10, 0, prefix_len, false);
		sb.append (separator);
		append_column_heading (sb, "Time", 8, 0, 0, false);
		sb.append (separator);
		append_column_heading (sb, "Lat", fdi_lat.field_width, 0, 0, false);
		sb.append (separator);
		append_column_heading (sb, "Lon", fdi_lon.field_width, 0, 0, false);
		sb.append (separator);
		append_column_heading (sb, "Depth", fdi_depth.field_width, 0, 0, false);
		sb.append (separator);
		append_column_heading (sb, "Mag", fdi_mag.field_width, 0, 0, false);
		sb.append (separator);
		append_column_heading (sb, "ID", 10, 0, 0, true);

		comments.add (sb.toString());
		return;
	}

}
