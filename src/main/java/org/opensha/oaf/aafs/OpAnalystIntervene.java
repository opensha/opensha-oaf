package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;


/**
 * Operation payload for analyst intervention in an event.
 * Author: Michael Barall 05/22/2018.
 */
public class OpAnalystIntervene extends DBPayload {

	//----- Constants and variables -----

	// State change request values.

	public static final int ASREQ_MIN = 1;
	public static final int ASREQ_NONE = 1;			// Do not change state
	public static final int ASREQ_START = 2;		// Start or continue generating forecasts
	public static final int ASREQ_STOP = 3;			// Stop generating forecasts
	public static final int ASREQ_WITHDRAW = 4;		// Withdraw the timeline
	public static final int ASREQ_MAX = 4;

	// Requested state change.

	public int state_change;

	// Flag, true to create timeline if it doesn't exist.

	public boolean f_create_timeline;

	// Parameters supplied by the analyst, or null if none.

	public AnalystOptions analyst_options;

	// Event identifier for associated relay item, or empty string "" if none.
	// If the task is has a Comcat ID in the event_id field, then relay_event_id should equal event_id.
	// Note: This does not have the relay item kind prefix.

	public String relay_event_id;

	// Time for associated relay item, or 0L if none.
	// Note: If there is an associated relay item, and analyst_options is non-null, and
	// analyst_options.analyst_time is non-zero, then relay_time should equal analyst_options.analyst_time
	// to ensure proper sequencing of analyst interventions.

	public long relay_time;

	// Origin stamp for associated relay item, or 0L if none.
	// Stamp values >= 0L indicate that the relay item should be sent to the partner server.

	public long relay_stamp;




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public OpAnalystIntervene () {}


	// Set up the contents, for no analyst data, and no associated relay item.

	public void setup (int the_state_change, boolean the_f_create_timeline) {
		state_change = the_state_change;
		f_create_timeline = the_f_create_timeline;
		analyst_options = null;

		relay_event_id = "";
		relay_time = 0L;
		relay_stamp = 0L;
		return;
	}


	// Set up the contents, with analyst data, and no associated relay item.

	public void setup (int the_state_change, boolean the_f_create_timeline,
						AnalystOptions the_analyst_options) {
		state_change = the_state_change;
		f_create_timeline = the_f_create_timeline;
		analyst_options = the_analyst_options;

		relay_event_id = "";
		relay_time = 0L;
		relay_stamp = 0L;
		return;
	}


	// Set up the contents, with analyst data and associated relay item.

	public void setup (int the_state_change, boolean the_f_create_timeline,
						AnalystOptions the_analyst_options,
						String the_relay_event_id, long the_relay_time, long the_relay_stamp) {
		state_change = the_state_change;
		f_create_timeline = the_f_create_timeline;
		analyst_options = the_analyst_options;

		relay_event_id = the_relay_event_id;
		relay_time = the_relay_time;
		relay_stamp = the_relay_stamp;
		return;
	}


	// Return the effective analyst parameters, or null if none.

	public ForecastParameters get_eff_analyst_params () {
		if (analyst_options != null) {
			return analyst_options.analyst_params;
		}
		return null;
	}


	// Return a string describing a state change request.

	public static String get_screq_as_string (int screq) {
		switch (screq) {
		case ASREQ_NONE: return "ASREQ_NONE";
		case ASREQ_START: return "ASREQ_START";
		case ASREQ_STOP: return "ASREQ_STOP";
		case ASREQ_WITHDRAW: return "ASREQ_WITHDRAW";
		}
		return "ASREQ_INVALID(" + screq + ")";
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 32001;

	private static final String M_VERSION_NAME = "OpAnalystIntervene";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Superclass

		super.do_marshal (writer);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			writer.marshalInt                       ("state_change"       , state_change       );
			writer.marshalBoolean                   ("f_create_timeline"  , f_create_timeline  );

			AnalystOptions.marshal_poly     (writer, "analyst_options"    , analyst_options    );

			writer.marshalString                    ("relay_event_id"     , relay_event_id     );
			writer.marshalLong                      ("relay_time"         , relay_time         );
			writer.marshalLong                      ("relay_stamp"        , relay_stamp        );

			break;
		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Superclass

		super.do_umarshal (reader);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1:

			state_change        = reader.unmarshalInt                       ("state_change"       , ASREQ_MIN, ASREQ_MAX);
			f_create_timeline   = reader.unmarshalBoolean                   ("f_create_timeline"  );

			analyst_options     = AnalystOptions.unmarshal_poly     (reader, "analyst_options"    );

			relay_event_id      = reader.unmarshalString                    ("relay_event_id"     );
			relay_time          = reader.unmarshalLong                      ("relay_time"         );
			relay_stamp         = reader.unmarshalLong                      ("relay_stamp"        );

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
	public OpAnalystIntervene unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a pending task.

	@Override
	public OpAnalystIntervene unmarshal_task (PendingTask ptask) {
		try {
			unmarshal (ptask.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling pending task payload\n" + ptask.toString() + "\nDump:\n" + ptask.dump_details(), e);
		}
		return this;
	}

}
