# https://jetbrains.team/p/bazel/packages/container/docker-private/hirschgarten-qodana
# to build and push this image locally run `build-and-push.sh qodana private` from `tools/docker` folder

FROM registry.jetbrains.team/p/bazel/qodana-private/qodana-android:2024.3-nightly
ARG TARGETARCH
RUN apt-get update
RUN apt-get -y install gcc mono-mcs zip
RUN apt-get -y install --reinstall build-essential python3

ADD https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-$TARGETARCH /usr/bin/bazel
RUN chmod 777 /usr/bin/bazel
