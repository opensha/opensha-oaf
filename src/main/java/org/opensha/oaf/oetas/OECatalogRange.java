package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to specify the time and magnitude ranges of an Operational ETAS catalog.
// Author: Michael Barall 03/07/2022.

public class OECatalogRange {

	// The range of times for which earthquakes are generated, in days.
	// The time origin is not specified by this class.

	public double tbegin;
	public double tend;

	// The minimum magnitude to use for the simulation.

	public double mag_min_sim;

	// The maximum magnitude to use for the simulation.

	public double mag_max_sim;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		tbegin          = 0.0;
		tend            = 0.0;
		mag_min_sim     = 0.0;
		mag_max_sim     = 0.0;
		return;
	}




	// Default constructor.

	public OECatalogRange () {
		clear();
	}




	// Construct and set all values.

	public OECatalogRange (
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim
	) {
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
	}




	// Set all values.

	public final OECatalogRange set (
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim
	) {
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		return this;
	}




	// Copy all values from the other object.

	public final OECatalogRange copy_from (OECatalogRange other) {
		this.tbegin          = other.tbegin;
		this.tend            = other.tend;
		this.mag_min_sim     = other.mag_min_sim;
		this.mag_max_sim     = other.mag_max_sim;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogRange:" + "\n");

		result.append ("tbegin = "          + tbegin          + "\n");
		result.append ("tend = "            + tend            + "\n");
		result.append ("mag_min_sim = "     + mag_min_sim     + "\n");
		result.append ("mag_max_sim = "     + mag_max_sim     + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 100001;

	private static final String M_VERSION_NAME = "OECatalogRange";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("tbegin"         , tbegin         );
			writer.marshalDouble ("tend"           , tend           );
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

			tbegin          = reader.unmarshalDouble ("tbegin"         );
			tend            = reader.unmarshalDouble ("tend"           );
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

	public OECatalogRange unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogRange catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogRange static_unmarshal (MarshalReader reader, String name) {
		OECatalogRange catalog = new OECatalogRange();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two range structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_range_equal (OECatalogRange other) {
		if (
			   this.tbegin          == other.tbegin         
			&& this.tend            == other.tend           
			&& this.mag_min_sim     == other.mag_min_sim    
			&& this.mag_max_sim     == other.mag_max_sim    
		) {
			return true;
		}
		return false;
	}

}
