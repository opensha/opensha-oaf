package org.opensha.oaf.rj;

/**
 * Holds a global flag to control if aftershock code should write progress to System.out.
 * Author: Michael Barall 10/13/2018.
 */
public class AftershockVerbose {

	// Flag is set true to write progress to System.out.

	private static boolean verbose_mode = true;


	// Turn verbose mode on or off.
	// This routine is synchronized to allow access from multiple threads.

	public static synchronized void set_verbose_mode (boolean f_verbose) {
		verbose_mode = f_verbose;
		return;
	}


	// Get the verbose mode setting.
	// This routine is synchronized to allow access from multiple threads.
	// The recommended use, when possible, is for an object to read the
	// verbose mode setting in its constructor and save the setting to a
	// field.  Then subsequently it looks at the field rather than
	// repeatedly calling this function.

	public static synchronized boolean get_verbose_mode () {
		return verbose_mode;
	}

}
