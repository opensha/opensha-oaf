package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.util.OEDiscreteRange;


// Convenience class to hold definition of a sub-voxel (values of ams and mu).
// Author: Michael Barall 03/06/2023.
//
// If constructed from separate ranges of ams and mu, also holds the separate ranges
// and functions to convert between separate and joint indexes.

public class OEDisc2InitSubVoxDef {

	//----- Sub-voxel definition -----

	// Values and elements for zams, for each sub-voxel, length = subvox_count, must be non-empty.

	private OEValueElement[] a_zams_velt;

	// Values and elements for zmu, for each sub-voxel, length = subvox_count, can be null to force zmu == 0.

	private OEValueElement[] a_zmu_velt;


	// Get the number of sub-voxels.
	// Threading: Can be called simultaneously by multiple threads.

	public final int get_subvox_count () {
		return a_zams_velt.length;
	}


	// Get the zams value element for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_zams_velt (int combo_index) {
		return a_zams_velt[combo_index];
	}


	// Get the zams-value for a given index.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_zams_value (int combo_index) {
		return a_zams_velt[combo_index].get_ve_value();
	}


	// Get the zmu value element for a given index.
	// Returns null if zmu == 0 is forced.
	// Threading: Can be called simultaneously by multiple threads.

	public final OEValueElement get_zmu_velt (int combo_index) {
		if (a_zmu_velt == null) {
			return null;
		}
		return a_zmu_velt[combo_index];
	}


	// Get the zmu-value for a given index.
	// If zmu == 0 is forced, return 0.
	// Threading: Can be called simultaneously by multiple threads.

	public final double get_zmu_value (int combo_index) {
		if (a_zmu_velt == null) {
			return 0.0;
		}
		return a_zmu_velt[combo_index].get_ve_value();
	}


	// Set the sub-voxel definition into the given voxel.
	// Threading: Can be called simultaneously by multiple threads.

	public final void set_subvox_def (OEDisc2InitStatVox stat_vox) {
		stat_vox.set_subvox_def (
			a_zams_velt,
			a_zmu_velt
		);
		return;
	}




	//----- Separate ranges -----

	// Values and elements for zams, length = sep_zams_count, must be non-empty if separate ranges are being used.

	private OEValueElement[] sep_zams_velt;

	// Values and elements for zmu, length = sep_zmu_count, can be null to force zmu == 0, if non-null then must be non-empty.

	private OEValueElement[] sep_zmu_velt;


	// Get the number of separate zams values.

	public final int get_sep_zams_count () {
		return sep_zams_velt.length;
	}


	// Get one of the separate zams values.

	public final OEValueElement get_sep_zams_velt (int amsix) {
		return sep_zams_velt[amsix];
	}


	// Dump the separate zams values into a string.

	public final String dump_sep_zams_velt () {
		StringBuilder result = new StringBuilder();

		int sep_zams_count = get_sep_zams_count();
		for (int amsix = 0; amsix < sep_zams_count; ++amsix) {
			result.append ("zams_velt[" + amsix + "] = " + sep_zams_velt[amsix].shortened_string() + "\n");
		}

		return result.toString();
	}


	// Get the number of separate zmu values.

	public final int get_sep_zmu_count () {
		if (sep_zmu_velt == null) {
			return 0;
		}
		return sep_zmu_velt.length;
	}


	// Get the number of separate zmu values, but at least one.

	public final int get_nonzero_sep_zmu_count () {
		if (sep_zmu_velt == null) {
			return 1;
		}
		return sep_zmu_velt.length;
	}


	// Get one of the separate zmu values.

	public final OEValueElement get_sep_zmu_velt (int muix) {
		return sep_zmu_velt[muix];
	}


	// Dump the separate zmu values into a string.

	public final String dump_sep_zmu_velt () {
		StringBuilder result = new StringBuilder();

		if (sep_zmu_velt == null) {
				result.append ("zmu_velt = <null>" + "\n");
		} else {
			int sep_zmu_count = get_sep_zmu_count();
			for (int muix = 0; muix < sep_zmu_count; ++muix) {
				result.append ("zmu_velt[" + muix + "] = " + sep_zmu_velt[muix].shortened_string() + "\n");
			}
		}

		return result.toString();
	}


	// Convert separate indexes into a joint sub-voxel index.

	public final int get_subvox_index (int amsix, int muix) {
		return muix * sep_zams_velt.length + amsix;
	}


	// Convert a joint sub-voxel index into a separate zams index.

	public final int get_sep_amsix (int subvox_index) {
		return subvox_index % sep_zams_velt.length;
	}


	// Convert a joint sub-voxel index into a separate zmu index.

	public final int get_sep_muix (int subvox_index) {
		return subvox_index / sep_zams_velt.length;
	}


	// Finish setting up separate ranges.
	// On entry, sep_zams_velt and sep_zmu_velt are already set up.

	private void finish_make_sep () {

		// If no background ...

		if (sep_zmu_velt == null) {
			final int subvox_count = sep_zams_velt.length;
			a_zams_velt = new OEValueElement[subvox_count];
			a_zmu_velt = null;
			for (int subvox_index = 0; subvox_index < subvox_count; ++subvox_index) {
				a_zams_velt[subvox_index] = sep_zams_velt[subvox_index];
			}
		}

		// Otherwise, we have background ...

		else {
			final int subvox_count = sep_zams_velt.length * sep_zmu_velt.length;
			a_zams_velt = new OEValueElement[subvox_count];
			a_zmu_velt = new OEValueElement[subvox_count];
			for (int subvox_index = 0; subvox_index < subvox_count; ++subvox_index) {
				a_zams_velt[subvox_index] = sep_zams_velt[get_sep_amsix (subvox_index)];
				a_zmu_velt[subvox_index] = sep_zmu_velt[get_sep_muix (subvox_index)];
			}
		}

		return;
	}


	// Get the log-density of the given voxel into a grid.
	// Parameters:
	//  stat_vox = Voxel, must have been set up using this object, must have Bayesian prior and log-likelihoods computed.
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  grid = Grid to receive log-density values, indexed as grid[amsix][muix].
	// Note: This function is primarily for testing.

	public final void get_log_density_grid (OEDisc2InitStatVox stat_vox, double bay_weight, double[][] grid) {
		final int subvox_count = get_subvox_count();
		for (int subvox_index = 0; subvox_index < subvox_count; ++subvox_index) {
			grid[get_sep_amsix (subvox_index)][get_sep_muix (subvox_index)] = stat_vox.get_subvox_log_density (subvox_index, bay_weight);
		}
		return;
	}


	// Get the sub-voxel index at the center of both ranges.

	public final int get_center_subvox_index () {
		return get_subvox_index (get_sep_zams_count() / 2, get_sep_zmu_count() / 2);
	}




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		a_zams_velt = null;
		a_zmu_velt = null;

		sep_zams_velt = null;
		sep_zmu_velt = null;

		return;
	}




	// Default constructor.

	public OEDisc2InitSubVoxDef () {
		clear();
	}




	// Set the sub-voxel definition, from separate arrays of values.
	// Parameters:
	//  sep_zams_velt = Values and elements for zams, must be non-empty.
	//  sep_zmu_velt = Values and elements for zmu, can be null to force zmu == 0, if non-null must be non-empty.
	// Returns this object.
	// Note: This object copies the arrays.

	public final OEDisc2InitSubVoxDef set_from_sep_values (
		OEValueElement[] sep_zams_velt,
		OEValueElement[] sep_zmu_velt
	) {
		if (!( sep_zams_velt != null && sep_zams_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitSubVoxDef.set_from_sep_values: No zams values");
		}
		if (!( sep_zmu_velt == null || sep_zmu_velt.length > 0 )) {
			throw new IllegalArgumentException ("OEDisc2InitSubVoxDef.set_from_sep_values: No zmu values");
		}

		// Copy the supplied arrays

		this.sep_zams_velt = new OEValueElement[sep_zams_velt.length];
		for (int j = 0; j < sep_zams_velt.length; ++j) {
			this.sep_zams_velt[j] = sep_zams_velt[j];
		}

		if (sep_zmu_velt == null) {
			this.sep_zmu_velt = null;
		} else {
			this.sep_zmu_velt = new OEValueElement[sep_zmu_velt.length];
			for (int j = 0; j < sep_zmu_velt.length; ++j) {
				this.sep_zmu_velt[j] = sep_zmu_velt[j];
			}
		}

		// Finish setup

		finish_make_sep();
		return this;
	}




	// Set the sub-voxel definition, from separate ranges of values.
	// Parameters:
	//  sep_zams_range = Range for zams.
	//  sep_zmu_range = Range for zmu, can be null to force zmu == 0.
	// Returns this object.
	// Note: Ranges are guaranteed to be non-empty.

	public final OEDisc2InitSubVoxDef set_from_sep_ranges (
		OEDiscreteRange sep_zams_range,
		OEDiscreteRange sep_zmu_range
	) {
		if (!( sep_zams_range != null )) {
			throw new IllegalArgumentException ("OEDisc2InitSubVoxDef.set_from_sep_ranges: No zams values");
		}

		// Convert ranges into arrays of values

		this.sep_zams_velt = sep_zams_range.get_velt_array();

		if (sep_zmu_range == null) {
			this.sep_zmu_velt = null;
		} else {
			this.sep_zmu_velt = sep_zmu_range.get_velt_array();
		}

		// Finish setup

		finish_make_sep();
		return this;
	}




	//----- Testing -----





}
