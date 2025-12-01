package org.opensha.oaf.aafs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Program version information.
 * Author: Michael Barall 08/16/2018.
 */
public class VersionInfo  {

	// Program name.

	public static final String program_name = "USGS Aftershock Forecasting System";

	// Program version.

	public static final String program_version = "Version 1.05.1825 (12/01/2025)";

	// Program sponsor.

	public static final String program_sponsor = "U.S. Geological Survey, Earthquake Science Center";

	// Major version.

	public static final int major_version = 1;

	// Minor version.

	public static final int minor_version = 5;

	// Build.

	public static final int build = 1825;




	// Get the title, as multiple lines but no final newline.

	public static String get_title () {
		return program_name + "\n"
				+ program_version + "\n"
				+ program_sponsor;
	}


	// Get a one-line name and version.

	public static String get_one_line_version () {
		return program_name + ", " + program_version;
	}


	// Get a one-line generator name and version.

	public static String get_generator_name () {
		return String.format ("%s %d.%02d.%04d", program_name, major_version, minor_version, build);
	}


	// Get the version number, as a string.

	public static String get_version_number () {
		return String.format ("%d.%02d.%04d", major_version, minor_version, build);
	}


	// Get the version date, as a string.

	public static String get_version_date () {
		Pattern pattern = Pattern.compile ("\\d{2}/\\d{2}/\\d{4}");
		Matcher matcher = pattern.matcher (program_version);
		if (matcher.find()) {
			return matcher.group();	// returns the first match
		}
		return "00/00/0000";	// should never happen
	}

}
