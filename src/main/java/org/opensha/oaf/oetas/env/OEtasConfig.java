package org.opensha.oaf.oetas.env;

import java.util.Set;
import java.util.List;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.OAFRegimeParams;
import org.opensha.oaf.rj.OAFRegion;
import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.commons.geo.Location;


// Configuration file for operational ETAS.
// Author: Michael Barall 05/21/2022.
//
// To use, create an object of this class, and then call its methods to obtain configuration parameters.
//
// Parameters come from a configuration file, in the format of OEtasConfigFile.

public class OEtasConfig {

	//----- Parameter set -----

	// Cached parameter set.

	private static OEtasConfigFile cached_param_set = null;

	// Parameter set.

	private OEtasConfigFile param_set;

	// Get the parameter set.

	private static synchronized OEtasConfigFile get_param_set () {

		// If we have a cached parameter set, return it

		if (cached_param_set != null) {
			return cached_param_set;
		}

		// Working data

		OEtasConfigFile wk_param_set = null;

		// Any error reading the parameters aborts the program

		try {

			// Read the configuation file

			wk_param_set = (new OEtasConfigFile()).unmarshal_config (OEtasConfigFile.OE_CONFIG_FILENAME, OEtasConfig.class);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("OEtasConfig: Error loading parameter file EtasConfig.json, unable to continue");
			System.exit(0);
			//throw new RuntimeException("OEtasConfig: Error loading parameter file EtasConfig.json", e);
		}

		// Save the parameter set

		cached_param_set = wk_param_set;
		return cached_param_set;
	}

	// unload_data - Remove the cached data from memory.
	// The data will be reloaded the next time one of these objects is created.
	// Any existing objects will continue to use the old data.
	// This makes it possible to load new parameter values without restarting the program.

	public static synchronized void unload_data () {
		cached_param_set = null;
		return;
	}

	// cache_sample_data - Create a sample file and cache it.
	// This can be used during development to avoid reading an actual config file.

	public static synchronized void cache_sample_data () {
		cached_param_set = (new OEtasConfigFile()).set_to_sample();
		return;
	}


	//----- Construction -----

	// Default constructor.

	public OEtasConfig () {
		param_set = get_param_set ();
	}

	// Display our contents

	@Override
	public String toString() {
		return "OEtasConfig:\n" + param_set.toString();
	}




	//----- Parameter access -----
	



	// Find the resolved parameters for the given location.
	// If the location is null, return default parameters.
	// The returned OAFRegimeParams is newly-allocated.
	// It is guaranteed that the returned OAFRegimeParams contains regime and parameters,
	// and the contained OEtasParameters are newly-allocated.

	public OAFRegimeParams<OEtasParameters> get_resolved_params (Location loc, OEtasParameters analyst_params) {
		return param_set.get_resolved_params (loc, analyst_params);
	}




	// Find the resolved parameters for the given location, and return the result as a string.
	// This function is primarily for testing.

	public String get_resolved_params_as_string (Location loc, OEtasParameters analyst_params) {
		return param_set.regime_params_to_string (get_resolved_params (loc, analyst_params));
	}
	



	// Return a set containing the tectonic regimes.

	public Set<OAFTectonicRegime> getRegimeSet () {
		return param_set.getRegimeSet();
	}
	

	// Return a set containing the names of tectonic regimes, as they appear in the file.

	public Set<String> getRegimeNameSet () {
		return param_set.getRegimeNameSet ();
	}


	// Return a read-only view of the list of regions in the file.

	public List<OAFRegion> get_region_list () {
		return param_set.get_region_list();
	}


	// Get the list of non-null parameters.
	// Note: The caller must not modify the list or any of the parameters.
	// The intended use is to iterate over the list of parmaeters.

	public final List<OEtasParameters> get_parameter_list () {
		return param_set.get_parameter_list();
	}




	//----- Parameter modification -----




	// Get the internal ETAS configuration file.
	// Note: This is provided so that command-line code can adjust parameters.
	// Note: Calling unload_data will revert all parameters to the values in
	// the configuration file.

	public OEtasConfigFile get_etas_config_file () {
		return param_set;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEtasConfig");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Create an object, and display the parameters.

		if (testargs.is_test ("test1")) {

			// Zero additional argument

			testargs.end_test();

			// Create a configuration object

			OEtasConfig etas_config = new OEtasConfig();

			// Display it

			System.out.println ();
			System.out.println ("********** ETAS configuration **********");
			System.out.println ();

			System.out.println (etas_config.toString());

			// Display list of regimes

			Set<OAFTectonicRegime> my_regime_set = etas_config.getRegimeSet();

			System.out.println ();
			System.out.println ("********** Regimes **********");
			System.out.println ();

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println (regime.toString());
			}

			// Display list of regime names

			Set<String> my_regime_name_set = etas_config.getRegimeNameSet();

			System.out.println ();
			System.out.println ("********** Regime names **********");
			System.out.println ();

			for (String name : my_regime_name_set) {
				System.out.println (name);
			}

			// Query by location

			System.out.println ();
			System.out.println ("********** Resolved Query by Location **********");

			List<Location> my_loc_list = OAFParameterSet.getTestLocations();

			for (Location loc : my_loc_list) {
				System.out.println ();
				System.out.println ("Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
				System.out.println (etas_config.get_resolved_params_as_string (loc, null));
			}

			System.out.println ();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Create an object, and display the parameters.
		// Same as test #1, except the cached file is forced to be a sample file
		// (thereby avoiding the need to load an actual configuration file).

		if (testargs.is_test ("test2")) {

			// Zero additional argument

			testargs.end_test();

			// Create a configuration object

			OEtasConfig.cache_sample_data();
			OEtasConfig etas_config = new OEtasConfig();

			// Display it

			System.out.println ();
			System.out.println ("********** ETAS configuration **********");
			System.out.println ();

			System.out.println (etas_config.toString());

			// Display list of regimes

			Set<OAFTectonicRegime> my_regime_set = etas_config.getRegimeSet();

			System.out.println ();
			System.out.println ("********** Regimes **********");
			System.out.println ();

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println (regime.toString());
			}

			// Display list of regime names

			Set<String> my_regime_name_set = etas_config.getRegimeNameSet();

			System.out.println ();
			System.out.println ("********** Regime names **********");
			System.out.println ();

			for (String name : my_regime_name_set) {
				System.out.println (name);
			}

			// Query by location

			System.out.println ();
			System.out.println ("********** Resolved Query by Location **********");

			List<Location> my_loc_list = OAFParameterSet.getTestLocations();

			for (Location loc : my_loc_list) {
				System.out.println ();
				System.out.println ("Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
				System.out.println (etas_config.get_resolved_params_as_string (loc, null));
			}

			System.out.println ();

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Create an object, and display the list of non-null parameters.

		if (testargs.is_test ("test3")) {

			// Zero additional argument

			testargs.end_test();

			// Create a configuration object

			OEtasConfig etas_config = new OEtasConfig();

			// Display parameter values

			System.out.println ();
			System.out.println ("********** Parameter values **********");
			System.out.println ();

			int n = 0;
			for (OEtasParameters value : etas_config.get_parameter_list()) {
				System.out.println (n + ": " + value.toString());
				++n;
			}

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
