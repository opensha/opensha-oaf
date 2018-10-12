package scratch.aftershockStatistics.gamma;

import scratch.aftershockStatistics.ComcatAccessor;

import scratch.aftershockStatistics.util.SimpleUtils;

import scratch.aftershockStatistics.aafs.ActionConfig;

import org.apache.commons.math3.distribution.UniformRealDistribution;



/**
 * Gamma configuration information.
 * Author: Michael Barall 10/08/2018.
 */
public class GammaConfig {

	//----- Bin structure -----

	// Number of forecast lags.

	public int forecast_lag_count;

	// Array of forecast lags, in increasing order, length is forecast_lag_count.

	public long[] forecast_lags;

	// Maximum forecast lag, equals last element of forecast_lags.

	public long max_forecast_lag;

	// Number of model kinds.

	public int model_kind_count;

	// Array of model kinds, length is model_kind_count.

	public int[] model_kinds;

	// Values for model kinds.

	public static final int MODEL_KIND_GENERIC = 0;
	public static final int MODEL_KIND_SEQ_SPEC = 1;
	public static final int MODEL_KIND_BAYESIAN = 2;

	// Number of advisory forecast windows.

	public int adv_window_count;

	// Array of advisory forecast window start offsets, length is adv_window_count.

	public long[] adv_window_start_offs;

	// Array of advisory forecast window end offsets, length is adv_window_count.

	public long[] adv_window_end_offs;

	// Array of advisory forecast window names, length is adv_window_count.

	public String[] adv_window_names;

	// Maximum end offset for any advisory forecast window, equals largest element of adv_window_end_offs.

	public long max_adv_window_end_off;

	// Number of advisory magnitude bins.

	public int adv_min_mag_bin_count;

	// Array of minimum magnitudes for advisory magnitude bins, in increasing order.

	public double[] adv_min_mag_bins;




	//----- Simulation parameters -----

	// Number of simulations per forecast and model.

	public int simulation_count;

	// The random number generator to use for simulations.

	public UniformRealDistribution rangen;

	// Offset from rupture at which simulation starts, in milliseconds.

	public long sim_start_off;

	// Number of simulation slots to use when summing over earthquakes.

	public int eqk_summation_count;

	// True to randomize slots when summing over earthquakes.

	public boolean eqk_summation_randomize;

	// True to discard simulations with an aftershock larger than the mainshock.

	public boolean discard_sim_with_large_as;





	//----- Construction -----

	// Constructor.

	public GammaConfig () {

		// Get configuration

		ActionConfig action_config = new ActionConfig();

		// Fill in bin structure

		forecast_lags = action_config.get_forecast_lag_array (0L);
		forecast_lag_count = forecast_lags.length;
		max_forecast_lag = forecast_lags[forecast_lag_count - 1];

		model_kind_count = 3;
		model_kinds = new int[model_kind_count];
		model_kinds[0] = MODEL_KIND_GENERIC;
		model_kinds[1] = MODEL_KIND_SEQ_SPEC;
		model_kinds[2] = MODEL_KIND_BAYESIAN;

		adv_window_count = action_config.get_adv_window_count();
		adv_window_start_offs = action_config.get_adv_window_start_offs_array();
		adv_window_end_offs = action_config.get_adv_window_end_offs_array();
		adv_window_names = action_config.get_adv_window_names_array();
		max_adv_window_end_off = action_config.get_max_adv_window_end_off();

		adv_min_mag_bin_count = action_config.get_adv_min_mag_bin_count();
		adv_min_mag_bins = action_config.get_adv_min_mag_bins_array();

		// Fill in simulation parameters

		simulation_count = 1000;
		rangen = new UniformRealDistribution();
		sim_start_off = 60000L;

		eqk_summation_count = 10000;
		eqk_summation_randomize = true;
		discard_sim_with_large_as = true;
	}




	// Convert a model kind to a string.

	public String model_kind_to_string (int kind) {
		switch (kind) {
			case MODEL_KIND_GENERIC: return "Generic";
			case MODEL_KIND_SEQ_SPEC: return "Seq-Spec";
			case MODEL_KIND_BAYESIAN: return "Bayesian";
		}
		return "Unknown(" + kind + ")";
	}




	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append ("ActionConfigFile:" + "\n");

		result.append ("forecast_lag_count = " + forecast_lag_count + "\n");

		result.append ("forecast_lags = [" + "\n");
		for (int i = 0; i < forecast_lags.length; ++i) {
			result.append ("  " + i + ":  " + SimpleUtils.duration_raw_and_string (forecast_lags[i]) + "\n");
		}
		result.append ("]" + "\n");

		result.append ("max_forecast_lag = " + max_forecast_lag + "\n");
		result.append ("model_kind_count = " + model_kind_count + "\n");

		result.append ("model_kinds = [" + "\n");
		for (int i = 0; i < model_kinds.length; ++i) {
			result.append ("  " + i + ":  " + model_kinds[i] + " (" + model_kind_to_string(model_kinds[i]) + ")" + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_window_count = " + adv_window_count + "\n");

		result.append ("adv_window_start_offs = [" + "\n");
		for (int i = 0; i < adv_window_start_offs.length; ++i) {
			result.append ("  " + i + ":  " + SimpleUtils.duration_raw_and_string (adv_window_start_offs[i]) + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_window_end_offs = [" + "\n");
		for (int i = 0; i < adv_window_end_offs.length; ++i) {
			result.append ("  " + i + ":  " + SimpleUtils.duration_raw_and_string (adv_window_end_offs[i]) + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_window_names = [" + "\n");
		for (int i = 0; i < adv_window_names.length; ++i) {
			result.append ("  " + i + ":  " + adv_window_names[i] + "\n");
		}
		result.append ("]" + "\n");

		result.append ("adv_min_mag_bin_count = " + adv_min_mag_bin_count + "\n");

		result.append ("adv_min_mag_bins = [" + "\n");
		for (int i = 0; i < adv_min_mag_bins.length; ++i) {
			result.append ("  " + i + ":  " + adv_min_mag_bins[i] + "\n");
		}
		result.append ("]" + "\n");

		result.append ("simulation_count = " + simulation_count + "\n");
		result.append ("sim_start_off = " + SimpleUtils.duration_raw_and_string (sim_start_off) + "\n");
		result.append ("eqk_summation_count = " + eqk_summation_count + "\n");
		result.append ("eqk_summation_randomize = " + eqk_summation_randomize + "\n");

		return result.toString();
	}




	// Return the advisory forecast window name, or "Sum-Win" if the index equals adv_window_count.
	// This is used for naming an extra element representing the sum over windows.

	public String get_adv_window_name_or_sum (int i) {
		return ((i == adv_window_count) ? "Sum-Win" : adv_window_names[i]);
	}




	// Return the forecast lag as a string, or "Sum-Lag" if the index equals forecast_lag_count.
	// This is used for naming an extra element representing the sum over forecast lags.

	public String get_forecast_lag_string_or_sum (int i) {
		return ((i == forecast_lag_count) ? "Sum-Lag" : SimpleUtils.duration_to_string (forecast_lags[i]));
	}




	//----- Testing -----

	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("GammaConfig : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Create an object, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Zero additional argument

			if (args.length != 1) {
				System.err.println ("GammaConfig : Invalid 'test1' subcommand");
				return;
			}

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();

			// Display it

			System.out.println (gamma_config.toString());

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("GammaConfig : Unrecognized subcommand : " + args[0]);
		return;
	}
}
