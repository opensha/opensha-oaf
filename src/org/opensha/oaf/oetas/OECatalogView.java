package org.opensha.oaf.oetas;


// Interface to provide access to a catalog of Operational ETAS ruptures.
// Author: Michael Barall 11/03/2019.
//
// The catalog appears as a read-only list of OERupture objects.
//
// The ordering of ruptures in the catalog is not specified by this interface, but
// might be specified when the catalog is obtained.
//
// Design note: The interface is designed to fill in a caller-supplied OERupture object.
// This avoids the need to create very large numbers of OERupture objects, while still
// allowing for the possibility of a thread-safe implementation.
//
// Implementation note: Typically, the catalog implementation stores ruptures in a
// compact form, not as a collection of OERupture objects.

public interface OECatalogView {

	// Get the number of generations in the catalog.

	public int get_gen_count ();

	// Get information about the m-th generation in the catalog.
	// Parameters:
	//  m_gen = Generation number.
	//  gen_info = Structure to receive the generation information.

	public void get_gen_info (int m_gen, OEGenerationInfo gen_info);

	// Get the n-th rupture in the m-th generation in the catalog.

	public void get (int m_gen, int n_rup, OERupture rup);

}
