package org.opensha.oaf.oetas;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.gui.GUIExternalCatalog;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.catalog.AbsoluteTimeLocation;
import org.opensha.oaf.util.catalog.RelativeTimeLocation;
import org.opensha.oaf.util.catalog.AbsRelTimeLocConverter;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;

import org.opensha.oaf.oetas.fit.OEMagCompFn;

import static org.opensha.oaf.oetas.OEConstants.C_MILLIS_PER_DAY;
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS;
import static org.opensha.oaf.oetas.OEConstants.HUGE_TIME_DAYS_CHECK;

import static org.opensha.oaf.oetas.OERupture.RUPPAR_SEED;

import org.opensha.oaf.aafs.ForecastMainshock;
import org.opensha.oaf.aafs.ForecastResults;

import org.opensha.oaf.rj.CompactEqkRupList;

import static org.opensha.commons.geo.GeoTools.TO_DEG;
import static org.opensha.commons.geo.GeoTools.TO_RAD;


// Class to hold origin of the coordinate system of an Operational ETAS catalog.
// Author: Michael Barall 02/17/2020.
//
// An operational ETAS catalog uses:
// - Time in days, relative to an unspecified origin.
// - Location in (x,y) coordinates in kilometers, relative to an unspecified origin.
//
// This class is used to specify the origin, allowing conversion between catalog
// coordinates and real-world coordinates.
//
// This class also is used for general conversions between absolute and relative
// times and locations.

public class OEOrigin implements AbsRelTimeLocConverter {

	//----- Origin -----

	// Origin time, in milliseconds since the epoch.

	public long origin_time;

	// Origin latitude, in decimal degrees, from -90.0 to +90.0.

	public double origin_lat;

	// Origin longitude, in decimal degrees, from -180.0 to +180.0.

	public double origin_lon;

	// Origin depth, in kilometers, from 0.0 to 700.0.
	// Note: ETAS catalogs do not contain depth; this is the assumed depth for all earthquakes.

	public double origin_depth;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		origin_time  = 0L;
		origin_lat   = 0.0;
		origin_lon   = 0.0;
		origin_depth = 0.0;
		return;
	}




	// Default constructor.

	public OEOrigin () {
		clear();
	}




	// Set all values.

	public OEOrigin set (
		long origin_time,
		double origin_lat,
		double origin_lon,
		double origin_depth
	) {
		this.origin_time  = origin_time;
		this.origin_lat   = origin_lat;
		this.origin_lon   = origin_lon;
		this.origin_depth = origin_depth;
		return this;
	}




	// Constructor that sets all values.

	public OEOrigin (
		long origin_time,
		double origin_lat,
		double origin_lon,
		double origin_depth
	) {
		this.origin_time  = origin_time;
		this.origin_lat   = origin_lat;
		this.origin_lon   = origin_lon;
		this.origin_depth = origin_depth;
	}




	// Copy all values from the other object.

	public OEOrigin copy_from (OEOrigin other) {
		this.origin_time  = other.origin_time;
		this.origin_lat   = other.origin_lat;
		this.origin_lon   = other.origin_lon;
		this.origin_depth = other.origin_depth;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEOrigin:" + "\n");

		result.append ("origin_time = "  + origin_time + " (" + SimpleUtils.time_to_parseable_string(origin_time) + ")"  + "\n");
		result.append ("origin_lat = "   + origin_lat   + "\n");
		result.append ("origin_lon = "   + origin_lon   + "\n");
		result.append ("origin_depth = " + origin_depth + "\n");

		return result.toString();
	}




	// Set all values to place the origin at the given mainshock.

	public OEOrigin set_from_mainshock (ForecastMainshock fcmain) {
	
		// Check that mainshock is available

		if (!( fcmain.mainshock_avail )) {
			throw new IllegalArgumentException ("OEOrigin.set_from_mainshock - No mainshock available");
		}

		// Set values

		this.origin_time  = fcmain.mainshock_time;
		this.origin_lat   = fcmain.mainshock_lat;
		this.origin_lon   = fcmain.mainshock_lon;
		this.origin_depth = fcmain.mainshock_depth;
		return this;
	}




	// Set all values to place the origin at the given mainshock, horizontally only.

	public OEOrigin set_from_mainshock_horz (ForecastMainshock fcmain) {
	
		// Check that mainshock is available

		if (!( fcmain.mainshock_avail )) {
			throw new IllegalArgumentException ("OEOrigin.set_from_mainshock - No mainshock available");
		}

		// Set values

		this.origin_time  = fcmain.mainshock_time;
		this.origin_lat   = fcmain.mainshock_lat;
		this.origin_lon   = fcmain.mainshock_lon;
		this.origin_depth = 0.0;
		return this;
	}




	// Set all values to place the origin at the given rupture.

	public OEOrigin set_from_rupture (ObsEqkRupture rup) {

		// Set values

		origin_time  = rup.getOriginTime();
		Location hypo = rup.getHypocenterLocation();
		origin_lat   = hypo.getLatitude();
		origin_lon   = hypo.getLongitude();
		origin_depth = hypo.getDepth();

		if (origin_lon > 180.0) {
			origin_lon -= 360.0;		// force longitude into range -180 to +180.
		}

		return this;
	}




	// Set all values to place the origin at the given rupture, horizontally only.

	public OEOrigin set_from_rupture_horz (ObsEqkRupture rup) {

		// Set values

		origin_time  = rup.getOriginTime();
		Location hypo = rup.getHypocenterLocation();
		origin_lat   = hypo.getLatitude();
		origin_lon   = hypo.getLongitude();
		origin_depth = 0.0;

		if (origin_lon > 180.0) {
			origin_lon -= 360.0;		// force longitude into range -180 to +180.
		}

		return this;
	}




	//----- Conversions -----




	// Convert from observed rupture to ETAS rupture.
	// Parameters:
	//  obs_rup = Observed rupture.
	//  etas_rup = Structure to receive ETAS rupture.
	// Note: The productivity is set to zero, and the parent is set to RUPPAR_SEED == -1.

	public void convert_obs_to_etas (ObsEqkRupture obs_rup, OERupture etas_rup) {
		double t_day = ((double)(obs_rup.getOriginTime() - origin_time)) / C_MILLIS_PER_DAY;
		double rup_mag = obs_rup.getMag();
		double k_prod = 0.0;
		int rup_parent = RUPPAR_SEED;

		double x_km;
		double y_km;

		Location hypoLoc = obs_rup.getHypocenterLocation();
		double obs_lat = hypoLoc.getLatitude();
		double obs_lon = hypoLoc.getLongitude();
		double r_km = SphLatLon.horzDistance (origin_lat, origin_lon, obs_lat, obs_lon);
		if (r_km <= 1.0e-5) {
			x_km = 0.0;
			y_km = 0.0;
		} else {
			double az_rad = SphLatLon.azimuth_rad (origin_lat, origin_lon, obs_lat, obs_lon);
			x_km = r_km * Math.sin (az_rad);
			y_km = r_km * Math.cos (az_rad);
		}

		etas_rup.set (t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
		return;
	}




	// Convert from ETAS rupture to observed rupture.
	// Parameters:
	//  etas_rup = Structure containing ETAS rupture.
	//  event_id = Event ID to insert into the observed rupture, or null.
	// Returns the observed rupture.
	// Note: If event_id is null, an event ID is constructed from the time and magnitude.

	public ObsEqkRupture convert_etas_to_obs (OERupture etas_rup, String event_id) {

		double etas_millis = etas_rup.t_day * C_MILLIS_PER_DAY;
		long obs_millis = Math.round(etas_millis) + origin_time;
		double obs_mag = etas_rup.rup_mag;

		double obs_lat;
		double obs_lon;

		double dist_km = Math.sqrt (etas_rup.x_km * etas_rup.x_km + etas_rup.y_km * etas_rup.y_km);
		if (dist_km <= 1.0e-5) {
			obs_lat = origin_lat;
			obs_lon = origin_lon;
		} else {
			double az_rad = Math.atan2 (etas_rup.x_km, etas_rup.y_km);
			SphLatLon p = SphLatLon.gc_travel_km (origin_lat * TO_RAD, origin_lon * TO_RAD, az_rad, dist_km);
			obs_lat = p.get_lat();
			obs_lon = p.get_lon();
		}

		double obs_depth = origin_depth;
		Location hypoLoc = new Location (obs_lat, obs_lon, obs_depth);

		String obs_event_id;
		if (event_id != null) {
			obs_event_id = event_id;
		} else {
			obs_event_id = String.format (Locale.US, "%d_M%.3g", obs_millis, obs_mag);
		}
		
		return new ObsEqkRupture (obs_event_id, obs_millis, hypoLoc, obs_mag);
	}




	// Convert an ETAS catalog to a list of observed ruptures.
	// Parameters:
	//  etas_cat = Catalog of ETAS ruptures.
	//  f_include_seed = True to include the seed generation, false if not.
	//  i_mainshock = Index of the mainshock within the seed generation, or -1 if none.
	//  t_day_min = Minimum time to include, or -HUGE_TIME_DAYS if none.
	//  t_day_max = Maximum time to include, or HUGE_TIME_DAYS if none.
	//  mag_comp = Function giving minimum magnitude to include, or null if none.
	// Returns the list of observed ruptures.
	// Note: The returned list is sorted by time.

	public ObsEqkRupList convert_etas_cat_to_obs (
		OECatalogView etas_cat,
		boolean f_include_seed,
		int i_mainshock,
		double t_day_min,
		double t_day_max,
		OEMagCompFn mag_comp
	) {

		// Make the list

		ObsEqkRupList obs_rups = new ObsEqkRupList();

		// Rupture structure

		OERupture etas_rup = new OERupture();

		// Loop over generations ...

		int gen_count = etas_cat.get_gen_count();
		for (int i_gen = 0; i_gen < gen_count; ++i_gen) {

			// Loop over ruptures within the generation ...

			int gen_size = etas_cat.get_gen_size (i_gen);
			for (int j_rup = 0; j_rup < gen_size; ++j_rup) {

				// Get the rupture

				etas_cat.get_rup_full (i_gen, j_rup, etas_rup);

				// If filter condition is satisfied ...

				if (
					   (i_gen > 0 || f_include_seed)			// allowed generation
					&& (i_gen > 0 || j_rup != i_mainshock)		// not the mainshock
					&& (t_day_min <= -HUGE_TIME_DAYS_CHECK || etas_rup.t_day >= t_day_min)	// at least min time
					&& (t_day_max >= HUGE_TIME_DAYS_CHECK || etas_rup.t_day <= t_day_max)	// at most max time
					&& (mag_comp == null || etas_rup.rup_mag >= mag_comp.get_mag_completeness (etas_rup.t_day))	// at least min mag
				) {
					// Convert and save the rupture

					obs_rups.add (convert_etas_to_obs (etas_rup, null));
				}
			}
		}

		// Sort list by time

		GUIExternalCatalog.sort_aftershocks (obs_rups);

		return obs_rups;
	}




	// Write an ETAS catalog to a file, in GUI external format.
	// Parameters:
	//  etas_cat = Catalog of ETAS ruptures.
	//  f_include_seed = True to include the seed generation, false if not.
	//  i_mainshock = Index of the mainshock within the seed generation, or -1 if none.
	//  t_day_min = Minimum time to include, or -HUGE_TIME_DAYS if none.
	//  t_day_max = Maximum time to include, or HUGE_TIME_DAYS if none.
	//  mag_comp = Function giving minimum magnitude to include, or null if none.
	//  catalog_event_id = Event ID to place in header line, or null if no
	//    header line should be written.
	//  catalog_max_days = Catalog duration, measured in days since the mainshock,
	//    which should be written to the hedaer line; ignored if no header line is written.
	//  filename = Name of file to write.
	// Returns the list of observed ruptures.
	// Note: The aftershocks are sorted by time.
	// Note: Throws an exception if file write error.

	public void write_etas_cat_to_gui_ext (
		OECatalogView etas_cat,
		boolean f_include_seed,
		int i_mainshock,
		double t_day_min,
		double t_day_max,
		OEMagCompFn mag_comp,
		String catalog_event_id,
		double catalog_max_days,
		String filename
	) {

		// Get the list of aftershocks

		ObsEqkRupList aftershocks = convert_etas_cat_to_obs (
			etas_cat,
			f_include_seed,
			i_mainshock,
			t_day_min,
			t_day_max,
			mag_comp
		);

		// Get the mainshock, or null if none

		ObsEqkRupture mainshock = null;

		if (i_mainshock >= 0) {
			OERupture etas_rup = new OERupture();
			etas_cat.get_rup_full (0, i_mainshock, etas_rup);
			mainshock = convert_etas_to_obs (etas_rup, catalog_event_id);
		};

		// Set up for an external catalog

		GUIExternalCatalog ext_cat = new GUIExternalCatalog();

		ext_cat.setup_catalog (
			aftershocks,
			mainshock,
			catalog_event_id,
			catalog_max_days
		);

		// Write the file

		ext_cat.write_to_file (filename);

		return;
	}




	// Convert from observed rupture to ETAS rupture.
	// Parameters:
	//  obs_time = Observed rupture time, in milliseconds since the epoch.
	//  rup_mag = Observed rupture magnitude.
	//  obs_lat = Observed rupture latitude, in degrees.
	//  obs_lon = Observed rupture longitude, in degrees.
	//  etas_rup = Structure to receive ETAS rupture.
	// Note: The productivity is set to zero, and the parent is set to RUPPAR_SEED == -1.

	public void convert_obs_to_etas (long obs_time, double rup_mag, double obs_lat, double obs_lon, OERupture etas_rup) {
		double t_day = ((double)(obs_time - origin_time)) / C_MILLIS_PER_DAY;
		double k_prod = 0.0;
		int rup_parent = RUPPAR_SEED;

		double x_km;
		double y_km;

		double r_km = SphLatLon.horzDistance (origin_lat, origin_lon, obs_lat, obs_lon);
		if (r_km <= 1.0e-5) {
			x_km = 0.0;
			y_km = 0.0;
		} else {
			double az_rad = SphLatLon.azimuth_rad (origin_lat, origin_lon, obs_lat, obs_lon);
			x_km = r_km * Math.sin (az_rad);
			y_km = r_km * Math.cos (az_rad);
		}

		etas_rup.set (t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
		return;
	}




	// Convert from compact rupture to ETAS rupture.
	// Parameters:
	//  compact_cat = Compact rupture catalog.
	//  n = Index number of rupture within the catalog.
	//  etas_rup = Structure to receive ETAS rupture.
	// Note: The productivity is set to zero, and the parent is set to -1.

	public void convert_compact_to_etas (CompactEqkRupList compact_cat, int n, OERupture etas_rup) {
		convert_obs_to_etas (
			compact_cat.get_time(n),
			compact_cat.get_mag(n),
			compact_cat.get_lat(n),
			compact_cat.get_unwrapped_lon(n),
			etas_rup
		);
		return;
	}




	// Convert from mainshock rupture to ETAS rupture.
	// Parameters:
	//  fcmain = Mainshock.
	//  etas_rup = Structure to receive ETAS rupture.
	// Note: The productivity is set to zero, and the parent is set to -1.

	public void convert_mainshock_to_etas (ForecastMainshock fcmain, OERupture etas_rup) {

		if (!( fcmain.mainshock_avail )) {
			throw new IllegalArgumentException ("OEOrigin.convert_mainshock_to_etas - No mainshock available");
		}

		convert_obs_to_etas (
			fcmain.mainshock_time,
			fcmain.mainshock_mag,
			fcmain.mainshock_lat,
			fcmain.mainshock_lon,
			etas_rup
		);
		return;
	}



	// Convert a compact catalog to a list of ETAS ruptures.
	// Parameters:
	//  rup_list = Receives the list of ruptures.
	//  compact_cat = Compact rupture catalog.
	//  fcmain = Mainshock, can be null or not-available, can be omitted.
	// Adds a list of ETAS ruptures to rup_list.
	// If fcmain is non-null and available, then the mainshock is added to the list.
	// The ordering of the returned list is not specified.

	public void convert_compact_cat_to_etas_list (Collection<OERupture> rup_list, CompactEqkRupList compact_cat) {
		convert_compact_cat_to_etas_list (rup_list, compact_cat, null);
		return;
	}

	public void convert_compact_cat_to_etas_list (Collection<OERupture> rup_list, CompactEqkRupList compact_cat, ForecastMainshock fcmain) {

		// Loop over catalog, adding each earthquake to the list

		int eqk_count = compact_cat.get_eqk_count();
		for (int n = 0; n < eqk_count; ++n) {
			OERupture etas_rup = new OERupture();
			convert_compact_to_etas (compact_cat, n, etas_rup);
			rup_list.add (etas_rup);
		}

		// If the mainshock is supplied, add it to the list

		if (fcmain != null && fcmain.mainshock_avail) {
			OERupture etas_rup = new OERupture();
			convert_mainshock_to_etas (fcmain, etas_rup);
			rup_list.add (etas_rup);
		}

		return;
	}




	// Convert from absolute to relative time and location.
	// Parameters:
	//	abs_tloc = Absolute time and location.
	//	rel_tloc = Receives relative time and location.

	@Override
	public void convert_abs_to_rel (AbsoluteTimeLocation abs_tloc, RelativeTimeLocation rel_tloc) {

		rel_tloc.rel_t_day = ((double)(abs_tloc.abs_time - origin_time)) / C_MILLIS_PER_DAY;

		double r_km = SphLatLon.horzDistance (origin_lat, origin_lon, abs_tloc.abs_lat, abs_tloc.abs_lon);
		if (r_km <= 1.0e-5) {
			rel_tloc.rel_x_km = 0.0;
			rel_tloc.rel_y_km = 0.0;
		} else {
			double az_rad = SphLatLon.azimuth_rad (origin_lat, origin_lon, abs_tloc.abs_lat, abs_tloc.abs_lon);
			rel_tloc.rel_x_km = r_km * Math.sin (az_rad);
			rel_tloc.rel_y_km = r_km * Math.cos (az_rad);
		}

		rel_tloc.rel_d_km = abs_tloc.abs_depth - origin_depth;

		return;
	}




	// Convert from relative to absolute time and location.
	// Parameters:
	//	rel_tloc = Relative time and location.
	//	abs_tloc = Receives absolute time and location.

	@Override
	public void convert_rel_to_abs (RelativeTimeLocation rel_tloc, AbsoluteTimeLocation abs_tloc) {

		double rel_millis = rel_tloc.rel_t_day * C_MILLIS_PER_DAY;
		abs_tloc.abs_time = Math.round(rel_millis) + origin_time;

		double dist_km = Math.sqrt (rel_tloc.rel_x_km * rel_tloc.rel_x_km + rel_tloc.rel_y_km * rel_tloc.rel_y_km);
		if (dist_km <= 1.0e-5) {
			abs_tloc.abs_lat = origin_lat;
			abs_tloc.abs_lon = origin_lon;
		} else {
			double az_rad = Math.atan2 (rel_tloc.rel_x_km, rel_tloc.rel_y_km);
			SphLatLon p = SphLatLon.gc_travel_km (origin_lat * TO_RAD, origin_lon * TO_RAD, az_rad, dist_km);
			abs_tloc.abs_lat = p.get_lat();
			abs_tloc.abs_lon = p.get_lon();
		}

		abs_tloc.abs_depth = rel_tloc.rel_d_km + origin_depth;

		return;
	}




	// Convert time from absolute to relative.
	// Parameters:
	//  abs_time = Absolute time, in milliseconds since the epoch.
	// Returns relative time, in days since the origin.

	@Override
	public double convert_time_abs_to_rel (long abs_time) {

		double rel_t_day = ((double)(abs_time - origin_time)) / C_MILLIS_PER_DAY;

		return rel_t_day;
	}




	// Convert time from relative to absolute.
	// Parameters:
	//  rel_t_day = Relative time, in days since the origin.
	// Returns absolute time, in milliseconds since the epoch.

	@Override
	public long convert_time_rel_to_abs (double rel_t_day) {

		double rel_millis = rel_t_day * C_MILLIS_PER_DAY;
		long abs_time = Math.round(rel_millis) + origin_time;

		return abs_time;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 84001;

	private static final String M_VERSION_NAME = "OEOrigin";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalLong   ("origin_time"  , origin_time );
			writer.marshalDouble ("origin_lat"   , origin_lat  );
			writer.marshalDouble ("origin_lon"   , origin_lon  );
			writer.marshalDouble ("origin_depth" , origin_depth);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			origin_time  = reader.unmarshalLong   ("origin_time" );
			origin_lat   = reader.unmarshalDouble ("origin_lat"  );
			origin_lon   = reader.unmarshalDouble ("origin_lon"  );
			origin_depth = reader.unmarshalDouble ("origin_depth");

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

	public OEOrigin unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEOrigin catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEOrigin static_unmarshal (MarshalReader reader, String name) {
		OEOrigin catalog = new OEOrigin();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two origin structures are identical.
	// Note: This is primarily for testing.

	public boolean check_origin_equal (OEOrigin other) {
		if (
			   this.origin_time  == other.origin_time
			&& this.origin_lat   == other.origin_lat
			&& this.origin_lon   == other.origin_lon
			&& this.origin_depth == other.origin_depth
		) {
			return true;
		}
		return false;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEOrigin : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  n  p  c  b  alpha  gen_size_target  gen_count_max  mag_main  tbegin
		//         mag_min_sim  mag_max_sim  mag_min_lo  mag_min_hi
		//         org_time  org_lat  org_lon  org_depth
		//         mag_cat  cat_event_id  cat_max_days  filename
		// Build a catalog with the given parameters.
		// Convert to observed ruptures with the given origin parameters and time/magnitude window.
		// Then write it to a GUI external file.
		// The "n" is the branch ratio; "a" is computed from it.
		// The "org_time" is in ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 21 additional arguments

			if (args.length != 22) {
				System.err.println ("OEOrigin : Invalid 'test1' subcommand");
				return;
			}

			try {

				double n = Double.parseDouble (args[1]);
				double p = Double.parseDouble (args[2]);
				double c = Double.parseDouble (args[3]);
				double b = Double.parseDouble (args[4]);
				double alpha = Double.parseDouble (args[5]);
				int gen_size_target = Integer.parseInt (args[6]);
				int gen_count_max = Integer.parseInt (args[7]);
				double mag_main = Double.parseDouble (args[8]);
				double the_tbegin = Double.parseDouble (args[9]);
				double the_mag_min_sim = Double.parseDouble (args[10]);
				double the_mag_max_sim = Double.parseDouble (args[11]);
				double the_mag_min_lo = Double.parseDouble (args[12]);
				double the_mag_min_hi = Double.parseDouble (args[13]);
				long org_time = SimpleUtils.string_to_time (args[14]);
				double org_lat = Double.parseDouble (args[15]);
				double org_lon = Double.parseDouble (args[16]);
				double org_depth = Double.parseDouble (args[17]);
				double mag_cat = Double.parseDouble (args[18]);
				String cat_event_id = args[19];
				double cat_max_days = Double.parseDouble (args[20]);
				String filename = args[21];

				// Say hello

				System.out.println ("Generating catalog with given parameters");
				System.out.println ("n = " + n);
				System.out.println ("p = " + p);
				System.out.println ("c = " + c);
				System.out.println ("b = " + b);
				System.out.println ("alpha = " + alpha);
				System.out.println ("gen_size_target = " + gen_size_target);
				System.out.println ("gen_count_max = " + gen_count_max);
				System.out.println ("mag_main = " + mag_main);
				System.out.println ("the_tbegin = " + the_tbegin);
				System.out.println ("the_mag_min_sim = " + the_mag_min_sim);
				System.out.println ("the_mag_max_sim = " + the_mag_max_sim);
				System.out.println ("the_mag_min_lo = " + the_mag_min_lo);
				System.out.println ("the_mag_min_hi = " + the_mag_min_hi);
				System.out.println ("org_time = " + SimpleUtils.time_raw_and_string (org_time));
				System.out.println ("org_lat = " + org_lat);
				System.out.println ("org_lon = " + org_lon);
				System.out.println ("org_depth = " + org_depth);
				System.out.println ("mag_cat = " + mag_cat);
				System.out.println ("cat_event_id = " + cat_event_id);
				System.out.println ("cat_max_days = " + cat_max_days);
				System.out.println ("filename = " + filename);

				// Set up catalog parameters

				double a = 0.0;			// for the moment
				OECatalogParams test_cat_params = (new OECatalogParams()).set_to_typical (
					a,
					p,
					c,
					b,
					alpha,
					gen_size_target,
					gen_count_max
				);

				// Compute productivity "a" for the given branch ratio

				System.out.println ();
				System.out.println ("Branch ratio calculation");

				a = OEStatsCalc.calc_inv_branch_ratio (n, test_cat_params);
				test_cat_params.a = a;
				System.out.println ("a = " + a);

				// Recompute branch ratio to check it agrees with input

				double n_2 = OEStatsCalc.calc_branch_ratio (test_cat_params);
				System.out.println ("n_2 = " + n_2);

				// Adjust forecast time

				test_cat_params.tbegin = the_tbegin;
				test_cat_params.tend = the_tbegin + 365.0;

				// Set magnitude tanges

				test_cat_params.mag_min_sim = the_mag_min_sim;
				test_cat_params.mag_max_sim = the_mag_max_sim;
				test_cat_params.mag_min_lo = the_mag_min_lo;
				test_cat_params.mag_min_hi = the_mag_min_hi;

				// Generate the catalog

				System.out.println ();
				System.out.println ("Generating catalog");

				OECatalogStorage etas_cat = OECatalogGenerator.gen_simple_catalog (test_cat_params, mag_main);

				// Display catalog summary

				System.out.println ();
				System.out.println (etas_cat.summary_and_gen_list_string());

				// Make the origin

				OEOrigin origin = new OEOrigin();
				origin.set (
					org_time,
					org_lat,
					org_lon,
					org_depth
				);

				// Write the catalog file

				System.out.println ();
				System.out.println ("Writing catalog file");

				OEMagCompFn mag_comp = OEMagCompFn.makeConstant (mag_cat);

				origin.write_etas_cat_to_gui_ext (
					etas_cat,						// etas_cat
					false,							// f_include_seed
					0,								// i_mainshock
					0.0,							// t_day_min
					cat_max_days,					// t_day_max
					mag_comp,						// mag_comp
					cat_event_id,					// catalog_event_id
					cat_max_days,					// catalog_max_days
					filename						// filename
				);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  org_time  org_lat  org_lon  org_depth
		//         abs_time  abs_lat  abs_lon  abs_depth
		// Create an absolute time and location object, and display it.
		// Convert to relative time and location, and display it.
		// Convert back to absolute time and location, and display it.
		// Note: org_time and abs_time can be given as a number of milliseconds since the epoch,
		// or in ISO-8601 format (like 2011-12-03T10:15:30Z), or "now".

		if (args[0].equalsIgnoreCase ("test2")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("OEOrigin : Invalid 'test2' subcommand");
				return;
			}

			try 
			{
				long org_time = SimpleUtils.string_or_number_or_now_to_time (args[1]);
				double org_lat = Double.parseDouble (args[2]);
				double org_lon = Double.parseDouble (args[3]);
				double org_depth = Double.parseDouble (args[4]);

				long abs_time = SimpleUtils.string_or_number_or_now_to_time (args[5]);
				double abs_lat = Double.parseDouble (args[6]);
				double abs_lon = Double.parseDouble (args[7]);
				double abs_depth = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Converting absolute to relative time and location");
				System.out.println ("org_time = " + SimpleUtils.time_raw_and_string (org_time));
				System.out.println ("org_lat = " + org_lat);
				System.out.println ("org_lon = " + org_lon);
				System.out.println ("org_depth = " + org_depth);
				System.out.println ("abs_time = " + SimpleUtils.time_raw_and_string (abs_time));
				System.out.println ("abs_lat = " + abs_lat);
				System.out.println ("abs_lon = " + abs_lon);
				System.out.println ("abs_depth = " + abs_depth);

				// Make the origin

				OEOrigin origin = new OEOrigin();
				origin.set (
					org_time,
					org_lat,
					org_lon,
					org_depth
				);

				System.out.println();
				System.out.println (origin.toString());

				// Make the absolute time and location

				AbsoluteTimeLocation abs_tloc = new AbsoluteTimeLocation();
				abs_tloc.set (
					abs_time,
					abs_lat,
					abs_lon,
					abs_depth
				);

				System.out.println();
				System.out.println (abs_tloc.toString());
				System.out.println();
				System.out.println (abs_tloc.value_string());
				System.out.println();
				System.out.println (abs_tloc.one_line_string());

				// Convert to relative time and location

				RelativeTimeLocation rel_tloc = new RelativeTimeLocation();
				origin.convert_abs_to_rel (abs_tloc, rel_tloc);

				System.out.println();
				System.out.println (rel_tloc.toString());
				System.out.println();
				System.out.println (rel_tloc.value_string());
				System.out.println();
				System.out.println (rel_tloc.one_line_string());

				// Convert to absolute time and location

				AbsoluteTimeLocation abs_tloc2 = new AbsoluteTimeLocation();
				origin.convert_rel_to_abs (rel_tloc, abs_tloc2);

				System.out.println();
				System.out.println (abs_tloc2.toString());
				System.out.println();
				System.out.println (abs_tloc2.value_string());
				System.out.println();
				System.out.println (abs_tloc2.one_line_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  org_time  org_lat  org_lon  org_depth
		//         rel_t_day  rel_x_km  rel_y_km  rel_d_km
		// Create an absolute time and location object, and display it.
		// Convert to relative time and location, and display it.
		// Convert back to absolute time and location, and display it.
		// Note: org_time and rel_t_day can be given as a number of milliseconds since the epoch,
		// or in ISO-8601 format (like 2011-12-03T10:15:30Z), or "now".

		if (args[0].equalsIgnoreCase ("test3")) {

			// 8 additional arguments

			if (args.length != 9) {
				System.err.println ("OEOrigin : Invalid 'test3' subcommand");
				return;
			}

			try 
			{
				long org_time = SimpleUtils.string_or_number_or_now_to_time (args[1]);
				double org_lat = Double.parseDouble (args[2]);
				double org_lon = Double.parseDouble (args[3]);
				double org_depth = Double.parseDouble (args[4]);

				double rel_t_day = Double.parseDouble (args[5]);
				double rel_x_km = Double.parseDouble (args[6]);
				double rel_y_km = Double.parseDouble (args[7]);
				double rel_d_km = Double.parseDouble (args[8]);

				// Say hello

				System.out.println ("Converting relative to absolute time and location");
				System.out.println ("org_time = " + SimpleUtils.time_raw_and_string (org_time));
				System.out.println ("org_lat = " + org_lat);
				System.out.println ("org_lon = " + org_lon);
				System.out.println ("org_depth = " + org_depth);
				System.out.println ("rel_t_day = " + rel_t_day);
				System.out.println ("rel_x_km = " + rel_x_km);
				System.out.println ("rel_y_km = " + rel_y_km);
				System.out.println ("rel_d_km = " + rel_d_km);

				// Make the origin

				OEOrigin origin = new OEOrigin();
				origin.set (
					org_time,
					org_lat,
					org_lon,
					org_depth
				);

				System.out.println();
				System.out.println (origin.toString());

				// Make the relative time and location

				RelativeTimeLocation rel_tloc = new RelativeTimeLocation();
				rel_tloc.set (
					rel_t_day,
					rel_x_km,
					rel_y_km,
					rel_d_km
				);

				System.out.println();
				System.out.println (rel_tloc.toString());
				System.out.println();
				System.out.println (rel_tloc.value_string());
				System.out.println();
				System.out.println (rel_tloc.one_line_string());

				// Convert to absolute time and location

				AbsoluteTimeLocation abs_tloc = new AbsoluteTimeLocation();
				origin.convert_rel_to_abs (rel_tloc, abs_tloc);

				System.out.println();
				System.out.println (abs_tloc.toString());
				System.out.println();
				System.out.println (abs_tloc.value_string());
				System.out.println();
				System.out.println (abs_tloc.one_line_string());

				// Convert to relative time and location

				RelativeTimeLocation rel_tloc2 = new RelativeTimeLocation();
				origin.convert_abs_to_rel (abs_tloc, rel_tloc2);

				System.out.println();
				System.out.println (rel_tloc2.toString());
				System.out.println();
				System.out.println (rel_tloc2.value_string());
				System.out.println();
				System.out.println (rel_tloc2.one_line_string());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OEOrigin : Unrecognized subcommand : " + args[0]);
		return;

	}

}
