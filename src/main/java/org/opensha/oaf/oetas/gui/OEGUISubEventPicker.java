package org.opensha.oaf.oetas.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;
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
import org.opensha.commons.param.editor.AbstractParameterEditor;
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
import gov.usgs.earthquake.event.JsonEvent;
//import org.opensha.oaf.pdl.OAF_Publisher;
import org.opensha.oaf.pdl.PDLProductBuilderOaf;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLCodeChooserOaf;
import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SphRegionWorld;
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
import org.opensha.oaf.util.gui.GUIParameterListParameterEditor;
import org.opensha.oaf.util.gui.GUIDialogParameter;
import org.opensha.oaf.util.gui.GUISeparatorParameter;
import org.opensha.oaf.util.gui.GUIPredicateStringParameter;
import org.opensha.oaf.util.gui.GUIDropdownParameter;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.ActionConfig;
import org.opensha.oaf.aafs.ActionConfigFile;
import org.opensha.oaf.aafs.GUICmd;
import org.opensha.oaf.aafs.AnalystOptions;
import org.opensha.oaf.aafs.EventSequenceParameters;
import org.opensha.oaf.aafs.PDLSupport;
import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.PickerSphRegion;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.oaf.comcat.ComcatOAFProduct;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatEventWebService;
import org.opensha.commons.data.comcat.ComcatVisitor;

import org.json.simple.JSONObject;


// Operational RJ & ETAS GUI - Sub-controller for event picker.
// Michael Barall
//
// This is a modeless dialog for picking an event from a list of earthquakes with oaf products.


public class OEGUISubEventPicker extends OEGUIListener {


	//----- Internal constants -----


	// Implementation option: Use modal or modeless dialog.

	private static final boolean f_imp_modal = true;


	// Parameter groups.

	private static final int PARMGRP_EVPICK_EDIT = 1301;			// Button to open the event picker dialog
	private static final int PARMGRP_EVPICK_VALUE = 1302;			// Event picker value
	private static final int PARMGRP_EVPICK_POPULATE = 1303;		// Button to populate the list
	private static final int PARMGRP_EVPICK_TRANSFER = 1304;		// Button to transfer event ID
	private static final int PARMGRP_EVPICK_EVENT = 1305;			// Event selection dropdown
	private static final int PARMGRP_EVPICK_MODAL_OPEN = 1307;		// Open modal dialog




	//----- Controls for the Event Picker dialog -----




	// Event picker minimum magnitude option; drop-down list.

	private EnumParameter<EvPickMinMagOption> evPickMinMagParam;

	private EnumParameter<EvPickMinMagOption> init_evPickMinMagParam () throws GUIEDTException {
		evPickMinMagParam = new EnumParameter<EvPickMinMagOption>(
				"Minimum Magnitude", EnumSet.allOf(EvPickMinMagOption.class), EvPickMinMagOption.ANY, null);
		evPickMinMagParam.setInfo ("Select the minimum magnitude to use when searching for events");
		register_param (evPickMinMagParam, "evPickMinMagParam", PARMGRP_EVPICK_VALUE);
		return evPickMinMagParam;
	}




	// Event picker region option; drop-down list.

	private List<PickerSphRegion> evPickRegionList;

	private GUIDropdownParameter evPickRegionParam;

	private GUIDropdownParameter init_evPickRegionParam () throws GUIEDTException {
		evPickRegionList = (new ActionConfig()).get_picker_regions();
		evPickRegionParam = new GUIDropdownParameter(
				"Search Region", evPickRegionList, 0, null);
		evPickRegionParam.setInfo("Select the region to search for events");
		register_param (evPickRegionParam, "evPickRegionParam", PARMGRP_EVPICK_VALUE);
		return evPickRegionParam;
	}




	// Populate list; button.

	private ButtonParameter evPickPopulateButton;

	private ButtonParameter init_evPickPopulateButton () throws GUIEDTException {
		evPickPopulateButton = new ButtonParameter("Populate Event List", "Populate...");
		evPickPopulateButton.setInfo ("Get a list of earthquakes with OAF products from Comcat");
		register_param (evPickPopulateButton, "evPickPopulateButton", PARMGRP_EVPICK_POPULATE);
		return evPickPopulateButton;
	}




	// Transfer event ID; button.

	private ButtonParameter evPickTransferButton = null;

	private ButtonParameter init_evPickTransferButton () throws GUIEDTException {
		evPickTransferButton = new ButtonParameter("Transfer Event ID", "Select");
		evPickTransferButton.setInfo ("Transfer the event ID for the selected earthquake to the main window");
		register_param (evPickTransferButton, "evPickTransferButton", PARMGRP_EVPICK_TRANSFER);
		return evPickTransferButton;
	}

	// Refresh the transfer button text according to the currently selected item in the dropdown.

	private void refresh_evPickTransferButton () throws GUIEDTException {
		if (f_imp_modal) {

			int evix = validParam(evPickListDropdown);
			if (evix < 0 || evix > evPickList.size()) {
				eventPickerEditParam.setOkButtonText ("Select");
				eventPickerEditParam.setOkButtonEnabled (false);
			} else {
				eventPickerEditParam.setOkButtonText ("Select  " + evPickList.get(evix).event_id);
				eventPickerEditParam.setOkButtonEnabled (true);
			}
			//eventPickerEditParam.getEditor().refreshParamEditor();
			eventPickerEditParam.refreshButtons();

		} else {

			int evix = validParam(evPickListDropdown);
			if (evix < 0 || evix > evPickList.size()) {
				evPickTransferButton.setButtonText ("Select");
			} else {
				evPickTransferButton.setButtonText ("Select  " + evPickList.get(evix).event_id);
			}
			evPickTransferButton.getEditor().refreshParamEditor();

		}
		return;
	}




	// Earthquake list dropdown -- Holds a list of AvailableEarthquake objects.

	// Items that appear in the dropdown list.

	private static final TimeZone tz_utc = TimeZone.getTimeZone("UTC");

	private static final SimpleDateFormat time_to_string_pick_fmt = new SimpleDateFormat ("yyyy-MM-dd");
	static {
		time_to_string_pick_fmt.setTimeZone (tz_utc);
	}

	public static class AvailableEarthquake {
		public String label;
		public long time;
		public String event_id;
		public ObsEqkRupture rupture;

		@Override
		public String toString () {
			return label;
		}

		public final void clear () {
			label = null;
			time = 0L;
			event_id = null;
			rupture = null;
			return;
		}

		public AvailableEarthquake (ObsEqkRupture rup) {
			clear();			// indicate not succeeded

			time = rup.getOriginTime();
			event_id = rup.getEventId();
			double mag = rup.getMag();
			Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);
			String description = eimap.get (ComcatOAFAccessor.PARAM_NAME_DESCRIPTION);

			// If success, complete setup

			if (!( event_id == null || event_id.isEmpty() || description == null || description.isEmpty() )) {
				String s1 = description.replaceAll ("[\\x00-\\x1F\\x7F\\xA0]", " ");	// map ASCII control chars and nbsp to space
				//String s2 = s1.replaceAll ("[^\\x00-\\xFF]", "?");			// map non-ASCII, non-Latin-1 chars to question mark
				String s2 = s1;
				String s_time = time_to_string_pick_fmt.format (new Date (time));
				label = s_time + " M " + SimpleUtils.double_to_string ("%.1f", mag) + " - " + s2;
				rupture = rup;
			}
		}

		// Return true if construction succeeded (failure should be rare).

		public final boolean succeeded () {
			return rupture != null;
		}
	}

	// Comparator to sort list in decreasing order by time, latest first.

	public static class AvailableEarthquakeComparator implements Comparator<AvailableEarthquake> {
		@Override
		public int compare (AvailableEarthquake prod1, AvailableEarthquake prod2) {
			return Long.compare (prod2.time, prod1.time);
		}
	}

	// The list that appears in the dropdown.

	private ArrayList<AvailableEarthquake> evPickList;

	// True if list was attempted.

	private boolean evPickListAttempted;

	// The dropdown paramter.

	private GUIDropdownParameter evPickListDropdown;

	private GUIDropdownParameter init_evPickListDropdown () throws GUIEDTException {
		evPickListAttempted = false;
		evPickList = new ArrayList<AvailableEarthquake>();
		evPickListDropdown = new GUIDropdownParameter(
				"Available Earthquakes", evPickList, GUIDropdownParameter.DROPDOWN_INDEX_EXTRA, "Click Populate...");
		evPickListDropdown.setInfo("List of earthquakes with OAF products");
		register_param (evPickListDropdown, "evPickListDropdown", PARMGRP_EVPICK_EVENT);
		return evPickListDropdown;
	}

	private void refresh_evPickListDropdown () throws GUIEDTException {
		if (evPickList.isEmpty()) {
			evPickListDropdown.modify_dropdown (evPickList, GUIDropdownParameter.DROPDOWN_INDEX_EXTRA, evPickListAttempted ? "No earthquakes found" : "Click Populate...");
		} else {
			evPickListDropdown.modify_dropdown (evPickList, 0, null);
		}
		evPickListDropdown.getEditor().refreshParamEditor();
		refresh_evPickTransferButton();
		return;
	}

	private void clear_evPickListDropdown () throws GUIEDTException {
		if (!( evPickList.isEmpty() && !evPickListAttempted )) {
			evPickList = new ArrayList<AvailableEarthquake>();
			evPickListAttempted = false;
			refresh_evPickListDropdown();
		}
		return;
	}

	// Make a pick list, with earthquakes above the given magnitude.
	// Note: This function should be called on a worker thread,
	// and therefore cannot access the parameters.

	private static ArrayList<AvailableEarthquake> make_pick_list (String description, double minMag, long search_duration, boolean f_require_oaf, SphRegion search_region) {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor();

		// Make an empty list

		final ArrayList<AvailableEarthquake> eventList = new ArrayList<AvailableEarthquake>();

		// Visitor for building the list

		ComcatVisitor visitor = new ComcatVisitor() {
			@Override
			public int visit (ObsEqkRupture rup, JsonEvent geojson) {
				AvailableEarthquake eqk = new AvailableEarthquake (rup);
				if (eqk.succeeded()) {
					eventList.add (eqk);
				}
				return 0;
			}
		};

		// Call Comcat

		String rup_event_id = null;
		long endTime = System.currentTimeMillis();
		long startTime = endTime - search_duration;

		double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
		double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;
		SphRegion region;
		if (search_region == null) {
			region = new SphRegionWorld ();
		} else {
			region = search_region;
		}
		boolean wrapLon = false;
		boolean extendedInfo = true;

		String productType = null;
		if (f_require_oaf) {
			productType = server_config.get_pdl_oaf_type();
		}
		boolean includeDeleted = false;

		int visit_result = accessor.visitEventList (visitor, rup_event_id, startTime, endTime,
				minDepth, maxDepth, region, wrapLon, extendedInfo,
				minMag, productType, includeDeleted);

		System.out.println ("Count of events (" + description + ") = " + eventList.size());

		// Sort the list

		eventList.sort (new AvailableEarthquakeComparator());

		return eventList;
	}




	// Open modal dialog; button.

	private ButtonParameter evPickModalOpenButton = null;

	private ButtonParameter init_evPickModalOpenButton () throws GUIEDTException {
		evPickModalOpenButton = new ButtonParameter("Event Picker", "Find Earthquake...");
		evPickModalOpenButton.setInfo ("Search for earthquakes with an OAF product");
		register_param (evPickModalOpenButton, "evPickModalOpenButton", PARMGRP_EVPICK_MODAL_OPEN);
		return evPickModalOpenButton;
	}




	// Event picker dialog: initialize parameters within the dialog.

	private void init_eventPickerDialogParam () throws GUIEDTException {

		init_evPickMinMagParam();

		init_evPickRegionParam();

		init_evPickPopulateButton();

		if (f_imp_modal) {
			init_evPickModalOpenButton();
		} else {
			init_evPickTransferButton();
		}

		init_evPickListDropdown();

		return;
	}




	// Event picker edit: button to activate the dialog.

	private ParameterList eventPickerList;			// List of parameters in the dialog
	private GUIParameterListParameter eventPickerEditParam;
	

	private void updateventPickerParamList () throws GUIEDTException {
		eventPickerList.clear();

		if (f_imp_modal) {

			boolean f_button_row = true;

			eventPickerList.addParameter(evPickListDropdown);

			eventPickerList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));

			eventPickerList.addParameter(evPickMinMagParam);
			eventPickerList.addParameter(evPickRegionParam);
			eventPickerList.addParameter(evPickPopulateButton);

			eventPickerEditParam.setListTitleText ("Search for Earthquake");
			eventPickerEditParam.setDialogDimensions (gui_top.get_dialog_dims_wide(4, f_button_row, 1, 2.2));

		} else {

			boolean f_button_row = false;

			eventPickerList.addParameter(evPickListDropdown);

			eventPickerList.addParameter(new GUISeparatorParameter("Separator1", gui_top.get_separator_color()));

			eventPickerList.addParameter(evPickMinMagParam);
			eventPickerList.addParameter(evPickRegionParam);
			eventPickerList.addParameter(evPickPopulateButton);

			eventPickerList.addParameter(evPickTransferButton);

			eventPickerEditParam.setListTitleText ("Search for Earthquake");
			eventPickerEditParam.setDialogDimensions (gui_top.get_dialog_dims_wide(5, f_button_row, 1, 2.2));

		}
		
		eventPickerEditParam.getEditor().refreshParamEditor();
	}
	

	private GUIParameterListParameter init_eventPickerEditParam () throws GUIEDTException {
		eventPickerList = new ParameterList();
		if (f_imp_modal) {
			eventPickerEditParam = new GUIParameterListParameter("Event Picker", eventPickerList, "Find Earthquake...",
								"Find Earthquake", "Search for Earthquake", "Select", "Cancel", true, gui_top.get_trace_events(),
								gui_top.make_help_modal ("help_mtool_event_picker.html"));
			eventPickerEditParam.setInfo("Search for earthquakes");
			eventPickerEditParam.setOkButtonEnabled (false);
		} else {
			eventPickerEditParam = new GUIParameterListParameter("Event Picker", eventPickerList, "Find Earthquake...",
								"Find Earthquake", "Search for Earthquake", null, null, false, gui_top.get_trace_events(),
								gui_top.make_help ("help_mtool_event_picker_modeless.html"));
			eventPickerEditParam.setInfo("Search for earthquakes");
		}
		register_param (eventPickerEditParam, "eventPickerEditParam", PARMGRP_EVPICK_EDIT);

		updateventPickerParamList();

		return eventPickerEditParam;
	}




	//----- Control enable/disable -----




	// True if this sub-controller is enabled.

	private boolean f_sub_enable;

	// True to enable event picker controls.

	private boolean f_event_picker_enable;


	// Adjust the enable/disable state of our controls.

	private void adjust_enable () throws GUIEDTException {

		enableParam(evPickMinMagParam, f_event_picker_enable);
		enableParam(evPickRegionParam, f_event_picker_enable);
		enableParam(evPickPopulateButton, f_event_picker_enable);

		if (!( f_event_picker_enable )) {
			clear_evPickListDropdown();
		}
		enableParam(evPickListDropdown, f_event_picker_enable);

		if (f_imp_modal) {
			enableParam(evPickModalOpenButton, f_sub_enable);
		} else {
			int evix = validParam(evPickListDropdown);
			enableParam(evPickTransferButton, f_event_picker_enable && (evix >= 0) && (evix < evPickList.size()));

			enableParam(eventPickerEditParam, f_sub_enable);
		}
		return;
	}




	//----- Parameter transfer -----




	// Class to view relevant parameters.
	// This class holds copies of the parameters.

	public static abstract class XferEventPickerView {

		// Event picker minimum magnitude option.

		public EvPickMinMagOption x_evPickMinMagParam;		// parameter value, checked for validity

		// Selected pick region, or null if none selected.

		public PickerSphRegion x_evPickRegion;

		// If non-null, this is a new list for the dropdown.

		public ArrayList<AvailableEarthquake> x_evPickList;
		public abstract void modify_evPickList (ArrayList<AvailableEarthquake> x);

		// Get the implementation class.

		public abstract XferEventPickerImpl xfer_get_impl ();
	}




	// Transfer parameters for event picker.

	public class XferEventPickerImpl extends XferEventPickerView implements OEGUIXferCommon {

		// Get the implementation class.

		@Override
		public XferEventPickerImpl xfer_get_impl () {
			return this;
		}

		// Constructor, ensure clean state.

		public XferEventPickerImpl () {
			internal_clean();
		}

		// Event list.

		private boolean dirty_evPickList;	// true if needs to be written back

		@Override
		public void modify_evPickList (ArrayList<AvailableEarthquake> x) {
			x_evPickList = x;
			dirty_evPickList = true;
		}


		// Clear all dirty-value flags.

		private void internal_clean () {
			dirty_evPickList = false;
			return;
		}

		@Override
		public void xfer_clean () {
			internal_clean();
			return;
		}


		// Load values.

		@Override
		public XferEventPickerImpl xfer_load () {

			// Clean state

			xfer_clean();

			// Event picker minimum magnitude option

			x_evPickMinMagParam = validParam(evPickMinMagParam);

			// Selected pick region, or null if none selected.

			int rix = validParam(evPickRegionParam);
			if (rix >= 0) {
				x_evPickRegion = evPickRegionList.get (rix);
			} else {
				x_evPickRegion = null;
			}

			// List for the dropdown, we don't pass it during load

			x_evPickList = null;

			return this;
		}


		// Store modified values back into the parameters.

		@Override
		public void xfer_store () throws GUIEDTException {

			// List for the dropdown

			if (dirty_evPickList) {
				dirty_evPickList = false;
				evPickList = x_evPickList;
				evPickListAttempted = true;
				refresh_evPickListDropdown();
			}

			return;
		}
	}




	//----- Client interface -----




	// Construct the sub-controller.
	// This creates all the controls.

	public OEGUISubEventPicker (OEGUIListener parent) throws GUIEDTException {
		super(parent);

		// Default enable state

		f_sub_enable = true;
		f_event_picker_enable = false;

		// Create and initialize controls

		init_eventPickerDialogParam();
		init_eventPickerEditParam();

		// Set initial enable state

		adjust_enable();
	}


	// Get the event picker edit button.
	// The intent is to use this only to insert the control into the client.

	public Parameter get_eventPickerEditParam () throws GUIEDTException {
		if (f_imp_modal) {
			return evPickModalOpenButton;
		}
		return eventPickerEditParam;
	}


	// Enable or disable the sub-controller.

	public void sub_event_picker_enable (boolean f_sub_enable, boolean f_event_picker_enable) throws GUIEDTException {
		this.f_sub_enable = f_sub_enable;
		this.f_event_picker_enable = f_event_picker_enable;
		adjust_enable();
		return;
	}


	// Make an event picker transfer object.

	public XferEventPickerImpl make_event_picker_xfer () {
		return new XferEventPickerImpl();
	}


	// Private function, used to report that the event picker options have changed.

	private void report_event_picker_change () throws GUIEDTException {
		//gui_controller.notify_event_picker_option_change();
		return;
	}


	// Update event picker values from model.

	//public void update_event_picker_from_model () throws GUIEDTException {
	//	return;
	//}




	//----- Parameter change actions ------




	@Override
	public void parameterChange_EDT (ParameterChangeEvent event) throws GUIEDTException {

		Parameter<?> param = event.getParameter();
		int parmgrp = get_parmgrp (param);


		// Switch on parameter group

		switch (parmgrp) {




		// Event picker option changed.
		// - Clear the dropdown list (also updates the select button).
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_EVPICK_VALUE: {
			if (!( f_sub_enable && f_event_picker_enable )) {
				return;
			}
			clear_evPickListDropdown();
			adjust_enable();
			report_event_picker_change();
		}
		break;




		// Event selection drowdown.
		// - Update the select button.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_EVPICK_EVENT: {
			if (!( f_sub_enable && f_event_picker_enable )) {
				return;
			}
			refresh_evPickTransferButton();
			adjust_enable();
			report_event_picker_change();
		}
		break;




		// Populate button.
		// - Adjust enable.
		// - Report to top-level controller.

		case PARMGRP_EVPICK_POPULATE: {
			if (!( f_sub_enable && f_event_picker_enable )) {
				return;
			}

			// Load the event picker parameters

			final XferEventPickerImpl xfer_event_picker_impl = new XferEventPickerImpl();
			if (!( gui_top.call_xfer_load (xfer_event_picker_impl, "Incorrect event picker parameters") )) {
				return;
			}

			// Clear the dropdown list

			clear_evPickListDropdown();
			adjust_enable();
			report_event_picker_change();

			// In case of error, this contains the error message (element 0) and the stack trace (element 1)

			final String[] search_result = new String[2];
			search_result[0] = null;
			search_result[1] = null;

			// Step to search for earthquakes in ComcatL.

			GUICalcStep searchStep_1 = new GUICalcStep(
				"Searching for Earthquakes",
				"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
				new Runnable() {

				@Override
				public void run() {
					try {
						String description = "mag = " + xfer_event_picker_impl.x_evPickMinMagParam.toString();
						double minMag = xfer_event_picker_impl.x_evPickMinMagParam.get_mag();
						long search_duration = xfer_event_picker_impl.x_evPickMinMagParam.get_search_duration();
						boolean f_require_oaf = xfer_event_picker_impl.x_evPickMinMagParam.get_f_require_oaf();
						SphRegion search_region = null;
						if (xfer_event_picker_impl.x_evPickRegion != null) {
							search_region = xfer_event_picker_impl.x_evPickRegion.get_region();
						}
						ArrayList<AvailableEarthquake> eventList = make_pick_list (description, minMag, search_duration, f_require_oaf, search_region);
						xfer_event_picker_impl.modify_evPickList (eventList);
					} catch (Exception e) {
						search_result[0] = "Error: " + ClassUtils.getClassNameWithoutPackage(e.getClass()) + ": " + e.getMessage();
						search_result[1] = SimpleUtils.getStackTraceAsString(e);
					}
				}
			});

			// Step to write back any parameter changes

			GUICalcStep searchStep_2 = new GUICalcStep(
				"Searching for Earthquakes",
				"Contacting USGS ComCat. This is occasionally slow. If it fails, trying again often works.",
				new GUIEDTRunnable() {

				@Override
				public void run_in_edt() throws GUIEDTException {
					xfer_event_picker_impl.xfer_store();
					adjust_enable();
					report_event_picker_change();
				}
			});

			// Step to report result to user

			GUIEDTRunnable postSearchStep = new GUIEDTRunnable() {
				@Override
				public void run_in_edt() throws GUIEDTException {

					// If success, report what we did

					if (search_result[0] == null) {
						if (evPickList.isEmpty()) {
							JOptionPane.showMessageDialog(gui_top.get_top_window(), "No earthquakes were found", "Search Result", JOptionPane.INFORMATION_MESSAGE);
						}
					}

					// If error, report the exception
							
					else {
						System.err.println (search_result[1]);
						JOptionPane.showMessageDialog(gui_top.get_top_window(), search_result[0], "Error sending DELETE product", JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			// Run the steps

			GUICalcRunnable.run_steps (gui_top.get_top_window(), postSearchStep, searchStep_1, searchStep_2);
		}
		break;




		// Transfer button.
		// - Transfer to data source.

		case PARMGRP_EVPICK_TRANSFER: {
			if (f_imp_modal) {
				return;
			}
			if (!( f_sub_enable && f_event_picker_enable )) {
				return;
			}
			int evix = validParam(evPickListDropdown);
			if (evix < 0 || evix > evPickList.size()) {
				return;
			}
			gui_controller.inject_event_id (evPickList.get(evix).event_id);
		}
		break;




		// Open modal dialog.
		// - Display the dialog.
		// - Check if user pressed the OK button.
		// - Transfer to data source.

		case PARMGRP_EVPICK_MODAL_OPEN: {
			if (!( f_imp_modal )) {
				return;
			}
			if (!( f_sub_enable )) {
				return;
			}

			// Set owner component for the dialog

			eventPickerEditParam.setOwnerParameter (evPickModalOpenButton);

			// Open the dialog

			eventPickerEditParam.openDialog();

			// If user pressed OK, transfer to data source

			if (eventPickerEditParam.getDialogTermCode() == GUIDialogParameter.TERMCODE_OK) {
				int evix = validParam(evPickListDropdown);
				if (evix < 0 || evix > evPickList.size()) {
					return;
				}
				gui_controller.inject_event_id (evPickList.get(evix).event_id);
			}
		}
		break;




		// Event picker edit button.
		// - Do nothing.

		case PARMGRP_EVPICK_EDIT: {
			if (!( f_sub_enable )) {
				return;
			}
		}
		break;




		// Unknown parameter group

		default:
			throw new IllegalStateException("OEGUISubEventPicker: Unknown parameter group: " + get_symbol_and_type(param));
		}


		return;
	}
	



	//----- Testing -----




	public static void main(String[] args) {

		return;
	}

}
