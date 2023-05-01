package org.opensha.oaf.util.catalog;


// Interface for converting between absolute and relative time and location.
// Author: Michael Barall 10/11/2021.

public interface AbsRelTimeLocConverter {

	// Convert from absolute to relative time and location.
	// Parameters:
	//	abs_tloc = Absolute time and location.
	//	rel_tloc = Receives relative time and location.

	public void convert_abs_to_rel (AbsoluteTimeLocation abs_tloc, RelativeTimeLocation rel_tloc);


	// Convert from relative to absolute time and location.
	// Parameters:
	//	rel_tloc = Relative time and location.
	//	abs_tloc = Receives absolute time and location.

	public void convert_rel_to_abs (RelativeTimeLocation rel_tloc, AbsoluteTimeLocation abs_tloc);


	// Convert time from absolute to relative.
	// Parameters:
	//  abs_time = Absolute time, in milliseconds since the epoch.
	// Returns relative time, in days since the origin.

	public double convert_time_abs_to_rel (long abs_time);


	// Convert time from relative to absolute.
	// Parameters:
	//  rel_t_day = Relative time, in days since the origin.
	// Returns absolute time, in milliseconds since the epoch.

	public long convert_time_rel_to_abs (double rel_t_day);
}
