package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.OEConstants;

import  org.opensha.oaf.oetas.util.OEMarginalDistSetBuilder;
import  org.opensha.oaf.oetas.util.OEMarginalDistSet;


// Voxel statistics accumulator that calculates a set of marginal distributions.
// Author: Michael Barall 08/12/2024.

public class OEDisc2VoxStatAccumMarginal implements OEDisc2VoxStatAccum {


	//----- Marginal distribution -----

	// The grid parameters.

	private OEGridParams grid_params;

	// True to create a full distribution (with bivariate marginal), false for a slim distribution (for PDL).

	private boolean f_full;

	// True to include bins for out-of-range variable values.

	private boolean f_out;

	// True to include a second set of data to accumulate marginals for the sub-critical part of parameter space.

	private boolean f_dual;


	//----- Data -----

	// Active bayesian weight.

	private double act_bay_weight;

	// Maximum log density for the active, generic, sequence-specific, and Bayesian models.

	private double act_max_log_density;
	private double gen_max_log_density;
	private double seq_max_log_density;
	private double bay_max_log_density;

	// The marginal distribution set builder.

	private OEMarginalDistSetBuilder dist_set_builder;

	// True if the current voxel is super-critical.

	private boolean f_vox_super_critical;




	//----- Operations -----




	// Get the log-density.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  bay_log_density = Bayesian prior log density.
	//  log_likelihood = Log likelihood density from parameter fitting.

	public static double get_log_density (
		double bay_weight,
		double bay_log_density,
		double log_likelihood
	) {
		return (
			(bay_weight <= 1.0)
			? ((bay_log_density * bay_weight) + log_likelihood)
			: (bay_log_density + (log_likelihood * (2.0 - bay_weight)))
		);
	}




	// Get the probability.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  bay_log_density = Bayesian prior log density.
	//  bay_vox_volume = Voxel volume, from Bayesian prior.
	//  log_likelihood = Log likelihood density from parameter fitting.
	//  max_log_density = Maximum log-density.

	public static double get_probability (
		double bay_weight,
		double bay_log_density,
		double bay_vox_volume,
		double log_likelihood,
		double max_log_density
	) {
		final double norm_log_density = get_log_density (bay_weight, bay_log_density, log_likelihood) - max_log_density;
		return Math.exp(norm_log_density) * bay_vox_volume;
	}




	//----- Construction and access -----




	// Constructor.
	// Parameters:
	//  grid_params = Definition of the parameters.
	//  f_full = True to create a full distribution (with bivariate marginal), false for a slim distribution (for PDL).
	//  f_out = True to include bins for out-of-range variable values.
	// Note: This function does not retain the grid_params object.

	public OEDisc2VoxStatAccumMarginal (OEGridParams grid_params, boolean f_full, boolean f_out) {

		// Save the parameters

		this.grid_params = (new OEGridParams()).copy_from (grid_params);
		this.f_full = f_full;
		this.f_out = f_out;

		if (f_full && grid_params.n_range.get_range_max() > OEConstants.MAX_MARGINAL_BR_SUB_CRITICAL) {
			this.f_dual = true;
		} else {
			this.f_dual = false;
		}

		// Clear data

		act_bay_weight = 0.0;
		act_max_log_density = 0.0;
		gen_max_log_density = 0.0;
		seq_max_log_density = 0.0;
		bay_max_log_density = 0.0;

		dist_set_builder = null;

		f_vox_super_critical = false;
	}




	// Get the marginal distribution set.
	// This should not be called until after vsaccum_end().

	public final OEMarginalDistSet get_dist_set () {
		return dist_set_builder.get_dist_set();
	}




	//----- Accumulation functions -----




	// Begin accumulating statistics.
	// Parameters:
	//  act_bay_weight = Active Bayesian weight.
	//  act_max_log_density = Maximum log density over all sub-voxels, for the active model.
	//  gen_max_log_density = Maximum log density over all sub-voxels, for the generic model.
	//  seq_max_log_density = Maximum log density over all sub-voxels, for the sequence-specific model.
	//  bay_max_log_density = Maximum log density over all sub-voxels, for the Bayesian model.

	@Override
	public void vsaccum_begin (
		double act_bay_weight,
		double act_max_log_density,
		double gen_max_log_density,
		double seq_max_log_density,
		double bay_max_log_density
	) {
		// Save Parameters

		this.act_bay_weight = act_bay_weight;

		this.act_max_log_density = act_max_log_density;
		this.gen_max_log_density = gen_max_log_density;
		this.seq_max_log_density = seq_max_log_density;
		this.bay_max_log_density = bay_max_log_density;

		// Make the set builder and prepare it to receive data

		dist_set_builder = new OEMarginalDistSetBuilder();
		dist_set_builder.add_etas_vars (grid_params, f_out);
		if (f_full) {
			if (f_dual) {
				dist_set_builder.add_etas_data_gen_seq_bay_act_dual();
			} else {
				dist_set_builder.add_etas_data_gen_seq_bay_act();
			}
			dist_set_builder.begin_accum (true);
		} else {
			dist_set_builder.add_etas_data_prob();
			dist_set_builder.begin_accum (false);
		}

		return;
	}




	// Set the values of b, alpha, c, p, n.
	// These values are to be stored, and used with subsequent calls to vsaccum_add_data().

	@Override
	public void vsaccum_set_b_alpha_c_p_n (
		double b,
		double alpha,
		double c,
		double p,
		double n
	) {
		// Pass it into the builder

		dist_set_builder.set_etas_var_b_alpha_c_p_n (
			b,
			alpha,
			c,
			p,
			n
		);

		// Remember if this is super-critical in a dual marginal

		if (f_dual && n > OEConstants.MAX_MARGINAL_BR_SUB_CRITICAL) {
			f_vox_super_critical = true;
		} else {
			f_vox_super_critical = false;
		}

		return;
	}




	// Add data to the accumulation.
	// Parameters:
	//  zams = Value of zams.
	//  zmu = Value of zmu (background rate).
	//  bay_log_density = Bayesian prior log density.
	//  bay_vox_volume = Voxel volume, from Bayesian prior.
	//  log_likelihood = Log likelihood density from parameter fitting.

	@Override
	public void vsaccum_add_data (
		double zams,
		double zmu,
		double bay_log_density,
		double bay_vox_volume,
		double log_likelihood
	) {
		// Pass variables into the builder

		dist_set_builder.set_etas_var_zams_zmu (
			zams,
			zmu
		);

		// Make data for full marginals

		if (f_full) {

			// Calculate probabilities

			double gen = get_probability (
				OEConstants.BAY_WT_GENERIC,
				bay_log_density,
				bay_vox_volume,
				log_likelihood,
				gen_max_log_density
			);

			double seq = get_probability (
				OEConstants.BAY_WT_SEQ_SPEC,
				bay_log_density,
				bay_vox_volume,
				log_likelihood,
				seq_max_log_density
			);

			double bay = get_probability (
				OEConstants.BAY_WT_BAYESIAN,
				bay_log_density,
				bay_vox_volume,
				log_likelihood,
				bay_max_log_density
			);

			double act = get_probability (
				act_bay_weight,
				bay_log_density,
				bay_vox_volume,
				log_likelihood,
				act_max_log_density
			);

			// Pass data into the builder

			if (f_dual) {

				dist_set_builder.set_etas_data_gen_seq_bay_act_dual (
					gen,
					seq,
					bay,
					act,
					f_vox_super_critical ? 0.0 : gen,
					f_vox_super_critical ? 0.0 : seq,
					f_vox_super_critical ? 0.0 : bay,
					f_vox_super_critical ? 0.0 : act
				);

			} else {

				dist_set_builder.set_etas_data_gen_seq_bay_act (
					gen,
					seq,
					bay,
					act
				);

			}
		}

		// Make data for slim marginals

		else {

			// Calculate probability

			double prob = get_probability (
				act_bay_weight,
				bay_log_density,
				bay_vox_volume,
				log_likelihood,
				act_max_log_density
			);

			// Pass data into the builder

			dist_set_builder.set_etas_data_prob (
				prob
			);
		}

		// Accumulate

		dist_set_builder.accum();
		return;
	}




	// End accumulating statistics.

	@Override
	public void vsaccum_end () {

		// End accumulation in the distribution set builder

		dist_set_builder.end_etas_accum();
		return;
	}

}
