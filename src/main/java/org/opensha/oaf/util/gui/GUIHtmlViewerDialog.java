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
import javax.swing.text.Document;


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

	// Text to appear on the back button (cannot be null).
	//private static final String DEF_BACK_BUTTON_TEXT = "<";
	//private static final String DEF_BACK_BUTTON_TEXT = "\u2190";		// arrow
	//private static final String DEF_BACK_BUTTON_TEXT = "\u21A9";		// hook arrow
	private static final String DEF_BACK_BUTTON_TEXT = "\u2B9C";		// equilateral arrowhead
	private String back_button_text = DEF_BACK_BUTTON_TEXT;

	// Text to appear on the forward button (cannot be null).
	//private static final String DEF_FORWARD_BUTTON_TEXT = ">";
	//private static final String DEF_FORWARD_BUTTON_TEXT = "\u2192";	// arrow
	//private static final String DEF_FORWARD_BUTTON_TEXT = "\u21AA";	// hook arrow
	private static final String DEF_FORWARD_BUTTON_TEXT = "\u2B9E";		// equilateral arrowhead
	private String forward_button_text = DEF_FORWARD_BUTTON_TEXT;

	// Set to enable the navigation bar.
	private boolean f_nav_enable = false;

	// Set to clear the navigation list whenever the dialog is closed.
	private boolean f_nav_clear_on_close = true;

	// Set to clear the navigation list on each call to openDialog.
	private boolean f_nav_clear_on_open = false;

	// The navigation list.
	// Each element of the list must be either URL or String, or null.
	private ArrayList<Object> nav_list = new ArrayList<Object>();

	// Index into the navigation list, can range from 0 to nav_list.size().
	// It points one past the currently displayed page.
	private int nav_index = 0;

	// The back button, or null if not allocated.
	protected JButton back_button = null;

	// The forward button, or null if not allocated.
	protected JButton forward_button = null;

	// True if the current editor content was supplies using setText.
	private boolean f_used_set_text = false;




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

		if (f_used_set_text) {
			f_used_set_text = false;

			// There is a problem with JEditorPane in that if setPage is used after setText,
			// then the display may not be updated, particularly if setPage is redisplaying
			// the last URL that was used (https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4412125).

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: Applying workaround for setText issue");
			}

			// This is the workaround recommended by the page linked above.
			Document doc = editor_pane.getDocument();
			doc.putProperty (Document.StreamDescriptionProperty, null);

			// This is the workaround recommended by Copilot.
			//editor_pane.setContentType ("text/html");
			//editor_pane.setDocument (editor_pane.getEditorKit().createDefaultDocument());
		}

		editor_pane.setPage (editor_url);
		editor_pane.setCaretPosition (0);
		editor_pane.getCaret().setVisible(false);
		return;
	}


	// Set text in the editor.

	private void editor_set_text (String editor_text) {
		f_used_set_text = true;
		editor_pane.setText (editor_text);
		editor_pane.setCaretPosition (0);
		editor_pane.getCaret().setVisible(false);
		return;
	}


	// Set error text in the editor.

	private void editor_set_error_text (String editor_text) {
		f_used_set_text = true;
		editor_pane.setText (editor_text);
		editor_pane.setCaretPosition (0);
		editor_pane.getCaret().setVisible(false);
		return;
	}


	// Set the editor content.
	// If the URL is non-null, the content is set from the URL.
	// Otherwise, the content is set from the text (which must be HTML).
	// An error page is displayed if both parameters are null, or if there
	// is an error retrieving text from the URL.

	protected void set_editor_content (URL editor_url, String editor_text) {
		if (editor_url != null) {
			try {
				editor_set_page (editor_url);
			} catch (IOException e) {
				editor_set_error_text ("<html><body><h2>Error loading HTML from URL</h2></body></html>");
				e.printStackTrace();
			}
		}
		else if (editor_text != null) {
			editor_set_text (editor_text);
		}
		else {
			editor_set_error_text ("<html><body><h2>No text is available</h2></body></html>");
		}
		return;
	}


	// Set the editor content and add page to navigation.
	// If the URL is non-null, the content is set from the URL.
	// Otherwise, the content is set from the text (which must be HTML).
	// An error page is displayed if both parameters are null, or if there
	// is an error retrieving text from the URL.
	// After displaying the page, it is added to navigation.

	protected void set_editor_content_and_nav (URL editor_url, String editor_text) {
		set_editor_content (editor_url, editor_text);
		nav_new_page (editor_url, editor_text);
		return;
	}


	// Set the editor content.
	// The content must be either URL or String.
	// This is intended for use by the navigation code to implement back/forward.

	protected void set_editor_content (Object editor_content) {
		//if (D) {
		//	System.out.println ("$$$$$ GUIHtmlViewerDialog: Displaying navigation page: " + debug_content_to_string(editor_content));
		//}

		if (editor_content != null && editor_content instanceof URL) {
			set_editor_content ((URL)editor_content, null);
		}
		else if (editor_content != null && editor_content instanceof String) {
			set_editor_content (null, (String)editor_content);
		}
		else {
			set_editor_content (null, null);
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

		// Clear navigation list if desired

		if (f_nav_clear_on_open) {
			nav_clear_list();
		}

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

				set_editor_content_and_nav (editor_url, editor_text);

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
		f_used_set_text = false;

		set_editor_content_and_nav (editor_url, editor_text);

		editor_pane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (e instanceof HTMLFrameHyperlinkEvent) {
						HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
						HTMLDocument doc = (HTMLDocument) editor_pane.getDocument();
						doc.processHTMLFrameHyperlinkEvent(evt);
					} else {
						set_editor_content_and_nav (e.getURL(), null);
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

		// If we want the navigation pane ...

		if (f_nav_enable) {

			// BACK button

			back_button = new JButton();
			back_button.setText(back_button_text);
			back_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					back_actionPerformed(e);
					return;
				}
			});

			// FORWARD button

			forward_button = new JButton();
			forward_button.setText(forward_button_text);
			forward_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					forward_actionPerformed(e);
					return;
				}
			});

			// Force buttons to have the same size

			unifyButtonSizes (back_button, forward_button);

			// Navigation panel

			JPanel nav_panel = new JPanel();
			nav_panel.setLayout(new GridBagLayout());

			// Add the BACK button

			int gridx = 0;
			nav_panel.add (back_button, new GridBagConstraints (gridx, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 4), 0, 0));
			++gridx;

			// Add the OK button

			if (ok_button != null) {
				nav_panel.add (ok_button, new GridBagConstraints (gridx, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0));
				++gridx;
			}

			// Add the CANCEL button

			if (cancel_button != null) {
				nav_panel.add (cancel_button, new GridBagConstraints (gridx, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0));
				++gridx;
			}

			// Add the FORWARD button

			nav_panel.add (forward_button, new GridBagConstraints (gridx, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0));
			++gridx;

			// Add the navigation panel to the main panel

			frame.getContentPane().add (nav_panel, new GridBagConstraints (0, 1, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));

			// Adjust the enable state of the navigation buttons

			nav_adjust_button_enable();
		}

		// Otherwise, no navigation pane ...

		else {

			// No navigation buttons

			back_button = null;
			forward_button = null;

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

			back_button = null;
			forward_button = null;
			if (f_nav_clear_on_close) {
				nav_clear_list();
			}

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


	// Configure the navigation bar.
	// Parameters:
	//  f_nav_enable = True to enable navigation bar (takes effect the next time the dialog is created).
	//  f_nav_clear_on_close = True to clear navigation list whenever dialog is closed (takes effect immediately).
	//  f_nav_clear_on_close = True to clear navigation list whenever openDialog is called (takes effect immediately).
	//  back_button_text = Text to appear on the back button, or null for default text (takes effect the next time the dialog is created).
	//  forward_button_text = Text to appear on the forward button, or null for default text (takes effect the next time the dialog is created).

	public void set_nav_config (boolean f_nav_enable, boolean f_nav_clear_on_close, boolean f_nav_clear_on_open, String back_button_text, String forward_button_text) {
		this.f_nav_enable = f_nav_enable;
		this.f_nav_clear_on_close = f_nav_clear_on_close;
		this.f_nav_clear_on_open = f_nav_clear_on_open;
		this.back_button_text = ((back_button_text == null) ? DEF_BACK_BUTTON_TEXT : back_button_text);
		this.forward_button_text = ((forward_button_text == null) ? DEF_FORWARD_BUTTON_TEXT : forward_button_text);
		return;
	}


	// Given two buttons, set them to have the same preferred and minimum sizes.

	protected static void unifyButtonSizes (JButton button1, JButton button2) {

		// Get original preferred sizes
		Dimension pref1 = button1.getPreferredSize();
		Dimension pref2 = button2.getPreferredSize();

		// Get original minimum sizes
		Dimension min1 = button1.getMinimumSize();
		Dimension min2 = button2.getMinimumSize();

		// Calculate maximum preferred size
		int maxPrefWidth = Math.max(pref1.width, pref2.width);
		int maxPrefHeight = Math.max(pref1.height, pref2.height);
		Dimension newPrefSize = new Dimension(maxPrefWidth, maxPrefHeight);

		// Calculate maximum minimum size
		int maxMinWidth = Math.max(min1.width, min2.width);
		int maxMinHeight = Math.max(min1.height, min2.height);
		Dimension newMinSize = new Dimension(maxMinWidth, maxMinHeight);

		// Apply new sizes to both buttons
		button1.setPreferredSize(newPrefSize);
		button2.setPreferredSize(newPrefSize);

		button1.setMinimumSize(newMinSize);
		button2.setMinimumSize(newMinSize);

		return;
	}


	// Return true if navigation can go back.

	protected final boolean nav_has_back () {
		return nav_index > 1;
	}

	
	// Return true if navigation can go forward.

	protected final boolean nav_has_forward () {
		return nav_index < nav_list.size();
	}


	// Set the enabled state of the navigation buttons.

	protected final void nav_adjust_button_enable () {
		if (back_button != null) {
			back_button.setEnabled (nav_has_back());
		}
		if (forward_button != null) {
			forward_button.setEnabled (nav_has_forward());
		}
		return;
	}


	// This function is called when user clicks the BACK button.

	protected void back_actionPerformed (ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: BACK button pressed, index = " + nav_index + ", size = " + nav_list.size());
			}

			if (nav_has_back()) {
				--nav_index;
				set_editor_content (nav_list.get (nav_index - 1));
				nav_adjust_button_enable();
			}
		}
		return;
	}


	// This function is called when user clicks the FORWARD button.

	protected void forward_actionPerformed (ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: FORWARD button pressed, index = " + nav_index + ", size = " + nav_list.size());
			}

			if (nav_has_forward()) {
				++nav_index;
				set_editor_content (nav_list.get (nav_index - 1));
				nav_adjust_button_enable();
			}
		}
		return;
	}


	// Return true if the two contents are the same.
	// Each content must be URL or String, or null.
	// Nulls are handled correctly (null is only equal to another null).
	// Implementation note: A simple technique would be to use equals().
	// but applying equals() to URLs requires name resolution which can take time.
	// So we compare URLs by comparing their string representations.

	protected static boolean is_same_content (Object c1, Object c2) {
		if (c1 == null) {
			if (c2 == null) {
				return true;
			}
			return false;
		}
		if (c2 == null) {
			return false;
		}

		if (c1 instanceof URL && c2 instanceof URL) {
			return c1.toString().equals (c2.toString());
		}

		if (c1 instanceof String && c2 instanceof String) {
			return c1.equals (c2);
		}

		return false;
	}


	// Store a new page in the navigation list.
	// The content must be URL or String, or null.

	protected void nav_new_page (Object content) {
		//if (D) {
		//	System.out.println ("$$$$$ GUIHtmlViewerDialog: Adding navigation page: " + debug_content_to_string(content));
		//}

		while (nav_index < nav_list.size()) {
			nav_list.remove (nav_list.size() - 1);
		}
		if (!( nav_index > 0 && is_same_content (nav_list.get (nav_index - 1), content) )) {	// Don't add duplicates

			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: Adding navigation page, index = " + nav_index + ", size = " + nav_list.size());
			}

			nav_list.add (content);
			++nav_index;
		}
		else {
			if (D) {
				System.out.println ("$$$$$ GUIHtmlViewerDialog: Duplicate navigation page, index = " + nav_index + ", size = " + nav_list.size());
			}
		}
		nav_adjust_button_enable();
		return;
	}


	// Store a new page in the navigation list.
	// If the URL is non-null, the content is the URL.
	// Otherwise, the content is the text (which must be HTML).

	protected void nav_new_page (URL content_url, String content_text) {
		if (content_url != null) {
			nav_new_page (content_url);
		}
		else if (content_text != null) {
			nav_new_page (content_text);
		}
		else {
			nav_new_page (null);
		}
		return;
	}


	// Clear the navigation list.

	protected void nav_clear_list () {
		nav_list.clear();
		nav_index = 0;
		nav_adjust_button_enable();
		return;
	}


	// Convert content to string for debugging (only).

	private String debug_content_to_string (Object content) {
		if (content == null) {
			return "<null>";
		}
		if (content instanceof String) {
			String text = ((String)content);
			return "String: " + text;
		}
		if (content instanceof URL) {
			URL url = ((URL)content);
			return "URL: " + url.toString();
		}
		return "Unknown: " + (content.toString());
	}


}
