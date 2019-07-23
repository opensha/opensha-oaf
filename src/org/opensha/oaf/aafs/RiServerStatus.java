package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

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

	//----- Constants and variables -----


	// This flag is set whenever any serialized variables within this object are changed.
	// It indicates that the server status needs to be written to the database.
	// Note: This variable is not serialized.

	private boolean f_changed;

	// Get the flag indicating if this object is changed.

	public boolean is_changed () {
		return f_changed;
	}

	// Clear the flag indicating if this object is changed.
	// This should be called after writing server status to the database.

	public void clear_changed () {
		f_changed = false;
	}




	//----- Fixed information -----

	// This is information about the server that is placed here during initialization.
	// It is for operator monitoring.

	// Program version.

	private String program_version;

	// Major version.

	private int major_version;

	// Minor version.

	private int minor_version;

	// Build.

	private int build;



	// Get the program version.

	public String get_program_version () {
		return program_version;
	}

	// Get the major version.

	public int get_major_version () {
		return major_version;
	}

	// Get the minor version.

	public int get_minor_version () {
		return minor_version;
	}

	// Get the build.

	public int get_build () {
		return build;
	}




	//----- Configuration -----

	// The time that the configuration was established, in milliseconds since the epoch.

	private long config_timestamp;

	// A unique identifier associated with this configuration.
	// It can be used as a tie-breaker if two configurations have exactly the same time.

	private String config_id;

	// The name of the primary server.

	private String primary_server_name;

	// The pairing mode.

	private int pairing_mode;

	public static final int PAIR_MODE_MIN = 1;
	public static final int PAIR_MODE_SOLO = 1;				// Solo mode, no pairing.
	public static final int PAIR_MODE_WATCH = 2;			// Watch other server, but no failover.
	public static final int PAIR_MODE_FAILOVER = 3;			// Failover enabled.
	public static final int PAIR_MODE_MAX = 3;




	//----- Boot -----

	// The time at which the server was booted.

	private long boot_time;




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiServerStatus () {}


	// Set up the contents.

	public void setup () {
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 58001;

	private static final String M_VERSION_NAME = "RiServerStatus";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents


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
