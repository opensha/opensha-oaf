package org.opensha.oaf.oetas.bay;

import java.util.List;

import org.opensha.oaf.oetas.util.OEValueElement;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.Marshalable;
import org.opensha.oaf.util.MarshalUtils;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.TestArgs;

import org.opensha.oaf.rj.OAFParameterSet;

import org.opensha.commons.geo.Location;


// Bayesian prior factory function, for operational ETAS.
// Author: Michael Barall 08/26/2024.
//
// This abstract class represents a Bayesian prior factory function.
//
// Objects of this class, and its subclasses, are immutable and stateless.
// They are pure functions, which means that their outputs depend only on the
// supplied parameters.

public abstract class OEBayFactory implements Marshalable {


	//----- Creation -----


	// Create a Bayesian prior function.
	// Parameters:
	//  factory_params = Parameters passed in to the Bayesian prior factory.
	// Returns the Bayesian prior function.
	// Threading: This function may be called simultaneously by multiple threads
	// (although in practice it is called by a single thread at the start of a calculation;
	// the resulting Bayesian prior function can then be used by multiple threads.)

	public abstract OEBayPrior make_bay_prior (
		OEBayFactoryParams factory_params
	);




	//----- Construction -----


	// Default constructor does nothing.

	public OEBayFactory () {}


	// Display our contents

	@Override
	public String toString() {
		return "OEBayFactory";
	}




	//----- Factory methods -----


	// Construct a factory that returns a function with constant density.

	public static OEBayFactory makeUniform () {
		return new OEBayFactoryUniform ();
	}


	// Construct a factory that returns a fixed function.

	public static OEBayFactory makeFixed (OEBayPrior bay_prior) {
		return new OEBayFactoryFixed (bay_prior);
	}


	// Construct a factory for a Gauss a/p/c prior, using mainshock location to select parameters.

	public static OEBayFactory makeGaussAPC () {
		return new OEBayFactoryGaussAPC ();
	}

	// Construct a factory for a Gauss a/p/c prior, using parameters for the given regime.
	// Default parameters are used if the regime is null or blank, or if it is not found.
	// Due to overloading, a null argument must be explicitly a String (or use "" instead).

	public static OEBayFactory makeGaussAPC (String the_regime) {
		return new OEBayFactoryGaussAPC (the_regime);
	}

	// Construct a factory for a Gauss a/p/c prior, using the given location to select parameters.
	// Default parameters are used if the location is null.
	// Due to overloading, a null argument must be explicitly a Location.

	public static OEBayFactory makeGaussAPC (Location the_loc) {
		return new OEBayFactoryGaussAPC (the_loc);
	}

	// Construct a factory for a Gauss a/p/c prior, using the given parameters.
	// Default parameters are used if the parameter object is null.
	// Due to overloading, a null argument must be explicitly a OEGaussAPCParams.

	public static OEBayFactory makeGaussAPC (OEGaussAPCParams the_params) {
		return new OEBayFactoryGaussAPC (the_params);
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 136001;

	private static final String M_VERSION_NAME = "OEBayFactory";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 136000;
	protected static final int MARSHAL_UNIFORM = 137001;
	protected static final int MARSHAL_FIXED = 138001;
	protected static final int MARSHAL_GAUSSIAN_APC = 139001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected abstract int get_marshal_type ();

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Contents

		// <None>
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		// <None>

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
	public OEBayFactory unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, OEBayFactory obj) {

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

	public static OEBayFactory unmarshal_poly (MarshalReader reader, String name) {
		OEBayFactory result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("OEBayFactory.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_UNIFORM:
			result = new OEBayFactoryUniform();
			result.do_umarshal (reader);
			break;

		case MARSHAL_FIXED:
			result = new OEBayFactoryFixed();
			result.do_umarshal (reader);
			break;

		case MARSHAL_GAUSSIAN_APC:
			result = new OEBayFactoryGaussAPC();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}




	//----- Testing -----




	// A marshalable test object that holds a Bayesian factory.

	private static class FactoryHolder implements Marshalable {

		public OEBayFactory bay_factory = null;

		@Override
		public void marshal (MarshalWriter writer, String name) {
			writer.marshalMapBegin (name);
			OEBayFactory.marshal_poly (writer, "bay_factory", bay_factory);
			writer.marshalMapEnd ();
			return;
		}

		@Override
		public FactoryHolder unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);
			bay_factory = OEBayFactory.unmarshal_poly (reader, "bay_factory");
			reader.unmarshalMapEnd ();
			return this;
		}

		@Override
		public String toString () {
			if (bay_factory == null) {
				return "bay_factory = <null>";
			}
			return "bay_factory = " + bay_factory.toString();
		}

	}




	// A marshalable test object that holds a Bayesian prior.

	private static class PriorHolder implements Marshalable {

		public OEBayPrior bay_prior = null;

		@Override
		public void marshal (MarshalWriter writer, String name) {
			writer.marshalMapBegin (name);
			OEBayPrior.marshal_poly (writer, "bay_prior", bay_prior);
			writer.marshalMapEnd ();
			return;
		}

		@Override
		public PriorHolder unmarshal (MarshalReader reader, String name) {
			reader.unmarshalMapBegin (name);
			bay_prior = OEBayPrior.unmarshal_poly (reader, "bay_prior");
			reader.unmarshalMapEnd ();
			return this;
		}

		@Override
		public String toString () {
			if (bay_prior == null) {
				return "bay_prior = <null>";
			}
			return "bay_prior = " + bay_prior.toString();
		}

	}




	// Perform tests on a Bayesian factory, using unknown magnitude and location.

	private static void test_factory_unknown_loc (OEBayFactory the_bay_factory) {
		FactoryHolder factory_holder = new FactoryHolder();
		FactoryHolder factory_holder_2 = new FactoryHolder();
		PriorHolder prior_holder = new PriorHolder();
		PriorHolder prior_holder_2 = new PriorHolder();

		factory_holder.bay_factory = the_bay_factory;

		// Display factory

		System.out.println ();
		System.out.println ("********** Factory **********");
		System.out.println ();

		System.out.println (factory_holder.toString());

		// Marshal to JSON

		System.out.println ();
		System.out.println ("********** Marshal factory **********");
		System.out.println ();

		//String json_string = MarshalUtils.to_json_string (factory_holder);
		//System.out.println (MarshalUtils.display_json_string (json_string));

		String json_string = MarshalUtils.to_formatted_compact_json_string (factory_holder);
		System.out.println (json_string);

		// Unmarshal from JSON

		System.out.println ();
		System.out.println ("********** Unmarshal factory **********");
		System.out.println ();
			
		MarshalUtils.from_json_string (factory_holder_2, json_string);

		System.out.println (factory_holder_2.toString());

		// Make factory parameters

		System.out.println ();
		System.out.println ("********** Factory parameters **********");
		System.out.println ();

		OEBayFactoryParams factory_params = new OEBayFactoryParams();
		System.out.println (factory_params.toString());

		// Make prior

		System.out.println ();
		System.out.println ("********** Prior **********");
		System.out.println ();

		prior_holder.bay_prior = factory_holder.bay_factory.make_bay_prior (factory_params);
		System.out.println (prior_holder.toString());

		// Marshal to JSON

		System.out.println ();
		System.out.println ("********** Marshal prior **********");
		System.out.println ();

		//String json_string = MarshalUtils.to_json_string (prior_holder);
		//System.out.println (MarshalUtils.display_json_string (json_string));

		String json_string_2 = MarshalUtils.to_formatted_compact_json_string (prior_holder);
		System.out.println (json_string_2);

		// Unmarshal from JSON

		System.out.println ();
		System.out.println ("********** Unmarshal prior **********");
		System.out.println ();
			
		MarshalUtils.from_json_string (prior_holder_2, json_string_2);

		System.out.println (prior_holder_2.toString());

		return;
	}




	// Perform tests on a Bayesian factory, using unknown magnitude and known location.

	private static void test_factory_known_loc (OEBayFactory the_bay_factory, Location the_loc) {
		FactoryHolder factory_holder = new FactoryHolder();
		FactoryHolder factory_holder_2 = new FactoryHolder();
		PriorHolder prior_holder = new PriorHolder();
		PriorHolder prior_holder_2 = new PriorHolder();

		factory_holder.bay_factory = the_bay_factory;

		// Display factory

		System.out.println ();
		System.out.println ("********** Factory **********");
		System.out.println ();

		System.out.println (factory_holder.toString());

		// Marshal to JSON

		System.out.println ();
		System.out.println ("********** Marshal factory **********");
		System.out.println ();

		//String json_string = MarshalUtils.to_json_string (factory_holder);
		//System.out.println (MarshalUtils.display_json_string (json_string));

		String json_string = MarshalUtils.to_formatted_compact_json_string (factory_holder);
		System.out.println (json_string);

		// Unmarshal from JSON

		System.out.println ();
		System.out.println ("********** Unmarshal factory **********");
		System.out.println ();
			
		MarshalUtils.from_json_string (factory_holder_2, json_string);

		System.out.println (factory_holder_2.toString());

		// Make factory parameters

		System.out.println ();
		System.out.println ("********** Factory parameters **********");
		System.out.println ();

		OEBayFactoryParams factory_params = new OEBayFactoryParams (OEBayFactoryParams.unknown_mag(), the_loc);
		System.out.println (factory_params.toString());

		// Make prior

		System.out.println ();
		System.out.println ("********** Prior **********");
		System.out.println ();

		prior_holder.bay_prior = factory_holder.bay_factory.make_bay_prior (factory_params);
		System.out.println (prior_holder.toString());

		// Marshal to JSON

		System.out.println ();
		System.out.println ("********** Marshal prior **********");
		System.out.println ();

		//String json_string = MarshalUtils.to_json_string (prior_holder);
		//System.out.println (MarshalUtils.display_json_string (json_string));

		String json_string_2 = MarshalUtils.to_formatted_compact_json_string (prior_holder);
		System.out.println (json_string_2);

		// Unmarshal from JSON

		System.out.println ();
		System.out.println ("********** Unmarshal prior **********");
		System.out.println ();
			
		MarshalUtils.from_json_string (prior_holder_2, json_string_2);

		System.out.println (prior_holder_2.toString());

		return;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "TestArgs");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Test the uniform prior factory.

		if (testargs.is_test ("test1")) {

			// Zero additional argument

			testargs.end_test();

			// Factory to test

			OEBayFactory bay_factory = OEBayFactory.makeUniform();

			// Test it

			test_factory_unknown_loc (bay_factory);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2
		// Test the fixed prior factory, containing a uniform prior.

		if (testargs.is_test ("test2")) {

			// Zero additional argument

			testargs.end_test();

			// Factory to test

			OEBayFactory bay_factory = OEBayFactory.makeFixed (OEBayPrior.makeUniform());

			// Test it

			test_factory_unknown_loc (bay_factory);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3
		// Test the Gauss a/p/c factory, for the CALIFORNIA regime.

		if (testargs.is_test ("test3")) {

			// Zero additional argument

			testargs.end_test();

			// Factory to test

			OEBayFactory bay_factory = OEBayFactory.makeGaussAPC ("CALIFORNIA");

			// Test it

			test_factory_unknown_loc (bay_factory);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4
		// Test the Gauss a/p/c factory, for given parameters.

		if (testargs.is_test ("test4")) {

			// Zero additional argument

			testargs.end_test();

			// Factory to test

			OEBayFactory bay_factory = OEBayFactory.makeGaussAPC ( (new OEGaussAPCConfig()).get_default_params().params );

			// Test it

			test_factory_unknown_loc (bay_factory);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #5
		// Command format:
		//  test5
		// Test the Gauss a/p/c factory, for mainshock location, but unknown.

		if (testargs.is_test ("test5")) {

			// Zero additional argument

			testargs.end_test();

			// Factory to test

			OEBayFactory bay_factory = OEBayFactory.makeGaussAPC ();

			// Test it

			test_factory_unknown_loc (bay_factory);

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #6
		// Command format:
		//  test6
		// Test the Gauss a/p/c factory, for mainshock location, at a list of locations.

		if (testargs.is_test ("test6")) {

			// Zero additional argument

			testargs.end_test();

			// Factory to test

			OEBayFactory bay_factory = OEBayFactory.makeGaussAPC ();

			// List of locations

			List<Location> my_loc_list = OAFParameterSet.getTestLocations();
			for (Location loc : my_loc_list) {
				System.out.println ();
				System.out.println ("********** Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
				System.out.println ();

				// Test factory

				test_factory_known_loc (bay_factory, loc);
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7
		// Test the Gauss a/p/c factory, for specified location, at a list of locations.

		if (testargs.is_test ("test7")) {

			// Zero additional argument

			testargs.end_test();

			// List of locations

			List<Location> my_loc_list = OAFParameterSet.getTestLocations();
			for (Location loc : my_loc_list) {
				System.out.println ();
				System.out.println ("********** Query location : lat = " + loc.getLatitude() + ", lon = " + loc.getLongitude() + ", depth = " + loc.getDepth());
				System.out.println ();

				// Factory to test

				OEBayFactory bay_factory = OEBayFactory.makeGaussAPC (loc);

				// Test factory

				test_factory_unknown_loc (bay_factory);
			}

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
