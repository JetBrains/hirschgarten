# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-codecanvas
# to build and push this image locally use following commands:
# docker build -t registry.jetbrains.team/p/bazel/docker/hirschgarten-codecanvas:$(date +%d%m%y) .
# docker tag registry.jetbrains.team/p/bazel/docker/hirschgarten-codecanvas:$(date +%d%m%y) registry.jetbrains.team/p/bazel/docker/hirschgarten-codecanvas:latest
# docker push registry.jetbrains.team/p/bazel/docker/hirschgarten-codecanvas:$(date +%d%m%y)
# docker push registry.jetbrains.team/p/bazel/docker/hirschgarten-codecanvas:latest

FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# Install additional dependencies
RUN apt-get update --quiet --quiet --yes && \
    apt-get install --quiet --quiet --fix-broken --yes \
    clang \
    cmake \
    docker.io \
    expect \
    ninja-build \
    pkg-config \
    libssl-dev \
    libcurl4-openssl-dev \
    libc-ares-dev \
    libprotobuf-dev \
    protobuf-compiler-grpc \
    libgrpc++-dev \
    libgtest-dev \
    libgflags-dev \
    libbenchmark-dev \
    libboost-all-dev \
    openssh-server \
    python-is-python3 \
    tigervnc-standalone-server \
    util-linux \
    xfce4 \
    && rm -rf /var/lib/apt/lists/*

# install noVNC
RUN git clone https://github.com/novnc/noVNC noVNC

## Create the EngFlow user and group
#RUN groupadd -g 1001 hirschuser \
#    && useradd -u 1001 -g hirschuser -m -s /bin/bash hirschuser
#
#RUN chown -R hirschuser:hirschuser /home/hirschuser
#
#ENV HOME=/home/hirschuser
#
### Set up work directory
#WORKDIR /home/hirschuser
#
## Switch to the hirschuser user
#USER hirschuser
