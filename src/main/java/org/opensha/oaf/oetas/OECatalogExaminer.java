package org.opensha.oaf.oetas;


// Interface for examining a catalog of Operational ETAS ruptures.
// Author: Michael Barall 10/09/2020.
//
// This interface is an object that can examine the contents of a single
// realization of an operational ETAS catalog.
//
// Threading: If multiple objects are used by different threads, then
// the implementation must supply appropriate synchronization.
//
// Note: This interface does not specify whether an examiner can be used
// repeatedly to examine multiple catalogs in succession.

public interface OECatalogExaminer {

	// Examine the catalog.
	// Parameters:
	//  view = Catalog view.
	//  rangen = Random number generator to use.

	public void examine_cat (OECatalogView view, OERandomGenerator rangen);

}
