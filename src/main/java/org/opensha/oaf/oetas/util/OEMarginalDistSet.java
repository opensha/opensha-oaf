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
	// Exception: If there is an entry in var_values for the variable, then that entry
	// specifies which out-of-range bins exist.
	// If a range has size 1, then typically there are no distributions for that variable.
	// An element of this array can be null if the corresponding variable is not used.

	public OEDiscreteRange[] var_ranges;

	// Values of the variables.
	// May be an empty array, but cannot be null.
	// If not null, it must contain an entry for every variable that appears in any marginal,
	// and optionally can contain entries for other variables.
	// An element of this array cannot be null, but can be an empty range if the corresponding
	// variable is not used.

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




	//----- Searching -----




	// Return true if we have names of the variables.

	public final boolean has_var_names () {
		if (var_names.length == 0) {
			return false;
		}
		return true;
	}




	// Return true if we have names of the data.

	public final boolean has_data_names () {
		if (data_names.length == 0) {
			return false;
		}
		return true;
	}




	// Return true if we have variable ranges.

	public final boolean has_var_ranges () {
		if (var_ranges.length == 0) {
			return false;
		}
		return true;
	}




	// Return true if we have variable values.

	public final boolean has_var_values () {
		if (var_values.length == 0) {
			return false;
		}
		return true;
	}




	// Return true if we have all grid definition information.

	public final boolean has_grid_def_info () {
		if (var_names.length == 0) {
			return false;
		}
		if (data_names.length == 0) {
			return false;
		}
		if (var_ranges.length == 0) {
			return false;
		}
		if (var_values.length == 0) {
			return false;
		}
		return true;
	}




	// Return true if we have univariate distributions.

	public final boolean has_univar () {
		if (univar.length == 0) {
			return false;
		}
		return true;
	}




	// Return true if we have bivariate distributions.

	public final boolean has_bivar () {
		if (bivar.length == 0) {
			return false;
		}
		return true;
	}




	// Find the index number for a given variable name.
	// Returns -1 if name not found.
	// Throws exception if we don't have variable names.
	
	public final int find_var_index (String name) {
		if (var_names.length == 0) {
			throw new IllegalStateException ("OEMarginalDistSet.find_var_index: Variable names not available");
		}
		for (int n = 0; n < var_names.length; ++n) {
			if (var_names[n].equals (name)) {
				return n;
			}
		}
		return -1;
	}




	// Find the index number for a given data name.
	// Returns -1 if name not found.
	// Throws exception if we don't have data names.
	
	public final int find_data_index (String name) {
		if (data_names.length == 0) {
			throw new IllegalStateException ("OEMarginalDistSet.find_data_index: Data names not available");
		}
		for (int n = 0; n < data_names.length; ++n) {
			if (data_names[n].equals (name)) {
				return n;
			}
		}
		return -1;
	}




	// Find the univariate marginal distribution, given the variable and data indexes.
	// Returns null if not found.

	public final OEMarginalDistUni find_univar (int var_index, int data_index) {
		for (OEMarginalDistUni x : univar) {
			if (x.data_index == data_index) {
				if (x.var_index == var_index) {
					return x;
				}
			}
		}
		return null;
	}




	// Find the bivariate marginal distribution, given the variable and data indexes.
	// Returns null if not found.
	// If found, reversed[0] is set true if the index numbers are reversed in the marginal, false if not.

	public final OEMarginalDistBi find_bivar (int var_index1, int var_index2, int data_index, boolean[] reversed) {
		for (OEMarginalDistBi x : bivar) {
			if (x.data_index == data_index) {
				if (x.var_index1 == var_index1 && x.var_index2 == var_index2) {
					reversed[0] = false;
					return x;
				}
				if (x.var_index1 == var_index2 && x.var_index2 == var_index1) {
					reversed[0] = true;
					return x;
				}
			}
		}
		return null;
	}




	// Find the bivariate marginal distribution, given the variable and data indexes.
	// Returns null if not found.
	// If found, and the index numbers are reversed in the marginal, then make and return the transpose of the marginal.

	public final OEMarginalDistBi find_bivar_or_transpose (int var_index1, int var_index2, int data_index) {
		for (OEMarginalDistBi x : bivar) {
			if (x.data_index == data_index) {
				if (x.var_index1 == var_index1 && x.var_index2 == var_index2) {
					return x;
				}
				if (x.var_index1 == var_index2 && x.var_index2 == var_index1) {
					return (new OEMarginalDistBi()).transpose_of (x);
				}
			}
		}
		return null;
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




	// Default constructor makes an empty set.

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




	// Display an extended version of our contents.

	public String extended_string () {
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

		for (int n = 0; n < var_values.length; ++n) {
			result.append ("var_values[" + n + "] = {" + var_values[n].summary_string() + "}\n");
		}

		for (int n = 0; n < univar.length; ++n) {
			result.append ("univar[" + n + "] = {" + univar[n].summary_string() + "}\n");
		}

		for (int n = 0; n < bivar.length; ++n) {
			result.append ("bivar[" + n + "] = {" + bivar[n].summary_string() + "}\n");
		}

		return result.toString();
	}




	// Display an extended version of our contents.
	// This version includes contents of variable ranges and univariate distributions,
	// and partial contents of bivariate distributions.

	public String extended_string_2 () {
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

		for (int n = 0; n < var_values.length; ++n) {
			result.append ("var_values[" + n + "] = {" + var_values[n].toString() + "}\n");
		}

		for (int n = 0; n < univar.length; ++n) {
			result.append ("univar[" + n + "] = {" + univar[n].toString() + "}\n");
		}

		for (int n = 0; n < bivar.length; ++n) {
			result.append ("bivar[" + n + "] = {" + bivar[n].toString() + "}\n");
		}

		return result.toString();
	}




	// Deep copy of another object.
	// Returns this object.

	public OEMarginalDistSet copy_from (OEMarginalDistSet other) {
		var_names		= OEArraysCalc.array_copy (other.var_names);
		data_names		= OEArraysCalc.array_copy (other.data_names);
		var_ranges		= new OEDiscreteRange[other.var_ranges.length];
		for (int i = 0; i < var_ranges.length; ++i) {
			var_ranges[i]	= other.var_ranges[i];	// OEDiscreteRange objects are immutable
		}
		var_values		= OEMarginalDistRange.array_copy (other.var_values);
		univar			= OEMarginalDistUni.array_copy (other.univar);
		bivar			= OEMarginalDistBi.array_copy (other.bivar);
		return this;
	}




	// Deep copy an array of objects.
	// Returns the newly-allocated array.

	public static OEMarginalDistSet[] array_copy (final OEMarginalDistSet[] x) {
		final int c0 = x.length;
		final OEMarginalDistSet[] r0 = new OEMarginalDistSet[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = (new OEMarginalDistSet()).copy_from (x[m0]);
		}
		return r0;
	}




	// Get the amount of table storage used (in units of double).

	public final long get_table_storage () {
		long storage = 0L;
		for (OEMarginalDistRange x : var_values) {
			storage += x.get_table_storage();
		}
		for (OEMarginalDistUni x : univar) {
			storage += x.get_table_storage();
		}
		for (OEMarginalDistBi x : bivar) {
			storage += x.get_table_storage();
		}
		return storage;
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




	// Test the find_var_index function.

	public static void test_find_var_index (OEMarginalDistSet dist_set, String name) {
		int index = dist_set.find_var_index (name);

		System.out.println ();
		System.out.println ("find_var_index(\"" + name + "\") = " + index);

		return;
	}




	// Test the find_data_index function.

	public static void test_find_data_index (OEMarginalDistSet dist_set, String name) {
		int index = dist_set.find_data_index (name);

		System.out.println ();
		System.out.println ("find_data_index(\"" + name + "\") = " + index);

		return;
	}




	// Test the find_univar function.

	public static void test_find_univar (OEMarginalDistSet dist_set, int var_index, int data_index) {
		OEMarginalDistUni x = dist_set.find_univar (var_index, data_index);

		System.out.println ();
		if (x == null) {
			System.out.println ("find_univar(" + var_index + ", " + data_index + ") = " + "<null>");
		} else {
			System.out.println ("find_univar(" + var_index + ", " + data_index + ") = {" + x.summary_string() + "}");
		}

		return;
	}




	// Test the find_bivar function.

	public static void test_find_bivar (OEMarginalDistSet dist_set, int var_index1, int var_index2, int data_index) {
		boolean[] reversed = new boolean[1];
		OEMarginalDistBi x = dist_set.find_bivar (var_index1, var_index2, data_index, reversed);

		System.out.println ();
		if (x == null) {
			System.out.println ("find_bivar(" + var_index1 + ", " + var_index2 + ", " + data_index + ", reversed) = " + "<null>");
		} else {
			System.out.println ("find_bivar(" + var_index1 + ", " + var_index2 + ", " + data_index + ", reversed) = {" + x.summary_string() + "}");
			System.out.println ("reversed[0] = " + reversed[0]);
		}

		return;
	}




	// Test the find_bivar_or_transpose function.

	public static void test_find_bivar_or_transpose (OEMarginalDistSet dist_set, int var_index1, int var_index2, int data_index) {
		OEMarginalDistBi x = dist_set.find_bivar_or_transpose (var_index1, var_index2, data_index);

		System.out.println ();
		if (x == null) {
			System.out.println ("find_bivar_or_transpose(" + var_index1 + ", " + var_index2 + ", " + data_index + ") = " + "<null>");
		} else {
			System.out.println ("find_bivar_or_transpose(" + var_index1 + ", " + var_index2 + ", " + data_index + ") = {" + x.summary_string() + "}");
		}

		return;
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




		// Subcommand : Test #3
		// Command format:
		//  test3  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Copy and display the copy.
		// This version includes grid information.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and copying, set of marginal distributions");
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

			// Copy

			System.out.println ();
			System.out.println ("********** Copy **********");
			System.out.println ();
			
			OEMarginalDistSet dist_set2 = (new OEMarginalDistSet()).copy_from (dist_set);

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




		// Subcommand : Test #4
		// Command format:
		//  test4  reps
		// Construct test values, using the specified number of repetitions, and display it.
		// Display the results of various query functions.
		// This version includes grid information.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Constructing, displaying, and querying, set of marginal distributions");
			int reps = testargs.get_int ("reps");
			testargs.end_test();

			// Create the values

			OEMarginalDistSet dist_set = make_test_value_2 (reps);

			// Display the contents

			System.out.println ();
			System.out.println ("********** Display with toString **********");
			System.out.println ();

			System.out.println (dist_set.toString());

			System.out.println ();
			System.out.println ("********** Display with extended_string **********");
			System.out.println ();

			System.out.println (dist_set.extended_string());

			System.out.println ();
			System.out.println ("********** Display with extended_string_2 **********");
			System.out.println ();

			System.out.println (dist_set.extended_string_2());

			System.out.println ();
			System.out.println ("********** Availability query functions **********");

			System.out.println ();
			System.out.println ("has_var_names() = " + dist_set.has_var_names());

			System.out.println ();
			System.out.println ("has_data_names() = " + dist_set.has_data_names());

			System.out.println ();
			System.out.println ("has_var_ranges() = " + dist_set.has_var_ranges());

			System.out.println ();
			System.out.println ("has_var_values() = " + dist_set.has_var_values());

			System.out.println ();
			System.out.println ("has_grid_def_info() = " + dist_set.has_grid_def_info());

			System.out.println ();
			System.out.println ("has_univar() = " + dist_set.has_univar());

			System.out.println ();
			System.out.println ("has_bivar() = " + dist_set.has_bivar());

			System.out.println ();
			System.out.println ("********** Size query functions **********");

			System.out.println ();
			System.out.println ("get_table_storage() = " + dist_set.get_table_storage());

			System.out.println ();
			System.out.println ("********** Name query functions **********");

			System.out.println ();
			System.out.println ("find_var_index (\"test_var1\") = " + dist_set.find_var_index ("test_var1"));

			System.out.println ();
			System.out.println ("find_var_index (\"test_var2\") = " + dist_set.find_var_index ("test_var2"));

			System.out.println ();
			System.out.println ("find_var_index (\"test_var3\") = " + dist_set.find_var_index ("test_var3"));

			System.out.println ();
			System.out.println ("find_data_index (\"test_data\") = " + dist_set.find_data_index ("test_data"));

			System.out.println ();
			System.out.println ("find_data_index (\"test_data2\") = " + dist_set.find_data_index ("test_data2"));

			System.out.println ();
			System.out.println ("********** Univariate search functions **********");

			test_find_univar (dist_set, 0, 0);

			test_find_univar (dist_set, 1, 0);

			test_find_univar (dist_set, 2, 0);

			test_find_univar (dist_set, 0, 1);

			System.out.println ();
			System.out.println ("********** Bivariate search functions **********");

			test_find_bivar (dist_set, 0, 1, 0);

			test_find_bivar (dist_set, 1, 0, 0);

			test_find_bivar (dist_set, 2, 1, 0);

			test_find_bivar (dist_set, 0, 2, 0);

			test_find_bivar (dist_set, 0, 1, 1);

			System.out.println ();
			System.out.println ("********** Bivariate search functions with transpose **********");

			test_find_bivar_or_transpose (dist_set, 0, 1, 0);

			test_find_bivar_or_transpose (dist_set, 1, 0, 0);

			test_find_bivar_or_transpose (dist_set, 2, 1, 0);

			test_find_bivar_or_transpose (dist_set, 0, 2, 0);

			test_find_bivar_or_transpose (dist_set, 0, 1, 1);

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
