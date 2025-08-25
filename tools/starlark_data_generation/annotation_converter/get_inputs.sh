#!/bin/bash

# Usage: bash get_inputs.sh $PATH_TO_BAZEL_REPO

# This script finds all files in the bazel repo that contain the annotation @GlobalMethods,
# which means it contains @StarlarkMethod annotations that can be extracted.
# Located files then can be fed to the annotation_converter to generate documentation data.

# WARNING! Not all fils that contain @StarlarkMethod also contain @GlobalMethod,
# these exceptions are included in the inputs file, so overwrite that carefully.

# We don't look for all files containing @StarlarkMethod because that would also include
# native build rules which we handle separately.

if [ $# -eq 0 ]; then
    echo "Please provide repository path as an argument"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "Directory $1 does not exist"
    exit 1
fi

find "$1" -type f -exec grep -l "@GlobalMethods" {} \; | grep "bazel/src/main/java/com/google/devtools/build/lib/"
