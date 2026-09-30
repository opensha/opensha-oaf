package org.opensha.oaf.util.gui;

import java.awt.Component;
import javax.swing.SwingUtilities;


// Functional interface to receive a help request.
// Author: Michael Barall.

@FunctionalInterface
public interface GUIHelpListener {

	// Process a help request.
	// Parameters:
	//  help_component = Component that triggered the help request (typically a JButton).  Can be null.
	//                   This component might be used to position the help dialog,
	//                   or its containing window might be used as the help dialog's owner.

	public void help_request (Component help_component) throws GUIEDTException;


	// Invoke the help request.
	// Parameters:
	//  help_component = Component that triggered the help request (typically a JButton).  Can be null.
	//                   This component might be used to position the help dialog,
	//                   or its containing window might be used as the help dialog's owner.

	public default void invoke_help_request (Component help_component) {

		// Check that we are on the EDT, throw an exception if not

		if (!( SwingUtilities.isEventDispatchThread() )) {
			throw new IllegalStateException ("GUIHelpListener.invoke_help_request -  Called while not on the event dispatch thread!");
		}

		// Pass to application

		try {
			help_request (help_component);
		} catch (GUIEDTException e) {
			throw new IllegalStateException ("GUIHelpListener.invoke_help_request - Caught GUIEDTException, which should never be thrown", e);
		}

		return;
	}

}
