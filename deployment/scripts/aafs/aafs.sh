#! /bin/sh

# This script starts and stops the AAFS system.

# This script must be run in the aftershock user account.

# This script starts and stops both the services and the application.

# Operations:
#
# start - Start the AAFS system.
#
# stop - Stop the AAFS system.
#
# initdb - Initialize the AAFS database.
#
# rebuild_indexes - Rebuild AAFS database indexes.

case "$1" in

    start)
        /opt/aafs/aafs-svc.sh start
        /opt/aafs/aafs-app.sh start
        ;;

    stop)
        /opt/aafs/aafs-app.sh stop
        /opt/aafs/aafs-svc.sh stop
        ;;

    initdb)
        /opt/aafs/aafs-svc.sh start
        /opt/aafs/aafs-app.sh initdb
        /opt/aafs/aafs-svc.sh stop
        ;;

    rebuild_indexes)
        /opt/aafs/aafs-svc.sh start
        /opt/aafs/aafs-app.sh rebuild_indexes
        /opt/aafs/aafs-svc.sh stop
        ;;

       *)
        echo "Usage: aafs.sh {start|stop|initdb|rebuild_indexes}"
        exit 1
        ;;
esac

exit 0

