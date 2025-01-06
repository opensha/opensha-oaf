package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.oetas.util.OEArraysCalc;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEOmoriCalc;


// Class to hold parameters for specifying a grid of likelihoods.
// Author: Michael Barall 04/28/2022.

public class OEGridParams {

	//----- Parameter ranges -----

	// The range of Gutenberg-Richter parameter, b-value.

	public OEDiscreteRange b_range;

	// The range of ETAS intensity parameter, alpha-value.
	// Can be null to force alpha == b.

	public OEDiscreteRange alpha_range;

	// The range of Omori c-value.

	public OEDiscreteRange c_range;

	// The range of Omori p-value.

	public OEDiscreteRange p_range;

	// The range of branch ratio, n-value.
	// This controls the productivity of secondary triggering.

	public OEDiscreteRange n_range;

	// The range of mainshock productivity, ams-value, for reference magnitude equal to ZAMS_MREF == 0.0.

	public OEDiscreteRange zams_range;

	// The range of background rate, mu-value, for reference magnitude equal to ZMU_MREF.
	// Can be null to force zmu = 0.0.

	public OEDiscreteRange zmu_range;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		b_range     = null;
		alpha_range = null;
		c_range     = null;
		p_range     = null;
		n_range     = null;
		zams_range  = null;
		zmu_range   = null;

		return;
	}




	// Default constructor.

	public OEGridParams () {
		clear();
	}




	// Constructor that sets up the ranges with the supplied values.
	// Returns this object.
	// Implementation note: Since OEDiscreteRange objects are immutable, we can simply save them.

	public OEGridParams (
		OEDiscreteRange b_range,
		OEDiscreteRange alpha_range,
		OEDiscreteRange c_range,
		OEDiscreteRange p_range,
		OEDiscreteRange n_range,
		OEDiscreteRange zams_range,
		OEDiscreteRange zmu_range
	) {
		this.b_range     = b_range;
		this.alpha_range = alpha_range;
		this.c_range     = c_range;
		this.p_range     = p_range;
		this.n_range     = n_range;
		this.zams_range  = zams_range;
		this.zmu_range   = zmu_range;
	}




	// Set up the ranges with the supplied values.
	// Returns this object.
	// Implementation note: Since OEDiscreteRange objects are immutable, we can simply save them.

	public final OEGridParams set (
		OEDiscreteRange b_range,
		OEDiscreteRange alpha_range,
		OEDiscreteRange c_range,
		OEDiscreteRange p_range,
		OEDiscreteRange n_range,
		OEDiscreteRange zams_range,
		OEDiscreteRange zmu_range
	) {
		this.b_range     = b_range;
		this.alpha_range = alpha_range;
		this.c_range     = c_range;
		this.p_range     = p_range;
		this.n_range     = n_range;
		this.zams_range  = zams_range;
		this.zmu_range   = zmu_range;
		return this;
	}




	// Copy the ranges from the other object.
	// Returns this object.
	// Implementation note: Since OEDiscreteRange objects are immutable, we can simply save them.

	public final OEGridParams copy_from (
		OEGridParams other
	) {
		this.b_range     = other.b_range;
		this.alpha_range = other.alpha_range;
		this.c_range     = other.c_range;
		this.p_range     = other.p_range;
		this.n_range     = other.n_range;
		this.zams_range  = other.zams_range;
		this.zmu_range   = other.zmu_range;
		return this;
	}




	// Convert a range to a string, or "<null>" if the supplied range is null.

	private String range_to_string (OEDiscreteRange range) {
		if (range == null) {
			return "<null>";
		}
		return range.toString();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGridParams:" + "\n");

		result.append ("b_range = "     + range_to_string(b_range)     + "\n");
		result.append ("alpha_range = " + range_to_string(alpha_range) + "\n");
		result.append ("c_range = "     + range_to_string(c_range)     + "\n");
		result.append ("p_range = "     + range_to_string(p_range)     + "\n");
		result.append ("n_range = "     + range_to_string(n_range)     + "\n");
		result.append ("zams_range = "  + range_to_string(zams_range)  + "\n");
		result.append ("zmu_range = "   + range_to_string(zmu_range)   + "\n");

		return result.toString();
	}




	// Set up the ranges with the typical values.
	// Returns this object.

	public final OEGridParams set_typical () {
		this.b_range = OEDiscreteRange.makeSingle (1.0);
		this.alpha_range = null;
		this.c_range = OEDiscreteRange.makeLog (21, 0.00001, 1.00000);
		this.p_range = OEDiscreteRange.makeLinear (37, 0.50, 2.00);
		this.n_range = OEDiscreteRange.makeLog (21, 0.01, 1.00);
		this.zams_range = OEDiscreteRange.makeLinear (81, -4.50, -0.50);
		//this.zmu_range = null;
		this.zmu_range = OEDiscreteRange.makeSingle (0.0);
		return this;
	}




//	//----- Derived range arrays -----
//
//
//
//
//	// Get the array of values of (10^a)*Q, which is the productivity for secondary triggering.
//	// Parameters:
//	//  base_ten_a_q = Value of (10^a)*Q that corresponds to branch ratio == 1.0.
//	// Note: Because branch ratio is proportional to (10^a)*Q, this function
//	// returns n_range.get_range_array() multiplied by base_ten_a_q.
//
//	public final double[] get_ten_a_q_array (double base_ten_a_q) {
//		double[] result = n_range.get_range_array();
//		for (int i = 0; i < result.length; ++i) {
//			result[i] *= base_ten_a_q;
//		}
//		return result;
//	}
//
//
//
//	// Get the array of values of (10^a)*Q, which is the productivity for secondary triggering.
//	// Parameters:
//	//  p = Omori exponent parameter.
//	//  c = Omori offset parameter.
//	//  mref = Reference magnitude = minimum considered magnitude.
//	//  mag_min = Minimum magnitude.
//	//  mag_max = Maximum magnitude.
//	//  tint = Time interval.
//	// This function first calculates the value of (10^a)*Q such that the branch ratio equals 1.0
//	// Because branch ratio is proportional to (10^a)*Q, this function then
//	// returns n_range.get_range_array() multiplied by that value of (10^a)*Q .
//
//	public final double[] get_ten_a_q_array (
//		double p,
//		double c,
//		double mref,
//		double mag_min,
//		double mag_max,
//		double tint
//	) {
//
//		// Get the value of (10^a)*Q that corresponds to branch ratio == 1.0
//
//		final double base_ten_a_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
//			1.0,		// n
//			p,			// p
//			c,			// c
//			b,			// b
//			alpha,		// alpha
//			mref,		// mref
//			mag_min,	// mag_min
//			mag_max,	// mag_max
//			tint		// tint
//		);
//
//		// Now get the array
//
//		return get_ten_a_q_array (base_ten_a_q);
//	}
//
//
//
//
//	// Get the array of values of (10^ams)*Q, which is the productivity for mainshock triggering.
//	// Parameters:
//	//  mref = Reference magnitude.
//	// Note: For ams, we force Q == 1.
//
//	public final double[] get_ten_ams_q_array (double mref) {
//		double[] result = zams_range.get_range_array();
//		final double base_ams = OEStatsCalc.calc_a_new_from_mref_new (
//			0.0,			// a_old
//			b,				// b
//			alpha,			// alpha
//			0.0,			// mref_old
//			mref			// mref_new
//		);
//		for (int i = 0; i < result.length; ++i) {
//			result[i] = Math.pow(10.0, base_ams + result[i]);
//		}
//		return result;
//	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 103001;

	private static final String M_VERSION_NAME = "OEGridParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			OEDiscreteRange.marshal_poly (writer, "b_range"    , b_range    );
			OEDiscreteRange.marshal_poly (writer, "alpha_range", alpha_range);
			OEDiscreteRange.marshal_poly (writer, "c_range"    , c_range    );
			OEDiscreteRange.marshal_poly (writer, "p_range"    , p_range    );
			OEDiscreteRange.marshal_poly (writer, "n_range"    , n_range    );
			OEDiscreteRange.marshal_poly (writer, "zams_range" , zams_range );
			OEDiscreteRange.marshal_poly (writer, "zmu_range"  , zmu_range  );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			b_range     = OEDiscreteRange.unmarshal_poly (reader, "b_range"    );
			alpha_range = OEDiscreteRange.unmarshal_poly (reader, "alpha_range");
			c_range     = OEDiscreteRange.unmarshal_poly (reader, "c_range"    );
			p_range     = OEDiscreteRange.unmarshal_poly (reader, "p_range"    );
			n_range     = OEDiscreteRange.unmarshal_poly (reader, "n_range"    );
			zams_range  = OEDiscreteRange.unmarshal_poly (reader, "zams_range" );
			zmu_range   = OEDiscreteRange.unmarshal_poly (reader, "zmu_range"  );

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OEGridParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEGridParams params) {
		writer.marshalMapBegin (name);
		params.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEGridParams static_unmarshal (MarshalReader reader, String name) {
		OEGridParams params = new OEGridParams();
		reader.unmarshalMapBegin (name);
		params.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return params;
	}




	//----- Testing -----





}
