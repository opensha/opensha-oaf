package org.opensha.oaf.oetas.gui;


// Reasenberg & Jones GUI - Common base class.
// Michael Barall 04/16/2021
//
// GUI for working with the Reasenberg & Jones model.
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
	// Catalog state: The model contains a mainshock, a search region,
	//  a list of aftershocks, and a generic model (which is used to
	//  obtain default parameters).
	// Aftershock parameter state: The model contains a sequence specific
	//  model, and, if applicable, a Bayesian model.
	// Forecast state: The model contains foreacast tables.


	// The model state indicators.

	public static final int MODSTATE_MIN = 1;
	public static final int MODSTATE_INITIAL = 1;
	public static final int MODSTATE_CATALOG = 2;
	public static final int MODSTATE_PARAMETERS = 3;
	public static final int MODSTATE_FORECAST = 4;
	public static final int MODSTATE_MAX = 4;


	// Convert a model state to a string.

	public static String get_modstate_as_string (int x) {
		switch (x) {
		case MODSTATE_INITIAL: return "MODSTATE_INITIAL";
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
		RJ_SIMULATION("RJ Simulation"),
		ETAS_SIMULATION("ETAS Simulation");
		
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





	//----- Testing -----


	public static void main(String[] args) {

		return;
	}

}
