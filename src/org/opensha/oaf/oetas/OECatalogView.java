package org.opensha.oaf.oetas;


// Interface to provide access to a catalog of Operational ETAS ruptures.
// Author: Michael Barall 11/03/2019.
//
// The catalog appears as a read-only list of OERupture objects.
//
// The ordering of ruptures in the catalog is not specified by this interface,
// except that ruptures are organized into a series of generations.
//
// It is required that multiple threads must be able to access a catalog through
// this interface, without the use of locks.
//
// Design note: The interface is designed to fill in a caller-supplied OERupture object.
// This avoids the need to create very large numbers of OERupture objects, while still
// allowing for a thread-safe implementation.
//
// Implementation note: Typically, the catalog implementation stores ruptures in a
// compact form, not as a collection of OERupture objects.

public interface OECatalogView {

	// Get parameters for the catalog.
	// Parameters:
	//  cat_params = Structure to receive the catalog parameters.

	public void get_cat_params (OECatalogParams cat_params);

	// Get the total number of ruptures in the catalog.

	public int size ();

	// Get the number of generations in the catalog.

	public int get_gen_count ();

	// Get the number of ruptures in the i-th generation.
	// Parameters:
	//  i_gen = Generation number.

	public int get_gen_size (int i_gen);

	// Get information about the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  gen_info = Structure to receive the generation information.

	public void get_gen_info (int i_gen, OEGenerationInfo gen_info);

	// Get the j-th rupture in the i-th generation in the catalog.
	// Parameters:
	//  i_gen = Generation number.
	//  j_rup = Rupture number, within the generation.
	//  rup = Structure to receive the rupture information.

	public void get_rup (int i_gen, int j_rup, OERupture rup);

}
