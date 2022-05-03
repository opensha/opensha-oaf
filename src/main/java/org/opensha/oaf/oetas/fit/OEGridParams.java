package org.opensha.oaf.oetas.fit;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.oetas.OEArraysCalc;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEDiscreteRange;
import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEOmoriCalc;


// Class to hold parameters for specifying a grid of likelihoods.
// Author: Michael Barall 04/28/2022.

public class OEGridParams {

	//----- Parameter ranges -----

	// The range of c-values.

	private OEDiscreteRange c_range;

	// The range of p-values.

	private OEDiscreteRange p_range;

	// The range of branch ratios.
	// This controls the productivity of secondary triggering.

	private OEDiscreteRange br_range;

	// The range of mainshock productivity values.
	// If f_ms_relative is false, this is the actual value of (10^ams)*Q.
	// If f_ms_relative is true, this is relative to some estimated value of (10^ams)*Q.

	private OEDiscreteRange ms_range;

	// Flag, true if ms_range is relative to some estimated productivity.

	private boolean f_ms_relative;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		c_range = null;
		p_range = null;
		br_range = null;
		ms_range = null;
		f_ms_relative = false;

		return;
	}




	// Default constructor.

	public OEGridParams () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGridParams:" + "\n");

		result.append ("c_range = "       + c_range.toString()  + "\n");
		result.append ("p_range = "       + p_range.toString()  + "\n");
		result.append ("br_range = "      + br_range.toString() + "\n");
		result.append ("ms_range = "      + ms_range.toString() + "\n");
		result.append ("f_ms_relative = " + f_ms_relative       + "\n");

		return result.toString();
	}




	// Set up the ranges with the supplied values.
	// Returns this object.
	// Implementation note: Since OEDiscreteRange objects are immutable, we can simply save them.

	public final OEGridParams set (
		OEDiscreteRange c_range,
		OEDiscreteRange p_range,
		OEDiscreteRange br_range,
		OEDiscreteRange ms_range,
		boolean f_ms_relative
	) {
		this.c_range = c_range;
		this.p_range = p_range;
		this.br_range = br_range;
		this.ms_range = ms_range;
		this.f_ms_relative = f_ms_relative;
		return this;
	}




	// Set up the ranges with the typical values.
	// Returns this object.

	public final OEGridParams set_typical () {
		this.c_range = OEDiscreteRange.makeLog (21, 0.00001, 1.00000);
		this.p_range = OEDiscreteRange.makeLinear (31, 0.70, 1.30);
		this.br_range = OEDiscreteRange.makeLog (21, 0.01, 1.00);
		this.ms_range = OEDiscreteRange.makeLog (41, 0.01, 100.00);
		this.f_ms_relative = true;
		return this;
	}




	//----- Readout -----




	// Get the number of c-values.

	public final int get_c_range_size () {
		return c_range.get_range_size();
	}


	// Get the minimum c-value.

	public final double get_c_range_min () {
		return c_range.get_range_min();
	}


	// Get the maximum c-value.

	public final double get_c_range_max () {
		return c_range.get_range_max();
	}




	// Get the number of p-values.

	public final int get_p_range_size () {
		return p_range.get_range_size();
	}


	// Get the minimum p-value.

	public final double get_p_range_min () {
		return p_range.get_range_min();
	}


	// Get the maximum p-value.

	public final double get_p_range_max () {
		return p_range.get_range_max();
	}




	// Get the number of branch ratio values.

	public final int get_br_range_size () {
		return br_range.get_range_size();
	}


	// Get the minimum branch ratio value.

	public final double get_br_range_min () {
		return br_range.get_range_min();
	}


	// Get the maximum branch ratio value.

	public final double get_br_range_max () {
		return br_range.get_range_max();
	}




	// Get the number of mainshock productivity values.

	public final int get_ms_range_size () {
		return ms_range.get_range_size();
	}


	// Get the minimum dmainshock productivity value.

	public final double get_ms_range_min () {
		return ms_range.get_range_min();
	}


	// Get the maximum mainshock productivity value.

	public final double get_ms_range_max () {
		return ms_range.get_range_max();
	}


	// Get the mainshock productivity relative flag.

	public final boolean get_f_ms_relative () {
		return f_ms_relative;
	}




	//----- Range arrays -----




	// Get the array of c-values.

	public final double[] get_c_range_array () {
		return c_range.get_range_array();
	}




	// Get the array of p-values.

	public final double[] get_p_range_array () {
		return p_range.get_range_array();
	}




	// Get the array of branch ratio values.

	public final double[] get_br_range_array () {
		return br_range.get_range_array();
	}




	// Get the array of mainshock productivity values.

	public final double[] get_ms_range_array () {
		return ms_range.get_range_array();
	}




	//----- Derived range arrays -----




	// Get the array of values of (10^a)*Q, which is the productivity for secondary triggering.
	// Parameters:
	//  base_ten_a_q = Value of (10^a)*Q that corresponds to branch ratio == 1.0.
	// Note: Because branch ratio is proportional to (10^a)*Q, this function
	// returns get_br_range_array() multiplied by base_ten_a_q.

	public final double[] get_ten_a_q_array (double base_ten_a_q) {
		double[] result = br_range.get_range_array();
		for (int i = 0; i < result.length; ++i) {
			result[i] *= base_ten_a_q;
		}
		return result;
	}



	// Get the array of values of (10^ams)*Q, which is the productivity for mainshock triggering.
	// Parameters:
	//  base_ten_ams_q = Estimated maximum likelihood value of (10^ams)*Q.
	// Note: If f_ms_relative is true, returns get_ms_range_array() multiplied by base_ten_ams_q.
	// Otherwise, returns get_ms_range_array().

	public final double[] get_ten_ams_q_array (double base_ten_ams_q) {
		double[] result = ms_range.get_range_array();
		if (f_ms_relative) {
			for (int i = 0; i < result.length; ++i) {
				result[i] *= base_ten_ams_q;
			}
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

			OEDiscreteRange.marshal_poly (writer, "c_range",  c_range );
			OEDiscreteRange.marshal_poly (writer, "p_range",  p_range );
			OEDiscreteRange.marshal_poly (writer, "br_range", br_range);
			OEDiscreteRange.marshal_poly (writer, "ms_range", ms_range);
			writer.marshalBoolean ("f_ms_relative", f_ms_relative);

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

			c_range  = OEDiscreteRange.unmarshal_poly (reader, "c_range" );
			p_range  = OEDiscreteRange.unmarshal_poly (reader, "p_range" );
			br_range = OEDiscreteRange.unmarshal_poly (reader, "br_range");
			ms_range = OEDiscreteRange.unmarshal_poly (reader, "ms_range");
			f_ms_relative = reader.unmarshalBoolean ("f_ms_relative");

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
