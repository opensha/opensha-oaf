package org.opensha.oaf.util.gui;

import javax.swing.SwingUtilities;


/**
 * An operation that runs on the event dispatch thread.
 * Author: Michael Barall 04/06/2021.
 */
public abstract class GUIEDTRunnable implements Runnable {

	// This is the function that must run in the event dispatch thread.

	public abstract void run_in_edt () throws GUIEDTException;

	// Entry point.

	@Override
	public final void run() {

		// Check that we are on the EDT, throw an exception if not

		if (!( SwingUtilities.isEventDispatchThread() )) {
			throw new IllegalStateException ("GUIEDTRunnable.run -  Called while not on the event dispatch thread!");
		}

		// Pass to application

		try {
			run_in_edt ();
		} catch (GUIEDTException e) {
			throw new IllegalStateException ("GUIEDTRunnable.run - Caught GUIEDTException, which should never be thrown", e);
		}

		return;
	}


	// Function to check if we are in the EDT, and throw an exception if not.

	public static void check_on_edt () {
		if (!( SwingUtilities.isEventDispatchThread() )) {
			throw new IllegalStateException ("GUIEDTRunnable.check_on_edt -  Called while not on the event dispatch thread!");
		}
		return;
	}

}
