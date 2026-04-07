package org.opensha.oaf.oetas.ver;

import org.opensha.oaf.oetas.fit.OEDisc2InitFitInfo;


// Class to store fitting options for fitting verification.
// Author: Michael Barall.
//
// This is an immutable object that holds options for parameter fitting.

public class OEVerFitOptions {

	//----- Magnitude ranges -----

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	private double mref;

	public final double get_mref () {
		return mref;
	}

	// Maximum considered magnitude, for parameter definition.

	private double msup;

	public final double get_msup () {
		return msup;
	}

	// The minimum magnitude to use for the simulation.

	private double mag_min;

	public final double get_mag_min () {
		return mag_min;
	}

	// The maximum magnitude to use for the simulation.

	private double mag_max;

	public final double get_mag_max () {
		return mag_max;
	}


	//----- Configuration options -----

	// True to use intervals to fill in below magnitude of completeness.

	private boolean f_intervals;

	public final boolean get_f_intervals () {
		return f_intervals;
	}

	// Coefficient for intervals to act as productivity sources for later intervals.
	// Ignored unless f_intervals is true.

	private double c_cross_intervals;

	public final double get_c_cross_intervals () {
		return c_cross_intervals;
	}

	// Coefficient for intervals to act as productivity sources for themselves.
	// Ignored unless f_intervals is true.

	private double c_self_intervals;

	public final double get_c_self_intervals () {
		return c_self_intervals;
	}

	// Likelihood magnitude range option (LMR_OPT_XXXX).

	private int lmr_opt;

	public final int get_lmr_opt () {
		return lmr_opt;
	}

	// Time interval for interpreting branch ratios, in days.
	// Used for branch ratio conversion.

	private double tint_br;

	public final double get_tint_br () {
		return tint_br;
	}




	//----- Construction -----




	// Constructor that sets all values.

	public OEVerFitOptions (
		double mref,
		double msup,
		double mag_min,
		double mag_max,
		boolean f_intervals,
		double c_cross_intervals,
		double c_self_intervals,
		int lmr_opt,
		double tint_br
	) {
		this.mref               = mref;
		this.msup               = msup;
		this.mag_min            = mag_min;
		this.mag_max            = mag_max;
		this.f_intervals        = f_intervals;
		this.c_cross_intervals  = c_cross_intervals;
		this.c_self_intervals   = c_self_intervals;
		this.lmr_opt            = lmr_opt;
		this.tint_br            = tint_br;
	}




	// Constructor sets values from fitting info.

	public OEVerFitOptions (
		OEDisc2InitFitInfo fit_info
	) {
		this.mref               = fit_info.mref;
		this.msup               = fit_info.msup;
		this.mag_min            = fit_info.mag_min;
		this.mag_max            = fit_info.mag_max;
		this.f_intervals        = fit_info.f_intervals;
		this.c_cross_intervals  = fit_info.c_cross_intervals;
		this.c_self_intervals   = fit_info.c_self_intervals;
		this.lmr_opt            = fit_info.lmr_opt;
		this.tint_br            = fit_info.tint_br;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEVerFitOptions:" + "\n");

		result.append ("mref = "              + mref              + "\n");
		result.append ("msup = "              + msup              + "\n");
		result.append ("mag_min = "           + mag_min           + "\n");
		result.append ("mag_max = "           + mag_max           + "\n");

		result.append ("f_intervals = "       + f_intervals       + "\n");
		result.append ("c_cross_intervals = " + c_cross_intervals + "\n");
		result.append ("c_self_intervals = "  + c_self_intervals  + "\n");
		result.append ("lmr_opt = "           + lmr_opt           + "\n");
		result.append ("tint_br = "           + tint_br           + "\n");

		return result.toString();
	}

}
