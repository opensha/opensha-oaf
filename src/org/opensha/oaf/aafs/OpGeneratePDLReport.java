package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;


/**
 * Operation payload for generating a PDL report.
 * Author: Michael Barall 05/14/2018.
 */
public class OpGeneratePDLReport extends DBPayload {

	//----- Constants and variables -----

	// Time stamp for the timeline entry when this was issued, in milliseconds since the epoch.

	public long action_time;

	// Forecast stamp which identifies the last forecast issued.
	// The contained forecast lag can be -1L if there have been no prior forecasts.
	// This is always non-null.

	public ForecastStamp last_forecast_stamp;

	// Time of the first attempt to send report to PDL, in milliseconds since the epoch.

	public long base_pdl_time;

	// Time at which the mainshock occurred, in milliseconds since the epoch.
	// Can be 0L if this object was unmarshaled from old version 1.
	// If pdl_relay_ids is non-empty, then mainshock_time cannot be 0L.

	public long mainshock_time;




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public OpGeneratePDLReport () {}


	// Set up the contents.

	public void setup (long the_action_time, ForecastStamp the_last_forecast_stamp, long the_base_pdl_time, long the_mainshock_time) {
		action_time = the_action_time;
		last_forecast_stamp = the_last_forecast_stamp;
		base_pdl_time = the_base_pdl_time;
		mainshock_time = the_mainshock_time;
		return;
	}




	//----- Service functions -----

	// Return the lag of the last forecast, or -1L if there is no last forecast.

	public long get_last_forecast_lag () {
		return last_forecast_stamp.get_forecast_lag();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 29001;
	private static final int MARSHAL_VER_2 = 29002;
	private static final int MARSHAL_VER_3 = 29003;

	private static final String M_VERSION_NAME = "OpGeneratePDLReport";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_3;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Superclass

		super.do_marshal (writer);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			long last_forecast_lag = last_forecast_stamp.get_forecast_lag();

			writer.marshalLong ("action_time"      , action_time      );
			writer.marshalLong ("last_forecast_lag", last_forecast_lag);
			writer.marshalLong ("base_pdl_time"    , base_pdl_time    );

			}
			break;

		case MARSHAL_VER_2: {

			long last_forecast_lag = last_forecast_stamp.get_forecast_lag();

			writer.marshalLong ("action_time"      , action_time      );
			writer.marshalLong ("last_forecast_lag", last_forecast_lag);
			writer.marshalLong ("base_pdl_time"    , base_pdl_time    );
			writer.marshalLong ("mainshock_time"   , mainshock_time   );

			}
			break;

		case MARSHAL_VER_3:

			writer.marshalLong    ("action_time"      , action_time      );
			ForecastStamp.marshal (writer, "last_forecast_stamp", last_forecast_stamp);
			writer.marshalLong    ("base_pdl_time"    , base_pdl_time    );
			writer.marshalLong    ("mainshock_time"   , mainshock_time   );

			break;
		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_3);

		// Superclass

		super.do_umarshal (reader);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			long last_forecast_lag;

			action_time       = reader.unmarshalLong ("action_time"      );
			last_forecast_lag = reader.unmarshalLong ("last_forecast_lag");
			base_pdl_time     = reader.unmarshalLong ("base_pdl_time"    );
			
			mainshock_time = 0L;

			last_forecast_stamp = new ForecastStamp (last_forecast_lag);

			}
			break;

		case MARSHAL_VER_2: {

			long last_forecast_lag;

			action_time       = reader.unmarshalLong ("action_time"      );
			last_forecast_lag = reader.unmarshalLong ("last_forecast_lag");
			base_pdl_time     = reader.unmarshalLong ("base_pdl_time"    );
			mainshock_time    = reader.unmarshalLong ("mainshock_time"   );

			last_forecast_stamp = new ForecastStamp (last_forecast_lag);

			}
			break;

		case MARSHAL_VER_3:

			action_time         = reader.unmarshalLong    ("action_time"      );
			last_forecast_stamp = ForecastStamp.unmarshal (reader, "last_forecast_stamp");
			base_pdl_time       = reader.unmarshalLong    ("base_pdl_time"    );
			mainshock_time      = reader.unmarshalLong    ("mainshock_time"   );

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
	public OpGeneratePDLReport unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a pending task.

	@Override
	public OpGeneratePDLReport unmarshal_task (PendingTask ptask) {
		try {
			unmarshal (ptask.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling pending task payload\n" + ptask.toString() + "\nDump:\n" + ptask.dump_details(), e);
		}
		return this;
	}

}
