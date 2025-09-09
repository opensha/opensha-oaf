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


// Class for marshaling objects to an output stream.
// Author: Michael Barall.
//
// This writer can marshal multiple top-level objects to the destination.
//
// If the destination is Closeable, then closing the writer also closes the destination.

public class MarshalImpOutputWriter implements MarshalWriter, Closeable {

	//----- Data storage -----

	// The data destination.

	private MarshalOutput data_out;

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

	// Root and current context for writing.

	private ContextRoot root_context_write;
	private Context current_context_write;

	//----- Implementation of MarshalWriter -----

	/**
	 * Begin a map context.
	 */
	@Override
	public void marshalMapBegin (String name) {
		current_context_write = new ContextMap (name, current_context_write);
		return;
	}

	/**
	 * End a map context.
	 */
	@Override
	public void marshalMapEnd () {
		current_context_write = current_context_write.close_map();
		return;
	}

	/**
	 * Begin an array context, specify the array size.
	 */
	@Override
	public void marshalArrayBegin (String name, int array_size) {
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeInt (array_size);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		current_context_write = new ContextArray (name, current_context_write, array_size);
		return;
	}

	/**
	 * End an array context.
	 */
	@Override
	public void marshalArrayEnd () {
		current_context_write = current_context_write.close_array();
		return;
	}

	/**
	 * Marshal a long.
	 */
	@Override
	public void marshalLong (String name, long x) {
		current_context_write.check_name (name);
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeLong (x);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		return;
	}

	/**
	 * Marshal a double.
	 */
	@Override
	public void marshalDouble (String name, double x) {
		current_context_write.check_name (name);
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeDouble (x);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		return;
	}

	/**
	 * Marshal a string.  (Null strings are not allowed.)
	 */
	@Override
	public void marshalString (String name, String x) {
		current_context_write.check_name (name);
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeString (x);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		return;
	}

	/**
	 * Marshal an int.
	 */
	@Override
	public void marshalInt (String name, int x) {
		current_context_write.check_name (name);
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeInt (x);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		return;
	}

	/**
	 * Marshal a boolean.
	 */
	@Override
	public void marshalBoolean (String name, boolean x) {
		current_context_write.check_name (name);
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeBoolean (x);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		return;
	}

	/**
	 * Marshal a float.
	 */
	@Override
	public void marshalFloat (String name, float x) {
		current_context_write.check_name (name);
		try {
			if (f_store_names && name != null) {
				data_out.writeName (name);
			}
			data_out.writeFloat (x);
		} catch (IOException e) {
			throw new MarshalException ("MarshalImpOutputWriter: I/O exception", e);
		}
		return;
	}

	//----- Construction -----

	/**
	 * Create an object that writes to the given destination.
	 */
	public MarshalImpOutputWriter (MarshalOutput data_out, boolean f_store_names) {
		this.data_out = data_out;
		this.f_store_names = f_store_names;

		root_context_write = new ContextRoot();
		current_context_write = root_context_write;
	}

	//----- Control -----

	/**
	 * Get the data destination.
	 */
	public MarshalOutput get_data_out () {
		return data_out;
	}

	/**
	 * Get the field name store flag.
	 */
	public boolean get_f_store_names () {
		return f_store_names;
	}

	/**
	 * Check write status, return number of top-level object written, exception if in progress.
	 */
	public long check_write_complete () {
		return root_context_write.get_root_status();
	}

	// Check write completion status.
	// Throw exception if the current top-level object is incomplete.
	// Returns the number of top level object written (which can be 0L if
	// nothing has been written), or -1L if the number is unknown.
	// Note that some writers are limited to a single top-level object.
	// This function should be called when finished using the writer.

	@Override
	public long write_completion_check () {
		return check_write_complete();
	}

	/**
	 * Close the data store, if it is Closeable.
	 */
	@Override
	public void close () throws IOException {
		if (data_out != null) {
			if (data_out instanceof Closeable) {
				((Closeable)data_out).close();
			}
		}
		data_out = null;
		return;
	}




	//----- Testing -----




	// A string builder used as a data store for testing.

	private static StringBuilder test_sb = null;




	// Make an input source for testing.
	// The following special filenames are supported:
	//  "+"  --  A line with JSON escapes.
	//  "-"  --  A line with JSON escapes.
	// In case of a line source, the first 3000 characters are dumped to the screen.

	private static MarshalInput test_make_input (String filename) throws IOException {
		if (filename.equalsIgnoreCase ("+")) {
			String s = test_sb.toString();
			String display = s;
			if (display.length() > 3000) {
				display = s.substring (0, 3000) + " ...";
			}
			System.out.println();
			System.out.println(display);
			System.out.println();
			return new MarshalInputLine (s);
		}
		if (filename.equalsIgnoreCase ("-")) {
			String s = test_sb.toString();
			String display = s;
			if (display.length() > 3000) {
				display = s.substring (0, 3000) + " ...";
			}
			System.out.println();
			System.out.println(display);
			System.out.println();
			return new MarshalInputLine (s);
		}
		return new MarshalInputBinaryStream (new DataInputStream (new BufferedInputStream (new FileInputStream (filename))));
	}




	// Make an output source for testing.
	// The following special filenames are supported:
	//  "+"  --  A line with JSON escapes with all strings quoted.
	//  "-"  --  A line with JSON escapes with strings quoted only as needed.

	private static MarshalOutput test_make_output (String filename) throws IOException {
		if (filename.equalsIgnoreCase ("+")) {
			test_sb = new StringBuilder();
			return new MarshalOutputLine (test_sb, false, true);
		}
		if (filename.equalsIgnoreCase ("-")) {
			test_sb = new StringBuilder();
			return new MarshalOutputLine (test_sb, false, false);
		}
		return new MarshalOutputBinaryStream (new DataOutputStream (new BufferedOutputStream (new FileOutputStream (filename))));
	}




	// Get extra characters to insert in string data.

	private static String string_extra (int i) {
		if (i % 8 == 2) {
			return " ";
		}
		if (i % 8 == 6) {
			return "\u00E6";
		}
		return "";
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MarshalImpOutputWriter : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  filename  num_long  num_double  num_string  num_int
		// Test marshaling and unmarshaling with the given numbers of long and double and string and int.
		// Data values are randomly generated.
		// Filename can be "+" for a line with all strings quoted, "-" for a line with strings quoted only as needed.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Five additional arguments

			if (args.length != 6) {
				System.err.println ("MarshalImpOutputWriter : Invalid 'test1' subcommand");
				return;
			}

			try {

				String filename = args[1];
				int num_long = Integer.parseInt(args[2]);
				int num_double = Integer.parseInt(args[3]);
				int num_string = Integer.parseInt(args[4]);
				int num_int = Integer.parseInt(args[5]);

				System.out.println (
					"filename = " + filename + "\n" +
					"num_long = " + num_long + "\n" +
					"num_double = " + num_double + "\n" +
					"num_string = " + num_string + "\n" +
					"num_int = " + num_int + "\n"
				);

				// Random number generator

				UniformRealDistribution rangen = SimpleUtils.make_uniform_rangen();

				// Generate random values

				System.out.println ("Generating random data ...");

				long[] long_data = new long[num_long];
				for (int i = 0; i < num_long; ++i) {
					long_data[i] = Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("long_data[" + i + "] = " + long_data[i]);
					}
				}

				double[] double_data = new double[num_double];
				for (int i = 0; i < num_double; ++i) {
					switch (i % 5) {
					case 0: double_data[i] = rangen.sample() * 1.0e12; break;
					case 1: double_data[i] = rangen.sample() * 2.0e7; break;
					case 2: double_data[i] = rangen.sample() * 3.0e0; break;
					case 3: double_data[i] = rangen.sample() * 2.0e-3; break;
					case 4: double_data[i] = rangen.sample() * 1.0e-12; break;
					}
					if (i < 10) {
						System.out.println ("double_data[" + i + "] = " + double_data[i]);
					}
				}

				String[] string_data = new String[num_string];
				for (int i = 0; i < num_string; ++i) {
					string_data[i] = "String" + string_extra(i) + Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("string_data[" + i + "] = " + string_data[i]);
					}
				}

				int[] int_data = new int[num_int];
				for (int i = 0; i < num_int; ++i) {
					int_data[i] = (int)(Math.round (rangen.sample() * 1.0e8));
					if (i < 10) {
						System.out.println ("int_data[" + i + "] = " + int_data[i]);
					}
				}

				// Marshal the data

				System.out.println ("Marshaling data ...");

				MarshalImpOutputWriter writer = new MarshalImpOutputWriter (
					test_make_output (filename), true);

				writer.marshalArrayBegin (null, num_long + num_double + num_string + num_int + 2);

				writer.marshalBoolean (null, true);
				writer.marshalBoolean (null, false);

				for (int i = 0; i < num_long; ++i) {
					writer.marshalLong (null, long_data[i]);
				}

				for (int i = 0; i < num_double; ++i) {
					writer.marshalDouble (null, double_data[i]);
				}

				for (int i = 0; i < num_string; ++i) {
					writer.marshalString (null, string_data[i]);
				}

				for (int i = 0; i < num_int; ++i) {
					writer.marshalInt (null, int_data[i]);
				}

				writer.marshalArrayEnd ();

				if (!( writer.check_write_complete() == 1L )) {
					System.out.println ("Writer reports writing not complete");
					return;
				}

				writer.close();
				writer = null;

				// Unmarshal and check the data

				System.out.println ("Unmarshaling data ...");

				MarshalImpInputReader reader = new MarshalImpInputReader (
					test_make_input (filename), true);

				int array_size = reader.unmarshalArrayBegin (null);

				if (array_size != num_long + num_double + num_string + num_int + 2) {
					System.out.println ("Reader reports incorrect array size: " + array_size);
					return;
				}

				int errors = 0;

				if (!( reader.unmarshalBoolean (null) )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				if ( reader.unmarshalBoolean (null) ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				for (int i = 0; i < num_long; ++i) {
					long x = reader.unmarshalLong (null);
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_double; ++i) {
					double x = reader.unmarshalDouble (null);
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_string; ++i) {
					String x = reader.unmarshalString (null);
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_int; ++i) {
					int x = reader.unmarshalInt (null);
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalArrayEnd ();

				if (!( reader.check_read_complete() == 1L )) {
					System.out.println ("Reader reports reading not complete");
					return;
				}

				reader.close();
				reader = null;

				System.out.println ("Error count: " + errors);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename  num_long  num_double  num_string  num_int
		// Test marshaling and unmarshaling with the given numbers of long and double and string and int.
		// Data values are randomly generated.
		// Filename can be "+" for a line with all strings quoted, "-" for a line with strings quoted only as needed.
		// This is the same as test #1, except data values are written to a map instead of an array.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Five additional arguments

			if (args.length != 6) {
				System.err.println ("MarshalImpOutputWriter : Invalid 'test2' subcommand");
				return;
			}

			try {

				String filename = args[1];
				int num_long = Integer.parseInt(args[2]);
				int num_double = Integer.parseInt(args[3]);
				int num_string = Integer.parseInt(args[4]);
				int num_int = Integer.parseInt(args[5]);

				System.out.println (
					"filename = " + filename + "\n" +
					"num_long = " + num_long + "\n" +
					"num_double = " + num_double + "\n" +
					"num_string = " + num_string + "\n" +
					"num_int = " + num_int + "\n"
				);

				// Random number generator

				UniformRealDistribution rangen = SimpleUtils.make_uniform_rangen();

				// Generate random values

				System.out.println ("Generating random data ...");

				long[] long_data = new long[num_long];
				for (int i = 0; i < num_long; ++i) {
					long_data[i] = Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("long_data[" + i + "] = " + long_data[i]);
					}
				}

				double[] double_data = new double[num_double];
				for (int i = 0; i < num_double; ++i) {
					switch (i % 5) {
					case 0: double_data[i] = rangen.sample() * 1.0e12; break;
					case 1: double_data[i] = rangen.sample() * 2.0e7; break;
					case 2: double_data[i] = rangen.sample() * 3.0e0; break;
					case 3: double_data[i] = rangen.sample() * 2.0e-3; break;
					case 4: double_data[i] = rangen.sample() * 1.0e-12; break;
					}
					if (i < 10) {
						System.out.println ("double_data[" + i + "] = " + double_data[i]);
					}
				}

				String[] string_data = new String[num_string];
				for (int i = 0; i < num_string; ++i) {
					string_data[i] = "String" + string_extra(i) + Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("string_data[" + i + "] = " + string_data[i]);
					}
				}

				int[] int_data = new int[num_int];
				for (int i = 0; i < num_int; ++i) {
					int_data[i] = (int)(Math.round (rangen.sample() * 1.0e8));
					if (i < 10) {
						System.out.println ("int_data[" + i + "] = " + int_data[i]);
					}
				}

				// Marshal the data

				System.out.println ("Marshaling data ...");

				MarshalImpOutputWriter writer = new MarshalImpOutputWriter (
					test_make_output (filename), true);

				writer.marshalMapBegin (null);

				writer.marshalBoolean ("boolean_data[true]", true);
				writer.marshalBoolean ("boolean_data[false]", false);

				for (int i = 0; i < num_long; ++i) {
					writer.marshalLong ("long_data[" + i + "]", long_data[i]);
				}

				for (int i = 0; i < num_double; ++i) {
					writer.marshalDouble ("double_data[" + i + "]", double_data[i]);
				}

				for (int i = 0; i < num_string; ++i) {
					writer.marshalString ("string_data[" + i + "]", string_data[i]);
				}

				for (int i = 0; i < num_int; ++i) {
					writer.marshalInt ("int_data[" + i + "]", int_data[i]);
				}

				writer.marshalMapEnd ();

				if (!( writer.check_write_complete() == 1L )) {
					System.out.println ("Writer reports writing not complete");
					return;
				}

				writer.close();
				writer = null;

				// Unmarshal and check the data

				System.out.println ("Unmarshaling data ...");

				MarshalImpInputReader reader = new MarshalImpInputReader (
					test_make_input (filename), true);

				reader.unmarshalMapBegin (null);

				int errors = 0;

				if (!( reader.unmarshalBoolean ("boolean_data[true]") )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				if ( reader.unmarshalBoolean ("boolean_data[false]") ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				for (int i = 0; i < num_long; ++i) {
					long x = reader.unmarshalLong ("long_data[" + i + "]");
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_double; ++i) {
					double x = reader.unmarshalDouble ("double_data[" + i + "]");
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_string; ++i) {
					String x = reader.unmarshalString ("string_data[" + i + "]");
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_int; ++i) {
					int x = reader.unmarshalInt ("int_data[" + i + "]");
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();

				if (!( reader.check_read_complete() == 1L )) {
					System.out.println ("Reader reports reading not complete");
					return;
				}

				reader.close();
				reader = null;

				System.out.println ("Error count: " + errors);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename  num_long  num_double  num_string  num_int
		// Test marshaling and unmarshaling with the given numbers of long and double and string and int.
		// Data values are randomly generated.
		// Filename can be "+" for a line with all strings quoted, "-" for a line with strings quoted only as needed.
		// This is the same as test #2, except field names are not stored in the file.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Five additional arguments

			if (args.length != 6) {
				System.err.println ("MarshalImpOutputWriter : Invalid 'test3' subcommand");
				return;
			}

			try {

				String filename = args[1];
				int num_long = Integer.parseInt(args[2]);
				int num_double = Integer.parseInt(args[3]);
				int num_string = Integer.parseInt(args[4]);
				int num_int = Integer.parseInt(args[5]);

				System.out.println (
					"filename = " + filename + "\n" +
					"num_long = " + num_long + "\n" +
					"num_double = " + num_double + "\n" +
					"num_string = " + num_string + "\n" +
					"num_int = " + num_int + "\n"
				);

				// Random number generator

				UniformRealDistribution rangen = SimpleUtils.make_uniform_rangen();

				// Generate random values

				System.out.println ("Generating random data ...");

				long[] long_data = new long[num_long];
				for (int i = 0; i < num_long; ++i) {
					long_data[i] = Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("long_data[" + i + "] = " + long_data[i]);
					}
				}

				double[] double_data = new double[num_double];
				for (int i = 0; i < num_double; ++i) {
					switch (i % 5) {
					case 0: double_data[i] = rangen.sample() * 1.0e12; break;
					case 1: double_data[i] = rangen.sample() * 2.0e7; break;
					case 2: double_data[i] = rangen.sample() * 3.0e0; break;
					case 3: double_data[i] = rangen.sample() * 2.0e-3; break;
					case 4: double_data[i] = rangen.sample() * 1.0e-12; break;
					}
					if (i < 10) {
						System.out.println ("double_data[" + i + "] = " + double_data[i]);
					}
				}

				String[] string_data = new String[num_string];
				for (int i = 0; i < num_string; ++i) {
					string_data[i] = "String" + string_extra(i) + Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("string_data[" + i + "] = " + string_data[i]);
					}
				}

				int[] int_data = new int[num_int];
				for (int i = 0; i < num_int; ++i) {
					int_data[i] = (int)(Math.round (rangen.sample() * 1.0e8));
					if (i < 10) {
						System.out.println ("int_data[" + i + "] = " + int_data[i]);
					}
				}

				// Marshal the data

				System.out.println ("Marshaling data ...");

				MarshalImpOutputWriter writer = new MarshalImpOutputWriter (
					test_make_output (filename), false);

				writer.marshalMapBegin (null);

				writer.marshalBoolean ("boolean_data[true]", true);
				writer.marshalBoolean ("boolean_data[false]", false);

				for (int i = 0; i < num_long; ++i) {
					writer.marshalLong ("long_data[" + i + "]", long_data[i]);
				}

				for (int i = 0; i < num_double; ++i) {
					writer.marshalDouble ("double_data[" + i + "]", double_data[i]);
				}

				for (int i = 0; i < num_string; ++i) {
					writer.marshalString ("string_data[" + i + "]", string_data[i]);
				}

				for (int i = 0; i < num_int; ++i) {
					writer.marshalInt ("int_data[" + i + "]", int_data[i]);
				}

				writer.marshalMapEnd ();

				if (!( writer.check_write_complete() == 1L )) {
					System.out.println ("Writer reports writing not complete");
					return;
				}

				writer.close();
				writer = null;

				// Unmarshal and check the data

				System.out.println ("Unmarshaling data ...");

				MarshalImpInputReader reader = new MarshalImpInputReader (
					test_make_input (filename), false);

				reader.unmarshalMapBegin (null);

				int errors = 0;

				if (!( reader.unmarshalBoolean ("boolean_data[true]") )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				if ( reader.unmarshalBoolean ("boolean_data[false]") ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				for (int i = 0; i < num_long; ++i) {
					long x = reader.unmarshalLong ("long_data[" + i + "]");
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_double; ++i) {
					double x = reader.unmarshalDouble ("double_data[" + i + "]");
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_string; ++i) {
					String x = reader.unmarshalString ("string_data[" + i + "]");
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				for (int i = 0; i < num_int; ++i) {
					int x = reader.unmarshalInt ("int_data[" + i + "]");
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();

				if (!( reader.check_read_complete() == 1L )) {
					System.out.println ("Reader reports reading not complete");
					return;
				}

				reader.close();
				reader = null;

				System.out.println ("Error count: " + errors);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  filename  num_long  num_double  num_string  num_int
		// Test marshaling and unmarshaling with the given numbers of long and double and string and int.
		// Data values are randomly generated.
		// Filename can be "+" for a line with all strings quoted, "-" for a line with strings quoted only as needed.
		// This is the same as test #2, except data values are written to multiple maps, and all data is written twice.
		// Also uses the close_writer and close_reader methods.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Five additional arguments

			if (args.length != 6) {
				System.err.println ("MarshalImpOutputWriter : Invalid 'test4' subcommand");
				return;
			}

			try {

				String filename = args[1];
				int num_long = Integer.parseInt(args[2]);
				int num_double = Integer.parseInt(args[3]);
				int num_string = Integer.parseInt(args[4]);
				int num_int = Integer.parseInt(args[5]);

				System.out.println (
					"filename = " + filename + "\n" +
					"num_long = " + num_long + "\n" +
					"num_double = " + num_double + "\n" +
					"num_string = " + num_string + "\n" +
					"num_int = " + num_int + "\n"
				);

				// Random number generator

				UniformRealDistribution rangen = SimpleUtils.make_uniform_rangen();

				// Generate random values

				System.out.println ("Generating random data ...");

				long[] long_data = new long[num_long];
				for (int i = 0; i < num_long; ++i) {
					long_data[i] = Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("long_data[" + i + "] = " + long_data[i]);
					}
				}

				double[] double_data = new double[num_double];
				for (int i = 0; i < num_double; ++i) {
					switch (i % 5) {
					case 0: double_data[i] = rangen.sample() * 1.0e12; break;
					case 1: double_data[i] = rangen.sample() * 2.0e7; break;
					case 2: double_data[i] = rangen.sample() * 3.0e0; break;
					case 3: double_data[i] = rangen.sample() * 2.0e-3; break;
					case 4: double_data[i] = rangen.sample() * 1.0e-12; break;
					}
					if (i < 10) {
						System.out.println ("double_data[" + i + "] = " + double_data[i]);
					}
				}

				String[] string_data = new String[num_string];
				for (int i = 0; i < num_string; ++i) {
					string_data[i] = "String" + string_extra(i) + Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("string_data[" + i + "] = " + string_data[i]);
					}
				}

				int[] int_data = new int[num_int];
				for (int i = 0; i < num_int; ++i) {
					int_data[i] = (int)(Math.round (rangen.sample() * 1.0e8));
					if (i < 10) {
						System.out.println ("int_data[" + i + "] = " + int_data[i]);
					}
				}

				// Marshal the data

				System.out.println ("Marshaling data ...");

				MarshalImpOutputWriter writer = new MarshalImpOutputWriter (test_make_output (filename), true);

				writer.marshalMapBegin (null);

				writer.marshalBoolean ("boolean_data[true]", true);
				writer.marshalBoolean ("boolean_data[false]", false);

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_long; ++i) {
					writer.marshalLong ("long_data[" + i + "]", long_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_double; ++i) {
					writer.marshalDouble ("double_data[" + i + "]", double_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_string; ++i) {
					writer.marshalString ("string_data[" + i + "]", string_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_int; ++i) {
					writer.marshalInt ("int_data[" + i + "]", int_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				writer.marshalBoolean ("boolean_data[false]", false);
				writer.marshalBoolean ("boolean_data[true]", true);

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_long - 1; i >= 0; --i) {
					writer.marshalLong ("long_data[" + i + "]", long_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_double - 1; i >= 0; --i) {
					writer.marshalDouble ("double_data[" + i + "]", double_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_string - 1; i >= 0; --i) {
					writer.marshalString ("string_data[" + i + "]", string_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_int - 1; i >= 0; --i) {
					writer.marshalInt ("int_data[" + i + "]", int_data[i]);
				}

				writer.marshalMapEnd ();

				if (!( writer.check_write_complete() == 10L )) {
					System.out.println ("Writer reports writing not complete");
					return;
				}

				writer.close_writer();
				writer = null;

				// Unmarshal and check the data

				System.out.println ("Unmarshaling data ...");

				MarshalImpInputReader reader = new MarshalImpInputReader (test_make_input (filename), true);

				int errors = 0;

				reader.unmarshalMapBegin (null);

				if (!( reader.unmarshalBoolean ("boolean_data[true]") )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				if ( reader.unmarshalBoolean ("boolean_data[false]") ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_long; ++i) {
					long x = reader.unmarshalLong ("long_data[" + i + "]");
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_double; ++i) {
					double x = reader.unmarshalDouble ("double_data[" + i + "]");
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_string; ++i) {
					String x = reader.unmarshalString ("string_data[" + i + "]");
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_int; ++i) {
					int x = reader.unmarshalInt ("int_data[" + i + "]");
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				if ( reader.unmarshalBoolean ("boolean_data[false]") ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				if (!( reader.unmarshalBoolean ("boolean_data[true]") )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_long - 1; i >= 0; --i) {
					long x = reader.unmarshalLong ("long_data[" + i + "]");
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_double - 1; i >= 0; --i) {
					double x = reader.unmarshalDouble ("double_data[" + i + "]");
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_string - 1; i >= 0; --i) {
					String x = reader.unmarshalString ("string_data[" + i + "]");
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_int - 1; i >= 0; --i) {
					int x = reader.unmarshalInt ("int_data[" + i + "]");
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();

				if (!( reader.check_read_complete() == 10L )) {
					System.out.println ("Reader reports reading not complete");
					return;
				}

				reader.close_reader();
				reader = null;

				System.out.println ("Error count: " + errors);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  filename  num_long  num_double  num_string  num_int
		// Test marshaling and unmarshaling with the given numbers of long and double and string and int.
		// Data values are randomly generated.
		// Filename can be "+" for a line with all strings quoted, "-" for a line with strings quoted only as needed.
		// This is the same as test #4, except field names are not stored in the file.
		// Also uses the close_writer and close_reader methods.

		if (args[0].equalsIgnoreCase ("test5")) {

			// Five additional arguments

			if (args.length != 6) {
				System.err.println ("MarshalImpOutputWriter : Invalid 'test5' subcommand");
				return;
			}

			try {

				String filename = args[1];
				int num_long = Integer.parseInt(args[2]);
				int num_double = Integer.parseInt(args[3]);
				int num_string = Integer.parseInt(args[4]);
				int num_int = Integer.parseInt(args[5]);

				System.out.println (
					"filename = " + filename + "\n" +
					"num_long = " + num_long + "\n" +
					"num_double = " + num_double + "\n" +
					"num_string = " + num_string + "\n" +
					"num_int = " + num_int + "\n"
				);

				// Random number generator

				UniformRealDistribution rangen = SimpleUtils.make_uniform_rangen();

				// Generate random values

				System.out.println ("Generating random data ...");

				long[] long_data = new long[num_long];
				for (int i = 0; i < num_long; ++i) {
					long_data[i] = Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("long_data[" + i + "] = " + long_data[i]);
					}
				}

				double[] double_data = new double[num_double];
				for (int i = 0; i < num_double; ++i) {
					switch (i % 5) {
					case 0: double_data[i] = rangen.sample() * 1.0e12; break;
					case 1: double_data[i] = rangen.sample() * 2.0e7; break;
					case 2: double_data[i] = rangen.sample() * 3.0e0; break;
					case 3: double_data[i] = rangen.sample() * 2.0e-3; break;
					case 4: double_data[i] = rangen.sample() * 1.0e-12; break;
					}
					if (i < 10) {
						System.out.println ("double_data[" + i + "] = " + double_data[i]);
					}
				}

				String[] string_data = new String[num_string];
				for (int i = 0; i < num_string; ++i) {
					string_data[i] = "String" + string_extra(i) + Math.round (rangen.sample() * 1.0e12);
					if (i < 10) {
						System.out.println ("string_data[" + i + "] = " + string_data[i]);
					}
				}

				int[] int_data = new int[num_int];
				for (int i = 0; i < num_int; ++i) {
					int_data[i] = (int)(Math.round (rangen.sample() * 1.0e8));
					if (i < 10) {
						System.out.println ("int_data[" + i + "] = " + int_data[i]);
					}
				}

				// Marshal the data

				System.out.println ("Marshaling data ...");

				MarshalImpOutputWriter writer = new MarshalImpOutputWriter (test_make_output (filename), false);

				writer.marshalMapBegin (null);

				writer.marshalBoolean ("boolean_data[true]", true);
				writer.marshalBoolean ("boolean_data[false]", false);

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_long; ++i) {
					writer.marshalLong ("long_data[" + i + "]", long_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_double; ++i) {
					writer.marshalDouble ("double_data[" + i + "]", double_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_string; ++i) {
					writer.marshalString ("string_data[" + i + "]", string_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = 0; i < num_int; ++i) {
					writer.marshalInt ("int_data[" + i + "]", int_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				writer.marshalBoolean ("boolean_data[false]", false);
				writer.marshalBoolean ("boolean_data[true]", true);

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_long - 1; i >= 0; --i) {
					writer.marshalLong ("long_data[" + i + "]", long_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_double - 1; i >= 0; --i) {
					writer.marshalDouble ("double_data[" + i + "]", double_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_string - 1; i >= 0; --i) {
					writer.marshalString ("string_data[" + i + "]", string_data[i]);
				}

				writer.marshalMapEnd ();
				writer.marshalMapBegin (null);

				for (int i = num_int - 1; i >= 0; --i) {
					writer.marshalInt ("int_data[" + i + "]", int_data[i]);
				}

				writer.marshalMapEnd ();

				if (!( writer.check_write_complete() == 10L )) {
					System.out.println ("Writer reports writing not complete");
					return;
				}

				writer.close_writer();
				writer = null;

				// Unmarshal and check the data

				System.out.println ("Unmarshaling data ...");

				MarshalImpInputReader reader = new MarshalImpInputReader (test_make_input (filename), false);

				int errors = 0;

				reader.unmarshalMapBegin (null);

				if (!( reader.unmarshalBoolean ("boolean_data[true]") )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				if ( reader.unmarshalBoolean ("boolean_data[false]") ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_long; ++i) {
					long x = reader.unmarshalLong ("long_data[" + i + "]");
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_double; ++i) {
					double x = reader.unmarshalDouble ("double_data[" + i + "]");
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_string; ++i) {
					String x = reader.unmarshalString ("string_data[" + i + "]");
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = 0; i < num_int; ++i) {
					int x = reader.unmarshalInt ("int_data[" + i + "]");
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				if ( reader.unmarshalBoolean ("boolean_data[false]") ) {
					++errors;
					System.out.println ("Mismatched boolean: expecting false, got true");
				}

				if (!( reader.unmarshalBoolean ("boolean_data[true]") )) {
					++errors;
					System.out.println ("Mismatched boolean: expecting true, got false");
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_long - 1; i >= 0; --i) {
					long x = reader.unmarshalLong ("long_data[" + i + "]");
					if (x != long_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched long: i = " + i + ", d = " + long_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_double - 1; i >= 0; --i) {
					double x = reader.unmarshalDouble ("double_data[" + i + "]");
					if (x != double_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched double: i = " + i + ", d = " + double_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_string - 1; i >= 0; --i) {
					String x = reader.unmarshalString ("string_data[" + i + "]");
					if (!( x.equals(string_data[i]) )) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched string: i = " + i + ", d = " + string_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();
				reader.unmarshalMapBegin (null);

				for (int i = num_int - 1; i >= 0; --i) {
					int x = reader.unmarshalInt ("int_data[" + i + "]");
					if (x != int_data[i]) {
						++errors;
						if (errors <= 10) {
							System.out.println ("Mismatched int: i = " + i + ", d = " + int_data[i] + ", x = " + x);
						}
					}
				}

				reader.unmarshalMapEnd ();

				if (!( reader.check_read_complete() == 10L )) {
					System.out.println ("Reader reports reading not complete");
					return;
				}

				reader.close_reader();
				reader = null;

				System.out.println ("Error count: " + errors);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("MarshalImpOutputWriter : Unrecognized subcommand : " + args[0]);
		return;

	}




}
