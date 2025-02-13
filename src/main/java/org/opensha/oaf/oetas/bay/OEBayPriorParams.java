package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEStatsCalc;

import org.opensha.oaf.oetas.fit.OEGridOptions;


// Class to hold parameters to pass when calling the Bayesian prior.
// Author: Michael Barall 02/28/2023.
//
// This object may be accessed simultaneously from multiple threads,
// therefore it must not be modified after it is initialized.

public class OEBayPriorParams {

	//----- Value -----


	// Mainshock magnitude, the largest magnitude among ruptures considered mainshocks, or NO_MAG_NEG if none.

	private double mag_main;

	public final double get_mag_main () {
		return mag_main;
	}


	// Time interval used for converting branch ratio into productivity, in days.

	private double tint_br;

	public final double get_tint_br () {
		return tint_br;
	}


	// True if the value of zams is interpreted relative to the a-value.

	private boolean relative_zams;

	public final boolean get_relative_zams () {
		return relative_zams;
	}


	// The minimum magnitude to use for the simulation.
	// This is included to support absolute/relative conversion for ams.

	private double mag_min;

	public final double get_mag_min () {
		return mag_min;
	}


	// The maximum magnitude to use for the simulation.
	// This is included to support absolute/relative conversion for ams.

	private double mag_max;

	public final double get_mag_max () {
		return mag_max;
	}




	//----- Construction -----




//	// Clear to empty values.
//
//	public final void clear () {
//		mag_main = 0.0;
//		tint_br  = 0.0;
//		relative_zams = false;
//		mag_min  = 0.0;
//		mag_max  = 0.0;
//		return;
//	}




//	// Default constructor.
//
//	public OEBayPriorParams () {
//		clear();
//	}




	// Constructor that sets the supplied values. [DEPRECATED]

	public OEBayPriorParams (
		double mag_main,
		double tint_br,
		OEGridOptions grid_options
	) {
		this.mag_main = mag_main;
		this.tint_br  = tint_br;
		this.relative_zams = grid_options.get_relative_zams();
		this.mag_min  = OEConstants.DEF_MREF;
		this.mag_max  = OEConstants.DEF_MSUP;
	}




//	// Set the supplied values.
//	// Returns this object.
//
//	public final OEBayPriorParams set (
//		double mag_main,
//		double tint_br,
//		OEGridOptions grid_options
//	) {
//		this.mag_main = mag_main;
//		this.tint_br  = tint_br;
//		this.relative_zams = grid_options.get_relative_zams();
//		this.mag_min  = OEConstants.DEF_MREF;
//		this.mag_max  = OEConstants.DEF_MSUP;
//		return this;
//	}




	// Constructor that sets the supplied values.

	public OEBayPriorParams (
		double mag_main,
		double tint_br,
		OEGridOptions grid_options,
		double mag_min,
		double mag_max
	) {
		this.mag_main = mag_main;
		this.tint_br  = tint_br;
		this.relative_zams = grid_options.get_relative_zams();
		this.mag_min  = mag_min;
		this.mag_max  = mag_max;
	}




//	// Set the supplied values.
//	// Returns this object.
//
//	public final OEBayPriorParams set (
//		double mag_main,
//		double tint_br,
//		OEGridOptions grid_options,
//		double mag_min,
//		double mag_max
//	) {
//		this.mag_main = mag_main;
//		this.tint_br  = tint_br;
//		this.relative_zams = grid_options.get_relative_zams();
//		this.mag_min  = mag_min;
//		this.mag_max  = mag_max;
//		return this;
//	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEBayPriorParams:" + "\n");

		result.append ("mag_main = " + mag_main + "\n");
		result.append ("tint_br = "  + tint_br  + "\n");
		result.append ("relative_zams = " + relative_zams + "\n");
		result.append ("mag_min = "  + mag_min  + "\n");
		result.append ("mag_max = "  + mag_max  + "\n");

		return result.toString();
	}




	// Return true if we have a mainshock magnitude.

	public final boolean has_mag_main () {
		return mag_main > OEConstants.NO_MAG_NEG_CHECK;
	}




	//----- Operations -----




	// Calculate the offset for converting relative zams to absolute zams.
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	// This function calculates the offset to add to zams in order to obtain an absolute zams.
	// Note: If zams is not relative, this function returns zero.
	// Note: If zams is relative, this function returns a zams value such that calc_ten_ams_q_from_zams
	// returns the same value as calc_ten_a_q_from_branch_ratio.  In other words, a zams Value
	// such that (10^ams)*Q equals (10^a)*Q (noting that Q = 1.0 for the ams conversion).
	// Note: Another formulation is that a mainshock has the same productivity as an aftershock
	// of the same magnitude.
	// Note: Must match code in OEDisc2InitFitInfo.

	public final double calc_rel_to_abs_zams_offset (
		double n,
		double p,
		double c,
		double b,
		double alpha
	) {
		// If zams is relative ...

		if (relative_zams) {

			//  // Calculate (10^ams)*Q, assuming it is equal to (10^a)*Q
			//  
			//  double ten_ams_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
			//  	n,
			//  	p,
			//  	c,
			//  	b,
			//  	alpha,
			//  	mref,
			//  	mag_min,
			//  	mag_max,
			//  	tint_br
			//  );
			//  
			//  // Assuming Q = 1.0, calculate ams
			//  
			//  double ams = Math.log10 (ten_ams_q);
			//  
			//  // Convert from the reference magnitude for this object to the reference magnitude for zams
			//  
			//  return OEStatsCalc.calc_a_new_from_mref_new (
			//  	ams,					// a_old
			//  	b,						// b
			//  	alpha,					// alpha
			//  	mref,					// mref_old
			//  	OEConstants.ZAMS_MREF	// mref_new
			//  );

			// Convert from branch ratio to zams, preserving productivity

			return OEStatsCalc.calc_zams_from_br (
				n,
				p,
				c,
				b,
				alpha,
				mag_min,
				mag_max,
				tint_br
			);
		}

		// If zams is absolute, return zero

		return 0.0;
	}




	// Calculate the offset for converting absolute zams to relative zams.
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	// This function calculates the offset to add to zams in order to obtain a relative zams.
	// Note: If zams is relative, this function returns zero.
	// Note: If zams is not relative, this function returns the negative of a zams value such that calc_ten_ams_q_from_zams
	// returns the same value as calc_ten_a_q_from_branch_ratio.  In other words, a zams Value
	// such that (10^ams)*Q equals (10^a)*Q (noting that Q = 1.0 for the ams conversion).
	// Note: Another formulation is that a mainshock has the same productivity as an aftershock
	// of the same magnitude.
	// Note: Must match code in OEDisc2InitFitInfo.

	public final double calc_abs_to_rel_zams_offset (
		double n,
		double p,
		double c,
		double b,
		double alpha
	) {
		// If zams is not relative ...

		if (!( relative_zams )) {

			//  // Calculate (10^ams)*Q, assuming it is equal to (10^a)*Q
			//  
			//  double ten_ams_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
			//  	n,
			//  	p,
			//  	c,
			//  	b,
			//  	alpha,
			//  	mref,
			//  	mag_min,
			//  	mag_max,
			//  	tint_br
			//  );
			//  
			//  // Assuming Q = 1.0, calculate ams
			//  
			//  double ams = Math.log10 (ten_ams_q);
			//  
			//  // Convert from the reference magnitude for this object to the reference magnitude for zams
			//  
			//  return -OEStatsCalc.calc_a_new_from_mref_new (
			//  	ams,					// a_old
			//  	b,						// b
			//  	alpha,					// alpha
			//  	mref,					// mref_old
			//  	OEConstants.ZAMS_MREF	// mref_new
			//  );

			// Convert from branch ratio to zams, preserving productivity

			return -OEStatsCalc.calc_zams_from_br (
				n,
				p,
				c,
				b,
				alpha,
				mag_min,
				mag_max,
				tint_br
			);
		}

		// If zams is relative, return zero

		return 0.0;
	}




}
