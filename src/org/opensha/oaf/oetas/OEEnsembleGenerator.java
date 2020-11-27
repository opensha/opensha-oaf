package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;


// Class for generating an ensemble of operational ETAS catalog.
// Author: Michael Barall 02/01/2020.
//
// This is a multi-threaded catalog ensemble generator.  Each catalog is
// seeded, generated, scanned, accumulated, and then discarded.  Multiple
// threads permit multiple catalogs to be generated simultaneously.

public class OEEnsembleGenerator implements SimpleThreadTarget {

	//----- Parameters -----

	// Note: Parameters cannot be modified while threads are running.

	// The ensemble parameters.

	private OEEnsembleParams ensemble_params;


	// Get the ensemble parameters.

	public OEEnsembleParams get_ensemble_params () {
		return ensemble_params;
	}




	//----- Thread communication -----

	// The number of catalogs generated so far.

	private int catalog_count;


	// Get the next work unit.
	// If there is more work to do, return the current catalog number and increment the catalog count.
	// If there is no more work to do, return -1.

	private synchronized int get_work_unit () {
		if (catalog_count >= ensemble_params.num_catalogs) {
			return -1;
		}
		int result = catalog_count;
		++catalog_count;
		return result;
	}




	//----- Construction -----




	// Erase the contents.

	public void clear () {
		ensemble_params = null;

		catalog_count = 0;
		return;
	}




	// Default constructor.

	public OEEnsembleGenerator () {
		clear();
	}




	//----- Generator thread -----




	// Entry point for a thread.
	// Parameters:
	//  thread_manager = The thread manager.
	//  thread_number = The thread number, which ranges from 0 to the number of
	//                  threads in the pool minus 1.
	// Threading: This function is called by all the threads in the pool, and
	// so must be thread-safe and use any needed synchronization.

	@Override
	public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

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

		// Loop until prompt termination is requested

		while (!( thread_manager.get_req_termination() )) {

			// Get next work unit, end loop if none

			final int ncat = get_work_unit();
			if (ncat < 0) {
				break;
			}

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




	// Get the number of catalogs processed or being processed.
	// This may be called while running to monitor progress,
	// or after termination to obtain the number of catalogs generated.

	public synchronized int get_catalog_count () {
		return catalog_count;
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




	// Get the elapsed time, in seconds, as a string.

	private String get_elapsed_time (long start_time) {
		long elapsed_time = System.currentTimeMillis() - start_time;
		String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);
		return s_elapsed_time;
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

		// Validate parameters

		if (!( the_num_threads > 0 )) {
			throw new IllegalArgumentException ("OEEnsembleGenerator.generate_all_catalogs: Invalid number of threads: " + the_num_threads);
		}
	
		// Pre-launch operations

		pre_launch (the_ensemble_params);

		// Launch the threads

		SimpleThreadManager thread_manager = new SimpleThreadManager();
		thread_manager.launch_threads (this, executor, the_num_threads);

		// Get the start time

		long start_time = thread_manager.get_start_time();

		// Loop until terminated

		while (!( thread_manager.await_termination (max_runtime, progress_time) )) {

			// Display progress message

			if (progress_time >= 0L) {
				System.out.println ("Generating " + get_catalog_count() + " ETAS catalogs so far in " + get_elapsed_time (start_time) + " seconds");
			}
		}

		// Check for thread abort

		if (thread_manager.is_abort()) {
			if (progress_time >= 0L) {
				System.out.println ("Stopped because of thread abort in " + get_elapsed_time (start_time) + " seconds");
				System.out.println (thread_manager.get_abort_message_string());
			}
		}

		// Otherwise, check for timeout

		else if (thread_manager.is_timeout()) {
			if (progress_time >= 0L) {
				System.out.println ("Reached runtime limit after generating " + get_catalog_count() + " ETAS catalogs in " + get_elapsed_time (start_time) + " seconds");
			}
		}

		// Otherwise, normal termination

		else {
			if (progress_time >= 0L) {
				System.out.println ("Finished generating " + get_catalog_count() + " ETAS catalogs in " + get_elapsed_time (start_time) + " seconds");
			}
		}

		// Post-termination operations

		post_termination();

		return;
	}




	// Generate all the catalogs.
	// Parameters:
	//  the_ensemble_params = The ensemble parameters.
	//  executor = The executor to use for launching the threads.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// This combines the function of pre_launch, launch_threads, await_termination, and post_termination.
	// This version creates the executor.

	public void generate_all_catalogs (OEEnsembleParams the_ensemble_params, AutoExecutorService executor, long max_runtime, long progress_time) {

		// Generate catalogs
		
		generate_all_catalogs (the_ensemble_params, executor.get_executor(), executor.get_num_threads(), max_runtime, progress_time);
		return;
	}




	// Generate all the catalogs.
	// Parameters:
	//  the_ensemble_params = The ensemble parameters.
	//  the_num_threads = The number of threads to use, must be > 0, or -1 for default number of threads.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// This combines the function of pre_launch, launch_threads, await_termination, and post_termination.
	// This version creates the executor.

	public void generate_all_catalogs (OEEnsembleParams the_ensemble_params, int the_num_threads, long max_runtime, long progress_time) {

		try (

			// Create the executor

			AutoExecutorService auto_executor = new AutoExecutorService (the_num_threads);
		){

			// Generate catalogs
		
			generate_all_catalogs (the_ensemble_params, auto_executor, max_runtime, progress_time);
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
