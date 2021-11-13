package org.opensha.oaf.oetas.gui;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.event.ParameterChangeEvent;
import org.opensha.commons.param.impl.ButtonParameter;
import org.opensha.commons.param.impl.DoubleParameter;
import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIParameterListParameter;


// Operational ETAS GUI - Sub-controller for forecast parameter values.
// Michael Barall 09/01/2021
//
// This is a modeless dialog for entering forecast parameter values.


public class OEGUISubForecast extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_FCAST_EDIT = 701;		// Button to open the forecast parameter edit dialog
	private static final int PARMGRP_FCAST_VALUE = 702;		// Forecast parameter value
	private static final int PARMGRP_NOW_BUTTON = 703;		// Button to set start time to now




	//----- Controls for the forecast parameter dialog -----


	// Set To Now button.
	// Sets forecast start time so the forecast starts now.
	// Sets forecast end time to preserve end minus start.

	private ButtonParameter forecastStartTimeNowParam;

	private ButtonParameter init_forecastStartTimeNowParam () throws GUIEDTException {
		forecastStartTimeNowParam = new ButtonParameter("Set Forecast Start Time", "Set to Now");
		register_param (forecastStartTimeNowParam, "forecastStartTimeNowParam", PARMGRP_NOW_BUTTON);
		return forecastStartTimeNowParam;
	}


	// Forecast start time, in days since the mainshock; edit box containing a number.
	// Default is 0.0.

	private DoubleParameter forecastStartTimeParam;

	private DoubleParameter init_forecastStartTimeParam () throws GUIEDTException {
		forecastStartTimeParam = new DoubleParameter("Forecast Start Time", 0d, 36500d, new Double(0d));
		forecastStartTimeParam.setUnits("Days");
		forecastStartTimeParam.setInfo("Forecast start relative to main shock origin time");
		register_param (forecastStartTimeParam, "forecastStartTimeParam", PARMGRP_FCAST_VALUE);
		return forecastStartTimeParam;
	}


	// Forecast end time, in days since the mainshock; edit box containing a number.
	// Default is 7.0.

	private DoubleParameter forecastEndTimeParam;

	private DoubleParameter init_forecastEndTimeParam () throws GUIEDTException {
		forecastEndTimeParam = new DoubleParameter("Forecast End Time", 0d, 36500d, new Double(7d));
		forecastEndTimeParam.setUnits("Days");
		forecastEndTimeParam.setInfo("Forecast end relative to main shock origin time");
		register_param (forecastEndTimeParam, "forecastEndTimeParam", PARMGRP_FCAST_VALUE);
		return forecastEndTimeParam;
	}




	// Forecast parameter value dialog: initialize parameters within the dialog.

	private void init_fcValueDialogParam () throws GUIEDTException {

		init_forecastStartTimeNowParam();
		
		init_forecastStartTimeParam();
		
		init_forecastEndTimeParam();

		return;
	}




	// Forecast parameter edit: button to activate the dialog.

	private ParameterList fcValueList;			// List of parameters in the dialog
	private GUIParameterListParameter fcValueEditParam;
	

	private void updateFCValueParamList () throws GUIEDTException {
		fcValueList.clear();

		boolean f_button_row = false;

		fcValueEditParam.setListTitleText ("Forecast Parameters");
		fcValueEditParam.setDialogDimensions (gui_top.get_dialog_dims(3, f_button_row));

		fcValueList.addParameter(forecastStartTimeNowParam);
		fcValueList.addParameter(forecastStartTimeParam);
		fcValueList.addParameter(forecastEndTimeParam);
		
		fcValueEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_fcValueEditParam () throws GUIEDTException {
		fcValueList = new ParameterList();
		fcValueEditParam = new GUIParameterListParameter("Forecast Parameters", fcValueList, "Forecast Params...",
							"Edit Forecast Params", "Forecast Parameters", null, null, false, gui_top.get_trace_events());
		fcValueEditParam.setInfo("Set forecast parameter values");
		register_param (fcValueEditParam, "fcValueEditParam", PARMGRP_FCAST_EDIT);

		updateFCValueParamList();

		return fcValueEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable the RJ parameter value controls.

	private boolean f_fc_value_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(forecastStartTimeNowParam, f_fc_value_enable);
		enableParam(forecastStartTimeParam, f_fc_value_enable);
		enableParam(forecastEndTimeParam, f_fc_value_enable);

		enableParam(fcValueEditParam, f_sub_enable);

		// Parameters that are cleared when they are disabled

		//if (!( f_fc_value_enable )) {
		//}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferFCValueView {

		// Forecast start time, in days since the mainshock.

		public double x_forecastStartTimeParam;	// parameter value, checked for validity

		// Forecast start time, in days since the mainshock.

		public double x_forecastEndTimeParam;	// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferFCValueImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferFCValueImpl extends XferFCValueView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferFCValueImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferFCValueImpl () {
			internal_clean();
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			return;
		}


		// Load values.

		@Override
		public XferFCValueImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Forecast start and end time

			x_forecastStartTimeParam = validParam(forecastStartTimeParam);

			x_forecastEndTimeParam = validParam(forecastEndTimeParam);

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {
			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubForecast (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = false;
		f_fc_value_enable = false;

		// Create and initialize controls

		init_fcValueDialogParam();
		init_fcValueEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the forecast parameter value edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_fcValueEditParam () throws GUIEDTException {
		return fcValueEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_fc_value_enable (boolean f_sub_enable, boolean f_fc_value_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_fc_value_enable = f_fc_value_enable;
		adjust_enable();
		return;
	}


	// Make a forecast value transfer object.

	public XferFCValueImpl make_fc_value_xfer () {
		return new XferFCValueImpl();
	}


	// Private function, used to report that the forecast parameter values have changed.

	private void report_fc_value_change () throws GUIEDTException {
		gui_controller.notify_forecast_param_change();
		return;
	}


	// Update RJ values from model.

	public void update_fc_value_from_model () throws GUIEDTException {
		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Forecast parameter edit button.
		// - Do nothing.

		case PARMGRP_FCAST_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Forecast parameter value.
		// - Report to top-level controller.

		case PARMGRP_FCAST_VALUE: {
			if (!( f_sub_enable && f_fc_value_enable )) {
				return;
			}
			report_fc_value_change();
		}
		break;




		// Button to set forecast time to now.
		// - Dump any forecast that has been computed.
		// - Update forecast start time to now.
		// - Update forecast end time to preserve the delta.

		case PARMGRP_NOW_BUTTON: {
			if (!( f_sub_enable && f_fc_value_enable )) {
				return;
			}
			report_fc_value_change();

			SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
			Instant now = Instant.now();
			System.out.println("Computing delta from mainshock time ("
					+df.format(new Date (gui_model.get_cur_mainshock().getOriginTime()))+") to now ("+df.format(Date.from(now))+")");
			double delta = USGS_AftershockForecast.getDateDelta(Instant.ofEpochMilli(gui_model.get_cur_mainshock().getOriginTime()), now);
			System.out.println("Delta: "+delta+" days");
			double prevDiff = validParam(forecastEndTimeParam) - validParam(forecastStartTimeParam);
			if (prevDiff <= 0.0) {
				prevDiff = 7.0;
			}
			updateParam(forecastStartTimeParam, delta);
			updateParam(forecastEndTimeParam, delta+prevDiff);
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubForecast: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
