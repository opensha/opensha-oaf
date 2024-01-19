package org.opensha.oaf.rj;

import java.util.List;
import java.util.Set;
import java.util.Collection;

import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.Region;

import org.opensha.oaf.rj.OAFTectonicRegime;

import org.opensha.oaf.util.SphLatLon;
import org.opensha.oaf.util.SphRegion;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// OAFSphRegion is a region for defining a tectonic regime.
// It includes a SphRegion which defines a region of the earth's surface,
// and minimum and maximum depths which define a depth range.
// It also includes the tectonic regime.

public class OAFSphRegion extends OAFRegion {

	// region - The region.

	private SphRegion region;

	// min_depth - The minimum depth, in km (depth is positive down).

	private double min_depth;

	// max_depth - The maximum depth, in km (depth is positive down).

	private double max_depth;

	// toString - Convert to string.

	@Override
	public String toString() {
		String str = "OAFSphRegion\n"
			+ "\tRegime: " + get_regime() + "\n"
			+ "\tMinLat: " + region.getMinLat() + "\n"
			+ "\tMinLon: " + region.getMinLon() + "\n"
			+ "\tMaxLat: " + region.getMaxLat() + "\n"
			+ "\tMaxLon: " + region.getMaxLon() + "\n"
			+ "\tMinDepth: " + min_depth + "\n"
			+ "\tMaxDepth: " + max_depth;
		return str;
	}

	// Constructor saves the regime, region, and depth range.

	public OAFSphRegion (OAFTectonicRegime the_regime, SphRegion the_region, double the_min_depth, double the_max_depth) {
		super (the_regime);
		region = the_region;
		min_depth = the_min_depth;
		max_depth = the_max_depth;
	}

	// contains - Determine whether the given location is inside the region.

	@Override
	public boolean contains (Location loc) {
		return loc.getDepth() >= min_depth
			&& loc.getDepth() <= max_depth
			&& region.contains(loc);
	}




	//----- Marshaling -----




	// Static function to marshal an OAFSphRegion.
	// An exception is thrown if obj is not of type OAFSphRegion.

	public static void static_marshal (MarshalWriter writer, String name, OAFRegion obj) {

		// Check for correct type

		if (!( obj instanceof OAFSphRegion )) {
			throw new MarshalException ("OAFSphRegion: Region object is not of type OAFSphRegion");
		}

		// Get the region

		OAFSphRegion the_region = (OAFSphRegion)obj;

		// Begin the JSON object

		writer.marshalMapBegin (name);

		// Write the tectonic regime

		writer.marshalString ("regime", the_region.get_regime().toString());

		// Write the depth range
				
		writer.marshalDouble ("min_depth", the_region.min_depth);
		writer.marshalDouble ("max_depth", the_region.max_depth);

		// Write the spherical region

		SphRegion.marshal_poly (writer, "region", the_region.region) ;

		// End the JSON object

		writer.marshalMapEnd ();

		return;
	}




	// Static function to unmarshal an OAFSphRegion.

	public static OAFSphRegion static_unmarshal (MarshalReader reader, String name) {

		// Begin the JSON object

		reader.unmarshalMapBegin (name);

		// Get the tectonic regime

		String the_regime_name = reader.unmarshalString ("regime");
		OAFTectonicRegime the_regime = OAFTectonicRegime.forName (the_regime_name);

		// Get the depth range
				
		double the_min_depth = reader.unmarshalDouble ("min_depth");
		double the_max_depth = reader.unmarshalDouble ("max_depth");

		if (the_min_depth >= the_max_depth) {
			throw new MarshalException ("OAFSphRegion: Minimum and maximum depths are reversed");
		}

		// Get the spherical region

		SphRegion the_sph_region = SphRegion.unmarshal_poly (reader, "region") ;

		if (the_sph_region == null) {
			throw new MarshalException ("OAFSphRegion: No spherical region specified");
		}

		// Form the region

		OAFSphRegion the_region = new OAFSphRegion (the_regime, the_sph_region, the_min_depth, the_max_depth);

		// End the JSON object

		reader.unmarshalMapEnd ();

		return the_region;
	}




	// Static function to marshal a list of OAFSphRegion.
	// An exception is thrown if any list element is not of type OAFSphRegion.

	public static void static_marshal_list (MarshalWriter writer, String name, List<? extends OAFRegion> region_list) {

		// Number of regions

		int region_count = region_list.size();
		writer.marshalArrayBegin (name, region_count);

		// Unmarshal each region

		for (int region_i = 0; region_i < region_count; ++region_i) {
			OAFRegion obj = region_list.get (region_i);
			static_marshal (writer, null, obj);
		}

		// End array of special regions

		writer.marshalArrayEnd ();

		return;
	}




	// Static function to unmarshal a a list of OAFSphRegion.

	public static void static_unmarshal_list (MarshalReader reader, String name, List<? super OAFSphRegion> region_list) {

		// Number of regions

		int region_count = reader.unmarshalArrayBegin (name);

		// Unmarshal each region

		for (int region_i = 0; region_i < region_count; ++region_i) {
			OAFSphRegion the_region = static_unmarshal (reader, null);
			region_list.add (the_region);
		}

		// End array of special regions

		reader.unmarshalArrayEnd ();

		return;
	}

}
