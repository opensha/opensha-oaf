package org.opensha.oaf.oetas.gui;

import org.opensha.oaf.util.GUIEDTException;


// Operational ETAS GUI - Common functions for transfer structures.
// Michael Barall 08/21/2021
//
// Parameter values from the controller are copied into this object,
// so that the model can access them without having to be on the EDT
// and without having to interact directly with the parameters.
//
// Some variables may be modifiable by the model.  The modified values
// are stored within this object, so that the modification can be done
// when not on the EDT.  Modified variables are marked as dirty, and
// are written back to the controller at a later time.


public interface OEGUIXferCommon {


	// Mark all variables as unmodified, by clearing all dirty-variable flags.

	public void xfer_clean ();


	// Load values.
	// Load values from the controller into variables within this object.
	// Only relevant variables are loaded; any other variables are undefined.
	// This function also marks all variables as unmodified (including undefined ones).
	// The return value is this object.

	public OEGUIXferCommon xfer_load ();


	// Store modified values back into the controller.
	// All variables flagged as dirty are written back,
	// and the dirty-variable flags are cleared.

	public void xfer_store () throws GUIEDTException;

}
