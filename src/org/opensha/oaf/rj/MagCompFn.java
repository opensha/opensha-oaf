package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Time-dependent magnitude of completeness function.
 * Author: Michael Barall 11/09/2018.
 *
 * This abstract class represents a time-dependent magnitude of completeness
 * function.  It is time-dependent because, immediately following an earthquake,
 * the catalog's magnitude of completeness is higher than in normal times.
 *
 * Objects of this class, and its subclasses, are immutable and stateless.
 * They are pure functions, which means that their outputs depend only on the
 * supplied values of tDays (time since the origin in days), magMain (mainshock
 * magnitude), and magCat (magnitude of completeness in normal times).
 *
 * There are methods to calculate magComp (the magnitude of completeness) and
 * tComp (the time of completeness).  Considered as a function of time, magComp
 * must satisfy the following conditions:
 *
 * 1. magComp is a continuous, monotone decreasing function.
 *
 * 2. For times t >= tComp, magComp equals magCat.  (If magComp never equals magCat,
 * say, because it approaches magCat exponentially, it is acceptable to set tComp
 * equal to a very large value.)
 *
 * 3. For times epsilon < t < tComp, magComp is a smooth function with derivatives
 * of all orders.  Here, epsilon is some very small value, typically the smallest
 * positive normalized floating-point value.  (The purpose of this rule is to ensure
 * that numerical integration of the aftershock rate is well-behaved.)
 *
 * 4. For times 0 <= t <= epsilon, magComp is constant and equal to its maximum
 * value.  (Note that magComp must have a finite value at t == 0).
 *
 * Considered as a function of magCat, magComp must satisfy the following condition:
 *
 * 5. If magCat1 > magCat2, then magComp(magCat1) = max(magComp(magCat2), magCat1).
 * (The purpose of this rule is to properly account for cases where earthquakes
 * detected by the seismic network are not available from Comcat unless they
 * exceed a magnitude threshold (which could be our Comcat search magnitude).
 * This is most easily achieved by choosing magComp to have the form
 * magComp(magMain, magCat, tDays) = max(F(magMain, tDays), magCat)
 * where F is some function that depends only on magMain and tDays.)
 */
public abstract class MagCompFn {

	//----- Evaluation -----

	/**
	 * Calculate the time-dependent magnitude of completeness.
	 * @param magMain = Magnitude of mainshock.
	 * @param magCat = Magnitude of completeness when there has not been a mainshock.
	 * @param tDays = Time (since origin time), in days.
	 * @return
	 * Returns the time-dependent magnitude of completeness.
	 */
	public abstract double getMagCompleteness (double magMain, double magCat, double tDays);




	/**
	 * Calculate the time of completeness, in days since the mainshock.
	 * @param magMain = Magnitude of mainshock.
	 * @param magCat = Magnitude of completeness when there has not been a mainshock.
	 * @return
	 * Returns the time, in days, beyond which the magnitude of completeness equals magCat.
	 * Returns a very large value if there is no such time.
	 * Returns zero if the magnitude of completeness is always magCat.
	 */
	public abstract double getTimeOfCompleteness (double magMain, double magCat);




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public MagCompFn () {}

	// Display our contents

	@Override
	public String toString() {
		return "MagCompFn";
	}




	//----- Factory methods -----


	/**
	 * Construct a function which is constant, equal to magCat.
	 */
	public static MagCompFn makeConstant () {
		return new MagCompFnConstant ();
	}


	/**
	 * Construct a function which is the Page et al. (2016) magnitude of completeness.
	 * @param capG = The "F" parameter in the time-dependent magnitude of completeness model. 
	 * @param capG = The "G" parameter in the time-dependent magnitude of completeness model. 
	 * @param capH = The "H" parameter in the time-dependent magnitude of completeness model.
	 */
	public static MagCompFn makePage (double capF, double capG, double capH) {
		return new MagCompFnPage (capF, capG, capH);
	}


	/**
	 * Construct a function which is the Page et al. (2016) magnitude of completeness, or a constant.
	 * @param capF = The "F" parameter in the time-dependent magnitude of completeness model. 
	 * @param capG = The "G" parameter in the time-dependent magnitude of completeness model. 
	 * @param capH = The "H" parameter in the time-dependent magnitude of completeness model.
	 * If capG = 100.0, then return a constant function whose value is always magCat.
	 * Otherwise, return the Page et al. time-dependent function.
	 */
	public static MagCompFn makePageOrConstant (double capF, double capG, double capH) {
		if (capG > 99.999) {
			return new MagCompFnConstant ();
		}
		return new MagCompFnPage (capF, capG, capH);
	}




	//----- Legacy methods -----


	/**
	 * [DEPRECATED]
	 * Construct a function which is the Page et al. (2016) magnitude of completeness, or a constant.
	 * @param capG = The "G" parameter in the time-dependent magnitude of completeness model. 
	 * @param capH = The "H" parameter in the time-dependent magnitude of completeness model.
	 * This is for unmarshaling "legacy" files that were written before MagCompFn was created.
	 * If capG = 10.0, then return a constant function whose value is always magCat.
	 * Otherwise, return the Page et al. time-dependent function, with capF = 0.5.
	 */
	public static MagCompFn makeLegacyPage (double capG, double capH) {
		if (capG > 9.999) {
			return new MagCompFnConstant ();
		}
		double capF = 0.5;
		return new MagCompFnPage (capF, capG, capH);
	}


	/**
	 * [DEPRECATED]
	 * Get the "legacy" value of capG.
	 * If the function can be represented in "legacy" format, return the legacy capG.
	 * Otherwise, throw an exception.
	 * This is for marshaling "legacy" files in formats used before MagCompFn was created.
	 * The "legacy" format is the Page et al. (2016) function, with capF = 0.5, and with
	 * capG == 10.0 to indicate a constant value equal to magCat.
	 */
	public double getLegacyCapG () {
		throw new MarshalException ("MagCompFn.getLegacyCapG: Function is not of legacy format.");
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
	public double getLegacyCapH () {
		throw new MarshalException ("MagCompFn.getLegacyCapH: Function is not of legacy format.");
	}




	//----- Support methods -----


	// Get the default capF value to be inserted into the GUI.

	public double getDefaultGUICapF () {
		return 0.5;
	}


	// Get the default capG value to be inserted into the GUI.

	public double getDefaultGUICapG () {
		return 0.25;
	}


	// Get the default capH value to be inserted into the GUI.

	public double getDefaultGUICapH () {
		return 1.0;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 51001;

	private static final String M_VERSION_NAME = "MagCompFn";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 51000;
	protected static final int MARSHAL_CONSTANT = 52001;
	protected static final int MARSHAL_PAGE = 53001;

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

	public MagCompFn unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, MagCompFn obj) {

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

	public static MagCompFn unmarshal_poly (MarshalReader reader, String name) {
		MagCompFn result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("MagCompFn.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CONSTANT:
			result = new MagCompFnConstant();
			result.do_umarshal (reader);
			break;

		case MARSHAL_PAGE:
			result = new MagCompFnPage();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
