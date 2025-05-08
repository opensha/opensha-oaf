package org.opensha.oaf.oetas.util;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * A discrete range of parameter values, for operational ETAS -- Sub-range of a parent range.
 * Author: Michael Barall 12/20/2024.
 *
 * This class produces a discrete range of values that are a sub-range of a given parent range.
 */
public class OEDiscreteRangeSubRange extends OEDiscreteRange {

	//----- Parameters -----


	// The parent range.

	private OEDiscreteRange parent_range;

	// The beginning index of the sub-range within the parent range, inclusive.

	private int subix_lo;

	// The ending index of the sub-range within the parent range, exclusive.
	// Must satisfy 0 <= subix_lo < subix_hi <= parent_range.get_range_size().

	private int subix_hi;

	// Minimum and maximum values.
	// Must satisfy range_max >= range_min.
	// If range_size == 1, then must satisfy range_max == range_min.

	private double range_min;
	private double range_max;

	// Middle of the range.

	private double range_middle;




	//----- Range -----


	// Get the number of parameter values in the discrete range.

	@Override
	public int get_range_size () {
		return subix_hi - subix_lo;
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
		return range_middle;
	}




	// Get the discrete parameter values as an array.
	// It is guaranteed that the length of the array equals get_range_size(),
	// the first element of the array equals get_range_min(), and the last
	// element of the array equals get_range_max().
	// Elements of this array appear in non-decreasing (and typically increasing) order.
	// The returned array is newly-allocated, so the caller is free to modify it.

	@Override
	public double[] get_range_array () {
		double[] x = parent_range.get_range_array();
		return Arrays.copyOfRange (x, subix_lo, subix_hi);
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
		double[] x = parent_range.get_bin_array();
		return Arrays.copyOfRange (x, subix_lo, subix_hi + 1);
	}




	// Get an array of value elements, each spanning one bin.
	// The value of each element equals the corresponding element of get_range_array().
	// The lower and upper limits of each element equal the corresponding elements of get_bin_array().

	@Override
	public OEValueElement[] get_velt_array () {

		// This code would return a single-value element in the case of a single-element subrange,
		// however, the endpoints of the value element would not match the elements of bin_array.

		//  if (get_range_size() == 1) {
		//  	final double[] range_array = get_range_array();
		//  	final OEValueElement[] velt_array = new OEValueElement[1];
		//  	velt_array[0] = new OEValueElement (range_array[0]);
		//  	return velt_array;
		//  }

		// Return a subarray of the parent's array

		OEValueElement[] x = parent_range.get_velt_array();
		return Arrays.copyOfRange (x, subix_lo, subix_hi);
	}




	// Get the natural scale for this range.
	// Returns one of the values RSCALE_XXXXX.
	// Note: A single-value range may return any value.

	@Override
	public int get_natural_scale () {
		return parent_range.get_natural_scale();
	}




	// Return true if the range is uniformly spaced in its natural scale.
	// Note: A single-value range should always return true.

	@Override
	public boolean is_natural_uniform () {
		return (get_range_size() == 1 || parent_range.is_natural_uniform());
	}




	// Get the relative measure of each value in the natural scale, as an array.
	// It is guaranteed that the length of the array equals get_range_size().
	// The measure is the size of the bin corresponding to the value, in the natural scale.
	// Measures are normalized so that the largest measure is 1.0.
	// If the range is uniform, then each element of the returned array is 1.0.
	// The returned array is newly-allocated, so the caller is free to modify it.
	// Note: A range with unknown natural scale, that can contain more than one
	// element, should override this method.

	@Override
	public double[] get_natural_measure_array () {
		int rsize = get_range_size();
		double[] measure_array;
		if (rsize == 1 || parent_range.is_natural_uniform()) {
			measure_array = new double[rsize];
			for (int i = 0; i < rsize; ++i) {
				measure_array[i] = 1.0;
			}
		} else {
			measure_array = Arrays.copyOfRange (parent_range.get_natural_measure_array(), subix_lo, subix_hi);

			// Normalize largest value to 1.0

			double top = 0.0;
			for (int i = 0; i < rsize; ++i) {
				top = Math.max (top, measure_array[i]);
			}
			for (int i = 0; i < rsize; ++i) {
				measure_array[i] = measure_array[i] / top;
			}
		}

		return measure_array;
	}




	//----- Construction -----


	// Finish the range setup, fill in min, max, middle.

	private void finish_setup () {
		double[] x = parent_range.get_range_array();
		range_min = x[subix_lo];
		range_max = x[subix_hi - 1];

		if ((subix_hi - subix_lo) % 2 == 0) {
			double[] y = parent_range.get_bin_array();
			range_middle = y[(subix_hi + subix_lo) / 2];
		} else {
			range_middle = x[(subix_hi + subix_lo) / 2];
		}

		return;
	}


	// Default constructor does nothing.

	public OEDiscreteRangeSubRange () {}


	// Construct from given parameters.

	public OEDiscreteRangeSubRange (OEDiscreteRange parent_range, int subix_lo, int subix_hi) {
		if (!( 0 <= subix_lo && subix_lo < subix_hi && subix_hi <= parent_range.get_range_size() )) {
			throw new IllegalArgumentException ("OEDiscreteRangeSubRange.OEDiscreteRangeSubRange: Sub-range indexes out-of-range: parent_range.get_range_size() = " + parent_range.get_range_size() + ", subix_lo = " + subix_lo + ", subix_hi = " + subix_hi);
		}

		this.parent_range = parent_range;
		this.subix_lo = subix_lo;
		this.subix_hi = subix_hi;

		finish_setup();
	}


	// Display our contents.

	@Override
	public String toString() {
		return "RangeSubRange[parent_range=" + parent_range.toString()
		+ ", subix_lo=" + subix_lo
		+ ", subix_hi=" + subix_hi
		+ ", range_min=" + range_min
		+ ", range_max=" + range_max
		+ "]";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 144001;

	private static final String M_VERSION_NAME = "OEDiscreteRangeSubRange";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_SUB_RANGE;
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

			OEDiscreteRange.marshal_poly (writer, "parent_range", parent_range);
			writer.marshalInt ("subix_lo", subix_lo);
			writer.marshalInt ("subix_hi", subix_hi);
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
			throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			parent_range = OEDiscreteRange.unmarshal_poly (reader, "parent_range");
			subix_lo = reader.unmarshalInt ("subix_lo");
			subix_hi = reader.unmarshalInt ("subix_hi");

			if (!( 0 <= subix_lo && subix_lo < subix_hi && subix_hi <= parent_range.get_range_size() )) {
				throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal: Sub-range indexes out-of-range: parent_range.get_range_size() = " + parent_range.get_range_size() + ", subix_lo = " + subix_lo + ", subix_hi = " + subix_hi);
			}

			finish_setup();
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			parent_range = OEDiscreteRange.unmarshal_poly (reader, "parent_range");
			subix_lo = reader.unmarshalInt ("subix_lo");
			subix_hi = reader.unmarshalInt ("subix_hi");

			if (!( 0 <= subix_lo && subix_lo < subix_hi && subix_hi <= parent_range.get_range_size() )) {
				throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal: Sub-range indexes out-of-range: parent_range.get_range_size() = " + parent_range.get_range_size() + ", subix_lo = " + subix_lo + ", subix_hi = " + subix_hi);
			}

			finish_setup();
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
	public OEDiscreteRangeSubRange unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEDiscreteRangeSubRange obj) {

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

	public static OEDiscreteRangeSubRange unmarshal_poly (MarshalReader reader, String name) {
		OEDiscreteRangeSubRange result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEDiscreteRangeSubRange.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_SUB_RANGE:
			result = new OEDiscreteRangeSubRange();
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
		writer.marshalString (null, MF_KEY_SUB_RANGE);
		OEDiscreteRange.marshal_friendly (writer, null, parent_range);
		writer.marshalInt    (null, subix_lo);
		writer.marshalInt    (null, subix_hi);
		writer.marshalArrayEnd ();
		return;
	}

	// Friendly unmarshal object, internal.
	// The caller has already started unmarshalling the array, and supplied the array size n.

	@Override
	protected void do_umarshal_friendly (MarshalReader reader, int n) {
		if (n != 4) {
			throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal_friendly: Invalid array length: n = " + n);
		}

		parent_range = OEDiscreteRange.unmarshal_friendly (reader, null);
		subix_lo = reader.unmarshalInt (null);
		subix_hi = reader.unmarshalInt (null);

		if (!( 0 <= subix_lo && subix_lo < subix_hi && subix_hi <= parent_range.get_range_size() )) {
			throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal_friendly: Sub-range indexes out-of-range: parent_range.get_range_size() = " + parent_range.get_range_size() + ", subix_lo = " + subix_lo + ", subix_hi = " + subix_hi);
		}

		finish_setup();

		return;
	}




	// Marshal object with external version, internal.
	// This is called with the map already open.  It must write the MF_TAG_KIND value.

	@Override
	protected void do_marshal_xver (MarshalWriter writer, int xver) {

		switch (xver) {

		default:
			throw new MarshalException ("OEDiscreteRangeSubRange.do_marshal_xver: Unknown version code: version = " + xver);

		// Version 1

		case XVER_1: {

			// Write kind and parameters

			writer.marshalString (MF_TAG_KIND, MF_KEY_SUB_RANGE);
			OEDiscreteRange.marshal_xver (writer, MF_TAG_PARENT, xver, parent_range);
			writer.marshalInt (MF_TAG_LO, subix_lo);
			writer.marshalInt (MF_TAG_HI, subix_hi);
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
			throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal_xver: Unknown version code: version = " + xver);
		
		// Version 1

		case XVER_1: {

			// Read parameters

			parent_range = OEDiscreteRange.unmarshal_xver (reader, MF_TAG_PARENT, xver);
			subix_lo = reader.unmarshalInt (MF_TAG_LO);
			subix_hi = reader.unmarshalInt (MF_TAG_HI);

			if (!( 0 <= subix_lo && subix_lo < subix_hi && subix_hi <= parent_range.get_range_size() )) {
				throw new MarshalException ("OEDiscreteRangeSubRange.do_umarshal_xver: Sub-range indexes out-of-range: parent_range.get_range_size() = " + parent_range.get_range_size() + ", subix_lo = " + subix_lo + ", subix_hi = " + subix_hi);
			}

			finish_setup();
		}
		break;

		}
		return;
	}

}
