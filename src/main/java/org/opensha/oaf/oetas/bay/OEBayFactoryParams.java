package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.OEConstants;

import org.opensha.commons.geo.Location;


// Class to hold parameters to pass when calling the Bayesian prior factory function.
// Author: Michael Barall 08/26/2024.
//
// This object may be accessed simultaneously from multiple threads,
// therefore it must not be modified after it is initialized.
// (In practice it is only accessed from a single thread.)

public class OEBayFactoryParams {

	//----- Value -----


	// Mainshock magnitude (or another magnitude representing the sequence).
	// Can be OEConstants.NO_MAG_NEG if not specified or unknown.
	// Note: Unknown value must agree with the special value in OEtasCatalogInfo.

	private double mag_main;

	public final boolean has_mag_main () {
		if (mag_main <= OEConstants.NO_MAG_NEG_CHECK) {
			return false;
		}
		return true;
	}

	public final double get_mag_main () {
		return mag_main;
	}


	// Mainshock location (or another location representing the sequence).
	// Can be null if not specified or unknown.

	private Location loc_main;

	public final boolean has_loc_main () {
		if (loc_main == null) {
			return false;
		}
		return true;
	}

	public final Location get_loc_main () {
		return loc_main;
	}




	//----- Construction -----



	// Get the value for an unknown magnitude.

	public static double unknown_mag () {
		return OEConstants.NO_MAG_NEG;
	}


	// Get the value for an unknown location.

	public static Location unknown_loc () {
		return null;
	}




	// Clear to empty values.

	public final void clear () {
		mag_main = OEConstants.NO_MAG_NEG;
		loc_main  = null;
		return;
	}




	// Default constructor sets unknown magnitude and location.

	public OEBayFactoryParams () {
		clear();
	}




	// Constructor that sets the supplied values.

	public OEBayFactoryParams (
		double mag_main,
		Location loc_main
	) {
		this.mag_main = mag_main;
		this.loc_main = loc_main;
	}




	// Set the supplied values.
	// Returns this object.

	public final OEBayFactoryParams set (
		double mag_main,
		Location loc_main
	) {
		this.mag_main = mag_main;
		this.loc_main = loc_main;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEBayFactoryParams:" + "\n");

		if (has_mag_main()) {
			result.append ("mag_main = " + mag_main + "\n");
		} else {
			result.append ("mag_main = " + "<none>" + "\n");
		}

		if (has_loc_main()) {
			result.append ("loc_main.lat = " + loc_main.getLatitude() + "\n");
			result.append ("loc_main.lon = " + loc_main.getLongitude() + "\n");
			result.append ("loc_main.depth = " + loc_main.getDepth() + "\n");
		} else {
			result.append ("loc_main = " + "<null>" + "\n");
		}

		return result.toString();
	}

}
