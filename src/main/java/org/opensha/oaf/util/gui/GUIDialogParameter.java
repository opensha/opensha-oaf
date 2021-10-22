package org.opensha.oaf.util.gui;

/**
 * Interface for parameters that display a dialog box.
 * Author: Michael Barall 08/06/2021.
 */
public interface GUIDialogParameter {

	// Status code values.
	// These indicate the current dialog state.

	public static final int DLGSTAT_NONE = 0;		// No status code (dialog never shown)
	public static final int DLGSTAT_OPEN = 1;		// Dialog is currently open
	public static final int DLGSTAT_CLOSING = 2;	// Dialog is in the process of closing
	public static final int DLGSTAT_CLOSED = 3;		// Dialog is closed


	// Termination code values.
	// These indicate the reason the dialog was closed.

	public static final int TERMCODE_NONE = 0;		// No termination code (dialog never shown)
	public static final int TERMCODE_OPEN = 1;		// Dialog is currently open
	public static final int TERMCODE_CLOSED = 2;	// Closed dialog using X at upper right (or Alt+F4 on some systems)
	public static final int TERMCODE_SYSTEM = 3;	// Dialog closed by system
	public static final int TERMCODE_PARENT = 4;	// Closed because parent dialog was closed
	public static final int TERMCODE_REMOVED = 5;	// Closed because it was removed from parent dialog
	public static final int TERMCODE_DISABLED = 6;	// Closed because parameter was disabled
	public static final int TERMCODE_OK = 10;		// User pressed OK or similar
	public static final int TERMCODE_CANCEL = 11;	// User pressed Cancel or similar
	//public static final int TERMCODE_YES = 12;		// User pressed Yes or similar
	//public static final int TERMCODE_NO = 13;		// User pressed No or similar
	public static final int TERMCODE_USER = 100;	// User-defined termination code


	// Get the current dialog status code.

	public int getDialogStatus ();


	// Get the current dialog termination code.
	// The termination code becomes available when the dialog begins the process of closing.

	public int getDialogTermCode ();


	// Return true if this is a modal dialog.

	public boolean getModalDialog ();


	// Open the dialog.
	// Returns true if the dialog was created, false if not (because it is open or in the process of closing).
	// If a modal dialog is created, this does not return until the dialog begins the process of closing
	// (at which time the termination code will be available).

	public boolean openDialog ();


	// Close the dialog, and set the termination code.
	// Return true if success, false if not open or already in the process of closing.
	// Upon return, the dialog will have begun the process of closing, and may or may not be closed.
	// The termination code will available through getDialogTermCode.

	public boolean closeDialog (int the_dialogTermCode);

}
