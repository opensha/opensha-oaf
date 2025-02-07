package org.opensha.oaf.oetas.bay;

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


// Configuration file for mixed rel-zams/n/p/c Bayesian prior parameters for operational ETAS.
// Author: Michael Barall.
//
// To use, create an object of this class, and then call its methods to obtain configuration parameters.
//
// Parameters come from a configuration file, in the format of OEMixedRNPCConfigFile.

public class OEMixedRNPCConfig {

	//----- Parameter set -----

	// Cached parameter set.

	private static OEMixedRNPCConfigFile cached_param_set = null;

	// Parameter set.

	private OEMixedRNPCConfigFile param_set;

	// Get the parameter set.

	private static synchronized OEMixedRNPCConfigFile get_param_set () {

		// If we have a cached parameter set, return it

		if (cached_param_set != null) {
			return cached_param_set;
		}

		// Working data

		OEMixedRNPCConfigFile wk_param_set = null;

		// Any error reading the parameters aborts the program

		try {

			// Read the configuation file

			wk_param_set = (new OEMixedRNPCConfigFile()).unmarshal_config (OEMixedRNPCConfigFile.OE_MIXED_RNPC_FILENAME, OEMixedRNPCConfig.class);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("OEMixedRNPCConfig: Error loading parameter file " + OEMixedRNPCConfigFile.OE_MIXED_RNPC_FILENAME + ", unable to continue");
			System.exit(0);
			//throw new RuntimeException("OEMixedRNPCConfig: Error loading parameter file " + OEMixedRNPCConfigFile.OE_MIXED_RNPC_FILENAME + ", unable to continue", e);
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
		cached_param_set = (new OEMixedRNPCConfigFile()).set_to_sample();
		return;
	}


	//----- Construction -----

	// Default constructor.

	public OEMixedRNPCConfig () {
		param_set = get_param_set ();
	}

	// Display our contents

	@Override
	public String toString() {
		return "OEMixedRNPCConfig:\n" + param_set.toString();
	}




	//----- Parameter access -----


	// Find the parameters for the tectonic regime with the given name.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<OEMixedRNPCParams> get_params (String name) {
		return param_set.get_params (name);
	}
	

	// Find the parameters for the given location.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<OEMixedRNPCParams> get_params (Location loc) {
		return param_set.get_params (loc);
	}
	

	// Find the default parameters.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	public OAFRegimeParams<OEMixedRNPCParams> get_default_params () {
		return param_set.get_default_params ();
	}




	// Find the parameters for the tectonic regime with the given name,
	// or default parameters if there are none.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.
	// Note: The config file is guaranteed to have a default entry, so this
	// always returns non-null parameters and regime.

	public OAFRegimeParams<OEMixedRNPCParams> get_params_or_default (String name) {
		if (!( name == null || name.trim().isEmpty() )) {
			 OAFRegimeParams<OEMixedRNPCParams> rp = param_set.get_params (name);
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

	public OAFRegimeParams<OEMixedRNPCParams> get_params_or_default (String name, Location loc) {
		if (!( name == null || name.trim().isEmpty() )) {
			 OAFRegimeParams<OEMixedRNPCParams> rp = param_set.get_params (name);
			 if (rp.has_params()) {
				 return rp;
			 }
		}
		if (loc != null) {
			 OAFRegimeParams<OEMixedRNPCParams> rp = param_set.get_params (loc);
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


	// Get the list of non-null parameters.
	// Note: The caller must not modify the list or any of the parameters.
	// The intended use is to iterate over the list of parmaeters.

	public final List<OEMixedRNPCParams> get_parameter_list () {
		return param_set.get_parameter_list();
	}




	//----- Parameter modification -----




	// Get the internal ETAS configuration file.
	// Note: This is provided so that command-line code can adjust parameters.
	// Note: Calling unload_data will revert all parameters to the values in
	// the configuration file.

	public OEMixedRNPCConfigFile get_mixed_rnpc_config_file () {
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

			OEMixedRNPCConfig mixed_rnpc_config = new OEMixedRNPCConfig();

			// Display it

			System.out.println ();
			System.out.println ("********** Mixed rel-zams/n/p/c configuration **********");
			System.out.println ();

			System.out.println (mixed_rnpc_config.toString());

			// Display list of regimes

			Set<OAFTectonicRegime> my_regime_set = mixed_rnpc_config.getRegimeSet();

			System.out.println ();
			System.out.println ("********** Regimes **********");
			System.out.println ();

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println (regime.toString());
			}

			// Display list of regime names

			Set<String> my_regime_name_set = mixed_rnpc_config.getRegimeNameSet();

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
				System.out.println (mixed_rnpc_config.get_params_as_string (loc));
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

			OEMixedRNPCConfig.cache_sample_data();
			OEMixedRNPCConfig mixed_rnpc_config = new OEMixedRNPCConfig();

			// Display it

			System.out.println ();
			System.out.println ("********** Mixed rel-zams/n/p/c configuration **********");
			System.out.println ();

			System.out.println (mixed_rnpc_config.toString());

			// Display list of regimes

			Set<OAFTectonicRegime> my_regime_set = mixed_rnpc_config.getRegimeSet();

			System.out.println ();
			System.out.println ("********** Regimes **********");
			System.out.println ();

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println (regime.toString());
			}

			// Display list of regime names

			Set<String> my_regime_name_set = mixed_rnpc_config.getRegimeNameSet();

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
				System.out.println (mixed_rnpc_config.get_params_as_string (loc));
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

			OEMixedRNPCConfig mixed_rnpc_config = new OEMixedRNPCConfig();

			// Display parameter values

			System.out.println ();
			System.out.println ("********** Parameter values **********");
			System.out.println ();

			int n = 0;
			for (OEMixedRNPCParams value : mixed_rnpc_config.get_parameter_list()) {
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
