package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS -- linear scale with power-law skew.
 * Author: Michael Barall 10/09/2024.
 *
 * This class produces a discrete range of values that are unequally-spaced on a linear scale.
 * A skew parameter specifies how much the values are clustered towards one end of the range.
 * A power law controls the distribution of points.
 */
public class OEDiscreteRangeLinearPowerSkew extends OEDiscreteRange {

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
	// So, range_skew > 1.0 puts more points at the high end of the range.
	// Taking the reciprocal effectively reverses the two ends of the range.
	// Values of range_skew close to 1.0 are not allowed.

	private double range_skew;

	// Linearization power.
	// Points are spaced linearly according to a segment of the curve with
	// this power.  Setting range_power > 1 makes point density increase toward
	// the high end of the range, while range_power < 1 makes point density
	// increase toward the low end of the range (assuming range_skew > 1.0).
	// Values of range_power close to 1.0 are not allowed.

	private double range_power;




	//----- Derived values -----


	// Linearization coefficient.
	// Points are distributed linearly according to a power law (x - c)^m where
	// x is the value and and m == range_power.

	private double c;

	// Direction in which point density is increasing.
	// This is +1.0 if point density increases toward the high end of the range
	// (which implies that c is below the low end of the range), otherwise -1.0.

	private double dir;




	//----- Formulas -----


	// Linearization function.
	// Parameters:
	//  x = Value within range.
	// Returns w such that points are spaced linearly in w.

	private double lin_fcn (double x) {
		return Math.pow ((x - c) * dir, range_power);
	}


	// Inverse linearization function.
	// Parameters:
	//  w = Variable such that points are spaced linearly in w.
	//  c = Parameter derived from skew.
	// Returns value x within the range.

	private double inv_lin_fcn (double w) {
		return c + (dir * Math.pow (w, 1.0/range_power));
	}


	// Derivative of x with respect to w, expressed as a function of x.
	// Parameters:
	//  x = Value within range.
	//  c = Parameter derived from skew.
	// Returns the derivative (d/dw)(x).

	private double dx_dw (double x) {
		return Math.pow ((x - c) * dir, 1.0 - range_power) * dir / range_power;
	}




	// Allowed range of range_power above 1.0.

	private static final double MIN_HI_RANGE_POWER = 1.5;
	private static final double MAX_HI_RANGE_POWER = 10.0;

	private static final double RANGE_POWER_EPS = 0.0001;
	private static final double CHECK_MIN_HI_RANGE_POWER = MIN_HI_RANGE_POWER - RANGE_POWER_EPS;
	private static final double CHECK_MAX_HI_RANGE_POWER = MAX_HI_RANGE_POWER + RANGE_POWER_EPS;
	private static final double CHECK_MIN_LO_RANGE_POWER = 1.0 / CHECK_MAX_HI_RANGE_POWER;
	private static final double CHECK_MAX_LO_RANGE_POWER = 1.0 / CHECK_MIN_HI_RANGE_POWER;

	// Allowed range of range_skew above 1.0.

	private static final double MIN_HI_RANGE_SKEW = 1.1;
	private static final double MAX_HI_RANGE_SKEW = 20.0;

	private static final double RANGE_SKEW_EPS = 0.0001;
	private static final double CHECK_MIN_HI_RANGE_SKEW = MIN_HI_RANGE_SKEW - RANGE_SKEW_EPS;
	private static final double CHECK_MAX_HI_RANGE_SKEW = MAX_HI_RANGE_SKEW + RANGE_SKEW_EPS;
	private static final double CHECK_MIN_LO_RANGE_SKEW = 1.0 / CHECK_MAX_HI_RANGE_SKEW;
	private static final double CHECK_MAX_LO_RANGE_SKEW = 1.0 / CHECK_MIN_HI_RANGE_SKEW;




	// Set up all parameters and derived values.
	// Throws an exception if invalid.

	private void setup (int the_range_size, double the_range_min, double the_range_max, double the_range_skew, double the_range_power) {

		// Check parameters

		if (!( the_range_size >= 1 )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLinearPowerSkew.setup: Range size is non-positive: range_size = " + the_range_size);
		}

		if (!( (the_range_skew >= CHECK_MIN_HI_RANGE_SKEW && the_range_skew <= CHECK_MAX_HI_RANGE_SKEW)
			|| (the_range_skew >= CHECK_MIN_LO_RANGE_SKEW && the_range_skew <= CHECK_MAX_LO_RANGE_SKEW) )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLinearPowerSkew.setup: Range skew is invalid: range_skew = " + the_range_skew);
		}

		if (!( (the_range_power >= CHECK_MIN_HI_RANGE_POWER && the_range_power <= CHECK_MAX_HI_RANGE_POWER)
			|| (the_range_power >= CHECK_MIN_LO_RANGE_POWER && the_range_power <= CHECK_MAX_LO_RANGE_POWER) )) {
			throw new IllegalArgumentException ("OEDiscreteRangeLinearPowerSkew.setup: Range power is invalid: range_power = " + the_range_power);
		}

		// Handle a single-point range

		if (the_range_size == 1) {
			range_size = the_range_size;
			range_min = Math.min (the_range_min, the_range_max);
			range_max = range_min;
			range_skew = the_range_skew;
			range_power = the_range_power;

			c = 0.0;
			dir = 0.0;
		}

		// Otherwise, we have a multi-point range

		else {

			// Save parameters, ensuring min and max are correct order

			range_size = the_range_size;
			if (the_range_min <= the_range_max) {
				range_min = the_range_min;
				range_max = the_range_max;
			} else {
				range_min = the_range_max;
				range_max = the_range_min;
			}
			range_skew = the_range_skew;
			range_power = the_range_power;

			// If power and skew are both >1 or both <1 ...

			if ((range_power >= 1 && range_skew >= 1) || (range_power < 1 && range_skew < 1)) {

				// Density increasing toward high end of range

				dir = 1.0;

				// Value of c is below low end of range

				c = range_min - ((range_max - range_min) / ( Math.expm1 (Math.log(range_skew) / (range_power - 1.0)) ));
			}

			// Otherwise ...

			else {

				// Density increasing toward low end of range

				dir = -1.0;

				// Value of c is above high end of range

				c = range_max + ((range_max - range_min) / ( Math.expm1 (Math.log(range_skew) / (1.0 - range_power)) ));
			}
		}

		return;
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




	// Get the middle parameter value.
	// The value is guaranteed to lie between get_range_min() and get_range_max(),
	// but the definition of "middle" varies by subclass.

	@Override
	public double get_range_middle () {
		if (range_size == 1) {
			return range_min;
		}
		final double wlo = lin_fcn (range_min);
		final double whi = lin_fcn (range_max);
		return inv_lin_fcn (0.5*(wlo + whi));

		//return (range_min + range_max) * 0.5;
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
				final double wlo = lin_fcn (range_min);
				final double whi = lin_fcn (range_max);
				final double w_delta = (whi - wlo) / ((double)(range_size - 1));
				for (int k = 1; k < range_size - 1; ++k) {
					result[k] = inv_lin_fcn (wlo + (w_delta * ((double)k)));
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
			final double wlo = lin_fcn (range_min);
			final double whi = lin_fcn (range_max);
			final double w_delta = (whi - wlo) / ((double)(range_size - 1));
			result[0] = range_min - (0.5 * w_delta * dx_dw (range_min));
			result[range_size] = range_max + (0.5 * w_delta * dx_dw (range_max));
			for (int k = 1; k < range_size; ++k) {
				result[k] = inv_lin_fcn (wlo + (w_delta * (((double)k) - 0.5)));
			}
		} else {
			result[0] = range_min;
			result[1] = range_min;
		}
		return result;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRangeLinearPowerSkew () {}


	// Construct from given parameters.

	public OEDiscreteRangeLinearPowerSkew (int the_range_size, double the_range_min, double the_range_max, double the_range_skew, double the_range_power) {
		setup (the_range_size, the_range_min, the_range_max, the_range_skew, the_range_power);
	}


	// Display our contents.

	@Override
	public String toString() {
		return "RangeLinearPowerSkew[range_size=" + range_size
		+ ", range_min=" + range_min
		+ ", range_max=" + range_max
		+ ", range_skew=" + range_skew
		+ ", range_power=" + range_power
		+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 143001;

	private static final String M_VERSION_NAME = "OEDiscreteRangeLinearPowerSkew";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_LINEAR_POWER_SKEW;
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
			writer.marshalDouble ("range_power", range_power);

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
			throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			int the_range_size = reader.unmarshalInt ("range_size");
			double the_range_min = reader.unmarshalDouble ("range_min");
			double the_range_max = reader.unmarshalDouble ("range_max");
			double the_range_skew = reader.unmarshalDouble ("range_skew");
			double the_range_power = reader.unmarshalDouble ("range_power");

			try {
				setup (the_range_size, the_range_min, the_range_max, the_range_skew, the_range_power);
			} catch (Exception e) {
				throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_umarshal: Range has invalid parameters", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			int the_range_size = reader.unmarshalInt ("range_size");
			double the_range_min = reader.unmarshalDouble ("range_min");
			double the_range_max = reader.unmarshalDouble ("range_max");
			double the_range_skew = reader.unmarshalDouble ("range_skew");
			double the_range_power = reader.unmarshalDouble ("range_power");

			try {
				setup (the_range_size, the_range_min, the_range_max, the_range_skew, the_range_power);
			} catch (Exception e) {
				throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_umarshal: Range has invalid parameters", e);
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
	public OEDiscreteRangeLinearPowerSkew unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRangeLinearPowerSkew obj) {

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

	public static OEDiscreteRangeLinearPowerSkew unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRangeLinearPowerSkew result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_LINEAR_POWER_SKEW:
			result = new OEDiscreteRangeLinearPowerSkew();
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
		int n = 6;
		writer.marshalArrayBegin (name, n);
		writer.marshalString (null, MF_KEY_LINEAR_POWER_SKEW);
		writer.marshalInt    (null, range_size);
		writer.marshalDouble (null, range_min);
		writer.marshalDouble (null, range_max);
		writer.marshalDouble (null, range_skew);
		writer.marshalDouble (null, range_power);
		writer.marshalArrayEnd ();
		return;
	}

	// Friendly unmarshal object, internal.
	// The caller has already started unmarshalling the array, and supplied the array size n.

	@Override
	protected void do_umarshal_friendly (MarshalReader reader, int n) {
		if (n != 6) {
			throw new MarshalException ("OEDiscreteRangeLinear.do_umarshal_friendly: Invalid array length: n = " + n);
		}

		int the_range_size = reader.unmarshalInt (null);
		double the_range_min = reader.unmarshalDouble (null);
		double the_range_max = reader.unmarshalDouble (null);
		double the_range_skew = reader.unmarshalDouble (null);
		double the_range_power = reader.unmarshalDouble (null);

		try {
			setup (the_range_size, the_range_min, the_range_max, the_range_skew, the_range_power);
		} catch (Exception e) {
			throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_umarshal_friendly: Range has invalid parameters", e);
		}

		return;
	}




	// Marshal object with external version, internal.
	// This is called with the map already open.  It must write the MF_TAG_KIND value.

	@Override
	protected void do_marshal_xver (MarshalWriter writer, int xver) {

		switch (xver) {

		default:
			throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_marshal_xver: Unknown version code: version = " + xver);

		// Version 1

		case XVER_1: {

			// Write kind and parameters

			writer.marshalString (MF_TAG_KIND, MF_KEY_LINEAR_POWER_SKEW);
			writer.marshalInt    (MF_TAG_NUM, range_size);
			writer.marshalDouble (MF_TAG_MIN, range_min);
			writer.marshalDouble (MF_TAG_MAX, range_max);
			writer.marshalDouble (MF_TAG_SKEW, range_skew);
			writer.marshalDouble (MF_TAG_POWER, range_power);
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
			throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_umarshal_xver: Unknown version code: version = " + xver);
		
		// Version 1

		case XVER_1: {

			// Read parameters

			int the_range_size = reader.unmarshalInt (MF_TAG_NUM);
			double the_range_min = reader.unmarshalDouble (MF_TAG_MIN);
			double the_range_max = reader.unmarshalDouble (MF_TAG_MAX);
			double the_range_skew = reader.unmarshalDouble (MF_TAG_SKEW);
			double the_range_power = reader.unmarshalDouble (MF_TAG_POWER);

			try {
				setup (the_range_size, the_range_min, the_range_max, the_range_skew, the_range_power);
			} catch (Exception e) {
				throw new MarshalException ("OEDiscreteRangeLinearPowerSkew.do_umarshal_xver: Range has invalid parameters", e);
			}
		}
		break;

		}
		return;
	}

}
