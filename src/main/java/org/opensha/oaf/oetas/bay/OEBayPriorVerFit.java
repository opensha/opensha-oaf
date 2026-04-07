package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.oetas.fit.OEDisc2ExtFit;

import org.opensha.oaf.oetas.ver.OEVerFitLogLikeCalc;
import org.opensha.oaf.oetas.ver.OEVerFitAccum;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Bayesian prior function, for operational ETAS - Verification test for parameter fitting.
// Author: Michael Barall.
//
// This function supplies a prior that is the log-likelihood density
// computed using separate parameter fitting verification code.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied value elements.

public class OEBayPriorVerFit extends OEBayPrior {

	//----- Parameters -----

	// Object to calculate log-likelihood for fitting verification.
	// Note: This object is not thread-safe and so calls must be synchronized.

	private OEVerFitLogLikeCalc log_like_calc = null;




	//----- Evaluation -----


	// Calculate the value of the Bayesian prior function.
	// Parameters:
	//  bay_params = Parameters passed in to the Bayesian prior.
	//  bay_value = Receives the Bayesian prior value.
	//  b_velt = Gutenberg-Richter value and element.
	//  alpha_velt = ETAS intensity value and element, can be null to force alpha == b.
	//  c_velt = Omori c-value and element.
	//  p_velt = Omori p-value and element.
	//  n_velt = Branch ratio value and element.
	//  zams_velt = Mainshock productivity value and element, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  zmu_velt = Background rate value and element, assuming reference magnitude equal to ZMU_MREF, can be null to force zmu == 0.
	// Threading: This function may be called simultaneously by multiple threads.

	@Override
	public synchronized void get_bay_value (
		OEBayPriorParams bay_params,
		OEBayPriorValue bay_value,
		OEValueElement b_velt,
		OEValueElement alpha_velt,
		OEValueElement c_velt,
		OEValueElement p_velt,
		OEValueElement n_velt,
		OEValueElement zams_velt,
		OEValueElement zmu_velt
	) {
		// Check we have been configured

		if (!( log_like_calc != null )) {
			throw new IllegalStateException ("OEBayPriorVerFit.get_bay_value: Prior is not configured");
		}

		// Set the voxel volume

		bay_value.vox_volume = common_vox_volume (
			b_velt,
			alpha_velt,
			c_velt,
			p_velt,
			n_velt,
			zams_velt,
			zmu_velt
		);

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double c = c_velt.get_ve_value();
		final double p = p_velt.get_ve_value();
		final double n = n_velt.get_ve_value();

		// Set the voxel for likelihood calculation

		log_like_calc.ver_set_voxel (
			b,
			alpha,
			c,
			p,
			n
		);

		// Get the parameter values
		// It is assumed that log_like_calc knows if zams is absolute or relative.

		final double zams = zams_velt.get_ve_value();
		final double zmu = ((zmu_velt == null) ? 0.0 : zmu_velt.get_ve_value());

		// Set the log density

		bay_value.log_density = log_like_calc.ver_subvox_log_like (
			zams,
			zmu
		);

		return;
	}




	// Calculate the value of the Bayesian prior function, for a range of (zams, zmu) values.
	// Parameters:
	//  bay_params = Parameters passed in to the Bayesian prior.
	//  a_log_density = Array to receive log of the probability density, for each (zams, zmu) pair.
	//  a_vox_volume = Array to receive volume of the voxel in parameter space, for each (zams, zmu) pair.
	//  b_velt = Gutenberg-Richter value and element.
	//  alpha_velt = ETAS intensity value and element, can be null to force alpha == b.
	//  c_velt = Omori c-value and element.
	//  p_velt = Omori p-value and element.
	//  n_velt = Branch ratio value and element.
	//  a_zams_velt = Array of mainshock productivity value and element, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  a_zmu_velt = Array of background rate value and element, assuming reference magnitude equal to ZMU_MREF, can be null to force zmu == 0.
	// Threading: This function may be called simultaneously by multiple threads.
	// Note: All arrays must have the same length.
	// Note: The default implementation calls the function above, but subclasses may be
	// more efficient by implementing this directly.

	@Override
	public synchronized void get_bay_value (
		OEBayPriorParams bay_params,
		double[] a_log_density,
		double[] a_vox_volume,
		OEValueElement b_velt,
		OEValueElement alpha_velt,
		OEValueElement c_velt,
		OEValueElement p_velt,
		OEValueElement n_velt,
		OEValueElement[] a_zams_velt,
		OEValueElement[] a_zmu_velt
	) {
		// Check we have been configured

		if (!( log_like_calc != null )) {
			throw new IllegalStateException ("OEBayPriorVerFit.get_bay_value: Prior is not configured");
		}

		// Check consistent array lengths

		int k = a_zams_velt.length;
		if (!( a_log_density.length == k && a_vox_volume.length == k && (a_zmu_velt == null || a_zmu_velt.length == k) )) {
			throw new IllegalArgumentException ("OEBayPriorVerFit.get_bay_value: Array length mismatch");
		}

		// Get voxel volume

		double stat_vox_volume = common_stat_vox_volume (
			b_velt,
			alpha_velt,
			c_velt,
			p_velt,
			n_velt
		);

		// Get the parameter values

		final double b = b_velt.get_ve_value();
		final double alpha = ((alpha_velt == null) ? b : alpha_velt.get_ve_value());
		final double c = c_velt.get_ve_value();
		final double p = p_velt.get_ve_value();
		final double n = n_velt.get_ve_value();

		// Set the voxel for likelihood calculation

		log_like_calc.ver_set_voxel (
			b,
			alpha,
			c,
			p,
			n
		);

		// Loop over each (zams, zmu) pair

		OEValueElement zams_velt = null;
		OEValueElement zmu_velt = null;

		for (int i = 0; i < k; ++i) {
			zams_velt = a_zams_velt[i];
			if (a_zmu_velt != null) {
				zmu_velt = a_zmu_velt[i];
			}

			a_vox_volume[i] = common_vox_volume (
				stat_vox_volume,
				zams_velt,
				zmu_velt
			);

			// Get the parameter values
			// It is assumed that log_like_calc knows if zams is absolute or relative.

			final double zams = zams_velt.get_ve_value();
			final double zmu = ((zmu_velt == null) ? 0.0 : zmu_velt.get_ve_value());

			// Set the log density

			a_log_density[i] = log_like_calc.ver_subvox_log_like (
				zams,
				zmu
			);
		}

		return;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEBayPriorVerFit () {
		log_like_calc = null;
	}


	//// Construct from given parameters.
	//
	//public OEBayPriorVerFit () {
	//}


	// Display our contents

	@Override
	public String toString() {
		return "OEBayPriorVerFit";
	}


	// Configure the prior, by supplying the verification fitter.

	public synchronized void config_prior (OEVerFitLogLikeCalc log_like_calc) {
		if (log_like_calc == null) {
			throw new IllegalArgumentException ("OEBayPriorVerFit.config_prior: No log-likelihood calculator supplied");
		}
		if (this.log_like_calc != null) {
			throw new IllegalStateException ("OEBayPriorVerFit.config_prior: Prior is already configured");
		}

		this.log_like_calc = log_like_calc;

		// Output message saying that we are configured

		System.out.println ();
		System.out.println ("OEBayPriorVerFit has been configured.");
		System.out.println (log_like_calc.toString());

		return;
	}


	// Configure the prior, by supplying the parameter fitter.

	public void config_prior (OEDisc2ExtFit fitter) {
		OEVerFitAccum fit_accum = new OEVerFitAccum();
		fit_accum.set_config (fitter);
		config_prior (fit_accum);
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 152001;

	private static final String M_VERSION_NAME = "OEBayPriorVerFit";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_VER_FIT;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			// <None>

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("OEBayPriorVerFit.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			log_like_calc = null;

		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			log_like_calc = null;

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
	public OEBayPriorVerFit unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayPriorVerFit obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static OEBayPriorVerFit unmarshal_poly (MarshalReader reader, String name) {
		OEBayPriorVerFit result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayPriorVerFit.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_VER_FIT:
			result = new OEBayPriorVerFit();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
