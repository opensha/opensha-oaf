package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.OEConstants;

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




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {
		mag_main = 0.0;
		tint_br  = 0.0;
		relative_zams = false;
		return;
	}




	// Default constructor.

	public OEBayPriorParams () {
		clear();
	}




	// Constructor that sets the supplied values.

	public OEBayPriorParams (
		double mag_main,
		double tint_br,
		OEGridOptions grid_options
	) {
		this.mag_main = mag_main;
		this.tint_br  = tint_br;
		this.relative_zams = grid_options.get_relative_zams();
	}




	// Set the supplied values.
	// Returns this object.

	public final OEBayPriorParams set (
		double mag_main,
		double tint_br,
		OEGridOptions grid_options
	) {
		this.mag_main = mag_main;
		this.tint_br  = tint_br;
		this.relative_zams = grid_options.get_relative_zams();
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEBayPriorParams:" + "\n");

		result.append ("mag_main = " + mag_main + "\n");
		result.append ("tint_br = "  + tint_br  + "\n");
		result.append ("relative_zams = " + relative_zams + "\n");

		return result.toString();
	}




	// Return true if we have a mainshock magnitude.

	public final boolean has_mag_main () {
		return mag_main > OEConstants.NO_MAG_NEG_CHECK;
	}

}
