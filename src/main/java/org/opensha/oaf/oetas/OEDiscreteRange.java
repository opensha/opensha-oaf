package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS.
 * Author: Michael Barall 03/15/2020.
 *
 * This abstract class represents a discrete range of parameter values.
 * The range may be retrieved as an array.
 *
 * Objects of this class, and its subclasses, are immutable and stateless.
 */
public abstract class OEDiscreteRange {

	//----- Range -----


	// Get the number of parameter values in the discrete range.

	public abstract int get_range_size ();




	// Get the minimum parameter value.

	public abstract double get_range_min ();




	// Get the maximum parameter value.

	public abstract double get_range_max ();




	// Get the discrete parameter values as an array.
	// It is guaranteed that the length of the array equals get_range_size(),
	// the first element of the array equals get_range_min(), and the last
	// element of the array equals get_range_max().

	public abstract double[] get_range_array ();




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRange () {}


	// Display our contents

	@Override
	public String toString() {
		return "OEDiscreteRange";
	}




	//----- Factory methods -----


	// Construct a single-value range, equal to the given value.

	public static OEDiscreteRange makeSingle (double range_value) {
		return new OEDiscreteRangeSingle (range_value);
	}


	// Construct a linear range with the given number and range of values.

	public static OEDiscreteRange makeLinear (int range_size, double range_min, double range_max) {
		return new OEDiscreteRangeLinear (range_size, range_min, range_max);
	}


	// Construct a log range with the given number and range of values.

	public static OEDiscreteRange makeLog (int range_size, double range_min, double range_max) {
		return new OEDiscreteRangeLog (range_size, range_min, range_max);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 85001;

	private static final String M_VERSION_NAME = "OEDiscreteRange";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 85000;
	protected static final int MARSHAL_SINGLE = 86001;
	protected static final int MARSHAL_LINEAR = 87001;
	protected static final int MARSHAL_LOG = 88001;

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

	public OEDiscreteRange unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRange obj) {

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

	public static OEDiscreteRange unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRange result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRange.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_SINGLE:
			result = new OEDiscreteRangeSingle();
			result.do_umarshal (reader);
			break;

		case MARSHAL_LINEAR:
			result = new OEDiscreteRangeLinear();
			result.do_umarshal (reader);
			break;

		case MARSHAL_LOG:
			result = new OEDiscreteRangeLog();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
