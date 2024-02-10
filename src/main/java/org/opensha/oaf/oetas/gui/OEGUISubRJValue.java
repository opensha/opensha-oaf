package org.opensha.oaf.oetas.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
//import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.Range;
import org.jfree.chart.ui.RectangleEdge;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DefaultXY_DataSet;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.data.function.EvenlyDiscretizedFunc;
import org.opensha.commons.data.function.HistogramFunction;
import org.opensha.commons.data.function.XY_DataSet;
import org.opensha.commons.data.function.XY_DatasetBinner;

import org.opensha.oaf.rj.AftershockStatsCalc;
import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.rj.GenericRJ_Parameters;
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;
import org.opensha.oaf.rj.MagCompFn;
import org.opensha.oaf.rj.MagCompPage_Parameters;
import org.opensha.oaf.rj.MagCompPage_ParametersFetch;
import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.RJ_AftershockModel;
import org.opensha.oaf.rj.RJ_AftershockModel_Bayesian;
import org.opensha.oaf.rj.RJ_AftershockModel_Generic;
import org.opensha.oaf.rj.RJ_AftershockModel_SequenceSpecific;
import org.opensha.oaf.rj.SearchMagFn;
import org.opensha.oaf.rj.SearchRadiusFn;
import org.opensha.oaf.rj.SeqSpecRJ_Parameters;
import org.opensha.oaf.rj.USGS_AftershockForecast;
import org.opensha.oaf.rj.USGS_AftershockForecast.Duration;
import org.opensha.oaf.rj.USGS_AftershockForecast.Template;

import org.opensha.commons.data.xyz.EvenlyDiscrXYZ_DataSet;
import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
//import org.opensha.commons.geo.Region;
//import org.opensha.commons.gui.ConsoleWindow;
import org.opensha.commons.gui.plot.GraphWidget;
import org.opensha.commons.gui.plot.PlotCurveCharacterstics;
import org.opensha.commons.gui.plot.PlotElement;
import org.opensha.commons.gui.plot.PlotLineType;
import org.opensha.commons.gui.plot.PlotSpec;
import org.opensha.commons.gui.plot.PlotSymbol;
import org.opensha.commons.gui.plot.jfreechart.xyzPlot.XYZPlotSpec;
import org.opensha.commons.mapping.gmt.elements.GMT_CPT_Files;
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
//import org.opensha.commons.param.impl.ParameterListParameter;
import org.opensha.commons.param.impl.RangeParameter;
import org.opensha.commons.param.impl.StringParameter;
import org.opensha.commons.util.ClassUtils;
import org.opensha.commons.util.DataUtils.MinMaxAveTracker;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.commons.util.cpt.CPT;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupListCalc;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.faultSurface.FaultTrace;
import org.opensha.sha.faultSurface.RuptureSurface;
//import org.opensha.sha.gui.infoTools.CalcProgressBar;
import org.opensha.sha.magdist.GutenbergRichterMagFreqDist;
import org.opensha.sha.magdist.IncrementalMagFreqDist;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.primitives.Doubles;
import java.awt.Font;

import gov.usgs.earthquake.product.Product;
//import org.opensha.oaf.pdl.OAF_Publisher;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLCodeChooserOaf;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.gui.GUIParameterListParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Sub-controller for Reasenberg & Jones parameter values.
// Michael Barall 09/01/2021
//
// This is a modeless dialog for entering Reasenberg & Jones parameter values.


public class OEGUISubRJValue extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_RJ_EDIT = 501;		// Button to open the RJ parameter edit dialog
	private static final int PARMGRP_RJ_VALUE = 502;	// Reasenberg & Jones parameter value
	private static final int PARMGRP_RANGE_RJ_A = 503;	// Reasenberg & Jones parameter range a
	private static final int PARMGRP_RANGE_RJ_P = 504;	// Reasenberg & Jones parameter range p
	private static final int PARMGRP_RANGE_RJ_C = 505;	// Reasenberg & Jones parameter range c




	//----- Controls for the RJ parameter dialog -----


	// Range of a-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter aValRangeParam;

	private RangeParameter init_aValRangeParam () throws GUIEDTException {
		aValRangeParam = new RangeParameter("RJ a-value Range", new Range(-4.5, -0.5));
		register_param (aValRangeParam, "aValRangeParam", PARMGRP_RANGE_RJ_A);
		return aValRangeParam;
	}


	// Number of a-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter aValNumParam;

	private IntegerParameter init_aValNumParam () throws GUIEDTException {
		aValNumParam = new IntegerParameter("RJ a-value Number", 1, 10000, Integer.valueOf(101));
		aValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (aValNumParam, "aValNumParam", PARMGRP_RANGE_RJ_A);
		return aValNumParam;
	}


	// Range of p-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter pValRangeParam;

	private RangeParameter init_pValRangeParam () throws GUIEDTException {
		pValRangeParam = new RangeParameter("RJ p-value Range", new Range(0.5, 2.0));
		register_param (pValRangeParam, "pValRangeParam", PARMGRP_RANGE_RJ_P);
		return pValRangeParam;
	}


	// Number of p-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter pValNumParam;

	private IntegerParameter init_pValNumParam () throws GUIEDTException {
		pValNumParam = new IntegerParameter("RJ p-value Number", 1, 10000, Integer.valueOf(45));
		pValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (pValNumParam, "pValNumParam", PARMGRP_RANGE_RJ_P);
		return pValNumParam;
	}


	// Range of c-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter cValRangeParam;

	private RangeParameter init_cValRangeParam () throws GUIEDTException {
		cValRangeParam = new RangeParameter("RJ c-value Range", new Range(0.018, 0.018));
		register_param (cValRangeParam, "cValRangeParam", PARMGRP_RANGE_RJ_C);
		return cValRangeParam;
	}


	// Number of c-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter cValNumParam;

	private IntegerParameter init_cValNumParam () throws GUIEDTException {
		cValNumParam = new IntegerParameter("RJ c-value Number", 1, 10000, Integer.valueOf(1));
		cValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (cValNumParam, "cValNumParam", PARMGRP_RANGE_RJ_C);
		return cValNumParam;
	}




	// RJ parameter value dialog: initialize parameters within the dialog.

	private void init_rjValueDialogParam () throws GUIEDTException {
		
		init_aValRangeParam();
		
		init_aValNumParam();
		
		init_pValRangeParam();
		
		init_pValNumParam();
		
		init_cValRangeParam();
		
		init_cValNumParam();

		return;
	}




	// RJ parameter edit: button to activate the dialog.

	private ParameterList rjValueList;			// List of parameters in the dialog
	private GUIParameterListParameter rjValueEditParam;
	

	private void updateRJValueParamList () throws GUIEDTException {
		rjValueList.clear();

		boolean f_button_row = false;

		rjValueEditParam.setListTitleText ("RJ Parameters");
		rjValueEditParam.setDialogDimensions (gui_top.get_dialog_dims(6, f_button_row));
		rjValueList.addParameter(aValRangeParam);
		rjValueList.addParameter(aValNumParam);
		rjValueList.addParameter(pValRangeParam);
		rjValueList.addParameter(pValNumParam);
		rjValueList.addParameter(cValRangeParam);
		rjValueList.addParameter(cValNumParam);
		
		rjValueEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_rjValueEditParam () throws GUIEDTException {
		rjValueList = new ParameterList();
		rjValueEditParam = new GUIParameterListParameter("RJ Parameters", rjValueList, "Edit RJ Params...",
							"Edit RJ Params", "RJ Parameters", null, null, false, gui_top.get_trace_events());
		rjValueEditParam.setInfo("Set Reasenberg and Jones parameter values");
		register_param (rjValueEditParam, "rjValueEditParam", PARMGRP_RJ_EDIT);

		updateRJValueParamList();

		return rjValueEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable the RJ parameter value controls.

	private boolean f_rj_value_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(aValRangeParam, f_rj_value_enable);
		enableParam(aValNumParam, f_rj_value_enable);
		enableParam(pValRangeParam, f_rj_value_enable);
		enableParam(pValNumParam, f_rj_value_enable);
		enableParam(cValRangeParam, f_rj_value_enable);
		enableParam(cValNumParam, f_rj_value_enable);

		enableParam(rjValueEditParam, f_sub_enable);

		// Parameters that are cleared when they are disabled

		if (!( f_rj_value_enable )) {
			updateParam(aValRangeParam, null);
			updateParam(aValNumParam, null);
			updateParam(pValRangeParam, null);
			updateParam(pValNumParam, null);
			updateParam(cValRangeParam, null);
			updateParam(cValNumParam, null);
		}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferRJValueView {

		// Range of a-values.
	
		public Range x_aValRangeParam;			// parameter value, checked for validity

		// Number of a-values.

		public int x_aValNumParam;				// parameter value, checked for validity

		// Range of p-values.
	
		public Range x_pValRangeParam;			// parameter value, checked for validity

		// Number of p-values.

		public int x_pValNumParam;				// parameter value, checked for validity

		// Range of c-values.
	
		public Range x_cValRangeParam;			// parameter value, checked for validity

		// Number of c-values.

		public int x_cValNumParam;				// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferRJValueImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferRJValueImpl extends XferRJValueView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferRJValueImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferRJValueImpl () {
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
		public XferRJValueImpl xfer_load () {

			// Clean state

			xfer_clean();

			// a-values

			x_aValRangeParam = validParam(aValRangeParam);
			x_aValNumParam = validParam(aValNumParam);
			gui_controller.validateRange(x_aValRangeParam, x_aValNumParam, "RJ a-value");

			// p-values

			x_pValRangeParam = validParam(pValRangeParam);
			x_pValNumParam = validParam(pValNumParam);
			gui_controller.validateRange(x_pValRangeParam, x_pValNumParam, "RJ p-value");

			// c-values

			x_cValRangeParam = validParam(cValRangeParam);
			x_cValNumParam = validParam(cValNumParam);
			gui_controller.validateRange(x_cValRangeParam, x_cValNumParam, "RJ c-value");

			int max_grid = 100000;
			max_grid = max_grid / x_aValNumParam;
			max_grid = max_grid / x_pValNumParam;
			max_grid = max_grid / x_cValNumParam;
			Preconditions.checkState(max_grid > 0, "RJ parameter search grid exceeds 100,000 entries");

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

	public OEGUISubRJValue (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = false;
		f_rj_value_enable = false;

		// Create and initialize controls

		init_rjValueDialogParam();
		init_rjValueEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the RJ parameter value edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_rjValueEditParam () throws GUIEDTException {
		return rjValueEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_rj_value_enable (boolean f_sub_enable, boolean f_rj_value_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_rj_value_enable = f_rj_value_enable;
		adjust_enable();
		return;
	}


	// Make an RJ value transfer object.

	public XferRJValueImpl make_rj_value_xfer () {
		return new XferRJValueImpl();
	}


	// Private function, used to report that the RJ parameter values have changed.

	private void report_rj_value_change () throws GUIEDTException {
		gui_controller.notify_aftershock_param_change();
		return;
	}


	// Update RJ values from model.

	public void update_rj_value_from_model () throws GUIEDTException {
		updateParam(aValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_a(), gui_model.get_seqSpecParams().get_max_a()));
		updateParam(aValNumParam, gui_model.get_seqSpecParams().get_num_a());
		updateParam(pValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_p(), gui_model.get_seqSpecParams().get_max_p()));
		updateParam(pValNumParam, gui_model.get_seqSpecParams().get_num_p());
		updateParam(cValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_c(), gui_model.get_seqSpecParams().get_max_c()));
		updateParam(cValNumParam, gui_model.get_seqSpecParams().get_num_c());

		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// RJ parameter edit button.
		// - Do nothing.

		case PARMGRP_RJ_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// RJ parameter value.
		// - Report to top-level controller.

		case PARMGRP_RJ_VALUE: {
			if (!( f_sub_enable && f_rj_value_enable )) {
				return;
			}
			report_rj_value_change();
		}
		break;




		// RJ parameter value, a-range.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_RJ_A: {
			if (!( f_sub_enable && f_rj_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(aValRangeParam, aValNumParam, 51, -4.5, -0.5);
			report_rj_value_change();
		}
		break;




		// RJ parameter value, p-range.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_RJ_P: {
			if (!( f_sub_enable && f_rj_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(pValRangeParam, pValNumParam, 45, 0.5, 2.0);
			report_rj_value_change();
		}
		break;




		// RJ parameter value, c-range.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_RJ_C: {
			if (!( f_sub_enable && f_rj_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(cValRangeParam, cValNumParam, 45, 0.018, 0.018);
			report_rj_value_change();
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubRJValue: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
