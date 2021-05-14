package org.opensha.oaf.oetas;


// Class to communicate with a catalog seeder.
// Author: Michael Barall 01/26/2020.

public class OECatalogSeedComm {

	//----- Per-catalog data -----

	// The catalog builder.

	public OECatalogBuilder cat_builder;

	// Random number generator to use while seeding this catalog.

	public OERandomGenerator rangen;




	//----- Construction -----




	// Default constructor.

	public OECatalogSeedComm () {
		cat_builder = null;
		rangen = null;
	}




	//----- Setup functions -----




	// Set up per-catalog data.
	// Parameters:
	//  the_cat_builder = Catalog builder.
	//  the_rangen = Random number generator.

	public void setup_seed_comm (OECatalogBuilder the_cat_builder, OERandomGenerator the_rangen) {

		// Save the objects

		cat_builder = the_cat_builder;
		rangen = the_rangen;
		return;
	}




	// Forget retained objects.

	public void forget () {
		cat_builder = null;
		rangen = null;
		return;
	}

}
