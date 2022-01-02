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
 * Configuration for a relay link.
 * Author: Michael Barall 08/12/2019.
 *
 * This is an immutable object.
 */
public class RelayConfig {

	//----- Relay configuration -----

	// Timestamp for this configuration.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	private long mode_timestamp;

	// Configured relay mode, see RelayLink.RMODE_XXXXX.

	private int relay_mode;

	// Configured primary server number.

	private int configured_primary;




	//----- Service functions -----

	// Get the timestamp for this configuration.

	public final long get_mode_timestamp () {
		return mode_timestamp;
	}

	// Get the configured relay mode, see RelayLink.RMODE_XXXXX.

	public final int get_relay_mode () {
		return relay_mode;
	}

	// Get the configured primary server number.

	public final int get_configured_primary () {
		return configured_primary;
	}


	// Get the timestamp as a string.

	public final String get_mode_timestamp_as_string () {
		return SimpleUtils.time_raw_and_string (mode_timestamp);
	}


	// Get the configured relay mode as a string.

	public final String get_relay_mode_as_string () {
		return RelayLink.get_relay_mode_as_string (relay_mode);
	}


	// Get the configured primary server number as a string.

	public final String get_configured_primary_as_string () {
		return "P" + configured_primary;
	}




	// Compare two relay configurations.
	// Returns 0 if config1 is identical to config2.
	// Returns < 0 if config1 is before (earlier than) config2.
	// Returns > 0 if config1 is after (later than) config2.
	// The comparison is primarily based on mode_timestamp.
	// If the value of mode_timestamp is equal, then relay_mode and configured_primary are used as tie-breakers.

	public static int compare (RelayConfig config1, RelayConfig config2) {

		// Compare first by earliest timestamp, second by lower mode, third by higher configured primary
	
		int result = Long.compare (config1.mode_timestamp, config2.mode_timestamp);
		if (result == 0) {
			result = Integer.compare (config1.relay_mode, config2.relay_mode);
			if (result == 0) {
				result = Integer.compare (config2.configured_primary, config1.configured_primary);
			}
		}

		return result;
	}




	// Return true if two relay configurations are equal.

	public boolean is_equal_to (RelayConfig other) {
		if (mode_timestamp == other.mode_timestamp
			&& relay_mode == other.relay_mode
			&& configured_primary == other.configured_primary) {

			return true;
		}
		return false;
	}




	// Return true if two relay configurations are congruent.
	// Congruent relay configurations are the same except possibly for the timestamp.

	public boolean is_congruent_to (RelayConfig other) {
		if (relay_mode == other.relay_mode
			&& configured_primary == other.configured_primary) {

			return true;
		}
		return false;
	}




	//----- Construction -----


	// Constructor, create an object with the given configuration.

	public RelayConfig (long the_mode_timestamp, int the_relay_mode, int the_configured_primary) {
		mode_timestamp = the_mode_timestamp;
		relay_mode = the_relay_mode;
		configured_primary = the_configured_primary;
	}


	// Constructor, create an object with the default configuration, which is solo primary.

	public RelayConfig (long the_mode_timestamp) {
		this (the_mode_timestamp, RelayLink.RMODE_SOLO, (new ServerConfig()).get_server_number());
	}


	// The default constructor is only used for unmarshaling.

	private RelayConfig () {}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 72001;

	private static final String M_VERSION_NAME = "RelayConfig";

	// Serialization offset for longs.

	private static final long OFFSERL = 2000000000000000L;		// 2*10^15 ~ 60,000 years

	// Serialization offset for ints.

	//private static final int OFFSER = 200000000;				// 2*10^8   (note 2^28 = 268435456)

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		writer.marshalLong ("mode_timestamp"    , mode_timestamp    + OFFSERL);
		writer.marshalInt  ("relay_mode"        , relay_mode        );
		writer.marshalInt  ("configured_primary", configured_primary);

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		mode_timestamp     = reader.unmarshalLong ("mode_timestamp"    ) - OFFSERL;
		relay_mode         = reader.unmarshalInt  ("relay_mode"        , RelayLink.RMODE_MIN, RelayLink.RMODE_MAX);
		configured_primary = reader.unmarshalInt  ("configured_primary", ServerConfigFile.SRVNUM_MIN, ServerConfigFile.SRVNUM_MAX);

		return;
	}

	// Marshal object.

	public static void marshal (MarshalWriter writer, String name, RelayConfig config) {
		writer.marshalMapBegin (name);
		config.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static RelayConfig unmarshal (MarshalReader reader, String name) {
		RelayConfig config = new RelayConfig();
		reader.unmarshalMapBegin (name);
		config.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return config;
	}

}
