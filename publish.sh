#!/usr/bin/env bash

source ./PROJECT_CONFIG

AWS_REGION=ap-southeast-2

DEFAULT_ENVIRONMENT=dev
DEFAULT_RELEASE_TYPE=SNAPSHOT
DEFAULT_PUBLISH_AWS_ECR=true
DEFAULT_PUBLISH_ARTIFACTORY_ECR=false
DEFAULT_AWS_ECR_ACCOUNT_ID=992962979097
DEFAULT_GO_PIPELINE_COUNTER=local
DEFAULT_DRY_RUN=false

ENVIRONMENT=${ENVIRONMENT:-$DEFAULT_ENVIRONMENT}                                # If variable not set or null, use default
PUBLISH_AWS_ECR=${PUBLISH_AWS_ECR:-$DEFAULT_PUBLISH_AWS_ECR}                    # If variable not set or null, use default
PUBLISH_ARTIFACTORY=${PUBLISH_ARTIFACTORY:-$DEFAULT_PUBLISH_ARTIFACTORY_ECR}    # If variable not set or null, use default
AWS_ECR_ACCOUNT_ID=${AWS_ECR_ACCOUNT_ID:-$DEFAULT_AWS_ECR_ACCOUNT_ID}           # If variable not set or null, use default
RELEASE_TYPE=${RELEASE_TYPE:-$DEFAULT_RELEASE_TYPE}           # If variable not set or null, use default
GO_PIPELINE_COUNTER=${GO_PIPELINE_COUNTER:-$DEFAULT_GO_PIPELINE_COUNTER}           # If variable not set or null, use default
DRY_RUN=${DRY_RUN:-$DEFAULT_DRY_RUN}           # If variable not set or null, use default

ARTIFACTORY_HOST="artifacts.tabdigital.com.au"
ARTIFACTORY_PROJECT_REPO="analytics-docker-${ENVIRONMENT}"
ARTIFACTORY_REPO_HOST=${ARTIFACTORY_PROJECT_REPO}.$ARTIFACTORY_HOST

AWS_ECR_REPO_HOST="$AWS_ECR_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

ARTIFACTORY_SSM_PUBLISH_USER="/resource/artifactory/write/username"
ARTIFACTORY_SSM_PUBLISH_TOKEN="/resource/artifactory/write/token"

REV=""
REV=$(git rev-parse --short HEAD)
echo "-- Revision: $REV"
echo "-- GO_PIPELINE_COUNTER: $GO_PIPELINE_COUNTER"
echo "-- RELEASE_TYPE: $RELEASE_TYPE"
echo "-- BUILD_VERSION $BUILD_VERSION"

BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)
#BRANCH_NAME=master
BRANCH_NAME_TAG=${BRANCH_NAME//[^a-zA-Z0-9_.]/-}

#TODO: if branch is not /develop or /release or master - force the branch name in the tag??
if [ "$RELEASE_TYPE" == "RELEASE" ] && [ "$GO_PIPELINE_COUNTER" != "local" ]
then
    echo "-- ***** Building RELEASE ****"
    STD_DOCKER_TAG=v$BUILD_VERSION-$GO_PIPELINE_COUNTER-$REV-$RELEASE_TYPE
    BUILD_VERSION_RELEASE_TYPE=v$BUILD_VERSION-$RELEASE_TYPE
else
    echo "-- ***** Building SNAPSHOT **** as RELEASE_TYPE != RELEASE and GO_PIPELINE_COUNTER != local"
    STD_DOCKER_TAG=v$BUILD_VERSION-$GO_PIPELINE_COUNTER-$REV-$BRANCH_NAME_TAG
    BUILD_VERSION_RELEASE_TYPE=v$BUILD_VERSION-latest
fi
STD_DOCKER_TAG=${STD_DOCKER_TAG//[^a-zA-Z0-9_.]/-}

echo "-- BRANCH_NAME_TAG $BRANCH_NAME_TAG"
echo "-- STD_DOCKER_TAG $STD_DOCKER_TAG"
echo "-- BUILD_VERSION_RELEASE_TYPE $BUILD_VERSION_RELEASE_TYPE"

LOGIN_AWS_ECR_STATUS=""
LOGIN_ARTIFACTORY_STATUS=""

login_artifactory() {
    echo "-- Login Artifactory Repository"
    local exit_code=0
    publish_user=$(aws ssm get-parameter --name "${ARTIFACTORY_SSM_PUBLISH_USER}" --region ${AWS_REGION} --query 'Parameter.Value' --output text)
    exit_code=$? && [ $exit_code -ne 0 ] && return $exit_code
    publish_token=$(aws ssm get-parameter --name "${ARTIFACTORY_SSM_PUBLISH_TOKEN}" --with-decryption --region ${AWS_REGION} --query 'Parameter.Value' --output text)
    exit_code=$? && [ $exit_code -ne 0 ] && return $exit_code
    echo ${publish_token} | docker login --username "${publish_user}" --password-stdin ${ARTIFACTORY_DEV_REPO_HOST}
    exit_code=$?
    return $exit_code
}

login_aws_ecr() {
    echo "-- Login AWS ECR Repository"
    local exit_code=0
    $(aws ecr get-login --region ap-southeast-2 --no-include-email)
    exit_code=$?
    return $exit_code
}

check_error() {
    if [ $1 -ne 0 ] ; then
        echo "Error: Fatal error has been encountered!"
        exit 1
    fi
}

publish_image_to_repository() {
    repository=$1
    repository_image="${repository}/${COMPONENT_NAME}"

    docker tag $repository_image:latest $repository_image:$REV
    check_error $?

    docker tag $repository_image:latest $repository_image:$STD_DOCKER_TAG
        check_error $?
    echo "Pushing image to $repository_image:$STD_DOCKER_TAG"
    if [ "$DRY_RUN" = "false" ]; then docker push $repository_image:$STD_DOCKER_TAG; fi
        check_error $?

    echo "Pushing image to $repository_image:$REV"
    if [ "$DRY_RUN" = "false" ]; then docker push $repository_image:$REV; fi
    check_error $?

    docker tag $repository_image:latest $repository_image:$BUILD_VERSION_RELEASE_TYPE
    check_error $?
    echo "Pushing image to $repository_image:$BUILD_VERSION_RELEASE_TYPE"
    if [ "$DRY_RUN" = "false" ]; then docker push $repository_image:$BUILD_VERSION_RELEASE_TYPE; fi
    check_error $?

    echo "-- Successfully published using tag: $STD_DOCKER_TAG"
    echo "-- Successfully published using tag: $BUILD_VERSION_RELEASE_TYPE"
    echo "-- Successfully published using tag: $REV"

}

#
# Main
#

echo "-- Publish to Repository [$COMPONENT_NAME | $ENVIRONMENT]"

exit_code=0

if [ "$PUBLISH_ARTIFACTORY" = true ] ; then
    echo "-- Publish to Artifactory : $ARTIFACTORY_REPO_HOST"
    login_artifactory
    exit_code=$?
    if [ $exit_code -eq 0 ]; then
        publish_image_to_repository "$ARTIFACTORY_REPO_HOST"
        LOGIN_ARTIFACTORY_STATUS="OK"
    else
        echo "-- Skipping Publishing. Login failed!"
        LOGIN_ARTIFACTORY_STATUS="FAILED"
    fi
fi

if [ "$PUBLISH_AWS_ECR" = true ] ; then
    echo "-- Publish to AWS ECR : $AWS_ECR_REPO_HOST"
    login_aws_ecr
    exit_code=$?
    if [ $exit_code -eq 0 ]; then
        publish_image_to_repository "$AWS_ECR_REPO_HOST"
        LOGIN_AWS_ECR_STATUS="OK"
    else
        echo "-- Skipping Publishing. Login failed!"
        LOGIN_AWS_ECR_STATUS="FAILED"
    fi
fi

[ "$PUBLISH_ARTIFACTORY" = true ] && echo "-- Publish to Artifactory Repository: $ARTIFACTORY_REPO_HOST [$LOGIN_ARTIFACTORY_STATUS]"
[ "$PUBLISH_AWS_ECR" = true ]     && echo "-- Publish to AWS ECR Repository: $AWS_ECR_REPO_HOST [$LOGIN_AWS_ECR_STATUS]"

echo "-- Completed"