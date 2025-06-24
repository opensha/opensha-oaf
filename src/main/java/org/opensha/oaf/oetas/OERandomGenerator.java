package org.opensha.oaf.oetas;

import java.util.Arrays;

// For Colt 0.9.4
import cern.jet.stat.tdouble.Probability;
import cern.jet.stat.tdouble.Gamma;
import cern.jet.random.tdouble.engine.RandomGenerator;
import cern.jet.random.tdouble.engine.DoubleRandomEngine;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import cern.jet.random.tdouble.Poisson;
import cern.jet.random.tdouble.DoubleUniform;

import org.opensha.oaf.util.TestMode;
import static org.opensha.oaf.util.SimpleUtils.rndd;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;	// natural logarithm of 10
import static org.opensha.oaf.oetas.OEConstants.BKGD_TIME_DAYS;			// time value for a background source
import static org.opensha.oaf.oetas.OEConstants.BKGD_TIME_DAYS_CHECK;	// use x <= BKGD_TIME_DAYS_CHECK to check for BKGD_TIME_DAYS




// Class to provide random number generator for an Operational ETAS catalog.
// Author: Michael Barall 11/11/2019.
//
// Functions provided by this class:
// * Holds the underlying pseudo random number generator.
// * Provides a separate instance for each thread.
// * Contains functions to generate the random distributions required for ETAS.

public class OERandomGenerator {

	//----- Generators -----

	// The underlying PRNG, from which all distributions are obtained.

	DoubleMersenneTwister prng_engine;

	// Generates uniformly distributed random numbers.

	DoubleUniform gen_uniform;

	// Generates random numbers with a Poisson distribution.

	Poisson gen_poisson;




	//----- Construction -----




	// The random seed, or 0L if not set yet.

	private static long random_seed = 0L;


	// Get the next random seed.
	// Note: This is designed so that the lower 32 bits is an acceptable seed.

	private static synchronized long get_next_seed () {

		// If seed is not set yet ...

		if (random_seed <= 0L) {
		
			// Set seed according to test mode

			random_seed = TestMode.get_test_ranseed();

			// If no test mode seed, then set seed according to current time

			if (random_seed <= 0L) {
				random_seed = System.currentTimeMillis();
			}
		}

		// Increment seed so each call returns a different values

		random_seed += 987654321L;

		if (random_seed <= 0L) {	// really this should never happen
			random_seed = 123456789L - (random_seed / 2L);
		}
	
		return random_seed;
	}




	// Default constructor obtains a random seed and sets up the generators.

	public OERandomGenerator () {
		this (get_next_seed());
	}




	// Constructor sets up the generators using the supplied seed.
	// N.B.: Currently only the lower 32 bits of the seed are used.

	public OERandomGenerator (long seed) {
	
		// Mersenne twister with the given seed

		int mt_seed = (int)seed;
		prng_engine = new DoubleMersenneTwister (mt_seed);

		// Uniform random number generator
		// (We set limits to 0.0 and 1.0, but these are changed as needed)

		gen_uniform = new DoubleUniform (0.0, 1.0, prng_engine);

		// Poisson random number generator
		// (We set the mean to 1.0, but this is changed as needed)

		gen_poisson = new Poisson (1.0, prng_engine);
	}




	//----- Threading -----




	// Holds the per-thread random generator.

	private static final ThreadLocal<OERandomGenerator> per_thread_rangen =
		new ThreadLocal<OERandomGenerator>() {
			@Override protected OERandomGenerator initialValue () {
				return null;
			}
		};




	// Get the per-thread random generator for this thread, create it if necessary.

	public static OERandomGenerator get_thread_rangen () {

		// Get thread-local value

		OERandomGenerator rangen = per_thread_rangen.get();

		// If not created yet ...

		if (rangen == null) {

			// Create the random generator

			rangen = new OERandomGenerator ();

			// Save the random generator
		
			per_thread_rangen.set (rangen);
		}
	
		// Return it

		return rangen;
	}




	//----- Sampling -----




	// Sample from a Poisson distribution.
	// Parameters:
	//  mean = The mean of the Poisson distribution.

	public int poisson_sample (double mean) {
		return gen_poisson.nextInt (mean);
	}




	// Sample from a Poisson distribution, with sanity checking.
	// Parameters:
	//  mean = The mean of the Poisson distribution.
	// Note: The Poisson distribution in principle is unbounded.
	// Code using the Poisson distribution may fail if the return
	// value is extremely large, so this function clips the Poisson
	// distribution at about 10 sigma.  Note that the standard
	// deviation of the Poisson distribution is the square root
	// of the mean.  I do not know the largest value that can be
	// returned by nextInt().

	public int poisson_sample_checked (double mean) {

		// Evaluate the Poisson distribution

		int x = gen_poisson.nextInt (mean);

		// Get the cap as approximately the mean plus 10 sigma;
		// the extra +1.0 below ensures that the result is not
		// forced to zero when the mean is small

		int cap = (int)Math.round(mean + 10.0*Math.sqrt(mean + 1.0));

		// Return the capped value

		return Math.min(x, cap);
	}




	// Sample from a uniform distribution.
	// Parameters:
	//  u1 = Lower limit.
	//  u2 = Upper limit, must satisfy u1 <= u2.

	public double uniform_sample (double u1, double u2) {
		double u = gen_uniform.nextDoubleFromTo (u1, u2);

		// Force result to lie between u1 and u2

		if (u > u2) {
			u = u2;
		}
		if (u < u1) {
			u = u1;
		}

		return u;
	}




	// Sample from an integer-valued uniform distribution.
	// Parameters:
	//  i1 = Lower limit.
	//  i2 = Upper limit, must satisfy i1 <= i2.
	// Note: Because random number resolution is typically 32 bits,
	// very large ranges of integers may have individual probabilites
	// noticeably different than uniform.

	public int uniform_int_sample (int i1, int i2) {
		double u = gen_uniform.nextDoubleFromTo (((double)i1) - 0.5, ((double)i2) + 0.5);

		// Convert to integer and force result to lie between i1 and i2

		long v = Math.round (u);

		if (v > (long)i2) {
			v = (long)i2;
		}
		if (v < (long)i1) {
			v = (long)i1;
		}

		return (int)v;
	}




	// Pick an element given the cumulative probability density.
	// Parameters:
	//  x = Array of cumulative probability values, x[i] = P(v <= i).
	//  len = Length of array (uses x[0] through x[len-1]).
	//  u = Random number uniformly distributed between 0 and 1.
	// Returns an integer v such that 0 <= v < len, where the probability
	// of choosing v is proportional to x[v] - x[v-1] (for this purpose,
	// x[-1] is taken to be zero).
	// Requires 0 <= x[0] <= x[1] <= ... <= x[len-1].
	// Note that if there is a run of array elements with the same cumulative
	// probability, only the first element in the run can be chosen.
	// Note that x need not be normalized (that is, x[len-1] need not equal 1).

	// Implementation notes: Works by binary search.
	// The cutoff value is cut = x[len-1] * u.
	// The search indices lo and hi are maintained so that
	// x[lo] < cut and x[hi] >= cut.
	//
	// We use max(u, 1e-16) because otherwise u == 0.0 and x[0] == 0.0 would
	// cause the algorithm to incorrectly return 0.

	public static int cumulative_pick (double[] x, int len, double u) {
		int lo = -1;
		int hi = len - 1;
		double cut = x[hi] * Math.max(u, 1.0e-16);

		while (hi - lo > 1) {
			int mid = (hi + lo) / 2;
			if (x[mid] < cut) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		return hi;
	}




	// Sample an element given the cumulative probability density.
	// Parameters:
	//  x = Array of cumulative probability values, x[i] = P(v <= i).
	//  len = Length of array (uses x[0] through x[len-1]).
	//  u = Random number uniformly distributed between 0 and 1.
	// Returns an integer v such that 0 <= v < len, where the probability
	// of choosing v is proportional to x[v] - x[v-1] (for this purpose,
	// x[-1] is taken to be zero).
	// Requires 0 <= x[0] <= x[1] <= ... <= x[len-1].
	// Note that if there is a run of array elements with the same cumulative
	// probability, only the first element in the run can be chosen.
	// Note that x need not be normalized (that is, x[len-1] need not equal 1).

	public int cumulative_sample (double[] x, int len) {
		double u = uniform_sample (0.0, 1.0);
		return cumulative_pick (x, len, u);
	}




	// Return an Omori expected rate.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter.
	//  t1 = Lower time value in days, must satisfy t1 + c > 0.
	//  t2 = Upper time value in days, must satisfy t2 >= t1.
	// Returns Integral(t1, t2, ((t+c)^(-p))*dt).

	// Implementation note: For p != 1, the value is
	//
	// (t1+c)^q * (r^q - 1) / q
	//
	// where
	//
	// q = 1 - p
	//
	// r = (t2+c)/(t1+c)
	//
	// In the limit of p == 1 or q == 0, the value reduces to log(r).
	//
	// For small q, we use the first 2 terms in the Taylor series around q = 0.

//	public static double omori_rate (double p, double c, double t1, double t2) {
//		double q = 1.0 - p;
//		//double lnr = Math.log((t2 + c)/(t1 + c));
//		double lnr = Math.log1p((t2 - t1)/(t1 + c));
//
//		// For abs(q) <= 1.0e-9, this formula is correct to 12 digits in the
//		// worst case, and full double precision in the typical case.
//
//		if (Math.abs(q) <= 1.0e-9) {
//			double lns = Math.log((t2 + c)*(t1 + c));
//			return (0.5*lns*q + 1.0) * lnr;
//		}
//
//		// Calculate directly, using expm1 to avoid cancellation when
//		// t1 and t2 are almost equal.
//
//		return Math.pow(t1 + c, q) * Math.expm1(lnr*q) / q;
//	}

	// New version based on OEOmoriCalc.omext_single_integral.

	public static double omori_rate (double p, double c, double t1, double t2) {
		final double q = 1.0 - p;
		final double w = t1 + c;
		final double tr = t2 - t1;

		final double a = Math.log1p(tr/w);
		final double aq = a*q;
		if (Math.abs(aq) < 1.0e-15) {
			return Math.pow(w, q) * a;
		}
		return Math.pow(w, q) * Math.expm1(aq) / q;
	}




	// Return an Omori expected rate, raw version.
	// Parameters:
	//  p = Omori p parameter.
	//  w = Omori base offset, must satisfy w > 0.
	//  tr = Time range in days, must satisfy w + tr > 0, prefereably tr >= 0.
	// Returns Integral(0, tr, ((t+w)^(-p))*dt).

	// This relates to the previous function as
	// omori_rate (p, c, t1, t2) == omori_rate_raw (p, w, tr)
	// w == t1 + c
	// tr == t2 - t1
	// This function is more accurate if tr is small compared to w.

	// Implementation note: For p != 1, the value is
	//
	// w^q * (r^q - 1) / q
	//
	// where
	//
	// q = 1 - p
	//
	// r = (tr/w) + 1
	//
	// In the limit of p == 1 or q == 0, the value reduces to log(r).
	//
	// For small q, we use the first 2 terms in the Taylor series around q = 0.
	//
	// Note: Can be derived from the formulas for the previous function with the
	// substitutions t1+c -> w, t2+c -> tr+t1+c -> tr+w

	public static double omori_rate_raw (double p, double w, double tr) {
		final double q = 1.0 - p;

		final double a = Math.log1p(tr/w);
		final double aq = a*q;
		if (Math.abs(aq) < 1.0e-15) {
			return Math.pow(w, q) * a;
		}
		return Math.pow(w, q) * Math.expm1(aq) / q;
	}




	// Return an Omori average expected rate.
	// Parameters:
	//  p = Omori p parameter, must satisfy p >= 0.
	//  c = Omori c parameter.
	//  t1 = Lower time value in days, must satisfy t1 + c > 0.
	//  t2 = Upper time value in days, must satisfy t2 >= t1.
	// Returns Integral(t1, t2, ((t+c)^(-p))*dt) / (t2 - t1).

	// Implementation note: For p != 1, the value is
	//
	// (t1+c)^q * (r^q - 1) / (q * (t2 - t1))
	//
	// where
	//
	// q = 1 - p
	//
	// r = (t2+c)/(t1+c) = 1 + (t2-t1)/(t1+c)
	//
	// In the limit of p == 1 or q == 0, the value reduces to log(r) / (t2 - t1).
	//
	// For small q, we use the first 2 terms in the Taylor series around q = 0.
	//
	// For small t2-t1, we use the one-point midpoint integration rule.

//	public static double omori_average_rate (double p, double c, double t1, double t2) {
//		final double q = 1.0 - p;
//		final double dt = t2 - t1;
//		final double sdt = dt / (t1 + c);
//
//		// For sufficiently small dt, the integral can be computed by the one-point
//		// midpoint rule, to an accuracy of about 15 digits.
//		// (8.83e-15 = 24 * 1.0e-15 / e)
//		// (This works out to sdt <= 6.64e-8 for p = 2)
//
//		if (Math.abs(sdt * sdt * p * (p + 1.0)) <= 8.83e-15) {
//			return Math.pow((t1 + t2) * 0.5 + c, -p);
//		}
//
//		// For abs(q) <= 1.0e-9, this formula is correct to 12 digits in the
//		// worst case, and full double precision in the typical case.
//
//		if (Math.abs(q) <= 1.0e-9) {
//			final double lns = Math.log((t2 + c)*(t1 + c));
//			return (0.5*lns*q + 1.0) * Math.log1p(sdt) / dt;
//		}
//
//		// Calculate directly, using expm1 to avoid cancellation when
//		// t1 and t2 are close (but not close enough for midpoint rule).
//
//		return Math.pow(t1 + c, q) * Math.expm1(Math.log1p(sdt) * q) / (q * dt);
//	}

	// New version based on OEOmoriCalc.omext_single_density_integral.

	public static double omori_average_rate (double p, double c, double t1, double t2) {
		final double q = 1.0 - p;
		final double w = t1 + c;
		final double tr = t2 - t1;

		// If the relative t range is small enough so the midpoint rule is accurate to about 15 digits ...

		if (tr * tr * p * (1.0 + p) <= 24.0e-15 * w * w) {

			// Use the midpoint rule

			return Math.pow((t1 + t2) * 0.5 + c, -p);
		}

		// Otherwise, evaluate the integral

		final double a = Math.log1p(tr/w);
		final double aq = a*q;
		if (Math.abs(aq) < 1.0e-15) {
			return Math.pow(w, q) * a / tr;
		}
		return Math.pow(w, q) * Math.expm1(aq) / (q * tr);
	}




	// Same as above, except with w and tr as parameters.
	// w == t1 + c
	// tr == t2 - t1

	public static double omori_average_rate_raw (double p, double w, double tr) {
		final double q = 1.0 - p;

		// If the relative t range is small enough so the midpoint rule is accurate to about 15 digits ...

		if (tr * tr * p * (1.0 + p) <= 24.0e-15 * w * w) {

			// Use the midpoint rule

			return Math.pow(tr * 0.5 + w, -p);
		}

		// Otherwise, evaluate the integral

		final double a = Math.log1p(tr/w);
		final double aq = a*q;
		if (Math.abs(aq) < 1.0e-15) {
			return Math.pow(w, q) * a / tr;
		}
		return Math.pow(w, q) * Math.expm1(aq) / (q * tr);
	}




	// Rescale a time value in an Omori distribution.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter.
	//  t1 = Lower time value in days, must satisfy t1 + c > 0.
	//  t2 = Upper time value in days, must satisfy t2 > t1.
	//  u = Random number uniformly distributed between 0 and 1.
	// Returns a time t between t1 and t2, chosen according to a probability density
	// proportional to lambda(t) = (t+c)^(-p), so that the cumulative distribution
	// from t1 to t equals u times the cumulative distribution from t1 to t2
	// (in other words, the u fractile of the distribution).
	// Note: This is a truncated power-law distribution.

	// Implementation note: Uses the inversion method.
	// The following is a uniform random variable:
	// u = Integral(t1, t, lambda(t')*dt') / Integral(t1, t2, lambda(t')*dt')
	// Solving for t gives, assuming p != 1,
	//
	// log((t+c)/(t1+c)) = log(u*(r^q - 1) + 1) / q
	//
	// where
	//
	// q = 1 - p
	//
	// r = (t2+c)/(t1+c)
	//
	// In the limit of p == 1 or q == 0, this reduces to log((t+c)/(t1+c)) = u*log(r).
	//
	// For small q, we use the first 4 terms in the Taylor series around q = 0.

	public static double omori_rescale (double p, double c, double t1, double t2, double u) {
		double q = 1.0 - p;
		double lnr = Math.log((t2 + c)/(t1 + c));
		double qlnr = q*lnr;
		double y;

		// If abs(qlnr) <= 0.01 then the following formula is correct to
		// about 10 digits, which matches the typical resolution of the
		// random number generator (32 bits).

		if (Math.abs(qlnr) <= 0.01) {
			double c1 = (1.0 - u)/2.0;
			double c2 = ((2.0*u - 3.0)*u + 1.0)/6.0;
			double c3 = (((-6.0*u + 12.0)*u - 7.0)*u + 1.0)/24.0;
			y = (((c3*qlnr + c2)* qlnr + c1)*qlnr + 1.0)*u*lnr;
		}

		// Calculate directly, using expm1 and log1p to avoid cancellation

		else {
			y = Math.log1p(Math.expm1(qlnr)*u)/q;
		}

		// Finish computation of time

		double t = Math.exp(y)*(t1 + c) - c;

		// Force result to lie between t1 and t2

		if (t > t2) {
			t = t2;
		}
		if (t < t1) {
			t = t1;
		}

		return t;
	}




	// Rescale a time value in an Omori distribution, raw version.
	// Parameters:
	//  p = Omori p parameter.
	//  w = Omori base offset, must satisfy w > 0.
	//  tr = Time range in days, must satisfy w + tr > 0, prefereably tr >= 0.
	//  u = Random number uniformly distributed between 0 and 1.
	// Returns a time t between 0 and tr, chosen according to a probability density
	// proportional to lambda(t) = (t+w)^(-p), so that the cumulative distribution
	// from 0 to t equals u times the cumulative distribution from 0 to tr
	// (in other words, the u fractile of the distribution).
	// Note: This is a truncated power-law distribution.
	// Note: Due to rounding errors, the returned value may be sligntly less than 0
	// or sligntly greater than tr.  If this is a issue, then the caller should
	// coerce the return value into range.

	// This relates to the previous function as
	// omori_rescale (p, c, t1, t2, u) == t1 + omori_rescale_raw (p, w, tr, u)
	// w == t1 + c
	// tr == t2 - t1
	// This function is more accurate if tr is small compared to w.

	// Implementation note: Uses the inversion method.
	// The following is a uniform random variable:
	// u = Integral(0, t, lambda(t')*dt') / Integral(0, tr, lambda(t')*dt')
	// Solving for t gives, assuming p != 1,
	//
	// log((t/w) + 1) = log(u*(r^q - 1) + 1) / q
	//
	// where
	//
	// q = 1 - p
	//
	// r = (tr/w) + 1
	//
	// If we define  y = log(u*(r^q - 1) + 1) / q  then  t = (exp(y) - 1)*w .
	//
	// In the limit of p == 1 or q == 0, this reduces to log((t/w) + 1) = u*log(r).
	//
	// For small q, we use the first 4 terms in the Taylor series around q = 0.
	//
	// Note: Can be derived from the formulas for the previous function with the
	// substitutions t1+c -> w, t2+c -> tr+t1+c -> tr+w, t+c -> t+t1+c -> t+w

	public static double omori_rescale_raw (double p, double w, double tr, double u) {
		final double q = 1.0 - p;
		final double lnr = Math.log1p(tr/w);
		final double qlnr = q*lnr;
		double y;

		// If abs(qlnr) <= 0.01 then the following formula is correct to
		// about 10 digits, which matches the typical resolution of the
		// random number generator (32 bits).

		if (Math.abs(qlnr) <= 0.01) {
			final double c1 = (1.0 - u)/2.0;
			final double c2 = ((2.0*u - 3.0)*u + 1.0)/6.0;
			final double c3 = (((-6.0*u + 12.0)*u - 7.0)*u + 1.0)/24.0;
			y = (((c3*qlnr + c2)* qlnr + c1)*qlnr + 1.0)*u*lnr;
		}

		// Calculate directly, using expm1 and log1p to avoid cancellation

		else {
			y = Math.log1p(Math.expm1(qlnr)*u)/q;
		}

		// Finish computation of time

		return Math.expm1(y)*w;
	}




	// Sample from an Omori distribution.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter.
	//  t1 = Lower time value in days, must satisfy t1 + c > 0.
	//  t2 = Upper time value in days, must satisfy t2 > t1.
	// Returns a random time t between t1 and t2, chosen according to a probability density
	// proportional to lambda(t) = (t+c)^(-p).
	// Note: This is a truncated power-law distribution.

	public double omori_sample (double p, double c, double t1, double t2) {
		double u = uniform_sample (0.0, 1.0);
		return omori_rescale (p, c, t1, t2, u);
	}




	// Return an Omori expected rate, with shifted time.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  t0 = Time of earthquake, in days.
	//  teps = Time epsilon, the minimum time interval considered, must satisfy teps >= 0.
	//  t1 = Lower time value in days.
	//  t2 = Upper time value in days.
	// If t0 <= t1 <= t2 - teps, then returns Integral(t1, t2, ((t-t0+c)^(-p))*dt).
	// If t1 < t0 <= t2 - teps, then returns Integral(t0, t2, ((t-t0+c)^(-p))*dt).
	// Otherwise, returns 0.
	//
	// As a special case, if t0 == BKGD_TIME_DAYS, then return a background expected rate.
	// If t1 <= t2 - teps, then returns t2-t1.
	// Otherwise, returns 0.

	public static double omori_rate_shifted (double p, double c, double t0, double teps, double t1, double t2) {
		double rate;

		// Case where earthquake is a background source

		if (t0 <= BKGD_TIME_DAYS_CHECK) {
		
			// Small or degenerate interval

			if (t2 <= t1 + teps) {
				rate = 0.0;
			}

			// Otherwise, compute the background rate between t1 and t2

			else {
				rate = t2 - t1;
			}
		}

		// Case where earthquake is before start of time interval

		else if (t0 <= t1) {
		
			// Small or degenerate interval

			if (t2 <= t1 + teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between t1 and t2

			else {
				//rate = omori_rate (p, c, t1 - t0, t2 - t0);
				rate = omori_rate_raw (p, t1 - t0 + c, t2 - t1);
			}
		}

		// Otherwise, earthquake is within the time interval

		else {
		
			// Small or degenerate interval, or earthquake is after end of interval

			if (t2 <= t0 + teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between t0 and t2

			else {
				//rate = omori_rate (p, c, 0.0, t2 - t0);
				rate = omori_rate_raw (p, c, t2 - t0);
			}
		}

		return rate;
	}


	// Original version, used omori_rate and did not support background.

	public static double omori_rate_shifted_original (double p, double c, double t0, double teps, double t1, double t2) {
		double rate;

		// Case where earthquake is before start of time interval

		if (t0 <= t1) {
		
			// Small or degenerate interval

			if (t2 <= t1 + teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between t1 and t2

			else {
				rate = omori_rate (p, c, t1 - t0, t2 - t0);
			}
		}

		// Otherwise, earthquake is within the time interval

		else {
		
			// Small or degenerate interval, or earthquake is after end of interval

			if (t2 <= t0 + teps) {
				rate = 0.0;
			}

			// Otherwise, compute the rate between t0 and t2

			else {
				rate = omori_rate (p, c, 0.0, t2 - t0);
			}
		}

		return rate;
	}




	// Rescale a time value in an Omori distribution, with shifted time.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  t0 = Time of earthquake, in days.
	//  t1 = Lower time value in days.
	//  t2 = Upper time value in days, must satisfy t2 > t1.
	//  u = Random number uniformly distributed between 0 and 1.
	// Returns a time t chosen according to a probability density proportional
	// to lambda(t) = (t-t0+c)^(-p).
	// If t0 <= t1 < t2, then the result is between t1 and t2.
	// If t1 < t0 < t2, then the result is between t0 and t2.
	// Otherwise, the result is t2 (strictly speaking this is an error condition,
	// but the return value of t2 accommodates some possible rounding error cases).
	// The returned value is the u fractile of the distribution within the given bounds.
	// Note: This is a truncated power-law distribution.
	//
	// As a special case, if t0 == BKGD_TIME_DAYS, then rescale a background time value.
	// If t1 < t2, then the result is between t1 and t2.
	// Otherwise, the result is t2.
	// Note: This is a uniform distribution.

	public static double omori_rescale_shifted (double p, double c, double t0, double t1, double t2, double u) {
		double t;

		// Case where earthquake is a background source

		if (t0 <= BKGD_TIME_DAYS_CHECK) {
		
			// Degenerate interval

			if (t2 <= t1) {
				t = t2;
			}

			// Otherwise, rescale between t1 and t2

			else {

				t = ((t2 - t1)*u) + t1;

				// Force result to lie between t1 and t2

				if (t > t2) {
					t = t2;
				}
				if (t < t1) {
					t = t1;
				}
			}
		}

		// Case where earthquake is before start of time interval

		else if (t0 <= t1) {
		
			// Degenerate interval

			if (t2 <= t1) {
				t = t2;
			}

			// Otherwise, rescale between t1 and t2

			else {

				//t = omori_rescale (p, c, t1 - t0, t2 - t0, u) + t0;
				t = omori_rescale_raw (p, t1 - t0 + c, t2 - t1, u) + t1;

				// Force result to lie between t1 and t2

				if (t > t2) {
					t = t2;
				}
				if (t < t1) {
					t = t1;
				}
			}
		}

		// Otherwise, earthquake is within the time interval

		else {
		
			// Degenerate interval, or earthquake is after end of interval

			if (t2 <= t0) {
				t = t2;
			}

			// Otherwise, rescale between t0 and t2

			else {

				//t = omori_rescale (p, c, 0.0, t2 - t0, u) + t0;
				t = omori_rescale_raw (p, c, t2 - t0, u) + t0;

				// Force result to lie between t0 and t2

				if (t > t2) {
					t = t2;
				}
				if (t < t0) {
					t = t0;
				}
			}
		}

		return t;
	}


	// Original version, used omori_rescale and did not support background.

	public static double omori_rescale_shifted_original (double p, double c, double t0, double t1, double t2, double u) {
		double t;

		// Case where earthquake is before start of time interval

		if (t0 <= t1) {
		
			// Degenerate interval

			if (t2 <= t1) {
				t = t2;
			}

			// Otherwise, rescale between t1 and t2

			else {

				t = omori_rescale (p, c, t1 - t0, t2 - t0, u) + t0;

				// Force result to lie between t1 and t2

				if (t > t2) {
					t = t2;
				}
				if (t < t1) {
					t = t1;
				}
			}
		}

		// Otherwise, earthquake is within the time interval

		else {
		
			// Degenerate interval, or earthquake is after end of interval

			if (t2 <= t0) {
				t = t2;
			}

			// Otherwise, rescale between t0 and t2

			else {

				t = omori_rescale (p, c, 0.0, t2 - t0, u) + t0;

				// Force result to lie between t0 and t2

				if (t > t2) {
					t = t2;
				}
				if (t < t0) {
					t = t0;
				}
			}
		}

		return t;
	}




	// Sample from an Omori distribution, with shifted time.
	// Parameters:
	//  p = Omori p parameter.
	//  c = Omori c parameter, must satisfy c > 0.
	//  t0 = Time of earthquake, in days.
	//  t1 = Lower time value in days.
	//  t2 = Upper time value in days, must satisfy t2 > t1.
	// Returns a random time t chosen according to a probability density proportional
	// to lambda(t) = (t-t0+c)^(-p).
	// If t0 <= t1 < t2, then the result is between t1 and t2.
	// If t1 < t0 < t2, then the result is between t0 and t2.
	// Otherwise, the result is t2 (strictly speaking this is an error condition,
	// but the return value of t2 accommodates some possible rounding error cases).
	// Note: This is a truncated power-law distribution.
	//
	// As a special case, if t0 == BKGD_TIME_DAYS, then sample from a background distribution.
	// If t1 < t2, then the result is between t1 and t2.
	// Otherwise, the result is t2.
	// Note: This is a uniform distribution.

	public double omori_sample_shifted (double p, double c, double t0, double t1, double t2) {
		double u = uniform_sample (0.0, 1.0);
		return omori_rescale_shifted (p, c, t0, t1, t2, u);
	}




	// Sample from a background distribution.
	// Parameters:
	//  t1 = Lower time value in days.
	//  t2 = Upper time value in days, must satisfy t2 > t1.
	// Returns a random time t chosen according to a uniform probability density.
	// If t1 < t2, then the result is between t1 and t2.
	// Otherwise, the result is t2 (strictly speaking this is an error condition,
	// but the return value of t2 accommodates some possible rounding error cases).

	public double background_sample (double t1, double t2) {
		final double u = uniform_sample (0.0, 1.0);
		double t = ((t2 - t1)*u) + t1;

		// Force result to lie between t1 and t2

		if (t < t1) {
			t = t1;
		}
		if (t > t2) {
			t = t2;
		}

		return t;
	}




	// Return a Gutenberg-Richter expected rate.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  mref = Reference magnitude.
	//  m1 = Lower magnitude.
	//  m2 = Upper magnitude, must satisfy m2 > m1.
	// Returns Integral(m1, m2, (b*log(10)*10^(-b*(m - mref)))*dm).
	// Take note of the factor b*log(10) in the return value definition,
	// which indicates the integrand is a rate per unit magnitude.
	// Note that if m1 == mref and m2 --> infinity, then rate --> 1.

	// Implementation note: The value is
	//
	// 10^(-b*(m1-mref)) - 10^(-b*(m2-mref))
	//
	// The value may be written
	//
	// exp(-beta*(m1 - mref))*(1 - exp(-beta*(m2 - m1)))
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_rate (double b, double mref, double m1, double m2) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly, using expm1 to avoid cancellation when
		// m1 and m2 are almost equal.

		return -Math.exp(-beta*(m1 - mref))*Math.expm1(-beta*(m2 - m1));
	}




	// Return a Gutenberg-Richter expected rate, with unbounded range.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  mref = Reference magnitude.
	//  m1 = Lower magnitude.
	// Returns Integral(m1, infinity, (b*log(10)*10^(-b*(m - mref)))*dm).
	// Take note of the factor b*log(10) in the return value definition,
	// which indicates the integrand is a rate per unit magnitude.
	// Note that if m1 == mref, then rate --> 1.
	// Note: This is the same as gr_rate with m2 = infinity.

	// Implementation note: The value is
	//
	// 10^(-b*(m1-mref))
	//
	// The value may be written
	//
	// exp(-beta*(m1 - mref))
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_rate_unbounded (double b, double mref, double m1) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly.

		return Math.exp(-beta*(m1 - mref));
	}




	// Return the ratio between two Gutenberg-Richter expected rates.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  sm1 = Source range lower magnitude.
	//  sm2 = Source range upper magnitude, must satisfy sm2 > sm1.
	//  tm1 = Target range lower magnitude.
	//  tm2 = Target range upper magnitude, must satisfy tm2 >= tm1.
	// Returns Integral(tm1, tm2, (10^(-b*m))*dm) / Integral(sm1, sm2, (10^(-b*m))*dm).
	// If the return value is r, then the expected rate in the target range
	// equals r times the expected rate in the source range.
	// Note that if sm2 --> infinity and tm2 --> infinity then r --> 10^(-b*(tm1 - sm1))
	// which is the classic G-R relation with no upper bound.
	// Note: tm2 == tm1 returns zero, but sm2 == sm1 triggers divide-by-zero.
	// Note: By comparison to the formulas for gr_rate, we have canceled out the
	// common factor b*log(10)*10^(b*mref) from both integrals.

	// Implementation note: The value is
	//
	// (10^(-b*tm1) - 10^(-b*tm2)) / (10^(-b*sm1) - 10^(-b*sm2))
	//
	// The value may be written
	//
	// exp(-beta*(tm1 - sm1)) * (1 - exp(-beta*(tm2 - tm1))) / (1 - exp(-beta*(sm2 - sm1)))
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_ratio_rate (double b, double sm1, double sm2, double tm1, double tm2) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly, using expm1 to avoid cancellation when
		// sm1 and sm2 are almost equal, or tm1 and tm2 are almost equal.

		return Math.exp(-beta*(tm1 - sm1)) * Math.expm1(-beta*(tm2 - tm1)) / Math.expm1(-beta*(sm2 - sm1));
	}




	// Return the ratio between two Gutenberg-Richter expected rates, with unbounded target range.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  sm1 = Source range lower magnitude.
	//  sm2 = Source range upper magnitude, must satisfy sm2 > sm1.
	//  tm1 = Target range lower magnitude.
	// Returns Integral(tm1, infinity, (10^(-b*m))*dm) / Integral(sm1, sm2, (10^(-b*m))*dm).
	// If the return value is r, then the expected rate in the target range
	// equals r times the expected rate in the source range.
	// Note that if sm2 --> infinity then r --> 10^(-b*(tm1 - sm1))
	// which is the classic G-R relation with no upper bound.
	// Note: sm2 == sm1 triggers divide-by-zero.
	// Note: By comparison to the formulas for gr_rate, we have canceled out the
	// common factor b*log(10)*10^(b*mref) from both integrals.
	// Note: This is the same as gr_ratio_rate with tm2 = infinity.

	// Implementation note: The value is
	//
	// (10^(-b*tm1)) / (10^(-b*sm1) - 10^(-b*sm2))
	//
	// The value may be written
	//
	// exp(-beta*(tm1 - sm1)) / (1 - exp(-beta*(sm2 - sm1)))
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_ratio_rate_unb_target (double b, double sm1, double sm2, double tm1) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly, using expm1 to avoid cancellation when
		// sm1 and sm2 are almost equal.

		return -Math.exp(-beta*(tm1 - sm1)) / Math.expm1(-beta*(sm2 - sm1));
	}




	// Invert a Gutenberg-Richter expected rate.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  mref = Reference magnitude.
	//  m2 = Upper magnitude.
	//  rate = Desired expected rate.
	// Returns the value of m1 that satisfies
	// r == Integral(m1, m2, (b*log(10)*10^(-b*(m - mref)))*dm).
	// Take note of the factor b*log(10) in the return value definition,
	// which indicates the integrand is a rate per unit magnitude.

	// Implementation note: The value is
	//
	// mref - log(rate + exp(-beta*(m2 - mref))) / beta
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_inv_rate (double b, double mref, double m2, double rate) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly

		return mref - Math.log(rate + Math.exp(-beta*(m2 - mref))) / beta;
	}




	// Invert a Gutenberg-Richter expected rate, with unbounded range.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  mref = Reference magnitude.
	//  rate = Desired expected rate.
	// Returns the value of m1 that satisfies
	// r == Integral(m1, m2, (b*log(10)*10^(-b*(m - mref)))*dm).
	// Take note of the factor b*log(10) in the return value definition,
	// which indicates the integrand is a rate per unit magnitude.
	// Note: This is the same as gr_inv_rate with m2 = infinity.

	// Implementation note: The value is
	//
	// mref - log(rate) / beta
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_inv_rate_unbounded (double b, double mref, double rate) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly

		return mref - Math.log(rate) / beta;
	}




	// Invert a ratio between two Gutenberg-Richter expected rates.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  sm1 = Source range lower magnitude.
	//  sm2 = Source range upper magnitude, typically sm2 > sm1.
	//  tm2 = Target range upper magnitude.
	//  r = Ratio of expected rates, must satisfy r > 0.
	// Returns the value of tm1, a target range lower magnitude, so that
	//   r == Integral(tm1, tm2, (10^(-b*m))*dm) / Integral(sm1, sm2, (10^(-b*m))*dm).
	// Note: By comparison to the formulas for gr_rate, we have canceled out the
	// common factor b*log(10)*10^(b*mref) from both integrals.
	// Note: If sm2 > sm1 (the typical case), then there is always a solution
	// for tm1 and it satisfies tm2 > tm1.
	// Note: Setting sm1 > sm2 allows solving for the case where the upper rather
	// than lower magnitude is adjusted, because a solution will have tm1 > tm2.
	// However in this case, a solution is possible only for
	//   r < 10^(-b*tm2) / (10^(-b*sm2) - 10^(-b*sm1))
	// This condition is equivalent to the argument of log() below being positive.

	// Implementation note: The ratio is
	//
	// r = (10^(-b*tm1) - 10^(-b*tm2)) / (10^(-b*sm1) - 10^(-b*sm2))
	//
	// The solution for tm1 can be written
	//
	// tm1 = tm2 - (1/beta) * log(exp(beta*(sm2 - sm1) - 1)*r*exp(beta*(tm2 - sm2)) + 1)
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_inv_ratio_rate (double b, double sm1, double sm2, double tm2, double r) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly, using expm1 and log1p

		return tm2 - ( Math.log1p( Math.expm1(beta*(sm2 - sm1)) * r * Math.exp(beta*(tm2 - sm2)) ) / beta );
	}




	// Rescale a magnitude value in a Gutenberg-Richter distribution.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  m1 = Lower magnitude.
	//  m2 = Upper magnitude, must satisfy m2 > m1.
	//  u = Random number uniformly distributed between 0 and 1.
	// Returns a magnitude m between m1 and m2, chosen according to a probability density
	// proportional to 10^(-b*m), so that the cumulative distribution
	// from m1 to m equals u times the cumulative distribution from m1 to m2
	// (in other words, the u fractile of the distribution).
	// Note: This is a truncated exponential distribution.

	// Implementation note: Uses the inversion method.
	// The following is a uniform random variable:
	// u = Integral(m1, m, 10^(-b*m')*dm') / Integral(m1, m2, 10^(-b*m')*dm')
	// Solving for m gives
	//
	// m = m1 - log(1 + u*(exp(-beta*(m2 - m1)) - 1)) / beta
	//
	// where
	//
	// beta = b*log(10)

	public static double gr_rescale (double b, double m1, double m2, double u) {
		double beta = C_LOG_10 * b;	// log(10) * b

		// Calculate directly, using expm1 and log1p to avoid cancellation

		double m = m1 - (Math.log1p(u*Math.expm1(-beta*(m2 - m1))) / beta);

		// Force result to lie between m1 and m2

		if (m > m2) {
			m = m2;
		}
		if (m < m1) {
			m = m1;
		}

		return m;
	}




	// Sample from a Gutenberg-Richter distribution.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  m1 = Lower magnitude.
	//  m2 = Upper magnitude, must satisfy m2 > m1.
	// Returns a magnitude m between m1 and m2, chosen according to a probability density
	// proportional to 10^(-b*m).
	// Note: This is a truncated exponential distribution.

	public double gr_sample (double b, double m1, double m2) {
		double u = uniform_sample (0.0, 1.0);
		return gr_rescale (b, m1, m2, u);
	}




	// Sample from a normal distribution.
	// Parameters:
	//  mean = Mean of normal distribution.
	//  sdev = Standard deviation of normal distribution.
	// Returns a normally distributed random value.
	// Note: Uses the Box-Muller transform.

	public double normal_sample (double mean, double sdev) {
		final double u1 = uniform_sample (0.0, 1.0);
		final double u2 = uniform_sample (0.0, 1.0);
		return ( Math.sqrt (Math.max (0.0, -2.0 * Math.log (Math.max (Double.MIN_NORMAL, u1)))) * Math.cos (2.0 * Math.PI * u2) * sdev ) + mean;
	}




	//----- Testing -----




	// Direct calculation of Omori expected rate.

	private static double test_omori_rate (double p, double c, double t1, double t2) {
		double result;
		final double q1 = 1.0 - p;
		if (q1 == 0.0) {
			result = Math.log(t2 + c) - Math.log(t1 + c);
		} else {
			result = (Math.pow(t2 + c, q1) - Math.pow(t1 + c, q1))/q1;
		}
		return result;
	}




	// Direct calculation of Omori average expected rate.

	private static double test_omori_average_rate (double p, double c, double t1, double t2) {
		double result;
		if (t2 == t1) {
			result = Math.pow(t1 + c, -p);
		} else {
			result = test_omori_rate (p, c, t1, t2) / (t2 - t1);
		}
		return result;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OERandomGenerator : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  u1  u2  n
		// Generate n random numbers from a uniform distribution with limits u1 and u2.
		// Display the list of random numbers.
		// Then sort the list and display the sorted list.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OERandomGenerator : Invalid 'test1' subcommand");
				return;
			}

			try {

				double u1 = Double.parseDouble (args[1]);
				double u2 = Double.parseDouble (args[2]);
				int n = Integer.parseInt(args[3]);

				// Say hello

				System.out.println ("Generating uniform random numbers");
				System.out.println ("u1 = " + u1);
				System.out.println ("u2 = " + u2);
				System.out.println ("n = " + n);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Generate random numbers

				double[] rand = new double[n];
				for (int k = 0; k < n; ++k) {
					rand[k] = rangen.uniform_sample (u1, u2);
				}

				// Display the list

				System.out.println ();
				System.out.println ("Random numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

				// Sort the list

				Arrays.sort (rand);

				// Display the sorted list

				System.out.println ();
				System.out.println ("Sorted andom numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  mean  n
		// Generate n random numbers from a Poisson distribution with the given mean.
		// Display the list of random numbers.
		// Then sort the list and display the sorted list.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OERandomGenerator : Invalid 'test2' subcommand");
				return;
			}

			try {

				double mean = Double.parseDouble (args[1]);
				int n = Integer.parseInt(args[2]);

				// Say hello

				System.out.println ("Generating Poisson random numbers");
				System.out.println ("mean = " + mean);
				System.out.println ("n = " + n);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Generate random numbers

				int[] rand = new int[n];
				for (int k = 0; k < n; ++k) {
					rand[k] = rangen.poisson_sample (mean);
				}

				// Display the list

				System.out.println ();
				System.out.println ("Random numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

				// Sort the list

				Arrays.sort (rand);

				// Display the sorted list

				System.out.println ();
				System.out.println ("Sorted andom numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  p  c  t1  t2
		// Display the Omori rate and average Omori rate.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OERandomGenerator : Invalid 'test3' subcommand");
				return;
			}

			try {

				double p = Double.parseDouble (args[1]);
				double c = Double.parseDouble (args[2]);
				double t1 = Double.parseDouble (args[3]);
				double t2 = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Calculating Omori rate and average Omori rate");
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("t1 = " + t1);
				System.out.println ("t2 = " + t2);

				// Calculate the rate

				double rate = OERandomGenerator.omori_rate (p, c, t1, t2);

				System.out.println ();
				System.out.println ("rate = " + rndd(rate));

				// Calculate the average rate

				double average_rate = OERandomGenerator.omori_average_rate (p, c, t1, t2);

				System.out.println ();
				System.out.println ("average_rate = " + rndd(average_rate));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  p  c  t1  t2  n
		// Rescale time according to an Omori distribution.
		// Shows rescaled values for u = 0/n, 1/n, 2/n ... n/n.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 5 additional arguments

			if (args.length != 6) {
				System.err.println ("OERandomGenerator : Invalid 'test4' subcommand");
				return;
			}

			try {

				double p = Double.parseDouble (args[1]);
				double c = Double.parseDouble (args[2]);
				double t1 = Double.parseDouble (args[3]);
				double t2 = Double.parseDouble (args[4]);
				int n = Integer.parseInt(args[5]);

				// Say hello

				System.out.println ("Rescaling Omori time");
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("t1 = " + t1);
				System.out.println ("t2 = " + t2);
				System.out.println ("n = " + n);

				// Calculate and display the rescaled values

				System.out.println ();

				for (int k = 0; k <= n; ++k) {
					double u = ((double)k)/((double)n);
					double t = OERandomGenerator.omori_rescale (p, c, t1, t2, u);
					System.out.println (k + "   " + rndd(t));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  p  c  t1  t2  n
		// Generate n random numbers from an Omori distribution with parameters p, c, t1, t2.
		// Display the list of random numbers.
		// Then sort the list and display the sorted list.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 5 additional arguments

			if (args.length != 6) {
				System.err.println ("OERandomGenerator : Invalid 'test5' subcommand");
				return;
			}

			try {

				double p = Double.parseDouble (args[1]);
				double c = Double.parseDouble (args[2]);
				double t1 = Double.parseDouble (args[3]);
				double t2 = Double.parseDouble (args[4]);
				int n = Integer.parseInt(args[5]);

				// Say hello

				System.out.println ("Generating Omori random times");
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("t1 = " + t1);
				System.out.println ("t2 = " + t2);
				System.out.println ("n = " + n);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Generate random numbers

				double[] rand = new double[n];
				for (int k = 0; k < n; ++k) {
					rand[k] = rangen.omori_sample (p, c, t1, t2);
				}

				// Display the list

				System.out.println ();
				System.out.println ("Random numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

				// Sort the list

				Arrays.sort (rand);

				// Display the sorted list

				System.out.println ();
				System.out.println ("Sorted andom numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  b  m1  m2  n
		// Rescale time according to a Gutenberg-Richter distribution.
		// Shows rescaled values for u = 0/n, 1/n, 2/n ... n/n.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OERandomGenerator : Invalid 'test6' subcommand");
				return;
			}

			try {

				double b = Double.parseDouble (args[1]);
				double m1 = Double.parseDouble (args[2]);
				double m2 = Double.parseDouble (args[3]);
				int n = Integer.parseInt(args[4]);

				// Say hello

				System.out.println ("Rescaling Gutenberg-Richeter magnitude");
				System.out.println ("b = " + b);
				System.out.println ("m1 = " + m1);
				System.out.println ("m2 = " + m2);
				System.out.println ("n = " + n);

				// Calculate and display the rescaled values

				System.out.println ();

				for (int k = 0; k <= n; ++k) {
					double u = ((double)k)/((double)n);
					double m = OERandomGenerator.gr_rescale (b, m1, m2, u);
					System.out.println (k + "   " + rndd(m));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  b  m1  m2  n
		// Generate n random numbers from a Gutenberg-Richter distribution with parameters b, m1, m2.
		// Display the list of random numbers.
		// Then sort the list and display the sorted list.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OERandomGenerator : Invalid 'test7' subcommand");
				return;
			}

			try {

				double b = Double.parseDouble (args[1]);
				double m1 = Double.parseDouble (args[2]);
				double m2 = Double.parseDouble (args[3]);
				int n = Integer.parseInt(args[4]);

				// Say hello

				System.out.println ("Generating Gutenberg-Richter random magnitudes");
				System.out.println ("b = " + b);
				System.out.println ("m1 = " + m1);
				System.out.println ("m2 = " + m2);
				System.out.println ("n = " + n);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Generate random numbers

				double[] rand = new double[n];
				for (int k = 0; k < n; ++k) {
					rand[k] = rangen.gr_sample (b, m1, m2);
				}

				// Display the list

				System.out.println ();
				System.out.println ("Random numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

				// Sort the list

				Arrays.sort (rand);

				// Display the sorted list

				System.out.println ();
				System.out.println ("Sorted andom numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  b  mref  m1  m2
		// Display the Gutenberg-Richter rate.

		if (args[0].equalsIgnoreCase ("test8")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OERandomGenerator : Invalid 'test8' subcommand");
				return;
			}

			try {

				double b = Double.parseDouble (args[1]);
				double mref = Double.parseDouble (args[2]);
				double m1 = Double.parseDouble (args[3]);
				double m2 = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Calculating Gutenberg-Richter rate");
				System.out.println ("b = " + b);
				System.out.println ("mref = " + mref);
				System.out.println ("m1 = " + m1);
				System.out.println ("m2 = " + m2);

				// Calculate the rate

				double rate = OERandomGenerator.gr_rate (b, mref, m1, m2);

				System.out.println ();
				System.out.println ("rate = " + rndd(rate));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  b  mref  m2  rate
		// Display the inverse Gutenberg-Richter rate.

		if (args[0].equalsIgnoreCase ("test9")) {

			// 4 additional arguments

			if (args.length != 5) {
				System.err.println ("OERandomGenerator : Invalid 'test9' subcommand");
				return;
			}

			try {

				double b = Double.parseDouble (args[1]);
				double mref = Double.parseDouble (args[2]);
				double m2 = Double.parseDouble (args[3]);
				double rate = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Calculating inverse Gutenberg-Richter rate");
				System.out.println ("b = " + b);
				System.out.println ("mref = " + mref);
				System.out.println ("m2 = " + m2);
				System.out.println ("rate = " + rate);

				// Invert the rate

				double m1 = OERandomGenerator.gr_inv_rate (b, mref, m2, rate);

				System.out.println ();
				System.out.println ("m1 = " + rndd(m1));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  c  t1  t2
		// Omori rate for various values of p.

		if (args[0].equalsIgnoreCase ("test10")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OERandomGenerator : Invalid 'test10' subcommand");
				return;
			}

			try {

				double c = Double.parseDouble (args[1]);
				double t1 = Double.parseDouble (args[2]);
				double t2 = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Calculating Omori rate");
				System.out.println ("c = " + c);
				System.out.println ("t1 = " + t1);
				System.out.println ("t2 = " + t2);

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
					double rate = OERandomGenerator.omori_rate (p, c, t1, t2);
					double comp = test_omori_rate (p, c, t1, t2);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #11
		// Command format:
		//  test11  c  t1  t2
		// Omori average rate for various values of p.

		if (args[0].equalsIgnoreCase ("test11")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OERandomGenerator : Invalid 'test11' subcommand");
				return;
			}

			try {

				double c = Double.parseDouble (args[1]);
				double t1 = Double.parseDouble (args[2]);
				double t2 = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Calculating Omori average rate");
				System.out.println ("c = " + c);
				System.out.println ("t1 = " + t1);
				System.out.println ("t2 = " + t2);

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
					double rate = OERandomGenerator.omori_average_rate (p, c, t1, t2);
					double comp = test_omori_average_rate (p, c, t1, t2);
					System.out.println (p + "   " + rate + "   " + comp);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #12
		// Command format:
		//  test12  mean  sdev  n
		// Generate n random numbers from a normal distribution with given mean and standard deviation.
		// Display the list of random numbers.
		// Then sort the list and display the sorted list.

		if (args[0].equalsIgnoreCase ("test12")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OERandomGenerator : Invalid 'test12' subcommand");
				return;
			}

			try {

				double mean = Double.parseDouble (args[1]);
				double sdev = Double.parseDouble (args[2]);
				int n = Integer.parseInt(args[3]);

				// Say hello

				System.out.println ("Generating normal random numbers");
				System.out.println ("mean = " + mean);
				System.out.println ("sdev = " + sdev);
				System.out.println ("n = " + n);

				// Get the random number generator

				OERandomGenerator rangen = OERandomGenerator.get_thread_rangen();

				// Generate random numbers

				double[] rand = new double[n];
				for (int k = 0; k < n; ++k) {
					rand[k] = rangen.normal_sample (mean, sdev);
				}

				// Display the list

				System.out.println ();
				System.out.println ("Random numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

				// Sort the list

				Arrays.sort (rand);

				// Display the sorted list

				System.out.println ();
				System.out.println ("Sorted andom numbers");
				System.out.println ();
				for (int k = 0; k < n; ++k) {
					System.out.println (k + "   " + rand[k]);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OERandomGenerator : Unrecognized subcommand : " + args[0]);
		return;

	}

}
