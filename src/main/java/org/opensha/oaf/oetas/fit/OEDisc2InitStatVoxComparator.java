package org.opensha.oaf.oetas.fit;

import java.util.Comparator;


// Comparator to sort voxels of statistical parameters in canonical order.
// Author: Michael Barall 03/14/2023.

public class OEDisc2InitStatVoxComparator implements Comparator<OEDisc2InitStatVox> {
	
	// Compares its two arguments for order. Returns a negative integer, zero, or a positive
	// integer as the first argument is less than, equal to, or greater than the second.

	@Override
	public final int compare (OEDisc2InitStatVox voxel1, OEDisc2InitStatVox voxel2) {
		return voxel1.compareTo (voxel2);
	}

}
