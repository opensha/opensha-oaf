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
 * Relay item payload for detection of an OAF product from another source.
 * Author: Michael Barall 07/05/2019.
 *
 * This items is generated when:
 * - The cleanup process finds an existing OAF product from another source
 *   (in practice, only when there is no OAF product from our source). 
 *
 * The relay_id should contain the authoritative event id.
 *
 * The existence of this item means PDL contains an OAF product from another source,
 * which was visible at ripfrn_detect_time.
 *
 * This item can be used in this way:
 * - When the cleanup process finds an earthquake with an existing OAF product,
 *   it can look for this item to determine if there was an OAF product from another
 *   source, and skip that earthquake if that product was detected recently, without
 *   doing a separate Comcat call.
 *
 * There is no need to retain items when ripfrn_detect_time is more than one month old,
 * because detections older than that are not used.
 * (Specifically, an item can be deleted if ripfrn_detect_time is older than
 * ActionConfig.get_removal_foreign_block().)
 */
public class RiPDLForeign extends DBPayload {

	//----- Constants and variables -----

	// Status code.
	// Note: All status codes are 3 digits, so that the JSON-encoded form
	// of this object has fixed length.

	public int ripfrn_status;

	// The time when the OAF product from another source was detected.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public long ripfrn_detect_time;

	// Status codes, which are possible values of ripfrn_status.

	public static final int RIPFRN_STAT_MIN				= 101;
	public static final int RIPFRN_STAT_FOREIGN			= 101;
		// The cleanup process found an existing OAF product from another source.
	public static final int RIPFRN_STAT_MAX				= 101;

	// Return a string describing the ripfrn_status.

	public String get_ripfrn_status_as_string () {
		switch (ripfrn_status) {
		case RIPFRN_STAT_FOREIGN: return "RIPFRN_STAT_FOREIGN";
		}
		return "RIPFRN_STAT_INVALID(" + ripfrn_status + ")";
	}

	// Return a friendly string representation of ripfrn_detect_time.

	public String get_ripfrn_detect_time_as_string () {
		return SimpleUtils.time_raw_and_string (ripfrn_detect_time);
	}

	// Return true if this item is expired.
	// Parameters:
	//  time_now = Current time, in milliseconds since the epoch.
	//  action_config = Action configuration parameters.
	// Returns true if this item is old enough that it can be deleted.

	public boolean is_expired (long time_now, ActionConfig action_config) {
		if (ripfrn_detect_time + action_config.get_removal_foreign_block() < time_now) {

			return true;
		}
		return false;
	}

	// Serialization offset for longs.

	private static final long OFFSERL = 2000000000000000L;		// 2*10^15 ~ 60,000 years




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiPDLForeign () {}


	// Set up the contents.

	public void setup (int the_ripfrn_status, long the_ripfrn_detect_time) {
		ripfrn_status = the_ripfrn_status;
		ripfrn_detect_time = the_ripfrn_detect_time;
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 63001;

	private static final String M_VERSION_NAME = "RiPDLForeign";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		writer.marshalInt  ("ripfrn_status"     , ripfrn_status     );
		writer.marshalLong ("ripfrn_detect_time", ripfrn_detect_time + OFFSERL);

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

		ripfrn_status      = reader.unmarshalInt  ("ripfrn_status"     , RIPFRN_STAT_MIN, RIPFRN_STAT_MAX);
		ripfrn_detect_time = reader.unmarshalLong ("ripfrn_detect_time") - OFFSERL;

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
	public RiPDLForeign unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a relay item.

	@Override
	public RiPDLForeign unmarshal_relay (RelayItem relit) {
		try {
			unmarshal (relit.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling relay item payload\n" + relit.toString() + "\nDump:\n" + relit.dump_details(), e);
		}
		return this;
	}

}
