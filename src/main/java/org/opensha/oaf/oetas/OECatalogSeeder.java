package org.opensha.oaf.oetas;


// Interface for seeding a catalog of Operational ETAS ruptures.
// Author: Michael Barall 01/26/2020.
//
// This interface is an object that can seed the contents of a single
// realization of an operational ETAS catalog.
//
// Lifecyle: Each usage of this object consists of a call to open(), followed
// by a series of calls to the data methods, followed by a call to close().
// After the object is closed, it can be opened again by calling open().
//
// The data methods currently consist of a single call to seed_catalog().
//
// Threading: Multiple objects may exist that all refer to the same initializer.
// These objects may be used simultaneously by different threads.
//
// During each use of an object, all data method calls are performed by a single
// worker thread.  Ideally, data method calls should not need to synchronize.
// If an object is re-used, then the second use may be executed by a different
// worker thread than the first use.
//
// Calls to open() and close() may be made by the same thread as the data methods,
// or by a different thread (e.g., the main thread).
//
// Memory consistency requirements: When seeders are used by multiple worker
// threads, the following is required to ensure memory consistency.
//  * Actions required to set up the initializer must happen-before calls to the
//    initializer's make_seeder() and begin_initialization() methods.
//  * The call to the initializer's begin_initialization() method must happen-before
//    the call to open().
//  * The call to open() for a given catalog must happen-before the calls to any
//    data methods for that catalog.
//  * All calls to data methods for a given catalog must be made by the same thread.
//  * The calls to data methods for a given catalog must happen-before the call to
//    close() for that catalog.
//  * If the seeder is re-used for another catalog, the call to close() must
//    happen-before the call to open() for the next catalog.
//  * The call to close() must happen-before the call to the initializer's
//    end_initialization() method.
//  * If the initializer is re-used for another ensemble, the call to
//    end_initialization() must happen-before the call to begin_initialization()
//    for the next ensemble.
//
// A simple and recommended way to satisfy memory consistency requirements is to
// call the initializer's begin_initialization() method before creating the worker
// threads; then have each worker thread call the initializer's make_seeder()
// and begin_initialization() methods, open(), close(), and data methods; and then
// call the initializer's end_initialization() method after the worker threads are
// all terminated.  (This presumes that worker thread creation and termination are
// synchronized to the main thread; using SimpleThreadManager is one way to do it.)

public interface OECatalogSeeder extends AutoCloseable {

	//----- Open/Close methods -----

	// Open the catalog seeder.
	// Perform any setup needed.

	public void open ();

	// Close the catalog seeder.
	// Perform any final tasks needed.

	@Override
	public void close ();


	//----- Data methods -----

	// Seed a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog values set up.
	// This function should perform the following steps:
	// 1. Construct a OECatalogParams object containing the catalog parameters.
	// 2. Call comm.cat_builder.begin_catalog() to begin constructing the catalog.
	// 3. Construct a OEGenerationInfo object containing info for the first (seed) generation.
	// 4. Call comm.cat_builder.begin_generation() to begin the first (seed) generation.
	// 5. Call comm.cat_builder.add_rup one or more times to add the seed ruptures.
	// 6. Call comm.cat_builder.end_generation() to end the first (seed) generation.

	public void seed_catalog (OECatalogSeedComm comm);

}
