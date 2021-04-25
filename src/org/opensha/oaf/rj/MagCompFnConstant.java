package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Time-dependent magnitude of completeness function -- Constant.
 * Author: Michael Barall 11/09/2018.
 *
 * This class represents a constant time-dependent magnitude of completeness function.
 * The magnitude of completeness is always magCat.
 * The time of completeness is zero.
 */
public class MagCompFnConstant extends MagCompFn {

	//----- Evaluation -----

	/**
	 * Calculate the time-dependent magnitude of completeness.
	 * @param magMain = Magnitude of mainshock.
	 * @param magCat = Magnitude of completeness when there has not been a mainshock.
	 * @param tDays = Time (since origin time), in days.
	 * @return
	 * Returns the time-dependent magnitude of completeness.
	 */
	@Override
	public double getMagCompleteness (double magMain, double magCat, double tDays) {

		return magCat;
	}




	/**
	 * Calculate the time of completeness, in days since the mainshock.
	 * @param magMain = Magnitude of mainshock.
	 * @param magCat = Magnitude of completeness when there has not been a mainshock.
	 * @return
	 * Returns the time, in days, beyond which the magnitude of completeness equals magCat.
	 * Returns a very large value if there is no such time.
	 * Returns zero if the magnitude of completeness is always magCat.
	 */
	@Override
	public double getTimeOfCompleteness (double magMain, double magCat) {

		return 0.0;
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public MagCompFnConstant () {}


	// Display our contents

	@Override
	public String toString() {
		return "FnConstant";
	}




	//----- Legacy methods -----


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of capG.
	 * If the function can be represented in "legacy" format, return the legacy capG.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before MagCompFn was created.
	 * The "legacy" format is the Page et al. (2016) function, with capF = 0.5, and with
	 * capG == 10.0 to indicate a constant value equal to magCat.
	 */
	@Override
	public double getLegacyCapG () {
		return 10.0;
	}


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of capH.
	 * If the function can be represented in "legacy" format, return the legacy capG.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before MagCompFn was created.
	 * The "legacy" format is the Page et al. (2016) function, with capF = 0.5, and with
	 * capG == 10.0 to indicate a constant value equal to magCat.
	 */
	@Override
	public double getLegacyCapH () {
		return 0.0;
	}




	//----- Support methods -----


	// Get the default capF value to be inserted into the GUI.

	@Override
	public double getDefaultGUICapF () {
		return 0.0;
	}


	// Get the default capG value to be inserted into the GUI.

	@Override
	public double getDefaultGUICapG () {
		return 100.0;
	}


	// Get the default capH value to be inserted into the GUI.

	@Override
	public double getDefaultGUICapH () {
		return 0.0;
	}


	// Return true if the function is a constant, always equal to magCat.

	@Override
	public boolean is_constant () {
		return true;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 52001;

	private static final String M_VERSION_NAME = "MagCompFnConstant";

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

		// <None>

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("MagCompFnConstant.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			// <None>
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			// <None>
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
	public MagCompFnConstant unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, MagCompFnConstant obj) {

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

	public static MagCompFnConstant unmarshal_poly (MarshalReader reader, String name) {
		MagCompFnConstant result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("MagCompFnConstant.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CONSTANT:
			result = new MagCompFnConstant();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
