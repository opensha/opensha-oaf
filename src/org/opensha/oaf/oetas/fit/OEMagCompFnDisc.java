package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Arrays;

import static org.opensha.oaf.oetas.OEConstants.C_LOG_10;				// natural log of 10
import static org.opensha.oaf.oetas.OEConstants.C_MILLIS_PER_DAY;		// milliseconds per day
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;			// very large time value
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;	// use x >= HUGE_TIME_DAYS_CHECK to check for HUGE_TIME_DAYS
import static org.opensha.oaf.oetas.OEConstants.LOG10_HUGE_TIME_DAYS;	// log10 of very large time value


/**
 * Time-dependent magnitude of completeness function -- Discrete function.
 * Author: Michael Barall 07/05/2020.
 *
 * This class represents a time-dependent magnitude of completeness function
 * which is discrete.  That is, it is piecewise-constant.
 *
 * This is a concrete class which can be instantiated by specifying the sequence
 * of intervals and the function value within each interval.  It can also be
 * subclassed to create functions where the intervals and values are determined
 * in some other way, e.g., by Helmstetter parameters.
 *
 * ==================================
 *
 * The function is piecewise constant on N intervals.  It is defined by a sequence
 * of N-1 times
 *
 *   t0 < t1 < t2 < ... < t(N-2)
 *
 * and N magnitudes
 *
 *   M0, M1, M2, ... , M(N-1)
 *
 * The magnitude of completeness is defined to be
 *
 *   Mc(t) = M0   if   t <= t0
 *
 *   Mc(t) = M(i)   if   t(i-1) < t <= t(i)   for   i = 1, ... , N-2
 *
 *   Mc(t) = M(N-1)   if   t(N-2) < t
 *
 * Notice that each interval is defined to be open on the left and closed on the right.
 */
public class OEMagCompFnDisc extends OEMagCompFn {

	//----- Parameters -----


	// Catalog magnitude of completeness.

	protected double magCat;




	//----- Data structures -----
	//
	// The time range is partitioned into N intervals.  Within each interval,
	// the magnitude of completeness is a constant.
	// The function definition is held in two arrays.
	//
	// For the n-th interval, 0 <= n < N, the data is defined as follows.
	//
	// * For n >= 1, the start time of the interval is a_time[n-1].
	//   Note that the first interval implicitly begins at -infinity, and the last
	//   interval implicitly extends to +infinity.  Also, note that intervals are
	//   open at the left and closed at the right, so the n-th interval is defined
	//   by a_time[n-1] < t <= a_time[n].
	//
	// * The function is constant in the interval, and a_mag[n].

	// Array of length N-1, containing the start time of each interval (after the first).

	protected double[] a_time;

	// Array of length N, containing the value of a constant function.

	protected double[] a_mag;


	// Get the number of intervals.
	// This is one more than the number of times.

	public final int get_interval_count () {
		if (a_mag == null) {
			return 0;
		}
		return a_mag.length;
	}


	// Create a string describing the n-th interval.

	public String get_interval_string (int n) {
		StringBuilder result = new StringBuilder();

		// Interval start time, but not on the first interval which starts at -infinity

		if (n > 0) {
			result.append("time = ").append(a_time[n-1]).append(": ");
		}

		// Constant function

		result.append("constant: mag = ").append(a_mag[n]);
	
		return result.toString();
	}


	// Get the n-th time, for 0 <= n < interval_count-1.

	public final double get_time (int n) {
		return a_time[n];
	}


	// Get the n-th magnitude, for 0 <= n < interval_count.
	// Note: Intervals of finite duration have 1 <= n < interval_count-1.
	// For interval n, the start time is get_time(n-1) and the end time is get_time(n).

	public final double get_mag (int n) {
		return a_mag[n];
	}


	// Get the index of the interval that contains the given time.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the value of n such that get_time(n-1) < t_days <= get_time(n),
	// which means that get_mag(n) is the magnitude of completeness at time t_days.
	// The returned n satisfies 0 <= n < interval_count.
	// (For purposes of this definition, pretend that get_time(-1) == -infinity
	// and get_time(interval_count-1) == +infinity.)

	public final int get_interval_index (double t_days) {

		// Binary search to find the interval

		int lo = -1;				// implicitly, a_time[-1] = -infinity
		int hi = a_time.length;		// implicitly, a_time[length] = +infinity

		// Search, maintaining the condition a_time[lo] < t_days <= a_time[hi]

		while (hi - lo > 1) {
			int mid = (hi + lo)/2;
			if (a_time[mid] < t_days) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		// Return the interval index

		return hi;
	}


	// Get the index of the time that is closest to the given time.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the value of n that minimizes abs(t_days - get_time(n)).
	// The returned n satisfies 0 <= n < interval_count-1.
	// Note: For times t1 < t2, the range of intervals that most closely
	// fits the range of times from t1 to t2 is intervals of index n where
	// get_closest_time_index(t1) < n <= get_closest_time_index(t2).

	public final int get_closest_time_index (double t_days) {
		int n = get_interval_index (t_days);
		if (n == a_time.length || (n != 0 && (t_days - a_time[n-1] < a_time[n] - t_days))) {
			--n;
		}
		return n;
	}




	//----- Evaluation -----


	// Calculate the time-dependent magnitude of completeness.
	// Parameters:
	//  t_days = Time since the origin, in days.
	// Returns the magnitude of completeness at the given time.
	// Note: This function does not define the origin of time.
	// Note: The returned value must be >= the value returned by get_mag_cat().
	// It is expected that as t_days becomes large, the returned value
	// equals or approaches the value returned by get_mag_cat()

	@Override
	public double get_mag_completeness (double t_days) {

		// Binary search to find the interval

		int lo = -1;				// implicitly, a_time[-1] = -infinity
		int hi = a_time.length;		// implicitly, a_time[length] = +infinity

		// Search, maintaining the condition a_time[lo] < t_days <= a_time[hi]

		while (hi - lo > 1) {
			int mid = (hi + lo)/2;
			if (a_time[mid] < t_days) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		// Return the constant function value

		return a_mag[hi];
	}




	// Get the catalog magnitude of completeness.
	// Returns the magnitude of completeness in normal times.

	@Override
	public double get_mag_cat () {
		return magCat;
	}




	//----- Building, Memory Management -----




	// Marshal type codes for splitting functions.

	protected static final int MARSHAL_SPLITFN_NULL = 90100;
	protected static final int MARSHAL_SPLITFN_CONSTANT = 90101;
	protected static final int MARSHAL_SPLITFN_RATIO = 90102;

	//protected static final String M_TYPE_NAME = "ClassType";	// inherited




	// Nested abstract class to define a splitting function.
	// Supplying an object of this class enables splitting.
	// Extending this class permits specification of maximum interval duration.

	public static abstract class SplitFn {

		// Get the duration limit.
		// Parameters:
		//  tsplit = Most recent split time, in days, can be -HUGE_TIME_DAYS if none.
		//  tstart = Start time of interval, in days.
		// Returns the maximum allowed duration of the interval.
		// Can return HUGE_TIME_DAYS if there is no upper limit.

		public abstract double get_durlim (double tsplit, double tstart);

		// Return true if this function equals the other function.

		public abstract boolean equals_fn (SplitFn other);

		// Get the type code.

		public abstract int get_marshal_splitfn_type ();

		// Marshal object.

		public abstract void do_marshal_splitfn (MarshalWriter writer);
	}




	// Splitting function with constant duration limit.
	// A large constant value can be used to obtain no upper limit.

	public static class SplitFnConstant extends SplitFn {

		// The duration limit, must be >= 0, can be HUGE_TIME_DAYS for no limit.

		private double durlim;

		// Get the duration limit.
		// Parameters:
		//  tsplit = Most recent split time, in days, can be -HUGE_TIME_DAYS if none.
		//  tstart = Start time of interval, in days.
		// Returns the maximum allowed duration of the interval.
		// Can return HUGE_TIME_DAYS if there is no upper limit.

		@Override
		public double get_durlim (double tsplit, double tstart) {
			return durlim;
		}

		// Construct a function with a very large constant value.

		public SplitFnConstant () {
			durlim = HUGE_TIME_DAYS;
		}

		// Construct a function with the given constant value.

		public SplitFnConstant (double durlim) {
			if (!( durlim > 0.0 )) {
				throw new IllegalArgumentException ("OEMagCompFnDisc.SplitFnConstant - Invalid constructor argument: durlim = " + durlim);
			}
			this.durlim = durlim;
		}

		// Display a one-line summary.

		@Override
		public String toString() {
			return "SplitFnConstant: durlim = " + durlim;
		}

		// Return true if this function equals the other function.

		@Override
		public boolean equals_fn (SplitFn other) {
			if (other != null) {
				if (other instanceof SplitFnConstant) {
					SplitFnConstant o = (SplitFnConstant)other;
					if (this.durlim == o.durlim) {
						return true;
					}
				}
			}
			return false;
		}

		// Get the type code.

		@Override
		public int get_marshal_splitfn_type () {
			return MARSHAL_SPLITFN_CONSTANT;
		}

		// Marshal object.

		@Override
		public void do_marshal_splitfn (MarshalWriter writer) {
			writer.marshalDouble ("durlim", durlim);
			return;
		}

		// Unmarshal object.

		public SplitFnConstant (MarshalReader reader) {
			durlim = reader.unmarshalDouble ("durlim");
		}
	}




	// Splitting function where duration limit is a ratio of the time since
	// the most recent split time.

	public static class SplitFnRatio extends SplitFn {

		// Duration limit ratio, minimum, and maximum.
		// These are used to calculate the maximum allowed duration of an interval according to
		// min(durlim_max, max (durlim_min, durlim_ratio*(t - tsplit)))
		// where t is the start of the interval and tsplit is the most recent split time.

		// Duration limit ratio, must be >= 0.

		private double durlim_ratio;

		// Duration limit minimum, must be > 0.

		private double durlim_min;

		// Duration limit maximum, must be >= durlim_min.

		private double durlim_max;

		// Get the duration limit.
		// Parameters:
		//  tsplit = Most recent split time, in days, can be -HUGE_TIME_DAYS if none.
		//  tstart = Start time of interval, in days.
		// Returns the maximum allowed duration of the interval.
		// Can return HUGE_TIME_DAYS if there is no upper limit.

		@Override
		public double get_durlim (double tsplit, double tstart) {
			if (tsplit <= -HUGE_TIME_DAYS_CHECK) {
				return durlim_max;
			}
			return Math.min(durlim_max, Math.max (durlim_min, durlim_ratio*(tstart - tsplit)));
		}

		// Construct a function with the given constant parameters.

		public SplitFnRatio (double durlim_ratio, double durlim_min, double durlim_max) {
			if (!( durlim_ratio >= 0.0
				&& durlim_min > 0.0 
				&& durlim_max >= durlim_min )) {
				throw new IllegalArgumentException ("OEMagCompFnDisc.SplitFnRatio - Invalid constructor argument: durlim_ratio = " + durlim_ratio + ", durlim_min = " + durlim_min + ", durlim_max = " + durlim_max);
			}
			this.durlim_ratio = durlim_ratio;
			this.durlim_min = durlim_min;
			this.durlim_max = durlim_max;
		}

		// Display a one-line summary.

		@Override
		public String toString() {
			return "SplitFnRatio: durlim_ratio = " + durlim_ratio + ", durlim_min = " + durlim_min + ", durlim_max = " + durlim_max;
		}

		// Return true if this function equals the other function.

		@Override
		public boolean equals_fn (SplitFn other) {
			if (other != null) {
				if (other instanceof SplitFnRatio) {
					SplitFnRatio o = (SplitFnRatio)other;
					if (this.durlim_ratio == o.durlim_ratio
						&& this.durlim_min == o.durlim_min
						&& this.durlim_max == o.durlim_max) {
						return true;
					}
				}
			}
			return false;
		}

		// Get the type code.

		@Override
		public int get_marshal_splitfn_type () {
			return MARSHAL_SPLITFN_RATIO;
		}

		// Marshal object.

		@Override
		public void do_marshal_splitfn (MarshalWriter writer) {
			writer.marshalDouble ("durlim_ratio", durlim_ratio);
			writer.marshalDouble ("durlim_min", durlim_min);
			writer.marshalDouble ("durlim_max", durlim_max);
			return;
		}

		// Unmarshal object.

		public SplitFnRatio (MarshalReader reader) {
			durlim_ratio = reader.unmarshalDouble ("durlim_ratio");
			durlim_min = reader.unmarshalDouble ("durlim_min");
			durlim_max = reader.unmarshalDouble ("durlim_max");
		}
	}




	// Return true if the two split-function objects are equal.

	public static boolean equals_splitfn (SplitFn f1, SplitFn f2) {
		if (f1 == null) {
			return (f2 == null);
		}
		return f1.equals_fn (f2);
	}




	// Marshal split-function object, polymorphic.

	public static void marshal_splitfn_poly (MarshalWriter writer, String name, SplitFn obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_SPLITFN_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_splitfn_type());
			obj.do_marshal_splitfn (writer);
		}

		writer.marshalMapEnd ();

		return;
	}




	// Unmarshal split-function object, polymorphic.

	public static SplitFn unmarshal_splitfn_poly (MarshalReader reader, String name) {
		SplitFn result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFnDisc.SplitFn.unmarshal_splitfn_poly: Unknown class type code: type = " + type);

		case MARSHAL_SPLITFN_NULL:
			result = null;
			break;

		case MARSHAL_SPLITFN_CONSTANT:
			result = new SplitFnConstant (reader);
			break;

		case MARSHAL_SPLITFN_RATIO:
			result = new SplitFnRatio (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	// Inner class to manage memory.
	// The user must begin by calling setup_arrays.
	// Then, the user calls the various add_interval and add_split methods.
	// Then, the user must call finish_up_arrays to put the arrays into final form.

	protected class MemoryBuilder {


		//--- Basic parameters and variables for interval construction

		// The current number of times, separating intervals.

		protected int time_count;

		// True if the last interval added was a magCat interval.

		protected boolean last_was_magCat;

		// The difference between magnitudes which are indistinguishable.
		// If magnitudes for successive intervals differ by this amount or less,
		// then the intervals are combined.  A magnitude within this amount of
		// magCat is forced exactly equal to magCat.  A value of 0.0 combines
		// intervals only if magnitudes are exactly equal, while a negative
		// value prevents any combinations.

		protected double mag_eps;

		// The difference between times which are indistinguishable.
		// If times for successive intervals differ by this amount or less,
		// then the prior interval is discarded and the new interval begins
		// at the earlier time.  Must be >= 0.

		protected double time_eps;

		// The last time supplied by the user, or -HUGE_TIME_DAYS if none.

		protected double last_user_time;


		//--- Parameters and variables for interval splitting

		// True to enable splitting.

		protected boolean f_enable_split;

		// Array of split times.
		// If an interval spans a split time, it is split so the final intervals do not span a split time.
		// Split times must be in increasing order.

		protected double[] a_split_time;

		// Array of splitting functions for the split times.
		// Null means to continue using the prior splitting function.

		protected SplitFn[] a_split_fn;

		// The current number of split times.

		protected int split_count;

		// The index of the next split time waiting to be used.
		// If split_index == split_count then there is no split time waiting to be used.
		// If split_index > 0 then a_split_time[split_index - 1] is the last split time used.

		protected int split_index;


		//--- Variables for holding the most recent pending interval

		// True if we are currently holding an interval.

		protected boolean pend_full;

		// The time of the pending interval.

		protected double pend_time;

		// The magnitude of the pending interval.

		protected double pend_mag;

		// True if the pending interval is a split time.

		protected boolean pend_is_split;

		// The splitting function for this split time, or null to continue using the prior function (only used if pend_is_split is true).

		protected SplitFn pend_split_fn;

		// The currently active splitting function, or null if none.

		protected SplitFn active_split_fn;

		// The base time for the currently active splitting function, or -HUGE_TIME_DAYS for unspecified.

		protected double active_split_time;




		//--- Setup functions




		// Set up the arrays.

		public void setup_arrays (double mag_eps, double time_eps, boolean f_enable_split) {

			// Check arguments

			if (!( time_eps >= 0.0 )) {
				throw new IllegalArgumentException ("OEMagCompFnDisc.MemoryBuilder.setup_arrays - Invalid time epsilon: time_eps = " + time_eps);
			}

			// Save magnitude epsilon

			this.mag_eps = mag_eps;

			// Save time epsilon

			this.time_eps = time_eps;

			// Save flat to enable splitting

			this.f_enable_split = f_enable_split;

			// Default capacity, in number of times

			int default_capacity = 16;

			// Allocate the initial arrays

			a_time = new double[default_capacity];
			a_mag = new double[default_capacity + 1];

			// No times yet

			time_count = 0;
			last_user_time = -HUGE_TIME_DAYS;

			// Insert an initial magCat interval

			last_was_magCat = true;
			a_mag[0] = magCat;

			// Allocate array of split times

			a_split_time = new double[default_capacity];
			a_split_fn = new SplitFn[default_capacity];

			// No split times yet

			split_count = 0;
			split_index = 0;

			// No pending interval

			pend_full = false;
			pend_time = 0.0;
			pend_mag = 0.0;
			pend_is_split = false;
			pend_split_fn = null;

			// No active splitting function

			active_split_fn = null;
			active_split_time = -HUGE_TIME_DAYS;

			return;
		}




		// Set the magnitude for the first interval.
		// Note: The first interval by default is magCat, so this only
		// needs to be called if the value is other than magCat.

		public void first_interval (double mag) {
		
			// Error if not at the first interval

			if (time_count > 0) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.first_interval - Already after first interval");
			}

			// Force magnitude to magCat if within epsilon

			double eff_mag = mag;
			if (Math.abs (eff_mag - magCat) <= mag_eps) {
				eff_mag = magCat;
			}

			// Set value for first interval

			a_mag[0] = eff_mag;
			return;
		}




		// Set the splitting function for the first interval.
		// This will be used if the first interval is not a split with its own splitting function.
		// This version of the function sets the base time to the distant past.

		public void first_split_fn (SplitFn split_fn) {
		
			// Error if not at the first interval

			if (time_count > 0) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.first_split_fn - Already after first interval");
			}

			// Set splitting function for first interval

			if (f_enable_split) {
				active_split_fn = split_fn;
				active_split_time = -HUGE_TIME_DAYS;
			}
			return;
		}




		// Set the splitting function for the first interval.
		// This will be used if the first interval is not a split with its own splitting function.
		// This version of the function sets the base time to the supplied split time.

		public void first_split_fn (SplitFn split_fn, double tsplit) {
		
			// Error if not at the first interval

			if (time_count > 0) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.first_split_fn - Already after first interval");
			}

			// Set splitting function for first interval

			if (f_enable_split) {
				active_split_fn = split_fn;
				active_split_time = tsplit;
			}
			return;
		}




		// Get the time epsilon.

		public double get_time_eps () {
			return time_eps;
		}




		// Get the magnitude epsilon.

		public double get_mag_eps () {
			return mag_eps;
		}




		//--- Functions for accessing the list of split times




		// Return true if using split times.

		public boolean is_using_split_times () {
			return f_enable_split;
		}




		// Add a split time, expanding the array if needed.
		// Split times must be added in non-decreasing order.
		// Duplicate split times are discarded.
		// Split times can be added during construction, but to be effective a split time
		// must be added before creating an interval whose start time is greater than the split time.
		// If split times are not being used, it is OK to call this function but
		// the split times have no effect.

		public void add_split (double time, SplitFn split_fn) {

			// If we are using split times ...

			if (f_enable_split) {

				// Check for increasing time, and for duplicate time

				if (split_count > 0) {

					double delta_t = time - a_split_time[split_count - 1];

					if (delta_t < 0.0) {
						throw new IllegalArgumentException ("OEMagCompFnDisc.MemoryBuilder.add_split - Split time out-of-order: time = " + time + ", last split time = " + a_split_time[split_count - 1]);
					}

					// If duplicate time, merge the splitting functions

					if (delta_t <= 0.0) {
						if (split_fn != null) {
							a_split_fn[split_count - 1] = split_fn;
						}
						return;
					}
				}
		
				// Expand array if needed

				if (split_count >= a_split_time.length) {

					// Calculate new capacity required

					int default_capacity = 16;
					int new_capacity = Math.max (default_capacity, a_split_time.length) * 2;

					// Reallocate array

					a_split_time = Arrays.copyOf (a_split_time, new_capacity);
					a_split_fn = Arrays.copyOf (a_split_fn, new_capacity);
				}

				// Insert the new interval

				a_split_time[split_count] = time;
				a_split_fn[split_count] = split_fn;

				// Count it

				++split_count;
			}

			return;
		}




		// Return true if there is an unconsumed split time available.
		// It is guaranteed this returns false if not using split times.

		private boolean has_split () {
			return split_index < split_count;
		}




		// Get the first unconsumed split time.

		private double get_split_time () {
			if (split_index >= split_count) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.get_split_time - No split time available");
			}
			return a_split_time[split_index];
		}




		// Get the splitting function for the first unconsumed split time, or null to continue using the prior function.

		private SplitFn get_split_fn () {
			if (split_index >= split_count) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.get_split_fn - No split time available");
			}
			return a_split_fn[split_index];
		}




		// Consume the first unconsumed split time.

		private void consume_split () {
			if (split_index >= split_count) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.consume_split - No split time available");
			}
			++split_index;
			return;
		}




		//--- Functions for adding intervals to the arrays




		// Add an interval, expanding the arrays if needed.

		private void append_interval (double time, double mag) {

			// Check for increasing time, and for duplicate time

			if (time_count > 0) {

				double delta_t = time - a_time[time_count - 1];

				if (delta_t < 0.0) {
					throw new IllegalArgumentException ("OEMagCompFnDisc.MemoryBuilder.append_interval - Time out-of-order: time = " + time + ", last time = " + a_time[time_count - 1]);
				}

				// If duplicate time, just change the magnitude in the existing interval

				if (delta_t <= 0.0) {
					a_mag[time_count] = mag;
					return;
				}
			}
		
			// Expand arrays if needed

			if (time_count >= a_time.length) {

				// Calculate new capacity required

				int default_capacity = 16;
				int new_capacity = Math.max (default_capacity, a_time.length) * 2;

				// Reallocate arrays

				a_time = Arrays.copyOf (a_time, new_capacity);
				a_mag = Arrays.copyOf (a_mag, new_capacity + 1);
			}

			// Insert the new interval

			a_time[time_count] = time;
			a_mag[time_count + 1] = mag;

			// Count it

			++time_count;
			return;
		}




		// Split the current interval, by appending a new interval with the same magnitude.

		private void split_interval (double time) {
			append_interval (time, a_mag[time_count]);
			return;
		}




		// Get the magnitude of the last interval.

		private double get_last_interval_mag () {
			return a_mag[time_count];
		}




		//--- Function for splitting intervals by length




		// Split the current interval if needed to comply with duration limit.
		// Note: All newly created intervals have start time < time.

		private void split_for_durlim (double time) {

			// If we are using duration limits, and there is a start time available ...

			if (active_split_fn != null && time_count > 0) {

				// Loop until all splits are done ...

				for (;;) {

					// Start time of current interval

					double start_time = a_time[time_count - 1];

					// Duration we would like to create

					double delta_t = time - start_time;

					// (At this point we might throw an exception if delta_t < 0.0)

					// Duration limit, based on elapsed time since the most recently-used split time

					double durlim = Math.max(time_eps, active_split_fn.get_durlim (active_split_time, start_time));
				
					// If limit exceeds delta, we're done

					if (durlim >= delta_t) {
						break;
					}

					// Otherwise, if limit exceeds half of delta, split the interval at its midpoint and stop

					else if (durlim >= 0.5 * delta_t) {
						split_interval (0.5 * delta_t + start_time);
						break;
					}

					// Otherwise, split the interval at the duration limit and continue splitting

					else {
						split_interval (durlim + start_time);
					}
				}
			}

			return;
		}




		//--- Functions for the pending interval




		// Flush the pending interval, if we have one.

		private void pend_flush () {
		
			// If there is a pending interval ...

			if (pend_full) {

				// Split current interval as needed to satisfy duration limit, if any

				split_for_durlim (pend_time);

				// Append the new interval
			
				append_interval (pend_time, pend_mag);

				// If this contains a split time with a splitting function, it becomes the new active splitting function

				if (pend_is_split && pend_split_fn != null) {
					active_split_fn = pend_split_fn;
					active_split_time = pend_time;
				}

				// No pending interval

				pend_full = false;
			}

			return;
		}




		// Set the pending interval.
		// Parameters:
		//  time = Start time of the interval.
		//  mag = Magnitude of the interval.
		//  is_split = True if this time is a splitting time.
		//  split_fn = Associated splitting function, or null to continue using the prior function (must be null if is_split is false).
		// If there is an existing pending interval, it is flushed.

		private void pend_flush_and_set (double time, double mag, boolean is_split, SplitFn split_fn) {
			pend_flush();
			pend_time = time;
			pend_mag = mag;
			pend_is_split = is_split;
			pend_split_fn = split_fn;
			pend_full = true;
			return;
		}




		// Set the pending interval.
		// Parameters:
		//  time = Start time of the interval.
		//  mag = Magnitude of the interval.
		//  is_split = True if this time is a splitting time.
		//  split_fn = Associated splitting function, or null to continue using the prior function (must be null if is_split is false).
		// If there is an existing pending interval, it is overwritten without being flushed.

		private void pend_set_no_flush (double time, double mag, boolean is_split, SplitFn split_fn) {
			pend_time = time;
			pend_mag = mag;
			pend_is_split = is_split;
			pend_split_fn = split_fn;
			pend_full = true;
			return;
		}




		// Change the time of the pending interval.
		// Parameters:
		//  time = Start time of the interval.
		//  is_split = True if this time is a splitting time.
		//  split_fn = Associated splitting function, or null to continue using the prior function (must be null if is_split is false).
		// There must be an existing pending interval.
		// The existing magnitude is retained.
		// The result is a split time if either is_split is true or the existing interval is a split time.

		private void pend_adjust_time (double time, boolean is_split, SplitFn split_fn) {

			// Error if no pending interval

			if (!( pend_full )) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.pend_adjust_time - No pending interval available");
			}

			// Change time

			pend_time = time;
			if (is_split) {
				pend_is_split = true;
				if (split_fn != null) {
					pend_split_fn = split_fn;
				}
			}
			return;
		}




		// Split the pending interval at the given time.
		// Parameters:
		//  time = Start time of the interval.
		//  is_split = True if this time is a splitting time.
		//  split_fn = Associated splitting function, or null to continue using the prior function (must be null if is_split is false).
		// If there is a pending interval, it is flushed.
		// The magnitude is retained from the prior interval.
		// This function can be used regardless of whether or not there is a pending interval.

		private void pend_flush_and_split (double time, boolean is_split, SplitFn split_fn) {
			pend_flush();
			pend_time = time;
			pend_mag = get_last_interval_mag();
			pend_is_split = is_split;
			pend_split_fn = split_fn;
			pend_full = true;
			return;
		}




		// Change the magnitude of the pending interval.
		// Parameters:
		//  mag = Magnitude of the interval.
		// There must be an existing pending interval.
		// The time and splitting are retained from the existing pending interval.

		private void pend_adjust_mag (double mag) {

			// Error if no pending interval

			if (!( pend_full )) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.pend_adjust_mag - No pending interval available");
			}

			// Change magnitude

			pend_mag = mag;
			return;
		}




		// Adjust the split flags of the pending interval.
		// Parameters:
		//  is_split = True if this time is a splitting time.
		//  split_fn = Associated splitting function, or null to continue using the prior function (must be null if is_split is false).
		// There must be an existing pending interval.
		// The time and magnitude are retained from the existing pending interval.
		// The result is a split time if either is_split is true or the existing interval is a split time.

		private void pend_adjust_split (boolean is_split, SplitFn split_fn) {

			// Error if no pending interval

			if (!( pend_full )) {
				throw new IllegalStateException ("OEMagCompFnDisc.MemoryBuilder.pend_adjust_split - No pending interval available");
			}

			// Change aplit

			if (is_split) {
				pend_is_split = true;
				if (split_fn != null) {
					pend_split_fn = split_fn;
				}
			}
			return;
		}




		//--- Function to insert split times




		// Split the current interval if needed to obey split times.
		// Parameters:
		//  time = Start time of the interval we want to create.
		// Note: All newly created intervals have start time < time.
		// Note: After this call, the next available split time, if any, will be >= time.
		// Note: This may be called with time equal to a very large value to flush out all split times.

		private void split_for_split (double time) {

			// While there are split times waiting to be used ...

			while (has_split()) {

				// The next split time

				double tsplit = get_split_time();
				SplitFn split_fn = get_split_fn();

				// If the end of our interval, stop

				if (tsplit >= time) {
					break;
				}

				// If there is a pending interval ...

				if (pend_full) {

					// If the pending interval is a split ...

					if (pend_is_split) {

						// If later than pending interval (should always be true), write the new split time, otherwise drop the new split time

						if (tsplit > pend_time) {
							pend_flush_and_split (tsplit, true, split_fn);
						}
					}

					// Otherwise, pending interval is not a split ...

					else {

						// Time difference from pending interval to split

						double delta_t = tsplit - pend_time;

						// If before the pending time (should never happen) ...

						if (delta_t < 0.0) {
						
							// If within eplison, convert pending interval to a split, otherwise drop the new split time

							if (delta_t >= -time_eps) {
								pend_adjust_split (true, split_fn);
							}
						}

						// Otherwise, split is after pending time ...

						else {

							// If within epsilon, snap the pending interval to the split time

							if (delta_t <= time_eps) {
								pend_adjust_time (tsplit, true, split_fn);
							}

							// Otherwise, write the split time

							else {
								pend_flush_and_split (tsplit, true, split_fn);
							}
						}
					}
				}

				// Otherwise, no pending interval, so just write the split time

				else {
					pend_flush_and_split (tsplit, true, split_fn);
				}

				// Consume the split time

				consume_split();
			}

			return;
		}




		//--- Functions for user-supplied intervals




		// Add an interval, expanding the arrays if needed.

		private void add_interval (double time, double mag) {

			// Check for non-decreasing time

			if (time < last_user_time) {
				throw new IllegalArgumentException ("OEMagCompFnDisc.MemoryBuilder.add_interval - Time out-of-order: time = " + time + ", last time = " + last_user_time);
			}

			last_user_time = time;

			// If there is a pending interval ...

			if (pend_full) {

				// If there is a pending interval with a later or equal time, just update the magnitude
				// (Only happens if an earlier interval was snapped forward to a later split time)

				if (pend_time >= time) {
					pend_adjust_mag (mag);
					return;
				}

				// If magnitude matches within epsilon, drop the interval
			
				if (Math.abs (pend_mag - mag) <= mag_eps) {
					return;
				}
			}

			// Insert any split times from before our time

			split_for_split (time);

			// If there is a split time after our time ...

			if (has_split()) {

				// The next split time, which will be later than our time

				double tsplit = get_split_time();
				SplitFn split_fn = get_split_fn();

				// If it is in range for this interval to snap to the next split time ...

				if (tsplit - time <= time_eps) {

					// If there is a pending interval ...

					if (pend_full) {

						// If the pending interval is a split ...

						if (pend_is_split) {

							// If we are closer to the pending interval ...

							if (time - pend_time <= tsplit - time) {
							
								// Snap to the pending interval by updating its magnitude, do not consume the next split time

								pend_adjust_mag (mag);
								return;
							}
						}

						// Otherwise the pending interval is not a split ...

						else {

							// If the pending interval is also in range to snap to the next split time ...

							if (tsplit - pend_time <= time_eps) {
							
								// Snap both by overwriting with our magnitude and next split time, and consume it

								pend_set_no_flush (tsplit, mag, true, split_fn);
								consume_split();
								return;
							}
						}
					}
							
					// Snap to the next split by setting interval with our magnitude and next split time, and consume it

					pend_flush_and_set (tsplit, mag, true, split_fn);
					consume_split();
					return;
				}
			}

			// If there is a pending interval ...

			if (pend_full) {

				// If we are in range to snap to the pending interval ...

				if (time - pend_time <= time_eps) {
							
					// Snap to the pending interval by updating its magnitude

					pend_adjust_mag (mag);
					return;
				}
			}

			// Write the pending interval

			pend_flush_and_set (time, mag, false, null);
			return;
		}




		// Add a magCat interval.

		public void add_interval_magCat (double time) {

			// Add it if the previous interval is not a magCat interval

			if (!( last_was_magCat )) {
				add_interval (time, magCat);
				last_was_magCat = true;
			}
		
			return;
		}




		// Add a constant interval.

		public void add_interval_constant (double time, double mag) {

			// If within epsilon of magCat, add a magCat interval

			if (Math.abs (mag - magCat) <= mag_eps) {
				add_interval_magCat (time);
				return;
			}

			// Add constant interval, not magCat

			add_interval (time, mag);
			last_was_magCat = false;
			return;
		}




		//--- Function to finish




		// Finish up the arrays, by truncating to the exact length needed.

		public void finish_up_arrays () {

			// Flush any remaining split times

			split_for_split (HUGE_TIME_DAYS);

			// Flush any pending interval

			pend_flush();
		
			// If arrays have extra space, trim them

			if (time_count < a_time.length) {
				a_time = Arrays.copyOf (a_time, time_count);
				a_mag = Arrays.copyOf (a_mag, time_count + 1);
			}

			return;
		}
	}




	//----- Building, Application Level -----




	// Build from an array of magnitudes and times.
	// Parameters:
	//  mag_time_array = Array containing magnitudes and times.
	//  mag_eps = The difference between magnitudes which are indistinguishable, if < 0 then none.
	//  time_eps = The difference between times which are indistinguishable, must be >= 0.
	// Note: All parameters must be already set up.
	// Note: For N intervals, mag_time_array contains 2*N-1 elements, which are:
	//   M0  t0  M1  t1  M2  t2  ...  M(N-2)  t(N-2)  M(N-1)
	// Times must be in strictly increasing order.

	public void build_from_mag_time_array (double[] mag_time_array, double mag_eps, double time_eps) {

		// Check the array has an odd number of elements

		if (mag_time_array.length % 2 == 0) {
			throw new IllegalArgumentException ("OEMagCompFnDisc.build_from_mag_time_array - Magnitude-time array length is even: length = " + mag_time_array.length);
		}

		// Make the memory builder

		MemoryBuilder memory_builder = new MemoryBuilder();

		// Set it up

		memory_builder.setup_arrays (mag_eps, time_eps, false);

		// Set the first interval

		memory_builder.first_interval (mag_time_array[0]);

		// Set succeeding intervals

		for (int n = 1; n < mag_time_array.length; n += 2) {
			memory_builder.add_interval_constant (mag_time_array[n], mag_time_array[n + 1]);
		}

		// Finish the arrays

		memory_builder.finish_up_arrays();

		return;
	}




	// Build from an array of magnitudes and times.
	// Parameters:
	//  mag_time_array = Array containing magnitudes and times.
	//  mag_eps = The difference between magnitudes which are indistinguishable, if < 0 then none.
	//  time_eps = The difference between times which are indistinguishable, must be >= 0.
	//  split_fn = Duration limit splitting function, if null then no splitting.
	//  tsplit_array = Array of split times.
	//  dlbase_array = Array of duration limit base flags, true means to apply the splitting function.
	// Note: All parameters must be already set up.
	// Note: For N intervals, mag_time_array contains 2*N-1 elements, which are:
	//   M0  t0  M1  t1  M2  t2  ...  M(N-2)  t(N-2)  M(N-1)
	// Times must be in strictly increasing order.

	public void build_from_mag_time_array (double[] mag_time_array, double mag_eps, double time_eps,
											SplitFn split_fn, double[] tsplit_array, boolean[] dlbase_array) {

		// Check the array has an odd number of elements

		if (mag_time_array.length % 2 == 0) {
			throw new IllegalArgumentException ("OEMagCompFnDisc.build_from_mag_time_array - Magnitude-time array length is even: length = " + mag_time_array.length);
		}

		// Check the split time and duration limit base arrays have the same number of elements

		if (tsplit_array.length != dlbase_array.length) {
			throw new IllegalArgumentException ("OEMagCompFnDisc.build_from_mag_time_array - Time split arrays have different length: tsplit_array.length = " + tsplit_array.length + ", dlbase_array.length = " + dlbase_array.length);
		}

		// Make the memory builder

		MemoryBuilder memory_builder = new MemoryBuilder();

		// Set it up

		memory_builder.setup_arrays (mag_eps, time_eps, split_fn != null);

		// Set split times

		for (int m = 0; m < tsplit_array.length; ++ m) {
			SplitFn the_split_fn = null;
			if (dlbase_array[m]) {
				the_split_fn = split_fn;
			}
			memory_builder.add_split (tsplit_array[m], the_split_fn);
		}

		// Set the first interval

		memory_builder.first_interval (mag_time_array[0]);

		// Set succeeding intervals

		for (int n = 1; n < mag_time_array.length; n += 2) {
			memory_builder.add_interval_constant (mag_time_array[n], mag_time_array[n + 1]);
		}

		// Finish the arrays

		memory_builder.finish_up_arrays();

		return;
	}




	//----- Construction -----




	// Default constructor does nothing.

	public OEMagCompFnDisc () {}




	// Construct from given parameters.

	public OEMagCompFnDisc (double magCat, double[] mag_time_array, double mag_eps, double time_eps,
							SplitFn split_fn, double[] tsplit_array, boolean[] dlbase_array) {

		// Save parameters

		this.magCat = magCat;

		// Generate the function

		build_from_mag_time_array (mag_time_array, mag_eps, time_eps,
									split_fn, tsplit_array, dlbase_array);
	}




	// Construct from given parameters.

	public OEMagCompFnDisc (double magCat, double[] mag_time_array, double mag_eps, double time_eps) {

		// Save parameters

		this.magCat = magCat;

		// Generate the function

		build_from_mag_time_array (mag_time_array, mag_eps, time_eps);
	}




	// Display our contents, short form.

	@Override
	public String toString() {
		
		return "FnDisc[magCat=" + magCat
		+ ", interval_count=" + get_interval_count()
		+ "]";
	}




	// Dump our entire contents to a string.

	public String dump_string () {
		StringBuilder result = new StringBuilder();

		int interval_count = get_interval_count();

		result.append ("OEMagCompFnDisc:" + "\n");
		result.append ("magCat = " + magCat + "\n");
		result.append ("interval_count = " + interval_count + "\n");
		for (int n = 0; n < interval_count; ++n) {
			result.append (get_interval_string (n) + "\n");
		}
		
		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 90001;

	private static final String M_VERSION_NAME = "OEMagCompFnDisc";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_DISC;
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

			writer.marshalDouble ("magCat", magCat);

			writer.marshalDoubleArray ("a_time", a_time);
			writer.marshalDoubleArray ("a_mag", a_mag);

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
			throw new MarshalException ("OEMagCompFnDisc.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			magCat = reader.unmarshalDouble ("magCat");

			// Array containing magnitudes and times

			double[] mag_time_array = reader.unmarshalDoubleArray ("mag_time_array");

			// Construct intervals

			try {

				double mag_eps = 0.0;
				double time_eps = 0.0;
				build_from_mag_time_array (mag_time_array, mag_eps, time_eps);
			}
			catch (Exception e) {
				throw new MarshalException ("OEMagCompFnDisc.do_umarshal: Unable to construct function from given magnitudes and times", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			magCat = reader.unmarshalDouble ("magCat");

			a_time = reader.unmarshalDoubleArray ("a_time");
			a_mag = reader.unmarshalDoubleArray ("a_mag");
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
	public OEMagCompFnDisc unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEMagCompFnDisc obj) {

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

	public static OEMagCompFnDisc unmarshal_poly (MarshalReader reader, String name) {
		OEMagCompFnDisc result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEMagCompFnDisc.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_DISC:
			result = new OEMagCompFnDisc();
			result.do_umarshal (reader);
			break;

		case MARSHAL_DISCFGH:
			result = new OEMagCompFnDiscFGH();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEMagCompFnDisc : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  magCat  query_time  query_delta  query_count  mag_eps  time_eps  mag  [time  mag]...
		// Build a function with the given parameters and rupture list.
		// Perform queries at the specified set of times.
		// Display detailed results.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 7 or more additional arguments

			if (!( args.length >= 8 && args.length % 2 == 0 )) {
				System.err.println ("OEMagCompFnDisc : Invalid 'test1' subcommand");
				return;
			}

			try {

				double magCat = Double.parseDouble (args[1]);
				double query_time = Double.parseDouble (args[2]);
				double query_delta = Double.parseDouble (args[3]);
				int query_count = Integer.parseInt (args[4]);
				double mag_eps = Double.parseDouble (args[5]);
				double time_eps = Double.parseDouble (args[6]);

				double[] mag_time_array = new double[args.length - 7];
				for (int ntm = 0; ntm < mag_time_array.length; ++ntm) {
					mag_time_array[ntm] = Double.parseDouble (args[ntm + 7]);
				}

				// Say hello

				System.out.println ("Generating discrete magnitude of completeness function");
				System.out.println ("magCat = " + magCat);
				System.out.println ("query_time = " + query_time);
				System.out.println ("query_delta = " + query_delta);
				System.out.println ("query_count = " + query_count);
				System.out.println ("mag_eps = " + mag_eps);
				System.out.println ("time_eps = " + time_eps);

				System.out.println ("mag_time_array:");
				System.out.println ("  mag = " + mag_time_array[0]);
				for (int ntm = 1; ntm < mag_time_array.length; ntm += 2) {
					System.out.println ("  time = " + mag_time_array[ntm] + ", mag = " + mag_time_array[ntm + 1]);
				}

				// Make the magnitude of completeness function

				OEMagCompFnDisc mag_comp_fn = new OEMagCompFnDisc (magCat, mag_time_array, mag_eps, time_eps);

				System.out.println ();
				System.out.println (mag_comp_fn.dump_string());

				// Do queries

				//System.out.println ();
				System.out.println ("queries:");

				for (int nq = 0; nq < query_count; ++nq) {
					double t = query_time + nq * query_delta;
					double mc = mag_comp_fn.get_mag_completeness (t);
					System.out.println ("  t = " + t + ", mc = " + mc);
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  magCat  query_time  query_delta  query_count  mag_eps  time_eps
		//         durlim_ratio  durlim_min  durlim_max
		//         split_count  [tsplit  dlbase]  mag  [time  mag]...
		// Build a function with the given parameters, rupture list, and splitting.
		// Perform queries at the specified set of times.
		// Display detailed results.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 11 or more additional arguments

			if (!( args.length >= 12 && args.length % 2 == 0 )) {
				System.err.println ("OEMagCompFnDisc : Invalid 'test2' subcommand");
				return;
			}

			try {

				double magCat = Double.parseDouble (args[1]);
				double query_time = Double.parseDouble (args[2]);
				double query_delta = Double.parseDouble (args[3]);
				int query_count = Integer.parseInt (args[4]);
				double mag_eps = Double.parseDouble (args[5]);
				double time_eps = Double.parseDouble (args[6]);

				double durlim_ratio = Double.parseDouble (args[7]);
				double durlim_min = Double.parseDouble (args[8]);
				double durlim_max = Double.parseDouble (args[9]);

				int split_count = Integer.parseInt (args[10]);
				double[] tsplit_array = new double[split_count];
				boolean[] dlbase_array = new boolean[split_count];

				if (!( args.length >= split_count*2 + 12 )) {
					System.err.println ("OEMagCompFnDisc : Invalid 'test2' subcommand: Insufficient length for time-splitting arrays");
				}

				for (int ntm = 0; ntm < tsplit_array.length; ++ntm) {
					tsplit_array[ntm] = Double.parseDouble (args[ntm*2 + 11]);
					dlbase_array[ntm] = Boolean.parseBoolean (args[ntm*2 + 12]);
				}

				if (!( (args.length - (split_count*2 + 11)) % 2 == 1 )) {
					System.err.println ("OEMagCompFnDisc : Invalid 'test2' subcommand: Invalid length for time-magnitude array");
				}

				double[] mag_time_array = new double[args.length - (split_count*2 + 11)];
				for (int ntm = 0; ntm < mag_time_array.length; ++ntm) {
					mag_time_array[ntm] = Double.parseDouble (args[ntm + (split_count*2 + 11)]);
				}

				// Say hello

				System.out.println ("Generating discrete magnitude of completeness function");
				System.out.println ("magCat = " + magCat);
				System.out.println ("query_time = " + query_time);
				System.out.println ("query_delta = " + query_delta);
				System.out.println ("query_count = " + query_count);
				System.out.println ("mag_eps = " + mag_eps);
				System.out.println ("time_eps = " + time_eps);
				System.out.println ("durlim_ratio = " + durlim_ratio);
				System.out.println ("durlim_min = " + durlim_min);
				System.out.println ("durlim_max = " + durlim_max);
				System.out.println ("split_count = " + split_count);

				System.out.println ("time-split arrays:");
				for (int ntm = 0; ntm < tsplit_array.length; ++ntm) {
					System.out.println ("  tsplit = " + tsplit_array[ntm] + ", dlbase = " + dlbase_array[ntm]);
				}

				System.out.println ("mag_time_array:");
				System.out.println ("  mag = " + mag_time_array[0]);
				for (int ntm = 1; ntm < mag_time_array.length; ntm += 2) {
					System.out.println ("  time = " + mag_time_array[ntm] + ", mag = " + mag_time_array[ntm + 1]);
				}

				// Make time-splitting function

				SplitFn split_fn = new SplitFnRatio (durlim_ratio, durlim_min, durlim_max);

				// Make the magnitude of completeness function

				OEMagCompFnDisc mag_comp_fn = new OEMagCompFnDisc (magCat, mag_time_array, mag_eps, time_eps,
																	split_fn, tsplit_array, dlbase_array);

				System.out.println ();
				System.out.println (mag_comp_fn.dump_string());

				// Do queries

				//System.out.println ();
				System.out.println ("queries:");

				for (int nq = 0; nq < query_count; ++nq) {
					double t = query_time + nq * query_delta;
					double mc = mag_comp_fn.get_mag_completeness (t);
					System.out.println ("  t = " + t + ", mc = " + mc);
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEMagCompFnDisc : Unrecognized subcommand : " + args[0]);
		return;

	}

}
