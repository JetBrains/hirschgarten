#!/bin/bash

# usage:
#   ./script.sh <folder_name> [private] [space_git_credentials] [tag]
#
# if [tag] is provided, it overrides building/pushing date/latest tags

# check if folder name is provided
if [ $# -lt 1 ]; then
  echo "usage: $0 <folder_name> [private] [space_git_credentials] [tag]"
  exit 1
fi

FOLDER_NAME=$1
DATE_TAG=$(date +%d%m%y)
PRIVATE=false
SPACE_GIT_CREDENTIALS=""
CUSTOM_TAG=""

# check if 'private' flag is passed
if [[ "$2" == "private" ]]; then
  PRIVATE=true
  REGISTRY="registry.jetbrains.team/p/bazel/docker-private"
  # if space_git_credentials is provided
  if [ $# -ge 3 ]; then
    SPACE_GIT_CREDENTIALS=$3
  fi
  # if a custom tag is provided as the 4th arg
  if [ $# -eq 4 ]; then
    CUSTOM_TAG=$4
  fi
else
  REGISTRY="registry.jetbrains.team/p/bazel/docker"
  # if a custom tag is provided as the 3rd arg (no 'private' arg used)
  if [ $# -eq 3 ]; then
    CUSTOM_TAG=$3
  fi
fi

IMAGE_NAME="hirschgarten-$FOLDER_NAME"
FULL_IMAGE_NAME="$REGISTRY/$IMAGE_NAME"

# function to check login
check_login() {
  docker login "$REGISTRY" -u dummy -p dummy &>/dev/null
  return $?
}

# function to handle login
handle_login() {
  echo "authentication required. please log in:"
  docker login "$REGISTRY"
  if [ $? -ne 0 ]; then
    echo "login failed. exiting."
    exit 1
  fi
}

# function to build docker image
build_image() {
  local build_command="docker build --progress=plain"

  if $PRIVATE && [ -n "$SPACE_GIT_CREDENTIALS" ]; then
    build_command+=" --no-cache --secret id=space_git_credentials,src=<(echo \"$SPACE_GIT_CREDENTIALS\")"
  fi

  if [ -n "$CUSTOM_TAG" ]; then
    build_command+=" -t $FULL_IMAGE_NAME:$CUSTOM_TAG ."
  else
    build_command+=" -t $FULL_IMAGE_NAME:$DATE_TAG ."
  fi

  if ! eval "$build_command"; then
    echo "docker build failed. exiting."
    exit 1
  fi

  # only tag as latest if custom tag wasn't specified
  if [ -z "$CUSTOM_TAG" ]; then
    docker tag "$FULL_IMAGE_NAME:$DATE_TAG" "$FULL_IMAGE_NAME:latest"
  fi
}

# function to push image
push_image() {
  local tag=$1
  if ! docker push "$FULL_IMAGE_NAME:$tag"; then
    echo "failed to push $FULL_IMAGE_NAME:$tag, re-authenticating..."
    handle_login
    if ! docker push "$FULL_IMAGE_NAME:$tag"; then
      echo "push failed again, exiting."
      exit 1
    fi
  fi
}

# main
cd "$(dirname "$0")/$FOLDER_NAME" || exit 1

if ! check_login; then
  handle_login
fi

build_image

if [ -n "$CUSTOM_TAG" ]; then
  push_image "$CUSTOM_TAG"
else
  push_image "$DATE_TAG"
  push_image "latest"
fi

echo "docker image $IMAGE_NAME built and pushed successfully!"
