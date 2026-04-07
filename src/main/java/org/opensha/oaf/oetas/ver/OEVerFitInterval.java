package org.opensha.oaf.oetas.ver;


// Abstraction of an interval for fitting verification.
// Author: Michael Barall.

public interface OEVerFitInterval {

	//----- Access -----

	// Get the start and end time of the interval, in days.

	public double get_int_time_1 ();
	public double get_int_time_2 ();

	// Get the interval duration, in days.
	// The value should be get_int_time_2() - get_int_time_1().

	public double get_int_duration ();

	// Get the interval midpoint, in days.
	// The value should be (get_int_time_2() + get_int_time_1()) / 2.0.

	public double get_int_midpoint ();

	// Get the interval magnitude of completeness.

	public double get_int_mc ();

	// Return true if the interval magnitude of completeness is the history magCat.

	public boolean get_int_is_magcat ();


	//----- Storage -----

	// Get an index number for the interval.
	// The index ranges from 0 to one less than the number of intervals in the history.
	// This can be used for access to working storage per-interval.
	// Clients should not make any assumptions about index values, eg, that they are
	// increasing or sequential. The only guarantee is that each interval has a different index.
	
	public int get_int_index ();


	//----- Display -----

	// Get a one-line display of the interval, not ending in newline.

	public String one_line_string ();


}
