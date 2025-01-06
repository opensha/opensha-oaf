package org.opensha.oaf.oetas.util;


// Interface for finding bins for accumulating marginal distributions.
// Author: Michael Barall 08/06/2024.
//
// This interface is an object that finds the bin that contains a given floating-point number.

public interface OEMarginalBinFinder {


	//----- Bin finder methods -----


	// Get the number of bins.
	// Threading: Can be called by multiple threads.

	public int get_bin_count ();


	// Find the bin which contains a given value.
	// The return value ranges from 0 to get_bin_count()-1.
	// Threading: Can be called by multiple threads.

	public int find_bin (double v);

}
