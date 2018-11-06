#! /bin/sh

# This script starts and stops the AAFS application.

# This script must be run in the aftershock user account.

# The required services must be started before starting the application,
# and must be stopped after stopping the application.

# Operations:
#
# start - Start the AAFS application.
#
# stop - Stop the AAFS application.

case "$1" in
    start)
		if [ ! -d /data/aafs/logs ]; then
		    mkdir /data/aafs/logs
		fi
		if [ ! -d /data/aafs/pids ]; then
		    mkdir /data/aafs/pids
		fi
        /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd show_version 2>&1
        echo "Pausing 30 seconds..."
        sleep 30
        echo "Starting AAFS server..."
        nohup /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd start >> /data/aafs/logs/aafs.log 2>&1 & echo $! > /data/aafs/pids/aafs.pid
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
        ;;
    stop)
        echo "Stopping PDL listener..."
        cd /opt/aafs/intake
        /opt/aafs/intake/init.sh stop
        cd -
        echo "Pausing 30 seconds..."
        sleep 30
        echo "Stopping AAFS server..."
        if [ -f /data/aafs/pids/aafs.pid ]; then
            /usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd stop 2>&1
	        rm /data/aafs/pids/aafs.pid
        fi
        echo "Pausing 90 seconds..."
        sleep 90
        ;;
       *)
        echo "Usage: aafs-app.sh {start|stop}"
        exit 1
        ;;
esac

exit 0

