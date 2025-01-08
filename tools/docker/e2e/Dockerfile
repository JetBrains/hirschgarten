# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-e2e
# to build and push this image locally run `build-and-push.sh e2e` from `tools/docker` folder

FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# Create a non-root user with UID/GID 1001
# 1001 used because it's default user UID/GID on TC agents, and it'll allow to work around permissions for files passed to/from container
RUN groupadd -g 1001 hirschuser && useradd -u 1001 -g hirschuser -m -s /bin/bash hirschuser

# Set up work directory and change ownership
WORKDIR /home/hirschuser/workspace
RUN chown -R hirschuser:hirschuser /home/hirschuser

# Switch to the non-root user
USER hirschuser

# Clone hirschgarten repository
RUN git clone --depth 1 https://github.com/JetBrains/hirschgarten.git /home/hirschuser/hirschgarten

# Generate synthetic 10 benchmark project
RUN cd /home/hirschuser/hirschgarten \
    && bazel run //server/bspcli:generator -- /home/hirschuser/project_10 10 --targetssize 1 \
    && cd /home/hirschuser \
    && rm -rf /home/hirschuser/hirschgarten