package org.opensha.oaf.aftershockStatistics.gamma;

import java.util.List;
import java.util.Arrays;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.aftershockStatistics.comcat.ComcatAccessor;
import org.opensha.oaf.aftershockStatistics.AftershockStatsCalc;
import org.opensha.oaf.aftershockStatistics.RJ_AftershockModel;
import org.opensha.oaf.aftershockStatistics.RJ_AftershockModel_Generic;

import org.opensha.oaf.aftershockStatistics.aafs.ForecastMainshock;
import org.opensha.oaf.aftershockStatistics.aafs.ForecastParameters;
import org.opensha.oaf.aftershockStatistics.aafs.ForecastResults;

import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;
import org.opensha.oaf.aftershockStatistics.util.MarshalReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalWriter;
import org.opensha.oaf.aftershockStatistics.util.MarshalException;


/**
 * A set of cumulative probabilities an aftershock forecast.
 * Author: Michael Barall 10/09/2018.
 *
 * This object holds a set of cumulative probabilities and event counts for a single
 * earthquake, forecast lag, and aftershock model.
 */
public class CumProbSet {

	//----- Data -----

	// The forecast lag, in milliseconds.

	private long forecast_lag;

	// Number of simulations (not used for this class).

	private int num_sim;

	// True if explicitly set to zero.

	private boolean is_zero;

	// Array of low cumulative probabilities for observed aftershock sequence.
	// (Probability that number of aftershocks is less than observed number.)
	// Dimension of the array is
	//  obs_zeta_lo[adv_window_count][adv_min_mag_bin_count].
	
	private double[][] obs_zeta_lo;

	// Array of high cumulative probabilities for observed aftershock sequence.
	// (Probability that number of aftershocks is less than or equal to observed number.)
	// Dimension of the array is
	//  obs_zeta_hi[adv_window_count][adv_min_mag_bin_count].
	
	private double[][] obs_zeta_hi;

	// Array of event counts for observed aftershock sequence.
	// Dimension of the array is
	//  obs_event_count[adv_window_count][adv_min_mag_bin_count].
	
	private int[][] obs_event_count;





	//----- Construction -----

	// Constructor creates an empty object.

	public CumProbSet () {
		forecast_lag = -1L;
		num_sim = -1;
		is_zero = false;
		obs_zeta_lo = null;
		obs_zeta_hi = null;
		obs_event_count = null;
	}




	// Compute cumulative probabilities for a mainshock and aftershock sequence.
	// (Despite the name, this function does not run simulations, it uses the analytic formulas.)
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds.
	//  the_num_sim = The number of simulations to run (not used for this class).
	//  fcmain = Mainshock information.
	//  model = RJ aftershock model, including transient data.
	//  all_aftershocks = Aftershock sequence used to set observed data.
	//  verbose = True to write output for each simulation.

	public void run_simulations (GammaConfig gamma_config, long the_forecast_lag, int the_num_sim,
		ForecastMainshock fcmain, RJ_AftershockModel model, List<ObsEqkRupture> all_aftershocks, boolean verbose) {

		// Save forecast lag and number of simulations

		forecast_lag = the_forecast_lag;
		num_sim = the_num_sim;

		// Adjust zero flag

		is_zero = false;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// If no epistemic uncertainty desired, replace the model with a fixed-parameter model

		if (gamma_config.no_epistemic_uncertainty) {
			model = RJ_AftershockModel_Generic.from_max_like (model);
		}

		// Allocate all the arrays

		obs_zeta_lo = new double[num_adv_win][num_mag_bin];
		obs_zeta_hi = new double[num_adv_win][num_mag_bin];
		obs_event_count = new int[num_adv_win][num_mag_bin];

		// Compute the probability distribution of the model

		ProbDistSet prob_dist_set = new ProbDistSet (gamma_config, forecast_lag, model);

		// Compute the bin count and cumulative probabilities for the observed aftershock sequence

		int[][] bin_count = prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);

		double[][] zeta_lo = new double[num_adv_win][num_mag_bin];
		double[][] zeta_hi = new double[num_adv_win][num_mag_bin];
		prob_dist_set.compute_cum_prob (gamma_config, bin_count, zeta_lo, zeta_hi);

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				obs_event_count[i_adv_win][i_mag_bin] = bin_count[i_adv_win][i_mag_bin];
				obs_zeta_lo[i_adv_win][i_mag_bin] = zeta_lo[i_adv_win][i_mag_bin];
				obs_zeta_hi[i_adv_win][i_mag_bin] = zeta_hi[i_adv_win][i_mag_bin];
			}
		}

		return;
	}




	// Allocate and zero-initialize all arrays.
	// (zeto_lo and zeta_hi are initialized to -1.0, representing no data).
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds, can be -1L for sums over forecast lags.
	//  the_num_sim = The number of simulations to run (not used in this class).

	public void zero_init (GammaConfig gamma_config, long the_forecast_lag, int the_num_sim) {

		// Save forecast lag and number of simulations

		forecast_lag = the_forecast_lag;
		num_sim = the_num_sim;

		// Adjust zero flag

		is_zero = true;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Allocate all the arrays

		obs_zeta_lo = new double[num_adv_win][num_mag_bin];
		obs_zeta_hi = new double[num_adv_win][num_mag_bin];
		obs_event_count = new int[num_adv_win][num_mag_bin];

		// Zero-initialize

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				obs_event_count[i_adv_win][i_mag_bin] = 0;
				//obs_zeta_lo[i_adv_win][i_mag_bin] = 0.0;
				//obs_zeta_hi[i_adv_win][i_mag_bin] = 1.0;
				obs_zeta_lo[i_adv_win][i_mag_bin] = -1.0;
				obs_zeta_hi[i_adv_win][i_mag_bin] = -1.0;
			}
		}

		return;
	}




	//----- Querying -----

	// Compute the single-event cumulative probabilities.
	// Parameters:
	//  gamma_config = Configuration information.
	//  zeta_lo = Array to receive low cumulative probability, dimension zeta_lo[adv_window_count][adv_min_mag_bin_count].
	//  zeta_hi = Array to receive high cumulative probability, dimension zeta_hi[adv_window_count][adv_min_mag_bin_count].

	public void single_event_zeta (GammaConfig gamma_config, double[][] zeta_lo, double[][] zeta_hi) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Loop over windows and magnitude bins, computing zeta for each

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				// Compute zetas

				zeta_lo[i_adv_win][i_mag_bin] = obs_zeta_lo[i_adv_win][i_mag_bin];
				zeta_hi[i_adv_win][i_mag_bin] = obs_zeta_hi[i_adv_win][i_mag_bin];
			}
		}

		return;
	}




	// Compute event count statistics.
	// Parameters:
	//  gamma_config = Configuration information.
	//  obs_count = Array to receive observed count, dimension obs_count[adv_window_count][adv_min_mag_bin_count].

	public void compute_count_stats (GammaConfig gamma_config, int[][] obs_count) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Loop over windows and magnitude bins, computing statistics for each

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				// Observed count

				obs_count[i_adv_win][i_mag_bin] = obs_event_count[i_adv_win][i_mag_bin];
			}
		}

		return;
	}




	// Compute the single-event zeta, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.

	public String single_event_zeta_to_string (GammaConfig gamma_config) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		double[][] zeta_lo = new double[num_adv_win][num_mag_bin];
		double[][] zeta_hi = new double[num_adv_win][num_mag_bin];

		single_event_zeta (gamma_config, zeta_lo, zeta_hi);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				sb.append (
					gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
					+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
					+ "zeta_lo = " + String.format ("%.6f", zeta_lo[i_adv_win][i_mag_bin]) + ",  "
					+ "zeta_hi = " + String.format ("%.6f", zeta_hi[i_adv_win][i_mag_bin]) + "\n"
				);
			}
		}

		return sb.toString();
	}




	// Compute event count statistics, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.

	public String compute_count_stats_to_string (GammaConfig gamma_config) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		int[][] generic_obs_count = new int[num_adv_win][num_mag_bin];

		compute_count_stats (gamma_config, generic_obs_count);

		// Convert to string

		StringBuilder sb = new StringBuilder();
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				sb.append (
					gamma_config.adv_window_names[i_adv_win] + ",  "
					+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
					+ "obs = " + generic_obs_count[i_adv_win][i_mag_bin] + "\n"
				);
			}
		}

		return sb.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 49001;

	private static final String M_VERSION_NAME = "CumProbSet";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 49000;
	protected static final int MARSHAL_CUM_PROB_SET = 49001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_CUM_PROB_SET;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalLong          ("forecast_lag"   , forecast_lag   );
		writer.marshalInt           ("num_sim"        , num_sim        );
		writer.marshalBoolean       ("is_zero"        , is_zero        );
		writer.marshalDouble2DArray ("obs_zeta_lo"    , obs_zeta_lo    );
		writer.marshalDouble2DArray ("obs_zeta_hi"    , obs_zeta_hi    );
		writer.marshalInt2DArray    ("obs_event_count", obs_event_count);
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		forecast_lag    = reader.unmarshalLong          ("forecast_lag"   );
		num_sim         = reader.unmarshalInt           ("num_sim"        );
		is_zero         = reader.unmarshalBoolean       ("is_zero"        );
		obs_zeta_lo     = reader.unmarshalDouble2DArray ("obs_zeta_lo"    );
		obs_zeta_hi     = reader.unmarshalDouble2DArray ("obs_zeta_hi"    );
		obs_event_count = reader.unmarshalInt2DArray    ("obs_event_count");

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

	public CumProbSet unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, CumProbSet obj) {

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

	public static CumProbSet unmarshal_poly (MarshalReader reader, String name) {
		CumProbSet result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("CumProbSet.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CUM_PROB_SET:
			result = new CumProbSet();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal an array of objects, polymorphic.

	public static void marshal_array_poly (MarshalWriter writer, String name, CumProbSet[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshal_poly (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Marshal a 2D array of objects, polymorphic.

	public static void marshal_2d_array_poly (MarshalWriter writer, String name, CumProbSet[][] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshal_array_poly (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects, polymorphic.

	public static CumProbSet[] unmarshal_array_poly (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		CumProbSet[] x = new CumProbSet[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshal_poly (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}

	// Unmarshal a 2d array of objects, polymorphic.

	public static CumProbSet[][] unmarshal_2d_array_poly (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		CumProbSet[][] x = new CumProbSet[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshal_array_poly (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	// Test routine, to compute and show cumulative probabilities for a model.
	// Parameters are the same as run_simulations.

	private static void compute_and_show_cum_prob (GammaConfig gamma_config, long the_forecast_lag, int the_num_sim,
		ForecastMainshock fcmain, RJ_AftershockModel model, List<ObsEqkRupture> all_aftershocks, boolean verbose) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute cumulative probabilities and collect results

		CumProbSet cum_prob_set = new CumProbSet();
		cum_prob_set.run_simulations (gamma_config, the_forecast_lag, the_num_sim,
			fcmain, model, all_aftershocks, verbose);

		double[][] zeta_lo = new double[num_adv_win][num_mag_bin];
		double[][] zeta_hi = new double[num_adv_win][num_mag_bin];

		cum_prob_set.single_event_zeta (gamma_config, zeta_lo, zeta_hi);

		// Display results

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				System.out.println (
					gamma_config.adv_window_names[i_adv_win] + ",  "
					+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
					+ "zeta_lo = " + String.format ("%.6f", zeta_lo[i_adv_win][i_mag_bin]) + ",  "
					+ "zeta_hi = " + String.format ("%.6f", zeta_hi[i_adv_win][i_mag_bin])
				);
			}
		}
		
		return;
	}




	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("CumProbSet : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  forecast_lag
		// Compute model for the given event at the given forecast lag.
		// The forecast_lag is given in java.time.Duration format.
		// Show the cumulative probabilities for the given model and the given forecast lag.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("CumProbSet : Invalid 'test1' subcommand");
				return;
			}

			String the_event_id = args[1];
			long the_forecast_lag = SimpleUtils.string_to_duration (args[2]);

			int the_num_sim = 0;
			//boolean discard_large_as = true;

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();
			//gamma_config.discard_sim_with_large_as = discard_large_as;

			// Number of advisory windows and magnitude bins

			int num_adv_win = gamma_config.adv_window_count;
			int num_mag_bin = gamma_config.adv_min_mag_bin_count;

			// Fetch the mainshock info

			ForecastMainshock fcmain = new ForecastMainshock();
			fcmain.setup_mainshock_only (the_event_id);

			System.out.println ("");
			System.out.println (fcmain.toString());

			// Get parameters

			ForecastParameters params = new ForecastParameters();
			params.fetch_all_params (the_forecast_lag, fcmain, null);

			// Get results

			ForecastResults results = new ForecastResults();
			results.calc_all (fcmain.mainshock_time + the_forecast_lag, ForecastResults.ADVISORY_LAG_WEEK, "", fcmain, params, true);

			if (!( results.generic_result_avail
				&& results.seq_spec_result_avail
				&& results.bayesian_result_avail )) {
				throw new RuntimeException ("CumProbSet: Failed to compute aftershock models");
			}

			// Get catalog of all aftershocks

			List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			compute_and_show_cum_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, results.generic_model, all_aftershocks, false);

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			compute_and_show_cum_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, results.seq_spec_model, all_aftershocks, false);

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			compute_and_show_cum_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, results.bayesian_model, all_aftershocks, false);

			// Zero-epistemic generic model with no epistemic uncertainty

			System.out.println ("");
			System.out.println ("Zero-epistemic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			RJ_AftershockModel_Generic ze_model = RJ_AftershockModel_Generic.from_max_like (results.generic_model);

			compute_and_show_cum_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, ze_model, all_aftershocks, false);

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("CumProbSet : Unrecognized subcommand : " + args[0]);
		return;
	}
}
