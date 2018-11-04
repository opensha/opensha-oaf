package org.opensha.oaf.aftershockStatistics.gamma;

import java.util.List;
import java.util.Arrays;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.aftershockStatistics.comcat.ComcatAccessor;
import org.opensha.oaf.aftershockStatistics.AftershockStatsCalc;
import org.opensha.oaf.aftershockStatistics.RJ_AftershockModel;
import org.opensha.oaf.aftershockStatistics.RJ_AftershockModel_Generic;
import org.opensha.oaf.aftershockStatistics.GenericRJ_Parameters;
import org.opensha.oaf.aftershockStatistics.GenericRJ_ParametersFetch;
import org.opensha.oaf.aftershockStatistics.MagCompPage_Parameters;
import org.opensha.oaf.aftershockStatistics.MagCompPage_ParametersFetch;
import org.opensha.oaf.aftershockStatistics.OAFTectonicRegime;
import org.opensha.oaf.aftershockStatistics.SeqSpecRJ_Parameters;
import org.opensha.oaf.aftershockStatistics.RJ_Summary_Generic;

import org.opensha.oaf.aftershockStatistics.aafs.ForecastMainshock;
import org.opensha.oaf.aftershockStatistics.aafs.ForecastParameters;
import org.opensha.oaf.aftershockStatistics.aafs.ForecastResults;

import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;
import org.opensha.oaf.aftershockStatistics.util.MarshalReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalWriter;
import org.opensha.oaf.aftershockStatistics.util.MarshalException;


/**
 * A set of log-likelihood setss for an earthquake.
 * Author: Michael Barall 10/10/2018.
 *
 * This object holds a set of log-likelihood sets for a single earthquake.
 * There is one log-likelihood set for each forecast lag and aftershock model.
 */
public class EqkForecastSet {

	//----- Data -----

	// Number of simulations.

	private int num_sim;

	// Information about the mainshock.

	private ForecastMainshock fcmain;

	// Array of log-likelihood sets for the earthquake.
	// Dimension of the array is
	//  obs_log_like[forecast_lag_count][model_kind_count].
	
	private LogLikeSet[][] log_like_sets;




	//----- Construction -----

	// Constructor creates an empty object.

	public EqkForecastSet () {
		fcmain = null;
		num_sim = -1;
		log_like_sets = null;
	}




	// Run simulations.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_num_sim = The number of simulations to run.
	//  the_fcmain = Mainshock information.
	//  verbose = True to write output for each simulation.

	public void run_simulations (GammaConfig gamma_config, int the_num_sim,
		ForecastMainshock the_fcmain, boolean verbose) {

		// Save number of simulations and mainshock information

		num_sim = the_num_sim;
		fcmain = the_fcmain;

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Allocate all the arrays

		log_like_sets = new LogLikeSet[num_fc_lag][num_model];

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model] = new LogLikeSet();
			}
		}

		// Get catalog of all aftershocks

		List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

		// Loop over forecast lags ...

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {

			// Get the forecast lag

			long forecast_lag = gamma_config.forecast_lags[i_fc_lag];

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (forecast_lag, fcmain, null);

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all (fcmain.mainshock_time + forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "", fcmain, params, true);

			//if (!( results.generic_result_avail
			//	&& results.seq_spec_result_avail
			//	&& results.bayesian_result_avail )) {
			//	throw new RuntimeException ("EqkForecastSet: Failed to compute aftershock models");
			//}

			if (!( results.generic_result_avail )) {
				throw new RuntimeException ("EqkForecastSet: Failed to compute aftershock models");
			}

			// Generic model

			log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_GENERIC].run_simulations (
				gamma_config, forecast_lag, num_sim,
				fcmain, results.generic_model, all_aftershocks, verbose);

			// Sequence specific model

			if (results.seq_spec_result_avail
				&& results.seq_spec_model.get_num_aftershocks() >= gamma_config.seq_spec_min_aftershocks) {

				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_SEQ_SPEC].run_simulations (
					gamma_config, forecast_lag, num_sim,
					fcmain, results.seq_spec_model, all_aftershocks, verbose);
			}
			else {
				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_SEQ_SPEC].zero_init (
					gamma_config, forecast_lag, num_sim);
			}

			// Bayesian model

			if (results.bayesian_result_avail
				&& results.seq_spec_result_avail
				&& results.seq_spec_model.get_num_aftershocks() >= gamma_config.bayesian_min_aftershocks) {

				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_BAYESIAN].run_simulations (
					gamma_config, forecast_lag, num_sim,
					fcmain, results.bayesian_model, all_aftershocks, verbose);
			}
			else {
				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_BAYESIAN].zero_init (
					gamma_config, forecast_lag, num_sim);
			}
		}

		return;
	}




	// Run simulations for simulated data.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_num_sim = The number of simulations to run.
	//  the_fcmain = Mainshock information.
	//	cat_min_mag = Minimum magnitude for simulated aftershock catalog.
	//  f_epi = True to use epistemic uncertaintly when choosing simulation parameters.
	//  verbose = True to write output for each simulation.
	//  show_models = True to write summary for each R&J model.

	public void run_sim_simulations (GammaConfig gamma_config, int the_num_sim,
		ForecastMainshock the_fcmain, double cat_min_mag, boolean f_epi, boolean verbose, boolean show_models) {

		// Save number of simulations and mainshock information

		num_sim = the_num_sim;
		fcmain = the_fcmain;

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Allocate all the arrays

		log_like_sets = new LogLikeSet[num_fc_lag][num_model];

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model] = new LogLikeSet();
			}
		}

		// Get the generic model for this mainshock, to use for simulation parameters

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();
		OAFTectonicRegime regime = fetch.getRegion (fcmain.get_eqk_location());
		GenericRJ_Parameters sim_generic_params = fetch.get (regime);
		RJ_AftershockModel_Generic sim_generic_model = new RJ_AftershockModel_Generic (fcmain.mainshock_mag, sim_generic_params);

		if (show_models) {
			RJ_Summary_Generic sim_generic_summary = new RJ_Summary_Generic (sim_generic_model);
			System.out.println ("Mainshock generic model:");
			System.out.println (sim_generic_summary.toString());
		}

		// Loop to reject simulations with magnitude larger than mainshock

		List<ObsEqkRupture> all_aftershocks;

		for (;;) {

			// Choose simulation parameters, either using epistemic prob dist, or max likelihood values

			double[] apcval = new double[3];
			if (f_epi) {
				sim_generic_model.sample_apc (gamma_config.rangen.sample(), apcval);
			} else {
				apcval[0] = sim_generic_model.getMaxLikelihood_a();
				apcval[1] = sim_generic_model.getMaxLikelihood_p();
				apcval[2] = sim_generic_model.getMaxLikelihood_c();
			}
		
			double a = apcval[0];
			double b = sim_generic_model.get_b();
			double magMain = fcmain.mainshock_mag;
			double magCat = cat_min_mag;
			double capG = 10.0;
			double capH = 0.0;
			double p = apcval[1];
			double c = apcval[2];
			//double tMinDays = ((double)(gamma_config.sim_start_off)) / ComcatAccessor.day_millis;
			double tMinDays = 0.0;
			double tMaxDays = ((double)(gamma_config.max_forecast_lag + gamma_config.max_adv_window_end_off)) / ComcatAccessor.day_millis;
			long originTime = fcmain.mainshock_time;

			// Run the simulation

			all_aftershocks = AftershockStatsCalc.simAftershockSequence (
						a, b, magMain, magCat, capG, capH, p, c, tMinDays, tMaxDays, originTime, gamma_config.rangen);

			// Find the maximum magnitude

			double max_mag = -1000.0;
			for (ObsEqkRupture rup : all_aftershocks) {
				double mag = rup.getMag();
				if (max_mag < mag) {
					max_mag = mag;
				}
			}

			// If maximum magnitude does not exceed mainshock, stop

			if (!( gamma_config.discard_sim_with_large_as )) {
				break;
			}

			if (max_mag <= magMain) {
				break;
			}
		}

		// Loop over forecast lags ...

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {

			// Get the forecast lag

			long forecast_lag = gamma_config.forecast_lags[i_fc_lag];

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (forecast_lag, fcmain, null);

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all_from_known_as (fcmain.mainshock_time + forecast_lag,
				ForecastResults.ADVISORY_LAG_WEEK, "", fcmain, params, true, all_aftershocks);

			//if (!( results.generic_result_avail
			//	&& results.seq_spec_result_avail
			//	&& results.bayesian_result_avail )) {
			//	throw new RuntimeException ("EqkForecastSet: Failed to compute aftershock models");
			//}

			if (!( results.generic_result_avail )) {
				throw new RuntimeException ("EqkForecastSet: Failed to compute aftershock models");
			}

			// Generic model

			if (show_models) {
				System.out.println ("Generic model:");
				System.out.println (results.generic_summary.toString());
			}

			log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_GENERIC].run_simulations (
				gamma_config, forecast_lag, num_sim,
				fcmain, results.generic_model, all_aftershocks, verbose);

			// Sequence specific model

			if (results.seq_spec_result_avail
				&& results.seq_spec_model.get_num_aftershocks() >= gamma_config.seq_spec_min_aftershocks) {

				if (show_models) {
					System.out.println ("Sequence specific model:");
					System.out.println (results.seq_spec_summary.toString());
				}

				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_SEQ_SPEC].run_simulations (
					gamma_config, forecast_lag, num_sim,
					fcmain, results.seq_spec_model, all_aftershocks, verbose);
			}
			else {
				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_SEQ_SPEC].zero_init (
					gamma_config, forecast_lag, num_sim);
			}

			// Bayesian model

			if (results.bayesian_result_avail
				&& results.seq_spec_result_avail
				&& results.seq_spec_model.get_num_aftershocks() >= gamma_config.bayesian_min_aftershocks) {

				if (show_models) {
					System.out.println ("Bayesian model:");
					System.out.println (results.bayesian_summary.toString());
				}

				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_BAYESIAN].run_simulations (
					gamma_config, forecast_lag, num_sim,
					fcmain, results.bayesian_model, all_aftershocks, verbose);
			}
			else {
				log_like_sets[i_fc_lag][GammaConfig.MODEL_KIND_BAYESIAN].zero_init (
					gamma_config, forecast_lag, num_sim);
			}
		}

		return;
	}




	// Allocate and zero-initialize all arrays.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_num_sim = The number of simulations to run.

	public void zero_init (GammaConfig gamma_config, int the_num_sim) {

		// Save number of simulations and mainshock information

		num_sim = the_num_sim;
		fcmain = null;

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Allocate all the arrays and zero-initialize

		log_like_sets = new LogLikeSet[num_fc_lag][num_model];

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			long forecast_lag = gamma_config.forecast_lags[i_fc_lag];
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model] = new LogLikeSet();
				log_like_sets[i_fc_lag][i_model].zero_init (gamma_config, forecast_lag, num_sim);
			}
		}

		return;
	}




	// Add array contents from another object into this object.
	// Parameters:
	//  gamma_config = Configuration information.
	//  other = The other object.
	//  randomize = True to select simulations from the other object randomly.

	public void add_from (GammaConfig gamma_config, EqkForecastSet other, boolean randomize) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Add each array element

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model].add_from (
					gamma_config, other.log_like_sets[i_fc_lag][i_model], randomize);
			}
		}

		return;
	}




	//----- Querying -----

	// Compute the single-event gamma.
	// Parameters:
	//  gamma_config = Configuration information.
	//  gamma_lo = Array to receive low value of gamma, dimension:
	//    gamma_lo[forecast_lag_count + 1][model_kind_count][adv_window_count + 1][adv_min_mag_bin_count].
	//  gamma_hi = Array to receive high value of gamma, dimension:
	//    gamma_hi[forecast_lag_count + 1][model_kind_count][adv_window_count + 1][adv_min_mag_bin_count].
	// The extra forecast lag slot is used to report the sum over all forecast lags.
	// The extra advisory window slot is used to report the sum over all windows.

	public void single_event_gamma (GammaConfig gamma_config, double[][][][] gamma_lo, double[][][][] gamma_hi) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Loop over forecast lags and aftershock models, computing gamma for each

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model].single_event_gamma (gamma_config,
					gamma_lo[i_fc_lag][i_model], gamma_hi[i_fc_lag][i_model]);
			}
		}

		// Loop over models, computing gamma for sum over forecast lags

		for (int i_model = 0; i_model < num_model; ++i_model) {

			// Accumulate sum

			LogLikeSet sum = new LogLikeSet();
			sum.zero_init (gamma_config, -1L, num_sim);

			for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
				sum.add_from (gamma_config, log_like_sets[i_fc_lag][i_model], false);
			}

			// Compute gamma

			sum.single_event_gamma (gamma_config,
				gamma_lo[num_fc_lag][i_model], gamma_hi[num_fc_lag][i_model]);
		}

		return;
	}




	// Compute the single-event zeta.
	// Parameters:
	//  gamma_config = Configuration information.
	//  zeta_lo = Array to receive low value of zeta, dimension:
	//    zeta_lo[forecast_lag_count + 1][model_kind_count][adv_window_count + 1][adv_min_mag_bin_count].
	//  zeta_hi = Array to receive high value of zeta, dimension:
	//    zeta_hi[forecast_lag_count + 1][model_kind_count][adv_window_count + 1][adv_min_mag_bin_count].
	// The extra forecast lag slot is used to report the sum over all forecast lags.
	// The extra advisory window slot is used to report the sum over all windows.

	public void single_event_zeta (GammaConfig gamma_config, double[][][][] zeta_lo, double[][][][] zeta_hi) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Loop over forecast lags and aftershock models, computing zeta for each

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model].single_event_zeta (gamma_config,
					zeta_lo[i_fc_lag][i_model], zeta_hi[i_fc_lag][i_model]);
			}
		}

		// Loop over models, computing zeta for sum over forecast lags

		for (int i_model = 0; i_model < num_model; ++i_model) {

			// Accumulate sum

			LogLikeSet sum = new LogLikeSet();
			sum.zero_init (gamma_config, -1L, num_sim);

			for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
				sum.add_from (gamma_config, log_like_sets[i_fc_lag][i_model], false);
			}

			// Compute zeta

			sum.single_event_zeta (gamma_config,
				zeta_lo[num_fc_lag][i_model], zeta_hi[num_fc_lag][i_model]);
		}

		return;
	}




	// Compute event count statistics.
	// Parameters:
	//  gamma_config = Configuration information.
	//  obs_count = Array to receive observed count, dimension:
	//    obs_count[forecast_lag_count + 1][model_kind_count][adv_window_count][adv_min_mag_bin_count].
	//  sim_median_count = Array to receive simulated median count, dimension:
	//    sim_median_count[forecast_lag_count + 1][model_kind_count][adv_window_count][adv_min_mag_bin_count].
	//  sim_fractile_5_count = Array to receive simulated 5 percent fractile count, dimension:
	//    sim_fractile_5_count[forecast_lag_count + 1][model_kind_count][adv_window_count][adv_min_mag_bin_count].
	//  sim_fractile_95_count = Array to receive simulated 95 percent fractile count, dimension:
	//    sim_fractile_95_count[forecast_lag_count + 1][model_kind_count][adv_window_count][adv_min_mag_bin_count].
	// The extra forecast lag slot is used to report the sum over all forecast lags.

	public void compute_count_stats (GammaConfig gamma_config, int[][][][] obs_count,
		int[][][][] sim_median_count, int[][][][] sim_fractile_5_count, int[][][][] sim_fractile_95_count) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Loop over forecast lags and aftershock models, computing statistics for each

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model].compute_count_stats (gamma_config,
					obs_count[i_fc_lag][i_model], sim_median_count[i_fc_lag][i_model],
					sim_fractile_5_count[i_fc_lag][i_model], sim_fractile_95_count[i_fc_lag][i_model]);
			}
		}

		// Loop over models, computing statistics for sum over forecast lags

		for (int i_model = 0; i_model < num_model; ++i_model) {

			// Accumulate sum

			LogLikeSet sum = new LogLikeSet();
			sum.zero_init (gamma_config, -1L, num_sim);

			for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
				sum.add_from (gamma_config, log_like_sets[i_fc_lag][i_model], false);
			}

			// Compute statistics

			sum.compute_count_stats (gamma_config,
				obs_count[num_fc_lag][i_model], sim_median_count[num_fc_lag][i_model],
				sim_fractile_5_count[num_fc_lag][i_model], sim_fractile_95_count[num_fc_lag][i_model]);
		}

		return;
	}




	// Compute event count statistics.
	// Parameters:
	//  gamma_config = Configuration information.
	//  obs_count = Array to receive observed count, dimension:
	//    obs_count[forecast_lag_count + 1][model_kind_count][adv_window_count + 1][adv_min_mag_bin_count].
	// The extra advisory window slot is used to report the sum over all windows.
	// The extra forecast lag slot is used to report the sum over all forecast lags.
	// Note the dimension of obs_count is different than in the overloaded function above.

	public void compute_count_stats (GammaConfig gamma_config, int[][][][] obs_count) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Loop over forecast lags and aftershock models, computing statistics for each

		for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				log_like_sets[i_fc_lag][i_model].compute_count_stats (gamma_config,
					obs_count[i_fc_lag][i_model]);
			}
		}

		// Loop over models, computing statistics for sum over forecast lags

		for (int i_model = 0; i_model < num_model; ++i_model) {

			// Accumulate sum

			LogLikeSet sum = new LogLikeSet();
			sum.zero_init (gamma_config, -1L, num_sim);

			for (int i_fc_lag = 0; i_fc_lag < num_fc_lag; ++i_fc_lag) {
				sum.add_from (gamma_config, log_like_sets[i_fc_lag][i_model], false);
			}

			// Compute statistics

			sum.compute_count_stats (gamma_config,
				obs_count[num_fc_lag][i_model]);
		}

		return;
	}




	// Compute the single-event gamma, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.

	public String single_event_gamma_to_string (GammaConfig gamma_config) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		double[][][][] gamma_lo = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];
		double[][][][] gamma_hi = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];

		single_event_gamma (gamma_config, gamma_lo, gamma_hi);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
					for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
						sb.append (
							gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + ",  "
							+ gamma_config.model_kind_to_string(i_model) + ",  "
							+ gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
							+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
							+ "gamma_lo = " + String.format ("%.6f", gamma_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + ",  "
							+ "gamma_hi = " + String.format ("%.6f", gamma_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + "\n"
						);
					}
				}
			}
		}

		return sb.toString();
	}




	// Compute the single-event zeta, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.
	//  keep_empty = True to retain lines that contain no data, false to omit them.

	public String single_event_zeta_to_string (GammaConfig gamma_config, boolean keep_empty) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		double[][][][] zeta_lo = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];
		double[][][][] zeta_hi = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];

		single_event_zeta (gamma_config, zeta_lo, zeta_hi);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
					for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
						if (keep_empty
							|| zeta_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin] > -0.5
							|| zeta_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin] > -0.5) {
							sb.append (
								gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + ",  "
								+ gamma_config.model_kind_to_string(i_model) + ",  "
								+ gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
								+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
								+ "zeta_lo = " + String.format ("%.6f", zeta_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + ",  "
								+ "zeta_hi = " + String.format ("%.6f", zeta_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + "\n"
							);
						}
					}
				}
			}
		}

		return sb.toString();
	}




	// Compute event count statistics, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.

	public String compute_count_stats_to_string (GammaConfig gamma_config) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		int[][][][] obs_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];
		int[][][][] sim_median_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];
		int[][][][] sim_fractile_5_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];
		int[][][][] sim_fractile_95_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];

		compute_count_stats (gamma_config, obs_count,
			sim_median_count, sim_fractile_5_count, sim_fractile_95_count);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
					for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
						sb.append (
							gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + ",  "
							+ gamma_config.model_kind_to_string(i_model) + ",  "
							+ gamma_config.adv_window_names[i_adv_win] + ",  "
							+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
							+ "obs = " + obs_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + ",  "
							+ "median = " + sim_median_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + ",  "
							+ "fractile_5 = " + sim_fractile_5_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + ",  "
							+ "fractile_95 = " + sim_fractile_95_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + "\n"
						);
					}
				}
			}
		}

		return sb.toString();
	}




	// Compute the single-event zeta, and convert the table to a string.
	// The string is formatted as a series of lines in a data file.
	// Parameters:
	//  gamma_config = Configuration information.
	//  i_eqk = Earthquake number to insert at the start of each line.
	//  keep_empty = True to retain lines that contain no data, false to omit them.

	public String single_event_zeta_to_lines (GammaConfig gamma_config, int i_eqk, boolean keep_empty) {

		// Number of forecast lags and aftershock models

		int num_fc_lag = gamma_config.forecast_lag_count;
		int num_model = gamma_config.model_kind_count;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		double[][][][] zeta_lo = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];
		double[][][][] zeta_hi = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];

		single_event_zeta (gamma_config, zeta_lo, zeta_hi);

		int[][][][] obs_count = new int[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];

		compute_count_stats (gamma_config, obs_count);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
			for (int i_model = 0; i_model < num_model; ++i_model) {
				for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
					for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
						if (keep_empty
							|| zeta_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin] > -0.5
							|| zeta_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin] > -0.5) {
							sb.append (
								i_eqk + " "
								+ i_fc_lag + " "
								+ i_model + " "
								+ i_adv_win + " "
								+ i_mag_bin + " "
								+ gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + " "
								+ gamma_config.model_kind_to_string(i_model).replace(' ', '-') + " "
								+ gamma_config.get_adv_window_name_or_sum(i_adv_win).replace(' ', '-') + " "
								+ gamma_config.adv_min_mag_bins[i_mag_bin] + " "
								+ obs_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + " "
								+ String.format ("%.6f", zeta_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + " "
								+ String.format ("%.6f", zeta_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + "\n"
							);
						}
					}
				}
			}
		}

		return sb.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 48001;

	private static final String M_VERSION_NAME = "EqkForecastSet";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 48000;
	protected static final int MARSHAL_EQK_FORECAST_SET = 48001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_EQK_FORECAST_SET;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalInt                (        "num_sim"        , num_sim        );
		ForecastMainshock.marshal_poly   (writer, "fcmain"         , fcmain         );
		LogLikeSet.marshal_2d_array_poly (writer, "log_like_sets"  , log_like_sets  );
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		num_sim         = reader.unmarshalInt                (        "num_sim"        );
		fcmain          = ForecastMainshock.unmarshal_poly   (reader, "fcmain"         );
		log_like_sets   = LogLikeSet.unmarshal_2d_array_poly (reader, "log_like_sets"  );

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public EqkForecastSet unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, EqkForecastSet obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static EqkForecastSet unmarshal_poly (MarshalReader reader, String name) {
		EqkForecastSet result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("EqkForecastSet.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_EQK_FORECAST_SET:
			result = new EqkForecastSet();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal an array of objects, polymorphic.

	public static void marshal_array_poly (MarshalWriter writer, String name, EqkForecastSet[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshal_poly (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Marshal a 2D array of objects, polymorphic.

	public static void marshal_2d_array_poly (MarshalWriter writer, String name, EqkForecastSet[][] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshal_array_poly (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects, polymorphic.

	public static EqkForecastSet[] unmarshal_array_poly (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		EqkForecastSet[] x = new EqkForecastSet[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshal_poly (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}

	// Unmarshal a 2d array of objects, polymorphic.

	public static EqkForecastSet[][] unmarshal_2d_array_poly (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		EqkForecastSet[][] x = new EqkForecastSet[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshal_array_poly (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----

	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("EqkForecastSet : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id
		// Compute all models at all forecast lags for the given event.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("EqkForecastSet : Invalid 'test1' subcommand");
				return;
			}

			String the_event_id = args[1];

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();

			// Number of forecast lags and aftershock models

			int num_fc_lag = gamma_config.forecast_lag_count;
			int num_model = gamma_config.model_kind_count;

			// Number of advisory windows and magnitude bins

			int num_adv_win = gamma_config.adv_window_count;
			int num_mag_bin = gamma_config.adv_min_mag_bin_count;

			// Fetch the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Compute models

			System.out.println ("");
			System.out.println ("Computing models for event_id = " + the_event_id);

			EqkForecastSet eqk_forecast_set = new EqkForecastSet();
			eqk_forecast_set.run_simulations (gamma_config,
				gamma_config.simulation_count, fcmain, false);

			double[][][][] gamma_lo = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];
			double[][][][] gamma_hi = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];

			eqk_forecast_set.single_event_gamma (gamma_config, gamma_lo, gamma_hi);

			System.out.println ("");
			for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
				for (int i_model = 0; i_model < num_model; ++i_model) {
					for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
						for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
							System.out.println (
								gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + ",  "
								+ gamma_config.model_kind_to_string(i_model) + ",  "
								+ gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
								+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
								+ "gamma_lo = " + String.format ("%.6f", gamma_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + ",  "
								+ "gamma_hi = " + String.format ("%.6f", gamma_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin])
							);
						}
					}
				}
			}

			int[][][][] obs_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];
			int[][][][] sim_median_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];
			int[][][][] sim_fractile_5_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];
			int[][][][] sim_fractile_95_count = new int[num_fc_lag + 1][num_model][num_adv_win][num_mag_bin];

			eqk_forecast_set.compute_count_stats (gamma_config, obs_count,
				sim_median_count, sim_fractile_5_count, sim_fractile_95_count);

			System.out.println ("");
			for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
				for (int i_model = 0; i_model < num_model; ++i_model) {
					for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
						for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
							System.out.println (
								gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + ",  "
								+ gamma_config.model_kind_to_string(i_model) + ",  "
								+ gamma_config.adv_window_names[i_adv_win] + ",  "
								+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
								+ "obs = " + obs_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + ",  "
								+ "median = " + sim_median_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + ",  "
								+ "fractile_5 = " + sim_fractile_5_count[i_fc_lag][i_model][i_adv_win][i_mag_bin] + ",  "
								+ "fractile_95 = " + sim_fractile_95_count[i_fc_lag][i_model][i_adv_win][i_mag_bin]
							);
						}
					}
				}
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id
		// Compute all models at all forecast lags for the given event.
		// This test displays the cumulative probability (zeta) statistic.

		if (args[0].equalsIgnoreCase ("test2")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("EqkForecastSet : Invalid 'test2' subcommand");
				return;
			}

			String the_event_id = args[1];

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();

			// Number of forecast lags and aftershock models

			int num_fc_lag = gamma_config.forecast_lag_count;
			int num_model = gamma_config.model_kind_count;

			// Number of advisory windows and magnitude bins

			int num_adv_win = gamma_config.adv_window_count;
			int num_mag_bin = gamma_config.adv_min_mag_bin_count;

			// Fetch the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Compute models

			System.out.println ("");
			System.out.println ("Computing models for event_id = " + the_event_id);

			EqkForecastSet eqk_forecast_set = new EqkForecastSet();
			eqk_forecast_set.run_simulations (gamma_config,
				gamma_config.simulation_count, fcmain, false);

			double[][][][] zeta_lo = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];
			double[][][][] zeta_hi = new double[num_fc_lag + 1][num_model][num_adv_win + 1][num_mag_bin];

			eqk_forecast_set.single_event_zeta (gamma_config, zeta_lo, zeta_hi);

			System.out.println ("");
			for (int i_fc_lag = 0; i_fc_lag <= num_fc_lag; ++i_fc_lag) {
				for (int i_model = 0; i_model < num_model; ++i_model) {
					for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
						for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
							System.out.println (
								gamma_config.get_forecast_lag_string_or_sum(i_fc_lag) + ",  "
								+ gamma_config.model_kind_to_string(i_model) + ",  "
								+ gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
								+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
								+ "zeta_lo = " + String.format ("%.6f", zeta_lo[i_fc_lag][i_model][i_adv_win][i_mag_bin]) + ",  "
								+ "zeta_hi = " + String.format ("%.6f", zeta_hi[i_fc_lag][i_model][i_adv_win][i_mag_bin])
							);
						}
					}
				}
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id  keep_empty
		// Compute all models at all forecast lags for the given event.
		// If keep_empty is true, lines with no data are retained.
		// This test displays the cumulative probability (zeta) statistic.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("EqkForecastSet : Invalid 'test3' subcommand");
				return;
			}

			String the_event_id = args[1];
			boolean keep_empty = Boolean.parseBoolean (args[2]);

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();

			// Number of forecast lags and aftershock models

			int num_fc_lag = gamma_config.forecast_lag_count;
			int num_model = gamma_config.model_kind_count;

			// Number of advisory windows and magnitude bins

			int num_adv_win = gamma_config.adv_window_count;
			int num_mag_bin = gamma_config.adv_min_mag_bin_count;

			// Fetch the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Compute models

			System.out.println ("");
			System.out.println ("Computing models for event_id = " + the_event_id);

			EqkForecastSet eqk_forecast_set = new EqkForecastSet();
			eqk_forecast_set.run_simulations (gamma_config,
				gamma_config.simulation_count, fcmain, false);

			// Display lines

			System.out.println ("");
			System.out.println (eqk_forecast_set.single_event_zeta_to_lines (gamma_config, 0, keep_empty));

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("EqkForecastSet : Unrecognized subcommand : " + args[0]);
		return;
	}
}
