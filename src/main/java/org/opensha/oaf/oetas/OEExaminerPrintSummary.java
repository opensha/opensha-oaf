package org.opensha.oaf.oetas;


// A catalog examiner that prints out a summary of the catalog.
// Author: Michael Barall 12/14/2022.

public class OEExaminerPrintSummary implements OECatalogExaminer {

	// Constructor does nothing.

	public OEExaminerPrintSummary () {
	}


	// Examine the catalog.
	// Parameters:
	//  view = Catalog view.
	//  rangen = Random number generator to use.

	@Override
	public void examine_cat (OECatalogView view, OERandomGenerator rangen) {

		// Print a catalog summary

		System.out.println ();
		System.out.println (view.summary_and_gen_list_string());
		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEExaminerPrintSummary : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OEExaminerPrintSummary : Unrecognized subcommand : " + args[0]);
		return;

	}

}
