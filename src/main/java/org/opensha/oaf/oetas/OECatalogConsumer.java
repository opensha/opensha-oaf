package org.opensha.oaf.oetas;


// Interface for consuming a catalog of Operational ETAS ruptures.
// Author: Michael Barall 12/28/2019.
//
// This interface is an object that can consume the contents of a single
// realization of an operational ETAS catalog.
//
// Lifecyle: Each usage of this object consists of a call to open(), followed
// by a series of calls to the data methods, followed by a call to close().
// After the object is closed, it can be opened again by calling open().
//
// The data methods begin with a call to begin_catalog() and end with a call to
// end_catalog().  In between there is a series of generations.
//
// The first (seed) generation is provided by a call to begin_seed_generation(),
// followed by a series of calls to next_seed_rup(), and concluded by aa call
// to end_seed_generation().
//
// Each succeeding (ETAS) generation is provided by a call to begin_generation(),
// followed by a series of calls to next_rup() and next_sterile_rup(), and
// concluded by a call to end_generation().
//
// Threading: Multiple objects may exist that all refer to the same accumulator.
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
// Memory consistency requirements: When consumers are used by multiple worker
// threads, the following is required to ensure memory consistency.
//  * Actions required to set up the accumulator must happen-before calls to the
//    accumulator's make_consumer() and begin_accumulation() methods.
//  * The call to the accumulator's begin_accumulation() method must happen-before
//    the call to open().
//  * The call to open() for a given catalog must happen-before the calls to any
//    data methods for that catalog.
//  * All calls to data methods for a given catalog must be made by the same thread.
//  * The calls to data methods for a given catalog must happen-before the call to
//    close() for that catalog.
//  * If the consumer is re-used for another catalog, the call to close() must
//    happen-before the call to open() for the next catalog.
//  * The call to close() must happen-before the call to the accumulator's
//    end_accumulation() method.
//
// A simple and recommended way to satisfy memory consistency requirements is to
// call the accumulator's begin_accumulation() method before creating the worker
// threads; then have each worker thread call the accumulator's make_consumer()
// and begin_accumulation() methods, open(), close(), and data methods; and then
// call the accumulator's end_accumulation() method after the worker threads are
// all terminated.  (This presumes that worker thread creation and termination are
// synchronized to the main thread; using SimpleThreadManager is one way to do it.)

public interface OECatalogConsumer extends AutoCloseable {

	//----- Open/Close methods -----

	// Open the catalog consumer.
	// Perform any setup needed to begin consuming a catalog.

	public void open ();

	// Close the catalog consumer.
	// Perform any final tasks needed to finish consuming a catalog,
	// such as storing results into an accumulator.

	@Override
	public void close ();


	//----- Data methods -----

	// Begin consuming a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog values set up.

	public void begin_catalog (OECatalogScanComm comm);

	// End consuming a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog values set up.

	public void end_catalog (OECatalogScanComm comm);

	// Begin consuming the first (seed) generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog and per-generation values set up.

	public void begin_seed_generation (OECatalogScanComm comm);

	// End consuming the first (seed) generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog and per-generation values set up.

	public void end_seed_generation (OECatalogScanComm comm);

	// Next rupture in the first (seed) generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.
	//
	// Usage note:  A rupture passed to next_seed_rup() can be a background rate, so the function must
	// be prepared to handle them.  A background rate has a time in the far past, so its time will
	// compare earlier than the time range of the simulation.  OERandomGenerator.omori_rate_shifted
	// and OERandomGenerator.omori_sample_shifted check for background rates and handle them.

	public void next_seed_rup (OECatalogScanComm comm);

	// Begin consuming the next generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog and per-generation values set up.

	public void begin_generation (OECatalogScanComm comm);

	// End consuming a generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog and per-generation values set up.

	public void end_generation (OECatalogScanComm comm);

	// Next rupture in the current generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

	public void next_rup (OECatalogScanComm comm);

	// Next sterile rupture in the current generation of a catalog.
	// Parameters:
	//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

	public void next_sterile_rup (OECatalogScanComm comm);

}
