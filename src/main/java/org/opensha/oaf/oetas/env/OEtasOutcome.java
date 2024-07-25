package org.opensha.oaf.oetas.env;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;


// Common base class to hold results for operational ETAS.
// Author: Michael Barall 06/25/2024.
//
// This is a common base class for results that are saved in the database and the download file.

public abstract class OEtasOutcome implements Marshalable {


	//----- Forecast -----

	// Return true if there is a (non-null and non-empty) ETAS results JSON.

	public abstract boolean has_etas_json ();

	// Get the ETAS results JSON, or null or empty if none.

	public abstract String get_etas_json ();




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 128001;

	private static final String M_VERSION_NAME = "OEtasOutcome";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 128000;
	protected static final int MARSHAL_ETAS_RESULTS = 126001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected abstract int get_marshal_type ();

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		// <None>
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		// <None>

		return;
	}

	// Marshal object.

	@Override
	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		int mytype = get_marshal_type();
		writer.marshalInt (M_TYPE_NAME, mytype);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	@Override
	public OEtasOutcome unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		int mytype = get_marshal_type();
		int type = reader.unmarshalInt (M_TYPE_NAME, mytype, mytype);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEtasOutcome obj) {

		writer.marshalMapBegin (name);

		if (obj == null) {
			writer.marshalInt (M_TYPE_NAME, MARSHAL_NULL);
		} else {
			writer.marshalInt (M_TYPE_NAME, obj.get_marshal_type());
			obj.do_marshal (writer);
		}

		writer.marshalMapEnd ();

		return;
	}

	// Unmarshal object, polymorphic.

	public static OEtasOutcome unmarshal_poly (MarshalReader reader, String name) {
		OEtasOutcome result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEtasOutcome.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_ETAS_RESULTS:
			result = new OEtasResults();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEtasOutcome:" + "\n");

		boolean f_has_etas_json = has_etas_json();
		result.append ("has_etas_json = " + f_has_etas_json + "\n");

		if (f_has_etas_json) {
			result.append ("etas_json = " + get_etas_json() + "\n");
		}

		return result.toString();
	}




	// Display our contents, with the JSON string in display format.

	public String to_display_string () {
		StringBuilder result = new StringBuilder();

		result.append ("OEtasOutcome:" + "\n");

		boolean f_has_etas_json = has_etas_json();
		result.append ("has_etas_json = " + f_has_etas_json + "\n");

		if (f_has_etas_json) {
			result.append ("etas_json:" + "\n");
			result.append (MarshalUtils.display_json_string (get_etas_json()));
		}

		return result.toString();
	}

}
