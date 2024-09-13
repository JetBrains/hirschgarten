# https://jetbrains.team/p/bazel/packages/container/docker-private/hirschgarten-benchmarks,kyuhhhhhhhhhhhhhhhhhhhhyyyyyyyyyyyyyyyyyyyyyyyyyy
# to build and push this image locally run `build-and-push.sh benchmarks %space.git.credentials%` from `tools/docker` folder

FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# Create a non-root user with UID/GID 1001
# 1001 used because it's default user UID/GID on TC agents, and it'll allow to work around permissions for files passed to/from container
RUN groupadd -g 1001 hirschuser && useradd -u 1001 -g hirschuser -m -s /bin/bash hirschuser

# Set up work directory and change ownership
WORKDIR /home/hirschuser/workspace
RUN chown -R hirschuser:hirschuser /home/hirschuser

# Switch to the non-root user
USER hirschuser

# Clone projects for benchmarking
RUN --mount=type=secret,id=space_git_credentials,uid=1001 \
    cd /home/hirschuser \
    && git clone --depth 1 https://$(cat /run/secrets/space_git_credentials)@git.jetbrains.team/dl/Datalore.git \
    && git clone --depth 1 https://github.com/JetBrains/hirschgarten.git \
    && git clone --depth 1 https://github.com/bazelbuild/bazel.git \
    && git clone --depth 1 https://github.com/grpc/grpc-java.git \
    && git clone --depth 1 https://github.com/JetBrains/bazel-bsp.git

RUN cd /home/hirschuser/hirschgarten \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_1000 1000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_5000 5000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_10000 10000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_20000 20000 --targetssize 1 \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_1 1 --targetssize 50000