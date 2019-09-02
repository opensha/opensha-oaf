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
 * Relay item payload for analyst selection of options.
 * Author: Michael Barall 05/14/2019.
 */
public class RiAnalystSelection extends DBPayload {

	//----- Constants and variables -----

	// Requested state change, see OpAnalystIntervene.ASREQ_XXXX.

	public int state_change;

	// Flag, true to create timeline if it doesn't exist.

	public boolean f_create_timeline;

	// Parameters supplied by the analyst, or null if none.

	public AnalystOptions analyst_options;


	// Return a friendly string representation of state_change.

	public String get_state_change_as_string () {
		return OpAnalystIntervene.get_screq_as_string (state_change);
	}

	// Return a friendly string representation of analyst_options.analyst_time.

	public String get_analyst_time_as_string () {
		if (analyst_options == null) {
			return "None";
		}
		if (analyst_options.analyst_time <= 0L) {
			return String.valueOf (analyst_options.analyst_time);
		}
		return SimpleUtils.time_raw_and_string (analyst_options.analyst_time);
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiAnalystSelection () {}


	// Set up the contents, for no analyst data.

	public void setup (int the_state_change, boolean the_f_create_timeline) {
		state_change = the_state_change;
		f_create_timeline = the_f_create_timeline;
		analyst_options = null;
		return;
	}


	// Set up the contents, with analyst data

	public void setup (int the_state_change, boolean the_f_create_timeline,
						AnalystOptions the_analyst_options) {
		state_change = the_state_change;
		f_create_timeline = the_f_create_timeline;
		analyst_options = the_analyst_options;
		return;
	}


	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("RiAnalystSelection:" + "\n");

		result.append ("state_change = " + get_state_change_as_string() + "\n");
		result.append ("f_create_timeline = " + f_create_timeline + "\n");
		result.append ("analyst_time = " + get_analyst_time_as_string() + "\n");
		result.append ("analyst_options = " + ((analyst_options == null) ? "null\n" : analyst_options.toString()));

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 57001;

	private static final String M_VERSION_NAME = "RiAnalystSelection";

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

			state_change        = reader.unmarshalInt                       ("state_change"       , OpAnalystIntervene.ASREQ_MIN, OpAnalystIntervene.ASREQ_MAX);
			f_create_timeline   = reader.unmarshalBoolean                   ("f_create_timeline"  );

			analyst_options     = AnalystOptions.unmarshal_poly     (reader, "analyst_options"    );

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
	public RiAnalystSelection unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a relay item.

	@Override
	public RiAnalystSelection unmarshal_relay (RelayItem relit) {
		try {
			unmarshal (relit.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling relay item payload\n" + relit.toString() + "\nDump:\n" + relit.dump_details(), e);
		}
		return this;
	}

}
