package org.opensha.oaf.comcat;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.Comparator;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.regex.Pattern;

import java.io.UnsupportedEncodingException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;
import org.opensha.commons.param.impl.StringParameter;

import org.opensha.oaf.util.SimpleUtils;

/**
 * An entry in a local earthquake catalog.
 * Author: Michael Barall 09/19/2018.
 *
 * All fields are public, since there is little benefit to having lots of getters and setters.
 */
public class ComcatLocalCatalogEntry {

	// Regular expression that defines a valid Comcat id (see java.util.regex.Pattern).

	public static final String REGEX_ID = "\\S+";




	//----- Rupture parameters -----

	// Rupture network.

	public String rup_network = null;

	// Rupture code.

	public String rup_code = null;

	// Rupture id list, with the first element equal to the rupture event id.

	public String[] rup_id_list = null;

	// Rupture place.

	public String rup_place = null;

	// Rupture time, in milliseconds since the epoch.

	public long rup_time = 0L;

	// Rupture magnitude.

	public double rup_mag = 0.0;

	// Rupture latitude, in degrees, from -90 to +90.

	public double rup_lat = 0.0;

	// Rupture longitude, in degrees, from -180 to +180.

	public double rup_lon = 0.0;

	// Rupture depth, in kilometers, positive underground.

	public double rup_depth = 0.0;




	//----- Setup functionss -----


	// Set rupture parameters to default.

	public void set_default_rup_params () {
		rup_network = null;
		rup_code = null;
		rup_id_list = null;
		rup_place = null;
		rup_time = 0L;
		rup_mag = 0.0;
		rup_lat = 0.0;
		rup_lon = 0.0;
		rup_depth = 0.0;
		return;
	}




	// Set rupture parameters from rupture information.
	// The ObsEqkRupture must contain extended information.
	// An exception is thrown if the operation cannot be completed.

	public void set_eqk_rupture (ObsEqkRupture rup) {

		Pattern pattern = Pattern.compile(REGEX_ID);

		String rup_event_id = rup.getEventId();
		if (rup_event_id == null || !(pattern.matcher(rup_event_id).matches())) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: Missing or invalid event id");
		}

		Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);
		rup_network = eimap.get (ComcatOAFAccessor.PARAM_NAME_NETWORK);
		if (rup_network == null || !(pattern.matcher(rup_network).matches())) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: Missing or invalid event network: rup_event_id = " + rup_event_id);
		}
		rup_code = eimap.get (ComcatOAFAccessor.PARAM_NAME_CODE);
		if (rup_code == null || !(pattern.matcher(rup_code).matches())) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: Missing or invalid event code: rup_event_id = " + rup_event_id);
		}

		String comcat_idlist = eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST);
		if (comcat_idlist == null || comcat_idlist.isEmpty()) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: No id list: rup_event_id = " + rup_event_id);
		}
		List<String> idlist = ComcatOAFAccessor.idsToList (comcat_idlist, rup_event_id);
		if (idlist.isEmpty()) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: Empty id list: rup_event_id = " + rup_event_id);
		}
		rup_id_list = idlist.toArray (new String[0]);
		for (String id : rup_id_list) {
			if (id == null || !(pattern.matcher(id).matches())) {
				throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: Missing or invalid Comcat id: rup_event_id = " + rup_event_id);
			}
		}

		rup_place = eimap.get (ComcatOAFAccessor.PARAM_NAME_DESCRIPTION);
		if (rup_place == null || rup_place.isEmpty()) {
			rup_place = "Unknown";
		}

		rup_time = rup.getOriginTime();
		rup_mag = rup.getMag();
		Location hypo = rup.getHypocenterLocation();
		rup_lat = hypo.getLatitude();
		rup_lon = hypo.getLongitude();
		rup_depth = hypo.getDepth();

		if (rup_lon > 180.0) {
			rup_lon -= 360.0;
		}
		if (rup_lon < -180.0) {
			rup_lon = 180.0;
		}

		if (rup_lat > 90.0) {
			rup_lat = 90.0;
		}
		else if (rup_lat < -90.0) {
			rup_lat = -90.0;
		}

		if (rup_depth < 0.0) {
			rup_depth = 0.0;
		} else if (rup_depth > 700.0) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.set_eqk_rupture: Depth too large: rup_event_id = " + rup_event_id + ", rup_depth = " + rup_depth);
		}

		return;
	}

	


	//----- Access functions -----


	// Get the Comcat id list.

	public String get_comcat_idlist () {
		return "," + String.join(",", rup_id_list) + ",";
	}




	// Get location from rupture parameters.

	public Location get_eqk_location () {
		return new Location (rup_lat, rup_lon, rup_depth);
	}




	// Get rupture information from rupture parameters.
	// If wrapLon is false, longitudes range from -180 to +180.
	// If wrapLon is true, longitudes range from 0 to +360.
	// If extendedInfo is true, extended information is added to the ObsEqkRupture,
	//  which presently is the place description, list of ids, network, and event code.

	public ObsEqkRupture get_eqk_rupture (boolean wrapLon, boolean extendedInfo) {

		double lon = rup_lon;
		if (wrapLon && lon < 0.0) {
			lon += 360.0;
		}
		Location hypo = new Location(rup_lat, lon, rup_depth);

		ObsEqkRupture rup = new ObsEqkRupture(rup_id_list[0], rup_time, hypo, rup_mag);
		
		if (extendedInfo) {
			// adds the place description ("10km from wherever")
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_DESCRIPTION, rup_place));
			// adds the event id list, which can be used to resolve duplicates
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_IDLIST, get_comcat_idlist()));
			// adds the seismic network, which is needed for reporting to PDL
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_NETWORK, rup_network));
			// adds the event code, which is needed for reporting to PDL
			rup.addParameter(new StringParameter(ComcatOAFAccessor.PARAM_NAME_CODE, rup_code));
		}
		
		return rup;
	}




	//----- Construction -----


	// Default constructor.

	public ComcatLocalCatalogEntry () {}




	// Copy from another object.

	public void copy_from (ComcatLocalCatalogEntry other) {
		rup_network = other.rup_network;
		rup_code = other.rup_code;
		rup_id_list = ((other.rup_id_list == null) ? (other.rup_id_list) : (other.rup_id_list.clone()));
		rup_place = other.rup_place;
		rup_time = other.rup_time;
		rup_mag = other.rup_mag;
		rup_lat = other.rup_lat;
		rup_lon = other.rup_lon;
		rup_depth = other.rup_depth;
		return;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("ComcatLocalCatalogEntry:" + "\n");
		result.append ("rup_network = " + rup_network + "\n");
		result.append ("rup_code = " + rup_code + "\n");
		result.append ("rup_id_list = " + get_comcat_idlist() + "\n");
		result.append ("rup_place = " + rup_place + "\n");
		result.append ("rup_time = " + SimpleUtils.time_raw_and_string(rup_time) + "\n");
		result.append ("rup_mag = " + rup_mag + "\n");
		result.append ("rup_lat = " + rup_lat + "\n");
		result.append ("rup_lon = " + rup_lon + "\n");
		result.append ("rup_depth = " + rup_depth + "\n");

		return result.toString();
	}




	// Format our contents as a single line.
	// The line is formatted so it can be read back in using parse_line.
	// Implementation note:  Places are URL-encoded so each place is a single token.
	// Comcat IDs need not be URL-encoded because they are checked against a regular
	// expression which ensures they are single tokens.
	// Note: The line does not have a newline at the end.

	public String format_line () {
		StringBuilder result = new StringBuilder();

		result.append (rup_network + " ");
		result.append (rup_code + " ");
		result.append (rup_time + " ");
		result.append (rup_mag + " ");
		result.append (rup_lat + " ");
		result.append (rup_lon + " ");
		result.append (rup_depth + " ");
		int idlen = rup_id_list.length;
		result.append (idlen + " ");
		for (int i = 0; i < idlen; ++i) {
			result.append (rup_id_list[i] + " ");
		}
		try {
			result.append (URLEncoder.encode (rup_place, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.format_line: Invalid encoding");
		}

		return result.toString();
	}




	// Parse our contents from a scanner.
	// An exception is thrown if the operation cannot be completed.

	public void parse_line (Scanner scanner) {

		rup_network = scanner.next();
		rup_code = scanner.next();
		rup_time = scanner.nextLong();
		rup_mag = scanner.nextDouble();
		rup_lat = scanner.nextDouble();
		rup_lon = scanner.nextDouble();
		rup_depth = scanner.nextDouble();
		int idlen = scanner.nextInt();
		if (idlen < 1) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.parse_line: Invalid id count");
		}
		rup_id_list = new String[idlen];
		for (int i = 0; i < idlen; ++i) {
			rup_id_list[i] = scanner.next();
		}
		try {
			rup_place = URLDecoder.decode (scanner.next(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException ("ComcatLocalCatalogEntry.parse_line: Invalid encoding");
		}

		return;
	}




	// Parse our contents from a string.
	// An exception is thrown if the operation cannot be completed.

	public void parse_line (String s) {
		try (
			Scanner scanner = new Scanner (s);
		){
			parse_line (scanner);
			if (scanner.hasNext()) {
				throw new RuntimeException ("ComcatLocalCatalogEntry.parse_line: Extra tokens on line");
			}
		}
		return;
	}




	// Format our contents as a single line for a flat file.
	// Note: The line does not have a newline at the end.
	// The line has 10 fields, separated by a single space.
	// Field 1: year, 4 digits.
	// Field 2: month, 1 or 2 digits, right-justified in 2 columns.
	// Field 3: day of month, 1 or 2 digits, right-justified in 2 columns.
	// Field 4: hour, 1 or 2 digits, right-justified in 2 columns.
	// Field 5: minute, 1 or 2 digits, right-justified in 2 columns.
	// Field 6: second, 1 or 2 digits left of decimal point,
	//          2 digits right of decimal point, right-justified in 5 columns.
	// Field 7: latitude in degrees, north positive, 1 or 2 digits left of decimal point,
	//          3 digits right of decimal point, right-justified in 7 columns.
	// Field 8: longitude in degrees, east positive, 1 to 3 digits left of decimal point,
	//          3 digits right of decimal point, right-justified in 8 columns.
	// Field 9: depth in km, down positive, 1 to 4 digits left of decimal point,
	//          1 digit right of decimal point, right-justified in 6 columns.
	// Field 10: magnitude, 1 or 2 digits left of decimal point (or 1 digit plus sign),
	//           2 digits right of decimal point, right-justified in 5 columns. 

	public String format_flat_line () {

		// Convert the time

		SimpleDateFormat fmt = new SimpleDateFormat ("yyyy MM dd HH mm ss.SSS");
		fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
		String t1 = fmt.format (new Date (rup_time));

		// Remove leading zeros from month, day, hour, minute, second

		String t2 = t1.replace (" 0", "  ");

		// Strip off the last decimal place

		String t3 = t2.substring (0, t2.length() - 1);

		// Format the string

		String s = String.format ("%s %7.3f %8.3f %6.1f %5.2f",
					t3, rup_lat, rup_lon, rup_depth, rup_mag);

		return s;
	}




	//----- Utilities -----


	// Add all our ids to a map.
	// Returns null if none of our ids are already in the map.
	// Returns the duplicate id if one of our ids is already in the map.
	// Note: This version does not modify any entries.

	public String add_ids_to_map_nofix (Map<String, ComcatLocalCatalogEntry> map) {
		for (String id : rup_id_list) {
			if (map.containsKey (id)) {
				return id;
			}
		}
		for (String id : rup_id_list) {
			map.put (id, this);
		}
		return null;
	}




	// Add all our ids to a map.
	// Returns null if the operation is successful.
	// Returns the duplicate id if this entry is not added to the map.
	// Note: This version may modify entries to resolve conflicts.
	// We have observed that on rare occasions Comcat may have the same id
	// refer to two different earthquakes; either primary in one and secondary
	// in the other, or secondary in both.  (We have not observed the same id
	// to be primary in two different earthquakes, and such a case would be
	// filtered in ComcatOAFAccessor anyway.)  [Note: Comcat maintainers have
	// confirmed that this is a bug in Comcat.  When/if the bug is fixed,
	// this will not occur any more.]
	// If this entry has the same primary id as another entry that is
	// already in the map, then this function returns the primary id and
	// this entry is not added to the map.
	// All other conflicts are resolved by deleting a secondary id, either
	// from this entry or from a previously-processed entry.  If there is
	// a choice, it is deleted from this entry.

	public String add_ids_to_map (Map<String, ComcatLocalCatalogEntry> map) {

		// If our primary id is in use ...

		ComcatLocalCatalogEntry other = map.get (rup_id_list[0]);
		if (other != null) {
		
			// If it's the primary id of the other entry, report conflict

			if (rup_id_list[0].equals (other.rup_id_list[0])) {
				return rup_id_list[0];
			}

			// Remove secondary id from the other entry

			other.delete_secondary_id (rup_id_list[0]);
		}

		// Now scan our secondary ids ...

		String[] original_rup_id_list = rup_id_list;
		for (int k = 1; k < original_rup_id_list.length; ++k) {

			// If the id is in use, remove it from this entry
		
			if (map.containsKey (original_rup_id_list[k])) {
				delete_secondary_id (original_rup_id_list[k]);
			}
		}

		// Add all our ids to the map

		for (String id : rup_id_list) {
			map.put (id, this);
		}
		return null;
	}




	// Delete the given id, which must be a secondary id.
	// Throw an exception if the id is not a secondary id.
	// Note: It is expected that calls to this function will be rare.

	private void delete_secondary_id (String id) {

		// Search for the index of the element to delete

		int delix;
		for (delix = 1; delix < rup_id_list.length; ++delix) {
			if (rup_id_list[delix].equals(id)) {
				break;
			}
		}

		// Didn't find it

		if (delix >= rup_id_list.length) {
			throw new IllegalArgumentException ("ComcatLocalCatalogEntry.delete_secondary_id: Not a secondary id, id = " + id);
		}

		// Make the new array and fill it

		String[] new_rup_id_list = new String[rup_id_list.length - 1];
		int k = 0;
		for (int j = 0; j < rup_id_list.length; ++j) {
			if (j != delix) {
				new_rup_id_list[k] = rup_id_list[j];
				++k;
			}
		}

		// Set the new List

		rup_id_list = new_rup_id_list;
		return;
	}




	// Add all our ids to a map.
	// Returns true if this event has not been seen before.
	// Note: This version does not modify any entries.
	// If our primary id equals the primary id of an existing map entry,
	// then return false and make no changes to map or dup_ids.
	// Otherwise, for all our ids (primary and secondary):
	// - If the id is not in the map, then add the id to the map.
	// - If our id is in the map, then add the id to dup_ids.

	public boolean add_ids_to_map_checkall (Map<String, ComcatLocalCatalogEntry> map, List<String> dup_ids) {

		// If our primary id is in use ...

		ComcatLocalCatalogEntry other = map.get (rup_id_list[0]);
		if (other != null) {
		
			// If it's the primary id of the other entry, discard

			if (rup_id_list[0].equals (other.rup_id_list[0])) {
				return false;
			}

			// Record it as a duplicate

			dup_ids.add (rup_id_list[0]);
		}

		// Otherwise, add it to the map
		
		else {
			map.put (rup_id_list[0], this);
		}

		// Now scan our secondary ids ...

		for (int k = 1; k < rup_id_list.length; ++k) {

			// If the secondary id is in use, record it as a duplicate
		
			if (map.containsKey (rup_id_list[k])) {
				dup_ids.add (rup_id_list[k]);
			}

			// Otherwise, add it to the map
		
			else {
				map.put (rup_id_list[k], this);
			}
		}

		return true;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatLocalCatalogEntry : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id
		// Fetch information for an event, and display it.
		// Then convert to a local catalog entry, and display it.
		// Then format a string, and display it.
		// Then parse the string to obtain a new entry, and display it.
		// Then convert back to a rupture, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ComcatLocalCatalogEntry : Invalid 'test1' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				// Convert to local catalog entry

				ComcatLocalCatalogEntry entry = new ComcatLocalCatalogEntry();
				entry.set_eqk_rupture (rup);

				System.out.println (entry.toString());

				// Format a line

				String line = entry.format_line();

				System.out.println (line);
				System.out.println ("");

				// Parse the line

				ComcatLocalCatalogEntry entry2 = new ComcatLocalCatalogEntry();
				entry2.parse_line (line);

				System.out.println (entry2.toString());

				// Convert to a rupture

				ObsEqkRupture rup2 = entry2.get_eqk_rupture (false, true);

				System.out.println (ComcatOAFAccessor.rupToString (rup2));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id
		// Fetch information for an event, and display it.
		// Then convert to a local catalog entry, and display it.
		// Then format a string, and display it.
		// Then format a line for a flat file, and display it.

		if (args[0].equalsIgnoreCase ("test2")) {

			// One additional argument

			if (args.length != 2) {
				System.err.println ("ComcatLocalCatalogEntry : Invalid 'test2' subcommand");
				return;
			}

			String event_id = args[1];

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				// Convert to local catalog entry

				ComcatLocalCatalogEntry entry = new ComcatLocalCatalogEntry();
				entry.set_eqk_rupture (rup);

				System.out.println (entry.toString());

				// Format a line

				String line = entry.format_line();

				System.out.println (line);
				System.out.println ("");

				// Format a flat-file line

				String line2 = entry.format_flat_line();

				System.out.println (line2);
				System.out.println ("");

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatLocalCatalogEntry : Unrecognized subcommand : " + args[0]);
		return;

	}

}
