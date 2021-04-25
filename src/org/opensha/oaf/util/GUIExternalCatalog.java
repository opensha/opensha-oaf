package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Date;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;


/**
 * Read/write the external catalog file used by the two GUIs.
 * Author: Michael Barall 02/16/2020.
 *
 * Note: Much of the code in this class is excerpted from the GUIs.
 *
 * This class can be used to read and write, the external catalog
 * files used by the R&J GUI and ETAS GUI.
 *
 * The file format is:
 *
 *   header_line
 *   column_line
 *   mainshock_line_1
 *   mainshock_line_2
 *   aftershock_line_1
 *   aftershock_line_2
 *   . . .
 *   aftershock_line_N
 *
 * Any line whose first non-white character is '#' is a comment.
 * A completely blank line is also a comment.
 *
 * Any line which is not a comment contains an aftershock.  An aftershock
 * line has 10 fields, as follows.  Field are separated with a single tab.
 *
 *   <Year> <Month> <Day> <Hour> <Minute> <Sec> <Lat> <Lon> <Depth> <Magnitude>
 *
 * The GUIs write the aftershocks in time order.  It is unclear if there is any code
 * that depends on the aftershock ordering.
 *
 * The ETAS GUI writes a header line at the start of the file which contains the
 * mainshock event ID and the end time of the catalog (in days since the mainshock).
 * The R&J GUI does not write a header line, and ignores one if it is present.
 * The header line format is as follows.  Fields are separated with a single space.
 *
 *   # Header: eventID <event_id> dataEndTime <end_time>
 *
 * The column line labels the 10 data columns.  It is as follows, with a single
 * space after the '#'.
 *
 *   # Year\tMonth\tDay\tHour\tMinute\tSec\tLat\tLon\tDepth\tMagnitude
 *
 * If there is a mainshock, it is represented on two consecutive lines as follows.
 * Words on the first line are separated by a single space; fields on the second
 * line are separated by a single tab; on each line there is a single space after '#'.
 *
 *   # Main Shock:
 *   # <Year> <Month> <Day> <Hour> <Minute> <Sec> <Lat> <Lon> <Depth> <Magnitude>
 * 
 */
public class GUIExternalCatalog {

	//----- External file contents -----

	// The aftershocks listed in the file.

	public List<ObsEqkRupture> aftershocks;

	// The mainshock.
	// Can be null if there is no mainshock in the file.

	public ObsEqkRupture mainshock;

	// Mainshock event ID appearing in the file.
	// This is only used for the ETAS file format.
	// It can be null if there is no mainshock event ID in the file.

	public String catalog_event_id;

	// Catalog duration in days.
	// This is only used for the ETAS file format.
	// On input:
	// - If there is a header line (indicated by catalog_event_id != null),
	//    then it is the duration from the header line.
	// - If there is no header line (indicated by catalog_event_id == null),
	//    but there is a mainshock (indicated by mainshock != null) and at
	//    least one aftershock, then it is the difference between the time of
	//    the latest aftershock and the time of the mainshock.
	// - Otherwise, it is set to zero.
	// On output, if a header line is written (indicated by catalog_event_id != null),
	// then it is written to the header line.

	public double catalog_max_days;

	// The latest origin time of any aftershock.
	// On input, it is set to the latest origin time of any aftershock,
	// or Long.MIN_VALUE if there are no aftershocks.
	// On output, it is not used.

	public long latest_origin_time;

	// The earliest origin time of any aftershock.
	// On input, it is set to the earliest origin time of any aftershock,
	// or Long.MAX_VALUE if there are no aftershocks.
	// On output, it is not used.

	public long earliest_origin_time;




	//----- Internal definitions -----




	// The number of milliseconds in a day.

	public static final double C_MILLIS_PER_DAY = 86400000.0;




	// Formatter used for converting dates.

	private static SimpleDateFormat catDateFormat = new SimpleDateFormat("yyyy\tMM\tdd\tHH\tmm\tss");
	private static final TimeZone utc = TimeZone.getTimeZone("UTC");
	static {
		catDateFormat.setTimeZone(utc);
	}




	// Bring longitude value into range.
	// Parameters:
	//  lon = Longitude, in range -180.0 to +360.0 (allowed range for Location).
	// Returns longitude, coerced to the range -180.0 to +180.0.

	private static double unwrap (double lon){
		if (lon > 180)
			lon -= 360;
		return lon;
	}




	// Given an earthquake rupture, construct the catalog line.
	// Fields are tab-separated, and the line does not end in a newline.
	//
	// Design note: The use of (float) to limit output precision is not desireable,
	// but we retain it because the GUI code works this way.
	
	public static String getCatalogLine (ObsEqkRupture rup) {
		StringBuilder sb = new StringBuilder();
		Location hypoLoc = rup.getHypocenterLocation();
		sb.append(catDateFormat.format(rup.getOriginTimeCal().getTime())).append("\t");
		sb.append((float)hypoLoc.getLatitude()).append("\t");
		sb.append((float)unwrap(hypoLoc.getLongitude())).append("\t");
		sb.append((float)hypoLoc.getDepth()).append("\t");
		sb.append((float)rup.getMag());
		return sb.toString();
	}




	// Given a line from the file, construct the earthquake rupture.
	// Fields must be tab-separated.
	// Throws an exception if parsing error.
	// Note: An event ID is generated from the rupture time and magnitude.
	
	public static ObsEqkRupture fromCatalogLine (String line) {
		ObsEqkRupture result;

		try {
			line = line.trim();
			String[] split = line.split("\\s+");
			if (split.length != 10) {
				throw new IllegalArgumentException ("GUIExternalCatalog.fromCatalogLine: Expected 10 columns, got " + split.length);
			}
			String dateStr = split[0]+"\t"+split[1]+"\t"+split[2]+"\t"+split[3]+"\t"+split[4]+"\t"+split[5];
			Date date = catDateFormat.parse(dateStr);
			double lat = Double.parseDouble(split[6]);
			double lon = Double.parseDouble(split[7]);
			double depth = Double.parseDouble(split[8]);
			double mag = Double.parseDouble(split[9]);
			Location hypoLoc = new Location(lat, lon, depth);
		
			String eventId = dateStr.replaceAll("\t", "_")+"_M"+(float)mag;
			long originTimeInMillis = date.getTime();
		
			result = new ObsEqkRupture(eventId, originTimeInMillis, hypoLoc, mag);
		}
		catch (ParseException e) {
			throw new IllegalArgumentException ("GUIExternalCatalog.fromCatalogLine: Parse error in line: " + line, e);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException ("GUIExternalCatalog.fromCatalogLine: Numeric conversion error in line: " + line, e);
		}
		catch (Exception e) {
			throw new IllegalArgumentException ("GUIExternalCatalog.fromCatalogLine: Error in line: " + line, e);
		}

		return result;
	}




	// Given a line from the file, return the selected parameter value.
	// Parameters:
	//  line = Line from file, without a leading '#' or a trailing linefeed.
	//  target = Keyword to search for.
	// Returns the value, or "" if not found.

	private String fromHeaderLine (String line, String target) throws ParseException {
		line = line.trim();
		String[] split = line.split("\\s+");
		
		String match = "";
		
		for (int i = 0; i < split.length; i++)
			if (split[i].equals(target)){
				if (i < split.length - 1)
					match = split[i+1];
				continue;
			}
		return match;
	}




	//----- User functions -----




	// Read the catalog from a file.
	// Parameters:
	//  filename = Name of file to read.
	//  aftershock_list = List to which aftershocks are appended, or null.
	// The aftershock_list parameter can be used to select the desired type
	// of list; if null, then ObsEqkRupList is used.
	// Throws an exception if I/O or parse error.

	public void read_from_file (String filename, List<ObsEqkRupture> aftershock_list) {
		read_from_file (new File(filename), aftershock_list);
		return;
	}




	// Read the catalog from a file.
	// Parameters:
	//  the_file = File to read.
	//  aftershock_list = List to which aftershocks are appended, or null.
	// The aftershock_list parameter can be used to select the desired type
	// of list; if null, then ObsEqkRupList is used.
	// Throws an exception if I/O or parse error.

	public void read_from_file (File the_file, List<ObsEqkRupture> aftershock_list) {

		// Save or allocate the aftershock list

		if (aftershock_list == null) {
			aftershocks = new ObsEqkRupList();
		} else {
			aftershocks = aftershock_list;
		}

		// Mainshock not seen

		mainshock = null;

		// Catalog event id not seen

		catalog_event_id = null;

		// Catalog duration not known

		catalog_max_days = 0.0;

		// The latest and earliest origin time of any aftershock

		latest_origin_time = Long.MIN_VALUE;
		earliest_origin_time = Long.MAX_VALUE;

		// Open the file ...

		try (
			BufferedReader br = new BufferedReader (new FileReader (the_file));
		){

			// State variable: 0 = initial state, 1 = previous line was mainshock line 1

			int state = 0;

			// Loop over lines in file

			for (String s = br.readLine(); s != null; s = br.readLine()) {
				String line = s.trim();

				// If comment ...

				if (line.startsWith("#")) {

					// If this is the second line in a mainshock ...

					if (state == 1) {

						// Not first line of mainshock

						state = 0;
					
						// Mainshock comes after the #

						String mainshockLine = line.substring(1).trim();

						try {
							mainshock = fromCatalogLine(mainshockLine);
						} 
						catch (Exception e) {
							throw new IllegalArgumentException ("GUIExternalCatalog.read_from_file: Error parsing mainshock line: " + line);
						}

					}

					// If this line indicates there is a mainshock on the next line ...

					else if (line.toLowerCase().startsWith("# main")) {					

						// First line of mainshock

						state = 1;
					}

					// Othewise, if this line is a header line (used for ETAS files) ...

					else if (line.toLowerCase().startsWith("# header:")) {

						// Not first line of mainshock

						state = 0;

						// Parse the header line

						String headerLine = line.substring(1).trim();
						try {
							catalog_event_id = fromHeaderLine(headerLine, "eventID");
							catalog_max_days = Double.parseDouble(fromHeaderLine(headerLine, "dataEndTime"));
						} catch (Exception e) {
							throw new IllegalArgumentException ("GUIExternalCatalog.read_from_file: Error parsing header line: " + line);
						}
					}

					// Otherwise, an ordinary comment

					else {

						// Not first line of mainshock

						state = 0;
					}
				}

				// Otherwise, if line is empty ...

				else if (line.isEmpty()) {

					// Not first line of mainshock

					state = 0;
				}

				// Otherwise, expect an aftershock line ...

				else {

					// Not first line of mainshock

					state = 0;

					// Parse aftershock line

					try {

						// Parse line and add to list of aftershocks

						ObsEqkRupture rup = fromCatalogLine (line);
						aftershocks.add (rup);

						// Accumulate the latest and ealiest origin time

						latest_origin_time = Math.max (latest_origin_time, rup.getOriginTime());
						earliest_origin_time = Math.min (earliest_origin_time, rup.getOriginTime());

					} catch (Exception e) {
						throw new IllegalArgumentException ("GUIExternalCatalog.read_from_file: Error parsing aftershock line: " + line);
					}
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException ("GUIExternalCatalog.read_from_file: I/O error reading catalog file: " + the_file.getPath(), e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalog.read_from_file: Parsing error reading catalog file: " + the_file.getPath(), e);
		}

		// If there is no header, but there is a mainshock, and we got at least one aftershock...

		if (catalog_event_id == null && mainshock != null && latest_origin_time != Long.MIN_VALUE) {
		
			// Set the catalog duration to be the interval from mainshock to last aftershock, in days

			catalog_max_days = ((double)(latest_origin_time - mainshock.getOriginTime())) / C_MILLIS_PER_DAY;
		}

		// If there is a header and a mainshock, insert the event ID into the mainshock

		if (catalog_event_id != null && mainshock != null) {
			mainshock.setEventId (catalog_event_id);
		}
	
		return;
	}




	// Write the catalog to a file.
	// Parameters:
	//  filename = Name of file to write.
	// Throws an exception if I/O or other error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public void write_to_file (String filename) {
		write_to_file (new File(filename));
		return;
	}




	// Write the catalog to a file.
	// Parameters:
	//  the_file = File to write.
	// Throws an exception if I/O or other error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public void write_to_file (File the_file) {

		// Open the file ...

		try (
			BufferedWriter bw = new BufferedWriter (new FileWriter (the_file));
		){

			// Write the header string, if desired

			if (catalog_event_id != null) {
				String headerString = "# Header: eventID " + catalog_event_id + " dataEndTime " + catalog_max_days;  
				bw.write (headerString);
				bw.newLine();
			}

			// Write columns

			String columnString = "# Year\tMonth\tDay\tHour\tMinute\tSec\tLat\tLon\tDepth\tMagnitude";
			bw.write (columnString);
			bw.newLine();

			// Write the mainshock, if desired

			if (mainshock != null) {
				String mainString1 = "# Main Shock:";
				String mainString2 = "# " + getCatalogLine(mainshock);
				bw.write (mainString1);
				bw.newLine();
				bw.write (mainString2);
				bw.newLine();
			}

			// Write the aftershocks

			for (ObsEqkRupture rup : aftershocks) {
				String asString = getCatalogLine(rup);
				bw.write (asString);
				bw.newLine();
			}
		}
		catch (IOException e) {
			throw new RuntimeException ("GUIExternalCatalog.write_to_file: I/O error writing catalog file: " + the_file.getPath(), e);
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalog.write_to_file: Error writing catalog file: " + the_file.getPath(), e);
		}
	
		return;
	}




	// Write the catalog to a string.
	// Returns a string containing the entire catalog.
	// Throws an exception if error.
	// Note: This function writes aftershocks in the order they appear in the list.
	// It is recommended that the list be sorted by time.

	public String write_to_string () {
		StringBuilder sb = new StringBuilder();

		try {

			// Write the header string, if desired

			if (catalog_event_id != null) {
				String headerString = "# Header: eventID " + catalog_event_id + " dataEndTime " + catalog_max_days;  
				sb.append (headerString);
				sb.append ("\n");
			}

			// Write columns

			String columnString = "# Year\tMonth\tDay\tHour\tMinute\tSec\tLat\tLon\tDepth\tMagnitude";
			sb.append (columnString);
			sb.append ("\n");

			// Write the mainshock, if desired

			if (mainshock != null) {
				String mainString1 = "# Main Shock:";
				String mainString2 = "# " + getCatalogLine(mainshock);
				sb.append (mainString1);
				sb.append ("\n");
				sb.append (mainString2);
				sb.append ("\n");
			}

			// Write the aftershocks

			for (ObsEqkRupture rup : aftershocks) {
				String asString = getCatalogLine(rup);
				sb.append (asString);
				sb.append ("\n");
			}
		}
		catch (Exception e) {
			throw new RuntimeException ("GUIExternalCatalog.write_to_string: Error writing catalog file", e);
		}
	
		return sb.toString();
	}




	// Set up the catalog, before writing.
	// Parameters:
	//  aftershocks = List of aftershocks.
	//  mainshock = Mainshock, or null if no mainshock should be written.
	//  catalog_event_id = Event ID to place in header line, or null if no
	//    header line should be written.
	//  catalog_max_days = Catalog duration, measured in days since the mainshock,
	//    which should be written to the hedaer line; ignored if no header line is written.
	// Note: This function retains the passed-in objects.

	public void setup_catalog (
		List<ObsEqkRupture> aftershocks,
		ObsEqkRupture mainshock,
		String catalog_event_id,
		double catalog_max_days
	) {
		this.aftershocks = aftershocks;
		this.mainshock = mainshock;
		this.catalog_event_id = catalog_event_id;
		this.catalog_max_days = catalog_max_days;

		latest_origin_time = Long.MIN_VALUE;
		earliest_origin_time = Long.MAX_VALUE;
		return;
	}




	// Set up the catalog, before writing.
	// Parameters:
	//  aftershocks = List of aftershocks.
	//  mainshock = Mainshock, or null if no mainshock should be written.
	// Note: This function retains the passed-in objects.
	// Note: This function can be used if no hearder line is desired.

	public void setup_catalog (
		List<ObsEqkRupture> aftershocks,
		ObsEqkRupture mainshock
	) {
		setup_catalog (
			aftershocks,
			mainshock,
			null,
			0.0
		);
		return;
	}




	// Sort the list of aftershocks, in order of increasing time.

	public void sort_aftershocks () {
		aftershocks.sort (new ObsEqkRupMinTimeComparator());
		return;
	}

	// Static version that operates on any list.

	public static void sort_aftershocks (List<ObsEqkRupture> aftershock_list) {
		aftershock_list.sort (new ObsEqkRupMinTimeComparator());
		return;
	}

}
