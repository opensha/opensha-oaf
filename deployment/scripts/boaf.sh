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
# compile - Compile the OpenSHA code to create AAFS.
#           After compiling, you must run the pack operation to produce a runnable jar file.
#
# pack - Package the AAFS jar file.
#
# compilegui - Compile the OpenSHA code to create the aftershock GUI.
#              This creates the generic GUI.
#
# packgui - Package the GUI jar file, and bundle with private configuration.
#           After the 'packgui' keyword comes the GUI date in form YYYY_MM_DD, followed by the
#           name of the private configuration file.  This creates the production GUI.
#
# deploy - Copy the AAFS jar file and required libraries into /opt/aafs/oefjava.
#
# deploycfg - Copy the AAFS configuration files and scripts into /opt/aafs and its subdirectories.
#             The user is prompted before any existing file in /opt/aafs is changed.
#
# diffcfg - Use git diff to compare the configuration files in /opt/aafs to the originals.
#
# diffcfgc - Same as diffcfg except forces the use of colored text when displaying changes.
#
# run - Run a class in the org.opensha.oaf package, using the compiled-in configuration.
#       After the 'run' keyword comes the name of the class (without the 'org.opensha.oaf.'
#       prefix), followed by any command-line parameters for the class.
#
# runcfg - Run a class in the org.opensha.oaf package, reading configuration from ./oafcfg.
#          After the 'runcfg' keyword comes the name of the class (without the 'org.opensha.oaf.'
#          prefix), followed by any command-line parameters for the class.
#
# runaafs - Run a class in the org.opensha.oaf package, reading configuration from /opt/aafs/oafcfg.
#           After the 'runaafs' keyword comes the name of the class (without the 'org.opensha.oaf.'
#           prefix), followed by any command-line parameters for the class.
#
# runany - Run a class in any package, using the compiled-in configuration.
#          After the 'runany' keyword comes the full name of the class, followed by any command-line
#          parameters for the class.




# Function to copy a script file, prompting if existing file is being changed.
# $1 = Source file.
# $2 = Destination file.

copyscr () {

    # If the destination file exists then test if the content is changing

    if [ -f "$2" ]; then
        if cmp -s "$1" "$2"; then
            # File exists, but content is the same
            rm "$2"
            cp -pi "$1" "$2"
            chmod 755 "$2"
        else
            # File exists, and content is different
            while true; do
                read -p "Overwrite $2 (y/n)? " -n 1 -r
                echo
                case "$REPLY" in
                    y|Y)
                        rm "$2"
                        cp -pi "$1" "$2"
                        chmod 755 "$2"
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
        fi
    else
        # File does not exist
        cp -pi "$1" "$2"
        chmod 755 "$2"
    fi
}




# Function to copy a configuration file, prompting if existing file is being changed.
# $1 = Source file.
# $2 = Destination file.

copycfg () {

    # If the destination file exists then test if the content is changing

    if [ -f "$2" ]; then
        if cmp -s "$1" "$2"; then
            # File exists, but content is the same
            rm "$2"
            cp -pi "$1" "$2"
        else
            # File exists, and content is different
            while true; do
                read -p "Overwrite $2 (y/n)? " -n 1 -r
                echo
                case "$REPLY" in
                    y|Y)
                        rm "$2"
                        cp -pi "$1" "$2"
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
        fi
    else
        # File does not exist
        cp -pi "$1" "$2"
    fi
}




# Function to copy a file, overwriting any existing file.
# $1 = Source file.
# $2 = Destination file.

copyover () {
    if [ -f "$2" ]; then
        rm "$2"
    fi
    cp -pi "$1" "$2"
}




# Function to make a directory, if the directory does not exist.
# $1 = Directory.

makenewdir () {
    if [ ! -d "$1" ]; then
        mkdir "$1"
    fi
}




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
        else
            echo "Program has not been compiled yet"
        fi
        ;;

    compilegui)
        cd opensha-oaf
        ./gradlew appOAFJar
        cd ..
        ls opensha-oaf/build/libs
        ;;

    packgui)
        if [ -f opensha-oaf/build/libs/AftershockGUI-current-$2.jar ]; then
            cd opensha-oaf/build/libs
            if [ -d gtmp ]; then
                rm -r gtmp
            fi
            if [ -f AftershockGUI-prod-$2.jar ]; then
                rm AftershockGUI-prod-$2.jar
            fi
            mkdir gtmp
            cd gtmp
            unzip -uoq ../AftershockGUI-current-$2.jar
            cd ..
            cd ../../..
            if [ -f opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json ]; then
                rm opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json
            fi
            cp -pi "$3" opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json
            cd opensha-oaf/build/libs
            jar -cf AftershockGUI-prod-$2.jar -C gtmp .
            cd ../../..
        else
            echo "GUI has not been compiled yet"
        fi
        ;;

    deploy)
        if [ -f opensha-oaf/build/libs/oefjava.jar ]; then
            makenewdir /opt/aafs/oefjava
            copyover opensha-oaf/build/libs/oefjava.jar /opt/aafs/oefjava/oefjava.jar
            copyover opensha-oaf/lib/ProductClient.jar /opt/aafs/oefjava/ProductClient.jar
        else
            echo "Program has not been compiled and packed yet"
        fi
        ;;

    deploycfg)
        makenewdir /opt/aafs/oefjava
        makenewdir /opt/aafs/oafcfg
        makenewdir /opt/aafs/intake
        makenewdir /opt/aafs/key
        copycfg opensha-oaf/src/org/opensha/oaf/aafs/ServerConfig.json /opt/aafs/oafcfg/ServerConfig.json
        copycfg opensha-oaf/src/org/opensha/oaf/aafs/ActionConfig.json /opt/aafs/oafcfg/ActionConfig.json
        copycfg opensha-oaf/src/org/opensha/oaf/rj/GenericRJ_ParametersFetch.json /opt/aafs/oafcfg/GenericRJ_ParametersFetch.json
        copycfg opensha-oaf/src/org/opensha/oaf/rj/MagCompPage_ParametersFetch.json /opt/aafs/oafcfg/MagCompPage_ParametersFetch.json
        copyscr opensha-oaf/deployment/scripts/aafs/aafs.sh /opt/aafs/aafs.sh
        copyscr opensha-oaf/deployment/scripts/aafs/aafs-app.sh /opt/aafs/aafs-app.sh
        copyscr opensha-oaf/deployment/scripts/aafs/aafs-svc.sh /opt/aafs/aafs-svc.sh
        copyscr opensha-oaf/deployment/scripts/aafs/intake/init.sh /opt/aafs/intake/init.sh
        copyscr opensha-oaf/deployment/scripts/aafs/intake/listener.sh /opt/aafs/intake/listener.sh
        copycfg opensha-oaf/deployment/scripts/aafs/intake/config.ini /opt/aafs/intake/config.ini
        ;;

    diffcfg)
        git diff opensha-oaf/src/org/opensha/oaf/aafs/ServerConfig.json /opt/aafs/oafcfg/ServerConfig.json
        git diff opensha-oaf/src/org/opensha/oaf/aafs/ActionConfig.json /opt/aafs/oafcfg/ActionConfig.json
        git diff opensha-oaf/src/org/opensha/oaf/rj/GenericRJ_ParametersFetch.json /opt/aafs/oafcfg/GenericRJ_ParametersFetch.json
        git diff opensha-oaf/src/org/opensha/oaf/rj/MagCompPage_ParametersFetch.json /opt/aafs/oafcfg/MagCompPage_ParametersFetch.json
        git diff opensha-oaf/deployment/scripts/aafs/intake/config.ini /opt/aafs/intake/config.ini
        ;;

    diffcfgc)
        git diff --color opensha-oaf/src/org/opensha/oaf/aafs/ServerConfig.json /opt/aafs/oafcfg/ServerConfig.json
        git diff --color opensha-oaf/src/org/opensha/oaf/aafs/ActionConfig.json /opt/aafs/oafcfg/ActionConfig.json
        git diff --color opensha-oaf/src/org/opensha/oaf/rj/GenericRJ_ParametersFetch.json /opt/aafs/oafcfg/GenericRJ_ParametersFetch.json
        git diff --color opensha-oaf/src/org/opensha/oaf/rj/MagCompPage_ParametersFetch.json /opt/aafs/oafcfg/MagCompPage_ParametersFetch.json
        git diff --color opensha-oaf/deployment/scripts/aafs/intake/config.ini /opt/aafs/intake/config.ini
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

    runaafs)
        JCLASS="org.opensha.oaf.$2"
        shift 2
        java -Doafcfg=/opt/aafs/oafcfg -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@"
        ;;

    runany)
        JCLASS="$2"
        shift 2
        java -cp opensha-oaf/build/libs/oefjava.jar:opensha-oaf/lib/ProductClient.jar $JCLASS "$@"
        ;;

    *)
        echo "Usage: boaf.sh {clone|update|clean|compile|pack|compilegui|packgui|deploy|deploycfg|diffcfg|diffcfgc|run|runcfg|runaafs|runany}"
        exit 1
        ;;
esac

exit 0

