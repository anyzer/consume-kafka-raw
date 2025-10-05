#!/usr/bin/env bash
set -e

# Read the config
source ./PROJECT_CONFIG

DEFAULT_AWS_ECR_ACCOUNT_ID=992962979097
DEFAULT_AWS_REGION=ap-southeast-2
DEFAULT_STAGE_DIR=target/universal/stage
DEFAULT_JAVA_11_HOME=/godata/java-11-amazon-corretto.x86_64

AWS_ECR_ACCOUNT_ID=${AWS_ECR_ACCOUNT_ID:-$DEFAULT_AWS_ECR_ACCOUNT_ID}
AWS_REGION=${AWS_REGION:-$DEFAULT_AWS_REGION}
STAGE_DIR=${STAGE_DIR:-$DEFAULT_STAGE_DIR}
BUILD_JAVA_HOME=${BUILD_JAVA_11_HOME:-$DEFAULT_JAVA_11_HOME}

# Build the application. Pass appname as a parameter to sbt
#sbt -Dappname=$COMPONENT_NAME coverage test coverageReport

# Build the application. Pass appname as a parameter to sbt
env JAVA_OPTS="-Xss1g -XX:MaxMetaspaceSize=1024m" sbt -java-home "$BUILD_JAVA_HOME" -Dappname=$COMPONENT_NAME clean universal:stage

STAGE_DIR=target/universal/stage

# Create the 'code' directory that holds our application code if doesn't exist already
[ -d $STAGE_DIR/code ] || mkdir $STAGE_DIR/code

# Move our application jar from 'lib' folder to 'code' folder. We do this as we create two layers in docker. The first
# layer holds all the dependency jars and the second layer holds only our application jar. This is an optimization so
# that we don't have to copy all the dependency jars every time our source code changes.
mv $STAGE_DIR/lib/*$COMPONENT_NAME*.jar $STAGE_DIR/code/

#echo "old"
#aws --version
#sudo apt remove awscli
#sleep 60
#echo "new"
#aws --version
#
#echo "install"
#curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
#unzip awscliv2.zip
#./aws/install --bin-dir /usr/local/bin --install-dir /usr/local/aws-cli --update
#aws --version

$(aws ecr get-login --region ap-southeast-2 --no-include-email)
#aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ECR_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
docker build -t $COMPONENT_NAME .
docker tag $COMPONENT_NAME:latest $AWS_ECR_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$COMPONENT_NAME:latest
echo "Built image $COMPONENT_NAME:latest"