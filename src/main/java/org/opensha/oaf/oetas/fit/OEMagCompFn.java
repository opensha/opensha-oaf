package org.opensha.oaf.oetas.fit;

import java.util.Collection;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import static org.opensha.oaf.oetas.OEConstants.HELM_CAPG_DISABLE;		// value of Helmstetter G to disable incompleteness
import static org.opensha.oaf.oetas.OEConstants.HELM_CAPG_DISABLE_CHECK;	// use capG > HELM_CAPG_DISABLE_CHECK to check for HELM_CAPG_DISABLE


/**
 * Time-dependent magnitude of completeness function, for operational ETAS.
 * Author: Michael Barall 02/16/2020.
 *
 * This abstract class represents a time-dependent magnitude of completeness
 * function.
 *
 * Objects of this class, and its subclasses, are immutable and stateless.
 * They are pure functions, which means that their outputs depend only on the
 * supplied value of t_days (time since the origin in days).
 */
public abstract class OEMagCompFn {

	//----- Evaluation -----


	// Calculate the time-dependent magnitude of completeness.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the magnitude of completeness at the given time.
	// Note: This function does not define the origin of time.
	// Note: The returned value must be >= the value returned by get_mag_cat().
	// It is expected that as t_days becomes large, the returned value
	// equals or approaches the value returned by get_mag_cat()

	public abstract double get_mag_completeness (double t_days);




	// Get the catalog magnitude of completeness.
	// Returns the magnitude of completeness in normal times.

	public abstract double get_mag_cat ();




	//----- Construction -----


	// Default constructor does nothing.

	public OEMagCompFn () {}


	// Display our contents

	@Override
	public String toString() {
		return "OEMagCompFn";
	}




	//----- Factory methods -----


	// Construct a function which is constant, equal to magCat.

	public static OEMagCompFn makeConstant (double magCat) {
		return new OEMagCompFnConstant (magCat);
	}


	// Construct a function which is defined by F,G,H parameters.
	// This funnction is a placeholder.

	public static OEMagCompFn makeFGH (double magCat, double capF, double capG, double capH) {
		return new OEMagCompFnFGH (magCat, capF, capG, capH);
	}


	// Construct a function which is defined by F,G,H parameters, or a constant.
	// If capG = 100.0 (== HELM_CAPG_DISABLE), then return a constant function whose value is always magCat.
	// Otherwise, return the F,G,H function.

	public static OEMagCompFn makeFGHOrConstant (double magCat, double capF, double capG, double capH) {
		if (capG > HELM_CAPG_DISABLE_CHECK) {
			return new OEMagCompFnConstant (magCat);
		}
		return new OEMagCompFnFGH (magCat, capF, capG, capH);
	}


	// Construct a function which is defined by F,G,H parameters for multiple earthquakes.

	public static OEMagCompFn makeMultiFGH (double magCat, double capF, double capG, double capH,
											double t_range_begin, double t_range_end,
											Collection<OERupture> rup_list,
											Collection<OERupture> accept_list, Collection<OERupture> reject_list,
											double eligible_mag, int eligible_count) {
		return new OEMagCompFnMultiFGH (magCat, capF, capG, capH,
										t_range_begin, t_range_end,
										rup_list,
										accept_list, reject_list,
										eligible_mag, eligible_count);
	}


	// Construct a function which is defined by discrete values on a sequence of intervals.

	public static OEMagCompFnDisc makeDisc (double magCat, double[] mag_time_array, double mag_eps, double time_eps) {
		return new OEMagCompFnDisc (magCat, mag_time_array, mag_eps, time_eps);
	}

	public static OEMagCompFnDisc makeDisc (double magCat, double[] mag_time_array, double mag_eps, double time_eps,
							OEMagCompFnDisc.SplitFn split_fn, double[] tsplit_array, boolean[] dlbase_array) {
		return new OEMagCompFnDisc (magCat, mag_time_array, mag_eps, time_eps,
										split_fn, tsplit_array, dlbase_array);
	}


	// Construct a discrete function which is defined by F,G,H parameters for multiple earthquakes.

	public static OEMagCompFnDisc makeDiscFGH (double magCat, double capF, double capG, double capH,
											double t_range_begin, double t_range_end,
											Collection<OERupture> rup_list,
											Collection<OERupture> accept_list, Collection<OERupture> reject_list,
											double eligible_mag, int eligible_count,
											double mag_eps, double time_eps,
											double disc_base, double disc_delta, double disc_round, double disc_gap, OEMagCompFnDisc.SplitFn split_fn,
											int mag_cat_count, double division_mag, int division_count, double[] t_req_splits, int before_max_count) {
		return new OEMagCompFnDiscFGH (magCat, capF, capG, capH,
										t_range_begin, t_range_end,
										rup_list,
										accept_list, reject_list,
										eligible_mag, eligible_count,
										mag_eps, time_eps,
										disc_base, disc_delta, disc_round, disc_gap, split_fn,
										mag_cat_count, division_mag, division_count, t_req_splits, before_max_count);
	}


	// Construct a discrete function which is defined by F,G,H parameters for multiple earthquakes.

	public static OEMagCompFnDisc makeDiscFGH (OEDiscFGHParams params, Collection<OERupture> rup_list,
								Collection<OERupture> accept_list, Collection<OERupture> reject_list) {
		return new OEMagCompFnDiscFGH (params, rup_list,
										accept_list, reject_list);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 81001;

	private static final String M_VERSION_NAME = "OEMagCompFn";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 81000;
	protected static final int MARSHAL_CONSTANT = 82001;
	protected static final int MARSHAL_FGH = 83001;
	protected static final int MARSHAL_MULTIFGH = 89001;
	protected static final int MARSHAL_DISC = 90001;
	protected static final int MARSHAL_DISCFGH = 91001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected abstract int get_marshal_type ();

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		// <None>
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		// <None>

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

	public OEMagCompFn unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEMagCompFn obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static OEMagCompFn unmarshal_poly (MarshalReader reader, String name) {
		OEMagCompFn result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFn.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CONSTANT:
			result = new OEMagCompFnConstant();
			result.do_umarshal (reader);
			break;

		case MARSHAL_FGH:
			result = new OEMagCompFnFGH();
			result.do_umarshal (reader);
			break;

		case MARSHAL_MULTIFGH:
			result = new OEMagCompFnMultiFGH();
			result.do_umarshal (reader);
			break;

		case MARSHAL_DISC:
			result = new OEMagCompFnDisc();
			result.do_umarshal (reader);
			break;

		case MARSHAL_DISCFGH:
			result = new OEMagCompFnDiscFGH();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
