package org.opensha.oaf.oetas;

import java.util.Comparator;
import java.util.Collection;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import static org.opensha.oaf.util.SimpleUtils.rndd;


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
	// Note: When used in rupture history, this field can contain magnitude of completeness.

	public double k_prod;

	// Parent rupture number.
	// The index number of the parent rupture, within the prior generation.
	// The value -1 indicates no parent.
	// Note: When used in rupture history, this field can contain rupture category.
	// Note: This field is now considered optional, which a generator may or may not produce.

	public int rup_parent;

	// Rupture location.
	// This is measured in a planar (x,y) coordinate system, in kilometers.
	// The origin and map projection are unspecified; typically an azimuthal equidistant
	// projection (commonly called a polar projection) centered at the mainshock location.
	// For temporal ETAS, these are set to zero.

	public double x_km;
	public double y_km;




	//----- Constants -----




	// Special values that can be used in the rup_parent field (all are negative).

	public static final int RUPPAR_SEED = -1;		// Rupture belongs to seed generation.
	public static final int RUPPAR_UNKNOWN = -2;	// Parent rupture is unknown.
	public static final int RUPPAR_DELETED = -3;	// Rupture is deleted from catalog.




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		t_day      = 0.0;
		rup_mag    = 0.0;
		k_prod     = 0.0;
		rup_parent = RUPPAR_SEED;
		x_km       = 0.0;
		y_km       = 0.0;
		return;
	}




	// Default constructor.

	public OERupture () {
		clear();
	}




	// Set the values, for temporal ETAS.
	// Return this object.

	public final OERupture set (double t_day, double rup_mag, double k_prod, int rup_parent) {
		this.t_day      = t_day;
		this.rup_mag    = rup_mag;
		this.k_prod     = k_prod;
		this.rup_parent = rup_parent;
		this.x_km       = 0.0;
		this.y_km       = 0.0;
		return this;
	}




	// Set the values, for spatial ETAS.
	// Return this object.

	public final OERupture set (double t_day, double rup_mag, double k_prod, int rup_parent, double x_km, double y_km) {
		this.t_day      = t_day;
		this.rup_mag    = rup_mag;
		this.k_prod     = k_prod;
		this.rup_parent = rup_parent;
		this.x_km       = x_km;
		this.y_km       = y_km;
		return this;
	}




	// Set the values, time and magnitude only.
	// Return this object.

	public final OERupture set (double t_day, double rup_mag) {
		this.t_day      = t_day;
		this.rup_mag    = rup_mag;
		this.k_prod     = 0.0;
		this.rup_parent = RUPPAR_SEED;
		this.x_km       = 0.0;
		this.y_km       = 0.0;
		return this;
	}




	// Set the values, for an object that represents a background rate.
	// Return this object.
	// Note: A background rate object has t_day == OEConstants.BKGD_TIME_DAYS
	// and k_prod == mu where mu is the background rate.  Then the time-independent
	// rate of background earthquakes, per unit time, per unit magnitude, is
	//   lambda(m) = mu * b * log(10) * (10^(-b*(m - mref)))

	public final OERupture set_background (double mu) {
		this.t_day      = OEConstants.BKGD_TIME_DAYS;
		this.rup_mag    = 0.0;
		this.k_prod     = mu;
		this.rup_parent = RUPPAR_SEED;
		this.x_km       = 0.0;
		this.y_km       = 0.0;
		return this;
	}




	// Return true if this object represents a background rate.

	public final boolean is_background () {
		return t_day <= OEConstants.BKGD_TIME_DAYS_CHECK;
	}




	// Copy the values from the other object.
	// Return this object.

	public final OERupture copy_from (OERupture other) {
		this.t_day      = other.t_day;
		this.rup_mag    = other.rup_mag;
		this.k_prod     = other.k_prod;
		this.rup_parent = other.rup_parent;
		this.x_km       = other.x_km;
		this.y_km       = other.y_km;
		return this;
	}




	// Set the values for a seed rupture, time, magnitude, and productivity only.
	// Return this object.

	public final OERupture set_seed (double t_day, double rup_mag, double k_prod) {
		this.t_day      = t_day;
		this.rup_mag    = rup_mag;
		this.k_prod     = k_prod;
		this.rup_parent = RUPPAR_SEED;
		this.x_km       = 0.0;
		this.y_km       = 0.0;
		return this;
	}




	// Set the values for a seed rupture, time and productivity only.
	// Return this object.

	public final OERupture set_seed (double t_day, double k_prod) {
		this.t_day      = t_day;
		this.rup_mag    = 0.0;
		this.k_prod     = k_prod;
		this.rup_parent = RUPPAR_SEED;
		this.x_km       = 0.0;
		this.y_km       = 0.0;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OERupture:" + "\n");

		result.append ("t_day = " + t_day + "\n");
		result.append ("rup_mag = " + rup_mag + "\n");
		result.append ("k_prod = " + k_prod + "\n");
		result.append ("rup_parent = " + rup_parent + "\n");
		result.append ("x_km = " + x_km + "\n");
		result.append ("y_km = " + y_km + "\n");

		return result.toString();
	}




	// Produce a one-line string containing our contents (not newline-terminated).

	public final String one_line_string () {
		return String.format ("t=%.5f, mag=%.3f, k=%.4e, parent=%d, x=%.3f, y=%.3f",
			t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
	}




	// Produce a one-line string containing our contents (not newline-terminated).
	// This version prepends an index.

	public final String one_line_string (int j_rup) {
		return String.format ("%d: t=%.5f, mag=%.3f, k=%.4e, parent=%d, x=%.3f, y=%.3f",
			j_rup, t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
	}




	// Produce a one-line string containing our contents (not newline-terminated).
	// This version prepends two indexes.

	public final String one_line_string (int i_gen, int j_rup) {
		return String.format ("%d, %d: t=%.5f, mag=%.3f, k=%.4e, parent=%d, x=%.3f, y=%.3f",
			i_gen, j_rup, t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
	}




	// Produce a one-line string containing unrounded time and magnitude (not newline-terminated).

	public final String u_time_mag_string () {
		//return "t = " + t_day + ", mag = " + rup_mag;
		return "t = " + rndd(t_day) + ", mag = " + rndd(rup_mag);
	}




	// Produce a one-line string containing unrounded time, magnitude, and productivity (not newline-terminated).

	public final String u_time_mag_prod_string () {
		//return "t = " + t_day + ", mag = " + rup_mag + ", k_prod = " + k_prod;
		return "t = " + rndd(t_day) + ", mag = " + rndd(rup_mag) + ", k_prod = " + rndd(k_prod);
	}




	// Produce a one-line string containing unrounded time, magnitude, and magnitude of completeness (not newline-terminated).
	// Note: The k_prod field is assumed to contain magnitude of completeness.

	public final String u_time_mag_mc_string () {
		//return "t = " + t_day + ", mag = " + rup_mag + ", mc = " + k_prod;
		return "t = " + rndd(t_day) + ", mag = " + rndd(rup_mag) + ", mc = " + rndd(k_prod);
	}




	// Produce a one-line string containing our contents (not newline-terminated).
	// Note: The k_prod field is assumed to contain magnitude of completeness.
	// Note: The parent field is assumed to contain a rupture category.
	// Versions are provided that prepend one or two indexes.

	public final String one_line_mc_cat_string () {
		return String.format ("t=%.5f, mag=%.3f, mc=%.3f, cat=%d, x=%.3f, y=%.3f",
			t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
	}


	public final String one_line_mc_cat_string (int j) {
		return String.format ("%d: t=%.5f, mag=%.3f, mc=%.3f, cat=%d, x=%.3f, y=%.3f",
			j, t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
	}


	public final String one_line_mc_cat_string (int i, int j) {
		return String.format ("%d, %d: t=%.5f, mag=%.3f, mc=%.3f, cat=%d, x=%.3f, y=%.3f",
			i, j, t_day, rup_mag, k_prod, rup_parent, x_km, y_km);
	}




	// Comparator to sort by ascending time, and then by descending magnitude.

	public static class TimeAscMagDescComparator implements Comparator<OERupture> {
	
		// Compares its two arguments for order. Returns a negative integer, zero, or a positive
		// integer as the first argument is less than, equal to, or greater than the second.

		@Override
		public int compare (OERupture rup1, OERupture rup2) {

			// Order by time, earliest first

			int result = Double.compare (rup1.t_day, rup2.t_day);

			if (result == 0) {

				// Order by magnitude, largest first

				result = Double.compare (rup2.rup_mag, rup1.rup_mag);
			}

			return result;
		}
	}




	// Comparator to sort by descending magnitude, and then by descending time.

	public static class MagDescTimeDescComparator implements Comparator<OERupture> {
	
		// Compares its two arguments for order. Returns a negative integer, zero, or a positive
		// integer as the first argument is less than, equal to, or greater than the second.

		@Override
		public int compare (OERupture rup1, OERupture rup2) {

			// Order by magnitude, largest first

			int result = Double.compare (rup2.rup_mag, rup1.rup_mag);

			if (result == 0) {

				// Order by time, latest first

				result = Double.compare (rup2.t_day, rup1.t_day);
			}

			return result;
		}
	}




	// Fill a list of ruptures with ruptures created from the given times and magnitudes.
	// Parameters:
	//  rup_list = List (or Collection) to receive the ruptures.
	//  time_mag_array = Array with N pairs of elements.  In each pair, the first
	//                   element is time in days, the second element is magnitude.
	//                   It is an error if this array has an odd number of elements.

	public static void make_time_mag_list (Collection<OERupture> rup_list, double[] time_mag_array) {
	
		// Require even number of elements

		if (time_mag_array.length % 2 != 0) {
			throw new IllegalArgumentException ("OERupture.make_time_mag_list: Odd array length: length = " + time_mag_array.length);
		}

		// Add to list

		for (int n = 0; n < time_mag_array.length; n += 2) {
			OERupture rup = (new OERupture()).set (time_mag_array[n], time_mag_array[n+1]);
			rup_list.add (rup);
		}

		return;
	}





	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 79001;

	private static final String M_VERSION_NAME = "OERupture";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalDouble ("t_day"     , t_day     );
			writer.marshalDouble ("rup_mag"   , rup_mag   );
			writer.marshalDouble ("k_prod"    , k_prod    );
			writer.marshalInt    ("rup_parent", rup_parent);
			writer.marshalDouble ("x_km"      , x_km      );
			writer.marshalDouble ("y_km"      , y_km      );

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

			t_day      = reader.unmarshalDouble ("t_day"     );
			rup_mag    = reader.unmarshalDouble ("rup_mag"   );
			k_prod     = reader.unmarshalDouble ("k_prod"    );
			rup_parent = reader.unmarshalInt    ("rup_parent");
			x_km       = reader.unmarshalDouble ("x_km"      );
			y_km       = reader.unmarshalDouble ("y_km"      );

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

	public OERupture unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OERupture rup) {
		writer.marshalMapBegin (name);
		rup.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OERupture static_unmarshal (MarshalReader reader, String name) {
		OERupture rup = new OERupture();
		reader.unmarshalMapBegin (name);
		rup.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return rup;
	}

	// Marshal an array of objects.

	public static void marshal_array (MarshalWriter writer, String name, OERupture[] x) {
		int n = x.length;
		writer.marshalArrayBegin (name, n);
		for (int i = 0; i < n; ++i) {
			static_marshal (writer, null, x[i]);
		}
		writer.marshalArrayEnd ();
		return;
	}

	// Unmarshal an array of objects.

	public static OERupture[] unmarshal_array (MarshalReader reader, String name) {
		int n = reader.unmarshalArrayBegin (name);
		OERupture[] x = new OERupture[n];
		for (int i = 0; i < n; ++i) {
			x[i] = static_unmarshal (reader, null);
		}
		reader.unmarshalArrayEnd ();
		return x;
	}




	//----- Testing -----




	// Check if two ruptures are identical.
	// Note: This is primarily for testing.

	public final boolean check_rup_equal (OERupture other) {
		if (
			   this.t_day      == other.t_day
			&& this.rup_mag    == other.rup_mag
			&& this.k_prod     == other.k_prod
			&& this.rup_parent == other.rup_parent
			&& this.x_km       == other.x_km
			&& this.y_km       == other.y_km
		) {
			return true;
		}
		return false;
	}




	// Set to plausible random values.
	// Note: This is primarily for testing.

	public final OERupture set_to_random (OERandomGenerator rangen) {
		this.t_day      = rangen.uniform_sample (0.0, 3650.0);
		this.rup_mag    = rangen.uniform_sample (3.0, 9.0);
		this.k_prod     = Math.pow (10.0, rangen.uniform_sample (-5.0, 5.0));
		this.rup_parent = rangen.uniform_int_sample (-1, 500);
		this.x_km       = rangen.uniform_sample (-2000.0, 2000.0);
		this.y_km       = rangen.uniform_sample (-2000.0, 2000.0);
		return this;
	}

}
