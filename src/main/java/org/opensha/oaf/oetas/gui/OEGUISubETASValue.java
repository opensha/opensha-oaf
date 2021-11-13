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
import org.opensha.oaf.util.gui.GUISeparatorParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Sub-controller for ETAS parameter values.
// Michael Barall 09/06/2021
//
// This is a modeless dialog for entering ETAS parameter values.


public class OEGUISubETASValue extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_ETAS_EDIT = 801;			// Button to open the ETAS parameter edit dialog
	private static final int PARMGRP_ETAS_VALUE = 802;			// ETAS parameter value
	private static final int PARMGRP_RANGE_ETAS_LOG_NMS = 803;	// ETAS parameter range log_nms
	private static final int PARMGRP_RANGE_ETAS_LOG_N = 804;	// ETAS parameter range log_n
	private static final int PARMGRP_RANGE_ETAS_P = 805;		// ETAS parameter range p
	private static final int PARMGRP_RANGE_ETAS_LOG_C = 806;	// ETAS parameter range log_c
	private static final int PARMGRP_ALPHA_EQUALS_B = 807;		// Option to take alpha == b




	//----- Controls for the ETAS parameter dialog -----


	// Range of a-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter lognmsETASValRangeParam;

	private RangeParameter init_lognmsETASValRangeParam () throws GUIEDTException {
		lognmsETASValRangeParam = new RangeParameter("ETAS log_nms Range", new Range(-2.5, 0.5));
		register_param (lognmsETASValRangeParam, "lognmsETASValRangeParam", PARMGRP_RANGE_ETAS_LOG_NMS);
		return lognmsETASValRangeParam;
	}


	// Number of a-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter lognmsETASValNumParam;

	private IntegerParameter init_lognmsETASValNumParam () throws GUIEDTException {
		lognmsETASValNumParam = new IntegerParameter("ETAS log_nms Number", 1, 10000, new Integer(51));
		lognmsETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (lognmsETASValNumParam, "lognmsETASValNumParam", PARMGRP_RANGE_ETAS_LOG_NMS);
		return lognmsETASValNumParam;
	}


	// Range of a-values; two edit boxes containing numbers.
	// Default is obtained from generic parameters when the data is loaded.

	private RangeParameter lognETASValRangeParam;

	private RangeParameter init_lognETASValRangeParam () throws GUIEDTException {
		lognETASValRangeParam = new RangeParameter("ETAS log_n Range", new Range(-2.5, 0.0));
		register_param (lognETASValRangeParam, "lognETASValRangeParam", PARMGRP_RANGE_ETAS_LOG_N);
		return lognETASValRangeParam;
	}


	// Number of a-values; edit box containing an integer.
	// Default is obtained from generic parameters when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter lognETASValNumParam;

	private IntegerParameter init_lognETASValNumParam () throws GUIEDTException {
		lognETASValNumParam = new IntegerParameter("ETAS log_n Number", 1, 10000, new Integer(31));
		lognETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (lognETASValNumParam, "lognETASValNumParam", PARMGRP_RANGE_ETAS_LOG_N);
		return lognETASValNumParam;
	}


	// Range of p-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter pETASValRangeParam;

	private RangeParameter init_pETASValRangeParam () throws GUIEDTException {
		pETASValRangeParam = new RangeParameter("ETAS p Range", new Range(0.5, 2.0));
		register_param (pETASValRangeParam, "pETASValRangeParam", PARMGRP_RANGE_ETAS_P);
		return pETASValRangeParam;
	}


	// Number of p-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter pETASValNumParam;

	private IntegerParameter init_pETASValNumParam () throws GUIEDTException {
		pETASValNumParam = new IntegerParameter("ETAS p Number", 1, 10000, new Integer(31));
		pETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (pETASValNumParam, "pETASValNumParam", PARMGRP_RANGE_ETAS_P);
		return pETASValNumParam;
	}


	// Range of c-values; two edit boxes containing numbers.
	// Default is an empty range obtained from generic parameters when the data is loaded.

	private RangeParameter logcETASValRangeParam;

	private RangeParameter init_logcETASValRangeParam () throws GUIEDTException {
		logcETASValRangeParam = new RangeParameter("ETAS log_c Range", new Range(-5.0, 0.0));
		register_param (logcETASValRangeParam, "logcETASValRangeParam", PARMGRP_RANGE_ETAS_LOG_C);
		return logcETASValRangeParam;
	}


	// Number of c-values; edit box containing an integer.
	// Default is 1, set when the data is loaded.
	// The value is forced == 1 when the range is empty, > 1 if the range is non-empty.

	private IntegerParameter logcETASValNumParam;

	private IntegerParameter init_logcETASValNumParam () throws GUIEDTException {
		logcETASValNumParam = new IntegerParameter("ETAS log_c Number", 1, 10000, new Integer(21));
		logcETASValNumParam.getConstraint().setNullAllowed(true);	// allows clearing when disabled
		register_param (logcETASValNumParam, "logcETASValNumParam", PARMGRP_RANGE_ETAS_LOG_C);
		return logcETASValNumParam;
	}


	// Option to use alpha == b.

	private BooleanParameter alphaEqualsBParam;

	private BooleanParameter init_alphaEqualsBParam () throws GUIEDTException {
		alphaEqualsBParam = new BooleanParameter("Use alpha = b", true);
		register_param (alphaEqualsBParam, "alphaEqualsBParam", PARMGRP_ALPHA_EQUALS_B);
		return alphaEqualsBParam;
	}


	// ETAS alpha parameter; edit box containing a number.

	private DoubleParameter alphaParam;

	private DoubleParameter init_alphaParam () throws GUIEDTException {
		alphaParam = new DoubleParameter("alpha", 1.0);	// can't use DoubleParameter("alpha", null) because the call would be ambiguous
		alphaParam.setValue(null);
		register_param (alphaParam, "alphaParam", PARMGRP_ETAS_VALUE);
		return alphaParam;
	}




	// ETAS parameter value dialog: initialize parameters within the dialog.

	private void init_etasValueDialogParam () throws GUIEDTException {
		
		init_lognmsETASValRangeParam();
		
		init_lognmsETASValNumParam();
		
		init_lognETASValRangeParam();
		
		init_lognETASValNumParam();
		
		init_pETASValRangeParam();
		
		init_pETASValNumParam();
		
		init_logcETASValRangeParam();
		
		init_logcETASValNumParam();

		init_alphaEqualsBParam();

		init_alphaParam();

		return;
	}




	// ETAS parameter edit: button to activate the dialog.

	private ParameterList etasValueList;			// List of parameters in the dialog
	private GUIParameterListParameter etasValueEditParam;
	

	private void updateETASValueParamList () throws GUIEDTException {
		etasValueList.clear();

		boolean f_button_row = false;

		etasValueEditParam.setListTitleText ("ETAS Parameters");
		etasValueEditParam.setDialogDimensions (gui_top.get_dialog_dims(10, f_button_row, 1));

		etasValueList.addParameter(lognmsETASValRangeParam);
		etasValueList.addParameter(lognmsETASValNumParam);
		etasValueList.addParameter(lognETASValRangeParam);
		etasValueList.addParameter(lognETASValNumParam);
		etasValueList.addParameter(pETASValRangeParam);
		etasValueList.addParameter(pETASValNumParam);
		etasValueList.addParameter(logcETASValRangeParam);
		etasValueList.addParameter(logcETASValNumParam);

		etasValueList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));

		etasValueList.addParameter(alphaEqualsBParam);
		etasValueList.addParameter(alphaParam);
		
		etasValueEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_etasValueEditParam () throws GUIEDTException {
		etasValueList = new ParameterList();
		etasValueEditParam = new GUIParameterListParameter("ETAS Parameters", etasValueList, "Edit ETAS Params...",
							"Edit ETAS Params", "ETAS Parameters", null, null, false, gui_top.get_trace_events());
		etasValueEditParam.setInfo("Set Reasenberg and Jones parameter values");
		register_param (etasValueEditParam, "etasValueEditParam", PARMGRP_ETAS_EDIT);

		updateETASValueParamList();

		return etasValueEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable the ETAS parameter value controls.

	private boolean f_etas_value_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		// Get flag indicating if alpha == b is forced

		boolean f_alpha_eq_b = true;
		if (f_etas_value_enable) {
			f_alpha_eq_b = validParam(alphaEqualsBParam);
		}

		// Enable parameters

		enableParam(lognmsETASValRangeParam, f_etas_value_enable);
		enableParam(lognmsETASValNumParam, f_etas_value_enable);
		enableParam(lognETASValRangeParam, f_etas_value_enable);
		enableParam(lognETASValNumParam, f_etas_value_enable);
		enableParam(pETASValRangeParam, f_etas_value_enable);
		enableParam(pETASValNumParam, f_etas_value_enable);
		enableParam(logcETASValRangeParam, f_etas_value_enable);
		enableParam(logcETASValNumParam, f_etas_value_enable);

		enableParam(alphaEqualsBParam, f_etas_value_enable);
		enableParam(alphaParam, !f_alpha_eq_b);

		enableParam(etasValueEditParam, f_sub_enable);

		// Parameters that are cleared when they are disabled

		if (!( f_etas_value_enable )) {
			updateParam(lognmsETASValRangeParam, null);
			updateParam(lognmsETASValNumParam, null);
			updateParam(lognETASValRangeParam, null);
			updateParam(lognETASValNumParam, null);
			updateParam(pETASValRangeParam, null);
			updateParam(pETASValNumParam, null);
			updateParam(logcETASValRangeParam, null);
			updateParam(logcETASValNumParam, null);

			updateParam(alphaEqualsBParam, true);

			updateParam(alphaParam, null);
		}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferETASValueView {

		// Range of log_nms.
	
		public Range x_lognmsETASValRangeParam;			// parameter value, checked for validity

		// Number of log_nms.

		public int x_lognmsETASValNumParam;				// parameter value, checked for validity

		// Range of log_n.
	
		public Range x_lognETASValRangeParam;			// parameter value, checked for validity

		// Number of log_n.

		public int x_lognETASValNumParam;				// parameter value, checked for validity

		// Range of p.
	
		public Range x_pETASValRangeParam;			// parameter value, checked for validity

		// Number of p.

		public int x_pETASValNumParam;				// parameter value, checked for validity

		// Range of log_c.
	
		public Range x_logcETASValRangeParam;			// parameter value, checked for validity

		// Number of log_c.

		public int x_logcETASValNumParam;				// parameter value, checked for validity

		// Flag indicating if alpha == b.

		public boolean x_alphaEqualsBParam;				// parameter value, checked for validity

		// Value of alpha, present if x_alphaEqualsBParam is false.

		public double x_alphaParam;				// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferETASValueImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferETASValueImpl extends XferETASValueView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferETASValueImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferETASValueImpl () {
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
		public XferETASValueImpl xfer_load () {

			// Clean state

			xfer_clean();

			// a-values

			x_lognmsETASValRangeParam = validParam(lognmsETASValRangeParam);
			x_lognmsETASValNumParam = validParam(lognmsETASValNumParam);
			gui_controller.validateRange(x_lognmsETASValRangeParam, x_lognmsETASValNumParam, "ETAS log_nms");

			// a-values

			x_lognETASValRangeParam = validParam(lognETASValRangeParam);
			x_lognETASValNumParam = validParam(lognETASValNumParam);
			gui_controller.validateRange(x_lognETASValRangeParam, x_lognETASValNumParam, "ETAS log_n");

			// p-values

			x_pETASValRangeParam = validParam(pETASValRangeParam);
			x_pETASValNumParam = validParam(pETASValNumParam);
			gui_controller.validateRange(x_pETASValRangeParam, x_pETASValNumParam, "ETAS p");

			// c-values

			x_logcETASValRangeParam = validParam(logcETASValRangeParam);
			x_logcETASValNumParam = validParam(logcETASValNumParam);
			gui_controller.validateRange(x_logcETASValRangeParam, x_logcETASValNumParam, "ETAS log_c");

			int max_grid = 2000000;
			max_grid = max_grid / x_lognmsETASValNumParam;
			max_grid = max_grid / x_lognETASValNumParam;
			max_grid = max_grid / x_pETASValNumParam;
			max_grid = max_grid / x_logcETASValNumParam;
			Preconditions.checkState(max_grid > 0, "ETAS parameter search grid exceeds 2,000,000 entries");

			// Flag indicating if alpha == b.

			x_alphaEqualsBParam = validParam(alphaEqualsBParam);

			// Value of alpha, present if x_alphaEqualsBParam is false.

			if (!( x_alphaEqualsBParam )) {
				x_alphaParam = validParam(alphaParam);
			}

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// If we're forcing alpha == b, then update alpha with b-value from the model

			if (x_alphaEqualsBParam) {
				updateParam(alphaParam, gui_model.get_cat_bParam());
			}

			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubETASValue (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = false;
		f_etas_value_enable = false;

		// Create and initialize controls

		init_etasValueDialogParam();
		init_etasValueEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the ETAS parameter value edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_etasValueEditParam () throws GUIEDTException {
		return etasValueEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_etas_value_enable (boolean f_sub_enable, boolean f_etas_value_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_etas_value_enable = f_etas_value_enable;
		adjust_enable();
		return;
	}


	// Make an ETAS value transfer object.

	public XferETASValueImpl make_etas_value_xfer () {
		return new XferETASValueImpl();
	}


	// Private function, used to report that the ETAS parameter values have changed.

	private void report_etas_value_change () throws GUIEDTException {
		gui_controller.notify_aftershock_param_change();
		return;
	}


	// Update ETAS values from model.
	// This is called after catalog fetch or load.

	public void update_etas_value_from_model () throws GUIEDTException {
		//updateParam(lognmsETASValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_a(), gui_model.get_seqSpecParams().get_max_a()));
		//updateParam(lognmsETASValNumParam, gui_model.get_seqSpecParams().get_num_a());
		//updateParam(lognETASValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_a(), gui_model.get_seqSpecParams().get_max_a()));
		//updateParam(lognETASValNumParam, gui_model.get_seqSpecParams().get_num_a());
		//updateParam(pETASValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_p(), gui_model.get_seqSpecParams().get_max_p()));
		//updateParam(pETASValNumParam, gui_model.get_seqSpecParams().get_num_p());
		//updateParam(logcETASValRangeParam, new Range(gui_model.get_seqSpecParams().get_min_c(), gui_model.get_seqSpecParams().get_max_c()));
		//updateParam(logcETASValNumParam, gui_model.get_seqSpecParams().get_num_c());

		updateParam(lognmsETASValRangeParam, new Range(-2.5, 0.5));
		updateParam(lognmsETASValNumParam, 51);
		updateParam(lognETASValRangeParam, new Range(-2.5, 0.0));
		updateParam(lognETASValNumParam, 31);
		updateParam(pETASValRangeParam, new Range(0.5, 2.0));
		updateParam(pETASValNumParam, 31);
		updateParam(logcETASValRangeParam, new Range(-5.0, 0.0));
		updateParam(logcETASValNumParam, 21);

		updateParam(alphaEqualsBParam, true);
		updateParam(alphaParam, 1.0);

		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// ETAS parameter edit button.
		// - Do nothing.

		case PARMGRP_ETAS_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// ETAS parameter value.
		// - Report to top-level controller.

		case PARMGRP_ETAS_VALUE: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, log_nms.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_LOG_NMS: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(lognmsETASValRangeParam, lognmsETASValNumParam, 51, -2.5, 0.5);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, log_n.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_LOG_N: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(lognETASValRangeParam, lognETASValNumParam, 31, -2.5, 0.0);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, p.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_P: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(pETASValRangeParam, pETASValNumParam, 31, 0.5, 2.0);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, log_c.
		// - Adjust parameters if needed.
		// - Report to top-level controller.

		case PARMGRP_RANGE_ETAS_LOG_C: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			gui_controller.updateRangeParams(logcETASValRangeParam, logcETASValNumParam, 21, -5.0, 0.0);
			report_etas_value_change();
		}
		break;




		// ETAS parameter value, log_c.
		// - Report to top-level controller.
		// - Adjust enable.

		case PARMGRP_ALPHA_EQUALS_B: {
			if (!( f_sub_enable && f_etas_value_enable )) {
				return;
			}
			report_etas_value_change();
			adjust_enable();
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubETASValue: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
