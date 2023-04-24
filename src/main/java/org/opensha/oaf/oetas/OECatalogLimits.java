package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to specify the size limits of an Operational ETAS catalog.
// Author: Michael Barall 04/07/2023.
//
// Holds the subset of the parameters in OECatalogParams that limit the
// number of ruptures and generations in a simulation.

public class OECatalogLimits {

	// The maximum number of generations.

	public int gen_count_max;

	// The maximum number of ruptures in a catalog, or 0 if no limit.
	// Note: This is likely a soft limit, with catalog size allowed to exceed this
	// but eventually be stopped if size continues to increase.

	public int max_cat_size;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		gen_count_max   = 0;
		max_cat_size    = 0;
		return;
	}




	// Default constructor.

	public OECatalogLimits () {
		clear();
	}




	// Construct and set all values.

	public OECatalogLimits (
		int gen_count_max,
		int max_cat_size
	) {
		this.gen_count_max   = gen_count_max;
		this.max_cat_size    = max_cat_size;
	}




	// Set all values.

	public final OECatalogLimits set (
		int gen_count_max,
		int max_cat_size
	) {
		this.gen_count_max   = gen_count_max;
		this.max_cat_size    = max_cat_size;
		return this;
	}




	// Set the generation count to be seeds only.

	public final OECatalogLimits set_seed_only () {
		this.gen_count_max   = 1;
		return this;
	}




	// Copy all values from the other object.

	public final OECatalogLimits copy_from (OECatalogLimits other) {
		this.gen_count_max   = other.gen_count_max;
		this.max_cat_size    = other.max_cat_size;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogLimits:" + "\n");

		result.append ("gen_count_max = "   + gen_count_max   + "\n");
		result.append ("max_cat_size = "    + max_cat_size    + "\n");

		return result.toString();
	}




	// Display our contents in a form used for progress reporting.

	public final String progress_string() {
		StringBuilder result = new StringBuilder();

		result.append ("gen_count_max = "   + gen_count_max   + "\n");
		result.append ("max_cat_size = "    + max_cat_size    + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 117001;

	private static final String M_VERSION_NAME = "OECatalogLimits";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt    ("gen_count_max"  , gen_count_max  );
			writer.marshalInt    ("max_cat_size"   , max_cat_size   );

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

			gen_count_max   = reader.unmarshalInt    ("gen_count_max"  );
			max_cat_size    = reader.unmarshalInt    ("max_cat_size"   );

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

	public OECatalogLimits unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogLimits catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogLimits static_unmarshal (MarshalReader reader, String name) {
		OECatalogLimits catalog = new OECatalogLimits();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two range structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_range_equal (OECatalogLimits other) {
		if (
			   this.gen_count_max   == other.gen_count_max  
			&& this.max_cat_size    == other.max_cat_size  
		) {
			return true;
		}
		return false;
	}

}
