package org.opensha.oaf.aafs;


/**
 * Program version information.
 * Author: Michael Barall 08/16/2018.
 */
public class VersionInfo  {

	// Program name.

	public static final String program_name = "USGS Aftershock Forecasting System";

	// Program version.

	public static final String program_version = "Version 1.04.1770 (06/13/2025)";

	// Program sponsor.

	public static final String program_sponsor = "U.S. Geological Survey, Earthquake Science Center";

	// Major version.

	public static final int major_version = 1;

	// Minor version.

	public static final int minor_version = 4;

	// Build.

	public static final int build = 1770;




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

}
