package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.AutoExecutorService;

import org.opensha.oaf.oetas.util.OEArraysCalc;

import static org.opensha.oaf.oetas.OEConstants.INFILL_METH_MIN;
import static org.opensha.oaf.oetas.OEConstants.INFILL_METH_NONE;
import static org.opensha.oaf.oetas.OEConstants.INFILL_METH_SCALE;
import static org.opensha.oaf.oetas.OEConstants.INFILL_METH_POISSON;
import static org.opensha.oaf.oetas.OEConstants.INFILL_METH_STERILE;
import static org.opensha.oaf.oetas.OEConstants.INFILL_METH_MAX;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;


// Operational ETAS catalog accumulator for a cumulative time/magnitude grid.
// Author: Michael Barall 02/01/2022.
//
// This object contains a 2D array of bins, each representing a range of
// time and magnitude.  Bins are cumulative, and include all ruptures at
// earlier times and greater magnitudes.
//
// The contents of each bin is an array of rupture counts, one per catalog.
// During accumulation, the counts for the n-th catalog are stored in the
// n-th element of each column.  After accumulation, each bin is sorted,
// so that fractiles may be extracted.
//
// This version accommodates catalogs with variable length, and that yield
// a variable number of catalogs per bin.

public class OEAccumVarTimeMag implements OEEnsembleAccumulator, OEAccumReadoutTimeMag {

	//----- Control variables -----

	// The infill method used (see OEConstants.INFILL_METH_XXXXX).

	private int infill_meth;

	// The value to insert into a bin to indicate it is omitted.
	// It is large so it moves to the end when the list of counts is sorted.

	private static final int OMIT_BIN = Integer.MAX_VALUE;

	// The number of time bins that are used for reporting ruptures.

	private int active_time_bins;

	// The number of magnitude bins that are used for reporting ruptures.

	private int active_mag_bins;

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

	// The current capacity of the accumulator.

	private int acc_capacity;

	// The size of the accumulator, that is, the number of catalogs accumulated.

	private int acc_size;

	// The accumulated counts.
	// Dimension: acc_counts[time_bins][mag_bins][acc_capacity]

	private int[][][] acc_counts;

	// The number of time bins that are being counted in each catalog.
	// Dimension: acc_counted_bins[acc_capacity]

	private int[] acc_counted_bins;

	// An array containing zero for each bin, used as lower limit of column index.
	// Dimension: acc_bin_zero[time_bins][mag_bins]

	private int[][] acc_bin_zero;

	// The number of counts in each bin, also used as upper limit of column index.
	// Dimension: acc_bin_size[time_bins][mag_bins]

	private int[][] acc_bin_size;

	// The number of catalogs that are live within each time bin.
	// Dimension: acc_live_counts[time_bins]

	private int[] acc_live_counts;


	// The next catalog index to use.

	private AtomicInteger acc_catix = new AtomicInteger();

	// Get the next catalog index to use.
	// Throw an exception if all capacity is exhausted.

	private int get_acc_catix () {
		int n;
		do {
			n = acc_catix.get();
			if (n >= acc_capacity) {
				throw new IllegalStateException ("OEAccumVarTimeMag.get_acc_catix: No room in accumulator");
			}
		} while (!( acc_catix.compareAndSet (n, n+1) ));
		return n;
	}




	//----- Construction -----




	// Erase the contents.

	public final void clear () {
		infill_meth = 0;
		active_time_bins = 0;
		active_mag_bins = 0;
		min_cat_stop_time = 0.0;

		time_bins = 0;
		mag_bins = 0;
		time_values = new double[0];
		mag_values = new double[0];

		acc_capacity = 0;
		acc_size = 0;
		acc_catix.set(0);

		acc_counts = new int[0][0][0];
		acc_counted_bins = new int[0];
		acc_bin_zero = new int[0][0];
		acc_bin_size = new int[0][0];
		acc_live_counts = new int[0];

		return;
	}




	// Default constructor.

	public OEAccumVarTimeMag () {
		clear();
	}




	// Set up to begin accumulating.
	// Parameters:
	//  the_infill_meth = The infill method to use.
	//  the_time_values = The time values to define the bins, in days, must be in increasing order.
	//  the_mag_values = The magnitude values to define the bins, must be in increasing order.
	// Note: The function stores copies of the given arrays.

	public void setup (int the_infill_meth, double[] the_time_values, double[] the_mag_values) {

		// Parameter validation

		if (!( the_infill_meth >= INFILL_METH_MIN && the_infill_meth <= INFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEAccumVarTimeMag.setup: Invalid infill method: " + the_infill_meth);
		}

		if (!( the_time_values != null && the_time_values.length >= 2 )) {
			throw new IllegalArgumentException ("OEAccumVarTimeMag.setup: Missing time values");
		}

		for (int i = 0; i+1 < the_time_values.length; ++i) {
			if (!( the_time_values[i] < the_time_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumVarTimeMag.setup: Out-of-order time values: index = " + i + ", value[i] = " + the_time_values[i] + ", value[i+1] = " + the_time_values[i+1]);
			}
		}

		if (!( the_mag_values != null && the_mag_values.length >= 2 )) {
			throw new IllegalArgumentException ("OEAccumVarTimeMag.setup: Missing magnitude values");
		}

		for (int i = 0; i+1 < the_mag_values.length; ++i) {
			if (!( the_mag_values[i] < the_mag_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumVarTimeMag.setup: Out-of-order magnitude values: index = " + i + ", value[i] = " + the_mag_values[i] + ", value[i+1] = " + the_mag_values[i+1]);
			}
		}

		// Copy parameters

		infill_meth = the_infill_meth;

		time_bins = the_time_values.length - 1;
		mag_bins = the_mag_values.length - 1;
		time_values = Arrays.copyOf (the_time_values, the_time_values.length);
		mag_values = Arrays.copyOf (the_mag_values, the_mag_values.length);

		active_time_bins = time_bins;
		active_mag_bins = mag_bins;
		min_cat_stop_time = time_values[0];

		// Empty accumulators

		acc_capacity = 0;
		acc_size = 0;
		acc_catix.set(0);

		acc_counts = null;
		acc_counted_bins = null;
		acc_bin_zero = new int[time_bins][mag_bins];
		OEArraysCalc.zero_array (acc_bin_zero);
		acc_bin_size = null;
		acc_live_counts = null;

		return;
	}




	//----- Consumers -----




	// Consumer common base class.
	// The general design of a consumer is that it contains an array of bins,
	// which are used to accumulate ruptures in the catalog, and possibly
	// extrapolated ruptures to fill in ranges outside the catalog.
	// When the consumer is closed, the bin counts are transferred into one
	// column of the global array, which requires synchronization only to
	// get the column index.
	// The consumer also contains the number of time bins for which the
	// catalog is live, which is added to the global live counts.

	private abstract class ConsumerBase implements OECatalogConsumer {

		//----- Accumulators -----

		// True if consumer is open.

		protected boolean f_open;

		// The accumulated counts.
		// Dimension: csr_counts[time_bins][mag_bins]

		protected int[][] csr_counts;

		// The bin during which the catalog stops.
		// Can be -1 if it stops before the first bin, active_time_bins if it stops after the last active bin.

		protected int stop_time_bin;

		// The number of time bins for which events are being accumulated.
		// This determines which portion of csr_counts is being incremented as ruptures are processed.

		protected int cat_time_bins;

		// The number of time bins being reported to the accumulator.
		// This determines which portion of csr_counts is set to OMIT_BIN.

		protected int report_time_bins;


		//----- Construction -----

		// Default constructor.

		protected ConsumerBase () {
			f_open = false;
			csr_counts = new int[time_bins][mag_bins];
			stop_time_bin = 0;
			cat_time_bins = 0;
			report_time_bins = 0;
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
		
			// Zero the counts

			OEArraysCalc.zero_array (csr_counts);

			// The effective end time is the stop time, but not after the configured end time

			double eff_tend = Math.min (comm.cat_params.tend, comm.cat_stop_time);

			// Calculate the bin during which the catalog stops
			// (accepting a bin as complete if we get within epsilon of the bin's end time)

			stop_time_bin = OEArraysCalc.bsearch_array (time_values, eff_tend + comm.cat_params.teps, 0, active_time_bins + 1) - 1;

			// The number of time bins for which we count ruptures is by default
			// the number of bins before the bin that contains the stop time
			// (the number of bins for which the catalog reaches the end of the bin)

			cat_time_bins = Math.max (stop_time_bin, 0);

			// Don't count any ruptures if the catalog does not achieve minimum required duration
			// (The use of 0.5*teps ensures that if min_cat_stop_time is one of the time_values,
			// then any catalog considered sufficiently long will have stop_time_bin to be at
			// least the bin with min_cat_stop_time as its right endpoint.)

			if (eff_tend + (0.5 * comm.cat_params.teps) < min_cat_stop_time) {
				cat_time_bins = 0;
			}

			// The number of time bins we report is by default the same.

			report_time_bins = cat_time_bins;

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
		
				// Cumulate the counts: time upward, magnitude downward; for the reporting time bins

				OEArraysCalc.cumulate_2d_array (csr_counts, true, false, 0, report_time_bins);

				// Fill omit value into time bins that we are not reporting

				OEArraysCalc.fill_array (csr_counts, report_time_bins, time_bins, OMIT_BIN);

				// Get the index for this catalog

				int catix = get_acc_catix();

				// Store our counts into the accumulator

				OEArraysCalc.set_each_array_column (acc_counts, catix, csr_counts);

				acc_counted_bins[catix] = cat_time_bins;
			}

			return;
		}

		// Begin consuming the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_seed_generation (OECatalogScanComm comm) {
			return;
		}

		// End consuming the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_seed_generation (OECatalogScanComm comm) {
			return;
		}

		// Next rupture in the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_seed_rup (OECatalogScanComm comm) {
			return;
		}

		// Begin consuming the next generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_generation (OECatalogScanComm comm) {
			return;
		}

		// End consuming a generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_generation (OECatalogScanComm comm) {
			return;
		}

		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		//@Override
		//public void next_rup (OECatalogScanComm comm) {
		//	return;
		//}

		// Next sterile rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_sterile_rup (OECatalogScanComm comm) {
			return;
		}


		//----- Service methods (for use by subclasses) -----

		// Find the time bin for the current rupture, within the time bins being counted.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.
		// Returns the time bin that contains the rupture.
		// Returns -1 if before the first bin, cat_time_bins if after the last bin.

		protected final int find_cur_rup_time_bin (OECatalogScanComm comm) {
		
			// Find the time bin for this rupture,
			// -1 if before the first bin, cat_time_bins if after the last bin

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 0, cat_time_bins + 1) - 1;

			return time_ix;
		}


		// Count the current rupture, within the time bins being counted.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.
		// Returns the time bin that contains the rupture.
		// Returns -1 if before the first bin, cat_time_bins if after the last bin.
		// Note that a return other than -1 or cat_time_bins does not mean the rupture was counted,
		// because it might not lie in a magnitude bin.

		protected final int count_cur_rup (OECatalogScanComm comm) {
		
			// Find the time bin for this rupture,
			// -1 if before the first bin, cat_time_bins if after the last bin

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 0, cat_time_bins + 1) - 1;

			if (time_ix < 0 || time_ix >= cat_time_bins) {
				return time_ix;
			}
		
			// Find the magnitude bin for this rupture,
			// -1 if before the first bin, active_mag_bins if after the last active bin

			int mag_ix = OEArraysCalc.bsearch_array (mag_values, comm.rup.rup_mag, 0, active_mag_bins + 1) - 1;

			if (mag_ix < 0 || mag_ix >= active_mag_bins) {
				return time_ix;
			}

			// Count the rupture

			csr_counts[time_ix][mag_ix]++;

			return time_ix;
		}
	}




	// Consumer for no infill.

	private class ConsumerNone extends ConsumerBase {

		//----- Accumulators -----


		//----- Construction -----

		// Default constructor.

		public ConsumerNone () {
		}

		//----- Open/Close methods (Implementation of OECatalogConsumer) -----


		//----- Data methods (Implementation of OECatalogConsumer) -----

		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {

			// Count the rupture

			count_cur_rup (comm);
			return;
		}

	}




	// Consumer for infill by scaling.

	private class ConsumerScale extends ConsumerBase {

		//----- Accumulators -----

		// Count in each time bin, in the current generation.
		// Dimension: cur_gen_counts[time_bins]

		protected int[] cur_gen_counts;

		// The last magnitude bin + 1 that needs infill in the current generation.
		// 0 if no infill needed, active_mag_bins if all active bins need infill.

		protected int infill_mag_bin_hi;


		//----- Construction -----

		// Default constructor.

		public ConsumerScale () {
			cur_gen_counts = new int[time_bins];
			infill_mag_bin_hi = 0;
		}

		//----- Open/Close methods (Implementation of OECatalogConsumer) -----


		//----- Data methods (Implementation of OECatalogConsumer) -----

		// Begin consuming the next generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_generation (OECatalogScanComm comm) {
		
			// Zero the counts for the current generation

			OEArraysCalc.zero_array (cur_gen_counts);

			// Get the range of magnitude bins that require infill
			// (the offset by mag_eps avoids near-zero-range infill);
			// note this is based on the min mag of the current generation

			infill_mag_bin_hi = OEArraysCalc.bsearch_array (
						mag_values, comm.gen_info.gen_mag_min - comm.cat_params.mag_eps, 0, active_mag_bins);

			return;
		}

		// End consuming a generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_generation (OECatalogScanComm comm) {

			// Loop over magnitude bins needing infill ...

			for (int mag_ix = 0; mag_ix < infill_mag_bin_hi; ++mag_ix) {
			
				// Get the scaling ratio

				double scale = OERandomGenerator.gr_ratio_rate (
					comm.cat_params.b,											// b
					comm.gen_info.gen_mag_min,									// sm1
					comm.gen_info.gen_mag_max,									// sm2
					mag_values[mag_ix],											// tm1
					Math.min(mag_values[mag_ix+1], comm.gen_info.gen_mag_min)	// tm2
				);

				// Apply the scaling ratio in each time bin (that is being counted) ...

				for (int time_ix = 0; time_ix < cat_time_bins; ++time_ix) {
					csr_counts[time_ix][mag_ix] += (int)Math.round (
						((double)(cur_gen_counts[time_ix])) * scale
					);
				}
			}

			return;
		}

		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {

			// Count the rupture, and get the time bin containing the rupture,
			// -1 if before the first bin, cat_time_bins if after the last bin

			int time_ix = count_cur_rup (comm);

			// Count the rupture within the current generation

			if (time_ix >= 0 && time_ix < cat_time_bins) {
				cur_gen_counts[time_ix]++;
			}

			return;
		}

	}




	// Consumer for infill by Poisson random values.

	private class ConsumerPoisson extends ConsumerBase {

		//----- Accumulators -----

		// The accumulated expected values.
		// Dimension: csr_counts[time_bins][mag_bins]
		// Each bin contains the total expected number of ruptures below
		// the minimum magnitudes of the generations.

		private double[][] csr_expected;

		// Rates for each time bin, for the current generation.
		// Dimension: cur_gen_rates[time_bins]
		// Each element contains the sum of the rates:
		//  r = SUM( k * Integral(((t-t0+c)^(-p))) )
		// where k is the rupture productivity, t0 is the rupture time,
		// p and c are the Omori parameters, the sum runs over all ruptures
		// in the current generation, and each integral runs over the
		// portion of the time bin that comes after the rupture.
		// With this definition, the rate of ruptures in the time bin per
		// unit magnitude is:
		//  r * b * log(10) * (10^(-b*(m - mref)))
		// where b is the Gutenberg-Richter parameter and mref is the reference
		// magnitude.

		private double[] cur_gen_rates;

		// The last magnitude bin + 1 that needs infill in the current generation.
		// 0 if no infill needed, active_mag_bins if all active bins need infill.

		private int infill_mag_bin_hi;


		//----- Construction -----

		// Default constructor.

		public ConsumerPoisson () {
			csr_expected = new double[time_bins][mag_bins];
			cur_gen_rates = new double[time_bins];
			infill_mag_bin_hi = 0;
		}


		//----- Open/Close methods (Implementation of OECatalogConsumer) -----


		//----- Data methods (Implementation of OECatalogConsumer) -----

		// Begin consuming a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog values set up.

		@Override
		public void begin_catalog (OECatalogScanComm comm) {
			super.begin_catalog (comm);
		
			// Zero the expected values

			OEArraysCalc.zero_array (csr_expected);
			return;
		}

		// End consuming a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog values set up.

		@Override
		public void end_catalog (OECatalogScanComm comm) {

			// Increase each count by a Poisson random value

			OEArraysCalc.add_poisson_array (comm.rangen, csr_counts, csr_expected, 0, cat_time_bins);

			super.end_catalog (comm);
			return;
		}

		// Begin consuming the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_seed_generation (OECatalogScanComm comm) {
		
			// Zero the rates for the current generation

			OEArraysCalc.zero_array (cur_gen_rates);

			// Get the range of magnitude bins that require infill
			// (the offset by mag_eps avoids near-zero-range infill);
			// note this is based on the min mag of the next generation

			if (comm.f_final_gen) {
				infill_mag_bin_hi = 0;
			} else {
				infill_mag_bin_hi = OEArraysCalc.bsearch_array (
							mag_values, comm.next_gen_info.gen_mag_min - comm.cat_params.mag_eps, 0, active_mag_bins);
			}

			return;
		}

		// End consuming the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_seed_generation (OECatalogScanComm comm) {

			// Loop over magnitude bins needing infill ...

			for (int mag_ix = 0; mag_ix < infill_mag_bin_hi; ++mag_ix) {
			
				// Get the magnitude integral

				double mag_int = OERandomGenerator.gr_rate (
					comm.cat_params.b,												// b
					comm.cat_params.mref,											// mref
					mag_values[mag_ix],												// m1
					Math.min(mag_values[mag_ix+1], comm.next_gen_info.gen_mag_min)	// m2
				);

				// Accumulate the rates within each time bin

				for (int time_ix = 0; time_ix < cat_time_bins; ++time_ix) {
					csr_expected[time_ix][mag_ix] += (cur_gen_rates[time_ix] * mag_int);
				}
			}

			return;
		}

		// Next rupture in the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_seed_rup (OECatalogScanComm comm) {
		
			// Find the time bin for this rupture,
			// -1 if before the first bin, cat_time_bins if after the last bin

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 0, cat_time_bins + 1) - 1;

			// If infilling, add the rate for each time bin after the rupture

			if (infill_mag_bin_hi > 0) {
				for (int tix = Math.max (time_ix, 0); tix < cat_time_bins; ++tix) {
					cur_gen_rates[tix] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
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
		
			// Zero the rates for the current generation

			OEArraysCalc.zero_array (cur_gen_rates);

			// Get the range of magnitude bins that require infill
			// (the offset by mag_eps avoids near-zero-range infill);
			// note this is based on the min mag of the next generation

			if (comm.f_final_gen) {
				infill_mag_bin_hi = 0;
			} else {
				infill_mag_bin_hi = OEArraysCalc.bsearch_array (
							mag_values, comm.next_gen_info.gen_mag_min - comm.cat_params.mag_eps, 0, active_mag_bins);
			}

			return;
		}

		// End consuming a generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void end_generation (OECatalogScanComm comm) {

			// Loop over magnitude bins needing infill ...

			for (int mag_ix = 0; mag_ix < infill_mag_bin_hi; ++mag_ix) {
			
				// Get the magnitude integral

				double mag_int = OERandomGenerator.gr_rate (
					comm.cat_params.b,												// b
					comm.cat_params.mref,											// mref
					mag_values[mag_ix],												// m1
					Math.min(mag_values[mag_ix+1], comm.next_gen_info.gen_mag_min)	// m2
				);

				// Accumulate the rates within each time bin

				for (int time_ix = 0; time_ix < cat_time_bins; ++time_ix) {
					csr_expected[time_ix][mag_ix] += (cur_gen_rates[time_ix] * mag_int);
				}
			}

			return;
		}

		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {

			// Count the rupture, and get the time bin containing the rupture,
			// -1 if before the first bin, cat_time_bins if after the last bin

			int time_ix = count_cur_rup (comm);

			// If infilling, add the rate for each time bin after the rupture

			if (infill_mag_bin_hi > 0) {
				for (int tix = Math.max (time_ix, 0); tix < cat_time_bins; ++tix) {
					cur_gen_rates[tix] += (comm.rup.k_prod * OERandomGenerator.omori_rate_shifted (
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

		// Next sterile rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_sterile_rup (OECatalogScanComm comm) {
			return;
		}

	}




	// Consumer for sterile ruptures.

	private class ConsumerSterile extends ConsumerBase {

		//----- Accumulators -----


		//----- Construction -----

		// Default constructor.

		public ConsumerSterile () {
		}


		//----- Open/Close methods (Implementation of OECatalogConsumer) -----


		//----- Data methods (Implementation of OECatalogConsumer) -----

		// Begin consuming the next generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog and per-generation values set up.

		@Override
		public void begin_generation (OECatalogScanComm comm) {

			// Request sterile ruptures if needed, down to our lowest magnitude

			comm.set_sterile_mag (mag_values[0]);

			return;
		}

		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {

			// Count the rupture

			count_cur_rup (comm);
			return;
		}

		// Next sterile rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_sterile_rup (OECatalogScanComm comm) {

			// Count the rupture

			count_cur_rup (comm);
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
	
		// Return the consumer for the selected infill method

		OECatalogConsumer consumer = null;

		switch (infill_meth) {
		default:
			throw new IllegalStateException ("OEAccumVarTimeMag.make_consumer: Invalid infill method, infill_meth = " + infill_meth);

		case INFILL_METH_NONE:
			consumer = new ConsumerNone();
			break;

		case INFILL_METH_SCALE:
			consumer = new ConsumerScale();
			break;

		case INFILL_METH_POISSON:
			consumer = new ConsumerPoisson();
			break;

		case INFILL_METH_STERILE:
			consumer = new ConsumerSterile();
			break;
		}

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
	
		// Allocate the accumulator

		if (capacity > acc_capacity) {
			acc_capacity = capacity;
			acc_counts = new int[time_bins][mag_bins][acc_capacity];
			acc_counted_bins = new int[acc_capacity];
		}

		// Initialize the counters

		acc_size = 0;
		acc_catix.set(0);
		acc_bin_size = null;		// created during end_accumulation
		acc_live_counts = null;		// created during end_accumulation

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
	
		// If increasing capacity, resize accumulator

		if (capacity > acc_capacity) {
			acc_capacity = capacity;
			OEArraysCalc.resize_each_array_column (acc_counts, acc_capacity);
			acc_counted_bins = Arrays.copyOf (acc_counted_bins, acc_capacity);
		}

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

		// Get the size

		acc_size = acc_catix.get();
	
		// Sort each column, so fractiles are available

		OEArraysCalc.sort_each_array_column (acc_counts, 0, acc_size);

		// Get the bin sizes by searching for the omit value in each column

		acc_bin_size = OEArraysCalc.bsearch_each_array_column (acc_counts, OMIT_BIN - 1, 0, acc_size);

		// Cumulate the number of live catalogs

		acc_live_counts = new int[time_bins];
		OEArraysCalc.zero_array (acc_live_counts);

		for (int j = 0; j < acc_size; ++j) {
			int n = acc_counted_bins[j];
			if (n > 0) {
				acc_live_counts[n - 1]++;
			}
		}

		OEArraysCalc.cumulate_array (acc_live_counts, false) ;

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


	// Get the capacity.

	public final int get_capacity () {
		return acc_capacity;
	}


	// Get the size, which is the number of catalogs accumulated.

	public final int get_size () {
		return acc_size;
	}




	// Get a fractile.
	// Parameters:
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is an integer N, such that the probability of N or fewer
	// ruptures is approximately equal to fractile.
	// Note: The returned array should be newly-allocated and not retained by this object.

	@Override
	public final int[][] get_fractile_array (double fractile) {
		return OEArraysCalc.fractile_each_array_column (acc_counts, fractile, acc_bin_zero, acc_bin_size);
	}




	// Get the probability of occurrence array.
	// Returns a 2D array with dimensions r[time_bins][mag_bins].
	// Each element corresponds to one bin (which is cumulative by definition).
	// Each value is a real number v, such that v is the probability that
	// at least one rupture occurs.

	public final double[][] get_prob_occur_array () {
		return OEArraysCalc.probex_each_array_column (acc_counts, 0, acc_bin_zero, acc_bin_size);
	}




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

	@Override
	public final double[][] get_prob_occur_array (int xcount) {
		return OEArraysCalc.probex_each_array_column (acc_counts, xcount, acc_bin_zero, acc_bin_size);
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
				result.append (String.format ("  %10.5f", prob_occur_array[time_ix][mag_ix] * 100.0));
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




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 99001;

	private static final String M_VERSION_NAME = "OEAccumVarTimeMag";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt         ("infill_meth"      , infill_meth      );
			writer.marshalInt         ("active_time_bins" , active_time_bins );
			writer.marshalInt         ("active_mag_bins"  , active_mag_bins  );
			writer.marshalDouble      ("min_cat_stop_time", min_cat_stop_time);

			writer.marshalInt         ("time_bins"        , time_bins        );
			writer.marshalInt         ("mag_bins"         , mag_bins         );
			writer.marshalDoubleArray ("time_values"      , time_values      );
			writer.marshalDoubleArray ("mag_values"       , mag_values       );

			writer.marshalInt         ("acc_capacity"     , acc_capacity     );
			writer.marshalInt         ("acc_size"         , acc_size         );
			writer.marshalInt3DArray  ("acc_counts"       , acc_counts       );
			writer.marshalIntArray    ("acc_counted_bins" , acc_counted_bins );
			writer.marshalInt2DArray  ("acc_bin_zero"     , acc_bin_zero     );
			writer.marshalInt2DArray  ("acc_bin_size"     , acc_bin_size     );
			writer.marshalIntArray    ("acc_live_counts"  , acc_live_counts  );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			infill_meth       = reader.unmarshalInt          ("infill_meth"      );
			active_time_bins  = reader.unmarshalInt          ("active_time_bins" );
			active_mag_bins   = reader.unmarshalInt          ("active_mag_bins"  );
			min_cat_stop_time = reader.unmarshalDouble       ("min_cat_stop_time");

			time_bins         = reader.unmarshalInt          ("time_bins"        );
			mag_bins          = reader.unmarshalInt          ("mag_bins"         );
			time_values       = reader.unmarshalDoubleArray  ("time_values"      );
			mag_values        = reader.unmarshalDoubleArray  ("mag_values"       );

			acc_capacity      = reader.unmarshalInt          ("acc_capacity"     );
			acc_size          = reader.unmarshalInt          ("acc_size"         );
			acc_counts        = reader.unmarshalInt3DArray   ("acc_counts"       );
			acc_counted_bins  = reader.unmarshalIntArray     ("acc_counted_bins" );
			acc_bin_zero      = reader.unmarshalInt2DArray   ("acc_bin_zero"     );
			acc_bin_size      = reader.unmarshalInt2DArray   ("acc_bin_size"     );
			acc_live_counts   = reader.unmarshalIntArray     ("acc_live_counts"  );

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OEAccumVarTimeMag unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEAccumVarTimeMag accumulator) {
		writer.marshalMapBegin (name);
		accumulator.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEAccumVarTimeMag static_unmarshal (MarshalReader reader, String name) {
		OEAccumVarTimeMag accumulator = new OEAccumVarTimeMag();
		reader.unmarshalMapBegin (name);
		accumulator.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return accumulator;
	}




	//----- Testing -----




	// Set up using typical values for time and magnitude.
	// Parameters:
	//  the_infill_meth = The infill method to use.
	//  tbegin = Begin time for forecast, in days.

	public void typical_test_setup (int the_infill_meth, double tbegin) {
	
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

		setup (the_infill_meth, the_time_values, the_mag_values);
		return;
	}




	// Create a typical set of test outputs, as a string.

	public String typical_test_outputs_to_string () {
		StringBuilder result = new StringBuilder();

		int[][] fractile_array;
		double[][] prob_occur_array;

		// Number of catalog processed

		result.append ("\n");
		result.append ("Catalogs = " + acc_size + "\n");
		result.append ("\n");

		// Live counts

		result.append ("\n");
		result.append ("Live counts\n");
		result.append ("\n");

		result.append (live_count_array_to_string (acc_live_counts));

		// Bin sizes

		result.append ("\n");
		result.append ("Bin sizes\n");
		result.append ("\n");

		result.append (fractile_array_to_string (acc_bin_size));

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
	//  the_infill_meth = Infill method to use.
	//  num_cats = Number of catalogs to run.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run (OECatalogParams test_cat_params, double mag_main, int the_infill_meth, int num_cats) {

		// Say hello

		System.out.println ();
		System.out.println ("Generating " + num_cats + " catalogs");
		System.out.println ();

		// Make the accumulator and set up the bins

		OEAccumVarTimeMag time_mag_accum = new OEAccumVarTimeMag();
		time_mag_accum.typical_test_setup (the_infill_meth, test_cat_params.tbegin);

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
	//  the_infill_meth = Infill method to use.
	//  num_cats = Number of catalogs to run.
	//  num_threads = Number of threads to use, can be -1 for default number of threads.
	//  max_runtime = Maximum running time allowed.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run_mt (OECatalogParams test_cat_params, double mag_main, int the_infill_meth, int num_cats, int num_threads, long max_runtime) {

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

		OEAccumVarTimeMag time_mag_accum = new OEAccumVarTimeMag();
		time_mag_accum.typical_test_setup (the_infill_meth, test_cat_params.tbegin);

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
			System.err.println ("OEAccumVarTimeMag : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumVarTimeMag : Invalid 'test1' subcommand");
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
				int the_infill_meth = Integer.parseInt (args[10]);
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
				System.out.println ("the_infill_meth = " + the_infill_meth);
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

				typical_test_run (test_cat_params, mag_main, the_infill_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the catalog summary and generation list.
		// Then display the accumulated fractiles and probability of occurrence.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumVarTimeMag : Invalid 'test2' subcommand");
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
				int the_infill_meth = Integer.parseInt (args[10]);
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
				System.out.println ("the_infill_meth = " + the_infill_meth);
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

				typical_test_run (test_cat_params, mag_main, the_infill_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Same as test #1 and #2 except with control over the magnitude ranges.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 15 additional arguments

			if (args.length != 16) {
				System.err.println ("OEAccumVarTimeMag : Invalid 'test3' subcommand");
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
				int the_infill_meth = Integer.parseInt (args[10]);
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
				System.out.println ("the_infill_meth = " + the_infill_meth);
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

				typical_test_run (test_cat_params, mag_main, the_infill_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  num_threads  max_runtime
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #3 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 17 additional arguments

			if (args.length != 18) {
				System.err.println ("OEAccumVarTimeMag : Invalid 'test4' subcommand");
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
				int the_infill_meth = Integer.parseInt (args[10]);
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
				System.out.println ("the_infill_meth = " + the_infill_meth);
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

				typical_test_run_mt (test_cat_params, mag_main, the_infill_meth, num_cats, num_threads, max_runtime);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mag_excess
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Same as test #1 thru #4 except with control over the magnitude ranges and excess.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 16 additional arguments

			if (args.length != 17) {
				System.err.println ("OEAccumVarTimeMag : Invalid 'test5' subcommand");
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
				int the_infill_meth = Integer.parseInt (args[10]);
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
				System.out.println ("the_infill_meth = " + the_infill_meth);
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

				typical_test_run (test_cat_params, mag_main, the_infill_meth, num_cats);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi  mag_excess  num_threads  max_runtime
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the accumulated fractiles and probability of occurrence.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #5 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 18 additional arguments

			if (args.length != 19) {
				System.err.println ("OEAccumVarTimeMag : Invalid 'test6' subcommand");
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
				int the_infill_meth = Integer.parseInt (args[10]);
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
				System.out.println ("the_infill_meth = " + the_infill_meth);
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

				typical_test_run_mt (test_cat_params, mag_main, the_infill_meth, num_cats, num_threads, max_runtime);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEAccumVarTimeMag : Unrecognized subcommand : " + args[0]);
		return;

	}

}
