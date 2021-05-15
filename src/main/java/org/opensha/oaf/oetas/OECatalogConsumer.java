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
// or by a different thread (e.g., the main thread).  These methods generally need
// to synchronize in order to access data structures within the accumulator.
// So, it may be more efficient to call open() and close() in the main thread so
// that worker threads don't need to block.

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
