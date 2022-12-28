package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to store statistics parameters for seeding an Operational ETAS catalog.
// Author: Michael Barall 12/27/2022.
//
// Holds the subset of the parameters in OESeedParams that determine the
// statistical properties of catalog seed ruptures
//
// See OESeedParams for definition of parameters.

public class OESeedParamsStats {

	//----- Parameters -----

	// Mainshock productivity parameter.

	public double ams;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		ams             = 0.0;
		return;
	}




	// Default constructor.

	public OESeedParamsStats () {
		clear();
	}




	// Constructor that sets all values.

	public OESeedParamsStats (
		double ams
	) {
		this.ams             = ams;
	}




	// Set all values.
	// Returns this object.

	public final OESeedParamsStats set (
		double ams
	) {
		this.ams             = ams;
		return this;
	}




	// Copy all values from the other object.
	// Returns this object.

	public final OESeedParamsStats copy_from (OESeedParamsStats other) {
		this.ams             = other.ams;
		return this;
	}




	// Set all values, given zero-mref ams-value plus catalog parameters.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming zero reference magnitude.
	//  cat_params = Catalog parameters:
	//   cat_params.b = G-R b-value.
	//   cat_params.alpha = ETAS alpha-value.
	//   cat_params.mref = Reference magnitude = Minimum magnitude.
	//   cat_params.msup = Maximum magnitude.
	// Returns this object.
	// Note: This is a subset of the values produced by OESeedParams.set_from_zams.

	public final OESeedParamsStats set_from_zams (double zams, OECatalogParams cat_params) {
		this.ams             = cat_params.calc_ams_from_zams (zams);
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OESeedParamsStats:" + "\n");

		result.append ("ams = "             + ams             + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 109001;

	private static final String M_VERSION_NAME = "OESeedParamsStats";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("ams"            , ams            );

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

			ams             = reader.unmarshalDouble ("ams"            );

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

	public OESeedParamsStats unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OESeedParamsStats catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OESeedParamsStats static_unmarshal (MarshalReader reader, String name) {
		OESeedParamsStats catalog = new OESeedParamsStats();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two catalog parameter structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_param_equal (OESeedParamsStats other) {
		if (
			   this.ams             == other.ams              
		) {
			return true;
		}
		return false;
	}




	// Set to typical values, with user-adjustable ams.
	// Returns this object.
	// Note: This is primarily for testing.
	// Note: This is a subset of the values produced by OESeedParams.set_to_typical.

	public final OESeedParamsStats set_to_typical (
		double ams
	) {
		this.ams             = ams;
		return this;
	}

}
