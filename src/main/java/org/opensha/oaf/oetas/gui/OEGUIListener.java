package org.opensha.oaf.oetas.gui;

import java.util.IdentityHashMap;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;

import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.editor.impl.GriddedParameterListEditor;
import org.opensha.commons.param.editor.impl.ParameterListEditor;
import org.opensha.commons.param.event.ParameterChangeEvent;
import org.opensha.commons.param.event.ParameterChangeListener;
import org.opensha.commons.param.impl.BooleanParameter;
import org.opensha.commons.param.impl.ButtonParameter;
import org.opensha.commons.param.impl.DoubleParameter;
import org.opensha.commons.param.impl.EnumParameter;
import org.opensha.commons.param.impl.IntegerParameter;
import org.opensha.commons.param.impl.LocationParameter;
import org.opensha.commons.param.impl.ParameterListParameter;
import org.opensha.commons.param.impl.RangeParameter;
import org.opensha.commons.param.impl.StringParameter;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Doubles;

import org.jfree.data.Range;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.gui.GUIExternalCatalogV2;
import org.opensha.oaf.util.gui.GUIPredicateStringParameter;
import org.opensha.oaf.util.SharedCounter;
import org.opensha.oaf.util.SharedCounterAutoInc;


// Reasenberg & Jones GUI - Common listener class.
// Michael Barall 04/22/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is a common base class for GUI components
// that create widgets and listen to change notifications.


public abstract class OEGUIListener extends OEGUIComponent implements ParameterChangeListener {




	//----- Debugging support, and parameter groups -----


	// Symbol table.

	protected IdentityHashMap<Object, String> symbol_table;

	// Parameter group table.

	protected IdentityHashMap<Object, Integer> parmgrp_table;

	// Value to use for no parameter group.

	protected static final int PARMGRP_NONE = 0;

	// Add symbol.

	protected synchronized void add_symbol (Object ptr, String symbol) {
		add_symbol (ptr, symbol, PARMGRP_NONE);
		return;
	}

	protected synchronized void add_symbol (Object ptr, String symbol, int parmgrp) {
		if (ptr != null) {
			if (symbol != null) {
				symbol_table.put (ptr, symbol);
			}
			if (parmgrp != PARMGRP_NONE) {
				parmgrp_table.put (ptr, parmgrp);
			}
		}
		return;
	}

	// Get symbol for an object.

	protected synchronized String get_symbol (Object ptr) {
		String symbol;
		if (ptr == null) {
			symbol = "<null>";
		} else {
			symbol = symbol_table.get (ptr);
			if (symbol == null || symbol.isEmpty()) {
				symbol = "<unknown>";
			}
		}
		return symbol;
	}

	// Get symbol and type for an object.

	protected synchronized String get_symbol_and_type (Object ptr) {
		String symbol;
		if (ptr == null) {
			symbol = "<null>";
		} else {
			symbol = symbol_table.get (ptr);
			if (symbol == null || symbol.isEmpty()) {
				symbol = "<unknown>";
			}
			int parmgrp = PARMGRP_NONE;
			Integer x = parmgrp_table.get (ptr);
			if (x != null) {
				parmgrp = x;
			}
			symbol = symbol + " (" + parmgrp + ") [" + ptr.getClass().getSimpleName() + "]";
		}
		return symbol;
	}

	// Get parameter group for an object.

	protected synchronized int get_parmgrp (Object ptr) {
		int parmgrp;
		if (ptr == null) {
			parmgrp = PARMGRP_NONE;
		} else {
			Integer x = parmgrp_table.get (ptr);
			if (x == null) {
				parmgrp = PARMGRP_NONE;
			} else {
				parmgrp = x;
			}
		}
		return parmgrp;
	}

	// Register a parameter.
	// This function adds this object as a parameter change listener,
	// and sets the symbol name and parameter group.

	protected void register_param (Parameter<?> param, String symbol, int parmgrp) {
		param.addParameterChangeListener(this);
		add_symbol (param, symbol, parmgrp);
		return;
	}




	//----- Parameter access functions -----


	// Get the value of a parameter, with no checking.
	// Note that the result can be null.

	//  protected <E> E getParam (Parameter<E> param) {
	//  	return param.getValue();
	//  }


	// Get the value of a parameter, with validation.
	// Throws an exception if the result is null.
	// For DoubleParameter, also throws an exception if the result is not finite.
	// For StringParameter, also throws an exception if the result is empty.

	protected <E> E validParam (Parameter<E> param) {
		E value = param.getValue();
		Preconditions.checkState (value != null, "Missing value: " + param.getName());
		return value;
	}

	protected Double validParam (DoubleParameter param) {
		Double value = param.getValue();
		Preconditions.checkState (value != null, "Missing value: " + param.getName());
		Preconditions.checkState (Doubles.isFinite(value), "Non-finite value: " + param.getName());
		return value;
	}

	protected String validParam (StringParameter param) {
		String value = param.getValue();
		Preconditions.checkState (value != null && !value.isEmpty(), "Missing value: " + param.getName());
		return value;
	}

	protected String validParam (GUIPredicateStringParameter param) {
		String value = param.getValue();
		Preconditions.checkState (value != null && !value.isEmpty(), "Missing value: " + param.getName());
		return value;
	}


	// Return true if a parameter is defined and passes validation.
	// Returns false if the result is null.
	// For DoubleParameter, also returns false if the result is not finite.
	// For StringParameter, also returns false if the result is empty.
	// Note: definedParam returns false if and only if validParam would throw an exception.

	protected <E> boolean definedParam (Parameter<E> param) {
		E value = param.getValue();
		return value != null;
	}

	protected boolean definedParam (DoubleParameter param) {
		Double value = param.getValue();
		return value != null && Doubles.isFinite(value);
	}

	protected boolean definedParam (StringParameter param) {
		String value = param.getValue();
		return value != null && !value.isEmpty();
	}

	protected boolean definedParam (GUIPredicateStringParameter param) {
		String value = param.getValue();
		return value != null && !value.isEmpty();
	}


	// Return true if a parameter is non-null.
	// Note that a non-null parameter may still fail validation.

	protected <E> boolean nonNullParam (Parameter<E> param) {
		E value = param.getValue();
		return value != null;
	}


	// Set the value of a parameter, without updating the screen.
	// Note: ConstraintException and ParameterException are unchecked exceptions,
	// but we list them for the sake of documentation.

	//  protected <E> void setParam (Parameter<E> param, E value) throws GUIEDTException, ConstraintException, ParameterException {
	//  	param.setValue (value);
	//  	return;
	//  }


	// Set the value of a parameter, and update the screen.
	// Returns true if the value has changed.
	// Does not fire a parameter change notification.  (More precisely, blocks any change
	// notification fired by the parameter.)
	// Note: The commented-out parameter change test is the exact test used in AbstractParameter to determine
	// whether to fire a change notification.  Notice that if the old and new values are both null,
	// then the parameter is (probably incorrectly) considered to be changed.
	// We use a test that also considers the parameter unchanged if the old and new values are both null.
	// Note: ConstraintException and ParameterException are unchecked exceptions,
	// but we list them for the sake of documentation.

	protected <E> boolean updateParam (Parameter<E> param, E value) throws GUIEDTException, ConstraintException, ParameterException {
		boolean result = true;
		try (
			SharedCounterAutoInc change_block = new SharedCounterAutoInc (change_block_count);
		){
			E old_value = param.getValue();
			param.setValue (value);
			param.getEditor().refreshParamEditor();

			//  if (old_value != null && old_value.equals(value)) {
			//  	result = false;
			//  }

			if ((old_value == null) ? (value == null) : (old_value.equals(value))) {
				result = false;
			}
		}

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ updateParam (" + get_symbol(param) + ", " + (value == null ? "<null>" : value.toString()) + ") -> " + result);
		}

		return result;
	}


	// Enable or disable a parameter in the GUI.

	protected <E> void enableParam (Parameter<E> param, boolean enabled) throws GUIEDTException {
		param.getEditor().setEnabled(enabled);
		return;
	}




	//----- Date/Time parameter -----


	// Make an edit field parameter for entering date and time.
	// It accepts any time format recognized by SimpleUtils.string_to_time_permissive,
	// plus an empty string and a string comprised entirely of white space.
	// The default units is "yyyy-mm-dd hh:mm".
	// If the initial value is given as a long, it is formatted as "yyyy-mm-dd hh:mm".
	// Note: ConstraintException is an unchecked exceptions,
	// but we list it for the sake of documentation.

	protected GUIPredicateStringParameter makeTimeParam (String name, String units, String value) throws ConstraintException {
		Predicate<String> predicate = (String s) -> {
			boolean result = false;
			if (s.trim().isEmpty()) {
				result = true;
			} else {
				try {
					SimpleUtils.string_to_time_permissive (s);
					result = true;
				} catch (Exception e) {
					result = false;
				}
			}
			return result;
		};
		return new GUIPredicateStringParameter(name, predicate, units, value);
	}

	GUIPredicateStringParameter makeTimeParam (String name, String value) throws ConstraintException {
		return makeTimeParam (name, "yyyy-mm-dd hh:mm", value);
	}

	GUIPredicateStringParameter makeTimeParam (String name, long time) throws ConstraintException {
		return makeTimeParam (name, "yyyy-mm-dd hh:mm", SimpleUtils.time_to_string_no_sec (time));
	}


	// Get the value of a time parameter, with validation.
	// Throws an exception if value is null, empty, white space, or not a vaiid time.

	protected long validTimeParam (GUIPredicateStringParameter param) {
		if (!( definedParam (param) )) {
			throw new RuntimeException ("Missing value: " + param.getName());
		}
		String s = validParam (param);
		if (s.trim().isEmpty()) {
			throw new RuntimeException ("Missing value: " + param.getName());
		}
		long time;
		try {
			time = SimpleUtils.string_to_time_permissive (s);
		} catch (Exception e) {
			throw new RuntimeException ("Invalid value: " + param.getName());	// should never happen
		}
		return time;
	}


	// Return true if a time parameter is defined and passes validation.
	// Note: definedTimeParam returns false if and only if validTimeParam would throw an exception.

	protected boolean definedTimeParam (GUIPredicateStringParameter param) {
		if (!( definedParam (param) )) {
			return false;
		}
		String s = validParam (param);
		if (s.trim().isEmpty()) {
			return false;
		}
		long time;
		try {
			time = SimpleUtils.string_to_time_permissive (s);
		} catch (Exception e) {
			return false;	// should never happen
		}
		return true;
	}


	// Set the value of a time parameter, and update the screen.
	// Returns true if the value has changed.
	// Does not fire a parameter change notification.  (More precisely, blocks any change
	// notification fired by the parameter.)
	// Note: ConstraintException and ParameterException are unchecked exceptions,
	// but we list them for the sake of documentation.

	protected boolean updateTimeParam (GUIPredicateStringParameter param, long time) throws GUIEDTException, ConstraintException, ParameterException {
		return updateParam (param, SimpleUtils.time_to_string_no_sec (time));
	}




	//----- Range parameter checking -----




//	// This function is called when there is a change in one of the grid range
//	// parameter pairs, either the range (lower/upper bounds) or the number.
//	// If the range is non-empty but the number is 1, then the number is set to defaultNum.
//	// If the range is empty but the number is > 1, then the number is set to 1.
//	// Otherwise, or if the range is invalid, then do nothing.
//	
//	private void updateRangeParams(RangeParameter rangeParam, IntegerParameter numParam, int defaultNum) throws GUIEDTException {
//		if (gui_top.get_trace_events()) {
//			System.out.println ("@@@@@ updateRangeParams (" + get_symbol(rangeParam) + ", " + get_symbol(numParam) + ", " + defaultNum + ")");
//		}
//
//		Preconditions.checkState(defaultNum > 1);
//		if (nonNullParam(rangeParam)) {
//			Range range = validParam(rangeParam);
//			boolean same = range.getLowerBound() == range.getUpperBound();
//			if (same && ((!definedParam(numParam)) || validParam(numParam) > 1)) {
//				updateParam(numParam, 1);
//			}
//			else if (!same && ((!definedParam(numParam)) || validParam(numParam) == 1)) {
//				updateParam(numParam, defaultNum);
//			}
//		}
//		return;
//	}




	// This function is called when there is a change in one of the grid range
	// parameter pairs, either the range (lower/upper bounds) or the number.
	// If the range is null, change it to the default range.  This is to work around a
	// problem with the range parameter, which is that if the user clears the parameter
	// then there is no obvious way for the user to enter a new value.
	// If the number is null, change it to 1 or the default number depending on
	// whether the range is empty or non-empty.
	// We do not attempt to force correspondence between range and number, because
	// doing so has proved to be awkward in use.
	
	public void updateRangeParams(RangeParameter rangeParam, IntegerParameter numParam,
			int defaultNum, double defaultLower, double defaultUpper) throws GUIEDTException {
		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ updateRangeParams (" + get_symbol(rangeParam) + ", " + get_symbol(numParam) + ", " + defaultNum + ", " + defaultLower + ", " + defaultUpper + ")");
		}

		Preconditions.checkState(defaultNum >= 1);
		Preconditions.checkState(defaultUpper >= defaultLower);

		if (!( definedParam(rangeParam) )) {
			updateParam(rangeParam, new Range(defaultLower, defaultUpper));
		}

		if (!( definedParam(numParam) )) {
			Range range = validParam(rangeParam);
			if (range.getLowerBound() == range.getUpperBound()) {
				updateParam(numParam, 1);
			} else {
				updateParam(numParam, defaultNum);
			}
		}

		return;
	}
	



//	// Check thet the grid range number is consistent with the range limits,
//	// throw an exception if not.
//	// validateRange must run on worker threads, and so must not set any parameters or write to the screen.
//
//	private void validateRange(Range range, int num, String name) {
//		Preconditions.checkState(range != null, "Must supply "+name+" range");
//		boolean same = range.getLowerBound() == range.getUpperBound();
//		if (same)
//			Preconditions.checkState(num == 1, "Num must equal 1 for fixed "+name);
//		else
//			Preconditions.checkState(num > 1, "Num must be >1 for variable "+name);
//		return;
//	}
	



	// Check thet the grid range number is consistent with the range limits,
	// throw an exception if not.

	public void validateRange(Range range, int num, String name) {
		Preconditions.checkState(range != null, "Must supply " + name + " range");
		if (range.getLowerBound() == range.getUpperBound())
			Preconditions.checkState(num == 1, "Number must equal 1 for empty " + name + " range");
		else
			Preconditions.checkState(num > 1, "Number must be >1 for non-empty " + name + " range");
		return;
	}




	//----- Construction -----


	// Constructor.

	public OEGUIListener () {

		parent_listener = null;

		// Allocate the symbol table

		symbol_table = new IdentityHashMap<Object, String>();
		parmgrp_table = new IdentityHashMap<Object, Integer>();

		// Allocate shared counters

		change_block_count = new SharedCounter();
		change_depth_count = new SharedCounter();
	}


	// Constructor, for a sub-listener.

	public OEGUIListener (OEGUIListener parent) {

		parent_listener = parent;

		// Get linkage to the top-level components from the parent

		link_components (parent);

		// Allocate the symbol table

		symbol_table = new IdentityHashMap<Object, String>();
		parmgrp_table = new IdentityHashMap<Object, Integer>();

		// Allocate shared counters, sharing with the parent

		change_block_count = parent.change_block_count;
		change_depth_count = parent.change_depth_count;
	}




	//----- Parameter change actions ------


	// The parent listener for a sub-listener, or null if none.

	protected OEGUIListener parent_listener;


	// Mechanism for blocking calls to parameterChange.

	private SharedCounter change_block_count;


	// Mechanism for depth of calls to parameterChange.

	private SharedCounter change_depth_count;




	// Change notification from the parameter.

	@Override
	public void parameterChange (ParameterChangeEvent event) {

		// Ensure we are on the event dispatch thread

		GUIEDTRunnable.check_on_edt();

		// Block notfications where old and new values are both null.
		// It is apparently a bug in AbstractParameter that such notifications occur.
		// Such notifications can occur even if the control is disabled.

		if (event.getOldValue() == null && event.getNewValue() == null) {
			if (gui_top.get_trace_events()) {
				System.out.println ("@@@@@ Null-null notification: " + get_symbol_and_type (event.getParameter()));
			}
			return;
		}

		// Just return if notifications are blocked
		// (It is OK if depth > 1 here)

		if (change_block_count.get_count() != 0) {
			if (gui_top.get_trace_events()) {
				System.out.println ("@@@@@ Blocked notification: " + get_symbol_and_type (event.getParameter()));
			}
			return;
		}

		// Invoke the change function, knowing that we are on the EDT

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Begin notification: " + get_symbol_and_type (event.getParameter()));
		}

		try (
			SharedCounterAutoInc change_depth = new SharedCounterAutoInc (change_depth_count);
		){
			if (change_depth.get_saved_count() > 1) {
				if (gui_top.get_trace_events()) {
					System.out.println ("@@@@@ !!!!! Notification depth = " + change_depth.get_saved_count() + " : " + get_symbol_and_type (event.getParameter()));
				}
			}

			parameterChange_EDT (event);
		} catch (GUIEDTException e) {
			throw new IllegalStateException ("OEGUIListener.parameterChange - Caught GUIEDTException, which should never be thrown", e);
		}

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ End notification: " + get_symbol_and_type (event.getParameter()));
		}

		return;
	}




	// Receives the change notification.

	public abstract void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException;




	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
