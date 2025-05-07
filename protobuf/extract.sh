#!/bin/bash

# Check if at least two arguments are provided (output directory + at least one JAR file)
if [ "$#" -lt 2 ]; then
  echo "Usage: $0 <output-dir> <jar-file> [jar-file...]"
  exit 1
fi

# Set output directory from the first argument
OUTPUT_DIR="$1"
shift # Remove the first argument so that $@ now contains only the JAR files

# Create the output directory
mkdir -p "$OUTPUT_DIR"

pwd

# Process each JAR file
for JAR in "$@"; do
  if [ ! -f "$JAR" ]; then
    echo "File $JAR not found, skipping."
    continue
  fi

  # Extract contents into OUTPUT_DIR without overwrite prompts
  unzip -qo "$JAR" -d "$OUTPUT_DIR"
done

# Remove all non-Java files
find "$OUTPUT_DIR" -type f ! -name "*.java" -delete

# Remove empty directories recursively, starting from deepest level
find "$OUTPUT_DIR" -depth -type d -empty -delete

echo "Extraction complete. Java files are in $OUTPUT_DIR."
