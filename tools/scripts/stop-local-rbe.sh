#!/bin/bash
docker rm -f bazel-rbe-local 2>/dev/null && echo "NativeLink stopped." || echo "Not running."
