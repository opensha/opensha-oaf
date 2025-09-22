package org.opensha.oaf.util;

import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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




	// Unmarshal an object of polymorphic type.
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




	// Convenience function to convert a JSON string to display format.
	// Note: The returned string is valid JSON, and is intended to be human-readable.
	// Note: This function just calls GeoJsonUtils.jsonStringToString().

	public static String display_valid_json_string (String json_string) {
		return GeoJsonUtils.jsonStringToString (json_string, false, true);
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

	public static void to_json_file (Marshalable x, File file) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (file));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			x.marshal (writer, null);
			writer.check_write_complete ();
			writer.write_json_file (file_writer);
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while writing JSON file: " + file.getPath(), e);
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

	public static void to_formatted_json_file (Marshalable x, File file) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (file));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			x.marshal (writer, null);
			writer.check_write_complete ();

			Object json_container = writer.get_json_container();
			String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, false);

			file_writer.write (formatted_string);
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while writing JSON file: " + file.getPath(), e);
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

	public static void to_formatted_compact_json_file (Marshalable x, File file) {

		try (
			BufferedWriter file_writer = new BufferedWriter (new FileWriter (file));
		){
			MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
			x.marshal (writer, null);
			writer.check_write_complete ();

			Object json_container = writer.get_json_container();
			String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);

			file_writer.write (formatted_string);
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while writing JSON file: " + file.getPath(), e);
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

	public static void from_json_file (Marshalable x, File file) {

		try (
			BufferedReader file_reader = new BufferedReader (new FileReader (file));
		){
			MarshalImpJsonReader reader = new MarshalImpJsonReader (file_reader);
			x.unmarshal (reader, null);
			reader.check_read_complete ();
		}
		catch (IOException e) {
			throw new MarshalException ("MarshalUtils: I/O error while reading JSON file: " + file.getPath(), e);
		}

		return;
	}




	//----- Reader/Writer creation -----




	// Create a reader for a line of text.
	// Parameters:
	//  line = Line of text, with words separated by whitespace and using JSON escape sequences.
	//  f_store_names = True if field names are stored in the line.
	// Returns a subclass of MarshalReader.

	public static MarshalImpInputReader reader_for_line (String line, boolean f_store_names) {
		return new MarshalImpInputReader (
			new MarshalInputLine (line),
			f_store_names
		);
	}




	// Create a writer for a line of text.
	// Parameters:
	//  sb = Destination for output, with words separated by whitespace and using JSON escape sequences.
	//  f_unicode = True if output can contain Unicode characters U+0080 to U+FFFF.
	//  f_quote_all = True to wrap all strings in quotes, false to use quotes only when necessary.
	//  f_store_names = True if field names are stored in the line.
	// Returns a subclass of MarshalWriter.

	public static MarshalImpOutputWriter writer_for_line (StringBuilder sb, boolean f_unicode, boolean f_quote_all, boolean f_store_names) {
		return new MarshalImpOutputWriter (
			new MarshalOutputLine (sb, f_unicode, f_quote_all),
			f_store_names
		);
	}




	// Create a reader for a binary file.
	// Parameters:
	//  data_in = Binary data input.
	//  f_store_name = True if field names are stored in the file.
	// Returns a subclass of MarshalReader.
	// Note: A data input (subclass of DataInput) can be created from a filename like this:
	//  new DataInputStream (new BufferedInputStream (new FileInputStream (filename)))

	public static MarshalImpInputReader reader_for_binary_file (DataInput data_in, boolean f_store_names) {
		return new MarshalImpInputReader (
			new MarshalInputBinaryStream (data_in),
			f_store_names
		);
	}




	// Create a writer for a binary file.
	// Parameters:
	//  data_out = Binary data output.
	//  f_store_name = True if field names are stored in the file.
	// Returns a subclass of MarshalWriter.
	// Note: A data output (subclass of DataOutput) can be created from a filename like this:
	//  new DataOutputStream (new BufferedOutputStream (new FileOutputStream (filename)))

	public static MarshalImpOutputWriter writer_for_binary_file (DataOutput data_out, boolean f_store_names) {
		return new MarshalImpOutputWriter (
			new MarshalOutputBinaryStream (data_out),
			f_store_names
		);
	}




	//----- Functional interfaces -----




	// A lambda function that marshals an object of type T, given a writer and a name.  The signature is:
	//   (MarshalWriter writer, String name, T obj) -> void
	// This signature typically matches:
	//  - A static marshaling method (often called lambda_marshal or marshal_poly).
	// Note: An instance marshaling method typically has the same arguments but in a
	// different order.  It can be wrapped in mifcn to convert it to a marshal function.

	@FunctionalInterface
	public static interface MarshalFunction<T> {
		public void lambda_marshal (MarshalWriter writer, String name, T obj);
	}




	// A lambda function that unmarshals an object of type T, given a reader and a name.  The signature is:
	//   (MarshalReader reader, String name) -> T
	// This signature typically matches:
	//  - A static unmarshaling method (often called static_unmarshal or unmarshal_poly).
	//  - An instance unmarshaling method that is bound to an object (using the obj::method syntax).
	//  - A constructor that performs unmarshaling.
	// Note: An instance unmarshaling method typically can be wrapped in uifcn to convert it into
	// an unmarshal function, which uses a supplier to allocate a new object on each call.
	// Note: An instance unmarshaling method typically can be wrapped in uibfcn to convert it into
	// a bound unmarshal function, which always unmarshals into the same bound object; this is the
	// same effect as using the object::method syntax.

	@FunctionalInterface
	public static interface UnmarshalFunction<T> {
		public T lambda_unmarshal (MarshalReader reader, String name);
	}




	// An instance method for marshaling an object (signature of Marshalable.marshal).  The signature is:
	//   (T this, MarshalWriter writer, String name) -> void
	// This is the signature of Marshalable.marshal, and typically matches an instance marshaling method.
	// Note: Typically this interface is not used directly but instead convertd to a marshal function.

	@FunctionalInterface
	public static interface MarshalInstanceFunction<T> {
		public void lambda_marshal (T obj, MarshalWriter writer, String name);

		// Convert to a marshal function with the operand order of a static function.

		public default MarshalFunction<T> to_mfcn () {
			return (MarshalWriter writer, String name, T obj) -> {
				lambda_marshal (obj, writer, name);
				return;
			};
		}
	}




	// An instance method for unmarshaling an object (signature of Marshalable.unmarshal).  The signatue is:
	//   (T this, MarshalReader reader, String name) -> T
	// This is the signature of Marshalable.unmarshal, and typically matches an instance unmarshaling method.
	// Note that the function returns the object that was unmarshaled.
	// Note: Typically this interface is not used directly but instead convertd to an unmarshal function.

	@FunctionalInterface
	public static interface UnmarshalInstanceFunction<T> {
		public T lambda_unmarshal (T obj, MarshalReader reader, String name);

		// Convert to an unmarshal function that is bound to an individual object.
		// The unmarshal function always unmarshals into, and returns, the same bound object.

		public default <S extends T> UnmarshalFunction<T> to_bound_ufcn (final S obj) {
			return (MarshalReader reader, String name) -> {
				return lambda_unmarshal (obj, reader, name);
			};
		}

		// Convert to an unmarshal function that is bound to a supplier.
		// The unmarshal function uses the supplier to allocate a new object for each call.

		public default <S extends T> UnmarshalFunction<T> to_ufcn (final Supplier<S> supplier) {
			return (MarshalReader reader, String name) -> {
				return lambda_unmarshal (supplier.get(), reader, name);
			};
		}

		// Convert to an unmarshal function that is bound to a class object.
		// The unmarshal function uses the class object to allocate a new object for each call.

		public default <S extends T> UnmarshalFunction<T> to_class_ufcn (final Class<S> clazz) {
			return (MarshalReader reader, String name) -> {
				T obj;
				try {
					obj = clazz.getDeclaredConstructor().newInstance();
				}
				catch (Exception e) {
					throw new MarshalException ("MarshalUtils.UnmarshalInstanceFunction: Unable to allocate new object: class = " + clazz.getName(), e);
				}
				return lambda_unmarshal (obj, reader, name);
			};
		}
	}




	// Wrapper function to convert an instance marshal method to a marshal function.

	public static <T> MarshalFunction<T> mifcn (MarshalInstanceFunction<T> ifunc) {
		return ifunc.to_mfcn ();
	}




	// Wrapper function to convert an instance unmarshal method to a bound unmarshal function.
	// This binds the given object, so the function always unmarshals into, and returns, the same bound object.

	public static <T, S extends T> UnmarshalFunction<T> uibfcn (UnmarshalInstanceFunction<T> ifunc, S obj) {
		return ifunc.to_bound_ufcn (obj);
	}




	// Wrapper function to convert an instance unmarshal method to an unmarshal function.
	// This binds the given supplier, so the function uses the supplier to allocate a new object on each call.

	public static <T, S extends T> UnmarshalFunction<T> uifcn (UnmarshalInstanceFunction<T> ifunc, Supplier<S> supplier) {
		return ifunc.to_ufcn (supplier);
	}




	// Wrapper function to convert an instance unmarshal method to an unmarshal function.
	// This binds the given class object, so the function uses the class object to allocate a new object on each call.

	public static <T, S extends T> UnmarshalFunction<T> uicfcn (UnmarshalInstanceFunction<T> ifunc, Class<S> clazz) {
		return ifunc.to_class_ufcn (clazz);
	}




	//----- JSON string access for lambda functions -----




	// Write marshalable object to JSON string.

	public static <T> String lambda_to_json_string (MarshalFunction<T> func, T obj) {

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		func.lambda_marshal (writer, null, obj);
		writer.check_write_complete ();

		String json_string = writer.get_json_string();

		return json_string;
	}




	// Write marshalable object to nicely-formatted JSON string.
	// Note: The returned string is valid JSON.

	public static <T> String lambda_to_formatted_json_string (MarshalFunction<T> func, T obj) {

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		func.lambda_marshal (writer, null, obj);
		writer.check_write_complete ();

		Object json_container = writer.get_json_container();
		String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, false);

		return formatted_string;
	}




	// Write marshalable object to nicely-formatted compact JSON string.
	// Note: The returned string is valid JSON.

	public static <T> String lambda_to_formatted_compact_json_string (MarshalFunction<T> func, T obj) {

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		func.lambda_marshal (writer, null, obj);
		writer.check_write_complete ();

		Object json_container = writer.get_json_container();
		String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);

		return formatted_string;
	}




	// Read marshalable object from JSON string.

	public static <T> T lambda_from_json_string (UnmarshalFunction<T> func, String json_string) {

		MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);
		T result = func.lambda_unmarshal (reader, null);
		reader.check_read_complete ();

		return result;
	}




	//----- Special reader-writer access for lambda functions -----




	// Write a marshalable object to a line of text.
	// Parameters:
	//  func = Marshal function to use.
	//  obj = Object to write.
	//  f_unicode = True if output can contain Unicode characters U+0080 to U+FFFF.
	//  f_quote_all = True to wrap all strings in quotes, false to use quotes only when necessary.
	//  f_store_names = True if field names are stored in the line.
	// Returns a string containing the line.

	public static <T, S extends T> String lambda_to_line (MarshalFunction<T> func, S obj, boolean f_unicode, boolean f_quote_all, boolean f_store_names) {
		
		StringBuilder sb = new StringBuilder();
		MarshalWriter writer = writer_for_line (sb, f_unicode, f_quote_all, f_store_names);
		func.lambda_marshal (writer, null, obj);

		writer.write_completion_check();

		return sb.toString();
	}




	// Read a marshalable object from a line of text.
	// Parameters:
	//  func = Unmarshal function to use.
	//  line = Line of text, with words separated by whitespace and using JSON escape sequences.
	//  f_store_names = True if field names are stored in the line.

	public static <T> T lambda_from_line (UnmarshalFunction<T> func, String line, boolean f_store_names) {

		MarshalReader reader = reader_for_line (line, f_store_names);
		T result = func.lambda_unmarshal (reader, null);

		boolean f_require_eof = true;
		reader.read_completion_check (f_require_eof);

		return result;
	}




	// Write a marshalable object to a binary file.
	// Parameters:
	//  func = Marshal function to use.
	//  obj = Object to write.
	//  data_out = Binary data output.
	//  f_store_name = True if field names are stored in the line.
	// Returns a string containing the line.

	public static <T, S extends T> void lambda_to_binary_file (MarshalFunction<T> func, S obj, DataOutput data_out, boolean f_store_names) {
		
		MarshalWriter writer = writer_for_binary_file (data_out, f_store_names);
		func.lambda_marshal (writer, null, obj);

		writer.write_completion_check();

		return;
	}




	// Read a marshalable object from a binary file.
	// Parameters:
	//  func = Unmarshal function to use.
	//  data_in = Binary data input.
	//  f_store_name = True if field names are stored in the line.
	//  f_require_eof = True to attempt to check for end-of-file after reading object.

	public static <T> T lambda_from_binary_file (UnmarshalFunction<T> func, DataInput data_in, boolean f_store_names, boolean f_require_eof) {

		MarshalReader reader = reader_for_binary_file (data_in, f_store_names);
		T result = func.lambda_unmarshal (reader, null);

		reader.read_completion_check (f_require_eof);

		return result;
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




		// Subcommand : Test #4
		// Command format:
		//  test4
		// Test lambda function read/write of JSON strings.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("MarshalUtils : Invalid 'test4' subcommand");
				return;
			}

			// Mkae a circle

			System.out.println ();
			System.out.println ("***** Make circle *****");
			System.out.println ();

			SphRegion circle = SphRegion.makeCircle (new SphLatLon (37.0, -120.0), 50.0);

			System.out.println (circle.toString());


			// --- Tests of JSON string

			// Marshal to JSON string using static method marshal_poly

			System.out.println ();
			System.out.println ("***** Static marshal to JSON string using marshal_poly *****");
			System.out.println ();

			String s_1 = lambda_to_json_string (SphRegion::marshal_poly, circle);

			System.out.println (s_1);

			// Unmarshal from JSON string using static method unmarshal_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from JSON string using unmarshal_poly *****");
			System.out.println ();

			SphRegion circle_2 = lambda_from_json_string (SphRegion::unmarshal_poly, s_1);

			System.out.println (circle_2.toString());

			// Marshal to JSON string using instance method marshal

			System.out.println ();
			System.out.println ("***** Instance marshal to JSON string using marshal (mifcn wrapper) *****");
			System.out.println ();

			String s_2 = lambda_to_json_string (mifcn(SphRegion::marshal), circle);

			System.out.println (s_2);

			System.out.println ();
			System.out.println ("***** Instance unmarshal from JSON string using unmarshal (uifcn wrapper) *****");
			System.out.println ();

			SphRegion circle_3 = lambda_from_json_string (uifcn(SphRegion::unmarshal, SphRegionCircle::new), s_2);

			System.out.println (circle_3.toString());


			// --- Tests of nicely-formatted JSON string

			// Marshal to nicely-formatted JSON string using static method marshal_poly

			System.out.println ();
			System.out.println ("***** Static marshal to nicely-formatted JSON string using marshal_poly *****");
			System.out.println ();

			String s_3 = lambda_to_formatted_json_string (SphRegion::marshal_poly, circle);

			System.out.println (s_3);

			// Unmarshal from JSON string using static method unmarshal_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from nicely-formatted JSON string using unmarshal_poly *****");
			System.out.println ();

			SphRegion circle_4 = lambda_from_json_string (SphRegion::unmarshal_poly, s_3);

			System.out.println (circle_4.toString());

			// Marshal to nicely-formatted JSON string using instance method marshal

			System.out.println ();
			System.out.println ("***** Instance marshal to nicely-formatted JSON string using marshal (mifcn wrapper) *****");
			System.out.println ();

			String s_4 = lambda_to_formatted_json_string (mifcn(SphRegion::marshal), circle);

			System.out.println (s_4);

			System.out.println ();
			System.out.println ("***** Instance unmarshal from nicely-formatted JSON string using unmarshal (uibfcn wrapper) *****");
			System.out.println ();

			//SphRegion circle_5 = lambda_from_json_string (uifcn(SphRegion::unmarshal, SphRegionCircle::new), s_4);
			SphRegionCircle tmp_circle_5 = new SphRegionCircle();
			SphRegion circle_5 = lambda_from_json_string (uibfcn(SphRegion::unmarshal, tmp_circle_5), s_4);
			if (circle_5 != tmp_circle_5) {
				System.out.println ("Bound unmarshal function did not return the bound object!");
				System.out.println ();
			}

			System.out.println (circle_5.toString());


			// --- Tests of nicely-formatted compact JSON string

			// Marshal to nicely-formatted compact JSON string using static method marshal_poly

			System.out.println ();
			System.out.println ("***** Static marshal to nicely-formatted compact JSON string using marshal_poly *****");
			System.out.println ();

			String s_6 = lambda_to_formatted_compact_json_string (SphRegion::marshal_poly, circle);

			System.out.println (s_6);

			// Unmarshal from nicely-formatted compact JSON string using static method unmarshal_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from nicely-formatted compact JSON string using unmarshal_poly *****");
			System.out.println ();

			SphRegion circle_6 = lambda_from_json_string (SphRegion::unmarshal_poly, s_6);

			System.out.println (circle_6.toString());

			// Marshal to nicely-formatted compact JSON string using instance method marshal

			System.out.println ();
			System.out.println ("***** Instance marshal to nicely-formatted compact JSON string using marshal (mifcn wrapper) *****");
			System.out.println ();

			String s_7 = lambda_to_formatted_compact_json_string (mifcn(SphRegion::marshal), circle);

			System.out.println (s_7);

			System.out.println ();
			System.out.println ("***** Instance unmarshal from nicely-formatted compact JSON string using unmarshal (obj::method syntax) *****");
			System.out.println ();

			//SphRegion circle_7 = lambda_from_json_string (uifcn(SphRegion::unmarshal, SphRegionCircle::new), s_7);
			SphRegionCircle tmp_circle_7 = new SphRegionCircle();
			SphRegion circle_7 = lambda_from_json_string (tmp_circle_7::unmarshal, s_7);
			if (circle_7 != tmp_circle_7) {
				System.out.println ("Bound unmarshal function did not return the bound object!");
				System.out.println ();
			}

			System.out.println (circle_7.toString());


			// --- Tests of nicely-formatted compact JSON string using line format

			// Marshal to nicely-formatted compact JSON string using static method marshal_to_line_poly

			System.out.println ();
			System.out.println ("***** Static marshal to nicely-formatted compact JSON string using marshal_to_line_poly *****");
			System.out.println ();

			String s_8 = lambda_to_formatted_compact_json_string (SphRegion::marshal_to_line_poly, circle);

			System.out.println (s_8);

			// Unmarshal from nicely-formatted compact JSON string using static method unmarshal_from_line_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from nicely-formatted compact JSON string using unmarshal_from_line_poly *****");
			System.out.println ();

			SphRegion circle_8 = lambda_from_json_string (SphRegion::unmarshal_from_line_poly, s_8);

			System.out.println (circle_8.toString());


			// --- Tests of plain line using line format

			// Marshal to plain line using static method marshal_to_line_poly

			System.out.println ();
			System.out.println ("***** Static marshal plain line using marshal_to_line_poly *****");
			System.out.println ();

			String s_9 = lambda_to_line (SphRegion::marshal_to_line_poly, circle, false, false, false);

			System.out.println (s_9);

			// Unmarshal from plain line using static method unmarshal_from_line_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from plain line using unmarshal_from_line_poly *****");
			System.out.println ();

			SphRegion circle_9 = lambda_from_line (SphRegion::unmarshal_from_line_poly, s_9, false);

			System.out.println (circle_9.toString());

			// Marshal to plain line with names using static method marshal_to_line_poly

			System.out.println ();
			System.out.println ("***** Static marshal plain line with names using marshal_to_line_poly *****");
			System.out.println ();

			String s_10 = lambda_to_line (SphRegion::marshal_to_line_poly, circle, false, false, true);

			System.out.println (s_10);

			// Unmarshal from plain line with names using static method unmarshal_from_line_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from plain line with names using unmarshal_from_line_poly *****");
			System.out.println ();

			SphRegion circle_10 = lambda_from_line (SphRegion::unmarshal_from_line_poly, s_10, true);

			System.out.println (circle_10.toString());

			// Marshal to plain line with names and quotes using static method marshal_to_line_poly

			System.out.println ();
			System.out.println ("***** Static marshal plain line with names and quotes using marshal_to_line_poly *****");
			System.out.println ();

			String s_11 = lambda_to_line (SphRegion::marshal_to_line_poly, circle, false, true, true);

			System.out.println (s_11);

			// Unmarshal from plain line with names and quotes using static method unmarshal_from_line_poly

			System.out.println ();
			System.out.println ("***** Static unmarshal from plain line with names and quotes using unmarshal_from_line_poly *****");
			System.out.println ();

			SphRegion circle_11 = lambda_from_line (SphRegion::unmarshal_from_line_poly, s_11, true);

			System.out.println (circle_11.toString());


			// --- Set up for array/list tests

			// Mkae another circle

			System.out.println ();
			System.out.println ("***** Make another circle *****");
			System.out.println ();

			SphRegion another_circle = SphRegion.makeCircle (new SphLatLon (35.0, -125.0), 20.0);

			System.out.println (another_circle.toString());

			// Mkae a rectangle

			System.out.println ();
			System.out.println ("***** Make a rectangle *****");
			System.out.println ();

			SphRegion rectangle = SphRegion.makeMercRectangle (39.0, 43.0, -115.0, -110.0);

			System.out.println (rectangle.toString());

			// Mkae a polygon

			System.out.println ();
			System.out.println ("***** Make a polygon *****");
			System.out.println ();

			List<SphLatLon> vertex_list = new ArrayList<SphLatLon>();
			vertex_list.add (new SphLatLon (45.0, -100.0));
			vertex_list.add (new SphLatLon (45.0, -90.0));
			vertex_list.add (new SphLatLon (47.0, -95.0));

			SphRegion polygon = SphRegion.makeMercPolygon (vertex_list);

			System.out.println (polygon.toString());

			// Make an array of 4 regions

			System.out.println ();
			System.out.println ("***** Make an array of 4 regions *****");
			System.out.println ();

			SphRegion[] region_array = new SphRegion[4];
			region_array[0] = circle;
			region_array[1] = another_circle;
			region_array[2] = rectangle;
			region_array[3] = polygon;

			System.out.println ("Array type = " + region_array.getClass().getName());
			System.out.println ("Array length = " + region_array.length);
			System.out.println ();

			for (int j = 0; j < region_array.length; ++j) {
				System.out.println (j + ": " + region_array[j].toString());
			}

			// Make an array of 2 circles

			System.out.println ();
			System.out.println ("***** Make an array of 2 circles *****");
			System.out.println ();

			SphRegionCircle[] circle_array = new SphRegionCircle[2];
			circle_array[0] = (SphRegionCircle)circle;
			circle_array[1] = (SphRegionCircle)another_circle;

			System.out.println ("Array type = " + circle_array.getClass().getName());
			System.out.println ("Array length = " + circle_array.length);
			System.out.println ();

			for (int j = 0; j < circle_array.length; ++j) {
				System.out.println (j + ": " + circle_array[j].toString());
			}

			// Make a list of 4 regions

			List<SphRegion> region_list = new ArrayList<SphRegion>();
			for (int j = 0; j < region_array.length; ++j) {
				region_list.add (region_array[j]);
			}

			// Make a list of 2 circles

			List<SphRegionCircle> circle_list = new ArrayList<SphRegionCircle>();
			for (int j = 0; j < circle_array.length; ++j) {
				circle_list.add (circle_array[j]);
			}


			// --- Array marshal/unmarshal

			// Marshal/unmarshal the array of regions using static method

			{
				System.out.println ();
				System.out.println ("***** Marshal array of 4 regions using marshal_poly *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectArray ("region_array", region_array, SphRegion::marshal_poly);
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal array of 4 regions using unmarshal_poly *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				SphRegion[] new_array = reader.unmarshalObjectArray ("region_array", SphRegion.class, SphRegion::unmarshal_poly);
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("Array type = " + new_array.getClass().getName());
				System.out.println ("Array length = " + new_array.length);
				System.out.println ();

				for (int j = 0; j < new_array.length; ++j) {
					System.out.println (j + ": " + new_array[j].toString());
				}
			}

			// Marshal/unmarshal the array of circles using instance method with supplier

			{
				System.out.println ();
				System.out.println ("***** Marshal array of 2 circles using marshal (mifcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectArray ("circle_array", circle_array, mifcn(SphRegionCircle::marshal));
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal array of 2 circles using unmarshal (uifcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				SphRegionCircle[] new_array = reader.unmarshalObjectArray ("circle_array", SphRegionCircle.class, uifcn(SphRegionCircle::unmarshal, SphRegionCircle::new));
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("Array type = " + new_array.getClass().getName());
				System.out.println ("Array length = " + new_array.length);
				System.out.println ();

				for (int j = 0; j < new_array.length; ++j) {
					System.out.println (j + ": " + new_array[j].toString());
				}
			}

			// Marshal/unmarshal the array of circles using instance method with class object

			{
				System.out.println ();
				System.out.println ("***** Marshal array of 2 circles using marshal (mifcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectArray ("circle_array", circle_array, mifcn(SphRegionCircle::marshal));
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal array of 2 circles using unmarshal (uicfcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				SphRegionCircle[] new_array = reader.unmarshalObjectArray ("circle_array", SphRegionCircle.class, uicfcn(SphRegionCircle::unmarshal, SphRegionCircle.class));
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("Array type = " + new_array.getClass().getName());
				System.out.println ("Array length = " + new_array.length);
				System.out.println ();

				for (int j = 0; j < new_array.length; ++j) {
					System.out.println (j + ": " + new_array[j].toString());
				}
			}

			// Marshal/unmarshal the array of circles using Marshalable

			{
				System.out.println ();
				System.out.println ("***** Marshal array of 2 circles using Marshalable.marshal *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectArray ("circle_array", circle_array);
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal array of 2 circles using Marshalable.unmarshal and class object *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				SphRegionCircle[] new_array = reader.unmarshalObjectArray ("circle_array", SphRegionCircle.class);
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("Array type = " + new_array.getClass().getName());
				System.out.println ("Array length = " + new_array.length);
				System.out.println ();

				for (int j = 0; j < new_array.length; ++j) {
					System.out.println (j + ": " + new_array[j].toString());
				}
			}


			// --- List marshal/unmarshal

			// Marshal/unmarshal the list of regions using static method

			{
				System.out.println ();
				System.out.println ("***** Marshal list of 4 regions using marshal_poly *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectCollection ("region_list", region_list, SphRegion::marshal_poly);
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal list of 4 regions using unmarshal_poly *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				List<SphRegion> new_list = new ArrayList<SphRegion>();
				reader.unmarshalObjectCollection ("region_list", new_list, SphRegion::unmarshal_poly);
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("List length = " + new_list.size());
				System.out.println ();

				for (int j = 0; j < new_list.size(); ++j) {
					System.out.println (j + ": " + new_list.get(j).toString());
				}
			}

			// Marshal/unmarshal the list of circles using instance method with supplier

			{
				System.out.println ();
				System.out.println ("***** Marshal list of 2 circles using marshal (mifcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectCollection ("circle_list", circle_list, mifcn(SphRegionCircle::marshal));
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal list of 2 circles using unmarshal (uifcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				List<SphRegionCircle> new_list = new ArrayList<SphRegionCircle>();
				reader.unmarshalObjectCollection ("circle_list", new_list, uifcn(SphRegionCircle::unmarshal, SphRegionCircle::new));
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("List length = " + new_list.size());
				System.out.println ();

				for (int j = 0; j < new_list.size(); ++j) {
					System.out.println (j + ": " + new_list.get(j).toString());
				}
			}

			// Marshal/unmarshal the list of circles using instance method with class object

			{
				System.out.println ();
				System.out.println ("***** Marshal list of 2 circles using marshal (mifcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectCollection ("circle_list", circle_list, mifcn(SphRegionCircle::marshal));
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal list of 2 circles using unmarshal (uicfcn wrapper) *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				List<SphRegionCircle> new_list = new ArrayList<SphRegionCircle>();
				reader.unmarshalObjectCollection ("circle_list", new_list, uicfcn(SphRegionCircle::unmarshal, SphRegionCircle.class));
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("List length = " + new_list.size());
				System.out.println ();

				for (int j = 0; j < new_list.size(); ++j) {
					System.out.println (j + ": " + new_list.get(j).toString());
				}
			}

			// Marshal/unmarshal the list of circles using Marshalable

			{
				System.out.println ();
				System.out.println ("***** Marshal list of 2 circles using Marshalable.marshal *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				writer.marshalObjectList ("circle_list", circle_list);
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal list of 2 circles using Marshalable.unmarshal and class object *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				List<SphRegionCircle> new_list = reader.unmarshalObjectList ("circle_list", SphRegionCircle.class);
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("List length = " + new_list.size());
				System.out.println ();

				for (int j = 0; j < new_list.size(); ++j) {
					System.out.println (j + ": " + new_list.get(j).toString());
				}
			}


			// --- Individual object marshal/unmarshal

			// Marshal/unmarshal the array of regions using static method

			{
				System.out.println ();
				System.out.println ("***** Marshal array of 4 regions using marshalObject with marshal_poly *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				for (int j = 0; j < region_array.length; ++j) {
					writer.marshalObject ("entry_" + j, region_array[j], SphRegion::marshal_poly);
				}
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal array of 4 regions using unmarshalObject with unmarshal_poly *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				SphRegion[] new_array = new SphRegion[region_array.length];
				for (int j = 0; j < region_array.length; ++j) {
					new_array[j] = reader.unmarshalObject ("entry_" + j, SphRegion::unmarshal_poly);
				}
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("Array type = " + new_array.getClass().getName());
				System.out.println ("Array length = " + new_array.length);
				System.out.println ();

				for (int j = 0; j < new_array.length; ++j) {
					System.out.println (j + ": " + new_array[j].toString());
				}
			}

			// Marshal/unmarshal the array of circles using Marshalable

			{
				System.out.println ();
				System.out.println ("***** Marshal array of 2 circles using marshalObject with Marshalable.marshal *****");
				System.out.println ();

				MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
				writer.marshalMapBegin (null);
				for (int j = 0; j < circle_array.length; ++j) {
					writer.marshalObject ("entry_" + j, circle_array[j]);
				}
				writer.marshalMapEnd ();
				writer.check_write_complete ();

				Object json_container = writer.get_json_container();
				String formatted_string = GeoJsonUtils.jsonObjectToString (json_container, false, true);
				System.out.println (formatted_string);

				System.out.println ();
				System.out.println ("***** Unmarshal array of 2 circles using unmarshalObject with Marshalable.unmarshal and class object *****");
				System.out.println ();

				MarshalImpJsonReader reader = new MarshalImpJsonReader (formatted_string);
				reader.unmarshalMapBegin (null);
				SphRegion[] new_array = new SphRegion[circle_array.length];
				for (int j = 0; j < circle_array.length; ++j) {
					new_array[j] = reader.unmarshalObject ("entry_" + j, SphRegionCircle.class);
				}
				reader.unmarshalMapEnd ();
				reader.check_read_complete ();

				System.out.println ("Array type = " + new_array.getClass().getName());
				System.out.println ("Array length = " + new_array.length);
				System.out.println ();

				for (int j = 0; j < new_array.length; ++j) {
					System.out.println (j + ": " + new_array[j].toString());
				}
			}


			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("MarshalUtils : Unrecognized subcommand : " + args[0]);
		return;

	}




}
