package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS -- linear scale.
 * Author: Michael Barall 03/15/2020.
 *
 * This class produces a discrete range of values that are equally-spaced on a linear scale.
 */
public class OEDiscreteRangeLinear extends OEDiscreteRange {

	//----- Parameters -----


	// Number of values, must be > 0.

	private int range_size;

	// Minimum and maximum values.
	// Must satisfy range_max >= range_min.
	// If range_size == 1, then must satisfy range_max == range_min.

	private double range_min;
	private double range_max;




	//----- Range -----


	// Get the number of parameter values in the discrete range.

	@Override
	public int get_range_size () {
		return range_size;
	}




	// Get the minimum parameter value.

	@Override
	public double get_range_min () {
		return range_min;
	}




	// Get the maximum parameter value.

	@Override
	public double get_range_max () {
		return range_max;
	}




	// Get the middle parameter value.
	// The value is guaranteed to lie between get_range_min() and get_range_max(),
	// but the definition of "middle" varies by subclass.

	@Override
	public double get_range_middle () {
		if (range_size == 1) {
			return range_min;
		}
		return (range_min + range_max) * 0.5;
	}




	// Get the discrete parameter values as an array.
	// It is guaranteed that the length of the array equals get_range_size(),
	// the first element of the array equals get_range_min(), and the last
	// element of the array equals get_range_max().
	// Elements of this array appear in non-decreasing (and typically increasing) order.
	// The returned array is newly-allocated, so the caller is free to modify it.

	@Override
	public double[] get_range_array () {
		double[] result = new double[range_size];
		result[0] = range_min;
		if (range_size > 1) {
			result[range_size - 1] = range_max;
			if (range_size > 2) {
				final double r_delta = (range_max - range_min) / ((double)(range_size - 1));
				for (int k = 1; k < range_size - 1; ++k) {
					result[k] = range_min + (r_delta * ((double)k));
				}
			}
		}
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
		double[] result = new double[range_size + 1];
		if (range_size > 1) {
			final double r_delta = (range_max - range_min) / ((double)(range_size - 1));
			result[0] = range_min - (0.5 * r_delta);
			result[range_size] = range_max + (0.5 * r_delta);
			for (int k = 1; k < range_size; ++k) {
				result[k] = range_min + (r_delta * (((double)k) - 0.5));
			}
		} else {
			result[0] = range_min;
			result[1] = range_min;
		}
		return result;
	}




	// Get the natural scale for this range.
	// Returns one of the values RSCALE_XXXXX.
	// Note: A single-value range may return any value.

	@Override
	public int get_natural_scale () {
		return RSCALE_LINEAR;
	}




	// Return true if the range is uniformly spaced in its natural scale.
	// Note: A single-value range should always return true.

	@Override
	public boolean is_natural_uniform () {
		return true;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRangeLinear () {}


	// Construct from given parameters.

	public OEDiscreteRangeLinear (int range_size, double range_min, double range_max) {
		if (!( range_size >= 1 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLinear.OEDiscreteRangeLinear: Range size is non-positive: range_size = " + range_size);
		}
		this.range_size = range_size;

		if (range_min <= range_max) {
			if (range_size == 1) {
				this.range_min = range_min;
				this.range_max = range_min;
			} else {
				this.range_min = range_min;
				this.range_max = range_max;
			}
		} else {
			if (range_size == 1) {
				this.range_min = range_max;
				this.range_max = range_max;
			} else {
				this.range_min = range_max;
				this.range_max = range_min;
			}
		}
	}


	// Display our contents.

	@Override
	public String toString() {
		return "RangeLinear[range_size=" + range_size
		+ ", range_min=" + range_min
		+ ", range_max=" + range_max
		+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 87001;

	private static final String M_VERSION_NAME = "OEDiscreteRangeLinear";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_LINEAR;
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

			writer.marshalInt    ("range_size", range_size);
			writer.marshalDouble ("range_min", range_min);
			writer.marshalDouble ("range_max", range_max);

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
			throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			range_size = reader.unmarshalInt ("range_size");
			double the_range_min = reader.unmarshalDouble ("range_min");
			double the_range_max = reader.unmarshalDouble ("range_max");

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal: Range size is non-positive: range_size = " + range_size);
			}

			if (the_range_min <= the_range_max) {
				if (range_size == 1) {
					range_min = the_range_min;
					range_max = the_range_min;
				} else {
					range_min = the_range_min;
					range_max = the_range_max;
				}
			} else {
				if (range_size == 1) {
					range_min = the_range_max;
					range_max = the_range_max;
				} else {
					range_min = the_range_max;
					range_max = the_range_min;
				}
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			range_size = reader.unmarshalInt ("range_size");
			range_min = reader.unmarshalDouble ("range_min");
			range_max = reader.unmarshalDouble ("range_max");

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal: Range size is non-positive: range_size = " + range_size);
			}

			if (!( range_min <= range_max )) {
				throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal: Range limits out-of-order: range_min = " + range_min + ", range_max = " + range_max);
			}

			//  if (range_size == 1) {
			//  	range_max = range_min;
			//  }
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
	public OEDiscreteRangeLinear unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRangeLinear obj) {

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

	public static OEDiscreteRangeLinear unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRangeLinear result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRangeLinear.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_LINEAR:
			result = new OEDiscreteRangeLinear();
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
		int n = 4;
		writer.marshalArrayBegin (name, n);
		writer.marshalString (null, MF_KEY_LINEAR);
		writer.marshalInt    (null, range_size);
		writer.marshalDouble (null, range_min);
		writer.marshalDouble (null, range_max);
		writer.marshalArrayEnd ();
		return;
	}

	// Friendly unmarshal object, internal.
	// The caller has already started unmarshalling the array, and supplied the array size n.

	@Override
	protected void do_umarshal_friendly (MarshalReader reader, int n) {
		if (n != 4) {
			throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal_friendly: Invalid array length: n = " + n);
		}

		range_size = reader.unmarshalInt (null);
		double the_range_min = reader.unmarshalDouble (null);
		double the_range_max = reader.unmarshalDouble (null);

		if (!( range_size >= 1 )) {
			throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal_friendly: Range size is non-positive: range_size = " + range_size);
		}

		if (the_range_min <= the_range_max) {
			if (range_size == 1) {
				range_min = the_range_min;
				range_max = the_range_min;
			} else {
				range_min = the_range_min;
				range_max = the_range_max;
			}
		} else {
			if (range_size == 1) {
				range_min = the_range_max;
				range_max = the_range_max;
			} else {
				range_min = the_range_max;
				range_max = the_range_min;
			}
		}

		return;
	}




	// Marshal object with external version, internal.
	// This is called with the map already open.  It must write the MF_TAG_KIND value.

	@Override
	protected void do_marshal_xver (MarshalWriter writer, int xver) {

		switch (xver) {

		default:
			throw new MarshalException ("OEDiscreteRangeLinear.do_marshal_xver: Unknown version code: version = " + xver);

		// Version 1

		case XVER_1: {

			// Write kind and parameters

			writer.marshalString (MF_TAG_KIND, MF_KEY_LINEAR);
			writer.marshalInt    (MF_TAG_NUM, range_size);
			writer.marshalDouble (MF_TAG_MIN, range_min);
			writer.marshalDouble (MF_TAG_MAX, range_max);
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
			throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal_xver: Unknown version code: version = " + xver);
		
		// Version 1

		case XVER_1: {

			// Read parameters

			range_size = reader.unmarshalInt (MF_TAG_NUM);
			double the_range_min = reader.unmarshalDouble (MF_TAG_MIN);
			double the_range_max = reader.unmarshalDouble (MF_TAG_MAX);

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal_xver: Range size is non-positive: range_size = " + range_size);
			}

			if (the_range_min <= the_range_max) {
				if (range_size == 1) {
					range_min = the_range_min;
					range_max = the_range_min;
				} else {
					range_min = the_range_min;
					range_max = the_range_max;
				}
			} else {
				if (range_size == 1) {
					range_min = the_range_max;
					range_max = the_range_max;
				} else {
					range_min = the_range_max;
					range_max = the_range_min;
				}
			}
		}
		break;

		}
		return;
	}

}
