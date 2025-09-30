package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.commons.calc.magScalingRelations.magScalingRelImpl.WC1994_MagLengthRelationship;


/**
 * Magnitude-dependent search radius function -- Wells and Coppersmith with clipping.
 * Author: Michael Barall 07/27/2018.
 *
 * This class represents the Wells and Coppersmith (1994) median rupture length for
 * their "All" case (which has unspecified rake angle).  The WC radius is:
 *
 * 10^(0.69*mag - 3.22)
 *
 * This function also applies a multiplier to the Wells and Coppersmith value,
 * and imposes minimum and maximum values on the computed radius.
 *
 * Here is a little table showing the WC radius for various magnitudes:
 * mag	wc_radius
 * 4.0     0.3
 * 4.5     0.8
 * 5.0     1.7
 * 5.5     3.8
 * 6.0     8.3
 * 6.5    18.4
 * 7.0    40.7
 * 7.5    90.2
 * 8.0   199.5
 * 8.5   441.6
 * 9.0   977.2
 * 9.5  2162.7
 */
public class SearchRadiusFnWCClip extends SearchRadiusFn {

	//----- Parameters -----

	// Radius multiplier, 1.0 = Wells and Coppersmith.

	private double radiusMult;

	// Minimum radius, 0.0 means no minimum.

	private double radiusMin;

	// Maximum radius, 0.0 means no maximum.

	private double radiusMax;




	//----- Evaluation -----


	/**
	 * Calculate the magnitude-dependent search radius, in km.
	 * @param magMain = Magnitude of mainshock.
	 * @return
	 * Returns the magnitude-dependent search radius.
	 * The return value must be greater than 0.0, and <= MAX_RADIUS.
	 */
	@Override
	public double getRadius (double magMain) {

		// Get the Wells and Coppersmith radius

		WC1994_MagLengthRelationship wcMagLen = new WC1994_MagLengthRelationship();
		double wc_radius = wcMagLen.getMedianLength(magMain);

		// Get the effective maximum

		double eff_max = ((radiusMax > 1.0e-6 && radiusMax < MAX_RADIUS - 1.0e-3) ? radiusMax : MAX_RADIUS);

		// Apply the multiplier and clip to range

		return Math.min (eff_max, Math.max (Math.max (MIN_RADIUS, radiusMin), wc_radius * radiusMult));
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SearchRadiusFnWCClip () {}


	/**
	 * Construct from given parameters.
	 * @param radiusMult = Radius multiplier, 1.0 = Wells and Coppersmith.
	 * @param radiusMin = Minimum radius, 0.0 means no minimum.
	 * @param radiusMax = Maximum radius, 0.0 means no maximum.
	 */
	public SearchRadiusFnWCClip (double radiusMult, double radiusMin, double radiusMax) {
		if (!( radiusMult >= 0.0 )) {
			throw new IllegalArgumentException ("SearchRadiusFnWCClip.SearchRadiusFnWCClip: radiusMult parameter is negative: radiusMult = " + radiusMult);
		}
		this.radiusMult = radiusMult;
		this.radiusMin = radiusMin;
		this.radiusMax = radiusMax;
	}


	// Display our contents

	@Override
	public String toString() {
		return "FnWCClip[radiusMult=" + radiusMult
		+ ", radiusMin=" + radiusMin
		+ ", radiusMax=" + radiusMax
		+ "]";
	}




	//----- Legacy methods -----


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of search radius multiplier.
	 * If the function can be represented in "legacy" format, return the legacy radius.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before SearchRadiusFn was created.
	 * The "legacy" format has only a multiple of the Wells and Coppersmith radius.
	 */
	@Override
	public double getLegacyRadius () {
		if ((radiusMax > 1.0e-6 && radiusMax < MAX_RADIUS - 1.0e-3) || radiusMin > 1.0e-6) {
			throw new MarshalException ("SearchRadiusFnWCClip.getLegacyRadius: Function is not of legacy format.");
		}
		return radiusMult;
	}




	//----- Support methods -----


	// Get the default Wells and Coppersmith radius multiplier value to be inserted into the GUI.
	// The value is coerced to lie between 0.0 and 100.0.

	@Override
	public double getDefaultGUIRadiusMult () {
		if (radiusMult <= 1.0e-6) {
			return 0.0;
		}
		return Math.min (100.0, radiusMult);
	}


	// Get the default minimum radius value to be inserted into the GUI.
	// The value is coerced to lie between 0.0 and 20000.0.

	@Override
	public double getDefaultGUIRadiusMin () {
		if (radiusMin <= MIN_RADIUS) {
			return 0.0;
		}
		return Math.min (MAX_RADIUS, radiusMin);
	}


	// Get the default maximum radius value to be inserted into the GUI.
	// The value is coerced to lie between 0.0 and 20000.0.

	@Override
	public double getDefaultGUIRadiusMax () {
		if (radiusMax <= 1.0e-6) {
			return 0.0;
		}
		return Math.min (MAX_RADIUS, radiusMax);
	}


	// Return true if the GUI should regard this function as constant.
	// If the return is true, then getDefaultGUIRadiusMin() retrieves the constant.

	@Override
	public boolean getDefaultGUIIsConstant () {
		return (radiusMult <= 1.0e-6);
	}


	// Return true if the GUI should regard the other function as being the same as this function.

	@Override
	public boolean getDefaultGUIIsEqual (SearchRadiusFn other) {

		// If the other function is a different type, return true if they are both constants

		if (!( other instanceof SearchRadiusFnWCClip )) {
			return (
				getDefaultGUIIsConstant()
				&& other.getDefaultGUIIsConstant()
				&& Math.abs (getDefaultGUIRadiusMin() - other.getDefaultGUIRadiusMin()) <= 1.0e-2
			);
		}

		// Compare two functions using effective multiplier, effective max, and effective min

		SearchRadiusFnWCClip x = (SearchRadiusFnWCClip)other;

		double eff_mult_1 = ( (radiusMult <= 1.0e-6) ? 0.0 : Math.min (100.0, radiusMult) );
		double eff_mult_2 = ( (x.radiusMult <= 1.0e-6) ? 0.0 : Math.min (100.0, x.radiusMult) );
		if (!( Math.abs (eff_mult_1 - eff_mult_2) <= 1.0e-5 )) {
			return false;
		}

		double eff_max_1 = ((radiusMax > 1.0e-6 && radiusMax < MAX_RADIUS - 1.0e-3) ? radiusMax : MAX_RADIUS);
		double eff_max_2 = ((x.radiusMax > 1.0e-6 && x.radiusMax < MAX_RADIUS - 1.0e-3) ? x.radiusMax : MAX_RADIUS);
		if (!( Math.abs (eff_max_1 - eff_max_2) <= 1.0e-2 )) {
			return false;
		}

		double eff_min_1 = Math.max (MIN_RADIUS, radiusMin);
		double eff_min_2 = Math.max (MIN_RADIUS, x.radiusMin);
		if (!( Math.abs (eff_min_1 - eff_min_2) <= 1.0e-2 )) {
			return false;
		}

		return true;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 67001;

	private static final String M_VERSION_NAME = "SearchRadiusFnWCClip";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_WC_CLIP;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		writer.marshalDouble ("radiusMult", radiusMult);
		writer.marshalDouble ("radiusMin" , radiusMin );
		writer.marshalDouble ("radiusMax" , radiusMax );

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("SearchRadiusFnWCClip.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			radiusMult = reader.unmarshalDouble ("radiusMult");
			radiusMin  = reader.unmarshalDouble ("radiusMin" );
			radiusMax  = reader.unmarshalDouble ("radiusMax" );

			if (!( radiusMult >= 0.0 )) {
				throw new MarshalException ("SearchRadiusFnWCClip.do_umarshal: radiusMult parameter is negative: radiusMult = " + radiusMult);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			radiusMult = reader.unmarshalDouble ("radiusMult");
			radiusMin  = reader.unmarshalDouble ("radiusMin" );
			radiusMax  = reader.unmarshalDouble ("radiusMax" );

			if (!( radiusMult >= 0.0 )) {
				throw new MarshalException ("SearchRadiusFnWCClip.do_umarshal: radiusMult parameter is negative: radiusMult = " + radiusMult);
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
	public SearchRadiusFnWCClip unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SearchRadiusFnWCClip obj) {

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

	public static SearchRadiusFnWCClip unmarshal_poly (MarshalReader reader, String name) {
		SearchRadiusFnWCClip result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SearchRadiusFnWCClip.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_WC_CLIP:
			result = new SearchRadiusFnWCClip();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
