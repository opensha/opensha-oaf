package org.opensha.oaf.oetas.util;

import java.util.Random;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;


// Class to hold a bivariate marginal distribution for operational ETAS.
// Author: Michael Barall 08/08/2024.

public class OEMarginalDistBi implements Marshalable {


	//----- Identification -----

	// Name of the variable #1, must be non-null.

	public String var_name1;

	// Index number of the variable #1 (0-based).

	public int var_index1;

	// Name of the variable #2, must be non-null.

	public String var_name2;

	// Index number of the variable #2 (0-based).

	public int var_index2;

	// Name of the data, must be non-null.

	public String data_name;

	// Index number of the data (0-based).

	public int data_index;



	//----- Data -----

	// Distribution.
	// The value of dist[n1][n2] is the weight of the n1-th bin
	// for variable #1 and the n2-th bin for variable #2.

	public double[][] dist;

	// The bin numbers for variables #1 and #2 with the greatest weight.

	public int mode1;
	public int mode2;

	// Scale factor (total weight).

	public double scale;

	// Number of data values supplied.
	// This field is not marshaled.

	//private long data_count;




	//----- Accumulation functions -----




	// Set up to begin accumulation.
	// Parameters:
	//  the_var_name1 = Name of the variable #1, cannot be null.
	//  the_var_index1 = Index number of the variable #1, 0-based.
	//  the_var_name2 = Name of the variable #2, cannot be null.
	//  the_var_index2 = Index number of the variable #2, 0-based.
	//  the_data_name = Name of the data, cannot be null.
	//  the_data_index = Index number of the data, 0-based.
	//  bin_count1 = Number of bins in the distribution for variable #1.
	//  bin_count2 = Number of bins in the distribution for variable #2.

	public final OEMarginalDistBi begin_accum (
		String the_var_name1,
		int the_var_index1,
		String the_var_name2,
		int the_var_index2,
		String the_data_name,
		int the_data_index,
		int bin_count1,
		int bin_count2
	) {
		// Save Identification

		var_name1 = the_var_name1;
		var_index1 = the_var_index1;
		var_name2 = the_var_name2;
		var_index2 = the_var_index2;
		data_name = the_data_name;
		data_index = the_data_index;
		
		// Initialize data

		dist = new double[bin_count1][bin_count2];
		OEArraysCalc.zero_array (dist);

		mode1 = -1;
		mode2 = -1;
		scale = 0.0;

		//data_count = 0L;

		return this;
	}




	// Add a weight to a bin.
	// Parameters:
	//  n1 = Bin number for variable #1.
	//  n2 = Bin number for variable #2.
	//  w = Weight, must be >= 0.0.
	// Note: This must be called at least once with w > 0.0.

	public final void add_weight (int n1, int n2, double w) {
		dist[n1][n2] += w;
		//++data_count;
		return;
	}




	// Add a weight to a bin.
	// Parameters:
	//  n = Array of bin numbers.
	//  w = Array of weights, each must be >= 0.0.
	// The indexes are used to select two bin numbers and one weight from the arrays.
	// Note: This must be called at least once with w > 0.0.

	public final void add_weight (int[] n, double[] w) {
		dist[n[var_index1]][n[var_index2]] += w[data_index];
		//++data_count;
		return;
	}




	// Finish accumulation.
	// Parameters:
	//  norm = Desired total weight, use a negative value for no normalization.
	//  format = Format code for rounding, or null if none. (see SimpleUtils.round_double_via_string).

	public final void end_accum (double norm, String format) {

		// Find total, and mode

		final int bin_count1 = dist.length;
		final int bin_count2 = dist[0].length;
		double total = 0.0;
		double max_weight = -1.0;
		for (int n1 = 0; n1 < bin_count1; ++n1) {
			for (int n2 = 0; n2 < bin_count2; ++n2) {
				final double w = dist[n1][n2];
				total += w;
				if (w > max_weight) {
					mode1 = n1;
					mode2 = n2;
					max_weight = w;
				}
			}
		}

		// Normalize, round

		//final double mult = ((norm < 0.0 || total <= 0.0) ? 1.0 : (norm / total));
		final double mult = ((norm < 0.0 || max_weight <= 0.0) ? 1.0 : (norm / max_weight));

		if (format == null) {
			scale = total * mult;
		} else {
			scale = SimpleUtils.round_double_via_string (format, total * mult);
		}

		for (int n1 = 0; n1 < bin_count1; ++n1) {
			for (int n2 = 0; n2 < bin_count2; ++n2) {
				final double w = dist[n1][n2];
				if (format == null) {
					dist[n1][n2] = w * mult;
				} else {
					dist[n1][n2] = SimpleUtils.round_double_via_string (format, w * mult);
				}
			}
		}

		return;
	}




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		var_name1		= "";
		var_index1		= -1;
		var_name2		= "";
		var_index2		= -1;
		data_name		= "";
		data_index		= -1;
		dist			= new double[0][];
		mode1			= -1;
		mode2			= -1;
		scale			= 0.0;
		//data_count		= 0L;
		return;
	}




	// Default constructor.

	public OEMarginalDistBi () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEMarginalDistBi:" + "\n");

		result.append ("var_name1 = "        + var_name1        + "\n");
		result.append ("var_index1 = "       + var_index1       + "\n");
		result.append ("var_name2 = "        + var_name2        + "\n");
		result.append ("var_index2 = "       + var_index2       + "\n");
		result.append ("data_name = "        + data_name        + "\n");
		result.append ("data_index = "       + data_index       + "\n");
		result.append ("dist.length = "      + dist.length      + "\n");
		if (dist.length > 0) {
			result.append ("dist[0].length = "   + dist[0].length   + "\n");
		}
		result.append ("mode1 = "            + mode1            + "\n");
		result.append ("mode2 = "            + mode2            + "\n");
		result.append ("scale = "            + scale            + "\n");

		if (mode2 >= 0) {
			for (int n = 0; n < dist.length; ++n) {
				result.append ("dist[" + n + "][" + mode2 + "] = " + dist[n][mode2] + "\n");
			}
		}

		if (mode1 >= 0) {
			for (int n = 0; n < dist[0].length; ++n) {
				result.append ("dist[" + mode1 + "][" + n + "] = " + dist[mode1][n] + "\n");
			}
		}

		return result.toString();
	}




	// Display a summary of our contents.

	public String summary_string () {
		StringBuilder result = new StringBuilder();

		result.append ("OEMarginalDistBi:" + "\n");

		result.append ("var_name1 = "        + var_name1        + "\n");
		result.append ("var_index1 = "       + var_index1       + "\n");
		result.append ("var_name2 = "        + var_name2        + "\n");
		result.append ("var_index2 = "       + var_index2       + "\n");
		result.append ("data_name = "        + data_name        + "\n");
		result.append ("data_index = "       + data_index       + "\n");
		result.append ("dist.length = "      + dist.length      + "\n");
		if (dist.length > 0) {
			result.append ("dist[0].length = "   + dist[0].length   + "\n");
		}
		result.append ("mode1 = "            + mode1            + "\n");
		result.append ("mode2 = "            + mode2            + "\n");
		result.append ("scale = "            + scale            + "\n");

		return result.toString();
	}




	// Deep copy of another object.
	// Returns this object.

	public OEMarginalDistBi copy_from (OEMarginalDistBi other) {
		var_name1		= other.var_name1;
		var_index1		= other.var_index1;
		var_name2		= other.var_name2;
		var_index2		= other.var_index2;
		data_name		= other.data_name;
		data_index		= other.data_index;
		dist			= OEArraysCalc.array_copy (other.dist);
		mode1			= other.mode1;
		mode2			= other.mode2;
		scale			= other.scale;
		//data_count		= other.data_count;
		return this;
	}




	// Deep copy an array of objects.
	// Returns the newly-allocated array.

	public static OEMarginalDistBi[] array_copy (final OEMarginalDistBi[] x) {
		final int c0 = x.length;
		final OEMarginalDistBi[] r0 = new OEMarginalDistBi[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = (new OEMarginalDistBi()).copy_from (x[m0]);
		}
		return r0;
	}




	// Set this objec to be the transpose of another object.
	// Returns this object.

	public OEMarginalDistBi transpose_of (OEMarginalDistBi other) {
		var_name1		= other.var_name2;
		var_index1		= other.var_index2;
		var_name2		= other.var_name1;
		var_index2		= other.var_index1;
		data_name		= other.data_name;
		data_index		= other.data_index;
		int n2 = other.dist.length;
		if (n2 == 0) {
			dist = new double[0][];
		} else {
			int n1 = other.dist[0].length;
			dist = new double[n1][n2];
			for (int i1 = 0; i1 < n1; ++i1) {
				for (int i2 = 0; i2 < n2; ++i2) {
					dist[i1][i2] = other.dist[i2][i1];
				}
			}
		}
		mode1			= other.mode2;
		mode2			= other.mode1;
		scale			= other.scale;
		//data_count		= other.data_count;
		return this;
	}




	// Get the amount of table storage used (in units of double).

	public final long get_table_storage () {
		if (dist.length == 0) {
			return 0L;
		}
		return ((long)(dist.length)) * ((long)(dist[0].length));
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 130001;

	private static final String M_VERSION_NAME = "OEMarginalDistBi";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalString ("var_name1", var_name1);
			writer.marshalInt ("var_index1", var_index1);
			writer.marshalString ("var_name2", var_name2);
			writer.marshalInt ("var_index2", var_index2);
			writer.marshalString ("data_name", data_name);
			writer.marshalInt ("data_index", data_index);
			writer.marshalDouble2DArray ("dist", dist);
			writer.marshalInt ("mode1", mode1);
			writer.marshalInt ("mode2", mode2);
			writer.marshalDouble ("scale", scale);

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

			var_name1 = reader.unmarshalString ("var_name1");
			var_index1 = reader.unmarshalInt ("var_index1");
			var_name2 = reader.unmarshalString ("var_name2");
			var_index2 = reader.unmarshalInt ("var_index2");
			data_name = reader.unmarshalString ("data_name");
			data_index = reader.unmarshalInt ("data_index");
			dist = reader.unmarshalDouble2DArray ("dist");
			mode1 = reader.unmarshalInt ("mode1");
			mode2 = reader.unmarshalInt ("mode2");
			scale = reader.unmarshalDouble ("scale");

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
	public OEMarginalDistBi unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEMarginalDistBi dist_bi) {
		dist_bi.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEMarginalDistBi static_unmarshal (MarshalReader reader, String name) {
		return (new OEMarginalDistBi()).unmarshal (reader, name);
	}

	// Marshal array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OEMarginalDistBi[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			static_marshal (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal array of objects.

	public static OEMarginalDistBi[] unmarshal_array (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		OEMarginalDistBi[] x = new OEMarginalDistBi[n];
		for (int i = 0; i < n; ++i) {
			x[i] = static_unmarshal (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEMarginalDistBi make_test_value (int reps) {
		OEMarginalDistBi dist_bi = new OEMarginalDistBi();

		Random rangen = new Random();

		OEDiscreteRange range1 = OEDiscreteRange.makeLinear (61, -3.0, 3.0);
		OEMarginalBinFinder bin_finder1 = range1.make_bin_finder (false, false);

		OEDiscreteRange range2 = OEDiscreteRange.makeLinear (41, -2.0, 2.0);
		OEMarginalBinFinder bin_finder2 = range2.make_bin_finder (false, false);

		dist_bi.begin_accum (
			"test_var1",
			1,
			"test_var2",
			3,
			"test_data",
			2,
			bin_finder1.get_bin_count(),
			bin_finder2.get_bin_count()
		);

		for (int i = 0; i < reps; ++i) {
			int n1 = bin_finder1.find_bin (rangen.nextGaussian());
			int n2 = bin_finder2.find_bin (0.5 * rangen.nextGaussian());
			double w = rangen.nextDouble();
			dist_bi.add_weight (n1, n2, w);
		}

		dist_bi.end_accum (100.0, "%.5e");

		return dist_bi;
	}




	// Make a value to use for testing purposes.
	// This version uses the array form of add_weight.

	public static OEMarginalDistBi make_test_value_2 (int reps) {
		OEMarginalDistBi dist_bi = new OEMarginalDistBi();

		Random rangen = new Random();

		OEDiscreteRange range1 = OEDiscreteRange.makeLinear (61, -3.0, 3.0);
		OEMarginalBinFinder bin_finder1 = range1.make_bin_finder (false, false);

		OEDiscreteRange range2 = OEDiscreteRange.makeLinear (41, -2.0, 2.0);
		OEMarginalBinFinder bin_finder2 = range2.make_bin_finder (false, false);

		dist_bi.begin_accum (
			"test_var1",
			1,
			"test_var2",
			3,
			"test_data",
			2,
			bin_finder1.get_bin_count(),
			bin_finder2.get_bin_count()
		);

		int[] n = new int[10];
		double[] w = new double[10];

		for (int i = 0; i < reps; ++i) {
			n[1] = bin_finder1.find_bin (rangen.nextGaussian());
			n[3] = bin_finder2.find_bin (0.5 * rangen.nextGaussian());
			w[2] = rangen.nextDouble();
			dist_bi.add_weight (n, w);
		}

		dist_bi.end_accum (100.0, "%.5e");

		return dist_bi;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEMarginalDistBi");




		// Subcommand : Test #1
		// Command format:
		//  test1  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling, marginal bivariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistBi dist_bi = make_test_value (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_bi.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (dist_bi);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_bi);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistBi dist_bi2 = new OEMarginalDistBi();
			MarshalUtils.from_json_string (dist_bi2, json_string);

			// Display the contents

			System.out.println (dist_bi2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.
		// This version uses the array form of add_weight.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling, marginal bivariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistBi dist_bi = make_test_value_2 (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_bi.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (dist_bi);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_bi);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistBi dist_bi2 = new OEMarginalDistBi();
			MarshalUtils.from_json_string (dist_bi2, json_string);

			// Display the contents

			System.out.println (dist_bi2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Copy and display the copy.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and copying, marginal bivariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistBi dist_bi = make_test_value (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_bi.toString());

			// Copy

			System.out.println ();
			System.out.println ("********** Copy **********");
			System.out.println ();
			
			OEMarginalDistBi dist_bi2 = (new OEMarginalDistBi()).copy_from (dist_bi);

			// Display the contents

			System.out.println (dist_bi2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Transpose and display the transpose.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and transposing, marginal bivariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistBi dist_bi = make_test_value (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_bi.toString());

			// Copy

			System.out.println ();
			System.out.println ("********** Transpose **********");
			System.out.println ();
			
			OEMarginalDistBi dist_bi2 = (new OEMarginalDistBi()).transpose_of (dist_bi);

			// Display the contents

			System.out.println (dist_bi2.toString());

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
