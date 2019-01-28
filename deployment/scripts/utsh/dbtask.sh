# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for task database operations (create/query/modify/delete).
# Note: These tests should start with the task collection empty.

n=100

# Tests for basic task creation and display

rundbut task$n aafs.ServerTest task_add_some

rundbut task$n aafs.ServerTest task_display_unsorted
rundbut task$n aafs.ServerTest task_display_list
rundbut task$n aafs.ServerTest task_display_first
rundbut task$n aafs.ServerTest task_display_iterate

rundbut task$n aafs.ServerTest task_query_list    0 0
rundbut task$n aafs.ServerTest task_query_iterate 0 0
rundbut task$n aafs.ServerTest task_query_first   0 0

# Tests for task query by execution time and event id

rundbut task$n aafs.ServerTest task_query_list    10010 0
rundbut task$n aafs.ServerTest task_query_iterate 10010 0
rundbut task$n aafs.ServerTest task_query_first   10010 0

rundbut task$n aafs.ServerTest task_query_list    30010 0
rundbut task$n aafs.ServerTest task_query_iterate 30010 0
rundbut task$n aafs.ServerTest task_query_first   30010 0

rundbut task$n aafs.ServerTest task_query_list    60010 0
rundbut task$n aafs.ServerTest task_query_iterate 60010 0
rundbut task$n aafs.ServerTest task_query_first   60010 0

rundbut task$n aafs.ServerTest task_query_list    0 10090
rundbut task$n aafs.ServerTest task_query_iterate 0 10090
rundbut task$n aafs.ServerTest task_query_first   0 10090

rundbut task$n aafs.ServerTest task_query_list    0 50090
rundbut task$n aafs.ServerTest task_query_iterate 0 50090
rundbut task$n aafs.ServerTest task_query_first   0 50090

rundbut task$n aafs.ServerTest task_query_list    0 60090
rundbut task$n aafs.ServerTest task_query_iterate 0 60090
rundbut task$n aafs.ServerTest task_query_first   0 60090

rundbut task$n aafs.ServerTest task_query_list    10010 10090
rundbut task$n aafs.ServerTest task_query_iterate 10010 10090
rundbut task$n aafs.ServerTest task_query_first   10010 10090

rundbut task$n aafs.ServerTest task_query_list    10010 50090
rundbut task$n aafs.ServerTest task_query_iterate 10010 50090
rundbut task$n aafs.ServerTest task_query_first   10010 50090

rundbut task$n aafs.ServerTest task_query_list    30010 50090
rundbut task$n aafs.ServerTest task_query_iterate 30010 50090
rundbut task$n aafs.ServerTest task_query_first   30010 50090

rundbut task$n aafs.ServerTest task_query_list    10010 60090
rundbut task$n aafs.ServerTest task_query_iterate 10010 60090
rundbut task$n aafs.ServerTest task_query_first   10010 60090

rundbut task$n aafs.ServerTest task_query_list    30010 60090
rundbut task$n aafs.ServerTest task_query_iterate 30010 60090
rundbut task$n aafs.ServerTest task_query_first   30010 60090

rundbut task$n aafs.ServerTest task_query_list    60010 60090
rundbut task$n aafs.ServerTest task_query_iterate 60010 60090
rundbut task$n aafs.ServerTest task_query_first   60010 60090

rundbut task$n aafs.ServerTest task_query_list    30100 50100
rundbut task$n aafs.ServerTest task_query_iterate 30100 50100
rundbut task$n aafs.ServerTest task_query_first   30100 50100

rundbut task$n aafs.ServerTest task_query_list    30101 50099
rundbut task$n aafs.ServerTest task_query_iterate 30101 50099
rundbut task$n aafs.ServerTest task_query_first   30101 50099

rundbut task$n aafs.ServerTest task_query_list    0 0 Event_4
rundbut task$n aafs.ServerTest task_query_iterate 0 0 Event_4
rundbut task$n aafs.ServerTest task_query_first   0 0 Event_4

rundbut task$n aafs.ServerTest task_query_list    0 0 Event_6
rundbut task$n aafs.ServerTest task_query_iterate 0 0 Event_6
rundbut task$n aafs.ServerTest task_query_first   0 0 Event_6

rundbut task$n aafs.ServerTest task_query_list    30010 0 Event_2
rundbut task$n aafs.ServerTest task_query_iterate 30010 0 Event_2
rundbut task$n aafs.ServerTest task_query_first   30010 0 Event_2

rundbut task$n aafs.ServerTest task_query_list    30010 0 Event_3
rundbut task$n aafs.ServerTest task_query_iterate 30010 0 Event_3
rundbut task$n aafs.ServerTest task_query_first   30010 0 Event_3

rundbut task$n aafs.ServerTest task_query_list    0 50090 Event_1
rundbut task$n aafs.ServerTest task_query_iterate 0 50090 Event_1
rundbut task$n aafs.ServerTest task_query_first   0 50090 Event_1

rundbut task$n aafs.ServerTest task_query_list    0 50090 Event_5
rundbut task$n aafs.ServerTest task_query_iterate 0 50090 Event_5
rundbut task$n aafs.ServerTest task_query_first   0 50090 Event_5

rundbut task$n aafs.ServerTest task_query_list    30010 50090 Event_4
rundbut task$n aafs.ServerTest task_query_iterate 30010 50090 Event_4
rundbut task$n aafs.ServerTest task_query_first   30010 50090 Event_4

rundbut task$n aafs.ServerTest task_query_list    30010 50090 Event_2
rundbut task$n aafs.ServerTest task_query_iterate 30010 50090 Event_2
rundbut task$n aafs.ServerTest task_query_first   30010 50090 Event_2

rundbut task$n aafs.ServerTest task_query_list    30010 50090 Event_6
rundbut task$n aafs.ServerTest task_query_iterate 30010 50090 Event_6
rundbut task$n aafs.ServerTest task_query_first   30010 50090 Event_6

# Tests for task activation, stage, delete

rundbut task$n aafs.ServerTest task_cutoff_activate    10010
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate    40010
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate    40010
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_stage    40010 60100 600
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_stage    40010 70100 700
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate    40010
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    40010
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    50010
rundbut task$n aafs.ServerTest task_query_list    0 0

# Delete any remaining tasks

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut task$n aafs.ServerTest task_query_list    0 0

rundbut task$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut task$n aafs.ServerTest task_query_list    0 0


