package org.opensha.oaf.oetas.ver;


// Abstraction of an object that can calculate log-likelihood, for fitting verification.
// Author: Michael Barall.

public interface OEVerFitLogLikeCalc {


	// Set the voxel in which to operate.
	// A voxel is defined by values of b, alpha, c, p, and n.

	public void ver_set_voxel (
		double b,
		double alpha,
		double c,
		double p,
		double n
	);


	// Calculate the log-likelihood for a subvoxel.

	public double ver_subvox_log_like (
		double zams,
		double zmu
	);


}
