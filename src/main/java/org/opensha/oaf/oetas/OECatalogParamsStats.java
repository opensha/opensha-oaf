package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to store statistics parameters for an Operational ETAS catalog.
// Author: Michael Barall 12/27/2022.
//
// Holds the subset of the parameters in OECatalogParams that determine the
// statistical properties of a catalog.
//
// See OECatalogParams for definition of parameters.

public class OECatalogParamsStats {

	//----- Parameters -----

	// Productivity parameter.

	public double a;

	// Omori exponent parameter.

	public double p;

	// Omori offset parameter.

	public double c;

	// Gutenberg-Richter parameter.

	public double b;

	// ETAS intensity parameter.

	public double alpha;

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	public double mref;

	// Maximum considered magnitude, for parameter definition.

	public double msup;

	// The range of times for which earthquakes are generated, in days.
	// The time origin is not specified by this class.  It could be the epoch
	// (Jan 1, 1970) or it could be the mainshock time.
	// Note: For statistical properties, only the time interval tbend - tbegin is significant.

	public double tbegin;
	public double tend;

	// The minimum magnitude to use for the simulation.

	public double mag_min_sim;

	// The maximum magnitude to use for the simulation.

	public double mag_max_sim;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		a               = 0.0;
		p               = 0.0;
		c               = 0.0;
		b               = 0.0;
		alpha           = 0.0;
		mref            = 0.0;
		msup            = 0.0;
		tbegin          = 0.0;
		tend            = 0.0;
		mag_min_sim     = 0.0;
		mag_max_sim     = 0.0;
		return;
	}




	// Default constructor.

	public OECatalogParamsStats () {
		clear();
	}




	// Constructor that sets all values.

	public OECatalogParamsStats (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim
	) {
		this.a               = a;
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = mref;
		this.msup            = msup;
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
	}




	// Set all values.
	// Returns this object.

	public final OECatalogParamsStats set (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim
	) {
		this.a               = a;
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = mref;
		this.msup            = msup;
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		return this;
	}




	// Copy all values from the other object.
	// Returns this object.

	public final OECatalogParamsStats copy_from (OECatalogParamsStats other) {
		this.a               = other.a;
		this.p               = other.p;
		this.c               = other.c;
		this.b               = other.b;
		this.alpha           = other.alpha;
		this.mref            = other.mref;
		this.msup            = other.msup;
		this.tbegin          = other.tbegin;
		this.tend            = other.tend;
		this.mag_min_sim     = other.mag_min_sim;
		this.mag_max_sim     = other.mag_max_sim;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogParamsStats:" + "\n");

		result.append ("a = "               + a               + "\n");
		result.append ("p = "               + p               + "\n");
		result.append ("c = "               + c               + "\n");
		result.append ("b = "               + b               + "\n");
		result.append ("alpha = "           + alpha           + "\n");
		result.append ("mref = "            + mref            + "\n");
		result.append ("msup = "            + msup            + "\n");
		result.append ("tbegin = "          + tbegin          + "\n");
		result.append ("tend = "            + tend            + "\n");
		result.append ("mag_min_sim = "     + mag_min_sim     + "\n");
		result.append ("mag_max_sim = "     + mag_max_sim     + "\n");

		return result.toString();
	}




	// Set the minimum magnitude to a fixed value.
	// Note: Produces a minimum magnitude that is non-adjustable.
	// Note: This produces the same result as OECatalogParams.set_fixed_mag_min followed by get_params_stats.

	public final void set_fixed_mag_min (double mag_min) {
		this.mag_min_sim     = mag_min;
		return;
	}




	// Set the maximum magnitude to a fixed value.
	// Note: Produces a maximum magnitude that is non-adjustable.
	// Note: This produces the same result as OECatalogParams.set_fixed_mag_max followed by get_params_stats.

	public final void set_fixed_mag_max (double mag_max) {
		this.mag_max_sim     = mag_max;
		return;
	}




	// Return the magnitude range parameters.
	// The returned object is newly allocated.
	// Note: This produces the same result as OECatalogParams.get_params_mags.

	public final OECatalogParamsMags get_params_mags () {
		return new OECatalogParamsMags (
			mref,
			msup,
			mag_min_sim,
			mag_max_sim
		);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 107001;

	private static final String M_VERSION_NAME = "OECatalogParamsStats";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("a"              , a              );
			writer.marshalDouble ("p"              , p              );
			writer.marshalDouble ("c"              , c              );
			writer.marshalDouble ("b"              , b              );
			writer.marshalDouble ("alpha"          , alpha          );
			writer.marshalDouble ("mref"           , mref           );
			writer.marshalDouble ("msup"           , msup           );
			writer.marshalDouble ("tbegin"         , tbegin         );
			writer.marshalDouble ("tend"           , tend           );
			writer.marshalDouble ("mag_min_sim"    , mag_min_sim    );
			writer.marshalDouble ("mag_max_sim"    , mag_max_sim    );

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

			a               = reader.unmarshalDouble ("a"              );
			p               = reader.unmarshalDouble ("p"              );
			c               = reader.unmarshalDouble ("c"              );
			b               = reader.unmarshalDouble ("b"              );
			alpha           = reader.unmarshalDouble ("alpha"          );
			mref            = reader.unmarshalDouble ("mref"           );
			msup            = reader.unmarshalDouble ("msup"           );
			tbegin          = reader.unmarshalDouble ("tbegin"         );
			tend            = reader.unmarshalDouble ("tend"           );
			mag_min_sim     = reader.unmarshalDouble ("mag_min_sim"    );
			mag_max_sim     = reader.unmarshalDouble ("mag_max_sim"    );

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

	public OECatalogParamsStats unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogParamsStats catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogParamsStats static_unmarshal (MarshalReader reader, String name) {
		OECatalogParamsStats catalog = new OECatalogParamsStats();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two catalog parameter structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_param_equal (OECatalogParamsStats other) {
		if (
			   this.a               == other.a              
			&& this.p               == other.p              
			&& this.c               == other.c              
			&& this.b               == other.b              
			&& this.alpha           == other.alpha          
			&& this.mref            == other.mref           
			&& this.msup            == other.msup           
			&& this.tbegin          == other.tbegin         
			&& this.tend            == other.tend           
			&& this.mag_min_sim     == other.mag_min_sim    
			&& this.mag_max_sim     == other.mag_max_sim    
		) {
			return true;
		}
		return false;
	}




	// Set to typical values, with some user-adjustable parameters.
	// Returns this object.
	// Note: This is primarily for testing.
	// Note: This is a subset of the values produced by OECatalogParams.set_to_typical.

	public final OECatalogParamsStats set_to_typical (
		double a,
		double p,
		double c,
		double b,
		double alpha
	) {
		this.a               = a;
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = 3.0;
		this.msup            = 9.5;
		this.tbegin          = 1.0;
		this.tend            = 366.0;
		this.mag_min_sim     = 3.0;
		this.mag_max_sim     = 9.5;
		return this;
	}




	// Set to values for simulation within a fixed magnitude range.
	// Parameters:
	//  a = Productivity parameter.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter, in days.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Minimum magnitude, also the reference magnitude and min mag for parameter definition.
	//  msup = Maximum magnitude, also the max mag for parameter definition.
	//  tbegin = Beginning time for which earthquakes are generated, in days.
	//  tend = Ending time for which earthquakes are generated, in days.
	// Returns this object.
	// Note: This is a subset of the values produced by OECatalogParams.set_to_fixed_mag.

	public final OECatalogParamsStats set_to_fixed_mag (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tbegin,
		double tend
	) {
		this.a               = a;
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = mref;
		this.msup            = msup;
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mref;
		this.mag_max_sim     = msup;
		return this;
	}




	// Set to values for simulation within a fixed magnitude range.
	// The productivity is specified as a branch ratio.
	// Parameters:
	//  n = Branch ratio, as computed for these parameters.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter, in days.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  mref = Minimum magnitude, also the reference magnitude and min mag for parameter definition.
	//  msup = Maximum magnitude, also the max mag for parameter definition.
	//  tbegin = Beginning time for which earthquakes are generated, in days.
	//  tend = Ending time for which earthquakes are generated, in days.
	// Returns this object.
	// Note: This is a subset of the values produced by OECatalogParams.set_to_fixed_mag_br.

	public final OECatalogParamsStats set_to_fixed_mag_br (
		double n,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tbegin,
		double tend
	) {
		this.a               = OEStatsCalc.calc_inv_branch_ratio (n, p, c, b, alpha, mref, msup, tend - tbegin);
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = mref;
		this.msup            = msup;
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mref;
		this.mag_max_sim     = msup;
		return this;
	}




	// Set the branch ratio, leaving everything else unchanged.
	// Parameters:
	//  n = Branch ratio, as computed for these parameters.
	// Returns this object.
	// Note: Same as OECatalogParams.set_br.

	public final OECatalogParamsStats set_br (
		double n
	) {
		this.a               = OEStatsCalc.calc_inv_branch_ratio (n, p, c, b, alpha, mref, msup, tend - tbegin);
		return this;
	}




	// Calculate an a-value for a given branch ratio.
	// This function does not change the contents of the object.
	// Parameters:
	//  n = Branch ratio, as computed for these parameters.
	// Returns the a-value.
	// Note: Same as OECatalogParams.calc_a_for_br.

	public final double calc_a_for_br (
		double n
	) {
		return OEStatsCalc.calc_inv_branch_ratio (n, p, c, b, alpha, mref, msup, tend - tbegin);
	}




	// Set the a-value, leaving everything else unchanged.
	// Parameters:
	//  a = New value of a.
	// Returns this object.
	// Note: Same as OECatalogParams.set_a.

	public final OECatalogParamsStats set_a (
		double a
	) {
		this.a               = a;
		return this;
	}




	// Calculate an ams-value for a given zero-mref ams-value.
	// This function does not change the contents of the object.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming zero reference magnitude.
	// Returns the ams-value, for the reference magnitude mref in this object.
	// Note: Same as OECatalogParams.calc_ams_from_zams.

	public final double calc_ams_from_zams (
		double zams
	) {
		return OEStatsCalc.calc_a_new_from_mref_new (
			zams,			// a_old
			b,				// b
			alpha,			// alpha
			0.0,			// mref_old
			mref			// mref_new
		);
	}




	// Calculate zero-mref ams-value for a ginve ams-value
	// This function does not change the contents of the object.
	// Parameters:
	//  ams = Mainshock productivity parameter, assuming reference magnitude equal to mref.
	// Returns the ams-value, for zero reference magnitude.
	// Note: Same as OECatalogParams.calc_zams_from_ams.

	public final double calc_zams_from_ams (
		double ams
	) {
		return OEStatsCalc.calc_a_new_from_mref_new (
			ams,			// a_old
			b,				// b
			alpha,			// alpha
			mref,			// mref_old
			0.0				// mref_new
		);
	}

}
