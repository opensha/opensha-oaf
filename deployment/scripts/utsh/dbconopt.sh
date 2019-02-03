# Note: This file is intended to be sourced by utoaf.sh.

# Unit tests for task database operations (create/query/modify/delete).
# Note: These tests should start with the task collection empty.

n=100

# Test for setting of connection option, by displaying task list

rundbut conopt$n aafs.ServerTest traced_task_display_list


