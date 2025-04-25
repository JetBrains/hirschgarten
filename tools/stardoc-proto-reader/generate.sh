#!/bin/bash

if [ ! -d "./inputs" ]; then
  echo "Error: Directory ./inputs does not exist."
  exit 1
fi

ARGS=$(realpath ./inputs/*)

bazel run reader -- $ARGS
