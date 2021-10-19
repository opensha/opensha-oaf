package org.opensha.oaf.util.catalog;

import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.oetas.OEOrigin;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;


// Factory class to externalize and re-create AbsRelTimeLocConverter objects.
// Author: Michael Barall 10/11/2021.
//
// For OEOrigin, the factory string consists of 5 words:
//   "etas_origin"  <time in ISO-8601 format>  <latitude>  <longitude>  <depth>
// When reading, the time can be given either in ISO-8601 format or as a number
// of milliseconds since the epoch.

public class AbsRelTimeLocConverterFactory {

	// Identifiers for known types.

	public static final String ABS_REL_OEORIGIN_TEXT = "etas_origin";
	public static final String ABS_REL_OEORIGIN_CLASS = "org.opensha.oaf.oetas.OEOrigin";




	// Get a string that describes an absolute/relative converter object.
	// Parameters:
	//  abs_rel_conv = Converter object to be externalized.
	// Returns a string describing the converter object.
	// The string can be passed to make_abs_rel_conv to re-construct the converter.
	// It should consist of a sequence of words, separated by a single space.
	// Preferably it should be human readable and understandable.
	// Throws an exception if the object is not understood by the factory.

	public static String describe_abs_rel_conv (AbsRelTimeLocConverter abs_rel_conv) {

		// Get the class name

		String cname = abs_rel_conv.getClass().getName();

		// Switch on class name

		switch (cname) {

		// OEOrigin

		case ABS_REL_OEORIGIN_CLASS: {
			OEOrigin origin = (OEOrigin)abs_rel_conv;
			StringBuilder sb = new StringBuilder ();
			sb.append (ABS_REL_OEORIGIN_TEXT);
			sb.append (" ");
			sb.append (SimpleUtils.time_to_parseable_string (origin.origin_time));
			sb.append (" ");
			sb.append (origin.origin_lat);
			sb.append (" ");
			sb.append (origin.origin_lon);
			sb.append (" ");
			sb.append (origin.origin_depth);
			return sb.toString();
		}

		}

		// Unrecognized type

		throw new IllegalArgumentException ("AbsRelTimeLocConverterFactory.describe_abs_rel_conv: Unrecognized class name: " + cname);
	}




	// Make an absolute/relative converter object, given the description.
	// Parameters:
	//  description = String describing the converter, usually created by describe_abs_rel_conv.
	// The string may be human-edited, and so this function should attempt to be
	// forgiving about the format, including leading, trailing, and internal white space.
	// Throws an exception if the string is not understood by the factory.

	public static AbsRelTimeLocConverter make_abs_rel_conv (String description) {

		// Split into words

		String[] words = SimpleUtils.split_around_trim (description.trim());

		if (words.length == 0) {
			throw new IllegalArgumentException ("AbsRelTimeLocConverterFactory.make_abs_rel_conv: Missing object type: description = " + description);
		}

		// Switch on class type

		switch (words[0]) {

		// OEOrigin

		case ABS_REL_OEORIGIN_TEXT: {
			try {
				if (words.length != 5) {
					throw new IllegalArgumentException ("Incorrect number of words: Expecting 5, got " + words.length);
				}
				return new OEOrigin (
					SimpleUtils.string_or_number_to_time (words[1]),	// time
					Double.parseDouble (words[2]),						// latitude
					Double.parseDouble (words[3]),						// longitude
					Double.parseDouble (words[4])						// depth
				);
			}
			catch (Exception e) {
				throw new IllegalArgumentException ("AbsRelTimeLocConverterFactory.make_abs_rel_conv: Unable to parse description: description = " + description, e);
			}
		}

		}

		// Unrecognized type

		throw new IllegalArgumentException ("AbsRelTimeLocConverterFactory.make_abs_rel_conv: Unrecognized object type: description = " + description);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("AbsRelTimeLocConverterFactory : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  origin_event_id
		// Fetch origin event from Comcat, and display it.
		// Make the origin object, and display it.
		// Describe it, and display.
		// Make a new origin object, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("AbsRelTimeLocConverterFactory : Invalid 'test1' subcommand");
				return;
			}

			try {

				String origin_event_id = args[1];

				// Say hello

				System.out.println ("Testing description and creation of OEOrigin.");
				System.out.println ("origin_event_id = " + origin_event_id);

				// Load origin from Comcat

				boolean wrapLon = false;
				boolean extendedInfo = false;

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();
				ObsEqkRupture origin_rup = accessor.fetchEvent (origin_event_id, wrapLon, extendedInfo);

				System.out.println();
				System.out.println(ComcatOAFAccessor.rupToString (origin_rup));

				// Make the origin object

				AbsRelTimeLocConverter origin = (new OEOrigin()).set_from_rupture_horz(origin_rup);

				System.out.println();
				System.out.println(origin.toString());

				// Describe it

				String description = AbsRelTimeLocConverterFactory.describe_abs_rel_conv (origin);

				System.out.println();
				System.out.println(description);

				origin = null;

				// Make an origin object from the description

				AbsRelTimeLocConverter origin2 = AbsRelTimeLocConverterFactory.make_abs_rel_conv (description);

				System.out.println();
				System.out.println(origin2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("AbsRelTimeLocConverterFactory : Unrecognized subcommand : " + args[0]);
		return;

	}




}
