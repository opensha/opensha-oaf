package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


// Class for managing a simple pool of threads.
// Author: Michael Barall 11/06/2020.
//
// This provides common management functions for a pool of threads.
//
// It is intended for an application where all threads call a multi-threaded
// function in a single target object, which implements SimpleThreadTarget.
// This design relieves the user from having to create and manage multiple
// Runnable objects.
//
// Typically, each thread executes a series of work units, which the threads
// obtain from a shared queue of work units.  Threads terminate when there
// are no more work units.
//
// There is a mechanism to request that all threads terminate as promptly as
// possible, even if there are more work units remaining.  Typically, each
// thread checks for the prompt termination request before starting a work
// unit.  This mechanism can be used to implement a timeout.
//
// There is a mechanism for threads to indicate that an abort has occurred,
// and provide an abort message.  This mechanism is invoked automatically if
// a thread throws an exception (instead of terminating normally).  When
// one thread aborts, all other threads are requested to terminate promptly.
//
// There is a mechanism for the originating thread to wait for all the
// worker threads to terminate.  The originating thread can specify a
// maximum run time, and a maximum wait time.  The latter makes it possible
// to display progress messages while the worker threads are running.  The
// former makes it possible to limit the total running time, although when
// the timeout occurs typically each thread will complete its current work
// unit before terminating.

public class SimpleThreadManager {

	//----- Thread communication -----

	// The number of threads that were launched.
	// This is set when threads are launched.
	// It can be accessed (but not changed) while threads are running.

	private int num_threads;

	// The time that threads were launched, in milliseconds since the epoch.
	// This is set when threads are launched.
	// It can be accessed (but not changed) while threads are running.

	private long start_time;

	// The countdown latch used for thread termination.
	// This is set when threads are launched.
	// It can be accessed (but not changed) while threads are running.

	private CountDownLatch count_down_latch;

	// Flag that can be set to request termination of all threads as soon as possible.
	// After launching threads, access to this must be synchronized.
	// It can be accessed simultaneously by multiple threads.

	private boolean req_termination;

	// List of thread abort messages.
	// Each message ends with a linefeed.
	// After launching threads, access to this must be synchronized.
	// It can be accessed simultaneously by multiple threads.

	private ArrayList<String> abort_messages;


	//----- Information retrieval -----


	// Get the number of threads that were launched.

	public int get_num_threads () {
		return num_threads;
	}


	// Get the time that threads were launched, in milliseconds since the epoch.

	public long get_start_time () {
		return start_time;
	}


	// Return true if thread abort has occurred.
	// If this returns true, then abort messages are available.

	public synchronized boolean is_abort () {
		return !(abort_messages.isEmpty());
	}


	// Return true if timeout has been requested.
	// We return true if prompt termination has been requested but no abort has occurred.

	public synchronized boolean is_timeout () {
		return req_termination && abort_messages.isEmpty();
	}


	//----- Termination requests -----


	// Set a request to terminate all threads promptly.

	public synchronized void request_termination () {
		req_termination = true;
		return;
	}


	// Return true if prompt termination has been requested.
	// This may be called while running to monitor progress,
	// or after termination to check if prompt termination occurred.

	public synchronized boolean get_req_termination () {
		return req_termination;
	}


	//----- Abort messages -----


	// Get the number of abort messages.
	// If nonzero, it means an exception occurred in one or more threads.

	public synchronized int get_abort_message_count () {
		return abort_messages.size();
	}


	// Get the n-th abort message.

	public synchronized String get_abort_message (int n) {
		return abort_messages.get (n);
	}


	// Add an abort message.
	// If the message does not end in a linefeed, then a linefeed is appended.
	// This also sets a request to terminate all threads promptly.

	public synchronized void add_abort_message (String message) {
		if (message == null) {
			abort_messages.add ("<null message>\n");
		} else if (message.trim().isEmpty()) {
			abort_messages.add ("<empty message>\n");
		} else if (message.endsWith ("\n")) {
			abort_messages.add (message);
		} else {
			abort_messages.add (message + "\n");
		}
		req_termination = true;
		return;
	}


	// Add an abort message, constructed from the given exception.
	// If the message does not end in a linefeed, then a linefeed is appended.
	// This also sets a request to terminate all threads promptly.

	public void add_abort_message (Throwable e) {
		add_abort_message (SimpleUtils.getStackTraceAsString (e));
		return;
	}


	// Get a copy of the list of abort messages.

	public synchronized List<String> get_abort_message_list () {
		return new ArrayList<String> (abort_messages);
	} 


	// Get the abort messages as a single string.

	public String get_abort_message_string () {
		List<String> messages = get_abort_message_list();
		int count = messages.size();
		StringBuilder result = new StringBuilder();
		for (int n = 0; n < count; ++n) {
			if (n > 0) {
				result.append ("\n");
			}
			result.append (messages.get (n));
		}
		return result.toString();
	}


	// Display all the abort messages on System.out.
	// Performs no operation if there are no abort messages.

	public void display_abort_messages () {
		List<String> messages = get_abort_message_list();
		int count = messages.size();
		for (int n = 0; n < count; ++n) {
			System.out.println (messages.get (n));
		}
		return;
	}




	//----- Construction -----




	// Erase the contents.

	public void clear () {
		num_threads = 0;
		start_time = 0L;
		count_down_latch = null;
		req_termination = false;
		abort_messages = new ArrayList<String>();
		return;
	}




	// Default constructor.

	public SimpleThreadManager () {
		clear();
	}




	//----- Worker thread -----




	// One thread in the pool.

	private class WorkerThread implements Runnable {

		// The thread number.

		private int thread_number;

		// The thread target.
		// Design note: The thread target is stored in each WorkerThread, rather
		// than in a single variable in the enclosing SimpleThreadManager, so that
		// when all threads have terminated there is no longer any reference to
		// the SimpleThreadTarget object.

		private SimpleThreadTarget thread_target;


		// Construct the thread and set the thread number and target.

		public WorkerThread (int thread_number, SimpleThreadTarget thread_target) {
			this.thread_number = thread_number;
			this.thread_target = thread_target;
		}
	

		// Run the worker thread.

		@Override
		public void run() {

			// Always advance the count down latch on thread termination

			try (
				AutoCountDownLatch auto_count_down_latch = new AutoCountDownLatch (count_down_latch);
			){

				// Ensure that no reference to the thread target is retained

				SimpleThreadTarget my_thread_target = thread_target;
				thread_target = null;

				// Call the target

				try {
					my_thread_target.thread_entry (SimpleThreadManager.this, thread_number);
				}

				// Catch all exceptions 

				catch (Exception e) {
					add_abort_message (
						"Aborting SimpleThreadManager thread number " + thread_number + "\n"
						+ SimpleUtils.getStackTraceAsString (e)
					);
				}
				catch (Throwable e) {
					add_abort_message (
						"Aborting SimpleThreadManager thread number " + thread_number + "\n"
						+ SimpleUtils.getStackTraceAsString (e)
					);
				}
			}

			return;
		}

	}




	//----- Service functions -----




	// Launch the threads.
	// Parameters:
	//  thread_target = The thread target.
	//  executor = The executor to use for launching the threads.
	//  the_num_threads = The number of threads to use, must be > 0.
	// This function launches one or more background threads.

	public void launch_threads (SimpleThreadTarget thread_target, Executor executor, int the_num_threads) {

		// Validate parameters

		if (!( thread_target != null )) {
			throw new IllegalArgumentException ("SimpleThreadManager.launch_threads: Null thread_target");
		}

		if (!( executor != null )) {
			throw new IllegalArgumentException ("SimpleThreadManager.launch_threads: Null executor");
		}

		if (!( the_num_threads > 0 )) {
			throw new IllegalArgumentException ("SimpleThreadManager.launch_threads: Invalid number of threads: " + the_num_threads);
		}

		// Save the parameters

		num_threads = the_num_threads;

		// Record the start time

		start_time = System.currentTimeMillis();

		// Initialize communication variables

		count_down_latch = new CountDownLatch (num_threads);
		req_termination = false;
		abort_messages = new ArrayList<String>();

		// Create and launch the threads

		for (int thread_number = 0; thread_number < num_threads; ++thread_number) {
			executor.execute (new WorkerThread (thread_number, thread_target));
		}

		return;
	}




	// Launch the threads.
	// Parameters:
	//  thread_target = The thread target.
	//  executor = The executor to use for launching the threads.
	// This function launches one or more background threads.
	// The number of threads is the configured number of threads in the executor.

	public void launch_threads (SimpleThreadTarget thread_target, AutoExecutorService executor) {

		// Validate parameters

		if (!( thread_target != null )) {
			throw new IllegalArgumentException ("SimpleThreadManager.launch_threads: Null thread_target");
		}

		if (!( executor != null )) {
			throw new IllegalArgumentException ("SimpleThreadManager.launch_threads: Null executor");
		}

		// Pass thru

		launch_threads (thread_target, executor.get_executor(), executor.get_num_threads());
		return;
	}




	// Check if all the threads have terminated.
	// Returns true if terminated, false if not.

	public boolean is_terminated () {
		boolean f_terminated = false;
		boolean f_waiting = true;
		while (f_waiting) {
			try {

				// Call with zero timeout always returns immediately

				f_terminated = count_down_latch.await (0L, TimeUnit.MILLISECONDS);
				f_waiting = false;
			}
			catch (InterruptedException e) {
				f_waiting = true;
			}
		}
		return f_terminated;
	}




	// Wait indefinitely for all threads to terminate.

	public void await_termination () {
		boolean f_waiting = true;
		while (f_waiting) {
			try {
				count_down_latch.await ();
				f_waiting = false;
			}
			catch (InterruptedException e) {
				f_waiting = true;
			}
		}
		return;
	}




	// Wait for all threads to terminate, with maximum runtime and maximum wait time.
	// Parameters:
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  max_waittime = Maximum time this function will wait before returning, in milliseconds, can be -1L for no limit.
	// Returns true if all threads have terminated, false if not.
	// If normal termination has not occurred within the maximum runtime
	// (counted from the start time), then prompt termination is requested.
	// Note: If max_waittime == -1L then this function always waits for termination and returns true.
	// Note: If max_wait_time == 0L then this function always returns immediately, after checking if
	// the threads are terminated and checking if it is time to begin prompt termination.
	// Note: If threads have been terminated, then the type of termination can be determined as follows:
	//   If is_abort() is true, then one or more threads aborted, and there are abort messages available.
	//   Otherwise, if is_timeout() is true, then threads were requested to terminate promptly, probably due to a timeout.
	//   Otherwise, all threads terminated normally.

	public boolean await_termination (long max_runtime, long max_waittime) {

		// Wait loop

		boolean f_waiting = true;					// true if we are waiting
		boolean f_terminated = false;				// true if threads have terminated
		long time_now = System.currentTimeMillis();	// the current time, as of the start of the wait loop
		long call_time = time_now;					// the time that this function was called

		while (f_waiting) {
			try {

				// The timeout for the countdown latch, we use -1L to indicate none

				long timeout = -1L;
				boolean f_runtime = false;			// true if the current timeout is for max_runtime

				// If there is a maximum waiting time, apply it

				if (max_waittime >= 0L) {
					timeout = Math.max (0L, call_time + max_waittime - time_now);
				}

				// If there is a maximum runtime, and prompt termination is not requested yet ...

				if (max_runtime >= 0L && !(get_req_termination())) {

					// Possible timeout for runtime

					long runtime_timeout = Math.max (0L, start_time + max_runtime - time_now);

					// If the current timeout will be for runtime, apply it

					if (timeout < 0L || runtime_timeout <= timeout) {
						timeout = runtime_timeout;
						f_runtime = true;
					}
				}

				// Check for termination, with requested timeout

				if (timeout >= 0L) {
					f_terminated = count_down_latch.await (timeout, TimeUnit.MILLISECONDS);
				} else {
					count_down_latch.await();
					f_terminated = true;
				}

				// If terminated, stop waiting

				if (f_terminated) {
					f_waiting = false;
				}

				// Otherwise, if we just reached maximum runtime, request prompt termination

				else if (f_runtime) {
					request_termination();
					f_waiting = true;
				}

				// Otherwise, we just reached maximum wait time, so stop waiting

				else {
					f_waiting = false;
				}
			}
			catch (InterruptedException e) {

				// On interrupt, keep waiting

				f_waiting = true;
			}

			// If still waiting, update time for next loop iteration

			if (f_waiting) {
				time_now = System.currentTimeMillis();
			}
		}

		// Return termination flag

		return f_terminated;
	}




	//----- Testing -----




	// Class to count the number of primes less than max_n.
	// (This is by far not the fastest algorithm, it is just a test of multithreading.)

	private static class PrimeCounter implements SimpleThreadTarget {

		// The maximum n.

		private int max_n;

		// The n currently being tested.

		private int current_n;

		// The number of primes found so far.

		private int prime_count;

		// Get the next work unit, which is the value of n to test for primality.
		// Returns 0 if no more work.

		private synchronized int get_work_unit () {
			if (current_n >= max_n) {
				return 0;
			}
			++current_n;
			return current_n;
		}

		// Count a prime.

		private synchronized void add_prime () {
			++prime_count;
			return;
		}

		// Get the n currently being tested.

		private synchronized int get_current_n () {
			return current_n;
		}

		// Get the number of primes found so far.

		private synchronized int get_prime_count () {
			return prime_count;
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
		
			// If max_n is zero, throw an exception as a test

			if (max_n == 0) {
				throw new IllegalArgumentException ("Exception test, thread_number = " + thread_number);
			}

			// Loop until prompt termination is requested

			while (!( thread_manager.get_req_termination() )) {

				// Get next work unit, end loop if none

				int n = get_work_unit();
				if (n == 0) {
					break;
				}

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

		// Show the current state.

		private void show_state (long start_time) {
			long elapsed_time = System.currentTimeMillis() - start_time;
			String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
			System.out.println ("Time = " + s_elapsed_time + ", current_n = " + get_current_n() + ", prime_count = " + get_prime_count());
			return;
		}

		// Calculate the number of primes, using multiple threads.
		// Parameters:
		//  max_n = Maximum value of n to check, can be 0 to force exceptions to be thrown.
		//  num_threads = Number of threads to use, can be -1 to use default number of threads.
		//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
		//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	
		public int calc_prime_count (int max_n, int num_threads, long max_runtime, long progress_time) {

			try (

				// Create the executor

				AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
			){

				// Display executor info

				System.out.println ();
				System.out.println (auto_executor.toString());
				System.out.println ();

				// Initialize variables

				this.max_n = max_n;
				this.current_n = 1;
				this.prime_count = 0;

				// Make the thread manager

				SimpleThreadManager thread_manager = new SimpleThreadManager();

				// Launch the threads

				thread_manager.launch_threads (this, auto_executor);

				// Get the start time

				long start_time = thread_manager.get_start_time();

				// Loop until terminated

				while (!( thread_manager.await_termination (max_runtime, progress_time) )) {

					// Display progress message

					show_state (start_time);
				}

				// Check for thread abort

				if (thread_manager.is_abort()) {
					System.out.println ("Stopped because of thread abort");
					System.out.println (thread_manager.get_abort_message_string());
				}

				// Otherwise, check for timeout

				else if (thread_manager.is_timeout()) {
					System.out.println ("Stopped because of timeout");
				}

				// Otherwise, normal termination

				else {
					System.out.println ("Normal termination");
				}

				// Show final state
					
				show_state (start_time);
			}

			// Return the count of primes

			return get_prime_count();
		}

	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("SimpleThreadManager : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  max_n  num_threads  max_runtime  progress_time
		// Use multiple threads to count the number of primes <= max_n.
		// See PrimeCounter.calc_prime_count() for parameter description.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 4 additional arguments

			if (!( args.length == 5 )) {
				System.err.println ("SimpleThreadManager : Invalid 'test1' subcommand");
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

				PrimeCounter prime_counter = new PrimeCounter();

				int final_count = prime_counter.calc_prime_count (max_n, num_threads, max_runtime, progress_time);

				System.out.println ("final_count = " + final_count);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("SimpleThreadManager : Unrecognized subcommand : " + args[0]);
		return;

	}

}
