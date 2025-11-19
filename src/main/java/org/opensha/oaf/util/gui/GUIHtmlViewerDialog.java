package org.opensha.oaf.util.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.net.URL;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.SwingUtilities;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;


// Dialog box that displays HTML, used mainly to display help text.
// Author: Michael Barall.
//
// HTML is displayed using JEditorPane and therefore HTML 3.2 is supported.
//
// The dialog can be modal or modeless.  It can optionally display a close button.

public class GUIHtmlViewerDialog {

	/** If true print out debug statements. */
	protected boolean D = false;

	// Editor to display HTML.
	protected JEditorPane editor_pane = null;

	// Frame for displaying the dialog.
	protected JDialog frame = null;

	// The Window that owns the frame.
	protected Window frame_window = null;

	// The button that closes the dialog, with OK, or null if not allocated.
	protected JButton ok_button = null;

	// The button that closes the dialog, with CANCEL, or null if not allocated.
	protected JButton cancel_button = null;

	// Set if a resize command was received while the dialog is open, but not processed yet.
	protected boolean pendingResize = false;
	
	// Set to make the dialog modal.
	private boolean modal = true;
	
	// Dialog size, if null or too small then use a default size.
	private Dimension dialogDims = null;

	// Dialog position, if null then use a default position.
	private Point dialogPosition = null;

	// Dialog title, or null for default title.
	private String dialog_title = null;

	// True if the dialog title has changed since the dialog was opened.
	private boolean changed_dialog_title = false;

	// Text to appear on the ok button, or null for no button.
	private String ok_button_text = null;

	// True if the text on the ok button has changed since the dialog was opened.
	private boolean changed_ok_button_text = false;

	// Text to appear on the cancel button, or null for no button.
	private String cancel_button_text = null;

	// True if the text on the cancel button has changed since the dialog was opened.
	private boolean changed_cancel_button_text = false;

	// Dialog status code (see GUIDialogParameter.DLGSTAT_XXXX).
	private int dialog_status = 0;

	// Dialog termination code (see GUIDialogParameter.TERMCODE_XXXX).
	private int dialog_termcode = 0;




	// Window listener to catch close events.

	private static class MyWindowListener implements WindowListener {

		// Link to outer class.
		// (We code this explicitly so that we can break the link when the dialog is closed.)

		private GUIHtmlViewerDialog outer;

		public final void set_outer (GUIHtmlViewerDialog the_outer) {
			outer = the_outer;
			return;
		}

		public MyWindowListener (GUIHtmlViewerDialog the_outer) {
			set_outer (the_outer);
		}

		// WindowListener functions.

		@Override public void windowActivated (WindowEvent e) {
			return;
		}

		@Override public void windowClosed (WindowEvent e) {
			if (outer != null) {
				outer.frame_windowClosed (e);
			}
			return;
		}

		@Override public void windowClosing (WindowEvent e) {
			if (outer != null) {
				outer.frame_windowClosing (e);
			}
			return;
		}

		@Override public void windowDeactivated (WindowEvent e) {
			return;
		}

		@Override public void windowDeiconified (WindowEvent e) {
			return;
		}

		@Override public void windowIconified (WindowEvent e) {
			return;
		}

		@Override public void windowOpened (WindowEvent e) {
			return;
		}
	}

	// The current listener.

	protected MyWindowListener frame_listener = null;




	// Constructor.

	public GUIHtmlViewerDialog () {
		this (true, false);
	}
	
	public GUIHtmlViewerDialog (boolean modalDialog, boolean enableTrace) {
		this.modal = modalDialog;
		this.D = enableTrace;

		this.dialog_status = GUIDialogParameter.DLGSTAT_NONE;
		this.dialog_termcode = GUIDialogParameter.TERMCODE_NONE;
		return;
	}


	// Set dialog size for the next time it is opened.
	// If called while the dialog is open, call refresh_dialog to resize the on-screen dialog.
	
	public void setDialogDimensions (Dimension dialogDims) {

		// If the dialog is displayed, treat it as a pending resize with no change in position

		if (frame != null) {
			this.dialogDims = dialogDims;
			this.pendingResize = true;
		}

		// Otherwise, save the new size and force to default position

		else {
			this.dialogDims = dialogDims;
			this.dialogPosition = null;
		}

		return;
	}


	// Set dialog title for the next time it is opened.
	// If called while the dialog is open, call refresh_dialog to change the on-screen title.
	
	public void setDialogTitle (String dialog_title) {
		this.dialog_title = dialog_title;
		this.changed_dialog_title = true;
		return;
	}


	// Set ok button text for the next time it is opened.
	// If called while the dialog is open, call refresh_dialog to change the on-screen cancel button text.
	// Note: This function cannot add or remove the ok button while the dialog is open.
	
	public void setOkText (String ok_button_text) {
		this.ok_button_text = ok_button_text;
		this.changed_ok_button_text = true;
		return;
	}


	// Set cancel button text for the next time it is opened.
	// If called while the dialog is open, call refresh_dialog to change the on-screen cancel button text.
	// Note: This function cannot add or remove the cancel button while the dialog is open.
	
	public void setCancelText (String cancel_button_text) {
		this.cancel_button_text = cancel_button_text;
		this.changed_cancel_button_text = true;
		return;
	}


	// Set page in the editor.

	private void editor_set_page (URL editor_url) throws IOException {
		editor_pane.setPage (editor_url);
		editor_pane.setCaretPosition (0);
		editor_pane.getCaret().setVisible(false);
		return;
	}


	// Set text in the editor.

	private void editor_set_text (String editor_text) {
		editor_pane.setText (editor_text);
		editor_pane.setCaretPosition (0);
		editor_pane.getCaret().setVisible(false);
		return;
	}


	// Set the editor content.
	// If the URL is non-null, the content is set from the URL.
	// Otherwise, the content is set from the text (which must be HTML).

	protected void set_editor_content (URL editor_url, String editor_text) {
		if (editor_url != null) {
			try {
				editor_set_page (editor_url);
			} catch (IOException e) {
				editor_set_text ("<html><body><h2>Error loading HTML</h2></body></html>");
				e.printStackTrace();
			}
		}
		else if (editor_text != null) {
			editor_set_text (editor_text);
		}
		else {
			editor_set_text ("<html><body><h2>No text was given</h2></body></html>");
		}
		return;
	}


	// Refresh the title and button text, and size, if needed.

	public void refresh_dialog () {

		// If the dialog is open ...

		if (frame != null) {

			// Dialog title text

			if (changed_dialog_title) {
				changed_dialog_title = false;
				if (dialog_title != null) {
					frame.setTitle (dialog_title);
				}
			}

			// OK button text

			if (changed_ok_button_text) {
				changed_ok_button_text = false;
				if (ok_button_text != null && ok_button != null) {
					ok_button.setText (ok_button_text);
				}
			}

			// Cancel button text

			if (changed_cancel_button_text) {
				changed_cancel_button_text = false;
				if (cancel_button_text != null && cancel_button != null) {
					cancel_button.setText (cancel_button_text);
				}
			}

			// Resize dialog if requested

			if (pendingResize) {
				pendingResize = false;
				if (!( dialogDims == null || dialogDims.width < 60 || dialogDims.height < 60 )) {
					frame.setSize(dialogDims);
				}
			}

			// And repaint the dialog if requested

			//if (f_repaint) {
			//	frame.repaint();
			//}
		}

		return;
	}


	// Open the dialog.
	// Returns true if the dialog was created, false if not (because it already exists).

	protected boolean doOpenDialog (Component owner, Component locator, URL editor_url, String editor_text) {

		// If the dialog already exists ...

		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: doOpenDialog: Dialog already exists: modal = " + modal);
			}

			// If the owner has the same window ...

			Window new_frame_window = null;
			if (owner != null) {
				if (owner instanceof Window) {
					new_frame_window = (Window)owner;
				} else {
					new_frame_window = SwingUtilities.windowForComponent (owner);
				}
			}

			if (new_frame_window == frame_window) {

				// Update the content in the existing dialog

				set_editor_content (editor_url, editor_text);

				// Update title and button text and size if needed

				refresh_dialog();

				// If the dialog is modeless, bring it to the front

				if (!( modal )) {
					frame.toFront();
				}

				// Could not create dialog

				return false;
			}

			// Different owner window, close the current dialog

			doCloseDialog (GUIDialogParameter.TERMCODE_REMOVED, true);
		}

		// If dialog size is not available, set default size and position

		if (dialogDims == null || dialogDims.width < 60 || dialogDims.height < 60) {
			dialogDims = new Dimension(600,400);
			dialogPosition = null;
		}

		if (D) {
			if (dialogPosition == null) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: doOpenDialog: Creating dialog: modal = " + modal + ", width = " + dialogDims.width + ", height = " + dialogDims.height);
			} else {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: doOpenDialog: Creating dialog: modal = " + modal + ", width = " + dialogDims.width + ", height = " + dialogDims.height + ", x = " + dialogPosition.x + ", y = " + dialogPosition.y);
			}
		}

		// Set flags indicating no change

		pendingResize = false;
		changed_dialog_title = false;
		changed_ok_button_text = false;
		changed_cancel_button_text = false;

		// Set dialog status

		dialog_status = GUIDialogParameter.DLGSTAT_OPEN;

		// Initialize termination code

		dialog_termcode = GUIDialogParameter.TERMCODE_OPEN;

		// Set up the dialog box

		frame_window = null;
		if (owner != null) {
			if (owner instanceof Window) {
				frame_window = (Window)owner;
			} else {
				frame_window = SwingUtilities.windowForComponent (owner);
			}
		}

		if (frame_window != null) {
			frame = new JDialog(frame_window);
		} else {
			frame = new JDialog();
		}

		frame.setModal(modal);

		frame.setSize(dialogDims);

		if (dialogPosition != null) {
			frame.setLocation(dialogPosition);
		} else if (locator != null) {
			frame.setLocationRelativeTo(locator);
		}

		if (dialog_title != null) {
			frame.setTitle (dialog_title);
		}

		// Add a window listener to be notified when user clicks the X to close the window, and when window is closed

		frame_listener = new MyWindowListener (this);

		frame.setDefaultCloseOperation (WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener (frame_listener);

		// Create and initialize the editor

		editor_pane = new JEditorPane();
		editor_pane.setContentType ("text/html");
		editor_pane.setEditable (false);

		set_editor_content (editor_url, editor_text);

		editor_pane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (e instanceof HTMLFrameHyperlinkEvent) {
						HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
						HTMLDocument doc = (HTMLDocument) editor_pane.getDocument();
						doc.processHTMLFrameHyperlinkEvent(evt);
					} else {
						try {
							editor_set_page(e.getURL());
						} catch (IOException ex) {
							editor_set_text ("<html><body><h2>Error loading linked HTML</h2></body></html>");
							ex.printStackTrace();
						}
					}
				}
			}
		});

		// Add the editor to the dialog box

		frame.getContentPane().setLayout (new GridBagLayout());

		JScrollPane scroll_pane = new JScrollPane (editor_pane);
		frame.getContentPane().add (scroll_pane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));

		// OK button

		ok_button = null;
		if (ok_button_text != null) {
			ok_button = new JButton();
			ok_button.setText(ok_button_text);
			ok_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					button_actionPerformed(e);
					return;
				}
			});
		}

		// Button to cancel the operation and close the dialog

		cancel_button = null;
		if (cancel_button_text != null) {
			cancel_button = new JButton();
			cancel_button.setText(cancel_button_text);
			cancel_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					cancel_actionPerformed(e);
					return;
				}
			});
		}

		// Add 0, 1, or 2 buttons

		if (ok_button != null && cancel_button != null) {
			JPanel button_panel = new JPanel();
			button_panel.setLayout(new BorderLayout(8, 0));
			button_panel.add (ok_button, BorderLayout.WEST);
			button_panel.add (cancel_button, BorderLayout.EAST);
			frame.getContentPane().add(button_panel,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
		}
		else if (ok_button != null) {
			frame.getContentPane().add(ok_button,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
		}
		else if (cancel_button != null) {
			frame.getContentPane().add(cancel_button,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
		}

		// Make the dialog visible

		frame.setVisible(true);		// For modal dialog, does not return until dialog is closed
//		frame.getlo
//		frame.pack();

		// Created the dialog

		return true;
	}


	// Open the dialog.
	// Returns true if the dialog was created, false if not (because it already exists).
	// If a modal dialog is created, this does not return until the dialog is closed.

	public boolean openDialog (Component owner, Component locator, URL editor_url) {

		if (D) {
			System.out.println ("$$$$$ GUIHtmlViewerDialog: openDialog called with URL");
		}

		return doOpenDialog (owner, locator, editor_url, null);
	}


	public boolean openDialog (Component owner, Component locator, String editor_text) {

		if (D) {
			System.out.println ("$$$$$ GUIHtmlViewerDialog: openDialog called with text");
		}

		return doOpenDialog (owner, locator, null, editor_text);
	}


	// Close the dialog, and set the termination code.
	// Return true if dialog closed, false if not open.

	protected boolean doCloseDialog (int the_dialogTermCode, boolean f_dispose) {

		// If dialog exists ...

		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: doCloseDialog: Closing dialog: termCode = " + the_dialogTermCode);
			}

			// Save the termination code

			dialog_termcode = the_dialogTermCode;

			// Set dialog status

			dialog_status = GUIDialogParameter.DLGSTAT_CLOSED;

			// Save the dialog size and position

			if (pendingResize) {
				pendingResize = false;
				dialogPosition = null;	// force to default position as if resize occurred while dialog is closed
			} else {
				dialogDims = frame.getSize();
				dialogPosition = frame.getLocation();
			}

			// Release resources

			JDialog saved_frame = frame;

			frame = null;
			frame_listener.set_outer (null);
			frame_listener = null;
			frame_window = null;
			ok_button = null;
			cancel_button = null;
			editor_pane = null;

			// Dispose of the dialog if requested

			if (f_dispose) {
				saved_frame.dispose();
			}

			// Success

			return true;
		}

		// Otherwise, dialog is not open or already disposed ...

		if (D) {
			System.out.println ("$$$$$ GUIHtmlViewerDialog: doCloseDialog: Dialog already closed: termCode = " + the_dialogTermCode);
		}

		return false;
	}


	// Close the dialog, and set the termination code.
	// Return true if dialog closed, false if not open or already disposed.

	public boolean closeDialog (int the_dialogTermCode) {

		if (D) {
			System.out.println ("$$$$$ GUIHtmlViewerDialog: closeDialog called: termCode = " + the_dialogTermCode);
		}

		return doCloseDialog (the_dialogTermCode, true);
	}


	/**
	 * This function is called when user clicks the OK button.
	 * @param e
	 */
	protected void button_actionPerformed(ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: OK button pressed");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_OK, true);
		}
		return;
	}


	/**
	 * This function is called when user clicks the CANCEL button.
	 * @param e
	 */
	protected void cancel_actionPerformed(ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: CANCEL button pressed");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_CANCEL, true);
		}
		return;
	}


	/**
	 * This function is called when user closes the dialog by clicking the X.
	 * @param e
	 */
	protected void frame_windowClosing(WindowEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: Close window clicked");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_CLOSED, true);
		}
		return;
	}


	/**
	 * This function is called after the dialog is closed.
	 * @param e
	 */
	protected void frame_windowClosed(WindowEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: Window closed by system");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_SYSTEM, false);
		}
		return;
	}

}
