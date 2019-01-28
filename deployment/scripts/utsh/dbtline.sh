# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for timeline database operations (create/query/delete).
# Note: These tests should start with the timeline collection empty.

n=100

# Tests for basic timeline creation and display

rundbut tline$n aafs.ServerTest tline_add_some

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0

# Test for timeline fetch by object id

rundbut tline$n aafs.ServerTest tline_query_refetch    0 0 0

# Tests for timeline query by action time and event id

rundbut tline$n aafs.ServerTest tline_query_list    10010 0 0
rundbut tline$n aafs.ServerTest tline_query_iterate 10010 0 0
rundbut tline$n aafs.ServerTest tline_query_first   10010 0 0

rundbut tline$n aafs.ServerTest tline_query_list    30010 0 0
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 0 0
rundbut tline$n aafs.ServerTest tline_query_first   30010 0 0

rundbut tline$n aafs.ServerTest tline_query_list    60010 0 0
rundbut tline$n aafs.ServerTest tline_query_iterate 60010 0 0
rundbut tline$n aafs.ServerTest tline_query_first   60010 0 0

rundbut tline$n aafs.ServerTest tline_query_list    0 10090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 0 10090 0
rundbut tline$n aafs.ServerTest tline_query_first   0 10090 0

rundbut tline$n aafs.ServerTest tline_query_list    0 50090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 0 50090 0
rundbut tline$n aafs.ServerTest tline_query_first   0 50090 0

rundbut tline$n aafs.ServerTest tline_query_list    0 60090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 0 60090 0
rundbut tline$n aafs.ServerTest tline_query_first   0 60090 0

rundbut tline$n aafs.ServerTest tline_query_list    10010 10090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 10010 10090 0
rundbut tline$n aafs.ServerTest tline_query_first   10010 10090 0

rundbut tline$n aafs.ServerTest tline_query_list    10010 50090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 10010 50090 0
rundbut tline$n aafs.ServerTest tline_query_first   10010 50090 0

rundbut tline$n aafs.ServerTest tline_query_list    30010 50090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 50090 0
rundbut tline$n aafs.ServerTest tline_query_first   30010 50090 0

rundbut tline$n aafs.ServerTest tline_query_list    10010 60090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 10010 60090 0
rundbut tline$n aafs.ServerTest tline_query_first   10010 60090 0

rundbut tline$n aafs.ServerTest tline_query_list    30010 60090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 60090 0
rundbut tline$n aafs.ServerTest tline_query_first   30010 60090 0

rundbut tline$n aafs.ServerTest tline_query_list    60010 60090 0
rundbut tline$n aafs.ServerTest tline_query_iterate 60010 60090 0
rundbut tline$n aafs.ServerTest tline_query_first   60010 60090 0

rundbut tline$n aafs.ServerTest tline_query_list    30103 50105 0
rundbut tline$n aafs.ServerTest tline_query_iterate 30103 50105 0
rundbut tline$n aafs.ServerTest tline_query_first   30103 50105 0

rundbut tline$n aafs.ServerTest tline_query_list    30104 50104 0
rundbut tline$n aafs.ServerTest tline_query_iterate 30104 50104 0
rundbut tline$n aafs.ServerTest tline_query_first   30104 50104 0

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 Event_4
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 Event_4
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 Event_4

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 Event_6
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 Event_6
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 Event_6

rundbut tline$n aafs.ServerTest tline_query_list    30010 0 0 Event_2
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 0 0 Event_2
rundbut tline$n aafs.ServerTest tline_query_first   30010 0 0 Event_2

rundbut tline$n aafs.ServerTest tline_query_list    30010 0 0 Event_3
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 0 0 Event_3
rundbut tline$n aafs.ServerTest tline_query_first   30010 0 0 Event_3

rundbut tline$n aafs.ServerTest tline_query_list    0 50090 0 Event_1
rundbut tline$n aafs.ServerTest tline_query_iterate 0 50090 0 Event_1
rundbut tline$n aafs.ServerTest tline_query_first   0 50090 0 Event_1

rundbut tline$n aafs.ServerTest tline_query_list    0 50090 0 Event_5
rundbut tline$n aafs.ServerTest tline_query_iterate 0 50090 0 Event_5
rundbut tline$n aafs.ServerTest tline_query_first   0 50090 0 Event_5

rundbut tline$n aafs.ServerTest tline_query_list    30010 50090 0 Event_4
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 50090 0 Event_4
rundbut tline$n aafs.ServerTest tline_query_first   30010 50090 0 Event_4

rundbut tline$n aafs.ServerTest tline_query_list    30010 50090 0 Event_2
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 50090 0 Event_2
rundbut tline$n aafs.ServerTest tline_query_first   30010 50090 0 Event_2

rundbut tline$n aafs.ServerTest tline_query_list    30010 50090 0 Event_6
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 50090 0 Event_6
rundbut tline$n aafs.ServerTest tline_query_first   30010 50090 0 Event_6

# Tests for timeline query by action time modulus

rundbut tline$n aafs.ServerTest tline_query_list    0 0 2000
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 2000
rundbut tline$n aafs.ServerTest tline_query_first   0 0 2000

rundbut tline$n aafs.ServerTest tline_query_list    0 0 2001
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 2001
rundbut tline$n aafs.ServerTest tline_query_first   0 0 2001

rundbut tline$n aafs.ServerTest tline_query_list    0 0 4000
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 4000
rundbut tline$n aafs.ServerTest tline_query_first   0 0 4000

rundbut tline$n aafs.ServerTest tline_query_list    0 0 4001
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 4001
rundbut tline$n aafs.ServerTest tline_query_first   0 0 4001

rundbut tline$n aafs.ServerTest tline_query_list    0 0 5003
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 5003
rundbut tline$n aafs.ServerTest tline_query_first   0 0 5003

rundbut tline$n aafs.ServerTest tline_query_list    30010 50090 2001
rundbut tline$n aafs.ServerTest tline_query_iterate 30010 50090 2001
rundbut tline$n aafs.ServerTest tline_query_first   30010 50090 2001

rundbut tline$n aafs.ServerTest tline_query_list    0 0 2001 Event_1
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 2001 Event_1
rundbut tline$n aafs.ServerTest tline_query_first   0 0 2001 Event_1

rundbut tline$n aafs.ServerTest tline_query_list    0 0 2001 Event_2
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 2001 Event_2
rundbut tline$n aafs.ServerTest tline_query_first   0 0 2001 Event_2

# Tests for timeline query by Comcat id

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_11
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_11
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_11

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_22
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_22
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_22

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_31
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_31
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_31

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_43
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_43
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_43

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_52
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_52
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_52

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_13
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_13
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_13

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_23
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_23
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_23

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_11 ccid_42
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_11 ccid_42
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_11 ccid_42

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_13 ccid_23

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 - ccid_21 ccid_32 ccid_41
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 - ccid_21 ccid_32 ccid_41
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 - ccid_21 ccid_32 ccid_41

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 Event_4 ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 Event_4 ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 Event_4 ccid_13 ccid_23

rundbut tline$n aafs.ServerTest tline_query_list    0 0 0 Event_3 ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_iterate 0 0 0 Event_3 ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_first   0 0 0 Event_3 ccid_13 ccid_23

rundbut tline$n aafs.ServerTest tline_query_list    20010 50090 0 - ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_iterate 20010 50090 0 - ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_first   20010 50090 0 - ccid_13 ccid_23

rundbut tline$n aafs.ServerTest tline_query_list    20010 50090 0 Event_4 ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_iterate 20010 50090 0 Event_4 ccid_13 ccid_23
rundbut tline$n aafs.ServerTest tline_query_first   20010 50090 0 Event_4 ccid_13 ccid_23

# Tests for timeline delete

rundbut tline$n aafs.ServerTest tline_query_delete  30010 50090 0
rundbut tline$n aafs.ServerTest tline_query_list    0 0 0

rundbut tline$n aafs.ServerTest tline_query_delete  0 0 0 Event_1
rundbut tline$n aafs.ServerTest tline_query_list    0 0 0

rundbut tline$n aafs.ServerTest tline_query_delete  0 0 0
rundbut tline$n aafs.ServerTest tline_query_list    0 0 0


