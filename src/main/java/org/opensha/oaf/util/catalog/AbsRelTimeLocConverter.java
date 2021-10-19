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

}
