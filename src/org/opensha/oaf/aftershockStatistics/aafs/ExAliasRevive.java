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

import org.opensha.oaf.aftershockStatistics.ComcatException;
import org.opensha.oaf.aftershockStatistics.CompactEqkRupList;

/**
 * Execute task: Notify alias timeline revive.
 * Author: Michael Barall 07/19/2018.
 */
public class ExAliasRevive extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_alias_revive (task);
	}




	// Notify alias timeline revive.

	private int exec_alias_revive (PendingTask task) {

		//--- Get payload

		OpAliasRevive payload = new OpAliasRevive();

		try {
			payload.unmarshal_task (task);
		}

		// Invalid task

		catch (Exception e) {

			// Display the error and log the task

			sg.task_sup.display_invalid_task (task, e);
			return RESCODE_TASK_CORRUPT;
		}

		//--- Final steps

		// Log the task

		return RESCODE_SUCCESS;
	}




	//----- Construction -----


	// Default constructor.

	public ExAliasRevive () {}

}
