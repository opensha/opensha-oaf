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
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value


/**
 * Time-dependent magnitude of completeness function -- using F,G,H parameters for multiple earthquakes.
 * Author: Michael Barall 02/16/2020.
 *
 * This class represents a time-dependent magnitude of completeness function
 * governed by Helmstetter parameters F, G, and H.  Multiple earthquakes can contribute
 * to the incompleteness.  At any time, the magnitude of completeness is the maximum
 * of the Helmstetter function for any of the considered earthquakes.
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
 * The Helmstetter formula for magnitude of completeness following an earthquake is:
 *
 *   Mc(t) = F * M0 - G - H * log10(t - t0)
 *
 * Here the earthquake has magnitude M0 and occurs at time t0, and F, G, and H are parameters.
 *
 * Typical California parameters are F = 1.00, G = 4.50, H = 0.75.
 * Typical World parameters are F = 0.50, G = 0.25, H = 1.00.
 *
 * Let Mcat be the catalog magnitude of completeness at normal times.
 * The time-dependent magnitude of completeness is clipped below at Mcat, so implicitly:
 *
 *   Mc(t) = max (F * M0 - G - H * log10(t - t0), Mcat)
 *
 * This formula equals Mcat for all time t >= tc, where the time of completeness tc is:
 *
 *   tc = t0 + 10^((F * M0 - G - Mcat) / H)
 *
 * Also, the time-dependent magnitude of completeness is clipped above at M0, so implicitly:
 *
 *   Mc(t) = min (max (F * M0 - G - H * log10(t - t0), Mcat), M0)
 *
 * This formula equals M0 for time t0 <= t <= tf, where the falloff time tf is:
 *
 *   tf = t0 + 10^(((F - 1) * M0 - G) / H)
 *
 * When there are multiple earthquakes, the overall magnitude of completeness is taken to be
 * the maximum of the individual magnitudes of completeness.
 *
 * ==================================
 *
 * We construct the overall magnitude of completeness function use the skyline algorithm
 * as implemented in OERupSkyline.  Suppose the i-th rupture has magnitude Mi and occurs
 * at time ti.  Ruptures are in time order, that is, tj <= ti if j < i.  For the i-th
 * rupture, define a function f(t; i) for t > ti as:
 *
 *   f(t; i) = Mi   if   t <= tfi
 *
 *   f(t; i) = F * Mi - G - H * log10(t - ti)   if   t >= tfi
 *
 * where tfi is the falloff time of the i-th rupture,
 *
 *   tfi = ti + 10^(((F - 1) * Mi - G) / H)
 *
 * Note that this formula accounts for the upper cutoff (at Mi) but not the lower
 * cutoff (at magCat).  This is because the skyline algorithm takes care of keeping
 * function values above magCat.
 *
 * We say the i-th rupture is "accepted" if Mi is at least the magnitude of completeness
 * at time ti.  In other words, if Mi >= magCat and also Mi >= f(ti; j) for j < i.
 * If only "accepted" ruptures are considered, then the functions f(t; i) satisfy
 * the requirements of OERupSkyline.
 *
 * Each rupture participating in the skyline algorithm is represented by an object
 * of class BldRupture.  The initial rupture in the skyline algorithm is represented
 * by null, and is a constant function equal to magCat.
 *
 * ==================================
 *
 * To perform the skyline algorithm, it is necessary to compute the intersections
 * of functions.  First, consider the intersection of a log function with a constant.
 * Specifically, consider a rupture of magnitude M0 occurring at time t0, and
 * compute the time when the corresponding log function equals M.  That is, solve
 *
 *   M == F * M0 - G - H * log10(tx - t0)
 *
 * The solution to this equation is
 *
 *   tx == t0 + 10^((F * M0 - G - M) / H)
 *
 * However, we want to avoid evaluating the above formula when the exponent is large.
 * The skyline algorithm does not require calculating times arbitrarily far into the
 * future.  Instead, if it can be determined that the intersection time is beyond
 * the largest time of interest, then the actual intersection time is not required.
 * If T represents the largest time of interest, we can show
 *
 *   tx >= T   iff   log(10) * (F * M0 - G - M) / H >= log(T - t0)
 *
 * The latter inequality can be evaluated to determine if the intersection time is
 * beyond the largest time of interest.  If not, then the exponent is small enough
 * so the intersection time can be computed without risk of overflow.
 *
 * Consider when the function f(t; 0) equals M.  If M >= M0, then there is no
 * intersection, because f(t; 0) <= M for all t > t0.  If M < M0, then the intersection
 * is when the log function equals M, which is given by tx in the formulas above.
 * In this case, f(t; 0) > M for t0 < t < tx, and f(t; 0) <= M for t >= tx.
 *
 * Now consider the intersection of two log function.  Suppose we have ruptures of
 * magnitude M1 and M2 occurring at times t1 and t2 respectively, with t2 >= t1.
 * The equation to be solved is
 *
 *   F * M1 - G - H * log10(tx - t1) == F * M2 - G - H * log10(tx - t2)
 *
 * The solution to this equation is
 *
 *   tx == t2 + (t2 - t1) / expm1(log(10) * F * (M1 - M2) / H)
 *
 * where expm1(x) = exp(x - 1).  Note that if M2 >= M1 then there is no solution,
 * because the denominator becomes zero or negative, and a negative denominator
 * would imply tx <= t2 which is impossible.
 *
 * So we assume M2 < M1.  We want to avoid evaluating tx when the denominator is
 * small.  If T represents the largest time of interest, we can show
 *
 *   tx >= T   iff   log1p((t2 - t2) / (T - t2)) >= log(10) * F * (M1 - M2) / H
 *
 * The latter inequality can be evaluated to determine if the intersection time is
 * beyond the largest time of interest.  If not, then the denominator is large enough
 * so the intersection time can be computed without risk of overflow.
 *
 * Consider when the function f(t; 2) equals f(t; 1).  If M2 >= M1, then there is no
 * intersection, because f(t; 2) >= f(t; 1) for all t > t2.  If M2 < M1, when we
 * need to invoke the assumption that rupture 2 is "accepted", which means that
 * M2 >= F * M1 - G - H * log10(t2 - t1), which implies t2 > tf1.  In this case,
 * the intersection is when the two log functions are equal, which is given by tx
 * in the formulas above.  In this case, f(t; 2) > f(t; 1) for t2 < t < tx, and
 * f(t; 2) <= f(t; 1) for t >= tx.
 */
public class OEMagCompFnMultiFGH extends OEMagCompFn {

	//----- Parameters -----


	// Catalog magnitude of completeness.

	private double magCat;

	// Helmstetter Parameters.

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




	//----- Data structures -----
	//
	// The time range is partitioned into N intervals.  Within each interval,
	// the magnitude of completeness is either a constant, or a logarithmic function.
	// The function definition is held in three arrays.
	//
	// For the n-th interval, 0 <= n < N, the data is defined as follows.
	//
	// * For n >= 1, the start time of the interval is a_time[n-1].
	//   Note that the first interval implicitly begins at -infinity, and the last
	//   interval implicitly extends to +infinity.  Also, note that intervals are
	//   open at the left and closed at the right, so the n-th interval is defined
	//   by a_time[n-1] < t <= a_time[n].
	//
	// * If the function is constant in the interval, then a_mag[n] is the constant value,
	//   and a_t0[n] is HUGE_TIME_DAYS*2.0.  (This allows the test a_t0[n] < HUGE_TIME_DAYS
	//   to decide if the function is constant or logarithmic).
	//
	// * If the function is logarithmic in the interval, then the function value is
	//   a_mag[n] - capH * log10(t - a_t0[n]).  This means a_mag[n] = capF * M0 - capG
	//   where M0 is the magnitude of the earthquake.  Note that a range check on t or
	//   on the function value is not required because the interval start and end
	//   times are limited to valid times.

	// Array of length N-1, containing the start time of each interval (after the first).

	private double[] a_time;

	// Array of length N, containing the value of a constant function, or the
	// constant term in a logarithmic function.

	private double[] a_mag;

	// Array of length N, containing HUGE_TIME_DAYS*2.0 for a constant function,
	// or the origin time for a logarithmic function.

	private double [] a_t0;


	// Get the number of intervals.

	private int get_interval_count () {
		if (a_mag == null) {
			return 0;
		}
		return a_mag.length;
	}


	// Create a string describing the n-th interval.

	private String get_interval_string (int n) {
		StringBuilder result = new StringBuilder();

		// Interval start time, but not on the first interval which starts at -infinity

		if (n > 0) {
			result.append("time = ").append(a_time[n-1]).append(": ");
		}

		// Log function

		if (a_t0[n] < HUGE_TIME_DAYS) {
			result.append("log: mag = ").append(a_mag[n]).append(", t0 = ").append(a_t0[n]);
		}

		// Constant function

		else {
			result.append("constant: mag = ").append(a_mag[n]);
		}
	
		return result.toString();
	}




	//----- Evaluation -----


	// Calculate the time-dependent magnitude of completeness.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the magnitude of completeness at the given time.
	// Note: This function does not define the origin of time.
	// Note: The returned value must be >= the value returned by get_mag_cat().
	// It is expected that as t_days becomes large, the returned value
	// equals or approaches the value returned by get_mag_cat()

	@Override
	public double get_mag_completeness (double t_days) {

		// Binary search to find the interval

		int lo = -1;				// implicitly, a_time[-1] = -infinity
		int hi = a_time.length;		// implicitly, a_time[length] = +infinity

		// Search, maintaining the condition a_time[lo] < t_days <= a_time[hi]

		while (hi - lo > 1) {
			int mid = (hi + lo)/2;
			if (a_time[mid] < t_days) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		// If this is a constant function, return it

		if (a_t0[hi] >= HUGE_TIME_DAYS) {
			return a_mag[hi];
		}

		// Return the logarithmic function

		return a_mag[hi] - capH * Math.log10(t_days - a_t0[hi]);
	}




	// Get the catalog magnitude of completeness.
	// Returns the magnitude of completeness in normal times.

	@Override
	public double get_mag_cat () {
		return magCat;
	}




	//----- Building, Memory Management -----




	// Inner class to manage memory.
	// The user must begin by calling setup_arrays.
	// Then, the user calls the various add_interval methods.
	// Then, the user must call finish_up_arrays to put the arrays into final form.

	protected class MemoryBuilder {

		// The current number of times, separating intervals.

		protected int time_count;

		// True if the last interval added was a magCat interval.

		protected boolean last_was_magCat;


		// Set up the arrays.

		public void setup_arrays () {

			// Default capacity, in number of times

			int default_capacity = 16;

			// Allocate the initial arrays

			a_time = new double[default_capacity];
			a_mag = new double[default_capacity + 1];
			a_t0 = new double[default_capacity + 1];

			// No times yet

			time_count = 0;

			// Insert an initial magCat interval

			last_was_magCat = true;
			a_mag[0] = magCat;
			a_t0[0] = HUGE_TIME_DAYS * 2.0;

			return;
		}


		// Add an interval, expanding the arrays if needed.

		private void add_interval (double time, double mag, double t0) {

			// Check for increasing time

			if (time_count > 0) {
				if (time <= a_time[time_count - 1]) {
					throw new IllegalArgumentException ("OEMagCompFnMultiFGH.MemoryBuilder.add_interval - Time out-of-order: time = " + time + ", last time = " + a_time[time_count - 1]);
				}
			}
		
			// Expand arrays if needed

			if (time_count >= a_time.length) {

				// Calculate new capacity required

				int default_capacity = 16;
				int new_capacity = Math.max (default_capacity, a_time.length) * 2;

				// Reallocate arrays

				a_time = Arrays.copyOf (a_time, new_capacity);
				a_mag = Arrays.copyOf (a_mag, new_capacity + 1);
				a_t0 = Arrays.copyOf (a_t0, new_capacity + 1);
			}

			// Insert the new interval

			a_time[time_count] = time;
			a_mag[time_count + 1] = mag;
			a_t0[time_count + 1] = t0;

			// Count it

			++time_count;
			return;
		}


		// Add a magCat interval.

		public void add_interval_magCat (double time) {

			// Add it if the previous interval is not a magCat interval

			if (!( last_was_magCat )) {
				add_interval (time, magCat, HUGE_TIME_DAYS * 2.0);
				last_was_magCat = true;
			}
		
			return;
		}


		// Add a constant interval.

		public void add_interval_constant (double time, double mag) {
			add_interval (time, mag, HUGE_TIME_DAYS * 2.0);
			last_was_magCat = false;
			return;
		}


		// Add a log interval, applying capF and capG.

		public void add_interval_log_FG (double time, double m0, double t0) {
			add_interval (time, capF * m0 - capG, t0);
			last_was_magCat = false;
			return;
		}


		// Finish up the arrays, by truncating to the exact length needed.

		public void finish_up_arrays () {
		
			// If arrays have extra space, trim them

			if (time_count < a_time.length) {
				a_time = Arrays.copyOf (a_time, time_count);
				a_mag = Arrays.copyOf (a_mag, time_count + 1);
				a_t0 = Arrays.copyOf (a_t0, time_count + 1);
			}

			return;
		}
	}




	//----- Building, Ruptures -----




	// Nested class used during building to represent one rupture.

	protected static class BldRupture {

		// Rupture time, in days.

		public double b_t_day;

		// Rupture magnitude.

		public double b_rup_mag;

		// The time of completeness, when the magnitude of completeness reaches magCat; in days.

		public double b_t_comp;

		// The falloff time, when the magnitude of completeness falls below the rupture magnitude; in days.
		// Note: It is guaranteed that b_t_fall <= b_t_comp.

		public double b_t_fall;

		// Expiration time, when the magnitude of completeness falls below an earlier earthquake, in days.

		public double b_t_expire;
	}




	// Fill in a BldRupture, given the time and magnitude.
	// Parameters:
	//  bld_rup = The BldRupture to fill.
	//  t_day = Rupture time, in days.
	//  rup_mag = Rupture magnitude.
	// Returns bld_rup.

	protected BldRupture fill_BldRupture (BldRupture bld_rup, double t_day, double rup_mag) {

		// Fill in the time and magnitude

		bld_rup.b_t_day = t_day;
		bld_rup.b_rup_mag = rup_mag;

		// Compute the times

		// Use an effective maximum time which is at least a finite amount after the rupture time and time range

		double eff_max_t = Math.max (t_range_end + T_RANGE_EXPAND, bld_rup.b_t_day) + T_RANGE_EXPAND;
		double log_max_t_delta = Math.log (eff_max_t - bld_rup.b_t_day);

		// Time of completeness is when the log function reaches magCat

		double r_comp = C_LOG_10 * (capF * bld_rup.b_rup_mag - capG - magCat) / capH;
		if (r_comp >= log_max_t_delta) {
			bld_rup.b_t_comp = eff_max_t;
		} else {
			bld_rup.b_t_comp = bld_rup.b_t_day + Math.exp (r_comp);
		}

		// Falloff time is when the log function reaches bld_rup

		double r_fall = C_LOG_10 * (capF * bld_rup.b_rup_mag - capG - bld_rup.b_rup_mag) / capH;
		if (r_fall >= log_max_t_delta) {
			bld_rup.b_t_fall = eff_max_t;
		} else {
			bld_rup.b_t_fall = bld_rup.b_t_day + Math.exp (r_fall);
		}

		// Initialize the expire time to the time of completeness

		bld_rup.b_t_expire = bld_rup.b_t_comp;

		return bld_rup;
	}




	// Fill in a BldRupture, from an OERupture.
	// Parameters:
	//  bld_rup = The BldRupture to fill.
	//  rup = The rupture, containing time and magnitude.
	// Returns bld_rup.

	protected BldRupture fill_BldRupture (BldRupture bld_rup, OERupture rup) {
		return fill_BldRupture (bld_rup, rup.t_day, rup.rup_mag);
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
	// but not the lower cutoff (at magCat).  The reason is that the skyline algorithm
	// enforces the lower cutoff.

	protected int intersect_BldRupture (BldRupture bld_rup_2, BldRupture bld_rup_1) {

		// Get the difference in time between the ruptures (t2 - t1)

		double t_delta = bld_rup_2.b_t_day - bld_rup_1.b_t_day;

		// If negative (t2 < t1), ruptures are in the wrong order

		if (t_delta < 0.0) {
			throw new IllegalArgumentException ("OEMagCompFnMultiFGH.intersect_BldRupture - Time out of order: t1 = " + bld_rup_1.b_t_day + ", t2 = " + bld_rup_2.b_t_day);
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

		// Otherwise (M1 > M2), if before the falloff time for rupture 1, then the function for rupture 2 is less

		if (bld_rup_2.b_t_day <= bld_rup_1.b_t_fall) {
			bld_rup_2.b_t_expire = bld_rup_2.b_t_day - T_RANGE_EXPAND;
			return -1;
		}

		// (We now know that t_delta is a finite (not vanishingly small) value)
		// If below the magnitude of completeness for rupture 1, then the function for rupture 2 is less

		if (bld_rup_2.b_rup_mag < capF * bld_rup_1.b_rup_mag - capG - capH * Math.log10(t_delta)) {
			bld_rup_2.b_t_expire = bld_rup_2.b_t_day - T_RANGE_EXPAND;
			return -1;
		}

		// Test if log function intersection is past the maximum time

		double r = C_LOG_10 * capF * mag_delta / capH;

		if (Math.log1p(t_delta / (eff_max_t - bld_rup_2.b_t_day)) >= r) {
			bld_rup_2.b_t_expire = eff_max_t;
			return 1;
		}
	
		// (We now know that r is a finite (not vanishingly small) value)
		// Compute the log function intersection

		bld_rup_2.b_t_expire = bld_rup_2.b_t_day + t_delta / Math.expm1(r);
		return 0;
	}




	// Find the intersection time for a constant magnitude and a BldRupture object.
	// Parameters:
	//  bld_rup = Rupture.
	//  mag = Constant magnitude.
	// Return value:
	//   1 = The function for bld_rup is always >= mag,
	//       at least until distinctly after the time range of interest.
	//       A value >= t_range_end + 2*T_RANGE_EXPAND is stored into bld_rup.b_t_expire.
	//   0 = The function for bld_rup is initially >= mag, but falls below (or becomes
	//       equal to) it at some time within or shortly after the time range of
	//       interest.  That time is stored into bld_rup.b_t_expire.
	//       Because of the upper cutoff in the magnitude of completeness function,
	//       the intersection time is after bld_rup.b_t_fall, which means that the
	//       function for bld_rup is >= the function for bld_rup_1 for some finite
	//       time (not a vanishingly small interval).
	//  -1 = The function for bld_rup is initially < mag.
	//       A value < bld_rup.b_t_day is stored into bld_rup.b_t_expire.
	// Note: This routine considers the upper cutoff (at bld_rup.b_rup_mag) but not the
	// lower cutoff (at magCat).  The reason is that the skyline algorithm enforces the
	// lower cutoff.

	protected int intersect_BldRupture (BldRupture bld_rup, double mag) {

		// Use an effective maximum time which is at least a finite amount after the rupture time and time range

		double eff_max_t = Math.max (t_range_end + T_RANGE_EXPAND, bld_rup.b_t_day) + T_RANGE_EXPAND;
		double log_max_t_delta = Math.log (eff_max_t - bld_rup.b_t_day);

		// Get the difference in magnitudes (mag - M)

		double mag_delta = mag - bld_rup.b_rup_mag;

		// If positive (mag > M), then the function for bld_rup is less

		if (mag_delta > 0.0) {
			bld_rup.b_t_expire = bld_rup.b_t_day - T_RANGE_EXPAND;
			return -1;
		}

		// Test if log function intersection is past the maximum time

		double r = C_LOG_10 * (capF * bld_rup.b_rup_mag - capG - mag) / capH;
		if (r >= log_max_t_delta) {
			bld_rup.b_t_expire = eff_max_t;
			return 1;
		}

		// Compute the log function intersection
			
		bld_rup.b_t_expire = bld_rup.b_t_day + Math.exp (r);
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

		// If magnitude is at least the rupture magnitude (mag >= M0), then function is satisfied ...

		if (mag >= bld_rup.b_rup_mag) {

			// If before the falloff time, the function value is the magnitude

			if (t <= bld_rup.b_t_fall) {
				result = bld_rup.b_rup_mag;
			}

			// Otherwise, the function value is the log

			else {
				// (We now know that t_delta is a finite (not vanishingly small) value)

				result = capF * bld_rup.b_rup_mag - capG - capH * Math.log10(t_delta);

				// Force result to not exceed mag, in case of rounding errors

				if (mag < result) {
					result = mag;
				}
			}
		}

		// Otherwise (mag < M), we need to compare with the function ...

		else {

			// If before the falloff time, then the function is not satisfied

			if (t <= bld_rup.b_t_fall) {
				result = NO_MAG_POS;
			}

			// Otherwise, if >= magnitude of completeness for bld_rup, then the function is satisfied

			else {
				// (We now know that t_delta is a finite (not vanishingly small) value)

				result = capF * bld_rup.b_rup_mag - capG - capH * Math.log10(t_delta);

				// If below magnitude of completeness, then not satisfied

				if (mag < result) {
					result = NO_MAG_POS;
				}
			}
		}

		return result;
	}




//	// Determine if a magnitude is >= the function value for a BldRupture.
//	// Parameters:
//	//  bld_rup = Rupture.
//	//  t = Time.
//	//  mag = Magnitude.
//	// Returns true if mag is >= the function value at time t.
//	// Returns false if mag is < the function value at time t.
//	// Note: This routine considers the upper cutoff (at bld_rup.b_rup_mag) but not the
//	// lower cutoff (at magCat).  The reason is that the skyline algorithm enforces the
//	// lower cutoff.
//
//	protected boolean satisfies_BldRupture (BldRupture bld_rup, double t, double mag) {
//
//		// Get the difference in time (t - t0)
//
//		double t_delta = t - bld_rup.b_t_day;
//
//		// Get the difference in magnitudes (mag - M0)
//
//		double mag_delta = mag - bld_rup.b_rup_mag;
//
//		// If positive (mag >= M0), then function is satisfied
//
//		if (mag_delta >= 0.0) {
//			return true;
//		}
//
//		// Otherwise (mag < M), if before the falloff time, then the function is not satisfied
//
//		if (t <= bld_rup.b_t_fall) {
//			return false;
//		}
//
//		// (We now know that t_delta is a finite (not vanishingly small) value)
//		// If >= magnitude of completeness for bld_rup, then the function is satisfied
//
//		if (mag >= capF * bld_rup.b_rup_mag - capG - capH * Math.log10(t_delta)) {
//			return true;
//		}
//
//		// Otherwise, not satisfied
//
//		return false;
//	}




	//----- Building, Skyline -----




	// Inner class to construct intervals from BldRupture objects.

	protected class SkylineBuilder extends OERupSkyline<BldRupture> {


		//----- Data -----


		// The memory builder we use.

		protected MemoryBuilder memory_builder;

		// A BldRupture object that can be reused, or null if none.

		protected BldRupture reusable_bld_rup;




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
				memory_builder.add_interval_magCat (interval_t_lo);
			}

			// Otherwise we have a rupture ...

			else {

				// If the interval contains a constant function ...

				if (interval_t_lo < rup.b_t_fall) {
				
					// Add the constant interval

					memory_builder.add_interval_constant (interval_t_lo, rup.b_rup_mag);

					// If there is also a log function interval, add it

					if (rup.b_t_fall < interval_t_hi) {
						memory_builder.add_interval_log_FG (rup.b_t_fall, rup.b_rup_mag, rup.b_t_day);
					}
				}

				// Otherwise, the interval contains a log function ...

				else {
					memory_builder.add_interval_log_FG (interval_t_lo, rup.b_rup_mag, rup.b_t_day);
				}
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

			int iresult;
			if (cur_rup == null) {
				iresult = intersect_BldRupture (rup, magCat);
			}

			// Otherwise we have a rupture ...

			else {
				iresult = intersect_BldRupture (rup, cur_rup);
			}

			// Note: The expiration time computed by intersect_BldRupture is
			// consistent with the requirements of this method.

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
		
			// Allocate and set up the memory builder

			memory_builder = new MemoryBuilder();
			memory_builder.setup_arrays();

			// Indicate no reuseable BldRupture

			reusable_bld_rup = null;

			return;
		}




		// Notification that finish_up() has been called.
		// This is called at the end of finish_up().
		// This gives the subclass the opportunity to perform any final actions.

		@Override
		protected void hook_finish_up () {

			// Finish the arrays

			memory_builder.finish_up_arrays();

			// Discard the memory builder

			memory_builder = null;

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
		// Note: All parameters in the top-level class must be already set up.

		public void setup_skyline () {
		
			// Set up the superclass, using null for the initial rupture

			double t_min = t_range_begin - T_RANGE_EXPAND;
			double t_max = t_range_end + T_RANGE_EXPAND;
			BldRupture init_rup = null;

			setup (t_min, t_max, init_rup);

			return;
		}




		// Finish constructing the skyline.

		public void finish_up_skyline () {
		
			// Finish up the superclass.

			finish_up();

			return;
		}




		// Offer a new rupture.
		// Parameters:
		//  t_day = Rupture time in days.
		//  rup_mag = Rupture magnitude.
		//  f_eligible = True if rupture is eligible to participate in the skyline.
		// If the rupure is accepted (magnitude is at least the current magnitude of
		//  completeness, and time is within the allowed range), then return the current
		//  magnitude of completeness.
		// If the rupture is not accepted, then return NO_MAG_POS.
		//  Check for this condition with the test x >= NO_MAG_POS_CHECK.
		// Note: Calling with f_eligible false can be used to check if a rupture is above the
		//  current magnitude of completeness without using it in the skyline construction.
		// Note: Ruptures must be offered in non-decreasing order of time.

		public double offer_rupture (double t_day, double rup_mag, boolean f_eligible) {

			// Filter by time and magCat

			if (!( t_day > get_sky_t_min()
				&& t_day < get_sky_t_max()
				&& rup_mag >= magCat )) {
				
				return NO_MAG_POS;
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

			// If eligible ...

			if (f_eligible) {

				// Allocate and set up a rupture object

				bld_rup = alloc_bld_rup();
				fill_BldRupture (bld_rup, t_day, rup_mag);

				// Supply it to the superclass

				boolean f_retained = supply_rup (bld_rup);

				// If rupture was not retained, we can reuse the object

				if (!( f_retained )) {
					free_bld_rup (bld_rup);
				}
			}

			// Return accepted

			return mag_comp;
		}

	}




//		// Offer a new rupture.
//		// Parameters:
//		//  t_day = Rupture time in days.
//		//  rup_mag = Rupture magnitude.
//		//  f_eligible = True if rupture is eligible to participate in the skyline.
//		// Returns true if the rupure is accepted (magnitude is at least the current magnitude
//		//  of completeness, and time is within the allowed range).
//		// Returns false if rupture is not accepted.
//		// Note: Calling with f_eligible false can be used to check if a rupture is above the
//		//  current magnitude of completeness without using it in the skyline construction.
//		// Note: Ruptures must be offered in non-decreasing order of time.
//
//		public boolean offer_rupture (double t_day, double rup_mag, boolean f_eligible) {
//
//			// Filter by time and magCat
//
//			if (!( t_day > get_sky_t_min()
//				&& t_day < get_sky_t_max()
//				&& rup_mag >= magCat )) {
//				
//				return false;
//			}
//
//			// Advance to requested time
//
//			advance_time (t_day);
//
//			// Get the current rupture, or null if magCat
//
//			BldRupture bld_rup = get_current_rup();
//
//			// If not satisfying magnitude of completeness, return not accepted
//
//			if (bld_rup != null) {
//				if (!( satisfies_BldRupture (bld_rup, t_day, rup_mag) )) {
//					return false;
//				}
//			}
//
//			// If eligible ...
//
//			if (f_eligible) {
//
//				// Allocate and set up a rupture object
//
//				bld_rup = alloc_bld_rup();
//				fill_BldRupture (bld_rup, t_day, rup_mag);
//
//				// Supply it to the superclass
//
//				boolean f_retained = supply_rup (bld_rup);
//
//				// If rupture was not retained, we can reuse the object
//
//				if (!( f_retained )) {
//					free_bld_rup (bld_rup);
//				}
//			}
//
//			// Return accepted
//
//			return true;
//		}
//
//	}




	//----- Building, Application Level -----




	// Build from a list of OERupture objects.
	// Parameters:
	//  rup_list = List of OERupture objects.
	//  accept_list = List to fill in with accepted OERupture objects, or null if not required.
	//  reject_list = List to fill in with rejected OERupture objects, or null if not required.
	//  eligible_mag = Minimum magnitude for eligible ruptures, or NO_MAG_NEG if none.
	//  eligible_count = Maximum number of eligible ruptures, or 0 if no limit (limit is flexible).
	// Note: All parameters must be already set up.
	// Note: In each OERupture object, the k_prod field is set equal to the pre-existing magnitude
	//  of completeness if the rupture is accepted, or NO_MAG_POS if the rupture is rejected.

	public void build_from_rup_list (Collection<OERupture> rup_list, Collection<OERupture> accept_list, Collection<OERupture> reject_list, double eligible_mag, int eligible_count) {
	
		// Copy the given list so we can sort it

		ArrayList<OERupture> work_list = new ArrayList<OERupture>(rup_list);

		// Effective eligible magnitude

		double eff_eligible_mag = eligible_mag;

		// If there is a count, and it limits the number of ruptures to consider ...

		if (eligible_count > 0 && work_list.size() > eligible_count) {
		
			// Sort the list by magnitude, descending

			work_list.sort (new OERupture.MagDescTimeDescComparator());

			// Set effective eligible magnitude to enforce the count

			eff_eligible_mag = Math.max (eff_eligible_mag, work_list.get(eligible_count).rup_mag);
		}
		
		// Sort the list by time, ascending

		work_list.sort (new OERupture.TimeAscMagDescComparator());

		// Create the skyline builder

		SkylineBuilder sky_builder = new SkylineBuilder();

		// Set it up

		sky_builder.setup_skyline();

		// Loop over ruptures ...

		for (OERupture rup : work_list) {
			double t_day = rup.t_day;
			double rup_mag = rup.rup_mag;

			// Rupture is eligible for skyline if it is at least the minimum magnitude

			boolean f_eligible = (rup_mag >= eff_eligible_mag);

			// Offer rupture to skyline, check if it is above mag of completeness

			rup.k_prod = sky_builder.offer_rupture (t_day, rup_mag, f_eligible);

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

//			// Offer rupture to skyline, check if it is above mag of completeness
//
//			boolean f_accepted = sky_builder.offer_rupture (t_day, rup_mag, f_eligible);
//
//			// Add rupture to accepted or rejected list
//
//			if (f_accepted) {
//				if (accept_list != null) {
//					accept_list.add (rup);
//				}
//			} else {
//				if (reject_list != null) {
//					reject_list.add (rup);
//				}
//			}
		}

		// Finish the skyline

		sky_builder.finish_up_skyline();

		return;
	}




	//----- Construction -----




	// Default constructor does nothing.

	public OEMagCompFnMultiFGH () {}




	// Construct from given parameters.

	public OEMagCompFnMultiFGH (double magCat, double capF, double capG, double capH,
								double t_range_begin, double t_range_end,
								Collection<OERupture> rup_list,
								Collection<OERupture> accept_list, Collection<OERupture> reject_list,
								double eligible_mag, int eligible_count) {

		// Save parameters

		if (!( capH >= MIN_CAP_H )) {
			throw new IllegalArgumentException ("OEMagCompFnMultiFGH.OEMagCompFnMultiFGH: H parameter is invalid: H = " + capH);
		}

		if (!( t_range_begin < t_range_end )) {
			throw new IllegalArgumentException ("OEMagCompFnMultiFGH.OEMagCompFnMultiFGH: Time range is invalid: t_range_begin = " + t_range_begin + ", t_range_end = " + t_range_end);
		}

		this.magCat = magCat;
		this.capF = capF;
		this.capG = capG;
		this.capH = capH;
		this.t_range_begin = t_range_begin;
		this.t_range_end = t_range_end;

		// Generate the function with the skyline algorithm

		build_from_rup_list (rup_list, accept_list, reject_list, eligible_mag, eligible_count);
	}




	// Display our contents, short form.

	@Override
	public String toString() {
		
		return "FnMultiFGH[magCat=" + magCat
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

		result.append ("OEMagCompFnMultiFGH:" + "\n");
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
	private static final int MARSHAL_VER_1 = 89001;

	private static final String M_VERSION_NAME = "OEMagCompFnMultiFGH";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_MULTIFGH;
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

			writer.marshalDouble ("magCat", magCat);
			writer.marshalDouble ("capF", capF);
			writer.marshalDouble ("capG", capG);
			writer.marshalDouble ("capH", capH);
			writer.marshalDouble ("t_range_begin", t_range_begin);
			writer.marshalDouble ("t_range_end", t_range_end);

			writer.marshalDoubleArray ("a_time", a_time);
			writer.marshalDoubleArray ("a_mag", a_mag);
			writer.marshalDoubleArray ("a_t0", a_t0);

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
			throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			magCat = reader.unmarshalDouble ("magCat");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");
			t_range_begin = reader.unmarshalDouble ("t_range_begin");
			t_range_end = reader.unmarshalDouble ("t_range_end");

			if (!( capH >= MIN_CAP_H )) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: H parameter is invalid: H = " + capH);
			}

			if (!( t_range_begin < t_range_end )) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: Time range is invalid: t_range_begin = " + t_range_begin + ", t_range_end = " + t_range_end);
			}

			// Array containing time and magnitude for each rupture

			double[] time_mag_array = reader.unmarshalDoubleArray ("time_mag_array");

			// Construct skyline for these ruptures

			try {

				// Make the OERupture objects with given times and magnitudes

				ArrayList<OERupture> rup_list = new ArrayList<OERupture>();
				OERupture.make_time_mag_list (rup_list,time_mag_array);

				// Generate the function with the skyline algorithm

				Collection<OERupture> accept_list = null;
				Collection<OERupture> reject_list = null;
				double eligible_mag = NO_MAG_NEG;
				int eligible_count = 0;
				build_from_rup_list (rup_list, accept_list, reject_list, eligible_mag, eligible_count);
			}
			catch (Exception e) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: Unable to construct skyline function from given ruptures", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			magCat = reader.unmarshalDouble ("magCat");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");
			t_range_begin = reader.unmarshalDouble ("t_range_begin");
			t_range_end = reader.unmarshalDouble ("t_range_end");

			if (!( capH >= MIN_CAP_H )) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: H parameter is invalid: H = " + capH);
			}

			if (!( t_range_begin < t_range_end )) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: Time range is invalid: t_range_begin = " + t_range_begin + ", t_range_end = " + t_range_end);
			}

			a_time = reader.unmarshalDoubleArray ("a_time");
			a_mag = reader.unmarshalDoubleArray ("a_mag");
			a_t0 = reader.unmarshalDoubleArray ("a_t0");
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
	public OEMagCompFnMultiFGH unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEMagCompFnMultiFGH obj) {

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

	public static OEMagCompFnMultiFGH unmarshal_poly (MarshalReader reader, String name) {
		OEMagCompFnMultiFGH result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFnMultiFGH.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_MULTIFGH:
			result = new OEMagCompFnMultiFGH();
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
			System.err.println ("OEMagCompFnMultiFGH : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         query_time  query_delta  query_count  [t_day  rup_mag]...
		// Build a function with the given parameters and rupture list.
		// Perform queries at the specified set of times.
		// Display detailed results.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 11 or more additional arguments

			if (!( args.length >= 12 && args.length % 2 == 0 )) {
				System.err.println ("OEMagCompFnMultiFGH : Invalid 'test1' subcommand");
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
				double query_time = Double.parseDouble (args[9]);
				double query_delta = Double.parseDouble (args[10]);
				int query_count = Integer.parseInt (args[11]);

				double[] time_mag_array = new double[args.length - 12];
				for (int ntm = 0; ntm < time_mag_array.length; ++ntm) {
					time_mag_array[ntm] = Double.parseDouble (args[ntm + 12]);
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

				OEMagCompFnMultiFGH mag_comp_fn = new OEMagCompFnMultiFGH (magCat, capF, capG, capH,
								t_range_begin, t_range_end,
								rup_list,
								accept_list, reject_list,
								eligible_mag, eligible_count);

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

		System.err.println ("OEMagCompFnMultiFGH : Unrecognized subcommand : " + args[0]);
		return;

	}

}
