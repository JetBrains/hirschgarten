#!/bin/bash
set -e

IDE_VERSION=$(echo $CANVAS_IDE_VERSION | cut -f1 -d' ')

# Source and Destination directories
src_dir=".codecanvas/res/config"
dest_dir="/root/.config/JetBrains/IntelliJIdea$IDE_VERSION"

mkdir -p "$dest_dir"
cp -r "$src_dir"/* "$dest_dir"
