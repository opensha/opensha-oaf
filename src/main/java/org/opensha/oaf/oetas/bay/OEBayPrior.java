package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.commons.geo.Location;


// Bayesian prior function, for operational ETAS.
// Author: Michael Barall 02/21/2023.
//
// This abstract class represents a Bayesian prior function.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied value elements.

public abstract class OEBayPrior implements Marshalable {

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

	public abstract void get_bay_value (
		OEBayPriorParams bay_params,
		OEBayPriorValue bay_value,
		OEValueElement b_velt,
		OEValueElement alpha_velt,
		OEValueElement c_velt,
		OEValueElement p_velt,
		OEValueElement n_velt,
		OEValueElement zams_velt,
		OEValueElement zmu_velt
	);




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
			throw new IllegalArgumentException ("OEBayPrior.get_bay_value: Array length mismatch");
		}

		OEBayPriorValue bay_value = new OEBayPriorValue();
		OEValueElement zams_velt = null;
		OEValueElement zmu_velt = null;

		// Call single-voxel version for each (zams, zmu) pair

		for (int i = 0; i < k; ++i) {
			zams_velt = a_zams_velt[i];
			if (a_zmu_velt != null) {
				zmu_velt = a_zmu_velt[i];
			}
			get_bay_value (
				bay_params,
				bay_value,
				b_velt,
				alpha_velt,
				c_velt,
				p_velt,
				n_velt,
				zams_velt,
				zmu_velt
			);
			a_log_density[i] = bay_value.log_density;
			a_vox_volume[i] = bay_value.vox_volume;
		}

		return;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEBayPrior () {}


	// Display our contents

	@Override
	public String toString() {
		return "OEBayPrior";
	}




	//----- Services to subclasses -----




	// Calculate the voxel volume, in a standard way.
	// Parameters:
	//  b_velt = Gutenberg-Richter value and element.
	//  alpha_velt = ETAS intensity value and element, can be null to force alpha == b.
	//  c_velt = Omori c-value and element.
	//  p_velt = Omori p-value and element.
	//  n_velt = Branch ratio value and element.
	//  zams_velt = Mainshock productivity value and element, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  zmu_velt = Background rate value and element, assuming reference magnitude equal to ZMU_MREF, can be null to force zmu == 0.
	// Returns the voxel volume.
	// Threading: This function may be called simultaneously by multiple threads.

	protected double common_vox_volume (
		OEValueElement b_velt,
		OEValueElement alpha_velt,
		OEValueElement c_velt,
		OEValueElement p_velt,
		OEValueElement n_velt,
		OEValueElement zams_velt,
		OEValueElement zmu_velt
	) {

		// Calculate factors for each element

		final double b_factor = b_velt.get_width (1.0);
		final double alpha_factor = ((alpha_velt == null) ? 1.0 : (alpha_velt.get_width (1.0)));
		final double c_factor = c_velt.get_log_ratio (1.0);
		final double p_factor = p_velt.get_width (1.0);
		final double n_factor = n_velt.get_log_ratio (1.0);
		final double zams_factor = ((zams_velt == null) ? 1.0 : (zams_velt.get_width (1.0)));
		final double zmu_factor = ((zmu_velt == null) ? 1.0 : (zmu_velt.get_log_ratio (1.0)));

		// Combine

		return b_factor * alpha_factor * c_factor * p_factor * n_factor * zams_factor * zmu_factor;
	}




	// Calculate the statistics voxel volume (the volume due to b, alpha, c, p, and n), in a standard way.
	// Parameters:
	//  b_velt = Gutenberg-Richter value and element.
	//  alpha_velt = ETAS intensity value and element, can be null to force alpha == b.
	//  c_velt = Omori c-value and element.
	//  p_velt = Omori p-value and element.
	//  n_velt = Branch ratio value and element.
	// Returns the statistics voxel volume.
	// Threading: This function may be called simultaneously by multiple threads.

	protected double common_stat_vox_volume (
		OEValueElement b_velt,
		OEValueElement alpha_velt,
		OEValueElement c_velt,
		OEValueElement p_velt,
		OEValueElement n_velt
	) {

		// Calculate factors for each element

		final double b_factor = b_velt.get_width (1.0);
		final double alpha_factor = ((alpha_velt == null) ? 1.0 : (alpha_velt.get_width (1.0)));
		final double c_factor = c_velt.get_log_ratio (1.0);
		final double p_factor = p_velt.get_width (1.0);
		final double n_factor = n_velt.get_log_ratio (1.0);

		// Combine

		return b_factor * alpha_factor * c_factor * p_factor * n_factor;
	}




	// Calculate the voxel volume, in a standard way.
	// Parameters:
	//  stat_vox_volume = Statistics voxel volume (the volume due to b, alpha, c, p, and n).
	//  zams_velt = Mainshock productivity value and element, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  zmu_velt = Background rate value and element, assuming reference magnitude equal to ZMU_MREF, can be null to force zmu == 0.
	// Returns the voxel volume.
	// Threading: This function may be called simultaneously by multiple threads.

	protected double common_vox_volume (
		double stat_vox_volume,
		OEValueElement zams_velt,
		OEValueElement zmu_velt
	) {

		// Calculate factors for each element

		final double zams_factor = ((zams_velt == null) ? 1.0 : (zams_velt.get_width (1.0)));
		final double zmu_factor = ((zmu_velt == null) ? 1.0 : (zmu_velt.get_log_ratio (1.0)));

		// Combine

		return stat_vox_volume * zams_factor * zmu_factor;
	}




	//----- Factory methods -----


	// Construct a function with constant density.

	public static OEBayPrior makeUniform () {
		return new OEBayPriorUniform ();
	}


	// Construct a function with normal density.
	// This function is a placeholder.

	public static OEBayPrior makeNormal () {
		return new OEBayPriorNormal ();
	}


	// Construct a function for Gauss distribution in a, p, and c.

	public static OEBayPrior makeGaussAPC (OEGaussAPCParams the_params) {
		return new OEBayPriorGaussAPC (the_params);
	}


	// Construct a function for mixed distribution in relative-ams, n, p, and c.

	public static OEBayPrior makeMixedRNPC (OEMixedRNPCParams the_params) {
		return new OEBayPriorMixedRNPC (the_params);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 111001;

	private static final String M_VERSION_NAME = "OEBayPrior";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 111000;
	protected static final int MARSHAL_UNIFORM = 112001;
	protected static final int MARSHAL_NORMAL = 113001;
	protected static final int MARSHAL_GAUSSIAN_APC = 135001;
	protected static final int MARSHAL_MIXED_RNPC = 147001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected abstract int get_marshal_type ();

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		// <None>
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		// <None>

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
	public OEBayPrior unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayPrior obj) {

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

	public static OEBayPrior unmarshal_poly (MarshalReader reader, String name) {
		OEBayPrior result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayPrior.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_UNIFORM:
			result = new OEBayPriorUniform();
			result.do_umarshal (reader);
			break;

		case MARSHAL_NORMAL:
			result = new OEBayPriorNormal();
			result.do_umarshal (reader);
			break;

		case MARSHAL_GAUSSIAN_APC:
			result = new OEBayPriorGaussAPC();
			result.do_umarshal (reader);
			break;

		case MARSHAL_MIXED_RNPC:
			result = new OEBayPriorMixedRNPC();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
