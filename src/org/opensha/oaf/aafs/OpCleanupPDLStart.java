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
 * Operation payload to start cleaning up old forecasts in PDL.
 * Author: Michael Barall 07/03/2019.
 */
public class OpCleanupPDLStart extends DBPayload {

	//----- Constants and variables -----




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public OpCleanupPDLStart () {}


	// Set up the contents.

	public void setup () {
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 61001;

	private static final String M_VERSION_NAME = "OpCleanupPDLStart";

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
	public OpCleanupPDLStart unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a pending task.

	@Override
	public OpCleanupPDLStart unmarshal_task (PendingTask ptask) {
		try {
			unmarshal (ptask.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling pending task payload\n" + ptask.toString() + "\nDump:\n" + ptask.dump_details(), e);
		}
		return this;
	}

}
