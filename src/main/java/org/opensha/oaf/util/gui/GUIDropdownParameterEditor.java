package org.opensha.oaf.util.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.constraint.impl.EnumConstraint;
//import org.opensha.commons.param.editor.AbstractParameterEditor;


// GUI parameter editor that displays a drop-down list.
// Author: Michael Barall 10/26/2021.
//
// This code is based on the OpenSHA class EnumParameterEditor.
//
// This parameter appears as a drop-down list.  The parameter contains a list
// of strings, which are the items shown in the drop-down list.  It also
// contains an "extra" string which, if non-null, displays as the first item
// in the drop-down list.  The user can select any one (and only one) item
// from the list, and in some cases, nothing.
//
// The value of the parameter is a non-null Integer.  If non-negative, it is
// the index into the list of the selected item.  The value -1 indicates no
// selection, and the value -2 indicates that the "extra" string is selected.
//
// To specify the list of strings, you can supply a list of any type.  The
// strings are obtained by applying toString to each element of the supplied
// list.  The supplied list is not retained.  Note that changes made to the
// list items after the list is passed in do not change the strings appearing
// in the drop-down list.

public class GUIDropdownParameterEditor extends GUIAbstractParameterEditor<Integer> implements ItemListener {

	private static final long serialVersionUID = 1L;

	/** Class name for debugging. */
	protected final static String C = "GUIDropdownParameterEditor";

	/** If true print out debug statements. */
	protected final static boolean D = false;


	// The swing object to display on the screen.

	private JComboBox<String> ed_widget = null;


	// Construct an editor for the given parameter.

	public GUIDropdownParameterEditor(GUIDropdownParameter model) {
		super (model);
		refreshParamEditor();
	}


	// Notification that the dropdown state has changed.

	@Override
	public void itemStateChanged(ItemEvent e) {
		int index = ed_widget.getSelectedIndex();
		((GUIDropdownParameter) getParameter()).set_raw_value (index);
		return;
	}


	// Test if the supplied parameter is supported by this editor.

	@Override
	public boolean isParameterSupported(Parameter<Integer> param) {
		if (param == null)
			return false;
		
		if (!(param instanceof GUIDropdownParameter))
			return false;
		
		return true;
	}


	// Enable this parameter.

	@Override
	public void setEnabled(boolean enabled) {
		ed_widget.setEnabled(enabled);
//		ed_widget.repaint();
		return;
	}


	// Build our on-screen widget.

	@Override
	protected JComponent buildWidget() {

		if (D) {
			System.out.println ("$$$$$ GUIDropdownParameterEditor (" + getParameter().getName() + "): buildWidget called");
		}

		GUIDropdownParameter param = (GUIDropdownParameter) getParameter();
		ed_widget = new JComboBox<String> (new DefaultComboBoxModel<String> (param.get_choice_list()));
		//ed_widget.setModel (new DefaultComboBoxModel<String> (param.get_choice_list()));
		ed_widget.setMaximumRowCount(40);
		ed_widget.setSelectedIndex (param.get_raw_value());
		ed_widget.addItemListener(this);
		ed_widget.setPreferredSize(WIGET_PANEL_DIM);
		ed_widget.setMinimumSize(WIGET_PANEL_DIM);
		return ed_widget;
	}


	// Update our on-screen widget.

	@Override
	protected JComponent updateWidget() {

		if (D) {
			System.out.println ("$$$$$ GUIDropdownParameterEditor (" + getParameter().getName() + "): updateWidget called");
		}

		GUIDropdownParameter param = (GUIDropdownParameter) getParameter();
		ed_widget.removeItemListener(this);
		ed_widget.setModel (new DefaultComboBoxModel<String> (param.get_choice_list()));
		ed_widget.setSelectedIndex (param.get_raw_value());
		ed_widget.addItemListener(this);
		return ed_widget;
	}

}
