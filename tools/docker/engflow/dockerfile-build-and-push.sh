#!/bin/bash

# Set the date format for the tag
DATE_TAG=$(date +%d%m%y)
REGISTRY="registry.jetbrains.team/p/bazel/docker/hirschgarten-engflow"

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

# Build the Docker image
if ! docker build -t $REGISTRY:$DATE_TAG .; then
    echo "Docker build failed. Exiting."
    exit 1
fi

# Tag the image with 'latest'
docker tag $REGISTRY:$DATE_TAG $REGISTRY:latest

# Check if we're logged in, if not, attempt to log in
if ! check_login; then
    handle_login
fi

# Function to push image
push_image() {
    local tag=$1
    if ! docker push $REGISTRY:$tag; then
        echo "Failed to push $REGISTRY:$tag. Attempting to re-authenticate."
        handle_login
        if ! docker push $REGISTRY:$tag; then
            echo "Push failed again for $REGISTRY:$tag. Exiting."
            exit 1
        fi
    fi
}

# Push the images
push_image $DATE_TAG
push_image "latest"

echo "Docker image built and pushed successfully!"