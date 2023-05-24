package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS -- single-value scale.
 * Author: Michael Barall 03/15/2020.
 *
 * This class produces a discrete range of values that is a single value.
 */
public class OEDiscreteRangeSingle extends OEDiscreteRange {

	//----- Parameters -----


	// The single value in the range.

	private double range_value;




	//----- Range -----


	// Get the number of parameter values in the discrete range.

	@Override
	public int get_range_size () {
		return 1;
	}




	// Get the minimum parameter value.

	@Override
	public double get_range_min () {
		return range_value;
	}




	// Get the maximum parameter value.

	@Override
	public double get_range_max () {
		return range_value;
	}




	// Get the middle parameter value.
	// The value is guaranteed to lie between get_range_min() and get_range_max(),
	// but the definition of "middle" varies by subclass.

	public double get_range_middle () {
		return range_value;
	}




	// Get the discrete parameter values as an array.
	// It is guaranteed that the length of the array equals get_range_size(),
	// the first element of the array equals get_range_min(), and the last
	// element of the array equals get_range_max().
	// Elements of this array appear in non-decreasing (and typically increasing) order.
	// The returned array is newly-allocated, so the caller is free to modify it.

	@Override
	public double[] get_range_array () {
		double[] result = new double[1];
		result[0] = range_value;
		return result;
	}




	// Get the array to partition the discrete parameter values into bins.
	// It is guaranteed that the length of the array equals get_range_size() + 1, with
	//   bin_array[n] <= range_array[n] <= bin_array[n+1]
	// Typically all the inequalities are strict, except possibly for the inequalities
	// involving the first and last elements of bin_array, and often those are strict too.
	// The first element of the array is <= get_range_min(), and the last element of The
	// array is >= get_range_max(), and often but not always those inequalities are strict.
	// If the range consists of a single value, it is possible that the returned
	// two-element array contains that single value in both elements.
	// The returned array is newly-allocated, so the caller is free to modify it.

	@Override
	public double[] get_bin_array () {
		double[] result = new double[2];
		result[0] = range_value;
		result[1] = range_value;
		return result;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRangeSingle () {}


	// Construct from given parameters.

	public OEDiscreteRangeSingle (double range_value) {
		this.range_value = range_value;
	}


	// Display our contents.

	@Override
	public String toString() {
		return "RangeSingle[range_value=" + range_value + "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 86001;

	private static final String M_VERSION_NAME = "OEDiscreteRangeSingle";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_SINGLE;
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

			writer.marshalDouble ("range_value", range_value);

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
			throw new MarshalException ("OEDiscreteRangeSingle.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			range_value = reader.unmarshalDouble ("range_value");
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			range_value = reader.unmarshalDouble ("range_value");
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
	public OEDiscreteRangeSingle unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRangeSingle obj) {

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

	public static OEDiscreteRangeSingle unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRangeSingle result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRangeSingle.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_SINGLE:
			result = new OEDiscreteRangeSingle();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	// Friendly marshal object, internal.
	// The friendly form is an array whose first element is one of the keys.  An empty array is a null object.

	@Override
	protected void do_marshal_friendly (MarshalWriter writer, String name) {
		int n = 2;
		writer.marshalArrayBegin (name, n);
		writer.marshalString (null, MF_KEY_SINGLE);
		writer.marshalDouble (null, range_value);
		writer.marshalArrayEnd ();
		return;
	}

	// Friendly unmarshal object, internal.
	// The caller has already started unmarshalling the array, and supplied the array size n.

	@Override
	protected void do_umarshal_friendly (MarshalReader reader, int n) {
		if (n != 2) {
			throw new MarshalException ("OEDiscreteRangeSingle.do_umarshal_friendly: Invalid array length: n = " + n);
		}
		range_value = reader.unmarshalDouble (null);
		return;
	}




	// Marshal object with external version, internal.
	// This is called with the map already open.  It must write the MF_TAG_KIND value.

	@Override
	protected void do_marshal_xver (MarshalWriter writer, int xver) {

		switch (xver) {

		default:
			throw new MarshalException ("OEDiscreteRangeSingle.do_marshal_xver: Unknown version code: version = " + xver);

		// Version 1

		case XVER_1: {

			// Write kind and parameters

			writer.marshalString (MF_TAG_KIND, MF_KEY_SINGLE);
			writer.marshalDouble (MF_TAG_VALUE, range_value);
		}
		break;

		}
		return;
	}

	// Unmarshal object with external version, internal.
	// This is called with the map already open.  The MF_TAG_KIND value has already been read.

	@Override
	protected void do_umarshal_xver (MarshalReader reader, int xver) {

		switch (xver) {

		default:
			throw new MarshalException ("OEDiscreteRangeSingle.do_umarshal_xver: Unknown version code: version = " + xver);
		
		// Version 1

		case XVER_1: {

			// Read parameters

			range_value = reader.unmarshalDouble (MF_TAG_VALUE);
		}
		break;

		}
		return;
	}

}
