package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import static org.opensha.oaf.util.SimpleUtils.rndd;

import org.opensha.oaf.oetas.OERupture;

import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value
import static org.opensha.oaf.oetas.OEConstants.TINY_DURATION_DAYS;		// an extremely small duration


// Discretized rupture history.
// Author: Michael Barall 09/06/2020.
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

public class OEDiscHistory {

	//----- General -----

	// Catalog magnitude of completeness.

	public double magCat;


	//----- Ruptures -----

	// Number of ruptures.

	public int rupture_count;

	// List of ruptures, in order of increasing time.
	// The length is rupture_count.
	// a_rupture_obj[n].t_day is the rupture time, in days.
	// a_rupture_obj[n].rup_mag is the rupture magnitude.
	// a_rupture_obj[n].k_prod is the rupture magnitude of completeness.
	// a_rupture_obj[n].rup_parent is the rupture category (currently undefined).
	// a_rupture_obj[n].x_km is the rupture x-ccordinate, in km (currently zero).
	// a_rupture_obj[n].y_km is the rupture y-ccordinate, in km (currently zero).

	public OERupture[] a_rupture_obj;

	// Interval time index for each rupture.
	// The length is rupture_count.
	// a_rup_int_time_index[n] is the index into a_interval_time that matches the time of rupture n.
	// That is, a_interval_time[a_rup_int_time_index[n]] matches a_rupture_obj[n].t_day.
	// Note: a_rup_int_time_index[n] is the number of intervals before rupture n, and is the index
	// of the first interval after rupture n.
	// Note: a_rup_int_time_index is non-strictly monotone increasing.

	public int[] a_rupture_int_time_index;

	// The index into a_rupture_obj (and other per-rupture arrays) for the mainshock.

	public int i_mainshock;


	//----- Intervals -----

	// Number of intervals.

	public int interval_count;

	// Magnitude of completeness for each interval.
	// The length is interval_count.
	// a_interval_mc[n] is the magnitude of completeness for interval n.

	public double[] a_interval_mc;

	// Begin and end times for each interval, in days.
	// The length is interval_count + 1.
	// a_interval_time[n] is the begin time of interval n.
	// a_interval_time[n + 1] is the end time of interval n.
	// a_interval_time[0] is the beginning of the time range.
	// a_interval_time[interval_count] is the ending of the time range.

	public double[] a_interval_time;

	// Get the beginning of the time range.

	public final double get_t_range_begin () {
		return a_interval_time[0];
	}

	// Get the ending of the time range.

	public final double get_t_range_end () {
		return a_interval_time[interval_count];
	}

	// Get the duration of interval n, in days.

	public final double get_interval_duration (int n) {
		return a_interval_time[n + 1] - a_interval_time[n];
	}

	// Get the midpoint of interval n, in days.

	public final double get_interval_midpoint (int n) {
		return (a_interval_time[n + 1] + a_interval_time[n]) * 0.5;
	}




	//----- Searching -----




	// Return values for find_time.

	public static class FindTimeResult {
	
		// The number of ruptures strictly before time t.
		// It is also the index of the first rupture at or after time t.
		// This is the minimum n such that  t <= a_rupture_obj[n].t_day .
		// It is 0 if t is equal to or before the time of the first rupture.
		// It is rupture_count if t is after the time of the last rupture.

		public int rup_index;

		// The rescaled time t, to be used in the interval list.

		public double int_time;

		// The index of the interval that contains time t.
		// It is n such that  a_interval_time[n] < t <= a_interval_time[n+1] .
		// It is -1 if time t is before the first interval, that is,  t <= a_interval_time[0] .
		// It is interval_count if time t is after the last interval, that is,  t > a_interval_time[inteval_count] .

		public int int_index;

		// True if the time is before the first interval.

		public boolean int_before_first;

		// True if the time is after the last interval.

		public boolean int_after_last;
	}




	// Find where the given time falls in the lists of ruptures and intervals.
	// Parameters:
	//  result = Structure to receive the result.
	//  t = Time to search for, in days.

	public void find_time (FindTimeResult result, double t) {

		// Binary search to find the rupture

		int lo = -1;				// implicitly, a_rupture_obj[-1].t_day = -infinity
		int hi = rupture_count;		// implicitly, a_rupture_obj[rupture_count].t_day = +infinity

		// Search, maintaining the condition a_rupture_obj[lo].t_day < t <= a_rupture_obj[hi].t_day

		while (hi - lo > 1) {
			int mid = (hi + lo)/2;
			if (a_rupture_obj[mid].t_day < t) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		// Return the rupture index

		result.rup_index = hi;

		// If before the first rupture ...

		if (hi == 0) {

			// Get the bounding rupture times

			final double rthi = a_rupture_obj[hi].t_day;

			// Set to search within the corresponding intervals

			lo = -1;
			hi = a_rupture_int_time_index[hi];

			// Rescale time to align with intervals

			result.int_time = a_interval_time[hi] - (rthi - t);
		}

		// Otherwise, if after the last rupture ...

		else if (hi == rupture_count) {

			// Get the bounding rupture times (note lo == rupture_count - 1 here)

			final double rtlo = a_rupture_obj[lo].t_day;

			// Set to search within the corresponding intervals

			lo = a_rupture_int_time_index[lo];
			hi = interval_count + 1;

			// Rescale time to align with intervals

			result.int_time = a_interval_time[lo] + (t - rtlo);
		}

		// Otherwise, internal to the rupture list ...

		else {

			// Get the bounding rupture times

			final double rtlo = a_rupture_obj[lo].t_day;
			final double rthi = a_rupture_obj[hi].t_day;

			// Set to search within the corresponding intervals (possibly producing lo == hi)

			lo = a_rupture_int_time_index[lo];
			hi = a_rupture_int_time_index[hi];

			// Rescale time to align with intervals

			result.int_time = a_interval_time[hi] - (rthi - t)*(a_interval_time[hi] - a_interval_time[lo])/(rthi - rtlo + TINY_DURATION_DAYS);
		}

		// Search, maintaining the condition a_interval_time[lo] < int_time <= a_interval_time[hi]

		while (hi - lo > 1) {
			int mid = (hi + lo)/2;
			if (a_interval_time[mid] < result.int_time) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		// Return the interval index, noting that lo == hi is possible

		result.int_index = hi - 1;

		// Set the flags

		result.int_before_first = (hi <= 0);
		result.int_after_last = (hi > interval_count);
	
		return;
	}




	//----- Building discretization structure -----




	// Clear the data.

	public void clear () {

		magCat = 0.0;

		rupture_count = 0;
		a_rupture_obj = null;
		a_rupture_int_time_index = null;

		interval_count = 0;
		a_interval_mc = null;
		a_interval_time = null;

		return;
	}




	// Default constructor creates an empty history.

	public OEDiscHistory () {
		clear();
	}




	// Build history from FGH discretization.
	// Parameters:
	//  params = Helmstetter and discretization parameters.
	//  rup_list = List of ruptures.
	// Note: This function modifies params, leaving it with the parameters actually used.
	// Note: This function modifies the OERupture objects, and retains some of them.

	public void build_from_fgh (OEDiscFGHParams params, Collection<OERupture> rup_list) {

		// Force splitting to be enabled

		params.require_splitting();

		// Force parameters to enable division for all ruptures

		params.set_division_all();

		// Make the magnitude of completeness function

		ArrayList<OERupture> accept_list = new ArrayList<OERupture>();
		ArrayList<OERupture> reject_list = new ArrayList<OERupture>();

		OEMagCompFnDiscFGH mag_comp_fn = new OEMagCompFnDiscFGH (params, rup_list,
						accept_list, reject_list);

		// Save the magnitude of completeness

		magCat = mag_comp_fn.get_mag_cat();

		// Build the rupture data

		rupture_count = accept_list.size();
		a_rupture_obj = new OERupture[rupture_count];
		a_rupture_int_time_index = new int[rupture_count];

		int n;

		for (n = 0; n < rupture_count; ++n) {
			a_rupture_obj[n] = accept_list.get(n);
		}

		// Build the interval data

		int i_lo = mag_comp_fn.get_closest_time_index (params.t_range_begin);
		int i_hi = mag_comp_fn.get_closest_time_index (params.t_range_end);

		interval_count = i_hi - i_lo;
		a_interval_mc = new double[interval_count];
		a_interval_time = new double[interval_count + 1];

		int m;

		for (m = 0; m < interval_count; ++m) {
			a_interval_mc[m] = mag_comp_fn.get_mag (m + i_lo + 1);
		}

		for (m = 0; m <= interval_count; ++m) {
			a_interval_time[m] = mag_comp_fn.get_time (m + i_lo);
		}

		// Align the ruptures to the interval times

		m = 0;
		for (n = 0; n < rupture_count; ++n) {
			while (m < interval_count && a_rupture_obj[n].t_day >= a_interval_time[m + 1]) {
				++m;
			}
			if (m < interval_count && a_interval_time[m + 1] - a_rupture_obj[n].t_day <= a_rupture_obj[n].t_day - a_interval_time[m]) {
				++m;
			}
			a_rupture_int_time_index[n] = m;
		}

		return;
	}




	// Display our contents.
	// Displays the first 20 ruptures and intervals.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDiscHistory:" + "\n");

		result.append ("magCat = " + rndd(magCat) + "\n");

		result.append ("rupture_count = " + rupture_count + "\n");
		int r_count = Math.min (20, rupture_count);
		for (int i_rup = 0; i_rup < r_count; ++i_rup) {
			result.append (a_rupture_obj[i_rup].one_line_mc_cat_string (i_rup, a_rupture_int_time_index[i_rup]) + "\n");
		}
		if (r_count < rupture_count) {
			result.append ("... and " + (rupture_count - r_count) + " more ruptures" + "\n");
		}

		//result.append ("i_mainshock = " + i_mainshock + "\n");
		result.append ("i_mainshock = " + a_rupture_obj[i_mainshock].one_line_mc_cat_string (i_mainshock, a_rupture_int_time_index[i_mainshock]) + "\n");

		result.append ("interval_count = " + interval_count + "\n");
		int i_count = Math.min (20, interval_count);
		for (int i_int = 0; i_int < i_count; ++i_int) {
			result.append (i_int + ": time = " + a_interval_time[i_int] + ", mc = " + a_interval_mc[i_int] + "\n");
		}
		result.append (i_count + ": time = " + a_interval_time[i_count] + "\n");
		if (i_count < interval_count) {
			result.append ("... and " + (interval_count - i_count) + " more intervals" + "\n");
		}

		return result.toString();
	}




	// Display a summary of our contents.

	public String summary_string() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDiscHistory:" + "\n");

		result.append ("magCat = " + rndd(magCat) + "\n");

		result.append ("rupture_count = " + rupture_count + "\n");

		//result.append ("i_mainshock = " + i_mainshock + "\n");
		result.append ("i_mainshock = " + a_rupture_obj[i_mainshock].one_line_mc_cat_string (i_mainshock, a_rupture_int_time_index[i_mainshock]) + "\n");

		result.append ("interval_count = " + interval_count + "\n");

		return result.toString();
	}




	// Display our full contents.
	// Warning: This can be quite large.

	public String dump_string() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDiscHistory:" + "\n");

		result.append ("magCat = " + rndd(magCat) + "\n");

		result.append ("rupture_count = " + rupture_count + "\n");
		for (int i_rup = 0; i_rup < rupture_count; ++i_rup) {
			result.append (a_rupture_obj[i_rup].one_line_mc_cat_string (i_rup, a_rupture_int_time_index[i_rup]) + "\n");
		}

		//result.append ("i_mainshock = " + i_mainshock + "\n");
		result.append ("i_mainshock = " + a_rupture_obj[i_mainshock].one_line_mc_cat_string (i_mainshock, a_rupture_int_time_index[i_mainshock]) + "\n");

		result.append ("interval_count = " + interval_count + "\n");
		for (int i_int = 0; i_int < interval_count; ++i_int) {
			result.append (i_int + ": time = " + rndd(a_interval_time[i_int]) + ", mc = " + rndd(a_interval_mc[i_int]) + "\n");
		}
		result.append (interval_count + ": time = " + rndd(a_interval_time[interval_count]) + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 93001;

	private static final String M_VERSION_NAME = "OEDiscHistory";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble      ("magCat"                  , magCat                  );

			writer.marshalInt         ("rupture_count"           , rupture_count           );
			OERupture.marshal_array (writer, "a_rupture_obj"     , a_rupture_obj           );
			writer.marshalIntArray    ("a_rupture_int_time_index", a_rupture_int_time_index);
			writer.marshalInt         ("i_mainshock"             , i_mainshock             );

			writer.marshalInt         ("interval_count"          , interval_count          );
			writer.marshalDoubleArray ("a_interval_mc"           , a_interval_mc           );
			writer.marshalDoubleArray ("a_interval_time"         , a_interval_time         );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			magCat = reader.unmarshalDouble                     ("magCat"                  );

			rupture_count  = reader.unmarshalInt                ("rupture_count"           );
			a_rupture_obj = OERupture.unmarshal_array (reader,   "a_rupture_obj"           );
			a_rupture_int_time_index = reader.unmarshalIntArray ("a_rupture_int_time_index");
			i_mainshock  = reader.unmarshalInt                  ("i_mainshock"             );

			interval_count  = reader.unmarshalInt               ("interval_count"          );
			a_interval_mc = reader.unmarshalDoubleArray         ("a_interval_mc"           );
			a_interval_time = reader.unmarshalDoubleArray       ("a_interval_time"         );

		}
		break;

		}

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

	public OEDiscHistory unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDiscHistory history) {
		writer.marshalMapBegin (name);
		history.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEDiscHistory static_unmarshal (MarshalReader reader, String name) {
		OEDiscHistory history = new OEDiscHistory();
		reader.unmarshalMapBegin (name);
		history.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return history;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEDiscHistory : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 20 or more additional arguments

			if (!( args.length >= 21 && args.length % 2 == 1 )) {
				System.err.println ("OEDiscHistory : Invalid 'test1' subcommand");
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

				double durlim_ratio = Double.parseDouble (args[15]);
				double durlim_min = Double.parseDouble (args[16]);
				double durlim_max = Double.parseDouble (args[17]);

				int mag_cat_count = Integer.parseInt (args[18]);
				double division_mag = Double.parseDouble (args[19]);
				int division_count = Integer.parseInt (args[20]);

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

				// Make the history

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

				OEDiscHistory history = new OEDiscHistory();

				history.build_from_fgh (params, rup_list);

				System.out.println ();
				System.out.println (history.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  magCat  capF  capG  capH  t_range_begin  t_range_end  eligible_mag  eligible_count
		//         mag_eps  time_eps  disc_base  disc_delta  disc_round  disc_gap
		//         durlim_ratio  durlim_min  durlim_max
		//         mag_cat_count  division_mag  division_count
		//         [t_day  rup_mag]...
		// Build a history with the given parameters and rupture list.
		// Display detailed results.
		// Same as test #1 except it dumps the entire history (caution: can be large).

		if (args[0].equalsIgnoreCase ("test2")) {

			// 20 or more additional arguments

			if (!( args.length >= 21 && args.length % 2 == 1 )) {
				System.err.println ("OEDiscHistory : Invalid 'test2' subcommand");
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

				double durlim_ratio = Double.parseDouble (args[15]);
				double durlim_min = Double.parseDouble (args[16]);
				double durlim_max = Double.parseDouble (args[17]);

				int mag_cat_count = Integer.parseInt (args[18]);
				double division_mag = Double.parseDouble (args[19]);
				int division_count = Integer.parseInt (args[20]);

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

				// Make the history

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

				OEDiscHistory history = new OEDiscHistory();

				history.build_from_fgh (params, rup_list);

				System.out.println ();
				System.out.println (history.dump_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEDiscHistory : Unrecognized subcommand : " + args[0]);
		return;

	}

}
