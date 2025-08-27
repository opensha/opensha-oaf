package org.opensha.oaf.pdl;


// Common base class for configuration settings for any PDL sender.
// Author: Michael Barall.

public abstract class PDLAnySenderConfig {

	// Constructor.

	public PDLAnySenderConfig () {
	}


	// Return true if this PDL sender is able to sign products.

	public abstract boolean sender_can_sign ();

}
