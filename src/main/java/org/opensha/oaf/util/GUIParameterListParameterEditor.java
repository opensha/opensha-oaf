package org.opensha.oaf.util;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ListIterator;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.editor.AbstractParameterEditor;
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


public class GUIParameterListParameterEditor extends AbstractParameterEditor<ParameterList> implements
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

	// The button that closes the dialog.
	protected JButton ok_button = null;

	// Set if a parameter has been changed.
	protected boolean parameterChangeFlag = false;

	// Set to enable firing a parameter change event.
	protected boolean armFireChangeFlag = false;

	// Set if the dialog has already been disposed.
	protected boolean frameDisposed = false;
	
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

	protected void updateListeners (ParameterList paramList) {

		// If we are currently watching any parameters, remove their listeners

		removeListeners();

		// Add listeners for the parameters currently in the list

		watchedParams = new ArrayList<Parameter<?>>();
		ListIterator<Parameter<?>> it = paramList.getParametersIterator();
		while(it.hasNext()) {
			Parameter<?> param = it.next();
			param.addParameterChangeListener(this);
			watchedParams.add(param);
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
	
	public void setDialogDimensions(Dimension dialogDims) {
		this.dialogDims = dialogDims;
		this.dialogPosition = null;
		return;
	}


	/**
	 * It enables/disables the editor according to whether user is allowed to
	 * fill in the values.
	 */
	@Override
	public void setEnabled(boolean isEnabled) {
		this.editor.setEnabled(isEnabled);
		if (useButton) {
			this.button.setEnabled(isEnabled);
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

		// Initialize termination code

		((GUIParameterListParameter)getParameter()).setDialogTermCode(GUIParameterListParameter.TERMCODE_OPEN);

		// Set up the dialog box

		frame = new JDialog();
		frame.setModal(modal);
		frame.setSize(dialogDims);
		if (dialogPosition != null) {
			frame.setLocation(dialogPosition);
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

		// Add button to update the parameters and close the dialog

		ok_button = new JButton();
		ok_button.setText(((GUIParameterListParameter)getParameter()).getOkButtonText());
		ok_button.setForeground(new Color(80,80,133));
		ok_button.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				button_actionPerformed(e);
				return;
			}
		});
		frame.getContentPane().add(ok_button,new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

		// Make the dialog visible

		frame.setVisible(true);		// For modal dialog, does not return until dialog is closed
//		frame.getlo
//		frame.pack();

		// Created the dialog

		return true;
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

			// Save the dialog size and position

			dialogDims = frame.getSize();
			dialogPosition = frame.getLocation();

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
	 * This function is called when user clicks the button to update the parameters.
	 * @param e
	 */
	protected void button_actionPerformed(ActionEvent e) {
		if (frame != null) {

			if (D) {
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): OK button pressed");
			}

			doCloseDialog (GUIParameterListParameter.TERMCODE_OK);
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

			doCloseDialog (GUIParameterListParameter.TERMCODE_CLOSED);
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
				System.out.println ("$$$$$ GUIParameterListParameterEditor (" + getParameter().getName() + "): Window closed");
			}

			// If any parameters have changed, send a notification
			// Note: This must be done in the window-closed handler, and not earlier.
			// Otherwise, it is possible to miss a parameter change notification, for example,
			// if the user edits a parameter and then closes the dialog by clicking the X
			// while the parameter still has focus.

			//ParameterList paramList = editor.getParameterList();
			if (parameterChangeFlag && armFireChangeFlag) {
				Parameter<ParameterList> param = getParameter();
				//param.setValue(paramList);
				// this is needed because although the list hasn't changed, values inside of it have.
				param.firePropertyChange(new ParameterChangeEvent(param, param.getName(), param.getValue(), param.getValue()));
				parameterChangeFlag = false;
				armFireChangeFlag = false;
			}

			ok_button = null;
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
			frame.repaint();
		}
		
		// Set up the button that brings up the list

		if (useButton) {
			//button = new JButton(getParameter().getName());
			button = new JButton(((GUIParameterListParameter)getParameter()).getButtonText());
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
			frame.repaint();
		}
		
		// Set up the button that brings up the list
		
		if (useButton) {
			if (button == null) {
				//button = new JButton(getParameter().getName());
				button = new JButton(((GUIParameterListParameter)getParameter()).getButtonText());
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
