package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleThreadLoopHelper;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;


// Class for generating an ensemble of operational ETAS catalog.
// Author: Michael Barall 02/01/2020.
//
// This is a multi-threaded catalog ensemble generator.  Each catalog is
// seeded, generated, scanned, accumulated, and then discarded.  Multiple
// threads permit multiple catalogs to be generated simultaneously.
//
// The recommended procedure is to create a new OEEnsembleGenerator
// object for each ensemble of catalogs generated (although the code
// would permit the object to be reused).

public class OEEnsembleGenerator implements SimpleThreadTarget {

	//----- Parameters -----

	// Note: Parameters cannot be modified while threads are running.

	// The ensemble parameters.

	private OEEnsembleParams ensemble_params;


	// Get the ensemble parameters.

	public final OEEnsembleParams get_ensemble_params () {
		return ensemble_params;
	}




	//----- Status messages -----

	// Progress message format while running.

	private static final String PMFMT_RUNNING = "Generated %C ETAS catalogs so far in %E seconds using %U";

	// Progress message format after completion.

	private static final String PMFMT_DONE = "Finished generating %C ETAS catalogs in %E seconds";

	// Progress message format for timeout.

	private static final String PMFMT_TIMEOUT = "Reached time limit after generating %C ETAS catalogs in %E seconds";

	// Progress message format for abort.

	private static final String PMFMT_ABORT = "Aborted because of error after generating %C ETAS catalogs in %E seconds";

	// Message when no message is available yet.

	private static final String PMFMT_NONE = "No status available";


	// The status message for the last catalog generation.

	private String status_msg;




	//----- Thread communication -----

	// The loop helper.

	private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);




	//----- Construction -----




	//// Erase the contents.
	//
	//public final void clear () {
	//	ensemble_params = null;
	//
	//	catalog_count.set(0);
	//	f_thread_abort = false;
	//	return;
	//}




	// Default constructor.

	public OEEnsembleGenerator () {
		ensemble_params = null;
		status_msg = PMFMT_NONE;
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

		// Loop until loop completed or prompt termination is requested

		for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

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

	private void pre_launch (OEEnsembleParams the_ensemble_params) {

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




	// Get the number of catalogs generated.
	// Threading: This function may only be called from the main thread after termination.

	public final int get_catalog_count () {
		return loop_helper.get_completions();
	}




	// Return true if thread abort occurred during the last operation.
	// Threading: This function may only be called from the main thread after termination.

	public final boolean has_thread_abort () {
		return loop_helper.is_abort();
	}




	// Get the final status message for the last operation.
	// Threading: This function may only be called from the main thread after termination.

	public final String get_status_msg () {
		return status_msg;
	}




	// Perform post-termination operations.
	// This must be called after all threads are terminated to finish accumulation.

	private void post_termination () {

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
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// Returns the number of catalogs generated, or -1 if thread abort.
	// This combines the function of pre_launch, launch_threads, await_termination, and post_termination.
	// This version creates the executor.

	public int generate_all_catalogs (OEEnsembleParams the_ensemble_params, AutoExecutorService executor, long max_runtime, long progress_time) {

		int ncat_gen = 0;

		// No status

		status_msg = PMFMT_NONE;
	
		// Pre-launch operations

		pre_launch (the_ensemble_params);

		// Run the loop

		loop_helper.run_loop (this, executor, 0, ensemble_params.num_catalogs, max_runtime, progress_time);

		// Check for thread abort

		if (loop_helper.is_abort()) {
			System.out.println (loop_helper.get_abort_message_string());
			status_msg = loop_helper.make_progress_message (PMFMT_ABORT);
			System.out.println (status_msg);
			ncat_gen = -1;
		}

		// Otherwise, check for timeout

		else if (loop_helper.is_incomplete()) {
			status_msg = loop_helper.make_progress_message (PMFMT_TIMEOUT);
			System.out.println (status_msg);
			ncat_gen = loop_helper.get_completions();
		}

		// Otherwise, normal termination

		else {
			status_msg = loop_helper.make_progress_message (PMFMT_DONE);
			System.out.println (status_msg);
			ncat_gen = loop_helper.get_completions();
		}

		// Post-termination operations

		post_termination();

		return ncat_gen;
	}




	// Generate all the catalogs.
	// Parameters:
	//  the_ensemble_params = The ensemble parameters.
	//  the_num_threads = The number of threads to use, must be > 0, or -1 for default number of threads.
	//  max_runtime = Maximum runtime requested, in milliseconds, can be -1L for no limit.
	//  progress_time = Time interval for progress messages, in milliseconds, can be -1L for no progress messages.
	// Returns the number of catalogs generated, or -1 if thread abort.
	// This combines the function of pre_launch, launch_threads, await_termination, and post_termination.
	// This version creates the executor.

	public int generate_all_catalogs (OEEnsembleParams the_ensemble_params, int the_num_threads, long max_runtime, long progress_time) {

		// Validate parameters

		if (!( the_num_threads > 0 || the_num_threads == -1 )) {
			throw new IllegalArgumentException ("OEEnsembleGenerator.generate_all_catalogs: Invalid number of threads: the_num_threads = " + the_num_threads);
		}

		// Create accessor and generate catalogs

		int ncat_gen = 0;

		try (

			// Create the executor

			AutoExecutorService auto_executor = new AutoExecutorService (the_num_threads);
		){

			// Generate catalogs
		
			ncat_gen = generate_all_catalogs (the_ensemble_params, auto_executor, max_runtime, progress_time);
		}

		return ncat_gen;
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
