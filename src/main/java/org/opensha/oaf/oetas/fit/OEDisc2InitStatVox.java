package org.opensha.oaf.oetas.fit;

import java.util.List;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.InvariantViolationException;
import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;

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


// Catalog initialization for one voxel of statistical parameters (b, alpha, c, p, a|n).
// Author: Michael Barall 02/23/2023.
//
// Each voxel contains sub-voxels for various values of excitation parameters (zams, zmu).
//
// For each sub-voxel, contains the fitted log-likelihood, and the Bayesiam prior
// log-density and volume.
//
// For each source group, contains coefficients for seeding the catalog. 

public class OEDisc2InitStatVox implements Comparable<OEDisc2InitStatVox> {

	//---- Voxel defintion ---

	// Gutenberg-Richter value and element.

	private OEValueElement b_velt;

	// ETAS intensity value and element, can be null to force alpha == b.

	private OEValueElement alpha_velt;

	// Omori c-value and element.

	private OEValueElement c_velt;

	// Omori p-value and element.

	private OEValueElement p_velt;

	// Branch ratio value and element.

	private OEValueElement n_velt;




	//----- Sub-voxel definition -----

	// Values and elements for zams, for each sub-voxel, length = subvox_count, must be non-empty.
	// This array can be shared among multiple voxels, and so must not be modified.

	private OEValueElement[] a_zams_velt;

	// Values and elements for zmu, for each sub-voxel, length = subvox_count, can be null to force zmu == 0.
	// This array can be shared among multiple voxels, and so must not be modified.

	private OEValueElement[] a_zmu_velt;




	//----- Bayesian prior -----

	// The Bayesian prior log-density for each sub-voxel, length = subvox_count.

	private double[] bay_log_density;

	// The Bayesian prior volume for each sub-voxel, length = subvox_count.

	private double[] bay_vox_volume;




	//----- Fitted data -----

	// The value of (10^aint)*Q used for fitting parameters to this voxel.

	private double ten_aint_q;

	// The fitted log-likelihood for each sub-voxel, length = subvox_count.

	private double[] log_likelihood;




	//----- Seeding data -----
	//
	// For group n, the productivity to use for seeding simulations is:
	//  prod_scnd[n]*(10^a)*Q + prod_main[n]*(10^ams)*Q + prod_bkgd[n]*mu
	//  (Note that the two Q's in the above formula are generally different; the second Q is typically 1.)

	//  Unscaled productivity density due to non-mainshocks; length = group_count.

	private double[] prod_scnd;

	// Unscaled productivity density due to mainshocks; length = group_count.

	private double[] prod_main;

	// Unscaled productivity density due to a background rate; length = group_count; can be null.
	// Note: After setup, this is non-null if and only if background rates are supported.
	// In particular, if a_zmu_velt is null, then this is null.

	private double[] prod_bkgd;




	//----- Intensity computation -----
	//
	// Information needed to calculate the extended source contribution to the intensity function.

	// The unscaled productivity density for each interval, due to non-mainshocks.
	// With length == interval_count, each element contains the unscaled productivity density for the corresponding interval.
	// Multiply by the interval duration to get total productivity (not density).
	// Multiply by (10^a)*Q to scale the productivity of the non-mainshock source ruptures.
	// The two multiplications yield the productivity to use for seeding simulations.
	// Null if the intensity function is not being computed.

	private double[] unscaled_prod_density_int;

	// The unscaled productivity density for each interval, due to mainshocks.
	// With length == interval_count, each element contains the unscaled productivity density for the corresponding interval.
	// Multiply by the interval duration to get total productivity (not density).
	// Multiply by (10^ams)*Q to scale the productivity of the mainshock source ruptures.
	// The two multiplications yield the productivity to use for seeding simulations.
	// Null if the intensity function is not being computed.

	private double[] unscaled_prod_density_main_int;

	// The unscaled productivity density for each interval, due to a background rate.
	// With length == interval_count, each element contains the unscaled productivity density for the corresponding interval.
	// Multiply by the interval duration to get total productivity (not density).
	// Multiply by mu to scale the the background rate.
	// The two multiplications yield the productivity to use for seeding simulations.
	// Null if the intensity function is not being computed, or if there is no background rate.

	private double[] unscaled_prod_density_bkgd_int;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		b_velt = null;
		alpha_velt = null;
		c_velt = null;
		p_velt = null;
		n_velt = null;

		a_zams_velt = null;
		a_zmu_velt = null;

		bay_log_density = null;
		bay_vox_volume = null;

		ten_aint_q = 0.0;
		log_likelihood = null;

		prod_scnd = null;
		prod_main = null;
		prod_bkgd = null;

		unscaled_prod_density_int = null;
		unscaled_prod_density_main_int = null;
		unscaled_prod_density_bkgd_int = null;

		return;
	}




	// Default constructor.

	public OEDisc2InitStatVox () {
		clear();
	}




	// Set the voxel definition.
	// Parameters:
	//  b_velt = Gutenberg-Richter value and element.
	//  alpha_velt = ETAS intensity value and element, can be null to force alpha == b.
	//  c_velt = Omori c-value and element.
	//  p_velt = Omori p-value and element.
	//  n_velt = Branch ratio value and element.
	// Returns this object.

	public OEDisc2InitStatVox set_voxel_def (
		OEValueElement b_velt,
		OEValueElement alpha_velt,
		OEValueElement c_velt,
		OEValueElement p_velt,
		OEValueElement n_velt
	) {
		this.b_velt = b_velt;
		this.alpha_velt = alpha_velt;
		this.c_velt = c_velt;
		this.p_velt = p_velt;
		this.n_velt = n_velt;
		return this;
	}




	// Set the sub-voxel definition.
	// Parameters:
	//  a_zams_velt = Values and elements for zams, for each sub-voxel, length = subvox_count, must be non-empty.
	//  a_zmu_velt = Values and elements for zmu, for each sub-voxel, length = subvox_count, can be null to force zmu == 0.
	// Returns this object.
	// Note: Arrays can be shared among multiple voxels.  This object retains the arrays.

	public OEDisc2InitStatVox set_subvox_def (
		OEValueElement[] a_zams_velt,
		OEValueElement[] a_zmu_velt
	) {
		if (!( a_zams_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitStatVox.set_subvox_def: No sub-voxels");
		}
		if (!( a_zams_velt.length <= OEConstants.MAX_EXCITATION_GRID )) {
			throw new IllegalArgumentException ("OEDisc2InitStatVox.set_subvox_def: Too many sub-voxels, a_zams_velt.length = " + a_zams_velt.length);
		}
		if (!( a_zmu_velt == null || a_zams_velt.length == a_zmu_velt.length )) {
			throw new IllegalArgumentException ("OEDisc2InitStatVox.set_subvox_def: Inconsistent sub-voxel count, a_zams_velt.length = " + a_zams_velt.length + ", a_zmu_velt.length = " + a_zmu_velt.length);
		}

		this.a_zams_velt = a_zams_velt;
		this.a_zmu_velt = a_zmu_velt;
		return this;
	}




	// Display our contents.
	// Parameters:
	//  max_subvox = Maximum number of sub-voxels to display, or -1 for no limit.
	//  max_group = Maximum number of groups to display, or -1 for no limit.

	public String toString (int max_subvox, int max_group) {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2InitStatVox:" + "\n");

		result.append ("b_velt = " + ((b_velt == null) ? "<null>" : b_velt.shortened_string()) + "\n");
		result.append ("alpha_velt = " + ((alpha_velt == null) ? "<null>" : alpha_velt.shortened_string()) + "\n");
		result.append ("c_velt = " + ((c_velt == null) ? "<null>" : c_velt.shortened_string()) + "\n");
		result.append ("p_velt = " + ((p_velt == null) ? "<null>" : p_velt.shortened_string()) + "\n");
		result.append ("n_velt = " + ((n_velt == null) ? "<null>" : n_velt.shortened_string()) + "\n");

		if (log_likelihood != null) {
			result.append ("ten_aint_q = " + rndd(ten_aint_q) + "\n");
		}

		final int subvox_count = ((a_zams_velt == null) ? 0 : a_zams_velt.length);
		result.append ("subvox_count = " + subvox_count + "\n");

		if (subvox_count > 0) {
			final int subvox_disp = ((max_subvox < 0) ? subvox_count : Math.min(subvox_count, max_subvox));
			for (int j = 0; j < subvox_disp; ++j) {
				result.append (j + ": zams_velt = " + a_zams_velt[j].shorter_string());
				if (a_zmu_velt != null) {
					result.append (", zmu_velt = " + a_zmu_velt[j].shorter_string());
				}
				if (bay_log_density != null) {
					result.append (", bay_log_density = " + rndd(bay_log_density[j]));
				}
				if (bay_vox_volume != null) {
					result.append (", bay_vox_volume = " + rndd(bay_vox_volume[j]));
				}
				if (log_likelihood != null) {
					result.append (", log_likelihood = " + rndd(log_likelihood[j]));
				}
				result.append ("\n");
			}
			if (subvox_disp > 0 && subvox_disp < subvox_count) {
				result.append ("   ... and " + (subvox_count - subvox_disp) + " additional sub-voxels ..." + "\n");
			}
		}

		final int group_count = ((prod_scnd == null) ? 0 : prod_scnd.length);
		result.append ("group_count = " + group_count + "\n");

		if (group_count > 0) {
			final int group_disp = ((max_group < 0) ? group_count : Math.min(group_count, max_group));
			for (int j = 0; j < group_disp; ++j) {
				result.append (j + ": prod_scnd = " + rndd(prod_scnd[j]));
				if (prod_main != null) {
					result.append (", prod_main = " + rndd(prod_main[j]));
				}
				if (prod_bkgd != null) {
					result.append (", prod_bkgd = " + rndd(prod_bkgd[j]));
				}
				result.append ("\n");
			}
			if (group_disp > 0 && group_disp < group_count) {
				result.append ("   ... and " + (group_count - group_disp) + " additional groups ..." + "\n");
			}
		}

		result.append (((unscaled_prod_density_int == null) ? "unscaled_prod_density_int = <null>" : ("unscaled_prod_density_int.length = " + unscaled_prod_density_int.length)) + "\n");
		result.append (((unscaled_prod_density_main_int == null) ? "unscaled_prod_density_main_int = <null>" : ("unscaled_prod_density_main_int.length = " + unscaled_prod_density_main_int.length)) + "\n");
		result.append (((unscaled_prod_density_bkgd_int == null) ? "unscaled_prod_density_bkgd_int = <null>" : ("unscaled_prod_density_bkgd_int.length = " + unscaled_prod_density_bkgd_int.length)) + "\n");

		return result.toString();
	}




	// Display our contents.

	@Override
	public String toString() {
		return toString (100, 100);
	}




	// Display a summary string.

	public String summary_string () {
		return toString (0, 0);
	}




	// Dump the entire contents to a string.
	// Caution: Can be very large.

	public String dump_string () {
		return toString (-1, -1);
	}




	// Dump the log density data to a string, with one line for each sub-voxel.
	// Parameters:
	//  sb = Buffer to receive data, which is appended to the buffer.
	// Returns sb.
	// Each line contains the following:
	//   b  alpha  c  p  n  zams  zmu  bay_log_density  bay_vox_volume  log_likelihood
	// Both Bayesian prior and log-likelihoods must have been computed.

	public StringBuilder dump_log_density_to_string (StringBuilder sb) {

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double c = c_velt.get_ve_value();
		final double p = p_velt.get_ve_value();
		final double n = n_velt.get_ve_value();

		// Make the prefix string that is used for each line

		final String prefix = rndf(b) + " " + rndf(alpha) + " " + rndf(c) + " " + rndf(p) + " " + rndf(n) + " ";

		// Get the array lengths

		final int subvox_count = get_subvox_count();

		// Loop to append lines

		for (int j = 0; j < subvox_count; ++j) {

			// Get parameters for this sub-voxel

			final double zams = a_zams_velt[j].get_ve_value();
			final double zmu = ((a_zmu_velt == null) ? 0.0 : a_zmu_velt[j].get_ve_value());

			sb.append (prefix);
			sb.append (rndf(zams));
			sb.append (" ");
			sb.append (rndf(zmu));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string ("%.11E", bay_log_density[j]));
			//sb.append (rndf(bay_log_density[j]));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string ("%.11E", bay_vox_volume[j]));
			//sb.append (rndf(bay_vox_volume[j]));
			sb.append (" ");
			sb.append (SimpleUtils.double_to_string ("%.11E", log_likelihood[j]));
			//sb.append (rndf(log_likelihood[j]));
			sb.append ("\n");
		}

		return sb;
	}




	// Get the grid point corresponding to a given sub-voxel.
	// Parameters:
	//  subvox_index = Sub-voxel index (0-based).
	//  grid_point = Receives the grid point.
	// Returns grid_point.

	public OEGridPoint get_subvox_grid_point (int subvox_index, OEGridPoint grid_point) {

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double c = c_velt.get_ve_value();
		final double p = p_velt.get_ve_value();
		final double n = n_velt.get_ve_value();

		// Get parameters for this sub-voxel

		final double zams = a_zams_velt[subvox_index].get_ve_value();
		final double zmu = ((a_zmu_velt == null) ? 0.0 : a_zmu_velt[subvox_index].get_ve_value());

		// Set the grid point

		grid_point.set (
			b,
			alpha,
			c,
			p,
			n,
			zams,
			zmu
		);

		return grid_point;
	}




	//----- Operations -----




	// Get the number of sub-voxels.
	// Threading: Can be called simultaneously by multiple threads.

	public final int get_subvox_count () {
		return a_zams_velt.length;
	}




	// Return true if background rates are supported.
	// Threading: Can be called simultaneously by multiple threads.

	public final boolean is_background_supported () {
		return prod_bkgd != null;
	}




	// Get the b-value for this voxel.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_b_value () {
		return b_velt.get_ve_value();
	}




	// Apply the Bayesian prior.
	// Parameters:
	//  bay_prior = Bayesian prior.
	//  bay_params = Parameters to pass when calling the Bayesian prior.
	// Returns this object.

	public final OEDisc2InitStatVox apply_bay_prior (OEBayPrior bay_prior, OEBayPriorParams bay_params) {

		// Allocate storage

		final int subvox_count = get_subvox_count();

		bay_log_density = new double[subvox_count];
		bay_vox_volume = new double[subvox_count];

		// Apply the prior

		bay_prior.get_bay_value (
			bay_params,
			bay_log_density,
			bay_vox_volume,
			b_velt,
			alpha_velt,
			c_velt,
			p_velt,
			n_velt,
			a_zams_velt,
			a_zmu_velt
		);

		return this;
	}




	// Calculate the log-likelihoods.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  avpr = Handle we can use to build and read out per-a-value processing.
	//  pmom = Handle to a pair of exponent and Omori values, which has already been built.
	// Returns this object.

	public final OEDisc2InitStatVox calc_likelihood (
		OEDisc2InitFitInfo fit_info,
		OEDisc2ExtFit.AValueProdHandle avpr,
		OEDisc2ExtFit.PairMagOmoriHandle pmom
	) {

		// Get the parameter values

		final double p = p_velt.get_ve_value();
		final double c = c_velt.get_ve_value();
		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double n = n_velt.get_ve_value();

		// Check that the parameter values match

		String check = pmom.pmom_check_param_values (
			p,
			c,
			b,
			alpha
		);

		if (check != null) {
			throw new IllegalArgumentException ("OEDisc2InitStatVox.calc_likelihood: Parameter value mismatch: " + check);
		}

		// Get the array lengths

		final int subvox_count = get_subvox_count();

		// Allocate storage

		log_likelihood = new double[subvox_count];
		prod_scnd = new double[fit_info.group_count];
		prod_main = new double[fit_info.group_count];
		if (fit_info.f_background && a_zmu_velt != null) {	// if fitter supports background and we have background values
			prod_bkgd = new double[fit_info.group_count];
		} else {
			prod_bkgd = null;
		}

		// Get (10^a)*Q for our branch ratio, use it for (10^aint)*Q

		//  ten_aint_q = pmom.pmom_calc_ten_a_q_from_branch_ratio (
		//  	n_velt.get_ve_value()
		//  );

		ten_aint_q = fit_info.calc_ten_a_q_from_branch_ratio (
			n,
			p,
			c,
			b,
			alpha
		) ;

		// Build the avpr object

		avpr.avpr_build (pmom, ten_aint_q);

		// If background supported ...

		if (is_background_supported()) {

			// Get the log-likelihood values for each (zams, zmu) pair

			for (int j = 0; j < subvox_count; ++j) {

				// Convert from zams to (10^ams)*Q, with Q == 1

				final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
					a_zams_velt[j].get_ve_value(),
					b,
					alpha
				);

				// Convert from zmu to mu, noting a_zmu_velt is non-null if background is supported

				final double mu = fit_info.calc_mu_from_zmu (
					a_zmu_velt[j].get_ve_value(),
					b
				);

				// Get the log-likelihood

				log_likelihood[j] = avpr.avpr_calc_log_like (ten_aint_q, ten_ams_q, mu);
			}
		}

		// Otherwise, no background ...

		else {

			// Get the log-likelihood values for each zams (assuming zmu == 0)

			for (int j = 0; j < subvox_count; ++j) {

				// Convert from zams to (10^ams)*Q, with Q == 1

				final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
					a_zams_velt[j].get_ve_value(),
					b,
					alpha
				);

				// Get the log-likelihood

				log_likelihood[j] = avpr.avpr_calc_log_like (ten_aint_q, ten_ams_q);
			}
		}

		// Get the unscaled productivity for each group

		avpr.avpr_get_grouped_unscaled_prod_all (prod_scnd, prod_main, prod_bkgd);

		// If we need interval productivites ...

		if (fit_info.needs_interval_intensity()) {

			// Interval productivity descended from non-mainshock

			unscaled_prod_density_int = avpr.avpr_get_unscaled_prod_density_int();

			if (unscaled_prod_density_int != null && unscaled_prod_density_int.length == 0) {
				unscaled_prod_density_int = null;
			}

			// Interval productivity descended from mainshock

			unscaled_prod_density_main_int = avpr.avpr_get_unscaled_prod_density_main_int();

			if (unscaled_prod_density_main_int != null && unscaled_prod_density_main_int.length == 0) {
				unscaled_prod_density_main_int = null;
			}

			// If we have a background rate, interval productivity descended from the background rate.

			if (is_background_supported()) {
				unscaled_prod_density_bkgd_int = avpr.avpr_get_unscaled_prod_density_bkgd_int();

				if (unscaled_prod_density_bkgd_int != null && unscaled_prod_density_bkgd_int.length == 0) {
					unscaled_prod_density_bkgd_int = null;
				}
			}
		}

		return this;
	}




	// Get the log-density for the given sub-voxel.
	// Parameters:
	//  subvox_index = Sub-voxel index (0-based).
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	// Both Bayesian prior and log-likelihoods must have been computed.

	public final double get_subvox_log_density (int subvox_index, double bay_weight) {
		return (
			(bay_weight <= 1.0)
			? ((bay_log_density[subvox_index] * bay_weight) + log_likelihood[subvox_index])
			: (bay_log_density[subvox_index] + (log_likelihood[subvox_index] * (2.0 - bay_weight)))
		);
	}




	// Calculate the maximum log-density over the sub-voxels.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	// Both Bayesian prior and log-likelihoods must have been computed.

	public final double get_max_subvox_log_density (double bay_weight) {

		// Get the array lengths

		final int subvox_count = get_subvox_count();

		// Loop to find maximum

		double x = get_subvox_log_density (0, bay_weight);
		for (int j = 1; j < subvox_count; ++j) {
			x = Math.max (x, get_subvox_log_density (j, bay_weight));
		}

		return x;
	}




	// Find the sub-voxel index which has the maximum log-density over the sub-voxels.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	// Both Bayesian prior and log-likelihoods must have been computed.
	// Note: After calling this function, use get_subvox_log_density() to get the maximum log-density.

	public final int get_max_subvox_index_log_density (double bay_weight) {

		// Get the array lengths

		final int subvox_count = get_subvox_count();

		// Loop to find maximum

		int subvox_index = 0;
		double x = get_subvox_log_density (0, bay_weight);

		for (int j = 1; j < subvox_count; ++j) {
			double y = get_subvox_log_density (j, bay_weight);
			if (x < y) {
				subvox_index = j;
				x = y;
			}
		}

		return subvox_index;
	}




	// Get all sub-voxel probabilities and place them in bins.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  max_log_density = Maximum log-density over all voxels and sub-voxels.
	//  a_subvox_prob = Receives the probability of each sub-voxel, beginning at a_subvox_prob[dest_index].
	//  a_density_bin = Receives the bin for the log-density of each sub-voxel, beginning at a_density_bin[dest_index].
	//  dest_index = Initial index in a_subvox_prob and a_density_bin; the total number of sub-voxels in all prior voxels.
	//  bin_size_lnu = The size of each log-density bin, in natural log units.
	//  a_prob_accum = Accumulators for the probabiliy in each bin; length = bin_count.
	//  a_tally_accum = Accumulators for the number of sub-voxels in each bin; length = bin_count.
	//  stat_accum = Statistics accumulator.
	// Both Bayesian prior and log-likelihoods must have been computed.
	// This function computes the log-density and probability for each voxel.  It identifies the bin that
	// contains the log-density, with larger bin numbers corresponding to lower log-densities.  If the
	// log-density would place it past the last bin, it is included in the last bin.  The probability and
	// bin index are returned in a_subvox_prob and a_density_bin respectively, and the probability is
	// added to the element of a_prob_accum correspoinding to the bin.
	// Note: The i-th density bin contains sub-voxels whose negative normalized log-density lies between
	// i*bin_size_lnu and (i+1)*bin_size_lnu.  The last bin contains all sub-voxels whose negative
	// normalized log-density is greater than (bin_count-1)*bin_size_lnu.

	public final void get_probabilities_and_bin (
		double bay_weight,
		double max_log_density,
		double[] a_subvox_prob,
		int[] a_density_bin,
		int dest_index,
		double bin_size_lnu,
		double[] a_prob_accum,
		int[] a_tally_accum,
		OEDisc2VoxStatAccum stat_accum
	) {

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double c = c_velt.get_ve_value();
		final double p = p_velt.get_ve_value();
		final double n = n_velt.get_ve_value();

		// Pass them to the statistics accumulator

		stat_accum.vsaccum_set_b_alpha_c_p_n (
			b,
			alpha,
			c,
			p,
			n
		);

		// Get the number of density bins, both integer and floating-point

		final int bin_count = a_prob_accum.length;
		final double d_bin_count = (double)bin_count;

		// Loop over subvoxels ...

		final int subvox_count = get_subvox_count();
		for (int j = 0; j < subvox_count; ++j) {

			// Get parameters for this sub-voxel

			final double zams = a_zams_velt[j].get_ve_value();
			final double zmu = ((a_zmu_velt == null) ? 0.0 : a_zmu_velt[j].get_ve_value());

			// Pass them to the statistics accumulator

			stat_accum.vsaccum_add_data (
				zams,
				zmu,
				bay_log_density[j],
				bay_vox_volume[j],
				log_likelihood[j]
			);

			// The log density, normalized to be non-positive

			final double norm_log_density = get_subvox_log_density (j, bay_weight) - max_log_density;

			// Get the bin index, 0 for the highest log density

			final double d_bin_index = (- norm_log_density) / bin_size_lnu;
			int bin_index;
			if (d_bin_index >= d_bin_count) {
				bin_index = bin_count - 1;
			} else {
				bin_index = (int)Math.round (d_bin_index - 0.5);
				if (bin_index < 0) {
					bin_index = 0;
				}
				else if (bin_index >= bin_count) {
					bin_index = bin_count - 1;
				}
			}

			// Compute and accumulate the probability

			final double probability = Math.exp(norm_log_density) * bay_vox_volume[j];

			a_subvox_prob[j + dest_index] = probability;
			a_density_bin[j + dest_index] = bin_index;
			a_prob_accum[bin_index] += probability;
			a_tally_accum[bin_index]++;
		}

		return;
	}

	// Following older verson without stat_accum is deprecated.

	public final void get_probabilities_and_bin (
		double bay_weight,
		double max_log_density,
		double[] a_subvox_prob,
		int[] a_density_bin,
		int dest_index,
		double bin_size_lnu,
		double[] a_prob_accum,
		int[] a_tally_accum
	) {

		// Get the number of density bins, both integer and floating-point

		final int bin_count = a_prob_accum.length;
		final double d_bin_count = (double)bin_count;

		// Loop over subvoxels ...

		final int subvox_count = get_subvox_count();
		for (int j = 0; j < subvox_count; ++j) {

			// The log density, normalized to be non-positive

			final double norm_log_density = get_subvox_log_density (j, bay_weight) - max_log_density;

			// Get the bin index, 0 for the highest log density

			final double d_bin_index = (- norm_log_density) / bin_size_lnu;
			int bin_index;
			if (d_bin_index >= d_bin_count) {
				bin_index = bin_count - 1;
			} else {
				bin_index = (int)Math.round (d_bin_index - 0.5);
				if (bin_index < 0) {
					bin_index = 0;
				}
				else if (bin_index >= bin_count) {
					bin_index = bin_count - 1;
				}
			}

			// Compute and accumulate the probability

			final double probability = Math.exp(norm_log_density) * bay_vox_volume[j];

			a_subvox_prob[j + dest_index] = probability;
			a_density_bin[j + dest_index] = bin_index;
			a_prob_accum[bin_index] += probability;
			a_tally_accum[bin_index]++;
		}

		return;
	}




	// Get all sub-voxel probabilities and place them in bins.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  max_log_density = Maximum log-density over all voxels and sub-voxels.
	//  a_subvox_prob = Receives the probability of each sub-voxel, beginning at a_subvox_prob[dest_index].
	//  a_density_bin = Receives the bin for the log-density of each sub-voxel, beginning at a_density_bin[dest_index].
	//  dest_index = Initial index in a_subvox_prob and a_density_bin; the total number of sub-voxels in all prior voxels.
	//  bin_size_lnu = The size of each log-density bin, in natural log units.
	//  a_prob_accum = Accumulators for the probabiliy in each bin; length = bin_count.
	// Both Bayesian prior and log-likelihoods must have been computed.
	// This function computes the log-density and probability for each voxel.  It identifies the bin that
	// contains the log-density, with larger bin numbers corresponding to lower log-densities.  If the
	// log-density would place it past the last bin, it is included in the last bin.  The probability and
	// bin index are returned in a_subvox_prob and a_density_bin respectively, and the probability is
	// added to the element of a_prob_accum correspoinding to the bin.
	// Note: The i-th density bin contains sub-voxels whose negative normalized log-density lies between
	// i*bin_size_lnu and (i+1)*bin_size_lnu.  The last bin contains all sub-voxels whose negative
	// normalized log-density is greater than (bin_count-1)*bin_size_lnu.

//	public final void get_probabilities_and_bin (
//		double bay_weight,
//		double max_log_density,
//		double[] a_subvox_prob,
//		int[] a_density_bin,
//		int dest_index,
//		double bin_size_lnu,
//		double[] a_prob_accum
//	) {
//
//		// Get the number of density bins, both integer and floating-point
//
//		final int bin_count = a_prob_accum.length;
//		final double d_bin_count = (double)bin_count;
//
//		// Loop over subvoxels ...
//
//		final int subvox_count = get_subvox_count();
//		for (int j = 0; j < subvox_count; ++j) {
//
//			// The log density, normalized to be non-positive
//
//			final double norm_log_density = get_subvox_log_density (j, bay_weight) - max_log_density;
//
//			// Get the bin index, 0 for the highest log density
//
//			final double d_bin_index = (- norm_log_density) / bin_size_lnu;
//			int bin_index;
//			if (d_bin_index >= d_bin_count) {
//				bin_index = bin_count - 1;
//			} else {
//				bin_index = (int)Math.round (d_bin_index - 0.5);
//				if (bin_index < 0) {
//					bin_index = 0;
//				}
//				else if (bin_index >= bin_count) {
//					bin_index = bin_count - 1;
//				}
//			}
//
//			// Compute and accumulate the probability
//
//			final double probability = Math.exp(norm_log_density) * bay_vox_volume[j];
//
//			a_subvox_prob[j + dest_index] = probability;
//			a_density_bin[j + dest_index] = bin_index;
//			a_prob_accum[bin_index] += probability;
//		}
//
//		return;
//	}




	// Get catalog parameters.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  proto_cat_params = Catalog parameters, containing everything except our statistics (a, p, c, b, alpha).
	//  local_cat_params = Receives the catalog parameters, with our statistics inserted.
	// Threading: Parameters fit_info and proto_cat_params can be shared among multiple threads and so
	//  must not be modified.  Parameter local_cat_params is written by this function and so must be
	//  local to the current thread.

	public final void get_cat_params (
		OEDisc2InitFitInfo fit_info,
		OECatalogParams proto_cat_params,
		OECatalogParams local_cat_params
	) {

		// Get the parameter values

		final double p = p_velt.get_ve_value();
		final double c = c_velt.get_ve_value();
		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double n = n_velt.get_ve_value();

		// Convert branch ratio into productivity for the catalog's magnitude range

		final double a = fit_info.calc_a_from_branch_ratio (
			n,
			p,
			c,
			b,
			alpha,
			proto_cat_params.mref,
			proto_cat_params.msup
		);

		// Insert into catalog parameters

		local_cat_params.set_stat_and_copy_from (
			a,
			p,
			c,
			b,
			alpha,
			proto_cat_params
		);

		return;
	}




	// Get seed parameters.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  subvox_index = Index (0-based) of the sub-voxel to use for ams and mu.
	//  local_seed_params = Receives the seed parameters.
	// Threading: Parameter fit_info can be shared among multiple threads and so must not be modified.
	//  Parameter local_seed_params is written by this function and so must be local to the current thread.

	public final void get_seed_params (
		OEDisc2InitFitInfo fit_info,
		int subvox_index,
		OESeedParams local_seed_params
	) {

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());

		// Convert from zams to ams

		final double ams = fit_info.calc_ams_from_zams (
			a_zams_velt[subvox_index].get_ve_value(),
			b,
			alpha
		);

		// Convert from zmu to mu, noting a_zmu_velt is non-null if background is supported

		final double mu = (is_background_supported() ? fit_info.calc_mu_from_zmu (
			a_zmu_velt[subvox_index].get_ve_value(),
			b
		) : 0.0);

		// Fill the seed parameters

		local_seed_params.set (
			ams,			// ams
			mu,				// mu
			fit_info.mref,	// seed_mag_min
			fit_info.msup	// seed_mag_max
		);

		return;
	}




	// Seed a catalog.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  subvox_index = Index (0-based) of the sub-voxel to use for ams and mu.
	//  cat_params = Catalog parameters, possibly obtained via get_cat_params.
	//  comm = Communication area, with per-catalog values set up.
	//  local_rup = An object that this function can use as working storage (i.e., can overwrite).
	// Threading: Parameters fit_info and cat_params can be shared among multiple threads and so
	//  must not be modified (although typically cat_params is local to the current thread so that
	//  different statistical parameters can be used for each simulation).  Parameter local_rup
	//  must be local to the current thread and not shared, so it can be overwritten (local_rup
	//  can be newly allocated on each call, or re-used for multiple calls).
	// This function should perform the following steps:
	// 1. Construct a OECatalogParams object containing the catalog parameters.
	// 2. Call comm.cat_builder.begin_catalog() to begin constructing the catalog.
	// 3. Construct a OEGenerationInfo object containing info for the first (seed) generation.
	// 4. Call comm.cat_builder.begin_generation() to begin the first (seed) generation.
	// 5. Call comm.cat_builder.add_rup one or more times to add the seed ruptures.
	// 6. Call comm.cat_builder.end_generation() to end the first (seed) generation.

	public void seed_catalog (
		OEDisc2InitFitInfo fit_info,
		int subvox_index,
		OECatalogParams cat_params,
		OECatalogSeedComm comm,
		OERupture local_rup
	) {

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());

		// Begin the catalog

		comm.cat_builder.begin_catalog (cat_params);

		// Begin the first generation

		comm.cat_builder.begin_generation (fit_info.seed_gen_info);

		// If we are supporting background rate ...

		if (is_background_supported()) {

			// Convert from zams to (10^ams)*Q, with Q == 1

			final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
				a_zams_velt[subvox_index].get_ve_value(),
				b,
				alpha
			);

			// Convert from zmu to mu, noting a_zmu_velt is non-null if background is supported

			final double mu = fit_info.calc_mu_from_zmu (
				a_zmu_velt[subvox_index].get_ve_value(),
				b
			);

			// If the background rate is positive, add a background rate to the catalog

			if (mu > OEConstants.TINY_BACKGROUND_RATE) {
				local_rup.set_background (mu);
				comm.cat_builder.add_rup (local_rup);
			}

			// Now add a rupture for each source group

			final int group_count = fit_info.group_count;
			final double[] a_group_time = fit_info.a_group_time;

			for (int j = 0; j < group_count; ++j) {
				final double k_prod = prod_scnd[j]*ten_aint_q + prod_main[j]*ten_ams_q + prod_bkgd[j]*mu;
				final double rup_mag = fit_info.calc_m0_from_prod_and_ten_ams_q (
					k_prod,
					ten_ams_q,
					alpha
				);
				final double t_day = a_group_time[j];
				local_rup.set_seed (t_day, rup_mag, k_prod);
				comm.cat_builder.add_rup (local_rup);
			}
		}

		// Otherwise, not supporting background rate ...

		else {

			// Convert from zams to (10^ams)*Q, with Q == 1

			final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
				a_zams_velt[subvox_index].get_ve_value(),
				b,
				alpha
			);

			// Add a rupture for each source group

			final int group_count = fit_info.group_count;
			final double[] a_group_time = fit_info.a_group_time;

			for (int j = 0; j < group_count; ++j) {
				final double k_prod = prod_scnd[j]*ten_aint_q + prod_main[j]*ten_ams_q;		// note no background term
				final double rup_mag = fit_info.calc_m0_from_prod_and_ten_ams_q (
					k_prod,
					ten_ams_q,
					alpha
				);
				final double t_day = a_group_time[j];
				local_rup.set_seed (t_day, rup_mag, k_prod);
				comm.cat_builder.add_rup (local_rup);
			}
		}

		// End the first generation

		comm.cat_builder.end_generation();
		
		return;
	}




	// Comparison implements a canonical ordering based on the voxel definition.
	// Returns <0, ==0, or >0 depending on whether this object is less than, equal to, or greater than the other object.
	// Implementation note: We take advantage of the fact that OEValueElement objects
	// with the same value are probably the same object.

	@Override
	public final int compareTo (OEDisc2InitStatVox other) {
		if (this.b_velt != other.b_velt) {
			final int x = Double.compare (this.b_velt.get_ve_value(), other.b_velt.get_ve_value());
			if (x != 0) {
				return x;
			}
		}
		if (this.alpha_velt != other.alpha_velt) {
			final int x = Double.compare (
				(this.alpha_velt == null) ? this.b_velt.get_ve_value() : this.alpha_velt.get_ve_value(),
				(other.alpha_velt == null) ? other.b_velt.get_ve_value() : other.alpha_velt.get_ve_value()
			);
			if (x != 0) {
				return x;
			}
		}
		if (this.c_velt != other.c_velt) {
			final int x = Double.compare (this.c_velt.get_ve_value(), other.c_velt.get_ve_value());
			if (x != 0) {
				return x;
			}
		}
		if (this.p_velt != other.p_velt) {
			final int x = Double.compare (this.p_velt.get_ve_value(), other.p_velt.get_ve_value());
			if (x != 0) {
				return x;
			}
		}
		if (this.n_velt != other.n_velt) {
			final int x = Double.compare (this.n_velt.get_ve_value(), other.n_velt.get_ve_value());
			if (x != 0) {
				return x;
			}
		}
		return 0;
	}




	// Get parameters to calculate integrated intensity function.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  subvox_index = Index (0-based) of the sub-voxel to use for ams and mu.
	//  intensity_calc = Receives the parameters and extended source information.
	// Threading: Parameter fit_info can be shared among multiple threads and so must not be modified.
	//  Parameter intensity_calc is written by this function and so must be local to the current thread.

	public final void get_intensity_calc_params (
		OEDisc2InitFitInfo fit_info,
		int subvox_index,
		OEDisc2IntensityCalc intensity_calc
	) {

		// Get the parameter values

		final double p = p_velt.get_ve_value();
		final double c = c_velt.get_ve_value();
		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());

		// Use ten_aint_q for ten_a_q

		final double ten_a_q = ten_aint_q;

		// Convert from zams to (10^ams)*Q, with Q == 1

		final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
			a_zams_velt[subvox_index].get_ve_value(),
			b,
			alpha
		);

		// Convert from zmu to mu, noting a_zmu_velt is non-null if background is supported

		final double mu = (is_background_supported() ? fit_info.calc_mu_from_zmu (
			a_zmu_velt[subvox_index].get_ve_value(),
			b
		) : 0.0);

		// Pass parameters to intensity calculation

		intensity_calc.set_params (
			b,
			alpha,
			c,
			p,
			ten_a_q,
			ten_ams_q,
			mu
		);

		// If background supported ...

		if (is_background_supported()) {

			// If we have extended source productivity info ...

			if (unscaled_prod_density_int != null
				&& unscaled_prod_density_main_int != null
				&& unscaled_prod_density_bkgd_int != null
			) {

				// Create extended source productivity info

				final int interval_count = unscaled_prod_density_int.length;
				final double[] extended_prod_density = new double[interval_count];

				for (int j = 0; j < interval_count; ++j) {
					extended_prod_density[j] = (
						(unscaled_prod_density_int[j] * ten_a_q)
						+ (unscaled_prod_density_main_int[j] * ten_ams_q)
						+ (unscaled_prod_density_bkgd_int[j] * mu)
					);
				}

				// Pass extended source productivity into to intensity calculation

				intensity_calc.set_extended (
					extended_prod_density
				);
			}

			// Otherwise, no extended source productivity

			else {
				if (fit_info.needs_interval_intensity()) {
					throw new InvariantViolationException ("OEDisc2InitStatVox.get_intensity_calc_params: Information about extended source productivity is not available.");
				}

				intensity_calc.set_extended (
					null
				);
			}
		}

		// Otherwise, no background ...

		else {

			// If we have extended source productivity info ...

			if (unscaled_prod_density_int != null
				&& unscaled_prod_density_main_int != null
			) {

				// Create extended source productivity info

				final int interval_count = unscaled_prod_density_int.length;
				final double[] extended_prod_density = new double[interval_count];

				for (int j = 0; j < interval_count; ++j) {
					extended_prod_density[j] = (
						(unscaled_prod_density_int[j] * ten_a_q)
						+ (unscaled_prod_density_main_int[j] * ten_ams_q)
					);
				}

				// Pass extended source productivity into to intensity calculation

				intensity_calc.set_extended (
					extended_prod_density
				);
			}

			// Otherwise, no extended source productivity

			else {
				if (fit_info.needs_interval_intensity()) {
					throw new InvariantViolationException ("OEDisc2InitStatVox.get_intensity_calc_params: Information about extended source productivity is not available.");
				}

				intensity_calc.set_extended (
					null
				);
			}
		}

		return;
	}




	//----- Testing -----




	// Lay out a display of grouped productivities.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  subvox_index = Index (0-based) of the sub-voxel to use for ams and mu.
	// Assumes that log-likelihood and Bayesiam prior have been calculated.

	public String layout_grouped_prod (
		OEDisc2InitFitInfo fit_info,
		int subvox_index
	) {
		StringBuilder result = new StringBuilder();

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());

		// If we are supporting background rate ...

		if (is_background_supported()) {

			// Convert from zams to (10^ams)*Q, with Q == 1

			final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
				a_zams_velt[subvox_index].get_ve_value(),
				b,
				alpha
			);

			// Convert from zmu to mu, noting a_zmu_velt is non-null if background is supported

			final double mu = fit_info.calc_mu_from_zmu (
				a_zmu_velt[subvox_index].get_ve_value(),
				b
			);

			// Header line with productivity parameters

			final double ten_a_q = ten_aint_q;

			result.append (String.format ("ten_a_q = %.4e, ten_ams_q = %.4e, mu = %.4e",
					ten_a_q, ten_ams_q, mu));
			result.append ("\n");

			// Data lines, one for each group

			final int group_count = fit_info.group_count;
			final double[] a_group_time = fit_info.a_group_time;

			for (int i = 0; i < group_count; ++i) {
				result.append (String.format ("t = %12.6f", a_group_time[i]));
				result.append (String.format ("    scnd = %.3e", prod_scnd[i]));
				result.append (String.format ("  main = %.3e", prod_main[i]));
				result.append (String.format ("  bkgd = %.3e", prod_bkgd[i]));
				result.append (String.format ("    scnd_k = %.3e", prod_scnd[i] * ten_a_q));
				result.append (String.format ("  main_k = %.3e", prod_main[i] * ten_ams_q));
				result.append (String.format ("  bkgd_k = %.3e", prod_bkgd[i] * mu));
				result.append (String.format ("   k = %.3e", (prod_scnd[i] * ten_a_q) + (prod_main[i] * ten_ams_q) + (prod_bkgd[i] * mu) ));
				result.append ("\n");
			}
		}

		// Otherwise, not supporting background rate ...

		else {

			// Convert from zams to (10^ams)*Q, with Q == 1

			final double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
				a_zams_velt[subvox_index].get_ve_value(),
				b,
				alpha
			);

			// Header line with productivity parameters

			final double ten_a_q = ten_aint_q;

			result.append (String.format ("ten_a_q = %.4e, ten_ams_q = %.4e",
					ten_a_q, ten_ams_q));
			result.append ("\n");

			// Data lines, one for each group

			final int group_count = fit_info.group_count;
			final double[] a_group_time = fit_info.a_group_time;

			for (int i = 0; i < group_count; ++i) {
				result.append (String.format ("t = %12.6f", a_group_time[i]));
				result.append (String.format ("    scnd = %.3e", prod_scnd[i]));
				result.append (String.format ("  main = %.3e", prod_main[i]));
				result.append (String.format ("    scnd_k = %.3e", prod_scnd[i] * ten_a_q));
				result.append (String.format ("  main_k = %.3e", prod_main[i] * ten_ams_q));
				result.append (String.format ("   k = %.3e", (prod_scnd[i] * ten_a_q) + (prod_main[i] * ten_ams_q) ));
				result.append ("\n");
			}
		}
	
		return result.toString();
	}




	// Test to seed a catalog.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  subvox_index = Index (0-based) of the sub-voxel to use for ams and mu.
	// Returns a string containing catalog parameters and contents.
	// Assumes that log-likelihood and Bayesiam prior have been calculated.

	public String test_seed_catalog (
		OEDisc2InitFitInfo fit_info,
		int subvox_index
	) {
		StringBuilder result = new StringBuilder();

		// Create prototype catalog parameters, with statistics zeroed out

		OECatalogParams proto_cat_params = new OECatalogParams();
		proto_cat_params.set_to_fixed_mag (
			0.0,				// a
			0.0,				// p
			0.0,				// c
			0.0,				// b
			0.0,				// alpha
			fit_info.mref,		// mref
			fit_info.msup,		// msup
			0.0,				// tbegin
			fit_info.tint_br	// tend
		);
		proto_cat_params.set_fixed_mag_min (fit_info.mag_min);
		proto_cat_params.set_fixed_mag_max (fit_info.mag_max);

		// Fill the local catalog parameters

		OECatalogParams local_cat_params = new OECatalogParams();
		get_cat_params (
			fit_info,
			proto_cat_params,
			local_cat_params
		);

		result.append (local_cat_params.toString());
		result.append ("\n");

		// Fill the local seed parameters

		OESeedParams local_seed_params = new OESeedParams();
		get_seed_params (
			fit_info,
			subvox_index,
			local_seed_params
		);

		result.append (local_seed_params.toString());
		result.append ("\n");

		// Get the random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Allocate the storage

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Seed communication area

		OECatalogSeedComm comm = new OECatalogSeedComm();
		comm.setup_seed_comm (cat_storage, rangen);

		// Seed the catalog

		OERupture local_rup = new OERupture();

		seed_catalog (
			fit_info,
			subvox_index,
			local_cat_params,
			comm,
			local_rup
		);

		// End the catalog

		comm.cat_builder.end_catalog();

		// Dump it

		result.append (cat_storage.dump_to_string());
	
		return result.toString();
	}




	//----- Marshaling -----




	// Convert a null array into a zero-length array.

	private double[] array_null_to_empty (double[] x) {
		if (x == null) {
			return new double[0];
		}
		return x;
	}




	// Convert an empty array into a null array.

	private double[] array_empty_to_null (double [] x) {
		if (x != null && x.length == 0) {
			return null;
		}
		return x;
	}




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 115001;

	private static final String M_VERSION_NAME = "OEDisc2InitStatVox";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer, IdentityHashMap<Object, Integer> dedup) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			OEValueElement.marshal_obj (writer, "b_velt", b_velt, dedup);
			OEValueElement.marshal_obj (writer, "alpha_velt", alpha_velt, dedup);
			OEValueElement.marshal_obj (writer, "c_velt", c_velt, dedup);
			OEValueElement.marshal_obj (writer, "p_velt", p_velt, dedup);
			OEValueElement.marshal_obj (writer, "n_velt", n_velt, dedup);

			OEValueElement.marshal_array (writer, "a_zams_velt", a_zams_velt, dedup);
			OEValueElement.marshal_array (writer, "a_zmu_velt", a_zmu_velt, dedup);

			writer.marshalDoubleArray ("bay_log_density", array_null_to_empty (bay_log_density));
			writer.marshalDoubleArray ("bay_vox_volume", array_null_to_empty (bay_vox_volume));

			writer.marshalDouble ("ten_aint_q", ten_aint_q);
			writer.marshalDoubleArray ("log_likelihood", array_null_to_empty (log_likelihood));

			writer.marshalDoubleArray ("prod_scnd", array_null_to_empty (prod_scnd));
			writer.marshalDoubleArray ("prod_main", array_null_to_empty (prod_main));
			writer.marshalDoubleArray ("prod_bkgd", array_null_to_empty (prod_bkgd));

			writer.marshalDoubleArray ("unscaled_prod_density_int", array_null_to_empty (unscaled_prod_density_int));
			writer.marshalDoubleArray ("unscaled_prod_density_main_int", array_null_to_empty (unscaled_prod_density_main_int));
			writer.marshalDoubleArray ("unscaled_prod_density_bkgd_int", array_null_to_empty (unscaled_prod_density_bkgd_int));

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader, List<Object> dedup) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			b_velt = OEValueElement.unmarshal_obj (reader, "b_velt", dedup);
			alpha_velt = OEValueElement.unmarshal_obj (reader, "alpha_velt", dedup);
			c_velt = OEValueElement.unmarshal_obj (reader, "c_velt", dedup);
			p_velt = OEValueElement.unmarshal_obj (reader, "p_velt", dedup);
			n_velt = OEValueElement.unmarshal_obj (reader, "n_velt", dedup);

			a_zams_velt = OEValueElement.unmarshal_array (reader, "a_zams_velt", dedup);
			a_zmu_velt = OEValueElement.unmarshal_array (reader, "a_zmu_velt", dedup);

			bay_log_density = array_empty_to_null (reader.unmarshalDoubleArray ("bay_log_density"));
			bay_vox_volume = array_empty_to_null (reader.unmarshalDoubleArray ("bay_vox_volume"));

			ten_aint_q = reader.unmarshalDouble ("ten_aint_q");
			log_likelihood = array_empty_to_null (reader.unmarshalDoubleArray ("log_likelihood"));

			prod_scnd = array_empty_to_null (reader.unmarshalDoubleArray ("prod_scnd"));
			prod_main = array_empty_to_null (reader.unmarshalDoubleArray ("prod_main"));
			prod_bkgd = array_empty_to_null (reader.unmarshalDoubleArray ("prod_bkgd"));

			unscaled_prod_density_int = array_empty_to_null (reader.unmarshalDoubleArray ("unscaled_prod_density_int"));
			unscaled_prod_density_main_int = array_empty_to_null (reader.unmarshalDoubleArray ("unscaled_prod_density_main_int"));
			unscaled_prod_density_bkgd_int = array_empty_to_null (reader.unmarshalDoubleArray ("unscaled_prod_density_bkgd_int"));

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name, IdentityHashMap<Object, Integer> dedup) {
		writer.marshalMapBegin (name);
		do_marshal (writer, dedup);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OEDisc2InitStatVox unmarshal (MarshalReader reader, String name, List<Object> dedup) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader, dedup);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDisc2InitStatVox stat_vox, IdentityHashMap<Object, Integer> dedup) {
		writer.marshalMapBegin (name);
		stat_vox.do_marshal (writer, dedup);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEDisc2InitStatVox static_unmarshal (MarshalReader reader, String name, List<Object> dedup) {
		OEDisc2InitStatVox stat_vox = new OEDisc2InitStatVox();
		reader.unmarshalMapBegin (name);
		stat_vox.do_umarshal (reader, dedup);
		reader.unmarshalMapEnd ();
		return stat_vox;
	}

	// Marshal an array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OEDisc2InitStatVox[] x, IdentityHashMap<Object, Integer> dedup) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			static_marshal (writer, null, x[i], dedup);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects.

	public static OEDisc2InitStatVox[] unmarshal_array (MarshalReader reader, String name, List<Object> dedup) {
		int n = reader.unmarshalArrayBegin (name);
		OEDisc2InitStatVox[] x = new OEDisc2InitStatVox[n];
		for (int i = 0; i < n; ++i) {
			x[i] = static_unmarshal (reader, null, dedup);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}

	// Marshal an array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OEDisc2InitStatVox[] x) {
		IdentityHashMap<Object, Integer> dedup = new IdentityHashMap<Object, Integer>();
		marshal_array (writer, name, x, dedup);
		return;
	}

	// Unmarshal an array of objects.

	public static OEDisc2InitStatVox[] unmarshal_array (MarshalReader reader, String name) {
		List<Object> dedup = new ArrayList<Object>();
		return unmarshal_array (reader, name, dedup);
	}




	//----- Testing -----





}
