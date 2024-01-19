package org.opensha.oaf.rj;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.Region;


// Class for region-dependent parameters.
// Author: Michael Barall 01/12/2024
//
// This class holds a set of parameters, which is determined by region.
// Type T is the class that holds parameter values.
//
// The primary function of this class is:  Given a location, return the corresponding
// parameter values, in an object of type T.  The Earth is paritioned into regions
// (which may also be tectonic regimes).  A query consists of finding the region that
// contains the given location, and then returning the parameter values assigned to
// that region.
//
// The parameter values come from a file, which is read in and stored in an object
// of this class.  This class contains the logic for reading the file.  The file
// contains the parameter values to be used within each region.  A region can be one
// of the 15 Garcia regions, or a region bounded by a user-supplied polygon.  Each
// region is identified by an OAFTectonicRegime, and the user-supplied polygons are
// represented by OAFRegion objects (which in this class must be OAFSphRegion).
//
// This is an abstract class.  There must be a subclass for each distinct type T.
// In practice, there is also an outer class, which holds a singleton of the subclass.
// The subclass must override these abstract functions to specify how to marshal
// and unmarshal objects of type T.
//
//     marshal_parameters (MarshalWriter writer, String name, T params)
//     unmarshal_parameters (MarshalReader reader, String name)
//
// The subclass may override the following function to specify how to display objects
// of type T.  If not overridden, the toString() method is used.
//
//     parameters_to_string (T params)
//
// The subclass may override the following functions to impose requirements on the
// parameter set.
//
//     require_full_coverage ()
//     require_non_null_parameters ()
//     require_default_parameters ()
//
// The subclass may override the following function to re-initialize instance variables
// (instance variables should also be initialized in the subclass constructor).
//
//     clear ()
//
// The subclass may override the following functions to add additional items to the
// JSON file.
//
//     do_marshal (MarshalWriter writer)
//     do_umarshal (MarshalReader reader)
//
// The subclass may override the following functions to maintain covariant return types
// and for additional flexibility in marshaling.
//
//     marshal (MarshalWriter writer, String name)
//     unmarshal (MarshalReader reader, String name)
//     unmarshal_config (String filename, Class<?> requester)
//
// The JSON file format is:
//
//	{
//	"OAF2ParameterSet" = Integer version number, currently 127001.
//	"selections" = [ Array containing parameter values for each tectonic regime.
//		element = { Structure containing regime and parameter values.
//			"regimes" = [ Array containing regimes that select these values
//				element = Name of tectonic regime.
//			]
//			"params" = { Structure containing parameter values.
//				. . .
//			}
//		. . .
//	]
//	"regions" = [ Array containing special regions.
//		element = { Structure containing regime and region.
//			"regime" = Name of tectonic regime.
//			"min_depth" = Minimum depth in km, positive down; use -1.0e10 if no bound.
//			"max_depth" = Maximum depth in km, positive down; use 1.0e10 if no bound.
//			"region" = { Structure containing marshaled SphRegion
//				"ClassType" = Integer code to select region type (see SphRegion.java).
//				. . .
//			}
//		. . .
//	]
//	}


public abstract class OAF2ParameterSet<T> implements Marshalable {


	//----- Constants -----


	// Value of minimum depth for no lower bound.

	public static final double NO_MIN_DEPTH = -1.0e10;

	// Value of maximum depth for no lower bound.

	public static final double NO_MAX_DEPTH = 1.0e10;


	// List of Garcia regions.

	protected static final String[] garcia_regions = {
		"ANSR-DEEPCON",
		"ANSR-HOTSPOT",
		"ANSR-OCEANBD",
		"ANSR-SHALCON",
		"ANSR-ABSLDEC",
		"ANSR-ABSLOCB",
		"ANSR-ABSLSHC",
		"SCR-ABVSLAB",
		"SCR-GENERIC",
		"SOR-ABVSLAB",
		"SOR-GENERIC",
		"SZ-GENERIC",
		"SZ-INLBACK",
		"SZ-ONSHORE",
		"SZ-OUTERTR"
	};

	// Get a copy of the list of Garcia regions.

	public static String[] get_garcia_regions () {
		return Arrays.copyOf (garcia_regions, garcia_regions.length);
	}

	// The world region.

	public static final String world_region = "WORLD";

	// The default region.

	public static final String default_region = "DEFAULT";

	// The exclude region.

	public static final String exclude_region = "EXCLUDE";




	//----- Subclass functions -----




	// Marshal parameters.

	protected abstract void marshal_parameters (MarshalWriter writer, String name, T params);

	// Unmarshal parameters.

	protected abstract T unmarshal_parameters (MarshalReader reader, String name);

	// Convert parameters to string for display.
	// Default is to use the toString function.
	// Note: Must accept null parameters.

	protected String parameters_to_string (T params) {
		if (params == null) {
			return "<null>";
		}
		return params.toString();
	}

	// Return true if the file must contain either a world region or all Garcia regions.
	// Default is to return false.
	// Note: If true, then any location query returns non-null regime.

	protected boolean require_full_coverage () {
		return false;
	}

	// Return true if all parameter objects in the file must be non-null.
	// Default is to return false.
	// Note: If true, then any query that returns non-null regime also returns non-null
	// parameters; and the parameters argument to marshal_parameters ia always non-null.
	// Note: If true, and if require_full_coverage is also true, then any location query
	// returns non-null parameters.

	protected boolean require_non_null_parameters () {
		return false;
	}

	// Return true if all there must be a default parameter object in the file.
	// Default is to return false.

	protected boolean require_default_parameters () {
		return false;
	}




	//----- File contents -----




	// Nested class to hold a selection

	private static class selection_holder<S> {

		// The regimes for this selection, must be non-empty.

		public String[] select_regimes;

		// The parameters for this selection.

		public S select_params;

		// Construct a selection with the given parameters and regimes.

		public selection_holder (S the_params, String... the_regimes) {
			if (the_regimes == null) {
				throw new IllegalArgumentException ("OAF2ParameterSet: No list of regimes provided for parameter selection");
			}
			if (the_regimes.length == 0) {
				throw new MarshalException ("OAF2ParameterSet: Empty list of regimes provided for parameter selection");
			}
			int len = the_regimes.length;
			select_regimes = new String[len];
			for (int i = 0; i < len; ++i) {
				select_regimes[i] = the_regimes[i];
			}
			select_params = the_params;
		}
	}


	// Produce a string representation of a selection holder.

	private String selection_to_string (selection_holder<T> selection) {
		StringBuilder result = new StringBuilder();

		result.append ("regimes = [");
		int len = selection.select_regimes.length;
		for (int i = 0; i < len; ++i) {
			if (i > 0) {
				result.append (", ");
			}
			result.append (selection.select_regimes[i]);
		}
		result.append ("]\n");

		result.append ("params = {");
		result.append (parameters_to_string (selection.select_params));
		result.append ("}\n");

		return result.toString();
	}


	// Marshal a selection.

	private void marshal_selection (MarshalWriter writer, String name, selection_holder<T> selection) {
		writer.marshalMapBegin (name);
		writer.marshalStringArray ("regimes", selection.select_regimes);
		marshal_parameters (writer, "params", selection.select_params);
		writer.marshalMapEnd ();
		return;
	}


	// Unmarshal a selection.

	private selection_holder<T> unmarshal_selection (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		String[] the_regimes = reader.unmarshalStringArray ("regimes");
		T the_params = unmarshal_parameters (reader, "params");
		reader.unmarshalMapEnd ();
		return new selection_holder<T> (the_params, the_regimes);
	}


	// Marshal a selection list.

	private void marshal_selection_list (MarshalWriter writer, String name, List<selection_holder<T>> the_list) {
		int selection_count = the_list.size();
		writer.marshalArrayBegin (name, selection_count);

		for (int i = 0; i < selection_count; ++i) {
			marshal_selection (writer, null, the_list.get(i));
		}

		writer.marshalArrayEnd ();
		return;
	}


	// Unmarshal a selection list.

	private void unmarshal_selection_list (MarshalReader reader, String name, List<selection_holder<T>> the_list) {
		int selection_count = reader.unmarshalArrayBegin (name);

		for (int i = 0; i < selection_count; ++i) {
			the_list.add (unmarshal_selection (reader, null));
		}

		reader.unmarshalArrayEnd ();
		return;
	}




	// selection_list - List of parameter selections in the file.

	private List<selection_holder<T>> selection_list = null;

	// region_list - List of regions in the file.
	// Note: We currently require all elements of this list to be of type OAFSphRegion.

	private List<OAFRegion> region_list = null;


	// Add a selection.
	// Parameters:
	//  the_params = Parameters for the selection.
	//  the_regimes = Regimes for the selection.

	public void add_selection (T the_params, String... the_regimes) {
		selection_list.add (new selection_holder<T> (the_params, the_regimes));
		return;
	}


	// Add a region.
	// Parameters:
	//  regime_name = Name of the regime.
	//  sph_region = Spherical region.
	//  min_depth = Minimum depth.
	//  max_depth = Maximum depth.

	public void add_region (String regime_name, SphRegion sph_region, double min_depth, double max_depth) {
		OAFTectonicRegime regime = OAFTectonicRegime.forName (regime_name);
		region_list.add (new OAFSphRegion (regime, sph_region, min_depth, max_depth));
		return;
	}


	// Add a region.
	// Parameters:
	//  the_region = The region to add.
	// Note: We currently require the region to be of type OAFSphRegion.

	public void add_region (OAFRegion the_region) {
		if (!( the_region instanceof OAFSphRegion )) {
			throw new IllegalArgumentException ("OAF2ParameterSet.add_region: Region is not of type OAFSphRegion");
		}
		region_list.add (the_region);
		return;
	}




	//----- Computed data -----




	// Map from tectonic regime to the parameter selection.

	private Map<OAFTectonicRegime, selection_holder<T>> regime_to_selection = null;

	// regime_names - Names of regimes, as they appear in the file.

	private Set<String> regime_names = null;

	// Number of Garcia regions used.

	private int garcia_count = 0;

	// The world selection, or null if none.

	private selection_holder<T> world_selection = null;

	// The default selection, or null if none.

	private selection_holder<T> default_selection = null;

	// The world regime (set up during construction and not subsequently changed).

	private OAFTectonicRegime world_regime = null;

	// The default regime (set up during construction and not subsequently changed).

	private OAFTectonicRegime default_regime = null;

	//The exclude regime (set up during construction and not subsequently changed).

	private OAFTectonicRegime exclude_regime = null;




	// Finish setting up.
	// Assumes the file contents have been loaded.
	// Calculates computed data.
	// Throws exception if error found.

	public void finish_setup () {

		// Clear computed data

		regime_to_selection = new LinkedHashMap<OAFTectonicRegime, selection_holder<T>>();
		regime_names = new LinkedHashSet<String>();
		garcia_count = 0;
		world_selection = null;
		default_selection = null;

		// Requirement flags

		boolean f_req_full_coverage = require_full_coverage();
		boolean f_req_non_null_parameters = require_non_null_parameters();
		boolean f_req_default_parameters = require_default_parameters();

		// Make the set of garcia regimes

		Set<OAFTectonicRegime> garcia_regimes = new HashSet<OAFTectonicRegime>();
		for (String s : garcia_regions) {
			garcia_regimes.add (OAFTectonicRegime.forName (s));
		}

		// Loop over selections ...

		for (selection_holder<T> selection : selection_list) {

			// Enforce non-null parameters if desired

			if (f_req_non_null_parameters) {
				if (selection.select_params == null) {
					throw new MarshalException ("OAF2ParameterSet.finish_setup: Found null parameter object");
				}
			}

			// Loop over regime names ...

			for (String s : selection.select_regimes) {

				// Get the regime

				OAFTectonicRegime r = OAFTectonicRegime.forName (s);

				// Error if we have already seen this regime

				if (regime_to_selection.containsKey (r)) {
					throw new MarshalException ("OAF2ParameterSet.finish_setup: Duplicate regime name in parameter selections: " + s);
				}

				// Error if this is the exclude regime

				if (r.equals (exclude_regime)) {
					throw new MarshalException ("OAF2ParameterSet.finish_setup: Excluded regime name in parameter selections: " + exclude_region);
				}

				// Add to the map

				regime_to_selection.put (r, selection);

				// Add to regime names

				regime_names.add (s);

				// If this is a Garcia region, count it

				if (garcia_regimes.contains (r)) {
					++garcia_count;
				}

				// If this is the world regime, save the selection

				if (r.equals (world_regime)) {
					world_selection = selection;
				}

				// If this is the default regime, save the selection

				if (r.equals (default_regime)) {
					default_selection = selection;
				}
			}
		}

		// Enforce full coverage if desired

		if (f_req_full_coverage) {
			if (!( world_selection != null || garcia_count == garcia_regimes.size() )) {
				throw new MarshalException ("OAF2ParameterSet.finish_setup: File does not contain world regime or all " + garcia_regimes.size() + " Garcia regimes");
			}
		}

		// Enforce default parameters if desired

		if (f_req_default_parameters) {
			if (!( default_selection != null )) {
				throw new MarshalException ("OAF2ParameterSet.finish_setup: File does not contain default parameters");
			}
		}

		// Loop over regions ...

		for (OAFRegion region : region_list) {

			// Get the regime

			OAFTectonicRegime r = region.get_regime();

			// Error if this is a Garcia regime

			if (garcia_regimes.contains (r)) {
				throw new MarshalException ("OAF2ParameterSet.finish_setup: Region name conflicts with Garcia region name: " + r.toString());
			}

			// Error if this is the world regime

			if (r.equals (world_regime)) {
				throw new MarshalException ("OAF2ParameterSet.finish_setup: Region name conflicts with world region name: " + world_region);
			}

			// Error if this is the default regime

			if (r.equals (default_regime)) {
				throw new MarshalException ("OAF2ParameterSet.finish_setup: Region name conflicts with default region name: " + default_region);
			}

			// If this is not the exclude regime, then it must appear in the regime to selection map

			if (!( r.equals (exclude_regime) )) {
				if (!( regime_to_selection.containsKey (r) )) {
					throw new MarshalException ("OAF2ParameterSet.finish_setup: No parameters defined for region name: " + r.toString());
				}
			}
		}

		// Success

		return;
	}




	// Get the default regime.

	public final OAFTectonicRegime get_default_regime () {
		return default_regime;
	}




	//----- Searching -----




	// Find the parameters for the given tectonic regime.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<T> get_params (OAFTectonicRegime r) {

		// Return this regime if we have a parameter selection for it

		selection_holder<T> selection = regime_to_selection.get (r);
		if (selection != null) {
			return new OAFRegimeParams<T> (r, selection.select_params);
		}

		// Didn't find any parameter selection for this regime

		return new OAFRegimeParams<T>();
	}


	// Find the parameters for the tectonic regime with the given name.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<T> get_params (String name) {

		// Get the regime with the given name, or null if none exists

		OAFTectonicRegime r = OAFTectonicRegime.forExistingName (name);

		// Return this regime if we have a parameter selection for it

		if (r != null) {
			selection_holder<T> selection = regime_to_selection.get (r);
			if (selection != null) {
				return new OAFRegimeParams<T> (r, selection.select_params);
			}
		}

		// Didn't find any parameter selection for this regime

		return new OAFRegimeParams<T>();
	}
	

	// Find the parameters for the given location.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<T> get_params (Location loc) {

		// Location allows longitude -180 to 360, so bring it in range

		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		boolean f_changed = false;

		while (lon > 180.0) {
			lon -= 360.0;
			f_changed = true;
		}

		// For locations very close to the date line or pole, nudge them so that
		// they compare as expected to regions that end right at the date line or pole

		if (lon < -179.995) {
			lon = -179.995;
			f_changed = true;
		}

		if (lon > 179.995) {
			lon = 179.995;
			f_changed = true;
		}

		if (lat < -89.995) {
			lat = -89.995;
			f_changed = true;
		}

		if (lat > 89.995) {
			lat = 89.995;
			f_changed = true;
		}

		if (f_changed) {
			loc = new Location (lat, lon, loc.getDepth());
		}

		// I don't know if Region.contains is thread-safe, so synchronize this

		synchronized (this) {

			// If the point is in a special region, use the tectonic regime for that region

			for (OAFRegion region : region_list) {
				if (region.contains (loc)) {
					OAFTectonicRegime r = region.get_regime();

					// Return this regime if we have a parameter selection for it

					selection_holder<T> selection = regime_to_selection.get (r);
					if (selection != null) {
						return new OAFRegimeParams<T> (r, selection.select_params);
					}

					// Otherwise, it must be an exclude region, stop searching the List

					break;
				}
			}
		}

		// If there are Garcia regions ...

		if (garcia_count > 0) {

			// Use the tectonic regime table to get the Garcia region

			TectonicRegimeTable regime_table = new TectonicRegimeTable();
			OAFTectonicRegime r =  OAFTectonicRegime.forName (regime_table.get_strec_name (loc.getLatitude(), loc.getLongitude()));

			// Return this regime if we have a parameter selection for it

			selection_holder<T> selection = regime_to_selection.get (r);
			if (selection != null) {
				return new OAFRegimeParams<T> (r, selection.select_params);
			}
		}

		// Return the world regime if we have a parameter selection for it

		if (world_selection != null) {
			return new OAFRegimeParams<T> (world_regime, world_selection.select_params);
		}

		// Didn't find any parameter selection for this location

		return new OAFRegimeParams<T>();
	}
	

	// Find the default parameters.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<T> get_default_params () {

		// Return the default regime if we have a parameter selection for it

		if (default_selection != null) {
			return new OAFRegimeParams<T> (default_regime, default_selection.select_params);
		}

		// Don't have default parameters

		return new OAFRegimeParams<T>();
	}




	// Produce a string representation of an OAFRegimeParams object.

	public String regime_params_to_string (OAFRegimeParams<T> regime_params) {
		StringBuilder result = new StringBuilder();

		result.append ("regime = ");
		if (regime_params.regime == null) {
			result.append ("<null>");
		} else {
			result.append (regime_params.regime.toString());
		}
		result.append ("\n");

		result.append ("params = {");
		result.append (parameters_to_string (regime_params.params));
		result.append ("}\n");

		return result.toString();
	}




	// Find the parameters for the given tectonic regime, and return the result as a string.
	// This function is primarily for testing.

	public String get_params_as_string (OAFTectonicRegime r) {
		return regime_params_to_string (get_params (r));
	}


	// Find the parameters for the tectonic regime with the given name, and return the result as a string.
	// This function is primarily for testing.

	public String get_params_as_string (String name) {
		return regime_params_to_string (get_params (name));
	}


	// Find the parameters for the given location, and return the result as a string.
	// This function is primarily for testing.

	public String get_params_as_string (Location loc) {
		return regime_params_to_string (get_params (loc));
	}


	// Find the default parameters, and return the result as a string.
	// This function is primarily for testing.

	public String get_default_params_as_string () {
		return regime_params_to_string (get_default_params ());
	}




	// Return a set containing the tectonic regimes for which parameters are defined.

	public Set<OAFTectonicRegime> getRegimeSet() {
		return regime_to_selection.keySet();
	}
	

	// Return a set containing the names of tectonic regimes for which parameters are defined, as they appear in the file.

	public Set<String> getRegimeNameSet() {
		return regime_names;
	}


	// Return a read-only view of the list of regions in the file.

	public List<OAFRegion> get_region_list () {
		return Collections.unmodifiableList (region_list);
	}




	//----- Construction -----




	// Clear to an empty parameter set.

	private void pset_clear () {
		selection_list = new ArrayList<selection_holder<T>>();
		region_list = new ArrayList<OAFRegion>();

		regime_to_selection = new LinkedHashMap<OAFTectonicRegime, selection_holder<T>>();
		regime_names = new LinkedHashSet<String>();
		garcia_count = 0;
		world_selection = null;
		default_selection = null;

		return;
	}




	// Clear to an empty parameter set.

	public void clear () {
		pset_clear();
		return;
	}




	// Constructor makes an empty parameter set.

	public OAF2ParameterSet () {

		// Make the world regime

		world_regime = OAFTectonicRegime.forName (world_region);

		// Make the default regime

		default_regime = OAFTectonicRegime.forName (default_region);

		// Make the exclude regime

		exclude_regime = OAFTectonicRegime.forName (exclude_region);

		// Clear everything

		pset_clear();
	}




	// Write string containing contents.

	@Override
	public String toString () {
		StringBuilder result = new StringBuilder();

		result.append ("OAF2ParameterSet:" + "\n");
		result.append ("selections = [" + "\n");

		for (selection_holder<T> selection : selection_list) {
			result.append ("\n");
			result.append (selection_to_string (selection));
		}

		result.append ("]" + "\n");
		result.append ("regions = [" + "\n");

		for (OAFRegion region : region_list) {
			result.append (region.toString() + "\n");
		}

		result.append ("]" + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 127001;

	private static final String M_VERSION_NAME = "OAF2ParameterSet";




	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			marshal_selection_list (writer, "selections", selection_list);
			OAFSphRegion.static_marshal_list (writer, "regions", region_list);

			break;
		}
	
		return;
	}




	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {

		clear();
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			unmarshal_selection_list (reader, "selections", selection_list);
			OAFSphRegion.static_unmarshal_list (reader, "regions", region_list);

			break;
		}

		finish_setup();

		return;
	}




	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}




	// Unmarshal object.

	@Override
	public OAF2ParameterSet<T> unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}




	// Unmarshal object from a configuration file.
	// Parameters:
	//  filename = Name of file (not including a path).
	//  requester = Class that is requesting the file.

	public OAF2ParameterSet<T> unmarshal_config (String filename, Class<?> requester) {
		OAFParameterSet.unmarshal_file_as_json (this, filename, requester);
		return this;
	}




	//----- Testing -----




	// Make a test parameter set which is empty.
	// Note: The parameter set allows null parameters, but null parameters cannot be marshaled.

	private static OAF2ParameterSet<String> test_make_empty () {

		OAF2ParameterSet<String> pset = new OAF2ParameterSet<String>() {

			@Override
			protected void marshal_parameters (MarshalWriter writer, String name, String params) {
				writer.marshalString (name, params);
				return;
			}

			@Override
			protected String unmarshal_parameters (MarshalReader reader, String name) {
				return reader.unmarshalString (name);
			}

		};

		return pset;
	}




	// Make a test parameter set from the RJ generic parameters.

	private static OAF2ParameterSet<String> test_make_from_rj_generic () {

		OAF2ParameterSet<String> pset = test_make_empty();

		// Load the data

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();

		// Add a selection for each regime name

		Set<String> fetch_regime_names = fetch.getRegimeNameSet();
		for (String s : fetch_regime_names) {
			pset.add_selection ("Parameters for '" + s + "' region", s);
		}

		// Add a default selection

		pset.add_selection ("Parameters for '" + default_region + "' region", default_region);

		// Add a region for each region in the file

		List<OAFRegion> fetch_region_list = fetch.get_region_list ();
		for (OAFRegion r : fetch_region_list) {
			pset.add_region (r);
		}

		// Finish the setup

		pset.finish_setup();

		return pset;
	}




	// Make a test parameter set from the RJ generic parameters, grouping regimes in pairs.
	// Parameters:
	//  skip = Number of regimes to skip.
	//  f_null = If true, skipped and default regimes are set to null (instead of being omitted).

	private static OAF2ParameterSet<String> test_make_paired_from_rj_generic (int skip, boolean f_null) {

		OAF2ParameterSet<String> pset = test_make_empty();

		// Load the data

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();

		// Add a selection for each pair of regime names

		Set<String> fetch_regime_names = fetch.getRegimeNameSet();
		int n = 0;
		String previous = null;

		for (String s : fetch_regime_names) {
			if (n < skip) {
				if (f_null) {
					pset.add_selection (null, s);
				}
			}
			else {
				if (previous == null) {
					previous = s;
				}
				else {
					pset.add_selection ("Parameters for '" + previous + "' and '" + s + "' regions", previous, s);
					previous = null;
				}
			}
			++n;
		}

		if (previous != null) {
			pset.add_selection ("Parameters for '" + previous + "' region", previous);
		}

		// Add a default selection if requested

		if (f_null) {
			pset.add_selection (null, default_region);
		}

		// Add a region for each region in the file

		List<OAFRegion> fetch_region_list = fetch.get_region_list ();
		for (OAFRegion r : fetch_region_list) {
			pset.add_region (r);
		}

		// Finish the setup

		pset.finish_setup();

		return pset;
	}




	// Make a test parameter set from the magnitude of completeness parameters.

	private static OAF2ParameterSet<String> test_make_from_mag_comp () {

		OAF2ParameterSet<String> pset = test_make_empty();

		// Load the data

		MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();

		// Add a selection for each regime name

		Set<String> fetch_regime_names = fetch.getRegimeNameSet();
		for (String s : fetch_regime_names) {
			pset.add_selection ("Parameters for '" + s + "' region", s);
		}

		// Add a default selection

		pset.add_selection ("Parameters for '" + default_region + "' region", default_region);

		// Add a region for each region in the file

		List<OAFRegion> fetch_region_list = fetch.get_region_list ();
		for (OAFRegion r : fetch_region_list) {
			pset.add_region (r);
		}

		// Finish the setup

		pset.finish_setup();

		return pset;
	}




	// Run a test of marshaling.
	// Parameters:
	//  pset = Parameter set.
	//  dest = Empty parameter set used as the destination for unmarshaling.
	// Note: This function is usable by other classes with any type of parameters.

	public static void test_marshaling (OAF2ParameterSet<?> pset, OAF2ParameterSet<?> dest) {

		// Display source

		System.out.println ();
		System.out.println ("********** Source Parameter Set **********");
		System.out.println ();

		System.out.println (pset.toString());

		// Marshal

		System.out.println ();
		System.out.println ("********** Marshal Parameters **********");
		System.out.println ();

		String json_string = MarshalUtils.to_formatted_json_string (pset);
		System.out.println (json_string);

		// Unmarshal

		System.out.println ();
		System.out.println ("********** Unmarshal Parameters **********");
		System.out.println ();

		MarshalUtils.from_json_string (dest, json_string);
		System.out.println (dest.toString());

		return;
	}




	// Run a test of searching.
	// Parameters:
	//  pset = Parameter set.
	//  loc_list = List of locations to query, or null to use a default list.
	// Note: This function is usable by other classes with any type of parameters.

	public static void test_searching (OAF2ParameterSet<?> pset, List<Location> loc_list) {

		// Display parameter set

		System.out.println ();
		System.out.println ("********** Parameter Set **********");
		System.out.println ();

		System.out.println (pset.toString());

		// Query by location

		System.out.println ();
		System.out.println ("********** Query by Location **********");

		List<Location> my_loc_list = loc_list;
		if (my_loc_list == null) {
			my_loc_list = OAFParameterSet.getTestLocations();
		}

		for (Location loc : my_loc_list) {
			System.out.println ();
			System.out.println ("Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
			System.out.println (pset.get_params_as_string (loc));
		}

		// Query by regime name

		System.out.println ();
		System.out.println ("********** Query by Regime Name **********");

		for (String name : pset.getRegimeNameSet()) {
			System.out.println ();
			System.out.println ("Query name : " + name);
			System.out.println (pset.get_params_as_string (name));
		}

		System.out.println ();
		System.out.println ("Query name : " + exclude_region);
		System.out.println (pset.get_params_as_string (exclude_region));

		String unknown_region = "Unknown_Region";

		System.out.println ();
		System.out.println ("Query name : " + unknown_region);
		System.out.println (pset.get_params_as_string (unknown_region));

		// Query by regime

		System.out.println ();
		System.out.println ("********** Query by Regime **********");

		for (OAFTectonicRegime r : pset.getRegimeSet()) {
			System.out.println ();
			System.out.println ("Query regime : " + r.toString());
			System.out.println (pset.get_params_as_string (r));
		}

		System.out.println ();
		System.out.println ("Query regime : " + exclude_region);
		System.out.println (pset.get_params_as_string (OAFTectonicRegime.forName (exclude_region)));

		// Query default

		System.out.println ();
		System.out.println ("********** Query Default **********");

		System.out.println ();
		System.out.println ("Query default");
		System.out.println (pset.get_default_params_as_string ());

		System.out.println ();

		return;
	}




	// Entry point.

	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "TestArgs");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Create a test set of parameters based on rj generic, and display the parameters.

		if (testargs.is_test ("test1")) {

			// Zero additional argument

			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_from_rj_generic ();

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (pset.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  skip  f_null
		// Create a test set of parameters based on paired rj generic, and display the parameters.

		if (testargs.is_test ("test2")) {

			// Get arguments

			int skip = testargs.get_int ("skip");
			boolean f_null = testargs.get_boolean ("f_null");
			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_paired_from_rj_generic (skip, f_null);

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (pset.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Create a test set of parameters based on magnitude of completeness, and display the parameters.

		if (testargs.is_test ("test3")) {

			// Zero additional argument

			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_from_mag_comp ();

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (pset.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4
		// Create a test set of parameters based on rj generic, and test marshaling.

		if (testargs.is_test ("test4")) {

			// Zero additional argument

			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_from_rj_generic ();

			// Test it

			OAF2ParameterSet<String> dest = test_make_empty ();
			test_marshaling (pset, dest);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  skip  f_null
		// Create a test set of parameters based on paired rj generic, and test marshaling.

		if (testargs.is_test ("test5")) {

			// Get arguments

			int skip = testargs.get_int ("skip");
			boolean f_null = testargs.get_boolean ("f_null");
			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_paired_from_rj_generic (skip, f_null);

			// Test it

			OAF2ParameterSet<String> dest = test_make_empty ();
			test_marshaling (pset, dest);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6
		// Create a test set of parameters based on magnitude of completeness, and test marshaling.

		if (testargs.is_test ("test6")) {

			// Zero additional argument

			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_from_mag_comp ();

			// Test it

			OAF2ParameterSet<String> dest = test_make_empty ();
			test_marshaling (pset, dest);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7
		// Create a test set of parameters based on rj generic, and test searching.

		if (testargs.is_test ("test7")) {

			// Zero additional argument

			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_from_rj_generic ();

			// Test it

			test_searching (pset, null);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  skip  f_null
		// Create a test set of parameters based on paired rj generic, and test searching.

		if (testargs.is_test ("test8")) {

			// Get arguments

			int skip = testargs.get_int ("skip");
			boolean f_null = testargs.get_boolean ("f_null");
			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_paired_from_rj_generic (skip, f_null);

			// Test it

			test_searching (pset, null);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9
		// Create a test set of parameters based on magnitude of completeness, and test searching.

		if (testargs.is_test ("test9")) {

			// Zero additional argument

			testargs.end_test();

			// Create a parameter set

			OAF2ParameterSet<String> pset = test_make_from_mag_comp ();

			// Test it

			test_searching (pset, null);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}

}
