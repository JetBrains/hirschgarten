#!/bin/bash

# Check if folder name is provided
if [ $# -eq 0 ]; then
	echo "Usage: $0 <folder_name> [space_git_credentials]"
	exit 1
fi

FOLDER_NAME=$1
DATE_TAG=$(date +%d%m%y)

# Set registry and image name based on folder
if [ "$FOLDER_NAME" == "benchmarks" ]; then
	REGISTRY="registry.jetbrains.team/p/bazel/docker-private"
	IMAGE_NAME="hirschgarten-benchmarks"

	# Check if space_git_credentials is provided for benchmarks
	if [ $# -lt 2 ]; then
		echo "Error: space_git_credentials is required for benchmarks"
		exit 1
	fi
	SPACE_GIT_CREDENTIALS=$2
else
	REGISTRY="registry.jetbrains.team/p/bazel/docker"
	IMAGE_NAME="hirschgarten-$FOLDER_NAME"
fi

FULL_IMAGE_NAME="$REGISTRY/$IMAGE_NAME"

# Function to check if we're logged in
check_login() {
	docker login $REGISTRY -u dummy -p dummy &>/dev/null
	return $?
}

# Function to handle login
handle_login() {
	echo "Authentication required. Please log in:"
	docker login $REGISTRY
	if [ $? -ne 0 ]; then
		echo "Login failed. Exiting."
		exit 1
	fi
}

# Function to build the Docker image
build_image() {
	if [ "$FOLDER_NAME" == "benchmarks" ]; then
		if ! docker build --secret id=space_git_credentials,src=<(echo "$SPACE_GIT_CREDENTIALS") -t $FULL_IMAGE_NAME:$DATE_TAG .; then
			echo "Docker build failed for benchmarks. Exiting."
			exit 1
		fi
	else
		if ! docker build -t $FULL_IMAGE_NAME:$DATE_TAG .; then
			echo "Docker build failed. Exiting."
			exit 1
		fi
	fi

	# Tag the image with 'latest'
	docker tag $FULL_IMAGE_NAME:$DATE_TAG $FULL_IMAGE_NAME:latest
}

# Function to push image
push_image() {
	local tag=$1
	if ! docker push $FULL_IMAGE_NAME:$tag; then
		echo "Failed to push $FULL_IMAGE_NAME:$tag. Attempting to re-authenticate."
		handle_login
		if ! docker push $FULL_IMAGE_NAME:$tag; then
			echo "Push failed again for $FULL_IMAGE_NAME:$tag. Exiting."
			exit 1
		fi
	fi
}

# Main execution
cd "$(dirname "$0")/$FOLDER_NAME" || exit 1

# Check if we're logged in, if not, attempt to log in
if ! check_login; then
	handle_login
fi

# Build the Docker image
build_image

# Push the images
push_image $DATE_TAG
push_image "latest"

echo "Docker image $IMAGE_NAME built and pushed successfully!"
