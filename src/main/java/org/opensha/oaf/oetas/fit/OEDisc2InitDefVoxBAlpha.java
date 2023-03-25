package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.util.OEDiscreteRange;


// Convenience class to hold definition of (b, alpha) pairs.
// Author: Michael Barall 03/16/2023.
//
// If constructed from separate ranges of b and alpha, also holds the separate ranges
// and functions to convert between separate and joint indexes.

public class OEDisc2InitDefVoxBAlpha {

	//----- Combination definition -----

	// Values and elements for b, for each combination, length = combo_count, must be non-empty.

	private OEValueElement[] a_b_velt;

	// Values and elements for alpha, for each combination, length = combo_count, can be null to force alpha == b.

	private OEValueElement[] a_alpha_velt;


	// Get the number of combinations.
	// Threading: Can be called simultaneously by multiple threads.

	public final int get_combo_count () {
		return a_b_velt.length;
	}


	// Get the b value element for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_b_velt (int combo_index) {
		return a_b_velt[combo_index];
	}


	// Get the b-value for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_b_value (int combo_index) {
		return a_b_velt[combo_index].get_ve_value();
	}


	// Get the alpha value element for a given index.
	// Returns null if alpha == b is forced.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_alpha_velt (int combo_index) {
		if (a_alpha_velt == null) {
			return null;
		}
		return a_alpha_velt[combo_index];
	}


	// Get the alpha-value for a given index.
	// If alpha == b is forced, return the b-value.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_alpha_value (int combo_index) {
		if (a_alpha_velt == null) {
			return a_b_velt[combo_index].get_ve_value();
		}
		return a_alpha_velt[combo_index].get_ve_value();
	}


	// Get the b-value to use for scaling.
	// If all combinations use the same b-value element, return that b-value.
	// Otherwise, return OEConstants.UNKNOWN_B_VALUE == -1.0 to signal the scaling b-value is unknown.

	public final double get_b_scaling () {
		OEValueElement b_velt = a_b_velt[0];
		final int combo_count = get_combo_count();
		for (int combo_index = 1; combo_index < combo_count; ++combo_index) {
			if (a_b_velt[combo_index] != b_velt) {
				return OEConstants.UNKNOWN_B_VALUE;		// found an entry with different value element
			}
		}
		return b_velt.get_ve_value();	// the single b-value
	}




	//----- Separate ranges -----

	// Values and elements for b, length = sep_b_count, must be non-empty if separate ranges are being used.

	private OEValueElement[] sep_b_velt;

	// Values and elements for alpha, length = sep_alpha_count, can be null to force alpha == b, if non-null then must be non-empty.

	private OEValueElement[] sep_alpha_velt;


	// Get the number of separate b values.

	public final int get_sep_b_count () {
		return sep_b_velt.length;
	}


	// Get one of the separate b values.

	public final OEValueElement get_sep_b_velt (int bix) {
		return sep_b_velt[bix];
	}


	// Dump the separate b values into a string.

	public final String dump_sep_b_velt () {
		StringBuilder result = new StringBuilder();

		int sep_b_count = get_sep_b_count();
		for (int bix = 0; bix < sep_b_count; ++bix) {
			result.append ("b_velt[" + bix + "] = " + sep_b_velt[bix].shortened_string() + "\n");
		}

		return result.toString();
	}


	// Get the number of separate alpha values.

	public final int get_sep_alpha_count () {
		if (sep_alpha_velt == null) {
			return 0;
		}
		return sep_alpha_velt.length;
	}


	// Get the number of separate alpha values, but at least one.

	public final int get_nonzero_sep_alpha_count () {
		if (sep_alpha_velt == null) {
			return 1;
		}
		return sep_alpha_velt.length;
	}


	// Get one of the separate alpha values.

	public final OEValueElement get_sep_alpha_velt (int alphaix) {
		return sep_alpha_velt[alphaix];
	}


	// Dump the separate alpha values into a string.

	public final String dump_sep_alpha_velt () {
		StringBuilder result = new StringBuilder();

		if (sep_alpha_velt == null) {
				result.append ("alpha_velt = <null>" + "\n");
		} else {
			int sep_alpha_count = get_sep_alpha_count();
			for (int alphaix = 0; alphaix < sep_alpha_count; ++alphaix) {
				result.append ("alpha_velt[" + alphaix + "] = " + sep_alpha_velt[alphaix].shortened_string() + "\n");
			}
		}

		return result.toString();
	}


	// Convert separate indexes into a joint combination index.

	public final int get_combo_index (int bix, int alphaix) {
		return alphaix * sep_b_velt.length + bix;
	}


	// Convert a joint combination index into a separate b index.

	public final int get_sep_bix (int combo_index) {
		return combo_index % sep_b_velt.length;
	}


	// Convert a joint combination index into a separate alpha index.

	public final int get_sep_alphaix (int combo_index) {
		return combo_index / sep_b_velt.length;
	}


	// Finish setting up separate ranges.
	// On entry, sep_b_velt and sep_alpha_velt are already set up.

	private void finish_make_sep () {

		// If forcing alpha == b ...

		if (sep_alpha_velt == null) {
			final int combo_count = sep_b_velt.length;
			a_b_velt = new OEValueElement[combo_count];
			a_alpha_velt = null;
			for (int combo_index = 0; combo_index < combo_count; ++combo_index) {
				a_b_velt[combo_index] = sep_b_velt[combo_index];
			}
		}

		// Otherwise, we have general alpha ...

		else {
			final int combo_count = sep_b_velt.length * sep_alpha_velt.length;
			a_b_velt = new OEValueElement[combo_count];
			a_alpha_velt = new OEValueElement[combo_count];
			for (int combo_index = 0; combo_index < combo_count; ++combo_index) {
				a_b_velt[combo_index] = sep_b_velt[get_sep_bix (combo_index)];
				a_alpha_velt[combo_index] = sep_alpha_velt[get_sep_alphaix (combo_index)];
			}
		}

		return;
	}


	// Get the combination index at the center of both ranges.

	public final int get_center_combo_index () {
		return get_combo_index (get_sep_b_count() / 2, get_sep_alpha_count() / 2);
	}




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		a_b_velt = null;
		a_alpha_velt = null;

		sep_b_velt = null;
		sep_alpha_velt = null;

		return;
	}




	// Default constructor.

	public OEDisc2InitDefVoxBAlpha () {
		clear();
	}




	// Set the combination definition, from separate arrays of values.
	// Parameters:
	//  sep_b_velt = Values and elements for b, must be non-empty.
	//  sep_alpha_velt = Values and elements for alpha, can be null to force alpha == b, if non-null must be non-empty.
	// Returns this object.
	// Note: This object copies the arrays.

	public final OEDisc2InitDefVoxBAlpha set_from_sep_values (
		OEValueElement[] sep_b_velt,
		OEValueElement[] sep_alpha_velt
	) {
		if (!( sep_b_velt != null && sep_b_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxBAlpha.set_from_sep_values: No b values");
		}
		if (!( sep_alpha_velt == null || sep_alpha_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxBAlpha.set_from_sep_values: No alpha values");
		}

		// Copy the supplied arrays

		this.sep_b_velt = new OEValueElement[sep_b_velt.length];
		for (int j = 0; j < sep_b_velt.length; ++j) {
			this.sep_b_velt[j] = sep_b_velt[j];
		}

		if (sep_alpha_velt == null) {
			this.sep_alpha_velt = null;
		} else {
			this.sep_alpha_velt = new OEValueElement[sep_alpha_velt.length];
			for (int j = 0; j < sep_alpha_velt.length; ++j) {
				this.sep_alpha_velt[j] = sep_alpha_velt[j];
			}
		}

		// Finish setup

		finish_make_sep();
		return this;
	}




	// Set the combination definition, from separate ranges of values.
	// Parameters:
	//  sep_b_range = Range for b.
	//  sep_alpha_range = Range for alpha, can be null to force alpha == b.
	// Returns this object.
	// Note: Ranges are guaranteed to be non-empty.

	public final OEDisc2InitDefVoxBAlpha set_from_sep_ranges (
		OEDiscreteRange sep_b_range,
		OEDiscreteRange sep_alpha_range
	) {
		if (!( sep_b_range != null )) {
			throw new IllegalArgumentException ("OEDisc2InitDefVoxBAlpha.set_from_sep_ranges: No b values");
		}

		// Convert ranges into arrays of values

		this.sep_b_velt = sep_b_range.get_velt_array();

		if (sep_alpha_range == null) {
			this.sep_alpha_velt = null;
		} else {
			this.sep_alpha_velt = sep_alpha_range.get_velt_array();
		}

		// Finish setup

		finish_make_sep();
		return this;
	}




	//----- Testing -----





}
