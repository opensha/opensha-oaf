package org.opensha.oaf.oetas.env;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;
import org.opensha.oaf.util.InvariantViolationException;

import org.opensha.oaf.rj.OAFTectonicRegime;
import org.opensha.oaf.rj.OAFParameterSet;
import org.opensha.oaf.rj.OAF2ParameterSet;
import org.opensha.oaf.rj.OAFRegimeParams;
import org.opensha.oaf.rj.OAFRegion;
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;

import org.opensha.commons.geo.Location;


// Configuration file for operational ETAS.
// Author: Michael Barall 05/21/2022.
//
// JSON file format:
//
//	{
//	"OEtasConfigFile" = Integer giving file version number, currently 122001.
//  ... embedded OAF2ParameterSet
//	}

public class OEtasConfigFile extends OAF2ParameterSet<OEtasParameters> /* implements Marshalable */ {


	//----- Constants -----


	// The default configuration filename.

	public static final String OE_CONFIG_FILENAME = "EtasConfig.json";




	//----- Overridden methods -----




	// Marshal parameters.

	@Override
	protected void marshal_parameters (MarshalWriter writer, String name, OEtasParameters params) {
		OEtasParameters.static_marshal (writer, name, params);
		return;
	}


	// Unmarshal parameters.

	@Override
	protected OEtasParameters unmarshal_parameters (MarshalReader reader, String name) {
		return OEtasParameters.static_unmarshal (reader, name);
	}


	// Convert parameters to string for display.
	// Default is to use the toString function.
	// Note: Must accept null parameters.

	@Override
	protected String parameters_to_string (OEtasParameters params) {
		if (params == null) {
			return "<null>";
		}
		return params.toString();
	}


	// Return true if the file must contain either a world region or all Garcia regions.
	// Default is to return false.
	// Note: If true, then any location query returns non-null regime.

	@Override
	protected boolean require_full_coverage () {
		return false;
	}


	// Return true if all parameter objects in the file must be non-null.
	// Default is to return false.
	// Note: If true, then any query that returns non-null regime also returns non-null
	// parameters; and the parameters argument to marshal_parameters ia always non-null.
	// Note: If true, and if require_full_coverage is also true, then any location query
	// returns non-null parameters.

	@Override
	protected boolean require_non_null_parameters () {
		return true;
	}


	// Return true if all there must be a default parameter object in the file.
	// Default is to return false.

	@Override
	protected boolean require_default_parameters () {
		return false;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 122001;

	private static final String M_VERSION_NAME = "OEtasConfigFile";




	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Pass thru

		super.do_marshal (writer);
	
		return;
	}




	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Pass thru

		super.do_umarshal (reader);

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
	public OEtasConfigFile unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}




	// Unmarshal object from a configuration file.
	// Parameters:
	//  filename = Name of file (not including a path).
	//  requester = Class that is requesting the file.

	@Override
	public OEtasConfigFile unmarshal_config (String filename, Class<?> requester) {
		OAFParameterSet.unmarshal_file_as_json (this, filename, requester);
		return this;
	}




	//----- Searching -----
	



	// Find the resolved parameters for the given location.
	// The returned OAFRegimeParams is newly-allocated.
	// It is guaranteed that the returned OAFRegimeParams contains regime and parameters,
	// and the contained OEtasParameters are newly-allocated.

	public OAFRegimeParams<OEtasParameters> get_resolved_params (Location loc, OEtasParameters analyst_params) {

		// Start with the built-in defaults, using default regime

		OAFRegimeParams<OEtasParameters> result = new OAFRegimeParams<OEtasParameters> (get_default_regime(), (new OEtasParameters()).set_to_typical());

		// Merge in the default parameters from the file, if we have any

		OAFRegimeParams<OEtasParameters> x = get_default_params ();
		if (x.has_params()) {
			result.params.merge_from (x.params);
			result.regime = x.regime;
		}

		// Merge in parameters for the location, if we have any

		x = get_params (loc);
		if (x.has_params()) {
			result.params.merge_from (x.params);
			result.regime = x.regime;
		}

		// Merge in analyst parameters, if we have any

		if (analyst_params != null) {
			result.params.merge_from (analyst_params);
		}

		return result;
	}




	// Find the resolved parameters for the given location, and return the result as a string.
	// This function is primarily for testing.

	public String get_resolved_params_as_string (Location loc, OEtasParameters analyst_params) {
		return regime_params_to_string (get_resolved_params (loc, analyst_params));
	}




	//----- Construction -----




	// Default constructor.

	public OEtasConfigFile () {
	}




	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append ("OEtasConfigFile:" + "\n");

		result.append (super.toString());

		return result.toString();
	}




	// Set to sample values.
	// The default parameters are set to typical (default) values.
	// The regional parameters have empty values, and are defined for each R&J regime.

	public OEtasConfigFile set_to_sample () {

		// Load the data

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();

		// Add a default selection which contains typical parameters

		add_selection ((new OEtasParameters()).set_to_typical(), OAF2ParameterSet.default_region);

		// Add a selection which contains empty parameters and applies to all regions 
		// Note that the OEtasParameters constructor produces empty parameters

		Set<String> fetch_regime_names = fetch.getRegimeNameSet();
		add_selection (new OEtasParameters(), fetch_regime_names.toArray (new String[0]));

		// Add a region for each region in the file

		List<OAFRegion> fetch_region_list = fetch.get_region_list ();
		for (OAFRegion r : fetch_region_list) {
			add_region (r);
		}

		// Finish the setup

		finish_setup();

		return this;
	}




	// Set to sample values.
	// The parameter is used as the default parameters.
	// The regional parameters have empty values, and are defined for each R&J regime.

	public OEtasConfigFile set_to_sample (OEtasParameters def_params) {

		// Load the data

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();

		// Add a default selection which contains typical parameters

		add_selection (def_params, OAF2ParameterSet.default_region);

		// Add a selection which contains empty parameters and applies to all regions 
		// Note that the OEtasParameters constructor produces empty parameters

		Set<String> fetch_regime_names = fetch.getRegimeNameSet();
		add_selection (new OEtasParameters(), fetch_regime_names.toArray (new String[0]));

		// Add a region for each region in the file

		List<OAFRegion> fetch_region_list = fetch.get_region_list ();
		for (OAFRegion r : fetch_region_list) {
			add_region (r);
		}

		// Finish the setup

		finish_setup();

		return this;
	}




	//----- Testing -----




	// Run a test of resolved searching.
	// Parameters:
	//  pset = Parameter set.
	//  loc_list = List of locations to query, or null to use a default list.
	//  analyst_params = Analyst parameters to apply, or null if none.

	public static void test_resolved_searching (OEtasConfigFile pset, List<Location> loc_list, OEtasParameters analyst_params) {

		// Display parameter set

		System.out.println ();
		System.out.println ("********** Parameter Set **********");
		System.out.println ();

		System.out.println (pset.toString());

		// Query by location

		System.out.println ();
		System.out.println ("********** Resolved Query by Location **********");

		List<Location> my_loc_list = loc_list;
		if (my_loc_list == null) {
			my_loc_list = OAFParameterSet.getTestLocations();
		}

		for (Location loc : my_loc_list) {
			System.out.println ();
			System.out.println ("Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
			System.out.println (pset.get_resolved_params_as_string (loc, analyst_params));
		}

		System.out.println ();

		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "TestArgs");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Unmarshal from the configuration file, and display it.

		if (testargs.is_test ("test1")) {

			// Zero additional argument

			testargs.end_test();

			// Read the configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).unmarshal_config (OE_CONFIG_FILENAME, OEtasConfig.class);

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (etas_config.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Unmarshal from the configuration file, and display it.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled results.

		if (testargs.is_test ("test2")) {

			// Zero additional argument

			testargs.end_test();

			// Read the configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).unmarshal_config (OE_CONFIG_FILENAME, OEtasConfig.class);

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (etas_config.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal **********");
			System.out.println ();

			String json_string = MarshalUtils.to_json_string (etas_config);
			System.out.println (MarshalUtils.display_json_string (json_string));

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal **********");
			System.out.println ();
			
			etas_config = new OEtasConfigFile();
			MarshalUtils.from_json_string (etas_config, json_string);

			System.out.println (etas_config.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Unmarshal from the configuration file, and display it.
		// Then perform a test of superclass searching.

		if (testargs.is_test ("test3")) {

			// Zero additional argument

			testargs.end_test();

			// Read the configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).unmarshal_config (OE_CONFIG_FILENAME, OEtasConfig.class);

			// Test it

			OAF2ParameterSet.test_searching (etas_config, null);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4
		// Unmarshal from the configuration file, and display it.
		// Then perform a test of resolved searching.

		if (testargs.is_test ("test4")) {

			// Zero additional argument

			testargs.end_test();

			// Read the configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).unmarshal_config (OE_CONFIG_FILENAME, OEtasConfig.class);

			// Test it

			test_resolved_searching (etas_config, null, null);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Make a sample configuration file, and display it.

		if (testargs.is_test ("test5")) {

			// Zero additional argument

			testargs.end_test();

			// Sample configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).set_to_sample();

			// Display it

			System.out.println ();
			System.out.println ("********** Sample Parameter Set **********");
			System.out.println ();

			System.out.println (etas_config.toString());

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  filename
		// Make a sample configuration file, and write it to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test6")) {

			// Read arguments

			System.out.println ("Writing sample ETAS configuration file, raw JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Sample configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).set_to_sample();

			// Write to file

			MarshalUtils.to_json_file (etas_config, filename);

			// Read back the file and display it

			OEtasConfigFile etas_config2 = new OEtasConfigFile();
			MarshalUtils.from_json_file (etas_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Sample Parameter Set **********");
			System.out.println ();

			System.out.println (etas_config2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  filename
		// Make a sample configuration file, and write it to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test7")) {

			// Read arguments

			System.out.println ("Writing sample ETAS configuration file, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Sample configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).set_to_sample();

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_config, filename);

			// Read back the file and display it

			OEtasConfigFile etas_config2 = new OEtasConfigFile();
			MarshalUtils.from_json_file (etas_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Sample Parameter Set **********");
			System.out.println ();

			System.out.println ();
			System.out.println (etas_config2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  filename
		// Make a sample configuration file, and write it to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.
		// Same as test #7 except forces the use of a uniform Bayesian prior.

		if (testargs.is_test ("test8")) {

			// Read arguments

			System.out.println ("Writing sample ETAS configuration file, uniform prior, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Default parameters for the configuration file

			OEtasParameters def_params = (new OEtasParameters()).set_to_typical();
			def_params.set_bay_prior_to_typical_uniform();

			// Sample configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).set_to_sample(def_params);

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_config, filename);

			// Read back the file and display it

			OEtasConfigFile etas_config2 = new OEtasConfigFile();
			MarshalUtils.from_json_file (etas_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Sample Parameter Set **********");
			System.out.println ();

			System.out.println ();
			System.out.println (etas_config2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  filename
		// Make a sample configuration file, and write it to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.
		// Same as test #7 except forces the use of a Gauss a/p/c Bayesian prior.

		if (testargs.is_test ("test9")) {

			// Read arguments

			System.out.println ("Writing sample ETAS configuration file, uniform prior, formatted JSON");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Default parameters for the configuration file

			OEtasParameters def_params = (new OEtasParameters()).set_to_typical();
			def_params.set_bay_prior_to_typical_gauss_apc();

			// Sample configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).set_to_sample(def_params);

			// Write to file

			MarshalUtils.to_formatted_json_file (etas_config, filename);

			// Read back the file and display it

			OEtasConfigFile etas_config2 = new OEtasConfigFile();
			MarshalUtils.from_json_file (etas_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Sample Parameter Set **********");
			System.out.println ();

			System.out.println ();
			System.out.println (etas_config2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test8  filename
		// Read the file and displays it.

		if (testargs.is_test ("test10")) {

			// Read arguments

			System.out.println ("Read ETAS configuration file");
			String filename = testargs.get_string ("filename");
			testargs.end_test();

			// Read the file and display it

			OEtasConfigFile etas_config2 = new OEtasConfigFile();
			MarshalUtils.from_json_file (etas_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Parameter Set **********");
			System.out.println ();

			System.out.println ();
			System.out.println (etas_config2.toString());

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
