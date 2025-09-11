package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;

import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;

import static java.lang.Math.PI;
import static org.opensha.commons.geo.GeoTools.TWOPI;
import static org.opensha.commons.geo.GeoTools.TO_DEG;
import static org.opensha.commons.geo.GeoTools.TO_RAD;
import static org.opensha.commons.geo.GeoTools.EARTH_RADIUS_MEAN;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import static org.opensha.oaf.util.MarshalUtils.mifcn;
import static org.opensha.oaf.util.MarshalUtils.uifcn;
import static org.opensha.oaf.util.MarshalUtils.uibfcn;


/**
 * Class to hold a (lat, lon) pair in degrees, denoting a point on the sphere.
 * Author: Michael Barall 04/13/2018.
 *
 * This is an immutable object.
 */
public class SphLatLon implements Marshalable {

	//----- Contents -----

	// Latitude, in degress, in range -180 to +180.

	private double lat;

	// Longitude, in degress, in range -90 to +90.

	private double lon;




	//----- Construction -----

	// Set the latitude and longitude, return true if success, false if out-of-range.
	// Values slightly outside the range are coerced to be in range.

	private boolean set (double the_lat, double the_lon) {
		lat = the_lat;
		lon = the_lon;

		if (lat < -90.0) {
			if (lat < -90.0001) {
				return false;
			}
			lat = -90.0;
		} else if (lat > 90.0) {
			if (lat > 90.0001) {
				return false;
			}
			lat = 90.0;
		}

		if (lon < -180.0) {
			if (lon < -180.0001) {
				return false;
			}
			lon = -180.0;
		} else if (lon > 180.0) {
			if (lon > 180.0001) {
				return false;
			}
			lon = 180.0;
		}

		return true;
	}

	// Default constructor.

	public SphLatLon () {}

	// Construct from given latitude and longitude.

	public SphLatLon (double lat, double lon) {
		if (!( set (lat, lon) )) {
			throw new IllegalArgumentException ("SphLatLon: Coordinates out-of-range: lat = " + lat + ", lon = " + lon);
		}
	}

	// Construct from a Location.

	public SphLatLon (Location loc) {
		double the_lat = loc.getLatitude();
		double the_lon = loc.getLongitude();

		// Location permits longitudes -180 to +360, so do the wrapping
		if (the_lon > 180.0) {
			the_lon -= 360.0;
		}

		if (!( set (the_lat, the_lon) )) {
			throw new IllegalArgumentException ("SphLatLon: Coordinates out-of-range: lat = " + the_lat + ", lon = " + the_lon);
		}
	}

	// Display our contents

	@Override
	public String toString() {
		return "SphLatLon:" + "\n"
		+ "lat = " + lat + "\n"
		+ "lon = " + lon;
	}




	//----- Getters -----

	// Get latitude.

	public final double get_lat () {
		return lat;
	}

	// Get longitude.

	public final double get_lon () {
		return lon;
	}

	// Get longitude.
	// If f_wrap_lon is true, the result lies between 0 and +360.
	// If f_wrap_lon is false, the result lies between -180 and +180.

	public final double get_lon (boolean f_wrap_lon) {
		return (f_wrap_lon && lon < 0.0) ? (lon + 360.0) : lon;
	}

	// Get latitude, in radians.

	public final double get_lat_rad () {
		return lat * TO_RAD;
	}

	// Get longitude, in radians.

	public final double get_lon_rad () {
		return lon * TO_RAD;
	}

	// Get longitude, in radians.
	// If f_wrap_lon is true, the result lies between 0 and +360.
	// If f_wrap_lon is false, the result lies between -180 and +180.

	public final double get_lon_rad (boolean f_wrap_lon) {
		return ((f_wrap_lon && lon < 0.0) ? (lon + 360.0) : lon) * TO_RAD;
	}




	//----- Services for Location -----

	// Get location.

	public final Location get_location () {
		return new Location (get_lat(), get_lon());
	}

	// Get location.
	// If f_wrap_lon is true, the longitude lies between 0 and +360.
	// If f_wrap_lon is false, the longitude lies between -180 and +180.

	public final Location get_location (boolean f_wrap_lon) {
		return new Location (get_lat(), get_lon(f_wrap_lon));
	}

	// Get longitude from a Location.
	// If f_wrap_lon is true, the result lies between 0 and +360.
	// If f_wrap_lon is false, the result lies between -180 and +180.

	public static double get_lon (Location loc, boolean f_wrap_lon) {
		double lon = loc.getLongitude();
		if (f_wrap_lon) {
			if (lon < 0.0) {
				lon += 360.0;
			}
		}
		else {
			if (lon > 180.0) {
				lon -= 360.0;
			}
		}
		return lon;
	}

	// Force longitude in a Location to lie in the selected domain.
	// If f_wrap_lon is true, the result has longitude between 0 and +360.
	// If f_wrap_lon is false, the result has longitude between -180 and +180.
	// Note: If the longitude is already in the desired domain, then loc is returned.

	public static Location wrap_location (Location loc, boolean f_wrap_lon) {
		double lon = loc.getLongitude();
		if (f_wrap_lon) {
			if (lon < 0.0) {
				return new Location (loc.getLatitude(), lon + 360.0, loc.getDepth());
			}
		}
		else {
			if (lon > 180.0) {
				return new Location (loc.getLatitude(), lon - 360.0, loc.getDepth());
			}
		}
		return loc;
	}

	// Force longitude for each Location in a list to lie in the selected domain.
	// If f_wrap_lon is true, then each result has longitude between 0 and +360.
	// If f_wrap_lon is false, then each result has longitude between -180 and +180.
	// Note: Returns a newly-allocated LocationList.  Elements of the list may include
	// both Location objects from the original list (if they are already in the
	// desired domain) and newly-allocated Location objects.

	public static LocationList wrap_location_list (List<Location> locs, boolean f_wrap_lon) {
		LocationList result = new LocationList();
		for (Location loc : locs) {
			result.add (wrap_location (loc, f_wrap_lon));
		}
		return result;
	}




	//----- Spherical geometry -----


	// Earth radius used by Comcat for comverting between km and degrees.
	// Note: Comcat uses exactly 111.12 km per degree.
	// Remark: The radius used by ComCat is a value that historically has been used for navigation of ships and aircraft.
	// Historically, one degree equals 60 nautical miles, and a nautical mile is currently defined to be 1852 meters.

	public static final double EARTH_RADIUS_COMCAT = 111.12 * Math.toDegrees(1.0);


	/**
	 * Calculates the angle between two points using the haversine formula.
	 * http://en.wikipedia.org/wiki/Haversine_formula
	 * This method properly handles values spanning 180 degrees longitude.
	 * See http://www.edwilliams.org/avform.htm Aviation Formulary for source.
	 * Result is returned in radians.
	 * Note: Same code as in LocationUtils.java.
	 * 
	 * @param p1 the first point
	 * @param p2 the second point
	 * @return the angle between the points (in radians)
	 */
	public static double angle_rad (SphLatLon p1, SphLatLon p2) {
		double lat1 = p1.get_lat_rad();
		double lat2 = p2.get_lat_rad();
		double sinDlatBy2 = Math.sin((lat2 - lat1) / 2.0);
		double sinDlonBy2 = Math.sin((p2.get_lon_rad() - p1.get_lon_rad()) / 2.0);
		// half length of chord connecting points
		double c = (sinDlatBy2 * sinDlatBy2) +
			(Math.cos(lat1) * Math.cos(lat2) * sinDlonBy2 * sinDlonBy2);
		return 2.0 * Math.atan2(Math.sqrt(c), Math.sqrt(1.0 - c));
	}

	/**
	 * Version that accepts one SphLatLon and one Location.
	 */
	public static double angle_rad (SphLatLon p1, Location p2) {
		double lat1 = p1.get_lat_rad();
		double lat2 = p2.getLatRad();
		double sinDlatBy2 = Math.sin((lat2 - lat1) / 2.0);
		double sinDlonBy2 = Math.sin((p2.getLonRad() - p1.get_lon_rad()) / 2.0);
		// half length of chord connecting points
		double c = (sinDlatBy2 * sinDlatBy2) +
			(Math.cos(lat1) * Math.cos(lat2) * sinDlonBy2 * sinDlonBy2);
		return 2.0 * Math.atan2(Math.sqrt(c), Math.sqrt(1.0 - c));
	}

	/**
	 * Version that accepts one SphLatLon and latitude/longitude in degrees.
	 */
	public static double angle_rad (SphLatLon p1, double lat, double lon) {
		double lat1 = p1.get_lat_rad();
		double lat2 = lat * TO_RAD;
		double sinDlatBy2 = Math.sin((lat2 - lat1) / 2.0);
		double sinDlonBy2 = Math.sin((lon * TO_RAD - p1.get_lon_rad()) / 2.0);
		// half length of chord connecting points
		double c = (sinDlatBy2 * sinDlatBy2) +
			(Math.cos(lat1) * Math.cos(lat2) * sinDlonBy2 * sinDlonBy2);
		return 2.0 * Math.atan2(Math.sqrt(c), Math.sqrt(1.0 - c));
	}

	/**
	 * Version that accepts two latitude/longitude pairs in degrees.
	 */
	public static double angle_rad (double lat1, double lon1, double lat2, double lon2) {
		double lat1_rad = lat1 * TO_RAD;
		double lat2_rad = lat2 * TO_RAD;
		double sinDlatBy2 = Math.sin((lat2_rad - lat1_rad) / 2.0);
		double sinDlonBy2 = Math.sin((lon2 * TO_RAD - lon1 * TO_RAD) / 2.0);
		// half length of chord connecting points
		double c = (sinDlatBy2 * sinDlatBy2) +
			(Math.cos(lat1_rad) * Math.cos(lat2_rad) * sinDlonBy2 * sinDlonBy2);
		return 2.0 * Math.atan2(Math.sqrt(c), Math.sqrt(1.0 - c));
	}

	/**
	 * Calculates the great circle surface distance between two
	 * points using the haversine formula for computing the
	 * angle between two points.
	 * Note: Same code as in LocationUtils.java.
	 * 
	 * @param p1 the first point
	 * @param p2 the second point
	 * @return the distance between the points in km
	 */
	public static double horzDistance (SphLatLon p1, SphLatLon p2) {
		return EARTH_RADIUS_COMCAT * angle_rad (p1, p2);
	}

	/**
	 * Version that accepts one SphLatLon and one Location.
	 */
	public static double horzDistance (SphLatLon p1, Location p2) {
		return EARTH_RADIUS_COMCAT * angle_rad (p1, p2);
	}

	/**
	 * Version that accepts one SphLatLon and latitude/longitude in degrees.
	 */
	public static double horzDistance (SphLatLon p1, double lat, double lon) {
		return EARTH_RADIUS_COMCAT * angle_rad (p1, lat, lon);
	}

	/**
	 * Version that accepts two latitude/longitude pairs in degrees.
	 */
	public static double horzDistance (double lat1, double lon1, double lat2, double lon2) {
		return EARTH_RADIUS_COMCAT * angle_rad (lat1, lon1, lat2, lon2);
	}

	/**
	 * Convert horizontal distance to radians.
	 */
	public static double distance_to_rad (double distance) {
		return distance / EARTH_RADIUS_COMCAT;
	}

	/**
	 * Convert horizontal distance to degrees.
	 */
	public static double distance_to_deg (double distance) {
		return (distance / EARTH_RADIUS_COMCAT) * TO_DEG;
	}

	/**
	 * Calculates the endpoint when traveling along a great circle,
	 * with a given starting point, azimuth, and angular distance traveled.
	 * Note: Derived from code in LocationUtils.java.
	 * 
	 * @param lat_rad = Starting point latitude, in radians.
	 * @param lon_rad = Starting point longitude, in radians.
	 * @param az_rad = Initial azimuth, in radians; 0 = north, pi/2 = east.
	 * @param dist_rad = Angular distance traveled, in radians.
	 * @return
	 * Returns the ending point.
	 * Note: Correctly handles angles with any value (angular range is not restricted).
	 */
	public static SphLatLon gc_travel_rad (double lat_rad, double lon_rad, double az_rad, double dist_rad) {

		double sinLat1 = Math.sin(lat_rad);
		double cosLat1 = Math.cos(lat_rad);
		double sinD = Math.sin(dist_rad);
		double cosD = Math.cos(dist_rad);

		double lat2 = Math.asin(sinLat1 * cosD + cosLat1 * sinD * Math.cos(az_rad));

		double lon2 = lon_rad +
			Math.atan2(Math.sin(az_rad) * sinD * cosLat1,
				cosD - sinLat1 * Math.sin(lat2));

		lat2 = lat2 * TO_DEG;
		lon2 = lon2 * TO_DEG;

		while (lon2 < -180.0) {
			lon2 += 360.0;
		}
		while (lon2 > 180.0) {
			lon2 -= 360.0;
		}

		return new SphLatLon (lat2, lon2);
	}

	/**
	 * Calculates the endpoint when traveling along a great circle,
	 * with a given starting point, azimuth, and kilometer distance traveled.
	 * Note: Derived from code in LocationUtils.java.
	 * 
	 * @param lat_rad = Starting point latitude, in radians.
	 * @param lon_rad = Starting point longitude, in radians.
	 * @param az_rad = Initial azimuth, in radians; 0 = north, pi/2 = east.
	 * @param dist_km = Distance traveled, in kilometers.
	 * @return
	 * Returns the ending point.
	 * Note: Correctly handles angles with any value (angular range is not restricted).
	 */
	public static SphLatLon gc_travel_km (double lat_rad, double lon_rad, double az_rad, double dist_km) {
		return gc_travel_rad (lat_rad, lon_rad, az_rad, dist_km / EARTH_RADIUS_COMCAT);
	}

	/**
	 * Computes the initial azimuth (bearing) when moving from one point to another.
	 * See http://www.edwilliams.org/avform.htm Aviation Formulary for source.
	 * Result is returned in radians over the interval 0 to 2*pi.
	 * Note: Derived from code in LocationUtils.java.
	 * 
	 * @param p1 = The first point.
	 * @param p2 = The second point.
	 * @return
	 * Returns the azimuth (bearing) from p1 to p2 in radians, from 0 to 2*pi (0 = north, pi/2 = east).
	 * Note: Correctly handles angles with any value (angular range is not restricted).
	 */
	public static double azimuth_rad (SphLatLon p1, SphLatLon p2) {

		double lat1_rad = p1.get_lat_rad();
		double lat2_rad = p2.get_lat_rad();

		double cosLat1 = Math.cos(lat1_rad);
		double cosLat2 = Math.cos(lat2_rad);

		double sinLat1 = Math.sin(lat1_rad);
		double sinLat2 = Math.sin(lat2_rad);

		// If either point lies within ~ 1 cm of the N or S pole, force 0 or pi return

		if (Math.abs(cosLat1) < 1.745e-8 || Math.abs(cosLat2) < 1.745e-8) {
			return ((sinLat1 > sinLat2) ? PI : 0); // N : S pole
		}

		// For start or end points other than the poles

		double dLon = p2.get_lon_rad() - p1.get_lon_rad();

		double azRad = Math.atan2(Math.sin(dLon) * cosLat2,
			cosLat1 * sinLat2 - sinLat1 * cosLat2 * Math.cos(dLon));

		if (azRad < 0.0) {
			azRad += TWOPI;
		}

		return azRad;
	}

	/**
	 * Version that accepts two Location.
	 */
	public static double azimuth_rad (Location p1, Location p2) {

		double lat1_rad = p1.getLatRad();
		double lat2_rad = p2.getLatRad();

		double cosLat1 = Math.cos(lat1_rad);
		double cosLat2 = Math.cos(lat2_rad);

		double sinLat1 = Math.sin(lat1_rad);
		double sinLat2 = Math.sin(lat2_rad);

		// If either point lies within ~ 1 cm of the N or S pole, force 0 or pi return

		if (Math.abs(cosLat1) < 1.745e-8 || Math.abs(cosLat2) < 1.745e-8) {
			return ((sinLat1 > sinLat2) ? PI : 0); // N : S pole
		}

		// For start or end points other than the poles

		double dLon = p2.getLonRad() - p1.getLonRad();

		double azRad = Math.atan2(Math.sin(dLon) * cosLat2,
			cosLat1 * sinLat2 - sinLat1 * cosLat2 * Math.cos(dLon));

		if (azRad < 0.0) {
			azRad += TWOPI;
		}

		return azRad;
	}

	/**
	 * Version that accepts one SphLatLon and one Location.
	 */
	public static double azimuth_rad (SphLatLon p1, Location p2) {

		double lat1_rad = p1.get_lat_rad();
		double lat2_rad = p2.getLatRad();

		double cosLat1 = Math.cos(lat1_rad);
		double cosLat2 = Math.cos(lat2_rad);

		double sinLat1 = Math.sin(lat1_rad);
		double sinLat2 = Math.sin(lat2_rad);

		// If either point lies within ~ 1 cm of the N or S pole, force 0 or pi return

		if (Math.abs(cosLat1) < 1.745e-8 || Math.abs(cosLat2) < 1.745e-8) {
			return ((sinLat1 > sinLat2) ? PI : 0); // N : S pole
		}

		// For start or end points other than the poles

		double dLon = p2.getLonRad() - p1.get_lon_rad();

		double azRad = Math.atan2(Math.sin(dLon) * cosLat2,
			cosLat1 * sinLat2 - sinLat1 * cosLat2 * Math.cos(dLon));

		if (azRad < 0.0) {
			azRad += TWOPI;
		}

		return azRad;
	}

	/**
	 * Version that accepts one SphLatLon and latitude/longitude in degrees.
	 */
	public static double azimuth_rad (SphLatLon p1, double lat, double lon) {

		double lat1_rad = p1.get_lat_rad();
		double lat2_rad = lat * TO_RAD;

		double cosLat1 = Math.cos(lat1_rad);
		double cosLat2 = Math.cos(lat2_rad);

		double sinLat1 = Math.sin(lat1_rad);
		double sinLat2 = Math.sin(lat2_rad);

		// If either point lies within ~ 1 cm of the N or S pole, force 0 or pi return

		if (Math.abs(cosLat1) < 1.745e-8 || Math.abs(cosLat2) < 1.745e-8) {
			return ((sinLat1 > sinLat2) ? PI : 0); // N : S pole
		}

		// For start or end points other than the poles

		double dLon = lon * TO_RAD - p1.get_lon_rad();

		double azRad = Math.atan2(Math.sin(dLon) * cosLat2,
			cosLat1 * sinLat2 - sinLat1 * cosLat2 * Math.cos(dLon));

		if (azRad < 0.0) {
			azRad += TWOPI;
		}

		return azRad;
	}

	/**
	 * Version that accepts two latitude/longitude pairs in degrees.
	 */
	public static double azimuth_rad (double lat1, double lon1, double lat2, double lon2) {

		double lat1_rad = lat1 * TO_RAD;
		double lat2_rad = lat2 * TO_RAD;

		double cosLat1 = Math.cos(lat1_rad);
		double cosLat2 = Math.cos(lat2_rad);

		double sinLat1 = Math.sin(lat1_rad);
		double sinLat2 = Math.sin(lat2_rad);

		// If either point lies within ~ 1 cm of the N or S pole, force 0 or pi return

		if (Math.abs(cosLat1) < 1.745e-8 || Math.abs(cosLat2) < 1.745e-8) {
			return ((sinLat1 > sinLat2) ? PI : 0); // N : S pole
		}

		// For start or end points other than the poles

		double dLon = lon2 * TO_RAD - lon1 * TO_RAD;

		double azRad = Math.atan2(Math.sin(dLon) * cosLat2,
			cosLat1 * sinLat2 - sinLat1 * cosLat2 * Math.cos(dLon));

		if (azRad < 0.0) {
			azRad += TWOPI;
		}

		return azRad;
	}

	/**
	 * Computes the initial azimuth (bearing) when moving from one point to another.
	 * See http://www.edwilliams.org/avform.htm Aviation Formulary for source.
	 * Result is returned in degrees over the interval 0 to 360.
	 * Note: Derived from code in LocationUtils.java.
	 * 
	 * @param p1 = The first point.
	 * @param p2 = The second point.
	 * @return
	 * Returns the azimuth (bearing) from p1 to p2 in degrees, from 0 to 360 (0 = north, 90 = east).
	 * Note: Correctly handles angles with any value (angular range is not restricted).
	 */
	public static double azimuth_deg (SphLatLon p1, SphLatLon p2) {
		double az_deg = azimuth_rad (p1, p2) * TO_DEG;
		if (az_deg > 360.0) {
			az_deg -= 360.0;
		}
		return az_deg;
	}

	/**
	 * Version that accepts two Location.
	 */
	public static double azimuth_deg (Location p1, Location p2) {
		double az_deg = azimuth_rad (p1, p2) * TO_DEG;
		if (az_deg > 360.0) {
			az_deg -= 360.0;
		}
		return az_deg;
	}

	/**
	 * Version that accepts one SphLatLon and one Location.
	 */
	public static double azimuth_deg (SphLatLon p1, Location p2) {
		double az_deg = azimuth_rad (p1, p2) * TO_DEG;
		if (az_deg > 360.0) {
			az_deg -= 360.0;
		}
		return az_deg;
	}

	/**
	 * Version that accepts one SphLatLon and latitude/longitude in degrees.
	 */
	public static double azimuth_deg (SphLatLon p1, double lat, double lon) {
		double az_deg = azimuth_rad (p1, lat, lon) * TO_DEG;
		if (az_deg > 360.0) {
			az_deg -= 360.0;
		}
		return az_deg;
	}

	/**
	 * Version that accepts two latitude/longitude pairs in degrees.
	 */
	public static double azimuth_deg (double lat1, double lon1, double lat2, double lon2) {
		double az_deg = azimuth_rad (lat1, lon1, lat2, lon2) * TO_DEG;
		if (az_deg > 360.0) {
			az_deg -= 360.0;
		}
		return az_deg;
	}





	/**
	 * Version that accepts two Location points.
	 *
	 * Note: Same code as in LocationUtils.java, duplicated so we can change the radius.
	 */
	public static double angle_rad (Location p1, Location p2) {
		double lat1 = p1.getLatRad();
		double lat2 = p2.getLatRad();
		double sinDlatBy2 = Math.sin((lat2 - lat1) / 2.0);
		double sinDlonBy2 = Math.sin((p2.getLonRad() - p1.getLonRad()) / 2.0);
		// half length of chord connecting points
		double c = (sinDlatBy2 * sinDlatBy2) +
			(Math.cos(lat1) * Math.cos(lat2) * sinDlonBy2 * sinDlonBy2);
		return 2.0 * Math.atan2(Math.sqrt(c), Math.sqrt(1.0 - c));
	}

	/**
	 * Calculates the great circle surface distance between two
	 * Location points using the Haversine formula for computing the
	 * angle between two points. For a faster, but less accurate implementation
	 * at large separations, see horzDistanceFast(Location, Location).
	 *
	 * Note: Same code as in LocationUtils.java, duplicated so we can change the radius.
	 * 
	 * @param p1 the first Location point
	 * @param p2 the second Location point
	 * @return the distance between the points in km
	 */
	public static double horzDistance (Location p1, Location p2) {
		return EARTH_RADIUS_COMCAT * angle_rad (p1, p2);
	}

	/**
	 * Calculates approximate distance between two Location points. This
	 * method is about 2 orders of magnitude faster than
	 * horzDistance(), but is imprecise at large distances. Method
	 * uses the latitudinal and longitudinal differences between the points as
	 * the sides of a right triangle. The longitudinal distance is scaled by the
	 * cosine of the mean latitude.
	 * 
	 * Note: This method does NOT support values spanning
	 * +/- 180 degrees and fails where the numeric angle exceeds 180 degrees.
	 * Convert data to the 0-360 degree interval or use
	 * horzDistance() in such instances.
	 *
	 * Note: Same code as in LocationUtils.java, duplicated so we can change the radius.
	 * 
	 * @param p1 the first Location point
	 * @param p2 the second Location point
	 * @return the distance between the points in km
	 */
	public static double horzDistanceFast (Location p1, Location p2) {
		// modified from J. Zechar:
		// calculates distance between two points, using formula
		// as specifed by P. Shebalin via email 5.8.2004
		double lat1 = p1.getLatRad();
		double lat2 = p2.getLatRad();
		double dLat = lat1 - lat2;
		double dLon = (p1.getLonRad() - p2.getLonRad()) *
			Math.cos((lat1 + lat2) * 0.5);
		return EARTH_RADIUS_COMCAT * Math.sqrt((dLat * dLat) + (dLon * dLon));
	}




	// Return true if two points have both coordinates equal to within a tolerance.

	public boolean equals_tol (SphLatLon other, double tol) {
		return (Math.abs (this.lat - other.lat) <= tol && Math.abs (this.lon - other.lon) <= tol);
	}

	public static boolean equals_tol (SphLatLon p1, SphLatLon p2, double tol) {
		return (Math.abs (p1.lat - p2.lat) <= tol && Math.abs (p1.lon - p2.lon) <= tol);
	}


	// Return true if two lists of points have coordinates equal to within a tolerance.

	public static boolean equals_tol (List<SphLatLon> plist1, List<SphLatLon> plist2, double tol) {
		int n = plist1.size();
		if (n != plist2.size()) {
			return false;
		}
		for (int j = 0; j < n; ++j) {
			if (!( equals_tol (plist1.get(j), plist2.get(j), tol) )) {
				return false;
			}
		}
		return true;
	}




	//----- Marshaling -----

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		writer.marshalDouble ("lat", lat);
		writer.marshalDouble ("lon", lon);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public SphLatLon unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		double the_lat = reader.unmarshalDouble ("lat");
		double the_lon = reader.unmarshalDouble ("lon");
		if (!( set (the_lat, the_lon) )) {
			throw new MarshalException ("SphLatLon: Coordinates out-of-range: lat = " + the_lat + ", lon = " + the_lon);
		}
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal a list of objects.

	public static void marshal_list (MarshalWriter writer, String name, List<SphLatLon> obj_list) {
	//	int n = obj_list.size();
	//	writer.marshalArrayBegin (name, n);
	//	for (SphLatLon obj : obj_list) {
	//		obj.marshal (writer, null);
	//	}
	//	writer.marshalArrayEnd ();

		writer.marshalObjectCollection (name, obj_list, mifcn(SphLatLon::marshal));
		return;
	}

	// Unmarshal a list of objects.

	public static ArrayList<SphLatLon> unmarshal_list (MarshalReader reader, String name) {
		ArrayList<SphLatLon> obj_list = new ArrayList<SphLatLon>();

	//	int n = reader.unmarshalArrayBegin (name);
	//	for (int i = 0; i < n; ++i) {
	//		obj_list.add ((new SphLatLon()).unmarshal (reader, null));
	//	}
	//	reader.unmarshalArrayEnd ();

		reader.unmarshalObjectCollection (name, obj_list, uifcn(SphLatLon::unmarshal, SphLatLon::new));
		return obj_list;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("SphLatLon : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  lat1  lon1  lat2  lon2
		// Compute the distance and azimuth between two given points.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 4 additional arguments

			if (!( args.length == 5 )) {
				System.err.println ("SphLatLon : Invalid 'test1' subcommand");
				return;
			}

			try {

				double lat1 = Double.parseDouble (args[1]);
				double lon1 = Double.parseDouble (args[2]);
				double lat2 = Double.parseDouble (args[3]);
				double lon2 = Double.parseDouble (args[4]);

				// Say hello

				System.out.println ("Computing distance and azimuth");
				System.out.println ("lat1 = " + lat1);
				System.out.println ("lon1 = " + lon1);
				System.out.println ("lat2 = " + lat2);
				System.out.println ("lon2 = " + lon2);

				// Make the points

				SphLatLon p1 = new SphLatLon (lat1, lon1);
				SphLatLon p2 = new SphLatLon (lat2, lon2);

				// Angle between the two points

				System.out.println();
				System.out.println ("Angle in degrees = " + angle_rad (p1, p2) * TO_DEG);

				// Distance between the two points

				System.out.println();
				System.out.println ("Distance in km = " + horzDistance (p1, p2));

				// Azimuth between the two points

				System.out.println();
				System.out.println ("Azimuth in degrees = " + azimuth_deg (p1, p2));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Display the values of some constants.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 0 additional arguments

			if (!( args.length == 1 )) {
				System.err.println ("SphLatLon : Invalid 'test2' subcommand");
				return;
			}

			try {

				// Say hello

				System.out.println ("Displaying some constants");

				// Constants to display

				System.out.println();
				System.out.println ("EARTH_RADIUS_MEAN = " + EARTH_RADIUS_MEAN);

				System.out.println();
				System.out.println ("EARTH_RADIUS_COMCAT = " + EARTH_RADIUS_COMCAT);

				System.out.println();
				System.out.println ("TO_DEG = " + TO_DEG);

				System.out.println();
				System.out.println ("TO_RAD = " + TO_RAD);

				System.out.println();
				System.out.println ("PI = " + PI);

				System.out.println();
				System.out.println ("TWOPI = " + TWOPI);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("SphLatLon : Unrecognized subcommand : " + args[0]);
		return;

	}




}
