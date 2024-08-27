package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Bayesian prior factory function, for operational ETAS - Fixed.
// Author: Michael Barall 08/26/2024.
//
// This factory function supplies a fixed Bayesian prior function, specified in the constructor.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied parameters.

public class OEBayFactoryFixed extends OEBayFactory {

	//----- Parameters -----

	// Our Bayesian prior function.

	private OEBayPrior bay_prior;




	//----- Creation -----


	// Create a Bayesian prior function.
	// Parameters:
	//  factory_params = Parameters passed in to the Bayesian prior factory.
	// Returns the Bayesian prior function.
	// Threading: This function may be called simultaneously by multiple threads
	// (although in practice it is called by a single thread at the start of a calculation;
	// the resulting Bayesian prior function can then be used by multiple threads.)

	@Override
	public OEBayPrior make_bay_prior (
		OEBayFactoryParams factory_params
	) {
		return bay_prior;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEBayFactoryFixed () {
		this.bay_prior = null;
	}


	// Construct from given parameters.
	
	public OEBayFactoryFixed (OEBayPrior bay_prior) {
		this.bay_prior = bay_prior;
	}


	// Display our contents

	@Override
	public String toString() {
		return "OEBayFactoryFixed";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 138001;

	private static final String M_VERSION_NAME = "OEBayFactoryFixed";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_FIXED;
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

			OEBayPrior.marshal_poly (writer, "bay_prior", bay_prior);

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
			throw new MarshalException ("OEBayFactoryFixed.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			bay_prior = OEBayPrior.unmarshal_poly (reader, "bay_prior");

		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			bay_prior = OEBayPrior.unmarshal_poly (reader, "bay_prior");

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
	public OEBayFactoryFixed unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayFactoryFixed obj) {

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

	public static OEBayFactoryFixed unmarshal_poly (MarshalReader reader, String name) {
		OEBayFactoryFixed result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayFactoryFixed.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_FIXED:
			result = new OEBayFactoryFixed();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
