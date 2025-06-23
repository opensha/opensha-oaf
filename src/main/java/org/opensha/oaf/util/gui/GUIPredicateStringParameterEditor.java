package org.opensha.oaf.util.gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.WarningException;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.editor.AbstractParameterEditor;


// Editor for a String-valued parameter that can be constrained using a Predicate<String>.
// Author: Michael Barall.
// Based on: StringParameterEditor from OpenSHA.

public class GUIPredicateStringParameterEditor
extends AbstractParameterEditor<String> implements FocusListener, KeyListener
{
	
	private boolean focusLostProcessing = false;
	private boolean keyTypeProcessing = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Class name for debugging. */
	protected final static String C = "GUIPredicateStringParameterEditor";
	/** If true print out debug statements. */
	protected final static boolean D = false;
	
	private JTextField widget;

	/** No-Arg constructor calls parent constructtor */
	protected GUIPredicateStringParameterEditor() { super(); }

	/**
	 * Constructor that sets the parameter that it edits. An
	 * Exception is thrown if the model is not an GUIPredicateStringParameter.
	 *
	 * Note: When calling the super() constuctor addWidget() is called
	 * which configures the IntegerTextField as the editor widget. <p>
	 */
	public GUIPredicateStringParameterEditor(Parameter model) throws Exception{

		super(model);
	}

	/**
	 * Called everytime a key is typed in the text field to validate it
	 * as a valid integer character ( digits and - sign in first position ).
	 */
	@Override
	public void keyTyped(KeyEvent e) {


		String S = C + ": keyTyped(): ";
		if(D) System.out.println(S + "Starting");

		keyTypeProcessing = false;
		if( focusLostProcessing == true ) return;


		if (e.getKeyChar() == '\n') {
			keyTypeProcessing = true;
			if(D) System.out.println(S + "Return key typed");
			String value = widget.getText();

			if(D) System.out.println(S + "New Value = " + value);
			try {
				String d = "";
				if( !value.equals( "" ) ) d = value;
				setValue(d);
				refreshParamEditor();
				widget.validate();
				widget.repaint();
			}
			catch (ConstraintException ee) {
				if(D) System.out.println(S + "Error = " + ee.toString());

				Object obj = getValue();
				if (obj == null)
					widget.setText("");
				else
					widget.setText(obj.toString());

				this.unableToSetValue(value);
				keyTypeProcessing = false;
			}
			catch (WarningException ee){
				keyTypeProcessing = false;
				refreshParamEditor();
				widget.validate();
				widget.repaint();
			}
		}

		keyTypeProcessing = false;
		if(D) System.out.println(S + "Ending");


	}

	/**
	 * Called when the user clicks on another area of the GUI outside
	 * this editor panel. This synchornizes the editor text field
	 * value to the internal parameter reference.
	 */
	@Override
	public void focusLost(FocusEvent e) {

		String S = C + ": focusLost(): ";
		if(D) System.out.println(S + "Starting");

		focusLostProcessing = false;
		if( keyTypeProcessing == true ) return;
		focusLostProcessing = true;

		String value = widget.getText();
		try {

			String d = "";
			if( !value.equals( "" ) ) d = value;
			setValue(d);
			refreshParamEditor();
			widget.validate();
			widget.repaint();
		}
		catch (ConstraintException ee) {
			if(D) System.out.println(S + "Error = " + ee.toString());

			Object obj = getValue();
			if (obj == null)
				widget.setText("");
			else
				widget.setText(obj.toString());

			this.unableToSetValue(value);
			focusLostProcessing = false;
		}
		catch (WarningException ee){
			focusLostProcessing = false;
			refreshParamEditor();
			widget.validate();
			widget.repaint();
		}


		focusLostProcessing = false;
		if(D) System.out.println(S + "Ending");

	}

	@Override
	public boolean isParameterSupported(Parameter<String> param) {
		//if ( getParameter() != null &&  !( getParameter() instanceof GUIPredicateStringParameter))	// test from StringParameterEditor, appears to be a bug
		if (param != null && !(param instanceof GUIPredicateStringParameter))
			return false;
		return true;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (widget != null)
			widget.setEnabled(enabled);
	}
	
	@Override
	public boolean isEnabled() {
		return widget != null && widget.isEnabled();
	}

	@Override
	protected JComponent buildWidget() {
		widget = new JTextField();
		widget.setPreferredSize(LABEL_DIM);
		widget.setMinimumSize(LABEL_DIM);
		widget.setBorder(ETCHED);

		widget.addFocusListener( this );
		widget.addKeyListener(this);

		updateWidget();
		
		return widget;
	}

	@Override
	protected JComponent updateWidget() {
		String val = getValue();
		if (val == null || val.length() == 0)
			widget.setText("");
		else
			widget.setText(val);
		return widget;
	}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void focusGained(FocusEvent e) {}
}
