package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.opensha.commons.geo.Region;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.geo.BorderType;
import org.opensha.commons.geo.GeoTools;

import org.opensha.commons.data.comcat.ComcatRegion;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.MarshalUtils;
import org.opensha.oaf.util.TestArgs;


/**
 * Region on a sphere.
 * Author: Michael Barall 04/12/2018.
 *
 * The OpenSHA class Region performs plane geometry on an equirectangular
 * projection of the Earth.  Attempting to use Region in AAFS has proven to be
 * error-prone.  This is an abstract class that represents a region on a sphere,
 * with calculations done using spherical geometry.  Subclasses define specific
 * types of region (circle, rectangle, polygon, etc.).
 */
public abstract class SphRegion implements ComcatRegion, Marshalable, MarshalableAsLine {

	//----- Querying -----

	/**
	 * contains - Test if the region contains the given location.
	 * @param loc = Location to check.
	 * @return
	 * Returns true if loc is inside the region, false if loc is outside the region.
	 * Note: Due to rounding errors, it may be indeterminate whether points exactly on,
	 * or very close to, the boundary of the region are considered inside or outside.
	 */
	public abstract boolean contains (SphLatLon loc);

	/**
	 * contains - Test if the region contains the given location.
	 * @param loc = Location to check.
	 * @return
	 * Returns true if loc is inside the region, false if loc is outside the region.
	 * Note: Due to rounding errors, it may be indeterminate whether points exactly on,
	 * or very close to, the boundary of the region are considered inside or outside.
	 */
	@Override
	public abstract boolean contains (Location loc);

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
	public abstract boolean contains (double lat, double lon);




	//----- Plotting -----

	// OpenSHA allows longitude to range from -180 to +360, which means that
	// each point in the western hemisphere has two possible longitudes.  Since
	// SphRegion does spherical geometry, SphRegion and related classes only permit
	// longitude to range from -180 to +180.  This can be a problem when plotting
	// regions that cross the date line.  We address this by defining two plotting
	// domains: one that extends from -180 to +180, and one that extends from 0 to +360.
	// Regions that cross the date line are plotted in the second domain.  Note that
	// both domains lie within the OpenSHA permitted longitude range.
	//
	// If plot_wrap is false, then plots should be drawn by coercing longitudes
	// to lie between -180 and +180.  If plot_wrap is true, then plots should
	// be drawn by coercing longitudes to lie between 0 and +360.
	// This field should be set up by the subclass.

	protected boolean plot_wrap;

	/**
	 * Returns the plot domain.
	 */
	public boolean getPlotWrap() {
		return plot_wrap;
	}

	// These fields define a latitude/longitude bounding box for the region.
	// When plotting, this box can be used to set the limits of the plot.
	// The latitude values must satisfy:
	//  -90 <= min_lat <= max_lat <= +90
	// If plot_wrap is false, then the longitude values must satisfy:
	//  -180 <= min_lon <= max_lon <= +180
	// If plot_wrap is true, then the longitude values must satisfy:
	//  0 <= min_lon <= max_lon <= +360
	// These fields should be set up by the subclass.  A subclass may also
	// choose to use these fields for a "quick reject" of points that are
	// well outside the region.

	protected double min_lat;
	protected double max_lat;
	protected double min_lon;
	protected double max_lon;

	/**
	 * Returns the minimum latitude.
	 */
	@Override
	public double getMinLat() {
		return min_lat;
	}

	/**
	 * Returns the maximum latitude.
	 */
	@Override
	public double getMaxLat() {
		return max_lat;
	}

	/**
	 * Returns the minimum longitude.
	 */
	@Override
	public double getMinLon() {
		return min_lon;
	}

	/**
	 * Returns the maximum longitude.
	 */
	@Override
	public double getMaxLon() {
		return max_lon;
	}

	// A list of points that is used for drawing the region's border on a plot.
	// Each point must be within the bounding box defined by min_lat, etc.
	// This is initialized to null, and built when needed.
	// The last point should not coincide with the first point.
	//
	// Note: This field is typically not marshaled because it can be re-built
	// when needed. The subclass constructor and do_unmarshal and unmarshal_from_line methods must either
	// set plot_border to null, or else build the list.  If they build the list,
	// than make_plot_border can just throw an exception.  This class's
	// constructor and do_unmarshal method set plot_border to null.

	protected LocationList plot_border = null;

	// This function is called when it is necessary to build plot_border.
	// The subclass should supply a function to build the border.
	// A subclass might choose to build the border in its constructor and
	// do_marshal method, in which case this function can just throw an exception.

	protected abstract void make_plot_border ();

	// The subclass can call this function to add a point to the border.
	// The longitude is adjusted to the plotting domain, then the latitude
	// and longitude are clipped to the bounding box.

	protected void add_border_point (SphLatLon loc) {
		double lat = loc.get_lat();
		double lon = loc.get_lon(plot_wrap);

		if (lat < min_lat) {
			lat = min_lat;
		} else if (lat > max_lat) {
			lat = max_lat;
		}

		if (lon < min_lon) {
			lon = min_lon;
		} else if (lon > max_lon) {
			lon = max_lon;
		}

		plot_border.add (new Location (lat, lon));
		return;
	}

	/**
	 * Returns an unmodifiable view of the border.
	 */
	public LocationList getBorder() {
		if (plot_border == null) {
			make_plot_border();
		}
		return plot_border.unmodifiableList();
	}




	//----- Special regions -----

	/**
	 * Return true if this is a rectangular region in a Mercator projection.
	 * If this function returns true, then the region is exactly the box
	 * given by min_lat, etc.
	 */
	@Override
	public boolean isRectangular() {
		return false;
	}

	/**
	 * Return true if this is a circular region on the sphere.
	 * If this function returns true, then the center and radius can be
	 * obtained from getCircleCenter and getCircleRadiusDeg.
	 */
	@Override
	public boolean isCircular() {
		return false;
	}

	/**
	 * If this is a circular region on the sphere, then get the center.
	 * Otherwise, throw an exception.
	 */
	public SphLatLon getCircleCenter() {
		throw new UnsupportedOperationException ("The region is not a circle");
	}

	/**
	 * If this is a circular region on the sphere, then get the center latitude.
	 * The returned value ranges from -90 to +90.
	 * Otherwise, throw an exception.
	 */
	@Override
	public double getCircleCenterLat() {
		return getCircleCenter().get_lat();
	}

	/**
	 * If this is a circular region on the sphere, then get the center longitude.
	 * The returned value ranges from -180 to +180.
	 * Otherwise, throw an exception.
	 */
	@Override
	public double getCircleCenterLon() {
		return getCircleCenter().get_lon();
	}

	/**
	 * If this is a circular region on the sphere, then get the radius in degrees.
	 * The returned value ranges from 0 to +180.
	 */
	@Override
	public double getCircleRadiusDeg() {
		throw new UnsupportedOperationException ("The region is not a circle");
	}

	/**
	 * If this is a circular region on the sphere, then get the radius in kilometers.
	 * The returned value ranges from 0 to one-half of the earth's circumference.
	 */
	public double getCircleRadiusKm() {
		throw new UnsupportedOperationException ("The region is not a circle");
	}

	/**
	 * Return true if this region is the entire world.
	 * If this function returns true, then the region is exactly the box
	 * given by min_lat, etc., and that box has latitude -90 to +90
	 * and longitude -180 to +180.
	 */
	public boolean isWorld() {
		return false;
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SphRegion () {}

	// Display our contents

	@Override
	public String toString() {
		return "SphRegion:" + "\n"
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

	public abstract void get_display_params (Map<String, Object> userParamMap);




	//----- Factory methods -----


	/**
	 * Construct a circle from given center location and radius.
	 * @param center = Center of the circle.
	 * @param radius = Radius of the circle, in kilometers, must be positive and span no more than 180 degrees.
	 */
	public static SphRegion makeCircle (SphLatLon center, double radius) {
		return new SphRegionCircle (center, radius);
	}


	/**
	 * Construct a Mercator rectangle from given corners.
	 * @param corner_1 = Rectangle corner #1.
	 * @param corner_2 = Rectangle corner #2, must be diagonally opposite to corner #1.
	 */
	public static SphRegion makeMercRectangle (SphLatLon corner_1, SphLatLon corner_2) {
		return new SphRegionMercRectangle (corner_1, corner_2);
	}


	// Construct a Mercator rectangle from the given latitude and longitude ranges.
	// Parameters:
	//  lat_1, lat_2 = Latitude range (can be given in either order).
	//  lon_1, lon_2 = Longitude range (can be given in either order).
	// The longitude range must lie entirely within the -180 to +180 domain,
	// or entirely within the 0 to +360 domain.
	// This can create rectangles that span over 180 degrees in longitude, up to 360 degrees.

	public static SphRegion makeMercRectangle (double lat_1, double lat_2, double lon_1, double lon_2) {
		return new SphRegionMercRectangle (lat_1, lat_2, lon_1, lon_2);
	}


	/**
	 * Construct a Mercator polygon from given list of vertices.
	 * @param vertex_list = List of vertices.
	 */
	public static SphRegion makeMercPolygon (List<SphLatLon> vertex_list) {
		return new SphRegionMercPolygon (vertex_list);
	}


	/**
	 * Construct a region that is the entire world.
	 */
	public static SphRegion makeWorld () {
		return new SphRegionWorld ();
	}


	// Make a rectangle which is the bounding box for this region.

	public SphRegionMercRectangle make_bounding_box () {
		return new SphRegionMercRectangle (min_lat, max_lat, min_lon, max_lon);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 12001;

	private static final String M_VERSION_NAME = "SphRegion";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 12000;
	protected static final int MARSHAL_CIRCLE = 13001;
	protected static final int MARSHAL_MERC_POLYGON = 14001;
	protected static final int MARSHAL_MERC_RECTANGLE = 20001;
	protected static final int MARSHAL_GC_POLYGON = 21001;
	protected static final int MARSHAL_WORLD = 33001;
	protected static final int MARSHAL_BOUNDING_BOX = 149001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Strings corresponding to each type code

	protected static final String MARSHAL_NULL_S = "null";
	protected static final String MARSHAL_CIRCLE_S = "circle";
	protected static final String MARSHAL_MERC_POLYGON_S = "polygon";
	protected static final String MARSHAL_MERC_RECTANGLE_S = "rectangle";
	protected static final String MARSHAL_GC_POLYGON_S = "gc_polygon";
	protected static final String MARSHAL_WORLD_S = "world";
	protected static final String MARSHAL_BOUNDING_BOX_S = "bounding_box";

	// Get the type code.

	protected abstract int get_marshal_type ();

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalBoolean ("plot_wrap", plot_wrap);
		writer.marshalDouble  ("min_lat"  , min_lat  );
		writer.marshalDouble  ("max_lat"  , max_lat  );
		writer.marshalDouble  ("min_lon"  , min_lon  );
		writer.marshalDouble  ("max_lon"  , max_lon  );
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		plot_wrap = reader.unmarshalBoolean ("plot_wrap");
		min_lat   = reader.unmarshalDouble  ("min_lat"  );
		max_lat   = reader.unmarshalDouble  ("max_lat"  );
		min_lon   = reader.unmarshalDouble  ("min_lon"  );
		max_lon   = reader.unmarshalDouble  ("max_lon"  );

		plot_border = null;

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
	public SphRegion unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SphRegion obj) {

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

	public static SphRegion unmarshal_poly (MarshalReader reader, String name) {
		SphRegion result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SphRegion.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_CIRCLE:
			result = new SphRegionCircle();
			result.do_umarshal (reader);
			break;

		case MARSHAL_MERC_POLYGON:
			result = new SphRegionMercPolygon();
			result.do_umarshal (reader);
			break;

		case MARSHAL_MERC_RECTANGLE:
			result = new SphRegionMercRectangle();
			result.do_umarshal (reader);
			break;

		case MARSHAL_WORLD:
			result = new SphRegionWorld();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal object to a single unadorned line of text.

	@Override
	public abstract String marshal_to_line ();

	// Unmarshal object from a single unadorned line of text.

	@Override
	public abstract SphRegion unmarshal_from_line (String line);

	// Marshal object to a single unadorned line of text, polymorphic.

	public static String marshal_to_line_poly (SphRegion obj) {
		StringBuilder result = new StringBuilder();

		if (obj == null) {
			result.append (MARSHAL_NULL_S);
		} else {
			int type = obj.get_marshal_type();
			switch (type) {

			default:
				throw new MarshalException ("SphRegion.marshal_to_line_poly: Unknown class type code: type = " + type);

			case MARSHAL_CIRCLE:
				result.append (MARSHAL_CIRCLE_S);
				break;

			case MARSHAL_MERC_POLYGON:
				result.append (MARSHAL_MERC_POLYGON_S);
				break;

			case MARSHAL_MERC_RECTANGLE:
				result.append (MARSHAL_MERC_RECTANGLE_S);
				break;

			case MARSHAL_WORLD:
				result.append (MARSHAL_WORLD_S);
				break;
			}

			String s = obj.marshal_to_line();
			if (s.length() > 0) {
				result.append (" ");
				result.append (s);
			}
		}

		return result.toString();
	}

	// Unmarshal object from a single unadorned line of text, polymorphic.

	public static SphRegion unmarshal_from_line_poly (String line) {
		SphRegion result;

		String s = line.trim();
		String[] w = s.split ("[ \\t]+", 2);
		if (w.length == 0) {
			throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Invalid line: " + s);
		}

		switch (w[0]) {

		default:
			throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Unknown region type in line: " + s);

		case MARSHAL_NULL_S:
			if (w.length != 1) {
				throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Invalid line: " + s);
			}
			result = null;
			break;

		case MARSHAL_CIRCLE_S:
			if (w.length != 2) {
				throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Invalid line: " + s);
			}
			result = new SphRegionCircle();
			result.unmarshal_from_line (w[1]);
			break;

		case MARSHAL_MERC_POLYGON_S:
			if (w.length != 2) {
				throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Invalid line: " + s);
			}
			result = new SphRegionMercPolygon();
			result.unmarshal_from_line (w[1]);
			break;

		case MARSHAL_MERC_RECTANGLE_S:
			if (w.length != 2) {
				throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Invalid line: " + s);
			}
			result = new SphRegionMercRectangle();
			result.unmarshal_from_line (w[1]);
			break;

		case MARSHAL_WORLD_S:
			if (w.length != 1) {
				throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Invalid line: " + s);
			}
			result = new SphRegionWorld();
			result.unmarshal_from_line ("");
			break;
		}

		return result;
	}




	//----- Testing -----




	// Test marshal/unmarshal polymorphic functions.

	private static void test_marshal_poly (SphRegion region) {

		// Display the region

		System.out.println ();
		System.out.println ((region == null) ? "<null>" : (region.toString()));

		// Marshal to string

		System.out.println ();
		System.out.println ("***** Writing to JSON string *****");
		System.out.println ();

		MarshalImpJsonWriter writer = new MarshalImpJsonWriter();
		marshal_poly (writer, null, region);
		writer.check_write_complete ();
		String json_string = writer.get_json_string();

		System.out.println (MarshalUtils.display_valid_json_string (json_string));

		// Unmarshal from string

		System.out.println ();
		System.out.println ("***** Reading from JSON string *****");
		System.out.println ();

		MarshalImpJsonReader reader = new MarshalImpJsonReader (json_string);
		SphRegion region2 = unmarshal_poly (reader,null);
		reader.check_read_complete ();

		System.out.println ((region2 == null) ? "<null>" : (region2.toString()));

		// Marshal to line

		System.out.println ();
		System.out.println ("***** Writing to line *****");
		System.out.println ();

		String line = marshal_to_line_poly (region);
		System.out.println (line);

		// Unmarshal from line

		System.out.println ();
		System.out.println ("***** Reading from line *****");
		System.out.println ();

		SphRegion region3 = unmarshal_from_line_poly (line);
		System.out.println ((region3 == null) ? "<null>" : (region3.toString()));

		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEGUIPartAutomation");




		// Subcommand : Test #1
		// Command format:
		//  test1  lat  lon  radius_km
		// Test marshal/unmarshal for a circle.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for a circle");
			double lat = testargs.get_double ("lat");
			double lon = testargs.get_double ("lon");
			double radius_km = testargs.get_double ("radius_km");
			testargs.end_test();

			// Make the circle

			System.out.println ();
			System.out.println ("***** Making circle *****");

			SphRegion region = SphRegion.makeCircle (new SphLatLon (lat, lon), radius_km);

			// Run tests

			test_marshal_poly (region);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  lat_1  lat_2  lon_1  lon_2
		// Test marshal/unmarshal for a rectangle, specified by corners.
		// Note: Longitudes must be in range -180 to +180.

		if (testargs.is_test ("test2")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for a rectangle, specified by corners");
			double lat_1 = testargs.get_double ("lat_1");
			double lat_2 = testargs.get_double ("lat_2");
			double lon_1 = testargs.get_double ("lon_1");
			double lon_2 = testargs.get_double ("lon_2");
			testargs.end_test();

			// Make the rectangle, specified by corners

			System.out.println ();
			System.out.println ("***** Making rectangle, specified by corners *****");

			SphRegion region = SphRegion.makeMercRectangle (new SphLatLon (lat_1, lon_1), new SphLatLon (lat_2, lon_2));

			// Run tests

			test_marshal_poly (region);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  lat_1  lat_2  lon_1  lon_2
		// Test marshal/unmarshal for a rectangle, specified by ranges.
		// Note: Longitudes must be in range -180 to +180, or in range 0 to +360.

		if (testargs.is_test ("test3")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for a rectangle, specified by ranges");
			double lat_1 = testargs.get_double ("lat_1");
			double lat_2 = testargs.get_double ("lat_2");
			double lon_1 = testargs.get_double ("lon_1");
			double lon_2 = testargs.get_double ("lon_2");
			testargs.end_test();

			// Make the rectangle, specified by ranges

			System.out.println ();
			System.out.println ("***** Making rectangle, specified by ranges *****");

			SphRegion region = SphRegion.makeMercRectangle (lat_1, lat_2, lon_1, lon_2);

			// Run tests

			test_marshal_poly (region);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  lat_1  lon_1 ...
		// Test marshal/unmarshal for a polygon.

		if (testargs.is_test ("test4")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for a polygon");
			double[] coords = testargs.get_double_tuple_array ("coords", -1, 6, 2, "lat", "lon");
			testargs.end_test();

			// Make the polygon

			System.out.println ();
			System.out.println ("***** Making polygon *****");

			List<SphLatLon> vertex_list = new ArrayList<SphLatLon>();
			for (int i = 0; i < coords.length; i += 2) {
				vertex_list.add (new SphLatLon (coords[i], coords[i+1]));
			}
			SphRegion region = SphRegion.makeMercPolygon (vertex_list);

			// Run tests

			test_marshal_poly (region);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Test marshal/unmarshal for world.

		if (testargs.is_test ("test5")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for world");
			testargs.end_test();

			// Make the world

			System.out.println ();
			System.out.println ("***** Making world *****");

			SphRegion region = SphRegion.makeWorld ();

			// Run tests

			test_marshal_poly (region);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6
		// Test marshal/unmarshal for null.

		if (testargs.is_test ("test6")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for null");
			testargs.end_test();

			// Make null

			System.out.println ();
			System.out.println ("***** Making null *****");

			SphRegion region = null;

			// Run tests

			test_marshal_poly (region);

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
