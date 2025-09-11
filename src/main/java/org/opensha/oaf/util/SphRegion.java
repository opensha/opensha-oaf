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
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;
import static org.opensha.oaf.util.MarshalUtils.mifcn;
import static org.opensha.oaf.util.MarshalUtils.uifcn;
import static org.opensha.oaf.util.MarshalUtils.uibfcn;
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
public abstract class SphRegion implements ComcatRegion, Marshalable {

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
	// when needed. The subclass constructor and do_unmarshal and do_umarshal_from_line methods must either
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




	//----- Comparison -----


	// Possible results of a region comparison.
	// Note: Regions that touch on the boundary are considered to intersect.

	public static final int RCMP_UNKNOWN			= 0;	// Result unknown
	public static final int RCMP_EQUAL				= 1;	// Regions are equal
	public static final int RCMP_DISJOINT			= 2;	// Regions are disjoint
	public static final int RCMP_SUBSET				= 3;	// This region (#1) is a subset of the other region (#2)
	public static final int RCMP_CONTAINS			= 4;	// This region (#1) contains the other region (#2)
	public static final int RCMP_OVERLAP			= 5;	// Regions intersect, but neither contains the other
	//public static final int RCMP_SUBSET_OR_EQUAL	= 6;	// This region (#1) is a subset of the other region (#2), and they may be equal
	//public static final int RCMP_CONTAINS_OR_EQUAL	= 7;	// This region (#1) contains the other region (#2), and they may be equal
	//public static final int RCMP_INTERSECT		= 8;	// Regions intersect, but it is unknown if either contains the other or if they are equal


	// Default tolerance for region comparisons.

	public static final double DEF_TOL_RCMP = 0.000005;


	// Return true if the result code indicates it is known this region is not a subset of the other region.

	public static boolean is_known_non_subset (int rcmp) {
		switch (rcmp) {
		case RCMP_DISJOINT:
		case RCMP_CONTAINS:
		case RCMP_OVERLAP:
			return true;
		}
		return false;
	}


	// Compare this region's bounding box to the other region's bounding box.
	// Returns one of the RCMP_XXXX values.
	// Note: Bounding box borders are considered to coincide if they agree within a tolerance.

	public int compare_bbox_to (SphRegion other) {

		// Check for the same object

		if (this == other) {
			return RCMP_EQUAL;
		}

		// Compare latitude ranges

		int lat_rcmp = compare_coord_ranges_tol (min_lat, max_lat, other.min_lat, other.max_lat);

		if (lat_rcmp == RCMP_DISJOINT) {
			return RCMP_DISJOINT;
		}

		// Compare longitude ranges

		int lon_rcmp = compare_lon_ranges (min_lon, max_lon, other.min_lon, other.max_lon);

		if (lon_rcmp == RCMP_DISJOINT) {
			return RCMP_DISJOINT;
		}

		// Return the combination

		return combine_rcmp (lat_rcmp, lon_rcmp);
	}


	// Return true if this object's bounding box is disjoint from the other object's bounding box.
	// Note: Bounding boxes are disjoint if they are separated by at least a tolerance.

	public boolean is_disjoint_bbox (SphRegion other) {

		// Check for the same object

		if (this == other) {
			return false;
		}

		// Disjoint if either latitude or longitude range is disjoint

		return (is_disjoint_coord_ranges_tol (min_lat, max_lat, other.min_lat, other.max_lat)
				|| is_disjoint_lon_ranges (min_lon, max_lon, other.min_lon, other.max_lon));
	}


	// Compare this region to the other region.
	// Returns one of the RCMP_XXXX values.
	// Note: Comparisons may use a tolerance to allow for rounding errors.
	// Note: This function returns a result other than RCMP_UNKNOWN only in cases
	// where the comparison can be done relatively easily.
	// Note: The default method uses only the bounding box and the is-rectangle flags.

	public int compare_to (SphRegion other) {

		// Check for the same object

		if (this == other) {
			return RCMP_EQUAL;
		}

		// Compare the bounding boxes

		int rcmp = compare_bbox_to (other);

		// Apply the is-rectangle flags

		switch (rcmp) {

		case RCMP_EQUAL:
			if (this.isRectangular()) {
				if (!( other.isRectangular() )) {
					rcmp = RCMP_CONTAINS;
				}
			} else {
				if (other.isRectangular()) {
					rcmp = RCMP_SUBSET;
				} else {
					rcmp = RCMP_UNKNOWN;
				}
			}
			break;

		case RCMP_SUBSET:
			if (!( other.isRectangular() )) {
				rcmp = RCMP_UNKNOWN;
			}
			break;

		case RCMP_CONTAINS:
			if (!( this.isRectangular() )) {
				rcmp = RCMP_UNKNOWN;
			}
			break;

		case RCMP_OVERLAP:
			if (!( this.isRectangular() && other.isRectangular() )) {
				rcmp = RCMP_UNKNOWN;
			}
			break;
		}

		return rcmp;
	}


	// Compare coordinate ranges.
	// Parameters:
	//  min_coord1 = Minimum for coordinate range #1.
	//  max_coord1 = Maximum for coordinate range #1.
	//  min_coord2 = Minimum for coordinate range #2.
	//  max_coord2 = Maximum for coordinate range #2.
	// Returns an RCMP_XXXX value, for comparison of range #1 to range #2.

	public static int compare_coord_ranges (double min_coord1, double max_coord1, double min_coord2, double max_coord2) {

		// Check for disjoint coordinate ranges

		if (min_coord1 > max_coord2 || max_coord1 < min_coord2) {
			return RCMP_DISJOINT;
		}

		// Check for equal ranges

		if (min_coord1 == min_coord2 && max_coord1 == max_coord2) {
			return RCMP_EQUAL;
		}

		// Check for one range containing the other

		if (min_coord1 >= min_coord2 && max_coord1 <= max_coord2) {
			return RCMP_SUBSET;
		}
		if (min_coord1 <= min_coord2 && max_coord1 >= max_coord2) {
			return RCMP_CONTAINS;
		}

		// Ranges overlap
		
		return RCMP_OVERLAP;
	}


	// Compare coordinate ranges, with a small tolerance for equality, when it is known they are not disjoint.
	// Parameters:
	//  min_coord1 = Minimum for coordinate range #1.
	//  max_coord1 = Maximum for coordinate range #1.
	//  min_coord2 = Minimum for coordinate range #2.
	//  max_coord2 = Maximum for coordinate range #2.
	//  tol = Tolerance.
	// Returns an RCMP_XXXX value, for comparison of range #1 to range #2.

	protected static int compare_coord_ranges_tol_no_disjoint (double min_coord1, double max_coord1, double min_coord2, double max_coord2, final double tol) {

		// Comparison results, with tolerance for equality

		final double diffmin = min_coord1 - min_coord2;
		int cmpmin = 0;		// 1, 0, or -1 according as min_coord1 is >, ==, or < min_coord2
		if (diffmin > tol) {
			cmpmin = 1;
		} else if (diffmin < -tol) {
			cmpmin = -1;
		}

		final double diffmax = max_coord1 - max_coord2;
		int cmpmax = 0;		// 1, 0, or -1 according as max_coord1 is >, ==, or < max_coord2
		if (diffmax > tol) {
			cmpmax = 1;
		} else if (diffmax < -tol) {
			cmpmax = -1;
		}

		// Check for equal ranges

		if (cmpmin == 0 && cmpmax == 0) {
			return RCMP_EQUAL;
		}

		// Check for one range containing the other

		if (cmpmin >= 0 && cmpmax <= 0) {
			return RCMP_SUBSET;
		}
		if (cmpmin <= 0 && cmpmax >= 0) {
			return RCMP_CONTAINS;
		}

		// Ranges overlap
		
		return RCMP_OVERLAP;
	}


	// Compare coordinate ranges, with a small tolerance for equality.
	// Parameters:
	//  min_coord1 = Minimum for coordinate range #1.
	//  max_coord1 = Maximum for coordinate range #1.
	//  min_coord2 = Minimum for coordinate range #2.
	//  max_coord2 = Maximum for coordinate range #2.
	// Returns an RCMP_XXXX value, for comparison of range #1 to range #2.

	public static int compare_coord_ranges_tol (double min_coord1, double max_coord1, double min_coord2, double max_coord2) {

		// Tolerance

		final double tol = DEF_TOL_RCMP;

		// Check for disjoint coordinate ranges, must exceed tolerance to be considered disjoint

		if (min_coord1 - max_coord2 > tol || min_coord2 - max_coord1 > tol) {
			return RCMP_DISJOINT;
		}

		// Finish comparison

		return compare_coord_ranges_tol_no_disjoint (min_coord1, max_coord1, min_coord2, max_coord2, tol);
	}


	// Test if coordinate ranges are disjoint, with a small tolerance.
	// Parameters:
	//  min_coord1 = Minimum for coordinate range #1.
	//  max_coord1 = Maximum for coordinate range #1.
	//  min_coord2 = Minimum for coordinate range #2.
	//  max_coord2 = Maximum for coordinate range #2.
	// Returns true if ranges are disjoint.

	public static boolean is_disjoint_coord_ranges_tol (double min_coord1, double max_coord1, double min_coord2, double max_coord2) {

		// Tolerance

		final double tol = DEF_TOL_RCMP;

		// Check for disjoint coordinate ranges, must exceed tolerance to be considered disjoint

		return (min_coord1 - max_coord2 > tol || min_coord2 - max_coord1 > tol);
	}


	// Compare longitude ranges, with a small tolerance.
	// Parameters:
	//  min_lon1 = Minimum for longitude range #1.
	//  max_lon1 = Maximum for longitude range #1.
	//  min_lon2 = Minimum for longitude range #2.
	//  max_lon2 = Maximum for longitude range #2.
	// Returns an RCMP_XXXX value, for comparison of range #1 to range #2.
	// Each longitude must be between -360 and +360, and must have min < max.

	public static int compare_lon_ranges (double min_lon1, double max_lon1, double min_lon2, double max_lon2) {

		// Tolerance

		final double tol = DEF_TOL_RCMP;

		// Handle cases where one or both ranges spans 360 degrees, allowing tolerance on both ends

		final double full_circ = 360.0 - (2.0 * tol);

		if (max_lon1 - min_lon1 >= full_circ) {
			if (max_lon2 - min_lon2 >= full_circ) {
				return RCMP_EQUAL;
			}
			return RCMP_CONTAINS;
		} else {
			if (max_lon2 - min_lon2 >= full_circ) {
				return RCMP_SUBSET;
			}
		}

		// If range #2 ends after range #1 (or at the same time)

		if (max_lon1 <= max_lon2) {

			// If wrapped (by -360) range #2 is at or after the start of range #1 (allowing tolerance)

			final double max_wrap2 = max_lon2 - 360.0;

			if (min_lon1 - max_wrap2 <= tol) {		// equivalent to: max_lon2 >= min_lon1 + 360.0 - tol

				final double min_wrap2 = min_lon2 - 360.0;

				// Disjoint if range #1 lies in the space between the wrapped and double-wrapped range #2

				if (min_lon1 - (max_lon2 - 720.0) > tol && min_wrap2 - max_lon1 > tol) {
					return RCMP_DISJOINT;
				}

				// Compare range #1 to wrapped range #2

				return compare_coord_ranges_tol_no_disjoint (min_lon1, max_lon1, min_wrap2, max_wrap2, tol);
			}
		}

		// Otherwise, range #1 ends after range #2

		else {

			// If wrapped (by -360) range #1 is at or after the start of range #2 (allowing tolerance)

			final double max_wrap1 = max_lon1 - 360.0;

			if (min_lon2 - max_wrap1 <= tol) {		// equivalent to: max_lon1 >= min_lon2 + 360.0 - tol

				final double min_wrap1 = min_lon1 - 360.0;

				// Disjoint if range #2 lies in the space between the wrapped and double-wrapped range #1

				if (min_lon2 - (max_lon1 - 720.0) > tol && min_wrap1 - max_lon2 > tol) {
					return RCMP_DISJOINT;
				}

				// Compare range #1 to wrapped range #2

				return compare_coord_ranges_tol_no_disjoint (min_wrap1, max_wrap1, min_lon2, max_lon2, tol);
			}
		}

		// Disjoint if ranges (without wrapping) don't touch

		if (min_lon1 - max_lon2 > tol || min_lon2 - max_lon1 > tol) {
			return RCMP_DISJOINT;
		}

		// Compare range #1 to range #2

		return compare_coord_ranges_tol_no_disjoint (min_lon1, max_lon1, min_lon2, max_lon2, tol);
	}


	// Test if longitude ranges are disjoint, with a small tolerance.
	// Parameters:
	//  min_lon1 = Minimum for longitude range #1.
	//  max_lon1 = Maximum for longitude range #1.
	//  min_lon2 = Minimum for longitude range #2.
	//  max_lon2 = Maximum for longitude range #2.
	// Returns true if ranges are disjoint.
	// Each longitude must be between -360 and +360, and must have min < max.

	public static boolean is_disjoint_lon_ranges (double min_lon1, double max_lon1, double min_lon2, double max_lon2) {

		// Tolerance

		final double tol = DEF_TOL_RCMP;

		// Handle cases where one or both ranges spans 360 degrees

		final double full_circ = 360.0 - (2.0 * tol);

		if (max_lon1 - min_lon1 >= full_circ || max_lon2 - min_lon2 >= full_circ) {
			return false;
		}

		// If range #2 ends after range #1 (or at the same time)

		if (max_lon1 <= max_lon2) {

			// If wrapped (by -360) range #2 is at or after the start of range #1 (allowing tolerance)

			final double max_wrap2 = max_lon2 - 360.0;

			if (min_lon1 - max_wrap2 <= tol) {		// equivalent to: max_lon2 >= min_lon1 + 360.0 - tol

				final double min_wrap2 = min_lon2 - 360.0;

				// Disjoint if range #1 lies in the space between the wrapped and double-wrapped range #2

				return (min_lon1 - (max_lon2 - 720.0) > tol && min_wrap2 - max_lon1 > tol);
			}
		}

		// Otherwise, range #1 ends after range #2

		else {

			// If wrapped (by -360) range #1 is at or after the start of range #2 (allowing tolerance)

			final double max_wrap1 = max_lon1 - 360.0;

			if (min_lon2 - max_wrap1 <= tol) {		// equivalent to: max_lon1 >= min_lon2 + 360.0 - tol

				final double min_wrap1 = min_lon1 - 360.0;

				// Disjoint if range #2 lies in the space between the wrapped and double-wrapped range #1

				return (min_lon2 - (max_lon1 - 720.0) > tol && min_wrap1 - max_lon2 > tol);
			}
		}

		// Disjoint if ranges (without wrapping) don't touch

		return (min_lon1 - max_lon2 > tol || min_lon2 - max_lon1 > tol);
	}


	// Combine two range comparisons.
	// Parameters:
	//  rcmp1 = Comparison result #1.
	//  rcmp2 = Comparison result #2.
	// Returns and RCMP_XXXX values, for a 2D comparison.

	public static int combine_rcmp (int rcmp1, int rcmp2) {

		// If either is unknown, the combined result is unknown

		if (rcmp1 == RCMP_UNKNOWN || rcmp2 == RCMP_UNKNOWN) {
			return RCMP_UNKNOWN;
		}

		// If either is disjoint, the combined result is disjoint

		if (rcmp1 == RCMP_DISJOINT || rcmp2 == RCMP_DISJOINT) {
			return RCMP_DISJOINT;
		}

		// If either is equal, the result is the other

		if (rcmp1 == RCMP_EQUAL) {
			return rcmp2;
		}
		if (rcmp2 == RCMP_EQUAL) {
			return rcmp1;
		}

		// We know both results intersect, if they intersect the same way then return the common value

		if (rcmp1 == rcmp2) {
			return rcmp1;
		}

		// Otherwise, they overlap

		return RCMP_OVERLAP;
	}




	//----- Ranging -----




	// LatitudeAccum - Accumulator for latitude ranges.

	public static class LatitudeAccum {

		// Accumulated minimum and maximum latitude.

		public double accum_min_lat;
		public double accum_max_lat;

		// Constructor makes an "empty" range.

		public LatitudeAccum () {
			accum_min_lat = 180.0;
			accum_max_lat = -180.0;
		}

		// Return true if the accumulator is empty (that is, no ranges have been accumulated).

		public final boolean isEmpty () {
			return (accum_max_lat < accum_min_lat);
		}

		// Accumulate a latitude range.
		// Parameters must satisfy:  -90 <= lat_lo <= lat_hi < +90

		public final void accum_lat_range (double lat_lo, double lat_hi) {
			accum_min_lat = Math.min (accum_min_lat, lat_lo);
			accum_max_lat = Math.max (accum_max_lat, lat_hi);
			return;
		}

		// Accumulate the latitude range from the region.

		public final void accum_lat_range (SphRegion region) {
			accum_lat_range (region.min_lat, region.max_lat);
			return;
		}

		// Set the latitude range in the region.

		public final void set_lat_range (SphRegion region) {
			if (isEmpty()) {
				throw new IllegalStateException ("SphRegion.LatitudeAccum.set_lat_range: Attempt to set empty latitude range");
			}
			region.min_lat = accum_min_lat;
			region.max_lat = accum_max_lat;
			return;
		}
	}




	// LongitudeAccum - Accumulator for longitude ranges.

	public static class LongitudeAccum {

		// Accumulated minimum and maximum longitude in the unwrapped domain -180 to +180..

		public double unwrap_min_lon;
		public double unwrap_max_lon;

		// Accumulated minimum and maximum longitude in the wrapped domain 0 to +360..

		public double wrap_min_lon;
		public double wrap_max_lon;

		// Constructor makes an "empty" range.

		public LongitudeAccum () {
			unwrap_min_lon = 720.0;
			unwrap_max_lon = -720.0;
			wrap_min_lon = 720.0;
			wrap_max_lon = -720.0;
		}

		// Return true if the accumulator is empty (that is, no ranges have been accumulated).

		public final boolean isEmpty () {
			return (unwrap_max_lon < unwrap_min_lon);
		}

		// Return true if the wrapped range is preferred.
		// Note: Bias slightly in favor of the unwrapped range.

		public final boolean is_wrap_preferred () {
			return (0.000001 + wrap_max_lon - wrap_min_lon < unwrap_max_lon - unwrap_min_lon);
		}

		// Internal function to accumulate in the unwrapped domain.

		private void unwrap_accum (double lon_lo, double lon_hi)  {
			unwrap_min_lon = Math.min (unwrap_min_lon, lon_lo);
			unwrap_max_lon = Math.max (unwrap_max_lon, lon_hi);
		}

		// Internal function to accumulate in the wrapped domain.

		private void wrap_accum (double lon_lo, double lon_hi)  {
			wrap_min_lon = Math.min (wrap_min_lon, lon_lo);
			wrap_max_lon = Math.max (wrap_max_lon, lon_hi);
		}

		// Internal function to set the unwrapped domain to the world.

		private void unwrap_world ()  {
			unwrap_min_lon = -180.0;
			unwrap_max_lon = 180.0;
		}

		// Internal function to set the wrapped domain to the world.

		private void wrap_world ()  {
			wrap_min_lon = 0.0;
			wrap_max_lon = 360.0;
		}

		// Accumulate a longitude range.
		// Parameters must satisfy:  -360 <= lon_lo <= lon_hi < +360

		public final void accum_lon_range (double lon_lo, double lon_hi) {

			// Upper end lies in the +180 to +360 range ...

			if (lon_hi > 180.0) {

				// Crosses date line and prime meridian

				if (lon_lo < 0.0) {
					unwrap_world();
					wrap_world();
				}

				// Crosses date line (entirely in the 0 to +360 range)

				else if (lon_lo < 180.0) {
					unwrap_world();
					wrap_accum (lon_lo, lon_hi);
				}

				// Entirely in the +180 to +360 range

				else {
					unwrap_accum (lon_lo - 360.0, lon_hi - 360.0);
					wrap_accum (lon_lo, lon_hi);
				}
			}

			// Lower end lies in the -360 to -180 range ...

			else if (lon_lo < -180.0) {

				// Crosses date line and prime meridian

				if (lon_hi > 0.0) {
					unwrap_world();
					wrap_world();
				}

				// Crosses date line (entirely in the -360 to 0 range)

				else if (lon_hi > -180.0) {
					unwrap_world();
					wrap_accum (lon_lo + 360.0, lon_hi + 360.0);
				}

				// Entirely in the -360 to -180 range

				else {
					unwrap_accum (lon_lo + 360.0, lon_hi + 360.0);
					wrap_accum (lon_lo + 360.0, lon_hi + 360.0);
				}
			}

			// Lower end lies in the -180 to 0 range ...

			else if (lon_lo < 0.0) {

				// Crosses prime meridian (entirely in the -180 to +180 range)

				if (lon_hi > 0.0) {
					unwrap_accum (lon_lo, lon_hi);
					wrap_world();
				}

				// Entirely in the -180 to 0 range

				else {
					unwrap_accum (lon_lo, lon_hi);
					wrap_accum (lon_lo + 360.0, lon_hi + 360.0);
				}
			}

			// Entirely in the 0 to +180 range

			else {
				unwrap_accum (lon_lo, lon_hi);
				wrap_accum (lon_lo, lon_hi);
			}

			return;
		}

		// Accumulate the longitude range from the region.

		public final void accum_lon_range (SphRegion region) {
			accum_lon_range (region.min_lon, region.max_lon);
			return;
		}

		// Set the longitude range in the region.

		public final void set_lon_range (SphRegion region) {
			if (isEmpty()) {
				throw new IllegalStateException ("SphRegion.LongitudeAccum.set_lon_range: Attempt to set empty longitude range");
			}
			if (is_wrap_preferred()) {
				region.plot_wrap = true;
				region.min_lon = wrap_min_lon;
				region.max_lon = wrap_max_lon;
			} else {
				region.plot_wrap = false;
				region.min_lon = unwrap_min_lon;
				region.max_lon = unwrap_max_lon;
			}
			return;
		}
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


	// Make a circular or rectangular region from user display parameters.
	// Parameters:
	//  userParamMap = Map of parameters, which this function adds to.
	// Returns null if the region could not be created.
	// Each value in the map should be Number (or subclass thereof), String, or Boolean.
	// Note: This function throws no exceptions.

	public static SphRegion make_circle_or_rect_from_display_params (Map<String, Object> userParamMap) {

		SphRegionCircle circle = SphRegionCircle.make_from_display_params (userParamMap);
		if (circle != null) {
			return circle;
		}

		SphRegionMercRectangle rectangle = SphRegionMercRectangle.make_from_display_params (userParamMap);
		if (rectangle != null) {
			return rectangle;
		}

		return null;
	}


	/**
	 * Construct a union from given lists of included and excluded regions.
	 * @param include_list = List of regions to include.
	 * @param exclude_list = List of regions to exclude.
	 */
	public static SphRegion makeUnion (List<SphRegion> include_list, List<SphRegion> exclude_list) {
		return new SphRegionUnion (include_list, exclude_list);
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
	protected static final int MARSHAL_UNION = 151001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Strings corresponding to each type code

	protected static final String MARSHAL_NULL_S = "null";
	protected static final String MARSHAL_CIRCLE_S = "circle";
	protected static final String MARSHAL_MERC_POLYGON_S = "polygon";
	protected static final String MARSHAL_MERC_RECTANGLE_S = "rectangle";
	protected static final String MARSHAL_GC_POLYGON_S = "gc_polygon";
	protected static final String MARSHAL_WORLD_S = "world";
	protected static final String MARSHAL_BOUNDING_BOX_S = "bounding_box";
	protected static final String MARSHAL_UNION_S = "union";

	// Get the string for the given type code.

	protected static String get_string_for_type (int type) {
		switch (type) {
		case MARSHAL_NULL: return MARSHAL_NULL_S;
		case MARSHAL_CIRCLE: return MARSHAL_CIRCLE_S;
		case MARSHAL_MERC_POLYGON: return MARSHAL_MERC_POLYGON_S;
		case MARSHAL_MERC_RECTANGLE: return MARSHAL_MERC_RECTANGLE_S;
		case MARSHAL_GC_POLYGON: return MARSHAL_GC_POLYGON_S;
		case MARSHAL_WORLD: return MARSHAL_WORLD_S;
		case MARSHAL_BOUNDING_BOX: return MARSHAL_BOUNDING_BOX_S;
		case MARSHAL_UNION: return MARSHAL_UNION_S;
		}
		throw new IllegalArgumentException ("SphRegion.get_string_for_type: Unknown type code: type = " + type);
	}

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

		case MARSHAL_UNION:
			result = new SphRegionUnion();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal array of objects.

	public static void marshal_array (MarshalWriter writer, String name, SphRegion[] x) {
	//	int n = x.length;
	//	writer.marshalArrayBegin (name, n);
	//	for (int i = 0; i < n; ++i) {
	//		marshal_poly (writer, null, x[i]);
	//	}
	//	writer.marshalArrayEnd ();

		writer.marshalObjectArray (name, x, SphRegion::marshal_poly);
		return;
	}

	// Unmarshal array of objects.

	public static SphRegion[] unmarshal_array (MarshalReader reader, String name) {
	//	int n = reader.unmarshalArrayBegin (name);
	//	SphRegion[] x = new SphRegion[n];
	//	for (int i = 0; i < n; ++i) {
	//		x[i] = unmarshal_poly (reader, null);
	//	}
	//	reader.unmarshalArrayEnd ();
	//	return x;

		return reader.unmarshalObjectArray (name, new SphRegion[0], SphRegion::unmarshal_poly);
	}

	// Marshal object for a single unadorned line of text, internal.

	protected abstract void do_marshal_to_line (MarshalWriter writer);

	// Unmarshal object for a single unadorned line of text, internal.

	protected abstract void do_umarshal_from_line (MarshalReader reader);

	// Marshal object to a single unadorned line of text, polymorphic.

	public static void marshal_to_line_poly (MarshalWriter writer, String name, SphRegion obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalString ("type", MARSHAL_NULL_S);
		} else {
			int type = obj.get_marshal_type();
			switch (type) {

			default:
				throw new MarshalException ("SphRegion.marshal_to_line_poly: Unknown class type code: type = " + type);

			case MARSHAL_CIRCLE:
				writer.marshalString ("type", MARSHAL_CIRCLE_S);
				break;

			case MARSHAL_MERC_POLYGON:
				writer.marshalString ("type", MARSHAL_MERC_POLYGON_S);
				break;

			case MARSHAL_MERC_RECTANGLE:
				writer.marshalString ("type", MARSHAL_MERC_RECTANGLE_S);
				break;

			case MARSHAL_WORLD:
				writer.marshalString ("type", MARSHAL_WORLD_S);
				break;

			case MARSHAL_UNION:
				writer.marshalString ("type", MARSHAL_UNION_S);
				break;
			}

			obj.do_marshal_to_line (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object from a single unadorned line of text, polymorphic.

	public static SphRegion unmarshal_from_line_poly (MarshalReader reader, String name) {
		SphRegion result;

		reader.unmarshalMapBegin (name);

		String type_s = reader.unmarshalString ("type");

		switch (type_s) {

		default:
			throw new MarshalException ("SphRegion.unmarshal_from_line_poly: Unknown region type: " + type_s);

		case MARSHAL_NULL_S:
			result = null;
			break;

		case MARSHAL_CIRCLE_S:
			result = new SphRegionCircle();
			result.do_umarshal_from_line (reader);
			break;

		case MARSHAL_MERC_POLYGON_S:
			result = new SphRegionMercPolygon();
			result.do_umarshal_from_line (reader);
			break;

		case MARSHAL_MERC_RECTANGLE_S:
			result = new SphRegionMercRectangle();
			result.do_umarshal_from_line (reader);
			break;

		case MARSHAL_WORLD_S:
			result = new SphRegionWorld();
			result.do_umarshal_from_line (reader);
			break;

		case MARSHAL_UNION_S:
			result = new SphRegionUnion();
			result.do_umarshal_from_line (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

	// Marshal object to a single unadorned line of text, polymorphic.

	public static String marshal_to_line_poly (SphRegion obj) {

	//	StringBuilder sb = new StringBuilder();
	//	boolean f_unicode = false;
	//	boolean f_quote_all = false;
	//	boolean f_store_names = false;
	//	MarshalWriter writer = MarshalUtils.writer_for_line (sb, f_unicode, f_quote_all, f_store_names);
	//
	//	marshal_to_line_poly (writer, null, obj);
	//
	//	writer.write_completion_check();
	//
	//	return sb.toString();

		boolean f_unicode = false;
		boolean f_quote_all = false;
		boolean f_store_names = false;
		return MarshalUtils.lambda_to_line (SphRegion::marshal_to_line_poly, obj, f_unicode, f_quote_all, f_store_names);
	}

	// Unmarshal object from a single unadorned line of text, polymorphic.

	public static SphRegion unmarshal_from_line_poly (String line) {

	//	boolean f_store_names = false;
	//	MarshalReader reader = MarshalUtils.reader_for_line (line, f_store_names);
	//
	//	SphRegion result = unmarshal_from_line_poly (reader, null);
	//
	//	boolean f_require_eof = true;
	//	reader.read_completion_check (f_require_eof);
	//
	//	return result;

		boolean f_store_names = false;
		return MarshalUtils.lambda_from_line (SphRegion::unmarshal_from_line_poly, line, f_store_names);
	}

	// Marshal array of objects to a single unadorned line of text, polymorphic..

	public static void marshal_array_to_line_poly (MarshalWriter writer, String name, SphRegion[] x) {
	//	int n = x.length;
	//	writer.marshalArrayBegin (name, n);
	//	for (int i = 0; i < n; ++i) {
	//		marshal_to_line_poly (writer, null, x[i]);
	//	}
	//	writer.marshalArrayEnd ();

		writer.marshalObjectArray (name, x, SphRegion::marshal_to_line_poly);
		return;
	}

	// Unmarshal array of objects from a single unadorned line of text, polymorphic.

	public static SphRegion[] unmarshal_array_from_line_poly (MarshalReader reader, String name) {
	//	int n = reader.unmarshalArrayBegin (name);
	//	SphRegion[] x = new SphRegion[n];
	//	for (int i = 0; i < n; ++i) {
	//		x[i] = unmarshal_from_line_poly (reader, null);
	//	}
	//	reader.unmarshalArrayEnd ();
	//	return x;

		return reader.unmarshalObjectArray (name, new SphRegion[0], SphRegion::unmarshal_from_line_poly);
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

		// Marshal recovered region to string

		System.out.println ();
		System.out.println ("***** Writing recovered region to JSON string *****");
		System.out.println ();

		MarshalImpJsonWriter writer2 = new MarshalImpJsonWriter();
		marshal_poly (writer2, null, region2);
		writer2.check_write_complete ();
		String json_string2 = writer2.get_json_string();

		System.out.println (MarshalUtils.display_valid_json_string (json_string2));

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

		// Marshal recovered region to string

		System.out.println ();
		System.out.println ("***** Writing recovered region to JSON string *****");
		System.out.println ();

		MarshalImpJsonWriter writer3 = new MarshalImpJsonWriter();
		marshal_poly (writer3, null, region3);
		writer3.check_write_complete ();
		String json_string3 = writer3.get_json_string();

		System.out.println (MarshalUtils.display_valid_json_string (json_string3));

		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "SphRegion");




		// Subcommand : Test #1
		// Command format:
		//  test1  lat  lon  radius_km
		// Test marshal/unmarshal for a circle.
		// Here is an example command line:
		//  test1  37.0 -120.0 200.0

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
		// Here is an example command line:
		//  test2  39.0 43.0 -115.0 -110.0

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
		// Here is an example command line:
		//  test3  39.0 43.0 -115.0 -110.0

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
		// Here is an example command line:
		//  test4  38.0 -119.0 40.0 -119.0 39.0 -112.0

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




		// Subcommand : Test #7
		// Command format:
		//  test7  c_lat  c_lon  c_radius_km  r_lat_1  r_lat_2  r_lon_1  r_lon_2  p_lat_1  p_lon_1 ...
		// Test marshal/unmarshal for a union.
		// This creates a union containing a circle and a rectangle as include regions,
		// and a polygon as an exclude region.
		// Here is an example command line:
		//  test7  37.0 -120.0 200.0  39.0 43.0 -115.0 -110.0  38.0 -119.0 40.0 -119.0 39.0 -112.0

		if (testargs.is_test ("test7")) {

			// Read arguments

			System.out.println ("Test marshal/unmarshal for a union");
			double c_lat = testargs.get_double ("c_lat");
			double c_lon = testargs.get_double ("c_lon");
			double c_radius_km = testargs.get_double ("c_radius_km");
			double r_lat_1 = testargs.get_double ("r_lat_1");
			double r_lat_2 = testargs.get_double ("r_lat_2");
			double r_lon_1 = testargs.get_double ("r_lon_1");
			double r_lon_2 = testargs.get_double ("r_lon_2");
			double[] coords = testargs.get_double_tuple_array ("coords", -1, 6, 2, "p_lat", "p_lon");
			testargs.end_test();

			// Make the polygon

			System.out.println ();
			System.out.println ("***** Making union *****");

			SphRegion c_region = SphRegion.makeCircle (new SphLatLon (c_lat, c_lon), c_radius_km);

			SphRegion r_region = SphRegion.makeMercRectangle (r_lat_1, r_lat_2, r_lon_1, r_lon_2);

			List<SphLatLon> vertex_list = new ArrayList<SphLatLon>();
			for (int i = 0; i < coords.length; i += 2) {
				vertex_list.add (new SphLatLon (coords[i], coords[i+1]));
			}
			SphRegion p_region = SphRegion.makeMercPolygon (vertex_list);

			List<SphRegion> include_list = new ArrayList<SphRegion>();
			include_list.add (c_region);
			include_list.add (r_region);

			List<SphRegion> exclude_list = new ArrayList<SphRegion>();
			exclude_list.add (p_region);

			SphRegion region = SphRegion.makeUnion (include_list, exclude_list);

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
