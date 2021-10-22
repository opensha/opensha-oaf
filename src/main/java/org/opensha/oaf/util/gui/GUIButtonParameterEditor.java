package org.opensha.oaf.util.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.editor.AbstractParameterEditor;

// ButtonParameterEditor extended to support setting the color of the button.
// Michael Barall 09/03/2021

public class GUIButtonParameterEditor extends AbstractParameterEditor<Integer> implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JButton button;

	public GUIButtonParameterEditor(GUIButtonParameter buttonParameter) {
		super(buttonParameter);
	}

	@Override
	public boolean isParameterSupported(Parameter<Integer> param) {
		return param instanceof GUIButtonParameter;
	}

	@Override
	public void setEnabled(boolean enabled) {
		button.setEnabled(enabled);
	}

	@Override
	protected JComponent buildWidget() {
		if (button == null) {
			button = new JButton();
			button.addActionListener(this);
			GUIButtonParameter buttonParam = (GUIButtonParameter)getParameter();
			Color fg = buttonParam.getButtonForeground();
			Color bg = buttonParam.getButtonBackground();
			if (fg != null) {
				button.setForeground(fg);
			}
			if (bg != null) {
				button.setBackground(bg);
				button.setOpaque(true);
				button.setBorderPainted(false);
			}
		}
		return updateWidget();
	}

	@Override
	protected JComponent updateWidget() {
		GUIButtonParameter buttonParam = (GUIButtonParameter)getParameter();
		button.setText(buttonParam.getButtonText());
		return button;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button) {
			Parameter<Integer> param = getParameter();
			param.setValue(param.getValue()+1);
		}
	}

}
