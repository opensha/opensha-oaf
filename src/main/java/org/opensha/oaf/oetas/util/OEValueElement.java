package org.opensha.oaf.oetas.util;

import java.util.Collection;
import java.util.List;
import java.util.IdentityHashMap;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;


// A parameter value, and a range which the value represents.
// Author: Michael Barall 02/19/2023.
//
// This is an immutable object.

public class OEValueElement {

	//----- Parameters -----


	// Possible kinds of elements.

	public static final int VE_SINGLE = 0;		// Element is a single point, lower limit == upper limit.
	public static final int VE_LEFT = 1;		// Value is at left endpoint of interval, value == lower limit.
	public static final int VE_RIGHT = 2;		// Value is at right endpoint of interval, value == upper limit.
	public static final int VE_INTERIOR = 3;	// Value is interior to interval, lower limit < value < uipper limit.

	private static final int VE_NULL = -1;		// Special value to indicate null object during marshal/unmarshal.


	// Kind of element, VE_XXXXX.

	private int ve_kind;

	// Value.

	private double ve_value;

	// Lower and upper limits.

	private double ve_lower;
	private double ve_upper;




	//----- Access -----


	// Get the kind of element.

	public final int get_ve_kind () {
		return ve_kind;
	}

	// Get the value.

	public final double get_ve_value () {
		return ve_value;
	}

	// Get the lower limit.

	public final double get_ve_lower () {
		return ve_lower;
	}

	// Get the upper limit.

	public final double get_ve_upper () {
		return ve_upper;
	}


	// Get the width of the element.

	public final double get_width () {
		return ve_upper - ve_lower;
	}


	// Get the width of the element, except return single_width if the interval is a single point.

	public final double get_width (double single_width) {
		return ((ve_kind == VE_SINGLE) ? single_width : (ve_upper - ve_lower));
	}


	// Get the ratio of the element linits.

	public final double get_ratio () {
		return ve_upper / ve_lower;
	}


	// Get the ratio of the element limits, except return single_ratio if the interval is a single point.

	public final double get_ratio (double single_ratio) {
		return ((ve_kind == VE_SINGLE) ? single_ratio : (ve_upper / ve_lower));
	}


	// Get the logarithm of the ratio of the element linits.

	public final double get_log_ratio () {
		return Math.log (ve_upper / ve_lower);
	}


	// Get the logarithm of the ratio of the element limits, except return single_log_ratio if the interval is a single point.

	public final double get_log_ratio (double single_log_ratio) {
		return ((ve_kind == VE_SINGLE) ? single_log_ratio : (ve_upper / ve_lower));
	}




	//----- Construction -----


	// Make a single point element.

	public OEValueElement (double value) {
		ve_kind = VE_SINGLE;
		ve_lower = value;
		ve_value = value;
		ve_upper = value;
	}


	// Make an element with the given value and limits.
	// The kind is set according to whether the value is equal to either or both limits.

	public OEValueElement (double lower, double value, double upper) {
		if (value == lower) {
			if (value == upper) {
				ve_kind = VE_SINGLE;
				ve_lower = value;
				ve_value = value;
				ve_upper = value;
			}
			else if (value > upper) {
				throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: lower = " + lower + ", value = " + value + ", upper = " + upper);
			}
			else {	// value < upper
				ve_kind = VE_LEFT;
				ve_lower = value;
				ve_value = value;
				ve_upper = upper;
			}
		}
		else if (value < lower) {
			throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: lower = " + lower + ", value = " + value + ", upper = " + upper);
		}
		else {	// value > lower
			if (value == upper) {
				ve_kind = VE_RIGHT;
				ve_lower = lower;
				ve_value = value;
				ve_upper = value;
			}
			else if (value > upper) {
				throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: lower = " + lower + ", value = " + value + ", upper = " + upper);
			}
			else {	// value < upper
				ve_kind = VE_INTERIOR;
				ve_lower = lower;
				ve_value = value;
				ve_upper = upper;
			}
		}
	}


	// Make an element with the given value and limits.
	// The kind is set according to whether the value is equal to either or both limits.
	// Two numbers are considered equal if their difference is <= eps.

	public OEValueElement (double lower, double value, double upper, double eps) {
		if (Math.abs (value - lower) <= eps) {
			if (Math.abs (value - upper) <= eps) {
				ve_kind = VE_SINGLE;
				ve_lower = value;
				ve_value = value;
				ve_upper = value;
			}
			else if (value > upper) {
				throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: lower = " + lower + ", value = " + value + ", upper = " + upper + ", eps = " + eps);
			}
			else {	// value < upper
				ve_kind = VE_LEFT;
				ve_lower = value;
				ve_value = value;
				ve_upper = upper;
			}
		}
		else if (value < lower) {
			throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: lower = " + lower + ", value = " + value + ", upper = " + upper + ", eps = " + eps);
		}
		else {	// value > lower
			if (Math.abs (value - upper) <= eps) {
				ve_kind = VE_RIGHT;
				ve_lower = lower;
				ve_value = value;
				ve_upper = value;
			}
			else if (value > upper) {
				throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: lower = " + lower + ", value = " + value + ", upper = " + upper + ", eps = " + eps);
			}
			else {	// value < upper
				ve_kind = VE_INTERIOR;
				ve_lower = lower;
				ve_value = value;
				ve_upper = upper;
			}
		}
	}


	// Make an element with the given limits.
	// The value is at an endpoint, the left endpoint if f_left is true, the right endpoint if f_left is false.
	// The kind is set according to whether the limits are equal.

	public OEValueElement (boolean f_left, double lower, double upper) {
		if (lower == upper) {
			if (f_left) {
				ve_kind = VE_SINGLE;
				ve_lower = lower;
				ve_value = lower;
				ve_upper = lower;
			} else {
				ve_kind = VE_SINGLE;
				ve_lower = upper;
				ve_value = upper;
				ve_upper = upper;
			}
		}
		else if (lower > upper) {
			throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: f_left = " + f_left + ", lower = " + lower + ", upper = " + upper);
		}
		else {	// lower < upper
			if (f_left) {
				ve_kind = VE_LEFT;
				ve_lower = lower;
				ve_value = lower;
				ve_upper = upper;
			} else {
				ve_kind = VE_RIGHT;
				ve_lower = lower;
				ve_value = upper;
				ve_upper = upper;
			}
		}
	}


	// Make an element with the given limits.
	// The value is at an endpoint, the left endpoint if f_left is true, the right endpoint if f_left is false.
	// The kind is set according to whether the limits are equal.
	// Two numbers are considered equal if their difference is <= eps.

	public OEValueElement (boolean f_left, double lower, double upper, double eps) {
		if (Math.abs (lower - upper) <= eps) {
			if (f_left) {
				ve_kind = VE_SINGLE;
				ve_lower = lower;
				ve_value = lower;
				ve_upper = lower;
			} else {
				ve_kind = VE_SINGLE;
				ve_lower = upper;
				ve_value = upper;
				ve_upper = upper;
			}
		}
		else if (lower > upper) {
			throw new IllegalArgumentException ("OEValueElement.OEValueElement: Invalid limits: f_left = " + f_left + ", lower = " + lower + ", upper = " + upper + ", eps = " + eps);
		}
		else {	// lower < upper
			if (f_left) {
				ve_kind = VE_LEFT;
				ve_lower = lower;
				ve_value = lower;
				ve_upper = upper;
			} else {
				ve_kind = VE_RIGHT;
				ve_lower = lower;
				ve_value = upper;
				ve_upper = upper;
			}
		}
	}




	//----- Display -----


	// Convert a kind to a string.

	public static String kind_to_string (int kind) {
		switch (kind) {
		case VE_SINGLE: return "VE_SINGLE";
		case VE_LEFT: return "VE_LEFT";
		case VE_RIGHT: return "VE_RIGHT";
		case VE_INTERIOR: return "VE_INTERIOR";
		}
		return "VE_INVALID(" + kind + ")";
	}


	// Return a string for this object's kind.

	public final String get_ve_kind_string () {
		return kind_to_string (ve_kind);
	}


	// Convert a kind to a shorter string.

	public static String kind_to_shorter_string (int kind) {
		switch (kind) {
		case VE_SINGLE: return "S";
		case VE_LEFT: return "L";
		case VE_RIGHT: return "R";
		case VE_INTERIOR: return "I";
		}
		return "X(" + kind + ")";
	}


	// Return a shorter string for this object's kind.

	public final String get_ve_kind_shorter_string () {
		return kind_to_shorter_string (ve_kind);
	}


	// Display our contents.

	@Override
	public String toString() {
		return "OEValueElement[ve_kind = " + kind_to_string (ve_kind)
		+ ", ve_value = " + ve_value
		+ ", ve_lower = " + ve_lower
		+ ", ve_upper = " + ve_upper
		+ "]";
	}


	// Display our contents, with rounded values.

	public String rounded_string () {
		return "OEValueElement[ve_kind = " + kind_to_string (ve_kind)
		+ ", ve_value = " + rndd(ve_value)
		+ ", ve_lower = " + rndd(ve_lower)
		+ ", ve_upper = " + rndd(ve_upper)
		+ "]";
	}


	// Display a shortened version our contents, with rounded values.

	public String shortened_string () {
		return kind_to_string (ve_kind)
		+ "[" + rndd(ve_value)
		+ ", " + rndd(ve_lower)
		+ ", " + rndd(ve_upper)
		+ "]";
	}


	// Display an even shorter version our contents, with rounded values.

	public String shorter_string () {
		return kind_to_shorter_string (ve_kind)
		+ "[" + rndf(ve_value)
		+ ", " + rndf(ve_lower)
		+ ", " + rndf(ve_upper)
		+ "]";
	}




	//----- Marshaling -----


	// Marshal the contents of an object.

	private void do_marshal (MarshalWriter writer) {
		writer.marshalInt ("ve_kind", ve_kind);
		switch (ve_kind) {
		case VE_SINGLE:
			writer.marshalDouble ("ve_value", ve_value);
			break;
		case VE_LEFT:
			writer.marshalDouble ("ve_value", ve_value);
			writer.marshalDouble ("ve_upper", ve_upper);
			break;
		case VE_RIGHT:
			writer.marshalDouble ("ve_lower", ve_lower);
			writer.marshalDouble ("ve_value", ve_value);
			break;
		case VE_INTERIOR:
			writer.marshalDouble ("ve_lower", ve_lower);
			writer.marshalDouble ("ve_value", ve_value);
			writer.marshalDouble ("ve_upper", ve_upper);
			break;
		default:
			throw new MarshalException ("OEValueElement.do_marshal: Object contains invalid kind: ve_kind = " + ve_kind);
		}
		return;
	}


	// Unmarshal the contents of an object (the kind is already unmarshaled).

	private OEValueElement (MarshalReader reader, int kind) {
		ve_kind = kind;
		switch (ve_kind) {
		case VE_SINGLE:
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_lower = ve_value;
			ve_upper = ve_value;
			break;
		case VE_LEFT:
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_upper = reader.unmarshalDouble ("ve_upper");
			ve_lower = ve_value;
			break;
		case VE_RIGHT:
			ve_lower = reader.unmarshalDouble ("ve_lower");
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_upper = ve_value;
			break;
		case VE_INTERIOR:
			ve_lower = reader.unmarshalDouble ("ve_lower");
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_upper = reader.unmarshalDouble ("ve_upper");
			break;
		default:
			throw new MarshalException ("OEValueElement.OEValueElement: Object contains invalid kind: ve_kind = " + ve_kind);
		}
		return;
	}


	// Constructor for unmarshaling.

	private OEValueElement () {
	}


	// Unmarshal the contents of an object.

	private void do_umarshal (MarshalReader reader) {
		ve_kind = reader.unmarshalInt ("ve_kind");
		switch (ve_kind) {
		case VE_SINGLE:
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_lower = ve_value;
			ve_upper = ve_value;
			break;
		case VE_LEFT:
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_upper = reader.unmarshalDouble ("ve_upper");
			ve_lower = ve_value;
			break;
		case VE_RIGHT:
			ve_lower = reader.unmarshalDouble ("ve_lower");
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_upper = ve_value;
			break;
		case VE_INTERIOR:
			ve_lower = reader.unmarshalDouble ("ve_lower");
			ve_value = reader.unmarshalDouble ("ve_value");
			ve_upper = reader.unmarshalDouble ("ve_upper");
			break;
		default:
			throw new MarshalException ("OEValueElement.do_umarshal: Object contains invalid kind: ve_kind = " + ve_kind);
		}
		return;
	}


	// Marshal object, or null.

	public static void marshal_obj (MarshalWriter writer, String name, OEValueElement obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt ("ve_kind", VE_NULL);
		} else {
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, or null.

	public static OEValueElement unmarshal_obj (MarshalReader reader, String name) {
		OEValueElement result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int kind = reader.unmarshalInt ("ve_kind");

		if (kind == VE_NULL) {
			result = null;
		} else {
			result = new OEValueElement (reader, kind);
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal a list of objects.

	public static void marshal_list (MarshalWriter writer, String name, Collection<OEValueElement> obj_list) {
		int n = obj_list.size();
		writer.marshalArrayBegin (name, n);
		for (OEValueElement obj : obj_list) {
			marshal_obj (writer, null, obj);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a list of objects.  (Unmarshaled objects are added to the given collection.)

	public static void unmarshal_list (MarshalReader reader, String name, Collection<OEValueElement> obj_list) {
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			obj_list.add (unmarshal_obj (reader, null));
		}
		reader.unmarshalArrayEnd ();
		return;
	}

	// Marshal an array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OEValueElement[] obj_list) {
		int n = obj_list.length;
		writer.marshalArrayBegin (name, n);
		for (OEValueElement obj : obj_list) {
			marshal_obj (writer, null, obj);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects.

	public static OEValueElement[] unmarshal_array (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		OEValueElement[] obj_list = new OEValueElement[n];
		for (int i = 0; i < n; ++i) {
			obj_list[i] = unmarshal_obj (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return obj_list;
	}


	// Marshal object, or null, with de-duplication.

	public static void marshal_obj (MarshalWriter writer, String name, OEValueElement obj, IdentityHashMap<Object, Integer> dedup) {

		writer.marshalMapBegin (name);

		if (obj == null) {

			// Null object has -1 as index

			writer.marshalInt ("$", -1);

		} else {
			Integer x = dedup.get (obj);
			if (x != null) {

				// Duplicated object has just the index

				writer.marshalInt ("$", x);

			} else {

				// New object has the new index followed by the object contents

				int k = dedup.size();
				dedup.put (obj, k);
				writer.marshalInt ("$", k);
				obj.do_marshal (writer);

			}
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, or null, with de-duplication.

	public static OEValueElement unmarshal_obj (MarshalReader reader, String name, List<Object> dedup) {
		OEValueElement result;

		reader.unmarshalMapBegin (name);

		int k = dedup.size();
		int dup_ix = reader.unmarshalInt ("$", -1, k);

		if (dup_ix == -1) {

			// Null object has -1 as index

			result = null;

		} else if (dup_ix < k) {

			// Duplicated object has just the index

			try {
				result = (OEValueElement)(dedup.get (dup_ix));
			}
			catch (Exception e) {
				throw new MarshalException ("OEValueElement.unmarshal_obj: Duplicated object is not of expected type", e);
			}

		} else {

			// New object has the new index followed by the object contents

			result = new OEValueElement();
			dedup.add (result);
			result.do_umarshal (reader);
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal an array of objects, or null, with de-duplication.

	public static void marshal_array (MarshalWriter writer, String name, OEValueElement[] obj_list, IdentityHashMap<Object, Integer> dedup) {

		if (obj_list == null) {

			// Null list is an empty array

			writer.marshalArrayBegin (name, 0);

		} else {
			Integer x = dedup.get (obj_list);
			if (x != null) {

				// Duplicated list is a one-element array containing the index

				writer.marshalArrayBegin (name, 1);
				writer.marshalInt (null, x);

			} else {

				// New list is an array containing the new index followed by the list elements

				int k = dedup.size();
				dedup.put (obj_list, k);

				int n = obj_list.length;
				writer.marshalArrayBegin (name, n + 1);
				writer.marshalInt (null, k);
				for (OEValueElement obj : obj_list) {
					marshal_obj (writer, null, obj, dedup);
				}
			}
		}

		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects, or null, with de-duplication.

	public static OEValueElement[] unmarshal_array (MarshalReader reader, String name, List<Object> dedup) {
		OEValueElement[] obj_list;
		int n = reader.unmarshalArrayBegin (name);

		if (n == 0) {

			// Null list is an empty array

			obj_list = null;

		} else {
			int k = dedup.size();
			int dup_ix = reader.unmarshalInt (null, 0, k);
			if (dup_ix < k) {

				// Duplicated list is a one-element array containing the index

				if (n != 1) {
					throw new MarshalException ("OEValueElement.unmarshal_array: Duplicated list is not a one-element array, n = " + n);
				}
				try {
					obj_list = (OEValueElement[])(dedup.get (dup_ix));
				}
				catch (Exception e) {
					throw new MarshalException ("OEValueElement.unmarshal_array: Duplicated object is not of expected type", e);
				}

			} else {

				// New list is an array containing the new index followed by the list elements

				int len = n - 1;
				obj_list = new OEValueElement[len];
				dedup.add (obj_list);
				for (int i = 0; i < len; ++i) {
					obj_list[i] = unmarshal_obj (reader, null, dedup);
				}
			}
		}

		reader.unmarshalArrayEnd ();
		return obj_list;
	}

}
