package org.opensha.oaf.oetas;


// Interface for reading out estimated catalog sizes on a generation/magnitude grid for Operational ETAS.
// Author: Michael Barall 04/10/2023.
//
// This interface represents an accumulator that can compute estimated catalog sizes and
// probability of occurrence for each bin in a generation/magnitude grid.
//
// Threading: The intent is for a single thread to use this interface after an
// ensemble of ETAS simulations has been completed.
//
// Note: The actual generation/magnitude grid is specified externally to this interface.
// Each point in the grid represents an estimate of catalog size, for a given number of
// generations and given minimum simulation magnitude.  Note that if the number of
// generations is 2, then the estimate includes only direct aftershocks of the seeds.

public interface OEAccumReadoutSeedEst {


	// Get a fractile.
	// Parameters:
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns a 2D array with dimensions r[gen_bins][mag_bins].
	// Each element corresponds to one combination of generations and magnitude.
	// Each value is an integer N, such that the probability of N or fewer
	// ruptures in the catalog is approximately equal to fractile.
	// Note: The returned array should be newly-allocated and not retained by this object.

	public int[][] get_fractile_array (double fractile);




	// Get the probability of occurrence array.
	// Parameters:
	//  xcount = Number of ruptures to check, should be >= 0.
	// Returns a 2D array with dimensions r[gen_bins][mag_bins].
	// Each element corresponds to one combination of generations and magnitude.
	// Each value is a real number v, such that v is the probability that
	// the number of ruptures in the catalog is greater than xcount.
	// Note: Currently, implementations are only required to support xcount == 0,
	// which gives the probability that there is at least one rupture in the catalog.
	// Note: The returned array should be newly-allocated and not retained by this object.

	public double[][] get_prob_occur_array (int xcount);

}
