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
#           name of the private server configuration file.  This creates the production GUI.
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
# config_server_solo - Create a server configuration file for a single-server configuration.
#     After the 'config_server_solo' keyword comes the following parameters:
#       SRVIP1  REPSET1  DBNAME  DBUSER  DBPASS  SRVNAME  PDLOPT
#     Where:
#       SRVIP1 = Server IP address.
#       REPSET1 = MongoDB replica set name.
#       DBNAME = MongoDB database name.
#       DBUSER = MongoDB username.
#       DBPASS = MongoDB password.
#       SRVNAME = Server name.
#       PDLOPT = PDL option.
#     The PDL option must be one of the following:
#       "none" = Forecasts are not sent to PDL.
#       "dev" = Forecasts are sent to PDL-Development.
#       keyfile name = Forecasts are sent to PDL-Production; the keyfile name must be a file in /opt/aafs/key.
#     It is assumed that DBNAME is both the data storage database and authentication database.
#     The server configuration file is written to /opt/aafs/oafcfg/ServerConfig.json.
#
# config_file_server_solo - Create a server configuration file for a single-server configuration.
#     After the 'config_file_server_solo' keyword comes the following parameters:
#       FILENAME  SRVIP1  REPSET1  DBNAME  DBUSER  DBPASS  SRVNAME  PDLOPT
#     Same as 'config_server_solo' except the configuration file is written to FILENAME.
#
# config_server_1 - Create a server configuration file for server #1 in a dual-server configuration.
#     After the 'config_server_1' keyword comes the following parameters:
#       SRVIP1  REPSET1  SRVIP2  REPSET2  DBNAME  DBUSER  DBPASS  SRVNAME  PDLOPT
#     Where:
#       SRVIP1 = Server IP address for server #1.
#       REPSET1 = MongoDB replica set name for server #1.
#       SRVIP2 = Server IP address for server #2.
#       REPSET2 = MongoDB replica set name for server #2.
#       DBNAME = MongoDB database name.
#       DBUSER = MongoDB username.
#       DBPASS = MongoDB password.
#       SRVNAME = Server name.
#       PDLOPT = PDL option.
#     The PDL option must be one of the following:
#       "none" = Forecasts are not sent to PDL.
#       "dev" = Forecasts are sent to PDL-Development.
#       keyfile name = Forecasts are sent to PDL-Production; the keyfile name must be a file in /opt/aafs/key.
#     It is assumed that DBNAME is both the data storage database and authentication database.
#     The two servers MUST have different replica set names.
#     It is assumed that both servers use the same database name, username, and password.
#     The server configuration file is written to /opt/aafs/oafcfg/ServerConfig.json.
#
# config_file_server_1 - Create a server configuration file for server #1 in a dual-server configuration.
#     After the 'config_file_server_1' keyword comes the following parameters:
#       FILENAME  SRVIP1  REPSET1  SRVIP2  REPSET2  DBNAME  DBUSER  DBPASS  SRVNAME  PDLOPT
#     Same as 'config_server_1' except the configuration file is written to FILENAME.
#
# config_server_2 - Create a server configuration file for server #2 in a dual-server configuration.
#     After the 'config_server_2' keyword comes the following parameters:
#       SRVIP1  REPSET1  SRVIP2  REPSET2  DBNAME  DBUSER  DBPASS  SRVNAME  PDLOPT
#     Where:
#       SRVIP1 = Server IP address for server #1.
#       REPSET1 = MongoDB replica set name for server #1.
#       SRVIP2 = Server IP address for server #2.
#       REPSET2 = MongoDB replica set name for server #2.
#       DBNAME = MongoDB database name.
#       DBUSER = MongoDB username.
#       DBPASS = MongoDB password.
#       SRVNAME = Server name.
#       PDLOPT = PDL option.
#     The PDL option must be one of the following:
#       "none" = Forecasts are not sent to PDL.
#       "dev" = Forecasts are sent to PDL-Development.
#       keyfile name = Forecasts are sent to PDL-Production; the keyfile name must be a file in /opt/aafs/key.
#     It is assumed that DBNAME is both the data storage database and authentication database.
#     The two servers MUST have different replica set names.
#     It is assumed that both servers use the same database name, username, and password.
#     The server configuration file is written to /opt/aafs/oafcfg/ServerConfig.json.
#
# config_file_server_2 - Create a server configuration file for server #2 in a dual-server configuration.
#     After the 'config_file_server_2' keyword comes the following parameters:
#       FILENAME  SRVIP1  REPSET1  SRVIP2  REPSET2  DBNAME  DBUSER  DBPASS  SRVNAME  PDLOPT
#     Same as 'config_server_2' except the configuration file is written to FILENAME.
#
# config_server_dev - Install a server configuration file for a development server in single-server configuration.
#     This restores the default server configuration, which is intended for development.
#     The server configuration file is written to /opt/aafs/oafcfg/ServerConfig.json.
#
# config_file_server_dev - Write a server configuration file for a development server in single-server configuration.
#     After the 'config_file_server_dev' keyword comes the following parameters:
#       FILENAME
#     Same as 'config_server_dev' except the configuration file is written to FILENAME.
#
# config_action_usa - Install an action configuration file that accepts earthquakes only from the US.
#     The action configuration file is written to /opt/aafs/oafcfg/ActionConfig.json.
#
# config_file_action_usa - Write an action configuration file that accepts earthquakes only from the US.
#     After the 'config_file_action_usa' keyword comes the following parameters:
#       FILENAME
#     Same as 'config_action_usa' except the configuration file is written to FILENAME.
#
# config_action_dev - Install an action configuration file that accepts earthquakes world-wide.
#     This restores the default action configuration, which is intended for development.
#     The action configuration file is written to /opt/aafs/oafcfg/ActionConfig.json.
#
# config_file_action_dev - Write an action configuration file that accepts earthquakes world-wide.
#     After the 'config_file_action_dev' keyword comes the following parameters:
#       FILENAME
#     Same as 'config_action_dev' except the configuration file is written to FILENAME.
#
# config_packgui - Configure and package the production GUI.
#     After the 'config_packgui' keyword comes the following parameters:
#       GUIDATE  SRVIP1  REPSET1  SRVIP2  REPSET2  DBNAME  DBUSER  DBPASS
#     Where:
#       GUIDATE = GUI date in form YYYY_MM_DD.
#       SRVIP1 = Server IP address for server #1.
#       REPSET1 = MongoDB replica set name for server #1.
#       SRVIP2 = Server IP address for server #2.
#       REPSET2 = MongoDB replica set name for server #2.
#       DBNAME = MongoDB database name.
#       DBUSER = MongoDB username.
#       DBPASS = MongoDB password.
#     It is assumed that DBNAME is both the data storage database and authentication database.
#     For a single-server configuration, enter the same IP address and replica set name for both server #1 and server #2.
#     For a dual-server configuration, it is assumed that both servers use the same database name, username, and password.
#     This works by modifying the generic GUI, so the generic must already be built and up-to-date.
#     The server configuration is bound into the production GUI.
#
# config_file_packgui - Write a server configuration file for the production GUI.
#     After the 'config_file_packgui' keyword comes the following parameters:
#       FILENAME  SRVIP1  REPSET1  SRVIP2  REPSET2  DBNAME  DBUSER  DBPASS
#     Same as 'config_packgui' except the configuration file is written to FILENAME.
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




# Function to check if a file exists and, if so, ask user if it is OK to overwrite the file.
# Sets WRITEISOK to "Y" if OK to write, "N" if not OK to write.
# $1 = File.

isfilewriteok () {
    if [ -f "$1" ]; then
        # File exists
        while true; do
            read -p "Overwrite $1 (y/n)? " -n 1 -r
            echo
            case "$REPLY" in
                y|Y)
                    WRITEISOK="Y"
                    break
                    ;;
                n|N)
                    WRITEISOK="N"
                    break
                    ;;
                *)
                    echo "Please reply y or n"
                    ;;
            esac
        done
    else
        # File does not exist
        WRITEISOK="Y"
    fi
}




# Function to copy a server configuration file with substitutions, overwriting any existing file.
# ${1} = Source file.
# ${2} = Destination file.
# ${3} thru ${22} = Server parameters, see definitions below.
# Note that ${22} is one of:
#  "none" if forecasts are not sent to PDL.
#  "dev" if forecasts are sent to PDL-Development.
#  the name of a key file (which must be in /opt/aafs/key), if forecasts are sent to PDL-Production.

copysubsrv () {
    if [ -f "${2}" ]; then
        rm "${2}"
    fi

    MONGO_REP_SET_0="${3}"
    MONGO_USER_0="${4}"
    MONGO_AUTH_DB_0="${5}"
    MONGO_PASS_0="${6}"
    MONGO_DATA_DB_0="${7}"

    MONGO_REP_SET_1="${8}"
    MONGO_USER_1="${9}"
    MONGO_AUTH_DB_1="${10}"
    MONGO_PASS_1="${11}"
    MONGO_DATA_DB_1="${12}"

    MONGO_REP_SET_2="${13}"
    MONGO_USER_2="${14}"
    MONGO_AUTH_DB_2="${15}"
    MONGO_PASS_2="${16}"
    MONGO_DATA_DB_2="${17}"

    SERVER_IP_1="${18}"
    SERVER_IP_2="${19}"
    SERVER_NAME_0="${20}"
    SERVER_NUMBER_0="${21}"

    case "${22}" in
        none)
            PDL_ENABLE_0="0"
            PDL_KEYFILE_0=""
            ;;
        dev)
            PDL_ENABLE_0="1"
            PDL_KEYFILE_0=""
            ;;
        *)
            PDL_ENABLE_0="2"
            PDL_KEYFILE_0="/opt/aafs/key/${22}"
            ;;
    esac

    cat "${1}"    \
    | sed "s/---MONGO_REP_SET_0---/$MONGO_REP_SET_0/g"    \
    | sed "s/---MONGO_USER_0---/$MONGO_USER_0/g"    \
    | sed "s/---MONGO_AUTH_DB_0---/$MONGO_AUTH_DB_0/g"    \
    | sed "s/---MONGO_PASS_0---/$MONGO_PASS_0/g"    \
    | sed "s/---MONGO_DATA_DB_0---/$MONGO_DATA_DB_0/g"    \
    | sed "s/---MONGO_REP_SET_1---/$MONGO_REP_SET_1/g"    \
    | sed "s/---MONGO_USER_1---/$MONGO_USER_1/g"    \
    | sed "s/---MONGO_AUTH_DB_1---/$MONGO_AUTH_DB_1/g"    \
    | sed "s/---MONGO_PASS_1---/$MONGO_PASS_1/g"    \
    | sed "s/---MONGO_DATA_DB_1---/$MONGO_DATA_DB_1/g"    \
    | sed "s/---MONGO_REP_SET_2---/$MONGO_REP_SET_2/g"    \
    | sed "s/---MONGO_USER_2---/$MONGO_USER_2/g"    \
    | sed "s/---MONGO_AUTH_DB_2---/$MONGO_AUTH_DB_2/g"    \
    | sed "s/---MONGO_PASS_2---/$MONGO_PASS_2/g"    \
    | sed "s/---MONGO_DATA_DB_2---/$MONGO_DATA_DB_2/g"    \
    | sed "s/---SERVER_IP_1---/$SERVER_IP_1/g"    \
    | sed "s/---SERVER_IP_2---/$SERVER_IP_2/g"    \
    | sed "s/---SERVER_NAME_0---/$SERVER_NAME_0/g"    \
    | sed "s/---SERVER_NUMBER_0---/$SERVER_NUMBER_0/g"    \
    | sed "s/---PDL_ENABLE_0---/$PDL_ENABLE_0/g"    \
    | sed "s|---PDL_KEYFILE_0---|$PDL_KEYFILE_0|g"    \
    > "${2}"
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
            if [ -f opensha-oaf-oaf-fixed.jar ]; then
                rm opensha-oaf-oaf-fixed.jar
            fi
            zip -q -F opensha-oaf-oaf.jar --out opensha-oaf-oaf-fixed.jar
            mkdir otmp
            cd otmp
            unzip -uoq ../opensha-oaf-oaf-fixed.jar
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
            if [ -f AftershockGUI-current-$2-fixed.jar ]; then
                rm AftershockGUI-current-$2-fixed.jar
            fi
            zip -q -F AftershockGUI-current-$2.jar --out AftershockGUI-current-$2-fixed.jar
            mkdir gtmp
            cd gtmp
            unzip -uoq ../AftershockGUI-current-$2-fixed.jar
            cd ..
            cd ../../..
            if [ -f opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json ]; then
                rm opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json
            fi
            cp -pi "$3" opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json
            cd opensha-oaf/build/libs
            jar -cfe AftershockGUI-prod-$2.jar org.opensha.oaf.rj.AftershockStatsGUI -C gtmp .
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
        copyscr opensha-oaf/deployment/scripts/aafs/moaf.sh /opt/aafs/moaf.sh
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

    config_server_solo)
        if [ -d /opt/aafs/oafcfg ]; then
            SRVIP1="${2}"
            REPSET1="${3}"
            DBNAME="${4}"
            DBUSER="${5}"
            DBPASS="${6}"
            SRVNAME="${7}"
            PDLOPT="${8}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json /opt/aafs/oafcfg/ServerConfig.json    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP1" "$SRVNAME" "1" "$PDLOPT"
        else
            echo "Configuration directory /opt/aafs/oafcfg has not been created yet"
        fi
        ;;

    config_file_server_solo)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            SRVIP1="${3}"
            REPSET1="${4}"
            DBNAME="${5}"
            DBUSER="${6}"
            DBPASS="${7}"
            SRVNAME="${8}"
            PDLOPT="${9}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json "$2"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP1" "$SRVNAME" "1" "$PDLOPT"
        else
            echo "Operation aborted"
        fi
        ;;

    config_server_1)
        if [ -d /opt/aafs/oafcfg ]; then
            SRVIP1="${2}"
            REPSET1="${3}"
            SRVIP2="${4}"
            REPSET2="${5}"
            DBNAME="${6}"
            DBUSER="${7}"
            DBPASS="${8}"
            SRVNAME="${9}"
            PDLOPT="${10}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json /opt/aafs/oafcfg/ServerConfig.json    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP2" "$SRVNAME" "1" "$PDLOPT"
        else
            echo "Configuration directory /opt/aafs/oafcfg has not been created yet"
        fi
        ;;

    config_file_server_1)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            SRVIP1="${3}"
            REPSET1="${4}"
            SRVIP2="${5}"
            REPSET2="${6}"
            DBNAME="${7}"
            DBUSER="${8}"
            DBPASS="${9}"
            SRVNAME="${10}"
            PDLOPT="${11}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json "$2"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP2" "$SRVNAME" "1" "$PDLOPT"
        else
            echo "Operation aborted"
        fi
        ;;

    config_server_2)
        if [ -d /opt/aafs/oafcfg ]; then
            SRVIP1="${2}"
            REPSET1="${3}"
            SRVIP2="${4}"
            REPSET2="${5}"
            DBNAME="${6}"
            DBUSER="${7}"
            DBPASS="${8}"
            SRVNAME="${9}"
            PDLOPT="${10}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json /opt/aafs/oafcfg/ServerConfig.json    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP2" "$SRVNAME" "2" "$PDLOPT"
        else
            echo "Configuration directory /opt/aafs/oafcfg has not been created yet"
        fi
        ;;

    config_file_server_2)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            SRVIP1="${3}"
            REPSET1="${4}"
            SRVIP2="${5}"
            REPSET2="${6}"
            DBNAME="${7}"
            DBUSER="${8}"
            DBPASS="${9}"
            SRVNAME="${10}"
            PDLOPT="${11}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json "$2"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP2" "$SRVNAME" "2" "$PDLOPT"
        else
            echo "Operation aborted"
        fi
        ;;

    config_server_dev)
        if [ -d /opt/aafs/oafcfg ]; then
            copycfg opensha-oaf/src/org/opensha/oaf/aafs/ServerConfig.json /opt/aafs/oafcfg/ServerConfig.json
        else
            echo "Configuration directory /opt/aafs/oafcfg has not been created yet"
        fi
        ;;

    config_file_server_dev)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            copyover opensha-oaf/src/org/opensha/oaf/aafs/ServerConfig.json "$2"
        else
            echo "Operation aborted"
        fi
        ;;

    config_action_usa)
        if [ -d /opt/aafs/oafcfg ]; then
            copycfg opensha-oaf/deployment/scripts/prodcfg/ActionConfig_usa.json /opt/aafs/oafcfg/ActionConfig.json
        else
            echo "Configuration directory /opt/aafs/oafcfg has not been created yet"
        fi
        ;;

    config_file_action_usa)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            copyover opensha-oaf/deployment/scripts/prodcfg/ActionConfig_usa.json "$2"
        else
            echo "Operation aborted"
        fi
        ;;

    config_action_dev)
        if [ -d /opt/aafs/oafcfg ]; then
            copycfg opensha-oaf/src/org/opensha/oaf/aafs/ActionConfig.json /opt/aafs/oafcfg/ActionConfig.json
        else
            echo "Configuration directory /opt/aafs/oafcfg has not been created yet"
        fi
        ;;

    config_file_action_dev)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            copyover opensha-oaf/src/org/opensha/oaf/aafs/ActionConfig.json "$2"
        else
            echo "Operation aborted"
        fi
        ;;

    config_packgui)
        if [ -f opensha-oaf/build/libs/AftershockGUI-current-$2.jar ]; then
            cd opensha-oaf/build/libs
            if [ -d gtmp ]; then
                rm -r gtmp
            fi
            if [ -f AftershockGUI-prod-$2.jar ]; then
                rm AftershockGUI-prod-$2.jar
            fi
            if [ -f AftershockGUI-current-$2-fixed.jar ]; then
                rm AftershockGUI-current-$2-fixed.jar
            fi
            zip -q -F AftershockGUI-current-$2.jar --out AftershockGUI-current-$2-fixed.jar
            mkdir gtmp
            cd gtmp
            unzip -uoq ../AftershockGUI-current-$2-fixed.jar
            cd ..
            cd ../../..
            if [ -f opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json ]; then
                rm opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json
            fi
            SRVIP1="${3}"
            REPSET1="${4}"
            SRVIP2="${5}"
            REPSET2="${6}"
            DBNAME="${7}"
            DBUSER="${8}"
            DBPASS="${9}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json opensha-oaf/build/libs/gtmp/org/opensha/oaf/aafs/ServerConfig.json    \
            "rs0" "usgs" "usgs" "usgs" "usgs"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP2" "test" "1" "none"
            cd opensha-oaf/build/libs
            jar -cfe AftershockGUI-prod-$2.jar org.opensha.oaf.rj.AftershockStatsGUI -C gtmp .
            cd ../../..
        else
            echo "GUI has not been compiled yet"
        fi
        ;;

    config_file_packgui)
        isfilewriteok "$2"
        if [ "$WRITEISOK" == "Y" ]; then
            echo "Writing file $2 ..."
            SRVIP1="${3}"
            REPSET1="${4}"
            SRVIP2="${5}"
            REPSET2="${6}"
            DBNAME="${7}"
            DBUSER="${8}"
            DBPASS="${9}"
            copysubsrv opensha-oaf/deployment/scripts/prodcfg/ServerConfig_sub.json "$2"     \
            "rs0" "usgs" "usgs" "usgs" "usgs"    \
            "$REPSET1" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$REPSET2" "$DBUSER" "$DBNAME" "$DBPASS" "$DBNAME"    \
            "$SRVIP1" "$SRVIP2" "test" "1" "none"
        else
            echo "Operation aborted"
        fi
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

    help)
        echo "Clone the OpenSHA repositories into the current directory:"
        echo "  boaf.sh clone"
        echo "Update the OpenSHA repositories:"
        echo "  boaf.sh update"
        echo "Delete all compiled files:"
        echo "  boaf.sh clean"
        echo "Compile the OpenSHA code to create AAFS:"
        echo "  boaf.sh compile"
        echo "Package the AAFS jar file:"
        echo "  boaf.sh pack"
        echo "Compile the OpenSHA code to create the generic aftershock GUI:"
        echo "  boaf.sh compilegui"
        echo "Package the GUI jar file, and bundle with private server configuration file:"
        echo "  boaf.sh packgui GUIDATE FILENAME"
        echo "Copy the AAFS jar file and required libraries into /opt/aafs/oefjava:"
        echo "  boaf.sh deploy"
        echo "Copy the AAFS configuration files and scripts into /opt/aafs and its subdirectories:"
        echo "  boaf.sh deploycfg"
        echo "Use git diff to compare the configuration files in /opt/aafs to the originals:"
        echo "  boaf.sh diffcfg"
        echo "Same as diffcfg except forces the use of colored text when displaying changes:"
        echo "  boaf.sh diffcfgc"
        echo "Create a server configuration file for a single-server configuration:"
        echo "  boaf.sh config_server_solo SRVIP1 REPSET1 DBNAME DBUSER DBPASS SRVNAME PDLOPT"
        echo "Create and save a server configuration file for a single-server configuration:"
        echo "  boaf.sh config_file_server_solo FILENAME SRVIP1 REPSET1 DBNAME DBUSER DBPASS SRVNAME PDLOPT"
        echo "Create a server configuration file for server #1 in a dual-server configuration:"
        echo "  boaf.sh config_server_1 SRVIP1 REPSET1 SRVIP2 REPSET2 DBNAME DBUSER DBPASS SRVNAME PDLOPT"
        echo "Create and save a server configuration file for server #1 in a dual-server configuration:"
        echo "  boaf.sh config_file_server_1 FILENAME SRVIP1 REPSET1 SRVIP2 REPSET2 DBNAME DBUSER DBPASS SRVNAME PDLOPT"
        echo "Create a server configuration file for server #2 in a dual-server configuration:"
        echo "  boaf.sh config_server_2 SRVIP1 REPSET1 SRVIP2 REPSET2 DBNAME DBUSER DBPASS SRVNAME PDLOPT"
        echo "Create and save a server configuration file for server #2 in a dual-server configuration:"
        echo "  boaf.sh config_file_server_2 FILENAME SRVIP1 REPSET1 SRVIP2 REPSET2 DBNAME DBUSER DBPASS SRVNAME PDLOPT"
        echo "Install a server configuration file for a development server in single-server configuration:"
        echo "  boaf.sh config_server_dev"
        echo "Write a server configuration file for a development server in single-server configuration:"
        echo "  boaf.sh config_file_server_dev FILENAME"
        echo "Install an action configuration file that accepts earthquakes only from the US:"
        echo "  boaf.sh config_action_usa"
        echo "Write an action configuration file that accepts earthquakes only from the US:"
        echo "  boaf.sh config_file_action_usa FILENAME"
        echo "Install an action configuration file that accepts earthquakes world-wide:"
        echo "  boaf.sh config_action_dev"
        echo "Write an action configuration file that accepts earthquakes world-wide:"
        echo "  boaf.sh config_file_action_dev FILENAME"
        echo "Configure and package the production GUI:"
        echo "  boaf.sh config_packgui GUIDATE SRVIP1 REPSET1 SRVIP2 REPSET2 DBNAME DBUSER DBPASS"
        echo "Create and save a server configuration file for the production GUI:"
        echo "  boaf.sh config_file_packgui FILENAME SRVIP1 REPSET1 SRVIP2 REPSET2 DBNAME DBUSER DBPASS"
        echo "Run a class in the org.opensha.oaf package, using the compiled-in configuration:"
        echo "  boaf.sh run CLASSNAME [PARAMETER...]"
        echo "Run a class in the org.opensha.oaf package, reading configuration from ./oafcfg:"
        echo "  boaf.sh runcfg CLASSNAME [PARAMETER...]"
        echo "Run a class in the org.opensha.oaf package, reading configuration from /opt/aafs/oafcfg:"
        echo "  boaf.sh runaafs CLASSNAME [PARAMETER...]"
        echo "Run a class in any package, using the compiled-in configuration:"
        echo "  boaf.sh runany FULLCLASSNAME [PARAMETER...]"
        ;;

    *)
        echo "Usage: 'boaf.sh help' to display help."
        exit 1
        ;;
esac

exit 0

