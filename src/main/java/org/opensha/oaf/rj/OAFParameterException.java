package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalException;


// Exception class for OAF parameter marshaling/unmarshaling.
// Author: Michael Barall 01/16/2024.
 
public class OAFParameterException extends MarshalException {

	// Constructors.

	public OAFParameterException () {
		super ();
	}

	public OAFParameterException (String s) {
		super (s);
	}

	public OAFParameterException (String message, Throwable cause) {
		super (message, cause);
	}

	public OAFParameterException (Throwable cause) {
		super (cause);
	}

}
