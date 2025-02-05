package org.opensha.oaf.comcat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.opensha.oaf.util.JSONOrderedObject;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;


/**
 * Utilities for parsing GeoJSON objects as received from Comcat.
 * Author: Michael Barall 12/08/2018.
 */
public class GeoJsonUtils {




	//----- Display functions -----




	// [DEPRECATED]
	// Print a JSONObject.
	// This is a debugging function, not used in normal operation.

	public static void printJSON(JSONObject json) {
		printJSON(json, "");
	}
	public static void printJSON(JSONObject json, String prefix) {
		for (Object key : json.keySet()) {
			Object val = json.get(key);
			if (val != null && val.toString().startsWith("[{")) {
				String str = val.toString();
				try {
					val = new JSONParser().parse(str.substring(1, str.length()-1));
				} catch (ParseException e) {
//					e.printStackTrace();
				}
			}
			if (val != null && val instanceof JSONObject) {
				System.out.println(prefix+key+":");
				String prefix2 = prefix;
				if (prefix2 == null)
					prefix2 = "";
				prefix2 += "\t";
				printJSON((JSONObject)val, prefix2);
			} else {
				System.out.println(prefix+key+": "+val);
			}
		}
	}




	// Convert a JSON object to a string.
	// The result is a nicely-formatted JSON file.
	// If f_friendly is false, the result is valid JSON.
	// If f_friendly is true, the result is made easier to read by not quoting names,
	// numbering array elements, and omitting commas.
	// Note that f_friendly defaults to true.
	// If f_compact is ture, the result is made compact by allowing some array elements
	// and map items to appear on the same line.
	// Note that f_compact defaults to false.
	// This is primarily a debugging function.

	public static String jsonObjectToString (Object o) {
		StringBuilder sb = new StringBuilder();
		jsonSubObjectToString (sb, o, null, "", true, false, false, false);
		return sb.toString();
	}

	public static String jsonObjectToString (Object o, boolean f_friendly, boolean f_compact) {
		StringBuilder sb = new StringBuilder();
		jsonSubObjectToString (sb, o, null, "", f_friendly, f_compact, false, false);
		return sb.toString();
	}




	// Convert a JSON subobject to a string.
	// Parameters:
	//  sb = String destination.
	//  o = JSON object to convert.
	//  name = Name for a map entry, array index (as a string) for array element, or null if top-level.
	//  prefix = Prefix to use on each line; used for indenting nested objects.
	//  f_friendly = If true, use a friendly format which is easier to read but not valid JSON.
	//  f_compact = If true, use a compact format which allows multiple map entries or array elements per line.
	//  f_array = If true, this object is an array element.
	//  f_comma = If true, there ia another element after this one in the map or array containing this object.
	// This is a recursive function, that calls itself for nested subobjects.

	private static void jsonSubObjectToString (StringBuilder sb, Object o, String name, String prefix, boolean f_friendly, boolean f_compact, boolean f_array, boolean f_comma) {

		// Write prefix and name

		sb.append (prefix);
		if (name != null) {
			if (f_array) {
				if (f_friendly) {
					sb.append (name);
					sb.append (": ");
				}
			} else {
				jsonNameToString (sb, name, f_friendly);
				sb.append (": ");
			}
		}

		// Handle null

		if (o == null) {
			sb.append ("null");
			sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
		}

		// Handle string

		else if (o instanceof String) {
			String s = (String) o;
			sb.append ("\"");
			escapeString (sb, s);
			sb.append ("\"");
			sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
		}

		// Handle object (Map)

		else if (o instanceof JSONObject) {
			JSONObject m = (JSONObject) o;
			if (!( f_compact && compactMapToString (sb, m, prefix, f_friendly, f_comma) )) {
				sb.append ("{");
				sb.append ("\n");
				String new_prefix = prefix + "  ";
				int n = m.size();
				int k = 0;
				for (Object key : m.keySet()) {
					Object val = m.get (key);
					jsonSubObjectToString (sb, val, key.toString(), new_prefix, f_friendly, f_compact, false, k + 1 < n);
					++k;
				}
				sb.append (prefix);
				sb.append ("}");
				sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
			}
		}

		// Handle ordered object (Map)

		else if (o instanceof JSONOrderedObject) {
			JSONOrderedObject m = (JSONOrderedObject) o;
			if (!( f_compact && compactMapToString (sb, m, prefix, f_friendly, f_comma) )) {
				sb.append ("{");
				sb.append ("\n");
				String new_prefix = prefix + "  ";
				int n = m.size();
				int k = 0;
				for (Object key : m.keySet()) {
					Object val = m.get (key);
					jsonSubObjectToString (sb, val, key.toString(), new_prefix, f_friendly, f_compact, false, k + 1 < n);
					++k;
				}
				sb.append (prefix);
				sb.append ("}");
				sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
			}
		}

		// Handle array (List)

		else if (o instanceof JSONArray) {
			JSONArray a = (JSONArray) o;
			if (!( f_compact && compactArrayToString (sb, a, prefix, f_friendly, f_comma) )) {
				sb.append ("[");
				sb.append ("\n");
				String new_prefix = prefix + "  ";
				int n = a.size();
				for (int k = 0; k < n; ++k) {
					Object val = a.get (k);
					jsonSubObjectToString (sb, val, Integer.toString (k), new_prefix, f_friendly, f_compact, true, k + 1 < n);
				}
				sb.append (prefix);
				sb.append ("]");
				sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
			}
		}

		// Anything else

		else {
			sb.append (o.toString());
			sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
		}

		return;
	}




	// Convert a compact item to a string.

	private static void compactItemToString (StringBuilder sb, Object o, boolean f_friendly, boolean f_comma) {

		// Handle null

		if (o == null) {
			sb.append ("null");
		}

		// Handle string

		else if (o instanceof String) {
			String s = (String) o;
			sb.append ("\"");
			escapeString (sb, s);
			sb.append ("\"");
		}

		// Anything else

		else {
			sb.append (o.toString());
		}

		// Append comma if requested

		if (f_comma && !f_friendly) {
			sb.append (",");
		}

		return;
	}




	// Convert a compact array to a string.
	// Returns true if processed, false if not.

	private static boolean compactArrayToString (StringBuilder sb, JSONArray a, String prefix, boolean f_friendly, boolean f_comma) {

		// Test if this is eligible to be a compact array

		for (Object o : a) {
			if (o != null) {
				if ((o instanceof JSONObject) || (o instanceof JSONOrderedObject) || (o instanceof JSONArray)) {
					return false;
				}
				if (o.toString().length() > 28) {
					return false;
				}
			}
		}

		// Small array, on a single line

		int n = a.size();
		if (n <= 10) {
			sb.append ("[");
			for (int k = 0; k < n; ++k) {
				Object val = a.get (k);
				if (k > 0) {
					sb.append (" ");
				}
				compactItemToString (sb, val, f_friendly, k + 1 < n);
			}
			sb.append ("]");
			sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
		}

		// Large array, 10 items per line

		else {
			sb.append ("[");
			String new_prefix = prefix + "  ";
			for (int k = 0; k < n; ++k) {
				Object val = a.get (k);
				if (k % 10 == 0) {
					sb.append ("\n");
					sb.append (new_prefix);
					if (f_friendly) {
						sb.append (Integer.toString (k));
						sb.append (": ");
					}
				} else {
					sb.append (" ");
				}
				compactItemToString (sb, val, f_friendly, k + 1 < n);
			}
			sb.append ("\n");
			sb.append (prefix);
			sb.append ("]");
			sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");
		}

		return true;
	}




	// Convert a compact map to a string.
	// Returns true if processed, false if not.

	private static boolean compactMapToString (StringBuilder sb, JSONObject m, String prefix, boolean f_friendly, boolean f_comma) {

		// Test if this is eligible to be a compact map

		int n = m.size();
		if (n > 4) {
			return false;
		}

		for (Object o : m.values()) {
			if (o != null) {
				if ((o instanceof JSONObject) || (o instanceof JSONOrderedObject) || (o instanceof JSONArray)) {
					return false;
				}
				if (o.toString().length() > 28) {
					return false;
				}
			}
		}

		for (Object o : m.keySet()) {
			if (o == null || o.toString().length() > 28) {
				return false;
			}
		}

		// Small map, on a single line

		sb.append ("{");
		int k = 0;
		for (Object key : m.keySet()) {
			Object val = m.get (key);
			if (k > 0) {
				sb.append (" ");
			}
			jsonNameToString (sb, key.toString(), f_friendly);
			sb.append (": ");
			compactItemToString (sb, val, f_friendly, k + 1 < n);
			++k;
		}
		sb.append ("}");
		sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");

		return true;
	}




	// Convert a compact map to a string.
	// Returns true if processed, false if not.

	private static boolean compactMapToString (StringBuilder sb, JSONOrderedObject m, String prefix, boolean f_friendly, boolean f_comma) {

		// Test if this is eligible to be a compact map

		int n = m.size();
		if (n > 4) {
			return false;
		}

		for (Object o : m.values()) {
			if (o != null) {
				if ((o instanceof JSONObject) || (o instanceof JSONOrderedObject) || (o instanceof JSONArray)) {
					return false;
				}
				if (o.toString().length() > 28) {
					return false;
				}
			}
		}

		for (Object o : m.keySet()) {
			if (o == null || o.toString().length() > 28) {
				return false;
			}
		}

		// Small map, on a single line

		sb.append ("{");
		int k = 0;
		for (Object key : m.keySet()) {
			Object val = m.get (key);
			if (k > 0) {
				sb.append (" ");
			}
			jsonNameToString (sb, key.toString(), f_friendly);
			sb.append (": ");
			compactItemToString (sb, val, f_friendly, k + 1 < n);
			++k;
		}
		sb.append ("}");
		sb.append ((f_comma && !f_friendly) ? ",\n" : "\n");

		return true;
	}




	// Write a JSON name.
	// Friendly format writes the name with no quotes or escapes, if possible.

	//private static final Pattern friendly_name_pattern = Pattern.compile ("[a-zA-Z0-9_.~$@/\\\\+-]+");
	private static final Pattern friendly_name_pattern = Pattern.compile ("[.~/\\\\+-]*[a-zA-Z0-9_$@][a-zA-Z0-9_$@.~/\\\\+-]*");

	private static void jsonNameToString (StringBuilder sb, String name, boolean f_friendly) {
		if (f_friendly && friendly_name_pattern.matcher(name).matches()) {
			sb.append (name);
		} else {
			sb.append ("\"");
			escapeString (sb, name);
			sb.append ("\"");
		}
		return;
	}




	// Convert control characters to escape sequences, for the given string.

	public static String escapeString (String s) {
		StringBuilder sb = new StringBuilder();
		escapeString (sb, s);
		return sb.toString();
	}


	public static void escapeString (StringBuilder sb, String s) {

		// Loop over characters in string

		int len = s.length();
		for (int i = 0; i < len; ++i) {

			// Switch on character

			char ch=s.charAt(i);
			switch(ch) {

			case '"':
				sb.append ("\\\"");
				break;

			case '\\':
				sb.append ("\\\\");
				break;

			case '\b':
				sb.append ("\\b");
				break;

			case '\f':
				sb.append ("\\f");
				break;

			case '\n':
				sb.append ("\\n");
				break;

			case '\r':
				sb.append ("\\r");
				break;

			case '\t':
				sb.append ("\\t");
				break;

			default:

				// Handle Unicode control characters

				if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
					int k = ch;
					sb.append (String.format ("\\u%04X", k));
				}

				// All other characters are just appended

				else {
					sb.append (ch);
				}
				break;
			}
		}

		return;
	}




	// Given a string containing JSON, convert it to nicely-formatted JSON.
	// Parameters:
	//  s = String containing JSON.
	//  f_friendly = True to produce a friendly display format, false to produce nicely-formatted valid JSON.
	//  f_compact = True to produce a compact format with some map items or array elements on the same line.
	// Return a string containing nicely-formatted JSON, which is valid JSON if f_friendly is false.
	// Throws an exception if error parsing s.

	public static String jsonStringToString (String s, boolean f_friendly, boolean f_compact) {
		String result = null;

		try {

			// Parse the JSON

			Object o = JSONOrderedObject.parseWithException (s);

			// Convert to nice format

			result = jsonObjectToString (o, f_friendly, f_compact);
		}
		catch (ParseException e) {
			throw new RuntimeException ("GeoJsonUtils.jsonStringToString: JSON parse error", e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GeoJsonUtils.jsonStringToString: Unexpected error", e);
		}

		return result;
	}




	// Given a reader containing JSON, convert it to nicely-formatted JSON.
	// Parameters:
	//  in = Data source containing JSON.
	//  f_friendly = True to produce a friendly display format, false to produce nicely-formatted valid JSON.
	//  f_compact = True to produce a compact format with some map items or array elements on the same line.
	// Return a string containing nicely-formatted JSON, which is valid JSON if f_friendly is false.
	// Throws an exception if error parsing input.

	public static String jsonReaderToString (Reader in, boolean f_friendly, boolean f_compact) {
		String result = null;

		try {

			// Parse the JSON

			Object o = JSONOrderedObject.parseWithException (in);

			// Convert to nice format

			result = jsonObjectToString (o, f_friendly, f_compact);
		}
		catch (ParseException e) {
			throw new RuntimeException ("GeoJsonUtils.jsonReaderToString: JSON parse error", e);
		}
		catch (IOException e) {
			throw new RuntimeException ("GeoJsonUtils.jsonReaderToString: I/O error", e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GeoJsonUtils.jsonReaderToString: Unexpected error", e);
		}

		return result;
	}




	// Given a file containing JSON, convert it to nicely-formatted JSON.
	// Parameters:
	//  filename = Name of file containing JSON.
	//  f_friendly = True to produce a friendly display format, false to produce nicely-formatted valid JSON.
	//  f_compact = True to produce a compact format with some map items or array elements on the same line.
	// Return a string containing nicely-formatted JSON, which is valid JSON if f_friendly is false.
	// Throws an exception if error parsing input.

	public static String jsonFileToString (String filename, boolean f_friendly, boolean f_compact) {
		String result = null;

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (filename));
		){

			// Parse the JSON

			Object o = JSONOrderedObject.parseWithException (file_reader);

			// Convert to nice format

			result = jsonObjectToString (o, f_friendly, f_compact);
		}
		catch (ParseException e) {
			throw new RuntimeException ("GeoJsonUtils.jsonFileToString: JSON parse error reading file: " + filename, e);
		}
		catch (IOException e) {
			throw new RuntimeException ("GeoJsonUtils.jsonFileToString: I/O error reading file: " + filename, e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GeoJsonUtils.jsonFileToString: Unexpected error", e);
		}

		return result;
	}




	//----- Access functions -----




	// Get the sub-object identified by the given sequence of keys.
	// This function walks down a sequence of containers according to the given keys.
	// Each key must be String or Integer.
	// If a key is String, the corresponding container must be JSONObject.
	// If a key is Integer, the corresponding container must be JSONArray.
	// Returns null if any container does not exist or is the wrong type,
	// of if the key is not found (for String) or out-of-range (for Integer).

	public static Object getSubObject (final Object x, Object... keys) {

		// Object to return

		Object o = x;

		// Scan list of keys

		for (Object key : keys) {

			// String key

			if (key instanceof String) {
				if (o != null) {
					if (o instanceof JSONObject) {
						JSONObject m = (JSONObject) o;
						String s = (String) key;
						o = m.get (s);
					} else {
						o = null;
					}
				}
			}

			// Integer key

			else if (key instanceof Integer) {
				if (o != null) {
					if (o instanceof JSONArray) {
						JSONArray a = (JSONArray) o;
						int k = ((Integer) key).intValue();
						if (k >= 0 && k < a.size()) {
							o = a.get (k);
						} else {
							o = null;
						}
					} else {
						o = null;
					}
				}
			}

			// Wrong type of key

			else {
				throw new IllegalArgumentException ("GeoJsonUtils.getSubObject: Invalid key type");
			}
		}

		return o;
	}




	//----- Conversion functions -----




	// Convert to JSONObject.
	// Return null if object is not a JSONObject.
	// See getSubObject for explanation of keys.

	public static JSONObject getJsonObject (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof JSONObject) {
				return (JSONObject) o;
			}
		}
		return null;
	}




	// Convert to JSONArray.
	// Return null if object is not a JSONArray.
	// See getSubObject for explanation of keys.

	public static JSONArray getJsonArray (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof JSONArray) {
				return (JSONArray) o;
			}
		}
		return null;
	}




	// Convert to BigDecimal.
	// Return null if object is not a BigDecimal.
	// See getSubObject for explanation of keys.
	// Note: Floating point numbers can be stored as BigDecimal to avoid rounding.

	public static BigDecimal getBigDecimal (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Number) {
				return new BigDecimal (((Number) o).toString());
			}
		}
		return null;
	}




	// Convert to String.
	// Return null if object is not a String.
	// See getSubObject for explanation of keys.

	public static String getString (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof String) {
				return (String) o;
			}
		}
		return null;
	}




	// Convert to Long.
	// Return null if object is not an integral value.
	// See getSubObject for explanation of keys.

	public static Long getLong (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Number) {
				if (!( o instanceof Double || o instanceof Float )) {
					return Long.valueOf (((Number) o).longValue());
				}
			}
		}
		return null;
	}




	// Convert to Integer.
	// Return null if object is not an integral value within the range of an integer.
	// See getSubObject for explanation of keys.

	public static Integer getInteger (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Number) {
				if (!( o instanceof Double || o instanceof Float )) {
					long y = ((Number) o).longValue();
					if (y >= (long)Integer.MIN_VALUE && y <= (long)Integer.MAX_VALUE) {
						return Integer.valueOf ((int) y);
					}
				}
			}
		}
		return null;
	}




	// Convert to Date.
	// Return null if object is not a date or an integral value.
	// See getSubObject for explanation of keys.

	public static Date getDate (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Number) {
				if (!( o instanceof Double || o instanceof Float )) {
					return new Date (((Number) o).longValue());
				}
			} else if (o instanceof Date) {
				return (Date) o;
			}
		}
		return null;
	}




	// Convert to Long containing a time in milliseconds since the epoch.
	// Return null if object is not a date or an integral value.
	// See getSubObject for explanation of keys.

	public static Long getTimeMillis (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Number) {
				if (!( o instanceof Double || o instanceof Float )) {
					return Long.valueOf (((Number) o).longValue());
				}
			} else if (o instanceof Date) {
				return Long.valueOf (((Date) o).getTime());
			}
		}
		return null;
	}




	// Convert to Double.
	// Return null if object is not a numeric value.
	// See getSubObject for explanation of keys.

	public static Double getDouble (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Number) {
				return Double.valueOf (((Number) o).doubleValue());
			}
		}
		return null;
	}




	// Convert to Boolean.
	// Return null if object is not a Boolean.
	// See getSubObject for explanation of keys.

	public static Boolean getBoolean (final Object x, Object... keys) {
		Object o = getSubObject (x, keys);
		if (o != null) {
			if (o instanceof Boolean) {
				return (Boolean) o;
			}
		}
		return null;
	}




	//----- Top-level properties -----




	public static BigDecimal getMag (JSONObject event) {
		return getBigDecimal (event, "properties", "mag");
	}

	public static BigDecimal getCdi (JSONObject event) {
		return getBigDecimal (event, "properties", "cdi");
	}

	public static BigDecimal getMmi (JSONObject event) {
		return getBigDecimal (event, "properties", "mmi");
	}

	public static BigDecimal getDmin (JSONObject event) {
		return getBigDecimal (event, "properties", "dmin");
	}

	public static BigDecimal getRms (JSONObject event) {
		return getBigDecimal (event, "properties", "rms");
	}

	public static BigDecimal getGap (JSONObject event) {
		return getBigDecimal (event, "properties", "gap");
	}

	public static BigDecimal getLongitude (JSONObject event) {
		return getBigDecimal (event, "geometry", "coordinates", 0);
	}

	public static BigDecimal getLatitude (JSONObject event) {
		return getBigDecimal (event, "geometry", "coordinates", 1);
	}

	public static BigDecimal getDepth (JSONObject event) {
		return getBigDecimal (event, "geometry", "coordinates", 2);
	}

	public static String getPlace (JSONObject event) {
		return getString (event, "properties", "place");
	}

	public static String getUrl (JSONObject event) {
		return getString (event, "properties", "url");
	}

	public static String getDetail (JSONObject event) {
		return getString (event, "properties", "detail");
	}

	public static String getAlert (JSONObject event) {
		return getString (event, "properties", "alert");
	}

	public static String getStatus (JSONObject event) {
		return getString (event, "properties", "status");
	}

	public static String getNet (JSONObject event) {
		return getString (event, "properties", "net");
	}

	public static String getCode (JSONObject event) {
		return getString (event, "properties", "code");
	}

	public static String getIds (JSONObject event) {
		return getString (event, "properties", "ids");
	}

	public static String getSources (JSONObject event) {
		return getString (event, "properties", "sources");
	}

	public static String getTypes (JSONObject event) {
		return getString (event, "properties", "types");
	}

	public static String getMagType (JSONObject event) {
		return getString (event, "properties", "magType");
	}

	public static Date getTime (JSONObject event) {
		return getDate (event, "properties", "time");
	}

	public static Date getUpdated (JSONObject event) {
		return getDate (event, "properties", "updated");
	}

	public static Integer getTz (JSONObject event) {
		return getInteger (event, "properties", "tz");
	}

	public static Integer getFelt (JSONObject event) {
		return getInteger (event, "properties", "felt");
	}

	public static Integer getTsunami (JSONObject event) {
		return getInteger (event, "properties", "tsunami");
	}

	public static Integer getSig (JSONObject event) {
		return getInteger (event, "properties", "sig");
	}

	public static Integer getNst (JSONObject event) {
		return getInteger (event, "properties", "nst");
	}

	


	//----- Testing -----




	// Show a named value, or null

	private static void test_show_value (String name, BigDecimal x) {
		if (x == null) {
			System.out.println (name + " = <null>");
		} else {
			System.out.println (name + " = " + x.toString());
		}
		return;
	}

	private static void test_show_value (String name, String x) {
		if (x == null) {
			System.out.println (name + " = <null>");
		} else {
			System.out.println (name + " = " + x);
		}
		return;
	}

	private static void test_show_value (String name, Date x) {
		if (x == null) {
			System.out.println (name + " = <null>");
		} else {
			SimpleDateFormat fmt = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss z");
			fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
			System.out.println (name + " = " + x.getTime() + " (" + fmt.format (x) + ")");
		}
		return;
	}

	private static void test_show_value (String name, Integer x) {
		if (x == null) {
			System.out.println (name + " = <null>");
		} else {
			System.out.println (name + " = " + x.toString());
		}
		return;
	}






	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("GeoJsonUtils : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id
		// Fetch information for an event, and display it.
		// Then display all the top-level parameters.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("GeoJsonUtils : Invalid 'test1' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();
				JSONObject event = accessor.get_last_geojson();

				test_show_value ("mag", getMag (event));
				test_show_value ("cdi", getCdi (event));
				test_show_value ("mmi", getMmi (event));
				test_show_value ("dmin", getDmin (event));
				test_show_value ("rms", getRms (event));
				test_show_value ("gap", getGap (event));
				test_show_value ("longitude", getLongitude (event));
				test_show_value ("latitude", getLatitude (event));
				test_show_value ("depth", getDepth (event));
				test_show_value ("place", getPlace (event));
				test_show_value ("url", getUrl (event));
				test_show_value ("detail", getDetail (event));
				test_show_value ("alert", getAlert (event));
				test_show_value ("status", getStatus (event));
				test_show_value ("net", getNet (event));
				test_show_value ("code", getCode (event));
				test_show_value ("ids", getIds (event));
				test_show_value ("sources", getSources (event));
				test_show_value ("types", getTypes (event));
				test_show_value ("magType", getMagType (event));
				test_show_value ("time", getTime (event));
				test_show_value ("updated", getUpdated (event));
				test_show_value ("tz", getTz (event));
				test_show_value ("felt", getFelt (event));
				test_show_value ("tsunami", getTsunami (event));
				test_show_value ("sig", getSig (event));
				test_show_value ("nst", getNst (event));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id
		// Fetch information for an event, and display it.
		// Then display the GeoJSON various ways.

		if (args[0].equalsIgnoreCase ("test2")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("GeoJsonUtils : Invalid 'test2' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();
				JSONObject event = accessor.get_last_geojson();

				// Display raw json

				System.out.println ();
				System.out.println ("********** Raw JSON **********");
				System.out.println ();

				String json_string = event.toJSONString();

				System.out.println (json_string);

				// Convert object to friendly string

				System.out.println ();
				System.out.println ("********** Object to Friendly String **********");
				System.out.println ();

				System.out.println (jsonObjectToString (event, true, false));

				// Convert object to valid string

				System.out.println ();
				System.out.println ("********** Object to Valid String **********");
				System.out.println ();

				System.out.println (jsonObjectToString (event, false, false));

				// Convert object to friendly compact string

				System.out.println ();
				System.out.println ("********** Object to Friendly Compact String **********");
				System.out.println ();

				System.out.println (jsonObjectToString (event, true, true));

				// Convert object to valid compact string

				System.out.println ();
				System.out.println ("********** Object to Valid Compact String **********");
				System.out.println ();

				System.out.println (jsonObjectToString (event, false, true));

				// Convert string to friendly string

				System.out.println ();
				System.out.println ("********** String to Friendly String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, true, false));

				// Convert string to valid string

				System.out.println ();
				System.out.println ("********** String to Valid String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, false, false));

				// Convert string to friendly compact string

				System.out.println ();
				System.out.println ("********** String to Friendly Compact String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, true, true));

				// Convert string to valid compact string

				System.out.println ();
				System.out.println ("********** String to Valid Compact String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, false, true));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id  filename
		// Fetch information for an event, and display it.
		// Write the raw GeoJSON to a file, then display the file various ways.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("GeoJsonUtils : Invalid 'test3' subcommand");
				return;
			}

			String event_id = args[1];
			String filename = args[2];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();
				JSONObject event = accessor.get_last_geojson();

				// Write raw json

				System.out.println ();
				System.out.println ("Writing file: " + filename);

				String json_string = event.toJSONString();

				SimpleUtils.write_string_as_file (filename, json_string);

				// Convert file to friendly string

				System.out.println ();
				System.out.println ("********** File to Friendly String **********");
				System.out.println ();

				System.out.println (jsonFileToString (filename, true, false));

				// Convert file to valid string

				System.out.println ();
				System.out.println ("********** File to Valid String **********");
				System.out.println ();

				System.out.println (jsonFileToString (filename, false, false));

				// Convert file to friendly compact string

				System.out.println ();
				System.out.println ("********** File to Friendly Compact String **********");
				System.out.println ();

				System.out.println (jsonFileToString (filename, true, true));

				// Convert file to valid compact string

				System.out.println ();
				System.out.println ("********** File to Valid Compact String **********");
				System.out.println ();

				System.out.println (jsonFileToString (filename, false, true));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  url_spec
		// Fetch a JSON file from a URL, and display it.
		// Then display the JSON various ways.

		if (args[0].equalsIgnoreCase ("test4")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("GeoJsonUtils : Invalid 'test4' subcommand");
				return;
			}

			String url_spec = args[1];

			try {

				// Say hello

				System.out.println ("Fetching URL: " + url_spec);

				// Get the File

				String json_string = ComcatProduct.read_string_from_url (url_spec, -1L, -1L);

				// Display raw json

				System.out.println ();
				System.out.println ("********** Raw JSON **********");
				System.out.println ();

				System.out.println (json_string);

				// Convert string to friendly string

				System.out.println ();
				System.out.println ("********** String to Friendly String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, true, false));

				// Convert string to valid string

				System.out.println ();
				System.out.println ("********** String to Valid String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, false, false));

				// Convert string to friendly compact string

				System.out.println ();
				System.out.println ("********** String to Friendly Compact String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, true, true));

				// Convert string to valid compact string

				System.out.println ();
				System.out.println ("********** String to Valid Compact String **********");
				System.out.println ();

				System.out.println (jsonStringToString (json_string, false, true));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  in_filename  f_friendly  f_compact  out_filename
		// Read a JSON file, then convert it according to the flags.
		// If f_friendly is true, use a friendly format that is not valid JSON; if false, use nicely-formatted valid JSON.
		// If f_compact is true, use a compact format (multiple values per line).
		// If out_filename is "-" then write to standard output.
		// This can be used as a JSON pretty-print.

		if (args[0].equalsIgnoreCase ("test5")) {

			// Four additional argument

			if (args.length != 5) {
				System.err.println ("GeoJsonUtils : Invalid 'test5' subcommand");
				return;
			}

			String in_filename = args[1];
			boolean f_friendly = Boolean.parseBoolean (args[2]);
			boolean f_compact = Boolean.parseBoolean (args[3]);
			String out_filename = args[4];

			try {

				// Read and convert the file

				String contents = jsonFileToString (in_filename, f_friendly, f_compact);

				// Write the result

				if (out_filename.equals ("-")) {
					System.out.println (contents);
				}
				else {
					SimpleUtils.write_string_as_file (out_filename, contents);
					System.out.println ("Converted " + in_filename + " --> " + out_filename);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("GeoJsonUtils : Unrecognized subcommand : " + args[0]);
		return;

	}

}
