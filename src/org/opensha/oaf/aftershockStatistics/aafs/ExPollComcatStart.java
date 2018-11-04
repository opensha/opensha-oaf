package org.opensha.oaf.aftershockStatistics.aafs;

import java.util.List;

import org.opensha.oaf.aftershockStatistics.aafs.entity.PendingTask;
import org.opensha.oaf.aftershockStatistics.aafs.entity.LogEntry;
import org.opensha.oaf.aftershockStatistics.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aftershockStatistics.aafs.entity.TimelineEntry;
import org.opensha.oaf.aftershockStatistics.aafs.entity.AliasFamily;

import org.opensha.oaf.aftershockStatistics.util.MarshalReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalWriter;
import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;

import org.opensha.oaf.aftershockStatistics.CompactEqkRupList;
import org.opensha.oaf.aftershockStatistics.comcat.ComcatAccessor;
import org.opensha.oaf.aftershockStatistics.comcat.ComcatException;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupList;
import org.opensha.commons.geo.Location;

/**
 * Execute task: Start Comcat polling.
 * Author: Michael Barall 08/05/2018.
 */
public class ExPollComcatStart extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_poll_comcat_start (task);
	}




	// Start Comcat polling.

	private int exec_poll_comcat_start (PendingTask task) {

		//--- Get payload

		OpPollComcatStart payload = new OpPollComcatStart();

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

		// Enable polling

		sg.poll_sup.set_polling_enabled();

		//--- Final steps

		// Log the task

		return RESCODE_SUCCESS;
	}




	//----- Construction -----


	// Default constructor.

	public ExPollComcatStart () {}

}
