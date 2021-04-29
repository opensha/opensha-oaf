package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.comcat.ComcatOAFAccessor;


/**
 * Magnitude-dependent search radius function.
 * Author: Michael Barall 07/27/2018.
 *
 * This abstract class represents a magnitude-dependent search radius,
 * which is used to search for aftershocks.
 */
public abstract class SearchRadiusFn {

	//----- Evaluation -----


	// The maximum allowed search radius (this is a Comcat limit).

	public static final double MAX_RADIUS = 20000.0;


	// The minimum allowed search radius (recommended).

	public static final double MIN_RADIUS = 1.0e-6;


	/**
	 * Calculate the magnitude-dependent search radius, in km.
	 * @param magMain = Magnitude of mainshock.
	 * @return
	 * Returns the magnitude-dependent search radius.
	 * The return value must be greater than 0.0, and <= MAX_RADIUS.
	 */
	public abstract double getRadius (double magMain);





	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SearchRadiusFn () {}


	// Display our contents

	@Override
	public String toString() {
		return "SearchRadiusFn";
	}




	//----- Factory methods -----


	/**
	 * Construct a function which is a clipped multiple of the Wells and Coppersmith radius.
	 * @param radiusMult = Radius multiplier, 1.0 = Wells and Coppersmith.
	 * @param radiusMin = Minimum radius, 0.0 means no minimum.
	 * @param radiusMax = Maximum radius, 0.0 means no maximum.
	 */
	public static SearchRadiusFn makeWCClip (double radiusMult, double radiusMin, double radiusMax) {
		return new SearchRadiusFnWCClip (radiusMult, radiusMin, radiusMax);
	}


	/**
	 * Construct a function which is a multiple of the Wells and Coppersmith radius.
	 * @param radiusMult = Radius multiplier, 1.0 = Wells and Coppersmith.
	 */
	public static SearchRadiusFn makeWC (double radiusMult) {
		return new SearchRadiusFnWCClip (radiusMult, 0.0, 0.0);
	}


	/**
	 * Construct a function which is a constant radius.
	 * @param radius = Radius.
	 */
	public static SearchRadiusFn makeConstant (double radius) {
		return new SearchRadiusFnWCClip (0.0, radius, radius);
	}




	//----- Legacy methods -----


	/**
	 * [DEPRECATED]
	 * Construct a function which is a multiple of the Wells and Coppersmith radius.
	 * @param radiusMult = Radius multiplier, 1.0 = Wells and Coppersmith. 
	 * This is for unmarshaling "legacy" files that were written before SearchRadiusFn was created.
	 */
	public static SearchRadiusFn makeLegacyWC (double radiusMult) {
		return new SearchRadiusFnWCClip (radiusMult, 0.0, 0.0);
	}


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of search radius multiplier.
	 * If the function can be represented in "legacy" format, return the legacy radius.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before SearchRadiusFn was created.
	 * The "legacy" format has only a multiple of the Wells and Coppersmith radius.
	 */
	public double getLegacyRadius () {
		throw new MarshalException ("SearchRadiusFn.getLegacyRadius: Function is not of legacy format.");
	}




	//----- Support methods -----


	// Get the default Wells and Coppersmith radius multiplier value to be inserted into the GUI.

	public double getDefaultGUIRadiusMult () {
		return 1.0;
	}


	// Get the default minimum radius value to be inserted into the GUI.

	public double getDefaultGUIRadiusMin () {
		return 0.0;
	}


	// Get the default maximum radius value to be inserted into the GUI.

	public double getDefaultGUIRadiusMax () {
		return 0.0;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 66001;

	private static final String M_VERSION_NAME = "SearchRadiusFn";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 66000;
	protected static final int MARSHAL_WC_CLIP = 67001;

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

	public SearchRadiusFn unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SearchRadiusFn obj) {

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

	public static SearchRadiusFn unmarshal_poly (MarshalReader reader, String name) {
		SearchRadiusFn result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SearchRadiusFn.unmarshal_poly: Unknown class type code: type = " + type);

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
