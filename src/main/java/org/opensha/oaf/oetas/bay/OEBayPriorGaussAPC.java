package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.OEStatsCalc;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.rj.OAFRegimeParams;

import org.opensha.commons.geo.Location;


// Bayesian prior function, for operational ETAS - Gauss distribution in a, p, and c..
// Author: Michael Barall 08/21/2024.
//
// This function supplies a Bayesian prior density, which is a multivariate Gaussian
// distribution on a, p, and c, combined with a Gaussian distribution on ams.
// Parameters and formulas are given in OEGaussAPCParams, and locatoin-dependent
// parameters are available from a configuration file through OEGaussAPCConfig.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied value elements.

public class OEBayPriorGaussAPC extends OEBayPrior {

	//----- Parameters -----

	// Gaussian parameters.

	private OEGaussAPCParams gauss_params;




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
	public void get_bay_value (
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

		// Get a-value for given branch ratio and Gauss parameter magnitude range

		final double a = OEStatsCalc.calc_inv_branch_ratio (
			n,							// n
			p,							// p
			c,							// c
			b,							// b
			alpha,						// alpha
			gauss_params.get_refMag(),	// mref
			gauss_params.get_maxMag(),	// msup
			bay_params.get_tint_br()	// tint
		);

		// Get the parameter values

		final double zams = zams_velt.get_ve_value();
		final double zmu = ((zmu_velt == null) ? 0.0 : zmu_velt.get_ve_value());

		// If zams is relative, shift it so zams = mean_ams-aValueMean is the peak of the distribution

		double ams;

		if (bay_params.get_relative_zams()) {
			ams = zams + gauss_params.get_aValue_mean();
		}

		// If ams is absolute, convert zams to Gauss parameter magnitude range

		else {
			ams = OEStatsCalc.calc_ams_from_zams (
				zams,						// zams
				b,							// b
				alpha,						// alpha
				gauss_params.get_refMag()	// mref
			);
		}

		// Set the log density

		bay_value.log_density = gauss_params.log_prior_likelihood_ams_a_p_c (ams, a, p, c);

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
	public void get_bay_value (
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
		int k = a_zams_velt.length;
		if (!( a_log_density.length == k && a_vox_volume.length == k && (a_zmu_velt == null || a_zmu_velt.length == k) )) {
			throw new IllegalArgumentException ("OEBayPriorGaussAPC.get_bay_value: Array length mismatch");
		}

		final double stat_vox_volume = common_stat_vox_volume (
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

		// Get a-value for given branch ratio and Gauss parameter magnitude range

		final double a = OEStatsCalc.calc_inv_branch_ratio (
			n,							// n
			p,							// p
			c,							// c
			b,							// b
			alpha,						// alpha
			gauss_params.get_refMag(),	// mref
			gauss_params.get_maxMag(),	// msup
			bay_params.get_tint_br()	// tint
		);

		// Partial log density

		final double stat_vox_log_like = gauss_params.log_prior_likelihood_a_p_c (a, p, c);

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

			final double zams = zams_velt.get_ve_value();
			final double zmu = ((zmu_velt == null) ? 0.0 : zmu_velt.get_ve_value());

			// If zams is relative, shift it so zams = mean_ams-aValueMean is the peak of the distribution

			double ams;

			if (bay_params.get_relative_zams()) {
				ams = zams + gauss_params.get_aValue_mean();
			}

			// If ams is absolute, convert zams to Gauss parameter magnitude range

			else {
				ams = OEStatsCalc.calc_ams_from_zams (
					zams,						// zams
					b,							// b
					alpha,						// alpha
					gauss_params.get_refMag()	// mref
				);
			}

			// Set the log density

			a_log_density[i] = stat_vox_log_like + gauss_params.log_prior_likelihood_ams (ams);
		}

		return;
	}




	//----- Construction -----




	// Default constructor does nothing.

	public OEBayPriorGaussAPC () {}




	// Construct from given parameters.
	// Parameters:
	//  the_params = Gauss a/p/c parameters, or null if none
	// If the_params is null, then default (global-average) parameters are used.
	
	public OEBayPriorGaussAPC (OEGaussAPCParams the_params) {

		// If no parameters, use default

		if (the_params == null) {
			gauss_params = (new OEGaussAPCParams()).copy_from ( (new OEGaussAPCConfig()).get_default_params().params );
		}

		// If we have parameters, copy them

		else {
			gauss_params =  (new OEGaussAPCParams()).copy_from (the_params);
		}
	}




	// Display our contents

	@Override
	public String toString() {
		return "OEBayPriorGaussAPC["
			+ "gauss_params=" + gauss_params.summary_string_2()
			+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 135001;

	private static final String M_VERSION_NAME = "OEBayPriorGaussAPC";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_GAUSSIAN_APC;
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

			OEGaussAPCParams.static_marshal (writer, "gauss_params", gauss_params);

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
			throw new MarshalException ("OEBayPriorGaussAPC.do_umarshal: Unknown version code: version = " + ver);

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			gauss_params = OEGaussAPCParams.static_unmarshal (reader, "gauss_params");

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
	public OEBayPriorGaussAPC unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayPriorGaussAPC obj) {

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

	public static OEBayPriorGaussAPC unmarshal_poly (MarshalReader reader, String name) {
		OEBayPriorGaussAPC result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayPriorGaussAPC.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_GAUSSIAN_APC:
			result = new OEBayPriorGaussAPC();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
