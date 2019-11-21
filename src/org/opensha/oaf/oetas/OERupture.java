package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;


// Class to hold one earthquake rupture in an Operational ETAS catalog.
// Author: Michael Barall 11/03/2019.

public class OERupture {

	//----- Contents -----

	// Rupture time.
	// This is measured in days from an unspecified origin; typically the mainshock time.

	public double t_day;

	// Rupture magnitude.

	public double rup_mag;

	// Productivity "k" value.
	// The rate of direct aftershocks of this earthquake, per unit time,
	// per unit magnitude, is:
	//   lambda(t, m) = k * b * log(10) * (10^(-b*(m - mref))) * ((t-t0+c)^(-p))
	// where b is the Gutenberg-Richter parameters, mref is the reference magnitude,
	// p and c are the Omori parameters, and t0 is the time of this earthquake.
	// The k value subsumes the productivity parameter a, the ETAS parameter alpha,
	// the magnitude of this earthquake, and the correction for the magnitude range
	// from which this earthquake is drawn.  For the first generation, k also
	// subsumes the mainshock productivity ams, and any correction for magnitude
	// of completeness.

	public double k_prod;

	// Rupture location.
	// This is measured in a planar (x,y) coordinate system, in kilometers.
	// The origin and map projection are unspecified; typically an azimuthal equidistant
	// projection (commonly called a polar projection) centered at the mainshock location.
	// For temporal ETAS, these are set to zero.

	public double x_km;
	public double y_km;




	//----- Construction -----




	// Clear to default values.

	public void clear () {
		t_day = 0.0;
		rup_mag = 0.0;
		k_prod = 0.0;
		x_km = 0.0;
		y_km = 0.0;
		return;
	}




	// Default constructor.

	public OERupture () {
		clear();
	}




	// Set the values, for temporal ETAS.

	public void set (double t_day, double rup_mag, double k_prod) {
		this.t_day = t_day;
		this.rup_mag = rup_mag;
		this.k_prod = k_prod;
		x_km = 0.0;
		y_km = 0.0;
		return;
	}




	// Set the values, for spatial ETAS.

	public void set (double t_day, double rup_mag, double k_prod, double x_km, double y_km) {
		this.t_day = t_day;
		this.rup_mag = rup_mag;
		this.k_prod = k_prod;
		this.x_km = x_km;
		this.y_km = y_km;
		return;
	}




	//----- Marshaling -----

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		writer.marshalDouble ("t_day", t_day);
		writer.marshalDouble ("rup_mag", rup_mag);
		writer.marshalDouble ("k_prod", k_prod);
		writer.marshalDouble ("x_km", x_km);
		writer.marshalDouble ("y_km", y_km);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OERupture unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		t_day = reader.unmarshalDouble ("t_day");
		rup_mag = reader.unmarshalDouble ("rup_mag");
		k_prod = reader.unmarshalDouble ("k_prod");
		x_km = reader.unmarshalDouble ("x_km");
		y_km = reader.unmarshalDouble ("y_km");
		reader.unmarshalMapEnd ();
		return this;
	}

}
