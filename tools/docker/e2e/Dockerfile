# syntax=docker/dockerfile:1
# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-e2e
# to build and push this image locally run `build-and-push.sh e2e` from `tools/docker` folder

FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# Clone repo and generate synthetic benchmark
RUN git clone --depth 1 https://github.com/JetBrains/hirschgarten.git /home/hirschuser/hirschgarten && \
    cd /home/hirschuser/hirschgarten && \
    bazel run //server/bspcli:generator -- /home/hirschuser/project_10 10 --targetssize 1 && \
    cd /home/hirschuser && rm -rf /home/hirschuser/hirschgarten
