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
import org.opensha.oaf.util.gui.GUIHelpListener;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.ForecastParameters;
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
	private DoubleParameter minDepthParam;		// Minimum depth; appears for all except Standard
	private DoubleParameter maxDepthParam;		// Maximum depth; appears for all except Standard
	private DoubleParameter wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
	private DoubleParameter minRadiusParam;		// Minimum radius; appears for WC regions
	private DoubleParameter maxRadiusParam;		// Maximum radius; appears for WC regions
	private DoubleParameter centerLatParam;		// Circle center latitude; appears for custom circle
	private DoubleParameter centerLonParam;		// Circle center longitude; appears for custom circle


	private void init_regionDialogParam () throws GUIEDTException {
		radiusParam = new DoubleParameter("Radius", 0d, 20000d, Double.valueOf(20d));
		radiusParam.setUnits("km");
		radiusParam.setInfo("Radius of circular region");
		register_param (radiusParam, "radiusParam", PARMGRP_REGION_PARAM);

		minLatParam = new DoubleParameter("Min Latitude", -90d, 90d, Double.valueOf(32d));
		minLatParam.setInfo("Minimum latitude of rectangular region");
		register_param (minLatParam, "minLatParam", PARMGRP_REGION_PARAM);

		maxLatParam = new DoubleParameter("Max Latitude", -90d, 90d, Double.valueOf(36d));
		maxLatParam.setInfo("Maximum latitude of rectangular region");
		register_param (maxLatParam, "maxLatParam", PARMGRP_REGION_PARAM);

		minLonParam = new DoubleParameter("Min Longitude", -180d, 180d, Double.valueOf(32d));
		minLonParam.setInfo("Minimum longitude of rectangular region");
		register_param (minLonParam, "minLonParam", PARMGRP_REGION_PARAM);

		maxLonParam = new DoubleParameter("Max Longitude", -180d, 180d, Double.valueOf(36d));
		maxLonParam.setInfo("Maximum longitude of rectangular region");
		register_param (maxLonParam, "maxLonParam", PARMGRP_REGION_PARAM);

		minDepthParam = new DoubleParameter("Min Depth", ComcatOAFAccessor.DEFAULT_MIN_DEPTH, ComcatOAFAccessor.DEFAULT_MAX_DEPTH, Double.valueOf(ComcatOAFAccessor.DEFAULT_MIN_DEPTH));
		minDepthParam.setUnits("km");
		minDepthParam.setInfo("Minimum depth of region");
		register_param (minDepthParam, "minDepthParam", PARMGRP_REGION_PARAM);
		
		maxDepthParam = new DoubleParameter("Max Depth", ComcatOAFAccessor.DEFAULT_MIN_DEPTH, ComcatOAFAccessor.DEFAULT_MAX_DEPTH, Double.valueOf(ComcatOAFAccessor.DEFAULT_MAX_DEPTH));
		maxDepthParam.setUnits("km");
		maxDepthParam.setInfo("Maximum depth of region");
		register_param (maxDepthParam, "maxDepthParam", PARMGRP_REGION_PARAM);
		
		wcMultiplierParam = new DoubleParameter("WC Multiplier", 0d, 100d, Double.valueOf(1d));
		wcMultiplierParam.setInfo("Multiplier for WC radius");
		register_param (wcMultiplierParam, "wcMultiplierParam", PARMGRP_REGION_PARAM);

		minRadiusParam = new DoubleParameter("Min Radius", 0d, 20000d, Double.valueOf(10d));
		minRadiusParam.setUnits("km");
		minRadiusParam.setInfo("Minimum radius of circular region");
		register_param (minRadiusParam, "minRadiusParam", PARMGRP_REGION_PARAM);
		
		maxRadiusParam = new DoubleParameter("Max Radius", 0d, 20000d, Double.valueOf(2000d));
		maxRadiusParam.setUnits("km");
		maxRadiusParam.setInfo("Maximum radius of circular region");
		register_param (maxRadiusParam, "maxRadiusParam", PARMGRP_REGION_PARAM);
		
		centerLatParam = new DoubleParameter("Center Latitude", -90d, 90d, Double.valueOf(34d));
		centerLatParam.setInfo("Latitude of center of circular region");
		register_param (centerLatParam, "centerLatParam", PARMGRP_REGION_PARAM);

		centerLonParam = new DoubleParameter("Center Longitude", -180d, 180d, Double.valueOf(34d));
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
		GUIHelpListener help_listener = gui_top.make_help (() -> {
			String help_file = null;
			switch (regionTypeParam.getValue()) {
			case STANDARD:             help_file = null;                             break;
			case CENTROID_WC_CIRCLE:   help_file = "help_region_centroid_wc.html";   break;
			case CENTROID_CIRCLE:      help_file = "help_region_centroid.html";      break;
			case EPICENTER_WC_CIRCLE:  help_file = "help_region_epicenter_wc.html";  break;
			case EPICENTER_CIRCLE:     help_file = "help_region_epicenter.html";     break;
			case CUSTOM_CIRCLE:        help_file = "help_region_circle.html";        break;
			case CUSTOM_RECTANGLE:     help_file = "help_region_rectangle.html";     break;
			}
			return help_file;
		});
		if (gui_top.get_trace_events()) {
			regionEditParam = new GUIParameterListParameter("Region", regionList, "Edit Region...",
								"Edit Region", "Region Parameters", "Done", "Cancel", false, gui_top.get_trace_events(), help_listener);
		} else {
			regionEditParam = new GUIParameterListParameter("Region", regionList, "Edit Region...",
								"Edit Region", "Region Parameters", null, null, false, gui_top.get_trace_events(), help_listener);
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





	//----- Region operations -----




	// Class to hold a region specification.

	public static class RegionSpec {

		// Search region.

		public RegionType x_regionTypeParam;	// Region type

		public double x_radiusParam;		// Search radius; appears when Region Type is Circular
		public double x_minLatParam;		// Minimum latitude; appears when Region Type is Rectangular
		public double x_maxLatParam;		// Maximum latitude; appears when Region Type is Rectangular
		public double x_minLonParam;		// Minimum longitude; appears when Region Type is Rectangular
		public double x_maxLonParam;		// Maximum longitude; appears when Region Type is Rectangular
		public double x_minDepthParam;		// Minimum depth; appears for all except Standard
		public double x_maxDepthParam;		// Maximum depth; appears for all except Standard
		public double x_wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
		public double x_minRadiusParam;		// Minimum radius; appears for WC regions
		public double x_maxRadiusParam;		// Maximum radius; appears for WC regions
		public double x_centerLatParam;		// Circle center latitude; appears for custom circle
		public double x_centerLonParam;		// Circle center longitude; appears for custom circle


		// Return true if this is a Standard region.

		public final boolean is_standard_region () {
			return (x_regionTypeParam == RegionType.STANDARD);
		}


		// Set to a Standard region.

		public final void set_region_standard () {

			x_regionTypeParam = RegionType.STANDARD;

			x_radiusParam = 0.0;
			x_minLatParam = 0.0;
			x_maxLatParam = 0.0;
			x_minLonParam = 0.0;
			x_maxLonParam = 0.0;
			x_minDepthParam = 0.0;
			x_maxDepthParam = 0.0;
			x_wcMultiplierParam = 0.0;
			x_minRadiusParam = 0.0;
			x_maxRadiusParam = 0.0;
			x_centerLatParam = 0.0;
			x_centerLonParam = 0.0;

			return;
		}


		// Set to a Centroid WC Circle region.

		public final void set_region_centroid_wc_circle (
			double wc_mult,
			double min_radius,
			double max_radius,
			double min_depth,
			double max_depth
		) {
			set_region_standard();

			x_regionTypeParam = RegionType.CENTROID_WC_CIRCLE;
			x_minRadiusParam = min_radius;
			x_maxRadiusParam = max_radius;
			x_minDepthParam = min_depth;
			x_maxDepthParam = max_depth;

			return;
		}


		// Set to a Centroid Circle region.

		public final void set_region_centroid_circle (
			double radius,
			double min_depth,
			double max_depth
		) {
			set_region_standard();

			x_regionTypeParam = RegionType.CENTROID_CIRCLE;
			x_radiusParam = radius;
			x_minDepthParam = min_depth;
			x_maxDepthParam = max_depth;

			return;
		}


		// Set to an Epicenter WC Circle region.

		public final void set_region_epicenter_wc_circle (
			double wc_mult,
			double min_radius,
			double max_radius,
			double min_depth,
			double max_depth
		) {
			set_region_standard();

			x_regionTypeParam = RegionType.EPICENTER_WC_CIRCLE;
			x_minRadiusParam = min_radius;
			x_maxRadiusParam = max_radius;
			x_minDepthParam = min_depth;
			x_maxDepthParam = max_depth;

			return;
		}


		// Set to an Epicenter Circle region.

		public final void set_region_epicenter_circle (
			double radius,
			double min_depth,
			double max_depth
		) {
			set_region_standard();

			x_regionTypeParam = RegionType.EPICENTER_CIRCLE;
			x_radiusParam = radius;
			x_minDepthParam = min_depth;
			x_maxDepthParam = max_depth;

			return;
		}


		// Set to a Custom Circle region.

		public final void set_region_custom_circle (
			double radius,
			double center_lat,
			double center_lon,
			double min_depth,
			double max_depth
		) {
			set_region_standard();

			x_regionTypeParam = RegionType.CUSTOM_CIRCLE;
			x_radiusParam = radius;
			x_centerLatParam = center_lat;
			x_centerLonParam = center_lon;
			x_minDepthParam = min_depth;
			x_maxDepthParam = max_depth;

			return;
		}


		// Set to a Custom Rectangle region.

		public final void set_region_custom_rectangle (
			double min_lat,
			double max_lat,
			double min_lon,
			double max_lon,
			double min_depth,
			double max_depth
		) {
			set_region_standard();

			x_regionTypeParam = RegionType.CUSTOM_RECTANGLE;
			x_minLatParam = min_lat;
			x_maxLatParam = max_lat;
			x_minLonParam = min_lon;
			x_maxLonParam = max_lon;
			x_minDepthParam = min_depth;
			x_maxDepthParam = max_depth;

			return;
		}


		// Constructor sets up a Standard region.

		public RegionSpec () {
			set_region_standard();
		}


		// Set the region specified by the given analyst parameters.
		// Parmaeters:
		//  anparam = Forecast parameters, as they would appear in AnalystOptions. Can be null.
		//  loc = Mainshock location.
		// Returns this object.

		public final RegionSpec set_region_for_analyst (ForecastParameters anparam, Location loc) {

			// Check for standard region

			if (is_anparam_standard_region (anparam, loc)) {
				set_region_standard();
				return this;
			}

			// Default depths

			double min_depth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
			double max_depth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

			// If aftershock search parameters are available ...

			if (anparam.aftershock_search_avail) {

				// Adjust depths if they are not defaulted

				if (!( anparam.min_depth >= ForecastParameters.SEARCH_PARAM_TEST || Math.abs (anparam.min_depth - ComcatOAFAccessor.DEFAULT_MIN_DEPTH) <= 1.0e-3 )) {
					min_depth = anparam.min_depth;
				}
				if (!( anparam.max_depth >= ForecastParameters.SEARCH_PARAM_TEST || Math.abs (anparam.max_depth - ComcatOAFAccessor.DEFAULT_MAX_DEPTH) <= 1.0e-3 )) {
					max_depth = anparam.max_depth;
				}

				// If we have a custom region ...

				if (!( anparam.aftershock_search_region == null )) {

					// Circular region

					if (anparam.aftershock_search_region.isCircular()) {
						set_region_custom_circle (
							anparam.aftershock_search_region.getCircleRadiusKm(),
							anparam.aftershock_search_region.getCircleCenterLat(),
							anparam.aftershock_search_region.getCircleCenterLon(),
							min_depth,
							max_depth
						);
						return this;
					}

					// Rectangular region (treat any non-circle as a rectangle)

					set_region_custom_rectangle (
						anparam.aftershock_search_region.getMinLat(),
						anparam.aftershock_search_region.getMaxLat(),
						anparam.aftershock_search_region.getMinLon(),
						anparam.aftershock_search_region.getMaxLon(),
						min_depth,
						max_depth
					);
					return this;
				}
			}

			// If magnitude of completeness parameters are available (should always be true) ...

			if (anparam.mag_comp_avail) {

				//SearchMagFn magSample = anparam.mag_comp_params.get_fcn_magSample();
				SearchRadiusFn radiusSample = anparam.mag_comp_params.get_fcn_radiusSample();
				SearchMagFn magCentroid = anparam.mag_comp_params.get_fcn_magCentroid();
				//SearchRadiusFn radiusCentroid = anparam.mag_comp_params.get_fcn_radiusCentroid();

				// If constant search radius ...

				if (radiusSample.getDefaultGUIIsConstant()) {

					// Get the radius

					double radius = radiusSample.getDefaultGUIRadiusMin();

					// If skip centroid, Epicenter Circle
					
					if (magCentroid.isSkipCentroid()) {
						set_region_epicenter_circle (
							radius,
							min_depth,
							max_depth
						);
						return this;
					}

					// Otherwise, Centroid Circle

					set_region_centroid_circle (
						radius,
						min_depth,
						max_depth
					);
					return this;
				}

				// Otherwise, WC search radius ...

				double wc_mult = radiusSample.getDefaultGUIRadiusMult();
				double min_radius = radiusSample.getDefaultGUIRadiusMin();
				double max_radius = radiusSample.getDefaultGUIRadiusMax();

				// If skip centroid, Epicenter WC Circle
					
				if (magCentroid.isSkipCentroid()) {
					set_region_epicenter_wc_circle (
						wc_mult,
						min_radius,
						max_radius,
						min_depth,
						max_depth
					);
					return this;
				}

				// Otherwise, Centroid WC Circle

				set_region_centroid_wc_circle (
					wc_mult,
					min_radius,
					max_radius,
					min_depth,
					max_depth
				);
				return this;

			}

			// No magnitude of completeness parameters (should never get here), set Standard region

			set_region_standard();
			return this;
		}

		// Display the region specification.
		// This produces multi-line output with each line ending in newline.

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append ("region_type = " + x_regionTypeParam.toString() + "\n");
		
			switch (x_regionTypeParam) {

			case STANDARD:
				// do nothing
				break;

			case CENTROID_WC_CIRCLE:
				result.append ("wc_mult = " + x_wcMultiplierParam + "\n");
				result.append ("min_radius = " + x_minRadiusParam + "\n");
				result.append ("max_radius = " + x_maxRadiusParam + "\n");
				result.append ("min_depth = " + x_minDepthParam + "\n");
				result.append ("max_depth = " + x_maxDepthParam + "\n");
				break;

			case CENTROID_CIRCLE:
				result.append ("radius = " + x_radiusParam + "\n");
				result.append ("min_depth = " + x_minDepthParam + "\n");
				result.append ("max_depth = " + x_maxDepthParam + "\n");
				break;

			case EPICENTER_WC_CIRCLE:
				result.append ("wc_mult = " + x_wcMultiplierParam + "\n");
				result.append ("min_radius = " + x_minRadiusParam + "\n");
				result.append ("max_radius = " + x_maxRadiusParam + "\n");
				result.append ("min_depth = " + x_minDepthParam + "\n");
				result.append ("max_depth = " + x_maxDepthParam + "\n");
				break;

			case EPICENTER_CIRCLE:
				result.append ("radius = " + x_radiusParam + "\n");
				result.append ("min_depth = " + x_minDepthParam + "\n");
				result.append ("max_depth = " + x_maxDepthParam + "\n");
				break;

			case CUSTOM_CIRCLE:
				result.append ("radius = " + x_radiusParam + "\n");
				result.append ("center_lat = " + x_centerLatParam + "\n");
				result.append ("center_lon = " + x_centerLonParam + "\n");
				result.append ("min_depth = " + x_minDepthParam + "\n");
				result.append ("max_depth = " + x_maxDepthParam + "\n");
				break;

			case CUSTOM_RECTANGLE:
				result.append ("min_lat = " + x_minLatParam + "\n");
				result.append ("max_lat = " + x_maxLatParam + "\n");
				result.append ("min_lon = " + x_minLonParam + "\n");
				result.append ("max_lon = " + x_maxLonParam + "\n");
				result.append ("min_depth = " + x_minDepthParam + "\n");
				result.append ("max_depth = " + x_maxDepthParam + "\n");
				break;

			default:
				throw new IllegalStateException("Unknown region type: " + x_regionTypeParam);
			}

			return result.toString();
		}

	}




	// Test if two sets of magnitude of completeness parameters are considered
	// to refer to the same search region.
	// Note: This returns true if:
	//  - Both centroid magnitudes are skip-centroid or not skip-centroid.
	//  - Both search radius functions are the same (within tolerance).
	// Note that search magniudes and centroid radius are not considered.

	public static boolean is_mag_comp_same_region (MagCompPage_Parameters mag_comp_1, MagCompPage_Parameters mag_comp_2) {

		//SearchMagFn magSample_1 = mag_comp_1.get_fcn_magSample();
		SearchRadiusFn radiusSample_1 = mag_comp_1.get_fcn_radiusSample();
		SearchMagFn magCentroid_1 = mag_comp_1.get_fcn_magCentroid();
		//SearchRadiusFn radiusCentroid_1 = mag_comp_1.get_fcn_radiusCentroid();

		//SearchMagFn magSample_2 = mag_comp_2.get_fcn_magSample();
		SearchRadiusFn radiusSample_2 = mag_comp_2.get_fcn_radiusSample();
		SearchMagFn magCentroid_2 = mag_comp_2.get_fcn_magCentroid();
		//SearchRadiusFn radiusCentroid_2 = mag_comp_2.get_fcn_radiusCentroid();

		// Check agreement on skip-centroid

		if (!( magCentroid_1.isSkipCentroid() == magCentroid_2.isSkipCentroid() )) {
			return false;
		}

		// Check agreement on search Radius

		if (!( radiusSample_1.getDefaultGUIIsEqual (radiusSample_2) )) {
			return false;
		}

		return true;
	}




	// Test if a set of analyst forecast parameters select a Standard region.
	// Parmaeters:
	//  anparam = Forecast parameters, as they would appear in AnalystOptions. Can be null.
	//  loc = Mainshock location.

	public static boolean is_anparam_standard_region (ForecastParameters anparam, Location loc) {

		// Null parameters select a Standard region.

		if (anparam == null) {
			return true;
		}

		// If aftershock search parameters are available ...

		if (anparam.aftershock_search_avail) {

			// Must not have a custom aftershock search region

			if (!( anparam.aftershock_search_region == null )) {
				return false;
			}

			// Depths must be defaulted or equal to the default

			if (!( anparam.min_depth >= ForecastParameters.SEARCH_PARAM_TEST || Math.abs (anparam.min_depth - ComcatOAFAccessor.DEFAULT_MIN_DEPTH) <= 1.0e-3 )) {
				return false;
			}
			if (!( anparam.max_depth >= ForecastParameters.SEARCH_PARAM_TEST || Math.abs (anparam.max_depth - ComcatOAFAccessor.DEFAULT_MAX_DEPTH) <= 1.0e-3 )) {
				return false;
			}
		}

		// If magnitude of completeness parameters are available ...

		if (anparam.mag_comp_avail) {

			// Get magnitude of completeness parameters for mainshock location

			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			OAFTectonicRegime regime = fetch.getRegion (loc);
			MagCompPage_Parameters main_mag_comp_params = fetch.get(regime);

			// Regions must be the same

			if (!( is_mag_comp_same_region (anparam.mag_comp_params, main_mag_comp_params) )) {
				return false;
			}
		}

		// Found same region

		return true;
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
		public double x_minDepthParam;		// Minimum depth; appears for all except Standard
		public double x_maxDepthParam;		// Maximum depth; appears for all except Standard
		public double x_wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
		public double x_minRadiusParam;		// Minimum radius; appears for WC regions
		public double x_maxRadiusParam;		// Maximum radius; appears for WC regions
		public double x_centerLatParam;		// Circle center latitude; appears for custom circle
		public double x_centerLonParam;		// Circle center longitude; appears for custom circle

		// Region modification

		public abstract void modify_regionTypeParam (RegionType x);	// Region type

		public abstract void modify_radiusParam (double x);			// Search radius; appears when Region Type is Circular
		public abstract void modify_minLatParam (double x);			// Minimum latitude; appears when Region Type is Rectangular
		public abstract void modify_maxLatParam (double x);			// Maximum latitude; appears when Region Type is Rectangular
		public abstract void modify_minLonParam (double x);			// Minimum longitude; appears when Region Type is Rectangular
		public abstract void modify_maxLonParam (double x);			// Maximum longitude; appears when Region Type is Rectangular
		public abstract void modify_minDepthParam (double x);		// Minimum depth; appears for all except Standard
		public abstract void modify_maxDepthParam (double x);		// Maximum depth; appears for all except Standard
		public abstract void modify_wcMultiplierParam (double x);	// Wells and Coppersmith multiplier; appears for WC regions
		public abstract void modify_minRadiusParam (double x);		// Minimum radius; appears for WC regions
		public abstract void modify_maxRadiusParam (double x);		// Maximum radius; appears for WC regions
		public abstract void modify_centerLatParam (double x);		// Circle center latitude; appears for custom circle
		public abstract void modify_centerLonParam (double x);		// Circle center longitude; appears for custom circle

		// Get the implementation class.

		public abstract XferRegionImpl xfer_get_impl ();


		// Return true if this is a Standard region.

		public final boolean is_standard_region () {
			return (x_regionTypeParam == RegionType.STANDARD);
		}


		// Load from a region specification.

		public abstract void load_from_region_spec (RegionSpec rspec);


		// Save to a region specification.

		public abstract void save_to_region_spec (RegionSpec rspec);
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


		// Region modification

		private boolean dirty_regionTypeParam;		// Region type

		private boolean dirty_radiusParam;			// Search radius; appears when Region Type is Circular
		private boolean dirty_minLatParam;			// Minimum latitude; appears when Region Type is Rectangular
		private boolean dirty_maxLatParam;			// Maximum latitude; appears when Region Type is Rectangular
		private boolean dirty_minLonParam;			// Minimum longitude; appears when Region Type is Rectangular
		private boolean dirty_maxLonParam;			// Maximum longitude; appears when Region Type is Rectangular
		private boolean dirty_minDepthParam;		// Minimum depth; appears for all except Standard
		private boolean dirty_maxDepthParam;		// Maximum depth; appears for all except Standard
		private boolean dirty_wcMultiplierParam;	// Wells and Coppersmith multiplier; appears for WC regions
		private boolean dirty_minRadiusParam;		// Minimum radius; appears for WC regions
		private boolean dirty_maxRadiusParam;		// Maximum radius; appears for WC regions
		private boolean dirty_centerLatParam;		// Circle center latitude; appears for custom circle
		private boolean dirty_centerLonParam;		// Circle center longitude; appears for custom circle

		@Override
		public void modify_regionTypeParam (RegionType x) {	// Region type
			x_regionTypeParam = x;
			dirty_regionTypeParam = true;
		}

		@Override
		public void modify_radiusParam (double x) {			// Search radius; appears when Region Type is Circular
			x_radiusParam = x;
			dirty_radiusParam = true;
		}

		@Override
		public void modify_minLatParam (double x) {			// Minimum latitude; appears when Region Type is Rectangular
			x_minLatParam = x;
			dirty_minLatParam = true;
		}

		@Override
		public void modify_maxLatParam (double x) {			// Maximum latitude; appears when Region Type is Rectangular
			x_maxLatParam = x;
			dirty_maxLatParam = true;
		}

		@Override
		public void modify_minLonParam (double x) {			// Minimum longitude; appears when Region Type is Rectangular
			x_minLonParam = x;
			dirty_minLonParam = true;
		}

		@Override
		public void modify_maxLonParam (double x) {			// Maximum longitude; appears when Region Type is Rectangular
			x_maxLonParam = x;
			dirty_maxLonParam = true;
		}

		@Override
		public void modify_minDepthParam (double x) {		// Minimum depth; appears for all except Standard
			x_minDepthParam = x;
			dirty_minDepthParam = true;
		}

		@Override
		public void modify_maxDepthParam (double x) {		// Maximum depth; appears for all except Standard
			x_maxDepthParam = x;
			dirty_maxDepthParam = true;
		}

		@Override
		public void modify_wcMultiplierParam (double x) {	// Wells and Coppersmith multiplier; appears for WC regions
			x_wcMultiplierParam = x;
			dirty_wcMultiplierParam = true;
		}

		@Override
		public void modify_minRadiusParam (double x) {		// Minimum radius; appears for WC regions
			x_minRadiusParam = x;
			dirty_minRadiusParam = true;
		}

		@Override
		public void modify_maxRadiusParam (double x) {		// Maximum radius; appears for WC regions
			x_maxRadiusParam = x;
			dirty_maxRadiusParam = true;
		}

		@Override
		public void modify_centerLatParam (double x) {		// Circle center latitude; appears for custom circle
			x_centerLatParam = x;
			dirty_centerLatParam = true;
		}

		@Override
		public void modify_centerLonParam (double x) {		// Circle center longitude; appears for custom circle
			x_centerLonParam = x;
			dirty_centerLonParam = true;
		}


		// Set to a Standard region.

		public final void set_region_standard () {

			modify_regionTypeParam (RegionType.STANDARD);

			return;
		}


		// Set to a Centroid WC Circle region.

		public final void set_region_centroid_wc_circle (
			double wc_mult,
			double min_radius,
			double max_radius,
			double min_depth,
			double max_depth
		) {
			modify_regionTypeParam (RegionType.CENTROID_WC_CIRCLE);
			modify_minRadiusParam (min_radius);
			modify_maxRadiusParam (max_radius);
			modify_minDepthParam (min_depth);
			modify_maxDepthParam (max_depth);

			return;
		}


		// Set to a Centroid Circle region.

		public final void set_region_centroid_circle (
			double radius,
			double min_depth,
			double max_depth
		) {
			modify_regionTypeParam (RegionType.CENTROID_CIRCLE);
			modify_radiusParam (radius);
			modify_minDepthParam (min_depth);
			modify_maxDepthParam (max_depth);

			return;
		}


		// Set to an Epicenter WC Circle region.

		public final void set_region_epicenter_wc_circle (
			double wc_mult,
			double min_radius,
			double max_radius,
			double min_depth,
			double max_depth
		) {
			modify_regionTypeParam (RegionType.EPICENTER_WC_CIRCLE);
			modify_minRadiusParam (min_radius);
			modify_maxRadiusParam (max_radius);
			modify_minDepthParam (min_depth);
			modify_maxDepthParam (max_depth);

			return;
		}


		// Set to an Epicenter Circle region.

		public final void set_region_epicenter_circle (
			double radius,
			double min_depth,
			double max_depth
		) {
			modify_regionTypeParam (RegionType.EPICENTER_CIRCLE);
			modify_radiusParam (radius);
			modify_minDepthParam (min_depth);
			modify_maxDepthParam (max_depth);

			return;
		}


		// Set to a Custom Circle region.

		public final void set_region_custom_circle (
			double radius,
			double center_lat,
			double center_lon,
			double min_depth,
			double max_depth
		) {
			modify_regionTypeParam (RegionType.CUSTOM_CIRCLE);
			modify_radiusParam (radius);
			modify_centerLatParam (center_lat);
			modify_centerLonParam (center_lon);
			modify_minDepthParam (min_depth);
			modify_maxDepthParam (max_depth);

			return;
		}


		// Set to a Custom Rectangle region.

		public final void set_region_custom_rectangle (
			double min_lat,
			double max_lat,
			double min_lon,
			double max_lon,
			double min_depth,
			double max_depth
		) {
			modify_regionTypeParam (RegionType.CUSTOM_RECTANGLE);
			modify_minLatParam (min_lat);
			modify_maxLatParam (max_lat);
			modify_minLonParam (min_lon);
			modify_maxLonParam (max_lon);
			modify_minDepthParam (min_depth);
			modify_maxDepthParam (max_depth);

			return;
		}


		// Load from a region specification.

		@Override
		public void load_from_region_spec (RegionSpec rspec) {
		
			switch (rspec.x_regionTypeParam) {

			case STANDARD:
				set_region_standard ();
				break;

			case CENTROID_WC_CIRCLE:
				set_region_centroid_wc_circle (
					rspec.x_wcMultiplierParam,
					rspec.x_minRadiusParam,
					rspec.x_maxRadiusParam,
					rspec.x_minDepthParam,
					rspec.x_maxDepthParam
				);
				break;

			case CENTROID_CIRCLE:
				set_region_centroid_circle (
					rspec.x_radiusParam,
					rspec.x_minDepthParam,
					rspec.x_maxDepthParam
				);
				break;

			case EPICENTER_WC_CIRCLE:
				set_region_epicenter_wc_circle (
					rspec.x_wcMultiplierParam,
					rspec.x_minRadiusParam,
					rspec.x_maxRadiusParam,
					rspec.x_minDepthParam,
					rspec.x_maxDepthParam
				);
				break;

			case EPICENTER_CIRCLE:
				set_region_epicenter_circle (
					rspec.x_radiusParam,
					rspec.x_minDepthParam,
					rspec.x_maxDepthParam
				);
				break;

			case CUSTOM_CIRCLE:
				set_region_custom_circle (
					rspec.x_radiusParam,
					rspec.x_centerLatParam,
					rspec.x_centerLonParam,
					rspec.x_minDepthParam,
					rspec.x_maxDepthParam
				);
				break;

			case CUSTOM_RECTANGLE:
				set_region_custom_rectangle (
					rspec.x_minLatParam,
					rspec.x_maxLatParam,
					rspec.x_minLonParam,
					rspec.x_maxLonParam,
					rspec.x_minDepthParam,
					rspec.x_maxDepthParam
				);
				break;

			default:
				throw new IllegalStateException("Unknown region type: " + rspec.x_regionTypeParam);
			}

			return;
		}


		// Save to a region specification.

		@Override
		public void save_to_region_spec (RegionSpec rspec) {
		
			switch (x_regionTypeParam) {

			case STANDARD:
				rspec.set_region_standard ();
				break;

			case CENTROID_WC_CIRCLE:
				rspec.set_region_centroid_wc_circle (
					x_wcMultiplierParam,
					x_minRadiusParam,
					x_maxRadiusParam,
					x_minDepthParam,
					x_maxDepthParam
				);
				break;

			case CENTROID_CIRCLE:
				rspec.set_region_centroid_circle (
					x_radiusParam,
					x_minDepthParam,
					x_maxDepthParam
				);
				break;

			case EPICENTER_WC_CIRCLE:
				rspec.set_region_epicenter_wc_circle (
					x_wcMultiplierParam,
					x_minRadiusParam,
					x_maxRadiusParam,
					x_minDepthParam,
					x_maxDepthParam
				);
				break;

			case EPICENTER_CIRCLE:
				rspec.set_region_epicenter_circle (
					x_radiusParam,
					x_minDepthParam,
					x_maxDepthParam
				);
				break;

			case CUSTOM_CIRCLE:
				rspec.set_region_custom_circle (
					x_radiusParam,
					x_centerLatParam,
					x_centerLonParam,
					x_minDepthParam,
					x_maxDepthParam
				);
				break;

			case CUSTOM_RECTANGLE:
				rspec.set_region_custom_rectangle (
					x_minLatParam,
					x_maxLatParam,
					x_minLonParam,
					x_maxLonParam,
					x_minDepthParam,
					x_maxDepthParam
				);
				break;

			default:
				throw new IllegalStateException("Unknown region type: " + x_regionTypeParam);
			}

			return;
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			dirty_regionTypeParam = false;
			dirty_radiusParam = false;
			dirty_minLatParam = false;
			dirty_maxLatParam = false;
			dirty_minLonParam = false;
			dirty_maxLonParam = false;
			dirty_minDepthParam = false;
			dirty_maxDepthParam = false;
			dirty_wcMultiplierParam = false;
			dirty_minRadiusParam = false;
			dirty_maxRadiusParam = false;
			dirty_centerLatParam = false;
			dirty_centerLonParam = false;
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

			if (dirty_radiusParam) {		// Search radius; appears when Region Type is Circular
				dirty_radiusParam = false;
				updateParam(radiusParam, x_radiusParam);
			}

			if (dirty_minLatParam) {		// Minimum latitude; appears when Region Type is Rectangular
				dirty_minLatParam = false;
				updateParam(minLatParam, x_minLatParam);
			}

			if (dirty_maxLatParam) {		// Maximum latitude; appears when Region Type is Rectangular
				dirty_maxLatParam = false;
				updateParam(maxLatParam, x_maxLatParam);
			}

			if (dirty_minLonParam) {		// Minimum longitude; appears when Region Type is Rectangular
				dirty_minLonParam = false;
				updateParam(minLonParam, x_minLonParam);
			}

			if (dirty_maxLonParam) {		// Maximum longitude; appears when Region Type is Rectangular
				dirty_maxLonParam = false;
				updateParam(maxLonParam, x_maxLonParam);
			}

			if (dirty_minDepthParam) {		// Minimum depth; appears for all except Standard
				dirty_minDepthParam = false;
				updateParam(minDepthParam, x_minDepthParam);
			}

			if (dirty_maxDepthParam) {		// Maximum depth; appears for all except Standard
				dirty_maxDepthParam = false;
				updateParam(maxDepthParam, x_maxDepthParam);
			}

			if (dirty_wcMultiplierParam) {	// Wells and Coppersmith multiplier; appears for WC regions
				dirty_wcMultiplierParam = false;
				updateParam(wcMultiplierParam, x_wcMultiplierParam);
			}

			if (dirty_minRadiusParam) {		// Minimum radius; appears for WC regions
				dirty_minRadiusParam = false;
				updateParam(minRadiusParam, x_minRadiusParam);
			}

			if (dirty_maxRadiusParam) {		// Maximum radius; appears for WC regions
				dirty_maxRadiusParam = false;
				updateParam(maxRadiusParam, x_maxRadiusParam);
			}

			if (dirty_centerLatParam) {		// Circle center latitude; appears for custom circle
				dirty_centerLatParam = false;
				updateParam(centerLatParam, x_centerLatParam);
			}

			if (dirty_centerLonParam) {		// Circle center longitude; appears for custom circle
				dirty_centerLonParam = false;
				updateParam(centerLonParam, x_centerLonParam);
			}

			if (dirty_regionTypeParam) {	// Region type
				dirty_regionTypeParam = false;
				updateParam(regionTypeParam, x_regionTypeParam);

				updateRegionParamList(validParam(regionTypeParam));
				adjust_enable();
			}

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
