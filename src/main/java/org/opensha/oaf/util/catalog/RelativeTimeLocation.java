package org.opensha.oaf.util.catalog;

import org.opensha.oaf.util.SimpleUtils;


// Holds a relative time and location - relative time, x-coordinate, y-coordinate, depth-coordinate.
// Author: Michael Barall 09/14/2021.
//
// This is a modifiable object.

public class RelativeTimeLocation {

	// Time, in days since the origin.

	public double rel_t_day;

	// x-coordinate, in kilometers relative to the origin.

	public double rel_x_km;

	// y-coordinate, in kilometers relative to the origin.

	public double rel_y_km;

	// Relative depth, in kilometers relative to the origin.

	public double rel_d_km;




	// Clear to zero.

	public final RelativeTimeLocation clear () {
		rel_t_day = 0.0;
		rel_x_km = 0.0;
		rel_y_km = 0.0;
		rel_d_km = 0.0;
		return this;
	}

	// Consructor - clear to zero.

	public RelativeTimeLocation () {
		clear();
	}




	// Set values.

	public final RelativeTimeLocation set (double t_day, double x_km, double y_km, double d_km) {
		this.rel_t_day = t_day;
		this.rel_x_km = x_km;
		this.rel_y_km = y_km;
		this.rel_d_km = d_km;
		return this;
	}

	// Constructor - set values.

	public RelativeTimeLocation (double t_day, double x_km, double y_km, double d_km) {
		set (t_day, x_km, y_km, d_km);
	}




	// Copy from another object.

	public final RelativeTimeLocation copy_from (RelativeTimeLocation other) {
		this.rel_t_day = other.rel_t_day;
		this.rel_x_km = other.rel_x_km;
		this.rel_y_km = other.rel_y_km;
		this.rel_d_km = other.rel_d_km;
		return this;
	}

	// Constructor - copy from another object.

	public RelativeTimeLocation (RelativeTimeLocation other) {
		copy_from (other);
	}




	// Compare for equality to another object.

	public final boolean equal_to (RelativeTimeLocation other) {
		return this.rel_t_day == other.rel_t_day
			&& this.rel_x_km == other.rel_x_km
			&& this.rel_y_km == other.rel_y_km
			&& this.rel_d_km == other.rel_d_km;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj != null && obj instanceof RelativeTimeLocation) {
			return equal_to ((RelativeTimeLocation)obj);
		}
		return false;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("RelativeTimeLocation:" + "\n");

		result.append ("rel_t_day = " + rel_t_day + "\n");
		result.append ("rel_x_km = " + rel_x_km + "\n");
		result.append ("rel_y_km = " + rel_y_km + "\n");
		result.append ("rel_d_km = " + rel_d_km + "\n");

		return result.toString();
	}

	// Value string.

	public final String value_string() {
		StringBuilder result = new StringBuilder();

		result.append ("rel_t_day = " + rel_t_day + "\n");
		result.append ("rel_x_km = " + rel_x_km + "\n");
		result.append ("rel_y_km = " + rel_y_km + "\n");
		result.append ("rel_d_km = " + rel_d_km + "\n");

		return result.toString();
	}

	// One-line string.

	public final String one_line_string() {
		return rel_t_day + " " + rel_x_km + " " + rel_y_km + " " + rel_d_km;
	}

	// Set from a string, as produced by toString.
	// Throws an exception if invalid string.

	public final RelativeTimeLocation set (String s) {
		String[] x = s.trim().split("\\s+");
		if (x.length != 4) {
			throw new IllegalArgumentException ("RelativeTimeLocation.set: Invalid string argument: " + s);
		}
		try {
			rel_t_day = Double.parseDouble (x[0]);
			rel_x_km = Double.parseDouble (x[1]);
			rel_y_km = Double.parseDouble (x[2]);
			rel_d_km = Double.parseDouble (x[3]);
		}
		catch (Exception e) {
			throw new IllegalArgumentException ("RelativeTimeLocation.set: Invalid string argument: " + s, e);
		}
		return this;
	}

	// Construct from a string, as produced by toString.

	public RelativeTimeLocation (String s) {
		set (s);
	}




	// Relative time limits.

	private static final double T_DAY_LIM_TINY_MIN = -1.0e-16;
	private static final double T_DAY_LIM_TINY_MAX = 1.0e-16;


	// Coerce a relative time.
	// There are no limits on parameter range.
	// We just force very small values to zero.

	public static double coerce_t_day (double t_day) {
		return SimpleUtils.coerce_tiny_to_zero (
			t_day,
			T_DAY_LIM_TINY_MIN, T_DAY_LIM_TINY_MAX
		);
	}


	// Set the coerced relative time.

	public final void set_coerce_t_day (double t_day) {
		rel_t_day = coerce_t_day (t_day);
		return;
	}


	// Get the coerced relative time.

	public final double get_coerce_t_day () {
		return coerce_t_day (rel_t_day);
	}


	// Get relative time field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.
	// Note: There are no limits on the range of t_day, so we have chosen to assume
	// a range of -9999 to +99999 days (about -30 to +300 years) for this purpose.

	public static int get_width_t_day (int decimals, boolean f_pos_lead) {
		return decimals + (f_pos_lead ? 7 : 6);
	}




	// Relative x-coordinate limits.

	private static final double X_KM_LIM_TINY_MIN = -1.0e-16;
	private static final double X_KM_LIM_TINY_MAX = 1.0e-16;

	private static final double X_KM_LIM_MIN = -9999.0;
	private static final double X_KM_LIM_COERCE_MIN = -9999.0001;
	private static final double X_KM_LIM_MAX = 9999.0;
	private static final double X_KM_LIM_COERCE_MAX = 9999.0001;


	// Coerce a relative x-coordinate.
	// The expected parameter range is -9999 to +9999.
	// The result range is -9999 to +9999.

	public static double coerce_x_km (double x_km) {
		return SimpleUtils.coerce_value (
			x_km,
			X_KM_LIM_MIN, X_KM_LIM_COERCE_MIN,
			X_KM_LIM_MAX, X_KM_LIM_COERCE_MAX,
			X_KM_LIM_TINY_MIN, X_KM_LIM_TINY_MAX
		);
	}


	// Set the coerced relative x-coordinate.

	public final void set_coerce_x_km (double x_km) {
		rel_x_km = coerce_x_km (x_km);
		return;
	}


	// Get the coerced relative x-coordinate.

	public final double get_coerce_x_km () {
		return coerce_x_km (rel_x_km);
	}


	// Get relative x-coordinate field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_x_km (int decimals, boolean f_pos_lead) {
		return decimals + 6;
	}




	// Relative y-coordinate limits.

	private static final double Y_KM_LIM_TINY_MIN = -1.0e-16;
	private static final double Y_KM_LIM_TINY_MAX = 1.0e-16;

	private static final double Y_KM_LIM_MIN = -9999.0;
	private static final double Y_KM_LIM_COERCE_MIN = -9999.0001;
	private static final double Y_KM_LIM_MAX = 9999.0;
	private static final double Y_KM_LIM_COERCE_MAX = 9999.0001;


	// Coerce a relative y-coordinate.
	// The expected parameter range is -9999 to +9999.
	// The result range is -9999 to +9999.

	public static double coerce_y_km (double y_km) {
		return SimpleUtils.coerce_value (
			y_km,
			Y_KM_LIM_MIN, Y_KM_LIM_COERCE_MIN,
			Y_KM_LIM_MAX, Y_KM_LIM_COERCE_MAX,
			Y_KM_LIM_TINY_MIN, Y_KM_LIM_TINY_MAX
		);
	}


	// Set the coerced relative y-coordinate.

	public final void set_coerce_y_km (double y_km) {
		rel_y_km = coerce_y_km (y_km);
		return;
	}


	// Get the coerced relative y-coordinate.

	public final double get_coerce_y_km () {
		return coerce_y_km (rel_y_km);
	}


	// Get relative y-coordinate field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_y_km (int decimals, boolean f_pos_lead) {
		return decimals + 6;
	}




	// Relative depth limits.

	private static final double D_KM_LIM_TINY_MIN = -1.0e-16;
	private static final double D_KM_LIM_TINY_MAX = 1.0e-16;

	private static final double D_KM_LIM_MIN = -99.0;
	private static final double D_KM_LIM_COERCE_MIN = -99.0001;
	private static final double D_KM_LIM_MAX = 999.0;
	private static final double D_KM_LIM_COERCE_MAX = 999.0001;


	// Coerce a relative depth.
	// The expected parameter range is -99 to +999.
	// The result range is -99 to +999.

	public static double coerce_d_km (double d_km) {
		return SimpleUtils.coerce_value (
			d_km,
			D_KM_LIM_MIN, D_KM_LIM_COERCE_MIN,
			D_KM_LIM_MAX, D_KM_LIM_COERCE_MAX,
			D_KM_LIM_TINY_MIN, D_KM_LIM_TINY_MAX
		);
	}


	// Set the coerced relative depth.

	public final void set_coerce_d_km (double d_km) {
		rel_d_km = coerce_d_km (d_km);
		return;
	}


	// Get the coerced relative depth.

	public final double get_coerce_d_km () {
		return coerce_d_km (rel_d_km);
	}


	// Get relative depth field width to hold given number of decimals, for fixed-point.
	// f_pos_lead is true if positive numbers have leading plus sign or space.

	public static int get_width_d_km (int decimals, boolean f_pos_lead) {
		return decimals + (f_pos_lead ? 5 : 4);
	}

}
