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

import org.opensha.oaf.rj.RJ_AftershockModel;


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

	// The list of variable values defined so far.
	// An element of this list cannot be null, but can be an empty range if the corresponding variable is not used.

	private List<OEMarginalDistRange> var_value_list;

	// The list of flags to include a bin for out-of-range low.

	private List<Boolean> f_out_lo_list;

	// The list of flags to include a bin for out-of-range high.

	private List<Boolean> f_out_hi_list;

	// The list of formats used for rounding variable values.
	// Elements of this list can be null if no rounding is needed.

	private List<String> var_format_list;

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
		var_value_list = new ArrayList<OEMarginalDistRange>();
		f_out_lo_list = new ArrayList<Boolean>();
		f_out_hi_list = new ArrayList<Boolean>();
		var_format_list = new ArrayList<String>();
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
	//  f_out_lo = True to include an extra bin for values out-of-range below the first bin.
	//  f_out_hi = True to include an extra bin for values out-of-range above the last bin.
	//  format = Format code for rounding variable values, or null if none. (see SimpleUtils.round_double_via_string)
	//  f_used = True to use variable in the marginals, false if not.
	// Returns the index number of this variable.

	public final int add_var (String name, OEDiscreteRange range, boolean f_out_lo, boolean f_out_hi, String format, boolean f_used) {
		var_name_list.add ((name == null) ? "" : name);
		var_range_list.add (range);
		f_out_lo_list.add (f_out_lo);
		f_out_hi_list.add (f_out_hi);
		var_format_list.add (format);
		var_usage_list.add (f_used);
		return var_name_list.size() - 1;
	}




	// Add a variable.
	// Parameters:
	//  name = Name of the variable.  If null, it is changed to an empty string.
	//  range = Range of the varialbe.  Can be null if the variable is not used in marginals.
	//  f_out_lo = True to include an extra bin for values out-of-range below the first bin.
	//  f_out_hi = True to include an extra bin for values out-of-range above the last bin.
	//  format = Format code for rounding variable values, or null if none. (see SimpleUtils.round_double_via_string)
	// Returns the index number of this variable.
	// Note: The variable is used in marginals if the range is non-null has contains more than one value.

	public final int add_var (String name, OEDiscreteRange range, boolean f_out_lo, boolean f_out_hi, String format) {
		boolean f_used = false;
		if (range != null && range.get_range_size() > 1) {
			f_used = true;
		}
		return add_var (name, range, f_out_lo, f_out_hi, format, f_used);
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

		// Make the bin finders and variable values

		bin_finders = new OEMarginalBinFinder[var_count];
		var_value_list = new ArrayList<OEMarginalDistRange>();

		for (int i = 0; i < var_count; ++i) {
			OEDiscreteRange range = var_range_list.get(i);
			if (range == null) {
				// for null range, use a single bin and empty values
				bin_finders[i] = new OEMarginalBinSingle();

				var_value_list.add ((new OEMarginalDistRange()).setup_empty_range (
					var_name_list.get(i),
					i
				));
			} else {
				bin_finders[i] = range.make_bin_finder (f_out_lo_list.get(i), f_out_hi_list.get(i));

				var_value_list.add ((new OEMarginalDistRange()).setup_range (
					var_name_list.get(i),
					i,
					range,
					f_out_lo_list.get(i),
					f_out_hi_list.get(i),
					var_format_list.get(i)
				));
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
			.set_grid_info (var_name_list, data_name_list, var_range_list, var_value_list)
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




	// Index numbers and names for variables in ETAS grid parameters (see OEGridParams).

	public static final int VMIX_B = 0;
	public static final int VMIX_ALPHA = 1;
	public static final int VMIX_C = 2;
	public static final int VMIX_P = 3;
	public static final int VMIX_N = 4;
	public static final int VMIX_ZAMS = 5;
	public static final int VMIX_ZMU = 6;

	public static final String VNAME_B = "b";
	public static final String VNAME_ALPHA = "alpha";
	public static final String VNAME_C = "c";
	public static final String VNAME_P = "p";
	public static final String VNAME_N = "n";
	public static final String VNAME_ZAMS = "zams";
	public static final String VNAME_ZMU = "zmu";




	// Add the variables for an ETAS grid.
	// Parameters:
	//  grid_params = Definition of the parameters.
	//  f_out = True to include bins for out-of-range variable values.
	// Note: To agree with the above index number, this must be the only (or at least the first) variables.

	public final void add_etas_vars (OEGridParams grid_params, boolean f_out) {
		add_var ("b", grid_params.b_range, f_out, f_out, "%.6e");
		add_var ("alpha", grid_params.alpha_range, f_out, f_out, "%.6e");
		add_var ("c", grid_params.c_range, f_out, f_out, "%.6e");
		add_var ("p", grid_params.p_range, f_out, f_out, "%.6e");
		add_var ("n", grid_params.n_range, f_out, f_out, "%.6e");
		add_var ("zams", grid_params.zams_range, f_out, f_out, "%.6e");
		add_var ("zmu", grid_params.zmu_range, f_out, f_out, "%.6e");
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




	// Index numbers and names for ETAS data, a single probability.

	public static final int DMIX_PROB = 0;

	public static final String DNAME_PROB = "likelihood";




	// Index numbers and names for ETAS data, generic, sequence specific, bayesian, and active.

	public static final int DMIX_GENERIC = 0;
	public static final int DMIX_SEQ_SPEC = 1;
	public static final int DMIX_BAYESIAN = 2;
	public static final int DMIX_ACTIVE = 3;

	public static final String DNAME_GENERIC = "generic";
	public static final String DNAME_SEQ_SPEC = "seqspec";
	public static final String DNAME_BAYESIAN = "bayesian";
	public static final String DNAME_ACTIVE = "active";




	// Index numbers and names for second set of ETAS data, generic, sequence specific, bayesian, and active.

	public static final int DMIX_GENERIC_2 = 4;
	public static final int DMIX_SEQ_SPEC_2 = 5;
	public static final int DMIX_BAYESIAN_2 = 6;
	public static final int DMIX_ACTIVE_2 = 7;

	public static final String DNAME_GENERIC_2 = "generic2";
	public static final String DNAME_SEQ_SPEC_2 = "seqspec2";
	public static final String DNAME_BAYESIAN_2 = "bayesian2";
	public static final String DNAME_ACTIVE_2 = "active2";




	// Add the data for ETAS, a single probability.

	public final void add_etas_data_prob () {
		add_data ("likelihood");
		return;
	}




	// Add the data for ETAS, generic, sequence specific, and bayesian.

	public final void add_etas_data_gen_seq_bay () {
		add_data ("generic");
		add_data ("seqspec");
		add_data ("bayesian");
		return;
	}




	// Add the data for ETAS, generic, sequence specific, bayesian, and active.

	public final void add_etas_data_gen_seq_bay_act () {
		add_data ("generic");
		add_data ("seqspec");
		add_data ("bayesian");
		add_data ("active");
		return;
	}




	// Add the data for ETAS, generic, sequence specific, bayesian, and active, including second set.

	public final void add_etas_data_gen_seq_bay_act_dual () {
		add_data ("generic");
		add_data ("seqspec");
		add_data ("bayesian");
		add_data ("active");
		add_data ("generic2");
		add_data ("seqspec2");
		add_data ("bayesian2");
		add_data ("active2");
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
		dist_w[DMIX_SEQ_SPEC] = seq;
		dist_w[DMIX_BAYESIAN] = bay;
		return;
	}




	// Set the data for ETAS, generic, sequence specific, bayesian, and active.

	public final void set_etas_data_gen_seq_bay_act (
		double gen,
		double seq,
		double bay,
		double act
	) {
		dist_w[DMIX_GENERIC] = gen;
		dist_w[DMIX_SEQ_SPEC] = seq;
		dist_w[DMIX_BAYESIAN] = bay;
		dist_w[DMIX_ACTIVE] = act;
		return;
	}




	// Set the data for ETAS, generic, sequence specific, bayesian, and active, including second set..

	public final void set_etas_data_gen_seq_bay_act_dual (
		double gen,
		double seq,
		double bay,
		double act,
		double gen2,
		double seq2,
		double bay2,
		double act2
	) {
		dist_w[DMIX_GENERIC] = gen;
		dist_w[DMIX_SEQ_SPEC] = seq;
		dist_w[DMIX_BAYESIAN] = bay;
		dist_w[DMIX_ACTIVE] = act;
		dist_w[DMIX_GENERIC_2] = gen2;
		dist_w[DMIX_SEQ_SPEC_2] = seq2;
		dist_w[DMIX_BAYESIAN_2] = bay2;
		dist_w[DMIX_ACTIVE_2] = act2;
		return;
	}




	// Finish accumulation, using ETAS options.
	// Returns the marginal distribution set.

	public final OEMarginalDistSet end_etas_accum () {
		//return end_accum (1000.0, 10000.0, "%.5e");
		return end_accum (1000.0, 1000.0, "%.5e");
	}




	//----- Special purpose: RJ model -----




	// Index numbers and names for variables in RJ grid parameters.

	public static final int VMIX_RJ_B = 0;
	public static final int VMIX_RJ_A = 1;
	public static final int VMIX_RJ_P = 2;
	public static final int VMIX_RJ_C = 3;

	public static final String VNAME_RJ_B = "b";
	public static final String VNAME_RJ_A = "a";
	public static final String VNAME_RJ_P = "p";
	public static final String VNAME_RJ_C = "c";




	// Index numbers and names for RJ data, a single probability.

	public static final int DMIX_RJ_PROB = 0;

	public static final String DNAME_RJ_PROB = "likelihood";




	// Make marginal distribution for an RJ model.
	// Parameters:
	//  rjmod = RJ model.
	//  f_bivar_marg = True if bivariate marginal distributions are desired.
	// Returns the marginal distribution set.

	public final OEMarginalDistSet make_rj_marginals (RJ_AftershockModel rjmod, boolean f_bivar_marg) {

		// Variable b

		final double value_b = rjmod.get_b();

		final OEDiscreteRange range_b = OEDiscreteRange.makeSingle (value_b);

		add_var ("b", range_b, false, false, "%.6e");

		// Variable a

		final double min_a = rjmod.getMin_a();
		final double max_a = rjmod.getMax_a();
		final int num_a = rjmod.getNum_a();

		final OEDiscreteRange range_a = ((num_a == 1) ? (OEDiscreteRange.makeSingle (min_a)) : (OEDiscreteRange.makeLinear (num_a, min_a, max_a)));
		final double[] values_a = range_a.get_range_array();

		add_var ("a", range_a, false, false, "%.6e");

		// Variable p

		final double min_p = rjmod.getMin_p();
		final double max_p = rjmod.getMax_p();
		final int num_p = rjmod.getNum_p();

		final OEDiscreteRange range_p = ((num_p == 1) ? (OEDiscreteRange.makeSingle (min_p)) : (OEDiscreteRange.makeLinear (num_p, min_p, max_p)));
		final double[] values_p = range_p.get_range_array();

		add_var ("p", range_p, false, false, "%.6e");

		// Variable c

		final double min_c = rjmod.getMin_c();
		final double max_c = rjmod.getMax_c();
		final int num_c = rjmod.getNum_c();

		final OEDiscreteRange range_c = ((num_c == 1) ? (OEDiscreteRange.makeSingle (min_c)) : (OEDiscreteRange.makeLinear (num_c, min_c, max_c)));
		final double[] values_c = range_c.get_range_array();

		add_var ("c", range_c, false, false, "%.6e");

		// Data likelihood

		add_data ("likelihood");

		// Begin accumulation

		begin_accum (f_bivar_marg);

		// Set b-value

		set_var (VMIX_RJ_B, value_b);

		// Loop over combinations of a/p/c, setting the data values

		for (int aIndex = 0; aIndex < num_a; aIndex++) {
			set_var (VMIX_RJ_A, values_a[aIndex]);

			for (int pIndex = 0; pIndex < num_p; pIndex++) {
				set_var (VMIX_RJ_P, values_p[pIndex]);

				for (int cIndex = 0; cIndex < num_c; cIndex++) {
					set_var (VMIX_RJ_C, values_c[cIndex]);

					set_data_and_accum (DMIX_RJ_PROB, rjmod.get_apc_prob (aIndex, pIndex, cIndex));
				}
			}
		}

		// End accumulation and return the marginal distribution set

		return end_accum (1000.0, 1000.0, "%.5e");
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
