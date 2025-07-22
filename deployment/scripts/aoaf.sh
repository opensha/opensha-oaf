#! /bin/bash

# This is a quick-start script for deploying and updating AAFS.

# This script must be run in the aftershock user account.
# When the script is started, the current directory must be the directory
# that is used to download and compile the OAF code.

# This script starts by sourcing the option file oaf_config.sh.
# It looks for oaf_config.sh first in the current directory, and then
# in the home directory.

# COMMANDS FOR INITIAL INSTALLATION
#
# prepare_system
#
#     Install required software packages, and then install Java.
#     Also, set the AAFS environment variables.
#     This is the first command to run when setting up a new system
#     (exception: if you are installing certificates, install them first).
#     Before running this command, update the operating system.
#     After running this command, you will need to log out and log in, or reboot.
#     Then, use "install_oaf" to continue the installation.
#
#     Note: Update the operating system before running this command.
#
# install_oaf
#
#     Install and configure the OAF software and MongoDB.
#     After running this command, you should reboot the system.
#     Then, use "initialize_oaf" to initialize the OAF database,
#     or use "restore_oaf" to restore the OAF database from a backup.
#
# install_oaf_44
# install_oaf_50
# install_oaf_60
# install_oaf_70
#
#     Install and configure the OAF software and a specific version of MongoDB.
#     Performs the same function as install_oaf except using MongoDB 4.4, 5.0,
#     6.0, or 7.0, respectively.
#
# install_oaf_no_mongo
#
#     Install and configure the OAF software without installing MongoDB.
#
# initialize_oaf  <relay_mode>  <configured_primary>
#
#     Initialize the OAF database.
#     After running this command, you should be able to launch the AAFS software.
#
#     Normally, this command is given as "initialize_oaf pair 1" for a dual-server
#     configuration, or "initialize_oaf solo 1" for a single-server configuration,
#     when creating a new installation.
#
#     <relay_mode> is the desired mode.  It can have one of three values:
#
#       solo - AAFS operates in single-server mode.
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
#       Single-server configurations must use "solo".  Dual-server configurations
#       must use "pair" or "watch".
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
#       Single-server configurations must use "1" or "this".  Dual-server
#       configurations can use "1", "2", "this", or "other".
#
# restore_oaf  <filename>  <relay_mode>  <configured_primary>
#
#     Restore the OAF database from a backup.
#     After running this command, you should be able to launch the AAFS software.
#
#     The intended use of this command is to completely rebuild a server,
#     or to replace one server with another.
#
#     <filename> is the name of a file that contains a database backup.  It must
#     have been created with the "stop_aafs_for_update" command, or with the
#     "moaf.sh backup_database_gzip" command.
#
#     <relay_mode> and <configured_primary> are the same as above.

# COMMANDS TO MODIFY THE INSTALLATION
#
# update_oaf
#
#     Download updates to the OAF software, then install and configure it.
#
# freshen_oaf
#
#     Download a fresh copy of the OAF software, then install and configure it.
#
# reconfigure_oaf
#
#     Reconfigure the OAF software, without updating or rebuilding it.
#
#     This command does not update configuration information stored in the GUI.
#     You can use "rebuild_gui" to do that.
#
# rebuild_gui
#
#     Rebuild the GUI, without updating or rebuilding OAF.
#
#     This command can be executed while AAFS and MongoDB are running.
#
# update_java
#
#     Obtain and install a fresh copy of Java.  This command can be used to
#     install Java updates, or to switch from one Java distribution to another.
#
#     Note: After updating Java, you should update (or freshen) the OAF software.
#
# update_aafs_vars
#
#     Update the AAFS environment variables.
#     After running this command, you will need to log out and log in, or reboot.
#
# reconfigure_mongo
#
#     Create a new MongoDB configuration file.  This command can be used to
#     change the MongoDB configuration file when needed because of updates to
#     the OAF software or to MongoDB itself.
#
# upgrade_mongo_42_to_44
# upgrade_mongo_44_to_50
# upgrade_mongo_50_to_60
# upgrade_mongo_60_to_70_not_recommended
#
#     Upgrade an existing installation of MongoDB from one version to the next
#     version.  It upgrades from 4.2 to 4.4, or from 4.4 to 5.0, or from 5.0 to 6.0,
#     or from 6.0 to 7.0, respectively.
#     This command registers the upgraded MongoDB with the OS package manager.
#     After running this command, you need to do an OS update to complete the
#     MongoDB upgrade.
#
#     Note: Upgrade from 6.0 to 7.0 is not recommended because we have seen it fail.
#
# set_mongo_compatibility_to_44
# set_mongo_compatibility_to_50
# set_mongo_compatibility_to_60
# set_mongo_compatibility_to_70
#
#     After upgrading an existing installation of MongoDB from one version to
#     the next version, and checking that the new version is operating successfully,
#     use this command to set the MongoDB feature compatibility version to the
#     new version, which can be 4.4, 5.0, 6.0, or 7.0 respectively.
#
# get_mongo_compatibility
#
#     Display the current MongoDB feature compatibility version.

# COMMANDS FOR UPDATING THE SYSTEM
#
# stop_aafs_for_update  <java_option>  <oaf_option>  <backup_filename>
#
#     Stop AAFS and MongoDB, so that updates can be performed.
#
#     The java_option specifies whether or not to update Java.  It must be
#     "java" to update Java, or "nojava" to skip the Java update.
#
#     The oaf_option specifies whether or not to update the OAF software.  It must be
#     "oaf" to update the OAF software, or "nooaf" to skip the OAF software update.
#
#     The OAF database is backed up into the file specified by backup_filename.
#     To skip the database backup, use "nobackup" as the filename.
#
#     After running this command, you can update the operating system and perform
#     any other updates, and you can reboot if needed.
#
#     This command can be used in dual-server or single-server configurations.
#     In a dual-server configuration, it switches the local server to secondary
#     and waits for confirmation that the remote server has switched to primary.
#
#     After completing all updates, use "start_aafs_after_update" to re-start AAFS.
#
# start_aafs_after_update
#
#     Re-start AAFS and MongoDB after performing updates.
#
#     This command should be used if "stop_aafs_for_update" was used for shutdown.
#
#     In a dual-server configuration, after running this command you should update
#     the other server if you have not already done so.  After both servers are
#     updated, use "resume_normal_mode" on one of the servers to resume the normal
#     "pair 1" mode of operation.
#
# resume_normal_mode
#
#     In a dual-server configuration, use this command to resotre the normal
#     "pair 1" mode of operation after both servers have been updated.
#
# stop_local_for_update  <java_option>  <oaf_option>  <backup_filename>
#
#     Stop local AAFS and MongoDB, so that updates can be performed.
#
#     The java_option specifies whether or not to update Java.  It must be
#     "java" to update Java, or "nojava" to skip the Java update.
#
#     The oaf_option specifies whether or not to update the OAF software.  It must be
#     "oaf" to update the OAF software, or "nooaf" to skip the OAF software update.
#
#     The OAF database is backed up into the file specified by backup_filename.
#     To skip the database backup, use "nobackup" as the filename.
#
#     After running this command, you can update the operating system and perform
#     any other updates, and you can reboot if needed.
#
#     This command can be used in dual-server or single-server configurations.
#     This command does not switch the mode, and so can be used when shutting
#     down both servers in a dual-server configuration.  (Use "stop_aafs_for_update"
#     on the secondary server, and then "stop_local_for_update" on the primary.)
#
#     After completing all updates, use "start_local_after_update" to re-start AAFS.
#
# start_local_after_update
#
#     Re-start local AAFS and MongoDB after performing updates.
#
#     This command should be used if "stop_local_for_update" was used for shutdown.

# COMMANDS TO MANAGE CERTIFICATES
#
# install_certs  <source_dir>
#
#     Install certificates.  This is used to enable TLS and client authentication.
#
#     <source_dir> is a directory that contains the certificates to install.
#     It may contain three sets of files:
#
#       Root CA certificate:
#         oafcert_root_ca.pem
#
#       Server certificate:
#         oafcert_server.pem
#         oafcert_server_pass.txt
# 
#       Client or application certificate:
#         oafcert_app.pem
#         oafcert_app.p12
#         oafcert_app_pass.txt
#         oafcert_app_p12_pass.txt
#
#     The following combinations are allowed:
#
#       Root+Server:        A server that requires TLS.
#       Root+Server+Client: A server that requires TLS and client authentication.
#       Root:               A workstation that uses TLS.
#       Root+Client:        A workstation that uses TLS and client authentication.
#
#     This command can be used either before or after the software is installed.
#     If used after the software is installed, then any existing certificates are
#     replaced, and the command also updates Java, the AAFS environment variables,
#     the MongoDB configuration, and the AAFS configuration.
#
# remove_certs
#
#     Remove certificates.  This is used to disable TLS.
#
#     This command removes any certificates that were previously installed
#     with the install_certs command.
#
#     This command can be used either before or after the software is installed.
#     If used after the software is installed, then any existing certificates are
#     removed, and the command also updates Java, the AAFS environment variables,
#     the MongoDB configuration, and the AAFS configuration.

# COMMANDS FOR SPECIAL FUNCTIONS
#
# run_mongosh
#
#     Start the MongoDB shell (mongosh).
#
#     It is started with the appropriate command-line options for TLS.
#
# run_mongosh_to  <host_address>
#
#     Start the MongoDB shell (mongosh), for the given host address.
#
#     It is started with the appropriate command-line options for TLS.




#----- Functions -----




# Function to test if MongoDB is running.
# There are no arguments.
# Return value is 0 if MongoDB is running, non-zero if not.

q_is_mongo_running () {
    pgrep mongod >/dev/null 2>&1
}




# Function to test if AAFS is running.
# There are no arguments.
# Return value is 0 if AAFS is running, non-zero if not.

q_is_aafs_running () {
    ps -eF | grep -q "/opt/aafs/oefjava/[o]efjava"
}




# Function to start MongoDB.
# There are no arguments.
# The caller should check that MongoDB is not running before calling this function.

q_do_start_mongo () {
    echo "Starting MongoDB..."
    sudo /usr/sbin/service mongod start
}




# Function to stop MongoDB.
# There are no arguments.
# The caller should check that MongoDB is running before calling this function.

q_do_stop_mongo () {
    echo "Stopping MongoDB..."
    sudo /usr/sbin/service mongod stop
}




# Load and validate the OAF configuration file.
# Also set up internal variables.
# Exit the script if an error is detected.

q_load_oaf_config () {
    echo "Loading OAF configuration file..."

    #--- Named constants ---

    # Allowed values of the OS version.
    val_OS_AMAZON_LINUX_2="amazonlinux2"
    val_OS_AMAZON_LINUX_2023="amazonlinux2023"
    val_OS_UBUNTU_2004="ubuntu2004"
    val_OS_UBUNTU_2204="ubuntu2204"
    val_OS_CENTOS_7="centos7"

    # Allowed values of the server option.
    val_SERVER_PRIMARY="primary"
    val_SERVER_SECONDARY="secondary"
    val_SERVER_SOLO="solo"
    val_SERVER_DEV="dev"

    # Allowed values of the action option.
    val_ACTION_USA="usa"
    val_ACTION_DEV="dev"

    # Special values of the PDL option.
    val_PDL_NONE="none"
    val_PDL_DEV="dev"

    # Yes and No values for flags.
    val_YES="Y"
    val_NO="N"

    # Values of the internal variable my_OS_TYPE.
    val_OSTYPE_AMAZON="amazon"
    val_OSTYPE_UBUNTU="ubuntu"
    val_OSTYPE_CENTOS="centos"

    # Values of the internal variable my_OS_GEN.
    val_OSGEN_ANY="any"
    val_OSGEN_UBUNTU_2004="focal"
    val_OSGEN_UBUNTU_2204="jammy"
    val_OSGEN_AMAZON_2="amazon2"
    val_OSGEN_AMAZON_2023="amazon2023"

    # The local IP address, as returned by hostname.
    # val_LOCAL_IP="$(hostname -I | cut -d' ' -f1)"

    # The local account, as returned by whoami.
    val_LOCAL_ACCOUNT="$(whoami)"

    # The initial working directory, as returned by pwd
    val_PWD="$(pwd)"

    # Temporary work directory (within the current directory).
    val_TEMP_WORK_DIR="$val_PWD/selcfg"

    # Regular expression used to test for non-negative integer (do not enclose in quotes when used in an if statement).
    val_NNINT_REGEX='^(0|([1-9][0-9]*))$'

    # Filename for the launch options script.
    val_LAUNCH_OPTIONS_FILE="/opt/aafs/oafcfg/LaunchOptions.sh"

    # Filenames used for certificate management.
    cval_file_root_ca="oafcert_root_ca"
    cval_file_server="oafcert_server"
    cval_file_client="oafcert_app"
    cval_suffix_pass="_pass.txt"
    cval_suffix_p12_pass="_p12_pass.txt"

    # Directory where certificates are installed on the server.
    cval_CERT_DIR="/etc/oafssl"


    #--- Internal variables, derived from system configuration ---

    # Internal variable holding the operating system type
    my_OS_TYPE=

    # Internal variable holding the operating system generation or version
    my_OS_GEN=

    # Internal flag indicating if Centos SELinux is enabled, "$val_YES" or "$val_NO"
    my_IS_SELINUX="$val_NO"

    # Internal variable holding the path to OS-specific scripts
    my_OS_SPECIFIC_PATH=

    # Internal variable holding the local account in which MongoDB runs
    my_MONGO_LOCAL_ACCOUNT=

    # Internal variables holding default locations of MongoDB data and log files
    my_MONGO_DEF_DATA_PATH=
    my_MONGO_DEF_LOG_PATH=

    # Internal variables holding MongoDB configuration file, and the file we use for backup
    my_MONGO_CONF_FILE=
    my_MONGO_CONF_BACKUP=

    # Internal variables holding MongoDB replica set key file, and the content
    my_MONGO_KEY_FILE=
    my_MONGO_KEY_CONTENT=

    # Internal variable holding list of IP addresses for MongoDB to bind to, comma-separated
    my_MONGO_BIND_IP_LIST=

    # Internal variable holding the replica set name for the local MongoDB
    my_MONGO_LOCAL_REPSET=


    #--- Internal variables, derived from AAFS configuration ---

    # Internal flag indicating if this is a dual server configuration, "$val_YES" or "$val_NO"
    my_IS_DUAL_SERVER="$val_NO"

    # Internal flag indicating if this is a secondary server, "$val_YES" or "$val_NO"
    my_IS_SECONDARY_SERVER="$val_NO"

    # Internal flag indicating if the Java source is a file, "$val_YES" or "$val_NO"
    my_IS_JAVA_FILE_SOURCE="$val_NO"

    # Base filename of the Java source.
    my_JAVA_FILE_BASENAME=


    #--- Internal variables, derived from installed certificates ---

    # Internal flag indicating if a root certificate is installed, "$val_YES" or "$val_NO"
    my_HAS_ROOT_CERT="$val_NO"

    # Internal flag indicating if a server certificate is installed, "$val_YES" or "$val_NO"
    my_HAS_SERVER_CERT="$val_NO"

    # Internal flag indicating if a client certificate is installed, "$val_YES" or "$val_NO"
    my_HAS_CLIENT_CERT="$val_NO"


    #--- Load configuration file ---

    # Source the configuration file, from current or home directory

    if [ -f "oaf_config.sh" ]; then
        source oaf_config.sh
    elif [ -f "~/oaf_config.sh" ]; then
        source ~/oaf_config.sh
    else
        echo "Cannot find configuration file oaf_config.sh"
        exit 1
    fi


    #--- Certificate checks -----

    if [ -d "$cval_CERT_DIR" ]; then
        if [ -f "${cval_CERT_DIR}/${cval_file_root_ca}.pem" ]; then
            my_HAS_ROOT_CERT="$val_YES"
        fi
        if [ -f "${cval_CERT_DIR}/${cval_file_server}.pem" ]; then
            my_HAS_SERVER_CERT="$val_YES"
        fi
        if [ -f "${cval_CERT_DIR}/${cval_file_client}.pem" ]; then
            my_HAS_CLIENT_CERT="$val_YES"
        fi
    fi


    #--- Check and analyze the configuration file ---

    # Check the OS version and set the type and generation

    if [ "$THE_OS_VERSION" == "$val_OS_AMAZON_LINUX_2" ]; then
        my_OS_TYPE="$val_OSTYPE_AMAZON"
        my_OS_GEN="$val_OSGEN_AMAZON_2"
    elif [ "$THE_OS_VERSION" == "$val_OS_AMAZON_LINUX_2023" ]; then
        my_OS_TYPE="$val_OSTYPE_AMAZON"
        my_OS_GEN="$val_OSGEN_AMAZON_2023"
    elif [ "$THE_OS_VERSION" == "$val_OS_UBUNTU_2004" ]; then
        my_OS_TYPE="$val_OSTYPE_UBUNTU"
        my_OS_GEN="$val_OSGEN_UBUNTU_2004"
    elif [ "$THE_OS_VERSION" == "$val_OS_UBUNTU_2204" ]; then
        my_OS_TYPE="$val_OSTYPE_UBUNTU"
        my_OS_GEN="$val_OSGEN_UBUNTU_2204"
    elif [ "$THE_OS_VERSION" == "$val_OS_CENTOS_7" ]; then
        my_OS_TYPE="$val_OSTYPE_CENTOS"
        my_OS_GEN="$val_OSGEN_ANY"
    else
        echo "Invalid operating system version: THE_OS_VERSION = $THE_OS_VERSION" 
        exit 1
    fi

    # Set variables that depend on the OS type

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        my_IS_SELINUX="$val_NO"
        my_OS_SPECIFIC_PATH="$val_PWD/opensha-oaf/deployment/scripts/amazon"

        my_MONGO_LOCAL_ACCOUNT="mongod"
        my_MONGO_DEF_DATA_PATH="/var/lib/mongo"
        my_MONGO_DEF_LOG_PATH="/var/log/mongodb"
        my_MONGO_CONF_FILE="/etc/mongod.conf"
        my_MONGO_CONF_BACKUP="/etc/mongod_0.conf"
        my_MONGO_KEY_FILE="/etc/mongod_key.yaml"
        my_MONGO_KEY_CONTENT="oafmongorepsetkey"

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        my_IS_SELINUX="$val_NO"
        my_OS_SPECIFIC_PATH="$val_PWD/opensha-oaf/deployment/scripts/ubuntu"

        my_MONGO_LOCAL_ACCOUNT="mongodb"
        my_MONGO_DEF_DATA_PATH="/var/lib/mongodb"
        my_MONGO_DEF_LOG_PATH="/var/log/mongodb"
        my_MONGO_CONF_FILE="/etc/mongod.conf"
        my_MONGO_CONF_BACKUP="/etc/mongod_0.conf"
        my_MONGO_KEY_FILE="/etc/mongod_key.yaml"
        my_MONGO_KEY_CONTENT="oafmongorepsetkey"

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        my_IS_SELINUX="$val_NO"
        my_OS_SPECIFIC_PATH="$val_PWD/opensha-oaf/deployment/scripts/centos"

        my_MONGO_LOCAL_ACCOUNT="mongod"
        my_MONGO_DEF_DATA_PATH="/var/lib/mongo"
        my_MONGO_DEF_LOG_PATH="/var/log/mongodb"
        my_MONGO_CONF_FILE="/etc/mongod.conf"
        my_MONGO_CONF_BACKUP="/etc/mongod_0.conf"
        my_MONGO_KEY_FILE="/etc/mongod_key.yaml"
        my_MONGO_KEY_CONTENT="oafmongorepsetkey"

        if [ "$(getenforce)" == "Permissive" ]; then
            my_IS_SELINUX="$val_YES"
        elif [ "$(getenforce)" == "Enforcing" ]; then
            my_IS_SELINUX="$val_YES"
        fi

    fi

    # Check for ARM CPU

    my_UNAME_ARCH=$(uname -i)
    case "$my_UNAME_ARCH" in
        arm*)
            my_ARM_ARCH="$val_YES"
            ;;
        aarch64)
            my_ARM_ARCH="$val_YES"
            ;;
        *)
            my_ARM_ARCH="$val_NO"
            ;;
    esac

    # If core count is blank, default it to zero; check it is non-negative

    if [ -z "$CPU_CORE_COUNT" ]; then
        CPU_CORE_COUNT="0"
    fi

    if [[ ! "$CPU_CORE_COUNT" =~ $val_NNINT_REGEX ]]; then
        echo "Core count must be an integer"
        exit 1
    fi

    if [ "$CPU_CORE_COUNT" -lt 0 ]; then
        echo "Core count cannot be negative"
        exit 1
    fi

    # If Java source is omitted, default to Coretto 17

    if [ -z "$JAVA_SOURCE" ]; then
        if [ "$my_ARM_ARCH" == "$val_YES" ]; then
           JAVA_SOURCE="https://corretto.aws/downloads/latest/amazon-corretto-17-aarch64-linux-jdk.tar.gz"
        else
           JAVA_SOURCE="https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz"
        fi
    fi

    if [ "${JAVA_SOURCE:0:1}" == "/" ]; then
        my_IS_JAVA_FILE_SOURCE="$val_YES"
    elif [ "${JAVA_SOURCE:0:1}" == "~" ]; then
        JAVA_SOURCE="${JAVA_SOURCE/#~\//$HOME/}"
        my_IS_JAVA_FILE_SOURCE="$val_YES"
    else
        my_IS_JAVA_FILE_SOURCE="$val_NO"
    fi

    my_JAVA_FILE_BASENAME="$(basename "$JAVA_SOURCE")"

    if [ "$my_IS_JAVA_FILE_SOURCE" == "$val_YES" ]; then
        if [ ! -f "$JAVA_SOURCE" ]; then
            echo "Cannot find Java distribution file: JAVA_SOURCE = $JAVA_SOURCE"
            exit 1
        fi
    fi

    if [ -n "$JAVA_CERT_FILE" ]; then
        if [ "${JAVA_CERT_FILE:0:1}" == "~" ]; then
            JAVA_CERT_FILE="${JAVA_CERT_FILE/#~\//$HOME/}"
        fi
        if [ ! -f "$JAVA_CERT_FILE" ]; then
            echo "Cannot find Java certificate file: JAVA_CERT_FILE = $JAVA_CERT_FILE"
            exit 1
        fi
    fi

    # If Java maximum memory size is blank, default it to zero; check it is non-negative

    if [ -z "$JAVA_MAX_MEMORY_GB" ]; then
        JAVA_MAX_MEMORY_GB="0"
    fi

    if [[ ! "$JAVA_MAX_MEMORY_GB" =~ $val_NNINT_REGEX ]]; then
        echo "Java maximum memory size must be an integer"
        exit 1
    fi

    if [ "$JAVA_MAX_MEMORY_GB" -lt 0 ]; then
        echo "Java maximum memory size cannot be negative"
        exit 1
    fi

    # If MongoDB cache size is blank, default it to zero; check it is non-negative

    if [ -z "$MONGO_CACHE_GB" ]; then
        MONGO_CACHE_GB="0"
    fi

    if [[ ! "$MONGO_CACHE_GB" =~ $val_NNINT_REGEX ]]; then
        echo "MongoDB cache size must be an integer"
        exit 1
    fi

    if [ "$MONGO_CACHE_GB" -lt 0 ]; then
        echo "MongoDB cache size cannot be negative"
        exit 1
    fi

    # Check the server option

    if [ "$SERVER_OPTION" == "$val_SERVER_PRIMARY" ]; then

        # Primary server in a dual-server configuration

        my_IS_DUAL_SERVER="$val_YES"
        my_IS_SECONDARY_SERVER="$val_NO"

    elif [ "$SERVER_OPTION" == "$val_SERVER_SECONDARY" ]; then

        # Secondary server in a dual-server configuration

        my_IS_DUAL_SERVER="$val_YES"
        my_IS_SECONDARY_SERVER="$val_YES"

    elif [ "$SERVER_OPTION" == "$val_SERVER_SOLO" ]; then

        # Single-server configuration

        my_IS_DUAL_SERVER="$val_NO"
        my_IS_SECONDARY_SERVER="$val_NO"

    elif [ "$SERVER_OPTION" == "$val_SERVER_DEV" ]; then

        # Single-server development configuration

        my_IS_DUAL_SERVER="$val_NO"
        my_IS_SECONDARY_SERVER="$val_NO"

        # Force defaults for all except ACTION_OPTION and SERVER_IP_1

        MONGO_ADMIN_USER="mongoadmin"
        MONGO_ADMIN_PASS="mongoadmin"
        MONGO_NAME="usgs"
        MONGO_USER="usgs"
        MONGO_PASS="usgs"
        MONGO_REP_SET_1="rs0"
        MONGO_REP_SET_2=
        PDL_OPTION="none"
        SERVER_IP_2=
        SERVER_NAME_1="test"
        SERVER_NAME_2=

    else
        echo "Invalid server option: SERVER_OPTION = $SERVER_OPTION"
        exit 1
    fi

    # Check the action option

    if [ "$ACTION_OPTION" == "$val_ACTION_USA" ]; then
        :
    elif [ "$ACTION_OPTION" == "$val_ACTION_DEV" ]; then
        :
    else
        echo "Invalid action option: ACTION_OPTION = $ACTION_OPTION"
        exit 1
    fi

    # Check MongoDB options

    if [ -z "$MONGO_ADMIN_USER" ]; then
        echo "Missing MongoDB administrative username: MONGO_ADMIN_USER"
        exit 1
    fi

    if [ -z "$MONGO_ADMIN_PASS" ]; then
        echo "Missing MongoDB administrative password: MONGO_ADMIN_PASS"
        exit 1
    fi

    if [ -z "$MONGO_NAME" ]; then
        echo "Missing MongoDB database name: MONGO_NAME"
        exit 1
    fi

    if [ -z "$MONGO_USER" ]; then
        echo "Missing MongoDB username: MONGO_USER"
        exit 1
    fi

    if [ -z "$MONGO_PASS" ]; then
        echo "Missing MongoDB password: MONGO_PASS"
        exit 1
    fi

    if [ -z "$MONGO_REP_SET_1" ]; then
        echo "Missing MongoDB replica set #1 name: MONGO_REP_SET_1"
        exit 1
    fi

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        if [ -z "$MONGO_REP_SET_2" ]; then
            echo "Missing MongoDB replica set #2 name: MONGO_REP_SET_2"
            exit 1
        fi
        if [ "$MONGO_REP_SET_1" == "$MONGO_REP_SET_2" ]; then
            echo "MongoDB replica set #1 and #2 names cannot be identical"
            exit 1
        fi
    else
        if [ -n "$MONGO_REP_SET_2" ]; then
            echo "Cannot specify MongoDB replica set #2 name in a single-server configuration"
            exit 1
        fi
    fi

    # Check PDL option

    if [ -z "$PDL_OPTION" ]; then
        echo "Missing PDL option: PDL_OPTION"
        exit 1
    fi

    if [ "$PDL_OPTION" != "$val_PDL_NONE" ]; then
        if [ "$ACTION_OPTION" == "$val_ACTION_DEV" ]; then
            echo "Cannot send forecasts to PDL when ACTION_OPTION = "'"'"$val_ACTION_DEV"'"'
            exit 1
        fi
    fi

    # Check server options

    if [ "$my_IS_SECONDARY_SERVER" == "$val_YES" ]; then
        if [ -z "$SERVER_IP_2" ]; then
            SERVER_IP_2="$(hostname -I | cut -d' ' -f1)"
        fi
    else
        if [ -z "$SERVER_IP_1" ]; then
            SERVER_IP_1="$(hostname -I | cut -d' ' -f1)"
        fi
    fi

    if [ -z "$SERVER_IP_1" ]; then
        echo "Missing server #1 IP address: SERVER_IP_1"
        exit 1
    fi

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        if [ -z "$SERVER_IP_2" ]; then
            echo "Missing server #2 IP address: SERVER_IP_2"
            exit 1
        fi
        if [ "$SERVER_IP_1" == "$SERVER_IP_2" ]; then
            echo "Server #1 and #2 IP addresses cannot be identical"
            exit 1
        fi
    else
        if [ -n "$SERVER_IP_2" ]; then
            echo "Cannot specify server #2 IP address in a single-server configuration"
            exit 1
        fi
    fi

    if [ -z "$SERVER_NAME_1" ]; then
        echo "Missing server #1 name: SERVER_NAME_1"
        exit 1
    fi

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        if [ -z "$SERVER_NAME_2" ]; then
            echo "Missing server #2 name: SERVER_NAME_2"
            exit 1
        fi
        if [ "$SERVER_NAME_1" == "$SERVER_NAME_2" ]; then
            echo "Server #1 and #2 names cannot be identical"
            exit 1
        fi
    else
        if [ -n "$SERVER_NAME_2" ]; then
            echo "Cannot specify server #2 name in a single-server configuration"
            exit 1
        fi
    fi

    # More MongoDB values, local IP and replica set

    if [ "$my_IS_SECONDARY_SERVER" == "$val_YES" ]; then
        my_MONGO_LOCAL_REPSET="$MONGO_REP_SET_2"
    else
        my_MONGO_LOCAL_REPSET="$MONGO_REP_SET_1"
    fi

    if [ -z "$MONGO_BIND_IP" ]; then
        if [ "$my_IS_SECONDARY_SERVER" == "$val_YES" ]; then
            MONGO_BIND_IP="$SERVER_IP_2"
        else
            MONGO_BIND_IP="$SERVER_IP_1"
        fi
    fi

    if [ "$MONGO_BIND_IP" == "127.0.0.1" ]; then
        my_MONGO_BIND_IP_LIST="$MONGO_BIND_IP"
    elif [ "$MONGO_BIND_IP" == "0.0.0.0" ]; then
        my_MONGO_BIND_IP_LIST="$MONGO_BIND_IP"
    else
        my_MONGO_BIND_IP_LIST="127.0.0.1,$MONGO_BIND_IP"
    fi

    # Check the GUI date

    if [ -z "$GUI_DATE" ]; then
        GUI_DATE="$(date +%Y_%m_%d)"
    fi

}




# Function to ensure that the temporary work directory exists.
# There are no arguments.

q_ensure_temp_work_dir () {
    if [ ! -d "$val_TEMP_WORK_DIR" ]; then
        mkdir "$val_TEMP_WORK_DIR"
    fi
}




# Function to check if the PDL key file is installed.
# Displays a message (but does not abort the script) if the PDL key file is not installed.
# Also announces the PDL options.
# There are no arguments.

q_check_pdl_key_file () {

    if [ "$PDL_OPTION" == "$val_PDL_NONE" ]; then
        echo "OAF is configured so it will NOT send forecasts to PDL"
    elif [ "$PDL_OPTION" == "$val_PDL_DEV" ]; then
        echo "OAF is configured send forecasts to PDL-DEVELOPMENT"
    else
        echo "OAF is configured send forecasts to PDL-PRODUCTION"
        if [ ! -f "/opt/aafs/key/$PDL_OPTION" ]; then
            echo "You need to install the PDL key file: /opt/aafs/key/$PDL_OPTION"
        fi
    fi

}




# Check if AAFS is already installed.
# Exit the script if it is already installed.

q_check_installed () {

    if [ -d /opt/aafs ]; then
        echo "AAFS software is already installed"
        exit 1
    fi

    if [ -d /data ]; then
        if [ -d /data/aafs ]; then
            echo "AAFS software is already installed"
            exit 1
        fi
    fi

}




# Install required packages.

q_install_packages () {
    echo "Installing required packages..."

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        sudo yum groupinstall -y "Development Tools"

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        sudo apt-get -y install vim
        sudo apt-get -y install build-essential
        sudo apt-get -y install git
        sudo apt-get -y install curl
        sudo apt-get -y install gnupg
        sudo apt-get -y install zip

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        sudo yum install -y vim-enhanced
        sudo yum groupinstall -y "Development Tools"
        sudo yum install -y wget

    fi

}




# Create required directories.

q_create_dirs () {
    echo "Creating required directories..."

    # Code directory

    sudo mkdir /opt/aafs
    sudo chown "${val_LOCAL_ACCOUNT}:" /opt/aafs

    # Data directories

    if [ ! -d /data ]; then
        sudo mkdir /data
    fi

    sudo mkdir /data/aafs
    sudo chown "${val_LOCAL_ACCOUNT}:" /data/aafs

    mkdir /data/aafs/mongodata
    mkdir /data/aafs/mongolog
    mkdir /data/aafs/logs
    mkdir /data/aafs/diag
    mkdir /data/aafs/pdldata
    mkdir /data/aafs/pids
    mkdir /data/aafs/backup

}




# Download OpenSHA into the current directory.

q_download_opensha () {
    echo "Downloading OpenSHA code..."

    # Download the OAF build script

    if [ -f boaf.sh ]; then
        rm boaf.sh
    fi
    wget https://github.com/opensha/opensha-oaf/raw/master/deployment/scripts/boaf.sh
    chmod 755 boaf.sh

    # Download the OpenSHA code

    ./boaf.sh clone

}




# Update OpenSHA in the current directory.

q_update_opensha () {
    echo "Updating OpenSHA code..."

    # Download the OAF build script

    if [ -f boaf.sh ]; then
        rm boaf.sh
    fi
    wget https://github.com/opensha/opensha-oaf/raw/master/deployment/scripts/boaf.sh
    chmod 755 boaf.sh

    # Update the OpenSHA code

    ./boaf.sh update

}




# Install Java in /usr/local/java.
# If there is an existing Java installation, it is replaced.

q_install_java () {
    echo "Installing or updating Java..."

    # If there is an existing Java installation, remove it

    if [ -d /usr/local/java ]; then
        sudo rm -r /usr/local/java
    fi

    q_ensure_temp_work_dir

    # Make the Java directory

    sudo mkdir /usr/local/java

    # Copy the Java distribution, from a local file or network location

    if [ "$my_IS_JAVA_FILE_SOURCE" == "$val_YES" ]; then
        sudo cp "$JAVA_SOURCE" /usr/local/java
    else
        if [ -f "$val_TEMP_WORK_DIR/$my_JAVA_FILE_BASENAME" ]; then
            rm "$val_TEMP_WORK_DIR/$my_JAVA_FILE_BASENAME"
        fi
        cd "$val_TEMP_WORK_DIR"
        wget "$JAVA_SOURCE"
        cd - >/dev/null
        if [ ! -f "$val_TEMP_WORK_DIR/$my_JAVA_FILE_BASENAME" ]; then
            echo "Failed to obtain Java distribution: $my_JAVA_FILE_BASENAME"
            echo "Java download was attempted from: $JAVA_SOURCE"
            exit 1
        fi
        sudo cp "$val_TEMP_WORK_DIR/$my_JAVA_FILE_BASENAME" /usr/local/java
        rm "$val_TEMP_WORK_DIR/$my_JAVA_FILE_BASENAME"
    fi

    if [ ! -f "/usr/local/java/$my_JAVA_FILE_BASENAME" ]; then
        echo "Failed to obtain Java distribution: $my_JAVA_FILE_BASENAME"
        exit 1
    fi

    # Extract the Java files

    cd /usr/local/java
    sudo tar --strip-components 1 -zxvf "$my_JAVA_FILE_BASENAME"
    cd - >/dev/null

    if [ ! -f /usr/local/java/bin/java ]; then
        echo "Failed to obtain Java executable"
        exit 1
    fi

    # Install digital certificate if needed

    if [ -n "$JAVA_CERT_FILE" ]; then
        sudo /usr/local/java/bin/keytool -importcert -noprompt -keystore /usr/local/java/lib/security/cacerts -storepass changeit -alias "oafjavacert" -file "$JAVA_CERT_FILE"
    fi

    if [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
        sudo /usr/local/java/bin/keytool -importcert -noprompt -keystore /usr/local/java/lib/security/cacerts -storepass changeit -alias "oafrootcert" -file "${cval_CERT_DIR}/${cval_file_root_ca}.pem"
    fi

    # Insert a link to Java in /usr/bin, if it was not already done

    if [ ! -f /usr/bin/java ]; then
        cd /usr/bin
        sudo ln -s /usr/local/java/bin/java java
        cd - >/dev/null
    fi

    # Add Java to the PATH and JAVA_HOME, if it was not already done

    if [ ! -f /etc/profile.d/javapath.sh ]; then

        if [ -f "$val_TEMP_WORK_DIR/javapath.sh" ]; then
            rm "$val_TEMP_WORK_DIR/javapath.sh"
        fi

        echo 'JAVA_HOME=/usr/local/java' > "$val_TEMP_WORK_DIR/javapath.sh"
        echo 'PATH=$PATH:$JAVA_HOME/bin' >> "$val_TEMP_WORK_DIR/javapath.sh"
        echo 'export JAVA_HOME' >> "$val_TEMP_WORK_DIR/javapath.sh"
        echo 'export PATH' >> "$val_TEMP_WORK_DIR/javapath.sh"

        sudo cp "$val_TEMP_WORK_DIR/javapath.sh" /etc/profile.d/javapath.sh
        source /etc/profile.d/javapath.sh
    fi

}




# Check if Java is installed.
# Exit the script if it is not installed, or if logout/login is required.

q_check_java () {

    if [ ! -f /usr/local/java/bin/java ]; then
        echo "Java is not installed"
        exit 1
    fi
    if [ ! -f /etc/profile.d/javapath.sh ]; then
        echo "Java is not installed"
        exit 1
    fi

    if [ "$JAVA_HOME" != "/usr/local/java" ]; then
        echo "You need to log out and log in (or reboot) to complete the Java installation"
        exit 1
    fi

}




# Configure Java DNS setting.

q_configure_java_dns () {
    echo "Configuring Java DNS..."

    # The standard location of java.security for Java 11 and higher, and a backup filename

    my_JAVA_SEC_FILE="/usr/local/java/conf/security/java.security"
    my_JAVA_SEC_BACKUP="/usr/local/java/conf/security/java.security.bak"

    # Only do this if the file exists

    if [ -f "$my_JAVA_SEC_FILE" ]; then

        # Save the original java.security file, if not previously saved

        if [ ! -f "$my_JAVA_SEC_BACKUP" ]; then
            sudo cp -pi "$my_JAVA_SEC_FILE" "$my_JAVA_SEC_BACKUP"
        fi

        # Make file edits, starting with the original file

        q_ensure_temp_work_dir

        cat "$my_JAVA_SEC_BACKUP"    \
        | sed 's|^ *# *networkaddress\.cache\.ttl *=.*$|networkaddress.cache.ttl=60|'    \
        | sed 's|^ *networkaddress\.cache\.ttl *= *-.*$|networkaddress.cache.ttl=60|'    \
        > "$val_TEMP_WORK_DIR/java.security.tmp"

        sudo cp "$val_TEMP_WORK_DIR/java.security.tmp" "$my_JAVA_SEC_FILE"
    else
        echo "Cannot configure Java DNS because java.security file is not found"
    fi

}




# Set AAFS environment variables.

q_set_aafs_vars () {
    echo "Setting AAFS environment variables..."

    if [ "$CPU_CORE_COUNT" -gt 0 ]; then

        q_ensure_temp_work_dir

        if [ -f "$val_TEMP_WORK_DIR/aafsvars.sh" ]; then
            rm "$val_TEMP_WORK_DIR/aafsvars.sh"
        fi

        echo "AAFS_NUM_THREADS=$CPU_CORE_COUNT" > "$val_TEMP_WORK_DIR/aafsvars.sh"
        echo 'export AAFS_NUM_THREADS' >> "$val_TEMP_WORK_DIR/aafsvars.sh"

        if [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
            echo "AAFS_SSL_DIR=\"$cval_CERT_DIR\"" >> "$val_TEMP_WORK_DIR/aafsvars.sh"
            echo 'export AAFS_SSL_DIR' >> "$val_TEMP_WORK_DIR/aafsvars.sh"
        fi

        if [ -f /etc/profile.d/aafsvars.sh ]; then
            sudo rm -f /etc/profile.d/aafsvars.sh
        fi

        sudo cp "$val_TEMP_WORK_DIR/aafsvars.sh" /etc/profile.d/aafsvars.sh
        source /etc/profile.d/aafsvars.sh

    elif [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then

        q_ensure_temp_work_dir

        if [ -f "$val_TEMP_WORK_DIR/aafsvars.sh" ]; then
            rm "$val_TEMP_WORK_DIR/aafsvars.sh"
        fi

        echo "AAFS_SSL_DIR=\"$cval_CERT_DIR\"" > "$val_TEMP_WORK_DIR/aafsvars.sh"
        echo 'export AAFS_SSL_DIR' >> "$val_TEMP_WORK_DIR/aafsvars.sh"

        if [ -f /etc/profile.d/aafsvars.sh ]; then
            sudo rm -f /etc/profile.d/aafsvars.sh
        fi

        sudo cp "$val_TEMP_WORK_DIR/aafsvars.sh" /etc/profile.d/aafsvars.sh
        source /etc/profile.d/aafsvars.sh

    else
        if [ -f /etc/profile.d/aafsvars.sh ]; then
            sudo rm -f /etc/profile.d/aafsvars.sh
        fi
    fi

}




# Install MongoDB, version 4.4.

q_install_mongo_44 () {
    echo "Installing MongoDB 4.4..."

    # Check if MongoDB is already installed

    if [ -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is already installed"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-4.4.repo" /etc/yum.repos.d/mongodb-org-4.4.repo

        # Download and install

        sudo yum install -y mongodb-org

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        wget -qO - https://www.mongodb.org/static/pgp/server-4.4.asc | sudo apt-key add -

        # Create a list file

        echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/4.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.4.list

        # Reload the local package database

        sudo apt-get update

        # Download and install MongoDB

        sudo apt-get -y install mongodb-org

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-4.4.repo" /etc/yum.repos.d/mongodb-org-4.4.repo

        # Download and install

        sudo yum install -y mongodb-org

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    fi

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB failed to install"
        exit 1
    fi

    # If MongoDB is running now, stop it

    echo "Pausing 20 seconds..."
    sleep 20

    if q_is_mongo_running ; then
        q_do_stop_mongo

        echo "Pausing 10 seconds..."
        sleep 10
    fi

    # Change ownership of the MongoDB data and log directories

    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongodata
    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongolog

    # If SELinux needs to be configured...

    if [ "$my_IS_SELINUX" == "$val_YES" ]; then

        # Install checkmodule if needed

        if [ ! -f /usr/bin/checkmodule ]; then
            sudo yum install -y checkpolicy
        fi

        if [ ! -f /usr/bin/checkmodule ]; then
            echo "Failed to install checkmodule"
            exit 1
        fi

        # Install semodule_package if needed

        if [ ! -f /usr/bin/semodule_package ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/bin/semodule_package ]; then
            echo "Failed to install semodule_package"
            exit 1
        fi

        # Install semanage if needed

        if [ ! -f /usr/sbin/semanage ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/sbin/semanage ]; then
            echo "Failed to install semanage"
            exit 1
        fi

        # Enable access to system resources

        q_ensure_temp_work_dir
        cd "$val_TEMP_WORK_DIR"

        cp "$my_OS_SPECIFIC_PATH/mongodb_cgroup_memory.te" .
        checkmodule -M -m -o mongodb_cgroup_memory.mod mongodb_cgroup_memory.te
        semodule_package -o mongodb_cgroup_memory.pp -m mongodb_cgroup_memory.mod
        sudo semodule -i mongodb_cgroup_memory.pp

        # Enable access to the data directory

        sudo semanage fcontext -a -t mongod_var_lib_t '/data/aafs/mongodata.*'
        sudo chcon -Rv -u system_u -t mongod_var_lib_t '/data/aafs/mongodata'
        sudo restorecon -R -v '/data/aafs/mongodata'

        # Enable access to the log directory

        sudo semanage fcontext -a -t mongod_log_t '/data/aafs/mongolog.*'
        sudo chcon -Rv -u system_u -t mongod_log_t '/data/aafs/mongolog'
        sudo restorecon -R -v '/data/aafs/mongolog'

        cd - >/dev/null

    fi

}




# Install MongoDB, version 5.0.

q_install_mongo_50 () {
    echo "Installing MongoDB 5.0..."

    # Check if MongoDB is already installed

    if [ -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is already installed"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        if [ "$my_ARM_ARCH" == "$val_YES" ]; then
            sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-5.0.arm.repo" /etc/yum.repos.d/mongodb-org-5.0.repo
        else
            sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-5.0.repo" /etc/yum.repos.d/mongodb-org-5.0.repo
        fi

        # For ARM, download and install MongoDB database tools

        if [ "$my_ARM_ARCH" == "$val_YES" ]; then
            sudo cp "$my_OS_SPECIFIC_PATH/mongodb-database-tools-4.4.arm.repo" /etc/yum.repos.d/mongodb-database-tools-4.4.repo
            sudo yum install -y mongodb-database-tools
        fi

        # Download and install

        sudo yum install -y mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        sudo yum install -y mongodb-mongosh

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        wget -qO - https://www.mongodb.org/static/pgp/server-5.0.asc | sudo apt-key add -

        # Create a list file

        echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/5.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-5.0.list

        # Reload the local package database

        sudo apt-get update

        # Download and install MongoDB

        sudo apt-get -y install mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        sudo apt-get -y install mongodb-mongosh

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-5.0.repo" /etc/yum.repos.d/mongodb-org-5.0.repo

        # Download and install

        sudo yum install -y mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        sudo yum install -y mongodb-mongosh

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    fi

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB failed to install"
        exit 1
    fi

    # If MongoDB is running now, stop it

    echo "Pausing 20 seconds..."
    sleep 20

    if q_is_mongo_running ; then
        q_do_stop_mongo

        echo "Pausing 10 seconds..."
        sleep 10
    fi

    # Change ownership of the MongoDB data and log directories

    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongodata
    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongolog

    # If SELinux needs to be configured...

    if [ "$my_IS_SELINUX" == "$val_YES" ]; then

        # Install checkmodule if needed

        if [ ! -f /usr/bin/checkmodule ]; then
            sudo yum install -y checkpolicy
        fi

        if [ ! -f /usr/bin/checkmodule ]; then
            echo "Failed to install checkmodule"
            exit 1
        fi

        # Install semodule_package if needed

        if [ ! -f /usr/bin/semodule_package ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/bin/semodule_package ]; then
            echo "Failed to install semodule_package"
            exit 1
        fi

        # Install semanage if needed

        if [ ! -f /usr/sbin/semanage ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/sbin/semanage ]; then
            echo "Failed to install semanage"
            exit 1
        fi

        # Enable access to system resources

        q_ensure_temp_work_dir
        cd "$val_TEMP_WORK_DIR"

        cp "$my_OS_SPECIFIC_PATH/mongodb_cgroup_memory.te" .
        checkmodule -M -m -o mongodb_cgroup_memory.mod mongodb_cgroup_memory.te
        semodule_package -o mongodb_cgroup_memory.pp -m mongodb_cgroup_memory.mod
        sudo semodule -i mongodb_cgroup_memory.pp

        cp "$my_OS_SPECIFIC_PATH/mongodb_proc_net.te" .
        checkmodule -M -m -o mongodb_proc_net.mod mongodb_proc_net.te
        semodule_package -o mongodb_proc_net.pp -m mongodb_proc_net.mod
        sudo semodule -i mongodb_proc_net.pp

        # Enable access to the data directory

        sudo semanage fcontext -a -t mongod_var_lib_t '/data/aafs/mongodata.*'
        sudo chcon -Rv -u system_u -t mongod_var_lib_t '/data/aafs/mongodata'
        sudo restorecon -R -v '/data/aafs/mongodata'

        # Enable access to the log directory

        sudo semanage fcontext -a -t mongod_log_t '/data/aafs/mongolog.*'
        sudo chcon -Rv -u system_u -t mongod_log_t '/data/aafs/mongolog'
        sudo restorecon -R -v '/data/aafs/mongolog'

        cd - >/dev/null

    fi

}




# Install MongoDB, version 6.0.

q_install_mongo_60 () {
    echo "Installing MongoDB 6.0..."

    # Check if MongoDB is already installed

    if [ -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is already installed"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        if [ "$my_ARM_ARCH" == "$val_YES" ]; then
            sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-6.0.arm.repo" /etc/yum.repos.d/mongodb-org-6.0.repo
        else
            sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-6.0.repo" /etc/yum.repos.d/mongodb-org-6.0.repo
        fi

        # For ARM, download and install MongoDB database tools

        ##if [ "$my_ARM_ARCH" == "$val_YES" ]; then
        ##    sudo cp "$my_OS_SPECIFIC_PATH/mongodb-database-tools-4.4.arm.repo" /etc/yum.repos.d/mongodb-database-tools-4.4.repo
        ##    sudo yum install -y mongodb-database-tools
        ##fi

        # Download and install

        sudo yum install -y mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        sudo yum install -y mongodb-mongosh

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        curl -fsSL https://pgp.mongodb.com/server-6.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-6.0.gpg --dearmor

        # Create a list file

        if [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2004" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-6.0.gpg ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
        elif [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2204" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-6.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
        else
            echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
            exit 1
        fi

        # Reload the local package database

        sudo apt-get update

        # Download and install MongoDB

        sudo apt-get -y install mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        sudo apt-get -y install mongodb-mongosh

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-6.0.repo" /etc/yum.repos.d/mongodb-org-6.0.repo

        # Download and install

        sudo yum install -y mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        sudo yum install -y mongodb-mongosh

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    fi

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB failed to install"
        exit 1
    fi

    # If MongoDB is running now, stop it

    echo "Pausing 20 seconds..."
    sleep 20

    if q_is_mongo_running ; then
        q_do_stop_mongo

        echo "Pausing 10 seconds..."
        sleep 10
    fi

    # Change ownership of the MongoDB data and log directories

    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongodata
    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongolog

    # If SELinux needs to be configured...

    if [ "$my_IS_SELINUX" == "$val_YES" ]; then

        # Install checkmodule if needed

        if [ ! -f /usr/bin/checkmodule ]; then
            sudo yum install -y checkpolicy
        fi

        if [ ! -f /usr/bin/checkmodule ]; then
            echo "Failed to install checkmodule"
            exit 1
        fi

        # Install semodule_package if needed

        if [ ! -f /usr/bin/semodule_package ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/bin/semodule_package ]; then
            echo "Failed to install semodule_package"
            exit 1
        fi

        # Install semanage if needed

        if [ ! -f /usr/sbin/semanage ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/sbin/semanage ]; then
            echo "Failed to install semanage"
            exit 1
        fi

        # Enable access to system resources

        q_ensure_temp_work_dir
        cd "$val_TEMP_WORK_DIR"

        cp "$my_OS_SPECIFIC_PATH/mongodb_cgroup_memory.te" .
        checkmodule -M -m -o mongodb_cgroup_memory.mod mongodb_cgroup_memory.te
        semodule_package -o mongodb_cgroup_memory.pp -m mongodb_cgroup_memory.mod
        sudo semodule -i mongodb_cgroup_memory.pp

        cp "$my_OS_SPECIFIC_PATH/mongodb_proc_net.te" .
        checkmodule -M -m -o mongodb_proc_net.mod mongodb_proc_net.te
        semodule_package -o mongodb_proc_net.pp -m mongodb_proc_net.mod
        sudo semodule -i mongodb_proc_net.pp

        # Enable access to the data directory

        sudo semanage fcontext -a -t mongod_var_lib_t '/data/aafs/mongodata.*'
        sudo chcon -Rv -u system_u -t mongod_var_lib_t '/data/aafs/mongodata'
        sudo restorecon -R -v '/data/aafs/mongodata'

        # Enable access to the log directory

        sudo semanage fcontext -a -t mongod_log_t '/data/aafs/mongolog.*'
        sudo chcon -Rv -u system_u -t mongod_log_t '/data/aafs/mongolog'
        sudo restorecon -R -v '/data/aafs/mongolog'

        cd - >/dev/null

    fi

}




# Install MongoDB, version 7.0.

q_install_mongo_70 () {
    echo "Installing MongoDB 7.0..."

    # Check if MongoDB is already installed

    if [ -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is already installed"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        if [ "$my_ARM_ARCH" == "$val_YES" ]; then
            if [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.arm.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            elif [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2023" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.2023.arm.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            else
                echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
                exit 1
            fi
        else
            if [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            elif [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2023" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.2023.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            else
                echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
                exit 1
            fi
        fi

        # For ARM, download and install MongoDB database tools

        ##if [ "$my_ARM_ARCH" == "$val_YES" ]; then
        ##    sudo cp "$my_OS_SPECIFIC_PATH/mongodb-database-tools-4.4.arm.repo" /etc/yum.repos.d/mongodb-database-tools-4.4.repo
        ##    sudo yum install -y mongodb-database-tools
        ##fi

        # Amazon Linux 2023 requires a special version of mongosh

        if [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2023" ]; then
            sudo yum install -y mongodb-mongosh-shared-openssl3
        fi

        # Download and install

        sudo yum install -y mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        ##sudo yum install -y mongodb-mongosh

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        curl -fsSL https://pgp.mongodb.com/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

        # Create a list file

        if [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2004" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
        elif [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2204" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
        else
            echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
            exit 1
        fi

        # Reload the local package database

        sudo apt-get update

        # Download and install MongoDB

        sudo apt-get -y install mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        ##sudo apt-get -y install mongodb-mongosh

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.repo" /etc/yum.repos.d/mongodb-org-7.0.repo

        # Download and install

        sudo yum install -y mongodb-org

        # Download and install the MongoDB shell (might already be installed)

        ##sudo yum install -y mongodb-mongosh

        # Enable MongoDB startup and shutdown without a password

        q_ensure_temp_work_dir
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod start" > "$val_TEMP_WORK_DIR/aftershock_sudo"
        echo "%${val_LOCAL_ACCOUNT} ALL=NOPASSWD: /usr/sbin/service mongod stop" >> "$val_TEMP_WORK_DIR/aftershock_sudo"
        sudo cp -f "$val_TEMP_WORK_DIR/aftershock_sudo" /etc/sudoers.d
        sudo chmod 440 /etc/sudoers.d/aftershock_sudo

    fi

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB failed to install"
        exit 1
    fi

    # If MongoDB is running now, stop it

    echo "Pausing 20 seconds..."
    sleep 20

    if q_is_mongo_running ; then
        q_do_stop_mongo

        echo "Pausing 10 seconds..."
        sleep 10
    fi

    # Change ownership of the MongoDB data and log directories

    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongodata
    sudo chown -R "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" /data/aafs/mongolog

    # If SELinux needs to be configured...

    if [ "$my_IS_SELINUX" == "$val_YES" ]; then

        # Install checkmodule if needed

        if [ ! -f /usr/bin/checkmodule ]; then
            sudo yum install -y checkpolicy
        fi

        if [ ! -f /usr/bin/checkmodule ]; then
            echo "Failed to install checkmodule"
            exit 1
        fi

        # Install semodule_package if needed

        if [ ! -f /usr/bin/semodule_package ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/bin/semodule_package ]; then
            echo "Failed to install semodule_package"
            exit 1
        fi

        # Install semanage if needed

        if [ ! -f /usr/sbin/semanage ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/sbin/semanage ]; then
            echo "Failed to install semanage"
            exit 1
        fi

        # Enable access to system resources

        q_ensure_temp_work_dir
        cd "$val_TEMP_WORK_DIR"

        cp "$my_OS_SPECIFIC_PATH/mongodb_cgroup_memory.te" .
        checkmodule -M -m -o mongodb_cgroup_memory.mod mongodb_cgroup_memory.te
        semodule_package -o mongodb_cgroup_memory.pp -m mongodb_cgroup_memory.mod
        sudo semodule -i mongodb_cgroup_memory.pp

        cp "$my_OS_SPECIFIC_PATH/mongodb_proc_net.te" .
        checkmodule -M -m -o mongodb_proc_net.mod mongodb_proc_net.te
        semodule_package -o mongodb_proc_net.pp -m mongodb_proc_net.mod
        sudo semodule -i mongodb_proc_net.pp

        # Enable access to the data directory

        sudo semanage fcontext -a -t mongod_var_lib_t '/data/aafs/mongodata.*'
        sudo chcon -Rv -u system_u -t mongod_var_lib_t '/data/aafs/mongodata'
        sudo restorecon -R -v '/data/aafs/mongodata'

        # Enable access to the log directory

        sudo semanage fcontext -a -t mongod_log_t '/data/aafs/mongolog.*'
        sudo chcon -Rv -u system_u -t mongod_log_t '/data/aafs/mongolog'
        sudo restorecon -R -v '/data/aafs/mongolog'

        cd - >/dev/null

    fi

}




# Install MongoDB, default version.

q_install_mongo () {
    echo "Installing MongoDB (default)..."

    # Default is 7.0 for now

    q_install_mongo_70

}




# Upgrade MongoDB, version 4.2 to 4.4.
# This updates the package registry and selinux, but does not do the actual installation.

q_upgrade_mongo_42_to_44 () {
    echo "Upgrading MongoDB 4.2 to 4.4..."

    # Check if MongoDB is installed and not running

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is not installed"
        exit 1
    fi

    if q_is_mongo_running ; then
        echo "MongoDB is running, you need to stop it before you can upgrade"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-4.4.repo" /etc/yum.repos.d/mongodb-org-4.4.repo

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        wget -qO - https://www.mongodb.org/static/pgp/server-4.4.asc | sudo apt-key add -

        # Create a list file

        echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/4.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.4.list

        # Reload the local package database

        sudo apt-get update

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-4.4.repo" /etc/yum.repos.d/mongodb-org-4.4.repo

    fi

}




# Upgrade MongoDB, version 4.4 to 5.0.
# This updates the package registry and selinux, but does not do the actual installation.

q_upgrade_mongo_44_to_50 () {
    echo "Upgrading MongoDB 4.4 to 5.0..."

    # Check if MongoDB is installed and not running

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is not installed"
        exit 1
    fi

    if q_is_mongo_running ; then
        echo "MongoDB is running, you need to stop it before you can upgrade"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-5.0.repo" /etc/yum.repos.d/mongodb-org-5.0.repo

        # Download and install the MongoDB shell

        sudo yum install -y mongodb-mongosh

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        wget -qO - https://www.mongodb.org/static/pgp/server-5.0.asc | sudo apt-key add -

        # Create a list file

        echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/5.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-5.0.list

        # Reload the local package database

        sudo apt-get update

        # Download and install the MongoDB shell

        sudo apt-get -y install mongodb-mongosh

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-5.0.repo" /etc/yum.repos.d/mongodb-org-5.0.repo

        # Download and install the MongoDB shell

        sudo yum install -y mongodb-mongosh

    fi

    # If SELinux needs to be configured...

    if [ "$my_IS_SELINUX" == "$val_YES" ]; then

        # Install checkmodule if needed

        if [ ! -f /usr/bin/checkmodule ]; then
            sudo yum install -y checkpolicy
        fi

        if [ ! -f /usr/bin/checkmodule ]; then
            echo "Failed to install checkmodule"
            exit 1
        fi

        # Install semodule_package if needed

        if [ ! -f /usr/bin/semodule_package ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/bin/semodule_package ]; then
            echo "Failed to install semodule_package"
            exit 1
        fi

        # Install semanage if needed

        if [ ! -f /usr/sbin/semanage ]; then
            sudo yum install -y policycoreutils-python
        fi

        if [ ! -f /usr/sbin/semanage ]; then
            echo "Failed to install semanage"
            exit 1
        fi

        # Enable access to system resources, that were not needed in 4.4

        q_ensure_temp_work_dir
        cd "$val_TEMP_WORK_DIR"

        cp "$my_OS_SPECIFIC_PATH/mongodb_proc_net.te" .
        checkmodule -M -m -o mongodb_proc_net.mod mongodb_proc_net.te
        semodule_package -o mongodb_proc_net.pp -m mongodb_proc_net.mod
        sudo semodule -i mongodb_proc_net.pp

        cd - >/dev/null

    fi

}




# Upgrade MongoDB, version 5.0 to 6.0.
# This updates the package registry and selinux, but does not do the actual installation.

q_upgrade_mongo_50_to_60 () {
    echo "Upgrading MongoDB 5.0 to 6.0..."

    # Check if MongoDB is installed and not running

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is not installed"
        exit 1
    fi

    if q_is_mongo_running ; then
        echo "MongoDB is running, you need to stop it before you can upgrade"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-6.0.repo" /etc/yum.repos.d/mongodb-org-6.0.repo

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        curl -fsSL https://pgp.mongodb.com/server-6.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-6.0.gpg --dearmor

        # Create a list file

        if [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2004" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-6.0.gpg ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
        elif [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2204" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-6.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
        else
            echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
            exit 1
        fi

        # Reload the local package database

        sudo apt-get update

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-6.0.repo" /etc/yum.repos.d/mongodb-org-6.0.repo

    fi

}




# Upgrade MongoDB, version 6.0 to 7.0.
# This updates the package registry and selinux, but does not do the actual installation.

q_upgrade_mongo_60_to_70 () {
    echo "Upgrading MongoDB 6.0 to 7.0..."

    # Check if MongoDB is installed and not running

    if [ ! -f "$my_MONGO_CONF_FILE" ]; then
        echo "MongoDB is not installed"
        exit 1
    fi

    if q_is_mongo_running ; then
        echo "MongoDB is running, you need to stop it before you can upgrade"
        exit 1
    fi

    # Operating-system specific installation

    if [ "$my_OS_TYPE" == "$val_OSTYPE_AMAZON" ]; then

        # Register MongoDB with yum
        # Note that 6.0 should not have been installed on AL2023, and the upgrade may not work on AL2023 due to mongosh

        if [ "$my_ARM_ARCH" == "$val_YES" ]; then
            if [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.arm.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            elif [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2023" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.2023.arm.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            else
                echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
                exit 1
            fi
        else
            if [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            elif [ "$my_OS_GEN" == "$val_OSGEN_AMAZON_2023" ]; then
                sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.2023.repo" /etc/yum.repos.d/mongodb-org-7.0.repo
            else
                echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
                exit 1
            fi
        fi

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_UBUNTU" ]; then

        # Import the public key

        curl -fsSL https://pgp.mongodb.com/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

        # Create a list file

        if [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2004" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
        elif [ "$my_OS_GEN" == "$val_OSGEN_UBUNTU_2204" ]; then
            echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
        else
            echo "Invalid operating system generation: my_OS_GEN = $my_OS_GEN" 
            exit 1
        fi

        # Reload the local package database

        sudo apt-get update

    elif [ "$my_OS_TYPE" == "$val_OSTYPE_CENTOS" ]; then

        # Register MongoDB with yum

        sudo cp "$my_OS_SPECIFIC_PATH/mongodb-org-7.0.repo" /etc/yum.repos.d/mongodb-org-7.0.repo

    fi

}




# Configure MongoDB by editing the configuration file.
# This can also be used to change the MongoDB configuration.
# This would work with MongoDB 4.2.
# MongoDB 4.4 requires a replica set keyfile.

q_configure_mongo_42 () {
    echo "Configuring MongoDB..."

    # Save the original MongoDB configuration file, if not previously saved

    if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
        sudo cp -pi "$my_MONGO_CONF_FILE" "$my_MONGO_CONF_BACKUP"
    fi

    # Make file edits, starting with the original file

    q_ensure_temp_work_dir

    cat "$my_MONGO_CONF_BACKUP"    \
    | sed "s|$my_MONGO_DEF_DATA_PATH|/data/aafs/mongodata|"    \
    | sed "s|$my_MONGO_DEF_LOG_PATH|/data/aafs/mongolog|"    \
    | sed "s|127.0.0.1|$my_MONGO_BIND_IP_LIST|"    \
    | sed "s|#replication:|replication:|"    \
    | sed "s|replication:|replication:"'\'$'\n'"  replSetName: "'"'"$my_MONGO_LOCAL_REPSET"'"'"|"    \
    | sed "s|#security:|security:|"    \
    | sed "s|security:|security:"'\'$'\n'"  authorization: enabled|"    \
    > "$val_TEMP_WORK_DIR/mongod_temp.conf"

    echo "" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
    echo "setParameter:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
    echo "  transactionLifetimeLimitSeconds: 1200" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"

    sudo cp "$val_TEMP_WORK_DIR/mongod_temp.conf" "$my_MONGO_CONF_FILE"

}




# Configure MongoDB by editing the configuration file.
# This can also be used to change the MongoDB configuration.

q_configure_mongo () {
    echo "Configuring MongoDB..."

    # Save the original MongoDB configuration file, if not previously saved

    if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
        sudo cp -pi "$my_MONGO_CONF_FILE" "$my_MONGO_CONF_BACKUP"
    fi

    # Make a replica set keyfile

    q_ensure_temp_work_dir

    if [ -f "$my_MONGO_KEY_FILE" ]; then
        sudo rm -f "$my_MONGO_KEY_FILE"
    fi

    echo "$my_MONGO_KEY_CONTENT" > "$val_TEMP_WORK_DIR/mongod_key_temp.yaml"

    sudo cp "$val_TEMP_WORK_DIR/mongod_key_temp.yaml" "$my_MONGO_KEY_FILE"
    sudo chmod 400 "$my_MONGO_KEY_FILE"
    sudo chown "${my_MONGO_LOCAL_ACCOUNT}:${my_MONGO_LOCAL_ACCOUNT}" "$my_MONGO_KEY_FILE"

    # Make file edits, starting with the original file

    cat "$my_MONGO_CONF_BACKUP"    \
    | sed "s|$my_MONGO_DEF_DATA_PATH|/data/aafs/mongodata|"    \
    | sed "s|$my_MONGO_DEF_LOG_PATH|/data/aafs/mongolog|"    \
    | sed "s|127.0.0.1|$my_MONGO_BIND_IP_LIST|"    \
    | sed "s|#replication:|replication:|"    \
    | sed "s|replication:|replication:"'\'$'\n'"  replSetName: "'"'"$my_MONGO_LOCAL_REPSET"'"'"|"    \
    | sed "s|#security:|security:|"    \
    | sed "s|security:|security:"'\'$'\n'"  authorization: enabled"'\'$'\n'"  keyFile: $my_MONGO_KEY_FILE|"    \
    > "$val_TEMP_WORK_DIR/mongod_temp.conf"

    if [ "$my_HAS_SERVER_CERT" == "$val_YES" ]; then
        echo "" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "net:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "  tls:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "    mode: requireTLS" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "    certificateKeyFile: ${cval_CERT_DIR}/${cval_file_server}.pem" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "    certificateKeyFilePassword: $(cat "${cval_CERT_DIR}/${cval_file_server}${cval_suffix_pass}")" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
        echo "    CAFile: ${cval_CERT_DIR}/${cval_file_root_ca}.pem" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        fi
    fi

    if [ "$MONGO_CACHE_GB" -gt 0 ]; then
        echo "" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "storage:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "  wiredTiger:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "    engineConfig:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
        echo "      cacheSizeGB: $MONGO_CACHE_GB" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
    fi

    echo "" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
    echo "setParameter:" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"
    echo "  transactionLifetimeLimitSeconds: 1200" >> "$val_TEMP_WORK_DIR/mongod_temp.conf"

    sudo cp "$val_TEMP_WORK_DIR/mongod_temp.conf" "$my_MONGO_CONF_FILE"

}




# Set up MongoDB by creating the replica set and user accounts, for version 4.X.

q_setup_mongo_4x () {
    echo "Setting up MongoDB..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Initialize the replica set

    echo "Initializing MongoDB replica set..."

    echo "rs.initiate()" > "$val_TEMP_WORK_DIR/mongo_setup_1"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_setup_1"

    mongo < "$val_TEMP_WORK_DIR/mongo_setup_1"

    echo "Pausing 30 seconds..."
    sleep 30

    # Create the administrative user

    echo "Creating MongoDB administrative user..."

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_setup_2"
    echo "db.createUser({user:"'"'"$MONGO_ADMIN_USER"'"'", pwd:"'"'"$MONGO_ADMIN_PASS"'"'", roles:[{role:"'"'"userAdminAnyDatabase"'"'", db:"'"'"admin"'"'"},{role:"'"'"clusterAdmin"'"'", db:"'"'"admin"'"'"}]})" >> "$val_TEMP_WORK_DIR/mongo_setup_2"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_setup_2"

    mongo < "$val_TEMP_WORK_DIR/mongo_setup_2"

    # Create the database user

    echo "Creating MongoDB database user..."

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "use $MONGO_NAME" >> "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "db.createUser({user:"'"'"$MONGO_USER"'"'", pwd:"'"'"$MONGO_PASS"'"'", roles:[{role:"'"'"dbOwner"'"'", db:"'"'"$MONGO_NAME"'"'"}]})" >> "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_setup_3"

    mongo < "$val_TEMP_WORK_DIR/mongo_setup_3"

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Run the mongo shell, passing in required certificates.
# This function produces no output of its own.

q_run_mongosh () {

    if [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            mongosh --tls --tlsCAFile "${cval_CERT_DIR}/${cval_file_root_ca}.pem" --tlsCertificateKeyFile "${cval_CERT_DIR}/${cval_file_client}.pem" --tlsCertificateKeyFilePassword "$(cat "${cval_CERT_DIR}/${cval_file_client}${cval_suffix_pass}")"
        else
            mongosh --tls --tlsCAFile "${cval_CERT_DIR}/${cval_file_root_ca}.pem"
        fi
    else
        mongosh
    fi

}




# Run the mongo shell to the given host, passing in required certificates.
# $1 = Host address.
# This function produces no output of its own.

q_run_mongosh_to () {

    if [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            mongosh --host "$1" --tls --tlsCAFile "${cval_CERT_DIR}/${cval_file_root_ca}.pem" --tlsCertificateKeyFile "${cval_CERT_DIR}/${cval_file_client}.pem" --tlsCertificateKeyFilePassword $(cat "${cval_CERT_DIR}/${cval_file_client}${cval_suffix_pass}")
        else
            mongosh --host "$1" --tls --tlsCAFile "${cval_CERT_DIR}/${cval_file_root_ca}.pem"
        fi
    else
        mongosh --host "$1"
    fi

}




# Set up MongoDB by creating the replica set and user accounts, for version 5.0, 6.0, and 7.0.
# (Difference is using mongosh instead of mongo.)

q_setup_mongo () {
    echo "Setting up MongoDB..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Initialize the replica set

    echo "Initializing MongoDB replica set..."

    echo "rs.initiate()" > "$val_TEMP_WORK_DIR/mongo_setup_1"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_setup_1"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_setup_1"

    echo "Pausing 30 seconds..."
    sleep 30

    # Create the administrative user

    echo "Creating MongoDB administrative user..."

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_setup_2"
    echo "db.createUser({user:"'"'"$MONGO_ADMIN_USER"'"'", pwd:"'"'"$MONGO_ADMIN_PASS"'"'", roles:[{role:"'"'"userAdminAnyDatabase"'"'", db:"'"'"admin"'"'"},{role:"'"'"clusterAdmin"'"'", db:"'"'"admin"'"'"}]})" >> "$val_TEMP_WORK_DIR/mongo_setup_2"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_setup_2"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_setup_2"

    # Create the database user

    echo "Creating MongoDB database user..."

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "use $MONGO_NAME" >> "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "db.createUser({user:"'"'"$MONGO_USER"'"'", pwd:"'"'"$MONGO_PASS"'"'", roles:[{role:"'"'"dbOwner"'"'", db:"'"'"$MONGO_NAME"'"'"}]})" >> "$val_TEMP_WORK_DIR/mongo_setup_3"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_setup_3"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_setup_3"

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Set MongoDB feature compatibility version to 4.4.

q_set_mongo_compatibility_to_44 () {
    echo "Setting MongoDB feature compatibility version to 4.4..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Set the feature compatibility version

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_fcv_44"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_fcv_44"
    echo "db.adminCommand( { setFeatureCompatibilityVersion: "'"'"4.4"'"'" } )" >> "$val_TEMP_WORK_DIR/mongo_fcv_44"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_fcv_44"

    mongo < "$val_TEMP_WORK_DIR/mongo_fcv_44"

    echo "Pausing 30 seconds..."
    sleep 30

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Set MongoDB feature compatibility version to 5.0.

q_set_mongo_compatibility_to_50 () {
    echo "Setting MongoDB feature compatibility version to 5.0..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Set the feature compatibility version

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_fcv_50"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_fcv_50"
    echo "db.adminCommand( { setFeatureCompatibilityVersion: "'"'"5.0"'"'" } )" >> "$val_TEMP_WORK_DIR/mongo_fcv_50"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_fcv_50"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_fcv_50"

    echo "Pausing 30 seconds..."
    sleep 30

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Set MongoDB feature compatibility version to 6.0.

q_set_mongo_compatibility_to_60 () {
    echo "Setting MongoDB feature compatibility version to 6.0..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Set the feature compatibility version

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_fcv_60"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_fcv_60"
    echo "db.adminCommand( { setFeatureCompatibilityVersion: "'"'"6.0"'"'" } )" >> "$val_TEMP_WORK_DIR/mongo_fcv_60"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_fcv_60"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_fcv_60"

    echo "Pausing 30 seconds..."
    sleep 30

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Set MongoDB feature compatibility version to 7.0.

q_set_mongo_compatibility_to_70 () {
    echo "Setting MongoDB feature compatibility version to 7.0..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Set the feature compatibility version

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_fcv_70"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_fcv_70"
    echo "db.adminCommand( { setFeatureCompatibilityVersion: "'"'"7.0"'"'" } )" >> "$val_TEMP_WORK_DIR/mongo_fcv_70"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_fcv_70"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_fcv_70"

    echo "Pausing 30 seconds..."
    sleep 30

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Get MongoDB feature compatibility version, for 5.0 and later.

q_get_mongo_compatibility () {
    echo "Getting MongoDB feature compatibility version..."

    # Start MongoDB

    q_do_start_mongo

    echo "Pausing 15 seconds..."
    sleep 15

    if q_is_mongo_running ; then
        :
    else
        echo "MongoDB failed to start"
        exit 1
    fi

    # Get the feature compatibility version

    echo "use admin" > "$val_TEMP_WORK_DIR/mongo_fcv_get"
    echo "db.auth("'"'"$MONGO_ADMIN_USER"'"'", "'"'"$MONGO_ADMIN_PASS"'"'")" >> "$val_TEMP_WORK_DIR/mongo_fcv_get"
    echo "db.adminCommand( { getParameter: 1, featureCompatibilityVersion: 1 } )" >> "$val_TEMP_WORK_DIR/mongo_fcv_get"
    echo "quit()" >> "$val_TEMP_WORK_DIR/mongo_fcv_get"

    q_run_mongosh < "$val_TEMP_WORK_DIR/mongo_fcv_get"

    echo "Pausing 5 seconds..."
    sleep 5

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Configure OAF by creating its configuration files.
# This can also be used to update the OAF configuration.

q_configure_oaf () {
    echo "Configuring OAF..."

    # Determine SSL options based on installed certificates

    if [ "$my_HAS_SERVER_CERT" == "$val_YES" ]; then
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true\&sslClientAuth=true"
        else
            my_SSL_OPTIONS="sslEnable=true"
        fi
    else
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true\&sslClientAuth=true"
        elif [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true"
        else
            my_SSL_OPTIONS="sslEnable=false"
        fi
    fi

    # Create directories if needed, and copy configuration files and scripts into position

    ./boaf.sh updatecfg

    # Configure the server

    ./boaf.sh erase_config_server

    # Check the server option

    if [ "$SERVER_OPTION" == "$val_SERVER_PRIMARY" ]; then

        # Primary server in a dual-server configuration

        ./boaf.sh config_server_1 "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_2" "$MONGO_REP_SET_2" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$SERVER_NAME_1" "$PDL_OPTION" "$my_SSL_OPTIONS"

    elif [ "$SERVER_OPTION" == "$val_SERVER_SECONDARY" ]; then

        # Secondary server in a dual-server configuration

        ./boaf.sh config_server_2 "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_2" "$MONGO_REP_SET_2" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$SERVER_NAME_2" "$PDL_OPTION" "$my_SSL_OPTIONS"

    elif [ "$SERVER_OPTION" == "$val_SERVER_SOLO" ]; then

        # Single-server configuration

        ./boaf.sh config_server_solo "$SERVER_IP_1" "$MONGO_REP_SET_1" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$SERVER_NAME_1" "$PDL_OPTION" "$my_SSL_OPTIONS"

    elif [ "$SERVER_OPTION" == "$val_SERVER_DEV" ]; then

        # Single-server development configuration

        ./boaf.sh config_server_dev

    else
        echo "Invalid server option: SERVER_OPTION = $SERVER_OPTION"
        exit 1
    fi

    # Configure the action

    ./boaf.sh erase_config_action

    # Check the action option

    if [ "$ACTION_OPTION" == "$val_ACTION_USA" ]; then

        # USA action configuration

        ./boaf.sh config_action_usa

    elif [ "$ACTION_OPTION" == "$val_ACTION_DEV" ]; then

        # Development action configuration

        ./boaf.sh config_action_dev

    else
        echo "Invalid action option: ACTION_OPTION = $ACTION_OPTION"
        exit 1
    fi

    # Write the launch options script

    if [ -f "$val_LAUNCH_OPTIONS_FILE" ]; then
        rm "$val_LAUNCH_OPTIONS_FILE"
    fi

    echo "MOAF_JAVA_MAX_MEMORY_GB=\"$JAVA_MAX_MEMORY_GB\"" > "$val_LAUNCH_OPTIONS_FILE"

}




# Compile and deploy OAF.
# This can also be used to compile and deploy a new version.

q_compile_oaf () {
    echo "Compiling OAF..."

    # Compile and package the OAF software

    ./boaf.sh clean
    ./boaf.sh compile

    if [ ! -f opensha-oaf/build/libs/opensha-oaf-oaf.jar ]; then
        echo "OAF software compilation failed"
        exit 1
    fi

    echo "Packaging OAF..."

    ./boaf.sh pack

    # Deploy the OAF software

    echo "Deploying OAF..."

    ./boaf.sh deploy

    # Determine SSL options based on installed certificates

    if [ "$my_HAS_SERVER_CERT" == "$val_YES" ]; then
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true\&sslClientAuth=true"
        else
            my_SSL_OPTIONS="sslEnable=true"
        fi
    else
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true\&sslClientAuth=true"
        elif [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true"
        else
            my_SSL_OPTIONS="sslEnable=false"
        fi
    fi

    # Build the generic GUI

    echo "Building generic GUI..."

    ./boaf.sh compilegui "$GUI_DATE"

    # Build the production GUI

    echo "Building production GUI..."

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        ./boaf.sh config_packgui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_2" "$MONGO_REP_SET_2" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    else
        ./boaf.sh config_packgui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    fi

    # Build the generic ETAS GUI

    echo "Building generic ETAS GUI..."

    ./boaf.sh compile_etas_gui "$GUI_DATE"

    # Build the production ETAS GUI

    echo "Building production ETAS GUI..."

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        ./boaf.sh config_pack_etas_gui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_2" "$MONGO_REP_SET_2" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    else
        ./boaf.sh config_pack_etas_gui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    fi

}




# Rebuild the GUI.

q_rebuild_gui () {
    echo "Rebuilding GUI..."

    rm opensha-oaf/build/libs/AftershockGUI*
    rm opensha-oaf/build/libs/AftershockETAS_GUI*

    # Determine SSL options based on installed certificates

    if [ "$my_HAS_SERVER_CERT" == "$val_YES" ]; then
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true\&sslClientAuth=true"
        else
            my_SSL_OPTIONS="sslEnable=true"
        fi
    else
        if [ "$my_HAS_CLIENT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true\&sslClientAuth=true"
        elif [ "$my_HAS_ROOT_CERT" == "$val_YES" ]; then
            my_SSL_OPTIONS="sslEnable=true"
        else
            my_SSL_OPTIONS="sslEnable=false"
        fi
    fi

    # Build the generic GUI

    echo "Building generic RJ GUI..."

    ./boaf.sh compilegui "$GUI_DATE"

    # Build the production GUI

    echo "Building production RJ GUI..."

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        ./boaf.sh config_packgui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_2" "$MONGO_REP_SET_2" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    else
        ./boaf.sh config_packgui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    fi

    # Build the generic ETAS GUI

    echo "Building generic ETAS GUI..."

    ./boaf.sh compile_etas_gui "$GUI_DATE"

    # Build the production ETAS GUI

    echo "Building production ETAS GUI..."

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        ./boaf.sh config_pack_etas_gui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_2" "$MONGO_REP_SET_2" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    else
        ./boaf.sh config_pack_etas_gui "$GUI_DATE" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$SERVER_IP_1" "$MONGO_REP_SET_1" "$MONGO_NAME" "$MONGO_USER" "$MONGO_PASS" "$my_SSL_OPTIONS"
    fi

}




# Validate the relay mode.
# $1 = relay mode.
# $2 = configured primary.
# Exit the script if the relay mode or configured primary are invalid.

q_validate_relay_mode () {

    if [ -z "$1" ]; then
        echo "Relay mode is not specified"
        exit 1
    fi

    if [ -z "$2" ]; then
        echo "Configured primary is not specified"
        exit 1
    fi

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then

        # Dual server

        if [ "$1" == "pair" ]; then
            :
        elif [ "$1" == "watch" ]; then
            :
        else
            echo "Invalid relay mode and configured primary: $1 $2"
            echo "The relay mode can be one of:  pair  watch"
            echo "The configured primary can be one of:  1  2  this  other"
            exit 1
        fi

        if [ "$2" == "1" ]; then
            :
        elif [ "$2" == "2" ]; then
            :
        elif [ "$2" == "this" ]; then
            :
        elif [ "$2" == "other" ]; then
            :
        else
            echo "Invalid relay mode and configured primary: $1 $2"
            echo "The relay mode can be one of:  pair  watch"
            echo "The configured primary can be one of:  1  2  this  other"
            exit 1
        fi

    else

        # Single server

        if [ "$1" == "solo" ]; then
            :
        else
            echo "Invalid relay mode and configured primary: $1 $2"
            echo "The relay mode must be:  solo"
            echo "The configured primary can be one of:  1  this"
            exit 1
        fi

        if [ "$2" == "1" ]; then
            :
        elif [ "$2" == "this" ]; then
            :
        else
            echo "Invalid relay mode and configured primary: $1 $2"
            echo "The relay mode must be:  solo"
            echo "The configured primary can be one of:  1  this"
            exit 1
        fi

    fi

}




# Initialize the OAF database.
# $1 = relay mode.
# $2 = configured primary.
# This should only be run once.

q_init_oaf_database () {
    echo "Initializing OAF database..."

    # Check that OAF is Installed

    if [ ! -f "/opt/aafs/moaf.sh" ]; then
        echo "OAF software is not installed yet"
        exit 1
    fi

    # Check that we have valid relay mode and configured primary

    q_validate_relay_mode "$1" "$2"

    # Start MongoDB

    if q_is_mongo_running ; then

        echo "Pausing 10 seconds..."
        sleep 10

    else

        q_do_start_mongo

        echo "Pausing 15 seconds..."
        sleep 15

        if q_is_mongo_running ; then
            :
        else
            echo "MongoDB failed to start"
            exit 1
        fi

    fi

    # Initialize the database

    cd /opt/aafs
    ./moaf.sh initdb
    cd - >/dev/null

    # Initialize the relay mode

    cd /opt/aafs
    ./moaf.sh init_relay_mode "$1" "$2"
    cd - >/dev/null

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Restore the OAF database.
# $1 = Backup filename, in gzipped format.
# $2 = relay mode.
# $3 = configured primary.
# This should only be run once, after creating or erasing the database.

q_restore_oaf_database () {
    echo "Restoring OAF database..."

    # Check that OAF is Installed

    if [ ! -f "/opt/aafs/moaf.sh" ]; then
        echo "OAF software is not installed yet"
        exit 1
    fi

    # Check that the backup file exists

    if [ -z "$1" ]; then
        echo "Database backup file is not specified"
        exit 1
    fi

    if [ ! -f "$1" ]; then
        echo "Database backup file is not found: $1"
        exit 1
    fi

    # Check that we have valid relay mode and configured primary

    q_validate_relay_mode "$2" "$3"

    # Start MongoDB

    if q_is_mongo_running ; then

        echo "Pausing 10 seconds..."
        sleep 10

    else

        q_do_start_mongo

        echo "Pausing 15 seconds..."
        sleep 15

        if q_is_mongo_running ; then
            :
        else
            echo "MongoDB failed to start"
            exit 1
        fi

    fi

    # Restore the database

    /opt/aafs/moaf.sh restore_database_gzip "$1"

    # Initialize the relay mode

    cd /opt/aafs
    ./moaf.sh init_relay_mode "$2" "$3"
    cd - >/dev/null

    # Stop MongoDB

    q_do_stop_mongo

    echo "Pausing 10 seconds..."
    sleep 10

}




# Stop AAFS in preparation for an update, and optionally back up the database.
# $1 = backup filename, or "nobackup" to skip the backup.

q_stop_aafs_for_update () {
    echo "Stopping AAFS for update..."

    # Check that AAFS is running

    if q_is_aafs_running ; then
        :
    else
        echo "AAFS is not running"
        exit 1
    fi

    # If a backup file is specified, check it does not already exist and is writeable

    if [ -z "$1" ]; then
        echo "Backup filename is not specified."
        echo "You must supply a filename for the database backup, or use \"nobackup\" to skip the backup."
        exit 1
    fi
    if [ "$1" != "nobackup" ]; then
        if [ -a "$1" ]; then
            echo "Backup file already exists: $1"
            echo "This function cannot replace an existing backup file."
            exit 1
        fi
        if touch "$1" ; then
            rm "$1"
        else
            echo "Cannot create backup file: $1"
            exit 1
        fi
    fi

    # Stop AAFS

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        cd /opt/aafs
        ./moaf.sh stop_secondary_aafs
        cd - >/dev/null
    else
        cd /opt/aafs
        ./moaf.sh stop_aafs
        cd - >/dev/null
    fi

    # Back up the database

    if [ "$1" != "nobackup" ]; then
        /opt/aafs/moaf.sh backup_database_gzip "$1"
    fi

    # Stop MongoDB

    cd /opt/aafs
    ./moaf.sh stop_mongo
    cd - >/dev/null

    echo "Pausing 10 seconds..."
    sleep 10

}




# Stop local AAFS in preparation for an update, and optionally back up the database.
# $1 = backup filename, or "nobackup" to skip the backup.

q_stop_local_for_update () {
    echo "Stopping local AAFS for update..."

    # Check that AAFS is running

    if q_is_aafs_running ; then
        :
    else
        echo "AAFS is not running"
        exit 1
    fi

    # If a backup file is specified, check it does not already exist and is writeable

    if [ -z "$1" ]; then
        echo "Backup filename is not specified."
        echo "You must supply a filename for the database backup, or use \"nobackup\" to skip the backup."
        exit 1
    fi
    if [ "$1" != "nobackup" ]; then
        if [ -a "$1" ]; then
            echo "Backup file already exists: $1"
            echo "This function cannot replace an existing backup file."
            exit 1
        fi
        if touch "$1" ; then
            rm "$1"
        else
            echo "Cannot create backup file: $1"
            exit 1
        fi
    fi

    # Stop AAFS

    cd /opt/aafs
    ./moaf.sh stop_aafs
    cd - >/dev/null

    # Back up the database

    if [ "$1" != "nobackup" ]; then
        /opt/aafs/moaf.sh backup_database_gzip "$1"
    fi

    # Stop MongoDB

    cd /opt/aafs
    ./moaf.sh stop_mongo
    cd - >/dev/null

    echo "Pausing 10 seconds..."
    sleep 10

}




# Start AAFS after performing an update.

q_start_aafs_after_update () {
    echo "Starting AAFS after update..."

    # Start MongoDB

    cd /opt/aafs
    ./moaf.sh start_mongo
    cd - >/dev/null

    # Start AAFS

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        cd /opt/aafs
        ./moaf.sh start_secondary_aafs
        cd - >/dev/null
    else
        cd /opt/aafs
        ./moaf.sh start_aafs
        cd - >/dev/null
    fi

}




# Start local AAFS after performing an update.

q_start_local_after_update () {
    echo "Starting local AAFS after update..."

    # Start MongoDB

    cd /opt/aafs
    ./moaf.sh start_mongo
    cd - >/dev/null

    # Start AAFS

    cd /opt/aafs
    ./moaf.sh start_aafs
    cd - >/dev/null

}




# Resume normal mode after update.

q_resume_normal_mode () {
    echo "Resuming normal mode after update..."

    # Check that AAFS is running

    if q_is_aafs_running ; then
        :
    else
        echo "AAFS is not running"
        exit 1
    fi

    # Set normal relay mode

    if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
        cd /opt/aafs
        ./moaf.sh change_relay_mode both pair 1
        cd - >/dev/null
    else
        echo "Single-server configurations are always in normal mode"
    fi

}




# Remove installed certificates, if any.

q_remove_certs () {
    echo "Removing certificates..."

    if q_is_mongo_running ; then
        echo "Please shut down AAFS and MongoDB before attempting to remove certificates."
        exit 1
    fi

    # Remove the certificate directory, if it exists

    if [ -d "$cval_CERT_DIR" ]; then
        sudo rm -r "$cval_CERT_DIR"
    fi

    # Set flags

    my_HAS_ROOT_CERT="$val_NO"
    my_HAS_SERVER_CERT="$val_NO"
    my_HAS_CLIENT_CERT="$val_NO"

}




# Install certificates.
# $1 = Source directory.

q_install_certs () {
    echo "Installing certificates..."

    if q_is_mongo_running ; then
        echo "Please shut down AAFS and MongoDB before attempting to install certificates."
        exit 1
    fi

    # Existence checks

    if [ ! -d "$1" ]; then
        echo "Cannot find directory : $1"
        exit 1
    fi

    if [ ! -f "$1/${cval_file_root_ca}.pem" ]; then
        echo "Cannot find file : $1/${cval_file_root_ca}.pem"
        exit 1
    fi

    if [ -f "$1/${cval_file_server}.pem" ]; then
        if [ ! -f "$1/${cval_file_server}${cval_suffix_pass}" ]; then
            echo "Cannot find file : $1/${cval_file_server}${cval_suffix_pass}"
            exit 1
        fi
    fi

    if [ -f "$1/${cval_file_client}.pem" ]; then
        if [ ! -f "$1/${cval_file_client}${cval_suffix_pass}" ]; then
            echo "Cannot find file : $1/${cval_file_client}${cval_suffix_pass}"
            exit 1
        fi
        if [ ! -f "$1/${cval_file_client}.p12" ]; then
            echo "Cannot find file : $1/${cval_file_client}.p12"
            exit 1
        fi
        if [ ! -f "$1/${cval_file_client}${cval_suffix_p12_pass}" ]; then
            echo "Cannot find file : $1/${cval_file_client}${cval_suffix_p12_pass}"
            exit 1
        fi
    fi

    # Remove the certificate directory, if it exists

    if [ -d "$cval_CERT_DIR" ]; then
        sudo rm -r "$cval_CERT_DIR"
    fi

    # Set flags

    my_HAS_ROOT_CERT="$val_NO"
    my_HAS_SERVER_CERT="$val_NO"
    my_HAS_CLIENT_CERT="$val_NO"

    # Make the certificate directory

    sudo mkdir "$cval_CERT_DIR"

    # Copy certs

    sudo cp --preserve=timestamps "$1/${cval_file_root_ca}.pem" "${cval_CERT_DIR}/${cval_file_root_ca}.pem"
    echo "Installed root CA certificate"
    my_HAS_ROOT_CERT="$val_YES"

    if [ -f "$1/${cval_file_server}.pem" ]; then
        sudo cp --preserve=timestamps "$1/${cval_file_server}.pem" "${cval_CERT_DIR}/${cval_file_server}.pem"
        sudo cp --preserve=timestamps "$1/${cval_file_server}${cval_suffix_pass}" "${cval_CERT_DIR}/${cval_file_server}${cval_suffix_pass}"
        echo "Installed server certificate"
        my_HAS_SERVER_CERT="$val_YES"
    fi

    if [ -f "$1/${cval_file_client}.pem" ]; then
        sudo cp --preserve=timestamps "$1/${cval_file_client}.pem" "${cval_CERT_DIR}/${cval_file_client}.pem"
        sudo cp --preserve=timestamps "$1/${cval_file_client}${cval_suffix_pass}" "${cval_CERT_DIR}/${cval_file_client}${cval_suffix_pass}"
        sudo cp --preserve=timestamps "$1/${cval_file_client}.p12" "${cval_CERT_DIR}/${cval_file_client}.p12"
        sudo cp --preserve=timestamps "$1/${cval_file_client}${cval_suffix_p12_pass}" "${cval_CERT_DIR}/${cval_file_client}${cval_suffix_p12_pass}"
        echo "Installed client certificate"
        my_HAS_CLIENT_CERT="$val_YES"
    fi

}




case "$1" in

    test_load_oaf_config)
        q_load_oaf_config

        echo ""
        echo "Configuration parameters:"
        echo "THE_OS_VERSION = $THE_OS_VERSION"
        echo "CPU_CORE_COUNT = $CPU_CORE_COUNT"
        echo "JAVA_SOURCE = $JAVA_SOURCE"
        echo "JAVA_CERT_FILE = $JAVA_CERT_FILE"
        echo "JAVA_MAX_MEMORY_GB = $JAVA_MAX_MEMORY_GB"
        echo "MONGO_BIND_IP = $MONGO_BIND_IP"
        echo "MONGO_CACHE_GB = $MONGO_CACHE_GB"
        echo "SERVER_OPTION = $SERVER_OPTION"
        echo "ACTION_OPTION = $ACTION_OPTION"
        echo "MONGO_ADMIN_USER = $MONGO_ADMIN_USER"
        echo "MONGO_ADMIN_PASS = $MONGO_ADMIN_PASS"
        echo "MONGO_NAME = $MONGO_NAME"
        echo "MONGO_USER = $MONGO_USER"
        echo "MONGO_PASS = $MONGO_PASS"
        echo "MONGO_REP_SET_1 = $MONGO_REP_SET_1"
        echo "MONGO_REP_SET_2 = $MONGO_REP_SET_2"
        echo "PDL_OPTION = $PDL_OPTION"
        echo "SERVER_IP_1 = $SERVER_IP_1"
        echo "SERVER_IP_2 = $SERVER_IP_2"
        echo "SERVER_NAME_1 = $SERVER_NAME_1"
        echo "SERVER_NAME_2 = $SERVER_NAME_2"
        echo "GUI_DATE = $GUI_DATE"

        echo ""
        echo "Named constants:"
        echo "val_OS_AMAZON_LINUX_2 = $val_OS_AMAZON_LINUX_2"
        echo "val_OS_AMAZON_LINUX_2023 = $val_OS_AMAZON_LINUX_2023"
        echo "val_OS_UBUNTU_2004 = $val_OS_UBUNTU_2004"
        echo "val_OS_UBUNTU_2204 = $val_OS_UBUNTU_2204"
        echo "val_OS_CENTOS_7 = $val_OS_CENTOS_7"
        echo "val_SERVER_PRIMARY = $val_SERVER_PRIMARY"
        echo "val_SERVER_SECONDARY = $val_SERVER_SECONDARY"
        echo "val_SERVER_SOLO = $val_SERVER_SOLO"
        echo "val_SERVER_DEV = $val_SERVER_DEV"
        echo "val_ACTION_USA = $val_ACTION_USA"
        echo "val_ACTION_DEV = $val_ACTION_DEV"
        echo "val_PDL_NONE = $val_PDL_NONE"
        echo "val_PDL_DEV = $val_PDL_DEV"
        echo "val_YES = $val_YES"
        echo "val_NO = $val_NO"
        echo "val_OSTYPE_AMAZON = $val_OSTYPE_AMAZON"
        echo "val_OSTYPE_UBUNTU = $val_OSTYPE_UBUNTU"
        echo "val_OSTYPE_CENTOS = $val_OSTYPE_CENTOS"
        echo "val_OSGEN_ANY = $val_OSGEN_ANY"
        echo "val_OSGEN_UBUNTU_2004 = $val_OSGEN_UBUNTU_2004"
        echo "val_OSGEN_UBUNTU_2204 = $val_OSGEN_UBUNTU_2204"
        echo "val_LOCAL_ACCOUNT = $val_LOCAL_ACCOUNT"
        echo "val_PWD = $val_PWD"
        echo "val_TEMP_WORK_DIR = $val_TEMP_WORK_DIR"
        echo "val_NNINT_REGEX = $val_NNINT_REGEX"
        echo "val_LAUNCH_OPTIONS_FILE = $val_LAUNCH_OPTIONS_FILE"
        echo "cval_file_root_ca = $cval_file_root_ca"
        echo "cval_file_server = $cval_file_server"
        echo "cval_file_client = $cval_file_client"
        echo "cval_suffix_pass = $cval_suffix_pass"
        echo "cval_suffix_p12_pass = $cval_suffix_p12_pass"
        echo "cval_CERT_DIR = $cval_CERT_DIR"

        echo ""
        echo "Internal variables:"
        echo "my_OS_TYPE = $my_OS_TYPE"
        echo "my_OS_GEN = $my_OS_GEN"
        echo "my_IS_SELINUX = $my_IS_SELINUX"
        echo "my_OS_SPECIFIC_PATH = $my_OS_SPECIFIC_PATH"
        echo "my_MONGO_LOCAL_ACCOUNT = $my_MONGO_LOCAL_ACCOUNT"
        echo "my_MONGO_DEF_DATA_PATH = $my_MONGO_DEF_DATA_PATH"
        echo "my_MONGO_DEF_LOG_PATH = $my_MONGO_DEF_LOG_PATH"
        echo "my_MONGO_CONF_FILE = $my_MONGO_CONF_FILE"
        echo "my_MONGO_CONF_BACKUP = $my_MONGO_CONF_BACKUP"
        echo "my_MONGO_KEY_FILE = $my_MONGO_KEY_FILE"
        echo "my_MONGO_KEY_CONTENT = $my_MONGO_KEY_CONTENT"
        echo "my_MONGO_BIND_IP_LIST = $my_MONGO_BIND_IP_LIST"
        echo "my_MONGO_LOCAL_REPSET = $my_MONGO_LOCAL_REPSET"
        echo "my_IS_DUAL_SERVER = $my_IS_DUAL_SERVER"
        echo "my_IS_SECONDARY_SERVER = $my_IS_SECONDARY_SERVER"
        echo "my_UNAME_ARCH = $my_UNAME_ARCH"
        echo "my_ARM_ARCH = $my_ARM_ARCH"
        echo "my_IS_JAVA_FILE_SOURCE = $my_IS_JAVA_FILE_SOURCE"
        echo "my_JAVA_FILE_BASENAME = $my_JAVA_FILE_BASENAME"
        echo "my_HAS_ROOT_CERT = $my_HAS_ROOT_CERT"
        echo "my_HAS_SERVER_CERT = $my_HAS_SERVER_CERT"
        echo "my_HAS_CLIENT_CERT = $my_HAS_CLIENT_CERT"
        ;;

    test_check_installed)
        q_load_oaf_config
        q_check_installed
        echo "Test - Installation check"
        ;;

    test_check_java)
        q_load_oaf_config
        q_check_java
        echo "Test - Java check"
        ;;

    test_install_packages)
        q_load_oaf_config
        q_install_packages
        echo "Test - Installed packages"
        ;;

    test_install_java)
        q_load_oaf_config
        q_install_java
        echo "Test - Installed Java"
        ;;

    test_configure_java_dns)
        q_load_oaf_config
        q_configure_java_dns
        echo "Test - Configured Java DNS"
        ;;

    test_set_aafs_vars)
        q_load_oaf_config
        q_set_aafs_vars
        echo "Test - Set AAFS environment variables"
        ;;

    test_create_dirs)
        q_load_oaf_config
        q_create_dirs
        echo "Test - Created directories"
        ;;

    test_download_opensha)
        q_load_oaf_config
        q_download_opensha
        echo "Test - Download OpenSHA"
        ;;

    test_update_opensha)
        q_load_oaf_config
        q_update_opensha
        echo "Test - Update OpenSHA"
        ;;

    test_install_mongo)
        q_load_oaf_config
        q_install_mongo
        echo "Test - Installed MongoDB (default)"
        ;;

    test_install_mongo_44)
        q_load_oaf_config
        q_install_mongo_44
        echo "Test - Installed MongoDB 4.4"
        ;;

    test_install_mongo_50)
        q_load_oaf_config
        q_install_mongo_50
        echo "Test - Installed MongoDB 5.0"
        ;;

    test_install_mongo_60)
        q_load_oaf_config
        q_install_mongo_60
        echo "Test - Installed MongoDB 6.0"
        ;;

    test_install_mongo_70)
        q_load_oaf_config
        q_install_mongo_70
        echo "Test - Installed MongoDB 7.0"
        ;;

    test_configure_mongo)
        q_load_oaf_config
        q_configure_mongo
        echo "Test - Configured MongoDB"
        ;;

    test_setup_mongo)
        q_load_oaf_config
        q_setup_mongo
        echo "Test - Set up MongoDB"
        ;;

    test_setup_mongo_4x)
        q_load_oaf_config
        q_setup_mongo_4x
        echo "Test - Set up MongoDB 4.X"
        ;;

    test_set_mongo_compatibility_to_44)
        q_load_oaf_config
        q_set_mongo_compatibility_to_44
        echo "Test - Set MongoDB feature compatibility version to 4.4"
        ;;

    test_set_mongo_compatibility_to_50)
        q_load_oaf_config
        q_set_mongo_compatibility_to_50
        echo "Test - Set MongoDB feature compatibility version to 5.0"
        ;;

    test_set_mongo_compatibility_to_60)
        q_load_oaf_config
        q_set_mongo_compatibility_to_60
        echo "Test - Set MongoDB feature compatibility version to 6.0"
        ;;

    test_set_mongo_compatibility_to_70)
        q_load_oaf_config
        q_set_mongo_compatibility_to_70
        echo "Test - Set MongoDB feature compatibility version to 7.0"
        ;;

    test_get_mongo_compatibility)
        q_load_oaf_config
        q_get_mongo_compatibility
        echo "Test - Get MongoDB feature compatibility version"
        ;;

    test_configure_oaf)
        q_load_oaf_config
        q_configure_oaf
        echo "Test - Configured OAF"
        ;;

    test_compile_oaf)
        q_load_oaf_config
        q_compile_oaf
        echo "Test - Compiled OAF"
        ;;

    test_rebuild_gui)
        q_load_oaf_config
        q_rebuild_gui
        echo "Test - Rebuilt GUI"
        ;;

    # $2 = relay mode.
    # $3 = configured primary
    test_validate_relay_mode)
        q_load_oaf_config
        q_validate_relay_mode "$2" "$3"
        echo "Test - Validated relay mode"
        ;;

    # $2 = relay mode.
    # $3 = configured primary
    test_init_oaf_database)
        q_load_oaf_config
        q_init_oaf_database "$2" "$3"
        echo "Test - Initialized OAF database"
        ;;

    # $2 = Backup filename, in gzipped format.
    # $3 = relay mode.
    # $4 = configured primary
    test_restore_oaf_database)
        q_load_oaf_config
        q_restore_oaf_database "$2" "$3" "$4"
        echo "Test - Restored OAF database"
        ;;

    # $2 = Backup filename, or "nobackup" to skip backup.
    test_stop_aafs_for_update)
        q_load_oaf_config
        q_stop_aafs_for_update "$2"
        echo "Test - Stopped AAFS for update"
        ;;

    # $2 = Backup filename, or "nobackup" to skip backup.
    test_stop_local_for_update)
        q_load_oaf_config
        q_stop_local_for_update "$2"
        echo "Test - Stopped local AAFS for update"
        ;;

    test_start_aafs_after_update)
        q_load_oaf_config
        q_start_aafs_after_update
        echo "Test - Started AAFS after update"
        ;;

    test_start_local_after_update)
        q_load_oaf_config
        q_start_local_after_update
        echo "Test - Started local AAFS after update"
        ;;

    test_resume_normal_mode)
        q_load_oaf_config
        q_resume_normal_mode
        echo "Test - Resumed normal mode after update"
        ;;

    test_check_pdl_key_file)
        q_load_oaf_config
        q_check_pdl_key_file
        echo "Test - Checked for PDL key file"
        ;;

    test_install_oaf_no_mongo)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Test - Installed OAF without installing MongoDB."
        echo "It is recommended that you reboot the system now."
        ;;

    test_remove_certs)
        q_load_oaf_config
        q_remove_certs
        echo "Test - Removed certificates"
        ;;

    # $2 = Source directory.
    test_install_certs)
        q_load_oaf_config
        q_install_certs "$2"
        echo "Test - Installed certificates"
        ;;

    test_run_mongosh)
        q_load_oaf_config
        q_run_mongosh
        echo "Test - Ran MongoDB shell"
        ;;

    # $2 = Host address.
    test_run_mongosh_to)
        q_load_oaf_config
        q_run_mongosh_to "$2"
        echo "Test - Ran MongoDB shell to the given host"
        ;;




    prepare_system)
        q_load_oaf_config
        q_install_packages
        q_install_java
        q_configure_java_dns
        q_set_aafs_vars
        echo ""
        echo "********************"
        echo ""
        echo "You need to log out and log in (or reboot) to complete the Java installation."
        echo "Then, use "'"'"install_oaf"'"'" to install the OAF software and MongoDB."
        ;;




    install_oaf)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_install_mongo
        q_configure_mongo
        q_setup_mongo
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF installation."
        echo "It is recommended that you reboot the system now."
        echo "Then, use "'"'"initialize_oaf"'"'" to initialize the database, or "'"'"restore_oaf"'"'" to restore the database from a backup."
        ;;




    install_oaf_44)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_install_mongo_44
        q_configure_mongo
        q_setup_mongo_4x
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF installation."
        echo "It is recommended that you reboot the system now."
        echo "Then, use "'"'"initialize_oaf"'"'" to initialize the database, or "'"'"restore_oaf"'"'" to restore the database from a backup."
        ;;




    install_oaf_50)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_install_mongo_50
        q_configure_mongo
        q_setup_mongo
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF installation."
        echo "It is recommended that you reboot the system now."
        echo "Then, use "'"'"initialize_oaf"'"'" to initialize the database, or "'"'"restore_oaf"'"'" to restore the database from a backup."
        ;;




    install_oaf_60)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_install_mongo_60
        q_configure_mongo
        q_setup_mongo
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF installation."
        echo "It is recommended that you reboot the system now."
        echo "Then, use "'"'"initialize_oaf"'"'" to initialize the database, or "'"'"restore_oaf"'"'" to restore the database from a backup."
        ;;




    install_oaf_70)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_install_mongo_70
        q_configure_mongo
        q_setup_mongo
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF installation."
        echo "It is recommended that you reboot the system now."
        echo "Then, use "'"'"initialize_oaf"'"'" to initialize the database, or "'"'"restore_oaf"'"'" to restore the database from a backup."
        ;;




    # $2 = relay mode.
    # $3 = configured primary
    initialize_oaf)
        q_load_oaf_config
        q_validate_relay_mode "$2" "$3"
        q_init_oaf_database "$2" "$3"
        echo ""
        echo "********************"
        echo ""
        echo "The server relay mode has been initialized to "'"'"$2 $3"'"'"."
        echo ""
        echo "Completed OAF initialization."
        ;;




    # $2 = Backup filename, in gzipped format.
    # $3 = relay mode.
    # $4 = configured primary
    restore_oaf)
        q_load_oaf_config
        if [ -z "$2" ]; then
            echo "Database backup file is not specified"
            exit 1
        fi
        if [ ! -f "$2" ]; then
            echo "Database backup file is not found: $1"
            exit 1
        fi
        q_validate_relay_mode "$3" "$4"
        q_restore_oaf_database "$2" "$3" "$4"
        echo ""
        echo "********************"
        echo ""
        echo "Database has been restored from: $2"
        echo ""
        echo "The server relay mode has been initialized to "'"'"$3 $4"'"'"."
        echo ""
        echo "Completed OAF restoration."
        ;;




    update_oaf)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to update OAF."
            exit 1
        fi
        q_load_oaf_config
        q_update_opensha
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF update."
        ;;




    freshen_oaf)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to update OAF."
            exit 1
        fi
        q_load_oaf_config
        q_download_opensha
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF update."
        ;;




    reconfigure_oaf)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to reconfigure OAF."
            exit 1
        fi
        q_load_oaf_config
        q_configure_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF reconfiguration."
        ;;




    rebuild_gui)
        q_load_oaf_config
        q_rebuild_gui
        echo ""
        echo "********************"
        echo ""
        echo "Completed rebuilding GUI."
        ;;




    update_java)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to update Java."
            exit 1
        fi
        q_load_oaf_config
        my_ORIGINAL_JAVA_HOME="$JAVA_HOME"
        q_install_java
        q_configure_java_dns
        echo ""
        echo "********************"
        echo ""
        if [ "$my_ORIGINAL_JAVA_HOME" != "/usr/local/java" ]; then
            echo "You need to log out and log in (or reboot) to complete the Java installation."
        else
            echo "Completed Java update."
        fi
        ;;




    update_aafs_vars)
        q_load_oaf_config
        q_set_aafs_vars
        echo ""
        echo "********************"
        echo ""
        echo "You need to log out and log in (or reboot) for the new environment variables to take effect."
        ;;




    reconfigure_mongo)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to reconfigure MongoDB."
            exit 1
        fi
        q_load_oaf_config
        if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
            echo "Cannot find the MongoDB original configuration file: $my_MONGO_CONF_BACKUP"
            echo "To reconfigure MongoDB, the original configuration file must be available in: $my_MONGO_CONF_BACKUP"
            exit 1
        fi
        q_configure_mongo
        echo ""
        echo "********************"
        echo ""
        echo "Completed MongoDB reconfiguration."
        ;;




    upgrade_mongo_42_to_44)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to upgrade MongoDB."
            exit 1
        fi
        q_load_oaf_config
        if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
            echo "Cannot find the MongoDB original configuration file: $my_MONGO_CONF_BACKUP"
            echo "To upgrade MongoDB, the original configuration file must be available in: $my_MONGO_CONF_BACKUP"
            exit 1
        fi
        q_upgrade_mongo_42_to_44
        q_configure_mongo
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB 4.4 has been registered with the operating system."
        echo "To complete the MongoDB upgrade, you need to perform an operating system update."
        echo ""
        echo "After MongoDB 4.4 is running successfully, use \"set_mongo_compatibility_to_44\""
        echo "to enable MongoDB 4.4 features."
        ;;




    set_mongo_compatibility_to_44)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to set MongoDB compatibility."
            exit 1
        fi
        q_load_oaf_config
        q_set_mongo_compatibility_to_44
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB feature compatibility version has been set to 4.4."
        ;;




    upgrade_mongo_44_to_50)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to upgrade MongoDB."
            exit 1
        fi
        q_load_oaf_config
        if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
            echo "Cannot find the MongoDB original configuration file: $my_MONGO_CONF_BACKUP"
            echo "To upgrade MongoDB, the original configuration file must be available in: $my_MONGO_CONF_BACKUP"
            exit 1
        fi
        q_upgrade_mongo_44_to_50
        q_configure_mongo
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB 5.0 has been registered with the operating system."
        echo "To complete the MongoDB upgrade, you need to perform an operating system update."
        echo ""
        echo "After MongoDB 5.0 is running successfully, use \"set_mongo_compatibility_to_50\""
        echo "to enable MongoDB 5.0 features."
        ;;




    set_mongo_compatibility_to_50)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to set MongoDB compatibility."
            exit 1
        fi
        q_load_oaf_config
        q_set_mongo_compatibility_to_50
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB feature compatibility version has been set to 5.0."
        ;;




    upgrade_mongo_50_to_60)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to upgrade MongoDB."
            exit 1
        fi
        q_load_oaf_config
        if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
            echo "Cannot find the MongoDB original configuration file: $my_MONGO_CONF_BACKUP"
            echo "To upgrade MongoDB, the original configuration file must be available in: $my_MONGO_CONF_BACKUP"
            exit 1
        fi
        q_upgrade_mongo_50_to_60
        q_configure_mongo
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB 6.0 has been registered with the operating system."
        echo "To complete the MongoDB upgrade, you need to perform an operating system update."
        echo ""
        echo "After MongoDB 6.0 is running successfully, use \"set_mongo_compatibility_to_60\""
        echo "to enable MongoDB 6.0 features."
        ;;




    set_mongo_compatibility_to_60)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to set MongoDB compatibility."
            exit 1
        fi
        q_load_oaf_config
        q_set_mongo_compatibility_to_60
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB feature compatibility version has been set to 6.0."
        ;;




    upgrade_mongo_60_to_70_not_recommended)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to upgrade MongoDB."
            exit 1
        fi
        q_load_oaf_config
        if [ ! -f "$my_MONGO_CONF_BACKUP" ]; then
            echo "Cannot find the MongoDB original configuration file: $my_MONGO_CONF_BACKUP"
            echo "To upgrade MongoDB, the original configuration file must be available in: $my_MONGO_CONF_BACKUP"
            exit 1
        fi
        q_upgrade_mongo_60_to_70
        q_configure_mongo
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB 7.0 has been registered with the operating system."
        echo "To complete the MongoDB upgrade, you need to perform an operating system update."
        echo ""
        echo "After MongoDB 7.0 is running successfully, use \"set_mongo_compatibility_to_70\""
        echo "to enable MongoDB 7.0 features."
        ;;




    set_mongo_compatibility_to_70)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to set MongoDB compatibility."
            exit 1
        fi
        q_load_oaf_config
        q_set_mongo_compatibility_to_70
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB feature compatibility version has been set to 7.0."
        ;;




    get_mongo_compatibility)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to get MongoDB compatibility."
            exit 1
        fi
        q_load_oaf_config
        q_get_mongo_compatibility
        echo ""
        echo "********************"
        echo ""
        echo "MongoDB feature compatibility version has been displayed."
        ;;




    # $2 = "java" to update Java, or "nojava" to skip the Java update.
    # $3 = "oaf" to update OAF software, or "nooaf" to skip the OAF software update.
    # $4 = Backup filename, or "nobackup" to skip the backup.
    stop_aafs_for_update)
        if [ "$2" == "java" ]; then
            :
        elif [ "$2" == "nojava" ]; then
            :
        else
            echo "The Java option must be \"java\" to update Java, or \"nojava\" to skip the Java update."
            exit 1
        fi
        if [ "$3" == "oaf" ]; then
            :
        elif [ "$3" == "nooaf" ]; then
            :
        else
            echo "The OAF option must be \"oaf\" to update the OAF software, or \"nooaf\" to skip the OAF software update."
            exit 1
        fi
        if [ -z "$4" ]; then
            echo "Backup filename is not specified."
            echo "You must supply a filename for the database backup, or use \"nobackup\" to skip the backup."
            exit 1
        fi
        q_load_oaf_config
        q_stop_aafs_for_update "$4"
        if [ "$2" == "java" ]; then
            q_install_java
            q_configure_java_dns
        fi
        if [ "$3" == "oaf" ]; then
            q_update_opensha
            q_configure_oaf
            q_compile_oaf
        fi
        echo ""
        echo "********************"
        echo ""
        echo "AAFS and MongoDB have been stopped."
        if [ "$4" != "nobackup" ]; then
            echo "The OAF database has been backed up to: $4"
        fi
        if [ "$2" == "java" ]; then
            echo "Java has been updated."
        fi
        if [ "$3" == "oaf" ]; then
            echo "The OAF software has been updated, and the updated version is installed and configured."
        fi
        echo ""
        echo "You can now update the operating system, and do any other needed updates."
        echo "Then, you can reboot the system if needed."
        echo ""
        echo "After completing all updates, use \"start_aafs_after_update\" to re-start AAFS and MongoDB."
        ;;




    start_aafs_after_update)
        q_load_oaf_config
        q_start_aafs_after_update
        echo ""
        echo "********************"
        echo ""
        echo "AAFS and MongoDB have been re-started."
        echo ""
        if [ "$my_IS_DUAL_SERVER" == "$val_YES" ]; then
            echo "You should now update the other server, if you have not already done so."
            echo ""
            echo "After both servers are updated, use \"resume_normal_mode\" to resume normal operation."
            echo "(You only need to run \"resume_normal_mode\" on one of the servers. Either server will do.)"
        else
            echo "Completed update."
        fi
        ;;




    resume_normal_mode)
        q_load_oaf_config
        q_resume_normal_mode
        echo ""
        echo "********************"
        echo ""
        echo "Resumed normal mode of operation."
        ;;




    # $2 = "java" to update Java, or "nojava" to skip the Java update.
    # $3 = "oaf" to update OAF software, or "nooaf" to skip the OAF software update.
    # $4 = Backup filename, or "nobackup" to skip the backup.
    stop_local_for_update)
        if [ "$2" == "java" ]; then
            :
        elif [ "$2" == "nojava" ]; then
            :
        else
            echo "The Java option must be \"java\" to update Java, or \"nojava\" to skip the Java update."
            exit 1
        fi
        if [ "$3" == "oaf" ]; then
            :
        elif [ "$3" == "nooaf" ]; then
            :
        else
            echo "The OAF option must be \"oaf\" to update the OAF software, or \"nooaf\" to skip the OAF software update."
            exit 1
        fi
        if [ -z "$4" ]; then
            echo "Backup filename is not specified."
            echo "You must supply a filename for the database backup, or use \"nobackup\" to skip the backup."
            exit 1
        fi
        q_load_oaf_config
        q_stop_local_for_update "$4"
        if [ "$2" == "java" ]; then
            q_install_java
            q_configure_java_dns
        fi
        if [ "$3" == "oaf" ]; then
            q_update_opensha
            q_configure_oaf
            q_compile_oaf
        fi
        echo ""
        echo "********************"
        echo ""
        echo "Local AAFS and MongoDB have been stopped."
        if [ "$4" != "nobackup" ]; then
            echo "The OAF database has been backed up to: $4"
        fi
        if [ "$2" == "java" ]; then
            echo "Java has been updated."
        fi
        if [ "$3" == "oaf" ]; then
            echo "The OAF software has been updated, and the updated version is installed and configured."
        fi
        echo ""
        echo "You can now update the operating system, and do any other needed updates."
        echo "Then, you can reboot the system if needed."
        echo ""
        echo "After completing all updates, use \"start_local_after_update\" to re-start local AAFS and MongoDB."
        ;;




    start_local_after_update)
        q_load_oaf_config
        q_start_local_after_update
        echo ""
        echo "********************"
        echo ""
        echo "Local AAFS and MongoDB have been re-started."
        echo ""
        echo "Completed update."
        ;;




    install_oaf_no_mongo)
        q_load_oaf_config
        q_check_installed
        q_check_java
        q_create_dirs
        q_download_opensha
        q_configure_oaf
        q_compile_oaf
        echo ""
        echo "********************"
        echo ""
        echo "The selected ACTION_OPTION is "'"'"$ACTION_OPTION"'"'
        echo ""
        q_check_pdl_key_file
        echo ""
        echo "Completed OAF installation."
        echo "It is recommended that you reboot the system now."
        ;;




    remove_certs)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to remove certificates."
            exit 1
        fi
        q_load_oaf_config
        q_remove_certs

        # If there is an existing Java installation, update it, and the AAFS environment variables
        my_NEED_REBOOT="$val_NO"
        if [ -d /usr/local/java ]; then
            my_NEED_REBOOT="$val_YES"
            q_install_java
            q_configure_java_dns
            q_set_aafs_vars
        fi

        # If there is an existing MongoDB installation, update the configuration
        if [ -f "$my_MONGO_CONF_FILE" ]; then
            q_configure_mongo
        fi

        # If there is an existing OAF installation, update the configuration
        if [ -d /opt/aafs ]; then
            q_configure_oaf
        fi

        echo ""
        echo "********************"
        echo ""
        echo "Removed certificates."
        if [ "$my_NEED_REBOOT" == "$val_YES" ]; then
            echo "It is recommended that you reboot the system now."
        fi
        ;;




    # $2 = Source directory.
    install_certs)
        if q_is_mongo_running ; then
            echo "Please shut down AAFS and MongoDB before attempting to install certificates."
            exit 1
        fi
        q_load_oaf_config
        q_install_certs "$2"

        # If there is an existing Java installation, update it, and the AAFS environment variables
        my_NEED_REBOOT="$val_NO"
        if [ -d /usr/local/java ]; then
            my_NEED_REBOOT="$val_YES"
            q_install_java
            q_configure_java_dns
            q_set_aafs_vars
        fi

        # If there is an existing MongoDB installation, update the configuration
        if [ -f "$my_MONGO_CONF_FILE" ]; then
            q_configure_mongo
        fi

        # If there is an existing OAF installation, update the configuration
        if [ -d /opt/aafs ]; then
            q_configure_oaf
        fi

        echo ""
        echo "********************"
        echo ""
        echo "Installed certificates."
        if [ "$my_NEED_REBOOT" == "$val_YES" ]; then
            echo "It is recommended that you reboot the system now."
        fi
        ;;




    run_mongosh)
        q_load_oaf_config
        q_run_mongosh
        ;;




    # $2 = Host address.
    run_mongosh_to)
        q_load_oaf_config
        q_run_mongosh_to "$2"
        ;;




    help)
        echo "Install required packages and Java:"
        echo "  aoaf.sh prepare_system"
        echo "Install and configure the OAF software and MongoDB:"
        echo "  aoaf.sh install_oaf"
        echo "Install and configure the OAF software and a specific version of MongoDB:"
        echo "  aoaf.sh install_oaf_44"
        echo "  aoaf.sh install_oaf_50"
        echo "  aoaf.sh install_oaf_60"
        echo "  aoaf.sh install_oaf_70"
        echo "Install and configure the OAF software without installing MongoDB:"
        echo "  aoaf.sh install_oaf_no_mongo"
        echo "Initialize the OAF database:"
        echo "  aoaf.sh initialize_oaf  <relay_mode>  <configured_primary>"
        echo "Restore the OAF database:"
        echo "  aoaf.sh restore_oaf  <filename>  <relay_mode>  <configured_primary>"
        echo "Update and configure the OAF software:"
        echo "  aoaf.sh update_oaf"
        echo "Install and configure a fresh copy of the OAF software:"
        echo "  aoaf.sh freshen_oaf"
        echo "Reconfigure the OAF software (without updating or rebuilding it):"
        echo "  aoaf.sh reconfigure_oaf"
        echo "Rebuild the GUI (without updating or rebuilding OAF):"
        echo "  aoaf.sh rebuild_gui"
        echo "Update or re-install Java:"
        echo "  aoaf.sh update_java"
        echo "Update the AAFS environment variables:"
        echo "  aoaf.sh update_aafs_vars"
        echo "Update the MongoDB configuration file:"
        echo "  aoaf.sh reconfigure_mongo"
        echo "Upgrade an existing installation of MongoDB to the next version:"
        echo "  aoaf.sh upgrade_mongo_42_to_44"
        echo "  aoaf.sh upgrade_mongo_44_to_50"
        echo "  aoaf.sh upgrade_mongo_50_to_60"
        echo "  aoaf.sh upgrade_mongo_60_to_70_not_recommended"
        echo "Set MongoDB compatibility version:"
        echo "  aoaf.sh set_mongo_compatibility_to_44"
        echo "  aoaf.sh set_mongo_compatibility_to_50"
        echo "  aoaf.sh set_mongo_compatibility_to_60"
        echo "  aoaf.sh set_mongo_compatibility_to_70"
        echo "Get MongoDB compatibility version:"
        echo "  aoaf.sh get_mongo_compatibility"
        echo "Stop AAFS and MongoDB, so that updates can be performed:"
        echo "  aoaf.sh stop_aafs_for_update  <java_option>  <oaf_option>  <backup_filename>"
        echo "Re-start AAFS and MongoDB after performing updates:"
        echo "  aoaf.sh start_aafs_after_update"
        echo "Resume normal operation of a dual-server configuration after updating:"
        echo "  aoaf.sh resume_normal_mode"
        echo "Stop local AAFS and MongoDB, so that updates can be performed:"
        echo "  aoaf.sh stop_local_for_update  <java_option>  <oaf_option>  <backup_filename>"
        echo "Re-start local AAFS and MongoDB after performing updates:"
        echo "  aoaf.sh start_local_after_update"
        echo "Install certificates from the given directory:"
        echo "  aoaf.sh install_certs  <source_dir>"
        echo "Remove certificates:"
        echo "  aoaf.sh remove_certs"
        echo "Start the MongoDB shell (mongosh):"
        echo "  aoaf.sh run_mongosh"
        echo "Start the MongoDB shell (mongosh), for the given host address:"
        echo "  aoaf.sh run_mongosh_to  <host_address>"
        ;;




    *)
        echo "Usage: 'aoaf.sh help' to display help."
        exit 1
        ;;
esac

exit 0

