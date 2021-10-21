package org.opensha.oaf.util.catalog;

import org.opensha.oaf.util.SimpleUtils;


// Factory class to externalize and re-create RuptureLineFormatter objects.
// Author: Michael Barall 10/11/2021.
//
// For OEOrigin, the factory string consists of 5 words:
//   "etas_origin"  <time in ISO-8601 format>  <latitude>  <longitude>  <depth>
// When reading, the time can be given either in ISO-8601 format or as a number
// of milliseconds since the epoch.

public class RuptureLineFormatterFactory {

	// Identifiers for known types.

	public static final String LINE_FMT_GUI_LEGACY_TEXT = "gui_legacy";
	public static final String LINE_FMT_GUI_LEGACY_CLASS = "org.opensha.oaf.util.catalog.RuptureLineFormatGUILegacy";

	public static final String LINE_FMT_LOCAL_CATALOG_TEXT = "local_catalog";
	public static final String LINE_FMT_LOCAL_CATALOG_CLASS = "org.opensha.oaf.util.catalog.RuptureLineFormatLocalCatalog";

	public static final String LINE_FMT_GUI_OBSERVED_TEXT = "gui_observed";
	public static final String LINE_FMT_GUI_OBSERVED_CLASS = "org.opensha.oaf.util.catalog.RuptureLineFormatGUIObserved";




	// Get a string that describes a line formatter object.
	// Parameters:
	//  line_formatter = Line formatter object to be externalized.
	// Returns a string describing the formatter object.
	// The string can be passed to make_line_formatter to re-construct the formatter.
	// It should consist of a sequence of words, separated by a single space.
	// Preferably it should be human readable and understandable.
	// Throws an exception if the object is not understood by the factory.

	public static String describe_line_formatter (RuptureLineFormatter line_formatter) {

		// Get the class name

		String cname = line_formatter.getClass().getName();

		// Switch on class name

		switch (cname) {

		// RuptureLineFormatGUILegacy

		case LINE_FMT_GUI_LEGACY_CLASS: {
			RuptureLineFormatGUILegacy gui_legacy_formatter = (RuptureLineFormatGUILegacy)line_formatter;
			StringBuilder sb = new StringBuilder ();
			sb.append (LINE_FMT_GUI_LEGACY_TEXT);
			return sb.toString();
		}

		// RuptureLineFormatLocalCatalog

		case LINE_FMT_LOCAL_CATALOG_CLASS: {
			RuptureLineFormatLocalCatalog locat_formatter = (RuptureLineFormatLocalCatalog)line_formatter;
			StringBuilder sb = new StringBuilder ();
			sb.append (LINE_FMT_LOCAL_CATALOG_TEXT);
			return sb.toString();
		}

		// RuptureLineFormatGUIObserved

		case LINE_FMT_GUI_OBSERVED_CLASS: {
			RuptureLineFormatGUIObserved gui_obs_formatter = (RuptureLineFormatGUIObserved)line_formatter;
			StringBuilder sb = new StringBuilder ();
			sb.append (LINE_FMT_GUI_OBSERVED_TEXT);
			return sb.toString();
		}

		}

		// Unrecognized type

		throw new IllegalArgumentException ("RuptureLineFormatterFactory.describe_line_formatter: Unrecognized class name: " + cname);
	}




	// Make a line formatter object, given the description.
	// Parameters:
	//  description = String describing the formatter, usually created by describe_line_formatter.
	//  abs_rel_conv = An absolute/relative converter to be installed in the line formatter; can be null.
	// The string may be human-edited, and so this function should attempt to be
	// forgiving about the format, including leading, trailing, and internal white space.
	// Throws an exception if the string is not understood by the factory.

	public static RuptureLineFormatter make_line_formatter (String description, AbsRelTimeLocConverter abs_rel_conv) {

		// Split into words

		String[] words = SimpleUtils.split_around_trim (description.trim());

		if (words.length == 0) {
			throw new IllegalArgumentException ("RuptureLineFormatterFactory.make_line_formatter: Missing object type: description = " + description);
		}

		// Switch on class type

		switch (words[0]) {

		// RuptureLineFormatGUILegacy

		case LINE_FMT_GUI_LEGACY_TEXT: {
			try {
				if (words.length != 1) {
					throw new IllegalArgumentException ("Incorrect number of words: Expecting 1, got " + words.length);
				}
				return (new RuptureLineFormatGUILegacy ()).set_abs_rel_conv(abs_rel_conv);
			}
			catch (Exception e) {
				throw new IllegalArgumentException ("RuptureLineFormatterFactory.make_line_formatter: Unable to parse description: description = " + description, e);
			}
		}

		// RuptureLineFormatLocalCatalog

		case LINE_FMT_LOCAL_CATALOG_TEXT: {
			try {
				if (words.length != 1) {
					throw new IllegalArgumentException ("Incorrect number of words: Expecting 1, got " + words.length);
				}
				return (new RuptureLineFormatLocalCatalog ()).set_abs_rel_conv(abs_rel_conv);
			}
			catch (Exception e) {
				throw new IllegalArgumentException ("RuptureLineFormatterFactory.make_line_formatter: Unable to parse description: description = " + description, e);
			}
		}

		// RuptureLineFormatGUIObserved

		case LINE_FMT_GUI_OBSERVED_TEXT: {
			try {
				if (words.length != 1) {
					throw new IllegalArgumentException ("Incorrect number of words: Expecting 1, got " + words.length);
				}
				return (new RuptureLineFormatGUIObserved ()).set_abs_rel_conv(abs_rel_conv);
			}
			catch (Exception e) {
				throw new IllegalArgumentException ("RuptureLineFormatterFactory.make_line_formatter: Unable to parse description: description = " + description, e);
			}
		}

		}

		// Unrecognized type

		throw new IllegalArgumentException ("RuptureLineFormatterFactory.make_line_formatter: Unrecognized object type: description = " + description);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("RuptureLineFormatterFactory : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Make the RuptureLineFormatGUILegacy object, and display it.
		// Describe it, and display.
		// Make a new RuptureLineFormatGUILegacy object, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("RuptureLineFormatterFactory : Invalid 'test1' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Testing description and creation of RuptureLineFormatGUILegacy.");

				// Make the formatter object

				RuptureLineFormatter formatter = (new RuptureLineFormatGUILegacy()).set_abs_rel_conv(null);

				System.out.println();
				System.out.println(formatter.toString());

				// Describe it

				String description = RuptureLineFormatterFactory.describe_line_formatter (formatter);

				System.out.println();
				System.out.println(description);

				formatter = null;

				// Make an origin object from the description

				RuptureLineFormatter formatter2 = RuptureLineFormatterFactory.make_line_formatter (description, null);

				System.out.println();
				System.out.println(formatter2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Make the RuptureLineFormatLocalCatalog object, and display it.
		// Describe it, and display.
		// Make a new RuptureLineFormatLocalCatalog object, and display it.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("RuptureLineFormatterFactory : Invalid 'test2' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Testing description and creation of RuptureLineFormatLocalCatalog.");

				// Make the formatter object

				RuptureLineFormatter formatter = (new RuptureLineFormatLocalCatalog()).set_abs_rel_conv(null);

				System.out.println();
				System.out.println(formatter.toString());

				// Describe it

				String description = RuptureLineFormatterFactory.describe_line_formatter (formatter);

				System.out.println();
				System.out.println(description);

				formatter = null;

				// Make an origin object from the description

				RuptureLineFormatter formatter2 = RuptureLineFormatterFactory.make_line_formatter (description, null);

				System.out.println();
				System.out.println(formatter2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Make the RuptureLineFormatGUIObserved object, and display it.
		// Describe it, and display.
		// Make a new RuptureLineFormatGUIObserved object, and display it.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("RuptureLineFormatterFactory : Invalid 'test3' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Testing description and creation of RuptureLineFormatGUIObserved.");

				// Make the formatter object

				RuptureLineFormatter formatter = (new RuptureLineFormatGUIObserved()).set_abs_rel_conv(null);

				System.out.println();
				System.out.println(formatter.toString());

				// Describe it

				String description = RuptureLineFormatterFactory.describe_line_formatter (formatter);

				System.out.println();
				System.out.println(description);

				formatter = null;

				// Make an origin object from the description

				RuptureLineFormatter formatter2 = RuptureLineFormatterFactory.make_line_formatter (description, null);

				System.out.println();
				System.out.println(formatter2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("RuptureLineFormatterFactory : Unrecognized subcommand : " + args[0]);
		return;

	}

}
