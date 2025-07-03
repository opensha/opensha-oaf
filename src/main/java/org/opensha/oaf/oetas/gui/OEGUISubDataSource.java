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
import java.util.ArrayList;
import java.util.Comparator;

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
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.gui.GUIConsoleWindow;
import org.opensha.oaf.util.gui.GUICalcStep;
import org.opensha.oaf.util.gui.GUICalcRunnable;
import org.opensha.oaf.util.gui.GUICalcProgressBar;
import org.opensha.oaf.util.gui.GUIEDTException;
import org.opensha.oaf.util.gui.GUIEDTRunnable;
import org.opensha.oaf.util.gui.GUIEventAlias;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.gui.GUIParameterListParameter;
import org.opensha.oaf.util.gui.GUIDropdownParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.ForecastData;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;
import org.opensha.oaf.comcat.ComcatProductOaf;

import org.json.simple.JSONObject;


// Operational ETAS GUI - Sub-controller for data source parameters.
// Michael Barall 08/19/2021
//
// This is a modeless dialog for entering data source parameters,
// which define the earthquake catalog source.


public class OEGUISubDataSource extends OEGUIListener {


	//----- Internal constants -----


	// Parameter groups.

	private static final int PARMGRP_DATA_SOURCE_PARAM = 301;	// Data source parameters
	private static final int PARMGRP_DATA_SOURCE_TYPE = 302;	// Drop-down to select data source type
	private static final int PARMGRP_DATA_SOURCE_EDIT = 303;	// Button to open the data source edit dialog

	private static final int PARMGRP_CATALOG_FILE_BROWSE = 304;	// Button to browse for catalog filename

	private static final int PARMGRP_FC_PROD_POPULATE = 305;	// Button to populate list of forecasts
	private static final int PARMGRP_FC_INC_SUPERSEDED = 306;	// Checkbox to include superseded forecasts

	private static final int PARMGRP_EVENT_ID_PARAM = 307;		// Event ID parameter

	private static final int PARMGRP_DATA_ENABLE_PARAM = 308;	// Data source parameters, that also change enable state

	private static final int PARMGRP_DOWNLOAD_FILE_BROWSE = 309;	// Button to browse for download filename


	//----- Sub-controllers -----


	// Floating panel for region parameters.

	private OEGUISubRegion sub_ctl_region;




	//----- Controls for the data source dialog -----




	// Event ID; edit box containing a string.

	private StringParameter eventIDParam;

	private StringParameter init_eventIDParam () throws GUIEDTException {
		eventIDParam = new StringParameter("USGS Event ID");
		eventIDParam.setValue("");
		eventIDParam.setInfo("Get IDs from https://earthquake.usgs.gov/earthquakes/");
		register_param (eventIDParam, "eventIDParam", PARMGRP_EVENT_ID_PARAM);
		return eventIDParam;
	}




	// Data source type; dropdown containing an enumeration to select type of data source.

	private EnumParameter<DataSource> dataSourceTypeParam;

	private EnumParameter<DataSource> init_dataSourceTypeParam () throws GUIEDTException {
		dataSourceTypeParam = new EnumParameter<DataSource>(
				"Data Source Type", EnumSet.allOf(DataSource.class), DataSource.COMCAT, null);
		dataSourceTypeParam.setInfo("Source of earthquake catalog data");
		register_param (dataSourceTypeParam, "dataSourceTypeParam", PARMGRP_DATA_SOURCE_TYPE);
		return dataSourceTypeParam;
	}




	// Data source dialog: parameters within the dialog to select data source options.

	// Data start time, in days since the mainshock; edit box containing a number.
	// We provide a separate one for each data source type.

	private DoubleParameter dataStartTimeParam_comcat;
	private DoubleParameter dataStartTimeParam_catalog;
	private DoubleParameter dataStartTimeParam_forecast;
	private DoubleParameter dataStartTimeParam_dlf;
	private DoubleParameter dataStartTimeParam_rj_sim;
	private DoubleParameter dataStartTimeParam_etas_sim;

	private void init_dataStartTimeParam () throws GUIEDTException {

		dataStartTimeParam_comcat = new DoubleParameter("Data Start Time", -3650d, 36500d, Double.valueOf(0d));
		dataStartTimeParam_comcat.setUnits("Days");
		dataStartTimeParam_comcat.setInfo("Data start relative to mainshock origin time");
		register_param (dataStartTimeParam_comcat, "dataStartTimeParam_comcat", PARMGRP_DATA_SOURCE_PARAM);

		dataStartTimeParam_catalog = new DoubleParameter("Data Start Time", -3650d, 36500d, Double.valueOf(0d));
		dataStartTimeParam_catalog.setUnits("Days");
		dataStartTimeParam_catalog.setInfo("Data start relative to mainshock origin time");
		register_param (dataStartTimeParam_catalog, "dataStartTimeParam_catalog", PARMGRP_DATA_SOURCE_PARAM);

		dataStartTimeParam_forecast = new DoubleParameter("Data Start Time", -3650d, 36500d, Double.valueOf(0d));
		dataStartTimeParam_forecast.setUnits("Days");
		dataStartTimeParam_forecast.setInfo("Data start relative to mainshock origin time");
		register_param (dataStartTimeParam_forecast, "dataStartTimeParam_forecast", PARMGRP_DATA_SOURCE_PARAM);

		dataStartTimeParam_dlf = new DoubleParameter("Data Start Time", -3650d, 36500d, Double.valueOf(0d));
		dataStartTimeParam_dlf.setUnits("Days");
		dataStartTimeParam_dlf.setInfo("Data start relative to mainshock origin time");
		register_param (dataStartTimeParam_dlf, "dataStartTimeParam_dlf", PARMGRP_DATA_SOURCE_PARAM);

		dataStartTimeParam_rj_sim = new DoubleParameter("Data Start Time", -3650d, 36500d, Double.valueOf(0d));
		dataStartTimeParam_rj_sim.setUnits("Days");
		dataStartTimeParam_rj_sim.setInfo("Data start relative to mainshock origin time");
		register_param (dataStartTimeParam_rj_sim, "dataStartTimeParam_rj_sim", PARMGRP_DATA_SOURCE_PARAM);

		dataStartTimeParam_etas_sim = new DoubleParameter("Data Start Time", -3650d, 36500d, Double.valueOf(0d));
		dataStartTimeParam_etas_sim.setUnits("Days");
		dataStartTimeParam_etas_sim.setInfo("Data start relative to mainshock origin time");
		register_param (dataStartTimeParam_etas_sim, "dataStartTimeParam_etas_sim", PARMGRP_DATA_SOURCE_PARAM);

		return;
	}

	private DoubleParameter get_dataStartTimeParam (DataSource type) {
		DoubleParameter result;
		switch (type) {
		case COMCAT:             result = dataStartTimeParam_comcat;   break;
		case CATALOG_FILE:       result = dataStartTimeParam_catalog;  break;
		case PUBLISHED_FORECAST: result = dataStartTimeParam_forecast; break;
		case DOWNLOAD_FILE:      result = dataStartTimeParam_dlf;      break;
		case RJ_SIMULATION:      result = dataStartTimeParam_rj_sim;   break;
		case ETAS_SIMULATION:    result = dataStartTimeParam_etas_sim; break;

//		case PUBLISHED_FORECAST:
		case MAINSHOCK_ONLY:
//		case DOWNLOAD_FILE:
			throw new IllegalStateException("get_useStartEndTimeParam: Invalid data source type: " + type);

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		return result;
	}

	// Data end time, in days since the mainshock; edit box containing a number.
	// We provide a separate one for each data source type.

	private DoubleParameter dataEndTimeParam_comcat;
	private DoubleParameter dataEndTimeParam_catalog;
	private DoubleParameter dataEndTimeParam_forecast;
	private DoubleParameter dataEndTimeParam_dlf;
	private DoubleParameter dataEndTimeParam_rj_sim;
	private DoubleParameter dataEndTimeParam_etas_sim;

	private void init_dataEndTimeParam () throws GUIEDTException {

		dataEndTimeParam_comcat = new DoubleParameter("Data End Time", 0d, 36500d, Double.valueOf(7d));
		dataEndTimeParam_comcat.setUnits("Days");
		dataEndTimeParam_comcat.setInfo("Data end relative to mainshock origin time");
		register_param (dataEndTimeParam_comcat, "dataEndTimeParam_comcat", PARMGRP_DATA_SOURCE_PARAM);

		dataEndTimeParam_catalog = new DoubleParameter("Data End Time", 0d, 36500d, Double.valueOf(0d));
		dataEndTimeParam_catalog.setUnits("Days");
		dataEndTimeParam_catalog.setInfo("Data end relative to mainshock origin time");
		register_param (dataEndTimeParam_catalog, "dataEndTimeParam_catalog", PARMGRP_DATA_SOURCE_PARAM);

		dataEndTimeParam_forecast = new DoubleParameter("Data End Time", 0d, 36500d, Double.valueOf(0d));
		dataEndTimeParam_forecast.setUnits("Days");
		dataEndTimeParam_forecast.setInfo("Data end relative to mainshock origin time");
		register_param (dataEndTimeParam_forecast, "dataEndTimeParam_forecast", PARMGRP_DATA_SOURCE_PARAM);

		dataEndTimeParam_dlf = new DoubleParameter("Data End Time", 0d, 36500d, Double.valueOf(0d));
		dataEndTimeParam_dlf.setUnits("Days");
		dataEndTimeParam_dlf.setInfo("Data end relative to mainshock origin time");
		register_param (dataEndTimeParam_dlf, "dataEndTimeParam_dlf", PARMGRP_DATA_SOURCE_PARAM);

		dataEndTimeParam_rj_sim = new DoubleParameter("Data End Time", 0d, 36500d, Double.valueOf(7d));
		dataEndTimeParam_rj_sim.setUnits("Days");
		dataEndTimeParam_rj_sim.setInfo("Data end relative to mainshock origin time");
		register_param (dataEndTimeParam_rj_sim, "dataEndTimeParam_rj_sim", PARMGRP_DATA_SOURCE_PARAM);

		dataEndTimeParam_etas_sim = new DoubleParameter("Data End Time", 0d, 36500d, Double.valueOf(7d));
		dataEndTimeParam_etas_sim.setUnits("Days");
		dataEndTimeParam_etas_sim.setInfo("Data end relative to mainshock origin time");
		register_param (dataEndTimeParam_etas_sim, "dataEndTimeParam_etas_sim", PARMGRP_DATA_SOURCE_PARAM);

		return;
	}

	private DoubleParameter get_dataEndTimeParam (DataSource type) {
		DoubleParameter result;
		switch (type) {
		case COMCAT:             result = dataEndTimeParam_comcat;   break;
		case CATALOG_FILE:       result = dataEndTimeParam_catalog;  break;
		case PUBLISHED_FORECAST: result = dataEndTimeParam_forecast; break;
		case DOWNLOAD_FILE:      result = dataEndTimeParam_dlf;      break;
		case RJ_SIMULATION:      result = dataEndTimeParam_rj_sim;   break;
		case ETAS_SIMULATION:    result = dataEndTimeParam_etas_sim; break;

//		case PUBLISHED_FORECAST:
		case MAINSHOCK_ONLY:
//		case DOWNLOAD_FILE:
			throw new IllegalStateException("get_useStartEndTimeParam: Invalid data source type: " + type);

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		return result;
	}

	// Minimum magnitude to use when fetching from Comcat; edit box containing a number.

	private DoubleParameter minMagFetchParam;

	private DoubleParameter init_minMagFetchParam () throws GUIEDTException {
		minMagFetchParam = new DoubleParameter("Minimum magnitude", -8d, 9d, Double.valueOf(1d));
		minMagFetchParam.setInfo("Minimum magnitude when fetching data from Comcat");
		register_param (minMagFetchParam, "minMagFetchParam", PARMGRP_DATA_SOURCE_PARAM);
		return minMagFetchParam;
	}

	// Option to use minimum magnitude when fetching from Comcat; default true; check box.

	private BooleanParameter useMinMagFetchParam;

	private BooleanParameter init_useMinMagFetchParam () throws GUIEDTException {
		useMinMagFetchParam = new BooleanParameter("Use minimum magnitude", true);
		register_param (useMinMagFetchParam, "useMinMagFetchParam", PARMGRP_DATA_ENABLE_PARAM);
		return useMinMagFetchParam;
	}

	// Option to use start and end time when fetching from Comcat or catalog; default true; check box.

	private BooleanParameter useStartEndTimeParam_comcat;
	private BooleanParameter useStartEndTimeParam_catalog;

	private void init_useStartEndTimeParam () throws GUIEDTException {

		useStartEndTimeParam_comcat = new BooleanParameter("Use start and end times", false);
		register_param (useStartEndTimeParam_comcat, "useStartEndTimeParam_comcat", PARMGRP_DATA_ENABLE_PARAM);

		useStartEndTimeParam_catalog = new BooleanParameter("Use start and end times", false);
		register_param (useStartEndTimeParam_catalog, "useStartEndTimeParam_catalog", PARMGRP_DATA_ENABLE_PARAM);

		return;
	}

	private BooleanParameter get_useStartEndTimeParam (DataSource type) {
		BooleanParameter result;
		switch (type) {
		case COMCAT:             result = useStartEndTimeParam_comcat;   break;
		case CATALOG_FILE:       result = useStartEndTimeParam_catalog;  break;

		case PUBLISHED_FORECAST:
		case MAINSHOCK_ONLY:
		case DOWNLOAD_FILE:
		case RJ_SIMULATION:
		case ETAS_SIMULATION:
			throw new IllegalStateException("get_useStartEndTimeParam: Invalid data source type: " + type);

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		return result;
	}

	// Option to use analyst options when fetching from Comcat, catalog, or mainshock; default true (Comcat, mainshock), false (catalog); check box.

	private BooleanParameter useAnalystOptionsParam_comcat;
	private BooleanParameter useAnalystOptionsParam_catalog;
	private BooleanParameter useAnalystOptionsParam_mainonly;

	private void init_useAnalystOptionsParam () throws GUIEDTException {

		useAnalystOptionsParam_comcat = new BooleanParameter("Use analyst options", true);
		register_param (useAnalystOptionsParam_comcat, "useAnalystOptionsParam_comcat", PARMGRP_DATA_SOURCE_PARAM);

		useAnalystOptionsParam_catalog = new BooleanParameter("Use analyst options", false);
		register_param (useAnalystOptionsParam_catalog, "useAnalystOptionsParam_catalog", PARMGRP_DATA_SOURCE_PARAM);

		useAnalystOptionsParam_mainonly = new BooleanParameter("Use analyst options", true);
		register_param (useAnalystOptionsParam_mainonly, "useAnalystOptionsParam_mainonly", PARMGRP_DATA_SOURCE_PARAM);

		return;
	}

	private BooleanParameter get_useAnalystOptionsParam (DataSource type) {
		BooleanParameter result;
		switch (type) {
		case COMCAT:             result = useAnalystOptionsParam_comcat;    break;
		case CATALOG_FILE:       result = useAnalystOptionsParam_catalog;   break;
		case MAINSHOCK_ONLY:     result = useAnalystOptionsParam_mainonly;  break;

		case PUBLISHED_FORECAST:
		case DOWNLOAD_FILE:
		case RJ_SIMULATION:
		case ETAS_SIMULATION:
			throw new IllegalStateException("get_useAnalystOptionsParam: Invalid data source type: " + type);

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		return result;
	}

	// Option to use outer region when fetching from Comcat; default true; check box.

	private BooleanParameter useOuterRegionParam;

	private BooleanParameter init_useOuterRegionParam () throws GUIEDTException {
		useOuterRegionParam = new BooleanParameter("Show outer region", true);
		register_param (useOuterRegionParam, "useOuterRegionParam", PARMGRP_DATA_SOURCE_PARAM);
		return useOuterRegionParam;
	}

	// Catalog or download file; edit box containing a string.

	private StringParameter catalogFileParam;
	private StringParameter downloadFileParam;

	private void init_catalogFileParam () throws GUIEDTException {

		catalogFileParam = new StringParameter("Catalog Filename");
		catalogFileParam.setValue("");
		catalogFileParam.setInfo("Filename for loading a saved catalog");
		register_param (catalogFileParam, "catalogFileParam", PARMGRP_DATA_SOURCE_PARAM);

		downloadFileParam = new StringParameter("Download Filename");
		downloadFileParam.setValue("");
		downloadFileParam.setInfo("Filename for loading a saved download file");
		register_param (downloadFileParam, "downloadFileParam", PARMGRP_DATA_SOURCE_PARAM);

		return;
	}

	// Browse catalog or download file button.

	private ButtonParameter browseCatalogFileButton;
	private ButtonParameter browseDownloadFileButton;

	private void init_browseCatalogFileButton () throws GUIEDTException {

		browseCatalogFileButton = new ButtonParameter("Browse Catalog File", "Browse...");
		browseCatalogFileButton.setInfo("Browse for a catalog file");
		register_param (browseCatalogFileButton, "browseCatalogFileButton", PARMGRP_CATALOG_FILE_BROWSE);

		browseDownloadFileButton = new ButtonParameter("Browse Download File", "Browse...");
		browseDownloadFileButton.setInfo("Browse for a saved download file");
		register_param (browseDownloadFileButton, "browseDownloadFileButton", PARMGRP_DOWNLOAD_FILE_BROWSE);

		return;
	}

	// Option to use Comcat to get mainshock info when loading a catalog or download file; default false; check box.

	private BooleanParameter useComcatForMainshockParam_catalog;
	private BooleanParameter useComcatForMainshockParam_dlf;

	private void init_useComcatForMainshockParam () throws GUIEDTException {

		useComcatForMainshockParam_catalog = new BooleanParameter("Call Comcat for mainshock", false);
		register_param (useComcatForMainshockParam_catalog, "useComcatForMainshockParam_catalog", PARMGRP_DATA_ENABLE_PARAM);

		useComcatForMainshockParam_dlf = new BooleanParameter("Call Comcat for mainshock", false);
		register_param (useComcatForMainshockParam_dlf, "useComcatForMainshockParam_dlf", PARMGRP_DATA_ENABLE_PARAM);

		return;
	}

	private BooleanParameter get_useComcatForMainshockParam (DataSource type) {
		BooleanParameter result;
		switch (type) {
		case CATALOG_FILE:       result = useComcatForMainshockParam_catalog;  break;
		case DOWNLOAD_FILE:      result = useComcatForMainshockParam_dlf;      break;

		case COMCAT:
		case PUBLISHED_FORECAST:
		case MAINSHOCK_ONLY:
		case RJ_SIMULATION:
		case ETAS_SIMULATION:
			throw new IllegalStateException("get_useStartEndTimeParam: Invalid data source type: " + type);

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		return result;
	}

	// Option to set aftershock region when loading a catalog; default false; check box.

	private BooleanParameter useRegionForLoadParam;

	private void init_useRegionForLoadParam () throws GUIEDTException {
		useRegionForLoadParam = new BooleanParameter("Set aftershock region", false);
		register_param (useRegionForLoadParam, "useRegionForLoadParam", PARMGRP_DATA_ENABLE_PARAM);
		return;
	}

	// Aftershock region when loading a catalog; three edit boxes containing radius, latitude, longitude.

	private DoubleParameter aftershockRegionParam_radius;
	private DoubleParameter aftershockRegionParam_latitude;
	private DoubleParameter aftershockRegionParam_longitude;

	private void init_aftershockRegionParam () throws GUIEDTException {

		aftershockRegionParam_radius = new DoubleParameter("Region Radius", 1d, 5000d, Double.valueOf(10d));
		aftershockRegionParam_radius.setUnits("Km");
		aftershockRegionParam_radius.setInfo("Radius of the aftershock region");
		register_param (aftershockRegionParam_radius, "aftershockRegionParam_radius", PARMGRP_DATA_SOURCE_PARAM);

		aftershockRegionParam_latitude = new DoubleParameter("Region Latitude", -90d, 90d, Double.valueOf(0d));
		aftershockRegionParam_latitude.setUnits("Deg");
		aftershockRegionParam_latitude.setInfo("Latitude of the center of the aftershock region");
		register_param (aftershockRegionParam_latitude, "aftershockRegionParam_latitude", PARMGRP_DATA_SOURCE_PARAM);

		aftershockRegionParam_longitude = new DoubleParameter("Region Longitude", -180d, 180d, Double.valueOf(0d));
		aftershockRegionParam_longitude.setUnits("Deg");
		aftershockRegionParam_longitude.setInfo("Longitude of the center of the aftershock region");
		register_param (aftershockRegionParam_longitude, "aftershockRegionParam_longitude", PARMGRP_DATA_SOURCE_PARAM);

		return;
	}

	// Populate forecast list button.

	private ButtonParameter populateForecastListButton;

	private ButtonParameter init_populateForecastListButton () throws GUIEDTException {
		populateForecastListButton = new ButtonParameter("Populate Forecast List", "Populate...");
		populateForecastListButton.setInfo("Fetch list of available forecasts");
		register_param (populateForecastListButton, "populateForecastListButton", PARMGRP_FC_PROD_POPULATE);
		return populateForecastListButton;
	}

	// Option to include superseded products; default true; check box.

	private BooleanParameter includeSupersededParam;

	private BooleanParameter init_includeSupersededParam () throws GUIEDTException {
		includeSupersededParam = new BooleanParameter("Include superseded", true);
		register_param (includeSupersededParam, "includeSupersededParam", PARMGRP_FC_INC_SUPERSEDED);
		return includeSupersededParam;
	}




	// Forecast list dropdown -- Holds a list of AvailableForecast objects.

	// Items that appear in the dropdown list.

	public static class AvailableForecast {
		public String label;
		public long time;
		public ComcatProductOaf oaf_product;

		@Override
		public String toString () {
			return label;
		}

		public AvailableForecast (ComcatProductOaf oaf_product) {
			this.oaf_product = oaf_product;
			this.time = oaf_product.updateTime;
			this.label = SimpleUtils.time_to_string_no_z (oaf_product.updateTime);
		}
	}

	// Comparator to sort list in decreasing order by time, latest first.

	public static class AvailableForecastComparator implements Comparator<AvailableForecast> {
		@Override
		public int compare (AvailableForecast prod1, AvailableForecast prod2) {
			return Long.compare (prod2.time, prod1.time);
		}
	}

	// The list that appears in the dropdown.

	private ArrayList<AvailableForecast> forecastList;

	// The event ID that was queried to produce the list, or null if none.

	private String forecastListEventID;

	// The dropdown paramter.

	private GUIDropdownParameter forecastListDropdown;

	private GUIDropdownParameter init_forecastListDropdown () throws GUIEDTException {
		forecastList = new ArrayList<AvailableForecast>();
		forecastListEventID = null;
		forecastListDropdown = new GUIDropdownParameter(
				"Available Forecasts", forecastList, GUIDropdownParameter.DROPDOWN_INDEX_EXTRA, "Use current forecast");
		forecastListDropdown.setInfo("List of forecasts for the selected earthquake");
		register_param (forecastListDropdown, "forecastListDropdown", PARMGRP_DATA_SOURCE_PARAM);
		return forecastListDropdown;
	}

	private void refresh_forecastListDropdown () throws GUIEDTException {
		if (forecastList.isEmpty()) {
			forecastListDropdown.modify_dropdown (forecastList, GUIDropdownParameter.DROPDOWN_INDEX_EXTRA, (forecastListEventID == null) ? "Use current forecast" : "No forecasts found");
		} else {
			forecastListDropdown.modify_dropdown (forecastList, 0, null);
		}
		forecastListDropdown.getEditor().refreshParamEditor();
		return;
	}

	private void clear_forecastListDropdown () throws GUIEDTException {
		if (!( forecastList.isEmpty() && forecastListEventID == null )) {
			forecastList = new ArrayList<AvailableForecast>();
			forecastListEventID = null;
			refresh_forecastListDropdown();
		}
		return;
	}

	// Return true if the forecast list dropdown is populated for the event in the event ID textbox.

	private boolean is_dropdown_list_current () {
		return (forecastListEventID != null) && definedParam(eventIDParam) && validParam(eventIDParam).equals(forecastListEventID);
	}




	// Initialize all dialog parameters

	private void init_dataSourceDialogParam () throws GUIEDTException {
		init_dataStartTimeParam();
		init_dataEndTimeParam();
		init_minMagFetchParam();
		init_useMinMagFetchParam();
		init_useStartEndTimeParam();
		init_useAnalystOptionsParam();
		init_useOuterRegionParam();
		init_catalogFileParam();
		init_browseCatalogFileButton();
		init_useComcatForMainshockParam();
		init_useRegionForLoadParam();
		init_aftershockRegionParam();
		init_populateForecastListButton();
		init_includeSupersededParam();
		init_forecastListDropdown();

		return;
	}




	// Data source edit: button to activate the dialog.

	private ParameterList dataSourceList;			// List of parameters in the dialog
	private GUIParameterListParameter dataSourceEditParam;
	

	private void updateDataSourceParamList (DataSource type) throws GUIEDTException {
		dataSourceList.clear();

		boolean f_button_row = false;
		
		switch (type) {

		case COMCAT:
			dataSourceEditParam.setListTitleText ("Comcat");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(9, f_button_row));
			dataSourceList.addParameter(get_useStartEndTimeParam (type));
			dataSourceList.addParameter(get_dataStartTimeParam (type));
			dataSourceList.addParameter(get_dataEndTimeParam (type));
			dataSourceList.addParameter(useMinMagFetchParam);
			dataSourceList.addParameter(minMagFetchParam);
			dataSourceList.addParameter(get_useAnalystOptionsParam (type));
			dataSourceList.addParameter(sub_ctl_region.get_regionTypeParam());
			dataSourceList.addParameter(sub_ctl_region.get_regionEditParam());
			dataSourceList.addParameter(useOuterRegionParam);
			break;

		case CATALOG_FILE:
			dataSourceEditParam.setListTitleText ("Catalog File");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(11, f_button_row));
			dataSourceList.addParameter(catalogFileParam);
			dataSourceList.addParameter(browseCatalogFileButton);
			dataSourceList.addParameter(get_useComcatForMainshockParam (type));
			dataSourceList.addParameter(get_useAnalystOptionsParam (type));
			dataSourceList.addParameter(get_useStartEndTimeParam (type));
			dataSourceList.addParameter(get_dataStartTimeParam (type));
			dataSourceList.addParameter(get_dataEndTimeParam (type));
			dataSourceList.addParameter(useRegionForLoadParam);
			dataSourceList.addParameter(aftershockRegionParam_radius);
			dataSourceList.addParameter(aftershockRegionParam_latitude);
			dataSourceList.addParameter(aftershockRegionParam_longitude);
			break;

		case PUBLISHED_FORECAST:
			dataSourceEditParam.setListTitleText ("Published Forecast");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(5, f_button_row));
			dataSourceList.addParameter(populateForecastListButton);
			dataSourceList.addParameter(includeSupersededParam);
			dataSourceList.addParameter(forecastListDropdown);
			dataSourceList.addParameter(get_dataStartTimeParam (type));
			dataSourceList.addParameter(get_dataEndTimeParam (type));
			break;

		case MAINSHOCK_ONLY:
			dataSourceEditParam.setListTitleText ("Mainshock Only");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(1, f_button_row));
			dataSourceList.addParameter(get_useAnalystOptionsParam (type));
			break;

		case DOWNLOAD_FILE:
			dataSourceEditParam.setListTitleText ("Download File");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(5, f_button_row));
			dataSourceList.addParameter(downloadFileParam);
			dataSourceList.addParameter(browseDownloadFileButton);
			dataSourceList.addParameter(get_useComcatForMainshockParam (type));
			dataSourceList.addParameter(get_dataStartTimeParam (type));
			dataSourceList.addParameter(get_dataEndTimeParam (type));
			break;

		case RJ_SIMULATION:
			dataSourceEditParam.setListTitleText ("RJ Simulation");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(2, f_button_row));
			dataSourceList.addParameter(get_dataStartTimeParam (type));
			dataSourceList.addParameter(get_dataEndTimeParam (type));
			break;

		case ETAS_SIMULATION:
			dataSourceEditParam.setListTitleText ("ETAS Simulation");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(2, f_button_row));
			dataSourceList.addParameter(get_dataStartTimeParam (type));
			dataSourceList.addParameter(get_dataEndTimeParam (type));
			break;

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}
		
		dataSourceEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_dataSourceEditParam () throws GUIEDTException {
		dataSourceList = new ParameterList();
		dataSourceEditParam = new GUIParameterListParameter("Data Source", dataSourceList, "Edit Data Source...",
							"Edit Data Source", "Data Source Parameters", null, null, false, gui_top.get_trace_events());
		dataSourceEditParam.setInfo("Select catalog data source options");
		register_param (dataSourceEditParam, "dataSourceEditParam", PARMGRP_DATA_SOURCE_EDIT);

		updateDataSourceParamList(dataSourceTypeParam.getValue());

		return dataSourceEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;


	// Adjust the enable/disable state of our controls.
	// Note: Controls within the dialog do not need to be enabled/disabled
	// because the dialog is closed when the activation button is disabled,
	// and closing this dialog also closes the region dialog.
	//
	// Type control is enabled if sub-controller is enabled.
	// Edit control is enabled if sub-controller is enabled, currently for any type.

	private void adjust_enable () throws GUIEDTException {
		boolean f_edit = false;
		if (f_sub_enable) {
			f_edit = true;
		}
		enableParam(dataSourceTypeParam, f_sub_enable);
		enableParam(dataSourceEditParam, f_edit);
		sub_ctl_region.sub_region_enable (f_sub_enable);

		DataSource type = validParam(dataSourceTypeParam);
		switch (type) {

		case COMCAT:
			enableParam(get_dataStartTimeParam (type), validParam(get_useStartEndTimeParam (type)));
			enableParam(get_dataEndTimeParam (type), validParam(get_useStartEndTimeParam (type)));
			enableParam(minMagFetchParam, validParam(useMinMagFetchParam));
			break;
			
		case CATALOG_FILE:
			enableParam(get_dataStartTimeParam (type), validParam(get_useStartEndTimeParam (type)));
			enableParam(get_dataEndTimeParam (type), validParam(get_useStartEndTimeParam (type)));
			enableParam(get_useAnalystOptionsParam (type), validParam(get_useComcatForMainshockParam (type)));
			enableParam(aftershockRegionParam_radius, validParam(useRegionForLoadParam));
			enableParam(aftershockRegionParam_latitude, validParam(useRegionForLoadParam));
			enableParam(aftershockRegionParam_longitude, validParam(useRegionForLoadParam));
			break;

		case PUBLISHED_FORECAST:
			enableParam(get_dataStartTimeParam (type), false);
			enableParam(get_dataEndTimeParam (type), false);
			break;

		case MAINSHOCK_ONLY:
			break;

		case DOWNLOAD_FILE:
			enableParam(get_dataStartTimeParam (type), false);
			enableParam(get_dataEndTimeParam (type), false);
			break;

		case RJ_SIMULATION:
			enableParam(get_dataStartTimeParam (type), true);
			enableParam(get_dataEndTimeParam (type), true);
			break;

		case ETAS_SIMULATION:
			enableParam(get_dataStartTimeParam (type), true);
			enableParam(get_dataEndTimeParam (type), true);
			break;

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}

		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferDataSourceView {

		public DataSource x_dataSourceTypeParam;	// Data source type

		// Event ID. [COMCAT, CATALOG_FILE, PUBLISHED_FORECAST, MAINSHOCK_ONLY, DOWNLOAD_FILE]  (can be modified for any type)
		// For CATALOG_FILE and DOWNLOAD_FILE only, can be null if the edit field is empty.

		public String x_eventIDParam;				// parameter value, checked for validity
		public abstract void modify_eventIDParam (String x);

		// Data start time, in days since the mainshock. [COMCAT, CATALOG_FILE, PUBLISHED_FORECAST, DOWNLOAD_FILE]
		// For PUBLISHED_FORECAST and DOWNLOAD_FILE, used to return the time range to the user.

		public double x_dataStartTimeParam;			// parameter value, checked for validity
		public abstract void modify_dataStartTimeParam (double x);

		// Data end time, in days since the mainshock. [COMCAT, CATALOG_FILE, PUBLISHED_FORECAST, DOWNLOAD_FILE]
		// For PUBLISHED_FORECAST and DOWNLOAD_FILE, used to return the time range to the user.

		public double x_dataEndTimeParam;			// parameter value, checked for validity
		public abstract void modify_dataEndTimeParam (double x);

		// Minimum magnitude to use when fetching from Comcat. [COMCAT]

		public double x_minMagFetchParam;			// parameter value, checked for validity

		// Option to use minimum magnitude when fetching from Comcat. [COMCAT]

		public boolean x_useMinMagFetchParam;		// parameter value, checked for validity

		// Option to use start and end time when fetching from Comcat. [COMCAT, CATALOG_FILE]

		public boolean x_useStartEndTimeParam;		// parameter value, checked for validity

		// Option to use analyst options when fetching from Comcat. [COMCAT, CATALOG_FILE, MAINSHOCK_ONLY]

		public boolean x_useAnalystOptionsParam;	// parameter value, checked for validity

		// Option to use outer region when fetching from Comcat. [COMCAT]

		public boolean x_useOuterRegionParam;	// parameter value, checked for validity

		// Search region. [COMCAT]

		public OEGUISubRegion.XferRegionView x_region;	// Region parameters

		// Catalog or download file. [CATALOG_FILE, DOWNLOAD_FILE]

		public String x_catalogFileParam;			// parameter value, checked for validity

		// Option to use Comcat to get mainshock info when loading a catalog or download file. [CATALOG_FILE, DOWNLOAD_FILE]

		public boolean x_useComcatForMainshockParam;	// parameter value, checked for validity

		// Option to set aftershock region when loading a catalog file. [CATALOG_FILE]

		public boolean x_useRegionForLoadParam;	// parameter value, checked for validity

		// Aftershock region when loading a catalog file, or null if none. [CATALOG_FILE]

		public SphRegion x_aftershockRegionParam;	// parameter value, checked for validity

		// OAF product, for retrieving published forecast.  [PUBLISHED_FORECAST]
		// If null, use the current product for the given event ID.

		public ComcatProductOaf x_oaf_product;

		// Get the implementation class.

		public abstract XferDataSourceImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferDataSourceImpl extends XferDataSourceView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferDataSourceImpl xfer_get_impl () {
			return this;
		}

		// Allocate sub-controller transfer during construction, ensure clean state.

		public XferDataSourceImpl () {
			x_region = sub_ctl_region.make_region_xfer();
			internal_clean();
		}

		// Event ID.

		private boolean dirty_eventIDParam;	// true if needs to be written back

		@Override
		public void modify_eventIDParam (String x) {
			x_eventIDParam = x;
			dirty_eventIDParam = true;
		}

		// Data start time, in days since the mainshock.

		private boolean dirty_dataStartTimeParam;	// true if needs to be written back

		@Override
		public void modify_dataStartTimeParam (double x) {
			x_dataStartTimeParam = x;
			dirty_dataStartTimeParam = true;
		}

		// Data end time, in days since the mainshock.

		private boolean dirty_dataEndTimeParam;		// true if needs to be written back

		@Override
		public void modify_dataEndTimeParam (double x) {
			x_dataEndTimeParam = x;
			dirty_dataEndTimeParam = true;
		}

		// Flag indicating that forecast list needs to be cleared.

		private boolean f_clear_forecastListDropdown;


		// Clear all dirty-value flags.

		private void internal_clean () {
			dirty_eventIDParam = false;
			dirty_dataStartTimeParam = false;
			dirty_dataEndTimeParam = false;
			f_clear_forecastListDropdown = false;
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			x_region.xfer_get_impl().xfer_clean();
			return;
		}


		// Load values.

		@Override
		public XferDataSourceImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Data source

			x_dataSourceTypeParam = validParam(dataSourceTypeParam);
		
			switch (x_dataSourceTypeParam) {

			case COMCAT:
				x_eventIDParam = validParam(eventIDParam);
				x_dataStartTimeParam = validParam(get_dataStartTimeParam (x_dataSourceTypeParam));
				x_dataEndTimeParam = validParam(get_dataEndTimeParam (x_dataSourceTypeParam));
				x_minMagFetchParam = validParam(minMagFetchParam);
				x_useMinMagFetchParam = validParam(useMinMagFetchParam);
				x_useStartEndTimeParam = validParam(get_useStartEndTimeParam (x_dataSourceTypeParam));
				x_useAnalystOptionsParam = validParam(get_useAnalystOptionsParam (x_dataSourceTypeParam));
				x_useOuterRegionParam = validParam(useOuterRegionParam);
				x_region.xfer_get_impl().xfer_load();
				break;

			case CATALOG_FILE:
				if (definedParam(eventIDParam)) {
					x_eventIDParam = validParam(eventIDParam);
				} else {
					x_eventIDParam = null;
				}
				x_dataStartTimeParam = validParam(get_dataStartTimeParam (x_dataSourceTypeParam));
				x_dataEndTimeParam = validParam(get_dataEndTimeParam (x_dataSourceTypeParam));
				x_useStartEndTimeParam = validParam(get_useStartEndTimeParam (x_dataSourceTypeParam));
				x_useAnalystOptionsParam = validParam(get_useAnalystOptionsParam (x_dataSourceTypeParam));
				x_catalogFileParam = validParam(catalogFileParam);
				x_useComcatForMainshockParam = validParam(get_useComcatForMainshockParam (x_dataSourceTypeParam));
				x_useRegionForLoadParam = validParam(useRegionForLoadParam);
				if (validParam(useRegionForLoadParam)) {
					double radius = validParam(aftershockRegionParam_radius);
					double lat = validParam(aftershockRegionParam_latitude);
					double lon = validParam(aftershockRegionParam_longitude);
					SphLatLon center = new SphLatLon (lat, lon);
					x_aftershockRegionParam = SphRegion.makeCircle (center, radius);
				} else {
					x_aftershockRegionParam = null;
				}
				break;

			case PUBLISHED_FORECAST:
				x_eventIDParam = validParam(eventIDParam);
				x_dataStartTimeParam = validParam(get_dataStartTimeParam (x_dataSourceTypeParam));
				x_dataEndTimeParam = validParam(get_dataEndTimeParam (x_dataSourceTypeParam));
				x_oaf_product = null;
				if (is_dropdown_list_current()) {
					int fcix = validParam(forecastListDropdown);
					if (fcix < 0 || fcix >= forecastList.size()) {
						throw new IllegalStateException("Invalid forecast list dropdown selection: " + fcix);
					}
					x_oaf_product = forecastList.get(fcix).oaf_product;
				}
				break;

			case MAINSHOCK_ONLY:
				x_eventIDParam = validParam(eventIDParam);
				x_useAnalystOptionsParam = validParam(get_useAnalystOptionsParam (x_dataSourceTypeParam));
				break;

			case DOWNLOAD_FILE:
				if (definedParam(eventIDParam)) {
					x_eventIDParam = validParam(eventIDParam);
				} else {
					x_eventIDParam = null;
				}
				x_dataStartTimeParam = validParam(get_dataStartTimeParam (x_dataSourceTypeParam));
				x_dataEndTimeParam = validParam(get_dataEndTimeParam (x_dataSourceTypeParam));
				x_catalogFileParam = validParam(downloadFileParam);
				x_useComcatForMainshockParam = validParam(get_useComcatForMainshockParam (x_dataSourceTypeParam));
				break;

			case RJ_SIMULATION:
				x_dataStartTimeParam = validParam(get_dataStartTimeParam (x_dataSourceTypeParam));
				x_dataEndTimeParam = validParam(get_dataEndTimeParam (x_dataSourceTypeParam));
				break;

			case ETAS_SIMULATION:
				x_dataStartTimeParam = validParam(get_dataStartTimeParam (x_dataSourceTypeParam));
				x_dataEndTimeParam = validParam(get_dataEndTimeParam (x_dataSourceTypeParam));
				break;

			default:
				throw new IllegalStateException("Unknown data source type: " + x_dataSourceTypeParam);
			}

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// Event ID

			if (dirty_eventIDParam) {
				dirty_eventIDParam = false;
				updateParam(eventIDParam, x_eventIDParam);
			}

			// Data start and end time

			if (dirty_dataStartTimeParam) {
				dirty_dataStartTimeParam = false;
				updateParam(get_dataStartTimeParam (x_dataSourceTypeParam), x_dataStartTimeParam);
			}

			if (dirty_dataEndTimeParam) {
				dirty_dataEndTimeParam = false;
				updateParam(get_dataEndTimeParam (x_dataSourceTypeParam), x_dataEndTimeParam);
			}

			// Forecast list

			if (f_clear_forecastListDropdown) {
				f_clear_forecastListDropdown = false;
				clear_forecastListDropdown();
			}

			// Region

			x_region.xfer_get_impl().xfer_store();
			return;
		}

	}




	//----- Parameter transfer, to populate the list of forecasts -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferForecastPopulateView {

		public DataSource x_dataSourceTypeParam;	// Data source type

		// Event ID.  (can be modified for any type)

		public String x_eventIDParam;				// parameter value, checked for validity
		public abstract void modify_eventIDParam (String x);

		// Option to include superseded products.

		public boolean x_includeSuperseded;			// parameter value, checked for validity

		// Get the implementation class.

		public abstract XferForecastPopulateImpl xfer_get_impl ();
	}




	// Implementation class to transfer parameters.

	public class XferForecastPopulateImpl extends XferForecastPopulateView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferForecastPopulateImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferForecastPopulateImpl () {
			internal_clean();
		}

		// Event ID.

		private boolean dirty_eventIDParam;	// true if needs to be written back

		@Override
		public void modify_eventIDParam (String x) {
			x_eventIDParam = x;
			dirty_eventIDParam = true;
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			dirty_eventIDParam = false;
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			return;
		}


		// Load values.

		@Override
		public XferForecastPopulateImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Event ID.
				
			x_eventIDParam = validParam(eventIDParam);

			// Option to include superseded products

			x_includeSuperseded = validParam(includeSupersededParam);

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// Event ID

			if (dirty_eventIDParam) {
				dirty_eventIDParam = false;
				updateParam(eventIDParam, x_eventIDParam);
			}

			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubDataSource (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Make sub-controller
		
		sub_ctl_region = new OEGUISubRegion (this);

		// Set flag to enable controls

		f_sub_enable = true;

		// Create and initialize controls

		init_eventIDParam();
		init_dataSourceTypeParam();
		init_dataSourceDialogParam();
		init_dataSourceEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the event ID edit box.
	// The intent is to use this only to insert the control into the client.

	public StringParameter get_eventIDParam () throws GUIEDTException {
		return eventIDParam;
	}


	// Get the data source type dropdown.
	// The intent is to use this only to insert the control into the client.

	public EnumParameter<DataSource> get_dataSourceTypeParam () throws GUIEDTException {
		return dataSourceTypeParam;
	}


	// Get the data source edit button.
	// The intent is to use this only to insert the control into the client.

	public GUIParameterListParameter get_dataSourceEditParam () throws GUIEDTException {
		return dataSourceEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_data_source_enable (boolean f_enabled) throws GUIEDTException {
		f_sub_enable = f_enabled;
		adjust_enable();
		return;
	}


	// Make a data source transfer object.

	public XferDataSourceImpl make_data_source_xfer () {
		return new XferDataSourceImpl();
	}


	// Private function, used to report that the data source parameters have changed.

	private void report_data_source_change () throws GUIEDTException {
		gui_controller.notify_data_source_change();
		return;
	}


	// Return true if data fetch is apparently possible.

	public boolean can_fetch_data () throws GUIEDTException {
		boolean result = false;

		DataSource type = validParam(dataSourceTypeParam);
		
		switch (type) {

		case COMCAT:
			if (definedParam(eventIDParam)) {
				result = true;
			}
			break;

		case CATALOG_FILE:
			if (definedParam(catalogFileParam)) {
				result = true;
			}
			break;

		case PUBLISHED_FORECAST:
			if (definedParam(eventIDParam) && !( forecastList.isEmpty() && forecastListEventID != null )) {
				int selected = validParam(forecastListDropdown);
				if (selected > 0) {
					result = true;
				}
			}
			break;

		case MAINSHOCK_ONLY:
			if (definedParam(eventIDParam)) {
				result = true;
			}
			break;

		case DOWNLOAD_FILE:
			if (definedParam(downloadFileParam)) {
				result = true;
			}
			break;

		case RJ_SIMULATION:
			break;

		case ETAS_SIMULATION:
			break;

		default:
			throw new IllegalStateException("Unknown data source type: " + type);
		}

		return result;
	}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {


		// Data source parameters.
		// - Report to top-level controller.

		case PARMGRP_DATA_SOURCE_PARAM: {
			if (!( f_sub_enable )) {
				return;
			}
			report_data_source_change();
		}
		break;


		// Data source parameters, that also change enable state of our controls.
		// - Enable or disable our controls.
		// - Report to top-level controller.

		case PARMGRP_DATA_ENABLE_PARAM: {
			if (!( f_sub_enable )) {
				return;
			}
			adjust_enable();
			report_data_source_change();
		}
		break;


		// Event ID parameter.
		// - Clear the forecast list dropdown, unless it contains a list for the current event ID.
		// - Report to top-level controller.

		case PARMGRP_EVENT_ID_PARAM: {
			if (!( f_sub_enable )) {
				return;
			}
			if (!( is_dropdown_list_current() )) {
				clear_forecastListDropdown();
			}
			report_data_source_change();
		}
		break;


		// Data source type.
		// - Update the edit data source dialog, to include the controls for the selected data source type.
		// - Enable or disable the edit button.
		// - Report to top-level controller.

		case PARMGRP_DATA_SOURCE_TYPE: {
			gui_controller.notify_data_source_type(validParam(dataSourceTypeParam));
			if (!( f_sub_enable )) {
				return;
			}
			updateDataSourceParamList(validParam(dataSourceTypeParam));
			adjust_enable();
			report_data_source_change();
		}
		break;


		// Data source edit button.
		// - Do nothing.

		case PARMGRP_DATA_SOURCE_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;


		// Catalog file browse button.
		// - Open the file chooser.
		// - If the user makes a selection, update the filename edit box.

		case PARMGRP_CATALOG_FILE_BROWSE: {
			if (!( f_sub_enable )) {
				return;
			}
			File file = gui_top.showSelectFileDialog (browseCatalogFileButton, "Select");
			if (file != null) {
				String filename = file.getAbsolutePath();
				updateParam(catalogFileParam, filename);
				report_data_source_change();
			}
		}
		break;


		// Download file browse button.
		// - Open the file chooser.
		// - If the user makes a selection, update the filename edit box.

		case PARMGRP_DOWNLOAD_FILE_BROWSE: {
			if (!( f_sub_enable )) {
				return;
			}
			File file = gui_top.showSelectFileDialog (browseDownloadFileButton, "Select");
			if (file != null) {
				String filename = file.getAbsolutePath();
				updateParam(downloadFileParam, filename);
				report_data_source_change();
			}
		}
		break;


		// Include superseded product checkbox.
		// - Clear the dropdown list.
		// - Report to top-level controller.

		case PARMGRP_FC_INC_SUPERSEDED: {
			if (!( f_sub_enable )) {
				return;
			}
			clear_forecastListDropdown();
			report_data_source_change();
		}
		break;


		// Populate forecast list button.
		// - Clear the dropdown list.
		// - Report to top-level controller.
		// - Fetch available forecasts from Comcat, and populate the dropdown.

		case PARMGRP_FC_PROD_POPULATE: {
			if (!( f_sub_enable )) {
				return;
			}
			clear_forecastListDropdown();
			report_data_source_change();

			final GUICalcProgressBar progress = new GUICalcProgressBar(gui_top.get_top_window(), "", "", false);
			final XferForecastPopulateImpl xfer_fc_populate_impl = new XferForecastPopulateImpl();

			// Load the forecast populate parameters

			if (!( gui_top.call_xfer_load (xfer_fc_populate_impl, "Incorrect parameters for fecthing forecasts") )) {
				return;
			}

			// Number of events received, or -1 if error

			final int[] received = new int[1];
			received[0] = -1;

			// The new list we will build

			final ArrayList<AvailableForecast> newForecastList = new ArrayList<AvailableForecast>();

			// Call Comcat to get list of forecasts

			GUICalcStep fetchStep_1 = new GUICalcStep(
				"Fetching Forecasts",
				"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
				new Runnable() {
						
				@Override
				public void run() {

					// See if the event ID is an alias, and change it if so

					String xlatid = GUIEventAlias.query_alias_dict (xfer_fc_populate_impl.x_eventIDParam);
					if (xlatid != null) {
						System.out.println("Translating Event ID: " + xfer_fc_populate_impl.x_eventIDParam + " -> " + xlatid);
						xfer_fc_populate_impl.modify_eventIDParam(xlatid);
					}

					// Get the event id

					String event_id = xfer_fc_populate_impl.x_eventIDParam;

					// Get the superseded flag

					boolean f_superseded = xfer_fc_populate_impl.x_includeSuperseded;

					// Get the event information

					ComcatOAFAccessor accessor = new ComcatOAFAccessor ();
					ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true, f_superseded);

					if (rup == null) {
						throw new RuntimeException ("Earthquake not found: id = " + event_id);
					}

					// Get the list of products

					boolean delete_ok = false;
					List<ComcatProductOaf> product_list = ComcatProductOaf.make_list_from_gj (accessor.get_last_geojson(), delete_ok);

					// Save products that have a ForecastData file

					for (ComcatProductOaf prod : product_list) {
						if (prod.contains_file (ForecastData.FORECAST_DATA_FILENAME)) {
							newForecastList.add (new AvailableForecast (prod));
						}
					}

					// Sort the list into decreasing order by time (latest first)

					newForecastList.sort (new AvailableForecastComparator());

					// Set number of events received, this indicates success

					received[0] = newForecastList.size();
				}
			});

			// Populate dropdown list

			GUICalcStep fetchStep_2 = new GUICalcStep("Populating List", "...", new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {

					// Store back changed parameters

					xfer_fc_populate_impl.xfer_store();

					// If success, update the list

					if (received[0] >= 0) {
						forecastList = newForecastList;
						forecastListEventID = xfer_fc_populate_impl.x_eventIDParam;
						refresh_forecastListDropdown();
					}
				}
			});

			// Display number received

			GUIEDTRunnable postFetchStep = new GUIEDTRunnable() {
						
				@Override
				public void run_in_edt() throws GUIEDTException {
					String title = "Available Forecasts";
					String message;
					int message_type;

					if (received[0] < 0) {
						message = "Error occurred while attempting to retrieve forecasts";
						message_type = JOptionPane.ERROR_MESSAGE;
					}
					else if (received[0] == 0) {
						message = "No forecasts with a download file were found";
						message_type = JOptionPane.WARNING_MESSAGE;
					}
					else if (received[0] == 1) {
						message = "Found 1 forecast with a download file";
						message_type = JOptionPane.INFORMATION_MESSAGE;
					}
					else {
						message = "Found " + received[0] + " forecasts with a download file";
						message_type = JOptionPane.INFORMATION_MESSAGE;
					}
					JOptionPane.showMessageDialog(gui_top.get_top_window(), message, title, message_type);
				}
			};

			// Run in threads

			GUICalcRunnable.run_steps (progress, postFetchStep, fetchStep_1, fetchStep_2);
		}
		break;


		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubDataSource: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
