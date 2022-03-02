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

import static org.opensha.oaf.oetas.OEConstants.MAGFILL_METH_MIN;
import static org.opensha.oaf.oetas.OEConstants.MAGFILL_METH_NONE;
import static org.opensha.oaf.oetas.OEConstants.MAGFILL_METH_PDF_ONLY;
import static org.opensha.oaf.oetas.OEConstants.MAGFILL_METH_PDF_HYBRID;
import static org.opensha.oaf.oetas.OEConstants.MAGFILL_METH_PDF_STERILE;
import static org.opensha.oaf.oetas.OEConstants.MAGFILL_METH_MAX;

import static org.opensha.oaf.oetas.OEConstants.OUTFILL_METH_MIN;
import static org.opensha.oaf.oetas.OEConstants.OUTFILL_METH_NONE;
import static org.opensha.oaf.oetas.OEConstants.OUTFILL_METH_OMIT;
import static org.opensha.oaf.oetas.OEConstants.OUTFILL_METH_PDF_DIRECT;
import static org.opensha.oaf.oetas.OEConstants.OUTFILL_METH_MAX;

import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_MIN;
import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_ANY;
import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_ANY_CLIP;
import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_RANGE;
import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_ENTIRE;
import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_ENTIRE_CLIP;
import static org.opensha.oaf.oetas.OEConstants.CATLEN_METH_MAX;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;


// Operational ETAS catalog accumulator for a cumulative time/magnitude grid.
// Author: Michael Barall 02/01/2022.
//
// This object contains a 2D array of bins, each representing a range of
// time and magnitude.  Bins are cumulative, and include all ruptures at
// earlier times and greater magnitudes.
//
// The contents of each bin is an array of stacked shifted Poisson
// distributions.  Each catalog yields a shifted Poisson distribution for
// each bin, where the Poisson mean is derived from expected aftershock
// rates, and the Poisson shift is a count of events in the catalog.

public class OEAccumRateTimeMag implements OEEnsembleAccumulator {

	//----- Control variables -----

	// The stacked Poisson distribution cache.

	private OEStackedPoisson stacked_poisson;

	// The magnitude fill method used (see OEConstants.MAGFILL_METH_XXXXX).

	private int magfill_meth;

	// The outfill method used (see OEConstants.OUTFILL_METH_XXXXX).

	private int outfill_meth;

	// The catalog length method used (see OEConstants.CATLEN_METH_XXXXX).

	private int catlen_meth;

	// The number of time bins that are used for reporting ruptures.

	private int active_time_bins;

	// The number of magnitude bins that are used for reporting ruptures.

	private int active_mag_bins;

	// True to skip rate calculation in last generation.

	private boolean f_skip_last_gen_rate;

	// Minimum required stop time for a catalog.

	private double min_cat_stop_time;




	//----- Bin definitions -----

	// The number of time bins.

	private int time_bins;

	// The number of magnitude bins.

	private int mag_bins;

	// The time values, in days.
	// Dimension: time_values[time_bins + 1]
	// Each time bin represents an interval between two successive time values.

	private double[] time_values;

	// The magnitude values.
	// Dimension: mag_values[mag_bins + 1]
	// Each time bin represents an interval between two successive magnitude values.
	// Note: Typically the last magnitude is very large.  Its inclusion simplfies the code.

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

		// The number of catalogs that are live within each time bin.
		// Dimension: acc_live_counts[time_bins]
		// A catalog is considered live if events within the bin are being counted.

		public int[] acc_live_counts;

		// The probability distributions.
		// Dimension: partial_accum[time_bins][mag_bins]
		// There is a separate probability distribution for each time/magnitude bin.

		public OEStackedPoisson.Accumulator[][] acc_distribution;


		//--- Construction ---


		// Make an object, initially open, and create the accumulators.
		// The accumulators are zero-initialized.

		public EnsembleAccum () {
			f_open = true;

			acc_size = 0;

			acc_live_counts = new int[time_bins];
			OEArraysCalc.zero_array (acc_live_counts);

			acc_distribution = new OEStackedPoisson.Accumulator[time_bins][mag_bins];
			stacked_poisson.make_acc_array (acc_distribution);
		}


		// Clear the accumulators, and mark open.

		public final void clear_acc () {
			f_open = true;

			acc_size = 0;

			OEArraysCalc.zero_array (acc_live_counts);

			stacked_poisson.clear_acc_array (acc_distribution);
			return;
		}


		//--- Post-accumulation functions ---


		// Combine the counts from another accumulator into this one.

		public final void combine_with (EnsembleAccum other) {
			acc_size += other.acc_size;
			for (int j = 0; j < time_bins; ++j) {
				acc_live_counts[j] += other.acc_live_counts[j];
			}
			OEStackedPoisson.combine_acc_array (acc_distribution, other.acc_distribution);
			return;
		}


		// Cumulate the distributions.

		public final void cumulate () {
			OEArraysCalc.cumulate_array (acc_live_counts, false) ;
			OEStackedPoisson.cumulate_acc_array (acc_distribution);
			return;
		}


		//--- Accumulation functions ---


		// Add the counted time bins.
		// Parameters:
		//  counted_bins = Number of time bins being counted in the current catalog.
		// This also increments the total catalog count.

		public final void add_counted_bin_count (int counted_bins) {
			if (counted_bins > 0) {
				acc_live_counts[counted_bins - 1]++;
			}
			++acc_size;
			return;
		}


		// Add point mass to the distributions.
		// Parameters:
		//  time_bin_lo = Lower limit of time bin range, inclusive.
		//  time_bin_hi = Upper limit of time bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  value = Array containing the value of the point mass, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The value array must have the dimension [time_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_point_mass (int time_bin_lo, int time_bin_hi, int mag_bin_lo, int mag_bin_hi, int[][] value, double weight) {
			for (int time_ix = time_bin_lo; time_ix < time_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_point_mass (value[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_point_mass (int time_bin_lo, int time_bin_hi, int mag_bin_lo, int mag_bin_hi, int[][] value) {
			for (int time_ix = time_bin_lo; time_ix < time_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_point_mass (value[time_ix][mag_ix]);
				}
			}
			return;
		}


		// Add Poisson distributions to the distributions.
		// Parameters:
		//  time_bin_lo = Lower limit of time bin range, inclusive.
		//  time_bin_hi = Upper limit of time bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  lambda = Array containing the mean of each Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The lambda array must have the dimension [time_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_poisson (int time_bin_lo, int time_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, double weight) {
			for (int time_ix = time_bin_lo; time_ix < time_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_poisson (lambda[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_poisson (int time_bin_lo, int time_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda) {
			for (int time_ix = time_bin_lo; time_ix < time_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_poisson (lambda[time_ix][mag_ix]);
				}
			}
			return;
		}


		// Add shifted Poisson distributions to the distributions.
		// Parameters:
		//  time_bin_lo = Lower limit of time bin range, inclusive.
		//  time_bin_hi = Upper limit of time bin range, exclusive.
		//  mag_bin_lo = Lower limit of magnitude bin range, inclusive.
		//  mag_bin_hi = Upper limit of magnitude bin range, exclusive.
		//  lambda = Array containing the mean of each Poisson distribution, must be >= 0.
		//  shift = Array containing the shift to apply to each Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
		// The lambda and shift arrays must have the dimension [time_bins][mag_bins].
		// The same weight is used for all accumulators.

		public void add_shifted_poisson (int time_bin_lo, int time_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, int[][] shift, double weight) {
			for (int time_ix = time_bin_lo; time_ix < time_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_shifted_poisson (lambda[time_ix][mag_ix], shift[time_ix][mag_ix], weight);
				}
			}
			return;
		}


		public void add_shifted_poisson (int time_bin_lo, int time_bin_hi, int mag_bin_lo, int mag_bin_hi, double[][] lambda, int[][] shift) {
			for (int time_ix = time_bin_lo; time_ix < time_bin_hi; ++time_ix) {
				for (int mag_ix = mag_bin_lo; mag_ix < mag_bin_hi; ++mag_ix) {
					acc_distribution[time_ix][mag_ix].add_shifted_poisson (lambda[time_ix][mag_ix], shift[time_ix][mag_ix]);
				}
			}
			return;
		}


		//--- Readout functions ---


		// Get the probability of occurrence for each bin.
		// Returns a 2D array, of dimension r[time_bins][mag_bins].
		// Each element contains the probability that the value is > 0.

		public double[][] get_prob_occur () {
			return OEStackedPoisson.prob_occur_acc_array (acc_distribution);
		}



		// Get a fractile for each bin.
		// Parameters:
		//  frac = Fractile to find, should be between 0.0 and 1.0.
		// Returns a 2D array, of dimension r[time_bins][mag_bins].
		// Each element contains a value whose cumulative distribution function is > frac.
		// Note: The cumulate() function must have been called.

		public int[][] get_fractile (double frac) {
			return OEStackedPoisson.fractile_acc_array (acc_distribution, frac);
		}



		// Get the number of catalogs that contributed to each bin.
		// Returns a 2D array, of dimension r[time_bins][mag_bins].
		// Each element contains the number of catalogs contributing to the bin.
		// Note: This function returns the total weight of each distribution rounded
		// to integer, since we add distributions with a weight of 1.0.

		public int[][] get_bin_size () {
			int[][] bin_size = new int[time_bins][mag_bins];
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
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
		magfill_meth = 0;
		outfill_meth = 0;
		catlen_meth = 0;
		active_time_bins = 0;
		active_mag_bins = 0;
		f_skip_last_gen_rate = false;
		min_cat_stop_time = 0.0;

		time_bins = 0;
		mag_bins = 0;
		time_values = new double[0];
		mag_values = new double[0];

		total_acc = null;
		partial_acc_list = null;

		return;
	}




	// Default constructor.

	public OEAccumRateTimeMag () {
		clear();
	}




	// Set up to begin accumulating.
	// Parameters:
	//	the_statcked_poisson = Stacked Poisson distribution cache, or null for default.
	//  rate_acc_meth = The rate accumulation method to use (see OEConstants.make_rate_acc_meth()).
	//  the_time_values = The time values to define the bins, in days, must be in increasing order.
	//  the_mag_values = The magnitude values to define the bins, must be in increasing order.
	// Note: The function stores copies of the given arrays.

	public void setup (OEStackedPoisson the_stacked_poisson, int rate_acc_meth, double[] the_time_values, double[] the_mag_values) {

		// Parameter validation

		if (!( OEConstants.validate_rate_acc_meth (rate_acc_meth) )) {
			throw new IllegalArgumentException ("OEAccumRateTimeMag.setup: Invalid rate accumulation method: " + rate_acc_meth);
		}

		if (!( the_time_values != null && the_time_values.length >= 2 )) {
			throw new IllegalArgumentException ("OEAccumRateTimeMag.setup: Missing time values");
		}

		for (int i = 0; i+1 < the_time_values.length; ++i) {
			if (!( the_time_values[i] < the_time_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumRateTimeMag.setup: Out-of-order time values: index = " + i + ", value[i] = " + the_time_values[i] + ", value[i+1] = " + the_time_values[i+1]);
			}
		}

		if (!( the_mag_values != null && the_mag_values.length >= 2 )) {
			throw new IllegalArgumentException ("OEAccumRateTimeMag.setup: Missing magnitude values");
		}

		for (int i = 0; i+1 < the_mag_values.length; ++i) {
			if (!( the_mag_values[i] < the_mag_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumRateTimeMag.setup: Out-of-order magnitude values: index = " + i + ", value[i] = " + the_mag_values[i] + ", value[i+1] = " + the_mag_values[i+1]);
			}
		}

		// Copy parameters

		stacked_poisson = ((the_stacked_poisson != null) ? the_stacked_poisson : OEStackedPoisson.get_singleton());
		magfill_meth = OEConstants.extract_magfill_from_rate_acc (rate_acc_meth);
		outfill_meth = OEConstants.extract_outfill_from_rate_acc (rate_acc_meth);
		catlen_meth = OEConstants.extract_catlen_from_rate_acc (rate_acc_meth);

		time_bins = the_time_values.length - 1;
		mag_bins = the_mag_values.length - 1;
		time_values = Arrays.copyOf (the_time_values, the_time_values.length);
		mag_values = Arrays.copyOf (the_mag_values, the_mag_values.length);

		active_time_bins = time_bins;
		active_mag_bins = mag_bins;
		f_skip_last_gen_rate = false;
		min_cat_stop_time = time_values[0];

		// Empty accumulators

		total_acc = null;
		partial_acc_list = null;

		return;
	}




	//----- Consumers -----




	// Consumer base class implementation.
	//
	// The general design of a consumer is that it contains arrays of bins,
	// one to accumulate ruptures in the catalog, and one to accumulate
	// expected aftershock rates.  The rates are used to extrapolate to
	// times and magnitudes outside the range of the catalog.
	//
	// The time/magnitude grid is partitioned into several regions, based on
	// the catalog stop time, minimum magnitude, and maximum magnitude.
	// Note that the stop time applies to the entire catalog, but the
	// minimum and maximum magnitudes can vary from one generation to the
	// next, so the regions can vary with the generation.  The regions are:
	// - Count region: Before the stop time, between the minimum and
	//   maximum magnitudes.
	// - Downfill region: Before the stop time, below the minimum magnitude.
	// - Upfill region: Before the stop time, above the maximum magnitude.
	// - Outfill region: After the stop time.
	// - Infill region: Before the stop time.  This includes the count,
	//   downfill, and upfill regions.
	// Note that there is an option to extend the downfill region to include
	// the entire infill region, thereby eliminating the count and upfill
	// regions.
	//
	// As the catalog is scanned, the time/magnitude bins are treated as
	// discrete.  At the end of the catalog, the bins are cumulated so that
	// each bin includes earlier times and larger magnitudes.  Finally, the
	// counts and rates are converted into shifted Poisson distributions,
	// which are stacked together with other catalogs in global accumulators.
	//
	// The consumer also contains the number of time bins for which the
	// catalog is live, which is added to the global live counts.

	private class ConsumerBase implements OECatalogConsumer {

		//----- Control variables -----

		// True if consumer is open.

		protected boolean f_open;

		//----- Accumulators, for the entire catalog -----

		// The accumulated counts, for the entire catalog.
		// Dimension: csr_counts[time_bins][mag_bins]
		// Each bin contains the total number of ruptures in that bin.

		protected int[][] csr_counts;

		// The accumulated expected values, for the entire catalog.
		// Dimension: csr_counts[time_bins][mag_bins]
		// Each bin contains the total expected number of ruptures.
		// Depending on the fill option, this may exclude regions of
		// the time/mag grid that are covered by csr_counts.

		private double[][] csr_expected;

		// Rates for each time bin, to be used for outfill, for the entire catalog.
		// Dimension: cat_outfill_rates[time_bins]
		// Each element contains the sum of the rates:
		//  r = SUM( k * Integral(((t-t0+c)^(-p))) )
		// where k is the rupture productivity, t0 is the rupture time,
		// p and c are the Omori parameters, the sum runs over all ruptures
		// in the current generation, and each integral runs over the
		// portion of the time bin that comes after the rupture
		// and before the time horizon.
		// With this definition, the rate of ruptures in the time bin per
		// unit magnitude is:
		//  r * b * log(10) * (10^(-b*(m - mref)))
		// where b is the Gutenberg-Richter parameter and mref is the reference
		// magnitude.
		// Note: Rates are derived from all (non-sterile) ruptures in all generations
		// except the last (if skipping rate calculation in last generation), even
		// if they do not lie within a magnitude bin.

		private double[] cat_outfill_rates;

		//----- Accumulators, for the current generation -----

		// Count in each time bin, in the current generation.
		// Dimension: cur_gen_counts[time_bins]
		// Note: This count includes all (non-sterile) ruptures in the current
		// generation, even if they do not lie within a magnitude bin.

		protected int[] cur_gen_counts;

		// Rates for each time bin, for the current generation.
		// Dimension: cur_gen_rates[time_bins]
		// Each element contains the sum of the rates:
		//  r = SUM( k * Integral(((t-t0+c)^(-p))) )
		// where k is the rupture productivity, t0 is the rupture time,
		// p and c are the Omori parameters, the sum runs over all ruptures
		// in the current generation, and each integral runs over the
		// portion of the time bin that comes after the rupture
		// and before the time horizon.
		// With this definition, the rate of ruptures in the time bin per
		// unit magnitude is:
		//  r * b * log(10) * (10^(-b*(m - mref)))
		// where b is the Gutenberg-Richter parameter and mref is the reference
		// magnitude.
		// Note: Rates are derived from all (non-sterile) ruptures in the current
		// generation, even if they do not lie within a magnitude bin.

		private double[] cur_gen_rates;

		//----- Time ranges -----

		// The bin during which the catalog stops.
		// Can be -1 if it stops before the first bin, active_time_bins if it stops after the last active bin.

		protected int stop_time_bin;

		// The number of time bins for which events are being accumulated.
		// This determines which portion of csr_counts is being incremented as ruptures are processed.
		// It is 0 if the catalog is being skipped.

		protected int cat_time_bins;

		// The last time bin for infilling in the current catalog (not the last+1 bin).
		// This is set at the start of the catalog and remains constant over all generations.
		// As a special case, this is -2 if the catalog is being skipped.

		protected int infill_last_time_bin;

		// The maximum time for infilling in the current catalog.
		// This time must lie within infill_last_time_bin (but not at the start of it).
		// If not at the end of infill_last_time_bin, then the last bin is a partial bin.
		// This is set at the start of the catalog and remains constant over all generations.

		protected double infill_max_time;

		// Flag indicating if rates are needed for infilling, in the current generation.
		// This specifies if cur_gen_rates needs to be filled in.
		// If true, then it is required that infill_last_time_bin >= 0.
		// This is set at the start of each generation, because it depends on the next
		// generation's magnitude range, and should be false for the last generation
		// (if skipping rate calculation in last generation).

		protected boolean f_infill_rates;

		// The first time bin for outfilling in the current catalog.
		// It is active_time_bins if there is no outfilling.
		// This is set at the start of the catalog and remains constant over all generations.

		protected int outfill_first_time_bin;

		// The minimum time for outfilling in the current catalog.
		// This time must lie within outfill_first_time_bin (but not at the end of it).
		// If not at the start of infill_last_time_bin, then the first bin is a partial bin.
		// This is set at the start of the catalog and remains constant over all generations.

		protected double outfill_min_time;

		// Flag indicating if rates are needed for outfilling, in the current generation.
		// This specifies if cat_outfill_rates needs to be filled in.
		// If true, then it is required that outfill_first_time_bin < active_time_bins.
		// This is set at the start of each generation, because it should be false for
		// the last generation (if skipping rate calculation in last generation).

		protected boolean f_outfill_rates;

		//----- Magnitude ranges -----

		// The last magnitude bin for downfilling in the current generation (not the last+1 bin).
		// It satisifes:  -1 <= downfill_last_mag_bin < active_mag_bins
		// This is set at the start of each generation because it depends on the next
		// generation's magnitude range, and there is no downfilling in the last generation
		// (if skipping rate calculation in last generation).

		protected int downfill_last_mag_bin;

		// The maximum magnitude for downfilling in the current generation.
		// This magnitude must lie within downfill_last_mag_bin (but not at the start of it).
		// If not at the end of downfill_last_mag_bin, then the last bin is a partial bin.
		// This is set at the start of each generation, and is generally equal to the next
		// generation's minimum magnitude if there is downfilling.

		protected double downfill_max_mag;

		// True if downfilling in the current generation.
		// If true, then it is required that downfill_last_mag_bin >= 0.

		protected boolean f_downfill;

		// The last magnitude bin for downfilling in the current generation.
		// It satisfies:  0 <= upfill_first_mag_bin <= active_mag_bins
		// This is set at the start of each generation because it depends on the next
		// generation's magnitude range, and there is no upfilling in the last generation
		// (if skipping rate calculation in last generation).

		protected int upfill_first_mag_bin;

		// The minimum magnitude for upfilling in the current generation.
		// This magnitude must lie within upfill_first_mag_bin (but not at the end of it).
		// If not at the start of upfill_last_mag_bin, then the first bin is a partial bin.
		// This is set at the start of each generation, and is generally equal to the next
		// generation's maximum magnitude if there is upfilling.

		protected double upfill_min_mag;

		// True if upfilling in the current generation.
		// If true, then it is required that upfill_first_mag_bin < active_mag_bins.

		protected boolean f_upfill;

		// True if downfill is done using sterile ruptures.

		protected boolean f_downfill_sterile;

		//----- Reporting -----

		// Number of time bins to report to the accumulator.
		// This determines which portion of csr_counts and/or csr_expected are reported.

		protected int report_time_bins;

		// True to report counts.

		protected boolean f_report_counts;

		// True to report expected rates.

		protected boolean f_report_expected;




		//----- Time subroutines -----




		// Return true if the catalog is being skipped.

		protected final boolean is_cat_skipped () {
			return infill_last_time_bin == -2;
		}




		// Set up the time ranges at the start of a catalog.
		// This function sets up the time range variables.
		// It also initializes the per-catalog accumulators.

		protected void cat_setup_time_ranges (OECatalogScanComm comm) {

			// Zero the per-catalog counts and rates

			OEArraysCalc.zero_array (csr_counts);
			OEArraysCalc.zero_array (csr_expected);
			OEArraysCalc.zero_array (cat_outfill_rates);

			// The effective end time is the stop time, but not after the configured end time

			double eff_tend = Math.min (comm.cat_params.tend, comm.cat_stop_time);

			// Calculate the bin during which the catalog stops
			// Note: If the stop time is within epsilon of the end of a bin, then the stop time
			// is considered to lie in the next bin.
			// Can be -1 if it stops before the first bin, active_time_bins if it stops after the last active bin.

			stop_time_bin = OEArraysCalc.bsearch_array (time_values, eff_tend + comm.cat_params.teps, 0, active_time_bins + 1) - 1;

			// The number of time bins for which we count ruptures is by default
			// the number of bins before the bin that contains the stop time
			// (the number of bins for which the catalog reaches the end of the bin)

			cat_time_bins = Math.max (stop_time_bin, 0);


			//-- Catalog length determination

			// Flag indicates if we want to accept the catalog
			// If true, then the stop time must be at least epsilon after the start of the first time bin.

			boolean f_accept = false;

			// Flag indicates if we want to clip to full bin
			// If true, then stop_time_bin must be at least 1.
			// If false, then the stop time must be at least epsilon after the start of stop_time_bin.

			boolean f_clip = false;

			// Switch on catalog length method

			switch (catlen_meth) {

			default:
				throw new IllegalStateException ("OEAccumRateTimeMag.ConsumerBase.begin_catalog: Invalid catalog length method, catlen_meth = " + catlen_meth);

			// Any catalog length is acceptable

			case CATLEN_METH_ANY: {

				// Accept if it is at least epsilon into the first time bin

				if (stop_time_bin >= 0 && eff_tend - comm.cat_params.teps > time_values[0]) {
					f_accept = true;
				}

				// Clip if it is within epsilon of the start of the bin, other than the first bin

				if (stop_time_bin >= 1 && eff_tend - comm.cat_params.teps <= time_values[stop_time_bin]) {
					f_clip = true;
				}
			}
			break;

			// Any catalog length is acceptable, and clip to a full time bin

			case CATLEN_METH_ANY_CLIP: {

				// Accept and clip if it is at least into the second time bin

				if (stop_time_bin >= 1) {
					f_accept = true;
					f_clip = true;
				}
			}
			break;

			// Catalog must cover all active time bins

			case CATLEN_METH_RANGE: {

				// Accept and don't clip if it covers all active time bins

				if (stop_time_bin >= active_time_bins) {
					f_accept = true;
				}
			}
			break;

			// Catalog must have reached simulation end time

			case CATLEN_METH_ENTIRE: {

				// Accept if it is within epsilon of the simulation end time, and it is at least epsilon into the first time bin

				if (eff_tend + comm.cat_params.teps >= comm.cat_params.tend
					&& stop_time_bin >= 0 && eff_tend - comm.cat_params.teps > time_values[0] ) {
					f_accept = true;
				}

				// Clip if it is within epsilon of the start of the bin, other than the first bin

				if (stop_time_bin >= 1 && eff_tend - comm.cat_params.teps <= time_values[stop_time_bin]) {
					f_clip = true;
				}
			}
			break;

			// Catalog must have reached simulation end time, and clip to a full time bin

			case CATLEN_METH_ENTIRE_CLIP: {

				// Accept and clip if it is within epsilon of the simulation end time, and it is at least into the second time bin

				if (eff_tend + comm.cat_params.teps >= comm.cat_params.tend
					&& stop_time_bin >= 1) {
					f_accept = true;
					f_clip = true;
				}
			}
			break;

			}

			// Case where the catalog is not accepted

			if (!( f_accept )) {

				// No infill

				infill_last_time_bin = -2;		// special value to use when skipping the catalog
				infill_max_time = time_values[0];

				// No outfill

				outfill_first_time_bin = active_time_bins;
				outfill_min_time = time_values[outfill_first_time_bin];

				// Also no rates and no sterile ruptures

				f_infill_rates = false;
				f_outfill_rates = false;
				f_downfill_sterile = false;
			}

			// Case where catalog covers all active time bins

			else if (stop_time_bin >= active_time_bins) {

				// Infill to end of last active time bin, but not past the catalog stop time

				infill_last_time_bin = active_time_bins - 1;
				infill_max_time = Math.min (eff_tend, time_values[infill_last_time_bin + 1]);

				// No outfill

				outfill_first_time_bin = active_time_bins;
				outfill_min_time = time_values[outfill_first_time_bin];
			}

			// Case where we need to clip to a full time bin

			else if (f_clip) {

				// Infill to end of the time bin before the bin that contains the stop time, but not past the catalog stop time

				infill_last_time_bin = stop_time_bin - 1;
				infill_max_time = Math.min (eff_tend, time_values[infill_last_time_bin + 1]);

				// Outfill from the start of the time bin that contains the stop time

				outfill_first_time_bin = stop_time_bin;
				outfill_min_time = time_values[outfill_first_time_bin];
			}

			// Case where catalog ends in the middle of a time bin

			else {

				// Infill to end of the time bin that contains the stop time, but not past the catalog stop time

				infill_last_time_bin = stop_time_bin;
				infill_max_time = Math.min (eff_tend, time_values[infill_last_time_bin + 1]);

				// Outfill from the start of the time bin that contains the stop time, but not before the catalog stop time

				outfill_first_time_bin = stop_time_bin;
				outfill_min_time = Math.max (eff_tend, time_values[outfill_first_time_bin]);
			}

			return;
		}




		//----- Magnitude subroutines -----




		// Return true if we want to skip rate calculation in this generation.

		protected final boolean skip_gen_rate (OECatalogScanComm comm) {
			return comm.f_final_gen && f_skip_last_gen_rate;
		}




		// Get the minimum magnitude for the next generation.
		// If final generation, return the lower minimum magnitude from the parameters.

		protected final double get_next_gen_mag_min (OECatalogScanComm comm) {
			if (comm.f_final_gen) {
				return comm.cat_params.mag_min_lo;
			}
			return comm.next_gen_info.gen_mag_min; 
		}




		// Get the maximum magnitude for the next generation.
		// If final generation, return the maximum magnitude from the parameters.

		protected final double get_next_gen_mag_max (OECatalogScanComm comm) {
			if (comm.f_final_gen) {
				return comm.cat_params.mag_max_sim;
			}
			return comm.next_gen_info.gen_mag_max; 
		}




		// Set up the magnitude ranges at the start of a generation.
		// This function sets up the magnitude range variables, plus f_infill_rates and f_outfill_rates.
		// It also initializes the per-generation accumulators.

		protected void gen_setup_mag_ranges (OECatalogScanComm comm) {

			// Zero the per-generation counts and rates

			OEArraysCalc.zero_array (cur_gen_counts);
			OEArraysCalc.zero_array (cur_gen_rates);

			// Switch on magnitude fill method

			switch (magfill_meth) {

			default:
				throw new IllegalStateException ("OEAccumRateTimeMag.ConsumerBase.gen_setup_mag_ranges: Invalid magnitude fill method, magfill_meth = " + magfill_meth);

			// No magnitude fill

			case MAGFILL_METH_NONE: {

				// No downfill or upfill

				downfill_last_mag_bin = -1;
				downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
				f_downfill = false;

				upfill_first_mag_bin = active_mag_bins;
				upfill_min_mag = mag_values[upfill_first_mag_bin];
				f_upfill = false;

				f_downfill_sterile = false;

				f_infill_rates = false;
			}
			break;

			// Use probability distribution function with expected rate over entire magnitude range

			case MAGFILL_METH_PDF_ONLY: {

				// If skipping generation or no infill, then no downfill

				if (skip_gen_rate(comm) || infill_last_time_bin < 0) {
					downfill_last_mag_bin = -1;
					downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
					f_downfill = false;
				}

				// Otherwise, downfill the entire magnitude range

				else {
					downfill_last_mag_bin = active_mag_bins - 1;
					downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
					f_downfill = true;
				}

				// No upfill

				upfill_first_mag_bin = active_mag_bins;
				upfill_min_mag = mag_values[upfill_first_mag_bin];
				f_upfill = false;

				// No sterile ruptures

				f_downfill_sterile = false;

				// Need rates if we are downfilling

				f_infill_rates = f_downfill;
			}
			break;

			// Combine counts with a pdf derived from expected rate outside magnitude range

			case MAGFILL_METH_PDF_HYBRID: {

				// If skipping generation or no infill, then no downfill or upfill

				if (skip_gen_rate(comm) || infill_last_time_bin < 0) {
					downfill_last_mag_bin = -1;
					downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
					f_downfill = false;

					upfill_first_mag_bin = active_mag_bins;
					upfill_min_mag = mag_values[upfill_first_mag_bin];
					f_upfill = false;
				}

				// Otherwise ...

				else {

					// Magnitude bin that contains the next generation's minimum magnitude
					// -1 if before the first bin, active_mag_bins - 1 if in or after the last bin.
					// If magnitude is slightly after the start of a bin, it is considered to be in the prior bin.

					downfill_last_mag_bin = OEArraysCalc.bsearch_array (
						mag_values, get_next_gen_mag_min(comm) - comm.cat_params.mag_eps, 0, active_mag_bins) - 1;

					// No downfill if before the first bin, otherwise allow for possible partial bin

					if (downfill_last_mag_bin < 0) {
						downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
						f_downfill = false;
					} else {
						downfill_max_mag = Math.min (mag_values[downfill_last_mag_bin + 1], get_next_gen_mag_min(comm));
						f_downfill = true;
					}

					// Magnitude bin that contains the next generation's maximum magnitude
					// 0 if in or before the first bin, active_mag_bins if after the last bin.
					// If magnitude is slightly before the end of a bin, it is considered to be in the next bin.

					upfill_first_mag_bin = OEArraysCalc.bsearch_array (
						mag_values, get_next_gen_mag_max(comm) + comm.cat_params.mag_eps, 1, active_mag_bins + 1) - 1;

					// No upfill if after the last bin, otherwise allow for possible partial bin

					if (upfill_first_mag_bin >= active_mag_bins) {
						upfill_min_mag = mag_values[upfill_first_mag_bin];
						f_upfill = false;
					} else {
						upfill_min_mag = Math.max (mag_values[upfill_first_mag_bin], get_next_gen_mag_max(comm));
						f_upfill = true;
					}
				}

				// No sterile ruptures

				f_downfill_sterile = false;

				// Need rates if we are downfilling or upfilling

				f_infill_rates = (f_downfill || f_upfill);
			}
			break;

			// Combine counts, sterile ruptures, and a pdf derived from expected rate above mag range

			case MAGFILL_METH_PDF_STERILE: {

				// No downfill

				downfill_last_mag_bin = -1;
				downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
				f_downfill = false;

				// If skipping generation or no infill, then no upfill

				if (skip_gen_rate(comm) || infill_last_time_bin < 0) {
					upfill_first_mag_bin = active_mag_bins;
					upfill_min_mag = mag_values[upfill_first_mag_bin];
					f_upfill = false;
				}

				// Otherwise ...

				else {

					// Magnitude bin that contains the next generation's maximum magnitude
					// 0 if in or before the first bin, active_mag_bins if after the last bin.
					// If magnitude is slightly before the end of a bin, it is considered to be in the next bin.

					upfill_first_mag_bin = OEArraysCalc.bsearch_array (
						mag_values, get_next_gen_mag_max(comm) + comm.cat_params.mag_eps, 1, active_mag_bins + 1) - 1;

					// No upfill if after the last bin, otherwise allow for possible partial bin

					if (upfill_first_mag_bin >= active_mag_bins) {
						upfill_min_mag = mag_values[upfill_first_mag_bin];
						f_upfill = false;
					} else {
						upfill_min_mag = Math.max (mag_values[upfill_first_mag_bin], get_next_gen_mag_max(comm));
						f_upfill = true;
					}
				}

				// Request sterile ruptures if needed, down to our lowest magnitude

				comm.set_sterile_mag (mag_values[0]);

				// Accept sterile ruptures

				f_downfill_sterile = true;

				// Need rates if we are upfilling

				f_infill_rates = f_upfill;
			}
			break;

			}

			// Switch on outfill method

			switch (outfill_meth) {

			default:
				throw new IllegalStateException ("OEAccumRateTimeMag.ConsumerBase.gen_setup_mag_ranges: Invalid outfill method, outfill_meth = " + outfill_meth);

			// No outfill, use exactly as simulated, zero-filling after end of simulation

			case OUTFILL_METH_NONE: {

				// No rates

				f_outfill_rates = false;
			}
			break;

			// Do not participate in probability distributions after end of simulation

			case OUTFILL_METH_OMIT: {

				// No rates

				f_outfill_rates = false;
			}
			break;

			// Use pdf derived from expected rate of direct aftershocks

			case OUTFILL_METH_PDF_DIRECT: {

				// If skipping generation or no outfill, then no rates

				if (skip_gen_rate(comm) || outfill_first_time_bin >= active_time_bins) {
					f_outfill_rates = false;
				}

				// Otherwise, need rates

				else {
					f_outfill_rates = true;
				}
			}
			break;

			}

			return;
		}




		// Finish using the magnitude ranges at the end of a generation.

		protected void gen_finish_mag_ranges (OECatalogScanComm comm) {

			// If downfill ...

			if (f_downfill) {

				// Loop over full magnitude bins needing downfill ...

				for (int mag_ix = 0; mag_ix < downfill_last_mag_bin; ++mag_ix) {
			
					// Get the magnitude integral

					double mag_int = OERandomGenerator.gr_rate (
						comm.cat_params.b,				// b
						comm.cat_params.mref,			// mref
						mag_values[mag_ix],				// m1
						mag_values[mag_ix+1]			// m2
					);

					// Accumulate the rates within each infill time bin

					for (int time_ix = 0; time_ix <= infill_last_time_bin; ++time_ix) {
						csr_expected[time_ix][mag_ix] += (cur_gen_rates[time_ix] * mag_int);
					}
				}

				// Possible partial magnitude bin
			
				// Get the magnitude integral

				double mag_int2 = OERandomGenerator.gr_rate (
					comm.cat_params.b,					// b
					comm.cat_params.mref,				// mref
					mag_values[downfill_last_mag_bin],	// m1
					downfill_max_mag					// m2
				);

				// Accumulate the rates within each infill time bin

				for (int time_ix = 0; time_ix <= infill_last_time_bin; ++time_ix) {
					csr_expected[time_ix][downfill_last_mag_bin] += (cur_gen_rates[time_ix] * mag_int2);
				}
			}

			// If upfill ...

			if (f_upfill) {

				// Possible partial magnitude bin
			
				// Get the magnitude integral

				double mag_int2 = OERandomGenerator.gr_rate (
					comm.cat_params.b,						// b
					comm.cat_params.mref,					// mref
					upfill_min_mag,							// m1
					mag_values[upfill_first_mag_bin + 1]	// m2
				);

				// Accumulate the rates within each infill time bin

				for (int time_ix = 0; time_ix <= infill_last_time_bin; ++time_ix) {
					csr_expected[time_ix][upfill_first_mag_bin] += (cur_gen_rates[time_ix] * mag_int2);
				}

				// Loop over full magnitude bins needing upfill ...

				for (int mag_ix = upfill_first_mag_bin + 1; mag_ix < active_mag_bins; ++mag_ix) {
			
					// Get the magnitude integral

					double mag_int = OERandomGenerator.gr_rate (
						comm.cat_params.b,				// b
						comm.cat_params.mref,			// mref
						mag_values[mag_ix],				// m1
						mag_values[mag_ix+1]			// m2
					);

					// Accumulate the rates within each infill time bin

					for (int time_ix = 0; time_ix <= infill_last_time_bin; ++time_ix) {
						csr_expected[time_ix][mag_ix] += (cur_gen_rates[time_ix] * mag_int);
					}
				}
			}

			return;
		}




		// Finish performing outfill at the end of a catalog.
		// This also sets the reporting variables.

		public void cat_finish_outfill (OECatalogScanComm comm) {

			// Switch on magnitude fill method

			switch (magfill_meth) {

			default:
				throw new IllegalStateException ("OEAccumRateTimeMag.ConsumerBase.cat_finish_outfill: Invalid magnitude fill method, magfill_meth = " + magfill_meth);

			// No magnitude fill

			case MAGFILL_METH_NONE: {
			}
			break;

			// Use probability distribution function with expected rate over entire magnitude range

			case MAGFILL_METH_PDF_ONLY: {
		
				// Zero the counts, since we are using just the probability distribution
				// Note: If outfill requires counts, a copy of the counts should be saved.

				OEArraysCalc.zero_array (csr_counts);
			}
			break;

			// Combine counts with a pdf derived from expected rate outside magnitude range

			case MAGFILL_METH_PDF_HYBRID: {
			}
			break;

			// Combine counts, sterile ruptures, and a pdf derived from expected rate above mag range

			case MAGFILL_METH_PDF_STERILE: {
			}
			break;

			}

			// Switch on outfill method

			switch (outfill_meth) {

			default:
				throw new IllegalStateException ("OEAccumRateTimeMag.ConsumerBase.cat_finish_outfill: Invalid outfill method, outfill_meth = " + outfill_meth);

			// No outfill, use exactly as simulated, zero-filling after end of simulation

			case OUTFILL_METH_NONE: {

				// Report all time bins

				report_time_bins = active_time_bins;
			}
			break;

			// Do not participate in probability distributions after end of simulation

			case OUTFILL_METH_OMIT: {

				// Report only infilled time bins

				report_time_bins = infill_last_time_bin + 1;
			}
			break;

			// Use pdf derived from expected rate of direct aftershocks

			case OUTFILL_METH_PDF_DIRECT: {

				// Report all time bins

				report_time_bins = active_time_bins;

				// If outfill ...

				if (outfill_first_time_bin < active_time_bins) {

					// Loop over all magnitude bins ...

					for (int mag_ix = 0; mag_ix < active_mag_bins; ++mag_ix) {
			
						// Get the magnitude integral

						double mag_int = OERandomGenerator.gr_rate (
							comm.cat_params.b,				// b
							comm.cat_params.mref,			// mref
							mag_values[mag_ix],				// m1
							mag_values[mag_ix+1]			// m2
						);

						// Accumulate the rates within each outfill time bin

						for (int time_ix = outfill_first_time_bin; time_ix < active_time_bins; ++time_ix) {
							csr_expected[time_ix][mag_ix] += (cat_outfill_rates[time_ix] * mag_int);
						}
					}
				}
			}
			break;

			}

			return;
		}




		//----- Construction -----




		// Clear to the inactive state.

		protected final void clear_base() {

			//-- Time ranges

			stop_time_bin = -1;
			cat_time_bins = 0;

			// No infill

			infill_last_time_bin = -2;		// special value to use when skipping the catalog
			infill_max_time = time_values[0];

			// No outfill

			outfill_first_time_bin = active_time_bins;
			outfill_min_time = time_values[outfill_first_time_bin];

			// Also no rates

			f_infill_rates = false;
			f_outfill_rates = false;

			//--- Magnitude ranges

			// No downfill

			downfill_last_mag_bin = -1;
			downfill_max_mag = mag_values[downfill_last_mag_bin + 1];
			f_downfill = false;

			// No upfill

			upfill_first_mag_bin = active_mag_bins;
			upfill_min_mag = mag_values[upfill_first_mag_bin];
			f_upfill = false;

			// No sterile ruptures

			f_downfill_sterile = false;

			//--- Reporting

			report_time_bins = 0;

			return;
		}




		// Default constructor.

		public ConsumerBase () {
			f_open = false;

			csr_counts = new int[time_bins][mag_bins];
			csr_expected = new double[time_bins][mag_bins];
			cat_outfill_rates = new double[time_bins];

			cur_gen_counts = new int[time_bins];
			cur_gen_rates = new double[time_bins];

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

			// Set up time ranges and initialize per-catalog accumulators

			cat_setup_time_ranges (comm);

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

				// Finish performing outfill at the end of a catalog, and set reporting variables

				cat_finish_outfill (comm);
		
				// Cumulate the counts and rates: time upward, magnitude downward; for the reporting time bins

				OEArraysCalc.cumulate_2d_array (csr_counts, true, false, 0, report_time_bins);
				OEArraysCalc.cumulate_2d_array (csr_expected, true, false, 0, report_time_bins);

				// Get an accumulator

				try (
					EnsembleAccum ens_accum = get_partial_acc();
				) {

					// Accumulate counted time bins and number of catalogs

					ens_accum.add_counted_bin_count (infill_last_time_bin + 1);

					// Accumulate shifted Poisson distributions

					ens_accum.add_shifted_poisson (0, report_time_bins, 0, active_mag_bins, csr_expected, csr_counts);
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

			// Set up magnitude ranges

			gen_setup_mag_ranges (comm);
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

			// Finish using magnitude ranges

			gen_finish_mag_ranges (comm);
			return;
		}




		// Next rupture in the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_seed_rup (OECatalogScanComm comm) {
		
			// Find the time bin for this rupture,
			// -1 if before the first bin, infill_last_time_bin + 1 if after the last bin
			// Note that if infill_last_time_bin == -2 then time_ix == -1.

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 0, infill_last_time_bin + 2) - 1;

			// Drop rupture if past the last infill time bin
			// Note: If catalogs can be clipped at a time other than the end of a bin, a test should be inserted here.

			if (time_ix > infill_last_time_bin) {
				return;
			}

			// If infill rate calculation is desired, add the rate for each time bin after the rupture

			if (f_infill_rates) {

				// Full bins

				for (int tix = Math.max (time_ix, 0) ; tix < infill_last_time_bin; ++tix) {
					cur_gen_rates[tix] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
						comm.cat_params.p,		// p
						comm.cat_params.c,		// c
						comm.rup.t_day,			// t0
						comm.cat_params.teps,	// teps
						time_values[tix],		// t1
						time_values[tix + 1]	// t2
					));
				}

				// Possible partial bin
				// (Note that omori_rate_shifted() properly handles the case comm.rup.t_day >= infill_max_time)

				cur_gen_rates[infill_last_time_bin] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
					comm.cat_params.p,		// p
					comm.cat_params.c,		// c
					comm.rup.t_day,			// t0
					comm.cat_params.teps,	// teps
					time_values[infill_last_time_bin],	// t1
					infill_max_time			// t2
				));
			}

			// If outfill rate calculation is desired, add the rate for each time bin (which are all after the rupture)

			if (f_outfill_rates) {

				// Possible partial bin
				// (Note that omori_rate_shifted() properly handles the case comm.rup.t_day >= end-of-bin)

				cat_outfill_rates[outfill_first_time_bin] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
					comm.cat_params.p,		// p
					comm.cat_params.c,		// c
					comm.rup.t_day,			// t0
					comm.cat_params.teps,	// teps
					outfill_min_time,		// t1
					time_values[outfill_first_time_bin + 1]	// t2
				));

				// Full bins

				for (int tix = outfill_first_time_bin + 1 ; tix < active_time_bins; ++tix) {
					cat_outfill_rates[tix] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
						comm.cat_params.p,		// p
						comm.cat_params.c,		// c
						comm.rup.t_day,			// t0
						comm.cat_params.teps,	// teps
						time_values[tix],		// t1
						time_values[tix + 1]	// t2
					));
				}
			}

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

			// Set up magnitude ranges

			gen_setup_mag_ranges (comm);
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

			// Finish using magnitude ranges

			gen_finish_mag_ranges (comm);
			return;
		}




		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {
		
			// Find the time bin for this rupture,
			// -1 if before the first bin, infill_last_time_bin + 1 if after the last bin
			// Note that if infill_last_time_bin == -2 then time_ix == -1.

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 0, infill_last_time_bin + 2) - 1;

			// Drop rupture if past the last infill time bin
			// Note: If catalogs can be clipped at a time other than the end of a bin, a test should be inserted here.

			if (time_ix > infill_last_time_bin) {
				return;
			}

			// If infill rate calculation is desired, add the rate for each time bin after the rupture

			if (f_infill_rates) {

				// Full bins

				for (int tix = Math.max (time_ix, 0) ; tix < infill_last_time_bin; ++tix) {
					cur_gen_rates[tix] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
						comm.cat_params.p,		// p
						comm.cat_params.c,		// c
						comm.rup.t_day,			// t0
						comm.cat_params.teps,	// teps
						time_values[tix],		// t1
						time_values[tix + 1]	// t2
					));
				}

				// Possible partial bin
				// (Note that omori_rate_shifted() properly handles the case comm.rup.t_day >= infill_max_time)

				cur_gen_rates[infill_last_time_bin] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
					comm.cat_params.p,		// p
					comm.cat_params.c,		// c
					comm.rup.t_day,			// t0
					comm.cat_params.teps,	// teps
					time_values[infill_last_time_bin],	// t1
					infill_max_time			// t2
				));
			}

			// If outfill rate calculation is desired, add the rate for each time bin (which are all after the rupture)

			if (f_outfill_rates) {

				// Possible partial bin
				// (Note that omori_rate_shifted() properly handles the case comm.rup.t_day >= end-of-bin)

				cat_outfill_rates[outfill_first_time_bin] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
					comm.cat_params.p,		// p
					comm.cat_params.c,		// c
					comm.rup.t_day,			// t0
					comm.cat_params.teps,	// teps
					outfill_min_time,		// t1
					time_values[outfill_first_time_bin + 1]	// t2
				));

				// Full bins

				for (int tix = outfill_first_time_bin + 1 ; tix < active_time_bins; ++tix) {
					cat_outfill_rates[tix] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
						comm.cat_params.p,		// p
						comm.cat_params.c,		// c
						comm.rup.t_day,			// t0
						comm.cat_params.teps,	// teps
						time_values[tix],		// t1
						time_values[tix + 1]	// t2
					));
				}
			}

			// Drop rupture if not within active time range

			if (time_ix < 0) {
				return;
			}

			// Count the rupture within the current generation

			cur_gen_counts[time_ix]++;
		
			// Find the magnitude bin for this rupture,
			// -1 if before the first bin, active_mag_bins if after the last active bin

			int mag_ix = OEArraysCalc.bsearch_array (mag_values, comm.rup.rup_mag, 0, active_mag_bins + 1) - 1;

			// Drop rupture if not within active magnitude range

			if (mag_ix < 0 || mag_ix >= active_mag_bins) {
				return;
			}

			// Count the rupture

			csr_counts[time_ix][mag_ix]++;

			return;
		}




		// Next sterile rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_sterile_rup (OECatalogScanComm comm) {

			// Drop rupture if downfill is not sterile ruptures

			if (!( f_downfill_sterile )) {
				return;
			}
		
			// Find the time bin for this rupture,
			// -1 if before the first bin, infill_last_time_bin + 1 if after the last bin
			// Note that if infill_last_time_bin == -2 then time_ix == -1.

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 0, infill_last_time_bin + 2) - 1;

			// Drop rupture if not within active time range

			if (time_ix < 0 || time_ix > infill_last_time_bin) {
				return;
			}
		
			// Find the magnitude bin for this rupture,
			// -1 if before the first bin, active_mag_bins if after the last active bin

			int mag_ix = OEArraysCalc.bsearch_array (mag_values, comm.rup.rup_mag, 0, active_mag_bins + 1) - 1;

			// Drop rupture if not within active magnitude range

			if (mag_ix < 0 || mag_ix >= active_mag_bins) {
				return;
			}

			// Count the rupture

			csr_counts[time_ix][mag_ix]++;

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




	// Get the number of time bins.

	public final int get_time_bins () {
		return time_bins;
	}


	// Get the number of magnitude bins.

	public final int get_mag_bins () {
		return mag_bins;
	}


	// Get a time value.

	public final double get_time_value (int n) {
		return time_values[n];
	}


	// Get a magnitude value.

	public final double get_mag_value (int n) {
		return mag_values[n];
	}


	// Get the size, which is the number of catalogs accumulated.

	public final int get_size () {
		return total_acc.acc_size;
	}


	// Get the counts of the number of catalog alive in each time bin.

	public final int[] get_live_counts () {
		return total_acc.acc_live_counts;
	}




	// Get the bin sizes.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is the number of catalogs contributing to the bin.

	public final int[][] get_bin_size_array () {
		return total_acc.get_bin_size();
	}




	// Get a fractile.
	// Parameters:
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is an integer N, such that the probability of N or fewer
	// ruptures is approximately equal to fractile.

	public final int[][] get_fractile_array (double fractile) {
		return total_acc.get_fractile (fractile);
	}




	// Get the probability of occurrence array.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is a real number v, such that v is the probability that
	// at least one rupture occurs.

	public final double[][] get_prob_occur_array () {
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
	// Note: The parameter can be any array of dimension int[time_bins][mag_bins].

	public final String fractile_array_to_string (int[][] fractile_array) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (" M \\ T ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10.3f", time_values[time_ix + 1] - time_values[0]));
		}
		result.append ("\n");

		// Data lines

		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			result.append (String.format ("%7.3f", mag_values[mag_ix]));
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
				result.append (String.format ("  %10d", fractile_array[time_ix][mag_ix]));
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
	// Note: The parameter can be any array of dimension double[time_bins][mag_bins]
	// where fixed-point display to 3 decimal places is appropriate.

	public final String prob_occur_array_to_string (double[][] prob_occur_array) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (" M \\ T ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10.3f", time_values[time_ix + 1] - time_values[0]));
		}
		result.append ("\n");

		// Data lines

		for (int mag_ix = 0; mag_ix < mag_bins; ++mag_ix) {
			result.append (String.format ("%7.3f", mag_values[mag_ix]));
			for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
				result.append (String.format ("  %10.3f", prob_occur_array[time_ix][mag_ix] * 100.0));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	// Convert a live count array to a string.
	// Parameters:
	//  live_count_array = Fractile array as returned by get_fractile_array().
	// Table layout is:
	//  7 cols blank
	//  2 cols spacer
	//  10 cols for time/value
	//  2 cols spacer ...
	// Note: The parameter can be any array of dimension int[time_bins].

	public final String live_count_array_to_string (int[] live_count_array) {
		StringBuilder result = new StringBuilder();

		// Header line

		result.append (" - \\ T ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10.3f", time_values[time_ix + 1] - time_values[0]));
		}
		result.append ("\n");

		// Data lines

		result.append ("       ");
		for (int time_ix = 0; time_ix < time_bins; ++time_ix) {
			result.append (String.format ("  %10d", live_count_array[time_ix]));
		}
		result.append ("\n");

		return result.toString();
	}




	//----- Testing -----




	// Set up using typical values for time and magnitude.
	// Parameters:
	//  rate_acc_meth = The rate accumulation method to use (see OEConstants.make_rate_acc_meth()).
	//  tbegin = Begin time for forecast, in days.

	public void typical_test_setup (int rate_acc_meth, double tbegin) {
	
		// Make time array for day, week, month, year

		double[] the_time_values = new double[5];
		the_time_values[0] = tbegin;
		the_time_values[1] = tbegin + 1.0;
		the_time_values[2] = tbegin + 7.0;
		the_time_values[3] = tbegin + 30.0;
		the_time_values[4] = tbegin + 365.0;

		// Make magnitude array for 3, 4, 5, 6, 7

		double[] the_mag_values = new double[6];
		the_mag_values[0] = 3.0;
		the_mag_values[1] = 4.0;
		the_mag_values[2] = 5.0;
		the_mag_values[3] = 6.0;
		the_mag_values[4] = 7.0;
		the_mag_values[5] = 10.0;

		// Do the setup

		setup (null, rate_acc_meth, the_time_values, the_mag_values);
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

		// Live counts

		result.append ("\n");
		result.append ("Live counts\n");
		result.append ("\n");

		result.append (live_count_array_to_string (get_live_counts()));

		// Bin sizes

		result.append ("\n");
		result.append ("Bin sizes\n");
		result.append ("\n");

		result.append (fractile_array_to_string (get_bin_size_array()));

		// 0% fractile (minimum over all catalogs)

		result.append ("\n");
		result.append ("0% fractile (minimum)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.0);
		result.append (fractile_array_to_string (fractile_array));

		// 2.5% fractile (forecast lower limit)

		result.append ("\n");
		result.append ("2.5% fractile (forecast low)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.025);
		result.append (fractile_array_to_string (fractile_array));

		// 50% fractile (median)

		result.append ("\n");
		result.append ("50% fractile (median)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.50);
		result.append (fractile_array_to_string (fractile_array));

		// 97.5% fractile (forecast upper limit)

		result.append ("\n");
		result.append ("97.5% fractile (forecast high)\n");
		result.append ("\n");

		fractile_array = get_fractile_array (0.975);
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
	//  rate_acc_meth = The rate accumulation method to use (see OEConstants.make_rate_acc_meth()).
	//  num_cats = Number of catalogs to run.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run (OECatalogParams test_cat_params, double mag_main, int rate_acc_meth, int num_cats) {

		// Say hello

		System.out.println ();
		System.out.println ("Generating " + num_cats + " catalogs");
		System.out.println ();

		// Make the accumulator and set up the bins

		OEAccumRateTimeMag time_mag_accum = new OEAccumRateTimeMag();
		time_mag_accum.typical_test_setup (rate_acc_meth, test_cat_params.tbegin);

		// Create the array of accumulators

		OEEnsembleAccumulator[] accumulators = new OEEnsembleAccumulator[1];
		accumulators[0] = time_mag_accum;

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

		System.out.println (time_mag_accum.typical_test_outputs_to_string());

		return;
	}




	// Perform a typical test run, multi-threaded version.
	// Parameters:
	//  test_cat_params = Catalog parameters.
	//  mag_main = Mainshock magnitude.
	//  rate_acc_meth = The rate accumulation method to use (see OEConstants.make_rate_acc_meth()).
	//  num_cats = Number of catalogs to run.
	//  num_threads = Number of threads to use, can be -1 for default number of threads.
	//  max_runtime = Maximum running time allowed.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run_mt (OECatalogParams test_cat_params, double mag_main, int rate_acc_meth, int num_cats, int num_threads, long max_runtime) {

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

		OEAccumRateTimeMag time_mag_accum = new OEAccumRateTimeMag();
		time_mag_accum.typical_test_setup (rate_acc_meth, test_cat_params.tbegin);

		// Create the list of accumulators

		ArrayList<OEEnsembleAccumulator> accumulators = new ArrayList<OEEnsembleAccumulator>();
		accumulators.add (time_mag_accum);

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

		System.out.println (time_mag_accum.typical_test_outputs_to_string());

		return;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEAccumRateTimeMag : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test1' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Do the test run

				typical_test_run (test_cat_params, mag_main, the_rate_acc_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Then display the accumulated fractiles and probability of occurrence.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test2' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Prevent minimum magnitude adjustment

				test_cat_params.mag_min_lo = test_cat_params.mag_min_sim;
				test_cat_params.mag_min_hi = test_cat_params.mag_min_sim;

				// Do the test run

				typical_test_run (test_cat_params, mag_main, the_rate_acc_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Same as test #1 and #2 except with control over the magnitude ranges.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 15 additional arguments

			if (args.length != 16) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test3' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);
				double the_mag_min_sim = Double.parseDouble (args[12]);
				double the_mag_max_sim = Double.parseDouble (args[13]);
				double the_mag_min_lo = Double.parseDouble (args[14]);
				double the_mag_min_hi = Double.parseDouble (args[15]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Do the test run

				typical_test_run (test_cat_params, mag_main, the_rate_acc_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  num_threads  max_runtime
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #3 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 17 additional arguments

			if (args.length != 18) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test4' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);
				double the_mag_min_sim = Double.parseDouble (args[12]);
				double the_mag_max_sim = Double.parseDouble (args[13]);
				double the_mag_min_lo = Double.parseDouble (args[14]);
				double the_mag_min_hi = Double.parseDouble (args[15]);
				int num_threads = Integer.parseInt (args[16]);
				long max_runtime = Long.parseLong (args[17]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("num_threads = " + num_threads);
				System.out.println ("max_runtime = " + max_runtime);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Do the test run

				typical_test_run_mt (test_cat_params, mag_main, the_rate_acc_meth, num_cats, num_threads, max_runtime);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mag_excess
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Same as test #1 thru #4 except with control over the magnitude ranges and excess.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 16 additional arguments

			if (args.length != 17) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test5' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);
				double the_mag_min_sim = Double.parseDouble (args[12]);
				double the_mag_max_sim = Double.parseDouble (args[13]);
				double the_mag_min_lo = Double.parseDouble (args[14]);
				double the_mag_min_hi = Double.parseDouble (args[15]);
				double the_mag_excess = Double.parseDouble (args[16]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("the_mag_excess = " + the_mag_excess);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges and excess

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				test_cat_params.mag_excess = the_mag_excess;

				// Do the test run

				typical_test_run (test_cat_params, mag_main, the_rate_acc_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mag_excess  num_threads  max_runtime
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #5 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 18 additional arguments

			if (args.length != 19) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test6' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);
				double the_mag_min_sim = Double.parseDouble (args[12]);
				double the_mag_max_sim = Double.parseDouble (args[13]);
				double the_mag_min_lo = Double.parseDouble (args[14]);
				double the_mag_min_hi = Double.parseDouble (args[15]);
				double the_mag_excess = Double.parseDouble (args[16]);
				int num_threads = Integer.parseInt (args[17]);
				long max_runtime = Long.parseLong (args[18]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("the_mag_excess = " + the_mag_excess);
				System.out.println ("num_threads = " + num_threads);
				System.out.println ("max_runtime = " + max_runtime);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges and excess

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				test_cat_params.mag_excess = the_mag_excess;

				// Do the test run

				typical_test_run_mt (test_cat_params, mag_main, the_rate_acc_meth, num_cats, num_threads, max_runtime);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mag_excess  duration
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Same as test #1 thru #6 except with control over the magnitude ranges and excess, and catalog duration.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 17 additional arguments

			if (args.length != 18) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test7' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);
				double the_mag_min_sim = Double.parseDouble (args[12]);
				double the_mag_max_sim = Double.parseDouble (args[13]);
				double the_mag_min_lo = Double.parseDouble (args[14]);
				double the_mag_min_hi = Double.parseDouble (args[15]);
				double the_mag_excess = Double.parseDouble (args[16]);
				double duration = Double.parseDouble (args[17]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("the_mag_excess = " + the_mag_excess);
				System.out.println ("duration = " + duration);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + duration;

				// Set magnitude tanges and excess

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				test_cat_params.mag_excess = the_mag_excess;

				// Do the test run

				typical_test_run (test_cat_params, mag_main, the_rate_acc_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  rate_acc_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mag_excess  num_threads  max_runtime
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #7 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test8")) {

			// 19 additional arguments

			if (args.length != 20) {
				System.err.println ("OEAccumRateTimeMag : Invalid 'test8' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				int the_rate_acc_meth = Integer.parseInt (args[10]);
				int num_cats = Integer.parseInt (args[11]);
				double the_mag_min_sim = Double.parseDouble (args[12]);
				double the_mag_max_sim = Double.parseDouble (args[13]);
				double the_mag_min_lo = Double.parseDouble (args[14]);
				double the_mag_min_hi = Double.parseDouble (args[15]);
				double the_mag_excess = Double.parseDouble (args[16]);
				double duration = Double.parseDouble (args[17]);
				int num_threads = Integer.parseInt (args[18]);
				long max_runtime = Long.parseLong (args[19]);

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_rate_acc_meth = " + the_rate_acc_meth);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("the_mag_excess = " + the_mag_excess);
				System.out.println ("duration = " + duration);
				System.out.println ("num_threads = " + num_threads);
				System.out.println ("max_runtime = " + max_runtime);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + duration;

				// Set magnitude tanges and excess

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				test_cat_params.mag_excess = the_mag_excess;

				// Do the test run

				typical_test_run_mt (test_cat_params, mag_main, the_rate_acc_meth, num_cats, num_threads, max_runtime);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEAccumRateTimeMag : Unrecognized subcommand : " + args[0]);
		return;

	}

}
