FROM registry.jetbrains.team/p/sa/containers/qodana-jvm:2024.2-nightly
ARG TARGETARCH
RUN apt-get update
RUN apt-get -y install gcc mono-mcs zip
RUN apt-get -y install --reinstall build-essential python3
ARG USERNAME=bazel_user
ARG USER_UID=1000
ARG USER_GID=0
RUN adduser --uid $USER_UID --gid $USER_GID $USERNAME
ADD https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-$TARGETARCH /usr/bin/bazel
RUN chmod 777 /usr/bin/bazel
USER $USERNAME
RUN mkdir /root/.cache/