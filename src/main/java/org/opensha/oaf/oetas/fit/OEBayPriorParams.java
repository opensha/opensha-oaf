package org.opensha.oaf.oetas.fit;


// Class to hold parameters to pass when calling the Bayesian prior.
// Author: Michael Barall 02/28/2023.
//
// This object may be accessed simultaneously from multiple threads,
// therefore it must not be modified after it is initialized.

public class OEBayPriorParams {

	//----- Value -----


	// Mainshock magnitude (actually the largest magnitude in the sequence).

	private double mag_main;

	public double get_mag_main () {
		return mag_main;
	}


	// Time interval used for converting branch ratio into productivity..

	private double tint_br;

	public double get_tint_br () {
		return tint_br;
	}




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {
		mag_main = 0.0;
		tint_br  = 0.0;
		return;
	}




	// Default constructor.

	public OEBayPriorParams () {
		clear();
	}




	// Constructor that sets the supplied values.

	public OEBayPriorParams (
		double mag_main,
		double tint_br
	) {
		this.mag_main = mag_main;
		this.tint_br  = tint_br;
	}




	// Set  the supplied values.
	// Returns this object.

	public final OEBayPriorParams set (
		double mag_main,
		double tint_br
	) {
		this.mag_main = mag_main;
		this.tint_br  = tint_br;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEBayPriorParams:" + "\n");

		result.append ("mag_main = " + mag_main + "\n");
		result.append ("tint_br = "  + tint_br  + "\n");

		return result.toString();
	}

}
