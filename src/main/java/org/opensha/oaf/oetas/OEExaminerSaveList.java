package org.opensha.oaf.oetas;

import java.util.Collection;


// A catalog examiner that saves the catalog into a list.
// Note: The ordering of the ruptures is unspecified.
// Author: Michael Barall 12/14/2022.

public class OEExaminerSaveList implements OECatalogExaminer {

	// The list to receive the catalog contents.

	public Collection<OERupture> rup_list;

	// Flag to also print out a summary of the list.

	public boolean f_verbose;


	// Constructor specifies the list.

	public OEExaminerSaveList (Collection<OERupture> rup_list, boolean f_verbose) {
		this.rup_list = rup_list;
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

		view.dump_to_collection (rup_list);
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
