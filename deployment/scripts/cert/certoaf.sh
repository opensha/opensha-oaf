#! /bin/bash

# This is a script for generating and signing X.509 certificates for AAFS.

# These certificates are used with MongoDB, to enable data-in-transit encryption
# and client authentication, via TLS.

# COMMANDS FOR GENERATING CERTIFICATES
#
# gen_root_ca
#
#     Generate a root CA certificate.
#
#     The command creates a directory "cert-root-ca" within the current directory.
#     The following files are created (files marked with ** are distributed):
#       
#       logfile.txt -- Log of operations.
#       oafcert_root_ca.key -- Private root certificate key.
#       oafcert_root_ca.pem -- Certificate authority file. **
#       oafcert_root_ca_pass.txt -- Password for private root certificate key.
#
# gen_app  <dest_dir>  <auth_option>
#
#     Generate certificates which must be installed in the client.  If client
#     authentication is in effect, include an application certificate to install
#     in the client.
#
#     The current directory must contain the "cert-root-ca" directory.
#
#     <dest_dir> is the destination directory, which this command creates.
#
#     <auth_option> must be "auth" if client authentication is in effect, or "noauth"
#     if client authentication is not in effect.
#
#     The following files are created (files marked with ** are distributed):
#
#       logfile.txt -- Log of operations.
#       oafcert_root_ca.pem -- Certificate authority file. **
#
#    If client authentication is in effect, the following additional files are created:
#
#       oafcert_app.crt -- Signed certificate.
#       oafcert_app.csr -- Certificate signing request.
#       oafcert_app.key -- Private key.
#       oafcert_app.pem -- Signed certificate plus private key.
#       oafcert_app.p12 -- Signed certificate plus private key, in PKCS12 form. **
#       oafcert_app_pass.txt -- Password for private key.
#       oafcert_app_p12_pass.txt -- Password for PKCS12 file. **
#
# gen_server  <dest_dir>  <auth_option>  <addr_type>  <addr>
#
#     Generate a server certificate, which must be installed into the MongoDB server.
#     Optionally, also generate a client certificate, to be used in the server-resident
#     application and the MongoDB shell.
#
#     The current directory must contain the "cert-root-ca" directory.
#
#     <dest_dir> is the destination directory, which this command creates.
#
#     <auth_option> must be "auth" if client authentication is in effect, or "noauth"
#     if client authentication is not in effect.
#
#     <addr_type> is the type of the server address, either "ip" or "dns".
#
#     <addr> is the server address, which can be given as an IP address or DNS name.
#     This address is also used in the common name (CN) field of the certificate.
#     The generated certificate is valid for this address and for 127.0.0.1.
#
#     The following files are created (files marked with ** are distributed):
#
#       logfile.txt -- Log of operations.
#       oafcert_root_ca.pem -- Certificate authority file. **
#       oafcert_server.crt -- Signed certificate.
#       oafcert_server.csr -- Certificate signing request.
#       oafcert_server.key -- Private key.
#       oafcert_server.pem -- Signed certificate plus private key. **
#       oafcert_server_pass.txt -- Password for private key. **
#
#    If client authentication is in effect, the following additional files are created:
#
#       oafcert_app.crt -- Signed certificate.
#       oafcert_app.csr -- Certificate signing request.
#       oafcert_app.key -- Private key.
#       oafcert_app.pem -- Signed certificate plus private key. **
#       oafcert_app.p12 -- Signed certificate plus private key, in PKCS12 form. **
#       oafcert_app_pass.txt -- Password for private key. **
#       oafcert_app_p12_pass.txt -- Password for PKCS12 file. **




#----- Distinguished Name (DN) -----
#
# These are the values used by the OAF team in USGS.
# They should be changed if this script is used for any other purpose.




# C = Country (standard 2-letter code)

dn_c="US"

# ST = State or province (full name, no abbreviations)

dn_st="California"

# L = Locality (city)

dn_l="Moffett Field"

# O = Organization

dn_o="USGS"

# OU = Orgizational unit

dn_ou="OAF"

# Prefix combining all the above

dn_prefix="/C=${dn_c}/ST=${dn_st}/L=${dn_l}/O=${dn_o}/OU=${dn_ou}"

# CN = Common name for root CA certificate

dn_cn_root_ca="OAF-ROOT"

# CN = Common name for server-resident client

dn_cn_client="OAF-CLIENT"

# CN = Common name for desktop application (GUI)

dn_cn_app="OAF-APP"




#----- Certificate lifetimes -----



# Lifetime of root CA certificate, in days

days_root_ca="30000"

# Lifetime of server certificate, in days

days_server="30000"

# Lifetime of server-resident client certificate, in days

days_client="30000"

# Lifetime of desktop app certificate, in days

days_app="30000"




#----- Filenames -----




# Directory for root CA (certificate authority) certificate

dir_root_ca="cert-root-ca"

# Filename for root CA (certificate authority) certificate

file_root_ca="oafcert_root_ca"

# Filename for server certificate

file_server="oafcert_server"

# Filename for server-resident client certificate (currently same as for desktop app)

file_client="oafcert_app"

# Filename for desktop application certificate

file_app="oafcert_app"

# Suffix for password files

suffix_pass="_pass.txt"

# Suffix for PKCS12 password files

suffix_p12_pass="_p12_pass.txt"

# Log filename

file_log="logfile.txt"




#----- Functions -----




# Begin the log file.
# $1 = Title.

q_begin_log () {
    echo `date -u "+%Y-%m-%d %H:%M:%S (UTC) :"` "$1" > "${file_log}"
}




# Skip a line in the log file

q_skip_log () {
    echo >> "${file_log}"
}




# Append a line to the log file
# $1 = Text.

q_append_log () {
    echo "$1" >> "${file_log}"
}




# Generate the root CA certificate.
# Exit the script if already exists.

q_gen_root_ca () {

    # Prevent overwrite of existing directory

    if [ -d "${dir_root_ca}" ]; then
        echo "Root certificate directory already exists : ${dir_root_ca}"
        exit 1
    fi

    # Make the directory and change to it

    mkdir "${dir_root_ca}"

    if [ ! -d "${dir_root_ca}" ]; then
        echo "Cannot create root certificate directory : ${dir_root_ca}"
        exit 1
    fi

    cd "${dir_root_ca}"

    # Begin the log file

    q_begin_log "Generate root CA certificate"

    # Create a password file

    q_skip_log
    q_append_log "*** Create password file : ${file_root_ca}${suffix_pass}"
    q_skip_log
    echo 'openssl rand -hex 32 > '"${file_root_ca}${suffix_pass}" >> "${file_log}"

    openssl rand -hex 32 > "${file_root_ca}${suffix_pass}"

    q_skip_log
    cat "${file_root_ca}${suffix_pass}" >> "${file_log}"

    # Create the private key

    q_skip_log
    q_append_log "*** Create private key : ${file_root_ca}.key"
    q_skip_log
    echo 'openssl genrsa -aes256 -out '"${file_root_ca}.key"' -passout '"file:${file_root_ca}${suffix_pass}"' 2048' >> "${file_log}"

    openssl genrsa -aes256 -out "${file_root_ca}.key" -passout "file:${file_root_ca}${suffix_pass}" 2048

    # Display the private key

    q_skip_log
    echo 'openssl rsa -in '"${file_root_ca}.key"' -passin '"file:${file_root_ca}${suffix_pass}"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl rsa -in "${file_root_ca}.key" -passin "file:${file_root_ca}${suffix_pass}" -text -noout >> "${file_log}"

    # Self-sign the certificate
    # Note: The "-nodes" option is deprecated as of OpenSSL 3.0; it can be replaced with "-noenc"; it may be unnecessary.
    # Note: The "-noenc" option is not supported on OpenSSL 1.0, so for now we avoid using it.
    # Note: Some sources suggest adding "-extensions v3_ca"; however this appears to be the default.

    q_skip_log
    q_append_log "*** Self-sign certificate : ${file_root_ca}.pem"
    q_skip_log
    echo 'openssl req -x509 -new -nodes -key '"${file_root_ca}.key"' -passin '"file:${file_root_ca}${suffix_pass}"' -sha256 -days '"${days_root_ca}"' -subj "'"${dn_prefix}/CN=${dn_cn_root_ca}"'" -out '"${file_root_ca}.pem" >> "${file_log}"

    openssl req -x509 -new -nodes -key "${file_root_ca}.key" -passin "file:${file_root_ca}${suffix_pass}" -sha256 -days "${days_root_ca}" -subj "${dn_prefix}/CN=${dn_cn_root_ca}" -out "${file_root_ca}.pem"

    # Display the certificate

    q_skip_log
    echo 'openssl x509 -in '"${file_root_ca}.pem"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl x509 -in "${file_root_ca}.pem" -text -noout >> "${file_log}"

    # List the directory

    q_skip_log
    q_append_log "*** List directory"
    q_skip_log
    echo 'ls -l' >> "${file_log}"

    q_skip_log
    ls -l >> "${file_log}"

    # Return to original directory

    cd ..

}




# Make a signed application certificate.
# $1 = Directory containing root certificate.
# $2 = Certificate lifetime, in days.
# $3 = Destination base filename.
# $4 = Common name (CN).
# Assumes the current directory is the destination, and the log file is already started.

q_make_app_cert () {

    myfile="$3"

    if [ ! -d "$1" ]; then
        echo "Cannot find root certificate directory : $1"
        exit 1
    fi

    if [ ! -f "$1/${file_root_ca}.pem" ]; then
        echo "Cannot find root certificate pem file : $1/${file_root_ca}.pem"
        exit 1
    fi

    if [ ! -f "$1/${file_root_ca}.key" ]; then
        echo "Cannot find root certificate private key file : $1/${file_root_ca}.key"
        exit 1
    fi

    if [ ! -f "$1/${file_root_ca}${suffix_pass}" ]; then
        echo "Cannot find root certificate private key password file : $1/${file_root_ca}${suffix_pass}"
        exit 1
    fi

    # Create a password file

    q_skip_log
    q_append_log "*** Create password file : ${myfile}${suffix_pass}"
    q_skip_log
    echo 'openssl rand -hex 10 > '"${myfile}${suffix_pass}" >> "${file_log}"

    openssl rand -hex 10 > "${myfile}${suffix_pass}"

    q_skip_log
    cat "${myfile}${suffix_pass}" >> "${file_log}"

    # Create a PKCS12 password file

    q_skip_log
    q_append_log "*** Create PKCS12 password file : ${myfile}${suffix_p12_pass}"
    q_skip_log
    echo 'p=""; for (( i=0 ; i<10 ; i++ )); do p="$p$(($SRANDOM%10))"; done; echo "$p" > '"${myfile}${suffix_p12_pass}" >> "${file_log}"

    p=""; for (( i=0 ; i<10 ; i++ )); do p="$p$(($SRANDOM%10))"; done; echo "$p" > "${myfile}${suffix_p12_pass}"

    q_skip_log
    cat "${myfile}${suffix_p12_pass}" >> "${file_log}"

    # Create the private key

    q_skip_log
    q_append_log "*** Create private key : ${myfile}.key"
    q_skip_log
    echo 'openssl genrsa -aes256 -out '"${myfile}.key"' -passout '"file:${myfile}${suffix_pass}"' 2048' >> "${file_log}"

    openssl genrsa -aes256 -out "${myfile}.key" -passout "file:${myfile}${suffix_pass}" 2048

    # Display the private key

    q_skip_log
    echo 'openssl rsa -in '"${myfile}.key"' -passin '"file:${myfile}${suffix_pass}"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl rsa -in "${myfile}.key" -passin "file:${myfile}${suffix_pass}" -text -noout >> "${file_log}"

    # Create the certificate signing request

    q_skip_log
    q_append_log "*** Create certificate signing request : ${myfile}.csr"
    q_skip_log
    echo 'openssl req -new -key '"${myfile}.key"' -passin '"file:${myfile}${suffix_pass}"' -subj "'"${dn_prefix}/CN=$4"'" -out '"${myfile}.csr" >> "${file_log}"

    openssl req -new -key "${myfile}.key" -passin "file:${myfile}${suffix_pass}" -subj "${dn_prefix}/CN=$4" -out "${myfile}.csr"

    # Display the certificate signing request

    q_skip_log
    echo 'openssl req -in '"${myfile}.csr"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl req -in "${myfile}.csr" -text -noout >> "${file_log}"

    # Sign the certificate
    # Note: Some sources suggest adding "-CAcreateserial", which creates a serial file "$1/${file_root_ca}.srl" that is
    # initialized with a large random number.  Each time a certificate is signed, the number is incremented and written
    # back into the file, and the new value is used as the serial number of the signed certificate.  If there is no
    # serial file, then each signed certificate is given a random serial number; this is the recommended practice.

    q_skip_log
    q_append_log "*** Sign certificate : ${myfile}.crt"
    q_skip_log
    echo 'openssl x509 -req -in '"${myfile}.csr"' -CA '"${file_root_ca}.pem"' -CAkey '"${file_root_ca}.key"' -passin '"file:${file_root_ca}${suffix_pass}"' -out '"${myfile}.crt"' -days '"$2"' -sha256' >> "${file_log}"

    q_skip_log

    openssl x509 -req -in "${myfile}.csr" -CA "$1/${file_root_ca}.pem" -CAkey "$1/${file_root_ca}.key" -passin "file:$1/${file_root_ca}${suffix_pass}" -out "${myfile}.crt" -days "$2" -sha256 >> "${file_log}" 2>&1

    # Display the signed certificate

    q_skip_log
    echo 'openssl x509 -in '"${myfile}.crt"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl x509 -in "${myfile}.crt" -text -noout >> "${file_log}"

    # Create the pem file

    q_skip_log
    q_append_log "*** Create pem file : ${myfile}.pem"
    q_skip_log
    echo 'cat '"${myfile}.key"' '"${myfile}.crt"' > '"${myfile}.pem" >> "${file_log}"

    cat "${myfile}.key" "${myfile}.crt" > "${myfile}.pem"

    # Display the pem file

    q_skip_log
    echo 'openssl x509 -in '"${myfile}.pem"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl x509 -in "${myfile}.pem" -text -noout >> "${file_log}"

    # Create the PKCS12 file

    q_skip_log
    q_append_log "*** Create PKCS12 file : ${myfile}.p12"
    q_skip_log
    echo 'openssl pkcs12 -export -in '"${myfile}.pem"' -passin '"file:${myfile}${suffix_pass}"' -out '"${myfile}.p12"' -passout '"file:${myfile}${suffix_p12_pass}" >> "${file_log}"

    openssl pkcs12 -export -in "${myfile}.pem" -passin "file:${myfile}${suffix_pass}" -out "${myfile}.p12" -passout "file:${myfile}${suffix_p12_pass}"

    q_skip_log
    echo 'chmod +r '"${myfile}.p12" >> "${file_log}"

    chmod +r "${myfile}.p12"

    # Display the PKCS12 file

    q_skip_log
    echo 'openssl pkcs12 -nokeys -info -in '"${myfile}.p12"' -passin '"file:${myfile}${suffix_p12_pass}" >> "${file_log}"
    q_skip_log

    openssl pkcs12 -nokeys -info -in "${myfile}.p12" -passin "file:${myfile}${suffix_p12_pass}" >> "${file_log}" 2>&1

}




# Generate desktop application certificates.
# $1 = Destination directory (which is created by this command).
# $2 = Client authentication option, "auth" or "noauth".

q_gen_app () {

    # Prevent overwrite of existing directory

    if [ -d "$1" ]; then
        echo "Destination directory already exists : $1"
        exit 1
    fi

    # Validate authentication option

    if [ "$2" == "auth" ]; then
        :
    elif [ "$2" == "noauth" ]; then
        :
    else
        echo "Invalid client authentication option : $2"
        exit 1
    fi

    # Check existence of root CA certificate

    rcadir="$(pwd)/${dir_root_ca}"

    if [ ! -d "${rcadir}" ]; then
        echo "Cannot find root certificate directory : ${rcadir}"
        exit 1
    fi

    if [ ! -f "${rcadir}/${file_root_ca}.pem" ]; then
        echo "Cannot find root certificate pem file : ${rcadir}/${file_root_ca}.pem"
        exit 1
    fi

    if [ ! -f "${rcadir}/${file_root_ca}.key" ]; then
        echo "Cannot find root certificate private key file : ${rcadir}/${file_root_ca}.key"
        exit 1
    fi

    if [ ! -f "${rcadir}/${file_root_ca}${suffix_pass}" ]; then
        echo "Cannot find root certificate private key password file : ${rcadir}/${file_root_ca}${suffix_pass}"
        exit 1
    fi

    # Make the directory and change to it

    mkdir "$1"

    if [ ! -d "$1" ]; then
        echo "Cannot create destination directory : $1"
        exit 1
    fi

    cd "$1"

    # Begin the log file

    q_begin_log "Generate desktop application certificates"

    # Copy the root CA certificate

    q_skip_log
    q_append_log "*** Copy root CA certificate : ${file_root_ca}.pem"
    q_skip_log
    echo 'cp -pi "'"${rcadir}/${file_root_ca}.pem"'" .' >> "${file_log}"

    cp -pi "${rcadir}/${file_root_ca}.pem" .

    # Create the certificates, if client authentication is selected

    if [ "$2" == "auth" ]; then
        q_make_app_cert "${rcadir}" "${days_app}" "${file_app}" "${dn_cn_app}"
    fi

    # List the directory

    q_skip_log
    q_append_log "*** List directory"
    q_skip_log
    echo 'ls -l' >> "${file_log}"

    q_skip_log
    ls -l >> "${file_log}"

    # Return to original directory

    cd - >/dev/null

}




# Make a signed server certificate.
# $1 = Directory containing root certificate.
# $2 = Certificate lifetime, in days.
# $3 = Destination base filename.
# $4 = Server address type, "ip" or "dns".
# $5 = Server IP address or DNS name, also used as the common name.
# Assumes the current directory is the destination, and the log file is already started.

q_make_server_cert () {

    myfile="$3"

    if [ ! -d "$1" ]; then
        echo "Cannot find root certificate directory : $1"
        exit 1
    fi

    if [ ! -f "$1/${file_root_ca}.pem" ]; then
        echo "Cannot find root certificate pem file : $1/${file_root_ca}.pem"
        exit 1
    fi

    if [ ! -f "$1/${file_root_ca}.key" ]; then
        echo "Cannot find root certificate private key file : $1/${file_root_ca}.key"
        exit 1
    fi

    if [ ! -f "$1/${file_root_ca}${suffix_pass}" ]; then
        echo "Cannot find root certificate private key password file : $1/${file_root_ca}${suffix_pass}"
        exit 1
    fi

    # Construct the subject alternate name

    if [ "$4" == "ip" ]; then
        if [ "$5" == "127.0.0.1" ]; then
            mysan="IP:127.0.0.1"
        else
            mysan="IP:$5,IP:127.0.0.1"
        fi
    elif [ "$4" == "dns" ]; then
        mysan="DNS:$5,IP:127.0.0.1"
    else
        echo "Invalid server address type : $4"
        exit 1
    fi

    # Create a password file

    q_skip_log
    q_append_log "*** Create password file : ${myfile}${suffix_pass}"
    q_skip_log
    echo 'openssl rand -hex 10 > '"${myfile}${suffix_pass}" >> "${file_log}"

    openssl rand -hex 10 > "${myfile}${suffix_pass}"

    q_skip_log
    cat "${myfile}${suffix_pass}" >> "${file_log}"

    # Create the private key

    q_skip_log
    q_append_log "*** Create private key : ${myfile}.key"
    q_skip_log
    echo 'openssl genrsa -aes256 -out '"${myfile}.key"' -passout '"file:${myfile}${suffix_pass}"' 2048' >> "${file_log}"

    openssl genrsa -aes256 -out "${myfile}.key" -passout "file:${myfile}${suffix_pass}" 2048

    # Display the private key

    q_skip_log
    echo 'openssl rsa -in '"${myfile}.key"' -passin '"file:${myfile}${suffix_pass}"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl rsa -in "${myfile}.key" -passin "file:${myfile}${suffix_pass}" -text -noout >> "${file_log}"

    # Create the certificate signing request

    q_skip_log
    q_append_log "*** Create certificate signing request : ${myfile}.csr"
    q_skip_log
    echo 'openssl req -new -key '"${myfile}.key"' -passin '"file:${myfile}${suffix_pass}"' -subj "'"${dn_prefix}/CN=$5"'" -reqexts SAN -config <(cat /etc/ssl/openssl.cnf <(printf "'"\n[SAN]\nsubjectAltName=${mysan}"'")) -out '"${myfile}.csr" >> "${file_log}"

    openssl req -new -key "${myfile}.key" -passin "file:${myfile}${suffix_pass}" -subj "${dn_prefix}/CN=$5" -reqexts SAN -config <(cat /etc/ssl/openssl.cnf <(printf "\n[SAN]\nsubjectAltName=${mysan}")) -out "${myfile}.csr"

    # Display the certificate signing request

    q_skip_log
    echo 'openssl req -in '"${myfile}.csr"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl req -in "${myfile}.csr" -text -noout >> "${file_log}"

    # Sign the certificate
    # Note: Some sources suggest adding "-CAcreateserial", which creates a serial file "$1/${file_root_ca}.srl" that is
    # initialized with a large random number.  Each time a certificate is signed, the number is incremented and written
    # back into the file, and the new value is used as the serial number of the signed certificate.  If there is no
    # serial file, then each signed certificate is given a random serial number; this is the recommended practice.

    q_skip_log
    q_append_log "*** Sign certificate : ${myfile}.crt"
    q_skip_log
    echo 'openssl x509 -req -in '"${myfile}.csr"' -CA '"${file_root_ca}.pem"' -CAkey '"${file_root_ca}.key"' -passin '"file:${file_root_ca}${suffix_pass}"' -copy_extensions copyall -out '"${myfile}.crt"' -days '"$2"' -sha256' >> "${file_log}"

    q_skip_log

    openssl x509 -req -in "${myfile}.csr" -CA "$1/${file_root_ca}.pem" -CAkey "$1/${file_root_ca}.key" -passin "file:$1/${file_root_ca}${suffix_pass}" -copy_extensions copyall -out "${myfile}.crt" -days "$2" -sha256 >> "${file_log}" 2>&1

    # Display the signed certificate

    q_skip_log
    echo 'openssl x509 -in '"${myfile}.crt"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl x509 -in "${myfile}.crt" -text -noout >> "${file_log}"

    # Create the pem file

    q_skip_log
    q_append_log "*** Create pem file : ${myfile}.pem"
    q_skip_log
    echo 'cat '"${myfile}.key"' '"${myfile}.crt"' > '"${myfile}.pem" >> "${file_log}"

    cat "${myfile}.key" "${myfile}.crt" > "${myfile}.pem"

    # Display the pem file

    q_skip_log
    echo 'openssl x509 -in '"${myfile}.pem"' -text -noout' >> "${file_log}"
    q_skip_log

    openssl x509 -in "${myfile}.pem" -text -noout >> "${file_log}"

}




# Generate server certificates.
# $1 = Destination directory (which is created by this command).
# $2 = Client authentication option, "auth" or "noauth".
# $3 = Server address type, "ip" or "dns".
# $4 = Server IP address or DNS name, also used as the common name.

q_gen_server () {

    # Prevent overwrite of existing directory

    if [ -d "$1" ]; then
        echo "Destination directory already exists : $1"
        exit 1
    fi

    # Validate authentication option

    if [ "$2" == "auth" ]; then
        :
    elif [ "$2" == "noauth" ]; then
        :
    else
        echo "Invalid client authentication option : $2"
        exit 1
    fi

    # Validate server address type

    if [ "$3" == "ip" ]; then
        :
    elif [ "$3" == "dns" ]; then
        :
    else
        echo "Invalid server address type : $3"
        exit 1
    fi

    # Check existence of root CA certificate

    rcadir="$(pwd)/${dir_root_ca}"

    if [ ! -d "${rcadir}" ]; then
        echo "Cannot find root certificate directory : ${rcadir}"
        exit 1
    fi

    if [ ! -f "${rcadir}/${file_root_ca}.pem" ]; then
        echo "Cannot find root certificate pem file : ${rcadir}/${file_root_ca}.pem"
        exit 1
    fi

    if [ ! -f "${rcadir}/${file_root_ca}.key" ]; then
        echo "Cannot find root certificate private key file : ${rcadir}/${file_root_ca}.key"
        exit 1
    fi

    if [ ! -f "${rcadir}/${file_root_ca}${suffix_pass}" ]; then
        echo "Cannot find root certificate private key password file : ${rcadir}/${file_root_ca}${suffix_pass}"
        exit 1
    fi

    # Make the directory and change to it

    mkdir "$1"

    if [ ! -d "$1" ]; then
        echo "Cannot create destination directory : $1"
        exit 1
    fi

    cd "$1"

    # Begin the log file

    q_begin_log "Generate server certificates : $2 $3 $4"

    # Copy the root CA certificate

    q_skip_log
    q_append_log "*** Copy root CA certificate : ${file_root_ca}.pem"
    q_skip_log
    echo 'cp -pi "'"${rcadir}/${file_root_ca}.pem"'" .' >> "${file_log}"

    cp -pi "${rcadir}/${file_root_ca}.pem" .

    # Create the server certificates

    q_make_server_cert "${rcadir}" "${days_server}" "${file_server}" "$3" "$4"

    # Create the client certificates, if client authentication is selected

    if [ "$2" == "auth" ]; then
        q_make_app_cert "${rcadir}" "${days_client}" "${file_client}" "${dn_cn_client}"
    fi

    # List the directory

    q_skip_log
    q_append_log "*** List directory"
    q_skip_log
    echo 'ls -l' >> "${file_log}"

    q_skip_log
    ls -l >> "${file_log}"

    # Return to original directory

    cd - >/dev/null

}




#----- Main function -----




case "$1" in

    test_display_defs)

        echo ""
        echo "Distinguished name:"
        echo "dn_c = $dn_c"
        echo "dn_st = $dn_st"
        echo "dn_l = $dn_l"
        echo "dn_o = $dn_o"
        echo "dn_ou = $dn_ou"
        echo "dn_prefix = $dn_prefix"
        echo "dn_cn_root_ca = $dn_cn_root_ca"
        echo "dn_cn_client = $dn_cn_client"
        echo "dn_cn_app = $dn_cn_app"

        echo ""
        echo "Certificate lifetimes:"
        echo "days_root_ca = $days_root_ca"
        echo "days_server = $days_server"
        echo "days_client = $days_client"
        echo "days_app = $days_app"

        echo ""
        echo "Filenames:"
        echo "dir_root_ca = $dir_root_ca"
        echo "file_root_ca = $file_root_ca"
        echo "file_server = $file_server"
        echo "file_client = $file_client"
        echo "file_app = $file_app"
        echo "suffix_pass = $suffix_pass"
        echo "suffix_p12_pass = $suffix_p12_pass"
        echo "file_log = $file_log"
        ;;




    # Generate the root CA certificate.

    gen_root_ca)
        q_gen_root_ca
        echo ""
        echo "********************"
        echo ""
        echo "Generated root CA certificate in : ${dir_root_ca}"
        ;;




    # Generate the desktop application certificates.
    # $2 = Destination directory (which is created by this command).
    # $3 = Client authentication option, "auth" or "noauth".

    gen_app)
        q_gen_app "$2" "$3"
        echo ""
        echo "********************"
        echo ""
        echo "Generated desktop application certificates in : $2"
        ;;




    # Generate the server certificates.
    # $2 = Destination directory (which is created by this command).
    # $3 = Client authentication option, "auth" or "noauth".
    # $4 = Server address type, "ip" or "dns".
    # $5 = Server IP address or DNS name, also used as the common name.

    gen_server)
        q_gen_server "$2" "$3" "$4" "$5"
        echo ""
        echo "********************"
        echo ""
        echo "Generated server certificates in : $2"
        ;;




    help)
        echo "Generate root CA certificate:"
        echo "  certoaf.sh gen_root_ca"
        echo "Generate desktop application certificates:"
        echo "  certoaf.sh gen_app dest_dir auth_option"
        echo "Generate server certificates:"
        echo "  certoaf.sh gen_server dest_dir auth_option addr_type addr"
        ;;




    *)
        echo "Usage: 'certoaf.sh help' to display help."
        exit 1
        ;;
esac

exit 0

