package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SimpleExecTimer;
import static org.opensha.oaf.util.SimpleUtils.rndd;

import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OECatalogSeedComm;
import org.opensha.oaf.oetas.OECatalogBuilder;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESeedParams;
import org.opensha.oaf.oetas.OERandomGenerator;
import org.opensha.oaf.oetas.OECatalogStorage;
import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OECatalogSeeder;
import org.opensha.oaf.oetas.OECatalogRange;
import org.opensha.oaf.oetas.OECatalogLimits;
import org.opensha.oaf.oetas.OEGenerationInfo;

import org.opensha.oaf.oetas.util.OEArraysCalc;

import org.opensha.oaf.oetas.except.OEException;


// Operational ETAS catalog initializer for fitted parameters.
// Author: Michael Barall 03/09/2023.

public class OEDisc2InitVoxSet implements OEEnsembleInitializer, OEDisc2InitVoxConsumer, Marshalable {


	//----- Implementation of OEDisc2InitVoxConsumer -----



	// The fitting information.

	private OEDisc2InitFitInfo fit_info;

	// The b-vallue to use for scaling catalog size, as supplied thru the consumer interface, or OEConstants.UNKNOWN_B_VALUE == -1.0 if unknown.

	private double b_scaling;

	// Number of voxels (guaranteed to be at least 1).

	private int voxel_count;

	// The statistics voxels, as an array; length = voxel_count.

	private OEDisc2InitStatVox[] a_voxel_list;

	// Temporary list, used for accumulating voxels.

	private ArrayList<OEDisc2InitStatVox> temp_voxel_list;




	// Begin consuming voxels.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  b_scaling = The b-value to use for scaling catalog size, or OEConstants.UNKNOWN_B_VALUE == -1.0 if unknown.
	// Note: This function retains the fit_info object, so the caller must
	// not modify it after the function returns.
	// Threading: This function should be called by a single thread,
	// before any calls to add_voxel.

	@Override
	public void begin_voxel_consume (OEDisc2InitFitInfo fit_info, double b_scaling) {
		//if (b_scaling < OEConstants.UNKNOWN_B_VALUE_CHECK) {
		//	throw new IllegalArgumentException ("OEDisc2InitVoxSet.begin_voxel_consume: Negative b_scaling not supported: b_scaling = " + b_scaling);
		//}
		synchronized (this) {
			this.fit_info = fit_info;
			this.b_scaling = b_scaling;
			this.a_voxel_list = null;
			this.temp_voxel_list = new ArrayList<OEDisc2InitStatVox>();
		}
		return;
	}




	// End consuming voxels.
	// Threading: This function should be called by a single thread,
	// after all calls to add_voxel have returned.

	@Override
	public void end_voxel_consume () {
		synchronized (this) {
			voxel_count = temp_voxel_list.size();
			if (voxel_count == 0) {
				throw new IllegalArgumentException ("OEDisc2InitVoxSet.end_voxel_consume: No voxels supplied");
			}
			if (voxel_count > OEConstants.MAX_STATISTICS_GRID) {
				throw new IllegalArgumentException ("OEDisc2InitVoxSet.end_voxel_consume: Too many voxels supplied - voxel_count = " + voxel_count);
			}
			a_voxel_list = temp_voxel_list.toArray (new OEDisc2InitStatVox[0]);
			temp_voxel_list = null;
		}

		// Sort voxels into canonical order

		Arrays.sort (a_voxel_list, new OEDisc2InitStatVoxComparator());

		// Check the sort, specifically to check for duplicate voxels

		for (int index = 1; index < voxel_count; ++index) {
			int cmp = a_voxel_list[index - 1].compareTo (a_voxel_list[index]);
			if (!( cmp < 0 )) {
				throw new InvariantViolationException (
					"OEDisc2InitVoxSet.end_voxel_consume: Duplicate voxel or sort error: cmp = "
					+ cmp
					+ ", index = "
					+ index
					+ "\n"
					+ "voxel1 = "
					+ a_voxel_list[index - 1].summary_string()
					+ "voxel2 = "
					+ a_voxel_list[index].summary_string()
				);
			}
		}

		return;
	}




	// Add a list of voxels to the set of voxels.
	// Parameters:
	//  voxels = List of voxels to add to the set.
	// Threading: Can be called simulataneously by multiple threads, so the
	// implementation must synchronize appropriately.  To minimize synchronization
	// cost, instead of supplying voxels one at a time, the caller should supply
	// them in groups, perhaps as large as all voxels created by the caller's thread.

	@Override
	public void add_voxels (Collection<OEDisc2InitStatVox> voxels){
		synchronized (this) {
			temp_voxel_list.addAll (voxels);
		}
		return;
	}




	//----- Post-fitting -----




	//--- Input parameters

	// Original parameters for the catalog, from when the initializer was set up.

	private OECatalogParams original_cat_params;

	// Bayesian prior weight (1 = Bayesian, 0 = Sequence-specific, see OEConstants.BAY_WT_XXX).

	private double bay_weight;

	// The number of selected sub-voxels for seeding.

	private int seed_subvox_count;

	// The time at which the forecast begins, in days.  (Should be >= original_cat_params.tbegin.)

	private double t_forecast;


	//--- Derived data

	// Prototype parameters for the catalog.
	// This contains the current range, but not the statistical parameters (which are done locally in each thread).

	private OECatalogParams proto_cat_params;

	// The total number of sub-voxels.

	private int total_subvox_count;

	// Cumulative number of sub-voxels for each voxel; length = voxel_count + 1.
	// cum_subvox_count[i] is the total number of sub-voxels in all voxels prior to i.

	private int[] cum_subvox_count;

	// The maximum log-density over all sub-voxels.

	private double max_log_density;

	// The voxel and sub-voxel indexes where the maximum log-density occurs.
	// Note: This is the location of the MLE parameter values.

	private int mle_voxel_index;
	private int mle_subvox_index;

	// The MLE grid point.

	private OEGridPoint mle_grid_point;

	// Maximum log-density, voxel and sub-voxel indexes where the maximum occurs, and MLE grid point for the generic model.

	private double gen_max_log_density;
	private int gen_mle_voxel_index;
	private int gen_mle_subvox_index;
	private OEGridPoint gen_mle_grid_point;

	// Maximum log-density, voxel and sub-voxel indexes where the maximum occurs, and MLE grid point for the sequence-specific model.

	private double seq_max_log_density;
	private int seq_mle_voxel_index;
	private int seq_mle_subvox_index;
	private OEGridPoint seq_mle_grid_point;

	// Maximum log-density, voxel and sub-voxel indexes where the maximum occurs, and MLE grid point for the Bayesian model.

	private double bay_max_log_density;
	private int bay_mle_voxel_index;
	private int bay_mle_subvox_index;
	private OEGridPoint bay_mle_grid_point;

	// The sub-voxels for seeding; length = seed_subvox_count.

	private int[] a_seed_subvox;


	//--- Output statistics

	// Dithering mismatch, ideally should be zero.

	private int dither_mismatch;

	// Probability and tally of sub-voxels passing first clip, removing small log-density.

	private double clip_log_density_prob;
	private int clip_log_density_tally;

	// Probability and tally of sub-voxels passing second clip, removing tail of probability distribution.

	private double clip_tail_prob;
	private int clip_tail_tally;

	// Probability and tally of sub-voxels used for seeding.

	private double clip_seed_prob;
	private int clip_seed_tally;

	// Average b-value for sub-voxels used for seeding.

	private double seed_b_value;

	// The b-value to use for ranging.

	private double ranging_b_value;




	// Find the voxel index for a given global sub-voxel index.
	// Parameters:
	//  global_subvox_index = Global sub-voxel index, 0 thru total_subvox_count-1.

	private int get_voxel_for_subvox (int global_subvox_index) {
		return OEArraysCalc.bsearch_array (cum_subvox_count, global_subvox_index) - 1;
	}




	// Find the local sub-voxel index (index within the voxel) for a given global sub-voxel index.
	// Parameters:
	//  global_subvox_index = Global sub-voxel index, 0 thru total_subvox_count-1.
	//  voxel_index = Voxel index, 0 thru voxel_count-1, can be obtained from get_voxel_for_subvox().

	private int get_local_for_global_subvox (int global_subvox_index, int voxel_index) {
		return global_subvox_index - cum_subvox_count[voxel_index];
	}




	// Get the fitting information.
	// Note: Can be called any time after voxels are constructed, even before setup_post_fitting.
	// The caller should not modify the returned object.

	public final OEDisc2InitFitInfo get_fit_info () {
		return fit_info;
	}




	// Set up for initialization, post fitting.
	// Parameters:
	//  the_cat_params = Catalog parameters to use.
	//  the_t_forecast = The time at which the forecast begins, in days.  (Should be >= the_cat_params.tbegin.)
	//  the_bay_weight = Bayesian prior weight (1 = Bayesian, 0 = Sequence-specific, 2 = Generic, see OEConstants.BAY_WT_XXX).
	//  density_bin_size_lnu = Size of each bin for binning sub-voxels according to log-density, in natural log units.
	//  density_bin_count = Number of bins for binning sub-voxels according to log-density; must be >= 2.
	//  prob_tail_trim = Fraction of the probability distribution to trim.
	//  the_seed_subvox_count = Number of sub-voxels to use for seeding, must be a power of 2.
	//  stat_accum = Statistics accumulator.
	// Note: The i-th density bin contains sub-voxels whose negative normalized log-density lies between
	// i*density_bin_size_lnu and (i+1)*density_bin_size_lnu.  The last bin contains all sub-voxels whose negative
	// normalized log-density is greater than (density_bin_count-1)*density_bin_size_lnu.  The density bins are
	// used for trimming the tail of the probability distribution.

	public final void setup_post_fitting (
		OECatalogParams the_cat_params,
		double the_t_forecast,
		double the_bay_weight,
		double density_bin_size_lnu,
		int density_bin_count,
		double prob_tail_trim,
		int the_seed_subvox_count,
		OEDisc2VoxStatAccum stat_accum
	) {

		// Save the parameters

		proto_cat_params = (new OECatalogParams()).copy_from (the_cat_params);
		original_cat_params = (new OECatalogParams()).copy_from (the_cat_params);
		bay_weight = the_bay_weight;
		seed_subvox_count = the_seed_subvox_count;
		t_forecast = the_t_forecast;

		// First scan, find total sub-voxel count and maximum log-density

		cum_subvox_count = new int[voxel_count + 1];

		cum_subvox_count[0] = 0;
		total_subvox_count = a_voxel_list[0].get_subvox_count();

		mle_voxel_index = 0;
		mle_subvox_index = a_voxel_list[0].get_max_subvox_index_log_density (bay_weight);
		max_log_density = a_voxel_list[0].get_subvox_log_density (mle_subvox_index, bay_weight);

		gen_mle_voxel_index = 0;
		gen_mle_subvox_index = a_voxel_list[0].get_max_subvox_index_log_density (OEConstants.BAY_WT_GENERIC);
		gen_max_log_density = a_voxel_list[0].get_subvox_log_density (gen_mle_subvox_index, OEConstants.BAY_WT_GENERIC);

		seq_mle_voxel_index = 0;
		seq_mle_subvox_index = a_voxel_list[0].get_max_subvox_index_log_density (OEConstants.BAY_WT_SEQ_SPEC);
		seq_max_log_density = a_voxel_list[0].get_subvox_log_density (seq_mle_subvox_index, OEConstants.BAY_WT_SEQ_SPEC);

		bay_mle_voxel_index = 0;
		bay_mle_subvox_index = a_voxel_list[0].get_max_subvox_index_log_density (OEConstants.BAY_WT_BAYESIAN);
		bay_max_log_density = a_voxel_list[0].get_subvox_log_density (bay_mle_subvox_index, OEConstants.BAY_WT_BAYESIAN);

		for (int j = 1; j < voxel_count; ++j) {
			final OEDisc2InitStatVox voxel = a_voxel_list[j];
			cum_subvox_count[j] = total_subvox_count;
			total_subvox_count += voxel.get_subvox_count();

			int k = voxel.get_max_subvox_index_log_density (bay_weight);
			double x = voxel.get_subvox_log_density (k, bay_weight);
			if (max_log_density < x) {
				mle_voxel_index = j;
				mle_subvox_index = k;
				max_log_density = x;
			}

			k = voxel.get_max_subvox_index_log_density (OEConstants.BAY_WT_GENERIC);
			x = voxel.get_subvox_log_density (k, OEConstants.BAY_WT_GENERIC);
			if (gen_max_log_density < x) {
				gen_mle_voxel_index = j;
				gen_mle_subvox_index = k;
				gen_max_log_density = x;
			}

			k = voxel.get_max_subvox_index_log_density (OEConstants.BAY_WT_SEQ_SPEC);
			x = voxel.get_subvox_log_density (k, OEConstants.BAY_WT_SEQ_SPEC);
			if (seq_max_log_density < x) {
				seq_mle_voxel_index = j;
				seq_mle_subvox_index = k;
				seq_max_log_density = x;
			}

			k = voxel.get_max_subvox_index_log_density (OEConstants.BAY_WT_BAYESIAN);
			x = voxel.get_subvox_log_density (k, OEConstants.BAY_WT_BAYESIAN);
			if (bay_max_log_density < x) {
				bay_mle_voxel_index = j;
				bay_mle_subvox_index = k;
				bay_max_log_density = x;
			}
		}

		cum_subvox_count[voxel_count] = total_subvox_count;

		mle_grid_point = a_voxel_list[mle_voxel_index].get_subvox_grid_point (mle_subvox_index, new OEGridPoint());

		gen_mle_grid_point = a_voxel_list[gen_mle_voxel_index].get_subvox_grid_point (gen_mle_subvox_index, new OEGridPoint());
		seq_mle_grid_point = a_voxel_list[seq_mle_voxel_index].get_subvox_grid_point (seq_mle_subvox_index, new OEGridPoint());
		bay_mle_grid_point = a_voxel_list[bay_mle_voxel_index].get_subvox_grid_point (bay_mle_subvox_index, new OEGridPoint());

		// Array to hold the probability of each sub-voxel

		double[] a_subvox_prob = new double[total_subvox_count];

		// Array to hold the density bin index of each sub-voxel

		int[] a_density_bin = new int[total_subvox_count];

		// Array to hold total probability for the sub-voxels in each bin, later cumulated, initialized to zero

		double[] a_prob_accum = new double[density_bin_count];
		OEArraysCalc.zero_array (a_prob_accum);

		// Array to hold the number of sub-voxels in each bin, later cumulated, initialized to zero

		int[] a_tally_accum = new int[density_bin_count];
		OEArraysCalc.zero_array (a_tally_accum);

		// Prepare the statistics accumulator

		stat_accum.vsaccum_begin (
			bay_weight,
			max_log_density,
			gen_max_log_density,
			seq_max_log_density,
			bay_max_log_density
		);

		// Second scan, get the probabilities and density bins

		for (int j = 0; j < voxel_count; ++j) {
			final OEDisc2InitStatVox voxel = a_voxel_list[j];
			voxel.get_probabilities_and_bin (
				bay_weight,
				max_log_density,
				a_subvox_prob,
				a_density_bin,
				cum_subvox_count[j],	// dest_index
				density_bin_size_lnu,	// bin_size_lnu
				a_prob_accum,
				a_tally_accum,
				stat_accum
			);
		}

		// Finish the statistics accumulator

		stat_accum.vsaccum_end();

		// Cumulate the probabilities and tallies in the bins

		OEArraysCalc.cumulate_array (a_prob_accum, true);
		OEArraysCalc.cumulate_array (a_tally_accum, true);

		if (!( a_tally_accum[density_bin_count - 1] == total_subvox_count )) {
			throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Sub-voxel tally mismatch: total_subvox_count = " + total_subvox_count + ", a_tally_accum[density_bin_count - 1] = " + a_tally_accum[density_bin_count - 1]);
		}

		// Clip the tail as a fraction of the cumulative probability in the next-to-last bin (always clip the last bin)
		// (clip_bin_index is the first bin excluded, and is guaranteed to be >= 1)

		final double clip_prob = a_prob_accum[density_bin_count - 2] * (1.0 - prob_tail_trim);
		final int clip_bin_index = OEArraysCalc.bsearch_array (a_prob_accum, clip_prob, 1, density_bin_count - 1);

		// Initialize the clipping statistics

		clip_log_density_prob = a_prob_accum[density_bin_count - 2] / a_prob_accum[density_bin_count - 1];
		clip_log_density_tally = a_tally_accum[density_bin_count - 2];

		clip_tail_prob = a_prob_accum[clip_bin_index - 1] / a_prob_accum[density_bin_count - 1];
		clip_tail_tally = a_tally_accum[clip_bin_index - 1];

		clip_seed_prob = 0.0;
		clip_seed_tally = 0;

		seed_b_value = 0.0;
		ranging_b_value = 0.0;

		// Array of scrambled indexes for seed sub-voxels

		final int[] a_scramble = OEArraysCalc.make_bit_rev_array (seed_subvox_count);

		// Allocate array for seed sub-voxels

		a_seed_subvox = new int[seed_subvox_count];
		OEArraysCalc.fill_array (a_seed_subvox, -1);	// allows detection of unfilled elements

		// Dither probabilities to select the seed sub-voxels

		final double dither_step = a_prob_accum[clip_bin_index - 1] / ((double)seed_subvox_count);	// (total probability) / (nummber of seeds)
		double dither_accum = -0.5 * dither_step;

		int ix_seed = 0;

		int ix_voxel = 0;
		double b_voxel = a_voxel_list[ix_voxel].get_b_value();

		int tol = seed_subvox_count / 32;		// tolerance for dither mismatch

		for (int ix_subvox = 0; ix_subvox < total_subvox_count; ++ix_subvox) {

			// If end of voxel, advance to the next Voxel

			if (ix_subvox == cum_subvox_count[ix_voxel + 1]) {
				++ix_voxel;
				b_voxel = a_voxel_list[ix_voxel].get_b_value();
			}

			// If this sub-voxel passes the probability tail clip...

			if (a_density_bin[ix_subvox] < clip_bin_index) {

				// Add the sub-voxel probability to the dither

				dither_accum += a_subvox_prob[ix_subvox];

				// If this sub-voxel is contributing to the seeding...

				if (dither_accum >= 0.0) {

					// Accumulate it for statistics

					clip_seed_prob += a_subvox_prob[ix_subvox];
					++clip_seed_tally;

					// Add it to seeds as many times as needed

					do {

						// Use this sub-voxel

						if (ix_seed < seed_subvox_count) {
							if (a_seed_subvox[a_scramble[ix_seed]] != -1) {
								throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Seeding array collision: ix_seed = " + ix_seed + ", ix_subvox = " + ix_subvox + ", a_scramble[ix_seed] = " + a_scramble[ix_seed] + ", a_seed_subvox[a_scramble[ix_seed]] = " + a_seed_subvox[a_scramble[ix_seed]]);
							}

							a_seed_subvox[a_scramble[ix_seed]] = ix_subvox;
							seed_b_value += b_voxel;
						}
						else if (ix_seed > seed_subvox_count + tol) {
							throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Dither mismatch: seed_subvox_count = " + seed_subvox_count + ", ix_seed = " + ix_seed);
						}
						++ix_seed;

						// Advance the dither

						dither_accum -= dither_step;

					} while (dither_accum >= 0.0);
				}
			}
		}

		// Finish the clipping statistics

		clip_seed_prob /= a_prob_accum[density_bin_count - 1];

		dither_mismatch = ix_seed - seed_subvox_count;

		// The dither mismatch should be zero, but we permit some sloppiness

		if (dither_mismatch != 0) {

			// Check for mismatch exceeding tolerance

			if (dither_mismatch > tol || dither_mismatch < -tol) {
				throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Dither mismatch: seed_subvox_count = " + seed_subvox_count + ", final ix_seed = " + ix_seed);
			}

			// If we didn't fill all the seeds, pad using elements from the middle

			int mid = seed_subvox_count / 2;
			while (ix_seed < seed_subvox_count) {

				if (a_seed_subvox[a_scramble[ix_seed]] != -1) {
					throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Seeding array collision: ix_seed = " + ix_seed + ", a_scramble[ix_seed] = " + a_scramble[ix_seed] + ", a_seed_subvox[a_scramble[ix_seed]] = " + a_seed_subvox[a_scramble[ix_seed]]);
				}

				final int ix_subvox = a_seed_subvox[a_scramble[ix_seed - mid]];
				if (ix_subvox == -1) {
					throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Seeding array entry empty: ix_seed - mid = " + (ix_seed - mid) + ", a_scramble[ix_seed - mid] = " + a_scramble[ix_seed - mid] + ", a_seed_subvox[a_scramble[ix_seed - mid]] = " + a_seed_subvox[a_scramble[ix_seed - mid]]);
				}

				a_seed_subvox[a_scramble[ix_seed]] = ix_subvox;
				++ix_seed;
				seed_b_value += a_voxel_list[get_voxel_for_subvox (ix_subvox)].get_b_value();
			}
		}

		// Finish the b-value computation, and use it for scaling if needed

		seed_b_value /= ((double)seed_subvox_count);

		if (b_scaling < OEConstants.UNKNOWN_B_VALUE_CHECK) {
			ranging_b_value = seed_b_value;
		} else {
			ranging_b_value = b_scaling;
		}

		// Here there could be computation of parameter statistics...

		return;
	}




	// Get the MLE grid point.
	// The returned OEGridPoint is newly-allocated and not retained in this object.
	// Can only be called after setup_post_fitting has been called.

	public final OEGridPoint get_mle_grid_point () {
		return (new OEGridPoint()).copy_from (mle_grid_point);
	}




	// Get the generic MLE grid point.
	// The returned OEGridPoint is newly-allocated and not retained in this object.
	// Can only be called after setup_post_fitting has been called.

	public final OEGridPoint get_gen_mle_grid_point () {
		return (new OEGridPoint()).copy_from (gen_mle_grid_point);
	}




	// Get the sequence-specific MLE grid point.
	// The returned OEGridPoint is newly-allocated and not retained in this object.
	// Can only be called after setup_post_fitting has been called.

	public final OEGridPoint get_seq_mle_grid_point () {
		return (new OEGridPoint()).copy_from (seq_mle_grid_point);
	}




	// Get the Bayesian MLE grid point.
	// The returned OEGridPoint is newly-allocated and not retained in this object.
	// Can only be called after setup_post_fitting has been called.

	public final OEGridPoint get_bay_mle_grid_point () {
		return (new OEGridPoint()).copy_from (bay_mle_grid_point);
	}




	// Get the Bayesian prior weight.

	public final double get_bay_weight () {
		return bay_weight;
	}




	// Get the parameters to calculate the integrated intensity function, for the MLE.

	public final void get_intensity_calc_params (OEDisc2IntensityCalc intensity_calc) {
		a_voxel_list[mle_voxel_index].get_intensity_calc_params (fit_info, mle_subvox_index, intensity_calc);
		return;
	}




	// Get the parameters to calculate the integrated intensity function, for the genericMLE.

	public final void get_gen_intensity_calc_params (OEDisc2IntensityCalc intensity_calc) {
		a_voxel_list[gen_mle_voxel_index].get_intensity_calc_params (fit_info, gen_mle_subvox_index, intensity_calc);
		return;
	}




	// Get the parameters to calculate the integrated intensity function, for the sequence-specific MLE.

	public final void get_seq_intensity_calc_params (OEDisc2IntensityCalc intensity_calc) {
		a_voxel_list[seq_mle_voxel_index].get_intensity_calc_params (fit_info, seq_mle_subvox_index, intensity_calc);
		return;
	}




	// Get the parameters to calculate the integrated intensity function, for the Bayesian MLE.

	public final void get_bay_intensity_calc_params (OEDisc2IntensityCalc intensity_calc) {
		a_voxel_list[bay_mle_voxel_index].get_intensity_calc_params (fit_info, bay_mle_subvox_index, intensity_calc);
		return;
	}




	//----- Construction -----




	// Erase the consume data.

	private final void clear_consumer () {
		fit_info = null;
		b_scaling = 0.0;
		voxel_count = 0;
		a_voxel_list = null;
		temp_voxel_list = null;
		return;
	}




	// Erse the post-fitting data.

	public final void clear_post_fitting () {
		original_cat_params = null;
		bay_weight = 0.0;
		seed_subvox_count = 0;
		t_forecast = 0.0;

		proto_cat_params = null;
		total_subvox_count = 0;
		cum_subvox_count = null;
		max_log_density = 0.0;
		mle_voxel_index = 0;
		mle_subvox_index = 0;
		mle_grid_point = null;
		gen_max_log_density = 0.0;
		gen_mle_voxel_index = 0;
		gen_mle_subvox_index = 0;
		gen_mle_grid_point = null;
		seq_max_log_density = 0.0;
		seq_mle_voxel_index = 0;
		seq_mle_subvox_index = 0;
		seq_mle_grid_point = null;
		bay_max_log_density = 0.0;
		bay_mle_voxel_index = 0;
		bay_mle_subvox_index = 0;
		bay_mle_grid_point = null;
		a_seed_subvox = null;

		dither_mismatch = 0;
		clip_log_density_prob = 0.0;
		clip_log_density_tally = 0;
		clip_tail_prob = 0.0;
		clip_tail_tally = 0;
		clip_seed_prob = 0.0;
		clip_seed_tally = 0;
		seed_b_value = 0.0;
		ranging_b_value = 0.0;
		return;
	}




	// Erase the contents.

	public final void clear () {
		clear_consumer();
		clear_post_fitting();
		return;
	}




	// Default constructor.

	public OEDisc2InitVoxSet () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2InitVoxSet:" + "\n");

		if (fit_info != null) {
			result.append ("fit_info = {" + fit_info.toString() + "}\n");
		}
		result.append ("b_scaling = " + b_scaling + "\n");
		result.append ("voxel_count = " + voxel_count + "\n");
		if (a_voxel_list != null) {
			result.append ("a_voxel_list.length = " + a_voxel_list.length + "\n");
		}
		if (original_cat_params != null) {
			result.append ("original_cat_params = {" + original_cat_params.toString() + "}\n");
		}
		result.append ("bay_weight = " + bay_weight + "\n");
		result.append ("seed_subvox_count = " + seed_subvox_count + "\n");
		result.append ("t_forecast = " + t_forecast + "\n");
		if (proto_cat_params != null) {
			result.append ("proto_cat_params = {" + proto_cat_params.toString() + "}\n");
		}
		result.append ("total_subvox_count = " + total_subvox_count + "\n");
		if (cum_subvox_count != null) {
			result.append ("cum_subvox_count.length = " + cum_subvox_count.length + "\n");
			if (cum_subvox_count.length >= 2) {
				result.append ("cum_subvox_count[0] = " + cum_subvox_count[0] + "\n");
				result.append ("cum_subvox_count[1] = " + cum_subvox_count[1] + "\n");
				result.append ("cum_subvox_count[" + (cum_subvox_count.length - 1) + "] = " + cum_subvox_count[cum_subvox_count.length - 1] + "\n");
			}
		}
		result.append ("max_log_density = " + max_log_density + "\n");
		result.append ("mle_voxel_index = " + mle_voxel_index + "\n");
		result.append ("mle_subvox_index = " + mle_subvox_index + "\n");
		if (mle_grid_point != null) {
			result.append ("mle_grid_point = {" + mle_grid_point.toString() + "}\n");
		}
		result.append ("gen_max_log_density = " + gen_max_log_density + "\n");
		result.append ("gen_mle_voxel_index = " + gen_mle_voxel_index + "\n");
		result.append ("gen_mle_subvox_index = " + gen_mle_subvox_index + "\n");
		if (gen_mle_grid_point != null) {
			result.append ("gen_mle_grid_point = {" + gen_mle_grid_point.toString() + "}\n");
		}
		result.append ("seq_max_log_density = " + seq_max_log_density + "\n");
		result.append ("seq_mle_voxel_index = " + seq_mle_voxel_index + "\n");
		result.append ("seq_mle_subvox_index = " + seq_mle_subvox_index + "\n");
		if (seq_mle_grid_point != null) {
			result.append ("seq_mle_grid_point = {" + seq_mle_grid_point.toString() + "}\n");
		}
		result.append ("bay_max_log_density = " + bay_max_log_density + "\n");
		result.append ("bay_mle_voxel_index = " + bay_mle_voxel_index + "\n");
		result.append ("bay_mle_subvox_index = " + bay_mle_subvox_index + "\n");
		if (bay_mle_grid_point != null) {
			result.append ("bay_mle_grid_point = {" + bay_mle_grid_point.toString() + "}\n");
		}
		if (a_seed_subvox != null) {
			result.append ("a_seed_subvox.length = " + a_seed_subvox.length + "\n");
			if (a_seed_subvox.length >= 4) {
				result.append ("a_seed_subvox[0] = " + a_seed_subvox[0] + "\n");
				result.append ("a_seed_subvox[1] = " + a_seed_subvox[1] + "\n");
				result.append ("a_seed_subvox[2] = " + a_seed_subvox[2] + "\n");
				result.append ("a_seed_subvox[3] = " + a_seed_subvox[3] + "\n");
			}
		}
		result.append ("dither_mismatch = " + dither_mismatch + "\n");
		result.append ("clip_log_density_prob = " + clip_log_density_prob + "\n");
		result.append ("clip_log_density_tally = " + clip_log_density_tally + "\n");
		result.append ("clip_tail_prob = " + clip_tail_prob + "\n");
		result.append ("clip_tail_tally = " + clip_tail_tally + "\n");
		result.append ("clip_seed_prob = " + clip_seed_prob + "\n");
		result.append ("clip_seed_tally = " + clip_seed_tally + "\n");
		result.append ("seed_b_value = " + seed_b_value + "\n");
		result.append ("ranging_b_value = " + ranging_b_value + "\n");

		return result.toString();
	}




	// Dump the entire log-density grid to a file.
	// Parameters:
	//  filename = File name.
	// Throws exception if I/O error.
	// Each line contains the following:
	//   b  alpha  c  p  n  zams  zmu  bay_log_density  bay_vox_volume  log_likelihood

	public final void dump_log_density_to_file (String filename) throws IOException {
		try (
			BufferedWriter buf = new BufferedWriter (new FileWriter (filename));
		) {
			for (int j = 0; j < voxel_count; ++j) {
				buf.write (a_voxel_list[j].dump_log_density_to_string (new StringBuilder()).toString());
			}
		}
		return;
	}




	// Write the integrated intensity data to a string.
	// Parmaeters:
	//  max_frac_duration = Maximum interval duration, as a fraction of the total duration, or zero if no maximum.
	//  history = The rupture history.
	//  exec_timer = Execution timer for multi-threading, can be null to use single-threading.
	// Throws exception if multi-threading error.
	// Note: The fitting information must have f_intensity set, indicating that fitting saved the necessary information.

	public final String write_integrated_intensity_to_string (
		double max_frac_duration,
		OEDisc2History history,
		SimpleExecTimer exec_timer
	) throws OEException {

		// Create and set configuration for MLE

		OEDisc2IntensityCalc intensity_calc = (new OEDisc2IntensityCalc()).set_config (
			max_frac_duration,
			history,
			fit_info
		);

		// Create and set configuration for generic, Sequence-specific, and Bayesian MLE

		OEDisc2IntensityCalc gen_intensity_calc = (new OEDisc2IntensityCalc()).copy_config_from (intensity_calc);
		OEDisc2IntensityCalc seq_intensity_calc = (new OEDisc2IntensityCalc()).copy_config_from (intensity_calc);
		OEDisc2IntensityCalc bay_intensity_calc = (new OEDisc2IntensityCalc()).copy_config_from (intensity_calc);

		// Set parameters

		get_intensity_calc_params (intensity_calc);
		get_gen_intensity_calc_params (gen_intensity_calc);
		get_seq_intensity_calc_params (seq_intensity_calc);
		get_bay_intensity_calc_params (bay_intensity_calc);

		// Multi-threaded calculation

		if (exec_timer != null) {
			intensity_calc.calc_integrated_lambda_mt (exec_timer);
			gen_intensity_calc.calc_integrated_lambda_mt (exec_timer);
			seq_intensity_calc.calc_integrated_lambda_mt (exec_timer);
			bay_intensity_calc.calc_integrated_lambda_mt (exec_timer);
		}

		// Single-threaded calculation

		else {
			intensity_calc.calc_integrated_lambda_st();
			gen_intensity_calc.calc_integrated_lambda_st();
			seq_intensity_calc.calc_integrated_lambda_st();
			bay_intensity_calc.calc_integrated_lambda_st();
		}

		// Produce the file as a string

		String result = intensity_calc.output_file_as_string (gen_intensity_calc, seq_intensity_calc, bay_intensity_calc);

		return result;
	}




	// Write the integrated intensity data to a file.
	// Parmaeters:
	//  filename = File name.
	//  max_frac_duration = Maximum interval duration, as a fraction of the total duration, or zero if no maximum.
	//  history = The rupture history.
	//  exec_timer = Execution timer for multi-threading, can be null to use single-threading.
	// Throws exception if multi-threading error or I/O error.
	// Note: The fitting information must have f_intensity set, indicating that fitting saved the necessary information.

	public final void write_integrated_intensity_to_file (
		String filename,
		double max_frac_duration,
		OEDisc2History history,
		SimpleExecTimer exec_timer
	) throws OEException, IOException {

		String result = write_integrated_intensity_to_string (
			max_frac_duration,
			history,
			exec_timer
		);

		SimpleUtils.write_string_as_file (filename, result);

		return;
	}




	//----- Seeders -----




	// The current index into the seeding array.

	private final AtomicInteger seeding_index = new AtomicInteger();




	// Seeder for voxel set.

	private class SeederVoxSet implements OECatalogSeeder {

		//----- Control -----

		// True if seeder is open.

		private boolean f_open;


		//----- Per-thread data structures -----

		private final OECatalogParams local_cat_params = new OECatalogParams();
		private final OERupture local_rup = new OERupture();


		//----- Construction -----

		// Default constructor.

		public SeederVoxSet () {
			f_open = false;
		}

		//----- Open/Close methods (Implementation of OECatalogSeeder) -----

		// Open the catalog seeder.
		// Perform any setup needed.

		@Override
		public void open () {

			// Clear the local data sttructures

			local_cat_params.clear();
			local_rup.clear();

			// Mark it open

			f_open = true;
			return;
		}

		// Close the catalog seeder.
		// Perform any final tasks needed.

		@Override
		public void close () {

			// If open ...

			if (f_open) {
			
				// Mark it closed

				f_open = false;
			}
			return;
		}


		//----- Data methods (Implementation of OECatalogSeeder) -----

		// Seed a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog values set up.
		// This function should perform the following steps:
		// 1. Construct a OECatalogParams object containing the catalog parameters.
		// 2. Call comm.cat_builder.begin_catalog() to begin constructing the catalog.
		// 3. Construct a OEGenerationInfo object containing info for the first (seed) generation.
		// 4. Call comm.cat_builder.begin_generation() to begin the first (seed) generation.
		// 5. Call comm.cat_builder.add_rup one or more times to add the seed ruptures.
		// 6. Call comm.cat_builder.end_generation() to end the first (seed) generation.

		@Override
		public void seed_catalog (OECatalogSeedComm comm) {

			boolean f_loop = true;
			while (f_loop) {

				// Get the voxel and sub-voxel indexes

				final int local_seed_index = seeding_index.incrementAndGet();		// a different index for each catalog, first index we use is 1
				final int global_subvox_index = a_seed_subvox[local_seed_index % seed_subvox_count];	// wrap if number of catalogs exceeds number of seeds
				final int voxel_index = get_voxel_for_subvox (global_subvox_index);
				final int local_subvox_index = get_local_for_global_subvox (global_subvox_index, voxel_index);

				// Let the voxel make the catalog parameters

				final double cat_br = a_voxel_list[voxel_index].get_vox_cat_params (
					fit_info,
					proto_cat_params,
					local_cat_params
				);

				// If branch ratio does not exceed maximum ...

				if (cat_br <= OEConstants.EXCLUDE_DITHERING_BR_FOR_SIM) {

					// Stop looping

					f_loop = false;

					// Let the voxel seed the catalog

					a_voxel_list[voxel_index].seed_catalog (
						fit_info,
						local_subvox_index,
						local_cat_params,
						comm,
						local_rup
					);
				}
			}
		
			return;
		}

//		@Override
//		public void seed_catalog (OECatalogSeedComm comm) {
//
//			// Get the voxel and sub-voxel indexes
//
//			final int local_seed_index = seeding_index.incrementAndGet();		// a different index for each catalog, first index we use is 1
//			final int global_subvox_index = a_seed_subvox[local_seed_index % seed_subvox_count];	// wrap if number of catalogs exceeds number of seeds
//			final int voxel_index = get_voxel_for_subvox (global_subvox_index);
//			final int local_subvox_index = get_local_for_global_subvox (global_subvox_index, voxel_index);
//
//			// Let the voxel make the catalog parameters
//
//			a_voxel_list[voxel_index].get_cat_params (
//				fit_info,
//				proto_cat_params,
//				local_cat_params
//			);
//
//			// Let the voxel seed the catalog
//
//			a_voxel_list[voxel_index].seed_catalog (
//				fit_info,
//				local_subvox_index,
//				local_cat_params,
//				comm,
//				local_rup
//			);
//		
//			return;
//		}

	}




	//----- Implementation of OEEnsembleInitializer -----




	// Make a catalog seeder.
	// Returns a seeder which is able to seed the contents of one catalog
	// (or several catalogs in succession).
	// This function may be called repeatedly to create several seeders,
	// which can be used in multiple worker threads.
	// Threading: Can be called in multiple threads, before or after the call to
	// begin_initialization, and while there are existing open seeders, and so
	// must be properly synchronized.
	// Note: The returned seeder cannot be opened until after the call to
	// begin_initialization, and must be closed before the call to end_initialization.
	// Note: The returned seeder can be opened and closed repeatedly to seed
	// multiple catalogs.

	@Override
	public OECatalogSeeder make_seeder () {
		OECatalogSeeder seeder;
		seeder = new SeederVoxSet ();
		return seeder;
	}




	// Begin initializing catalogs.
	// This function should be called before any other control methods.
	// The initializer should allocate any resources it needs.
	// Threading: No other thread should be accessing this object,
	// and none of its seeders can be open.

	@Override
	public void begin_initialization () {

		// Reset the seeding index, so each ensemble repeats the same sequence of seedings

		seeding_index.set (0);
		return;
	}




	// End initializing catalogs.
	// This function should be called after all other control functions.
	// It provides an opportunity for the initializer to release any resources it holds.
	// Threading: No other thread should be accessing this object,
	// and none of its seeders can be open.

	@Override
	public void end_initialization () {
		return;
	}




	// Return true if there is a mainshock magnitude available.
	// Threading: No other thread should be accessing this object,
	// and be either before calling begin_initialization() or after
	// calling end_initialization().

	@Override
	public boolean has_mainshock_mag () {
		return fit_info.has_mag_main();
	}




	// Get a list of custom minimum magnitude bins for computing the forecast.
	// The returned object is newly-allocated and not retained in this object.
	// Can return null or an empty array if no custom minimum magnitude bins have been set.

	private double[] custom_min_mag_bins = null;

	@Override
	public double[] get_custom_min_mag_bins () {
		return custom_min_mag_bins;
	}




	// Set a list of custom minimum magnitude bins for computing the forecast.
	// The supplied object is not retained in this object.
	// The supplied object can be null or an empty array to set no custom minimum magnitude bins.

	@Override
	public void set_custom_min_mag_bins (double[] custom_min_mag_bins) {
		if (custom_min_mag_bins == null) {
			this.custom_min_mag_bins = null;
		} else {
			this.custom_min_mag_bins = custom_min_mag_bins.clone();
		}
		return;
	}




	// Return the mainshock magnitude.
	// Check has_mainshock_mag() before calling this function.
	// Note: If has_mainshock_mag() returns false, then this function should
	// return a scaling magnitude (e.g., largest earthquake in a swarm), which
	// can be used for simulation ranging, but is not reported in the forecast.
	// Threading: No other thread should be accessing this object,
	// and be either before calling begin_initialization() or after
	// calling end_initialization().

	@Override
	public double get_mainshock_mag () {
		return fit_info.mag_main;
	}




	// Get the time and magnitude range of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	@Override
	public OECatalogRange get_range () {
		return proto_cat_params.get_range();
	}




	// Get the initial time and magnitude range of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	@Override
	public OECatalogRange get_initial_range () {
		return original_cat_params.get_range();
	}




	// Set the time and magnitude range to use for catalog simulations.
	// The supplied OECatalogRange object is not retained.
	// Note: This function allows adjusting time and magnitude ranges
	// without the need to construct an entirely new initializer.

	@Override
	public void set_range (OECatalogRange range) {
		proto_cat_params.set_range (range);
		return;
	}




	// Get the size limits of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	@Override
	public OECatalogLimits get_limits () {
		return proto_cat_params.get_limits();
	}




	// Get the initial size limits of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	@Override
	public OECatalogLimits get_initial_limits () {
		return original_cat_params.get_limits();
	}




	// Set the size limits to use for catalog simulations.
	// The supplied OECatalogLimits object is not retained.
	// Note: This function allows adjusting size limits
	// without the need to construct an entirely new initializer.

	@Override
	public void set_limits (OECatalogLimits limits) {
		proto_cat_params.set_limits (limits);
		return;
	}




	// Get the b-value used by the initializer.
	// The purpose of this function is to obtain a b-value that can be used
	// for adjusting the magnitude range in order to get a desired median
	// or expected catalog size.

	@Override
	public double get_b_value () {
		return ranging_b_value;
	}




	// Get the time at which the forecast begins, in days.
	// The value should be >= the simulation begin time in the catalog parameters.

	@Override
	public double get_t_forecast () {
		return t_forecast;
	}




	// Get parameters that can be displayed to the user.
	// Parameters:
	//  paramMap = Map of parameters, which this function adds to.
	// For consistency, it is recommended that parameter names use camelCase.
	// Each value should be one of: Integer, Long, Double, Float, Boolean, or String.

	@Override
	public void get_display_params (Map<String, Object> paramMap) {
		paramMap.put ("b", SimpleUtils.round_double_via_string ("%.2f", mle_grid_point.b));
		paramMap.put ("alpha", SimpleUtils.round_double_via_string ("%.2f", mle_grid_point.alpha));
		paramMap.put ("c", SimpleUtils.round_double_via_string ("%.2e", mle_grid_point.c));
		paramMap.put ("p", SimpleUtils.round_double_via_string ("%.2f", mle_grid_point.p));
		paramMap.put ("n", SimpleUtils.round_double_via_string ("%.2e", mle_grid_point.n));
		paramMap.put ("zams", SimpleUtils.round_double_via_string ("%.2e", mle_grid_point.zams));
		paramMap.put ("zmu", SimpleUtils.round_double_via_string ("%.2e", mle_grid_point.zmu));

		//paramMap.put ("etas_Mref", SimpleUtils.round_double_via_string ("%.2f", proto_cat_params.mref));
		//paramMap.put ("etas_Msup", SimpleUtils.round_double_via_string ("%.2f", proto_cat_params.msup));
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 116001;

	private static final String M_VERSION_NAME = "OEDisc2InitVoxSet";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			OEDisc2InitFitInfo.static_marshal (writer, "fit_info"              , fit_info              );
			writer.marshalDouble              (        "b_scaling"             , b_scaling             );
			writer.marshalInt                 (        "voxel_count"           , voxel_count           );
			OEDisc2InitStatVox.marshal_array  (writer, "a_voxel_list"          , a_voxel_list          );
			OECatalogParams.static_marshal    (writer, "original_cat_params"   , original_cat_params   );
			writer.marshalDouble              (        "bay_weight"            , bay_weight            );
			writer.marshalInt                 (        "seed_subvox_count"     , seed_subvox_count     );
			writer.marshalDouble              (        "t_forecast"            , t_forecast            );
			OECatalogParams.static_marshal    (writer, "proto_cat_params"      , proto_cat_params      );
			writer.marshalInt                 (        "total_subvox_count"    , total_subvox_count    );
			writer.marshalIntArray            (        "cum_subvox_count"      , cum_subvox_count      );
			writer.marshalDouble              (        "max_log_density"       , max_log_density       );
			writer.marshalInt                 (        "mle_voxel_index"       , mle_voxel_index       );
			writer.marshalInt                 (        "mle_subvox_index"      , mle_subvox_index      );
			OEGridPoint.static_marshal        (writer, "mle_grid_point"        , mle_grid_point        );
			writer.marshalDouble              (        "gen_max_log_density"   , gen_max_log_density   );
			writer.marshalInt                 (        "gen_mle_voxel_index"   , gen_mle_voxel_index   );
			writer.marshalInt                 (        "gen_mle_subvox_index"  , gen_mle_subvox_index  );
			OEGridPoint.static_marshal        (writer, "gen_mle_grid_point"    , gen_mle_grid_point    );
			writer.marshalDouble              (        "seq_max_log_density"   , seq_max_log_density   );
			writer.marshalInt                 (        "seq_mle_voxel_index"   , seq_mle_voxel_index   );
			writer.marshalInt                 (        "seq_mle_subvox_index"  , seq_mle_subvox_index  );
			OEGridPoint.static_marshal        (writer, "seq_mle_grid_point"    , seq_mle_grid_point    );
			writer.marshalDouble              (        "bay_max_log_density"   , bay_max_log_density   );
			writer.marshalInt                 (        "bay_mle_voxel_index"   , bay_mle_voxel_index   );
			writer.marshalInt                 (        "bay_mle_subvox_index"  , bay_mle_subvox_index  );
			OEGridPoint.static_marshal        (writer, "bay_mle_grid_point"    , bay_mle_grid_point    );
			writer.marshalIntArray            (        "a_seed_subvox"         , a_seed_subvox         );
			writer.marshalInt                 (        "dither_mismatch"       , dither_mismatch       );
			writer.marshalDouble              (        "clip_log_density_prob" , clip_log_density_prob );
			writer.marshalInt                 (        "clip_log_density_tally", clip_log_density_tally);
			writer.marshalDouble              (        "clip_tail_prob"        , clip_tail_prob        );
			writer.marshalInt                 (        "clip_tail_tally"       , clip_tail_tally       );
			writer.marshalDouble              (        "clip_seed_prob"        , clip_seed_prob        );
			writer.marshalInt                 (        "clip_seed_tally"       , clip_seed_tally       );
			writer.marshalDouble              (        "seed_b_value"          , seed_b_value          );
			writer.marshalDouble              (        "ranging_b_value"       , ranging_b_value       );

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

			fit_info               = OEDisc2InitFitInfo.static_unmarshal (reader, "fit_info"              );
			b_scaling              = reader.unmarshalDouble              (        "b_scaling"             );
			voxel_count            = reader.unmarshalInt                 (        "voxel_count"           );
			a_voxel_list           = OEDisc2InitStatVox.unmarshal_array  (reader, "a_voxel_list"          );
			original_cat_params    = OECatalogParams.static_unmarshal    (reader, "original_cat_params"   );
			bay_weight             = reader.unmarshalDouble              (        "bay_weight"            );
			seed_subvox_count      = reader.unmarshalInt                 (        "seed_subvox_count"     );
			t_forecast             = reader.unmarshalDouble              (        "t_forecast"            );
			proto_cat_params       = OECatalogParams.static_unmarshal    (reader, "proto_cat_params"      );
			total_subvox_count     = reader.unmarshalInt                 (        "total_subvox_count"    );
			cum_subvox_count       = reader.unmarshalIntArray            (        "cum_subvox_count"      );
			max_log_density        = reader.unmarshalDouble              (        "max_log_density"       );
			mle_voxel_index        = reader.unmarshalInt                 (        "mle_voxel_index"       );
			mle_subvox_index       = reader.unmarshalInt                 (        "mle_subvox_index"      );
			mle_grid_point         = OEGridPoint.static_unmarshal        (reader, "mle_grid_point"        );
			gen_max_log_density    = reader.unmarshalDouble              (        "gen_max_log_density"   );
			gen_mle_voxel_index    = reader.unmarshalInt                 (        "gen_mle_voxel_index"   );
			gen_mle_subvox_index   = reader.unmarshalInt                 (        "gen_mle_subvox_index"  );
			gen_mle_grid_point     = OEGridPoint.static_unmarshal        (reader, "gen_mle_grid_point"    );
			seq_max_log_density    = reader.unmarshalDouble              (        "seq_max_log_density"   );
			seq_mle_voxel_index    = reader.unmarshalInt                 (        "seq_mle_voxel_index"   );
			seq_mle_subvox_index   = reader.unmarshalInt                 (        "seq_mle_subvox_index"  );
			seq_mle_grid_point     = OEGridPoint.static_unmarshal        (reader, "seq_mle_grid_point"    );
			bay_max_log_density    = reader.unmarshalDouble              (        "bay_max_log_density"   );
			bay_mle_voxel_index    = reader.unmarshalInt                 (        "bay_mle_voxel_index"   );
			bay_mle_subvox_index   = reader.unmarshalInt                 (        "bay_mle_subvox_index"  );
			bay_mle_grid_point     = OEGridPoint.static_unmarshal        (reader, "bay_mle_grid_point"    );
			a_seed_subvox          = reader.unmarshalIntArray            (        "a_seed_subvox"         );
			dither_mismatch        = reader.unmarshalInt                 (        "dither_mismatch"       );
			clip_log_density_prob  = reader.unmarshalDouble              (        "clip_log_density_prob" );
			clip_log_density_tally = reader.unmarshalInt                 (        "clip_log_density_tally");
			clip_tail_prob         = reader.unmarshalDouble              (        "clip_tail_prob"        );
			clip_tail_tally        = reader.unmarshalInt                 (        "clip_tail_tally"       );
			clip_seed_prob         = reader.unmarshalDouble              (        "clip_seed_prob"        );
			clip_seed_tally        = reader.unmarshalInt                 (        "clip_seed_tally"       );
			seed_b_value           = reader.unmarshalDouble              (        "seed_b_value"          );
			ranging_b_value        = reader.unmarshalDouble              (        "ranging_b_value"       );

		}
		break;

		}

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public OEDisc2InitVoxSet unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDisc2InitVoxSet accumulator) {
		accumulator.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEDisc2InitVoxSet static_unmarshal (MarshalReader reader, String name) {
		return (new OEDisc2InitVoxSet()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Get the number of voxels.
	// This function is for testing.

	public final int test_get_voxel_count () {
		return voxel_count;
	}




	// Get the voxel at the specified index.
	// This function is for testing.

	public final OEDisc2InitStatVox test_get_voxel (int index) {
		return a_voxel_list[index];
	}




	// Fill a grid with a c/p/a/ams likelihood grid.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  grid = Grid to receive log-density values, indexed as grid[cix][pix][aix][amsix].
	// This function is for testing.
	// Note: This function assumes that b, alpha, and zmu are fixed, and that the
	// voxels represent a grid constructed from separate ranges of c, p, n, and zams.
	// The function depends on the fact that voxels are sorted into canoncial order,
	// which sorts first on b, then alpha, then c, then p, then n.  It also depends
	// on zams values appearing in order in the list of sub=voxels.

	public final void test_get_c_p_a_ams_log_density_grid (double bay_weight, double[][][][] grid) {
		final int c_length = grid.length;
		final int p_length = grid[0].length;
		final int a_length = grid[0][0].length;
		final int ams_length = grid[0][0][0].length;
		if (!( c_length * p_length * a_length == voxel_count )) {
			throw new InvariantViolationException ("OEDisc2InitVoxSet.test_get_log_density_grid: Grid size mismatch: c_length = " + c_length + ", p_length = " + p_length + ", a_length = " + a_length + ", voxel_count = " + voxel_count);
		}
		for (int cix = 0; cix < c_length; ++cix) {
			for (int pix = 0; pix < p_length; ++pix) {
				for (int aix = 0; aix < a_length; ++aix) {
					final OEDisc2InitStatVox stat_vox = a_voxel_list[(((cix * p_length) + pix) * a_length) + aix];
					final int subvox_count = stat_vox.get_subvox_count();
					if (!( ams_length == subvox_count )) {
						throw new InvariantViolationException ("OEDisc2InitVoxSet.test_get_log_density_grid: Grid size mismatch: ams_length = " + ams_length + ", subvox_count = " + subvox_count);
					}
					for (int amsix = 0; amsix < ams_length; ++amsix) {
						grid[cix][pix][aix][amsix] = stat_vox.get_subvox_log_density (amsix, bay_weight);
					}
				}
			}
		}
		return;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEDisc2InitVoxSet : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OEDisc2InitVoxSet : Unrecognized subcommand : " + args[0]);
		return;

	}

}
