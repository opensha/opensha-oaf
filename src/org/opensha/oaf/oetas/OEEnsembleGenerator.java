package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.AutoCountDownLatch;
import org.opensha.oaf.util.AutoCleanup;
import org.opensha.oaf.util.SimpleUtils;


// Class for generating an ensemble of operational ETAS catalog.
// Author: Michael Barall 02/01/2020.
//
// This is a multi-threaded catalog ensemble generator.  Each catalog is
// seeded, generated, scanned, accumulated, and then discarded.  Multiple
// threads permit multiple catalogs to be generated simultaneously.

public class OEEnsembleGenerator {

	//----- Parameters -----

	// Note: Parameters cannot be modified while threads are running.

	// The ensemble parameters.

	private OEEnsembleParams ensemble_params;

	// The selected number of threads.

	private int num_threads;

	// The time that threads were launched, in milliseconds since the epoch.

	private long start_time;


	// Get the ensemble parameters.

	public OEEnsembleParams get_ensemble_params () {
		return ensemble_params;
	}

	// Get the selected number of threads.

	public int get_num_threads () {
		return num_threads;
	}

	// Get the time that threads were launched, in milliseconds since the epoch.

	public long get_start_time () {
		return start_time;
	}




	//----- Thread communication -----

	// The countdown latch used for thread termination.

	private CountDownLatch count_down_latch;

	// The number of catalogs generated so far.

	private int catalog_count;

	// Flag that can be set to request termination of all threads as soon as possible.

	private boolean req_termination;

	// List of thread abort messages.

	private ArrayList<String> abort_messages;


	// Get the next work unit.
	// If there is more work to do, return the current catalog number and increment the catalog count.
	// If there is no more work to do, return -1.

	private synchronized int get_work_unit () {
		if (req_termination || catalog_count >= ensemble_params.num_catalogs) {
			return -1;
		}
		int result = catalog_count;
		++catalog_count;
		return result;
	}


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

	private synchronized void add_abort_message (String message) {
		abort_messages.add (message);
		return;
	}


	// Display all the abort messages.
	// Performs no operation if there are no abort messages.

	public synchronized void display_abort_messages () {
		int count = abort_messages.size();
		for (int n = 0; n < count; ++n) {
			System.out.println (abort_messages.get (n));
			System.out.println ();
		}
		return;
	}




	//----- Construction -----




	// Erase the contents.

	public void clear () {
		ensemble_params = null;
		num_threads = 0;
		start_time = 0L;

		count_down_latch = null;
		catalog_count = 0;
		req_termination = false;
		abort_messages = null;
		return;
	}




	// Default constructor.

	public OEEnsembleGenerator () {
		clear();
	}




	//----- Generator thread -----




	// One thread in the generator.

	private class GeneratorThread implements Runnable {

		// The thread number.

		private int thread_number;


		// Construct the thread and set the thread number.

		public GeneratorThread (int thread_number) {
			this.thread_number = thread_number;
		}
	

		// Run the generator thread.

		@Override
		public void run() {

			// Always advance the count down latch on thread termination

			try (
				AutoCountDownLatch auto_count_down_latch = new AutoCountDownLatch (count_down_latch);
			){

				// Generate the catalogs

				try {
					gen_catalogs();
				}

				// Catch all exceptions 

				catch (Exception e) {
					add_abort_message (
						"Aborting OEEnsembleGenerator thread number " + thread_number + "\n"
						+ SimpleUtils.getStackTraceAsString (e)
					);
					//System.out.println ("Exception from OEEnsembleGenerator thread number " + thread_number);
					//e.printStackTrace();
				}
				catch (Throwable e) {
					add_abort_message (
						"Aborting OEEnsembleGenerator thread number " + thread_number + "\n"
						+ SimpleUtils.getStackTraceAsString (e)
					);
					//System.out.println ("Exception from OEEnsembleGenerator thread number " + thread_number);
					//e.printStackTrace();
				}
			}

			return;
		}


		// Generate the catalogs.

		private void gen_catalogs () {

			// Get the random number generator

			OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

			// Create a seeder for our initializer, which we re-use for each catalog

			OECatalogSeeder seeder = ensemble_params.initializer.make_seeder();

			// Allocate a seeder communication area, which we re-use for each catalog

			OECatalogSeedComm seed_comm = new OECatalogSeedComm();

			// Create a scanner for our accumulators, which we re-use for each catalog
			// (the scanner creates the consumers and the scan communication area)

			OECatalogScanner cat_scanner = new OECatalogScanner();
			cat_scanner.setup (ensemble_params.accumulators);

			// Allocate the storage (which is also the builder), which we re-use for each catalog

			OECatalogStorage cat_storage = new OECatalogStorage();

			// Allocate a generator, which we re-use for each catalog

			OECatalogGenerator cat_generator = new OECatalogGenerator();

			// Loop over number of catalogs ...

			for (int ncat = get_work_unit(); ncat >= 0; ncat = get_work_unit()) {

				// Set up the seeder communication area

				seed_comm.setup_seed_comm (cat_storage, rangen);

				// Open the seeder

				seeder.open();

				// Seed the catalog

				seeder.seed_catalog (seed_comm);

				// Close the seeder

				seeder.close();

				// Set up the catalog generator
				
				cat_generator.setup (rangen, cat_storage, false);

				// Calculate all generations and end the catalog

				cat_generator.calc_all_gen();

				// Tell the generator to forget the catalog

				cat_generator.forget();
		
				// Open the consumers

				cat_scanner.open();

				// Scan the catalog

				cat_scanner.scan (cat_storage, rangen);

				// Close the consumers

				cat_scanner.close();
			}

		return;
		}

	}




	//----- Service functions -----




	// Perform pre-launch operations.
	// Parameters:
	//  the_ensemble_params = The ensemble parameters.
	// This must be called before launching threads.

	public void pre_launch (OEEnsembleParams the_ensemble_params) {

		// Validate parameters

		if (!( the_ensemble_params.num_catalogs > 0 )) {
			throw new IllegalArgumentException ("OEEnsembleGenerator.pre_launch: Invalid number of catalogs: " + the_ensemble_params.num_catalogs);
		}

		// Save the parameters

		ensemble_params = the_ensemble_params;

		// Begin initialization

		ensemble_params.initializer.begin_initialization();

		// Begin accumulation, passing the number of catalogs to each accumulator

		for (OEEnsembleAccumulator accumulator : ensemble_params.accumulators) {
			accumulator.begin_accumulation (ensemble_params.num_catalogs);
		}
	
		return;
	}




	// Launch the threads.
	// Parameters:
	//  executor = The executor to use for launching the threads.
	//  the_num_threads = The number of threads to use, must be > 0.
	// This function launches one or more background threads to generate the catalogs.

	public void launch_threads (Executor executor, int the_num_threads) {

		// Validate parameters

		if (!( the_num_threads > 0 )) {
			throw new IllegalArgumentException ("OEEnsembleGenerator.launch_threads: Invalid number of threads: " + the_num_threads);
		}

		// Save the parameters

		num_threads = the_num_threads;

		// Record the start time

		start_time = System.currentTimeMillis();

		// Initialize communication variables

		count_down_latch = new CountDownLatch (num_threads);
		catalog_count = 0;
		req_termination = false;
		abort_messages = new ArrayList<String>();

		// Create and launch the threads

		for (int thread_number = 0; thread_number < num_threads; ++thread_number) {
			executor.execute (new GeneratorThread (thread_number));
		}

		return;
	}




	// Get the number of catalogs processed or being processed.
	// This may be called while running to monitor progress,
	// or after termination to obtain the number of catalogs generated.

	public synchronized int get_catalog_count () {
		return catalog_count;
	}




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




	// Wait for all threads to terminate, with maximum runtime.
	// Parameters:
	//  max_runtime = Maximum runtime requested, in milliseconds, can be 0L for no limit.
	// Returns true for normal termination, false if prompt termination was requested.
	// If normal termination has not occurred within the maximum runtime
	// (counted from the start time), then prompt termination is requested.
	// The function does not return until all threads have terminated.

	//  public boolean await_termination (long max_runtime) {
	//  
	//  	// If no time limit, just wait indefinitely
	//  
	//  	if (max_runtime == 0L) {
	//  		await_termination();
	//  		return true;
	//  	}
	//  
	//  	// Wait for normal termination
	//  
	//  	boolean f_terminated = false;
	//  	boolean f_waiting = true;
	//  	while (f_waiting) {
	//  		try {
	//  			long time_now = System.currentTimeMillis();
	//  			long timeout = Math.max (10L, start_time + max_runtime - time_now);
	//  			f_terminated = count_down_latch.await (timeout, TimeUnit.MILLISECONDS);
	//  			f_waiting = false;
	//  		}
	//  		catch (InterruptedException e) {
	//  			f_waiting = true;
	//  		}
	//  	}
	//  
	//  	// If already terminated, return normal termination
	//  
	//  	if (f_terminated) {
	//  		return true;
	//  	}
	//  
	//  	// Request prompt termination
	//  
	//  	request_termination();
	//  
	//  	// Wait for termination
	//  
	//  	f_waiting = true;
	//  	while (f_waiting) {
	//  		try {
	//  			count_down_latch.await ();
	//  			f_waiting = false;
	//  		}
	//  		catch (InterruptedException e) {
	//  			f_waiting = true;
	//  		}
	//  	}
	//  
	//  	// Return prompt termination
	//  
	//  	return false;
	//  }




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




	// Perform post-termination operations.
	// This must be called after all threads are terminated to finish accumulation.

	public void post_termination () {

		// End initialization

		ensemble_params.initializer.end_initialization();

		// End accumulation

		for (OEEnsembleAccumulator accumulator : ensemble_params.accumulators) {
			accumulator.end_accumulation ();
		}

		return;
	}




	// Generate all the catalogs.
	// Parameters:
	//  the_ensemble_params = The ensemble parameters.
	//  executor = The executor to use for launching the threads.
	//  the_num_threads = The number of threads to use, must be > 0.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// This combines the function of pre_launch, launch_threads, await_termination, and post_termination.

	public void generate_all_catalogs (OEEnsembleParams the_ensemble_params, Executor executor, int the_num_threads, long max_runtime, long progress_time) {
	
		// Pre-launch operations

		pre_launch (the_ensemble_params);

		// Launch the threads

		launch_threads (executor, the_num_threads);

		// Loop until terminated

		boolean f_terminated = false;
		while (!( f_terminated )) {

			// Wait for termination, or polling timeout

			f_terminated = await_termination (max_runtime, progress_time);

			// Display progress message if desired

			if (progress_time >= 0L) {
				long elapsed_time = System.currentTimeMillis() - start_time;
				String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
				if (f_terminated) {
					if (get_req_termination()) {
						System.out.println ("Reached runtime limit after generating " + get_catalog_count() + " ETAS catalogs in " + s_elapsed_time + " seconds");
					} else {
						System.out.println ("Finished generating " + get_catalog_count() + " ETAS catalogs in " + s_elapsed_time + " seconds");
					}
				} else {
					System.out.println ("Generating " + get_catalog_count() + " ETAS catalogs so far in " + s_elapsed_time + " seconds");
				}
			}
		}

		// Post-termination operations

		post_termination();

		return;
	}




	// Generate all the catalogs.
	// Parameters:
	//  the_ensemble_params = The ensemble parameters.
	//  the_num_threads = The number of threads to use, must be > 0.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// This combines the function of pre_launch, launch_threads, await_termination, and post_termination.
	// This version creates the executor.

	public void generate_all_catalogs (OEEnsembleParams the_ensemble_params, int the_num_threads, long max_runtime, long progress_time) {

		try (

			// Create the executor

			AutoExecutorService auto_executor = new AutoExecutorService (the_num_threads, 30000L, AutoExecutorService.AESTO_NO_WAIT);
		){

			// Generate catalogs
		
			generate_all_catalogs (the_ensemble_params, auto_executor.get_executor(), the_num_threads, max_runtime, progress_time);
		}

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEEnsembleGenerator : Missing subcommand");
			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEEnsembleGenerator : Unrecognized subcommand : " + args[0]);
		return;

	}

}
