package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.List;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;				// natural log of 10
import static org.opensha.oaf.oetas.OEConstants.C_MILLIS_PER_DAY;		// milliseconds per day
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value


/**
 * Time-dependent magnitude of completeness function -- using F,G,H parameters for multiple earthquakes.
 * Author: Michael Barall 02/16/2020.
 *
 * This class represents a time-dependent magnitude of completeness function
 * governed by Helmstetter parameters F, G, and G.  Multiple earthquakes can contribute
 * to the incompleteness.  At any time, the magnitude of completeness is the maximum
 * of the Helmstetter function for any of the considered earthquakes.
 *
 * In addition, this class also selects the earthquakes that are above the time-dependent
 * magnitude of completeness.  This must be done concurrently with building the function,
 * since the function itself depends on the selected earthquakes.
 *
 * The Helmstetter function is clipped at the magnitude of the earthquake, so an
 * earthquake never blocks detection of a larger earthquake.  This is necessary for
 * the stability of the algorithms, and to avoid situations where a large earthquake
 * is discarded because a smaller earthquake occurs just before.
 *
 * In practice, the function would be built from only a few of the largest earthquakes.
 *
 * At present this is a place-holder class, to be implemented later.
 *
 * ==================================
 *
 * The Helmstetter formula for magnitude of completeness following an earthquake is:
 *
 * Mc(t) = F * M0 - G - H * log10(t - t0)
 *
 * Here the earthquake has magnitude M0 and occurs at time t0, and F, G, and H are parameters.
 *
 * Typical California parameters are F = 1.00, G = 4.50, H = 0.75.
 * Typical World parameters are F = 0.50, G = 0.25, H = 1.00.
 *
 * Let Mcat be the catalog magnitude of completeness at normal times.
 * The time-dependent magnitude of completeness is clipped below at Mcat, so implicitly:
 *
 * Mc(t) = max (F * M0 - G - H * log10(t - t0), Mcat)
 *
 * This formula equals Mcat for all time t >= tc, where the time of completeness tc is:
 *
 * tc = t0 + 10^((F * M0 - G - Mcat) / H)
 *
 * Also, the time-dependent magnitude of completeness is clipped above at M0, so implicitly:
 *
 * Mc(t) = min (max (F * M0 - G - H * log10(t - t0), Mcat), M0)
 *
 * This formula equals M0 for time t0 <= t <= tf, where the falloff time tf is:
 *
 * tf = t0 + 10^(((F - 1) * M0 - G) / H)
 *
 * When there are multiple earthquakes, the overall magnitude of completeness is taken to be
 * the maximum of the individual magnitudes of completeness.
 *
 * ==================================
 *
 * Abstract model.
 *
 * For efficient evaluation, the overall magnitude of completeness function is constructed
 * as a piecewise function, with a simple formula within each piece. To construct the maximum
 * of multiple functions, it is necessary to find function intersections, to partition time
 * into intervals where a single function is the maximum within each interval.  The algorithm
 * employed here is designed to work with functions that satisfy certain abstract properties.
 * 
 *
 *
 */
public class OEMagCompFnMultiFGH extends OEMagCompFn {

	//----- Parameters -----


	// Catalog magnitude of completeness.

	private double magCat;

	// Helmstetter Parameters.

	private double capF;
	private double capG;
	private double capH;

	// Beginning and end of the time range of interest, in days.

	private double t_range_begin;
	private double t_range_end;

	// Amount by which time range is expanded to allow for rounding errors, in days.

	private static final double T_RANGE_EXPAND = 1.0;




	//----- Data structures -----




	//----- Evaluation -----


	// Calculate the time-dependent magnitude of completeness.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the magnitude of completeness at the given time.
	// Note: This function does not define the origin of time.
	// Note: The returned value must be >= the value returned by get_mag_cat().
	// It is expected that as t_days becomes large, the returned value
	// equals or approaches the value returned by get_mag_cat()

	@Override
	public double get_mag_completeness (double t_days) {
		return magCat;
	}




	// Get the catalog magnitude of completeness.
	// Returns the magnitude of completeness in normal times.

	@Override
	public double get_mag_cat () {
		return magCat;
	}




	//----- Building -----




	// Nested class used during building to represent one rupture.

	protected static class BldRupture {

		// Rupture time, in days.

		public double b_t_day;

		// Rupture magnitude.

		public double b_rup_mag;

		// The time of completeness, when the magnitude of completeness reaches magCat; in days.

		public double b_t_comp;

		// The falloff time, when the magnitude of completeness falls below the rupture magnitude; in days.
		// Note: It is guaranteed that b_t_fall <= b_t_comp.

		public double b_t_fall;

		// Expiration time, when the magnitude of completeness falls below an earlier earthquake, in days.

		public double b_t_expire;
	}




	// Factory function to make a BldRupture, from an OERupture.

	protected BldRupture make_BldRupture (OERupture rup) {
		BldRupture bld_rup = new BldRupture ();
		bld_rup.b_t_day = rup.t_day;
		bld_rup.b_rup_mag = rup.rup_mag;

		bld_rup.b_t_comp = bld_rup.b_t_day + Math.pow (10.0, Math.min (LOG10_HUGE_TIME_DAYS, (capF * bld_rup.b_rup_mag - capG - magCat) / capH));
		bld_rup.b_t_fall = Math.min (bld_rup.b_t_comp, bld_rup.b_t_day + Math.pow (10.0, Math.min (LOG10_HUGE_TIME_DAYS, (capF * bld_rup.b_rup_mag - capG - bld_rup.b_rup_mag) / capH)));
		
		return bld_rup;
	}




	// Factory function to make a BldRupture, which evaluates to magCat.

	protected BldRupture make_BldRupture () {
		BldRupture bld_rup = new BldRupture ();
		bld_rup.b_t_day = -(HUGE_TIME_DAYS * 2.0);
		bld_rup.b_rup_mag = magCat;
		bld_rup.b_t_comp = (HUGE_TIME_DAYS * 2.0);
		bld_rup.b_t_fall = (HUGE_TIME_DAYS * 2.0);
		return bld_rup;
	}




	// Inner class to build the function.

	protected class MFGHBuilder {

		// List of active ruptures.
		// An active rupture has magnitude and expiration time both strictly greater than any later event.

		private BldRupture[] active;

		// Beginning and end+1 of the list of open ruptures witin the open_rup array.

		private int active_begin;
		private int active_end;
	



	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEMagCompFnMultiFGH () {}


	// Construct from given parameters.

	public OEMagCompFnMultiFGH (double magCat, double capF, double capG, double capH) {
		if (!( capH >= 0.0 )) {
			throw new IllegalArgumentException ("OEMagCompFnMultiFGH.OEMagCompFnMultiFGH: H parameter is negative: H = " + capH);
		}
		this.magCat = magCat;
		this.capF = capF;
		this.capG = capG;
		this.capH = capH;
	}


	// Display our contents

	@Override
	public String toString() {
		return "FnFGH[magCat=" + magCat
		+ ", capF=" + capF
		+ ", capG=" + capG
		+ ", capH=" + capH
		+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 89001;

	private static final String M_VERSION_NAME = "OEMagCompFnMultiFGH";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_MULTIFGH;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			writer.marshalDouble ("magCat", magCat);
			writer.marshalDouble ("capF", capF);
			writer.marshalDouble ("capG", capG);
			writer.marshalDouble ("capH", capH);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			magCat = reader.unmarshalDouble ("magCat");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");

			if (!( capH >= 0.0 )) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: H parameter is negative: H = " + capH);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			magCat = reader.unmarshalDouble ("magCat");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");

			if (!( capH >= 0.0 )) {
				throw new MarshalException ("OEMagCompFnMultiFGH.do_umarshal: H parameter is negative: H = " + capH);
			}
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
	public OEMagCompFnMultiFGH unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEMagCompFnMultiFGH obj) {

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

	public static OEMagCompFnMultiFGH unmarshal_poly (MarshalReader reader, String name) {
		OEMagCompFnMultiFGH result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFnMultiFGH.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_MULTIFGH:
			result = new OEMagCompFnMultiFGH();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
