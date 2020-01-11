package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to hold information about a generation in an Operational ETAS catalog.
// Author: Michael Barall 11/19/2019.

public class OEGenerationInfo {

	//----- Contents -----

	// Minimum and maximum magnitudes for this generation.
	// The maximum magnitude is generally the same for all generations.
	// The minimum magnitude may vary, in order to limit the size of the generation.

	public double gen_mag_min;
	public double gen_mag_max;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		gen_mag_min = 0.0;
		gen_mag_max = 0.0;
		return;
	}




	// Default constructor.

	public OEGenerationInfo () {
		clear();
	}




	// Set the values.

	public OEGenerationInfo set (double gen_mag_min, double gen_mag_max) {
		this.gen_mag_min = gen_mag_min;
		this.gen_mag_max = gen_mag_max;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEGenerationInfo:" + "\n");

		result.append ("gen_mag_min = " + gen_mag_min + "\n");
		result.append ("gen_mag_max = " + gen_mag_max + "\n");

		return result.toString();
	}




	// Produce a one-line string containing our contents (not newline-terminated).

	public String one_line_string () {
		return String.format ("mag_min=%.3f, mag_max=%.3f",
			gen_mag_min, gen_mag_max);
	}




	// Produce a one-line string containing our contents (not newline-terminated).
	// This version prepends an index and size.

	public String one_line_string (int i_gen, int gen_size) {
		return String.format ("%d: size=%d, mag_min=%.3f, mag_max=%.3f",
			i_gen, gen_size, gen_mag_min, gen_mag_max);
	}




	//----- Marshaling -----

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		writer.marshalDouble ("gen_mag_min", gen_mag_min);
		writer.marshalDouble ("gen_mag_max", gen_mag_max);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OEGenerationInfo unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		gen_mag_min = reader.unmarshalDouble ("gen_mag_min");
		gen_mag_max = reader.unmarshalDouble ("gen_mag_max");
		reader.unmarshalMapEnd ();
		return this;
	}




	//----- Testing -----




	// Check if two generation information structures are identical.
	// Note: This is primarily for testing.

	public boolean check_gen_equal (OEGenerationInfo other) {
		if (
			   this.gen_mag_min == other.gen_mag_min
			&& this.gen_mag_max == other.gen_mag_max
		) {
			return true;
		}
		return false;
	}




	// Set to plausible random values.
	// Note: This is primarily for testing.

	public OEGenerationInfo set_to_random (OERandomGenerator rangen) {
		this.gen_mag_min = rangen.uniform_sample (2.0, 6.0);
		this.gen_mag_max = rangen.uniform_sample (8.5, 9.5);
		return this;
	}

}
