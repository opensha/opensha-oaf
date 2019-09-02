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
 * Operation payload for analyst selection of options.
 * Author: Michael Barall 08/29/2019.
 */
public class OpAnalystSelection extends DBPayload {

	//----- Constants and variables -----

	// The contained analyst selection relay item.
	// Note: This cannot be null.  It must contain an analyst selection relay item.
	// The payload (of type RiAnalystSelection) must contain a non-null analyst_options,
	// and analyst_options.analyst_time must be the same as relay_time.
	// The relay_id should contain (in analyst form) the authoritative event id.

	public RelayItem ansel_relit;

	// Origin stamp to use when writing the relay item.
	// Stamp values >= 0L indicate that the relay item should be sent to the partner server.
	// Note: This should be 0L for a command issued by the analyst, or -1L for a command
	// issued in the relay link in response to a replicated relay item.

	public long relay_stamp;

	// Option for writing relay item to database, see RWOPT_XXXXX.

	public int relay_write_option;

	// The analyst selection payload.
	// Note: This is not independently marshaled/unmarshaled.  It is extracted from
	// ansel_relit when the object is unmarshaled or set up.  This allows conditions
	// to be checked, and error handling to be centralized.

	public RiAnalystSelection ansel_payload;

	// The event id.
	// This should be the authoritative Comcat event id.
	// Note: This is not independently marshaled/unmarshaled.  It is extracted from
	// ansel_relit when the object is unmarshaled or set up.  This allows conditions
	// to be checked, and error handling to be centralized.

	public String event_id;


	// Relay write option values.

	public static final int RWOPT_MIN = 1;
	public static final int RWOPT_NONE = 1;			// Do not write relay item, proceed unconditionally
	public static final int RWOPT_WRITE_NEW = 2;	// Write relay item, proceed only if it is a new relay item
	public static final int RWOPT_MAX = 2;


	// Return a string describing a relay write option.

	public static String get_rwopt_as_string (int rwopt) {
		switch (rwopt) {
		case RWOPT_NONE: return "RWOPT_NONE";
		case RWOPT_WRITE_NEW: return "RWOPT_WRITE_NEW";
		}
		return "RWOPT_INVALID(" + rwopt + ")";
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public OpAnalystSelection () {}


	// Extract payload and event id from the relay item.
	// An exception indicates an error in unmarshaling, or in parameter validation.

	private void extract_info () {

		// Relay item must be non-null

		if (ansel_relit == null) {
			throw new MarshalException ("OpAnalystSelection - Null relay item");
		}

		// Relay item must hold an analyst selection

		int ritype = RelaySupport.classify_relay_id (ansel_relit.get_relay_id());

		if (ritype != RelaySupport.RITYPE_ANALYST_SELECTION) {
			throw new MarshalException ("OpAnalystSelection - Invalid relay item type: " + RelaySupport.get_ritype_as_string (ritype));
		}

		// Extract the event id

		event_id = RelaySupport.analyst_relay_id_to_event_id (ansel_relit.get_relay_id());

		// Extract the payload

		ansel_payload = new RiAnalystSelection();
		ansel_payload.unmarshal_relay (ansel_relit);

		// Check for non-null analyst options

		if (ansel_payload.analyst_options == null) {
			throw new MarshalException ("OpAnalystSelection - Null analyst options");
		}

		// Check for correct analyst time

		if (ansel_payload.analyst_options.analyst_time != ansel_relit.get_relay_time()) {
			throw new MarshalException ("OpAnalystSelection - Analyst time mismatch: analyst_time = " + ansel_payload.analyst_options.analyst_time + ", relay_time = " + ansel_relit.get_relay_time());
		}

		// Check relay write option

		if (!( relay_write_option >= RWOPT_MIN && relay_write_option <= RWOPT_MAX )) {
			throw new MarshalException ("OpAnalystSelection - Invalid relay write option: " + get_rwopt_as_string (relay_write_option));
		}
	
		return;
	}


	// Set up the contents.
	// Note: An exception from here indicates a problem with the supplied parameters.

	public void setup (RelayItem the_ansel_relit, long the_relay_stamp, int the_relay_write_option) {
		ansel_relit = the_ansel_relit;
		relay_stamp = the_relay_stamp;
		relay_write_option = the_relay_write_option;

		// Extract information, and do error checking

		extract_info();
		return;
	}


	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OpAnalystSelection:" + "\n");

		result.append ("relay_stamp = " + relay_stamp + "\n");
		result.append ("relay_write_option = " + get_rwopt_as_string (relay_write_option) + "\n");
		result.append ("event_id = " + ((event_id == null) ? "null" : event_id) + "\n");
		result.append ("ansel_payload = " + ((ansel_payload == null) ? "null\n" : ansel_payload.toString()));

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 74001;

	private static final String M_VERSION_NAME = "OpAnalystSelection";

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

			RelayItem.marshal_poly          (writer, "ansel_relit"        , ansel_relit        );
			writer.marshalLong                      ("relay_stamp"        , relay_stamp        );
			writer.marshalInt                       ("relay_write_option" , relay_write_option );

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

			ansel_relit         = RelayItem.unmarshal_poly          (reader, "ansel_relit"        );
			relay_stamp         = reader.unmarshalLong                      ("relay_stamp"        );
			relay_write_option  = reader.unmarshalInt                       ("relay_write_option" , RWOPT_MIN, RWOPT_MAX);

			extract_info();

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
	public OpAnalystSelection unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a pending task.

	@Override
	public OpAnalystSelection unmarshal_task (PendingTask ptask) {
		try {
			unmarshal (ptask.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling pending task payload\n" + ptask.toString() + "\nDump:\n" + ptask.dump_details(), e);
		}
		return this;
	}

}
