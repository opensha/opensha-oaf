# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for alias family database operations (create/query/delete).
# Note: These tests should start with the alias collection empty.

n=100

# Tests for basic alias creation and display

rundbut alfam$n aafs.ServerTest alias_add_some

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0

# Test for alias fetch by object id

rundbut alfam$n aafs.ServerTest alias_query_refetch    0 0 0

# Tests for alias query by action time and event id

rundbut alfam$n aafs.ServerTest alias_query_list    10010 0 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 10010 0 0
rundbut alfam$n aafs.ServerTest alias_query_first   10010 0 0

rundbut alfam$n aafs.ServerTest alias_query_list    30010 0 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 0 0
rundbut alfam$n aafs.ServerTest alias_query_first   30010 0 0

rundbut alfam$n aafs.ServerTest alias_query_list    60010 0 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 60010 0 0
rundbut alfam$n aafs.ServerTest alias_query_first   60010 0 0

rundbut alfam$n aafs.ServerTest alias_query_list    0 10090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 10090 0
rundbut alfam$n aafs.ServerTest alias_query_first   0 10090 0

rundbut alfam$n aafs.ServerTest alias_query_list    0 50090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 50090 0
rundbut alfam$n aafs.ServerTest alias_query_first   0 50090 0

rundbut alfam$n aafs.ServerTest alias_query_list    0 60090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 60090 0
rundbut alfam$n aafs.ServerTest alias_query_first   0 60090 0

rundbut alfam$n aafs.ServerTest alias_query_list    10010 10090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 10010 10090 0
rundbut alfam$n aafs.ServerTest alias_query_first   10010 10090 0

rundbut alfam$n aafs.ServerTest alias_query_list    10010 50090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 10010 50090 0
rundbut alfam$n aafs.ServerTest alias_query_first   10010 50090 0

rundbut alfam$n aafs.ServerTest alias_query_list    30010 50090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 50090 0
rundbut alfam$n aafs.ServerTest alias_query_first   30010 50090 0

rundbut alfam$n aafs.ServerTest alias_query_list    10010 60090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 10010 60090 0
rundbut alfam$n aafs.ServerTest alias_query_first   10010 60090 0

rundbut alfam$n aafs.ServerTest alias_query_list    30010 60090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 60090 0
rundbut alfam$n aafs.ServerTest alias_query_first   30010 60090 0

rundbut alfam$n aafs.ServerTest alias_query_list    60010 60090 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 60010 60090 0
rundbut alfam$n aafs.ServerTest alias_query_first   60010 60090 0

rundbut alfam$n aafs.ServerTest alias_query_list    30103 50105 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 30103 50105 0
rundbut alfam$n aafs.ServerTest alias_query_first   30103 50105 0

rundbut alfam$n aafs.ServerTest alias_query_list    30104 50104 0
rundbut alfam$n aafs.ServerTest alias_query_iterate 30104 50104 0
rundbut alfam$n aafs.ServerTest alias_query_first   30104 50104 0

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_1
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_1
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_1

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_2

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_3
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_3
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_3

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_4
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_4
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_4

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_5
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_5
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_5

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_6
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_6
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_6

rundbut alfam$n aafs.ServerTest alias_query_list    30010 0 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 0 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_first   30010 0 0 event_2

rundbut alfam$n aafs.ServerTest alias_query_list    30010 0 0 event_3
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 0 0 event_3
rundbut alfam$n aafs.ServerTest alias_query_first   30010 0 0 event_3

rundbut alfam$n aafs.ServerTest alias_query_list    0 50090 0 event_1
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 50090 0 event_1
rundbut alfam$n aafs.ServerTest alias_query_first   0 50090 0 event_1

rundbut alfam$n aafs.ServerTest alias_query_list    0 50090 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 50090 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_first   0 50090 0 event_2

rundbut alfam$n aafs.ServerTest alias_query_list    0 50090 0 event_5
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 50090 0 event_5
rundbut alfam$n aafs.ServerTest alias_query_first   0 50090 0 event_5

rundbut alfam$n aafs.ServerTest alias_query_list    30010 50090 0 event_4
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 50090 0 event_4
rundbut alfam$n aafs.ServerTest alias_query_first   30010 50090 0 event_4

rundbut alfam$n aafs.ServerTest alias_query_list    30010 50090 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 50090 0 event_2
rundbut alfam$n aafs.ServerTest alias_query_first   30010 50090 0 event_2

rundbut alfam$n aafs.ServerTest alias_query_list    30010 50090 0 event_6
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 50090 0 event_6
rundbut alfam$n aafs.ServerTest alias_query_first   30010 50090 0 event_6

# Tests for alias query by family time modulus

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 2000
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 2000
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 2000

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 2001
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 2001
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 2001

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 4000
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 4000
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 4000

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 4001
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 4001
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 4001

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 5003
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 5003
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 5003

rundbut alfam$n aafs.ServerTest alias_query_list    30010 50090 2001
rundbut alfam$n aafs.ServerTest alias_query_iterate 30010 50090 2001
rundbut alfam$n aafs.ServerTest alias_query_first   30010 50090 2001

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 2001 event_1
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 2001 event_1
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 2001 event_1

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 2001 event_2
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 2001 event_2
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 2001 event_2

# Tests for alias query by Comcat id

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_11
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_11
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_11

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_22
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_22
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_22

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_31
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_31
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_31

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_41
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_41
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_41

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - rmid_22
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - rmid_22
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - rmid_22

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_13
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_13
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_13

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_23
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_23
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_23

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - rmid_31
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - rmid_31
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - rmid_31

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_11 rmid_31
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_11 rmid_31
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_11 rmid_31

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - rmid_21 ccid_42
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - rmid_21 ccid_42
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - rmid_21 ccid_42

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - ccid_13 ccid_42
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - ccid_13 ccid_42
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - ccid_13 ccid_42

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 - rmid_21 ccid_31 ccid_11
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 - rmid_21 ccid_31 ccid_11
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 - rmid_21 ccid_31 ccid_11

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_4 ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_4 ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_4 ccid_13 ccid_23

rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0 event_3 ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_iterate 0 0 0 event_3 ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_first   0 0 0 event_3 ccid_13 ccid_23

rundbut alfam$n aafs.ServerTest alias_query_list    20010 50090 0 - ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_iterate 20010 50090 0 - ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_first   20010 50090 0 - ccid_13 ccid_23

rundbut alfam$n aafs.ServerTest alias_query_list    20010 50090 0 event_4 ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_iterate 20010 50090 0 event_4 ccid_13 ccid_23
rundbut alfam$n aafs.ServerTest alias_query_first   20010 50090 0 event_4 ccid_13 ccid_23

# Tests for alias delete

rundbut alfam$n aafs.ServerTest alias_query_delete  30010 50090 0
rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0

rundbut alfam$n aafs.ServerTest alias_query_delete  0 0 0 event_1
rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0

rundbut alfam$n aafs.ServerTest alias_query_delete  0 0 0
rundbut alfam$n aafs.ServerTest alias_query_list    0 0 0


