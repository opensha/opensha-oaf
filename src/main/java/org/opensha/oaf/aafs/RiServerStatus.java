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
 * Relay item payload for current server status.
 * Author: Michael Barall 07/17/2019.
 *
 * Server status is considered to be volatile.  It is stored in the database only as
 * a way to make it available to the other server and to operators.  Status is initialized
 * from scratch (not read from the database) at startup.
 */
public class RiServerStatus extends DBPayload {




	//----- Server information -----

	// Software major version.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public int sw_major_version;

	// Software minor version.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public int sw_minor_version;

	// Software build number.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public int sw_build;

	// Relay protocol version number.
	// This is used to verify that the partner servers use compatible protocols.

	public int protocol_version;

	// The server number, 1 or 2.

	public int server_number;




	//----- Heartbeat -----

	// The time of the last heartbeat, in milliseconds since the epoch.
	// This is set to the current time whenever the status changes.
	// It is also set to the current time periodically if the status remains unchanged,
	// to signal that the system is alive.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public long heartbeat_time;




	//----- Link state -----

	// The current link state, see RelayLink.LINK_XXXXX.

	public int link_state;




	//----- Relay configuration -----

	// The current relay configuration.

	public RelayConfig relay_config;




	//----- Primary negotiation -----

	// Current primary state, see RelayLink.PRIST_XXXXX.

	public int primary_state;

	// Time that the server was started (or restarted after error).
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public long start_time;




	//----- Inferred state -----

	// The inferred remote server state, see RelayLink.IST_XXXXX.

	public int inferred_state;




	//----- Health status -----

	// The subsystem health status flags, see HealthSupport.HS_XXXXX.

	public long health_status;




	//----- Service functions -----

	// Check if we can connect to remote server with this status.

	public boolean is_connectable () {
	
		// Can't connect if incompatible protocol

		if (protocol_version != RelayLink.OUR_PROTOCOL_VERSION) {
			return false;
		}

		// Note that stale heartbeat or bad health does not prevent connection

		// Can't connect if link state is shutdown or solo
		// (This test could possibly be omitted, given the tests on relay mode and primary state)

		if (link_state == RelayLink.LINK_SHUTDOWN || link_state == RelayLink.LINK_SOLO) {
			return false;
		}

		// Can't connect if relay mode is solo

		if (relay_config.get_relay_mode() == RelayLink.RMODE_SOLO) {
			return false;
		}

		// Can't connect if primary state is shutdown

		if (primary_state == RelayLink.PRIST_SHUTDOWN) {
			return false;
		}

		// No blocking condition, can connect

		return true;
	}


	// Get the software version as a string

	public String get_software_version_as_string () {
		return sw_major_version + "." + sw_minor_version + "." + sw_build;
	}


	// Get the protocol version as a string.

	public String get_protocol_version_as_string () {
		return "C" + protocol_version;
	}


	// Get the server number as a string.

	public String get_server_number_as_string () {
		return "N" + server_number;
	}


	// Get the heartbeat time as a string.

	public String get_heartbeat_time_as_string () {
		return SimpleUtils.time_raw_and_string_with_cutoff (heartbeat_time, 0L);
	}


	// Get the link state as a string.

	public String get_link_state_as_string () {
		return RelayLink.get_link_state_as_string (link_state);
	}


	// Get the mode timestamp as a string.

	public String get_mode_timestamp_as_string () {
		return SimpleUtils.time_raw_and_string_with_cutoff (relay_config.get_mode_timestamp(), 1000L);
	}


	// Get the configured relay mode as a string.

	public String get_relay_mode_as_string () {
		return RelayLink.get_relay_mode_as_string (relay_config.get_relay_mode());
	}


	// Get the configured primary server number as a string.

	public String get_configured_primary_as_string () {
		return "P" + relay_config.get_configured_primary();
	}


	// Get the primary state as a string.

	public String get_primary_state_as_string () {
		return RelayLink.get_primary_state_as_string (primary_state);
	}


	// Return true if we are configured as a primary server.

	public boolean is_configured_primary () {
		if (server_number == relay_config.get_configured_primary()) {
			return true;
		}
		return false;
	}


	// Get the start time as a string.

	public String get_start_time_as_string () {
		return SimpleUtils.time_raw_and_string_with_cutoff (start_time, 0L);
	}


	// Get the inferred state as a string.

	public String get_inferred_state_as_string () {
		return RelayLink.get_inferred_state_as_string (inferred_state);
	}


	// Get the health status as a short string.

	public String get_health_status_as_short_string () {
		return HealthSupport.hs_short_summary (health_status);
	}


	// Get the health status as a long string.

	public String get_health_status_as_long_string () {
		return HealthSupport.hs_long_summary (health_status);
	}


	// Test if the state matches the user strings.
	// Parameters:
	//  s_link_state = Link state to test (see RelayLink.test_user_string_link_state()).
	//  s_relay_mode = Relay mode to test (see RelayLink.test_user_string_relay_mode()).
	//  s_configured_primary = Configured primary to test (see RelayLink.test_user_string_configured_primary()).
	//  s_primary_state = Primary state to test (see RelayLink.test_user_string_primary_state()).
	// Returns true if state matches, false if not.
	// Throws IllegalArgumentException if any string is invalid.

	public boolean test_user_string_match (String s_link_state, String s_relay_mode, String s_configured_primary, String s_primary_state) {
	
		// Link state

		int r_link_state = RelayLink.test_user_string_link_state (s_link_state, link_state);
		if (r_link_state < 0) {
			throw new IllegalArgumentException ("RiServerStatus.test_user_string_match: Invalid link state selection: " + s_link_state);
		}
	
		// Relay mode

		int r_relay_mode = RelayLink.test_user_string_relay_mode (s_relay_mode, relay_config.get_relay_mode());
		if (r_relay_mode < 0) {
			throw new IllegalArgumentException ("RiServerStatus.test_user_string_match: Invalid relay mode selection: " + s_relay_mode);
		}
	
		// Configured primary

		int r_configured_primary = RelayLink.test_user_string_configured_primary (s_configured_primary, relay_config.get_configured_primary());
		if (r_configured_primary < 0) {
			throw new IllegalArgumentException ("RiServerStatus.test_user_string_match: Invalid configured primary selection: " + s_configured_primary);
		}
	
		// Primary state

		int r_primary_state = RelayLink.test_user_string_primary_state (s_primary_state, primary_state);
		if (r_primary_state < 0) {
			throw new IllegalArgumentException ("RiServerStatus.test_user_string_match: Invalid primary state selection: " + s_primary_state);
		}

		// Get result

		if (r_link_state > 0 && r_relay_mode > 0 && r_configured_primary > 0 && r_primary_state > 0) {
			return true;
		}
		return false;
	}


	// Convert to string.
	// The string produced does not have a final newline.

	@Override
	public String toString() {

		StringBuilder result = new StringBuilder();

		result.append ("info: ");
		result.append (get_server_number_as_string());
		result.append (", V");
		result.append (get_software_version_as_string());
		result.append (", ");
		result.append (get_protocol_version_as_string());
		result.append (", start = ");
		result.append (get_start_time_as_string());
		result.append ("\n");

		result.append ("config: ");
		result.append (get_relay_mode_as_string());
		result.append (", ");
		result.append (get_configured_primary_as_string());
		result.append (", timestamp = ");
		result.append (get_mode_timestamp_as_string());
		result.append ("\n");

		result.append ("state: ");
		result.append (get_link_state_as_string());
		result.append (", ");
		result.append (get_primary_state_as_string());
		result.append (", ");
		result.append (get_inferred_state_as_string());
		result.append (", ");
		result.append (get_health_status_as_short_string());
		result.append (", heartbeat = ");
		result.append (get_heartbeat_time_as_string());

		return result.toString();
	}


	// Create a one-line summary string.

	public String one_line_summary () {

		StringBuilder result = new StringBuilder();

		result.append (get_server_number_as_string());
		result.append (", ");
		result.append (get_relay_mode_as_string());
		result.append (", ");
		result.append (get_configured_primary_as_string());
		result.append (", ");
		result.append (get_link_state_as_string());
		result.append (", ");
		result.append (get_primary_state_as_string());
		result.append (", ");
		result.append (get_inferred_state_as_string());
		result.append (", ");
		result.append (get_health_status_as_short_string());
		result.append (", heartbeat = ");
		result.append (get_heartbeat_time_as_string());

		return result.toString();
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiServerStatus () {}


	// Fill the fixed server information fields

	public void fill_fixed_info () {
		sw_major_version = VersionInfo.major_version;
		sw_minor_version = VersionInfo.minor_version;
		sw_build = VersionInfo.build;
		protocol_version = RelayLink.OUR_PROTOCOL_VERSION;
		server_number = (new ServerConfig()).get_server_number();
		return;
	}


	// Fill fields to establish shutdown state.
	// This does not alter the fixed fields, or the relay configuration.

	public void fill_shutdown_state () {
		heartbeat_time = 0L;
		link_state = RelayLink.LINK_SHUTDOWN;
		primary_state = RelayLink.PRIST_SHUTDOWN;
		start_time = 0L;
		inferred_state = RelayLink.ISR_NONE;
		health_status = HealthSupport.HS_NOT_AVAILABLE;
		return;
	}


	// Fill relay configuration with the given configuration.

	public void fill_relay_config (RelayConfig the_relay_config) {
		relay_config = the_relay_config;
		return;
	}


	// Fill relay configuration with the given configuration, if the given configuration is more recent.
	// Returns true if relay configuration is changed, false if not.

	public boolean fill_relay_config_if_later (RelayConfig the_relay_config) {
		if (RelayConfig.compare (relay_config, the_relay_config) < 0) {
			relay_config = the_relay_config;
			return true;
		}
		return false;
	}


	// Return true if this object and the other object have the same relay configuration.

	public boolean is_same_relay_config (RiServerStatus other) {
		if (relay_config.is_equal_to (other.relay_config)) {
			return true;
		}
		return false;
	}


	// Return true if this object and the other object have a congruent relay configuration.
	// Congruent relay configurations are the same except possibly for the timestamp.

	public boolean is_congruent_relay_config (RiServerStatus other) {
		if (relay_config.is_congruent_to (other.relay_config)) {
			return true;
		}
		return false;
	}


	// Set up the contents, as shutdown with default relay configuration, which is solo primary.

	//  public void setup () {
	//  	fill_fixed_info ();
	//  	fill_shutdown_state ();
	//  	fill_relay_config ();
	//  	return;
	//  }


	// Set up the contents, as shutdown with the given relay configuration.

	public void setup (RelayConfig the_relay_config) {
		fill_fixed_info ();
		fill_shutdown_state ();
		fill_relay_config (the_relay_config);
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 58001;
	private static final int MARSHAL_VER_2 = 58002;
	private static final int MARSHAL_VER_3 = 58003;

	private static final String M_VERSION_NAME = "RiServerStatus";

	// Serialization offset for longs.

	private static final long OFFSERL = 2000000000000000L;		// 2*10^15 ~ 60,000 years

	// Serialization offset for ints.

	private static final int OFFSER = 200000000;				// 2*10^8   (note 2^28 = 268435456)

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

		case MARSHAL_VER_1:

			writer.marshalInt  ("sw_major_version"  , sw_major_version  + OFFSER);
			writer.marshalInt  ("sw_minor_version"  , sw_minor_version  + OFFSER);
			writer.marshalInt  ("sw_build"          , sw_build          + OFFSER);

			writer.marshalInt  ("protocol_version"  , protocol_version  );
			writer.marshalInt  ("server_number"     , server_number     );

			writer.marshalLong ("heartbeat_time"    , heartbeat_time    + OFFSERL);

			writer.marshalInt  ("link_state"        , link_state        );

			RelayConfig.marshal (writer, "relay_config", relay_config);

			writer.marshalInt  ("primary_state"     , primary_state     );
			writer.marshalLong ("start_time"        , start_time        + OFFSERL);

			break;

		case MARSHAL_VER_2:

			writer.marshalInt  ("sw_major_version"  , sw_major_version  + OFFSER);
			writer.marshalInt  ("sw_minor_version"  , sw_minor_version  + OFFSER);
			writer.marshalInt  ("sw_build"          , sw_build          + OFFSER);

			writer.marshalInt  ("protocol_version"  , protocol_version  );
			writer.marshalInt  ("server_number"     , server_number     );

			writer.marshalLong ("heartbeat_time"    , heartbeat_time    + OFFSERL);

			writer.marshalInt  ("link_state"        , link_state        );

			RelayConfig.marshal (writer, "relay_config", relay_config);

			writer.marshalInt  ("primary_state"     , primary_state     );
			writer.marshalLong ("start_time"        , start_time        + OFFSERL);

			writer.marshalInt  ("inferred_state"    , inferred_state    );

			break;

		case MARSHAL_VER_3:

			writer.marshalInt  ("sw_major_version"  , sw_major_version  + OFFSER);
			writer.marshalInt  ("sw_minor_version"  , sw_minor_version  + OFFSER);
			writer.marshalInt  ("sw_build"          , sw_build          + OFFSER);

			writer.marshalInt  ("protocol_version"  , protocol_version  );
			writer.marshalInt  ("server_number"     , server_number     );

			writer.marshalLong ("heartbeat_time"    , heartbeat_time    + OFFSERL);

			writer.marshalInt  ("link_state"        , link_state        );

			RelayConfig.marshal (writer, "relay_config", relay_config);

			writer.marshalInt  ("primary_state"     , primary_state     );
			writer.marshalLong ("start_time"        , start_time        + OFFSERL);

			writer.marshalInt  ("inferred_state"    , inferred_state    );

			writer.marshalLong ("health_status"     , health_status     + OFFSERL);

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

		case MARSHAL_VER_1:

			sw_major_version   = reader.unmarshalInt  ("sw_major_version"  ) - OFFSER;
			sw_minor_version   = reader.unmarshalInt  ("sw_minor_version"  ) - OFFSER;
			sw_build           = reader.unmarshalInt  ("sw_build"          ) - OFFSER;

			protocol_version   = reader.unmarshalInt  ("protocol_version"  );
			server_number      = reader.unmarshalInt  ("server_number"     , ServerConfigFile.SRVNUM_MIN, ServerConfigFile.SRVNUM_MAX);

			heartbeat_time     = reader.unmarshalLong ("heartbeat_time"    ) - OFFSERL;

			link_state         = reader.unmarshalInt  ("link_state"        , RelayLink.LINK_MIN, RelayLink.LINK_MAX);

			relay_config       = RelayConfig.unmarshal (reader, "relay_config");

			primary_state      = reader.unmarshalInt  ("primary_state"     , RelayLink.PRIST_MIN, RelayLink.PRIST_MAX);
			start_time         = reader.unmarshalLong ("start_time"        ) - OFFSERL;

			inferred_state     = RelayLink.ISR_NOT_SUPPLIED;

			health_status      = HealthSupport.HS_NOT_AVAILABLE;

			break;

		case MARSHAL_VER_2:

			sw_major_version   = reader.unmarshalInt  ("sw_major_version"  ) - OFFSER;
			sw_minor_version   = reader.unmarshalInt  ("sw_minor_version"  ) - OFFSER;
			sw_build           = reader.unmarshalInt  ("sw_build"          ) - OFFSER;

			protocol_version   = reader.unmarshalInt  ("protocol_version"  );
			server_number      = reader.unmarshalInt  ("server_number"     , ServerConfigFile.SRVNUM_MIN, ServerConfigFile.SRVNUM_MAX);

			heartbeat_time     = reader.unmarshalLong ("heartbeat_time"    ) - OFFSERL;

			link_state         = reader.unmarshalInt  ("link_state"        , RelayLink.LINK_MIN, RelayLink.LINK_MAX);

			relay_config       = RelayConfig.unmarshal (reader, "relay_config");

			primary_state      = reader.unmarshalInt  ("primary_state"     , RelayLink.PRIST_MIN, RelayLink.PRIST_MAX);
			start_time         = reader.unmarshalLong ("start_time"        ) - OFFSERL;

			inferred_state     = reader.unmarshalInt  ("inferred_state"    );

			health_status      = HealthSupport.HS_NOT_AVAILABLE;

			break;

		case MARSHAL_VER_3:

			sw_major_version   = reader.unmarshalInt  ("sw_major_version"  ) - OFFSER;
			sw_minor_version   = reader.unmarshalInt  ("sw_minor_version"  ) - OFFSER;
			sw_build           = reader.unmarshalInt  ("sw_build"          ) - OFFSER;

			protocol_version   = reader.unmarshalInt  ("protocol_version"  );
			server_number      = reader.unmarshalInt  ("server_number"     , ServerConfigFile.SRVNUM_MIN, ServerConfigFile.SRVNUM_MAX);

			heartbeat_time     = reader.unmarshalLong ("heartbeat_time"    ) - OFFSERL;

			link_state         = reader.unmarshalInt  ("link_state"        , RelayLink.LINK_MIN, RelayLink.LINK_MAX);

			relay_config       = RelayConfig.unmarshal (reader, "relay_config");

			primary_state      = reader.unmarshalInt  ("primary_state"     , RelayLink.PRIST_MIN, RelayLink.PRIST_MAX);
			start_time         = reader.unmarshalLong ("start_time"        ) - OFFSERL;

			inferred_state     = reader.unmarshalInt  ("inferred_state"    );

			health_status      = reader.unmarshalLong ("health_status"     ) - OFFSERL;

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
	public RiServerStatus unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a relay item.

	@Override
	public RiServerStatus unmarshal_relay (RelayItem relit) {
		try {
			unmarshal (relit.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling relay item payload\n" + relit.toString() + "\nDump:\n" + relit.dump_details(), e);
		}
		return this;
	}

}
