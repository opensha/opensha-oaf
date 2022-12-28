package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to store magnitude range parameters for an Operational ETAS catalog.
// Author: Michael Barall 12/27/2022.
//
// Holds the subset of the parameters in OECatalogParams that determine the
// magnitude ranges of a catalog.
//
// See OECatalogParams for definition of parameters.

public class OECatalogParamsMags {

	//----- Parameters -----

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	public double mref;

	// Maximum considered magnitude, for parameter definition.

	public double msup;

	// The minimum magnitude to use for the simulation.

	public double mag_min_sim;

	// The maximum magnitude to use for the simulation.

	public double mag_max_sim;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		mref            = 0.0;
		msup            = 0.0;
		mag_min_sim     = 0.0;
		mag_max_sim     = 0.0;
		return;
	}




	// Default constructor.

	public OECatalogParamsMags () {
		clear();
	}




	//Constructor that sets all values.

	public OECatalogParamsMags (
		double mref,
		double msup,
		double mag_min_sim,
		double mag_max_sim
	) {
		this.mref            = mref;
		this.msup            = msup;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
	}




	// Set all values.
	// Returns this object.

	public final OECatalogParamsMags set (
		double mref,
		double msup,
		double mag_min_sim,
		double mag_max_sim
	) {
		this.mref            = mref;
		this.msup            = msup;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		return this;
	}




	// Copy all values from the other object.
	// Returns this object.

	public final OECatalogParamsMags copy_from (OECatalogParamsMags other) {
		this.mref            = other.mref;
		this.msup            = other.msup;
		this.mag_min_sim     = other.mag_min_sim;
		this.mag_max_sim     = other.mag_max_sim;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogParamsMags:" + "\n");

		result.append ("mref = "            + mref            + "\n");
		result.append ("msup = "            + msup            + "\n");
		result.append ("mag_min_sim = "     + mag_min_sim     + "\n");
		result.append ("mag_max_sim = "     + mag_max_sim     + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 108001;

	private static final String M_VERSION_NAME = "OECatalogParamsMags";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("mref"           , mref           );
			writer.marshalDouble ("msup"           , msup           );
			writer.marshalDouble ("mag_min_sim"    , mag_min_sim    );
			writer.marshalDouble ("mag_max_sim"    , mag_max_sim    );

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

			mref            = reader.unmarshalDouble ("mref"           );
			msup            = reader.unmarshalDouble ("msup"           );
			mag_min_sim     = reader.unmarshalDouble ("mag_min_sim"    );
			mag_max_sim     = reader.unmarshalDouble ("mag_max_sim"    );

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

	public OECatalogParamsMags unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogParamsMags catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogParamsMags static_unmarshal (MarshalReader reader, String name) {
		OECatalogParamsMags catalog = new OECatalogParamsMags();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two catalog parameter structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_param_equal (OECatalogParamsMags other) {
		if (
			   this.mref            == other.mref           
			&& this.msup            == other.msup           
			&& this.mag_min_sim     == other.mag_min_sim    
			&& this.mag_max_sim     == other.mag_max_sim    
		) {
			return true;
		}
		return false;
	}




	// Set to typical values, with some user-adjustable parameters.
	// Returns this object.
	// Note: This is primarily for testing.
	// Note: This is a subset of the values produced by OECatalogParams.set_to_typical.

	public final OECatalogParamsMags set_to_typical (
	) {
		this.mref            = 3.0;
		this.msup            = 9.5;
		this.mag_min_sim     = 3.0;
		this.mag_max_sim     = 9.5;
		return this;
	}




	// Set to values for simulation within a fixed magnitude range.
	// Parameters:
	//  mref = Minimum magnitude, also the reference magnitude and min mag for parameter definition.
	//  msup = Maximum magnitude, also the max mag for parameter definition.
	// Returns this object.
	// Note: This is a subset of the values produced by OECatalogParams.set_to_fixed_mag.

	public final OECatalogParamsMags set_to_fixed_mag (
		double mref,
		double msup
	) {
		this.mref            = mref;
		this.msup            = msup;
		this.mag_min_sim     = mref;
		this.mag_max_sim     = msup;
		return this;
	}

}
