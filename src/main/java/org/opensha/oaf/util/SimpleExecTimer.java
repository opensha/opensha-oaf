package org.opensha.oaf.util;


// Simple timer for implementing execution times, particularly in connection with SimpleThreadManager.
// Author: Michael Barall 03/15/2023.

public class SimpleExecTimer {

	//----- Constants -----

	// Default maximum execution time, in milliseconds, can be -1L for no limit.

	public static final long DEF_MAX_RUNTIME = -1L;		// no limit

	// Default time interval for progress messages, in milliseconds, can be -1L for no progress messages.

	public static final long DEF_PROGRESS_TIME = 30000L;		// 30 seconds

	// Default minimum positive time remaining to return, in milliseconds.

	public static final long DEF_MIN_REMAINING_TIME = 2000L;		// 2 seconds


	// Value to use for no maximum execution time.

	public static final long NO_MAX_RUNTIME = -1L;		// no limit

	// Value to use for no progress messages.

	public static final long NO_PROGRESS_TIME = -1L;		// no messages





	//----- Timer parameters -----

	// Maximum total execution time, in milliseconds, can be -1L for no limit.

	private long max_total_runtime;

	// Minimum positive time remaining to return, in milliseconds.  Must be >= 0L.

	private long min_remaining_time;




	//----- Split parameters -----

	// If the time is running, the maximum execution time of the current split, in milliseconds, can be -1L for no limit.
	// If the timer is not running, zero.

	private long max_split_runtime;




	//----- Convenience parameters -----

	// Executor to use, can be null if unspecified.
	// This is not used within the timer, but is a convience for passing to multi-threaded code.

	private AutoExecutorService executor;

	// Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// This is not used within the timer, but is a convience for passing to multi-threaded code.

	private long progress_time;




	//----- Timers -----

	// True if the timer is currently running.

	private boolean f_running;

	// Walltime at which the timer was last reset, or created if there has been no reset.

	private long reset_walltime;

	// If the timer is running, the walltime at which the current split began.
	// If the timer is not running, zero.

	private long split_walltime;

	// If the timer is running, the accumulated running time from before the beginning of the split.
	// If the timer is not running, the total accumulated running time.

	private long prior_runtime;

	// If the timer is running, zero.
	// If the timer is not running, the execution time of the most recent split.

	private long split_runtime;




	//----- Construction -----




	// Reset the timer.
	// This does not alter any parameters.
	// Returns this object.
	// After this call, the timer is stopped.

	public final SimpleExecTimer reset_timer () {

		max_split_runtime = 0L;

		f_running = false;
		reset_walltime = System.currentTimeMillis();
		split_walltime = 0L;
		prior_runtime = 0L;
		split_runtime = 0L;

		return this;
	}




	// Make an object with specified maximum total execution time and progress time interval, in milliseconds, and executor.

	public SimpleExecTimer (long max_total_runtime, long progress_time, AutoExecutorService executor) {
		if (!( max_total_runtime >= -1L )) {
			throw new IllegalArgumentException ("SimpleExecTimer.SimpleExecTimer: Invalid argument: max_total_runtime = " + max_total_runtime);
		}
		if (!( progress_time >= -1L )) {
			throw new IllegalArgumentException ("SimpleExecTimer.SimpleExecTimer: Invalid argument: progress_time = " + progress_time);
		}

		// Save parameters

		this.max_total_runtime = max_total_runtime;
		this.min_remaining_time = DEF_MIN_REMAINING_TIME;

		this.executor = executor;
		this.progress_time = progress_time;

		// Reset the timer

		reset_timer();
	}




	// Make an object with specified maximum total execution time and progress time interval, in milliseconds.

	public SimpleExecTimer (long max_total_runtime, long progress_time) {
		this (max_total_runtime, progress_time, null);
	}




	// Make an object with specified maximum total execution time, in milliseconds, and executor, and default progress time interval.

	public SimpleExecTimer (long max_total_runtime, AutoExecutorService executor) {
		this (max_total_runtime, DEF_PROGRESS_TIME, executor);
	}




	// Make an object with specified maximum total execution time, in milliseconds, and default progress time interval.

	public SimpleExecTimer (long max_total_runtime) {
		this (max_total_runtime, DEF_PROGRESS_TIME, null);
	}




	// Make an object with default maximum total execution time (unlimited) and default progress time interval.

	public SimpleExecTimer () {
		this (DEF_MAX_RUNTIME, DEF_PROGRESS_TIME, null);
	}




	//----- Parameter access -----




	// Get the maximum total execution time, in milliseconds, can be -1L for no limit.

	public final long get_max_total_runtime () {
		return max_total_runtime;
	}




	// Set the maximum total execution time, in milliseconds, can be -1L for no limit.
	// Returns this object.

	public final SimpleExecTimer set_max_total_runtime (long max_total_runtime) {
		if (!( max_total_runtime >= -1L )) {
			throw new IllegalArgumentException ("SimpleExecTimer.set_max_total_runtime: Invalid argument: max_total_runtime = " + max_total_runtime);
		}
		this.max_total_runtime = max_total_runtime;
		return this;
	}




	// Get the minimum positive time remaining to return, in milliseconds.

	public final long get_min_remaining_time () {
		return min_remaining_time;
	}




	// Set the minimum positive time remaining to return, in milliseconds.
	// Returns this object.

	public final SimpleExecTimer set_min_remaining_time (long min_remaining_time) {
		if (min_remaining_time < 0L) {
			throw new IllegalArgumentException ("SimpleExecTimer.set_min_remaining_time: Negative argument: min_remaining_time = " + min_remaining_time);
		}
		this.min_remaining_time = min_remaining_time;
		return this;
	}




	// Get the executor to use, can be null if unspecified.

	public final AutoExecutorService get_executor () {
		return executor;
	}




	// Set the executor to use, can be null if unspecified.
	// Returns this object.

	public final SimpleExecTimer set_executor (AutoExecutorService executor) {
		this.executor = executor;
		return this;
	}




	// Get the time interval for progress messages, in milliseconds, can be -1L for no progress messages.

	public final long get_progress_time () {
		return progress_time;
	}




	// Set the time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// Returns this object.

	public final SimpleExecTimer set_progress_time (long progress_time) {
		if (!( progress_time >= -1L )) {
			throw new IllegalArgumentException ("SimpleExecTimer.set_progress_time: Invalid argument: progress_time = " + progress_time);
		}
		this.progress_time = progress_time;
		return this;
	}



	//----- Readout functions -----




	// Return true if the timer is running.

	public final boolean is_running () {
		return f_running;
	}




	// Get the walltime at which the timer was last reset, or created if there has been no reset.

	public final long get_reset_walltime () {
		return reset_walltime;
	}




	// Get the walltime at which the current split began, or 0L if the timer is not running.

	public final long get_split_walltime () {
		return split_walltime;
	}




	// Get the elapsed wallclock time since the last reset (or timer creation), in milliseconds.

	public final long get_elapsed_walltime () {
		final long time_now = System.currentTimeMillis();
		return Math.max (0L, time_now - reset_walltime);
	}




	// Get the running time of the current or most recent split, in milliseconds.

	public final long get_split_runtime () {
		if (f_running) {
			final long time_now = System.currentTimeMillis();
			return Math.max (0L, time_now - split_walltime);
		}
		return split_runtime;
	}




	// Get the total running time since the last reset (or timer creation), in milliseconds.

	public final long get_total_runtime () {
		if (f_running) {
			final long time_now = System.currentTimeMillis();
			return Math.max (0L, time_now - split_walltime) + prior_runtime;
		}
		return prior_runtime;
	}




	// Get the remaining running time, in milliseconds.
	// Returns the remaining running time in milliseconds, or -1L (== NO_MAX_RUNTIME) if there is no limit.
	// If the timer is running, takes into accout both the total limit and the split limit.
	// If the return value is positive, it is not less than the effective minimum time, which is
	// defined to be the smaller of min_remaining_time and 1/5 of any time limit in effect.

	public final long get_remaining_time () {

		// Assume no limit

		long remaining_time = NO_MAX_RUNTIME;

		if (f_running) {
			if (max_total_runtime >= 0L) {
				if (max_split_runtime >= 0L) {

					// Running, and total and split runtime are both limited

					final long time_now = System.currentTimeMillis();
					remaining_time = Math.min (max_total_runtime - prior_runtime, max_split_runtime) - Math.max (0L, time_now - split_walltime);	// note, might be negative
					if (remaining_time < Math.min (min_remaining_time, Math.min (max_total_runtime, max_split_runtime) / 5L)) {
						remaining_time = 0L;
					}
				} else {

					// Running, and only total runtime is limited

					final long time_now = System.currentTimeMillis();
					remaining_time = max_total_runtime - prior_runtime - Math.max (0L, time_now - split_walltime);	// note, might be negative
					if (remaining_time < Math.min (min_remaining_time, max_total_runtime / 5L)) {
						remaining_time = 0L;
					}
				}
			}
			else if (max_split_runtime >= 0L) {

				// Running, and only split runtime is limited

				final long time_now = System.currentTimeMillis();
				remaining_time = max_split_runtime - Math.max (0L, time_now - split_walltime);	// note, might be negative
				if (remaining_time < Math.min (min_remaining_time, max_split_runtime / 5L)) {
					remaining_time = 0L;
				}
			}
		}
		else if (max_total_runtime >= 0L) {

			// Not running, and total runtime is limited

			remaining_time = max_total_runtime - prior_runtime;	// note, might be negative
			if (remaining_time < Math.min (min_remaining_time, max_total_runtime / 5L)) {
				remaining_time = 0L;
			}
		}

		return remaining_time;
	}




	// Calculate a fraction of the given time interval.
	// Parameters:
	//  x = Time interval, in milliseconds.
	//  frac = Desired fraction, 0.0 to 1.0.
	// Returns the resulting fraction of x, in milliseconds.
	// If x <= 0L, the value of x is returned unchanged.
	// If x > 0L, the returned value is guaranteed to be between 0L and x, inclusive.

	private static long calc_time_fraction (long x, double frac) {
		if (x <= 0L) {
			return x;
		}
		long y = Math.round (((double)x) * frac);
		if (y < 0L) {
			y = 0L;
		} else if (y > x) {
			y = x;
		}
		return y;
	}




	// Get a fraction of the remaining running time, in milliseconds.
	//  frac = Desired fraction, 0.0 to 1.0.
	// Returns a fraction of the remaining running time in milliseconds, or -1L (== NO_MAX_RUNTIME) if there is no limit.
	// If the timer is running, takes into accout both the total limit and the split limit.
	// If the return value is positive, it is not less than the effective minimum time, which is
	// defined to be the smaller of min_remaining_time and 1/5 of any time limit in effect.

	public final long get_frac_remaining_time (double frac) {
		if (!( frac >= 0.0 && frac <= 1.0001 )) {
			throw new IllegalArgumentException ("SimpleExecTimer.get_frac_remaining_time: Invalid argument: frac = " + frac);
		}

		// Assume no limit

		long remaining_time = NO_MAX_RUNTIME;

		if (f_running) {
			if (max_total_runtime >= 0L) {
				if (max_split_runtime >= 0L) {

					// Running, and total and split runtime are both limited

					final long time_now = System.currentTimeMillis();
					remaining_time = Math.min (max_total_runtime - prior_runtime, max_split_runtime) - Math.max (0L, time_now - split_walltime);	// note, might be negative
					remaining_time = calc_time_fraction (remaining_time, frac);
					if (remaining_time < Math.min (min_remaining_time, Math.min (max_total_runtime, max_split_runtime) / 5L)) {
						remaining_time = 0L;
					}
				} else {

					// Running, and only total runtime is limited

					final long time_now = System.currentTimeMillis();
					remaining_time = max_total_runtime - prior_runtime - Math.max (0L, time_now - split_walltime);	// note, might be negative
					remaining_time = calc_time_fraction (remaining_time, frac);
					if (remaining_time < Math.min (min_remaining_time, max_total_runtime / 5L)) {
						remaining_time = 0L;
					}
				}
			}
			else if (max_split_runtime >= 0L) {

				// Running, and only split runtime is limited

				final long time_now = System.currentTimeMillis();
				remaining_time = max_split_runtime - Math.max (0L, time_now - split_walltime);	// note, might be negative
				remaining_time = calc_time_fraction (remaining_time, frac);
				if (remaining_time < Math.min (min_remaining_time, max_split_runtime / 5L)) {
					remaining_time = 0L;
				}
			}
		}
		else if (max_total_runtime >= 0L) {

			// Not running, and total runtime is limited

			remaining_time = max_total_runtime - prior_runtime;	// note, might be negative
			remaining_time = calc_time_fraction (remaining_time, frac);
			if (remaining_time < Math.min (min_remaining_time, max_total_runtime / 5L)) {
				remaining_time = 0L;
			}
		}

		return remaining_time;
	}




	// Format an elapsed time as a string, in seconds.

	private String format_elapsed_time (long elapsed_time) {
		if (elapsed_time == NO_MAX_RUNTIME) {
			return "inf";
		}
		return String.format ("%.3f", ((double)elapsed_time)/1000.0);
	}




	// Make a one-line string showing our elapsed times.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("wall = ");
		result.append (format_elapsed_time (get_elapsed_walltime()));

		result.append (", total = ");
		result.append (format_elapsed_time (get_total_runtime()));

		result.append (", split = ");
		result.append (format_elapsed_time (get_split_runtime()));

		result.append (", remaining = ");
		result.append (format_elapsed_time (get_remaining_time()));

		return result.toString();
	}




	// Make a one-line string showing our elapsed times and some fractional remaining times.

	public String string_with_frac () {
		StringBuilder result = new StringBuilder();

		result.append ("wall = ");
		result.append (format_elapsed_time (get_elapsed_walltime()));

		result.append (", total = ");
		result.append (format_elapsed_time (get_total_runtime()));

		result.append (", split = ");
		result.append (format_elapsed_time (get_split_runtime()));

		result.append (", remaining = ");
		result.append (format_elapsed_time (get_remaining_time()));

		result.append (", 25% = ");
		result.append (format_elapsed_time (get_frac_remaining_time (0.25)));

		result.append (", 50% = ");
		result.append (format_elapsed_time (get_frac_remaining_time (0.50)));

		result.append (", 75% = ");
		result.append (format_elapsed_time (get_frac_remaining_time (0.75)));

		result.append (", running = ");
		result.append (Boolean.toString (is_running()));

		return result.toString();
	}




	//----- Timing operations -----




	// Start the timer, beginning a new split.
	// Parameters:
	//  the_max_split_runtime = Maximum execution time of the new split, in milliseconds, can be -1L for no limit.
	// Returns this object.
	// Performs no operation if already running.

	public final SimpleExecTimer start_timer (long the_max_split_runtime) {
		if (!( f_running )) {
			this.max_split_runtime = the_max_split_runtime;

			f_running = true;
			split_walltime = System.currentTimeMillis();
			split_runtime = 0L;
		}
		return this;
	}




	// Start the timer, beginning a new split, with no time limit.
	// Returns this object.
	// Performs no operation if already running.

	public final SimpleExecTimer start_timer () {
		return start_timer (NO_MAX_RUNTIME);
	}




	// Stop the timer, ending the current split.
	// Returns this object.
	// Performs no operation if not running.

	public final SimpleExecTimer stop_timer () {
		if ( f_running ) {
			this.max_split_runtime = 0L;

			f_running = false;
			final long time_now = System.currentTimeMillis();
			split_runtime = Math.max (0L, time_now - split_walltime);
			prior_runtime += split_runtime;
			split_walltime = 0L;
		}
		return this;
	}




	// Class that stops the timer when closed.
	// Can be used in a try-with-resources to run timer during execution of a block of code.

	public class AutoStopTimer implements AutoCloseable {

		// Flag indicates we are open.

		boolean f_open;

		// Constructor.

		private AutoStopTimer () {
			f_open = true;
		}

		// Close function stops the timer.

		@Override
		public void close() {
			if (f_open) {
				f_open = false;
				stop_timer ();
			}
			return;
		}
	}




	// Start the timer, beginning a new split, and return an object that stops the timer when closed.
	// Parameters:
	//  the_max_split_runtime = Maximum execution time of the new split, in milliseconds, can be -1L for no limit.
	// The returned object can be used in a try-with-resources to run timer during execution of a block of code.
	// If already running, the timer continues to run, but is stopped when the returned object is closed.

	public final AutoStopTimer start_auto_timer (long the_max_split_runtime) {
		start_timer (the_max_split_runtime);
		return new AutoStopTimer ();
	}




	// Start the timer, beginning a new split, with no time limit, and return an object that stops the timer when closed.
	// The returned object can be used in a try-with-resources to run timer during execution of a block of code.
	// If already running, the timer continues to run, but is stopped when the returned object is closed.

	public final AutoStopTimer start_auto_timer () {
		start_timer ();
		return new AutoStopTimer ();
	}




	// Return an object that stops the timer when closed.
	// The returned object can be used in a try-with-resources to run timer during execution of a block of code.
	// This function does not start the timer.

	public final AutoStopTimer get_auto_stop_timer () {
		return new AutoStopTimer ();
	}




	//----- Testing -----




	// Sleep for one second.

	private static void sleep_one_second () {
		try {
			Thread.sleep (1000L);
		} catch (InterruptedException e) {
		}
		return;
	}




	// Track timer for specified number of seconds, outputting status once per second.

	private static void track_timer (SimpleExecTimer exec_timer, int secs) {
		for (int j = 0; j <= secs; ++j) {
			if (j != 0) {
				sleep_one_second();
			}
			System.out.println (j + ": " + exec_timer.string_with_frac());
		}
		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "SimpleExecTimer");




		// Subcommand : Test #1
		// Command format:
		//  test1  max_total  max_split
		// Create a timer and perform two splits, a reset, and then another split.
		// Use the given values for maximum total runtime and maximum split runtime.
		// Display timer status every 1 second.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Testing execution timer");
			long max_total = testargs.get_long ("max_total");
			long max_split = testargs.get_long ("max_split");
			testargs.end_test();

			// Create timer, and track 5 seconds

			System.out.println ();
			System.out.println ("Create timer");

			SimpleExecTimer exec_timer = new SimpleExecTimer (max_total);

			track_timer (exec_timer, 5);

			// Start timer, and track 20 seconds

			System.out.println ();
			System.out.println ("Start timer");

			exec_timer.start_timer (max_split);

			track_timer (exec_timer, 20);

			// Stop timer, and track 5 seconds

			System.out.println ();
			System.out.println ("Stop timer");

			exec_timer.stop_timer ();

			track_timer (exec_timer, 5);

			// Start timer using try-with-resources, and track 20 seconds

			System.out.println ();
			System.out.println ("Start timer, using try-with-resources");

			try (
				SimpleExecTimer.AutoStopTimer auto_stop_timer = exec_timer.start_auto_timer (max_split);
			) {

				track_timer (exec_timer, 20);

				// Stop timer using try-with-resources, and track 5 seconds

				System.out.println ();
				System.out.println ("Stop timer, using try-with-resources");

			}

			track_timer (exec_timer, 5);

			// Reset timer, and track 5 seconds

			System.out.println ();
			System.out.println ("Reset timer");

			exec_timer.reset_timer ();

			track_timer (exec_timer, 5);

			// Start timer using try-with-resources, no limit, and track 20 seconds

			System.out.println ();
			System.out.println ("Start timer, using try-with-resources, no limit");

			try (
				SimpleExecTimer.AutoStopTimer auto_stop_timer = exec_timer.start_auto_timer ();
			) {

				track_timer (exec_timer, 20);

				// Stop timer using try-with-resources, and track 5 seconds

				System.out.println ();
				System.out.println ("Stop timer, using try-with-resources");

			}

			track_timer (exec_timer, 5);

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
