package org.opensha.oaf.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Create an executor service that is shut down automatically at the end of a block.
 * Author: Michael Barall 01/30/2020.
 *
 * This class can be used in a try-with-resources to create an executor service
 * which is shut down at the end of the block.
 *
 * When closed, this class implements a two-stage shutdown of the executor service.
 * First it attempts an orderly shutdown (ExecutorService.shutdown).  If that does not
 * succeed in terminating the executor service, then it performs an abrupt shutdown
 * (ExecutorService.shutdownNow).  There are adjustable timeouts for the shutdown.
 */
public class AutoExecutorService implements AutoCloseable {

	//----- Parameters -----

	// The executor service, or null if not created.

	private ExecutorService executor_service;

	// The number of threads used to create the executor service.
	// If 0, the executor service has no fixed limit on number of threads.

	private int num_threads;

	// Timeout for the orderly shutdown, in milliseconds, must be > 0L or one of the special values below.

	private long timeout_orderly;

	// Timeout for the abrupt shutdown, in milliseconds, must be > 0L or one of the special values below.
	// Note: This is only used if termination is not verfied following the orderly shutdown;
	// which happens if timeout_orderly is AESTO_NO_SHUT, or if timeout_orderly > 0L
	// and termination does not occur within the timeout.
	// Note: Setting both timeout_orderly and timeout_abrupt to AESTO_NO_SHUT disables the
	// shutdown, so that the executor service can continue to be used after this object is closed.

	private long timeout_abrupt;

	// Special values for the shutdown timeouts.

	public static final long AESTO_FOREVER = -1L;		// perform shutdown and then wait forever until the executor terminates
	public static final long AESTO_NO_WAIT = -2L;		// perform shutdown and then continue with no wait, assuming it will succeed
	public static final long AESTO_NO_SHUT = -3L;		// do not issue the shutdown command




	//----- Shutdown -----


	// Wait for termination.
	// Parameters:
	//  executor = The executor service to use.
	//  timeout = Timout, in milliseconds, must be > 0L or one of the special values above.
	// Returns true if executor is known to be terminated, false if not.
	// Design note: The executor service is passed as a parameter because close() clears
	// executor_service before performing the shutdown.

	private static boolean wait_for_termination (ExecutorService executor, long timeout) {

		boolean f_term = false;

		// If shutdown was not done, then not terminated

		if (timeout == AESTO_NO_SHUT) {
			f_term = false;
		}
	
		// If no wait, assume success

		else if (timeout == AESTO_NO_WAIT) {
			f_term = true;
		}

		// If indefinite timeout, keep waiting until termination

		else if (timeout == AESTO_FOREVER) {
			f_term = false;
			while (!( f_term )) {
				try {
					f_term = executor.awaitTermination (86400000L, TimeUnit.MILLISECONDS);	// wait up to 1 day
				}
				catch (InterruptedException e) {
					f_term = false;
				}
			}
		}

		// Otherwise, wait up to the given amount of time

		else if (timeout > 0L) {
			long time_now = System.currentTimeMillis();
			long give_up_time = time_now + timeout;
			boolean f_waiting = true;
			while (f_waiting) {
				try {
					f_term = executor.awaitTermination (Math.max (10L, give_up_time - time_now), TimeUnit.MILLISECONDS);
					f_waiting = false;
				}
				catch (InterruptedException e) {
					time_now = System.currentTimeMillis();
					f_waiting = true;
				}
			}
		}

		// Otherwise, invalid timeout value

		else {
			throw new IllegalArgumentException ("AutoExecutorService.wait_for_termination: Invalid timeout: " + timeout);
		}

		// Return termination flag

		return f_term;
	}


	// Closing shuts down the executor service, if it is open.

	@Override
	public void close() {
		ExecutorService executor = executor_service;
		if (executor != null) {
		
			// Clear the executor_service so close is idempotent

			executor_service = null;

			// Perform orderly shutdown, unless the user has bypassed it

			if (timeout_orderly != AESTO_NO_SHUT) {
				executor.shutdown();
			}

			// Wait for termination

			boolean f_term = wait_for_termination (executor, timeout_orderly);

			// If not terminated ...

			if (!( f_term )) {

				// Perform abrupt shutdown, unless the user has bypassed it

				if (timeout_abrupt != AESTO_NO_SHUT) {
					executor.shutdownNow();
				}

				// Wait for termination

				f_term = wait_for_termination (executor, timeout_abrupt);
			}
		}

		return;
	}




	//----- Construction -----


	// Return true if a timeout value is valid.

	private static boolean is_valid_timeout (long timeout) {
		if (   timeout > 0L
			|| timeout == AESTO_FOREVER
			|| timeout == AESTO_NO_WAIT
			|| timeout == AESTO_NO_SHUT
		) {
			return true;
		}
		return false;
	}


	// Make an object holding an executor.
	// Parameters:
	//  num_threads = Number of threads to use for executor:
	//    > 1 = Use a pool with a fixed number of threads.
	//    1 = Use a single background thread.
	//    0 = use a pool that caches an unlimited number of threads.
	//  timeout_orderly = Timeout for orderly shutdown, in milliseconds; must be > 0L or one of the special values above.
	//  timeout_abrupt = Timeout for abrupt shutdown, in milliseconds; must be > 0L or one of the special values above.

	public AutoExecutorService (int num_threads, long timeout_orderly, long timeout_abrupt) {

		// Save and validate parameters

		this.executor_service = null;
		this.num_threads = num_threads;
		this.timeout_orderly = timeout_orderly;
		this.timeout_abrupt = timeout_abrupt;

		if (!( num_threads >= 0 )) {
			throw new IllegalArgumentException ("AutoExecutorService: Invalid number of threads: " + num_threads);
		}

		if (!( is_valid_timeout (timeout_orderly) )) {
			throw new IllegalArgumentException ("AutoExecutorService: Invalid orderly shutdown timeout: " + timeout_orderly);
		}

		if (!( is_valid_timeout (timeout_abrupt) )) {
			throw new IllegalArgumentException ("AutoExecutorService: Invalid abrupt shutdown timeout: " + timeout_abrupt);
		}

		// Create the executor

		if (this.num_threads == 0) {
			
			// Executor with unlimited number of threads

			executor_service = Executors.newCachedThreadPool ();

		} else if (this.num_threads == 1) {
		
			// Executor with a single background thread

			executor_service = Executors.newSingleThreadExecutor ();

		} else {
		
			// Executor with fixed size thread pool

			executor_service = Executors.newFixedThreadPool (this.num_threads);

		}
	}


	// Change the timeouts.
	// Parameters:
	//  the_timeout_orderly = Timeout for orderly shutdown, in milliseconds; must be > 0L or one of the special values above.
	//  the_timeout_abrupt = Timeout for abrupt shutdown, in milliseconds; must be > 0L or one of the special values above.

	public void set_timeouts (long the_timeout_orderly, long the_timeout_abrupt) {

		// Validate parameters

		if (!( is_valid_timeout (the_timeout_orderly) )) {
			throw new IllegalArgumentException ("AutoExecutorService.set_timeouts: Invalid orderly shutdown timeout: " + the_timeout_orderly);
		}

		if (!( is_valid_timeout (the_timeout_abrupt) )) {
			throw new IllegalArgumentException ("AutoExecutorService.set_timeouts: Invalid abrupt shutdown timeout: " + the_timeout_abrupt);
		}

		// Save values

		this.timeout_orderly = the_timeout_orderly;
		this.timeout_abrupt = the_timeout_abrupt;

		return;
	}




	//----- Access -----


	// Get the executor service.

	public ExecutorService get_executor () {
		return executor_service;
	}


	// Get the configured number of threads.

	public int get_num_threads () {
		return num_threads;
	}

	
	// Get the configured number of threads.
	// If the configured number of threads is 0 (meaning no fixed limit),
	// then return the supplied default number of threads.

	public int get_num_threads (int default_num_threads) {
		if (num_threads == 0) {
			return default_num_threads;
		}
		return num_threads;
	}

	
	// Get the configured number of threads.
	// If the configured number of threads is 0 (meaning no fixed limit),
	// then return the supplied default number of threads.
	// If the configured number of threads is greater then the supplied
	// maximum number, then return the supplied maximum number.

	public int get_num_threads (int default_num_threads, int max_num_threads) {
		if (num_threads == 0) {
			return default_num_threads;
		}
		if (num_threads > max_num_threads) {
			return max_num_threads;
		}
		return num_threads;
	}


	// Get the timeout for orderly shutdown, in milliseconds; or one of the special values above.

	public long get_timeout_orderly () {
		return timeout_orderly;
	}


	// Get the timeout for abrupt shutdown, in milliseconds; or one of the special values above.

	public long get_timeout_abrupt () {
		return timeout_abrupt;
	}

}
