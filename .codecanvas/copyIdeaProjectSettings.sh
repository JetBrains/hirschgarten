#!/bin/bash
set -e

# Source and Destination directories
src_dir=".codecanvas/res/idea/project"
dest_dir=".idea"

mkdir -p "$dest_dir"
cp "$src_dir"/* "$dest_dir"
