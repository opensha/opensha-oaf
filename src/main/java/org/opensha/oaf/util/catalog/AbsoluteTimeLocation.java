package org.opensha.oaf.util.catalog;

import org.opensha.oaf.util.SimpleUtils;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;


// Holds an absolute time and location - time, latitude, longitude, depth.
// Author: Michael Barall 09/14/2021.
//
// This is a modifiable object.

public class AbsoluteTimeLocation {

	// Time, in milliseconds since the epoch.

	public long abs_time;

	// Latitude, in degrees.

	public double abs_lat;

	// Longitude, in degrees.

	public double abs_lon;

	// Depth, in kilometers.

	public double abs_depth;




	// Clear to zero.

	public final AbsoluteTimeLocation clear () {
		abs_time = 0L;
		abs_lat = 0.0;
		abs_lon = 0.0;
		abs_depth = 0.0;
		return this;
	}

	// Consructor - clear to zero.

	public AbsoluteTimeLocation () {
		clear();
	}




	// Set values.

	public final AbsoluteTimeLocation set (long time, double lat, double lon, double depth) {
		this.abs_time = time;
		this.abs_lat = lat;
		this.abs_lon = lon;
		this.abs_depth = depth;
		return this;
	}

	// Constructor - set values.

	public AbsoluteTimeLocation (long time, double lat, double lon, double depth) {
		set (time, lat, lon, depth);
	}




	// Copy from another object.

	public final AbsoluteTimeLocation copy_from (AbsoluteTimeLocation other) {
		this.abs_time = other.abs_time;
		this.abs_lat = other.abs_lat;
		this.abs_lon = other.abs_lon;
		this.abs_depth = other.abs_depth;
		return this;
	}

	// Constructor - copy from another object.

	public AbsoluteTimeLocation (AbsoluteTimeLocation other) {
		copy_from (other);
	}




	// Compare for equality to another object.

	public final boolean equal_to (AbsoluteTimeLocation other) {
		return this.abs_time == other.abs_time
			&& this.abs_lat == other.abs_lat
			&& this.abs_lon == other.abs_lon
			&& this.abs_depth == other.abs_depth;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj != null && obj instanceof AbsoluteTimeLocation) {
			return equal_to ((AbsoluteTimeLocation)obj);
		}
		return false;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("AbsoluteTimeLocation:" + "\n");

		result.append ("abs_time = " + abs_time + " (" + SimpleUtils.time_to_parseable_string(abs_time) + ")" + "\n");
		result.append ("abs_lat = " + abs_lat + "\n");
		result.append ("abs_lon = " + abs_lon + "\n");
		result.append ("abs_depth = " + abs_depth + "\n");

		return result.toString();
	}

	// Value string.

	public final String value_string() {
		StringBuilder result = new StringBuilder();

		result.append ("abs_time = " + abs_time + " (" + SimpleUtils.time_to_parseable_string(abs_time) + ")" + "\n");
		result.append ("abs_lat = " + abs_lat + "\n");
		result.append ("abs_lon = " + abs_lon + "\n");
		result.append ("abs_depth = " + abs_depth + "\n");

		return result.toString();
	}

	// One-line string.

	public final String one_line_string() {
		return abs_time + " " + abs_lat + " " + abs_lon + " " + abs_depth;
	}

	// Set from a string, as produced by one_line_string.
	// Throws an exception if invalid string.

	public final AbsoluteTimeLocation set (String s) {
		String[] x = s.trim().split("\\s+");
		if (x.length != 4) {
			throw new IllegalArgumentException ("AbsoluteTimeLocation.set: Invalid string argument: " + s);
		}
		try {
			abs_time = Long.parseLong (x[0]);
			abs_lat = Double.parseDouble (x[1]);
			abs_lon = Double.parseDouble (x[2]);
			abs_depth = Double.parseDouble (x[3]);
		}
		catch (Exception e) {
			throw new IllegalArgumentException ("AbsoluteTimeLocation.set: Invalid string argument: " + s, e);
		}
		return this;
	}

	// Constructor - set from a string, as produced by one_line_string.

	public AbsoluteTimeLocation (String s) {
		set (s);
	}




	// Possible longitude ranges.

	public static final int LON_RANGE_SPH = 1;		// Spherical range, -180 to +180
	public static final int LON_RANGE_WRAP = 2;		// Wrapped range, 0 to +360
	public static final int LON_RANGE_LOC = 3;		// Location range, -180 to +360
	public static final int LON_RANGE_FULL = 4;		// Full range, -360 to +360


	// Longitude limits.

	private static final double LON_LIM_TINY_MIN = -1.0e-16;
	private static final double LON_LIM_TINY_MAX = 1.0e-16;

	private static final double LON_LIM_SPH_MIN = -180.0;
	private static final double LON_LIM_SPH_COERCE_MIN = -180.0001;
	private static final double LON_LIM_SPH_MAX = 180.0;
	private static final double LON_LIM_SPH_COERCE_MAX = 180.0001;

	private static final double LON_LIM_WRAP_MIN = 0.0;
	private static final double LON_LIM_WRAP_COERCE_MIN = -0.0001;
	private static final double LON_LIM_WRAP_MAX = 360.0;
	private static final double LON_LIM_WRAP_COERCE_MAX = 360.0001;

	private static final double LON_LIM_LOC_MIN = -180.0;
	private static final double LON_LIM_LOC_COERCE_MIN = -180.0001;
	private static final double LON_LIM_LOC_MAX = 360.0;
	private static final double LON_LIM_LOC_COERCE_MAX = 360.0001;

	private static final double LON_LIM_FULL_MIN = -360.0;
	private static final double LON_LIM_FULL_COERCE_MIN = -360.0001;
	private static final double LON_LIM_FULL_MAX = 360.0;
	private static final double LON_LIM_FULL_COERCE_MAX = 360.0001;


	// Coerce a longitude.
	// The expected parameter range (the range of lon) is selected by in_range = LON_RANGE_XXXX.
	// The result range is selected by out_range = LON_RANGE_XXXX.

	public static double coerce_lon (double lon, int in_range, int out_range) {
		switch (in_range) {

		case LON_RANGE_SPH:
			switch (out_range) {
			case LON_RANGE_SPH:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_SPH_MIN, LON_LIM_SPH_COERCE_MIN,
					LON_LIM_SPH_MAX, LON_LIM_SPH_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_WRAP:
				return SimpleUtils.coerce_wrap_value (
					lon,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_MAX,
					LON_LIM_SPH_MIN, LON_LIM_SPH_COERCE_MIN,
					LON_LIM_SPH_MAX, LON_LIM_SPH_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_LOC:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_SPH_MIN, LON_LIM_SPH_COERCE_MIN,
					LON_LIM_SPH_MAX, LON_LIM_SPH_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_FULL:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_SPH_MIN, LON_LIM_SPH_COERCE_MIN,
					LON_LIM_SPH_MAX, LON_LIM_SPH_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			}
			throw new IllegalArgumentException ("AbsoluteTimeLocation.coerce_lon: Invalid output range: out_range = " + out_range);

		case LON_RANGE_WRAP:
			switch (out_range) {
			case LON_RANGE_SPH:
				return SimpleUtils.coerce_wrap_value (
					lon,
					LON_LIM_SPH_MIN, LON_LIM_SPH_MAX,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_COERCE_MIN,
					LON_LIM_WRAP_MAX, LON_LIM_WRAP_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_WRAP:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_COERCE_MIN,
					LON_LIM_WRAP_MAX, LON_LIM_WRAP_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_LOC:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_COERCE_MIN,
					LON_LIM_WRAP_MAX, LON_LIM_WRAP_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_FULL:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_COERCE_MIN,
					LON_LIM_WRAP_MAX, LON_LIM_WRAP_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			}
			throw new IllegalArgumentException ("AbsoluteTimeLocation.coerce_lon: Invalid output range: out_range = " + out_range);

		case LON_RANGE_LOC:
			switch (out_range) {
			case LON_RANGE_SPH:
				return SimpleUtils.coerce_wrap_value (
					lon,
					LON_LIM_SPH_MIN, LON_LIM_SPH_MAX,
					LON_LIM_LOC_MIN, LON_LIM_LOC_COERCE_MIN,
					LON_LIM_LOC_MAX, LON_LIM_LOC_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_WRAP:
				return SimpleUtils.coerce_wrap_value (
					lon,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_MAX,
					LON_LIM_LOC_MIN, LON_LIM_LOC_COERCE_MIN,
					LON_LIM_LOC_MAX, LON_LIM_LOC_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_LOC:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_LOC_MIN, LON_LIM_LOC_COERCE_MIN,
					LON_LIM_LOC_MAX, LON_LIM_LOC_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_FULL:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_LOC_MIN, LON_LIM_LOC_COERCE_MIN,
					LON_LIM_LOC_MAX, LON_LIM_LOC_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			}
			throw new IllegalArgumentException ("AbsoluteTimeLocation.coerce_lon: Invalid output range: out_range = " + out_range);

		case LON_RANGE_FULL:
			switch (out_range) {
			case LON_RANGE_SPH:
				return SimpleUtils.coerce_wrap_value (
					lon,
					LON_LIM_SPH_MIN, LON_LIM_SPH_MAX,
					LON_LIM_FULL_MIN, LON_LIM_FULL_COERCE_MIN,
					LON_LIM_FULL_MAX, LON_LIM_FULL_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_WRAP:
				return SimpleUtils.coerce_wrap_value (
					lon,
					LON_LIM_WRAP_MIN, LON_LIM_WRAP_MAX,
					LON_LIM_FULL_MIN, LON_LIM_FULL_COERCE_MIN,
					LON_LIM_FULL_MAX, LON_LIM_FULL_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			case LON_RANGE_LOC:
				double x = SimpleUtils.coerce_value (
					lon,
					LON_LIM_FULL_MIN, LON_LIM_FULL_COERCE_MIN,
					LON_LIM_FULL_MAX, LON_LIM_FULL_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
				if (x < LON_LIM_LOC_MIN) {
					x += 360.0;
				}
				return x;
			case LON_RANGE_FULL:
				return SimpleUtils.coerce_value (
					lon,
					LON_LIM_FULL_MIN, LON_LIM_FULL_COERCE_MIN,
					LON_LIM_FULL_MAX, LON_LIM_FULL_COERCE_MAX,
					LON_LIM_TINY_MIN, LON_LIM_TINY_MAX
				);
			}
			throw new IllegalArgumentException ("AbsoluteTimeLocation.coerce_lon: Invalid output range: out_range = " + out_range);
		}
		throw new IllegalArgumentException ("AbsoluteTimeLocation.coerce_lon: Invalid input range: in_range = " + in_range);
	}


	// Set the coerced longitude.
	// The expected parameter range is selected by in_range = LON_RANGE_XXXX.

	public final void set_coerce_lon (double lon, int in_range) {
		abs_lon = coerce_lon (lon, in_range, LON_RANGE_SPH);
		return;
	}


	// Get the coerced longitude.
	// The result range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.

	public final double get_coerce_lon (boolean wrapLon) {
		return coerce_lon (abs_lon, LON_RANGE_SPH, wrapLon ? LON_RANGE_WRAP : LON_RANGE_SPH);
	}


	// Get longitude field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_lon (int decimals, boolean f_pos_lead) {
		return decimals + 5;
	}




	// Latitude limits.

	private static final double LAT_LIM_TINY_MIN = -1.0e-16;
	private static final double LAT_LIM_TINY_MAX = 1.0e-16;

	private static final double LAT_LIM_MIN = -90.0;
	private static final double LAT_LIM_COERCE_MIN = -90.0001;
	private static final double LAT_LIM_MAX = 90.0;
	private static final double LAT_LIM_COERCE_MAX = 90.0001;


	// Coerce a latitude.
	// The expected parameter range is -90 to +90.
	// The result range is -90 to +90.

	public static double coerce_lat (double lat) {
		return SimpleUtils.coerce_value (
			lat,
			LAT_LIM_MIN, LAT_LIM_COERCE_MIN,
			LAT_LIM_MAX, LAT_LIM_COERCE_MAX,
			LAT_LIM_TINY_MIN, LAT_LIM_TINY_MAX
		);
	}


	// Set the coerced latitude.

	public final void set_coerce_lat (double lat) {
		abs_lat = coerce_lat (lat);
		return;
	}


	// Get the coerced latitude.

	public final double get_coerce_lat () {
		return coerce_lat (abs_lat);
	}


	// Get latitude field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_lat (int decimals, boolean f_pos_lead) {
		return decimals + 4;
	}




	// Depth limits.

	private static final double DEPTH_LIM_TINY_MIN = -1.0e-16;
	private static final double DEPTH_LIM_TINY_MAX = 1.0e-16;

	private static final double DEPTH_LIM_MIN = -5.0;
	private static final double DEPTH_LIM_COERCE_MIN = -5.0001;
	private static final double DEPTH_LIM_MAX = 700.0;
	private static final double DEPTH_LIM_COERCE_MAX = 700.0001;


	// Coerce a depth.
	// The expected parameter range is -5 to +700.
	// The result range is -5 to +700.

	public static double coerce_depth (double depth) {
		return SimpleUtils.coerce_value (
			depth,
			DEPTH_LIM_MIN, DEPTH_LIM_COERCE_MIN,
			DEPTH_LIM_MAX, DEPTH_LIM_COERCE_MAX,
			DEPTH_LIM_TINY_MIN, DEPTH_LIM_TINY_MAX
		);
	}


	// Set the coerced depth.

	public final void set_coerce_depth (double depth) {
		abs_depth = coerce_depth (depth);
		return;
	}


	// Get the coerced depth.

	public final double get_coerce_depth () {
		return coerce_depth (abs_depth);
	}


	// Get depth field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_depth (int decimals, boolean f_pos_lead) {
		return decimals + (f_pos_lead ? 5 : 4);
	}




	// Magnitude limits.
	// Note: Athough this class does not hold a magnitude, we put these functions here
	// because there is no similar class that holds a magnitude.

	private static final double MAG_LIM_TINY_MIN = -1.0e-16;
	private static final double MAG_LIM_TINY_MAX = 1.0e-16;

	private static final double MAG_LIM_MIN = -12.0;
	private static final double MAG_LIM_COERCE_MIN = -12.0001;
	private static final double MAG_LIM_MAX = 12.0;
	private static final double MAG_LIM_COERCE_MAX = 12.0001;


	// Coerce a magnitude.
	// The expected parameter range is -12 to +12.
	// The result range is -12 to +12.

	public static double coerce_mag (double depth) {
		return SimpleUtils.coerce_value (
			depth,
			MAG_LIM_MIN, MAG_LIM_COERCE_MIN,
			MAG_LIM_MAX, MAG_LIM_COERCE_MAX,
			MAG_LIM_TINY_MIN, MAG_LIM_TINY_MAX
		);
	}


	// Get magnitude field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_mag (int decimals, boolean f_pos_lead) {
		return decimals + 4;
	}




	// Set from a time and location.

	public final AbsoluteTimeLocation set (long time, Location loc) {
		this.abs_time = time;
		set_coerce_lat (loc.getLatitude());
		set_coerce_lon (loc.getLongitude(), LON_RANGE_LOC);
		set_coerce_depth (loc.getDepth());
		return this;
	}

	// Construct - from a time and location.

	public AbsoluteTimeLocation (long time, Location loc) {
		set (time, loc);
	}

	// Make a Location object.
	// Longitude range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.

	public final Location getLocation (boolean wrapLon) {
		return new Location (get_coerce_lat(), get_coerce_lon(wrapLon), get_coerce_depth());
	}




	// Set from a rupture.

	public final AbsoluteTimeLocation set (ObsEqkRupture rup) {
		this.abs_time = rup.getOriginTime();
		Location hypo = rup.getHypocenterLocation();
		set_coerce_lat (hypo.getLatitude());
		set_coerce_lon (hypo.getLongitude(), LON_RANGE_LOC);
		set_coerce_depth (hypo.getDepth());
		return this;
	}

	// Constructor - from a rupture.

	public AbsoluteTimeLocation (ObsEqkRupture rup) {
		set (rup);
	}

	// Make an ObsEqkRupture object.
	// Longitude range is 0 to +360 if wrapLon is true, -180 to +180 if wrapLon is false.

	public final ObsEqkRupture getObsEqkRupture (String event_id, double mag, boolean wrapLon) {
		return new ObsEqkRupture (event_id, abs_time, getLocation(wrapLon), mag);
	}

}
