package org.opensha.oaf.oetas.env;


// Log information for operational ETAS.
// Author: Michael Barall 06/25/2024.
//
// This class contains the information necessary for logging an operational ETAS operation.
//
// Note: This class is not intended to be marshaled.

public class OEtasLogInfo {


	//----- Constants -----

	// Log type codes.

	public static final int ETAS_LOGTYPE_MIN = 0;			// Minimum value
	public static final int ETAS_LOGTYPE_OMIT = 0;			// Omit log entry for this operation
	public static final int ETAS_LOGTYPE_OK = 1;			// Success
	public static final int ETAS_LOGTYPE_FAIL = 2;			// ETAS operation failed
	public static final int ETAS_LOGTYPE_SKIP = 3;			// ETAS was skipped
	public static final int ETAS_LOGTYPE_UNKNOWN = 4;		// Unknown log type
	public static final int ETAS_LOGTYPE_REJECT = 5;		// ETAS forecast was rejected
	public static final int ETAS_LOGTYPE_MAX = 5;			// Maximum value

	// Return a string identifying the log type

	public static String log_type_to_string (int logtype) {
		switch (logtype) {
		case ETAS_LOGTYPE_OMIT: return "ETAS_LOGTYPE_OMIT";
		case ETAS_LOGTYPE_OK: return "ETAS_LOGTYPE_OK";
		case ETAS_LOGTYPE_FAIL: return "ETAS_LOGTYPE_FAIL";
		case ETAS_LOGTYPE_SKIP: return "ETAS_LOGTYPE_SKIP";
		case ETAS_LOGTYPE_UNKNOWN: return "ETAS_LOGTYPE_UNKNOWN";
		case ETAS_LOGTYPE_REJECT: return "ETAS_LOGTYPE_REJECT";
		}
		return "ETAS_LOGTYPE_INVALID(" + logtype + ")";
	}



	//----- Log information -----

	// The type of log entry to produce.

	public int etas_logtype;

	// A string suitable for a log entry.
	// It is a one-line string, not ending in newline.
	// Can be null or empty to indicate there is no log entry string.

	public String etas_log_string;

	// The abort message from an exception, or null if none.

	public String etas_abort_message;




	//----- Functions -----


	// Return true if there is an abort message.

	public final boolean has_etas_abort_message () {
		return etas_abort_message != null;
	}


	// Return true if this contains a non-omitted log entry.

	public final boolean has_log_entry () {
		if (etas_logtype == ETAS_LOGTYPE_OMIT) {
			return false;
		}
		return true;
	}


	// Return true if there is non-null and non-empty log string.

	public final boolean has_etas_log_string () {
		return (etas_log_string != null && etas_log_string.length() > 0);
	}




	//----- Global log information -----

	// True if global log information is available.

	public boolean g_avail;

	// True if the global log information is a success (ETAS forecast completed).

	public boolean g_success;

	// Result code.

	public int g_rescode;

	//  Total execution time, in milliseconds, or -1L if unknown.

	public long g_total_time;

	// Fitting execution time, in milliseconds, or -1L if unknown.

	public long g_fit_time;

	// Simulation execution time, in milliseconds, or -1L if unknown.

	public long g_sim_time;

	// Fitting memory, in bytes, or -1L if unknown.

	public long g_fit_memory;

	// Simulation memory, in bytes, or -1L if unknown.

	public long g_sim_memor;




	//----- Global log functions -----




	// Save parameters to global accumulate an ETAS failure.
	// Parameters:
	//  rescode = ETAS result code.

	public void set_global_accum_failure (int rescode) {
		g_avail = true;
		g_success = false;
		g_rescode = rescode;
		g_total_time = -1L;
		g_fit_time = -1L;
		g_sim_time = -1L;
		g_fit_memory = -1L;
		g_sim_memor = -1L;
		return;
	}




	// Save parameters to global accumulate an ETAS success.
	// Parameters:
	//  rescode = ETAS result code.
	//  total_time = Total execution time, in milliseconds, or -1L if unknown.
	//  fit_time = Fitting execution time, in milliseconds, or -1L if unknown.
	//  sim_time = Simulation execution time, in milliseconds, or -1L if unknown.
	//  fit_memory = Fitting memory, in bytes, or -1L if unknown.
	//  sim_memory = Simulation memory, in bytes, or -1L if unknown.

	public void set_global_accum_success (
		int rescode,
		long total_time,
		long fit_time,
		long sim_time,
		long fit_memory,
		long sim_memory
	) {
		g_avail = true;
		g_success = true;
		g_rescode = rescode;
		g_total_time = total_time;
		g_fit_time = fit_time;
		g_sim_time = sim_time;
		g_fit_memory = fit_memory;
		g_sim_memor = sim_memory;
		return;
	}




	// Apply saved parameters to the global accumulator.
	// Returns the global accumulator contents as a string.
	// Callers should assume the return may be null or empty.

	public String apply_to_global_accum () {
		String global_stats;

		if (g_avail) {
			if (g_success) {

				// ETAS success

				global_stats = OEtasLogAccum.global_accum_success_to_string (
					g_rescode,			// int rescode,
					g_total_time,		// long total_time,
					g_fit_time,			// long fit_time,
					g_sim_time,			// long sim_time,
					g_fit_memory,		// long fit_memory,
					g_sim_memor			// long sim_memory
				);

			} else {

				// ETAS failure

				global_stats = OEtasLogAccum.global_accum_failure_to_string (g_rescode);

			}
		} else {

			// No ETAS operation

			global_stats = OEtasLogAccum.global_to_string();

		}

		return global_stats;
	}




	//----- Construction -----


	// Clear the information.

	public final void clear () {
		etas_logtype = ETAS_LOGTYPE_OMIT;
		etas_log_string = null;
		etas_abort_message = null;

		g_avail = false;
		g_success = false;
		g_rescode = OEExecEnvironment.ETAS_RESCODE_NOT_ATTEMPTED;
		g_total_time = -1L;
		g_fit_time = -1L;
		g_sim_time = -1L;
		g_fit_memory = -1L;
		g_sim_memor = -1L;
		return;
	}


	// Default constructor.

	public OEtasLogInfo () {
		clear();
	}


	// Constructor.

	public OEtasLogInfo (int etas_logtype, String etas_log_string, String etas_abort_message) {
		if (!( etas_logtype >= ETAS_LOGTYPE_MIN && etas_logtype <= ETAS_LOGTYPE_MAX )) {
			throw new IllegalArgumentException ("OEtasLogInfo.OEtasLogInfo: Invalid log type: etas_logtype = " + etas_logtype);
		}

		this.etas_logtype = etas_logtype;
		this.etas_log_string = etas_log_string;
		this.etas_abort_message = etas_abort_message;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append (log_type_to_string (etas_logtype));
		if (has_etas_log_string()) {
			result.append (": " + etas_log_string);
		}
		result.append ("\n");

		if (has_etas_abort_message()) {
			result.append (etas_abort_message + "\n");
		}

		return result.toString();
	}




}
