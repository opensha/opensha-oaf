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
 * Region for locating aftershocks -- Entire world.
 * Author: Michael Barall 05/23/2018.
 *
 * This class represents a region that includes the entire world.
 */
public class SphRegionWorld extends SphRegion {

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
		return true;
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
		return true;
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
		return true;
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

	/**
	 * Return true if this region is the entire world.
	 * If this function returns true, then the region is exactly the box
	 * given by min_lat, etc., and that box has latitude -90 to +90
	 * and longitude -180 to +180.
	 */
	@Override
	public boolean isWorld() {
		return true;
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	//public SphRegionWorld () {}


	/**
	 * Construct.
	 */
	public SphRegionWorld () {
		setup ();
	}


	/**
	 * Set up the region.
	 */
	private void setup () {

		// Set up a box in the -180 to +180 domain

		plot_wrap = false;

		min_lat = -90.0;
		max_lat = 90.0;

		min_lon = -180.0;
		max_lon = 180.0;

		plot_border = null;

		return;
	}

	// Display our contents

	@Override
	public String toString() {
		return "SphRegionWorld:" + "\n"
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
		userParamMap.put ("regionType", "world");
		userParamMap.put ("regionSouthLat", min_lat);
		userParamMap.put ("regionNorthLat", max_lat);
		userParamMap.put ("regionWestLon", min_lon);
		userParamMap.put ("regionEastLon", max_lon);
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version
	private static final int MARSHAL_VER_1 = 33001;

	private static final String M_VERSION_NAME = "SphRegionWorld";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_WORLD;
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
			throw new MarshalException ("SphRegionWorld.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Set up region

			try {
				setup ();
			}
			catch (Exception e) {
				throw new MarshalException ("SphRegionWorld.do_umarshal: Failed to set up region", e);
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
	public SphRegionWorld unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, SphRegionWorld obj) {

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

	public static SphRegionWorld unmarshal_poly (MarshalReader reader, String name) {
		SphRegionWorld result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("SphRegionWorld.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_WORLD:
			result = new SphRegionWorld();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal object for a single unadorned line of text, internal.

	@Override
	protected void do_marshal_to_line (MarshalWriter writer) {

		// Save nothing

		return;
	}

	// Unmarshal object for a single unadorned line of text, internal.

	@Override
	protected void do_umarshal_from_line (MarshalReader reader) {

		// Get nothing

		// Set up region

		try {
			setup ();
		}
		catch (Exception e) {
			throw new MarshalException ("SphRegionWorld.do_umarshal_from_line: Failed to set up region", e);
		}

		return;
	}

}
