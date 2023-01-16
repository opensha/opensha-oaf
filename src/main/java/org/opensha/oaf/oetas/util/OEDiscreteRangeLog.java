package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS -- logarithmic scale.
 * Author: Michael Barall 03/15/2020.
 *
 * This class produces a discrete range of values that are equally-spaced on a logarithmic scale.
 */
public class OEDiscreteRangeLog extends OEDiscreteRange {

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

	public double get_range_middle () {
		if (range_size == 1) {
			return range_min;
		}
		return Math.sqrt (range_min) * Math.sqrt (range_max);
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
				final double log_range_min = Math.log (range_min);
				final double log_range_max = Math.log (range_max);
				final double r_delta = (log_range_max - log_range_min) / ((double)(range_size - 1));
				for (int k = 1; k < range_size - 1; ++k) {
					result[k] = Math.exp (log_range_min + (r_delta * ((double)k)));
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
			final double log_range_min = Math.log (range_min);
			final double log_range_max = Math.log (range_max);
			final double r_delta = (log_range_max - log_range_min) / ((double)(range_size - 1));
			for (int k = 0; k <= range_size; ++k) {
				result[k] = Math.exp (log_range_min + (r_delta * (((double)k) - 0.5)));
			}
		} else {
			result[0] = range_min;
			result[1] = range_min;
		}
		return result;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRangeLog () {}


	// Construct from given parameters.

	public OEDiscreteRangeLog (int range_size, double range_min, double range_max) {
		if (!( range_size >= 1 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLog.OEDiscreteRangeLog: Range size is non-positive: range_size = " + range_size);
		}

		if (!( range_min > 0.0 && range_max > 0.0 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLog.OEDiscreteRangeLog: Range limits are non-positive: range_min = " + range_min + ", range_max = " + range_max);
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
		return "RangeLog[range_size=" + range_size
		+ ", range_min=" + range_min
		+ ", range_max=" + range_max
		+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 88001;

	private static final String M_VERSION_NAME = "OEDiscreteRangeLog";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_LOG;
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
			throw new MarshalException ("OEDiscreteRangeLog.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			range_size = reader.unmarshalInt ("range_size");
			double the_range_min = reader.unmarshalDouble ("range_min");
			double the_range_max = reader.unmarshalDouble ("range_max");

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLog.do_umarshal: Range size is non-positive: range_size = " + range_size);
			}

			if (!( the_range_min > 0.0 && the_range_max > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLog.do_umarshal: Range limits are non-positive: range_min = " + the_range_min + ", range_max = " + the_range_max);
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
				throw new MarshalException ("OEDiscreteRangeLog.do_umarshal: Range size is non-positive: range_size = " + range_size);
			}

			if (!( range_min > 0.0 && range_max > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLog.do_umarshal: Range limits are non-positive: range_min = " + range_min + ", range_max = " + range_max);
			}

			if (!( range_min <= range_max )) {
				throw new MarshalException ("OEDiscreteRangeLog.do_umarshal: Range limits out-of-order: range_min = " + range_min + ", range_max = " + range_max);
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
	public OEDiscreteRangeLog unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRangeLog obj) {

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

	public static OEDiscreteRangeLog unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRangeLog result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRangeLog.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
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
