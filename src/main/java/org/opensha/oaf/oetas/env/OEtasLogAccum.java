package org.opensha.oaf.oetas.env;


// Accumulator for log information across multiple forecasts for operational ETAS.
// Author: Michael Barall 07/19/2024.
//
// This class contains the information necessary for logging accumulated
// performance information across multiple operational ETAS operations.
//
// Note: This class is not intended to be marshaled.

public class OEtasLogAccum {


	//----- Result table -----

	// Success/fail counts.

	private long success_count;
	private long failure_count;

	// Nummber of operations returning each result code.

	private long[] rescode_count;




	//----- Timing table -----

	// Timing cuts, in seconds.

	private static final long[] timing_cuts_secs = {
		30L,
		60L,
		90L,
		120L,
		150L,
		180L,
		210L,
		240L,
		270L,
		300L,
		360L,
		420L,
		480L,
		540L,
		600L,
		720L,
		840L,
		960L,
		1080L,
		1200L		// Last bin includes anything higher
	};

	// Maximum times, in milliseconds.

	private long total_time_max;
	private long fit_time_max;
	private long sim_time_max;

	// Counts in various time bins.

	private long[] total_time_count;
	private long[] fit_time_count;
	private long[] sim_time_count;

	// A second, in milliseconds.

	private static final long SEC = 1000L;




	//----- Memory table -----

	// Memory cuts, in megabytes.

	private static final long[] memory_cuts_mb = {
		250L,
		500L,
		1000L,
		1500L,
		2000L,
		3000L,
		4000L,
		5000L,
		6000L,
		8000L,
		10000L,
		13000L,
		16000L,
		20000L,
		25000L,
		30000L,
		35000L,
		40000L,
		50000L,
		60000L		// Last bin includes anything higher
	};

	// Maximum memory, in bytes.

	private long fit_memory_max;
	private long sim_memory_max;

	// Counts in various memory bins.

	private long[] fit_memory_count;
	private long[] sim_memory_count;

	// A megabyte, in bytes.

	private static final long MB = 1048576L;




	//----- Functions -----


	// Find an index within a cut array.
	// Parameters:
	//  cuts = Array of cuts, in increasing order.
	//  x = Value to search for.
	// Returns the smallest index such that cuts[index] >= x,
	// or cuts.length if no such index exists.

	private static int find_cut_index (long[] cuts, long x) {
		int n = cuts.length;
		for (int i = 0; i < n; ++i) {
			if (cuts[i] >= x) {
				return i;
			}
		}
		return n;
	}


	// Increment the count, selected by a cut.
	// Parameters:
	//  cuts = Array of cuts, in increasing order.
	//  counts = Array containing a count for each cut, plus one above last.
	//  x = Value to search for.
	// Increments the count for x.

	private static void apply_cut_inc (long[] cuts, long counts[], long x) {
		int i = find_cut_index (cuts, x);
		counts[i] = counts[i] + 1;
		return;
	}


//	// Apply maximum to the count, selected by a cut.
//	// Parameters:
//	//  cuts = Array of cuts, in increasing order.
//	//  counts = Array containing a count for each cut, plus one above last.
//	//  x = Value to search for.
//	//  val = Value for maximum
//	// Increases the count for x, if necessary, so it is >= val..
//
//	private static void apply_cut_max (long[] cuts, long counts[], long x, long val) {
//		int i = find_cut_index (cuts, x);
//		counts[i] = Math.max (counts[i], val);
//		return;
//	}


	// Convert long to string.

	private static String l_to_s (long x) {
		return String.format ("%d", x);
	}


	// Get the length of a long, when converted to characters.

	private static int l_chars (long x) {
		return (String.format ("%d", x)).length();
	}


	// Get the string from of the cut at the given index.
	// String begins with ">" if index is one past the end.

	private static String cut_to_s (long[] cuts, int index) {
		if (index == cuts.length) {
			return ">" + l_to_s (cuts[index - 1]);
		}
		return l_to_s (cuts[index]);
	}


	// Append string, padding on the left to achieve the requested field width.

	private static void append_lpad (StringBuilder sb, int width, String s) {
		int n = width - s.length();
		for (int i = 0; i < n; ++i) {
			sb.append (" ");
		}
		sb.append (s);
		return;
	}

	private static void append_lpad (StringBuilder sb, int width, long x) {
		append_lpad (sb, width, l_to_s (x));
		return;
	}


	// Append string, padding on the right to achieve the requested field width.

	private static void append_rpad (StringBuilder sb, int width, String s) {
		sb.append (s);
		int n = width - s.length();
		for (int i = 0; i < n; ++i) {
			sb.append (" ");
		}
		return;
	}

	private static void append_rpad (StringBuilder sb, int width, long x) {
		append_rpad (sb, width, l_to_s (x));
		return;
	}




	// Accumulate an ETAS failure.
	// Parameters:
	//  rescode = ETAS result code.

	public void accum_failure (int rescode) {

		// Count failure

		++failure_count;

		// Force result code into range, and count it

		int my_rescode = rescode;
		if (my_rescode < OEExecEnvironment.ETAS_RESCODE_MIN || my_rescode > OEExecEnvironment.ETAS_RESCODE_MAX) {
			my_rescode = OEExecEnvironment.ETAS_RESCODE_UNKNOWN_FAILURE;
		}

		int j = my_rescode - OEExecEnvironment.ETAS_RESCODE_MIN;
		rescode_count[j] = rescode_count[j] + 1;

		return;
	}




	// Accumulate an ETAS success.
	// Parameters:
	//  rescode = ETAS result code.
	//  total_time = Total execution time, in milliseconds, or -1L if unknown.
	//  fit_time = Fitting execution time, in milliseconds, or -1L if unknown.
	//  sim_time = Simulation execution time, in milliseconds, or -1L if unknown.
	//  fit_memory = Fitting memory, in bytes, or -1L if unknown.
	//  sim_memory = Simulation memory, in bytes, or -1L if unknown.

	public void accum_success (
		int rescode,
		long total_time,
		long fit_time,
		long sim_time,
		long fit_memory,
		long sim_memory
	) {

		// Count success

		++success_count;

		// Force result code into range, and count it

		int my_rescode = rescode;
		if (my_rescode < OEExecEnvironment.ETAS_RESCODE_MIN || my_rescode > OEExecEnvironment.ETAS_RESCODE_MAX) {
			my_rescode = OEExecEnvironment.ETAS_RESCODE_UNKNOWN_FAILURE;
		}

		int j = my_rescode - OEExecEnvironment.ETAS_RESCODE_MIN;
		rescode_count[j] = rescode_count[j] + 1;

		// If total time is not provided, compute from the fitting and simulation times

		long my_total_time = total_time;
		if (my_total_time < 0L) {
			if (fit_time >= 0L) {
				if (sim_time >= 0L) {
					my_total_time = fit_time + sim_time;
				} else {
					my_total_time = fit_time;
				}
			} else {
				if (sim_time >= 0L) {
					my_total_time = sim_time;
				}
			}
		}

		// Record total time

		if (my_total_time >= 0L) {
			total_time_max = Math.max (total_time_max, my_total_time);
			apply_cut_inc (timing_cuts_secs, total_time_count, my_total_time/SEC);
		}

		// Record fitting time

		if (fit_time >= 0L) {
			fit_time_max = Math.max (fit_time_max, fit_time);
			apply_cut_inc (timing_cuts_secs, fit_time_count, fit_time/SEC);
		}

		// Record simulation time

		if (sim_time >= 0L) {
			sim_time_max = Math.max (sim_time_max, sim_time);
			apply_cut_inc (timing_cuts_secs, sim_time_count, sim_time/SEC);
		}

		// Record fitting memory

		if (fit_memory >= 0L) {
			fit_memory_max = Math.max (fit_memory_max, fit_memory);
			apply_cut_inc (memory_cuts_mb, fit_memory_count, fit_memory/MB);
		}

		// Record simulation memory

		if (sim_memory >= 0L) {
			sim_memory_max = Math.max (sim_memory_max, sim_memory);
			apply_cut_inc (memory_cuts_mb, sim_memory_count, sim_memory/MB);
		}

		return;
	}




	//----- Construction -----




	// Clear the information.

	public final void clear () {

		success_count = 0L;
		failure_count = 0L;

		int n = OEExecEnvironment.ETAS_RESCODE_MAX + 1 - OEExecEnvironment.ETAS_RESCODE_MIN;
		rescode_count = new long[n];
		for (int i = 0; i < n; ++i) {
			rescode_count[i] = 0L;
		}

		total_time_max = 0L;
		fit_time_max = 0L;
		sim_time_max = 0L;

		n = timing_cuts_secs.length + 1;
		total_time_count = new long[n];
		fit_time_count = new long[n];
		sim_time_count = new long[n];
		for (int i = 0; i < n; ++i) {
			total_time_count[i] = 0L;
			fit_time_count[i] = 0L;
			sim_time_count[i] = 0L;
		}

		fit_memory_max = 0L;
		sim_memory_max = 0L;

		n = memory_cuts_mb.length + 1;
		fit_memory_count = new long[n];
		sim_memory_count = new long[n];
		for (int i = 0; i < n; ++i) {
			fit_memory_count[i] = 0L;
			sim_memory_count[i] = 0L;
		}

		return;
	}




	// Default constructor.

	public OEtasLogAccum () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		int w1;
		int w2;
		int w3;
		int w4;

		String h1;
		String h2;
		String h3;
		String h4;

		String spacer = "  ";

		//Summary

		result.append ("Accumulated ETAS performance data.\n");
		result.append ("\n");

		w1 = Math.max (l_chars (success_count), l_chars (failure_count));

		result.append ("success_count = ");
		append_lpad (result, w1, success_count);
		result.append ("\n");

		result.append ("failure_count = ");
		append_lpad (result, w1, failure_count);
		result.append ("\n");

		// Result table

		result.append ("\n");
		result.append ("Operations returning each result code.\n");
		result.append ("\n");

		h1 = "code";
		h2 = "count";

		w1 = h1.length();
		w2 = h2.length();

		int n = rescode_count.length;

		for (int i = 0; i < n; ++i) {
			int rc = i + OEExecEnvironment.ETAS_RESCODE_MIN;
			if (rescode_count[i] != 0L || rc == OEExecEnvironment.ETAS_RESCODE_OK) {
				w1 = Math.max (w1, OEExecEnvironment.etas_result_to_string(rc).length());
				w2 = Math.max (w2, l_chars (rescode_count[i]));
			}
		}

		append_rpad (result, w1, h1);
		result.append (spacer);
		append_lpad (result, w2, h2);
		result.append ("\n");

		for (int i = 0; i < n; ++i) {
			int rc = i + OEExecEnvironment.ETAS_RESCODE_MIN;
			if (rescode_count[i] != 0L || rc == OEExecEnvironment.ETAS_RESCODE_OK) {
				append_rpad (result, w1, OEExecEnvironment.etas_result_to_string(rc));
				result.append (spacer);
				append_lpad (result, w2, rescode_count[i]);
				result.append ("\n");
			}
		}

		// Timing table

		result.append ("\n");
		result.append ("Timing summary.\n");
		result.append ("\n");

		w1 = Math.max (Math.max (l_chars (total_time_max/SEC), l_chars (fit_time_max/SEC)),  l_chars (sim_time_max/SEC));

		result.append ("total_time_max = ");
		append_lpad (result, w1, total_time_max/SEC);
		result.append (" s\n");

		result.append ("fit_time_max   = ");
		append_lpad (result, w1, fit_time_max/SEC);
		result.append (" s\n");

		result.append ("sim_time_max   = ");
		append_lpad (result, w1, sim_time_max/SEC);
		result.append (" s\n");

		n = timing_cuts_secs.length;
		while (n > 0 && total_time_count[n] == 0L && fit_time_count[n] == 0L && sim_time_count[n] == 0L) {
			--n;
		}

		result.append ("\n");
		result.append ("Distribution of operations within each execution time range.\n");
		result.append ("\n");

		h1 = "sec";
		h2 = "total";
		h3 = "fit";
		h4 = "sim";

		w1 = h1.length();
		w2 = h2.length();
		w3 = h3.length();
		w4 = h4.length();

		for (int i = 0; i <= n; ++i) {
			w1 = Math.max (w1, cut_to_s(timing_cuts_secs, i).length());
			w2 = Math.max (w2, l_chars (total_time_count[i]));
			w3 = Math.max (w3, l_chars (fit_time_count[i]));
			w4 = Math.max (w4, l_chars (sim_time_count[i]));
		}

		append_lpad (result, w1, h1);
		result.append (spacer);
		append_lpad (result, w2, h2);
		result.append (spacer);
		append_lpad (result, w3, h3);
		result.append (spacer);
		append_lpad (result, w4, h4);
		result.append ("\n");

		for (int i = 0; i <= n; ++i) {
			append_lpad (result, w1, cut_to_s(timing_cuts_secs, i));
			result.append (spacer);
			append_lpad (result, w2, total_time_count[i]);
			result.append (spacer);
			append_lpad (result, w3, fit_time_count[i]);
			result.append (spacer);
			append_lpad (result, w4, sim_time_count[i]);
			result.append ("\n");
		}

		// Memory table

		result.append ("\n");
		result.append ("Memory summary.\n");
		result.append ("\n");

		w1 = Math.max (l_chars (fit_memory_max/MB), l_chars (sim_memory_max/MB));

		result.append ("fit_memory_max = ");
		append_lpad (result, w1, fit_memory_max/MB);
		result.append (" M\n");

		result.append ("sim_memory_max = ");
		append_lpad (result, w1, sim_memory_max/MB);
		result.append (" M\n");

		n = memory_cuts_mb.length;
		while (n > 0 && fit_memory_count[n] == 0L && sim_memory_count[n] == 0L) {
			--n;
		}

		result.append ("\n");
		result.append ("Distribution of operations within each memory range.\n");
		result.append ("\n");

		h1 = "MB";
		h2 = "fit";
		h3 = "sim";

		w1 = h1.length();
		w2 = h2.length();
		w3 = h3.length();

		for (int i = 0; i <= n; ++i) {
			w1 = Math.max (w1, cut_to_s(memory_cuts_mb, i).length());
			w2 = Math.max (w2, l_chars (fit_memory_count[i]));
			w3 = Math.max (w3, l_chars (sim_memory_count[i]));
		}

		append_lpad (result, w1, h1);
		result.append (spacer);
		append_lpad (result, w2, h2);
		result.append (spacer);
		append_lpad (result, w3, h3);
		result.append ("\n");

		for (int i = 0; i <= n; ++i) {
			append_lpad (result, w1, cut_to_s(memory_cuts_mb, i));
			result.append (spacer);
			append_lpad (result, w2, fit_memory_count[i]);
			result.append (spacer);
			append_lpad (result, w3, sim_memory_count[i]);
			result.append ("\n");
		}

		return result.toString();
	}




	//----- Global accumulator -----




	// The global accumulator.

	private static OEtasLogAccum global_accum = null;




	// Clear the global accumulator.

	public static synchronized void global_clear () {
		if (global_accum == null) {
			global_accum = new OEtasLogAccum();
		}

		global_accum.clear();
		return;
	}




	// Write the global accumulator contents into a string.

	public static synchronized String global_to_string () {
		if (global_accum == null) {
			global_accum = new OEtasLogAccum();
		}

		return global_accum.toString();
	}




	// Global accumulate an ETAS failure.
	// Parameters:
	//  rescode = ETAS result code.

	public static synchronized void global_accum_failure (int rescode) {
		if (global_accum == null) {
			global_accum = new OEtasLogAccum();
		}

		global_accum.accum_failure (rescode);
		return;
	}




	// Global accumulate an ETAS failure, and return the contents as a string.
	// Parameters:
	//  rescode = ETAS result code.

	public static synchronized String global_accum_failure_to_string (int rescode) {
		if (global_accum == null) {
			global_accum = new OEtasLogAccum();
		}

		global_accum.accum_failure (rescode);

		return global_accum.toString();
	}




	// Global accumulate an ETAS success.
	// Parameters:
	//  rescode = ETAS result code.
	//  total_time = Total execution time, in milliseconds, or -1L if unknown.
	//  fit_time = Fitting execution time, in milliseconds, or -1L if unknown.
	//  sim_time = Simulation execution time, in milliseconds, or -1L if unknown.
	//  fit_memory = Fitting memory, in bytes, or -1L if unknown.
	//  sim_memory = Simulation memory, in bytes, or -1L if unknown.

	public static synchronized void global_accum_success (
		int rescode,
		long total_time,
		long fit_time,
		long sim_time,
		long fit_memory,
		long sim_memory
	) {
		if (global_accum == null) {
			global_accum = new OEtasLogAccum();
		}

		global_accum.accum_success (
			rescode,
			total_time,
			fit_time,
			sim_time,
			fit_memory,
			sim_memory
		);
		return;
	}




	// Global accumulate an ETAS success, and return the contents as a string.
	// Parameters:
	//  rescode = ETAS result code.
	//  total_time = Total execution time, in milliseconds, or -1L if unknown.
	//  fit_time = Fitting execution time, in milliseconds, or -1L if unknown.
	//  sim_time = Simulation execution time, in milliseconds, or -1L if unknown.
	//  fit_memory = Fitting memory, in bytes, or -1L if unknown.
	//  sim_memory = Simulation memory, in bytes, or -1L if unknown.

	public static synchronized String global_accum_success_to_string (
		int rescode,
		long total_time,
		long fit_time,
		long sim_time,
		long fit_memory,
		long sim_memory
	) {
		if (global_accum == null) {
			global_accum = new OEtasLogAccum();
		}

		global_accum.accum_success (
			rescode,
			total_time,
			fit_time,
			sim_time,
			fit_memory,
			sim_memory
		);

		return global_accum.toString();
	}




}
