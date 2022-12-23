package org.opensha.oaf.oetas.fit;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.oetas.OEConstants;

import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value


// Class to store parameters for discretized magnitude of completeness with FGH (Helmstetter) parameters.
// Author: Michael Barall 09/03/2020.

public class OEDiscFGHParams {

	//----- Base Parameters -----

	// Catalog magnitude of completeness.

	public double magCat;

	// Helmstetter Parameters.
	// Use capG = 100.0 to disable time-dependent magnitude of completeness.

	public double capF;
	public double capG;
	public double capH;

	// Beginning and end of the time range of interest, in days.
	// Rupture times should be within this time range.  There is some allowance (currently 1 day)
	// to include ruptures close to this range, but ruptures farther away are disregarded.

	public double t_range_begin;
	public double t_range_end;

	//----- Discretization Parameters -----

	// The difference between magnitudes which are indistinguishable.  See OEMagCompFnDisc.MemoryBuilder.

	public double mag_eps;

	// The difference between times which are indistinguishable.  See OEMagCompFnDisc.MemoryBuilder.

	public double time_eps;

	// Discretization base magnitude.  See OEMagCompFnDiscFGH.make_disc_even().

	public double disc_base;

	// Discretization delta between discrete values.  See OEMagCompFnDiscFGH.make_disc_even().

	public double disc_delta;

	// Discretization rounding option for cutoffs: 0.0 = floor, 0.5 = nearest, 1.0 = ceiling.  See OEMagCompFnDiscFGH.make_disc_even().

	public double disc_round;

	// Minimum gap between magCat and any other constant values (magnitudes and discretized log values).  See OEMagCompFnDiscFGH.set_disc_options().

	public double disc_gap;

	//----- Splitting Parameters -----

	// Maximum number of ruptures that can have magnitudes >= magCat, or 0 if no limit (limit is flexible);
	// the value of magCat is increased if necessary to reduce the number of ruptures.

	public int mag_cat_count;

	// Minimum magnitude for eligible ruptures, or NO_MAG_NEG if no minimum
	// (all ruptures eligible), or NO_MAG_POS if no ruptures are eligible.
	// Note: Eligible ruptures are those that produce time-dependent incompleteness.

	public double eligible_mag;

	// Maximum number of eligible ruptures, or 0 if no limit (limit is flexible);
	// the minimum magnitude is increased if necessary to reduce the number of ruptures.

	public int eligible_count;

	// Minimum magnitude for ruptures to force interval division, or NO_MAG_NEG if no minimum
	// (all ruptures force division), or NO_MAG_POS if no ruptures force division.

	public double division_mag;

	// Maximum number of ruptures that force division, or 0 if no limit (limit is flexible);
	// the minimum magnitude is increased if necessary to reduce the number of ruptures.

	public int division_count;

	// Splitting function, or null to disable all splitting.

	public OEMagCompFnDisc.SplitFn split_fn;

	//----- Joining Parameters -----

	// List of times at which splits are required (in addition to rupture times and automatically
	// generated splits), in days.  If null, then t_range_begin and t_range_end are used.
	// These times should lie within the time range t_range_begin to t_range_end.

	public double[] t_req_splits;

	// Maximum number of ruptures allowed before the first required split (limit is flexible),
	// or 0 or -1 if no limit.  If non-zero, them mag_cat_count applies only to ruptures
	// after the first required split.  If zero, then mag_cat_count applies to all ruptures.

	public int before_max_count;

	// Option to join intervals whose magnitude of completeness is magCat: 0 = no join, 1 = join.

	public int mag_cat_int_join;





	//----- Construction -----




	// Utility function to copy an array, or null.

	private static double[] copy_array_or_null (double[] x) {
		if (x == null) {
			return null;
		}
		return Arrays.copyOf (x, x.length);
	}




	// Utility function to equality-test two arrays, or null.

	private static boolean equals_array_or_null (double[] x1, double[] x2) {
		if (x1 == null) {
			if (x2 == null) {
				return true;
			}
			return false;
		}
		if (x2 == null) {
			return false;
		}
		int len = x1.length;
		if (x2.length != len) {
			return false;
		}
		for (int i = 0; i < len; ++i) {
			if (x1[i] != x2[i]) {
				return false;
			}
		}
		return true;
	}




	// Clear to default values.

	public final void clear () {

		magCat         = 0.0;
		capF           = 0.0;
		capG           = 0.0;
		capH           = 0.0;
		t_range_begin  = 0.0;
		t_range_end    = 0.0;

		mag_eps        = 0.0;
		time_eps       = 0.0;
		disc_base      = 0.0;
		disc_delta     = 0.0;
		disc_round     = 0.0;
		disc_gap       = 0.0;

		mag_cat_count  = 0;
		eligible_mag   = 0.0;
		eligible_count = 0;
		division_mag   = 0.0;
		division_count = 0;
		split_fn       = null;

		t_req_splits = null;
		before_max_count = 0;
		mag_cat_int_join = 0;

		return;
	}




	// Default constructor.

	public OEDiscFGHParams () {
		clear();
	}




	// Set all values.

	public final OEDiscFGHParams set (
		double magCat,
		double capF,
		double capG,
		double capH,
		double t_range_begin,
		double t_range_end,

		double mag_eps,
		double time_eps,
		double disc_base,
		double disc_delta,
		double disc_round,
		double disc_gap,

		int mag_cat_count,
		double eligible_mag,
		int eligible_count,
		double division_mag,
		int division_count,
		OEMagCompFnDisc.SplitFn split_fn
	) {

		this.magCat         = magCat;
		this.capF           = capF;
		this.capG           = capG;
		this.capH           = capH;
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = mag_eps;
		this.time_eps       = time_eps;
		this.disc_base      = disc_base;
		this.disc_delta     = disc_delta;
		this.disc_round     = disc_round;
		this.disc_gap       = disc_gap ;

		this.mag_cat_count  = mag_cat_count;
		this.eligible_mag   = eligible_mag;
		this.eligible_count = eligible_count;
		this.division_mag   = division_mag;
		this.division_count = division_count;
		this.split_fn       = split_fn;

		this.t_req_splits = null;
		this.before_max_count = 0;
		this.mag_cat_int_join = 0;

		return this;
	}




	// Set all values, with joining parameters.

	public final OEDiscFGHParams set (
		double magCat,
		double capF,
		double capG,
		double capH,
		double t_range_begin,
		double t_range_end,

		double mag_eps,
		double time_eps,
		double disc_base,
		double disc_delta,
		double disc_round,
		double disc_gap,

		int mag_cat_count,
		double eligible_mag,
		int eligible_count,
		double division_mag,
		int division_count,
		OEMagCompFnDisc.SplitFn split_fn,

		double[] t_req_splits,
		int before_max_count,
		int mag_cat_int_join
	) {

		this.magCat         = magCat;
		this.capF           = capF;
		this.capG           = capG;
		this.capH           = capH;
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = mag_eps;
		this.time_eps       = time_eps;
		this.disc_base      = disc_base;
		this.disc_delta     = disc_delta;
		this.disc_round     = disc_round;
		this.disc_gap       = disc_gap ;

		this.mag_cat_count  = mag_cat_count;
		this.eligible_mag   = eligible_mag;
		this.eligible_count = eligible_count;
		this.division_mag   = division_mag;
		this.division_count = division_count;
		this.split_fn       = split_fn;

		this.t_req_splits = copy_array_or_null (t_req_splits);
		this.before_max_count = before_max_count;
		this.mag_cat_int_join = mag_cat_int_join;

		return this;
	}




	// Set values to make all ruptures eligible.

	public final OEDiscFGHParams set_eligible_all () {
		eligible_mag = NO_MAG_NEG;
		eligible_count = 0;
		return this;
	}




	// Set values to make no ruptures eligible.

	public final OEDiscFGHParams set_eligible_none () {
		eligible_mag = NO_MAG_POS;
		eligible_count = 0;
		return this;
	}




	// Set values to make all ruptures force division.

	public final OEDiscFGHParams set_division_all () {
		division_mag = NO_MAG_NEG;
		division_count = 0;
		return this;
	}




	// Set values to make no ruptures force division.

	public final OEDiscFGHParams set_division_none () {
		division_mag = NO_MAG_POS;
		division_count = 0;
		return this;
	}




	// Force splitting to be enabled, if no splitting function supplied.

	public final OEDiscFGHParams require_splitting () {
		if (split_fn == null) {
			split_fn = new OEMagCompFnDisc.SplitFnConstant();
		}
		return this;
	}




//	// Get the time when the first required interval begins.
//	// Note: Strictly speaking this is undefined if t_req_splits is a zero-length array.
//
//	public final double get_interval_begin () {
//		double t = t_range_begin;
//		if (t_req_splits != null && t_req_splits.length > 0) {
//			t = t_req_splits[0];
//			for (int i = 1; i < t_req_splits.length; ++i) {
//				t = Math.min (t, t_req_splits[i]);
//			}
//		}
//		return t;
//	}




	// Return true if we want to join intervals whose magnitude of completeness is magCat.

	public final boolean is_mag_cat_int_join () {
		if (mag_cat_int_join == 0) {
			return false;
		}
		return true;
	}




	// Copy all values from the other object.

	public final OEDiscFGHParams copy_from (OEDiscFGHParams other) {

		this.magCat         = other.magCat;
		this.capF           = other.capF;
		this.capG           = other.capG;
		this.capH           = other.capH;
		this.t_range_begin  = other.t_range_begin;
		this.t_range_end    = other.t_range_end;

		this.mag_eps        = other.mag_eps;
		this.time_eps       = other.time_eps;
		this.disc_base      = other.disc_base;
		this.disc_delta     = other.disc_delta;
		this.disc_round     = other.disc_round;
		this.disc_gap       = other.disc_gap ;

		this.mag_cat_count  = other.mag_cat_count;
		this.eligible_mag   = other.eligible_mag;
		this.eligible_count = other.eligible_count;
		this.division_mag   = other.division_mag;
		this.division_count = other.division_count;
		this.split_fn       = other.split_fn;

		this.t_req_splits = copy_array_or_null (other.t_req_splits);
		this.before_max_count = other.before_max_count;
		this.mag_cat_int_join = other.mag_cat_int_join;

		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDiscFGHParams:" + "\n");

		result.append ("magCat = "         + magCat         + "\n");
		result.append ("capF = "           + capF           + "\n");
		result.append ("capG = "           + capG           + "\n");
		result.append ("capH = "           + capH           + "\n");
		result.append ("t_range_begin = "  + t_range_begin  + "\n");
		result.append ("t_range_end = "    + t_range_end    + "\n");

		result.append ("mag_eps = "        + mag_eps        + "\n");
		result.append ("time_eps = "       + time_eps       + "\n");
		result.append ("disc_base = "      + disc_base      + "\n");
		result.append ("disc_delta = "     + disc_delta     + "\n");
		result.append ("disc_round = "     + disc_round     + "\n");
		result.append ("disc_gap = "       + disc_gap       + "\n");

		result.append ("mag_cat_count = "  + mag_cat_count  + "\n");
		result.append ("eligible_mag = "   + eligible_mag   + "\n");
		result.append ("eligible_count = " + eligible_count + "\n");
		result.append ("division_mag = "   + division_mag   + "\n");
		result.append ("division_count = " + division_count + "\n");
		result.append ("split_fn = "       + split_fn.toString() + "\n");

		result.append ("t_req_splits = " + Arrays.toString (t_req_splits) + "\n");
		result.append ("before_max_count = " + before_max_count + "\n");
		result.append ("mag_cat_int_join = " + mag_cat_int_join + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 92001;

	private static final String M_VERSION_NAME = "OEDiscFGHParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("magCat"         , magCat        );
			writer.marshalDouble ("capF"           , capF          );
			writer.marshalDouble ("capG"           , capG          );
			writer.marshalDouble ("capH"           , capH          );
			writer.marshalDouble ("t_range_begin"  , t_range_begin );
			writer.marshalDouble ("t_range_end"    , t_range_end   );

			writer.marshalDouble ("mag_eps"        , mag_eps       );
			writer.marshalDouble ("time_eps"       , time_eps      );
			writer.marshalDouble ("disc_base"      , disc_base     );
			writer.marshalDouble ("disc_delta"     , disc_delta    );
			writer.marshalDouble ("disc_round"     , disc_round    );
			writer.marshalDouble ("disc_gap"       , disc_gap      );

			writer.marshalInt    ("mag_cat_count"  , mag_cat_count );
			writer.marshalDouble ("eligible_mag"   , eligible_mag  );
			writer.marshalInt    ("eligible_count" , eligible_count);
			writer.marshalDouble ("division_mag"   , division_mag  );
			writer.marshalInt    ("division_count" , division_count);
			OEMagCompFnDisc.marshal_splitfn_poly (writer, "split_fn", split_fn);

			boolean has_req_splits = (t_req_splits != null);
			writer.marshalBoolean ("has_req_splits", has_req_splits);
			if (has_req_splits) {
				writer.marshalDoubleArray ("t_req_splits", t_req_splits);
			}
			writer.marshalInt    ("before_max_count", before_max_count);
			writer.marshalInt    ("mag_cat_int_join", mag_cat_int_join);

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

			magCat         = reader.unmarshalDouble ("magCat"        );
			capF           = reader.unmarshalDouble ("capF"          );
			capG           = reader.unmarshalDouble ("capG"          );
			capH           = reader.unmarshalDouble ("capH"          );
			t_range_begin  = reader.unmarshalDouble ("t_range_begin" );
			t_range_end    = reader.unmarshalDouble ("t_range_end"   );

			mag_eps        = reader.unmarshalDouble ("mag_eps"       );
			time_eps       = reader.unmarshalDouble ("time_eps"      );
			disc_base      = reader.unmarshalDouble ("disc_base"     );
			disc_delta     = reader.unmarshalDouble ("disc_delta"    );
			disc_round     = reader.unmarshalDouble ("disc_round"    );
			disc_gap       = reader.unmarshalDouble ("disc_gap"      );

			mag_cat_count  = reader.unmarshalInt    ("mag_cat_count" );
			eligible_mag   = reader.unmarshalDouble ("eligible_mag"  );
			eligible_count = reader.unmarshalInt    ("eligible_count");
			division_mag   = reader.unmarshalDouble ("division_mag"  );
			division_count = reader.unmarshalInt    ("division_count");
			split_fn = OEMagCompFnDisc.unmarshal_splitfn_poly (reader, "split_fn");

			boolean has_req_splits = reader.unmarshalBoolean  ("has_req_splits");
			if (has_req_splits) {
				t_req_splits = reader.unmarshalDoubleArray ("t_req_splits");
			} else {
				t_req_splits = null;
			}
			before_max_count = reader.unmarshalInt  ("before_max_count");
			mag_cat_int_join = reader.unmarshalInt  ("mag_cat_int_join");

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

	public OEDiscFGHParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDiscFGHParams params) {
		writer.marshalMapBegin (name);
		params.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEDiscFGHParams static_unmarshal (MarshalReader reader, String name) {
		OEDiscFGHParams params = new OEDiscFGHParams();
		reader.unmarshalMapBegin (name);
		params.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return params;
	}




	//----- Testing -----




	// Check if two parameter structures are identical.
	// Note: This is primarily for testing.

	public boolean check_param_equal (OEDiscFGHParams other) {
		if (
			   this.magCat         == other.magCat
			&& this.capF           == other.capF
			&& this.capG           == other.capG
			&& this.capH           == other.capH
			&& this.t_range_begin  == other.t_range_begin
			&& this.t_range_end    == other.t_range_end
			
			&& this.mag_eps        == other.mag_eps
			&& this.time_eps       == other.time_eps
			&& this.disc_base      == other.disc_base
			&& this.disc_delta     == other.disc_delta
			&& this.disc_round     == other.disc_round
			&& this.disc_gap       == other.disc_gap

			&& this.mag_cat_count  == other.mag_cat_count
			&& this.eligible_mag   == other.eligible_mag
			&& this.eligible_count == other.eligible_count
			&& this.division_mag   == other.division_mag
			&& this.division_count == other.division_count
			&& OEMagCompFnDisc.equals_splitfn (this.split_fn, other.split_fn)

			&& equals_array_or_null (this.t_req_splits, other.t_req_splits)
			&& this.before_max_count == other.before_max_count
			&& this.mag_cat_int_join == other.mag_cat_int_join
		) {
			return true;
		}
		return false;
	}




	// Set to typical values, for a given mainshock magnitude and time range.
	// Note: This is primarily for testing.

	public OEDiscFGHParams set_to_typical (
		double main_mag,
		double t_range_begin,
		double t_range_end
	) {

		this.magCat         = Math.min (3.0, main_mag - 1.0);
		this.capF           = 0.5;
		this.capG           = 0.25;
		this.capH           = 1.0;
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = 0.001;
		this.time_eps       = 1.0e-7;
		this.disc_base      = 0.005;
		this.disc_delta     = 0.2;
		this.disc_round     = 0.5;
		this.disc_gap       = 0.05;

		this.mag_cat_count  = 2000;
		this.eligible_mag   = main_mag - 3.0;
		this.eligible_count = 20;
		this.division_mag   = main_mag - 4.0;
		this.division_count = 200;
		this.split_fn       = new OEMagCompFnDisc.SplitFnRatio (0.3, 0.04, 3.0);

		this.t_req_splits = null;
		this.before_max_count = 0;
		this.mag_cat_int_join = 0;

		return this;
	}


	public OEDiscFGHParams set_to_typical (
		double main_mag,
		double t_range_begin,
		double t_range_end,
		double t_interval_begin
	) {

		this.magCat         = Math.min (3.0, main_mag - 1.0);
		this.capF           = 0.5;
		this.capG           = 0.25;
		this.capH           = 1.0;
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = 0.001;
		this.time_eps       = 1.0e-7;
		this.disc_base      = 0.005;
		this.disc_delta     = 0.2;
		this.disc_round     = 0.5;
		this.disc_gap       = 0.05;

		this.mag_cat_count  = 2000;
		this.eligible_mag   = main_mag - 3.0;
		this.eligible_count = 20;
		this.division_mag   = main_mag - 4.0;
		this.division_count = 200;
		this.split_fn       = new OEMagCompFnDisc.SplitFnRatio (0.3, 0.04, 3.0);

		this.t_req_splits = new double[2];
		this.t_req_splits[0] = t_interval_begin;
		this.t_req_splits[1] = t_range_end;
		this.before_max_count = 50;
		this.mag_cat_int_join = 1;

		return this;
	}




	// Set to typical values, for constructing a history from a simulation.
	// Parameters:
	//  magCat = Catalog magnitude of completeness.
	//  helm_param = Helmstetter parameter option, see OEConstants.HELM_PARAM_XXXXX.
	//  t_range_begin = Beginning of time range, in days.
	//  t_range_end = End of time range, in days.
	//  disc_delta = Discretization delta, should be 0.01 multiplied by some integer.
	//  mag_cat_count = Maximum number of ruptures that can have magnitudes >= magCat, or 0 if no limit.
	//  eligible_mag = Minimum magnitude for eligible ruptures, NO_MAG_NEG = all, NO_MAG_POS = none.
	//  eligible_count = Maximum number of eligible ruptures, or 0 if no limit.
	//  split_fn = Splitting function, must be non-null for any splitting to occur.
	//  t_interval_begin = If included, the time at which needed intervals begin.
	//  before_max_count = If included, the max number of ruptures before the first required split, or 0 or -1 if no limit.
	//  mag_cat_int_join = If included, option to join intervals with mc equal to magCat,  0 = no join, 1 = join.

	public OEDiscFGHParams set_sim_history_typical (
		double magCat,
		int helm_param,
		double t_range_begin,
		double t_range_end,
		double disc_delta,
		int mag_cat_count,
		double eligible_mag,
		int eligible_count,
		OEMagCompFnDisc.SplitFn split_fn
	) {

		this.magCat         = magCat;
		this.capF           = OEConstants.helm_capF (helm_param);
		this.capG           = OEConstants.helm_capG (helm_param);
		this.capH           = OEConstants.helm_capH (helm_param);
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = OEConstants.FIT_MAG_EPS;
		this.time_eps       = OEConstants.FIT_TIME_EPS;
		this.disc_base      = 0.005;	// put base in between mags to 2 decimal places
		this.disc_delta     = disc_delta;
		this.disc_round     = 0.5;		// round to nearest
		this.disc_gap       = disc_delta * 0.25;

		this.mag_cat_count  = mag_cat_count;
		this.eligible_mag   = eligible_mag;
		this.eligible_count = eligible_count;
		this.division_mag   = NO_MAG_NEG;
		this.division_count = 0;
		this.split_fn       = split_fn;

		this.t_req_splits = null;
		this.before_max_count = 0;
		this.mag_cat_int_join = 0;

		return this;
	}

	public OEDiscFGHParams set_sim_history_typical (
		double magCat,
		int helm_param,
		double t_range_begin,
		double t_range_end,
		double disc_delta,
		int mag_cat_count,
		double eligible_mag,
		int eligible_count,
		OEMagCompFnDisc.SplitFn split_fn,
		double t_interval_begin
	) {

		this.magCat         = magCat;
		this.capF           = OEConstants.helm_capF (helm_param);
		this.capG           = OEConstants.helm_capG (helm_param);
		this.capH           = OEConstants.helm_capH (helm_param);
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = OEConstants.FIT_MAG_EPS;
		this.time_eps       = OEConstants.FIT_TIME_EPS;
		this.disc_base      = 0.005;	// put base in between mags to 2 decimal places
		this.disc_delta     = disc_delta;
		this.disc_round     = 0.5;		// round to nearest
		this.disc_gap       = disc_delta * 0.25;

		this.mag_cat_count  = mag_cat_count;
		this.eligible_mag   = eligible_mag;
		this.eligible_count = eligible_count;
		this.division_mag   = NO_MAG_NEG;
		this.division_count = 0;
		this.split_fn       = split_fn;

		this.t_req_splits = new double[2];
		this.t_req_splits[0] = t_interval_begin;
		this.t_req_splits[1] = t_range_end;
		this.before_max_count = 50;
		this.mag_cat_int_join = 1;

		return this;
	}

	public OEDiscFGHParams set_sim_history_typical (
		double magCat,
		int helm_param,
		double t_range_begin,
		double t_range_end,
		double disc_delta,
		int mag_cat_count,
		double eligible_mag,
		int eligible_count,
		OEMagCompFnDisc.SplitFn split_fn,
		double t_interval_begin,
		int before_max_count,
		int mag_cat_int_join
	) {

		this.magCat         = magCat;
		this.capF           = OEConstants.helm_capF (helm_param);
		this.capG           = OEConstants.helm_capG (helm_param);
		this.capH           = OEConstants.helm_capH (helm_param);
		this.t_range_begin  = t_range_begin;
		this.t_range_end    = t_range_end;

		this.mag_eps        = OEConstants.FIT_MAG_EPS;
		this.time_eps       = OEConstants.FIT_TIME_EPS;
		this.disc_base      = 0.005;	// put base in between mags to 2 decimal places
		this.disc_delta     = disc_delta;
		this.disc_round     = 0.5;		// round to nearest
		this.disc_gap       = disc_delta * 0.25;

		this.mag_cat_count  = mag_cat_count;
		this.eligible_mag   = eligible_mag;
		this.eligible_count = eligible_count;
		this.division_mag   = NO_MAG_NEG;
		this.division_count = 0;
		this.split_fn       = split_fn;

		this.t_req_splits = new double[2];
		this.t_req_splits[0] = t_interval_begin;
		this.t_req_splits[1] = t_range_end;
		this.before_max_count = before_max_count;
		this.mag_cat_int_join = mag_cat_int_join;

		return this;
	}

}
