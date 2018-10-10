package scratch.aftershockStatistics.gamma;

import java.util.List;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import scratch.aftershockStatistics.ComcatAccessor;
import scratch.aftershockStatistics.AftershockStatsCalc;
import scratch.aftershockStatistics.RJ_AftershockModel;

import scratch.aftershockStatistics.aafs.ForecastMainshock;
import scratch.aftershockStatistics.aafs.ForecastParameters;
import scratch.aftershockStatistics.aafs.ForecastResults;

import scratch.aftershockStatistics.util.SimpleUtils;
import scratch.aftershockStatistics.util.MarshalReader;
import scratch.aftershockStatistics.util.MarshalWriter;
import scratch.aftershockStatistics.util.MarshalException;


/**
 * A set of log-likelihoods for an aftershock forecast.
 * Author: Michael Barall 10/09/2018.
 *
 * This object holds a set of log-likelihoods and event counts for a single
 * earthquake, forecast lag, and aftershock model. For each combinarion of
 * advisory window and magnitude bin, there is an array of log-likelihoods
 * and event counts for a series of simulations.  There are also log-likelihoods
 * and event counts for the observed aftershock sequence.
 */
public class LogLikeSet {

	//----- Data -----

	// The forecast lag, in milliseconds.

	private long forecast_lag;

	// Number of simulations.

	private int num_sim;

	// Array of log-likelihoods for simulations.
	// Dimension of the array is
	//  sim_log_like[adv_window_count][adv_min_mag_bin_count][num_sim].
	
	private double[][][] sim_log_like;

	// Array of event counts for simulations.
	// Dimension of the array is
	//  sim_event_count[adv_window_count][adv_min_mag_bin_count][num_sim].
	
	private int[][][] sim_event_count;

	// Array of log-likelihoods for observed aftershock sequence.
	// Dimension of the array is
	//  obs_log_like[adv_window_count][adv_min_mag_bin_count].
	
	private double[][] obs_log_like;

	// Array of event counts for observed aftershock sequence.
	// Dimension of the array is
	//  obs_event_count[adv_window_count][adv_min_mag_bin_count].
	
	private int[][] obs_event_count;





	//----- Construction -----

	// Constructor creates an empty object.

	public LogLikeSet () {
		forecast_lag = -1L;
		num_sim = -1;
		sim_log_like = null;
		sim_event_count = null;
		obs_log_like = null;
		obs_event_count = null;
	}




	// Run simulations.
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds.
	//  the_num_sim = The number of simulations to run.
	//  fcmain = Mainshock information.
	//  model = RJ aftershock model, including transient data.
	//  all_aftershocks = Aftershock sequence used to set observed data.
	//  verbose = True to write output for each simulation.

	private void run_simulations (GammaConfig gamma_config, long the_forecast_lag, int the_num_sim,
		ForecastMainshock fcmain, RJ_AftershockModel model, List<ObsEqkRupture> all_aftershocks, boolean verbose) {

		// Save forecast lag and number of simulations

		forecast_lag = the_forecast_lag;
		num_sim = the_num_sim;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Allocate all the arrays

		sim_log_like = new double[num_adv_win][num_mag_bin][num_sim];
		sim_event_count = new int[num_adv_win][num_mag_bin][num_sim];

		obs_log_like = new double[num_adv_win][num_mag_bin];
		obs_event_count = new int[num_adv_win][num_mag_bin];

		// Compute the probability distribution of the model

		ProbDistSet prob_dist_set = new ProbDistSet (gamma_config, forecast_lag, model);

		// Compute the bin count and log-likelihoods for the observed aftershock sequence

		int[][] bin_count = prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
		double[][] log_like = prob_dist_set.compute_log_like (gamma_config, bin_count);

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				obs_event_count[i_adv_win][i_mag_bin] = bin_count[i_adv_win][i_mag_bin];
				obs_log_like[i_adv_win][i_mag_bin] = log_like[i_adv_win][i_mag_bin];
			}
		}

		// Loop to compute simulations

		int i_sim = 0;
		while (i_sim < num_sim) {

			// Sample a/p/c parameters from the model

			double[] apcval = new double[3];
			model.sample_apc (gamma_config.rangen.sample(), apcval);

			// Parameters for the simulation
		
			double a = apcval[0];
			double b = model.get_b();
			double magMain = fcmain.mainshock_mag;
			double magCat = gamma_config.adv_min_mag_bins[0];
			double capG = 10.0;
			double capH = 0.0;
			double p = apcval[1];
			double c = apcval[2];
			double tMinDays = ((double)(forecast_lag)) / ComcatAccessor.day_millis;
			double tMaxDays = ((double)(forecast_lag + gamma_config.max_adv_window_end_off)) / ComcatAccessor.day_millis;

			// Run the simulation

			ObsEqkRupList sim_aftershocks = AftershockStatsCalc.simAftershockSequence (
						a, b, magMain, magCat, capG, capH, p, c, tMinDays, tMaxDays, gamma_config.rangen);

			// Find the maximum magnitude

			double max_mag = -1000.0;
			for (ObsEqkRupture rup : sim_aftershocks) {
				double mag = rup.getMag();
				if (max_mag < mag) {
					max_mag = mag;
				}
			}

			// If the simulation has an aftershock larger than the mainshock, discard it

			if (max_mag > magMain) {
				if (verbose) {
					System.out.println ("Discarding simulation, max_mag = " + max_mag);
				}
				continue;
			}

			if (verbose) {
				System.out.println ("Simulation " + i_sim + ", count = " + sim_aftershocks.size());
			}

			// Compute the bin count and log-likelihoods for the observed aftershock sequence

			bin_count = prob_dist_set.count_bins (gamma_config, sim_aftershocks, 0L);
			log_like = prob_dist_set.compute_log_like (gamma_config, bin_count);

			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					sim_event_count[i_adv_win][i_mag_bin][i_sim] = bin_count[i_adv_win][i_mag_bin];
					sim_log_like[i_adv_win][i_mag_bin][i_sim] = log_like[i_adv_win][i_mag_bin];
				}
			}

			// Next simulation

			++i_sim;
		}

		return;
	}





	//----- Querying -----

	// Compute the single-event gamma.
	// Parameters:
	//  gamma_config = Configuration information.
	//  gamma_lo = Array to receive low value of gamma.
	//  gamma_hi = Array to receive high value of gamma.

	public void single_event_gamma (GammaConfig gamma_config, double[][] gamma_lo, double[][] gamma_hi) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Loop over windows and magnitude bins

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				// Accumulate number of simulations with log-likelihood below and above observation

				int below = 0;
				int above = 0;
				for (int i_sim = 0; i_sim < num_sim; ++i_sim) {
					if (sim_log_like[i_adv_win][i_mag_bin][i_sim] < obs_log_like[i_adv_win][i_mag_bin]) {
						++below;
					}
					else if (sim_log_like[i_adv_win][i_mag_bin][i_sim] > obs_log_like[i_adv_win][i_mag_bin]) {
						++above;
					}
				}

				// Compute gammas

				gamma_lo[i_adv_win][i_mag_bin] = ((double)below) / ((double)num_sim);
				gamma_hi[i_adv_win][i_mag_bin] = ((double)(num_sim - above)) / ((double)num_sim);
			}
		}

		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 47001;

	private static final String M_VERSION_NAME = "LogLikeSet";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 47000;
	protected static final int MARSHAL_LOG_LIKE_SET = 47001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_LOG_LIKE_SET;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalLong          ("forecast_lag"   , forecast_lag   );
		writer.marshalInt           ("num_sim"        , num_sim        );
		writer.marshalDouble3DArray ("sim_log_like"   , sim_log_like   );
		writer.marshalInt3DArray    ("sim_event_count", sim_event_count);
		writer.marshalDouble2DArray ("obs_log_like"   , obs_log_like   );
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
		sim_log_like    = reader.unmarshalDouble3DArray ("sim_log_like"   );
		sim_event_count = reader.unmarshalInt3DArray    ("sim_event_count");
		obs_log_like    = reader.unmarshalDouble2DArray ("obs_log_like"   );
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

	public LogLikeSet unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, LogLikeSet obj) {

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

	public static LogLikeSet unmarshal_poly (MarshalReader reader, String name) {
		LogLikeSet result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("LogLikeSet.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_LOG_LIKE_SET:
			result = new LogLikeSet();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----

	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("LogLikeSet : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  forecast_lag
		// Compute model for the given event at the given forecast lag.
		// The forecast_lag is given in java.time.Duration format.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("LogLikeSet : Invalid 'test1' subcommand");
				return;
			}

			String the_event_id = args[1];
			long the_forecast_lag = SimpleUtils.string_to_duration (args[2]);

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();

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
				throw new RuntimeException ("LogLikeSet: Failed to compute aftershock models");
			}

			// Get catalog of all aftershocks

			List<ObsEqkRupture> all_aftershocks = ProbDistSet.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string (the_forecast_lag));

			LogLikeSet generic_log_like_set = new LogLikeSet();
			generic_log_like_set.run_simulations (gamma_config, the_forecast_lag, gamma_config.simulation_count,
				fcmain, results.generic_model, all_aftershocks, true);

			double[][] generic_gamma_lo = new double[num_adv_win][num_mag_bin];
			double[][] generic_gamma_hi = new double[num_adv_win][num_mag_bin];

			generic_log_like_set.single_event_gamma (gamma_config, generic_gamma_lo, generic_gamma_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "gamma_lo = " + generic_gamma_lo[i_adv_win][i_mag_bin] + ",  "
						+ "gamma_hi = " + generic_gamma_hi[i_adv_win][i_mag_bin]
					);
				}
			}

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string (the_forecast_lag));

			LogLikeSet seq_spec_log_like_set = new LogLikeSet();
			seq_spec_log_like_set.run_simulations (gamma_config, the_forecast_lag, gamma_config.simulation_count,
				fcmain, results.seq_spec_model, all_aftershocks, true);

			double[][] seq_spec_gamma_lo = new double[num_adv_win][num_mag_bin];
			double[][] seq_spec_gamma_hi = new double[num_adv_win][num_mag_bin];

			seq_spec_log_like_set.single_event_gamma (gamma_config, seq_spec_gamma_lo, seq_spec_gamma_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "gamma_lo = " + seq_spec_gamma_lo[i_adv_win][i_mag_bin] + ",  "
						+ "gamma_hi = " + seq_spec_gamma_hi[i_adv_win][i_mag_bin]
					);
				}
			}

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string (the_forecast_lag));

			LogLikeSet bayesian_log_like_set = new LogLikeSet();
			bayesian_log_like_set.run_simulations (gamma_config, the_forecast_lag, gamma_config.simulation_count,
				fcmain, results.bayesian_model, all_aftershocks, true);

			double[][] bayesian_gamma_lo = new double[num_adv_win][num_mag_bin];
			double[][] bayesian_gamma_hi = new double[num_adv_win][num_mag_bin];

			bayesian_log_like_set.single_event_gamma (gamma_config, bayesian_gamma_lo, bayesian_gamma_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "gamma_lo = " + bayesian_gamma_lo[i_adv_win][i_mag_bin] + ",  "
						+ "gamma_hi = " + bayesian_gamma_hi[i_adv_win][i_mag_bin]
					);
				}
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("LogLikeSet : Unrecognized subcommand : " + args[0]);
		return;
	}
}
