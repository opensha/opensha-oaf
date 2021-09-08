package org.opensha.oaf.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JSeparator;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.editor.ParameterEditor;
import org.opensha.commons.util.ClassUtils;


// Dummy parameter to draw a separator line in a parameter list.
// Michael Barall 09/05/2021

public class GUISeparatorParameterEditor extends JPanel implements ParameterEditor<Integer>, ActionListener {

	private static final long serialVersionUID = 1L;
	
	private GUISeparatorParameter param;
	private JSeparator separator;


	public GUISeparatorParameterEditor(GUISeparatorParameter param) {
		this.param = param;

		// Put the separator in the panel

		separator = new JSeparator (SwingConstants.HORIZONTAL);

		Color c = param.getSeparatorColor();
		if (c != null) {
			separator.setForeground (c);
			separator.setBackground (c);
		}

		setLayout (new BorderLayout());
		setBorder (BorderFactory.createEmptyBorder (0, 24, 0, 24));
		add (separator, BorderLayout.CENTER);
	}


	public boolean isParameterSupported(Parameter<Integer> param) {
		return param instanceof GUISeparatorParameter;
	}




	/** Set the value of the Parameter this editor is editing. */
	@Override
	public final void setValue( Integer value ) {
		this.param.setValue(value);
		refreshParamEditor();
		return;
	}

	/** Returns the value of the parameter object.  */
	@Override
	public final Integer getValue() {
		return param.getValue();
	}

	/**
	 * Needs to be called by subclasses when editable widget field change fails
	 * due to constraint problems. Allows rollback to the previous good value.
	 */
	@Override
	public void unableToSetValue( Object value ) {
		param.unableToSetValue(value);
		return;
	}

	/**
	 * Called when the parameter has changed independently from
	 * the editor. This function needs to be called to to update
	 * the GUI component ( text field, picklsit, etc. ) with
	 * the new parameter value.
	 */
	@Override
	public void refreshParamEditor() {
		return;
	}

	/** Returns the parameter that is stored internally that this GUI widget is editing */
	@Override
	public final Parameter<Integer> getParameter() {
		return param;
	}

	/** Sets the parameter that is stored internally for this GUI widget to edit */
	@Override
	public final void setParameter( Parameter<Integer> model ) {
		if (!isParameterSupported(model)) {
			if (model == null)
				throw new IllegalArgumentException("Null parameters not supported by this editor");
			else
				throw new IllegalArgumentException(
						"Parameter '" + model.getName() + "' of type '"
						+ ClassUtils.getClassNameWithoutPackage(model.getClass())
						+ "' not supported by this editor");
		}
		this.param = (GUISeparatorParameter)model;
		//updateTitle();
		refreshParamEditor();
	}

	/** Sets the focusEnabled boolean indicating this is the GUI componet with the current focus */
	@Override
	public final void setFocusEnabled( boolean newFocusEnabled ) {
		return;
	}

	/** Returns the focusEnabled boolean indicating this is the GUI componet with the current focus */
	@Override
	public final boolean isFocusEnabled() {
		return false;
	}
	
	@Override
	public final void setEnabled(boolean isEnabled) {
		return;
	}
	
	@Override
	public final void setVisible(boolean isVisible) {
		super.setVisible(isVisible);
		return;
	}
	
	@Override
	public final boolean isVisible() {
		return separator.isVisible();
	}
	
	@Override
	public final JComponent getComponent() {
		return this;
	}
	
	@Override
	public final void setEditorBorder(Border b) {
		setBorder(b);
		return;
	}




	@Override
	public void actionPerformed(ActionEvent e) {
		//if (e.getSource() == separator) {
		//	param.setValue(param.getValue()+1);
		//}
		return;
	}

}
