package org.opensha.oaf.oetas.util;


// A marginal bin finder that always returns a single bin.
// Author: Michael Barall 08/06/2024.
//
// The bin count, and the bin returned, are configurable.

public class OEMarginalBinSingle implements OEMarginalBinFinder {


	//----- Bin definition -----

	// The bin count.

	private int my_bin_count;

	// The bin we return.

	private int my_bin;


	//----- Bin finder methods -----


	// Get the number of bins.
	// Threading: Can be called by multiple threads.

	@Override
	public int get_bin_count () {
		return my_bin_count;
	}


	// Find the bin which contains a given value.
	// The return value ranges from 0 to get_bin_count()-1.
	// Threading: Can be called by multiple threads.

	@Override
	public int find_bin (double v) {
		return my_bin;
	}


	//----- Construction -----


	// Construct from a given bin count and bin.
	// Parameters:
	//  the_bin_count = Number of bins to report
	//  the_bin = The bin number to return..

	public OEMarginalBinSingle (int the_bin_count, int the_bin) {
		my_bin_count = the_bin_count;
		my_bin = the_bin;
	}


	// Default constructor sets up a finder with a single bin.

	public OEMarginalBinSingle () {
		my_bin_count = 1;
		my_bin = 0;
	}

}
