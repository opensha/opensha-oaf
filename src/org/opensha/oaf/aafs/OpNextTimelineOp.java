package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.RelayItem;


/**
 * Operation payload for issuing next delayed operation for a timeline.
 * Author: Michael Barall 05/30/2019.
 */
public class OpNextTimelineOp extends DBPayload {

	//----- Constants and variables -----

	// Reason codes.

	public static final int NTOREAS_MIN = 1;
	public static final int NTOREAS_REBUILD = 1;	// Rebuilding task queue from timelines
	public static final int NTOREAS_PRIMARY = 2;	// Transitioning from secondary to primary server
	public static final int NTOREAS_MAX = 2;

	// Reason for issuing next timeline operation.

	public int nto_reason;




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public OpNextTimelineOp () {}


	// Set up the contents, with reason code.

	public void setup (int the_nto_reason) {
		nto_reason = the_nto_reason;
		return;
	}





	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 59001;

	private static final String M_VERSION_NAME = "OpNextTimelineOp";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		writer.marshalInt ("nto_reason", nto_reason);

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

		nto_reason = reader.unmarshalInt ("nto_reason", NTOREAS_MIN, NTOREAS_MAX);

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
	public OpNextTimelineOp unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a pending task.

	@Override
	public OpNextTimelineOp unmarshal_task (PendingTask ptask) {
		try {
			unmarshal (ptask.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling pending task payload\n" + ptask.toString() + "\nDump:\n" + ptask.dump_details(), e);
		}
		return this;
	}

}
