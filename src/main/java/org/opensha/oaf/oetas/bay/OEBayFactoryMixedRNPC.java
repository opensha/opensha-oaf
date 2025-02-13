package org.opensha.oaf.oetas.bay;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.rj.OAFRegimeParams;

import org.opensha.commons.geo.Location;


// Bayesian prior factory function, for operational ETAS - Mixed distribution in relative-ams, n, p, and c.
// Author: Michael Barall.
//
// This factory function supplies a Bayesian prior density, which combines a triangular
// distribution with exponential tails on relative-ams, a skew normal distribution
// on n, a Cauchy distribution on p, and a normal distribution on c.
// Parameters and formulas are given in OEMixedRNPCParams, and locatoin-dependent
// parameters are available from a configuration file through OEMixedRNPCConfig.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied value elements.

public class OEBayFactoryMixedRNPC extends OEBayFactory {

	//----- Parameters -----

	// Creation mode.

	private int cmode;

	private static final int CMODE_MAINLOC = 1;			// use mainshock location, or default if not available
	private static final int CMODE_DEFAULT = 2;			// use default parameters
	private static final int CMODE_REGIME = 3;			// use specified regime, or default if not found
	private static final int CMODE_LOC = 4;				// use specified location
	private static final int CMODE_PARAM = 5;			// use specified parameters

	// Parameter probability distribution parameters, or null if not needed for the creation mode.

	private OEMixedRNPCParams ppdist_params;

	// Location, or null if not needed for the creation mode.

	private Location loc;

	// Regime name, if needed for the creation mode.
	// An empty string selects default parameters.

	private String regime;




	//----- Creation -----


	// Create a Bayesian prior function.
	// Parameters:
	//  factory_params = Parameters passed in to the Bayesian prior factory.
	// Returns the Bayesian prior function.
	// Threading: This function may be called simultaneously by multiple threads
	// (although in practice it is called by a single thread at the start of a calculation;
	// the resulting Bayesian prior function can then be used by multiple threads.)

	@Override
	public OEBayPrior make_bay_prior (
		OEBayFactoryParams factory_params
	) {
		OEMixedRNPCParams result_params = null;

		// Switch according to creation mode

		switch (cmode) {

		default:
			throw new IllegalStateException ("OEBayFactoryMixedRNPC.make_bay_prior: Illegal creation mode: cmode = " + cmode);

		case CMODE_MAINLOC: {

			// Use mainshock location, or default if Unknown

			if (factory_params.has_loc_main()) {
				result_params = (new OEMixedRNPCConfig()).get_params(factory_params.get_loc_main()).params;
			} else {
				result_params = (new OEMixedRNPCConfig()).get_default_params().params;
			}
		}
		break;

		case CMODE_DEFAULT: {

			// Use default parameters

			result_params = (new OEMixedRNPCConfig()).get_default_params().params;
		}
		break;

		case CMODE_REGIME: {

			// Use parameters for the given regime, or default if the regime is not found

			result_params = (new OEMixedRNPCConfig()).get_params_or_default(regime).params;
		}
		break;

		case CMODE_LOC: {

			// Use parameters for the given location

			result_params = (new OEMixedRNPCConfig()).get_params(loc).params;
		}
		break;

		case CMODE_PARAM: {

			// Use the explicitly given parameters

			result_params = ppdist_params;
		}
		break;

		}

		// Return a prior containing a copy of the parameters

		return new OEBayPriorMixedRNPC ((new OEMixedRNPCParams()).copy_from (result_params));	// even though the OEBayPriorMixedRNPC constructor does a copy
		//return new OEBayPriorMixedRNPC (result_params);
	}




	//----- Construction -----




	// Default constructor sets up for using mainshock location.

	public OEBayFactoryMixedRNPC () {
		this.cmode = CMODE_MAINLOC;
		this.ppdist_params = null;
		this.loc = null;
		this.regime = null;
	}



	// Construct for a given regime.
	// A null or empty argument selects default parameters.
	// Due to overloading, a null argument must be explicitly a String (or use "" instead).

	public OEBayFactoryMixedRNPC (String the_regime) {
		if (the_regime == null || the_regime.trim().isEmpty()) {
			this.cmode = CMODE_DEFAULT;
			this.ppdist_params = null;
			this.loc = null;
			this.regime = null;
		} else {
			this.cmode = CMODE_REGIME;
			this.ppdist_params = null;
			this.loc = null;
			this.regime = the_regime;
		}
	}



	// Construct for a given location.
	// A null argument selects default parameters.
	// Due to overloading, a null argument must be explicitly a Location.

	public OEBayFactoryMixedRNPC (Location the_loc) {
		if (the_loc == null) {
			this.cmode = CMODE_DEFAULT;
			this.ppdist_params = null;
			this.loc = null;
			this.regime = null;
		} else {
			this.cmode = CMODE_LOC;
			this.ppdist_params = null;
			this.loc = the_loc;
			this.regime = null;
		}
	}



	// Construct for given parameters.
	// A null argument selects default parameters.
	// Due to overloading, a null argument must be explicitly a OEMixedRNPCParams.

	public OEBayFactoryMixedRNPC (OEMixedRNPCParams the_params) {
		if (the_params == null) {
			this.cmode = CMODE_DEFAULT;
			this.ppdist_params = null;
			this.loc = null;
			this.regime = null;
		} else {
			this.cmode = CMODE_PARAM;
			this.ppdist_params = (new OEMixedRNPCParams()).copy_from (the_params);
			this.loc = null;
			this.regime = null;
		}
	}




	// Display our contents

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEBayFactoryMixedRNPC:" + "\n");
		result.append ("cmode = " + cmode + "\n");

		// Switch according to creation mode

		switch (cmode) {

		default:
			throw new IllegalStateException ("OEBayFactoryMixedRNPC.toString: Illegal creation mode: cmode = " + cmode);

		case CMODE_MAINLOC: {
		}
		break;

		case CMODE_DEFAULT: {
		}
		break;

		case CMODE_REGIME: {
			result.append ("regime = " + regime + "\n");
		}
		break;

		case CMODE_LOC: {
			result.append ("loc.lat = " + loc.getLatitude() + "\n");
			result.append ("loc.lon = " + loc.getLongitude() + "\n");
			result.append ("loc.depth = " + loc.getDepth() + "\n");
		}
		break;

		case CMODE_PARAM: {
			result.append ("ppdist_params = " + ppdist_params.to_string_2() + "\n");
		}
		break;

		}

		return result.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_HWV_1 = 1;		// human-writeable version, use mainshock location, or default if not available
	private static final int MARSHAL_HWV_2 = 2;		// human-writeable version, use default parameters
	private static final int MARSHAL_HWV_3 = 3;		// human-writeable version, use specified regime, or default if not found
	private static final int MARSHAL_HWV_4 = 4;		// human-writeable version, use specified location
	private static final int MARSHAL_VER_1 = 148001;	// use mainshock location, or default if not available
	private static final int MARSHAL_VER_2 = 148002;	// use default parameters
	private static final int MARSHAL_VER_3 = 148003;	// use specified regime, or default if not found
	private static final int MARSHAL_VER_4 = 148004;	// use specified location
	private static final int MARSHAL_VER_5 = 148005;	// use specified parameters

	private static final String M_VERSION_NAME = "OEBayFactoryMixedRNPC";

	// Get the type code.

	@Override
	protected int get_marshal_type () {
		return MARSHAL_MIXED_RNPC;
	}

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver;

		switch (cmode) {

		default:
			throw new IllegalStateException ("OEBayFactoryMixedRNPC.do_marshal: Illegal creation mode: cmode = " + cmode);

		case CMODE_MAINLOC: {
			ver = MARSHAL_VER_1;
		}
		break;

		case CMODE_DEFAULT: {
			ver = MARSHAL_VER_2;
		}
		break;

		case CMODE_REGIME: {
			ver = MARSHAL_VER_3;
		}
		break;

		case CMODE_LOC: {
			ver = MARSHAL_VER_4;
		}
		break;

		case CMODE_PARAM: {
			ver = MARSHAL_VER_5;
		}
		break;

		}

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			// <None>

		}
		break;

		case MARSHAL_VER_2: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			// <None>

		}
		break;

		case MARSHAL_VER_3: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			writer.marshalString ("regime", regime);

		}
		break;

		case MARSHAL_VER_4: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			double depth = loc.getDepth();

			writer.marshalDouble ("lat", lat);
			writer.marshalDouble ("lon", lon);
			writer.marshalDouble ("depth", depth);

		}
		break;

		case MARSHAL_VER_5: {

			// Superclass

			super.do_marshal (writer);

			// Contents

			OEMixedRNPCParams.static_marshal (writer, "ppdist_params", ppdist_params);

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME);

		switch (ver) {

		default:
			throw new MarshalException ("OEBayFactoryMixedRNPC.do_umarshal: Unknown version code: version = " + ver);
		
		// Human-writeable version

		case MARSHAL_HWV_1: {

			// Get parameters

			cmode = CMODE_MAINLOC;

			ppdist_params = null;
			loc = null;
			regime = null;

		}
		break;

		case MARSHAL_HWV_2: {

			// Get parameters

			cmode = CMODE_DEFAULT;

			ppdist_params = null;
			loc = null;
			regime = null;

		}
		break;

		case MARSHAL_HWV_3: {

			// Get parameters

			cmode = CMODE_REGIME;

			ppdist_params = null;
			loc = null;
			regime = reader.unmarshalString ("regime");

		}
		break;

		case MARSHAL_HWV_4: {

			// Get parameters

			cmode = CMODE_LOC;

			ppdist_params = null;

			double lat = reader.unmarshalDouble ("lat");
			double lon = reader.unmarshalDouble ("lon");
			double depth = reader.unmarshalDouble ("depth");
			try {
				loc = new Location (lat, lon, depth);
			}
			catch (Exception e) {
				throw new MarshalException ("OEBayFactoryMixedRNPC.do_umarshal: Invalid location: lat = " + lat + ", lon = " + lon + ", depth = " + depth, e);
			}

			regime = null;

		}
		break;

		// Machine-written version

		case MARSHAL_VER_1: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cmode = CMODE_MAINLOC;

			ppdist_params = null;
			loc = null;
			regime = null;

		}
		break;

		case MARSHAL_VER_2: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cmode = CMODE_DEFAULT;

			ppdist_params = null;
			loc = null;
			regime = null;

		}
		break;

		case MARSHAL_VER_3: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cmode = CMODE_REGIME;

			ppdist_params = null;
			loc = null;
			regime = reader.unmarshalString ("regime");

		}
		break;

		case MARSHAL_VER_4: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cmode = CMODE_LOC;

			ppdist_params = null;

			double lat = reader.unmarshalDouble ("lat");
			double lon = reader.unmarshalDouble ("lon");
			double depth = reader.unmarshalDouble ("depth");
			try {
				loc = new Location (lat, lon, depth);
			}
			catch (Exception e) {
				throw new MarshalException ("OEBayFactoryMixedRNPC.do_umarshal: Invalid location: lat = " + lat + ", lon = " + lon + ", depth = " + depth, e);
			}

			regime = null;

		}
		break;

		case MARSHAL_VER_5: {

			// Superclass

			super.do_umarshal (reader);

			// Contents

			cmode = CMODE_PARAM;

			ppdist_params = OEMixedRNPCParams.static_unmarshal (reader, "ppdist_params");
			loc = null;
			regime = null;

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
	public OEBayFactoryMixedRNPC unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayFactoryMixedRNPC obj) {

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

	public static OEBayFactoryMixedRNPC unmarshal_poly (MarshalReader reader, String name) {
		OEBayFactoryMixedRNPC result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayFactoryMixedRNPC.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_MIXED_RNPC:
			result = new OEBayFactoryMixedRNPC();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}

}
