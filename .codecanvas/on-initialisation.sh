#!/bin/bash
set -e

# tools
# TODO move it to a docker image
echo "Install bazelisk"
npm install -g @bazel/bazelisk

echo "Install plugins"
.codecanvas/installPlugins.sh

echo "Copy IDE settings"
.codecanvas/copyIdeaSettings.sh

echo "On container creation hook"
echo "Forward ports"
.codecanvas/copyIdeaProjectSettings.sh

echo "Install VNC server and noVNC"
.codecanvas/vnc/vnc-config.sh
echo "Installed VNC server and noVNC"
