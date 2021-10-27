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
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

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
		register_param (eventIDParam, "eventIDParam", PARMGRP_DATA_SOURCE_PARAM);
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

	private DoubleParameter dataStartTimeParam;

	private DoubleParameter init_dataStartTimeParam () throws GUIEDTException {
		dataStartTimeParam = new DoubleParameter("Data Start Time", 0d, 36500d, new Double(0d));
		dataStartTimeParam.setUnits("Days");
		dataStartTimeParam.setInfo("Data start relative to main shock origin time");
		register_param (dataStartTimeParam, "dataStartTimeParam", PARMGRP_DATA_SOURCE_PARAM);
		return dataStartTimeParam;
	}

	// Data end time, in days since the mainshock; edit box containing a number.

	private DoubleParameter dataEndTimeParam;

	private DoubleParameter init_dataEndTimeParam () throws GUIEDTException {
		dataEndTimeParam = new DoubleParameter("Data End Time", 0d, 36500d, new Double(7d));
		dataEndTimeParam.setUnits("Days");
		dataEndTimeParam.setInfo("Data end relative to main shock origin time");
		register_param (dataEndTimeParam, "dataEndTimeParam", PARMGRP_DATA_SOURCE_PARAM);
		return dataEndTimeParam;
	}

	// Catalog file; edit box containing a string.

	private StringParameter catalogFileParam;

	private StringParameter init_catalogFileParam () throws GUIEDTException {
		catalogFileParam = new StringParameter("Catalog Filename");
		catalogFileParam.setValue("");
		catalogFileParam.setInfo("Filename for loading a saved catalog");
		register_param (catalogFileParam, "catalogFileParam", PARMGRP_DATA_SOURCE_PARAM);
		return catalogFileParam;
	}

	// Browse catalog file button.

	private ButtonParameter browseCatalogFileButton;

	private ButtonParameter init_browseCatalogFileButton () throws GUIEDTException {
		browseCatalogFileButton = new ButtonParameter("Browse Catalog File", "Browse...");
		browseCatalogFileButton.setInfo("Browse for a catalog file");
		register_param (browseCatalogFileButton, "browseCatalogFileButton", PARMGRP_CATALOG_FILE_BROWSE);
		return browseCatalogFileButton;
	}

	// Populate forecast list button.

	private ButtonParameter populateForecastListButton;

	private ButtonParameter init_populateForecastListButton () throws GUIEDTException {
		populateForecastListButton = new ButtonParameter("Populate Forecast List", "Populate...");
		populateForecastListButton.setInfo("Fetch list of available forecasts");
		register_param (populateForecastListButton, "populateForecastListButton", PARMGRP_FC_PROD_POPULATE);
		return populateForecastListButton;
	}

	// Forecast list dropdown -- Holds a list of AvailableForecast objects.

	public static class AvailableForecast {
		public String label;

		@Override
		public String toString () {
			return label;
		}

		public AvailableForecast (String label) {
			this.label = label;
		}
	}

	private ArrayList<AvailableForecast> forecastList;

	private GUIDropdownParameter forecastListDropdown;

	private GUIDropdownParameter init_forecastListDropdown () throws GUIEDTException {
		forecastList = new ArrayList<AvailableForecast>();
		forecastListDropdown = new GUIDropdownParameter(
				"Available Forecasts", forecastList, GUIDropdownParameter.DROPDOWN_INDEX_EXTRA, "--- Empty ---");
		forecastListDropdown.setInfo("List of forecasts for the selected earthquake");
		register_param (forecastListDropdown, "forecastListDropdown", PARMGRP_DATA_SOURCE_PARAM);
		return forecastListDropdown;
	}

	private void refresh_forecastListDropdown () throws GUIEDTException {
		if (forecastList.isEmpty()) {
			forecastListDropdown.modify_dropdown (forecastList, GUIDropdownParameter.DROPDOWN_INDEX_EXTRA, "--- Empty ---");
		} else {
			forecastListDropdown.modify_dropdown (forecastList, 0, null);
		}
		forecastListDropdown.getEditor().refreshParamEditor();
		return;
	}

	// Initialize all dialog parameters

	private void init_dataSourceDialogParam () throws GUIEDTException {
		init_dataStartTimeParam();
		init_dataEndTimeParam();
		init_catalogFileParam();
		init_browseCatalogFileButton();
		init_populateForecastListButton();
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
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(4, f_button_row));
			dataSourceList.addParameter(dataStartTimeParam);
			dataSourceList.addParameter(dataEndTimeParam);
			dataSourceList.addParameter(sub_ctl_region.get_regionTypeParam());
			dataSourceList.addParameter(sub_ctl_region.get_regionEditParam());
			break;

		case CATALOG_FILE:
			dataSourceEditParam.setListTitleText ("Catalog File");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(4, f_button_row));
			dataSourceList.addParameter(catalogFileParam);
			dataSourceList.addParameter(browseCatalogFileButton);
			dataSourceList.addParameter(dataStartTimeParam);
			dataSourceList.addParameter(dataEndTimeParam);
			break;

		case LAST_FORECAST:
			dataSourceEditParam.setListTitleText ("Forecast");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(0, f_button_row));
			dataSourceList.addParameter(populateForecastListButton);
			dataSourceList.addParameter(forecastListDropdown);
			break;

		case RJ_SIMULATION:
			dataSourceEditParam.setListTitleText ("RJ Simulation");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(0, f_button_row));
			break;

		case ETAS_SIMULATION:
			dataSourceEditParam.setListTitleText ("ETAS Simulation");
			dataSourceEditParam.setDialogDimensions (gui_top.get_dialog_dims(0, f_button_row));
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
		return;
	}




	//----- Parameter transfer -----




	// Class to view or modify relevant parameters.
	// This class holds copies of the parameters, and so may be accessed on any thread.
	// Modification functions change the copy, and are not immediately written back to parameters.

	public static abstract class XferDataSourceView {

		public DataSource x_dataSourceTypeParam;	// Data source type

		// Event ID. [COMCAT, LAST_FORECAST]  (can be modified for any type)

		public String x_eventIDParam;				// parameter value, checked for validity
		public abstract void modify_eventIDParam (String x);

		// Data start time, in days since the mainshock. [COMCAT, CATALOG_FILE]

		public double x_dataStartTimeParam;			// parameter value, checked for validity
		public abstract void modify_dataStartTimeParam (double x);

		// Data end time, in days since the mainshock. [COMCAT, CATALOG_FILE]

		public double x_dataEndTimeParam;			// parameter value, checked for validity
		public abstract void modify_dataEndTimeParam (double x);

		// Search region. [COMCAT]

		public OEGUISubRegion.XferRegionView x_region;	// Region parameters

		// Catalog file. [CATALOG_FILE]

		public String x_catalogFileParam;			// parameter value, checked for validity

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


		// Clear all dirty-value flags.

		private void internal_clean () {
			dirty_eventIDParam = false;
			dirty_dataStartTimeParam = false;
			dirty_dataEndTimeParam = false;
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
				x_dataStartTimeParam = validParam(dataStartTimeParam);
				x_dataEndTimeParam = validParam(dataEndTimeParam);
				x_region.xfer_get_impl().xfer_load();
				break;

			case CATALOG_FILE:
				x_dataStartTimeParam = validParam(dataStartTimeParam);
				x_dataEndTimeParam = validParam(dataEndTimeParam);
				x_catalogFileParam = validParam(catalogFileParam);
				break;

			case LAST_FORECAST:
				x_eventIDParam = validParam(eventIDParam);
				break;

			case RJ_SIMULATION:
				break;

			case ETAS_SIMULATION:
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
				updateParam(dataStartTimeParam, x_dataStartTimeParam);
			}

			if (dirty_dataEndTimeParam) {
				dirty_dataEndTimeParam = false;
				updateParam(dataEndTimeParam, x_dataEndTimeParam);
			}

			// Region

			x_region.xfer_get_impl().xfer_store();
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

		case LAST_FORECAST:
			if (definedParam(eventIDParam)) {
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


		// Populate forecast list button.
		// - Fetch available forecasts from Comcat, and populate the dropdown.

		case PARMGRP_FC_PROD_POPULATE: {
			if (!( f_sub_enable )) {
				return;
			}

			// As a test, just put a few items in the list

			forecastList = new ArrayList<AvailableForecast>();
			forecastList.add (new AvailableForecast ("Forecast 1"));
			forecastList.add (new AvailableForecast ("Forecast 2"));
			forecastList.add (new AvailableForecast ("Forecast 3"));
			forecastList.add (new AvailableForecast ("Forecast 4"));
			forecastList.add (new AvailableForecast ("Forecast 5"));
			refresh_forecastListDropdown();
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
