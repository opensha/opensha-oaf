package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to specify the time and magnitude ranges of an Operational ETAS catalog.
// Author: Michael Barall 03/07/2022.

public class OECatalogRange {

	// The range of times for which earthquakes are generated, in days.
	// The time origin is not specified by this class.

	public double tbegin;
	public double tend;

	// The minimum magnitude to use for the simulation.

	public double mag_min_sim;

	// The maximum magnitude to use for the simulation.

	public double mag_max_sim;

	// The magnitude excess, or 0.0 if none.
	// If positive, then a generator can produce ruptures with magnitudes between
	// mag_max_sim and mag_max_sim + mag_excess, and stop the simulation at the
	// time of the first such rupture.

	public double mag_excess;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		tbegin          = 0.0;
		tend            = 0.0;
		mag_min_sim     = 0.0;
		mag_max_sim     = 0.0;
		mag_excess      = 0.0;
		return;
	}




	// Default constructor.

	public OECatalogRange () {
		clear();
	}




	// Construct and set all values.

	public OECatalogRange (
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim,
		double mag_excess
	) {
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		this.mag_excess      = mag_excess;
	}




	// Set all values.

	public final OECatalogRange set (
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim,
		double mag_excess
	) {
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		this.mag_excess      = mag_excess;
		return this;
	}




	// Copy all values from the other object.

	public final OECatalogRange copy_from (OECatalogRange other) {
		this.tbegin          = other.tbegin;
		this.tend            = other.tend;
		this.mag_min_sim     = other.mag_min_sim;
		this.mag_max_sim     = other.mag_max_sim;
		this.mag_excess      = other.mag_excess;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogRange:" + "\n");

		result.append ("tbegin = "          + tbegin          + "\n");
		result.append ("tend = "            + tend            + "\n");
		result.append ("mag_min_sim = "     + mag_min_sim     + "\n");
		result.append ("mag_max_sim = "     + mag_max_sim     + "\n");
		result.append ("mag_excess = "      + mag_excess      + "\n");

		return result.toString();
	}




	// Display our contents in a form used for progress reporting.

	public final String progress_string() {
		StringBuilder result = new StringBuilder();

		result.append ("tbegin = "      + tbegin                      + "\n");
		result.append ("tend = "        + tend                        + "\n");
		result.append ("duration = "    + (tend - tbegin)             + "\n");
		result.append ("mag_min_sim = " + mag_min_sim                 + "\n");
		result.append ("mag_max_sim = " + mag_max_sim                 + "\n");
		result.append ("span = "        + (mag_max_sim - mag_min_sim) + "\n");
		result.append ("mag_excess = "  + mag_excess                  + "\n");

		return result.toString();
	}




	// Clip the end time to be no more than the supplied value.

	public final void clip_tend (double max_tend) {
		if (tend > max_tend) {
			tend = max_tend;
		}
		return;
	}




	// Calculate the minimum magnitude that would change the expected rate by a desired ratio.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  r = Ratio of expected rates, must satisfy r > 0.
	// Returns the minimum magnitude that would produce the desired change in rate.
	// Note: The intended use is to rescale the minimum magnitude to change the
	// size of generated catalogs; set r = new_size / old_size.

	public final double calc_rescaled_min_mag (double b, double r) {
		return OERandomGenerator.gr_inv_ratio_rate (b, mag_min_sim, mag_max_sim, mag_max_sim, r);
	}




	// Set the minimum magnitude to change the expected rate by a desired ratio.
	// Parameters:
	//  b = Gutenberg-Richter b parameter.
	//  r = Ratio of expected rates, must satisfy r > 0.
	// Sets the minimum magnitude that would produce the desired change in rate.
	// Note: The intended use is to rescale the minimum magnitude to change the
	// size of generated catalogs; set r = new_size / old_size.

	public final void set_rescaled_min_mag (double b, double r) {
		mag_min_sim = calc_rescaled_min_mag (b, r);
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 100001;

	private static final String M_VERSION_NAME = "OECatalogRange";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("tbegin"         , tbegin         );
			writer.marshalDouble ("tend"           , tend           );
			writer.marshalDouble ("mag_min_sim"    , mag_min_sim    );
			writer.marshalDouble ("mag_max_sim"    , mag_max_sim    );
			writer.marshalDouble ("mag_excess"     , mag_excess     );

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

			tbegin          = reader.unmarshalDouble ("tbegin"         );
			tend            = reader.unmarshalDouble ("tend"           );
			mag_min_sim     = reader.unmarshalDouble ("mag_min_sim"    );
			mag_max_sim     = reader.unmarshalDouble ("mag_max_sim"    );
			mag_excess      = reader.unmarshalDouble ("mag_excess"     );

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OECatalogRange unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogRange catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogRange static_unmarshal (MarshalReader reader, String name) {
		OECatalogRange catalog = new OECatalogRange();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two range structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_range_equal (OECatalogRange other) {
		if (
			   this.tbegin          == other.tbegin         
			&& this.tend            == other.tend           
			&& this.mag_min_sim     == other.mag_min_sim    
			&& this.mag_max_sim     == other.mag_max_sim    
			&& this.mag_excess      == other.mag_excess    
		) {
			return true;
		}
		return false;
	}

}
