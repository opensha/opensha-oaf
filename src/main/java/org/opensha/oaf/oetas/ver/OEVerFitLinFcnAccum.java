package org.opensha.oaf.oetas.ver;


// Linear function accumulator.
// Author: Michael Barall.
//
// An array of linear function accumulators. Each element is a linear function of
// the three variables (10^a)*Q, (10^ams)*Q, and mu, which are the aftershock
// productivity, mainshock/foreshock productivity, and background rate, and are
// typically represented by variables ten_a_q, ten_ams_q, and mu.
//
// Each element of the array is initialized to zero, and functions are provided to
// add to an element of the array, and to evaluate an element.

public class OEVerFitLinFcnAccum {

	//----- Array of accumulators -----

	// Number of accumulators in this array.

	private int n_accum;

	// Coefficients of the aftershock productivity.

	private double[] c_aftr;

	// Coefficients of the mainshock/foreshock productivity.

	private double[] c_main;

	// Coefficients of the background rate.

	private double[] c_bkgd;


	//----- Construction -----

	// Constructor allocates and zero-initializes the arrays.

	public OEVerFitLinFcnAccum (int n_accum) {
		this.n_accum = n_accum;
		c_aftr = new double[n_accum];
		c_main = new double[n_accum];
		c_bkgd = new double[n_accum];
		for (int it = 0; it < n_accum; ++it) {
			c_aftr[it] = 0.0;
			c_main[it] = 0.0;
			c_bkgd[it] = 0.0;
		}
	}


	//----- Coefficient accumulation -----


	// Add to the coefficient of the aftershock or mainshock productivity.
	// Parameters:
	//  it = Target index (index into this array).
	//  c = Coefficient to add.
	//  f_is_main = True to add to the mainshock coefficient, false to add to the aftershock coefficient.

	public final void add_aftr_or_main (int it, double c, boolean f_is_main) {
		if (f_is_main) {
			c_main[it] += c;
		} else {
			c_aftr[it] += c;
		}
		return;
	}


	// Add to the coefficient of the aftershock rate.
	// Parameters:
	//  it = Target index (index into this array).
	//  c = Coefficient to add.

	public final void add_aftr (int it, double c) {
		c_aftr[it] += c;
		return;
	}


	// Add to the coefficient of the mainshock/foreshock rate.
	// Parameters:
	//  it = Target index (index into this array).
	//  c = Coefficient to add.

	public final void add_main (int it, double c) {
		c_main[it] += c;
		return;
	}


	// Add to the coefficient of the background rate.
	// Parameters:
	//  it = Target index (index into this array).
	//  c = Coefficient to add.

	public final void add_bkgd (int it, double c) {
		c_bkgd[it] += c;
		return;
	}


	//----- Function accumulation -----


	// Add a scaled source function to the target function.
	// Parameters:
	//  it = Target index (index into this array).
	//  src = Source accumulator array.
	//  is = Source index (index into the src array).
	//  ss = Scale factor to apply to the source function.

	public final void add_src (int it, OEVerFitLinFcnAccum src, int is, double ss) {
		this.c_aftr[it] += (src.c_aftr[is] * ss);
		this.c_main[it] += (src.c_main[is] * ss);
		this.c_bkgd[it] += (src.c_bkgd[is] * ss);
	}


	// Apply a scale factor to the target function.
	// Parameters:
	//  it = Target index (index into this array).
	//  st = Scale factor to apply to the target function.

	public final void apply_scale (int it, double st) {
		this.c_aftr[it] *= st;
		this.c_main[it] *= st;
		this.c_bkgd[it] *= st;
	}


	//----- Evaluation -----


	// Evaluate a function for a given set of productivities.
	// Parameters:
	//  it = Target index (index into this array).
	//  ten_a_q = Aftershock productivity.
	//  ten_ams_q = Mainshock/foreshock productivity.
	//  mu = Background rate.

	public final double eval_fcn (int it, double ten_a_q, double ten_ams_q, double mu) {
		return (c_aftr[it] * ten_a_q) + (c_main[it] * ten_ams_q) + (c_bkgd[it] * mu);
	}


}
