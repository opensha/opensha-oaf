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

import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.oaf.rj.CompactEqkRupList;

/**
 * Execute task: Issue next delayed operation for a timeline.
 * Author: Michael Barall 05/30/2019.
 */
public class ExNextTimelineOp extends ServerExecTask {


	//----- Task execution -----


	// Execute the task, called from the task dispatcher.
	// The parameter is the task to execute.
	// The return value is a result code.
	// Support functions, task context, and result functions are available through the server group.

	@Override
	public int exec_task (PendingTask task) {
		return exec_next_timeline_op (task);
	}




	// Issue next delayed operation for a timeline.

	private int exec_next_timeline_op (PendingTask task) {

		//--- Get payload and timeline status

		OpNextTimelineOp payload = new OpNextTimelineOp();
		TimelineStatus tstatus = new TimelineStatus();

		int rescode = sg.timeline_sup.open_timeline (task, tstatus, payload);

		switch (rescode) {

		case RESCODE_TIMELINE_EXISTS:
			break;

		case RESCODE_TIMELINE_NOT_FOUND:
			sg.task_disp.set_display_taskres_log ("TASK-ERR: Timeline entry not found:\n"
				+ "event_id = " + task.get_event_id());
			return rescode;

		default:
			return rescode;
		}

		//--- Found existing timeline

		// Note: This task is not allowed to make any calls to Comcat.
		// This restriction ensures that operations can be issued quickly without retries.

		// Delete any existing delayed operations for the timeline

		sg.timeline_sup.delete_delayed_timeline_tasks (tstatus.event_id);

		// Issue any new delayed command that is needed

		sg.timeline_sup.next_auto_timeline (tstatus);

		// Log the task

		return RESCODE_SUCCESS;
	}




	//----- Construction -----


	// Default constructor.

	public ExNextTimelineOp () {}

}
