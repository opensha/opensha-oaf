#! /bin/bash

# This script contains operations for downloading, compiling, and deploying the AAFS application.
# It must always be run with the current directory equal to the directory that contains the OpenSHA repositories.

# Operations:
#
# clone - Clone the OpenSHA repositories into the current directory.
#         This only needs to be done once; the update operation can retrieve any updates.
#
# update - Update the OpenSHA repositories.
#
# clean - Delete all compiled files.
#
# compile - Compile the OpenSHA code.
#           After compiling, you must run the pack operation to produce a runnable jar file.
#
# pack - Package the AAFS jar file.
#
# deploy - Copy the AAFS jar file and required libraries into /opt/aafs/oefjava.
#
# deploycfg - Copy the AAFS configuration files into /opt/aafs/oafcfg.
#             The ServerConfig.json file is not copied if it already exists,
#             to avoid overwriting passwords and other server configuration settings.
#
# run - Run a class in the org.opensha.oaf package, using the compiled-in configuration.
#       After the 'run' keyword comes the name of the class (without the 'org.opensha.oaf.'
#       prefix), followed by any command-line parameters for the class.
#
# runcfg - Run a class in the org.opensha.oaf package, reading configuration from ./oafcfg.
#          After the 'run' keyword comes the name of the class (without the 'org.opensha.oaf.'
#          prefix), followed by any command-line parameters for the class.
#
# runany - Run a class in any package, using the compiled-in configuration.
#          After the 'run' keyword comes the full name of the class, followed by any command-line
#          parameters for the class.

case "$1" in

    clone)
        git clone https://github.com/opensha/opensha-commons
        git clone https://github.com/opensha/opensha-core
        git clone https://github.com/opensha/opensha-ucerf3
        git clone https://github.com/opensha/opensha-apps
        git clone https://github.com/opensha/opensha-oaf
        ;;

    update)
        cd opensha-commons
        git pull origin master
        cd ..
        cd opensha-core
        git pull origin master
        cd ..
        cd opensha-ucerf3
        git pull origin master
        cd ..
        cd opensha-apps
        git pull origin master
        cd ..
        cd opensha-oaf
        git pull origin master
        cd ..
        ;;

    clean)
        if [ -d opensha-commons/build ]; then
            rm -r opensha-commons/build
        fi
        if [ -d opensha-core/build ]; then
            rm -r opensha-core/build
        fi
        if [ -d opensha-ucerf3/build ]; then
            rm -r opensha-ucerf3/build
        fi
        if [ -d opensha-apps/build ]; then
            rm -r opensha-apps/build
        fi
        if [ -d opensha-oaf/build ]; then
            rm -r opensha-oaf/build
        fi
        ;;

    compile)
        cd opensha-oaf
        ./gradlew oafJar
        cd ..
        ;;

    pack)
        if [ -f opensha-oaf/build/libs/opensha-oaf-oaf.jar ]; then
            cd opensha-oaf/build/libs
            if [ -d otmp ]; then
                rm -r otmp
            fi
            if [ -f oefjava.jar ]; then
                rm oefjava.jar
            fi
            mkdir otmp
            cd otmp
            unzip -uoq ../opensha-oaf-oaf.jar
            if [ -f META-INF/MANIFEST.MF ]; then
                rm META-INF/MANIFEST.MF
            fi
            if [ -f META-INF/BCKEY.DSA ]; then
                rm META-INF/BCKEY.DSA
            fi
            if [ -f META-INF/BCKEY.SF ]; then
                rm META-INF/BCKEY.SF
            fi
            if [ -f META-INF/IDRSIG.DSA ]; then
                rm META-INF/IDRSIG.DSA
            fi
            if [ -f META-INF/IDRSIG.SF ]; then
                rm META-INF/IDRSIG.SF
            fi
            cd ..
            jar -cf oefjava.jar -C otmp .
            cd ../../..
        fi
        ;;

    deploy)
        if [ -f opensha-oaf/build/libs/oefjava.jar ]; then
            if [ -f /opt/aafs/oefjava/oefjava.jar ]; then
                rm /opt/aafs/oefjava/oefjava.jar
            fi
            cp -pi opensha-oaf/build/libs/oefjava.jar /opt/aafs/oefjava/oefjava.jar
            if [ -f /opt/aafs/oefjava/ProductClient.jar ]; then
                rm /opt/aafs/oefjava/ProductClient.jar
            fi
            cp -pi opensha-oaf/lib/ProductClient.jar /opt/aafs/oefjava/ProductClient.jar
        fi
        ;;

    deploycfg)
        if [ ! -f /opt/aafs/oafcfg/ServerConfig.json ]; then
            cp -pi opensha-oaf/src/org/opensha/oaf/aafs/ServerConfig.json /opt/aafs/oafcfg/ServerConfig.json
        fi
        if [ -f /opt/aafs/oafcfg/ActionConfig.json ]; then
            rm /opt/aafs/oafcfg/ActionConfig.json
        fi
        cp -pi opensha-oaf/src/org/opensha/oaf/aafs/ActionConfig.json /opt/aafs/oafcfg/ActionConfig.json
        if [ -f /opt/aafs/oafcfg/GenericRJ_ParametersFetch.json ]; then
            rm /opt/aafs/oafcfg/GenericRJ_ParametersFetch.json
        fi
        cp -pi opensha-oaf/src/org/opensha/oaf/rj/GenericRJ_ParametersFetch.json /opt/aafs/oafcfg/GenericRJ_ParametersFetch.json
        if [ -f /opt/aafs/oafcfg/MagCompPage_ParametersFetch.json ]; then
            rm /opt/aafs/oafcfg/MagCompPage_ParametersFetch.json
        fi
        cp -pi opensha-oaf/src/org/opensha/oaf/rj/MagCompPage_ParametersFetch.json /opt/aafs/oafcfg/MagCompPage_ParametersFetch.json
        ;;

    run)
        JCLASS="org.opensha.oaf.$2"
        shift 2
        java -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@"
        ;;

    runcfg)
        JCLASS="org.opensha.oaf.$2"
        shift 2
        java -Doafcfg=./oafcfg -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@"
        ;;

    runany)
        JCLASS="$2"
        shift 2
        java -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@"
        ;;

    *)
        echo "Usage: boaf.sh {clone|update|clean|compile|pack|deploy|deploycfg|run|runcfg|runany}"
        exit 1
        ;;
esac

exit 0

