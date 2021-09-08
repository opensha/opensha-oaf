package org.opensha.oaf.oetas.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.geom.Point2D;
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
import org.opensha.commons.gui.plot.jfreechart.xyzPlot.XYZGraphPanel;
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
import org.opensha.oaf.util.GUIConsoleWindow;
import org.opensha.oaf.util.GUICalcStep;
import org.opensha.oaf.util.GUICalcRunnable;
import org.opensha.oaf.util.GUICalcProgressBar;
import org.opensha.oaf.util.GUIEDTException;
import org.opensha.oaf.util.GUIEDTRunnable;
import org.opensha.oaf.util.GUIEventAlias;
import org.opensha.oaf.util.GUIExternalCatalog;
import org.opensha.oaf.util.GUIParameterListParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Sub-controller for region parameters.
// Michael Barall 08/18/2021
//
// This is a modeless dialog for entering region parameters,
// which define the Comcat search region.


public class OEGUISubRegion extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_REGION_PARAM = 201;	// Region parameters
	private static final int PARMGRP_REGION_TYPE = 202;		// Drop-down to select region type
	private static final int PARMGRP_REGION_EDIT = 203;		// Button to open the region edit dialog




	//----- Controls for the Region dialog -----




	// Region type; dropdown containing an enumeration to select type of search region.

	private EnumParameter<RegionType> regionTypeParam;

	private EnumParameter<RegionType> init_regionTypeParam () throws GUIEDTException {
		regionTypeParam = new EnumParameter<RegionType>(
				"Region Type", EnumSet.allOf(RegionType.class), RegionType.STANDARD, null);
		regionTypeParam.setInfo("Type of region for collecting aftershocks");
		register_param (regionTypeParam, "regionTypeParam", PARMGRP_REGION_TYPE);
		return regionTypeParam;
	}


	// Region dialog: parameters within the dialog to select search region location, size, and shape.
	
	private DoubleParameter radiusParam;		// Search radius; appears when Region Type is Circular
	private DoubleParameter minLatParam;		// Minimum latitude; appears when Region Type is Rectangular
	private DoubleParameter maxLatParam;		// Maximum latitude; appears when Region Type is Rectangular
	private DoubleParameter minLonParam;		// Minimum longitude; appears when Region Type is Rectangular
	private DoubleParameter maxLonParam;		// Maximum longitude; appears when Region Type is Rectangular
	private DoubleParameter minDepthParam;		// Minimum depth; always appears
	private DoubleParameter maxDepthParam;		// Maximum depth; always appears
	private DoubleParameter wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
	private DoubleParameter minRadiusParam;		// Minimum radius; appears for WC regions
	private DoubleParameter maxRadiusParam;		// Maximum radius; appears for WC regions
	private DoubleParameter centerLatParam;		// Circle center latitude; appears for custom circle
	private DoubleParameter centerLonParam;		// Circle center longitude; appears for custom circle


	private void init_regionDialogParam () throws GUIEDTException {
		radiusParam = new DoubleParameter("Radius", 0d, 20000d, new Double(20d));
		radiusParam.setUnits("km");
		radiusParam.setInfo("Radius of circular region");
		register_param (radiusParam, "radiusParam", PARMGRP_REGION_PARAM);

		minLatParam = new DoubleParameter("Min Lat", -90d, 90d, new Double(32d));
		minLatParam.setInfo("Minimum latitude of rectangular region");
		register_param (minLatParam, "minLatParam", PARMGRP_REGION_PARAM);

		maxLatParam = new DoubleParameter("Max Lat", -90d, 90d, new Double(36d));
		maxLatParam.setInfo("Maximum latitude of rectangular region");
		register_param (maxLatParam, "maxLatParam", PARMGRP_REGION_PARAM);

		minLonParam = new DoubleParameter("Min Lon", -180d, 180d, new Double(32d));
		minLonParam.setInfo("Minimum longitude of rectangular region");
		register_param (minLonParam, "minLonParam", PARMGRP_REGION_PARAM);

		maxLonParam = new DoubleParameter("Max Lon", -180d, 180d, new Double(36d));
		maxLonParam.setInfo("Maximum longitude of rectangular region");
		register_param (maxLonParam, "maxLonParam", PARMGRP_REGION_PARAM);

		minDepthParam = new DoubleParameter("Min Depth", ComcatOAFAccessor.DEFAULT_MIN_DEPTH, ComcatOAFAccessor.DEFAULT_MAX_DEPTH, new Double(ComcatOAFAccessor.DEFAULT_MIN_DEPTH));
		minDepthParam.setUnits("km");
		minDepthParam.setInfo("Minimum depth of region");
		register_param (minDepthParam, "minDepthParam", PARMGRP_REGION_PARAM);
		
		maxDepthParam = new DoubleParameter("Max Depth", ComcatOAFAccessor.DEFAULT_MIN_DEPTH, ComcatOAFAccessor.DEFAULT_MAX_DEPTH, new Double(ComcatOAFAccessor.DEFAULT_MAX_DEPTH));
		maxDepthParam.setUnits("km");
		maxDepthParam.setInfo("Maximum depth of region");
		register_param (maxDepthParam, "maxDepthParam", PARMGRP_REGION_PARAM);
		
		wcMultiplierParam = new DoubleParameter("WC Multiplier", 0d, 100d, new Double(1d));
		wcMultiplierParam.setInfo("Multiplier for WC radius");
		register_param (wcMultiplierParam, "wcMultiplierParam", PARMGRP_REGION_PARAM);

		minRadiusParam = new DoubleParameter("Min Radius", 0d, 20000d, new Double(10d));
		minRadiusParam.setUnits("km");
		minRadiusParam.setInfo("Minimum radius of circular region");
		register_param (minRadiusParam, "minRadiusParam", PARMGRP_REGION_PARAM);
		
		maxRadiusParam = new DoubleParameter("Max Radius", 0d, 20000d, new Double(2000d));
		maxRadiusParam.setUnits("km");
		maxRadiusParam.setInfo("Maximum radius of circular region");
		register_param (maxRadiusParam, "maxRadiusParam", PARMGRP_REGION_PARAM);
		
		centerLatParam = new DoubleParameter("Center Lat", -90d, 90d, new Double(34d));
		centerLatParam.setInfo("Latitude of center of circular region");
		register_param (centerLatParam, "centerLatParam", PARMGRP_REGION_PARAM);

		centerLonParam = new DoubleParameter("Center Lon", -180d, 180d, new Double(34d));
		centerLonParam.setInfo("Longitude of center of circular region");
		register_param (centerLonParam, "centerLonParam", PARMGRP_REGION_PARAM);

		return;
	}


	// Region edit: button to activate the dialog.

	private ParameterList regionList;			// List of parameters in the dialog
	private GUIParameterListParameter regionEditParam;
	

	private void updateRegionParamList (RegionType type) throws GUIEDTException {
		regionList.clear();

		boolean f_button_row = false;

		if (gui_top.get_trace_events()) {
			f_button_row = true;
		}
		
		switch (type) {

		case STANDARD:
			regionEditParam.setListTitleText ("Standard");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(0, f_button_row));
			break;

		case CENTROID_WC_CIRCLE:
			regionList.addParameter(wcMultiplierParam);
			regionList.addParameter(minRadiusParam);
			regionList.addParameter(maxRadiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Centroid WC Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(5, f_button_row));
			break;

		case CENTROID_CIRCLE:
			regionList.addParameter(radiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Centroid Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(3, f_button_row));
			break;

		case EPICENTER_WC_CIRCLE:
			regionList.addParameter(wcMultiplierParam);
			regionList.addParameter(minRadiusParam);
			regionList.addParameter(maxRadiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Epicenter WC Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(5, f_button_row));
			break;

		case EPICENTER_CIRCLE:
			regionList.addParameter(radiusParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Epicenter Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(3, f_button_row));
			break;

		case CUSTOM_CIRCLE:
			regionList.addParameter(radiusParam);
			regionList.addParameter(centerLatParam);
			regionList.addParameter(centerLonParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Custom Circle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(5, f_button_row));
			break;

		case CUSTOM_RECTANGLE:
			regionList.addParameter(minLatParam);
			regionList.addParameter(maxLatParam);
			regionList.addParameter(minLonParam);
			regionList.addParameter(maxLonParam);
			regionList.addParameter(minDepthParam);
			regionList.addParameter(maxDepthParam);
			regionEditParam.setListTitleText ("Custom Rectangle");
			regionEditParam.setDialogDimensions (gui_top.get_dialog_dims(6, f_button_row));
			break;

		default:
			throw new IllegalStateException("Unknown region type: " + type);
		}
		
		regionEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_regionEditParam () throws GUIEDTException {
		regionList = new ParameterList();
		if (gui_top.get_trace_events()) {
			regionEditParam = new GUIParameterListParameter("Region", regionList, "Edit Region...",
								"Edit Region", "Region Parameters", "Done", "Cancel", false, gui_top.get_trace_events());
		} else {
			regionEditParam = new GUIParameterListParameter("Region", regionList, "Edit Region...",
								"Edit Region", "Region Parameters", null, null, false, gui_top.get_trace_events());
		}
		regionEditParam.setInfo("Select the region to search for aftershocks");
		register_param (regionEditParam, "regionEditParam", PARMGRP_REGION_EDIT);

		updateRegionParamList(regionTypeParam.getValue());

		return regionEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;


	// Adjust the enable/disable state of our controls.
	// Note: Controls within the dialog do not need to be enabled/disabled
	// because the dialog is closed when the activation button is disabled.
	//
	// Type control is enabled if sub-controller is enabled.
	// Edit control is enabled if sub-controller is enabled and type is not standard.

	private void adjust_enable () throws GUIEDTException {
		boolean f_edit = false;
		if (f_sub_enable) {
			f_edit = (validParam(regionTypeParam) != RegionType.STANDARD);
		}
		enableParam(regionTypeParam, f_sub_enable);
		enableParam(regionEditParam, f_edit);
		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferRegionView {

		// Search region.
		// Only includes values used for the selected region type.
		// Included values are checked for validity

		public RegionType x_regionTypeParam;	// Region type

		public double x_radiusParam;		// Search radius; appears when Region Type is Circular
		public double x_minLatParam;		// Minimum latitude; appears when Region Type is Rectangular
		public double x_maxLatParam;		// Maximum latitude; appears when Region Type is Rectangular
		public double x_minLonParam;		// Minimum longitude; appears when Region Type is Rectangular
		public double x_maxLonParam;		// Maximum longitude; appears when Region Type is Rectangular
		public double x_minDepthParam;		// Minimum depth; always appears
		public double x_maxDepthParam;		// Maximum depth; always appears
		public double x_wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
		public double x_minRadiusParam;		// Minimum radius; appears for WC regions
		public double x_maxRadiusParam;		// Maximum radius; appears for WC regions
		public double x_centerLatParam;		// Circle center latitude; appears for custom circle
		public double x_centerLonParam;		// Circle center longitude; appears for custom circle

		// Get the implementation class.

		public abstract XferRegionImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferRegionImpl extends XferRegionView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferRegionImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferRegionImpl () {
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
		public XferRegionImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Search region

			x_regionTypeParam = validParam(regionTypeParam);
		
			switch (x_regionTypeParam) {

			case STANDARD:
				// do nothing
				break;

			case CENTROID_WC_CIRCLE:
				x_wcMultiplierParam = validParam(wcMultiplierParam);
				x_minRadiusParam = validParam(minRadiusParam);
				x_maxRadiusParam = validParam(maxRadiusParam);
				x_minDepthParam = validParam(minDepthParam);
				x_maxDepthParam = validParam(maxDepthParam);
				break;

			case CENTROID_CIRCLE:
				x_radiusParam = validParam(radiusParam);
				x_minDepthParam = validParam(minDepthParam);
				x_maxDepthParam = validParam(maxDepthParam);
				break;

			case EPICENTER_WC_CIRCLE:
				x_wcMultiplierParam = validParam(wcMultiplierParam);
				x_minRadiusParam = validParam(minRadiusParam);
				x_maxRadiusParam = validParam(maxRadiusParam);
				x_minDepthParam = validParam(minDepthParam);
				x_maxDepthParam = validParam(maxDepthParam);
				break;

			case EPICENTER_CIRCLE:
				x_radiusParam = validParam(radiusParam);
				x_minDepthParam = validParam(minDepthParam);
				x_maxDepthParam = validParam(maxDepthParam);
				break;

			case CUSTOM_CIRCLE:
				x_radiusParam = validParam(radiusParam);
				x_centerLatParam = validParam(centerLatParam);
				x_centerLonParam = validParam(centerLonParam);
				x_minDepthParam = validParam(minDepthParam);
				x_maxDepthParam = validParam(maxDepthParam);
				break;

			case CUSTOM_RECTANGLE:
				x_minLatParam = validParam(minLatParam);
				x_maxLatParam = validParam(maxLatParam);
				x_minLonParam = validParam(minLonParam);
				x_maxLonParam = validParam(maxLonParam);
				x_minDepthParam = validParam(minDepthParam);
				x_maxDepthParam = validParam(maxDepthParam);
				break;

			default:
				throw new IllegalStateException("Unknown region type: " + x_regionTypeParam);
			}

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

	public OEGUISubRegion (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Set flag to enable controls

		f_sub_enable = true;

		// Create and initialize controls

		init_regionTypeParam();
		init_regionDialogParam();
		init_regionEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the region type dropdown.
	// The intent is to use this only to insert the control into the client.

	public EnumParameter<RegionType> get_regionTypeParam () throws GUIEDTException {
		return regionTypeParam;
	}


	// Get the region edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_regionEditParam () throws GUIEDTException {
		return regionEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_region_enable (boolean f_enabled) throws GUIEDTException {
		f_sub_enable = f_enabled;
		adjust_enable();
		return;
	}


	// Make a region transfer object.

	public XferRegionImpl make_region_xfer () {
		return new XferRegionImpl();
	}


	// Private function, used to report that the region parameters have changed.

	private void report_region_change () throws GUIEDTException {
		gui_controller.notify_data_source_change();
		return;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {


		// Region parameters.
		// - Report to top-level controller.

		case PARMGRP_REGION_PARAM: {
			if (!( f_sub_enable )) {
				return;
			}
			report_region_change();
		}
		break;


		// Region type.
		// - Update the edit region dialog, to include the controls for the selected region type.
		// - Enable or disable the edit button.
		// - Report to top-level controller.

		case PARMGRP_REGION_TYPE: {
			if (!( f_sub_enable )) {
				return;
			}
			updateRegionParamList(validParam(regionTypeParam));
			adjust_enable();
			report_region_change();
		}
		break;


		// Region edit button.
		// - Do nothing.

		case PARMGRP_REGION_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;


		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubRegion: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
