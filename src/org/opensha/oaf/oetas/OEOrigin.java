package org.opensha.oaf.oetas;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.GUIExternalCatalog;
import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;

import static org.opensha.oaf.oetas.OEConstants.C_MILLIS_PER_DAY;

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

public class OEOrigin {

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

		result.append ("origin_time = "  + origin_time  + "\n");
		result.append ("origin_lat = "   + origin_lat   + "\n");
		result.append ("origin_lon = "   + origin_lon   + "\n");
		result.append ("origin_depth = " + origin_depth + "\n");

		return result.toString();
	}




	//----- Conversions -----




	// Convert from observed rupture to ETAS rupture.
	// Parameters:
	//  obs_rup = Observed rupture.
	//  etas_rup = Structure to receive ETAS rupture.
	// Note: The productivity is set to zero, and the parent is set to -1.

	public void convert_obs_to_etas (ObsEqkRupture obs_rup, OERupture etas_rup) {
		double t_day = ((double)(obs_rup.getOriginTime() - origin_time)) / C_MILLIS_PER_DAY;
		double rup_mag = obs_rup.getMag();
		double k_prod = 0.0;
		int rup_parent = -1;

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
	//  t_day_min = Minimum time to include, or -1.0e10 if none.
	//  t_day_max = Maximum time to include, or 1.0e10 if none.
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

				etas_cat.get_rup (i_gen, j_rup, etas_rup);

				// If filter condition is satisfied ...

				if (
					   (i_gen > 0 || f_include_seed)			// allowed generation
					&& (i_gen > 0 || j_rup != i_mainshock)		// not the mainshock
					&& (t_day_min < -0.9e10 || etas_rup.t_day >= t_day_min)	// at least min time
					&& (t_day_max > 0.9e10 || etas_rup.t_day <= t_day_max)	// at most max time
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
	//  t_day_min = Minimum time to include, or -1.0e10 if none.
	//  t_day_max = Maximum time to include, or 1.0e10 if none.
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
			etas_cat.get_rup (0, i_mainshock, etas_rup);
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




		// Unrecognized subcommand.

		System.err.println ("OEOrigin : Unrecognized subcommand : " + args[0]);
		return;

	}

}
