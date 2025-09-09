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
 * Region for locating aftershocks -- Mercator Rectangle.
 * Author: Michael Barall 04/10/2018.
 *
 * The AAFS server locates aftershocks by drawing a region surrounding the
 * mainshock.  Earthquakes within that region are considered to be aftershocks.
 * This class represents a rectangle on a Mercator map.
 * The rectangle is specified by giving two diagonally opposite corners.
 * A rectangle is limited to 180 degrees of longitude.
 * Attempting to make a rectangle spanning more than 180 degrees of longitude
 * will make a rectangle that goes around the Earth the "other way".
 *
 * New: It is now possible to create a rectangle that spans more than 180
 * degrees in longitude, by explicitly giving minimum and maximum latitude
 * and longitude in the constructor.  Due to the fact that longitudes are
 * restricted to be either in the range -180 to +180 or in the range 0 to +360,
 * it is not possible to represent every rectangle that spans more than 180
 * degrees in longitude.
 */
public class SphRegionMercRectangle extends SphRegion {

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
		return contains (loc.get_lat(), loc.get_lon());
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
		return contains (loc.getLatitude(), loc.getLongitude());
	}

	/**
	 * contains - Test if the region contains the given location.
	 * @param lat = Latitude to check.
	 * @param lon = Longitude to check, can be -180 to +360.
	 * @return
	 * Returns true if lat/lon is inside the region, false if lat/lon is outside the region.
	 * Note: Due to rounding errors, it may be indeterminate whether points exactly on,
	 * or very close to, the boundary of the region are considered inside or outside.
	 * Implementation note: The function uses plane geometry, in the domain selected
	 * by plot_wrap.
	 */
	@Override
	public boolean contains (double lat, double lon) {

		// Coerce longitude according to our wrapping domain.

		if (plot_wrap) {
			if (lon < 0.0) {
				lon += 360.0;
			}
		} else {
			if (lon > 180.0) {
				lon -= 360.0;
			}
		}

		// Now just compare to box limits

		return lon >= min_lon && lon <= max_lon && lat >= min_lat && lat <= max_lat;
	}




	//----- Plotting -----

	// This function is called when it is necessary to build plot_border.
	// The subclass should supply a function to build the border.
	// A subclass might choose to build the border in its constructor and
	// do_marshal method, in which case this function can just throw an exception.

	@Override
	protected void make_plot_border () {
		plot_border = new LocationList();
	
		plot_border.add(new Location(min_lat, min_lon));
		plot_border.add(new Location(min_lat, max_lon));
		plot_border.add(new Location(max_lat, max_lon));
		plot_border.add(new Location(max_lat, min_lon));

		return;
	}




	//----- Special regions -----

	/**
	 * Return true if this is a rectangular region in a Mercator projection.
	 * If this function returns true, then the region is exactly the box
	 * given by min_lat, etc.
	 */
	@Override
	public boolean isRectangular() {
		return true;
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SphRegionMercRectangle () {}


	/**
	 * Construct from given corners.
	 * @param corner_1 = Rectangle corner #1.
	 * @param corner_2 = Rectangle corner #2, must be diagonally opposite to corner #1.
	 */
	public SphRegionMercRectangle (SphLatLon corner_1, SphLatLon corner_2) {
		setup (corner_1, corner_2);
	}


	/**
	 * Set up the region.
	 */
	private void setup (SphLatLon corner_1, SphLatLon corner_2) {

		// Get the latitudes and longitudes

		double lat_1 = corner_1.get_lat();
		double lon_1 = corner_1.get_lon();

		double lat_2 = corner_2.get_lat();
		double lon_2 = corner_2.get_lon();

		// Set up a box in the -180 to +180 domain

		plot_wrap = false;

		min_lat = Math.min (lat_1, lat_2);
		max_lat = Math.max (lat_1, lat_2);

		min_lon = Math.min (lon_1, lon_2);
		max_lon = Math.max (lon_1, lon_2);

		// If it spans more than 180 degrees, use the 0 to 360 domain to go around the other way

		if (max_lon - min_lon > 180.0) {
			plot_wrap = true;
		
			min_lon = Math.max (lon_1, lon_2);
			max_lon = Math.min (lon_1, lon_2) + 360.0;
		}

		plot_border = null;

		return;
	}


	// Construct from given ranges of latitude and longitude.

	public SphRegionMercRectangle (double lat_1, double lat_2, double lon_1, double lon_2) {
		setup (lat_1, lat_2, lon_1, lon_2);
	}


	// Set up the region.

	private void setup (double lat_1, double lat_2, double lon_1, double lon_2) {

		// Set up a box in the -180 to +180 domain

		plot_wrap = false;

		min_lat = Math.min (lat_1, lat_2);
		max_lat = Math.max (lat_1, lat_2);

		min_lon = Math.min (lon_1, lon_2);
		max_lon = Math.max (lon_1, lon_2);

		// Check valid latitude range

		if (!( min_lat >= -90.0 && max_lat <= 90.0 )) {
			throw new IllegalArgumentException ("SphRegionMercRectangle.setup: Illegal latitude range: lat_1 = " + lat_1 + ", lat_2 = " + lat_2);
		}

		// Check for valid longitude range in the -180 to +180 domain

		if (min_lon >= -180.0 && max_lon <= 180.0) {
			plot_wrap = false;
		}

		// Check for valid longitude range in the 0 to +360 domain

		else if (min_lon >= 0.0 && max_lon <= 360.0) {

			// If it's the full 0 to +360 range, change to -180 to +180

			if (min_lon == 0.0 && max_lon == 360.0) {
				plot_wrap = false;
				min_lon = -180.0;
				max_lon = 180.0;
			}

			// Otherwise, if it can be shifted into the -180 to +180 domain, do so

			else if (min_lon >= 180.0) {
				plot_wrap = false;
				min_lon -= 360.0;
				max_lon -= 360.0;
			}

			// Otherwise, use the 0 to +360 domain

			else {
				plot_wrap = true;
			}
		}

		else {
			throw new IllegalArgumentException ("SphRegionMercRectangle.setup: Illegal longidude range: lon_1 = " + lon_1 + ", lon_2 = " + lon_2);
		}

		plot_border = null;

		return;
	}


	// Display our contents

	@Override
	public String toString() {
		return "SphRegionMercRectangle:" + "\n"
		+ "plot_wrap = " + plot_wrap + "\n"
		+ "min_lat = " + min_lat + "\n"
		+ "max_lat = " + max_lat + "\n"
		+ "min_lon = " + min_lon + "\n"
		+ "max_lon = " + max_lon;
	}


	// Get parameters that can be displayed to the user.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.

	@Override
	public void get_display_params (Map<String, Object> userParamMap) {
		userParamMap.put ("regionType", "rectangle");
		userParamMap.put ("regionMinLat", SimpleUtils.round_double_via_string ("%.4f", min_lat));
		userParamMap.put ("regionMaxLat", SimpleUtils.round_double_via_string ("%.4f", max_lat));
		userParamMap.put ("regionMinLon", SimpleUtils.round_double_via_string ("%.4f", min_lon));
		userParamMap.put ("regionMaxLon", SimpleUtils.round_double_via_string ("%.4f", max_lon));
		return;
	}


	// Make a region from user display parameters.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	// Returns null if the region could not be created.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.
	// Note: This function catches all exceptions, and returns null if an exception occurs.

	public static SphRegionMercRectangle make_from_display_params (Map<String, Object> userParamMap) {
		SphRegionMercRectangle region = null;
		try {

			// Check the region type

			if (check_string_from_display_params (userParamMap, "regionType", "rectangle")) {

				// Try to make the region

				double lat_1 = get_double_from_display_params (userParamMap, "regionMinLat");
				double lat_2 = get_double_from_display_params (userParamMap, "regionMaxLat");
				double lon_1 = get_double_from_display_params (userParamMap, "regionMinLon");
				double lon_2 = get_double_from_display_params (userParamMap, "regionMaxLon");

				region = new SphRegionMercRectangle (lat_1, lat_2, lon_1, lon_2);
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
	private static final int MARSHAL_VER_1 = 20001;

	private static final String M_VERSION_NAME = "SphRegionMercRectangle";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_MERC_RECTANGLE;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents


		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("SphRegionMercRectangle.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get corners

			SphLatLon corner_1 = (new SphLatLon()).unmarshal (reader, "corner_1");;
			SphLatLon corner_2 = (new SphLatLon()).unmarshal (reader, "corner_2");;

			// Set up region

			try {
				setup (corner_1, corner_2);
			}
			catch (Exception e) {
				throw new MarshalException ("SphRegionMercRectangle.do_umarshal: Failed to set up region", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

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
	public SphRegionMercRectangle unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SphRegionMercRectangle obj) {

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

	public static SphRegionMercRectangle unmarshal_poly (MarshalReader reader, String name) {
		SphRegionMercRectangle result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SphRegionMercRectangle.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_MERC_RECTANGLE:
			result = new SphRegionMercRectangle();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal object for a single unadorned line of text, internal.

	@Override
	protected void do_marshal_to_line (MarshalWriter writer) {

		// Save ranges

		writer.marshalDouble ("min_lat", min_lat);
		writer.marshalDouble ("max_lat", max_lat);
		writer.marshalDouble ("min_lon", min_lon);
		writer.marshalDouble ("max_lon", max_lon);
		return;
	}

	// Unmarshal object for a single unadorned line of text, internal.

	@Override
	protected void do_umarshal_from_line (MarshalReader reader) {

		// Get ranges

		double the_min_lat = reader.unmarshalDouble ("min_lat");
		double the_max_lat = reader.unmarshalDouble ("max_lat");
		double the_min_lon = reader.unmarshalDouble ("min_lon");
		double the_max_lon = reader.unmarshalDouble ("max_lon");

		// Set up region

		try {
			setup (the_min_lat, the_max_lat, the_min_lon, the_max_lon);
		}
		catch (Exception e) {
			throw new MarshalException ("SphRegionMercRectangle.do_umarshal_from_line: Failed to set up region", e);
		}

		return;
	}

}
