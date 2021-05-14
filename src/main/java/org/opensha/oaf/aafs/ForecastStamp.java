package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;


/**
 * Identifies a specific forecast.
 * Author: Michael Barall 08/26/2019.
 *
 * This is an immutable object.
 *
 * This object is guaranteed to contain a forecast lag, which can be retrieved.
 * Other than that, the object should be regarded as opaque.
 *
 * This object has several uses:
 * - To identify when two forecasts are the same.
 * - To identify when one forecast can be considered to confirm another forecast.
 * - To determine if analyst options have changed since a forecast was issued.
 * - To determine if two sets of analyst options are the same.
 *
 * Implementation note: We assume that two different sets of analyst-supplied
 * options have different values of analyst_time, and so analyst_time can identify
 * a set of options uniquely.  Zero analyst_time should only be encountered for
 * the system default analyst options.  Although null analyst options should not
 * be encountered, we use analyst_time == -1L to represent null analyst options.
 */
public class ForecastStamp {

	//----- Stamp -----

	// Forecast time lag, in milliseconds since the mainshock.
	// The value -1L is used if if no forecast lag is available, such as
	// if this does not refer to any forecast.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	private long forecast_lag;

	// The time stamp for the analyst options in effect, in milliseconds since the epoch.
	// The value -1L is used if if no time stamp is available, such as
	// if this does not refer to any forecast.
	// The value 0L should be used for default analyst options, i.e., all automatic.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	private long analyst_time;




	//----- Service functions -----

	// Get the forecast lag.

	public long get_forecast_lag () {
		return forecast_lag;
	}


	// Get the forecast lag as a string.

	public String get_forecast_lag_as_string () {
		return SimpleUtils.duration_to_string_2 (forecast_lag);
	}


	// Get a friendly string representation of this object.

	public String get_friendly_string () {
		String s_forecast_lag;
		if (forecast_lag <= 0L) {
			s_forecast_lag = String.valueOf (forecast_lag);
		} else {
			s_forecast_lag = SimpleUtils.duration_to_string_2 (forecast_lag);
		}
		String s_analyst_time;
		if (analyst_time <= 0L) {
			s_analyst_time = String.valueOf (analyst_time);
		} else {
			s_analyst_time = SimpleUtils.time_raw_and_string (analyst_time);
		}
		return "{forecast_lag = " + s_forecast_lag + ", analyst_time = " + s_analyst_time + "}";
	}




	// Return true if two forecast stamps are equal, meaning they identify the same forecast.

	public boolean is_equal_to (ForecastStamp other) {
		if (forecast_lag == other.forecast_lag
			&& analyst_time == other.analyst_time) {

			return true;
		}
		return false;
	}




	//  // Return true if this forecast is considered to confirm the other forecast.
	//  // Note: Confirmation means that one of the following two conditions is satisfied:
	//  // - this.forecast_lag > other.forecast_lag
	//  // - this.forecast_lag == other.forecast_lag and this.analyst_time >= other.analyst_time
	//  // The intent is that if this forecast has occurred, and it confirms the other forecast,
	//  // then the other forecast need not be generated.
	//  
	//  public boolean is_confirmation_of (ForecastStamp other) {
	//  	if (forecast_lag > other.forecast_lag) {
	//  		return true;
	//  	}
	//  	if (forecast_lag == other.forecast_lag && analyst_time >= other.analyst_time) {
	//  		return true;
	//  	}
	//  	return false;
	//  }




	// Return true if this forecast is considered to confirm the other forecast.
	// Note: Confirmation means that both of the following two conditions are satisfied:
	// - this.forecast_lag >= other.forecast_lag
	// - this.analyst_time >= other.analyst_time
	// The intent is that if this forecast has occurred, and it confirms the other forecast,
	// then the other forecast need not be generated.
	// We have adopted a very conservative criterion to minimize the chance of a forecast
	// with stale analyst options not being updated.

	public boolean is_confirmation_of (ForecastStamp other) {
		if (forecast_lag >= other.forecast_lag && analyst_time >= other.analyst_time) {
			return true;
		}
		return false;
	}




	// Return true if this stamp was constructed from the given options.
	// Implementation note: We assume different options have different analyst_time.

	public boolean is_from_options (AnalystOptions analyst_options) {
		if (analyst_options == null) {
			if (analyst_time == -1L) {
				return true;
			}
		} else {
			if (analyst_time == analyst_options.analyst_time) {
				return true;
			}
		}
		return false;
	}




	// Return true if the two sets of analyst options are the same.
	// Implementation note: We assume different options have different analyst_time.

	public static boolean are_same_options (AnalystOptions analyst_options_1, AnalystOptions analyst_options_2) {

		long analyst_time_1;
		if (analyst_options_1 == null) {
			analyst_time_1 = -1L;
		} else {
			analyst_time_1 = analyst_options_1.analyst_time;
		}

		long analyst_time_2;
		if (analyst_options_2 == null) {
			analyst_time_2 = -1L;
		} else {
			analyst_time_2 = analyst_options_2.analyst_time;
		}

		if (analyst_time_1 == analyst_time_2) {
			return true;
		}
		return false;
	}




	//----- Construction -----


	// Constructor, create an object for the given forecast lag and analyst options.

	public ForecastStamp (long the_forecast_lag, AnalystOptions analyst_options) {
		forecast_lag = the_forecast_lag;
		if (analyst_options == null) {
			analyst_time = -1L;
		} else {
			analyst_time = analyst_options.analyst_time;
		}
	}


	// Constructor, create an object for the given forecast lag, with default analyst options.
	// Note: This is not the same as ForecastStamp (the_forecast_lag, null).

	public ForecastStamp (long the_forecast_lag) {
		forecast_lag = the_forecast_lag;
		analyst_time = 0L;
	}


	// The default constructor creates an object that does not refer to any forecast.

	public ForecastStamp () {
		forecast_lag = -1L;
		analyst_time = -1L;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 73001;

	private static final String M_VERSION_NAME = "ForecastStamp";

	// Serialization offset for longs.

	private static final long OFFSERL = 2000000000000000L;		// 2*10^15 ~ 60,000 years

	// Serialization offset for ints.

	//private static final int OFFSER = 200000000;				// 2*10^8   (note 2^28 = 268435456)

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			writer.marshalLong ("forecast_lag"      , forecast_lag      + OFFSERL);
			writer.marshalLong ("analyst_time"      , analyst_time      + OFFSERL);

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

		case MARSHAL_VER_1:

			forecast_lag       = reader.unmarshalLong ("forecast_lag"      ) - OFFSERL;
			analyst_time       = reader.unmarshalLong ("analyst_time"      ) - OFFSERL;

			break;
		}

		return;
	}

	// Marshal object.

	public static void marshal (MarshalWriter writer, String name, ForecastStamp stamp) {
		writer.marshalMapBegin (name);
		stamp.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static ForecastStamp unmarshal (MarshalReader reader, String name) {
		ForecastStamp stamp = new ForecastStamp();
		reader.unmarshalMapBegin (name);
		stamp.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return stamp;
	}

}
