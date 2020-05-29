package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Time-dependent magnitude of completeness function -- using F,G,H parameters.
 * Author: Michael Barall 02/16/2020.
 *
 * This class represents a time-dependent magnitude of completeness function
 * governed by Helmstetter parameters F, G, and G.
 *
 * At present this is a place-holder class, to be implemented later.
 */
public class OEMagCompFnFGH extends OEMagCompFn {

	//----- Parameters -----


	// Catalog magnitude of completeness.

	private double magCat;

	// Helmstetter Parameters.

	private double capF;
	private double capG;
	private double capH;




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




	//----- Construction -----


	// Default constructor does nothing.

	public OEMagCompFnFGH () {}


	// Construct from given parameters.

	public OEMagCompFnFGH (double magCat, double capF, double capG, double capH) {
		if (!( capH >= 0.0 )) {
			throw new IllegalArgumentException ("OEMagCompFnFGH.OEMagCompFnFGH: H parameter is negative: H = " + capH);
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
	private static final int MARSHAL_VER_1 = 83001;

	private static final String M_VERSION_NAME = "OEMagCompFnFGH";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_FGH;
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
			throw new MarshalException ("OEMagCompFnFGH.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			magCat = reader.unmarshalDouble ("magCat");
			capF = reader.unmarshalDouble ("capF");
			capG = reader.unmarshalDouble ("capG");
			capH = reader.unmarshalDouble ("capH");

			if (!( capH >= 0.0 )) {
				throw new MarshalException ("OEMagCompFnFGH.do_umarshal: H parameter is negative: H = " + capH);
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
				throw new MarshalException ("OEMagCompFnFGH.do_umarshal: H parameter is negative: H = " + capH);
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
	public OEMagCompFnFGH unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEMagCompFnFGH obj) {

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

	public static OEMagCompFnFGH unmarshal_poly (MarshalReader reader, String name) {
		OEMagCompFnFGH result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFnFGH.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_FGH:
			result = new OEMagCompFnFGH();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
