package org.opensha.oaf.util;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.Closeable;
import java.util.function.Supplier;

/**
 * Interface for unmarshaling parameters/data from the OAF database.
 * Author: Michael Barall 03/31/2018.
 */
public interface MarshalReader {

	/**
	 * Begin a map context.
	 */
	public void unmarshalMapBegin (String name);

	/**
	 * End a map context.
	 */
	public void unmarshalMapEnd ();

	/**
	 * Begin an array context, return the array size.
	 */
	public int unmarshalArrayBegin (String name);

	/**
	 * End an array context.
	 */
	public void unmarshalArrayEnd ();

	/**
	 * Unmarshal a long.
	 */
	public long unmarshalLong (String name);

	/**
	 * Unmarshal a double.
	 */
	public double unmarshalDouble (String name);

	/**
	 * Unmarshal a string.  (Null strings are not allowed.)
	 */
	public String unmarshalString (String name);

	/**
	 * Unmarshal a long, with required minimum value.
	 */
	public default long unmarshalLong (String name, long minValue) {
		long x = unmarshalLong (name);
		if (x < minValue) {
			throw new MarshalException ("Unmarshaled long out-of-range: value = " + x + ", min = " + minValue);
		}
		return x;
	}

	/**
	 * Unmarshal a long, with required minimum and maximum values.
	 */
	public default long unmarshalLong (String name, long minValue, long maxValue) {
		long x = unmarshalLong (name);
		if (x < minValue || x > maxValue) {
			throw new MarshalException ("Unmarshaled long out-of-range: value = " + x + ", min = " + minValue + ", max = " + maxValue);
		}
		return x;
	}

	/**
	 * Unmarshal an int.
	 */
	public default int unmarshalInt (String name) {
		long x = unmarshalLong (name);
		if (x < (long)Integer.MIN_VALUE || x > (long)Integer.MAX_VALUE) {
			throw new MarshalException ("Unmarshaled int out-of-range: value = " + x + ", min = " + Integer.MIN_VALUE + ", max = " + Integer.MAX_VALUE);
		}
		return (int)x;
	}

	/**
	 * Unmarshal an int, with required minimum value.
	 */
	public default int unmarshalInt (String name, int minValue) {
		long x = unmarshalLong (name);
		if (x < (long)minValue || x > (long)Integer.MAX_VALUE) {
			throw new MarshalException ("Unmarshaled int out-of-range: value = " + x + ", min = " + minValue + ", max = " + Integer.MAX_VALUE);
		}
		return (int)x;
	}

	/**
	 * Unmarshal an int, with required minimum and maximum values.
	 */
	public default int unmarshalInt (String name, int minValue, int maxValue) {
		long x = unmarshalLong (name);
		if (x < (long)minValue || x > (long)maxValue) {
			throw new MarshalException ("Unmarshaled int out-of-range: value = " + x + ", min = " + minValue + ", max = " + maxValue);
		}
		return (int)x;
	}

	/**
	 * Unmarshal a boolean.
	 */
	public default boolean unmarshalBoolean (String name) {
		long x = unmarshalLong (name);
		if (x == 0L) {
			return false;
		}
		if (x == 1L) {
			return true;
		}
		throw new MarshalException ("Unmarshaled boolean out-of-range: value = " + x + ", min = 0, max = 1");
	}

	/**
	 * Unmarshal a float.
	 */
	public default float unmarshalFloat (String name) {
		double x = unmarshalDouble (name);
		return (float)x;
	}

	/**
	 * Unmarshal a JSON string.  (Null strings are not allowed.)
	 * The string must contain a JSON object or array, or be an empty string.
	 * For JSON storage, the string is merged into the JSON instead of being
	 * embedded as string-valued data.  (An empty string becomes a JSON null.)
	 * The unmarshaled string may differ from the marshaled string due to JSON parsing.
	 * (Named element ordering, numeric formats, and spacing may be changed).
	 */
	public default String unmarshalJsonString (String name) {
		return unmarshalString (name);
	}

	/**
	 * Unmarshal a long array.
	 */
	public default long[] unmarshalLongArray (String name) {
		int n = unmarshalArrayBegin (name);
		long[] x = new long[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalLong (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default long[][] unmarshalLong2DArray (String name) {
		int n = unmarshalArrayBegin (name);
		long[][] x = new long[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalLongArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default long[][][] unmarshalLong3DArray (String name) {
		int n = unmarshalArrayBegin (name);
		long[][][] x = new long[n][][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalLong2DArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	/**
	 * Unmarshal a double array.
	 */
	public default double[] unmarshalDoubleArray (String name) {
		int n = unmarshalArrayBegin (name);
		double[] x = new double[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalDouble (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default double[][] unmarshalDouble2DArray (String name) {
		int n = unmarshalArrayBegin (name);
		double[][] x = new double[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalDoubleArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default double[][][] unmarshalDouble3DArray (String name) {
		int n = unmarshalArrayBegin (name);
		double[][][] x = new double[n][][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalDouble2DArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	/**
	 * Unmarshal a string array.  (Null strings are not allowed.)
	 */
	public default String[] unmarshalStringArray (String name) {
		int n = unmarshalArrayBegin (name);
		String[] x = new String[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalString (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default String[][] unmarshalString2DArray (String name) {
		int n = unmarshalArrayBegin (name);
		String[][] x = new String[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalStringArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default String[][][] unmarshalString3DArray (String name) {
		int n = unmarshalArrayBegin (name);
		String[][][] x = new String[n][][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalString2DArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	/**
	 * Unmarshal an int array.
	 */
	public default int[] unmarshalIntArray (String name) {
		int n = unmarshalArrayBegin (name);
		int[] x = new int[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalInt (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default int[][] unmarshalInt2DArray (String name) {
		int n = unmarshalArrayBegin (name);
		int[][] x = new int[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalIntArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default int[][][] unmarshalInt3DArray (String name) {
		int n = unmarshalArrayBegin (name);
		int[][][] x = new int[n][][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalInt2DArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	/**
	 * Unmarshal a boolean array.
	 */
	public default boolean[] unmarshalBooleanArray (String name) {
		int n = unmarshalArrayBegin (name);
		boolean[] x = new boolean[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalBoolean (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default boolean[][] unmarshalBoolean2DArray (String name) {
		int n = unmarshalArrayBegin (name);
		boolean[][] x = new boolean[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalBooleanArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default boolean[][][] unmarshalBoolean3DArray (String name) {
		int n = unmarshalArrayBegin (name);
		boolean[][][] x = new boolean[n][][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalBoolean2DArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	/**
	 * Unmarshal a float array.
	 */
	public default float[] unmarshalFloatArray (String name) {
		int n = unmarshalArrayBegin (name);
		float[] x = new float[n];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalFloat (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default float[][] unmarshalFloat2DArray (String name) {
		int n = unmarshalArrayBegin (name);
		float[][] x = new float[n][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalFloatArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	public default float[][][] unmarshalFloat3DArray (String name) {
		int n = unmarshalArrayBegin (name);
		float[][][] x = new float[n][][];
		for (int i = 0; i < n; ++i) {
			x[i] = unmarshalFloat2DArray (null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	/**
	 * Unmarshal a long collection.
	 */
	public default void unmarshalLongCollection (String name, Collection<Long> x) {
		int n = unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (Long.valueOf (unmarshalLong (null)));
		}
		unmarshalArrayEnd ();
		return;
	}

	/**
	 * Unmarshal a double collection.
	 */
	public default void unmarshalDoubleCollection (String name, Collection<Double> x) {
		int n = unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (Double.valueOf (unmarshalDouble (null)));
		}
		unmarshalArrayEnd ();
		return;
	}

	/**
	 * Unmarshal a string collection.  (Null strings are not allowed.)
	 */
	public default void unmarshalStringCollection (String name, Collection<String> x) {
		int n = unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (unmarshalString (null));
		}
		unmarshalArrayEnd ();
		return;
	}

	/**
	 * Unmarshal an int collection.
	 */
	public default void unmarshalIntCollection (String name, Collection<Integer> x) {
		int n = unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (Integer.valueOf (unmarshalInt (null)));
		}
		unmarshalArrayEnd ();
		return;
	}

	/**
	 * Unmarshal a float collection.
	 */
	public default void unmarshalFloatCollection (String name, Collection<Float> x) {
		int n = unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (Float.valueOf (unmarshalFloat (null)));
		}
		unmarshalArrayEnd ();
		return;
	}


	//----- Extended JSON support -----

	// Begin a map context.
	// If keys is non-null, then all the JSON keys are added to the collection.

	public default void  unmarshalJsonMapBegin (String name, Collection<String> keys) {
		throw new MarshalUnsupportedException ("unmarshalJsonMapBegin is not supported");
	}

	// End a map context.
	// If f_check_keys is true, then throw an exception if any keys were not used.
	// Note that f_check_keys = true gives the same behavior as unmarshalMapEnd.

	public default void unmarshalJsonMapEnd (boolean f_check_keys) {
		throw new MarshalUnsupportedException ("unmarshalJsonMapEnd is not supported");
	}

	// Unmarshal a JSON null value.

	public default void unmarshalJsonNull (String name) {
		throw new MarshalUnsupportedException ("unmarshalJsonNull is not supported");
	}

	// Get the type of the next object to be read.
	// This function does not consume the next object.

	public static final int JPT_NULL = 0;
	public static final int JPT_INTEGER = 1;
	public static final int JPT_LONG = 2;
	public static final int JPT_FLOAT = 3;
	public static final int JPT_DOUBLE = 4;
	public static final int JPT_BOOLEAN = 5;
	public static final int JPT_STRING = 6;

	public static final int JPT_MAP = 7;
	public static final int JPT_ARRAY = 8;

	public default int unmarshalJsonPeekType (String name) {
		throw new MarshalUnsupportedException ("unmarshalJsonPeekType is not supported");
	}

	// Unmarshal a JSON scalar.
	// The returned object may be one of the following types:
	//  null
	//  Integer
	//  Long
	//  Float
	//  Double
	//  Boolean
	//  String
	// Note: For numeric types, the type of object returned may be different from
	// the type of object suppled to marshalJsonScalar(), but the value is the same.

	public default Object unmarshalJsonScalar (String name) {
		int jpt = unmarshalJsonPeekType (name);
		Object x = null;

		switch (jpt) {

		case JPT_NULL:
			unmarshalJsonNull (name);
			break;

		case JPT_INTEGER:
			x = unmarshalInt (name);
			break;

		case JPT_LONG:
			x = unmarshalLong (name);
			break;

		case JPT_FLOAT:
			x = unmarshalFloat (name);
			break;

		case JPT_DOUBLE:
			x = unmarshalDouble (name);
			break;

		case JPT_BOOLEAN:
			x = unmarshalBoolean (name);
			break;

		case JPT_STRING:
			x = unmarshalString (name);
			break;

		default:
			throw new MarshalException ("unmarshalJsonScalar found an invalid object type code: " + jpt);
		}

		return x;
	}

	// Unmarshal a JSON scalar, required to be non-null.
	// The returned object may be one of the following types:
	//  Integer
	//  Long
	//  Float
	//  Double
	//  Boolean
	//  String
	// Note: For numeric types, the type of object returned may be different from
	// the type of object suppled to marshalJsonScalar(), but the value is the same.

	public default Object unmarshalJsonScalarNonNull (String name) {
		Object x = unmarshalJsonScalar (name);
		if (x == null) {
			throw new MarshalException ("unmarshalJsonScalarNonNull found a null object");
		}
		return x;
	}


	//----- Control functions -----

	// Check read completion status.
	// Throw exception if the current top-level object is incomplete.
	// Returns the number of top level object read (which can be 0L if
	// nothing has been read), or -1L if the number is unknown.
	// Note that some readers are limited to a single top-level object.
	// If f_require_eof is true, throw exception if the data source is not
	// fully consumed.  Note that not all readers can perform this check.
	// This function should be called when finished using the reader.

	public long read_completion_check (boolean f_require_eof);

	// Attempt to close the data source, throw exception if error.
	// Performs no operation if the reader cannot do this.

	public default void close_reader () {
		try {
			if (this instanceof Closeable) {
				((Closeable)this).close();
			}
		}
		catch (Exception e) {
			throw new MarshalException ("Error attempting to close reader", e);
		}
		return;
	}


	//----- Object arrays, lists, and individual objects -----

//	// Unmarshal an array of objects of type T.
//	// Each object is unmarshaled using a function with signature
//	//   (MarshalReader reader, String name) -> S.
//	// This is the typical signature of a static unmarshaling method.
//	// Here S is a subclass of T, so objects of type S can be assigned
//	// to variables of type T.  Typically S = T.
//	// See MarshalUtils for ways to adapt an instance unmarshaling method.
//	// If the prototype array is the correct size then it is used to return
//	// the result, otherwise a new array of the same run-time type is allocated.
//
//	public default <T> T[] unmarshalObjectArray (String name, T[] prototype, MarshalUtils.UnmarshalFunction<? extends T> func) {
//		int n = unmarshalArrayBegin (name);
//		T[] x =  SimpleUtils.optional_new_array_of_same_type (prototype, n);
//		for (int i = 0; i < n; ++i) {
//			x[i] = func.lambda_unmarshal (this, null);
//		}
//		unmarshalArrayEnd ();
//		return x;
//	}

	// Unmarshal an array of objects of type T.
	// Each object is unmarshaled using a function with signature
	//   (MarshalReader reader, String name) -> S.
	// This is the typical signature of a static unmarshaling method.
	// Here S is a subclass of T, so objects of type S can be assigned
	// to variables of type T.  Typically S = T.
	// See MarshalUtils for ways to adapt an instance unmarshaling method.
	// The Class object determines the type of array returned.

	public default <T> T[] unmarshalObjectArray (String name, Class<T> clazz, MarshalUtils.UnmarshalFunction<? extends T> func) {
		int n = unmarshalArrayBegin (name);
		T[] x =  SimpleUtils.new_array_of_type (clazz, n);
		for (int i = 0; i < n; ++i) {
			x[i] = func.lambda_unmarshal (this, null);
		}
		unmarshalArrayEnd ();
		return x;
	}

	// Unmarshal a collection of objects of type T.
	// Each object is unmarshaled using a function with signature
	//   (MarshalReader reader, String name) -> S.
	// This is the typical signature of a static unmarshaling method.
	// Here S is a subclass of T, so objects of type S can be assigned
	// to variables of type T.  Typically S = T.
	// See MarshalUtils for ways to adapt an instance unmarshaling method.

	public default <T> void unmarshalObjectCollection (String name, Collection<T> x, MarshalUtils.UnmarshalFunction<? extends T> func) {
		int n = unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			x.add (func.lambda_unmarshal (this, null));
		}
		unmarshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects of type T that implements Marshalable.
	// Each object is unmarshaled using Marshalable.unmarshal.
	// The Class object specifies the type, and is used to create the array
	// and the unmarshaled objects.

	public default <T extends Marshalable> T[] unmarshalObjectArray (String name, Class<T> clazz) {
		int n = unmarshalArrayBegin (name);
		T[] x = SimpleUtils.new_array_of_type (clazz, n);
		try {
			for (int i = 0; i < n; ++i) {
				T obj = clazz.getDeclaredConstructor().newInstance();
				obj.unmarshal (this, null);
				x[i] = obj;
			}
		}
		catch (Exception e) {
			throw new MarshalException ("MarshalReader.unmarshalObjectArray: Unable to allocate new object: class = " + clazz.getName(), e);
		}
		unmarshalArrayEnd ();
		return x;
	}

	// Unmarshal a list of objects of type T that implements Marshalable.
	// Each object is unmarshaled using Marshalable.unmarshal.
	// The Class object specifies the type, and is used to create the
	// unmarshaled objects.

	public default <T extends Marshalable> ArrayList<T> unmarshalObjectList (String name, Class<T> clazz) {
		int n = unmarshalArrayBegin (name);
		ArrayList<T> x = new ArrayList<T>();
		try {
			for (int i = 0; i < n; ++i) {
				T obj = clazz.getDeclaredConstructor().newInstance();
				obj.unmarshal (this, null);
				x.add (obj);
			}
		}
		catch (Exception e) {
			throw new MarshalException ("MarshalReader.unmarshalObjectList: Unable to allocate new object: class = " + clazz.getName(), e);
		}
		unmarshalArrayEnd ();
		return x;
	}

	// Unmarshal an object of type T.
	// The object is unmarshaled using a function with signature
	//   (MarshalReader reader, String name) -> T.
	// This is the typical signature of a static unmarshaling method.
	// See MarshalUtils for ways to adapt an instance unmarshaling method.

	public default <T> T unmarshalObject (String name, MarshalUtils.UnmarshalFunction<T> func) {
		return func.lambda_unmarshal (this, name);
	}

	// Unmarshal an objects of type T that implements Marshalable.
	// The object is unmarshaled using Marshalable.unmarshal.
	// The Class object specifies the type, and is used to create the object.

	public default <T extends Marshalable> T unmarshalObject (String name, Class<T> clazz) {
		T obj;
		try {
			obj = clazz.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new MarshalException ("MarshalReader.unmarshalObject: Unable to allocate new object: class = " + clazz.getName(), e);
		}
		obj.unmarshal (this, name);
		return obj;
	}

}
