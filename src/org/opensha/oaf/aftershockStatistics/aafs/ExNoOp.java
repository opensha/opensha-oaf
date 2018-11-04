package org.opensha.oaf.aftershockStatistics.aafs;

import java.util.List;

import org.opensha.oaf.aftershockStatistics.aafs.entity.PendingTask;
import org.opensha.oaf.aftershockStatistics.aafs.entity.LogEntry;
import org.opensha.oaf.aftershockStatistics.aafs.entity.CatalogSnapshot;
import org.opensha.oaf.aftershockStatistics.aafs.entity.TimelineEntry;

import org.opensha.oaf.aftershockStatistics.util.MarshalReader;
import org.opensha.oaf.aftershockStatistics.util.MarshalWriter;
import org.opensha.oaf.aftershockStatistics.util.SimpleUtils;

import org.opensha.oaf.aftershockStatistics.CompactEqkRupList;

/**
 * Execute task: No operation.
 * Author: Michael Barall 06/25/2018.
 */
public class ExNoOp extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_no_op (task);
	}




	// Execute no operation.

	private int exec_no_op (PendingTask task) {

		// Remove the task from the queue

		return RESCODE_DELETE;
	}




	//----- Construction -----


	// Default constructor.

	public ExNoOp () {}

}
