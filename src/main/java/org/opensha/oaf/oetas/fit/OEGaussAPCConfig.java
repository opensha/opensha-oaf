package org.opensha.oaf.oetas.fit;

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


// Configuration file for Gaussian a/p/c Bayesian prior parameters for operational ETAS.
// Author: Michael Barall 08/20/2024.
//
// To use, create an object of this class, and then call its methods to obtain configuration parameters.
//
// Parameters come from a configuration file, in the format of OEGaussAPCConfigFile.

public class OEGaussAPCConfig {

	//----- Parameter set -----

	// Cached parameter set.

	private static OEGaussAPCConfigFile cached_param_set = null;

	// Parameter set.

	private OEGaussAPCConfigFile param_set;

	// Get the parameter set.

	private static synchronized OEGaussAPCConfigFile get_param_set () {

		// If we have a cached parameter set, return it

		if (cached_param_set != null) {
			return cached_param_set;
		}

		// Working data

		OEGaussAPCConfigFile wk_param_set = null;

		// Any error reading the parameters aborts the program

		try {

			// Read the configuation file

			wk_param_set = (new OEGaussAPCConfigFile()).unmarshal_config (OEGaussAPCConfigFile.OE_GAUSS_APC_FILENAME, OEGaussAPCConfig.class);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("OEGaussAPCConfig: Error loading parameter file " + OEGaussAPCConfigFile.OE_GAUSS_APC_FILENAME + ", unable to continue");
			System.exit(0);
			//throw new RuntimeException("OEGaussAPCConfig: Error loading parameter file " + OEGaussAPCConfigFile.OE_GAUSS_APC_FILENAME + ", unable to continue", e);
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
		cached_param_set = (new OEGaussAPCConfigFile()).set_to_sample();
		return;
	}


	//----- Construction -----

	// Default constructor.

	public OEGaussAPCConfig () {
		param_set = get_param_set ();
	}

	// Display our contents

	@Override
	public String toString() {
		return "OEGaussAPCConfig:\n" + param_set.toString();
	}




	//----- Parameter access -----


	// Find the parameters for the tectonic regime with the given name.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<OEGaussAPCParams> get_params (String name) {
		return param_set.get_params (name);
	}
	

	// Find the parameters for the given location.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<OEGaussAPCParams> get_params (Location loc) {
		return param_set.get_params (loc);
	}
	

	// Find the default parameters.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<OEGaussAPCParams> get_default_params () {
		return param_set.get_default_params ();
	}




	// Find the parameters for the tectonic regime with the given name,
	// or default parameters if there are none.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.
	// Note: The config file is guaranteed to have a default entry, so this
	// always returns non-null parameters and regime.

	public OAFRegimeParams<OEGaussAPCParams> get_params_or_default (String name) {
		if (!( name == null || name.trim().isEmpty() )) {
			 OAFRegimeParams<OEGaussAPCParams> rp = param_set.get_params (name);
			 if (rp.has_params()) {
				 return rp;
			 }
		}
		return param_set.get_default_params ();
	}




	// Find the parameters for the tectonic regime with the given name,
	// or given locatoin, or default parameters.
	// If name is non-null and non-empty and refers to an entry in the config file,
	// then return those parameters;  otherwise, if loc is non-null then return the
	// parameters for that location;  otherwise, return the default parameters.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.
	// Note: The config file is guaranteed to have a default entry, so this
	// always returns non-null parameters and regime.

	public OAFRegimeParams<OEGaussAPCParams> get_params_or_default (String name, Location loc) {
		if (!( name == null || name.trim().isEmpty() )) {
			 OAFRegimeParams<OEGaussAPCParams> rp = param_set.get_params (name);
			 if (rp.has_params()) {
				 return rp;
			 }
		}
		if (loc != null) {
			 OAFRegimeParams<OEGaussAPCParams> rp = param_set.get_params (loc);
			 if (rp.has_params()) {		// should always be true
				 return rp;
			 }
		}
		return param_set.get_default_params ();
	}




	// Find the parameters for the tectonic regime with the given name, and return the result as a string.
	// This function is primarily for testing.

	public String get_params_as_string (String name) {
		return param_set.get_params_as_string (name);
	}


	// Find the parameters for the given location, and return the result as a string.
	// This function is primarily for testing.

	public String get_params_as_string (Location loc) {
		return param_set.get_params_as_string (loc);
	}


	// Find the default parameters, and return the result as a string.
	// This function is primarily for testing.

	public String get_default_params_as_string () {
		return param_set.get_default_params_as_string ();
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




	//----- Parameter modification -----




	// Get the internal ETAS configuration file.
	// Note: This is provided so that command-line code can adjust parameters.
	// Note: Calling unload_data will revert all parameters to the values in
	// the configuration file.

	public OEGaussAPCConfigFile get_gauss_apc_config_file () {
		return param_set;
	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "TestArgs");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Create an object, and display the parameters.

		if (testargs.is_test ("test1")) {

			// Zero additional argument

			testargs.end_test();

			// Create a configuration object

			OEGaussAPCConfig gauss_apc_config = new OEGaussAPCConfig();

			// Display it

			System.out.println ();
			System.out.println ("********** Gauss a/p/c configuration **********");
			System.out.println ();

			System.out.println (gauss_apc_config.toString());

			// Display list of regimes

			Set<OAFTectonicRegime> my_regime_set = gauss_apc_config.getRegimeSet();

			System.out.println ();
			System.out.println ("********** Regimes **********");
			System.out.println ();

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println (regime.toString());
			}

			// Display list of regime names

			Set<String> my_regime_name_set = gauss_apc_config.getRegimeNameSet();

			System.out.println ();
			System.out.println ("********** Regime names **********");
			System.out.println ();

			for (String name : my_regime_name_set) {
				System.out.println (name);
			}

			// Query by location

			System.out.println ();
			System.out.println ("********** Query by Location **********");

			List<Location> my_loc_list = OAFParameterSet.getTestLocations();

			for (Location loc : my_loc_list) {
				System.out.println ();
				System.out.println ("********** Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
				System.out.println (gauss_apc_config.get_params_as_string (loc));
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

			OEGaussAPCConfig.cache_sample_data();
			OEGaussAPCConfig gauss_apc_config = new OEGaussAPCConfig();

			// Display it

			System.out.println ();
			System.out.println ("********** Gauss a/p/c configuration **********");
			System.out.println ();

			System.out.println (gauss_apc_config.toString());

			// Display list of regimes

			Set<OAFTectonicRegime> my_regime_set = gauss_apc_config.getRegimeSet();

			System.out.println ();
			System.out.println ("********** Regimes **********");
			System.out.println ();

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println (regime.toString());
			}

			// Display list of regime names

			Set<String> my_regime_name_set = gauss_apc_config.getRegimeNameSet();

			System.out.println ();
			System.out.println ("********** Regime names **********");
			System.out.println ();

			for (String name : my_regime_name_set) {
				System.out.println (name);
			}

			// Query by location

			System.out.println ();
			System.out.println ("********** Query by Location **********");

			List<Location> my_loc_list = OAFParameterSet.getTestLocations();

			for (Location loc : my_loc_list) {
				System.out.println ();
				System.out.println ("********** Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
				System.out.println (gauss_apc_config.get_params_as_string (loc));
			}

			System.out.println ();

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
