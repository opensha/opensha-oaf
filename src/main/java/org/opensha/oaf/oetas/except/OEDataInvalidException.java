package org.opensha.oaf.oetas.except;

// Checked exception class that indicates receipt of invalid data for ETAS.
// Author: Michael Barall 03/15/2023.

public class OEDataInvalidException extends OEException {

	// Constructors.

	public OEDataInvalidException () {
		super ();
	}

	public OEDataInvalidException (String s) {
		super (s);
	}

	public OEDataInvalidException (String message, Throwable cause) {
		super (message, cause);
	}

	public OEDataInvalidException (Throwable cause) {
		super (cause);
	}

}
