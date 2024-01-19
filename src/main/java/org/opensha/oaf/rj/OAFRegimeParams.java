package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalException;


// Return value for region-dependent parameters.
// Author: Michael Barall 01/15/2024
//
// This class holds a set of parameters, and the tectonic regime to which they apply.
// Type T is the class that holds parameter values.

public class OAFRegimeParams<T> {

	//----- Values -----

	// The tectonic regime.
	// If null, it means that the configuration file contains no information
	// for the specified location.
	// If null, then params is guarenteed to also be null.

	public OAFTectonicRegime regime;

	// The parameters.
	// If null, then no parametera are available.
	// If null, and regime is non-null, it means the configuration file supplies
	// an explicitly null parameter object.

	public T params;


	//----- Construction -----

	// Make an object with both fields null.

	public OAFRegimeParams () {
		regime = null;
		params = null;
	}

	// Make an object with specified regime and parameters.
	// Note: This object retains the parameters object.

	public OAFRegimeParams (OAFTectonicRegime the_regime, T the_params) {
		if (the_regime == null && the_params != null) {
			throw new IllegalArgumentException ("OAFRegimeParams: Got null regime with non-null parameters");
		}
		regime = the_regime;
		params = the_params;
	}


	//----- Functions -----

	// Return true if this object has a non-null regime.

	public final boolean has_regime () {
		return regime != null;
	}

	// Throw an exception if this object has a null regime.
	// Returns this object.

	public final OAFRegimeParams<T> require_regime () {
		if (regime == null) {
			throw new MarshalException ("OAFRegimeParams: No parameters supplied for given location or regime");
		}
		return this;
	}

	// Return true if this object has a non-null parameters.

	public final boolean has_params () {
		return params != null;
	}

	// Throw an exception if this object has null parameters.
	// Returns this object.

	public final OAFRegimeParams<T> require_params () {
		if (params == null) {
			if (regime == null) {
				throw new MarshalException ("OAFRegimeParams: Null parameters supplied for given location or regime");
			}
			throw new MarshalException ("OAFRegimeParams: Null parameters supplied for regime: " + regime.toString());
		}
		return this;
	}

}
