# https://jetbrains.team/p/ij/packages/container/bazel-private/bazel-plugin-qodana
# to build and push this image locally run `build-and-push.sh qodana private` from `tools/docker` folder

ARG PLATFORMDOT=2025.1
FROM registry.jetbrains.team/p/ij/space-private-mirror/qodana-jvm:${PLATFORMDOT}-nightly
ARG TARGETARCH
RUN apt-get update && \
    apt-get install -y wget apt-transport-https gpg && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get -y install gcc mono-mcs zip temurin-21-jdk
RUN apt-get -y install --reinstall build-essential python3

ADD https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-$TARGETARCH /usr/bin/bazel
RUN chmod 777 /usr/bin/bazel
