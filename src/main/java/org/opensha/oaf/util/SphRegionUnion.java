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


// Region for locating aftershocks -- Union.
// Author: Michael Barall.
//
// This class represents a union of other regions.
//
// It contains a list of included regions and a list of excluded regions.
// A point lies within the union if it is contained in at least one of
// the included regions and none of the excluded regions.  There must be
// at least one included region.

public class SphRegionUnion extends SphRegion {

	//----- Region definition -----

	// Included regions.

	private SphRegion[] include;

	// Excluded regions.

	private SphRegion[] exclude;




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
	 */
	@Override
	public boolean contains (double lat, double lon) {

		// Coerce longitude according to our wrapping domain

		if (plot_wrap) {
			if (lon < 0.0) {
				lon += 360.0;
			}
		} else {
			if (lon > 180.0) {
				lon -= 360.0;
			}
		}

		// If outside the box limits, we can return false

		if (!( lon >= min_lon && lon <= max_lon && lat >= min_lat && lat <= max_lat )) {
			return false;
		}

		// Look for an included region that contains the location

		for (SphRegion inc : include) {

			// If region contains the location ...

			if (inc.contains (lat, lon)) {

				// Check that it is not in an exclude region ...

				for (SphRegion exc : exclude) {
					if (exc.contains (lat, lon)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}




	//----- Plotting -----

	// This function is called when it is necessary to build plot_border.
	// The subclass should supply a function to build the border.
	// A subclass might choose to build the border in its constructor and
	// do_marshal method, in which case this function can just throw an exception.

	@Override
	protected void make_plot_border () {
		plot_border = new LocationList();

		// If there is a single include region, return its border

		if (include.length == 1) {
			plot_border.addAll (include[0].getBorder());
		}

		// Otherwise, draw our bounding rectangle.

		else {
			plot_border.add(new Location(min_lat, min_lon));
			plot_border.add(new Location(min_lat, max_lon));
			plot_border.add(new Location(max_lat, max_lon));
			plot_border.add(new Location(max_lat, min_lon));
		}

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
		return include.length == 1 && exclude.length == 0 && include[0].isRectangular();
	}

	/**
	 * Return true if this is a circular region on the sphere.
	 * If this function returns true, then the center and radius can be
	 * obtained from getCircleCenter and getCircleRadiusDeg.
	 */
	@Override
	public boolean isCircular() {
		return include.length == 1 && exclude.length == 0 && include[0].isCircular();
	}

	/**
	 * If this is a circular region on the sphere, then get the center.
	 * Otherwise, throw an exception.
	 */
	public SphLatLon getCircleCenter() {
		if (isCircular()) {
			return include[0].getCircleCenter();
		}
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
		if (isCircular()) {
			return include[0].getCircleRadiusDeg();
		}
		throw new UnsupportedOperationException ("The region is not a circle");
	}

	/**
	 * If this is a circular region on the sphere, then get the radius in kilometers.
	 * The returned value ranges from 0 to one-half of the earth's circumference.
	 */
	public double getCircleRadiusKm() {
		if (isCircular()) {
			return include[0].getCircleRadiusKm();
		}
		throw new UnsupportedOperationException ("The region is not a circle");
	}

	/**
	 * Return true if this region is the entire world.
	 * If this function returns true, then the region is exactly the box
	 * given by min_lat, etc., and that box has latitude -90 to +90
	 * and longitude -180 to +180.
	 */
	public boolean isWorld() {
		return include.length == 1 && exclude.length == 0 && include[0].isWorld();
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

		// If the other region is aunion, do a union comparison

		if (other instanceof SphRegionUnion) {
			return compare_to_union ((SphRegionUnion)other);
		}

		// Otherwise, pass thru

		return super.compare_to (other);
	}


	// Compare this union to the other union.
	// Returns one of the RCMP_XXXX values.
	// Note: Comparisons may use a tolerance to allow for rounding errors.

	public int compare_to_union (SphRegionUnion other) {

		// Check for the same object

		if (this == other) {
			return RCMP_EQUAL;
		}

		// If the bounding boxes are disjoint, report as disjoint

		if (is_disjoint_bbox (other)) {
			return RCMP_DISJOINT;
		}

		// If include list is different, we don't know

		if (!( include.length == other.include.length )) {
			return RCMP_UNKNOWN;
		}

		for (int j = 0; j < include.length; ++j) {
			if (include[j].compare_to (other.include[j]) != RCMP_EQUAL) {
				return RCMP_UNKNOWN;
			}
		}

		// If exclude list is different, we don't know

		if (!( exclude.length == other.exclude.length )) {
			return RCMP_UNKNOWN;
		}

		for (int j = 0; j < exclude.length; ++j) {
			if (exclude[j].compare_to (other.exclude[j]) != RCMP_EQUAL) {
				return RCMP_UNKNOWN;
			}
		}

		// All are equal

		return RCMP_EQUAL;
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public SphRegionUnion () {}


	/**
	 * Construct from given lists of regions.
	 * @param include_list = List of regions to include.
	 * @param exclude_list = List of regions to exclude.
	 */
	public SphRegionUnion (List<SphRegion> include_list, List<SphRegion> exclude_list) {
		include = include_list.toArray (new SphRegion[0]);
		exclude = exclude_list.toArray (new SphRegion[0]);
		setup ();
	}


	/**
	 * Set up the region.
	 * The include and exclude arrays must already exist.
	 */
	private void setup () {

		// Must be at least one include region

		if (include.length == 0) {
			throw new IllegalArgumentException ("SphRegionUnion: Must have at least one include region");
		}

		// Include regions must be non-null

		for (SphRegion inc : include) {
			if (inc == null) {
				throw new IllegalArgumentException ("SphRegionUnion: Found a null include region");
			}
		}

		// Exclude regions must be non-null

		for (SphRegion exc : exclude) {
			if (exc == null) {
				throw new IllegalArgumentException ("SphRegionUnion: Found a null exclude region");
			}
		}

		// If there is one include region, copy its bounding box
		// (Note: Necessary to ensure the border plot lies within the bounding box)

		if (include.length == 1) {
			plot_wrap = include[0].plot_wrap;
			min_lat = include[0].min_lat;
			max_lat = include[0].max_lat;
			min_lon = include[0].min_lon;
			max_lon = include[0].max_lon;
		}

		// Otherwise, form the union of the bounding boxes of the include regions

		else {
			LatitudeAccum lat_accum = new LatitudeAccum();
			LongitudeAccum lon_accum = new LongitudeAccum();

			for (SphRegion inc : include) {
				lat_accum.accum_lat_range (inc);
				lon_accum.accum_lon_range (inc);
			}

			lat_accum.set_lat_range (this);
			lon_accum.set_lon_range (this);
		}

		// Plot border not constructed yet

		plot_border = null;

		return;
	}

	// Display our contents

	@Override
	public String toString() {
		return "SphRegionUnion:" + "\n"
		+ "plot_wrap = " + plot_wrap + "\n"
		+ "min_lat = " + min_lat + "\n"
		+ "max_lat = " + max_lat + "\n"
		+ "min_lon = " + min_lon + "\n"
		+ "max_lon = " + max_lon + "\n"
		+ "include regions = " + include.length + "\n"
		+ "exclude regions = " + exclude.length;
	}


	// Get parameters that can be displayed to the user.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.

	@Override
	public void get_display_params (Map<String, Object> userParamMap) {
		userParamMap.put ("regionType", "union");
		userParamMap.put ("regionIncludeCount", include.length);
		userParamMap.put ("regionExcludeCount", exclude.length);
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 151001;

	private static final String M_VERSION_NAME = "SphRegionUnion";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_UNION;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		SphRegion.marshal_array (writer, "include", include);
		SphRegion.marshal_array (writer, "exclude", exclude);

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("SphRegionUnion.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Contents

			include = SphRegion.unmarshal_array (reader, "include");
			exclude = SphRegion.unmarshal_array (reader, "exclude");

			// Set up region

			try {
				setup ();
			}
			catch (Exception e) {
				throw new MarshalException ("SphRegionUnion.do_umarshal: Failed to set up region", e);
			}
		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			include = SphRegion.unmarshal_array (reader, "include");
			exclude = SphRegion.unmarshal_array (reader, "exclude");
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
	public SphRegionUnion unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SphRegionUnion obj) {

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

	public static SphRegionUnion unmarshal_poly (MarshalReader reader, String name) {
		SphRegionUnion result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SphRegionUnion.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_UNION:
			result = new SphRegionUnion();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal object to a single unadorned line of text.

	@Override
	public String marshal_to_line () {
		//throw new UnsupportedOperationException ("SphRegionUnion.marshal_to_line: Unsupported operation");
		return "test";
	}

	// Unmarshal object from a single unadorned line of text.

	@Override
	public SphRegionUnion unmarshal_from_line (String line) {
		//throw new UnsupportedOperationException ("SphRegionUnion.unmarshal_from_line: Unsupported operation");
		include = new SphRegion[1];
		exclude = new SphRegion[0];
		include[0] = SphRegion.makeWorld();
		setup();
		return this;
	}

}
