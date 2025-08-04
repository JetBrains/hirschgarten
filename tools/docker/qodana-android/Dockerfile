# https://jetbrains.team/p/ij/packages/container/bazel-private/bazel-plugin-qodana-android
# to build and push this image locally run `build-and-push.sh qodana-android private` from `tools/docker` folder

ARG PLATFORMDOT=2025.1
FROM registry.jetbrains.team/p/ij/space-private-mirror/qodana-android:${PLATFORMDOT}-nightly
ARG TARGETARCH
RUN apt-get update
RUN apt-get -y install gcc mono-mcs zip
RUN apt-get -y install --reinstall build-essential python3

# Install Android API level 35
RUN ${ANDROID_SDK_ROOT}/cmdline-tools/tools/bin/sdkmanager --install "platforms;android-35" "build-tools;35.0.0"
ARG ANDROID_API_LEVEL="35"

ADD https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-$TARGETARCH /usr/bin/bazel
RUN chmod 777 /usr/bin/bazel
