package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS -- logarithmic scale with skew.
 * Author: Michael Barall 10/07/2024.
 *
 * This class produces a discrete range of values that are unequally-spaced on a logarithmic scale.
 * A skew parameter specifies how much the values are clustered towards one end of the range.
 */
public class OEDiscreteRangeLogSkew extends OEDiscreteRange {

	//----- Parameters -----


	// Number of values, must be > 0.

	private int range_size;

	// Minimum and maximum values.
	// Must satisfy range_max >= range_min.
	// If range_size == 1, then must satisfy range_max == range_min.

	private double range_min;
	private double range_max;

	// Skew factor.
	// This is (approximately) the ratio of the density of points at the high end
	// of the range, divided by the density of points at the low end of the range.
	// So, range_skew > 1.0 puts more points at the high end of the range,
	// while range_skew == 1.0 makes the points equally spaced.

	private double range_skew;




	//----- Formulas -----


	// Linearization function.
	// Parameters:
	//  x = Value within range.
	//  c = Parameter derived from skew.
	// Retursn w such that points are spaced linearly in w.

	private double lin_fcn (double x, double c) {
		return Math.log(x/(1.0 - (c*x)));
	}


	// Inverse linearization function.
	// Parameters:
	//  w = Variable such that points are spaced linearly in w.
	//  c = Parameter derived from skew.
	// Returns value x within the range.

	private double inv_lin_fcn (double w, double c) {
		final double expw = Math.exp(w);
		return expw/(1.0 + (c*expw));
	}


	// Derivative of log(x) with respect to w, expressed as a function of x.
	// Parameters:
	//  x = Value within range.
	//  c = Parameter derived from skew.
	// Returns the derivative (d/dw)(log(x)).
	// Note: (d/dw)(log(x)) = 1/(1 + c*exp(w)) = 1 - (c*x).

	private double dlogx_dw (double x, double c) {
		return 1.0 - (c*x);
	}


	// Calculate the coefficient c used above.
	// Returns c.
	// Note that c = 0 if skew = 1, and c > 0 if skew > 1.

	private double calc_c () {
		return (range_skew - 1.0)/((range_skew*range_max) - range_min);
	}




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




	// Get the skew factor.

	public double get_range_skew () {
		return range_skew;
	}




	// Get the middle parameter value.
	// The value is guaranteed to lie between get_range_min() and get_range_max(),
	// but the definition of "middle" varies by subclass.

	@Override
	public double get_range_middle () {
		if (range_size == 1) {
			return range_min;
		}
		final double c = calc_c();
		final double wlo = lin_fcn (range_min, c);
		final double whi = lin_fcn (range_max, c);
		return inv_lin_fcn (0.5*(wlo + whi), c);

		//return Math.sqrt (range_min) * Math.sqrt (range_max);
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
				final double c = calc_c();
				final double wlo = lin_fcn (range_min, c);
				final double whi = lin_fcn (range_max, c);
				final double w_delta = (whi - wlo) / ((double)(range_size - 1));
				for (int k = 1; k < range_size - 1; ++k) {
					result[k] = inv_lin_fcn (wlo + (w_delta * ((double)k)), c);
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
			final double c = calc_c();
			final double wlo = lin_fcn (range_min, c);
			final double whi = lin_fcn (range_max, c);
			final double w_delta = (whi - wlo) / ((double)(range_size - 1));
			result[0] = Math.exp ( Math.log (range_min) - (0.5 * w_delta * dlogx_dw (range_min, c)) );
			result[range_size] = Math.exp ( Math.log (range_max) + (0.5 * w_delta * dlogx_dw (range_max, c)) );
			for (int k = 1; k < range_size; ++k) {
				result[k] = inv_lin_fcn (wlo + (w_delta * (((double)k) - 0.5)), c);
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
		return RSCALE_LOG;
	}




	// Return true if the range is uniformly spaced in its natural scale.
	// Note: A single-value range should always return true.

	@Override
	public boolean is_natural_uniform () {
		return (range_size == 1);
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRangeLogSkew () {}


	// Construct from given parameters.

	public OEDiscreteRangeLogSkew (int range_size, double range_min, double range_max, double range_skew) {
		if (!( range_size >= 1 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLogSkew.OEDiscreteRangeLogSkew: Range size is non-positive: range_size = " + range_size);
		}

		if (!( range_min > 0.0 && range_max > 0.0 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLogSkew.OEDiscreteRangeLogSkew: Range limits are non-positive: range_min = " + range_min + ", range_max = " + range_max);
		}

		if (!( range_skew > 0.0 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLogSkew.OEDiscreteRangeLogSkew: Range skew is non-positive: range_skew = " + range_skew);
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

		this.range_skew = range_skew;
	}


	// Display our contents.

	@Override
	public String toString() {
		return "RangeLogSkew[range_size=" + range_size
		+ ", range_min=" + range_min
		+ ", range_max=" + range_max
		+ ", range_skew=" + range_skew
		+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 141001;

	private static final String M_VERSION_NAME = "OEDiscreteRangeLogSkew";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_LOG_SKEW;
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
			writer.marshalDouble ("range_skew", range_skew);

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
			throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			range_size = reader.unmarshalInt ("range_size");
			double the_range_min = reader.unmarshalDouble ("range_min");
			double the_range_max = reader.unmarshalDouble ("range_max");
			double the_range_skew = reader.unmarshalDouble ("range_skew");

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range size is non-positive: range_size = " + range_size);
			}

			if (!( the_range_min > 0.0 && the_range_max > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range limits are non-positive: range_min = " + the_range_min + ", range_max = " + the_range_max);
			}

			if (!( the_range_skew > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range skew is non-positive: range_skew = " + range_skew);
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

			range_skew = the_range_skew;
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
			range_skew = reader.unmarshalDouble ("range_skew");

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range size is non-positive: range_size = " + range_size);
			}

			if (!( range_min > 0.0 && range_max > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range limits are non-positive: range_min = " + range_min + ", range_max = " + range_max);
			}

			if (!( range_min <= range_max )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range limits out-of-order: range_min = " + range_min + ", range_max = " + range_max);
			}

			if (!( range_skew > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal: Range skew is non-positive: range_skew = " + range_skew);
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
	public OEDiscreteRangeLogSkew unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRangeLogSkew obj) {

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

	public static OEDiscreteRangeLogSkew unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRangeLogSkew result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRangeLogSkew.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_LOG_SKEW:
			result = new OEDiscreteRangeLogSkew();
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
		int n = 5;
		writer.marshalArrayBegin (name, n);
		writer.marshalString (null, MF_KEY_LOG_SKEW);
		writer.marshalInt    (null, range_size);
		writer.marshalDouble (null, range_min);
		writer.marshalDouble (null, range_max);
		writer.marshalDouble (null, range_skew);
		writer.marshalArrayEnd ();
		return;
	}

	// Friendly unmarshal object, internal.
	// The caller has already started unmarshalling the array, and supplied the array size n.

	@Override
	protected void do_umarshal_friendly (MarshalReader reader, int n) {
		if (n != 5) {
			throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal_friendly: Invalid array length: n = " + n);
		}

		range_size = reader.unmarshalInt (null);
		double the_range_min = reader.unmarshalDouble (null);
		double the_range_max = reader.unmarshalDouble (null);
		double the_range_skew = reader.unmarshalDouble (null);

		if (!( range_size >= 1 )) {
			throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_friendly: Range size is non-positive: range_size = " + range_size);
		}

		if (!( the_range_min > 0.0 && the_range_max > 0.0 )) {
			throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_friendly: Range limits are non-positive: range_min = " + the_range_min + ", range_max = " + the_range_max);
		}

		if (!( the_range_skew > 0.0 )) {
			throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_friendly: Range skew is non-positive: range_skew = " + range_skew);
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

		range_skew = the_range_skew;

		return;
	}




	// Marshal object with external version, internal.
	// This is called with the map already open.  It must write the MF_TAG_KIND value.

	@Override
	protected void do_marshal_xver (MarshalWriter writer, int xver) {

		switch (xver) {

		default:
			throw new MarshalException ("OEDiscreteRangeLogSkew.do_marshal_xver: Unknown version code: version = " + xver);

		// Version 1

		case XVER_1: {

			// Write kind and parameters

			writer.marshalString (MF_TAG_KIND, MF_KEY_LOG_SKEW);
			writer.marshalInt    (MF_TAG_NUM, range_size);
			writer.marshalDouble (MF_TAG_MIN, range_min);
			writer.marshalDouble (MF_TAG_MAX, range_max);
			writer.marshalDouble (MF_TAG_SKEW, range_skew);
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
			throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_xver: Unknown version code: version = " + xver);
		
		// Version 1

		case XVER_1: {

			// Read parameters

			range_size = reader.unmarshalInt (MF_TAG_NUM);
			double the_range_min = reader.unmarshalDouble (MF_TAG_MIN);
			double the_range_max = reader.unmarshalDouble (MF_TAG_MAX);
			double the_range_skew = reader.unmarshalDouble (MF_TAG_SKEW);

			if (!( range_size >= 1 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_xver: Range size is non-positive: range_size = " + range_size);
			}

			if (!( the_range_min > 0.0 && the_range_max > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_xver: Range limits are non-positive: range_min = " + the_range_min + ", range_max = " + the_range_max);
			}

			if (!( the_range_skew > 0.0 )) {
				throw new MarshalException ("OEDiscreteRangeLogSkew.do_umarshal_xver: Range skew is non-positive: range_skew = " + range_skew);
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

			range_skew = the_range_skew;
		}
		break;

		}
		return;
	}

}
