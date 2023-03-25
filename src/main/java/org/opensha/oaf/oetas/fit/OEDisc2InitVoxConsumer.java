package org.opensha.oaf.oetas.fit;

import java.util.Collection;


// Destination for voxels of statistical parameters (b, alpha, c, p, a|n).
// Author: Michael Barall 03/08/2023.

public interface OEDisc2InitVoxConsumer {


	//----- Consumer functions -----




	// Begin consuming voxels.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  b_scaling = The b-value to use for scaling catalog size, or OEConstants.UNKNOWN_B_VALUE == -1.0 if unknown.
	// Note: This function retains the fit_info object, so the caller must
	// not modify it after the function returns.
	// Threading: This function should be called by a single thread,
	// before any calls to add_voxel.

	public void begin_voxel_consume (OEDisc2InitFitInfo fit_info, double b_scaling);




	// End consuming voxels.
	// Threading: This function should be called by a single thread,
	// after all calls to add_voxel have returned.

	public void end_voxel_consume ();




	// Add a list of voxels to the set of voxels.
	// Parameters:
	//  voxels = List of voxels to add to the set.
	// Threading: Can be called simulataneously by multiple threads, so the
	// implementation must synchronize appropriately.  To minimize synchronization
	// cost, instead of supplying voxels one at a time, the caller should supply
	// them in groups, perhaps as large as all voxels created by the caller's thread.

	public void add_voxels (Collection<OEDisc2InitStatVox> voxels);

}
