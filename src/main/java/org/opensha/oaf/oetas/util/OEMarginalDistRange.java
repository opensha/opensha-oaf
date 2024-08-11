package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;


// Class to hold bin values marginal distribution for operational ETAS.
// Author: Michael Barall 08/11/2024.

public class OEMarginalDistRange implements Marshalable {


	//----- Identification -----

	// Name of the variable, must be non-null.

	public String var_name;

	// Index number of the variable (0-based).

	public int var_index;

	// The number of bins used for the marginal distribution.

	public int bin_count;

	// If true, the first bin is for out-of-range values below the first value.

	public boolean out_lo;

	// If true, the last bin is for out-of-range values above the first value.

	public boolean out_hi;



	//----- Data -----

	// Values.
	// The number of values is equal to bin_count, minus 1 for each of the flags out_lo and out_hi that are true.

	public double[] values;




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		var_name		= "";
		var_index		= -1;
		bin_count		= 0;
		out_lo			= false;
		out_hi			= false;
		values			= new double[0];
		return;
	}




	// Default constructor.

	public OEMarginalDistRange () {
		clear();
	}




	// Set up the range.
	// Parameters:
	//  the_var_name = Name of the variable, cannot be null.
	//  the_var_index = Index number of the variable, 0-based.
	//  range = Discrete range.
	//  the_out_lo = True to include a bin for out-of-range values below the first value in the range.
	//  the_out_hi = True to include a bin for out-of-range values above the last value in the range.
	//  format = Format code for rounding, or null if none. (see SimpleUtils.round_double_via_string).

	public final OEMarginalDistRange setup_range (
		String the_var_name,
		int the_var_index,
		OEDiscreteRange range,
		boolean the_out_lo,
		boolean the_out_hi,
		String format
	) {
		int rsize = range.get_range_size();

		// Save identification

		var_name = the_var_name;
		var_index = the_var_index;
		bin_count = rsize + (the_out_lo ? 1 : 0) + (the_out_hi ? 1 : 0);
		out_lo = the_out_lo;
		out_hi = the_out_hi;

		// Set up the values.

		values = range.get_range_array();
		if (format != null) {
			for (int i = 0; i < values.length; ++i) {
				values[i] = SimpleUtils.round_double_via_string (format, values[i]);
			}
		}

		return this;
	}



	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEMarginalDistRange:" + "\n");

		result.append ("var_name = "         + var_name         + "\n");
		result.append ("var_index = "        + var_index        + "\n");
		result.append ("bin_count = "        + bin_count        + "\n");
		result.append ("out_lo = "           + out_lo           + "\n");
		result.append ("out_hi = "           + out_hi           + "\n");
		result.append ("values.length = "    + values.length    + "\n");

		for (int n = 0; n < values.length; ++n) {
			result.append ("values[" + n + "] = " + values[n] + "\n");
		}

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 132001;

	private static final String M_VERSION_NAME = "OEMarginalDistRange";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalString ("var_name", var_name);
			writer.marshalInt ("var_index", var_index);
			writer.marshalInt ("bin_count", bin_count);
			writer.marshalBoolean ("out_lo", out_lo);
			writer.marshalBoolean ("out_hi", out_hi);
			writer.marshalDoubleArray ("values", values);

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

			var_name = reader.unmarshalString ("var_name");
			var_index = reader.unmarshalInt ("var_index");
			bin_count = reader.unmarshalInt ("bin_count");
			out_lo = reader.unmarshalBoolean ("out_lo");
			out_hi = reader.unmarshalBoolean ("out_hi");
			values = reader.unmarshalDoubleArray ("values");

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
	public OEMarginalDistRange unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEMarginalDistRange dist_range) {
		dist_range.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEMarginalDistRange static_unmarshal (MarshalReader reader, String name) {
		return (new OEMarginalDistRange()).unmarshal (reader, name);
	}

	// Marshal array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OEMarginalDistRange[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			static_marshal (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal array of objects.

	public static OEMarginalDistRange[] unmarshal_array (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		OEMarginalDistRange[] x = new OEMarginalDistRange[n];
		for (int i = 0; i < n; ++i) {
			x[i] = static_unmarshal (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEMarginalDistRange make_test_value (int count, boolean f_out_lo, boolean f_out_hi) {
		OEMarginalDistRange dist_range = new OEMarginalDistRange();

//		OEDiscreteRange range = OEDiscreteRange.makeLinear (count, 1.0, 1.0 + ((count - 1)*0.1));
		OEDiscreteRange range = OEDiscreteRange.makeLinear (count, 0.0, 10.0);

		dist_range.setup_range (
			"test_var",
			1,
			range,
			f_out_lo,
			f_out_hi,
			"%.6e"
		);

		return dist_range;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEMarginalDistRange");




		// Subcommand : Test #1
		// Command format:
		//  test1  count  f_out_lo  f_out_hi
		// Construct test values, using the specified number of values and out-of-range flags, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling, marginal univariate distribution");
			int count = testargs.get_int ("count");
			boolean f_out_lo = testargs.get_boolean ("f_out_lo");
			boolean f_out_hi = testargs.get_boolean ("f_out_hi");
			testargs.end_test();

			// Create the values

			OEMarginalDistRange dist_range = make_test_value (count, f_out_lo, f_out_hi);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Range Display **********");
			System.out.println ();

			System.out.println (dist_range.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (dist_range);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_range);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistRange dist_range2 = new OEMarginalDistRange();
			MarshalUtils.from_json_string (dist_range2, json_string);

			// Display the contents

			System.out.println (dist_range2.toString());

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
