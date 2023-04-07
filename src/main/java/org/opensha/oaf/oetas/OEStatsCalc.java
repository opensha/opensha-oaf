package org.opensha.oaf.oetas;

import static org.opensha.oaf.util.SimpleUtils.rndd;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;	// natural logarithm of 10
import static org.opensha.oaf.oetas.OEConstants.SMALL_EXPECTED_COUNT;	// negligably small expected number of earthquakes


// Holds statistical calculation functions for Operational ETAS.
// Author: Michael Barall 12/02/2019.
//
// This class contains static functions that are used to perform
// statistical and related calculations.
//
// See OERandomGenerator for additional functions related to
// probability distributions, rates, and sampling.
//
// See OECatalogParams for parameter definitions.

public class OEStatsCalc {




	// Calculate the uncorrected "k" productivity value.
	// Parameters:
	//  m0 = Earthquake (mainshock) magnitude.
	//  a = Productivity parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	// This function does not apply any correction for the range of magnitudes
	// from which m0 is drawn.  It implicitly assumes that the given "a" value
	// corresponds to the range [mref, msup] from which m0 is drawn.
	//
	// The uncorrected k value is
	//
	//   k = 10^(a + alpha*(m0 - mref))

	public static double calc_k_uncorr (
		double m0,
		double a,
		double alpha,
		double mref
	) {
		return Math.pow (10.0, a + alpha*(m0 - mref));
	}




	// Calculate the uncorrected "k" productivity value.
	// Parameters:
	//  m0 = Earthquake (mainshock) magnitude.
	//  cat_params = Catalog parameters.
	// This function does not apply any correction for the range of magnitudes
	// from which m0 is drawn.  It implicitly assumes that the given "a" value
	// corresponds to the range [mref, msup] from which m0 is drawn.

	public static double calc_k_uncorr (
		double m0,
		OECatalogParams cat_params
	) {
		return calc_k_uncorr (
			m0,
			cat_params.a,
			cat_params.alpha,
			cat_params.mref
		);
	}




	// Calculate the corrected "k" productivity value.
	// Parameters:
	//  m0 = Earthquake (mainshock) magnitude.
	//  a = Productivity parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  msup = Maximum considered magnitude.
	//  mag_min = Minimum magnitude.
	//  mag_max = Maximum magnitude.
	// This function calculates a corrected k value, under the assumption
	// that the magnitude m0 is drawn from the range [mag_min, mag_max],
	// but the "a" value is given for the range [mref, msup].
	//
	// The corrected "k" value is
	//
	//   k_corr = k * Q
	//
	// Here k is the uncorrected "k" value, and Q is a correction factor:
	//
	//   Q = (10^((alpha - b)*msup) - 10^((alpha - b)*mref)) / (10^((alpha - b)*mag_max) - 10^((alpha - b)*mag_min))
	//
	//   or   Q = (msup - mref)/(mag_max - mag_min)   in the case where alpha == b.
	//
	// To avoid problems with cancellation or divide-by-zero, the following equivalen form is used:
	//
	//   Q = exp(v*(mref - mag_min)) * ( W(v*(msup - mref)) * (msup - mref) ) / ( W(v*(mag_max - mag_min)) * (mag_max - mag_min) )
	//
	//   W(x) = (exp(x) - 1)/x
	//
	//   v = log(10) * (alpha - b)
	//
	// This form is well-behaved for alpha == b because W(0) = 1.
	//
	// The formula for Q is derived by requiring that the corrected and uncorrected
	// productivity produce the same branch ratio.  Specifically, if the uncorrected
	// productivity is used with a mainshock magnitude chosen from a G-R distribution
	// truncated to the interval [mref, msup], and if the corrected productivity is
	// used with a mainshock magnitude chosen from the *same* G-R distribution
	// truncated to the interval [mag_min, mag_max], then the expected intensity
	// function is the same.
	//
	// Note that the above formula for Q assumes that the same mathematical formula is
	// used for the intensity function in both cases.  In particular, mref is used as
	// the reference magnitude in both cases (so magnitudes appear in exponents in the
	// combination m - mref).  The factor exp(v*(mref - mag_min)) is due to using the
	// same reference magnitude.  If instead the lower end of the G-R distribution were
	// used as the reference magnitude (so in the corrected case magnitudes appear in
	// exponents in the combination m - mag_min), then that term of Q would go away.
	// The remaining terms of Q are due to the change in magnitude range.  Notice they
	// depend only on the magnitude intervals, msup - mref and mag_max - mag_min, not
	// the absolute magnitudes, and they equal 1 if msup - mref == mag_max - mag_min.
	//
	// Note also that the formula for branch ratio assumes that the distribution of
	// earthquake magnitudes is well-approximated by a continuous exponential
	// distribution.  This likely requires over 100 earthquakes in the topmost
	// magnitude, which is much larger than the number found in a typical application.
	// For this reason, using corrected k to shift the magnitude range is only an
	// approximation to the behavior of the original magnitude range.

	public static double calc_k_corr (
		double m0,
		double a,
		double b,
		double alpha,
		double mref,
		double msup,
		double mag_min,
		double mag_max
	) {

		// Start with the uncorrected k

		double k = Math.pow (10.0, a + alpha*(m0 - mref));

		// Multiply by the exponential (first) term in Q

		double v = C_LOG_10 * (alpha - b);
		k = k * Math.exp(v*(mref - mag_min));

		// If the arguments to W are very small, use the approximation W = 1

		double delta_sup_ref = msup - mref;
		double delta_max_min = mag_max - mag_min;

		if (Math.max (Math.abs(v*delta_sup_ref), Math.abs(v*delta_max_min)) <= 1.0e-16) {
			k = k * (delta_sup_ref / delta_max_min);
		}

		// Otherwise, use the formula for W, noting that the factor v cancels out in the fraction

		else {
			k = k * (Math.expm1(v*delta_sup_ref) / Math.expm1(v*delta_max_min));
		}

		// Return corrected k

		return k;
	}




	// Calculate the corrected "k" productivity value.
	// Parameters:
	//  m0 = Earthquake (mainshock) magnitude.
	//  cat_params = Catalog parameters.
	//  mag_min = Minimum magnitude.
	//  mag_max = Maximum magnitude.
	// This function calculates a corrected k value, under the assumption
	// that the magnitude m0 is drawn from the range [mag_min, mag_max],
	// but the "a" value is given for the range [mref, msup].
	// See function above for details.

	public static double calc_k_corr (
		double m0,
		OECatalogParams cat_params,
		double mag_min,
		double mag_max
	) {
		return calc_k_corr (
			m0,
			cat_params.a,
			cat_params.b,
			cat_params.alpha,
			cat_params.mref,
			cat_params.msup,
			mag_min,
			mag_max
		);
	}




	// Calculate the corrected "k" productivity value.
	// Parameters:
	//  m0 = Earthquake (mainshock) magnitude.
	//  cat_params = Catalog parameters.
	//  gen_info = Catalog generation information
	// This function calculates a corrected k value, under the assumption
	// that the magnitude m0 is drawn from the range [mag_min, mag_max],
	// but the "a" value is given for the range [mref, msup].
	// See function above for details.

	public static double calc_k_corr (
		double m0,
		OECatalogParams cat_params,
		OEGenerationInfo gen_info
	) {
		return calc_k_corr (
			m0,
			cat_params.a,
			cat_params.b,
			cat_params.alpha,
			cat_params.mref,
			cat_params.msup,
			gen_info.gen_mag_min,
			gen_info.gen_mag_max
		);
	}




	// Calculate the branch ratio.
	// Parameters:
	//  a = Productivity parameter.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  msup = Maximum considered magnitude.
	//  tint = Time interval.
	//
	// The branch ratio is:
	//
	//   b * log(10) * 10^a * (msup - mref) * W(v*(msup - mref)) * Integral(0, tint, ((t+c)^(-p))*dt)
	//
	//   W(x) = (exp(x) - 1)/x
	//
	//   v = log(10) * (alpha - b)
	//
	// Note that the branch ratio depends only on the magnitude interval magint = msup - mref,
	// and not on the values of mref and msup separately.

	public static double calc_branch_ratio (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tint
	) {

		// Start with the G-R and Omori terms

		double r = b * C_LOG_10 * OERandomGenerator.omori_rate (p, c, 0.0, tint);

		// Apply the productivity

		r = r * Math.pow(10.0, a);

		// If the argument to W is very small, use the approximation W = 1

		double v = C_LOG_10 * (alpha - b);
		double delta_sup_ref = msup - mref;

		if (Math.abs(v*delta_sup_ref) <= 1.0e-16) {
			r = r * delta_sup_ref;
		}

		// Otherwise, use the formula for W

		else {
			r = r * Math.expm1(v*delta_sup_ref) / v;
		}

		// Return branch ratio

		return r;
	}




	// Calculate the branch ratio.
	// Parameters:
	//  cat_params = Catalog parameters.
	// See function above for details.

	public static double calc_branch_ratio (
		OECatalogParams cat_params
	) {
		return calc_branch_ratio (
			cat_params.a,
			cat_params.p,
			cat_params.c,
			cat_params.b,
			cat_params.alpha,
			cat_params.mref,
			cat_params.msup,
			cat_params.tend - cat_params.tbegin
		);
	}




	// Calculate the branch ratio.
	// Parameters:
	//  cat_params = Catalog parameters.
	//  tint = Time interval.
	// See function above for details.

	public static double calc_branch_ratio (
		OECatalogParams cat_params,
		double tint
	) {
		return calc_branch_ratio (
			cat_params.a,
			cat_params.p,
			cat_params.c,
			cat_params.b,
			cat_params.alpha,
			cat_params.mref,
			cat_params.msup,
			tint
		);
	}




	// Calculate the inverse branch ratio.
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  msup = Maximum considered magnitude.
	//  tint = Time interval.
	// This function calculates the productivity "a" such that the branch ratio equals n.
	// See function above for formulas.

	public static double calc_inv_branch_ratio (
		double n,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tint
	) {

		// Start with the G-R and Omori terms

		double r = b * C_LOG_10 * OERandomGenerator.omori_rate (p, c, 0.0, tint);

		// If the argument to W is very small, use the approximation W = 1

		double v = C_LOG_10 * (alpha - b);
		double delta_sup_ref = msup - mref;

		if (Math.abs(v*delta_sup_ref) <= 1.0e-16) {
			r = r * delta_sup_ref;
		}

		// Otherwise, use the formula for W

		else {
			r = r * Math.expm1(v*delta_sup_ref) / v;
		}

		// Return inverse branch ratio

		return Math.log10(n/r);
	}




	// Calculate the inverse branch ratio.
	// Parameters:
	//  n = Branch ratio.
	//  cat_params = Catalog parameters.
	// This function calculates the productivity "a" such that the branch ratio equals n.
	// See function above for details.

	public static double calc_inv_branch_ratio (
		double n,
		OECatalogParams cat_params
	) {
		return calc_inv_branch_ratio (
			n,
			cat_params.p,
			cat_params.c,
			cat_params.b,
			cat_params.alpha,
			cat_params.mref,
			cat_params.msup,
			cat_params.tend - cat_params.tbegin
		);
	}




	// Calculate the inverse branch ratio.
	// Parameters:
	//  n = Branch ratio.
	//  cat_params = Catalog parameters.
	//  tint = Time interval.
	// This function calculates the productivity "a" such that the branch ratio equals n.
	// See function above for details.

	public static double calc_inv_branch_ratio (
		double n,
		OECatalogParams cat_params,
		double tint
	) {
		return calc_inv_branch_ratio (
			n,
			cat_params.p,
			cat_params.c,
			cat_params.b,
			cat_params.alpha,
			cat_params.mref,
			cat_params.msup,
			tint
		);
	}




	// Calculate the value of (10^a)*Q from the branch ratio.
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  mag_min = Minimum magnitude.
	//  mag_max = Maximum magnitude.
	//  tint = Time interval.
	// This function calculates the productivity "a" such that the branch ratio equals n,
	// and returns the value of (10^a)*Q.
	// Note: The result is proportional to n.  So, if it is desired to compute (10^a)*Q
	// for multiple values of the branch ratio, this can be done by calling this function
	// with n == 1, and then multiplying the returned value by each value of the branch ratio.
	//
	// The correction factor Q is defined as
	//
	//   Q = exp(v*(mref - mag_min)) * ( W(v*(msup - mref)) * (msup - mref) ) / ( W(v*(mag_max - mag_min)) * (mag_max - mag_min) )
	//
	//   W(x) = (exp(x) - 1)/x
	//
	//   v = log(10) * (alpha - b)
	//
	// The branch ratio is
	//
	//   n = b * log(10) * 10^a * (msup - mref) * W(v*(msup - mref)) * Integral(0, tint, ((t+c)^(-p))*dt)
	//
	// Combining the above formulas
	//
	//   (10^a)*Q = n * exp(v*(mref - mag_min)) / ( W(v*(mag_max - mag_min)) * (mag_max - mag_min) * b * log(10) * Integral(0, tint, ((t+c)^(-p))*dt)  )
	//
	// Notice that msup cancels out of the calculation.
	//
	// Notice that if alpha == b then this reduces to
	//
	//   Q = (msup - mref) / (mag_max - mag_min)
	//
	//   n = b * log(10) * 10^a * (msup - mref) * Integral(0, tint, ((t+c)^(-p))*dt)
	//
	//   (10^a)*Q = n / ( (mag_max - mag_min) * b * log(10) * Integral(0, tint, ((t+c)^(-p))*dt)  )

	public static double calc_ten_a_q_from_branch_ratio (
		double n,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double mag_min,
		double mag_max,
		double tint
	) {

		// Start with the G-R and Omori terms

		double r = b * C_LOG_10 * OERandomGenerator.omori_rate (p, c, 0.0, tint);

		// If the argument to W is very small, use the approximation W = 1

		double v = C_LOG_10 * (alpha - b);
		double delta_max_min = mag_max - mag_min;

		if (Math.abs(v*delta_max_min) <= 1.0e-16) {
			r = r * delta_max_min;
		}

		// Otherwise, use the formula for W

		else {
			r = r * Math.expm1(v*delta_max_min) / v;
		}

		// Return (10^a)*Q

		return n * Math.exp(v*(mref - mag_min)) / r;
	}




	// Calculate the expected direct aftershock count.
	// Parameters:
	//  a = Productivity parameter.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  msup = Maximum considered magnitude.
	//  mag_min = Minimum magnitude.
	//  mag_max = Maximum magnitude.
	//  m0 = Earthquake (mainshock) magnitude.
	//  t0 = Earthquake time (in days).
	//  m1 = Start of magnitude interval.
	//  m2 = End of magnitude interval.
	//  t1 = Start of time interval.
	//  t2 = End of time interval.
	// Returns the expected number of earthquakes, between magnitudes m1 and m2,
	// and between times t1 and t1, that are direct aftershocks of the mainshock.
	// This function uses a corrected k value, under the assumption
	// that the magnitude m0 is drawn from the range [mag_min, mag_max],
	// but the "a" value is given for the range [mref, msup].
	//
	// The rate of direct aftershocks, per unit time, per unit magnitude, is
	//
	//   lambda(t, m) = k_corr * b * log(10) * (10^(-b*(m - mref))) * ((t-t0+c)^(-p))
	//
	//   k_corr = 10^(a + alpha*(m0 - mref)) * Q
	//
	// where Q is the correction factor defined above.
	//
	// The expected count of direct aftershocks is
	//
	//   count = Integral (m = m1, m = m2; t = t1, t = t2; lambda(t, m) * dt * dm)

	public static double calc_expected_da_count (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double mag_min,
		double mag_max,
		double m0,
		double t0,
		double m1,
		double m2,
		double t1,
		double t2
	) {

		// Corrected productivity

		double k_corr = calc_k_corr (
			m0,
			a,
			b,
			alpha,
			mref,
			msup,
			mag_min,
			mag_max
		);

		// Time integral

		double time_int = OERandomGenerator.omori_rate_shifted (
			p,
			c,
			t0,
			0.0,
			t1,
			t2
		);

		// Magnitude integral

		double mag_int = OERandomGenerator.gr_rate (
			b,
			mref,
			m1,
			m2
		);

		// Return the expected direct aftershock count

		return k_corr * time_int * mag_int;
	}




	// Calculate the value of (10^a)*Q from the expected direct aftershock count.
	// Parameters:
	//  da_count = Expected direct aftershock count.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  m0 = Earthquake (mainshock) magnitude.
	//  t0 = Earthquake time (in days).
	//  m1 = Start of magnitude interval.
	//  m2 = End of magnitude interval.
	//  t1 = Start of time interval.
	//  t2 = End of time interval.
	// Computes the value of (10^a)*Q so that da_count is the expected number of
	// earthquakes, between magnitudes m1 and m2, and between times t1 and t1,
	// that are direct aftershocks of the mainshock.
	// This function assumes the use of a corrected k value.
	// Caution: The function will cause divide-by-zero if the parameters are chosen
	// so as to produce a zero expected direct aftershock count.
	// Note: The result is proportional to da_count.  So, if it is desired to compute (10^a)*Q
	// for multiple values of the expected direct aftershock count, this can be done by calling
	// this function with da_count == 1, and then multiplying the returned value by each value
	// of the expected direct aftershock count.

	public static double calc_ten_a_q_from_expected_da_count (
		double da_count,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double m0,
		double t0,
		double m1,
		double m2,
		double t1,
		double t2
	) {

		// Corrected productivity, excluding the factor of (10^a)*Q

		double partial_k = Math.pow (10.0, alpha*(m0 - mref));

		// Time integral

		double time_int = OERandomGenerator.omori_rate_shifted (
			p,
			c,
			t0,
			0.0,
			t1,
			t2
		);

		// Magnitude integral

		double mag_int = OERandomGenerator.gr_rate (
			b,
			mref,
			m1,
			m2
		);

		// Return (10^a)*Q

		return da_count / (partial_k * time_int * mag_int);
	}




	// Calculate the correction factor "Q".
	// Parameters:
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	//  msup = Maximum considered magnitude.
	//  mag_min = Minimum magnitude.
	//  mag_max = Maximum magnitude.
	// Returns the value of Q, as defined above.

	public static double calc_q_correction (
		double b,
		double alpha,
		double mref,
		double msup,
		double mag_min,
		double mag_max
	) {

		// The exponential (first) term in Q

		double v = C_LOG_10 * (alpha - b);
		double q = Math.exp(v*(mref - mag_min));

		// If the arguments to W are very small, use the approximation W = 1

		double delta_sup_ref = msup - mref;
		double delta_max_min = mag_max - mag_min;

		if (Math.max (Math.abs(v*delta_sup_ref), Math.abs(v*delta_max_min)) <= 1.0e-16) {
			q = q * (delta_sup_ref / delta_max_min);
		}

		// Otherwise, use the formula for W, noting that the factor v cancels out in the fraction

		else {
			q = q * (Math.expm1(v*delta_sup_ref) / Math.expm1(v*delta_max_min));
		}

		// Return correction factor Q

		return q;
	}




	// Calculate the earthquake (mainshock) magnitude from the corrected "k" productivity value.
	// Parameters:
	//  k_corr = Corrected "k" productivity value.
	//  ten_a_q = Value of (10^a)*Q.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude = minimum considered magnitude.
	// Returns the earthquake (mainshock) magnitude m0 which would produce the
	// given corrected "k" productivity value.
	// Note that the magnitude range is implicity in Q.
	//
	// The corrected k productivity value is
	//
	//   k_corr = 10^(a + alpha*(m0 - mref)) * Q

	public static double calc_m0_from_k_corr (
		double k_corr,
		double ten_a_q,
		double alpha,
		double mref
	) {

		// Return m0

		return (Math.log10(k_corr / ten_a_q) / alpha) + mref;
	}




	// Calculate a new value of "a" for a changed value of the reference magnitude.
	// Parameters:
	//  a_old = Productivity parameter, original value.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref_old = Reference magnitude, original value.
	//  mref_new = Reference magnitude, new value.
	// Returns a new value of "a", denoted a_new, such that the computed value
	// of the intensity function "lambda" is unchanged when mref_new is used
	// as the reference magnitude in place of mref_old.
	// Note: The result is obtained by adding an offset to a_old.  So, if it is desired
	// to compute a_new for multiple values of a_old, this can be done by calling the
	// function with a_old == 0, and then adding the returned value to each value of a_old.
	//
	// The new value of "a" is:
	//
	//   a_new = a_old + (alpha - b) * (mref_new - mref_old)
	//
	// The formula is derived from the condition:
	//
	//   10^(a_old + alpha*(m0 - mref_old) - b*(m - mref_old)) == 10^(a_new + alpha*(m0 - mref_new) - b*(m - mref_new))

	public static double calc_a_new_from_mref_new (
		double a_old,
		double b,
		double alpha,
		double mref_old,
		double mref_new
	) {
		return a_old + ((alpha - b) * (mref_new - mref_old));
	}




	// Calculate a new value of background rate "mu" for a changed value of the reference magnitude.
	// Parameters:
	//  mu_old = Background rate parameter, original value.
	//  b = Gutenberg-Richter parameter.
	//  mref_old = Reference magnitude, original value.
	//  mref_new = Reference magnitude, new value.
	// Returns a new value of "mu", denoted mu_new, such that the computed value
	// of the intensity function "lambda" is unchanged when mref_new is used
	// as the reference magnitude in place of mref_old.
	//
	// The new value of "mu" is:
	//
	//   mu_new = mu_old * 10^(b * (mref_old - mref_new))
	//
	// The formula is derived from the condition:
	//
	//   mu_old * 10^(- b*(m - mref_old)) == mu_new * 10^(- b*(m - mref_new))

	public static double calc_mu_new_from_mref_new (
		double mu_old,
		double b,
		double mref_old,
		double mref_new
	) {
		return mu_old * Math.pow(10.0, b * (mref_old - mref_new));
	}




	// Calculate an ams-value for a given zero-mref ams-value.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude, for the definition of ams.
	// Returns the ams-value, for the reference magnitude mref.

	public static double calc_ams_from_zams (
		double zams,
		double b,
		double alpha,
		double mref
	) {
		return calc_a_new_from_mref_new (
			zams,					// a_old
			b,						// b
			alpha,					// alpha
			OEConstants.ZAMS_MREF,	// mref_old
			mref					// mref_new
		);
	}




	// Calculate zero-mref ams-value for a ginve ams-value
	// Parameters:
	//  ams = Mainshock productivity parameter, assuming reference magnitude equal to mref.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Reference magnitude, for the definition of ams.
	// Returns the ams-value, for reference magnitude equal to ZAMS_MREF == 0.0.

	public static double calc_zams_from_ams (
		double ams,
		double b,
		double alpha,
		double mref
	) {
		return calc_a_new_from_mref_new (
			ams,					// a_old
			b,						// b
			alpha,					// alpha
			mref,					// mref_old
			OEConstants.ZAMS_MREF	// mref_new
		);
	}




	// Calculate a mu-value for a given reference magnitude ZMU_MREF mu-value.
	// Parameters:
	//  zmu = Background rate parameter, assuming reference magnitude equal to ZMU_MREF.
	//  b = Gutenberg-Richter parameter.
	//  mref = Reference magnitude, for the definition of mu.
	// Returns the mu-value, for the reference magnitude mref.

	public static double calc_mu_from_zmu (
		double zmu,
		double b,
		double mref
	) {
		return calc_mu_new_from_mref_new (
			zmu,					// mu_old
			b,						// b
			OEConstants.ZMU_MREF,	// mref_old
			mref					// mref_new
		);
	}




	// Calculate the reference value ZMU_MREF mu-value for a ginve mu-value
	// Parameters:
	//  mu = Background rate parameter, assuming reference magnitude equal to mref.
	//  b = Gutenberg-Richter parameter.
	//  mref = Reference magnitude, for the definition of mu.
	// Returns the mu-value, for reference magnitude equal to ZMU_MREF.

	public final double calc_zmu_from_mu (
		double mu,
		double b,
		double mref
	) {
		return calc_mu_new_from_mref_new (
			mu,						// mu_old
			b,						// b
			mref,					// mref_old
			OEConstants.ZMU_MREF	// mref_new
		);
	}




	// Calculate the generalized factor "Q" for a magnitude range change.
	// Parameters:
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref_old = Reference magnitude, original value.
	//  mag_min_old = Minimum magnitude, original value.
	//  mag_max_old = Maximum magnitude, original value.
	//  mref_new = Reference magnitude, new value.
	//  mag_min_new = Minimum magnitude, new value.
	//  mag_max_new = Maximum magnitude, new value.
	//
	// Returns the generalized conversion factor Q_gen, defined as:
	//
	//   Q_gen = ((10^a_new)*Q_new) / ((10^a_old)*Q_old)
	//
	//   a_old = Productivity "a" defined on magnitude range [mref_old, msup_old].
	//   Q_old = Conversion factor, to change from  [mref_old, msup_old] to  [mag_min_old, mag_max_old].
	//   a_new = Productivity "a" defined on magnitude range [mref_new, msup_new].
	//   Q_new = Conversion factor, to change from  [mref_new, msup_new] to  [mag_min_new, mag_max_new].
	//
	// The formula is:
	//
	//   Q_gen =   [ exp(v*(mref_new - mag_min_new)) * ( W(v*(mag_max_old - mag_min_old)) * (mag_max_old - mag_min_old) ) ]
	//
	//           / [ exp(v*(mref_old - mag_min_old)) * ( W(v*(mag_max_new - mag_min_new)) * (mag_max_new - mag_min_new) ) ]
	//
	//   W(x) = (exp(x) - 1)/x
	//
	//   v = log(10) * (alpha - b)
	//
	// Notice that if alpha == b then this reduces to
	//
	//   Q_gen = (mag_max_old - mag_min_old) / (mag_max_new - mag_min_new)
	//
	// Also notice that msup_old and msup_new do not appear in the formulas.
	//
	// This function can be used to convert productivity, which is scaled both
	// according to reference magnitude and magnitude range.
	// Typically this would apply to an "a" parameter used for secondary triggering.

	public static double calc_q_gen_for_range_change (
		double b,
		double alpha,
		double mref_old,
		double mag_min_old,
		double mag_max_old,
		double mref_new,
		double mag_min_new,
		double mag_max_new
	) {

		// The exponential terms in Q_gen

		double v = C_LOG_10 * (alpha - b);
		double q = Math.exp(v*((mref_new - mag_min_new) - (mref_old - mag_min_old)));

		// If the arguments to W are very small, use the approximation W = 1

		double delta_max_min_old = mag_max_old - mag_min_old;
		double delta_max_min_new = mag_max_new - mag_min_new;

		if (Math.max (Math.abs(v*delta_max_min_old), Math.abs(v*delta_max_min_new)) <= 1.0e-16) {
			q = q * (delta_max_min_old / delta_max_min_new);
		}

		// Otherwise, use the formula for W, noting that the factor v cancels out in the fraction

		else {
			q = q * (Math.expm1(v*delta_max_min_old) / Math.expm1(v*delta_max_min_new));
		}

		// Return generalized correction factor Q_gen

		return q;
	}




	// Calculate the generalized factor "Q" for a reference magnitude change only.
	// Parameters:
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref_old = Reference magnitude, original value.
	//  mref_new = Reference magnitude, new value.
	//
	// Returns the generalized conversion factor Q_gen, defined as:
	//
	//   Q_gen = ((10^a_new)*Q_new) / ((10^a_old)*Q_old)
	//
	//   a_old = Productivity "a" defined on magnitude range [mref_old, msup_old].
	//   Q_old = 1.
	//   a_new = Productivity "a" defined on magnitude range [mref_new, msup_new].
	//   Q_new = 1.
	//
	// The formula is:
	//
	//   Q_gen = exp(v*(mref_new - mref_old))
	//
	//   v = log(10) * (alpha - b)
	//
	// Notice that if alpha == b then this reduces to
	//
	//   Q_gen = 1
	//
	// Also notice that msup_old and msup_new do not appear in the formulas.
	//
	// This function can be used to convert productivity, which is scaled
	// according to reference magnitude but NOT according to magnitude range.
	// Typically this would apply to an "ams" parameter used for mainshock triggering.

	public static double calc_q_gen_for_ref_mag_change (
		double b,
		double alpha,
		double mref_old,
		double mref_new
	) {

		// The exponential terms in Q_gen

		double v = C_LOG_10 * (alpha - b);
		double q = Math.exp(v*(mref_new - mref_old));

		// Return generalized correction factor Q_gen

		return q;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEStatsCalc : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  a  p  c  b  alpha  mref  msup  tint
		// Calculate the branch ratio from the given parameters.
		// Then check by re-computing the value of a.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 8 additional arguments

			if (!( args.length == 9 )) {
				System.err.println ("OEStatsCalc : Invalid 'test1' subcommand");
				return;
			}

			try {

				double a = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tint = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Computing branch ratio, given productivity a-value");
				System.out.println ("a = " + a);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tint = " + tint);

				// Convert

				double n = calc_branch_ratio (
					a,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tint
				);

				double a_check = calc_inv_branch_ratio (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tint
				);

				// Display result

				System.out.println();
				System.out.println ("n = " + rndd(n));

				System.out.println();
				System.out.println ("a_check = " + rndd(a_check));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  n  p  c  b  alpha  mref  msup  tint
		// Calculate the value of a from the given parameters.
		// Then check by re-computing the branch ratio.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 8 additional arguments

			if (!( args.length == 9 )) {
				System.err.println ("OEStatsCalc : Invalid 'test2' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				double mref = Double.parseDouble (args[6]);
				double msup = Double.parseDouble (args[7]);
				double tint = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Computing productivity a-value, given branch ratio");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("mref = " + mref);
				System.out.println ("msup = " + msup);
				System.out.println ("tint = " + tint);

				// Convert

				double a = calc_inv_branch_ratio (
					n,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tint
				);

				double n_check = calc_branch_ratio (
					a,
					p,
					c,
					b,
					alpha,
					mref,
					msup,
					tint
				);

				// Display result

				System.out.println();
				System.out.println ("a = " + rndd(a));

				System.out.println();
				System.out.println ("n_check = " + rndd(n_check));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEStatsCalc : Unrecognized subcommand : " + args[0]);
		return;

	}




}
