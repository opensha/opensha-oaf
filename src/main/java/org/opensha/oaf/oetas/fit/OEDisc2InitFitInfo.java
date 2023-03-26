package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEGenerationInfo;


// Information about parameter fitting, needed for catalog initialization.
// Author: Michael Barall 03/02/2023.
//
// Threading: This object will be accessed from multiple threads, therefore
// it must not be modified after initial setup.

public class OEDisc2InitFitInfo {

	//----- Configuration -----

	// True to calculate data needed to fit background rate.

	public boolean f_background;


	//----- Source grouping -----

	// Number of groups.  (Zero if grouping was not enabled.)

	public int group_count;

	// Time for each group, in days.  (Null if grouping was not enabled.)
	// The length is group_count.
	// a_group_time[n] is the time of group n, to be used when seeding a simulation.
	// Threading: After setup, this object can be accessed by multiple threads.

	public double[] a_group_time;


	//----- Simulation parameters -----

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	public double mref;

	// Maximum considered magnitude, for parameter definition.

	public double msup;

	// The minimum magnitude to use for the simulation.

	public double mag_min;

	// The maximum magnitude to use for the simulation.

	public double mag_max;


	//----- Export parameters -----

	// Mainshock magnitude, the largest magnitude among ruptures considered mainshocks, or NO_MAG_NEG if none.

	public double mag_main;

	// Time interval for interpreting branch ratios, in days.

	public double tint_br;


	//----- Derived objects -----

	// Parameters for the Bayesian prior.
	// Threading: After setup, this object can be accessed by multiple threads.

	public OEBayPriorParams bay_prior_params;

	// Info for the seed generation.
	// Threading: After setup, this object can be accessed by multiple threads.

	public OEGenerationInfo seed_gen_info;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		f_background = false;

		group_count = 0;
		a_group_time = null;

		mref = 0.0;
		msup = 0.0;
		mag_min = 0.0;
		mag_max = 0.0;

		mag_main = 0.0;
		tint_br = 0.0;

		bay_prior_params = null;
		seed_gen_info = null;

		return;
	}




	// Default constructor.

	public OEDisc2InitFitInfo () {
		clear();
	}




	// Set the values.
	// Returns this object.

	public final OEDisc2InitFitInfo set (
		boolean f_background,
		int group_count,
		double[] a_group_time,
		double mref,
		double msup,
		double mag_min,
		double mag_max,
		double mag_main,
		double tint_br
	) {
		this.f_background = f_background;
		this.group_count = group_count;
		this.a_group_time = a_group_time;
		this.mref = mref;
		this.msup = msup;
		this.mag_min = mag_min;
		this.mag_max = mag_max;
		this.mag_main = mag_main;
		this.tint_br = tint_br;

		this.bay_prior_params = make_bay_prior_params();
		this.seed_gen_info = make_seed_gen_info();

		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2InitFitInfo:" + "\n");

		result.append ("f_background = " + f_background + "\n");
		result.append ("group_count = " + group_count + "\n");
		if (a_group_time != null) {
			result.append ("a_group_time.length = " + a_group_time.length + "\n");
		}
		result.append ("mref = " + mref + "\n");
		result.append ("msup = " + msup + "\n");
		result.append ("mag_min = " + mag_min + "\n");
		result.append ("mag_max = " + mag_max + "\n");
		result.append ("mag_main = " + mag_main + "\n");
		result.append ("tint_br = " + tint_br + "\n");
		if (bay_prior_params != null) {
			result.append (bay_prior_params.toString());
		}
		if (seed_gen_info != null) {
			result.append (seed_gen_info.toString());
		}

		return result.toString();
	}




	//----- Operations -----




	// Calculate the value of (10^a)*Q from the branch ratio.
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	// This function calculates the productivity "a" such that the branch ratio equals n,
	// for the magnitudes and time interval in this object, and returns the value of (10^a)*Q.

	public final double calc_ten_a_q_from_branch_ratio (
		double n,
		double p,
		double c,
		double b,
		double alpha
	) {
		return OEStatsCalc.calc_ten_a_q_from_branch_ratio (
			n,
			p,
			c,
			b,
			alpha,
			mref,
			mag_min,
			mag_max,
			tint_br
		);
	}




	// Calculate the value of ams for a given zero-mref ams-value.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	// Returns the value of ams, for the reference magnitude in this object.

	public final double calc_ams_from_zams (
		double zams,
		double b,
		double alpha
	) {
		return OEStatsCalc.calc_a_new_from_mref_new (
			zams,					// a_old
			b,						// b
			alpha,					// alpha
			OEConstants.ZAMS_MREF,	// mref_old
			mref					// mref_new
		);
	}




	// Calculate the value of (10^ams)*Q for a given zero-mref ams-value.
	// Parameters:
	//  zams = Mainshock productivity parameter, assuming reference magnitude equal to ZAMS_MREF == 0.0.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	// Returns the value of (10^ams)*Q, for the reference magnitude in this object.
	// Note: For mainshock productivities, we take Q == 1.0.

	public final double calc_ten_ams_q_from_zams (
		double zams,
		double b,
		double alpha
	) {
		return Math.pow (10.0, OEStatsCalc.calc_a_new_from_mref_new (
			zams,					// a_old
			b,						// b
			alpha,					// alpha
			OEConstants.ZAMS_MREF,	// mref_old
			mref					// mref_new
		));
	}




	// Calculate a mu-value for a given reference magnitude ZMU_MREF mu-value.
	// Parameters:
	//  zmu = Background rate parameter, assuming reference magnitude equal to ZMU_MREF.
	//  b = Gutenberg-Richter parameter.
	// Returns the mu-value, for the reference magnitude in this object.

	public final double calc_mu_from_zmu (
		double zmu,
		double b
	) {
		return OEStatsCalc.calc_mu_new_from_mref_new (
			zmu,					// mu_old
			b,						// b
			OEConstants.ZMU_MREF,	// mref_old
			mref					// mref_new
		);
	}




	// Calculate an a-value from the branch ratio, for use in catalog simulation.
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	//  cat_mref = Catalog reference magnitude = minimum considered magnitude.
	//  cat_msup = Catalog maximum considered magnitude.
	// This function calculates the productivity "a" such that the branch ratio equals n,
	// for the given magnitudes, and for the time interval in this object.

	public final double calc_a_from_branch_ratio (
		double n,
		double p,
		double c,
		double b,
		double alpha,
		double cat_mref,
		double cat_msup
	) {
		return OEStatsCalc.calc_inv_branch_ratio (
			n,
			p,
			c,
			b,
			alpha,
			cat_mref,
			cat_msup,
			tint_br
		);
	}




	// Calculate an equivalent magnitude for given values of productivity and (10^ams)*Q.
	// Parameters:
	//  k_prod = Seed rupture productivity.
	//  ten_ams_q = Value of (10^ams)*Q, noting that for seed ruptures Q == 1.
	//  alpha = ETAS intensity parameter.
	// Returns the eqivalent mainshock magnitude, that would produce the given productivity.
	// Note that mainshock productivity is calculated as calc_k_uncorr(mag, ams, alpha, mref).

	public final double calc_m0_from_prod_and_ten_ams_q (
		double k_prod,
		double ten_ams_q,
		double alpha
	) {
		return OEStatsCalc.calc_m0_from_k_corr (
			k_prod,			// k_corr
			ten_ams_q,		// ten_a_q
			alpha,			// alpha
			mref			// mref
		);
	}




	// Make parameters for the Bayesian prior.

	public final OEBayPriorParams make_bay_prior_params () {
		return new OEBayPriorParams (
			mag_main,
			tint_br
		);
	}




	// Make info for the seed generation.

	public final OEGenerationInfo make_seed_gen_info () {
		return (new OEGenerationInfo()).set (
			mref,	// gen_mag_min
			msup	// gen_mag_max
		);
	}




	// Return true if we have a mainshock magnitude.

	public final boolean has_mag_main () {
		return mag_main > OEConstants.NO_MAG_NEG_CHECK;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 114001;

	private static final String M_VERSION_NAME = "OEDisc2InitFitInfo";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalBoolean ("f_background", f_background);
			writer.marshalInt     ("group_count" , group_count );
			if (group_count > 0) {
				writer.marshalDoubleArray ("a_group_time", a_group_time);
			}
			writer.marshalDouble  ("mref"        , mref        );
			writer.marshalDouble  ("msup"        , msup        );
			writer.marshalDouble  ("mag_min"     , mag_min     );
			writer.marshalDouble  ("mag_max"     , mag_max     );
			writer.marshalDouble  ("mag_main"    , mag_main    );
			writer.marshalDouble  ("tint_br"     , tint_br     );

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

			f_background = reader.unmarshalBoolean ("f_background");
			group_count  = reader.unmarshalInt     ("group_count" );
			if (group_count > 0) {
				a_group_time = reader.unmarshalDoubleArray ("a_group_time");
			} else {
				a_group_time = null;
			}
			mref         = reader.unmarshalDouble  ("mref"        );
			msup         = reader.unmarshalDouble  ("msup"        );
			mag_min      = reader.unmarshalDouble  ("mag_min"     );
			mag_max      = reader.unmarshalDouble  ("mag_max"     );
			mag_main     = reader.unmarshalDouble  ("mag_main"    );
			tint_br      = reader.unmarshalDouble  ("tint_br"     );

			bay_prior_params = make_bay_prior_params();
			seed_gen_info = make_seed_gen_info();

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

	public OEDisc2InitFitInfo unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDisc2InitFitInfo params) {
		writer.marshalMapBegin (name);
		params.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEDisc2InitFitInfo static_unmarshal (MarshalReader reader, String name) {
		OEDisc2InitFitInfo params = new OEDisc2InitFitInfo();
		reader.unmarshalMapBegin (name);
		params.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return params;
	}




	//----- Testing -----





}
