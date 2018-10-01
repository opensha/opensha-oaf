package scratch.aftershockStatistics.pdl;

/**
 * Exception class for simulated PDL errors.
 * Author: Michael Barall 09/28/2018.
 *
 * This exception is thrown to simulate a PDL access error.
 */
public class PDLSimulatedException extends RuntimeException {

	// Constructors.

	public PDLSimulatedException () {
		super ();
	}

	public PDLSimulatedException (String s) {
		super (s);
	}

	public PDLSimulatedException (String message, Throwable cause) {
		super (message, cause);
	}

	public PDLSimulatedException (Throwable cause) {
		super (cause);
	}

}
