package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Collection;
import java.util.Arrays;

import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OEOmoriCalc;
import org.opensha.oaf.oetas.OECatalogParams;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;				// natural logarithm of 10
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value


// Discretized rupture history, parameter fitting with extended sources.
// Author: Michael Barall 09/14/2020.
//
// A history consistes of a sequence of ruptures and a sequence of intervals,
// spanning a given time range t_range_begin through t_range_end.
//
// In this implementation, ruptures are located only at interval endpoints, so that there are
// no ruptures inside an interval.  (Note that OEMagCompFnDiscFGH is capable of generating
// histories where smaller earthquakes can be interior to an interval.)  The implementation
// permits more than one rupture at the same time, although this should be rare.
//
// There are interval endpoints which do not correspond to any rupture, which are introduced
// so that the intervals can track the time-dependent magnitude of completeness.
//
// NOTATIONS
//
//   W(x) = (exp(x) - 1)/x     so that   W(0) = 1
//
//   W(x, y) = (exp(x*y) - 1)/x = W(x*y)*y     so that   W(0, y) = y   and   W(x, 0) = 0
//
//   beta = log(10) * b
//
//   v = log(10) * (alpha - b)
//
// In OEOmoriCalc there is an implementation of W called expm1dx.
//
// MAGNITUDE RANGES
//
// The parameter "a" is defined assuming that earthquake magnitudes lie in a range [mref, msup]:
//
//   mref = Reference magnitude = Minimum considered magnitude.
//   msup = Maximum considered magnitude.
//
// ETAS simulations are performed in a magnitude range [mag_min, mag_max]:
//
//   mag_min = Minimum magnitude used for ETAS simulation.
//   mag_max = Maximum magnitude used for ETAS simulation.
//
// A correction factor "Q" is applied to compensate for the difference in the two magnitude ranges.
// The value of Q is chosen to preserve the branching ratio, that is, the expected number of
// child earthquakes for each source earthquake.
//
//   Q = exp(v*(mref - mag_min)) * W(v, msup - mref) / W(v, mag_max - mag_min)
//
//   or   Q = (msup - mref)/(mag_max - mag_min)   in the case where alpha == b.
//
// MAGNITUDE OF COMPLETENESS
//
// Each rupture and each interval has an associated magnitude of completeness.
//
// For a rupture, the magnitude of completess is the value that obtained immediately before the
// rupture.  It should generally be the magnitude of completeness of the immediately preceding
// interval, but this implementation allows it to be different (which might occur, for example,
// if the immediately preceding interval was so short that it was elided).
//
// Parameter "magCat" is the catalog magnitude of completeness.  Conceptually it is the detection
// threshold during quiet times, but it may be increased to reduce the number of earthquakes
// considered.  Also, it should satisfy  magCat >= mag_min  so that we do not consider
// earthquakes outside the range of the ETAS simulation.
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
// (Note that is is possible to replace mref with mag_min in the above two formulas, if you also
// change the defintion of Q to be  W(v, msup - mref) / W(v, mag_max - mag_min) .)
//
// Consider a source interval from time s1 to s2.  An interval is assigned a productivity density
// ks, which represents productivity per unit time within the interval, and is assumed to be
// constant within the interval.  The total productivity of the interval is then  ks*(s2 - s1),
// which could be converted to an equivalent magnitude using the formula above.  The intensity
// function for the interval, acting as a source, for time t > s1, is
//
//   lambda(t, m) = ks * beta * (10^(-b*(m - mref))) * Integral(s = s1, s = min(t,s2); (t - s + c)^(-p) * ds)
//
// The total intensity function at time t is obtained by summing the above formulas over all
// ruptures at times s < t, and all intervals with beginning times s1 < t.
//
// PRODUCTIVITY DENSITY
//
// Consider a target interval from time t1 to t2.  We wish to compute the productivity density
// kt which is induced by prior sources.  Let mct be the magnitude of completeness of the
// interval.  The interval's productivity represents the total productivity of unobserved
// earthquakes in the interval with magnitude  mag_min <= m < mct.  (If mag_min >= mct then
// the interval's productivity is zero).  Suppose the interval is subjected to an intensity
// function of the form
//
//   lambda(t, m) = N(t) * (10^(-b*(m - mref)))
//
// where the function N(t) is independent of the target magnitude m.  Then the resulting
// productivity density is
//
//   kt = 10^(a + (alpha - b)*(mag_min - mref)) * Q * W(v, mct - mag_min) * Integral(t = t1, t = t2; N(t) * dt) / (t2 - t1)
//
// If the source is a rupture of magnitude ms at time s <= t1, then:
//
//   kt = k * beta * 10^(a + (alpha - b)*(mag_min - mref)) * Q * W(v, mct - mag_min)
//
//          * Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) / (t2 - t1)
//
// Notice that this contains two factors of 10^a.
//
// If the source is an interval from s1 to s2 <= t1, and source productivity density ks, then:
//
//   kt = ks * beta * 10^(a + (alpha - b)*(mag_min - mref)) * Q * W(v, mct - mag_min)
//
//           * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * ds * dt) / (t2 - t1)
//
// After summing the productivity from all prior ruptures and intervals, then there is an additional
// contribution from the target acting as its own source (source == target, t1 == s1, and t2 == s2):
//
//   kt = ks * beta * 10^(a + (alpha - b)*(mag_min - mref)) * Q * W(v, mct - mag_min)
//
//           * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * ds * dt) / (s2 - s1)
//
// LOG-LIKELIHOOD
//
// We base the log-likelihood calculation on the rate of earthquakes with magnitude m >= mc, where
// mc is the magnitude of completeness.  Because the the G-R distribution, this is
// lambda(t, mc)/beta.  One could alternatively use the rate of earthquakes with magnitudes between
// mc and some upper magnitude.
//
// For each target rupture at time t, there is a term of the form
//
//   log(lambda_total(t, mct)/beta)
//
// where mct is the magnitude of completeness of the target rupture.  This is to be summed over
// all target ruptures.  The argument of the log is a sum of contributions from all sources prior
// to t.  For each source rupture at time s <= t (excluding the target rupture itself):
//
//   k * (10^(-b*(mct - mref))) * ((t - s + c)^(-p))
//
//   k = 10^(a + alpha*(ms - mref)) * Q
//
// For each source interval at time s1 to s2, with s1 < t:
//
//   ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = min(t,s2); (t - s + c)^(-p) * ds)
//
// For each target interval at time t1 to t2, there is a term of the form
//
//   - Integral(t = t1, t = t2; lambda_total(t, mct)/beta * dt)
//
// where mct is the magnitude of completeness of the target interval.  This is to be summed over
// all target intervals.  Because it is a linear combination, the contributions of sources
// contributing to lambda_total can all be summed together rather than separately for each target.
// For each source rupture at time s < t2, the contribution to the integral is:
//
//   k * (10^(-b*(mct - mref))) * Integral(t = max(t1,s), t = t2; (t - s + c)^(-p) * dt)
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

public class OEDiscExtFit {

	//----- History -----

	// The rupture history to fit.

	private OEDiscHistory history;



	//----- Configuration -----

	// True to use intervals to fill in below magnitude of completeness.

	private boolean f_intervals;

	// True to calculate data needed for log-likelihood.

	private boolean f_likelihood;

	// Likelihood magnitude range option.

	private int lmr_opt;

	public static final int LMR_OPT_MCT_INFINITY = 1;		// From time-dependent magnitude of completeness to infinity.
	public static final int LMR_OPT_MCT_MAG_MAX = 2;		// From time-dependent magnitude of completeness to maximum simulation magnitude.
	public static final int LMR_OPT_MAGCAT_INFINITY = 3;	// From catalog magnitude of completeness to infinity.
	public static final int LMR_OPT_MAGCAT_MAG_MAX = 4;		// From catalog magnitude of completeness to maximum simulation magnitude.

	// Likelihood rupture range, the beginning and ending+1 of ruptures to include in likelihood calculation.

	private int like_rup_begin;
	private int like_rup_end;

	// Likelihood interval range, the beginning and ending+1 of intervals to include in likelihood calculation.

	private int like_int_begin;
	private int like_int_end;


	//----- Simulation parameters -----

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	private double mref;

	// Maximum considered magnitude, for parameter definition.

	private double msup;

	// The minimum magnitude to use for the simulation.

	private double mag_min;

	// The maximum magnitude to use for the simulation.

	private double mag_max;




	//-----  Per-Exponent Processing -----




	// Calculate W(x, y) = (exp(y*x) - 1)/x
	// The function avoids cancellation and divide-by-zero.
	// (Same code as OEOmoriCalc.expm1dx, reproduced here for readability.)

	public static double calc_w (double x, double y) {
		final double yx = y*x;
		if (Math.abs(yx) < 1.0e-15) {
			return y;
		}
		return Math.expm1(yx) / x;
	}



	// Produce a summary string describing a vector.

	public static String vec_summary_string (final double[] v) {
		if (v == null) {
			return "null";
		}
		return ("length = " + v.length);
	}




	// Inner class to process particular values of the (b, alpha) parameters.
	// It contains vectors with pre-computed exponentials of magnitudes,
	// so these values can be re-used with multiple Omori and productivity parameters.
	// Note: This class never modifies anything in the outer class, and so it is possible for
	// different threads to simultaneously use different objects.

	public class MagExponent {

		//----- Parameters -----

		// Gutenberg-Richter parameter.

		private double b;

		// ETAS intensity parameter.

		private double alpha;


		//----- Derived values -----

		// beta = log(10) * b

		private double beta;

		// v = log(10) * (alpha - b)

		private double v_term;

		// Q = exp(v*(mref - mag_min)) * W(v, msup - mref) / W(v, mag_max - mag_min)
		// where W(x, y) = (exp(x*y) - 1)/x

		private double q_correction;

		// Get the Q correction term.

		public final double get_q_correction () {
			return q_correction;
		}




		//----- Productivity and Gutenberg-Richter values -----




		// Productivity for each source rupture, assuming a == 0 and Q == 1.
		// The value for each rupture is
		//   10^(alpha*(ms - mref))
		// where ms is the magnitude of the rupture.
		// The length is history.rupture_count.
		// This is for source ruptures classified as non-mainshock, which are scaled by a.
		// It may contain offset value (ams -> -infinity) for mainshock ruptures.

		public double[] rup_azero_prod;

		// This is for source ruptures classified as mainshock, which are scaled by ams.
		// Scaling may be more complicated if there are non-zero offset values; in this
		// case the scale factor can be negative but must must always yied a positive
		// productivity when combined with the offset.

		public double[] rup_main_azero_prod;


		// Allocate the source productivity for each rupture, assuming a == 0 and Q == 1.

		private void alloc_rup_azero_prod () {
			final int rupture_count = history.rupture_count;

			rup_azero_prod = new double[rupture_count];
			rup_main_azero_prod = new double[rupture_count];

			return;
		}


		// Build the source productivity for each rupture, assuming a == 0 and Q == 1.

		private void build_rup_azero_prod () {
			final int rupture_count = history.rupture_count;
			final OERupture[] a_rupture_obj = history.a_rupture_obj;
			final int i_mainshock = history.i_mainshock;

			for (int n = 0; n < rupture_count; ++n) {
				if (n == i_mainshock) {
					rup_azero_prod[n] = 0.0;
					rup_main_azero_prod[n] = Math.pow(10.0, alpha*(a_rupture_obj[n].rup_mag - mref));
				} else {
					rup_azero_prod[n] = Math.pow(10.0, alpha*(a_rupture_obj[n].rup_mag - mref));
					rup_main_azero_prod[n] = 0.0;
				}
			}

			return;
		}




		// Partial likelihood for each target rupture, with unlimited or limited upper magnitude.
		// The value for each rupture is
		//   10^(-b*(mct - mref))
		// where mct is the magnitude of completeness of the target rupture.
		// If there is an upper magnitude mup, with mup > mct, then the value for each rupture is
		//   10^(-b*(mct - mref)) * (1 - exp(-beta*(mup - mct)))
		// The length is history.rupture_count.

		public double[] rup_part_like_unlim;


		// Allocate the partial likelihood for each target rupture, with unlimited or limited upper magnitude.

		private void alloc_rup_part_like_unlim () {
			final int rupture_count = history.rupture_count;

			rup_part_like_unlim = new double[rupture_count];

			return;
		}


		// Build the partial likelihood for each target rupture, with unlimited or limited upper magnitude.

		private void build_rup_part_like_unlim () {
			final int rupture_count = history.rupture_count;
			final OERupture[] a_rupture_obj = history.a_rupture_obj;
			final double magCat = history.magCat;

			switch (lmr_opt) {

			default:
				throw new IllegalArgumentException ("OEDiscExtFit.MagExponent.build_rup_part_like_unlim - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
			case LMR_OPT_MCT_INFINITY:
				// From time-dependent magnitude of completeness to infinity
				for (int n = 0; n < rupture_count; ++n) {
					rup_part_like_unlim[n] = Math.pow(10.0, b*(mref - a_rupture_obj[n].k_prod));
				}
				break;
			
			case LMR_OPT_MCT_MAG_MAX:
				// From time-dependent magnitude of completeness to maximum simulation magnitude
				// (We force the range to be at least 0.5 magnitude)
				for (int n = 0; n < rupture_count; ++n) {
					rup_part_like_unlim[n] = Math.pow(10.0, b*(mref - a_rupture_obj[n].k_prod)) * (- Math.expm1(beta*(Math.min(-0.5, a_rupture_obj[n].k_prod - mag_max))));
				}
				break;
			
			case LMR_OPT_MAGCAT_INFINITY:
				// From catalog magnitude of completeness to infinity
				for (int n = 0; n < rupture_count; ++n) {
					rup_part_like_unlim[n] = Math.pow(10.0, b*(mref - magCat));
				}
				break;
			
			case LMR_OPT_MAGCAT_MAG_MAX:
				// From catalog magnitude of completeness to maximum simulation magnitude
				// (We force the range to be at least 0.5 magnitude)
				for (int n = 0; n < rupture_count; ++n) {
					rup_part_like_unlim[n] = Math.pow(10.0, b*(mref - magCat)) * (- Math.expm1(beta*(Math.min(-0.5, magCat - mag_max))));
				}
				break;
			}

			return;
		}




		// Partial likelihood for each target interval, with unlimited or limited upper magnitude.
		// The value for each interval is
		//   (10^(-b*(mct - mref))) * (t2 - t1)
		// where mct is the magnitude of completeness of the interval, and where t1 and t2
		// are the begin and end times of the interval.
		// If there is an upper magnitude mup, with mup > mct, then the value for each rupture is
		//   (10^(-b*(mct - mref))) * (t2 - t1) * (1 - exp(-beta*(mup - mct)))
		// If an interval is not to be included in the likelihood calculation, then the value is zero.
		// The length is history.interval_count.

		public double[] int_part_like_unlim;


		// Allocate the partial likelihood for each target interval, with unlimited or limited upper magnitude.

		private void alloc_int_part_like_unlim () {
			final int interval_count = history.interval_count;

			int_part_like_unlim = new double[interval_count];

			return;
		}


		// Build the partial likelihood for each target interval, with unlimited or limited upper magnitude.

		private void build_int_part_like_unlim () {
			final int interval_count = history.interval_count;
			final double[] a_interval_mc = history.a_interval_mc;
			final double[] a_interval_time = history.a_interval_time;
			final double magCat = history.magCat;

			final int nlo = like_int_begin;
			final int nhi = like_int_end;

			switch (lmr_opt) {

			default:
				throw new IllegalArgumentException ("OEDiscExtFit.MagExponent.build_int_part_like_unlim - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
			case LMR_OPT_MCT_INFINITY:
				// From time-dependent magnitude of completeness to infinity
				for (int n = 0; n < interval_count; ++n) {
					if (nlo <= n && n < nhi) {
						int_part_like_unlim[n] = Math.pow(10.0, b*(mref - a_interval_mc[n])) * (a_interval_time[n + 1] - a_interval_time[n]);
					} else {
						int_part_like_unlim[n] = 0.0;
					}
				}
				break;
			
			case LMR_OPT_MCT_MAG_MAX:
				// From time-dependent magnitude of completeness to maximum simulation magnitude
				// (We force the range to be at least 0.5 magnitude)
				for (int n = 0; n < interval_count; ++n) {
					if (nlo <= n && n < nhi) {
						int_part_like_unlim[n] = Math.pow(10.0, b*(mref - a_interval_mc[n])) * (a_interval_time[n + 1] - a_interval_time[n]) * (- Math.expm1(beta*(Math.min(-0.5, a_interval_mc[n] - mag_max))));
					} else {
						int_part_like_unlim[n] = 0.0;
					}
				}
				break;
			
			case LMR_OPT_MAGCAT_INFINITY:
				// From catalog magnitude of completeness to infinity
				for (int n = 0; n < interval_count; ++n) {
					if (nlo <= n && n < nhi) {
						int_part_like_unlim[n] = Math.pow(10.0, b*(mref - magCat)) * (a_interval_time[n + 1] - a_interval_time[n]);
					} else {
						int_part_like_unlim[n] = 0.0;
					}
				}
				break;
			
			case LMR_OPT_MAGCAT_MAG_MAX:
				// From catalog magnitude of completeness to maximum simulation magnitude
				// (We force the range to be at least 0.5 magnitude)
				for (int n = 0; n < interval_count; ++n) {
					if (nlo <= n && n < nhi) {
						int_part_like_unlim[n] = Math.pow(10.0, b*(mref - magCat)) * (a_interval_time[n + 1] - a_interval_time[n]) * (- Math.expm1(beta*(Math.min(-0.5, magCat - mag_max))));
					} else {
						int_part_like_unlim[n] = 0.0;
					}
				}
				break;
			}

			return;
		}




		// Partial productivity for each target interval.
		// The value for each interval is
		//   beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		// where mct is the magnitude of completeness of the interval,
		// except that the value is zero if mag_min >= mct.
		// The length is history.interval_count.

		public double[] int_part_targ_prod;


		// Allocate the partial productivity for each target interval.

		private void alloc_int_part_targ_prod () {
			final int interval_count = history.interval_count;

			int_part_targ_prod = new double[interval_count];

			return;
		}


		// Build the partial productivity for each target interval.

		private void build_int_part_targ_prod () {
			final int interval_count = history.interval_count;
			final double[] a_interval_mc = history.a_interval_mc;
			final double magCat = history.magCat;

			final double c_mag = beta * Math.pow(10.0, (alpha - b)*(mag_min - mref));

			switch (lmr_opt) {

			default:
				throw new IllegalArgumentException ("OEDiscExtFit.MagExponent.build_int_part_targ_prod - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
			case LMR_OPT_MCT_INFINITY:
			case LMR_OPT_MCT_MAG_MAX:
				// Lower limit is time-dependent magnitude of completeness
				for (int n = 0; n < interval_count; ++n) {
					if (mag_min < a_interval_mc[n]) {
						int_part_targ_prod[n] = calc_w(v_term, a_interval_mc[n] - mag_min) * c_mag;
					} else {
						int_part_targ_prod[n] = 0.0;
					}
				}
				break;
			
			case LMR_OPT_MAGCAT_INFINITY:
			case LMR_OPT_MAGCAT_MAG_MAX:
				// Lower limit is catalog magnitude of completeness
				for (int n = 0; n < interval_count; ++n) {
					if (mag_min < magCat) {
						int_part_targ_prod[n] = calc_w(v_term, magCat - mag_min) * c_mag;
					} else {
						int_part_targ_prod[n] = 0.0;
					}
				}
				break;
			}

			return;
		}




		//----- Building -----




		// Clear all variables.

		public void clear () {
			b = 0.0;
			alpha = 0.0;

			beta = 0.0;
			v_term = 0.0;
			q_correction = 0.0;

			rup_azero_prod = null;
			rup_main_azero_prod = null;
			rup_part_like_unlim = null;
			int_part_like_unlim = null;
			int_part_targ_prod = null;

			return;
		}




		// Allocate all arrays.

		private void mexp_alloc () {

			// Allocate all the arrays that we need

			alloc_rup_azero_prod();
			alloc_rup_part_like_unlim();
			alloc_int_part_like_unlim();
			alloc_int_part_targ_prod();
		
			return;
		}




		// Fill in all arrays, using the given magnitude parameters.
		// Note: A MagExponent object can be used multiple times,
		// by calling mexp_build for each new pair of magnitude parameters.

		public void mexp_build (double b, double alpha) {

			// Save the magnitude parameters

			this.b = b;
			this.alpha = alpha;

			// Calculate the derived values

			beta = b * C_LOG_10;
			v_term = (alpha - b) * C_LOG_10;
			q_correction = Math.exp(v_term*(mref - mag_min)) * calc_w(v_term, msup - mref) / calc_w(v_term, mag_max - mag_min);

			// Build all the arrays that we need

			build_rup_azero_prod();
			build_rup_part_like_unlim();
			build_int_part_like_unlim();
			build_int_part_targ_prod();
		
			return;
		}




		// Constructor allocates the arrays.

		public MagExponent () {
			clear();
			mexp_alloc();
		}




		// Display our contents.

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append ("OEDiscExtFit.MagExponent:" + "\n");

			result.append ("b = "            + b            + "\n");
			result.append ("alpha = "        + alpha        + "\n");

			result.append ("beta = "         + beta         + "\n");
			result.append ("v_term = "       + v_term       + "\n");
			result.append ("q_correction = " + q_correction + "\n");

			result.append ("rup_azero_prod: "      + vec_summary_string(rup_azero_prod)      + "\n");
			result.append ("rup_main_azero_prod: " + vec_summary_string(rup_main_azero_prod) + "\n");
			result.append ("rup_part_like_unlim: " + vec_summary_string(rup_part_like_unlim) + "\n");
			result.append ("int_part_like_unlim: " + vec_summary_string(int_part_like_unlim) + "\n");
			result.append ("int_part_targ_prod: "  + vec_summary_string(int_part_targ_prod)  + "\n");

			return result.toString();
		}

	}




	// List of MagExponent objects available for re-use.
	// Threading: Access must be synchronized to allow access from multiple threads.

	private ArrayDeque<MagExponent> mexp_list;

	// Handle object to obtain or allocate a MagExponent.
	// The handle should be created in a try-with-resources.

	public class MagExponentHandle implements AutoCloseable {

		// The contained MagExponent.

		private MagExponent mexp;

		// Get the contained MagExponent.

		public final MagExponent get_mexp () {
			return mexp;
		}

		// Constructor obtains or allocates the MagExponent.

		public MagExponentHandle () {
			synchronized (mexp_list) {
				mexp = mexp_list.pollLast();
			}
			if (mexp == null) {
				mexp = new MagExponent();
			}
		}

		// Closing puts the MagExponent on the list for re-use.

		@Override
		public void close () {
			if (mexp != null) {
				synchronized (mexp_list) {
					mexp_list.addLast (mexp);
				}
				mexp = null;
			}
			return;
		}


		//----- Forwarding functions -----


		// Get the Q correction term.

		public final double get_q_correction () {
			return mexp.get_q_correction();
		}

		// Fill in all arrays, using the given magnitude parameters.
		// Note: A MagExponent object can be used multiple times,
		// by calling mexp_build for each new pair of magnitude parameters.

		public final void mexp_build (double b, double alpha) {
			mexp.mexp_build (b, alpha);
			return;
		}

		// Display our contents.

		@Override
		public String toString() {
			return mexp.toString();
		}
	
	}

	// Make a handle object.

	public MagExponentHandle make_MagExponentHandle () {
		return new MagExponentHandle();
	}







	//----- Per-Omori Processing -----




//	// Apply a triangular matrix to a vector.
//	// Performs the operation:
//	//   y[i] = SUM(m[i][j] * x[j])
//	// where the sum is over j satisfying 0 <= j < m[i].length.
//	// The operation is performed for i satisfying 0 <= i <= m.length.
//	// This is the matrix operation
//	//   y = m * x
//
//	public static void tri_mat_apply (final double[] y, final double[][] m, final double[] x) {
//		final int i_top = m.length;
//		for (int i = 0; i < i_top; ++i) {
//			double sum = 0.0;
//			final double[] row = m[i];
//			final int j_top = row.length;
//			for (int j = 0; j < j_top; ++j) {
//				sum += (row[j] * x[j]);
//			}
//			y[i] = sum;
//		}
//		return;
//	}
//
//
//
//
//	// Apply a triangular matrix to a vector, with a scale factor.
//	// Performs the operation:
//	//   y[i] = SUM(m[i][j] * x[j]) * s
//	// where the sum is over j satisfying 0 <= j < m[i].length.
//	// The operation is performed for i satisfying 0 <= i <= m.length.
//	// This is the matrix operation
//	//   y = s * (m * x)
//
//	public static void tri_mat_apply_scale (final double[] y, final double[][] m, final double[] x, final double s) {
//		final int i_top = m.length;
//		for (int i = 0; i < i_top; ++i) {
//			double sum = 0.0;
//			final double[] row = m[i];
//			final int j_top = row.length;
//			for (int j = 0; j < j_top; ++j) {
//				sum += (row[j] * x[j]);
//			}
//			y[i] = sum * s;
//		}
//		return;
//	}
//
//
//
//
//	// Apply a triangular matrix to a vector, with a diagonal matrix factor.
//	// Performs the operation:
//	//   y[i] = SUM(m[i][j] * x[j]) * d[i]
//	// where the sum is over j satisfying 0 <= j < m[i].length.
//	// The operation is performed for i satisfying 0 <= i <= m.length.
//	// This is the matrix operation
//	//   y = diag(d) * (m * x)
//
//	public static void tri_mat_apply_diag (final double[] y, final double[][] m, final double[] x, final double[] d) {
//		final int i_top = m.length;
//		for (int i = 0; i < i_top; ++i) {
//			double sum = 0.0;
//			final double[] row = m[i];
//			final int j_top = row.length;
//			for (int j = 0; j < j_top; ++j) {
//				sum += (row[j] * x[j]);
//			}
//			y[i] = sum * d[i];
//		}
//		return;
//	}
//
//
//
//
//	// Apply a triangular matrix to two vectors, with a diagonal matrix factor.
//	// Performs the operation:
//	//   y1[i] = SUM(m[i][j] * x1[j]) * d[i]
//	//   y2[i] = SUM(m[i][j] * x2[j]) * d[i]
//	// where the sum is over j satisfying 0 <= j < m[i].length.
//	// The operation is performed for i satisfying 0 <= i <= m.length.
//	// This is the matrix operation
//	//   y1 = diag(d) * (m * x1)
//	//   y2 = diag(d) * (m * x2)
//
//	public static void tri_mat_dual_apply_diag (final double[] y1, final double[] y2, final double[][] m, final double[] x1, final double[] x2, final double[] d) {
//		final int i_top = m.length;
//		for (int i = 0; i < i_top; ++i) {
//			double sum1 = 0.0;
//			double sum2 = 0.0;
//			final double[] row = m[i];
//			final int j_top = row.length;
//			for (int j = 0; j < j_top; ++j) {
//				sum1 += (row[j] * x1[j]);
//				sum2 += (row[j] * x2[j]);
//			}
//			y1[i] = sum1 * d[i];
//			y2[i] = sum2 * d[i];
//		}
//		return;
//	}
//
//
//
//
//	// Apply a triangular matrix to a vector, then perform a dot product.
//	// Performs the operation:
//	//   y[0] = SUM(m[i][j] * x[j] * d[i])
//	// where the sum is over i satisfying 0 <= i <= m.length and j satisfying 0 <= j < m[i].length.
//	// This is the matrix operation
//	//   y = d . (m * x)
//
//	public static void tri_mat_apply_dot (final double[] y, final double[][] m, final double[] x, final double[] d) {
//		final int i_top = m.length;
//		double total = 0.0;
//		for (int i = 0; i < i_top; ++i) {
//			double sum = 0.0;
//			final double[] row = m[i];
//			final int j_top = row.length;
//			for (int j = 0; j < j_top; ++j) {
//				sum += (row[j] * x[j]);
//			}
//			total += (sum * d[i]);
//		}
//		y[0] = total;
//		return;
//	}
//
//
//
//
//	// Apply a triangular matrix to two vectors, then perform a dot product.
//	// Performs the operation:
//	//   y1[0] = SUM(m[i][j] * x1[j] * d[i])
//	//   y2[0] = SUM(m[i][j] * x2[j] * d[i])
//	// where the sum is over i satisfying 0 <= i <= m.length and j satisfying 0 <= j < m[i].length.
//	// This is the matrix operation
//	//   y1 = d . (m * x1)
//	//   y2 = d . (m * x2)
//
//	public static void tri_mat_dual_apply_dot (final double[] y1, final double[] y2, final double[][] m, final double[] x1, final double[] x2, final double[] d) {
//		final int i_top = m.length;
//		double total1 = 0.0;
//		double total2 = 0.0;
//		for (int i = 0; i < i_top; ++i) {
//			double sum1 = 0.0;
//			double sum2 = 0.0;
//			final double[] row = m[i];
//			final int j_top = row.length;
//			for (int j = 0; j < j_top; ++j) {
//				sum1 += (row[j] * x1[j]);
//				sum2 += (row[j] * x2[j]);
//			}
//			total1 += (sum1 * d[i]);
//			total2 += (sum2 * d[i]);
//		}
//		y1[0] = total1;
//		y2[0] = total2;
//		return;
//	}




	// Produce a summary string describing a triangular matrix.

	public static String tri_mat_summary_string (final double[][] m) {
		if (m == null) {
			return "null";
		}
		if (m.length == 0) {
			return "rows = 0";
		}
		int min_cols = m[0].length;
		int max_cols = m[0].length;
		for (int i = 1; i < m.length; ++i) {
			if (m[i].length < min_cols) {
				min_cols = m[i].length;
			}
			else if (m[i].length > max_cols) {
				max_cols = m[i].length;
			}
		}
		return ("rows = " + m.length + ", cols = " + min_cols + " - " + max_cols);
	}




	// True to build Omori matrix for rupture target and rupture source.

	private boolean f_omat_rup_targ_rup_src;

	// True to build Omori matrix for rupture target and interval source.

	private boolean f_omat_rup_targ_int_src;

	// True to build Omori matrix for interval target and rupture source.

	private boolean f_omat_int_targ_rup_src;

	// True to build Omori matrix for interval target and interval source.

	private boolean f_omat_int_targ_int_src;




	// Inner class to process particular values of the (p, c) parameters.
	// It contains matrices for values of the Omori function and integrals, so those values
	// can be re-used with multiple productivity parameters.
	// The memory requirements of this class are large.
	// Note: This class never modifies anything in the outer class, and so it is possible for
	// different threads to simultaneously use different objects.

	public class OmoriMatrix {

		//----- Parameters -----

		// Omori exponent parameter.

		public double p;

		// Omori offset parameter.

		public double c;




		//----- Matrices -----




		// Matrix of Omori values for a rupture target and rupture source.
		// omat_rup_targ_rup_src[i_t_rup][i_s_rup] is the Omori function for the source rupture
		// selected by index i_s_rup, acting on the target rupture selected by index i_t_rup,
		// which is
		//
		//   (t - s + c)^(-p)
		//
		//   t = a_rupture_obj[i_t_rup].t_day
		//   s = a_rupture_obj[i_s_rup].t_day
		//
		// This is defined for  0 <= i_s_rup < i_t_rup < rupture_count  because the source must
		// precede the target, so this is a triangular array with
		//
		//   omat_rup_targ_rup_src.length = rupture_count
		//   omat_rup_targ_rup_src[i_t_rup].length = i_t_rup
		//
		// This array is used to calculate the log-likelihood per-rupture terms due to rupture sources.
		// This array is allocated if likelihoods are being calculated.

		private double[][] omat_rup_targ_rup_src;


		// Function to allocate the matrix of Omori values for a rupture target and rupture source.

		private void alloc_omat_rup_targ_rup_src () {

			final int rupture_count = history.rupture_count;

			// Allocate the triangular array

			omat_rup_targ_rup_src = new double[rupture_count][];
			for (int i_t_rup = 0; i_t_rup < rupture_count; ++i_t_rup) {
				omat_rup_targ_rup_src[i_t_rup] = new double[i_t_rup];
			}

			return;
		}


		// Function to build the matrix of Omori values for a rupture target and rupture source.

		private void build_omat_rup_targ_rup_src () {

			final int rupture_count = history.rupture_count;
			final OERupture[] a_rupture_obj = history.a_rupture_obj;

			// Build the triangular array

			for (int i_t_rup = 0; i_t_rup < rupture_count; ++i_t_rup) {

				// Get the time for this target

				final double t_plus_c = a_rupture_obj[i_t_rup].t_day + c;

				// Build the row containing all sources for this target

				final double[] omat_row = omat_rup_targ_rup_src[i_t_rup];
				for (int i_s_rup = 0; i_s_rup < i_t_rup; ++i_s_rup) {

					// Get the time for this source

					final double s = a_rupture_obj[i_s_rup].t_day;

					// Omori value
				
					omat_row[i_s_rup] = Math.pow(t_plus_c - s, -p);
				}
			}

			return;
		}


		// Function to apply the matrix.
		// Parameters:
		//  y1 = Target vector #1, length = rupture_count.
		//  y2 = Target vector #2, length = rupture_count.
		//  x1 = Source vector #1, length = rupture_count.
		//  x2 = Source vector #2, length = rupture_count.
		//  d = Target scaling vector, length = rupture_count.
		// Performs the operation (with m denoting omat_rup_targ_rup_src):
		//   y1[i] = SUM(m[i][j] * x1[j]) * d[i]
		//   y2[i] = SUM(m[i][j] * x2[j]) * d[i]
		// where the sum is over j satisfying 0 <= j < m[i].length.
		// The operation is performed for i satisfying 0 <= i <= m.length.
		// This is the matrix operation
		//   y1 = diag(d) * (m * x1)
		//   y2 = diag(d) * (m * x2)

		public final void apply_omat_rup_targ_rup_src (
				final double[] y1,
				final double[] y2,
				final double[] x1,
				final double[] x2,
				final double[] d ) {

			// Zero-matrix case

			if (omat_rup_targ_rup_src == null) {
				final int ii_top = history.rupture_count;
				for (int ii = 0; ii < ii_top; ++ii) {
					y1[ii] = 0.0;
					y2[ii] = 0.0;
				}
				return;
			}

			// Non-zero matrix

			final int i_top = omat_rup_targ_rup_src.length;
			for (int i = 0; i < i_top; ++i) {
				double sum1 = 0.0;
				double sum2 = 0.0;
				final double[] row = omat_rup_targ_rup_src[i];
				final int j_top = row.length;
				for (int j = 0; j < j_top; ++j) {
					sum1 += (row[j] * x1[j]);
					sum2 += (row[j] * x2[j]);
				}
				y1[i] = sum1 * d[i];
				y2[i] = sum2 * d[i];
			}
			return;
		}




		// Matrix of Omori values for a rupture target and interval source.
		// omat_rup_targ_int_src[i_t_rup][i_s_int] is the Omori function for the source interval
		// selected by index i_s_int, acting on the target rupture selected by index i_t_rup,
		// which is
		//
		//   Integral(s = s1, s = s2; (t - s + c)^(-p) * ds)
		//
		//   t = a_interval_time[a_rupture_int_time_index[i_t_rup]]
		//   s1 = a_interval_time[i_s_int]
		//   s2 = a_interval_time[i_s_int + 1]
		//
		// This is defined for  0 <= i_t_rup < rupture_count  and where the source interval lies
		// before the target rupture, so  0 <= i_s_int < a_rupture_int_time_index[i_t_rup] .
		//
		//   omat_rup_targ_int_src.length = rupture_count
		//   omat_rup_targ_int_src[i_t_rup].length = a_rupture_int_time_index[i_t_rup]
		//
		// This array is used to calculate the log-likelihood per-rupture terms due to interval sources.
		// This array is allocated if likelihoods are being calculated and interval sources are being used.

		private double[][] omat_rup_targ_int_src;


		// Function to allocate the matrix of Omori values for a rupture target and interval source.

		private void alloc_omat_rup_targ_int_src () {

			final int rupture_count = history.rupture_count;
			final int[] a_rupture_int_time_index = history.a_rupture_int_time_index;

			// Allocate the triangular array

			omat_rup_targ_int_src = new double[rupture_count][];
			for (int i_t_rup = 0; i_t_rup < rupture_count; ++i_t_rup) {
				omat_rup_targ_int_src[i_t_rup] = new double[a_rupture_int_time_index[i_t_rup]];
			}

			return;
		}


		// Function to build the matrix of Omori values for a rupture target and interval source.

		private void build_omat_rup_targ_int_src () {

			final int rupture_count = history.rupture_count;
			final double[] a_interval_time = history.a_interval_time;

			// Build the triangular array

			for (int i_t_rup = 0; i_t_rup < rupture_count; ++i_t_rup) {

				// Get the time for this target, using the matching interval time

				final double[] omat_row = omat_rup_targ_int_src[i_t_rup];
				final int row_len = omat_row.length;
				final double t_plus_c = a_interval_time[row_len] + c;

				// Build the row containing all sources for this target

				for (int i_s_int = 0; i_s_int < row_len; ++i_s_int) {

					// Get the time for this source

					final double s1 = a_interval_time[i_s_int];
					final double s2 = a_interval_time[i_s_int + 1];

					// Omori value
				
					omat_row[i_s_int] = OEOmoriCalc.omext_single_integral (p, t_plus_c - s2, s2 - s1);
				}
			}

			return;
		}


		// Function to apply the matrix.
		// Parameters:
		//  y1 = Target vector #1, length = rupture_count.
		//  y2 = Target vector #2, length = rupture_count.
		//  x1 = Source vector #1, length = interval_count.
		//  x2 = Source vector #2, length = interval_count.
		//  o1 = Target offset vector #1, length = rupture_count.
		//  o2 = Target offset vector #2, length = rupture_count.
		//  d = Target scaling vector, length = rupture_count.
		// Performs the operation (with m denoting omat_rup_targ_int_src):
		//   y1[i] = SUM(m[i][j] * x1[j]) * d[i] + o1[i]
		//   y2[i] = SUM(m[i][j] * x2[j]) * d[i] + o2[i]
		// where the sum is over j satisfying 0 <= j < m[i].length.
		// The operation is performed for i satisfying 0 <= i <= m.length.
		// This is the matrix operation
		//   y1 = diag(d) * (m * x1) + o1
		//   y2 = diag(d) * (m * x2) + o2

		public final void apply_omat_rup_targ_int_src (
				final double[] y1,
				final double[] y2,
				final double[] x1,
				final double[] x2,
				final double[] o1,
				final double[] o2,
				final double[] d ) {

			// Zero-matrix case

			if (omat_rup_targ_int_src == null) {
				final int ii_top = history.rupture_count;
				for (int ii = 0; ii < ii_top; ++ii) {
					y1[ii] = o1[ii];
					y2[ii] = o2[ii];
				}
				return;
			}

			// Non-zero matrix

			final int i_top = omat_rup_targ_int_src.length;
			for (int i = 0; i < i_top; ++i) {
				double sum1 = 0.0;
				double sum2 = 0.0;
				final double[] row = omat_rup_targ_int_src[i];
				final int j_top = row.length;
				for (int j = 0; j < j_top; ++j) {
					sum1 += (row[j] * x1[j]);
					sum2 += (row[j] * x2[j]);
				}
				y1[i] = sum1 * d[i] + o1[i];
				y2[i] = sum2 * d[i] + o2[i];
			}
			return;
		}




		// Matrix of Omori values for an interval target and rupture source.
		// omat_int_targ_rup_src[i_t_int][i_s_rup] is the Omori function for the source rupture
		// selected by index i_s_rup, acting on the target interval selected by index i_t_int,
		// which is
		//
		//   Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) / (t2 - t1)
		//
		//   t1 = a_interval_time[i_t_int]
		//   t2 = a_interval_time[i_t_int + 1]
		//   s = a_interval_time[a_rupture_int_time_index[i_s_rup]]
		//
		// This is defined for  0 <= i_t_int < interval_count  and where the source rupture lies
		// before the target interval, so  a_rupture_int_time_index[i_s_rup] <= i_t_int .
		//
		//   omat_int_targ_rup_src.length = interval_count
		//   omat_int_targ_rup_src[i_t_int].length = maximum n such that  a_rupture_int_time_index[n-1] <= i_t_int
		//
		// This array is used to calculate the productivity of intervals due to rupture sources,
		// and the log-likelihood integral term due to rupture sources.
		// This array is allocated if likelihoods are being calculated or interval sources are being used.

		private double[][] omat_int_targ_rup_src;


		// Function to allocate the matrix of Omori values for an interval target and rupture source.

		private void alloc_omat_int_targ_rup_src () {

			final int rupture_count = history.rupture_count;
			final int[] a_rupture_int_time_index = history.a_rupture_int_time_index;
			final int interval_count = history.interval_count;

			// Allocate the triangular array

			omat_int_targ_rup_src = new double[interval_count][];
			int n = 0;
			for (int i_t_int = 0; i_t_int < interval_count; ++i_t_int) {
				while (n < rupture_count && a_rupture_int_time_index[n] <= i_t_int) {
					++n;
				}
				omat_int_targ_rup_src[i_t_int] = new double[n];
			}

			return;
		}


		// Function to build the matrix of Omori values for an interval target and rupture source.

		private void build_omat_int_targ_rup_src () {

			final int[] a_rupture_int_time_index = history.a_rupture_int_time_index;
			final int interval_count = history.interval_count;
			final double[] a_interval_time = history.a_interval_time;

			// Build the triangular array

			for (int i_t_int = 0; i_t_int < interval_count; ++i_t_int) {

				// Get the time for this target, using the matching interval time

				final double[] omat_row = omat_int_targ_rup_src[i_t_int];
				final int row_len = omat_row.length;

				final double t1_plus_c = a_interval_time[i_t_int] + c;
				final double t2_minus_t1 = a_interval_time[i_t_int + 1] - a_interval_time[i_t_int];

				// Build the row containing all sources for this target

				for (int i_s_rup = 0; i_s_rup < row_len; ++i_s_rup) {

					// Get the time for this source, using the matching interval time

					final double s = a_interval_time[a_rupture_int_time_index[i_s_rup]];

					// Omori value
				
					omat_row[i_s_rup] = OEOmoriCalc.omext_single_density_integral (p, t1_plus_c - s, t2_minus_t1);
				}
			}

			return;
		}


		// Function to apply the matrix.
		// Parameters:
		//  y1 = Target vector #1, length = interval_count.
		//  y2 = Target vector #2, length = interval_count.
		//  z1 = Target scalar #1, length = 1.
		//  z2 = Target scalar #2, length = 1.
		//  x1 = Source vector #1, length = rupture_count.
		//  x2 = Source vector #2, length = rupture_count.
		//  dy = Target scaling vector for y, length = interval_count.
		//  dz = Target scaling vector for z, length = interval_count.
		// Performs the operations (with m denoting omat_int_targ_rup_src):
		//   y1[i] = SUM(m[i][j] * x1[j]) * dy[i]
		//   y2[i] = SUM(m[i][j] * x2[j]) * dy[i]
		// where the sum is over j satisfying 0 <= j < m[i].length, for i satisfying 0 <= i <= m.length.
		//   z1[0] = SUM(m[i][j] * x1[j] * dz[i])
		//   z2[0] = SUM(m[i][j] * x2[j] * dz[i])
		// where the sum is over i satisfying 0 <= i <= m.length and j satisfying 0 <= j < m[i].length.
		// This is the matrix operation
		//   y1 = diag(dy) * (m * x1)
		//   y2 = diag(dy) * (m * x2)
		//   z1 = dz . (m * x1)
		//   z2 = dz . (m * x2)

		public final void apply_omat_int_targ_rup_src (
				final double[] y1,
				final double[] y2,
				final double[] z1,
				final double[] z2,
				final double[] x1,
				final double[] x2,
				final double[] dy,
				final double[] dz ) {

			// Zero-matrix case

			if (omat_int_targ_rup_src == null) {
				final int ii_top = history.interval_count;
				for (int ii = 0; ii < ii_top; ++ii) {
					y1[ii] = 0.0;
					y2[ii] = 0.0;
				}
				z1[0] = 0.0;
				z2[0] = 0.0;
				return;
			}

			// Non-zero matrix

			final int i_top = omat_int_targ_rup_src.length;
			double total1 = 0.0;
			double total2 = 0.0;
			for (int i = 0; i < i_top; ++i) {
				double sum1 = 0.0;
				double sum2 = 0.0;
				final double[] row = omat_int_targ_rup_src[i];
				final int j_top = row.length;
				for (int j = 0; j < j_top; ++j) {
					sum1 += (row[j] * x1[j]);
					sum2 += (row[j] * x2[j]);
				}
				y1[i] = sum1 * dy[i];
				y2[i] = sum2 * dy[i];
				total1 += (sum1 * dz[i]);
				total2 += (sum2 * dz[i]);
			}
			z1[0] = total1;
			z2[0] = total2;
			return;
		}




		// Matrix of Omori values for an interval target and interval source.
		// omat_int_targ_int_src[i_t_int][i_s_int] is the Omori function for the source interval
		// selected by index i_s_int, acting on the target interval selected by index i_t_int,
		// which is
		//
		//   Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * ds * dt) / (t2 - t1)
		//
		//   t1 = a_interval_time[i_t_int]
		//   t2 = a_interval_time[i_t_int + 1]
		//   s1 = a_interval_time[i_s_int]
		//   s2 = a_interval_time[i_s_int + 1]
		//
		// This is defined for  0 <= i_s_int < i_t_int < interval_count  because the source interval
		// must lie before the target interval.  So this is a triangular array with
		//
		//   omat_int_targ_int_src.length = interval_count
		//   omat_int_targ_int_src[i_t_int].length = i_t_int
		//
		// In addition, omat_self_int_src[i_s_int] is the Omori function for the interval
		// selected by index i_s_int, acting on itself as both target and source, which is
		//
		//   Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * dt * ds) / (s2 - s1)
		//
		//   omat_self_int_src.length = interval_count
		//
		// This array is allocated if ...
		// This array is used to calculate the productivity of intervals due to interval sources,
		// and the log-likelihood integral term due to interval sources.
		// This array is allocated if interval sources are being used.

		private double[][] omat_int_targ_int_src;

		private double[] omat_self_int_src;


		// Function to allocate the matrix of Omori values for an interval target and interval source.

		private void alloc_omat_int_targ_int_src () {

			final int interval_count = history.interval_count;

			// Allocate the triangular array

			omat_int_targ_int_src = new double[interval_count][];
			for (int i_t_int = 0; i_t_int < interval_count; ++i_t_int) {
				omat_int_targ_int_src[i_t_int] = new double[i_t_int];
			}

			// Allocate the self array

			omat_self_int_src = new double[interval_count];

			return;
		}


		// Function to build the matrix of Omori values for an interval target and interval source.

		private void build_omat_int_targ_int_src () {

			final int interval_count = history.interval_count;
			final double[] a_interval_time = history.a_interval_time;

			// Build the triangular array

			for (int i_t_int = 0; i_t_int < interval_count; ++i_t_int) {

				// Get the time for this target, using the matching interval time

				final double[] omat_row = omat_int_targ_int_src[i_t_int];

				final double t1_plus_c = a_interval_time[i_t_int] + c;
				final double t2_minus_t1 = a_interval_time[i_t_int + 1] - a_interval_time[i_t_int];

				// Build the row containing all sources for this target

				for (int i_s_int = 0; i_s_int < i_t_int; ++i_s_int) {

					// Get the time for this source

					final double s1 = a_interval_time[i_s_int];
					final double s2 = a_interval_time[i_s_int + 1];

					// Omori value
				
					omat_row[i_s_int] = OEOmoriCalc.omext_double_density_integral (p, t1_plus_c - s2, s2 - s1, t2_minus_t1);
				}

				// Build the self entry

				omat_self_int_src[i_t_int] = OEOmoriCalc.omext_self_double_density_integral (p, c, t2_minus_t1);
			}

			return;
		}


		// Function to apply the matrix.
		// Parameters:
		//  y1 = Target vector #1, length = interval_count.
		//  y2 = Target vector #2, length = interval_count.
		//  z1 = Target scalar #1, length = 1.
		//  z2 = Target scalar #2, length = 1.
		//  x1 = Source vector #1, length = interval_count.
		//  x2 = Source vector #2, length = interval_count.
		//  o1 = Source scalar offset #1, length = 1.
		//  o2 = Source scalar offset #2, length = 1.
		//  dy = Target scaling vector for y, length = interval_count.
		//  dz = Target scaling vector for z, length = interval_count.
		//  s = Scale factor.
		// Performs the operations (with m denoting omat_int_targ_int_src and v denoting omat_self_int_src):
		//   y1[i] = (SUM(m[i][j] * y1[j]) * dy[i] + x1[i]) * s * (1 + v[i] * dy[i] * s);
		//   y2[i] = (SUM(m[i][j] * y2[j]) * dy[i] + x2[i]) * s * (1 + v[i] * dy[i] * s);
		// where the sum is over j satisfying 0 <= j < m[i].length, for i satisfying 0 <= i <= m.length.
		// Because the formulas are a recurrence, they must be evaluated sequentially in increasing i.
		//   z1[0] = SUM((SUM(m[i][j] * y1[j]) + y1[i] * v[i]) * dz[i]) + o1[0]
		//   z2[0] = SUM((SUM(m[i][j] * y2[j]) + y2[i] * v[i]) * dz[i]) + o2[0]
		// where the inner sum is over j satisfying 0 <= j < m[i].length, and the outer sum is
		// over i satisfying 0 <= i <= m.length.  Because the formulas refer to y1[i] and y2[i],
		// the y values must be computed first.

		public final void apply_omat_int_targ_int_src (
				final double[] y1,
				final double[] y2,
				final double[] z1,
				final double[] z2,
				final double[] x1,
				final double[] x2,
				final double[] o1,
				final double[] o2,
				final double[] dy,
				final double[] dz,
				final double s) {

			// Zero-matrix case

			if (omat_int_targ_int_src == null) {
				final int ii_top = history.interval_count;
				for (int ii = 0; ii < ii_top; ++ii) {
					y1[ii] = x1[ii] * s;
					y2[ii] = x2[ii] * s;
				}
				z1[0] = o1[0];
				z2[0] = o2[0];
				return;
			}

			// Non-zero matrix

			final int i_top = omat_int_targ_int_src.length;

			// Accumulators across all targets for scalar output

			double total1 = 0.0;
			double total2 = 0.0;

			// For each target...

			for (int i = 0; i < i_top; ++i) {

				// Propagate from prior outputs

				double sum1 = 0.0;
				double sum2 = 0.0;

				final double[] row = omat_int_targ_int_src[i];
				final int j_top = row.length;

				for (int j = 0; j < j_top; ++j) {
					sum1 += (row[j] * y1[j]);
					sum2 += (row[j] * y2[j]);
				}

				// Vector output

				final double self = omat_self_int_src[i];

				y1[i] = (sum1 * dy[i] + x1[i]) * s * (self * dy[i] * s + 1.0);
				y2[i] = (sum2 * dy[i] + x2[i]) * s * (self * dy[i] * s + 1.0);

				// Scalar output

				total1 += ((y1[i] * self + sum1) * dz[i]);
				total2 += ((y2[i] * self + sum2) * dz[i]);
			}

			// Total scalar output

			z1[0] = total1 + o1[0];
			z2[0] = total2 + o2[0];
			return;
		}




		//----- Building -----




		// Clear all variables.

		public void clear () {
			p = 0.0;
			c = 0.0;

			omat_rup_targ_rup_src = null;
			omat_rup_targ_int_src = null;
			omat_int_targ_rup_src = null;
			omat_int_targ_int_src = null;
			omat_self_int_src = null;

			return;
		}




		// Allocate all matrices.

		private void omat_alloc () {

			// Allocate all the matrices that we need

			if (f_omat_rup_targ_rup_src) {
				alloc_omat_rup_targ_rup_src();
			}

			if (f_omat_rup_targ_int_src) {
				alloc_omat_rup_targ_int_src();
			}

			if (f_omat_int_targ_rup_src) {
				alloc_omat_int_targ_rup_src();
			}

			if (f_omat_int_targ_int_src) {
				alloc_omat_int_targ_int_src();
			}
		
			return;
		}




		// Fill in all matrices, using the given Omori parameters.
		// Note: An OmoriMatrix object can be used multiple times,
		// by calling omat_build for each new pair of Omori parameters.

		public void omat_build (double p, double c) {

			// Save the Omori parameters

			this.p = p;
			this.c = c;

			// Build all the matrices that we need

			if (f_omat_rup_targ_rup_src) {
				build_omat_rup_targ_rup_src();
			}

			if (f_omat_rup_targ_int_src) {
				build_omat_rup_targ_int_src();
			}

			if (f_omat_int_targ_rup_src) {
				build_omat_int_targ_rup_src();
			}

			if (f_omat_int_targ_int_src) {
				build_omat_int_targ_int_src();
			}
		
			return;
		}




		// Constructor allocates the matrices.

		public OmoriMatrix () {
			clear();
			omat_alloc();
		}




		// Display our contents.

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append ("OEDiscExtFit.OmoriMatrix:" + "\n");

			result.append ("p = " + p + "\n");
			result.append ("c = " + c + "\n");

			result.append ("omat_rup_targ_rup_src: " + tri_mat_summary_string(omat_rup_targ_rup_src) + "\n");
			result.append ("omat_rup_targ_int_src: " + tri_mat_summary_string(omat_rup_targ_int_src) + "\n");
			result.append ("omat_int_targ_rup_src: " + tri_mat_summary_string(omat_int_targ_rup_src) + "\n");
			result.append ("omat_int_targ_int_src: " + tri_mat_summary_string(omat_int_targ_int_src) + "\n");
			result.append ("omat_self_int_src: "     + vec_summary_string(omat_self_int_src)         + "\n");

			return result.toString();
		}

	}




	// List of OmoriMatrix objects available for re-use.
	// Threading: Access must be synchronized to allow access from multiple threads.

	private ArrayDeque<OmoriMatrix> omat_list;

	// Handle object to obtain or allocate an OmoriMatrix.
	// The handle should be created in a try-with-resources.

	public class OmoriMatrixHandle implements AutoCloseable {

		// The contained OmoriMatrix.

		private OmoriMatrix omat;

		// Get the contained OmoriMatrix.

		public final OmoriMatrix get_omat () {
			return omat;
		}

		// Constructor obtains or allocates the OmoriMatrix.

		public OmoriMatrixHandle () {
			synchronized (omat_list) {
				omat = omat_list.pollLast();
			}
			if (omat == null) {
				omat = new OmoriMatrix();
			}
		}

		// Closing puts the OmoriMatrix on the list for re-use.

		@Override
		public void close () {
			if (omat != null) {
				synchronized (omat_list) {
					omat_list.addLast (omat);
				}
				omat = null;
			}
			return;
		}


		//----- Forwarding functions -----


		// Fill in all matrices, using the given Omori parameters.
		// Note: An OmoriMatrix object can be used multiple times,
		// by calling omat_build for each new pair of Omori parameters.

		public final void omat_build (double p, double c) {
			omat.omat_build (p, c);
			return;
		}


		// Display our contents.

		@Override
		public String toString() {
			return omat.toString();
		}
	
	}

	// Make a handle object.

	public OmoriMatrixHandle make_OmoriMatrixHandle () {
		return new OmoriMatrixHandle();
	}




	//-----  Per-Pair Processing -----




	// Inner class to process particular pair of the (b, alpha) and (p, c) parameters.
	// It contains vectors with pre-computed values that depend on both magnitudes and times,
	// so these values can be re-used with multiple productivity parameters.
	// Note: This class never modifies anything in the outer class, and so it is possible for
	// different threads to simultaneously use different objects.

	public class PairMagOmori {

		//----- Parameters -----

		// Pre-computed magnitude exponentials.

		public MagExponent mexp;

		// Pre-computed Omori arrays.

		public OmoriMatrix omat;




		//----- Pre-computed partial values -----




		// Unscaled likelihood values for each target rupture, due to all source ruptures.
		// This is for non-mainshock source ruptures, assuming a == 0 and Q == 1.
		// It may also contain mainshock source ruptures with non-zero offset values.
		// like_rup_targ_rup_src[i_t_rup] is the unscaled likelihood for the target rupture
		// selected by i_t_rup, due to all prior non-mainshock source ruptures, which is
		//
		//   SUM( 10^(alpha*(ms - mref)) * (10^(-b*(mct - mref))) * ((t - s + c)^(-p)) )
		//
		// The sum runs over all i_s_rup which select non-mainshock source ruptures with
		// 0 <= i_s_rup < i_t_rup, and
		//
		//   t = a_rupture_obj[i_t_rup].t_day         = target rupture time
		//   s = a_rupture_obj[i_s_rup].t_day         = source rupture time
		//   mct = a_rupture_obj[i_t_rup].k_prod      = target rupture magnitude of completeness
		//   ms = a_rupture_obj[i_s_rup].rup_mag      = source rupture magnitude
		//
		//   like_rup_targ_rup_src.length = rupture_count
		//
		// Note: Multiply by (10^a)*Q to obtain the log-likelihood contribution at each target rupture,
		// due to prior source ruptures.

		public double[] like_rup_targ_rup_src;

		// Same as above, except for mainshock source ruptures.
		//
		// Note: Multiply by (10^ams)*Q to obtain the log-likelihood contribution at each target rupture,
		// due to prior source ruptures (or something more complicated if non-zero offsets are used).

		public double[] like_main_rup_targ_rup_src;




		// Unscaled likelihood values for all target intervals, due to all source ruptures.
		// This is for non-mainshock source ruptures, assuming a == 0 and Q == 1.
		// It may also contain mainshock source ruptures with non-zero offset values.
		// like_int_targ_rup_src[0] is the unscaled likelihood summed over all target intervals,
		// due to all prior non-mainshock source ruptures, which is
		//
		//   SUM( 10^(alpha*(ms - mref)) * (10^(-b*(mct - mref))) * Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) )
		//
		// The sum runs over all i_t_int, and all i_s_rup which select non-mainshock source ruptures
		// occurring before i_t_int, and
		//
		//   t1 = a_interval_time[i_t_int]            = target interval begin time
		//   t2 = a_interval_time[i_t_int + 1]        = target interval end time
		//   s = a_rupture_obj[i_s_rup].t_day         = source rupture time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ms = a_rupture_obj[i_s_rup].rup_mag      = source rupture magnitude
		//
		//   like_int_targ_rup_src.length = 1
		//
		// Note: Multiply by (10^a)*Q to obtain the log-likelihood contribution for all target intervals,
		// due to prior source ruptures.

		public double[] like_int_targ_rup_src;

		// Same as above, except for mainshock source ruptures.
		//
		// Note: Multiply by (10^ams)*Q to obtain the log-likelihood contribution for all target intervals,
		// due to prior source ruptures (or something more complicated if non-zero offsets are used).

		public double[] like_main_int_targ_rup_src;




		// Unscaled productivity density values for each target interval, due to all source ruptures.
		// This is for non-mainshock source ruptures, assuming a == 0 and Q == 1.
		// It may also contain mainshock source ruptures with non-zero offset values.
		// prod_int_targ_rup_src[i_t_int] is the unscaled productivity density for the target interval
		// selected by i_t_int, due to all prior non-mainshock source ruptures, which is
		//
		//   SUM( 10^(alpha*(ms - mref)) * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		//
		//        * Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) / (t2 - t1) )
		//
		// The sum runs over all i_s_rup which select non-mainshock source ruptures
		// occurring before i_t_int, and
		//
		//   t1 = a_interval_time[i_t_int]            = target interval begin time
		//   t2 = a_interval_time[i_t_int + 1]        = target interval end time
		//   s = a_rupture_obj[i_s_rup].t_day         = source rupture time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ms = a_rupture_obj[i_s_rup].rup_mag      = source rupture magnitude
		//
		//   prod_int_targ_rup_src.length = interval_count
		//
		// Note: Multiply by ((10^a)*Q)*((10^a)*Q) to obtain the productivity density contribution 
		// for each target interval, due to prior source ruptures.  The first factor is for the productivity
		// of the source ruptures, and the second factor is for the productivity of the target intervals.

		public double[] prod_int_targ_rup_src;

		// Same as above, except for mainshock source ruptures.
		//
		// Note: Multiply by ((10^ams)*Q)*((10^a)*Q) to obtain the productivity density contribution for each target interval,
		// due to prior source ruptures (or something more complicated if non-zero offsets are used).

		public double[] prod_main_int_targ_rup_src;




		//----- Building -----




		// Clear all variables.

		public void clear () {
			mexp = null;
			omat = null;

			like_rup_targ_rup_src = null;
			like_main_rup_targ_rup_src = null;
			like_int_targ_rup_src = null;
			like_main_int_targ_rup_src = null;
			prod_int_targ_rup_src = null;
			prod_main_int_targ_rup_src = null;

			return;
		}




		// Allocate all arrays.

		private void pmom_alloc () {

			final int rupture_count = history.rupture_count;
			final int interval_count = history.interval_count;

			// Allocate the arrays

			like_rup_targ_rup_src = new double[rupture_count];
			like_main_rup_targ_rup_src = new double[rupture_count];

			like_int_targ_rup_src = new double[1];
			like_main_int_targ_rup_src = new double[1];

			prod_int_targ_rup_src = new double[interval_count];
			prod_main_int_targ_rup_src = new double[interval_count];

		
			return;
		}




		// Fill in all arrays, using the given parameters.
		// Note: A PairMagOmori object can be used multiple times,
		// by calling pmom_build for each new set of parameters.

		public void pmom_build (MagExponent mexp, OmoriMatrix omat) {

			// Save the parameters

			this.mexp = mexp;
			this.omat = omat;

			// Build arrays with rupture target and rupture source

			omat.apply_omat_rup_targ_rup_src (
				like_rup_targ_rup_src,			// final double[] y1
				like_main_rup_targ_rup_src,		// final double[] y2
				mexp.rup_azero_prod,			// final double[] x1
				mexp.rup_main_azero_prod,		// final double[] x2
				mexp.rup_part_like_unlim		// final double[] d
			);

			// Build arrays with interval target and rupture source

			omat.apply_omat_int_targ_rup_src (
				prod_int_targ_rup_src,			// final double[] y1
				prod_main_int_targ_rup_src,		// final double[] y2
				like_int_targ_rup_src,			// final double[] z1
				like_main_int_targ_rup_src,		// final double[] z2
				mexp.rup_azero_prod,			// final double[] x1
				mexp.rup_main_azero_prod,		// final double[] x2
				mexp.int_part_targ_prod,		// final double[] dy
				mexp.int_part_like_unlim		// final double[] dz
			);
		
			return;
		}




		// Constructor allocates the matrices.

		public PairMagOmori () {
			clear();
			pmom_alloc();
		}




		// Display our contents.

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append (mexp.toString());
			result.append (omat.toString());

			result.append ("OEDiscExtFit.PairMagOmori:" + "\n");

			result.append ("like_rup_targ_rup_src: "      + vec_summary_string(like_rup_targ_rup_src)      + "\n");
			result.append ("like_main_rup_targ_rup_src: " + vec_summary_string(like_main_rup_targ_rup_src) + "\n");
			result.append ("like_int_targ_rup_src: "      + vec_summary_string(like_int_targ_rup_src)      + "\n");
			result.append ("like_main_int_targ_rup_src: " + vec_summary_string(like_main_int_targ_rup_src) + "\n");
			result.append ("prod_int_targ_rup_src: "      + vec_summary_string(prod_int_targ_rup_src)      + "\n");
			result.append ("prod_main_int_targ_rup_src: " + vec_summary_string(prod_main_int_targ_rup_src) + "\n");

			return result.toString();
		}

	}




	// List of PairMagOmori objects available for re-use.
	// Threading: Access must be synchronized to allow access from multiple threads.

	private ArrayDeque<PairMagOmori> pmom_list;

	// Handle object to obtain or allocate n PairMagOmori.
	// The handle should be created in a try-with-resources.

	public class PairMagOmoriHandle implements AutoCloseable {

		// The contained PairMagOmori.

		private PairMagOmori pmom;

		// Get the contained PairMagOmori.

		public final PairMagOmori get_pmom () {
			return pmom;
		}

		// Constructor obtains or allocates the PairMagOmori.

		public PairMagOmoriHandle () {
			synchronized (pmom_list) {
				pmom = pmom_list.pollLast();
			}
			if (pmom == null) {
				pmom = new PairMagOmori();
			}
		}

		// Closing puts the PairMagOmori on the list for re-use.

		@Override
		public void close () {
			if (pmom != null) {
				synchronized (pmom_list) {
					pmom_list.addLast (pmom);
				}
				pmom = null;
			}
			return;
		}


		//----- Forwarding functions -----


		// Fill in all arrays, using the given parameters.
		// Note: A PairMagOmori object can be used multiple times,
		// by calling pmom_build for each new set of parameters.

		public final void pmom_build (MagExponentHandle mexp, OmoriMatrixHandle omat) {
			pmom.pmom_build (mexp.get_mexp(), omat.get_omat());
		}


		// Display our contents.

		@Override
		public String toString() {
			return pmom.toString();
		}
	
	}

	// Make a handle object.

	public PairMagOmoriHandle make_PairMagOmoriHandle () {
		return new PairMagOmoriHandle();
	}




	//-----  Per-a-value Processing -----




	// Inner class to process particular pair of the (b, alpha) and (p, c) parameters.
	// It contains vectors with pre-computed values that depend on both magnitudes and times,
	// so these values can be re-used with multiple productivity parameters.
	// Note: This class never modifies anything in the outer class, and so it is possible for
	// different threads to simultaneously use different objects.

	public class AValueProd {

		//----- Parameters -----

		// Pre-computed values for magnitude exponentials and Omori parameters.

		public PairMagOmori pmom;

		// The value of (10^a)*Q used for calculating the productivity of intervals.

		public double ten_aint_q;

		// Get the value of (10^a)*Q used for calculating the productivity of intervals.

		public final double get_ten_aint_q () {
			return ten_aint_q;
		}

		// Get the index of the mainshock as identified in the history.

		public final int get_i_mainshock () {
			return history.i_mainshock;
		}




		//----- Pre-computed unscaled values -----




		// Unscaled likelihood values for each target rupture, due to all source intervals.
		// This is for productivity descended from non-mainshock source ruptures, assuming a == 0 and Q == 1.
		// It may also contain productivity descended from mainshock source ruptures with non-zero offset values.
		//
		//   like_rup_targ_all_src.length = rupture_count
		//
		// like_rup_targ_all_src[i_t_rup] is the unscaled likelihood for the target rupture
		// selected by i_t_rup, due to all prior non-mainshock source rupures.  It is the sum of two parts.
		//
		// The first part is the direct effect of each prior source rupture:
		//
		//   SUM( 10^(alpha*(ms - mref)) * (10^(-b*(mct - mref))) * ((t - s + c)^(-p)) )
		//
		// The sum runs over all i_s_rup which select non-mainshock source ruptures with
		// 0 <= i_s_rup < i_t_rup, and
		//
		//   t = a_rupture_obj[i_t_rup].t_day         = target rupture time
		//   s = a_rupture_obj[i_s_rup].t_day         = source rupture time
		//   mct = a_rupture_obj[i_t_rup].k_prod      = target rupture magnitude of completeness
		//   ms = a_rupture_obj[i_s_rup].rup_mag      = source rupture magnitude
		//
		// This first part is equal to:
		//
		//   pmom.like_rup_targ_rup_src[i_t_rup]
		//
		// The second part is the effect of each prior source interval:
		//
		//   SUM( ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; (t - s + c)^(-p) * ds) )
		//
		// The sum runs over all i_s_int which select source intervals prior to i_t_rup, and
		//
		//   t = a_rupture_obj[i_t_rup].t_day         = target rupture time
		//   s1 = a_interval_time[i_s_int]            = source interval begin time
		//   s2 = a_interval_time[i_s_int + 1]        = source interval end time
		//   mct = a_rupture_obj[i_t_rup].k_prod      = target rupture magnitude of completeness
		//   ks = prod_int_targ_all_src[i_s_int]      = source interval unscaled productivity
		//
		// Note: Multiply by (10^a)*Q to obtain the log-likelihood contribution at each target rupture,
		// due to prior sources.

		public double[] like_rup_targ_all_src;

		// Same as above, except for productivity descended from mainshock source ruptures.
		//
		// Note: Multiply by (10^ams)*Q to obtain the log-likelihood contribution at each target rupture,
		// due to prior source intervals (or something more complicated if non-zero offsets are used).

		public double[] like_main_rup_targ_all_src;




		// Unscaled likelihood values for all target intervals, due to all sources.
		// This is for productivity descended from non-mainshock source ruptures, assuming a == 0 and Q == 1.
		// It may also contain productivity descended from mainshock source ruptures with non-zero offset values.
		//
		//   like_int_targ_all_src.length = 1
		//
		// like_int_targ_all_src[0] is the unscaled likelihood summed over all target intervals,
		// due to all prior due to all prior non-mainshock source rupures.  It is the sum of three parts.
		//
		// The first part is the direct effect of each prior source rupture:
		//
		//   SUM( 10^(alpha*(ms - mref)) * (10^(-b*(mct - mref))) * Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) )
		//
		// The sum runs over all i_t_int, and all i_s_rup which select non-mainshock source ruptures
		// occurring before i_t_int, and
		//
		//   t1 = a_interval_time[i_t_int]            = target interval begin time
		//   t2 = a_interval_time[i_t_int + 1]        = target interval end time
		//   s = a_rupture_obj[i_s_rup].t_day         = source rupture time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ms = a_rupture_obj[i_s_rup].rup_mag      = source rupture magnitude
		//
		// This first part is also equal to
		//
		//   pmom.like_int_targ_rup_src[0]
		//
		// The second part is the effect of each prior interval:
		//
		//   SUM( ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * dt * ds) )
		//
		// The sum runs over all i_t_int, and all i_s_int which select source intervals
		// occurring before i_t_int, and
		//
		//   t1 = a_interval_time[i_t_int]            = target interval begin time
		//   t2 = a_interval_time[i_t_int + 1]        = target interval end time
		//   s1 = a_interval_time[i_s_int]            = source interval begin time
		//   s2 = a_interval_time[i_s_int + 1]        = source interval end time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ks = prod_int_targ_all_src[i_s_int]      = source interval unscaled productivity
		//
		// The third part is the effect of the target interval on itself, acting as both source and target:
		//
		//   ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * dt * ds)
		//
		//   s1 = a_interval_time[i_t_int]            = source/target interval begin time
		//   s2 = a_interval_time[i_t_int + 1]        = source/target interval end time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ks = prod_int_targ_all_src[i_t_int]      = source interval unscaled productivity
		//
		// Note: Multiply by (10^a)*Q to obtain the log-likelihood contribution for all target intervals,
		// due to prior source intervals.

		public double[] like_int_targ_all_src;

		// Same as above, except for mainshock source intervals.
		//
		// Note: Multiply by (10^ams)*Q to obtain the log-likelihood contribution for all target intervals,
		// due to prior source intervals (or something more complicated if non-zero offsets are used).

		public double[] like_main_int_targ_all_src;




		// Unscaled productivity density values for each target interval, due to all sources.
		// This is for productivity descended from non-mainshock source ruptures, assuming a == 0 and Q == 1.
		// It may also contain productivity descended from mainshock source ruptures with non-zero offset values.
		//
		//   prod_int_targ_all_src.length = interval_count
		//
		// prod_int_targ_all_src[i_t_int] is the unscaled productivity density for the target interval
		// selected by i_t_int, due to all prior non-mainshock source ruptures.  It is the sum of three parts.
		//
		// The first part is the direct effect of each prior source rupture:
		//
		//   (10^aint)*Q * SUM( 10^(alpha*(ms - mref)) * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		//
		//                      * Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) / (t2 - t1) )
		//
		// The sum runs over all i_s_rup which select non-mainshock source ruptures
		// occurring before i_t_int, and
		//
		//   t1 = a_interval_time[i_t_int]            = target interval begin time
		//   t2 = a_interval_time[i_t_int + 1]        = target interval end time
		//   s = a_rupture_obj[i_s_rup].t_day         = source rupture time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ms = a_rupture_obj[i_s_rup].rup_mag      = source rupture magnitude
		//
		// This first part is also equal to
		//
		//   pmom.prod_int_targ_rup_src[i_t_int] * (10^aint)*Q
		//
		// The second part is the effect of each prior interval:
		//
		//   (10^aint)*Q * SUM( ks * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		//
		//                      * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * ds * dt) / (t2 - t1) )
		//
		// The sum runs over all i_s_int which select source intervals occurring before i_t_int, and
		//
		//   t1 = a_interval_time[i_t_int]            = target interval begin time
		//   t2 = a_interval_time[i_t_int + 1]        = target interval end time
		//   s1 = a_interval_time[i_s_int]            = source interval begin time
		//   s2 = a_interval_time[i_s_int + 1]        = source interval end time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ks = prod_int_targ_all_src[i_s_int]      = source interval unscaled productivity
		//
		// Note that the second part depends on earlier values of prod_int_targ_all_src,
		// so the values must be computed and stored sequentially.
		//
		// The third part is the effect of the target interval on itself, acting as both source and target:
		//
		//   (10^aint)*Q * ks * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		//
		//               * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * ds * dt) / (s2 - s1)
		//
		//   s1 = a_interval_time[i_t_int]            = source/target interval begin time
		//   s2 = a_interval_time[i_t_int + 1]        = source/target interval end time
		//   mct = a_interval_mc[i_t_int]             = target interval magnitude of completeness
		//   ks = prod_int_targ_all_src[i_t_int]      = source interval unscaled productivity
		//
		// In the third part, the value of ks is the productivity of the target interval obtained
		// from the first and second parts, before the third part is added.
		//
		// Note: This contains the factor of (10^aint)*Q needed to scale the productivity of the interval.
		// You still need to multiply by (10^a)*Q to scale the productivity of the source ruptures;
		// the lack of this factor is why the value is called unscaled.

		public double[] prod_int_targ_all_src;

		// Same as above, except for productivity descended from mainshock source ruptures.
		//
		// Note: This contains the factor of (10^aint)*Q needed to scale the productivity of the interval.
		// You still need to multiply by (10^ams)*Q (or something more complicated) to scale the productivity
		// of the source ruptures; the lack of this factor is why the value is called unscaled.

		public double[] prod_main_int_targ_all_src;




		//----- Building -----




		// Clear all variables.

		public void clear () {
			pmom = null;

			like_rup_targ_all_src = null;
			like_main_rup_targ_all_src = null;
			like_int_targ_all_src = null;
			like_main_int_targ_all_src = null;
			prod_int_targ_all_src = null;
			prod_main_int_targ_all_src = null;

			return;
		}




		// Allocate all arrays.

		private void avpr_alloc () {

			final int rupture_count = history.rupture_count;
			final int interval_count = history.interval_count;

			// Allocate the arrays

			like_rup_targ_all_src = new double[rupture_count];
			like_main_rup_targ_all_src = new double[rupture_count];

			like_int_targ_all_src = new double[1];
			like_main_int_targ_all_src = new double[1];

			prod_int_targ_all_src = new double[interval_count];
			prod_main_int_targ_all_src = new double[interval_count];

		
			return;
		}




		// Fill in all arrays, using the given parameters.
		// Note: A AValueProd object can be used multiple times,
		// by calling avpr_build for each new set of parameters.

		public void avpr_build (PairMagOmori pmom, double ten_aint_q) {

			// Save the parameters

			this.pmom = pmom;
			this.ten_aint_q = ten_aint_q;

			// Build arrays with interval target and interval source (this one must be first!)

			pmom.omat.apply_omat_int_targ_int_src (
				prod_int_targ_all_src,				// final double[] y1
				prod_main_int_targ_all_src,			// final double[] y2
				like_int_targ_all_src,				// final double[] z1
				like_main_int_targ_all_src,			// final double[] z2
				pmom.prod_int_targ_rup_src,			// final double[] x1
				pmom.prod_main_int_targ_rup_src,	// final double[] x2
				pmom.like_int_targ_rup_src,			// final double[] o1
				pmom.like_main_int_targ_rup_src,	// final double[] o2
				pmom.mexp.int_part_targ_prod,		// final double[] dy
				pmom.mexp.int_part_like_unlim,		// final double[] dz
				ten_aint_q							// final double s
			);

			// Build arrays with rupture target and interval source

			pmom.omat.apply_omat_rup_targ_int_src (
				like_rup_targ_all_src,				// final double[] y1
				like_main_rup_targ_all_src,			// final double[] y2
				prod_int_targ_all_src,				// final double[] x1
				prod_main_int_targ_all_src,			// final double[] x2
				pmom.like_rup_targ_rup_src,			// final double[] o1
				pmom.like_main_rup_targ_rup_src,	// final double[] o2
				pmom.mexp.rup_part_like_unlim		// final double[] d
			);
		
			return;
		}




		// Constructor allocates the matrices.

		public AValueProd () {
			clear();
			avpr_alloc();
		}




		// Display our contents.

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append (pmom.toString());

			result.append ("OEDiscExtFit.AValueProd:" + "\n");
			result.append ("ten_aint_q = " + ten_aint_q + "\n");

			result.append ("like_rup_targ_all_src: "      + vec_summary_string(like_rup_targ_all_src)      + "\n");
			result.append ("like_main_rup_targ_all_src: " + vec_summary_string(like_main_rup_targ_all_src) + "\n");
			result.append ("like_int_targ_all_src: "      + vec_summary_string(like_int_targ_all_src)      + "\n");
			result.append ("like_main_int_targ_all_src: " + vec_summary_string(like_main_int_targ_all_src) + "\n");
			result.append ("prod_int_targ_all_src: "      + vec_summary_string(prod_int_targ_all_src)      + "\n");
			result.append ("prod_main_int_targ_all_src: " + vec_summary_string(prod_main_int_targ_all_src) + "\n");

			return result.toString();
		}




		//----- Readout -----




		// Calculate log-likelihood.
		// Parameters:
		//  ten_a_q = The value of (10^a)*Q applied to non-mainshock ruptures.  Normally equal to ten_aint_q.
		//  ten_ams_q = The value of (10^ams)*Q applied to mainshock ruptures.
		// Returns the log-likelihood value.
		// Note: The value of ten_ams_q may be more complicated if non-zero offsets for mainshocks are used.

		public double avpr_calc_log_like (final double ten_a_q, final double ten_ams_q) {

			// Accumulate contributions from each rupture

			double result = 0.0;

			final int nlo = like_rup_begin;
			final int nhi = like_rup_end;

			for (int n = nlo; n < nhi; ++n) {
				result += (Math.log((ten_a_q * like_rup_targ_all_src[n]) + (ten_ams_q * like_main_rup_targ_all_src[n])));
			}

			// Subtracted accumulated contributions from each interval

			result -= ((ten_a_q * like_int_targ_all_src[0]) + (ten_ams_q * like_main_int_targ_all_src[0]));

			return result;
		}

	}




	// List of AValueProd objects available for re-use.
	// Threading: Access must be synchronized to allow access from multiple threads.

	private ArrayDeque<AValueProd> avpr_list;

	// Handle object to obtain or allocate n AValueProd.
	// The handle should be created in a try-with-resources.

	public class AValueProdHandle implements AutoCloseable {

		// The contained AValueProd.

		private AValueProd avpr;

		// Get the contained AValueProd.

		public final AValueProd get_avpr () {
			return avpr;
		}

		// Constructor obtains or allocates the AValueProd.

		public AValueProdHandle () {
			synchronized (avpr_list) {
				avpr = avpr_list.pollLast();
			}
			if (avpr == null) {
				avpr = new AValueProd();
			}
		}

		// Closing puts the AValueProd on the list for re-use.

		@Override
		public void close () {
			if (avpr != null) {
				synchronized (avpr_list) {
					avpr_list.addLast (avpr);
				}
				avpr = null;
			}
			return;
		}


		//----- Forwarding functions -----


		// Fill in all arrays, using the given parameters.
		// Note: A AValueProd object can be used multiple times,
		// by calling avpr_build for each new set of parameters.

		public final void avpr_build (PairMagOmoriHandle pmom, double ten_aint_q) {
			avpr.avpr_build (pmom.get_pmom(), ten_aint_q);
			return;
		}


		// Display our contents.

		@Override
		public String toString() {
			return avpr.toString();
		}


		// Calculate log-likelihood.
		// Parameters:
		//  ten_a_q = The value of (10^a)*Q applied to non-mainshock ruptures.  Normally equal to ten_aint_q.
		//  ten_ams_q = The value of (10^ams)*Q applied to mainshock ruptures.
		// Returns the log-likelihood value.
		// Note: The value of ten_ams_q may be more complicated if non-zero offsets for mainshocks are used.

		public final double avpr_calc_log_like (final double ten_a_q, final double ten_ams_q) {
			return avpr.avpr_calc_log_like (ten_a_q, ten_ams_q);
		}
	
	}

	// Make a handle object.

	public AValueProdHandle make_AValueProdHandle () {
		return new AValueProdHandle();
	}





	//----- Construction -----




	// Clear to default values.

	public void clear () {

		history = null;

		f_intervals = true;
		f_likelihood = true;
		lmr_opt = LMR_OPT_MCT_INFINITY;
		like_rup_begin = 0;
		like_rup_end = 0;
		like_int_begin = 0;
		like_int_end = 0;

		mref = 0.0;
		msup = 0.0;
		mag_min = 0.0;
		mag_max = 0.0;

		f_omat_rup_targ_rup_src = true;
		f_omat_rup_targ_int_src = true;
		f_omat_int_targ_rup_src = true;
		f_omat_int_targ_int_src = true;

		mexp_list = new ArrayDeque<MagExponent>();
		omat_list = new ArrayDeque<OmoriMatrix>();
		pmom_list = new ArrayDeque<PairMagOmori>();
		avpr_list = new ArrayDeque<AValueProd>();

		return;
	}




	// Default constructor.

	public OEDiscExtFit () {
		clear();
	}




	// Initialize for discrete parameter fitting.
	// Parameters:
	//  history = Rupture history.
	//  cat_params = Simulation parameters, containing the following fields:
	//    cat_params.mref = Reference magnitude, also the minimum considered magnitude, for parameter definition.
	//    cat_params.msup = Maximum considered magnitude, for parameter definition.
	//    cat_params.mag_min_sim = The minimum magnitude to use for the simulation.
	//    cat_params.mag_max_sim = The maximum magnitude to use for the simulation.
	//  f_intervals = True to use intervals to fill in below magnitude of completeness.
	//  f_likelihood = True to calculate data needed for log-likelihood.
	//  lmr_opt = Option to select magnitude range for log-likelihood calculation (LMR_OPT_XXXX).
	//  i_start_rup = Index of rupture starting likelihood calculation, or -1 for all ruptures and intervals.  Often equal to history.i_mainshock.

	public void dfit_build (OEDiscHistory history, OECatalogParams cat_params, boolean f_intervals, boolean f_likelihood, int lmr_opt, int i_start_rup) {

		// Start by clearing

		clear();

		// Save the history

		this.history = history;

		// Save the configuration

		this.f_intervals = f_intervals;
		this.f_likelihood = f_likelihood;
		this.lmr_opt = lmr_opt;

		// Set the likelihood rupture and interval ranges

		if (i_start_rup < 0) {
			this.like_rup_begin = 0;
			this.like_rup_end = history.rupture_count;
			this.like_int_begin = 0;
			this.like_int_end = history.interval_count;
		} else if (i_start_rup >= history.rupture_count) {
			this.like_rup_begin = history.rupture_count;
			this.like_rup_end = history.rupture_count;
			this.like_int_begin = history.interval_count;
			this.like_int_end = history.interval_count;
		} else {
			this.like_rup_begin = i_start_rup + 1;
			this.like_rup_end = history.rupture_count;
			this.like_int_begin = history.a_rupture_int_time_index[i_start_rup];
			this.like_int_end = history.interval_count;
		}

		// Save the simulation Parameters

		this.mref = cat_params.mref;
		this.msup = cat_params.msup;
		this.mag_min = cat_params.mag_min_sim;
		this.mag_max = cat_params.mag_max_sim;

		// Set the matrix allocation flags

		f_omat_rup_targ_rup_src = f_likelihood;
		f_omat_rup_targ_int_src = (f_likelihood && f_intervals);
		f_omat_int_targ_rup_src = (f_likelihood || f_intervals);
		f_omat_int_targ_int_src = f_intervals;
	
		return;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDiscExtFit:" + "\n");

		if (history != null) {
			result.append ("history.magCat = "         + history.magCat         + "\n");
			result.append ("history.rupture_count = "  + history.rupture_count  + "\n");
			result.append ("history.i_mainshock = "    + history.i_mainshock    + "\n");
			result.append ("history.interval_count = " + history.interval_count + "\n");
		}

		result.append ("f_intervals = "            + f_intervals            + "\n");
		result.append ("f_likelihood = "           + f_likelihood           + "\n");
		result.append ("lmr_opt = "                + lmr_opt                + "\n");
		result.append ("like_rup_begin = "         + like_rup_begin         + "\n");
		result.append ("like_rup_end = "           + like_rup_end           + "\n");
		result.append ("like_int_begin = "         + like_int_begin         + "\n");
		result.append ("like_int_end = "           + like_int_end           + "\n");
		result.append ("mref = "                   + mref                   + "\n");
		result.append ("msup = "                   + msup                   + "\n");
		result.append ("mag_min = "                + mag_min                + "\n");
		result.append ("mag_max = "                + mag_max                + "\n");

		return result.toString();
	}




	//----- Testing -----





}
