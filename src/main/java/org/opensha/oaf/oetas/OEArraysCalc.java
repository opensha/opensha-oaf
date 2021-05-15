package org.opensha.oaf.oetas;

import java.util.Arrays;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;	// natural logarithm of 10
import static org.opensha.oaf.oetas.OEConstants.SMALL_EXPECTED_COUNT;	// negligably small expected number of earthquakes


// Holds array manipulation functions for Operational ETAS.
// Author: Michael Barall 11/26/2020.
//
// This class was separated out from OEStatsCalc.

public class OEArraysCalc {




	// Convert an array into cumulative values.
	// Parameters:
	//  x = Array to convert.
	//  f_up = True to accumulate upwards (values are increasing),
	//         false to accumulate downwards (values are decreasing).

	public static void cumulate_array (double[] x, boolean f_up) {
		int len = x.length;
		if (len >= 2) {
			if (f_up) {
				double total = x[0];
				for (int n = 1; n < len; ++n) {
					total += x[n];
					x[n] = total;
				}
			}
			else {
				double total = x[len-1];
				for (int n = len-2; n >= 0; --n) {
					total += x[n];
					x[n] = total;
				}
			}
		}
		return;
	}


	public static void cumulate_array (int[] x, boolean f_up) {
		int len = x.length;
		if (len >= 2) {
			if (f_up) {
				int total = x[0];
				for (int n = 1; n < len; ++n) {
					total += x[n];
					x[n] = total;
				}
			}
			else {
				int total = x[len-1];
				for (int n = len-2; n >= 0; --n) {
					total += x[n];
					x[n] = total;
				}
			}
		}
		return;
	}




	// Convert a 2D array into cumulative values.
	// Parameters:
	//  x = 2D array to convert.  The array must be rectangular,
	//      that is, each second-level array must have the same length.
	//  f_up_1 = True to accumulate upwards in the first index
	//           (values increase with increasing first index),
	//           false to accumulate downwards in the first index
	//           (values decrease with increasing first index).
	//  f_up_2 = True to accumulate upwards in the second index
	//           (values increase with increasing second index),
	//           false to accumulate downwards in the second index
	//           (values decrease with increasing second index).

	public static void cumulate_2d_array (double[][] x, boolean f_up_1, boolean f_up_2) {

		// Get the array dimensions, and make sure they are non-zero

		int len_1 = x.length;
		if (len_1 > 0) {
			int len_2 = x[0].length;
			if (len_2 > 0) {

				// Switch on the directions

				if (f_up_1) {
					if (f_up_2) {

						// Index 1 up, index 2 up

						double total = x[0][0];
						for (int n = 1; n < len_2; ++n) {
							total += x[0][n];
							x[0][n] = total;
						}

						for (int m = 1; m < len_1; ++m) {
							total = x[m][0];
							x[m][0] = total + x[m-1][0];
							for (int n = 1; n < len_2; ++n) {
								total += x[m][n];
								x[m][n] = total + x[m-1][n];
							}
						}

					} else {

						// Index 1 up, index 2 down

						double total = x[0][len_2 - 1];
						for (int n = len_2 - 2; n >= 0; --n) {
							total += x[0][n];
							x[0][n] = total;
						}

						for (int m = 1; m < len_1; ++m) {
							total = x[m][len_2 - 1];
							x[m][len_2 - 1] = total + x[m-1][len_2 - 1];
							for (int n = len_2 - 2; n >= 0; --n) {
								total += x[m][n];
								x[m][n] = total + x[m-1][n];
							}
						}

					}
				} else {
					if (f_up_2) {

						// Index 1 down, index 2 up

						double total = x[len_1 - 1][0];
						for (int n = 1; n < len_2; ++n) {
							total += x[len_1 - 1][n];
							x[len_1 - 1][n] = total;
						}

						for (int m = len_1 - 2; m >= 0; --m) {
							total = x[m][0];
							x[m][0] = total + x[m+1][0];
							for (int n = 1; n < len_2; ++n) {
								total += x[m][n];
								x[m][n] = total + x[m+1][n];
							}
						}

					} else {

						// Index 1 down, index 2 down

						double total = x[len_1 - 1][len_2 - 1];
						for (int n = len_2 - 2; n >= 0; --n) {
							total += x[len_1 - 1][n];
							x[len_1 - 1][n] = total;
						}

						for (int m = len_1 - 2; m >= 0; --m) {
							total = x[m][len_2 - 1];
							x[m][len_2 - 1] = total + x[m+1][len_2 - 1];
							for (int n = len_2 - 2; n >= 0; --n) {
								total += x[m][n];
								x[m][n] = total + x[m+1][n];
							}
						}

					}
				}
			}
		}

		return;
	}


	public static void cumulate_2d_array (int[][] x, boolean f_up_1, boolean f_up_2) {

		// Get the array dimensions, and make sure they are non-zero

		int len_1 = x.length;
		if (len_1 > 0) {
			int len_2 = x[0].length;
			if (len_2 > 0) {

				// Switch on the directions

				if (f_up_1) {
					if (f_up_2) {

						// Index 1 up, index 2 up

						int total = x[0][0];
						for (int n = 1; n < len_2; ++n) {
							total += x[0][n];
							x[0][n] = total;
						}

						for (int m = 1; m < len_1; ++m) {
							total = x[m][0];
							x[m][0] = total + x[m-1][0];
							for (int n = 1; n < len_2; ++n) {
								total += x[m][n];
								x[m][n] = total + x[m-1][n];
							}
						}

					} else {

						// Index 1 up, index 2 down

						int total = x[0][len_2 - 1];
						for (int n = len_2 - 2; n >= 0; --n) {
							total += x[0][n];
							x[0][n] = total;
						}

						for (int m = 1; m < len_1; ++m) {
							total = x[m][len_2 - 1];
							x[m][len_2 - 1] = total + x[m-1][len_2 - 1];
							for (int n = len_2 - 2; n >= 0; --n) {
								total += x[m][n];
								x[m][n] = total + x[m-1][n];
							}
						}

					}
				} else {
					if (f_up_2) {

						// Index 1 down, index 2 up

						int total = x[len_1 - 1][0];
						for (int n = 1; n < len_2; ++n) {
							total += x[len_1 - 1][n];
							x[len_1 - 1][n] = total;
						}

						for (int m = len_1 - 2; m >= 0; --m) {
							total = x[m][0];
							x[m][0] = total + x[m+1][0];
							for (int n = 1; n < len_2; ++n) {
								total += x[m][n];
								x[m][n] = total + x[m+1][n];
							}
						}

					} else {

						// Index 1 down, index 2 down

						int total = x[len_1 - 1][len_2 - 1];
						for (int n = len_2 - 2; n >= 0; --n) {
							total += x[len_1 - 1][n];
							x[len_1 - 1][n] = total;
						}

						for (int m = len_1 - 2; m >= 0; --m) {
							total = x[m][len_2 - 1];
							x[m][len_2 - 1] = total + x[m+1][len_2 - 1];
							for (int n = len_2 - 2; n >= 0; --n) {
								total += x[m][n];
								x[m][n] = total + x[m+1][n];
							}
						}

					}
				}
			}
		}

		return;
	}




	// Sort each column in an array, into ascending order.
	// Parameters:
	//  x = Array to sort.
	//  lo = Lower index within each column, inclusive.
	//  hi = Upper index within each column, exclusive.
	// Each array column is a one-dimensional array obtained by fixing
	// all array indexes except the last array index.  Within each column,
	// elements lo (inclusive) through hi (exclusive) are sorted into
	// ascending order.  (That is, the sort applies to column elements n
	// such that lo <= n < hi.)

	public static void sort_each_array_column (double[][] x, int lo, int hi) {
		if (hi - lo > 1) {
			for (int m = 0; m < x.length; ++m) {
				Arrays.sort (x[m], lo, hi);
			}
		}
		return;
	}


	public static void sort_each_array_column (int[][] x, int lo, int hi) {
		if (hi - lo > 1) {
			for (int m = 0; m < x.length; ++m) {
				Arrays.sort (x[m], lo, hi);
			}
		}
		return;
	}


	public static void sort_each_array_column (double[][][] x, int lo, int hi) {
		if (hi - lo > 1) {
			for (int m = 0; m < x.length; ++m) {
				sort_each_array_column (x[m], lo, hi);
			}
		}
		return;
	}


	public static void sort_each_array_column (int[][][] x, int lo, int hi) {
		if (hi - lo > 1) {
			for (int m = 0; m < x.length; ++m) {
				sort_each_array_column (x[m], lo, hi);
			}
		}
		return;
	}


//	public static void sort_each_array_column (double[][][] x, int lo, int hi) {
//		if (hi - lo > 1) {
//			for (int m = 0; m < x.length; ++m) {
//				for (int n = 0; n < x[m].length; ++n) {
//					Arrays.sort (x[m][n], lo, hi);
//				}
//			}
//		}
//		return;
//	}
//
//
//	public static void sort_each_array_column (int[][][] x, int lo, int hi) {
//		if (hi - lo > 1) {
//			for (int m = 0; m < x.length; ++m) {
//				for (int n = 0; n < x[m].length; ++n) {
//					Arrays.sort (x[m][n], lo, hi);
//				}
//			}
//		}
//		return;
//	}




	// Get an element from each column in an array.
	// Parameters:
	//  x = Array to use.
	//  index = Index number.
	// Returns an array, with one less dimension than x, where each element
	// is the element at the given index in the corresponding column.
	// If y denotes the result array, then if x is 2-dimensional:
	//  y[i] = x[i][index]
	// If x is 3-dimensional:
	//  y[i][j] = x[i][j][index]

	public static double[] get_each_array_column (double[][] x, int index) {
		double[] result = new double[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = x[m][index];
		}
		return result;
	}


	public static int[] get_each_array_column (int[][] x, int index) {
		int[] result = new int[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = x[m][index];
		}
		return result;
	}


	public static double[][] get_each_array_column (double[][][] x, int index) {
		double[][] result = new double[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = get_each_array_column (x[m], index);
		}
		return result;
	}


	public static int[][] get_each_array_column (int[][][] x, int index) {
		int[][] result = new int[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = get_each_array_column (x[m], index);
		}
		return result;
	}


//	public static double[][] get_each_array_column (double[][][] x, int index) {
//		double[][] result = new double[x.length][];
//		for (int m = 0; m < x.length; ++m) {
//			result[m] = new double[x[m].length];
//			for (int n = 0; n < x[m].length; ++n) {
//				result[m][n] = x[m][n][index];
//			}
//		}
//		return result;
//	}
//
//
//	public static int[][] get_each_array_column (int[][][] x, int index) {
//		int[][] result = new int[x.length][];
//		for (int m = 0; m < x.length; ++m) {
//			result[m] = new int[x[m].length];
//			for (int n = 0; n < x[m].length; ++n) {
//				result[m][n] = x[m][n][index];
//			}
//		}
//		return result;
//	}




	// Set an element in each column in an array.
	// Parameters:
	//  x = Array to use.
	//  index = Index number.
	//  v = Array of values to set.
	// Given an array v, with one less dimension than x, store each element
	// of v into the given index in the corresponding column of x.
	// If x is 2-dimensional:
	//  x[i][index] = v[i]
	// If x is 3-dimensional:
	//  x[i][j][index] = v[i][j]

	public static void set_each_array_column (double[][] x, int index, double[] v) {
		for (int m = 0; m < x.length; ++m) {
			x[m][index] = v[m];
		}
		return;
	}

	public static void set_each_array_column (int[][] x, int index, int[] v) {
		for (int m = 0; m < x.length; ++m) {
			x[m][index] = v[m];
		}
		return;
	}

	public static void set_each_array_column (double[][][] x, int index, double[][] v) {
		for (int m = 0; m < x.length; ++m) {
			set_each_array_column (x[m], index, v[m]);
		}
		return;
	}

	public static void set_each_array_column (int[][][] x, int index, int[][] v) {
		for (int m = 0; m < x.length; ++m) {
			set_each_array_column (x[m], index, v[m]);
		}
		return;
	}




	// Resize in each column in an array.
	// Parameters:
	//  x = Array to use.
	//  new_length = New length of each column.
	// Each column of the array is replaced with a new column of the given length.
	// For indexes valid in both the old and new columns, the values are preserved.
	// If the new length exceeds the existing length, the new elements are set to zero.

	public static void resize_each_array_column (double[][] x, int new_length) {
		for (int m = 0; m < x.length; ++m) {
			x[m] = Arrays.copyOf (x[m], new_length);
		}
		return;
	}

	public static void resize_each_array_column (int[][] x, int new_length) {
		for (int m = 0; m < x.length; ++m) {
			x[m] = Arrays.copyOf (x[m], new_length);
		}
		return;
	}

	public static void resize_each_array_column (double[][][] x, int new_length) {
		for (int m = 0; m < x.length; ++m) {
			resize_each_array_column (x[m], new_length);
		}
		return;
	}

	public static void resize_each_array_column (int[][][] x, int new_length) {
		for (int m = 0; m < x.length; ++m) {
			resize_each_array_column (x[m], new_length);
		}
		return;
	}




	// Set to zero all the elements an array.
	// Parameters:
	//  x = Array to use.

	public static void zero_array (double[] x) {
		Arrays.fill (x, 0.0);
		return;
	}

	public static void zero_array (int[] x) {
		Arrays.fill (x, 0);
		return;
	}

	public static void zero_array (double[][] x) {
		for (int m = 0; m < x.length; ++m) {
			Arrays.fill (x[m], 0.0);
		}
		return;
	}

	public static void zero_array (int[][] x) {
		for (int m = 0; m < x.length; ++m) {
			Arrays.fill (x[m], 0);
		}
		return;
	}

	public static void zero_array (double[][][] x) {
		for (int m = 0; m < x.length; ++m) {
			zero_array (x[m]);
		}
		return;
	}

	public static void zero_array (int[][][] x) {
		for (int m = 0; m < x.length; ++m) {
			zero_array (x[m]);
		}
		return;
	}




	// Compute the average of an array.
	// Parameters:
	//  x = Array to average.
	//  lo = Lower index, inclusive.
	//  hi = Upper index, exclusive.
	// Note: Requires hi > lo to avoid divide-by-zero.

	public static double array_average (int[] x, int lo, int hi) {
		double total = 0.0;
		for (int m = lo; m < hi; ++m) {
			total += (double)(x[m]);
		}
		return total / ((double)(hi - lo));
	}


	public static double array_average (double[] x, int lo, int hi) {
		double total = 0.0;
		for (int m = lo; m < hi; ++m) {
			total += x[m];
		}
		return total / ((double)(hi - lo));
	}




	// Average each column in an array.
	// Parameters:
	//  x = Array to use.
	//  lo = Lower index within each column, inclusive.
	//  hi = Upper index within each column, exclusive.
	// Returns an array, with one less dimension than x, where each element
	// is the average of the corresponding column.  Within each column,
	// elements lo (inclusive) through hi (exclusive) are averaged.  (That is,
	// the averaging applies to column elements n such that lo <= n < hi.)
	// If y denotes the result array, then if x is 2-dimensional:
	//  y[i] = average(x[i][lo], ... , x[i][hi-1])
	// If x is 3-dimensional:
	//  y[i][j] = average(x[i][j][lo], ... , x[i][j][hi-1])

	public static double[] average_each_array_column (double[][] x, int lo, int hi) {
		double[] result = new double[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = array_average (x[m], lo, hi);
		}
		return result;
	}


	public static double[] average_each_array_column (int[][] x, int lo, int hi) {
		double[] result = new double[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = array_average (x[m], lo, hi);
		}
		return result;
	}


	public static double[][] average_each_array_column (double[][][] x, int lo, int hi) {
		double[][] result = new double[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = average_each_array_column (x[m], lo, hi);
		}
		return result;
	}


	public static double[][] average_each_array_column (int[][][] x, int lo, int hi) {
		double[][] result = new double[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = average_each_array_column (x[m], lo, hi);
		}
		return result;
	}


//	public static double[][] average_each_array_column (double[][][] x, int lo, int hi) {
//		double[][] result = new double[x.length][];
//		for (int m = 0; m < x.length; ++m) {
//			result[m] = new double[x[m].length];
//			for (int n = 0; n < x[m].length; ++n) {
//				result[m][n] = array_average (x[m][n], lo, hi);
//			}
//		}
//		return result;
//	}
//
//
//	public static double[][] average_each_array_column (int[][][] x, int lo, int hi) {
//		double[][] result = new double[x.length][];
//		for (int m = 0; m < x.length; ++m) {
//			result[m] = new double[x[m].length];
//			for (int n = 0; n < x[m].length; ++n) {
//				result[m][n] = array_average (x[m][n], lo, hi);
//			}
//		}
//		return result;
//	}




	// Binary search an array.
	// Parameters:
	//  x = Array, must be sorted into increasing order.
	//  v = Value to search for
	//  lo = Lower index, inclusive.
	//  hi = Upper index, exclusive.
	// Returns the integer n such that:
	//  lo <= n <= hi
	//  x[n-1] <= v < x[n]
	// For the purpose of the last condition, x[lo-1] == -infinity and x[hi] == infinity.
	// Note that x[n] is the first array element that ia greater than v.

	public static int bsearch_array (double[] x, double v, int lo, int hi) {
	
		// Binary search algoritm, preserves the condition x[bslo] <= v < x[bshi]

		int bslo = lo - 1;
		int bshi = hi;

		while (bshi - bslo > 1) {
			int mid = (bshi + bslo) / 2;
			if (v < x[mid]) {
				bshi = mid;
			} else {
				bslo = mid;
			}
		}

		return bshi;
	}


	public static int bsearch_array (int[] x, int v, int lo, int hi) {
	
		// Binary search algoritm, preserves the condition x[bslo] <= v < x[bshi]

		int bslo = lo - 1;
		int bshi = hi;

		while (bshi - bslo > 1) {
			int mid = (bshi + bslo) / 2;
			if (v < x[mid]) {
				bshi = mid;
			} else {
				bslo = mid;
			}
		}

		return bshi;
	}




	// Binary search an array.
	// Parameters:
	//  x = Array, must be sorted into increasing order.
	//  v = Value to search for
	// Returns the integer n such that:
	//  0 <= n <= x.length
	//  x[n-1] <= v < x[n]
	// For the purpose of the last condition, x[-1] == -infinity and x[x.length] == infinity.
	// Note that x[n] is the first array element that ia greater than v.

	public static int bsearch_array (double[] x, double v) {
	
		// Binary search algoritm, preserves the condition x[bslo] <= v < x[bshi]

		int bslo = -1;
		int bshi = x.length;

		while (bshi - bslo > 1) {
			int mid = (bshi + bslo) / 2;
			if (v < x[mid]) {
				bshi = mid;
			} else {
				bslo = mid;
			}
		}

		return bshi;
	}


	public static int bsearch_array (int[] x, int v) {
	
		// Binary search algoritm, preserves the condition x[bslo] <= v < x[bshi]

		int bslo = -1;
		int bshi = x.length;

		while (bshi - bslo > 1) {
			int mid = (bshi + bslo) / 2;
			if (v < x[mid]) {
				bshi = mid;
			} else {
				bslo = mid;
			}
		}

		return bshi;
	}




	// Binary search each column in an array.
	// Parameters:
	//  x = Array to use, where each column has been sorted into increasing order.
	//  v = Value to search for
	//  lo = Lower index within each column, inclusive.
	//  hi = Upper index within each column, exclusive.
	// Returns an array, with one less dimension than x, where each element
	// is the result of a binary search of the corresponding column.  Specifically,
	// for each column the result is an integer n such that
	//  lo <= n <= hi
	//  x[n-1] <= v < x[n]
	// For the purpose of the last condition, x[lo-1] == -infinity and x[hi] == infinity.
	// If y denotes the result array, then if x is 2-dimensional:
	//  y[i] = bsearch(x[i][lo], ... , x[i][hi-1])
	// If x is 3-dimensional:
	//  y[i][j] = bsearch(x[i][j][lo], ... , x[i][j][hi-1])

	public static int[] bsearch_each_array_column (double[][] x, double v, int lo, int hi) {
		int[] result = new int[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = bsearch_array (x[m], v, lo, hi);
		}
		return result;
	}


	public static int[] bsearch_each_array_column (int[][] x, int v, int lo, int hi) {
		int[] result = new int[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = bsearch_array (x[m], v, lo, hi);
		}
		return result;
	}


	public static int[][] bsearch_each_array_column (double[][][] x, double v, int lo, int hi) {
		int[][] result = new int[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = bsearch_each_array_column (x[m], v, lo, hi);
		}
		return result;
	}


	public static int[][] bsearch_each_array_column (int[][][] x, int v, int lo, int hi) {
		int[][] result = new int[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = bsearch_each_array_column (x[m], v, lo, hi);
		}
		return result;
	}


//	public static int[][] bsearch_each_array_column (double[][][] x, double v, int lo, int hi) {
//		int[][] result = new int[x.length][];
//		for (int m = 0; m < x.length; ++m) {
//			result[m] = new double[x[m].length];
//			for (int n = 0; n < x[m].length; ++n) {
//				result[m][n] = bsearch_array (x[m][n], v, lo, hi);
//			}
//		}
//		return result;
//	}
//
//
//	public static int[][] bsearch_each_array_column (int[][][] x, int v, int lo, int hi) {
//		int[][] result = new int[x.length][];
//		for (int m = 0; m < x.length; ++m) {
//			result[m] = new double[x[m].length];
//			for (int n = 0; n < x[m].length; ++n) {
//				result[m][n] = bsearch_array (x[m][n], v, lo, hi);
//			}
//		}
//		return result;
//	}




	// Probability of exceedence for each column in an array.
	// Parameters:
	//  x = Array to use, where each column has been sorted into increasing order.
	//  v = Value to search for
	//  lo = Lower index within each column, inclusive.
	//  hi = Upper index within each column, exclusive.
	// Returns an array, with one less dimension than x, where each element
	// is a probability that the array value exceeds v.  Specifically,
	// for each column  the function finds an integer n such that
	//  lo <= n <= hi
	//  x[n-1] <= v < x[n]
	// For the purpose of the last condition, x[lo-1] == -infinity and x[hi] == infinity.
	// Then, the return value is (hi - n)/(hi - lo)

	public static double[] probex_each_array_column (double[][] x, double v, int lo, int hi) {
		double[] result = new double[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = ((double)(hi - bsearch_array (x[m], v, lo, hi))) / ((double)(hi - lo));
		}
		return result;
	}


	public static double[] probex_each_array_column (int[][] x, int v, int lo, int hi) {
		double[] result = new double[x.length];
		for (int m = 0; m < x.length; ++m) {
			result[m] = ((double)(hi - bsearch_array (x[m], v, lo, hi))) / ((double)(hi - lo));
		}
		return result;
	}


	public static double[][] probex_each_array_column (double[][][] x, double v, int lo, int hi) {
		double[][] result = new double[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = probex_each_array_column (x[m], v, lo, hi);
		}
		return result;
	}


	public static double[][] probex_each_array_column (int[][][] x, int v, int lo, int hi) {
		double[][] result = new double[x.length][];
		for (int m = 0; m < x.length; ++m) {
			result[m] = probex_each_array_column (x[m], v, lo, hi);
		}
		return result;
	}




	// Add a Poisson random value to each element in an array.
	// Parameters:
	//  rangen = Random number generator.
	//  x = Array to use.
	//  mean = Array of mean values for the Poisson random variables.
	// If x is 1-dimensional:
	//  x[i] = Poisson(mean[i])
	// If x is 2-dimensional:
	//  x[i][j] = Poisson(mean[i][j])

	public static void add_poisson_array (OERandomGenerator rangen, int[] x, double[] mean) {
		for (int m = 0; m < x.length; ++m) {
			if (mean[m] >= SMALL_EXPECTED_COUNT) {
				x[m] += rangen.poisson_sample_checked (mean[m]);
			}
		}
		return;
	}


	public static void add_poisson_array (OERandomGenerator rangen, int[][] x, double[][] mean) {
		for (int m = 0; m < x.length; ++m) {
			add_poisson_array (rangen, x[m], mean[m]);
		}
		return;
	}




	// Return the minimum value of an array.
	// Parameters:
	//  x = Array to search.
	//  ix = Array to receive index(es) where minimum is found, or null if not desired.
	// Returns Double.MAX_VALUE if array is empty.
	// The length of ix is equal to the number of dimensions of x.
	// If ix is non-null, it is filled with the index(es) where the minimum is located.

	public static double find_array_min (final double[] x, final int[] ix) {
		double result = Double.MAX_VALUE;
		int ix0 = -1;
		for (int m = 0; m < x.length; ++m) {
			if (x[m] < result) {
				result = x[m];
				ix0 = m;
			}
		}
		if (ix != null) {
			ix[0] = ix0;
		}
		return result;
	}


	public static double find_array_min (final double[][] x, final int[] ix) {
		double result = Double.MAX_VALUE;
		int ix0 = -1;
		int ix1 = -1;
		for (int m = 0; m < x.length; ++m) {
			final double[] row = x[m];
			for (int n = 0; n < row.length; ++n) {
				if (row[n] < result) {
					result = row[n];
					ix0 = m;
					ix1 = n;
				}
			}
		}
		if (ix != null) {
			ix[0] = ix0;
			ix[1] = ix1;
		}
		return result;
	}


	public static double find_array_min (final double[][][] x, final int[] ix) {
		double result = Double.MAX_VALUE;
		int ix0 = -1;
		int ix1 = -1;
		int ix2 = -1;
		for (int m = 0; m < x.length; ++m) {
			final double[][] x1 = x[m];
			for (int m1 = 0; m1 < x1.length; ++m1) {
				final double[] x2 = x1[m1];
				for (int m2 = 0; m2 < x2.length; ++m2) {
					if (x2[m2] < result) {
						result = x2[m2];
						ix0 = m;
						ix1 = m1;
						ix2 = m2;
					}
				}
			}
		}
		if (ix != null) {
			ix[0] = ix0;
			ix[1] = ix1;
			ix[2] = ix2;
		}
		return result;
	}


	public static double find_array_min (final double[][][][] x, final int[] ix) {
		double result = Double.MAX_VALUE;
		int ix0 = -1;
		int ix1 = -1;
		int ix2 = -1;
		int ix3 = -1;
		for (int m = 0; m < x.length; ++m) {
			final double[][][] x1 = x[m];
			for (int m1 = 0; m1 < x1.length; ++m1) {
				final double[][] x2 = x1[m1];
				for (int m2 = 0; m2 < x2.length; ++m2) {
					final double[] x3 = x2[m2];
					for (int m3 = 0; m3 < x3.length; ++m3) {
						if (x3[m3] < result) {
							result = x3[m3];
							ix0 = m;
							ix1 = m1;
							ix2 = m2;
							ix3 = m3;
						}
					}
				}
			}
		}
		if (ix != null) {
			ix[0] = ix0;
			ix[1] = ix1;
			ix[2] = ix2;
			ix[3] = ix3;
		}
		return result;
	}




	// Return the maximum value of an array.
	// Parameters:
	//  x = Array to search.
	//  ix = Array to receive index(es) where maximum is found, or null if not desired.
	// Returns -Double.MAX_VALUE if array is empty.
	// The length of ix is equal to the number of dimensions of x.
	// If ix is non-null, it is filled with the index(es) where the maximum is located.

	public static double find_array_max (final double[] x, final int[] ix) {
		double result = -Double.MAX_VALUE;
		int ix0 = -1;
		for (int m = 0; m < x.length; ++m) {
			if (x[m] > result) {
				result = x[m];
				ix0 = m;
			}
		}
		if (ix != null) {
			ix[0] = ix0;
		}
		return result;
	}


	public static double find_array_max (final double[][] x, final int[] ix) {
		double result = -Double.MAX_VALUE;
		int ix0 = -1;
		int ix1 = -1;
		for (int m = 0; m < x.length; ++m) {
			final double[] row = x[m];
			for (int n = 0; n < row.length; ++n) {
				if (row[n] > result) {
					result = row[n];
					ix0 = m;
					ix1 = n;
				}
			}
		}
		if (ix != null) {
			ix[0] = ix0;
			ix[1] = ix1;
		}
		return result;
	}


	public static double find_array_max (final double[][][] x, final int[] ix) {
		double result = -Double.MAX_VALUE;
		int ix0 = -1;
		int ix1 = -1;
		int ix2 = -1;
		for (int m = 0; m < x.length; ++m) {
			final double[][] x1 = x[m];
			for (int m1 = 0; m1 < x1.length; ++m1) {
				final double[] x2 = x1[m1];
				for (int m2 = 0; m2 < x2.length; ++m2) {
					if (x2[m2] > result) {
						result = x2[m2];
						ix0 = m;
						ix1 = m1;
						ix2 = m2;
					}
				}
			}
		}
		if (ix != null) {
			ix[0] = ix0;
			ix[1] = ix1;
			ix[2] = ix2;
		}
		return result;
	}


	public static double find_array_max (final double[][][][] x, final int[] ix) {
		double result = -Double.MAX_VALUE;
		int ix0 = -1;
		int ix1 = -1;
		int ix2 = -1;
		int ix3 = -1;
		for (int m = 0; m < x.length; ++m) {
			final double[][][] x1 = x[m];
			for (int m1 = 0; m1 < x1.length; ++m1) {
				final double[][] x2 = x1[m1];
				for (int m2 = 0; m2 < x2.length; ++m2) {
					final double[] x3 = x2[m2];
					for (int m3 = 0; m3 < x3.length; ++m3) {
						if (x3[m3] > result) {
							result = x3[m3];
							ix0 = m;
							ix1 = m1;
							ix2 = m2;
							ix3 = m3;
						}
					}
				}
			}
		}
		if (ix != null) {
			ix[0] = ix0;
			ix[1] = ix1;
			ix[2] = ix2;
			ix[3] = ix3;
		}
		return result;
	}




	// Make a deep copy of an array.
	// Parameters:
	//  x = Array to copy.
	// Returns an array, with the same extents as x, with the same contents.
	// This is a deep copy.

	public static double[] array_copy (final double[] x) {
		final int c0 = x.length;
		final double[] r0 = new double[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = x[m0];
		}
		return r0;
	}

	public static int[] array_copy (final int[] x) {
		final int c0 = x.length;
		final int[] r0 = new int[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = x[m0];
		}
		return r0;
	}

	public static double[][] array_copy (final double[][] x) {
		final int c0 = x.length;
		final double[][] r0 = new double[c0][];
		for (int m0 = 0; m0 < c0; ++m0) {
			final double[] x1 = x[m0];
			final int c1 = x1.length;
			final double[] r1 = new double[c1];
			r0[m0] = r1;
			for (int m1 = 0; m1 < c1; ++m1) {
				r1[m1] = x1[m1];
			}
		}
		return r0;
	}

	public static int[][] array_copy (final int[][] x) {
		final int c0 = x.length;
		final int[][] r0 = new int[c0][];
		for (int m0 = 0; m0 < c0; ++m0) {
			final int[] x1 = x[m0];
			final int c1 = x1.length;
			final int[] r1 = new int[c1];
			r0[m0] = r1;
			for (int m1 = 0; m1 < c1; ++m1) {
				r1[m1] = x1[m1];
			}
		}
		return r0;
	}




	// Make a deep copy of an array, and take log10 of each element
	// Parameters:
	//  x = Array to copy and apply Math.log10.
	// Returns an array, with the same extents as x, with log10 applied to the contents.
	// This is a deep copy.

	public static double[] array_copy_log10 (final double[] x) {
		final int c0 = x.length;
		final double[] r0 = new double[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = Math.log10(x[m0]);
		}
		return r0;
	}

	public static double[][] array_copy_log10 (final double[][] x) {
		final int c0 = x.length;
		final double[][] r0 = new double[c0][];
		for (int m0 = 0; m0 < c0; ++m0) {
			final double[] x1 = x[m0];
			final int c1 = x1.length;
			final double[] r1 = new double[c1];
			r0[m0] = r1;
			for (int m1 = 0; m1 < c1; ++m1) {
				r1[m1] = Math.log10(x1[m1]);
			}
		}
		return r0;
	}




	// Make a deep copy of an array, and take log of each element
	// Parameters:
	//  x = Array to copy and apply Math.log.
	// Returns an array, with the same extents as x, with log applied to the contents.
	// This is a deep copy.

	public static double[] array_copy_log (final double[] x) {
		final int c0 = x.length;
		final double[] r0 = new double[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = Math.log(x[m0]);
		}
		return r0;
	}

	public static double[][] array_copy_log (final double[][] x) {
		final int c0 = x.length;
		final double[][] r0 = new double[c0][];
		for (int m0 = 0; m0 < c0; ++m0) {
			final double[] x1 = x[m0];
			final int c1 = x1.length;
			final double[] r1 = new double[c1];
			r0[m0] = r1;
			for (int m1 = 0; m1 < c1; ++m1) {
				r1[m1] = Math.log(x1[m1]);
			}
		}
		return r0;
	}




	// Make a deep copy of an array, and take exp of each element
	// Parameters:
	//  x = Array to copy and apply Math.exp.
	// Returns an array, with the same extents as x, with exp applied to the contents.
	// This is a deep copy.

	public static double[] array_copy_exp (final double[] x) {
		final int c0 = x.length;
		final double[] r0 = new double[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = Math.exp(x[m0]);
		}
		return r0;
	}

	public static double[][] array_copy_exp (final double[][] x) {
		final int c0 = x.length;
		final double[][] r0 = new double[c0][];
		for (int m0 = 0; m0 < c0; ++m0) {
			final double[] x1 = x[m0];
			final int c1 = x1.length;
			final double[] r1 = new double[c1];
			r0[m0] = r1;
			for (int m1 = 0; m1 < c1; ++m1) {
				r1[m1] = Math.exp(x1[m1]);
			}
		}
		return r0;
	}




	// Make a deep copy of an array, and take power of 10 to each element
	// Parameters:
	//  x = Array to copy and apply Math.pow(10.0, *).
	// Returns an array, with the same extents as x, with pow(10.0, *) applied to the contents.
	// This is a deep copy.

	public static double[] array_copy_exp10 (final double[] x) {
		final int c0 = x.length;
		final double[] r0 = new double[c0];
		for (int m0 = 0; m0 < c0; ++m0) {
			r0[m0] = Math.pow(10.0, x[m0]);
		}
		return r0;
	}

	public static double[][] array_copy_exp10 (final double[][] x) {
		final int c0 = x.length;
		final double[][] r0 = new double[c0][];
		for (int m0 = 0; m0 < c0; ++m0) {
			final double[] x1 = x[m0];
			final int c1 = x1.length;
			final double[] r1 = new double[c1];
			r0[m0] = r1;
			for (int m1 = 0; m1 < c1; ++m1) {
				r1[m1] = Math.pow(10.0, x1[m1]);
			}
		}
		return r0;
	}




}
