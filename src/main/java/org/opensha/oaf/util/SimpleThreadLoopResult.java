package org.opensha.oaf.util;

import org.opensha.oaf.comcat.GeoJsonUtils;


// Class to hold the result of a simple multi-threaded loop.
// Author: Michael Barall 06/09/2023.
//
// This class is used to store the result of a loop managed by SimpleThreadLoopHelper.
// It can also aggregate the results of multiple loops.

public class SimpleThreadLoopResult {

	//----- Result variables -----

	// The number of results aggregated in this object.

	public int result_count;

	// The elapsed time, in milliseconds, or -1L if unknown.

	public long elapsed_time;

	// The memory usage, in bytes, or -1L if unknown.

	public long used_memory;

	// The requested loop count.

	public long loop_count;

	// The number of completions.

	public long completion_count;

	// The number of loops that were incomplete.

	public int incomplete_count;

	// The number of loops that requested timeout.

	public int timeout_count;

	// The number of loops that requested abort.

	public int abort_count;


	//----- Transient information -----

	// The abort message from the most recent abort, or null if none.
	// This field is not marshaled.

	public String abort_message;




	//----- Readout -----




	// Return true if we have the elapsed time.

	public final boolean has_elapsed_time () {
		return elapsed_time >= 0L;
	}

	// Return the elapsed time in seconds as a string, or the default string if not available.

	public final String get_elapsed_time_as_string (String def_string) {
		String result = def_string;
		if (has_elapsed_time()) {
			result = String.format ("%d.%01d", elapsed_time / 1000L, (elapsed_time % 1000L) / 100L);
		}
		return result;
	}

	// Return the elapsed time in seconds as a string, with prefix and suffix, or the default string if not available.

	public final String get_elapsed_time_as_string (String prefix, String suffix, String def_string) {
		String result = def_string;
		if (has_elapsed_time()) {
			result = String.format ("%s%d.%01d%s", prefix, elapsed_time / 1000L, (elapsed_time % 1000L) / 100L, suffix);
		}
		return result;
	}




	// Return true if we have the memory usage.

	public final boolean has_used_memory () {
		return used_memory >= 0L;
	}

	// Return the used memory as a string (including the unit), or the default string if not available.

	public final String get_used_memory_as_string (String def_string) {
		String result = def_string;
		if (has_used_memory()) {
			result = SimpleUtils.used_memory_to_string (used_memory);
		}
		return result;
	}

	// Return the used memory as a string (including the unit), with prefix and suffix, or the default string if not available.

	public final String get_used_memory_as_string (String prefix, String suffix, String def_string) {
		String result = def_string;
		if (has_used_memory()) {
			result = prefix + SimpleUtils.used_memory_to_string (used_memory) + suffix;
		}
		return result;
	}




	// Return true if we have an abort message.

	public final boolean has_abort_message () {
		return (abort_message != null && abort_message.length() > 0);
	}




	//----- Construction -----




	// Clear the results.

	public final SimpleThreadLoopResult clear () {
		result_count = 0;
		elapsed_time = -1L;
		used_memory = -1L;
		loop_count = 0L;
		completion_count = 0L;
		incomplete_count = 0;
		timeout_count = 0;
		abort_count = 0;
		abort_message = null;
		return this;
	}



	// Default constructor.

	public SimpleThreadLoopResult () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("SimpleThreadLoopResult:" + "\n");
		result.append ("result_count = " + result_count + "\n");

		result.append ("elapsed_time = " + elapsed_time + get_elapsed_time_as_string (" (", " seconds)", " (unknown)") + "\n");

		result.append ("used_memory = " + used_memory + get_used_memory_as_string (" (", ")", " (unknown)") + "\n");

		result.append ("loop_count = " + loop_count + "\n");
		result.append ("completion_count = " + completion_count + "\n");
		result.append ("incomplete_count = " + incomplete_count + "\n");
		result.append ("timeout_count = " + timeout_count + "\n");
		result.append ("abort_count = " + abort_count + "\n");

		if (has_abort_message()) {
			result.append ("abort_message:" + "\n");
			result.append (abort_message + "\n");
		}

		return result.toString();
	}




	//----- Accumulation -----




	// Add time to the amount of elapsed time.
	// Parameter:
	//  the_elapsed_time = Amount of time to add, in milliseconds.  A negative value indicates unknown.

	public final void add_elapsed_time (long the_elapsed_time) {
		if (the_elapsed_time >= 0L) {
			if (elapsed_time >= 0L) {
				elapsed_time += the_elapsed_time;
			} else {
				elapsed_time = the_elapsed_time;
			}
		}
		return;
	}




	// Add memory usage, in bytes.
	// Parameters:
	//  the_used_memory = Amount of memory to add, in bytes.  A negative value indicates unknown.
	// Memory usage is accumulated by taking the maximum.

	public final void add_used_memory (long the_used_memory) {
		used_memory = Math.max (used_memory, the_used_memory);
		return;
	}




	// Add an abort message.
	// Parameters:
	//  the_abort_message = Abort message.  Can be null if none.
	// Abort messages are accumulated by saving the last one.

	public final void add_abort_message (String the_abort_message) {
		if (the_abort_message != null && the_abort_message.length() > 0) {
			abort_message = the_abort_message;
		}
		return;
	}




	// Copy results from another object.

	public final SimpleThreadLoopResult copy_from (SimpleThreadLoopResult other) {
		this.result_count		= other.result_count;
		this.elapsed_time		= other.elapsed_time;
		this.used_memory		= other.used_memory;
		this.loop_count			= other.loop_count;
		this.completion_count	= other.completion_count;
		this.incomplete_count	= other.incomplete_count;
		this.timeout_count		= other.timeout_count;
		this.abort_count		= other.abort_count;
		this.abort_message		= other.abort_message;
		return this;
	}




	// Accumulate results from another object.

	public final SimpleThreadLoopResult accum_from (SimpleThreadLoopResult other) {
		this.result_count		+= other.result_count;
		add_elapsed_time		  (other.elapsed_time);
		add_used_memory			  (other.used_memory);
		this.loop_count			+= other.loop_count;
		this.completion_count	+= other.completion_count;
		this.incomplete_count	+= other.incomplete_count;
		this.timeout_count		+= other.timeout_count;
		this.abort_count		+= other.abort_count;
		add_abort_message		  (other.abort_message);
		return this;
	}




	// Accumulate results from a loop, which are added to any prior results.
	// Usage note: The recorded elapsed time is the time since the loop launched.
	// Threading: This function calls thread-safe functions to obtain information about
	// the loop, however it is intended to be used after the loop completes.

	public final SimpleThreadLoopResult accum_loop (SimpleThreadLoopHelper loop_helper) {
		++result_count;
		add_elapsed_time (System.currentTimeMillis() - loop_helper.get_start_time());
		add_used_memory (loop_helper.get_max_used_memory());
		loop_count += ((long)(loop_helper.get_loop_count()));
		completion_count += ((long)(loop_helper.get_completions()));
		if (loop_helper.is_incomplete()) {
			++incomplete_count;
		}
		if (loop_helper.is_timeout()) {
			++timeout_count;
		}
		if (loop_helper.is_abort()) {
			++abort_count;
			add_abort_message (loop_helper.get_abort_message_string());
		}
		return this;
	}




	// Set results from a loop, overwriting any prior results.
	// Usage note: The recorded elapsed time is the time since the loop launched.
	// Threading: This function calls thread-safe functions to obtain information about
	// the loop, however it is intended to be used after the loop completes.

	public final SimpleThreadLoopResult set_loop (SimpleThreadLoopHelper loop_helper) {
		clear();
		accum_loop (loop_helper);
		return this;
	}




	//----- Sentinel -----




	// Sentinel class used to time a block, and add its time to our elapsed time.
	// This is intended to be used in a try-with-resources.

	private class TimingSentinel extends AutoCleanup {

		// The time this sentinel was created.

		private long creation_time;

		// Constructor saves the creation time.

		public TimingSentinel () {
			creation_time = System.currentTimeMillis();
		}

		// On close, accumulate the elapsed time.

		@Override
		protected void cleanup () {
			add_elapsed_time (System.currentTimeMillis() - creation_time);
			return;
		}
	}




	// Get a sentinel for timing the duration of a block.
	// The returned AutoCloseable object calculates and accumulates elapsed time when closed.
	// This is intended to be used in a try-with-resources.

	public final AutoCleanup get_timing_sentinel () {
		return new TimingSentinel();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 124001;

	private static final String M_VERSION_NAME = "SimpleThreadLoopResult";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt ("result_count", result_count);
			writer.marshalLong ("elapsed_time", elapsed_time);
			writer.marshalLong ("used_memory", used_memory);
			writer.marshalLong ("loop_count", loop_count);
			writer.marshalLong ("completion_count", completion_count);
			writer.marshalInt ("incomplete_count", incomplete_count);
			writer.marshalInt ("timeout_count", timeout_count);
			writer.marshalInt ("abort_count", abort_count);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			clear();		// for unmarshaled fields

			result_count = reader.unmarshalInt ("result_count");
			elapsed_time = reader.unmarshalLong ("elapsed_time");
			used_memory = reader.unmarshalLong ("used_memory");
			loop_count = reader.unmarshalLong ("loop_count");
			completion_count = reader.unmarshalLong ("completion_count");
			incomplete_count = reader.unmarshalInt ("incomplete_count");
			timeout_count = reader.unmarshalInt ("timeout_count");
			abort_count = reader.unmarshalInt ("abort_count");

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public SimpleThreadLoopResult unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, SimpleThreadLoopResult perf_data) {
		writer.marshalMapBegin (name);
		perf_data.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static SimpleThreadLoopResult static_unmarshal (MarshalReader reader, String name) {
		SimpleThreadLoopResult perf_data = new SimpleThreadLoopResult();
		reader.unmarshalMapBegin (name);
		perf_data.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return perf_data;
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	private static SimpleThreadLoopResult make_test_value () {
		SimpleThreadLoopResult loop_result = new SimpleThreadLoopResult();

		loop_result.result_count = 3;
		loop_result.elapsed_time = 123456L;
		loop_result.used_memory = 7L * 1024L * 1024L * 1024L;
		loop_result.loop_count = 500000;
		loop_result.completion_count = 300000;
		loop_result.incomplete_count = 3;
		loop_result.timeout_count = 2;
		loop_result.abort_count = 1;
		loop_result.abort_message = "Test of the abort message";

		return loop_result;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "SimpleThreadLoopResult");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, copying, and accumulating results");
			testargs.end_test();

			// Create the values

			SimpleThreadLoopResult loop_result = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Result display **********");
			System.out.println ();

			System.out.println (loop_result.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			SimpleThreadLoopResult.static_marshal (store, null, loop_result);
			store.check_write_complete ();

			String json_string = store.get_json_string();
			//System.out.println (json_string);

			Object json_container = store.get_json_container();
			System.out.println (GeoJsonUtils.jsonObjectToString (json_container));

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			SimpleThreadLoopResult loop_result2 = null;

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			loop_result2 = SimpleThreadLoopResult.static_unmarshal (retrieve, null);
			retrieve.check_read_complete ();

			// Display the contents

			System.out.println (loop_result2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy results **********");
			System.out.println ();
			
			SimpleThreadLoopResult loop_result3 = new SimpleThreadLoopResult();
			loop_result3.copy_from (loop_result2);

			// Display the contents

			System.out.println (loop_result3.toString());

			// Accumulate values

			System.out.println ();
			System.out.println ("********** Accumulate results **********");
			System.out.println ();
			
			loop_result3.accum_from (loop_result2);
			System.out.println (loop_result3.toString());

			// Accumulate values

			System.out.println ();
			System.out.println ("********** Accumulate results **********");
			System.out.println ();
			
			loop_result3.accum_from (loop_result);
			System.out.println (loop_result3.toString());

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
