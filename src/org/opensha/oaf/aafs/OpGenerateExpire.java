package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.RelayItem;
import org.opensha.oaf.aafs.entity.DBEntity;


/**
 * Operation payload for expiring a timeline.
 * Author: Michael Barall 05/16/2018.
 */
public class OpGenerateExpire extends DBPayload {

	//----- Constants and variables -----

	// Time stamp for the timeline entry when this was issued, in milliseconds since the epoch.

	public long action_time;

	// Time lag at which the last forecast occured, in milliseconds since the mainshock.
	// The value is -1L if there have been no prior forecasts.

	public long last_forecast_lag;

	// If the last forecast was not sent to PDL due to being a secondary server, these are the
	// relay item ids that can be used to check if the primary server sent the forecast.
	// Otherwise, this is an empty array.  It cannot be null.

	public String[] pdl_relay_ids;




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public OpGenerateExpire () {}


	// Set up the contents.

	public void setup (long the_action_time, long the_last_forecast_lag, String... the_pdl_relay_ids) {
		action_time = the_action_time;
		last_forecast_lag = the_last_forecast_lag;

		if (the_pdl_relay_ids == null) {
			pdl_relay_ids = new String[0];
		} else {
			pdl_relay_ids = the_pdl_relay_ids.clone();
		}

		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 30001;
	private static final int MARSHAL_VER_2 = 30002;

	private static final String M_VERSION_NAME = "OpGenerateExpire";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_2;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Superclass

		super.do_marshal (writer);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			writer.marshalLong ("action_time"      , action_time      );
			writer.marshalLong ("last_forecast_lag", last_forecast_lag);

			break;

		case MARSHAL_VER_2:

			writer.marshalLong        ("action_time"      , action_time      );
			writer.marshalLong        ("last_forecast_lag", last_forecast_lag);
			writer.marshalStringArray ("pdl_relay_ids"    , pdl_relay_ids    );

			break;
		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_2);

		// Superclass

		super.do_umarshal (reader);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			action_time       = reader.unmarshalLong ("action_time"      );
			last_forecast_lag = reader.unmarshalLong ("last_forecast_lag");
			
			pdl_relay_ids = new String[0];

			break;

		case MARSHAL_VER_2:

			action_time       = reader.unmarshalLong        ("action_time"      );
			last_forecast_lag = reader.unmarshalLong        ("last_forecast_lag");
			pdl_relay_ids     = reader.unmarshalStringArray ("pdl_relay_ids"    );

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
	public OpGenerateExpire unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a pending task.

	@Override
	public OpGenerateExpire unmarshal_task (PendingTask ptask) {
		try {
			unmarshal (ptask.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling pending task payload\n" + ptask.toString() + "\nDump:\n" + ptask.dump_details(), e);
		}
		return this;
	}

}
