# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-engflow
# to build and push this image locally run `build-and-push.sh engflow` from `tools/docker` folder
FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# Create the EngFlow user and group
RUN groupadd --gid 114 engflow && \
    useradd --home-dir /home/engflow --create-home --uid 108 --gid 114 engflow

ENV HOME=/home/engflow

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Install additional dependencies
RUN apt-get update --quiet --quiet --yes && \
    apt-get install --quiet --quiet --fix-broken --yes \
    clang \
    cmake \
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
    python-is-python3 \
    && rm -rf /var/lib/apt/lists/*

# Switch to the EngFlow user
#USER engflow
#
## Set HOME environment variable
#ENV HOME=/home/engflow
#
## Clone hirschgarten repository
#RUN git clone --depth 1 https://github.com/JetBrains/hirschgarten.git /home/engflow/hirschgarten
#
## Generate synthetic 10 benchmark project
#RUN cd /home/engflow/hirschgarten \
#    && bazel run //server/bspcli:generator -- /home/engflow/project_10 10 --targetssize 1 \
#    && cd /home/engflow \
#    && rm -rf /home/engflow/hirschgarten
#
## Ensure proper ownership of the created directories
#USER root
#RUN chown -R engflow:engflow /home/engflow/project_10
#
## Set JAVA_HOME environment variable
#
## Set up work directory
#WORKDIR /home/engflow