package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.AutoExecutorService;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;


// Operational ETAS catalog accumulator for auto-ranging a simulation.
// Author: Michael Barall 03/01/2022.
//
// The intended application of this accumulator is to perform preliminary
// simulations to determine an end time with an acceptable rate of early
// terminations, and a magnitude range with an acceptable number of events.
//
// This object contains a 1D array of bins, each representing a range of
// time.  Bins are cumulative, and include all ruptures at earlier times.
// This accumulator differs from others in that the first time bin includes
// ruptures that occur at any time within or prior to the first bin.
//
// The contents of each bin is an array of rupture counts, one per catalog.
// During accumulation, the counts for the n-th catalog are stored in the
// n-th element of each column.  After accumulation, each bin is sorted,
// so that fractiles may be extracted.
//
// Catalogs may have variable length, and there is an array to accumulate
// the number of catalogs that terminate within or prior to each time bin.

public class OEAccumSimRanging implements OEEnsembleAccumulator, OEAccumReadoutRanging {

	//----- Control variables -----

	// The value to insert into a bin to indicate it is omitted.
	// It is large so it moves to the end when the list of counts is sorted.

	private static final int OMIT_BIN = Integer.MAX_VALUE;

	// The number of time bins that are used for reporting ruptures.

	private int active_time_bins;




	//----- Bin definitions -----

	// The number of time bins.

	private int time_bins;

	// The time values, in days.
	// Dimension: time_values[time_bins + 1]
	// Each time bin represents an interval between two successive time values.

	private double[] time_values;




	//----- Accumulators -----

	// The current capacity of the accumulator.

	private int acc_capacity;

	// The size of the accumulator, that is, the number of catalogs accumulated.

	private int acc_size;

	// The accumulated counts.
	// Dimension: acc_counts[time_bins][acc_capacity]

	private int[][] acc_counts;

	// The accumulated counts, with columns sorted.
	// Dimension: acc_counts_sorted[time_bins][acc_size]
	// Columns are lazy-allocated and sorted when needed for readout.

	private int[][] acc_counts_sorted;

	// The accumulated highest magnitudes.
	// Dimension: acc_high_mags[time_bins][acc_capacity]

	private double[][] acc_high_mags;

	// The accumulated highest magnitudes, with columns sorted.
	// Dimension: acc_high_mags[time_bins][acc_size]
	// Columns are lazy-allocated and sorted when needed for readout.

	private double[][] acc_high_mags_sorted;

	// The number of time bins that are being counted in each catalog.
	// Dimension: acc_counted_bins[acc_capacity]

	private int[] acc_counted_bins;

	// An array containing zero for each bin, used as lower limit of column index.
	// Dimension: acc_bin_zero[time_bins]

	private int[] acc_bin_zero;

	// The number of counts in each bin, also used as upper limit of column index.
	// Dimension: acc_bin_size[time_bins]

	private int[] acc_bin_size;

	// The number of catalogs that stopped before or within each time bin.
	// Dimension: acc_stop_counts[time_bins]

	private int[] acc_stop_counts;


	// The next catalog index to use.

	private AtomicInteger acc_catix = new AtomicInteger();

	// Get the next catalog index to use.
	// Throw an exception if all capacity is exhausted.

	private int get_acc_catix () {
		int n;
		do {
			n = acc_catix.get();
			if (n >= acc_capacity) {
				throw new IllegalStateException ("OEAccumSimRanging.get_acc_catix: No room in accumulator");
			}
		} while (!( acc_catix.compareAndSet (n, n+1) ));
		return n;
	}




	//----- Construction -----




	// Erase the contents.

	public final void clear () {
		active_time_bins = 0;

		time_bins = 0;
		time_values = new double[0];

		acc_capacity = 0;
		acc_size = 0;
		acc_catix.set(0);

		acc_counts = new int[0][0];
		acc_counts_sorted = new int[0][0];
		acc_high_mags = new double[0][0];
		acc_high_mags_sorted = new double[0][0];
		acc_counted_bins = new int[0];
		acc_bin_zero = new int[0];
		acc_bin_size = new int[0];
		acc_stop_counts = new int[0];

		return;
	}




	// Default constructor.

	public OEAccumSimRanging () {
		clear();
	}




	// Set up to begin accumulating.
	// Parameters:
	//  the_infill_meth = The infill method to use (currently not used).
	//  the_time_values = The time values to define the bins, in days, must be in increasing order.
	// Note: The function stores a copy of the given array.

	public void setup (int the_infill_meth, double[] the_time_values) {

		// Parameter validation

		if (!( the_time_values != null && the_time_values.length >= 2 )) {
			throw new IllegalArgumentException ("OEAccumSimRanging.setup: Missing time values");
		}

		for (int i = 0; i+1 < the_time_values.length; ++i) {
			if (!( the_time_values[i] < the_time_values[i+1] )) {
			throw new IllegalArgumentException ("OEAccumSimRanging.setup: Out-of-order time values: index = " + i + ", value[i] = " + the_time_values[i] + ", value[i+1] = " + the_time_values[i+1]);
			}
		}

		// Copy parameters

		time_bins = the_time_values.length - 1;
		time_values = Arrays.copyOf (the_time_values, the_time_values.length);

		active_time_bins = time_bins;

		// Empty accumulators

		acc_capacity = 0;
		acc_size = 0;
		acc_catix.set(0);

		acc_counts = null;
		acc_counts_sorted = null;
		acc_high_mags = null;
		acc_high_mags_sorted = null;
		acc_counted_bins = null;
		acc_bin_zero = new int[time_bins];
		OEArraysCalc.zero_array (acc_bin_zero);
		acc_bin_size = null;
		acc_stop_counts = null;

		return;
	}




	//----- Consumers -----




	// Consumer common base class.
	// The general design of a consumer is that it contains an array of bins,
	// which are used to accumulate ruptures in the catalog.
	// When the consumer is closed, the bin counts are transferred into one
	// column of the global array, which requires synchronization only to
	// get the column index.
	// The consumer also contains the number of time bins for which the
	// catalog is live, which is added to the global live counts.

	private class ConsumerBase implements OECatalogConsumer {

		//----- Accumulators -----

		// True if consumer is open.

		protected boolean f_open;

		// The accumulated counts.
		// Dimension: csr_counts[time_bins]

		protected int[] csr_counts;

		// The accumulated highes magnitudes.
		// Dimension: csr_high_mags[time_bins]

		protected double[] csr_high_mags;

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
			csr_counts = new int[time_bins];
			csr_high_mags = new double[time_bins];
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

			// Set the high magnitudes to a large negative value

			OEArraysCalc.fill_array (csr_high_mags, OEConstants.INFINITE_MAG_NEG);

			// The effective end time is the stop time, but not after the configured end time

			double eff_tend = Math.min (comm.cat_params.tend, comm.cat_stop_time);

			// Calculate the bin during which the catalog stops
			// (accepting a bin as complete if we get within epsilon of the bin's end time)
			// -1 if before the first bin, active_time_bins if after the last active bin

			stop_time_bin = OEArraysCalc.bsearch_array (time_values, eff_tend + comm.cat_params.teps, 0, active_time_bins + 1) - 1;

			// The number of time bins for which we count ruptures is by default
			// the number of bins before the bin that contains the stop time
			// (the number of bins for which the catalog reaches the end of the bin)

			cat_time_bins = Math.max (stop_time_bin, 0);

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
		
				// Cumulate the counts: time upward; for the reporting time bins

				OEArraysCalc.cumulate_array (csr_counts, true, 0, report_time_bins);

				// Fill omit value into time bins that we are not reporting

				OEArraysCalc.fill_array (csr_counts, report_time_bins, time_bins, OMIT_BIN);
		
				// Cumulate the high magnitudes: time upward; for the reporting time bins

				double high_mag = csr_high_mags[0];
				for (int j = 1; j < report_time_bins; ++j) {
					if (high_mag > csr_high_mags[j]) {
						csr_high_mags[j] = high_mag;
					} else {
						high_mag = csr_high_mags[j];
					}
				}

				// Fill large positive value into time bins that we are not reporting

				OEArraysCalc.fill_array (csr_high_mags, report_time_bins, time_bins, OEConstants.INFINITE_MAG_POS);

				// Get the index for this catalog

				int catix = get_acc_catix();

				// Store our counts and high magnitudes into the accumulators

				OEArraysCalc.set_each_array_column (acc_counts, catix, csr_counts);
				OEArraysCalc.set_each_array_column (acc_high_mags, catix, csr_high_mags);

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

		@Override
		public void next_rup (OECatalogScanComm comm) {
		
			// Find the time bin for this rupture,
			// 0 if in or before the first bin, cat_time_bins if after the last bin

			int time_ix = OEArraysCalc.bsearch_array (time_values, comm.rup.t_day, 1, cat_time_bins + 1) - 1;

			if (time_ix >= cat_time_bins) {
				return;
			}

			// Count the rupture
			// For this accumulator, ruptures before the first bin are counted in the first bin

			csr_counts[time_ix]++;

			// Check if this is a new highest magnitude

			if (comm.rup.rup_mag > csr_high_mags[time_ix]) {
				csr_high_mags[time_ix] = comm.rup.rup_mag;
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
	
		// Return the consumer

		OECatalogConsumer consumer = new ConsumerBase();;
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
			acc_counts = new int[time_bins][acc_capacity];
			acc_high_mags = new double[time_bins][acc_capacity];
			acc_counted_bins = new int[acc_capacity];
		}

		// Initialize the counters

		acc_size = 0;
		acc_catix.set(0);
		acc_bin_size = null;		// created during end_accumulation
		acc_stop_counts = null;		// created during end_accumulation

		// Initialize the sorted accumulators for readout

		acc_counts_sorted = null;		// created during end_accumulation
		acc_high_mags_sorted = null;	// created during end_accumulation

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
			OEArraysCalc.resize_each_array_column (acc_high_mags, acc_capacity);
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
	
		//// Sort each column, so fractiles are available
		//
		//OEArraysCalc.sort_each_array_column (acc_counts, 0, acc_size);
		//OEArraysCalc.sort_each_array_column (acc_high_mags, 0, acc_size);
		//
		//// Get the bin sizes by searching for the omit value in each column
		//
		//acc_bin_size = OEArraysCalc.bsearch_each_array_column (acc_counts, OMIT_BIN - 1, 0, acc_size);

		// Allocate bin size array and fill with -1 to indicate size unknown

		acc_bin_size = new int[time_bins];
		OEArraysCalc.fill_array (acc_bin_size, -1);

		// Cumulate the number of live catalogs

		acc_stop_counts = new int[time_bins];
		OEArraysCalc.zero_array (acc_stop_counts);

		for (int j = 0; j < acc_size; ++j) {
			int n = Math.max (0, acc_counted_bins[j]);
			if (n < active_time_bins) {
				acc_stop_counts[n]++;
			}
		}

		OEArraysCalc.cumulate_array (acc_stop_counts, true);

		// Allocate the readout arrays, top-level only

		acc_counts_sorted = new int[time_bins][];
		acc_high_mags_sorted = new double[time_bins][];

		return;
	}




	//----- Readout functions -----




	// Get the number of time bins.

	public final int get_time_bins () {
		return time_bins;
	}


	// Get a time value.

	public final double get_time_value (int n) {
		return time_values[n];
	}


	// Get the capacity.

	public final int get_capacity () {
		return acc_capacity;
	}


	// Get the size, which is the number of catalogs accumulated.

	public final int get_size () {
		return acc_size;
	}




	// Get the size of a bin, which is the number of catalogs contributing to the bin.
	// Parameters:
	//  n = Bin number.

	public final int get_bin_size (int n) {
		int x = acc_bin_size[n];
		
		// If bin size is unknown ...

		if (x < 0) {

			// Copy and sort the column, so fractiles are available

			acc_counts_sorted[n] = Arrays.copyOf (acc_counts[n], acc_size);
			acc_high_mags_sorted[n] = Arrays.copyOf (acc_high_mags[n], acc_size);

			Arrays.sort (acc_counts_sorted[n], 0, acc_size);
			Arrays.sort (acc_high_mags_sorted[n], 0, acc_size);

			// Get the bin size by searching for the omit value in the column

			x = OEArraysCalc.bsearch_array (acc_counts_sorted[n], OMIT_BIN - 1, 0, acc_size);
			acc_bin_size[n] = x;
		}

		return x;
	}




	// Get a fractile of a bin.
	// Parameters:
	//  n = Bin number.
	//  fractile = Fractile to find, should be between 0.0 and 1.0.
	// Returns an integer N, such that, among all catalogs that complete bin n,
	// the probability that the catalog has N or fewer ruptures in bins 0 thru n
	// (inclusive) is approximately equal to fractile.
	// (Note that the appropriate bin number is often one less than the
	// return value of get_survival_bins()).

	@Override
	public final int get_bin_fractile (int n, double fractile) {
		int hi = get_bin_size (n);
		return OEArraysCalc.fractile_array (acc_counts_sorted[n], fractile, 0, hi);
	}




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

	@Override
	public final int get_survival_bins (double stop_fraction) {
		int v = (int)Math.round (((double)(acc_size)) * stop_fraction);
		return OEArraysCalc.bsearch_array (acc_stop_counts, v, 0, active_time_bins);
	}




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

	@Override
	public final double get_high_mag_fractile (int n, double fractile, boolean f_total) {
		int hi = get_bin_size (n);	// needed to sort the bin
		return OEArraysCalc.fractile_array (acc_high_mags_sorted[n], fractile, 0, f_total ? acc_size : hi);
	}




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

	@Override
	public final double get_sel_high_mag_fractile (int n, double fractile, int n_sel) {

		// Build an array containing the high magnitudes from the selected catalogs

		double[] x = new double[acc_size];
		int k = 0;
		for (int j = 0; j < acc_size; ++j) {
			if (acc_counts[n_sel][j] != OMIT_BIN) {
				x[k] = acc_high_mags[n][j];
				++k;
			}
		}

		// If no catalogs

		if (k == 0) {
			return OEConstants.INFINITE_MAG_NEG;
		}

		// Sort and get the fractile

		Arrays.sort (x, 0, k);
		return OEArraysCalc.fractile_array (x, fractile, 0, k);
	}




	//----- Testing -----




	// Set up using typical values for time and magnitude.
	// Parameters:
	//  the_infill_meth = The infill method to use (currently not used).
	//  tbegin = Begin time for forecast, in days.

	public void typical_test_setup (int the_infill_meth, double tbegin) {
	
		// Make time array for each day in a year

		double[] the_time_values = new double[366];
		for (int n = 0; n < 366; ++n) {
			the_time_values[n] = tbegin + ((double)(n));
		}

		// Do the setup

		setup (the_infill_meth, the_time_values);
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

		// Survival bins

		result.append ("Survival bins\n");
		result.append ("\n");

		for (int n = 0; n <= 100; ++n) {
			double stop_fraction = ((double)(n)) / 100.0;
			int bins = get_survival_bins (stop_fraction);;
			result.append (String.format ("%10.2f  %6d  %10.2f\n",
				stop_fraction,
				bins,
				time_values[bins]
			));
		}

		result.append ("\n");

		// Time bins and count

		result.append ("Time bins: index, time, bin size, fractiles 0%, 5%, 25%, 50%, 75%, 95%, 100%\n");
		result.append ("\n");

		for (int n = 0; n < time_bins; ++n) {
			result.append (String.format ("%6d  %10.2f %10d %10d %10d %10d %10d %10d %10d %10d\n",
				n,
				time_values[n+1],
				get_bin_size (n),
				get_bin_fractile (n, 0.00),
				get_bin_fractile (n, 0.05),
				get_bin_fractile (n, 0.25),
				get_bin_fractile (n, 0.50),
				get_bin_fractile (n, 0.75),
				get_bin_fractile (n, 0.95),
				get_bin_fractile (n, 1.00)
			));
		}

		result.append ("\n");

		// High magnitudes

		result.append ("High mags: index, time, bin size, fractiles 0%, 5%, 25%, 50%, 75%, 95%, 100%\n");
		result.append ("\n");

		for (int n = 0; n < time_bins; ++n) {
			result.append (String.format ("%6d  %10.2f %10d %10.4f %10.4f %10.4f %10.4f %10.4f %10.4f %10.4f\n",
				n,
				time_values[n+1],
				get_bin_size (n),
				get_high_mag_fractile (n, 0.00, true),
				get_high_mag_fractile (n, 0.05, true),
				get_high_mag_fractile (n, 0.25, true),
				get_high_mag_fractile (n, 0.50, true),
				get_high_mag_fractile (n, 0.75, true),
				get_high_mag_fractile (n, 0.95, true),
				get_high_mag_fractile (n, 1.00, true)
			));
		}

		result.append ("\n");

		return result.toString();
	}




	// Perform a typical test run.
	// Parameters:
	//  test_cat_params = Catalog parameters.
	//  mag_main = Mainshock magnitude.
	//  the_infill_meth = Infill method to use (currently not used).
	//  num_cats = Number of catalogs to run.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run (OECatalogParams test_cat_params, double mag_main, int the_infill_meth, int num_cats) {

		// Say hello

		System.out.println ();
		System.out.println ("Generating " + num_cats + " catalogs");
		System.out.println ();

		// Make the accumulator and set up the bins

		OEAccumSimRanging time_mag_accum = new OEAccumSimRanging();
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
	//  the_infill_meth = Infill method to use (currently not used).
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

		OEAccumSimRanging time_mag_accum = new OEAccumSimRanging();
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
			System.err.println ("OEAccumSimRanging : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin  infill_meth  num_cats
		// Build a catalog with the given parameters.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then display the ranging outputs.
		// Note that infill_meth is not used, but included for consistency with other accumulators and possible future use.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumSimRanging : Invalid 'test1' subcommand");
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
		// Then display the ranging outputs.
		// Note that infill_meth is not used, but included for consistency with other accumulators and possible future use.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OEAccumSimRanging : Invalid 'test2' subcommand");
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
		// Then display the ranging outputs.
		// Note that infill_meth is not used, but included for consistency with other accumulators and possible future use.
		// Same as test #1 and #2 except with control over the magnitude ranges.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 15 additional arguments

			if (args.length != 16) {
				System.err.println ("OEAccumSimRanging : Invalid 'test3' subcommand");
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
		// Then display the ranging outputs.
		// Note that infill_meth is not used, but included for consistency with other accumulators and possible future use.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #3 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 17 additional arguments

			if (args.length != 18) {
				System.err.println ("OEAccumSimRanging : Invalid 'test4' subcommand");
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
		// Then display the ranging outputs.
		// Note that infill_meth is not used, but included for consistency with other accumulators and possible future use.
		// Same as test #1 thru #4 except with control over the magnitude ranges and excess.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 16 additional arguments

			if (args.length != 17) {
				System.err.println ("OEAccumSimRanging : Invalid 'test5' subcommand");
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
		// Then display the ranging outputs.
		// Note that infill_meth is not used, but included for consistency with other accumulators and possible future use.
		// Note that max_runtime must be -1 if no runtime limit is desired.
		// Same as test #5 except with multiple threads.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 18 additional arguments

			if (args.length != 19) {
				System.err.println ("OEAccumSimRanging : Invalid 'test6' subcommand");
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

		System.err.println ("OEAccumSimRanging : Unrecognized subcommand : " + args[0]);
		return;

	}

}
