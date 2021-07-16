package org.opensha.oaf.util;

import java.util.Map;
import java.util.HashMap;

import org.opensha.oaf.aafs.ForecastMainshock;


// GUI event ID alias dictionary.
// Michael Barall 04/23/2021
//
// Contains a dictionary of alias names that can be entered in the GUI
// in place of the event ID.
// Alias names are not case-sensitive, and spaces within the alias name
// are ignored.
// This list is for convenience only, and is not intended to be definitive.

public class GUIEventAlias {


	// The alias name dictionary.

	private static HashMap<String, String> alias_dict = null;


	// Get the alias dictionary.
	// Build the alias dictionary if it is not built yet.

	private static synchronized HashMap<String, String> get_alias_dict () {
		HashMap<String, String> dict = alias_dict;
		if (dict == null) {
			dict = build_alias_dict();
			alias_dict = dict;
		}
		return dict;
	}


	// Build the alias dictionary.

	private static HashMap<String, String> build_alias_dict () {
		HashMap<String, String> dict = new HashMap<String, String>();

		dict.put ("aceh", "official20120411083836720_20");	// M 8.6 - off the west coast of northern Sumatra 2012-04-11 08:38:36 (UTC) 2.327N 93.063E 20.0 km depth
		dict.put ("anchorage", "ak018fcnsk91");	// M 7.1 - 14km NNW of Anchorage, Alaska 2018-11-30 17:29:29 (UTC) 61.346N 149.955W 46.7 km depth
		dict.put ("antelopevalley", "nc73584926");	// M 6.0 - Antelope Valley, CA 2021-07-08 22:49:48 (UTC) 38.508N 119.500W 7.5 km depth
		dict.put ("challis", "us70008jr5");	// M 6.5 - 70km W of Challis, Idaho 2020-03-31 23:52:30 (UTC) 44.465N 115.118W 12.1 km depth
		dict.put ("elmayor", "ci14607652");	// M 7.2 - 12km SW of Delta, B.C., MX 2010-04-04 22:40:42 (UTC) 32.286N 115.295W 10.0 km depth
		dict.put ("haiti", "usp000h60h");	// M 7.0 - Haiti region 2010-01-12 21:53:10 (UTC) 18.443N 72.571W 13.0 km depth
		dict.put ("lonepine", "ci39493944");	// M 5.8 - 18km SSE of Lone Pine, CA 2020-06-24 17:40:49 (UTC) 36.447N 117.975W 4.7 km depth
		dict.put ("magna", "uu60363602");	// M 5.7 - 4km NNW of Magna, Utah 2020-03-18 13:09:31 (UTC) 40.751N 112.078W 11.9 km depth
		dict.put ("maule", "official20100227063411530_30");	// M 8.8 - offshore Bio-Bio, Chile 2010-02-27 06:34:11 (UTC) 36.122S 72.898W 22.9 km depth
		dict.put ("montecristo", "nn00725272");	// M 6.5 - Monte Cristo Range, NV Earthquake 2020-05-15 11:03:27 (UTC) 38.169N 117.850W 2.7 km depth
		dict.put ("montecristorange", "nn00725272");	// M 6.5 - Monte Cristo Range, NV Earthquake 2020-05-15 11:03:27 (UTC) 38.169N 117.850W 2.7 km depth
		dict.put ("mineral", "se609212");	// M 5.8 - 14km SSE of Louisa, Virginia 2011-08-23 17:51:04 (UTC) 37.910N 77.936W 6.0 km depth
		dict.put ("nepal", "us20002926");	// M 7.8 - 36km E of Khudi, Nepal 2015-04-25 06:11:25 (UTC) 28.231N 84.731E 8.2 km depth
		dict.put ("ridgecrest", "ci38457511");	// M 7.1 - 2019 Ridgecrest Earthquake Sequence 2019-07-06 03:19:53 (UTC) 35.770N 117.599W 8.0 km depth
		dict.put ("ridgecrest6", "ci38443183");	// M 6.4 - Ridgecrest Earthquake Sequence 2019-07-04 17:33:49 (UTC) 35.705N 117.504W 10.5 km depth
		dict.put ("ridgecrest7", "ci38457511");	// M 7.1 - 2019 Ridgecrest Earthquake Sequence 2019-07-06 03:19:53 (UTC) 35.770N 117.599W 8.0 km depth
		dict.put ("searlesvalley", "ci39462536");	// M 5.5 - 17km S of Searles Valley, CA 2020-06-04 01:32:11 (UTC) 35.615N 117.428W 8.4 km depth
		dict.put ("sichuan", "usp000g650");	// M 7.9 - eastern Sichuan, China 2008-05-12 06:28:01 (UTC) 31.002N 103.322E 19.0 km depth
		dict.put ("southnapa", "nc72282711");	// M 6.0 - South Napa 2014-08-24 10:20:44 (UTC) 38.215N 122.312W 11.1 km depth
		dict.put ("sumatra", "official20041226005853450_30");	// M 9.1 - 2004 Sumatra - Andaman Islands Earthquake 2004-12-26 00:58:53 (UTC) 3.295N 95.982E 30.0 km depth
		dict.put ("tohoku", "official20110311054624120_30");	// M 9.1 - 2011 Great Tohoku Earthquake, Japan 2011-03-11 05:46:24 (UTC) 38.297N 142.373E 29.0 km depth

		return dict;
	}


	// Query the alias dictionary.
	// Return the event ID, or null if the alias is not found.
	// The name is not case-sensitive.
	// White space within the name is ignored.

	public static String query_alias_dict (String name) {
		HashMap<String, String> dict = get_alias_dict();
		String query = name.replaceAll("\\s", "").toLowerCase();
		String alias = dict.get (query);
		return alias;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("GUIEventAlias : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  alias_name
		// Translate alias_name to an event ID.
		// If event ID is found, fetch event info from Comcat and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 additional argument

			if (args.length != 2) {
				System.err.println ("GUIEventAlias : Invalid 'test1' subcommand");
				return;
			}

			try {

				String alias_name = args[1];

				// Say hello

				System.out.println ("Translating alias name to event ID");
				System.out.println ("alias_name = " + alias_name);

				// Get the event ID

				String event_id = GUIEventAlias.query_alias_dict (alias_name);

				if (event_id == null) {
					System.out.println ();
					System.out.println ("Event ID not found");
				}

				// Found the event ID

				else {
					System.out.println ();
					System.out.println ("Event ID = " + event_id);

					// Fetch the event from Comcat

					ForecastMainshock fcmain = new ForecastMainshock();
					fcmain.setup_mainshock_only (event_id);

					System.out.println ();
					System.out.println (fcmain.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("GUIEventAlias : Unrecognized subcommand : " + args[0]);
		return;

	}

}
