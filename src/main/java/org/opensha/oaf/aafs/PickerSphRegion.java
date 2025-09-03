package org.opensha.oaf.aafs;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;

// PickerSphRegion is a region for selecting earthquakes in an event picker.
// It includes a SphRegion which defines a region of the earth's surface.
// It also includes the region description.

public class PickerSphRegion {

	// description - The picker region description.  Cannot be null.

	private String description;

	// region - The region.

	private SphRegion region;

	// Get the description.

	public String get_description() {
		return description;
	}

	// Get the region.

	public SphRegion get_region() {
		return region;
	}

	// toString returns the description.

	@Override
	public String toString() {
		return description;
	}

	// Display the contents.

	public String extended_string() {
		String str = "PickerSphRegion:\n"
			+ "description = " + description + "\n"
			+ "region = [" + region.toString() + "\n]";
		return str;
	}

	// Constructor saves the name, region, and magnitudes.

	public PickerSphRegion (String the_description, SphRegion the_region) {
		description = the_description;
		region = the_region;
	}

}
