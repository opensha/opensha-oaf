package org.opensha.oaf.oetas.ver;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;
import org.opensha.oaf.util.InvariantViolationException;

import org.opensha.oaf.oetas.OECatalogParamsMags;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEOmoriCalc;
import org.opensha.oaf.oetas.fit.OEGridOptions;
import org.opensha.oaf.oetas.fit.OEGridPoint;
import org.opensha.oaf.oetas.fit.OEDisc2Grouping;
import org.opensha.oaf.oetas.fit.OEDisc2ExtFit;
import org.opensha.oaf.oetas.fit.OEDisc2InitFitInfo;
import org.opensha.oaf.oetas.util.OEArraysCalc;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;				// natural logarithm of 10
//  import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
//  import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
//  import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
//  import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
//  import static org.opensha.oaf.oetas.OEConstants.TINY_MAG_DELTA;			// a very small change in magnitude
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
//  import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
//  import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value
import static org.opensha.oaf.oetas.OEConstants.TINY_DURATION_DAYS;		// an extremely small duration

import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MCT_INFINITY;		// 1 = From time-dependent magnitude of completeness to infinity.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MCT_MAG_MAX;		// 2 = From time-dependent magnitude of completeness to maximum simulation magnitude.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MAGCAT_INFINITY;	// 3 = From catalog magnitude of completeness to infinity.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MAGCAT_MAG_MAX;		// 4 = From catalog magnitude of completeness to maximum simulation magnitude.


// Discretized rupture history, parameter fitting with extended sources.
// Author: Michael Barall.
//
// This is an alternative implemention of parameter fitting from OEDisc2ExtFit.
// It exists for fitting verification.
//
// A history consists of a sequence of ruptures and a sequence of intervals,
// spanning a given time range t_range_begin through t_range_end.
//
// In this implementation, ruptures are located at interval endpoints, except that a
// rupture may be located inside an interval whose magnitude of completeness is magCat.
// Assuming that the minimum simulation magnitude equals magCat, such intervals have
// zero productivity and so there is no issue about how to handle triggering between
// the rupture and the interval that contains it.  The rupture does however contribute
// to the integrated intensity function over the interval for the likelihood calculation.
//
// The implementation permits more than one rupture at the same time, although this
// should be rare.
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
// MAINSHOCKS
//
// Some ruptures are designated as mainshocks.  For a mainshock source, the parameter "a" is
// replaced by "ams".  Typically mainshocks have Q == 1 (regardless of magnitude ranges) and
// can only be sources, not targets.  In any formula below involving a rupture source, it is
// understood that if the rupture is a mainshock then the substitution a -> ams should be made.
//
// BACKGROUND RATE
//
// A background rate "mu" is the rate of background earthquakes with magnitude >= mref per
// unit time.  The intensity function due to the background rate, which is time-independent, is
//
//   lambda(m) =  mu * beta * (10^(-b*(m - mref)))
//
// The background rate can be well-approximated by a mainshock source at time s which is far
// in the past.  Comparing lambda(m) and lambda(t, m), a formula for a mainshock source can be
// converted into a formula for background rate by substituting k -> mu * (-s)^p and
// (t - s + c) -> (-s).  Formulas can also be computed directly from lambda(m).
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
// Notice that this contains two factors of 10^a.  For a mainshock source, the factor of 10^a
// inside k is replaced by 10^ams, and typically the factor of Q inside k is taken to be 1.
// The explicit factor of 10^a can be replaced by 10^aint where "aint" is a productivity
// parameter for interval sources.  Typically aint == a but the code allows them to be different.
//
// If the source is a background rate them
//
//   kt = mu * beta * 10^(a + (alpha - b)*(mag_min - mref)) * Q * W(v, mct - mag_min)
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
// If the source rupture is a mainshock, then substitute a -> ams and typically Q -> 1.
//
// For a background rate source:
//
//   mu * (10^(-b*(mct - mref)))
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

// NOTES ON MAGNITUDE RANGES
//
// The value of mref is used throughout as the reference magnitude for exponents that contain
// magnitudes.
//
// The value of msup is used only for calculating the q_correction term.  Note that q_correction
// is not used within this code, because we always work with corrected productivities which are
// assumed to be of the form (10^a)*Q.  Note that mref, mag_min, and mag_max also enter into
// the calculation of q_correction.  The q_correction is the correction to convert productivity
// from the [mref, msup] range to the [mag_min, mag_max] range; in the case alpha == b it
// reduces to (msup - mref)/(mag_max - mag_min).
//
// The values of mref, mag_min, and mag_max are used to convert from branch ratio to (10^a)*Q
// (functions calc_ten_a_q_from_branch_ratio).  The conversion is not used within this code,
// but is available for use by callers.  In the case alpha == b, the value of mref drops out,
// and (10^a)*Q is inversely proportional to (mag_max - mag_min).  Note that the value of
// tint_br also enters into the conversion.
//
// If lmr_opt is LMR_OPT_MCT_MAG_MAX(2) or LMR_OPT_MAGCAT_MAG_MAX(4), then mag_max is used as
// the upper end of the magnitude range for calculating the likelihood of each target
// rupture and target interval.  Otherwise, the upper end is infinity.
//
// The value of mag_min is used to calculate the productivity of each target interval.
// If mag_min is less than the effective magnitude of completeness of the interval (mct), then
// the productivity is positive.  In the case alpha == b, it is proportional to (mct - mag_min).
// If mag_min is greater than mct, then the productivity of the interval is zero.
// Note that mct is magCat if lmr_opt is LMR_OPT_MAGCAT_INFINITY(3) or LMR_OPT_MAGCAT_MAG_MAX(4); and
// mct is the time-dependent magnitude of completeness if lmr_opt is LMR_OPT_MCT_INFINITY(1) or
// LMR_OPT_MCT_MAG_MAX(2).  Note that setting mag_min >= magCat will cause many or all
// intervals to have zero productivity.  Note that if f_intervals is false, then all intervals
// are treated as having zero productivity regardless of mag_min.  Note that if intervals with
// mct == magCat are joined when forming the history, then mag_min >= magCat is required.

public class OEVerFitAccum implements OEVerFitLogLikeCalc {


	//----- History -----

	// The rupture history to fit.

	private OEVerFitHistory history;

	// The catalog magnitude of completeness.

	private double magCat;

	// The number of ruptures in the history.

	private int rupture_count;

	// The number of intervals in the history.

	private int interval_count;


	//----- Statistics -----

	// Class that provides a simple statistics counter.

	private static class stat_counter {
		public String name;
		public int count;
		public double time_lo;
		public double time_hi;
		public double duration;

		public final void clear () {
			count = 0;
			time_lo = 0.0;
			time_hi = 0.0;
			duration = 0.0;
			return;
		}

		public stat_counter (String name) {
			this.name = name;
			clear();
		}

		private void advance_count (double tlo, double thi) {
			++count;
			if (count == 1) {
				time_lo = tlo;
				time_hi = thi;
			} else {
				time_lo = Math.min (time_lo, tlo);
				time_hi = Math.max (time_hi, thi);
			}
			return;
		}

		public final void count_rupture (OEVerFitRupture rupture) {
			advance_count (rupture.get_rup_t_day(), rupture.get_rup_t_day());
			return;
		}

		public final void count_interval (OEVerFitInterval interval) {
			advance_count (interval.get_int_time_1(), interval.get_int_time_2());
			duration += interval.get_int_duration();
			return;
		}

		@Override
		public String toString () {
			if (count == 0) {
				return name + " = {count = " + count + "}\n";
			}
			if (duration == 0.0) {
				return name + " = {count = " + count + ", time_lo = " + time_lo + ", time_hi = " + time_hi + ", span = " + (time_hi - time_lo) + "}\n";
			}
			return name + " = {count = " + count + ", time_lo = " + time_lo + ", time_hi = " + time_hi + ", span = " + (time_hi - time_lo) + ", duration = " + duration + "}\n";
		}

		public final int get_count () {
			return count;
		}
	}

	// The number of source ruptures.

	private stat_counter stat_source_rupture = new stat_counter ("stat_source_rupture");

	// The number of source ruptures that are mainshocks or foreshocks.

	private stat_counter stat_main_rupture = new stat_counter ("stat_main_rupture");

	// The number of source ruptures that are aftershocks.

	private stat_counter stat_after_rupture = new stat_counter ("stat_after_rupture");

	// The number of target ruptures.

	private stat_counter stat_target_rupture = new stat_counter ("stat_target_rupture");

	// The number of target ruptures that are interior to an interval.

	private stat_counter stat_interior_rupture = new stat_counter ("stat_interior_rupture");

	// The number of target/source intervals.

	private stat_counter stat_target_interval = new stat_counter ("stat_target_interval");

	// The number target/source intervals whose magnitude of completeness is magCat.

	private stat_counter stat_mag_cat_interval = new stat_counter ("stat_mag_cat_interval");

	// The number target/source intervals whose magnitude of completeness is not magCat.

	private stat_counter stat_mct_interval = new stat_counter ("stat_mct_interval");

	// Clear all statistics.

	private void stat_clear () {
		stat_source_rupture.clear();
		stat_main_rupture.clear();
		stat_after_rupture.clear();
		stat_target_rupture.clear();
		stat_interior_rupture.clear();
		stat_target_interval.clear();
		stat_mag_cat_interval.clear();
		stat_mct_interval.clear();
		return;
	}


	//----- Magnitude ranges -----

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	private double mref;

	// Maximum considered magnitude, for parameter definition.

	private double msup;

	// The minimum magnitude to use for the simulation.

	private double mag_min;

	// The maximum magnitude to use for the simulation.

	private double mag_max;


	//----- Configuration options -----

	// True to use intervals to fill in below magnitude of completeness.
	// This allows intervals to have non-zero productivity density.

	private boolean f_intervals;

	// Coefficient for intervals to act as productivity sources for later intervals.
	// Ignored unless f_intervals is true.

	private double c_cross_intervals;

	// Coefficient for intervals to act as productivity sources for themselves.
	// Ignored unless f_intervals is true.

	private double c_self_intervals;

	// Likelihood magnitude range option (LMR_OPT_XXXX).

	private int lmr_opt;

	// Time interval for interpreting branch ratios, in days.
	// Used for branch ratio conversion.

	private double tint_br;


	//----- Grid options -----

	// True if the value of zams is interpreted relative to the a-value.

	private boolean relative_zams;


	//----- Source grouping (not used yet) -----

	//  // Grouping of rupture and interval sources, or null if none specified.
	//  
	//  private OEDisc2Grouping grouping;
	//  
	//  // The beginning and ending+1 of ruptures to included in the grouping.
	//  
	//  private int accept_rup_begin;
	//  private int accept_rup_end;
	//  
	//  // The beginning and ending+1 of intervals to included in the grouping.
	//  
	//  private int accept_int_begin;
	//  private int accept_int_end;


	//----- Grid point -----

	// Gutenberg-Richter parameter, b-value.

	public double b;

	// ETAS intensity parameter, alpha-value.

	public double alpha;

	//Omori c-value.

	public double c;

	// Omori p-value.

	public double p;

	// Branch ratio, n-value.
	// This controls the productivity of secondary triggering.

	public double n;

	// If relative_zams is false: Mainshock productivity, ams-value, for reference magnitude equal to ZAMS_MREF == 0.0.
	// If relative_zams is true: Mainshock productiviey, ams-value, relative to the aftershock productivity.

	public double zams;

	// Background productivity, mu-value, for reference magnitude equal to ZMU_MREF.

	public double zmu;


	//----- Derived point values -----

	// beta = log(10) * b

	private double beta;

	// v = log(10) * (alpha - b)

	private double v_term;

	// Q = exp(v*(mref - mag_min)) * W(v, msup - mref) / W(v, mag_max - mag_min)
	// where W(x, y) = (exp(x*y) - 1)/x
	//
	// In the case where alpha == b, this reduces to
	// Q = (msup - mref)/(mag_max - mag_min)

	private double q_correction;

	// Corrected aftershock productivity: (10^a)*Q
	// This is the aftershock productivity that would have the desired branch ratio,
	// if used for simulations in magnitude range mag_min to mag_max, while
	// using reference magnitude mref.

	private double ten_a_q;

	// Corrected mainshock productivity: (10^ams)*Q
	// For absolute zams, this is the value of (10^ams)*Q for the reference magnitude mref, taking Q == 1.0.
	// For relative zams, this is the value of (10^a)*Q adjusted by a factor 10^zams.
	// Note: The condition Q == 1.0 means that when the magnitude range changes, the mainshock productivity
	// is not adjusted to preserve a branch ratio. However, if alpha != b then it is still necessary to
	// adjust ams to account for the shift in reference magnitude, to preserve the same productivity.

	private double ten_ams_q;

	// Corrected interval productivity: (10^aint)*Q

	private double ten_aint_q;

	// Offset to convert relative zams to absolute zams, or zero if zams is absolute.

	private double rel_to_abs_zams_offset;

	// Corrected background rate (background earthquakes per day with magnitude >= mref).

	private double mu;


	//----- Pre-computed terms -----

	// For each target rupture, a partial likelihood.
	// The value for each rupture is
	//   10^(-b*(mct - mref))
	// where mct is the magnitude of completeness of the target rupture.
	// This term is a Page-style adjustment of the likelihood to account for time
	// variation in the magnitude of completeness. It is actually an integral
	// over the range of magnitudes considered,
	//   Integral(m = m1, m = m2; beta * 10^(-b*(m - mref)))
	// in which m1 == mct and m2 == infinity.
	// If m2 == infinity, the integral evalutes to
	//   10^(-b*(m1 - mref))
	// If m1 < m2 < infinity, the integral evaluates to
	//   10^(-b*(m1 - mref)) * (1 - exp(-beta*(m2 - m1)))
	// We consider four possible ranges of magnitudes, selected by lmr_opt.
	//   lmr_opt == LMR_OPT_MCT_INFINITY: m1 = mct, m2 = infinity
	//   lmr_opt == LMR_OPT_MCT_MAG_MAX: m1 = mct, m2 = max(mag_max, mct + 0.5)
	//   lmr_opt == LMR_OPT_MAGCAT_INFINITY: m1 = magCat, m2 = infinity
	//   lmr_opt == LMR_OPT_MAGCAT_MAG_MAX: m1 = magCat, m2 = max(mag_max, magCat + 0.5)
	// The max functions ensure that the range of magnitudes considered is at least 0.5 units.

	private double[] a_t_rup_part_like;

	// For each source rupture, the productivity k.
	// The value for each aftershock is
	//    10^(a + alpha*(ms - mref)) * Q
	// where ms is the magnitude of the source rupture.
	// For each mainshock or foreshock, the value is
	//    10^(ams + alpha*(ms - mref)) * Q
	// Note that for mainshocks and foreshocks, typically Q == 1.0.
	// Factors of (10^a)*Q and (10^ams)*Q are implicit due to OEVerFitLinFcnAccum.

	private OEVerFitLinFcnAccum a_s_rup_k;

	// For each target interval, a partial likelihood.
	// The value for each interval is
	//   10^(-b*(mct - mref)) * (t2 - t1)
	// where mct is the magnitude of completeness of the target interval, and where t1 and t2
	// are the begin and end times of the interval.
	// This term is a Page-style adjustment of the likelihood to account for time
	// variation in the magnitude of completeness. It is actually an integral
	// over the range of magnitudes considered,
	//   Integral(m = m1, m = m2; beta * 10^(-b*(m - mref)) * (t2 - t1))
	// in which m1 == mct and m2 == infinity.
	// If m2 == infinity, the integral evalutes to
	//   10^(-b*(m1 - mref)) * (t2 - t1)
	// If m1 < m2 < infinity, the integral evaluates to
	//   10^(-b*(m1 - mref)) * (t2 - t1) * (1 - exp(-beta*(m2 - m1)))
	// We consider four possible ranges of magnitudes, selected by lmr_opt.
	//   lmr_opt == LMR_OPT_MCT_INFINITY: m1 = mct, m2 = infinity
	//   lmr_opt == LMR_OPT_MCT_MAG_MAX: m1 = mct, m2 = max(mag_max, mct + 0.5)
	//   lmr_opt == LMR_OPT_MAGCAT_INFINITY: m1 = magCat, m2 = infinity
	//   lmr_opt == LMR_OPT_MAGCAT_MAG_MAX: m1 = magCat, m2 = max(mag_max, magCat + 0.5)
	// The max functions ensure that the range of magnitudes considered is at least 0.5 units.

	private double[] a_t_int_part_like;

	// For each target interval, a partial productivity.
	// The value for each interval is
	//   beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
	// where mct is the magnitude of completeness of the interval,
	// except that the value is zero if mag_min >= mct.
	// If lmr_opt selects a magnitude range whose lower limit is magCat,
	// then replace mct with magCat in the above formula.

	private double[] a_t_int_part_prod;


	//----- Accumulators -----

	// For each target rupture, accumulate the intensity function lambda(t, mct)/beta,
	// where t is the time of the target rupture and mct is the magnitude of completeness
	// of the target rupture.
	// Because of the G-R distribution, this is the integral of lambda(t, mct) over
	// magnitudes mct to infinity.
	// Factors of (10^a)*Q, (10^ams)*Q, and mu are implicit due to OEVerFitLinFcnAccum.
	//
	// For source ruptures the contribution is
	//   SUM( 10^(alpha*(ms - mref)) * 10^(-b*(mct - mref)) * ((t - s + c)^(-p)) )
	// The sum runs over source ruptures (s <= t), with
	//   t = target rupture time
	//   s = source rupture time
	//   mct = target rupture magnitude of completeness
	//   ms = source rupture magnitude
	//
	// For a backgrouund rate the contribution is
	//   10^(-b*(mct - mref))
	// where
	//   mct = target rupture magnitude of completeness
	//
	// For source intervals the contribution is
	//   SUM( ks * 10^(-b*(mct - mref)) * Integral(s = s1, s = min(t,s2); (t - s + c)^(-p) * ds) )
	// The sum runs over source intervals (s1 < t), with
	//   t = target rupture time
	//   s1, s2 = source interval start and end time
	//   mct = target rupture magnitude of completeness
	//   ks = source interval productivity density
	// Note: The term min(t,s2) provides for the possibility that the target rupture lies inside the
	// source interval (s1 < t < s2). The scanning function for_each_interaction_v2 allows such interactions.
	// However, our history construction permits this only if the interval magnitude of completeness
	// equals the minimum magnitude, which implies that the interval productivity density is zero.
	// So we compute these terms, but they have zero contribution.

	private OEVerFitLinFcnAccum a_t_rup_lambda_over_beta;

	// For each target interval, accumulate the integral of the intensity function lambda(t, mct)/beta.
	// Factors of (10^a)*Q, (10^ams)*Q, and mu are implicit due to OEVerFitLinFcnAccum.
	//
	// For source ruptures the contribution is
	//   SUM( 10^(alpha*(ms - mref)) * 10^(-b*(mct - mref)) * Integral(t = max(t1,s), t = t2; (t - s + c)^(-p) * dt) )
	// The sum runs over source rupture (s < t2), with
	//   t1, t2 = target interval start and end time
	//   s = source rupture time
	//   mct = target interval magnitude of completeness
	//   ms = source rupture magnitude
	//
	// For a background rate the contribution is
	//   10^(-b*(mct - mref)) * (t2 - t1)
	// where
	//   t1, t2 = target interval start and end time
	//   mct = target interval magnitude of completeness
	//
	// For source intervals the contribution is
	//   SUM( ks * 10^(-b*(mct - mref)) * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * dt * ds) )
	// The sum runs over source intervals (s2 <= t1) with
	//   t1, t2 = target interval start and end time
	//   s1, s2 = source interval start and end time
	//   mct = target interval magnitude of completeness
	//   ks = source interval productivity density
	//
	// For the target interval acting as a source on itself the contribution is
	//   ks * 10^(-b*(mct - mref)) * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * dt * ds)
	// where
	//   s1, s2 = target/source interval start and end time
	//   mct = target interval magnitude of completeness
	//   ks = source interval productivity density

	private OEVerFitLinFcnAccum a_t_int_integrated_lambda_over_beta;

	// For each target/source interval, the productivity density ks.
	// Factors of (10^a)*Q, (10^ams)*Q, and mu are implicit due to OEVerFitLinFcnAccum.
	//
	// For source ruptures the contribution is
	//   SUM( 10^(alpha*(ms - mref)) * (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
	//        * Integral(t = t1, t = t2; (t - s + c)^(-p) * dt) / (t2 - t1) )
	// The sum runs over source ruptures (s <= t1), with
	//   t1, t2 = target/source interval start and end time
	//   s = source rupture time
	//   mct = target/source interval magnitude of completeness
	//   ms = source rupture magnitude
	// Note: This is defined only for source ruptures before the target interval (s <= t1), because
	// allowing a contribution from a source rupture interior to the interval would create a circularity
	// where the source rupture contributes to its own intensity function.
	//
	// For a background rate the contribution is
	//   (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
	// where
	//   mct = target/source interval magnitude of completeness
	//
	// For source intervals the contribution is
	//   SUM( ks * (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
	//        * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * ds * dt) / (t2 - t1) )
	// The sum runs over source intervals (s2 <= t1) with
	//   t1, t2 = target/source interval start and end time
	//   s1, s2 = source interval start and end time
	//   mct = target/source interval magnitude of completeness
	//   ks = source interval productivity density
	//
	// For the target interval acting as a source on itself the contribution is
	//   ks * (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
	//   * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * ds * dt) / (s2 - s1)
	// where
	//   s1, s2 = target/source interval start and end time
	//   mct = target/source interval magnitude of completeness
	//   ks = target/source interval productivity density, from all other sources

	private OEVerFitLinFcnAccum a_s_int_ks;

	// Total of a_t_int_integrated_lambda_over_beta over all target intervals.
	// The is the total integral of lambda/beta over all intervals.

	private OEVerFitLinFcnAccum a_total_integrated_lambda_over_beta;


	//----- Output values -----

	// Log-likelihood.
	//   SUM( log(lambda_total(t, mct)/beta) ) - SUM( Integral(t = t1, t = t2; lambda_total(t, mct)/beta * dt) )
	// The first sum runs over target ruptures, and the second sum runs over target intervals.
	// The function lambda_total(t, mct) is the intensity function for earthquakes with
	// magnitude m >= mct, evaluated at the time t. It includes intensity from all sources
	// (prior ruptures, prior intervals, interval self-action, background rate).

	private double log_like;




	//----- Math subroutines -----


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




	//----- Configuration functions -----




	// Set the configuration.
	// This can be done once, and then the accumulator can be used for multiple points.

	public void set_config (
		OEVerFitHistory history,
		OEVerFitOptions fit_options,
		OEGridOptions grid_options
	) {
		// Save history

		this.history = history;
		this.magCat = history.get_hist_mag_cat();
		this.rupture_count = history.get_rupture_count();
		this.interval_count = history.get_interval_count();

		// Save fitting options

		this.mref = fit_options.get_mref();
		this.msup = fit_options.get_msup();
		this.mag_min = fit_options.get_mag_min();
		this.mag_max = fit_options.get_mag_max();

		this.f_intervals = fit_options.get_f_intervals();
		this.c_cross_intervals = fit_options.get_c_cross_intervals();
		this.c_self_intervals = fit_options.get_c_self_intervals();
		this.lmr_opt = fit_options.get_lmr_opt();
		this.tint_br = fit_options.get_tint_br();

		// Save grid options

		this.relative_zams = grid_options.get_relative_zams();

		// Calculate statistics

		calc_history_stat();

		return;
	}




	// Set the configuration to match the given fitter.

	public void set_config (
		OEDisc2ExtFit fitter
	) {
		// Get info about the fitter

		OEDisc2InitFitInfo fit_info = fitter.get_fit_info();

		// Set configuration

		OEVerFitHistory my_history = new OEVerFitHistoryImp (fitter.get_history());
		OEVerFitOptions my_fit_options = new OEVerFitOptions (fit_info);
		OEGridOptions my_grid_options = (new OEGridOptions()).copy_from (fitter.get_grid_options());
		set_config (
			my_history,
			my_fit_options,
			my_grid_options
		);

		// Cross-check statistics

		int expected_main_rupture = fit_info.main_rup_end - fit_info.main_rup_begin;
		if (stat_main_rupture.get_count() != expected_main_rupture) {
			throw new InvariantViolationException ("OEVerFitAccum.set_config: Mainshock rupture count mismatch: expected " + expected_main_rupture + ", got " + stat_main_rupture.get_count());
		}

		int expected_target_interval = fit_info.like_int_end - fit_info.like_int_begin;
		if (stat_target_interval.get_count() != expected_target_interval) {
			throw new InvariantViolationException ("OEVerFitAccum.set_config: Interval count mismatch: expected " + expected_target_interval + ", got " + stat_target_interval.get_count());
		}

		return;
	}




	// Calculate history statistics.

	private void calc_history_stat () {

		// Initialize counters

		stat_clear();

		// Count source ruptures

		history.for_each_source_rupture ((OEVerFitRupture rups) -> {
			stat_source_rupture.count_rupture(rups);
			if (rups.get_rup_is_main()) {
				stat_main_rupture.count_rupture(rups);
			} else {
				stat_after_rupture.count_rupture(rups);
			}
		});

		// Count target ruptures

		history.for_each_like_rupture ((OEVerFitRupture rupt) -> {
			stat_target_rupture.count_rupture(rupt);
			if (rupt instanceof OEVerFitRuptureImp) {
				if (((OEVerFitRuptureImp)rupt).get_rup_is_interior()) {
					stat_interior_rupture.count_rupture(rupt);
				}
			}
		});

		// Count target/source intervals

		history.for_each_like_interval ((OEVerFitInterval intt) -> {
			stat_target_interval.count_interval(intt);
			if (intt.get_int_is_magcat()) {
				stat_mag_cat_interval.count_interval(intt);
			} else {
				stat_mct_interval.count_interval(intt);
			}
		});

		return;
	}




	//  // Set the point.
	//  
	//  public void set_point (OEGridPoint grid_point) {
	//  	this.b = grid_point.b;
	//  	this.alpha = grid_point.alpha;
	//  	this.c = grid_point.c;
	//  	this.p = grid_point.p;
	//  	this.n = grid_point.n;
	//  	this.zams = grid_point.zams;
	//  	this.zmu = grid_point.zmu;
	//  	return;
	//  }




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEVerFitAccum:" + "\n");
		result.append ("magCat = " + magCat + "\n");
		result.append ("rupture_count = " + rupture_count + "\n");
		result.append ("interval_count = " + interval_count + "\n");

		result.append (stat_source_rupture.toString());
		result.append (stat_main_rupture.toString());
		result.append (stat_after_rupture.toString());
		result.append (stat_target_rupture.toString());
		result.append (stat_interior_rupture.toString());
		result.append (stat_target_interval.toString());
		result.append (stat_mag_cat_interval.toString());
		result.append (stat_mct_interval.toString());

		result.append ("mref = " + mref + "\n");
		result.append ("msup = " + msup + "\n");
		result.append ("mag_min = " + mag_min + "\n");
		result.append ("mag_max = " + mag_max + "\n");
		result.append ("f_intervals = " + f_intervals + "\n");
		result.append ("c_cross_intervals = " + c_cross_intervals + "\n");
		result.append ("c_self_intervals = " + c_self_intervals + "\n");
		result.append ("lmr_opt = " + lmr_opt + "\n");
		result.append ("tint_br = " + tint_br + "\n");
		result.append ("relative_zams = " + relative_zams + "\n");

		result.append ("history = {" + "\n");
		result.append (history.dump_history_to_string (50, 50));
		result.append ("}" + "\n");

		return result.toString();
	}




	//----- Computation functions -----




	// Calculate the derived point values.
	// Note: See OEDisc2InitStatVox and OEDisc2InitFitInfo for correction conversions.

	private void calc_derived_point_values () {
		beta = b * C_LOG_10;
		v_term = (alpha - b) * C_LOG_10;
		q_correction = Math.exp(v_term*(mref - mag_min)) * calc_w(v_term, msup - mref) / calc_w(v_term, mag_max - mag_min);
		
		// Corrected aftershock productivity

		ten_a_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
			n,
			p,
			c,
			b,
			alpha,
			mref,
			mag_min,
			mag_max,
			tint_br
		);

		// Corrected interval productivity is taken equal to aftershock productivity

		ten_aint_q = ten_a_q;
	
		// Get offset to convert relative zams to absolute zams, or zero if zams is absolute

		if (relative_zams) {

			// Notes:
			// The function calc_zams_from_br returns a value such that
			//
			// 10^rel_to_abs_zams_offset == (10^a)*Q * 10^((alpha - b)*(ZAMS_MREF - mref))

			rel_to_abs_zams_offset = OEStatsCalc.calc_zams_from_br (
				n,
				p,
				c,
				b,
				alpha,
				mag_min,
				mag_max,
				tint_br
			);
		}
		else {
			rel_to_abs_zams_offset = 0.0;
		}

		// Convert from zams to (10^ams)*Q, with Q == 1
		// Notes:
		// The function calc_a_new_from_mref_new returns a value such that
		//
		// ten_ams_q == 10^(zams + rel_to_abs_zams_offset + (alpha - b)*(mref - ZAMS_MREF))
		//
		// In the case relative_zams == true, combining the above formulas gives
		//
		// ten_ams_q == (10^zams) * (10^a)*Q
		//
		//           == (10^(a + zams))*Q
		//
		// and we see that zams functions as a productivity relative to the aftershock productivity.

		ten_ams_q = Math.pow (10.0, OEStatsCalc.calc_a_new_from_mref_new (
			zams + rel_to_abs_zams_offset,	// a_old
			b,								// b
			alpha,							// alpha
			OEConstants.ZAMS_MREF,			// mref_old
			mref							// mref_new
		));

		// In the case relative_zams == true, check that we can recover the relative zams as expected

		if (relative_zams) {
			double recovered_zams = Math.log10 (ten_ams_q / ten_a_q);
			if (Math.abs (recovered_zams - zams) > 0.000001) {
				throw new IllegalStateException ("OEVerFitAccum.calc_derived_point_values - Failed to recover relative zams: zams = " + zams + ", recovered_zams = " + recovered_zams);
			}
		}

		// Correct background rate to our reference magnitude

		mu = OEStatsCalc.calc_mu_new_from_mref_new (
			zmu,					// mu_old
			b,						// b
			OEConstants.ZMU_MREF,	// mref_old
			mref					// mref_new
		);
		
		return;
	}




	// Calculate the derived voxel values.
	// These are values computable from b, alpha, c, p, and n (plus configuration values).
	// Note: See OEDisc2InitStatVox and OEDisc2InitFitInfo for correction conversions.

	private void calc_derived_voxel_values () {
		beta = b * C_LOG_10;
		v_term = (alpha - b) * C_LOG_10;
		q_correction = Math.exp(v_term*(mref - mag_min)) * calc_w(v_term, msup - mref) / calc_w(v_term, mag_max - mag_min);
		
		// Corrected aftershock productivity

		ten_a_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
			n,
			p,
			c,
			b,
			alpha,
			mref,
			mag_min,
			mag_max,
			tint_br
		);

		// Corrected interval productivity is taken equal to aftershock productivity

		ten_aint_q = ten_a_q;
		
		return;
	}




	// Calculate the derived sub-voxel values.
	// These are values computable from ten_a_q, zams, and zmu (plus voxel and configuration values).
	// Note: See OEDisc2InitStatVox and OEDisc2InitFitInfo for correction conversions.

	private void calc_derived_subvox_values () {
	
		// Get offset to convert relative zams to absolute zams, or zero if zams is absolute

		if (relative_zams) {

			// Notes:
			// The function calc_zams_from_br returns a value such that
			//
			// 10^rel_to_abs_zams_offset == (10^a)*Q * 10^((alpha - b)*(ZAMS_MREF - mref))

			rel_to_abs_zams_offset = OEStatsCalc.calc_zams_from_br (
				n,
				p,
				c,
				b,
				alpha,
				mag_min,
				mag_max,
				tint_br
			);
		}
		else {
			rel_to_abs_zams_offset = 0.0;
		}

		// Convert from zams to (10^ams)*Q, with Q == 1
		// Notes:
		// The function calc_a_new_from_mref_new returns a value such that
		//
		// ten_ams_q == 10^(zams + rel_to_abs_zams_offset + (alpha - b)*(mref - ZAMS_MREF))
		//
		// In the case relative_zams == true, combining the above formulas gives
		//
		// ten_ams_q == (10^zams) * (10^a)*Q
		//
		//           == (10^(a + zams))*Q
		//
		// and we see that zams functions as a productivity relative to the aftershock productivity.

		ten_ams_q = Math.pow (10.0, OEStatsCalc.calc_a_new_from_mref_new (
			zams + rel_to_abs_zams_offset,	// a_old
			b,								// b
			alpha,							// alpha
			OEConstants.ZAMS_MREF,			// mref_old
			mref							// mref_new
		));

		// In the case relative_zams == true, check that we can recover the relative zams as expected

		if (relative_zams) {
			double recovered_zams = Math.log10 (ten_ams_q / ten_a_q);
			if (Math.abs (recovered_zams - zams) > 0.000001) {
				throw new IllegalStateException ("OEVerFitAccum.calc_derived_subvox_values - Failed to recover relative zams: zams = " + zams + ", recovered_zams = " + recovered_zams);
			}
		}

		// Correct background rate to our reference magnitude

		mu = OEStatsCalc.calc_mu_new_from_mref_new (
			zmu,					// mu_old
			b,						// b
			OEConstants.ZMU_MREF,	// mref_old
			mref					// mref_new
		);
		
		return;
	}




	// Allocate and zero-initialize the pre-computed terms and accumulators.

	private void alloc_accums () {

		// Per-rupture pre-computed terms

		a_t_rup_part_like = new double[rupture_count];
		OEArraysCalc.zero_array (a_t_rup_part_like);

		a_s_rup_k = new OEVerFitLinFcnAccum (rupture_count);

		// Per-interval pre-computed terms

		a_t_int_part_like = new double[interval_count];
		OEArraysCalc.zero_array (a_t_int_part_like);

		a_t_int_part_prod = new double[interval_count];
		OEArraysCalc.zero_array (a_t_int_part_prod);

		// Per-rupture accumulators

		a_t_rup_lambda_over_beta = new OEVerFitLinFcnAccum (rupture_count);

		// Per-interval accumulators

		a_t_int_integrated_lambda_over_beta = new OEVerFitLinFcnAccum (interval_count);

		a_s_int_ks = new OEVerFitLinFcnAccum (interval_count);

		// Total accumulators

		a_total_integrated_lambda_over_beta = new OEVerFitLinFcnAccum (1);

		return;
	}




	// Calculate the partial likelihood for a target rupture.
	// Parameters:
	//  mct = Magnitude of completeness of the target rupture.
	// See comments above definition of a_t_rup_part_like.

	private double calc_t_rup_part_like (double mct) {
		double part_like;

		switch (lmr_opt) {

		default:
			throw new IllegalArgumentException ("OEVerFitAccum.calc_t_rup_part_like - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
		case LMR_OPT_MCT_INFINITY:
			// From time-dependent magnitude of completeness to infinity
			part_like = Math.pow(10.0, b*(mref - mct));
			break;
			
		case LMR_OPT_MCT_MAG_MAX:
			// From time-dependent magnitude of completeness to maximum simulation magnitude
			// (We force the range to be at least 0.5 magnitude)
			part_like = Math.pow(10.0, b*(mref - mct)) * (- Math.expm1(beta*(Math.min(-0.5, mct - mag_max))));
			break;
			
		case LMR_OPT_MAGCAT_INFINITY:
			// From catalog magnitude of completeness to infinity
			part_like = Math.pow(10.0, b*(mref - magCat));
			break;
			
		case LMR_OPT_MAGCAT_MAG_MAX:
			// From catalog magnitude of completeness to maximum simulation magnitude
			// (We force the range to be at least 0.5 magnitude)
			part_like = Math.pow(10.0, b*(mref - magCat)) * (- Math.expm1(beta*(Math.min(-0.5, magCat - mag_max))));
			break;
		}

		return part_like;
	}




	// Calculate the partial likelihood for a target interval.
	// Parameters:
	//  mct = Magnitude of completeness of the target interval.
	//  duration = Duration of the target interval = t2 - t1.
	// See comments above definition of a_t_int_part_like.

	private double calc_t_int_part_like (double mct, double duration) {
		double part_like;

		switch (lmr_opt) {

		default:
			throw new IllegalArgumentException ("OEVerFitAccum.calc_t_int_part_like - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
		case LMR_OPT_MCT_INFINITY:
			// From time-dependent magnitude of completeness to infinity
			part_like = Math.pow(10.0, b*(mref - mct)) * duration;
			break;
			
		case LMR_OPT_MCT_MAG_MAX:
			// From time-dependent magnitude of completeness to maximum simulation magnitude
			// (We force the range to be at least 0.5 magnitude)
			part_like = Math.pow(10.0, b*(mref - mct)) * duration * (- Math.expm1(beta*(Math.min(-0.5, mct - mag_max))));
			break;
			
		case LMR_OPT_MAGCAT_INFINITY:
			// From catalog magnitude of completeness to infinity
			part_like = Math.pow(10.0, b*(mref - magCat)) * duration;
			break;
			
		case LMR_OPT_MAGCAT_MAG_MAX:
			// From catalog magnitude of completeness to maximum simulation magnitude
			// (We force the range to be at least 0.5 magnitude)
			part_like = Math.pow(10.0, b*(mref - magCat)) * duration * (- Math.expm1(beta*(Math.min(-0.5, magCat - mag_max))));
			break;
		}

		return part_like;
	}




	// Calculate the partial productivity for a target interval.
	// Parameters:
	//  mct = Magnitude of completeness of the target interval.
	// See comments above definition of a_t_int_part_prod.

	private double calc_t_int_part_prod (double mct) {
		double part_prod;

		switch (lmr_opt) {

		default:
			throw new IllegalArgumentException ("OEVerFitAccum.calc_t_int_part_prod - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
		case LMR_OPT_MCT_INFINITY:
		case LMR_OPT_MCT_MAG_MAX:
			// Lower limit is time-dependent magnitude of completeness
			if (mag_min < mct) {
				part_prod = calc_w(v_term, mct - mag_min) * beta * Math.pow(10.0, (alpha - b)*(mag_min - mref));
			} else {
				part_prod = 0.0;
			}
			break;
			
		case LMR_OPT_MAGCAT_INFINITY:
		case LMR_OPT_MAGCAT_MAG_MAX:
			// Lower limit is catalog magnitude of completeness
			if (mag_min < magCat) {
				part_prod = calc_w(v_term, magCat - mag_min) * beta * Math.pow(10.0, (alpha - b)*(mag_min - mref));
			} else {
				part_prod = 0.0;
			}
			break;
		}

		return part_prod;
	}




	//  // Calculate the productivity k for a source rupture.
	//  // Parameters:
	//  //  ms = Magnitude of the source rupture.
	//  //  is_main = True if the rupture is categorized as a mainshock or foreshock.
	//  // See comments above definition of a_s_rup_k.
	//  
	//  private double calc_s_rup_k (double ms, boolean is_main) {
	//  	return (is_main ? ten_ams_q : ten_a_q) * Math.pow (10.0, alpha*(ms - mref));
	//  }




	// Calculate the productivity k for a source rupture.
	// Parameters:
	//  ms = Magnitude of the source rupture.
	// See comments above definition of a_s_rup_k.
	// Note: Factor of (10^a)*Q or (10^ams)*Q is left implicit.

	private double calc_s_rup_k (double ms) {
		return Math.pow (10.0, alpha*(ms - mref));
	}




	// Allocate and initialize output values.

	private void alloc_output () {
		log_like = 0.0;
		return;
	}




	//----- Consumers -----




	// Initialization for a source rupture.

	private Consumer<OEVerFitRupture> my_rup_source_init = (OEVerFitRupture rups) -> {

		// Index number of this source rupture

		final int irs = rups.get_rup_index();

		// Calculate and save the productivity k:
		// For aftershocks:
		//    10^(a + alpha*(ms - mref)) * Q
		// For mainshocks and foreshocks:
		//    10^(ams + alpha*(ms - mref)) * Q

		a_s_rup_k.add_aftr_or_main (
			irs,
			calc_s_rup_k (rups.get_rup_mag()),
			rups.get_rup_is_main()
		);

		return;
	};




	// Initialization for a target rupture.

	private Consumer<OEVerFitRupture> my_rup_target_init = (OEVerFitRupture rupt) -> {

		// Index number of this target rupture

		final int irt = rupt.get_rup_index();

		// Calculate and save the partial likelihood:
		//   10^(-b*(mct - mref))

		a_t_rup_part_like[irt] = calc_t_rup_part_like (rupt.get_rup_mc());

		// Add the background rate term to the value of lambda/beta for this target rupture
		//   10^(-b*(mct - mref))

		a_t_rup_lambda_over_beta.add_bkgd (irt, a_t_rup_part_like[irt]);

		return;
	};




	// Initialization for a target/source interval.

	private Consumer<OEVerFitInterval> my_int_target_init = (OEVerFitInterval intt) -> {

		// Index number of this target interval

		final int iit = intt.get_int_index();

		// Calculate and save the partial likelihood:
		//   10^(-b*(mct - mref)) * (t2 - t1)

		a_t_int_part_like[iit] = calc_t_int_part_like (intt.get_int_mc(), intt.get_int_duration());

		// Calculate and save the partial productivity:
		//   beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		// except that the value is zero if mag_min >= mct.

		a_t_int_part_prod[iit] = calc_t_int_part_prod (intt.get_int_mc());

		// Add the background rate term to the intergrated lambda/beta
		//   10^(-b*(mct - mref)) * (t2 - t1)

		a_t_int_integrated_lambda_over_beta.add_bkgd (iit, a_t_int_part_like[iit]);

		// Add the background rate term to the productivity density
		//   (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		// except that the value is zero if mag_min >= mct.

		if (f_intervals) {
			a_s_int_ks.add_bkgd (iit, ten_aint_q * a_t_int_part_prod[iit]);
		}

		return;
	};




	// Interaction for a rupture source and rupture target.

	private BiConsumer<OEVerFitRupture, OEVerFitRupture> my_rup_rup_action = (OEVerFitRupture rups, OEVerFitRupture rupt) -> {

		// Index number of this source rupture

		final int irs = rups.get_rup_index();

		// Index number of this target rupture

		final int irt = rupt.get_rup_index();

		// Contribution to target rupture lambda(t, mct)/beta
		//   10^(alpha*(ms - mref)) * 10^(-b*(mct - mref)) * ((t - s + c)^(-p))

		final double t = rupt.get_rup_t_day();
		final double s = rups.get_rup_t_day();
		final double ss = a_t_rup_part_like[irt] * Math.pow (t - s + c, -p);
		a_t_rup_lambda_over_beta.add_src (irt, a_s_rup_k, irs, ss);

		return;
	};




	// Interaction for an interval source and rupture target.

	private BiConsumer<OEVerFitInterval, OEVerFitRupture> my_int_rup_action = (OEVerFitInterval ints, OEVerFitRupture rupt) -> {

		// Index number of this source interval

		final int iis = ints.get_int_index();

		// Index number of this target rupture

		final int irt = rupt.get_rup_index();

		// Contribution to target rupture lambda(t, mct)/beta
		//   ks * 10^(-b*(mct - mref)) * Integral(s = s1, s = min(t,s2); (t - s + c)^(-p) * ds)

		final double t = rupt.get_rup_aligned_time();
		final double s1 = ints.get_int_time_1();
		final double s2 = Math.min (ints.get_int_time_2(), t);
		final double ss = a_t_rup_part_like[irt] * OEOmoriCalc.omext_single_integral (p, t - s2 + c, s2 - s1);
		a_t_rup_lambda_over_beta.add_src (irt, a_s_int_ks, iis, ss);
		
		return;
	};




	// Interaction for a rupture source and an interval target.

	private BiConsumer<OEVerFitRupture, OEVerFitInterval> my_rup_int_action = (OEVerFitRupture rups, OEVerFitInterval intt) -> {

		// Index number of this source rupture

		final int irs = rups.get_rup_index();

		// Index number of this target interval

		final int iit = intt.get_int_index();

		// Omori density integral of the source rupture over the target interval
		//   Integral(t = max(t1,s), t = t2; (t - s + c)^(-p) * dt) / (t2 - t1)
		
		final double s = rups.get_rup_aligned_time();
		final double t1 = intt.get_int_time_1();
		final double t2 = intt.get_int_time_2();

		double omint = 0.0;

		// Case where rupture is before the interval

		if (s <= t1) {
			omint = OEOmoriCalc.omext_single_density_integral (p, t1 + c - s, t2 - t1);
		}

		// Case where rupture is inside the interval but definitely spaced away from the end
		// Note: Here we know s > t1 and therefore the ratio (t2 - s)/(t2 - t1) is less than 1, and t2 - t1 >= TINY_DURATION_DAYS.

		else if (t2 - s > TINY_DURATION_DAYS) {
			omint = ((t2 - s)/(t2 - t1)) * OEOmoriCalc.omext_single_density_integral (p, c, t2 - s);
		}

		// Contribution to target interval integrated lambda(t, mct)/beta
		//   10^(alpha*(ms - mref)) * 10^(-b*(mct - mref)) * Integral(t = max(t1,s), t = t2; (t - s + c)^(-p) * dt)

		final double ss1 = a_t_int_part_like[iit] * omint;
		a_t_int_integrated_lambda_over_beta.add_src (iit, a_s_rup_k, irs, ss1);

		// Contribution to the target interval productivity density
		//   10^(alpha*(ms - mref)) * (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		//   * Integral(t = max(t1,s), t = t2; (t - s + c)^(-p) * dt) / (t2 - t1)

		// Only for rupture before the interval

		if (f_intervals) {
			if (rups.compare_to_interval(intt) < 0) {
				final double ss2 = a_t_int_part_prod[iit] * ten_aint_q * omint;
				a_s_int_ks.add_src (iit, a_s_rup_k, irs, ss2);
			}
		}

		return;
	};




	// Interaction for an interval source and an interval target.

	BiConsumer<OEVerFitInterval, OEVerFitInterval> my_int_int_action = (OEVerFitInterval ints, OEVerFitInterval intt) -> {

		// Index number of this source interval

		final int iis = ints.get_int_index();

		// Index number of this target interval

		final int iit = intt.get_int_index();

		// Omori density integral of the source interval over the target interval
		//  Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * ds * dt) / (t2 - t1)

		final double s1 = ints.get_int_time_1();
		final double s2 = ints.get_int_time_2();
		final double t1 = intt.get_int_time_1();
		final double t2 = intt.get_int_time_2();

		final double omint = OEOmoriCalc.omext_double_density_integral (p, t1 - s2 + c, s2 - s1, t2 - t1);

		// Contribution to target interval integrated lambda(t, mct)/beta
		//   ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * dt * ds)

		final double ss1 = a_t_int_part_like[iit] * omint;
		a_t_int_integrated_lambda_over_beta.add_src (iit, a_s_int_ks, iis, ss1);

		// Contribution to the target interval productivity density
		//  ks * beta * 10^(a + (alpha - b)*(mag_min - mref)) * Q * W(v, mct - mag_min)
		//  * Integral(s = s1, s = s2; t = t1, t = t2; (t - s + c)^(-p) * ds * dt) / (t2 - t1)

		if (f_intervals) {
			final double ss2 = a_t_int_part_prod[iit] * ten_aint_q * omint * c_cross_intervals;
			a_s_int_ks.add_src (iit, a_s_int_ks, iis, ss2);
		}

		return;
	};




	// All calls to a target interval completed

	Consumer<OEVerFitInterval> my_int_target_done = (OEVerFitInterval intt) -> {

		// Index number of this target interval

		final int iit = intt.get_int_index();

		// Omori density integral of the source/target interval over itself
		//   Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * ds * dt) / (s2 - s1)

		final double s1 = intt.get_int_time_1();
		final double s2 = intt.get_int_time_2();

		final double omint = OEOmoriCalc.omext_self_double_density_integral (p, c, s2 - s1);

		// Contribution to the target interval productivity density from self-interaction
		//   ks * (10^aint)*Q * beta * 10^((alpha - b)*(mag_min - mref)) * W(v, mct - mag_min)
		//   * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * ds * dt) / (s2 - s1)

		if (f_intervals) {
			final double ss2 = a_t_int_part_prod[iit] * ten_aint_q * omint * c_self_intervals;
			//a_s_int_ks.add_src (iit, a_s_int_ks, iit, ss2);
			a_s_int_ks.apply_scale (iit, ss2 + 1.0);
		}

		// Contribution to target interval integrated lambda(t, mct)/beta
		//   ks * (10^(-b*(mct - mref))) * Integral(s = s1, s = s2; t = s, t = s2; (t - s + c)^(-p) * dt * ds)

		final double ss1 = a_t_int_part_like[iit] * omint;
		a_t_int_integrated_lambda_over_beta.add_src (iit, a_s_int_ks, iit, ss1);

		// Add target interval integrated lambda(t, mct)/beta to the global accumulator

		a_total_integrated_lambda_over_beta.add_src (0, a_t_int_integrated_lambda_over_beta, iit, 1.0);

		return;
	};




	// Accumulate log likelihood for a target rupture.

	private Consumer<OEVerFitRupture> my_rup_target_log_like = (OEVerFitRupture rupt) -> {

		// Index number of this target rupture

		final int irt = rupt.get_rup_index();

		// Accumulate the log of the intensity function

		log_like += Math.log (a_t_rup_lambda_over_beta.eval_fcn (irt, ten_a_q, ten_ams_q, mu));

		return;
	};




	//----- Operations -----




	// Execute interactions for a voxel.
	// Assumes that configuration parameters and b, alpha, c, p, n are already set.

	private void exec_interactions () {

		// Calculate derived parameter values

		calc_derived_voxel_values();

		// Allocate and zero-initialize accumulators

		alloc_accums();

		// Initialize for each source rupture

		history.for_each_source_rupture (my_rup_source_init);

		// Initialize for each target rupture

		history.for_each_like_rupture (my_rup_target_init);

		// Initialize for each target/source interval

		history.for_each_like_interval (my_int_target_init);

		// Run all interactions 

		history.for_each_interaction_v2 (
			my_rup_rup_action,
			my_int_rup_action,
			my_rup_int_action,
			my_int_int_action,
			my_int_target_done
		);

		return;
	}




	// Execute evaluation for a subvoxel.
	// Assumes that ten_a_q, zams, zmu are already set.

	private void exec_evaluation () {

		// Calculate derived parameter values
		
		calc_derived_subvox_values();

		// Allocate and initialize output values

		alloc_output();

		// Accumulate likelihood for each target rupture

		history.for_each_like_rupture (my_rup_target_log_like);

		// Subtract the integrated Likelihood

		log_like -= a_total_integrated_lambda_over_beta.eval_fcn (0, ten_a_q, ten_ams_q, mu);

		return;
	}




	// Run interactions for a voxel.

	public void run_interactions (
		double b,
		double alpha,
		double c,
		double p,
		double n
	) {

		// Save the parameters

		this.b     = b;
		this.alpha = alpha;
		this.c     = c;
		this.p     = p;
		this.n     = n;

		// Execute interactions

		exec_interactions();

		return;
	}




	// Run evaluation for a subvoxel.

	public void run_evaluation (
		double ten_a_q,
		double zams,
		double zmu
	) {

		// Save the parameters

		this.ten_a_q = ten_a_q;
		this.zams    = zams;
		this.zmu     = zmu;

		// Execute evaluation

		exec_evaluation();

		return;
	}




	// Run evaluation for a subvoxel, with default ten_a_q.

	public void run_evaluation (
		double zams,
		double zmu
	) {
		run_evaluation (ten_aint_q, zams, zmu);
		return;
	}




	// Get the calculated log-likelihood

	public double get_log_like () {
		return log_like;
	}




	// Set the voxel in which to operate.
	// A voxel is defined by values of b, alpha, c, p, and n.

	@Override
	public void ver_set_voxel (
		double b,
		double alpha,
		double c,
		double p,
		double n
	) {
		run_interactions (
			b,
			alpha,
			c,
			p,
			n
		);

		return;
	}




	// Calculate the log-likelihood for a subvoxel.

	@Override
	public double ver_subvox_log_like (
		double zams,
		double zmu
	) {
		run_evaluation (
			zams,
			zmu
		);

		return get_log_like();
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEVerFitAccum");







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
