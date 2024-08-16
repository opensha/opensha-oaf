package org.opensha.oaf.oetas.fit;

import java.util.List;
import java.util.ArrayList;


// Voxel statistics accumulator that passes each call to a list of accumulators.
// Author: Michael Barall 08/12/2024.

public class OEDisc2VoxStatAccumMulti implements OEDisc2VoxStatAccum {


	//----- Targets -----

	// The list of statistics accumulators which are our targets.

	private OEDisc2VoxStatAccum[] stat_accums;




	//----- Construction -----




	// Constructor supplies the list of statistics accumulators.
	// Any null values supplied are ignored.

	public OEDisc2VoxStatAccumMulti (OEDisc2VoxStatAccum... accums) {
		List<OEDisc2VoxStatAccum> accum_list = new ArrayList<OEDisc2VoxStatAccum>();
		if (accums != null) {
			for (OEDisc2VoxStatAccum accum : accums) {
				if (accum != null) {
					accum_list.add (accum);
				}
			}
		}
		stat_accums = accum_list.toArray (new OEDisc2VoxStatAccum[0]);
	}




	// If there is eactly one target, return it.
	// If there are no targets, return a null accumulator.
	// Otherwise, return this object.
	// Note: This can be used to avoid function forwarding when it is not necessary.

	public OEDisc2VoxStatAccum get_stat_accum () {
		if (stat_accums.length == 1) {
			return stat_accums[0];
		}
		if (stat_accums.length == 0) {
			return new OEDisc2VoxStatAccumNull();
		}
		return this;
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
		for (OEDisc2VoxStatAccum accum : stat_accums) {
			accum.vsaccum_begin (
				act_bay_weight,
				act_max_log_density,
				gen_max_log_density,
				seq_max_log_density,
				bay_max_log_density
			);
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
		for (OEDisc2VoxStatAccum accum : stat_accums) {
			accum.vsaccum_set_b_alpha_c_p_n (
				b,
				alpha,
				c,
				p,
				n
			);
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
		for (OEDisc2VoxStatAccum accum : stat_accums) {
			accum.vsaccum_add_data (
				zams,
				zmu,
				bay_log_density,
				bay_vox_volume,
				log_likelihood
			);
		}
		return;
	}




	// End accumulating statistics.

	@Override
	public void vsaccum_end () {
		for (OEDisc2VoxStatAccum accum : stat_accums) {
			accum.vsaccum_end ();
		}
		return;
	}

}
