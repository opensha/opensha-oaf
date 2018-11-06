#!/usr/bin/env bash

# Script that is called to receive notifications from PDL.

cd /opt/aafs/intake
# echo "Notification received on" `date` >> /data/aafs/logs/listener.log

/usr/local/java/bin/java -Doafcfg=/opt/aafs/oafcfg -cp /opt/aafs/oefjava/oefjava.jar:/opt/aafs/oefjava/ProductClient.jar org.opensha.oaf.aafs.ServerCmd pdl_intake "$@" >> /data/aafs/logs/listener.log 2>&1


