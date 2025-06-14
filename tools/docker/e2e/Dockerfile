# syntax=docker/dockerfile:1
# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-e2e
# to build and push this image locally run `build-and-push.sh e2e` from `tools/docker` folder

FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# Extra test-time dependencies
USER root
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

# Workspace setup (dirs will be re-owned by fixuid on container start)
WORKDIR /home/hirschuser/workspace
RUN mkdir -p /home/hirschuser && chown -R hirschuser:hirschuser /home/hirschuser

# Switch back to canonical user
USER hirschuser

# Clone repo and generate synthetic benchmark
RUN git clone --depth 1 https://github.com/JetBrains/hirschgarten.git /home/hirschuser/hirschgarten && \
    cd /home/hirschuser/hirschgarten && \
    bazel run //server/bspcli:generator -- /home/hirschuser/project_10 10 --targetssize 1 && \
    cd /home/hirschuser && rm -rf /home/hirschuser/hirschgarten
