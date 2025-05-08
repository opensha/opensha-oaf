package org.opensha.oaf.util.catalog;

import java.util.function.Predicate;

import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;


// Filter to select ruptures by numerous criteria.
// Author: Michael Barall.

public class ObsEqkRupFilter implements Predicate<ObsEqkRupture> {


	//----- Filters -----


	// Filter for minimum time, inclusive.

	private boolean f_min_time = false;
	private long min_time = Long.MIN_VALUE;

	// Filter for maximum time, inclusive.

	private boolean f_max_time = false;
	private long max_time = Long.MAX_VALUE;

	// Filter for minimum magnitude, inclusive.

	private boolean f_min_mag = false;
	private double min_mag = -Double.MAX_VALUE;

	// Filter for maximum magnitude, inclusive.

	private boolean f_max_mag = false;
	private double max_mag = Double.MAX_VALUE;

	// Filter for minimum depth, inclusive.

	private boolean f_min_depth = false;
	private double min_depth = -Double.MAX_VALUE;

	// Filter for maximum depth, inclusive.

	private boolean f_max_depth = false;
	private double max_depth = Double.MAX_VALUE;

	// Filter for region to include.

	private SphRegion include_region = null;

	// Filter for region to exclude.

	private SphRegion exclude_region = null;


	//----- Test function -----


	@Override
	public boolean test (ObsEqkRupture rup) {
		Location hypo = rup.getHypocenterLocation();
		if (
			   (f_min_time && rup.getOriginTime() < min_time)
			|| (f_max_time && rup.getOriginTime() > max_time)
			|| (f_min_mag && rup.getMag() < min_mag)
			|| (f_max_mag && rup.getMag() > max_mag)
			|| (f_min_depth && hypo.getDepth() < min_depth)
			|| (f_max_depth && hypo.getDepth() > max_depth)
			|| (include_region != null && !(include_region.contains (hypo)))
			|| (exclude_region != null && exclude_region.contains (hypo))
		) {
			return false;
		}
		return true;
	}


	//----- Construction -----


	// Construct creates a filter that accepts all ruptures.

	public ObsEqkRupFilter () {}


	// Set a filter on a time range, with time given in milliseconds since the epoch.

	public final ObsEqkRupFilter set_time_range_filter (long min_time, long max_time) {
		this.f_min_time = true;
		this.min_time = min_time;
		this.f_max_time = true;
		this.max_time = max_time;
		return this;
	}


	// Set a filter on a time range, with time given in days relative to the mainshock time.

	public final ObsEqkRupFilter set_time_range_filter (long mainshock_time, double min_days, double max_days) {
		return set_time_range_filter (
			mainshock_time + SimpleUtils.days_to_millis (min_days),
			mainshock_time + SimpleUtils.days_to_millis (max_days)
		);
	}


	// Set a filter on a time range, with time given in days relative to the mainshock time.

	public final ObsEqkRupFilter set_time_range_filter (ObsEqkRupture mainshock, double min_days, double max_days) {
		return set_time_range_filter (mainshock.getOriginTime(), min_days, max_days);
	}


	// Set a filter on a minimum magnitude.

	public final ObsEqkRupFilter set_min_mag_filter (double min_mag) {
		this.f_min_mag = true;
		this.min_mag = min_mag;
		return this;
	}


	// Set a filter on a maximum magnitude.

	public final ObsEqkRupFilter set_max_mag_filter (double max_mag) {
		this.f_max_mag = true;
		this.max_mag = max_mag;
		return this;
	}


	// Set a filter on a depth range.

	public final ObsEqkRupFilter set_depth_range_filter (double min_depth, double max_depth) {
		this.f_min_depth = true;
		this.min_depth = min_depth;
		this.f_max_depth = true;
		this.max_depth = max_depth;
		return this;
	}


	// Set a filter on an include region.

	public final ObsEqkRupFilter set_include_region_filter (SphRegion include_region) {
		this.include_region = include_region;
		return this;
	}


	// Set a filter on an exclude region.

	public final ObsEqkRupFilter set_exclude_region_filter (SphRegion exclude_region) {
		this.exclude_region = exclude_region;
		return this;
	}

}
