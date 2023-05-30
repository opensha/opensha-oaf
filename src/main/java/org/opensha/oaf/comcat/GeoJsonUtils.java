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
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.opensha.oaf.util.JSONOrderedObject;

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




	// Convert a JSONObject to a string.
	// The result is a valid nicely-formatted JSON file.
	// This is primarily a debugging function.

	public static String jsonObjectToString (Object o) {
		StringBuilder sb = new StringBuilder();
		jsonObjectToString (sb, o, null, "", false);
		return sb.toString();
	}


	public static void jsonObjectToString (StringBuilder sb, Object o, String name, String prefix, boolean f_comma) {

		// Write prefix and name

		sb.append (prefix);
		if (name != null) {
			sb.append ("\"");
			escapeString (sb, name);
			sb.append ("\"");
			sb.append (": ");
		}

		// Handle null

		if (o == null) {
			sb.append ("null");
			sb.append (f_comma ? ",\n" : "\n");
		}

		// Handle string

		else if (o instanceof String) {
			String s = (String) o;
			sb.append ("\"");
			escapeString (sb, s);
			sb.append ("\"");
			sb.append (f_comma ? ",\n" : "\n");
		}

		// Handle object (Map)

		else if (o instanceof JSONObject) {
			JSONObject m = (JSONObject) o;
			sb.append ("{");
			sb.append ("\n");
			String new_prefix = prefix + "  ";
			int n = m.size();
			int k = 0;
			for (Object key : m.keySet()) {
				Object val = m.get (key);
				jsonObjectToString (sb, val, key.toString(), new_prefix, k + 1 < n);
				++k;
			}
			sb.append (prefix);
			sb.append ("}");
			sb.append (f_comma ? ",\n" : "\n");
		}

		// Handle ordered object (Map)

		else if (o instanceof JSONOrderedObject) {
			JSONOrderedObject m = (JSONOrderedObject) o;
			sb.append ("{");
			sb.append ("\n");
			String new_prefix = prefix + "  ";
			int n = m.size();
			int k = 0;
			for (Object key : m.keySet()) {
				Object val = m.get (key);
				jsonObjectToString (sb, val, key.toString(), new_prefix, k + 1 < n);
				++k;
			}
			sb.append (prefix);
			sb.append ("}");
			sb.append (f_comma ? ",\n" : "\n");
		}

		// Handle array (List)

		else if (o instanceof JSONArray) {
			JSONArray a = (JSONArray) o;
			sb.append ("[");
			sb.append ("\n");
			String new_prefix = prefix + "  ";
			int n = a.size();
			for (int k = 0; k < n; ++k) {
				Object val = a.get (k);
				jsonObjectToString (sb, val, Integer.toString (k), new_prefix, k + 1 < n);
			}
			sb.append (prefix);
			sb.append ("]");
			sb.append (f_comma ? ",\n" : "\n");
		}

		// Anything else

		else {
			sb.append (o.toString());
			sb.append (f_comma ? ",\n" : "\n");
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
					return new Long (((Number) o).longValue());
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
						return new Integer ((int) y);
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
					return new Long (((Number) o).longValue());
				}
			} else if (o instanceof Date) {
				return new Long (((Date) o).getTime());
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
				return new Double (((Number) o).doubleValue());
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




		// Unrecognized subcommand.

		System.err.println ("GeoJsonUtils : Unrecognized subcommand : " + args[0]);
		return;

	}

}
