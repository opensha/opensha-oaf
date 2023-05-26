package org.opensha.oaf.util;


// Implementation of AutoCleanup that performs no operation at the end of a block.
// Author: Michael Barall 05/25/2023.

public class AutoCleanupNoOp extends AutoCleanup {

	// The cleanup function, to be overridden by the subclass.

	@Override
	protected void cleanup () {
		return;
	}

}
