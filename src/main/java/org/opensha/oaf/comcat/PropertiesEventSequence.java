package org.opensha.oaf.comcat;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.io.File;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;

import org.opensha.oaf.comcat.ComcatOAFAccessor;

import org.opensha.oaf.pdl.PDLProductBuilderEventSequence;
import org.opensha.oaf.pdl.PDLSender;
import org.opensha.oaf.pdl.PDLProductBuilderEventSequenceText;
import org.opensha.oaf.pdl.PDLProductFile;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;
import org.opensha.oaf.aafs.VersionInfo;

import org.opensha.commons.geo.GeoTools;
import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;

import org.json.simple.JSONObject;

import gov.usgs.earthquake.product.Product;


// Properties for an event sequence product.
// Author: Michael Barall 01/07/2022.
//
// Holds the properties for an event-sequence product.
//
// All properties are stored in Comcat as strings.
// So, each numeric value is stored here in both numeric and string form.
// This makes to possible to make an exact copy of a product stored in Comcat.
//
// An omitted property has its variable set to null.
// Non-null properties may not be the empty string.
// If an empty string is supplied as a property value, it is converted to null.
// When marshalled, null strings are stored as empty strings.
//
// This can be used for a delete product, in which case only the network identifier
// and network code are filled in.
//
// This should be regarded as preliminary.

public class PropertiesEventSequence {

	//----- Property names and known values -----

	// Properties for the event-sequence product.
	// (In addition to the standard properties "eventsource" and "eventsourcecode".)
	//
	// Note: The three property names "eventtime", "starttime", and "endtime"
	// have special significance to PDL.  If their values are not a time in
	// ISO-8601 format, then the products do not appear in Comcat.  So, if time
	// is represented as number of mulliseconds (EVS_TIME_ISO_8601 = false),
	// then these names should be changed to "event-time", "start-time", and
	// "end-time" respectively.

	public static final String EVS_NAME_EVENT_TIME = "eventtime";
	public static final String EVS_NAME_REGION_TYPE = "region-type";
	public static final String EVS_NAME_CIRCLE_LONGITUDE = "circle-longitude";
	public static final String EVS_NAME_CIRCLE_LATITUDE = "circle-latitude";
	public static final String EVS_NAME_CIRCLE_RADIUS_KM = "circle-radiuskm";
	public static final String EVS_NAME_MAX_LATITUDE = "maximum-latitude";
	public static final String EVS_NAME_MIN_LATITUDE = "minimum-latitude";
	public static final String EVS_NAME_MAX_LONGITUDE = "maximum-longitude";
	public static final String EVS_NAME_MIN_LONGITUDE = "minimum-longitude";
	public static final String EVS_NAME_START_TIME = "starttime";
	public static final String EVS_NAME_END_TIME = "endtime";
	public static final String EVS_NAME_TITLE = "title";

	// Known property values for the event-sequence product.

	public static final String EVS_REGION_TYPE_CIRCLE = "circle";
	public static final String EVS_REGION_TYPE_RECTANGLE = "rectangle";

	public static final double EVS_CIRCLE_MAX_RADIUS_KM = 20001.6;

	public static final String EVS_TITLE_SUFFIX_SEQUENCE = "Sequence";
	public static final String EVS_TITLE_UNKNOWN = "Unknown";

	// Filename for the event-sequence-text product.

	public static final String EVSTEXT_FILENAME_SUMMARY = "summary.html";

	// Flags that specify which parts of the product are optional.

	public static final boolean EVS_OPTIONAL_EVENT = false;			// eventsource and eventsourcecode
	public static final boolean EVS_OPTIONAL_EVENT_TIME = true;		// eventtime
	public static final boolean EVS_OPTIONAL_BOUNDS = false;		// the four rectangle bounds (if region is not rectangle)

	// Flag for time format.

	public static final boolean EVS_TIME_ISO_8601 = true;			// true for ISO-8601 format, false for number of milliseconds

	// Extra properties for the event-sequence product.

	public static final String EVS_EXTRA_GENERATED_BY = "generated-by";




	//----- Properties and flags -----


	//--- Associated mainshock event
	
	// Network identifier for the event (for example, "us"), or null if none ("properties/eventsource" in the product).
	// Must be non-null and non-empty if EVS_OPTIONAL_EVENT is false.
	// Must be null if not available.
	// Comes from "properties/net" in the geojson.
	
	public String eventNetwork;
	
	// Network code for the event (for example, "10006jv5"), or null if none ("properties/eventsourcecode" in the product).
	// Must be non-null and non-empty if EVS_OPTIONAL_EVENT is false.
	// Must be null if not available.
	// Comes from "properties/code" in the geojson.
	
	public String eventCode;

	// Return true if we have event network and code.

	public final boolean has_network_and_code () {
		return (eventNetwork != null) && (eventCode != null);
	}


	//--- Reference time

	// The event origin time, or 0L/null if none ("properties/eventtime" in the product).
	// For a mainshock comes from "properties/time" in the geojson.
	// Must be available if EVS_OPTIONAL_EVENT_TIME is false.
	// The function of this property is to distinguish foreshocks from aftershocks.

	public long event_time;
	public String s_event_time;

	// Return true if we have event origin time.

	public final boolean has_event_time () {
		return s_event_time != null;
	}


	//--- Region selection.

	// The region type, should be "circle" or "rectangle" ("properties/region-type" in the product).
	// For a normal product, must be non-null and have one of the values "circle" or "rectangle".
	// For a delete product, this is null.

	public String region_type;

	public final boolean is_region_circle () {
		return region_type.equals(EVS_REGION_TYPE_CIRCLE);
	}

	public final boolean is_region_rectangle () {
		return region_type.equals(EVS_REGION_TYPE_RECTANGLE);
	}


	//--- Circle region.

	// The circle longitude, or 0.0/null if not a circle ("properties/circle-longitude" in the product).
	// Must be between -180 and 180.
	// Must be available if region type is circle.

	public double circle_longitude;
	public String s_circle_longitude;

	// The circle latitude, or 0.0/null if not a circle ("properties/circle-latitude" in the product).
	// Must be between -90 and 90.
	// Must be available if region type is circle.

	public double circle_latitude;
	public String s_circle_latitude;

	// The circle radius in km, or 0.0/null if not a circle ("properties/circle-radiuskm" in the product).
	// Must be > 0 and <= 20001.6.
	// Must be available if region type is circle.

	public double circle_radius_km;
	public String s_circle_radius_km;


	//--- Rectangle region.

	// The maximum latitude, or 0.0/null if no bounds included ("properties/maximum-latitude" in the product).
	// Must be between -90 and 90, and > minimum_latitude.
	// Must be available if region type is rectangle or EVS_OPTIONAL_BOUNDS is false.

	public double maximum_latitude;
	public String s_maximum_latitude;

	// The minimum latitude, or 0.0/null if no bounds included ("properties/minimum-latitude" in the product).
	// Must be between -90 and 90, and < maximum_latitude.
	// Must be available if region type is rectangle or EVS_OPTIONAL_BOUNDS is false.

	public double minimum_latitude;
	public String s_minimum_latitude;

	// The maximum longitude, or 0.0/null if no bounds included ("properties/maximum-longitude" in the product).
	// Must be between -360 and 360, and > minimum_longitude.
	// Generally should be between -180 and 180, but can be > 180 if the rectangle straddles the date line.
	// Must be available if region type is rectangle or EVS_OPTIONAL_BOUNDS is false.

	public double maximum_longitude;
	public String s_maximum_longitude;

	// The minimum longitude, or 0.0/null if no bounds included ("properties/minimum-longitude" in the product).
	// Must be between -360 and 360, and > maximum_longitude.
	// Generally should be between -180 and 180, but can be < -180 if the rectangle straddles the date line.
	// Must be available if region type is rectangle or EVS_OPTIONAL_BOUNDS is false.

	public double minimum_longitude;
	public String s_minimum_longitude;

	// Return true if bounds are available.

	public final boolean has_bounds () {
		return (s_maximum_latitude != null) && (s_minimum_latitude != null) && (s_maximum_longitude != null) && (s_minimum_longitude != null);
	}


	//--- Sequence duration.

	// The sequence start time ("properties/starttime" in the product).
	// This value is required.

	public long start_time;
	public String s_start_time;

	// The sequence end time, or 0L/null if none ("properties/endtime" in the product).
	// This value is optional, it may be omitted if a sequence is ongoing.

	public long end_time;
	public String s_end_time;

	// Return true if we have the sequence end time.

	public final boolean has_end_time () {
		return (s_end_time != null);
	}


	//--- Sequence identification

	// The title of the sequence ("properties/title" in the product).
	// Must be non-null and non-empty.
	// For a sequence associated with a minshock, this should have the form "<title> Sequence"
	// where <title> denotes the mainshock title ("properties/title" in the geojson).
	// Note that some titles end with the word "Sequence" and so an additional "Sequence"
	// should not be appended in those cases.

	public String title;




	//----- Property setters -----




	// Subroutine to convert time from long to string.
	// The converted form should have millisecond precision.
	
	private static SimpleDateFormat t2sDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private static final TimeZone utc = TimeZone.getTimeZone("UTC");
	static {
		t2sDateFormat.setTimeZone(utc);
	}

	private String evs_time_to_string (long x) {
		if (EVS_TIME_ISO_8601) {
			return t2sDateFormat.format (new Date(x));
		}
		return Long.toString(x);
	}




	// Subroutine to convert time from string to long.
	// Throws exception if conversion cannot be done.

	private long evs_string_to_time (String s) {
		if (EVS_TIME_ISO_8601) {
			return Instant.parse(s).toEpochMilli();
		}
		return Long.parseLong(s);
	}




	// Set or clear the network identifier for the event, false return indicates bad value.

	public final boolean set_eventNetwork (String s) {
		if (s == null || s.isEmpty()) {
			clear_eventNetwork();
			return EVS_OPTIONAL_EVENT;
		}
		eventNetwork = s;
		return true;
	}

	public final void clear_eventNetwork () {
		eventNetwork = null;
		return;
	}




	// Set or clear the network code for the event, false return indicates bad value.

	public final boolean set_eventCode (String s) {
		if (s == null || s.isEmpty()) {
			clear_eventCode();
			return EVS_OPTIONAL_EVENT;
		}
		eventCode = s;
		return true;
	}

	public final void clear_eventCode () {
		eventCode = null;
		return;
	}




	// Set or clear the event origin time, false return indicates bad value.

	public final boolean set_event_time (long x) {
		event_time = x;
		s_event_time = evs_time_to_string(x);
		return true;
	}

	public final boolean set_event_time (String s) {
		if (s == null || s.isEmpty()) {
			clear_event_time();
			return EVS_OPTIONAL_EVENT_TIME;
		}
		try {
			event_time = evs_string_to_time(s);
		} catch (Exception e) {
			clear_event_time();
			return false;
		}
		s_event_time = s;
		return true;
	}

	public final void clear_event_time () {
		event_time = 0L;
		s_event_time = null;
		return;
	}




	// Set or clear the region, false return indicates bad value.

	public final boolean set_region_type (String s) {
		if (s == null || s.isEmpty()) {
			clear_region_type();
			return false;
		}
		if (!( s.equals(EVS_REGION_TYPE_CIRCLE) || s.equals(EVS_REGION_TYPE_RECTANGLE) )) {
			clear_region_type();
			return false;
		}
		region_type = s;
		return true;
	}

	public final boolean set_region_circle () {
		region_type = EVS_REGION_TYPE_CIRCLE;
		return true;
	}

	public final boolean set_region_rectangle () {
		region_type = EVS_REGION_TYPE_RECTANGLE;
		return true;
	}

	public final boolean set_region_type_allow_null (String s) {
		if (s == null || s.isEmpty()) {
			clear_region_type();
			return true;
		}
		if (!( s.equals(EVS_REGION_TYPE_CIRCLE) || s.equals(EVS_REGION_TYPE_RECTANGLE) )) {
			clear_region_type();
			return false;
		}
		region_type = s;
		return true;
	}

	public final void clear_region_type () {
		region_type = null;
		return;
	}




	// Set or clear the circle longitude, false return indicates bad value.

	public final boolean set_circle_longitude (double x) {
		return set_circle_longitude (
			SimpleUtils.double_to_string_trailz ("%.7f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_circle_longitude (String s) {
		if (s == null || s.isEmpty()) {
			clear_circle_longitude();
			return true;
		}
		try {
			circle_longitude = Double.parseDouble(s);
		} catch (Exception e) {
			clear_circle_longitude();
			return false;
		}
		if (circle_longitude < -180.0 || circle_longitude > 180.0) {
			clear_circle_longitude();
			return false;
		}
		s_circle_longitude = s;
		return true;
	}

	public final void clear_circle_longitude () {
		circle_longitude = 0.0;
		s_circle_longitude = null;
		return;
	}




	// Set or clear the circle latitude, false return indicates bad value.

	public final boolean set_circle_latitude (double x) {
		return set_circle_latitude (
			SimpleUtils.double_to_string_trailz ("%.7f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_circle_latitude (String s) {
		if (s == null || s.isEmpty()) {
			clear_circle_latitude();
			return true;
		}
		try {
			circle_latitude = Double.parseDouble(s);
		} catch (Exception e) {
			clear_circle_latitude();
			return false;
		}
		if (circle_latitude < -90.0 || circle_latitude > 90.0) {
			clear_circle_latitude();
			return false;
		}
		s_circle_latitude = s;
		return true;
	}

	public final void clear_circle_latitude () {
		circle_latitude = 0.0;
		s_circle_latitude = null;
		return;
	}




	// Set or clear the circle radius in km, false return indicates bad value.

	public final boolean set_circle_radius_km (double x) {
		return set_circle_radius_km (
			SimpleUtils.double_to_string_trailz ("%.5f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_circle_radius_km (String s) {
		if (s == null || s.isEmpty()) {
			clear_circle_radius_km();
			return true;
		}
		try {
			circle_radius_km = Double.parseDouble(s);
		} catch (Exception e) {
			clear_circle_radius_km();
			return false;
		}
		if (circle_radius_km < 0.0 || circle_radius_km > EVS_CIRCLE_MAX_RADIUS_KM) {
			clear_circle_radius_km();
			return false;
		}
		s_circle_radius_km = s;
		return true;
	}

	public final void clear_circle_radius_km () {
		circle_radius_km = 0.0;
		s_circle_radius_km = null;
		return;
	}




	// Set or clear the maximum latitude, false return indicates bad value.

	public final boolean set_maximum_latitude (double x) {
		return set_maximum_latitude (
			SimpleUtils.double_to_string_trailz ("%.7f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_maximum_latitude (String s) {
		if (s == null || s.isEmpty()) {
			clear_maximum_latitude();
			return EVS_OPTIONAL_BOUNDS;
		}
		try {
			maximum_latitude = Double.parseDouble(s);
		} catch (Exception e) {
			clear_maximum_latitude();
			return false;
		}
		if (maximum_latitude < -90.0 || maximum_latitude > 90.0) {
			clear_maximum_latitude();
			return false;
		}
		s_maximum_latitude = s;
		return true;
	}

	public final void clear_maximum_latitude () {
		maximum_latitude = 0.0;
		s_maximum_latitude = null;
		return;
	}




	// Set or clear the minimum latitude, false return indicates bad value.

	public final boolean set_minimum_latitude (double x) {
		return set_minimum_latitude (
			SimpleUtils.double_to_string_trailz ("%.7f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_minimum_latitude (String s) {
		if (s == null || s.isEmpty()) {
			clear_minimum_latitude();
			return EVS_OPTIONAL_BOUNDS;
		}
		try {
			minimum_latitude = Double.parseDouble(s);
		} catch (Exception e) {
			clear_minimum_latitude();
			return false;
		}
		if (minimum_latitude < -90.0 || minimum_latitude > 90.0) {
			clear_minimum_latitude();
			return false;
		}
		s_minimum_latitude = s;
		return true;
	}

	public final void clear_minimum_latitude () {
		minimum_latitude = 0.0;
		s_minimum_latitude = null;
		return;
	}




	// Set or clear the maximum longitude, false return indicates bad value.

	public final boolean set_maximum_longitude (double x) {
		return set_maximum_longitude (
			SimpleUtils.double_to_string_trailz ("%.7f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_maximum_longitude (String s) {
		if (s == null || s.isEmpty()) {
			clear_maximum_longitude();
			return EVS_OPTIONAL_BOUNDS;
		}
		try {
			maximum_longitude = Double.parseDouble(s);
		} catch (Exception e) {
			clear_maximum_longitude();
			return false;
		}
		if (maximum_longitude < -360.0 || maximum_longitude > 360.0) {
			clear_maximum_longitude();
			return false;
		}
		s_maximum_longitude = s;
		return true;
	}

	public final void clear_maximum_longitude () {
		maximum_longitude = 0.0;
		s_maximum_longitude = null;
		return;
	}




	// Set or clear the minimum longitude, false return indicates bad value.

	public final boolean set_minimum_longitude (double x) {
		return set_minimum_longitude (
			SimpleUtils.double_to_string_trailz ("%.7f", SimpleUtils.TRAILZ_REMOVE, x));
	}

	public final boolean set_minimum_longitude (String s) {
		if (s == null || s.isEmpty()) {
			clear_minimum_longitude();
			return EVS_OPTIONAL_BOUNDS;
		}
		try {
			minimum_longitude = Double.parseDouble(s);
		} catch (Exception e) {
			clear_minimum_longitude();
			return false;
		}
		if (minimum_longitude < -360.0 || minimum_longitude > 360.0) {
			clear_minimum_longitude();
			return false;
		}
		s_minimum_longitude = s;
		return true;
	}

	public final void clear_minimum_longitude () {
		minimum_longitude = 0.0;
		s_minimum_longitude = null;
		return;
	}




	// Set or clear the sequence start time, false return indicates bad value.

	public final boolean set_start_time (long x) {
		start_time = x;
		s_start_time = evs_time_to_string(x);
		return true;
	}

	public final boolean set_start_time (String s) {
		if (s == null || s.isEmpty()) {
			clear_start_time();
			return false;
		}
		try {
			start_time = evs_string_to_time(s);
		} catch (Exception e) {
			clear_start_time();
			return false;
		}
		s_start_time = s;
		return true;
	}

	public final void clear_start_time () {
		start_time = 0L;
		s_start_time = null;
		return;
	}

	// Set start time relative to the event time, negative delta is before the event time.

	public final boolean set_relative_start_time_millis (long delta_millis) {
		return set_start_time (event_time + delta_millis);
	}

	public final boolean set_relative_start_time_days (double delta_days) {
		return set_start_time (event_time + Math.round (delta_days * 86400000.0));
	}




	// Set or clear the sequence end time, false return indicates bad value.

	public final boolean set_end_time (long x) {
		end_time = x;
		s_end_time = evs_time_to_string(x);
		return true;
	}

	public final boolean set_end_time (String s) {
		if (s == null || s.isEmpty()) {
			clear_end_time();
			return true;
		}
		try {
			end_time = evs_string_to_time(s);
		} catch (Exception e) {
			clear_end_time();
			return false;
		}
		s_end_time = s;
		return true;
	}

	public final void clear_end_time () {
		end_time = 0L;
		s_end_time = null;
		return;
	}

	// Set end time relative to the event time, negative delta is before the event time.

	public final boolean set_relative_end_time_millis (long delta_millis) {
		return set_end_time (event_time + delta_millis);
	}

	public final boolean set_relative_end_time_days (double delta_days) {
		return set_end_time (event_time + Math.round (delta_days * 86400000.0));
	}




	// Return values from cap_end_time.

	public static final int CET_UNCHANGED	= 0;		// the end time was not changed
	public static final int CET_CAPPED		= 1;		// the end time was reduced (capped)
	public static final int CET_INVALID		= 2;		// the new end time would be invalid (producing null sequence)

	// Minimum duration of a capped sequence (an absolute minimum, the automatic system should impose a higher minimum)

	private static final long CAP_MIN_DURATION = 60000L;	// 1 minute

	// Cap the end time.
	// Parameters:
	//  cap_time = Sequence cap time, in milliseconds since the epoch.
	// Returns CET_XXXX as above.
	// If the cap time is at or after the current end time,
	//  then leave the end time unchanged and return CET_UNCHANGED.
	// Otherwise, if the cap time is before the current start time or event time plus 1 minute,
	//  the leave the end time unchanged and return CET_INVALID.
	// Otherwise, set the end time to the cap time and return CET_CAPPED.

	public final int cap_end_time (long cap_time) {

		// If after current end time, do nothing

		if (has_end_time() && cap_time >= end_time) {
			return CET_UNCHANGED;
		}

		// Check cap time would leave a sequence of at least 1 minute

		if (cap_time < start_time + CAP_MIN_DURATION) {
			return CET_INVALID;
		}

		if (has_event_time() && cap_time < event_time + CAP_MIN_DURATION) {
			return CET_INVALID;
		}

		// Otherwise, set a new end time

		set_end_time (cap_time);		// always returns true
		return CET_CAPPED;
	}

	// Check if the end time can be capped.
	// Parameters:
	//  cap_time = Sequence cap time, in milliseconds since the epoch.
	// Returns CET_XXXX as above.
	// If the cap time is at or after the current end time, return CET_UNCHANGED.
	// Otherwise, if the cap time is before the current start time or event time plus 1 minute, return CET_INVALID.
	// Otherwise, return CET_CAPPED.
	// Note: This function does not modify any properties.
	// Note: It is guaranteed that a subsequent call to cap_end_time will have the same return value.
	// Note: If the return is CET_CAPPED, then the end time can be capped by
	//  calling set_end_time (cap_time), or by calling cap_end_time (cap_time).

	public final int check_cap_end_time (long cap_time) {

		// If after current end time, do nothing

		if (has_end_time() && cap_time >= end_time) {
			return CET_UNCHANGED;
		}

		// Check cap time would leave a sequence of at least 1 minute

		if (cap_time < start_time + CAP_MIN_DURATION) {
			return CET_INVALID;
		}

		if (has_event_time() && cap_time < event_time + CAP_MIN_DURATION) {
			return CET_INVALID;
		}

		// Otherwise, we can cap the end time

		return CET_CAPPED;
	}




	// Set or clear the title of the sequence, false return indicates bad value.

	public final boolean set_title (String s) {
		if (s == null || s.isEmpty()) {
			clear_title();
			return false;
		}
		title = s;
		return true;
	}

	public final void clear_title () {
		title = null;
		return;
	}

	// Set the title of the sequence, given the title of the associated event.

	public final boolean set_title_from_event_title (String event_title) {
		String s = event_title;
		if (s == null || s.isEmpty()) {
			s = EVS_TITLE_UNKNOWN;
		}
		if (!( s.endsWith (EVS_TITLE_SUFFIX_SEQUENCE) )) {
			s = s + " " + EVS_TITLE_SUFFIX_SEQUENCE;
		}
		return set_title (s);
	}




	// Check the object invariant.
	// Returns null if OK, error message if invariant violated.
	// Note: This function assumes the object was created by starting clear
	// and then calling setter functions above.  So it does not check for
	// every possible error.

	public String check_invariant () {

		// Associated mainshock event

		if (EVS_OPTIONAL_EVENT) {
			if (!( (eventNetwork != null && eventCode != null) 
				|| (eventNetwork == null && eventCode == null) )) {
				return "Only one of network identifier and network code is specified";
			}
		} else {
			if (!( eventNetwork != null && eventCode != null )) {
				return "Network identifier and network code are not specified";
			}
		}

		// Event time

		if (!( EVS_OPTIONAL_EVENT_TIME )) {
			if (!( s_event_time != null )) {
				return "Event time is not specified";
			}
		}

		// Region type

		if (!( region_type != null )) {
			return "Region type is not specified";
		}
		if (!( is_region_circle() || is_region_rectangle() )) {
			return "Region type is invalid";
		}

		// Circle

		if (is_region_circle()) {
			if (!( s_circle_longitude != null
				&& s_circle_latitude != null
				&& s_circle_radius_km != null )) {
				return "Circle parameters are not all specified";
			}
		} else {
			if (!( s_circle_longitude == null
				&& s_circle_latitude == null
				&& s_circle_radius_km == null )) {
				return "Circle parameters are specified but region is not a circle";
			}
		}

		// Rectangle

		if (is_region_rectangle() || (!EVS_OPTIONAL_BOUNDS)) {
			if (!( s_maximum_latitude != null
				&& s_minimum_latitude != null
				&& s_maximum_longitude != null
				&& s_minimum_longitude != null )) {
				return "Rectangle parameters are not all specified";
			}
			if (!( minimum_latitude <= maximum_latitude )) {
				return "Rectangle minimum and maximum latitudes are reversed";
			}
			if (!( minimum_longitude <= maximum_longitude )) {
				return "Rectangle minimum and maximum longitudes are reversed";
			}
		} else {
			if (!( s_maximum_latitude == null
				&& s_minimum_latitude == null
				&& s_maximum_longitude == null
				&& s_minimum_longitude == null )) {
				return "Rectangle parameters are specified but region is not a circle";
			}
		}

		// Start time

		if (!( s_start_time != null )) {
			return "Start time is not specified";
		}

		// End time

		if (s_end_time != null) {
			if (!( start_time <= end_time )) {
				return "End time is before start time";
			}
		}

		// Title

		if (!( title != null )) {
			return "Title is not specified";
		}

		// All OK

		return null;
	}




	// Check the object invariant, for constructing a delete product.
	// Returns null if OK, error message if invariant violated.
	// Note: This function assumes the object was created by starting clear
	// and then calling setter functions above.  So it does not check for
	// every possible error.

	public String check_invariant_for_delete () {

		// Associated mainshock event

		if (EVS_OPTIONAL_EVENT) {
			if (!( (eventNetwork != null && eventCode != null) 
				|| (eventNetwork == null && eventCode == null) )) {
				return "Only one of network identifier and network code is specified";
			}
		} else {
			if (!( eventNetwork != null && eventCode != null )) {
				return "Network identifier and network code are not specified";
			}
		}

		// Event time

		if (!( s_event_time == null )) {
			return "Event time is specified, but this is a delete product";
		}

		// Region type

		if (!( region_type == null )) {
			return "Region type is specified, but this is a delete product";
		}

		// Circle

		if (!( s_circle_longitude == null
			&& s_circle_latitude == null
			&& s_circle_radius_km == null )) {
			return "Circle parameters are specified, but this is a delete product";
		}

		// Rectangle

		if (!( s_maximum_latitude == null
			&& s_minimum_latitude == null
			&& s_maximum_longitude == null
			&& s_minimum_longitude == null )) {
			return "Rectangle parameters are specified, but this is a delete product";
		}

		// Start time

		if (!( s_start_time == null )) {
			return "Start time is specified, but this is a delete product";
		}

		// End time

		if (!( s_end_time == null )) {
			return "End time is specified, but this is a delete product";
		}


		// Title

		if (!( title == null )) {
			return "Title is specified, but this is a delete product";
		}

		// All OK

		return null;
	}




	//----- Construction -----




	// Clear all variables.

	public final void clear () {
		clear_eventNetwork();
		clear_eventCode();

		clear_event_time();

		clear_region_type();

		clear_circle_longitude();
		clear_circle_latitude();
		clear_circle_radius_km();

		clear_maximum_latitude();
		clear_minimum_latitude();
		clear_maximum_longitude();
		clear_minimum_longitude();

		clear_start_time();
		clear_end_time();

		clear_title();
		return;
	}




	// Constructor makes an empty object.

	public PropertiesEventSequence () {
		clear();
	}




	// Copy the contents from another object.
	// Returns this object.

	public PropertiesEventSequence copy_from (PropertiesEventSequence other) {

		eventNetwork		= other.eventNetwork;
		eventCode			= other.eventCode;

		event_time			= other.event_time;
		s_event_time		= other.s_event_time;

		region_type			= other.region_type;

		circle_longitude	= other.circle_longitude;
		s_circle_longitude	= other.s_circle_longitude;
		circle_latitude		= other.circle_latitude;
		s_circle_latitude	= other.s_circle_latitude;
		circle_radius_km	= other.circle_radius_km;
		s_circle_radius_km	= other.s_circle_radius_km;

		maximum_latitude	= other.maximum_latitude;
		s_maximum_latitude	= other.s_maximum_latitude;
		minimum_latitude	= other.minimum_latitude;
		s_minimum_latitude	= other.s_minimum_latitude;
		maximum_longitude	= other.maximum_longitude;
		s_maximum_longitude	= other.s_maximum_longitude;
		minimum_longitude	= other.minimum_longitude;
		s_minimum_longitude	= other.s_minimum_longitude;

		start_time			= other.start_time;
		s_start_time		= other.s_start_time;
		end_time			= other.end_time;
		s_end_time			= other.s_end_time;

		title				= other.title;

		return this;
	}




	// Subroutines to show parameter values, with final newline

	private String showval_string (String name, String s) {
		if (s == null) {
			return name + " = " + "<null>" + "\n";
		}
		return name + " = \"" + s + "\"\n";
	}

	private String showval_long (String name, String s, long x) {
		if (s == null) {
			return name + " = " + "<null>" + " = " + x + "\n";
		}
		return name + " = \"" + s + "\" = " + x + "\n";
	}

	private String showval_time (String name, String s, long x) {
		if (s == null) {
			return name + " = " + "<null>" + " = " + SimpleUtils.time_raw_and_string_with_cutoff(x, 0L) + "\n";
		}
		return name + " = \"" + s + "\" = " + SimpleUtils.time_raw_and_string_with_cutoff(x, 0L) + "\n";
	}

	private String showval_double (String name, String s, double x) {
		if (s == null) {
			return name + " = " + "<null>" + " = " + x + "\n";
		}
		return name + " = \"" + s + "\" = " + x + "\n";
	}




	// toString - Convert to string.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("PropertiesEventSequence:" + "\n");

		result.append (showval_string ("eventNetwork", eventNetwork));
		result.append (showval_string ("eventCode", eventCode));

		result.append (showval_time ("event_time", s_event_time, event_time));

		result.append (showval_string ("region_type", region_type));

		result.append (showval_double ("circle_longitude", s_circle_longitude, circle_longitude));
		result.append (showval_double ("circle_latitude", s_circle_latitude, circle_latitude));
		result.append (showval_double ("circle_radius_km", s_circle_radius_km, circle_radius_km));

		result.append (showval_double ("maximum_latitude", s_maximum_latitude, maximum_latitude));
		result.append (showval_double ("minimum_latitude", s_minimum_latitude, minimum_latitude));
		result.append (showval_double ("maximum_longitude", s_maximum_longitude, maximum_longitude));
		result.append (showval_double ("minimum_longitude", s_minimum_longitude, minimum_longitude));

		result.append (showval_time ("start_time", s_start_time, start_time));
		result.append (showval_time ("end_time", s_end_time, end_time));

		result.append (showval_string ("title", title));

		return result.toString();
	}




	// Get the property map and convert to string.

	public String property_map_to_string (boolean isReviewed) {
		StringBuilder result = new StringBuilder();

		LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
		write_to_property_map (properties, isReviewed);

		for (Map.Entry<String, String> entry : properties.entrySet()) {
			result.append (entry.getKey() + " = " + entry.getValue() + "\n");
		}

		return result.toString();
	}




	//----- Product access -----




	// Read contents from a product GeoJson.
	// Parameters:
	//  gj_product = GeoJson containing the product.
	// Returns true if success, false if data missing or mis-formatted.

	public boolean read_from_product_gj (JSONObject gj_product) {

		clear();

		// Event network

		if (!( set_eventNetwork (GeoJsonUtils.getString (gj_product, "properties", "eventsource")) )) {
			return false;
		}

		// Event network code

		if (!( set_eventCode (GeoJsonUtils.getString (gj_product, "properties", "eventsourcecode")) )) {
			return false;
		}

		// Event time

		if (!( set_event_time (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_EVENT_TIME)) )) {
			return false;
		}

		// Region selection

		if (!( set_region_type (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_REGION_TYPE)) )) {
			return false;
		}

		// Circle

		if (is_region_circle()) {

			if (!( set_circle_longitude (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_CIRCLE_LONGITUDE)) )) {
				return false;
			}

			if (!( set_circle_latitude (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_CIRCLE_LATITUDE)) )) {
				return false;
			}

			if (!( set_circle_radius_km (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_CIRCLE_RADIUS_KM)) )) {
				return false;
			}

		}

		// Rectangle

		if (is_region_rectangle() || (!EVS_OPTIONAL_BOUNDS)) {

			if (!( set_maximum_latitude (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_MAX_LATITUDE)) )) {
				return false;
			}

			if (!( set_minimum_latitude (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_MIN_LATITUDE)) )) {
				return false;
			}

			if (!( set_maximum_longitude (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_MAX_LONGITUDE)) )) {
				return false;
			}

			if (!( set_minimum_longitude (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_MIN_LONGITUDE)) )) {
				return false;
			}

		}

		// Start time

		if (!( set_start_time (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_START_TIME)) )) {
			return false;
		}

		// End time

		if (!( set_end_time (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_END_TIME)) )) {
			return false;
		}

		// Title

		if (!( set_title (GeoJsonUtils.getString (gj_product, "properties", EVS_NAME_TITLE)) )) {
			return false;
		}

		// Check invariant

		if (check_invariant() != null) {
			return false;
		}

		return true;
	}




	// Read contents from a product GeoJson, for a delete product.
	// Parameters:
	//  gj_product = GeoJson containing the product.
	// Returns true if success, false if data missing or mis-formatted.

	public boolean read_from_product_gj_for_delete (JSONObject gj_product) {

		clear();

		// Event network

		if (!( set_eventNetwork (GeoJsonUtils.getString (gj_product, "properties", "eventsource")) )) {
			return false;
		}

		// Event network code

		if (!( set_eventCode (GeoJsonUtils.getString (gj_product, "properties", "eventsourcecode")) )) {
			return false;
		}

		// Check invariant

		if (check_invariant_for_delete() != null) {
			return false;
		}

		return true;
	}




	// Subroutine to put a string into a map if it is non-null.

	private void put_if_non_null (Map<String, String> properties, String name, String s) {
		if (s != null) {
			properties.put (name, s);
		}
		return;
	}




	// Write to a property map.
	// Parameters:
	//  isReviewed = Product review status to insert into the map.

	public void write_to_property_map (Map<String, String> properties, boolean isReviewed) {

		// Event network

		put_if_non_null (properties, "eventsource", eventNetwork);

		// Event network code

		put_if_non_null (properties, "eventsourcecode", eventCode);

		// Review status

		if (isReviewed) {
			properties.put ("review-status", "reviewed");	// "reviewed" or "automatic"
		}

		// Event time

		put_if_non_null (properties, EVS_NAME_EVENT_TIME, s_event_time);

		// Region selection

		put_if_non_null (properties, EVS_NAME_REGION_TYPE, region_type);

		// Circle

		if (region_type != null) {
			if (is_region_circle()) {

				put_if_non_null (properties, EVS_NAME_CIRCLE_LONGITUDE, s_circle_longitude);

				put_if_non_null (properties, EVS_NAME_CIRCLE_LATITUDE, s_circle_latitude);

				put_if_non_null (properties, EVS_NAME_CIRCLE_RADIUS_KM, s_circle_radius_km);

			}
		}

		// Rectangle

		if (region_type != null) {
			if (is_region_rectangle() || (!EVS_OPTIONAL_BOUNDS)) {

				put_if_non_null (properties, EVS_NAME_MAX_LATITUDE, s_maximum_latitude);

				put_if_non_null (properties, EVS_NAME_MIN_LATITUDE, s_minimum_latitude);

				put_if_non_null (properties, EVS_NAME_MAX_LONGITUDE, s_maximum_longitude);

				put_if_non_null (properties, EVS_NAME_MIN_LONGITUDE, s_minimum_longitude);

			}
		}

		// Start time

		put_if_non_null (properties, EVS_NAME_START_TIME, s_start_time);

		// End time

		put_if_non_null (properties, EVS_NAME_END_TIME, s_end_time);

		// Title

		put_if_non_null (properties, EVS_NAME_TITLE, title);

		return;
	}




	//----- Additional setters -----




	// Set from an event GeoJson.
	// Parameters:
	//  gj_event = GeoJson containing the event.
	// Returns true if success, false if data missing or mis-formatted.
	// This function sets the following properties:
	//  eventNetwork
	//  eventCode
	//  event_time
	//  title

	public boolean set_from_event_gj (JSONObject gj_event) {

		// Event network

		String s = GeoJsonUtils.getString (gj_event, "properties", "net");
		if (s == null || s.isEmpty()) {
			return false;
		}
		if (!( set_eventNetwork (s) )) {
			return false;
		}

		// Event network code

		s = GeoJsonUtils.getString (gj_event, "properties", "code");
		if (s == null || s.isEmpty()) {
			return false;
		}
		if (!( set_eventCode (s) )) {
			return false;
		}

		// Event time

		Long t = GeoJsonUtils.getTimeMillis (gj_event, "properties", "time");
		if (t == null) {
			return false;
		}
		if (!( set_event_time (t.longValue()) )) {
			return false;
		}

		// Title

		s = GeoJsonUtils.getString (gj_event, "properties", "title");
		//if (s == null || s.isEmpty()) {
		//	return false;
		//}
		if (!( set_title_from_event_title (s) )) {
			return false;
		}

		return true;
	}




	// Set from an event GeoJson, for a delete product.
	// Parameters:
	//  gj_event = GeoJson containing the event.
	// Returns true if success, false if data missing or mis-formatted.
	// This function sets the following properties:
	//  eventNetwork
	//  eventCode

	public boolean set_from_event_gj_for_delete (JSONObject gj_event) {

		// Event network

		String s = GeoJsonUtils.getString (gj_event, "properties", "net");
		if (s == null || s.isEmpty()) {
			return false;
		}
		if (!( set_eventNetwork (s) )) {
			return false;
		}

		// Event network code

		s = GeoJsonUtils.getString (gj_event, "properties", "code");
		if (s == null || s.isEmpty()) {
			return false;
		}
		if (!( set_eventCode (s) )) {
			return false;
		}

		return true;
	}




	// Set up from an event network and code, for a delete product.
	// Parameters:
	//  the_eventNetwork = Network identifier for the event (for example, "us").
	//  the_eventCode =  Network code for the event (for example, "10006jv5").
	// Returns true if success, false if data missing or mis-formatted.
	// Note: The network and code serve to identifiy the event to which the PDL product is attached.
	// This function sets the following properties:
	//  eventNetwork
	//  eventCode
	// Other properties are cleared, as needed for a delete product.

	public boolean setup_from_event_network_and_code_for_delete (String the_eventNetwork, String the_eventCode) {

		clear();

		// Event network

		if (the_eventNetwork == null || the_eventNetwork.isEmpty()) {
			return false;
		}
		if (!( set_eventNetwork (the_eventNetwork) )) {
			return false;
		}

		// Event network code

		if (the_eventCode == null || the_eventCode.isEmpty()) {
			return false;
		}
		if (!( set_eventCode (the_eventCode) )) {
			return false;
		}

		// Check invariant

		if (check_invariant_for_delete() != null) {
			return false;
		}

		return true;
	}




	// Overwrite event network and code,.
	// Parameters:
	//  the_eventNetwork = Network identifier for the event (for example, "us").
	//  the_eventCode =  Network code for the event (for example, "10006jv5").
	// Returns true if success, false if data missing or mis-formatted.
	// Note: The network and code serve to identifiy the event to which the PDL product is attached.
	// This function sets the following properties:
	//  eventNetwork
	//  eventCode
	// Other properties are unchanged.

	public boolean overwrite_event_network_and_code (String the_eventNetwork, String the_eventCode) {

		// Event network

		if (the_eventNetwork == null || the_eventNetwork.isEmpty()) {
			return false;
		}
		if (!( set_eventNetwork (the_eventNetwork) )) {
			return false;
		}

		// Event network code

		if (the_eventCode == null || the_eventCode.isEmpty()) {
			return false;
		}
		if (!( set_eventCode (the_eventCode) )) {
			return false;
		}

		return true;
	}




	// Set region properties from an SphRegion.
	// Parameters:
	//  sph_region = Region on the sphere, must be a circle or rectangle.
	// Returns true if success, false if region is not a circle or rectangle, or some other problem.

	public boolean set_region_from_sph (SphRegion sph_region) {

		// Circle

		if (sph_region.isCircular()) {

			if (!( set_region_circle() )) {
				return false;
			}

			if (!( set_circle_longitude (sph_region.getCircleCenterLon()) )) {
				return false;
			}

			if (!( set_circle_latitude (sph_region.getCircleCenterLat()) )) {
				return false;
			}

			if (!( set_circle_radius_km (sph_region.getCircleRadiusKm()) )) {
				return false;
			}

			// If we need to also set rectangular bounds ...

			if (!( EVS_OPTIONAL_BOUNDS )) {

				if (!( set_maximum_latitude (sph_region.getMaxLat()) )) {
					return false;
				}

				if (!( set_minimum_latitude (sph_region.getMinLat()) )) {
					return false;
				}

				if (!( set_maximum_longitude (sph_region.getMaxLon()) )) {
					return false;
				}

				if (!( set_minimum_longitude (sph_region.getMinLon()) )) {
					return false;
				}
			}

			return true;
		}

		// Rectangle

		if (sph_region.isRectangular()) {

			if (!( set_region_rectangle() )) {
				return false;
			}

			if (!( set_maximum_latitude (sph_region.getMaxLat()) )) {
				return false;
			}

			if (!( set_minimum_latitude (sph_region.getMinLat()) )) {
				return false;
			}

			if (!( set_maximum_longitude (sph_region.getMaxLon()) )) {
				return false;
			}

			if (!( set_minimum_longitude (sph_region.getMinLon()) )) {
				return false;
			}

			return true;
		}

		// Invalid type of region

		return false;
	}




	//----- Marshaling -----




	// Marshal a string, with null strings converted to empty strings.

	protected final void marshal_string_or_null (MarshalWriter writer, String name, String s) {
		writer.marshalString (name, (s == null) ? "" : s);
		return;
	}




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 98001;		// For normal product
	private static final int MARSHAL_VER_2 = 98002;		// For delete product

	private static final String M_VERSION_NAME = "PropertiesEventSequence";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 98000;
	protected static final int MARSHAL_PROD_EVSEQ = 98001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_PROD_EVSEQ;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = ((region_type != null) ? MARSHAL_VER_1 : MARSHAL_VER_2);
		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("PropertiesEventSequence.do_marshal: Unknown version number: " + ver);

		case MARSHAL_VER_1:

			marshal_string_or_null (writer, "eventNetwork", eventNetwork);
			marshal_string_or_null (writer, "eventCode", eventCode);

			marshal_string_or_null (writer, "event_time", s_event_time);

			marshal_string_or_null (writer, "region_type", region_type);

			marshal_string_or_null (writer, "circle_longitude", s_circle_longitude);
			marshal_string_or_null (writer, "circle_latitude", s_circle_latitude);
			marshal_string_or_null (writer, "circle_radius_km", s_circle_radius_km);

			marshal_string_or_null (writer, "maximum_latitude", s_maximum_latitude);
			marshal_string_or_null (writer, "minimum_latitude", s_minimum_latitude);
			marshal_string_or_null (writer, "maximum_longitude", s_maximum_longitude);
			marshal_string_or_null (writer, "minimum_longitude", s_minimum_longitude);

			marshal_string_or_null (writer, "start_time", s_start_time);
			marshal_string_or_null (writer, "end_time", s_end_time);

			marshal_string_or_null (writer, "title", title);

			break;

		case MARSHAL_VER_2:

			marshal_string_or_null (writer, "eventNetwork", eventNetwork);
			marshal_string_or_null (writer, "eventCode", eventCode);

			break;
		}
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		String s;

		switch (ver) {

		default:
			throw new MarshalException ("PropertiesEventSequence.do_umarshal: Unknown version number: " + ver);

		case MARSHAL_VER_1:

			clear();

			s = reader.unmarshalString ("eventNetwork");
			if (!( set_eventNetwork(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad eventNetwork: " + s);
			}

			s = reader.unmarshalString ("eventCode");
			if (!( set_eventCode(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad eventCode: " + s);
			}

			s = reader.unmarshalString ("event_time");
			if (!( set_event_time(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad event_time: " + s);
			}

			s = reader.unmarshalString ("region_type");
			if (!( set_region_type(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad region_type: " + s);
			}

			s = reader.unmarshalString ("circle_longitude");
			if (!( set_circle_longitude(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad circle_longitude: " + s);
			}

			s = reader.unmarshalString ("circle_latitude");
			if (!( set_circle_latitude(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad circle_latitude: " + s);
			}

			s = reader.unmarshalString ("circle_radius_km");
			if (!( set_circle_radius_km(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad circle_radius_km: " + s);
			}

			s = reader.unmarshalString ("maximum_latitude");
			if (!( set_maximum_latitude(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad maximum_latitude: " + s);
			}

			s = reader.unmarshalString ("minimum_latitude");
			if (!( set_minimum_latitude(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad minimum_latitude: " + s);
			}

			s = reader.unmarshalString ("maximum_longitude");
			if (!( set_maximum_longitude(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad maximum_longitude: " + s);
			}

			s = reader.unmarshalString ("minimum_longitude");
			if (!( set_minimum_longitude(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad minimum_longitude: " + s);
			}

			s = reader.unmarshalString ("start_time");
			if (!( set_start_time(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad start_time: " + s);
			}

			s = reader.unmarshalString ("end_time");
			if (!( set_end_time(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad end_time: " + s);
			}

			s = reader.unmarshalString ("title");
			if (!( set_title(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad title: " + s);
			}

			s = check_invariant();
			if (s != null) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Invariant violation: " + s);
			}

			break;

		case MARSHAL_VER_2:

			clear();

			s = reader.unmarshalString ("eventNetwork");
			if (!( set_eventNetwork(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad eventNetwork: " + s);
			}

			s = reader.unmarshalString ("eventCode");
			if (!( set_eventCode(s) )) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Bad eventCode: " + s);
			}

			s = check_invariant_for_delete();
			if (s != null) {
				throw new MarshalException ("PropertiesEventSequence.do_umarshal: Invariant violation: " + s);
			}

			break;
		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public PropertiesEventSequence unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, PropertiesEventSequence obj) {

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

	public static PropertiesEventSequence unmarshal_poly (MarshalReader reader, String name) {
		PropertiesEventSequence result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("PropertiesEventSequence.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_PROD_EVSEQ:
			result = new PropertiesEventSequence();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----




	// Make event sequence properties for a mainshock, for testing purposes.
	// Parameters:
	//  gj_event = Geojson for mainshock.
	//  f_circle = True to create circle, false to create rectangle.
	//  radius_km = Circle radius, or rectange half-side, in km.
	//  start_delta_days = Start time relative to mainshock time, in days.
	//  end_delta_days = End time relative to mainshock time, in days, 0.0 to omit.
	// Returns the event sequence properties.
	// Throws exception if error.

	public static PropertiesEventSequence test_make_for_mainshock (JSONObject gj_event,
		boolean f_circle, double radius_km, double start_delta_days, double end_delta_days) {

		PropertiesEventSequence props = new PropertiesEventSequence();

		// Set properties for mainshock

		if (!( props.set_from_event_gj (gj_event) )) {
			throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock: Failed to set properties for mainshock");
		}

		// Set start time

		if (!( props.set_relative_start_time_days (start_delta_days) )) {
			throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock: Failed to set relative start time");
		}

		// Set end time, if requested

		if (end_delta_days > 1.0e-20) {
			if (!( props.set_relative_end_time_days (end_delta_days) )) {
				throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock: Failed to set relative end time");
			}
		}

		// Construct a region surrounding the hypocenter

		double event_lon = GeoJsonUtils.getDouble (gj_event, "geometry", "coordinates", 0);
		double event_lat = GeoJsonUtils.getDouble (gj_event, "geometry", "coordinates", 1);

		SphLatLon epicenter = new SphLatLon (event_lat, event_lon);
		SphRegion sph_region;

		if (f_circle) {
			sph_region = SphRegion.makeCircle (epicenter, radius_km);
		}

		else {
			double half_side = SphLatLon.distance_to_deg (radius_km);

			double ur_lat = Math.min (event_lat + half_side, 90.0);
			double ll_lat = Math.max (event_lat - half_side, -90.0);

			double ur_lon = event_lon + half_side;
			if (ur_lon > 180.0) {
				ur_lon -= 360.0;
			}

			double ll_lon = event_lon - half_side;
			if (ll_lon < -180.0) {
				ll_lon += 360.0;
			}

			SphLatLon ur_corner = new SphLatLon (ur_lat, ur_lon);
			SphLatLon ll_corner = new SphLatLon (ll_lat, ll_lon);

			sph_region = SphRegion.makeMercRectangle (ur_corner, ll_corner);
		}

		// Set region

		if (!( props.set_region_from_sph (sph_region) )) {
			throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock: Failed to set region");
		}

		// Check invariant

		String inv = props.check_invariant();

		if (inv != null) {
			throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock: Invariant check failed: " + inv);
		}

		return props;
	}




	// Make event sequence properties for a mainshock, for a delete product, for testing purposes.
	// Parameters:
	//  gj_event = Geojson for mainshock.
	// Returns the event sequence properties.
	// Throws exception if error.

	public static PropertiesEventSequence test_make_for_mainshock_for_delete (JSONObject gj_event) {

		PropertiesEventSequence props = new PropertiesEventSequence();

		// Set properties for mainshock

		if (!( props.set_from_event_gj_for_delete (gj_event) )) {
			throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock_for_delete: Failed to set properties for mainshock");
		}

		// Check invariant

		String inv = props.check_invariant_for_delete();

		if (inv != null) {
			throw new IllegalArgumentException ("PropertiesEventSequence.test_make_for_mainshock_for_delete: Invariant check failed: " + inv);
		}

		return props;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("PropertiesEventSequence : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  event_id  f_circle  radius_km  start_delta_days  end_delta_days  isReviewed
		// See test_make_for_mainshock for parameter description
		// Fetch information for an event, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 6 additional arguments

			if (args.length != 7) {
				System.err.println ("PropertiesEventSequence : Invalid 'test1' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean f_circle = Boolean.parseBoolean (args[2]);
				double radius_km = Double.parseDouble (args[3]);
				double start_delta_days = Double.parseDouble (args[4]);
				double end_delta_days = Double.parseDouble (args[5]);
				boolean isReviewed = Boolean.parseBoolean (args[6]);

				// Say hello

				System.out.println ("Constructing event sequence properties for event");
				System.out.println ("event_id: " + event_id);
				System.out.println ("f_circle: " + f_circle);
				System.out.println ("radius_km: " + radius_km);
				System.out.println ("start_delta_days: " + start_delta_days);
				System.out.println ("end_delta_days: " + end_delta_days);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock (gj_event, f_circle, radius_km, start_delta_days, end_delta_days);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  event_id  f_circle  radius_km  start_delta_days  end_delta_days  isReviewed
		// See test_make_for_mainshock for parameter description
		// Fetch information for an event, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled info.

		if (args[0].equalsIgnoreCase ("test2")) {

			// 6 additional arguments

			if (args.length != 7) {
				System.err.println ("PropertiesEventSequence : Invalid 'test2' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean f_circle = Boolean.parseBoolean (args[2]);
				double radius_km = Double.parseDouble (args[3]);
				double start_delta_days = Double.parseDouble (args[4]);
				double end_delta_days = Double.parseDouble (args[5]);
				boolean isReviewed = Boolean.parseBoolean (args[6]);

				// Say hello

				System.out.println ("Constructing event sequence properties for event");
				System.out.println ("event_id: " + event_id);
				System.out.println ("f_circle: " + f_circle);
				System.out.println ("radius_km: " + radius_km);
				System.out.println ("start_delta_days: " + start_delta_days);
				System.out.println ("end_delta_days: " + end_delta_days);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock (gj_event, f_circle, radius_km, start_delta_days, end_delta_days);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				PropertiesEventSequence.marshal_poly (store, null, props);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);
				System.out.println ("");

				// Unmarshal from JSON
			
				PropertiesEventSequence props2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				props2 = PropertiesEventSequence.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				// Display the contents

				System.out.println (props2.toString());

				// Display the property map

				System.out.println (props2.property_map_to_string (isReviewed));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  event_id  isReviewed
		// See test_make_for_mainshock_for_delete for parameter description.
		// Fetch information for an event, for a delete product, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.

		if (args[0].equalsIgnoreCase ("test3")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("PropertiesEventSequence : Invalid 'test3' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean isReviewed = Boolean.parseBoolean (args[2]);

				// Say hello

				System.out.println ("Constructing event sequence properties for event, for a delete product");
				System.out.println ("event_id: " + event_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock_for_delete (gj_event);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  event_id  isReviewed
		// See test_make_for_mainshock_for_delete for parameter description.
		// Fetch information for an event, for a delete product, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.
		// Then marshal to JSON, and display the JSON.
		// Then unmarshal, and display the unmarshaled info.

		if (args[0].equalsIgnoreCase ("test4")) {

			// 2 additional arguments

			if (args.length != 3) {
				System.err.println ("PropertiesEventSequence : Invalid 'test4' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean isReviewed = Boolean.parseBoolean (args[2]);

				// Say hello

				System.out.println ("Constructing event sequence properties for event, for a delete product");
				System.out.println ("event_id: " + event_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock_for_delete (gj_event);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

				// Marshal to JSON

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				PropertiesEventSequence.marshal_poly (store, null, props);
				store.check_write_complete ();
				String json_string = store.get_json_string();

				System.out.println ("");
				System.out.println (json_string);
				System.out.println ("");

				// Unmarshal from JSON
			
				PropertiesEventSequence props2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				props2 = PropertiesEventSequence.unmarshal_poly (retrieve, null);
				retrieve.check_read_complete ();

				// Display the contents

				System.out.println (props2.toString());

				// Display the property map

				System.out.println (props2.property_map_to_string (isReviewed));

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5  event_id  f_circle  radius_km  start_delta_days  end_delta_days  isReviewed  pdl_code
		// See test_make_for_mainshock for parameter description
		// Fetch information for an event, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.
		// Then build the product and send it to PDL-development using the specified code.
		// Same as test #1 except it sends the product to PDL-development.

		if (args[0].equalsIgnoreCase ("test5")) {

			// 7 additional arguments

			if (args.length != 8) {
				System.err.println ("PropertiesEventSequence : Invalid 'test5' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean f_circle = Boolean.parseBoolean (args[2]);
				double radius_km = Double.parseDouble (args[3]);
				double start_delta_days = Double.parseDouble (args[4]);
				double end_delta_days = Double.parseDouble (args[5]);
				boolean isReviewed = Boolean.parseBoolean (args[6]);
				String pdl_code = args[7];

				// Say hello

				System.out.println ("Constructing event sequence properties for event");
				System.out.println ("event_id: " + event_id);
				System.out.println ("f_circle: " + f_circle);
				System.out.println ("radius_km: " + radius_km);
				System.out.println ("start_delta_days: " + start_delta_days);
				System.out.println ("end_delta_days: " + end_delta_days);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("pdl_code: " + pdl_code);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock (gj_event, f_circle, radius_km, start_delta_days, end_delta_days);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

				// Direct operation to PDL-Development

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

				// Make the PDL product

				Map<String, String> extra_properties = new LinkedHashMap<String, String>();
				extra_properties.put (PropertiesEventSequence.EVS_EXTRA_GENERATED_BY, VersionInfo.get_generator_name());

				String jsonText = null;
				long modifiedTime = 0L;

				Product product = PDLProductBuilderEventSequence.createProduct (
					pdl_code, props, extra_properties, isReviewed, jsonText, modifiedTime);

				// Stop if unable to create product

				if (product == null) {
					System.out.println ("PDLProductBuilderEventSequence.createProduct returned null");
					return;
				}

				// Sign the product

				PDLSender.signProduct(product);

				// Send the product, true means it is text

				PDLSender.sendProduct(product, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6  event_id  isReviewed  pdl_code
		// See test_make_for_mainshock_for_delete for parameter description.
		// Fetch information for an event, for a delete product, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.
		// Then build the delete product and send it to PDL-development using the specified code.
		// Same as test #3 except it sends the deletion product to PDL-development.

		if (args[0].equalsIgnoreCase ("test6")) {

			// 3 additional arguments

			if (args.length != 4) {
				System.err.println ("PropertiesEventSequence : Invalid 'test5' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean isReviewed = Boolean.parseBoolean (args[2]);
				String pdl_code = args[3];

				// Say hello

				System.out.println ("Constructing event sequence properties for event, for a delete product");
				System.out.println ("event_id: " + event_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("pdl_code: " + pdl_code);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock_for_delete (gj_event);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

				// Direct operation to PDL-Development

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

				// Make the PDL delete product

				Map<String, String> extra_properties = null;

				long modifiedTime = 0L;

				Product product = PDLProductBuilderEventSequence.createDeletionProduct (
					pdl_code, props, extra_properties, isReviewed, modifiedTime);

				// Stop if unable to create product

				if (product == null) {
					System.out.println ("PDLProductBuilderEventSequence.createDeletionProduct returned null");
					return;
				}

				// Sign the product

				PDLSender.signProduct(product);

				// Send the product, true means it is text

				PDLSender.sendProduct(product, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  event_id  f_circle  radius_km  start_delta_days  end_delta_days  isReviewed  pdl_code  pdl_enable  [pdl_key_filename]
		// See test_make_for_mainshock for parameter description
		// Fetch information for an event, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.
		// Then build the product and send it to PDL using the specified code.
		// Same as test #1 and #5 except it sends the product to PDL.

		if (args[0].equalsIgnoreCase ("test7")) {

			// 8 or 9 additional arguments

			if (args.length != 9 && args.length != 10) {
				System.err.println ("PropertiesEventSequence : Invalid 'test7' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean f_circle = Boolean.parseBoolean (args[2]);
				double radius_km = Double.parseDouble (args[3]);
				double start_delta_days = Double.parseDouble (args[4]);
				double end_delta_days = Double.parseDouble (args[5]);
				boolean isReviewed = Boolean.parseBoolean (args[6]);
				String pdl_code = args[7];
				int pdl_enable = Integer.parseInt (args[8]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
				String pdl_key_filename = null;
				if (args.length >= 10) {
					pdl_key_filename = args[9];
				}

				// Say hello

				System.out.println ("Constructing event sequence properties for event");
				System.out.println ("event_id: " + event_id);
				System.out.println ("f_circle: " + f_circle);
				System.out.println ("radius_km: " + radius_km);
				System.out.println ("start_delta_days: " + start_delta_days);
				System.out.println ("end_delta_days: " + end_delta_days);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("pdl_code: " + pdl_code);
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + ((pdl_key_filename == null) ? "<null>" : pdl_key_filename));
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock (gj_event, f_circle, radius_km, start_delta_days, end_delta_days);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

				//  // Direct operation to PDL-Development
				//  
				//  ServerConfig server_config = new ServerConfig();
				//  server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (pdl_key_filename != null) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the PDL product

				Map<String, String> extra_properties = new LinkedHashMap<String, String>();
				extra_properties.put (PropertiesEventSequence.EVS_EXTRA_GENERATED_BY, VersionInfo.get_generator_name());

				String jsonText = null;
				long modifiedTime = 0L;

				Product product = PDLProductBuilderEventSequence.createProduct (
					pdl_code, props, extra_properties, isReviewed, jsonText, modifiedTime);

				// Stop if unable to create product

				if (product == null) {
					System.out.println ("PDLProductBuilderEventSequence.createProduct returned null");
					return;
				}

				// Sign the product

				PDLSender.signProduct(product);

				// Send the product, true means it is text

				PDLSender.sendProduct(product, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #8
		// Command format:
		//  test8  event_id  isReviewed  pdl_code  pdl_enable  [pdl_key_filename]
		// See test_make_for_mainshock_for_delete for parameter description.
		// Fetch information for an event, for a delete product, and display it.
		// Then construct a properties object and display it.
		// Then display the resulting property map.
		// Then build the delete product and send it to PDL using the specified code.
		// Same as test #3 and #6 except it sends the deletion product to PDL.

		if (args[0].equalsIgnoreCase ("test8")) {

			// 4 or 5 additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("PropertiesEventSequence : Invalid 'test8' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean isReviewed = Boolean.parseBoolean (args[2]);
				String pdl_code = args[3];
				int pdl_enable = Integer.parseInt (args[4]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
				String pdl_key_filename = null;
				if (args.length >= 6) {
					pdl_key_filename = args[5];
				}

				// Say hello

				System.out.println ("Constructing event sequence properties for event, for a delete product");
				System.out.println ("event_id: " + event_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("pdl_code: " + pdl_code);
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + ((pdl_key_filename == null) ? "<null>" : pdl_key_filename));
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Build event sequence properties

				JSONObject gj_event = accessor.get_last_geojson();

				PropertiesEventSequence props = test_make_for_mainshock_for_delete (gj_event);

				// Display the contents

				System.out.println (props.toString());

				// Display the property map

				System.out.println (props.property_map_to_string (isReviewed));

				//  // Direct operation to PDL-Development
				//  
				//  ServerConfig server_config = new ServerConfig();
				//  server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (pdl_key_filename != null) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the PDL delete product

				Map<String, String> extra_properties = null;

				long modifiedTime = 0L;

				Product product = PDLProductBuilderEventSequence.createDeletionProduct (
					pdl_code, props, extra_properties, isReviewed, modifiedTime);

				// Stop if unable to create product

				if (product == null) {
					System.out.println ("PDLProductBuilderEventSequence.createDeletionProduct returned null");
					return;
				}

				// Sign the product

				PDLSender.signProduct(product);

				// Send the product, true means it is text

				PDLSender.sendProduct(product, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #9
		// Command format:
		//  test9  event_id  disk_dirname  isReviewed  pdl_code  pdl_enable  [pdl_key_filename]
		// See test_make_for_mainshock for parameter description
		// Fetch information for an event, and display it.
		// Then build a product file list by scanning the given disk directory, and display it.
		// Then build an event-sequence-text product and send it to PDL using the specified code.
		// Similar to test #7 except it sends an event-sequence-text product to PDL.

		if (args[0].equalsIgnoreCase ("test9")) {

			// 5 or 6 additional arguments

			if (args.length != 6 && args.length != 7) {
				System.err.println ("PropertiesEventSequence : Invalid 'test9' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				String disk_dirname = args[2];
				boolean isReviewed = Boolean.parseBoolean (args[3]);
				String pdl_code = args[4];
				int pdl_enable = Integer.parseInt (args[5]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
				String pdl_key_filename = null;
				if (args.length >= 7) {
					pdl_key_filename = args[6];
				}

				// Say hello

				System.out.println ("Constructing event-sequence-text product for event, and sending to PDL");
				System.out.println ("event_id: " + event_id);
				System.out.println ("disk_dirname: " + disk_dirname);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("pdl_code: " + pdl_code);
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + ((pdl_key_filename == null) ? "<null>" : pdl_key_filename));
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Get event network and event network code

				JSONObject gj_event = accessor.get_last_geojson();

				String eventNetwork = GeoJsonUtils.getString (gj_event, "properties", "net");
				if (eventNetwork == null || eventNetwork.isEmpty()) {
					throw new RuntimeException ("Unable to get event network");
				}

				String eventCode = GeoJsonUtils.getString (gj_event, "properties", "code");
				if (eventCode == null || eventCode.isEmpty()) {
					throw new RuntimeException ("Unable to get event network code");
				}

				System.out.println ("eventNetwork: " + eventNetwork);
				System.out.println ("eventCode: " + eventCode);
				System.out.println ();

				// Build the file tree

				String pdl_dirname = null;

				Map<String, File> pdl_dir_tree = PDLProductFile.build_pdl_dir_tree (disk_dirname, pdl_dirname);

				// Build the list of pdl files

				List<PDLProductFile> pdl_file_list = PDLProductFile.build_file_list_from_tree (pdl_dir_tree);

				// Convert list to array

				PDLProductFile[] pdl_file_array = pdl_file_list.toArray (new PDLProductFile[0]);

				System.out.println ("PDL file list");

				for (PDLProductFile pdl_file : pdl_file_array) {
					System.out.println (pdl_file.toString());
				}

				System.out.println ();

				//  // Direct operation to PDL-Development
				//  
				//  ServerConfig server_config = new ServerConfig();
				//  server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (pdl_key_filename != null) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the PDL product

				Map<String, String> extra_properties = new LinkedHashMap<String, String>();
				extra_properties.put (PropertiesEventSequence.EVS_EXTRA_GENERATED_BY, VersionInfo.get_generator_name());

				String jsonText = null;
				long modifiedTime = 0L;

				Product product = PDLProductBuilderEventSequenceText.createProduct (
					pdl_code, eventNetwork, eventCode, extra_properties, isReviewed, jsonText, modifiedTime, pdl_file_array);

				// Stop if unable to create product

				if (product == null) {
					System.out.println ("PDLProductBuilderEventSequenceText.createProduct returned null");
					return;
				}

				// Sign the product

				PDLSender.signProduct(product);

				// Send the product, true means it is text

				PDLSender.sendProduct(product, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #10
		// Command format:
		//  test10  event_id  isReviewed  pdl_code  pdl_enable  [pdl_key_filename]
		// Fetch information for an event, for a delete product, and display it.
		// Then build an event-sequence-text delete product and send it to PDL using the specified code.
		// Similar to test #8 except it sends an event-sequence-text delete product to PDL.

		if (args[0].equalsIgnoreCase ("test10")) {

			// 4 or 5 additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("PropertiesEventSequence : Invalid 'test10' subcommand");
				return;
			}

			try {

				String event_id = args[1];
				boolean isReviewed = Boolean.parseBoolean (args[2]);
				String pdl_code = args[3];
				int pdl_enable = Integer.parseInt (args[4]);	// 0 = none, 1 = dev, 2 = prod, 3 = sim dev, 4 = sim prod, 5 = down dev, 6 = down prod
				String pdl_key_filename = null;
				if (args.length >= 6) {
					pdl_key_filename = args[5];
				}

				// Say hello

				System.out.println ("Constructing event-sequence-text product for event, for a delete product, and sending to PDL");
				System.out.println ("event_id: " + event_id);
				System.out.println ("isReviewed: " + isReviewed);
				System.out.println ("pdl_code: " + pdl_code);
				System.out.println ("pdl_enable: " + pdl_enable);
				System.out.println ("pdl_key_filename: " + ((pdl_key_filename == null) ? "<null>" : pdl_key_filename));
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor();

				// Get the rupture

				System.out.println ("Fetching event: " + event_id);
				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				System.out.println ();

				// Get event network and event network code

				JSONObject gj_event = accessor.get_last_geojson();

				String eventNetwork = GeoJsonUtils.getString (gj_event, "properties", "net");
				if (eventNetwork == null || eventNetwork.isEmpty()) {
					throw new RuntimeException ("Unable to get event network");
				}

				String eventCode = GeoJsonUtils.getString (gj_event, "properties", "code");
				if (eventCode == null || eventCode.isEmpty()) {
					throw new RuntimeException ("Unable to get event network code");
				}

				System.out.println ("eventNetwork: " + eventNetwork);
				System.out.println ("eventCode: " + eventCode);
				System.out.println ();

				//  // Direct operation to PDL-Development
				//  
				//  ServerConfig server_config = new ServerConfig();
				//  server_config.get_server_config_file().pdl_enable = ServerConfigFile.PDLOPT_DEV;

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				if (pdl_key_filename != null) {

					if (!( (new File (pdl_key_filename)).canRead() )) {
						System.out.println ("Unreadable pdl_key_filename = " + pdl_key_filename);
						return;
					}

					server_config.get_server_config_file().pdl_key_filename = pdl_key_filename;
				}

				// Make the PDL delete product

				Map<String, String> extra_properties = null;

				long modifiedTime = 0L;

				Product product = PDLProductBuilderEventSequenceText.createDeletionProduct (
					pdl_code, eventNetwork, eventCode, extra_properties, isReviewed, modifiedTime);

				// Stop if unable to create product

				if (product == null) {
					System.out.println ("PDLProductBuilderEventSequenceText.createDeletionProduct returned null");
					return;
				}

				// Sign the product

				PDLSender.signProduct(product);

				// Send the product, true means it is text

				PDLSender.sendProduct(product, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("PropertiesEventSequence : Unrecognized subcommand : " + args[0]);
		return;

	}

}
