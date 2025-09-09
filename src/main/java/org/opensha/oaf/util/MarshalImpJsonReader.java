package org.opensha.oaf.util;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import java.io.Writer;
import java.io.Reader;
import java.io.IOException;

import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;
import org.json.simple.ItemList;
import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.Yytoken;

import org.apache.commons.math3.distribution.UniformRealDistribution;

/**
 * Class for unmarshaling parameters/data from JSON data structures.
 * Author: Michael Barall 04/06/2018.
 */
public class MarshalImpJsonReader implements MarshalReader {

	//----- Context management -----

	// Class to hold current context.

	private static abstract class Context {

		// The previous context, null if this is the root context.

		protected Context previous;

		// The next context, null if this is the current context.

		protected Context next;

		// check_name - Check a name, and return the named object.

		public abstract Object check_name (String name);

		// peek_name - Check a name, and return the named object, without consuming it.

		public abstract Object peek_name (String name);

		// notify_child_begin - Notification that a child is beginning, and return the child object.

		public abstract Object notify_child_begin (String name, Context child);

		// notify_child_end - Notification that a child is ending.

		public abstract void notify_child_end ();

		// close_map - Close a map context, return the previous context.

		public abstract Context close_map (boolean f_check_keys);

		// close_array - Close an array context, return the previous context.

		public abstract Context close_array ();

		// Constructor.

		public Context (Context previous) {
			this.previous = previous;
			this.next = null;
		}
	}

	// Class to hold map context.

	private static class ContextMap extends Context {

		// The map.

		private JSONOrderedObject json_map;

		// Names currently in use.

		private Set<String> names;

		// Name of current child.

		private String child_name;

		// check_name - Check a name, and return the named object.

		@Override
		public Object check_name (String name) {

			// Find the named object, and throw exception if none

			if (name == null) {
				throw new MarshalException ("No name specified for element in map context");
			}
			if (!( names.add (name) )) {
				throw new MarshalException ("Duplicate element name in map context: name = " + name);
			}
			Object result = json_map.get (name);
			if (result == null) {
				if (!( json_map.containsKey (name) )) {
					throw new MarshalException ("Element not found in map context: name = " + name);
				}
			}

			return result;
		}

		// peek_name - Check a name, and return the named object, without consuming it.

		@Override
		public Object peek_name (String name) {

			// Find the named object, and throw exception if none

			if (name == null) {
				throw new MarshalException ("No name specified for element in map context");
			}
			if (names.contains (name)) {
				throw new MarshalException ("Duplicate element name in map context: name = " + name);
			}
			Object result = json_map.get (name);
			if (result == null) {
				if (!( json_map.containsKey (name) )) {
					throw new MarshalException ("Element not found in map context: name = " + name);
				}
			}

			return result;
		}

		// notify_child_begin - Notification that a child is beginning, and return the child object.

		@Override
		public Object notify_child_begin (String name, Context child) {
			Object result = check_name (name);
			child_name = name;
			next = child;
			return result;
		}

		// notify_child_end - Notification that a child is ending.

		@Override
		public void notify_child_end () {
			child_name = null;
			next = null;
			return;
		}

		// close_map - Close a map context, return the previous context.

		@Override
		public Context close_map (boolean f_check_keys) {
			
			// Check if all names were used

			for (Object o : json_map.keySet()) {
				if (!( o instanceof String )) {
					throw new MarshalException ("Non-string key found in map context");
				}
				if (f_check_keys) {
					String name = (String)o;
					if (!( names.contains(name) )) {
						throw new MarshalException ("Unused element name in map context: name = " + name);
					}
				}
			}

			previous.notify_child_end();
			return previous;
		}

		// close_array - Close an array context, return the previous context.

		@Override
		public Context close_array () {
			throw new MarshalException ("Attempt to end array context when in map context");
		}

		// Constructor.

		public ContextMap (String name, Context previous, Collection<String> keys) {
			super (previous);
			this.names = new HashSet<String>();
			this.child_name = null;
			Object o = this.previous.notify_child_begin (name, this);
			if (o == null) {
				throw new MarshalException ("Found null, expecting map context: name = " + ((name == null) ? "null" : name));
			}
			if (!( o instanceof JSONOrderedObject )) {
				throw new MarshalException ("Wrong element type, expecting map context: name = " + ((name == null) ? "null" : name));
			}
			this.json_map = (JSONOrderedObject)o;
			if (keys != null) {
				for (Object x : this.json_map.keySet()) {
					if (!( x instanceof String )) {
						throw new MarshalException ("Non-string key found in map context");
					}
					keys.add ((String)x);
				}
			}
		}
	}

	// Class to hold array context.

	private static class ContextArray extends Context {

		// The array.

		private JSONArray json_array;

		// The current index.

		private int array_index;

		// get_array_size - Get the array size.

		public int get_array_size () {
			return json_array.size();
		}

		// check_name - Check a name, and return the named object.

		@Override
		public Object check_name (String name) {

			// Increment the index and check for overrun

			if (name != null) {
				throw new MarshalException ("Name specified for element in array context: name = " + name);
			}
			if (array_index == json_array.size()) {
				throw new MarshalException ("Exceeded declared array size in array context: declared size = " + json_array.size());
			}

			return json_array.get(array_index++);
		}

		// peek_name - Check a name, and return the named object, without consuming it.

		@Override
		public Object peek_name (String name) {

			// Increment the index and check for overrun

			if (name != null) {
				throw new MarshalException ("Name specified for element in array context: name = " + name);
			}
			if (array_index == json_array.size()) {
				throw new MarshalException ("Exceeded declared array size in array context: declared size = " + json_array.size());
			}

			return json_array.get(array_index);
		}

		// notify_child_begin - Notification that a child is beginning, and return the child object.

		@Override
		public Object notify_child_begin (String name, Context child) {
			Object result = check_name (name);
			next = child;
			return result;
		}

		// notify_child_end - Notification that a child is ending.

		@Override
		public void notify_child_end () {
			next = null;
			return;
		}

		// close_map - Close a map context, return the previous context.

		@Override
		public Context close_map (boolean f_check_keys) {
			throw new MarshalException ("Attempt to end map context when in array context");
		}

		// close_array - Close an array context, return the previous context.

		@Override
		public Context close_array () {
			if (array_index != json_array.size()) {
				throw new MarshalException ("Array size mismatch in array context: declared size = " + json_array.size() + ", actual size = " + array_index);
			}
			previous.notify_child_end();
			return previous;
		}

		// Constructor.

		public ContextArray (String name, Context previous) {
			super (previous);
			this.array_index = 0;
			Object o = this.previous.notify_child_begin (name, this);
			if (o == null) {
				throw new MarshalException ("Found null, expecting array context: name = " + ((name == null) ? "null" : name));
			}
			if (!( o instanceof JSONArray )) {
				throw new MarshalException ("Wrong element type, expecting array context: name = " + ((name == null) ? "null" : name));
			}
			this.json_array = (JSONArray)o;
		}
	}

	// Class to hold root context.

	private static class ContextRoot extends Context {

		// The JSON container, can be either JSONOrderedObject or JSONArray, or null.

		private Object json_container;

		// True if a child of the root has been created.

		private boolean f_root_done;

		// Return true if a complete child has been processed, false if nothing processed, exception if in progress.

		public boolean get_root_status () {
			if (next != null) {
				throw new MarshalException ("Unmarshal is incomplete");
			}
			return f_root_done;
		}

		// check_name - Check a name, and return the named object.

		@Override
		public Object check_name (String name) {

			// Throw exception

			if (name == null) {
				throw new MarshalException ("Attempt to read element in root context: name = null");
			}
			throw new MarshalException ("Attempt to read element in root context: name = " + name);
		}

		// peek_name - Check a name, and return the named object, without consuming it.

		@Override
		public Object peek_name (String name) {

			// Throw exception

			if (name == null) {
				throw new MarshalException ("Attempt to peek element in root context: name = null");
			}
			throw new MarshalException ("Attempt to peek element in root context: name = " + name);
		}

		// notify_child_begin - Notification that a child is beginning, and return the child object.

		@Override
		public Object notify_child_begin (String name, Context child) {
			if (next != null) {
				if (name == null) {
					throw new MarshalException ("Attempt to begin second child context when in root context: name = null");
				}
				throw new MarshalException ("Attempt to begin second child context when in root context: name = " + name);
			}
			if (f_root_done) {
				if (name == null) {
					throw new MarshalException ("Attempt to begin child context when in already-used root context: name = null");
				}
				throw new MarshalException ("Attempt to begin child context when in already-used root context: name = " + name);
			}
			if (name != null) {
				throw new MarshalException ("Attempt to add named child context when in root context: name = " + name);
			}
			if (json_container == null) {
				throw new MarshalException ("Attempt to begin child context when in empty root context");
			}
			next = child;
			return json_container;
		}

		// notify_child_end - Notification that a child is ending.

		@Override
		public void notify_child_end () {
			if (next == null) {
				throw new MarshalException ("Attempt to end non-existent child context in root context");
			}
			next = null;
			f_root_done = true;
			return;
		}

		// close_map - Close a map context, return the previous context.

		@Override
		public Context close_map (boolean f_check_keys) {
			throw new MarshalException ("Attempt to end map context when in root context");
		}

		// close_array - Close an array context, return the previous context.

		@Override
		public Context close_array () {
			throw new MarshalException ("Attempt to end array context when in root context");
		}

		// Constructor.

		public ContextRoot (Object json_container) {
			super (null);
			if (json_container != null) {
				if (!( json_container instanceof JSONOrderedObject || json_container instanceof JSONArray )) {
					throw new MarshalException ("Supplied JSON container is of unrecognized type");
				}
			}
			this.json_container = json_container;
			f_root_done = false;
		}
	}

	// Root and current context for reading.

	private ContextRoot root_context_read;
	private Context current_context_read;

	//----- Implementation of MarshalReader -----

	/**
	 * Begin a map context.
	 */
	@Override
	public void unmarshalMapBegin (String name) {
		current_context_read = new ContextMap (name, current_context_read, null);
		return;
	}

	/**
	 * End a map context.
	 */
	@Override
	public void unmarshalMapEnd () {
		current_context_read = current_context_read.close_map (true);
		return;
	}

	/**
	 * Begin an array context, return the array size.
	 */
	@Override
	public int unmarshalArrayBegin (String name) {
		ContextArray context_array = new ContextArray (name, current_context_read);
		current_context_read = context_array;
		return context_array.get_array_size();
	}

	/**
	 * End an array context.
	 */
	@Override
	public void unmarshalArrayEnd () {
		current_context_read = current_context_read.close_array();
		return;
	}

	/**
	 * Unmarshal a long.
	 */
	@Override
	public long unmarshalLong (String name) {
		Object o = current_context_read.check_name (name);
		if (o == null) {
			throw new MarshalException ("Unmarshal long found null data: name = " + ((name == null) ? "null" : name));		
		}
		if (!( o instanceof Number )) {
			throw new MarshalException ("Unmarshal long found non-numeric data type: name = " + ((name == null) ? "null" : name));
		}
		if (o instanceof Double || o instanceof Float) {
			throw new MarshalException ("Unmarshal long found floating-point data type: name = " + ((name == null) ? "null" : name));
		}
		return ((Number)o).longValue();
	}

	/**
	 * Unmarshal a double.
	 */
	@Override
	public double unmarshalDouble (String name) {
		Object o = current_context_read.check_name (name);
		if (o == null) {
			throw new MarshalException ("Unmarshal double found null data: name = " + ((name == null) ? "null" : name));		
		}
		if (!( o instanceof Number )) {
			throw new MarshalException ("Unmarshal double found non-numeric data type: name = " + ((name == null) ? "null" : name));
		}
		return ((Number)o).doubleValue();
	}

	/**
	 * Unmarshal a string.  (Null strings are not allowed.)
	 */
	@Override
	public String unmarshalString (String name) {
		Object o = current_context_read.check_name (name);
		if (o == null) {
			throw new MarshalException ("Unmarshal string found null data: name = " + ((name == null) ? "null" : name));		
		}
		if (!( o instanceof String )) {
			throw new MarshalException ("Unmarshal string found non-string data type: name = " + ((name == null) ? "null" : name));
		}
		return ((String)o);
	}

	/**
	 * Unmarshal a boolean.
	 */
	@Override
	public boolean unmarshalBoolean (String name) {
		Object o = current_context_read.check_name (name);
		if (o == null) {
			throw new MarshalException ("Unmarshal boolean found null data: name = " + ((name == null) ? "null" : name));		
		}
		if (!( o instanceof Boolean )) {
			throw new MarshalException ("Unmarshal boolean found non-boolean data type: name = " + ((name == null) ? "null" : name));
		}
		return ((Boolean)o).booleanValue();
	}

	/**
	 * Unmarshal a float.
	 */
	@Override
	public float unmarshalFloat (String name) {
		Object o = current_context_read.check_name (name);
		if (o == null) {
			throw new MarshalException ("Unmarshal float found null data: name = " + ((name == null) ? "null" : name));		
		}
		if (!( o instanceof Number )) {
			throw new MarshalException ("Unmarshal float found non-numeric data type: name = " + ((name == null) ? "null" : name));
		}
		return ((Number)o).floatValue();
	}

	/**
	 * Unmarshal a JSON string.  (Null strings are not allowed.)
	 * The string must contain a JSON object or array, or be an empty string.
	 * For JSON storage, the string is merged into the JSON instead of being
	 * embedded as string-valued data.  (An empty string becomes a JSON null.)
	 * The unmarshaled string may differ from the marshaled string due to JSON parsing.
	 * (Named element ordering, numeric formats, and spacing may be changed).
	 */
	@Override
	public String unmarshalJsonString (String name) {
		Object o = current_context_read.check_name (name);
		String result;
		if (o == null) {
			result = "";
		}
		else {
			if (!( o instanceof JSONArray || o instanceof JSONOrderedObject )) {
				throw new MarshalException ("Unmarshal JSON string did not find a JSON object or JSON array: name = " + ((name == null) ? "null" : name));
			}
			try {
				result = JSONValue.toJSONString(o);
			}
			catch (Exception e) {
				throw new MarshalException ("Unmarshal JSON string encountered an exception while constructing string: name = " + ((name == null) ? "null" : name), e);
			}
		}
		return result;
	}

	//----- Extended JSON support -----

	// Begin a map context.
	// If keys is non-null, then all the JSON keys are added to the collection.

	@Override
	public void  unmarshalJsonMapBegin (String name, Collection<String> keys) {
		current_context_read = new ContextMap (name, current_context_read, keys);
		return;
	}

	// End a map context.
	// If f_check_keys is true, then throw an exception if any keys were not used.
	// Note that f_check_keys = true gives the same behavior as unmarshalMapEnd.

	@Override
	public void unmarshalJsonMapEnd (boolean f_check_keys) {
		current_context_read = current_context_read.close_map (f_check_keys);
		return;
	}

	// Unmarshal a JSON null value.

	@Override
	public void unmarshalJsonNull (String name) {
		Object o = current_context_read.check_name (name);
		if (o != null) {
			throw new MarshalException ("Unmarshal null found non-null data: name = " + ((name == null) ? "null" : name));		
		}
		return;
	}

	// Get the type of the next object to be read.
	// This function does not consume the next object.

	@Override
	public int unmarshalJsonPeekType (String name) {
		Object o = current_context_read.peek_name (name);
		int jpt = -1;

		if (o == null) {
			jpt = JPT_NULL;
		}
		else if (o instanceof Number) {
			if (o instanceof Double) {
				jpt =  JPT_DOUBLE;
			}
			else if (o instanceof Float) {
				jpt = JPT_FLOAT;
			}
			else {
				long x = ((Number)o).longValue();
				if (x < (long)Integer.MIN_VALUE || x > (long)Integer.MAX_VALUE) {
					jpt = JPT_LONG;
				}
				else {
					jpt = JPT_INTEGER;
				}
			}
		}
		else if (o instanceof Boolean) {
			jpt = JPT_BOOLEAN;
		}
		else if (o instanceof String) {
			jpt = JPT_STRING;
		}
		else if (o instanceof JSONOrderedObject) {
			jpt = JPT_MAP;
		}
		else if (o instanceof JSONArray) {
			jpt = JPT_ARRAY;
		}
		else {
			throw new MarshalException ("unmarshalJsonPeekType found element of unknown type");
		}

		return jpt;
	}

	//----- Construction -----

	/**
	 * Create an object that reads from the source, which can be one of:
	 *  null, JSONOrderedObject, JSONArray, String, or java.io.Reader.
	 */
	public MarshalImpJsonReader (Object json_source) {
		Object json_container;

		if (json_source == null) {
			json_container = null;
		}
		else if (json_source instanceof String) {
			try {
				json_container = JSONOrderedObject.parseWithException ((String)json_source);
			}
			catch (ParseException e) {
				throw new MarshalException ("Parsing error while parsing JSON string", e);
			}
			catch (Exception e) {
				throw new MarshalException ("Exception while parsing JSON string", e);
			}
		}
		else if (json_source instanceof Reader) {
			try {
				json_container = JSONOrderedObject.parseWithException ((Reader)json_source);
			}
			catch (ParseException e) {
				throw new MarshalException ("Parsing error while parsing JSON file", e);
			}
			catch (IOException e) {
				throw new MarshalException ("I/O error while parsing JSON file", e);
			}
			catch (Exception e) {
				throw new MarshalException ("Exception while parsing JSON file", e);
			}
		}
		else {
			json_container = json_source;
		}

		root_context_read = new ContextRoot (json_container);
		current_context_read = root_context_read;
	}

	//----- Control -----

	/**
	 * Check read status, return true if read complete, false if nothing read, exception if in progress.
	 */
	public boolean check_read_complete () {
		return root_context_read.get_root_status();
	}

	// Check read completion status.
	// Throw exception if the current top-level object is incomplete.
	// Returns the number of top level object read (which can be 0L if
	// nothing has been read), or -1L if the number is unknown.
	// Note that some readers are limited to a single top-level object.
	// If f_require_eof is true, throw exception if the data source is not
	// fully consumed.  Note that not all readers can perform this check.
	// This function should be called when finished using the reader.

	@Override
	public long read_completion_check (boolean f_require_eof) {
		return (check_read_complete() ? 1L : 0L);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MarshalImpJsonReader : Missing subcommand");
			return;
		}




		// Unrecognized subcommand.

		System.err.println ("MarshalImpJsonReader : Unrecognized subcommand : " + args[0]);
		return;

	}




}
