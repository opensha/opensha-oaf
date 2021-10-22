package org.opensha.oaf.util.gui;

import java.awt.Color;

import org.dom4j.Element;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.param.AbstractParameter;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.editor.ParameterEditor;

/**
 * This is a dummy parameter useful in GUIs/ParameterLists where buttons are needed but not
 * associated with an actual parameter. A ParameterChangeEvent will be fired whenever the button
 * is clicked in the editor.
 * 
 * @author kevin
 *
 * Code extended to allow setting the color of the button.
 * Michael Barall 09/03/2021
 *
 */
public class GUIButtonParameter extends AbstractParameter<Integer> {
	
	private String buttonText;
	private GUIButtonParameterEditor editor;

	private Color buttonForeground;
	private Color buttonBackground;
	
	public GUIButtonParameter(String name, String buttonText) {
		super(name, null, null, null);
		this.buttonText = buttonText;
		this.buttonForeground = null;
		this.buttonBackground = null;
		this.setInfo(buttonText);
		this.setValue(0);
	}

	@Override
	public ParameterEditor getEditor() {
		if (editor == null) {
			editor = new GUIButtonParameterEditor(this);
		}
		return editor;
	}
	
	public String getButtonText() {
		return buttonText;
	}
	
	public void setButtonText(String buttonText) {
		this.buttonText = buttonText;
	}

	@Override
	public Object clone() {
		GUIButtonParameter param = new GUIButtonParameter(getName(), buttonText);
		param.buttonForeground = this.buttonForeground;
		param.buttonBackground = this.buttonBackground;
		param.setValue(getValue());
		return param;
	}

	@Override
	protected boolean setIndividualParamValueFromXML(Element el) {
		// TODO Auto-generated method stub
		return false;
	}

	public Color getButtonForeground () {
		return buttonForeground;
	}

	public void setButtonForeground (Color color) {
		buttonForeground = color;
		return;
	}

	public Color getButtonBackground () {
		return buttonBackground;
	}

	public void setButtonBackground (Color color) {
		buttonBackground = color;
		return;
	}

}
