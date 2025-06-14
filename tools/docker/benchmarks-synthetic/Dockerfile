# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-benchmarks-synthetic
# to build and push this image locally run `build-and-push.sh benchmarks-synthetic` from `tools/docker` folder

FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

USER root
# Set up work directory and change ownership
WORKDIR /home/hirschuser/workspace
RUN mkdir -p /home/hirschuser && chown -R hirschuser:hirschuser /home/hirschuser

# Switch to the non-root user
USER hirschuser

# Clone hirschgarten repository
RUN git clone --depth 1 https://github.com/JetBrains/hirschgarten.git /home/hirschuser/hirschgarten

RUN cd /home/hirschuser/hirschgarten \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_1000 1000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_5000 5000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_10000 10000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_20000 20000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_1 1 --targetssize 50000 \
    && cd /home/hirschuser \
    && rm -rf /home/hirschuser/hirschgarten
