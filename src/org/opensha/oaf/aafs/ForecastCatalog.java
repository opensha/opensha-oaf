package org.opensha.oaf.aafs;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Earthquake catalog a forecast.
 * Author: Michael Barall 08/24/2018.
 *
 * Holds an earthquake catalog in a compact form that is easily marshaled.
 */
public class ForecastCatalog {

	//----- Catalog information -----

	// eqk_count - Number of earthquakes.
	// The special value -1 indicates a null catalog.

	public int eqk_count;

	// lat_lon_depth_list - Compressed latitude, longitude, and depth for each earthquake.
	// An entry may be zero if there is no location data for the particular earthquake.
	// The length must equal max(eqk_count,1).

	public long[] lat_lon_depth_list;

	// mag_time_list - Compressed magnitude and time for each earthquake.
	// An entry may be zero if there is no time and magnitude data for the particular earthquake.
	// The length must equal max(eqk_count,1).

	public long[] mag_time_list;




	//----- Construction -----




	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "ForecastCatalog\n"
			+ "\teqk_count: " + eqk_count + "\n"
			+ "\tlat_lon_depth_list: " + ((lat_lon_depth_list == null) ? ("null") : ("len=" + lat_lon_depth_list.length)) + "\n"
			+ "\tmag_time_list: " + ((mag_time_list == null) ? ("null") : ("len=" + mag_time_list.length)) + "\n";
		return str;
	}




	// get_rupture_list - Get the earthquake rupture list.

	public CompactEqkRupList get_rupture_list () {

		// For null list, return null

		if (eqk_count < 0) {
			return null;
		}

		// For empty list, pass in zero-size arrays

		if (eqk_count == 0) {
			return new CompactEqkRupList (eqk_count, new long[0], new long[0]);
		}

		// For non-empty list, pass our arrays

		return new CompactEqkRupList (eqk_count, lat_lon_depth_list, mag_time_list);
	}




	// set_rupture_list - Set the earthquake rupture list.

	public ForecastCatalog set_rupture_list (CompactEqkRupList rupture_list) {

		// For null list, use count = -1

		if (rupture_list == null) {
			eqk_count = -1;
			lat_lon_depth_list = new long[1];
			lat_lon_depth_list[0] = 0L;
			mag_time_list = new long[1];
			mag_time_list[0] = 0L;
			return this;
		}

		// Get earthquake count

		eqk_count = rupture_list.get_eqk_count();

		// For empty list, use one-element lists

		if (eqk_count == 0) {
			lat_lon_depth_list = new long[1];
			lat_lon_depth_list[0] = 0L;
			mag_time_list = new long[1];
			mag_time_list[0] = 0L;
			return this;
		}

		// For non-empty list, pull the existing arrays, and re-size them if needed

		lat_lon_depth_list = rupture_list.get_lat_lon_depth_list();
		mag_time_list = rupture_list.get_mag_time_list();

		if (lat_lon_depth_list.length != eqk_count) {
			lat_lon_depth_list = Arrays.copyOf (lat_lon_depth_list, eqk_count);
		}
		if (mag_time_list.length != eqk_count) {
			mag_time_list = Arrays.copyOf (mag_time_list, eqk_count);
		}
		return this;
	}




	// Constructor makes a null list.

	public ForecastCatalog () {
		eqk_count = -1;
		lat_lon_depth_list = new long[1];
		lat_lon_depth_list[0] = 0L;
		mag_time_list = new long[1];
		mag_time_list[0] = 0L;

		set_standard_format();
	}




	//----- Friendly format -----

	// Flag is true to use friendly format when marshaling.
	// Note: This and other variables associated with friendly format
	// are not marshaled in the standard format.

	protected boolean f_friendly_format = false;

	// Mainshock time, in milliseconds since the epoch.

	protected long mainshock_time = 0L;

	// Mainshock magnitude.

	protected double mainshock_mag = 0.0;

	// Mainshock latitude, in degrees, from -90 to +90.

	protected double mainshock_lat = 0.0;

	// Mainshock longitude, in degrees, from -180 to +180.

	protected double mainshock_lon = 0.0;

	// Mainshock depth, in kilometers, positive underground.

	protected double mainshock_depth = 0.0;




	// Set variables for standard format.
	// Note: This is the default.

	public void set_standard_format () {
		f_friendly_format = false;

		mainshock_time = 0L;
		mainshock_mag = 0.0;
		mainshock_lat = 0.0;
		mainshock_lon = 0.0;
		mainshock_depth = 0.0;

		return;
	}




	// Set variables for friendly format.

	public void set_friendly_format (ForecastMainshock fcmain) {
		f_friendly_format = true;

		if (fcmain != null && fcmain.mainshock_avail) {
			mainshock_time = fcmain.mainshock_time;
			mainshock_mag = fcmain.mainshock_mag;
			mainshock_lat = fcmain.mainshock_lat;
			mainshock_lon = fcmain.mainshock_lon;
			mainshock_depth = fcmain.mainshock_depth;
		} else {
			mainshock_time = 0L;
			mainshock_mag = 0.0;
			mainshock_lat = 0.0;
			mainshock_lon = 0.0;
			mainshock_depth = 0.0;
		}

		return;
	}




	//----- Marshaling -----




	// Marshal mainshock, in friendly format.

	protected void marshal_friendly_mainshock (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		String mainshock_utc = "";
		if (mainshock_time != 0L) {
			mainshock_utc = SimpleUtils.time_to_parseable_string (mainshock_time);
		}

		writer.marshalString      ("utc"  , mainshock_utc  );
		writer.marshalLong        ("time" , mainshock_time );
		writer.marshalDouble      ("mag"  , mainshock_mag  );
		writer.marshalDouble      ("lat"  , mainshock_lat  );
		writer.marshalDouble      ("lon"  , mainshock_lon  );
		writer.marshalDouble      ("depth", mainshock_depth);

		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal mainshock, in friendly format.

	protected void unmarshal_friendly_mainshock (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		String mainshock_utc;

		mainshock_utc      = reader.unmarshalString      ("utc"  );
		mainshock_time     = reader.unmarshalLong        ("time" );
		mainshock_mag      = reader.unmarshalDouble      ("mag"  );
		mainshock_lat      = reader.unmarshalDouble      ("lat"  );
		mainshock_lon      = reader.unmarshalDouble      ("lon"  );
		mainshock_depth    = reader.unmarshalDouble      ("depth");

		reader.unmarshalMapEnd ();
		return;
	}




	// Marshal aftershock, in friendly format.

	protected void marshal_friendly_aftershock (int n, MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);

		long aftershock_time    = CompactEqkRupList.extract_time (mag_time_list[n]);
		double aftershock_mag   = CompactEqkRupList.extract_mag (mag_time_list[n]);
		double aftershock_lat   = CompactEqkRupList.extract_lat (lat_lon_depth_list[n]);
		double aftershock_lon   = CompactEqkRupList.extract_lon (lat_lon_depth_list[n]);
		double aftershock_depth = CompactEqkRupList.extract_depth (lat_lon_depth_list[n]);

		String aftershock_utc = "";
		if (aftershock_time != 0L) {
			aftershock_utc = SimpleUtils.time_to_parseable_string (aftershock_time);
		}

		writer.marshalString      ("utc"  , aftershock_utc  );
		writer.marshalLong        ("time" , aftershock_time );
		writer.marshalDouble      ("mag"  , aftershock_mag  );
		writer.marshalDouble      ("lat"  , aftershock_lat  );
		writer.marshalDouble      ("lon"  , aftershock_lon  );
		writer.marshalDouble      ("depth", aftershock_depth);

		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal aftershock, in friendly format.

	protected void unmarshal_friendly_aftershock (int n, MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);

		long aftershock_time;
		double aftershock_mag;
		double aftershock_lat;
		double aftershock_lon;
		double aftershock_depth;

		String aftershock_utc;

		aftershock_utc      = reader.unmarshalString      ("utc"  );
		aftershock_time     = reader.unmarshalLong        ("time" );
		aftershock_mag      = reader.unmarshalDouble      ("mag"  );
		aftershock_lat      = reader.unmarshalDouble      ("lat"  );
		aftershock_lon      = reader.unmarshalDouble      ("lon"  );
		aftershock_depth    = reader.unmarshalDouble      ("depth");

		mag_time_list[n] = CompactEqkRupList.combine_mag_time (aftershock_mag, aftershock_time);
		lat_lon_depth_list[n] = CompactEqkRupList.combine_lat_lon_depth (aftershock_lat, aftershock_lon, aftershock_depth);

		reader.unmarshalMapEnd ();
		return;
	}

	// Marshal aftershock list, in friendly format.

	protected void marshal_friendly_aftershock_list (MarshalWriter writer, String name) {
		int list_len = Math.max (eqk_count, 0);
		writer.marshalArrayBegin (name, list_len);
		for (int n = 0; n < list_len; ++n) {
			marshal_friendly_aftershock (n, writer, null);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal aftershock list, in friendly format.
	// This also allocates the arrays.

	protected void unmarshal_friendly_aftershock_list (MarshalReader reader, String name) {
		int list_len = reader.unmarshalArrayBegin (name);
		if (list_len != Math.max (eqk_count, 0)) {
			throw new MarshalException ("ForecastCatalog: Aftershock list length mismatch: list_len = " + list_len + ", eqk_count = " + eqk_count);
		}
		lat_lon_depth_list = new long[Math.max (eqk_count, 1)];
		lat_lon_depth_list[0] = 0L;
		mag_time_list = new long[Math.max (eqk_count, 1)];
		mag_time_list[0] = 0L;
		for (int n = 0; n < list_len; ++n) {
			unmarshal_friendly_aftershock (n, reader, null);
		}
		reader.unmarshalArrayEnd ();
		return;
	}




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 45001;		// current version for database and PDL
	private static final int MARSHAL_VER_2 = 45002;		// current version for friendly output

	private static final String M_VERSION_NAME = "ForecastCatalog";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 45000;
	protected static final int MARSHAL_FCAST_RESULT = 45001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_FCAST_RESULT;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = (f_friendly_format ? MARSHAL_VER_2 : MARSHAL_VER_1);
		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("ForecastData.do_marshal: Unknown version number: " + ver);

		case MARSHAL_VER_1:

			writer.marshalInt         ("eqk_count"         , eqk_count         );
			writer.marshalLongArray   ("lat_lon_depth_list", lat_lon_depth_list);
			writer.marshalLongArray   ("mag_time_list"     , mag_time_list     );

			break;

		case MARSHAL_VER_2:

			marshal_friendly_mainshock       (writer, "mainshock"                  );
			writer.marshalInt                (        "aftershock_count", eqk_count);
			marshal_friendly_aftershock_list (writer, "aftershock_list"            );

			break;
		}
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("ForecastData.do_umarshal: Unknown version number: " + ver);

		case MARSHAL_VER_1:

			set_standard_format();

			eqk_count          = reader.unmarshalInt         ("eqk_count"         );
			lat_lon_depth_list = reader.unmarshalLongArray   ("lat_lon_depth_list");
			mag_time_list      = reader.unmarshalLongArray   ("mag_time_list"     );

			break;

		case MARSHAL_VER_2:

			set_friendly_format (null);

			unmarshal_friendly_mainshock       (reader, "mainshock"       );
			eqk_count = reader.unmarshalInt    (        "aftershock_count");
			unmarshal_friendly_aftershock_list (reader, "aftershock_list" );

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

	public ForecastCatalog unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, ForecastCatalog obj) {

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

	public static ForecastCatalog unmarshal_poly (MarshalReader reader, String name) {
		ForecastCatalog result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("ForecastCatalog.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_FCAST_RESULT:
			result = new ForecastCatalog();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----

	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ForecastCatalog : Missing subcommand");
			return;
		}


		// Unrecognized subcommand.

		System.err.println ("ForecastCatalog : Unrecognized subcommand : " + args[0]);
		return;

	}

}
