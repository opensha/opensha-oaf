package org.opensha.oaf.oetas;


// Interface for reading out accumulated ranging information on a time grid for Operational ETAS.
// Author: Michael Barall 03/17/2022.
//
// This interface represents an accumulator that can determine the probability
// of catalog survival, and the distribution of catalog sizes, for each bin
// in a time grid.
//
// Threading: The intent is for a single thread to use this interface after an
// ensemble of ETAS simulations has been completed.
//
// Note: The actual time grid is specified externally to this interface.

public interface OEAccumReadoutRanging {


	// Get a fractile of a bin.
	// Parameters:
	//  n = Bin number.
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns an integer N, such that, among all catalogs that complete bin n,
	// the probability that the catalog has N or fewer ruptures in bins 0 thru n
	// (inclusive) is approximately equal to fractile.
	// (Note that the appropriate bin number is often one less than the
	// return value of get_survival_bins()).

	public int get_bin_fractile (int n, double fractile);




	// Get the number of time bins with a desired survival rate.
	// Parameters:
	//  stop_fraction = Fraction of catalogs stopped, should be between 0.0 and 1.0.
	// Returns an integer N, such that the fraction of catalogs that stop
	// within the first N time bins is less than or equal to stop_fraction.
	// So, no more than stop_fraction catalogs stop before time_values[N].
	// Returns active_time_bins if no more than stop_fraction catalogs
	// stop during the active time bins.
	// (Here, time_values are the times that delimit the bins, and active_time_bins
	// is the number of bins used, typically time_values.length - 1.)

	public int get_survival_bins (double stop_fraction);




	// Get a fractile of a bin's highest magnitudes.
	// Parameters:
	//  n = Bin number.
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	//  f_total = True to get a fractile of the total number of catalogs,
	//            false to get a fractile of the catalogs that complete bin n.
	// Returns a magnitude M, such that, among all catalogs, the probability
	// that the highest magnitude in the catalog is <= M in bins 0 thru n
	// (inclusive) is approximately equal to fractile.
	// If fractile is larger than the fraction of catalogs that complete bin n,
	// then the return value is a large positive value (OEConstants.INFINITE_MAG_POS)
	// because no actual magnitude is available for catalogs that terminate
	// during or prior to bin n.
	// If fractile is smaller than the fraction of catalogs that have at least
	// one rupture by bin n, then the return value is a large negative value
	// (OEConstants.INFINITE_MAG_NEG) because no actual magnitude is available.
	// (Note that the appropriate bin number is often one less than the
	// return value of get_survival_bins()).

	public double get_high_mag_fractile (int n, double fractile, boolean f_total);




	// Get a fractile of a bin's highest magnitudes, using a second bin to select catalogs.
	// Parameters:
	//  n = Bin number.
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	//  n_sel = Bin used to select catalogs.
	// Returns a magnitude M, such that, among all catalogs that survive at least
	// to bin n_sel, the probability that the highest magnitude in the catalog
	// is <= M in bins 0 thru n (inclusive) is approximately equal to fractile.
	// If fractile is larger than the fraction of catalogs that complete bin n,
	// then the return value is a large positive value (OEConstants.INFINITE_MAG_POS)
	// because no actual magnitude is available for catalogs that terminate
	// during or prior to bin n.
	// If fractile is smaller than the fraction of catalogs that have at least
	// one rupture by bin n, then the return value is a large negative value
	// (OEConstants.INFINITE_MAG_NEG) because no actual magnitude is available.
	// (Note that the appropriate bin number n_sel is often one less than the
	// return value of get_survival_bins()).

	public double get_sel_high_mag_fractile (int n, double fractile, int n_sel);

}
