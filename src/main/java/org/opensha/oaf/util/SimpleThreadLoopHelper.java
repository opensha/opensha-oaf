package org.opensha.oaf.util;

import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


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


	// Initialize loop control variables, and specify the loop index bounds.
	// Threading: This function should be called before launching threads.

	public final void init_loop_bounds (int begin_index, int end_index) {
		my_begin_index = begin_index;
		my_end_index = end_index;
		current_loop_index.set (begin_index);
		current_completions.set (0);
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
	}




	//----- Services -----


	// Make a progress message.
	// Parameters:
	//  f_last = True if this is expected to be the last progress message.
	// Returns a one-line message, not terminated by linefeed.
	// Threading: This function is thread-safe.

	public String make_progress_message (boolean f_last) {
		long elapsed_time = System.currentTimeMillis() - thread_manager.get_start_time();
		String s_elapsed_time = String.format ("%.1f", ((double)elapsed_time)/1000.0);
		int loop_count = get_loop_count();
		int completions = get_completions();
		String message;
		if (f_last && completions > 0 && completions == loop_count) {
			message = "Completed all " + loop_count + " steps in " + s_elapsed_time + " seconds";
		} else {
			message = "Completed " + completions + " of " + loop_count + " steps in " + s_elapsed_time + " seconds";
		}
		return message;
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

			System.out.println (make_progress_message (false));
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
					
				System.out.println (loop_helper.make_progress_message (true));
			}

			// Return the count of primes

			return get_prime_count();
		}

	}




	// Class to count the number of primes less than or equal to max_n, version 2.
	// (This is by far not the fastest algorithm, it is just a test of multithreading.)

	private static class PrimeCounterLoopV2 implements SimpleThreadTarget {

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
					
			System.out.println (loop_helper.make_progress_message (true));

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
