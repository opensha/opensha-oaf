package org.opensha.oaf.oetas;

import java.util.Arrays;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to store parameters for an Operational ETAS catalog.
// Author: Michael Barall 12/02/2019.
//
// Holds the parameters for a single realization of an ETAS catalog.
//
// The parameter definition is as follows.  For an earthquake of magnitude m0
// occuring at time t0, the rate of direct aftershocks, per unit time, per unit
// magnitude, is
//
//   lambda(t, m) = k * b * log(10) * (10^(-b*(m - mref))) * ((t-t0+c)^(-p))
//
//   k = 10^(a + alpha*(m0 - mref))
//
//   mref <= m0 <= msup
//
//   a = Productivity parameter
//   p = Omori exponent parameter
//   c = Omori offset parameter
//   b = Gutenberg-Richter parameter
//   alpha = ETAS intensity parameter
//   mref = Reference magnitude = minimum considered magnitude.
//   msup = Maximum considered magnitude.
//
// The implication of the inequality mref <= m0 <= msup is that the earthquake
// is presumed to be drawn from a population of earthquakes with magnitudes
// that lie in the range [mref, msup] and follow a Gutenberg-Richter
// distribution.  This requirement is needed because, if m0 is allowed to
// vary without limit, then the total rate of aftershocks (integrated over m0)
// would be infinite.
//
// Notice that the magnitude range [mref, msup] is an intrinsic part of the
// definition of the productivity "a" parameter.  If the magnitude range is
// changed, then the value of "a" (or equivalently "k") would also change.
//
// In the first generation (that is, the seed earthquakes), the value of "a" may
// be varied to account for the seismic network magnitude of completeness, and
// for the different character of mainshocks versus aftershocks (perhaps due to
// mainshocks rupturing the full depth of the seismogenic zone whereas aftershocks
// do not).  This variation is sometimes thought of as varying the effective
// magnitudes of the seed earthquakes; the definition of "k" shows that varying
// either "a" or "m0" can achieve the same result.  For subsequent generations,
// the fixed parameter "a" value is used.

public class OECatalogParams {

	//----- Parameters -----

	// Productivity parameter.

	public double a;

	// Omori exponent parameter.

	public double p;

	// Omori offset parameter.

	public double c;

	// Gutenberg-Richter parameter.

	public double b;

	// ETAS intensity parameter.

	public double alpha;

	// Reference magnitude, also the minimum considered magnitude, for parameter definition.

	public double mref;

	// Maximum considered magnitude, for parameter definition.

	public double msup;

	// The range of times for which earthquakes are generated, in days.
	// The time origin is not specified by this class.  It could be the epoch
	// (Jan 1, 1970) or it could be the mainshock time.

	public double tbegin;
	public double tend;

	// The time epsilon, in days.
	// This is the minimum considered time between earthquakes.

	public double teps;

	// The minimum magnitude to use for the simulation.

	public double mag_min_sim;

	// The maximum magnitude to use for the simulation.

	public double mag_max_sim;

	// The range of minimum magnitudes to use for the simulation.
	// The minimum magnitude of each generation may be varied within this range
	// to control the number of earthquakes per generation.
	// Should satisfy mag_min_lo <= mag_min_sim <= mag_min_hi.

	public double mag_min_lo;
	public double mag_min_hi;

	// The range of maximum magnitudes to use for the simulation.
	// At present the maximum magnitude is not varied, and so this
	// should satisfy mag_max_lo == mag_max_sim == mag_max_hi.

	public double mag_max_lo;
	public double mag_max_hi;

	// The target generation size.

	public int gen_size_target;

	// The maximum number of generations.

	public int gen_count_max;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		a               = 0.0;
		p               = 0.0;
		c               = 0.0;
		b               = 0.0;
		alpha           = 0.0;
		mref            = 0.0;
		msup            = 0.0;
		tbegin          = 0.0;
		tend            = 0.0;
		teps            = 0.0;
		mag_min_sim     = 0.0;
		mag_max_sim     = 0.0;
		mag_min_lo      = 0.0;
		mag_min_hi      = 0.0;
		mag_max_lo      = 0.0;
		mag_max_hi      = 0.0;
		gen_size_target = 0;
		gen_count_max   = 0;
		return;
	}




	// Default constructor.

	public OECatalogParams () {
		clear();
	}




	// Set all values.

	public OECatalogParams set (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		double mref,
		double msup,
		double tbegin,
		double tend,
		double teps,
		double mag_min_sim,
		double mag_max_sim,
		double mag_min_lo,
		double mag_min_hi,
		double mag_max_lo,
		double mag_max_hi,
		int gen_size_target,
		int gen_count_max
	) {
		this.a               = a;
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = mref;
		this.msup            = msup;
		this.tbegin          = tbegin;
		this.tend            = tend;
		this.teps            = teps;
		this.mag_min_sim     = mag_min_sim;
		this.mag_max_sim     = mag_max_sim;
		this.mag_min_lo      = mag_min_lo;
		this.mag_min_hi      = mag_min_hi;
		this.mag_max_lo      = mag_max_lo;
		this.mag_max_hi      = mag_max_hi;
		this.gen_size_target = gen_size_target;
		this.gen_count_max   = gen_count_max;
		return this;
	}




	// Copy all values from the other object.

	public OECatalogParams copy_from (OECatalogParams other) {
		this.a               = other.a;
		this.p               = other.p;
		this.c               = other.c;
		this.b               = other.b;
		this.alpha           = other.alpha;
		this.mref            = other.mref;
		this.msup            = other.msup;
		this.tbegin          = other.tbegin;
		this.tend            = other.tend;
		this.teps            = other.teps;
		this.mag_min_sim     = other.mag_min_sim;
		this.mag_max_sim     = other.mag_max_sim;
		this.mag_min_lo      = other.mag_min_lo;
		this.mag_min_hi      = other.mag_min_hi;
		this.mag_max_lo      = other.mag_max_lo;
		this.mag_max_hi      = other.mag_max_hi;
		this.gen_size_target = other.gen_size_target;
		this.gen_count_max   = other.gen_count_max;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OECatalogParams:" + "\n");

		result.append ("a = "               + a               + "\n");
		result.append ("p = "               + p               + "\n");
		result.append ("c = "               + c               + "\n");
		result.append ("b = "               + b               + "\n");
		result.append ("alpha = "           + alpha           + "\n");
		result.append ("mref = "            + mref            + "\n");
		result.append ("msup = "            + msup            + "\n");
		result.append ("tbegin = "          + tbegin          + "\n");
		result.append ("tend = "            + tend            + "\n");
		result.append ("teps = "            + teps            + "\n");
		result.append ("mag_min_sim = "     + mag_min_sim     + "\n");
		result.append ("mag_max_sim = "     + mag_max_sim     + "\n");
		result.append ("mag_min_lo = "      + mag_min_lo      + "\n");
		result.append ("mag_min_hi = "      + mag_min_hi      + "\n");
		result.append ("mag_max_lo = "      + mag_max_lo      + "\n");
		result.append ("mag_max_hi = "      + mag_max_hi      + "\n");
		result.append ("gen_size_target = " + gen_size_target + "\n");
		result.append ("gen_count_max = "   + gen_count_max   + "\n");

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 76001;

	private static final String M_VERSION_NAME = "OECatalogParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("a"              , a              );
			writer.marshalDouble ("p"              , p              );
			writer.marshalDouble ("c"              , c              );
			writer.marshalDouble ("b"              , b              );
			writer.marshalDouble ("alpha"          , alpha          );
			writer.marshalDouble ("mref"           , mref           );
			writer.marshalDouble ("msup"           , msup           );
			writer.marshalDouble ("tbegin"         , tbegin         );
			writer.marshalDouble ("tend"           , tend           );
			writer.marshalDouble ("teps"           , teps           );
			writer.marshalDouble ("mag_min_sim"    , mag_min_sim    );
			writer.marshalDouble ("mag_max_sim"    , mag_max_sim    );
			writer.marshalDouble ("mag_min_lo"     , mag_min_lo     );
			writer.marshalDouble ("mag_min_hi"     , mag_min_hi     );
			writer.marshalDouble ("mag_max_lo"     , mag_max_lo     );
			writer.marshalDouble ("mag_max_hi"     , mag_max_hi     );
			writer.marshalInt    ("gen_size_target", gen_size_target);
			writer.marshalInt    ("gen_count_max"  , gen_count_max  );

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

			a               = reader.unmarshalDouble ("a"              );
			p               = reader.unmarshalDouble ("p"              );
			c               = reader.unmarshalDouble ("c"              );
			b               = reader.unmarshalDouble ("b"              );
			alpha           = reader.unmarshalDouble ("alpha"          );
			mref            = reader.unmarshalDouble ("mref"           );
			msup            = reader.unmarshalDouble ("msup"           );
			tbegin          = reader.unmarshalDouble ("tbegin"         );
			tend            = reader.unmarshalDouble ("tend"           );
			teps            = reader.unmarshalDouble ("teps"           );
			mag_min_sim     = reader.unmarshalDouble ("mag_min_sim"    );
			mag_max_sim     = reader.unmarshalDouble ("mag_max_sim"    );
			mag_min_lo      = reader.unmarshalDouble ("mag_min_lo"     );
			mag_min_hi      = reader.unmarshalDouble ("mag_min_hi"     );
			mag_max_lo      = reader.unmarshalDouble ("mag_max_lo"     );
			mag_max_hi      = reader.unmarshalDouble ("mag_max_hi"     );
			gen_size_target = reader.unmarshalInt    ("gen_size_target");
			gen_count_max   = reader.unmarshalInt    ("gen_count_max"  );

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

	public OECatalogParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OECatalogParams catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OECatalogParams static_unmarshal (MarshalReader reader, String name) {
		OECatalogParams catalog = new OECatalogParams();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two catalog parameter structures are identical.
	// Note: This is primarily for testing.

	public boolean check_param_equal (OECatalogParams other) {
		if (
			   this.a               == other.a              
			&& this.p               == other.p              
			&& this.c               == other.c              
			&& this.b               == other.b              
			&& this.alpha           == other.alpha          
			&& this.mref            == other.mref           
			&& this.msup            == other.msup           
			&& this.tbegin          == other.tbegin         
			&& this.tend            == other.tend           
			&& this.teps            == other.teps           
			&& this.mag_min_sim     == other.mag_min_sim    
			&& this.mag_max_sim     == other.mag_max_sim    
			&& this.mag_min_lo      == other.mag_min_lo     
			&& this.mag_min_hi      == other.mag_min_hi     
			&& this.mag_max_lo      == other.mag_max_lo     
			&& this.mag_max_hi      == other.mag_max_hi     
			&& this.gen_size_target == other.gen_size_target
			&& this.gen_count_max   == other.gen_count_max  
		) {
			return true;
		}
		return false;
	}




	// Set to plausible random values.
	// Note: Not all values are actually randomized.
	// Note: This is primarily for testing.

	public OECatalogParams set_to_random (OERandomGenerator rangen) {
		this.a               = rangen.uniform_sample (-5.0, 2.0);
		this.p               = rangen.uniform_sample (0.8, 1.2);
		this.c               = rangen.uniform_sample (0.001, 0.02);
		this.b               = rangen.uniform_sample (0.8, 1.2);
		this.alpha           = rangen.uniform_sample (0.8, 1.2);
		this.mref            = 3.0;
		this.msup            = 9.5;
		this.tbegin          = 1.0;
		this.tend            = 366.0;
		this.teps            = 0.00001;
		this.mag_min_sim     = 3.0;
		this.mag_max_sim     = 9.5;
		this.mag_min_lo      = 2.0;
		this.mag_min_hi      = 6.0;
		this.mag_max_lo      = 9.5;
		this.mag_max_hi      = 9.5;
		this.gen_size_target = rangen.uniform_int_sample (200, 500);
		this.gen_count_max   = rangen.uniform_int_sample (50, 150);
		return this;
	}




	// Set to typical values, with some user-adjustable parameters.
	// Note: This is primarily for testing.

	public OECatalogParams set_to_typical (
		double a,
		double p,
		double c,
		double b,
		double alpha,
		int gen_size_target,
		int gen_count_max
	) {
		this.a               = a;
		this.p               = p;
		this.c               = c;
		this.b               = b;
		this.alpha           = alpha;
		this.mref            = 3.0;
		this.msup            = 9.5;
		this.tbegin          = 1.0;
		this.tend            = 366.0;
		this.teps            = 0.00001;
		this.mag_min_sim     = 3.0;
		this.mag_max_sim     = 9.5;
		this.mag_min_lo      = 2.0;
		this.mag_min_hi      = 6.0;
		this.mag_max_lo      = 9.5;
		this.mag_max_hi      = 9.5;
		this.gen_size_target = gen_size_target;
		this.gen_count_max   = gen_count_max;
		return this;
	}

}
