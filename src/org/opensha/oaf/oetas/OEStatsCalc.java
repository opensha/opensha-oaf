package org.opensha.oaf.oetas;

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




}
