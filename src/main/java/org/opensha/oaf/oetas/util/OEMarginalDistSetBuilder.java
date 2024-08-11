package org.opensha.oaf.oetas.util;

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

import org.opensha.oaf.oetas.fit.OEGridParams;


// Class to build a set of univariate and bivariate marginal distribution for operational ETAS.
// Author: Michael Barall 08/09/2024.

public class OEMarginalDistSetBuilder {


	//----- Data -----

	// The distribution set.

	private OEMarginalDistSet dist_set;

	// The array of bin values for the current variables.
	// The length equals the number of variables.

	private int[] dist_n;

	// The weights for the current data.
	// The legnth equals the number of data.

	private double[] dist_w;

	// The bin finders, for comverting varialbe values to bin numbers.
	// The length equals the number of variables.

	private OEMarginalBinFinder[] bin_finders;




	//----- Temporaries used during setup -----

	// The list of variable names defined so far.

	private List<String> var_name_list;

	// The list of data names defined so far.

	private List<String> data_name_list;

	// The list of variable ranges defined so far.
	// Elements of this list may be null if the corresponding variable is not used.

	private List<OEDiscreteRange> var_range_list;

	// The list of flags, indicating which variables are used in the marginals.

	private List<Boolean> var_usage_list;




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		dist_set = null;
		dist_n = null;
		dist_w = null;
		bin_finders = null;

		var_name_list = new ArrayList<String>();
		data_name_list = new ArrayList<String>();
		var_range_list = new ArrayList<OEDiscreteRange>();
		var_usage_list = new ArrayList<Boolean>();
		return;
	}




	// Default constructor.

	public OEMarginalDistSetBuilder () {
		clear();
	}




	// Add a variable.
	// Parameters:
	//  name = Name of the variable.  If null, it is changed to an empty string.
	//  range = Range of the varialbe.  Can be null if the variable is not used in marginals.
	//  f_used = True to use variable in the marginals, false if not.
	// Returns the index number of this variable.

	public final int add_var (String name, OEDiscreteRange range, boolean f_used) {
		var_name_list.add ((name == null) ? "" : name);
		var_range_list.add (range);
		var_usage_list.add (f_used);
		return var_name_list.size() - 1;
	}




	// Add a variable.
	// Parameters:
	//  name = Name of the variable.  If null, it is changed to an empty string.
	//  range = Range of the varialbe.  Can be null if the variable is not used in marginals.
	// Returns the index number of this variable.
	// Note: The variable is used in marginals if the range is non-null has contains more than one value.

	public final int add_var (String name, OEDiscreteRange range) {
		boolean f_used = false;
		if (range != null && range.get_range_size() > 1) {
			f_used = true;
		}
		return add_var (name, range, f_used);
	}



	// Add a data field.
	// Parameters:
	//  name = Name of the data field.  If null, it is changed to an empty string.
	// Returns the index number of this data field.

	public final int add_data (String name) {
		data_name_list.add ((name == null) ? "" : name);
		return data_name_list.size() - 1;
	}




	// Finish setting up the distribution set, and begin accumulation.
	// Flag indicates if bivariate marginal distributions are desired.
	// This function creates all distributions for variables that have multiple values.

	public final void begin_accum (boolean f_bivar_marg) {

		// Number of variables and data

		final int var_count = var_name_list.size();
		final int data_count = data_name_list.size();

		// Arrays for bin values and data values

		dist_n = new int[var_count];
		dist_w = new double[data_count];

		// Make the bin finders

		bin_finders = new OEMarginalBinFinder[var_count];

		for (int i = 0; i < var_count; ++i) {
			OEDiscreteRange range = var_range_list.get(i);
			if (range == null) {
				bin_finders[i] = new OEMarginalBinSingle();	// for null range, use a single bin
			} else {
				bin_finders[i] = range.make_bin_finder (true, true);
			}
		}

		// List of univariate distributions

		List<OEMarginalDistUni> univar_list = new ArrayList<OEMarginalDistUni>();

		for (int i = 0; i < var_count; ++i) {
			if (var_usage_list.get(i)) {
				for (int k = 0; k < data_count; ++k) {
					univar_list.add ((new OEMarginalDistUni()).begin_accum (
						var_name_list.get(i),
						i,
						data_name_list.get(k),
						k,
						bin_finders[i].get_bin_count()
					));
				}
			}
		}

		// List of bivariate distributions

		List<OEMarginalDistBi> bivar_list = new ArrayList<OEMarginalDistBi>();

		if (f_bivar_marg) {

			for (int i = 0; i < var_count; ++i) {
				if (var_usage_list.get(i)) {
					for (int j = i + 1; j < var_count; ++j) {
						if (var_usage_list.get(j)) {
							for (int k = 0; k < data_count; ++k) {
								bivar_list.add ((new OEMarginalDistBi()).begin_accum (
									var_name_list.get(i),
									i,
									var_name_list.get(j),
									j,
									data_name_list.get(k),
									k,
									bin_finders[i].get_bin_count(),
									bin_finders[j].get_bin_count()
								));
							}
						}
					}
				}
			}

		}

		// Create the marginal dustrubution set

		dist_set = (new OEMarginalDistSet())
			.set_grid_info (var_name_list, data_name_list, var_range_list)
			.begin_accum (univar_list, bivar_list);

		return;
	}




	//----- Access -----




	// Get the marginal distribution set.

	public final OEMarginalDistSet get_dist_set () {
		return dist_set;
	}




	// Accumulate the variables and data into the marginal distributions.

	public final void accum () {
		dist_set.add_weight (dist_n, dist_w);
		return;
	}




	// Set a variable, converting from floating-point value to bin.
	// Parameters:
	//  v_index = Variable index number.
	//  v_value = Variable value.

	public final void set_var (int v_index, double v_value) {
		dist_n[v_index] = bin_finders[v_index].find_bin (v_value);
		return;
	}




	// Set a data field.
	// Parameters:
	//  d_index = Data index number.
	//  d_value = Data value.

	public final void set_data (int d_index, double d_value) {
		dist_w[d_index] = d_value;
		return;
	}



	// Set a data field, then accumulate the variables and data into the marginal distributions.
	// Parameters:
	//  d_index = Data index number.
	//  d_value = Data value.

	public final void set_data_and_accum (int d_index, double d_value) {
		dist_w[d_index] = d_value;
		dist_set.add_weight (dist_n, dist_w);
		return;
	}




	// Finish accumulation.
	// Parameters:
	//  norm_uni = Desired total weight for univariate marginals, use a negative value for no normalization.
	//  norm_bi = Desired total weight for bivariate marginals, use a negative value for no normalization.
	//  format = Format code for rounding, or null if none. (see SimpleUtils.round_double_via_string)
	// Returns the marginal distribution set.

	public final OEMarginalDistSet end_accum (double norm_uni, double norm_bi, String format) {
		dist_set.end_accum (norm_uni, norm_bi, format);
		return dist_set;
	}




	//----- Special purpose -----




	// Index numbers for variables in ETAS grid parameters (see OEGridParams).

	public static final int VMIX_B = 0;
	public static final int VMIX_ALPHA = 1;
	public static final int VMIX_C = 2;
	public static final int VMIX_P = 3;
	public static final int VMIX_N = 4;
	public static final int VMIX_ZAMS = 5;
	public static final int VMIX_ZMU = 6;




	// Add the variables for an ETAS grid.
	// Parameters:
	//  grid_params = Definition of the parameters.
	// Note: To agree with the above index number, this must be the only (or at least the first) variables.

	public final void add_etas_vars (OEGridParams grid_params) {
		add_var ("b", grid_params.b_range);
		add_var ("alpha", grid_params.alpha_range);
		add_var ("c", grid_params.c_range);
		add_var ("p", grid_params.p_range);
		add_var ("n", grid_params.n_range);
		add_var ("zams", grid_params.zams_range);
		add_var ("zmu", grid_params.zmu_range);
		return;
	}




	// Set the ETAS variables b, alpha, c, p, n.

	public final void set_etas_var_b_alpha_c_p_n (
		double b,
		double alpha,
		double c,
		double p,
		double n
	) {
		dist_n[VMIX_B] = bin_finders[VMIX_B].find_bin (b);
		dist_n[VMIX_ALPHA] = bin_finders[VMIX_ALPHA].find_bin (alpha);
		dist_n[VMIX_C] = bin_finders[VMIX_C].find_bin (c);
		dist_n[VMIX_P] = bin_finders[VMIX_P].find_bin (p);
		dist_n[VMIX_N] = bin_finders[VMIX_N].find_bin (n);
		return;
	}




	// Set the ETAS variables zams, zmu.

	public final void set_etas_var_zams_zmu (
		double zams,
		double zmu
	) {
		dist_n[VMIX_ZAMS] = bin_finders[VMIX_ZAMS].find_bin (zams);
		dist_n[VMIX_ZMU] = bin_finders[VMIX_ZMU].find_bin (zmu);
		return;
	}




	// Index numbers for ETAS data, a single probability..

	public static final int DMIX_PROB = 0;




	// Index numbers for ETAS data, generic, sequence specific, and bayesian.

	public static final int DMIX_GENERIC = 0;
	public static final int DMIX_DEQ_SPEC = 1;
	public static final int DMIX_BAYESIAN = 2;




	// Add the data for ETAS, a single probability.

	public final void add_etas_data_prob () {
		add_data ("probability");
		return;
	}




	// Add the data for ETAS, generic, sequence specific, and bayesian.

	public final void add_etas_data_gen_seq_bay () {
		add_data ("generic");
		add_data ("seqspec");
		add_data ("bayesian");
		return;
	}




	// Set the data for ETAS, a single probability.

	public final void set_etas_data_prob (double prob) {
		dist_w[DMIX_PROB] = prob;
		return;
	}




	// Set the data for ETAS, generic, sequence specific, and bayesian.

	public final void set_etas_data_gen_seq_bay (
		double gen,
		double seq,
		double bay
	) {
		dist_w[DMIX_GENERIC] = gen;
		dist_w[DMIX_DEQ_SPEC] = seq;
		dist_w[DMIX_BAYESIAN] = bay;
		return;
	}




	// Finish accumulation, using ETAS options.
	// Returns the marginal distribution set.

	public final OEMarginalDistSet end_etas_accum () {
		return end_accum (1000.0, 10000.0, "%.5e");
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEMarginalDistSetBuilder");



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
