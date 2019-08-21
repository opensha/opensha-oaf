package org.opensha.oaf.rj;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;

public class MagCompPage_Parameters {
	
	private double magCat;
	private MagCompFn magCompFn;

	private SearchMagFn magSample;
	private SearchRadiusFn radiusSample;
	private SearchMagFn magCentroid;
	private SearchRadiusFn radiusCentroid;
	
	/**
	 * This class is a container for the magnitude of completeness parameters and function. 
	 * Most oftern we use the function defined by Page et al. (2016, BSSA).
	 * According to Page et al. the magnitude of completeness is
	 *  magMin(t) = Max(F*magMain - G - H*log10(t), magCat)
	 * where t is measured in days.
	 *
	 * In "legacy" code, the Page function is used with capF = 0.5.  Also in "legacy" code,
	 * as a special case, if capG == 10.0 then the magnitude of completeness is always magCat
	 * (in which case it is recommended that capH == 0.0).
	 *
	 * This class also holds the magnitude and radius of the areas used to sample aftershocks
	 * (magSample and radiusSample) and to find the centroid of aftershock activity (magCentroid
	 * and radiusCentroid).  Magnitude is the minimum magnitude considered, and raidus is the
	 * multiple of the Wells and Coppersmith radius.  Magnitude can be -10.0 for no limit.
	 *
	 * In version 2 "legacy" code, magSample, radiusSample, magCentroid, and radiusCentroid
	 * were scalar variables.
	 * 
	 * @param magCat
	 * @param magCompFn
	 * @param magSample
	 * @param radiusSample
	 * @param magCentroid
	 * @param radiusCentroid
	 */
	public MagCompPage_Parameters (double magCat, MagCompFn magCompFn,
				SearchMagFn magSample, SearchRadiusFn radiusSample, SearchMagFn magCentroid, SearchRadiusFn radiusCentroid) {
		this.magCat = magCat;
		this.magCompFn = magCompFn;
		this.magSample = magSample;
		this.radiusSample = radiusSample;
		this.magCentroid = magCentroid;
		this.radiusCentroid = radiusCentroid;
	}

	
	/**
	 * This version defaults the magnitudes to -10.0 (no minimum) and the
	 * radii to 1.0 (Wells and Coppersmith value).
	 */
	public MagCompPage_Parameters (double magCat, MagCompFn magCompFn) {
		this (magCat, magCompFn, SearchMagFn.makeNoMinMag(), SearchRadiusFn.makeWC(1.0), SearchMagFn.makeNoMinMag(), SearchRadiusFn.makeWC(1.0));
	}

	
	/**
	 * Default constructor.
	 */
	public MagCompPage_Parameters () {}
	

	/**
	 * This returns the magnitude-independent catalog magnitude of completeness (magCat).
	 * (At present only the GUI uses this.)
	 * @return
	 */
	public double get_magCat () {
		return magCat;
	}
	

	/**
	 * This returns the catalog magnitude of completeness (magCat).
	 * @return
	 */
	public double get_magCat (double magMain) {
		return Math.max (magCat, magSample.getMag (magMain));
	}

	/**
	 * This returns the magnitude of completeness function (magCompFn).
	 * @return
	 */
	public MagCompFn get_magCompFn () {
		return magCompFn;
	}
	
	/**
	 * [DEPRECATED]
	 * This returns the G parameter (capG), for use in legacy code.
	 * @return
	 */
	//public double get_capG() {return magCompFn.getLegacyCapG();}
	
	/**
	 * [DEPRECATED]
	 * This returns the H parameter (capH), for use in legacy code.
	 * @return
	 */
	//public double get_capH() {return magCompFn.getLegacyCapH();}
	
	/**
	 * Return the minimum magnitude to use when sampling aftershocks, or -10.0 if none.
	 * @return
	 */
	public double get_magSample (double magMain) {
		return magSample.getMag (magMain);
	}
	
	/**
	 * Return the radius to use when sampling aftershocks, in km.
	 * @return
	 */
	public double get_radiusSample (double magMain) {
		return radiusSample.getRadius (magMain);
	}
	
	/**
	 * Return the minimum magnitude to use when finding the centroid of aftershock activity, or -10.0 if none.
	 * A value of 10.0 means to skip the centroid calculation and search around the hypocenter.
	 * @return
	 */
	public double get_magCentroid (double magMain) {
		return magCentroid.getMag (magMain);
	}
	
	/**
	 * Return the radius to use when finding the centroid of aftershock activity, in km.
	 * @return
	 */
	public double get_radiusCentroid (double magMain) {
		return radiusCentroid.getRadius (magMain);
	}


	// Functions to return the functions for sample magnitude and radius.

	public SearchMagFn get_fcn_magSample () {
		return magSample;
	}

	public SearchRadiusFn get_fcn_radiusSample () {
		return radiusSample;
	}

	public SearchMagFn get_fcn_magCentroid () {
		return magCentroid;
	}

	public SearchRadiusFn get_fcn_radiusCentroid () {
		return radiusCentroid;
	}


	@Override
	public String toString() {
		return "Page_Params:" + "\n"
			+ "  magCat = " + magCat + "\n"
			+ "  magCompFn = " + magCompFn.toString() + "\n"
			+ "  magSample = " + magSample.toString() + "\n"
			+ "  radiusSample = " + radiusSample.toString() + "\n"
			+ "  magCentroid = " + magCentroid.toString() + "\n"
			+ "  radiusCentroid = " + radiusCentroid.toString();
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 2001;
	private static final int MARSHAL_VER_2 = 2002;
	private static final int MARSHAL_VER_3 = 2003;

	private static final String M_VERSION_NAME = "MagCompPage_Parameters";

	// Marshal type code.

	protected static final int MARSHAL_NULL = 2000;
	protected static final int MARSHAL_MAG_COMP = 2001;

	protected static final String M_TYPE_NAME = "ClassType";

	// Get the type code.

	protected int get_marshal_type () {
		return MARSHAL_MAG_COMP;
	}

	// Marshal object, internal.

	protected void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_3;
		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("MagCompPage_Parameters.do_marshal: Unknown version number: " + ver);

		case MARSHAL_VER_1: {

			writer.marshalDouble ("magCat"        , magCat        );
			writer.marshalDouble ("capG"          , magCompFn.getLegacyCapG());
			writer.marshalDouble ("capH"          , magCompFn.getLegacyCapH());
			writer.marshalDouble ("magSample"     , magSample.getLegacyMag()        );
			writer.marshalDouble ("radiusSample"  , radiusSample.getLegacyRadius()  );
			writer.marshalDouble ("magCentroid"   , magCentroid.getLegacyMag()      );
			writer.marshalDouble ("radiusCentroid", radiusCentroid.getLegacyRadius());

		}
		break;

		case MARSHAL_VER_2: {

			writer.marshalDouble ("magCat"        , magCat        );
			MagCompFn.marshal_poly (writer, "magCompFn", magCompFn);
			writer.marshalDouble ("magSample"     , magSample.getLegacyMag()        );
			writer.marshalDouble ("radiusSample"  , radiusSample.getLegacyRadius()  );
			writer.marshalDouble ("magCentroid"   , magCentroid.getLegacyMag()      );
			writer.marshalDouble ("radiusCentroid", radiusCentroid.getLegacyRadius());

		}
		break;

		case MARSHAL_VER_3: {

			writer.marshalDouble        (        "magCat"        , magCat        );
			MagCompFn.marshal_poly      (writer, "magCompFn"     , magCompFn     );
			SearchMagFn.marshal_poly    (writer, "magSample"     , magSample     );
			SearchRadiusFn.marshal_poly (writer, "radiusSample"  , radiusSample  );
			SearchMagFn.marshal_poly    (writer, "magCentroid"   , magCentroid   );
			SearchRadiusFn.marshal_poly (writer, "radiusCentroid", radiusCentroid);

		}
		break;

		}
	
		return;
	}

	// Unmarshal object, internal.

	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_3);

		// Contents

		switch (ver) {

		default:
			throw new MarshalException ("ForecastData.do_umarshal: Unknown version number: " + ver);

		case MARSHAL_VER_1: {

			magCat         = reader.unmarshalDouble ("magCat"        );
			double capG    = reader.unmarshalDouble ("capG"          );
			double capH    = reader.unmarshalDouble ("capH"          );
			magCompFn      = MagCompFn.makeLegacyPage (capG, capH);
			magSample      = SearchMagFn.makeLegacyMag   (reader.unmarshalDouble ("magSample"     ));
			radiusSample   = SearchRadiusFn.makeLegacyWC (reader.unmarshalDouble ("radiusSample"  ));
			magCentroid    = SearchMagFn.makeLegacyMag   (reader.unmarshalDouble ("magCentroid"   ));
			radiusCentroid = SearchRadiusFn.makeLegacyWC (reader.unmarshalDouble ("radiusCentroid"));

		}
		break;

		case MARSHAL_VER_2: {

			magCat         = reader.unmarshalDouble ("magCat"        );
			magCompFn      = MagCompFn.unmarshal_poly (reader, "magCompFn");
			magSample      = SearchMagFn.makeLegacyMag   (reader.unmarshalDouble ("magSample"     ));
			radiusSample   = SearchRadiusFn.makeLegacyWC (reader.unmarshalDouble ("radiusSample"  ));
			magCentroid    = SearchMagFn.makeLegacyMag   (reader.unmarshalDouble ("magCentroid"   ));
			radiusCentroid = SearchRadiusFn.makeLegacyWC (reader.unmarshalDouble ("radiusCentroid"));

		}
		break;

		case MARSHAL_VER_3: {

			magCat         = reader.unmarshalDouble        (        "magCat"        );
			magCompFn      = MagCompFn.unmarshal_poly      (reader, "magCompFn"     );
			magSample      = SearchMagFn.unmarshal_poly    (reader, "magSample"     );
			radiusSample   = SearchRadiusFn.unmarshal_poly (reader, "radiusSample"  );
			magCentroid    = SearchMagFn.unmarshal_poly    (reader, "magCentroid"   );
			radiusCentroid = SearchRadiusFn.unmarshal_poly (reader, "radiusCentroid");

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

	public MagCompPage_Parameters unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object, polymorphic.

	public static void marshal_poly (MarshalWriter writer, String name, MagCompPage_Parameters obj) {

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

	public static MagCompPage_Parameters unmarshal_poly (MarshalReader reader, String name) {
		MagCompPage_Parameters result;

		reader.unmarshalMapBegin (name);
	
		// Switch according to type

		int type = reader.unmarshalInt (M_TYPE_NAME);

		switch (type) {

		default:
			throw new MarshalException ("MagCompPage_Parameters.unmarshal_poly: Unknown class type code: type = " + type);

		case MARSHAL_NULL:
			result = null;
			break;

		case MARSHAL_MAG_COMP:
			result = new MagCompPage_Parameters();
			result.do_umarshal (reader);
			break;
		}

		reader.unmarshalMapEnd ();

		return result;
	}
	
}
