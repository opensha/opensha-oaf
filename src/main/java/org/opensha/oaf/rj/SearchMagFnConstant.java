package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Magnitude-dependent search magnitude function -- Constant value.
 * Author: Michael Barall 07/27/2018.
 *
 * This class represents a magnitude that is constant.
 * This is useful for special values (-10.0 for no minimum magnitude in Comcat searches,
 * 10.0 to bypass the centroid search.)
 *
 * Objects of this class are immutable and pure (have no internal state).
 */
public class SearchMagFnConstant extends SearchMagFn {

	//----- Parameters -----

	// Constant magnitude.

	private double mag;




	//----- Evaluation -----


	/**
	 * Calculate the magnitude-dependent search magnitude.
	 * @param magMain = Magnitude of mainshock.
	 * @return
	 * Returns the magnitude-dependent search magnitude.
	 * A return value <= NO_MIN_MAG means no lower limit (not recommended).
	 */
	@Override
	public double getMag (double magMain) {
		return mag;
	}


	// Make a new function, taking into account an analyst-supplied magCat.
	// Parameters:
	//  magCat = Analyst-supplied magCat.
	// The purpose is to handle cases where an analyst has reduced magCat to
	// below the predefined search magnitude.
	// The return value can be this object, if it is suitable.

	@Override
	public SearchMagFn makeForAnalystMagCat (double magCat) {
		if (mag < NO_MIN_MAG_TEST || mag > SKIP_CENTROID_TEST) {
			return this;	// no change if magnitude has one of the special values
		}
		return new SearchMagFnConstant (Math.min (mag, magCat));
	}


	// Make a new function, with any minimum magnitude removed.
	// The return value can be this object, if it is suitable.
	// If this object returns the skip centroid special value,
	// then the returned object should also return the skip centroid
	// special value.
	// The purpose is for the GUI to be able to retrieve all aftershocks.

	@Override
	public SearchMagFn makeRemovedMinMag () {
		if (mag < NO_MIN_MAG_TEST || mag > SKIP_CENTROID_TEST) {
			return this;	// no change if magnitude has one of the special values
		}
		return makeNoMinMag();
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SearchMagFnConstant () {}


	/**
	 * Construct from given parameters.
	 * @param mag = Constant magnitude.
	 */
	public SearchMagFnConstant (double mag) {
		this.mag = mag;
	}


	// Display our contents

	@Override
	public String toString() {
		return "FnConstant[mag=" + mag
		+ "]";
	}




	//----- Legacy methods -----


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of search magnitude.
	 * If the function can be represented in "legacy" format, return the legacy magnitude.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before SearchMagFn was created.
	 * The "legacy" format has a constant magnitude.
	 */
	@Override
	public double getLegacyMag () {
		return mag;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 70001;

	private static final String M_VERSION_NAME = "SearchMagFnConstant";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_CONSTANT;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		writer.marshalDouble ("mag"       , mag       );

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("SearchMagFnConstant.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			mag        = reader.unmarshalDouble ("mag"       );
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			mag        = reader.unmarshalDouble ("mag"       );
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
	public SearchMagFnConstant unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SearchMagFnConstant obj) {

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

	public static SearchMagFnConstant unmarshal_poly (MarshalReader reader, String name) {
		SearchMagFnConstant result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SearchMagFnConstant.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
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
