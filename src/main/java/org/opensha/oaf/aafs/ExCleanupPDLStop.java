package org.opensha.oaf.aafs;

import java.util.List;

import org.opensha.oaf.aafs.entity.PendingTask;
import org.opensha.oaf.aafs.entity.LogEntry;
import org.opensha.oaf.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aafs.entity.TimelineEntry;
import org.opensha.oaf.aafs.entity.AliasFamily;
import org.opensha.oaf.aafs.entity.RelayItem;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.SimpleUtils;

import org.opensha.oaf.rj.CompactEqkRupList;
import org.opensha.oaf.comcat.ComcatOAFAccessor;
import org.opensha.commons.data.comcat.ComcatException;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

/**
 * Execute task: Stop cleaning up old forecasts in PDL.
 * Author: Michael Barall 07/03/2019.
 */
public class ExCleanupPDLStop extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_cleanup_pdl_stop (task);
	}




	// Stop cleaning up old forecasts in PDL.

	private int exec_cleanup_pdl_stop (PendingTask task) {

		//--- Get payload

		OpCleanupPDLStop payload = new OpCleanupPDLStop();

		try {
			payload.unmarshal_task (task);
		}

		// Invalid task

		catch (Exception e) {

			// Display the error and log the task

			sg.task_sup.display_invalid_task (task, e);
			return RESCODE_TASK_CORRUPT;
		}

		//--- Poll status

		// Disable cleanup

		sg.cleanup_sup.set_cleanup_disabled();

		//--- Final steps

		// Log the task

		return RESCODE_SUCCESS;
	}




	//----- Construction -----


	// Default constructor.

	public ExCleanupPDLStop () {}

}
