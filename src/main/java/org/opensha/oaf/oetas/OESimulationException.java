package org.opensha.oaf.oetas;

/**
 * Exception class for simulation failure for operational ETAS.
 * Author: Michael Barall 03/17/2022.
 */
public class OESimulationException extends RuntimeException {

	// Constructors.

	public OESimulationException () {
		super ();
	}

	public OESimulationException (String s) {
		super (s);
	}

	public OESimulationException (String message, Throwable cause) {
		super (message, cause);
	}

	public OESimulationException (Throwable cause) {
		super (cause);
	}

}
