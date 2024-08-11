package org.opensha.oaf.oetas.util;

import java.util.Arrays;


// A marginal bin finder for a general set of bins.
// Author: Michael Barall 08/06/2024.
//
// The bins are defined by an array that gives the bin boundaries.

public class OEMarginalBinGeneral implements OEMarginalBinFinder {


	//----- Bin definition -----


	// Array that defines the bins.
	// The number of bins is bin_array.length + 1;
	// Bin n consists of values v that satisfy
	//  bin_array[n-1] <= v < bin_array[n]
	// For the purpose of the last condition, bin_array[-1] == -infinity and bin_array[bin_array.length] == infinity.
	// Note that bin_array[n] is the first array element that is greater than v.

	private double[] bin_array;


	//----- Bin finder methods -----


	// Get the number of bins.
	// Threading: Can be called by multiple threads.

	@Override
	public int get_bin_count () {
		return bin_array.length + 1;
	}


	// Find the bin which contains a given value.
	// The return value ranges from 0 to get_bin_count()-1.
	// Threading: Can be called by multiple threads.

	@Override
	public int find_bin (double v) {
		return OEArraysCalc.bsearch_array (bin_array, v);
	}


	//----- Construction -----


	// Construct from a given range of bin boundaries.
	// Parameters:
	//  boundaries = Array of bin boundaries.
	//  lo = Lower limit of range, inclusive.
	//  hi = Upper limit of range, exclusive.
	// Note: This function copies the range, so subsequent changes to the
	// array have no effect.

	public OEMarginalBinGeneral (double[] boundaries, int lo, int hi) {
		bin_array = Arrays.copyOfRange (boundaries, lo, hi);
	}


	// Construct from a given array of bin boundaries.
	// Parameters:
	//  boundaries = Array of bin boundaries.
	// Note: This function copies the array, so subsequent changes to the
	// array have no effect.

	public OEMarginalBinGeneral (double[] boundaries) {
		bin_array = Arrays.copyOfRange (boundaries, 0, boundaries.length);
	}

}
