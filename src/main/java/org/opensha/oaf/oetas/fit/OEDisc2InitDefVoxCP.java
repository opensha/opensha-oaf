package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.util.OEDiscreteRange;


// Convenience class to hold definition of (c, p) pairs.
// Author: Michael Barall 03/16/2023.
//
// If constructed from separate ranges of c and p, also holds the separate ranges
// and functions to convert between separate and joint indexes.

public class OEDisc2InitDefVoxCP {

	//----- Combination definition -----

	// Values and elements for c, for each combination, length = combo_count, must be non-empty.

	private OEValueElement[] a_c_velt;

	// Values and elements for p, for each combination, length = combo_count, must be non-empty.

	private OEValueElement[] a_p_velt;


	// Get the number of combinations.
	// Threading: Can be called simultaneously by multiple threads.

	public final int get_combo_count () {
		return a_c_velt.length;
	}


	// Get the c value element for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_c_velt (int combo_index) {
		return a_c_velt[combo_index];
	}


	// Get the c-value for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_c_value (int combo_index) {
		return a_c_velt[combo_index].get_ve_value();
	}


	// Get the p value element for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_p_velt (int combo_index) {
		return a_p_velt[combo_index];
	}


	// Get the p-value for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_p_value (int combo_index) {
		return a_p_velt[combo_index].get_ve_value();
	}




	//----- Separate ranges -----

	// Values and elements for c, length = sep_c_count, must be non-empty if separate ranges are being used.

	private OEValueElement[] sep_c_velt;

	// Values and elements for p, length = sep_p_count, must be non-empty if separate ranges are being used.

	private OEValueElement[] sep_p_velt;


	// Get the number of separate c values.

	public final int get_sep_c_count () {
		return sep_c_velt.length;
	}


	// Get one of the separate c values.

	public final OEValueElement get_sep_c_velt (int cix) {
		return sep_c_velt[cix];
	}


	// Dump the separate c values into a string.

	public final String dump_sep_c_velt () {
		StringBuilder result = new StringBuilder();

		int sep_c_count = get_sep_c_count();
		for (int cix = 0; cix < sep_c_count; ++cix) {
			result.append ("c_velt[" + cix + "] = " + sep_c_velt[cix].shortened_string() + "\n");
		}

		return result.toString();
	}


	// Get the number of separate p values.

	public final int get_sep_p_count () {
		if (sep_p_velt == null) {
			return 0;
		}
		return sep_p_velt.length;
	}


	// Get one of the separate p values.

	public final OEValueElement get_sep_p_velt (int pix) {
		return sep_p_velt[pix];
	}


	// Dump the separate p values into a string.

	public final String dump_sep_p_velt () {
		StringBuilder result = new StringBuilder();

		int sep_p_count = get_sep_p_count();
		for (int pix = 0; pix < sep_p_count; ++pix) {
			result.append ("p_velt[" + pix + "] = " + sep_p_velt[pix].shortened_string() + "\n");
		}

		return result.toString();
	}


	// Convert separate indexes into a joint combination index.

	public final int get_combo_index (int cix, int pix) {
		return pix * sep_c_velt.length + cix;
	}


	// Convert a joint combination index into a separate c index.

	public final int get_sep_cix (int combo_index) {
		return combo_index % sep_c_velt.length;
	}


	// Convert a joint combination index into a separate p index.

	public final int get_sep_pix (int combo_index) {
		return combo_index / sep_c_velt.length;
	}


	// Finish setting up separate ranges.
	// On entry, sep_c_velt and sep_p_velt are already set up.

	private void finish_make_sep () {

		final int combo_count = sep_c_velt.length * sep_p_velt.length;
		a_c_velt = new OEValueElement[combo_count];
		a_p_velt = new OEValueElement[combo_count];
		for (int combo_index = 0; combo_index < combo_count; ++combo_index) {
			a_c_velt[combo_index] = sep_c_velt[get_sep_cix (combo_index)];
			a_p_velt[combo_index] = sep_p_velt[get_sep_pix (combo_index)];
		}

		return;
	}


	// Get the combination index at the center of both ranges.

	public final int get_center_combo_index () {
		return get_combo_index (get_sep_c_count() / 2, get_sep_p_count() / 2);
	}




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		a_c_velt = null;
		a_p_velt = null;

		sep_c_velt = null;
		sep_p_velt = null;

		return;
	}




	// Default constructor.

	public OEDisc2InitDefVoxCP () {
		clear();
	}




	// Set the combination definition, from separate arrays of values.
	// Parameters:
	//  sep_c_velt = Values and elements for c, must be non-empty.
	//  sep_p_velt = Values and elements for p, must be non-empty.
	// Returns this object.
	// Note: This object copies the arrays.

	public final OEDisc2InitDefVoxCP set_from_sep_values (
		OEValueElement[] sep_c_velt,
		OEValueElement[] sep_p_velt
	) {
		if (!( sep_c_velt != null && sep_c_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxCP.set_from_sep_values: No c values");
		}
		if (!( sep_p_velt != null && sep_p_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxCP.set_from_sep_values: No p values");
		}

		// Copy the supplied arrays

		this.sep_c_velt = new OEValueElement[sep_c_velt.length];
		for (int j = 0; j < sep_c_velt.length; ++j) {
			this.sep_c_velt[j] = sep_c_velt[j];
		}

		this.sep_p_velt = new OEValueElement[sep_p_velt.length];
		for (int j = 0; j < sep_p_velt.length; ++j) {
			this.sep_p_velt[j] = sep_p_velt[j];
		}

		// Finish setup

		finish_make_sep();
		return this;
	}




	// Set the combination definition, from separate ranges of values.
	// Parameters:
	//  sep_c_range = Range for c.
	//  sep_p_range = Range for p.
	// Returns this object.
	// Note: Ranges are guaranteed to be non-empty.

	public final OEDisc2InitDefVoxCP set_from_sep_ranges (
		OEDiscreteRange sep_c_range,
		OEDiscreteRange sep_p_range
	) {
		if (!( sep_c_range != null )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxCP.set_from_sep_ranges: No c values");
		}
		if (!( sep_p_range != null )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxCP.set_from_sep_ranges: No p values");
		}

		// Convert ranges into arrays of values

		this.sep_c_velt = sep_c_range.get_velt_array();

		this.sep_p_velt = sep_p_range.get_velt_array();

		// Finish setup

		finish_make_sep();
		return this;
	}




	//----- Testing -----





}
