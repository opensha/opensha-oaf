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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.SwingUtilities;

import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
//import org.opensha.commons.param.editor.AbstractParameterEditor;
import org.opensha.commons.param.event.ParameterChangeEvent;
import org.opensha.commons.param.event.ParameterChangeListener;
//import org.opensha.commons.param.impl.ParameterListParameter;
//import org.opensha.commons.param.editor.impl.ParameterListParameterEditor;
import org.opensha.commons.param.editor.impl.ParameterListEditor;


/**
 * GUI parameter editor that contains a list of parameters.
 * Author: Michael Barall 07/29/2021.
 *
 * This code is copied and then extended from OpenSHA class ParameterListParameterEditor.
 *
 * This parameter appears as a button.  When the button is pressed, a dialog opens
 * which displays all the parameters in the list.  The dialog also has a close button.
 *
 * This is extended from the OpenSHA version in that it permits setting various text
 * strings and overriding the remembered dialog size and position, as well as positioning
 * the dialog over the GUI window instead of the top left corner of the screen.
 * It also properly supports modeless dialogs, fixes a number of issues, and includes
 * an option to print debugging messages.
 */


public class GUIParameterListParameterEditor extends GUIAbstractParameterEditor<ParameterList> implements
	ActionListener, ParameterChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Class name for debugging. */
	protected final static String C = "GUIParameterListParameterEditor";

	/** If true print out debug statements. */
	protected boolean D = false;

	// Editor to hold all the parameters in this parameter.
	protected ParameterListEditor editor = null;
	
	// The button that activates the dialog.
	private JButton button = null;

	// Instance for the framee to show the all parameters in this editor.
	protected JDialog frame = null;

	// The button that closes the dialog, with OK, or null if not allocated.
	protected JButton ok_button = null;

	// The button that closes the dialog, with CANCEL, or null if not allocated.
	protected JButton cancel_button = null;

	// Set if a parameter has been changed.
	protected boolean parameterChangeFlag = false;

	// Set to enable firing a parameter change event.
	protected boolean armFireChangeFlag = false;

	// Set if the dialog has already been disposed.
	protected boolean frameDisposed = false;

	// Set if a resize command was received while the dialog is open, but not processed yet.
	protected boolean pendingResize = false;
	
	// Set to display this parameter as a button that opens a dialog.
	private boolean useButton = true;
	
	// Set to make the dialog modal.
	private boolean modal = true;
	
	// Dialog size, if null or too small then use a default size.
	private Dimension dialogDims = null;

	// Dialog position, if null then use a default position.
	private Point dialogPosition = null;

	// The parameters that we are currently watching, or null if none.
	private ArrayList<Parameter<?>> watchedParams = null;

	// The parameters that we are currently watching, that are dialogs, or null if none.
	private ArrayList<GUIDialogParameter> watchedDialogParams = null;




	// Remove all listeners.

	protected void removeListeners () {

		// If we are currently watching any parameters, remove their listeners

		if (watchedParams != null) {
			for (Parameter<?> w_param : watchedParams) {
				w_param.removeParameterChangeListener(this);
			}
			watchedParams = null;
		}

		return;
	}




	// Update all listeners.
	// Note: This function allocates new objects for watchedParams and watchedDialogParams,
	// so we don't retain pointers to parameters that are no longer in use.

	protected void updateListeners (ParameterList paramList) {

		// If we are currently watching any parameters, remove their listeners

		removeListeners();

		// Add listeners for the parameters currently in the list

		watchedParams = new ArrayList<Parameter<?>>();
		watchedDialogParams = new ArrayList<GUIDialogParameter>();

		ListIterator<Parameter<?>> it = paramList.getParametersIterator();
		while(it.hasNext()) {

			// Add a listener for the parameter

			Parameter<?> param = it.next();
			param.addParameterChangeListener(this);
			watchedParams.add(param);

			// If the parameter is a dialog, add to the list

			if (param instanceof GUIDialogParameter) {
				watchedDialogParams.add ((GUIDialogParameter)param);
			}
		}

		return;
	}




	// Close open dialogs.
	// Parameters:
	//  params = List of dialog parameters to close, can be null if none.
	//  excluded = List of dialog parameters not to close, or null if none.
	//  termCode = Termination code to use when closing dialogs.

	protected static void closeOpenDialogs (ArrayList<GUIDialogParameter> params, ArrayList<GUIDialogParameter> excluded, final int termCode) {
		if (params != null) {
			for (final GUIDialogParameter param : params) {

				// Check if this parameter is excluded
				// We use a linear search because typically the list of excluded dialogs is very small

				boolean ok = true;
				if (excluded != null) {
					for (GUIDialogParameter x_param : excluded) {
						if (x_param == param) {
							ok = false;
							break;
						}
					}
				}

				// If close is needed, do it asynchronously

				if (ok) {
					if (param.getDialogStatus() == GUIDialogParameter.DLGSTAT_OPEN) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override public void run() {
								param.closeDialog(termCode);
							}
						});
					}
				}
			}
		}
		return;
	}




	// Constructor.

	public GUIParameterListParameterEditor(Parameter<ParameterList> model) {
		this(model, true, true, false);
	}
	
	public GUIParameterListParameterEditor(Parameter<ParameterList> model, boolean useButton, boolean modalDialog, boolean enableTrace) {
		super(model);
		this.useButton = useButton;
		this.modal = modalDialog;
		this.D = enableTrace;
		refreshParamEditor();
		return;
	}


	// Set dialog size for the next time it is opened.
	// If called while the dialog is open, call refreshParamEditor to resize the on-screen dialog.
	
	public void setDialogDimensions(Dimension dialogDims) {

		// If the dialog is displayed, treat it as a pending resize with no change in position

		if (frame != null && !frameDisposed) {
			this.dialogDims = dialogDims;
			pendingResize = true;
		}

		// Otherwise, save the new size and force to default position

		else {
			this.dialogDims = dialogDims;
			this.dialogPosition = null;
		}

		return;
	}


	/**
	 * It enables/disables the editor according to whether user is allowed to
	 * fill in the values.
	 */
	@Override
	public void setEnabled(boolean isEnabled) {
		if (useButton) {
			this.button.setEnabled(isEnabled);
		} else {
			this.editor.setEnabled(isEnabled);
		}

		// If disabling while dialog is open, close the dialog

		if (!( isEnabled )) {
			if (frame != null) {

				if (D) {
					System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): Parameter disabled while dialog is open");
				}

				doCloseDialog (GUIDialogParameter.TERMCODE_DISABLED);
			}
		}
		return;
	}


//	/**
//	 * sets the title for this editor
//	 * @param title
//	 */
//	public void setEditorTitle(String title){
//		editor.setTitle(title);
//	}


	/**
	 *  Hides or shows one of the ParameterEditors in the ParameterList. setting
	 *  the boolean parameter to true shows the panel, setting it to false hides
	 *  the panel. <p>
	 *
	 * @param  parameterName  The parameter editor to toggle on or off.
	 * @param  visible      The boolean flag. If true editor is visible.
	 */
	public void setParameterVisible( String parameterName, boolean visible ) {
		editor.setParameterVisible(parameterName, visible);
	}


	/**
	 * Keeps track when parameter has been changed
	 * @param event
	 */
	@Override
	public void parameterChange(ParameterChangeEvent event){

		// Block notfications where old and new values are both null.
		// It is apparently a bug in AbstractParameter that such notifications occur.
		// Such notifications can occur even if the control is disabled.

		if (event.getOldValue() == null && event.getNewValue() == null) {
			return;
		}

		// Remember that a change occurred

		parameterChangeFlag = true;
		return;
	}


	// Open the dialog.
	// Returns true if the dialog was created, false if not (because it already exists).

	protected boolean doOpenDialog () {

		// If the dialog already exists ...

		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): doOpenDialog: Dialog already exists: modal = " + modal + ", disposed = " + frameDisposed);
			}

			// If the dialog is modeless and not disposed, bring it to the front

			if ((!modal) && (!frameDisposed)) {
				frame.toFront();
			}

			// Could not create dialog

			return false;
		}

		// If dialog size is not available, set default size and position

		if (dialogDims == null || dialogDims.width < 60 || dialogDims.height < 60) {
			dialogDims = new Dimension(300,400);
			dialogPosition = null;
		}

		if (D) {
			if (dialogPosition == null) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): doOpenDialog: Creating dialog: modal = " + modal + ", width = " + dialogDims.width + ", height = " + dialogDims.height);
			} else {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): doOpenDialog: Creating dialog: modal = " + modal + ", width = " + dialogDims.width + ", height = " + dialogDims.height + ", x = " + dialogPosition.x + ", y = " + dialogPosition.y);
			}
		}

		// Set flag indicating no parameters have changed while dialog is open, and not disposed yet

		parameterChangeFlag = false;
		armFireChangeFlag = false;
		frameDisposed = false;
		pendingResize = false;

		// Set dialog status

		((GUIParameterListParameter)getParameter()).setDialogStatus(GUIDialogParameter.DLGSTAT_OPEN);

		// Initialize termination code

		((GUIParameterListParameter)getParameter()).setDialogTermCode(GUIDialogParameter.TERMCODE_OPEN);

		// Set up the dialog box

		Component ownerComponent = ((GUIParameterListParameter)getParameter()).getOwnerComponent();

		Window owner = null;
		if (ownerComponent != null) {
			owner = SwingUtilities.windowForComponent(ownerComponent);
		}
		if (owner == null && button != null) {
			owner = SwingUtilities.windowForComponent(button);
		}
		if (owner != null) {
			frame = new JDialog(owner);
		} else {
			frame = new JDialog();
		}

		frame.setModal(modal);
		frame.setSize(dialogDims);
		if (dialogPosition != null) {
			frame.setLocation(dialogPosition);
		} else if (ownerComponent != null) {
			frame.setLocationRelativeTo(ownerComponent);
		} else if (button != null) {
			frame.setLocationRelativeTo(button);
		}
		frame.setTitle(((GUIParameterListParameter)getParameter()).getDialogTitleText());

		// Add a window listener to be notified when user clicks the X to close the window, and when window is closed

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowListener() {
			@Override public void windowActivated(WindowEvent e) {return;}
			@Override public void windowClosed(WindowEvent e) {frame_windowClosed(e); return;}
			@Override public void windowClosing(WindowEvent e) {frame_windowClosing(e); return;}
			@Override public void windowDeactivated(WindowEvent e) {return;}
			@Override public void windowDeiconified(WindowEvent e) {return;}
			@Override public void windowIconified(WindowEvent e) {return;}
			@Override public void windowOpened(WindowEvent e) {return;}
		});

		// Add the parameter list to the dialog box

		frame.getContentPane().setLayout(new GridBagLayout());
		frame.getContentPane().add(editor,new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));

		// Button to update the parameters and close the dialog

		ok_button = null;
		if (((GUIParameterListParameter)getParameter()).useOkButton()) {
			ok_button = new JButton();
			ok_button.setText(((GUIParameterListParameter)getParameter()).getOkButtonText());
			Color fg = ((GUIParameterListParameter)getParameter()).getOkButtonForeground();
			if (fg != null) {
				ok_button.setForeground(fg);
			} else {
				ok_button.setForeground(new Color(80,80,133));
			}
			ok_button.setEnabled(((GUIParameterListParameter)getParameter()).getOkButtonEnabled());
			ok_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					button_actionPerformed(e);
					return;
				}
			});
		}

		// Button to cancel the operation and close the dialog

		cancel_button = null;
		if (((GUIParameterListParameter)getParameter()).useCancelButton()) {
			cancel_button = new JButton();
			cancel_button.setText(((GUIParameterListParameter)getParameter()).getCancelButtonText());
			Color fg = ((GUIParameterListParameter)getParameter()).getCancelButtonForeground();
			if (fg != null) {
				cancel_button.setForeground(fg);
			} else {
				cancel_button.setForeground(new Color(80,80,133));
			}
			cancel_button.setEnabled(((GUIParameterListParameter)getParameter()).getCancelButtonEnabled());
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
			frame.getContentPane().add(button_panel,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
					,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
		}
		else if (ok_button != null) {
			frame.getContentPane().add(ok_button,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
					,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
		}
		else if (cancel_button != null) {
			frame.getContentPane().add(cancel_button,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
					,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
		}

		// Make the dialog visible

		frame.setVisible(true);		// For modal dialog, does not return until dialog is closed
//		frame.getlo
//		frame.pack();

		// Created the dialog

		return true;
	}


	// Refresh just the buttons, if they exist.

	public void refreshButtons () {

		// Button to update the parameters and close the dialog

		if (ok_button != null) {
			ok_button.setText(((GUIParameterListParameter)getParameter()).getOkButtonText());
			Color fg = ((GUIParameterListParameter)getParameter()).getOkButtonForeground();
			if (fg != null) {
				ok_button.setForeground(fg);
			} else {
				ok_button.setForeground(new Color(80,80,133));
			}
			ok_button.setEnabled(((GUIParameterListParameter)getParameter()).getOkButtonEnabled());
		}

		// Button to cancel the operation and close the dialog

		if (cancel_button != null) {
			cancel_button.setText(((GUIParameterListParameter)getParameter()).getCancelButtonText());
			Color fg = ((GUIParameterListParameter)getParameter()).getCancelButtonForeground();
			if (fg != null) {
				cancel_button.setForeground(fg);
			} else {
				cancel_button.setForeground(new Color(80,80,133));
			}
			cancel_button.setEnabled(((GUIParameterListParameter)getParameter()).getCancelButtonEnabled());
		}

	}


	// Open the dialog.
	// Returns true if the dialog was created, false if not (because it already exists).
	// If a modal dialog is created, this does not return until the dialog is closed (at
	// which time the termination code will have been set, but it is not clear if the
	// change notification is guaranteed to have been sent).

	public boolean openDialog () {

		if (D) {
			System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): openDialog called");
		}

		return doOpenDialog();
	}


	/**
	 * This function is called when the user clicks the GUIParameterListParameterEditor button.
	 *
	 * @param e
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (button != null && e.getSource() == button) {

			if (D) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): Activation button pressed");
			}

			doOpenDialog();
		}
		return;
	}


	// Close the dialog, and set the termination code.
	// Return true if dialog closed, false if not open or already disposed.

	protected boolean doCloseDialog (int the_dialogTermCode) {

		// If dialog exists and is not disposed ...

		if (frame != null && !frameDisposed) {

			if (D) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): doCloseDialog: Closing dialog: termCode = " + the_dialogTermCode);
			}

			// Enable parameter change notifiction to be fired
			// Note: One possibility is to enable or disable based on termination code, but if disabled then
			// the original parameter values should be restored, and there is no convenient way to do that.

			armFireChangeFlag = true;

			// Indicate dialog disposed

			frameDisposed = true;

			// Save the termination code

			((GUIParameterListParameter)getParameter()).setDialogTermCode(the_dialogTermCode);

			// Set dialog status

			((GUIParameterListParameter)getParameter()).setDialogStatus(GUIDialogParameter.DLGSTAT_CLOSING);

			// Save the dialog size and position

			if (pendingResize) {
				pendingResize = false;
				dialogPosition = null;	// force to default position as if resize occurred while dialog is closed
			} else {
				dialogDims = frame.getSize();
				dialogPosition = frame.getLocation();
			}

			// Close any open secondary dialogs

			closeOpenDialogs (watchedDialogParams, null, GUIDialogParameter.TERMCODE_PARENT);

			// Dispose of the dialog

			frame.dispose();

			// Success

			return true;
		}

		// Otherwise, dialog is not open or already disposed ...

		if (D) {
			System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): doCloseDialog: Dialog already closed: termCode = " + the_dialogTermCode);
		}

		return false;
	}


	// Close the dialog, and set the termination code.
	// Return true if dialog closed, false if not open or already disposed.

	public boolean closeDialog (int the_dialogTermCode) {

		if (D) {
			System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): closeDialog called: termCode = " + the_dialogTermCode);
		}

		return doCloseDialog (the_dialogTermCode);
	}


	/**
	 * This function is called when user clicks the OK button to update the parameters.
	 * @param e
	 */
	protected void button_actionPerformed(ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): OK button pressed");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_OK);
		}
		return;
	}


	/**
	 * This function is called when user clicks the CANCEL button to cancel the operation.
	 * @param e
	 */
	protected void cancel_actionPerformed(ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): CANCEL button pressed");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_CANCEL);
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
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): Close window clicked");
			}

			doCloseDialog (GUIDialogParameter.TERMCODE_CLOSED);
		}
		return;
	}


	/**
	 * This function is called after the dialog is closed.
	 * @param e
	 */
	protected void frame_windowClosed(WindowEvent e) {
		if (frame != null) {

			// If we didn't call dispose ...

			if (!( frameDisposed )) {

				if (D) {
					System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): Window closed by system");
				}

				// Enable parameter change notifiction to be fired

				armFireChangeFlag = true;

				// Indicate dialog disposed

				frameDisposed = true;

				// Save the termination code

				((GUIParameterListParameter)getParameter()).setDialogTermCode(GUIDialogParameter.TERMCODE_SYSTEM);

				// Save the dialog size and position

				if (pendingResize) {
					pendingResize = false;
					dialogPosition = null;	// force to default position as if resize occurred while dialog is closed
				} else {
					dialogDims = frame.getSize();
					dialogPosition = frame.getLocation();
				}

				// Close any open secondary dialogs

				closeOpenDialogs (watchedDialogParams, null, GUIDialogParameter.TERMCODE_PARENT);

			} else {

				if (D) {
					System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): Window closed");
				}

			}

			// Set dialog status

			((GUIParameterListParameter)getParameter()).setDialogStatus(GUIDialogParameter.DLGSTAT_CLOSED);

			// If any parameters have changed, send a notification
			// Note: This must be done in the window-closed handler, and not earlier.
			// Otherwise, it is possible to miss a parameter change notification, for example,
			// if the user edits a parameter and then closes the dialog by clicking the X
			// while the parameter still has focus.

			if (parameterChangeFlag && armFireChangeFlag) {
				final Parameter<ParameterList> param = getParameter();
				//ParameterList paramList = editor.getParameterList();
				//param.setValue(paramList);
				// this is needed because although the list hasn't changed, values inside of it have.

				//param.firePropertyChange(new ParameterChangeEvent(param, param.getName(), param.getValue(), param.getValue()));
				SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						param.firePropertyChange(new ParameterChangeEvent(param, param.getName(), param.getValue(), param.getValue()));
					}
				});
				
				parameterChangeFlag = false;
				armFireChangeFlag = false;
			}

			ok_button = null;
			cancel_button = null;
			frame = null;
		}

		return;
	}


	@Override
	public boolean isParameterSupported(Parameter<ParameterList> param) {
		if (param == null)
			return false;
		
		if (!(param instanceof GUIParameterListParameter))
			return false;
		
		return true;
	}


	@Override
	protected JComponent buildWidget() {

		if (D) {
			System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): buildWidget called");
		}

		// Set up listeners for parameters in the list, and set them as the independent parameters

		ArrayList<GUIDialogParameter> old_watchedDialogParams = watchedDialogParams;
		ParameterList paramList = getParameter().getValue();
		getParameter().setIndependentParameters(paramList);
		updateListeners(paramList);

		// Set up the editor for the parameter list

		editor = new ParameterListEditor(paramList);
		//editor.setTitle("Set "+getParameter().getName());
		editor.setTitle(((GUIParameterListParameter)getParameter()).getListTitleText());
		//editor.refreshParamEditor();

		// If the dialog is displayed, repaint it

		if (frame != null && !frameDisposed) {

			// Set text in dialog

			frame.setTitle(((GUIParameterListParameter)getParameter()).getDialogTitleText());
			if (ok_button != null) {
				ok_button.setText(((GUIParameterListParameter)getParameter()).getOkButtonText());
			}
			if (cancel_button != null) {
				cancel_button.setText(((GUIParameterListParameter)getParameter()).getCancelButtonText());
			}

			// Resize dialog if requested

			if (pendingResize) {
				pendingResize = false;
				if (!( dialogDims == null || dialogDims.width < 60 || dialogDims.height < 60 )) {
					frame.setSize(dialogDims);
				}
			}

			// Close any open secondary dialogs that were removed from the dialog

			closeOpenDialogs (old_watchedDialogParams, watchedDialogParams, GUIDialogParameter.TERMCODE_REMOVED);

			// And repaint the dialog

			frame.repaint();
		}
		
		// Set up the button that brings up the list

		if (useButton) {
			//button = new JButton(getParameter().getName());
			button = new JButton(((GUIParameterListParameter)getParameter()).getButtonText());
			Color fg = ((GUIParameterListParameter)getParameter()).getButtonForeground();
			if (fg != null) {
				button.setForeground(fg);
			}
			button.addActionListener(this);
			return button;
		}

		// If not using the button, just return the parameter list editor

		return editor;
	}


	@Override
	protected JComponent updateWidget() {

		if (D) {
			System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): updateWidget called");
		}

		// Set up listeners for parameters in the list, and set them as the independent parameters

		ArrayList<GUIDialogParameter> old_watchedDialogParams = watchedDialogParams;
		ParameterList paramList = getParameter().getValue();
		getParameter().setIndependentParameters(paramList);
		updateListeners(paramList);

		// Set up the editor for the parameter list

		if (editor == null) {
			editor = new ParameterListEditor(paramList);
		} else {
			editor.setParameterList(paramList);
		}
		editor.setTitle(((GUIParameterListParameter)getParameter()).getListTitleText());
		//editor.refreshParamEditor();

		// If the dialog is displayed, repaint it

		if (frame != null && !frameDisposed) {

			// Set text in dialog

			frame.setTitle(((GUIParameterListParameter)getParameter()).getDialogTitleText());
			if (ok_button != null) {
				ok_button.setText(((GUIParameterListParameter)getParameter()).getOkButtonText());
			}
			if (cancel_button != null) {
				cancel_button.setText(((GUIParameterListParameter)getParameter()).getCancelButtonText());
			}

			// Resize dialog if requested

			if (pendingResize) {
				pendingResize = false;
				frame.setSize(dialogDims);
			}

			// Close any open secondary dialogs that were removed from the dialog

			closeOpenDialogs (old_watchedDialogParams, watchedDialogParams, GUIDialogParameter.TERMCODE_REMOVED);

			// And repaint the dialog

			frame.repaint();
		}
		
		// Set up the button that brings up the list
		
		if (useButton) {
			if (button == null) {
				//button = new JButton(getParameter().getName());
				button = new JButton(((GUIParameterListParameter)getParameter()).getButtonText());
				Color fg = ((GUIParameterListParameter)getParameter()).getButtonForeground();
				if (fg != null) {
					button.setForeground(fg);
				}
				button.addActionListener(this);
			} else {
				//button.setText(getParameter().getName());
				button.setText(((GUIParameterListParameter)getParameter()).getButtonText());
			}
			return button;
		}

		// If not using the button, just return the parameter list editor

		return editor;
	}


//	// Select if the dialog box should be modal (default is modal).
//	
//	public void setModal(boolean modal) {
//		this.modal = modal;
//		if (frame != null) {
//			frame.setModal(modal);
//		}
//		return;
//	}
}
