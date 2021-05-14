package org.opensha.oaf.oetas;


// Interface for initializing a set of catalogs of Operational ETAS ruptures.
// Author: Michael Barall 01/26/2020.
//
// This interface is an object that can initialize the contents of multiple
// realizations of an operational ETAS catalog.

public interface OEEnsembleInitializer {

	//----- Factory methods -----

	// Make a catalog seeder.
	// Returns a seeder which is able to seed the contents of one catalog
	// (or several catalogs in succession).
	// This function may be called repeatedly to create several seeders,
	// which can be used in multiple worker threads.
	// Threading: Can be called in multiple threads, before or after the call to
	// begin_initialization, and while there are existing open seeders, and so
	// must be properly synchronized.
	// Note: The returned seeder cannot be opened until after the call to
	// begin_initialization, and must be closed before the call to end_initialization.
	// Note: The returned seeder can be opened and closed repeatedly to seed
	// multiple catalogs.

	public OECatalogSeeder make_seeder ();


	//----- Control methods -----

	// Begin initializing catalogs.
	// This function should be called before any other control methods.
	// The initializer should allocate any resources it needs.
	// Threading: No other thread should be accessing this object,
	// and none of its seeders can be open.

	public void begin_initialization ();

	// End initializing catalogs.
	// This function should be called after all other control functions.
	// It provides an opportunity for the initializer to release any resources it holds.
	// Threading: No other thread should be accessing this object,
	// and none of its seeders can be open.

	public void end_initialization ();

}
