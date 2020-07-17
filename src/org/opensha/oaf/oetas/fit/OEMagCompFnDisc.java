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

	protected int get_interval_count () {
		if (a_mag == null) {
			return 0;
		}
		return a_mag.length;
	}


	// Create a string describing the n-th interval.

	protected String get_interval_string (int n) {
		StringBuilder result = new StringBuilder();

		// Interval start time, but not on the first interval which starts at -infinity

		if (n > 0) {
			result.append("time = ").append(a_time[n-1]).append(": ");
		}

		// Constant function

		result.append("constant: mag = ").append(a_mag[n]);
	
		return result.toString();
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




	// Inner class to manage memory.
	// The user must begin by calling setup_arrays.
	// Then, the user calls the various add_interval methods.
	// Then, the user must call finish_up_arrays to put the arrays into final form.

	protected class MemoryBuilder {

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


		// Set up the arrays.

		public void setup_arrays (double mag_eps, double time_eps) {

			// Check arguments

			if (!( time_eps >= 0.0 )) {
				throw new IllegalArgumentException ("OEMagCompFnDisc.MemoryBuilder.setup_arrays - Invalid time epsilon: time_eps = " + time_eps);
			}

			// Save magnitude epsilon

			this.mag_eps = mag_eps;

			// Save time epsilon

			this.time_eps = time_eps;

			// Default capacity, in number of times

			int default_capacity = 16;

			// Allocate the initial arrays

			a_time = new double[default_capacity];
			a_mag = new double[default_capacity + 1];

			// No times yet

			time_count = 0;

			// Insert an initial magCat interval

			last_was_magCat = true;
			a_mag[0] = magCat;

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


		// Add an interval, expanding the arrays if needed.

		private void add_interval (double time, double mag) {

			// Check for increasing time, and for time within epsilon

			if (time_count > 0) {

				double delta_t = time - a_time[time_count - 1];

				if (delta_t < 0.0) {
					throw new IllegalArgumentException ("OEMagCompFnDisc.MemoryBuilder.add_interval - Time out-of-order: time = " + time + ", last time = " + a_time[time_count - 1]);
				}

				if (delta_t <= time_eps) {
					a_mag[time_count] = mag;
					return;
				}
			}

			// Check for combinable interval

			if (Math.abs (mag - a_mag[time_count]) <= mag_eps) {
				return;
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


		// Finish up the arrays, by truncating to the exact length needed.

		public void finish_up_arrays () {
		
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

		memory_builder.setup_arrays (mag_eps, time_eps);

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




		// Unrecognized subcommand.

		System.err.println ("OEMagCompFnDisc : Unrecognized subcommand : " + args[0]);
		return;

	}

}
