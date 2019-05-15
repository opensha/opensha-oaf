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
 * Relay item payload for analyst selection of options.
 * Author: Michael Barall 05/14/2019.
 */
public class RiAnalystSelection extends DBPayload {

	//----- Constants and variables -----




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiAnalystSelection () {}


	// Set up the contents.

	public void setup () {
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 57001;

	private static final String M_VERSION_NAME = "RiAnalystSelection";

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
