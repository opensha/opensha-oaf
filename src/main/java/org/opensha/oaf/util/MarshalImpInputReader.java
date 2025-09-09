package org.opensha.oaf.util;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.math3.distribution.UniformRealDistribution;


// Class for unmarshaling objects from an input stream.
// Author: Michael Barall.
//
// This reader can unmarshal multiple top-level objects from the source.
//
// If the source is Closeable, then closing the reader also closes the source.

public class MarshalImpInputReader implements MarshalReader, Closeable {

	//----- Data storage -----

	// The data source.

	private MarshalInput data_in;

	// Flag, true if field names are stored.

	private boolean f_store_names;

	//----- Context management -----

	// Class to hold current context.

	private static abstract class Context {

		// The previous context, null if this is the root context.

		protected Context previous;

		// The next context, null if this is the current context.

		protected Context next;

		// check_name - Check a name.

		public abstract void check_name (String name);

		// notify_child_begin - Notification that a child is beginning.

		public abstract void notify_child_begin (String name, Context child);

		// notify_child_end - Notification that a child is ending.

		public abstract void notify_child_end ();

		// close_map - Close a map context, return the previous context.

		public abstract Context close_map ();

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

		// Names currently in use.

		private Set<String> names;

		// Name of current child.

		private String child_name;

		// check_name - Check a name.

		@Override
		public void check_name (String name) {

			// Add the name, and throw exception if already in use

			if (name == null) {
				throw new MarshalException ("No name specified for element in map context");
			}
			if (!( names.add (name) )) {
				throw new MarshalException ("Duplicate element name in map context: name = " + name);
			}

			return;
		}

		// notify_child_begin - Notification that a child is beginning.

		@Override
		public void notify_child_begin (String name, Context child) {
			check_name (name);
			child_name = name;
			next = child;
			return;
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
		public Context close_map () {
			previous.notify_child_end();
			return previous;
		}

		// close_array - Close an array context, return the previous context.

		@Override
		public Context close_array () {
			throw new MarshalException ("Attempt to end array context when in map context");
		}

		// Constructor.

		public ContextMap (String name, Context previous) {
			super (previous);
			this.names = new HashSet<String>();
			this.child_name = null;
			this.previous.notify_child_begin (name, this);
		}
	}

	// Class to hold array context.

	private static class ContextArray extends Context {

		// The array size.

		private int array_size;

		// The current index.

		private int array_index;

		// check_name - Check a name.

		@Override
		public void check_name (String name) {

			// Increment the index and check for overrun

			if (name != null) {
				throw new MarshalException ("Name specified for element in array context: name = " + name);
			}
			if (array_index == array_size) {
				throw new MarshalException ("Exceeded declared array size in array context: declared size = " + array_size);
			}
			++array_index;

			return;
		}

		// notify_child_begin - Notification that a child is beginning.

		@Override
		public void notify_child_begin (String name, Context child) {
			check_name (name);
			next = child;
			return;
		}

		// notify_child_end - Notification that a child is ending.

		@Override
		public void notify_child_end () {
			next = null;
			return;
		}

		// close_map - Close a map context, return the previous context.

		@Override
		public Context close_map () {
			throw new MarshalException ("Attempt to end map context when in array context");
		}

		// close_array - Close an array context, return the previous context.

		@Override
		public Context close_array () {
			if (array_index != array_size) {
				throw new MarshalException ("Array size mismatch in array context: declared size = " + array_size + ", actual size = " + array_index);
			}
			previous.notify_child_end();
			return previous;
		}

		// Constructor, specifies array size.

		public ContextArray (String name, Context previous, int array_size) {
			super (previous);
			if (array_size < 0) {
				throw new MarshalException ("Negative array size in array context: size = " + array_size);
			}
			this.array_size = array_size;
			this.array_index = 0;
			this.previous.notify_child_begin (name, this);
		}
	}

	// Class to hold root context.

	private static class ContextRoot extends Context {

		// Number of top-level objects created.

		private long root_count;

		// Return number of complete children that have been processed, exception if in progress.

		public long get_root_status () {
			if (next != null) {
				throw new MarshalException ("Marshal/unmarshal is incomplete");
			}
			return root_count;
		}

		// check_name - Check a name.

		@Override
		public void check_name (String name) {

			// Throw exception

			if (name == null) {
				throw new MarshalException ("Attempt to add element in root context: name = null");
			}
			throw new MarshalException ("Attempt to add element in root context: name = " + name);
		}

		// notify_child_begin - Notification that a child is beginning.

		@Override
		public void notify_child_begin (String name, Context child) {
			if (next != null) {
				if (name == null) {
					throw new MarshalException ("Attempt to begin second child context when in root context: name = null");
				}
				throw new MarshalException ("Attempt to begin second child context when in root context: name = " + name);
			}
			//  if (root_count > 0L) {		// This would limit to one top-level object
			//  	if (name == null) {
			//  		throw new MarshalException ("Attempt to begin child context when in already-used root context: name = null");
			//  	}
			//  	throw new MarshalException ("Attempt to begin child context when in already-used root context: name = " + name);
			//  }
			if (name != null) {
				throw new MarshalException ("Attempt to add named child context when in root context: name = " + name);
			}
			next = child;
			return;
		}

		// notify_child_end - Notification that a child is ending.

		@Override
		public void notify_child_end () {
			if (next == null) {
				throw new MarshalException ("Attempt to end non-existent child context in root context");
			}
			next = null;
			++root_count;
			return;
		}

		// close_map - Close a map context, return the previous context.

		@Override
		public Context close_map () {
			throw new MarshalException ("Attempt to end map context when in root context");
		}

		// close_array - Close an array context, return the previous context.

		@Override
		public Context close_array () {
			throw new MarshalException ("Attempt to end array context when in root context");
		}

		// Constructor.

		public ContextRoot () {
			super (null);
			root_count = 0L;
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
		current_context_read = new ContextMap (name, current_context_read);
		return;
	}

	/**
	 * End a map context.
	 */
	@Override
	public void unmarshalMapEnd () {
		current_context_read = current_context_read.close_map();
		return;
	}

	/**
	 * Begin an array context, return the array size.
	 */
	@Override
	public int unmarshalArrayBegin (String name) {
		int array_size;
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			array_size = data_in.readInt();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
		current_context_read = new ContextArray (name, current_context_read, array_size);
		return array_size;
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
		current_context_read.check_name (name);
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			return data_in.readLong();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
	}

	/**
	 * Unmarshal a double.
	 */
	@Override
	public double unmarshalDouble (String name) {
		current_context_read.check_name (name);
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			return data_in.readDouble();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
	}

	/**
	 * Unmarshal a string.  (Null strings are not allowed.)
	 */
	@Override
	public String unmarshalString (String name) {
		current_context_read.check_name (name);
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			return data_in.readString();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
	}

	/**
	 * Unmarshal an int.
	 */
	@Override
	public int unmarshalInt (String name) {
		current_context_read.check_name (name);
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			return data_in.readInt();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
	}

	/**
	 * Unmarshal an int, with required minimum value.
	 */
	@Override
	public int unmarshalInt (String name, int minValue) {
		int x = unmarshalInt (name);
		if (x < minValue) {
			throw new MarshalException ("Unmarshaled int out-of-range: value = " + x + ", min = " + minValue + ", max = " + Integer.MAX_VALUE);
		}
		return x;
	}

	/**
	 * Unmarshal an int, with required minimum and maximum values.
	 */
	@Override
	public int unmarshalInt (String name, int minValue, int maxValue) {
		int x = unmarshalInt (name);
		if (x < minValue || x > maxValue) {
			throw new MarshalException ("Unmarshaled int out-of-range: value = " + x + ", min = " + minValue + ", max = " + maxValue);
		}
		return x;
	}

	/**
	 * Unmarshal a boolean.
	 */
	@Override
	public boolean unmarshalBoolean (String name) {
		current_context_read.check_name (name);
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			return data_in.readBoolean();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
	}

	/**
	 * Unmarshal a float.
	 */
	@Override
	public float unmarshalFloat (String name) {
		current_context_read.check_name (name);
		try {
			if (f_store_names && name != null) {
				String w = data_in.readName();
				if (!( name.equals(w) ))
				{
					throw new MarshalException ("Unmarshal field name mismatch: expected = " + name + ", got = " + w);
				}
			}
			return data_in.readFloat();
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpInputReader: I/O exception", e);
		}
	}

	//----- Construction -----

	/**
	 * Create an object that reads from the given source.
	 */
	public MarshalImpInputReader (MarshalInput data_in, boolean f_store_names) {
		this.data_in = data_in;
		this.f_store_names = f_store_names;

		root_context_read = new ContextRoot();
		current_context_read = root_context_read;
	}

	//----- Control -----

	/**
	 * Get the data source.
	 */
	public MarshalInput get_data_in () {
		return data_in;
	}

	/**
	 * Get the field name store flag.
	 */
	public boolean get_f_store_names () {
		return f_store_names;
	}

	/**
	 * Check read status, return number of top-level objects read, exception if in progress.
	 */
	public long check_read_complete () {
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
		return check_read_complete();
	}

	/**
	 * Close the data store, if it is Closeable.
	 */
	@Override
	public void close () throws IOException {
		if (data_in != null) {
			if (data_in instanceof Closeable) {
				((Closeable)data_in).close();
			}
		}
		data_in = null;
		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MarshalImpInputReader : Missing subcommand");
			return;
		}




		// Unrecognized subcommand.

		System.err.println ("MarshalImpInputReader : Unrecognized subcommand : " + args[0]);
		return;

	}




}
