package org.opensha.oaf.util;

import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;


// Helpwer class for implementing a simple multi-threaded loop.
// Author: Michael Barall 03/16/2023.
//
// This class is used together with SimpleThreadManager and SimpleThreadTarget
// to implement a simple multi-threaded loop.

public class SimpleThreadLoopHelper {

	//----- Thread communication -----

	// The thread manager.

	private SimpleThreadManager thread_manager = new SimpleThreadManager();


	// Get the thread manager.

	public SimpleThreadManager get_thread_manager () {
		return thread_manager;
	}


	// Get the time that threads were launched, in milliseconds since the epoch.
	// Note: This call is passed thru to the thread manager.

	public final long get_start_time () {
		return thread_manager.get_start_time();
	}


	// Return true if thread abort has occurred.
	// If this returns true, then abort messages are available.
	// Note: This call is passed thru to the thread manager.
	// Threading: Although this function is thread-safe, it is intended to be
	// accessed after all threads have terminated.

	public final boolean is_abort () {
		return thread_manager.is_abort();
	}


	// Return true if timeout has been requested.
	// We return true if prompt termination has been requested but no abort has occurred.
	// Note: This call is passed thru to the thread manager.
	// Threading: Although this function is thread-safe, it is intended to be
	// accessed after all threads have terminated.

	public final boolean is_timeout () {
		return thread_manager.is_timeout();
	}


	// Return true if prompt termination has been requested.
	// This may be called while running to monitor progress,
	// or after termination to check if prompt termination occurred.
	// Note: This call is passed thru to the thread manager.
	// Threading: This function is thread-safe.

	public final boolean get_req_termination () {
		return thread_manager.get_req_termination();
	}


	// Get a copy of the list of abort messages.
	// Note: This call is passed thru to the thread manager.
	// Threading: This function is thread-safe.

	public final List<String> get_abort_message_list () {
		return thread_manager.get_abort_message_list();
	} 


	// Get the abort messages as a single string.
	// Note: This call is passed thru to the thread manager.
	// Threading: This function is thread-safe.

	public final String get_abort_message_string () {
		return thread_manager.get_abort_message_string();
	}




	//----- Loop control variables -----

	// The beginning index of the loop, inclusive.
	// Threads must treat this as read-only.

	private int my_begin_index;

	// The ending index of the loop, exclusive.
	// Threads must treat this as read-only.

	private int my_end_index;

	// The current loop index.

	private AtomicInteger current_loop_index = new AtomicInteger();

	// The number of iterations completed so far.

	private AtomicInteger current_completions = new AtomicInteger();

	// The maximum amount of memory used, in bytes, or -1L if unknown.
	// Note: This is only updated when a progress message displays current memory usage,
	// or when the user calls update_max_used_memory.

	private AtomicLong max_used_memory = new AtomicLong();


	// Initialize loop control variables, and specify the loop index bounds.
	// Threading: This function should be called before launching threads.

	public final void init_loop_bounds (int begin_index, int end_index) {
		my_begin_index = begin_index;
		my_end_index = end_index;
		current_loop_index.set (begin_index);
		current_completions.set (0);
		max_used_memory.set (-1L);
		return;
	}


	// Get the beginning index of the loop, inclusive.
	// Threading: This function is thread-safe, assuming threads do not modify the loop bounds.

	public final int get_begin_index () {
		return my_begin_index;
	}


	// Get the ending index of the loop, exclusive.
	// Threading: This function is thread-safe, assuming threads do not modify the loop bounds.

	public final int get_end_index () {
		return my_end_index;
	}


	// Get the loop count.
	// Threading: This function is thread-safe, assuming threads do not modify the loop bounds.

	public final int get_loop_count () {
		return my_end_index - my_begin_index;
	}


	// Get the next loop index.
	// Returns -1 if no more iterations.
	// Returns -2 if prompt termination has been requested.
	// Threading: This function is thread-safe.

	public final int get_loop_index () {
		if (thread_manager.get_req_termination()) {
			return -2;
		}
		int loop_index = current_loop_index.getAndIncrement();
		if (loop_index >= my_end_index) {
			return -1;
		}
		return loop_index;
	}


	// Count a complete iteration, and then get the next loop index.
	// Returns -1 if no more iterations.
	// Returns -2 if prompt termination has been requested.
	// Threading: This function is thread-safe.
	// Note: This permits the thread handler to use a loop like:
	//   for (int loop_index = get_loop_index(); loop_index >= 0; loop_index = get_next_index())

	public final int get_next_index () {
		current_completions.incrementAndGet();
		if (thread_manager.get_req_termination()) {
			return -2;
		}
		int loop_index = current_loop_index.getAndIncrement();
		if (loop_index >= my_end_index) {
			return -1;
		}
		return loop_index;
	}


	// Count a complete iteration.
	// Returns the number of completions.
	// Threading: This function is thread-safe.

	public final int add_completion () {
		int completions = current_completions.incrementAndGet();
		return completions;
	}


	// Get the number of iterations completed so far.
	// Threading: This function is thread-safe.

	public final int get_completions () {
		int completions = current_completions.get();
		return completions;
	}


	// Return true if loop iterations have not been completed.
	// We return true if prompt termination has been requested but no abort has occurred.
	// Note: In some applications, is_timeout() may return true even if all work has
	// been completed, for example, if the timeout request occurred while all threads
	// were executing their final iteration. If completion of all iterations indicates
	// work is complete, this may be a more appropriate function to use to check for
	// timeout.
	// Threading: Although this function is thread-safe, it is intended to be
	// accessed after all threads have terminated.

	public final boolean is_incomplete () {
		int loop_count = get_loop_count();
		int completions = get_completions();
		return completions < loop_count;
	}


	// Get the maximum amount of memory used, in bytes, or -1L if unknown.
	// Threading: This function is thread-safe.

	public final long get_max_used_memory () {
		long my_max_used_memory = max_used_memory.get();
		return my_max_used_memory;
	}


	// Update the maximum amount of memory used.
	// Returns the current amount of memory used.
	// Threading: This function is thread-safe.

	private final LongBinaryOperator bimax = new LongBinaryOperator() {
		@Override public long applyAsLong (long left, long right) {
			return Math.max (left, right);
		}
	};

	public final long update_max_used_memory () {
		long used_memory = SimpleUtils.get_used_memory_bytes();
		long new_max_used_memory = max_used_memory.accumulateAndGet (used_memory, bimax);
		return used_memory;
	}

//	public final long update_max_used_memory () {
//		final long used_memory = SimpleUtils.get_used_memory_bytes();
//		long new_max_used_memory = max_used_memory.updateAndGet (
//			new LongUnaryOperator() {
//				@Override public long applyAsLong (long operand) {
//					return Math.max (operand, used_memory);
//				}
//			}
//		);
//		return used_memory;
//	}




	//----- Services -----

	// Default progress message format.
	// Threads must treat this as read-only.

	private String def_pm_fmt;

	// Default value of default progress message format.

	public static final String DEF_DEF_PM_FMT = "Completed %C of %L steps in %E seconds";

	// A default format clients can use when threads are complete, with completions == loop count.

	public static final String DEF_COMPLETE_PM_FMT = "Completed all %L steps in %E seconds";


	// Set the default progress message format.
	// Parameters:
	//  pm_fmt = New progress message format, if null then restore the default.
	// Returns this object.
	// Threading: Cannot be called while threads are running (recommended procedure is to set format in constructor).

	public final SimpleThreadLoopHelper set_def_pm_fmt (String pm_fmt) {
		if (pm_fmt == null) {
			def_pm_fmt = DEF_DEF_PM_FMT;
		} else {
			def_pm_fmt = pm_fmt;
		}
		return this;
	}


	// Get the default progress message format.

	public final String get_def_pm_fmt () {
		return def_pm_fmt;
	}


	// Make a progress message.
	// Parameters:
	//  pm_fmt = Format for the progress message, if null then use the default format.
	// Returns a one-line message, not terminated by linefeed.
	// Threading: This function is thread-safe.
	//
	// The format string can contain the following escapes:
	//  %% = A single percent sign.
	//  %L = Loop count (number of iterations).
	//  %C = Number of completions.
	//  %P = Percent complete (whole number of percent).
	//  %E = Elapsed time in seconds (seconds and tenthe).
	//  %U = Memory usage.

	public String make_progress_message (String pm_fmt) {
		StringBuilder sb = new StringBuilder();

		final int loop_count = get_loop_count();
		final int completions = get_completions();
		long time_now = 0L;
		long elapsed_time = 0L;

		String fmt = pm_fmt;
		if (fmt == null) {
			fmt = def_pm_fmt;
		}

		// Loop over characters in format string

		boolean f_escape = false;

		int len = fmt.length();
		for (int i = 0; i < len; ++i) {

			// Get character

			char ch = fmt.charAt(i);

			// Parse escape codes

			if (f_escape) {
				f_escape = false;

				// Switch on character

				switch(ch) {

				// Single percent

				case '%':
					sb.append ('%');
					break;

				// Loop count

				case 'L':
					sb.append (loop_count);
					break;

				// Number of completions

				case 'C':
					sb.append (completions);
					break;

				// Percent complete

				case 'P':
					if (loop_count <= 0) {
						sb.append (100);
					} else {
						long l_loop_count = (long)loop_count;
						long l_completions = (long)completions;
						sb.append ((l_completions * 100L) / l_loop_count);
					}
					break;

				// Elapsed time in seconds and tenths

				case 'E':
					if (time_now == 0L) {
						time_now = System.currentTimeMillis();
						elapsed_time = time_now - thread_manager.get_start_time();
					}
					sb.append (String.format ("%d.%01d", elapsed_time / 1000L, (elapsed_time % 1000L) / 100L));
					break;

				// Memory usage

				case 'U':
					//sb.append (SimpleUtils.used_memory_string());
					sb.append (SimpleUtils.used_memory_to_string (update_max_used_memory()));
					break;

				// Anything else, just output the escape code

				default:
					sb.append ('%');
					sb.append (ch);
					break;
				}
			}

			// Character outside escape

			else {

				// Check for start of escape

				if (ch == '%') {
					f_escape = true;
				}

				// Otherwise, just output the Character

				else {
					sb.append (ch);
				}
			}
		}

		// If within escape, output the partial escape code

		if (f_escape) {
			sb.append ('%');
		}

		return sb.toString();
	}


	// Make a progress message, using default format.

	public String make_progress_message () {
		return make_progress_message (null);
	}


	// Run the loop.
	// Parameters:
	//  thread_target = The thread target.
	//  executor = The executor to use for launching the threads.
	//  begin_index = The beginning index of the loop, inclusive.
	//  end_index = The ending index of the loop, exclusive.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit, or 0L for immediate timeout.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// This function sets up the loop control variables and then launches threads.
	// It waits for all threads to terminate, writing progress messages if enabled.
	// This function does not write a final progress message.
	// After return, the caller should call is_abort() and then is_timeout() to
	// check if termination was due to a thread abort or timeout.

	public void run_loop (SimpleThreadTarget thread_target, AutoExecutorService executor,
		int begin_index, int end_index, long max_runtime, long progress_time)
	{

		// Validate parameters

		if (!( thread_target != null )) {
			throw new IllegalArgumentException ("SimpleThreadLoopHelper.run_loop: Null thread_target");
		}

		if (!( executor != null )) {
			throw new IllegalArgumentException ("SimpleThreadLoopHelper.run_loop: Null executor");
		}

		if (!( begin_index <= end_index )) {
			throw new IllegalArgumentException ("SimpleThreadLoopHelper.run_loop: Negative loop count, begin_index = " + begin_index + ", end_index = " + end_index);
		}

		// Set up loop control variables

		init_loop_bounds (begin_index, end_index);

		// Check for immediate timeout

		if (max_runtime == 0L) {
			thread_manager.launch_in_timeout();
			return;
		}

		// Launch the threads

		thread_manager.launch_threads (thread_target, executor);

		// Loop until terminated

		while (!( thread_manager.await_termination (max_runtime, progress_time) )) {

			// Display progress message

			System.out.println (make_progress_message ());
		}

		return;
	}


	// Run the loop.
	// Parameters:
	//  thread_target = The thread target.
	//  exec_timer = Execution timer, containing executor and the time nterval for progress messages.
	//  begin_index = The beginning index of the loop, inclusive.
	//  end_index = The ending index of the loop, exclusive.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit, or 0L for immediate timeout.
	// This function sets up the loop control variables and then launches threads.
	// It waits for all threads to terminate, writing progress messages if enabled.
	// This function does not write a final progress message.
	// After return, the caller should call is_abort() and then is_timeout() to
	// check if termination was due to a thread abort or timeout.

	public void run_loop (SimpleThreadTarget thread_target, SimpleExecTimer exec_timer,
		int begin_index, int end_index, long max_runtime)
	{
		AutoExecutorService executor = exec_timer.get_executor();
		long progress_time = exec_timer.get_progress_time();

		run_loop (thread_target, executor, begin_index, end_index, max_runtime, progress_time);

		return;
	}


	// Run the loop.
	// Parameters:
	//  thread_target = The thread target.
	//  exec_timer = Execution timer, containing executor and the time nterval for progress messages.
	//  begin_index = The beginning index of the loop, inclusive.
	//  end_index = The ending index of the loop, exclusive.
	// The maximum runtime is set equal to the remaining execution time from exec_timer.
	// This function sets up the loop control variables and then launches threads.
	// It waits for all threads to terminate, writing progress messages if enabled.
	// This function does not write a final progress message.
	// After return, the caller should call is_abort() and then is_timeout() to
	// check if termination was due to a thread abort or timeout.

	public void run_loop (SimpleThreadTarget thread_target, SimpleExecTimer exec_timer,
		int begin_index, int end_index)
	{
		AutoExecutorService executor = exec_timer.get_executor();
		long max_runtime = exec_timer.get_remaining_time();
		long progress_time = exec_timer.get_progress_time();

		run_loop (thread_target, executor, begin_index, end_index, max_runtime, progress_time);

		return;
	}




	//----- Construction -----


	// Clear the contents.

	public final void clear () {
		thread_manager.clear();
		my_begin_index = 0;
		my_end_index = 0;
		current_loop_index.set (0);
		current_completions.set (0);
		return;
	}


	// Default constructor.

	public SimpleThreadLoopHelper () {
		my_begin_index = 0;
		my_end_index = 0;
		def_pm_fmt = DEF_DEF_PM_FMT;
	}


	// Constructor also sets the progress message format.

	public SimpleThreadLoopHelper (String pm_fmt) {
		my_begin_index = 0;
		my_end_index = 0;
		if (pm_fmt == null) {
			def_pm_fmt = DEF_DEF_PM_FMT;
		} else {
			def_pm_fmt = pm_fmt;
		}
	}




	//----- Testing -----




	// Class to count the number of primes less than or equal to max_n.
	// (This is by far not the fastest algorithm, it is just a test of multithreading.)

	private static class PrimeCounterLoop implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper();

		// The number of primes found so far.

		private AtomicInteger prime_count = new AtomicInteger();;

		// Count a prime.
		// Threading: This function is thread-safe.

		private void add_prime () {
			prime_count.incrementAndGet();
			return;
		}

		// Get the number of primes found so far.
		// Threading: This function is thread-safe.

		private int get_prime_count () {
			return prime_count.get();
		}

		// Set the number of primes found so far to zero.
		// Threading: This function is thread-safe.

		private void init_prime_count () {
			prime_count.set (0);
			return;
		}

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {
		
			// If max_n is 1111, all threads throw an exception as a test

			if (loop_helper.get_end_index() == 1112) {
				throw new IllegalArgumentException ("Exception test, thread_number = " + thread_number);
			}
		
			// If the last 3 digits of max_n are 222, only thread 0 throws an exception as a test

			if (loop_helper.get_end_index() % 1000 == 223 && thread_number == 0) {
				throw new IllegalArgumentException ("Exception test, thread_number = " + thread_number);
			}

			// Loop until loop completed or prompt termination is requested

			for (int n = loop_helper.get_loop_index(); n >= 0; n = loop_helper.get_next_index()) {

				// Test if n is prime, slowly

				int factors = 0;
				for (int m = 2; m < n; ++m) {
					if (n % m == 0) {
						++factors;
					}
				}

				if (factors == 0) {
					add_prime();
				}
			}

			return;
		}

		// Calculate the number of primes, using multiple threads.
		// Parameters:
		//  max_n = Maximum value of n to check, can be 1111 to force exceptions to be thrown.
		//  num_threads = Number of threads to use, can be -1 to use default number of threads.
		//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
		//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
		// As special cases:
		//  If max_n is 1111, all threads throw an exception as a test.
		//  If the last 3 digits of max_n are 222, only thread 0 throws an exception as a test.
	
		public int calc_prime_count (int max_n, int num_threads, long max_runtime, long progress_time) {

			try (

				// Create the executor

				AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
			){

				// Display executor info

				System.out.println ();
				System.out.println (auto_executor.toString());
				System.out.println ();

				// Initialize the count of primes

				init_prime_count();

				// Run the loop

				loop_helper.run_loop (this, auto_executor, 2, max_n + 1, max_runtime, progress_time);

				// Check for thread abort

				if (loop_helper.is_abort()) {
					System.out.println ("Stopped because of thread abort");
					System.out.println (loop_helper.get_abort_message_string());
				}

				// Otherwise, check for timeout

				else if (loop_helper.is_timeout()) {
					System.out.println ("Stopped because of timeout");
				}

				// Otherwise, normal termination

				else {
					System.out.println ("Normal termination");
				}

				// Show final state
					
				System.out.println (loop_helper.make_progress_message ());
			}

			// Return the count of primes

			return get_prime_count();
		}

	}




	// Class to count the number of primes less than or equal to max_n, version 2.
	// (This is by far not the fastest algorithm, it is just a test of multithreading.)

	private static final String PCLV2_PM_FMT = "Completed %C of %L steps in %E seconds using %U";

	private static class PrimeCounterLoopV2 implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PCLV2_PM_FMT);

		// The number of primes found so far.

		private AtomicInteger prime_count = new AtomicInteger();;

		// Count a prime.
		// Threading: This function is thread-safe.

		private void add_prime () {
			prime_count.incrementAndGet();
			return;
		}

		// Get the number of primes found so far.
		// Threading: This function is thread-safe.

		private int get_prime_count () {
			return prime_count.get();
		}

		// Set the number of primes found so far to zero.
		// Threading: This function is thread-safe.

		private void init_prime_count () {
			prime_count.set (0);
			return;
		}

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {
		
			// If max_n is 1111, all threads throw an exception as a test

			if (loop_helper.get_end_index() == 1112) {
				throw new IllegalArgumentException ("Exception test, thread_number = " + thread_number);
			}
		
			// If the last 3 digits of max_n are 222, only thread 0 throws an exception as a test

			if (loop_helper.get_end_index() % 1000 == 223 && thread_number == 0) {
				throw new IllegalArgumentException ("Exception test, thread_number = " + thread_number);
			}

			// Loop until loop completed or prompt termination is requested

			for (int n = loop_helper.get_loop_index(); n >= 0; n = loop_helper.get_next_index()) {

				// Test if n is prime, slowly

				int factors = 0;
				for (int m = 2; m < n; ++m) {
					if (n % m == 0) {
						++factors;
					}
				}

				if (factors == 0) {
					add_prime();
				}
			}

			return;
		}

		// Calculate the number of primes, using multiple threads.
		// Parameters:
		//  exec_timer = Execution timer, from which we obtain executor, max runtime, and progress time.
		//  max_n = Maximum value of n to check, can be 1111 to force exceptions to be thrown.
		// As special cases:
		//  If max_n is 1111, all threads throw an exception as a test.
		//  If the last 3 digits of max_n are 222, only thread 0 throws an exception as a test.
	
		public int calc_prime_count (SimpleExecTimer exec_timer, int max_n) {

			// Display executor info

			System.out.println ();
			System.out.println (exec_timer.get_executor().toString());
			System.out.println ("exec_timer: " + exec_timer.toString());
			System.out.println ();

			// Initialize the count of primes

			init_prime_count();

			// Run the loop

			loop_helper.run_loop (this, exec_timer, 2, max_n + 1);

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println ("Stopped because of thread abort");
				System.out.println (loop_helper.get_abort_message_string());
			}

			// Otherwise, check for timeout

			else if (loop_helper.is_timeout()) {
				System.out.println ("Stopped because of timeout");
			}

			// Otherwise, normal termination

			else {
				System.out.println ("Normal termination");
			}

			// Show final state

			long the_max_used_memory = loop_helper.get_max_used_memory();
			if (the_max_used_memory < 0L) {
				System.out.println ("Maximum used memory = Unknown");
			} else {
				System.out.println ("Maximum used memory = " + SimpleUtils.used_memory_to_string (the_max_used_memory));
			}
					
			System.out.println (loop_helper.make_progress_message ());

			// Return the count of primes

			return get_prime_count();
		}

	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("SimpleThreadLoopHelper : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  max_n  num_threads  max_runtime  progress_time
		// Use multiple threads to count the number of primes <= max_n.
		// See PrimeCounterLoop.calc_prime_count() for parameter description.
		// Note: Same parameters as SimpleThreadManager test #1.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 4 additional arguments

			if (!( args.length == 5 )) {
				System.err.println ("SimpleThreadLoopHelper : Invalid 'test1' subcommand");
				return;
			}

			try {

				int max_n = Integer.parseInt (args[1]);
				int num_threads = Integer.parseInt (args[2]);
				long max_runtime = Long.parseLong (args[3]);
				long progress_time = Long.parseLong (args[4]);

				// Say hello

				System.out.println ("Counting primes");
				System.out.println ("max_n = " + max_n);
				System.out.println ("num_threads = " + num_threads);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Count primes

				PrimeCounterLoop prime_counter = new PrimeCounterLoop();

				int final_count = prime_counter.calc_prime_count (max_n, num_threads, max_runtime, progress_time);

				System.out.println ("final_count = " + final_count);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  max_n  num_threads  max_runtime  progress_time
		// Use multiple threads to count the number of primes <= max_n.
		// Uses version 2, which makes ues of SimpleExecTimer.
		// See PrimeCounterLoop.calc_prime_count() for parameter description.
		// Note: Same parameters as SimpleThreadManager test #1.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 4 additional arguments

			if (!( args.length == 5 )) {
				System.err.println ("SimpleThreadLoopHelper : Invalid 'test2' subcommand");
				return;
			}

			try {

				int max_n = Integer.parseInt (args[1]);
				int num_threads = Integer.parseInt (args[2]);
				long max_runtime = Long.parseLong (args[3]);
				long progress_time = Long.parseLong (args[4]);

				// Say hello

				System.out.println ("Counting primes, version 2");
				System.out.println ("max_n = " + max_n);
				System.out.println ("num_threads = " + num_threads);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Count primes

				try (
					AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
				) {

					SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);

					PrimeCounterLoopV2 prime_counter = new PrimeCounterLoopV2();

					int final_count = prime_counter.calc_prime_count (exec_timer, max_n);

					System.out.println ("final_count = " + final_count);

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("SimpleThreadLoopHelper : Unrecognized subcommand : " + args[0]);
		return;

	}

}
