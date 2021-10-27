package org.opensha.oaf.util.gui;

import java.util.List;

import org.dom4j.Element;
import org.opensha.commons.param.AbstractParameter;
import org.opensha.commons.param.constraint.ParameterConstraint;
import org.opensha.commons.param.constraint.impl.EnumConstraint;
import org.opensha.commons.param.editor.ParameterEditor;
import org.opensha.commons.param.constraint.impl.IntegerConstraint;
import org.opensha.commons.exceptions.ConstraintException;


// GUI parameter that displays a drop-down list.
// Author: Michael Barall 10/26/2021.
//
// This code is based on the OpenSHA class EnumParameter.
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

public class GUIDropdownParameter extends AbstractParameter<Integer> {

	// The parameter editor.

	private GUIDropdownParameterEditor editor = null;

	// True if there is an "extra" string.

	private boolean f_extra = false;

	// The list of choices that are displayed in the dropdown.
	// If extra_string is non-null, then it appears as choices[0].

	private String[] choice_list = null;




	// The special value that indicates no selection.

	public static final int DROPDOWN_INDEX_NONE = -1;

	// The special value that indicates the "extra" item is selected.

	public static final int DROPDOWN_INDEX_EXTRA = -2;




	// Private constructor for internal cloning use only.

	private GUIDropdownParameter() {
	}




	// Constructor.
	// Parameters:
	//  name = Name of this parameter.
	//  choices = List of objects (of any type) that the user can choose.
	//  defaultValue = The initial value to use.  It must be between -1
	//    and choices.size() - 1 inclusive, or -2 if extra != null.
	//  extra = The "extra" string, to display at the start of the list, or null if none.
	// Note: The strings that appear in the dropdown are obtained by iterating
	// over choices and applying toString() to each element.
	
	public GUIDropdownParameter (String name, List<?> choices, int defaultValue, String extra) {
		super(name, new IntegerConstraint ((extra != null) ? DROPDOWN_INDEX_EXTRA : DROPDOWN_INDEX_NONE, choices.size()), null, defaultValue);

		// Save flag indicating extra string is present

		f_extra = (extra != null);

		// Make the list of choices

		choice_list = new String[choices.size() + (f_extra ? 1 : 0)];

		int n = 0;
		if (f_extra) {
			choice_list[n] = extra;
			++n;
		}

		for (Object o : choices) {
			choice_list[n] = o.toString();
			++n;
		}

		// Establish the current value as the default value

		setDefaultValue (defaultValue);
	}




	// Modify the dropdown list.
	// Parameters:
	//  choices = List of objects (of any type) that the user can choose.
	//  defaultValue = The new value to use.  It must be between -1
	//    and choices.size() - 1 inclusive, or -2 if extra != null.
	//  extra = The "extra" string, to display at the start of the list, or null if none.
	// Note: The strings that appear in the dropdown are obtained by iterating
	// over choices and applying toString() to each element.
	// Note: This function does not fire parameter change notifications, because
	// indexes for two different lists are not comparable.
	// Note: After calling this, call getEditor().refreshParamEditor() to update the screen.
	
	public void modify_dropdown (List<?> choices, int defaultValue, String extra) {

		// The new constraint

		IntegerConstraint new_constraint = new IntegerConstraint ((extra != null) ? DROPDOWN_INDEX_EXTRA : DROPDOWN_INDEX_NONE, choices.size());

		// Check that the new value satisfies the new constraint

		if (!( new_constraint.isAllowed (defaultValue) )) {
			System.out.println ("GUIDropdownParameter.modify_dropdown: Value not allowed: value = " + defaultValue);
			throw new ConstraintException ("GUIDropdownParameter.modify_dropdown: Value not allowed: value = " + defaultValue);
		}

		// Store constraint and value directly into AbstractParameter
		// Prevents parameter change from firing and prevents constraint violation

		this.constraint = new_constraint;
		this.value = defaultValue;

		// Save flag indicating extra string is present

		f_extra = (extra != null);

		// Make the list of choices

		choice_list = new String[choices.size() + (f_extra ? 1 : 0)];

		int n = 0;
		if (f_extra) {
			choice_list[n] = extra;
			++n;
		}

		for (Object o : choices) {
			choice_list[n] = o.toString();
			++n;
		}

		// Establish the current value as the default value

		setDefaultValue (defaultValue);
		return;
	}




	// Get (or make) the editor.

	@Override
	public ParameterEditor<Integer> getEditor() {
		if (editor == null) {
			editor = new GUIDropdownParameterEditor(this);
		}
		return editor;
	}



	// Make a clone of this object.

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() {
		GUIDropdownParameter ep = new GUIDropdownParameter();
		ep.editor = null;
		ep.f_extra = f_extra;
		ep.choice_list = choice_list.clone();
		ep.constraint = (IntegerConstraint)(constraint.clone());
		ep.defaultValue = defaultValue;
		ep.value = value;
		ep.name = name;
		return ep;
	}




	// Deserialization method, not used.

	@Override
	public boolean setIndividualParamValueFromXML(Element el) {
		try {
			int val = Integer.parseInt(el.attributeValue("value"));
			this.setValue(val);
			return true;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
	}




	// Returns the extra string, or null if none specified.

	public String getExtra () {
		if (f_extra) {
			return choice_list[0];
		}
		return null;
	}




	// Get the constraint for this parameter.

	@Override
	public IntegerConstraint getConstraint() {
		return (IntegerConstraint)(super.getConstraint());
	}




	//--- Functions for use by the editor ---




	// Set the raw value for this parameter.
	// The raw value is an index into choice_list, or -1 if nothing selected.

	public void set_raw_value (int x) {
		int y = x;
		if (f_extra) {
			if (x == 0) {
				y = DROPDOWN_INDEX_EXTRA;
			} else if (x > 0) {
				y = x - 1;
			}
		}
		setValue (y);
		return;
	}




	// Get the raw value for this parameter.
	// The raw value is an index into choice_list, or -1 if nothing selected.

	public int get_raw_value () {
		int x = getValue();
		if (f_extra) {
			if (x == DROPDOWN_INDEX_EXTRA) {
				x = 0;
			} else if (x >= 0) {
				++x;
			}
		}
		return x;
	}




	// Get the list of choices.

	public String[] get_choice_list () {
		return choice_list;
	}
}
