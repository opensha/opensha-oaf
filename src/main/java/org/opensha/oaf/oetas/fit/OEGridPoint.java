package org.opensha.oaf.oetas.fit;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;

import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.util.TestArgs;

import static org.opensha.oaf.util.SimpleUtils.rndd;
import static org.opensha.oaf.util.SimpleUtils.rndf;


// Class to hold one point in a grid of likelihoods.
// Author: Michael Barall 05/06/2023.

public class OEGridPoint implements Marshalable {

	//----- Parameters -----

	// Gutenberg-Richter parameter, b-value.

	public double b;

	// ETAS intensity parameter, alpha-value.

	public double alpha;

	//Omori c-value.

	public double c;

	// Omori p-value.

	public double p;

	// Branch ratio, n-value.
	// This controls the productivity of secondary triggering.

	public double n;

	// Mainshock productivity, ams-value, for reference magnitude equal to ZAMS_MREF == 0.0.

	public double zams;

	// Background productivity, mu-value, for reference magnitude equal to ZMU_MREF.

	public double zmu;




	//----- Construction -----




	// Clear to empty values.

	public final void clear () {

		b     = 0.0;
		alpha = 0.0;
		c     = 0.0;
		p     = 0.0;
		n     = 0.0;
		zams  = 0.0;
		zmu   = 0.0;

		return;
	}




	// Default constructor.

	public OEGridPoint () {
		clear();
	}




	// Constructor that sets up the ranges with the supplied values.

	public OEGridPoint (
		double b,
		double alpha,
		double c,
		double p,
		double n,
		double zams,
		double zmu
	) {
		this.b     = b;
		this.alpha = alpha;
		this.c     = c;
		this.p     = p;
		this.n     = n;
		this.zams  = zams;
		this.zmu   = zmu;
	}




	// Set up the ranges with the supplied values.
	// Returns this object.

	public final OEGridPoint set (
		double b,
		double alpha,
		double c,
		double p,
		double n,
		double zams,
		double zmu
	) {
		this.b     = b;
		this.alpha = alpha;
		this.c     = c;
		this.p     = p;
		this.n     = n;
		this.zams  = zams;
		this.zmu   = zmu;
		return this;
	}




	// Copy values from another object.
	// Returns this object.

	public final OEGridPoint copy_from (OEGridPoint other) {
		this.b     = other.b;
		this.alpha = other.alpha;
		this.c     = other.c;
		this.p     = other.p;
		this.n     = other.n;
		this.zams  = other.zams;
		this.zmu   = other.zmu;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGridPoint:" + "\n");

		result.append ("b = "     + b     + "\n");
		result.append ("alpha = " + alpha + "\n");
		result.append ("c = "     + c     + "\n");
		result.append ("p = "     + p     + "\n");
		result.append ("n = "     + n     + "\n");
		result.append ("zams = "  + zams  + "\n");
		result.append ("zmu = "   + zmu   + "\n");

		return result.toString();
	}




	// Dump the contents to a string.

	public String dump_to_string () {
		StringBuilder result = new StringBuilder();

		result.append ("b = "     + rndf(b)     + "\n");
		result.append ("alpha = " + rndf(alpha) + "\n");
		result.append ("c = "     + rndf(c)     + "\n");
		result.append ("p = "     + rndf(p)     + "\n");
		result.append ("n = "     + rndf(n)     + "\n");
		result.append ("zams = "  + rndf(zams)  + "\n");
		result.append ("zmu = "   + rndf(zmu)   + "\n");

		return result.toString();
	}




	//----- Marshaling -----




	// Marshal version number.

	private static final int MARSHAL_VER_1 = 119001;
	public static final int XVER_1 = 119001;

	private static final String M_VERSION_NAME = "OEGridPoint";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("b"    , b    );
			writer.marshalDouble ("alpha", alpha);
			writer.marshalDouble ("c"    , c    );
			writer.marshalDouble ("p"    , p    );
			writer.marshalDouble ("n"    , n    );
			writer.marshalDouble ("zams" , zams );
			writer.marshalDouble ("zmu"  , zmu  );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			b     = reader.unmarshalDouble ("b"    );
			alpha = reader.unmarshalDouble ("alpha");
			c     = reader.unmarshalDouble ("c"    );
			p     = reader.unmarshalDouble ("p"    );
			n     = reader.unmarshalDouble ("n"    );
			zams  = reader.unmarshalDouble ("zams" );
			zmu   = reader.unmarshalDouble ("zmu"  );

		}
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
	public OEGridPoint unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEGridPoint grid_point) {
		grid_point.marshal (writer, name);
		return;
	}

	// Unmarshal object.

	public static OEGridPoint static_unmarshal (MarshalReader reader, String name) {
		return (new OEGridPoint()).unmarshal (reader, name);
	}




//	// Marshal object with external version, internal.
//	// This is called with the map already open.
//
//	private void do_marshal_xver (MarshalWriter writer, int xver) {
//
//		switch (xver) {
//
//		default:
//			throw new MarshalException ("OEGridPoint.do_marshal_xver: Unknown version code: version = " + xver);
//
//		// Version 1
//
//		case XVER_1: {
//
//			writer.marshalDouble ("b"    , b    );
//			writer.marshalDouble ("alpha", alpha);
//			writer.marshalDouble ("c"    , c    );
//			writer.marshalDouble ("p"    , p    );
//			writer.marshalDouble ("n"    , n    );
//			writer.marshalDouble ("zams" , zams );
//			writer.marshalDouble ("zmu"  , zmu  );
//
//		}
//		break;
//
//		}
//		return;
//	}
//
//	// Unmarshal object with external version, internal.
//	// This is called with the map already open.
//
//	private void do_umarshal_xver (MarshalReader reader, int xver) {
//
//		switch (xver) {
//
//		default:
//			throw new MarshalException ("OEGridPoint.do_umarshal_xver: Unknown version code: version = " + xver);
//		
//		// Version 1
//
//		case XVER_1: {
//
//			b     = reader.unmarshalDouble ("b"    );
//			alpha = reader.unmarshalDouble ("alpha");
//			c     = reader.unmarshalDouble ("c"    );
//			p     = reader.unmarshalDouble ("p"    );
//			n     = reader.unmarshalDouble ("n"    );
//			zams  = reader.unmarshalDouble ("zams" );
//			zmu   = reader.unmarshalDouble ("zmu"  );
//
//		}
//		break;
//
//		}
//		return;
//	}
//
//	// Marshal object with external version.
//
//	public void marshal_xver (MarshalWriter writer, String name, int xver) {
//		writer.marshalMapBegin (name);
//		do_marshal_xver (writer, xver);
//		writer.marshalMapEnd ();
//		return;
//	}
//
//	// Unmarshal object with external version.
//
//	public OEGridPoint unmarshal_xver (MarshalReader reader, String name, int xver) {
//		reader.unmarshalMapBegin (name);
//		do_umarshal_xver (reader, xver);
//		reader.unmarshalMapEnd ();
//		return this;
//	}




	//----- Testing -----




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "OEGridPoint");







		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
