#! /bin/bash

# This script is used to start, stop, and manage the AAFS server.

# This script must be run in the aftershock user account.

# COMMANDS FOR DISPLAYING STATUS
#
# check_running
#
#     Check if MongoDB and the AAFS application are currently running.
#
# show_version
#
#     Display version information about the installed AAFS software.
#     AAFS does not need to be running.

# COMMANDS FOR STARTING AND STOPPING
#
# start_mongo
#
#     Start MongoDB.  Perform no operation if MongoDB is already started.
#
# stop_mongo
#
#     Stop MongoDB.  Perform no operation if MongoDB is already stopped.
#
# start_aafs
#
#     Start AAFS.  Perform no operation if AAFS is already started.
#     This command does not start MongoDB, and so MongoDB needs to be
#     started before using this command.
#
# stop_aafs
#
#     Stop AAFS.  Perform no operation if AAFS is already stopped.
#     This command does not stop MongoDB.
#
# start_all
#
#     Start MongoDB, and then start AAFS.  Perform no operation if AAFS is
#     already started.  If MongoDB is already started, then just start AAFS.
#
# stop_all
#
#     Stop AAFS, and then stop MongoDB.  If AAFS is already stopped, then
#     just stop MongoDB.  If both AAFS and MongoDB are already stopped,
#     then perform no operation.
#
# start_aafs_no_intake
#
#     Start AAFS, without starting the intake processes (PDL and polling).
#     Perform no operation if AAFS is already started.
#     This command is intended to be used for debug and test.
#     This command does not start MongoDB, and so MongoDB needs to be
#     started before using this command.
#
# stop_aafs_no_intake
#
#     Stop AAFS, assuming that there is no intake process running (PDL).
#     Perform no operation if AAFS is already stopped.
#     This command is intended to be used for debug and test.
#     This command does not stop MongoDB.

# COMMANDS TO MANAGE THE DATABASE
#
# initdb
#
#     Initialize the database.  This command creates the database collections
#     and indexes that AAFS needs.  You should run this command once when setting
#     up a new installation, or after erasing the the database to start fresh.
#
#     You must start MongoDB before running this command.
#
# rebuild_indexes
#
#     Delete the existing database indexes, and build new indexes.  This command
#     does not delete any data.
#
#     You must start MongoDB before running this command.
#
# check_collections
#
#     Check if the database collections required by AAFS exist.  (The initdb command
#     creates the database collections.)
#
#     You must start MongoDB before running this command.
#
# backup_database  <filename>
#
#     Make a backup of the entire database, and write it to a file.
#
#     <filename> is the name of the file.  If the file already exists, you
#     are prompted on whether or not to overwrite it.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.
#
# backup_database_gzip  <filename>
#
#     Make a backup of the entire database, compress it, and write it to a file.
#
#     <filename> is the name of the file.  If the file already exists, you
#     are prompted on whether or not to overwrite it.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.
#     This command performs the same function as backup_database except that
#     the output file is also compressed with gzip.
#
# restore_database  <filename>
#
#     Restore the entire database from a file.
#
#     <filename> is the name of the file.  The file must have been created
#     by the backup_database command.
#
#     When running this command, the database must be completely empty, either
#     because it is a new installation of MongoDB, or because you erased the database.
#
#     This command also creates the database collections and indexes required by AAFS.
#     So, you should NOT use the initdb command before running this command.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.
#
# restore_database_gzip  <filename>
#
#     Restore the entire database from a compressed file.
#
#     <filename> is the name of the file.  The file must have been created
#     by the backup_database_gzip command.
#
#     When running this command, the database must be completely empty, either
#     because it is a new installation of MongoDB, or because you erased the database.
#
#     This command also creates the database collections and indexes required by AAFS.
#     So, you should NOT use the initdb command before running this command.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.
#     This command performs the same function as restore_database except that
#     the input file is also uncompressed with gzip.
#
# erase_database_this_is_irreversible
#
#     Erase the entire database from MongoDB.
#
#     This action cannot be undone.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.

# COMMANDS TO MANAGE SERVER RELAY (FOR DUAL-SERVER CONFIGURATIONS)
#
# init_relay_mode  <relay_mode>  <configured_primary>
#
#     Initialize the server relay mode.  This command is used primarily in
#     dual-server configurations.  It sets the initial relay mode that AAFS
#     will assume when it starts.
#
#     This command works by writing the initial server relay mode into the database.
#     When AAFS is started, it reads the initial mode from the database.
#
#     <relay_mode> is the desired mode.  It can have one of three values:
#
#       solo - AAFS operates in single-server mode.  This is the default.
#
#       pair - AAFS operates in dual-server mode.  Each server continuously
#              monitors the other, to remain synchronized.  Only the primary server
#              sends forecasts to PDL.  If the primary server goes off-line, then
#              the secondary server automatically steps up to become primary and
#              begins sending forecasts to PDL.
#
#       watch - AAFS operates in dual-server mode.  Each server continuously
#               monitors the other, to remain synchronized.  Only the primary server
#               sends forecasts to PDL.  Unlike pair mode, there is no automatic
#               switching between secondary and primary;  each server operates as
#               configured.  This is useful for maintenance procedures where it is
#               helpful to control each server's primary/secondary status.
#
#     <configured_primary> selects which server is primary. It can have four values:
#
#       1 - Server #1 is the primary server.
#
#       2 - Server #2 is the primary server.
#
#       this - This server is the primary server.  It is the same as 1 or 2,
#              depending on whether this server is server #1 or server #2.
#
#       other - The other server is the primary server, in a dual-server
#               configuration.  It is the same as 1 or 2, depending on whether
#               this server is server #1 or server #2.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.
#     In a single-server configuration, the server is considered to be server #1.
#
# change_relay_mode  <srvnum>  <relay_mode>  <configured_primary>
#
#     Change the server relay mode in a running system.  This command is used
#     primarily in dual-server configurations.  It changes the relay mode while AAFS
#     is running.
#
#     This command works by writing a change request into the database. When
#     AAFS sees the change request, it changes its relay mode.  This usually
#     happens in about 15 seconds, but sometimes takes longer.
#
#     Notice that the relay mode does not change instantaneously.  The command
#     waits for acknowledgement that the mode has changed.  You can use
#     the show_relay_status command to confirm the change has taken effect.
#
#     <srvnum> is the server to which you are sending the change request.
#     It can have one of eight values:
#
#       0 - Send the change request to the local server (the server on which you
#           are running the command).
#
#       1 - Send the change request to server #1 in a dual-server configuration,
#           or to the only server in a single-server configuration.
#
#       2 - Send the change request to server #2 in a dual-server configuration.
#
#       9 - Send the change request to both servers in a dual-server configuration.
#           In a dual-server configuration, it is recommended that you send the
#           change request to both servers.  (If you only send the change request
#           to one server, the change is automatically relayed to the other server,
#           however, sending directly to both servers is preferred.)
#
#       local - Same as 0.
#
#       this - Send the change request to this server.  It is the same as 1 or 2,
#              depending on whether this server is server #1 or server #2.
#
#       other - Send the change request to the other server, in a dual-server
#               configuration.  It is the same as 1 or 2, depending on whether
#               this server is server #1 or server #2.
#
#       both - Same as 9.
#
#     <relay_mode> is the desired mode.  It can have one of three values:
#
#       solo - AAFS operates in single-server mode.  This is the default.
#
#       pair - AAFS operates in dual-server mode.  Each server continuously
#              monitors the other, to remain synchronized.  Only the primary server
#              sends forecasts to PDL.  If the primary server goes off-line, then
#              the secondary server automatically steps up to become primary and
#              begins sending forecasts to PDL.
#
#       watch - AAFS operates in dual-server mode.  Each server continuously
#               monitors the other, to remain synchronized.  Only the primary server
#               sends forecasts to PDL.  Unlike pair mode, there is no automatic
#               switching between secondary and primary;  each server operates as
#               configured.  This is useful for maintenance procedures where it is
#               helpful to control each server's primary/secondary status.
#
#     <configured_primary> selects which server is primary. It can have four values:
#
#       1 - Server #1 is the primary server.
#
#       2 - Server #2 is the primary server.
#
#       this - This server is the primary server.  It is the same as 1 or 2,
#              depending on whether this server is server #1 or server #2.
#
#       other - The other server is the primary server, in a dual-server
#               configuration.  It is the same as 1 or 2, depending on whether
#               this server is server #1 or server #2.
#
#     You must start AAFS before running this command.
#     In a single-server configuration, the server is considered to be server #1.
#
# show_relay_status  <srvnum>
#
#     Display the server relay status.  This command is used primarily in
#     dual-server configurations. 
#
#     This command works by reading status information from the database.
#     So, it can be used regardless of whether or not AAFS is running, but
#     it requires that MongoDB be running.
#
#     <srvnum> is the server to which you are sending the status request.
#     It can have one of eight values:
#
#       0 - Send the status request to the local server (the server on which you
#           are running the command).
#
#       1 - Send the status request to server #1 in a dual-server configuration,
#           or to the only server in a single-server configuration.
#
#       2 - Send the status request to server #2 in a dual-server configuration.
#
#       9 - Send the status request to both servers in a dual-server configuration.
#           The command first obtains status from server #1, and then from server #2.
#
#       local - Same as 0.
#
#       this - Send the status request to this server.  It is the same as 1 or 2,
#              depending on whether this server is server #1 or server #2.
#
#       other - Send the status request to the other server, in a dual-server
#               configuration.  It is the same as 1 or 2, depending on whether
#               this server is server #1 or server #2.
#
#       both - Same as 9.
#
#     The status information consists of three lines.
#
#       1. The first line shows server information.  It includes:
#          * The server number, "N1" for server #1, or "N2" for server #2.
#          * The AAFS software version number, for example "V1.0.1145".
#          * The relay communication protocol number, for example "C101".
#          * The time that the server was started.
#
#       2. The second line shows the server configuration.  It includes:
#          * The configured relay mode, which can be:
#            "RMODE_SOLO" - solo mode.
#            "RMODE_PAIR" - pair mode.
#            "RMODE_WATCH" - watch mode.
#          * The configured primary server, "P1" for server #1, or "P2" for server #2.
#          * The time at which the configuration was established.
#
#       3. The third line shows the current server state.  It includes:
#          * The current link status, which can be:
#            "LINK_SHUTDOWN" - AAFS is not running.
#            "LINK_SOLO" - AAFS is running in single-server mode, and so there is
#                          no connection to another server.
#            "LINK_DISCONNECTED" - AAFS running in dual-server mode, and there is
#                                  currently no connection to the other server.
#            "LINK_CALLING" - AAFS is running in dual-server mode, and the server is
#                             currently attempting to contact the other server.
#            "LINK_INITIAL_SYNC" - AAFS is running in dual-server mode, and the server
#                                  has just made contact with the other server and
#                                  is performing an initial synchronization.
#            "LINK_CONNECTED" - AAFS is running in dual-server mode, and there is a
#                               normal connection establised with the other server.
#            "LINK_RESYNC" - AAFS is running in dual-server mode, there is a normal
#                            connection established with the other server, and
#                            the server is currently re-synchronizing.  It is normal
#                            for the servers to re-synchronize periodically.  Note
#                            that either LINK_CONNECTED or LINK_RESYNC indicates
#                            that a normal connection has been established (but
#                            LINK_INITIAL_SYNC does not).
#          * The current primary selection status, which can be:
#            "PRIST_PRIMARY" - The server is currenlty running as a primary server.
#            "PRIST_SECONDARY" - The server is currenlty running as a secondary server.
#            "PRIST_INITIALIZING" - The server is currently initializing and has not
#                                   yet selected if it is primary or secondary.
#            "PRIST_SHUTDOWN" - The server is currently shut down.
#          * The time of the last server heartbeat.  Heartbeats normally occur every
#            five minutes, but can be longer if the server is busy.  The heartbeat
#            is considered stale if it is 45 minutes old.
#
#     You must start MongoDB before running this command.
#     In a single-server configuration, the server is considered to be server #1.
#
# start_secondary_aafs
#
#     Start AAFS, on the secondary server in a dual-server configuration.
#
#     The intent is that this command can be used to restart a server that was stopped
#     with the stop_secondary_aafs command.  This command does not change the relay
#     mode, so it is still "watch other".  You should use change_relay_mode to restore
#     the normal "pair 1" mode.
#
#     This command does not start MongoDB, and so MongoDB needs to be
#     started before using this command.
#
# stop_secondary_aafs
#
#     Set this server to be the secondary server in a dual-server configuration,
#     and then stop AAFS.  This command switches the relay mode to "watch other"
#     (which makes this server secondary and the other server primary), then waits for
#     both servers to acknowledge the change, then stops AAFS, and then waits for the
#     other server to acknowledge that it has seen this server shut down.
#
#     The intent is that this command can be used to stop one server in a dual-server
#     configuration, while the other server continues to operate as the primary server.
#     It should only be used when both servers are operating normally.
# 
#     Perform no operation if AAFS is already stopped.
#     This command does not stop MongoDB.

# COMMANDS TO ADJUST ANALYST PARAMETERS
#
# init_analyst_cli
#
#     Initialize analyst parameters for an earthquake.  This command launches the analyst
#     CLI, and then writes the analyst parameters into the database on this server.
#     When AAFS is started, it uses these parameters the next time it generates AAFS
#     forecast for the earthquake.
#
#     This command may be used before starting a server to ensure that all forecasts
#     for the earthquake use the analyst-selected parameters.
#
#     You must start MongoDB before running this command.
#     You cannot run this command while AAFS is running.
#
# analyst_cli  <srvnum>
#
#     Change analyst parameters in a running system.  This command launces the analyst
#     CLI to obtain parameters, and then writes a change request into the database.
#     When AAFS sees the change request, it changes the analyst parameters for the
#     earthquake, and possibly schedules a forecast.  This usually happens in about
#     15 seconds, but sometimes takes longer.
#
#     <srvnum> is the server to which you are sending the change request.
#     It can have one of eight values:
#
#       0 - Send the change request to the local server (the server on which you
#           are running the command).
#
#       1 - Send the change request to server #1 in a dual-server configuration,
#           or to the only server in a single-server configuration.
#
#       2 - Send the change request to server #2 in a dual-server configuration.
#
#       9 - Send the change request to both servers in a dual-server configuration.
#           In a dual-server configuration, it is recommended that you send the
#           change request to both servers.  (If you only send the change request
#           to one server, the change is automatically relayed to the other server,
#           however, sending directly to both servers is preferred.)
#
#       local - Same as 0.
#
#       this - Send the change request to this server.  It is the same as 1 or 2,
#              depending on whether this server is server #1 or server #2.
#
#       other - Send the change request to the other server, in a dual-server
#               configuration.  It is the same as 1 or 2, depending on whether
#               this server is server #1 or server #2.
#
#       both - Same as 9.
#
#     You must start AAFS before running this command.
#     In a single-server configuration, the server is considered to be server #1.



# Read the launch options file, if it exists, and set our options.
# jmem = Amount of memory to use for Java when starting AAFS, in GB.

if [ -f "/opt/aafs/oafcfg/LaunchOptions.sh" ]; then
    source /opt/aafs/oafcfg/LaunchOptions.sh
fi

if [ -z "$MOAF_JAVA_MAX_MEMORY_GB" ]; then
    jmem="0"
else
    jmem="$MOAF_JAVA_MAX_MEMORY_GB"
fi




# Function to test if MongoDB is running.
# There are no arguments.
# Return value is 0 if MongoDB is running, non-zero if not.

is_mongo_running () {
    pgrep mongod >/dev/null 2>&1
}




# Function to test if AAFS is running.
# There are no arguments.
# Return value is 0 if AAFS is running, non-zero if not.

is_aafs_running () {
    ps -eF | grep -q "/opt/aafs/oefjava/[o]efjava"
}




# Function to start MongoDB.
# There are no arguments.
# The caller should check that MongoDB is not running before calling this function.

do_start_mongo () {
    echo "Starting MongoDB..."
    sudo /usr/sbin/service mongod start
}




# Function to stop MongoDB.
# There are no arguments.
# The caller should check that MongoDB is running before calling this function.

do_stop_mongo () {
    echo "Stopping MongoDB..."
    sudo /usr/sbin/service mongod stop
}




# Function to show AAFS software version number
# There are no arguments.

do_show_version () {
    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd show_version 2>&1
}




# Function to start AAFS.
# There are no arguments.
# The caller should check that AAFS is not running before calling this function.

do_start_aafs () {
    echo "Starting AAFS server..."
    if [ ! -d /data/aafs/logs ]; then
        mkdir /data/aafs/logs
    fi
    if [ ! -d /data/aafs/pids ]; then
        mkdir /data/aafs/pids
    fi
    if [ "$jmem" -gt 0 ]; then
        nohup /usr/local/java/bin/java -Xmx${jmem}G -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start >> /data/aafs/logs/aafs.log 2>&1 & echo $! > /data/aafs/pids/aafs.pid
    else
        nohup /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start >> /data/aafs/logs/aafs.log 2>&1 & echo $! > /data/aafs/pids/aafs.pid
    fi
    echo "Pausing 30 seconds..."
    sleep 30
    echo "Removing old PDL listener data..."
    if [ -d /data/aafs/pdldata ]; then
        rm -r /data/aafs/pdldata
    fi
    mkdir /data/aafs/pdldata
    echo "Starting PDL listener..."
    cd /opt/aafs/intake
    /opt/aafs/intake/init.sh start
    cd -
    echo "Pausing 30 seconds..."
    sleep 30
    echo "Starting Comcat poll..."
    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start_comcat_poll 2>&1
    echo "Pausing 15 seconds..."
    sleep 15
    echo "Starting PDL cleanup..."
    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start_pdl_cleanup 2>&1
}




# Function to stop AAFS.
# There are no arguments.
# The caller should check that AAFS is running before calling this function.

do_stop_aafs () {
    echo "Stopping PDL listener..."
    cd /opt/aafs/intake
    /opt/aafs/intake/init.sh stop
    cd -
    echo "Pausing 30 seconds..."
    sleep 30
    echo "Stopping AAFS server..."
    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd stop 2>&1
    if [ -f /data/aafs/pids/aafs.pid ]; then
        rm /data/aafs/pids/aafs.pid
    fi
    echo "Waiting for AAFS server to terminate..."
    sleep 2
    while is_aafs_running ; do
        sleep 2
    done
}




# Function to start AAFS, without starting intake processes (PDL and polling).
# There are no arguments.
# The caller should check that AAFS is not running before calling this function.

do_start_aafs_no_intake () {
    echo "Starting AAFS server..."
    if [ ! -d /data/aafs/logs ]; then
        mkdir /data/aafs/logs
    fi
    if [ ! -d /data/aafs/pids ]; then
        mkdir /data/aafs/pids
    fi
    if [ "$jmem" -gt 0 ]; then
        nohup /usr/local/java/bin/java -Xmx${jmem}G -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start >> /data/aafs/logs/aafs.log 2>&1 & echo $! > /data/aafs/pids/aafs.pid
    else
        nohup /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start >> /data/aafs/logs/aafs.log 2>&1 & echo $! > /data/aafs/pids/aafs.pid
    fi
    echo "Pausing 15 seconds..."
    sleep 15
}




# Function to stop AAFS, assuming that there are no intake processes (PDL).
# There are no arguments.
# The caller should check that AAFS is running before calling this function.

do_stop_aafs_no_intake () {
    echo "Stopping AAFS server..."
    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd stop 2>&1
    if [ -f /data/aafs/pids/aafs.pid ]; then
        rm /data/aafs/pids/aafs.pid
    fi
    echo "Waiting for AAFS server to terminate..."
    sleep 2
    while is_aafs_running ; do
        sleep 2
    done
}




case "$1" in

    # Commands to display status

    check_running)
        if is_mongo_running ; then
            echo "MongoDB is running"
        else
            echo "MongoDB is NOT running"
        fi
        if is_aafs_running ; then
            echo "AAFS is running"
        else
            echo "AAFS is NOT running"
        fi
        ;;

    show_version)
        do_show_version
        ;;

    # Commands to start and stop

    start_mongo)
        if is_mongo_running ; then
            echo "MongoDB is already started"
        else
            do_start_mongo
        fi
        ;;

    stop_mongo)
        if is_mongo_running ; then
            do_stop_mongo
        else
            echo "MongoDB is already stopped"
        fi
        ;;

    start_aafs)
        if is_aafs_running ; then
            echo "AAFS is already started"
        else
            if is_mongo_running ; then
                do_show_version
                do_start_aafs
            else
                echo "Cannot start AAFS because MongoDB is not running"
            fi
        fi
        ;;

    stop_aafs)
        if is_aafs_running ; then
            do_stop_aafs
        else
            echo "AAFS is already stopped"
        fi
        ;;

    start_all)
        if is_aafs_running ; then
            echo "AAFS is already started"
        else
            do_show_version
            if is_mongo_running ; then
                echo "MongoDB is already started"
            else
                do_start_mongo
                echo "Pausing 20 seconds..."
                sleep 20
            fi
            do_start_aafs
        fi
        ;;

    stop_all)
        if is_aafs_running ; then
            do_stop_aafs
            echo "Pausing 20 seconds..."
            sleep 20
        else
            echo "AAFS is already stopped"
        fi
        if is_mongo_running ; then
            do_stop_mongo
        else
            echo "MongoDB is already stopped"
        fi
        ;;

    start_aafs_no_intake)
        if is_aafs_running ; then
            echo "AAFS is already started"
        else
            if is_mongo_running ; then
                do_show_version
                do_start_aafs_no_intake
            else
                echo "Cannot start AAFS because MongoDB is not running"
            fi
        fi
        ;;

    stop_aafs_no_intake)
        if is_aafs_running ; then
            do_stop_aafs_no_intake
        else
            echo "AAFS is already stopped"
        fi
        ;;

    # Commands to manage the database

    initdb)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                echo "Initializing AAFS database..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd make_indexes 2>&1
                echo "Pausing 20 seconds..."
                sleep 20
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    rebuild_indexes)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                echo "Dropping AAFS database indexes..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd drop_indexes 2>&1
                echo "Pausing 20 seconds..."
                sleep 20
                echo "Building new AAFS database indexes..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd make_indexes 2>&1
                echo "Pausing 20 seconds..."
                sleep 20
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    check_collections)
        if is_mongo_running ; then
            echo "Checking for existence of AAFS database collections..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd check_collections 2>&1
        else
            echo "You need to start MongoDB before running this command"
        fi
        ;;

    backup_database)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                if [ -f "$2" ] ; then
                    # File exists, ask permission to overwrite
                    while true; do
                        read -p "Overwrite $2 (y/n)? " -n 1 -r
                        echo
                        case "$REPLY" in
                            y|Y)
                                rm "$2"
                                echo "Backing up database to file $2 ..."
                                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd backup_database "$2" 2>&1
                                break
                                ;;
                            n|N)
                                break
                                ;;
                            *)
                                echo "Please reply y or n"
                                ;;
                        esac
                    done
                else
                    # File does not exist
                    echo "Backing up database to file $2 ..."
                    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd backup_database "$2" 2>&1
                fi
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    backup_database_gzip)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                if [ -f "$2" ] ; then
                    # File exists, ask permission to overwrite
                    while true; do
                        read -p "Overwrite $2 (y/n)? " -n 1 -r
                        echo
                        case "$REPLY" in
                            y|Y)
                                rm "$2"
                                echo "Backing up database to file $2 ..."
                                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd backup_database_gzip "$2" 2>&1
                                break
                                ;;
                            n|N)
                                break
                                ;;
                            *)
                                echo "Please reply y or n"
                                ;;
                        esac
                    done
                else
                    # File does not exist
                    echo "Backing up database to file $2 ..."
                    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd backup_database_gzip "$2" 2>&1
                fi
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    restore_database)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                if [ -f "$2" ] ; then
                    # File exists
                    echo "Restoring database from file $2 ..."
                    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd restore_database "$2" 2>&1
                else
                    # File does not exist
                    echo "Cannot find file $2"
                fi
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    restore_database_gzip)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                if [ -f "$2" ] ; then
                    # File exists
                    echo "Restoring database from file $2 ..."
                    /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd restore_database_gzip "$2" 2>&1
                else
                    # File does not exist
                    echo "Cannot find file $2"
                fi
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    erase_database_this_is_irreversible)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                while true; do
                    read -p "Erase database (y/n)? " -n 1 -r
                    echo
                    case "$REPLY" in
                        y|Y)
                            echo "Erasing database..."
                            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd erase_database_this_is_irreversible 2>&1
                            break
                            ;;
                        n|N)
                            break
                            ;;
                        *)
                            echo "Please reply y or n"
                            ;;
                    esac
                done
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    # Commands to manage server relay

    init_relay_mode)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                echo "Initializing relay mode to \"$2 $3\"..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd init_relay_mode "$2" "$3" 2>&1
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    change_relay_mode)
        if is_aafs_running ; then
            echo "Changing relay mode to \"$3 $4\"..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd change_relay_mode "$2" "$3" "$4" 2>&1
            echo "Pausing 5 seconds..."
            sleep 5
            echo "Waiting for server acknowledgement ..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd wait_relay_status "$2" "$3" "$4" any any 2>&1
        else
            echo "You need to start AAFS before running this command"
        fi
        ;;

    show_relay_status)
        if is_mongo_running ; then
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd show_relay_status "$2" 2>&1
        else
            echo "You need to start MongoDB before running this command"
        fi
        ;;

    start_secondary_aafs)
        if is_aafs_running ; then
            echo "AAFS is already started"
        else
            if is_mongo_running ; then
                do_show_version
                do_start_aafs
                echo "Pausing 5 seconds..."
                sleep 5
                echo "Waiting for acknowledgement from this server ..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd wait_relay_status this watch other linked secondary 2>&1
                echo "Waiting for acknowledgement from other server ..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd wait_relay_status other watch other linked primary 2>&1
            else
                echo "Cannot start AAFS because MongoDB is not running"
            fi
        fi
        ;;

    stop_secondary_aafs)
        if is_aafs_running ; then
            echo "Changing relay mode to \"watch other\"..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd change_relay_mode both watch other 2>&1
            echo "Pausing 5 seconds..."
            sleep 5
            echo "Waiting for acknowledgement from this server ..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd wait_relay_status this watch other linked secondary 2>&1
            echo "Waiting for acknowledgement from other server ..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd wait_relay_status other watch other linked primary 2>&1
            do_stop_aafs
            echo "Waiting for acknowledgement from other server ..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd wait_relay_status other watch other unlinked primary 2>&1
        else
            echo "AAFS is already stopped"
        fi
        ;;

    # Commands to adjust analyst parameters

    init_analyst_cli)
        if is_aafs_running ; then
            echo "You cannot run this command while AAFS is running"
        else
            if is_mongo_running ; then
                echo "Launching analyst CLI ..."
                /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd init_analyst_cli 2>&1
            else
                echo "You need to start MongoDB before running this command"
            fi
        fi
        ;;

    analyst_cli)
        if is_aafs_running ; then
            echo "Launching analyst CLI ..."
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd change_analyst_cli "$2" 2>&1
        else
            echo "You need to start AAFS before running this command"
        fi
        ;;

    # Help commands

    help)
        echo "Check if MongoDB and the AAFS application are currently running:"
        echo "  moaf.sh check_running"
        echo "Display version information about the installed AAFS software:"
        echo "  moaf.sh show_version"
        echo "Start MongoDB:"
        echo "  moaf.sh start_mongo"
        echo "Stop MongoDB:"
        echo "  moaf.sh stop_mongo"
        echo "Start AAFS:"
        echo "  moaf.sh start_aafs"
        echo "Stop AAFS:"
        echo "  moaf.sh stop_aafs"
        echo "Start MongoDB, and then start AAFS:"
        echo "  moaf.sh start_all"
        echo "Stop AAFS, and then stop MongoDB:"
        echo "  moaf.sh stop_all"
        echo "Start AAFS, without starting the intake processes (PDL and polling) (debug/test command):"
        echo "  moaf.sh start_aafs_no_intake"
        echo "Stop AAFS, assuming that there is no intake process running (PDL) (debug/test command):"
        echo "  moaf.sh stop_aafs_no_intake"
        echo "Initialize the database:"
        echo "  moaf.sh initdb"
        echo "Delete the existing database indexes, and build new indexes:"
        echo "  moaf.sh rebuild_indexes"
        echo "Check if the database collections required by AAFS exist:"
        echo "  moaf.sh check_collections"
        echo "Make a backup of the entire database, and write it to a file:"
        echo "  moaf.sh backup_database <filename>"
        echo "Make a backup of the entire database, compress it, and write it to a file:"
        echo "  moaf.sh backup_database_gzip <filename>"
        echo "Restore the entire database from a file:"
        echo "  moaf.sh restore_database <filename>"
        echo "Restore the entire database from a compressed file:"
        echo "  moaf.sh restore_database_gzip <filename>"
        echo "Erase the entire database from MongoDB:"
        echo "  moaf.sh erase_database_this_is_irreversible"
        echo "Initialize the server relay mode:"
        echo "  moaf.sh init_relay_mode <relay_mode> <configured_primary>"
        echo "Change the server relay mode in a running system:"
        echo "  moaf.sh change_relay_mode <srvnum> <relay_mode> <configured_primary>"
        echo "Display the server relay status:"
        echo "  moaf.sh show_relay_status <srvnum>"
        echo "Restart a secondary server, in a dual-server configuration:"
        echo "  moaf.sh start_secondary_aafs"
        echo "Set this server to be the secondary server in a dual-server configuration, and then stop AAFS:"
        echo "  moaf.sh stop_secondary_aafs"
        echo "Initialize analyst parameters, using the analyst CLI:"
        echo "  moaf.sh init_analyst_cli"
        echo "Change analyst parameters in a running system, using the analyst CLI::"
        echo "  moaf.sh analyst_cli <srvnum>"
        ;;

    *)
        echo "Usage: 'moaf.sh help' to display help."
        exit 1
        ;;
esac

exit 0

