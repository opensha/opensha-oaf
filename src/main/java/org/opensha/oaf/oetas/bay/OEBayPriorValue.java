package org.opensha.oaf.oetas.bay;


// Class to hold the value of a Bayesian prior.
// Author: Michael Barall 02/21/2023.
//
// A Beyesian prior value is evaluated for a given voxel in parameter space.
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
//
// A further refinement is to compute the probability as:
//
//   exp(log_likelihood * min(1, 2 - bay_weight) + log_density * min(1, bay_weight) - max_density) * vox_volume
//
// The constant bay_weight determines the weight given to the prior;
// bay_weight == 1 is the usual Bayesian probability, while bay_weight == 0
// yields a sequence-specific probability, and bay_weight == 2 yields
// a generic probability.  The constant max_density is
// chosen so that the arguement to the exp() function is non-positive;
// this prevents overflows, as the values of log_likelihood and log_density
// can be large and positive.

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
