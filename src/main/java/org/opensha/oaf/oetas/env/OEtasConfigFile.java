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
import org.opensha.oaf.rj.GenericRJ_ParametersFetch;


// Configuration file for operational ETAS.
// Author: Michael Barall 05/21/2022.
//
// All fields are public, since this is just a buffer for reading and writing files.
//
// JSON file format:
//
//	"OEtasConfigFile" = Integer giving file version number, currently 122001.
//  "global_params" = {
//      elements = OEtasParameters
//  }
//  "regional_params" = [
//      element = {
//          "regimes" = [
//              element = String giving name of tectonic regime
//          ]
//          "parameters" = {
//              elements = OEtasParameters
//          }
//      }
//  ]

public class OEtasConfigFile implements Marshalable {




	//----- Nested class for regional parameters -----




	// Class to hold parameters and a list of regimes in which they apply.

	public static class RegimeParams {

		// The regimes that apply to this set of parameters.

		public String[] regimes;

		// The parameters.

		public OEtasParameters parameters;

		// Set the parameters and regimes.

		public final RegimeParams set (OEtasParameters parameters, String... regimes) {
			this.parameters = parameters;
			this.regimes = new String[regimes.length];
			for (int j = 0; j < regimes.length; ++j) {
				this.regimes[j] = regimes[j];
			}
			return this;
		}

		// Display the contents

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append ("RegimeParams:" + "\n");

			result.append ("regimes = [" + "\n");
			for (int i = 0; i < regimes.length; ++i) {
				result.append (i + ": " + regimes[i] + "\n");
			}
			result.append ("]" + "\n");

			result.append ("parameters = {" + parameters.toString() + "}" + "\n");

			return result.toString();
		}

		// Check that values are valid, throw an exception if not.
		// If f_check_params is true, also check the invariant of the parameters (not necessary if params have just been unmarshaled).

		public final void check_invariant (boolean f_check_params) {

			if (!( regimes != null && regimes.length > 0 )) {
				throw new InvariantViolationException ("OEtasConfigFile.RegimeParams: Missing regime name list");
			}
			for (String s : regimes) {
				if (!( s != null && s.length() > 0 )) {
					throw new InvariantViolationException ("OEtasConfigFile.RegimeParams: Null or empty regime name");
				}
			}

			if (!( parameters != null )) {
				throw new InvariantViolationException ("OEtasConfigFile.RegimeParams: Missing regional parameters");
			}
			if (f_check_params) {
				String inv = parameters.check_invariant();
				if (inv != null) {
					throw new InvariantViolationException ("OEtasConfigFile.RegimeParams: Invalid regional parameters: " + inv);
				}
			}

			return;
		}

		// Marshal parameters and regimes.

		public void marshal (MarshalWriter writer, String name) {

			// Begin the JSON object

			writer.marshalMapBegin (name);

			// Write the regimes

			writer.marshalStringArray ("regimes", regimes);

			// Write the parameters

			OEtasParameters.static_marshal (writer, "parameters", parameters);

			// End the JSON object

			writer.marshalMapEnd ();
			return;
		}

		// Unmarshal parameters and regimes.

		public RegimeParams unmarshal (MarshalReader reader, String name) {

			// Begin the JSON object

			reader.unmarshalMapBegin (name);

			// Get the regimes

			regimes = reader.unmarshalStringArray ("regimes");

			// Get the parameters

			parameters = OEtasParameters.static_unmarshal (reader, "parameters");

			// End the JSON object

			reader.unmarshalMapEnd ();
			return this;
		}
	}

	// Marshal a list of regimes and parameters.

	public static void marshal_regime_params_list (MarshalWriter writer, String name, List<RegimeParams> regime_params_list) {
		int n = regime_params_list.size();
		writer.marshalArrayBegin (name, n);
		for (RegimeParams regime_params : regime_params_list) {
			regime_params.marshal (writer, null);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal a list of regimes and parameters.

	public static ArrayList<RegimeParams> unmarshal_regime_params_list (MarshalReader reader, String name) {
		ArrayList<RegimeParams> regime_params_list = new ArrayList<RegimeParams>();
		int n = reader.unmarshalArrayBegin (name);
		for (int i = 0; i < n; ++i) {
			regime_params_list.add ((new RegimeParams()).unmarshal (reader, null));
		}
		reader.unmarshalArrayEnd ();
		return regime_params_list;
	}




	//----- Parameter values -----




	// Operational ETAS global parameters.

	private OEtasParameters global_params = null;

	// List of operational ETAS regional parameters.

	private ArrayList<RegimeParams> regional_params = null;

	// Map of regimes to parameters.

	private LinkedHashMap<OAFTectonicRegime, RegimeParams> regime_to_params = null;

	// The regime names as they appear in the file.

	private LinkedHashSet<String> regime_names = null;




	//----- Access -----




	// Get a copy of the global parameters.

	public final OEtasParameters get_global_parameters () {
		OEtasParameters etas_params = (new OEtasParameters()).copy_from (global_params);
		return etas_params;
	}




	// Get a copy of the regional parameters for the given regime.
	// Return null if regime is null or if there are no regional parameters for the regime.

	public final OEtasParameters get_regional_parameters (OAFTectonicRegime regime) {
		OEtasParameters etas_params = null;
		if (regime != null) {
			RegimeParams regime_params = regime_to_params.get (regime);
			if (regime_params != null) {
				etas_params = (new OEtasParameters()).copy_from (regime_params.parameters);
			}
		}
		return etas_params;
	}




	// Get a copy of the regional parameters for the given regime name.
	// Return null if regime_name is null or if there are no regional parameters for the regime.

	public final OEtasParameters get_regional_parameters (String regime_name) {
		OAFTectonicRegime regime = null;
		if (regime_name != null) {
			regime = OAFTectonicRegime.forExistingName (regime_name);	// can return null
		}
		return get_regional_parameters (regime);
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
		OEtasParameters etas_params = (new OEtasParameters()).set_to_typical();
		etas_params.merge_from (global_params);
		if (regime != null) {
			RegimeParams regime_params = regime_to_params.get (regime);
			if (regime_params != null) {
				etas_params.merge_from (regime_params.parameters);
			}
		}
		if (analyst_params != null) {
			etas_params.merge_from (analyst_params);
		}
		return etas_params;
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
		OAFTectonicRegime regime = null;
		if (regime_name != null) {
			regime = OAFTectonicRegime.forExistingName (regime_name);	// can return null
		}
		return get_resolved_parameters (regime, analyst_params);
	}
	



	// Return a set containing the tectonic regimes.

	public Set<OAFTectonicRegime> getRegimeSet() {
		return regime_to_params.keySet();
	}
	



	// Return a set containing the names of tectonic regimes, as they appear in the file.

	public Set<String> getRegimeNameSet() {
		return regime_names;
	}




	//----- Construction -----




	// Clear contents.

	public final void clear () {
		global_params = null;
		regional_params = null;
		regime_to_params = null;
		regime_names = null;
		return;
	}




	// Default constructor.

	public OEtasConfigFile () {
		clear();
	}




	// Make the map of regimes to parameters.
	// The global_params and regional_params must already be set up.

	public final void make_regime_to_params () {
		regime_to_params = new LinkedHashMap<OAFTectonicRegime, RegimeParams>();
		regime_names = new LinkedHashSet<String>();

		// Scan the list of regional parameters

		for (RegimeParams regime_params : regional_params) {

			// Scan the list of regime names

			for (String regime_name : regime_params.regimes) {

				// Get the regime with this name

				OAFTectonicRegime regime = OAFTectonicRegime.forName (regime_name);

				// Add to the mapping

				if (regime_to_params.put (regime, regime_params) != null) {
					throw new InvariantViolationException ("OEtasConfigFile.make_regime_to_params: Duplicate tectonic regime: \"" + regime_name + "\" => \"" + regime.toString() + "\"");
				}

				regime_names.add (regime_name);
			}
		}

		return;
	}




	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append ("OEtasConfigFile:" + "\n");

		result.append ("global_params = {" + global_params.toString() + "}" + "\n");

		result.append ("regional_params = [" + "\n");
		for (int i = 0; i < regional_params.size(); ++i) {
			result.append (i + ": {" + regional_params.get(i).toString() + "}" + "\n");
		}
		result.append ("]" + "\n");

		return result.toString();
	}




	// Check that values are valid, throw an exception if not.
	// If f_check_params is true, also check the invariant of the parameters (not necessary if params have just been unmarshaled).

	public void check_invariant (boolean f_check_params) {

		if (!( global_params != null )) {
			throw new InvariantViolationException ("OEtasConfigFile: Missing global parameters");
		}
		if (f_check_params) {
			String inv = global_params.check_invariant();
			if (inv != null) {
				throw new InvariantViolationException ("OEtasConfigFile: Invalid global parameters: " + inv);
			}
		}

		if (!( regional_params != null && regional_params.size() > 0 )) {
			throw new InvariantViolationException ("OEtasConfigFile: Missing regional parameter list");
		}
		for (RegimeParams regime_params : regional_params) {
			regime_params.check_invariant (f_check_params);
		}

		if (!( regime_to_params != null )) {
			throw new InvariantViolationException ("OEtasConfigFile: Missing regime to parameter mapping");
		}

		if (!( regime_names != null )) {
			throw new InvariantViolationException ("OEtasConfigFile: Missing regime name list");
		}

		return;
	}




	// Set to sample values.
	// The global parameters are set to typical (default) values.
	// The regional parameters have empty values, and there is one for each R&J regime.

	public OEtasConfigFile set_to_sample () {

		// Get the list of regime names to use

		GenericRJ_ParametersFetch fetch = new GenericRJ_ParametersFetch();
		Set<String> my_regime_names = fetch.getRegimeNameSet();

		// Set global parameters to typical values

		global_params = (new OEtasParameters()).set_to_typical();

		// Add empty regional parameters

		regional_params = new ArrayList<RegimeParams>();

		for (String s : my_regime_names) {
			regional_params.add ((new RegimeParams()).set (new OEtasParameters(), s));	// OEtasParameters constructor produces empty parameters
		}

		// Make the mapping of regimes to parameters

		make_regime_to_params();

		// Check invariant

		check_invariant (true);

		return this;
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 122001;

	private static final String M_VERSION_NAME = "OEtasConfigFile";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Error check

		check_invariant (true);

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			OEtasParameters.static_marshal (writer, "global_params", global_params);
			marshal_regime_params_list (writer, "regional_params", regional_params);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			clear();

			global_params = OEtasParameters.static_unmarshal (reader, "global_params");
			regional_params = unmarshal_regime_params_list (reader, "regional_params");

			make_regime_to_params();	// make the mapping of regimes to parameters

		}
		break;

		}

		// Error check

		check_invariant (false);

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

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEtasConfigFile etas_config) {
		etas_config.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEtasConfigFile static_unmarshal (MarshalReader reader, String name) {
		return (new OEtasConfigFile()).unmarshal (reader, name);
	}

	// Unmarshal object from a configuration file.
	// Parameters:
	//  filename = Name of file (not including a path).
	//  requester = Class that is requesting the file.

	public static OEtasConfigFile unmarshal_config (String filename, Class<?> requester) {
		MarshalReader reader = OAFParameterSet.load_file_as_json (filename, requester);
		return (new OEtasConfigFile()).unmarshal (reader, null);
	}




	//----- Testing -----




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

			OEtasConfigFile etas_config = unmarshal_config ("EtasConfig.json", OEtasConfig.class);

			// Display it

			System.out.println (etas_config.toString());

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

			OEtasConfigFile etas_config = unmarshal_config ("EtasConfig.json", OEtasConfig.class);

			// Display it

			System.out.println (etas_config.toString());

			// Marshal to JSON

			String json_string = MarshalUtils.to_json_string (etas_config);
			System.out.println (MarshalUtils.display_json_string (json_string));

			// Unmarshal from JSON
			
			etas_config = new OEtasConfigFile();
			MarshalUtils.from_json_string (etas_config, json_string);

			System.out.println ("");
			System.out.println (etas_config.toString());

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4
		// Make a sample configuration file, and display it.

		if (testargs.is_test ("test4")) {

			// Zero additional argument

			testargs.end_test();

			// Sample configuration file

			OEtasConfigFile etas_config = (new OEtasConfigFile()).set_to_sample();

			// Display it

			System.out.println (etas_config.toString());

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  filename
		// Make a sample configuration file, and write it to a file.
		// This test writes the raw JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test5")) {

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
			System.out.println (etas_config2.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  filename
		// Make a sample configuration file, and write it to a file.
		// This test writes the formatted JSON.
		// Then it reads back the file and displays it.

		if (testargs.is_test ("test6")) {

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
