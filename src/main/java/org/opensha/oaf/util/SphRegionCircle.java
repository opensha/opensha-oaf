package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.opensha.commons.geo.Region;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.BorderType;

import static java.lang.Math.PI;
import static org.opensha.commons.geo.GeoTools.TWOPI;
import static org.opensha.commons.geo.GeoTools.TO_DEG;
import static org.opensha.commons.geo.GeoTools.TO_RAD;
import static org.opensha.commons.geo.GeoTools.EARTH_RADIUS_MEAN;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


/**
 * Region for locating aftershocks -- Circle.
 * Author: Michael Barall 04/10/2018.
 *
 * The AAFS server locates aftershocks by drawing a region surrounding the
 * mainshock.  Earthquakes within that region are considered to be aftershocks.
 * This class represents a circle.
 */
public class SphRegionCircle extends SphRegion {

	//----- Region definition -----

	// Latitude and longitude of the center, in degrees.

	private SphLatLon center;

	// Radius of the circle, in kilometers, must be positive, and no more than 180.0001 degrees.

	private double radius;




	//----- Querying -----

	/**
	 * contains - Test if the region contains the given location.
	 * @param loc = Location to check.
	 * @return
	 * Returns true if loc is inside the region, false if loc is outside the region.
	 * Note: Due to rounding errors, it may be indeterminate whether points exactly on,
	 * or very close to, the boundary of the region are considered inside or outside.
	 */
	@Override
	public boolean contains (SphLatLon loc) {
		return SphLatLon.horzDistance(center, loc) <= radius;
	}

	/**
	 * contains - Test if the region contains the given location.
	 * @param loc = Location to check.
	 * @return
	 * Returns true if loc is inside the region, false if loc is outside the region.
	 * Note: Due to rounding errors, it may be indeterminate whether points exactly on,
	 * or very close to, the boundary of the region are considered inside or outside.
	 */
	@Override
	public boolean contains (Location loc) {
		return SphLatLon.horzDistance(center, loc) <= radius;
	}

	/**
	 * contains - Test if the region contains the given location.
	 * @param lat = Latitude to check.
	 * @param lon = Longitude to check, can be -180 to +360.
	 * @return
	 * Returns true if lat/lon is inside the region, false if lat/lon is outside the region.
	 * Note: Due to rounding errors, it may be indeterminate whether points exactly on,
	 * or very close to, the boundary of the region are considered inside or outside.
	 */
	@Override
	public boolean contains (double lat, double lon) {
		return SphLatLon.horzDistance(center, lat, lon) <= radius;
	}




	//----- Plotting -----

	// This function is called when it is necessary to build plot_border.
	// The subclass should supply a function to build the border.
	// A subclass might choose to build the border in its constructor and
	// do_marshal method, in which case this function can just throw an exception.

	@Override
	protected void make_plot_border () {
		plot_border = new LocationList();

		// Latitude, longitude, and distance in radians

		double lat_rad = center.get_lat_rad();
		double lon_rad = center.get_lon_rad();
		double dist_rad = SphLatLon.distance_to_rad (radius);

		// Determine number of subdivisions
		// (Constant 230.0 chosen so radius = 1000 km corresponds to 36 divisions)

		int divs = (int)Math.round (230.0 * Math.sin(dist_rad));

		if (divs < 36) {
			divs = 36;
		} else if (divs > 150) {
			divs = 150;
		}

		double wedge = TWOPI / ((double)divs);

		// Form the circle by traveling along the great circle in each direction

		for (int i = 0; i < divs; ++i) {
			double az_rad = wedge * ((double)i);
			SphLatLon vertex = SphLatLon.gc_travel_rad (lat_rad, lon_rad, az_rad, dist_rad);
			add_border_point (vertex);
		}

		return;
	}




	//----- Special regions -----

	/**
	 * Return true if this is a circular region on the sphere.
	 * If this function returns true, then the center and radius can be
	 * obtained from getCircleCenter and getCircleRadiusDeg.
	 */
	@Override
	public boolean isCircular() {
		return true;
	}

	/**
	 * If this is a circular region on the sphere, then get the center.
	 * Otherwise, throw an exception.
	 */
	@Override
	public SphLatLon getCircleCenter() {
		return center;
	}

	/**
	 * If this is a circular region on the sphere, then get the radius in degrees.
	 * The returned value ranges from 0 to +180.
	 */
	@Override
	public double getCircleRadiusDeg() {
		return Math.min (180.0, SphLatLon.distance_to_deg (radius));
	}

	/**
	 * If this is a circular region on the sphere, then get the radius in kilometers.
	 * The returned value ranges from 0 to one-half of the earth's circumference.
	 */
	@Override
	public double getCircleRadiusKm() {
		return radius;
	}




	//----- Comparison -----


	// Compare this region to the other region.
	// Returns one of the RCMP_XXXX values.
	// Note: Comparisons may use a tolerance to allow for rounding errors.
	// Note: This function returns a result other than RCMP_UNKNOWN only in cases
	// where the comparison can be done relatively easily.
	// Note: The default method uses only the bounding box and the is-rectangle flags.

	@Override
	public int compare_to (SphRegion other) {

		// If the other region is a circle, do a circle comparison

		if (other instanceof SphRegionCircle) {
			return compare_to_circle ((SphRegionCircle)other);
		}

		// Otherwise, pass thru

		return super.compare_to (other);
	}


	// Compare this circle to the other circle.
	// Returns one of the RCMP_XXXX values.
	// Note: Comparisons may use a tolerance to allow for rounding errors.

	public int compare_to_circle (SphRegionCircle other) {

		// Check for the same object

		if (this == other) {
			return RCMP_EQUAL;
		}

		// Tolerance

		final double tol = DEF_TOL_RCMP;

		// Tolerance for radius, in km

		final double tol_km = tol * 100.0;

		// Get the distance between the circle centers, in km

		final double dist_km = SphLatLon.horzDistance(center, other.center);

		// Check for disjoint circles

		if (dist_km > radius + other.radius + tol_km) {
			return RCMP_DISJOINT;
		}

		// Check for equal circles

		if (dist_km <= tol_km && Math.abs (radius - other.radius) <= tol_km) {
			return RCMP_EQUAL;
		}

		// Check for one circle containing the other

		if (dist_km + radius <= other.radius + tol_km) {
			return RCMP_SUBSET;
		}
		if (dist_km + other.radius <= radius + tol_km) {
			return RCMP_CONTAINS;
		}

		// Circles overlap
		
		return RCMP_OVERLAP;
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SphRegionCircle () {}


	/**
	 * Construct from given center location and radius.
	 * @param center = Center of the circle.
	 * @param radius = Radius of the circle, in kilometers, must be positive and span no more than 180 degrees.
	 */
	public SphRegionCircle (SphLatLon center, double radius) {
		setup (center, radius);
	}


	/**
	 * Set up the region.
	 */
	private void setup (SphLatLon the_center, double the_radius) {

		// Save parameters

		center = the_center;
		radius = the_radius;

		// Radius in degrees

		double radius_deg = SphLatLon.distance_to_deg (radius);
		if (!( radius_deg > 0.0 && radius_deg <= 180.0001 )) {
			throw new IllegalArgumentException ("SphRegionCircle: Radius out-of-range: radius = " + radius);
		}

		// Use it to compute minimum and maximum latitude

		min_lat = center.get_lat() - radius_deg;
		max_lat = center.get_lat() + radius_deg;

		// If either exceeds 89 degrees, construct a polar region

		if (min_lat < -89.0 || max_lat > 89.0) {
			min_lat = Math.max (-90.0, min_lat);
			max_lat = Math.min ( 90.0, max_lat);

			// If the center is closer to the date line than the prime meridian, use the 0 to +360 domain

			if (center.get_lat() < -90.0 || center.get_lat() > 90.0) {
				min_lon = 0.0;
				max_lon = 360.0;
				plot_wrap = true;
			}

			// Otherwise use the -180 to +180 domain

			else {
				min_lon = -180.0;
				max_lon =  180.0;
				plot_wrap = false;
			}
		}

		// Otherwise, draw a box around the Circle

		else {

			// This is the half-angle subtended by the circle at the north or south pole.
			// It is derived from the law of sines for spherical triangles.

			double theta = Math.asin(Math.sin(radius_deg * TO_RAD) / Math.sin((90.0 - center.get_lat()) * TO_RAD)) * TO_DEG;

			// Use it to compute the minimum and maximum longitude

			min_lon = center.get_lon() - theta;
			max_lon = center.get_lon() + theta;

			// If it extends below -180, shift it

			if (min_lon < -180.0) {
				min_lon += 360.0;
				max_lon += 360.0;
				plot_wrap = true;
			}

			// If it extends above +180, no need to shift but the plot needs to be wrapped

			else if (max_lon > 180.0) {
				plot_wrap = true;
			}

			// Otherwise it's in the -180 to +180 domain

			else {
				plot_wrap = false;
			}
		
		}

		plot_border = null;

		return;
	}

	// Display our contents

	@Override
	public String toString() {
		return "SphRegionCircle:" + "\n"
		+ "plot_wrap = " + plot_wrap + "\n"
		+ "min_lat = " + min_lat + "\n"
		+ "max_lat = " + max_lat + "\n"
		+ "min_lon = " + min_lon + "\n"
		+ "max_lon = " + max_lon + "\n"
		+ "center_lat = " + center.get_lat() + "\n"
		+ "center_lon = " + center.get_lon() + "\n"
		+ "radius = " + radius;
	}


	// Get parameters that can be displayed to the user.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.

	@Override
	public void get_display_params (Map<String, Object> userParamMap) {
		userParamMap.put ("regionType", "circle");
		userParamMap.put ("regionCenterLat", SimpleUtils.round_double_via_string ("%.4f", center.get_lat()));
		userParamMap.put ("regionCenterLon", SimpleUtils.round_double_via_string ("%.4f", center.get_lon()));
		userParamMap.put ("regionRadius", SimpleUtils.round_double_via_string ("%.2f", radius));
		return;
	}


	// Make a region from user display parameters.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	// Returns null if the region could not be created.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.
	// Note: This function catches all exceptions, and returns null if an exception occurs.

	public static SphRegionCircle make_from_display_params (Map<String, Object> userParamMap) {
		SphRegionCircle region = null;
		try {

			// Check the region type

			if (check_string_from_display_params (userParamMap, "regionType", "circle")) {

				// Try to make the region

				double lat = get_double_from_display_params (userParamMap, "regionCenterLat");
				double lon = get_double_from_display_params (userParamMap, "regionCenterLon");
				double radius = get_double_from_display_params (userParamMap, "regionRadius");

				SphLatLon center = new SphLatLon (lat, lon);

				region = new SphRegionCircle (center, radius);
			}
		}

		// Return null for any exception

		catch (Exception e) {
			region = null;
		}
		
		return region;
	}


	// Check if a value of a parameter map has an expected string value, return false if not.

	private static boolean check_string_from_display_params (Map<String, Object> userParamMap, String name, String expected) {
		Object value = userParamMap.get (name);
		if (value == null) {
			return false;
		}
		if (value instanceof String) {
			return expected.equals ((String)value);
		}
		return false;
	}


	// Return a value of a parameter map as a double, throw exception if not possible

	private static double get_double_from_display_params (Map<String, Object> userParamMap, String name) {
		Object value = userParamMap.get (name);
		if (value == null) {
			throw new IllegalArgumentException ("Value is missing or null: name = " + name);
		}
		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}
		if (value instanceof String) {
			return Double.parseDouble ((String)value);
		}
		throw new IllegalArgumentException ("Value is incorrect type: name = " + name);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 13001;

	private static final String M_VERSION_NAME = "SphRegionCircle";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_CIRCLE;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		center.marshal (writer, "center");
		writer.marshalDouble ("radius", radius);

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("SphRegionCircle.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get center and radius

			SphLatLon the_center = (new SphLatLon()).unmarshal (reader, "center");;
			double the_radius = reader.unmarshalDouble ("radius");

			// Set up region

			try {
				setup (the_center, the_radius);
			}
			catch (Exception e) {
				throw new MarshalException ("SphRegionCircle.do_umarshal: Failed to set up region", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			center = (new SphLatLon()).unmarshal (reader, "center");
			radius = reader.unmarshalDouble ("radius");
		}
		break;

		}

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
	public SphRegionCircle unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SphRegionCircle obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static SphRegionCircle unmarshal_poly (MarshalReader reader, String name) {
		SphRegionCircle result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SphRegionCircle.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CIRCLE:
			result = new SphRegionCircle();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal object for a single unadorned line of text, internal.

	@Override
	protected void do_marshal_to_line (MarshalWriter writer) {

		// Save center and radius

		center.marshal (writer, "center");
		writer.marshalDouble ("radius", radius);
		return;
	}

	// Unmarshal object for a single unadorned line of text, internal.

	@Override
	protected void do_umarshal_from_line (MarshalReader reader) {

		// Get center and radius

		SphLatLon the_center = (new SphLatLon()).unmarshal (reader, "center");;
		double the_radius = reader.unmarshalDouble ("radius");

		// Set up region

		try {
			setup (the_center, the_radius);
		}
		catch (Exception e) {
			throw new MarshalException ("SphRegionCircle.do_umarshal_from_line: Failed to set up region", e);
		}

		return;
	}

}
