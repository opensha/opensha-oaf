package org.opensha.oaf.aftershockStatistics.gamma;

import java.util.List;
import java.util.Arrays;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.aftershockStatistics.comcat.ComcatOAFAccessor;
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
 * A set of log-likelihoods for an aftershock forecast.
 * Author: Michael Barall 10/09/2018.
 *
 * This object holds a set of log-likelihoods and event counts for a single
 * earthquake, forecast lag, and aftershock model. For each combinarion of
 * advisory window and magnitude bin, there is an array of log-likelihoods
 * and event counts for a series of simulations.  There are also log-likelihoods
 * and event counts for the observed aftershock sequence.
 *
 * Given an observation O, the likelihood is defined to be P(O), which is the
 * probability that O occurs assuming that the model is correct.  The
 * log-likelihood is of course log(P(O)).
 *
 * The gamma statistic is defined to be the cumulative probability of the
 * log-likelihood log(P(O)).  That is, gamma is sum(P(O')), where the sum runs
 * over all possible observations O' such that P(O') < P(O), or P(O') <= P(O).
 * We refer to the former case as gamma_lo, and the latter case as gamma_hi.
 * The two cases are different because our probability distributions are discrete.
 *
 * If O is the number of aftershocks that have been observed in some window,
 * then the zeta statistic is defined to be the cumulative probability of O.
 * That is, zeta is sum(P(O')), where the sum runs over all possible observations
 * O' such that O' < O, or O' <= O.  We refer to the former case as zeta_lo,
 * and the latter case as zeta_hi.
 */
public class LogLikeSet {

	//----- Data -----

	// The forecast lag, in milliseconds.

	private long forecast_lag;

	// Number of simulations.

	private int num_sim;

	// True if explicitly set to zero.

	private boolean is_zero;

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
		is_zero = false;
		sim_log_like = null;
		sim_event_count = null;
		obs_log_like = null;
		obs_event_count = null;
	}




	// Run simulations.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds.
	//  the_num_sim = The number of simulations to run.
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
			//double tMinDays = ((double)(forecast_lag)) / ComcatOAFAccessor.day_millis;
			//double tMaxDays = ((double)(forecast_lag + gamma_config.max_adv_window_end_off)) / ComcatOAFAccessor.day_millis;
			double tMinDays = ((double)(gamma_config.sim_start_off)) / ComcatOAFAccessor.day_millis;
			double tMaxDays = ((double)(gamma_config.max_forecast_lag + gamma_config.max_adv_window_end_off)) / ComcatOAFAccessor.day_millis;

			// Run the simulation

			ObsEqkRupList sim_aftershocks = AftershockStatsCalc.simAftershockSequence (
						a, b, magMain, magCat, capG, capH, p, c, tMinDays, tMaxDays, 0L, gamma_config.rangen);

			// Find the maximum magnitude

			double max_mag = -1000.0;
			for (ObsEqkRupture rup : sim_aftershocks) {
				double mag = rup.getMag();
				if (max_mag < mag) {
					max_mag = mag;
				}
			}

			// If the simulation has an aftershock larger than the mainshock, discard it

			if (gamma_config.discard_sim_with_large_as && max_mag > magMain) {
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




	// Allocate and zero-initialize all arrays.
	// Parameters:
	//  gamma_config = Configuration information.
	//  the_forecast_lag = Forecast lag, in milliseconds, can be -1L for sums over forecast lags.
	//  the_num_sim = The number of simulations to run.

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

		sim_log_like = new double[num_adv_win][num_mag_bin][num_sim];
		sim_event_count = new int[num_adv_win][num_mag_bin][num_sim];

		obs_log_like = new double[num_adv_win][num_mag_bin];
		obs_event_count = new int[num_adv_win][num_mag_bin];

		// Zero-initialize

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				obs_event_count[i_adv_win][i_mag_bin] = 0;
				obs_log_like[i_adv_win][i_mag_bin] = 0.0;
				for (int i_sim = 0; i_sim < num_sim; ++i_sim) {
					sim_event_count[i_adv_win][i_mag_bin][i_sim] = 0;
					sim_log_like[i_adv_win][i_mag_bin][i_sim] = 0.0;
				}
			}
		}

		return;
	}




	// Add array contents from another object into this object.
	// Parameters:
	//  gamma_config = Configuration information.
	//  other = The other object.
	//  randomize = True to select simulations from the other object randomly.

	public void add_from (GammaConfig gamma_config, LogLikeSet other, boolean randomize) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Adjust zero flag

		if (other.is_zero) {
			return;
		}
		is_zero = false;

		// Add observed data

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				obs_event_count[i_adv_win][i_mag_bin] += other.obs_event_count[i_adv_win][i_mag_bin];
				obs_log_like[i_adv_win][i_mag_bin] += other.obs_log_like[i_adv_win][i_mag_bin];
			}
		}

		// Add simulated data

		for (int i_sim = 0; i_sim < num_sim; ++i_sim) {

			// Select simulation from other object

			int o_sim = i_sim % other.num_sim;
			if (randomize) {
				o_sim = (int)(gamma_config.rangen.sample() * ((double)other.num_sim));
				if (o_sim < 0) {
					o_sim = 0;
				}
				else if (o_sim >= other.num_sim) {
					o_sim = other.num_sim - 1;
				}
			}

			// Add the data

			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					sim_event_count[i_adv_win][i_mag_bin][i_sim] += other.sim_event_count[i_adv_win][i_mag_bin][o_sim];
					sim_log_like[i_adv_win][i_mag_bin][i_sim] += other.sim_log_like[i_adv_win][i_mag_bin][o_sim];
				}
			}
		}

		return;
	}




	//----- Querying -----

	// Compute the single-event gamma.
	// Parameters:
	//  gamma_config = Configuration information.
	//  gamma_lo = Array to receive low value of gamma, dimension gamma_lo[adv_window_count + 1][adv_min_mag_bin_count].
	//  gamma_hi = Array to receive high value of gamma, dimension gamma_hi[adv_window_count + 1][adv_min_mag_bin_count].
	// The extra advisory window slot is used to report the sum over all windows.

	public void single_event_gamma (GammaConfig gamma_config, double[][] gamma_lo, double[][] gamma_hi) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Loop over windows and magnitude bins, computing gamma for each

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

		// Loop over magnitude bins, computing gamma for the sum over prospective windows

		for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

			// Accumulate number of simulations with log-likelihood below and above observation

			double total_obs_log_like = 0.0;
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				if (gamma_config.adv_window_start_offs[i_adv_win] >= 0L) {
					total_obs_log_like += obs_log_like[i_adv_win][i_mag_bin];
				}
			}

			int below = 0;
			int above = 0;
			for (int i_sim = 0; i_sim < num_sim; ++i_sim) {

				double total_sim_log_like = 0.0;
				for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
					if (gamma_config.adv_window_start_offs[i_adv_win] >= 0L) {
						total_sim_log_like += sim_log_like[i_adv_win][i_mag_bin][i_sim];
					}
				}

				if (total_sim_log_like < total_obs_log_like) {
					++below;
				}
				else if (total_sim_log_like > total_obs_log_like) {
					++above;
				}
			}

			// Compute gammas

			gamma_lo[num_adv_win][i_mag_bin] = ((double)below) / ((double)num_sim);
			gamma_hi[num_adv_win][i_mag_bin] = ((double)(num_sim - above)) / ((double)num_sim);
		}

		return;
	}




	// Compute the single-event zeta.
	// Parameters:
	//  gamma_config = Configuration information.
	//  zeta_lo = Array to receive low value of zeta, dimension zeta_lo[adv_window_count + 1][adv_min_mag_bin_count].
	//  zeta_hi = Array to receive high value of zeta, dimension zeta_hi[adv_window_count + 1][adv_min_mag_bin_count].
	// The extra advisory window slot is used to report the sum over all windows.
	// If the data was explicitly set to zero, then zeta_lo and zeta_hi are set to -1.0 to indicate no data.

	public void single_event_zeta (GammaConfig gamma_config, double[][] zeta_lo, double[][] zeta_hi) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Handle case of no data

		if (is_zero) {
			for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					zeta_lo[i_adv_win][i_mag_bin] = -1.0;
					zeta_hi[i_adv_win][i_mag_bin] = -1.0;
				}
			}
			return;
		}

		// Loop over windows and magnitude bins, computing zeta for each

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				// Accumulate number of simulations with event count below and above observation

				int below = 0;
				int above = 0;
				for (int i_sim = 0; i_sim < num_sim; ++i_sim) {
					if (sim_event_count[i_adv_win][i_mag_bin][i_sim] < obs_event_count[i_adv_win][i_mag_bin]) {
						++below;
					}
					else if (sim_event_count[i_adv_win][i_mag_bin][i_sim] > obs_event_count[i_adv_win][i_mag_bin]) {
						++above;
					}
				}

				// Compute zetas

				zeta_lo[i_adv_win][i_mag_bin] = ((double)below) / ((double)num_sim);
				zeta_hi[i_adv_win][i_mag_bin] = ((double)(num_sim - above)) / ((double)num_sim);
			}
		}

		// Loop over magnitude bins, computing zeta for the sum over prospective windows

		for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

			// Accumulate number of simulations with event count below and above observation

			int total_obs_event_count = 0;
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				if (gamma_config.adv_window_start_offs[i_adv_win] >= 0L) {
					total_obs_event_count += obs_event_count[i_adv_win][i_mag_bin];
				}
			}

			int below = 0;
			int above = 0;
			for (int i_sim = 0; i_sim < num_sim; ++i_sim) {

				int total_sim_event_count = 0;
				for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
					if (gamma_config.adv_window_start_offs[i_adv_win] >= 0L) {
						total_sim_event_count += sim_event_count[i_adv_win][i_mag_bin][i_sim];
					}
				}

				if (total_sim_event_count < total_obs_event_count) {
					++below;
				}
				else if (total_sim_event_count > total_obs_event_count) {
					++above;
				}
			}

			// Compute zetas

			zeta_lo[num_adv_win][i_mag_bin] = ((double)below) / ((double)num_sim);
			zeta_hi[num_adv_win][i_mag_bin] = ((double)(num_sim - above)) / ((double)num_sim);
		}

		return;
	}




	// Compute event count statistics.
	// Parameters:
	//  gamma_config = Configuration information.
	//  obs_count = Array to receive observed count, dimension obs_count[adv_window_count][adv_min_mag_bin_count].
	//  sim_median_count = Array to receive simulated median count, dimension sim_median_count[adv_window_count][adv_min_mag_bin_count].
	//  sim_fractile_5_count = Array to receive simulated 5 percent fractile count, dimension sim_fractile_5_count[adv_window_count][adv_min_mag_bin_count].
	//  sim_fractile_95_count = Array to receive simulated 95 percent fractile count, dimension sim_fractile_95_count[adv_window_count][adv_min_mag_bin_count].

	public void compute_count_stats (GammaConfig gamma_config, int[][] obs_count,
		int[][] sim_median_count, int[][] sim_fractile_5_count, int[][] sim_fractile_95_count) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Indexes for median and fractiles

		int i_median = num_sim / 2;
		int i_fractile_5 = (num_sim * 5 + 50) / 100;
		int i_fractile_95 = (num_sim * 95 + 50) / 100;

		// Loop over windows and magnitude bins, computing statistics for each

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				// Observed count

				obs_count[i_adv_win][i_mag_bin] = obs_event_count[i_adv_win][i_mag_bin];

				// Sort the array of simulated counts

				int[] sorted_sim_count = Arrays.copyOf (sim_event_count[i_adv_win][i_mag_bin], num_sim);
				Arrays.sort (sorted_sim_count);

				// Return median and fractiles

				sim_median_count[i_adv_win][i_mag_bin] = sorted_sim_count[i_median];
				sim_fractile_5_count[i_adv_win][i_mag_bin] = sorted_sim_count[i_fractile_5];
				sim_fractile_95_count[i_adv_win][i_mag_bin] = sorted_sim_count[i_fractile_95];
			}
		}

		return;
	}




	// Compute event count statistics.
	// Parameters:
	//  gamma_config = Configuration information.
	//  obs_count = Array to receive observed count, dimension obs_count[adv_window_count + 1][adv_min_mag_bin_count].
	// The extra advisory window slot is used to report the sum over all windows.
	// Note the dimension of obs_count is different than in the overloaded function above.

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

		// Loop over magnitude bins, computing statistics for the sum over prospective windows

		for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

			// Accumulate observed counts

			int total_obs_event_count = 0;
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				if (gamma_config.adv_window_start_offs[i_adv_win] >= 0L) {
					total_obs_event_count += obs_event_count[i_adv_win][i_mag_bin];
				}
			}

			obs_count[num_adv_win][i_mag_bin] = total_obs_event_count;
		}

		return;
	}




	// Compute event count means.
	// Parameters:
	//  gamma_config = Configuration information.
	//  sim_mean_count = Array to receive simulated mean count, dimension sim_mean_count[adv_window_count][adv_min_mag_bin_count].

	public void compute_count_means (GammaConfig gamma_config, double[][] sim_mean_count) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Loop over windows and magnitude bins, computing means for each

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {

				double sum = 0.0;
				for (int i_sim = 0; i_sim < num_sim; ++i_sim) {
					sum += (double)(sim_event_count[i_adv_win][i_mag_bin][i_sim]);
				}

				sim_mean_count[i_adv_win][i_mag_bin] = sum / ((double)num_sim);
			}
		}

		return;
	}




	// Compute the single-event gamma, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.

	public String single_event_gamma_to_string (GammaConfig gamma_config) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		double[][] gamma_lo = new double[num_adv_win + 1][num_mag_bin];
		double[][] gamma_hi = new double[num_adv_win + 1][num_mag_bin];

		single_event_gamma (gamma_config, gamma_lo, gamma_hi);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				sb.append (
					gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
					+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
					+ "gamma_lo = " + String.format ("%.6f", gamma_lo[i_adv_win][i_mag_bin]) + ",  "
					+ "gamma_hi = " + String.format ("%.6f", gamma_hi[i_adv_win][i_mag_bin]) + "\n"
				);
			}
		}

		return sb.toString();
	}




	// Compute the single-event zeta, and convert the table to a string.
	// Parameters:
	//  gamma_config = Configuration information.

	public String single_event_zeta_to_string (GammaConfig gamma_config) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute results

		double[][] zeta_lo = new double[num_adv_win + 1][num_mag_bin];
		double[][] zeta_hi = new double[num_adv_win + 1][num_mag_bin];

		single_event_zeta (gamma_config, zeta_lo, zeta_hi);

		// Convert the table

		StringBuilder sb = new StringBuilder();
		for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
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
		int[][] generic_sim_median_count = new int[num_adv_win][num_mag_bin];
		int[][] generic_sim_fractile_5_count = new int[num_adv_win][num_mag_bin];
		int[][] generic_sim_fractile_95_count = new int[num_adv_win][num_mag_bin];

		compute_count_stats (gamma_config, generic_obs_count,
			generic_sim_median_count, generic_sim_fractile_5_count, generic_sim_fractile_95_count);

		// Convert to string

		StringBuilder sb = new StringBuilder();
		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				sb.append (
					gamma_config.adv_window_names[i_adv_win] + ",  "
					+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
					+ "obs = " + generic_obs_count[i_adv_win][i_mag_bin] + ",  "
					+ "median = " + generic_sim_median_count[i_adv_win][i_mag_bin] + ",  "
					+ "fractile_5 = " + generic_sim_fractile_5_count[i_adv_win][i_mag_bin] + ",  "
					+ "fractile_95 = " + generic_sim_fractile_95_count[i_adv_win][i_mag_bin] + "\n"
				);
			}
		}

		return sb.toString();
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
		writer.marshalBoolean       ("is_zero"        , is_zero        );
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
		is_zero         = reader.unmarshalBoolean       ("is_zero"        );
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

	// Marshal an array of objects, polymorphic.

	public static void marshal_array_poly (MarshalWriter writer, String name, LogLikeSet[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshal_poly (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Marshal a 2D array of objects, polymorphic.

	public static void marshal_2d_array_poly (MarshalWriter writer, String name, LogLikeSet[][] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshal_array_poly (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects, polymorphic.

	public static LogLikeSet[] unmarshal_array_poly (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		LogLikeSet[] x = new LogLikeSet[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshal_poly (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}

	// Unmarshal a 2d array of objects, polymorphic.

	public static LogLikeSet[][] unmarshal_2d_array_poly (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		LogLikeSet[][] x = new LogLikeSet[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshal_array_poly (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	// Test routine, to run simulations for a model, and compare statistics to probability distributions.
	// Parameters are the same as run_simulations.

	private static void compare_sim_to_prob (GammaConfig gamma_config, long the_forecast_lag, int the_num_sim,
		ForecastMainshock fcmain, RJ_AftershockModel model, List<ObsEqkRupture> all_aftershocks, boolean verbose) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Run simulations and collect statistics

		LogLikeSet log_like_set = new LogLikeSet();
		log_like_set.run_simulations (gamma_config, the_forecast_lag, the_num_sim,
			fcmain, model, all_aftershocks, verbose);

		int[][] obs_count = new int[num_adv_win][num_mag_bin];
		int[][] sim_median_count = new int[num_adv_win][num_mag_bin];
		int[][] sim_fractile_5_count = new int[num_adv_win][num_mag_bin];
		int[][] sim_fractile_95_count = new int[num_adv_win][num_mag_bin];

		log_like_set.compute_count_stats (gamma_config, obs_count,
			sim_median_count, sim_fractile_5_count, sim_fractile_95_count);

		double[][] sim_mean_count = new double[num_adv_win + 1][num_mag_bin];

		log_like_set.compute_count_means (gamma_config, sim_mean_count);

		// Compute probability distribution and collect its statistics

		ProbDistSet prob_dist_set = new ProbDistSet (gamma_config, the_forecast_lag, model);

		double[][] mean_prob = new double[num_adv_win][num_mag_bin];
		int[][] median_prob = new int[num_adv_win][num_mag_bin];
		int[][] fractile_5_prob = new int[num_adv_win][num_mag_bin];
		int[][] fractile_95_prob = new int[num_adv_win][num_mag_bin];

		prob_dist_set.compute_prob_stats (gamma_config, mean_prob,
			median_prob, fractile_5_prob, fractile_95_prob);

		double[][] mean_rj = new double[num_adv_win][num_mag_bin];

		ProbDistSet.compute_rj_means (gamma_config, the_forecast_lag, model, mean_rj);

		// Display results

		for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				System.out.println (
					gamma_config.adv_window_names[i_adv_win] + ",  "
					+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
					+ "obs = " + obs_count[i_adv_win][i_mag_bin] + ",  "
					+ "s50 = " + sim_median_count[i_adv_win][i_mag_bin] + ",  "
					+ "s05 = " + sim_fractile_5_count[i_adv_win][i_mag_bin] + ",  "
					+ "s95 = " + sim_fractile_95_count[i_adv_win][i_mag_bin] + ",  "
					+ "sm = " + String.format ("%.2f", sim_mean_count[i_adv_win][i_mag_bin]) + ",  "
					+ "p50 = " + median_prob[i_adv_win][i_mag_bin] + ",  "
					+ "p05 = " + fractile_5_prob[i_adv_win][i_mag_bin] + ",  "
					+ "p95 = " + fractile_95_prob[i_adv_win][i_mag_bin] + ",  "
					+ "pm = " + String.format ("%.2f", mean_prob[i_adv_win][i_mag_bin]) + ",  "
					+ "rjm = " + String.format ("%.2f", mean_rj[i_adv_win][i_mag_bin])
				);
			}
		}
		
		return;
	}




	// Test routine, to test the selection of a/p/c for epistemic uncertainty.

	private static void test_epi_selection (GammaConfig gamma_config, int the_num_sim,
		RJ_AftershockModel model) {

		// Number of a/p/c values

		int num_a = model.getNum_a();
		int num_p = model.getNum_p();
		int num_c = model.getNum_c();

		// Allocate the count matrix

		int[][][] count = new int[num_a][num_p][num_c];

		for (int aIndex = 0; aIndex < num_a; aIndex++) {
			for (int pIndex = 0; pIndex < num_p; pIndex++) {
				for (int cIndex = 0; cIndex < num_c; cIndex++) {
					count[aIndex][pIndex][cIndex] = 0;
				}
			}
		}

		// Take the samples

		double[] apcval = new double[3];

		for (int i_sim = 0; i_sim < the_num_sim; ++i_sim) {
		
			// Take a sample

			model.sample_apc (gamma_config.rangen.sample(), apcval);

			// Convert to indexes

			int aIndex = model.get_aIndex_from_a (apcval[0]);
			int pIndex = model.get_pIndex_from_p (apcval[1]);
			int cIndex = model.get_cIndex_from_c (apcval[2]);

			// Increment counter, this will also check for indexes out-of-range

			count[aIndex][pIndex][cIndex] = count[aIndex][pIndex][cIndex] + 1;
		}

		// Display results

		for (int aIndex = 0; aIndex < num_a; aIndex++) {
			for (int pIndex = 0; pIndex < num_p; pIndex++) {
				for (int cIndex = 0; cIndex < num_c; cIndex++) {
					double expected = model.get_clipped_apc_prob (aIndex, pIndex, cIndex) * ((double)the_num_sim);
					System.out.println (
						"aIndex = " + aIndex + ",  "
						+ "pIndex = " + pIndex + ",  "
						+ "cIndex = " + cIndex + ",  "
						+ "count = " + count[aIndex][pIndex][cIndex] + ",  "
						+ "expected = " + String.format ("%.2f", expected)
					);
					count[aIndex][pIndex][cIndex] = 0;
				}
			}
		}
		
		return;
	}




	// Test routine, to compute and show cumulative probabilities for a model.
	// Parameters are the same as run_simulations.

	private static void compute_and_show_cum_prob (GammaConfig gamma_config, long the_forecast_lag, int the_num_sim,
		ForecastMainshock fcmain, RJ_AftershockModel model, List<ObsEqkRupture> all_aftershocks, boolean verbose) {

		// Number of advisory windows and magnitude bins

		int num_adv_win = gamma_config.adv_window_count;
		int num_mag_bin = gamma_config.adv_min_mag_bin_count;

		// Compute cumulative probabilities and collect results

		LogLikeSet log_like_set = new LogLikeSet();
		log_like_set.run_simulations (gamma_config, the_forecast_lag, the_num_sim,
			fcmain, model, all_aftershocks, verbose);

		double[][] zeta_lo = new double[num_adv_win + 1][num_mag_bin];
		double[][] zeta_hi = new double[num_adv_win + 1][num_mag_bin];

		log_like_set.single_event_zeta (gamma_config, zeta_lo, zeta_hi);

		// Display results

		for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
			for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
				System.out.println (
					gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
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

			List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			LogLikeSet generic_log_like_set = new LogLikeSet();
			generic_log_like_set.run_simulations (gamma_config, the_forecast_lag, gamma_config.simulation_count,
				fcmain, results.generic_model, all_aftershocks, false);

			double[][] generic_gamma_lo = new double[num_adv_win + 1][num_mag_bin];
			double[][] generic_gamma_hi = new double[num_adv_win + 1][num_mag_bin];

			generic_log_like_set.single_event_gamma (gamma_config, generic_gamma_lo, generic_gamma_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "gamma_lo = " + String.format ("%.6f", generic_gamma_lo[i_adv_win][i_mag_bin]) + ",  "
						+ "gamma_hi = " + String.format ("%.6f", generic_gamma_hi[i_adv_win][i_mag_bin])
					);
				}
			}

			int[][] generic_obs_count = new int[num_adv_win][num_mag_bin];
			int[][] generic_sim_median_count = new int[num_adv_win][num_mag_bin];
			int[][] generic_sim_fractile_5_count = new int[num_adv_win][num_mag_bin];
			int[][] generic_sim_fractile_95_count = new int[num_adv_win][num_mag_bin];

			generic_log_like_set.compute_count_stats (gamma_config, generic_obs_count,
				generic_sim_median_count, generic_sim_fractile_5_count, generic_sim_fractile_95_count);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "obs = " + generic_obs_count[i_adv_win][i_mag_bin] + ",  "
						+ "median = " + generic_sim_median_count[i_adv_win][i_mag_bin] + ",  "
						+ "fractile_5 = " + generic_sim_fractile_5_count[i_adv_win][i_mag_bin] + ",  "
						+ "fractile_95 = " + generic_sim_fractile_95_count[i_adv_win][i_mag_bin]
					);
				}
			}

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			LogLikeSet seq_spec_log_like_set = new LogLikeSet();
			seq_spec_log_like_set.run_simulations (gamma_config, the_forecast_lag, gamma_config.simulation_count,
				fcmain, results.seq_spec_model, all_aftershocks, false);

			double[][] seq_spec_gamma_lo = new double[num_adv_win + 1][num_mag_bin];
			double[][] seq_spec_gamma_hi = new double[num_adv_win + 1][num_mag_bin];

			seq_spec_log_like_set.single_event_gamma (gamma_config, seq_spec_gamma_lo, seq_spec_gamma_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "gamma_lo = " + String.format ("%.6f", seq_spec_gamma_lo[i_adv_win][i_mag_bin]) + ",  "
						+ "gamma_hi = " + String.format ("%.6f", seq_spec_gamma_hi[i_adv_win][i_mag_bin])
					);
				}
			}

			int[][] seq_spec_obs_count = new int[num_adv_win][num_mag_bin];
			int[][] seq_spec_sim_median_count = new int[num_adv_win][num_mag_bin];
			int[][] seq_spec_sim_fractile_5_count = new int[num_adv_win][num_mag_bin];
			int[][] seq_spec_sim_fractile_95_count = new int[num_adv_win][num_mag_bin];

			seq_spec_log_like_set.compute_count_stats (gamma_config, seq_spec_obs_count,
				seq_spec_sim_median_count, seq_spec_sim_fractile_5_count, seq_spec_sim_fractile_95_count);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "obs = " + seq_spec_obs_count[i_adv_win][i_mag_bin] + ",  "
						+ "median = " + seq_spec_sim_median_count[i_adv_win][i_mag_bin] + ",  "
						+ "fractile_5 = " + seq_spec_sim_fractile_5_count[i_adv_win][i_mag_bin] + ",  "
						+ "fractile_95 = " + seq_spec_sim_fractile_95_count[i_adv_win][i_mag_bin]
					);
				}
			}

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));

			LogLikeSet bayesian_log_like_set = new LogLikeSet();
			bayesian_log_like_set.run_simulations (gamma_config, the_forecast_lag, gamma_config.simulation_count,
				fcmain, results.bayesian_model, all_aftershocks, false);

			double[][] bayesian_gamma_lo = new double[num_adv_win + 1][num_mag_bin];
			double[][] bayesian_gamma_hi = new double[num_adv_win + 1][num_mag_bin];

			bayesian_log_like_set.single_event_gamma (gamma_config, bayesian_gamma_lo, bayesian_gamma_hi);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win <= num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.get_adv_window_name_or_sum(i_adv_win) + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "gamma_lo = " + String.format ("%.6f", bayesian_gamma_lo[i_adv_win][i_mag_bin]) + ",  "
						+ "gamma_hi = " + String.format ("%.6f", bayesian_gamma_hi[i_adv_win][i_mag_bin])
					);
				}
			}

			int[][] bayesian_obs_count = new int[num_adv_win][num_mag_bin];
			int[][] bayesian_sim_median_count = new int[num_adv_win][num_mag_bin];
			int[][] bayesian_sim_fractile_5_count = new int[num_adv_win][num_mag_bin];
			int[][] bayesian_sim_fractile_95_count = new int[num_adv_win][num_mag_bin];

			bayesian_log_like_set.compute_count_stats (gamma_config, bayesian_obs_count,
				bayesian_sim_median_count, bayesian_sim_fractile_5_count, bayesian_sim_fractile_95_count);

			System.out.println ("");
			for (int i_adv_win = 0; i_adv_win < num_adv_win; ++i_adv_win) {
				for (int i_mag_bin = 0; i_mag_bin < num_mag_bin; ++i_mag_bin) {
					System.out.println (
						gamma_config.adv_window_names[i_adv_win] + ",  "
						+ "mag = " + gamma_config.adv_min_mag_bins[i_mag_bin] + ",  "
						+ "obs = " + bayesian_obs_count[i_adv_win][i_mag_bin] + ",  "
						+ "median = " + bayesian_sim_median_count[i_adv_win][i_mag_bin] + ",  "
						+ "fractile_5 = " + bayesian_sim_fractile_5_count[i_adv_win][i_mag_bin] + ",  "
						+ "fractile_95 = " + bayesian_sim_fractile_95_count[i_adv_win][i_mag_bin]
					);
				}
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test1  event_id  forecast_lag  num_sim  discard_large_as
		// Compute model for the given event at the given forecast lag.
		// The forecast_lag is given in java.time.Duration format.
		// The boolean discard_large_as is true to discard simulations with an aftershock
		// larger than the mainshock (which is the default).
		// Run the requested number of simulations (overriding the number in GammaConfig)
		// and compare the simulated statistics with the R&J probability distributions.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Four additional arguments

			if (args.length != 5) {
				System.err.println ("LogLikeSet : Invalid 'test2' subcommand");
				return;
			}

			String the_event_id = args[1];
			long the_forecast_lag = SimpleUtils.string_to_duration (args[2]);
			int the_num_sim = Integer.parseInt (args[3]);
			boolean discard_large_as = Boolean.parseBoolean (args[4]);

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();
			gamma_config.discard_sim_with_large_as = discard_large_as;

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

			List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			compare_sim_to_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, results.generic_model, all_aftershocks, false);

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			compare_sim_to_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, results.seq_spec_model, all_aftershocks, false);

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			compare_sim_to_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, results.bayesian_model, all_aftershocks, false);

			// Zero-epistemic generic model with no epistemic uncertainty

			System.out.println ("");
			System.out.println ("Zero-epistemic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			double ze_magMain = results.generic_model.getMainShockMag();
			double ze_mean_a = results.generic_model.getMaxLikelihood_a();
			double ze_sigma_a = 1.0;
			double ze_min_a = results.generic_model.getMaxLikelihood_a();
			double ze_max_a = results.generic_model.getMaxLikelihood_a();
			double ze_delta_a = results.generic_model.getDelta_a();
			double ze_b = results.generic_model.get_b();
			double ze_p = results.generic_model.getMaxLikelihood_p();
			double ze_c = results.generic_model.getMaxLikelihood_c();

			RJ_AftershockModel_Generic ze_model = new RJ_AftershockModel_Generic (
				ze_magMain, ze_mean_a, ze_sigma_a, ze_min_a,ze_max_a, ze_delta_a, ze_b, ze_p, ze_c);

			compare_sim_to_prob (gamma_config, the_forecast_lag, the_num_sim,
				fcmain, ze_model, all_aftershocks, false);

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test1  event_id  forecast_lag  num_sim
		// Compute model for the given event at the given forecast lag.
		// The forecast_lag is given in java.time.Duration format.
		// Sample the epistemic probability distribution the requested number of times,
		// and display comparison to expected distribution.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Three additional arguments

			if (args.length != 4) {
				System.err.println ("LogLikeSet : Invalid 'test3' subcommand");
				return;
			}

			String the_event_id = args[1];
			long the_forecast_lag = SimpleUtils.string_to_duration (args[2]);
			int the_num_sim = Integer.parseInt (args[3]);

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

			List<ObsEqkRupture> all_aftershocks = GammaUtils.get_all_aftershocks (gamma_config, fcmain);

			System.out.println ("");
			System.out.println ("Total number of aftershocks = " + all_aftershocks.size());

			// Generic model

			System.out.println ("");
			System.out.println ("Generic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			test_epi_selection (gamma_config, the_num_sim, results.generic_model);

			// Sequence specific model

			System.out.println ("");
			System.out.println ("Sequence specific model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			test_epi_selection (gamma_config, the_num_sim, results.seq_spec_model);

			// Bayesian model

			System.out.println ("");
			System.out.println ("Bayesian model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			test_epi_selection (gamma_config, the_num_sim, results.bayesian_model);

			// Zero-epistemic generic model with no epistemic uncertainty

			System.out.println ("");
			System.out.println ("Zero-epistemic model, forecast_lag = " + SimpleUtils.duration_to_string_2 (the_forecast_lag));
			System.out.println ("");

			double ze_magMain = results.generic_model.getMainShockMag();
			double ze_mean_a = results.generic_model.getMaxLikelihood_a();
			double ze_sigma_a = 1.0;
			double ze_min_a = results.generic_model.getMaxLikelihood_a();
			double ze_max_a = results.generic_model.getMaxLikelihood_a();
			double ze_delta_a = results.generic_model.getDelta_a();
			double ze_b = results.generic_model.get_b();
			double ze_p = results.generic_model.getMaxLikelihood_p();
			double ze_c = results.generic_model.getMaxLikelihood_c();

			RJ_AftershockModel_Generic ze_model = new RJ_AftershockModel_Generic (
				ze_magMain, ze_mean_a, ze_sigma_a, ze_min_a,ze_max_a, ze_delta_a, ze_b, ze_p, ze_c);

			test_epi_selection (gamma_config, the_num_sim, ze_model);

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  event_id  forecast_lag
		// Compute model for the given event at the given forecast lag.
		// The forecast_lag is given in java.time.Duration format.
		// Show the cumulative probabilities (zeta) for the given model and the given forecast lag.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Two additional arguments

			if (args.length != 3) {
				System.err.println ("LogLikeSet : Invalid 'test4' subcommand");
				return;
			}

			String the_event_id = args[1];
			long the_forecast_lag = SimpleUtils.string_to_duration (args[2]);

			//boolean discard_large_as = true;

			// Get configuration

			GammaConfig gamma_config = new GammaConfig();
			//gamma_config.discard_sim_with_large_as = discard_large_as;

			int the_num_sim = gamma_config.simulation_count;

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

		System.err.println ("LogLikeSet : Unrecognized subcommand : " + args[0]);
		return;
	}
}
