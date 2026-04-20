package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.util.OEArraysCalc;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;


// Operational ETAS catalog accumulator for collecting information about generations.
// Author: Michael Barall.
//
// This accumulator collects information about all the generations in an
// ensemble of catalogs.

public class OEAccumGenInfo implements OEEnsembleAccumulator {

//	//----- Control variables -----
//
//	// The value to insert into a bin to indicate it is omitted.
//	// It is large so it moves to the end when the list of counts is sorted.
//
//	private static final int OMIT_BIN = Integer.MAX_VALUE;
//
//	// The number of time bins that are used for reporting ruptures.
//
//	private int active_time_bins;
//
//
//
//
//	//----- Bin definitions -----
//
//	// The number of time bins.
//
//	private int time_bins;
//
//	// The time values, in days.
//	// Dimension: time_values[time_bins + 1]
//	// Each time bin represents an interval between two successive time values.
//
//	private double[] time_values;




	//----- Per-catalog information -----

	// Class holding info for one catalog.

	private static class CatInfo {

		// Time at which the catalog stops, defaults to HUGE_TIME_DAYS.

		public double cat_stop_time;

		// True if the stop time is earlier than cat_params.tend.

		public boolean f_early_stop;

		// Number of ruptures in the catalog.

		public int cat_size;

		// Number of ruptures in the catalog, excluding seed ruptures.

		public int cat_etas_size;

		// Number of ruptures in the catalog before the stop time.

		public int cat_valid_size;

		// Number of ruptures in the catalog before the stop time, excluding seed ruptures.

		public int cat_valid_etas_size;

		// Number of generations in the catalog.

		public int gen_count;

		// Size of each generation.
		// Dimension: gen_size[gen_count]

		public int[] gen_size;

		// Number of ruptures in each generation before the stop time.
		// Dimension: gen_size[gen_count]

		public int[] gen_valid_size;

		// Minimum and maximum magnitudes for this generation, from the generation info.
		// Dimension: gen_min_mag[gen_count], gen_max_mag[gen_count]

		public double[] gen_min_mag;
		public double[] gen_max_mag;

		// Minimum and maximum magnitudes for any rupture in this generation.
		// Dimension: rup_min_mag[gen_count], rup_max_mag[gen_count]

		public double[] rup_min_mag;
		public double[] rup_max_mag;

		// Minimum and maximum times for any rupture in this generation.
		// Dimension: rup_min_t_day[gen_count], rup_max_t_day[gen_count]

		public double[] rup_min_t_day;
		public double[] rup_max_t_day;

		// Minimum and maximum magnitudes for any valid rupture in this generation.
		// Dimension: rup_min_mag[gen_count], rup_max_mag[gen_count]

		public double[] rup_valid_min_mag;
		public double[] rup_valid_max_mag;

		// Minimum and maximum times for any valid rupture in this generation.
		// Dimension: rup_min_t_day[gen_count], rup_max_t_day[gen_count]

		public double[] rup_valid_min_t_day;
		public double[] rup_valid_max_t_day;

		// Create an object for the specified number of generations.

		public CatInfo (double cat_stop_time, boolean f_early_stop, int gen_count) {
			this.cat_stop_time = cat_stop_time;
			this.f_early_stop = f_early_stop;
			this.cat_size = 0;
			this.cat_etas_size = 0;
			this.cat_valid_size = 0;
			this.cat_valid_etas_size = 0;
			this.gen_count = gen_count;

			gen_size = new int[gen_count];
			OEArraysCalc.zero_array (gen_size);
			gen_valid_size = new int[gen_count];
			OEArraysCalc.zero_array (gen_valid_size);

			gen_min_mag = new double[gen_count];
			OEArraysCalc.zero_array (gen_min_mag);
			gen_max_mag = new double[gen_count];
			OEArraysCalc.zero_array (gen_max_mag);

			rup_min_mag = new double[gen_count];
			OEArraysCalc.zero_array (rup_min_mag);
			rup_max_mag = new double[gen_count];
			OEArraysCalc.zero_array (rup_max_mag);

			rup_min_t_day = new double[gen_count];
			OEArraysCalc.zero_array (rup_min_t_day);
			rup_max_t_day = new double[gen_count];
			OEArraysCalc.zero_array (rup_max_t_day);

			rup_valid_min_mag = new double[gen_count];
			OEArraysCalc.zero_array (rup_valid_min_mag);
			rup_valid_max_mag = new double[gen_count];
			OEArraysCalc.zero_array (rup_valid_max_mag);

			rup_valid_min_t_day = new double[gen_count];
			OEArraysCalc.zero_array (rup_valid_min_t_day);
			rup_valid_max_t_day = new double[gen_count];
			OEArraysCalc.zero_array (rup_valid_max_t_day);
		}

		// Accumulate a rupture in the given generation.

		public void accum_rup (int i_gen, OERupture rup, boolean f_valid_rup) {
			if (i_gen >= gen_count) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.accum_rup: Generation index out-of-range: i_gen = " + i_gen + ", gen_count = " + gen_count);
			}

			// Catalog count

			++cat_size;
			if (i_gen > 0) {
				++cat_etas_size;
			}

			// Generation counts

			int nc = gen_size[i_gen];
			gen_size[i_gen] = nc + 1;

			// Rupture counts

			if (nc == 0) {
				rup_min_mag[i_gen] = rup.rup_mag;
				rup_max_mag[i_gen] = rup.rup_mag;
				rup_min_t_day[i_gen] = rup.t_day;
				rup_max_t_day[i_gen] = rup.t_day;
			} else {
				rup_min_mag[i_gen] = Math.min (rup_min_mag[i_gen], rup.rup_mag);
				rup_max_mag[i_gen] = Math.max (rup_max_mag[i_gen], rup.rup_mag);
				rup_min_t_day[i_gen] = Math.min (rup_min_t_day[i_gen], rup.t_day);
				rup_max_t_day[i_gen] = Math.max (rup_max_t_day[i_gen], rup.t_day);
			}

			// For valid ruptures ...

			if (f_valid_rup) {

				// Catalog count

				++cat_valid_size;
				if (i_gen > 0) {
					++cat_valid_etas_size;
				}

				// Generation count

				int nv = gen_valid_size[i_gen];
				gen_valid_size[i_gen] = nv + 1;

				// Rupture counts

				if (nv == 0) {
					rup_valid_min_mag[i_gen] = rup.rup_mag;
					rup_valid_max_mag[i_gen] = rup.rup_mag;
					rup_valid_min_t_day[i_gen] = rup.t_day;
					rup_valid_max_t_day[i_gen] = rup.t_day;
				} else {
					rup_valid_min_mag[i_gen] = Math.min (rup_valid_min_mag[i_gen], rup.rup_mag);
					rup_valid_max_mag[i_gen] = Math.max (rup_valid_max_mag[i_gen], rup.rup_mag);
					rup_valid_min_t_day[i_gen] = Math.min (rup_valid_min_t_day[i_gen], rup.t_day);
					rup_valid_max_t_day[i_gen] = Math.max (rup_valid_max_t_day[i_gen], rup.t_day);
				}
			}

			return;
		}

		// Actions at the end of a generation.

		public void end_generation (int i_gen, boolean f_final_gen, int gen_size, int gen_valid_size, OEGenerationInfo gen_info) {
			if (i_gen >= gen_count) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_generation: Generation index out-of-range: i_gen = " + i_gen + ", gen_count = " + gen_count);
			}
			if (f_final_gen) {
				if (i_gen != gen_count - 1) {
					throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_generation: Final generation index invalid: i_gen = " + i_gen + ", gen_count = " + gen_count);
				}
			} else {
				if (i_gen == gen_count - 1) {
					throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_generation: Non-final generation index invalid: i_gen = " + i_gen + ", gen_count = " + gen_count);
				}
			}

			// Check generation size

			if (gen_size != this.gen_size[i_gen]) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_generation: Generation size mismatch: expected = " + gen_size + ", got = " + this.gen_size[i_gen]);
			}
			if (gen_valid_size != this.gen_valid_size[i_gen]) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_generation: Generation valid size mismatch: expected = " + gen_valid_size + ", got = " + this.gen_valid_size[i_gen]);
			}

			// Save generation magnitude range

			gen_min_mag[i_gen] = gen_info.gen_mag_min;
			gen_max_mag[i_gen] = gen_info.gen_mag_max;

			return;
		}

		// Actions at the end of a catalog.

		public void end_catalog (int cat_size, int cat_etas_size, int cat_valid_size) {
			if (cat_size != this.cat_size) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_catalog: Catalog size mismatch: expected = " + cat_size + ", got = " + this.cat_size);
			}
			if (cat_etas_size != this.cat_etas_size) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_catalog: Catalog aftershock size mismatch: expected = " + cat_etas_size + ", got = " + this.cat_etas_size);
			}
			if (cat_valid_size != this.cat_valid_size) {
				throw new InvariantViolationException ("OEAccumGenInfo.CatInfo.end_catalog: Catalog valid size mismatch: expected = " + cat_valid_size + ", got = " + this.cat_valid_size);
			}

			return;
		}

		// Return true if the catalog has at least one valid direct aftershock (in generation 1).

		public boolean has_valid_direct () {
			return (gen_count >= 2 && gen_valid_size[1] > 0);
		}

		// Calculate an effective branch ratio.
		// Note: Only valid ruptures are considered.
		// Note: If D is the number of direct aftershocks, C the total number of aftershocks,
		// and n the branch ratio, then we expect C = D/(1 - n) or equivalently n = 1 - D/C.
		// Note: Caller may want to consider only catalogs for which has_valid_direct() is true;

		public double calc_effective_br () {
			double n = 0.0;
			if (has_valid_direct()) {
				double capD = (double)(gen_valid_size[1]);
				double capC = (double)(cat_valid_etas_size);
				n = 1.0 - (capD / capC);
			}
			return n;
		}

		// Format a line for a generation.

		private String line_for_gen (
			int i_gen,
			String kind,
			int g_size,
			double g_min_mag,
			double g_max_mag,
			double r_min_mag,
			double r_max_mag,
			double r_min_t_day,
			double r_max_t_day
		) {
			if (g_size == 0) {
				return String.format ("%d%s: g_size=%d, g_min_mag=%.3f, g_max_mag=%.3f",
					i_gen,
					kind,
					g_size,
					g_min_mag,
					g_max_mag
				);
			}
			return String.format ("%d%s: g_size=%d, g_min_mag=%.3f, g_max_mag=%.3f, r_min_mag=%.3f, r_max_mag=%.3f, r_min_t_day=%.5f, r_max_t_day=%.5f",
				i_gen,
				kind,
				g_size,
				g_min_mag,
				g_max_mag,
				r_min_mag,
				r_max_mag,
				r_min_t_day,
				r_max_t_day
			);
		}

		// Display our contents.

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append ("CatInfo:" + "\n");

			result.append ("cat_stop_time = " + cat_stop_time + "\n");
			result.append ("f_early_stop = " + f_early_stop + "\n");
			result.append ("cat_size = " + cat_size + "\n");
			result.append ("cat_etas_size = " + cat_etas_size + "\n");
			result.append ("cat_valid_size = " + cat_valid_size + "\n");
			result.append ("cat_valid_etas_size = " + cat_valid_etas_size + "\n");
			result.append ("gen_count = " + gen_count + "\n");
			result.append ("has_valid_direct = " + has_valid_direct() + "\n");
			result.append ("effective_br = " + calc_effective_br() + "\n");

			for (int i_gen = 0; i_gen < gen_count; ++i_gen) {
				if (gen_valid_size[i_gen] == gen_size[i_gen]) {
					String s = line_for_gen (
						i_gen,
						"",
						gen_size[i_gen],
						gen_min_mag[i_gen],
						gen_max_mag[i_gen],
						rup_min_mag[i_gen],
						rup_max_mag[i_gen],
						rup_min_t_day[i_gen],
						rup_max_t_day[i_gen]
					);
					result.append (s + "\n");
				} else {
					String s = line_for_gen (
						i_gen,
						"C",
						gen_size[i_gen],
						gen_min_mag[i_gen],
						gen_max_mag[i_gen],
						rup_min_mag[i_gen],
						rup_max_mag[i_gen],
						rup_min_t_day[i_gen],
						rup_max_t_day[i_gen]
					);
					result.append (s + "\n");
					s = line_for_gen (
						i_gen,
						"V",
						gen_valid_size[i_gen],
						gen_min_mag[i_gen],
						gen_max_mag[i_gen],
						rup_valid_min_mag[i_gen],
						rup_valid_max_mag[i_gen],
						rup_valid_min_t_day[i_gen],
						rup_valid_max_t_day[i_gen]
					);
					result.append (s + "\n");
				}
			}

			return result.toString();
		}

	}




	//----- Accumulators -----

	// The current capacity of the accumulator.

	private int acc_capacity;

	// The size of the accumulator, that is, the number of catalogs accumulated.

	private int acc_size;

	// The accumulated catalog information structures.
	// Dimension: acc_cat_info[acc_capacity]

	private CatInfo[] acc_cat_info;

	// The number of catalogs with at least one valid direct aftershock.

	private int dval_count;

	// Sorted list of the valid sizes of catalogs with at least one valid direct aftershock, excluding seed ruptures.
	// Dimension: dval_cat_size[dval_count]

	private int[] dval_cat_valid_etas_size;

	// Sorted list of the valid number of direct aftershocks of catalogs with at least one valid direct aftershock.
	// Dimension: dval_direct_valid_size[dval_count]

	private int[] dval_direct_valid_size;

	// Sorted list of the effective branch ratio of catalogs with at least one valid direct aftershock.
	// Dimension: dval_cat_size[dval_count]

	private double[] dval_effective_br;


	// The next catalog index to use.

	private AtomicInteger acc_catix = new AtomicInteger();

	// Get the next catalog index to use.
	// Throw an exception if all capacity is exhausted.

	//  private int get_acc_catix () {
	//  	int n;
	//  	do {
	//  		n = acc_catix.get();
	//  		if (n >= acc_capacity) {
	//  			throw new IllegalStateException ("OEAccumGenInfo.get_acc_catix: No room in accumulator");
	//  		}
	//  	} while (!( acc_catix.compareAndSet (n, n+1) ));
	//  	return n;
	//  }

	private int get_acc_catix () {
		int n = acc_catix.getAndIncrement();
		return n;
	}




	//----- Construction -----




	// Erase the contents.

	public final void clear () {
		acc_capacity = 0;
		acc_size = 0;
		acc_catix.set(0);

		acc_cat_info = new CatInfo[0];
		dval_count = 0;
		dval_cat_valid_etas_size = new int[0];
		dval_direct_valid_size = new int[0];
		dval_effective_br = new double[0];

		return;
	}




	// Default constructor.

	public OEAccumGenInfo () {
		clear();
	}




	// Set up to begin accumulating.

	public void setup () {

		// Empty accumulators

		acc_capacity = 0;
		acc_size = 0;
		acc_catix.set(0);

		acc_cat_info = null;
		dval_count = 0;
		dval_cat_valid_etas_size = null;
		dval_direct_valid_size = null;
		dval_effective_br = null;

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

		// The catalog information structure.

		protected CatInfo cat_info;


		//----- Construction -----

		// Default constructor.

		protected ConsumerBase () {
			f_open = false;
			cat_info = null;
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

			// Allocate and initialize the catalog information

			cat_info = new CatInfo (comm.cat_stop_time, comm.f_early_stop, comm.cat_gen_count);

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

				// If this is a successful catalog ...

				if (comm.f_result_success) {

					// Get the index for this catalog

					int catix = get_acc_catix();

					// Save our catalog info

					acc_cat_info[catix] = cat_info;
					cat_info = null;
				}
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
			cat_info.end_generation (comm.i_gen, comm.f_final_gen, comm.gen_size, comm.gen_valid_size, comm.gen_info);
			return;
		}

		// Next rupture in the first (seed) generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_seed_rup (OECatalogScanComm comm) {
			cat_info.accum_rup (comm.i_gen, comm.rup, comm.f_valid_rup);
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
			cat_info.end_generation (comm.i_gen, comm.f_final_gen, comm.gen_size, comm.gen_valid_size, comm.gen_info);
			return;
		}

		// Next rupture in the current generation of a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog, per-generation, and per-rupture values set up.

		@Override
		public void next_rup (OECatalogScanComm comm) {
			cat_info.accum_rup (comm.i_gen, comm.rup, comm.f_valid_rup);
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
			acc_cat_info = new CatInfo[acc_capacity];
		}

		// Initialize the counters

		acc_size = 0;
		acc_catix.set(0);
		dval_count = 0;

		// Initialize the sorted accumulators for readout

		dval_cat_valid_etas_size = null;	// created during end_accumulation
		dval_direct_valid_size = null;		// created during end_accumulation
		dval_effective_br = null;			// created during end_accumulation

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
			acc_cat_info = Arrays.copyOf (acc_cat_info, acc_capacity);
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

		// Count the number of catalogs with at least one valid direct aftershock

		dval_count = 0;
		for (int i_cat = 0; i_cat < acc_size; ++i_cat) {
			if (acc_cat_info[i_cat].has_valid_direct()) {
				++dval_count;
			}
		}

		// Create the readout arrays

		dval_cat_valid_etas_size = new int[dval_count];
		dval_direct_valid_size = new int[dval_count];
		dval_effective_br = new double[dval_count];

		int j = 0;
		for (int i_cat = 0; i_cat < acc_size; ++i_cat) {
			if (acc_cat_info[i_cat].has_valid_direct()) {
				dval_cat_valid_etas_size[j] = acc_cat_info[i_cat].cat_valid_etas_size;
				dval_direct_valid_size[j] = acc_cat_info[i_cat].gen_valid_size[1];
				dval_effective_br[j] = acc_cat_info[i_cat].calc_effective_br();
				++j;
			}
		}

		Arrays.sort (dval_cat_valid_etas_size);
		Arrays.sort (dval_direct_valid_size);
		Arrays.sort (dval_effective_br);

		return;
	}




	//----- Readout functions -----




	// Get a fractile of the valid sizes of catalogs with at least one valid direct aftershock, excluding seed ruptures.

	public final int get_fractile_cat_valid_etas_size (double fractile) {
		return OEArraysCalc.fractile_array (dval_cat_valid_etas_size, fractile, 0, dval_count);
	}


	// Get a fractile of the valid number of direct aftershocks of catalogs with at least one valid direct aftershock.

	public final int get_fractile_direct_valid_size (double fractile) {
		return OEArraysCalc.fractile_array (dval_direct_valid_size, fractile, 0, dval_count);
	}


	// Get a fractile of the effective branch ratio of catalogs with at least one valid direct aftershock.

	public final double get_fractile_effective_br (double fractile) {
		return OEArraysCalc.fractile_array (dval_effective_br, fractile, 0, dval_count);
	}




	//----- Testing -----




	// Set up using typical values.

	public void typical_test_setup () {

		// Do the setup

		setup ();
		return;
	}




	// Create a typical set of test outputs, as a string.

	public String typical_test_outputs_to_string (int out_cats) {
		StringBuilder result = new StringBuilder();

		int[][] fractile_array;
		double[][] prob_occur_array;

		// Number of catalog processed

		result.append ("\n");
		result.append ("Catalogs = " + acc_size + "\n");
		result.append ("Catalogs with valid direct aftershock = " + dval_count + "\n");

		// Output the requested number of catalogs

		for (int i_cat = 0; i_cat < Math.min (out_cats, acc_size); ++i_cat) {
			result.append ("\n");
			result.append (i_cat + ": " + acc_cat_info[i_cat].toString());
		}

		// Valid sizes of catalogs with at least one valid direct aftershock, excluding seed ruptures

		result.append ("\n");
		result.append ("Valid catalog sizes: fractiles 0%, 5%, 10%, 25%, 50%, 75%, 90%, 95%, 100%\n");
		result.append (String.format ("%10d %10d %10d %10d %10d %10d %10d %10d %10d\n",
			get_fractile_cat_valid_etas_size (0.00),
			get_fractile_cat_valid_etas_size (0.05),
			get_fractile_cat_valid_etas_size (0.10),
			get_fractile_cat_valid_etas_size (0.25),
			get_fractile_cat_valid_etas_size (0.50),
			get_fractile_cat_valid_etas_size (0.75),
			get_fractile_cat_valid_etas_size (0.90),
			get_fractile_cat_valid_etas_size (0.95),
			get_fractile_cat_valid_etas_size (1.00)
		));

		// Valid number of direct aftershocks of catalogs with at least one valid direct aftershock

		result.append ("\n");
		result.append ("Valid direct aftershocks: fractiles 0%, 5%, 10%, 25%, 50%, 75%, 90%, 95%, 100%\n");
		result.append (String.format ("%10d %10d %10d %10d %10d %10d %10d %10d %10d\n",
			get_fractile_direct_valid_size (0.00),
			get_fractile_direct_valid_size (0.05),
			get_fractile_direct_valid_size (0.10),
			get_fractile_direct_valid_size (0.25),
			get_fractile_direct_valid_size (0.50),
			get_fractile_direct_valid_size (0.75),
			get_fractile_direct_valid_size (0.90),
			get_fractile_direct_valid_size (0.95),
			get_fractile_direct_valid_size (1.00)
		));

		// Effective branch ratio of catalogs with at least one valid direct aftershock

		result.append ("\n");
		result.append ("Effective branch ratio: fractiles 0%, 5%, 10%, 25%, 50%, 75%, 90%, 95%, 100%\n");
		result.append (String.format ("%10.6f %10.6f %10.6f %10.6f %10.6f %10.6f %10.6f %10.6f %10.6f\n",
			get_fractile_effective_br (0.00),
			get_fractile_effective_br (0.05),
			get_fractile_effective_br (0.10),
			get_fractile_effective_br (0.25),
			get_fractile_effective_br (0.50),
			get_fractile_effective_br (0.75),
			get_fractile_effective_br (0.90),
			get_fractile_effective_br (0.95),
			get_fractile_effective_br (1.00)
		));

		result.append ("\n");

		return result.toString();
	}




	// Perform a typical test run, multi-threaded version.
	// Parameters:
	//  initializer = Initializer for catalogs (typically OEInitFixedState).
	//  num_cats = Number of catalogs to run.
	//  out_cats = Number of catalogs to display in output.
	//  num_threads = Number of threads to use, can be -1 for default number of threads.
	//  max_runtime = Maximum running time allowed, can be -1L for no limit.
	//  mear_opt = 0 for normal random generation, 1 for early aftershock times, 2 to also suppress Poisson.
	// All catalogs use the same parameters, and are seeded with
	// a single earthquake.

	public static void typical_test_run_mt (OEEnsembleInitializer initializer, int num_cats, int out_cats, int num_threads, long max_runtime, int mear_opt) {

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

		OEAccumGenInfo gen_info_accum = new OEAccumGenInfo();
		gen_info_accum.typical_test_setup ();

		// Create the list of accumulators

		ArrayList<OEEnsembleAccumulator> accumulators = new ArrayList<OEEnsembleAccumulator>();
		accumulators.add (gen_info_accum);

		// Set up the ensemble parameters

		OEEnsembleParams ensemble_params = new OEEnsembleParams();

		ensemble_params.set (
			initializer,		// initializer
			accumulators,		// accumulators
			num_cats			// num_catalogs
		);

		// Create the ensemble generator

		OEEnsembleGenerator ensemble_generator = new OEEnsembleGenerator();

		ensemble_generator.set_mear_opt (mear_opt);

		// Generate the catalogs

		long progress_time = 10000L;
		ensemble_generator.generate_all_catalogs (ensemble_params, num_threads, max_runtime, progress_time);

		// Display results

		System.out.println (gen_info_accum.typical_test_outputs_to_string (out_cats));

		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEAccumGenInfo");




		// Subcommand : Test #1
		// Command format:
		//  test1  zmu  zams  n  p  c  b  alpha  f_relative_zams  mref  msup  tbegin  tend  tint_br  mag_min_sim  mag_max_sim
		//         mag_min_fit  mag_max_fit  gen_count_max  max_cat_size  num_cats  out_cats  num_threads  max_runtime  mear_opt  [t_day  rup_mag]...
		// Run multiple catalogs and display the results.
		// See OEInitFixedState.setup_time_mag_list_for_params for description of zmu through max_cat_size, and time_mag_array.
		// The value of gen_count_max can be 0 for no limit, -1 for OEConstants.DEF_MAX_GEN_COUNT (= 100).
		// The value of max_cat_size can be 0 for no limit, -1 for OEConstants.DEF_MAX_CAT_SIZE (= 5000000).
		// The value of num_threads can be -1 for default number of threads.
		// The value of max_runtime can be -1 for no limit.
		// The value of mear_opt is 0 normally, 1 for setting aftershock times to start of interval, 2 for also suppressing Poisson.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Generating catalogs and displaying catalog info");
			double zmu = testargs.get_double ("zmu");
			double zams = testargs.get_double ("zams");
			double n = testargs.get_double ("n");
			double p = testargs.get_double ("p");
			double c = testargs.get_double ("c");
			double b = testargs.get_double ("b");
			double alpha = testargs.get_double ("alpha");
			boolean f_relative_zams = testargs.get_boolean ("f_relative_zams");
			double mref = testargs.get_double ("mref");
			double msup = testargs.get_double ("msup");
			double tbegin = testargs.get_double ("tbegin");
			double tend = testargs.get_double ("tend");
			double tint_br = testargs.get_double ("tint_br");
			double mag_min_sim = testargs.get_double ("mag_min_sim");
			double mag_max_sim = testargs.get_double ("mag_max_sim");
			double mag_min_fit = testargs.get_double ("mag_min_fit");
			double mag_max_fit = testargs.get_double ("mag_max_fit");
			int gen_count_max = testargs.get_int ("gen_count_max");
			int max_cat_size = testargs.get_int ("max_cat_size");

			int num_cats = testargs.get_int ("num_cats");
			int out_cats = testargs.get_int ("out_cats");
			int num_threads = testargs.get_int ("num_threads");
			long max_runtime = testargs.get_long ("max_runtime");
			int mear_opt = testargs.get_int ("mear_opt");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
			testargs.end_test();

			// Create the initializer

			boolean f_verbose = true;

			OEInitFixedState initializer = new OEInitFixedState();
			initializer.setup_time_mag_list_for_params (
				zmu,
				zams,
				n,
				p,
				c,
				b,
				alpha,
				f_relative_zams,
				mref,
				msup,
				tbegin,
				tend,
				tint_br,
				mag_min_sim,
				mag_max_sim,
				mag_min_fit,
				mag_max_fit,
				gen_count_max,
				max_cat_size,
				time_mag_array,
				f_verbose
			);

			// Run the test

			typical_test_run_mt (initializer, num_cats, out_cats, num_threads, max_runtime, mear_opt);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}

}
