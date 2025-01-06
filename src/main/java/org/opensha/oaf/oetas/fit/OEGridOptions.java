package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.util.TestArgs;

import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;


// Class to hold options for a grid of likelihoods.
// Author: Michael Barall 08/29/2024.
//
// This object may be accessed simultaneously from multiple threads,
// therefore it must not be modified after it is initialized.

public class OEGridOptions implements Marshalable {

	//----- Options -----


	// True if the value of zams is interpreted relative to the a-value.

	private boolean relative_zams;

	public final boolean get_relative_zams () {
		return relative_zams;
	}




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		relative_zams = false;
		return;
	}




	// Default constructor, sets up default values.

	public OEGridOptions () {
		clear();
	}




	// Constructor that sets the supplied values.

	public OEGridOptions (
		boolean relative_zams
	) {
		this.relative_zams = relative_zams;
	}




	// Set up the options with the supplied values.
	// Returns this object.

	public final OEGridOptions set (
		boolean relative_zams
	) {
		this.relative_zams = relative_zams;
		return this;
	}




	// Copy values from another object.
	// Returns this object.

	public final OEGridOptions copy_from (OEGridOptions other) {
		this.relative_zams = other.relative_zams;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGridOptions:" + "\n");

		result.append ("relative_zams = " + relative_zams + "\n");

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 140001;

	private static final String M_VERSION_NAME = "OEGridOptions";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalBoolean ("relative_zams", relative_zams);

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

			relative_zams = reader.unmarshalBoolean ("relative_zams");

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
	public OEGridOptions unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEGridOptions obj) {
		obj.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEGridOptions static_unmarshal (MarshalReader reader, String name) {
		return (new OEGridOptions()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes - Default value.

	public static OEGridOptions make_test_value_1 () {
		OEGridOptions grid_options = new OEGridOptions();

		return grid_options;
	}




	// Make a value to use for testing purposes - Non-default value.

	public static OEGridOptions make_test_value_2 () {
		OEGridOptions grid_options = new OEGridOptions();

		grid_options.set (
			true		// relative_zams
		);

		return grid_options;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEGridOptions");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// This uses default options.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying grid options");
			testargs.end_test();

			// Create the values

			OEGridOptions grid_options = make_test_value_1();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Catalog Info Display **********");
			System.out.println ();

			System.out.println (grid_options.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (grid_options);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (grid_options);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEGridOptions grid_options2 = new OEGridOptions();
			MarshalUtils.from_json_string (grid_options2, json_string);

			// Display the contents

			System.out.println (grid_options2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEGridOptions grid_options3 = new OEGridOptions();
			grid_options3.copy_from (grid_options2);

			// Display the contents

			System.out.println (grid_options3.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// This uses non-default options.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying grid options");
			testargs.end_test();

			// Create the values

			OEGridOptions grid_options = make_test_value_2();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Catalog Info Display **********");
			System.out.println ();

			System.out.println (grid_options.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (grid_options);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (grid_options);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEGridOptions grid_options2 = new OEGridOptions();
			MarshalUtils.from_json_string (grid_options2, json_string);

			// Display the contents

			System.out.println (grid_options2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEGridOptions grid_options3 = new OEGridOptions();
			grid_options3.copy_from (grid_options2);

			// Display the contents

			System.out.println (grid_options3.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
