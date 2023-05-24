package org.opensha.oaf.oetas.util;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import static org.opensha.oaf.util.SimpleUtils.rndd;


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
	// It is guaranteed that the returned value is > 0.

	public abstract int get_range_size ();




	// Get the minimum parameter value.

	public abstract double get_range_min ();




	// Get the maximum parameter value.

	public abstract double get_range_max ();




	// Get the middle parameter value.
	// The value is guaranteed to lie between get_range_min() and get_range_max(),
	// but the definition of "middle" varies by subclass.

	public abstract double get_range_middle ();




	// Get the discrete parameter values as an array.
	// It is guaranteed that the length of the array equals get_range_size(),
	// the first element of the array equals get_range_min(), and the last
	// element of the array equals get_range_max().
	// Elements of this array appear in non-decreasing (and typically increasing) order.
	// The returned array is newly-allocated, so the caller is free to modify it.

	public abstract double[] get_range_array ();




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

	public abstract double[] get_bin_array ();




	// Get an array of value elements, each spanning one bin.
	// The value of each element equals the corresponding element of get_range_array().
	// The lower and upper limits of each element equal the corresponding elements of get_bin_array().
	//
	// Implementation note: The default implementation assumes that a single-value range
	// has both bin limits equal to that single value, and that a range with multiple values
	// has strict inequalities as described for get_bin_array().  A subclass for which this
	// is not true should override this method.

	public OEValueElement[] get_velt_array () {
		final double[] range_array = get_range_array();
		final int range_size = range_array.length;
		final OEValueElement[] velt_array = new OEValueElement[range_size];
		if (range_size > 1) {
			final double[] bin_array = get_bin_array();
			for (int i = 0; i < range_size; ++i) {
				velt_array[i] = new OEValueElement (bin_array[i], range_array[i], bin_array[i + 1]);
			}
		}
		else if (range_size == 1) {
			velt_array[0] = new OEValueElement (range_array[0]);
		}
		return velt_array;
	}




	// Get a scaled array of value elements, each spanning one bin.
	// Parameters:
	//  scale = Scale factor applied to each value.
	//  offset = Offset applied to each value.
	// The value of each element equals v*scale+offset, where v is the corresponding element of get_range_array().
	// The lower and upper limits of each element equal x*scale+offset, where x is the corresponding elements of get_bin_array().
	// Note: If scale < 0 then the ordering is reversed, so the scaled values are in increasing order.
	//
	// Implementation note: The default implementation assumes that a single-value range
	// has both bin limits equal to that single value, and that a range with multiple values
	// has strict inequalities as described for get_bin_array().  A subclass for which this
	// is not true should override this method.

	public OEValueElement[] get_scaled_velt_array (double scale, double offset) {
		final double[] range_array = get_range_array();
		final int range_size = range_array.length;
		final OEValueElement[] velt_array = new OEValueElement[range_size];
		if (range_size > 1) {
			final double[] bin_array = get_bin_array();
			if (scale < 0) {
				for (int i = 0; i < range_size; ++i) {
					int j = (range_size - 1) - i;
					velt_array[i] = new OEValueElement (
						bin_array[j + 1] * scale + offset,
						range_array[j] * scale + offset,
						bin_array[j] * scale + offset
					);
				}
			} else {
				for (int i = 0; i < range_size; ++i) {
					velt_array[i] = new OEValueElement (
						bin_array[i] * scale + offset,
						range_array[i] * scale + offset,
						bin_array[i + 1] * scale + offset
					);
				}
			}
		}
		else if (range_size == 1) {
			velt_array[0] = new OEValueElement (range_array[0] * scale + offset);
		}
		return velt_array;
	}




	// Get a scaled array of log10 of value elements, each spanning one bin.
	// Parameters:
	//  scale = Scale factor applied to each value.
	//  offset = Offset applied to each value.
	// The value of each element equals log10(v)*scale+offset, where v is the corresponding element of get_range_array().
	// The lower and upper limits of each element equal log10(x)*scale+offset, where x is the corresponding elements of get_bin_array().
	// Note: If scale < 0 then the ordering is reversed, so the scaled values are in increasing order.
	//
	// Implementation note: The default implementation assumes that a single-value range
	// has both bin limits equal to that single value, and that a range with multiple values
	// has strict inequalities as described for get_bin_array().  A subclass for which this
	// is not true should override this method.

	public OEValueElement[] get_scaled_log10_velt_array (double scale, double offset) {
		final double[] range_array = get_range_array();
		final int range_size = range_array.length;
		final OEValueElement[] velt_array = new OEValueElement[range_size];
		if (range_size > 1) {
			final double[] bin_array = get_bin_array();
			if (scale < 0) {
				for (int i = 0; i < range_size; ++i) {
					int j = (range_size - 1) - i;
					velt_array[i] = new OEValueElement (
						Math.log10(bin_array[j + 1]) * scale + offset,
						Math.log10(range_array[j]) * scale + offset,
						Math.log10(bin_array[j]) * scale + offset
					);
				}
			} else {
				for (int i = 0; i < range_size; ++i) {
					velt_array[i] = new OEValueElement (
						Math.log10(bin_array[i]) * scale + offset,
						Math.log10(range_array[i]) * scale + offset,
						Math.log10(bin_array[i + 1]) * scale + offset
					);
				}
			}
		}
		else if (range_size == 1) {
			velt_array[0] = new OEValueElement (Math.log10(range_array[0]) * scale + offset);
		}
		return velt_array;
	}




	//----- Construction -----


	// Default constructor does nothing.

	public OEDiscreteRange () {}


	// Display our contents.

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




	// Friendly marshal keys.

	protected static final String MF_KEY_NULL = "null";
	protected static final String MF_KEY_SINGLE = "single";
	protected static final String MF_KEY_LINEAR = "linear";
	protected static final String MF_KEY_LOG = "log";

	// Friendly marshal tags.

	protected static final String MF_TAG_KIND = "kind";
	protected static final String MF_TAG_VALUE = "value";
	protected static final String MF_TAG_NUM = "num";
	protected static final String MF_TAG_MIN = "min";
	protected static final String MF_TAG_MAX = "max";

	// Friendly marshal external version numbers.

	public static final int XVER_1 = 85001;

	// Friendly marshal object, internal.
	// The friendly form is an array whose first element is one of the keys.  An empty array is a null object.

	protected abstract void do_marshal_friendly (MarshalWriter writer, String name);

	// Friendly unmarshal object, internal.
	// The caller has already started unmarshalling the array, and supplied the array size n.

	protected abstract void do_umarshal_friendly (MarshalReader reader, int n);

	// Friendly marshal object, polymorphic.

	public static void marshal_friendly (MarshalWriter writer, String name, OEDiscreteRange obj) {

		if (obj == null) {
			int n = 0;
			writer.marshalArrayBegin (name, n);
			writer.marshalArrayEnd ();
		} else {
			obj.do_marshal_friendly (writer, name);
		}

		return;
	}

	// Friendly unmarshal object, polymorphic.

	public static OEDiscreteRange unmarshal_friendly (MarshalReader reader, String name) {
		OEDiscreteRange result = null;

		int n = reader.unmarshalArrayBegin (name);

		if (n > 0) {
			String key = reader.unmarshalString (null);

			switch (key) {

			default:
				throw new MarshalException ("OEDiscreteRange.unmarshal_friendly: Unknown key: key = " + key);

			case MF_KEY_NULL:
				result = null;
				if (n != 1) {
					throw new MarshalException ("OEDiscreteRange.unmarshal_friendly: Invalid array length: n = " + n);
				}
				break;

			case MF_KEY_SINGLE:
				result = new OEDiscreteRangeSingle();
				result.do_umarshal_friendly (reader, n);
				break;

			case MF_KEY_LINEAR:
				result = new OEDiscreteRangeLinear();
				result.do_umarshal_friendly (reader, n);
				break;

			case MF_KEY_LOG:
				result = new OEDiscreteRangeLog();
				result.do_umarshal_friendly (reader, n);
				break;
			}
		}

		reader.unmarshalArrayEnd ();
		return result;
	}




	// Marshal object with external version, internal.
	// This is called with the map already open.  It must write the MF_TAG_KIND value.

	protected abstract void do_marshal_xver (MarshalWriter writer, int xver);

	// Unmarshal object with external version, internal.
	// This is called with the map already open.  The MF_TAG_KIND value has already been read.

	protected abstract void do_umarshal_xver (MarshalReader reader, int xver);

	// Marshal object with external version, polymorphic.

	public static void marshal_xver (MarshalWriter writer, String name, int xver, OEDiscreteRange obj) {
		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalString (MF_TAG_KIND, MF_KEY_NULL);
		} else {
			obj.do_marshal_xver (writer, xver);
		}

		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object with external version, polymorphic.

	public static OEDiscreteRange unmarshal_xver (MarshalReader reader, String name, int xver) {
		OEDiscreteRange result = null;

		reader.unmarshalMapBegin (name);
		String key = reader.unmarshalString (MF_TAG_KIND);

		switch (key) {

		default:
			throw new MarshalException ("OEDiscreteRange.unmarshal_xver: Unknown key: key = " + key);

		case MF_KEY_NULL:
			result = null;
			break;

		case MF_KEY_SINGLE:
			result = new OEDiscreteRangeSingle();
			result.do_umarshal_xver (reader, xver);
			break;

		case MF_KEY_LINEAR:
			result = new OEDiscreteRangeLinear();
			result.do_umarshal_xver (reader, xver);
			break;

		case MF_KEY_LOG:
			result = new OEDiscreteRangeLog();
			result.do_umarshal_xver (reader, xver);
			break;
		}

		reader.unmarshalMapEnd ();
		return result;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEDiscreteRange : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  range_value
		// Test the operation of OEDiscreteRangeSingle.
		// Create the range and display it.
		// Display the size.
		// Display the range and bin arrays.
		// Marshal to JSON and display the JSON.
		// Unmarshal from JSON and display the recovered range.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 additional arguments

			if (!( args.length == 2 )) {
				System.err.println ("OEDiscreteRange : Invalid 'test1' subcommand");
				return;
			}

			try {

				double range_value = Double.parseDouble (args[1]);

				// Say hello

				System.out.println ("Testing OEDiscreteRangeSingle, single-value range");
				System.out.println ("range_value = " + range_value);

				// Create the range and display it

				OEDiscreteRange range = OEDiscreteRange.makeSingle (range_value);

				System.out.println();
				System.out.println ("range = " + range.toString());

				// Get the range size and display it

				int range_length = range.get_range_size();

				System.out.println();
				System.out.println ("range_length = " + range_length);

				// Display the range minimum and maximum

				System.out.println();
				System.out.println ("get_range_min() = " + range.get_range_min());
				System.out.println ("get_range_max() = " + range.get_range_max());

				// Get the range and bin arrays and display them

				double[] range_array = range.get_range_array();
				double[] bin_array = range.get_bin_array();
				OEValueElement[] velt_array = range.get_velt_array();

				System.out.println();
				System.out.println ("range_array.length = " + range_array.length);
				System.out.println ("bin_array.length = " + bin_array.length);
				System.out.println ("velt_array.length = " + velt_array.length);

				System.out.println();
				for (int k = 0; k < range_length; ++k) {
					System.out.println (k + ": bin = " + rndd(bin_array[k]) + ", value = " + rndd(range_array[k]) + ", velt = " + velt_array[k].rounded_string());
				}
				System.out.println (range_length + ": bin = " + rndd(bin_array[range_length]));

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_poly (store, null, range);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// Unmarshal from JSON
			
				OEDiscreteRange range2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

				// Friendly marshal to JSON

				store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_friendly (store, null, range);
				store.check_write_complete ();
				json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// Friendly unmarshal from JSON
			
				range2 = null;

				retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_friendly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

				// External version marshal to JSON

				store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_xver (store, null, OEDiscreteRange.XVER_1, range);
				store.check_write_complete ();
				json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// External version unmarshal from JSON
			
				range2 = null;

				retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_xver (retrieve, null, OEDiscreteRange.XVER_1);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  range_size range_min  range_max
		// Test the operation of OEDiscreteRangeLinear.
		// Create the range and display it.
		// Display the size.
		// Display the range and bin arrays.
		// Marshal to JSON and display the JSON.
		// Unmarshal from JSON and display the recovered range.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 3 additional arguments

			if (!( args.length == 4 )) {
				System.err.println ("OEDiscreteRange : Invalid 'test2' subcommand");
				return;
			}

			try {

				int range_size = Integer.parseInt (args[1]);
				double range_min = Double.parseDouble (args[2]);
				double range_max = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Testing OEDiscreteRangeLinear, linear range");
				System.out.println ("range_size = " + range_size);
				System.out.println ("range_min = " + range_min);
				System.out.println ("range_max = " + range_max);

				// Create the range and display it

				OEDiscreteRange range = OEDiscreteRange.makeLinear (range_size, range_min, range_max);

				System.out.println();
				System.out.println ("range = " + range.toString());

				// Get the range size and display it

				int range_length = range.get_range_size();

				System.out.println();
				System.out.println ("range_length = " + range_length);

				// Display the range minimum and maximum

				System.out.println();
				System.out.println ("get_range_min() = " + range.get_range_min());
				System.out.println ("get_range_max() = " + range.get_range_max());

				// Get the range and bin arrays and display them

				double[] range_array = range.get_range_array();
				double[] bin_array = range.get_bin_array();
				OEValueElement[] velt_array = range.get_velt_array();
				System.out.println ("velt_array.length = " + velt_array.length);

				System.out.println();
				System.out.println ("range_array.length = " + range_array.length);
				System.out.println ("bin_array.length = " + bin_array.length);

				System.out.println();
				for (int k = 0; k < range_length; ++k) {
					System.out.println (k + ": bin = " + rndd(bin_array[k]) + ", value = " + rndd(range_array[k]) + ", velt = " + velt_array[k].rounded_string());
				}
				System.out.println (range_length + ": bin = " + rndd(bin_array[range_length]));

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_poly (store, null, range);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// Unmarshal from JSON
			
				OEDiscreteRange range2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

				// Friendly marshal to JSON

				store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_friendly (store, null, range);
				store.check_write_complete ();
				json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// Friendly unmarshal from JSON
			
				range2 = null;

				retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_friendly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

				// External version marshal to JSON

				store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_xver (store, null, OEDiscreteRange.XVER_1, range);
				store.check_write_complete ();
				json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// External version unmarshal from JSON
			
				range2 = null;

				retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_xver (retrieve, null, OEDiscreteRange.XVER_1);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  range_size range_min  range_max
		// Test the operation of OEDiscreteRangeLog.
		// Create the range and display it.
		// Display the size.
		// Display the range and bin arrays.
		// Marshal to JSON and display the JSON.
		// Unmarshal from JSON and display the recovered range.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 3 additional arguments

			if (!( args.length == 4 )) {
				System.err.println ("OEDiscreteRange : Invalid 'test3' subcommand");
				return;
			}

			try {

				int range_size = Integer.parseInt (args[1]);
				double range_min = Double.parseDouble (args[2]);
				double range_max = Double.parseDouble (args[3]);

				// Say hello

				System.out.println ("Testing OEDiscreteRangeLog, logarithmic range");
				System.out.println ("range_size = " + range_size);
				System.out.println ("range_min = " + range_min);
				System.out.println ("range_max = " + range_max);

				// Create the range and display it

				OEDiscreteRange range = OEDiscreteRange.makeLog (range_size, range_min, range_max);

				System.out.println();
				System.out.println ("range = " + range.toString());

				// Get the range size and display it

				int range_length = range.get_range_size();

				System.out.println();
				System.out.println ("range_length = " + range_length);

				// Display the range minimum and maximum

				System.out.println();
				System.out.println ("get_range_min() = " + range.get_range_min());
				System.out.println ("get_range_max() = " + range.get_range_max());

				// Get the range and bin arrays and display them

				double[] range_array = range.get_range_array();
				double[] bin_array = range.get_bin_array();
				OEValueElement[] velt_array = range.get_velt_array();

				System.out.println();
				System.out.println ("range_array.length = " + range_array.length);
				System.out.println ("bin_array.length = " + bin_array.length);
				System.out.println ("velt_array.length = " + velt_array.length);

				System.out.println();
				for (int k = 0; k < range_length; ++k) {
					System.out.println (k + ": bin = " + rndd(bin_array[k]) + ", value = " + rndd(range_array[k]) + ", velt = " + velt_array[k].rounded_string());
				}
				System.out.println (range_length + ": bin = " + rndd(bin_array[range_length]));

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_poly (store, null, range);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// Unmarshal from JSON
			
				OEDiscreteRange range2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

				// Friendly marshal to JSON

				store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_friendly (store, null, range);
				store.check_write_complete ();
				json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// Friendly unmarshal from JSON
			
				range2 = null;

				retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_friendly (retrieve, null);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

				// External version marshal to JSON

				store = new MarshalImpJsonWriter();
				OEDiscreteRange.marshal_xver (store, null, OEDiscreteRange.XVER_1, range);
				store.check_write_complete ();
				json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);

				// External version unmarshal from JSON
			
				range2 = null;

				retrieve = new MarshalImpJsonReader (json_string);
				range2 = OEDiscreteRange.unmarshal_xver (retrieve, null, OEDiscreteRange.XVER_1);
				retrieve.check_read_complete ();

				System.out.println ("");
				System.out.println (range2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEDiscreteRange : Unrecognized subcommand : " + args[0]);
		return;

	}


}
