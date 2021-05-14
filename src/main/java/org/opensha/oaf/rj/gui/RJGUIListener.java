package org.opensha.oaf.rj.gui;

import java.util.IdentityHashMap;

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

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.GUIConsoleWindow;
import org.opensha.oaf.util.GUICalcStep;
import org.opensha.oaf.util.GUICalcRunnable;
import org.opensha.oaf.util.GUICalcProgressBar;
import org.opensha.oaf.util.GUIEDTException;
import org.opensha.oaf.util.GUIEDTRunnable;
import org.opensha.oaf.util.GUIEventAlias;
import org.opensha.oaf.util.GUIExternalCatalog;


// Reasenberg & Jones GUI - Common listener class.
// Michael Barall 04/22/2021
//
// GUI for working with the Reasenberg & Jones model.
//
// The GUI follows the model-view-controller design pattern.
// This class is a common base class for GUI components
// that create widgets and listen to change notifications.


public abstract class RJGUIListener extends RJGUIComponent implements ParameterChangeListener {




	//----- Debugging support -----


	// Symbol table.

	protected IdentityHashMap<Object, String> symbol_table;

	// Add symbol.

	protected synchronized void add_symbol (Object ptr, String symbol) {
		if (ptr != null) {
			symbol_table.put (ptr, symbol);
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
			symbol = symbol + " [" + ptr.getClass().getName() + "]";
		}
		return symbol;
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
			ChangeBlock change_block = new ChangeBlock();
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




	//----- Construction -----


	// Constructor.

	public RJGUIListener () {

		// Allocate the symbol table

		symbol_table = new IdentityHashMap<Object, String>();
	}




	//----- Parameter change actions ------




	// Mechanism for blocking calls to parameterChange.

	private int change_block_count = 0;

	protected class ChangeBlock implements AutoCloseable {

		ChangeBlock () {
			if (!( SwingUtilities.isEventDispatchThread() )) {
				throw new IllegalStateException("RJGUIListener.ChangeBlock called while not on the event dispatch thread!");
			}
			synchronized (RJGUIListener.this) {
				++change_block_count;
			}
		}

		@Override
		public void close() {
			synchronized (RJGUIListener.this) {
				--change_block_count;
			}
			return;
		}
	}




	// Mechanism for depth of calls to parameterChange.

	private int change_depth_count = 0;

	protected class ChangeDepth implements AutoCloseable {

		public int my_change_depth_count;

		ChangeDepth () {
			if (!( SwingUtilities.isEventDispatchThread() )) {
				throw new IllegalStateException("RJGUIListener.ChangeDepth called while not on the event dispatch thread!");
			}
			synchronized (RJGUIListener.this) {
				++change_depth_count;
				my_change_depth_count = change_depth_count;
			}
		}

		@Override
		public void close() {
			synchronized (RJGUIListener.this) {
				--change_depth_count;
			}
			return;
		}
	}




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

		synchronized (this) {
			if (change_block_count != 0) {
				if (gui_top.get_trace_events()) {
					System.out.println ("@@@@@ Blocked notification: " + get_symbol_and_type (event.getParameter()));
				}
				return;
			}
		}

		// Invoke the change function, knowing that we are on the EDT

		if (gui_top.get_trace_events()) {
			System.out.println ("@@@@@ Begin notification: " + get_symbol_and_type (event.getParameter()));
		}

		try (
			ChangeDepth change_depth = new ChangeDepth ();
		){
			if (change_depth.my_change_depth_count > 1) {
				if (gui_top.get_trace_events()) {
					System.out.println ("@@@@@ !!!!! Notification depth = " + change_depth.my_change_depth_count + " : " + get_symbol_and_type (event.getParameter()));
				}
			}

			parameterChange_EDT (event);
		} catch (GUIEDTException e) {
			throw new IllegalStateException ("RJGUIListener.parameterChange - Caught GUIEDTException, which should never be thrown", e);
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
