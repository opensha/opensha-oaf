package org.opensha.oaf.aafs;

import java.util.List;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.RelayItem;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Support functions for server relay.
 * Author: Michael Barall 05/30/2019.
 */
public class RelaySupport extends ServerComponent {

	//----- Constants -----


	// Relay item type codes.

	public static final int RITYPE_UNKNOWN = 0;					// Unknown relay item type
	public static final int RITYPE_SERVER_STATUS = 1;			// Server status relay item
	public static final int RITYPE_PDL_COMPLETION = 2;			// PDL completion relay item
	public static final int RITYPE_ANALYST_SELECTION = 3;		// Analyst selection relay item

	// Return a string describing a relay item type.

	public String get_ritype_as_string (int x) {
		switch (x) {
		case RITYPE_UNKNOWN: return "RITYPE_UNKNOWN";
		case RITYPE_SERVER_STATUS: return "RITYPE_SERVER_STATUS";
		case RITYPE_PDL_COMPLETION: return "RITYPE_PDL_COMPLETION";
		case RITYPE_ANALYST_SELECTION: return "RITYPE_ANALYST_SELECTION";
		}
		return "RITYPE_INVALID(" + x + ")";
	}




	//----- Relay item namespaces -----


	// The name used for server status relay items.

	//public static final String RI_STATUS_ID = "===status===";

	// The prefix used for server status relay items.

	public static final String RI_STATUS_PREFIX = "=";

	// The prefix used for PDL completion relay items.

	public static final String RI_PDL_PREFIX = "%";

	// The prefix used for analyst selection relay items.

	public static final String RI_ANALYST_PREFIX = "@";


	// Classify a relay item id

	public static int classify_relay_id (String relay_id) {
		if (relay_id.startsWith (RI_STATUS_PREFIX)) {
			return RITYPE_SERVER_STATUS;
		}
		if (relay_id.startsWith (RI_PDL_PREFIX)) {
			return RITYPE_PDL_COMPLETION;
		}
		if (relay_id.startsWith (RI_ANALYST_PREFIX)) {
			return RITYPE_ANALYST_SELECTION;
		}
		return RITYPE_UNKNOWN;
	}


	// Convert a machine name to a server status relay id.

	public static String machine_name_to_status_relay_id (String machine_name) {
		return RI_STATUS_PREFIX + machine_name;
	}

	// Convert a server status relay id to a machine name.

	public static String status_relay_id_to_machine_name (String relay_id) {
		if (!( relay_id.startsWith (RI_STATUS_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.status_relay_id_to_machine_name: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_STATUS_PREFIX.length());
	}


	// Convert an event id to a pdl completion relay id.

	public static String event_id_to_pdl_relay_id (String event_id) {
		return RI_PDL_PREFIX + event_id;
	}

	// Convert an array of event ids to an array of pdl completion relay ids.

	public static String[] event_ids_to_pdl_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = RI_PDL_PREFIX + event_ids[i];
		}
		return result;
	}

	// Convert a pdl completion relay id to an event id.

	public static String pdl_relay_id_to_event_id (String relay_id) {
		if (!( relay_id.startsWith (RI_PDL_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.pdl_relay_id_to_event_id: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_PDL_PREFIX.length());
	}


	// Convert an event id to an analyst selection relay id.

	public static String event_id_to_analyst_relay_id (String event_id) {
		return RI_ANALYST_PREFIX + event_id;
	}

	// Convert an array of event ids to an array of analyst selection relay ids.

	public static String[] event_ids_to_analyst_relay_ids (String[] event_ids) {
		int len = event_ids.length;
		String[] result = new String[len];
		for (int i = 0; i < len; ++i) {
			result[i] = RI_ANALYST_PREFIX + event_ids[i];
		}
		return result;
	}

	// Convert an analyst selection relay id to an event id.

	public static String analyst_relay_id_to_event_id (String relay_id) {
		if (!( relay_id.startsWith (RI_ANALYST_PREFIX) )) {
			throw new IllegalArgumentException("RelaySupport.analyst_relay_id_to_event_id: Invalid relay ID supplied: " + relay_id);
		}

		return relay_id.substring (RI_ANALYST_PREFIX.length());
	}





	//----- Thread management -----


	// The thread for pulling relay items from the partner server.

	private RelayThread relay_thread;




	//----- Construction -----


	// Default constructor.

	public RelaySupport () {

		relay_thread = new RelayThread();

	}


	// Set up this component by linking to the server group.
	// A subclass may override this to perform additional setup operations.

	@Override
	public void setup (ServerGroup the_sg) {
		super.setup (the_sg);

		relay_thread.setup();

		return;
	}

}
