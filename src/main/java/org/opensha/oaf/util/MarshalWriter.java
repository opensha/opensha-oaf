package org.opensha.oaf.util;

import java.util.Collection;
import java.io.Closeable;

/**
 * Interface for marshaling parameters/data to the OAF database.
 * Author: Michael Barall 03/31/2018.
 */
public interface MarshalWriter {

	/**
	 * Begin a map context.
	 */
	public void marshalMapBegin (String name);

	/**
	 * End a map context.
	 */
	public void marshalMapEnd ();

	/**
	 * Begin an array context, specify the array size.
	 */
	public void marshalArrayBegin (String name, int array_size);

	/**
	 * End an array context.
	 */
	public void marshalArrayEnd ();

	/**
	 * Marshal a long.
	 */
	public void marshalLong (String name, long x);

	/**
	 * Marshal a double.
	 */
	public void marshalDouble (String name, double x);

	/**
	 * Marshal a string.  (Null strings are not allowed.)
	 */
	public void marshalString (String name, String x);

	/**
	 * Marshal an int.
	 */
	public default void marshalInt (String name, int x) {
		marshalLong (name, (long)x);
		return;
	}

	/**
	 * Marshal a boolean.
	 */
	public default void marshalBoolean (String name, boolean x) {
		marshalLong (name, x ? 1L : 0L);
		return;
	}

	/**
	 * Marshal a float.
	 */
	public default void marshalFloat (String name, float x) {
		marshalDouble (name, (double)x);
		return;
	}

	/**
	 * Marshal a JSON string.  (Null strings are not allowed.)
	 * The string must contain a JSON object or array, or be an empty string.
	 * For JSON storage, the string is merged into the JSON instead of being
	 * embedded as string-valued data.  (An empty string becomes a JSON null.)
	 * The unmarshaled string may differ from the marshaled string due to JSON parsing.
	 * (Named element ordering, numeric formats, and spacing may be changed).
	 */
	public default void marshalJsonString (String name, String x) {
		marshalString (name, x);
		return;
	}

	/**
	 * Marshal a long array.
	 */
	public default void marshalLongArray (String name, long[] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalLong (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalLong2DArray (String name, long[][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalLongArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalLong3DArray (String name, long[][][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalLong2DArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal a double array.
	 */
	public default void marshalDoubleArray (String name, double[] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalDouble (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalDouble2DArray (String name, double[][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalDoubleArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalDouble3DArray (String name, double[][][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalDouble2DArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal a string array.  (Null strings are not allowed.)
	 */
	public default void marshalStringArray (String name, String[] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalString (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}
	public default void marshalString2DArray (String name, String[][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalStringArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalString3DArray (String name, String[][][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalString2DArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}


	/**
	 * Marshal an int array.
	 */
	public default void marshalIntArray (String name, int[] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalInt (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalInt2DArray (String name, int[][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalIntArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalInt3DArray (String name, int[][][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalInt2DArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}


	/**
	 * Marshal a boolean array.
	 */
	public default void marshalBooleanArray (String name, boolean[] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalBoolean (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalBoolean2DArray (String name, boolean[][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalBooleanArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalBoolean3DArray (String name, boolean[][][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalBoolean2DArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}


	/**
	 * Marshal a float array.
	 */
	public default void marshalFloatArray (String name, float[] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalFloat (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalFloat2DArray (String name, float[][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalFloatArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	public default void marshalFloat3DArray (String name, float[][][] x) {
		int n = x.length;
		marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			marshalFloat2DArray (null, x[i]);
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal a long collection.
	 */
	public default void marshalLongCollection (String name, Collection<Long> x) {
		int n = x.size();
		marshalArrayBegin (name, n);
		for (Long y : x) {
			 marshalLong (null, y.longValue());
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal a double collection.
	 */
	public default void marshalDoubleCollection (String name, Collection<Double> x) {
		int n = x.size();
		marshalArrayBegin (name, n);
		for (Double y : x) {
			 marshalDouble (null, y.doubleValue());
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal a string collection.  (Null strings are not allowed.)
	 */
	public default void marshalStringCollection (String name, Collection<String> x) {
		int n = x.size();
		marshalArrayBegin (name, n);
		for (String y : x) {
			 marshalString (null, y);
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal an int collection.
	 */
	public default void marshalIntCollection (String name, Collection<Integer> x) {
		int n = x.size();
		marshalArrayBegin (name, n);
		for (Integer y : x) {
			 marshalInt (null, y.intValue());
		}
		marshalArrayEnd ();
		return;
	}

	/**
	 * Marshal a float collection.
	 */
	public default void marshalFloatCollection (String name, Collection<Float> x) {
		int n = x.size();
		marshalArrayBegin (name, n);
		for (Float y : x) {
			 marshalFloat (null, y.floatValue());
		}
		marshalArrayEnd ();
		return;
	}


	//----- Extended JSON support -----

	// Marshal a JSON null value.

	public default void marshalJsonNull (String name) {
		throw new MarshalUnsupportedException ("marshalJsonNull is not supported");
	}

	// Marshal a JSON scalar.
	// The object may be one of the following types:
	//  null
	//  Integer
	//  Long
	//  Float
	//  Double
	//  Boolean
	//  String

	public default void marshalJsonScalar (String name, Object x) {
		if (x == null) {
			marshalJsonNull (name);
		}
		else if (x instanceof Integer) {
			marshalInt (name, (Integer)x);
		}
		else if (x instanceof Long) {
			marshalLong (name, (Long)x);
		}
		else if (x instanceof Float) {
			marshalFloat (name, (Float)x);
		}
		else if (x instanceof Double) {
			marshalDouble (name, (Double)x);
		}
		else if (x instanceof Boolean) {
			marshalBoolean (name, (Boolean)x);
		}
		else if (x instanceof String) {
			marshalString (name, (String)x);
		}
		else {
			throw new MarshalException ("marshalJsonScalar called with an invalid type");
		}
		return;
	}


	//----- Control functions -----

	// Check write completion status.
	// Throw exception if the current top-level object is incomplete.
	// Returns the number of top level object written (which can be 0L if
	// nothing has been written), or -1L if the number is unknown.
	// Note that some writers are limited to a single top-level object.
	// This function should be called when finished using the writer.

	public long write_completion_check ();

	// Attempt to close the data destination, throw exception if error.
	// Performs no operation if the reader cannot do this.

	public default void close_writer () {
		try {
			if (this instanceof Closeable) {
				((Closeable)this).close();
			}
		}
		catch (Exception e) {
			throw new MarshalException ("Error attempting to close writer", e);
		}
		return;
	}

}
