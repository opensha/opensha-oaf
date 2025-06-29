package org.opensha.oaf.util.gui;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.TimeZone;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.Predicate;

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

import org.opensha.oaf.util.catalog.EventIDGenerator;

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
//   #  Date      Time       Lat         Lon       Depth    Mag        ID
// Then comes a series of earthquakes, one per line.  For example:
//   2019-07-06 03:19:53   35.7695   -117.59933     8.0     7.1    ci38457511
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


	// Standard symbol names.

	public static final String SSYM_TIME_RANGE = "time_range";
	public static final String SSYM_DEPTH_RANGE = "depth_range";
	public static final String SSYM_MIN_MAG = "min_mag";
	public static final String SSYM_AFTERSHOCK_REGION = "aftershock_region";
	public static final String SSYM_OUTER_REGION = "outer_region";
	public static final String SSYM_WRAP_LON = "wrap_lon";


	//----- Parsing options -----


	// Option to allow legacy mainshock. (Default true)

	private boolean f_allow_legacy_main;

	// Option to re-classify aftershocks, if the file contains one mainshock and no foreshocks. (Default false)

	private boolean f_reclassify_aftershocks;

	// Option to sort all lists of earthquakes in order of increasing time. (Default false)

	private boolean f_sort_all_lists;

	// Option to generate event IDs if they are not supplied in the file.  (Default true)

	private boolean f_generate_event_ids;


	//----- Internal variables used while parsing -----


	// The current category, or null if not specified, used while parsing (init to null).

	private String current_category;

	// The current line number, used while parsing (init to 0).

	private int current_line_number;

	// True if the previous line was the legacy mainshock introduction.

	private boolean f_seen_legacy_main_intro;

	// True if we got a mainshock from a legacy mainshock line.

	private boolean f_got_legacy_main;


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


	// Get the value of a symbol, as an int.
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


	// Get the value of a symbol, as an array of strings.
	// Throws exception if symbol is not defined or has invalid value.
	// If len >= 0, it is the expected array length.
	// Note: Strings are separated by spaces or tabs, and there is no parsing for quotes or special symbols.

	public final String[] get_symbol_as_string_array (String name, int len) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_string_array: Undefined symbol: " + name);
		}
		String[] w = value.trim().split ("[ \\t]+");
		if (len >= 0 && w.length != len) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_string_array: Invalid symbol: name = " + name + ", value = " + value);
		}
		return w;
	}


	// Get the value of a symbol, as an array of double.
	// Throws exception if symbol is not defined or has invalid value.
	// If len >= 0, it is the expected array length.
	// Note: Strings are separated by spaces or tabs, and there is no parsing for quotes or special symbols.

	public final double[] get_symbol_as_double_array (String name, int len) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_double_array: Undefined symbol: " + name);
		}
		String[] w = value.trim().split ("[ \\t]+");
		double[] result = new double[w.length];
		try {
			for (int i = 0; i < w.length; ++i) {
				result[i] = Double.parseDouble (w[i]);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_double_array: Invalid symbol: name = " + name + ", value = " + value);
		}
		return result;
	}


	// Get the value of a symbol, as an array of int.
	// Throws exception if symbol is not defined or has invalid value.
	// If len >= 0, it is the expected array length.
	// Note: Strings are separated by spaces or tabs, and there is no parsing for quotes or special symbols.

	public final int[] get_symbol_as_int_array (String name, int len) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_int_array: Undefined symbol: " + name);
		}
		String[] w = value.trim().split ("[ \\t]+");
		int[] result = new int[w.length];
		try {
			for (int i = 0; i < w.length; ++i) {
				result[i] = Integer.parseInt (w[i]);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_int_array: Invalid symbol: name = " + name + ", value = " + value);
		}
		return result;
	}


	// Get the value of a symbol, as a region.
	// Throws exception if symbol is not defined or has invalid value.

	public final SphRegion get_symbol_as_region (String name) {
		String value = symbols.get (name);
		if (value == null) {
			throw new NoSuchElementException ("GUIExternalCatalogV2.get_symbol_as_region: Undefined symbol: " + name);
		}
		SphRegion result;
		try {
			result = SphRegion.unmarshal_from_line_poly (value);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.get_symbol_as_region: Invalid symbol: name = " + name + ", value = " + value);
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


	// Get a list that contains all the ruptures in all the given categories that satisfy the filter.
	// If f_sort is true, the combined list is sorted in increasing order of time.

	public final ObsEqkRupList get_filtered_joined_rup_list (boolean f_sort, Predicate<ObsEqkRupture> filter, String... categories) {
		ObsEqkRupList rups = new ObsEqkRupList();
		for (String category : categories) {
			filter_earthquakes (filter, get_rup_list (category), rups);
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

	//private static final Pattern quake_line_pattern = Pattern.compile ("[ \\t]*(\\d\\d\\d\\d)-(\\d\\d?)-(\\d\\d?)[ \\t]+(\\d\\d?):(\\d\\d?):(\\d\\d?)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]*([a-zA-Z_0-9]*)[ \\t]*[\\n\\r]*");

	// This second form has better handling of the optional ID field, capture string is null if ID not present.

	//private static final Pattern quake_line_pattern = Pattern.compile ("[ \\t]*(\\d\\d\\d\\d)-(\\d\\d?)-(\\d\\d?)[ \\t]+(\\d\\d?):(\\d\\d?):(\\d\\d?)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)(?:[ \\t]+([a-zA-Z_0-9]+))?[ \\t]*[\\n\\r]*");

	// This third form is permissive on how the date and time are formatted:
	// - Month, day, hour, minute, and second can be one or two digits (year must be 4 digits).
	// - Seconds can optionally have a decimal part (which is discarded).
	// - Dashes and colons can be surrounded by spaces/tabs, or replaced by spaces/tabs.
	// - Date and time can be optionally separated by "T" (case-insensitive), optionally surrounded by spaces/tabs.
	// - Time can optionally be followed by "Z" or "UTC" (case-insensitive), optionally preceded by spaces/tabs.
	// As a consequence, date/time can be specified in ISO-8601 format (example: 2011-12-03T10:15:30Z).
	// We also change the ordering of fields (put mag after lat/lon/depth) to match the original GUI format (requires no change to regex).

	//private static final Pattern quake_line_pattern = Pattern.compile ("[ \\t]*(\\d\\d\\d\\d)(?:[ \\t]+|[ \\t]*-[ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*-[ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[tT][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*:[ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*:[ \\t]*)(\\d\\d?)(?:\\.\\d*)?(?:[ \\t]*(?:[zZ]|[uU][tT][cC]))?[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)(?:[ \\t]+([a-zA-Z_0-9]+))?[ \\t]*[\\n\\r]*");

	// (?:[ \\t]+|[ \\t]*-[ \\t]*)				= spaces/tabs, or dash optionally surrounded by spaces/tabs
	// (?:[ \\t]+|[ \\t]*:[ \\t]*)				= spaces/tabs, or colon optionally surrounded by spaces/tabs
	// (?:[ \\t]+|[ \\t]*[tT][ \\t]*)			= spaces/tabs, or "T" (case insensitive) optionally surrounded by spaces/tabs
	// (?:\\.\\d*)?								= optional decimal part, dot optionally followed by digits
	// (?:[ \\t]*(?:[zZ]|[uU][tT][cC]))?		= optional "Z" or "UTC" (case insensitive), optionally preceded by spaces/tabs

	// This fourth form is even more permissive on how the date and time are formatted:
	// - Month, day, hour, minute, and second can be one or two digits (year must be 4 digits).
	// - Seconds can optionally have a decimal part, consisting of period/comma followed by any number of digits (which is discarded).
	// - Parts of date can be separated by spaces/tabs, or by dash/slash/period/comma optionally surrounded by spaces/tabs.
	// - Parts of time can be separated by spaces/tabs, or by colon/period/comma optionally surrounded by spaces/tabs.
	// - Date and time can be optionally separated by "T" (case-insensitive), optionally surrounded by spaces/tabs.
	// - Time can optionally be followed by "Z" or "UTC" (case-insensitive), optionally preceded by spaces/tabs.
	// As a consequence, date/time can be specified in ISO-8601 format (example: 2011-12-03T10:15:30Z).
	// We also change the ordering of fields (put mag after lat/lon/depth) to match the original GUI format (requires no change to regex).
	// Additionally, we allow lat/lon/depth/mag to use period or comma as the decimal point.

	private static final Pattern quake_line_pattern = Pattern.compile ("[ \\t]*(\\d\\d\\d\\d)(?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[tT][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[:.,][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[:.,][ \\t]*)(\\d\\d?)(?:[.,]\\d*)?(?:[ \\t]*(?:[zZ]|[uU][tT][cC]))?[ \\t]+([0-9.,eE+-]+)[ \\t]+([0-9.,eE+-]+)[ \\t]+([0-9.,eE+-]+)[ \\t]+([0-9.,eE+-]+)(?:[ \\t]+([a-zA-Z_0-9]+))?[ \\t]*[\\n\\r]*");

	// (?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)			= spaces/tabs, or dash/slash/period/comma optionally surrounded by spaces/tabs
	// (?:[ \\t]+|[ \\t]*[:.,][ \\t]*)			= spaces/tabs, or colon/period/comma optionally surrounded by spaces/tabs
	// (?:[.,]\\d*)?							= optional decimal part, period/comma optionally followed by digits


	// Capture groups for the earthquake description.

	private static final int quake_year_capture_group = 1;
	private static final int quake_month_capture_group = 2;
	private static final int quake_day_capture_group = 3;

	private static final int quake_hour_capture_group = 4;
	private static final int quake_minute_capture_group = 5;
	private static final int quake_second_capture_group = 6;

	private static final int quake_lat_capture_group = 7;
	private static final int quake_lon_capture_group = 8;
	private static final int quake_depth_capture_group = 9;

	private static final int quake_mag_capture_group = 10;

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
	// If f_generate_id is true, generate an event ID if none is supplied in the line.
	// Throws exception if error.

	public static ObsEqkRupture quake_parse_line (String line, boolean f_generate_id) {
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

		String lat = matcher.group (quake_lat_capture_group);
		String lon = matcher.group (quake_lon_capture_group);
		String depth = matcher.group (quake_depth_capture_group);

		String mag = matcher.group (quake_mag_capture_group);

		String id = matcher.group (quake_id_capture_group);	// might be null
		if (id == null) {
			if (f_generate_id) {
				id = EventIDGenerator.generate_id();
			} else {
				id = "";
			}
		}

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

		// Convert location

		Location hypo;
		try {
			hypo = new Location(Double.parseDouble (lat.replace(',', '.')), Double.parseDouble (lon.replace(',', '.')), Double.parseDouble (depth.replace(',', '.')));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Numeric conversion error parsing location for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Error parsing location for line: " + line, e);
		}

		// Convert magnitude

		double r_mag;
		try {
			r_mag = Double.parseDouble (mag.replace(',', '.'));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Numeric conversion error parsing magnitude for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.quake_parse_line: Error parsing magnitude for line: " + line, e);
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
	//   date   time   lat   lon   depth   mag   id
	//    10  1  8   2  9  2  10 2   8   2  6  2 1+

	public static String quake_format_line (ObsEqkRupture rup) {
		StringBuilder result = new StringBuilder();
		Location hypo = rup.getHypocenterLocation();
		result.append (SimpleUtils.time_to_string_no_z (rup.getOriginTime()));
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
		result.append ("  ");
		result.append (SimpleUtils.double_to_string_trailz ("%6.3f", SimpleUtils.TRAILZ_PAD_RIGHT, rup.getMag()));
		String id = rup.getEventId();
		if (id != null) {
			id = id.trim();
			if (id.length() > 0 && is_quake_id_valid (id) && (!( EventIDGenerator.is_generated_id(id) ))) {
				result.append ("  ");
				result.append (id);
			}
		}
		return result.toString();
	}


	// Format a comment line containing column headers for earthquake description.
	// Line format, and number of columns:
	//   date   time   lat   lon   depth   mag   id
	//    10  1  8   2  9  2  10 2   8   2  6  2 1+

	public static String quake_format_header () {
		StringBuilder result = new StringBuilder();
		result.append ("#  Date   ");
		result.append (" ");
		result.append ("  Time  ");
		result.append ("  ");
		result.append ("   Lat   ");
		result.append ("  ");
		result.append ("    Lon   ");
		result.append ("  ");
		result.append ("  Depth ");
		result.append ("  ");
		result.append (" Mag  ");
		result.append ("  ");
		result.append ("    ID");
		return result.toString();
	}




	//--- Legacy Mainshock




	// Pattern to recognize the comment that introduces a legacy mainshock.
	// The comment should be "# Main Shock:";
	// We recognize any comment that begins with "Main" (case-insensitive).

	private static final Pattern legacy_main_intro_pattern = Pattern.compile ("[ \\t]*#[ \\t]*[Mm][Aa][Ii][Nn].*");

	// Pattern to recognize a line containing a legacy mainshock.
	// The legacy mainshock is given as a comment, on the next line after the introductory comment.
	// Note: In principle, we only need to recognize the legacy format (only spaces/tabs allowed for date/time separators, no time zone, no event ID).
	// For convenience, we use quake_line_pattern with a prefixed #, which is more permissive.

	//private static final Pattern legacy_main_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*(\\d\\d\\d\\d)(?:[ \\t]+|[ \\t]*-[ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*-[ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[tT][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*:[ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*:[ \\t]*)(\\d\\d?)(?:\\.\\d*)?(?:[ \\t]*(?:[zZ]|[uU][tT][cC]))?[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)[ \\t]+([0-9.eE+-]+)(?:[ \\t]+([a-zA-Z_0-9]+))?[ \\t]*[\\n\\r]*");
	
	private static final Pattern legacy_main_line_pattern = Pattern.compile ("[ \\t]*#[ \\t]*(\\d\\d\\d\\d)(?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[/.,-][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[tT][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[:.,][ \\t]*)(\\d\\d?)(?:[ \\t]+|[ \\t]*[:.,][ \\t]*)(\\d\\d?)(?:[.,]\\d*)?(?:[ \\t]*(?:[zZ]|[uU][tT][cC]))?[ \\t]+([0-9.,eE+-]+)[ \\t]+([0-9.,eE+-]+)[ \\t]+([0-9.,eE+-]+)[ \\t]+([0-9.,eE+-]+)(?:[ \\t]+([a-zA-Z_0-9]+))?[ \\t]*[\\n\\r]*");


	// Parse a line which may contain a legacy mainshock.
	// Returns the earthquake if parse successful.
	// Returns null if the line does not have the form of an earthquake.
	// If f_generate_id is true, generate an event ID if none is supplied in the line.
	// Throws exception if error.

	public static ObsEqkRupture legacy_main_parse_line (String line, boolean f_generate_id) {
		Matcher matcher = legacy_main_line_pattern.matcher (line);
		if (!( matcher.matches() )) {
			//throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Invalid earthquake data format for line: " + line);
			return null;
		}

		// Get the parts of the line

		String year = matcher.group (quake_year_capture_group);
		String month = matcher.group (quake_month_capture_group);
		String day = matcher.group (quake_day_capture_group);

		String hour = matcher.group (quake_hour_capture_group);
		String minute = matcher.group (quake_minute_capture_group);
		String second = matcher.group (quake_second_capture_group);

		String lat = matcher.group (quake_lat_capture_group);
		String lon = matcher.group (quake_lon_capture_group);
		String depth = matcher.group (quake_depth_capture_group);

		String mag = matcher.group (quake_mag_capture_group);

		String id = matcher.group (quake_id_capture_group);	// might be null
		if (id == null) {
			if (f_generate_id) {
				id = EventIDGenerator.generate_id();
			} else {
				id = "";
			}
		}

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
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Date/time parse error parsing time for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Error parsing time for line: " + line, e);
		}

		// Convert location

		Location hypo;
		try {
			hypo = new Location(Double.parseDouble (lat.replace(',', '.')), Double.parseDouble (lon.replace(',', '.')), Double.parseDouble (depth.replace(',', '.')));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Numeric conversion error parsing location for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Error parsing location for line: " + line, e);
		}

		// Convert magnitude

		double r_mag;
		try {
			r_mag = Double.parseDouble (mag.replace(',', '.'));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Numeric conversion error parsing magnitude for line: " + line, e);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Error parsing magnitude for line: " + line, e);
		}

		// Make the result

		ObsEqkRupture result;
		try {
			result = new ObsEqkRupture (id, time, hypo, r_mag);
		} catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalogV2.legacy_main_parse_line: Error forming earthquake rupture object for line: " + line, e);
		}

		return result;
	}


	// Format a comment line containing a legacy mainshock description.
	// Line format, and number of columns:
	//   date   time   lat   lon   depth   mag   id
	//    10  1  8   2  9  2  10 2   8   2  6  2 1+
	// Note: In principle, all separators should be a single tab, and the event ID should not be included.

	public static String legacy_main_format_line (ObsEqkRupture rup) {
		StringBuilder result = new StringBuilder();
		Location hypo = rup.getHypocenterLocation();
		result.append ("# ");
		result.append (SimpleUtils.time_to_string_no_z (rup.getOriginTime()));
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
		result.append ("  ");
		result.append (SimpleUtils.double_to_string_trailz ("%6.3f", SimpleUtils.TRAILZ_PAD_RIGHT, rup.getMag()));
		String id = rup.getEventId();
		if (id != null) {
			id = id.trim();
			if (id.length() > 0 && is_quake_id_valid (id) && (!( EventIDGenerator.is_generated_id(id) ))) {
				result.append ("  ");
				result.append (id);
			}
		}
		return result.toString();
	}


	// Return true if the given line is a comment line containing the legacy mainshock introduction.

	public static boolean is_legacy_main_intro_line (String line) {
		return legacy_main_intro_pattern.matcher(line).matches();
	}


	// Format a comment line containing the legacy mainshock introduction.

	public static String format_legacy_main_intro_line () {
		StringBuilder result = new StringBuilder();
		result.append ("# Main Shock:");
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

		f_allow_legacy_main = true;
		f_reclassify_aftershocks = false;
		f_sort_all_lists = false;
		f_generate_event_ids = true;

		current_category = null;
		current_line_number = 0;
		f_seen_legacy_main_intro = false;
		f_got_legacy_main = false;
		classification_time = Long.MIN_VALUE;
		classification_region = null;

		return;
	}




	// Constructor sets up an empty file.

	public GUIExternalCatalogV2 () {
		clear();
	}




	// Set the option to allow legacy mainshock.
	// Return this object.

	public GUIExternalCatalogV2 set_allow_legacy_main (boolean the_f_allow_legacy_main) {
		this.f_allow_legacy_main = the_f_allow_legacy_main;
		return this;
	}




	// Set the option to re-classify aftershocks.
	// Return this object.

	public GUIExternalCatalogV2 set_reclassify_aftershocks (boolean the_f_reclassify_aftershocks) {
		this.f_reclassify_aftershocks = the_f_reclassify_aftershocks;
		return this;
	}




	// Set the option to allow legacy mainshock.
	// Return this object.

	public GUIExternalCatalogV2 set_sort_all_lists (boolean the_f_sort_all_lists) {
		this.f_sort_all_lists = the_f_sort_all_lists;
		return this;
	}




	// Set the option to generate event IDs if they are not supplied in the file.
	// Return this object.

	public GUIExternalCatalogV2 set_generate_event_ids (boolean the_f_generate_event_ids) {
		this.f_generate_event_ids = the_f_generate_event_ids;
		return this;
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


	// Remove a symbol definition.
	// Returns the current value of the symbol, or null if the symbol does not currently exist.
	// Note: It is not an error to remove a symbol which does not currently exist.
	// Note: To change the value of a symbol which currently exists, you must first
	// remove the existing definition and then add the new definition.

	public final String symdef_remove (String name) {
		return symbols.remove (name);
	}


	// Add a symbol definition, with the value given as a double.
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


	// Add a symbol definition, with the value given as an int.
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


	// Add a symbol definition, with the value given as a long.
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


	// Add a symbol definition, with the value given as a boolean.
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


	// Add a symbol definition, with the value given as an array of String.
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.
	// Note: Leading and trailing spaces are stripped from each string (by trim()).
	// Note: Correct parsing requires each string to not contain embedded white space; this is not checked.

	public final void symdef_add_string_array (String name, String... value) {
		check_name (name);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length; ++i) {
			if (i > 0) {
				sb.append (" ");
			}
			sb.append (value[i].trim());
		}
		String my_value = symdef_check_value (sb.toString());
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add_string_array: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition, with the value given as an array of double.
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_double_array (String name, double... value) {
		check_name (name);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length; ++i) {
			if (i > 0) {
				sb.append (" ");
			}
			sb.append (Double.toString (value[i]));
		}
		String my_value = symdef_check_value (sb.toString());
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add_double_array: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition, with the value given as an array of int.
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_int_array (String name, int... value) {
		check_name (name);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length; ++i) {
			if (i > 0) {
				sb.append (" ");
			}
			sb.append (Integer.toString (value[i]));
		}
		String my_value = symdef_check_value (sb.toString());
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add_int_array: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add a symbol definition, with the value given as a region.
	// Throws exception if name or value is invalid, or if there is already a symbol with the name.

	public final void symdef_add_region (String name, SphRegion value) {
		check_name (name);
		String my_value = symdef_check_value (SphRegion.marshal_to_line_poly(value).trim());
		if (symbols.containsKey (name)) {
			throw new InvariantViolationException ("GUIExternalCatalogV2.symdef_add_region: Duplicate symbol name: " + name);
		}
		symbols.put (name, my_value);
		return;
	}


	// Add multiple symbols for a call to Comcat.
	// Parameters are the same as for a call to the Comcat accessor, except includes
	// both aftershock and outer regions, either or both of which can be null.
	// Note: As a special case, if maxDays == minDays, then the end time is the current time.

	public final void symdef_add_comcat (
		ObsEqkRupture mainshock,
		double minDays,
		double maxDays,
		double minDepth,
		double maxDepth,
		SphRegion aftershock_region,
		SphRegion outer_region,
		boolean wrapLon,
		double minMag
	) {
		symdef_add_comcat (
			mainshock.getOriginTime(),
			minDays,
			maxDays,
			minDepth,
			maxDepth,
			aftershock_region,
			outer_region,
			wrapLon,
			minMag
		);

		return;
	}


	public final void symdef_add_comcat (
		long mainshock_time,
		double minDays,
		double maxDays,
		double minDepth,
		double maxDepth,
		SphRegion aftershock_region,
		SphRegion outer_region,
		boolean wrapLon,
		double minMag
	) {

		// Time range
		// Note: ComcatAccessor uses cast to long, which truncates toward zero

		long startTime = mainshock_time + (long)(minDays * SimpleUtils.DAY_MILLIS_D);
		long endTime = mainshock_time + (long)(maxDays * SimpleUtils.DAY_MILLIS_D);

		double my_maxDays = maxDays;
		if (startTime == endTime) {
			endTime = System.currentTimeMillis();
			my_maxDays = SimpleUtils.millis_to_days (endTime - mainshock_time);
		}

		symdef_add_double_array (SSYM_TIME_RANGE, minDays, my_maxDays);

		// Minimum magnitude
		// Note: ComcatAccessor uses -10.0 for no minimum, and tests against -9.0

		if (minMag >= -9.0) {
			symdef_add_double (SSYM_MIN_MAG, minMag);
		}

		// Depth range

		symdef_add_double_array (SSYM_DEPTH_RANGE, minDepth, maxDepth);

		// Aftershock region

		if (aftershock_region != null) {
			symdef_add_region (SSYM_AFTERSHOCK_REGION, aftershock_region);
		}

		// Outer region

		if (outer_region != null) {
			symdef_add_region (SSYM_OUTER_REGION, outer_region);
		}

		// Wrap longitude (only for non-default value)

		if (wrapLon) {
			symdef_add_boolean (SSYM_WRAP_LON, wrapLon);
		}

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

		// Handle legacy mainshock, if it is allowed and no category has been Set

		if (f_allow_legacy_main && current_category == null) {

			try {

				// Check for legacy mainshock introduction

				boolean is_legacy_main_intro = is_legacy_main_intro_line (line);
				if (is_legacy_main_intro) {
					f_seen_legacy_main_intro = true;
					return;
				}

				// Check for legacy mainshock, if so then add to mainshock category and set aftershock as the current category

				if (f_seen_legacy_main_intro) {
					f_seen_legacy_main_intro = false;

					ObsEqkRupture rup = legacy_main_parse_line (line, f_generate_event_ids);
					if (rup != null) {
						add_quake (EQCAT_MAINSHOCK, rup);
						current_category = EQCAT_AFTERSHOCK;
						f_got_legacy_main = true;
						return;
					}
				}

			} catch (Exception e) {
				throw new IllegalArgumentException ("GUIExternalCatalogV2.parse_line: Error on line " + current_line_number + ": failed to parse comment line", e);
			}
		}

		// Handle earthquake

		try {
			ObsEqkRupture rup = quake_parse_line (line, f_generate_event_ids);
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




	// Finish parsing after all lines have been parsed.

	public final void finish_parse () {

		// If we want to reclassify aftershocks ...

		if (f_reclassify_aftershocks) {

			// Get the lists

			ObsEqkRupList mainshock_list = earthquakes.get (EQCAT_MAINSHOCK);
			ObsEqkRupList foreshock_list = earthquakes.get (EQCAT_FORESHOCK);
			ObsEqkRupList aftershock_list = earthquakes.get (EQCAT_AFTERSHOCK);

			// Reclassify if there is one mainshock, no foreshocks, and at least one aftershock

			if (mainshock_list.size() == 1 && foreshock_list.size() == 0 && aftershock_list.size() > 0) {

				// Get the mainshock time

				long mainshock_time = mainshock_list.get(0).getOriginTime();

				// Copy the aftershock list then clear it

				ArrayList<ObsEqkRupture> rup_list = new ArrayList<ObsEqkRupture> (aftershock_list);
				aftershock_list.clear();

				// Scan the rupture list and classify (by time only)

				for (ObsEqkRupture rup : rup_list) {
					if (rup.getOriginTime() < mainshock_time) {
						foreshock_list.add (rup);
					} else {
						aftershock_list.add (rup);
					}
				}
			}
		}

		// If we want to sort all lists ...

		if (f_sort_all_lists) {

			// Sort the lists

			sort_all_earthquakes();
		}

		return;
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




	// Filter a list of earthquakes.
	// Each earthquake in the source that passes the filter is added to the destination.

	public static void filter_earthquakes (Predicate<ObsEqkRupture> filter, Collection<ObsEqkRupture> src, Collection<ObsEqkRupture> dest) {
		for (ObsEqkRupture rup : src) {
			if (filter.test (rup)) {
				dest.add (rup);
			}
		}
		return;
	}




	// Filter a list of earthquakes.
	// Clear the source and then re-add all earthquakes that pass the filter.

	public static void filter_earthquakes (Predicate<ObsEqkRupture> filter, Collection<ObsEqkRupture> src) {
		ArrayList<ObsEqkRupture> x = new ArrayList<ObsEqkRupture>(src);
		src.clear();
		filter_earthquakes (filter, x, src);
		return;
	}




	// Filter each category of earthquakes, except the mainshock.

	public final void filter_all_earthquakes_except_main (final Predicate<ObsEqkRupture> filter) {
		earthquakes.forEach ((String category, ObsEqkRupList rups) -> {
			if (!( category.equals (EQCAT_MAINSHOCK) )) {
				filter_earthquakes (filter, rups);
			}
		});
		return;
	}




	// Reclassify earthquakes in the EQCAT_FORESHOCK, EQCAT_AFTERSHOCK, and EQCAT_REGIONAL categories.
	// There must be exactly one mainshock in the file.
	// If region is null, then there are no regional earthquakes.
	// If filter is provided and non-null, then the foreshocks, aftershocks, and regional earthquakes are filtered.
	// The resulting lists of foreshocks, aftershocks, and regional earthquakes are sorted.

	public final void reclassify_earthquakes (SphRegion region) {
		reclassify_earthquakes (region, null);
		return;
	}

	public final void reclassify_earthquakes (SphRegion region, Predicate<ObsEqkRupture> filter) {

		// Get the mainshock, exception if we don't have it

		ObsEqkRupture mainshock = get_mainshock();
		
		// Get all earthquakes to reclassify, and clear the existing lists

		ObsEqkRupList foreshock_list = earthquakes.get (EQCAT_FORESHOCK);
		ObsEqkRupList aftershock_list = earthquakes.get (EQCAT_AFTERSHOCK);
		ObsEqkRupList regional_list = earthquakes.get (EQCAT_REGIONAL);

		ArrayList<ObsEqkRupture> rups = new ArrayList<ObsEqkRupture>();

		if (filter == null) {
			rups.addAll (foreshock_list);
			rups.addAll (aftershock_list);
			rups.addAll (regional_list);
		} else {
			filter_earthquakes (filter, foreshock_list, rups);
			filter_earthquakes (filter, aftershock_list, rups);
			filter_earthquakes (filter, regional_list, rups);
		}

		foreshock_list.clear();
		aftershock_list.clear();
		regional_list.clear();

		// Sort the combined list, since the original ordering in the file has been lost

		sort_aftershocks (rups);

		// Set up classification

		classification_time = mainshock.getOriginTime();
		classification_region = region;

		// Classify and add all the earthquakes in the combined list

		for (ObsEqkRupture rup : rups) {
			classify_and_add_quake (rup);
		}

		return;
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
	
		try {
			finish_parse();
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_file: Parsing error reading catalog file: " + the_file.getPath(), e);
		}

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

		try {
			finish_parse();
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_string: Parsing error reading catalog file from string.", e);
		}

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
	
		try {
			finish_parse();
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalogV2.read_from_supplier: Error reading catalog file", e);
		}

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
