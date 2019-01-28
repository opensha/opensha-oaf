# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for log database operations (create/query/delete).
# Note: These tests should start with the task and log collections empty.

n=100

# Create some tasks to use

rundbut log$n aafs.ServerTest task_add_some
rundbut log$n aafs.ServerTest task_query_list    0 0

# Tests for basic log creation and display

rundbut log$n aafs.ServerTest log_add_from_tasks

rundbut log$n aafs.ServerTest log_search_for_tasks

rundbut log$n aafs.ServerTest log_query_list    0 0
rundbut log$n aafs.ServerTest log_query_iterate 0 0

# Tests for log query by execution time and event id

rundbut log$n aafs.ServerTest log_query_list    10110 0
rundbut log$n aafs.ServerTest log_query_iterate 10110 0

rundbut log$n aafs.ServerTest log_query_list    30110 0
rundbut log$n aafs.ServerTest log_query_iterate 30110 0

rundbut log$n aafs.ServerTest log_query_list    60110 0
rundbut log$n aafs.ServerTest log_query_iterate 60110 0

rundbut log$n aafs.ServerTest log_query_list    0 10190
rundbut log$n aafs.ServerTest log_query_iterate 0 10190

rundbut log$n aafs.ServerTest log_query_list    0 50190
rundbut log$n aafs.ServerTest log_query_iterate 0 50190

rundbut log$n aafs.ServerTest log_query_list    0 60190
rundbut log$n aafs.ServerTest log_query_iterate 0 60190

rundbut log$n aafs.ServerTest log_query_list    10110 10190
rundbut log$n aafs.ServerTest log_query_iterate 10110 10190

rundbut log$n aafs.ServerTest log_query_list    10110 50190
rundbut log$n aafs.ServerTest log_query_iterate 10110 50190

rundbut log$n aafs.ServerTest log_query_list    30110 50190
rundbut log$n aafs.ServerTest log_query_iterate 30110 50190

rundbut log$n aafs.ServerTest log_query_list    10110 60190
rundbut log$n aafs.ServerTest log_query_iterate 10110 60190

rundbut log$n aafs.ServerTest log_query_list    30110 60190
rundbut log$n aafs.ServerTest log_query_iterate 30110 60190

rundbut log$n aafs.ServerTest log_query_list    60110 60190
rundbut log$n aafs.ServerTest log_query_iterate 60110 60190

rundbut log$n aafs.ServerTest log_query_list    30200 50200
rundbut log$n aafs.ServerTest log_query_iterate 30200 50200

rundbut log$n aafs.ServerTest log_query_list    30201 50199
rundbut log$n aafs.ServerTest log_query_iterate 30201 50199

rundbut log$n aafs.ServerTest log_query_list    0 0 Event_4
rundbut log$n aafs.ServerTest log_query_iterate 0 0 Event_4

rundbut log$n aafs.ServerTest log_query_list    0 0 Event_6
rundbut log$n aafs.ServerTest log_query_iterate 0 0 Event_6

rundbut log$n aafs.ServerTest log_query_list    30110 0 Event_2
rundbut log$n aafs.ServerTest log_query_iterate 30110 0 Event_2

rundbut log$n aafs.ServerTest log_query_list    30110 0 Event_3
rundbut log$n aafs.ServerTest log_query_iterate 30110 0 Event_3

rundbut log$n aafs.ServerTest log_query_list    0 50190 Event_1
rundbut log$n aafs.ServerTest log_query_iterate 0 50190 Event_1

rundbut log$n aafs.ServerTest log_query_list    0 50190 Event_5
rundbut log$n aafs.ServerTest log_query_iterate 0 50190 Event_5

rundbut log$n aafs.ServerTest log_query_list    30110 50190 Event_4
rundbut log$n aafs.ServerTest log_query_iterate 30110 50190 Event_4

rundbut log$n aafs.ServerTest log_query_list    30110 50190 Event_2
rundbut log$n aafs.ServerTest log_query_iterate 30110 50190 Event_2

rundbut log$n aafs.ServerTest log_query_list    30110 50190 Event_6
rundbut log$n aafs.ServerTest log_query_iterate 30110 50190 Event_6

# Tests for log delete

rundbut log$n aafs.ServerTest log_query_list_delete    60110 0
rundbut log$n aafs.ServerTest log_query_list    0 0

rundbut log$n aafs.ServerTest log_query_list_delete    0 10190
rundbut log$n aafs.ServerTest log_query_list    0 0

rundbut log$n aafs.ServerTest log_query_list_delete    30110 50190
rundbut log$n aafs.ServerTest log_query_list    0 0

rundbut log$n aafs.ServerTest log_query_list_delete    0 0 Event_6
rundbut log$n aafs.ServerTest log_query_list    0 0

rundbut log$n aafs.ServerTest log_query_list_delete    0 0 Event_2
rundbut log$n aafs.ServerTest log_query_list    0 0

rundbut log$n aafs.ServerTest log_query_list_delete    0 0 Event_2
rundbut log$n aafs.ServerTest log_query_list    0 0

rundbut log$n aafs.ServerTest log_query_list_delete    0 0
rundbut log$n aafs.ServerTest log_query_list    0 0

# Delete the tasks

rundbut log$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut log$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut log$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut log$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut log$n aafs.ServerTest task_cutoff_activate_delete    999999999
rundbut log$n aafs.ServerTest task_query_list    0 0


