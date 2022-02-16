package org.opensha.oaf.oetas;

import java.util.Arrays;

// For Colt 0.9.4
import cern.jet.stat.tdouble.Probability;
import cern.jet.stat.tdouble.Gamma;
import cern.jet.random.tdouble.engine.RandomGenerator;
import cern.jet.random.tdouble.engine.DoubleRandomEngine;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import cern.jet.random.tdouble.Poisson;
import cern.jet.random.tdouble.DoubleUniform;

import org.opensha.oaf.util.TestMode;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;	// natural logarithm of 10




// Class to build stacked Poisson distributions for an Operational ETAS catalog.
// Author: Michael Barall 02/09/2012.
//
// This class is designed to allow rapid calculation of stacked Poisson distributions.
// It works by summing a set of Poisson distributions, each with its own mean and weight.
//
// Instead of evaluating the distribution at every integer value, it evaluates the
// distribution at a set of integer values that are approximately logarithmically spaced.
// During construction, you can specify the desired precision (number of significant digits).
//
// Also, it pre-computes and caches the probability distribution for a set of means,
// which are also approximately logarithmically spaced.  Each cached distribution is
// truncated at low and high values of its cumulative distribution.  When adding a
// Poisson distribution, it uses the cached distribution with the closest mean.
//
// Once an object of this class is created, you can create one or more accumulators.
// Each accumulator can sum a set of Poisson distributions and calculate fractiles.
// It is possible to have multiple accumulators, which can be used simulataneously by
// different threads.
//
// It is expensive to create objects of this type, so the recommended use is to create
// one and then use it repeatedly.  There is provision for a default object, which is
// accessed through a static variable.

public class OEStackedPoisson {

	//----- Configuration -----

	// Precision (number of significant digits) for Poisson values.
	// Must be between 1.0 and 6.0, inclusive, and be either integral or half-integral.

	private double value_precision;

	public static final double MIN_VALUE_PREC = 0.999999;
	public static final double MAX_VALUE_PREC = 6.000001;

	public static final double DEF_VALUE_PREC = 3.5;

	// Number of decades for Poisson values.
	// Must be between 1 and 9, inclusive.
	// The largest value is 10^value_decades.

	private int value_decades;

	public static final int MIN_VALUE_DECADES = 1;
	public static final int MAX_VALUE_DECADES = 9;

	public static final int DEF_VALUE_DECADES = 7;

	// Precision (number of significant digits) for Poisson means.
	// Must be between 1.0 and 6.0, inclusive.
	// Note that a fractional number of digits is possible.

	private double mean_precision;

	public static final double MIN_MEAN_PREC = 0.999999;
	public static final double MAX_MEAN_PREC = 6.000001;

	public static final double DEF_MEAN_PREC = 3.5;

	// Number of decades for Poisson means.
	// Must be between 1 and 9, inclusive.
	// The largest mean is 10^mean_decades.
	// Note: The RJ code uses 7 decades.

	private int mean_decades;

	public static final int MIN_MEAN_DECADES = 1;
	public static final int MAX_MEAN_DECADES = 9;

	public static final int DEF_MEAN_DECADES = 7;

	// Lower cumulative distribution limit for each stacked Poisson distribution.
	// Must be between 0.000001 and 0.5.
	// Note: The RJ code uses 0.0001.

	private double lower_cdf;

	public static final double MIN_LOWER_CDF = 0.000000999999;
	public static final double MAX_LOWER_CDF = 0.5;

	public static final double DEF_LOWER_CDF = 0.0001;

	// Upper cumulative distribution limit for each stacked Poisson distribution.
	// Must be between 0.5 and 0.999999.
	// Note: The RJ code uses 0.9999.

	private double upper_cdf;

	public static final double MIN_UPPER_CDF = 0.5;
	public static final double MAX_UPPER_CDF = 0.999999000001;

	public static final double DEF_UPPER_CDF = 0.9999;

	// Upper cumulative distribution limit for the maximum mean in a stack.
	// Must be between 0.5 and 0.999999.
	// Note: The RJ code uses 0.999.

	private double max_upper_cdf;

	public static final double DEF_MAX_UPPER_CDF = 0.999;




	//----- Ranges -----

	// Number of values in one decade.

	private int value_decade_length;

	// The list of values at which the Poisson distribution is evaluated.
	// The list begins with consecutive integers from 0 through 10^(round(value_precision) - 1).
	// After that, the spacing increases approximately logarithmically.
	// The last element is 10^value_decades.
	// It is guaranteed to be at least 11 elements, beginning with 0 through 10.
	// It is guaranteed that the spacing between adjacent elements is non-decreasing.

	private int[] value_range;

	// List that splits the values into intervals.
	// The length is the same as value_range.
	// For n < value_range.length - 1, define
	//   value_split[n] = (value_range[n] + value_range[n+1])/2
	// and therefore
	//   value_range[n] <= value_split[n] < value_range[n+1]
	// For the last element, one option is to define
	//   value_split[value_range.length - 1] = value_range[value_range.length - 1]
	// which is consistent with the above if we pretend that
	//   value_range[value_range.length] = value_range[value_range.length - 1]
	// Another option for the last element, which we currently use, is to define
	//   value_split[value_range.length - 1] = value_range[value_range.length - 1]
	//       + (value_range[value_range.length - 1] - value_range[value_range.length - 2])/2
	// which is consistent with the above if we pretend that
	//   value_range[value_range.length] = value_range[value_range.length - 1]
	//       + (value_range[value_range.length - 1] - value_range[value_range.length - 2])
	// It is guaranteed that the first 10 elements are 0 through 9.
	// It is guaranteed that the spacing between adjacent elements is non-decreasing,
	// except possibly for the spacing between the last two elements.
	//
	// For a given n, value_range[n] represents all Poisson values v satisfying
	//   value_split[n-1] < v <= value_split[n]
	// For this purpose, pretend value_split[-1] = -1.
	//
	// Given a Poisson value v, the element of value_range that represents it is found as follows.
	// Find n so that
	//   0 <= n <= value_split.length
	//   value_split[n-1] <= v - 1 < value_split[n]
	// For this purpose, pretend value_split[-1] = -infinity and value_split[value_split.length] = infinity.
	// If n == value_split.length then v is off the top of the scale.
	// Otherwise, v is represented by value_range[n].

	private int[] value_split;

	// Number of means in one decade.

	private int mean_decade_length;

	// The list of mean values for which the Poisson distribution is cached.
	// Contains logarithmically spaced means.

	private double[] mean_range;

	// List that splits the means into intervals.
	// The length is the same as mean_range.
	// For n > 0, define
	//   mean_split[n] = sqrt (mean_range[n-1] * mean_range[n])
	// and therefoe
	//   mean_range[n-1] < mean_split[n] < mean_range[n]
	// Also define
	//   mean_split[0] = mean_range[0]
	//
	// For a given n, mean_range[n] represents all means lambda satisfying
	//   mean_split[n] <= lambda < mean_split[n+1]
	// For this purpose, pretend mean_split[mean_split.length] = infinity.
	//
	// A given mean lambda is rounded to one of the values in mean_range as follows.
	// Find n so that
	//   -1 <= n <= mean_split.length - 1
	//   mean_split[n] <= lambda < mean_split[n+1]
	// For this purpose, pretend mean_split[-1] = -infinity and mean_split[mean_split.length] = infinity.
	// If n == -1 then lambda is rounded to zero.  (Note, zero is not in mean_range.)
	// Otherwise, round lambda to mean_range[n].

	private double[] mean_split;




	//----- Caches -----

	// Cached probability distribution functions.
	// Dimension: cached_pdf[mean_range.length][..]
	// The second dimension is variable, depending on the width of the distribution.
	// Each element cached_pdf[m][n] contains a value for value_range[n+o], where the
	// offset value o = cached_offset[m].  The offset must satisfy
	//   0 <= o <= value_range.length - cached_pdf[m].length
	// so that n+o is a valid index into value_range.
	// The value is the cumulative value of the Poisson distribution of mean mean_range[m],
	// from
	//   (value_range[n+o-1] + value_range[n+o])/2  ==  value_split[n+o-1]  exclusive,
	// to
	//   (value_range[n+o] + value_range[n+o+1])/2  ==  value_split[n+o]  inclusive.
	// For this purpose, pretend value_split[-1] = -1, and note that the Poisson cdf of -1 is zero.
	// For the case n+o == 0, the value is the probability of zero, which is exp(-mean).

	private double[][] cached_pdf;

	// Offsets for cached probability distribution functions.
	// Dimension: cached_offset[mean_range.length]

	private int[] cached_offset;

	// Offset where the cumulative distribution function reaches max_upper_cdf.
	// Dimension: cached_upper_offset[mean_range.length]

	private int[] cached_upper_offset;

	// Total of the elements in cached_pdf[m].
	// Dimension: cached_total[mean_range.length]

	private double[] cached_total;

	// Total number of words of cache memory, in the cached_pdf array.

	private long cached_words;

	// A 10-element array that contains the digits 0 through 9 as flowing-point.

	private double[] r_digit;

	// A 10-element array where element n contains 10^n.

	private int[] ten_to_int;




	//----- Construction -----


	

	// Make the array r_digit.

	private void make_r_digit () {
		r_digit = new double[10];
		for (int n = 0; n < 10; ++n) {
			r_digit[n] = (double)n;
		}
		return;
	}



	// Make the array ten_to_int.

	private void make_ten_to_int () {
		ten_to_int = new int[10];
		ten_to_int[0] = 1;
		for (int n = 1; n < 10; ++n) {
			ten_to_int[n] = 10 * ten_to_int[n-1];
		}
		return;
	}



	// Round a discrete precision to an integer.
	// The argument must be integral or half-integral.
	// Half-integers are rounded up to the next larger integer.

	private int round_precision (double prec) {
		return (int)Math.round (prec + 0.25);
	}




	// Return true if the precision is half-integral.
	// The argument must be integral or half-integral.

	private boolean is_half_precision (double prec) {
		int n = round_precision (prec);
		return ((double)n) - prec > 0.25;
	}



	// Calculate the number of elements in a decade, given the desired precision.
	// This version is used for a discrete range, that is, the values.
	// The argument must be integral or half-integral, and between 1.0 and 6.0 inclusive.
	// If prec == 1.0, the return value is 9, representing values 1 through 9.
	// Otherwise, let N = 10^(round(prec) - 1).
	// For integral precision, the return value allows for:
	//   N to 2*N-1 in steps of 1.
	//   2*N to 5*N-2 in steps of 2.
	//   5*N to 10*N-5 in steps of 5.
	// For half-integral precision, the return value allows for:
	//   N to 2*N-2 in steps of 2.
	//   2*N to 5*N-5 in steps of 5.
	//   5*N to 10*N-10 in steps of 10.

	private int calc_decade_length (double prec) {
		if (prec < MIN_VALUE_PREC || prec > MAX_VALUE_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.calc_decade_length: Precision out-of-range: prec = " + prec);
		}

		int round_prec = round_precision (prec);

		// Case of precision == 1

		if (round_prec == 1) {
			return 9;
		}

		// Starting value of decade

		int n = ten_to_int[round_prec - 1];

		// Half-integral case

		if (is_half_precision (prec)) {
			//return (2*n - n)/2 + (5*n - 2*n)/5 + (10*n - 5*n)/10;
			return (8*n)/5;
		}

		// Integral case

		//return (2*n - n) + (5*n - 2*n)/2 + (10*n - 5*n)/5;
		return (7*n)/2;
	}




	// Calculate the number of elements in a decade, given the desired precision.
	// This version is used for a continuous range, that is, the means.
	// The return value n is chosen so that
	//   (1 + 10^(1-prec))^n == 10
	// which is equivalent to
	//   n = log(10) / log(1 + 10^(1-prec))
	// We also impose the requirement n >= 9.

	private int calc_cont_decade_length (double prec) {
		if (prec < MIN_MEAN_PREC || prec > MAX_MEAN_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.calc_cont_decade_length: Precision out-of-range: prec = " + prec);
		}

		double n = C_LOG_10 / Math.log1p(Math.pow(10.0, 1.0 - prec));

		return Math.max (9, (int)Math.round(n + 0.5));
	}




	// Append a decade of Poisson values to an array.
	// Parameters:
	//  prec = Precision, 1.0 thru 6.0, integral or half-integral.
	//  arr = Array to receive values.
	//  index = Starting index into array.
	//  mult = Multiplier to apply to each value.
	// Returns the ending index into array.

	private int append_decade_values (double prec, int[] arr, int index, int mult) {
		if (prec < MIN_VALUE_PREC || prec > MAX_VALUE_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.append_decade_values: Precision out-of-range: prec = " + prec);
		}

		int round_prec = round_precision (prec);

		int ix = index;

		// For prec == 1, just the integers 1 thru 9

		if (round_prec == 1) {
			for (int j = 1; j < 10; ++j) {
				arr[ix++] = j * mult;
			}
		}

		// Otherwise, use spacings of 1, 2, and 5 to approximate log scale and use 'simple' values

		else {
			int n = ten_to_int[round_prec - 1];

			// Half-integral case

			if (is_half_precision (prec)) {
				for (int j = n; j < 2*n; j += 2) {
					arr[ix++] = j * mult;
				}
				for (int j = 2*n; j < 5*n; j += 5) {
					arr[ix++] = j * mult;
				}
				for (int j = 5*n; j < 10*n; j += 10) {
					arr[ix++] = j * mult;
				}
			}

			// Integral case

			else {
				for (int j = n; j < 2*n; ++j) {
					arr[ix++] = j * mult;
				}
				for (int j = 2*n; j < 5*n; j += 2) {
					arr[ix++] = j * mult;
				}
				for (int j = 5*n; j < 10*n; j += 5) {
					arr[ix++] = j * mult;
				}
			}
		}

		// Return next index

		return ix;
	}




	// Make the arrays value_range and value_split.

	private void make_value_range () {
		if (value_precision < MIN_VALUE_PREC || value_precision > MAX_VALUE_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.make_value_range: Precision out-of-range: value_precision = " + value_precision);
		}
		if (value_decades < MIN_VALUE_DECADES || value_decades > MAX_VALUE_DECADES) {
			throw new IllegalArgumentException ("OEStackedPoisson.make_value_range: Decades out-of-range: value_decades = " + value_decades);
		}

		// Get the number of dense and sparse decades

		int dense_decades = Math.min (value_decades, round_precision (value_precision) - 1);
		int sparse_decades = value_decades - dense_decades;

		// Total length is length of dense decades, plus sparse decades, plus 1

		value_decade_length = calc_decade_length(value_precision);

		int dense_len = ten_to_int[dense_decades];		// includes 0
		int sparse_len = sparse_decades * value_decade_length;

		int total_len = dense_len + sparse_len + 1;

		// Allocate the array

		value_range = new int[total_len];

		// Fill the dense decades

		int ix;
		for (ix = 0; ix < dense_len; ++ix) {
			value_range[ix] = ix;
		}

		// Fill the sparse decades

		for (int j = 0; j < sparse_decades; ++j) {
			ix = append_decade_values (value_precision, value_range, ix, ten_to_int[j]);
		}

		// The final power of 10

		value_range[ix++] = ten_to_int[value_decades];

		// Must be at the end of the array

		if (ix != total_len) {
			throw new IllegalStateException ("OEStackedPoisson.make_value_range: Array length mismatch: total_len = " + total_len + ", ix = " + ix);
		}

		// Now allocate the splitting array

		value_split = new int[total_len];

		// Fill in splitting values

		for (int k = 0; k < total_len - 1; ++k) {
			value_split[k] = (value_range[k] + value_range[k + 1])/2;
		}

		//value_split[total_len - 1] = value_range[total_len - 1];
		value_split[total_len - 1] = value_range[total_len - 1] + ((value_range[total_len - 1] - value_range[total_len - 2])/2);

		return;
	}




	// Make the arrays mean_range and mean_split.

	private void make_mean_range () {
		if (mean_precision < MIN_MEAN_PREC || mean_precision > MAX_MEAN_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.make_mean_range: Precision out-of-range: mean_precision = " + mean_precision);
		}
		if (mean_decades < MIN_MEAN_DECADES || mean_decades > MAX_MEAN_DECADES) {
			throw new IllegalArgumentException ("OEStackedPoisson.make_mean_range: Decades out-of-range: mean_decades = " + mean_decades);
		}

		// Number of points is the number of decades time the length per decade, plus 1 for the final power of 10

		mean_decade_length = calc_cont_decade_length(mean_precision);

		int num_points = (mean_decades * mean_decade_length) + 1;

		double lower_limit = 1.0;

		// Calculate the lower limit of the log scale so that the cdf of zero is less than upper_cdf
		// A given mean lambda needs to be included if
		//   exp(-lambda) < upper_cdf
		// or equivalently
		//   lambda > -log(upper_cdf)

		double[] lower_range = OEDiscreteRange.makeLog((6 * mean_decade_length) + 1, 0.000001, 1.0).get_range_array();
		int lower_index = OEArraysCalc.bsearch_array (lower_range, -Math.log(upper_cdf));

		lower_limit = lower_range[lower_index];		// note lower_index cannot be lower_range.length because upper_cdf >= 0.5
		int lower_points = (lower_range.length - 1) - lower_index;

		num_points += lower_points;

		// Create a log scale

		mean_range = OEDiscreteRange.makeLog(num_points, lower_limit, (double)(ten_to_int[mean_decades])).get_range_array();

		// Must be the correct length

		if (num_points != mean_range.length) {
			throw new IllegalStateException ("OEStackedPoisson.make_mean_range: Array length mismatch: num_points = " + num_points + ", mean_range.length = " + mean_range.length);
		}

		// Now allocate the splitting array

		mean_split = new double[num_points];

		// Fill in the splitting values

		mean_split[0] = mean_range[0];
		for (int k = 1; k < num_points; ++k) {
			mean_split[k] = Math.sqrt (mean_range[k - 1] * mean_range[k]);
		}

		return;
	}




	// Make the cached distributions.

	private void make_cached () {

		// Allocate the cache arrays

		cached_pdf = new double[mean_range.length][];
		cached_offset = new int[mean_range.length];
		cached_upper_offset = new int[mean_range.length];
		cached_total = new double[mean_range.length];
		cached_words = 0L;

		// Work array to hold distributions

		double[] work = new double[value_range.length];

		// Loop over means

		for (int mean_ix = 0; mean_ix < mean_range.length; ++mean_ix) {
			double mean = mean_range[mean_ix];

			// Start at the first value greater than the mean rounded to integer

			int start_ix = OEArraysCalc.bsearch_array (value_range, (int)Math.round(mean));

			if (start_ix <= 0) {
				throw new IllegalStateException ("OEStackedPoisson.make_cached: Mean apparently negative: start_ix = " + start_ix + ", mean = " + mean);
			}
			if (start_ix >= value_range.length) {
				start_ix = value_range.length - 1;
			}

			double start_cdf = Probability.poisson (value_split[start_ix], mean);

			// Downward scan, always doing at least one step
			// down_ix is one less than the first index that has a probability

			int down_ix = start_ix;				// one less than the first term that has a probability
			double down_cdf = start_cdf;		// the cdf for value_split[down_ix]

			do {
				double next_cdf = Math.min(down_cdf, ((down_ix == 0) ? 0.0 : Probability.poisson (value_split[down_ix - 1], mean)));
				work[down_ix] = down_cdf - next_cdf;

				--down_ix;
				down_cdf = next_cdf;

			} while (down_cdf > lower_cdf);

			// Upward scan

			int up_ix = start_ix;				// the last term that has a probability
			double up_cdf = start_cdf;			// the cdf for value_split[up_ix]

			cached_upper_offset[mean_ix] = up_ix;

			while (up_ix < value_range.length - 1 && up_cdf < upper_cdf) {
				++up_ix;
				double next_cdf = Math.max(up_cdf, Probability.poisson (value_split[up_ix], mean));
				work[up_ix] = next_cdf - up_cdf;

				if (up_cdf < max_upper_cdf) {
					cached_upper_offset[mean_ix] = up_ix;		// last index seen below upper cdf cutoff
				}

				up_cdf = next_cdf;
			}

			// Offset is the first term with a probability

			int off = down_ix + 1;
			int len = up_ix - down_ix;

			// Save the offset

			cached_offset[mean_ix] = off;

			// Allocate array and save the nonzero probabilities

			cached_pdf[mean_ix] = new double[len];
			double total = 0.0;
			for (int j = 0; j < len; ++j) {
				cached_pdf[mean_ix][j] = work[j + off];
				total += work[j + off];
				++cached_words;
			}
			cached_total[mean_ix] = total;
		}

		return;
	}




	// Constructor, specifying all parameters.

	public OEStackedPoisson (
		double the_value_precision,
		int the_value_decades,
		double the_mean_precision,
		int the_mean_decades,
		double the_lower_cdf,
		double the_upper_cdf,
		double the_max_upper_cdf
	) {

		// Validate parameters

		if (the_value_precision < MIN_VALUE_PREC || the_value_precision > MAX_VALUE_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Value precision out-of-range: the_value_precision = " + the_value_precision);
		}
		if (the_value_decades < MIN_VALUE_DECADES || the_value_decades > MAX_VALUE_DECADES) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Value decades out-of-range: the_value_decades = " + the_value_decades);
		}
		if (the_mean_precision < MIN_MEAN_PREC || the_mean_precision > MAX_MEAN_PREC) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Mean precision out-of-range: the_mean_precision = " + the_mean_precision);
		}
		if (the_mean_decades < MIN_MEAN_DECADES || the_mean_decades > MAX_MEAN_DECADES) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Mean decades out-of-range: the_mean_decades = " + the_mean_decades);
		}
		if (the_lower_cdf < MIN_LOWER_CDF || the_lower_cdf > MAX_LOWER_CDF) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Lower CDF cutoff out-of-range: the_lower_cdf = " + the_lower_cdf);
		}
		if (the_upper_cdf < MIN_UPPER_CDF || the_upper_cdf > MAX_UPPER_CDF) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Upper CDF cutoff out-of-range: the_upper_cdf = " + the_upper_cdf);
		}
		if (the_max_upper_cdf < MIN_UPPER_CDF || the_max_upper_cdf > MAX_UPPER_CDF) {
			throw new IllegalArgumentException ("OEStackedPoisson.OEStackedPoisson: Maximum-mean upper CDF cutoff out-of-range: the_max_upper_cdf = " + the_max_upper_cdf);
		}

		// Save parameters

		value_precision = the_value_precision;
		value_decades = the_value_decades;
		mean_precision = the_mean_precision;
		mean_decades = the_mean_decades;
		lower_cdf = the_lower_cdf;
		upper_cdf = the_upper_cdf;
		max_upper_cdf = the_max_upper_cdf;

		// Make arrays r_digit and ten_to_int

		make_r_digit();
		make_ten_to_int();

		// Make the ranges

		make_value_range();
		make_mean_range();

		// Make the cached distributions

		make_cached();
	}




	// Constructor, specifying just the precisions.

	public OEStackedPoisson (
		double the_value_precision,
		double the_mean_precision
	) {
		this (
			the_value_precision,
			DEF_VALUE_DECADES,
			the_mean_precision,
			DEF_MEAN_DECADES,
			DEF_LOWER_CDF,
			DEF_UPPER_CDF,
			DEF_MAX_UPPER_CDF
		);
	}




	// Constructor, using all defaults.

	public OEStackedPoisson (
	) {
		this (
			DEF_VALUE_PREC,
			DEF_VALUE_DECADES,
			DEF_MEAN_PREC,
			DEF_MEAN_DECADES,
			DEF_LOWER_CDF,
			DEF_UPPER_CDF,
			DEF_MAX_UPPER_CDF
		);
	}




	// A singleton, which can be used to store a system-wide default.
	// Access to this variable must be synchronized.

	private static OEStackedPoisson singleton = null;


	// Get the singleton.
	// If it doesn't exist, create one with default parameters.

	public static synchronized OEStackedPoisson get_singleton () {
		if (singleton == null) {
			singleton = new OEStackedPoisson();
		}
		return singleton;
	}


	// Set the singleton value.
	// Can be null to erase the current singleton without creating a new one.

	public static synchronized void set_singleton (OEStackedPoisson the_singleton) {
		singleton = the_singleton;
		return;
	}




	// Produce a summary string.

	public String summary_string () {
		StringBuilder result = new StringBuilder();

		// Variable summary

		result.append ("OEStackedPoisson:" + "\n");
		result.append ("value_precision = " + value_precision + "\n");
		result.append ("value_decades = " + value_decades + "\n");
		result.append ("mean_precision = " + mean_precision + "\n");
		result.append ("mean_decades = " + mean_decades + "\n");
		result.append ("lower_cdf = " + lower_cdf + "\n");
		result.append ("upper_cdf = " + upper_cdf + "\n");
		result.append ("max_upper_cdf = " + max_upper_cdf + "\n");

		result.append ("value_decade_length = " + value_decade_length + "\n");
		result.append ("value_range.length = " + value_range.length + "\n");
		result.append ("value_split.length = " + value_split.length + "\n");
		result.append ("mean_decade_length = " + mean_decade_length + "\n");
		result.append ("mean_range.length = " + mean_range.length + "\n");
		result.append ("mean_split.length = " + mean_split.length + "\n");

		result.append ("cached_pdf.length = " + cached_pdf.length + "\n");
		result.append ("cached_offset.length = " + cached_offset.length + "\n");
		result.append ("cached_upper_offset.length = " + cached_upper_offset.length + "\n");
		result.append ("cached_total.length = " + cached_total.length + "\n");
		result.append ("cached_words = " + cached_words + "\n");
		result.append ("r_digit.length = " + r_digit.length + "\n");
		result.append ("ten_to_int.length = " + ten_to_int.length + "\n");

		return result.toString();
	}




	// Produce a detail string.
	// If f_dump is true, dump the entire contents of the cached distributions.

	public String detail_string (boolean f_dump) {
		StringBuilder result = new StringBuilder();

		// Variable summary

		result.append ("OEStackedPoisson:" + "\n");
		result.append ("value_precision = " + value_precision + "\n");
		result.append ("value_decades = " + value_decades + "\n");
		result.append ("mean_precision = " + mean_precision + "\n");
		result.append ("mean_decades = " + mean_decades + "\n");
		result.append ("lower_cdf = " + lower_cdf + "\n");
		result.append ("upper_cdf = " + upper_cdf + "\n");
		result.append ("max_upper_cdf = " + max_upper_cdf + "\n");

		result.append ("value_decade_length = " + value_decade_length + "\n");
		result.append ("value_range.length = " + value_range.length + "\n");
		result.append ("value_split.length = " + value_split.length + "\n");
		result.append ("mean_decade_length = " + mean_decade_length + "\n");
		result.append ("mean_range.length = " + mean_range.length + "\n");
		result.append ("mean_split.length = " + mean_split.length + "\n");

		result.append ("cached_pdf.length = " + cached_pdf.length + "\n");
		result.append ("cached_offset.length = " + cached_offset.length + "\n");
		result.append ("cached_upper_offset.length = " + cached_upper_offset.length + "\n");
		result.append ("cached_total.length = " + cached_total.length + "\n");
		result.append ("cached_words = " + cached_words + "\n");
		result.append ("r_digit.length = " + r_digit.length + "\n");
		result.append ("ten_to_int.length = " + ten_to_int.length + "\n");

		// Values

		result.append ("\n");
		result.append ("Values:  range  split" + "\n");
		for (int value_ix = 0; value_ix < value_range.length; ++value_ix) {
			result.append (String.format ("%10d  %10d  %10d\n",
				value_ix,
				value_range[value_ix],
				value_split[value_ix]
			));
		}

		// Means

		result.append ("\n");
		result.append ("Means:  range  split  length  offset  upper  total" + "\n");
		for (int mean_ix = 0; mean_ix < mean_range.length; ++mean_ix) {
			result.append (String.format ("%10d  % .6e  % .6e  %10d  %10d  %10d  % .6f\n",
				mean_ix,
				mean_range[mean_ix],
				mean_split[mean_ix],
				cached_pdf[mean_ix].length,
				cached_offset[mean_ix],
				cached_upper_offset[mean_ix],
				cached_total[mean_ix]
			));
			if (f_dump) {
				for (int j = 0; j < cached_pdf[mean_ix].length; ++j) {
					if (j == 0) {
						result.append ("          ");
					} else if (j % 8 == 0) {
						result.append ("\n          ");
					}
					result.append (String.format ("  % .6e", cached_pdf[mean_ix][j]));
				}
				result.append ("\n");
			}
		}

		return result.toString();
	}




	//----- Accumulators -----




	// The Accumulator class is used to build a stacked Poisson distribution.
	// It can also accommodate point distributions and shifted Poisson distributions.
	// It contains a probability distribution for each Poisson value in value_range,
	// plus a separate variable to hold the probability of one or more.
	// The probability distribution is converted into a cumulative distribution for
	// the purpose of reading out fractiles.
	// Each distribution is added with a weight.  Weights do not need to sum to 1.0.
	// Threading: An individual accumulator can only be used by one thread, but
	// multiple threads can each have their own accumulator for the same OEStackedPoisson
	// object.

	public class Accumulator {

		//--- The distribution ---

		// The probability distribution.
		// Dimension: prob_dist[value_range.length]

		private double[] prob_dist;

		// The probability of occurrence (the complement of the probability of zero).

		private double prob_occur;

		// The total weight of all the distributions.

		private double total_weight;

		// The size of the initial segment of prob_dist which can contain non-zero values.
		// Must satisfy: 0 <= support_size <= active_size

		private int support_size;

		// The size of the initial segment of prob_dist being used for the current operation.
		// Must satisfy: 1 <= active_size <= prob_dist.length

		private int active_size;




		//--- Functions for supplying distributions -----




		// Add a point mass to the distribution.
		// Parameters:
		//  value = The value of the point mass, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.

		public final void add_point_mass (int value, double weight) {

			// Record the total weight

			total_weight += weight;

			// If non-zero, add to the probability of occurrence

			if (value != 0) {
				prob_occur += weight;
			}

			// Search to find the index for the given value

			final int value_ix = OEArraysCalc.bsearch_array (value_split, value - 1, 0, active_size);

			// If within the active range, add to probability distribution

			if (value_ix < active_size) {
				prob_dist[value_ix] += weight;

				// Adjust support if needed

				if (support_size <= value_ix) {
					support_size = value_ix + 1;
				}
			}

			return;
		}


		public final void add_point_mass (int value) {

			// Record the total weight

			total_weight += 1.0;

			// If non-zero, add to the probability of occurrence

			if (value != 0) {
				prob_occur += 1.0;
			}

			// Search to find the index for the given value

			final int value_ix = OEArraysCalc.bsearch_array (value_split, value - 1, 0, active_size);

			// If within the active range, add to probability distribution

			if (value_ix < active_size) {
				prob_dist[value_ix] += 1.0;

				// Adjust support if needed

				if (support_size <= value_ix) {
					support_size = value_ix + 1;
				}
			}

			return;
		}




		// Add a Poisson distribution.
		// Parameters:
		//  lambda = The mean of the Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.

		public final void add_poisson (double lambda, double weight) {

			// Record the total weight

			total_weight += weight;

			// Adjust the probability of occurrence

			prob_occur -= (Math.expm1(-lambda) * weight);

			// Find the index for the rounded mean

			final int mean_ix = OEArraysCalc.bsearch_array (mean_split, lambda) - 1;

			// If rounded mean is zero, just add probability to zero value

			if (mean_ix < 0) {
				prob_dist[0] += weight;

				if (support_size == 0) {
					support_size = 1;
				}
			}

			// Otherwise, add the cached distribution

			else {

				// Cached Poisson distribution for this mean

				final double[] cached_dist = cached_pdf[mean_ix];

				// Index into values where the Poisson begins

				int value_ix = cached_offset[mean_ix];

				// The top value index
				
				final int value_ix_top = Math.min(value_ix + cached_dist.length, active_size);

				// Add the cached distribution, with weight

				for (int ix = 0; value_ix < value_ix_top; ++ix, ++value_ix) {
					prob_dist[value_ix] += (cached_dist[ix] * weight);
				}

				// Adjust support if needed

				if (support_size < value_ix_top) {
					support_size = value_ix_top;
				}
			}

			return;
		}


		public final void add_poisson (double lambda) {

			// Record the total weight

			total_weight += 1.0;

			// Adjust the probability of occurrence

			prob_occur -= Math.expm1(-lambda);

			// Find the index for the rounded mean

			final int mean_ix = OEArraysCalc.bsearch_array (mean_split, lambda) - 1;

			// If rounded mean is zero, just add probability to zero value

			if (mean_ix < 0) {
				prob_dist[0] += 1.0;

				if (support_size == 0) {
					support_size = 1;
				}
			}

			// Otherwise, add the cached distribution

			else {

				// Cached Poisson distribution for this mean

				final double[] cached_dist = cached_pdf[mean_ix];

				// Index into values where the Poisson begins

				int value_ix = cached_offset[mean_ix];

				// The top value index
				
				final int value_ix_top = Math.min(value_ix + cached_dist.length, active_size);

				// Add the cached distribution, with weight

				for (int ix = 0; value_ix < value_ix_top; ++ix, ++value_ix) {
					prob_dist[value_ix] += cached_dist[ix];
				}

				// Adjust support if needed

				if (support_size < value_ix_top) {
					support_size = value_ix_top;
				}
			}

			return;
		}




		// Add a shifted Poisson distribution.
		// Parameters:
		//  lambda = The mean of the Poisson distribution, must be >= 0.
		//  shift = Shift to apply to Poisson distribution, must be >= 0.
		//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.

		// Implementation note:  Each element of the cached Poisson distribution is
		// applied to the value obtained by adding shift to the corresponding element
		// of value_range.  Because each element of value_range actually represents
		// the total probability over an interval of values, this introduces some error,
		// due to the fact that a shifted interval may not match up to one of the
		// unshifed intervals.  Because intervals are non-decreasing size, a shifted
		// interval can overlap at most two unshifted intervals, which means that
		// the error in applying probability can be at most one step in value_range.
		// Note that there is no error in the initial part of value_range that
		// consists of consecutive integers.

		// If shift == 0 then this function peforms the same operation as add_poisson(lambda, weight).
		// If lambda == 0 then this function performs the same operation as add_point_mass(shift, weight).

		public final void add_shifted_poisson (double lambda, int shift, double weight) {

			// If the shift is zero, use the unshifted routine

			if (shift == 0) {
				add_poisson (lambda, weight);
				return;
			}

			// Find the index for the rounded mean

			int mean_ix = OEArraysCalc.bsearch_array (mean_split, lambda) - 1;

			// If rounded mean is zero, treat it as a point mass

			if (mean_ix < 0) {
				add_point_mass (shift, weight);
				return;
			}

			// Record the total weight

			total_weight += weight;

			// Probability of occurrence is 1.0 because shift > 0

			prob_occur += weight;

			// Cached Poisson distribution for this mean

			final double[] cached_dist = cached_pdf[mean_ix];

			// The value index offset for this mean

			final int offset = cached_offset[mean_ix];

			// The current value

			int value = value_range[offset] + shift;

			// Search to find the index for the current value

			int value_ix = OEArraysCalc.bsearch_array (value_split, value - 1, 0, active_size);

			// If within the active range ...

			if (value_ix < active_size) {

				// Add the first element of cached distribution

				prob_dist[value_ix] += (cached_dist[0] * weight);

				// Loop over remaining elements of cached distribution

				for (int ix = 1; ix < cached_dist.length; ++ix) {

					// New current value

					value = value_range[ix + offset] + shift;

					// If we need to step the value index ...

					if (value > value_split[value_ix]) {
						++value_ix;

						// If reached end of active range, stop

						if (value_ix >= active_size) {

							// Adjust support if needed

							if (support_size < active_size) {
								support_size = active_size;
							}

							return;
						}
					}

					// Add element of cached distribution

					prob_dist[value_ix] += (cached_dist[ix] * weight);
				}

				// Adjust support if needed, value_ix contains the last element we wrote into

				if (support_size <= value_ix) {
					support_size = value_ix + 1;
				}
			}

			return;
		}


		public final void add_shifted_poisson (double lambda, int shift) {

			// If the shift is zero, use the unshifted routine

			if (shift == 0) {
				add_poisson (lambda);
				return;
			}

			// Find the index for the rounded mean

			int mean_ix = OEArraysCalc.bsearch_array (mean_split, lambda) - 1;

			// If rounded mean is zero, treat it as a point mass

			if (mean_ix < 0) {
				add_point_mass (shift);
				return;
			}

			// Record the total weight

			total_weight += 1.0;

			// Probability of occurrence is 1.0 because shift > 0

			prob_occur += 1.0;

			// Cached Poisson distribution for this mean

			final double[] cached_dist = cached_pdf[mean_ix];

			// The value index offset for this mean

			final int offset = cached_offset[mean_ix];

			// The current value

			int value = value_range[offset] + shift;

			// Search to find the index for the current value

			int value_ix = OEArraysCalc.bsearch_array (value_split, value - 1, 0, active_size);

			// If within the active range ...

			if (value_ix < active_size) {

				// Add the first element of cached distribution

				prob_dist[value_ix] += cached_dist[0];

				// Loop over remaining elements of cached distribution

				for (int ix = 1; ix < cached_dist.length; ++ix) {

					// New current value

					value = value_range[ix + offset] + shift;

					// If we need to step the value index ...

					if (value > value_split[value_ix]) {
						++value_ix;

						// If reached end of active range, stop

						if (value_ix >= active_size) {

							// Adjust support if needed

							if (support_size < active_size) {
								support_size = active_size;
							}

							return;
						}
					}

					// Add element of cached distribution

					prob_dist[value_ix] += cached_dist[ix];
				}

				// Adjust support if needed, value_ix contains the last element we wrote into

				if (support_size <= value_ix) {
					support_size = value_ix + 1;
				}
			}

			return;
		}




		//--- Construction ---




		// Constructor.

		public Accumulator () {
			prob_dist = new double[value_range.length];
			OEArraysCalc.zero_array (prob_dist);

			prob_occur = 0.0;
			total_weight = 0.0;

			support_size = 1;
			active_size = prob_dist.length;
		}




		// Clear the accumulator to zero.
		// This implictly sets that active region to the entire prob_dist array.

		public final void clear () {
			OEArraysCalc.zero_array (prob_dist, 0, support_size);

			prob_occur = 0.0;
			total_weight = 0.0;

			support_size = 1;
			active_size = prob_dist.length;

			return;
		}



		// Clear the accumulator to zero.
		// Parameters:
		//  max_lambda = The largest mean of any Poisson distribution, must be >= 0.
		//  max_shift = The largest shift to apply to any Poisson distribution, must be >= 0.
		//  max_value = The largest value of any point mass, must be >= 0.

		public final void clear (double max_lambda, int max_shift, int max_value) {
			OEArraysCalc.zero_array (prob_dist, 0, support_size);

			prob_occur = 0.0;
			total_weight = 0.0;

			support_size = 1;

			// The largest value we need to include, first assuming zero lambda

			int value = Math.max (max_value, max_shift);

			// Find the index for the rounded mean

			int mean_ix = OEArraysCalc.bsearch_array (mean_split, max_lambda) - 1;

			// If rounded mean is non-zero, use shift value associted with upper offset

			if (mean_ix >= 0) {
				value = Math.max (max_value, max_shift + value_range[cached_upper_offset[mean_ix]]);
			}

			// Search to find the index which includes that value

			int value_ix = OEArraysCalc.bsearch_array (value_split, value - 1);

			// The active region extends to that index, inclusive (note it is at least 1)

			active_size = Math.min (prob_dist.length, value_ix + 1);

			return;
		}




		// Convert the probability distribution to a cumulative distribution.
		// Note that only the support is converted.

		public final void cumulate () {
			double total = prob_dist[0];
			for (int ix = 1; ix < support_size; ++ix) {
				total += prob_dist[ix];
				prob_dist[ix] = total;
			}
			return;
		}




		// Produce a summary string.

		public String summary_acc_string () {
			StringBuilder result = new StringBuilder();

			// Variable summary

			result.append ("OEStackedPoisson.Accumulator:" + "\n");
			result.append ("prob_dist.length = " + prob_dist.length + "\n");
			result.append ("prob_occur = " + prob_occur + "\n");
			result.append ("total_weight = " + total_weight + "\n");
			result.append ("support_size = " + support_size + "\n");
			result.append ("active_size = " + active_size + "\n");

			return result.toString();
		}




		// Produce a detail string.
		// If f_full is true, display the entirety of prob_dist instead of just the support.

		public String detail_acc_string (boolean f_full) {
			StringBuilder result = new StringBuilder();

			// Variable summary

			result.append ("OEStackedPoisson.Accumulator:" + "\n");
			result.append ("prob_dist.length = " + prob_dist.length + "\n");
			result.append ("prob_occur = " + prob_occur + "\n");
			result.append ("total_weight = " + total_weight + "\n");
			result.append ("support_size = " + support_size + "\n");
			result.append ("active_size = " + active_size + "\n");

			// Values

			result.append ("\n");
			result.append ("Values:  range  split  prob" + "\n");
			int value_ix_top = (f_full ? value_range.length : support_size);
			for (int value_ix = 0; value_ix < value_ix_top; ++value_ix) {
				result.append (String.format ("%10d  %10d  %10d  % .6e\n",
					value_ix,
					value_range[value_ix],
					value_split[value_ix],
					prob_dist[value_ix]
				));
			}

			return result.toString();
		}




		// Combine this accumulator with another accumulator.
		// All probabilities from the other accumulator are added to this accumulator.

		public final void combine_with (Accumulator other) {
			prob_occur += other.prob_occur;
			total_weight += other.total_weight;
			support_size = Math.max (support_size, other.support_size);
			active_size = Math.max (active_size, other.active_size);

			for (int ix = 0; ix < other.support_size; ++ix) {
				prob_dist[ix] += other.prob_dist[ix];
			}

			return;
		}




		//--- Readout ---




		// Get the probability of occurrence.
		// Returns the probability that the value is > 0.

		public final double get_prob_occur () {
			if (total_weight == 0.0) {
				return 0.0;
			}
			return prob_occur / total_weight;
		}




		// Get a fractile.
		// Parameters:
		//  frac = Fractile to find, should be between 0.0 and 1.0.
		// Returns an element from value_range, such that the cumulative distribution
		// function of that value is > frac.
		// Note: The cumulate() function must have been called.

		public final int get_fractile (double frac) {
			int value_ix = Math.min (OEArraysCalc.bsearch_array (prob_dist, frac * total_weight, 0, support_size), prob_dist.length - 1);
			return value_range[value_ix];
		}




		// Get the cumulative probability.
		// Parameters:
		//  v = Value, must be >= 0.
		// Returns the probability that the value is <= v.
		// Note: The cumulate() function must have been called.

		public final double get_cum_prob (int v) {

			if (total_weight == 0.0) {
				return 1.0;
			}

			// Search to find the index which includes the requested value

			int value_ix = OEArraysCalc.bsearch_array (value_split, v - 1, 0, support_size);

			// Return the associated probability

			if (value_ix >= support_size) {
				return 1.0;
			}

			return prob_dist[value_ix] / total_weight;
		}




		// Get the probability of exceedence.
		// Parameters:
		//  v = Value, must be >= 0.
		// Returns the probability that the value is > v.
		// Note: The cumulate() function must have been called.
		// Note: Because of truncation, this function cannot return values very close
		// to 0.0 or 1.0.  Use get_prob_occur() to get very small probabilites of occurrence.

		public final double get_probex (int v) {

			if (total_weight == 0.0) {
				return 0.0;
			}

			//// Special handling for zero value so we can return very small probabilities of occurrence
			//
			//if (v == 0) {
			//	return prob_occur / total_weight;
			//}

			// Search to find the index which includes the requested value

			int value_ix = OEArraysCalc.bsearch_array (value_split, v - 1, 0, support_size);

			// Return the associated probability

			if (value_ix >= support_size) {
				return 0.0;
			}

			return 1.0 - (prob_dist[value_ix] / total_weight);
		}

	}




	//----- Accumulator creation -----




	// Make an accumulator.
	// Note: The accumulator is initially clear.

	public final Accumulator make_accumulator () {
		return new Accumulator();
	}




	// Make an array of accumulators.
	// Parameters:
	//  dest = 1D or 2D array.
	// Each element of the array is filled with a new accumulator.

	public final void make_acc_array (Accumulator[] dest) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m] = make_accumulator();
		}
		return;
	}


	public final void make_acc_array (Accumulator[][] dest) {
		for (int m = 0; m < dest.length; ++m) {
			make_acc_array (dest[m]);
		}
		return;
	}




	//----- Accumulator arrays -----




	// Add a point mass to each accumulator in an array of accumulators.
	// Parameters:
	//  dest = 1D or 2D array of accumulators.
	//  value = 1D or 2D array, containing the value of the point mass, must be >= 0.
	//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
	// The value array must have the same dimensions as dest.
	// The same weight is used for all accumulators.

	public static void add_point_mass_acc_array (Accumulator[] dest, int[] value, double weight) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].add_point_mass (value[m], weight);
		}
		return;
	}


	public static void add_point_mass_acc_array (Accumulator[][] dest, int[][] value, double weight) {
		for (int m = 0; m < dest.length; ++m) {
			add_point_mass_acc_array (dest[m], value[m], weight);
		}
		return;
	}


	public static void add_point_mass_acc_array (Accumulator[] dest, int[] value) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].add_point_mass (value[m]);
		}
		return;
	}


	public static void add_point_mass_acc_array (Accumulator[][] dest, int[][] value) {
		for (int m = 0; m < dest.length; ++m) {
			add_point_mass_acc_array (dest[m], value[m]);
		}
		return;
	}




	// Add a Poisson distribution to each accumulator in an array of accumulators.
	// Parameters:
	//  dest = 1D or 2D array of accumulators.
	//  lambda = 1D or 2D array, containing the mean of the Poisson distribution, must be >= 0.
	//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
	// The lambda array must have the same dimensions as dest.
	// The same weight is used for all accumulators.

	public static void add_poisson_acc_array (Accumulator[] dest, double[] lambda, double weight) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].add_poisson (lambda[m], weight);
		}
		return;
	}


	public static void add_poisson_acc_array (Accumulator[][] dest, double[][] lambda, double weight) {
		for (int m = 0; m < dest.length; ++m) {
			add_poisson_acc_array (dest[m], lambda[m], weight);
		}
		return;
	}


	public static void add_poisson_acc_array (Accumulator[] dest, double[] lambda) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].add_poisson (lambda[m]);
		}
		return;
	}


	public static void add_poisson_acc_array (Accumulator[][] dest, double[][] lambda) {
		for (int m = 0; m < dest.length; ++m) {
			add_poisson_acc_array (dest[m], lambda[m]);
		}
		return;
	}




	// Add a shifted Poisson distribution to each accumulator in an array of accumulators.
	// Parameters:
	//  dest = 1D or 2D array of accumulators.
	//  lambda = 1D or 2D array, containing the mean of the Poisson distribution, must be >= 0.
	//  shift = 1D or 2D array, containing the shift to apply to Poisson distribution, must be >= 0.
	//  weight = The weight, must be >= 0.0.  If omitted, 1.0 is assumed.
	// The lambda and shift arrays must have the same dimensions as dest.
	// The same weight is used for all accumulators.

	public static void add_shifted_poisson_acc_array (Accumulator[] dest, double[] lambda, int[] shift, double weight) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].add_shifted_poisson (lambda[m], shift[m], weight);
		}
		return;
	}


	public static void add_shifted_poisson_acc_array (Accumulator[][] dest, double[][] lambda, int[][] shift, double weight) {
		for (int m = 0; m < dest.length; ++m) {
			add_shifted_poisson_acc_array (dest[m], lambda[m], shift[m], weight);
		}
		return;
	}


	public static void add_shifted_poisson_acc_array (Accumulator[] dest, double[] lambda, int[] shift) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].add_shifted_poisson (lambda[m], shift[m]);
		}
		return;
	}


	public static void add_shifted_poisson_acc_array (Accumulator[][] dest, double[][] lambda, int[][] shift) {
		for (int m = 0; m < dest.length; ++m) {
			add_shifted_poisson_acc_array (dest[m], lambda[m], shift[m]);
		}
		return;
	}




	// Clear each accumulator in an array of accumulators.
	// Parameters:
	//  dest = 1D or 2D array of accumulators to clear.
	// Each element of the array is cleared.

	public static void clear_acc_array (Accumulator[] dest) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].clear();
		}
		return;
	}


	public static void clear_acc_array (Accumulator[][] dest) {
		for (int m = 0; m < dest.length; ++m) {
			clear_acc_array (dest[m]);
		}
		return;
	}




	// Convert each accumulator in an array of accumulators to a cumulative distribution.
	// Parameters:
	//  dest = 1D or 2D array of accumulators to cumulate.
	// Each element of the array is cumulated.

	public static void cumulate_acc_array (Accumulator[] dest) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].cumulate();
		}
		return;
	}


	public static void cumulate_acc_array (Accumulator[][] dest) {
		for (int m = 0; m < dest.length; ++m) {
			cumulate_acc_array (dest[m]);
		}
		return;
	}




	// Combine each accumulator in an array of accumulators with another accumulator.
	// Parameters:
	//  dest = 1D or 2D array of accumulators to receive the combination.
	//  src = 1D or 2D array of accumulators providing data for the combination.
	// Each element of the dest array is combined with the corresponding element
	// of the src array.  The two arrays must have the same dimensions.

	public static void combine_acc_array (Accumulator[] dest, Accumulator[] src) {
		for (int m = 0; m < dest.length; ++m) {
			dest[m].combine_with (src[m]);
		}
		return;
	}


	public static void combine_acc_array (Accumulator[][] dest, Accumulator[][] src) {
		for (int m = 0; m < dest.length; ++m) {
			combine_acc_array (dest[m], src[m]);
		}
		return;
	}




	// Get the probability of occurrence for each accumulator in an array of accumulators.
	// Parameters:
	//  src = 1D or 2D array of accumulators.
	// Returns an array, of the same dimension as src, where each element contains
	// the probability that the value is > 0.

	public static double[] prob_occur_acc_array (Accumulator[] src) {
		double[] result = new double[src.length];
		for (int m = 0; m < src.length; ++m) {
			result[m] = src[m].get_prob_occur();
		}
		return result;
	}


	public static double[][] prob_occur_acc_array (Accumulator[][] src) {
		double[][] result = new double[src.length][];
		for (int m = 0; m < src.length; ++m) {
			result[m] = prob_occur_acc_array (src[m]);
		}
		return result;
	}




	// Get a fractile for each accumulator in an array of accumulators.
	// Parameters:
	//  src = 1D or 2D array of accumulators.
	//  frac = Fractile to find, should be between 0.0 and 1.0.
	// Returns an array, of the same dimension as src, where each element contains
	// a value whose cumulative distribution function is > frac.
	// Note: The cumulate_acc_array() function must have been called.

	public static int[] fractile_acc_array (Accumulator[] src, double frac) {
		int[] result = new int[src.length];
		for (int m = 0; m < src.length; ++m) {
			result[m] = src[m].get_fractile (frac);
		}
		return result;
	}


	public static int[][] fractile_acc_array (Accumulator[][] src, double frac) {
		int[][] result = new int[src.length][];
		for (int m = 0; m < src.length; ++m) {
			result[m] = fractile_acc_array (src[m], frac);
		}
		return result;
	}




	// Get a cumulative probability for each accumulator in an array of accumulators.
	// Parameters:
	//  src = 1D or 2D array of accumulators.
	//  v = Value, must be >= 0.
	// Returns an array, of the same dimension as src, where each element contains
	// the probability that the value is <= v.
	// Note: The cumulate_acc_array() function must have been called.

	public static double[] cum_prob_acc_array (Accumulator[] src, int v) {
		double[] result = new double[src.length];
		for (int m = 0; m < src.length; ++m) {
			result[m] = src[m].get_cum_prob (v);
		}
		return result;
	}


	public static double[][] cum_prob_acc_array (Accumulator[][] src, int v) {
		double[][] result = new double[src.length][];
		for (int m = 0; m < src.length; ++m) {
			result[m] = cum_prob_acc_array (src[m], v);
		}
		return result;
	}




	// Get a probability of exceedence for each accumulator in an array of accumulators.
	// Parameters:
	//  src = 1D or 2D array of accumulators.
	//  v = Value, must be >= 0.
	// Returns an array, of the same dimension as src, where each element contains
	// the probability that the value is > v.
	// Note: The cumulate_acc_array() function must have been called.

	public static double[] probex_acc_array (Accumulator[] src, int v) {
		double[] result = new double[src.length];
		for (int m = 0; m < src.length; ++m) {
			result[m] = src[m].get_probex (v);
		}
		return result;
	}


	public static double[][] probex_acc_array (Accumulator[][] src, int v) {
		double[][] result = new double[src.length][];
		for (int m = 0; m < src.length; ++m) {
			result[m] = probex_acc_array (src[m], v);
		}
		return result;
	}




	//----- Testing -----




	// Test function to convert a 2D array to a string.

	private static String test_display_array (int[][] x) {
		StringBuilder result = new StringBuilder();

		for (int m = 0; m < x.length; ++m) {
			for (int n = 0; n < x[m].length; ++n) {
				if (n != 0) {
					result.append ("  ");
				}
				result.append (String.format ("%10d", x[m][n]));
			}
			result.append ("\n");
		}

		return result.toString();
	}

	private static String test_display_array (double[][] x) {
		StringBuilder result = new StringBuilder();

		for (int m = 0; m < x.length; ++m) {
			for (int n = 0; n < x[m].length; ++n) {
				if (n != 0) {
					result.append ("  ");
				}
				result.append (String.format ("% .6e", x[m][n]));
			}
			result.append ("\n");
		}

		return result.toString();
	}




	// Test function to make an array of parameters.

	private static int[][] test_make_parm_int_array (int rows, int cols, int v, int h_mult, int h_step, int v_mult, int v_step) {
		int[][] x = new int[rows][cols];
		x[0][0] = v;
		for (int n = 1; n < cols; ++n) {
			x[0][n] = x[0][n-1] * h_mult + h_step;
		}
		for (int m = 1; m < rows; ++m) {
			for (int n = 0; n < cols; ++n) {
				x[m][n] = x[m-1][n] * v_mult + v_step;
			}
		}
		return x;
	}

	private static double[][] test_make_parm_double_array (int rows, int cols, double v, double h_mult, double h_step, double v_mult, double v_step) {
		double[][] x = new double[rows][cols];
		x[0][0] = v;
		for (int n = 1; n < cols; ++n) {
			x[0][n] = x[0][n-1] * h_mult + h_step;
		}
		for (int m = 1; m < rows; ++m) {
			for (int n = 0; n < cols; ++n) {
				x[m][n] = x[m-1][n] * v_mult + v_step;
			}
		}
		return x;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEStackedPoisson : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  value_precision  mean_precision
		// Create a OEStackedPoisson object with the specified precisions.
		// Display the summary string.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("OEStackedPoisson : Invalid 'test1' subcommand");
				return;
			}

			try {

				double the_value_precision = Double.parseDouble(args[1]);
				double the_mean_precision = Double.parseDouble (args[2]);

				// Say hello

				System.out.println ("Creating OEStackedPoisson and displaying summary string");
				System.out.println ("value_precision = " + the_value_precision);
				System.out.println ("mean_precision = " + the_mean_precision);

				// Create the object

				OEStackedPoisson stkpois = new OEStackedPoisson (
					the_value_precision,
					the_mean_precision
				);

				// Display summary string

				System.out.println ();
				System.out.println (stkpois.summary_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  value_precision  mean_precision  f_dump
		// Create a OEStackedPoisson object with the specified precisions.
		// Display the detail string.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("OEStackedPoisson : Invalid 'test2' subcommand");
				return;
			}

			try {

				double the_value_precision = Double.parseDouble(args[1]);
				double the_mean_precision = Double.parseDouble (args[2]);
				boolean f_dump = Boolean.parseBoolean (args[3]);

				// Say hello

				System.out.println ("Creating OEStackedPoisson and displaying detail string");
				System.out.println ("value_precision = " + the_value_precision);
				System.out.println ("mean_precision = " + the_mean_precision);
				System.out.println ("f_dump = " + f_dump);

				// Create the object

				OEStackedPoisson stkpois = new OEStackedPoisson (
					the_value_precision,
					the_mean_precision
				);

				// Display detail string

				System.out.println ();
				System.out.println (stkpois.detail_string(f_dump));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  value_precision  mean_precision  f_full  [lambda  shift  weight]...
		// Create a OEStackedPoisson object with the specified precisions.
		// Display the summary string.
		// Create an Accumulator object and add the supplied distributions.
		// Display the detail string.
		// Cumulate the accumulator.
		// Display the detail string.
		// Display various outputs and fractiles.
		// Note: Use lambda = -1 to call the point mass functions (using shift as the value).
		// Note: Use shift = -1 to call the unshifted Poisson functions.
		// Note: Use weight = -1 to call the weight-less functions.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 3 + 3*n additional arguments

			if (!( args.length >= 4 && (args.length - 4) % 3 == 0 )) {
				System.err.println ("OEStackedPoisson : Invalid 'test3' subcommand");
				return;
			}

			try {

				double the_value_precision = Double.parseDouble (args[1]);
				double the_mean_precision = Double.parseDouble (args[2]);
				boolean f_full = Boolean.parseBoolean (args[3]);

				int dist_start = 4;			// index where the first distribution begins
				int num_dist = (args.length - dist_start) / 3;
				double[] lambda = new double[num_dist];
				int[] shift = new int[num_dist];
				double[] weight = new double[num_dist];
				for (int j = 0; j < num_dist; ++j) {
					lambda[j] = Double.parseDouble (args[dist_start + (3 * j) + 0]);
					shift[j] = Integer.parseInt (args[dist_start + (3 * j) + 1]);
					weight[j] = Double.parseDouble (args[dist_start + (3 * j) + 2]);
				}

				// Say hello

				System.out.println ("Creating OEStackedPoisson.Accumulator and adding some distributions");
				System.out.println ("value_precision = " + the_value_precision);
				System.out.println ("mean_precision = " + the_mean_precision);
				System.out.println ("f_full = " + f_full);
				System.out.println ("num_dist = " + num_dist);
				System.out.println ("num  lambda  shift  weight");
				for (int j = 0; j < num_dist; ++j) {
					System.out.println ("  " + j + "  " + lambda[j] + "  " + shift[j] + "  " + weight[j]);
				}

				// Create the object

				OEStackedPoisson stkpois = new OEStackedPoisson (
					the_value_precision,
					the_mean_precision
				);

				// Display summary string

				System.out.println ();
				System.out.println (stkpois.summary_string());

				// Create an accumulator

				OEStackedPoisson.Accumulator accum = stkpois.make_accumulator();

				// Add the distributions

				for (int j = 0; j < num_dist; ++j) {

					// Point mass case

					if (lambda[j] < -0.5) {
						if (weight[j] < -0.5) {
							accum.add_point_mass (shift[j]);
						} else {
							accum.add_point_mass (shift[j], weight[j]);
						}
					}

					// Unshifted Poisson case

					else if (shift[j] < 0) {
						if (weight[j] < -0.5) {
							accum.add_poisson (lambda[j]);
						} else {
							accum.add_poisson (lambda[j], weight[j]);
						}
					}

					// Shifted Poisson case

					else {
						if (weight[j] < -0.5) {
							accum.add_shifted_poisson (lambda[j], shift[j]);
						} else {
							accum.add_shifted_poisson (lambda[j], shift[j], weight[j]);
						}
					}
				}

				// Display accumulator detail string

				System.out.println ();
				System.out.println (accum.detail_acc_string(f_full));

				// Cumulate the distribution

				accum.cumulate();

				// Display accumulator detail string

				System.out.println ();
				System.out.println (accum.detail_acc_string(f_full));

				// Display probability of occurrence

				System.out.println ();
				System.out.println ("Probability of occurrence = " + accum.get_prob_occur());

				// Get some fractiles

				System.out.println ();
				System.out.println ("Fractile  2.5% = " + accum.get_fractile (0.025));
				System.out.println ("Fractile   25% = " + accum.get_fractile (0.25));
				System.out.println ("Fractile   50% = " + accum.get_fractile (0.5));
				System.out.println ("Fractile   75% = " + accum.get_fractile (0.75));
				System.out.println ("Fractile 97.5% = " + accum.get_fractile (0.975));

				// Get cumulative probability and probability of exceedence

				System.out.println ();
				for (int j = 0; j < num_dist; ++j) {
					int v;
					if (lambda[j] < -0.5) {
						v = shift[j];
					} else if (shift[j] < 0) {
						v = (int)Math.round(lambda[j]);
					} else {
						v = shift[j] + (int)Math.round(lambda[j]);
					}
					System.out.println (v + ":  cum_prob = " + accum.get_cum_prob(v) + ",  probex = " + accum.get_probex(v));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  value_precision  mean_precision  f_full  [lambda  shift  weight]...
		// Create a OEStackedPoisson object with the specified precisions.
		// Display the summary string.
		// Create an Accumulator object and add the supplied distributions.
		// Display the detail string.
		// Cumulate the accumulator.
		// Display the detail string.
		// Display various outputs and fractiles.
		// Note: Use lambda = -1 to call the point mass functions (using shift as the value).
		// Note: Use shift = -1 to call the unshifted Poisson functions.
		// Note: Use weight = -1 to call the weight-less functions.
		// Same as test #3, except that a separate accumulator is used for building distributions.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 3 + 3*n additional arguments

			if (!( args.length >= 4 && (args.length - 4) % 3 == 0 )) {
				System.err.println ("OEStackedPoisson : Invalid 'test4' subcommand");
				return;
			}

			try {

				double the_value_precision = Double.parseDouble (args[1]);
				double the_mean_precision = Double.parseDouble (args[2]);
				boolean f_full = Boolean.parseBoolean (args[3]);

				int dist_start = 4;			// index where the first distribution begins
				int num_dist = (args.length - dist_start) / 3;
				double[] lambda = new double[num_dist];
				int[] shift = new int[num_dist];
				double[] weight = new double[num_dist];
				for (int j = 0; j < num_dist; ++j) {
					lambda[j] = Double.parseDouble (args[dist_start + (3 * j) + 0]);
					shift[j] = Integer.parseInt (args[dist_start + (3 * j) + 1]);
					weight[j] = Double.parseDouble (args[dist_start + (3 * j) + 2]);
				}

				// Say hello

				System.out.println ("Creating OEStackedPoisson.Accumulator and adding some distributions from separate accumulators");
				System.out.println ("value_precision = " + the_value_precision);
				System.out.println ("mean_precision = " + the_mean_precision);
				System.out.println ("f_full = " + f_full);
				System.out.println ("num_dist = " + num_dist);
				System.out.println ("num  lambda  shift  weight");
				for (int j = 0; j < num_dist; ++j) {
					System.out.println ("  " + j + "  " + lambda[j] + "  " + shift[j] + "  " + weight[j]);
				}

				// Create the object

				OEStackedPoisson stkpois = new OEStackedPoisson (
					the_value_precision,
					the_mean_precision
				);

				// Display summary string

				System.out.println ();
				System.out.println (stkpois.summary_string());

				// Create an accumulator

				OEStackedPoisson.Accumulator accum = stkpois.make_accumulator();

				// Create an accumulator for partial sums

				OEStackedPoisson.Accumulator partial = stkpois.make_accumulator();

				// Add the distributions

				for (int j = 0; j < num_dist; ++j) {

					partial.clear();

					// Point mass case

					if (lambda[j] < -0.5) {
						if (weight[j] < -0.5) {
							partial.add_point_mass (shift[j]);
						} else {
							partial.add_point_mass (shift[j], weight[j]);
						}
					}

					// Unshifted Poisson case

					else if (shift[j] < 0) {
						if (weight[j] < -0.5) {
							partial.add_poisson (lambda[j]);
						} else {
							partial.add_poisson (lambda[j], weight[j]);
						}
					}

					// Shifted Poisson case

					else {
						if (weight[j] < -0.5) {
							partial.add_shifted_poisson (lambda[j], shift[j]);
						} else {
							partial.add_shifted_poisson (lambda[j], shift[j], weight[j]);
						}
					}

					// Combine into total accumulator

					accum.combine_with (partial);
				}

				// Display accumulator detail string

				System.out.println ();
				System.out.println (accum.detail_acc_string(f_full));

				// Cumulate the distribution

				accum.cumulate();

				// Display accumulator detail string

				System.out.println ();
				System.out.println (accum.detail_acc_string(f_full));

				// Display probability of occurrence

				System.out.println ();
				System.out.println ("Probability of occurrence = " + accum.get_prob_occur());

				// Get some fractiles

				System.out.println ();
				System.out.println ("Fractile  2.5% = " + accum.get_fractile (0.025));
				System.out.println ("Fractile   25% = " + accum.get_fractile (0.25));
				System.out.println ("Fractile   50% = " + accum.get_fractile (0.5));
				System.out.println ("Fractile   75% = " + accum.get_fractile (0.75));
				System.out.println ("Fractile 97.5% = " + accum.get_fractile (0.975));

				// Get cumulative probability and probability of exceedence

				System.out.println ();
				for (int j = 0; j < num_dist; ++j) {
					int v;
					if (lambda[j] < -0.5) {
						v = shift[j];
					} else if (shift[j] < 0) {
						v = (int)Math.round(lambda[j]);
					} else {
						v = shift[j] + (int)Math.round(lambda[j]);
					}
					System.out.println (v + ":  cum_prob = " + accum.get_cum_prob(v) + ",  probex = " + accum.get_probex(v));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  value_precision  mean_precision  f_full  [lambda  shift  weight]...
		// Create a OEStackedPoisson object with the specified precisions.
		// Display the summary string.
		// Create an Accumulator object and add the supplied distributions.
		// Display the detail string.
		// Cumulate the accumulator.
		// Display the detail string.
		// Display various outputs and fractiles.
		// Note: Use lambda = -1 to call the point mass functions (using shift as the value).
		// Note: Use shift = -1 to call the unshifted Poisson functions.
		// Note: Use weight = -1 to call the weight-less functions.
		// Similar to test #4, except using arrays of accumulators.  The test uses a 3-row by
		// 5-column array.  Each lambda or shift is converted to an array by placing the given
		// value in the upper left corner, filling the first row with multiples of the given
		// value, and then filling succeeding rows by multiplying the prior row by 10.
		// Only the accumulator in the upper left corner is displayed in detail.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 3 + 3*n additional arguments

			if (!( args.length >= 4 && (args.length - 4) % 3 == 0 )) {
				System.err.println ("OEStackedPoisson : Invalid 'test5' subcommand");
				return;
			}

			try {

				double the_value_precision = Double.parseDouble (args[1]);
				double the_mean_precision = Double.parseDouble (args[2]);
				boolean f_full = Boolean.parseBoolean (args[3]);

				int dist_start = 4;			// index where the first distribution begins
				int num_dist = (args.length - dist_start) / 3;
				double[] lambda = new double[num_dist];
				int[] shift = new int[num_dist];
				double[] weight = new double[num_dist];
				for (int j = 0; j < num_dist; ++j) {
					lambda[j] = Double.parseDouble (args[dist_start + (3 * j) + 0]);
					shift[j] = Integer.parseInt (args[dist_start + (3 * j) + 1]);
					weight[j] = Double.parseDouble (args[dist_start + (3 * j) + 2]);
				}

				// Say hello

				System.out.println ("Creating OEStackedPoisson.Accumulator arrays and adding some distributions");
				System.out.println ("value_precision = " + the_value_precision);
				System.out.println ("mean_precision = " + the_mean_precision);
				System.out.println ("f_full = " + f_full);
				System.out.println ("num_dist = " + num_dist);
				System.out.println ("num  lambda  shift  weight");
				for (int j = 0; j < num_dist; ++j) {
					System.out.println ("  " + j + "  " + lambda[j] + "  " + shift[j] + "  " + weight[j]);
				}

				// Create the object

				OEStackedPoisson stkpois = new OEStackedPoisson (
					the_value_precision,
					the_mean_precision
				);

				// Display summary string

				System.out.println ();
				System.out.println (stkpois.summary_string());

				// Create an accumulator array

				final int rows = 3;
				final int cols = 5;

				OEStackedPoisson.Accumulator accum[][] = new OEStackedPoisson.Accumulator[rows][cols];
				stkpois.make_acc_array (accum);

				// Create an accumulator for partial sums

				OEStackedPoisson.Accumulator partial[][] = new OEStackedPoisson.Accumulator[rows][cols];
				stkpois.make_acc_array (partial);

				// Add the distributions

				for (int j = 0; j < num_dist; ++j) {

					OEStackedPoisson.clear_acc_array (partial);

					// Make lambda and shift arrays (which might not both be used)

					double[][] lambda_array = test_make_parm_double_array (rows, cols, lambda[j], 1.0, lambda[j], 10.0, 0.0);
					int[][] shift_array = test_make_parm_int_array (rows, cols, shift[j], 1, shift[j], 10, 0);

					// Point mass case

					if (lambda[j] < -0.5) {
						if (weight[j] < -0.5) {
							OEStackedPoisson.add_point_mass_acc_array (partial, shift_array);
						} else {
							OEStackedPoisson.add_point_mass_acc_array (partial, shift_array, weight[j]);
						}
					}

					// Unshifted Poisson case

					else if (shift[j] < 0) {
						if (weight[j] < -0.5) {
							OEStackedPoisson.add_poisson_acc_array (partial, lambda_array);
						} else {
							OEStackedPoisson.add_poisson_acc_array (partial, lambda_array, weight[j]);
						}
					}

					// Shifted Poisson case

					else {
						if (weight[j] < -0.5) {
							OEStackedPoisson.add_shifted_poisson_acc_array (partial, lambda_array, shift_array);
						} else {
							OEStackedPoisson.add_shifted_poisson_acc_array (partial, lambda_array, shift_array, weight[j]);
						}
					}

					// Combine into total accumulator

					OEStackedPoisson.combine_acc_array (accum, partial);
				}

				// Display accumulator detail string

				System.out.println ();
				System.out.println (accum[0][0].detail_acc_string(f_full));

				// Cumulate the distribution

				OEStackedPoisson.cumulate_acc_array (accum);

				// Display accumulator detail string

				System.out.println ();
				System.out.println (accum[0][0].detail_acc_string(f_full));

				// Display probability of occurrence

				System.out.println ();
				System.out.println ("Probability of occurrence:\n" + test_display_array (OEStackedPoisson.prob_occur_acc_array (accum)));

				// Get some fractiles

				System.out.println ();
				System.out.println ("Fractile  2.5%:\n" + test_display_array (OEStackedPoisson.fractile_acc_array (accum, 0.025)));
				System.out.println ("Fractile   25%:\n" + test_display_array (OEStackedPoisson.fractile_acc_array (accum, 0.25 )));
				System.out.println ("Fractile   50%:\n" + test_display_array (OEStackedPoisson.fractile_acc_array (accum, 0.5  )));
				System.out.println ("Fractile   75%:\n" + test_display_array (OEStackedPoisson.fractile_acc_array (accum, 0.75 )));
				System.out.println ("Fractile 97.5%:\n" + test_display_array (OEStackedPoisson.fractile_acc_array (accum, 0.975)));

				// Get cumulative probability and probability of exceedence

				System.out.println ();
				for (int j = 0; j < num_dist; ++j) {
					int v;
					if (lambda[j] < -0.5) {
						v = shift[j];
					} else if (shift[j] < 0) {
						v = (int)Math.round(lambda[j]);
					} else {
						v = shift[j] + (int)Math.round(lambda[j]);
					}
					v *= 20;		// value for second row, second column
					System.out.println ("Cumulative probability for " + v + ":\n" + test_display_array (OEStackedPoisson.cum_prob_acc_array (accum, v)));
					System.out.println ("Probability of exceedence for " + v + ":\n" + test_display_array (OEStackedPoisson.probex_acc_array (accum, v)));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEStackedPoisson : Unrecognized subcommand : " + args[0]);
		return;

	}

}
