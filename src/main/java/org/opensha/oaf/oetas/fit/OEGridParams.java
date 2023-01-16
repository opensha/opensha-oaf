package org.opensha.oaf.oetas.fit;

import java.util.Arrays;

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

	// Gutenberg-Richter parameter (a single fixed value).

	public double b;

	// ETAS intensity parameter (a single fixed value).

	public double alpha;

	// The range of c-values.

	public OEDiscreteRange c_range;

	// The range of p-values.

	public OEDiscreteRange p_range;

	// The range of branch ratios.
	// This controls the productivity of secondary triggering.

	public OEDiscreteRange br_range;

	// The range of mainshock productivity values, assuming zero reference magnitude.

	public OEDiscreteRange zams_range;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		b               = 0.0;
		alpha           = 0.0;

		c_range         = null;
		p_range         = null;
		br_range        = null;
		zams_range      = null;

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
		double b,
		double alpha,
		OEDiscreteRange c_range,
		OEDiscreteRange p_range,
		OEDiscreteRange br_range,
		OEDiscreteRange zams_range
	) {
		this.b          = b;
		this.alpha      = alpha;
		this.c_range    = c_range;
		this.p_range    = p_range;
		this.br_range   = br_range;
		this.zams_range = zams_range;
	}




	// Set up the ranges with the supplied values.
	// Returns this object.
	// Implementation note: Since OEDiscreteRange objects are immutable, we can simply save them.

	public final OEGridParams set (
		double b,
		double alpha,
		OEDiscreteRange c_range,
		OEDiscreteRange p_range,
		OEDiscreteRange br_range,
		OEDiscreteRange zams_range
	) {
		this.b          = b;
		this.alpha      = alpha;
		this.c_range    = c_range;
		this.p_range    = p_range;
		this.br_range   = br_range;
		this.zams_range = zams_range;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGridParams:" + "\n");

		result.append ("b = "             + b                     + "\n");
		result.append ("alpha = "         + alpha                 + "\n");

		result.append ("c_range = "       + c_range.toString()    + "\n");
		result.append ("p_range = "       + p_range.toString()    + "\n");
		result.append ("br_range = "      + br_range.toString()   + "\n");
		result.append ("zams_range = "    + zams_range.toString() + "\n");

		return result.toString();
	}




	// Set up the ranges with the typical values.
	// Returns this object.

	public final OEGridParams set_typical () {
		this.b = 1.0;
		this.alpha = 1.0;
		this.c_range = OEDiscreteRange.makeLog (21, 0.00001, 1.00000);
		this.p_range = OEDiscreteRange.makeLinear (31, 0.70, 1.30);
		this.br_range = OEDiscreteRange.makeLog (21, 0.01, 1.00);
		this.zams_range = OEDiscreteRange.makeLog (41, 0.01, 100.00);
		return this;
	}




	//----- Derived range arrays -----




	// Get the array of values of (10^a)*Q, which is the productivity for secondary triggering.
	// Parameters:
	//  base_ten_a_q = Value of (10^a)*Q that corresponds to branch ratio == 1.0.
	// Note: Because branch ratio is proportional to (10^a)*Q, this function
	// returns br_range.get_range_array() multiplied by base_ten_a_q.

	public final double[] get_ten_a_q_array (double base_ten_a_q) {
		double[] result = br_range.get_range_array();
		for (int i = 0; i < result.length; ++i) {
			result[i] *= base_ten_a_q;
		}
		return result;
	}



	// Get the array of values of (10^a)*Q, which is the productivity for secondary triggering.
	// Parameters:
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  mag_min = Minimum magnitude.
	//  mag_max = Maximum magnitude.
	//  tint = Time interval.
	// This function first calculates the value of (10^a)*Q such that the branch ratio equals 1.0
	// Because branch ratio is proportional to (10^a)*Q, this function then
	// returns br_range.get_range_array() multiplied by that value of (10^a)*Q .

	public final double[] get_ten_a_q_array (
		double p,
		double c,
		double mref,
		double mag_min,
		double mag_max,
		double tint
	) {

		// Get the value of (10^a)*Q that corresponds to branch ratio == 1.0

		final double base_ten_a_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
			1.0,		// n
			p,			// p
			c,			// c
			b,			// b
			alpha,		// alpha
			mref,		// mref
			mag_min,	// mag_min
			mag_max,	// mag_max
			tint		// tint
		);

		// Now get the array

		return get_ten_a_q_array (base_ten_a_q);
	}




	// Get the array of values of (10^ams)*Q, which is the productivity for mainshock triggering.
	// Parameters:
	//  mref = Reference magnitude.
	// Note: For ams, we force Q == 1.

	public final double[] get_ten_ams_q_array (double mref) {
		double[] result = zams_range.get_range_array();
		final double base_ams = OEStatsCalc.calc_a_new_from_mref_new (
			0.0,			// a_old
			b,				// b
			alpha,			// alpha
			0.0,			// mref_old
			mref			// mref_new
		);
		for (int i = 0; i < result.length; ++i) {
			result[i] = Math.pow(10.0, base_ams + result[i]);
		}
		return result;
	}




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

			writer.marshalDouble ("b"    , b    );
			writer.marshalDouble ("alpha", alpha);
			OEDiscreteRange.marshal_poly (writer, "c_range"   , c_range   );
			OEDiscreteRange.marshal_poly (writer, "p_range"   , p_range   );
			OEDiscreteRange.marshal_poly (writer, "br_range"  , br_range  );
			OEDiscreteRange.marshal_poly (writer, "zams_range", zams_range);

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

			b          = reader.unmarshalDouble ("b"    );
			alpha      = reader.unmarshalDouble ("alpha");
			c_range    = OEDiscreteRange.unmarshal_poly (reader, "c_range"   );
			p_range    = OEDiscreteRange.unmarshal_poly (reader, "p_range"   );
			br_range   = OEDiscreteRange.unmarshal_poly (reader, "br_range"  );
			zams_range = OEDiscreteRange.unmarshal_poly (reader, "zams_range");

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
