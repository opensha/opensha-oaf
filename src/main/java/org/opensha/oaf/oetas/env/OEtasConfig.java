package org.opensha.oaf.oetas.env;

import java.util.Set;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.rj.OAFTectonicRegime;


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

			wk_param_set = OEtasConfigFile.unmarshal_config ("EtasConfig.json", OEtasConfig.class);

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


	// Get a copy of the global parameters.

	public final OEtasParameters get_global_parameters () {
		return param_set.get_global_parameters();
	}


	// Get a copy of the regional parameters for the given regime.
	// Return null if regime is null or if there are no regional parameters for the regime.

	public final OEtasParameters get_regional_parameters (OAFTectonicRegime regime) {
		return param_set.get_regional_parameters (regime);
	}


	// Get a copy of the regional parameters for the given regime name.
	// Return null if regime_name is null or if there are no regional parameters for the regime.

	public final OEtasParameters get_regional_parameters (String regime_name) {
		return param_set.get_regional_parameters (regime_name);
	}


	// Get resolved parameters for the given regime.
	// Parameters:
	//  regime = Tectonic regime, can be null to omit regional parameters.
	//  analyst_params = Analyst-supplied parameters, can be null if none.
	// Return a newly-allocated parameter structure.
	// This function starts with default (typical) parameters, then merges global parameters,
	// then merges regional parameters (if any), then merges analyst parameters (if any).
	// Parameters present in later sets of parameters replace those in earlier sets.

	public final OEtasParameters get_resolved_parameters (OAFTectonicRegime regime, OEtasParameters analyst_params) {
		return param_set.get_resolved_parameters (regime, analyst_params);
	}


	// Get resolved parameters for the given regime.
	// Parameters:
	//  regime_name = Name of tectonic regime, can be null to omit regional parameters.
	//  analyst_params = Analyst-supplied parameters, can be null if none.
	// Return a newly-allocated parameter structure.
	// This function starts with default (typical) parameters, then merges global parameters,
	// then merges regional parameters (if any), then merges analyst parameters (if any).
	// Parameters present in later sets of parameters replace those in earlier sets.

	public final OEtasParameters get_resolved_parameters (String regime_name, OEtasParameters analyst_params) {
		return param_set.get_resolved_parameters (regime_name, analyst_params);
	}
	

	// Return a set containing the tectonic regimes.

	public Set<OAFTectonicRegime> getRegimeSet () {
		return param_set.getRegimeSet();
	}
	

	// Return a set containing the names of tectonic regimes, as they appear in the file.

	public Set<String> getRegimeNameSet () {
		return param_set.getRegimeNameSet ();
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
		TestArgs testargs = new TestArgs (args, "TestArgs");




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

			// Display global parameters

			System.out.println ();
			System.out.println ("********** Global parameters **********");
			System.out.println ();

			System.out.println (etas_config.get_global_parameters().toString());

			// Display regional parameters by regime

			System.out.println ();
			System.out.println ("********** Regional/Resolved parameters by regime **********");

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println ();
				System.out.println ();
				System.out.println ("*** Regional, for regime: " + regime.toString());
				System.out.println ();
				System.out.println (etas_config.get_regional_parameters(regime).toString());
				System.out.println ();
				System.out.println ("*** Resolved, for regime: " + regime.toString());
				System.out.println ();
				System.out.println (etas_config.get_resolved_parameters(regime, null).toString());
			}

			// Display regional parameters by name

			System.out.println ();
			System.out.println ("********** Regional/Resolved parameters by name **********");

			for (String name : my_regime_name_set) {
				System.out.println ();
				System.out.println ();
				System.out.println ("*** Regional, for name: " + name);
				System.out.println ();
				System.out.println (etas_config.get_regional_parameters(name).toString());
				System.out.println ();
				System.out.println ("*** Resolved, for name: " + name);
				System.out.println ();
				System.out.println (etas_config.get_resolved_parameters(name, null).toString());
			}

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

			// Display global parameters

			System.out.println ();
			System.out.println ("********** Global parameters **********");
			System.out.println ();

			System.out.println (etas_config.get_global_parameters().toString());

			// Display regional parameters by regime

			System.out.println ();
			System.out.println ("********** Regional/Resolved parameters by regime **********");

			for (OAFTectonicRegime regime : my_regime_set) {
				System.out.println ();
				System.out.println ();
				System.out.println ("*** Regional, for regime: " + regime.toString());
				System.out.println ();
				System.out.println (etas_config.get_regional_parameters(regime).toString());
				System.out.println ();
				System.out.println ("*** Resolved, for regime: " + regime.toString());
				System.out.println ();
				System.out.println (etas_config.get_resolved_parameters(regime, null).toString());
			}

			// Display regional parameters by name

			System.out.println ();
			System.out.println ("********** Regional/Resolved parameters by name **********");

			for (String name : my_regime_name_set) {
				System.out.println ();
				System.out.println ();
				System.out.println ("*** Regional, for name: " + name);
				System.out.println ();
				System.out.println (etas_config.get_regional_parameters(name).toString());
				System.out.println ();
				System.out.println ("*** Resolved, for name: " + name);
				System.out.println ();
				System.out.println (etas_config.get_resolved_parameters(name, null).toString());
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
