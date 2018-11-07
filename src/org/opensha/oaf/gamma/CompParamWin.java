package org.opensha.oaf.gamma;

import java.util.List;
import java.util.Arrays;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.RJ_AftershockModel;
import org.opensha.oaf.rj.RJ_AftershockModel_Generic;
import org.opensha.oaf.rj.GenericRJ_Parameters;
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;
import org.opensha.oaf.rj.MagCompPage_Parameters;
import org.opensha.oaf.rj.MagCompPage_ParametersFetch;
import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.SeqSpecRJ_Parameters;
import org.opensha.oaf.rj.RJ_Summary_Generic;

import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Functions to compare R&J parameters for two time windows.
 * Author: Michael Barall 11/06/2018.
 */
public class CompParamWin {

	//----- Data -----

	// Start and end lags for a window.
	// This is an immutable object.

	public static class StartEnd {

		// Start and end of the window, in milliseconds, relative to mainshock time.

		private long start_lag;
		private long end_lag;

		// Constructor sets the values.

		public StartEnd (long the_start_lag, long the_end_lag) {
			start_lag = the_start_lag;
			end_lag = the_end_lag;
		}

		// Get the values.

		public long get_start_lag () {
			return start_lag;
		}

		public long get_end_lag () {
			return end_lag;
		}
	}

	// Holds data for a single model and window.

	public static class ModelWinData {

		// Flag indicating we have data.

		public boolean has_data = false;

		// The calculated mean, standard deviation, and maximum likelihood value of the a-value.

		public double stat_a_mean = 0.0;
		public double stat_a_sdev = -1.0;		// indicates no data
		public double stat_a_like = 0.0;

		// Set the values from an R&J model.

		public void set (RJ_AftershockModel rj_model) {
			stat_a_mean = rj_model.getMean_a();
			stat_a_sdev = rj_model.getStdDev_a();
			stat_a_like = rj_model.getMaxLike_a();
			has_data = true;
			return;
		}

		// Initialize to zero, with no data.

		public void zero_init () {
			stat_a_mean = 0.0;
			stat_a_sdev = -1.0;
			stat_a_like = 0.0;
			has_data = false;
			return;
		}

		// Get a string for insertion into a line of a table.

		public String string_for_line () {
			return String.format ("%.6f %.6f %.6f", stat_a_mean, stat_a_sdev, stat_a_like);
		}
	}

	// Number of windows.

	private int num_win;

	// List of windows.
	// Dimension of the array is:
	//  win_list[num_win]

	private StartEnd[] win_list;

	// Information about the mainshock.

	private ForecastMainshock fcmain;

	// Array of data sets for the earthquake.
	// Dimension of the array is
	//  obs_log_like[model_kind_count][num_win].
	
	private ModelWinData[][] model_win_data;




	//----- Construction -----

	// Constructor creates an empty object.

	public CompParamWin () {
		fcmain = null;
		num_win = -1;
		model_win_data = null;
	}




	// Calculate parameters.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_win_list = List of time windows.
	//  the_fcmain = Mainshock information.
	//  verbose = True to write output for each simulation.

	public void calc_params (GammaConfig gamma_config, StartEnd[] the_win_list,
		ForecastMainshock the_fcmain, boolean verbose) {

		// Save time windows and mainshock information

		num_win = the_win_list.length;
		win_list = the_win_list.clone();
		fcmain = the_fcmain;

		// Number of aftershock models

		int num_model = gamma_config.model_kind_count;

		// Allocate all the arrays

		model_win_data = new ModelWinData[num_model][num_win];

		for (int i_model = 0; i_model < num_model; ++i_model) {
			for (int i_win = 0; i_win < num_win; ++i_win) {
				model_win_data[i_model][i_win] = new ModelWinData();
			}
		}

		// Loop over windows ...

		for (int i_win = 0; i_win < num_win; ++i_win) {

			// Get the forecast lag and start lag

			long forecast_lag = win_list[i_win].get_end_lag();
			long start_lag = win_list[i_win].get_start_lag();

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (forecast_lag, fcmain, null, start_lag);

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all (fcmain.mainshock_time + forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "", fcmain, params, true);

			//if (!( results.generic_result_avail
			//	&& results.seq_spec_result_avail
			//	&& results.bayesian_result_avail )) {
			//	throw new RuntimeException ("CompParamWin: Failed to compute aftershock models");
			//}

			if (!( results.generic_result_avail )) {
				throw new RuntimeException ("CompParamWin: Failed to compute aftershock models");
			}

			// Generic model

			model_win_data[GammaConfig.MODEL_KIND_GENERIC][i_win].set (results.generic_model);

			// Sequence specific model

			if (results.seq_spec_result_avail
				&& results.seq_spec_model.get_num_aftershocks() >= gamma_config.seq_spec_min_aftershocks) {

				model_win_data[GammaConfig.MODEL_KIND_SEQ_SPEC][i_win].set (results.seq_spec_model);
			}
			else {
				model_win_data[GammaConfig.MODEL_KIND_SEQ_SPEC][i_win].zero_init ();
			}

			// Bayesian model

			if (results.bayesian_result_avail
				&& results.seq_spec_result_avail
				&& results.seq_spec_model.get_num_aftershocks() >= gamma_config.bayesian_min_aftershocks) {

				model_win_data[GammaConfig.MODEL_KIND_BAYESIAN][i_win].set (results.bayesian_model);
			}
			else {
				model_win_data[GammaConfig.MODEL_KIND_BAYESIAN][i_win].zero_init ();
			}
		}

		return;
	}




	// Return true if all windows for the given model have data.

	private boolean all_win_have_data (int i_model) {
		for (int i_win = 0; i_win < num_win; ++i_win) {
			if (!( model_win_data[i_model][i_win].has_data )) {
				return false;
			}
		}
		return true;
	}




	// Convert the comparison table to a string.
	// The string is formatted as a series of lines in a data file.
	// Parameters:
	//  gamma_config = Configuration information.
	//  i_eqk = Earthquake number to insert at the start of each line.
	//  keep_empty = True to retain lines that do not contain full data, false to omit them.

	public String single_event_comp_to_lines (GammaConfig gamma_config, int i_eqk, boolean keep_empty) {

		// Number of aftershock models

		int num_model = gamma_config.model_kind_count;

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_model = 0; i_model < num_model; ++i_model) {
			if (keep_empty || all_win_have_data (i_model)) {
				sb.append (
					i_eqk + " "
					+ i_model + " "
					+ gamma_config.model_kind_to_string(i_model).replace(' ', '-')
				);
				for (int i_win = 0; i_win < num_win; ++i_win) {
					sb.append (" " + model_win_data[i_model][i_win].string_for_line());
				}
				sb.append ("\n");
			}
		}

		return sb.toString();
	}




	//----- Testing -----

	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("CompParamWin : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  f_keep  start_lag_1  end_lag_1  [start_lag_2  end_lag_2]...
		// Compute all models at for the given event, for each window.
		// Lags are in java.time.Duration format.

		if (args[0].equalsIgnoreCase ("test1")) {

			// At least 5 arguments, and an odd number

			if (args.length < 5 || args.length % 2 != 1) {
				System.err.println ("CompParamWin : Invalid 'test1' subcommand");
				return;
			}

			String the_event_id = args[1];
			boolean f_keep = Boolean.parseBoolean (args[2]);

			// Get the list of windows

			int my_num_win = (args.length - 3) / 2;
			StartEnd[] my_win_list = new StartEnd[my_num_win];
			for (int i_win = 0; i_win < my_num_win; ++i_win) {
				long my_start_lag = SimpleUtils.string_to_duration (args[2*i_win + 3]);
				long my_end_lag = SimpleUtils.string_to_duration (args[2*i_win + 4]);
				my_win_list[i_win] = new StartEnd (my_start_lag, my_end_lag);
			}

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();

			// Number of aftershock models

			int num_model = gamma_config.model_kind_count;

			// Fetch the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Compute models

			System.out.println ("");
			System.out.println ("Computing models for event_id = " + the_event_id);

			System.out.println ("");
			for (int i_win = 0; i_win < my_num_win; ++i_win) {
				System.out.println ("Window " + i_win + ": "
					+ SimpleUtils.duration_to_string_2 (my_win_list[i_win].get_start_lag()) + " to "
					+ SimpleUtils.duration_to_string_2 (my_win_list[i_win].get_end_lag()));
			}
			System.out.println ("");

			CompParamWin comp_param_win = new CompParamWin();
			comp_param_win.calc_params (gamma_config, my_win_list, fcmain, false);

			System.out.println ("");
			System.out.println (comp_param_win.single_event_comp_to_lines (gamma_config, 0, f_keep));

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("CompParamWin : Unrecognized subcommand : " + args[0]);
		return;
	}
}
