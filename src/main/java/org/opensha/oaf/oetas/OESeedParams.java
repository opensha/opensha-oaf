package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to store parameters for seeding an Operational ETAS catalog.
// Author: Michael Barall 12/21/2022.
//
// Holds the parameters for setting up the seed generation of an ETAS catalog,
// from a list of rupture times and magnitudes.
//
// The parameter definition is as follows.  For an earthquake of magnitude m0
// occuring at time t0, the rate of direct aftershocks, per unit time, per unit
// magnitude, is
//
//   lambda(t, m) = k * b * log(10) * (10^(-b*(m - mref))) * ((t-t0+c)^(-p))
//
//   k = 10^(ams + alpha*(m0 - mref))
//
//   ams = Mainshock productivity parameter
//   p = Omori exponent parameter
//   c = Omori offset parameter
//   b = Gutenberg-Richter parameter
//   alpha = ETAS intensity parameter
//   mref = Reference magnitude = minimum considered magnitude.
//   msup = Maximum considered magnitude.
//
// Notice that the reference magnitude mref is an intrinsic part of the
// definition of the productivity "ams" parameter.  If the reference magnitude is
// changed, then the value of "ams" (or equivalently "k") would also change.
// (However, if b == alpha then there is no change in the value of "ams".)
//
// For a background rate mu, the time-independent rate of background earthquakes,
// per unit time, per unit magnitude, is
//
//   lambda(m) = mu * b * log(10) * (10^(-b*(m - mref)))
//
// Notice that the reference magnitude mref is an intrinsic part of the
// definition of the background rate "mu" parameter.  If the reference magnitude
// is changed, then the value of "mu" would also change.

public class OESeedParams {

	//----- Parameters -----

	// Mainshock productivity parameter.

	public double ams;

	// Background rate parameter.

	public double mu;

	// Minimum and maximum magnitudes to report for the seed generation (see OEGenerationInfo).
	// Note it is not required for the magnitudes of the earthquakes in the seed
	// generation to lie within this range.
	// Often these are set equal to mref and msup.

	public double seed_mag_min;
	public double seed_mag_max;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		ams             = 0.0;
		mu				= 0.0;
		seed_mag_min    = 0.0;
		seed_mag_max    = 0.0;
		return;
	}




	// Default constructor.

	public OESeedParams () {
		clear();
	}




	// Set all values.
	// Returns this object.

	public final OESeedParams set (
		double ams,
		double mu,
		double seed_mag_min,
		double seed_mag_max
	) {
		this.ams             = ams;
		this.mu              = mu;
		this.seed_mag_min    = seed_mag_min;
		this.seed_mag_max    = seed_mag_max;
		return this;
	}




	// Copy all values from the other object.
	// Returns this object.

	public final OESeedParams copy_from (OESeedParams other) {
		this.ams             = other.ams;
		this.mu              = other.mu;
		this.seed_mag_min    = other.seed_mag_min;
		this.seed_mag_max    = other.seed_mag_max;
		return this;
	}




	// Set all values, given zero-mref ams-value plus catalog parameters.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  cat_params = Catalog parameters:
	//   cat_params.b = G-R b-value.
	//   cat_params.alpha = ETAS alpha-value.
	//   cat_params.mref = Reference magnitude = Minimum magnitude.
	//   cat_params.msup = Maximum magnitude.
	// Returns this object.
	// Note: This function sets the background rate to zero.

	public final OESeedParams set_from_zams (double zams, OECatalogParams cat_params) {
		this.ams             = cat_params.calc_ams_from_zams (zams);
		this.mu              = 0.0;
		this.seed_mag_min    = cat_params.mref;
		this.seed_mag_max    = cat_params.msup;
		return this;
	}




	// Set all values, given zero-mref ams-value, ZMU_MREF mu-value, plus catalog parameters.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  zmu = Background rate parameter, assuming reference magnitude equal to ZMU_MREF.
	//  cat_params = Catalog parameters:
	//   cat_params.b = G-R b-value.
	//   cat_params.alpha = ETAS alpha-value.
	//   cat_params.mref = Reference magnitude = Minimum magnitude.
	//   cat_params.msup = Maximum magnitude.
	// Returns this object.

	public final OESeedParams set_from_zams_zmu (double zams, double zmu, OECatalogParams cat_params) {
		this.ams             = cat_params.calc_ams_from_zams (zams);
		this.mu              = cat_params.calc_mu_from_zmu (zmu);
		this.seed_mag_min    = cat_params.mref;
		this.seed_mag_max    = cat_params.msup;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OESeedParams:" + "\n");

		result.append ("ams = "             + ams             + "\n");
		result.append ("mu = "              + mu              + "\n");
		result.append ("seed_mag_min = "    + seed_mag_min    + "\n");
		result.append ("seed_mag_max = "    + seed_mag_max    + "\n");

		return result.toString();
	}




	// Return true if we have a positive background rate.

	public boolean has_background_rate () {
		return mu > OEConstants.TINY_BACKGROUND_RATE;
	}




	// Return the statistics parameters.
	// The returned object is newly allocated.

	public final OESeedParamsStats get_params_stats () {
		return new OESeedParamsStats (
			ams,
			mu
		);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 106001;

	private static final String M_VERSION_NAME = "OESeedParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("ams"            , ams            );
			writer.marshalDouble ("mu"             , mu             );
			writer.marshalDouble ("seed_mag_min"   , seed_mag_min   );
			writer.marshalDouble ("seed_mag_max"   , seed_mag_max   );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			ams             = reader.unmarshalDouble ("ams"            );
			mu              = reader.unmarshalDouble ("mu"             );
			seed_mag_min    = reader.unmarshalDouble ("seed_mag_min"   );
			seed_mag_max    = reader.unmarshalDouble ("seed_mag_max"   );

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OESeedParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OESeedParams catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OESeedParams static_unmarshal (MarshalReader reader, String name) {
		OESeedParams catalog = new OESeedParams();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two catalog parameter structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_param_equal (OESeedParams other) {
		if (
			   this.ams             == other.ams              
			&& this.mu              == other.mu              
			&& this.seed_mag_min    == other.seed_mag_min              
			&& this.seed_mag_max    == other.seed_mag_max              
		) {
			return true;
		}
		return false;
	}




	// Set to typical values, with user-adjustable ams.
	// Returns this object.
	// Note: This function sets the background rate to zero.
	// Note: This is primarily for testing.

	public final OESeedParams set_to_typical (
		double ams
	) {
		this.ams             = ams;
		this.mu              = 0.0;
		this.seed_mag_min    = OEConstants.DEF_MREF;
		this.seed_mag_max    = OEConstants.DEF_MSUP;
		return this;
	}




	// Set to typical values, with user-adjustable ams and mu.
	// Returns this object.
	// Note: This function sets the background rate to zero.
	// Note: This is primarily for testing.

	public final OESeedParams set_to_typical (
		double ams,
		double mu
	) {
		this.ams             = ams;
		this.mu              = mu;
		this.seed_mag_min    = OEConstants.DEF_MREF;
		this.seed_mag_max    = OEConstants.DEF_MSUP;
		return this;
	}

}
