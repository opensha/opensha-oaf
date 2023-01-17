package org.opensha.oaf.oetas;

import java.util.Collection;


// A catalog examiner that saves the catalog into a list.
// Note: The ordering of the ruptures is unspecified.
// Author: Michael Barall 12/14/2022.

public class OEExaminerSaveList implements OECatalogExaminer {

	// The list to receive the catalog contents.

	public Collection<OERupture> rup_list;

	// Flag to include seed ruptures (generation 0).

	public boolean f_seed;

	// Flag to include ruptures representing a background rate.

	public boolean f_background;

	// Flag to also print out a summary of the list.

	public boolean f_verbose;


	// Constructor specifies the list.
	// Parameters:
	//  rup_list = Collection to receive ruptures.
	//  f_verbose = True to print out a catalog summary.
	// This constructor includes seed ruptures but not ruptures representing a background rate.

	public OEExaminerSaveList (Collection<OERupture> rup_list, boolean f_verbose) {
		this.rup_list = rup_list;
		this.f_seed = true;
		this.f_background = false;
		this.f_verbose = f_verbose;
	}


	// Constructor specifies the list.
	// Parameters:
	//  rup_list = Collection to receive ruptures.
	//  f_seed = True to include seed ruptures (generation 0).
	//  f_background = True to include ruptures representing a background rate.
	//  f_verbose = True to print out a catalog summary.

	public OEExaminerSaveList (Collection<OERupture> rup_list, boolean f_seed, boolean f_background, boolean f_verbose) {
		this.rup_list = rup_list;
		this.f_seed = f_seed;
		this.f_background = f_background;
		this.f_verbose = f_verbose;
	}


	// Examine the catalog.
	// Parameters:
	//  view = Catalog view.
	//  rangen = Random number generator to use.

	@Override
	public void examine_cat (OECatalogView view, OERandomGenerator rangen) {

		// Print a catalog summary, if desired

		if (f_verbose) {
			System.out.println ();
			System.out.println (view.summary_and_gen_list_string());
		}

		// Add all the ruptures to the list

		view.dump_to_collection (rup_list, f_seed, f_background);
		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEExaminerSaveList : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OEExaminerSaveList : Unrecognized subcommand : " + args[0]);
		return;

	}

}
