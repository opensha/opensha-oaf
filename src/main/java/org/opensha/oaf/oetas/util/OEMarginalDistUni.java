package org.opensha.oaf.oetas.util;

import java.util.Random;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;


// Class to hold a univariate marginal distribution for operational ETAS.
// Author: Michael Barall 08/06/2024.

public class OEMarginalDistUni implements Marshalable {

	//----- Configuration -----

	// Fractile probabilites to compute.

	private static double[] frac_probs = {0.05, 0.25, 0.50, 0.75, 0.95};


	//----- Identification -----

	// Name of the variable, must be non-null.

	public String var_name;

	// Index number of the variable (0-based).

	public int var_index;

	// Name of the data, must be non-null.

	public String data_name;

	// Index number of the data (0-based).

	public int data_index;



	//----- Data -----

	// Distribution.
	// The value of dist[n] is the weight of the n-th bin.

	public double[] dist;

	// The bin number with the greatest weight.

	public int mode;

	// The bin numbers for the standard fractiles.

	public int[] fractiles;

	// Scale factor (total weight).

	public double scale;

	// Number of data values supplied.
	// This field is not marshaled.

	//private long data_count;




	//----- Accumulation functions -----




	// Set up to begin accumulation.
	// Parameters:
	//  the_var_name = Name of the variable, cannot be null.
	//  the_var_index = Index number of the variable, 0-based.
	//  the_data_name = Name of the data, cannot be null.
	//  the_data_index = Index number of the data, 0-based.
	//  bin_count = Number of bins in the distribution.

	public final OEMarginalDistUni begin_accum (
		String the_var_name,
		int the_var_index,
		String the_data_name,
		int the_data_index,
		int bin_count
	) {
		// Save Identification

		var_name = the_var_name;
		var_index = the_var_index;
		data_name = the_data_name;
		data_index = the_data_index;
		
		// Initialize data

		dist = new double[bin_count];
		OEArraysCalc.zero_array (dist);

		mode = -1;
		fractiles = new int[frac_probs.length];
		OEArraysCalc.fill_array (fractiles, -1);
		scale = 0.0;

		//data_count = 0L;

		return this;
	}




	// Add a weight to a bin.
	// Parameters:
	//  n = Bin number.
	//  w = Weight, must be >= 0.0.
	// Note: This must be called as least once with w > 0.0.

	public final void add_weight (int n, double w) {
		dist[n] += w;
		//++data_count;
		return;
	}




	// Add a weight to a bin.
	// Parameters:
	//  n = Array of bin numbers.
	//  w = Array of weights, each must be >= 0.0.
	// The indexes are used to select one element of each array.
	// Note: This must be called as least once with w > 0.0.

	public final void add_weight (int[] n, double[] w) {
		dist[n[var_index]] += w[data_index];
		//++data_count;
		return;
	}




	// Finish accumulation.
	// Parameters:
	//  norm = Desired total weight, use a negative value for no normalization.
	//  format = Format code for rounding, or null if none. (see SimpleUtils.round_double_via_string).

	public final void end_accum (double norm, String format) {

		// Find total, and mode

		final int bin_count = dist.length;
		double total = 0.0;
		double max_weight = -1.0;
		for (int n = 0; n < bin_count; ++n) {
			final double w = dist[n];
			total += w;
			if (w > max_weight) {
				mode = n;
				max_weight = w;
			}
		}

		// Normalize, round, find fractiles

		final int frac_count = frac_probs.length;
		int frac_ix = 0;
		double frac_sub = total * frac_probs[frac_ix];

		//final double mult = ((norm < 0.0 || total <= 0.0) ? 1.0 : (norm / total));
		final double mult = ((norm < 0.0 || max_weight <= 0.0) ? 1.0 : (norm / max_weight));

		if (format == null) {
			scale = total * mult;
		} else {
			scale = SimpleUtils.round_double_via_string (format, total * mult);
		}

		double sub_total = 0.0;
		for (int n = 0; n < bin_count; ++n) {
			final double w = dist[n];
			sub_total += w;
			while (frac_sub <= sub_total && frac_ix < frac_count) {
				fractiles[frac_ix] = n;
				++frac_ix;
				frac_sub = total * ((frac_ix < frac_count) ? frac_probs[frac_ix] : 100.0);
			}
			if (format == null) {
				dist[n] = w * mult;
			} else {
				dist[n] = SimpleUtils.round_double_via_string (format, w * mult);
			}
		}

		while (frac_ix < frac_count) {
			fractiles[frac_ix] = bin_count - 1;		// should not get here
			++frac_ix;
		}

		return;
	}




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		var_name		= "";
		var_index		= -1;
		data_name		= "";
		data_index		= -1;
		dist			= new double[0];
		mode			= -1;
		fractiles		= new int[0];
		scale			= 0.0;
		//data_count		= 0L;
		return;
	}




	// Default constructor.

	public OEMarginalDistUni () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEMarginalDistUni:" + "\n");

		result.append ("var_name = "         + var_name         + "\n");
		result.append ("var_index = "        + var_index        + "\n");
		result.append ("data_name = "        + data_name        + "\n");
		result.append ("data_index = "       + data_index       + "\n");
		result.append ("dist.length = "      + dist.length      + "\n");
		result.append ("mode = "             + mode             + "\n");
		result.append ("fractiles.length = " + fractiles.length + "\n");
		result.append ("scale = "            + scale            + "\n");

		for (int n = 0; n < dist.length; ++n) {
			result.append ("dist[" + n + "] = " + dist[n] + "\n");
		}
		for (int n = 0; n < fractiles.length; ++n) {
			result.append ("fractiles[" + n + "] = " + fractiles[n] + "\n");
		}

		return result.toString();
	}




	// Display a summary of our contents.

	public String summary_string () {
		StringBuilder result = new StringBuilder();

		result.append ("OEMarginalDistUni:" + "\n");

		result.append ("var_name = "         + var_name         + "\n");
		result.append ("var_index = "        + var_index        + "\n");
		result.append ("data_name = "        + data_name        + "\n");
		result.append ("data_index = "       + data_index       + "\n");
		result.append ("dist.length = "      + dist.length      + "\n");
		result.append ("mode = "             + mode             + "\n");
		result.append ("scale = "            + scale            + "\n");

		return result.toString();
	}




	// Deep copy of another object.
	// Returns this object.

	public OEMarginalDistUni copy_from (OEMarginalDistUni other) {
		var_name		= other.var_name;
		var_index		= other.var_index;
		data_name		= other.data_name;
		data_index		= other.data_index;
		dist			= OEArraysCalc.array_copy (other.dist);
		mode			= other.mode;
		fractiles		= OEArraysCalc.array_copy (other.fractiles);
		scale			= other.scale;
		//data_count		= other.data_count;
		return this;
	}




	// Deep copy an array of objects.
	// Returns the newly-allocated array.

	public static OEMarginalDistUni[] array_copy (final OEMarginalDistUni[] x) {
		final int c0 = x.length;
		final OEMarginalDistUni[] r0 = new OEMarginalDistUni[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = (new OEMarginalDistUni()).copy_from (x[m0]);
		}
		return r0;
	}




	// Get the amount of table storage used (in units of double).

	public final long get_table_storage () {
		return ((long)(dist.length)) + ((long)(fractiles.length));
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 129001;

	private static final String M_VERSION_NAME = "OEMarginalDistUni";

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
			writer.marshalString ("data_name", data_name);
			writer.marshalInt ("data_index", data_index);
			writer.marshalDoubleArray ("dist", dist);
			writer.marshalInt ("mode", mode);
			writer.marshalIntArray ("fractiles", fractiles);
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

			var_name = reader.unmarshalString ("var_name");
			var_index = reader.unmarshalInt ("var_index");
			data_name = reader.unmarshalString ("data_name");
			data_index = reader.unmarshalInt ("data_index");
			dist = reader.unmarshalDoubleArray ("dist");
			mode = reader.unmarshalInt ("mode");
			fractiles = reader.unmarshalIntArray ("fractiles");
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
	public OEMarginalDistUni unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEMarginalDistUni dist_uni) {
		dist_uni.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEMarginalDistUni static_unmarshal (MarshalReader reader, String name) {
		return (new OEMarginalDistUni()).unmarshal (reader, name);
	}

	// Marshal array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OEMarginalDistUni[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			static_marshal (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal array of objects.

	public static OEMarginalDistUni[] unmarshal_array (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		OEMarginalDistUni[] x = new OEMarginalDistUni[n];
		for (int i = 0; i < n; ++i) {
			x[i] = static_unmarshal (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEMarginalDistUni make_test_value (int reps) {
		OEMarginalDistUni dist_uni = new OEMarginalDistUni();

		Random rangen = new Random();

		OEDiscreteRange range = OEDiscreteRange.makeLinear (61, -3.0, 3.0);
		OEMarginalBinFinder bin_finder = range.make_bin_finder (false, false);

		dist_uni.begin_accum (
			"test_var",
			1,
			"test_data",
			2,
			bin_finder.get_bin_count()
		);

		for (int i = 0; i < reps; ++i) {
			int n = bin_finder.find_bin (rangen.nextGaussian());
			double w = rangen.nextDouble();
			dist_uni.add_weight (n, w);
		}

		dist_uni.end_accum (100.0, "%.5e");

		return dist_uni;
	}




	// Make a value to use for testing purposes.
	// This version uses the array form of add_weight.

	public static OEMarginalDistUni make_test_value_2 (int reps) {
		OEMarginalDistUni dist_uni = new OEMarginalDistUni();

		Random rangen = new Random();

		OEDiscreteRange range = OEDiscreteRange.makeLinear (61, -3.0, 3.0);
		OEMarginalBinFinder bin_finder = range.make_bin_finder (false, false);

		dist_uni.begin_accum (
			"test_var",
			1,
			"test_data",
			2,
			bin_finder.get_bin_count()
		);

		int[] n = new int[10];
		double[] w = new double[10];

		for (int i = 0; i < reps; ++i) {
			n[1] = bin_finder.find_bin (rangen.nextGaussian());
			w[2] = rangen.nextDouble();
			dist_uni.add_weight (n, w);
		}

		dist_uni.end_accum (100.0, "%.5e");

		return dist_uni;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEMarginalDistUni");




		// Subcommand : Test #1
		// Command format:
		//  test1  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling, marginal univariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistUni dist_uni = make_test_value (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_uni.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (dist_uni);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_uni);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistUni dist_uni2 = new OEMarginalDistUni();
			MarshalUtils.from_json_string (dist_uni2, json_string);

			// Display the contents

			System.out.println (dist_uni2.toString());

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

			System.out.println ("Constructing, displaying, and marshaling, marginal univariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistUni dist_uni = make_test_value_2 (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_uni.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (dist_uni);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_uni);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistUni dist_uni2 = new OEMarginalDistUni();
			MarshalUtils.from_json_string (dist_uni2, json_string);

			// Display the contents

			System.out.println (dist_uni2.toString());

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

			System.out.println ("Constructing, displaying, and copying, marginal univariate distribution");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistUni dist_uni = make_test_value (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_uni.toString());

			// Copy

			System.out.println ();
			System.out.println ("********** Copy **********");
			System.out.println ();
			
			OEMarginalDistUni dist_uni2 = (new OEMarginalDistUni()).copy_from (dist_uni);

			// Display the contents

			System.out.println (dist_uni2.toString());

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
