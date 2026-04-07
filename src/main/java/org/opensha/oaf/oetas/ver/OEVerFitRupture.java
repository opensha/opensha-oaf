package org.opensha.oaf.oetas.ver;


// Abstraction of a rupture for fitting verification.
// Author: Michael Barall.

public interface OEVerFitRupture {

	//----- Access -----

	// Get the rupture time, in days.
	// This time should be used for interactions between two ruptures.

	public double get_rup_t_day ();

	// Get the rupture magnitude.

	public double get_rup_mag ();

	// Get the rupture magnitude of completeness.

	public double get_rup_mc ();

	// Get the rupture x and y coordinates, in km.

	public double get_rup_x_km ();
	public double get_rup_y_km ();

	// Get the rupture time aligned with intervals, in days.
	// This time should be used for interactions between a rupture and an interval.
	// This time should be the same as or very close to get_rup_t_day();

	public double get_rup_aligned_time ();

	// Return true if this rupture is categorized as a mainshock or foreshock.

	public boolean get_rup_is_main ();

	// Compare this rupture to the given interval.
	// Returns:
	//  < 0 if the rupture is before the interval (including at the start time of the interval).
	//  = 0 if the rupture is interior to the interval.
	//  > 0 if the rupture is after the interval (including at the end time of the interval).

	public int compare_to_interval (OEVerFitInterval interval);


	//----- Storage -----

	// Get an index number for the rupture.
	// The index ranges from 0 to one less than the number of ruptures in the history.
	// This can be used for access to working storage per-rupture.
	// Clients should not make any assumptions about index values, eg, that they are
	// increasing or sequential. The only guarantee is that each rupture has a different index.
	
	public int get_rup_index ();


	//----- Display -----

	// Get a one-line display of the rupture, not ending in newline.

	public String one_line_string ();


}
