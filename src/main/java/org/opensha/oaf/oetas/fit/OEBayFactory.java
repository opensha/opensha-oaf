package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.commons.geo.Location;


// Bayesian prior factory function, for operational ETAS.
// Author: Michael Barall 08/26/2024.
//
// This abstract class represents a Bayesian prior factory function.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied parameters.

public abstract class OEBayFactory implements Marshalable {


	//----- Creation -----


	// Create a Bayesian prior function.
	// Parameters:
	//  factory_params = Parameters passed in to the Bayesian prior factory.
	// Returns the Bayesian prior function.
	// Threading: This function may be called simultaneously by multiple threads
	// (although in practice it is called by a single thread at the start of a calculation;
	// the resulting Bayesian prior function can then be used by multiple threads.)

	public abstract OEBayPrior make_bay_prior (
		OEBayFactoryParams factory_params
	);




	//----- Construction -----


	// Default constructor does nothing.

	public OEBayFactory () {}


	// Display our contents

	@Override
	public String toString() {
		return "OEBayFactory";
	}




	//----- Factory methods -----


	// Construct a factory that returns a function with constant density.

	public static OEBayFactory makeUniform () {
		return new OEBayFactoryUniform ();
	}


	// Construct a factory that returns a fixed function.

	public static OEBayFactory makeFixed (OEBayPrior bay_prior) {
		return new OEBayFactoryFixed (bay_prior);
	}


	// Construct a factory for a Gauss a/p/c prior, using mainshock location to select parameters.

	public static OEBayFactory makeGaussAPC () {
		return new OEBayFactoryGaussAPC ();
	}

	// Construct a factory for a Gauss a/p/c prior, using parameters for the given regime.
	// Default parameters are used if the regime is null or blank, or if it is not found.
	// Due to overloading, a null argument must be explicitly a String (or use "" instead).

	public static OEBayFactory makeGaussAPC (String the_regime) {
		return new OEBayFactoryGaussAPC (the_regime);
	}

	// Construct a factory for a Gauss a/p/c prior, using the given location to select parameters.
	// Default parameters are used if the location is null.
	// Due to overloading, a null argument must be explicitly a Location.

	public static OEBayFactory makeGaussAPC (Location the_loc) {
		return new OEBayFactoryGaussAPC (the_loc);
	}

	// Construct a factory for a Gauss a/p/c prior, using the given parameters.
	// Default parameters are used if the parameter object is null.
	// Due to overloading, a null argument must be explicitly a OEGaussAPCParams.

	public static OEBayFactory makeGaussAPC (OEGaussAPCParams the_params) {
		return new OEBayFactoryGaussAPC (the_params);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 136001;

	private static final String M_VERSION_NAME = "OEBayFactory";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 136000;
	protected static final int MARSHAL_UNIFORM = 137001;
	protected static final int MARSHAL_FIXED = 138001;
	protected static final int MARSHAL_GAUSSIAN_APC = 139001;

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

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public OEBayFactory unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayFactory obj) {

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

	public static OEBayFactory unmarshal_poly (MarshalReader reader, String name) {
		OEBayFactory result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayFactory.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_UNIFORM:
			result = new OEBayFactoryUniform();
			result.do_umarshal (reader);
			break;

		case MARSHAL_FIXED:
			result = new OEBayFactoryFixed();
			result.do_umarshal (reader);
			break;

		case MARSHAL_GAUSSIAN_APC:
			result = new OEBayFactoryGaussAPC();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
