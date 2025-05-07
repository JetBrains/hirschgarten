#!/bin/bash

# Check for "online" argument
ONLINE=false
[ "$1" = "online" ] && ONLINE=true

# Common docker arguments
docker_args=(
  -u "$(id -u):$(id -g)"
  -v "$(cd ../.. && pwd):/data/project/"
  -v "$(pwd)/plugins/plugin-bazel:/opt/idea/custom-plugins/plugin-bazel"
  -v "$(pwd)/results/:/data/results"
  -v "/tmp/.qodana-cache:/root/.cache"
  --cpus="10"
  --memory="31g"
  --memory-swap="32g"
)

# Add specific docker arguments based on "online" flag
if [ "$ONLINE" = true ]; then
  docker_args+=(
    -e "QODANA_TOKEN=$QODANA_TOKEN"
  )
else
  docker_args+=(
    -e "QODANA_LICENSE_ONLY_TOKEN=$QODANA_TOKEN"
    -p 8080:8080
  )
fi

# Qodana-specific arguments
qodana_args=(
  --property=bsp.build.project.on.sync=true
  --property=idea.is.internal=true
  --report-dir /data/results/report/
  --baseline tools/qodana/qodana.sarif.json
  --config tools/qodana/qodana.yaml
  $([ "$ONLINE" = true ] && echo "--save-report" || echo "--show-report")
)

# Print the complete command for debugging
echo "docker run ${docker_args[@]} registry.jetbrains.team/p/bazel/docker-private/hirschgarten-qodana:latest ${qodana_args[@]}"

# Run the docker command
docker run "${docker_args[@]}" registry.jetbrains.team/p/bazel/docker-private/hirschgarten-qodana:latest "${qodana_args[@]}"
