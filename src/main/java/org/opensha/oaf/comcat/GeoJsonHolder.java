package org.opensha.oaf.comcat;

import org.json.simple.JSONObject;


// Holder for a GeoJSON object.
// Author: Michael Barall 10/15/2022.
//
// Holds a GeoJSON object, and a flag indicating if it comes from
// Comcat-production (flag == true) or Comcat-development (flag == false).

public class GeoJsonHolder {

	//----- Contents -----

	// The GeoJSON object, or null if none.

	public JSONObject geojson;

	// True if geojson was retrieved from Comcat-production, false if it was retrieved from Comcat-development.

	public boolean f_gj_prod;


	//----- Construction -----


	// Construct a holder with null contents.

	public GeoJsonHolder () {
		geojson = null;
		f_gj_prod = true;
	}


	// Construct a holder with the given contents.

	public GeoJsonHolder (JSONObject the_geojson, boolean the_f_gj_prod) {
		geojson = the_geojson;
		f_gj_prod = the_f_gj_prod;
	}


	// Clear the holder to null contents.

	public void clear () {
		geojson = null;
		f_gj_prod = true;
		return;
	}


	// Set the holder to the given contents.

	public void set (JSONObject the_geojson, boolean the_f_gj_prod) {
		geojson = the_geojson;
		f_gj_prod = the_f_gj_prod;
		return;
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("GeoJsonHolder : Missing subcommand");
			return;
		}




		// Unrecognized subcommand.

		System.err.println ("GeoJsonHolder : Unrecognized subcommand : " + args[0]);
		return;

	}

}
