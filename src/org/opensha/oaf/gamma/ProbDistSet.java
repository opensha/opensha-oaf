package org.opensha.oaf.gamma;

import java.util.List;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.rj.RJ_AftershockModel;

import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastParameters;
import org.opensha.oaf.aafs.ForecastResults;

import org.opensha.oaf.util.SimpleUtils;


/**
 * A set of probability distributions for an aftershock forecast.
 * Author: Michael Barall 10/09/2018.
 *
 * This object holds a set of probability distributions for a single
 * earthquake, forecast lag, and aftershock model.  There is one distribution
 * for each combination of advisory window and magnitude bin.  Each
 * distribution is an array whose i-th element is the probability of there
 * being i aftershocks in the corresponding advisory window and magnitude bin.
 */
public class ProbDistSet {

	//----- Data -----

	// Array of probability distributions.
	// Dimension of the array is
	//  prob_dist[adv_window_count][adv_min_mag_bin_count][max_as]
	// where max_as is the maximum number of aftershocks with significantly
	// non-zero probability of occurring.  Note that max_as can be
	// different in each advisory window and magnitude bin.
	
	private double[][][] prob_dist;

	// The forecast lag, in milliseconds.

	private long forecast_lag;

	// Array of cumulative probability distributions.
	// Dimension of the array is
	//  cum_dist[adv_window_count][adv_min_mag_bin_count][max_as]
	// where max_as is the maximum number of aftershocks with significantly
	// non-zero probability of occurring.  Note that max_as can be
	// different in each advisory window and magnitude bin.
	// The value of cum_dist[i_adv_win][i_mag_bin][i_as] is the probability
	// of finding i_as or fewer events in the advisory window and magnitude bin.
	// This is set to null in the constructor, and built if needed.
	
	private double[][][] cum_dist;





	//----- Construction -----

	// Constructor.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds.
	//  model = RJ aftershock model, including transient data.

	public ProbDistSet (GammaConfig gamma_config, long the_forecast_lag, RJ_AftershockModel model) {

		// Save forecast lag.

		forecast_lag = the_forecast_lag;

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Get the probability distributions from the model

		prob_dist = new double[num_adv_win][][];
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			prob_dist[i_adv_win] = new double[num_mag_bin][];
			double tMinDays = ((double)(Math.max (gamma_config.sim_start_off, forecast_lag + gamma_config.adv_window_start_offs[i_adv_win]))) / ComcatOAFAccessor.day_millis;
			double tMaxDays = ((double)(forecast_lag + gamma_config.adv_window_end_offs[i_adv_win])) / ComcatOAFAccessor.day_millis;
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				double mag = gamma_config.adv_min_mag_bins[i_mag_bin];
				prob_dist[i_adv_win][i_mag_bin] = model.getDistFuncWithAleatory(mag, tMinDays, tMaxDays);
			}
		}

		// No cumulative distribution yet

		cum_dist = null;
	}




	// Build the cumulative distribution.

	private void build_cum_dist (GammaConfig gamma_config) {

		// If the cumulative distribution is not created yet ...

		if (cum_dist == null) {

			// Number of advisory windows and magnitude bins

			int num_adv_win = gamma_config.adv_window_count;
			int num_mag_bin = gamma_config.adv_min_mag_bin_count;

			// Allocate and fill in the cumulative distribution

			cum_dist = new double[num_adv_win][][];
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				cum_dist[i_adv_win] = new double[num_mag_bin][];
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					int max_as = prob_dist[i_adv_win][i_mag_bin].length;
					cum_dist[i_adv_win][i_mag_bin] = new double[max_as];
					double p = 0.0;
					for (int i_as = 0; i_as < max_as; ++i_as) {
						p += prob_dist[i_adv_win][i_mag_bin][i_as];
						if (p > 1.0) {
							p = 1.0;	// can only happen due to rounding errors
						}
						cum_dist[i_adv_win][i_mag_bin][i_as] = p;
					}
				}
			}
		}

		return;
	}





	//----- Querying -----

	// Count the number of events in each bin in an aftershock sequence.
	// Parameters:
	//  gamma_config = Configuration information.
	//  aftershocks = Sequence of aftershocks.
	//  origin_time = Origin time of the aftershock sequence, in milliseconds.
	// For an observed sequence, origin_time should be the rupture time.
	// For a simulated sequence, origin_time should be zero.
	// In the sequence of aftershocks, only the rupture time and magnitude are used.
	// The return value is an array dimensioned as
	//  bin_count[adv_window_count][adv_min_mag_bin_count].
	// Each element is the number of aftershocks in the corresponding advisory window
	// and magnitude bin.

	public int[][] count_bins (GammaConfig gamma_config, List<ObsEqkRupture> aftershocks, long origin_time) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Initialize the counters to zero

		int[][] bin_count = new int[num_adv_win][];
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			bin_count[i_adv_win] = new int[num_mag_bin];
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				bin_count[i_adv_win][i_mag_bin] = 0;
			}
		}

		// Calculate the time interval for each advisory window

		long[] time_lo = new long[num_adv_win];
		long[] time_hi = new long[num_adv_win];
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			time_lo[i_adv_win] = origin_time + forecast_lag + gamma_config.adv_window_start_offs[i_adv_win];
			time_hi[i_adv_win] = origin_time + forecast_lag + gamma_config.adv_window_end_offs[i_adv_win];
		}

		// Scan the list of aftershocks and count the number falling in each bin

		for (ObsEqkRupture rup : aftershocks) {
			long rup_time = rup.getOriginTime();
			double rup_mag = rup.getMag();
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				if (rup_time >= time_lo[i_adv_win] && rup_time <= time_hi[i_adv_win]) {
					for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
						if (rup_mag >= gamma_config.adv_min_mag_bins[i_mag_bin]) {
							bin_count[i_adv_win][i_mag_bin] = bin_count[i_adv_win][i_mag_bin] + 1;
						}
					}
				}
			}
		}

		return bin_count;
	}




	// Compute the log-likelihoods for an aftershock sequence.
	// Parameters:
	//  gamma_config = Configuration information.
	//  bin_count = Number of aftershocks in each bin, as returned from count_bins().
	// For an observed sequence, origin_time should be the rupture time.
	// For a simulated sequence, origin_time should be zero.
	// In the sequence of aftershocks, only the rupture time and magnitude are used.
	// The return value is an array dimensioned as
	//  log_like[adv_window_count][adv_min_mag_bin_count].
	// Each element is the log-likelihood for the corresponding advisory window and
	// magnitude bin (i.e., the natural log of the probability for the number of
	// aftershocks contained in the bin).

	public double[][] compute_log_like (GammaConfig gamma_config, int[][] bin_count) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute the log-likelihoods

		double[][] log_like = new double[num_adv_win][];
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			log_like[i_adv_win] = new double[num_mag_bin];
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				int num_as = bin_count[i_adv_win][i_mag_bin];
				if (num_as < prob_dist[i_adv_win][i_mag_bin].length) {
					log_like[i_adv_win][i_mag_bin] = Math.log (Math.max (prob_dist[i_adv_win][i_mag_bin][num_as], Double.MIN_NORMAL));
				} else {
					log_like[i_adv_win][i_mag_bin] = Math.log (Double.MIN_NORMAL);
				}
			}
		}

		return log_like;
	}




	// Compute the log-likelihoods for an aftershock sequence.
	// Parameters:
	//  gamma_config = Configuration information.
	//  aftershocks = Sequence of aftershocks.
	//  origin_time = Origin time of the aftershock sequence, in milliseconds.
	// For an observed sequence, origin_time should be the rupture time.
	// For a simulated sequence, origin_time should be zero.
	// In the sequence of aftershocks, only the rupture time and magnitude are used.
	// The return value is an array dimensioned as
	//  log_like[adv_window_count][adv_min_mag_bin_count].
	// Each element is the log-likelihood for the corresponding advisory window and
	// magnitude bin (i.e., the natural log of the probability for the number of
	// aftershocks contained in the bin).

	public double[][] compute_log_like (GammaConfig gamma_config, List<ObsEqkRupture> aftershocks, long origin_time) {

		// Count the number of aftershocks in each bin

		int[][] bin_count = count_bins (gamma_config, aftershocks, origin_time);

		// Compute the log-likelihoods

		return compute_log_like (gamma_config, bin_count);
	}




	// Compute probability distribution statistics.
	// Parameters:
	//  gamma_config = Configuration information.
	//  mean_prob = Array to receive mean, dimension mean_prob[adv_window_count][adv_min_mag_bin_count].
	//  median_prob = Array to receive median, dimension median_prob[adv_window_count][adv_min_mag_bin_count].
	//  fractile_5_prob = Array to receive 5 percent fractile, dimension fractile_5_prob[adv_window_count][adv_min_mag_bin_count].
	//  fractile_95_prob = Array to receive 95 percent fractile, dimension fractile_95_prob[adv_window_count][adv_min_mag_bin_count].

	public void compute_prob_stats (GammaConfig gamma_config, double[][] mean_prob,
		int[][] median_prob, int[][] fractile_5_prob, int[][] fractile_95_prob) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Loop over windows and magnitude bins, computing statistics for each

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				// Number of aftershock counts

				int num_as = prob_dist[i_adv_win][i_mag_bin].length;

				// Compute mean

				double sum = 0.0;
				double total = 0.0;

				for (int i_as = 0; i_as < num_as; ++i_as) {
					total += prob_dist[i_adv_win][i_mag_bin][i_as];
					sum += ((double)i_as) * prob_dist[i_adv_win][i_mag_bin][i_as];
				}

				mean_prob[i_adv_win][i_mag_bin] = sum / total;

				// Compute fractiles

				boolean want_5 = true;
				boolean want_50 = false;
				boolean want_95 = false;
				sum = 0.0;

				for (int i_as = 0; i_as < num_as; ++i_as) {
					sum += prob_dist[i_adv_win][i_mag_bin][i_as];
					if (want_5 && sum >= total*0.05) {
						fractile_5_prob[i_adv_win][i_mag_bin] = i_as;
						want_5 = false;
						want_50 = true;
					}
					if (want_50 && sum >= total*0.50) {
						median_prob[i_adv_win][i_mag_bin] = i_as;
						want_50 = false;
						want_95 = true;
					}
					if (want_95 && sum >= total*0.95) {
						fractile_95_prob[i_adv_win][i_mag_bin] = i_as;
						want_95 = false;
					}
				}
			}
		}

		return;
	}




	// Compute the R&J expected number of aftershocks in each interval for the maximum likelihood a/p/c.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds.
	//  model = RJ aftershock model, including transient data.
	//  mean_rj = Array to receive expected aftershocks, dimension mean_rj[adv_window_count][adv_min_mag_bin_count].

	public static void compute_rj_means (GammaConfig gamma_config, long the_forecast_lag, RJ_AftershockModel model, double[][] mean_rj) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Get the probability distributions from the model

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			double tMinDays = ((double)(Math.max (gamma_config.sim_start_off, the_forecast_lag + gamma_config.adv_window_start_offs[i_adv_win]))) / ComcatOAFAccessor.day_millis;
			double tMaxDays = ((double)(the_forecast_lag + gamma_config.adv_window_end_offs[i_adv_win])) / ComcatOAFAccessor.day_millis;
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				double mag = gamma_config.adv_min_mag_bins[i_mag_bin];
				mean_rj[i_adv_win][i_mag_bin] = model.getModalNumEvents(mag, tMinDays, tMaxDays);
			}
		}
	}




	// Compute cumulative probability distribution.
	// Parameters:
	//  gamma_config = Configuration information.
	//  bin_count = Number of aftershocks in each bin, as returned from count_bins().
	//  zeta_lo = Array to receive lo cumulative probability, dimension zeta_lo[adv_window_count][adv_min_mag_bin_count].
	//  zeta_hi = Array to receive hi cumulative probability, dimension zeta_hi[adv_window_count][adv_min_mag_bin_count].
	// Note: zeta_lo contains the cumulative probability that the number of aftershocks
	// is less than the value in bin_count, and zeta_hi contains the cumulative probability
	// that the number of aftershocks is less than or equal to the value in bin_count.

	public void compute_cum_prob (GammaConfig gamma_config, int[][] bin_count,
		double[][] zeta_lo, double[][] zeta_hi) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Build the cumulative distribution if needed

		build_cum_dist (gamma_config);

		// Compute the cumulative probabilities

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				int num_as = bin_count[i_adv_win][i_mag_bin];
				if (num_as < prob_dist[i_adv_win][i_mag_bin].length) {
					zeta_hi[i_adv_win][i_mag_bin] = cum_dist[i_adv_win][i_mag_bin][num_as];
				} else {
					zeta_hi[i_adv_win][i_mag_bin] = 1.0;
				}
				if (num_as == 0) {
					zeta_lo[i_adv_win][i_mag_bin] = 0.0;
				} else if (num_as <= prob_dist[i_adv_win][i_mag_bin].length) {
					zeta_lo[i_adv_win][i_mag_bin] = cum_dist[i_adv_win][i_mag_bin][num_as - 1];
				} else {
					zeta_lo[i_adv_win][i_mag_bin] = 1.0;
				}
			}
		}

		return;
	}




	// Compute cumulative probability distribution.
	// Parameters:
	//  gamma_config = Configuration information.
	//  aftershocks = Sequence of aftershocks.
	//  origin_time = Origin time of the aftershock sequence, in milliseconds.
	//  zeta_lo = Array to receive low cumulative probability, dimension zeta_lo[adv_window_count][adv_min_mag_bin_count].
	//  zeta_hi = Array to receive high cumulative probability, dimension zeta_hi[adv_window_count][adv_min_mag_bin_count].
	// For an observed sequence, origin_time should be the rupture time.
	// For a simulated sequence, origin_time should be zero.
	// In the sequence of aftershocks, only the rupture time and magnitude are used.
	// Note: zeta_lo contains the cumulative probability that the number of aftershocks
	// is less than the value in bin_count, and zeta_hi contains the cumulative probability
	// that the number of aftershocks is less than or equal to the value in bin_count.

	public void compute_cum_prob (GammaConfig gamma_config, List<ObsEqkRupture> aftershocks, long origin_time,
		double[][] zeta_lo, double[][] zeta_hi) {

		// Count the number of aftershocks in each bin

		int[][] bin_count = count_bins (gamma_config, aftershocks, origin_time);

		// Compute the cumulative probabilities

		compute_cum_prob (gamma_config, bin_count, zeta_lo,  zeta_hi);
		return;
	}




	//----- Testing -----

	// Entry point.
	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ProbDistSet : Missing subcommand");
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
				System.err.println ("ProbDistSet : Invalid 'test1' subcommand");
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
				throw new RuntimeException ("ProbDistSet: Failed to compute aftershock models");
			}

			// Get catalog of all aftershocks

			List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			ProbDistSet generic_prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, results.generic_model);

			int[][] generic_bin_count = generic_prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
			//double[][] generic_log_like = generic_prob_dist_set.compute_log_like (gamma_config, all_aftershocks, fcmain.mainshock_time);
			double[][] generic_log_like = generic_prob_dist_set.compute_log_like (gamma_config, generic_bin_count);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "count = " + generic_bin_count[i_adv_win][i_mag_bin] + ",  "
						+ "log_like = " + generic_log_like[i_adv_win][i_mag_bin]
					);
				}
			}

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			ProbDistSet seq_spec_prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, results.seq_spec_model);

			int[][] seq_spec_bin_count = seq_spec_prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
			//double[][] seq_spec_log_like = seq_spec_prob_dist_set.compute_log_like (gamma_config, all_aftershocks, fcmain.mainshock_time);
			double[][] seq_spec_log_like = seq_spec_prob_dist_set.compute_log_like (gamma_config, seq_spec_bin_count);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "count = " + seq_spec_bin_count[i_adv_win][i_mag_bin] + ",  "
						+ "log_like = " + seq_spec_log_like[i_adv_win][i_mag_bin]
					);
				}
			}

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			ProbDistSet bayesian_prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, results.bayesian_model);

			int[][] bayesian_bin_count = bayesian_prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
			//double[][] bayesian_log_like = bayesian_prob_dist_set.compute_log_like (gamma_config, all_aftershocks, fcmain.mainshock_time);
			double[][] bayesian_log_like = bayesian_prob_dist_set.compute_log_like (gamma_config, bayesian_bin_count);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "count = " + bayesian_bin_count[i_adv_win][i_mag_bin] + ",  "
						+ "log_like = " + bayesian_log_like[i_adv_win][i_mag_bin]
					);
				}
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test1  event_id  forecast_lag
		// Compute model for the given event at the given forecast lag.
		// The forecast_lag is given in java.time.Duration format.
		// Same as test #1 except also display the cumulative probabilities.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("ProbDistSet : Invalid 'test2' subcommand");
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
				throw new RuntimeException ("ProbDistSet: Failed to compute aftershock models");
			}

			// Get catalog of all aftershocks

			List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			ProbDistSet generic_prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, results.generic_model);

			int[][] generic_bin_count = generic_prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
			//double[][] generic_log_like = generic_prob_dist_set.compute_log_like (gamma_config, all_aftershocks, fcmain.mainshock_time);
			double[][] generic_log_like = generic_prob_dist_set.compute_log_like (gamma_config, generic_bin_count);

			double[][] generic_zeta_lo = new double[num_adv_win][num_mag_bin];
			double[][] generic_zeta_hi = new double[num_adv_win][num_mag_bin];
			generic_prob_dist_set.compute_cum_prob (gamma_config, generic_bin_count, generic_zeta_lo, generic_zeta_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "count = " + generic_bin_count[i_adv_win][i_mag_bin] + ",  "
						+ "log_like = " + generic_log_like[i_adv_win][i_mag_bin] + ",  "
						+ "zeta_lo = " + generic_zeta_lo[i_adv_win][i_mag_bin] + ",  "
						+ "zeta_hi = " + generic_zeta_hi[i_adv_win][i_mag_bin]
					);
				}
			}

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			ProbDistSet seq_spec_prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, results.seq_spec_model);

			int[][] seq_spec_bin_count = seq_spec_prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
			//double[][] seq_spec_log_like = seq_spec_prob_dist_set.compute_log_like (gamma_config, all_aftershocks, fcmain.mainshock_time);
			double[][] seq_spec_log_like = seq_spec_prob_dist_set.compute_log_like (gamma_config, seq_spec_bin_count);

			double[][] seq_spec_zeta_lo = new double[num_adv_win][num_mag_bin];
			double[][] seq_spec_zeta_hi = new double[num_adv_win][num_mag_bin];
			seq_spec_prob_dist_set.compute_cum_prob (gamma_config, seq_spec_bin_count, seq_spec_zeta_lo, seq_spec_zeta_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "count = " + seq_spec_bin_count[i_adv_win][i_mag_bin] + ",  "
						+ "log_like = " + seq_spec_log_like[i_adv_win][i_mag_bin] + ",  "
						+ "zeta_lo = " + seq_spec_zeta_lo[i_adv_win][i_mag_bin] + ",  "
						+ "zeta_hi = " + seq_spec_zeta_hi[i_adv_win][i_mag_bin]
					);
				}
			}

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			ProbDistSet bayesian_prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, results.bayesian_model);

			int[][] bayesian_bin_count = bayesian_prob_dist_set.count_bins (gamma_config, all_aftershocks, fcmain.mainshock_time);
			//double[][] bayesian_log_like = bayesian_prob_dist_set.compute_log_like (gamma_config, all_aftershocks, fcmain.mainshock_time);
			double[][] bayesian_log_like = bayesian_prob_dist_set.compute_log_like (gamma_config, bayesian_bin_count);

			double[][] bayesian_zeta_lo = new double[num_adv_win][num_mag_bin];
			double[][] bayesian_zeta_hi = new double[num_adv_win][num_mag_bin];
			bayesian_prob_dist_set.compute_cum_prob (gamma_config, bayesian_bin_count, bayesian_zeta_lo, bayesian_zeta_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "count = " + bayesian_bin_count[i_adv_win][i_mag_bin] + ",  "
						+ "log_like = " + bayesian_log_like[i_adv_win][i_mag_bin] + ",  "
						+ "zeta_lo = " + bayesian_zeta_lo[i_adv_win][i_mag_bin] + ",  "
						+ "zeta_hi = " + bayesian_zeta_hi[i_adv_win][i_mag_bin]
					);
				}
			}

			return;
		}

		


		// Unrecognized subcommand.

		System.err.println ("ProbDistSet : Unrecognized subcommand : " + args[0]);
		return;
	}
}
