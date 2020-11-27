package org.opensha.oaf.oetas;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;	// natural logarithm of 10




// Holds Omori calculation functions for Operational ETAS.
// Author: Michael Barall 11/26/2020.
//
// This class was separated out from OERandomGenerator.

public class OEOmoriCalc {




	// Calculate (exp(x) - 1)/x
	// The function avoids cancellation and divide-by-zero.

	public static double expm1dx (double x) {
		if (Math.abs(x) < 1.0e-15) {
			return 1.0;
		}
		return Math.expm1(x) / x;
	}




	// Calculate (exp(a*x) - 1)/x
	// The function avoids cancellation and divide-by-zero.

	public static double expm1dx (double x, double a) {
		final double ax = a*x;
		if (Math.abs(ax) < 1.0e-15) {
			return a;
		}
		return Math.expm1(ax) / x;
	}




	// Omori-extended single integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  w = Omori base offset, must satisfy w > 0.
	//  tr = Time range in days, must satisfy w + tr > 0, prefereably tr >= 0.
	// Returns the single integral:
	//   Integral(t = 0, t = tr; (w + t)^(-p) * dt)
	//
	// Note: When integrating the effect of a point source at time s,
	// on an extended target from t1 to t2, with s <= t1 <= t2, use
	//   w = t1 - s + c   and   tr = t2 - t1
	//
	// Note: When integrating the effect of an extended source from s1 to s2,
	// on a point target at time t, with s1 <= s2 <= t, use
	//   w = t - s2 + c   and   tr = s2 - s1
	//
	// Implementation notes:
	// With q = 1 - p, the integral is (provided that q != 0):
	//
	//   ((w + tr)^q - w^q)/q
	//
	// When tr is small compared to w, this formula can suffer from cancellation.
	// It can be rewritten as
	//
	//   w^q * ((1 + tr/w)^q - 1)/q
	//
	//   =   w^q * expm1dx(q, log(1 + tr/w))
	//
	// The last formula is valid for q == 0.

	public static double omext_single_integral (double p, double w, double tr) {
		final double q = 1.0 - p;
		return Math.pow(w, q) * expm1dx(q, Math.log1p(tr/w));
	}




	// Omori-extended single density integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  w = Omori base offset, must satisfy w > 0.
	//  tr = Time range in days, must satisfy w + tr > 0, prefereably tr >= 0.
	// Returns the single density integral:
	//   Integral(t = 0, t = tr; (w + t)^(-p) * dt) / tr
	//
	// If tr is sufficiently small, the integral over t can be replaced by a
	// midpoint approximation, yielding:
	//
	//   (w + tr/2)^(-p)
	//
	// The relative error of this approximation is about (1/24)*p*(1+p)*(tr/w)^2.

	public static double omext_single_density_integral (double p, double w, double tr) {

		// The minimum space between source and target (equals w if tr is non-negative)

		final double minw = w + Math.min(tr, 0.0);

		// Error tolerance squared for the midpoint rule to be accurate to about 15 digits

		final double mptolsq = (24.0e-15 * minw * minw) / (p*(1.0 + p));

		// If the relative t range is small ...

		if (tr*tr <= mptolsq) {

			// Use the midpoint rule

			return Math.pow(w + 0.5*tr, -p);
		}

		// Otherwise, evaluate the integral

		final double q = 1.0 - p;
		return Math.pow(w, q) * expm1dx(q, Math.log1p(tr/w)) / tr;
	}




	// Omori-extended double integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  w = Omori base offset, must satisfy w > 0.
	//  sr = Source time range in days, must satisfy w + sr > 0, prefereably sr >= 0.
	//  tr = Target time range in days, must satisfy w + tr > 0, prefereably tr >= 0.
	// Returns the double integral:
	//   Integral(s = 0, s = sr; t = 0, t = tr; (w + s + t)^(-p) * dt * ds)
	//
	// Note: When integrating the effect of an extended source from s1 to s2,
	// on an extended target from t1 to t2, with s1 <= s2 <= t1 <= t2, use
	//   w = t1 - s2 + c   and   sr = s2 - s1   and   tr = t2 - t1
	//
	// Implementation notes:
	// With q = 2 - p, the integral is:
	//
	//   ((w + sr + tr)^q - (w + tr)^q - (w + sr)^q + w^q) / (q*(q - 1))
	//
	// We re-write this as
	//
	//   w^q * ((1 + sr/w + tr/w)^q - (1 + tr/w)^q - (1 + sr/w)^q + 1) / (q*(q - 1))
	//
	// When tr or sr is small compared to w, this formula can suffer from cancellation.
	// Although the expm1() and log1p() functions can calculate accurately to order
	// (sr/w)+(tr/w), the sum is of order (sr/w)*(tr/w) which can be much smaller.
	//
	// If tr/w is sufficiently small, the integral over t can be replaced by a
	// midpoint approximation, yielding a single integral:
	//
	//   Integral(s = 0, s = sr; (w + s + tr/2)^(-p) * tr * ds)
	//
	// The relative error of this approximation is about (1/24)*p*(1+p)*(tr/w)^2.
	//
	// Likewise, if sr/w is sufficiently small, the integral over s can be replaced
	// by a midpoint approximation.  If both are small, we apply the midpoint
	// approximation to both integrals.

	public static double omext_double_integral (double p, double w, double sr, double tr) {

		// Exponent from single integral

		final double q1 = 1.0 - p;

		// The minimum space between source and target (equals w if tr and sr are both non-negative)

		final double minw = w + Math.min(tr, 0.0) + Math.min(sr, 0.0);

		// Error tolerance squared for the midpoint rule to be accurate to about 10 digits

		final double mptolsq = (24.0e-10 * minw * minw) / (p*(1.0 + p));

		// If the relative t range is small ...

		if (tr*tr <= mptolsq) {

			// If the relative s range is small ...

			if (sr*sr <= mptolsq) {
			
				// Use midpoint rule for both integrals

				return Math.pow(w + 0.5*tr + 0.5*sr, -p) * tr * sr;
			}

			// Use midpoint rule for t integral

			return Math.pow(w + 0.5*tr, q1) * expm1dx(q1, Math.log1p(sr/(w + 0.5*tr))) * tr;
		}

		// If the relative s range is small ...

		if (sr*sr <= mptolsq) {

			// Use midpoint rule for s integral

			return Math.pow(w + 0.5*sr, q1) * expm1dx(q1, Math.log1p(tr/(w + 0.5*sr))) * sr;
		}

		// Exponent from double integral

		final double q2 = 2.0 - p;

		// Relative sizes of time ranges

		final double tdw = tr/w;
		final double sdw = sr/w;

		// For large p, q1 is non-zero, but q2 might be zero

		if (p > 1.5) {
		
			// Expand with terms that cancel out 1/q2

			return (Math.pow(w, q2) / q1) * (
				expm1dx(q2, Math.log1p(tdw + sdw)) - expm1dx(q2, Math.log1p(tdw)) - expm1dx(q2, Math.log1p(sdw)) );
		}

		// For small p, q2 is non-zero, but q1 might be zero, so expand with terms that cancel out 1/q1

		return (Math.pow(w, q2) / q2) * (
			(1.0 + tdw + sdw)*expm1dx(q1, Math.log1p(tdw + sdw)) - (1.0 + tdw)*expm1dx(q1, Math.log1p(tdw)) - (1.0 + sdw)*expm1dx(q1, Math.log1p(sdw)) );
	}




	// Omori-extended double density integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  w = Omori base offset, must satisfy w > 0.
	//  sr = Source time range in days, must satisfy w + sr > 0, prefereably sr >= 0.
	//  tr = Target time range in days, must satisfy w + tr > 0, prefereably tr >= 0.
	// Returns the double integral:
	//   Integral(s = 0, s = sr; t = 0, t = tr; (w + s + t)^(-p) * dt * ds) / tr
	//
	// Note this is a density with respect to the target interval, but not with
	// respect to the source interval.

	public static double omext_double_density_integral (double p, double w, double sr, double tr) {

		// Exponent from single integral

		final double q1 = 1.0 - p;

		// The minimum space between source and target (equals w if tr and sr are both non-negative)

		final double minw = w + Math.min(tr, 0.0) + Math.min(sr, 0.0);

		// Error tolerance squared for the midpoint rule to be accurate to about 10 digits

		final double mptolsq = (24.0e-10 * minw * minw) / (p*(1.0 + p));

		// If the relative t range is small ...

		if (tr*tr <= mptolsq) {

			// If the relative s range is small ...

			if (sr*sr <= mptolsq) {
			
				// Use midpoint rule for both integrals

				return Math.pow(w + 0.5*tr + 0.5*sr, -p) * sr;
			}

			// Use midpoint rule for t integral

			return Math.pow(w + 0.5*tr, q1) * expm1dx(q1, Math.log1p(sr/(w + 0.5*tr)));
		}

		// If the relative s range is small ...

		if (sr*sr <= mptolsq) {

			// Use midpoint rule for s integral

			return Math.pow(w + 0.5*sr, q1) * expm1dx(q1, Math.log1p(tr/(w + 0.5*sr))) * sr / tr;
		}

		// Exponent from double integral

		final double q2 = 2.0 - p;

		// Relative sizes of time ranges

		final double tdw = tr/w;
		final double sdw = sr/w;

		// For large p, q1 is non-zero, but q2 might be zero

		if (p > 1.5) {
		
			// Expand with terms that cancel out 1/q2

			return (Math.pow(w, q2) / q1) * (
				expm1dx(q2, Math.log1p(tdw + sdw)) - expm1dx(q2, Math.log1p(tdw)) - expm1dx(q2, Math.log1p(sdw)) ) / tr;
		}

		// For small p, q2 is non-zero, but q1 might be zero, so expand with terms that cancel out 1/q1

		return (Math.pow(w, q2) / q2) * (
			(1.0 + tdw + sdw)*expm1dx(q1, Math.log1p(tdw + sdw)) - (1.0 + tdw)*expm1dx(q1, Math.log1p(tdw)) - (1.0 + sdw)*expm1dx(q1, Math.log1p(sdw)) ) / tr;
	}




	// Omori-extended self double integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  c = Omori offset c parmeter, must satisfy c > 0.
	//  sr = Source and target time range in days, must satisfy c + sr > 0, prefereably sr >= 0.
	// Returns the double integral:
	//   Integral(s = 0, s = sr; t = s, t = sr; (c + t - s)^(-p) * dt * ds)
	//
	// Note: When integrating the effect of an extended source from s1 to s2
	// on itself as a target, with s1 <= s2, use
	//   sr = s2 - s1
	//
	// Implementation notes:
	// With q = 2 - p, the integral is:
	//
	//   (c^q) * ((1 + sr/c)^q - q*sr/c - 1) / (q*(q - 1))
	//
	// When sr/c is small, this formula can suffer from cancellation.
	// This case is handled with a Taylor expansion in powers of sr/c.

	public static double omext_self_double_integral (double p, double c, double sr) {

		// Relative size of time range

		final double sdc = sr/c;

		// Exponent from double integral

		final double q2 = 2.0 - p;

		// If the relative time range is small ...

		if (sdc * sdc <= 1.0e-4) {
		
			// Use a Taylor expansion, accurate to about 10 digits

			return Math.pow(c, q2) * (((((3.0 + p)*sdc - 6.0)*(2.0 + p)*sdc + 30.0)*(1.0 + p)*sdc - 120.0)*p*sdc + 360.0)*sdc*sdc / 720.0;
		}

		// Exponent from single integral

		final double q1 = 1.0 - p;

		// For large p, q1 is non-zero, but q2 might be zero

		if (p > 1.5) {
		
			// Expand with terms that cancel out 1/q2

			return (Math.pow(c, q2) / q1) * (expm1dx(q2, Math.log1p(sdc)) - sdc);
		}

		// For small p, q2 is non-zero, but q1 might be zero, so expand with terms that cancel out 1/q1

		return (Math.pow(c, q2) / q2) * ((1.0 + sdc)*expm1dx(q1, Math.log1p(sdc)) - sdc);
	}




	// Omori-extended self double density integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  c = Omori offset c parmeter, must satisfy c > 0.
	//  sr = Source and target time range in days, must satisfy c + sr > 0, prefereably sr >= 0.
	// Returns the double integral:
	//   Integral(s = 0, s = sr; t = s, t = sr; (c + t - s)^(-p) * dt * ds) / sr

	public static double omext_self_double_density_integral (double p, double c, double sr) {

		// Relative size of time range

		final double sdc = sr/c;

		// Exponent from double integral

		final double q2 = 2.0 - p;

		// Exponent from single integral

		final double q1 = 1.0 - p;

		// If the relative time range is small ...

		if (sdc * sdc <= 1.0e-4) {
		
			// Use a Taylor expansion, accurate to about 10 digits

			return Math.pow(c, q1) * (((((3.0 + p)*sdc - 6.0)*(2.0 + p)*sdc + 30.0)*(1.0 + p)*sdc - 120.0)*p*sdc + 360.0)*sdc / 720.0;
		}

		// For large p, q1 is non-zero, but q2 might be zero

		if (p > 1.5) {
		
			// Expand with terms that cancel out 1/q2

			return (Math.pow(c, q1) / q1) * (expm1dx(q2, Math.log1p(sdc)) - sdc) / sdc;
		}

		// For small p, q2 is non-zero, but q1 might be zero, so expand with terms that cancel out 1/q1

		return (Math.pow(c, q1) / q2) * ((1.0 + sdc)*expm1dx(q1, Math.log1p(sdc)) - sdc) / sdc;
	}




	// Omori-extended self double density-square integral evaluation.
	// Parameters:
	//  p = Omori exponent p parameter.
	//  c = Omori offset c parmeter, must satisfy c > 0.
	//  sr = Source and target time range in days, must satisfy c + sr > 0, prefereably sr >= 0.
	// Returns the double integral:
	//   Integral(s = 0, s = sr; t = s, t = sr; (c + t - s)^(-p) * dt * ds) / (sr^2)

	public static double omext_self_double_density_sq_integral (double p, double c, double sr) {

		// Relative size of time range

		final double sdc = sr/c;

		// If the relative time range is small ...

		if (sdc * sdc <= 1.0e-4) {
		
			// Use a Taylor expansion, accurate to about 10 digits

			return Math.pow(c, -p) * (((((3.0 + p)*sdc - 6.0)*(2.0 + p)*sdc + 30.0)*(1.0 + p)*sdc - 120.0)*p*sdc + 360.0) / 720.0;
		}

		// Exponent from double integral

		final double q2 = 2.0 - p;

		// Exponent from single integral

		final double q1 = 1.0 - p;

		// For large p, q1 is non-zero, but q2 might be zero

		if (p > 1.5) {
		
			// Expand with terms that cancel out 1/q2

			return (Math.pow(c, -p) / q1) * (expm1dx(q2, Math.log1p(sdc)) - sdc) / (sdc*sdc);
		}

		// For small p, q2 is non-zero, but q1 might be zero, so expand with terms that cancel out 1/q1

		return (Math.pow(c, -p) / q2) * ((1.0 + sdc)*expm1dx(q1, Math.log1p(sdc)) - sdc) / (sdc*sdc);
	}




	// Calculate an Omori-extended rate for a point source and extended target.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  teps = Time epsilon, the minimum time interval considered, must satisfy teps >= 0.
	//  s = Source time, in days.
	//  t1 = Target time beginning, in days.
	//  t2 = Target time ending, in days.
	// If s <= t1 <= t2 - teps, then return:
	//   Integral(t = t1, t = t2; ((t - s + c)^(-p)) * dt).
	// If t1 < s <= t2 - teps, then return:
	//   Integral(t = s, t = t2; ((t - s + c)^(-p)) * dt).
	// Otherwise, return 0.

	public static double omext_rate_pt_src_ext_targ (double p, double c, double teps, double s, double t1, double t2) {
		double rate;
		double tr;

		// Case where source is before start of target

		if (s <= t1) {
		
			// Target time range in days

			tr = t2 - t1;
		
			// Small or degenerate target

			if (tr <= teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between t1 and t2

			else {
				rate = omext_single_integral (p, t1 - s + c, tr);
			}
		}

		// Otherwise, source is within or after the target

		else {
		
			// Target time range in days, only the portion after the source

			tr = t2 - s;
		
			// Small or degenerate target, or source is after end of target

			if (tr <= teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between s and t2

			else {
				rate = omext_single_integral (p, c, tr);
			}
		}

		return rate;
	}




	// Calculate an Omori-extended rate density for a point source and extended target.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  teps = Time epsilon, the minimum time interval considered, must satisfy teps >= 0.  [not used]
	//  s = Source time, in days.
	//  t1 = Target time beginning, in days.
	//  t2 = Target time ending, in days.
	// If s <= t1 <= t2, then return:
	//   Integral(t = t1, t = t2; ((t - s + c)^(-p)) * dt) / (t2 - t1).
	// If t1 < s < t2, then return:
	//   Integral(t = s, t = t2; ((t - s + c)^(-p)) * dt) / (t2 - t1).
	// Otherwise, return 0.

	public static double omext_rate_density_pt_src_ext_targ (double p, double c, double teps, double s, double t1, double t2) {
		double rate;
		double tr;

		// Case where source is before start of target

		if (s <= t1) {
		
			// Target time range in days

			tr = t2 - t1;
		
			// Degenerate target

			if (tr < 0.0) {
				rate = 0.0;
			}

			// Otherwise, compute the rate density between t1 and t2

			else {
				rate = omext_single_density_integral (p, t1 - s + c, tr);
			}
		}

		// Otherwise, source is within or after the target

		else {
		
			// Target time range in days, only the portion after the source

			tr = t2 - s;
		
			// Small or degenerate target, or source is after end of target

			if (tr <= 0.0) {
				rate = 0.0;
			}

			// Otherwise, compute the rate density between s and t2, and correct for portion of interval

			else {
				rate = omext_single_density_integral (p, c, tr) * tr / Math.max(t2 - t1, tr);
			}
		}

		return rate;
	}




	// Calculate an Omori-extended rate for an extended source and point target.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  teps = Time epsilon, the minimum time interval considered, must satisfy teps >= 0.
	//  s1 = Source time beginning, in days.
	//  s2 = Source time ending, in days.
	//  t = Target time, in days.
	// If s1 + eps <= s2 <= t, then return:
	//   Integral(s = s1, s = s2; ((t - s + c)^(-p)) * ds).
	// If s1 + eps <= t < s2, then return:
	//   Integral(s = s1, s = t; ((t - s + c)^(-p)) * ds).
	// Otherwise, return 0.

	public static double omext_rate_ext_src_pt_targ (double p, double c, double teps, double s1, double s2, double t) {
		double rate;
		double sr;

		// Case where target is after end of source

		if (s2 <= t) {
		
			// Source time range in days

			sr = s2 - s1;
		
			// Small or degenerate source

			if (sr <= teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between s1 and s2

			else {
				rate = omext_single_integral (p, t - s2 + c, sr);
			}
		}

		// Otherwise, target is within or before the source

		else {
		
			// Source time range in days, only the portion before the target

			sr = t - s1;
		
			// Small or degenerate source, or target is before beginning of source

			if (sr <= teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between s1 and t

			else {
				rate = omext_single_integral (p, c, sr);
			}
		}

		return rate;
	}




	// Calculate an Omori-extended rate for an extended source and extended target.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  teps = Time epsilon, the minimum time interval considered, must satisfy teps >= 0.
	//  s1 = Source time beginning, in days.
	//  s2 = Source time ending, in days.
	//  t1 = Target time beginning, in days.
	//  t2 = Target time ending, in days.
	// If s1 + eps <= s2 and max(t1,s2) <= t2 - teps, then return:
	//   Integral(s = s1, s = s2; t = max(t1,s2), t = t2; ((t - s + c)^(-p)) * dt * ds).
	// Otherwise, return 0.
	// Note: It is not intended that the source and target intervals overlap.  The intent is
	// that the target lies either completely after or completely before the source.  We
	// permit overlap only to allow for cases where rounding error causes a very small overlap.

	public static double omext_rate_ext_src_ext_targ (double p, double c, double teps, double s1, double s2, double t1, double t2) {
		double rate;

		// Target start, allowing for possible small overlap

		double tstart = Math.max(t1, s2);

		// Source and target time ranges

		double tr = t2 - tstart;
		double sr = s2 - s1;

		// Small or degenerate source or target, or target before the Source

		if (sr <= teps || tr <= teps) {
			rate = 0.0;
		}

		// Otherwise, target is after source and both are not small

		else {
			rate = omext_double_integral (p, tstart - s2 + c, sr, tr);
		}

		return rate;
	}




	// Calculate an Omori-extended rate density for an extended source and extended target.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  teps = Time epsilon, the minimum time interval considered, must satisfy teps >= 0.
	//  s1 = Source time beginning, in days.
	//  s2 = Source time ending, in days.
	//  t1 = Target time beginning, in days.
	//  t2 = Target time ending, in days.
	// If s1 + eps <= s2 and max(t1,s2) <= t2, then return:
	//   Integral(s = s1, s = s2; t = max(t1,s2), t = t2; ((t - s + c)^(-p)) * dt * ds) / (t2 - max(t1,s2)).
	// Otherwise, return 0.
	// Note: It is not intended that the source and target intervals overlap.  The intent is
	// that the target lies either completely after or completely before the source.  We
	// permit overlap only to allow for cases where rounding error causes a very small overlap.

	public static double omext_rate_density_ext_src_ext_targ (double p, double c, double teps, double s1, double s2, double t1, double t2) {
		double rate;

		// Target start, allowing for possible small overlap

		double tstart = Math.max(t1, s2);

		// Source and target time ranges

		double tr = t2 - tstart;
		double sr = s2 - s1;

		// Small or degenerate source or target, or target before the Source

		if (sr <= teps || tr < 0.0) {
			rate = 0.0;
		}

		// Otherwise, target is after source and source is not small

		else {
			rate = omext_double_density_integral (p, tstart - s2 + c, sr, tr);
		}

		return rate;
	}




	//----- Testing -----




	// Direct calculation of Omori-extended single integral.

	private static double test_omext_single_integral (double p, double w, double tr) {
		double result;
		double q1 = 1.0 - p;
		if (q1 == 0.0) {
			result = Math.log(w + tr) - Math.log(w);
		} else {
			result = (Math.pow(w + tr, q1) - Math.pow(w, q1))/q1;
		}
		return result;
	}




	// Direct calculation of Omori-extended single density integral.

	private static double test_omext_single_density_integral (double p, double w, double tr) {
		double result;
		if (tr == 0.0) {
			result = Math.pow(w, -p);
		} else {
			result = test_omext_single_integral (p, w, tr) / tr;
		}
		return result;
	}




	// Direct calculation of Omori-extended double integral.

	private static double test_omext_double_integral (double p, double w, double sr, double tr) {
		double result;
		double q1 = 1.0 - p;
		double q2 = 2.0 - p;
		if (q1 == 0.0) {
			result = (w + sr + tr)*(Math.log(w + sr + tr) - 1.0) - (w + tr)*(Math.log(w + tr) - 1.0) - (w + sr)*(Math.log(w + sr) - 1.0) + (w)*(Math.log(w) - 1.0);
		} else if (q2 == 0.0) {
			result = (Math.log(w + sr + tr) - Math.log(w + tr) - Math.log(w + sr) + Math.log(w)) / q1;
		} else {
			result = (Math.pow(w + sr + tr, q2) - Math.pow(w + tr, q2) - Math.pow(w + sr, q2) + Math.pow(w, q2)) / (q1*q2);
		}
		return result;
	}




	// Direct calculation of Omori-extended double density integral.

	private static double test_omext_double_density_integral (double p, double w, double sr, double tr) {
		double result;
		if (tr == 0.0) {
			result = test_omext_single_integral (p, w, sr);
		} else {
			result = test_omext_double_integral (p, w, sr, tr) / tr;
		}
		return result;
	}




	// Direct calculation of Omori-extended self double integral.

	private static double test_omext_self_double_integral (double p, double c, double sr) {
		double result;
		double q1 = 1.0 - p;
		double q2 = 2.0 - p;
		double sdc = sr/c;
		if (q1 == 0.0) {
			result = c * ((1.0 + sdc)*Math.log(1.0 + sdc) - sdc);
		} else if (q2 == 0.0) {
			result = sdc - Math.log(1.0 + sdc);
		} else {
			result = Math.pow(c, q2) * (Math.pow(1.0 + sdc, q2) - q2*sdc - 1.0) / (q1*q2);
		}
		return result;
	}




	// Direct calculation of Omori-extended double density integral.

	private static double test_omext_self_double_density_integral (double p, double c, double sr) {
		double result;
		if (sr == 0.0) {
			result = 0.0;
		} else {
			result = test_omext_self_double_integral (p, c, sr) / sr;
		}
		return result;
	}




	// Direct calculation of Omori-extended double density-square integral.

	private static double test_omext_self_double_density_sq_integral (double p, double c, double sr) {
		double result;
		if (sr == 0.0) {
			result = Math.pow(c, -p) / 2.0;
		} else {
			result = test_omext_self_double_integral (p, c, sr) / (sr*sr);
		}
		return result;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEOmoriCalc : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  w  tr
		// Omori-extended single integral for various values of p.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OEOmoriCalc : Invalid 'test1' subcommand");
				return;
			}

			try {

				double w = Double.parseDouble (args[1]);
				double tr = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Calculating Omori-extended single integral");
				System.out.println ("w = " + w);
				System.out.println ("tr = " + tr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					2.0,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_single_integral (p, w, tr);
					double comp = test_omext_single_integral (p, w, tr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  w  sr  tr
		// Omori-extended double integral for various values of p.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OEOmoriCalc : Invalid 'test2' subcommand");
				return;
			}

			try {

				double w = Double.parseDouble (args[1]);
				double sr = Double.parseDouble (args[2]);
				double tr = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Calculating Omori-extended double integral");
				System.out.println ("w = " + w);
				System.out.println ("sr = " + sr);
				System.out.println ("tr = " + tr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					1.99,
					1.999,
					1.9999,
					1.99999,
					1.999999,
					1.9999999,
					1.99999999,
					1.999999999,
					1.9999999999,
					1.99999999999,
					1.999999999999,
					2.0,
					2.000000000001,
					2.00000000001,
					2.0000000001,
					2.000000001,
					2.00000001,
					2.0000001,
					2.000001,
					2.00001,
					2.0001,
					2.001,
					2.01,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_double_integral (p, w, sr, tr);
					double comp = test_omext_double_integral (p, w, sr, tr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  w  tr
		// Omori-extended single density integral for various values of p.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OEOmoriCalc : Invalid 'test3' subcommand");
				return;
			}

			try {

				double w = Double.parseDouble (args[1]);
				double tr = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Calculating Omori-extended single density integral");
				System.out.println ("w = " + w);
				System.out.println ("tr = " + tr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					2.0,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_single_density_integral (p, w, tr);
					double comp = test_omext_single_density_integral (p, w, tr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  w  sr  tr
		// Omori-extended double density integral for various values of p.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OEOmoriCalc : Invalid 'test4' subcommand");
				return;
			}

			try {

				double w = Double.parseDouble (args[1]);
				double sr = Double.parseDouble (args[2]);
				double tr = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Calculating Omori-extended double density integral");
				System.out.println ("w = " + w);
				System.out.println ("sr = " + sr);
				System.out.println ("tr = " + tr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					1.99,
					1.999,
					1.9999,
					1.99999,
					1.999999,
					1.9999999,
					1.99999999,
					1.999999999,
					1.9999999999,
					1.99999999999,
					1.999999999999,
					2.0,
					2.000000000001,
					2.00000000001,
					2.0000000001,
					2.000000001,
					2.00000001,
					2.0000001,
					2.000001,
					2.00001,
					2.0001,
					2.001,
					2.01,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_double_density_integral (p, w, sr, tr);
					double comp = test_omext_double_density_integral (p, w, sr, tr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  c  sr
		// Omori-extended self double integral for various values of p.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OEOmoriCalc : Invalid 'test5' subcommand");
				return;
			}

			try {

				double c = Double.parseDouble (args[1]);
				double sr = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Calculating Omori-extended self double integral");
				System.out.println ("c = " + c);
				System.out.println ("sr = " + sr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					1.99,
					1.999,
					1.9999,
					1.99999,
					1.999999,
					1.9999999,
					1.99999999,
					1.999999999,
					1.9999999999,
					1.99999999999,
					1.999999999999,
					2.0,
					2.000000000001,
					2.00000000001,
					2.0000000001,
					2.000000001,
					2.00000001,
					2.0000001,
					2.000001,
					2.00001,
					2.0001,
					2.001,
					2.01,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_self_double_integral (p, c, sr);
					double comp = test_omext_self_double_integral (p, c, sr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  c  sr
		// Omori-extended self double integral for various values of p.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OEOmoriCalc : Invalid 'test6' subcommand");
				return;
			}

			try {

				double c = Double.parseDouble (args[1]);
				double sr = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Calculating Omori-extended self double density integral");
				System.out.println ("c = " + c);
				System.out.println ("sr = " + sr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					1.99,
					1.999,
					1.9999,
					1.99999,
					1.999999,
					1.9999999,
					1.99999999,
					1.999999999,
					1.9999999999,
					1.99999999999,
					1.999999999999,
					2.0,
					2.000000000001,
					2.00000000001,
					2.0000000001,
					2.000000001,
					2.00000001,
					2.0000001,
					2.000001,
					2.00001,
					2.0001,
					2.001,
					2.01,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_self_double_density_integral (p, c, sr);
					double comp = test_omext_self_double_density_integral (p, c, sr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  c  sr
		// Omori-extended self double integral for various values of p.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OEOmoriCalc : Invalid 'test7' subcommand");
				return;
			}

			try {

				double c = Double.parseDouble (args[1]);
				double sr = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Calculating Omori-extended self double density-square integral");
				System.out.println ("c = " + c);
				System.out.println ("sr = " + sr);

				// Values of p

				double[] pval = {
					0.5,
					0.6,
					0.7,
					0.8,
					0.9,
					0.99,
					0.999,
					0.9999,
					0.99999,
					0.999999,
					0.9999999,
					0.99999999,
					0.999999999,
					0.9999999999,
					0.99999999999,
					0.999999999999,
					1.0,
					1.000000000001,
					1.00000000001,
					1.0000000001,
					1.000000001,
					1.00000001,
					1.0000001,
					1.000001,
					1.00001,
					1.0001,
					1.001,
					1.01,
					1.1,
					1.2,
					1.3,
					1.4,
					1.5,
					1.6,
					1.7,
					1.8,
					1.9,
					1.99,
					1.999,
					1.9999,
					1.99999,
					1.999999,
					1.9999999,
					1.99999999,
					1.999999999,
					1.9999999999,
					1.99999999999,
					1.999999999999,
					2.0,
					2.000000000001,
					2.00000000001,
					2.0000000001,
					2.000000001,
					2.00000001,
					2.0000001,
					2.000001,
					2.00001,
					2.0001,
					2.001,
					2.01,
					2.1,
					2.2,
					2.3,
					2.4,
					2.5
				};

				// Output the values

				for (double p : pval) {
					double rate = omext_self_double_density_sq_integral (p, c, sr);
					double comp = test_omext_self_double_density_sq_integral (p, c, sr);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEOmoriCalc : Unrecognized subcommand : " + args[0]);
		return;

	}

}
