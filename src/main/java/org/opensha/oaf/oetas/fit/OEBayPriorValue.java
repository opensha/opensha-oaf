package org.opensha.oaf.oetas.fit;


// Class to hold the value of a Bayesian prior.
// Author: Michael Barall 02/21/2023.
//
// A Beyesian prior value is evalueted for e given voxel in parameter space.
// The probability of parameters lying in the voxel is:
//
//   exp(log_density) * vox_volume
//
// The value of log_density is combined with the likelihood value,
// and then the probability of parameters lying in the voxel is:
//
//   exp(log_likelihood + log_density) * vox_volume
//
// There is a tradeoff between log_density and vox_volume.
// The value of log_likelihood + log_density is used to identify the tail of
// the distribution, that is, the voxels that can be trimmed.

public class OEBayPriorValue {

	//----- Value -----

	// Log of the probability density.
	// Note: Probability density need not be normalized.

	public double log_density;

	// Volume of the voxel in parameter space.

	public double vox_volume;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {
		log_density = 0.0;
		vox_volume  = 0.0;
		return;
	}




	// Default constructor.

	public OEBayPriorValue () {
		clear();
	}




	// Constructor that sets the supplied values.

	public OEBayPriorValue (
		double log_density,
		double vox_volume
	) {
		this.log_density = log_density;
		this.vox_volume  = vox_volume;
	}




	// Set  the supplied values.
	// Returns this object.

	public final OEBayPriorValue set (
		double log_density,
		double vox_volume
	) {
		this.log_density = log_density;
		this.vox_volume  = vox_volume;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEBayPriorValue:" + "\n");

		result.append ("log_density = " + log_density + "\n");
		result.append ("vox_volume = "  + vox_volume  + "\n");

		return result.toString();
	}

}
