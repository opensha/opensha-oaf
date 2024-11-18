package org.opensha.oaf.rj;

import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.List;

import org.opensha.commons.geo.BorderType;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.Region;

import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.OAFRegion;
import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalImpArray;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;

public class MagCompPage_ParametersFetch {

	// parameter_set - The parameter set.

	private OAFParameterSet<MagCompPage_Parameters> parameter_set = null;

	// cached_parameter_set - The cached parameter set, so parameters do not have to be reloaded from file each time.

	private static OAFParameterSet<MagCompPage_Parameters> cached_parameter_set = null;

	// load_data - Load parameters from the data file.
	//
	// The text data file format is:  [NOT SUPPORTED ANY MORE]
	//	[int]		Number of tectonic regimes
	//	[repeated]	Repeated once for each tectonic regime:
	//		[string]	Name of tectonic regime
	//		[double]	magCat
	//		[double]	capG
	//		[double]	capH
	//		[double]	magSample
	//		[double]	radiusSample
	//		[double]	magCentroid
	//		[double]	radiusCentroid
	//	[int]		Number of special regions
	//	[repeated]	Repeated once for each special region:
	//		[string]	Name of tectonic regime to apply in this region
	//		[double]	Minimum depth in km, positive down; use -1.0e10 if no bound
	//		[double]	Maximum depth in km, positive down; use 1.0e10 if no bound
	//		[int]		Polygon border type: 1 = Mercator Linear, 2 = Great Circle
	//		[int]		Number of polygon vertices
	//		[repeated]	Repeated once for each polygon vertex:
	//			[double]	Latitude in degrees, between -90 and 90
	//			[double]	Longitude in degrees, between -180 and 180
	//
	// The JSON data file format is:
	//	{
	//	"version" = Integer version number, should be 1.
	//	"regimes" = [ Array containing parameter values for each tectonic regime.
	//		element = { Structure containing regime and parameter values.
	//			"regime" = Name of tectonic regime.
	//			"params" = { Structure containing marshaled MagCompPage_Parameters.
	//				"MagCompPage_Parameters" = Integer version number, should be 2001 to 2003 (2001 to 2002 are deprecated).
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
	//
	// Notes:
	// The list of tectonic regimes must include, at a minimum, either the 15 Garcia
	// regions or a world region (but not both).
	// A tectonic regime may appear in more than one special region.
	// If special regions overlap, the region listed first "wins".
	//
	// For the text format only:
	// In each special region, the first and last polygon vertices should be the same
	// (although, according to Region, this is not strictly necessary).
	// A special region cannot cross the date line.  If this is needed, use two regions.

	private static synchronized OAFParameterSet<MagCompPage_Parameters> load_data () {

		// If data is already loaded, do nothing

		if (cached_parameter_set != null) {
			return cached_parameter_set;
		}

		// Working data

		OAFParameterSet<MagCompPage_Parameters> wk_parameter_set = new OAFParameterSet<MagCompPage_Parameters>(){
			
			// load_parameter_values - Load parameter values for the tables.
			// This function should create a new object of type T, read the
			// parameter values from the Scanner, and return the object.
			// In case of error, this function should throw RuntimeException.

			@Override
			protected MagCompPage_Parameters load_parameter_values (Scanner sc) {

				//// Get the Page parameters
				//
				//double magCat = load_table_double (sc);
				//double capG = load_table_double (sc);
				//double capH = load_table_double (sc);
				//double magSample = load_table_double (sc);
				//double radiusSample = load_table_double (sc);
				//double magCentroid = load_table_double (sc);
				//double radiusCentroid = load_table_double (sc);
				//
				//// Make the parameter object
				//
				//return new MagCompPage_Parameters(magCat, capG, capH, magSample, radiusSample, magCentroid, radiusCentroid);

				throw new UnsupportedOperationException ("MagCompPage_ParametersFetch: Loading from text file is not supported");
			}

			// load_parameter_values - Load parameter values for the tables.
			// This function should create a new object of type T, read the
			// parameter values from the MarshalReader, and return the object.
			// In case of error, this function should throw RuntimeException (or a subclass).

			@Override
			protected MagCompPage_Parameters load_parameter_values (MarshalReader reader, String name) {
				return (new MagCompPage_Parameters()).unmarshal (reader, name);
			}
		};

		// Load the data

		//wk_parameter_set.load_data ("MagCompPage_ParametersFetch.txt", MagCompPage_ParametersFetch.class);
		wk_parameter_set.load_json_data ("MagCompPage_ParametersFetch.json", MagCompPage_ParametersFetch.class);

		// Save our working data into the static variable

		cached_parameter_set = wk_parameter_set;
		return cached_parameter_set;
	}

	// Constructor loads the data if needed.
	
	public MagCompPage_ParametersFetch() {
		parameter_set = load_data();
	}

	// unload_data - Remove the cached data from memory.
	// The data will be reloaded the next time one of these objects is created.
	// Any existing objects will continue to use the old data.
	// This makes it possible to load new parameter values without restarting the program.

	public static synchronized void unload_data () {
		cached_parameter_set = null;
		return;
	}
	
	/**
	 * Find the tectonic regime for the given location, and return its parameters.
	 * @param loc = Location.
	 * @return Object of type T containing parameters.
	 */
	public final MagCompPage_Parameters get(Location loc) {
		return parameter_set.get(loc);
	}
	
	/**
	 * Return parameters for a tectonic regime, throw exception if regime is unknown.
	 * @param region = Tectonic regime.
	 * @return Object of type T containing parameters.
	 * The function throws an exception if no parameters are defined for the region.
	 */
	public final MagCompPage_Parameters get(OAFTectonicRegime region) {
		return parameter_set.get(region);
	}
	
	/**
	 * Return parameters for a tectonic regime, or null if tectonic regime is unknown.
	 * @param region = Tectonic regime.
	 * @return Object of type T containing parameters.
	 * The function returns null if no parameters are defined for the region.
	 */
	public final MagCompPage_Parameters getOrNull(OAFTectonicRegime region) {
		return parameter_set.getOrNull(region);
	}
	
	/**
	 * Find the tectonic regime for the given location.
	 * @param loc = Location.
	 * @return Tectonic regime for the location.
	 */
	public final OAFTectonicRegime getRegion(Location loc) {
		return parameter_set.getRegion(loc);
	}
	
	/**
	 * Return a set containing the tectonic regimes.
	 */
	public final Set<OAFTectonicRegime> getRegimeSet() {
		return parameter_set.getRegimeSet();
	}
	
	/**
	 * Return a set containing the names of tectonic regimes, as they appear in the file.
	 */
	public final Set<String> getRegimeNameSet() {
		return parameter_set.getRegimeNameSet();
	}


	// Return a read-only view of the list of regions in the file.

	public final List<OAFRegion> get_region_list () {
		return parameter_set.get_region_list();
	}


	// Get the list of non-null parameters.
	// Note: The caller must not modify the list or any of the parameters.
	// The intended use is to iterate over the list of parmaeters.

	public final List<MagCompPage_Parameters> get_parameter_list () {
		return parameter_set.get_parameter_list();
	}



	
	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("MagCompPage_ParametersFetch : Missing subcommand");
			return;
		}

		// Subcommand : Test #1
		// Command format:
		//  test1
		// Display tectonic regime and parameters for a selected list of points.

		if (args[0].equalsIgnoreCase ("test1")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test1' subcommand");
				return;
			}

			// Display info for each point in a list of locations

			LocationList locs = OAFParameterSet.getTestLocations();;
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			for (Location loc : locs) {
				OAFTectonicRegime regime = fetch.getRegion(loc);
				System.out.println(loc+", "+regime+": "+fetch.get(regime));
			}

			return;
		}

		// Subcommand : Test #2
		// Command format:
		//  test2
		// List all regimes, and the properties assigned to each.

		if (args[0].equalsIgnoreCase ("test2")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test2' subcommand");
				return;
			}

			// Display info for each regime
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			Set<OAFTectonicRegime> regimes = fetch.getRegimeSet();
			for (OAFTectonicRegime regime : regimes) {
				System.out.println(regime+": "+fetch.get(regime));
			}

			return;
		}

		// Subcommand : Test #3
		// Command format:
		//  test3
		// List all regimes, and the properties assigned to each.
		// Each is displayed twice, once fetched from parameters, once unmarshaled.

		if (args[0].equalsIgnoreCase ("test3")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test3' subcommand");
				return;
			}

			// Display info for each regime
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			Set<OAFTectonicRegime> regimes = fetch.getRegimeSet();

			MarshalImpArray store = new MarshalImpArray();
			store.marshalMapBegin (null);

			for (OAFTectonicRegime regime : regimes) {
				//MagCompPage_Parameters.marshal_poly(store, regime.toString(), fetch.get(regime));
				fetch.get(regime).marshal(store, regime.toString());
			}

			store.marshalMapEnd ();
			store.check_write_complete ();
			store.unmarshalMapBegin (null);

			for (OAFTectonicRegime regime : regimes) {
				System.out.println(regime+": "+fetch.get(regime));
				//System.out.println(regime+": "+MagCompPage_Parameters.unmarshal_poly(store, regime.toString()));
				System.out.println(regime+": "+(new MagCompPage_Parameters()).unmarshal(store, regime.toString()));
			}

			store.unmarshalMapEnd ();
			store.check_read_complete ();

			return;
		}

		// Subcommand : Test #4
		// Command format:
		//  test4
		// List all regimes, and the properties assigned to each.
		// Each is displayed twice, once fetched from parameters, once unmarshaled.
		// This version is marshaled to JSON, and the JSON is displayed at the end.

		if (args[0].equalsIgnoreCase ("test4")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test4' subcommand");
				return;
			}

			// Display info for each regime
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			Set<OAFTectonicRegime> regimes = fetch.getRegimeSet();

			MarshalImpJsonWriter store = new MarshalImpJsonWriter();
			store.marshalMapBegin (null);

			for (OAFTectonicRegime regime : regimes) {
				MagCompPage_Parameters.marshal_poly(store, regime.toString(), fetch.get(regime));
			}

			store.marshalMapEnd ();
			store.check_write_complete ();
			String json_string = store.get_json_string();

			MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
			retrieve.unmarshalMapBegin (null);

			for (OAFTectonicRegime regime : regimes) {
				System.out.println(regime+": "+fetch.get(regime));
				System.out.println(regime+": "+MagCompPage_Parameters.unmarshal_poly(retrieve, regime.toString()));
			}

			retrieve.unmarshalMapEnd ();
			retrieve.check_read_complete ();

			System.out.println (json_string);

			return;
		}

		// Subcommand : Test #5
		// Command format:
		//  test5
		// List all regimes, and the properties assigned to each.
		// Also print a table of magnitude-dependent parameters for various magnitudes.

		if (args[0].equalsIgnoreCase ("test5")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test5' subcommand");
				return;
			}

			// Display info for each regime
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			Set<OAFTectonicRegime> regimes = fetch.getRegimeSet();
			for (OAFTectonicRegime regime : regimes) {
				MagCompPage_Parameters mc_param = fetch.get(regime);
				System.out.println (regime + ": " + mc_param.toString());
				for (int n = 40; n <= 95; n += 5) {
					double mag = ((double)n) / 10.0;
					System.out.println (
						"mag = " + String.format ("%.3f", mag) +
						", magCat = " + String.format ("%.3f", mc_param.get_magCat (mag)) +
						", magSample = " + String.format ("%.3f", mc_param.get_magSample (mag)) +
						", radiusSample = " + String.format ("%.1f", mc_param.get_radiusSample (mag)) +
						", magCentroid = " + String.format ("%.3f", mc_param.get_magCentroid (mag)) +
						", radiusCentroid = " + String.format ("%.1f", mc_param.get_radiusCentroid (mag)));
				}
				System.out.println();
			}

			return;
		}

		// Subcommand : Test #6
		// Command format:
		//  test6
		// List all regimes, and the properties assigned to each.
		// Also print a table of magnitude-dependent parameters for various magnitudes.

		if (args[0].equalsIgnoreCase ("test6")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test6' subcommand");
				return;
			}

			// Display name of each regime
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			Set<String> regime_names = fetch.getRegimeNameSet();

			for (String s : regime_names) {
				System.out.println (s);
			}

			return;
		}

		// Subcommand : Test #7
		// Command format:
		//  test7
		// List all non-null parameter values, as they appear in the file.

		if (args[0].equalsIgnoreCase ("test7")) {

			// No additional arguments

			if (args.length != 1) {
				System.err.println ("MagCompPage_ParametersFetch : Invalid 'test7' subcommand");
				return;
			}

			// Display values of non-null parameters
		
			MagCompPage_ParametersFetch fetch = new MagCompPage_ParametersFetch();
			List<MagCompPage_Parameters> parameter_values = fetch.get_parameter_list();

			int n = 0;
			for (MagCompPage_Parameters value : parameter_values) {
				System.out.println (n + ": " + value.toString());
				++n;
			}

			return;
		}

		// Unrecognized subcommand.

		System.err.println ("MagCompPage_ParametersFetch : Unrecognized subcommand : " + args[0]);
		return;

	}

}
