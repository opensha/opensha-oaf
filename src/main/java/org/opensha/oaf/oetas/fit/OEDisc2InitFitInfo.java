package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;

import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OEGenerationInfo;

import org.opensha.oaf.oetas.bay.OEBayFactory;
import org.opensha.oaf.oetas.bay.OEBayFactoryParams;
import org.opensha.oaf.oetas.bay.OEBayPrior;
import org.opensha.oaf.oetas.bay.OEBayPriorParams;


// Information about parameter fitting, needed for catalog initialization.
// Author: Michael Barall 03/02/2023.
//
// Threading: This object will be accessed from multiple threads, therefore
// it must not be modified after initial setup.

public class OEDisc2InitFitInfo implements Marshalable {

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

	// Options for the parameter grid. [v2]

	public OEGridOptions grid_options;


	//----- Time range end -----

	// The end of the interval time range, in days, as originally requested in the history parameters.

	public double req_t_interval_end;

	// The end of th interval time range, in days, covered by intervals in the history.

	public double hist_t_interval_end;

	// The end of the time range, in days, that is included in the source grouping.
	// (If grouping was not enabled, the end of the time range included in the fitting.)

	public double group_t_interval_end;


	//----- Intensity function calculation -----

	// True to use intervals to fill in below magnitude of completeness.

	public boolean f_intervals;

	// Likelihood magnitude range option.

	public int lmr_opt;

	// True to save information needed to calculate the intensity function.

	public boolean f_intensity;

	// Likelihood interval range, the beginning and ending+1 of intervals to include in likelihood calculation.

	public int like_int_begin;
	public int like_int_end;

	// Mainshock rupture range, the beginning and ending+1 of ruptures to use 'ams' as productivity.

	public int main_rup_begin;
	public int main_rup_end;


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
		grid_options = null;

		req_t_interval_end = 0.0;
		hist_t_interval_end = 0.0;
		group_t_interval_end = 0.0;

		f_intervals = true;
		lmr_opt = OEConstants.LMR_OPT_MCT_INFINITY;
		f_intensity = false;
		like_int_begin = 0;
		like_int_end = 0;
		main_rup_begin = 0;
		main_rup_end = 0;

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
		double tint_br,
		OEGridOptions grid_options,
		double req_t_interval_end,
		double hist_t_interval_end,
		double group_t_interval_end,
		boolean f_intervals,
		int lmr_opt,
		boolean f_intensity,
		int like_int_begin,
		int like_int_end,
		int main_rup_begin,
		int main_rup_end
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
		this.grid_options = (new OEGridOptions()).copy_from (grid_options);
		this.req_t_interval_end = req_t_interval_end;
		this.hist_t_interval_end = hist_t_interval_end;
		this.group_t_interval_end = group_t_interval_end;
		this.f_intervals = f_intervals;
		this.lmr_opt = lmr_opt;
		this.f_intensity = f_intensity;
		this.like_int_begin = like_int_begin;
		this.like_int_end = like_int_end;
		this.main_rup_begin = main_rup_begin;
		this.main_rup_end = main_rup_end;

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
		result.append ("grid_options = {" + grid_options.toString() + "}\n");
		result.append ("req_t_interval_end = " + req_t_interval_end + "\n");
		result.append ("hist_t_interval_end = " + hist_t_interval_end + "\n");
		result.append ("group_t_interval_end = " + group_t_interval_end + "\n");
		result.append ("f_intervals = " + f_intervals + "\n");
		result.append ("lmr_opt = " + lmr_opt + "\n");
		result.append ("f_intensity = " + f_intensity + "\n");
		result.append ("like_int_begin = " + like_int_begin + "\n");
		result.append ("like_int_end = " + like_int_end + "\n");
		result.append ("main_rup_begin = " + main_rup_begin + "\n");
		result.append ("main_rup_end = " + main_rup_end + "\n");
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




	// Calculate the offset for converting relative zams to absolute zams..
	// Parameters:
	//  n = Branch ratio.
	//  p = Omori exponent parameter.
	//  c = Omori offset parameter.
	//  b = Gutenberg-Richter parameter.
	//  alpha = ETAS intensity parameter.
	// This function calculates the offset to add to zams in order to obtain an absolute zams.
	// Note: If zams is not relative, this function returns zero.
	// Note: If zams is relative, this function returns a zams value such that calc_ten_ams_q_from_zams
	// returns the same value as calc_ten_a_q_from_branch_ratio.  In other word, a zams Value
	// such that (10^ams)*Q equals (10^a)*Q (noting that Q = 1.0 for the ams conversion).

	public final double calc_rel_to_abs_zams_offset (
		double n,
		double p,
		double c,
		double b,
		double alpha
	) {
		// If zams is relative ...

		if (grid_options.get_relative_zams()) {

			// Calculate (10^ams)*Q, assuming it is equal to (10^a)*Q

			double ten_ams_q = OEStatsCalc.calc_ten_a_q_from_branch_ratio (
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

			// Assuming Q = 1.0, calculate ams

			double ams = Math.log10 (ten_ams_q);

			// Convert to the reference magnitude for this object

			return OEStatsCalc.calc_a_new_from_mref_new (
				ams,					// a_old
				b,						// b
				alpha,					// alpha
				mref,					// mref_old
				OEConstants.ZAMS_MREF	// mref_new
			);
		}

		// If zams is absolute, return zero

		return 0.0;
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
			tint_br,
			grid_options
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




	// For times after the end of the requested time range, adjust to align with history intervals.
	// Otherwise, return the given time unchnaged.

	public final double adjust_late_time (double t) {
		if (t >= req_t_interval_end - (0.25 * OEConstants.FIT_TIME_EPS)) {
			final double t_delta = hist_t_interval_end - req_t_interval_end;
			return Math.max (hist_t_interval_end, t + t_delta);
		}
		return t;
	}




	// Return true if intensity function is being calculated and interval sources are needed.

	public final boolean needs_interval_intensity () {
		return f_intensity && f_intervals;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 114001;
	private static final int MARSHAL_VER_2 = 114002;

	private static final String M_VERSION_NAME = "OEDisc2InitFitInfo";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

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
			writer.marshalDouble  ("req_t_interval_end"  , req_t_interval_end  );
			writer.marshalDouble  ("hist_t_interval_end" , hist_t_interval_end );
			writer.marshalDouble  ("group_t_interval_end", group_t_interval_end);
			writer.marshalBoolean ("f_intervals"         , f_intervals         );
			writer.marshalInt     ("lmr_opt"             , lmr_opt             );
			writer.marshalBoolean ("f_intensity"         , f_intensity         );
			writer.marshalInt     ("like_int_begin"      , like_int_begin      );
			writer.marshalInt     ("like_int_end"        , like_int_end        );
			writer.marshalInt     ("main_rup_begin"      , main_rup_begin      );
			writer.marshalInt     ("main_rup_end"        , main_rup_end        );

		}
		break;

		case MARSHAL_VER_2: {

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
			OEGridOptions.static_marshal (writer, "grid_options", grid_options);
			writer.marshalDouble  ("req_t_interval_end"  , req_t_interval_end  );
			writer.marshalDouble  ("hist_t_interval_end" , hist_t_interval_end );
			writer.marshalDouble  ("group_t_interval_end", group_t_interval_end);
			writer.marshalBoolean ("f_intervals"         , f_intervals         );
			writer.marshalInt     ("lmr_opt"             , lmr_opt             );
			writer.marshalBoolean ("f_intensity"         , f_intensity         );
			writer.marshalInt     ("like_int_begin"      , like_int_begin      );
			writer.marshalInt     ("like_int_end"        , like_int_end        );
			writer.marshalInt     ("main_rup_begin"      , main_rup_begin      );
			writer.marshalInt     ("main_rup_end"        , main_rup_end        );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

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
			grid_options = new OEGridOptions();
			req_t_interval_end   = reader.unmarshalDouble  ("req_t_interval_end"  );
			hist_t_interval_end  = reader.unmarshalDouble  ("hist_t_interval_end" );
			group_t_interval_end = reader.unmarshalDouble  ("group_t_interval_end");
			f_intervals          = reader.unmarshalBoolean ("f_intervals"         );
			lmr_opt              = reader.unmarshalInt     ("lmr_opt"             );
			f_intensity          = reader.unmarshalBoolean ("f_intensity"         );
			like_int_begin       = reader.unmarshalInt     ("like_int_begin"      );
			like_int_end         = reader.unmarshalInt     ("like_int_end"        );
			main_rup_begin       = reader.unmarshalInt     ("main_rup_begin"      );
			main_rup_end         = reader.unmarshalInt     ("main_rup_end"        );

			bay_prior_params = make_bay_prior_params();
			seed_gen_info = make_seed_gen_info();

		}
		break;

		case MARSHAL_VER_2: {

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
			grid_options = OEGridOptions.static_unmarshal (reader, "grid_options");
			req_t_interval_end   = reader.unmarshalDouble  ("req_t_interval_end"  );
			hist_t_interval_end  = reader.unmarshalDouble  ("hist_t_interval_end" );
			group_t_interval_end = reader.unmarshalDouble  ("group_t_interval_end");
			f_intervals          = reader.unmarshalBoolean ("f_intervals"         );
			lmr_opt              = reader.unmarshalInt     ("lmr_opt"             );
			f_intensity          = reader.unmarshalBoolean ("f_intensity"         );
			like_int_begin       = reader.unmarshalInt     ("like_int_begin"      );
			like_int_end         = reader.unmarshalInt     ("like_int_end"        );
			main_rup_begin       = reader.unmarshalInt     ("main_rup_begin"      );
			main_rup_end         = reader.unmarshalInt     ("main_rup_end"        );

			bay_prior_params = make_bay_prior_params();
			seed_gen_info = make_seed_gen_info();

		}
		break;

		}

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
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
