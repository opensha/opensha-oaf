package org.opensha.oaf.oetas;


// Interface for reading out accumulated results on a time/magnitude grid for Operational ETAS.
// Author: Michael Barall 03/13/2022.
//
// This interface represents an accumulator that can compute fractiles and
// probability of occurrence for each bin in a time/magnitude grid.
//
// Threading: The intent is for a single thread to use this interface after an
// ensemble of ETAS simulations has been completed.
//
// Note: The actual time/magnitude grid is specified externally to this interface.

public interface OEAccumReadoutTimeMag {


	// Get a fractile.
	// Parameters:
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is an integer N, such that the probability of N or fewer
	// ruptures is approximately equal to fractile.
	// Note: The returned array should be newly-allocated and not retained by this object.

	public int[][] get_fractile_array (double fractile);




	// Get the probability of occurrence array.
	// Parameters:
	//  xcount = Number of ruptures to check, should be >= 0.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is a real number v, such that v is the probability that
	// the number of ruptures that occur is greater than xcount.
	// Note: Currently, implementations are only required to support xcount == 0,
	// which gives the probability that at least one rupture occurs.
	// Note: The returned array should be newly-allocated and not retained by this object.

	public double[][] get_prob_occur_array (int xcount);




	// Return true if catalog size information is available.

	public default boolean has_cat_size_info () {
		return false;
	}




	// Get a fractile for catalog size information.
	// Parameters:
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns a 1D array with dimensions r[time_bins].
	// Each element corresponds to one time bin (which is cumulative by definition).
	// Each value is an integer N, such that the probability of a catalog containing
	// N or fewer ruptures is approximately equal to fractile.
	// Note: The returned array should be newly-allocated and not retained by this object.
	// Note: Returns null if catalog size information is not available.

	public default int[] get_cat_size_fractile_array (double fractile) {
		return null;
	}

}
