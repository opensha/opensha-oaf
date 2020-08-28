package org.opensha.oaf.oetas;


// Holds constants used by Operational ETAS.
// Author: Michael Barall 11/27/2019.

public class OEConstants {

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

	// A time value or difference much larger than any actual value, in days.
	// (But still much smaller than the range of double.)

	public static final double HUGE_TIME_DAYS = 1.0e+20;

	// Use x >= HUGE_TIME_DAYS_CHECK to check if x contains HUGE_TIME_DAYS.

	public static final double HUGE_TIME_DAYS_CHECK = 0.99e+20;

	// The logarithm base 10 of HUGE_TIME_DAYS.

	public static final double LOG10_HUGE_TIME_DAYS = 20;

	// An Omori rate so extremely small it is considered to be zero.
	// Note: This is used to avoid divide-by-zero in formulas that have an
	// Omori rate in the denominator; it does not mean that rates larger
	// than this are considered significant.
	
	public static final double TINY_OMORI_RATE = 1.0e-150;

	// An expected count of earthquakes small enough to treat as zero.

	public static final double SMALL_EXPECTED_COUNT = 0.001;

	// Methods for infilling event counts below the minimum simulated magnitude.

	public static final int INFILL_METH_MIN		= 1;
	public static final int INFILL_METH_NONE	= 1;	// No infill, use exactly as simulated
	public static final int INFILL_METH_SCALE	= 2;	// Scale up, using G-R relation
	public static final int INFILL_METH_POISSON	= 3;	// Use Poisson random var applied to expected rate
	public static final int INFILL_METH_STERILE	= 4;	// Generate sterile ruptures
	public static final int INFILL_METH_MAX		= 4;

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

}
