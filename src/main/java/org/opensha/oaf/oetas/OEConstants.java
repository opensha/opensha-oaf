package org.opensha.oaf.oetas;

import org.opensha.oaf.oetas.util.OEDiscreteRange;
import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.util.SimpleUtils;


// Holds constants used by Operational ETAS.
// Author: Michael Barall 11/27/2019.

public class OEConstants {


	//----- Fixed constants -----


	// The number of milliseconds in a day.

	public static final double C_MILLIS_PER_DAY = 86400000.0;
	public static final long L_MILLIS_PER_DAY = 86400000L;

	// The number of milliseconds in a week.
	// Note: A week is defined to be 7 days.

	public static final double C_MILLIS_PER_WEEK = 604800000.0;
	public static final long L_MILLIS_PER_WEEK = 604800000L;

	// The number of milliseconds in a month.
	// Note: A month is defined to be 30 days.

	public static final double C_MILLIS_PER_MONTH = 2592000000.0;
	public static final long L_MILLIS_PER_MONTH = 2592000000L;

	// The number of milliseconds in a year.
	// Note: A year is defined to be 365 days.

	public static final double C_MILLIS_PER_YEAR = 31536000000.0;
	public static final long L_MILLIS_PER_YEAR = 31536000000L;

	// The natural logarithm of 10.

	public static final double C_LOG_10 = 2.3025850929940457;




	//----- Magnitudes -----




	// A postive magnitude larger than any possible magnitude.
	// Note: Chosen to be exactly representable in typical floating point
	// implementations, and in the encoding of CompactEqkRupList.

	public static final double NO_MAG_POS = 11.875;

	// Use x >= NO_MAG_POS_CHECK to check if x contains NO_MAG_POS.

	public static final double NO_MAG_POS_CHECK = 11.75;

	// A negative magnitude smaller than any possible magnitude.
	// Note: Chosen to be exactly representable in typical floating point
	// implementations, and in the encoding of CompactEqkRupList.

	public static final double NO_MAG_NEG = -11.875;

	// Use x <= NO_MAG_NEG_CHECK to check if x contains NO_MAG_NEG.

	public static final double NO_MAG_NEG_CHECK = -11.75;



	// A postive magnitude used to represent positive infinity.

	public static final double INFINITE_MAG_POS = 99.9;

	// Use x >= INFINITE_MAG_POS_CHECK to check if x contains INFINITE_MAG_POS.

	public static final double INFINITE_MAG_POS_CHECK = 99.0;

	// A negative magnitude used to represent negative infinity.

	public static final double INFINITE_MAG_NEG = -99.9;

	// Use x <= INFINITE_MAG_NEG_CHECK to check if x contains INFINITE_MAG_NEG.

	public static final double INFINITE_MAG_NEG_CHECK = -99.0;

	// A very small change in magnitude, smaller than any actual change.

	public static final double TINY_MAG_DELTA = 0.000001;




	//----- Times -----




	// A time value or difference much larger than any actual value, in days.
	// (But still much smaller than the range of double.)

	public static final double HUGE_TIME_DAYS = 1.0e+20;

	// Use x >= HUGE_TIME_DAYS_CHECK to check if x contains HUGE_TIME_DAYS.

	public static final double HUGE_TIME_DAYS_CHECK = 0.99e+20;

	// The logarithm base 10 of HUGE_TIME_DAYS.

	public static final double LOG10_HUGE_TIME_DAYS = 20;



	// A time value used to indicate that a rupture is a background source, in days.
	// Note: This is within the range representable by a 64-bit signed integer number
	// of milliseconds, which is about 1.0675e+11 days.  Also, displaying to 5 decimal
	// places is within the range of double precision.

	public static final double BKGD_TIME_DAYS = -1.0e+8;

	// Use x <= BKGD_TIME_DAYS_CHECK to check if x contains BKGD_TIME_DAYS.

	public static final double BKGD_TIME_DAYS_CHECK = -0.99e+8;




	//----- Negligability thresholds -----




	// A duration so extremely small it is considered to be zero.
	// Note: This is used to avoid divide-by-zero in formulas that have a
	// duration in the denominator; it does not mean that durations larger
	// than this are considered significant.
	
	public static final double TINY_DURATION_DAYS = 1.0e-150;

	// An Omori rate so extremely small it is considered to be zero.
	// Note: This is used to avoid divide-by-zero in formulas that have an
	// Omori rate in the denominator; it does not mean that rates larger
	// than this are considered significant.
	
	public static final double TINY_OMORI_RATE = 1.0e-150;

	// A background rate so extremely small it is considered to be zero.
	// Note: This is used to test if background rate code needs to be executed;
	// it does not mean that rates larger than this are considered significant.
	
	public static final double TINY_BACKGROUND_RATE = 1.0e-150;

	// An expected count of earthquakes small enough to treat as zero.

	public static final double SMALL_EXPECTED_COUNT = 0.001;




	//----- Accumulation and extrapolation -----




	// Methods for infilling event counts below the simulated magnitude range.

	public static final int INFILL_METH_MIN			= 1;
	public static final int INFILL_METH_NONE		= 1;	// No infill, use exactly as simulated
	public static final int INFILL_METH_SCALE		= 2;	// Scale up, using G-R relation
	public static final int INFILL_METH_POISSON		= 3;	// Use Poisson random var applied to expected rate
	public static final int INFILL_METH_STERILE		= 4;	// Generate sterile ruptures
	public static final int INFILL_METH_MAX			= 4;

	// Return a string describing the infill method.

	public static String get_infill_method_as_string (int infill_meth) {
		switch (infill_meth) {
		case INFILL_METH_NONE: return "INFILL_METH_NONE";
		case INFILL_METH_SCALE: return "INFILL_METH_SCALE";
		case INFILL_METH_POISSON: return "INFILL_METH_POISSON";
		case INFILL_METH_STERILE: return "INFILL_METH_STERILE";
		}
		return "INFILL_METH_INVALID(" + infill_meth + ")";
	}



	// Methods for magfilling event counts outside the simulated magnitude range.

	public static final int MAGFILL_METH_MIN			= 1;
	public static final int MAGFILL_METH_NONE			= 1;	// No magfill, use exactly as simulated
	public static final int MAGFILL_METH_PDF_ONLY		= 2;	// Use probability distribution function with expected rate over entire mag range
	public static final int MAGFILL_METH_PDF_HYBRID		= 3;	// Combine counts with a pdf derived from expected rate outside mag range
	public static final int MAGFILL_METH_PDF_STERILE	= 4;	// Combine counts, sterile ruptures, and a pdf derived from expected rate above mag range
	public static final int MAGFILL_METH_PDF_CTU_HYBRID	= 5;	// Combine counts with a pdf derived from expected rate outside mag range, with count-based upfill for probability of occurrence
	public static final int MAGFILL_METH_MAX			= 5;

	// Return a string describing the magfill method.

	public static String get_magfill_method_as_string (int magfill_meth) {
		switch (magfill_meth) {
		case MAGFILL_METH_NONE: return "MAGFILL_METH_NONE";
		case MAGFILL_METH_PDF_ONLY: return "MAGFILL_METH_PDF_ONLY";
		case MAGFILL_METH_PDF_HYBRID: return "MAGFILL_METH_PDF_HYBRID";
		case MAGFILL_METH_PDF_STERILE: return "MAGFILL_METH_PDF_STERILE";
		case MAGFILL_METH_PDF_CTU_HYBRID: return "MAGFILL_METH_PDF_CTU_HYBRID";
		}
		return "MAGFILL_METH_INVALID(" + magfill_meth + ")";
	}



	// Methods for outfilling event counts outside the simulated time range.

	public static final int OUTFILL_METH_MIN		= 1;
	public static final int OUTFILL_METH_NONE		= 1;	// No outfill, use exactly as simulated, zero-filling after end of simulation
	public static final int OUTFILL_METH_OMIT		= 2;	// Do not participate in probability distributions after end of simulation
	public static final int OUTFILL_METH_PDF_DIRECT	= 3;	// Use pdf derived from expected rate of direct aftershocks
	public static final int OUTFILL_METH_MAX		= 3;

	// Return a string describing the outfill method.

	public static String get_outfill_method_as_string (int outfill_meth) {
		switch (outfill_meth) {
		case OUTFILL_METH_NONE: return "OUTFILL_METH_NONE";
		case OUTFILL_METH_OMIT: return "OUTFILL_METH_OMIT";
		case OUTFILL_METH_PDF_DIRECT: return "OUTFILL_METH_PDF_DIRECT";
		}
		return "OUTFILL_METH_INVALID(" + outfill_meth + ")";
	}



	// Methods for selecting which catalogs are sufficiently long and determining their time range..

	public static final int CATLEN_METH_MIN			= 1;
	public static final int CATLEN_METH_ANY			= 1;	// Any catalog length is acceptable
	public static final int CATLEN_METH_ANY_CLIP	= 2;	// Any catalog length is acceptable, and clip to a full time bin
	public static final int CATLEN_METH_RANGE		= 3;	// Catalog must cover all active time bins
	public static final int CATLEN_METH_ENTIRE		= 4;	// Catalog must have reached simulation end time
	public static final int CATLEN_METH_ENTIRE_CLIP	= 5;	// Catalog must have reached simulation end time, and clip to a full time bin
	public static final int CATLEN_METH_MAX			= 5;

	// Return a string describing the catlen method.

	public static String get_catlen_method_as_string (int catlen_meth) {
		switch (catlen_meth) {
		case CATLEN_METH_ANY: return "CATLEN_METH_ANY";
		case CATLEN_METH_ANY_CLIP: return "CATLEN_METH_ANY_CLIP";
		case CATLEN_METH_RANGE: return "CATLEN_METH_RANGE";
		case CATLEN_METH_ENTIRE: return "CATLEN_METH_ENTIRE";
		case CATLEN_METH_ENTIRE_CLIP: return "CATLEN_METH_ENTIRE_CLIP";
		}
		return "CATLEN_METH_INVALID(" + catlen_meth + ")";
	}



	// Combine catalog length, outfill, and magfill methods into a single rate accumulation method.
	// The combined form is three digits: catlen, outfill, magfill.

	public static int make_rate_acc_meth (int catlen_meth, int outfill_meth, int magfill_meth) {
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.make_rate_acc_meth: Invalid catalog length method: catlen_meth = " + catlen_meth);
		}
		if (!( outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.make_rate_acc_meth: Invalid outfill method: outfill_meth = " + outfill_meth);
		}
		if (!( magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.make_rate_acc_meth: Invalid magnitude fill method: magfill_meth = " + magfill_meth);
		}
		int rate_acc_meth = (catlen_meth * 100) + (outfill_meth * 10) + magfill_meth;
		return rate_acc_meth;
	}

	// Extract catalog length, outfill, and magfill methods from a single rate accumulation method.

	public static int extract_catlen_from_rate_acc (int rate_acc_meth) {
		int catlen_meth = rate_acc_meth / 100;
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.extract_catlen_from_rate_acc: Invalid catalog length method: rate_acc_meth = " + rate_acc_meth + ", catlen_meth = " + catlen_meth);
		}
		return catlen_meth;
	}

	public static int extract_outfill_from_rate_acc (int rate_acc_meth) {
		int outfill_meth = (rate_acc_meth / 10) % 10;
		if (!( outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.extract_outfill_from_rate_acc: Invalid outfill method: rate_acc_meth = " + rate_acc_meth + ", outfill_meth = " + outfill_meth);
		}
		return outfill_meth;
	}

	public static int extract_magfill_from_rate_acc (int rate_acc_meth) {
		int magfill_meth = rate_acc_meth % 10;
		if (!( magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.extract_magfill_from_rate_acc: Invalid magnitude fill method: rate_acc_meth = " + rate_acc_meth + ", magfill_meth = " + magfill_meth);
		}
		return magfill_meth;
	}

	// Validate rate accumulation method.
	// Return true if valid, false if invalid.

	public static boolean validate_rate_acc_meth (int rate_acc_meth) {
		int catlen_meth = rate_acc_meth / 100;
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX )) {
			return false;
		}
		int outfill_meth = (rate_acc_meth / 10) % 10;
		if (!( outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX )) {
			return false;
		}
		int magfill_meth = rate_acc_meth % 10;
		if (!( magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX )) {
			return false;
		}
		return true;
	}

	// Return a string describing the rate accumulation method.

	public static String get_rate_acc_meth_as_string (int rate_acc_meth) {
		int catlen_meth = rate_acc_meth / 100;
		int outfill_meth = (rate_acc_meth / 10) % 10;
		int magfill_meth = rate_acc_meth % 10;
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX
			&& outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX
			&& magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX
		)) {
			return "RATE_ACC_METH_INVALID(" + rate_acc_meth + ")";
		}
		return get_catlen_method_as_string (catlen_meth)
				+ " + " + get_outfill_method_as_string (outfill_meth)
				+ " + " + get_magfill_method_as_string (magfill_meth);
	}



	// Codes for selecting an accumulator

	public static final int SEL_ACCUM_MIN				= 1;
	public static final int SEL_ACCUM_NONE				= 1;	// No accumulator
	public static final int SEL_ACCUM_CUM_TIME_MAG		= 2;	// OEAccumCumTimeMag
	public static final int SEL_ACCUM_VAR_TIME_MAG		= 3;	// OEAccumVarTimeMag
	public static final int SEL_ACCUM_RATE_TIME_MAG		= 4;	// OEAccumRateTimeMag
	public static final int SEL_ACCUM_SIM_RANGING		= 5;	// OEAccumSimRanging
	public static final int SEL_ACCUM_SEED_EST_RANGING	= 6;	// OEAccumSeedEstRanging
	public static final int SEL_ACCUM_MAX				= 6;



	// Codes for selecting a ranging method

	public static final int RANGING_METH_MIN			= 1;
	public static final int RANGING_METH_SIM			= 1;	// Via simulation, using OEAccumSimRanging
	public static final int RANGING_METH_SEED_EST		= 2;	// Via seed estimation, using OEAccumSeedEstRanging
	public static final int RANGING_METH_VAR_SEED_EST	= 3;	// Via seed estimation, variable range on a per-catalog basis
	public static final int RANGING_METH_MAX			= 3;




	//----- Catalog generation -----




	// Negligably small time interval, in days, when generating catalogs.

	public static final double GEN_TIME_EPS = 0.00001;

	// Negligably small magnitude interval, when generating catalogs.

	public static final double GEN_MAG_EPS = 0.0002;

	// Default maximum number of generations, when generating catalogs.

	public static final int DEF_MAX_GEN_COUNT = 100;

	// Default maximum number of ruptures in a catalog, when generating catalogs; 0 if none.

	public static final int DEF_MAX_CAT_SIZE = 5000000;

	// Default magnitude excess for selecting stop time, when generating catalogs; 0.0 if none.

	public static final double DEF_MAG_EXCESS = 2.0;

	// Value to disable use of magnitude excess for selecting stop time.

	public static final double ZERO_MAG_EXCESS = 0.0;

	// Minimum range of magnitudes to permit, when generating catalogs.

	public static final double GEN_MIN_MAG_RANGE = 2.0;




	// Catalog result codes.

	public static final int CAT_RESULT_MIN = 0;				// Must be 0 so these can be used as array indexes
	public static final int CAT_RESULT_OK = 0;				// Success
	public static final int CAT_RESULT_EARLY_STOP = 1;		// Success, but catalog stopped before end time
	public static final int CAT_RESULT_CAT_TOO_LARGE = 2;	// Catalog is too large
	public static final int CAT_RESULT_TOO_MANY_GEN = 3;	// Catalog has too many generations
	public static final int CAT_RESULT_GEN_TOO_LARGE = 4;	// Generation is too large
	public static final int CAT_RESULT_MAX = 4;

	// Return true if the result code indicates success

	public static boolean is_cat_result_success (int cat_result) {
		switch (cat_result) {
		case CAT_RESULT_OK:
		case CAT_RESULT_EARLY_STOP:
			return true;
		}
		return false;
	}

	// Return a string identifying the result code

	public static String cat_result_to_string (int cat_result) {
		switch (cat_result) {
		case CAT_RESULT_OK: return "CAT_RESULT_OK";
		case CAT_RESULT_EARLY_STOP: return "CAT_RESULT_EARLY_STOP";
		case CAT_RESULT_CAT_TOO_LARGE: return "CAT_RESULT_CAT_TOO_LARGE";
		case CAT_RESULT_TOO_MANY_GEN: return "CAT_RESULT_TOO_MANY_GEN";
		case CAT_RESULT_GEN_TOO_LARGE: return "CAT_RESULT_GEN_TOO_LARGE";
		}
		return "CAT_RESULT_INVALID(" + cat_result + ")";
	}




	//----- Source grouping -----




	// Negligably small time interval, in days, when grouping sources.

	public static final double GROUP_TIME_EPS = 0.001;

	// Default group span width relative base time, in days.

	public static final double DEF_GS_REL_BASE_TIME = 0.01;

	// Default group span width ratio.

	public static final double DEF_GS_RATIO = 0.02;

	// Default group span minimum width, in days.

	public static final double DEF_GS_MIN_WIDTH = 0.01;

	// Default group rupture width high magnitude delta.

	public static final double DEF_GR_HI_MAG_DELTA = 0.5;

	// Default group rupture magnitude delta during which ratio tapers between lo and hi ratios.

	public static final double DEF_GR_TAPER_MAG_DELTA = 0.2;

	// Default group rupture width initial magnitude.

	public static final double DEF_GR_INIT_MAG = 4.5;

	// Default group rupture width low ratio.

	public static final double DEF_GR_LO_RATIO = 0.0;

	// Default group rupture width high ratio.

	public static final double DEF_GR_HI_RATIO = 0.98;




	//----- Incompleteness function construction -----




	// Default incompleteness discretization delta between discrete values.  See OEMagCompFnDiscFGH.make_disc_even().

	public static final double DEF_DISC_DELTA = 0.2;

	// Default maximum number of ruptures that can have magnitudes >= magCat, or 0 if no limit.

	public static final int DEF_MAG_CAT_COUNT = 3000;

	// Default maximum number of eligible (to generate incompleteness) ruptures, or 0 if no limit.

	public static final int DEF_ELIGIBLE_COUNT = 5;

	// Default interval duration limit ratio, must be >= 0.

	public static final double DEF_DURLIM_RATIO = 0.5;

	// Default interval duration limit minimum, in days, must be > 0.

	public static final double DEF_DURLIM_MIN = 0.00001;

	// Default interval duration limit maximum, in days, must be >= durlim_min.

	public static final double DEF_DURLIM_MAX= 3.0;

	// Default maximum number of ruptures allowed before the first required split, or 0 or -1 if no limit.
	// If non-zero, them DEF_MAG_CAT_COUNT applies only to ruptures after the first required split.
	// If zero, then DEF_MAG_CAT_COUNT applies to all ruptures.

	public static final int DEF_BEFORE_MAX_COUNT = 50;

	// Default option to join intervals whose magnitude of completeness is magCat: 0 = no join, 1 = join.

	public static final int DEF_MAG_CAT_INT_JOIN = 1;




	//----- Helmstetter incompleteness -----




	// Value of Helmstetter G that disables incompleteness.

	public static final double HELM_CAPG_DISABLE = 100.0;

	// Use capG > HELM_CAPG_DISABLE_CHECK to check for incompleteness disabled.

	public static final double HELM_CAPG_DISABLE_CHECK = 99.999;



	// Options for common Helmstetter parameters.

	public static final int HELM_PARAM_MIN   = 1;
	public static final int HELM_PARAM_NONE  = 1;	// No Helmstetter incompleteness
	public static final int HELM_PARAM_WORLD = 2;	// World values: F = 0.50, G = 0.25, H = 1.00
	public static final int HELM_PARAM_CAL   = 3;	// California values: F = 1.00, G = 4.50, H = 0.75
	public static final int HELM_PARAM_MAX   = 3;

	// Functions to obtain Helmstetter parameters.

	public static double helm_capF (int helm_param) {
		double capF;
		switch (helm_param) {
		default:
			throw new IllegalArgumentException ("OEConstants.helm_capF: Invalid Helmstetter option: helm_param = " + helm_param);
		case HELM_PARAM_NONE:
			capF = 0.50;
			break;
		case HELM_PARAM_WORLD:
			capF = 0.50;
			break;
		case HELM_PARAM_CAL:
			capF = 1.00;
			break;
		}
		return capF;
	}

	public static double helm_capG (int helm_param) {
		double capG;
		switch (helm_param) {
		default:
			throw new IllegalArgumentException ("OEConstants.helm_capG: Invalid Helmstetter option: helm_param = " + helm_param);
		case HELM_PARAM_NONE:
			capG = 100.0;
			break;
		case HELM_PARAM_WORLD:
			capG = 0.25;
			break;
		case HELM_PARAM_CAL:
			capG = 4.50;
			break;
		}
		return capG;
	}

	public static double helm_capH (int helm_param) {
		double capH;
		switch (helm_param) {
		default:
			throw new IllegalArgumentException ("OEConstants.helm_capH: Invalid Helmstetter option: helm_param = " + helm_param);
		case HELM_PARAM_NONE:
			capH = 1.00;
			break;
		case HELM_PARAM_WORLD:
			capH = 1.00;
			break;
		case HELM_PARAM_CAL:
			capH = 0.75;
			break;
		}
		return capH;
	}




	//----- Model parameters to appear in the product -----




	// Model parameter keys.

	public static final String MKEY_MAG_MAIN = "magMain";
	public static final String MKEY_SIM_COUNT = "simCount";
	public static final String MKEY_SIM_DURATION = "simDuration";
	public static final String MKEY_SIM_MAG_MIN = "simMagMin";
	public static final String MKEY_SIM_MAG_MAX = "simMagMax";




	//----- Parameter fitting -----




	// Negligably small time interval, in days, when fitting parameters.

	public static final double FIT_TIME_EPS = 1.0e-7;

	// Negligably small magnitude interval, when fitting parameters.

	public static final double FIT_MAG_EPS = 0.001;



	// Options to select the magnitude range for rupture likelihood, and for
	// interval productivity and likelihood, when fitting parameters.

	public static final int LMR_OPT_MIN = 1;				// Minimum value.
	public static final int LMR_OPT_MCT_INFINITY = 1;		// From time-dependent magnitude of completeness to infinity.
	public static final int LMR_OPT_MCT_MAG_MAX = 2;		// From time-dependent magnitude of completeness to maximum simulation magnitude.
	public static final int LMR_OPT_MAGCAT_INFINITY = 3;	// From catalog magnitude of completeness to infinity.
	public static final int LMR_OPT_MAGCAT_MAG_MAX = 4;		// From catalog magnitude of completeness to maximum simulation magnitude.
	public static final int LMR_OPT_MAX = 4;				// Maximum value.



	// Default value of the option to select magnitude range for log-likelihood calculation (LMR_OPT_XXXX).

	public static final int DEF_LMR_OPT = 1;		// LMR_OPT_MCT_INFINITY

	// Default value of the option to use intervals to fill in below magnitude of completness.

	public static final boolean DEF_F_INTERVALS = true;



	// Default value of mref, reference and minimum magnitude, for parameter definition.

	public static final double DEF_MREF = 3.0;

	// Default value of msup, maximum magnitude, for parameter definition.

	public static final double DEF_MSUP = 9.5;

	// Reference magnitude "mref" used to define the mainshock productivity parameter "zams".

	public static final double ZAMS_MREF = 0.0;

	// Reference magnitude "mref" used to define the background rate parameter "zmu".

	public static final double ZMU_MREF = 3.0;



	// Default minimum time interval to allow for parameter fitting, for branch ratio calculation.

	public static final double DEF_TINT_BR_FITTING = 365.0;

	// Default additional time interval to allow for forecast simulation, for branch ratio calculation.

	public static final double DEF_TINT_BR_FORECAST = 0.0;



	// Default minimum magnitude range above magnitude of completeness, for parameter fitting. [Old version 1]

	public static final double DEF_FMAG_ABOVE_MAG_CAT_OV1 = 4.0;

	// Default minimum magnitude range above maximum magnitude in catalog, for parameter fitting. [Old version 1]

	public static final double DEF_FMAG_ABOVE_MAG_MAX_OV1 = 0.5;



	// Default minimum magnitude range above magnitude of completeness, for parameter fitting.

	public static final double DEF_FMAG_ABOVE_MAG_CAT = 2.0;

	// Default minimum magnitude range above maximum magnitude in catalog, for parameter fitting.

	public static final double DEF_FMAG_ABOVE_MAG_MAX = 0.2;




	//----- Bayesian weighting -----




	// Bayesian prior weight for the Bayesian model.

	public static final double BAY_WT_BAYESIAN = 1.0;

	// Bayesian prior weight for the sequence specific model.

	public static final double BAY_WT_SEQ_SPEC = 0.0;

	// Bayesian prior weight for the generic model.

	public static final double BAY_WT_GENERIC = 2.0;

	// Bayesian prior weight minimum and maximum values to allow for invariant check.

	public static final double BAY_WT_MIN = -0.000001;
	public static final double BAY_WT_MAX =  2.000001;

	// Default time in which to apply early Bayesian prior weight, in days.

	public static final double DEF_EARLY_BAY_TIME = 0.125;




	// The default Bayesian prior factory. [Old version 1]

	public static OEBayFactory def_bay_factory_ov1 () {
		//return OEBayFactory.makeUniform();
		return OEBayFactory.makeGaussAPC();
	}



	// The default Bayesian prior factory.

	public static OEBayFactory def_bay_factory () {
		return OEBayFactory.makeMixedRNPC();
	}




	//----- b-values -----




	// b-value used to indicate that the b-value is unknown.

	public static final double UNKNOWN_B_VALUE = -1.0;

	// Use b < UNKNOWN_B_VALUE_CHECK to test if b contains UNKNOWN_B_VALUE.

	public static final double UNKNOWN_B_VALUE_CHECK = 0.0;




	//----- Parameter grid, likelihood truncation, and dithering -----




	// Maximum number of excitation parameter combinations in a grid.
	// (Typically, the number of combinations of zams and zmu.)

	public static final int MAX_EXCITATION_GRID = 500000;

	// Maximum number of statistical parameter combinations in a grid.
	// (Typically, the number of combinations of c, p, and n.)

	//public static final int MAX_STATISTICS_GRID = 500000;
	public static final int MAX_STATISTICS_GRID = 10000000;	// increased value to support testing

	// Default size of the bins used to clip the normalized log density function, in matural log units

	public static final double DEF_DENSITY_BIN_SIZE_LNU = 0.01;	// Each bin is ~ 1%

	// Default number of bins used to clip the normalized log density function.
	// The normalized log-density function is clipped by default at
	// -DEF_DENSITY_BIN_SIZE_LNU * (DEF_DENSITY_BIN_COUNT - 1) natural log units.

//	public static final int DEF_DENSITY_BIN_COUNT = 622;	// clip at density ~ 0.002 of maximum
	public static final int DEF_DENSITY_BIN_COUNT = 692;	// clip at density ~ 0.001 of maximum

	// Default fraction of parameter set probability space to clip.

	public static final double DEF_PROB_TAIL_TRIM = 0.003;	// keep grid filling 99.7% of probability space

	// Number of pre-selected parameter sets for seeding catalogs, must be a power of 2.

	public static final int DEF_SEED_SUBVOX_COUNT = 262144;	// 2^18

	// Maximum branch ratio passed to simulation from dithered voxel set.
	// Note: This is primarily to allow the fitting code to be tested with branch ratio greater then 1.0,
	// but also can be used as upper limit for branch ratio parameter in GUI.

	public static final double MAX_DITHERING_BR_FOR_SIM = 0.99;

	// Branch ratio above which voxels should be excluded from dithered voxel set.
	// Note: This is primarily to allow the fitting code to be tested with branch ratio greater then 1.0.

	public static final double EXCLUDE_DITHERING_BR_FOR_SIM = 0.99001;

	// Maximum branch ratio considered sub-critical when computing marginals.
	// Note: This is primarily to allow the fitting code to be tested with branch ratio greater then 1.0.

	public static final double MAX_MARGINAL_BR_SUB_CRITICAL = 0.99001;




	// The default range of Gutenberg-Richter parameter, b-value. [Old version 1]

	public static OEDiscreteRange def_b_range_ov1 () {
		return OEDiscreteRange.makeSingle (1.0);
	}

	// The default range of ETAS intensity parameter, alpha-value. [Old version 1]
	// Can be null to force alpha == b.

	public static OEDiscreteRange def_alpha_range_ov1 () {
		return null;
	}

	// The default range of Omori c-value. [Old version 1]

	public static OEDiscreteRange def_c_range_ov1 () {
		return OEDiscreteRange.makeLog (21, 0.00001, 1.00000);
	}

	// The default range of Omori p-value. [Old version 1]

	public static OEDiscreteRange def_p_range_ov1 () {
		return OEDiscreteRange.makeLinear (37, 0.50, 2.00);
	}

	// The default range of branch ratio, n-value. [Old version 1]
	// This controls the productivity of secondary triggering.

	public static OEDiscreteRange def_n_range_ov1 () {
		//return OEDiscreteRange.makeLog (41, 0.01, 0.90);
		return OEDiscreteRange.makeLogSkew (41, 0.02, 0.90, 3.0);
	}

	// The default range of mainshock productivity, ams-value, for reference magnitude equal to ZAMS_MREF == 0.0. [Old version 1]

	public static OEDiscreteRange def_zams_range_ov1 () {
		return OEDiscreteRange.makeLinear (81, -4.50, -0.50);
	}

	// The default range of background rate, mu-value, for reference magnitude equal to ZMU_MREF. [Old version 1]
	// Can be null to force zmu = 0.0.

	public static OEDiscreteRange def_zmu_range_ov1 () {
		//return null;
		return OEDiscreteRange.makeSingle (0.0);
	}

	// True if the value of zams is interpreted relative to the a-value. [Old version 1]

	public static boolean def_relative_zams_ov1 () {
		return false;
	}




	// The default range of Gutenberg-Richter parameter, b-value.

	public static OEDiscreteRange def_b_range () {
		return OEDiscreteRange.makeSingle (1.0);
	}

	// The default range of ETAS intensity parameter, alpha-value.
	// Can be null to force alpha == b.

	public static OEDiscreteRange def_alpha_range () {
		return null;
	}

	// The default range of Omori c-value.

	public static OEDiscreteRange def_c_range () {
		return OEDiscreteRange.makeLog (21, 0.00001, 1.00000);
	}

	// The default range of Omori p-value.

	public static OEDiscreteRange def_p_range () {
		return OEDiscreteRange.makeLinear (37, 0.50, 2.00);
	}

	// The default range of branch ratio, n-value.
	// This controls the productivity of secondary triggering.

	public static OEDiscreteRange def_n_range () {
		//return OEDiscreteRange.makeLog (81, 0.025, 0.90);
		return OEDiscreteRange.makeLogSkew (81, 0.025, 0.90, 3.0);
	}

	// The default range of mainshock productivity, ams-value, for reference magnitude equal to ZAMS_MREF == 0.0.

	public static OEDiscreteRange def_zams_range () {
		return OEDiscreteRange.makeLinear (43, -2.0, 1.0);
	}

	// The default range of background rate, mu-value, for reference magnitude equal to ZMU_MREF.
	// Can be null to force zmu = 0.0.

	public static OEDiscreteRange def_zmu_range () {
		//return null;
		return OEDiscreteRange.makeSingle (0.0);
	}

	// True if the value of zams is interpreted relative to the a-value.

	public static boolean def_relative_zams () {
		return true;
	}




	//----- Ensemble simulation -----




	// Default number of catalogs.

	public static final int DEF_NUM_CATALOGS = 500000;

	// Default minimum acceptable number of catalogs.

	public static final int DEF_MIN_NUM_CATALOGS = 250000;

	// Required minimum number of catalogs.

	public static final int REQ_NUM_CATALOGS = 100;



	// Default target number of direct aftershocks of the seeds, for per-catalog min mag ranging, lower limit.

	public static final int DEF_RAN_DIRECT_SIZE_LO = 100;

	// Default target number of direct aftershocks of the seeds, for per-catalog min mag ranging, upper limit; or 0 if not used.

	public static final int DEF_RAN_DIRECT_SIZE_HI = 1000;

	// Default magnitude excess to use during simulations, or 0.0 to disable.
	// A positive value causes catalogs to be discarded if they produce an earthquake larger than max mag.

	public static final double DEF_RAN_MAG_EXCESS = 0.0;

	// Default generation count for branch ratio handling, for per-catalog max mag ranging.  Must be >= 2.
	// Note: 2 indicates to use the direct aftershocks of the seeds.

	public static final int DEF_RAN_GEN_BR = 20;

	// Default de-rating factor for branch ratio handling, for per-catalog max mag ranging.  Should be between 0 and 1 (and close to 1).

	public static final double DEF_RAN_DERATE_BR = 0.90;

	// Default allowable fraction of catalogs to exceed max mag, for per-catalog max mag ranging.  Must be > 0.0 (and close to 0).

	public static final double DEF_RAN_EXCEED_FRACTION = 0.02;



	// The default accumulator to use, for simulations.  (See OEConstants.SEL_ACCUM_XXXX.)

	public static int def_sim_accum_selection () {
		return OEConstants.SEL_ACCUM_RATE_TIME_MAG;
	}

	// Default accumulator options, for simulations.
	// for sim_accum_selection == SEL_ACCUM_RATE_TIME_MAG, it is the extrapolation options.

	public static int def_sim_accum_option () {
		return OEConstants.make_rate_acc_meth (
					OEConstants.CATLEN_METH_ENTIRE,
					OEConstants.OUTFILL_METH_PDF_DIRECT,
					OEConstants.MAGFILL_METH_PDF_HYBRID);	// 433
	}

	// Defalt accumulator additional parameter 1, for simulations.
	// For sim_accum_selection == SEL_ACCUM_RATE_TIME_MAG, it is the proportional reduction (0.0 to 1.0) to apply to secondary productivity when computing upfill.

	public static double def_sim_accum_param_1 () {
		return 0.25;
	}




	//----- Eligibility options -----




	// Default mainshock magnitude for ETAS eligibility.

	public static final double DEF_ELIGIBLE_MAIN_MAG = 4.45;

	// Default catalog maximum magnitude for ETAS eligibility.

	public static final double DEF_ELIGIBLE_CAT_MAX_MAG = 3.95;

	// Default catalog maximum magnitude delta for ETAS eligibility.

	//public static final double DEF_ELIGIBLE_CAT_MAX_DELTA = 0.50;

	// Default mainshock magnitude below which earthquake is considered small.
	// Can use OEConstants.NO_MAG_NEG (or zero) if none.

	public static final double DEF_ELIGIBLE_SMALL_MAG = 4.95;

	// Default amount mainshock magnitude must exceed magnitude of completeness.
	// Can use OEConstants.NO_MAG_NEG (in practice zero would work) if none.

	public static final double DEF_ELIGIBLE_ABOVE_MAG_CAT = 2.00;


	// Eligibility option codes.

	public static final int ELIGIBLE_OPT_MIN = 0;
	public static final int ELIGIBLE_OPT_DISABLE = 0;	// disable ETAS unconditinally for the earthquake
	public static final int ELIGIBLE_OPT_ENABLE = 1;	// enable ETAS unconditinall for the earthquake
	public static final int ELIGIBLE_OPT_AUTO = 2;		// use automatic eligibility criterion
	public static final int ELIGIBLE_OPT_MAX = 2;




	//----- Testing support -----




	// Default origin time/date for simulated sequences.

	public static final String DEF_SIM_ORIGIN_TIME = "2000-01-01T00:00:00Z";

	// Get the default origin time for simulated sequences, in milliseconds since the epoch.

	public static long get_def_sim_origin_time_millis () {
		return SimpleUtils.string_to_time (DEF_SIM_ORIGIN_TIME);
	}

	// Default resolution for integrated intensity as a fraction of total time range, or 0 for no requirement.

	public static final double DEF_INTEGRATED_LAMBDA_RES = 0.005;

}
