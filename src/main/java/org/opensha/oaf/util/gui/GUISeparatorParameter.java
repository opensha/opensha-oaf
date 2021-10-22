package org.opensha.oaf.util.gui;

import java.awt.Color;

import org.dom4j.Element;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.param.AbstractParameter;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.editor.ParameterEditor;

// Dummy parameter to draw a separator line in a parameter list.
// Michael Barall 09/05/2021

public class GUISeparatorParameter extends AbstractParameter<Integer> {
	
	private GUISeparatorParameterEditor editor = null;

	private Color separatorColor;

	// Create separator with given name and color.
	// Color can be null to use the system default.
	// Each parameter in a list must have a unique name.
	
	public GUISeparatorParameter(String name, Color sepColor) {
		super(name, null, null, null);
		separatorColor = sepColor;
		//this.setInfo("Separator");
		this.setNonEditable();
		this.setValue(0);
	}

	@Override
	public ParameterEditor getEditor() {
		if (editor == null) {
			editor = new GUISeparatorParameterEditor(this);
		}
		return editor;
	}

	@Override
	public Object clone() {
		GUISeparatorParameter param = new GUISeparatorParameter(getName(), getSeparatorColor());
		return param;
	}

	@Override
	protected boolean setIndividualParamValueFromXML(Element el) {
		// TODO Auto-generated method stub
		return false;
	}

	public Color getSeparatorColor () {
		return separatorColor;
	}

}
