package org.opensha.oaf.rj;

import java.util.Map;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;


/**
 * Time-dependent magnitude of completeness function -- Page et al. (2016).
 * Author: Michael Barall 11/09/2018.
 *
 * This class represents the Page et al. (2016) time-dependent magnitude of
 * completeness function.  The magnitude of completeness is:
 *
 *  magComp(t) = Max (F*magMain - G - H*log10(t), magCat)
 *
 * where t is the time in days since the mainshock, and F, G, and H are parameters.
 * The corresponding time of completeness is:
 *
 *  tComp = 10^((F*magMain - G - magCat)/H)
 *
 * However this must be evaluated carefully to avoid overflow or divide-by-zero.
 */
public class MagCompFnPage extends MagCompFn {

	//----- Parameters -----

	// Page et al. parameters.

	private double capF;
	private double capG;
	private double capH;




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

		return Math.max (magCat, capF * magMain - capG - capH * Math.log10 (Math.max (tDays, Double.MIN_NORMAL)));
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

		double x = capF * magMain - capG - magCat;

		if (x <= -8.0 * capH) {
			return 0.0;			// less than about 1 millisecond, just return 0
		}
		if (x >= 12.0 * capH) {
			return 1.0e12;		// more than about 3 billion years
		}

		return Math.pow(10.0, x/capH);
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public MagCompFnPage () {}


	/**
	 * Construct from given parameters.
	 * @param capG = The "F" parameter in the time-dependent magnitude of completeness model. 
	 * @param capG = The "G" parameter in the time-dependent magnitude of completeness model. 
	 * @param capH = The "H" parameter in the time-dependent magnitude of completeness model.
	 */
	public MagCompFnPage (double capF, double capG, double capH) {
		if (!( capH >= 0.0 )) {
			throw new IllegalArgumentException ("MagCompFnPage.MagCompFnPage: H parameter is negative: H = " + capH);
		}
		this.capF = capF;
		this.capG = capG;
		this.capH = capH;
	}


	// Display our contents

	@Override
	public String toString() {
		return "FnPage[capF=" + capF
		+ ", capG=" + capG
		+ ", capH=" + capH
		+ "]";
	}


	// Get parameters that can be displayed to the user.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	//  magCat = Magnitude of completeness when there has not been a mainshock.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.

	@Override
	public void get_display_params (Map<String, Object> userParamMap, double magCat) {
		userParamMap.put ("Mcat", SimpleUtils.round_double_via_string ("%.2f", magCat));
		userParamMap.put ("F", capF);
		userParamMap.put ("G", capG);
		userParamMap.put ("H", capH);
		return;
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
		if (!( Math.abs(capF - 0.5) < 1.0e-12 && capG <= 9.999 )) {
			throw new MarshalException ("MagCompFnPage.getLegacyCapG: Function is not of legacy format.");
		}
		return capG;
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
		if (!( Math.abs(capF - 0.5) < 1.0e-12 && capG <= 9.999 )) {
			throw new MarshalException ("MagCompFnPage.getLegacyCapH: Function is not of legacy format.");
		}
		return capH;
	}




	//----- Support methods -----


	// Get the default capF value to be inserted into the GUI.

	@Override
	public double getDefaultGUICapF () {
		return capF;
	}


	// Get the default capG value to be inserted into the GUI.

	@Override
	public double getDefaultGUICapG () {
		return capG;
	}


	// Get the default capH value to be inserted into the GUI.

	@Override
	public double getDefaultGUICapH () {
		return capH;
	}


	// Return true if the function is a constant, always equal to magCat.

	@Override
	public boolean is_constant () {
		return false;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 53001;

	private static final String M_VERSION_NAME = "MagCompFnPage";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_PAGE;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		writer.marshalDouble ("capF", capF);
		writer.marshalDouble ("capG", capG);
		writer.marshalDouble ("capH", capH);

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("MagCompFnPage.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");

			if (!( capH >= 0.0 )) {
				throw new MarshalException ("MagCompFnPage.do_umarshal: H parameter is negative: H = " + capH);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");

			if (!( capH >= 0.0 )) {
				throw new MarshalException ("MagCompFnPage.do_umarshal: H parameter is negative: H = " + capH);
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
	public MagCompFnPage unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, MagCompFnPage obj) {

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

	public static MagCompFnPage unmarshal_poly (MarshalReader reader, String name) {
		MagCompFnPage result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("MagCompFnPage.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
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
