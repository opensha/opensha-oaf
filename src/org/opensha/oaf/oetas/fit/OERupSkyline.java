package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;


/**
 * Skyline function construction for a series of earthquakes.
 * Author: Michael Barall 04/15/2020.
 *
 * Assume for each rupture a time T(rup) (typically the time the rupture occurred)
 * satisfying t_min <= T(rup) <= t_max, and a function F(t; rup) valid for times t > T(rup)
 * satisfying a certain condition.
 *
 * Assume also an "initial rupture" init_rup, with a function F(t; init_rup) valid for t > t_min.
 * This may not be an actual earthquake, e.g., F(t; init_rup) might simply be a constant.
 *
 * The required condition is:  Consider two ruptures, rup_1 and rup_2, with T(rup_1) <= T(rup_2).
 * Then, there is a time E(rup_2), referred to as the expiration time, satisfying:
 *
 *   T(rup_2) <= E(rup_2) <= t_max
 *
 *   F(t; rup_1) <= F(t; rup_2)   for    T(rup_2) < t <= E(rup_2)
 *
 *   F(t; rup_1) >= F(t; rup_2)   for    E(rup_2) < t <= t_max
 *
 * In other words, there can be an interval of time from T(rup_2) to E(rup_2), in which the
 * function for rup_2 exceeds (or equals) the function for rup_1.  But afterwards, the
 * function for rup_2 is less than (or equals) the function for rup_1.  This sort of behavior
 * occurs for functions that spike immediately after an earthquake and then settle down.
 *
 * Note that despite the notation, E(rup_2) implicitly depends on the choice of rup_1.
 *
 * The skyline function is the function S(t) = max F(t; rup) where the maximum runs over all
 * ruptures with T(rup) < t.
 *
 * The skyline function has this form:  There is a sequence of times
 *   t_min == t_0 < t_1 < t_2 < ... < t_n < t_(n+1) == t_max
 * and a sequence of ruptures
 *   rup_0, rup_1, ... , rup_n
 * such that
 *   S(t) = F(t, rup_i)   for   t_i < t <= t_(i+1)
 *
 * Notice that time intervals are defined to be open on the left, closed on the right.
 * This is so evaluating S(t) exactly at the time of an earthquake obtains the function
 * value in effect immediately before the earthquake.  It also means that a function
 * value does not need to be defined precisely at the time an earthquake occurs.
 *
 * ============================================================================================
 *
 * A rupture is represented by an object of type TRupture, which is an opaque type.
 * Since the initial rupture may not be representable by an object of type TRupture,
 * it is permitted (but not required) that null can be used to represent the initial
 * rupture.  All other ruptures must be non-null.
 *
 * The subclass must provide two operations on ruptures.  Method hook_calc_t_expire()
 * calculates E(rup_2) as described above, and stores the result within the TRupture
 * object, given rup_1 and rup_2.  Method hook_get_t_expire() retrieves the stored
 * value of E(rup_2).
 *
 * To construct the skyline function, the subclass must first call setup() to supply
 * the time range and initial rupture.  Then, the subclass can call advance_time() to
 * supply a sequence of times, which must be in (non-strictly) increasing order.
 * After each call to advance_time(), the subclass can call get_current_rup() to
 * obtain the rupture that has the largest function value at the current time, and/or
 * call supply_rup() to supply a rupture whose time T(rup) is the current time.
 * Finally, after supplying all ruptures, the subclass must call finish_up() to complete
 * the skyline function.
 *
 * The skyline function is provided to the subclass via the method hook_report_interval().
 * Each call to hook_report_interval() provides one of the intervals defining the
 * skyline function, in increasing order.
 *
 * ============================================================================================
 *
 * This class works by maintaining a stack, which contains the rupture that currently
 * has the largest function value, and all the ruptures that might have the largest
 * function value at some time in the future.
 *
 * The top element of the stack is in field current_rup, and the bottom element of the
 * stack is in field baseline_rup.  Intermediate elements are in the deque rup_stack.
 * The stack always contains at least one element, and if it contains exactly one
 * element then current_rup and baseline_rup both contain that element (and rup_stack
 * is empty).
 *
 * (Because the current and baseline ruptures can be null, they cannot be stored in the
 * deque as the deque does not permit null elements, so they are kept in fields.)
 *
 * Proceeding from the top to the bottom of the stack, function values F(t; rup) are
 * (non-strictly) decreasing, while expiration times E(rup) are strictly increasing.  The
 * baseline rupture implicitly has E(rup) == t_max, therefore all other ruptures on the
 * stack have E(rup) < t_max.  So, if time is advanced without supplying any additional
 * ruptures, then each rupture on the stack will contribute one interval to the skyline
 * function, proceeding from top to bottom, with the expiration times E(rup) being the interval
 * endpoints.
 *
 */
public abstract class OERupSkyline<TRupture> {


	//----- Data structures -----




	// The minimum and maximum times for the skyline.

	private double sky_t_min;
	private double sky_t_max;

	// Get the minimum time.

	protected double get_sky_t_min () {
		return sky_t_min;
	}

	// Get the maximum time.

	protected double get_sky_t_max () {
		return sky_t_max;
	}




	// The initial rupture, established at setup time.  It can be null.
	// The initial rupture represents the initial function.

	private TRupture initial_rup;

	// Get the initial rupture.  It can be null.

	protected TRupture get_initial_rup () {
		return initial_rup;
	}

	// Return true if the given rupture is the initial rupture.

	protected boolean is_initial_rup (TRupture rup) {
		return rup == initial_rup;
	}




	// The baseline rupture.  It can be null if and only if the initial rupture is null.
	// Initially it equals initial_rup, but can change.
	// The baseline rupture represents a function extending to the end of all considered times.
	// It is the rupture at the bottom of the stack.

	private TRupture baseline_rup;

	// Get the baseline rupture.  It can be null.

	protected TRupture get_baseline_rup () {
		return baseline_rup;
	}

	// Return true if the given rupture is the baseline rupture.

	protected boolean is_baseline_rup (TRupture rup) {
		return rup == baseline_rup;
	}




	// The current rupture.  It can be null if and only if the baseline rupture is null.
	// This is the rupture which has the greatest function value at the current time.
	// It is the rupture at the top of the stack.

	private TRupture current_rup;

	// Get the current rupture.  It can be null if and only if the baseline rupture is null.

	protected TRupture get_current_rup () {
		return current_rup;
	}




	// The current time.  The units and origin are not specified in this class.

	private double current_time;

	// Get the current time.

	protected double get_current_time () {
		return current_time;
	}




	// The time at the start of the current interval.  It is the time at which
	// the current rupture became the rupture with largest function value.

	private double interval_time;

	// Get the interval time.

	protected double get_interval_time () {
		return interval_time;
	}




	// Stack of ruptures that are active at the current time.
	// Elements are pushed and popped from the beginning of the deque.
	// This deque together with current_rup and baseline_rup forms a stack, with current_rup at the top.
	// From top to bottom, each element has greater-or-equal function value at the current time,
	// and strictly earlier expiration time.

	private ArrayDeque<TRupture> rup_stack;




	//----- Hook functions -----


	// Report an interval.
	// Parameters:
	//  interval_t_lo = Beginning time of the interval.
	//  interval_t_hi = Ending time of the interval.
	//  rup = Rupture for the interval.  Can be null, if the baseline rupture is null.
	// This call indicates that for times interval_t_lo < t <= interval_t_hi, the largest
	//  function value is the function F(t; rup) associated with the given rupture.
	// This is called with strictly increasing values of interval_t_lo.
	// It is guaranteed that interval_t_lo < interval_t_hi.

	protected abstract void hook_report_interval (double interval_t_lo, double interval_t_hi, TRupture rup);


	// Calculate the expiration time for the given rupture.
	// Parameters:
	//  cur_rup = The current rupture, which is the rupture that has the highest function value
	//            at the current time.  It can be null if and only if the initial rupture is null.
	//  rup = Rupture for which the expiration time is to be computed.
	// Returns the value of E(rup) which is defined by (where t_cur is the current time):
	//  F(t, rup) >= F(t, cur_rup)  for  t_cur < t <= E(rup)
	//  F(t, rup) <= F(t, cur_rup)  for  E(rup) < t <= min(t_max, E(cur_rup))
	// In other words, the return value is the endpoint of the interval, starting at the
	//  current time, in which the function associated with rup lies above (or equal to)
	//  the function associated with cur_rup.
	// The following applies:
	//  * If E(rup) <= t_cur, then rup is discarded as it can never be the rupture with
	//    the largest function value (though possibly it could be tied).  Note that any
	//    return value <= t_cur has the same effect, so if the routine determines this
	//    condition holds then it can return any convenient value <= tcur, for example, t_min.
	//  * Else, if E(rup) >= min(t_max, E(cur_rup)), then cur_rup is discarded as it
	//    cannot be the rupture with the largest function value at any time after t_cur.
	//    In this case, the stack is popped, a new current rupture is established, and
	//    this call is repeated.  Note that any return value >= min(t_max, E(cur_rup))
	//    has the same effect, and the return value is not used again, so a precise value is
	//    not required, and any value satisfying this condition will do, for example, t_max.
	//  * Else, t_cur < E(rup) < min(t_max, E(cur_rup)).  In this case, rup is pushed
	//    onto the stack and becomes the new current rupture.  When the time passes E(rup),
	//    rup is popped off the stack and cur_rup resumes being the current rupture
	//    (unless things change because of other intervening ruptures).  In this case,
	//    the value of E(rup) must be stored by the subclass so it can be retrieved again
	//    by calling hook_get_t_expire().

	protected abstract double hook_calc_t_expire (TRupture cur_rup, TRupture rup);


	// Get the expiration time for the given rupture.
	// Parameters:
	//  rup = Rupture from which to obtain the expiration time.
	// Returns the value of E(rup) calculated by the most recent call to hook_calc_t_expire().
	// Note: This routine is not called until after at least one call to hook_calc_t_expire().

	protected abstract double hook_get_t_expire (TRupture rup);


	// Notification that setup() has been called.
	// This is called at the end of setup().
	// This gives the subclass the opportunity to perform any initial actions.

	protected abstract void hook_setup ();


	// Notification that finish_up() has been called.
	// This is called at the end of finish_up().
	// This gives the subclass the opportunity to perform any final actions.

	protected abstract void hook_finish_up ();




	//----- Internal functions -----




	// Report an interval.
	// Parameters:
	//  interval_t_hi = Ending time of the interval.
	// This call indicates that for times interval_time < t <= interval_t_hi, the largest
	//  function value is the function F(t; current_rup) associated with the current rupture.
	// This call does not report zero-duration intervals.
	// This call also updates interval_time.

	protected final void report_interval (double interval_t_hi) {

		// If the current rupture has been largest for a non-zero interval of time ...

		if (interval_time < interval_t_hi) {
				
			// Report the interval

			hook_report_interval (interval_time, interval_t_hi, current_rup);

			// The current time becomes the beginning of the next interval

			interval_time = interval_t_hi;
		}

		return;
	}




	// Push a rupture onto the stack.
	// Parameters:
	//  rup = Rupture to push, cannot be null.
	// The given rupture becomes the new current rupture.

	protected final void push_stack (TRupture rup) {
	
		// If the current rupture is not the baseline rupture, push it onto the deque

		if (current_rup != baseline_rup) {
			rup_stack.addFirst (current_rup);
		}

		// The given rupture becomes the current rupture

		current_rup = rup;
		return;
	}




	// Put a rupture onto the stack, overwriting the current top of stack.
	// Parameters:
	//  rup = Rupture to poke, cannot be null.
	// The current rupture is discarded, and the given rupture becomes the new current rupture.

	protected final void poke_stack (TRupture rup) {
	
		// If the current rupture is the baseline rupture, the given rupture becomes the new baseline rupture

		if (current_rup == baseline_rup) {
			baseline_rup = rup;
		}

		// The given rupture becomes the current rupture

		current_rup = rup;
		return;
	}




	// Pop a rupture off the stack.
	// The current rupture is discarded, and the next rupture on the stack becomes the new current rupture.

	protected final void pop_stack () {
	
		// If the current rupture is the baseline rupture, error

		if (current_rup == baseline_rup) {
			throw new NoSuchElementException ("OERupSkyline.pop_stack - Attempt to pop baseline rupture");
		}

		// Remove element from deque

		current_rup = rup_stack.pollFirst();

		// If the deque was empty, it is the baseline rupture

		if (current_rup == null) {
			current_rup = baseline_rup;
		}

		return;
	}




	//----- Service functions -----




	// Set up to begin constructing the skyline function.
	// Parameters:
	//  t_min = Minimum time at which skyline function is required.
	//  t_max = Maximum time at which skyline function is required.
	//  init_rup = Initial rupture in effect at t_min.  Its function must be valid
	//             for all times with t_min <= t <= t_max.  It can be null.
	// Note: The initial rupture init_rup implicitly has E(init_rup) == t_max.
	//  This class will never call hook_get_t_expire(init_rup), so init_rup does
	//  not need to have the ability to store the value of E(init_rup).
	//  However, if init_rup is an "ordinary" object of class TRupture, then it is
	//  recommended that the stored value of E(init_rup) be initialized to a
	//  value >= t_max.  Calls of the form hook_calc_t_expire(init_rup, rup) will occur.

	protected void setup (double t_min, double t_max, TRupture init_rup) {

		// Validate and save the time interval
		
		if (!( t_min < t_max )) {
			throw new IllegalArgumentException ("OERupSkyline.setup - Invalid time interval: t_min = " + t_min + ", t_max = " + t_max);
		}

		sky_t_min = t_min;
		sky_t_max = t_max;

		// Save the initial rupture

		initial_rup = init_rup;

		// The initial rupture is the first baseline rupture

		baseline_rup = init_rup;

		// The initial rupture is the first current rupture

		current_rup = init_rup;

		// The initial current time is the minimum time

		current_time = t_min;

		// The start time of the initial interval is the minimum time

		interval_time = t_min;

		// Allocate an empty stack

		rup_stack = new ArrayDeque<TRupture>();

		// Notify the subclass

		hook_setup();

		return;
	}




	// Advance the current time.
	// Parameters:
	//  t = New current time.

	protected void advance_time (double t) {
	
		// Range check
		
		if (!( t >= current_time && t <= sky_t_max )) {
			throw new IllegalArgumentException ("OERupSkyline.advance_time - Invalid time: t = " + t + ", current_time = " + current_time + ", sky_t_max = " + sky_t_max);
		}

		// If the time is changing ...

		if (t > current_time) {

			// While the current rupture is not the baseline rupture ...

			while (current_rup != baseline_rup) {

				// Get the expiration time of the current rupture

				double t_expire = hook_get_t_expire (current_rup);

				// If the new time is not after the expiration time, nothing more to do

				if (t_expire >= t) {
					break;
				}

				// Report the interval if it is non-empty

				report_interval (t_expire);

				// Pop the stack, establishing a new current rupture

				pop_stack();
			}

			// Set the new current time

			current_time = t;
		}

		return;
	}




	// Supply a new rupture.
	// Parameters:
	//  rup = New rupture, cannot be null.
	// Returns true if the rupture is retained, meaning that there may be an interval with the rupture.
	// Returns false if the rupture is discarded, meaning the rup object may be re-used.
	// The function F(t; rup) is presumed valid for t > current_time.

	protected boolean supply_rup (TRupture rup) {

		// Calculate the expiration time of the new rupture relative to the current rupture

		double t_expire = hook_calc_t_expire (current_rup, rup);

		// If the new rupture is never the largest function value, discard it
		// (Note: This line can be "t_expire < current_time" to allow ruptures with zero-duration intervals)

		if (t_expire <= current_time) {
			return false;
		}

		// Report the interval if it is non-empty

		report_interval (current_time);

		// While the current rupture is not the baseline rupture ...

		while (current_rup != baseline_rup) {

			// If the expiration time of the new rupture is strictly before the expiration time of the current rupture ...

			if (t_expire < hook_get_t_expire (current_rup)) {

				// Push the new rupture on the stack, as it is now largest,
				// but the current rupture becomes largest again after t_expire

				push_stack (rup);
				return true;
			}

			// Pop the stack, as the current rupture cannot be the largest again

			pop_stack();

			// Calculate the expiration time of the new rupture relative to the new current rupture

			t_expire = hook_calc_t_expire (current_rup, rup);

			// If the new rupture is never the largest function value, discard it
			// (This should never happen, and perhaps should throw an exception)
			// (Note: This line can be "t_expire < current_time" to allow ruptures with zero-duration intervals)

			if (t_expire <= current_time) {
				return false;
			}
		}

		// If the expiration time of the new rupture is strictly before the expiration time of the skyline ...

		if (t_expire < sky_t_max) {

			// Push the new rupture on the stack, as it is now largest,
			// but the current rupture becomes largest again after t_expire

			push_stack (rup);
			return true;
		}

		// The new rupture becomes the new baseline and current rupture

		poke_stack (rup);
		return true;
	}




	// Finish processing after the last rupture is supplied.

	protected void finish_up () {
	
		// Advance time to end of skyline

		advance_time (sky_t_max);

		// Report any remaining interval

		report_interval (sky_t_max);

		// Notify the subclass

		hook_finish_up();

		return;
	}




	//----- Construction -----


	// Default constructor does nothing.

	protected OERupSkyline () {}




}
