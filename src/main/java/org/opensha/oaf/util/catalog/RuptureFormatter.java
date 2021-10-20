package org.opensha.oaf.util.catalog;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.text.SimpleDateFormat;

import java.time.Instant;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.oetas.OEOrigin;

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



// An object for formatting and parsing ruptures.
// Author: Michael Barall 09/10/2021.
//
// This contains fields for all possible rupture parameters, including
// both absolute and relative time and location.  Functions are provided
// to support conversion to/from various formats.  Formatting and parsing
// to/from text is typically done using a RuptureLineFormatter.

public class RuptureFormatter {


	//----- Earthquake parameters -----

	// Earthquake strings.

	public final RuptureStrings eqk_strs = new RuptureStrings();

	// Earthquake magnitude.

	public double eqk_mag = 0.0;

	// Absolute time and location, null if not available.

	public AbsoluteTimeLocation abs_tloc = null;

	// Relative time and location, null if not available.

	public RelativeTimeLocation rel_tloc = null;




	//----- Support for conversion between absolute and relative time and location -----




	// Require availability of relative time and location.
	// If relative time and location is not available, attempt to convert from absolute time and location.
	// If abs_rel_conv is null or not supplied, then throw an exception if relative time and location is not available.

	public final void require_has_relative (AbsRelTimeLocConverter abs_rel_conv) {

		// If we need to compute relative time and location ...

		if (rel_tloc == null) {

			// Error if no converter is provided

			if (abs_rel_conv == null) {
				throw new IllegalStateException ("RuptureFormatter.require_has_relative - No converter provided");
			}

			// If we don't have absolute time and location, it's an error

			if (abs_tloc == null) {
				throw new IllegalStateException ("RuptureFormatter.require_has_relative - Absolute time and location are not available");
			}

			// Do the conversion

			rel_tloc = new RelativeTimeLocation();
			abs_rel_conv.convert_abs_to_rel (abs_tloc, rel_tloc);
		}

		return;
	}


	public final void require_has_relative () {

		// If not available, throw exception

		if (rel_tloc == null) {
			throw new IllegalStateException ("RuptureFormatter.require_has_relative - Relative time and location are not available");
		}

		return;
	}




	// Require availability of absolute time and location.
	// If absolute time and location is not available, attempt to convert from relative time and location.
	// If abs_rel_conv is null or not supplied, then throw an exception if absolute time and location is not available.

	public final void require_has_absolute (AbsRelTimeLocConverter abs_rel_conv) {

		// If we need to compute absolute time and location ...

		if (abs_tloc == null) {

			// Error if no converter is provided

			if (abs_rel_conv == null) {
				throw new IllegalStateException ("RuptureFormatter.require_has_absolute - No converter provided");
			}

			// If we don't have relative time and location, it's an error

			if (rel_tloc == null) {
				throw new IllegalStateException ("RuptureFormatter.require_has_absolute - Relative time and location are not available");
			}

			// Do the conversion

			abs_tloc = new AbsoluteTimeLocation();
			abs_rel_conv.convert_rel_to_abs (rel_tloc, abs_tloc);
		}

		return;
	}


	public final void require_has_absolute () {

		// If not available, throw exception

		if (abs_tloc == null) {
			throw new IllegalStateException ("RuptureFormatter.require_has_absolute - Absolute time and location are not available");
		}

		return;
	}




	// Prepare to write relative time and location.
	// Allocate relative time and location if not already allocated, then clear it.

	public final void prepare_relative_tloc () {
		if (rel_tloc == null) {
			rel_tloc = new RelativeTimeLocation();	// constructor also clears it
		} else {
			rel_tloc.clear();
		}
		return;
	}




	// Prepare to write absolute time and location.
	// Allocate absolute time and location if not already allocated, then clear it.

	public final void prepare_absolute_tloc () {
		if (abs_tloc == null) {
			abs_tloc = new AbsoluteTimeLocation();	// constructor also clears it
		} else {
			abs_tloc.clear();
		}
		return;
	}




	// Delete the relative time and location, making it unavailable

	public final void delete_relative_tloc () {
		rel_tloc = null;
		return;
	}




	// Delete the absolute time and location, making it unavailable.

	public final void delete_absolute_tloc () {
		abs_tloc = null;
		return;
	}




	//----- Getters and setters -----




	// Clear parameters to default, no time and location available.

	public final void clear () {

		eqk_strs.clear();
		eqk_mag = 0.0;
		abs_tloc = null;
		rel_tloc = null;

		return;
	}




	// Clear parameters to default, and prepare to write relative time and location.
	// The effect is the same as clear() followed by prepare_relative_tloc();

	public final void clear_and_prep_relative_tloc () {

		eqk_strs.clear();
		eqk_mag = 0.0;
		abs_tloc = null;

		if (rel_tloc == null) {
			rel_tloc = new RelativeTimeLocation();	// constructor also clears it
		} else {
			rel_tloc.clear();
		}

		return;
	}




	// Clear parameters to default, and prepare to write relative time and location.
	// The effect is the same as clear() followed by prepare_absolute_tloc();

	public final void clear_and_prep_absolute_tloc () {

		eqk_strs.clear();
		eqk_mag = 0.0;
		rel_tloc = null;

		if (abs_tloc == null) {
			abs_tloc = new AbsoluteTimeLocation();	// constructor also clears it
		} else {
			abs_tloc.clear();
		}

		return;
	}




	// Get the earthquake strings.

	public final RuptureStrings get_eqk_strs () {
		return eqk_strs;
	}

	// Copy the earthquake strings.
	
	public final RuptureStrings copy_eqk_strs (RuptureStrings strs) {
		strs.copy_from (eqk_strs);
		return strs;
	}

	// Set the earthquake strings.

	public final void set_eqk_strs (RuptureStrings strs) {
		eqk_strs.copy_from (strs);
		return;
	}

	// Get an identification string to use in error messages, or null if none available.

	public final String get_error_id () {
		String result = null;
		if (!( eqk_strs.eqk_event_id == null || eqk_strs.eqk_event_id.trim().isEmpty() )) {
			result = eqk_strs.eqk_event_id.trim();
		}
		else if (!( eqk_strs.eqk_id_list == null || eqk_strs.eqk_id_list.length == 0 || eqk_strs.eqk_id_list[0] == null || eqk_strs.eqk_id_list[0].trim().isEmpty() )) {
			result = eqk_strs.eqk_id_list[0].trim();
		}
		return result;
	}




	// Get the magnitude.

	public final double get_eqk_mag () {
		return eqk_mag;
	}

	// Set the magnitude.

	public final void set_eqk_mag (double mag) {
		eqk_mag = mag;
		return;
	}

	// Set the coerced magnitude.

	public final void set_coerce_mag (double mag) {
		eqk_mag = AbsoluteTimeLocation.coerce_mag (mag);
		return;
	}

	// Get the coerced magnitude.

	public final double get_coerce_mag () {
		return AbsoluteTimeLocation.coerce_mag (eqk_mag);
	}




	// Get the absolute time and location.
	// Return null if not available (does not trigger relative-to-absolute conversion).

	public final AbsoluteTimeLocation get_abs_tloc () {
		return abs_tloc;
	}

	// Get the absolute time and location.
	// Perform relative-to-absolute conversion if needed, exception if not possible.

	public final AbsoluteTimeLocation req_abs_tloc () {
		require_has_absolute();
		return abs_tloc;
	}

	public final AbsoluteTimeLocation req_abs_tloc (AbsRelTimeLocConverter abs_rel_conv) {
		require_has_absolute (abs_rel_conv);
		return abs_tloc;
	}

	// Copy the absolute time and location.
	// Perform relative-to-absolute conversion if needed, exception if not possible.
	
	public final void copy_abs_tloc (AbsoluteTimeLocation tloc) {
		require_has_absolute();
		tloc.copy_from (abs_tloc);
		return;
	}
	
	public final void copy_abs_tloc (AbsoluteTimeLocation tloc, AbsRelTimeLocConverter abs_rel_conv) {
		require_has_absolute (abs_rel_conv);
		tloc.copy_from (abs_tloc);
		return;
	}

	// Set the absolute time and location.
	// The supplied time and location is copied (not retained).
	// The supplied time and location can be null to make the absolute time and location unavailable.

	public final void set_abs_tloc (AbsoluteTimeLocation tloc) {
		if (tloc == null) {
			abs_tloc = null;
		} else {
			if (abs_tloc == null) {
				abs_tloc = new AbsoluteTimeLocation (tloc);
			} else {
				abs_tloc.copy_from (tloc);
			}
		}
		return;
	}




	// Get the relative time and location.
	// Return null if not available (does not trigger absolute-to-relative conversion).

	public final RelativeTimeLocation get_rel_tloc () {
		return rel_tloc;
	}

	// Get the relative time and location.
	// Perform absolute-to-relative conversion if needed, exception if not possible.

	public final RelativeTimeLocation req_rel_tloc () {
		require_has_relative();
		return rel_tloc;
	}

	public final RelativeTimeLocation req_rel_tloc (AbsRelTimeLocConverter abs_rel_conv) {
		require_has_relative (abs_rel_conv);
		return rel_tloc;
	}

	// Copy the relative time and location.
	// Perform absolute-to-relative conversion if needed, exception if not possible.
	
	public final void copy_rel_tloc (RelativeTimeLocation tloc) {
		require_has_relative();
		tloc.copy_from (rel_tloc);
		return;
	}
	
	public final void copy_rel_tloc (RelativeTimeLocation tloc, AbsRelTimeLocConverter abs_rel_conv) {
		require_has_relative (abs_rel_conv);
		tloc.copy_from (rel_tloc);
		return;
	}

	// Set the relative time and location.
	// The supplied time and location is copied (not retained).
	// The supplied time and location can be null to make the relative time and location unavailable.

	public final void set_rel_tloc (RelativeTimeLocation tloc) {
		if (tloc == null) {
			rel_tloc = null;
		} else {
			if (rel_tloc == null) {
				rel_tloc = new RelativeTimeLocation (tloc);
			} else {
				rel_tloc.copy_from (tloc);
			}
		}
		return;
	}




	// Set earthquake parameters from rupture information.
	// Set the flag indicating that absolute time and location is valid.
	// Clear the flag indicating that relative time and location is valid.

	public final void set_eqk_rupture (ObsEqkRupture rup) {

		eqk_strs.set (rup);
		eqk_mag = rup.getMag();

		if (abs_tloc == null) {
			abs_tloc = new AbsoluteTimeLocation (rup);
		} else {
			abs_tloc.set (rup);
		}

		rel_tloc = null;
		return;
	}


	// Make an ObsEqkRupture object.
	// Longitude range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.
	// Perform relative-to-absolute conversion if needed, exception if not possible.
	// Throws an exception if unable to create the object.
	// Note: Throws an exception if there is not a valid event id.

	public final ObsEqkRupture get_eqk_rupture (boolean wrapLon) {
		require_has_absolute();
		return eqk_strs.getObsEqkRupture (abs_tloc, eqk_mag, wrapLon);
	}

	public final ObsEqkRupture get_eqk_rupture (boolean wrapLon, AbsRelTimeLocConverter abs_rel_conv) {
		require_has_absolute (abs_rel_conv);
		return eqk_strs.getObsEqkRupture (abs_tloc, eqk_mag, wrapLon);
	}




	//----- Construction -----




	// Constructor sets all parameters to default, no time and location available.

	public RuptureFormatter () {
		clear();
	}




	// Copy parameters from another object.

	public final RuptureFormatter copy_from (RuptureFormatter other) {
		set_eqk_strs (other.eqk_strs);
		eqk_mag = other.eqk_mag;
		set_abs_tloc (other.abs_tloc);
		set_rel_tloc (other.rel_tloc);
		return this;
	}

	// Constructor - copy parameters from anther object.

	public RuptureFormatter (RuptureFormatter other) {
		eqk_strs.copy_from (other.eqk_strs);
		eqk_mag = other.eqk_mag;

		if (other.abs_tloc == null) {
			abs_tloc = null;
		} else {
			abs_tloc = new AbsoluteTimeLocation (other.abs_tloc);
		}

		if (other.rel_tloc == null) {
			rel_tloc = null;
		} else {
			rel_tloc = new RelativeTimeLocation (other.rel_tloc);
		}
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("RuptureFormatter:" + "\n");

		result.append (eqk_strs.value_string());
		result.append ("eqk_mag = " + eqk_mag + "\n");

		if (abs_tloc == null) {
			result.append ("abs_tloc = " + "<null>" + "\n");
		} else {
			result.append (abs_tloc.value_string());
		}

		if (rel_tloc == null) {
			result.append ("rel_tloc = " + "<null>" + "\n");
		} else {
			result.append (rel_tloc.value_string());
		}

		return result.toString();
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("RuptureFormatter : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  wrapLon  extendedInfo
		// Fetch an event from Comcat, and display it.
		// Make a rupture formatter, and display the empty formatter.
		// Load the rupture into the rupture formatter, and display the formatter.
		// Obtain a new rupture object from the formatter, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 3 additional arguments

			if (!( args.length == 4 )) {
				System.err.println ("RuptureFormatter : Invalid 'test1' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean wrapLon = Boolean.parseBoolean (args[2]);
				boolean extendedInfo = Boolean.parseBoolean (args[3]);

				// Say hello

				System.out.println ("Testing set and get of a rupture.");
				System.out.println ("event_id = " + event_id);
				System.out.println ("wrapLon = " + wrapLon);
				System.out.println ("extendedInfo = " + extendedInfo);

				// Load from Comcat

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture rup = accessor.fetchEvent (event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup));

				// Make the rupture formatter

				RuptureFormatter rf = new RuptureFormatter();

				System.out.println();
				System.out.println(rf.toString());

				// Insert the rupture into it

				rf.set_eqk_rupture (rup);

				System.out.println();
				System.out.println(rf.toString());

				rup = null;

				// Make a new rupture

				ObsEqkRupture rup2 = rf.get_eqk_rupture (wrapLon);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup2));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id  wrapLon  extendedInfo
		// Fetch an event from Comcat, and display it.
		// Make a rupture formatter, and display the empty formatter.
		// Load the rupture into the rupture formatter, and display the formatter.
		// Write a line in GUI legacy 10-column format, and display it.
		// Read the line into a new rupture formatter, and display the new formatter.
		// Obtain a new rupture object from the new formatter, and display it.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 3 additional arguments

			if (!( args.length == 4 )) {
				System.err.println ("RuptureFormatter : Invalid 'test2' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean wrapLon = Boolean.parseBoolean (args[2]);
				boolean extendedInfo = Boolean.parseBoolean (args[3]);

				// Say hello

				System.out.println ("Testing format and parse of a line in GUI legacy format.");
				System.out.println ("event_id = " + event_id);
				System.out.println ("wrapLon = " + wrapLon);
				System.out.println ("extendedInfo = " + extendedInfo);

				// Load from Comcat

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture rup = accessor.fetchEvent (event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup));

				// Make the rupture formatter

				RuptureFormatter rf = new RuptureFormatter();

				System.out.println();
				System.out.println(rf.toString());

				// Insert the rupture into it

				rf.set_eqk_rupture (rup);

				System.out.println();
				System.out.println(rf.toString());

				rup = null;

				// Make a line formatter.

				RuptureLineFormatGUILegacy line_fmt = new RuptureLineFormatGUILegacy();

				System.out.println();
				System.out.println(line_fmt.show_format_info());

				// Format the line

				String line = line_fmt.format_line (rf);

				System.out.println();
				System.out.println("line = " + SimpleUtils.disp_string_for_user (line));

				rf = null;

				// Make a new rupture formatter and parse the line

				RuptureFormatter rf2 = new RuptureFormatter();

				line_fmt.parse_line (rf2, line);

				System.out.println();
				System.out.println(rf2.toString());

				// Make a new rupture from the new formatter

				ObsEqkRupture rup2 = rf2.get_eqk_rupture (wrapLon);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup2));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id  wrapLon  extendedInfo  origin_event_id
		// Fetch origin event from Comcat, and display it.
		// Make the origin object, and display it.
		// Fetch an event from Comcat, and display it.
		// Make a rupture formatter, and display the empty formatter.
		// Load the rupture into the rupture formatter, and display the formatter.
		// Trigger conversion to relative time and location, and display the formatter.
		// Delete absolute time and location, and display the formatter.
		// Obtain a new rupture object from the formatter, and display it.
		// Display the formatter.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 4 additional arguments

			if (!( args.length == 5 )) {
				System.err.println ("RuptureFormatter : Invalid 'test3' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean wrapLon = Boolean.parseBoolean (args[2]);
				boolean extendedInfo = Boolean.parseBoolean (args[3]);
				String origin_event_id = args[4];

				// Say hello

				System.out.println ("Testing set and get of a rupture with absolute/relative conversion.");
				System.out.println ("event_id = " + event_id);
				System.out.println ("wrapLon = " + wrapLon);
				System.out.println ("extendedInfo = " + extendedInfo);
				System.out.println ("origin_event_id = " + origin_event_id);

				// Load origin from Comcat

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture origin_rup = accessor.fetchEvent (origin_event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (origin_rup));

				// Make the origin object

				OEOrigin origin = (new OEOrigin()).set_from_rupture_horz(origin_rup);

				System.out.println();
				System.out.println(origin.toString());

				// Load from Comcat

				ObsEqkRupture rup = accessor.fetchEvent (event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup));

				// Make the rupture formatter

				RuptureFormatter rf = new RuptureFormatter();

				System.out.println();
				System.out.println(rf.toString());

				// Insert the rupture into it

				rf.set_eqk_rupture (rup);

				System.out.println();
				System.out.println(rf.toString());

				rup = null;

				// Force conversion to relative coordinates

				rf.require_has_relative (origin);

				System.out.println();
				System.out.println(rf.toString());

				// Delete absolute time and location

				rf.delete_absolute_tloc();

				System.out.println();
				System.out.println(rf.toString());

				// Make a new rupture

				ObsEqkRupture rup2 = rf.get_eqk_rupture (wrapLon, origin);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup2));

				// Display the formatter

				System.out.println();
				System.out.println(rf.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  event_id  wrapLon  extendedInfo  origin_event_id
		// Fetch origin event from Comcat, and display it.
		// Make the origin object, and display it.
		// Fetch an event from Comcat, and display it.
		// Make a rupture formatter, and display the empty formatter.
		// Load the rupture into the rupture formatter, and display the formatter.
		// Trigger conversion to relative time and location, and display the formatter.
		// Delete absolute time and location, and display the formatter.
		// Write a line in GUI legacy 10-column format, and display it.
		// Display the modified rupture formatter.
		// Read the line into a new rupture formatter, and display the new formatter.
		// Obtain a new rupture object from the new formatter, and display it.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 4 additional arguments

			if (!( args.length == 5 )) {
				System.err.println ("RuptureFormatter : Invalid 'test4' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean wrapLon = Boolean.parseBoolean (args[2]);
				boolean extendedInfo = Boolean.parseBoolean (args[3]);
				String origin_event_id = args[4];

				// Say hello

				System.out.println ("Testing format and parse of a line in GUI legacy format with absolute/relative conversion.");
				System.out.println ("event_id = " + event_id);
				System.out.println ("wrapLon = " + wrapLon);
				System.out.println ("extendedInfo = " + extendedInfo);
				System.out.println ("origin_event_id = " + origin_event_id);

				// Load origin from Comcat

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture origin_rup = accessor.fetchEvent (origin_event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (origin_rup));

				// Make the origin object

				OEOrigin origin = (new OEOrigin()).set_from_rupture_horz(origin_rup);

				System.out.println();
				System.out.println(origin.toString());

				// Load from Comcat

				ObsEqkRupture rup = accessor.fetchEvent (event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup));

				// Make the rupture formatter

				RuptureFormatter rf = new RuptureFormatter();

				System.out.println();
				System.out.println(rf.toString());

				// Insert the rupture into it

				rf.set_eqk_rupture (rup);

				System.out.println();
				System.out.println(rf.toString());

				rup = null;

				// Force conversion to relative coordinates

				rf.require_has_relative (origin);

				System.out.println();
				System.out.println(rf.toString());

				// Delete absolute time and location

				rf.delete_absolute_tloc();

				System.out.println();
				System.out.println(rf.toString());

				// Make a line formatter.

				RuptureLineFormatGUILegacy line_fmt = (new RuptureLineFormatGUILegacy()).set_abs_rel_conv(origin);

				System.out.println();
				System.out.println(line_fmt.show_format_info());

				// Format the line

				String line = line_fmt.format_line (rf);

				System.out.println();
				System.out.println("line = " + SimpleUtils.disp_string_for_user (line));

				// Display the modified rupture formatter

				System.out.println();
				System.out.println(rf.toString());

				rf = null;

				// Make a new rupture formatter and parse the line

				RuptureFormatter rf2 = new RuptureFormatter();

				line_fmt.parse_line (rf2, line);

				System.out.println();
				System.out.println(rf2.toString());

				// Make a new rupture from the new formatter

				ObsEqkRupture rup2 = rf2.get_eqk_rupture (wrapLon);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (rup2));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("RuptureFormatter : Unrecognized subcommand : " + args[0]);
		return;

	}




}
