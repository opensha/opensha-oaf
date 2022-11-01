package org.opensha.oaf.aafs;

import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.pdl.PDLCodeChooserEventSequence;
import org.opensha.oaf.comcat.PropertiesEventSequence;


// Holds the results of an event-sequence PDL operation.
// Author: Michael Barall 10/25/2022.
//
// Also used to encapsulate a send operation for later execution.

public class EventSequenceResult {

	//----- Contents -----

	//--- Common for logging identification

	// The event ID used for anp operation.

	public String event_id;


	//--- Results of a delete or cap operation

	// The result of a delete or cap operation, DOESP_XXXXX from PDLCodeChooserEventSequence.
	// Contains DOESP_NOP is no delete or cap operation was donw.
	// Contains DOESP_DELETED if one or more event-sequence products were deleted.
	// Contains DOESP_CAPPED if an event-sequence product was capped.
	// Other values are possible, and generally should be disregarded.

	public int doesp;

	// Cap time for a cap operation, or one of the CAP_TIME_XXXXX special values from PDLCodeChooserEventSequence.
	// Contains CAP_TIME_NOP if no cap operation was attempted.
	// Generally this is meaningful only if doesp == DOESP_CAPPED.

	public long cap_time;


	//--- Encapsulated send operation

	// True if send operation is completed, false if it is pending.

	public boolean f_send_complete;

	// Properties for sending an event-sequence product.
	// Null if no send operation pending or occurred.

	public PropertiesEventSequence props;

	// PDL code used for sending an event-sequence product.
	// Null if no send operation pending or occurred.

	public String pdl_code;

	// Review flag for the send operation.

	public boolean isReviewed;


	//----- Construction -----
	

	// Default constructor makes an object with no results.

	public EventSequenceResult () {
		event_id = null;

		doesp = PDLCodeChooserEventSequence.DOESP_NOP;
		cap_time = PDLCodeChooserEventSequence.CAP_TIME_NOP;

		f_send_complete = false;
		props = null;
		pdl_code = null;
		isReviewed = false;
	}


	// Clear to no operation.

	public final void clear () {
		event_id = null;

		doesp = PDLCodeChooserEventSequence.DOESP_NOP;
		cap_time = PDLCodeChooserEventSequence.CAP_TIME_NOP;

		f_send_complete = false;
		props = null;
		pdl_code = null;
		isReviewed = false;
		return;
	}


	// Set the result for a delete or cap operation.

	public final EventSequenceResult set_for_delete (String the_event_id, int the_doesp, long the_cap_time) {
		event_id = the_event_id;

		doesp = the_doesp;
		cap_time = the_cap_time;

		f_send_complete = false;
		props = null;
		pdl_code = null;
		isReviewed = false;
		return this;
	}


	// Set the result for a pending send operation.

	public final EventSequenceResult set_for_pending_send (String the_event_id,
			PropertiesEventSequence the_props, String the_pdl_code, boolean the_isReviewed) {

		event_id = the_event_id;

		doesp = PDLCodeChooserEventSequence.DOESP_NOP;
		cap_time = PDLCodeChooserEventSequence.CAP_TIME_NOP;

		f_send_complete = false;
		props = the_props;
		pdl_code = the_pdl_code;
		isReviewed = the_isReviewed;
		return this;
	}


	// Mark a send operation complete.
	// Note: If no properties, send remains incomplete.

	public final void mark_send_complete () {
		if (props != null) {
			f_send_complete = true;
		}
		return;
	}


	//----- Operations -----


	// Perform the send operation.
	// If successful, send is marked complete.
	// Does nothing if no properties have been supplied.
	// Throws exception if error.

	public final void perform_send () {
		if (props != null && (!f_send_complete)) {
			PDLCodeChooserEventSequence.sendEventSequenceProduct (props, pdl_code, isReviewed);
			f_send_complete = true;
		}
		return;
	}


	// Write log entry for this operation.
	// Parameters:
	//  sg = Server group.

	public final void write_log (ServerGroup sg) {

		// Log it, if we deleted or capped something

		if (doesp == PDLCodeChooserEventSequence.DOESP_DELETED) {
			sg.log_sup.report_evseq_delete_ok (event_id);
		}
		if (doesp == PDLCodeChooserEventSequence.DOESP_CAPPED) {
			sg.log_sup.report_evseq_cap_ok (event_id, cap_time);
		}

		// Log it, if we sent something.

		if (f_send_complete && props != null) {
			sg.log_sup.report_evseq_send_ok (event_id, props, pdl_code);
		}

		return;
	}


	// toString - Convert to string.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("EventSequenceResult:" + "\n");

		result.append ("event_id = " + ((event_id == null) ? "<null>" : event_id) + "\n");

		result.append ("doesp = " + PDLCodeChooserEventSequence.get_doesp_as_string(doesp) + "\n");
		result.append ("cap_time = " + PDLCodeChooserEventSequence.cap_time_raw_and_string(cap_time) + "\n");

		result.append ("f_send_complete = " + f_send_complete + "\n");
		result.append ("props = " + ((props == null) ? ("<null>" + "\n") : props.toString()));
		result.append ("pdl_code = " + ((pdl_code == null) ? "<null>" : pdl_code) + "\n");
		result.append ("isReviewed = " + isReviewed + "\n");

		return result.toString();
	}
		
}
