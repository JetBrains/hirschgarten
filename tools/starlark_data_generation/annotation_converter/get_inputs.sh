#!/bin/bash

if [ $# -eq 0 ]; then
    echo "Please provide repository path as an argument"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "Directory $1 does not exist"
    exit 1
fi

find "$1" -type f -exec grep -l "@GlobalMethods" {} \; | grep "bazel/src/main/java/com/google/devtools/build/lib/"
