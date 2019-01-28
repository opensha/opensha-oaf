# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for alias family tracking operations (rename/merge/split/stop/revive).
# Note: These tests should start with the task and alias collections empty.

n=100

# Event id query, for nothing found

rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0

# Nepal
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us20002926
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  gcmt20150425061126
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  atlas20150425061125
# South Napa
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  nc72282711
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  at00nat3ek
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  usb000s5tp
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  atlas20140824102044
# M5.7 Tonga
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us1000ehmb
# M5.3 Volcano Hawaii
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  hv70307431
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us1000eze5
# M5.9 Mayotte
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us1000e5k1
# Invalid
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  badevent

rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0

# Event id query, followed by timeline creation

# Nepal
rundbut aliasfam$n aafs.ServerTest alias_create_timeline_for_event  us20002926
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
# South Napa
rundbut aliasfam$n aafs.ServerTest alias_create_timeline_for_event  usb000s5tp
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
# M5.7 Tonga
rundbut aliasfam$n aafs.ServerTest alias_create_timeline_for_event  us1000ehmb
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
# M5.3 Volcano Hawaii
rundbut aliasfam$n aafs.ServerTest alias_create_timeline_for_event  us1000eze5
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
# M5.3 Volcano Hawaii
rundbut aliasfam$n aafs.ServerTest alias_create_timeline_for_event  hv70307431
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
# Invalid
rundbut aliasfam$n aafs.ServerTest alias_create_timeline_for_event  badevent
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0

# Event id query, find existing

# Nepal
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us20002926
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  gcmt20150425061126
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  atlas20150425061125
# South Napa
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  nc72282711
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  at00nat3ek
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  usb000s5tp
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  atlas20140824102044
# M5.7 Tonga
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us1000ehmb
# M5.3 Volcano Hawaii
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  hv70307431
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us1000eze5
# M5.9 Mayotte
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  us1000e5k1
# Invalid
rundbut aliasfam$n aafs.ServerTest alias_get_event_info  badevent

rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0

# Timeline id query, find existing

# Nepal
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .us20002926
# South Napa
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .nc72282711
# M5.7 Tonga
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .us1000ehmb
# M5.3 Volcano Hawaii
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .hv70307431
# M5.9 Mayotte
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .us1000e5k1
# Invalid
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .badevent

rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0

# Alias operations

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft01.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft02.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft03.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft04.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft05.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft06.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .b
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft06.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .b
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft07.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft08.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft09.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft10a.txt"
rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft10b.txt"
rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft10c.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft11a.txt"
rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft11b.txt"
rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft11c.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest alias_add_from_file  "$TSDIR/aliasfamily/aft12.txt"
rundbut aliasfam$n aafs.ServerTest alias_query_list    0 0 0
rundbut aliasfam$n aafs.ServerTest alias_get_timeline_info  .a
rundbut aliasfam$n aafs.ServerTest alias_query_delete    0 0 0
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

# Delete any remaining tasks

rundbut aliasfam$n aafs.ServerTest task_cutoff_activate_delete    999999999999999
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest task_cutoff_activate_delete    999999999999999
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest task_cutoff_activate_delete    999999999999999
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest task_cutoff_activate_delete    999999999999999
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0

rundbut aliasfam$n aafs.ServerTest task_cutoff_activate_delete    999999999999999
rundbut aliasfam$n aafs.ServerTest task_query_list    0 0


