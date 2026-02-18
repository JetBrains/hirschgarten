#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/../docker/nativelink"
CACHE_DIR="$HOME/.cache/nativelink"
CONTAINER_NAME="bazel-rbe-local"

mkdir -p "$CACHE_DIR"

echo "Building NativeLink image..."
docker build --platform=linux/amd64 -t nativelink-local "$DOCKER_DIR"

docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

echo "Starting NativeLink..."
docker run --init --rm -d \
  --platform=linux/amd64 \
  --name "$CONTAINER_NAME" \
  -v "$CACHE_DIR:/data" \
  -p 50051:50051 \
  nativelink-local

for i in $(seq 1 30); do
  if docker logs "$CONTAINER_NAME" 2>&1 | grep -q "Ready, listening on 0.0.0.0:50051"; then
    echo "NativeLink RBE ready at grpc://localhost:50051"
    echo ""
    echo "Add to your .bazelrc.user:"
    echo "  build --remote_executor=grpc://localhost:50051"
    echo "  build --remote_default_exec_properties=OSFamily=Linux"
    echo "  build --jobs=HOST_CPUS*.5"
    echo "  build --remote_timeout=600"
    exit 0
  fi
  sleep 1
done

echo "ERROR: NativeLink failed to start within 30s"
docker logs "$CONTAINER_NAME"
exit 1
