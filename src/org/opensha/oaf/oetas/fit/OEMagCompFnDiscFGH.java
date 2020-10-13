package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Arrays;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;				// natural log of 10
import static org.opensha.oaf.oetas.OEConstants.C_MILLIS_PER_DAY;		// milliseconds per day
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value


/**
 * Time-dependent magnitude of completeness function -- Discrete function using F,G,H parameters for multiple earthquakes.
 * Author: Michael Barall 07/07/2020.
 *
 * This class represents a time-dependent magnitude of completeness function
 * governed by Helmstetter parameters F, G, and H.  The Helmstetter function is
 * discretized, so that it is piecewise-constant;  this eliminates the need for any
 * numerical integrations.  Multiple earthquakes can contribute to the incompleteness.
 * At any time, the magnitude of completeness is the maximum of the discretized
 * Helmstetter function for any of the considered earthquakes.
 *
 * In addition, this class also selects the earthquakes that are above the time-dependent
 * magnitude of completeness.  This must be done concurrently with building the function,
 * since the function itself depends on the selected earthquakes.
 *
 * The Helmstetter function is clipped at the magnitude of the earthquake, so an
 * earthquake never blocks detection of a larger earthquake.  This is necessary for
 * the stability of the algorithms, and to avoid situations where a large earthquake
 * is discarded because a smaller earthquake occurs just before.
 *
 * In practice, the function would be built from only a few of the largest earthquakes.
 *
 * ==================================
 *
 * The original Helmstetter formula for magnitude of completeness following an earthquake is:
 *
 *   Mc(t) = F * M0 - G - H * log10(t - t0)
 *
 * Here the earthquake has magnitude M0 and occurs at time t0, and F, G, and H are parameters.
 *
 * Typical California parameters are F = 1.00, G = 4.50, H = 0.75.
 * Typical World parameters are F = 0.50, G = 0.25, H = 1.00.
 *
 * ==================================
 *
 * For an earthquake i of magnitude Mi occurring at time ti, the "raw" Helmstetter
 * function is
 *
 *   f(t; i) = Mi   if   t <= tfi
 *
 *   f(t; i) = F * Mi - G - H * log10(t - ti)   if   t >= tfi
 *
 * where tfi is the falloff time of the i-th rupture,
 *
 *   tfi = ti + 10^(((F - 1) * Mi - G) / H)
 *
 * This is equivalent to
 *
 *   f(t; i) = min (Mi, F * Mi - G - H * log10(t - ti))   if   t > ti
 *
 * Refer to class OEMagCompFnMultiFGH for discussion of this function.
 *
 * Introduce a discretization function of magnitude D(M).  It is required to
 * satisfy the two conditions
 *
 *   If  M1 >= M2  then  D(M1) >= D(M2)
 *
 *   D(M) >= magCat
 *
 * The first condition says that D is (non-strictly) monotone increasing,
 * and the second condition says it is bounded below by magCat.
 *
 * In practice, we construct the function D by defining a discrete set of possible
 * values for D(M), with magCat as the lowest possible value.  The mapping M -> D(M)
 * is then defined by an operation such as floor, ceiling, or nearest.
 *
 * The discretized magnitude of completeness is then defined to be
 *
 *   Df(t; i) = min (Mi, D(F * Mi - G - H * log10(t - ti)))   if   t > ti
 *
 * Define the discretized falloff time to be
 *
 *   tdfi = inf (t | D(F * Mi - G - H * log10(t - ti)) < Mi)
 *
 * Then
 *
 *   Df(t; i) = Mi   if   t < tdfi
 *
 *   Df(t; i) = D(F * Mi - G - H * log10(t - ti))  if   t > tdfi
 *
 * The value at t = tdfi is dependent on the choice of open/closed interval endpoints.
 * We choose to define D(M) as having a constant value on a set of intervals, each
 * of which is closed on the left and open on the right.  With this choice, the
 * first formula becomes
 *
 *   Df(t; i) = Mi   if   t <= tdfi
 *
 * It is also useful to define the discretized completion time tdci as the time when
 * the discretized function becomes equal to magCat.
 *
 *   tdci = inf (t | D(F * Mi - G - H * log10(t - ti)) == magCat)
 *
 * Then
 *
 *   Df(t; i) = magCat   if   t > tdci
 *
 * Functions defined in this way satisfy the requirements of the skyline algorithm,
 * provided that each rupture magnitude is greater than or equal to the magnitude of
 * completeness of prior ruptures.  For additional formulas see OEMagCompFnMultiFGH.
 */
public class OEMagCompFnDiscFGH extends OEMagCompFnDisc {

	//----- Parameters -----


	// Catalog magnitude of completeness.

	// protected double magCat;		// inherited

	// Helmstetter Parameters.
	// Use capG = 100.0 to disable time-dependent magnitude of completeness.

	private double capF;
	private double capG;
	private double capH;

	// Beginning and end of the time range of interest, in days.

	private double t_range_begin;
	private double t_range_end;

	// Amount by which time range is expanded to allow for rounding errors, in days.
	// Note: We construct a magnitude of completeness function that is valid for
	//  t_range_begin - T_RANGE_EXPAND <= t <= t_range_end + T_RANGE_EXPAND.
	//  This allows for times that might be slightly outside the declared time range
	//  due to rounding errors.
	// Note: We use t_future = t_range_end + 2*T_RANGE_EXPAND to mean "far future",
	//  a time distinctly after any time at which the function needs to be evaluated.
	//  Some calculations may have poor numerical properties when attempting to
	//  determine very large times, and in those cases, if we can determine that the
	//  result is >= t_future then we can simply use t_future as the result.

	private static final double T_RANGE_EXPAND = 1.0;

	// The minimum allowed value of capH.

	private static final double MIN_CAP_H = 0.1;

	// The minimum allowed delta between discrete values, in magnitude units.

	private static final double MIN_DISC_DELTA = 0.001;




	//----- Data structures -----
	//
	// The time range is partitioned into N intervals.  Within each interval,
	// the magnitude of completeness is a constant.
	// The function definition is held in two arrays.
	//
	// For the n-th interval, 0 <= n < N, the data is defined as follows.
	//
	// * For n >= 1, the start time of the interval is a_time[n-1].
	//   Note that the first interval implicitly begins at -infinity, and the last
	//   interval implicitly extends to +infinity.  Also, note that intervals are
	//   open at the left and closed at the right, so the n-th interval is defined
	//   by a_time[n-1] < t <= a_time[n].
	//
	// * The function is constant in the interval, and a_mag[n].

	// Array of length N-1, containing the start time of each interval (after the first).

	// protected double[] a_time;		// inherited

	// Array of length N, containing the value of a constant function.

	// double[] a_mag;		// inherited


	// Get the number of intervals.

	// int get_interval_count ();		// inherited


	// Create a string describing the n-th interval.

	// protected String get_interval_string (int n);		// inherited




	//----- Evaluation -----


	// Calculate the time-dependent magnitude of completeness.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the magnitude of completeness at the given time.
	// Note: This function does not define the origin of time.
	// Note: The returned value must be >= the value returned by get_mag_cat().
	// It is expected that as t_days becomes large, the returned value
	// equals or approaches the value returned by get_mag_cat()

	// @Override
	// public double get_mag_completeness (double t_days);		// inherited




	// Get the catalog magnitude of completeness.
	// Returns the magnitude of completeness in normal times.

	// @Override
	// public double get_mag_cat ();		// inherited




	//----- Building, Ruptures -----




	// Nested class used during building to represent one rupture.

	protected static class BldRupture {

		// Rupture time, in days.

		public double b_t_day;

		// Rupture magnitude.
		// It is guaranteed that b_rup_mag >= magCat.

		public double b_rup_mag;

		// The greatest index number used to discretize the log function.
		// This is the greatest index number with a value less than b_rup_mag.
		// Note: The number of discretization levels of the log function is:
		//   b_disc_upper_index - disc_lower_index + 1
		// It is guaranteed that this number is non-negative, so that
		//   b_disc_upper_index >= disc_lower_index - 1
		// A rupture has a log function if and only if b_disc_upper_index >= disc_lower_index.

		public int b_disc_upper_index;

		// The time of completeness, when the magnitude of completeness reaches magCat; in days.
		// Ordinarily, this is the time at which the log function reaches the lower cutoff
		// of the lowest index number used to discretize the log function (which is disc_lower_index).

		public double b_t_comp;

		// If true, then b_t_comp may be set earlier than ordinarily, because the ordinary value
		// would be too large.  In this case, b_t_comp contains a large value after the time range.

		public boolean b_early_comp;

		// The falloff time, when the magnitude of completeness falls below the discretized rupture magnitude; in days.
		// Ordinarily, this is the time at which the log function reaches the upper cutoff of the greatest
		// index number used to discretize the log function (which is b_disc_upper_index).
		// Note: It is guaranteed that b_t_fall <= b_t_comp.
		// Note: For times between b_t_fall and b_t_comp, the magnitude of completeness is the
		// discretized log function.  For times before b_t_fall, the magnitude of completeness
		// is b_rup_mag.  For times after b_t_comp, the magnitude of completeness is magCat.
		// Note: If b_disc_upper_index < disc_lower_index then b_t_fall == b_t_comp
		// because there are no discretization levels for the log function.

		public double b_t_fall;

		// If true, then b_t_fall may be set earlier than ordinarily, because the ordinary value
		// would be too large.  In this case, b_t_fall contains a large value after the time range.
		// Note: If b_early_fall is false, then for times after b_t_fall until the end of the time
		// range, the mangitude of completeness is the discretized log function, with the rule
		// that magnitudes below the lower cutoff of disc_lower_index are discretized to magCat.

		public boolean b_early_fall;

		// Expiration time, when the magnitude of completeness falls below an earlier earthquake, in days.

		public double b_t_expire;
	}




	//----- Building, Skyline -----




	// Inner class to construct intervals from BldRupture objects.

	protected class SkylineBuilder extends OERupSkyline<BldRupture> {


		//----- Data -----


		// The memory builder we use.

		protected MemoryBuilder memory_builder;

		// A BldRupture object that can be reused, or null if none.

		protected BldRupture reusable_bld_rup;

		// The common splitting function to use for our split times.

		protected OEMagCompFnDisc.SplitFn common_split_fn;

		// Flags indicating if splits are needed at the beginning and end of the time range.

		protected boolean f_need_begin_split;
		protected boolean f_need_end_split;

		// The list of discete values that the log functions can assume.
		// These are listed in increasing order.
		// The first element must be less than any magnitude of interest (e.g., less than magCat).
		// The last element omitted must be greater than any magnitude of interest (e.g., NO_MAG_POS_CHECK).

		protected double[] disc_value;

		// The list of cutoffs for the discrete values.
		// If disc_cutoff[i] <= M < disc_cutoff[i+1] then the discretized value of M is disc_value[i+1].
		// The length of disc_cutoff is one less than the length of disc_value.
		// Must satisfy disc_value[i] < disc_cutoff[i] <= disc_value[i+1].

		protected double[] disc_cutoff;

		// Minimum gap between magCat and any other constant values (magnitudes and discretized log values).
		// Any constant value must be strictly greater than magCat + disc_gap.
		// Must satisfy disc_gap >= 0.

		protected double disc_gap;

		// Lowest index number with a value greater than magCat (allowing for the requested minimum gap).
		// This is the lowest index number used to discretize a log function.
		// When a log function falls below the lower cutoff, the magnitude of completeness becomes magCat.

		protected int disc_lower_index;

		// The index number most recently used in discretizing a log function.
		// This is used to prevent "spikes" in the discretized log function.

		protected int disc_recent_index;




		//----- Internal functions -----




		// Allocate a BldRupture object, possibly reusing one.

		protected BldRupture alloc_bld_rup () {
			BldRupture result = reusable_bld_rup;
			if (result == null) {
				result = new BldRupture();
			} else {
				reusable_bld_rup = null;
			}
			return result;
		}




		// Indicate that a BldRupture object can be reused.

		protected void free_bld_rup (BldRupture bld_rup) {
			reusable_bld_rup = bld_rup;
			return;
		}




		// Get the index of the discetization interval for a given magnitude.
		// Parameters:
		//  mag = Magnitude.
		// Returns the discretization interval n for mag.
		// The discretized value of mag is disc_value[n].
		// Note: The function returns the value of n such that
		//   disc_cutoff[n-1] <= mag < disc_cutoff[n]   and   0 <= n <= disc_cutoff.length
		// pretending that
		//   disc_cutoff[-1] == -infinity   and   disc_cutoff[disc_cutoff.length] == +infinity 

		protected int get_disc_index (double mag) {
		
			// Binary search in cutoffs

			int lo = -1;
			int hi = disc_cutoff.length;

			// Preserve the condition disc_cutoff[lo] <= mag < disc_cutoff[hi]

			while (hi - lo > 1) {
				int mid = (hi + lo)/2;
				if (mag < disc_cutoff[mid]) {
					hi = mid;
				} else {
					lo = mid;
				}
			}

			// Return value index

			return hi;
		}




		// Get the index of the discetization interval for a given magnitude.
		// Parameters:
		//  mag = Magnitude.
		//  lower = Minimum index to return.
		//  upper = Maximum index to return.
		// Returns the discretization interval n for mag.
		// The discretized value of mag is disc_value[n].
		// Note: The function returns the value of n such that
		//   disc_cutoff[n-1] <= mag < disc_cutoff[n]   and   lower <= n <= upper
		// pretending that
		//   disc_cutoff[lower-1] == -infinity   and   disc_cutoff[upper] == +infinity 
		// Notes:
		//  Returns n if  lower < n < upper  and  disc_cutoff[n-1] <= mag < disc_cutoff[n]
		//  Returns upper if  lower == upper
		//  Returns lower if  lower < upper  and  mag < disc_cutoff[lower]
		//  Returns upper if  lower < upper  and  disc_cutoff[upper-1] <= mag
		//  Error if lower > upper
		// Note: The indended use of this function is to ensure that the returned index is
		// within a known range, when the function may be queried at or near a limit of
		// the range, to ensure that rounding errors do not cause the next higher or
		// lower index value to be returned.

		protected int get_disc_index (double mag, int lower, int upper) {

			// Range check

			if (lower > upper || lower < 0 || upper > disc_cutoff.length) {
				throw new IllegalArgumentException ("OEMagCompFnDiscFGH.SkylineBuilder.get_disc_index: Invalid index range: lower = " + lower + ", upper = " + upper);
			}
		
			// Binary search in cutoffs

			int lo = lower - 1;
			int hi = upper;

			// Preserve the condition disc_cutoff[lo] <= mag < disc_cutoff[hi]

			while (hi - lo > 1) {
				int mid = (hi + lo)/2;
				if (mag < disc_cutoff[mid]) {
					hi = mid;
				} else {
					lo = mid;
				}
			}

			// Return value index

			return hi;
		}




		// Get the index of the largest discretization value less than a given magnitude.
		// Parameters:
		//  mag = Magnitude.
		// Returns the maximum n such that disc_value[n] < mag.
		// It is guaranteed that n is interior to disc_value (1 <= n <= disc_value.length - 2)
		// so that the index refers to an interval with lower and upper cutoffs.

		protected int get_disc_strict_floor_index (double mag) {
		
			// Binary search in values

			int lo = -1;
			int hi = disc_value.length;

			// Preserve the condition disc_value[lo] < mag <= disc_value[hi]

			while (hi - lo > 1) {
				int mid = (hi + lo)/2;
				if (disc_value[mid] < mag) {
					lo = mid;
				} else {
					hi = mid;
				}
			}

			// Because disc_value extends below and above all magnitudes of interest,
			// we expect to obtain an interior value

			if (lo < 1 || hi >= disc_value.length) {
				throw new IllegalArgumentException ("OEMagCompFnDiscFGH.SkylineBuilder.get_disc_strict_floor_index: Magnitude out-of-range: mag = " + mag);
			}

			// Return value index

			return lo;
		}




		// Get the index of the smallest discretization value greater than a given magnitude.
		// Parameters:
		//  mag = Magnitude.
		// Returns the minimum n such that disc_value[n] > mag.
		// It is guaranteed that n is interior to disc_value (1 <= n <= disc_value.length - 2)
		// so that the index refers to an interval with lower and upper cutoffs.

		protected int get_disc_strict_ceil_index (double mag) {
		
			// Binary search in values

			int lo = -1;
			int hi = disc_value.length;

			// Preserve the condition disc_value[lo] <= mag < disc_value[hi]

			while (hi - lo > 1) {
				int mid = (hi + lo)/2;
				if (mag < disc_value[mid]) {
					hi = mid;
				} else {
					lo = mid;
				}
			}

			// Because disc_value extends below and above all magnitudes of interest,
			// we expect to obtain an interior value

			if (lo < 0 || hi >= disc_value.length - 1) {
				throw new IllegalArgumentException ("OEMagCompFnDiscFGH.SkylineBuilder.get_disc_strict_ceil_index: Magnitude out-of-range: mag = " + mag);
			}

			// Return value index

			return hi;
		}




		// Make the discretization tables, for evenly-spaced values.
		// Parameters:
		//  disc_base = Base magnitude.
		//  disc_delta = Delta between discrete values.
		//  disc_round = Rounding option for cutoffs: 0.0 = floor, 0.5 = nearest, 1.0 = ceiling.
		// This method creates disc_value and disc_cutoff.
		// The discrete values have the form
		//   disc_base + n*disc_delta
		// Cutoff values are computed as
		//   disc_cutoff[i] = disc_value[i+1] - disc_round*disc_delta
		// The effect is that if
		//   disc_base + (n - disc_round)*disc_delta <= M < disc_base + (n + 1 - disc_round)*disc_delta
		// then the discretized value of M is disc_base + n*disc_delta.

		protected void make_disc_even (double disc_base, double disc_delta, double disc_round) {

			// Parameter checks

			if (!( disc_base > NO_MAG_NEG_CHECK
				&& disc_base < NO_MAG_POS_CHECK
				&& disc_delta >= MIN_DISC_DELTA )) {

				throw new IllegalArgumentException ("OEMagCompFnDiscFGH.SkylineBuilder.make_disc_even: Invalid arguments: disc_base = " + disc_base + ", disc_delta = " + disc_delta + ", disc_round = " + disc_round);
			}

			// Find low value of n

			int lo = 0;
			while (disc_base + ((double)lo)*disc_delta > NO_MAG_NEG_CHECK) {
				--lo;
			}

			// Find high value of n

			int hi = 0;
			while (disc_base + ((double)hi)*disc_delta < NO_MAG_POS_CHECK) {
				++hi;
			}

			// Create the array of values

			disc_value = new double[hi - lo + 1];
			for (int n = lo; n <= hi; ++n) {
				disc_value[n - lo] = disc_base + ((double)n)*disc_delta;
			}

			// Create the array of cutoffs

			double disc_offset = disc_delta * disc_round;

			disc_cutoff = new double[disc_value.length - 1];
			for (int i = 0; i < disc_cutoff.length; ++i) {
				disc_cutoff[i] = disc_value[i + 1] - disc_offset;
			}

			return;
		}




		// Set discretization options.
		// Parameters:
		//  the_disc_gap = Minimum gap between magCat and any other constant values (magnitudes and discretized log values).
		// Note: Before calling this method, you must set up magCat, disc_value, and disc_cutoff.

		protected void set_disc_options (double the_disc_gap) {

			// Parameter check

			if (!( the_disc_gap >= 0.0 )) {
				throw new IllegalArgumentException ("OEMagCompFnDiscFGH.SkylineBuilder.set_disc_options: Invalid arguments: the_disc_gap = " + the_disc_gap);
			}

			// Check discretization is set up

			if (!( disc_value != null
				&& disc_cutoff != null )) {

				throw new IllegalStateException ("OEMagCompFnDiscFGH.SkylineBuilder.set_disc_options: Discretization tables are not set up");
			}
		
			// Save options

			disc_gap = the_disc_gap;

			// Get the lowest index with a value greater than magCat, allowing for minimum gap

			disc_lower_index = get_disc_strict_ceil_index (magCat + disc_gap);

			// Initialize the recent index to the lowest possible

			disc_recent_index = disc_lower_index;

			return;
		}




		//----- BldRupture operations -----




		// Fill in a BldRupture, given the time and magnitude.
		// Parameters:
		//  bld_rup = The BldRupture to fill.
		//  t_day = Rupture time, in days.
		//  rup_mag = Rupture magnitude.
		// Returns true if success.
		// Returns false if discretization could not be done because rup_mag is too small.
		//  In this case, the rupture should not be passed to the skyline algorithm
		//  because its completeness function is effectively equal to magCat.

		protected boolean fill_BldRupture (BldRupture bld_rup, double t_day, double rup_mag) {

			// Fill in the time and magnitude

			bld_rup.b_t_day = t_day;
			bld_rup.b_rup_mag = rup_mag;

			// Reject it if the magnitude is below magCat + disc_gap

			if (bld_rup.b_rup_mag <= magCat + disc_gap) {
				bld_rup.b_t_comp = t_day;
				bld_rup.b_early_comp = false;
				bld_rup.b_t_fall = t_day;
				bld_rup.b_early_fall = false;
				bld_rup.b_t_expire = t_day;
				return false;
			}

			// The upper index is the greatest index with a value below the magnitude

			bld_rup.b_disc_upper_index = get_disc_strict_floor_index (bld_rup.b_rup_mag - disc_gap);

			// The upper index should not be less than disc_lower_index-1, but if it is (due to rounding erros), adjust it

			if (bld_rup.b_disc_upper_index < disc_lower_index - 1) {
				bld_rup.b_disc_upper_index = disc_lower_index - 1;
			}

			// Compute the times

			// Use an effective maximum time which is at least a finite amount after the rupture time and time range

			double eff_max_t = Math.max (t_range_end + T_RANGE_EXPAND, bld_rup.b_t_day) + T_RANGE_EXPAND;
			double log_max_t_delta = Math.log (eff_max_t - bld_rup.b_t_day);

			// Time of completeness is when the log function reaches the lower cutoff of the lower index

			double cut_comp = disc_cutoff[disc_lower_index - 1];
			double r_comp = C_LOG_10 * (capF * bld_rup.b_rup_mag - capG - cut_comp) / capH;
			if (r_comp >= log_max_t_delta) {
				bld_rup.b_t_comp = eff_max_t;
				bld_rup.b_early_comp = true;
			} else {
				bld_rup.b_t_comp = bld_rup.b_t_day + Math.exp (r_comp);
				bld_rup.b_early_comp = false;
			}

			// Falloff time is when the log function reaches the upper cutoff of the upper index

			if (bld_rup.b_disc_upper_index == disc_lower_index - 1) {
				bld_rup.b_t_fall = bld_rup.b_t_comp;
				bld_rup.b_early_fall = bld_rup.b_early_comp;
			}
			else {
				double cut_fall = disc_cutoff[bld_rup.b_disc_upper_index];
				double r_fall = C_LOG_10 * (capF * bld_rup.b_rup_mag - capG - cut_fall) / capH;
				if (r_fall >= log_max_t_delta) {
					bld_rup.b_t_fall = eff_max_t;
					bld_rup.b_early_fall = true;
				} else {
					bld_rup.b_t_fall = bld_rup.b_t_day + Math.exp (r_fall);
					bld_rup.b_early_fall = false;
				}
			}

			// Initialize the expire time to the time of completeness

			bld_rup.b_t_expire = bld_rup.b_t_comp;

			// Return success

			return true;
		}




		// Fill in a BldRupture, from an OERupture.
		// Parameters:
		//  bld_rup = The BldRupture to fill.
		//  rup = The rupture, containing time and magnitude.
		// Returns true if success.
		// Returns false if discretization could not be done because rup_mag is too small.
		//  In this case, the rupture should not be passed to the skyline algorithm
		//  because its completeness function is effectively equal to magCat.

		protected boolean fill_BldRupture (BldRupture bld_rup, OERupture rup) {
			return fill_BldRupture (bld_rup, rup.t_day, rup.rup_mag);
		}




		// Get the discretization index of the log function, for a BldRupture.
		// Parameters:
		//  bld_rup = Rupture.  Must satisfy bld_rup.b_disc_upper_index >= disc_lower_index.
		//  t_delta = The time since the rupture time.  Must be > 0.
		// Returns the discretization index of the log function, constrained to
		//  lie between disc_lower_index and bld_rup.b_disc_upper_index inclusive.
		// This method operates on the log function only, ignoring cutoffs.
		// The resulting discretized log value is disc_value[n] where n is the return value.

		protected int disc_log_BldRupture (BldRupture bld_rup, double t_delta) {

			// Compute the raw magnitude of completeness, as the log function
			// (Note Math.log10 checks for t_delta > 0)

			double raw_mag_comp = capF * bld_rup.b_rup_mag - capG - capH * Math.log10(t_delta);

			// Compute the constrained discretization index
			// (Note get_dis_index checks for bld_rup.b_disc_upper_index >= disc_lower_index)

			int index = get_disc_index (raw_mag_comp, disc_lower_index, bld_rup.b_disc_upper_index);

			// Return the index

			return index;
		}




		// Find the intersection time for two BldRupture objects.
		// Parameters:
		//  bld_rup_2 = Rupture #2.  Its time must be >= the time of rupture #1.
		//  bld_rup_1 = Rupture #1.
		// Return value:
		//   1 = The function for bld_rup_2 is always >= the function for bld_rup_1,
		//       at least until distinctly after the time range of interest.
		//       A value >= t_range_end + 2*T_RANGE_EXPAND is stored into bld_rup_2.b_t_expire.
		//   0 = The function for bld_rup_2 is initially >= the function for bld_rup_1,
		//       but falls below it at some time within or shortly after the time range of
		//       interest.  That time is stored into bld_rup_2.b_t_expire.
		//       Because of the upper cutoff in the magnitude of completeness function,
		//       the intersection time is after bld_rup_2.b_t_fall, which means that the
		//       function for bld_rup_2 is >= the function for bld_rup_1 for some finite
		//       time (not a vanishingly small interval).
		//  -1 = The function for bld_rup_2 is initially < the function for bld_rup_1.
		//       A value < bld_rup_2.b_t_day is stored into bld_rup_2.b_t_expire.
		//       Because of the upper cutoff in the magnitude of completeness function, it
		//       is possible that there is some time at which the function for bld_rup_2
		//       is > the function for bld_rup_1, but in this case we consider bld_rup_2
		//       to be below the magnitude of completeness and so discard it.
		// Note: This routine considers the upper cutoff (at bld_rup_1.b_rup_mag and bld_rup_2.b_rup_mag)
		// to handle various special cases.  Explicit handling of the lower cutoff (at magCat)
		// is largely unnecessary, because the lower cutoff can be considered the result of a
		// slightly altered discretization function.

		protected int intersect_BldRupture (BldRupture bld_rup_2, BldRupture bld_rup_1) {

			// Get the difference in time between the ruptures (t2 - t1)

			double t_delta = bld_rup_2.b_t_day - bld_rup_1.b_t_day;

			// If negative (t2 < t1), ruptures are in the wrong order

			if (t_delta < 0.0) {
				throw new IllegalArgumentException ("OEMagCompFnDiscFGH.SkylineBuilder.intersect_BldRupture - Time out of order: t1 = " + bld_rup_1.b_t_day + ", t2 = " + bld_rup_2.b_t_day);
			}

			// Use an effective maximum time which is at least a finite amount after the rupture time and time range

			double eff_max_t = Math.max (t_range_end + T_RANGE_EXPAND, bld_rup_2.b_t_day) + T_RANGE_EXPAND;

			// Get the difference in magnitudes (M1 - M2)

			double mag_delta = bld_rup_1.b_rup_mag - bld_rup_2.b_rup_mag;

			// If negative or zero (M1 <= M2), then the fuction for rupture 2 is always greater or equal

			if (mag_delta <= 0.0) {
				bld_rup_2.b_t_expire = eff_max_t;
				return 1;
			}

			// Otherwise (M1 > M2), if before the falloff time for rupture 1,
			// of if the rupture 1 falloff time is after the end of the time range,
			// then the function for rupture 2 is less

			if (bld_rup_2.b_t_day <= bld_rup_1.b_t_fall
				|| bld_rup_1.b_early_fall ) {

				bld_rup_2.b_t_expire = bld_rup_2.b_t_day - T_RANGE_EXPAND;
				return -1;
			}

			// (We now know that t_delta is a finite (not vanishingly small) value)
			// If after the completion time for rupture 1, or if rupture 1 has no log function,
			// then the function for rupture 2 is always greater or equal

			if (bld_rup_2.b_t_day > bld_rup_1.b_t_comp
				|| bld_rup_1.b_disc_upper_index < disc_lower_index ) {		// second check is redundant

				bld_rup_2.b_t_expire = eff_max_t;
				return 1;
			}

			// (We now know that the time of rupture 2 is within the log function of rupture 1)
			// If below the magnitude of completeness for rupture 1, then the function for rupture 2 is less

			if (bld_rup_2.b_rup_mag < disc_value[disc_log_BldRupture (bld_rup_1, t_delta)]) {
				bld_rup_2.b_t_expire = bld_rup_2.b_t_day - T_RANGE_EXPAND;
				return -1;
			}

			// If the rupture 2 falloff time is after the end of the time range,
			// then the function for rupture 2 is always greater or equal

			if (bld_rup_2.b_early_fall) {
				bld_rup_2.b_t_expire = eff_max_t;
				return 1;
			}

			// (We now know that both ruptures have their ordinary falloff times, which means
			// both have identically-discretized log functions from the rupture 2 falloff time
			// until the end of the time range)
			// Test if log function intersection is past the maximum time

			double r = C_LOG_10 * capF * mag_delta / capH;

			if (Math.log1p(t_delta / (eff_max_t - bld_rup_2.b_t_day)) >= r) {
				bld_rup_2.b_t_expire = eff_max_t;
				return 1;
			}
	
			// (We now know that r is a finite (not vanishingly small) value)
			// Compute the log function intersection

			double t_logint = bld_rup_2.b_t_day + t_delta / Math.expm1(r);

			// Make it at least the rupture 2 falloff time, so it's within the log function

			if (t_logint < bld_rup_2.b_t_fall) {
				t_logint = bld_rup_2.b_t_fall;
			}

			// Return the intersection time

			bld_rup_2.b_t_expire = t_logint;
			return 0;
		}




		// Determine if a magnitude is >= the function value for a BldRupture.
		// Parameters:
		//  bld_rup = Rupture.
		//  t = Time.
		//  mag = Magnitude.
		// If mag is >= the function value at time t, then return the function value at time t.
		//  In this case, it is guaranteed that the return value is <= mag.
		// If mag is < the function value at time t, then return NO_MAG_POS.
		//  Check for this condition with the test x >= NO_MAG_POS_CHECK.
		// Note: This routine considers the upper cutoff (at bld_rup.b_rup_mag) but not the
		// lower cutoff (at magCat).  The reason is that the skyline algorithm enforces the
		// lower cutoff.

		protected double satisfies_BldRupture (BldRupture bld_rup, double t, double mag) {

			double result;

			// Get the difference in time (t - t0)

			double t_delta = t - bld_rup.b_t_day;

			// If before the falloff time, or if falloff time is beyond the time range ...

			if (t <= bld_rup.b_t_fall || bld_rup.b_early_fall) {

				// If magnitude is at least the rupture magnitude (mag >= M0), then function is satisfied, otherwise not

				if (mag >= bld_rup.b_rup_mag) {
					result = bld_rup.b_rup_mag;
				} else {
					result = NO_MAG_POS;
				}
			}

			// Otherwise, if after the completion time, or there is no log function ...

			else if (t > bld_rup.b_t_comp || bld_rup.b_disc_upper_index < disc_lower_index) {

				// If magnitude is at least the catalog magnitude (mag >= magCat), then function is satisfied, otherwise not

				if (mag >= magCat) {
					result = magCat;
				} else {
					result = NO_MAG_POS;
				}
			}

			// Otherwise, we are within the log function ...

			else {
			
				// (We now know that t_delta is a finite (not vanishingly small) value)
				// Get the discretized log function

				result = disc_value[disc_log_BldRupture (bld_rup, t_delta)];

				// If magnitude is less than this, then function is not satisfied

				if (mag < result) {
					result = NO_MAG_POS;
				}
			}

			return result;
		}




		//----- Hook functions -----




		// Report an interval.
		// Parameters:
		//  interval_t_lo = Beginning time of the interval.
		//  interval_t_hi = Ending time of the interval.
		//  rup = Rupture for the interval.  Can be null, if the baseline rupture is null.
		// This call indicates that for times interval_t_lo < t <= interval_t_hi, the largest
		//  function value is the function F(t; rup) associated with the given rupture.
		// This is called with strictly increasing values of interval_t_lo.
		// It is guaranteed that interval_t_lo < interval_t_hi.

		@Override
		protected void hook_report_interval (double interval_t_lo, double interval_t_hi, BldRupture rup) {
		
			// Handle magCat case

			if (rup == null) {

				// Just add a magCat interval

				memory_builder.add_interval_magCat (interval_t_lo);

				// Re-initialize the recent index to the lowest possible

				disc_recent_index = disc_lower_index;
				return;
			}

			// If the rupture contains no log function ...

			if (rup.b_disc_upper_index < disc_lower_index) {
			
				// All we can do is add a constant interval equal to the rupture magnitude

				memory_builder.add_interval_constant (interval_t_lo, rup.b_rup_mag);

				// Re-initialize the recent index to the lowest possible

				disc_recent_index = disc_lower_index;
				return;

			}

			// Current time

			double t = interval_t_lo;

			// If the interval contains the upper cutoff ...

			if (t < rup.b_t_fall) {
				
				// Add the constant interval

				memory_builder.add_interval_constant (t, rup.b_rup_mag);

				// Advance the time

				t = rup.b_t_fall;

				// Set recent discretization index to the next index to be checked

				disc_recent_index = rup.b_disc_upper_index;

				// If consumed entire interval, done

				if (t >= interval_t_hi) {
					return;
				}
			}

			// Otherwise, we're starting in the log function ...

			else {

				// Get the discretization index of the log function at the current time

				double t_delta = t - rup.b_t_day;
				int index = disc_log_BldRupture (rup, t_delta);

				// Set the recent discretization index to the first index to be checked
				// (The min avoids very short intervals if an interval endpoint happens
				// to be close to a discretization cutoff)

				disc_recent_index = Math.min (disc_recent_index, index);
			}

			// Until all discretization levels of the log function are processed ...

			for (;;) {

				// Add constant interval as the discrete value for the current index

				memory_builder.add_interval_constant (t, disc_value[disc_recent_index]);

				// If this is the lowest allowed index for log function discretization, stop

				if (disc_recent_index == disc_lower_index) {
					break;
				}

				// Advance time to the when the log function reaches the lower cutoff of the index
				// (The max ensures the time is non-decreasing)

				double cut = disc_cutoff[disc_recent_index - 1];
				double r = C_LOG_10 * (capF * rup.b_rup_mag - capG - cut) / capH;

				t = Math.max (t, rup.b_t_day + Math.exp (r));

				// If consumed entire interval, done

				if (t >= interval_t_hi) {
					break;
				}

				// Adjust index

				--disc_recent_index;
			}

			return;
		}




		// Calculate the expiration time for the given rupture.
		// Parameters:
		//  cur_rup = The current rupture, which is the rupture that has the highest function value
		//            at the current time.  It can be null if and only if the initial rupture is null.
		//  rup = Rupture for which the expiration time is to be computed.
		// Returns the value of E(rup) which is defined by (where t_cur is the current time):
		//  F(t, rup) >= F(t, cur_rup)  for  t_cur < t <= E(rup)
		//  F(t, rup) <= F(t, cur_rup)  for  E(rup) < t <= min(t_max, E(cur_rup))
		// In other words, the return value is the endpoint of the interval, starting at the
		//  current time, in which the function associated with rup lies above (or equal to)
		//  the function associated with cur_rup.
		// The following applies:
		//  * If E(rup) <= t_cur, then rup is discarded as it can never be the rupture with
		//    the largest function value (though possibly it could be tied).  Note that any
		//    return value <= t_cur has the same effect, so if the routine determines this
		//    condition holds then it can return any convenient value <= tcur, for example, t_min.
		//  * Else, if E(rup) >= min(t_max, E(cur_rup)), then cur_rup is discarded as it
		//    cannot be the rupture with the largest function value at any time after t_cur.
		//    In this case, the stack is popped, a new current rupture is established, and
		//    this call is repeated.  Note that any return value >= min(t_max, E(cur_rup))
		//    has the same effect, and the return value is not used again, so a precise value is
		//    not required, and any value satisfying this condition will do, for example, t_max.
		//  * Else, t_cur < E(rup) < min(t_max, E(cur_rup)).  In this case, rup is pushed
		//    onto the stack and becomes the new current rupture.  When the time passes E(rup),
		//    rup is popped off the stack and cur_rup resumes being the current rupture
		//    (unless things change because of other intervening ruptures).  In this case,
		//    the value of E(rup) must be stored by the subclass so it can be retrieved again
		//    by calling hook_get_t_expire().

		@Override
		protected double hook_calc_t_expire (BldRupture cur_rup, BldRupture rup) {

			// Note: For this class, the current time is equal to rup.b_t_day.
		
			// Handle magCat case

			if (cur_rup == null) {

				// We treat the portion of rup past its completion time as if it is slightly
				// less than magCat.  This ensures that no interval is ever reported for a
				// non-null rup that extends past its completion time, and all intervals that
				// should have value equal to magCat are reported using null rup.
				// So, we report the completion time as the expiration time.

				rup.b_t_expire = rup.b_t_comp;
			}

			// Otherwise we have a rupture ...

			else {

				// Calculate an intersection time where the function for rup transitions
				// from above to below the function for cur_rup.  Due to the discretization,
				// the intersection time is non-unique.
				// Note: The expiration time computed by intersect_BldRupture is
				// consistent with the requirements of this method.

				int iresult = intersect_BldRupture (rup, cur_rup);
			}

			// Return expiration time

			return rup.b_t_expire;
		}





		// Get the expiration time for the given rupture.
		// Parameters:
		//  rup = Rupture from which to obtain the expiration time.
		// Returns the value of E(rup) calculated by the most recent call to hook_calc_t_expire().
		// Note: This routine is not called until after at least one call to hook_calc_t_expire().

		@Override
		protected double hook_get_t_expire (BldRupture rup) {

			// Return expiration time

			return rup.b_t_expire;
		}




		// Notification that setup() has been called.
		// This is called at the end of setup().
		// This gives the subclass the opportunity to perform any initial actions.

		@Override
		protected void hook_setup () {
			return;
		}




		// Notification that finish_up() has been called.
		// This is called at the end of finish_up().
		// This gives the subclass the opportunity to perform any final actions.

		@Override
		protected void hook_finish_up () {
			return;
		}




		//----- Inherited functions -----




		// Get the minimum time.

		// protected double get_sky_t_min ()

		// Get the maximum time.

		// protected double get_sky_t_max ()




		// Get the initial rupture.  It can be null.

		// protected BldRupture get_initial_rup ()

		// Return true if the given rupture is the initial rupture.

		// protected boolean is_initial_rup (BldRupture rup)




		// Get the baseline rupture.  It can be null.

		// protected BldRupture get_baseline_rup ()

		// Return true if the given rupture is the baseline rupture.

		// protected boolean is_baseline_rup (BldRupture rup)




		// Get the current rupture.  It can be null if and only if the baseline rupture is null.

		// protected BldRupture get_current_rup ()




		// Get the current time.

		// protected double get_current_time ()




		// Get the interval time.

		// protected double get_interval_time ()




		// Set up to begin constructing the skyline function.
		// Parameters:
		//  t_min = Minimum time at which skyline function is required.
		//  t_max = Maximum time at which skyline function is required.
		//  init_rup = Initial rupture in effect at t_min.  Its function must be valid
		//             for all times with t_min <= t <= t_max.  It can be null.
		// Note: The initial rupture init_rup implicitly has E(init_rup) == t_max.
		//  This class will never call hook_get_t_expire(init_rup), so init_rup does
		//  not need to have the ability to store the value of E(init_rup).
		//  However, if init_rup is an "ordinary" object of class BldRupture, then it is
		//  recommended that the stored value of E(init_rup) be initialized to a
		//  value >= t_max.  Calls of the form hook_calc_t_expire(init_rup, rup) will occur.

		// protected void setup (double t_min, double t_max, BldRupture init_rup)




		// Advance the current time.
		// Parameters:
		//  t = New current time.

		// protected void advance_time (double t)




		// Supply a new rupture.
		// Parameters:
		//  rup = New rupture, cannot be null.
		// Returns true if the rupture is retained, meaning that there may be an interval with the rupture.
		// Returns false if the rupture is discarded, meaning the rup object may be re-used.
		// The function F(t; rup) is presumed valid for t > current_time.

		// protected boolean supply_rup (BldRupture rup)




		// Finish processing after the last rupture is supplied.

		// protected void finish_up ()




		//----- Construction -----




		// Set up to begin skyline construction.
		// Parameters:
		//  mag_eps = The difference between magnitudes which are indistinguishable.  See OEMagCompFnDisc.MemoryBuilder.
		//  time_eps = The difference between times which are indistinguishable.  See OEMagCompFnDisc.MemoryBuilder.
		//  disc_base = Discretization base magnitude.  See make_disc_even().
		//  disc_delta = Discretization delta between discrete values.  See make_disc_even().
		//  disc_round = Discretization rounding option for cutoffs: 0.0 = floor, 0.5 = nearest, 1.0 = ceiling.  See make_disc_even().
		//  the_disc_gap = Minimum gap between magCat and any other constant values (magnitudes and discretized log values).  See set_disc_options().
		//  split_fn = Splitting function, or null to disable all splitting.
		// Note: All parameters in the top-level class must be already set up.

		public void setup_skyline (double mag_eps, double time_eps, double disc_base, double disc_delta, double disc_round, double the_disc_gap, OEMagCompFnDisc.SplitFn split_fn) {
		
			// Set up the superclass, using null for the initial rupture

			double t_min = t_range_begin - T_RANGE_EXPAND;
			double t_max = t_range_end + T_RANGE_EXPAND;
			BldRupture init_rup = null;

			setup (t_min, t_max, init_rup);
		
			// Allocate and set up the memory builder

			memory_builder = new MemoryBuilder();
			memory_builder.setup_arrays (mag_eps, time_eps, split_fn != null);

			// Indicate no reuseable BldRupture

			reusable_bld_rup = null;

			// Save the splitting function

			common_split_fn = split_fn;

			// Set initial splitting function

			memory_builder.first_split_fn (common_split_fn);

			// Indicate we need splits at beginning and end of time range

			f_need_begin_split = true;
			f_need_end_split = true;

			// Set up the discretization tables

			make_disc_even (disc_base, disc_delta, disc_round);

			// Set up discretization options

			set_disc_options (the_disc_gap);

			return;
		}




		// Finish constructing the skyline.

		public void finish_up_skyline () {

			// If need beginning split, do the split now

			if (f_need_begin_split) {
				memory_builder.add_split (Math.max (t_range_begin, get_current_time()), null);
				f_need_begin_split = false;
			}

			// If need ending split, do the split now

			if (f_need_end_split) {
				memory_builder.add_split (Math.max (t_range_end, get_current_time()), null);
				f_need_end_split = false;
			}

			// Finish up the superclass.

			finish_up();

			// Finish the arrays

			memory_builder.finish_up_arrays();

			// Discard the memory builder

			memory_builder = null;

			// Discard the discretization tables

			disc_value = null;
			disc_cutoff = null;

			return;
		}




		// Offer a new rupture.
		// Parameters:
		//  t_day = Rupture time in days.
		//  rup_mag = Rupture magnitude.
		//  f_eligible = True if rupture is eligible to participate in the skyline.
		//  f_division = True if a split is required for this rupture.
		// If the rupure is accepted (magnitude is at least the current magnitude of
		//  completeness, and time is within the allowed range), then return the current
		//  magnitude of completeness.
		// If the rupture is not accepted, then return NO_MAG_POS.
		//  Check for this condition with the test x >= NO_MAG_POS_CHECK.
		// Note: Calling with f_eligible false can be used to check if a rupture is above the
		//  current magnitude of completeness without using it in the skyline construction.
		// Note: Ruptures must be offered in non-decreasing order of time.
		// Note: If the rupture is accepted, f_eligible is true, and the rupture is retained in
		//  the skyline, then a split is inserted using common_split_fn as the splitting function.
		//  Otherwise, if the rupture is accepted and f_division is true, then a split is
		//  inserted with null splitting function.
		//  Otherwise, no split is inserted (except as needed at the beginning and end of The
		//  time range.)

		public double offer_rupture (double t_day, double rup_mag, boolean f_eligible, boolean f_division) {

			// Filter by time and magCat

			if (!( t_day > get_sky_t_min()
				&& t_day < get_sky_t_max()
				&& rup_mag >= magCat )) {
				
				return NO_MAG_POS;
			}

			// Get the time epsilon

			double time_eps = memory_builder.get_time_eps();

			// If need beginning split, and our time is past the beginning, do the split now

			if (f_need_begin_split) {
				if (t_day > t_range_begin + time_eps) {
					memory_builder.add_split (Math.max (t_range_begin, get_current_time()), null);
					f_need_begin_split = false;
				}
			}

			// If need ending split, and our time is past the ending, do the split now

			if (f_need_end_split) {
				if (t_day > t_range_end + time_eps) {
					memory_builder.add_split (Math.max (t_range_end, get_current_time()), null);
					f_need_end_split = false;
				}
			}

			// If time-independent ...

			if (capG > 99.999) {
			
				// If want to split at this time ...

				if (f_eligible || f_division) {
				
					// Produce the split, with split function if marked eligible

					if (f_eligible) {
						memory_builder.add_split (t_day, common_split_fn);
					} else {
						memory_builder.add_split (t_day, null);
					}

					// Clear split flags that this split satisfies

					if (f_need_begin_split) {
						if (t_day >= t_range_begin - time_eps) {
							f_need_begin_split = false;
						}
					}

					if (f_need_end_split) {
						if (t_day > t_range_end - time_eps) {
							f_need_end_split = false;
						}
					}
				}

				// Magnitude of completeness is magCat

				return magCat;
			}

			// Advance to requested time

			advance_time (t_day);

			// Get the current rupture, or null if magCat

			BldRupture bld_rup = get_current_rup();

			// If not satisfying magnitude of completeness, return not accepted

			double mag_comp = magCat;

			if (bld_rup != null) {
				mag_comp = satisfies_BldRupture (bld_rup, t_day, rup_mag);
				if (mag_comp >= NO_MAG_POS_CHECK) {
					return NO_MAG_POS;
				}
			}

			// Flag indicates if we did a split

			boolean f_did_split = false;

			// If eligible ...

			if (f_eligible) {

				// Allocate and set up a rupture object

				bld_rup = alloc_bld_rup();
				boolean f_retained = fill_BldRupture (bld_rup, t_day, rup_mag);

				// Supply it to the superclass, if discretization was OK

				if (f_retained) {
					f_retained = supply_rup (bld_rup);
				}

				// If rupture was retained, create a split at the rupture time, with a splitting function

				if (f_retained) {
					memory_builder.add_split (t_day, common_split_fn);
					f_did_split = true;
				}

				// If rupture was not retained, we can reuse the object

				if (!( f_retained )) {
					free_bld_rup (bld_rup);
				}
			}

			// If split is required and we have not done it, then create split at rupture time with no splitting function

			if (f_division && (!f_did_split)) {
				memory_builder.add_split (t_day, null);
				f_did_split = true;
			}

			// If we did a split, clear split flags that this split satisfies

			if (f_did_split) {
				if (f_need_begin_split) {
					if (t_day >= t_range_begin - time_eps) {
						f_need_begin_split = false;
					}
				}

				if (f_need_end_split) {
					if (t_day > t_range_end - time_eps) {
						f_need_end_split = false;
					}
				}
			}

			// Return accepted

			return mag_comp;
		}

	}




	//----- Building, Application Level -----




	// Build from a list of OERupture objects.
	// Parameters:
	//  rup_list = List of OERupture objects.
	//  accept_list = List to fill in with accepted OERupture objects, or null if not required.
	//  reject_list = List to fill in with rejected OERupture objects, or null if not required.
	//  eligible_mag = Minimum magnitude for eligible ruptures, or NO_MAG_NEG if no minimum
	//                 (all ruptures eligible), or NO_MAG_POS if no ruptures are eligible.
	//  eligible_count = Maximum number of eligible ruptures, or 0 if no limit (limit is flexible);
	//                   the minimum magnitude is increased if necessary to reduce the number of ruptures.
	//  mag_eps = The difference between magnitudes which are indistinguishable.  See OEMagCompFnDisc.MemoryBuilder.
	//  time_eps = The difference between times which are indistinguishable.  See OEMagCompFnDisc.MemoryBuilder.
	//  disc_base = Discretization base magnitude.  See make_disc_even().
	//  disc_delta = Discretization delta between discrete values.  See make_disc_even().
	//  disc_round = Discretization rounding option for cutoffs: 0.0 = floor, 0.5 = nearest, 1.0 = ceiling.  See make_disc_even().
	//  disc_gap = Minimum gap between magCat and any other constant values (magnitudes and discretized log values).  See set_disc_options().
	//  split_fn = Splitting function, or null to disable all splitting.
	//  mag_cat_count = Maximum number of ruptures that can have magnitudes >= magCat, or 0 if no limit (limit is flexible);
	//                  the value of magCat is increased if necessary to reduce the number of ruptures.
	//  division_mag = Minimum magnitude for ruptures to force interval division, or NO_MAG_NEG if no minimum
	//                 (all ruptures force division), or NO_MAG_POS if no ruptures force division.
	//  division_count = Maximum number of ruptures that force division, or 0 if no limit (limit is flexible);
	//                   the minimum magnitude is increased if necessary to reduce the number of ruptures.
	// Note: All parameters must be already set up.
	// Note: In each OERupture object, the k_prod field is set equal to the pre-existing magnitude
	//  of completeness if the rupture is accepted, or NO_MAG_POS if the rupture is rejected.

	public void build_from_rup_list (Collection<OERupture> rup_list, Collection<OERupture> accept_list, Collection<OERupture> reject_list, double eligible_mag, int eligible_count,
									double mag_eps, double time_eps, double disc_base, double disc_delta, double disc_round, double disc_gap, OEMagCompFnDisc.SplitFn split_fn,
									int mag_cat_count, double division_mag, int division_count) {
	
		// Copy the given list so we can sort it

		ArrayList<OERupture> work_list = new ArrayList<OERupture>(rup_list);

		// Flag indicates if list is sorted by magnitude

		boolean f_mag_sorted = false;

		// Effective eligible magnitude

		double eff_eligible_mag = eligible_mag;

		// If there is an eligible rupture count, and it limits the number of ruptures to consider ...

		if (eligible_count > 0 && work_list.size() > eligible_count) {
		
			// Sort the list by magnitude, descending

			if (!( f_mag_sorted )) {
				work_list.sort (new OERupture.MagDescTimeDescComparator());
				f_mag_sorted = true;
			}

			// Set effective eligible magnitude to enforce the count

			eff_eligible_mag = Math.max (eff_eligible_mag, work_list.get(eligible_count).rup_mag - 0.1*mag_eps);
		}

		// If there is a catalog count, and it limits the number of ruptures to consider ...

		if (mag_cat_count > 0 && work_list.size() > mag_cat_count) {
		
			// Sort the list by magnitude, descending

			if (!( f_mag_sorted )) {
				work_list.sort (new OERupture.MagDescTimeDescComparator());
				f_mag_sorted = true;
			}

			// Set effective catalog magnitude to enforce the count

			magCat = Math.max (magCat, work_list.get(mag_cat_count).rup_mag - 0.1*mag_eps);
		}

		// Effective division magnitude

		double eff_division_mag = division_mag;

		// If there is an division rupture count, and it limits the number of ruptures to consider ...

		if (division_count > 0 && work_list.size() > division_count) {
		
			// Sort the list by magnitude, descending

			if (!( f_mag_sorted )) {
				work_list.sort (new OERupture.MagDescTimeDescComparator());
				f_mag_sorted = true;
			}

			// Set effective division magnitude to enforce the count

			eff_division_mag = Math.max (eff_division_mag, work_list.get(division_count).rup_mag - 0.1*mag_eps);
		}
		
		// Sort the list by time, ascending

		work_list.sort (new OERupture.TimeAscMagDescComparator());

		// Create the skyline builder

		SkylineBuilder sky_builder = new SkylineBuilder();

		// Set it up

		sky_builder.setup_skyline (mag_eps, time_eps, disc_base, disc_delta, disc_round, disc_gap, split_fn);

		// Loop over ruptures ...

		for (OERupture rup : work_list) {
			double t_day = rup.t_day;
			double rup_mag = rup.rup_mag;

			// Rupture is eligible for skyline if it is at least the minimum magnitude

			boolean f_eligible = (rup_mag >= eff_eligible_mag);

			// Rupture forces division if it is at least the minimum magnitude

			boolean f_division = (rup_mag >= eff_division_mag);

			// Offer rupture to skyline, check if it is above mag of completeness

			rup.k_prod = sky_builder.offer_rupture (t_day, rup_mag, f_eligible, f_division);

			// Add rupture to accepted or rejected list

			if (rup.k_prod < NO_MAG_POS_CHECK) {
				if (accept_list != null) {
					accept_list.add (rup);
				}
			} else {
				if (reject_list != null) {
					reject_list.add (rup);
				}
			}
		}

		// Finish the skyline

		sky_builder.finish_up_skyline();

		return;
	}




	//----- Construction -----




	// Default constructor does nothing.

	public OEMagCompFnDiscFGH () {}




	// Construct from given parameters.

	public OEMagCompFnDiscFGH (double magCat, double capF, double capG, double capH,
								double t_range_begin, double t_range_end,
								Collection<OERupture> rup_list,
								Collection<OERupture> accept_list, Collection<OERupture> reject_list,
								double eligible_mag, int eligible_count,
								double mag_eps, double time_eps,
								double disc_base, double disc_delta, double disc_round, double disc_gap, OEMagCompFnDisc.SplitFn split_fn,
								int mag_cat_count, double division_mag, int division_count) {

		// Save parameters

		if (!( capH >= MIN_CAP_H )) {
			throw new IllegalArgumentException ("OEMagCompFnDiscFGH.OEMagCompFnDiscFGH: H parameter is invalid: H = " + capH);
		}

		if (!( t_range_begin < t_range_end )) {
			throw new IllegalArgumentException ("OEMagCompFnDiscFGH.OEMagCompFnDiscFGH: Time range is invalid: t_range_begin = " + t_range_begin + ", t_range_end = " + t_range_end);
		}

		this.magCat = magCat;
		this.capF = capF;
		this.capG = capG;
		this.capH = capH;
		this.t_range_begin = t_range_begin;
		this.t_range_end = t_range_end;

		// Generate the function with the skyline algorithm

		build_from_rup_list (rup_list, accept_list, reject_list, eligible_mag, eligible_count, mag_eps, time_eps, disc_base, disc_delta, disc_round, disc_gap, split_fn, mag_cat_count, division_mag, division_count);
	}




	// Construct from given parameters.

	public OEMagCompFnDiscFGH (OEDiscFGHParams params, Collection<OERupture> rup_list,
								Collection<OERupture> accept_list, Collection<OERupture> reject_list) {

		this (params.magCat, params.capF, params.capG, params.capH,
				params.t_range_begin, params.t_range_end,
				rup_list,
				accept_list, reject_list,
				params.eligible_mag, params.eligible_count,
				params.mag_eps, params.time_eps,
				params.disc_base, params.disc_delta, params.disc_round, params.disc_gap, params.split_fn,
				params.mag_cat_count, params.division_mag, params.division_count);
	}




	// Display our contents, short form.

	@Override
	public String toString() {
		
		return "FnDiscFGH[magCat=" + magCat
		+ ", capF=" + capF
		+ ", capG=" + capG
		+ ", capH=" + capH
		+ ", t_range_begin=" + t_range_begin
		+ ", t_range_end=" + t_range_end
		+ ", interval_count=" + get_interval_count()
		+ "]";
	}




	// Dump our entire contents to a string.

	public String dump_string () {
		StringBuilder result = new StringBuilder();

		int interval_count = get_interval_count();

		result.append ("OEMagCompFnDiscFGH:" + "\n");
		result.append ("magCat = " + magCat + "\n");
		result.append ("capF = " + capF + "\n");
		result.append ("capG = " + capG + "\n");
		result.append ("capH = " + capH + "\n");
		result.append ("t_range_begin = " + t_range_begin + "\n");
		result.append ("t_range_end = " + t_range_end + "\n");
		result.append ("interval_count = " + interval_count + "\n");
		for (int n = 0; n < interval_count; ++n) {
			result.append (get_interval_string (n) + "\n");
		}
		
		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 91001;

	private static final String M_VERSION_NAME = "OEMagCompFnDiscFGH";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_DISCFGH;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			writer.marshalDouble ("capF", capF);
			writer.marshalDouble ("capG", capG);
			writer.marshalDouble ("capH", capH);
			writer.marshalDouble ("t_range_begin", t_range_begin);
			writer.marshalDouble ("t_range_end", t_range_end);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("OEMagCompFnDiscFGH.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Don't pass to superclass

			// Get parameters

			OEDiscFGHParams params = OEDiscFGHParams.static_unmarshal (reader, "params");

			magCat = params.magCat;
			capF = params.capF;
			capG = params.capG;
			capH = params.capH;
			t_range_begin = params.t_range_begin;
			t_range_end = params.t_range_end;

			if (!( capH >= MIN_CAP_H )) {
				throw new MarshalException ("OEMagCompFnDiscFGH.do_umarshal: H parameter is invalid: H = " + capH);
			}

			if (!( t_range_begin < t_range_end )) {
				throw new MarshalException ("OEMagCompFnDiscFGH.do_umarshal: Time range is invalid: t_range_begin = " + t_range_begin + ", t_range_end = " + t_range_end);
			}

			// Array containing time and magnitude for each rupture

			double[] time_mag_array = reader.unmarshalDoubleArray ("time_mag_array");

			// Construct skyline for these ruptures

			try {

				// Make the OERupture objects with given times and magnitudes

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OERupture.make_time_mag_list (rup_list, time_mag_array);

				// Generate the function with the skyline algorithm

				Collection<OERupture> accept_list = null;
				Collection<OERupture> reject_list = null;

				// double eligible_mag = NO_MAG_NEG;
				// int eligible_count = 0;
				// OEMagCompFnDisc.SplitFn split_fn = null;
				// int mag_cat_count = 0;
				// double division_mag = NO_MAG_POS;
				// int division_count = 0;

				build_from_rup_list (rup_list, accept_list, reject_list, params.eligible_mag, params.eligible_count,
									params.mag_eps, params.time_eps, params.disc_base, params.disc_delta, params.disc_round, params.disc_gap, params.split_fn,
									params.mag_cat_count, params.division_mag, params.division_count);
			}
			catch (Exception e) {
				throw new MarshalException ("OEMagCompFnDiscFGH.do_umarshal: Unable to construct skyline function from given ruptures", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");
			t_range_begin = reader.unmarshalDouble ("t_range_begin");
			t_range_end = reader.unmarshalDouble ("t_range_end");

			if (!( capH >= MIN_CAP_H )) {
				throw new MarshalException ("OEMagCompFnDiscFGH.do_umarshal: H parameter is invalid: H = " + capH);
			}

			if (!( t_range_begin < t_range_end )) {
				throw new MarshalException ("OEMagCompFnDiscFGH.do_umarshal: Time range is invalid: t_range_begin = " + t_range_begin + ", t_range_end = " + t_range_end);
			}
		}
		break;

		}

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public OEMagCompFnDiscFGH unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEMagCompFnDiscFGH obj) {

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

	public static OEMagCompFnDiscFGH unmarshal_poly (MarshalReader reader, String name) {
		OEMagCompFnDiscFGH result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFnDiscFGH.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_DISCFGH:
			result = new OEMagCompFnDiscFGH();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEMagCompFnDiscFGH : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         query_time  query_delta  query_count  [t_day  rup_mag]...
		// Build a function with the given parameters and rupture list.
		// Perform queries at the specified set of times.
		// Display detailed results.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 17 or more additional arguments

			if (!( args.length >= 18 && args.length % 2 == 0 )) {
				System.err.println ("OEMagCompFnDiscFGH : Invalid 'test1' subcommand");
				return;
			}

			try {

				double magCat = Double.parseDouble (args[1]);
				double capF = Double.parseDouble (args[2]);
				double capG = Double.parseDouble (args[3]);
				double capH = Double.parseDouble (args[4]);
				double t_range_begin = Double.parseDouble (args[5]);
				double t_range_end = Double.parseDouble (args[6]);
				double eligible_mag = Double.parseDouble (args[7]);
				int eligible_count = Integer.parseInt (args[8]);

				double mag_eps = Double.parseDouble (args[9]);
				double time_eps = Double.parseDouble (args[10]);
				double disc_base = Double.parseDouble (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				double disc_round = Double.parseDouble (args[13]);
				double disc_gap = Double.parseDouble (args[14]);

				double query_time = Double.parseDouble (args[15]);
				double query_delta = Double.parseDouble (args[16]);
				int query_count = Integer.parseInt (args[17]);

				double[] time_mag_array = new double[args.length - 18];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 18]);
				}

				// Say hello

				System.out.println ("Generating magnitude of completeness function");
				System.out.println ("magCat = " + magCat);
				System.out.println ("capF = " + capF);
				System.out.println ("capG = " + capG);
				System.out.println ("capH = " + capH);
				System.out.println ("t_range_begin = " + t_range_begin);
				System.out.println ("t_range_end = " + t_range_end);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("mag_eps = " + mag_eps);
				System.out.println ("time_eps = " + time_eps);
				System.out.println ("disc_base = " + disc_base);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("disc_round = " + disc_round);
				System.out.println ("disc_gap = " + disc_gap);

				System.out.println ("query_time = " + query_time);
				System.out.println ("query_delta = " + query_delta);
				System.out.println ("query_count = " + query_count);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the rupture list

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OERupture.make_time_mag_list (rup_list, time_mag_array);

				System.out.println ();
				System.out.println ("rup_list:");
				for (OERupture rup : rup_list) {
					System.out.println ("  " + rup.u_time_mag_string());
				}

				// Make the magnitude of completeness function

				ArrayList<OERupture> accept_list = new ArrayList<OERupture>();
				ArrayList<OERupture> reject_list = new ArrayList<OERupture>();

				int mag_cat_count = 0;
				double division_mag = NO_MAG_POS;
				int division_count = 0;

				OEMagCompFnDiscFGH mag_comp_fn = new OEMagCompFnDiscFGH (magCat, capF, capG, capH,
								t_range_begin, t_range_end,
								rup_list,
								accept_list, reject_list,
								eligible_mag, eligible_count,
								mag_eps, time_eps,
								disc_base, disc_delta, disc_round, disc_gap, null,
								mag_cat_count, division_mag, division_count);

				System.out.println ();
				System.out.println (mag_comp_fn.dump_string());

				//System.out.println ();
				System.out.println ("accept_list:");
				for (OERupture rup : accept_list) {
					System.out.println ("  " + rup.u_time_mag_mc_string());
				}

				System.out.println ();
				System.out.println ("reject_list:");
				for (OERupture rup : reject_list) {
					System.out.println ("  " + rup.u_time_mag_string());
				}

				// Do queries

				System.out.println ();
				System.out.println ("queries:");

				for (int nq = 0; nq < query_count; ++nq) {
					double t = query_time + nq * query_delta;
					double mc = mag_comp_fn.get_mag_completeness (t);
					System.out.println ("  t = " + t + ", mc = " + mc);
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         query_time  query_delta  query_count 
		//         durlim_ratio  durlim_min  durlim_max
		//        [t_day  rup_mag]...
		// Build a function with the given parameters and rupture list.
		// Perform queries at the specified set of times.
		// Display detailed results.
		// This version does splitting.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 20 or more additional arguments

			if (!( args.length >= 21 && args.length % 2 == 1 )) {
				System.err.println ("OEMagCompFnDiscFGH : Invalid 'test2' subcommand");
				return;
			}

			try {

				double magCat = Double.parseDouble (args[1]);
				double capF = Double.parseDouble (args[2]);
				double capG = Double.parseDouble (args[3]);
				double capH = Double.parseDouble (args[4]);
				double t_range_begin = Double.parseDouble (args[5]);
				double t_range_end = Double.parseDouble (args[6]);
				double eligible_mag = Double.parseDouble (args[7]);
				int eligible_count = Integer.parseInt (args[8]);

				double mag_eps = Double.parseDouble (args[9]);
				double time_eps = Double.parseDouble (args[10]);
				double disc_base = Double.parseDouble (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				double disc_round = Double.parseDouble (args[13]);
				double disc_gap = Double.parseDouble (args[14]);

				double query_time = Double.parseDouble (args[15]);
				double query_delta = Double.parseDouble (args[16]);
				int query_count = Integer.parseInt (args[17]);

				double durlim_ratio = Double.parseDouble (args[18]);
				double durlim_min = Double.parseDouble (args[19]);
				double durlim_max = Double.parseDouble (args[20]);

				double[] time_mag_array = new double[args.length - 21];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 21]);
				}

				// Say hello

				System.out.println ("Generating magnitude of completeness function");
				System.out.println ("magCat = " + magCat);
				System.out.println ("capF = " + capF);
				System.out.println ("capG = " + capG);
				System.out.println ("capH = " + capH);
				System.out.println ("t_range_begin = " + t_range_begin);
				System.out.println ("t_range_end = " + t_range_end);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("mag_eps = " + mag_eps);
				System.out.println ("time_eps = " + time_eps);
				System.out.println ("disc_base = " + disc_base);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("disc_round = " + disc_round);
				System.out.println ("disc_gap = " + disc_gap);

				System.out.println ("query_time = " + query_time);
				System.out.println ("query_delta = " + query_delta);
				System.out.println ("query_count = " + query_count);

				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the rupture list

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OERupture.make_time_mag_list (rup_list, time_mag_array);

				System.out.println ();
				System.out.println ("rup_list:");
				for (OERupture rup : rup_list) {
					System.out.println ("  " + rup.u_time_mag_string());
				}

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the magnitude of completeness function

				ArrayList<OERupture> accept_list = new ArrayList<OERupture>();
				ArrayList<OERupture> reject_list = new ArrayList<OERupture>();

				int mag_cat_count = 0;
				double division_mag = NO_MAG_POS;
				int division_count = 0;

				OEMagCompFnDiscFGH mag_comp_fn = new OEMagCompFnDiscFGH (magCat, capF, capG, capH,
								t_range_begin, t_range_end,
								rup_list,
								accept_list, reject_list,
								eligible_mag, eligible_count,
								mag_eps, time_eps,
								disc_base, disc_delta, disc_round, disc_gap, split_fn,
								mag_cat_count, division_mag, division_count);

				System.out.println ();
				System.out.println (mag_comp_fn.dump_string());

				//System.out.println ();
				System.out.println ("accept_list:");
				for (OERupture rup : accept_list) {
					System.out.println ("  " + rup.u_time_mag_mc_string());
				}

				System.out.println ();
				System.out.println ("reject_list:");
				for (OERupture rup : reject_list) {
					System.out.println ("  " + rup.u_time_mag_string());
				}

				// Do queries

				System.out.println ();
				System.out.println ("queries:");

				for (int nq = 0; nq < query_count; ++nq) {
					double t = query_time + nq * query_delta;
					double mc = mag_comp_fn.get_mag_completeness (t);
					System.out.println ("  t = " + t + ", mc = " + mc);
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         query_time  query_delta  query_count 
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//        [t_day  rup_mag]...
		// Build a function with the given parameters and rupture list.
		// Perform queries at the specified set of times.
		// Display detailed results.
		// This version does splitting.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 23 or more additional arguments

			if (!( args.length >= 24 && args.length % 2 == 0 )) {
				System.err.println ("OEMagCompFnDiscFGH : Invalid 'test3' subcommand");
				return;
			}

			try {

				double magCat = Double.parseDouble (args[1]);
				double capF = Double.parseDouble (args[2]);
				double capG = Double.parseDouble (args[3]);
				double capH = Double.parseDouble (args[4]);
				double t_range_begin = Double.parseDouble (args[5]);
				double t_range_end = Double.parseDouble (args[6]);
				double eligible_mag = Double.parseDouble (args[7]);
				int eligible_count = Integer.parseInt (args[8]);

				double mag_eps = Double.parseDouble (args[9]);
				double time_eps = Double.parseDouble (args[10]);
				double disc_base = Double.parseDouble (args[11]);
				double disc_delta = Double.parseDouble (args[12]);
				double disc_round = Double.parseDouble (args[13]);
				double disc_gap = Double.parseDouble (args[14]);

				double query_time = Double.parseDouble (args[15]);
				double query_delta = Double.parseDouble (args[16]);
				int query_count = Integer.parseInt (args[17]);

				double durlim_ratio = Double.parseDouble (args[18]);
				double durlim_min = Double.parseDouble (args[19]);
				double durlim_max = Double.parseDouble (args[20]);

				int mag_cat_count = Integer.parseInt (args[21]);
				double division_mag = Double.parseDouble (args[22]);
				int division_count = Integer.parseInt (args[23]);

				double[] time_mag_array = new double[args.length - 24];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 24]);
				}

				// Say hello

				System.out.println ("Generating magnitude of completeness function");
				System.out.println ("magCat = " + magCat);
				System.out.println ("capF = " + capF);
				System.out.println ("capG = " + capG);
				System.out.println ("capH = " + capH);
				System.out.println ("t_range_begin = " + t_range_begin);
				System.out.println ("t_range_end = " + t_range_end);
				System.out.println ("eligible_mag = " + eligible_mag);
				System.out.println ("eligible_count = " + eligible_count);

				System.out.println ("mag_eps = " + mag_eps);
				System.out.println ("time_eps = " + time_eps);
				System.out.println ("disc_base = " + disc_base);
				System.out.println ("disc_delta = " + disc_delta);
				System.out.println ("disc_round = " + disc_round);
				System.out.println ("disc_gap = " + disc_gap);

				System.out.println ("query_time = " + query_time);
				System.out.println ("query_delta = " + query_delta);
				System.out.println ("query_count = " + query_count);

				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);

				System.out.println ("mag_cat_count = " + mag_cat_count);
				System.out.println ("division_mag = " + division_mag);
				System.out.println ("division_count = " + division_count);

				System.out.println ("time_mag_array:");
				for (int ntm = 0; ntm < time_mag_array.length; ntm += 2) {
					System.out.println ("  time = " + time_mag_array[ntm] + ", mag = " + time_mag_array[ntm + 1]);
				}

				// Make the rupture list

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OERupture.make_time_mag_list (rup_list, time_mag_array);

				System.out.println ();
				System.out.println ("rup_list:");
				for (OERupture rup : rup_list) {
					System.out.println ("  " + rup.u_time_mag_string());
				}

				// Make time-splitting function

				OEMagCompFnDisc.SplitFn split_fn = new OEMagCompFnDisc.SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the magnitude of completeness function

				ArrayList<OERupture> accept_list = new ArrayList<OERupture>();
				ArrayList<OERupture> reject_list = new ArrayList<OERupture>();

				// OEMagCompFnDiscFGH mag_comp_fn = new OEMagCompFnDiscFGH (magCat, capF, capG, capH,
				// 				t_range_begin, t_range_end,
				// 				rup_list,
				// 				accept_list, reject_list,
				// 				eligible_mag, eligible_count,
				// 				mag_eps, time_eps,
				// 				disc_base, disc_delta, disc_round, disc_gap, split_fn,
				// 				mag_cat_count, division_mag, division_count);

				OEDiscFGHParams params = new OEDiscFGHParams();

				params.set (
					magCat,
					capF,
					capG,
					capH,
					t_range_begin,
					t_range_end,

					mag_eps,
					time_eps,
					disc_base,
					disc_delta,
					disc_round,
					disc_gap,

					mag_cat_count,
					eligible_mag,
					eligible_count,
					division_mag,
					division_count,
					split_fn
				);

				OEMagCompFnDiscFGH mag_comp_fn = new OEMagCompFnDiscFGH (params, rup_list,
								accept_list, reject_list);

				System.out.println ();
				System.out.println (mag_comp_fn.dump_string());

				//System.out.println ();
				System.out.println ("accept_list:");
				for (OERupture rup : accept_list) {
					System.out.println ("  " + rup.u_time_mag_mc_string());
				}

				System.out.println ();
				System.out.println ("reject_list:");
				for (OERupture rup : reject_list) {
					System.out.println ("  " + rup.u_time_mag_string());
				}

				// Do queries

				System.out.println ();
				System.out.println ("queries:");

				for (int nq = 0; nq < query_count; ++nq) {
					double t = query_time + nq * query_delta;
					double mc = mag_comp_fn.get_mag_completeness (t);
					System.out.println ("  t = " + t + ", mc = " + mc);
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEMagCompFnDiscFGH : Unrecognized subcommand : " + args[0]);
		return;

	}

}
