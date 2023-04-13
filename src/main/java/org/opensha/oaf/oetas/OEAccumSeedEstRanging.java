package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.AutoExecutorService;

import org.opensha.oaf.oetas.util.OEArraysCalc;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;


// Operational ETAS catalog accumulator for estimating catalog sizes on a generation/magnitude grid.
// Author: Michael Barall 04/10/2023.
//
// This accumulator only examines the seed generation, and so it is generally
// used with simulations that only generate the seeds.
//
// This object contains a 2D array of bins, each representing a combination of
// number of generations and minimum magnitudes.
//
// Each bin contains stacked Poisson distributions.  Each catalog yields a
// Poisson distribution for each bin, where the Poisson mean is derived from
// the combined productivity of the seeds, and multipled by a factor that
// depends on the number of generations.  If the number of generations is 2,
// then the Poisson mean is the expected number of direct aftershocks of the
// seeds.  The Poisson distributions are stacked by summing their probability
// density functions.
//
// Estimates are computed for the range of times specified in the catalog
// parameters.  Estimates of the effects of multiple generations are computed
// using the branch ratio implied by the catalog parameters.  Estimates are
// computed for an unlimited maximum magnitude, since a finite maximum would
// make little difference.

public class OEAccumSeedEstRanging implements OEEnsembleAccumulator, OEAccumReadoutSeedEst {

	//----- Code options -----

	// These options display expected number and count for each catalog for one time/magnitude bin 

	//private static final boolean dbg_out = false;	// true to enable output
	//private static final int dbg_gen_ix = -1;		// generation bin index, negative counts from end
	//private static final int dbg_mag_ix = 0;		// magnitude bin index, negative counts from end




	//----- Control variables -----

	// The stacked Poisson distribution cache.

	private OEStackedPoisson stacked_poisson;




	//----- Configuration variables -----

	// The time interval used for calculating branch ratios, in days.

	private double tint_br;

	// A de-rating factor applied to branch ratios, between 0 and 1.
	// This compensates for the fact that the upper part of the magnitude
	// range has few or no earthquakes, so the effective branch ratio is
	// less that the branch ratio given by the formula.  A de-rating
	// factor of 1 uses the formula value.

	private double derate_br;




	//----- Bin definitions -----

	// The number of generation bins.

	private int gen_bins;

	// The number of magnitude bins.

	private int mag_bins;

	// The generation values.
	// Dimension: gen_values[gen_bins]
	// Each generation bin represents the number of generations.
	// Each value must be >= 2.  If == 2, then it includes only direct aftershocks of the seeds.

	private int[] gen_values;

	// The magnitude values.
	// Dimension: mag_values[mag_bins]
	// Each generation bin represents a minimum magnitude.

	private double[] mag_values;




	//----- Accumulators -----


	// A class that holds an ensemble accumulator.
	// To avoid contention between threads, there are multiple of these objects,
	// so each thread can have its own.  There is also one for the entire ensemble.

	private class EnsembleAccum implements AutoCloseable {

		//--- Open/close ---

		// Flag indicates if we are currently open.
		// This is used to make the close() function idempotent.

		private boolean f_open;


		// Open the accumulator.

		public void open () {
			f_open = true;
			return;
		}


		// Closing puts this accumulator on the avaiability queue.

		@Override
		public void close() {
			if (f_open) {
				f_open = false;
				release_partial_acc (this);
			}
			return;
		}


		//--- Accumulators ---


		// The number of catalogs accumulated.

		public int acc_size;

		// The probability distributions.
		// Dimension: acc_distribution[gen_bins][mag_bins]
		// There is a separate probability distribution for each time/magnitude bin.

		public OEStackedPoisson.Accumulator[][] acc_distribution;


		//--- Construction ---


		// Make an object, initially open, and create the accumulators.
		// The accumulators are zero-initialized.

		public EnsembleAccum () {
			f_open = true;

			acc_size = 0;

			acc_distribution = new OEStackedPoisson.Accumulator[gen_bins][mag_bins];
			stacked_poisson.make_acc_array (acc_distribution);
		}


		// Clear the accumulators, and mark open.

		public final void clear_acc () {
			f_open = true;

			acc_size = 0;
			stacked_poisson.clear_acc_array (acc_distribution);
			return;
		}


		//--- Post-accumulation functions ---


		// Combine the counts from another accumulator into this one.

		public final void combine_with (EnsembleAccum other) {
			acc_size += other.acc_size;
			OEStackedPoisson.combine_acc_array (acc_distribution, other.acc_distribution);
			return;
		}


		// Cumulate the distributions.

		public final void cumulate () {
			OEStackedPoisson.cumulate_acc_array (acc_distribution);
			return;
		}


		//--- Accumulation functions ---


		// Increment the number of catalogs.

		public final void inc_catalog_count () {
			++acc_size;
			return;
		}


		// Add point mass to the distributions.
		// Parameters:
		//  gen_bin_lo = Lower limit of generation bin range, inclusive.
		//  gen_bin_hi = Upper limit of generation bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  value = Array containing the value of the point mass, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The value array must have the dimension [gen_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_point_mass (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, int[][] value, double weight) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_point_mass (value[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_point_mass (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, int[][] value) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_point_mass (value[time_ix][mag_ix]);
				}
			}
			return;
		}


		// Add Poisson distributions to the distributions.
		// Parameters:
		//  gen_bin_lo = Lower limit of generation bin range, inclusive.
		//  gen_bin_hi = Upper limit of generation bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  lambda = Array containing the mean of each Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The lambda array must have the dimension [gen_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, double weight) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_poisson (lambda[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_poisson (lambda[time_ix][mag_ix]);
				}
			}
			return;
		}


		// Add shifted Poisson distributions to the distributions.
		// Parameters:
		//  gen_bin_lo = Lower limit of generation bin range, inclusive.
		//  gen_bin_hi = Upper limit of generation bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  lambda = Array containing the mean of each Poisson distribution, must be >= 0.
		//  shift = Array containing the shift to apply to each Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The lambda and shift arrays must have the dimension [gen_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_shifted_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, int[][] shift, double weight) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_shifted_poisson (lambda[time_ix][mag_ix], shift[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_shifted_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, int[][] shift) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_shifted_poisson (lambda[time_ix][mag_ix], shift[time_ix][mag_ix]);
				}
			}
			return;
		}


		// Add Poisson distributions to the distributions, with separate mean for probability of occurrence.
		// Parameters:
		//  gen_bin_lo = Lower limit of generation bin range, inclusive.
		//  gen_bin_hi = Upper limit of generation bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  lambda = Array containing the mean of each Poisson distribution, must be >= 0.
		//  lam_occur = Array containing the mean of the Poisson distribution for probability of occurrence, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The lambda and lam_occur arrays must have the dimension [gen_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_split_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, double[][] lam_occur, double weight) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_split_poisson (lambda[time_ix][mag_ix], lam_occur[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_split_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, double[][] lam_occur) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_split_poisson (lambda[time_ix][mag_ix], lam_occur[time_ix][mag_ix]);
				}
			}
			return;
		}


		// Add shifted Poisson distributions to the distributions, with separate mean for probability of occurrence.
		// Parameters:
		//  gen_bin_lo = Lower limit of generation bin range, inclusive.
		//  gen_bin_hi = Upper limit of generation bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  lambda = Array containing the mean of each Poisson distribution, must be >= 0.
		//  lam_occur = Array containing the mean of the Poisson distribution for probability of occurrence, must be >= 0.
		//  shift = Array containing the shift to apply to each Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The lambda, lam_occur, and shift arrays must have the dimension [gen_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_shifted_split_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, double[][] lam_occur, int[][] shift, double weight) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_shifted_split_poisson (lambda[time_ix][mag_ix], lam_occur[time_ix][mag_ix], shift[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_shifted_split_poisson (int gen_bin_lo, int gen_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, double[][] lam_occur, int[][] shift) {
			for (int time_ix = gen_bin_lo; time_ix < gen_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_shifted_split_poisson (lambda[time_ix][mag_ix], lam_occur[time_ix][mag_ix], shift[time_ix][mag_ix]);
				}
			}
			return;
		}


		//--- Readout functions ---


		// Get the probability of occurrence for each bin.
		// Returns a 2D array, of dimension r[gen_bins][mag_bins].
		// Each element contains the probability that the value is > 0.

		public double[][] get_prob_occur () {
			return OEStackedPoisson.prob_occur_acc_array (acc_distribution);
		}


		// Get the mean for each bin.
		// Returns a 2D array, of dimension r[gen_bins][mag_bins].
		// Each element contains the mean.

		public double[][] get_mean () {
			return OEStackedPoisson.mean_acc_array (acc_distribution);
		}



		// Get a fractile for each bin.
		// Parameters:
		//  frac = Fractile to find, should be between 0.0 and 1.0.
		// Returns a 2D array, of dimension r[gen_bins][mag_bins].
		// Each element contains a value whose cumulative distribution function is > frac.
		// Note: The cumulate() function must have been called.

		public int[][] get_fractile (double frac) {
			return OEStackedPoisson.fractile_acc_array (acc_distribution, frac);
		}



		// Get the number of catalogs that contributed to each bin.
		// Returns a 2D array, of dimension r[gen_bins][mag_bins].
		// Each element contains the number of catalogs contributing to the bin.
		// Note: This function returns the total weight of each distribution rounded
		// to integer, since we add distributions with a weight of 1.0.
		// Note: In the current implementation, all bin sizes equal acc_size.

		public int[][] get_bin_size () {
			int[][] bin_size = new int[gen_bins][mag_bins];
			for (int time_ix = 0; time_ix < gen_bins; ++time_ix) {
				for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
					bin_size[time_ix][mag_ix] = (int)Math.round (acc_distribution[time_ix][mag_ix].get_total_weight());
				}
			}
			return bin_size;
		}

	}


	// The total accumulated values.

	private EnsembleAccum total_acc;

	// The list of available partial accumulators.

	private ConcurrentLinkedQueue<EnsembleAccum> partial_acc_list;


	// Get or make a partial accumulator.

	private EnsembleAccum get_partial_acc () {
		EnsembleAccum acc = partial_acc_list.poll();
		if (acc == null) {
			return new EnsembleAccum();
		}
		acc.open();
		return acc;
	}


	// Release a partial accumulator, and put it back on the queue.

	private void release_partial_acc (EnsembleAccum acc) {
		partial_acc_list.add (acc);
		return;
	}


	// Totalize all partial accumulators.

	private void totalize_partial_acc () {

		// Get the first accumulator off the list

		total_acc = partial_acc_list.poll();

		// If the list is empty, set up a zero total

		if (total_acc == null) {
			total_acc = new EnsembleAccum();
			return;
		}

		// Sum the remaining partial accumulators

		for (EnsembleAccum acc = partial_acc_list.poll(); acc != null; acc = partial_acc_list.poll()) {
			total_acc.combine_with (acc);
		}

		// And then delete the list itself

		partial_acc_list = null;
		return;
	}




	//----- Construction -----




	// Erase the contents.

	public final void clear () {
		stacked_poisson = null;

		tint_br = 0.0;
		derate_br = 0.0;

		gen_bins = 0;
		mag_bins = 0;
		gen_values = new int[0];
		mag_values = new double[0];

		total_acc = null;
		partial_acc_list = null;

		return;
	}




	// Default constructor.

	public OEAccumSeedEstRanging () {
		clear();
	}




	// Set up to begin accumulating.
	// Parameters:
	//	the_statcked_poisson = Stacked Poisson distribution cache, or null for default.
	//  seed_acc_meth = The seed accumulation method to use (currently not used, must be 0).
	//  the_tint_br = The time interval used for calculating branch ratios, in days.
	//  the_derate_br = A de-rating factor applied to branch ratios, typically between 0 and 1.
	//  the_gen_values = The generation values to define the bins, in days, must be in increasing order.
	//  the_mag_values = The magnitude values to define the bins, must be in increasing order.
	// Note: The function stores copies of the given arrays.
	// Note: Each generation value must be at least 2.  The value 2 includes only direct aftershocks of
	// the seeds.  In the current implementation, generation values should be small, perhaps no larger than 5.

	public void setup (OEStackedPoisson the_stacked_poisson, int seed_acc_meth, double the_tint_br, double the_derate_br, int[] the_gen_values, double[] the_mag_values) {

		// Parameter validation

		if (!( seed_acc_meth == 0 )) {
			throw new IllegalArgumentException ("OEAccumSeedEstRanging.setup: Invalid seed accumulation method: " + seed_acc_meth);
		}

		if (!( the_gen_values != null && the_gen_values.length >= 1 )) {
			throw new IllegalArgumentException ("OEAccumSeedEstRanging.setup: Missing generation values");
		}

		for (int i = 0; i+1 < the_gen_values.length; ++i) {
			if (!( the_gen_values[i] < the_gen_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumSeedEstRanging.setup: Out-of-order generation values: index = " + i + ", value[i] = " + the_gen_values[i] + ", value[i+1] = " + the_gen_values[i+1]);
			}
		}

		if (!( the_mag_values != null && the_mag_values.length >= 1 )) {
			throw new IllegalArgumentException ("OEAccumSeedEstRanging.setup: Missing magnitude values");
		}

		for (int i = 0; i+1 < the_mag_values.length; ++i) {
			if (!( the_mag_values[i] < the_mag_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumSeedEstRanging.setup: Out-of-order magnitude values: index = " + i + ", value[i] = " + the_mag_values[i] + ", value[i+1] = " + the_mag_values[i+1]);
			}
		}

		// Copy parameters

		stacked_poisson = ((the_stacked_poisson != null) ? the_stacked_poisson : OEStackedPoisson.get_singleton());

		tint_br = the_tint_br;
		derate_br = the_derate_br;

		gen_bins = the_gen_values.length;
		mag_bins = the_mag_values.length;
		gen_values = Arrays.copyOf (the_gen_values, the_gen_values.length);
		mag_values = Arrays.copyOf (the_mag_values, the_mag_values.length);

		// Empty accumulators

		total_acc = null;
		partial_acc_list = null;

		return;
	}




	//----- Consumers -----




	// Consumer base class implementation.
	//
	// The general design of a consumer is that it contains an array of bins,
	// each corresponding to one combination of generation count and minimum
	// magnitude.  Each bin contains a stacked Poisson distribution, which
	// is an estimate of the catalog size.

	private class ConsumerBase implements OECatalogConsumer {

		//----- Control variables -----

		// True if consumer is open.

		protected boolean f_open;

		//----- Accumulators, for the entire catalog -----

		// The accumulated expected values, for the entire catalog.
		// Dimension: csr_expected[gen_bins][mag_bins]
		// Each bin contains the total expected number of ruptures.

		protected double[][] csr_expected;

		//----- Per-catalog variables -----

		// The generation multiplier, for each generation value.
		// Contains SUM( dbr^(j-2) )
		// where dbr is the de-rated branch ratio, and the sum runs over
		// 2 <= j <= gen_values[gen_ix].
		// Dimension: cat_gen_multiplier[gen_bins]

		protected double[] cat_gen_multiplier;

		// The magnitude integral, for each minimum magnitude value.
		// Contains Integral(m1, infinity, (b*log(10)*10^(-b*(m - mref)))*dm)
		// where m1 is the corresponding entry of mag_values.
		// Dimension: cat_mag_int[mag_bins]

		protected double[] cat_mag_int;

		//----- Per-catalog variables -----

		// Rate, for the current generation.
		// Contains the sum of the rates:
		//  r = SUM( k * Integral(((t-t0+c)^(-p))) )
		// where k is the rupture productivity, t0 is the rupture time,
		// p and c are the Omori parameters, the sum runs over all ruptures
		// in the current generation, and each integral runs over the
		// portion of the time range that comes after the rupture
		// and before the time horizon.
		// With this definition, the rate of ruptures in the time range per
		// unit magnitude is:
		//  r * b * log(10) * (10^(-b*(m - mref)))
		// where b is the Gutenberg-Richter parameter and mref is the reference
		// magnitude.

		protected double cur_gen_rate;

		//----- Reporting -----

		// True to report expected rates.

//		protected boolean f_report_expected;




		//----- Subroutines -----




		// Return true if the catalog is being skipped.

		protected final boolean is_cat_skipped () {
			return false;
		}




		// Perform setup at the start of a catalog.

		protected void cat_setup (OECatalogScanComm comm) {

			// Zero the per-catalog rates

			OEArraysCalc.zero_array (csr_expected);

			// Get the de-rated branch ratio for the catalog

			double dbr = derate_br * OEStatsCalc.calc_branch_ratio (comm.cat_params, tint_br);

			// Get the multiplier for each generation count

			for (int gen_ix = 0; gen_ix < gen_bins; ++gen_ix) {
				double x = 1.0;
				for (int j = 2; j < gen_values[gen_ix]; ++ j) {
					x = (x * dbr) + 1.0;
				}
				cat_gen_multiplier[gen_ix] = x;
			}

			// Get magnitude integral for each minimum magnitude
			// Note: We use an unbounded maximum magnitude

			for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
				cat_mag_int[mag_ix] = OERandomGenerator.gr_rate_unbounded (
					comm.cat_params.b,				// b
					comm.cat_params.mref,			// mref
					mag_values[mag_ix]				// m1
				);
			}

			return;
		}




		// Perform setup at the start of a generation.

		protected void gen_setup (OECatalogScanComm comm) {

			// Zero the per-generation rate

			cur_gen_rate = 0.0;

			return;
		}




		// Finish at the end of a generation.

		protected void gen_finish (OECatalogScanComm comm) {

			// Scale the rates according to generation and magnitude, and add to catalog accumulators

			for (int gen_ix = 0; gen_ix < gen_bins; ++gen_ix) {
				for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
					csr_expected[gen_ix][mag_ix] += (cur_gen_rate * cat_mag_int[mag_ix] * cat_gen_multiplier[gen_ix]);
				}
			}

			return;
		}




		// Finish at the end of a catalog.
		// This also sets the reporting variables.

		protected void cat_finish (OECatalogScanComm comm) {

			return;
		}




		//----- Construction -----




		// Clear to the inactive state.

		protected final void clear_base() {

			OEArraysCalc.zero_array (csr_expected);

			OEArraysCalc.zero_array (cat_gen_multiplier);
			OEArraysCalc.zero_array (cat_mag_int);

			cur_gen_rate = 0.0;

			return;
		}




		// Default constructor.

		public ConsumerBase () {
			f_open = false;

			csr_expected = new double[gen_bins][mag_bins];

			cat_gen_multiplier = new double[gen_bins];
			cat_mag_int = new double[mag_bins];

			clear_base();
		}




		//----- Open/Close methods (Implementation of OECatalogConsumer) -----




		// Open the catalog consumer.
		// Perform any setup needed to begin consuming a catalog.

		@Override
		public void open () {
			return;
		}

		// Close the catalog consumer.
		// Perform any final tasks needed to finish consuming a catalog,
		// such as storing results into an accumulator.

		@Override
		public void close () {
			return;
		}




		//----- Data methods (Implementation of OECatalogConsumer) -----




		// Begin consuming a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog values set up.

		@Override
		public void begin_catalog (OECatalogScanComm comm) {

			// Set up per-catalog accumulators

			cat_setup (comm);

			// Mark it open

			f_open = true;
			
			return;
		}




		// End consuming a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog values set up.

		@Override
		public void end_catalog (OECatalogScanComm comm) {

			// If open ...

			if (f_open) {
			
				// Mark it closed

				f_open = false;

				// Stop if skipping catalog

				if (is_cat_skipped()) {
					return;
				}

				// Finish at the end of a catalog, and set reporting variables

				cat_finish (comm);

				// Get an accumulator

				try (
					EnsembleAccum ens_accum = get_partial_acc();
				) {

					// Count the catalog

					ens_accum.inc_catalog_count();

					// Accumulate Poisson distributions

					ens_accum.add_poisson (0, gen_bins, 0, mag_bins, csr_expected);

					// Dsiplay expected number and count for one time/magnitude bin, for debugging

					//if (dbg_out) {
					//	synchronized (OEAccumSeedEstRanging.this) {
					//		int g_ix = ((dbg_gen_ix >= 0) ? (dbg_gen_ix) : (active_gen_bins + dbg_gen_ix));
					//		int m_ix = ((dbg_mag_ix >= 0) ? (dbg_mag_ix) : (active_mag_bins + dbg_mag_ix));
					//		System.out.println (String.format ("%.3f  %.3f  %d", csr_expected[g_ix][m_ix]));
					//	}
					//}
				}
			}

			return;
		}




		// Begin consuming the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_seed_generation (OECatalogScanComm comm) {

			// Stop if skipping catalog

			if (is_cat_skipped()) {
				return;
			}

			// Set up for generation

			gen_setup (comm);
			return;
		}




		// End consuming the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_seed_generation (OECatalogScanComm comm) {

			// Stop if skipping catalog

			if (is_cat_skipped()) {
				return;
			}

			// Finish for generation

			gen_finish (comm);
			return;
		}




		// Next rupture in the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_seed_rup (OECatalogScanComm comm) {

			// Add rate for this rupture
			// (Note that omori_rate_shifted() properly handles the case comm.rup.t_day == BKGD_TIME_DAYS)

			cur_gen_rate += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
				comm.cat_params.p,		// p
				comm.cat_params.c,		// c
				comm.rup.t_day,			// t0
				comm.cat_params.teps,	// teps
				comm.cat_params.tbegin,	// t1
				comm.cat_params.tend	// t2
			));

			return;
		}




		// Begin consuming the next generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_generation (OECatalogScanComm comm) {

			// Stop if skipping catalog

			if (is_cat_skipped()) {
				return;
			}

			return;
		}




		// End consuming a generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_generation (OECatalogScanComm comm) {

			// Stop if skipping catalog

			if (is_cat_skipped()) {
				return;
			}

			return;
		}




		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {

			return;
		}




		// Next sterile rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_sterile_rup (OECatalogScanComm comm) {

			return;
		}

	}




	//----- Implementation of OEEnsembleAccumulator -----




	// Make a catalog consumer.
	// Returns a consumer which is able to consume the contents of one catalog
	// (or several catalogs in succession).
	// This function may be called repeatedly to create several consumers,
	// which can be used in multiple worker threads.
	// Threading: Can be called in multiple threads, before or after the call to
	// begin_accumulation, and while there are existing open consumers, and so
	// must be properly synchronized.
	// Note: The returned consumer cannot be opened until after the call to
	// begin_accumulation, and must be closed before the call to end_accumulation.
	// Note: The returned consumer can be opened and closed repeatedly to consume
	// multiple catalogs.

	@Override
	public OECatalogConsumer make_consumer () {
		OECatalogConsumer consumer = new ConsumerBase();
		return consumer;
	}




	// Begin accumulating catalogs.
	// Parameters:
	//  capacity = The number of catalogs that will be accumulated.
	// This function should be called before any other control methods.
	// The accumulator should allocate resources so it can hold results
	// from at least the specified number of catalogs.
	// Threading: No other thread should be accessing this object,
	// and none of its consumers can be open.
	// Design note: The number of catalogs is specified in advance, because
	// automatically increasing the capacity on-demand is likely to require
	// expensive synchronization, and typically the expected number of catalogs
	// is known.

	@Override
	public void begin_accumulation (int capacity) {

		// Initialize the accumulators

		total_acc = null;
		partial_acc_list = new ConcurrentLinkedQueue<EnsembleAccum>();

		return;
	}




	// Increase the capacity of the accumulator.
	// Parameters:
	//  capacity = The number of catalogs that will be accumulated.
	// This function can be called to increase the capacity of the accumulator
	// above its original or prior setting.
	// Note that this is likely to be an expensive operation, in part because
	// all worker threads must be idled.
	// Threading: No other thread should be accessing this object,
	// and none of its consumers can be open.

	@Override
	public void increase_capacity (int capacity) {
		return;
	}




	// End accumulating catalogs.
	// This function should be called after all other control functions.
	// It provides an opportunity for the accumulator to finish its binning.
	// Note that the final number of catalogs can be less than the configured capacity.
	// Threading: No other thread should be accessing this object,
	// and none of its consumers can be open.

	@Override
	public void end_accumulation () {

		// Total all the partial accumulators

		totalize_partial_acc();

		// Cumulate the distributions

		total_acc.cumulate();

		return;
	}




	//----- Readout functions -----




	// Get the number of generation bins.

	public final int get_gen_bins () {
		return gen_bins;
	}


	// Get the number of magnitude bins.

	public final int get_mag_bins () {
		return mag_bins;
	}


	// Get a generation value.

	public final int get_gen_value (int n) {
		return gen_values[n];
	}


	// Get a magnitude value.

	public final double get_mag_value (int n) {
		return mag_values[n];
	}


	// Get the size, which is the number of catalogs accumulated.

	public final int get_size () {
		return total_acc.acc_size;
	}




	// Get a fractile.
	// Parameters:
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns a 2D array with dimensions r[gen_bins][mag_bins].
	// Each element corresponds to one combination of generations and magnitude.
	// Each value is an integer N, such that the probability of N or fewer
	// ruptures in the catalog is approximately equal to fractile.
	// Note: The returned array should be newly-allocated and not retained by this object.

	@Override
	public final int[][] get_fractile_array (double fractile) {
		return total_acc.get_fractile (fractile);
	}




	// Get the probability of occurrence array.
	// Returns a 2D array with dimensions r[gen_bins][mag_bins].
	// Each element corresponds to one combination of generations and magnitude.
	// Each value is a real number v, such that v is the probability that
	// there is at least one rupture in the catalog.

	public final double[][] get_prob_occur_array () {
		return total_acc.get_prob_occur();
	}




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

	@Override
	public final double[][] get_prob_occur_array (int xcount) {
		if (xcount != 0) {
			throw new UnsupportedOperationException ("OEAccumSeedEstRanging.get_prob_occur_array: Unsupported exceedence count: xcount = " + xcount);
		}
		return total_acc.get_prob_occur();
	}




	// Convert a fractile array to a string.
	// Parameters:
	//  fractile_array = Fractile array as returned by get_fractile_array().
	// Table layout is:
	//  7 cols for magnitude
	//  2 cols spacer
	//  10 cols for time/value
	//  2 cols spacer ...
	// Note: The parameter can be any array of dimension int[gen_bins][mag_bins].

	public final String fractile_array_to_string (int[][] fractile_array) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (" M \\ G ");
		for (int gen_ix = 0; gen_ix < gen_bins; ++gen_ix) {
			result.append (String.format ("  %10d", gen_values[gen_ix]));
		}
		result.append ("\n");

		// Data lines

		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			result.append (String.format ("%7.3f", mag_values[mag_ix]));
			for (int gen_ix = 0; gen_ix < gen_bins; ++gen_ix) {
				result.append (String.format ("  %10d", fractile_array[gen_ix][mag_ix]));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	// Convert a probability of occurrence array, as percentages, to a string.
	// Parameters:
	//  prob_occur_array = Probability of occurence array as returned by get_prob_occur_array().
	// Table layout is:
	//  7 cols for magnitude
	//  2 cols spacer
	//  10 cols for time/value
	//  2 cols spacer ...
	// Note: The parameter can be any array of dimension double[gen_bins][mag_bins]
	// where fixed-point display to 3 decimal places is appropriate.

	public final String prob_occur_array_to_string (double[][] prob_occur_array) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (" M \\ G ");
		for (int gen_ix = 0; gen_ix < gen_bins; ++gen_ix) {
			result.append (String.format ("  %10d", gen_values[gen_ix]));
		}
		result.append ("\n");

		// Data lines

		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			result.append (String.format ("%7.3f", mag_values[mag_ix]));
			for (int gen_ix = 0; gen_ix < gen_bins; ++gen_ix) {
				result.append (String.format ("  %10.5f", prob_occur_array[gen_ix][mag_ix] * 100.0));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	//----- Testing -----




	// Set up using typical values for generation and magnitude.
	// Parameters:
	//  mag_main = Mainshock magnitude, used for setting up the magnitude range.
	//  seed_acc_meth = The seed accumulation method to use (currently not used, must be 0).
	//  the_tint_br = The time interval used for calculating branch ratios, in days.
	//  the_derate_br = A de-rating factor applied to branch ratios, typically between 0 and 1.

	public void typical_test_setup (double mag_main, int seed_acc_meth, double the_tint_br, double the_derate_br) {
	
		// Make generation array for seed-only up to 4 additional generations

		int[] the_gen_values = new int[5];
		the_gen_values[0] = 2;
		the_gen_values[1] = 3;
		the_gen_values[2] = 4;
		the_gen_values[3] = 5;
		the_gen_values[4] = 6;

		// Make magnitude array for intervals of 0.1, from -6.0 to +2.0 relative to mainshock magnitude

		double[] the_mag_values = new double[81];
		for (int j = 0; j <= 80; ++j) {
			the_mag_values[j] = ((double)(j - 60))/10.0 + mag_main;
		}

		// Do the setup

		setup (null, seed_acc_meth, the_tint_br, the_derate_br, the_gen_values, the_mag_values);
		return;
	}




	// Create a typical set of test outputs, as a string.

	public String typical_test_outputs_to_string () {
		StringBuilder result = new StringBuilder();

		int[][] fractile_array;
		double[][] prob_occur_array;

		// Number of catalog processed

		result.append ("\n");
		result.append ("Catalogs = " + get_size() + "\n");
		result.append ("\n");

		// 0% fractile (minimum over all catalogs)

		result.append ("\n");
		result.append ("0% fractile (minimum)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.0);
		result.append (fractile_array_to_string (fractile_array));

		// 1% fractile

		result.append ("\n");
		result.append ("1% fractile\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.01);
		result.append (fractile_array_to_string (fractile_array));

		// 2.5% fractile

		result.append ("\n");
		result.append ("2.5% fractile\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.025);
		result.append (fractile_array_to_string (fractile_array));

		// 50% fractile (median)

		result.append ("\n");
		result.append ("50% fractile (median)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.50);
		result.append (fractile_array_to_string (fractile_array));

		// 97.5% fractile

		result.append ("\n");
		result.append ("97.5% fractile\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.975);
		result.append (fractile_array_to_string (fractile_array));

		// 99% fractile

		result.append ("\n");
		result.append ("99% fractile\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.99);
		result.append (fractile_array_to_string (fractile_array));

		// 100% fractile (maximum over all catalogs)

		result.append ("\n");
		result.append ("100% fractile (maximum)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (1.0);
		result.append (fractile_array_to_string (fractile_array));

		// Probability of occurrence

		result.append ("\n");
		result.append ("Probability of occurrence\n");
		result.append ("\n");

		prob_occur_array = get_prob_occur_array ();
		result.append (prob_occur_array_to_string (prob_occur_array));

		return result.toString();
	}




	// Perform a typical test run.
	// Parameters:
	//  test_cat_params = Catalog parameters.
	//  mag_main = Mainshock magnitude.
	//  seed_acc_meth = The seed accumulation method to use (currently not used, must be 0).
	//  the_tint_br = The time interval used for calculating branch ratios, in days.
	//  the_derate_br = A de-rating factor applied to branch ratios, typically between 0 and 1.
	//  num_cats = Number of catalogs to run.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run (OECatalogParams test_cat_params, double mag_main, int seed_acc_meth, double the_tint_br, double the_derate_br, int num_cats) {

		// Say hello

		System.out.println ();
		System.out.println ("Generating " + num_cats + " catalogs");
		System.out.println ();

		// Make the accumulator and set up the bins

		OEAccumSeedEstRanging seed_est_accum = new OEAccumSeedEstRanging();
		seed_est_accum.typical_test_setup (mag_main, seed_acc_meth, the_tint_br, the_derate_br);

		// Create the array of accumulators

		OEEnsembleAccumulator[] accumulators = new OEEnsembleAccumulator[1];
		accumulators[0] = seed_est_accum;

		// Begin accumulation, passing the number of catalogs

		for (OEEnsembleAccumulator accumulator : accumulators) {
			accumulator.begin_accumulation (num_cats);
		}

		// Get the random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Create a scanner for our accumulators, which we re-use for each catalog

		OECatalogScanner cat_scanner = new OECatalogScanner();
		cat_scanner.setup (accumulators);

		// Allocate the storage (which is also the builder), which we re-use for each catalog

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Allocate a generator, which we re-use for each catalog

		OECatalogGenerator cat_generator = new OECatalogGenerator();

		// Loop over number of catalogs ...

		for (int ncat = 0; ncat < num_cats; ++ncat) {

			// Begin the catalog

			cat_storage.begin_catalog (test_cat_params);

			// Begin the first generation

			OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
				test_cat_params.mref,	// gen_mag_min
				test_cat_params.msup	// gen_mag_max
			);

			cat_storage.begin_generation (test_gen_info);

			// Insert the mainshock rupture

			OERupture mainshock_rup = new OERupture();

			double k_prod = OEStatsCalc.calc_k_corr (
				mag_main,			// m0
				test_cat_params,	// cat_params
				test_gen_info		// gen_info
			);

			mainshock_rup.set (
				0.0,			// t_day
				mag_main,		// rup_mag
				k_prod,			// k_prod
				RUPPAR_SEED,	// rup_parent
				0.0,			// x_km
				0.0				// y_km
			);

			cat_storage.add_rup (mainshock_rup);

			// End the first generation

			cat_storage.end_generation();

			// Set up the catalog generator
				
			cat_generator.setup (rangen, cat_storage, false);

			// Calculate all generations and end the catalog

			cat_generator.calc_all_gen();

			// Tell the generator to forget the catalog

			cat_generator.forget();

			// Report

			System.out.println (ncat + ": gens = " + cat_storage.get_gen_count() + ", size = " + cat_storage.size());
		
			// Open the consumers

			cat_scanner.open();

			// Scan the catalog

			cat_scanner.scan (cat_storage, rangen);

			// Close the consumers

			cat_scanner.close();
		}

		// End accumulation

		for (OEEnsembleAccumulator accumulator : accumulators) {
			accumulator.end_accumulation ();
		}

		// Display results

		System.out.println (seed_est_accum.typical_test_outputs_to_string());

		return;
	}




	// Perform a typical test run, multi-threaded version.
	// Parameters:
	//  test_cat_params = Catalog parameters.
	//  mag_main = Mainshock magnitude.
	//  seed_acc_meth = The seed accumulation method to use (currently not used, must be 0).
	//  the_tint_br = The time interval used for calculating branch ratios, in days.
	//  the_derate_br = A de-rating factor applied to branch ratios, typically between 0 and 1.
	//  num_cats = Number of catalogs to run.
	//  num_threads = Number of threads to use, can be -1 for default number of threads.
	//  max_runtime = Maximum running time allowed.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run_mt (OECatalogParams test_cat_params, double mag_main, int seed_acc_meth, double the_tint_br, double the_derate_br, int num_cats, int num_threads, long max_runtime) {

		// Say hello

		int actual_num_threads = num_threads;
		if (actual_num_threads == AutoExecutorService.AESNUM_DEFAULT) {
			actual_num_threads = AutoExecutorService.get_default_num_threads();
		}

		System.out.println ();
		System.out.println ("Generating " + num_cats + " catalogs");
		System.out.println ("Using " + actual_num_threads + " threads");
		System.out.println ("With " + max_runtime + " maximum runtime");
		System.out.println ();

		// Make the accumulator and set up the bins

		OEAccumSeedEstRanging seed_est_accum = new OEAccumSeedEstRanging();
		seed_est_accum.typical_test_setup (mag_main, seed_acc_meth, the_tint_br, the_derate_br);

		// Create the list of accumulators

		ArrayList<OEEnsembleAccumulator> accumulators = new ArrayList<OEEnsembleAccumulator>();
		accumulators.add (seed_est_accum);

		// Create the first generation info

		OEGenerationInfo test_gen_info = (new OEGenerationInfo()).set (
			test_cat_params.mref,	// gen_mag_min
			test_cat_params.msup	// gen_mag_max
		);

		// Create the mainshock rupture

		OERupture mainshock_rup = new OERupture();

		double k_prod = OEStatsCalc.calc_k_corr (
			mag_main,			// m0
			test_cat_params,	// cat_params
			test_gen_info		// gen_info
		);

		mainshock_rup.set (
			0.0,			// t_day
			mag_main,		// rup_mag
			k_prod,			// k_prod
			RUPPAR_SEED,	// rup_parent
			0.0,			// x_km
			0.0				// y_km
		);

		// Create the list of seed ruptures

		ArrayList<OERupture> seed_ruptures = new ArrayList<OERupture>();
		seed_ruptures.add (mainshock_rup);

		// Create the initializer

		OEInitFixedState initializer = new OEInitFixedState();
		initializer.setup (test_cat_params, test_gen_info, seed_ruptures);

		// Set up the ensemble parameters

		OEEnsembleParams ensemble_params = new OEEnsembleParams();

		ensemble_params.set (
			initializer,		// initializer
			accumulators,		// accumulators
			num_cats			// num_catalogs
		);

		// Create the ensemble generator

		OEEnsembleGenerator ensemble_generator = new OEEnsembleGenerator();

		// Generate the catalogs

		long progress_time = 10000L;
		ensemble_generator.generate_all_catalogs (ensemble_params, num_threads, max_runtime, progress_time);

		// Display results

		System.out.println (seed_est_accum.typical_test_outputs_to_string());

		return;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEAccumSeedEstRanging : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  mag_main  tbegin  num_cats  seed_acc_meth  tint_br  derate_br
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note: Mainshock time is 0, so tbegin is the time after mainshock.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumSeedEstRanging : Invalid 'test1' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				int num_cats = Integer.parseInt (args[8]);
				int seed_acc_meth = Integer.parseInt (args[9]);
				double the_tint_br = Double.parseDouble (args[10]);
				double the_derate_br = Double.parseDouble (args[11]);

				// Say hello

				System.out.println ("Test seed estimate ranging with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("seed_acc_meth = " + seed_acc_meth);
				System.out.println ("the_tint_br = " + the_tint_br);
				System.out.println ("the_derate_br = " + the_derate_br);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				test_cat_params.gen_count_max = 1;

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Do the test run

				typical_test_run (test_cat_params, mag_main, seed_acc_meth, the_tint_br, the_derate_br, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  mag_main  tbegin  num_cats  seed_acc_meth  tint_br  derate_br
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note: Mainshock time is 0, so tbegin is the time after mainshock.
		// Same as test #1, but uses multi-threaded code.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumSeedEstRanging : Invalid 'test2' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				int num_cats = Integer.parseInt (args[8]);
				int seed_acc_meth = Integer.parseInt (args[9]);
				double the_tint_br = Double.parseDouble (args[10]);
				double the_derate_br = Double.parseDouble (args[11]);

				// Say hello

				System.out.println ("Test seed estimate ranging with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("seed_acc_meth = " + seed_acc_meth);
				System.out.println ("the_tint_br = " + the_tint_br);
				System.out.println ("the_derate_br = " + the_derate_br);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				test_cat_params.gen_count_max = 1;

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Do the test run

				int num_threads = -1;
				long max_runtime = -1L;

				typical_test_run_mt (test_cat_params, mag_main, seed_acc_meth, the_tint_br, the_derate_br, num_cats, num_threads, max_runtime);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEAccumSeedEstRanging : Unrecognized subcommand : " + args[0]);
		return;

	}

}
