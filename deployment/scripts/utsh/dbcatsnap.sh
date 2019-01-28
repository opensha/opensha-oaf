# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for catalog snapshot database operations (create/query/delete).
# Note: These tests should start with the catalog snapshot collection empty.

n=100

# Tests for basic catalog snapshot creation and display

rundbut catsnap$n aafs.ServerTest catsnap_add    0.01   1.0  Event_1
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.0  0.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.0  0.0

rundbut catsnap$n aafs.ServerTest catsnap_add    0.03   3.0  Event_2
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.0  0.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.0  0.0

rundbut catsnap$n aafs.ServerTest catsnap_add    0.07   7.0  Event_3
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.0  0.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.0  0.0

rundbut catsnap$n aafs.ServerTest catsnap_add    0.14  14.0  Event_4
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.0  0.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.0  0.0

rundbut catsnap$n aafs.ServerTest catsnap_add    0.30  30.0  Event_5
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.0  0.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.0  0.0

# Test for catalog snapshot fetch by object id

rundbut catsnap$n aafs.ServerTest catsnap_query_refetch 0.0  0.0

# Tests for catalog snapshot verbose list

rundbut catsnap$n aafs.ServerTest catsnap_query_verbose 0.0  0.0  Event_1
rundbut catsnap$n aafs.ServerTest catsnap_query_verbose 0.0  0.0  Event_2
rundbut catsnap$n aafs.ServerTest catsnap_query_verbose 0.0  0.0  Event_3
rundbut catsnap$n aafs.ServerTest catsnap_query_verbose 0.0  0.0  Event_4
rundbut catsnap$n aafs.ServerTest catsnap_query_verbose 0.0  0.0  Event_5

# Tests for catalog snapshot query by end time and event id

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.5  0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.5  0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    32.0  0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 32.0  0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0  0.7
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0  0.7

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0  29.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0  29.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0  35.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0  35.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.5  0.7
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.5  0.7

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.5  29.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.5  29.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  29.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  29.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0.5  35.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0.5  35.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  35.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  35.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    32.0  35.0
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 32.0  35.0

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0 0  Event_4
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0 0  Event_4

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0 0  Event_6
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0 0  Event_6

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  0  Event_2
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  0  Event_2

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  0  Event_3
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  0  Event_3

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0  29.0  Event_1
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0  29.0  Event_1

rundbut catsnap$n aafs.ServerTest catsnap_query_list    0  29.0  Event_5
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 0  29.0  Event_5

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  29.0  Event_4
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  29.0  Event_4

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  29.0  Event_2
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  29.0  Event_2

rundbut catsnap$n aafs.ServerTest catsnap_query_list    5.0  29.0  Event_6
rundbut catsnap$n aafs.ServerTest catsnap_query_iterate 5.0  29.0  Event_6

# Tests for catalog snapshot delete

rundbut catsnap$n aafs.ServerTest catsnap_query_delete  5.0  29.0
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0 0

rundbut catsnap$n aafs.ServerTest catsnap_query_delete  0 0  Event_1
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0 0

rundbut catsnap$n aafs.ServerTest catsnap_query_delete  0 0
rundbut catsnap$n aafs.ServerTest catsnap_query_list    0 0


