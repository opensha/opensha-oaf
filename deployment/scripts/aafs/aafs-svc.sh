#! /bin/sh

# This script starts and stops services that are required by AAFS.
# At present, the only service required is MongoDB.

# This script must be run in the aftershock user account.
# It requires that the aftershock group must be permitted to run
# the service start/stop commands without a password in /etc/sudoers.d.

# Operations:
#
# start - Start the required services.
#
# stop - Stop the required services.

case "$1" in

    start)
        echo "Starting MongoDB..."
        sudo /usr/sbin/service mongod start
        ;;

    stop)
        echo "Stopping MongoDB..."
        sudo /usr/sbin/service mongod stop
        ;;

       *)
        echo "Usage: aafs-svc.sh {start|stop}"
        exit 1
        ;;
esac

exit 0

