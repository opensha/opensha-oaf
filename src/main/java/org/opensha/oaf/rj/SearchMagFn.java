package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.comcat.ComcatOAFAccessor;


/**
 * Magnitude-dependent search magnitude function.
 * Author: Michael Barall 07/27/2018.
 *
 * This abstract class represents a magnitude-dependent search magnitude,
 * which is used to search for aftershocks.
 *
 * Objects of this class are immutable and pure (have no internal state).
 */
public abstract class SearchMagFn {

	//----- Evaluation -----


	// The magnitude value that indicates no lower limit (values < -9.0 mean no lower limit).

	public static final double NO_MIN_MAG = ComcatOAFAccessor.COMCAT_NO_MIN_MAG;	// = -10.0

	// Magnitudes less than this mean no lower limit.

	public static final double NO_MIN_MAG_TEST = -9.0;

	// The magnitude value that indicates to skip the centroid search (values > 9.9 mean no centroid search).

	public static final double SKIP_CENTROID = 10.0;

	// Magnitudes greater than this indicate to skip the centroid search.

	public static final double SKIP_CENTROID_TEST = 9.9;


	/**
	 * Calculate the magnitude-dependent search magnitude.
	 * @param magMain = Magnitude of mainshock.
	 * @return
	 * Returns the magnitude-dependent search magnitude.
	 * A return value <= NO_MIN_MAG means no lower limit (not recommended).
	 */
	public abstract double getMag (double magMain);


	// Make a new function, taking into account an analyst-supplied magCat.
	// Parameters:
	//  magCat = Analyst-supplied magCat.
	// The purpose is to handle cases where an analyst has reduced magCat to
	// below the predefined search magnitude.
	// The return value can be this object, if it is suitable.

	public abstract SearchMagFn makeForAnalystMagCat (double magCat);


	// Make a new function, with any minimum magnitude removed.
	// The return value can be this object, if it is suitable.
	// If this object returns the skip centroid special value,
	// then the returned object should also return the skip centroid
	// special value.
	// The purpose is for the GUI to be able to retrieve all aftershocks.

	public abstract SearchMagFn makeRemovedMinMag ();


	// Make a new function, with any minimum magnitude adjusted or removed.
	// The return value can be this object, if it is suitable.
	// If this object returns the skip centroid special value,
	// then the returned object should also return the skip centroid
	// special value.
	// If min_mag is NO_MIN_MAG, then remove any minimum magnitude,
	// the same as makeRemovedMinMag with no arguments.  Otherwise,
	// set the minimum magnitude to min_mag.
	// The purpose is for the GUI to be able to retrieve all aftershocks
	// with magnitude >= min_mag.

	public abstract SearchMagFn makeRemovedMinMag (double min_mag);





	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SearchMagFn () {}


	// Display our contents

	@Override
	public String toString() {
		return "SearchMagFn";
	}




	//----- Factory methods -----


	/**
	 * Construct a function which is a constant, with a floor below the mainshock magnitude.
	 * @param mag = Constant magnitude.
	 * @param deltaMax = Minimum radius, 0.0 means no minimum.
	 */
	public static SearchMagFn makeFloor (double mag, double deltaMax) {
		return new SearchMagFnFloor (mag, deltaMax);
	}


	/**
	 * Construct a function which is a constant.
	 * @param mag = Constant magnitude.
	 */
	public static SearchMagFn makeFloor (double mag) {
		return new SearchMagFnConstant (mag);
	}


	/**
	 * Construct a function which is a constant, and returns no minimum magnitude.
	 */
	public static SearchMagFn makeNoMinMag () {
		return new SearchMagFnConstant (NO_MIN_MAG);
	}


	/**
	 * Construct a function which is a constant.
	 * If mag is NO_MIN_MAG, the function returns no minimum magnitude,
	 * the same as makeNoMinMag with no argument.  Otherwise, the function
	 * returns the constant magnitude mag.
	 */
	public static SearchMagFn makeNoMinMag (double mag) {
		if (mag < NO_MIN_MAG_TEST) {
			return new SearchMagFnConstant (NO_MIN_MAG);
		}
		return new SearchMagFnConstant (mag);
	}


	/**
	 * Construct a function which is a constant, and returns to skip the centroid calculation.
	 */
	public static SearchMagFn makeSkipCentroid () {
		return new SearchMagFnConstant (SKIP_CENTROID);
	}




	//----- Legacy methods -----


	/**
	 * [DEPRECATED]
	 * Construct a function which is a constant.
	 * @param mag = Constant magnitude.
	 * This is for unmarshaling "legacy" files that were written before SearchMagFn was created.
	 */
	public static SearchMagFn makeLegacyMag (double mag) {
		return new SearchMagFnConstant (mag);
	}


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of search magnitude.
	 * If the function can be represented in "legacy" format, return the legacy magnitude.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before SearchMagFn was created.
	 * The "legacy" format has a constant magnitude.
	 */
	public double getLegacyMag () {
		throw new MarshalException ("SearchMagFn.getLegacyRadius: Function is not of legacy format.");
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 68001;

	private static final String M_VERSION_NAME = "SearchMagFn";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 68000;
	protected static final int MARSHAL_FLOOR = 69001;
	protected static final int MARSHAL_CONSTANT = 70001;

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

	public SearchMagFn unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SearchMagFn obj) {

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

	public static SearchMagFn unmarshal_poly (MarshalReader reader, String name) {
		SearchMagFn result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SearchMagFn.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_FLOOR:
			result = new SearchMagFnFloor();
			result.do_umarshal (reader);
			break;

		case MARSHAL_CONSTANT:
			result = new SearchMagFnConstant();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
