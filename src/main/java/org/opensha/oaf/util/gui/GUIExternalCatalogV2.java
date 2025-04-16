package org.opensha.oaf.util.gui;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.TimeZone;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import java.time.format.DateTimeParseException;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;

import org.opensha.oaf.util.catalog.ObsEqkRupMinTimeComparator;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.NamedValue;
import org.opensha.oaf.util.TestArgs;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.LineSupplierFile;
import org.opensha.oaf.util.LineConsumerFile;

import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.commons.data.comcat.ComcatException;


// Read/write the external catalog file used by the GUI, version 2.
// Author: Michael Barall.
//
// This class can be used to read, write, and store an external catalog
// for use by the GUI, or more generally for computing forecasts.
//
// An object of this class contains a set of earthquake lists.
// Each list contains one catagory of earthquakes.  The default catagories
// are mainshock, foreshock, aftershock, and regional, but it is possible
// to add more catagores.  Each catagory name must consist of alphanumeric
// characters and underscore, and begin with a letter or underscore.
//
// The object also contains a list of symbol definitions of the form
// name = value.  A symbol name must consist of alphanumeric characters
// and underscore, and begin with a letter or underscore.  A symbol value
// is a string which cannot contain ASCII control characters (\x00-\x1F),
// and cannot begin or end with a space.
//
// The file format is as follows.  First comes the symbol definitions,
// in this form:
//   #define: name = value
//
// Next come a series of comments which give the total number of earthquakes
// in the file and the number within each catagory, in the form:
//   #count: name n
// The name is the name of a catagory or "total" for the total number.
// Only catagories with a non-zero number of earthquakes are listed.
//
// Next come the lists of earthquakes for each catagory.  Before each list
// is one line containing the name of the catagory, in this form:
//   #begin: name
// Then comes a comment which shows the names of the columes:
//   #  Date      Time     Mag       Lat         Lon       Depth       ID
// Then comes a series of earthquakes, one per line.  For example:
//   2019-07-06 03:19:53   7.1     35.7695   -117.59933     8.0    ci38457511
//
// When reading the file, any line that begins with # and does not have the
// form of a symbol definition or category selection is ignored.  completely
// blank lines are also ignored.

public class GUIExternalCatalogV2 {


	//----- External file contents -----


	// The earthquakes listed in the file.
	// Each key is the name of a category of earthquake (mainshock, aftershock, etc.).
	// Each value is a list of earthquakes.

	public Map<String, ObsEqkRupList> earthquakes;


	// Standard names for category of earthquake.

	public static final String EQCAT_MAINSHOCK = "mainshock";
	public static final String EQCAT_FORESHOCK = "foreshock";
	public static final String EQCAT_AFTERSHOCK = "aftershock";
	public static final String EQCAT_REGIONAL = "regional";

	public static final String EQCAT_TOTAL = "total";	// fictitious category used to report total number of earthquakes (don't create a category with this name)


	// The symbols listed in the file.
	// Each key is the name of a symbol (should be a valid Java identifier).
	// Each value is the value of that symbol, as a string.
	// Values do not have leading/trailing white space (as defined for String.trim())
	// and do not contain embedded ASCII control characters (x00-x1F).
	// A value can be an empty string, but cannot be null.

	public Map<String, String> symbols;


	//----- Internal variables used while parsing -----


	// The current category, or null if not specified, used while parsing (init to null).

	private String current_category;

	// The current line number, used while parsing (init to 0).

	private int current_line_number;


	//----- Internal variables used while building -----


	// Time for auto-classification, used while building (init to Long.MIN_VALUE).
	// Earthquakes before this time are classified as foreshocks.

	private long classification_time;

	// Region for auto-classification, used while building (init to null).
	// If non-null, then earthquakes outside this region are classified as regional.

	private SphRegion classification_region;




	//----- Access -----




	// Return true if there is a symbol with the given name.

	public final boolean has_symbol (String name) {
		return symbols.containsKey (name);
	}


	// Get the value of a symbol, or null if the symbol is not defined.

	public final String get_symbol (String name) {
		return symbols.get (name);
	}


	// Get the value of a symbol, as a non-null string.
	// Throws exception if symbol is not defined or has invalid value.

	public final String get_symbol_as_string (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_string: Undefined symbol: " + name);
		}
		return value;
	}


	// Get the value of a symbol, as a non-null non-empty string.
	// Throws exception if symbol is not defined or has invalid value.

	public final String get_symbol_as_nonempty_string (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_nonempty_string: Undefined symbol: " + name);
		}
		if (value.isEmpty()) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_nonempty_string: Found empty string for symbol: " + name);
		}
		return value;
	}


	// Get the value of a symbol, as a double.
	// Throws exception if symbol is not defined or has invalid value.

	public final double get_symbol_as_double (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_double: Undefined symbol: " + name);
		}
		double result;
		try {
			result = Double.parseDouble (value);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_double: Invalid symbol: name = " + name + ", value = " + value);
		}
		return result;
	}


	// Get the value of a symbol, as an  int.
	// Throws exception if symbol is not defined or has invalid value.

	public final int get_symbol_as_int (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_int: Undefined symbol: " + name);
		}
		int result;
		try {
			result = Integer.parseInt (value);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_int: Invalid symbol: name = " + name + ", value = " + value);
		}
		return result;
	}


	// Get the value of a symbol, as a long.
	// Throws exception if symbol is not defined or has invalid value.

	public final long get_symbol_as_long (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_long: Undefined symbol: " + name);
		}
		long result;
		try {
			result = Long.parseLong (value);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_long: Invalid symbol: name = " + name + ", value = " + value);
		}
		return result;
	}


	// Get the value of a symbol, as a boolean.
	// Throws exception if symbol is not defined or has invalid value.

	public final boolean get_symbol_as_boolean (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_boolean: Undefined symbol: " + name);
		}
		boolean result;
		try {
			result = Boolean.parseBoolean (value);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_boolean: Invalid symbol: name = " + name + ", value = " + value);
		}
		return result;
	}




	// Get the list of ruptures for a given category.
	// Retuns the list of ruptures.
	// If the category has not been defined, returns an empty list (hence never returns null).

	public final ObsEqkRupList get_rup_list (String category) {
		ObsEqkRupList rups = earthquakes.get (category);
		if (rups == null) {
			rups = new ObsEqkRupList();
		}
		return rups;
	}


	// Get a list that contains all the ruptures in all the given categories.
	// If f_sort is true, the combined list is sorted in increasing order of time.

	public final ObsEqkRupList get_joined_rup_list (boolean f_sort, String... categories) {
		ObsEqkRupList rups = new ObsEqkRupList();
		for (String category : categories) {
			rups.addAll (get_rup_list (category));
		}
		if (f_sort) {
			sort_aftershocks (rups);
		}
		return rups;
	}


	// Get the number of earthquakes in the given category.
	// If the category has not been defined, returns 0.

	public final int get_rup_list_size (String category) {
		ObsEqkRupList rups = earthquakes.get (category);
		if (rups == null) {
			return 0;
		}
		return rups.size();
	}


	// Get the total number of earthquakes in the file.

	public final int get_total_size () {
		int total = 0;
		for (Map.Entry<String, ObsEqkRupList> entry : earthquakes.entrySet()) {
			total += entry.getValue().size();
		}
		return total;
	}


	// Throw an exception if there is not exactly one mainshock.

	public final void check_single_mainshock () {
		int main_count = get_rup_list(EQCAT_MAINSHOCK).size();
		if (main_count < 1) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.check_single_mainshock: No mainshock found");
		}
		if (main_count > 1) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.check_single_mainshock: More than one mainshock found");
		}
		return;
	}


	// Get the mainshock.
	// Throw an exception if there is not exactly one mainshock.

	public final ObsEqkRupture get_mainshock () {
		ObsEqkRupList main_list = get_rup_list (EQCAT_MAINSHOCK);
		int main_count = main_list.size();
		if (main_count < 1) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.get_mainshock: No mainshock found");
		}
		if (main_count > 1) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.get_mainshock: More than one mainshock found");
		}
		return main_list.get(0);
	}




	//----- Parsing and formatting -----




	// Pattern to recognize a valid name (for a symbol or category).
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern name_pattern = Pattern.compile ("[a-zA-Z_][a-zA-Z_0-9]*");


	// Check if the given name is valid.
	// Returns the name.
	// Throws exception if the name is not valid.

	public static String check_name (String name) {
		Matcher matcher = name_pattern.matcher (name);
		if (!( matcher.matches() )) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.check_name: Invalid name: " + name);
		}
		return name;
	}




	//--- Comment




	// Pattern to recognize a comment line.

	private static final Pattern comment_line_pattern = Pattern.compile ("[ \\t]*(?:#[^\\n\\r]*)?[\\n\\r]*");


	// Return true if the given line is a comment line.

	public static boolean is_comment_line (String line) {
		return comment_line_pattern.matcher(line).matches();
	}




	//--- Symbol definition




	// Pattern to recognize a line in the form of a symbol definition.

	private static final Pattern symdef_form_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*define[ \\t]*:.*");

	// Pattern to recognize a line containing a symbol definition (value is non-empty).

	private static final Pattern symdef_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*define[ \\t]*:[ \\t]*([a-zA-Z_][a-zA-Z_0-9]*)[ \\t]*=[ \\t]*([^\\x00-\\x20]|[^\\x00-\\x20][^\\x00-\\x1F]*[^\\x00-\\x20])[ \\t]*[\\n\\r]*");

	// Pattern to recognize a line containing a symbol undefinition (value is empty).

	private static final Pattern symdef_undef_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*define[ \\t]*:[ \\t]*([a-zA-Z_][a-zA-Z_0-9]*)[ \\t]*(?:=[ \\t]*)?[\\n\\r]*");

	// Capture group for the symbol name.

	private static final int symdef_name_capture_group = 1;

	// Capture group for the symbol value.

	private static final int symdef_value_capture_group = 2;

	// Pattern to recognize a valid symbol value.

	private static final Pattern symdef_value_pattern = Pattern.compile ("(?:[^\\x00-\\x20]|[^\\x00-\\x20][^\\x00-\\x1F]*[^\\x00-\\x20])?");


	// Return true if the given line has the form of a symbol definition.

	public static boolean is_symdef_form (String line) {
		return symdef_form_line_pattern.matcher(line).matches();
	}


	// Check if the given symbol value name is valid.
	// Returns the value.
	// Throws exception if the value is not valid.

	public static String symdef_check_value (String value) {
		Matcher matcher = symdef_value_pattern.matcher (value);
		if (!( matcher.matches() )) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.symdef_check_value: Invalid symbol value: " + value);
		}
		return value;
	}


	// Format a symbol definition.

	public static String symdef_format_line (String name, String value) {
		check_name (name);
		String my_value = symdef_check_value (value.trim());

		StringBuilder result = new StringBuilder();
		result.append ("#define: ");
		result.append (name);
		if (my_value.isEmpty()) {
			result.append (" =");
		} else {
			result.append (" = ");
			result.append (my_value);
		}
		return result.toString();
	}


	// Format a symbol definition.

	public static String symdef_format_line (NamedValue<String> named_value) {
		return symdef_format_line (named_value.get_name(), named_value.get_value());
	}


	// Parse a line which may contain a symbol definition.
	// Returns a named value if the line was parsed.
	// Returns null if the line does not have the form of symbol definition.
	// Throws exception if error.

	public NamedValue<String> symdef_parse_line (String line) {
		if (!( is_symdef_form (line) )) {
			return null;
		}
		Matcher matcher = symdef_line_pattern.matcher (line);
		if (matcher.matches()) {
			return new NamedValue<String> (matcher.group (symdef_name_capture_group), matcher.group (symdef_value_capture_group));
		}
		matcher = symdef_undef_line_pattern.matcher (line);
		if (matcher.matches()) {
			return new NamedValue<String> (matcher.group (symdef_name_capture_group), "");
		}
		throw new IllegalArgumentException ("GUIExternalCatalogV2.symdef_parse_line: Symbol definition has invalid form: line = " + line);
	}




	//--- Category selection




	// Pattern to recognize a line in the form of a category selection.

	private static final Pattern catsel_form_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*begin[ \\t]*:.*");

	// Pattern to recognize a line containing a category selection.

	private static final Pattern catsel_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*begin[ \\t]*:[ \\t]*([a-zA-Z_][a-zA-Z_0-9]*)[ \\t]*[\\n\\r]*");

	// Capture group for the category name.

	private static final int catsel_name_capture_group = 1;


	// Return true if the given line has the form of a symbol definition.

	public static boolean is_catsel_form (String line) {
		return catsel_form_line_pattern.matcher(line).matches();
	}


	// Format a category selection.

	public static String catsel_format_line (String name) {
		check_name (name);

		StringBuilder result = new StringBuilder();
		result.append ("#begin: ");
		result.append (name);
		return result.toString();
	}


	// Parse a line which may contain a category selection.
	// Returns a category name if the line was parsed.
	// Returns null if the line does not have the form of category selection.
	// Throws exception if error.

	public String catsel_parse_line (String line) {
		if (!( is_catsel_form (line) )) {
			return null;
		}
		Matcher matcher = catsel_line_pattern.matcher (line);
		if (matcher.matches()) {
			return matcher.group (catsel_name_capture_group);
		}
		throw new IllegalArgumentException ("GUIExternalCatalogV2.catsel_parse_line: Category selection has invalid form: line = " + line);
	}




	//--- Earthquake




	// Pattern to recognize a line in the form of an earthquake.
	// At present, any non-blank line not beginning with # is assumed to be an earthquake.

	private static final Pattern quake_form_line_pattern = Pattern.compile ("[ \\t]*[^ #\\t\\n\\r].*");

	// Pattern to recognize a line containing an earthquake.
	// Note: A Pattern is an immutable object that can be used by multiple threads.

	private static final Pattern quake_line_pattern = Pattern.compile ("[ \\t]*(\\d\\d\\d\\d)-(\\d\\d?)-(\\d\\d?)[ \\t]+(\\d\\d?):(\\d\\d?):(\\d\\d?)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]*([a-zA-Z_0-9]*)[ \\t]*[\\n\\r]*");

	// Capture groups for the earthquake description.

	private static final int quake_year_capture_group = 1;
	private static final int quake_month_capture_group = 2;
	private static final int quake_day_capture_group = 3;

	private static final int quake_hour_capture_group = 4;
	private static final int quake_minute_capture_group = 5;
	private static final int quake_second_capture_group = 6;

	private static final int quake_mag_capture_group = 7;

	private static final int quake_lat_capture_group = 8;
	private static final int quake_lon_capture_group = 9;
	private static final int quake_depth_capture_group = 10;

	private static final int quake_id_capture_group = 11;

	// Pattern to recognize a valid event id.

	private static final Pattern quake_id_pattern = Pattern.compile ("[a-zA-Z_0-9]*");


	// Return true if the given earthquake id is valid.

	public static boolean is_quake_id_valid (String id) {
		return quake_id_pattern.matcher(id).matches();
	}


	// Return true if the given line has the form of an earthquake line.

	public static boolean is_quake_form (String line) {
		return quake_form_line_pattern.matcher(line).matches();
	}


	// Parse a line which may contain an earthquake.
	// Returns the earthquake if parse successful.
	// Returns null if the line does not have the form of an earthquake.
	// Throws exception if error.

	public static ObsEqkRupture quake_parse_line (String line) {
		if (!( is_quake_form (line) )) {
			return null;
		}

		Matcher matcher = quake_line_pattern.matcher (line);
		if (!( matcher.matches() )) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Invalid earthquake data format for line: " + line);
		}

		// Get the parts of the line

		String year = matcher.group (quake_year_capture_group);
		String month = matcher.group (quake_month_capture_group);
		String day = matcher.group (quake_day_capture_group);

		String hour = matcher.group (quake_hour_capture_group);
		String minute = matcher.group (quake_minute_capture_group);
		String second = matcher.group (quake_second_capture_group);

		String mag = matcher.group (quake_mag_capture_group);

		String lat = matcher.group (quake_lat_capture_group);
		String lon = matcher.group (quake_lon_capture_group);
		String depth = matcher.group (quake_depth_capture_group);

		String id = matcher.group (quake_id_capture_group);

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
		if (second.length() == 1) {
			daytime.append ("0");
		}
		daytime.append (second);
		daytime.append ("Z");

		long time;
		try {
			time = SimpleUtils.string_to_time (daytime.toString());
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Date/time parse error parsing time for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Error parsing time for line: " + line, e);
		}

		// Convert magnitude

		double r_mag;
		try {
			r_mag = Double.parseDouble (mag);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Numeric conversion error parsing magnitude for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Error parsing magnitude for line: " + line, e);
		}

		// Convert location

		Location hypo;
		try {
			hypo = new Location(Double.parseDouble (lat), Double.parseDouble (lon), Double.parseDouble (depth));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Numeric conversion error parsing location for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Error parsing location for line: " + line, e);
		}

		// Make the result

		ObsEqkRupture result;
		try {
			result = new ObsEqkRupture (id, time, hypo, r_mag);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Error forming earthquake rupture object for line: " + line, e);
		}

		return result;
	}


	// Format a line containing an earthquake description.
	// Line format, and number of columns:
	//   date   time   mag   lat   lon   depth   id
	//    10  1  8   2  6  2  9  2  10 2   8   2 1+

	public static String quake_format_line (ObsEqkRupture rup) {
		StringBuilder result = new StringBuilder();
		Location hypo = rup.getHypocenterLocation();
		result.append (SimpleUtils.time_to_string_no_z (rup.getOriginTime()));
		result.append ("  ");
		result.append (SimpleUtils.double_to_string_trailz ("%6.3f", SimpleUtils.TRAILZ_PAD_RIGHT, rup.getMag()));
		result.append ("  ");
		result.append (SimpleUtils.double_to_string_trailz ("%9.5f", SimpleUtils.TRAILZ_PAD_RIGHT, hypo.getLatitude()));
		result.append ("  ");
		double lon = hypo.getLongitude();
		if (lon > 180.0) {
			lon -= 360.0;
		}
		result.append (SimpleUtils.double_to_string_trailz ("%10.5f", SimpleUtils.TRAILZ_PAD_RIGHT, lon));
		result.append ("  ");
		result.append (SimpleUtils.double_to_string_trailz ("%8.3f", SimpleUtils.TRAILZ_PAD_RIGHT, hypo.getDepth()));
		String id = rup.getEventId();
		if (id != null) {
			id = id.trim();
			if (id.length() > 0 && is_quake_id_valid (id)) {
				result.append ("  ");
				result.append (id);
			}
		}
		return result.toString();
	}


	// Format a comment line containing column headers for earthquake description.
	// Line format, and number of columns:
	//   date   time   mag   lat   lon   depth   id
	//    10  1  8   2  6  2  9  2  10 2   8   2 1+

	public static String quake_format_header () {
		StringBuilder result = new StringBuilder();
		result.append ("#  Date   ");
		result.append (" ");
		result.append ("  Time  ");
		result.append ("  ");
		result.append (" Mag  ");
		result.append ("  ");
		result.append ("   Lat   ");
		result.append ("  ");
		result.append ("    Lon   ");
		result.append ("  ");
		result.append ("  Depth ");
		result.append ("  ");
		result.append ("    ID");
		return result.toString();
	}




	//--- Count




	// Format a count report.

	public static String count_format_line (String name, int count) {
		check_name (name);

		StringBuilder result = new StringBuilder();
		result.append ("#count: ");
		result.append (name);
		result.append (" ");
		result.append (Integer.toString (count));
		return result.toString();
	}




	//----- Building -----




	// Clear the contents to an empty file.
	// Creates the default categories.
	// Initializes for parse or build.

	public final void clear () {
		earthquakes = new LinkedHashMap<String, ObsEqkRupList>();
		symbols = new LinkedHashMap<String, String>();

		earthquakes.put (EQCAT_MAINSHOCK, new ObsEqkRupList());
		earthquakes.put (EQCAT_FORESHOCK, new ObsEqkRupList());
		earthquakes.put (EQCAT_AFTERSHOCK, new ObsEqkRupList());
		earthquakes.put (EQCAT_REGIONAL, new ObsEqkRupList());

		current_category = null;
		current_line_number = 0;
		classification_time = Long.MIN_VALUE;
		classification_region = null;

		return;
	}




	// Constructor sets up an empty file.

	public GUIExternalCatalogV2 () {
		clear();
	}




	//--- Symbol definitions




	// Add a symbol definition.
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add (String name, String value) {
		check_name (name);
		String my_value = symdef_check_value (value.trim());
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition.
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add (NamedValue<String> named_value) {
		symdef_add (named_value.get_name(), named_value.get_value());
		return;
	}


	// Add a symbol definition, with the value given as a double
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_double (String name, double value) {
		check_name (name);
		String my_value = symdef_check_value (Double.toString(value));
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition, with the value given as an int
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_int (String name, int value) {
		check_name (name);
		String my_value = symdef_check_value (Integer.toString(value));
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition, with the value given as a long
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_long (String name, long value) {
		check_name (name);
		String my_value = symdef_check_value (Long.toString(value));
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition, with the value given as a boolean
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_boolean (String name, boolean value) {
		check_name (name);
		String my_value = symdef_check_value (Boolean.toString(value));
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}




	//--- Earthquakes and Categories




	// Add an earthquake for the category with the given name.
	// A new category is created if the given category has net been seen before.

	public final void add_quake (String category, ObsEqkRupture rup) {
		check_name (category);
		ObsEqkRupList rup_list = earthquakes.get (category);
		if (rup_list == null) {
			rup_list = new ObsEqkRupList();
			earthquakes.put (category, rup_list);
		}
		rup_list.add (rup);
		return;
	}




	// Classify an earthquake, according to auto-classification.
	// Returns the category (one of foreshock, aftershock, or regional).

	public final String classify_quake (ObsEqkRupture rup) {
		if (classification_region != null) {
			if (!( classification_region.contains (rup.getHypocenterLocation()) )) {
				return EQCAT_REGIONAL;
			}
		}
		if (rup.getOriginTime() < classification_time) {
			return EQCAT_FORESHOCK;
		}
		return EQCAT_AFTERSHOCK;
	}




	// Add an earthquake, with auto-classification to select a category.
	// Returns the category (one of foreshock, aftershock, or regional).

	public final String classify_and_add_quake (ObsEqkRupture rup) {
		String category = classify_quake (rup);
		add_quake (category, rup);
		return category;
	}




	// Set up classification by supplying a mainshock and optional region.
	// Parameters:
	//  mainshock = The mainshock.
	//  region = Classification region to identify regional earthquakes, or null if none.
	// The mainshock is added to the file.
	// The classification time is set to the mainshock origin time.

	public final void setup_classification (ObsEqkRupture mainshock, SphRegion region) {
		add_quake (EQCAT_MAINSHOCK, mainshock);
		classification_time = mainshock.getOriginTime();
		classification_region = region;
		return;
	}




	// Classify and add all the earthquakes.
	// Parameters:
	//  mainshock = The mainshock.
	//  aftershocks = List of aftershocks (may include foreshocks and regional earthquakes).
	//  region = Classification region to identify regional earthquakes, or null if none.
	// Also sorts all the earthquakes.

	public final void add_all_quakes (ObsEqkRupture mainshock, List<ObsEqkRupture> aftershocks, SphRegion region) {
		setup_classification (mainshock, region);
		aftershocks.forEach ((ObsEqkRupture rup) -> classify_and_add_quake (rup));
		sort_all_earthquakes();
		return;
	}




	// Initialize for parsing a series of lines.

//	public final void parse_line_init () {
//		current_category = null;
//		current_line_number = 0;
//		classification_time = Long.MIN_VALUE;
//		classification_region = null;
//		return;
//	}




	//--- Parsing and formatting




	// Parse a line, and add it to the contents.
	// Throws exception if line does not parse.

	public final void parse_line (String line) {

		// First count the line

		++current_line_number;

		// Handle earthquake

		try {
			ObsEqkRupture rup = quake_parse_line (line);
			if (rup != null) {
				if (current_category == null) {
					throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": found earthquake with no category selected");
				}
				add_quake (current_category, rup);
				return;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": failed to parse earthquake", e);
		}

		// Handle symbol definition

		try {
			NamedValue<String> named_value = symdef_parse_line (line);
			if (named_value != null) {
				symdef_add (named_value);
				return;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": failed to parse symbol definition", e);
		}

		// Handle category selection

		try {
			String category = catsel_parse_line (line);
			if (category != null) {
				current_category = category;
				return;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": failed to parse category selection", e);
		}

		// Handle any comment not recognized as special purpose

		try {
			boolean is_comment = is_comment_line (line);
			if (is_comment) {
				return;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": failed to parse comment line", e);
		}

		// Cannot recognize the line
			
		throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": Unknown line format: line = " + line);
	}




	// Format all the lines, and send them to the destination.

	public final void format_all_lines (final Consumer<String> dest) {

		// Symbol definitions

		//BiConsumer<String, String> symdef_consumer = (String name, String value) -> {
		//	dest.accept (symdef_format_line (name, value));
		//};
		//
		//symbols.forEach (symdef_consumer);

		symbols.forEach ((String name, String value) -> dest.accept (symdef_format_line (name, value)));

		// Earthquake counts

		dest.accept (count_format_line (EQCAT_TOTAL, get_total_size()));

		BiConsumer<String, ObsEqkRupList> count_consumer = (String category, ObsEqkRupList rups) -> {
			if (rups.size() > 0) {
				dest.accept (count_format_line (category, rups.size()));
			}
		};

		earthquakes.forEach (count_consumer);

		// Categories and earthquakes

		BiConsumer<String, ObsEqkRupList> catsel_consumer = (String category, ObsEqkRupList rups) -> {
			if (rups.size() > 0) {
				dest.accept (catsel_format_line (category)); 
				dest.accept (quake_format_header());
				rups.forEach ((ObsEqkRupture rup) -> dest.accept (quake_format_line (rup)));
			}
		};

		earthquakes.forEach (catsel_consumer);

		return;
	}




	//--- Manipulation




	// Sort a list of aftershocks in order of increasing time.

	public static void sort_aftershocks (List<ObsEqkRupture> aftershock_list) {
		aftershock_list.sort (new ObsEqkRupMinTimeComparator());
		return;
	}




	// Sort each category of earthquakes in order of increasing time.

	public final void sort_all_earthquakes () {
		earthquakes.forEach ((String category, ObsEqkRupList rups) -> sort_aftershocks (rups));
		return;
	}




	// Copy a rupture, and wrap longitude to desired range.
	// Parameters:
	//  rup = Rupture to copy.
	//  wrapLon = Desired longitude range: false = -180 to 180; true = 0 to 360.
	// Returns a new rupture object, with longitude adjusted.
	// Note: Extended info, if any, is not copied.

	public static ObsEqkRupture copy_wrap_rup (ObsEqkRupture rup, boolean wrapLon) {

		// Extract fields from rupture

		String rup_event_id = rup.getEventId();
		long rup_time = rup.getOriginTime();
		double rup_mag = rup.getMag();
		Location hypo = rup.getHypocenterLocation();
		double rup_lat = hypo.getLatitude();
		double rup_lon = hypo.getLongitude();
		double rup_depth = hypo.getDepth();

		// Adjust longitude if necessary

		if (wrapLon) {
			if (rup_lon < 0.0) {
				rup_lon += 360.0;
			}
		} else {
			if (rup_lon > 180.0) {
				rup_lon -= 360.0;
			}
		}

		hypo = new Location (rup_lat, rup_lon, rup_depth);

		// Make the new rupture object

		return new ObsEqkRupture (rup_event_id, rup_time, hypo, rup_mag);
	}




	// Copy a rupture list, and wrap longitude to desired range in each rupture.
	// Parameters:
	//  rup_list = Rupture list to copy.
	//  wrapLon = Desired longitude range: false = -180 to 180; true = 0 to 360.
	// Returns a new rupture list, containing new rupture objects, with longitude adjusted.
	// Note: Extended info, if any, is not copied.

	public static ObsEqkRupList copy_wrap_rup_list (List<ObsEqkRupture> rup_list, boolean wrapLon) {
		ObsEqkRupList result = new ObsEqkRupList();
		for (ObsEqkRupture rup : rup_list) {
			result.add (copy_wrap_rup (rup, wrapLon));
		}
		return result;
	}




	//----- Read/Write -----
	



	// Read the catalog from a file.
	// Parameters:
	//  filename = Name of file to read.
	// Throws an exception if I/O or parse error.

	public void read_from_file (final String filename) {
		read_from_file (new File(filename));
		return;
	}




	// Read the catalog from a file.
	// Parameters:
	//  the_file = File to read.
	// Throws an exception if I/O or parse error.

	public void read_from_file (final File the_file) {

		// Start with an empty catalog and initialize parsing variables

		clear();

		// Open the file ...

		try (
			BufferedReader br = new BufferedReader (new FileReader (the_file));
		){

			// Loop over lines in file

			for (String line = br.readLine(); line != null; line = br.readLine()) {

				// Parse the line, throw exception if error

				parse_line (line);
			}
		}
		catch (IOException e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_file: I/O error reading catalog file: " + the_file.getPath(), e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_file: Parsing error reading catalog file: " + the_file.getPath(), e);
		}

		// Any final steps would be done here
	
		return;
	}




	// Read the catalog from a string.
	// Parameters:
	//  the_string = String to read.
	// Throws an exception if I/O or parse error.

	public void read_from_string (final String the_string) {

		// Start with an empty catalog and initialize parsing variables

		clear();

		// Split the string into lines

		String[] split = the_string.split ("\\r?\\n");

		try {

			// Loop over lines in string

			for (String line : split) {

				// Parse the line, throw exception if error

				parse_line (line);
			}
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_string: Parsing error reading catalog file from string.", e);
		}

		// Any final steps would be done here
	
		return;
	}




	// Read the catalog from a line supplier.
	// Parameters:
	//  line_supplier = Source of lines.

	public void read_from_supplier (Supplier<String> line_supplier) {

		// Start with an empty catalog and initialize parsing variables

		clear();

		try {

			// Loop over lines

			for (String line = line_supplier.get(); line != null; line = line_supplier.get()) {

				// Parse the line, throw exception if error

				parse_line (line);
			}
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_supplier: Error reading catalog file", e);
		}

		// Any final steps would be done here
	
		return;
	}




	// Write the catalog to a file.
	// Parameters:
	//  filename = Name of file to write.
	// Throws an exception if I/O or other error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public void write_to_file (String filename) {
		write_to_file (new File(filename));
		return;
	}




	// Write the catalog to a file.
	// Parameters:
	//  the_file = File to write.
	// Throws an exception if I/O or other error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public void write_to_file (final File the_file) {

		// Open the file ...

		try (
			final BufferedWriter bw = new BufferedWriter (new FileWriter (the_file));
		){
			// Format all the lines

			Consumer<String> line_consumer = (String line) -> {
				try {
					bw.write (line);
					bw.write ("\n");
				}
				catch (IOException e) {
					throw new RuntimeException ("GUIExternalCatalogV2.write_to_file: I/O error writing catalog file: " + the_file.getPath(), e);
				}
			};

			format_all_lines (line_consumer);
		}
		catch (IOException e) {
			throw new RuntimeException ("GUIExternalCatalogV2.write_to_file: I/O error writing catalog file: " + the_file.getPath(), e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.write_to_file: Error writing catalog file: " + the_file.getPath(), e);
		}
	
		return;
	}




	// Write the catalog to a string.
	// Returns a string containing the entire catalog.
	// Throws an exception if error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public String write_to_string () {
		final StringBuilder sb = new StringBuilder();

		try {

			// Format all the lines

			Consumer<String> line_consumer = (String line) -> {
				sb.append (line);
				sb.append ("\n");
			};

			format_all_lines (line_consumer);
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.write_to_string: Error writing catalog file", e);
		}
	
		return sb.toString();
	}




	// Write the catalog to a line consumer.
	// Parameters:
	//  line_consumer = Consumer to write.
	// Throws an exception if error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public void write_to_consumer (Consumer<String> line_consumer) {

		try {

			// Format all the lines

			format_all_lines (line_consumer);
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.write_to_consumer: Error writing catalog file", e);
		}
	
		return;
	}




	//----- Testing -----




	// Read a file from Comcat.

	public static GUIExternalCatalogV2 test_read_from_comcat (String event_id, double min_days, double max_days, double min_mag, double radius_km, double regional_radius_km) {
		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Get mainshock

		boolean wrapLon = false;
		boolean extendedInfo = true;
		boolean superseded = false;

		ObsEqkRupture mainshock = accessor.fetchEvent (event_id, wrapLon, extendedInfo, superseded);

		if (mainshock == null) {
			throw new RuntimeException ("GUIExternalCatalogV2.test_read_from_comcat: Did not find mainshock: event_id = " + event_id);
		}

		// Form Comcat search region and classification region.

		SphRegion comcat_region = null;
		SphRegion aftershock_region = null;

		SphLatLon sph_hypo = new SphLatLon (mainshock.getHypocenterLocation());

		if (regional_radius_km > radius_km) {
			comcat_region = SphRegion.makeCircle (sph_hypo, regional_radius_km);
			aftershock_region = SphRegion.makeCircle (sph_hypo, radius_km);
		} else {
			comcat_region = SphRegion.makeCircle (sph_hypo, radius_km);
		}

		// Get list of aftershocks

		double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

		ObsEqkRupList aftershocks = accessor.fetchAftershocks (mainshock, min_days, max_days,
			minDepth, maxDepth, comcat_region, wrapLon, min_mag);

		// Load into catalog

		GUIExternalCatalogV2 catalog = new GUIExternalCatalogV2();
		catalog.add_all_quakes (mainshock, aftershocks, aftershock_region);

		// Add some symbols

		catalog.symdef_add ("event_id", event_id);
		catalog.symdef_add_double ("min_days", min_days);
		catalog.symdef_add_double ("max_days", max_days);
		catalog.symdef_add_double ("min_mag", min_mag);
		catalog.symdef_add_double ("radius_km", radius_km);
		catalog.symdef_add_double ("regional_radius_km", regional_radius_km);

		catalog.symdef_add_double ("center_lat", comcat_region.getCircleCenterLat());
		catalog.symdef_add_double ("center_lon", comcat_region.getCircleCenterLon());

		catalog.symdef_add ("TestEmpty", "");
		catalog.symdef_add ("TestX", "X");
		catalog.symdef_add ("Test123", "one two three");

		return catalog;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "GUIExternalCatalogV2");




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  min_days  max_days  min_mag  radius_km  regional_radius_km
		// Fetch catalog data from Comcat and fill in a GUIExternalCatalogV2.
		// Write to string and display it.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing GUIExternalCatalogV2");
			String event_id = testargs.get_string ("event_id");
			double min_days = testargs.get_double ("min_days");
			double max_days = testargs.get_double ("max_days");
			double min_mag = testargs.get_double ("min_mag");
			double radius_km = testargs.get_double ("radius_km");
			double regional_radius_km = testargs.get_double ("regional_radius_km");
			testargs.end_test();

			// Fetch catalog from comcat

			System.out.println ();
			System.out.println ("***** Fetching data from Comcat *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog = test_read_from_comcat (event_id, min_days, max_days, min_mag, radius_km, regional_radius_km);

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string *****");
			System.out.println ();

			String catalog_str = catalog.write_to_string();

			System.out.println (catalog_str);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id  min_days  max_days  min_mag  radius_km  regional_radius_km
		// Fetch catalog data from Comcat and fill in a GUIExternalCatalogV2.
		// Write to string and display it.
		// Then read the string into a second GUIExternalCatalogV2.
		// Write to a second string and display it.
		// Compare the two strings - they should be equal.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Constructing GUIExternalCatalogV2 and testing string I/O");
			String event_id = testargs.get_string ("event_id");
			double min_days = testargs.get_double ("min_days");
			double max_days = testargs.get_double ("max_days");
			double min_mag = testargs.get_double ("min_mag");
			double radius_km = testargs.get_double ("radius_km");
			double regional_radius_km = testargs.get_double ("regional_radius_km");
			testargs.end_test();

			// Fetch catalog from comcat

			System.out.println ();
			System.out.println ("***** Fetching data from Comcat *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog = test_read_from_comcat (event_id, min_days, max_days, min_mag, radius_km, regional_radius_km);

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string *****");
			System.out.println ();

			String catalog_str = catalog.write_to_string();

			System.out.println (catalog_str);

			// Read from string

			System.out.println ();
			System.out.println ("***** Reading from string *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog_2 = new GUIExternalCatalogV2();
			catalog_2.read_from_string (catalog_str);

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string #2 *****");
			System.out.println ();

			String catalog_str_2 = catalog_2.write_to_string();

			System.out.println (catalog_str_2);

			// Compare strings

			System.out.println ();
			System.out.println ("***** Comparing strings *****");
			System.out.println ();

			boolean comparison_result = catalog_str.equals (catalog_str_2);

			System.out.println ("String comparison result: " + comparison_result);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename  event_id  min_days  max_days  min_mag  radius_km  regional_radius_km
		// Fetch catalog data from Comcat and fill in a GUIExternalCatalogV2.
		// Write to file and then string and display it.
		// Then read the file into a second GUIExternalCatalogV2.
		// Write to a second string and display it.
		// Compare the two strings - they should be equal.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Constructing GUIExternalCatalogV2 and testing file I/O");
			String filename = testargs.get_string ("filename");
			String event_id = testargs.get_string ("event_id");
			double min_days = testargs.get_double ("min_days");
			double max_days = testargs.get_double ("max_days");
			double min_mag = testargs.get_double ("min_mag");
			double radius_km = testargs.get_double ("radius_km");
			double regional_radius_km = testargs.get_double ("regional_radius_km");
			testargs.end_test();

			// Fetch catalog from comcat

			System.out.println ();
			System.out.println ("***** Fetching data from Comcat *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog = test_read_from_comcat (event_id, min_days, max_days, min_mag, radius_km, regional_radius_km);

			// Write to file

			System.out.println ();
			System.out.println ("***** Writing to file *****");
			System.out.println ();

			catalog.write_to_file (filename);

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string *****");
			System.out.println ();

			String catalog_str = catalog.write_to_string();

			System.out.println (catalog_str);

			// Read from file

			System.out.println ();
			System.out.println ("***** Reading from file *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog_2 = new GUIExternalCatalogV2();
			catalog_2.read_from_file (filename);

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string #2 *****");
			System.out.println ();

			String catalog_str_2 = catalog_2.write_to_string();

			System.out.println (catalog_str_2);

			// Compare strings

			System.out.println ();
			System.out.println ("***** Comparing strings *****");
			System.out.println ();

			boolean comparison_result = catalog_str.equals (catalog_str_2);

			System.out.println ("String comparison result: " + comparison_result);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  filename  event_id  min_days  max_days  min_mag  radius_km  regional_radius_km
		// Fetch catalog data from Comcat and fill in a GUIExternalCatalogV2.
		// Write to file and then string and display it.
		// Then read the file into a second GUIExternalCatalogV2.
		// Write to a second string and display it.
		// Compare the two strings - they should be equal.
		// Same as test #3 except uses the consumer functions.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Constructing GUIExternalCatalogV2 and testing consumer/supplier I/O");
			String filename = testargs.get_string ("filename");
			String event_id = testargs.get_string ("event_id");
			double min_days = testargs.get_double ("min_days");
			double max_days = testargs.get_double ("max_days");
			double min_mag = testargs.get_double ("min_mag");
			double radius_km = testargs.get_double ("radius_km");
			double regional_radius_km = testargs.get_double ("regional_radius_km");
			testargs.end_test();

			// Fetch catalog from comcat

			System.out.println ();
			System.out.println ("***** Fetching data from Comcat *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog = test_read_from_comcat (event_id, min_days, max_days, min_mag, radius_km, regional_radius_km);

			// Write to file

			System.out.println ();
			System.out.println ("***** Writing to file consumer *****");
			System.out.println ();

			try (
				LineConsumerFile line_consumer = new LineConsumerFile (filename);
			) {
				catalog.write_to_consumer (line_consumer);
			}

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string *****");
			System.out.println ();

			String catalog_str = catalog.write_to_string();

			System.out.println (catalog_str);

			// Read from file

			System.out.println ();
			System.out.println ("***** Reading from file supplier *****");
			System.out.println ();

			GUIExternalCatalogV2 catalog_2 = new GUIExternalCatalogV2();

			try (
				LineSupplierFile line_supplier = new LineSupplierFile (filename);
			) {
				catalog_2.read_from_supplier (line_supplier);
			}

			// Write to string and display

			System.out.println ();
			System.out.println ("***** Writing to string #2 *****");
			System.out.println ();

			String catalog_str_2 = catalog_2.write_to_string();

			System.out.println (catalog_str_2);

			// Compare strings

			System.out.println ();
			System.out.println ("***** Comparing strings *****");
			System.out.println ();

			boolean comparison_result = catalog_str.equals (catalog_str_2);

			System.out.println ("String comparison result: " + comparison_result);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}

}
