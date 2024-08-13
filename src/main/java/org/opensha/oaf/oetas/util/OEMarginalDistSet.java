package org.opensha.oaf.oetas.util;

import java.util.Random;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;


// Class to hold a set of univariate and bivariate marginal distribution for operational ETAS.
// Author: Michael Barall 08/08/2024.

public class OEMarginalDistSet implements Marshalable {


	//----- Grid definition -----

	// Names of the variables.
	// May be an empty array, but cannot be null.
	// The variable index numbers in the distributions may be used to index into this array.
	// Elements of this array must be non-null.

	public String[] var_names;

	// Names of the data.
	// May be an empty array, but cannot be null.
	// The data index numbers in the distributions may be used to index into this array.
	// Elements of this array must be non-null.

	public String[] data_names;

	// Ranges of the variables.
	// May be an empty array, but cannot be null.
	// The variable index numbers in the distributions may be used to index into this array.
	// If a range has size n, then the associated variable is partitioned into n+2 bins;
	// the first and last bins contain values that are out-of-range low or high, respectively.
	// If a range has size 1, then typically there are no distributions for that variable.
	// An element of this array can be null if the corresponding variable is not used.

	public OEDiscreteRange[] var_ranges;

	// Values of the variables.
	// May be an empty array, but cannot be null.
	// The variable index numbers in the distributions may be used to index into this array.
	// An element of this array cannot be null, but can be an empty range if the corresponding variable is not used.

	public OEMarginalDistRange[] var_values;


	//----- Data -----

	// Univariate marginal distributions.
	// May be an empty array, but cannot be null.

	public OEMarginalDistUni[] univar;

	// Bivariate marginal distributions.
	// May be an empty array, but cannot be null.

	public OEMarginalDistBi[] bivar;




	//----- Grid functions -----




	// Set up the grid information.
	// Parameters:
	//  var_name_list = List of variable names, can be null if none.  Elements must be non-null.
	//  data_name_list = List of data names, can be null if none.  Elements must be non-null.
	//  var_range_list = List of variable ranges, can be null if none.  Elements can be null.
	//  var_value_list = List of variable values, can be null if none.  Elements must be non-null.

	public final OEMarginalDistSet set_grid_info (
		Collection<String> var_name_list,
		Collection<String> data_name_list,
		Collection<OEDiscreteRange> var_range_list,
		Collection<OEMarginalDistRange> var_value_list
	) {
		if (var_name_list == null) {
			var_names = new String[0];
		} else {
			var_names = var_name_list.toArray (new String[0]);
		}

		if (data_name_list == null) {
			data_names = new String[0];
		} else {
			data_names = data_name_list.toArray (new String[0]);
		}

		if (var_range_list == null) {
			var_ranges = new OEDiscreteRange[0];
		} else {
			var_ranges = var_range_list.toArray (new OEDiscreteRange[0]);
		}

		if (var_value_list == null) {
			var_values = new OEMarginalDistRange[0];
		} else {
			var_values = var_value_list.toArray (new OEMarginalDistRange[0]);
		}

		return this;
	}




	//----- Accumulation functions -----




	// Set up to begin accumulation.
	// Parameters:
	//  univar_list = List of univariate distributions, can be null if none.
	//  bivar_list = List of bivariate distributions, can be null if none.
	// Note: The caller must have called begin_accum on each distribution.

	public final OEMarginalDistSet begin_accum (
		Collection<OEMarginalDistUni> univar_list,
		Collection<OEMarginalDistBi> bivar_list
	) {
		if (univar_list == null) {
			univar = new OEMarginalDistUni[0];
		} else {
			univar = univar_list.toArray (new OEMarginalDistUni[0]);
		}

		if (bivar_list == null) {
			bivar = new OEMarginalDistBi[0];
		} else {
			bivar = bivar_list.toArray (new OEMarginalDistBi[0]);
		}

		return this;
	}




	// Add a weight to a bin in each distribution.
	// Parameters:
	//  n = Array of bin numbers.
	//  w = Array of weights, each must be >= 0.0.
	// The indexes are used to select bin number(s) and weight from the arrays.

	public final void add_weight (int[] n, double[] w) {
		for (OEMarginalDistUni dist_uni : univar) {
			dist_uni.add_weight (n, w);
		}
		for (OEMarginalDistBi dist_bi : bivar) {
			dist_bi.add_weight (n, w);
		}
		return;
	}




	// Finish accumulation.
	// Parameters:
	//  norm_uni = Desired total weight for univariate marginals, use a negative value for no normalization.
	//  norm_bi = Desired total weight for bivariate marginals, use a negative value for no normalization.
	//  format = Format code for rounding, or null if none. (see SimpleUtils.round_double_via_string)

	public final void end_accum (double norm_uni, double norm_bi, String format) {
		for (OEMarginalDistUni dist_uni : univar) {
			dist_uni.end_accum (norm_uni, format);
		}
		for (OEMarginalDistBi dist_bi : bivar) {
			dist_bi.end_accum (norm_bi, format);
		}
		return;
	}




	// Erase the bivariate distributions.

	public final void erase_bivar_dist () {
		bivar = new OEMarginalDistBi[0];
		return;
	}




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		var_names	= new String[0];
		data_names	= new String[0];
		var_ranges	= new OEDiscreteRange[0];
		var_values	= new OEMarginalDistRange[0];

		univar		= new OEMarginalDistUni[0];
		bivar		= new OEMarginalDistBi[0];
		return;
	}




	// Default constructor.

	public OEMarginalDistSet () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEMarginalDistSet:" + "\n");

		result.append ("var_names.length = "  + var_names.length  + "\n");
		result.append ("data_names.length = " + data_names.length + "\n");
		result.append ("var_ranges.length = " + var_ranges.length + "\n");
		result.append ("var_values.length = " + var_values.length + "\n");

		result.append ("univar.length = "     + univar.length     + "\n");
		result.append ("bivar.length = "      + bivar.length      + "\n");

		for (int n = 0; n < var_names.length; ++n) {
			result.append ("var_names[" + n + "] = " + var_names[n] + "\n");
		}

		for (int n = 0; n < data_names.length; ++n) {
			result.append ("data_names[" + n + "] = " + data_names[n] + "\n");
		}

		for (int n = 0; n < var_ranges.length; ++n) {
			result.append ("var_ranges[" + n + "] = " + ((var_ranges[n] == null) ? "null" : (var_ranges[n].toString())) + "\n");
		}

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 131001;

	private static final String M_VERSION_NAME = "OEMarginalDistSet";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalStringArray ("var_names", var_names);
			writer.marshalStringArray ("data_names", data_names);
			OEDiscreteRange.marshal_array (writer, "var_ranges", var_ranges);
			OEMarginalDistRange.marshal_array (writer, "var_values", var_values);

			OEMarginalDistUni.marshal_array (writer, "univar", univar);
			OEMarginalDistBi.marshal_array (writer, "bivar", bivar);

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

			var_names = reader.unmarshalStringArray ("var_names");
			data_names = reader.unmarshalStringArray ("data_names");
			var_ranges = OEDiscreteRange.unmarshal_array (reader, "var_ranges");
			var_values = OEMarginalDistRange.unmarshal_array (reader, "var_values");

			univar = OEMarginalDistUni.unmarshal_array (reader, "univar");
			bivar = OEMarginalDistBi.unmarshal_array (reader, "bivar");

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
	public OEMarginalDistSet unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEMarginalDistSet dist_set) {
		dist_set.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEMarginalDistSet static_unmarshal (MarshalReader reader, String name) {
		return (new OEMarginalDistSet()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEMarginalDistSet make_test_value (int reps) {

		Random rangen = new Random();

		OEDiscreteRange range1 = OEDiscreteRange.makeLinear (61, -3.0, 3.0);
		OEMarginalBinFinder bin_finder1 = range1.make_bin_finder (false, false);

		OEDiscreteRange range2 = OEDiscreteRange.makeLinear (41, -2.0, 2.0);
		OEMarginalBinFinder bin_finder2 = range2.make_bin_finder (false, false);

		List<OEMarginalDistUni> univar_list = new ArrayList<OEMarginalDistUni>();
		List<OEMarginalDistBi> bivar_list = new ArrayList<OEMarginalDistBi>();

		univar_list.add ((new OEMarginalDistUni()).begin_accum (
			"test_var1",
			1,
			"test_data",
			2,
			bin_finder1.get_bin_count()
		));

		univar_list.add ((new OEMarginalDistUni()).begin_accum (
			"test_var2",
			3,
			"test_data",
			2,
			bin_finder2.get_bin_count()
		));

		bivar_list.add ((new OEMarginalDistBi()).begin_accum (
			"test_var1",
			1,
			"test_var2",
			3,
			"test_data",
			2,
			bin_finder1.get_bin_count(),
			bin_finder2.get_bin_count()
		));

		OEMarginalDistSet dist_set = (new OEMarginalDistSet()).begin_accum (univar_list, bivar_list);

		int[] n = new int[10];
		double[] w = new double[10];

		for (int i = 0; i < reps; ++i) {
			n[1] = bin_finder1.find_bin (rangen.nextGaussian());
			n[3] = bin_finder2.find_bin (0.5 * rangen.nextGaussian());
			w[2] = rangen.nextDouble();
			dist_set.add_weight (n, w);
		}

		dist_set.end_accum (1000.0, 10000.0, "%.5e");

		return dist_set;
	}




	// Make a value to use for testing purposes.
	// This version includes grid information.
	// Note this also extends the ranges with out-of-range bins.

	public static OEMarginalDistSet make_test_value_2 (int reps) {

		Random rangen = new Random();

		OEDiscreteRange range1 = OEDiscreteRange.makeLinear (61, -3.0, 3.0);
		OEDiscreteRange range2 = OEDiscreteRange.makeLinear (41, -2.0, 2.0);

		List<String> var_name_list = new ArrayList<String>();
		List<String> data_name_list = new ArrayList<String>();
		List<OEDiscreteRange> var_range_list = new ArrayList<OEDiscreteRange>();
		List<OEMarginalDistRange> var_value_list = new ArrayList<OEMarginalDistRange>();

		var_name_list.add ("test_var1");
		var_name_list.add ("test_var2");
		data_name_list.add ("test_data");
		var_range_list.add (range1);
		var_range_list.add (range2);

		OEMarginalBinFinder[] bin_finders = new OEMarginalBinFinder[var_range_list.size()];
		for (int i = 0; i < bin_finders.length; ++i) {
			bin_finders[i] = var_range_list.get(i).make_bin_finder (true, true);

			var_value_list.add ((new OEMarginalDistRange()).setup_range (
				var_name_list.get(i),
				i,
				var_range_list.get(i),
				true,
				true,
				"%.6e"
			));
		}

		List<OEMarginalDistUni> univar_list = new ArrayList<OEMarginalDistUni>();
		List<OEMarginalDistBi> bivar_list = new ArrayList<OEMarginalDistBi>();

		univar_list.add ((new OEMarginalDistUni()).begin_accum (
			var_name_list.get(0),
			0,
			data_name_list.get(0),
			0,
			bin_finders[0].get_bin_count()
		));

		univar_list.add ((new OEMarginalDistUni()).begin_accum (
			var_name_list.get(1),
			1,
			data_name_list.get(0),
			0,
			bin_finders[1].get_bin_count()
		));

		bivar_list.add ((new OEMarginalDistBi()).begin_accum (
			var_name_list.get(0),
			0,
			var_name_list.get(1),
			1,
			data_name_list.get(0),
			0,
			bin_finders[0].get_bin_count(),
			bin_finders[1].get_bin_count()
		));

		OEMarginalDistSet dist_set = (new OEMarginalDistSet())
			.set_grid_info (var_name_list, data_name_list, var_range_list, var_value_list)
			.begin_accum (univar_list, bivar_list);

		int[] n = new int[var_name_list.size()];
		double[] w = new double[data_name_list.size()];

		for (int i = 0; i < reps; ++i) {
			n[0] = bin_finders[0].find_bin (rangen.nextGaussian());
			n[1] = bin_finders[1].find_bin (0.5 * rangen.nextGaussian());
			w[0] = rangen.nextDouble();
			dist_set.add_weight (n, w);
		}

		dist_set.end_accum (1000.0, 10000.0, "%.5e");

		return dist_set;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEMarginalDistSet");




		// Subcommand : Test #1
		// Command format:
		//  test1  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling, set of marginal distributions");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistSet dist_set = make_test_value (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_set.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_set);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistSet dist_set2 = new OEMarginalDistSet();
			MarshalUtils.from_json_string (dist_set2, json_string);

			// Display the contents

			System.out.println (dist_set2.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON, again **********");
			System.out.println ();

			String json_string2 = MarshalUtils.to_formatted_compact_json_string (dist_set2);
			System.out.println (json_string2);

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
		// This version includes grid information.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and marshaling, set of marginal distributions");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistSet dist_set = make_test_value_2 (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Distribution Display **********");
			System.out.println ();

			System.out.println (dist_set.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			String json_string = MarshalUtils.to_formatted_compact_json_string (dist_set);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMarginalDistSet dist_set2 = new OEMarginalDistSet();
			MarshalUtils.from_json_string (dist_set2, json_string);

			// Display the contents

			System.out.println (dist_set2.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON, again **********");
			System.out.println ();

			String json_string2 = MarshalUtils.to_formatted_compact_json_string (dist_set2);
			System.out.println (json_string2);

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
