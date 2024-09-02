package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEOmoriCalc;
import org.opensha.oaf.oetas.OERandomGenerator;
import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OESeedParams;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OEInitFixedState;
import org.opensha.oaf.oetas.OEExaminerSaveList;
import org.opensha.oaf.oetas.OESimulator;
import org.opensha.oaf.oetas.OECatalogParamsStats;
import org.opensha.oaf.oetas.OESeedParamsStats;

import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.util.AutoExecutorService;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.SimpleExecTimer;
import org.opensha.oaf.util.SimpleThreadLoopHelper;
import org.opensha.oaf.util.SimpleThreadLoopResult;
import org.opensha.oaf.util.SimpleThreadManager;
import org.opensha.oaf.util.SimpleThreadTarget;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.oetas.except.OEException;
import org.opensha.oaf.oetas.except.OEFitException;
import org.opensha.oaf.oetas.except.OEFitThreadAbortException;
import org.opensha.oaf.oetas.except.OEFitTimeoutException;

import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;				// natural logarithm of 10
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
import static org.opensha.oaf.oetas.OEConstants.TINY_MAG_DELTA;			// a very small change in magnitude
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value
import static org.opensha.oaf.oetas.OEConstants.TINY_DURATION_DAYS;		// an extremely small duration
import static org.opensha.oaf.oetas.OEConstants.TINY_BACKGROUND_RATE;	// an extremely small background rate

import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MCT_INFINITY;		// 1 = From time-dependent magnitude of completeness to infinity.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MCT_MAG_MAX;		// 2 = From time-dependent magnitude of completeness to maximum simulation magnitude.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MAGCAT_INFINITY;	// 3 = From catalog magnitude of completeness to infinity.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MAGCAT_MAG_MAX;		// 4 = From catalog magnitude of completeness to maximum simulation magnitude.


// Class to calculate integrated intensity function at a set of intervals.
// Author: Michael Barall 06/30/2023.
//
// INTENSITY FUNCTIONS
//
// For an earthquake of magnitude ms occurring at time s, the (corrected) productivity is
//
//   k = 10^(a + alpha*(ms - mref)) * Q
//
// The intensity function, which gives the rate of direct aftershocks, per unit time t > s,
// per unit magnitude m, is
//
//   lambda(t, m) = k * beta * (10^(-b*(m - mref))) * ((t - s + c)^(-p))
//
// For a mainshock, substitute a -> ams.  Mainshocks also typically use Q -> 1.
//
// Consider a source interval from time s1 to s2.  An interval is assigned a productivity density
// ks, which represents productivity per unit time within the interval, and is assumed to be
// constant within the interval.  The total productivity of the interval is then  ks*(s2 - s1),
// which could be converted to an equivalent magnitude using the formula above.  The intensity
// function for the interval, acting as a source, for time t > s1, is
//
//   lambda(t, m) = ks * beta * (10^(-b*(m - mref))) * Integral(s = s1, s = min(t,s2); (t - s + c)^(-p) * ds)
//
// A background rate "mu" is the rate of background earthquakes with magnitude >= mref per
// unit time.  The intensity function due to the background rate, which is time-independent, is
//
//   lambda(m) =  mu * beta * (10^(-b*(m - mref)))
//
// The total intensity function at time t is obtained by summing the above formulas over all
// ruptures at times s < t, and all intervals with beginning times s1 < t, plus the background contribution.
//
// INTEGRATED INTENSITY FUNCTION
//
// Our goal is to calculate the time-integral of the rate of earthquakes with magnitude m >= mc, where
// mc is the magnitude of completeness.  Because the the G-R distribution, this is
// lambda(t, mc)/beta.  One could alternatively use the rate of earthquakes with magnitudes between
// mc and some upper magnitude.
//
// Specifically, for a target interval at time t1 to t2, we want to calculate the time-integral
//
//   Integral(t = t1, t = t2; lambda(t, mct)/beta * dt)
//
// where mct is the magnitude of completeness of the target interval.  (We assume that the target
// interval is chosen so that mct is constant within the target interval, specifically, that the
// target interval is a sub-interval of one of the intervals in the history.)
//
// For each source rupture at time s < t2, the contribution to the integral is:
//
//   k * (10^(-b*(mct - mref))) * Integral(t = max(t1,s), t = t2; (t - s + c)^(-p) * dt)
//
// For a background rate source, the contribution to the integral is the expected number of
// background earthquakes with magnitude >= mct:
//
//   mu * (10^(-b*(mct - mref)) * (t2 - t1)
//
// For each source interval at time s1 to s2, with s2 <= t1:
//
//   ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * dt * ds)
//
// For the interval itself, acting as its own source, t1 == s1 and t2 == s2:
//
//   ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * dt * ds)
//
// Note: If it is desired to have an upper magnitude mup, then the rate of earthquakes with
// magnitude mc <= m <= mup is (lambda(t, mc)/beta)*(1 - exp(-beta*(mup - mc))).  So, this can be
// done by changing (10^(-b*(mct - mref))) to (10^(-b*(mct - mref)))*(1 - exp(-beta*(mup - mct)))
// in the above formulas.

public class OEDisc2IntensityCalc {

	//----- Configuration options -----

	// Maximum interval duration, as a fraction of the total duration, or zero if no maximum.

	public double max_frac_duration;

	// The rupture history to fit.

	private OEDisc2History history;

	// The duration of each interval, in days; length = history.interval_count.

	private double[] history_a_interval_duration;

	// The midpoint of each interval, in days; length = history.interval_count.

	private double[] history_a_interval_midpoint;

	// Information about parameter fitting.
	// Note: The following fields of fit_info are used:
	//  mref
	//  mag_max
	//  lmr_opt
	//  f_intensity (must be true)
	//  like_int_begin
	//  like_int_end
	//  main_rup_begin
	//  main_rup_end
	// Note: For testing, if it is desired to calculate (10^a)*Q, then the following additional fields are needed:
	//  mag_min
	//  tint_br

	private OEDisc2InitFitInfo fit_info;




	//----- Timepoints -----

	// Kind, for timepoints and lines in the file.

	public static final int ILKIND_RUPTURE = 1;			// A rupture.
	public static final int ILKIND_FILL = 2;			// A filling timepoint.
	public static final int ILKIND_SUMMARY = 3;			// A summary line in the file.


	// Holds a timepoint.

	public static class TimePoint {

		// Kind, must be ILKIND_RUPTURE or ILKIND_FILL.

		public int ilkind;

		// Time in days (in the interval time sequence).

		public double t_day;

		// Magnitude for a rupture, otherwise NO_MAG_NEG.

		public double rup_mag;

		// Number of ruptures before this timepoint (includes ruptures before the first timepoint).
		// If this timepoint is a rupture, it is the index number of the rupture within the history.

		public int n_rup;

		// Number of intervals entirely before this timepoint (includes intervals before the first timepoint).
		// Also the index (within the history) of the first interval any part of which is after the timepoint.

		public int n_int;

		// True if this timepoint is in the interior of the interval (otherwise it is at the beginning).

		public boolean int_is_interior;


		// Set values for a rupture timepoint.

		public final TimePoint set_rupture (
			double t_day,
			double rup_mag,
			int n_rup,
			int n_int,
			boolean int_is_interior
		) {
			this.ilkind = ILKIND_RUPTURE;
			this.t_day = t_day;
			this.rup_mag = rup_mag;
			this.n_rup = n_rup;
			this.n_int = n_int;
			this.int_is_interior = int_is_interior;
			return this;
		}


		// Set values for a filling timepoint.

		public final TimePoint set_filling (
			double t_day,
			int n_rup,
			int n_int,
			boolean int_is_interior
		) {
			this.ilkind = ILKIND_FILL;
			this.t_day = t_day;
			this.rup_mag = NO_MAG_NEG;
			this.n_rup = n_rup;
			this.n_int = n_int;
			this.int_is_interior = int_is_interior;
			return this;
		}


		// Produce a one-line string describing the timepoint.
		// This is primarily for testing.

		@Override
		public String toString() {
			return ilkind
				+ ", t_day = " + rndd(t_day)
				+ ", rup_mag = " + SimpleUtils.double_to_string_trailz ("%.4f", SimpleUtils.TRAILZ_REMOVE, rup_mag)
				+ ", n_rup = " + n_rup
				+ ", n_int = " + n_int
				+ ", interior = " + int_is_interior;
		}


		// Return true if this timepoint is a rupture.

		public final boolean is_rupture () {
			return ilkind == ILKIND_RUPTURE;
		}


		// Make the prefix used for a line in the output file.

		public final String line_prefix () {
			String result = null;
			switch (ilkind) {
			default:
				throw new IllegalArgumentException ("OEDisc2IntensityCalc.TimePoint.line_prefix: Invalid kind: ilkind = " + ilkind);
			case ILKIND_RUPTURE:
				result = ilkind + " " + rndd(t_day) + " " + SimpleUtils.double_to_string_trailz ("%.4f", SimpleUtils.TRAILZ_REMOVE, rup_mag);
				break;
			case ILKIND_FILL:
				result = ilkind + " " + rndd(t_day) + " " + SimpleUtils.double_to_string_trailz ("%.4f", SimpleUtils.TRAILZ_REMOVE, rup_mag);
				break;
			}
			return result;
		}
	}


	// List of timepoints.
	// The first and last elements are filling timepoints at the start and end of the time range.

	private TimePoint[] a_timepoint_list;




	// Make the timepoint list.

	private void make_timepoint_list () {
		final int rupture_count = history.rupture_count;
		final OERupture[] a_rupture_obj = history.a_rupture_obj;
		final int[] a_rupture_int_time_index = history.a_rupture_int_time_index;
		final double[] a_rupture_int_time_value = history.a_rupture_int_time_value;
		final boolean[] a_rupture_int_is_interior = history.a_rupture_int_is_interior;
		final int interval_count = history.interval_count;
		final double[] a_interval_time = history.a_interval_time;

		// Beginning and ending+1 of the intervals to process

		final int n_int_begin = fit_info.like_int_begin;
		final int n_int_end = fit_info.like_int_end;

		// The maximum duration, zero if no minimum, and a flag indicating if fill is needed

		final boolean f_filling = max_frac_duration > 1.0e-5;
		final double max_duration = (a_interval_time[n_int_end] - a_interval_time[n_int_begin]) * max_frac_duration;

		// The list to build

		List<TimePoint> timepoint_list = new ArrayList<TimePoint>();

		// Number of whole intervals processed so far

		int n_int = n_int_begin;

		// Find the beginning of the ruptures to process, skip over any before or at the start of the first interval

		int n_rup_begin = 0;

		while ((n_rup_begin < rupture_count)
				&& (
					(a_rupture_int_time_index[n_rup_begin] < n_int_begin)
					|| ( (a_rupture_int_time_index[n_rup_begin] == n_int_begin) && !(a_rupture_int_is_interior[n_rup_begin]) )
				)
		) {
			++n_rup_begin;
		}

		// Find the ending+1 of the ruptures to process, so all ruptures are before the end of the last interval

		int n_rup_end = n_rup_begin;

		while ((n_rup_end < rupture_count)
				&& (a_rupture_int_time_index[n_rup_end] < n_int_end)
		) {
			++n_rup_end;
		}

		// Begin with initial filling

		double previous_t_day = a_interval_time[n_int];

		timepoint_list.add ((new TimePoint()).set_filling (
			previous_t_day,
			n_rup_begin,
			n_int,
			false
		));

		// Loop while we need more timepoints

		for (int n_rup = n_rup_begin; n_rup <= n_rup_end; ++n_rup) {

			// Time of the next timepoint, either rupture time or the time of the final fill

			final double next_t_day = ((n_rup < n_rup_end) ? a_rupture_int_time_value[n_rup] : a_interval_time[n_int_end]);

			// If we're filling, and the gap exceeds the maximum duration ...

			if (f_filling && next_t_day - previous_t_day > max_duration) {

				// Number of subintervals for filling

				int n_subint = Math.max (2, (int)(Math.round ( ((next_t_day - previous_t_day) / max_duration) + 0.5 )));

				// Duration of each subinterval

				double dur_subint = (next_t_day - previous_t_day) / ((double)n_subint);

				// Loop over subintervals

				for (int i_subint = 1; i_subint < n_subint; ++i_subint) {

					// Subinterval time, by interpolation

					double t_subint = previous_t_day + (dur_subint * ((double)i_subint));

					// Advance the interval count past any that lie entirely before the subinterval time

					while (n_int + 1 < n_int_end && a_interval_time[n_int + 1] < t_subint) {
						++n_int;
					}

					// Assume interior of interval

					boolean f_subint_interior = true;

					// If in first half of interval ...

					if (t_subint <= history_a_interval_midpoint[n_int]) {

						// If close to start of interval, move to start of interval

						if (Math.abs (t_subint - a_interval_time[n_int]) <= dur_subint * 0.02) {
							t_subint = a_interval_time[n_int];
							f_subint_interior = false;
						}
					}

					// Otherwise, in second half of interval ...

					else {

						// If close to end of interval, move to start of next interval

						if (Math.abs (a_interval_time[n_int + 1] - t_subint) <= dur_subint * 0.02) {
							++n_int;
							t_subint = a_interval_time[n_int];
							f_subint_interior = false;
						}
					}

					// Append the filling timepoint

					timepoint_list.add ((new TimePoint()).set_filling (
						t_subint,
						n_rup,
						n_int,
						f_subint_interior
					));
				}
			}

			// Append a rupture timepoint

			if (n_rup < n_rup_end) {

				n_int = a_rupture_int_time_index[n_rup];

				timepoint_list.add ((new TimePoint()).set_rupture (
					next_t_day,
					a_rupture_obj[n_rup].rup_mag,
					n_rup,
					n_int,
					a_rupture_int_is_interior[n_rup]
				));
			}

			// Append the final fitting timepoint
			
			else {

				n_int = n_int_end;

				timepoint_list.add ((new TimePoint()).set_filling (
					next_t_day,
					n_rup,
					n_int,
					false
				));
			}

			// Next time becomes the previous time

			previous_t_day = next_t_day;
		}

		// Get the list of timepoints as an array

		int len = timepoint_list.size();
		a_timepoint_list = new TimePoint[len];
		for (int i = 0; i < len; ++i) {
			a_timepoint_list[i] = timepoint_list.get(i);
		}

		return;
	}




	// Make a string containing the entire timepoint list.
	// This is primarily for testing.

	public String dump_timepoints () {
		StringBuilder result = new StringBuilder();

		for (TimePoint tp: a_timepoint_list) {
			result.append (tp.toString() + "\n");
		}

		return result.toString();
	}




	//----- ETAS parameters -----

	// Gutenberg-Richter parameter, b-value.

	private double b;

	// ETAS intensity parameter, alpha-value.

	private double alpha;

	//Omori c-value.

	private double c;

	// Omori p-value.

	private double p;

	// Secondary productivity, value of (10^a)*Q, for reference magnitude equal to fit_info.mref.

	private double ten_a_q;

	// Mainshock productivity, value of (10^ams)*Q, for reference magnitude equal to fit_info.mref.  (Typically Q == 1.)

	private double ten_ams_q;

	// Background productivity, mu-value, for reference magnitude equal to fit_info.mref.

	private double mu;



	//----- Extended sources -----

	// The productivity density for each interval, due to all sources.
	// With length == interval_count, each element contains the productivity density for the corresponding interval.
	// Multiply by the interval duration to get total productivity (not density).
	// Null if no extended sources are being used.

	private double[] extended_prod_density;




	//----- Results -----

	// Incremental integrated lambda.
	// Length = a_timepoint_list.length.
	// incr_integrated_lambda[n] is the integral of lambda between timepoint n-1 and timepoint n.

	public double[] incr_integrated_lambda;

	// Split integrated lambda.
	// Length = a_timepoint_list.length.
	// split_integrated_lambda[n] is the integral of lambda to timepoint n from the most recent prior rupture timepoint.

	public double[] split_integrated_lambda;

	// Cuulative integrated lambda.
	// Length = a_timepoint_list.length.
	// cum_integrated_lambda[n] is the integral of lambda to timepoint n from the beginning of the time range.

	public double[] cum_integrated_lambda;

	// Cumulative number of ruptures.
	// Length = a_timepoint_list.length.
	// cum_rupture_num[n] is the number of ruptures up to and including timepoint n from the beginning of the time range.

	public int[] cum_rupture_num;

	// The number of splits, equal to the number of ruptures in the time range.

	public int split_num;

	// Mean of the rupture splits of integrated lambda.

	public double split_mean;

	// Variance of the rupture splits of integrated lambda.

	public double split_var;




	// Finish computng results.
	// Assumes that incr_integrated_lambda has been computed.

	private void finish_results () {

		// Allocate arrays

		final int num_tp = a_timepoint_list.length;

		split_integrated_lambda = new double[num_tp];
		cum_integrated_lambda = new double[num_tp];
		cum_rupture_num = new int[num_tp];

		// Accumulators

		double split_accum = 0.0;
		double cum_accum = 0.0;

		double mean_accum = 0.0;
		int num_accum = 0;

		// Initial integrals are zero

		split_integrated_lambda[0] = 0.0;
		cum_integrated_lambda[0] = 0.0;
		cum_rupture_num[0] = 0;

		// Loop over timepoints after the first

		for (int j = 1; j < num_tp; ++j) {

			// Accumulate within split

			split_accum += incr_integrated_lambda[j];
			split_integrated_lambda[j] = split_accum;

			// Accumulation for ruptures

			if (a_timepoint_list[j].is_rupture()) {

				cum_accum += split_accum;
				cum_integrated_lambda[j] = cum_accum;

				mean_accum += split_accum;
				++num_accum;
				cum_rupture_num[j] = num_accum;

				split_accum = 0.0;
			}

			// Accumulation for filling

			else {
				cum_integrated_lambda[j] = cum_accum + split_accum;
				cum_rupture_num[j] = num_accum;
			}
		}

		// If no splits...

		if (num_accum == 0) {
			split_num = 0;
			split_mean = 0.0;
			split_var = 0.0;
		}

		// If splits, save mean and variance

		else {
			split_num = num_accum;
			split_mean = mean_accum / ((double)num_accum);

			double var_accum = 0.0;
			for (int j = 1; j < num_tp; ++j) {
				if (a_timepoint_list[j].is_rupture()) {
					final double delta = split_integrated_lambda[j] - split_mean;
					var_accum += (delta * delta);
				}
			}

			split_var = var_accum / ((double)num_accum);
		}

		return;
	}




	// Make a string containing the entire output file.
	// If other objects are supplied, their integrated lambda values are included on each line.

	public String output_file_as_string (OEDisc2IntensityCalc... others) {
		StringBuilder result = new StringBuilder();

		// One line for each timepoint

		int lines = a_timepoint_list.length;

		// First and last times

		double t_first = a_timepoint_list[0].t_day;
		double t_last = a_timepoint_list[lines - 1].t_day;

		// Summary line

		result.append (ILKIND_SUMMARY);
		result.append (" ");
		result.append (rndd(t_first));
		result.append (" ");
		result.append (rndd(t_last));
		result.append (" ");
		result.append (lines);
		result.append (" ");
		result.append (split_num);
		result.append (" ");
		result.append (rndd(split_mean));
		result.append (" ");
		result.append (rndd(split_var));

		// For each other object, append their mean and variance

		for (OEDisc2IntensityCalc other : others) {
			result.append (" ");
			result.append (rndd(other.split_mean));
			result.append (" ");
			result.append (rndd(other.split_var));
		}

		// Finish the summary line

		result.append ("\n");

		// Loop over timepoints

		for (int j = 0; j < lines; ++j) {

			// Prefix from the timepoint

			result.append (a_timepoint_list[j].line_prefix());

			// The cumulative number of ruptures

			result.append (" ");
			result.append (cum_rupture_num[j]);

			// The cumulative, split, and incremental integrated lambda

			result.append (" ");
			result.append (rndd(cum_integrated_lambda[j]));
			result.append (" ");
			result.append (rndd(split_integrated_lambda[j]));
			result.append (" ");
			result.append (rndd(incr_integrated_lambda[j]));

			// For each other object, append their cumulative, split, and incremental integrated lambda

			for (OEDisc2IntensityCalc other : others) {
				result.append (" ");
				result.append (rndd(other.cum_integrated_lambda[j]));
				result.append (" ");
				result.append (rndd(other.split_integrated_lambda[j]));
				result.append (" ");
				result.append (rndd(other.incr_integrated_lambda[j]));
			}

			// Finish the line

			result.append ("\n");
		}

		return result.toString();
	}




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		max_frac_duration = 0.0;
		history  = null;
		history_a_interval_duration = null;
		history_a_interval_midpoint = null;
		fit_info = null;

		a_timepoint_list = null;

		b         = 0.0;
		alpha     = 0.0;
		c         = 0.0;
		p         = 0.0;
		ten_a_q   = 0.0;
		ten_ams_q = 0.0;
		mu        = 0.0;

		extended_prod_density = null;

		incr_integrated_lambda = null;
		split_integrated_lambda = null;
		cum_integrated_lambda = null;
		cum_rupture_num = null;
		split_num = 0;
		split_mean = 0.0;
		split_var = 0.0;

		return;
	}




	// Default constructor.

	public OEDisc2IntensityCalc () {
		clear();
	}




	// Set configuration options.
	// The history and fitting info are retained in this object.
	// Note: The fitting information must have f_intensity set, indicating that fitting saved the necessary information.

	public OEDisc2IntensityCalc set_config (
		double max_frac_duration,
		OEDisc2History history,
		OEDisc2InitFitInfo fit_info
	) {
		if (!( fit_info.f_intensity )) {
			throw new IllegalArgumentException ("OEDisc2IntensityCalc.set_config: Fitting was done without saving information to calculate the intensity function");
		}

		this.max_frac_duration = max_frac_duration;
		this.history = history;
		this.fit_info = fit_info;

		this.history_a_interval_duration = history.make_a_interval_duration();
		this.history_a_interval_midpoint = history.make_a_interval_midpoint();

		make_timepoint_list();
		return this;
	}




	// Copy configuration options from another object.
	// This makes it possible to create several objects with the same list of timepoints.
	// Note: This is a shallow copy.

	public OEDisc2IntensityCalc copy_config_from (
		OEDisc2IntensityCalc other
	) {
		this.max_frac_duration = other.max_frac_duration;
		this.history = other.history;
		this.fit_info = other.fit_info;

		this.history_a_interval_duration = other.history_a_interval_duration;
		this.history_a_interval_midpoint = other.history_a_interval_midpoint;

		this.a_timepoint_list = other.a_timepoint_list;
		return this;
	}




	// Set ETAS parameters.

	public OEDisc2IntensityCalc set_params (
		double b,
		double alpha,
		double c,
		double p,
		double ten_a_q,
		double ten_ams_q,
		double mu
	) {
		this.b         = b;
		this.alpha     = alpha;
		this.c         = c;
		this.p         = p;
		this.ten_a_q   = ten_a_q;
		this.ten_ams_q = ten_ams_q;
		this.mu        = mu;
		return this;
	}




	// Set extended sources.
	// Note: If non-null, the array is retained in this object.

	public OEDisc2IntensityCalc set_extended (
		double[] extended_prod_density
	) {
		if (extended_prod_density != null) {
			if (!( extended_prod_density.length == history.interval_count )) {
				throw new IllegalArgumentException ("OEDisc2IntensityCalc.set_extended: Array length mismatch: extended_prod_density.length = " + extended_prod_density.length + ", history.interval_count = " + history.interval_count);
			}
		}

		this.extended_prod_density = extended_prod_density;
		return this;
	}




	//----- Productivity -----




	// Productivity for each source rupture.
	// The value for each non-mainshock rupture is
	//   10^(alpha*(ms - mref)) * ten_a_q
	// The value for each mainshock rupture is
	//   10^(alpha*(ms - mref)) * ten_ams_q
	// where ms is the magnitude of the rupture.
	// The length is history.rupture_count.

	private double[] rup_prod;


	// Build the source productivity for each rupture.

	private void build_rup_prod () {
		final int rupture_count = history.rupture_count;
		final OERupture[] a_rupture_obj = history.a_rupture_obj;

		rup_prod = new double[rupture_count];

		final double mref = fit_info.mref;
		final int nlo = fit_info.main_rup_begin;
		final int nhi = fit_info.main_rup_end;

		for (int n = 0; n < rupture_count; ++n) {
			if (n >= nlo && n < nhi) {
				rup_prod[n] = Math.pow(10.0, alpha*(a_rupture_obj[n].rup_mag - mref)) * ten_ams_q;
			} else {
				rup_prod[n] = Math.pow(10.0, alpha*(a_rupture_obj[n].rup_mag - mref)) * ten_a_q;
			}
		}

		return;
	}




	// Partial likelihood density for each target interval, with unlimited or limited upper magnitude.
	// The value for each interval is
	//   (10^(-b*(mct - mref)))
	// where mct is the magnitude of completeness of the interval.
	// If there is an upper magnitude mup, with mup > mct, then the value for each rupture is
	//   (10^(-b*(mct - mref))) * (1 - exp(-beta*(mup - mct)))
	// If an interval is not to be included in the likelihood calculation, then the value is zero.
	// The length is history.interval_count.
	// Multiply by the interval duration to get total partial likelihood (not density).

	public double[] int_part_like_density;


	// Build the partial likelihood density for each target interval, with unlimited or limited upper magnitude.

	private void build_int_part_like_density () {
		final int interval_count = history.interval_count;
		final double[] a_interval_mc = history.a_interval_mc;
		final double magCat = history.magCat;

		final double beta = b * C_LOG_10;

		int_part_like_density = new double[interval_count];

		final double mref = fit_info.mref;
		final double mag_max = fit_info.mag_max;
		final int nlo = fit_info.like_int_begin;
		final int nhi = fit_info.like_int_end;
		final int lmr_opt = fit_info.lmr_opt;

		switch (lmr_opt) {

		default:
			throw new IllegalArgumentException ("OEDisc2IntensityCalc.build_int_part_like_density - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
		case LMR_OPT_MCT_INFINITY:
			// From time-dependent magnitude of completeness to infinity
			for (int n = 0; n < interval_count; ++n) {
				if (nlo <= n && n < nhi) {
					int_part_like_density[n] = Math.pow(10.0, b*(mref - a_interval_mc[n]));
				} else {
					int_part_like_density[n] = 0.0;
				}
			}
			break;
			
		case LMR_OPT_MCT_MAG_MAX:
			// From time-dependent magnitude of completeness to maximum simulation magnitude
			// (We force the range to be at least 0.5 magnitude)
			for (int n = 0; n < interval_count; ++n) {
				if (nlo <= n && n < nhi) {
					int_part_like_density[n] = Math.pow(10.0, b*(mref - a_interval_mc[n])) * (- Math.expm1(beta*(Math.min(-0.5, a_interval_mc[n] - mag_max))));
				} else {
					int_part_like_density[n] = 0.0;
				}
			}
			break;
			
		case LMR_OPT_MAGCAT_INFINITY:
			// From catalog magnitude of completeness to infinity
			for (int n = 0; n < interval_count; ++n) {
				if (nlo <= n && n < nhi) {
					int_part_like_density[n] = Math.pow(10.0, b*(mref - magCat));
				} else {
					int_part_like_density[n] = 0.0;
				}
			}
			break;
			
		case LMR_OPT_MAGCAT_MAG_MAX:
			// From catalog magnitude of completeness to maximum simulation magnitude
			// (We force the range to be at least 0.5 magnitude)
			for (int n = 0; n < interval_count; ++n) {
				if (nlo <= n && n < nhi) {
					int_part_like_density[n] = Math.pow(10.0, b*(mref - magCat)) * (- Math.expm1(beta*(Math.min(-0.5, magCat - mag_max))));
				} else {
					int_part_like_density[n] = 0.0;
				}
			}
			break;
		}

		return;
	}




	//----- Integration -----




	// Calculate the integrated intensity over a given interval.
	// Parameters:
	//  t1 = Start of target interval, in days.
	//  t2 = End of target interval, in days.  Must satisfy t2 > t1.
	//  n_rup = Number of ruptures before the target interval (at times <= t1).
	//  n_int = Number of whole intervals before the target interval (ending at times <= t1).
	// Returns the integral of lambda, for times t1 to t1, and magnitudes above the magnitude of completeness.
	// Note: Assumes that the target interval is a sub-interval of history interval n_int,
	// and that there are no ruptures inside the target interval.
	// Threading: Can be called simultaneously by multiple threads.

	public final double integrate_lambda (double t1, double t2, int n_rup, int n_int) {
		double result = 0.0;

		final double duration = t2 - t1;
		final double t1_plus_c = t1 + c;

		final double[] a_rupture_int_time_value = history.a_rupture_int_time_value;
		final double[] a_interval_time = history.a_interval_time;

		// If there is a background rate ...

		if (mu > TINY_BACKGROUND_RATE) {

			// Background rate contribution

			result += (mu * duration);
		}

		// Loop over ruptures prior to the target interval ...

		for (int i_rup = 0; i_rup < n_rup; ++i_rup) {

			// Contribution of rupture

			result += (rup_prod[i_rup] * OEOmoriCalc.omext_single_integral (
				p, t1_plus_c - a_rupture_int_time_value[i_rup], duration
			));
		}

		// If there are extended sources ...

		if (extended_prod_density != null) {

			// Loop over whole intervals prior to the target interval ...

			for (int i_int = 0; i_int < n_int; ++i_int) {

				// Contribution of whole interval

				result += (extended_prod_density[i_int] * OEOmoriCalc.omext_double_integral (
					p, t1_plus_c - a_interval_time[i_int + 1], history_a_interval_duration[i_int], duration
				));
			}

			// If there is a partial interval ...

			final double partial_dur = t1 - a_interval_time[n_int];
			if (partial_dur > TINY_DURATION_DAYS) {

				// Contribution of partial interval

				result += (extended_prod_density[n_int] * OEOmoriCalc.omext_double_integral (
					p, c, partial_dur, duration
				));
			}

			// Contribution of target interval to itself

			result += (extended_prod_density[n_int] * OEOmoriCalc.omext_self_double_integral (
				p, c, duration
			));
		}

		// Apply the partial likelihood, which is the integral over magnitude

		result *= int_part_like_density[n_int];	// 10^(-b*(mct - mref))

		return result;
	}




	// Calculate the integrated intensity between two successive timepoints.
	// Parameters:
	//  n_tp = Index number of timepoint, must satisfy 1 <= n_tp < a_timepoint_list.length.
	// Returns the integral of lambda, for times between timepoint n_tp-1 and n_tp,
	// and magnitudes above the magnitude of completeness.
	// Threading: Can be called simultaneously by multiple threads.

	public final double integrate_lambda (int n_tp) {
		double result = 0.0;

		final double[] a_interval_time = history.a_interval_time;

		// Get the previous and current timepoints

		final TimePoint previous_tp = a_timepoint_list[n_tp - 1];
		final TimePoint current_tp = a_timepoint_list[n_tp];

		// Number of prior ruptures is the number prior to the current timepoint

		final int n_rup = current_tp.n_rup;

		// Start with the interval that contains the previous timepoint

		int n_int = previous_tp.n_int;
		boolean f_interior = previous_tp.int_is_interior;
		double t1 = previous_tp.t_day;

		// Loop over intervals that end before the current timepoint

		while (n_int < current_tp.n_int) {

			// Integrate to the end of the current interval

			final double t2 = a_interval_time[n_int + 1];
			result += integrate_lambda (t1, t2, n_rup, n_int);

			// Continue at the start of the next interval

			++n_int;
			t1 = t2;
			f_interior = false;
		}

		// If the current timepoint is interior to its interval, integrate over the partial interval

		if (current_tp.int_is_interior) {

			// Integrate over the partial interval

			double t2 = current_tp.t_day;
			result += integrate_lambda (t1, t2, n_rup, n_int);

			// What the continuation would be

			t1 = t2;
			f_interior = true;
		}

		// Return integral

		return result;
	}




	//----- Thread manager for integrating intensity function -----




	// Progress message format while running.

	private static final String PMFMT_RUNNING = "Completed %C of %L steps (%P%%) in %E seconds using %U";

	// Progress message format after completion.

	private static final String PMFMT_DONE = "Completed all %L steps in %E seconds";

	// Progress message format for timeout.

	private static final String PMFMT_TIMEOUT = "Stopped after completing %C of %L steps in %E seconds";

	// Progress message format for abort.

	private static final String PMFMT_ABORT = "Aborted after completing %C of %L steps in %E seconds";




	// Class to integrate intensity function between every successive pair of timepoints.

	private class TM_integrate_lambda implements SimpleThreadTarget {

		// The loop helper.

		private SimpleThreadLoopHelper loop_helper = new SimpleThreadLoopHelper (PMFMT_RUNNING);

		// Entry point for a thread.
		// Parameters:
		//  thread_manager = The thread manager.
		//  thread_number = The thread number, which ranges from 0 to the number of
		//                  threads in the pool minus 1.
		// Threading: This function is called by all the threads in the pool, and
		// so must be thread-safe and use any needed synchronization.

		@Override
		public void thread_entry (SimpleThreadManager thread_manager, int thread_number) throws Exception {

			// Loop until loop completed or prompt termination is requested

			for (int index = loop_helper.get_loop_index(); index >= 0; index = loop_helper.get_next_index()) {

				// Integrate lambda for the interval ending at timepoint index

				incr_integrated_lambda[index] = integrate_lambda (index);
			}

			return;
		}

		// Build the list lambda integrals for all pairs of successive timepoints.
		//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.

		public void build_integrated_lambda (SimpleExecTimer exec_timer) throws OEException {

			// Say hello

			System.out.println ("Start computing integrated ETAS intensity function");

			// Build rupture productivity

			build_rup_prod();

			// Build partial likelihood density

			build_int_part_like_density();

			// Create the array for incremental integrated lambda

			incr_integrated_lambda = new double[a_timepoint_list.length];
			incr_integrated_lambda[0] = 0.0;

			// Run the loop

			loop_helper.run_loop (this, exec_timer, 1, incr_integrated_lambda.length);

			// Capture the result

			//loop_result.accum_loop (loop_helper);

			// Check for thread abort

			if (loop_helper.is_abort()) {
				System.out.println (loop_helper.get_abort_message_string());
				String loop_stat = loop_helper.make_progress_message (PMFMT_ABORT);
				String msg = "Abort computing integrated ETAS intensity function because of thread abort";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitThreadAbortException (msg + ": " + loop_stat);
			}

			// Otherwise, check for timeout

			if (loop_helper.is_incomplete()) {
				String loop_stat = loop_helper.make_progress_message (PMFMT_TIMEOUT);
				String msg = "Abort computing integrated ETAS intensity function because of timeout";
				System.out.println (loop_stat);
				System.out.println (msg);
				throw new OEFitTimeoutException (msg + ": " + loop_stat);
			}

			// Otherwise, normal termination

			System.out.println (loop_helper.make_progress_message (PMFMT_DONE));

			// Finish computing results

			finish_results();
			System.out.println ("Finish computing integrated ETAS intensity function");

			return;
		}

	}




	// Calculate integrated lambda, with multiple threads.
	// Parameters:
	//  exec_timer = Execution timer, provides executor, time limit, and progress message interval.

	public final void calc_integrated_lambda_mt (SimpleExecTimer exec_timer) throws OEException {
		TM_integrate_lambda tm = new TM_integrate_lambda();
		tm.build_integrated_lambda (exec_timer);
		return;
	}




	// Calculate integrated lambda, with a single thread.

	public final void calc_integrated_lambda_st () {

		// Say hello

		System.out.println ("Start computing integrated ETAS intensity function");

		// Build rupture productivity

		build_rup_prod();

		// Build partial likelihood density

		build_int_part_like_density();

		// Create the array for incremental integrated lambda

		incr_integrated_lambda = new double[a_timepoint_list.length];
		incr_integrated_lambda[0] = 0.0;

		// Loop over timepoints

		for (int index = 1; index < incr_integrated_lambda.length; ++index) {

			// Integrate lambda for the interval ending at timepoint index

			incr_integrated_lambda[index] = integrate_lambda (index);
		}

		// Finish computing results

		finish_results();
		System.out.println ("Finish computing integrated ETAS intensity function");

		return;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEDisc2IntensityCalc");




		// Subcommand : Test #1
		// Command format:
		//  test1  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         lmr_opt  tint_br
		//         lambda_res  f_multi
		//         [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then create a dummy fitting information structure.
		// Then create an intensity calculator configured with the history and fitting info.
		// Then display the timepoints.
		// Then set the ETAS parameters and calculate integrated lambda, either single or multi threaded.
		// Then generate and display the output file.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitting information, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.

		if (testargs.is_test ("test1")) {

			System.out.println ("Test of integrated intensity function");
			double zmu = testargs.get_double ("zmu");
			double zams = testargs.get_double ("zams");
			double n = testargs.get_double ("n");
			double p = testargs.get_double ("p");
			double c = testargs.get_double ("c");
			double b = testargs.get_double ("b");
			double alpha = testargs.get_double ("alpha");
			double mref = testargs.get_double ("mref");
			double msup = testargs.get_double ("msup");
			double tbegin = testargs.get_double ("tbegin");
			double tend = testargs.get_double ("tend");

			double magCat = testargs.get_double ("magCat");
			int helm_param = testargs.get_int ("helm_param");
			double disc_delta = testargs.get_double ("disc_delta");
			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");
			double t_interval_begin = testargs.get_double ("t_interval_begin");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			int lmr_opt = testargs.get_int ("lmr_opt");
			double tint_br = testargs.get_double ("tint_br");

			double lambda_res = testargs.get_double ("lambda_res");
			boolean f_multi = testargs.get_boolean ("f_multi");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
			testargs.end_test();

			// Make the catalog parameters

			OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
				n,		// n
				p,		// p
				c,		// c
				b,		// b
				alpha,	// alpha
				mref,	// mref
				msup,	// msup
				tbegin,	// tbegin
				tend,	// tend
				tint_br	// tint_br
			);

			// Make the seed parameters

			OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

			// Make the catalog initializer

			OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

			// Make the catalog examiner

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

			// Generate a catalog

			OESimulator.gen_single_catalog (initializer, examiner);

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the history parameters

			double t_range_begin = Math.min (tbegin, t_interval_begin);
			double t_range_end = tend;
			for (int itm = 0; itm < time_mag_array.length; itm += 2) {
				t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
			}

			OEDiscFGHParams hist_params = new OEDiscFGHParams();

			hist_params.set_sim_history_typical (
				magCat,				// magCat
				helm_param,			// helm_param
				t_range_begin,		// t_range_begin
				t_range_end,		// t_range_end
				disc_delta,			// disc_delta
				mag_cat_count,		// mag_cat_count
				eligible_mag,		// eligible_mag
				eligible_count,		// eligible_count
				split_fn,			// split_fn
				t_interval_begin,	// t_interval_begin
				before_max_count,	// before_max_count
				mag_cat_int_join	// mag_cat_int_join
			);

			// Display the history parameters

			System.out.println ();
			System.out.println (hist_params.toString());

			// Make a history

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (hist_params, rup_list);

			// Display the history

			System.out.println ();
			System.out.println (history.toString());

			// Adjust the minimum simulation magnitude to be the history's magCat

			OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
			cat_params_stats.set_fixed_mag_min (history.magCat);

			// Statistics from seed parameters

			OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

			// Create a dummy fitting information

			OEDisc2InitFitInfo fit_info = new OEDisc2InitFitInfo();

			// Fitting info that we use

			int like_int_begin = 0;
			int like_int_end = history.interval_count;
			int main_rup_begin = 0;
			int main_rup_end = history.i_inside_begin;

			// Fitting info that we don't use, so use dummy values

			boolean f_background = true;
			int group_count = 0;
			double[] a_group_time = null;
			double group_t_interval_end = history.a_interval_time[like_int_end];

			double mag_main = NO_MAG_NEG;
			for (int i = main_rup_begin; i < main_rup_end; ++i) {
				final double x = history.a_rupture_obj[i].rup_mag;
				if (mag_main < x) {
					mag_main = x;
				}
			}

			// Now make the dummy fitting info object

			fit_info.set (
				f_background,					// f_background
				group_count,					// group_count
				a_group_time,					// a_group_time
				cat_params_stats.mref,			// mref
				cat_params_stats.msup,			// msup
				cat_params_stats.mag_min_sim,	// mag_min
				cat_params_stats.mag_max_sim,	// mag_max
				mag_main,						// mag_main
				tint_br,						// tint_br
				new OEGridOptions(),			// grid_options
				history.req_t_interval_end,		// req_t_interval_end
				history.get_t_range_end(),		// hist_t_interval_end
				group_t_interval_end,			// group_t_interval_end
				true,							// f_intervals
				lmr_opt,						// lmr_opt
				true,							// f_intensity
				like_int_begin,					// like_int_begin
				like_int_end,					// like_int_end
				main_rup_begin,					// main_rup_begin
				main_rup_end					// main_rup_end
			);

			// Display the fitting information

			System.out.println ();
			System.out.println (fit_info.toString());

			// Make the intensity calculator

			OEDisc2IntensityCalc intensity_calc = new OEDisc2IntensityCalc();

			// Set the configuration

			intensity_calc.set_config (
				lambda_res,
				history,
				fit_info
			);

			// Dump the timepoints

			System.out.println ();
			System.out.println ("***** Timepoints *****");
			System.out.println ();

			System.out.println (intensity_calc.dump_timepoints());

			// Set ETAS parameters

			double ten_a_q = fit_info.calc_ten_a_q_from_branch_ratio (
				n,
				p,
				c,
				b,
				alpha
			);

			double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
				zams,
				b,
				alpha
			);

			double mu = fit_info.calc_mu_from_zmu (
				zmu,
				b
			);

			intensity_calc.set_params (
				b,
				alpha,
				c,
				p,
				ten_a_q,
				ten_ams_q,
				mu
			);

			// No extended sources

			intensity_calc.set_extended (
				null
			);

			// Calculate integrated lambda, with multiple threads

			if (f_multi) {

				// Create multi-thread context

				int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
				long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
				long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

				try (
					AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
				){
					SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);
					exec_timer.start_timer();

					// Invoke multi-threaded computation

					intensity_calc.calc_integrated_lambda_mt (exec_timer);
				}
			}

			// Calculate integrated lambda, with single thread

			else {
				intensity_calc.calc_integrated_lambda_st();
			}

			// Dump the file

			System.out.println ();
			System.out.println ("***** Output file *****");
			System.out.println ();

			System.out.println (intensity_calc.output_file_as_string());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  zmu  zams  n  p  c  b  alpha  mref  msup  tbegin  tend
		//         magCat  helm_param  disc_delta  mag_cat_count  eligible_mag  eligible_count
		//         durlim_ratio  durlim_min  durlim_max  t_interval_begin  before_max_count  mag_cat_int_join
		//         lmr_opt  tint_br
		//         lambda_res  f_multi
		//         [t_day  rup_mag]...
		// Generate a catalog with the given parameters.
		// The catalog is seeded with ruptures at the given times and magnitudes.
		// Then construct a history containing the catalog.
		// Then create a dummy fitting information structure.
		// Then create an intensity calculator configured with the history and fitting info.
		// Then display the timepoints.
		// Then set the ETAS parameters and calculate integrated lambda, either single or multi threaded.
		// Then generate and display the output file.
		// Same as text #1, except it also performs intensity calculations with the values of zams and zmu
		// multiplied by 0.25 and 4.0, and produces a file containing three sets of values.
		// Notes:
		// [tbegin, tend] is the range of times for which simulation is performed.
		// [t_interval_begin, tend] is the range of times for which intervals are constructed
		//  in the history and should satisfy t_interval_begin >= tbegin.
		// [t_range_begin, tend] is the range of times for which history is constructed,
		//  where t_range_begin is the minimum of tbegin and any seed rupture time.
		// In the fitting information, the minimum simulation magnitude is set equal to magCat reported
		//  by the history, which forces magCat intervals to have zero productivity.

		if (testargs.is_test ("test2")) {

			System.out.println ("Test of integrated intensity function");
			double zmu = testargs.get_double ("zmu");
			double zams = testargs.get_double ("zams");
			double n = testargs.get_double ("n");
			double p = testargs.get_double ("p");
			double c = testargs.get_double ("c");
			double b = testargs.get_double ("b");
			double alpha = testargs.get_double ("alpha");
			double mref = testargs.get_double ("mref");
			double msup = testargs.get_double ("msup");
			double tbegin = testargs.get_double ("tbegin");
			double tend = testargs.get_double ("tend");

			double magCat = testargs.get_double ("magCat");
			int helm_param = testargs.get_int ("helm_param");
			double disc_delta = testargs.get_double ("disc_delta");
			int mag_cat_count = testargs.get_int ("mag_cat_count");
			double eligible_mag = testargs.get_double ("eligible_mag");
			int eligible_count = testargs.get_int ("eligible_count");

			double durlim_ratio = testargs.get_double ("durlim_ratio");
			double durlim_min = testargs.get_double ("durlim_min");
			double durlim_max = testargs.get_double ("durlim_max");
			double t_interval_begin = testargs.get_double ("t_interval_begin");
			int before_max_count = testargs.get_int ("before_max_count");
			int mag_cat_int_join = testargs.get_int ("mag_cat_int_join");

			int lmr_opt = testargs.get_int ("lmr_opt");
			double tint_br = testargs.get_double ("tint_br");

			double lambda_res = testargs.get_double ("lambda_res");
			boolean f_multi = testargs.get_boolean ("f_multi");

			double[] time_mag_array = testargs.get_double_tuple_array ("time_mag_array", -1, 0, 2, "time", "mag");
			testargs.end_test();

			// Make the catalog parameters

			OECatalogParams cat_params = (new OECatalogParams()).set_to_fixed_mag_tint_br (
				n,		// n
				p,		// p
				c,		// c
				b,		// b
				alpha,	// alpha
				mref,	// mref
				msup,	// msup
				tbegin,	// tbegin
				tend,	// tend
				tint_br	// tint_br
			);

			// Make the seed parameters

			OESeedParams seed_params = (new OESeedParams()).set_from_zams_zmu (zams, zmu, cat_params);

			// Make the catalog initializer

			OEEnsembleInitializer initializer = (new OEInitFixedState()).setup_time_mag_list (cat_params, seed_params, time_mag_array, true);

			// Make the catalog examiner

			ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
			OEExaminerSaveList examiner = new OEExaminerSaveList (rup_list, true);

			// Generate a catalog

			OESimulator.gen_single_catalog (initializer, examiner);

			// Make time-splitting function

			OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

			// Make the history parameters

			double t_range_begin = Math.min (tbegin, t_interval_begin);
			double t_range_end = tend;
			for (int itm = 0; itm < time_mag_array.length; itm += 2) {
				t_range_begin = Math.min (t_range_begin, time_mag_array[itm]);
			}

			OEDiscFGHParams hist_params = new OEDiscFGHParams();

			hist_params.set_sim_history_typical (
				magCat,				// magCat
				helm_param,			// helm_param
				t_range_begin,		// t_range_begin
				t_range_end,		// t_range_end
				disc_delta,			// disc_delta
				mag_cat_count,		// mag_cat_count
				eligible_mag,		// eligible_mag
				eligible_count,		// eligible_count
				split_fn,			// split_fn
				t_interval_begin,	// t_interval_begin
				before_max_count,	// before_max_count
				mag_cat_int_join	// mag_cat_int_join
			);

			// Display the history parameters

			System.out.println ();
			System.out.println (hist_params.toString());

			// Make a history

			OEDisc2History history = new OEDisc2History();

			history.build_from_fgh (hist_params, rup_list);

			// Display the history

			System.out.println ();
			System.out.println (history.toString());

			// Adjust the minimum simulation magnitude to be the history's magCat

			OECatalogParamsStats cat_params_stats = cat_params.get_params_stats();
			cat_params_stats.set_fixed_mag_min (history.magCat);

			// Statistics from seed parameters

			OESeedParamsStats seed_params_stats = seed_params.get_params_stats();

			// Create a dummy fitting information

			OEDisc2InitFitInfo fit_info = new OEDisc2InitFitInfo();

			// Fitting info that we use

			int like_int_begin = 0;
			int like_int_end = history.interval_count;
			int main_rup_begin = 0;
			int main_rup_end = history.i_inside_begin;

			// Fitting info that we don't use, so use dummy values

			boolean f_background = true;
			int group_count = 0;
			double[] a_group_time = null;
			double group_t_interval_end = history.a_interval_time[like_int_end];

			double mag_main = NO_MAG_NEG;
			for (int i = main_rup_begin; i < main_rup_end; ++i) {
				final double x = history.a_rupture_obj[i].rup_mag;
				if (mag_main < x) {
					mag_main = x;
				}
			}

			// Now make the dummy fitting info object

			fit_info.set (
				f_background,					// f_background
				group_count,					// group_count
				a_group_time,					// a_group_time
				cat_params_stats.mref,			// mref
				cat_params_stats.msup,			// msup
				cat_params_stats.mag_min_sim,	// mag_min
				cat_params_stats.mag_max_sim,	// mag_max
				mag_main,						// mag_main
				tint_br,						// tint_br
				new OEGridOptions(),			// grid_options
				history.req_t_interval_end,		// req_t_interval_end
				history.get_t_range_end(),		// hist_t_interval_end
				group_t_interval_end,			// group_t_interval_end
				true,							// f_intervals
				lmr_opt,						// lmr_opt
				true,							// f_intensity
				like_int_begin,					// like_int_begin
				like_int_end,					// like_int_end
				main_rup_begin,					// main_rup_begin
				main_rup_end					// main_rup_end
			);

			// Display the fitting information

			System.out.println ();
			System.out.println (fit_info.toString());

			// Make the intensity calculator

			OEDisc2IntensityCalc intensity_calc = new OEDisc2IntensityCalc();

			OEDisc2IntensityCalc[] others = new OEDisc2IntensityCalc[2];

			// Set the configuration

			intensity_calc.set_config (
				lambda_res,
				history,
				fit_info
			);

			others[0] = (new OEDisc2IntensityCalc()).copy_config_from (intensity_calc);
			others[1] = (new OEDisc2IntensityCalc()).copy_config_from (intensity_calc);

			// Dump the timepoints

			System.out.println ();
			System.out.println ("***** Timepoints *****");
			System.out.println ();

			System.out.println (intensity_calc.dump_timepoints());

			// Set ETAS parameters

			double ten_a_q = fit_info.calc_ten_a_q_from_branch_ratio (
				n,
				p,
				c,
				b,
				alpha
			);

			double ten_ams_q = fit_info.calc_ten_ams_q_from_zams (
				zams,
				b,
				alpha
			);

			double mu = fit_info.calc_mu_from_zmu (
				zmu,
				b
			);

			intensity_calc.set_params (
				b,
				alpha,
				c,
				p,
				ten_a_q,
				ten_ams_q,
				mu
			);

			others[0].set_params (
				b,
				alpha,
				c,
				p,
				ten_a_q,
				ten_ams_q * 0.25,
				mu * 0.25
			);

			others[1].set_params (
				b,
				alpha,
				c,
				p,
				ten_a_q,
				ten_ams_q * 4.0,
				mu * 4.0
			);

			// No extended sources

			intensity_calc.set_extended (
				null
			);

			others[0].set_extended (
				null
			);

			others[1].set_extended (
				null
			);

			// Calculate integrated lambda, with multiple threads

			if (f_multi) {

				// Create multi-thread context

				int num_threads = AutoExecutorService.AESNUM_DEFAULT;	// -1
				long max_runtime = SimpleExecTimer.NO_MAX_RUNTIME;		// -1L
				long progress_time = SimpleExecTimer.DEF_PROGRESS_TIME;

				try (
					AutoExecutorService auto_executor = new AutoExecutorService (num_threads);
				){
					SimpleExecTimer exec_timer = new SimpleExecTimer (max_runtime, progress_time, auto_executor);
					exec_timer.start_timer();

					// Invoke multi-threaded computation

					intensity_calc.calc_integrated_lambda_mt (exec_timer);

					for (OEDisc2IntensityCalc other : others) {
						other.calc_integrated_lambda_mt (exec_timer);
					}
				}
			}

			// Calculate integrated lambda, with single thread

			else {
				intensity_calc.calc_integrated_lambda_st();

				for (OEDisc2IntensityCalc other : others) {
					other.calc_integrated_lambda_st();
				}
			}

			// Dump the file

			System.out.println ();
			System.out.println ("***** Output file *****");
			System.out.println ();

			System.out.println (intensity_calc.output_file_as_string (others));

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
