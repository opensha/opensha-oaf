package org.opensha.oaf.oetas.gui;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;


// RJ & ETAS GUI - Common base class.
// Michael Barall 04/16/2021
//
// GUI for working with the RJ and ETAS model.
//
// The GUI follows the model-view-controller design pattern.
// This class is a common base class for GUI components.
// It holds component linkage and static definitions.


public class OEGUIComponent {

	//----- Linkage -----

	// The top-level window.

	protected OEGUITop gui_top;

	// The model.

	protected OEGUIModel gui_model;

	// The view.

	protected OEGUIView gui_view;

	// The controller.

	protected OEGUIController gui_controller;




	//----- Construction -----


	// Constructor.

	public OEGUIComponent () {
		this.gui_top = null;
		this.gui_model = null;
		this.gui_view = null;
		this.gui_controller = null;
	}


	// Establish linkage between the components,
	// by copying component addresses from the other component.

	public void link_components (OEGUIComponent other) {
		this.gui_top		= other.gui_top;
		this.gui_model		= other.gui_model;
		this.gui_view		= other.gui_view;
		this.gui_controller	= other.gui_controller;
		return;
	}




	//----- Model state -----


	// The model proceeds through a series of states.
	// Initial state: The model is empty.
	// Mainshock state: The model contains a mainshock, and default or
	//  existing adjustable parameters..
	// Catalog state: The model contains a mainshock, a search region,
	//  a list of aftershocks, and a generic RJ model (which is used to
	//  obtain default parameters).
	// Parameter state: The model contains a sequence specific RJ model,
	//  a Bayesian RJ model if applicable, and an ETAS model if enabled..
	// Forecast state: The model contains foreacast tables.
	//
	// Note: The RJ GUI goes from catalog to parameter state, and then
	// from parameter to forecast state, in two steps.  In the RJ & ETAS
	// GUI, these were merged into a single step due to the fact that the
	// ETAS code is designed to work that way.
	//
	// Note: The mainshock state was added in the RJ & ETAS GUI to ease
	// access to functionality that does not require a catalog to be loaded
	// or a forecast to be computed.


	// The model state indicators.

	public static final int MODSTATE_MIN = 1;
	public static final int MODSTATE_INITIAL = 1;
	public static final int MODSTATE_MAINSHOCK = 2;
	public static final int MODSTATE_CATALOG = 3;
	public static final int MODSTATE_PARAMETERS = 4;
	public static final int MODSTATE_FORECAST = 5;
	public static final int MODSTATE_MAX = 5;


	// Convert a model state to a string.

	public static String get_modstate_as_string (int x) {
		switch (x) {
		case MODSTATE_INITIAL: return "MODSTATE_INITIAL";
		case MODSTATE_MAINSHOCK: return "MODSTATE_MAINSHOCK";
		case MODSTATE_CATALOG: return "MODSTATE_CATALOG";
		case MODSTATE_PARAMETERS: return "MODSTATE_PARAMETERS";
		case MODSTATE_FORECAST: return "MODSTATE_FORECAST";
		}
		return "MODSTATE_INVALID(" + x + ")";
	}




	//----- Parameter definitions -----


	// Enumeration of Data Source, for reading the catalog.
	
	public enum DataSource {
		COMCAT("Comcat"),
		CATALOG_FILE("Catalog File"),
		PUBLISHED_FORECAST("Published Forecast"),
		MAINSHOCK_ONLY("Mainshock Only"),
		DOWNLOAD_FILE("Download File"),
		//RJ_SIMULATION("RJ Simulation"),
		//ETAS_SIMULATION("ETAS Simulation");
		RJ_SIMULATION("Placeholder 1"),
		ETAS_SIMULATION("Placeholder 2");
		
		private String name;
		
		private DataSource(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}


	// Enumeration of Region Type, for the search region.
	
	public enum RegionType {
		STANDARD("Standard"),
		CENTROID_WC_CIRCLE("Centroid WC Circle"),
		CENTROID_CIRCLE("Centroid Circle"),
		EPICENTER_WC_CIRCLE("Epicenter WC Circle"),
		EPICENTER_CIRCLE("Epicenter Circle"),
		CUSTOM_CIRCLE("Custom Circle"),
		CUSTOM_RECTANGLE("Custom Rectangle");
		
		private String name;
		
		private RegionType(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}


	// Enumeration of automatic system enable.
	
	public enum AutoEnable {
		NORMAL("Normal"),
		ENABLE("Enable"),
		DISABLE("Disable");
		
		private String name;
		
		private AutoEnable(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}




	// Enumeration of event-sequence report options.
	
	public enum EvSeqReportOption {
		AUTO("Auto"),
		UPDATE("Update"),
		IGNORE("Ignore"),
		DELETE("Delete");
		
		private String label;
		
		private EvSeqReportOption (String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of event-sequence delete options.
	
	public enum EvSeqDeleteOption {
		DELETE("Delete"),
		IGNORE("Ignore"),
		CAP("Cap End Time");
		
		private String label;
		
		private EvSeqDeleteOption (String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of options for setting the time of next forecast.
	
	public enum NextForecastOption {
		OMIT("Not Specified"),
		UNKNOWN("Unknown"),
		NONE("None"),
		SET_TIME("Set Time");
		
		private String label;
		
		private NextForecastOption (String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of options for setting the time-dependent magnitude of completeness.
	
	public enum TimeDepMagCompOption {
		ENABLE("Enable"),
		WORLD("World Values"),
		CALIFORNIA("California Values"),
		EQUALS_MCAT("Constant = Mcat"),
		EQUALS_MC("Constant = Mc");
		
		private String label;
		
		private TimeDepMagCompOption (String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of ETAS enable options.
	
	public enum EtasEnableOption {
		AUTO("Auto"),
		ENABLE("Enable"),
		DISABLE("Disable"),
		ENABLE_FIT_ONLY("Enable (Fit Only)");
		
		private String label;
		
		private EtasEnableOption(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of ETAS prior options.
	
	public enum EtasPriorOption {
		MIXED("Mixed/Hierarchical"),
		GAUSSIAN("Gaussian"),
		UNIFORM("Uniform");
		
		private String label;
		
		private EtasPriorOption(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of ETAS model options.
	
	public enum EtasModelOption {
		AUTO("Auto"),
		BAYESIAN("Bayesian"),
		SEQSPEC("Sequence Specific"),
		GENERIC("Generic"),
		CUSTOM("Custom Weight");
		
		private String label;
		
		private EtasModelOption(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of options for forecast minimum magnitude bins.
	
	public enum MinMagBinsOption {
		AUTO("Auto"),
		RANGE_30_70("3.0 to 7.0"),
		RANGE_30_80("3.0 to 8.0");
		
		private String label;
		
		private MinMagBinsOption(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	// Enumeration of options for minimum magnitude in the event picker.
	
	public enum EvPickMinMagOption {
		ANY("Any", -10.0),
		MAG_45("M 4.5", 4.45),
		MAG_50("M 5.0", 4.95),
		MAG_55("M 5.5", 5.45),
		MAG_60("M 6.0", 5.95),
		MAG_65("M 6.5", 6.45);
		
		private String label;
		private double mag;		// magnitude to use in Comcat search
		
		private EvPickMinMagOption(String label, double mag) {
			this.label = label;
			this.mag = mag;
		}
		
		@Override
		public String toString() {
			return label;
		}

		public double get_mag() {
			return mag;
		}
	}




	//----- Support -----


	// Get a string describing the destination for PDL.

	public String get_pdl_dest () {
		String pdl_dest = "PDL (Dry Run)";
		switch ((new ServerConfig()).get_pdl_enable()) {
		case ServerConfigFile.PDLOPT_DEV:
			pdl_dest = "PDL-Development";
			break;
		case ServerConfigFile.PDLOPT_PROD:
			pdl_dest = "PDL-PRODUCTION";
			break;
		case ServerConfigFile.PDLOPT_SIM_DEV:
			pdl_dest = "PDL-Dev [SIMULATED]";
			break;
		case ServerConfigFile.PDLOPT_SIM_PROD:
			pdl_dest = "PDL-PROD [SIMULATED]";
			break;
		case ServerConfigFile.PDLOPT_DOWN_DEV:
			pdl_dest = "PDL-Dev [SIM DOWN]";
			break;
		case ServerConfigFile.PDLOPT_DOWN_PROD:
			pdl_dest = "PDL-PROD [SIM DOWN]";
			break;
		}
		return pdl_dest;
	}





	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
