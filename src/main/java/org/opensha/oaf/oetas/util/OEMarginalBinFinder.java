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


	// Find the bins which contain the given values.
	// Returns an array of the same length as the argument,
	// in which each value ranges from 0 to get_bin_count()-1.
	// Threading: Can be called by multiple threads.

	public default int[] find_bins (double[] v) {
		int[] x = new int[v.length];
		for (int j = 0; j < v.length; ++j) {
			x[j] = find_bin (v[j]);
		}
		return x;
	}

}
