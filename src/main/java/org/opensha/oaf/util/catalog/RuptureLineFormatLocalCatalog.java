package org.opensha.oaf.util.catalog;

import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
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



// Line formatter for local catalog format.
// Author: Michael Barall 10/11/2021.
//
// Each line is formatted as follows:
//
//  <Network> <Code> <Time> <Magnitude> <Lat> <Lon> <Depth> <ID-Count> <ID>... <Description>
//
// Time is given as an integer number of milliseconds since the epoch.
// Magnitude, latitude, longitude, and depth use the default Java format, which
// is full precision but with trailing zeros removed; this typically results in
// fixed-point formats with a small number of decimal places.
// The ID-count is a positive integer giving the number of IDs, and it is followed
// by the list of IDs; the first ID in the list is the event ID.
// The description is URL-encoded so it appears in the line as a single word.
//
// For format, fields are separated by a single space.
//
// For parse, fields can be separated by any amount of white space, and there
// can be leading and trailing white space (as defined by String.trim).

public class RuptureLineFormatLocalCatalog extends RuptureLineFormatBase implements RuptureLineFormatter {

	// Constructor sets up formatting.

	public RuptureLineFormatLocalCatalog () {
		setup_time ("u");
		setup_lat ("s", 0, false, false, false);
		setup_lon ("s", 0, false, false, false);
		setup_depth ("s", 0, false, false, false);
		setup_mag ("s", 0, false, false, false);
	}


	// Set a converter for absolute/relative time and location conversions.

	public RuptureLineFormatLocalCatalog set_abs_rel_conv (AbsRelTimeLocConverter converter) {
		abs_rel_conv = converter;
		return this;
	}


	// Display internal formatting information.
		
	@Override
	public String show_format_info () {
		return "RuptureLineFormatLocalCatalog:\n" + super.show_format_info(); 
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

			sb.append (format_network (rf, IDMISS_EXCEPT));
			sb.append (" ");
			sb.append (format_code (rf, IDMISS_EXCEPT));
			sb.append (" ");
			sb.append (format_time (rf));
			sb.append (" ");
			sb.append (format_mag (rf));
			sb.append (" ");
			sb.append (format_lat (rf));
			sb.append (" ");
			sb.append (format_lon (rf));
			sb.append (" ");
			sb.append (format_depth (rf));

			String[] id_list = format_id_list_with_event_id (rf, IDMISS_EXCEPT);
			int idlen = id_list.length;
			sb.append (" ");
			sb.append (idlen);
			for (int i = 0; i < idlen; ++i) {
				sb.append (" ");
				sb.append (id_list[i]);
			}

			String desc = format_description (rf, IDMISS_NULL);
			if (desc == null) {
				desc = "Unknowm";
			}
			sb.append (" ");
			sb.append (SimpleUtils.url_encode (desc, true));
		}
		catch (Exception e) {
			throw new RuntimeException ("RuptureLineFormatLocalCatalog.format_line: Error formatting line", e);
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

			// Parse parameters

			parse_network (rf, get_word_array_next (IDMISS_EXCEPT), IDMISS_EXCEPT);
			parse_code (rf, get_word_array_next (IDMISS_EXCEPT), IDMISS_EXCEPT);
			parse_time (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_mag (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_lat (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_lon (rf, get_word_array_next (IDMISS_EXCEPT));
			parse_depth (rf, get_word_array_next (IDMISS_EXCEPT));

			String s_idlen = get_word_array_next (IDMISS_EXCEPT);
			int idlen;
			try {
				idlen = Integer.parseInt (s_idlen);
			} catch (Exception e) {
				throw new RuntimeException ("RuptureLineFormatLocalCatalog.parse_line: Invalid id list length: " + s_idlen, e);
			}
			if (idlen <= 0) {
				throw new RuntimeException ("RuptureLineFormatLocalCatalog.parse_line: Invalid id list length: " + s_idlen);
			}
			String[] id_list = new String[idlen];
			for (int i = 0; i < idlen; ++i) {
				id_list[i] = get_word_array_next (IDMISS_EXCEPT);
			}
			parse_id_list_with_event_id (rf, id_list, IDMISS_EXCEPT);

			String desc = SimpleUtils.url_decode (get_word_array_next (IDMISS_EXCEPT), true);
			parse_description (rf, desc, IDMISS_NULL);

			// Check that all words were used

			require_word_array_end();
		}
		catch (Exception e) {
			throw new RuntimeException ("RuptureLineFormatLocalCatalog.parse_line: Error parsing line: line = " + line, e);
		}
		return;
	}

}
