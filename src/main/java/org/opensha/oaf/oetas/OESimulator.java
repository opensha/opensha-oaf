package org.opensha.oaf.oetas;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.time.Instant;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SimpleThreadLoopResult;

import org.opensha.oaf.oetas.util.OEArraysCalc;
import org.opensha.oaf.oetas.util.OEDiscreteRange;

import org.opensha.oaf.oetas.except.OEException;
import org.opensha.oaf.oetas.except.OERangeConvergenceException;
import org.opensha.oaf.oetas.except.OERangeException;
import org.opensha.oaf.oetas.except.OERangeThreadAbortException;
import org.opensha.oaf.oetas.except.OERangeTimeoutException;
import org.opensha.oaf.oetas.except.OESimException;
import org.opensha.oaf.oetas.except.OESimThreadAbortException;
import org.opensha.oaf.oetas.except.OESimTimeoutException;

import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_ForecastModel;
import org.opensha.oaf.rj.USGS_ForecastException;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;


// Class for producing and accumulating simulations of operational ETAS catalogs.
// Author: Michael Barall 03/14/2022.
//
// Inputs to this class are an initializer, simulation parameters, and an executor.
// The outputs are a completed accumulator, forecast grid, and range.

public class OESimulator {

	//----- Inputs -----

	// The initializer to use for simulations.

	public OEEnsembleInitializer sim_initializer;

	// The simulation parameters.

	public OESimulationParams sim_parameters;

	// The executor.

	public AutoExecutorService sim_executor;

	// The execution timer, or null if none has been supplied.

	public SimpleExecTimer sim_exec_timer;

	//----- Outputs -----

	// The accumulator that holds the results of all the simulations.

	public OEEnsembleAccumulator sim_accumulator;

	// The forecast grid obtained from the accumulator.

	public OEForecastGrid sim_forecast_grid;

	// The range used for the simulation.

	public OECatalogRange sim_catalog_range;

	// The number of simulations performed.

	public int sim_count;

	//----- Intermediate -----

	// The accumulator used for ranging the simulations.

	public OEEnsembleAccumulator range_accumulator;

	// The loop result.

	private SimpleThreadLoopResult loop_result = new SimpleThreadLoopResult();




	//----- Construction -----




	// Erase the contents.

	public final void clear () {
		sim_initializer = null;
		sim_parameters = null;
		sim_executor = null;
		sim_exec_timer = null;

		sim_accumulator = null;
		sim_forecast_grid = null;
		sim_catalog_range = null;
		sim_count = 0;

		range_accumulator = null;
		return;
	}




	// Default constructor.

	public OESimulator () {
		clear();
	}




	//----- Execution -----




	// Set up inputs.
	// Save the inputs, and set up the initial forecast grid, range, and time/magnitude bins.

	private void do_setup_inputs (
		OEEnsembleInitializer the_sim_initializer,
		OESimulationParams the_sim_parameters,
		AutoExecutorService the_sim_executor
	) {

		// Save the inputs

		sim_initializer = the_sim_initializer;
		sim_parameters = the_sim_parameters;
		sim_executor = the_sim_executor;
		sim_exec_timer = null;

		// Set up the forecast grid, with advisory and mainshock magnitude

		sim_forecast_grid = new OEForecastGrid();

		// Advisory settings

		sim_forecast_grid.setup_advisory (sim_initializer.get_custom_min_mag_bins());

		// Mainshock settings, if initializer has one

		if (sim_initializer.has_mainshock_mag()) {
			sim_forecast_grid.setup_mainshock (sim_initializer.get_mainshock_mag());
		}

		// Get range from initializer (we only use the start time)

		sim_catalog_range = sim_initializer.get_initial_range();

		return;
	}




	// Set up inputs.
	// Save the inputs, and set up the initial forecast grid, range, and time/magnitude bins.

	private void do_setup_inputs (
		OEEnsembleInitializer the_sim_initializer,
		OESimulationParams the_sim_parameters,
		SimpleExecTimer the_sim_exec_timer
	) {

		// Save the inputs

		sim_initializer = the_sim_initializer;
		sim_parameters = the_sim_parameters;
		sim_executor = the_sim_exec_timer.get_executor();
		sim_exec_timer = the_sim_exec_timer;

		// Set up the forecast grid, with advisory and mainshock magnitude

		sim_forecast_grid = new OEForecastGrid();

		// Advisory settings

		sim_forecast_grid.setup_advisory (sim_initializer.get_custom_min_mag_bins());

		// Mainshock settings, if initializer has one

		if (sim_initializer.has_mainshock_mag()) {
			sim_forecast_grid.setup_mainshock (sim_initializer.get_mainshock_mag());
		}

		// Get range from initializer (we only use the start time)

		sim_catalog_range = sim_initializer.get_initial_range();

		return;
	}




	// Run the simulation.
	// Throws exception in case of failure.

	private void do_run_simulation () throws OEException {

		// Get the forecast time from the initializer

		final double t_forecast = sim_initializer.get_t_forecast();

		// Maximum runtime and progress message time

		final long max_runtime = ((sim_exec_timer == null) ? sim_parameters.sim_max_runtime : sim_exec_timer.get_remaining_time());
		final long progress_time = ((sim_exec_timer == null) ? sim_parameters.sim_progress_time : sim_exec_timer.get_progress_time());

		if (SimpleExecTimer.compare_remaining_time (max_runtime, 2000L) < 0) {
			String msg = "Insufficient time remaining to begin simulation";
			System.out.println ();
			System.out.println (msg);
			throw new OESimTimeoutException ("OESimulator.do_run_simulation: " + msg);
		}

		// Say hello

		System.out.println ();
		System.out.println ("Running ETAS simulation");
		System.out.println ();
		System.out.println (
			"Generating "
			+ sim_parameters.sim_num_catalogs
			+ " catalogs, using "
			+ sim_executor.get_num_threads()
			+ " threads, with "
			+ ((max_runtime < 0L) ? ("unlimited runtime") : (((max_runtime + 500L) / 1000L) + " seconds maximum runtime"))
		);
		System.out.println ();
		System.out.println ("Simulation range:");
		System.out.println (sim_catalog_range.progress_string());

		// Get the time and magnitude bins, starting at the time of the forecast

		double[] time_values = sim_forecast_grid.get_time_values (t_forecast);
		double[] mag_values = sim_forecast_grid.get_mag_values();

		// Clip the range to the last time value, and insert into initializer

		sim_catalog_range.clip_tend (time_values[time_values.length - 1]);
		sim_initializer.set_range (sim_catalog_range);

		// Allocate and set up the desired accumulator

		switch (sim_parameters.sim_accum_selection) {

		default:
			throw new IllegalArgumentException ("OESimulator.do_run_simulation: Invalid accumulator selection: sim_accum_selection = " + sim_parameters.sim_accum_selection);

		case OEConstants.SEL_ACCUM_CUM_TIME_MAG: {
			System.out.println ("Using accumulator: OEAccumCumTimeMag");
			System.out.println ("Accumulator option: " + sim_parameters.sim_accum_option + " (" + OEConstants.get_infill_method_as_string(sim_parameters.sim_accum_option) + ")");
			OEAccumCumTimeMag accum = new OEAccumCumTimeMag();
			accum.setup (sim_parameters.sim_accum_option, time_values, mag_values);
			sim_accumulator = accum;
		}
		break;

		case OEConstants.SEL_ACCUM_VAR_TIME_MAG: {
			System.out.println ("Using accumulator: OEAccumVarTimeMag");
			System.out.println ("Accumulator option: " + sim_parameters.sim_accum_option + " (" + OEConstants.get_infill_method_as_string(sim_parameters.sim_accum_option) + ")");
			OEAccumVarTimeMag accum = new OEAccumVarTimeMag();
			accum.setup (sim_parameters.sim_accum_option, time_values, mag_values);
			sim_accumulator = accum;
		}
		break;

		case OEConstants.SEL_ACCUM_RATE_TIME_MAG: {
			System.out.println ("Using accumulator: OEAccumRateTimeMag");
			System.out.println ("Accumulator option: " + sim_parameters.sim_accum_option + " (" + OEConstants.get_rate_acc_meth_as_string(sim_parameters.sim_accum_option) + ")");
			System.out.println ("Accumulator parameter (upfill_sec_reduce): " + sim_parameters.sim_accum_param_1);
			OEAccumRateTimeMag accum = new OEAccumRateTimeMag();
			accum.setup (null, sim_parameters.sim_accum_option, time_values, mag_values);
			accum.set_upfill_sec_reduce (sim_parameters.sim_accum_param_1);
			sim_accumulator = accum;
		}
		break;

		}

		// Create the list of accumulators

		ArrayList<OEEnsembleAccumulator> accumulators = new ArrayList<OEEnsembleAccumulator>();
		accumulators.add (sim_accumulator);

		// Set up the ensemble parameters

		OEEnsembleParams ensemble_params = new OEEnsembleParams();

		ensemble_params.set (
			sim_initializer,					// initializer
			accumulators,						// accumulators
			sim_parameters.sim_num_catalogs		// num_catalogs
		);

		// Create the ensemble generator

		OEEnsembleGenerator ensemble_generator = new OEEnsembleGenerator();
		
		// Set our loop results to be an upstream

		ensemble_generator.set_upstream_loop_result (loop_result);

		// Generate the catalogs

		int catalog_count = ensemble_generator.generate_all_catalogs (ensemble_params, sim_executor, max_runtime, progress_time);
		sim_count = catalog_count;

		// Accumulate loop results

		//loop_result.accum_from (ensemble_generator.get_loop_result());

		// Error checks

		if (catalog_count < 0) {
			String msg = "Simulation failed due to thread abort";
			System.out.println (msg);
			throw new OESimThreadAbortException ("OESimulator.do_run_simulation: " + msg + ": " + ensemble_generator.get_status_msg());
		}

		if (catalog_count < sim_parameters.sim_min_num_catalogs) {
			String msg = "Simulation failed due to insufficient number of catalogs generated within time limit: obtained = " + catalog_count + ", required = " + sim_parameters.sim_min_num_catalogs;
			System.out.println (msg);
			throw new OESimTimeoutException ("OESimulator.do_run_simulation: " + msg + ": " + ensemble_generator.get_status_msg());
		}

		// Transfer results to forecast grid

		sim_forecast_grid.supply_results ((OEAccumReadoutTimeMag)sim_accumulator);

		// Add model parameters to forecast grid

		LinkedHashMap<String, Object> display_params = new LinkedHashMap<String, Object>();
		sim_initializer.get_display_params (display_params);

		display_params.put (OEConstants.MKEY_SIM_COUNT, catalog_count);
		display_params.put (OEConstants.MKEY_SIM_DURATION,
			SimpleUtils.round_double_via_string ("%.1f", sim_catalog_range.tend - sim_catalog_range.tbegin));
		if (sim_catalog_range.is_fixed_mag_min()) {
			display_params.put (OEConstants.MKEY_SIM_MAG_MIN,
				SimpleUtils.round_double_via_string ("%.2f", sim_catalog_range.mag_min_sim));
		}
		if (sim_catalog_range.is_fixed_mag_max()) {
			display_params.put (OEConstants.MKEY_SIM_MAG_MAX,
				SimpleUtils.round_double_via_string ("%.2f", sim_catalog_range.mag_max_sim));
		}

		sim_forecast_grid.add_model_param (display_params);

		// Display results

		System.out.println ();
		System.out.println ("ETAS simulation results");
		System.out.println ();

		System.out.println (sim_forecast_grid.summary_string());

		return;
	}




	// Run the ranging.
	// Throws exception in case of failure.

	private void do_run_ranging () throws OEException {

		// Switch on ranging method

		switch (sim_parameters.range_method) {

		default:
			throw new IllegalArgumentException ("OESimulator.do_run_ranging: Invalid ranging method: range_method = " + sim_parameters.range_method);

		case OEConstants.RANGING_METH_SIM:
			do_run_ranging_v1();
			break;

		case OEConstants.RANGING_METH_SEED_EST:
			do_run_ranging_v2();
			break;

		case OEConstants.RANGING_METH_VAR_SEED_EST:
			do_run_ranging_v3();
			break;

		}

		return;
	}




	// Run the ranging, version 1.
	// Throws exception in case of failure.

	private void do_run_ranging_v1 () throws OEException {

		// Get the forecast time from the initializer

		final double t_forecast = sim_initializer.get_t_forecast();

		// Maximum runtime and progress message time

		final long max_runtime = ((sim_exec_timer == null) ? sim_parameters.range_max_runtime : sim_exec_timer.get_frac_remaining_time (sim_parameters.range_exec_time_frac));
		final long progress_time = ((sim_exec_timer == null) ? sim_parameters.range_progress_time : sim_exec_timer.get_progress_time());

		if (SimpleExecTimer.compare_remaining_time (max_runtime, 1000L) < 0) {
			String msg = "Insufficient time remaining to begin ranging";
			System.out.println ();
			System.out.println (msg);
			throw new OERangeTimeoutException ("OESimulator.do_run_ranging_v1: " + msg);
		}

		// Get the time and magnitude bins, starting at the time of the forecast

		final double[] ranging_time_values = sim_forecast_grid.get_ranging_time_values (t_forecast);

		// Get the b-value and scaling magnitude from the initializer

		final double b_value = sim_initializer.get_b_value();
		final double scaling_mag = sim_initializer.get_mainshock_mag();

		// Get the simulation start time from the existing range

		final double tbegin = sim_catalog_range.tbegin;

		// Set up a default range

		sim_catalog_range.set_range_fixed (
			tbegin,															// tbegin
			ranging_time_values[ranging_time_values.length - 1],			// tend
			scaling_mag + sim_parameters.range_min_rel_mag,					// mag_min_sim
			scaling_mag + sim_parameters.range_max_rel_mag,					// mag_max_sim
			OEConstants.DEF_MAG_EXCESS										// mag_excess
		);

		// Write the default range into the initializer

		sim_initializer.set_range (sim_catalog_range);

		// If no ranging is desired, just use the default range

		if (sim_parameters.range_accum_selection == OEConstants.SEL_ACCUM_NONE) {
			System.out.println ();
			System.out.println ("Using default range");
			return;
		}

		// Loop for ranging attempts

		int attempt = 0;
		int max_attempts = sim_parameters.range_max_attempts;

		boolean f_ranging = true;

		while (f_ranging) {

			// Count the attempt

			++attempt;
			if (attempt > max_attempts) {
				String msg = "Ranging failed due to reaching maximum number of attempts: max_attempts = " + max_attempts;
				System.out.println ();
				System.out.println (msg);
				System.out.println ();
				System.out.println ("Final range:");
				System.out.println (sim_catalog_range.progress_string());
				throw new OERangeConvergenceException ("OESimulator.do_run_ranging_v1: " + msg);
			}

			// Check if there is time to continue

			if (sim_exec_timer != null) {
				if (!( sim_exec_timer.has_remaining_time (max_runtime) )) {
					String msg = "Insufficient time remaining to continue ranging";
					System.out.println ();
					System.out.println (msg);
					throw new OERangeTimeoutException ("OESimulator.do_run_ranging_v1: " + msg);
				}
			}

			// Say hello

			System.out.println ();
			System.out.println ("Running ETAS ranging, using trial simulation method, attempt = " + attempt);
			System.out.println ();
			System.out.println (
				"Generating "
				+ sim_parameters.eff_range_num_catalogs()
				+ " catalogs, using "
				+ sim_executor.get_num_threads()
				+ " threads, with "
				+ ((max_runtime < 0L) ? ("unlimited runtime") : (((max_runtime + 500L) / 1000L) + " seconds maximum runtime"))
			);
			System.out.println ();
			System.out.println ("Current range:");
			System.out.println (sim_catalog_range.progress_string());

			// Allocate and set up the desired accumulator

			switch (sim_parameters.range_accum_selection) {

			default:
				throw new IllegalArgumentException ("OESimulator.do_run_ranging_v1: Invalid accumulator selection: range_accum_selection = " + sim_parameters.range_accum_selection);

			case OEConstants.SEL_ACCUM_SIM_RANGING: {
				System.out.println ("Using accumulator: OEAccumSimRanging");
				System.out.println ("Accumulator option: " + sim_parameters.range_accum_option);
				OEAccumSimRanging accum = new OEAccumSimRanging();
				accum.setup (sim_parameters.range_accum_option, ranging_time_values);
				range_accumulator = accum;
			}
			break;

			}

			// Create the list of accumulators

			ArrayList<OEEnsembleAccumulator> accumulators = new ArrayList<OEEnsembleAccumulator>();
			accumulators.add (range_accumulator);

			// Set up the ensemble parameters

			OEEnsembleParams ensemble_params = new OEEnsembleParams();

			ensemble_params.set (
				sim_initializer,					// initializer
				accumulators,						// accumulators
				sim_parameters.eff_range_num_catalogs()	// num_catalogs
			);

			// Create the ensemble generator

			OEEnsembleGenerator ensemble_generator = new OEEnsembleGenerator();
		
			// Set our loop results to be an upstream

			ensemble_generator.set_upstream_loop_result (loop_result);

			// Generate the catalogs

			int catalog_count = ensemble_generator.generate_all_catalogs (ensemble_params, sim_executor, max_runtime, progress_time);

			// Accumulate loop results

			//loop_result.accum_from (ensemble_generator.get_loop_result());

			// Error checks

			if (catalog_count < 0) {
				String msg = "Ranging failed due to thread abort";
				System.out.println (msg);
				throw new OERangeThreadAbortException ("OESimulator.do_run_ranging_v1: " + msg + ": " + ensemble_generator.get_status_msg());
			}

			if (catalog_count < sim_parameters.eff_range_min_num_catalogs() && catalog_count < sim_executor.get_num_threads()) {
				String msg = "Ranging failed due to insufficient number of catalogs generated within time limit: obtained = " + catalog_count + ", required = " + sim_parameters.eff_range_min_num_catalogs();
				System.out.println (msg);
				throw new OERangeTimeoutException ("OESimulator.do_run_ranging_v1: " + msg + ": " + ensemble_generator.get_status_msg());
			}

			// If too few catalogs, but we produced at least one per thread ...

			if (catalog_count < sim_parameters.eff_range_min_num_catalogs()) {

				System.out.println ();
				System.out.println ("Number of catalogs is below minimum required: catalog_count = " + catalog_count + ", minimum required = " + sim_parameters.eff_range_min_num_catalogs());

				// Ratio to reduce catalog size so the count increases to twice the target
				// number of catalogs, but not more than a factor of 10

				double r = Math.max (0.10, 0.5 * ((double)catalog_count)) / ((double)sim_parameters.eff_range_num_catalogs());
				System.out.println ("Catalog size adjustment ratio = " + r);

				// Rescale the range

				sim_catalog_range.set_rescaled_min_mag (b_value, r);
			}

			// Otherwise, there are enough catalogs to examine the results

			else {

				// The number of time bins with our desired survival rate

				int survival_bins = ((OEAccumReadoutRanging)range_accumulator).get_survival_bins (sim_parameters.range_exceed_fraction);

				// The survival time and duration

				double survival_time = ranging_time_values[survival_bins];
				double survival_duration = survival_time - ranging_time_values[0];		// ranging_time_values[0] == t_forecast

				System.out.println ();
				System.out.println ("Catalog survival duration = " + survival_duration + " days, with exceedence fraction = " + sim_parameters.range_exceed_fraction);

				// Check for minimum required duration

				if (survival_duration + OEConstants.GEN_TIME_EPS < sim_parameters.range_min_duration) {
					String msg = "Catalog survival duration is too small: survival_duration = " + survival_duration + ", required minimum = " + sim_parameters.range_min_duration;
					System.out.println (msg);
					throw new OERangeConvergenceException ("OESimulator.do_run_ranging_v1: " + msg);
				}

				if (survival_bins == 0) {		// would indicate catalogs don't survive to the start of the forecast (very unlikely)
					String msg = "Catalog survival duration is before start of forecast";
					System.out.println (msg);
					throw new OERangeConvergenceException ("OESimulator.do_run_ranging_v1: " + msg);
				}

				// The survival catalog size, for the fractile we are interested in

				int survival_size = ((OEAccumReadoutRanging)range_accumulator).get_bin_fractile (survival_bins - 1, sim_parameters.range_target_fractile);
				System.out.println ("Catalog size = " + survival_size + " at survival duration, with fractile = " + sim_parameters.range_target_fractile);

				// Ratio to change to target size

				double r = ((double)sim_parameters.range_target_size) / ((double)(Math.max(survival_size, 1)));
				System.out.println ("Catalog size adjustment ratio = " + r);

				// Limit change to a factor of 10, stop ranging if within 20%

				if (r > 10.0) {
					r = 10.0;
				} else if (r < 0.10) {
					r = 0.10;
				} else if (attempt > 1 && r >= 0.80 && r <= 1.20) {
					f_ranging = false;
				}

				// If still ranging, rescale the minimum magnitude ...
				
				if (f_ranging) {

					// If still ranging, rescale the magnitude range

					System.out.println ("Adjusting magnitude range minimum");
					System.out.println ("Catalog size adjustment ratio, clipped = " + r);

					sim_catalog_range.set_rescaled_min_mag (b_value, r);

				// Otherwise, rescale the time range and possibly the maximum magnitude ...

				} else {

					// Adjust simulation end time, if this is the last ranging

					System.out.println ("Adjusting time range, new end time = " + survival_time + ", existing end time = " + sim_catalog_range.tend);

					sim_catalog_range.tend = survival_time;

					// If no maximum magnitude adjustment ...

					if (sim_parameters.range_mag_lim_fraction < 1.0e-6) {
						System.out.println ("Maximum magnitude adjustment is not selected");
					}

					// Otherwise, attempt adjustment ...

					else {

						// Get the number of the time bin to use for checking

						int check_bin = OEArraysCalc.bsearch_array (ranging_time_values, ranging_time_values[0] + sim_parameters.range_mag_lim_time - OEConstants.GEN_TIME_EPS, 1, ranging_time_values.length - 1) - 1;

						double check_time = ranging_time_values[check_bin + 1];
						double check_duration = check_time - ranging_time_values[0];

						System.out.println ("Maximum magnitude check duration = " + check_duration + " days");

						if (check_bin >= survival_bins) {
							String msg = "Maximum magnitude check time is after catalog survival time: survival_duration = " + survival_duration + ", check_duration = " + check_duration;
							System.out.println (msg);
							throw new OERangeConvergenceException ("OESimulator.do_run_ranging_v1: " + msg);
						}

						// Among catalogs that complete the survival bins, get the desired fractile of highest magnitude

						double high_mag = ((OEAccumReadoutRanging)range_accumulator).get_sel_high_mag_fractile (check_bin, 1.0 - sim_parameters.range_mag_lim_fraction, survival_bins - 1);
						System.out.println ("High magnitude = " + high_mag + ", with exceedence fraction = " + sim_parameters.range_mag_lim_fraction);
						
						double new_mag_max = high_mag + 1.0;

						if (new_mag_max >= sim_catalog_range.mag_max_sim) {
							System.out.println ("No adjustment of maximum magnitude, proposed new max mag = " + new_mag_max + ", existing max mag = " + sim_catalog_range.mag_max_sim);
						} else if (new_mag_max < sim_catalog_range.mag_min_sim + 1.0) {
							String msg = "New maximum magnitude is too close to minimum magnitude, proposed new max mag = " + new_mag_max + ", existing min mag = " + sim_catalog_range.mag_min_sim;
							System.out.println (msg);
							throw new OERangeConvergenceException ("OESimulator.do_run_ranging_v1: " + msg);
						} else {
							System.out.println ("Adjusting magnitude range maximum, new max mag = " + new_mag_max + ", existing max mag = " + sim_catalog_range.mag_max_sim);
							sim_catalog_range.mag_max_sim = new_mag_max;
						}
					}
				}
			}

			// Write the new range into the initializer

			sim_initializer.set_range (sim_catalog_range);

		}	// end while

		// Forget the ranging accumulator

		//range_accumulator = null;

		// Say goodbye

		System.out.println ();
		System.out.println ("Completed ETAS ranging, in " + attempt + " attempts");

		return;
	}




	// Find the last element of an array which is at least a given threshold.
	// Parameters:
	//  x = Threshold.
	//  values = Array of data values.
	// Returns the largest index j such that values[j] >= x.
	// Returns -1 if no element of values is >= x.
	// Implementation note:  Uses linear search.  Although intended for arrays that are
	// sorted in decreasing order, we don't use binary search so it can be applied to arrays
	// that might be slightly out of order due to rounding.

	private int find_last_exceeder (double x, double[] values) {
		int j = values.length - 1;
		while (j >= 0 && values[j] < x) {
			--j;
		}
		return j;
	}

	private int find_last_exceeder (int x, int[] values) {
		int j = values.length - 1;
		while (j >= 0 && values[j] < x) {
			--j;
		}
		return j;
	}




	// Run the ranging, version 2.
	// Throws exception in case of failure.

	private void do_run_ranging_v2 () throws OEException {

		// Get the forecast time from the initializer

		final double t_forecast = sim_initializer.get_t_forecast();

		// Maximum runtime and progress message time (just use half the simulation time; we should be much faster than that)

		final long max_runtime = ((sim_exec_timer == null) ? sim_parameters.sim_max_runtime : sim_exec_timer.get_frac_remaining_time (0.5));
		final long progress_time = ((sim_exec_timer == null) ? sim_parameters.sim_progress_time : sim_exec_timer.get_progress_time());

		if (SimpleExecTimer.compare_remaining_time (max_runtime, 1000L) < 0) {
			String msg = "Insufficient time remaining to begin ranging";
			System.out.println ();
			System.out.println (msg);
			throw new OERangeTimeoutException ("OESimulator.do_run_ranging_v2: " + msg);
		}

		// Get the time and magnitude bins, starting at the time of the forecast (we really only need the ending time)

		final double[] ranging_time_values = sim_forecast_grid.get_ranging_time_values (t_forecast);

		// Get the b-value and scaling magnitude from the initializer

		final double b_value = sim_initializer.get_b_value();
		final double scaling_mag = sim_initializer.get_mainshock_mag();

		// Get the simulation start time from the existing range

		final double tbegin = sim_catalog_range.tbegin;

		// Set up a default range (the magnitude range here is not used, so we just use the version #1 values as a default)

		sim_catalog_range.set_range_fixed (
			tbegin,															// tbegin
			ranging_time_values[ranging_time_values.length - 1],			// tend
			scaling_mag + sim_parameters.range_min_rel_mag,					// mag_min_sim
			scaling_mag + sim_parameters.range_max_rel_mag,					// mag_max_sim
			sim_parameters.ranv2_mag_excess									// mag_excess
		);

		// Write the default range into the initializer

		sim_initializer.set_range (sim_catalog_range);

		// If no ranging is desired, just use the default range

		if (sim_parameters.range_accum_selection == OEConstants.SEL_ACCUM_NONE) {
			System.out.println ();
			System.out.println ("Using default range");
			return;
		}

		// Set the initializer so only seeds are generated

		final OECatalogLimits old_limits = sim_initializer.get_limits();
		final OECatalogLimits new_limits = sim_initializer.get_limits().set_seed_only();
		sim_initializer.set_limits (new_limits);

		// Say hello

		System.out.println ();
		System.out.println ("Running ETAS ranging, using seed estimation technique");
		System.out.println ();
		System.out.println ("Current range:");
		System.out.println (sim_catalog_range.progress_string());

		// Ranges of generation and magnitude

		double[] the_mag_values;
		int[] the_gen_values;

		// Allocate and set up the desired accumulator

		switch (sim_parameters.ranv2_accum_selection) {

		default:
			throw new IllegalArgumentException ("OESimulator.do_run_ranging_v2: Invalid accumulator selection: ranv2_accum_selection = " + sim_parameters.range_accum_selection);

		case OEConstants.SEL_ACCUM_SEED_EST_RANGING: {
			System.out.println ("Using accumulator: OEAccumSeedEstRanging");
			System.out.println ("Accumulator option: " + sim_parameters.ranv2_accum_option);
			OEAccumSeedEstRanging accum = new OEAccumSeedEstRanging();

			// For branch ratio time interval, use the time range

			final double the_tint_br = sim_catalog_range.tend - sim_catalog_range.tbegin;

			// Branch ratio de-rating from parameters

			final double the_derate_br = sim_parameters.ranv2_derate_br;

			// Generation list is just direct aftershocks of the seeds plus the paremter value

			the_gen_values = new int[2];
			the_gen_values[0] = 2;
			the_gen_values[1] = sim_parameters.ranv2_gen_br;

			// Magnitude range, currently a list of absolute magnitudes

			final double mag_range_min = sim_parameters.ranv2_min_mag;
			final double mag_range_max = sim_parameters.ranv2_max_mag;
			final int mag_range_size = (int)(Math.round ((mag_range_max - mag_range_min) / sim_parameters.ranv2_step_mag)) + 1;

			the_mag_values = (OEDiscreteRange.makeLinear (mag_range_size, mag_range_min, mag_range_max)).get_range_array();

			accum.setup (null, sim_parameters.ranv2_accum_option, the_tint_br, the_derate_br, the_gen_values, the_mag_values);
			range_accumulator = accum;
		}
		break;

		}

		// Create the list of accumulators

		ArrayList<OEEnsembleAccumulator> accumulators = new ArrayList<OEEnsembleAccumulator>();
		accumulators.add (range_accumulator);

		// Set up the ensemble parameters

		OEEnsembleParams ensemble_params = new OEEnsembleParams();

		int req_catalog_count = sim_parameters.eff_ranv2_num_catalogs();

		ensemble_params.set (
			sim_initializer,					// initializer
			accumulators,						// accumulators
			req_catalog_count					// num_catalogs
		);

		// Create the ensemble generator

		OEEnsembleGenerator ensemble_generator = new OEEnsembleGenerator();
		
		// Set our loop results to be an upstream

		ensemble_generator.set_upstream_loop_result (loop_result);

		// Generate the catalogs

		int catalog_count = ensemble_generator.generate_all_catalogs (ensemble_params, sim_executor, max_runtime, progress_time);

		// Accumulate loop results

		//loop_result.accum_from (ensemble_generator.get_loop_result());

		// Error checks

		if (catalog_count < 0) {
			String msg = "Ranging failed due to thread abort";
			System.out.println (msg);
			throw new OERangeThreadAbortException ("OESimulator.do_run_ranging_v2: " + msg + ": " + ensemble_generator.get_status_msg());
		}

		if (catalog_count < req_catalog_count) {
			String msg = "Ranging failed due to insufficient number of catalogs generated within time limit: obtained = " + catalog_count + ", required = " + req_catalog_count;
			System.out.println (msg);
			throw new OERangeTimeoutException ("OESimulator.do_run_ranging_v2: " + msg + ": " + ensemble_generator.get_status_msg());
		}

		// Get the probability of occurrence array

		final int xcount = 0;
		double[][] prob_occur_array = ((OEAccumReadoutSeedEst)range_accumulator).get_prob_occur_array (xcount);

		// Get the fractile array

		double fractile = sim_parameters.ranv2_direct_fractile;
		int[][] fractile_array = ((OEAccumReadoutSeedEst)range_accumulator).get_fractile_array (fractile);

		// Find the magnitude with requested probability of producing non-empty catalogs.

		int ix_mag_min = Math.max (0, find_last_exceeder (sim_parameters.ranv2_prob_nonempty, prob_occur_array[0]));
		System.out.println ("Minimum magnitude selected by probability of non-empty catalog exceeding " + sim_parameters.ranv2_prob_nonempty + " is " + the_mag_values[ix_mag_min]);

		// If also requesting minimum number of direct aftershocks, apply it

		if (sim_parameters.ranv2_direct_size > 0) {
			int ix_2 = Math.max (0, find_last_exceeder (sim_parameters.ranv2_direct_size, fractile_array[0]));
			System.out.println ("Minimum magnitude selected by number of direct aftershocks exceeding " + sim_parameters.ranv2_direct_size + " is " + the_mag_values[ix_2]);
			ix_mag_min = Math.min (ix_mag_min, ix_2);
		}

		// Find the magnitude with requested probability of exceeding

		int ix_mag_max = Math.max (0, find_last_exceeder (sim_parameters.ranv2_exceed_fraction, prob_occur_array[1]));
		System.out.println ("Maximum magnitude selected by exceedence probability of " + sim_parameters.ranv2_exceed_fraction + " in " + sim_parameters.ranv2_gen_br + " generations is " + the_mag_values[ix_mag_max]);

		// Now set the range

		sim_catalog_range.set_range_fixed (
			tbegin,															// tbegin
			ranging_time_values[ranging_time_values.length - 1],			// tend
			the_mag_values[ix_mag_min],										// mag_min_sim
			the_mag_values[ix_mag_max],										// mag_max_sim
			sim_parameters.ranv2_mag_excess									// mag_excess
		);

		// Restore the catalog limits

		sim_initializer.set_limits (old_limits);

		// Write the selected range into the initializer

		sim_initializer.set_range (sim_catalog_range);

		System.out.println ();
		System.out.println ("Selected range:");
		System.out.println (sim_catalog_range.progress_string());

		// Say goodbye

		System.out.println ();
		System.out.println ("Completed ETAS ranging");

		return;
	}




	// Run the ranging, version 3.
	// Throws exception in case of failure.

	private void do_run_ranging_v3 () throws OEException {

		// Get the forecast time from the initializer

		final double t_forecast = sim_initializer.get_t_forecast();

		// Get the time and magnitude bins, starting at the time of the forecast (we really only need the ending time)

		final double[] ranging_time_values = sim_forecast_grid.get_ranging_time_values (t_forecast);

		// Get the b-value and scaling magnitude from the initializer

		final double b_value = sim_initializer.get_b_value();
		final double scaling_mag = sim_initializer.get_mainshock_mag();

		// Get the simulation start time from the existing range

		final double tbegin = sim_catalog_range.tbegin;

		// Set up a per-catalog seed estimation range

		sim_catalog_range.set_range_seed_est (
			tbegin,													// tbegin
			ranging_time_values[ranging_time_values.length - 1],	// tend
			scaling_mag + sim_parameters.range_min_rel_mag,			// mag_min_sim
			scaling_mag + sim_parameters.range_max_rel_mag,			// mag_max_sim
			sim_parameters.ranv2_min_mag,							// mag_min_lo
			sim_parameters.ranv2_max_mag,							// mag_min_hi
			sim_parameters.ranv2_min_mag,							// mag_max_lo
			sim_parameters.ranv2_max_mag,							// mag_max_hi
			sim_parameters.ranv2_direct_size,						// gen_size_target
			sim_parameters.ranv2_mag_excess,						// mag_excess
			sim_parameters.ranv2_gen_br,							// madj_gen_br
			sim_parameters.ranv2_derate_br,							// madj_derate_br
			sim_parameters.ranv2_exceed_fraction,					// madj_exceed_fr
			sim_parameters.ranv3_direct_size_hi						// madj_target_hi
		);

		// Write the selected range into the initializer

		sim_initializer.set_range (sim_catalog_range);

		System.out.println ();
		System.out.println ("Selected range:");
		System.out.println (sim_catalog_range.progress_string());

		// Say goodbye

		System.out.println ();
		System.out.println ("Completed ETAS ranging");

		return;
	}




	// Run the simulation.
	// Parameters:
	//  the_sim_initializer = The catalog initializer to use for simulations.
	//  the_sim_parameters = Simulation parameters.
	//  the_sim_executor = Executor to use for spawning threads.
	// Returns true if simulation was successful, false if forecast could not be done.
	// Note that because of ranging, the initializer is used for multiple ensembles.
	// Upon a true return, the following are available:
	//  sim_accumulator = The accumulator that holds the results of the simulations.
	//  sim_forecast_grid = Forecast grid obtained from the accumulator.
	//  sim_catalog_range = The range used for the simulation.
	// This function catches all exceptions.  If an exception occurs, it is
	// displayed, and this function returns false.

	private boolean run_simulation (
		OEEnsembleInitializer the_sim_initializer,
		OESimulationParams the_sim_parameters,
		AutoExecutorService the_sim_executor
	) {

		// Initialize loop result

		loop_result.clear();

		// Display startup information

		long start_time = System.currentTimeMillis();
		System.out.println ("Beginning ETAS simulation at " + SimpleUtils.time_to_string (start_time));
		System.out.println (SimpleUtils.one_line_memory_status_string());
		System.out.println ();

		// Flag indicates if simulation was successful

		boolean f_success = false;

		try {

			// Setup

			do_setup_inputs (the_sim_initializer, the_sim_parameters, the_sim_executor);

			// Ranging

			do_run_ranging();

			// Simulation

			do_run_simulation();

			// Success

			f_success = true;

		} catch (OEException e) {
			System.out.println ("ETAS exception occurred during ETAS simulation");
			System.out.println (SimpleUtils.getStackTraceAsString(e));
			f_success = false;

		} catch (OESimulationException e) {
			System.out.println ("Simulation exception occurred during ETAS simulation");
			System.out.println (SimpleUtils.getStackTraceAsString(e));
			f_success = false;

		} catch (Exception e) {
			System.out.println ("Exception occurred during ETAS simulation");
			System.out.println (SimpleUtils.getStackTraceAsString(e));
			f_success = false;
		}

		// Display result information

		long end_time = System.currentTimeMillis();
		long elapsed_time = end_time - start_time;
		String s_elapsed_time = String.format ("%.3f", ((double)elapsed_time)/1000.0);

		System.out.println ();
		System.out.println ("Ending ETAS simulation at " + SimpleUtils.time_to_string (end_time) + ", elapsed time = " + s_elapsed_time + " seconds");

		if (f_success) {
			System.out.println ("ETAS simulation succeeded");
		} else {
			System.out.println ("ETAS simulation failed");
		}

		// Return success flag

		return f_success;
	}




	// Run the simulation, throwing exception if error.
	// Parameters:
	//  the_sim_initializer = The catalog initializer to use for simulations.
	//  the_sim_parameters = Simulation parameters.
	//  the_sim_exec_timer = Execution timer, containing the executor to use for spawning threads.
	// Returns true if simulation was successful, false if forecast could not be done.
	// Note that because of ranging, the initializer is used for multiple ensembles.
	// Upon a true return, the following are available:
	//  sim_accumulator = The accumulator that holds the results of the simulations.
	//  sim_forecast_grid = Forecast grid obtained from the accumulator.
	//  sim_catalog_range = The range used for the simulation.

	public void run_simulation_ex (
		OEEnsembleInitializer the_sim_initializer,
		OESimulationParams the_sim_parameters,
		SimpleExecTimer the_sim_exec_timer
	) throws OEException {

		// Initialize loop result

		loop_result.clear();

		// Display startup information

		System.out.println ();
		System.out.println ("Beginning ETAS ranging and simulation");
		System.out.println (SimpleUtils.one_line_memory_status_string());

		// Setup

		do_setup_inputs (the_sim_initializer, the_sim_parameters, the_sim_exec_timer);

		// Ranging

		do_run_ranging();

		// Simulation

		do_run_simulation();

		// Display result information

		System.out.println ();
		System.out.println ("Ending ETAS ranging and simulation");

		return;
	}




	// Get the loop result for the last operation.
	// The returned object is newly-allocated.

	public final SimpleThreadLoopResult get_loop_result () {
		return (new SimpleThreadLoopResult()).copy_from (loop_result);
	}




	// Set an upstream accumulator for the loop result.

	public final void set_upstream_loop_result (SimpleThreadLoopResult upstream_loop_result) {
		loop_result.set_upstream (upstream_loop_result);
		return;
	}




	//----- Utility functions -----




	// Generate a single catalog, using the given initializer and examiner.

	public static void gen_single_catalog (OEEnsembleInitializer initializer, OECatalogExaminer examiner) {

		// Tell the initializer to begin initializing catalogs

		initializer.begin_initialization();

		// Here begins code which could be per-thread

		// Get the random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Create a seeder for our initializer, which we re-use for each catalog

		OECatalogSeeder seeder = initializer.make_seeder();

		// Allocate a seeder communication area, which we re-use for each catalog

		OECatalogSeedComm seed_comm = new OECatalogSeedComm();

		// Allocate the storage (subclass of OECatalogBuilder and OECatalogView), which we re-use for each catalog

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Allocate a generator, which we re-use for each catalog

		OECatalogGenerator cat_generator = new OECatalogGenerator();

		// Here begins code which could be per-catalog

		// Set up the seeder communication area

		seed_comm.setup_seed_comm (cat_storage, rangen);

		// Open the seeder

		seeder.open();

		// Seed the catalog

		seeder.seed_catalog (seed_comm);

		// Close the seeder

		seeder.close();

		// Set up the catalog generator
				
		cat_generator.setup (rangen, cat_storage, false);

		// Calculate all generations and end the catalog

		cat_generator.calc_all_gen();

		// Tell the generator to forget the catalog

		cat_generator.forget();

		// Examine the catalog

		examiner.examine_cat (cat_storage, rangen);

		// Here ends code which could be per-catalog

		// Here ends code which could be per-thread

		// Tell the initializer to end initializing catalogs

		initializer.end_initialization();

		return;
	}




	// Generate a single catalog, containing just seeds with no aftershocks, using the given initializer and examiner.

	public static void gen_seed_only_catalog (OEEnsembleInitializer initializer, OECatalogExaminer examiner) {

		// Tell the initializer to begin initializing catalogs

		initializer.begin_initialization();

		// Here begins code which could be per-thread

		// Get the random number generator

		OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

		// Create a seeder for our initializer, which we re-use for each catalog

		OECatalogSeeder seeder = initializer.make_seeder();

		// Allocate a seeder communication area, which we re-use for each catalog

		OECatalogSeedComm seed_comm = new OECatalogSeedComm();

		// Allocate the storage (subclass of OECatalogBuilder and OECatalogView), which we re-use for each catalog

		OECatalogStorage cat_storage = new OECatalogStorage();

		// Allocate a generator, which we re-use for each catalog

		//OECatalogGenerator cat_generator = new OECatalogGenerator();

		// Here begins code which could be per-catalog

		// Set up the seeder communication area

		seed_comm.setup_seed_comm (cat_storage, rangen);

		// Open the seeder

		seeder.open();

		// Seed the catalog

		seeder.seed_catalog (seed_comm);

		// Close the seeder

		seeder.close();

		// Set up the catalog generator
				
		//cat_generator.setup (rangen, cat_storage, false);

		// Calculate all generations and end the catalog

		//cat_generator.calc_all_gen();

		// Tell the generator to forget the catalog

		//cat_generator.forget();

		// End the catalog

		seed_comm.cat_builder.end_catalog();

		// Examine the catalog

		examiner.examine_cat (cat_storage, rangen);

		// Here ends code which could be per-catalog

		// Here ends code which could be per-thread

		// Tell the initializer to end initializing catalogs

		initializer.end_initialization();

		return;
	}




	//----- Testing -----




	// Run simulation for testing purposes.
	// Parameters:
	//  the_sim_initializer = The catalog initializer to use for simulations.
	//  f_prod = True to select production parameter.
	//  num_cats = Number of catalogs to generate, or 0 for default.
	//  target_size = Target size for simulations, or 0 for default.
	//  max_runtime = Maximum running time in milliseconds, or -1L for no limit.
	//  progress_time = Progress message time, or -1L for no progress messages
	// Returns the OESimulator.
	// Throws exception if error.

	public static OESimulator test_run_simulation_ex (OEEnsembleInitializer the_sim_initializer, boolean f_prod, int num_cats, int target_size, long max_runtime, long progress_time) throws OEException {
		OESimulator simulator = null;

		// Create the simulation parameters

		OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

		if (num_cats > 0) {
			test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
			test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
		}

		if (target_size > 0) {
			test_sim_parameters.range_target_size = Math.max (100, target_size);
		}

		// Display the simulation parameters

		System.out.println ();
		System.out.println (test_sim_parameters.toString());

		// Create the executor

		try (

			// Create the executor

			AutoExecutorService auto_executor = new AutoExecutorService();
		){
			SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);
			exec_timer.start_timer();

			// Run the simulation

			simulator = new OESimulator();

			simulator.run_simulation_ex (
				the_sim_initializer,
				test_sim_parameters,
				exec_timer
			);

			// Display result

			exec_timer.stop_timer();

			long elapsed_time = exec_timer.get_total_runtime();
			String s_elapsed_time = String.format ("%.1f", ((double)elapsed_time)/1000.0);

			System.out.println ();
			System.out.println ("Elapsed time = " + s_elapsed_time + " seconds");
		}

		return simulator;
	}




	// Run simulation for testing purposes.
	// Parameters:
	//  the_sim_initializer = The catalog initializer to use for simulations.
	//  test_sim_parameters = Simulation parameters.
	//  max_runtime = Maximum running time in milliseconds, or -1L for no limit.
	//  progress_time = Progress message time, or -1L for no progress messages.
	// Returns the OESimulator.
	// Throws exception if error.

	public static OESimulator test_run_simulation_ex (OEEnsembleInitializer the_sim_initializer, OESimulationParams test_sim_parameters, long max_runtime, long progress_time) throws OEException {
		OESimulator simulator = null;

		// Display the simulation parameters

		System.out.println ();
		System.out.println (test_sim_parameters.toString());

		// Create the executor

		try (

			// Create the executor

			AutoExecutorService auto_executor = new AutoExecutorService();
		){
			SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);
			exec_timer.start_timer();

			// Run the simulation

			simulator = new OESimulator();

			simulator.run_simulation_ex (
				the_sim_initializer,
				test_sim_parameters,
				exec_timer
			);

			// Display result

			exec_timer.stop_timer();

			long elapsed_time = exec_timer.get_total_runtime();
			String s_elapsed_time = String.format ("%.1f", ((double)elapsed_time)/1000.0);

			System.out.println ();
			System.out.println ("Elapsed time = " + s_elapsed_time + " seconds");
		}

		return simulator;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OESimulator : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 10 additional arguments

			if (args.length != 11) {
				System.err.println ("OESimulator : Invalid 'test1' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);

				// Say hello

				System.out.println ("Running simulation with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Create the simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				// Create the executor

				try (

					// Create the executor

					AutoExecutorService auto_executor = new AutoExecutorService();
				){

					// Run the simulation

					OESimulator simulator = new OESimulator();

					boolean sim_result = simulator.run_simulation (
						test_sim_initializer,
						test_sim_parameters,
						auto_executor
					);

					// Display result

					System.out.println ();
					System.out.println ("sim_result = " + sim_result);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size  accum_opt
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats, target_size, and accum_opt can be zero to use default values.
		// Same as test #1 except allows setting sim_accum_option.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 11 additional arguments

			if (args.length != 12) {
				System.err.println ("OESimulator : Invalid 'test2' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);
				int accum_opt = Integer.parseInt (args[11]);

				// Say hello

				System.out.println ("Running simulation with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);
				System.out.println ("accum_opt = " + accum_opt);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Create the simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				if (accum_opt > 0) {
					test_sim_parameters.sim_accum_option = accum_opt;
				}

				// Create the executor

				try (

					// Create the executor

					AutoExecutorService auto_executor = new AutoExecutorService();
				){

					// Run the simulation

					OESimulator simulator = new OESimulator();

					boolean sim_result = simulator.run_simulation (
						test_sim_initializer,
						test_sim_parameters,
						auto_executor
					);

					// Display result

					System.out.println ();
					System.out.println ("sim_result = " + sim_result);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size  accum_opt
		//         max_rel_mag  exceed_fraction  accum_param_1
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats, target_size, accum_opt, and exceed_fraction can be zero to use default values.
		// max_rel_mag and accum_param_1 have a default value of zero.
		// Same as test #2 except allows setting range_max_rel_mag, range_exceed_fraction, and sim_accum_param_1.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 14 additional arguments

			if (args.length != 15) {
				System.err.println ("OESimulator : Invalid 'test3' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);
				int accum_opt = Integer.parseInt (args[11]);
				double max_rel_mag = Double.parseDouble (args[12]);
				double exceed_fraction = Double.parseDouble (args[13]);
				double accum_param_1 = Double.parseDouble (args[14]);

				// Say hello

				System.out.println ("Running simulation with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);
				System.out.println ("accum_opt = " + accum_opt);
				System.out.println ("max_rel_mag = " + max_rel_mag);
				System.out.println ("exceed_fraction = " + exceed_fraction);
				System.out.println ("accum_param_1 = " + accum_param_1);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Create the simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				if (accum_opt > 0) {
					test_sim_parameters.sim_accum_option = accum_opt;
				}

				test_sim_parameters.range_max_rel_mag = max_rel_mag;

				if (exceed_fraction > 0) {
					test_sim_parameters.range_exceed_fraction = exceed_fraction;
				}

				test_sim_parameters.sim_accum_param_1 = accum_param_1;

				// Create the executor

				try (

					// Create the executor

					AutoExecutorService auto_executor = new AutoExecutorService();
				){

					// Run the simulation

					OESimulator simulator = new OESimulator();

					boolean sim_result = simulator.run_simulation (
						test_sim_initializer,
						test_sim_parameters,
						auto_executor
					);

					// Display result

					System.out.println ();
					System.out.println ("sim_result = " + sim_result);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size  accum_opt
		//         max_rel_mag  exceed_fraction  accum_param_1  n_main
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats, target_size, accum_opt, and exceed_fraction can be zero to use default values.
		// max_rel_mag and accum_param_1 have a default value of zero.
		// Same as test #3 except allows setting mainshock branch ration n_main.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 15 additional arguments

			if (args.length != 16) {
				System.err.println ("OESimulator : Invalid 'test4' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);
				int accum_opt = Integer.parseInt (args[11]);
				double max_rel_mag = Double.parseDouble (args[12]);
				double exceed_fraction = Double.parseDouble (args[13]);
				double accum_param_1 = Double.parseDouble (args[14]);
				double n_main = Double.parseDouble (args[15]);

				// Say hello

				System.out.println ("Running simulation with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);
				System.out.println ("accum_opt = " + accum_opt);
				System.out.println ("max_rel_mag = " + max_rel_mag);
				System.out.println ("exceed_fraction = " + exceed_fraction);
				System.out.println ("accum_param_1 = " + accum_param_1);
				System.out.println ("n_main = " + n_main);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main, n_main);

				// Create the simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				if (accum_opt > 0) {
					test_sim_parameters.sim_accum_option = accum_opt;
				}

				test_sim_parameters.range_max_rel_mag = max_rel_mag;

				if (exceed_fraction > 0) {
					test_sim_parameters.range_exceed_fraction = exceed_fraction;
				}

				test_sim_parameters.sim_accum_param_1 = accum_param_1;

				// Create the executor

				try (

					// Create the executor

					AutoExecutorService auto_executor = new AutoExecutorService();
				){

					// Run the simulation

					OESimulator simulator = new OESimulator();

					boolean sim_result = simulator.run_simulation (
						test_sim_initializer,
						test_sim_parameters,
						auto_executor
					);

					// Display result

					System.out.println ();
					System.out.println ("sim_result = " + sim_result);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size  max_runtime  progress_time
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.
		// max_runtime can be -1L for no limit on runtime.
		// progress_time can be -1L for no progress messages.
		// Same as test #1 except uses the exec timer.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 12 additional arguments

			if (args.length != 13) {
				System.err.println ("OESimulator : Invalid 'test5' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);
				long max_runtime = Long.parseLong (args[11]);
				long progress_time = Long.parseLong (args[12]);

				// Say hello

				System.out.println ("Running simulation with given parameters, using execution timer");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Create the simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				if (target_size > 0) {
					test_sim_parameters.range_target_size = Math.max (100, target_size);
				}

				// Create the executor

				try (

					// Create the executor

					AutoExecutorService auto_executor = new AutoExecutorService();
				){
					SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);
					exec_timer.start_timer();

					// Run the simulation

					OESimulator simulator = new OESimulator();

					simulator.run_simulation_ex (
						test_sim_initializer,
						test_sim_parameters,
						exec_timer
					);

					// Display result

					exec_timer.stop_timer();

					long elapsed_time = exec_timer.get_total_runtime();
					String s_elapsed_time = String.format ("%.1f", ((double)elapsed_time)/1000.0);

					System.out.println ();
					System.out.println ("Elapsed time = " + s_elapsed_time + " seconds");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size  max_runtime  progress_time
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.
		// max_runtime can be -1L for no limit on runtime.
		// progress_time can be -1L for no progress messages.
		// Same as test #5 except uses test_run_simulation_ex.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 12 additional arguments

			if (args.length != 13) {
				System.err.println ("OESimulator : Invalid 'test6' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);
				long max_runtime = Long.parseLong (args[11]);
				long progress_time = Long.parseLong (args[12]);

				// Say hello

				System.out.println ("Running simulation with given parameters, using execution timer, using test function");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Run the simulations

				OESimulator.test_run_simulation_ex (test_sim_initializer, f_prod, num_cats, target_size, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  target_size  max_runtime  progress_time
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// Then output the forecast JSON.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.
		// max_runtime can be -1L for no limit on runtime.
		// progress_time can be -1L for no progress messages.
		// Same as test #6 except generates the forecast JSON.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 12 additional arguments

			if (args.length != 13) {
				System.err.println ("OESimulator : Invalid 'test7' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				int target_size = Integer.parseInt (args[10]);
				long max_runtime = Long.parseLong (args[11]);
				long progress_time = Long.parseLong (args[12]);

				// Say hello

				System.out.println ("Running simulation with given parameters, and producing forecast JSON, using execution timer, using test function");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("target_size = " + target_size);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Run the simulations

				OESimulator simulator = OESimulator.test_run_simulation_ex (test_sim_initializer, f_prod, num_cats, target_size, max_runtime, progress_time);

				// Produce the forecast JSON

				USGS_ForecastModel model = simulator.sim_forecast_grid;
				List<ObsEqkRupture> aftershocks = new ArrayList<ObsEqkRupture>();
				Instant eventDate = Instant.parse ("2020-01-01T12:00:00Z");
				Instant startDate = Instant.parse ("2020-01-31T12:00:00Z");

				USGS_AftershockForecast forecast = new USGS_AftershockForecast (model, aftershocks, eventDate, startDate);
				String forecast_json = forecast.buildJSONString();

				System.out.println ();
				System.out.println (forecast_json);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats  prob_nonempty  direct_size  exceed_fraction  max_runtime  progress_time
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.
		// max_runtime can be -1L for no limit on runtime.
		// progress_time can be -1L for no progress messages.
		// Same as test #6 except uses ranging version 2.

		if (args[0].equalsIgnoreCase ("test8")) {

			// 14 additional arguments

			if (args.length != 15) {
				System.err.println ("OESimulator : Invalid 'test8' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				double prob_nonempty = Double.parseDouble (args[10]);
				int direct_size = Integer.parseInt (args[11]);
				double exceed_fraction = Double.parseDouble (args[12]);
				long max_runtime = Long.parseLong (args[13]);
				long progress_time = Long.parseLong (args[14]);

				// Say hello

				System.out.println ("Running simulation with given parameters, using execution timer, using ranging version 2");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("prob_nonempty = " + prob_nonempty);
				System.out.println ("direct_size = " + direct_size);
				System.out.println ("exceed_fraction = " + exceed_fraction);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Set up simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				test_sim_parameters.ranv2_prob_nonempty = prob_nonempty;
				test_sim_parameters.ranv2_direct_size = direct_size;
				test_sim_parameters.ranv2_exceed_fraction = exceed_fraction;

				test_sim_parameters.range_method = OEConstants.RANGING_METH_SEED_EST;

				// Run the simulations

				OESimulator.test_run_simulation_ex (test_sim_initializer, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats
		//         prob_nonempty  direct_size  exceed_fraction  accum_option  accum_param_1  mag_excess  max_runtime  progress_time
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.
		// max_runtime can be -1L for no limit on runtime.
		// progress_time can be -1L for no progress messages.
		// accum_option can be -1 to use default.
		// accum_param_1 is the upfill secondary reduction multiplier.
		// Uses ranging version 2.
		// Same as test #8 with extra parameters.

		if (args[0].equalsIgnoreCase ("test9")) {

			// 17 additional arguments

			if (args.length != 18) {
				System.err.println ("OESimulator : Invalid 'test9' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);
				double prob_nonempty = Double.parseDouble (args[10]);
				int direct_size = Integer.parseInt (args[11]);
				double exceed_fraction = Double.parseDouble (args[12]);

				int accum_option = Integer.parseInt (args[13]);
				double accum_param_1 = Double.parseDouble (args[14]);
				double mag_excess = Double.parseDouble (args[15]);

				long max_runtime = Long.parseLong (args[16]);
				long progress_time = Long.parseLong (args[17]);

				// Say hello

				System.out.println ("Running simulation with given parameters, using execution timer, using ranging version 2");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("prob_nonempty = " + prob_nonempty);
				System.out.println ("direct_size = " + direct_size);
				System.out.println ("exceed_fraction = " + exceed_fraction);
				System.out.println ("accum_option = " + accum_option);
				System.out.println ("accum_param_1 = " + accum_param_1);
				System.out.println ("exceed_fraction = " + exceed_fraction);
				System.out.println ("mag_excess = " + mag_excess);
				System.out.println ("progress_time = " + progress_time);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Set up simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				test_sim_parameters.ranv2_prob_nonempty = prob_nonempty;
				test_sim_parameters.ranv2_direct_size = direct_size;
				test_sim_parameters.ranv2_exceed_fraction = exceed_fraction;

				test_sim_parameters.range_method = OEConstants.RANGING_METH_SEED_EST;

				if (accum_option >= 0) {
					test_sim_parameters.sim_accum_option = accum_option;
				}
				test_sim_parameters.sim_accum_param_1 = accum_param_1;
				test_sim_parameters.ranv2_mag_excess = mag_excess;

				// Run the simulations

				OESimulator.test_run_simulation_ex (test_sim_initializer, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  n  p  c  b  alpha  mag_main  tbegin  f_prod  num_cats
		//          direct_size  mag_excess  gen_br  derate_br  exceed_fraction  direct_size_hi  accum_option  accum_param_1  max_runtime  progress_time
		// Build a catalog with the given parameter, using multiple threads.
		// The "n" is the branch ratio; "a" is computed from it.
		// Then run the simulation.
		// f_prod chooses whether to use production or development simulation parameters.
		// num_cats and target_size can be zero to use default values.
		// max_runtime can be -1L for no limit on runtime.
		// progress_time can be -1L for no progress messages.
		// accum_option can be -1 to use default.
		// accum_param_1 is the upfill secondary reduction multiplier.
		// Uses ranging version 2.
		// Same as test #8 with extra parameters.

		if (args[0].equalsIgnoreCase ("test10")) {

			// 19 additional arguments

			if (args.length != 20) {
				System.err.println ("OESimulator : Invalid 'test10' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mag_main = Double.parseDouble (args[6]);
				double tbegin = Double.parseDouble (args[7]);
				boolean f_prod = Boolean.parseBoolean (args[8]);
				int num_cats = Integer.parseInt (args[9]);

				int direct_size = Integer.parseInt (args[10]);
				double mag_excess = Double.parseDouble (args[11]);
				int gen_br = Integer.parseInt (args[12]);
				double derate_br = Double.parseDouble (args[13]);
				double exceed_fraction = Double.parseDouble (args[14]);
				int direct_size_hi = Integer.parseInt (args[15]);

				int accum_option = Integer.parseInt (args[16]);
				double accum_param_1 = Double.parseDouble (args[17]);

				long max_runtime = Long.parseLong (args[18]);
				long progress_time = Long.parseLong (args[19]);

				// Say hello

				System.out.println ("Running simulation with given parameters, using execution timer, using ranging version 3");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("tbegin = " + tbegin);
				System.out.println ("f_prod = " + f_prod);
				System.out.println ("num_cats = " + num_cats);
				System.out.println ("direct_size = " + direct_size);
				System.out.println ("mag_excess = " + mag_excess);
				System.out.println ("gen_br = " + gen_br);
				System.out.println ("derate_br = " + derate_br);
				System.out.println ("exceed_fraction = " + exceed_fraction);
				System.out.println ("direct_size_hi = " + direct_size_hi);
				System.out.println ("accum_option = " + accum_option);
				System.out.println ("accum_param_1 = " + accum_param_1);
				System.out.println ("max_runtime = " + max_runtime);
				System.out.println ("progress_time = " + progress_time);

				// Set up catalog parameters

				double mref = 3.0;
				double msup = 9.5;
				double tend = OEForecastGrid.get_config_tend (tbegin);

				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_fixed_mag_limited_br (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tbegin,
					tend
				);

				// Branch ratio checks

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				System.out.println ("a = " + test_cat_params.a);

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Create the initializer

				double t_main = 0.0;

				OEInitFixedState test_sim_initializer = new OEInitFixedState();
				test_sim_initializer.setup_single (test_cat_params, mag_main, t_main);

				// Set up simulation parameters

				OESimulationParams test_sim_parameters = (new OESimulationParams()).set_to_typical (f_prod);

				if (num_cats > 0) {
					test_sim_parameters.sim_num_catalogs = Math.max (100, num_cats);
					test_sim_parameters.range_num_catalogs = Math.max (100, num_cats/10);
				}

				test_sim_parameters.ranv2_direct_size = direct_size;
				test_sim_parameters.ranv2_mag_excess = mag_excess;
				test_sim_parameters.ranv2_gen_br = gen_br;
				test_sim_parameters.ranv2_derate_br = derate_br;
				test_sim_parameters.ranv2_exceed_fraction = exceed_fraction;
				test_sim_parameters.ranv3_direct_size_hi = direct_size_hi;

				test_sim_parameters.range_method = OEConstants.RANGING_METH_VAR_SEED_EST;

				if (accum_option >= 0) {
					test_sim_parameters.sim_accum_option = accum_option;
				}
				test_sim_parameters.sim_accum_param_1 = accum_param_1;

				// Run the simulations

				OESimulator.test_run_simulation_ex (test_sim_initializer, test_sim_parameters, max_runtime, progress_time);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OESimulator : Unrecognized subcommand : " + args[0]);
		return;

	}

}
