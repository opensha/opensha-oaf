package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;


// Class to communicate between a catalog scanner and catalog consumer.
// Author: Michael Barall 12/28/2019.
//
// Note: We have deliberately omitted the catalog total size and number
// of generations.  If those are not used, then it is possible for a consumer
// to process a catalog that is never in memory all at once.  With the current
// interface, all that needs to be in memory is the current, previous, and
// next generations.
//
// Note: A scanner implementation may choose to inherit from this class,
// to have convenient access to these variables.

public class OECatalogScanComm {

	//----- Per-catalog data -----

	// Parameters for the catalog.

	public OECatalogParams cat_params;

	// Random number generator to use while scanning this catalog.

	public OERandomGenerator rangen;


	//----- Per-generation data -----

	// The current generation number.
	// Generation 0 is the first, or seed, generation in the catalog.

	public int i_gen;

	// A flag which is true if this is the final generation in the catalog.

	public boolean f_final_gen;

	// Size of the current generation.

	public int gen_size;

	// Information for the current generation.

	public OEGenerationInfo gen_info;

	// Information for the next generation.
	// This is not valid if f_final_gen is true.

	public OEGenerationInfo next_gen_info;

	// The minimum magnitude required for sterile ruptures within this generation.
	// It contains NO_MAG_POS if sterile ruptures are not required.

	public double sterile_mag;

	// Initialize the sterile magnitude to the "none" value.
	// This function is for use by the scanner.

	public void init_sterile_mag () {
		sterile_mag = NO_MAG_POS;
	}

	// Return true if sterile ruptures are required.
	// This function is for use by the scanner.

	public boolean is_sterile_mag () {
		if (sterile_mag >= NO_MAG_POS_CHECK) {
			return false;
		}
		return true;
	}

	// Set the required minimum magnitude for sterile ruptures in the current generation.
	// This function aggregrates requests from multiple consumers by taking
	// the minimum of all magnitudes requested.
	// Magnitudes >= gen_info.gen_mag_min are discarded, because they are
	// already included within the existing ruptures.
	// An argument of NO_MAG_POS, or any value >= gen_info.gen_mag_min, is permitted to
	// indicate no sterile ruptures are required; although such calls are not necessary.
	// This function may be called by consumers during the begin_generation() call.

	public void set_sterile_mag (double req_sterile_mag) {
		if (req_sterile_mag < sterile_mag && req_sterile_mag < gen_info.gen_mag_min - cat_params.mag_eps) {
			sterile_mag = req_sterile_mag;
		}
		return;
	}


	//----- Per-rupture data -----

	// The current rupture number.
	// Rupture 0 is the first rupture within the generation.

	public int j_rup;

	// The current rupture.

	public OERupture rup;




	//----- Construction -----




	// Default constructor.

	public OECatalogScanComm () {

		// Allocate the required structures

		cat_params = new OECatalogParams();
		gen_info = new OEGenerationInfo();
		next_gen_info = new OEGenerationInfo();
		rup = new OERupture();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogScanComm:" + "\n");

		result.append ("cat_params = " + cat_params.toString());

		result.append ("Per-generation data:" + "\n");
		result.append ("i_gen = " + i_gen + "\n");
		result.append ("f_final_gen = " + f_final_gen + "\n");
		result.append ("gen_size = " + gen_size + "\n");
		result.append ("gen_info = " + gen_info.one_line_string() + "\n");
		result.append ("sterile_mag = " + sterile_mag + "\n");

		result.append ("Per-rupture data:" + "\n");
		result.append ("j_rup = " + j_rup + "\n");
		result.append ("rup = " + rup.one_line_string() + "\n");

		return result.toString();
	}




	//----- Setup functions for use by the scanner -----




	// Set up per-catalog data from a catalog view.
	// Parameters:
	//  view = Catalog view.

	public void setup_cat_from_view (OECatalogView view, OERandomGenerator the_rangen) {

		// Get catalog parameters

		view.get_cat_params (cat_params);

		// Save the random number generator

		rangen = the_rangen;
		return;
	}




	// Set up per-generation data from a catalog view.
	// Parameters:
	//  view = Catalog view.
	//  the_i_gen = Current generation number.

	public void setup_gen_from_view (OECatalogView view, int the_i_gen) {

		// Save generation number

		i_gen = the_i_gen;

		// Get size and info for the current generation

		gen_size = view.get_gen_size (i_gen);
		view.get_gen_info (i_gen, gen_info);

		// If this is the final generation ...

		if (i_gen + 1 >= view.get_gen_count()) {
		
			// Set final generation

			f_final_gen = true;

			// Clear info for the next generation

			next_gen_info.clear();
		}

		// Otherwise, not the final generation ...

		else {
		
			// Set not final generation

			f_final_gen = false;

			// Get info for the next generation
		
			view.get_gen_info (i_gen + 1, next_gen_info);
		}

		// Initialize the sterile magnitude

		init_sterile_mag();
		return;
	}




	// Set up per-rupture data from a catalog view.
	// Parameters:
	//  view = Catalog view.
	//  the_j_rup = Current rupture number.

	public void setup_rup_from_view (OECatalogView view, int the_j_rup) {

		// Save rupture number

		j_rup = the_j_rup;

		// Get rupture info

		view.get_rup (i_gen, j_rup, rup);
		return;
	}




	// Forget retained objects.

	public void forget () {
		rangen = null;
		return;
	}

}
