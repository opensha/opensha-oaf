package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import static org.opensha.oaf.oetas.OECatalogParams.MAG_ADJ_ORIGINAL;
import static org.opensha.oaf.oetas.OECatalogParams.MAG_ADJ_FIXED;
import static org.opensha.oaf.oetas.OECatalogParams.MAG_ADJ_SEED_EST;


// Class to specify the time and magnitude ranges of an Operational ETAS catalog.
// Author: Michael Barall 03/07/2022.
//
// Holds the subset of the parameters in OECatalogParams that determine the
// time and magnitude range of a simulation.
//
// Usage note: This class is used both to extract the time and magnitude range from
// a set of catalog parameters, and to insert a modified time and magnitude range
// into a set of catalog parameters.

public class OECatalogRange {

	// The range of times for which earthquakes are generated, in days.
	// The time origin is not specified by this class.

	public double tbegin;
	public double tend;

	// The minimum magnitude to use for the simulation.

	public double mag_min_sim;

	// The maximum magnitude to use for the simulation.

	public double mag_max_sim;

	// The range of minimum magnitudes to use for the simulation.
	// The minimum magnitude of each generation may be varied within this range,
	// in accordance with the selected magnitude adjustment method.
	// Should satisfy mag_min_lo <= mag_min_sim <= mag_min_hi,
	// with equality if the minimum magnitude is not being adjusted.

	public double mag_min_lo;
	public double mag_min_hi;

	// The range of maximum magnitudes to use for the simulation.
	// The maximum magnitude of each generation may be varied within this range,
	// in accordance with the selected magnitude adjustment method.
	// Should satisfy mag_max_lo <= mag_max_sim <= mag_max_hi,
	// with equality if the maximum magnitude is not being adjusted.

	public double mag_max_lo;
	public double mag_max_hi;

	// The target generation size.
	// Depending on the selected magnitude adjustment method, the minimum magnitude
	// of a generation may be adjusted so that the expected number of aftershocks is
	// equal to gen_size_target, but within the range mag_min_lo to mag_min_hi.
	// If mag_min_lo == mag_min_sim == mag_min_hi, then the minimum magnitude is held
	// fixed at mag_min_sim, but gen_size_target must still be set to a reasonable value.

	public int gen_size_target;

	// The magnitude excess, or 0.0 if none.
	// If positive, then a generator can produce ruptures with magnitudes between
	// mag_max_sim and mag_max_sim + mag_excess, and stop the simulation at the
	// time of the first such rupture.

	public double mag_excess;

	// The magnitude adjustment method, see MAG_ADJ_XXXXX.
	// Selects the method that a generator uses to adjust the magnitude range
	// during a simulation.

	public int mag_adj_meth;

	// The generation count to use when estimating the ultimate catalog size,
	// when adjusting the magnitude range.  The value 2 selects the first generation
	// after the seeds, i.e., the direct aftershocks of the seeds.

	public int madj_gen_br;

	// The branch ratio de-rating factor to use when estimated the ultimate
	// catalog size, when adjusting the magnitude range.
	// This allows for the fact that the effective branch ratio decreases with
	// succeeding generations, as aftershocks become later in time.

	public double madj_derate_br;

	// The probability of exceeding the maximum magnitude, based on the estimated
	// ultimate catalog size, when adjusting the magnitude range.
	// A generator may assume that this value is small.

	public double madj_exceed_fr;




	//----- Magnitude range adjustment methods -----




	// Set the magnitude adjustment method to original.
	// Assumes that the following fields are already set up:
	//  mag_min_sim, mag_max_sim, mag_min_lo, mag_min_hi, gen_size_target.
	// Note: Same as OECatalogParams.set_mag_adj_original.

	public final OECatalogRange set_mag_adj_original () {
		mag_max_lo = mag_max_sim;
		mag_max_hi = mag_max_sim;
		mag_adj_meth = MAG_ADJ_ORIGINAL;
		madj_gen_br = 0;
		madj_derate_br = 0.0;
		madj_exceed_fr = 0.0;
		return this;
	}


	// Set the magnitude adjustment method to original, for a fixed magnitude range.
	// Assumes that the following fields are already set up:
	//  mag_min_sim, mag_max_sim.
	// Note: Same as OECatalogParams.set_mag_adj_original_fixed.

	public final OECatalogRange set_mag_adj_original_fixed () {
		mag_min_lo = mag_min_sim;
		mag_min_hi = mag_min_sim;
		mag_max_lo = mag_max_sim;
		mag_max_hi = mag_max_sim;
		gen_size_target = 100;
		mag_adj_meth = MAG_ADJ_ORIGINAL;
		madj_gen_br = 0;
		madj_derate_br = 0.0;
		madj_exceed_fr = 0.0;
		return this;
	}


	// Finish setting the magnitude adjustment method to original, for original code.
	// Assumes that the following fields are already set up:
	//  mag_min_sim, mag_max_sim, mag_min_lo, mag_min_hi, mag_max_lo, mag_max_hi, gen_size_target.
	// Note: Same as OECatalogParams.finish_mag_adj_original.

	private void finish_mag_adj_original () {
		mag_adj_meth = MAG_ADJ_ORIGINAL;
		madj_gen_br = 0;
		madj_derate_br = 0.0;
		madj_exceed_fr = 0.0;
		return;
	}




	// Set the magnitude adjustment method to fixed.
	// Assumes that the following fields are already set up:
	//  mag_min_sim, mag_max_sim.
	// Note: Same as OECatalogParams.set_mag_adj_fixed.

	public final OECatalogRange set_mag_adj_fixed () {
		mag_min_lo = mag_min_sim;
		mag_min_hi = mag_min_sim;
		mag_max_lo = mag_max_sim;
		mag_max_hi = mag_max_sim;
		gen_size_target = 100;
		mag_adj_meth = MAG_ADJ_FIXED;
		madj_gen_br = 0;
		madj_derate_br = 0.0;
		madj_exceed_fr = 0.0;
		return this;
	}




	// Set the magnitude adjustment method to seed estimate.
	// Assumes that the following fields are already set up:
	//  mag_min_sim, mag_max_sim, mag_min_lo, mag_min_hi, mag_max_lo, mag_max_hi,
	//  gen_size_target, madj_gen_br, madj_derate_br, madj_exceed_fr.
	// Note: Same as OECatalogParams.set_mag_adj_seed_est.

	public final OECatalogRange set_mag_adj_seed_est () {
		mag_adj_meth = MAG_ADJ_SEED_EST;
		return this;
	}




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		tbegin          = 0.0;
		tend            = 0.0;
		mag_min_sim     = 0.0;
		mag_max_sim     = 0.0;
		mag_min_lo      = 0.0;
		mag_min_hi      = 0.0;
		mag_max_lo      = 0.0;
		mag_max_hi      = 0.0;
		gen_size_target = 0;
		mag_excess      = 0.0;
		mag_adj_meth    = MAG_ADJ_ORIGINAL;
		madj_gen_br     = 0;
		madj_derate_br  = 0.0;
		madj_exceed_fr  = 0.0;
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
		double mag_min_lo,
		double mag_min_hi,
		double mag_max_lo,
		double mag_max_hi,
		int gen_size_target,
		double mag_excess,
		int mag_adj_meth,
		int madj_gen_br,
		double madj_derate_br,
		double madj_exceed_fr
	) {
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		this.mag_min_lo      = mag_min_lo;
		this.mag_min_hi      = mag_min_hi;
		this.mag_max_lo      = mag_max_lo;
		this.mag_max_hi      = mag_max_hi;
		this.gen_size_target = gen_size_target;
		this.mag_excess      = mag_excess;
		this.mag_adj_meth    = mag_adj_meth;
		this.madj_gen_br     = madj_gen_br;
		this.madj_derate_br  = madj_derate_br;
		this.madj_exceed_fr  = madj_exceed_fr;
	}




	// Set all values, for a range with fixed magnitude.

	public final OECatalogRange set_range_fixed (
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
		set_mag_adj_fixed();
		return this;
	}




	// Set all values, for a range with magnitude adjusted by seed productivity estimate.
	// Coerces mag_min_sim and mag_max_sim to lie witin their respective ranges.

	public final OECatalogRange set_range_seed_est (
		double tbegin,
		double tend,
		double mag_min_sim,
		double mag_max_sim,
		double mag_min_lo,
		double mag_min_hi,
		double mag_max_lo,
		double mag_max_hi,
		int gen_size_target,
		double mag_excess,
		int madj_gen_br,
		double madj_derate_br,
		double madj_exceed_fr
	) {
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		this.mag_min_lo      = mag_min_lo;
		this.mag_min_hi      = mag_min_hi;
		this.mag_max_lo      = mag_max_lo;
		this.mag_max_hi      = mag_max_hi;
		this.gen_size_target = gen_size_target;
		this.mag_excess      = mag_excess;
		this.madj_gen_br     = madj_gen_br;
		this.madj_derate_br  = madj_derate_br;
		this.madj_exceed_fr  = madj_exceed_fr;

		if (this.mag_min_sim < this.mag_min_lo) {
			this.mag_min_sim = this.mag_min_lo;
		} else if (this.mag_min_sim > this.mag_min_hi) {
			this.mag_min_sim = this.mag_min_hi;
		}

		if (this.mag_max_sim < this.mag_max_lo) {
			this.mag_max_sim = this.mag_max_lo;
		} else if (this.mag_max_sim > this.mag_max_hi) {
			this.mag_max_sim = this.mag_max_hi;
		}

		set_mag_adj_seed_est();
		return this;
	}




	// Copy all values from the other object.

	public final OECatalogRange copy_from (OECatalogRange other) {
		this.tbegin          = other.tbegin;
		this.tend            = other.tend;
		this.mag_min_sim     = other.mag_min_sim;
		this.mag_max_sim     = other.mag_max_sim;
		this.mag_min_lo      = other.mag_min_lo;
		this.mag_min_hi      = other.mag_min_hi;
		this.mag_max_lo      = other.mag_max_lo;
		this.mag_max_hi      = other.mag_max_hi;
		this.gen_size_target = other.gen_size_target;
		this.mag_excess      = other.mag_excess;
		this.mag_adj_meth    = other.mag_adj_meth;
		this.madj_gen_br     = other.madj_gen_br;
		this.madj_derate_br  = other.madj_derate_br;
		this.madj_exceed_fr  = other.madj_exceed_fr;
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
		result.append ("mag_min_lo = "      + mag_min_lo      + "\n");
		result.append ("mag_min_hi = "      + mag_min_hi      + "\n");
		result.append ("mag_max_lo = "      + mag_max_lo      + "\n");
		result.append ("mag_max_hi = "      + mag_max_hi      + "\n");
		result.append ("gen_size_target = " + gen_size_target + "\n");
		result.append ("mag_excess = "      + mag_excess      + "\n");
		result.append ("mag_adj_meth = "    + mag_adj_meth    + "\n");
		result.append ("madj_gen_br = "     + madj_gen_br     + "\n");
		result.append ("madj_derate_br = "  + madj_derate_br  + "\n");
		result.append ("madj_exceed_fr = "  + madj_exceed_fr  + "\n");

		return result.toString();
	}




	// Display our contents in a form used for progress reporting.

	public final String progress_string() {
		StringBuilder result = new StringBuilder();

		result.append ("tbegin = "      + tbegin                      + "\n");
		result.append ("tend = "        + tend                        + "\n");
		result.append ("duration = "    + (tend - tbegin)             + "\n");

		if (mag_adj_meth == MAG_ADJ_FIXED) {

			result.append ("mag_min_sim = "     + mag_min_sim                 + "\n");
			result.append ("mag_max_sim = "     + mag_max_sim                 + "\n");
			result.append ("span = "            + (mag_max_sim - mag_min_sim) + "\n");
			result.append ("mag_excess = "      + mag_excess                  + "\n");
			result.append ("mag_adj_meth = "    + mag_adj_meth                + "\n");

		} else {

			result.append ("mag_min_sim = "     + mag_min_sim                 + "\n");
			result.append ("mag_max_sim = "     + mag_max_sim                 + "\n");
			result.append ("mag_min_lo = "      + mag_min_lo                  + "\n");
			result.append ("mag_min_hi = "      + mag_min_hi                  + "\n");
			result.append ("mag_max_lo = "      + mag_max_lo                  + "\n");
			result.append ("mag_max_hi = "      + mag_max_hi                  + "\n");
			result.append ("gen_size_target = " + gen_size_target             + "\n");
			result.append ("mag_excess = "      + mag_excess                  + "\n");
			result.append ("mag_adj_meth = "    + mag_adj_meth                + "\n");
			result.append ("madj_gen_br = "     + madj_gen_br                 + "\n");
			result.append ("madj_derate_br = "  + madj_derate_br              + "\n");
			result.append ("madj_exceed_fr = "  + madj_exceed_fr              + "\n");

		}

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
	// Note: This should only be used when a fixed magnitude range is in effect.

	public final void set_rescaled_min_mag (double b, double r) {
		mag_min_sim = calc_rescaled_min_mag (b, r);
		set_mag_adj_fixed();
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
			writer.marshalDouble ("mag_min_lo"     , mag_min_lo     );
			writer.marshalDouble ("mag_min_hi"     , mag_min_hi     );
			writer.marshalDouble ("mag_max_lo"     , mag_max_lo     );
			writer.marshalDouble ("mag_max_hi"     , mag_max_hi     );
			writer.marshalInt    ("gen_size_target", gen_size_target);
			writer.marshalDouble ("mag_excess"     , mag_excess     );
			writer.marshalInt    ("mag_adj_meth"   , mag_adj_meth   );
			writer.marshalInt    ("madj_gen_br"    , madj_gen_br    );
			writer.marshalDouble ("madj_derate_br" , madj_derate_br );
			writer.marshalDouble ("madj_exceed_fr" , madj_exceed_fr );

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
			mag_min_lo      = reader.unmarshalDouble ("mag_min_lo"     );
			mag_min_hi      = reader.unmarshalDouble ("mag_min_hi"     );
			mag_max_lo      = reader.unmarshalDouble ("mag_max_lo"     );
			mag_max_hi      = reader.unmarshalDouble ("mag_max_hi"     );
			gen_size_target = reader.unmarshalInt    ("gen_size_target");
			mag_excess      = reader.unmarshalDouble ("mag_excess"     );
			mag_adj_meth    = reader.unmarshalInt    ("mag_adj_meth"   );
			madj_gen_br     = reader.unmarshalInt    ("madj_gen_br"    );
			madj_derate_br  = reader.unmarshalDouble ("madj_derate_br" );
			madj_exceed_fr  = reader.unmarshalDouble ("madj_exceed_fr" );

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

	public static void static_marshal (MarshalWriter writer, String name, OECatalogRange catalog_range) {
		writer.marshalMapBegin (name);
		catalog_range.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogRange static_unmarshal (MarshalReader reader, String name) {
		OECatalogRange catalog_range = new OECatalogRange();
		reader.unmarshalMapBegin (name);
		catalog_range.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog_range;
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
			&& this.mag_min_lo      == other.mag_min_lo     
			&& this.mag_min_hi      == other.mag_min_hi     
			&& this.mag_max_lo      == other.mag_max_lo     
			&& this.mag_max_hi      == other.mag_max_hi     
			&& this.gen_size_target == other.gen_size_target
			&& this.mag_excess      == other.mag_excess    
			&& this.mag_adj_meth    == other.mag_adj_meth  
			&& this.madj_gen_br     == other.madj_gen_br  
			&& this.madj_derate_br  == other.madj_derate_br  
			&& this.madj_exceed_fr  == other.madj_exceed_fr  
		) {
			return true;
		}
		return false;
	}

}
