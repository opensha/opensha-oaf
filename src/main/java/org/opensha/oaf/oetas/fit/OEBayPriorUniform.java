package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Bayesian prior function, for operational ETAS - Uniform.
// Author: Michael Barall 02/21/2023.
//
// This function supplies a uniform Bayesian prior density.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied value elements.

public class OEBayPriorUniform extends OEBayPrior {

	//----- Parameters -----

	// <None>




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

		// Set the log density

		bay_value.log_density = 0.0;

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
			throw new IllegalArgumentException ("OEBayPriorUniform.get_bay_value: Array length mismatch");
		}

		double stat_vox_volume = common_stat_vox_volume (
			b_velt,
			alpha_velt,
			c_velt,
			p_velt,
			n_velt
		);

		OEValueElement zams_velt = null;
		OEValueElement zmu_velt = null;

		// Loop over each (zams, zmu) pair

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

			a_log_density[i] = 0.0;
		}

		return;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEBayPriorUniform () {}


	//// Construct from given parameters.
	//
	//public OEBayPriorUniform () {
	//}


	// Display our contents

	@Override
	public String toString() {
		return "OEBayPriorUniform";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 112001;

	private static final String M_VERSION_NAME = "OEBayPriorUniform";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_UNIFORM;
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
			throw new MarshalException ("OEBayPriorUniform.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			// <None>

		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			// <None>

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
	public OEBayPriorUniform unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayPriorUniform obj) {

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

	public static OEBayPriorUniform unmarshal_poly (MarshalReader reader, String name) {
		OEBayPriorUniform result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayPriorUniform.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_UNIFORM:
			result = new OEBayPriorUniform();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
