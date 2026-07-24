package org.opensha.oaf.oetas.val;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.oetas.util.OEArraysCalc;


// Validation utility functions.
// Author: Michael Barall.

public class OEValUtils {




	// Interpolate function, where x-values are given as integers.
	// Parameters:
	//  x = Argument value at which to interpolate.
	//  x1 = First argument value.
	//  x2 = Second argument value.
	//  y1 = Function value at argument x1.
	//  y2 = Function value at argument x2.
	// Returns the function value at argument x.

	public static double interpolate_int (int x, int x1, int x2, double y1, double y2) {

		// Check for empty range

		if (x1 == x2) {
			//  if (y1 == y2) {
			//  	return y1;
			//  }
			throw new IllegalArgumentException ("OEValUtils.interpolate_int: Empty range: x1 = x2 = " + x1);
		}

		// Check for endpoints

		if (x == x1) {
			return y1;
		}
		if (x == x2) {
			return y2;
		}

		// Not at endpoints, interpolate

		double r = ((double)(x - x1)) / ((double)(x2 - x1));
		return (y1 * (1.0 - r)) + (y2 * r);
	}




	// Find p-values given an observed count and a list of integer-valued fractiles.
	// Parameters:
	//  probs = Array of probabilities, sorted in strictly increasing order, all between 0.0 and 1.0 exclusive.
	//  fractiles = Array of fractiles, sorted in increasing order, all non-negative integers.
	//  v = Observed value.
	//  p = 2-element array to receive p-values.
	//  hiopt = True to return upper p-value equal to 1.0 if the last fractile equals v.
	// Returns true if a single p-value was found, false if two different p-values are found.
	// On return, p[0] receives the lower p-value and p[1] receives the upper p-value.
	// 1. For purposes of this function, the probs and fractiles arrays are presumed to be extended as follows:
	//    probs[-1] = 0.0
	//    probs[probs.length] = 1.0
	//    fractiles[-1] = 0
	//    fractiles[fractiles.length] = infinity
	// 2. With these extensions, we assume that elements of the probs array are strictly increasing,
	//    and the elements of the fractiles array are (not necessarily strictly) increasing.
	// 3. The probs and fractiles arrays must have the same length, and thereby establish a mapping
	//    from non-negative integral observations to probabilites. The p-value is the probability that
	//    corresponds to the observed value v. If the p-value is ambiguous, then this function supplies
	//    the lowest and highest possible p-values and returns false.
	// 4. The lower p-value equals zero if and only if v equals zero.
	// 5. The upper p-value equals 1.0 if and only if v is greater than [[optionally: or equal to]]
	//    the last fractile.
	// 6. The lower and upper p-values differ if and only if at least one of these conditions is true:
	//    a) v is zero and there is at least one fractile equal to zero.
	//    b) Two or more fractiles are exactly equal to v.
	//    c) v is greater than [[optionally: or equal to]] the last fractile.
	// 7. The algorithm can be understood as follows (optional tests are taken if hiopt is true):
	//    If v equals zero, then:
	//      * If all fractiles are greater than zero, then both p-values equal zero.
	//      * [[optional]] If all fractiles are zero, then the lower p-value is zero and the
	//          upper p-value is one.
	//      * Otherwise, the lower p-value is zero and the upper p-value is the probability of
	//          the last fractile equal to zero.
	//    If v is greater than zero, then:
	//      * If v is less than the first fractile, then both p-values equal the probability obtained
	//          by interpolating between zero and the first fractile.
	//      * If v is greater than the last fractile, then the lower p-value is the last
	//          probability, and the upper p-value is one.
	//      * [[optional]] If the last fractile equals v, then the lower p-value is the probability
	//          of the first fractile equal to v, and the upper p-value is one.
	//      * Otherwise, if at least one fractile equals v, then the lower p-value is the probability
	//          of the first fractile equal to v, and the upper p-value is the probability of the
	//          last fractile equal to v.
	//      * Otherwise, v lies in between two consecutive fractiles, one less than v and one
	//          greater than v, and then both p-values equal the probability obtained by interpolating
	//          between the two fractiles.

	public static boolean calc_fractile_p (double[] probs, int[] fractiles, int v, double[] p, boolean hiopt) {
		if (probs == null || probs.length == 0) {
			throw new IllegalArgumentException ("OEValUtils.calc_fractile_p: No probabilites supplied");
		}
		if (fractiles == null || fractiles.length == 0) {
			throw new IllegalArgumentException ("OEValUtils.calc_fractile_p: No fractiles supplied");
		}
		if (probs.length != fractiles.length) {
			throw new IllegalArgumentException ("OEValUtils.calc_fractile_p: Arrays of probabilities and fractiles have different length: probs.length = " + probs.length + ", fractiles.length = " + fractiles.length);
		}
		if (v < 0) {
			throw new IllegalArgumentException ("OEValUtils.calc_fractile_p: Negative observed value: v = " + v);
		}
		if (p == null || p.length < 2) {
			throw new IllegalArgumentException ("OEValUtils.calc_fractile_p: No p array supplied");
		}

		// Length of probability and fractile arrays

		final int len = fractiles.length;

		// Find n such that fractiles[n] is the first fractile that is greater than v,
		// where 0 <= n <= len and we pretend that fractiles[len] == infinity

		final int n = OEArraysCalc.bsearch_array (fractiles, v);

		// If we just found the end+1 of a (possibly empty) run of fractiles equal to zero ...

		if (v == 0) {

			// Lower p-value for zero is always zero

			p[0] = 0.0;

			// If first fractile is > 0, then upper p-value is also zero

			if (n == 0) {
				p[1] = 0.0;
				return true;
			}

			// If the last fractile is zero, then the upper p-value is one [[optional]]

			if (hiopt) {
				if (n == len) {
					p[1] = 1.0;
					return false;
				}
			}

			// Otherwise, upper p-value is the probability of the last zero fractile

			p[1] = probs[n - 1];
			return false;
		}

		// If v is less than the first fractile ...

		if (n == 0) {

			// Interpolate between zero and the first fractile

			p[0] = interpolate_int (v, 0, fractiles[n], 0.0, probs[n]);
			p[1] = p[0];
			return true;
		}

		// If we just found the end+1 of a run of fractiles equal to v ...

		if (fractiles[n - 1] == v) {

			// Find m such that fractiles[m] is the first fractile equal to v,
			// where 0 <= m <= n-1, and we know that fractiles[n-1] == v and v > 0

			final int m = OEArraysCalc.bsearch_array (fractiles, v - 1, 0, n - 1);

			// The lower p-value is the probability of the first fractile equal to v

			p[0] = probs[m];

			// If the last fractile equals v, then the upper p-value is one [[optional]]

			if (hiopt) {
				if (n == len) {
					p[1] = 1.0;
					return false;
				}
			}

			// The upper p-value is the probability of the last fractile equal to v

			p[1] = probs[n - 1];

			// It's a single p-value if the run is of length 1

			if (m == n - 1) {
				return true;
			}
			return false;
		}

		// If v is greater than the last fractile ...

		if (n == len) {

			// Bracket v between the last probability and one

			p[0] = probs[n - 1];
			p[1] = 1.0;
			return false;
		}

		// We now know that fractiles[n-1] and fractiles[n] strictly bracket v, so interpolate

		p[0] = interpolate_int (v, fractiles[n - 1], fractiles[n], probs[n - 1], probs[n]);
		p[1] = p[0];
		return true;
	}




	// Make a probability array.
	// Parameters:
	//  len = Number of elements in the array, must be >= 2.
	//  prob_first = Probability assigned to first element of the array.
	//  prob_last = Probability assigned to last element of the array.
	//  f_round = True to round all probabilities to 12 decimal places.

	public static double[] make_prob_array (int len, double prob_first, double prob_last, boolean f_round) {
		if (!( len >= 2 )) {
			throw new IllegalArgumentException ("OEValUtils.make_prob_array: Invalid length: len = " + len);
		}
		if (!( 0.0 <= prob_first && prob_first < prob_last && prob_last <= 1.0 )) {
			throw new IllegalArgumentException ("OEValUtils.make_prob_array: Invalid probabilities: prob_first = " + prob_first + ", prob_last = " + prob_last);
		}

		double[] prob_array = new double[len];
		for (int j = 0; j < len; ++j) {
			prob_array[j] = interpolate_int (j, 0, len - 1, prob_first, prob_last);
			if (f_round) {
				prob_array[j] = SimpleUtils.round_double_via_string ("%.12f", prob_array[j]);
			}
		}
		return prob_array;
	}




	// Make a fractile values array for testing.
	// Parameters:
	//  len = Number of elements in the array, must be >= 2.
	//  value_first = Value assigned to the first element of the array.
	//  value_last = Value assigned to the last element of the array.

	public static int[] make_test_fractile_values (int len, int value_first, int value_last) {
		if (!( len >= 2 )) {
			throw new IllegalArgumentException ("OEValUtils.make_test_fractile_values: Invalid length: len = " + len);
		}
		if (!( 0 <= value_first && value_first <= value_last )) {
			throw new IllegalArgumentException ("OEValUtils.make_test_fractile_values: Invalid values: value_first = " + value_first + ", value_last = " + value_last);
		}

		// If the number of values is small compared to the number of elements,
		// use an offset when interpolating so the first and last values are not underrepresented

		double offset = Math.max (0.0, 0.5 - ( ((double)(value_last + 1 - value_first)) / ((double)(len * 2)) ));

		double r_value_first = ((double)value_first) - offset;
		double r_value_last = ((double)value_last) + offset;

		int[] fractile_values = new int[len];
		for (int j = 0; j < len; ++j) {
			fractile_values[j] = (int)(Math.round (interpolate_int (j, 0, len - 1, r_value_first, r_value_last)));
		}
		return fractile_values;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEValUtils");




		// Subcommand : Test #1
		// Command format:
		//  test1  len  prob_first  prob_last  f_round
		// Test the make_prob_array function.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Testing make_prob_array()");
			int len = testargs.get_int ("len");
			double prob_first = testargs.get_double ("prob_first");
			double prob_last = testargs.get_double ("prob_last");
			boolean f_round = testargs.get_boolean ("f_round");
			testargs.end_test();

			// Make probability array

			System.out.println ();
			System.out.println ("********** Make probability array **********");
			System.out.println ();

			double[] prob_array = make_prob_array (len, prob_first, prob_last, f_round);

			System.out.println ("prob_array = [");
			for (int ix = 0; ix < prob_array.length; ++ix) {
				System.out.println ("  " + ix + ": " + prob_array[ix]);
			}
			System.out.println ("]");

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test1  len  value_first  value_last
		// Test the make_test_fractile_values function.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Testing make_test_fractile_values()");
			int len = testargs.get_int ("len");
			int value_first = testargs.get_int ("value_first");
			int value_last = testargs.get_int ("value_last");
			testargs.end_test();

			// Make test fractile values array

			System.out.println ();
			System.out.println ("********** Make test fractile values **********");
			System.out.println ();

			int[] fractile_values = make_test_fractile_values (len, value_first, value_last);

			System.out.println ("fractile_values = [");
			for (int ix = 0; ix < fractile_values.length; ++ix) {
				System.out.println ("  " + ix + ": " + fractile_values[ix]);
			}
			System.out.println ("]");

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test1  len  prob_first  prob_last  f_round  value_first  value_last  hiopt  value...
		// Test the calc_fractile_p function.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Testing calc_fractile_p()");
			int len = testargs.get_int ("len");
			double prob_first = testargs.get_double ("prob_first");
			double prob_last = testargs.get_double ("prob_last");
			boolean f_round = testargs.get_boolean ("f_round");
			int value_first = testargs.get_int ("value_first");
			int value_last = testargs.get_int ("value_last");
			boolean hiopt = testargs.get_boolean ("hiopt");
			double[] r_value = testargs.get_double_array ("value", -1, 1);	// TODO: make get_int_array
			testargs.end_test();

			int[] value = new int[r_value.length];
			for (int k = 0; k < value.length; ++k) {
				value[k] = (int)(Math.round (r_value[k]));
			}

			// Make probability array

			System.out.println ();
			System.out.println ("********** Make probability array **********");
			System.out.println ();

			double[] prob_array = make_prob_array (len, prob_first, prob_last, f_round);

			System.out.println ("prob_array[0] = " + prob_array[0]);
			System.out.println ("prob_array[" + (len-1) + "] = " + prob_array[len-1]);

			// Make test fractile values array

			System.out.println ();
			System.out.println ("********** Make test fractile values **********");
			System.out.println ();

			int[] fractile_values = make_test_fractile_values (len, value_first, value_last);

			System.out.println ("fractile_values[0] = " + fractile_values[0]);
			System.out.println ("fractile_values[" + (len-1) + "] = " + fractile_values[len-1]);

			// Test p-value computation

			System.out.println ();
			System.out.println ("********** Test p-value computation **********");
			System.out.println ();

			for (int j = 0; j < value.length; ++j) {
				double[] p = new double[2];
				boolean single = calc_fractile_p (prob_array, fractile_values, value[j], p, hiopt);

				System.out.println (j + ": value = " + value[j] + ", single = " + single + ", p_lo = " + p[0] + ", p_hi = " + p[1]);
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}

}
