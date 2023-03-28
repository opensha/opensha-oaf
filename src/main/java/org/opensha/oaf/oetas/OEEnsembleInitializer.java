package org.opensha.oaf.oetas;

import java.util.Map;


// Interface for initializing a set of catalogs of Operational ETAS ruptures.
// Author: Michael Barall 01/26/2020.
//
// This interface is an object that can initialize the contents of multiple
// realizations of an operational ETAS catalog.
//
// Repeatability requirement:  A single instance of OEEnsembleInitializer can
// be used multiple times via repeated calls to begin_initialization and
// end_initialization.  If this is done, then the initializer must behave as if
// there is a pre-selected sequence of initial states, and each usage produces
// an initial subsequence of that pre-selected sequence.  In a multi-threaded
// usage, the assignment of initial states to threads need not be repeated,
// however, the total set of all initial states produced by all threads must be
// an initial subsequence.  In particular, if two usages initialize the same
// number of catalogs, then the set of initial states in the two usages is
// identical.  The purpose of this rule is to facilitate simulation ranging.
// Note that the rule applies only to a single instance of OEEnsembleInitializer;
// two separate instances need not produce the same sequence of initial states.

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


	//----- Ranging methods -----

	// Return true if there is a mainshock magnitude available.
	// Threading: No other thread should be accessing this object,
	// and be either before calling begin_initialization() or after
	// calling end_initialization().

	public boolean has_mainshock_mag ();

	// Return the mainshock magnitude.
	// Check has_mainshock_mag() before calling this function.
	// Note: If has_mainshock_mag() returns false, then this function should
	// return a scaling magnitude (e.g., largest earthquake in a swarm), which
	// can be used for simulation ranging, but is not reported in the forecast.
	// Threading: No other thread should be accessing this object,
	// and be either before calling begin_initialization() or after
	// calling end_initialization().

	public double get_mainshock_mag ();

	// Get the time and magnitude range of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	public OECatalogRange get_range ();

	// Get the initial time and magnitude range of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	public OECatalogRange get_initial_range ();

	// Set the time and magnitude range to use for catalog simulations.
	// The supplied OECatalogRange object is not retained.
	// Note: This function allows adjusting time and magnitude ranges
	// without the need to construct an entirely new initializer.

	public void set_range (OECatalogRange range);

	// Get the b-value used by the initializer.
	// The purpose of this function is to obtain a b-value that can be used
	// for adjusting the magnitude range in order to get a desired median
	// or expected catalog size.

	public double get_b_value ();

	// Get the time at which the forecast begins, in days.
	// The value should be >= the simulation begin time in the catalog parameters.

	public double get_t_forecast ();

	// Get parameters that can be displayed to the user.
	// Parameters:
	//  paramMap = Map of parameters, which this function adds to.
	// For consistency, it is recommended that parameter names use camelCase.
	// Each value should be one of: Integer, Long, Double, Float, Boolean, or String.

	public void get_display_params (Map<String, Object> paramMap);

}
