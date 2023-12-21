# This is a set of options for selecting the AAFS configuration.
# For most applications, the entire AAFS installation can be confgured
# from the information in this file.  Edit it to select your configuration.

# This file should be named oaf_config.sh and it should be placed either
# in your home directory or else in the directory where the aoaf.sh script
# is located (typically ~/opensha).  This file is sourced by the quick
# install script aoaf.sh, which looks first in the current directory
# and then in the home directory.

# You can use the following command to download this file from Github:
# wget https://github.com/opensha/opensha-oaf/raw/master/deployment/scripts/oaf_config.sh

# You can use these commands to download the script aoaf.h from Github:
# wget https://github.com/opensha/opensha-oaf/raw/master/deployment/scripts/aoaf.sh
# chmod 755 aoaf.sh




# Edit the values below.
#
# Each value should appear immediately after the equals sign with no
# intervening spaces.  A value can be left blank my putting nothing after
# the equals sign.  Generally, each value should be enclosed in double
# quotes; the quotes themselves are not part of the value.  Since this
# is a bash script file, any value valid in bash can be used.


# ----- Values that describe the computing environment -----


# Operating system option. Must be one of:
#  "amazonlinux2" -- Amazon Linux 2.
#  "amazonlinux2023" -- Amazon Linux 2023.
#  "ubuntu2004" -- Ubuntu 20.04 LTS.
#  "ubuntu2204" -- Ubuntu 22.04 LTS.
#  "centos7" -- Centos 7 (might also work for Centos 8 and Centos Stream).
THE_OS_VERSION=


# Number of CPU cores to use for computationally-intensive code.
# If blank or 0, then the core count is obtained from the Java VM,
# but note that the VM value is incorrect if hyperthreading is used.
CPU_CORE_COUNT=


# Source from which to obtain Java.
# If it begins with "/" or "~", it is a fully-qualified filename.
# Otherwise, it is a URL from which Java is downloaded.
# If left blank, it defaults to Amazon Corretto 11.
# This should be a path with the last component ending in .tar.gz.
JAVA_SOURCE=


# Digital certificate file to install in Java.
# Leave blank if no digital certificate is needed.
# If non-blank, it should be a fully-qualified file name, typically ending in .cer.
JAVA_CERT_FILE=


# Maximum memory to use for the Java VM heap, in GB, only for the AAFS server.
# If blank or 0, then the default value is used, which is typically one-fourth
# of the system memory.
JAVA_MAX_MEMORY_GB=


# The IP address that MongoDB should bind to.
# This may have any of the following values.
#   blank -- Use the value of SERVER_IP_2 if this is server #2 in a dual-server
#       configuration, otherwise use the value of SERVER_IP_1.  In this case, the
#       server IP address must be given as an IP address and not a DNS name.
#       (If the server IP address is blank, then "$(hostname -I)" is used.)
#   "127.0.0.1" -- Bind only to localhost.
#   "0.0.0.0" -- Bind to all IP addresses.
#   An IP address -- Bind to the specified IP address in addition to localhost.
#       This must be an IP address and not a DNS name.
#   A comma-separated list of IP addresses -- Bind to all the specified addresses
#       in addition to localhost.  The list should not include 127.0.0.1 because
#       it is added automatically.
#   "$(hostname -I)" -- Bind to the computer's IP address as returned by hostname.
MONGO_BIND_IP=


# Amount of memory to use for the MongoDB cache, in GB.
# If blank or 0, then the default value is used, which is typically one-half
# of the system memory.
MONGO_CACHE_GB=


# ----- Values that control the AAFS configuration -----


# Server option.  Must be one of:
#  "primary" -- Server #1 in a dual-server configuration.
#  "secondary" -- Server #2 in a dual-server configuration.
#  "solo" -- A single-server configuration.
#  "dev" -- A development server in single-server configuration with default usernames and passwords.
# The selected option affects the values that are allowed or required for the
# parameters below, as indicated in the individual parameter descriptions.
# For a dual-server configuration, all the parameters below should have the
# same value on both servers.
SERVER_OPTION=


# Action option.  Must be one of:
#  "usa" -- Generate forecasts for the United States.
#  "dev" -- Development settings, which generate forecasts for the world.
ACTION_OPTION=


# MongoDB administrative username.
# This username is used to perform administrative functions such as creating account.
# It will be created in the "admin" database with full access to all administrative tasks.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank.
#  "secondary" -- Cannot be blank.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "mongoadmin" is used.
# If not blank, it should consist of lowercase letters and digits, starting with a letter.
MONGO_ADMIN_USER=


# MongoDB administrative password.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank.
#  "secondary" -- Cannot be blank.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "mongoadmin" is used.
MONGO_ADMIN_PASS=


# Name of the MongoDB database that is used for storing AAFS data.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank.
#  "secondary" -- Cannot be blank.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "usgs" is used.
# If not blank, it should consist of lowercase letters and digits, starting with a letter.
MONGO_NAME=


# MongoDB username.
# The AAFS software uses this username to log in to MongoDB.
# It will be created in the MONGO_NAME database with full access to all database operation.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank.
#  "secondary" -- Cannot be blank.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "usgs" is used.
# If not blank, it should consist of lowercase letters and digits, starting with a letter.
MONGO_USER=


# MongoDB password.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank.
#  "secondary" -- Cannot be blank.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "usgs" is used.
MONGO_PASS=


# MongoDB replica set name for server #1.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank, and must be different than MONGO_REP_SET_2.
#  "secondary" -- Cannot be blank, and must be different than MONGO_REP_SET_2.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "rs0" is used.
# If not blank, it should consist of lowercase letters and digits, starting with a letter.
MONGO_REP_SET_1=


# MongoDB replica set name for server #2.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank, and must be different than MONGO_REP_SET_1.
#  "secondary" -- Cannot be blank, and must be different than MONGO_REP_SET_1.
#  "solo"      -- Must be blank.
#  "dev"       -- Ignored.  It is treated as being blank.
# If not blank, it should consist of lowercase letters and digits, starting with a letter.
MONGO_REP_SET_2=


# Destination for forecasts.  Must be one of:
#  "none" -- Do not send forecasts to PDL.
#  "dev" -- Send forecasts to PDL developement servers.
#  keyfile_name -- Send forecasts to PDL production servers.  The value is the name of
#    the cryptographic key file that is used to sign forecasts (filename only, without
#    a path).  This script does not install the key file.  The user must install the
#    key file in /opt/aafs/key.
# If the SERVER_OPTION is "dev", then this is ignored and the default value "none" is used.
PDL_OPTION=


# IP address or DNS name for server #1.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- If blank it defaults "$(hostname -I)" which should be
#                 the local IP address.  Must be different than SERVER_IP_2.
#  "secondary" -- Cannot be blank, and must be different than SERVER_IP_2.
#  "solo"      -- If blank it defaults "$(hostname -I)" which should be
#                 the local IP address.
#  "dev"       -- If blank it defaults "$(hostname -I)" which should be
#                 the local IP address.
SERVER_IP_1=


# IP address or DNS name for server #2.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank, and must be different than SERVER_IP_1.
#  "secondary" -- If blank it defaults "$(hostname -I)" which should be
#                 the local IP address.  Must be different than SERVER_IP_1.
#  "solo"      -- Must be blank.
#  "dev"       -- Ignored.  It is treated as being blank.
SERVER_IP_2=


# Name assigned to server #1.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank, and must be different than SERVER_NAME_2.
#  "secondary" -- Cannot be blank, and must be different than SERVER_NAME_2.
#  "solo"      -- Cannot be blank.
#  "dev"       -- Ignored.  The default value "test" is used.
SERVER_NAME_1=


# Name assigned to server #2.
# The following applies depending on the SERVER_OPTION:
#  "primary"   -- Cannot be blank, and must be different than SERVER_NAME_1.
#  "secondary" -- Cannot be blank, and must be different than SERVER_NAME_1.
#  "solo"      -- Must be blank.
#  "dev"       -- Ignored.  It is treated as being blank.
SERVER_NAME_2=


# The date or tag to include in the GUI filename.
# If blank, it defaults to "$(date +%Y_%m_%d)" which is the current date in the form YYYY_MM_DD.
# This must not contain spaces.
GUI_DATE=


