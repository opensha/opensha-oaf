package scratch.aftershockStatistics.aafs;

import java.util.Arrays;

import scratch.aftershockStatistics.util.MarshalReader;
import scratch.aftershockStatistics.util.MarshalWriter;
import scratch.aftershockStatistics.util.MarshalException;

import scratch.aftershockStatistics.CompactEqkRupList;

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
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 45001;

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

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalInt         ("eqk_count"         , eqk_count         );
		writer.marshalLongArray   ("lat_lon_depth_list", lat_lon_depth_list);
		writer.marshalLongArray   ("mag_time_list"     , mag_time_list     );
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		eqk_count          = reader.unmarshalInt         ("eqk_count"         );
		lat_lon_depth_list = reader.unmarshalLongArray   ("lat_lon_depth_list");
		mag_time_list      = reader.unmarshalLongArray   ("mag_time_list"     );

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
