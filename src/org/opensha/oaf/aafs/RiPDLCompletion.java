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
 * Relay item payload for submission of forecasts to PDL.
 * Author: Michael Barall 05/14/2019.
 *
 * This items is generated when:
 * - The primary server successfully sends a forecast to PDL.
 * - The cleanup process finds an existing OAF product that has no matching item. 
 *
 * The relay_id should contain the authoritative event id.
 *
 * The existence of this item means that a forecast was sent to PDL, with an
 * update time equal or close to the contained update time.
 *
 * This item can be used in two ways:
 * - When a server transitions from secondary to primary, it can look for this item
 *   to check if a forecast has already been sent.
 * - When the cleanup process finds an earthquake with an existing OAF product,
 *   it can look for this item to determine the update time, and skip that earthquake
 *   if it is too young to be deleted, without doing a separate Comcat call.
 *
 * There is no need to retain items when ripdl_update_time is more than one year old,
 * because forecasts older than that can be deleted from PDL based on their age.
 * (Specifically, an item can be deleted if ripdl_update_time is older than
 * ActionConfig.get_removal_forecast_age() + ActionConfig.get_removal_update_skew().)
 */
public class RiPDLCompletion extends DBPayload {

	//----- Constants and variables -----

	// Action code, which specifies what PDL action was taken.
	// Note: All action codes are 3 digits, so that the JSON-encoded form
	// of this object has fixed length.

	public int ripdl_action;

	// If associated with a forecast, this is the time lag in milliseconds since the mainshock.
	// The value -1L is used if no forecast lag is available.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public long ripdl_forecast_lag;

	// The update time associated with the OAF product.
	// It may vary slightly from the update time in PDL due to clock skew.
	// Note: An offset is added when the object is serialized, so that the
	// JSON-encoded form of this object has fixed length.

	public long ripdl_update_time;

	// Action codes, which are possible values of ripdl_action.

	public static final int RIPDL_ACT_MIN				= 101;
	public static final int RIPDL_ACT_FORECAST_PDL		= 101;
		// An OAF product containing a forecast was sent to PDL successfully;
		// forecast lag is supplied.
	public static final int RIPDL_ACT_CLEANUP_FIND		= 102;
		// The cleanup process found an existing OAF product;
		// forecast lag is -1L; the existing OAF product is unchanged.
	public static final int RIPDL_ACT_MAX				= 102;

	// Return a string describing the ripdl_action.

	public String get_ripdl_action_as_string () {
		switch (ripdl_action) {
		case RIPDL_ACT_FORECAST_PDL: return "RIPDL_ACT_FORECAST_PDL";
		case RIPDL_ACT_CLEANUP_FIND: return "RIPDL_ACT_CLEANUP_FIND";
		}
		return "RIPDL_ACT_INVALID(" + ripdl_action + ")";
	}

	// Return a friendly string representation of ripdl_forecast_lag.

	public String get_ripdl_forecast_lag_as_string () {
		if (ripdl_forecast_lag <= 0L) {
			return String.valueOf (ripdl_forecast_lag);
		}
		return SimpleUtils.duration_to_string_2 (ripdl_forecast_lag);
	}

	// Return a friendly string representation of ripdl_update_time.

	public String get_ripdl_update_time_as_string () {
		return SimpleUtils.time_raw_and_string (ripdl_update_time);
	}

	// Return true if this item is expired.
	// Parameters:
	//  time_now = Current time, in milliseconds since the epoch.
	//  action_config = Action configuration parameters.
	// Returns true if this item is old enough that it can be deleted.

	public boolean is_expired (long time_now, ActionConfig action_config) {
		if (ripdl_update_time + action_config.get_removal_forecast_age()
			+ action_config.get_removal_update_skew() < time_now) {

			return true;
		}
		return false;
	}

	// Serialization offset for longs.

	private static final long OFFSERL = 2000000000000000L;		// 2*10^15 ~ 60,000 years




	//----- Construction -----

	/**
	 * Default constructor does nothing.
	 */
	public RiPDLCompletion () {}


	// Set up the contents.

	public void setup (int the_ripdl_action, long the_ripdl_forecast_lag, long the_ripdl_update_time) {
		ripdl_action = the_ripdl_action;
		ripdl_forecast_lag = the_ripdl_forecast_lag;
		ripdl_update_time = the_ripdl_update_time;
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
		writer.marshalLong ("ripdl_forecast_lag", ripdl_forecast_lag + OFFSERL);
		writer.marshalLong ("ripdl_update_time" , ripdl_update_time  + OFFSERL);

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
		ripdl_forecast_lag = reader.unmarshalLong ("ripdl_forecast_lag") - OFFSERL;
		ripdl_update_time  = reader.unmarshalLong ("ripdl_update_time" ) - OFFSERL;

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
