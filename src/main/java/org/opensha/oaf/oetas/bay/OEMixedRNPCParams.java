package org.opensha.oaf.oetas.bay;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

import java.io.IOException;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEArraysCalc;
import org.opensha.oaf.oetas.util.OEDiscreteRange;

// For Colt 0.9.4
import cern.jet.stat.tdouble.Probability;
// Probability.errorFunction(x) is the error function erf(x).
// Probability.errorFunctionComplemented is the complemented error function erfc(x) = 1 - erf(x)

// For Apache commons math
// import org.apache.commons.math3.special.Erf;
// Erf.erf(x) is the error function erf(x).
// Erf.erfc is the complemented error function erfc(x) = 1 - erf(x)

// Used only for initial import from the CSV file
import org.opensha.commons.data.CSVFile;


// Class to hold parameters for a mixed distribution on rel-zams/n/p/c.
// Author: Michael Barall.

public class OEMixedRNPCParams implements Marshalable {

	//----- Configurable parameters -----

	// Name of the tectonic regime (must not be null).

	private String regimeName;

	// Parameters for a triangle distribution with exponential tails, used for relative zams.
	// Requires:
	//  rtx1 < rtx0 < rtx2
	//  rty1 < rty0
	//  rty2 < rty0
	// (rtx0, rty0) is the apex of the triangle.
	// (rtx1, rty1) is the point where the left tail transitions from linear to exponential.
	// (rtx2, rty2) is the point where the right tail transitions from linear to exponential.

	private double rtx0;
	private double rty0;
	private double rtx1;
	private double rty1;
	private double rtx2;
	private double rty2;

	// Parameters for a normal distribution, used for log(c).
	// cmu is the value of c [note, not log10(c)] at the peak.
	// csigma is the standard deviation, in log10 units.

	private double cmu;
	private double csigma;

	// Parameters for a Cauchy distribution, used for p.
	// pmu is the value of p at the peak.
	// psigma is the scale parameter.

	private double pmu;
	private double psigma;

	// Parameters for a skew normal distribution, used for log(n).
	// nzeta is the location parameter, expressed as a value of n [note, not log10(n)].
	// nomega is the scale parameter, in log10 units.
	// nalpha is the shape parameter (negative for left skew, i.e., long tail on the left).

	private double nzeta;
	private double nomega;
	private double nalpha;

	// Parameters for an alternate triangle distribution with exponential tails, used for relative zams.
	// This fit does not compensate for covariance between n and relative zams, and so is much wider.

	private double alt_rtx0;
	private double alt_rty0;
	private double alt_rtx1;
	private double alt_rty1;
	private double alt_rtx2;
	private double alt_rty2;

	// Parameters for an altername skew normal distribution, used for log(n).
	// This fit is derived only from sequences with sufficient activity to allow for a
	// simultaneous fit of n, p, and c; and is therefore higher.

	private double alt_nzeta;
	private double alt_nomega;
	private double alt_nalpha;




	//----- Assumed parameters -----

	// Flag to use alternate fit for relative zams.

	private boolean f_use_alt_fit_zams;

	// Flag to use alternate fit for n.

	private boolean f_use_alt_fit_n;




	// Set up assumed parameters, version 1.
	// Assumes that the configurable parameters are already set up.

	private void setup_assumed_params_v1 () {

		// Use standard fit for relative zams.

		f_use_alt_fit_zams = false;

		// Use standard fit for n.

		f_use_alt_fit_n = false;

		return;
	}




	//----- Derived parameters -----

	// Log normalization factor for the fit to relative zams.

	private double log_norm_fit_zams;

	// Log normalization factor for the fit to c.

	private double log_norm_fit_c;

	// Log normalization factor for the fit to p.

	private double log_norm_fit_p;

	// Log normalization factor for the fit to n.

	private double log_norm_fit_n;

	// Log normalization factor for the alternate fit to relative zams.

	private double log_norm_alt_fit_zams;

	// Log normalization factor for the alternate fit to n.

	private double log_norm_alt_fit_n;




	// Set up derived parameters.
	// Assumes that the configurable and assumed parameters are already set up.

	private void setup_derived_params () {

		// Normalization factor for fit to zams is the reciprocal of the area under the triangle curve

		log_norm_fit_zams = -Math.log (calc_area_triexp (rtx0, rty0, rtx1, rty1, rtx2, rty2));

		// Normalization factor for normal fit to c

		log_norm_fit_c = -0.5 * Math.log (2.0 * Math.PI * csigma * csigma);

		// Normalization factor for Cauchy fit to p

		log_norm_fit_p = -Math.log (Math.PI * psigma);

		// Normalization factor for skew normal fit to n

		log_norm_fit_n = -Math.log (calc_area_ulogskewnorm (nzeta, nomega, nalpha));

		// Normalization factor for alternate fit to zams is the reciprocal of the area under the triangle curve

		log_norm_alt_fit_zams = -Math.log (calc_area_triexp (alt_rtx0, alt_rty0, alt_rtx1, alt_rty1, alt_rtx2, alt_rty2));

		// Normalization factor for alternate skew normal fit to n

		log_norm_alt_fit_n = -Math.log (calc_area_ulogskewnorm (alt_nzeta, alt_nomega, alt_nalpha));

		return;
	}





	//----- Evaluation -----




	// Get the regime name.

	public final String get_regimeName () {
		return regimeName;
	}




	// Accessor functions.

	public final double get_rtx0 () {
		return rtx0;
	}

	public final double get_rty0 () {
		return rty0;
	}

	public final double get_rtx1 () {
		return rtx1;
	}

	public final double get_rty1 () {
		return rty1;
	}

	public final double get_rtx2 () {
		return rtx2;
	}

	public final double get_rty2 () {
		return rty2;
	}

	public final double get_cmu () {
		return cmu;
	}

	public final double get_csigma () {
		return csigma;
	}

	public final double get_pmu () {
		return pmu;
	}

	public final double get_psigma () {
		return psigma;
	}

	public final double get_nzeta () {
		return nzeta;
	}

	public final double get_nomega () {
		return nomega;
	}

	public final double get_nalpha () {
		return nalpha;
	}

	public final double get_alt_rtx0 () {
		return alt_rtx0;
	}

	public final double get_alt_rty0 () {
		return alt_rty0;
	}

	public final double get_alt_rtx1 () {
		return alt_rtx1;
	}

	public final double get_alt_rty1 () {
		return alt_rty1;
	}

	public final double get_alt_rtx2 () {
		return alt_rtx2;
	}

	public final double get_alt_rty2 () {
		return alt_rty2;
	}

	public final double get_alt_nzeta () {
		return alt_nzeta;
	}

	public final double get_alt_nomega () {
		return alt_nomega;
	}

	public final double get_alt_nalpha () {
		return alt_nalpha;
	}

	public final boolean get_f_use_alt_fit_zams () {
		return f_use_alt_fit_zams;
	}

	public final boolean get_f_use_alt_fit_n () {
		return f_use_alt_fit_n;
	}




	// Calculate the log of the value of the triangle distribution with exponential tails.
	// Requires:
	//  x1 < x0 < x2
	//  y1 < y0
	//  y2 < y0
	// (x0, y0) is the apex of the triangle.
	// (x1, y1) is the point where the left tail transitions from linear to exponential.
	// (x2, y2) is the point where the right tail transitions from linear to exponential.
	// The return value has a maximum value of log(y0).

	public static double calc_log_triexp (double x, double x0, double y0, double x1, double y1, double x2, double y2) {
		double y;

		if (x < x0) {
			double r = (x - x1)/(x0 - x1);
			if (x < x1) {
				y = Math.log(y1) + r * (y0/y1 - 1.0);
			} else {
				y = Math.log (r * y0 + (1.0 - r) * y1);
			}
		} else {
			double r = (x - x2)/(x0 - x2);
			if (x > x2) {
				y = Math.log(y2) + r * (y0/y2 - 1.0);
			} else {
				y = Math.log (r * y0 + (1.0 - r) * y2);
			}
		}

		return y;
	}




	// Calculate the area under the triangle distribution with exponential tails.
	// Requires:
	//  x1 < x0 < x2
	//  y1 < y0
	//  y2 < y0
	// (x0, y0) is the apex of the triangle.
	// (x1, y1) is the point where the left tail transitions from linear to exponential.
	// (x2, y2) is the point where the right tail transitions from linear to exponential.

	public static double calc_area_triexp (double x0, double y0, double x1, double y1, double x2, double y2) {

		double area = 0.5 * y0 * (x2 - x1)
					+ (0.5 + 1.0/(y0/y1 - 1.0)) * y1 * (x0 - x1)
					+ (0.5 + 1.0/(y0/y2 - 1.0)) * y2 * (x2 - x0);

		return area;
	}




	// Calculate the log of the fit to relative zams.

	public final double calc_log_fit_zams (double zams) {
		return calc_log_triexp (zams, rtx0, rty0, rtx1, rty1, rtx2, rty2) + log_norm_fit_zams;
	}




	// Calculate the log of the alternate fit to relative zams.

	public final double calc_log_alt_fit_zams (double zams) {
		return calc_log_triexp (zams, alt_rtx0, alt_rty0, alt_rtx1, alt_rty1, alt_rtx2, alt_rty2) + log_norm_alt_fit_zams;
	}




	// Calculate the log of the selected fit to relative zams.

	public final double calc_log_sel_fit_zams (double zams) {
		if (f_use_alt_fit_zams) {
			return calc_log_alt_fit_zams (zams);
		}
		return calc_log_fit_zams (zams);
	}




	// Calculate the log of the fit to c.

	public final double calc_log_fit_c (double c) {

		double r = (Math.log10(c) - Math.log10(cmu)) / csigma;

		return log_norm_fit_c - 0.5 * r * r;

	}




	// Calculate the log of the fit to p.

	public final double calc_log_fit_p (double p) {

		double r = (p - pmu) / psigma;

		return log_norm_fit_p - Math.log1p(r * r);

	}




	// Calculate the log of the unnormalized log-skew-normal distribution.
	// zeta is the location parameter, expressed as a value of x [note, not log10(x)].
	// omega is the scale parameter, in log10 units.
	// alpha is the shape parameter (negative for left skew, i.e., long tail on the left).

	public static double calc_log_ulogskewnorm (double x, double zeta, double omega, double alpha) {

		double r = (Math.log10(x) - Math.log10(zeta)) / omega;

		double s = Probability.errorFunctionComplemented (-alpha * r / Math.sqrt(2.0));

		return Math.log (Math.max (s, 1.0e-20)) - 0.5 * r * r;		// max avoids possibility of log(0)

	}




	// Calculate the area under the unnormalized log-skew-normal distribution.
	// zeta is the location parameter, expressed as a value of x [note, not log10(x)].
	// omega is the scale parameter, in log10 units.
	// alpha is the shape parameter (negative for left skew, i.e., long tail on the left).

	public static double calc_area_ulogskewnorm (double zeta, double omega, double alpha) {
		return Math.sqrt (2.0 * Math.PI * omega * omega);
	}




	// Calculate the log of the fit to n.

	public final double calc_log_fit_n (double n) {
		return calc_log_ulogskewnorm (n, nzeta, nomega, nalpha) + log_norm_fit_n;
	}




	// Calculate the log of the alternate fit to n.

	public final double calc_log_alt_fit_n (double n) {
		return calc_log_ulogskewnorm (n, alt_nzeta, alt_nomega, alt_nalpha) + log_norm_alt_fit_n;
	}




	// Calculate the log of the selected fit to n.

	public final double calc_log_sel_fit_n (double n) {
		if (f_use_alt_fit_n) {
			return calc_log_alt_fit_n (n);
		}
		return calc_log_fit_n (n);
	}




	// Calculate the log-prior likelhood for given n, p, and c.

	public final double log_prior_likelihood_n_p_c (double n, double p, double c) {

		double log_like = calc_log_sel_fit_n (n) + calc_log_fit_p (p) + calc_log_fit_c (c);
		return log_like;
	}




	// Calculate the log-prior likelhood for given relative zams.
	// Note: The value of zams must be relative.

	public final double log_prior_likelihood_zams (double zams) {

		double log_like = calc_log_sel_fit_zams (zams);
		return log_like;
	}




	// Calculate the log-prior likelhood for given relative zams, n, p, and c.
	// Note: The value of zams must be relative.

	public final double log_prior_likelihood_zams_n_p_c (double zams, double n, double p, double c) {

		double log_like = log_prior_likelihood_n_p_c (n, p, c)  + log_prior_likelihood_zams (zams);
		return log_like;
	}




	//----- Construction -----



	// Clear contents.

	public final void clear () {

		// Configurable parameters

		regimeName = "";
		rtx0 = 0.0;
		rty0 = 0.0;
		rtx1 = 0.0;
		rty1 = 0.0;
		rtx2 = 0.0;
		rty2 = 0.0;
		cmu = 0.0;
		csigma = 0.0;
		pmu = 0.0;
		psigma = 0.0;
		nzeta = 0.0;
		nomega = 0.0;
		nalpha = 0.0;
		alt_rtx0 = 0.0;
		alt_rty0 = 0.0;
		alt_rtx1 = 0.0;
		alt_rty1 = 0.0;
		alt_rtx2 = 0.0;
		alt_rty2 = 0.0;
		alt_nzeta = 0.0;
		alt_nomega = 0.0;
		alt_nalpha = 0.0;

		// Assumed parameters

		f_use_alt_fit_zams = false;
		f_use_alt_fit_n = false;

		// Derived parameters

		log_norm_fit_zams = 0.0;
		log_norm_fit_c = 0.0;
		log_norm_fit_p = 0.0;
		log_norm_fit_n = 0.0;
		log_norm_alt_fit_zams = 0.0;
		log_norm_alt_fit_n = 0.0;

		return;
	}




	// Default constructor.

	public OEMixedRNPCParams () {
		clear();
	}




	// Set the values.
	// This sets configurable parameters, and computes the rest.

	public final OEMixedRNPCParams set (
		String regimeName,
		double rtx0,
		double rty0,
		double rtx1,
		double rty1,
		double rtx2,
		double rty2,
		double cmu,
		double csigma,
		double pmu,
		double psigma,
		double nzeta,
		double nomega,
		double nalpha,
		double alt_rtx0,
		double alt_rty0,
		double alt_rtx1,
		double alt_rty1,
		double alt_rtx2,
		double alt_rty2,
		double alt_nzeta,
		double alt_nomega,
		double alt_nalpha
	) {
		this.regimeName	= regimeName;
		this.rtx0		= rtx0		;
		this.rty0		= rty0		;
		this.rtx1		= rtx1		;
		this.rty1		= rty1		;
		this.rtx2		= rtx2		;
		this.rty2		= rty2		;
		this.cmu		= cmu		;
		this.csigma		= csigma	;
		this.pmu		= pmu		;
		this.psigma		= psigma	;
		this.nzeta		= nzeta		;
		this.nomega		= nomega	;
		this.nalpha		= nalpha	;
		this.alt_rtx0	= alt_rtx0	;
		this.alt_rty0	= alt_rty0	;
		this.alt_rtx1	= alt_rtx1	;
		this.alt_rty1	= alt_rty1	;
		this.alt_rtx2	= alt_rtx2	;
		this.alt_rty2	= alt_rty2	;
		this.alt_nzeta	= alt_nzeta	;
		this.alt_nomega	= alt_nomega;
		this.alt_nalpha	= alt_nalpha;

		setup_assumed_params_v1();
		setup_derived_params();
		return this;
	}




	// Set the values.
	// This sets configurable and assumed parameters, and computes the rest.

	public final OEMixedRNPCParams set (
		String regimeName,
		double rtx0,
		double rty0,
		double rtx1,
		double rty1,
		double rtx2,
		double rty2,
		double cmu,
		double csigma,
		double pmu,
		double psigma,
		double nzeta,
		double nomega,
		double nalpha,
		double alt_rtx0,
		double alt_rty0,
		double alt_rtx1,
		double alt_rty1,
		double alt_rtx2,
		double alt_rty2,
		double alt_nzeta,
		double alt_nomega,
		double alt_nalpha,
		boolean f_use_alt_fit_zams,
		boolean f_use_alt_fit_n
	) {
		this.regimeName	= regimeName;
		this.rtx0		= rtx0		;
		this.rty0		= rty0		;
		this.rtx1		= rtx1		;
		this.rty1		= rty1		;
		this.rtx2		= rtx2		;
		this.rty2		= rty2		;
		this.cmu		= cmu		;
		this.csigma		= csigma	;
		this.pmu		= pmu		;
		this.psigma		= psigma	;
		this.nzeta		= nzeta		;
		this.nomega		= nomega	;
		this.nalpha		= nalpha	;
		this.alt_rtx0	= alt_rtx0	;
		this.alt_rty0	= alt_rty0	;
		this.alt_rtx1	= alt_rtx1	;
		this.alt_rty1	= alt_rty1	;
		this.alt_rtx2	= alt_rtx2	;
		this.alt_rty2	= alt_rty2	;
		this.alt_nzeta	= alt_nzeta	;
		this.alt_nomega	= alt_nomega;
		this.alt_nalpha	= alt_nalpha;

		this.f_use_alt_fit_zams	= f_use_alt_fit_zams;
		this.f_use_alt_fit_n	= f_use_alt_fit_n	;

		setup_derived_params();
		return this;
	}




	// Name of global regime.

	public static final String GLOBAL_REGIME = "GLOBAL";

	// Regime name for analyst-supplied parameters.

	public static final String ANALYST_REGIME = "ANALYST";


	// Set to global values.

	public final OEMixedRNPCParams set_to_global () {
		set (
			GLOBAL_REGIME,	// regimeName,
			0.0,			// rtx0,
			1000.0,			// rty0,
			-0.68,			// rtx1,
			275.0,			// rty1,
			0.45,			// rtx2,
			153.0,			// rty2,
			0.014,			// cmu,
			1.4,			// csigma,
			1.05,			// pmu,
			0.2,			// psigma,
			0.6,			// nzeta,
			0.7,			// nomega,
			-2.9,			// nalpha,
			0.0,			// alt_rtx0,
			1000.0,			// alt_rty0,
			-0.83,			// alt_rtx1,
			378.0,			// alt_rty1,
			1.43,			// alt_rtx2,
			145.0,			// alt_rty2,
			0.75,			// alt_nzeta,
			0.47,			// alt_nomega,
			-2.9			// alt_nalpha,
		);

		return this;
	}




//	// Copy a 2D array, or null.
//
//	private double[][] copy_2d_array_or_null (double[][] x) {
//		if (x == null) {
//			return null;
//		}
//		return OEArraysCalc.array_copy (x);
//	}




	// Copy the values.

	public final OEMixedRNPCParams copy_from (OEMixedRNPCParams other) {

		this.regimeName	= other.regimeName;
		this.rtx0		= other.rtx0		;
		this.rty0		= other.rty0		;
		this.rtx1		= other.rtx1		;
		this.rty1		= other.rty1		;
		this.rtx2		= other.rtx2		;
		this.rty2		= other.rty2		;
		this.cmu		= other.cmu			;
		this.csigma		= other.csigma		;
		this.pmu		= other.pmu			;
		this.psigma		= other.psigma		;
		this.nzeta		= other.nzeta		;
		this.nomega		= other.nomega		;
		this.nalpha		= other.nalpha		;
		this.alt_rtx0	= other.alt_rtx0	;
		this.alt_rty0	= other.alt_rty0	;
		this.alt_rtx1	= other.alt_rtx1	;
		this.alt_rty1	= other.alt_rty1	;
		this.alt_rtx2	= other.alt_rtx2	;
		this.alt_rty2	= other.alt_rty2	;
		this.alt_nzeta	= other.alt_nzeta	;
		this.alt_nomega	= other.alt_nomega	;
		this.alt_nalpha	= other.alt_nalpha	;

		this.f_use_alt_fit_zams		= other.f_use_alt_fit_zams		;
		this.f_use_alt_fit_n		= other.f_use_alt_fit_n			;

		this.log_norm_fit_zams		= other.log_norm_fit_zams		;
		this.log_norm_fit_c			= other.log_norm_fit_c			;
		this.log_norm_fit_p			= other.log_norm_fit_p			;
		this.log_norm_fit_n			= other.log_norm_fit_n			;
		this.log_norm_alt_fit_zams	= other.log_norm_alt_fit_zams	;
		this.log_norm_alt_fit_n		= other.log_norm_alt_fit_n		;

		return this;
	}




//	// Display a 2D array, or null.
//	// Assumes that second-level arrays are not null.
//
//	private void display_2d_array_or_null (StringBuilder sb, String name, double[][] x) {
//		if (x == null) {
//			sb.append (name + " = null" + "\n");
//		}
//		else {
//			for(int i = 0; i < x.length ; i++){
//				for(int j = 0; j < x[i].length ; j++){
//					sb.append (name + "[" + i + "][" + j + "] = " + x[i][j] + "\n");
//				}
//			}
//		}
//		return;
//	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEMixedRNPCParams:" + "\n");

		result.append ("regimeName = "	+ regimeName + "\n");
		result.append ("rtx0 = "		+ rtx0		 + "\n");
		result.append ("rty0 = "		+ rty0		 + "\n");
		result.append ("rtx1 = "		+ rtx1		 + "\n");
		result.append ("rty1 = "		+ rty1		 + "\n");
		result.append ("rtx2 = "		+ rtx2		 + "\n");
		result.append ("rty2 = "		+ rty2		 + "\n");
		result.append ("cmu = "			+ cmu		 + "\n");
		result.append ("csigma = "		+ csigma	 + "\n");
		result.append ("pmu = "			+ pmu		 + "\n");
		result.append ("psigma = "		+ psigma	 + "\n");
		result.append ("nzeta = "		+ nzeta		 + "\n");
		result.append ("nomega = "		+ nomega	 + "\n");
		result.append ("nalpha = "		+ nalpha	 + "\n");
		result.append ("alt_rtx0 = "	+ alt_rtx0	 + "\n");
		result.append ("alt_rty0 = "	+ alt_rty0	 + "\n");
		result.append ("alt_rtx1 = "	+ alt_rtx1	 + "\n");
		result.append ("alt_rty1 = "	+ alt_rty1	 + "\n");
		result.append ("alt_rtx2 = "	+ alt_rtx2	 + "\n");
		result.append ("alt_rty2 = "	+ alt_rty2	 + "\n");
		result.append ("alt_nzeta = "	+ alt_nzeta	 + "\n");
		result.append ("alt_nomega = "	+ alt_nomega + "\n");
		result.append ("alt_nalpha = "	+ alt_nalpha + "\n");

		result.append ("f_use_alt_fit_zams = "	+ f_use_alt_fit_zams + "\n");
		result.append ("f_use_alt_fit_n = "		+ f_use_alt_fit_n	 + "\n");

		result.append ("log_norm_fit_zams = "		+ log_norm_fit_zams		 + "\n");
		result.append ("log_norm_fit_c = "			+ log_norm_fit_c		 + "\n");
		result.append ("log_norm_fit_p = "			+ log_norm_fit_p		 + "\n");
		result.append ("log_norm_fit_n = "			+ log_norm_fit_n		 + "\n");
		result.append ("log_norm_alt_fit_zams = "	+ log_norm_alt_fit_zams	 + "\n");
		result.append ("log_norm_alt_fit_n = "		+ log_norm_alt_fit_n	 + "\n");

		return result.toString();
	}




	// Display our contents, showing only adjustable parameters

	public String to_string_2 () {
		StringBuilder result = new StringBuilder();

		result.append ("OEMixedRNPCParams:" + "\n");

		result.append ("regimeName = "	+ regimeName + "\n");
		result.append ("rtx0 = "		+ rtx0		 + "\n");
		result.append ("rty0 = "		+ rty0		 + "\n");
		result.append ("rtx1 = "		+ rtx1		 + "\n");
		result.append ("rty1 = "		+ rty1		 + "\n");
		result.append ("rtx2 = "		+ rtx2		 + "\n");
		result.append ("rty2 = "		+ rty2		 + "\n");
		result.append ("cmu = "			+ cmu		 + "\n");
		result.append ("csigma = "		+ csigma	 + "\n");
		result.append ("pmu = "			+ pmu		 + "\n");
		result.append ("psigma = "		+ psigma	 + "\n");
		result.append ("nzeta = "		+ nzeta		 + "\n");
		result.append ("nomega = "		+ nomega	 + "\n");
		result.append ("nalpha = "		+ nalpha	 + "\n");
		result.append ("alt_rtx0 = "	+ alt_rtx0	 + "\n");
		result.append ("alt_rty0 = "	+ alt_rty0	 + "\n");
		result.append ("alt_rtx1 = "	+ alt_rtx1	 + "\n");
		result.append ("alt_rty1 = "	+ alt_rty1	 + "\n");
		result.append ("alt_rtx2 = "	+ alt_rtx2	 + "\n");
		result.append ("alt_rty2 = "	+ alt_rty2	 + "\n");
		result.append ("alt_nzeta = "	+ alt_nzeta	 + "\n");
		result.append ("alt_nomega = "	+ alt_nomega + "\n");
		result.append ("alt_nalpha = "	+ alt_nalpha + "\n");

		result.append ("f_use_alt_fit_zams = "	+ f_use_alt_fit_zams + "\n");
		result.append ("f_use_alt_fit_n = "		+ f_use_alt_fit_n	 + "\n");

		return result.toString();
	}




	// Produce a one-line summary string.

	public final String summary_string () {
		String result = "OEMixedRNPCParams["
			+ regimeName	+ ", "
			+ rtx0			+ ", "
			+ rty0			+ ", "
			+ rtx1			+ ", "
			+ rty1			+ ", "
			+ rtx2			+ ", "
			+ rty2			+ ", "
			+ cmu			+ ", "
			+ csigma		+ ", "
			+ pmu			+ ", "
			+ psigma		+ ", "
			+ nzeta			+ ", "
			+ nomega		+ ", "
			+ nalpha		+ ", "
			+ alt_rtx0		+ ", "
			+ alt_rty0		+ ", "
			+ alt_rtx1		+ ", "
			+ alt_rty1		+ ", "
			+ alt_rtx2		+ ", "
			+ alt_rty2		+ ", "
			+ alt_nzeta		+ ", "
			+ alt_nomega	+ ", "
			+ alt_nalpha	+ "]";
		return result;
	}




	// Produce a one-line summary string, including assumed parameters.

	public final String summary_string_2 () {
		String result = "OEMixedRNPCParams["
			+ regimeName	+ ", "
			+ rtx0			+ ", "
			+ rty0			+ ", "
			+ rtx1			+ ", "
			+ rty1			+ ", "
			+ rtx2			+ ", "
			+ rty2			+ ", "
			+ cmu			+ ", "
			+ csigma		+ ", "
			+ pmu			+ ", "
			+ psigma		+ ", "
			+ nzeta			+ ", "
			+ nomega		+ ", "
			+ nalpha		+ ", "
			+ alt_rtx0		+ ", "
			+ alt_rty0		+ ", "
			+ alt_rtx1		+ ", "
			+ alt_rty1		+ ", "
			+ alt_rtx2		+ ", "
			+ alt_rty2		+ ", "
			+ alt_nzeta		+ ", "
			+ alt_nomega	+ ", "
			+ alt_nalpha	+ ", "
			+ f_use_alt_fit_zams	+ ", "
			+ f_use_alt_fit_n		+ "]";
		return result;
	}




	//----- Import from CSV -----




	// Import a row from the CSV.
	// Returns this object.

	private OEMixedRNPCParams import_csv_row (CSVFile<String> csv, int row) {

		// Read configurable parameters

		regimeName	= csv.get(row, 0).trim();
		rtx0		= Double.parseDouble(csv.get(row,  1));
		rty0		= Double.parseDouble(csv.get(row,  2));
		rtx1		= Double.parseDouble(csv.get(row,  3));
		rty1		= Double.parseDouble(csv.get(row,  4));
		rtx2		= Double.parseDouble(csv.get(row,  5));
		rty2		= Double.parseDouble(csv.get(row,  6));
		cmu			= Double.parseDouble(csv.get(row,  7));
		csigma		= Double.parseDouble(csv.get(row,  8));
		pmu			= Double.parseDouble(csv.get(row,  9));
		psigma		= Double.parseDouble(csv.get(row, 10));
		nzeta		= Double.parseDouble(csv.get(row, 11));
		nomega		= Double.parseDouble(csv.get(row, 12));
		nalpha		= Double.parseDouble(csv.get(row, 13));
		alt_rtx0	= Double.parseDouble(csv.get(row, 14));
		alt_rty0	= Double.parseDouble(csv.get(row, 15));
		alt_rtx1	= Double.parseDouble(csv.get(row, 16));
		alt_rty1	= Double.parseDouble(csv.get(row, 17));
		alt_rtx2	= Double.parseDouble(csv.get(row, 18));
		alt_rty2	= Double.parseDouble(csv.get(row, 19));
		alt_nzeta	= Double.parseDouble(csv.get(row, 20));
		alt_nomega	= Double.parseDouble(csv.get(row, 21));
		alt_nalpha	= Double.parseDouble(csv.get(row, 22));

		// Compute remaining parameters

		setup_assumed_params_v1();
		setup_derived_params();
		return this;
	}




	// Import all the rows from the CSV.
	// Returns a list of the resulting objects.
	// Note: The first row (row 0) consists of column headings, and so is skipped.

	private static List<OEMixedRNPCParams> import_csv_rows (CSVFile<String> csv) {
		if (!( csv.getNumCols() == 23 )) {
			throw new MarshalException ("OEMixedRNPCParams.import_csv_rows: Incorrect number of columns: " + csv.getNumCols());
		}

		List<OEMixedRNPCParams> result = new ArrayList<OEMixedRNPCParams>();

		for (int row = 1; row < csv.getNumRows(); row++) {
			result.add ((new OEMixedRNPCParams()).import_csv_row (csv, row));
		}

		return result;
	}




	// Import all the rows from the embedded CSV.
	// Returns a list of the resulting objects.

	public static List<OEMixedRNPCParams> import_embedded_csv () {
		List<OEMixedRNPCParams> result;

		try {
			URL paramsURL = OEConstants.class.getResource ("resources/mbMixedEtasParams_20250204.csv");
			CSVFile<String> csv = CSVFile.readURL (paramsURL, true);
			result = import_csv_rows (csv);
		}
		catch (Exception e) {
			throw new MarshalException ("OEMixedRNPCParams.import_embedded_csv: Error importing embedded CSV", e);
		}

		return result;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 145001;	// marshals only configurable parameters
	private static final int MARSHAL_VER_2 = 145002;	// marshals configurable and assumed parameters

	private static final String M_VERSION_NAME = "OEMixedRNPCParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalString ("regimeName"	, regimeName	);
			writer.marshalDouble ("rtx0"		, rtx0			);
			writer.marshalDouble ("rty0"		, rty0			);
			writer.marshalDouble ("rtx1"		, rtx1			);
			writer.marshalDouble ("rty1"		, rty1			);
			writer.marshalDouble ("rtx2"		, rtx2			);
			writer.marshalDouble ("rty2"		, rty2			);
			writer.marshalDouble ("cmu"			, cmu			);
			writer.marshalDouble ("csigma"		, csigma		);
			writer.marshalDouble ("pmu"			, pmu			);
			writer.marshalDouble ("psigma"		, psigma		);
			writer.marshalDouble ("nzeta"		, nzeta			);
			writer.marshalDouble ("nomega"		, nomega		);
			writer.marshalDouble ("nalpha"		, nalpha		);
			writer.marshalDouble ("alt_rtx0"	, alt_rtx0		);
			writer.marshalDouble ("alt_rty0"	, alt_rty0		);
			writer.marshalDouble ("alt_rtx1"	, alt_rtx1		);
			writer.marshalDouble ("alt_rty1"	, alt_rty1		);
			writer.marshalDouble ("alt_rtx2"	, alt_rtx2		);
			writer.marshalDouble ("alt_rty2"	, alt_rty2		);
			writer.marshalDouble ("alt_nzeta"	, alt_nzeta		);
			writer.marshalDouble ("alt_nomega"	, alt_nomega	);
			writer.marshalDouble ("alt_nalpha"	, alt_nalpha	);

		}
		break;

		case MARSHAL_VER_2: {

			writer.marshalString ("regimeName"	, regimeName	);
			writer.marshalDouble ("rtx0"		, rtx0			);
			writer.marshalDouble ("rty0"		, rty0			);
			writer.marshalDouble ("rtx1"		, rtx1			);
			writer.marshalDouble ("rty1"		, rty1			);
			writer.marshalDouble ("rtx2"		, rtx2			);
			writer.marshalDouble ("rty2"		, rty2			);
			writer.marshalDouble ("cmu"			, cmu			);
			writer.marshalDouble ("csigma"		, csigma		);
			writer.marshalDouble ("pmu"			, pmu			);
			writer.marshalDouble ("psigma"		, psigma		);
			writer.marshalDouble ("nzeta"		, nzeta			);
			writer.marshalDouble ("nomega"		, nomega		);
			writer.marshalDouble ("nalpha"		, nalpha		);
			writer.marshalDouble ("alt_rtx0"	, alt_rtx0		);
			writer.marshalDouble ("alt_rty0"	, alt_rty0		);
			writer.marshalDouble ("alt_rtx1"	, alt_rtx1		);
			writer.marshalDouble ("alt_rty1"	, alt_rty1		);
			writer.marshalDouble ("alt_rtx2"	, alt_rtx2		);
			writer.marshalDouble ("alt_rty2"	, alt_rty2		);
			writer.marshalDouble ("alt_nzeta"	, alt_nzeta		);
			writer.marshalDouble ("alt_nomega"	, alt_nomega	);
			writer.marshalDouble ("alt_nalpha"	, alt_nalpha	);

			writer.marshalBoolean ("f_use_alt_fit_zams", f_use_alt_fit_zams);
			writer.marshalBoolean ("f_use_alt_fit_n", f_use_alt_fit_n);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			regimeName = reader.unmarshalString ("regimeName");
			rtx0		= reader.unmarshalDouble ("rtx0");
			rty0		= reader.unmarshalDouble ("rty0");
			rtx1		= reader.unmarshalDouble ("rtx1");
			rty1		= reader.unmarshalDouble ("rty1");
			rtx2		= reader.unmarshalDouble ("rtx2");
			rty2		= reader.unmarshalDouble ("rty2");
			cmu			= reader.unmarshalDouble ("cmu");
			csigma		= reader.unmarshalDouble ("csigma");
			pmu			= reader.unmarshalDouble ("pmu");
			psigma		= reader.unmarshalDouble ("psigma");
			nzeta		= reader.unmarshalDouble ("nzeta");
			nomega		= reader.unmarshalDouble ("nomega");
			nalpha		= reader.unmarshalDouble ("nalpha");
			alt_rtx0	= reader.unmarshalDouble ("alt_rtx0");
			alt_rty0	= reader.unmarshalDouble ("alt_rty0");
			alt_rtx1	= reader.unmarshalDouble ("alt_rtx1");
			alt_rty1	= reader.unmarshalDouble ("alt_rty1");
			alt_rtx2	= reader.unmarshalDouble ("alt_rtx2");
			alt_rty2	= reader.unmarshalDouble ("alt_rty2");
			alt_nzeta	= reader.unmarshalDouble ("alt_nzeta");
			alt_nomega	= reader.unmarshalDouble ("alt_nomega");
			alt_nalpha	= reader.unmarshalDouble ("alt_nalpha");

			try {
				setup_assumed_params_v1();
				setup_derived_params();
			}
			catch (Exception e) {
				throw new MarshalException ("OEMixedRNPCParams.do_umarshal: Error completing setup", e);
			}

		}
		break;

		case MARSHAL_VER_2: {

			regimeName = reader.unmarshalString ("regimeName");
			rtx0		= reader.unmarshalDouble ("rtx0");
			rty0		= reader.unmarshalDouble ("rty0");
			rtx1		= reader.unmarshalDouble ("rtx1");
			rty1		= reader.unmarshalDouble ("rty1");
			rtx2		= reader.unmarshalDouble ("rtx2");
			rty2		= reader.unmarshalDouble ("rty2");
			cmu			= reader.unmarshalDouble ("cmu");
			csigma		= reader.unmarshalDouble ("csigma");
			pmu			= reader.unmarshalDouble ("pmu");
			psigma		= reader.unmarshalDouble ("psigma");
			nzeta		= reader.unmarshalDouble ("nzeta");
			nomega		= reader.unmarshalDouble ("nomega");
			nalpha		= reader.unmarshalDouble ("nalpha");
			alt_rtx0	= reader.unmarshalDouble ("alt_rtx0");
			alt_rty0	= reader.unmarshalDouble ("alt_rty0");
			alt_rtx1	= reader.unmarshalDouble ("alt_rtx1");
			alt_rty1	= reader.unmarshalDouble ("alt_rty1");
			alt_rtx2	= reader.unmarshalDouble ("alt_rtx2");
			alt_rty2	= reader.unmarshalDouble ("alt_rty2");
			alt_nzeta	= reader.unmarshalDouble ("alt_nzeta");
			alt_nomega	= reader.unmarshalDouble ("alt_nomega");
			alt_nalpha	= reader.unmarshalDouble ("alt_nalpha");

			f_use_alt_fit_zams = reader.unmarshalBoolean ("f_use_alt_fit_zams");
			f_use_alt_fit_n = reader.unmarshalBoolean ("f_use_alt_fit_n");

			try {
				setup_derived_params();
			}
			catch (Exception e) {
				throw new MarshalException ("OEMixedRNPCParams.do_umarshal: Error completing setup", e);
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
	public OEMixedRNPCParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEMixedRNPCParams mixed_rnpc_params) {
		mixed_rnpc_params.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEMixedRNPCParams static_unmarshal (MarshalReader reader, String name) {
		return (new OEMixedRNPCParams()).unmarshal (reader, name);
	}




	//----- Testing -----




	// Make a value to use for testing purposes.

	public static OEMixedRNPCParams make_test_value () {
		OEMixedRNPCParams mixed_rnpc_params = new OEMixedRNPCParams();

		mixed_rnpc_params.set_to_global ();

		return mixed_rnpc_params;
	}




	// Write a file containing function test values.
	// Parameters:
	//  xs = Array of function arguments.
	//  logfx = Array containing log(f(x)) for each argument.
	//  filename = Name of file to write.
	// Each line in the resulting file contains two values,
	// x and normalized f(x).
	// Function values are converted from log to linear, and normalized
	// so that the peak value is 1000.0.

	public static void write_fcn_test_values (double[] xs, double[] logfx, String filename) throws IOException {

		// Find maximum logfx

		double max_logfx = logfx[0];
		for (int i = 1; i < logfx.length; ++i) {
			max_logfx = Math.max (max_logfx, logfx[i]);
		}

		// Scale factor to mormalize max to 1000.0

		double scale = Math.log(1000.0) - max_logfx;

		// Make a string contining the file contents

		StringBuilder result = new StringBuilder();
		for (int i = 0; i < logfx.length; ++i) {
			result.append (xs[i] + " " + Math.exp (logfx[i] + scale) + "\n");
		}

		// Write the file

		SimpleUtils.write_string_as_file (filename, result.toString());

		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEMixedRNPCParams");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct test values, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing, displaying, marshaling, and copying catalog info");
			testargs.end_test();

			// Create the values

			OEMixedRNPCParams mixed_rnpc_params = make_test_value();

			// Display the contents

			System.out.println ();
			System.out.println ("********** Parameter Display **********");
			System.out.println ();

			System.out.println (mixed_rnpc_params.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal to JSON **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (mixed_rnpc_params);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (mixed_rnpc_params);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal from JSON **********");
			System.out.println ();
			
			OEMixedRNPCParams mixed_rnpc_params2 = new OEMixedRNPCParams();
			MarshalUtils.from_json_string (mixed_rnpc_params2, json_string);

			// Display the contents

			System.out.println (mixed_rnpc_params2.toString());

			// Copy values

			System.out.println ();
			System.out.println ("********** Copy info **********");
			System.out.println ();
			
			OEMixedRNPCParams mixed_rnpc_params3 = new OEMixedRNPCParams();
			mixed_rnpc_params3.copy_from (mixed_rnpc_params2);

			// Display the contents

			System.out.println (mixed_rnpc_params3.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  filename
		// Construct test values, and write them to a file.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Writing test values to a file");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEMixedRNPCParams mixed_rnpc_params = make_test_value();

			// Marshal to JSON

			String formatted_string = MarshalUtils.to_formatted_compact_json_string (mixed_rnpc_params);

			// Write the file

			SimpleUtils.write_string_as_file (filename, formatted_string);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  filename
		// Construct test values, and write them to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Writing test values to a file, raw JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEMixedRNPCParams mixed_rnpc_params = make_test_value();

			// Write to file

			MarshalUtils.to_json_file (mixed_rnpc_params, filename);

			// Read back the file and display it

			OEMixedRNPCParams mixed_rnpc_params2 = new OEMixedRNPCParams();
			MarshalUtils.from_json_file (mixed_rnpc_params2, filename);

			System.out.println ();
			System.out.println (mixed_rnpc_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  filename
		// Construct typical parameters, and write them to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Writing test values to a file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Create the values

			OEMixedRNPCParams mixed_rnpc_params = make_test_value();

			// Write to file

			MarshalUtils.to_formatted_json_file (mixed_rnpc_params, filename);

			// Read back the file and display it

			OEMixedRNPCParams mixed_rnpc_params2 = new OEMixedRNPCParams();
			MarshalUtils.from_json_file (mixed_rnpc_params2, filename);

			System.out.println ();
			System.out.println (mixed_rnpc_params2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Import values from the embedded CSV file, and display the resulting list.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Read embedded CSV file and display list");
			testargs.end_test();

			// Read embedded CSV file

			System.out.println ();
			System.out.println ("********** Read embedded CSV file **********");
			System.out.println ();

			List<OEMixedRNPCParams> params_list = import_embedded_csv();

			// Display the list

			System.out.println ();
			System.out.println ("********** Display list **********");
			System.out.println ();

			for (OEMixedRNPCParams params : params_list) {
				System.out.println (params.summary_string());
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  filename_prefix
		// Write files containing fitting function values.
		// Each function is evaluated at a pre-determined set of points,
		// converted from log to linear while normalizing to a maximum value of 1000,
		// and written to a file formed by appending a string to the filename prefix.

		if (testargs.is_test ("test6")) {

			// Read arguments

			System.out.println ("Writing files containing fitting function values");
			String filename_prefix = testargs.get_string ("filename_prefix");
			testargs.end_test();

			// Create the values

			OEMixedRNPCParams mixed_rnpc_params = make_test_value();

			// Fit to relative zams

			double[] xs;
			double[] logfx;

			xs = (OEDiscreteRange.makeLinear (81, -3.0, 3.0)).get_range_array();
			logfx = new double[xs.length];
			for (int i = 0; i < xs.length; ++i) {
				logfx[i] = mixed_rnpc_params.calc_log_fit_zams (xs[i]);
			}
			write_fcn_test_values (xs, logfx, filename_prefix + "zams.txt");

			// Alternate fit to relative zams

			xs = (OEDiscreteRange.makeLinear (81, -3.0, 3.0)).get_range_array();
			logfx = new double[xs.length];
			for (int i = 0; i < xs.length; ++i) {
				logfx[i] = mixed_rnpc_params.calc_log_alt_fit_zams (xs[i]);
			}
			write_fcn_test_values (xs, logfx, filename_prefix + "alt_zams.txt");

			// Fit to c

			xs = (OEDiscreteRange.makeLog (21, 1.0E-5, 1.0)).get_range_array();
			logfx = new double[xs.length];
			for (int i = 0; i < xs.length; ++i) {
				logfx[i] = mixed_rnpc_params.calc_log_fit_c (xs[i]);
			}
			write_fcn_test_values (xs, logfx, filename_prefix + "c.txt");

			//Fit to p

			xs = (OEDiscreteRange.makeLinear (37, 0.5, 2.0)).get_range_array();
			logfx = new double[xs.length];
			for (int i = 0; i < xs.length; ++i) {
				logfx[i] = mixed_rnpc_params.calc_log_fit_p (xs[i]);
			}
			write_fcn_test_values (xs, logfx, filename_prefix + "p.txt");

			// Fit to n

			xs = (OEDiscreteRange.makeLog (161, 0.025, 40.0)).get_range_array();
			logfx = new double[xs.length];
			for (int i = 0; i < xs.length; ++i) {
				logfx[i] = mixed_rnpc_params.calc_log_fit_n (xs[i]);
			}
			write_fcn_test_values (xs, logfx, filename_prefix + "n.txt");

			// Alternate fit to n

			xs = (OEDiscreteRange.makeLog (161, 0.025, 40.0)).get_range_array();
			logfx = new double[xs.length];
			for (int i = 0; i < xs.length; ++i) {
				logfx[i] = mixed_rnpc_params.calc_log_alt_fit_n (xs[i]);
			}
			write_fcn_test_values (xs, logfx, filename_prefix + "alt_n.txt");

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
