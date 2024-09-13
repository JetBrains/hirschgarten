# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-base
# to build and push this image locally run `build-and-push.sh base` from `tools/docker` folder

FROM ubuntu:22.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Set display variable for X11
ENV DISPLAY=:0

# Update and install dependencies
RUN apt-get update --quiet --quiet --yes && \
    apt-get install --quiet --quiet --fix-broken --yes \
    curl \
    git \
    xvfb \
    build-essential \
    gcc \
    g++ \
    python3 \
    python3-pip \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxrandr2 \
    zip \
    fontconfig \
    libfontconfig1 \
    fonts-dejavu \
    fonts-liberation \
    fonts-noto \
    x11-apps \
    android-sdk \
    openjdk-17-jdk-headless \
    protobuf-compiler \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Rebuild fontconfig
RUN fc-cache -f -v

# Install Bazelisk
RUN curl -Lo /usr/local/bin/bazel https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-amd64 \
    && chmod +x /usr/local/bin/bazel \
    && bazel version

# Install Coursier
RUN curl -fL "https://github.com/coursier/coursier/releases/download/v2.1.10/cs-x86_64-pc-linux.gz" | gzip -d > "/usr/local/bin/cs" \
    && chmod +x /usr/local/bin/cs