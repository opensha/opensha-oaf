package org.opensha.oaf.oetas.fit;


// Object that accumulates statistical information about voxels.
// Author: Michael Barall 08/12/2024.

public interface OEDisc2VoxStatAccum {


	//----- Accumulation functions -----




	// Begin accumulating statistics.
	// Parameters:
	//  act_bay_weight = Active Bayesian weight.
	//  act_max_log_density = Maximum log density over all sub-voxels, for the active model.
	//  gen_max_log_density = Maximum log density over all sub-voxels, for the generic model.
	//  seq_max_log_density = Maximum log density over all sub-voxels, for the sequence-specific model.
	//  bay_max_log_density = Maximum log density over all sub-voxels, for the Bayesian model.

	public void vsaccum_begin (
		double act_bay_weight,
		double act_max_log_density,
		double gen_max_log_density,
		double seq_max_log_density,
		double bay_max_log_density
	);




	// Set the values of b, alpha, c, p, n.
	// These values are to be stored, and used with subsequent calls to vsaccum_add_data().

	public void vsaccum_set_b_alpha_c_p_n (
		double b,
		double alpha,
		double c,
		double p,
		double n
	);




	// Add data to the accumulation.
	// Parameters:
	//  zams = Value of zams.
	//  zmu = Value of zmu (background rate).
	//  bay_log_density = Bayesian prior log density.
	//  bay_vox_volume = Voxel volume, from Bayesian prior.
	//  log_likelihood = Log likelihood density from parameter fitting.

	public void vsaccum_add_data (
		double zams,
		double zmu,
		double bay_log_density,
		double bay_vox_volume,
		double log_likelihood
	);




	// End accumulating statistics.

	public void vsaccum_end ();

}
