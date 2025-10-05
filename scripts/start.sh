#!/bin/bash

# added from bootstrap
set -e
source /scripts/kafka-env.sh
/scripts/retrieve-keytab.sh
/scripts/check-kerberos-connectivity.sh

source ./PROJECT_CONFIG

check_error() {
    if [ $1 -ne 0 ] ; then
        echo "Error: Fatal error has been encountered!"
        exit 1
    fi
}

get_aws_parameter() {
  parameter=$1
  aws_response=$(aws ssm get-parameters --names $parameter --region ap-southeast-2 --with-decryption)
  check_error $?
  value=$(echo $aws_response | grep -Po '\s*"Value"\s*:\s*"\K([^"]*)')
  echo $value
}

exec $APP_ROOT/bin/main $KAFKA_ENVIRONMENT