# https://jetbrains.team/p/ij/packages/container/bazel-private/bazel-plugin-qodana
# to build and push this image locally run `build-and-push.sh qodana private` from `tools/docker` folder

ARG PLATFORMDOT=2025.1
FROM registry.jetbrains.team/p/ij/space-private-mirror/qodana-jvm:${PLATFORMDOT}-nightly
ARG TARGETARCH
RUN apt-get update
RUN apt-get -y install gcc mono-mcs zip openjdk-21-jdk
RUN apt-get -y install --reinstall build-essential python3

ADD https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-$TARGETARCH /usr/bin/bazel
RUN chmod 777 /usr/bin/bazel
