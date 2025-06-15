# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-base
# to build and push this image locally run `build-and-push.sh base` from `tools/docker` folder

FROM ubuntu:22.04

# ───────────────────────────── system-wide defaults ────────────────────────────
# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Set display variable for X11
ENV DISPLAY=:0

# ───────────────────────────── dependency stack ────────────────────────────────
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
    python3-venv \
    python3-dev \
    python3-wheel \
    python3-setuptools \
    python3-distutils \
    python3-tk \
    python-is-python3 \
    ipython3 \
    libpq-dev \
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
    jq \
    wget

# Rebuild fontconfig
RUN fc-cache -f -v

# Install Bazelisk
RUN curl -Lo /usr/local/bin/bazel https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-amd64 \
    && chmod +x /usr/local/bin/bazel \
    && bazel version

# Upgrade pip to latest version
RUN python3 -m pip install --no-cache-dir --upgrade pip

# Install essential Python development tools
RUN python3 -m pip install --no-cache-dir \
    black \
    flake8 \
    mypy \
    pylint \
    poetry
