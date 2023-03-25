package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.util.OEDiscreteRange;


// Convenience class to hold definition of n values.
// Author: Michael Barall 03/16/2023.
//
// If constructed from separate range of n, also holds the separate range
// and functions to convert between separate and joint indexes.
//
// Design note: Although this class contains only a single variable, it maintains
// a distinction between separate and joint ranges (which are the same) for
// consistency with other definition classes.

public class OEDisc2InitDefVoxN {

	//----- Combination definition -----

	// Values and elements for n, for each combination, length = combo_count, must be non-empty.

	private OEValueElement[] a_n_velt;


	// Get the number of combinations.
	// Threading: Can be called simultaneously by multiple threads.

	public final int get_combo_count () {
		return a_n_velt.length;
	}


	// Get the n value element for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_n_velt (int combo_index) {
		return a_n_velt[combo_index];
	}


	// Get the n-value for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_n_value (int combo_index) {
		return a_n_velt[combo_index].get_ve_value();
	}




	//----- Separate ranges -----

	// Values and elements for n, length = sep_n_count, must be non-empty if separate ranges are being used.

	private OEValueElement[] sep_n_velt;


	// Get the number of separate n values.

	public final int get_sep_n_count () {
		return sep_n_velt.length;
	}


	// Get one of the separate n values.

	public final OEValueElement get_sep_n_velt (int nix) {
		return sep_n_velt[nix];
	}


	// Dump the separate n values into a string.

	public final String dump_sep_n_velt () {
		StringBuilder result = new StringBuilder();

		int sep_n_count = get_sep_n_count();
		for (int nix = 0; nix < sep_n_count; ++nix) {
			result.append ("n_velt[" + nix + "] = " + sep_n_velt[nix].shortened_string() + "\n");
		}

		return result.toString();
	}


	// Convert separate indexes into a joint combination index.

	public final int get_combo_index (int nix) {
		return nix;
	}


	// Convert a joint combination index into a separate n index.

	public final int get_sep_nix (int combo_index) {
		return combo_index;
	}


	// Finish setting up separate ranges.
	// On entry, sep_n_velt and sep_zmu_velt are already set up.

	private void finish_make_sep () {

		final int combo_count = sep_n_velt.length;
		a_n_velt = new OEValueElement[combo_count];
		for (int combo_index = 0; combo_index < combo_count; ++combo_index) {
			a_n_velt[combo_index] = sep_n_velt[get_sep_nix (combo_index)];
		}

		return;
	}


	// Get the combination index at the center of ALL ranges.

	public final int get_center_combo_index () {
		return get_combo_index (get_sep_n_count() / 2);
	}




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		a_n_velt = null;

		sep_n_velt = null;

		return;
	}




	// Default constructor.

	public OEDisc2InitDefVoxN () {
		clear();
	}




	// Set the combination definition, from separate arrays of values.
	// Parameters:
	//  sep_n_velt = Values and elements for n, must be non-empty.
	// Returns this object.
	// Note: This object copies the arrays.

	public final OEDisc2InitDefVoxN set_from_sep_values (
		OEValueElement[] sep_n_velt
	) {
		if (!( sep_n_velt != null && sep_n_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxN.set_from_sep_values: No n values");
		}

		// Copy the supplied arrays

		this.sep_n_velt = new OEValueElement[sep_n_velt.length];
		for (int j = 0; j < sep_n_velt.length; ++j) {
			this.sep_n_velt[j] = sep_n_velt[j];
		}

		// Finish setup

		finish_make_sep();
		return this;
	}




	// Set the combination definition, from separate ranges of values.
	// Parameters:
	//  sep_n_range = Range for n.
	// Returns this object.
	// Note: Ranges are guaranteed to be non-empty.

	public final OEDisc2InitDefVoxN set_from_sep_ranges (
		OEDiscreteRange sep_n_range
	) {
		if (!( sep_n_range != null )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxN.set_from_sep_ranges: No n values");
		}

		// Convert ranges into arrays of values

		this.sep_n_velt = sep_n_range.get_velt_array();

		// Finish setup

		finish_make_sep();
		return this;
	}




	//----- Testing -----





}
