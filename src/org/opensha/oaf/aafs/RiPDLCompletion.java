package org.opensha.oaf.aafs;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;


/**
 * Relay item payload for completion of PDL actions for the most recent forecast.
 * Author: Michael Barall 05/14/2019.
 *
 * These items are generated when:
 * - A forecast is sent to PDL.
 * - A forecast is skipped (the OAF product may or may not be deleted).
 * - Cleanup discovers a previously-unknown OAF product.
 * - Cleanup deletes an OAF product.
 */
public class RiPDLCompletion extends DBPayload {

	//----- Constants and variables -----

	// Action code, which specifies what PDL action was taken.

	public int ripdl_action;

	// If associated with a forecast, this is the time lag in milliseconds since the mainshock.
	// The value -1L is used if no forecast lag is available.

	public long ripdl_forecast_lag;

	// Action codes, which are possible values of ripdl_action.

	public static final int RIPDL_ACT_MIN				= 1;
	public static final int RIPDL_ACT_FORECAST_PDL		= 1;
		// An OAF product containing a forecast was sent to PDL;
		// forecast lag is supplied.
	public static final int RIPDL_ACT_FORECAST_NO_PDL	= 2;
		// A forecast was generated but did not produce results eligible for PDL;
		// forecast lag is supplied; any existing OAF product is unchanged.
	public static final int RIPDL_ACT_SKIPPED_STALE		= 3;
		// A forecast was skipped because it would be stale;
		// forecast lag is supplied; any existing OAF product is unchanged.
	public static final int RIPDL_ACT_SKIPPED_ANALYST	= 4;
		// A forecast was skipped at analyst request;
		// forecast lag is supplied; any existing OAF product is deleted.
	public static final int RIPDL_ACT_SKIPPED_INTAKE	= 5;
		// A forecast was skipped because the event does not pass the intake filter;
		// forecast lag is supplied; any existing OAF product is deleted.
	public static final int RIPDL_ACT_SKIPPED_SHADOWED	= 6;
		// A forecast was skipped because the mainshock was shadowed;
		// forecast lag is supplied; any existing OAF product is deleted.
	public static final int RIPDL_ACT_SKIPPED_FORESHOCK	= 7;
		// A forecast was skipped because the mainshock has an aftershock of larger magnitude;
		// forecast lag is supplied; any existing OAF product is deleted.
	public static final int RIPDL_ACT_CLEANUP_FIND		= 8;
		// The cleanup process found an existing OAF product;
		// forecast lag is -1L; the existing OAF product is unchanged.
	public static final int RIPDL_ACT_CLEANUP_DELETE	= 9;
		// The cleanup process deleted an OAF product; forecast lag is -1L.
	public static final int RIPDL_ACT_MAX				= 9;

	// Return a string describing the ripdl_action.

	public String get_ripdl_action_as_string () {
		switch (ripdl_action) {
		case RIPDL_ACT_FORECAST_PDL: return "RIPDL_ACT_FORECAST_PDL";
		case RIPDL_ACT_FORECAST_NO_PDL: return "RIPDL_ACT_FORECAST_NO_PDL";
		case RIPDL_ACT_SKIPPED_STALE: return "RIPDL_ACT_SKIPPED_STALE";
		case RIPDL_ACT_SKIPPED_ANALYST: return "RIPDL_ACT_SKIPPED_ANALYST";
		case RIPDL_ACT_SKIPPED_INTAKE: return "RIPDL_ACT_SKIPPED_INTAKE";
		case RIPDL_ACT_SKIPPED_SHADOWED: return "RIPDL_ACT_SKIPPED_SHADOWED";
		case RIPDL_ACT_SKIPPED_FORESHOCK: return "RIPDL_ACT_SKIPPED_FORESHOCK";
		case RIPDL_ACT_CLEANUP_FIND: return "RIPDL_ACT_CLEANUP_FIND";
		case RIPDL_ACT_CLEANUP_DELETE: return "RIPDL_ACT_CLEANUP_DELETE";
		}
		return "RIPDL_ACT_INVALID(" + ripdl_action + ")";
	}

	// Return true if this item represents the presence of an OAF product at the item time.

	public boolean is_product_update () {
		switch (ripdl_action) {
		case RIPDL_ACT_FORECAST_PDL:
		case RIPDL_ACT_CLEANUP_FIND:
			return true;
		}
		return false;
	}

	// Return true if this item represents the deletion of an OAF product at the item time.

	public boolean is_product_delete () {
		switch (ripdl_action) {
		case RIPDL_ACT_SKIPPED_ANALYST:
		case RIPDL_ACT_SKIPPED_INTAKE:
		case RIPDL_ACT_SKIPPED_SHADOWED:
		case RIPDL_ACT_SKIPPED_FORESHOCK:
		case RIPDL_ACT_CLEANUP_DELETE:
			return true;
		}
		return false;
	}

	// Return a friendly string representation of ripdl_forecast_lag.

	public String get_ripdl_forecast_lag_as_string () {
		if (ripdl_forecast_lag <= 0L) {
			return String.valueOf (ripdl_forecast_lag);
		}
		return SimpleUtils.duration_to_string (ripdl_forecast_lag);
	}




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiPDLCompletion () {}


	// Set up the contents.

	public void setup (int the_ripdl_action, long the_ripdl_forecast_lag) {
		ripdl_action = the_ripdl_action;
		ripdl_forecast_lag = the_ripdl_forecast_lag;
		return;
	}


	// Set up the contents, if no forecast lag is specified.

	public void setup (int the_ripdl_action) {
		ripdl_action = the_ripdl_action;
		ripdl_forecast_lag = -1L;
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 56001;

	private static final String M_VERSION_NAME = "RiPDLCompletion";

	// Marshal object, internal.

	@Override
	protected void do_marshal (MarshalWriter writer) {

		// Version

		writer.marshalInt (M_VERSION_NAME, MARSHAL_VER_1);

		// Superclass

		super.do_marshal (writer);

		// Contents

		writer.marshalInt  ("ripdl_action"      , ripdl_action      );
		writer.marshalLong ("ripdl_forecast_lag", ripdl_forecast_lag);

		return;
	}

	// Unmarshal object, internal.

	@Override
	protected void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Superclass

		super.do_umarshal (reader);

		// Contents

		ripdl_action       = reader.unmarshalInt  ("ripdl_action"      , RIPDL_ACT_MIN, RIPDL_ACT_MAX);
		ripdl_forecast_lag = reader.unmarshalLong ("ripdl_forecast_lag");

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
	public RiPDLCompletion unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Unmarshal object, for a relay item.

	@Override
	public RiPDLCompletion unmarshal_relay (RelayItem relit) {
		try {
			unmarshal (relit.get_details(), null);
		} catch (Exception e) {
			throw new DBCorruptException("Error unmarshaling relay item payload\n" + relit.toString() + "\nDump:\n" + relit.dump_details(), e);
		}
		return this;
	}

}
