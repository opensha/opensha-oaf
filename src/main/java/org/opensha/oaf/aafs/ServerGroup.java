package org.opensha.oaf.aafs;

/**
 * Holds all the components that make up a server.
 * Author: Michael Barall 06/24/2018.
 */
public class ServerGroup extends ServerComponent {


	//----- Components -----
	//
	// These link to all the server components.


	// Task dispatcher.

	public TaskDispatcher task_disp;

	// Task support.

	public TaskSupport task_sup;

	// Log support.

	public LogSupport log_sup;

	// Timeline support.

	public TimelineSupport timeline_sup;

	// Alias support.

	public AliasSupport alias_sup;

	// PDL support.

	public PDLSupport pdl_sup;

	// Backup support.

	public BackupSupport backup_sup;

	// Poll support.

	public PollSupport poll_sup;

	// Relay support.

	public RelaySupport relay_sup;

	// Cleanup support.

	public CleanupSupport cleanup_sup;

	// Health support.

	public HealthSupport health_sup;

	// Relay link management.

	public RelayLink relay_link;

	// Dispatch table.

	public ServerExecTask[] dispatch_table;



	//----- Construction -----


	// Default constructor.

	public ServerGroup () {
		this.task_disp      = null;
		this.task_sup       = null;
		this.log_sup        = null;
		this.timeline_sup   = null;
		this.alias_sup      = null;
		this.pdl_sup        = null;
		this.backup_sup     = null;
		this.poll_sup       = null;
		this.relay_sup      = null;
		this.cleanup_sup    = null;
		this.health_sup     = null;
		this.relay_link     = null;

		this.dispatch_table = null;
	}


	// Allocate and set up all the server components.
	// This should be called only from the task dispatcher.

	protected void alloc_comp (TaskDispatcher task_disp) {

		// Self-setup

		setup (this);

		// Save the task dispatcher, which has already set up itself

		this.task_disp    = task_disp;

		// Allocate and set up the other components

		this.task_sup       = new TaskSupport();
		this.task_sup.setup (this);

		this.log_sup        = new LogSupport();
		this.log_sup.setup (this);

		this.timeline_sup   = new TimelineSupport();
		this.timeline_sup.setup (this);

		this.alias_sup      = new AliasSupport();
		this.alias_sup.setup (this);

		this.pdl_sup        = new PDLSupport();
		this.pdl_sup.setup (this);

		this.backup_sup     = new BackupSupport();
		this.backup_sup.setup (this);

		this.poll_sup       = new PollSupport();
		this.poll_sup.setup (this);

		this.relay_sup      = new RelaySupport();
		this.relay_sup.setup (this);

		this.cleanup_sup    = new CleanupSupport();
		this.cleanup_sup.setup (this);

		this.health_sup     = new HealthSupport();
		this.health_sup.setup (this);

		this.relay_link     = new RelayLink();
		this.relay_link.setup (this);

		// Set up the dispatch table

		dispatch_table = new ServerExecTask[OPCODE_MAX + 1];

		for (int i = 0; i <= OPCODE_MAX; ++i) {
			dispatch_table[i] = null;
		}

		dispatch_table[OPCODE_NO_OP            ] = new ExNoOp();
		dispatch_table[OPCODE_SHUTDOWN         ] = new ExShutdown();
		dispatch_table[OPCODE_CON_MESSAGE      ] = new ExConsoleMessage();
		dispatch_table[OPCODE_GEN_FORECAST     ] = new ExGenerateForecast();
		dispatch_table[OPCODE_GEN_PDL_REPORT   ] = new ExGeneratePDLReport();
		dispatch_table[OPCODE_GEN_EXPIRE       ] = new ExGenerateExpire();
		dispatch_table[OPCODE_INTAKE_SYNC      ] = new ExIntakeSync();
		dispatch_table[OPCODE_INTAKE_PDL       ] = new ExIntakePDL();
		dispatch_table[OPCODE_ANALYST_INTERVENE] = new ExAnalystIntervene();
		dispatch_table[OPCODE_UNKNOWN          ] = new ExUnknown();
		dispatch_table[OPCODE_ALIAS_SPLIT      ] = new ExAliasSplit();
		dispatch_table[OPCODE_ALIAS_STOP       ] = new ExAliasStop();
		dispatch_table[OPCODE_ALIAS_REVIVE     ] = new ExAliasRevive();
		dispatch_table[OPCODE_INTAKE_POLL      ] = new ExIntakePoll();
		dispatch_table[OPCODE_POLL_COMCAT_RUN  ] = new ExPollComcatRun();
		dispatch_table[OPCODE_POLL_COMCAT_START] = new ExPollComcatStart();
		dispatch_table[OPCODE_POLL_COMCAT_STOP ] = new ExPollComcatStop();
		dispatch_table[OPCODE_NEXT_TIMELINE_OP ] = new ExNextTimelineOp();
		dispatch_table[OPCODE_CLEANUP_PDL_START] = new ExCleanupPDLStart();
		dispatch_table[OPCODE_CLEANUP_PDL_STOP ] = new ExCleanupPDLStop();
		dispatch_table[OPCODE_SET_RELAY_MODE   ] = new ExSetRelayMode();
		dispatch_table[OPCODE_ANALYST_SELECTION] = new ExAnalystSelection();
		dispatch_table[OPCODE_HEALTH_MON_RESET ] = new ExHealthMonitorReset();
		dispatch_table[OPCODE_HEALTH_MON_START ] = new ExHealthMonitorStart();
		dispatch_table[OPCODE_HEALTH_MON_STOP  ] = new ExHealthMonitorStop();

		for (int i = 0; i <= OPCODE_MAX; ++i) {
			if (dispatch_table[i] != null) {
				dispatch_table[i].setup (this);
			} else {
				dispatch_table[i] = dispatch_table[OPCODE_UNKNOWN];
			}
		}

		return;
	}

}
