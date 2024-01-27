package org.opensha.oaf.util;

import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;

import org.opensha.oaf.comcat.GeoJsonUtils;


// Class to hold some simple utility functions for marshaling.
// Author: Michael Barall 03/10/2022.
//
// All functions in this class are static.

public class MarshalUtils {

	// Integer codes to represent various types.

	public static final int MTYPE_NULL = 0;
	public static final int MTYPE_INTEGER = 1;
	public static final int MTYPE_LONG = 2;
	public static final int MTYPE_FLOAT = 3;
	public static final int MTYPE_DOUBLE = 4;
	public static final int MTYPE_BOOLEAN = 5;
	public static final int MTYPE_STRING = 6;

	// The value used for a null object.

	public static final int MVALUE_NULL = 0;




	// Marshal the type code for an object of polymorphic type, followed by the object.
	// The object must be one of the following types:
	//  Integer, Long, Float, Double, Boolean, String, or null.

	public static void marshalTypeAndPoly (MarshalWriter writer, String type_name, String name, Object x) {
		if (x == null) {
			writer.marshalInt (type_name, MTYPE_NULL);
			writer.marshalInt (name, MVALUE_NULL);

		} else if (x instanceof Integer) {
			writer.marshalInt (type_name, MTYPE_INTEGER);
			writer.marshalInt (name, (Integer)x);

		} else if (x instanceof Long) {
			writer.marshalInt (type_name, MTYPE_LONG);
			writer.marshalLong (name, (Long)x);

		} else if (x instanceof Float) {
			writer.marshalInt (type_name, MTYPE_FLOAT);
			writer.marshalFloat (name, (Float)x);

		} else if (x instanceof Double) {
			writer.marshalInt (type_name, MTYPE_DOUBLE);
			writer.marshalDouble (name, (Double)x);

		} else if (x instanceof Boolean) {
			writer.marshalInt (type_name, MTYPE_BOOLEAN);
			writer.marshalBoolean (name, (Boolean)x);

		} else if (x instanceof String) {
			writer.marshalInt (type_name, MTYPE_STRING);
			writer.marshalString (name, (String)x);

		} else {
			throw new MarshalException ("MarshalUtils.marshalTypeAndPoly: Unsupported object type: " + x.getClass().getName());
		}
		return;
	}




	// Unmarshal the type code for an object of polymorphic type, followed by the object.
	// The object must be one of the following types:
	//  Integer, Long, Float, Double, Boolean, String, or null.

	public static Object unmarshalTypeAndPoly (MarshalReader reader, String type_name, String name) {
		int mtype = reader.unmarshalInt (type_name);
		Object x = null;

		switch (mtype) {

		case MTYPE_NULL:
			reader.unmarshalInt (name, MVALUE_NULL, MVALUE_NULL);
			break;

		case MTYPE_INTEGER:
			x = reader.unmarshalInt (name);
			break;

		case MTYPE_LONG:
			x = reader.unmarshalLong (name);
			break;

		case MTYPE_FLOAT:
			x = reader.unmarshalFloat (name);
			break;

		case MTYPE_DOUBLE:
			x = reader.unmarshalDouble (name);
			break;

		case MTYPE_BOOLEAN:
			x = reader.unmarshalBoolean (name);
			break;

		case MTYPE_STRING:
			x = reader.unmarshalString (name);
			break;

		default:
			throw new MarshalException ("MarshalUtils.unmarshalTypeAndPoly: Invalid object type code: " + mtype);
		}

		return x;
	}




	// Marshal an object of polymorphic type.
	// The object must be one of the following types:
	//  Integer, Long, Float, Double, Boolean, String, or null.
	// Implementation note: The object is represented as a 2-element array,
	// in which the first element is the type code.

	public static void marshalPoly (MarshalWriter writer, String name, Object x) {
		writer.marshalArrayBegin (name, 2);
		marshalTypeAndPoly (writer, null, null, x);
		writer.marshalArrayEnd ();
		return;
	}




	// Unarshal an object of polymorphic type.
	// The object must be one of the following types:
	//  Integer, Long, Float, Double, Boolean, String, or null.
	// Implementation note: The object is represented as a 2-element array,
	// in which the first element is the type code.

	public static Object unmarshalPoly (MarshalReader reader, String name) {
		int len = reader.unmarshalArrayBegin (name);
		if (len != 2) {
			throw new MarshalException ("MarshalUtils.unmarshalPoly: Invalid array length: " + len);
		}
		Object x = unmarshalTypeAndPoly (reader, null, null);
		reader.unmarshalArrayEnd ();
		return x;
	}




	// Marshal an object of type Map<String, String>.
	// Each key and each value must be a non-null string.
	// Implementation note: The map is represented as an array, with two elements
	// for each map entry (key and value).
	// Note: Map entries are written in the order of the map's iterator.

	public static void marshalMapStringString (MarshalWriter writer, String name, Map<String, String> x) {
		int entries = x.size();
		int len = entries * 2;
		writer.marshalArrayBegin (name, len);
		int n = 0;
		for (Map.Entry<String, String> entry : x.entrySet()) {
			writer.marshalString (null, entry.getKey());
			writer.marshalString (null, entry.getValue());
			++n;
		}
		if (n != entries) {
			throw new MarshalException ("MarshalUtils.marshalMapStringString: Map size mis-match: n = " + n + ", entries = " + entries);
		}
		writer.marshalArrayEnd ();
		return;
	}




	// Unmarshal an object of type Map<String, String>.
	// Each key and each value must be a non-null string.
	// Implementation note: The map is represented as an array, with two elements
	// for each map entry (key and value).
	// Note: Map entries are added to x in the order obtained from the store.
	// If the order is significant, then x should be an order-preserving type.

	public static void unmarshalMapStringString (MarshalReader reader, String name, Map<String, String> x) {
		int len = reader.unmarshalArrayBegin (name);
		if (len % 2 != 0) {
			throw new MarshalException ("MarshalUtils.marshalMapStringString: Invalid array length: " + len);
		}
		int entries = len / 2;
		for (int n = 0; n < entries; ++n) {
			String key = reader.unmarshalString (null);
			String value = reader.unmarshalString (null);
			x.put (key, value);
		}
		reader.unmarshalArrayEnd ();
		return;
	}




	// Marshal an object of type Map<String, Object>.
	// Each key must be a non-null string.
	// Each value must be one of the following types:
	//  Integer, Long, Float, Double, Boolean, String, or null.
	// Implementation note: The map is represented as an array, with three elements
	// for each map entry (key, type code, and value).
	// Note: Map entries are written in the order of the map's iterator.

	public static void marshalMapStringObject (MarshalWriter writer, String name, Map<String, Object> x) {
		int entries = x.size();
		int len = entries * 3;
		writer.marshalArrayBegin (name, len);
		int n = 0;
		for (Map.Entry<String, Object> entry : x.entrySet()) {
			writer.marshalString (null, entry.getKey());
			marshalTypeAndPoly (writer, null, null, entry.getValue());
			++n;
		}
		if (n != entries) {
			throw new MarshalException ("MarshalUtils.marshalMapStringObject: Map size mis-match: n = " + n + ", entries = " + entries);
		}
		writer.marshalArrayEnd ();
		return;
	}




	// Unmarshal an object of type Map<String, Object>.
	// Each key must be a non-null string.
	// Each value must be one of the following types:
	//  Integer, Long, Float, Double, Boolean, String, or null.
	// Implementation note: The map is represented as an array, with three elements
	// for each map entry (key, type code, and value).
	// Note: Map entries are added to x in the order obtained from the store.
	// If the order is significant, then x should be an order-preserving type.

	public static void unmarshalMapStringObject (MarshalReader reader, String name, Map<String, Object> x) {
		int len = reader.unmarshalArrayBegin (name);
		if (len % 3 != 0) {
			throw new MarshalException ("MarshalUtils.unmarshalMapStringObject: Invalid array length: " + len);
		}
		int entries = len / 3;
		for (int n = 0; n < entries; ++n) {
			String key = reader.unmarshalString (null);
			Object value = unmarshalTypeAndPoly (reader, null, null);
			x.put (key, value);
		}
		reader.unmarshalArrayEnd ();
		return;
	}




	//----- JSON string access -----




	// Write marshalable object to JSON string.

	public static String to_json_string (Marshalable x) {

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		x.marshal (writer, null);
		writer.check_write_complete ();

		String json_string = writer.get_json_string();

		return json_string;
	}




	// Write marshalable object to nicely-formatted JSON string.
	// Note: The returned string is valid JSON.

	public static String to_formatted_json_string (Marshalable x) {

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		x.marshal (writer, null);
		writer.check_write_complete ();

		Object json_container = writer.get_json_container();
		String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, false);

		return formatted_string;
	}




	// Write marshalable object to nicely-formatted compact JSON string.
	// Note: The returned string is valid JSON.

	public static String to_formatted_compact_json_string (Marshalable x) {

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		x.marshal (writer, null);
		writer.check_write_complete ();

		Object json_container = writer.get_json_container();
		String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);

		return formatted_string;
	}




	// Read marshalable object from JSON string.

	public static void from_json_string (Marshalable x, String json_string) {

		MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);
		x.unmarshal (reader, null);
		reader.check_read_complete ();

		return;
	}




	// Convenience function to convert a JSON string to display format.
	// Note: The returned string is not valid JSON, but is intended to be human-readable.
	// Note: This function just calls GeoJsonUtils.jsonStringToString().

	public static String display_json_string (String json_string) {
		return GeoJsonUtils.jsonStringToString (json_string, true, true);
	}




	//----- JSON file access -----




	// Write marshalable object to JSON file.

	public static void to_json_file (Marshalable x, String filename) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (filename));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			x.marshal (writer, null);
			writer.check_write_complete ();
			writer.write_json_file (file_writer);
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while writing JSON file: " + filename, e);
		}

		return;
	}




	// Write marshalable object to nicely-formatted JSON file.

	public static void to_formatted_json_file (Marshalable x, String filename) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (filename));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			x.marshal (writer, null);
			writer.check_write_complete ();

			Object json_container = writer.get_json_container();
			String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, false);

			file_writer.write (formatted_string);
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while writing JSON file: " + filename, e);
		}

		return;
	}




	// Write marshalable object to nicely-formatted compact JSON file.

	public static void to_formatted_compact_json_file (Marshalable x, String filename) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (filename));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			x.marshal (writer, null);
			writer.check_write_complete ();

			Object json_container = writer.get_json_container();
			String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);

			file_writer.write (formatted_string);
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while writing JSON file: " + filename, e);
		}

		return;
	}




	// Read marshalable object from JSON file.

	public static void from_json_file (Marshalable x, String filename) {

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (filename));
		){
			MarshalImpJsonReader reader = new MarshalImpJsonReader (file_reader);
			x.unmarshal (reader, null);
			reader.check_read_complete ();
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while reading JSON file: " + filename, e);
		}

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MarshalUtils : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  num_reps
		// Test marshaling and unmarshaling of Map<String, Object> with the given number of repetitions.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("MarshalUtils : Invalid 'test1' subcommand");
				return;
			}
			int num_reps = Integer.parseInt(args[1]);

			System.out.println ("num_reps = " + num_reps);

			// Create the map

			LinkedHashMap<String, Object> xmap = new LinkedHashMap<String, Object>();

			for (int n = 0; n < num_reps; ++n) {
				xmap.put ("null_" + n, null);
				xmap.put ("int_" + n, 987000000 + n);
				xmap.put ("long_" + n, 123456987000000L + (long)n);
				xmap.put ("float_" + n, (float)(0.98765432111 + (double)n));
				xmap.put ("double_" + n, 0.98765432122 + (double)n);
				xmap.put ("boolean_" + n, n % 2 == 0);
				xmap.put ("string_" + n, "s_" + n);
			}

			// Marshal the data

			System.out.println();
			System.out.println ("Marshaling data ...");

			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();

			writer.marshalMapBegin (null);

			marshalMapStringObject (writer, "xmap", xmap);

			writer.marshalMapEnd ();

			if (!( writer.check_write_complete() )) {
				System.out.println ("Writer reports writing not complete");
				return;
			}

			String json_string = writer.get_json_string();

			writer = null;

			// Display the JSON

			System.out.println();
			System.out.println ("JSON data ...");
			System.out.println();
			System.out.println (json_string);

			// Unmarshal the data

			System.out.println();
			System.out.println ("Unmarshaling data ...");

			MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);

			reader.unmarshalMapBegin (null);

			LinkedHashMap<String, Object> xmap2 = new LinkedHashMap<String, Object>();

			unmarshalMapStringObject (reader, "xmap", xmap2);

			reader.unmarshalMapEnd ();

			if (!( reader.check_read_complete() )) {
				System.out.println ("Reader reports reading not complete");
				return;
			}

			// Display the map

			System.out.println();

			for (Map.Entry<String, Object> entry : xmap2.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				System.out.println (key + " = " + ((value == null) ? "<null>" : value.toString()));
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  num_reps
		// Test marshaling and unmarshaling of Map<String, String> with the given number of repetitions.

		if (args[0].equalsIgnoreCase ("test2")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("MarshalUtils : Invalid 'test2' subcommand");
				return;
			}
			int num_reps = Integer.parseInt(args[1]);

			System.out.println ("num_reps = " + num_reps);

			// Create the map

			LinkedHashMap<String, String> xmap = new LinkedHashMap<String, String>();

			for (int n = 0; n < num_reps; ++n) {
				xmap.put ("null_" + n, "<null>");
				xmap.put ("int_" + n, Integer.toString (987000000 + n));
				xmap.put ("long_" + n, Long.toString (123456987000000L + (long)n));
				xmap.put ("float_" + n, Float.toString ((float)(0.98765432111 + (double)n)));
				xmap.put ("double_" + n, Double.toString (0.98765432122 + (double)n));
				xmap.put ("boolean_" + n, Boolean.toString (n % 2 == 0));
				xmap.put ("string_" + n, "s_" + n);
			}

			// Marshal the data

			System.out.println();
			System.out.println ("Marshaling data ...");

			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();

			writer.marshalMapBegin (null);

			marshalMapStringString (writer, "xmap", xmap);

			writer.marshalMapEnd ();

			if (!( writer.check_write_complete() )) {
				System.out.println ("Writer reports writing not complete");
				return;
			}

			String json_string = writer.get_json_string();

			writer = null;

			// Display the JSON

			System.out.println();
			System.out.println ("JSON data ...");
			System.out.println();
			System.out.println (json_string);

			// Unmarshal the data

			System.out.println();
			System.out.println ("Unmarshaling data ...");

			MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);

			reader.unmarshalMapBegin (null);

			LinkedHashMap<String, String> xmap2 = new LinkedHashMap<String, String>();

			unmarshalMapStringString (reader, "xmap", xmap2);

			reader.unmarshalMapEnd ();

			if (!( reader.check_read_complete() )) {
				System.out.println ("Reader reports reading not complete");
				return;
			}

			// Display the map

			System.out.println();

			for (Map.Entry<String, String> entry : xmap2.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				System.out.println (key + " = " + value);
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  num_reps
		// Test marshaling and unmarshaling of polymorphic objects with the given number of repetitions.

		if (args[0].equalsIgnoreCase ("test3")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("MarshalUtils : Invalid 'test3' subcommand");
				return;
			}
			int num_reps = Integer.parseInt(args[1]);

			System.out.println ("num_reps = " + num_reps);

			// Create the map

			LinkedHashMap<String, Object> xmap = new LinkedHashMap<String, Object>();

			for (int n = 0; n < num_reps; ++n) {
				xmap.put ("null_" + n, null);
				xmap.put ("int_" + n, 987000000 + n);
				xmap.put ("long_" + n, 123456987000000L + (long)n);
				xmap.put ("float_" + n, (float)(0.98765432111 + (double)n));
				xmap.put ("double_" + n, 0.98765432122 + (double)n);
				xmap.put ("boolean_" + n, n % 2 == 0);
				xmap.put ("string_" + n, "s_" + n);
			}

			// Marshal the data

			System.out.println();
			System.out.println ("Marshaling data ...");

			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();

			writer.marshalMapBegin (null);

			for (int n = 0; n < num_reps; ++n) {
				marshalPoly (writer, "null_" + n, null);
				marshalPoly (writer, "int_" + n, 987000000 + n);
				marshalPoly (writer, "long_" + n, 123456987000000L + (long)n);
				marshalPoly (writer, "float_" + n, (float)(0.98765432111 + (double)n));
				marshalPoly (writer, "double_" + n, 0.98765432122 + (double)n);
				marshalPoly (writer, "boolean_" + n, n % 2 == 0);
				marshalPoly (writer, "string_" + n, "s_" + n);
			}

			writer.marshalMapEnd ();

			if (!( writer.check_write_complete() )) {
				System.out.println ("Writer reports writing not complete");
				return;
			}

			String json_string = writer.get_json_string();

			writer = null;

			// Display the JSON

			System.out.println();
			System.out.println ("JSON data ...");
			System.out.println();
			System.out.println (json_string);

			// Unmarshal the data

			System.out.println();
			System.out.println ("Unmarshaling data ...");
			System.out.println();

			MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);

			reader.unmarshalMapBegin (null);

			for (int n = 0; n < num_reps; ++n) {
				Object value;
				value = unmarshalPoly (reader, "null_" + n);
				System.out.println            ("null_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
				value = unmarshalPoly (reader, "int_" + n);
				System.out.println            ("int_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
				value = unmarshalPoly (reader, "long_" + n);
				System.out.println            ("long_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
				value = unmarshalPoly (reader, "float_" + n);
				System.out.println            ("float_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
				value = unmarshalPoly (reader, "double_" + n);
				System.out.println            ("double_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
				value = unmarshalPoly (reader, "boolean_" + n);
				System.out.println            ("boolean_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
				value = unmarshalPoly (reader, "string_" + n);
				System.out.println            ("string_" + n + " = " + ((value == null) ? "<null>" : value.toString()));
			}

			reader.unmarshalMapEnd ();

			if (!( reader.check_read_complete() )) {
				System.out.println ("Reader reports reading not complete");
				return;
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("MarshalUtils : Unrecognized subcommand : " + args[0]);
		return;

	}




}
