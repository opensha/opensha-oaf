package org.opensha.oaf.oetas.fit;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

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


// Configuration file for Gaussian a/p/c Bayesian prior parameters for operational ETAS.
// Author: Michael Barall 08/20/2024.
//
// JSON file format:
//
//	{
//	"OEGaussAPCConfigFile" = Integer giving file version number, currently 134001.
//  ... embedded OAF2ParameterSet
//	}

public class OEGaussAPCConfigFile extends OAF2ParameterSet<OEGaussAPCParams> /* implements Marshalable */ {


	//----- Constants -----


	// The default configuration filename.

	public static final String OE_GAUSS_APC_FILENAME = "GaussAPCConfig.json";




	//----- Overridden methods -----




	// Marshal parameters.

	@Override
	protected void marshal_parameters (MarshalWriter writer, String name, OEGaussAPCParams params) {
		OEGaussAPCParams.static_marshal (writer, name, params);
		return;
	}


	// Unmarshal parameters.

	@Override
	protected OEGaussAPCParams unmarshal_parameters (MarshalReader reader, String name) {
		return OEGaussAPCParams.static_unmarshal (reader, name);
	}


	// Convert parameters to string for display.
	// Default is to use the toString function.
	// Note: Must accept null parameters.

	@Override
	protected String parameters_to_string (OEGaussAPCParams params) {
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
		return true;
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
		return true;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 134001;

	private static final String M_VERSION_NAME = "OEGaussAPCConfigFile";




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
	public OEGaussAPCConfigFile unmarshal (MarshalReader reader, String name) {
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
	public OEGaussAPCConfigFile unmarshal_config (String filename, Class<?> requester) {
		OAFParameterSet.unmarshal_file_as_json (this, filename, requester);
		return this;
	}




	//----- Searching -----
	



	// Find the parameters for the given tectonic regime.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	//public OAFRegimeParams<OEGaussAPCParams> get_params (OAFTectonicRegime r) // inherited


	// Find the parameters for the tectonic regime with the given name.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	//public OAFRegimeParams<OEGaussAPCParams> get_params (String name) // inherited
	

	// Find the parameters for the given location.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	//public OAFRegimeParams<OEGaussAPCParams> get_params (Location loc) // inherited
	

	// Find the default parameters.
	// The returned OAFRegimeParams is newly-allocated.
	// The parameter object in OAFRegimeParams is the internal object and
	// should not be modified by the caller.

	//public OAFRegimeParams<OEGaussAPCParams> get_default_params () // inherited


	// Find the parameters for the given tectonic regime, and return the result as a string.
	// This function is primarily for testing.

	//public String get_params_as_string (OAFTectonicRegime r) // inherited


	// Find the parameters for the tectonic regime with the given name, and return the result as a string.
	// This function is primarily for testing.

	//public String get_params_as_string (String name) // inherited


	// Find the parameters for the given location, and return the result as a string.
	// This function is primarily for testing.

	//public String get_params_as_string (Location loc) // inherited


	// Find the default parameters, and return the result as a string.
	// This function is primarily for testing.

	//public String get_default_params_as_string () // inherited




	//----- Construction -----




	// Default constructor.

	public OEGaussAPCConfigFile () {
	}




	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append ("OEGaussAPCConfigFile:" + "\n");

		result.append (super.toString());

		return result.toString();
	}




	// Set to sample values.
	// The selections are the rows from the embedded CSV file.
	// The regions are from the Generic RJ parameter file.
	// If a region has a regime that does not correspond to any row in the CSV file,
	// then it is added to either the California or Global-Average selection,
	// depending on whether its name begins with "CAL".
	// The Global-Average selection is also made the default selection.
	// It is assumed that the CSV file contains entries for all Garcia regions,
	// plus Global-Average and California.

	public OEGaussAPCConfigFile set_to_sample () {

		// Load the data

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();
		List<OAFRegion> fetch_region_list = fetch.get_region_list ();

		// Load the embedded CSV file

		List<OEGaussAPCParams> embedded_csv = OEGaussAPCParams.import_embedded_csv();

		// Make a set containing all tectonic regimes in the CSV file

		Set<OAFTectonicRegime> csv_regimes = new HashSet<OAFTectonicRegime>();
		for (OEGaussAPCParams params : embedded_csv) {
			csv_regimes.add (OAFTectonicRegime.forName (params.get_regimeName()));
		}

		// List of regime names that apply to the global parameters, excluding duplicates

		Set<String> global_regime_names = new LinkedHashSet<String>();
		global_regime_names.add (OEGaussAPCParams.GLOBAL_REGIME);
		global_regime_names.add (default_region);

		// List of regime names that apply to the California parameters, excluding duplicates

		Set<String> california_regime_names = new LinkedHashSet<String>();
		california_regime_names.add (OEGaussAPCParams.CALIFORNIA_REGIME);

		// Scan the regions, and if any don't match a regime in the CSV file,
		// add their names to either the global or California list

		for (OAFRegion r : fetch_region_list) {
			OAFTectonicRegime r_regime = r.get_regime();
			if (!( csv_regimes.contains (r_regime) )) {
				String r_name = r_regime.toString();
				if (r_name.trim().toUpperCase().startsWith ("CAL")) {
					california_regime_names.add (r_name);
				} else {
					global_regime_names.add (r_name);
				}
			}
		}

		// Add a selection for each row in the CSV file

		for (OEGaussAPCParams params : embedded_csv) {
			String s_name = params.get_regimeName();
			if (s_name.equals (OEGaussAPCParams.GLOBAL_REGIME)) {
				add_selection (params, global_regime_names.toArray (new String[0]));
			}
			else if (s_name.equals (OEGaussAPCParams.CALIFORNIA_REGIME)) {
				add_selection (params, california_regime_names.toArray (new String[0]));
			}
			else {
				add_selection (params, s_name);
			}
		}

		// Add a region for each region in the file

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

	public static void test_searching (OEGaussAPCConfigFile pset, List<Location> loc_list) {

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
			System.out.println ("********** Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
			System.out.println (pset.get_params_as_string (loc));
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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).unmarshal_config (OE_GAUSS_APC_FILENAME, OEGaussAPCConfig.class);

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (gauss_apc_config.toString());

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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).unmarshal_config (OE_GAUSS_APC_FILENAME, OEGaussAPCConfig.class);

			// Display it

			System.out.println ();
			System.out.println ("********** Parameter Set **********");
			System.out.println ();

			System.out.println (gauss_apc_config.toString());

			// Marshal to JSON

			System.out.println ();
			System.out.println ("********** Marshal **********");
			System.out.println ();

			//String json_string = MarshalUtils.to_json_string (gauss_apc_config);
			//System.out.println (MarshalUtils.display_json_string (json_string));

			String json_string = MarshalUtils.to_formatted_compact_json_string (gauss_apc_config);
			System.out.println (json_string);

			// Unmarshal from JSON

			System.out.println ();
			System.out.println ("********** Unmarshal **********");
			System.out.println ();
			
			gauss_apc_config = new OEGaussAPCConfigFile();
			MarshalUtils.from_json_string (gauss_apc_config, json_string);

			System.out.println (gauss_apc_config.toString());

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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).unmarshal_config (OE_GAUSS_APC_FILENAME, OEGaussAPCConfig.class);

			// Test it

			OAF2ParameterSet.test_searching (gauss_apc_config, null);

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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).unmarshal_config (OE_GAUSS_APC_FILENAME, OEGaussAPCConfig.class);

			// Test it

			test_searching (gauss_apc_config, null);

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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).set_to_sample();

			// Display it

			System.out.println ();
			System.out.println ("********** Sample Parameter Set **********");
			System.out.println ();

			System.out.println (gauss_apc_config.toString());

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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).set_to_sample();

			// Write to file

			MarshalUtils.to_json_file (gauss_apc_config, filename);

			// Read back the file and display it

			OEGaussAPCConfigFile gauss_apc_config2 = new OEGaussAPCConfigFile();
			MarshalUtils.from_json_file (gauss_apc_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Sample Parameter Set **********");
			System.out.println ();

			System.out.println (gauss_apc_config2.toString());

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

			OEGaussAPCConfigFile gauss_apc_config = (new OEGaussAPCConfigFile()).set_to_sample();

			// Write to file

			//MarshalUtils.to_formatted_json_file (gauss_apc_config, filename);
			MarshalUtils.to_formatted_compact_json_file (gauss_apc_config, filename);

			// Read back the file and display it

			OEGaussAPCConfigFile gauss_apc_config2 = new OEGaussAPCConfigFile();
			MarshalUtils.from_json_file (gauss_apc_config2, filename);

			System.out.println ();
			System.out.println ("********** Unmarshaled Sample Parameter Set **********");
			System.out.println ();

			System.out.println ();
			System.out.println (gauss_apc_config2.toString());

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
