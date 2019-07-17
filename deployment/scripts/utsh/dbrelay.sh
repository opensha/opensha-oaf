# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for relay item database operations (create/query/delete).
# Note: These tests should start with the relay collection empty.

n=100

# Tests for basic alias creation and display

rundbut relit$n aafs.ServerTest relit_add_some

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

# Test for alias fetch by object id

rundbut relit$n aafs.ServerTest relit_query_refetch    false 0 0

# Tests for alias query by action time and event id

rundbut relit$n aafs.ServerTest relit_query_list    false 10010 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 10010 0
rundbut relit$n aafs.ServerTest relit_query_first   false 10010 0

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 0
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 0

rundbut relit$n aafs.ServerTest relit_query_list    false 60010 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 60010 0
rundbut relit$n aafs.ServerTest relit_query_first   false 60010 0

rundbut relit$n aafs.ServerTest relit_query_list    true 0 10090
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 10090
rundbut relit$n aafs.ServerTest relit_query_first   true 0 10090

rundbut relit$n aafs.ServerTest relit_query_list    true 0 50090
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 50090
rundbut relit$n aafs.ServerTest relit_query_first   true 0 50090

rundbut relit$n aafs.ServerTest relit_query_list    true 0 60090
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 60090
rundbut relit$n aafs.ServerTest relit_query_first   true 0 60090

rundbut relit$n aafs.ServerTest relit_query_list    false 10010 10090
rundbut relit$n aafs.ServerTest relit_query_iterate false 10010 10090
rundbut relit$n aafs.ServerTest relit_query_first   false 10010 10090

rundbut relit$n aafs.ServerTest relit_query_list    false 10010 50090
rundbut relit$n aafs.ServerTest relit_query_iterate false 10010 50090
rundbut relit$n aafs.ServerTest relit_query_first   false 10010 50090

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 50090
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 50090
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 50090

rundbut relit$n aafs.ServerTest relit_query_list    true 10010 60090
rundbut relit$n aafs.ServerTest relit_query_iterate true 10010 60090
rundbut relit$n aafs.ServerTest relit_query_first   true 10010 60090

rundbut relit$n aafs.ServerTest relit_query_list    true 30010 60090
rundbut relit$n aafs.ServerTest relit_query_iterate true 30010 60090
rundbut relit$n aafs.ServerTest relit_query_first   true 30010 60090

rundbut relit$n aafs.ServerTest relit_query_list    false 60010 60090
rundbut relit$n aafs.ServerTest relit_query_iterate false 60010 60090
rundbut relit$n aafs.ServerTest relit_query_first   false 60010 60090

rundbut relit$n aafs.ServerTest relit_query_list    false 30100 50100
rundbut relit$n aafs.ServerTest relit_query_iterate false 30100 50100
rundbut relit$n aafs.ServerTest relit_query_first   false 30100 50100

rundbut relit$n aafs.ServerTest relit_query_list    false 30101 50099
rundbut relit$n aafs.ServerTest relit_query_iterate false 30101 50099
rundbut relit$n aafs.ServerTest relit_query_first   false 30101 50099

rundbut relit$n aafs.ServerTest relit_query_list    true 0 0 Event_1
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 0 Event_1
rundbut relit$n aafs.ServerTest relit_query_first   true 0 0 Event_1

rundbut relit$n aafs.ServerTest relit_query_list    true 0 0 Event_2
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 0 Event_2
rundbut relit$n aafs.ServerTest relit_query_first   true 0 0 Event_2

rundbut relit$n aafs.ServerTest relit_query_list    true 0 0 Event_3
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 0 Event_3
rundbut relit$n aafs.ServerTest relit_query_first   true 0 0 Event_3

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0 Event_4
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0 Event_4
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0 Event_4

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0 Event_5
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0 Event_5
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0 Event_5

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0 Event_6
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0 Event_6
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0 Event_6

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 0 Event_2
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 0 Event_2
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 0 Event_2

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 0 Event_3
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 0 Event_3
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 0 Event_3

rundbut relit$n aafs.ServerTest relit_query_list    false 0 50090 Event_1
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 50090 Event_1
rundbut relit$n aafs.ServerTest relit_query_first   false 0 50090 Event_1

rundbut relit$n aafs.ServerTest relit_query_list    false 0 50090 Event_2
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 50090 Event_2
rundbut relit$n aafs.ServerTest relit_query_first   false 0 50090 Event_2

rundbut relit$n aafs.ServerTest relit_query_list    false 0 50090 Event_5
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 50090 Event_5
rundbut relit$n aafs.ServerTest relit_query_first   false 0 50090 Event_5

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 50090 Event_4
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 50090 Event_4
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 50090 Event_4

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 50090 Event_2
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 50090 Event_2
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 50090 Event_2

rundbut relit$n aafs.ServerTest relit_query_list    false 30010 50090 Event_6
rundbut relit$n aafs.ServerTest relit_query_iterate false 30010 50090 Event_6
rundbut relit$n aafs.ServerTest relit_query_first   false 30010 50090 Event_6

rundbut relit$n aafs.ServerTest relit_query_list    true 0 0 Event_1 Event_5 Event_3
rundbut relit$n aafs.ServerTest relit_query_iterate true 0 0 Event_1 Event_5 Event_3
rundbut relit$n aafs.ServerTest relit_query_first   true 0 0 Event_1 Event_5 Event_3

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0 Event_1 Event_6 Event_3
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0 Event_1 Event_6 Event_3
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0 Event_1 Event_6 Event_3

rundbut relit$n aafs.ServerTest relit_query_list    true 30010 50090 Event_1 Event_5 Event_3
rundbut relit$n aafs.ServerTest relit_query_iterate true 30010 50090 Event_1 Event_5 Event_3
rundbut relit$n aafs.ServerTest relit_query_first   true 30010 50090 Event_1 Event_5 Event_3

rundbut relit$n aafs.ServerTest relit_query_list    true 30010 50090 Event_1 Event_5 Event_2
rundbut relit$n aafs.ServerTest relit_query_iterate true 30010 50090 Event_1 Event_5 Event_2
rundbut relit$n aafs.ServerTest relit_query_first   true 30010 50090 Event_1 Event_5 Event_2

# Tests for update

rundbut relit$n aafs.ServerTest relit_add_one  Event_3 30090 Details_3_Before false 7001

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

rundbut relit$n aafs.ServerTest relit_add_one  Event_3 30110 Details_3_After false 7002

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

rundbut relit$n aafs.ServerTest relit_add_one  Event_2 20090 Details_2_Before false 7003

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

rundbut relit$n aafs.ServerTest relit_add_one  Event_2 20090 Details_2_Forced true 7004

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

rundbut relit$n aafs.ServerTest relit_add_one  Event_3 30110 Details_3_After false 7005

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

rundbut relit$n aafs.ServerTest relit_add_one  Event_3 30110 Details_2_After false 7006

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

rundbut relit$n aafs.ServerTest relit_add_one  Event_3 30110 Details_4_After false 7007

rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_iterate false 0 0
rundbut relit$n aafs.ServerTest relit_query_first   false 0 0

# Tests for delete

rundbut relit$n aafs.ServerTest relit_query_delete  false 30010 50090
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0

rundbut relit$n aafs.ServerTest relit_query_delete  true 0 0 Event_1
rundbut relit$n aafs.ServerTest relit_query_list    true 0 0

rundbut relit$n aafs.ServerTest relit_query_delete  false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0

# Tests for multiple cycles of change stream

rundbut relit$n aafs.ServerTest relit_add_cycles  20
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    true 0 0

rundbut relit$n aafs.ServerTest relit_query_delete  false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0

# Tests for relay thread

rundbut relit$n aafs.ServerTest relit_add_some
rundbut relit$n aafs.ServerTest relit_thread_dump
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    true 0 0

rundbut relit$n aafs.ServerTest relit_query_delete  false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0

rundbut relit$n aafs.ServerTest relit_thread_add_cycles  20
rundbut relit$n aafs.ServerTest relit_thread_dump
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    true 0 0

rundbut relit$n aafs.ServerTest relit_query_delete  false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0

rundbut relit$n aafs.ServerTest relit_thread_add_multi  20
rundbut relit$n aafs.ServerTest relit_thread_dump
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    true 0 0

rundbut relit$n aafs.ServerTest relit_query_delete  false 0 0
rundbut relit$n aafs.ServerTest relit_query_list    false 0 0




