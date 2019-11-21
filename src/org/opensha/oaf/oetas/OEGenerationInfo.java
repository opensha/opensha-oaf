package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to hold information about a generation in an Operational ETAS catalog.
// Author: Michael Barall 11/19/2019.

public class OEGenerationInfo {

	//----- Contents -----

	// Number of earthquakes in the generation.
	// This must be > 0.

	public int gen_size;

	// Minimum and maximum magnitudes for this generation.
	// The maximum magnitude is generally the same for all generations.
	// The minimum magnitude may vary, in order to limit the size of the generation.

	public double gen_mag_min;
	public double gen_mag_max;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		gen_size = 0;
		gen_mag_min = 0.0;
		gen_mag_max = 0.0;
		return;
	}




	// Default constructor.

	public OEGenerationInfo () {
		clear();
	}




	// Set the values.

	public void set (int gen_size, double gen_mag_min, double gen_mag_max) {
		this.gen_size = gen_size;
		this.gen_mag_min = gen_mag_min;
		this.gen_mag_max = gen_mag_max;
		return;
	}




	//----- Marshaling -----

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		writer.marshalInt    ("gen_size"   , gen_size   );
		writer.marshalDouble ("gen_mag_min", gen_mag_min);
		writer.marshalDouble ("gen_mag_max", gen_mag_max);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OEGenerationInfo unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		gen_size    = reader.unmarshalInt    ("gen_size"   );
		gen_mag_min = reader.unmarshalDouble ("gen_mag_min");
		gen_mag_max = reader.unmarshalDouble ("gen_mag_max");
		reader.unmarshalMapEnd ();
		return this;
	}

}
