package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Arrays;

import org.opensha.oaf.oetas.OERupture;

import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import static org.opensha.oaf.util.SimpleUtils.rndd;

import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG;				// negative mag smaller than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_NEG_CHECK;		// use x <= NO_MAG_NEG_CHECK to check for NO_MAG_NEG
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS;				// positive mag larger than any possible mag
import static org.opensha.oaf.oetas.OEConstants.NO_MAG_POS_CHECK;		// use x >= NO_MAG_POS_CHECK to check for NO_MAG_POS
import static org.opensha.oaf.oetas.OEConstants.TINY_MAG_DELTA;			// a very small change in magnitude
import static org.opensha.oaf.oetas.OEConstants.GEN_TIME_EPS;			// time epsilon when generating catalogs
import static org.opensha.oaf.oetas.OEConstants.GROUP_TIME_EPS;			// time epsilon when grouping sources
import static org.opensha.oaf.oetas.OEConstants.DEF_GS_REL_BASE_TIME;	// group span default relative base time
import static org.opensha.oaf.oetas.OEConstants.DEF_GS_RATIO;			// group span defalt ratio
import static org.opensha.oaf.oetas.OEConstants.DEF_GS_MIN_WIDTH;		// group span default minimum width
import static org.opensha.oaf.oetas.OEConstants.DEF_GR_HI_MAG_DELTA;	// group rupture default high magnitude delta
import static org.opensha.oaf.oetas.OEConstants.DEF_GR_TAPER_MAG_DELTA;	// group rupture default taper magnitude delta
import static org.opensha.oaf.oetas.OEConstants.DEF_GR_INIT_MAG;		// group rupture default initial magnitude
import static org.opensha.oaf.oetas.OEConstants.DEF_GR_LO_RATIO;		// group rupture default low ratio
import static org.opensha.oaf.oetas.OEConstants.DEF_GR_HI_RATIO;		// group rupture default high ratio

import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MCT_INFINITY;		// 1 = From time-dependent magnitude of completeness to infinity.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MCT_MAG_MAX;		// 2 = From time-dependent magnitude of completeness to maximum simulation magnitude.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MAGCAT_INFINITY;	// 3 = From catalog magnitude of completeness to infinity.
import static org.opensha.oaf.oetas.OEConstants.LMR_OPT_MAGCAT_MAG_MAX;		// 4 = From catalog magnitude of completeness to maximum simulation magnitude.


// Grouping of point and interval sources for a discretized rupture history.
// Author: Michael Barall 01/24/2023.
//
// When ruptures and intervals occur close together in time, they may be combined
// into a group which acts as a single source when seeding simulations.  This
// class specifies how they are grouped, and includes code for building the groups.
//
// The maximum allowed time span for a group is variable, and increses as one goes
// farther back into the past.
//
// A grouping can be constructed from an OEDisc2History object.
//
// Threading: After the grouping is built, the object is not subsequently changed
// and so it may be accessed concurrently from multiple threads.

public class OEDisc2Grouping {

	//----- Grouping -----

	// Number of groups.

	public int group_count;

	// Time for each group, in days.
	// The length is group_count.
	// a_group_time[n] is the time of group n, to be used when seeding a simulation.

	public double[] a_group_time;

	// Group number for each rupture.
	// The length is rupture_count.
	// a_rupture_group[n] is the group number for rupture n, or -1 if the rupture is not in any group.

	public int[] a_rupture_group;

	// Group rupture range, the beginning and ending+1 of ruptures that appear in a group.

	public int group_rup_begin;
	public int group_rup_end;

	// Group number for each interval.
	// The length is interval_count.
	// a_interval_group[n] is the group number for interval n, or -1 if the interval is not in any group.

	public int[] a_interval_group;

	// Group interval range, the beginning and ending+1 of intervals that appear in a group.

	public int group_int_begin;
	public int group_int_end;




	//----- Variable functions -----




	// Abstract class that defines a function giving the maximum allowed width of a group.
	//
	// Note: The function get_max_width(t) must have a derivative strictly between -2 and 2.
	// This ensures that both  t+0.5*get_max_width(t)  and  t-0.5*get_max_width(t) are
	// strictly monotone increasing.

	public static abstract class SpanWidthFcn {

		// Given a span center time, return the maximum allowed width of a span, in days.
		// Note this is the full width; each endpoint of the span is half the width from the center.

		public abstract double get_max_width (double t);
	}




	// Define the maximum allowed width of a group as time before an anchor, multiplied by a ratio.
	// The maximum width at time t is:
	//  max (min_width, width_ratio * (width_base_time - t))

	public static class SpanWidthFcnRatio extends SpanWidthFcn {

		// Width calculation base time, in days.

		private double width_base_time;

		// Width ratio (dimensionless).

		private double width_ratio;

		// Minimum width, in days.

		private double min_width;

		// Constructor.

		public SpanWidthFcnRatio (double width_base_time, double width_ratio, double min_width) {
			this.width_base_time = width_base_time;
			this.width_ratio = width_ratio;
			this.min_width = min_width;
		}

		// Given a span center time, return the maximum allowed width of a span, in days.
		// Note this is the full width; each endpoint of the span is half the width from the center.

		@Override
		public double get_max_width (double t) {
			return Math.max (min_width, (width_base_time - t) * width_ratio);
		}
	}




	// Given a width function, convert it so the time it sees is relative to a given start time.
	// The maximum width at time t is:
	//  wfcn.get_max_width (t - start_time)

	public static class SpanWidthFcnShift extends SpanWidthFcn {

		// The contained function.

		private SpanWidthFcn wfcn;

		// Start time, in days, typically the time at which the simulation will begin.

		private double start_time;

		// Constructor.

		public SpanWidthFcnShift (SpanWidthFcn wfcn, double start_time) {
			this.wfcn = wfcn;
			this.start_time = start_time;
		}

		// Given a span center time, return the maximum allowed width of a span, in days.
		// Note this is the full width; each endpoint of the span is half the width from the center.

		@Override
		public double get_max_width (double t) {
			return wfcn.get_max_width (t - start_time);
		}
	}




	// Abstract class that defines a function giving width of a rupture.
	//
	// Note: The function get_max_width(t) must have a derivative strictly between -2 and 2.
	// This ensures that both  t+0.5*get_max_width(t)  and  t-0.5*get_max_width(t) are
	// strictly monotone increasing.

	public static abstract class RupWidthFcn {

		// Get the width to use for a rupture.
		// Parameters:
		//  t = Time of rupture, in days.
		//  mag = Magnitude of rupture.
		//  largest_mag = Largest magnitude seen so far (including this rupture), scanning ruptures from last to first.
		//  max_width = Maximum allowed width, in days.
		// Returns a width between 0.0 and max_width.
		// Note this is the full width; each endpoint of the span is half the width from the center.

		public abstract double get_rup_width (double t, double mag, double largest_mag, double max_width);
	}




	// A function giving the width of a rupture, which is a constant ratio of the maximum.
	// The width is defined as follows:
	//   The width is max_width * ratio.

	public static class RupWidthFcnRatio extends RupWidthFcn {

		// Width ratio.

		private double ratio;

		// Constructor.

		public RupWidthFcnRatio (double ratio) {
			this.ratio = ratio;
		}

		// Get the width to use for a rupture.
		// Parameters:
		//  t = Time of rupture, in days.
		//  mag = Magnitude of rupture.
		//  largest_mag = Largest magnitude seen so far (including this rupture), scanning ruptures from last to first.
		//  max_width = Maximum allowed width, in days.
		// Returns a width between 0.0 and max_width.
		// Note this is the full width; each endpoint of the span is half the width from the center.

		@Override
		public double get_rup_width (double t, double mag, double largest_mag, double max_width) {
			double width = max_width * ratio;
			if (width < 0.0) {
				width = 0.0;
			} else if (width > max_width) {
				width = max_width;
			}
			return width;
		}
	}




	// A function giving the width of a rupture, using a linear taper, relative to the largest magnitude.
	// The width is defined as follows:
	//   If mag >= max(init_mag, largest_mag) - hi_mag_delta, then the width is max_width * hi_ratio.
	//   If mag <= max(init_mag, largest_mag) - (hi_mag_delta + taper_mag_delta), then the width is max_width * lo_ratio.
	//   Otherwise, the width is linearly tapered between those two values.
	// Magnitude deltas may be adjusted slightly to allow for rounding errors and avoid divide-by-zero.

	public static class RupWidthFcnTaper extends RupWidthFcn {

		// Hi and lo width ratios.

		private double lo_ratio;
		private double hi_ratio;

		// Magnitude delta during which hi ratio applies.

		private double hi_mag_delta;

		// Magnitude delta during which taper ratio applies.

		private double taper_mag_delta;

		// Initial magnitude to use for largest magnitude.

		private double init_mag;

		// Constructor.

		public RupWidthFcnTaper (double lo_ratio, double hi_ratio, double hi_mag_delta, double taper_mag_delta, double init_mag) {
			this.lo_ratio = lo_ratio;
			this.hi_ratio = hi_ratio;
			this.hi_mag_delta = Math.max (TINY_MAG_DELTA, hi_mag_delta);
			this.taper_mag_delta = Math.max (TINY_MAG_DELTA, taper_mag_delta);
			this.init_mag = init_mag;
		}

		// Get the width to use for a rupture.
		// Parameters:
		//  t = Time of rupture, in days.
		//  mag = Magnitude of rupture.
		//  largest_mag = Largest magnitude seen so far (including this rupture), scanning ruptures from last to first.
		//  max_width = Maximum allowed width, in days.
		// Returns a width between 0.0 and max_width.
		// Note this is the full width; each endpoint of the span is half the width from the center.

		@Override
		public double get_rup_width (double t, double mag, double largest_mag, double max_width) {
			double ratio = lo_ratio;
			final double cut_hi = Math.max(init_mag, largest_mag) - hi_mag_delta;
			final double cut_lo = cut_hi - taper_mag_delta;
			if (mag >= cut_hi) {
				ratio = hi_ratio;
			} else if (mag > cut_lo) {
				ratio += ( ((mag - cut_lo) / taper_mag_delta) * (hi_ratio - lo_ratio) );
			}
			double width = max_width * ratio;
			if (width < 0.0) {
				width = 0.0;
			} else if (width > max_width) {
				width = max_width;
			}
			return width;
		}
	}




	// Abstract class that defines a function specifying which sources to accept.
	// This can be used with both ruptures and intervals.

	public static abstract class AcceptSrcFcn {

		// Given a source index number, return true if the source is accepted and added to a group.

		public abstract boolean accept_source (int i_src);
	}




	// Define accepted sources, as no sources.

	public static class AcceptSrcFcnNone extends AcceptSrcFcn {

		// Constructor.

		public AcceptSrcFcnNone () {
		}

		// Given a source index number, return true if the source is accepted and added to a group.

		@Override
		public boolean accept_source (int i_src) {
			return true;
		}
	}




	// Define accepted sources, as all sources.

	public static class AcceptSrcFcnAll extends AcceptSrcFcn {

		// Constructor.

		public AcceptSrcFcnAll () {
		}

		// Given a source index number, return true if the source is accepted and added to a group.

		@Override
		public boolean accept_source (int i_src) {
			return false;
		}
	}




	// Define accepted sources, as lying in a range.

	public static class AcceptSrcFcnRange extends AcceptSrcFcn {

		// The beginning and ending+1 of acceptable sources.

		private int accept_begin;
		private int accept_end;

		// Constructor.

		public AcceptSrcFcnRange (int accept_begin, int accept_end) {
			this.accept_begin = accept_begin;
			this.accept_end = accept_end;
		}

		// Given a source index number, return true if the source is accepted and added to a group.

		@Override
		public boolean accept_source (int i_src) {
			if (i_src >= accept_begin && i_src < accept_end) {
				return true;
			}
			return false;
		}
	}




	// Define accepted sources, as lying in a range, with exclusions.

	public static class AcceptSrcFcnRangeExc extends AcceptSrcFcn {

		// The beginning and ending+1 of acceptable sources.

		private int accept_begin;
		private int accept_end;

		// Array, which is true if the source should be excluded.

		private boolean[] a_exclude;

		// Constructor.
		// The array a_exclude is retained.

		public AcceptSrcFcnRangeExc (int accept_begin, int accept_end, boolean[] a_exclude) {
			this.accept_begin = accept_begin;
			this.accept_end = accept_end;
			this.a_exclude = a_exclude;
		}

		// Given a source index number, return true if the source is accepted and added to a group.

		@Override
		public boolean accept_source (int i_src) {
			if (i_src >= accept_begin && i_src < accept_end && !(a_exclude[i_src])) {
				return true;
			}
			return false;
		}
	}




	// Define accepted sources, as lying in a range, with level exceeding a threshold.

	public static class AcceptSrcFcnRangeLevel extends AcceptSrcFcn {

		// The beginning and ending+1 of acceptable sources.

		private int accept_begin;
		private int accept_end;

		// Array, which gives a level for each source.

		private double[] a_level;

		// Threshold,  a_level[n] > threshold  is required to accept source n.

		private double threshold;

		// Constructor.
		// The array a_exclude is retained.

		public AcceptSrcFcnRangeLevel (int accept_begin, int accept_end, double[] a_level, double threshold) {
			this.accept_begin = accept_begin;
			this.accept_end = accept_end;
			this.a_level = a_level;
			this.threshold = threshold;
		}

		// Given a source index number, return true if the source is accepted and added to a group.

		@Override
		public boolean accept_source (int i_src) {
			if (i_src >= accept_begin && i_src < accept_end && a_level[i_src] > threshold) {
				return true;
			}
			return false;
		}
	}




	//----- Time span -----




	// Class that represents a time span.

	private static class Span {

		//--- Data

		// Lower limit of the time span, in days.

		private double t_lo;

		// Upper limit of the time span, in days.

		private double t_hi;

		// The center of the time span, in days.

		private double center;

		// Number of ruptures in the time span.

		private int n_rup;

		// Number of intervals in the time span.

		private int n_int;


		//--- Accessors

		// Get the lower limit.

		public final double get_t_lo () {
			return t_lo;
		}

		// Get the upper limit.

		public final double get_t_hi () {
			return t_hi;
		}

		// Get the center.

		public final double get_center () {
			return center;
		}

		// Get the width.

		public final double get_width () {
			return t_hi - t_lo;
		}

		// Get the number of ruptures.

		public final int get_n_rup () {
			return n_rup;
		}

		// Get the number of intervals.

		public final int get_n_int () {
			return n_int;
		}


		//--- Creation


		// Create a zero span.

		public Span () {
			t_lo = 0.0;
			t_hi = 0.0;
			center = 0.0;
			n_rup = 0;
			n_int = 0;
		}


		// Set the span to zero.

		public final void clear () {
			t_lo = 0.0;
			t_hi = 0.0;
			center = 0.0;
			n_rup = 0;
			n_int = 0;
			return;
		}


		// Copy from another span, return this object.

		public final Span copy_from (Span other) {
			this.t_lo = other.t_lo;
			this.t_hi = other.t_hi;
			this.center = other.center;
			this.n_rup = other.n_rup;
			this.n_int = other.n_int;
			return this;
		}


		//--- Setting functions


		// Set the center and width, for a rupture, return this object.

		public final Span set_for_rupture (double center, double width) {
			final double half_width = width * 0.5;
			this.t_lo = center - half_width;
			this.t_hi = center + half_width;
			this.center = center;
			this.n_rup = 1;
			this.n_int = 0;
			return this;
		}


		// Set the lower and upper limits, trim to maximum allowed width, for an interval, return this object.

		public final Span set_for_interval (double t_lo, double t_hi, SpanWidthFcn wfcn) {
			this.center = (t_lo + t_hi) * 0.5;
			final double width = t_hi - t_lo;;
			final double max_width = wfcn.get_max_width (this.center);
			if (width <= max_width) {
				this.t_lo = t_lo;
				this.t_hi = t_hi;
			} else {
				final double half_width = max_width * 0.5;
				this.t_lo = center - half_width;
				this.t_hi = center + half_width;
			}
			this.n_rup = 0;
			this.n_int = 1;
			return this;
		}


		//--- Operations


		// Attempt to merge in another span whose center is assumed to be less than (to the left of) this span.
		// Returns true if success, false if unable to merge.

		public final boolean merge_left (Span other, SpanWidthFcn wfcn, double teps) {

			// If not extending to the left ...

			if (other.t_lo >= this.t_lo) {

				// We should not be extending to the right, but in case we are
				// (due to rounding errors), update the span

				if (other.t_hi > this.t_hi) {
					this.t_hi = other.t_hi;
					this.center = (this.t_lo + this.t_hi) * 0.5;
				}

				// Successful merge

				this.n_rup += other.n_rup;
				this.n_int += other.n_int;
				return true;
			}

			// If the other span contains this span, just copy the other span

			if (other.t_hi >= this.t_hi) {
				this.t_lo = other.t_lo;
				this.t_hi = other.t_hi;
				this.center = other.center;

				// Successful merge

				this.n_rup += other.n_rup;
				this.n_int += other.n_int;
				return true;
			}

			// Extending to the left, check if width is in range, unsuccessful merge if not

			final double new_center = (other.t_lo + this.t_hi) * 0.5;
			final double new_width = this.t_hi - other.t_lo;
			final double max_width = wfcn.get_max_width (new_center);
			if (new_width > max_width + teps) {
				return false;
			}

			// Extend left and return success

			this.t_lo = other.t_lo;
			this.center = new_center;

			this.n_rup += other.n_rup;
			this.n_int += other.n_int;
			return true;
		}


		// Attempt to merge in another span whose center is assumed to be greater than (to the right of) this span.
		// Returns true if success, false if unable to merge.

		public final boolean merge_right (Span other, SpanWidthFcn wfcn, double teps) {

			// If not extending to the right ...

			if (other.t_hi <= this.t_hi) {

				// We should not be extending to the left, but in case we are
				// (due to rounding errors), update the span

				if (other.t_lo < this.t_lo) {
					this.t_lo = other.t_lo;
					this.center = (this.t_lo + this.t_hi) * 0.5;
				}

				// Successful merge

				this.n_rup += other.n_rup;
				this.n_int += other.n_int;
				return true;
			}

			// If the other span contains this span, just copy the other span

			if (other.t_lo <= this.t_lo) {
				this.t_lo = other.t_lo;
				this.t_hi = other.t_hi;
				this.center = other.center;

				// Successful merge

				this.n_rup += other.n_rup;
				this.n_int += other.n_int;
				return true;
			}

			// Extending to the right, check if width is in range, unsuccessful merge if not

			final double new_center = (this.t_lo + other.t_hi) * 0.5;
			final double new_width = other.t_hi - this.t_lo;
			final double max_width = wfcn.get_max_width (new_center);
			if (new_width > max_width + teps) {
				return false;
			}

			// Extend right and return success

			this.t_hi = other.t_hi;
			this.center = new_center;

			this.n_rup += other.n_rup;
			this.n_int += other.n_int;
			return true;
		}


		// Produce a one-line string showing the contents.

		@Override
		public String toString() {
			return String.format ("t_lo = %.6f, t_hi = %.6f, center = %.6f, width = %.6f, n_rup = %d, n_int = %d",
				t_lo,
				t_hi,
				center,
				get_width(),
				n_rup,
				n_int
			);
		}


		// Marshal object.

		public void marshal (MarshalWriter writer, String name) {
			writer.marshalMapBegin (name);

			writer.marshalDouble ("t_lo"  , t_lo  );
			writer.marshalDouble ("t_hi"  , t_hi  );
			writer.marshalDouble ("center", center);
			writer.marshalInt    ("n_rup" , n_rup );
			writer.marshalInt    ("n_int" , n_int );

			writer.marshalMapEnd ();
			return;
		}


		// Unmarshal object.

		public Span unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);

			t_lo   = reader.unmarshalDouble ("t_lo"  );
			t_hi   = reader.unmarshalDouble ("t_hi"  );
			center = reader.unmarshalDouble ("center");
			n_rup  = reader.unmarshalInt    ("n_rup" );
			n_int  = reader.unmarshalInt    ("n_int" );

			reader.unmarshalMapEnd ();
			return this;
		}
	}




	// Span for each group.
	// The length is group_count.
	// a_group_span[n] is the span of group n.

	private Span[] a_group_span;

	// Total number of ruptures in all groups.

	private int total_n_rup;

	// Total number of intervals in all groups.

	private int total_n_int;




	// Check the object invariant.
	// Returns null if OK, or error message if invariant is violated.

	public final String check_invariant () {

		// Check group count

		if (!( group_count >= 0 )) {
			return "Negative group count: group_count = " + group_count;
		}

		// Check array allocations

		if (!( a_group_time != null )) {
			return "Array a_group_time is null";
		}
		if (!( a_group_time.length == group_count )) {
			return "Array a_group_time has incorrect length: found " + a_group_time.length + ", expected" + group_count;
		}

		if (!( a_rupture_group != null )) {
			return "Array a_rupture_group is null";
		}

		if (!( a_interval_group != null )) {
			return "Array a_interval_group is null";
		}

		if (!( a_group_span != null )) {
			return "Array a_group_span is null";
		}
		if (!( a_group_span.length == group_count )) {
			return "Array a_group_span has incorrect length: found " + a_group_span.length + ", expected" + group_count;
		}

		// Check rupture index range

		if (!( 0 <= group_rup_begin && group_rup_begin <= group_rup_end && group_rup_end <= a_rupture_group.length )) {
			return "Invalid rupture index range: group_rup_begin = " + group_rup_begin + ", group_rup_end = " + group_rup_end + ", expected 0 thru " + a_rupture_group.length;
		}

		if (group_rup_begin < group_rup_end) {
			if (!( a_rupture_group[group_rup_begin] >= 0 )) {
				return "Found leading skipped rupture at index " + group_rup_begin;
			}
			if (!( a_rupture_group[group_rup_end - 1] >= 0 )) {
				return "Found trailing skipped rupture at index " + (group_rup_end - 1);
			}
		}

		// Check interval index range

		if (!( 0 <= group_int_begin && group_int_begin <= group_int_end && group_int_end <= a_interval_group.length )) {
			return "Invalid interval index range: group_int_begin = " + group_int_begin + ", group_int_end = " + group_int_end + ", expected 0 thru " + a_interval_group.length;
		}

		if (group_int_begin < group_int_end) {
			if (!( a_interval_group[group_int_begin] >= 0 )) {
				return "Found leading skipped interval at index " + group_int_begin;
			}
			if (!( a_interval_group[group_int_end - 1] >= 0 )) {
				return "Found trailing skipped interval at index " + (group_int_end - 1);
			}
		}

		// Check each group

		int i_rup = 0;
		int i_int = 0;

		int found_total_n_rup = 0;
		int found_total_n_int = 0;

		for (int i_grp = 0; i_grp < group_count; ++i_grp) {
			Span span = a_group_span[i_grp];

			// Check span existence

			if (!( span != null )) {
				return "Missing group span: a_group_span[" + i_grp + "] is null";
			}

			// Check the group is non-empty

			if (!( span.get_n_rup() != 0 || span.get_n_int() != 0 )) {
				return "Group span contains no ruptures or intervals: a_group_span[" + i_grp + "] is empty";
			}

			// Check time

			if (!( Math.abs (span.get_center() - a_group_time[i_grp]) <= Math.min (GROUP_TIME_EPS, GEN_TIME_EPS) )) {
				return "Incorrect group time: a_group_time[" + i_grp + "] = " + a_group_time[i_grp] + ", expected " + span.get_center();
			}

			// Scan for ruptures in this group

			int found_n_rup = 0;
			while (i_rup < a_rupture_group.length) {
				if (a_rupture_group[i_rup] < -1 || a_rupture_group[i_rup] >= group_count) {
					return "Invalid rupture group number: a_rupture_group[" + i_rup + "] = " + a_rupture_group[i_rup] + ", expected -1 thru " + (group_count - 1);
				}
				if (a_rupture_group[i_rup] >= 0) {
					if (a_rupture_group[i_rup] < i_grp) {
						return "Out-of-order rupture group number: a_rupture_group[" + i_rup + "] = " + a_rupture_group[i_rup] + ", expected " + i_grp + " thru " + (group_count - 1);
					}
					if (a_rupture_group[i_rup] > i_grp) {
						break;
					}
					if (!( i_rup >= group_rup_begin && i_rup < group_rup_end )) {
						return "Rupture index out-of-range: a_rupture_group[" + i_rup + "] = " + a_rupture_group[i_rup] + ", expected index " + group_rup_begin + " thru " + (group_rup_end - 1);
					}
					++found_n_rup;
					++found_total_n_rup;
				}
				++i_rup;
			}

			if (!( span.get_n_rup() == found_n_rup )) {
				return "Incorrect number of ruptures in group " + i_grp + ": found " + found_n_rup + ", expected " + span.get_n_rup();
			}

			// Scan for intervals in this group

			int found_n_int = 0;
			while (i_int < a_interval_group.length) {
				if (a_interval_group[i_int] < -1 || a_interval_group[i_int] >= group_count) {
					return "Invalid interval group number: a_interval_group[" + i_int + "] = " + a_interval_group[i_int] + ", expected -1 thru " + (group_count - 1);
				}
				if (a_interval_group[i_int] >= 0) {
					if (a_interval_group[i_int] < i_grp) {
						return "Out-of-order interval group number: a_interval_group[" + i_int + "] = " + a_interval_group[i_int] + ", expected " + i_grp + " thru " + (group_count - 1);
					}
					if (a_interval_group[i_int] > i_grp) {
						break;
					}
					if (!( i_int >= group_int_begin && i_int < group_int_end )) {
						return "Interval index out-of-range: a_interval_group[" + i_int + "] = " + a_interval_group[i_int] + ", expected index " + group_int_begin + " thru " + (group_int_end - 1);
					}
					++found_n_int;
					++found_total_n_int;
				}
				++i_int;
			}

			if (!( span.get_n_int() == found_n_int )) {
				return "Incorrect number of intervals in group " + i_grp + ": found " + found_n_int + ", expected " + span.get_n_int();
			}
		}

		// Check we checked all ruptures and intervals (should always be true)

		if (!( i_rup == a_rupture_group.length )) {
			return "Failed to check all ruptures, i_rup = " + i_rup + ", expected " + a_rupture_group.length;
		}

		if (!( i_int == a_interval_group.length )) {
			return "Failed to check all intervals, i_int = " + i_int + ", expected " + a_interval_group.length;
		}

		// Check total rupture count

		if (!( total_n_rup == found_total_n_rup )) {
			return "Incorrect total number of ruptures: found " + found_total_n_rup + ", expected " + total_n_rup;
		}

		// Check total interval count

		if (!( total_n_int == found_total_n_int )) {
			return "Incorrect total number of intervals: found " + found_total_n_int + ", expected " + total_n_int;
		}

		// All OK

		return null;
	}




	//----- Building -----




	// Class for building the grouping.
	// This version builds the list of groups in the reverse direction, from last to first.

	private class Builder {

		//--- Parameters

		// List of ruptures, in order of increasing time.
		// The length is rupture_count.
		// a_rupture_obj[n].t_day is the rupture time, in days.
		// a_rupture_obj[n].rup_mag is the rupture magnitude.
		// a_rupture_obj[n].k_prod is not used.
		// a_rupture_obj[n].rup_parent is not used.
		// a_rupture_obj[n].x_km is the rupture x-ccordinate, in km (currently zero).
		// a_rupture_obj[n].y_km is the rupture y-ccordinate, in km (currently zero).

		private OERupture[] a_rupture_obj;

		// Begin and end times for each interval, in days.
		// The length is interval_count + 1.
		// a_interval_time[n] is the begin time of interval n.
		// a_interval_time[n + 1] is the end time of interval n.
		// a_interval_time[0] is the beginning of the time range.
		// a_interval_time[interval_count] is the ending of the time range.
		// Note: If there are no intervals, this can be null, or an array with zero or one elements.

		private double[] a_interval_time;

		// Span width function, which gives the maximum width of a span.

		private SpanWidthFcn span_width_fcn;

		// Rupture width function, which gives the width to assign to a rupture.

		private RupWidthFcn rup_width_fcn;

		// Rupture accept function, which tells which ruptures to include.

		private AcceptSrcFcn rup_accept_fcn;

		// Interval accept function, which tells which intervals to include.

		private AcceptSrcFcn int_accept_fcn;

		// Time epsilon to use for width calculations.

		private double width_teps;


		//--- Working storage

		// Current rupture, should be initialized to rupture_count.

		private int current_rupture;

		// Current interval, should be initialized to interval_count.

		private int current_interval;

		// Span for current rupture, null if none.

		private Span rupture_span;

		// Span for current interval, null if none.

		private Span interval_span;

		// Largest magnitude seen so far, should be initialized to NO_MAG_NEG.

		private double largest_mag;

		// Span for current group, null if none.

		private Span group_span;

		// List of group spans constructed so far, in order of construction (from last to first).

		private ArrayList<Span> group_span_list;


		//--- Subroutines


		// Advance to next rupture, and set up the current rupture span.

		private final void next_rupture () {

			// Loop over ruptures, in decending order ...

			for (--current_rupture; current_rupture >= 0; --current_rupture) {

				// If this rupture needs to be in a group ...

				if (rup_accept_fcn.accept_source (current_rupture)) {

					final double t = a_rupture_obj[current_rupture].t_day;
					final double mag = a_rupture_obj[current_rupture].rup_mag;

					// Update the largest magnitude

					largest_mag = Math.max (largest_mag, mag);

					// Create the span for this rupture

					if (rupture_span == null) {
						rupture_span = new Span();
					}
					final double max_width = span_width_fcn.get_max_width (t);
					final double width = rup_width_fcn.get_rup_width (t, mag, largest_mag, max_width);
					rupture_span.set_for_rupture (t, width);

					// Done

					return;
				}

				// Otherwise, mark rupture as not belonging to any group

				a_rupture_group[current_rupture] = -1;
			}

			// No more ruptures

			rupture_span = null;
			return;
		}


		// Advance to next interval, and set up the current interval span.

		private final void next_interval () {

			// Loop over intervals, in decending order ...

			for (--current_interval; current_interval >= 0; --current_interval) {

				// If this interval needs to be in a group ...

				if (int_accept_fcn.accept_source (current_interval)) {

					final double t_lo = a_interval_time[current_interval];
					final double t_hi = a_interval_time[current_interval + 1];

					// Create the span for this interval

					if (interval_span == null) {
						interval_span = new Span();
					}
					interval_span.set_for_interval (t_lo, t_hi, span_width_fcn);

					// Done

					return;
				}

				// Otherwise, mark interval as not belonging to any group

				a_interval_group[current_interval] = -1;
			}

			// No more intervals

			interval_span = null;
			return;
		}


		// Finish the current group, if there is one.

		private final void finish_group () {
			if (group_span != null) {

				// Update rupture and interval counts

				total_n_rup += group_span.get_n_rup();
				total_n_int += group_span.get_n_int();

				// Add group span to the list

				group_span_list.add (group_span);
				group_span = null;
			}
			return;
		}


		// Add the current rupture to the current group.

		private final void add_rupture_to_group () {

			// If there is no current group, start one

			if (group_span == null) {
				group_span = rupture_span;
				rupture_span = null;
			}

			// Otherwise, attempt to merge into the current group

			else if (!( group_span.merge_left (rupture_span, span_width_fcn, width_teps) )) {

				// Merge failed, finish the current group and start a new group

				finish_group();
				group_span = rupture_span;
				rupture_span = null;
			}

			// Record the group number for this rupture (must be done after the attempted merge)

			a_rupture_group[current_rupture] = group_span_list.size();

			// Advance to the next rupture

			next_rupture();
			return;
		}


		// Add the current interval to the current group.

		private final void add_interval_to_group () {

			// If there is no current group, start one

			if (group_span == null) {
				group_span = interval_span;
				interval_span = null;
			}

			// Otherwise, attempt to merge into the current group

			else if (!( group_span.merge_left (interval_span, span_width_fcn, width_teps) )) {

				// Merge failed, finish the current group and start a new group

				finish_group();
				group_span = interval_span;
				interval_span = null;
			}

			// Record the group number for this interval (must be done after the attempted merge)

			a_interval_group[current_interval] = group_span_list.size();

			// Advance to the next interval

			next_interval();
			return;
		}


		// Finish processing the list of groups

		private final void finish_all_groups () {

			// If there is a final group, finish it

			finish_group();

			// Number of groups

			group_count = group_span_list.size();
			final int gcm1 = group_count - 1;

			// Allocate per-group storage

			a_group_time = new double[group_count];
			a_group_span = new Span[group_count];

			// Loop over groups in reverse order of construction, which makes them in increasing order of time

			total_n_rup = 0;
			total_n_int = 0;

			for (int i_grp = 0; i_grp < group_count; ++i_grp) {
				Span span = group_span_list.get (gcm1 - i_grp);

				// Save the span

				a_group_span[i_grp] = span;

				// Update the counts of ruptures and intervals

				total_n_rup += span.get_n_rup();
				total_n_int += span.get_n_int();

				// Save the group time

				a_group_time[i_grp] = span.get_center();
			}

			// Reverse rupture group numbers

			for (int i_rup = 0; i_rup < a_rupture_group.length; ++i_rup) {
				if (a_rupture_group[i_rup] >= 0) {
					a_rupture_group[i_rup] = gcm1 - a_rupture_group[i_rup];
				}
			}

			// Reverse interval group numbers

			for (int i_int = 0; i_int < a_interval_group.length; ++i_int) {
				if (a_interval_group[i_int] >= 0) {
					a_interval_group[i_int] = gcm1 - a_interval_group[i_int];
				}
			}

			// Get bracketing range of ruptures

			group_rup_begin = 0;
			group_rup_end = a_rupture_group.length;
			while (group_rup_begin < group_rup_end && a_rupture_group[group_rup_end - 1] < 0) {
				--group_rup_end;
			}
			while (group_rup_begin < group_rup_end && a_rupture_group[group_rup_begin] < 0) {
				++group_rup_begin;
			}

			// Get bracketing range of intervals

			group_int_begin = 0;
			group_int_end = a_interval_group.length;
			while (group_int_begin < group_int_end && a_interval_group[group_int_end - 1] < 0) {
				--group_int_end;
			}
			while (group_int_begin < group_int_end && a_interval_group[group_int_begin] < 0) {
				++group_int_begin;
			}

			// Done

			return;
		}


		// Build the groups.

		public final void do_build () {

			// Allocate arrays for group numbers

			int interval_count = 0;
			if (a_interval_time != null && a_interval_time.length > 0) {
				interval_count = a_interval_time.length - 1;
			}

			a_rupture_group = new int[a_rupture_obj.length];
			a_interval_group = new int[interval_count];

			// Initialize working storage

			current_rupture = a_rupture_obj.length;
			current_interval = interval_count;
			rupture_span = null;
			interval_span = null;
			largest_mag = NO_MAG_NEG;
			group_span = null;
			group_span_list = new ArrayList<Span>();

			// Find the first rupture and interval to process

			next_rupture();
			next_interval();

			// Loop while there is more to do ...

			while (rupture_span != null || interval_span != null) {

				// If we have a rupture, and there is either no interval or the interval is earlier, add the rupture

				if (rupture_span != null && (interval_span == null || rupture_span.get_center() >= interval_span.get_center())) {
					add_rupture_to_group();
				}

				// Otherwise, add the interval

				else {
					add_interval_to_group();
				}
			}

			// Finish up

			finish_all_groups();

			// Check invariant

			String s_inv = check_invariant();
			if (s_inv != null) {
				throw new InvariantViolationException ("OEDisc2Grouping.Builder.do_build : Invariant violation : " + s_inv);
			}

			return;
		}


		// Set up.

		public final void setup (
			OERupture[] a_rupture_obj,
			double[] a_interval_time,
			SpanWidthFcn span_width_fcn,
			RupWidthFcn rup_width_fcn,
			AcceptSrcFcn rup_accept_fcn,
			AcceptSrcFcn int_accept_fcn,
			double width_teps
		) {
			this.a_rupture_obj = a_rupture_obj;
			this.a_interval_time = a_interval_time;
			this.span_width_fcn = span_width_fcn;
			this.rup_width_fcn = rup_width_fcn;
			this.rup_accept_fcn = rup_accept_fcn;
			this.int_accept_fcn = int_accept_fcn;
			this.width_teps = width_teps;

			return;
		}

	}




	// Class for building the grouping.
	// This version builds the list of groups in the forward direction, from first to last.

	private class FwdBuilder {

		//--- Parameters

		// List of ruptures, in order of increasing time.
		// The length is rupture_count.
		// a_rupture_obj[n].t_day is the rupture time, in days.
		// a_rupture_obj[n].rup_mag is the rupture magnitude.
		// a_rupture_obj[n].k_prod is not used.
		// a_rupture_obj[n].rup_parent is not used.
		// a_rupture_obj[n].x_km is the rupture x-ccordinate, in km (currently zero).
		// a_rupture_obj[n].y_km is the rupture y-ccordinate, in km (currently zero).

		private OERupture[] a_rupture_obj;

		// Begin and end times for each interval, in days.
		// The length is interval_count + 1.
		// a_interval_time[n] is the begin time of interval n.
		// a_interval_time[n + 1] is the end time of interval n.
		// a_interval_time[0] is the beginning of the time range.
		// a_interval_time[interval_count] is the ending of the time range.
		// Note: If there are no intervals, this can be null, or an array with zero or one elements.

		private double[] a_interval_time;

		// Span width function, which gives the maximum width of a span.

		private SpanWidthFcn span_width_fcn;

		// Rupture width function, which gives the width to assign to a rupture.

		private RupWidthFcn rup_width_fcn;

		// Rupture accept function, which tells which ruptures to include.

		private AcceptSrcFcn rup_accept_fcn;

		// Interval accept function, which tells which intervals to include.

		private AcceptSrcFcn int_accept_fcn;

		// Time epsilon to use for width calculations.

		private double width_teps;


		//--- Working storage

		// The number of ruptures.

		private int rupture_count;

		// The number of intervals.

		private int interval_count;

		// Flag indicated if each rupture is accepted.

		private boolean[] a_rupture_accept;

		// Width of each rupture, for accepted ruptures.

		private double[] a_rupture_width;

		// Current rupture, should be initialized to -1.

		private int current_rupture;

		// Current interval, should be initialized to -1.

		private int current_interval;

		// Span for current rupture, null if none.

		private Span rupture_span;

		// Span for current interval, null if none.

		private Span interval_span;

		// Span for current group, null if none.

		private Span group_span;

		// List of group spans constructed so far, in order of construction (from last to first).

		private ArrayList<Span> group_span_list;


		//--- Subroutines


		// Advance to next rupture, and set up the current rupture span.

		private final void next_rupture () {

			// Loop over ruptures, in ascending order ...

			for (++current_rupture; current_rupture < rupture_count; ++current_rupture) {

				// If this rupture needs to be in a group ...

				if (a_rupture_accept[current_rupture]) {

					final double t = a_rupture_obj[current_rupture].t_day;

					// Create the span for this rupture

					if (rupture_span == null) {
						rupture_span = new Span();
					}
					rupture_span.set_for_rupture (t, a_rupture_width[current_rupture]);

					// Done

					return;
				}

				// Otherwise, mark rupture as not belonging to any group

				a_rupture_group[current_rupture] = -1;
			}

			// No more ruptures

			rupture_span = null;
			return;
		}


		// Advance to next interval, and set up the current interval span.

		private final void next_interval () {

			// Loop over intervals, in ascending order ...

			for (++current_interval; current_interval < interval_count; ++current_interval) {

				// If this interval needs to be in a group ...

				if (int_accept_fcn.accept_source (current_interval)) {

					final double t_lo = a_interval_time[current_interval];
					final double t_hi = a_interval_time[current_interval + 1];

					// Create the span for this interval

					if (interval_span == null) {
						interval_span = new Span();
					}
					interval_span.set_for_interval (t_lo, t_hi, span_width_fcn);

					// Done

					return;
				}

				// Otherwise, mark interval as not belonging to any group

				a_interval_group[current_interval] = -1;
			}

			// No more intervals

			interval_span = null;
			return;
		}


		// Finish the current group, if there is one.

		private final void finish_group () {
			if (group_span != null) {

				// Update rupture and interval counts

				total_n_rup += group_span.get_n_rup();
				total_n_int += group_span.get_n_int();

				// Add group span to the list

				group_span_list.add (group_span);
				group_span = null;
			}
			return;
		}


		// Add the current rupture to the current group.

		private final void add_rupture_to_group () {

			// If there is no current group, start one

			if (group_span == null) {
				group_span = rupture_span;
				rupture_span = null;
			}

			// Otherwise, attempt to merge into the current group

			else if (!( group_span.merge_right (rupture_span, span_width_fcn, width_teps) )) {

				// Merge failed, finish the current group and start a new group

				finish_group();
				group_span = rupture_span;
				rupture_span = null;
			}

			// Record the group number for this rupture (must be done after the attempted merge)

			a_rupture_group[current_rupture] = group_span_list.size();

			// Advance to the next rupture

			next_rupture();
			return;
		}


		// Add the current interval to the current group.

		private final void add_interval_to_group () {

			// If there is no current group, start one

			if (group_span == null) {
				group_span = interval_span;
				interval_span = null;
			}

			// Otherwise, attempt to merge into the current group

			else if (!( group_span.merge_right (interval_span, span_width_fcn, width_teps) )) {

				// Merge failed, finish the current group and start a new group

				finish_group();
				group_span = interval_span;
				interval_span = null;
			}

			// Record the group number for this interval (must be done after the attempted merge)

			a_interval_group[current_interval] = group_span_list.size();

			// Advance to the next interval

			next_interval();
			return;
		}


		// Finish processing the list of groups

		private final void finish_all_groups () {

			// If there is a final group, finish it

			finish_group();

			// Number of groups

			group_count = group_span_list.size();

			// Allocate per-group storage

			a_group_time = new double[group_count];
			a_group_span = new Span[group_count];

			// Loop over groups in order of construction, which makes them in increasing order of time

			total_n_rup = 0;
			total_n_int = 0;

			for (int i_grp = 0; i_grp < group_count; ++i_grp) {
				Span span = group_span_list.get (i_grp);

				// Save the span

				a_group_span[i_grp] = span;

				// Update the counts of ruptures and intervals

				total_n_rup += span.get_n_rup();
				total_n_int += span.get_n_int();

				// Save the group time

				a_group_time[i_grp] = span.get_center();
			}

			// Get bracketing range of ruptures

			group_rup_begin = 0;
			group_rup_end = a_rupture_group.length;
			while (group_rup_begin < group_rup_end && a_rupture_group[group_rup_end - 1] < 0) {
				--group_rup_end;
			}
			while (group_rup_begin < group_rup_end && a_rupture_group[group_rup_begin] < 0) {
				++group_rup_begin;
			}

			// Get bracketing range of intervals

			group_int_begin = 0;
			group_int_end = a_interval_group.length;
			while (group_int_begin < group_int_end && a_interval_group[group_int_end - 1] < 0) {
				--group_int_end;
			}
			while (group_int_begin < group_int_end && a_interval_group[group_int_begin] < 0) {
				++group_int_begin;
			}

			// Done

			return;
		}


		// Build the groups.

		public final void do_build () {

			// Allocate arrays for group numbers

			rupture_count = a_rupture_obj.length;
			interval_count = 0;
			if (a_interval_time != null && a_interval_time.length > 0) {
				interval_count = a_interval_time.length - 1;
			}

			a_rupture_group = new int[rupture_count];
			a_interval_group = new int[interval_count];

			// Initialize working storage

			a_rupture_accept = new boolean[rupture_count];
			a_rupture_width = new double[rupture_count];
			current_rupture = -1;
			current_interval = -1;
			rupture_span = null;
			interval_span = null;
			group_span = null;
			group_span_list = new ArrayList<Span>();

			// Set up arrays of rupture accept and width, scanning from last to first

			double largest_mag = NO_MAG_NEG;

			for (int i_rup = rupture_count - 1; i_rup >= 0; --i_rup) {
				a_rupture_accept[i_rup] = rup_accept_fcn.accept_source (i_rup);

				// If accepting this rupture ...

				if (a_rupture_accept[i_rup]) {

					final double t = a_rupture_obj[i_rup].t_day;
					final double mag = a_rupture_obj[i_rup].rup_mag;

					// Update the largest magnitude

					largest_mag = Math.max (largest_mag, mag);

					// Get the width for this rupture

					final double max_width = span_width_fcn.get_max_width (t);
					a_rupture_width[i_rup] = rup_width_fcn.get_rup_width (t, mag, largest_mag, max_width);
				}
			}

			// Find the first rupture and interval to process

			next_rupture();
			next_interval();

			// Loop while there is more to do ...

			while (rupture_span != null || interval_span != null) {

				// If we have a rupture, and there is either no interval or the interval is later, add the rupture

				if (rupture_span != null && (interval_span == null || rupture_span.get_center() <= interval_span.get_center())) {
					add_rupture_to_group();
				}

				// Otherwise, add the interval

				else {
					add_interval_to_group();
				}
			}

			// Finish up

			finish_all_groups();

			// Check invariant

			String s_inv = check_invariant();
			if (s_inv != null) {
				throw new InvariantViolationException ("OEDisc2Grouping.FwdBuilder.do_build : Invariant violation : " + s_inv);
			}

			return;
		}


		// Set up.

		public final void setup (
			OERupture[] a_rupture_obj,
			double[] a_interval_time,
			SpanWidthFcn span_width_fcn,
			RupWidthFcn rup_width_fcn,
			AcceptSrcFcn rup_accept_fcn,
			AcceptSrcFcn int_accept_fcn,
			double width_teps
		) {
			this.a_rupture_obj = a_rupture_obj;
			this.a_interval_time = a_interval_time;
			this.span_width_fcn = span_width_fcn;
			this.rup_width_fcn = rup_width_fcn;
			this.rup_accept_fcn = rup_accept_fcn;
			this.int_accept_fcn = int_accept_fcn;
			this.width_teps = width_teps;

			return;
		}

	}




	//----- Grouping functions -----




	// Display our contents.
	// Displays the first 20 groups.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2Grouping:" + "\n");

		result.append ("group_count = " + group_count + "\n");
		result.append ("rupture_count = " + a_rupture_group.length + "\n");
		result.append ("group_rup_begin = " + group_rup_begin + "\n");
		result.append ("group_rup_end = " + group_rup_end + "\n");
		result.append ("interval_count = " + a_interval_group.length + "\n");
		result.append ("group_int_begin = " + group_int_begin + "\n");
		result.append ("group_int_end = " + group_int_end + "\n");
		result.append ("total_n_rup = " + total_n_rup + "\n");
		result.append ("total_n_int = " + total_n_int + "\n");

		result.append ("a_group_span:" + "\n");
		int g_count = Math.min (20, group_count);
		for (int i_grp = 0; i_grp < g_count; ++i_grp) {
			result.append (a_group_span[i_grp].toString() + "\n");
		}
		if (g_count < group_count) {
			result.append ("... and " + (group_count - g_count) + " more groups" + "\n");
		}

		return result.toString();
	}




	// Display a summary of our contents.
	// Displays the first 20 groups.

	public String summary_string() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2Grouping:" + "\n");

		result.append ("group_count = " + group_count + "\n");
		result.append ("rupture_count = " + a_rupture_group.length + "\n");
		result.append ("group_rup_begin = " + group_rup_begin + "\n");
		result.append ("group_rup_end = " + group_rup_end + "\n");
		result.append ("interval_count = " + a_interval_group.length + "\n");
		result.append ("group_int_begin = " + group_int_begin + "\n");
		result.append ("group_int_end = " + group_int_end + "\n");
		result.append ("total_n_rup = " + total_n_rup + "\n");
		result.append ("total_n_int = " + total_n_int + "\n");

		return result.toString();
	}




	// Display our full contents.
	// Warning: This can be quite large.

	public String dump_string() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2Grouping:" + "\n");

		result.append ("group_count = " + group_count + "\n");
		result.append ("rupture_count = " + a_rupture_group.length + "\n");
		result.append ("group_rup_begin = " + group_rup_begin + "\n");
		result.append ("group_rup_end = " + group_rup_end + "\n");
		result.append ("interval_count = " + a_interval_group.length + "\n");
		result.append ("group_int_begin = " + group_int_begin + "\n");
		result.append ("group_int_end = " + group_int_end + "\n");
		result.append ("total_n_rup = " + total_n_rup + "\n");
		result.append ("total_n_int = " + total_n_int + "\n");

		result.append ("a_group_span:" + "\n");
		for (int i_grp = 0; i_grp < group_count; ++i_grp) {
			result.append (a_group_span[i_grp].toString() + "\n");
		}

		result.append ("a_rupture_group:");
		for (int i_rup = 0; i_rup < a_rupture_group.length; ++i_rup) {
			result.append ((i_rup % 20 == 0) ? "\n" : " ");
			result.append (a_rupture_group[i_rup]);
		}
		result.append ("\n");

		result.append ("a_interval_group:");
		for (int i_int = 0; i_int < a_interval_group.length; ++i_int) {
			result.append ((i_int % 20 == 0) ? "\n" : " ");
			result.append (a_interval_group[i_int]);
		}
		result.append ("\n");

		return result.toString();
	}




	// Create an empty grouping.

	public final void clear () {
		group_count = 0;
		a_group_time = new double[0];
		a_rupture_group = new int[0];
		group_rup_begin = 0;
		group_rup_end = 0;
		a_interval_group = new int[0];
		group_int_begin = 0;
		group_int_end = 0;

		a_group_span = new Span[0];
		total_n_rup = 0;
		total_n_int = 0;

		return;
	}




	// Constructor.

	public OEDisc2Grouping () {
		clear();
	}




	// Get the default span width function.
	// Note: The returned function expects to get time relative to some start time.

	public static SpanWidthFcn get_default_span_width_fcn () {
		return new SpanWidthFcnRatio (DEF_GS_REL_BASE_TIME, DEF_GS_RATIO, DEF_GS_MIN_WIDTH);
	}




	// Get the default rupture width function.

	public static RupWidthFcn get_default_rup_width_fcn () {
		return new RupWidthFcnTaper (DEF_GR_LO_RATIO, DEF_GR_HI_RATIO, DEF_GR_HI_MAG_DELTA, DEF_GR_TAPER_MAG_DELTA, DEF_GR_INIT_MAG);
	}




	// Build the grouping.
	// Parameters:
	//  a_rupture_obj = List of ruptures, in order of increasing time, length = rupture_count.
	//  a_interval_time = Begin and end times for each interval, in days, length = interval_count + 1.
	//  span_width_fcn = Span width function, which gives the maximum width of a span.
	//  rup_width_fcn = Rupture width function, which gives the width to assign to a rupture.
	//  rup_accept_fcn = Rupture accept function, which tells which ruptures to include.
	//  int_accept_fcn = Interval accept function, which tells which intervals to include.
	// Returns this object.
	// Note: The number of ruptures is implied by the length of a_rupture_obj.
	// Note: The number of intervals is implied by the length a_interval_time.  If there are no intervals,
	// then a_interval_time can be null, or an array with zero or one elements.

	public OEDisc2Grouping build_grouping (
		OERupture[] a_rupture_obj,
		double[] a_interval_time,
		SpanWidthFcn span_width_fcn,
		RupWidthFcn rup_width_fcn,
		AcceptSrcFcn rup_accept_fcn,
		AcceptSrcFcn int_accept_fcn
	) {

		// Create the builder

		FwdBuilder builder = new FwdBuilder();
		builder.setup (
			a_rupture_obj,
			a_interval_time,
			span_width_fcn,
			rup_width_fcn,
			rup_accept_fcn,
			int_accept_fcn,
			GROUP_TIME_EPS
		);

		// Build the grouping

		builder.do_build();
		return this;
	}


	// Build scanning in the forward direction, from first to last.

	public OEDisc2Grouping build_grouping_fwd (
		OERupture[] a_rupture_obj,
		double[] a_interval_time,
		SpanWidthFcn span_width_fcn,
		RupWidthFcn rup_width_fcn,
		AcceptSrcFcn rup_accept_fcn,
		AcceptSrcFcn int_accept_fcn
	) {

		// Create the builder

		FwdBuilder builder = new FwdBuilder();
		builder.setup (
			a_rupture_obj,
			a_interval_time,
			span_width_fcn,
			rup_width_fcn,
			rup_accept_fcn,
			int_accept_fcn,
			GROUP_TIME_EPS
		);

		// Build the grouping

		builder.do_build();
		return this;
	}


	// Build scanning in the reverse direction, from last to first.

	public OEDisc2Grouping build_grouping_rev (
		OERupture[] a_rupture_obj,
		double[] a_interval_time,
		SpanWidthFcn span_width_fcn,
		RupWidthFcn rup_width_fcn,
		AcceptSrcFcn rup_accept_fcn,
		AcceptSrcFcn int_accept_fcn
	) {

		// Create the builder

		Builder builder = new Builder();
		builder.setup (
			a_rupture_obj,
			a_interval_time,
			span_width_fcn,
			rup_width_fcn,
			rup_accept_fcn,
			int_accept_fcn,
			GROUP_TIME_EPS
		);

		// Build the grouping

		builder.do_build();
		return this;
	}




	// Build the grouping from a history.
	// Parameters:
	//  history = Discretized rupture history.
	//  int_mc_thresh = Interval magnitude of completeness threshold.
	//                  Intervals are included if their magnitude of completeness is > int_mc_thresh.
	//                  Can be >= NO_MAG_POS to include no intervals.
	//                  Can be <= NO_MAG_NEG to include all intervals.
	//  span_width_fcn = Span width function, which gives the maximum width of a span.
	//                   Time in span_width_fcn is relative to the end of the history.
	//                   Can be null to use default.
	//  rup_width_fcn = Rupture width function, which gives the width to assign to a rupture.
	//                  Can be null to use default.
	// Returns this object.
	// Implementation note: This function increases int_mc_thresh slightly to ensure intervals are
	// included only if their magnitude of completeness is definitely larger than the supplied threshold.

	public OEDisc2Grouping build_grouping_from_history (
		OEDisc2History history,
		double int_mc_thresh,
		SpanWidthFcn span_width_fcn,
		RupWidthFcn rup_width_fcn
	) {

		// Ranges of ruptures and intervals in the history (compare to OEDisc2ExtFit.dfit_build())

		int main_rup_begin = 0;
		int targ_rup_end = history.i_inside_end;

		int like_int_begin = 0;
		int like_int_end = history.interval_count;

		// Rupture acceptance function

		AcceptSrcFcn rup_accept_fcn = new AcceptSrcFcnRange (main_rup_begin, targ_rup_end);

		// Interval acceptance function

		AcceptSrcFcn int_accept_fcn;
		if (int_mc_thresh >= NO_MAG_POS_CHECK) {
			int_accept_fcn = new AcceptSrcFcnNone();
		}
		else if (int_mc_thresh <= NO_MAG_NEG_CHECK) {
			int_accept_fcn = new AcceptSrcFcnAll();
		}
		else {
			int_accept_fcn = new AcceptSrcFcnRangeLevel (like_int_begin, like_int_end, history.a_interval_mc, int_mc_thresh + TINY_MAG_DELTA);
		}

		// Make the span width function, apply default, and shift to end of history

		SpanWidthFcn my_span_width_fcn = span_width_fcn;
		if (my_span_width_fcn == null) {
			my_span_width_fcn = get_default_span_width_fcn();
		}

		SpanWidthFcn shifted_span_width_fcn = new SpanWidthFcnShift (my_span_width_fcn, history.a_interval_time[like_int_end]);

		// Make the rupture width function, apply default

		RupWidthFcn my_rup_width_fcn = rup_width_fcn;
		if (my_rup_width_fcn == null) {
			my_rup_width_fcn = get_default_rup_width_fcn();
		}

		// Build the grouping

		build_grouping (
			history.a_rupture_obj,
			history.a_interval_time,
			shifted_span_width_fcn,
			my_rup_width_fcn,
			rup_accept_fcn,
			int_accept_fcn
		);

		return this;
	}




	// Calculate the appropriate interval magnitude of completeness threshold.
	// Parameters:
	//  magCat = Catalog magnitude of completeness, typically from the history.
	//  mag_min_sim = Minimum magnitude that will be used for simulations.
	//  f_intervals = True to use intervals to fill in below magnitude of completeness.
	//  lmr_opt = Option to select magnitude range for log-likelihood calculation (LMR_OPT_XXXX).
	// Returns a threshold that can be used in build_grouping_from_history.
	// This function uses parameters that are utilized by OEDisc2ExtFit.

	public static double calc_int_mc_thresh (double magCat, double mag_min_sim, boolean f_intervals, int lmr_opt) {
		double int_mc_thresh = NO_MAG_POS;
		if (f_intervals) {

			switch (lmr_opt) {

			default:
				throw new IllegalArgumentException ("OEDisc2Grouping.calc_int_mc_thresh - Invalid likelihood magnitude range option: lmr_opt = " + lmr_opt);
			
			case LMR_OPT_MCT_INFINITY:
			case LMR_OPT_MCT_MAG_MAX:
				// Lower limit is time-dependent magnitude of completeness
				int_mc_thresh = mag_min_sim;
				break;
			
			case LMR_OPT_MAGCAT_INFINITY:
			case LMR_OPT_MAGCAT_MAG_MAX:
				// Lower limit is catalog magnitude of completeness
				if (mag_min_sim + TINY_MAG_DELTA < magCat) {
					int_mc_thresh = NO_MAG_NEG;
				}
				break;
			}
		}
		return int_mc_thresh;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 110001;

	private static final String M_VERSION_NAME = "OEDisc2Grouping";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt         ("group_count"     , group_count     );
			writer.marshalDoubleArray ("a_group_time"    , a_group_time    );
			writer.marshalIntArray    ("a_rupture_group" , a_rupture_group );
			writer.marshalInt         ("group_rup_begin" , group_rup_begin );
			writer.marshalInt         ("group_rup_end"   , group_rup_end   );
			writer.marshalIntArray    ("a_interval_group", a_interval_group);
			writer.marshalInt         ("group_int_begin" , group_int_begin );
			writer.marshalInt         ("group_int_end"   , group_int_end   );
			writer.marshalInt         ("total_n_rup"     , total_n_rup     );
			writer.marshalInt         ("total_n_int"     , total_n_int     );

			writer.marshalArrayBegin ("a_group_span", group_count);
			for (int i = 0; i < group_count; ++i) {
				a_group_span[i].marshal (writer, null);
			}
			writer.marshalArrayEnd ();

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			group_count  = reader.unmarshalInt          ("group_count"     );
			a_group_time = reader.unmarshalDoubleArray  ("a_group_time"    );
			a_rupture_group = reader.unmarshalIntArray  ("a_rupture_group" );
			group_rup_begin  = reader.unmarshalInt      ("group_rup_begin" );
			group_rup_end  = reader.unmarshalInt        ("group_rup_end"   );
			a_interval_group = reader.unmarshalIntArray ("a_interval_group");
			group_int_begin  = reader.unmarshalInt      ("group_int_begin" );
			group_int_end  = reader.unmarshalInt        ("group_int_end"   );
			total_n_rup  = reader.unmarshalInt          ("total_n_rup"     );
			total_n_int  = reader.unmarshalInt          ("total_n_int"     );

			int n = reader.unmarshalArrayBegin ("a_group_span");
			if (n != group_count) {
				throw new MarshalException ("OEDisc2Grouping.do_umarshal: Incorroect a_group_span array length: got " + n + ", expected " + group_count);
			}
			a_group_span = new Span[group_count];
			for (int i = 0; i < group_count; ++i) {
				a_group_span[i] = (new Span()).unmarshal (reader, null);
			}
			reader.unmarshalArrayEnd ();

		}
		break;

		}

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

	public OEDisc2Grouping unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDisc2Grouping grouping) {
		writer.marshalMapBegin (name);
		grouping.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEDisc2Grouping static_unmarshal (MarshalReader reader, String name) {
		OEDisc2Grouping grouping = new OEDisc2Grouping();
		reader.unmarshalMapBegin (name);
		grouping.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return grouping;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEDisc2Grouping : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OEDisc2Grouping : Unrecognized subcommand : " + args[0]);
		return;

	}

}
