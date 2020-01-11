package org.opensha.oaf.oetas;


// Interface for building a catalog of Operational ETAS ruptures.
// Author: Michael Barall 11/29/2019.
//
// The catalog appears as an extendible list of OERupture objects.
//
// The ordering of ruptures in the catalog is not specified by this interface,
// except that ruptures are organized into a series of generations.
//
// Only a single thread may access a catalog through this interface.
//
// Design note: The interface is designed to use a caller-supplied OERupture object.
// This avoids the need to create very large numbers of OERupture objects.
//
// Implementation note: Typically, the catalog implementation stores ruptures in a
// compact form, not as a collection of OERupture objects.

public interface OECatalogBuilder extends OECatalogView {

	//----- Methods inherited from OECatalogView -----

	// Get parameters for the catalog.
	// Parameters:
	//  cat_params = Structure to receive the catalog parameters.

	// public void get_cat_params (OECatalogParams cat_params);		// inherited

	// Get the total number of ruptures in the catalog.

	// public int size ();		// inherited

	// Get the number of generations in the catalog.

	// public int get_gen_count ();		// inherited

	// Get the number of ruptures in the i-th generation.
	// Parameters:
	//  i_gen = Generation number.

	// public int get_gen_size (int i_gen);		// inherited

	// Get information about the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  gen_info = Structure to receive the generation information.

	// public void get_gen_info (int i_gen, OEGenerationInfo gen_info);		// inherited

	// Get the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.

	// public void get_rup (int i_gen, int j_rup, OERupture rup);		// inherited

	// Construct a string that summarizes the catalog contents.
	// This displays the catalog size and generation count,
	// parameters, and info for each generation.

	// public default String summary_and_gen_list_string();		// inherited

	// Construct a string that dumps the entire catalog contents.
	// Caution: This can be very large!

	// public default String dump_to_string();		// inherited


	//----- Methods for catalog construction -----

	// Begin construction of a catalog.
	// Parameters:
	//  cat_params = Parameters to use for this catalog.
	// This method clears the internal data structures and sets up
	// an empty catalog with zero generations.
	// Note: This allows re-using a catalog object to generate a new catalog.

	public void begin_catalog (OECatalogParams cat_params);

	// End construction of a catalog.

	public void end_catalog ();

	// Begin a new generation of a catalog.
	// Parameters:
	//  gen_info = Structure containing the generation information to set.
	// This method increments the number of generations, and creates a
	// new empty generation.

	public void begin_generation (OEGenerationInfo gen_info);

	// End a generation of a catalog.

	public void end_generation ();

	// Add a rupture to the current generation of a catalog.
	// Parameters:
	//  rup = Structure containing the rupture information to set.
	// Note: Ruptures can only be added to the generation currently being built.

	public void add_rup (OERupture rup);

}
