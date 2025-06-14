# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-base
# to build and push this image locally run `build-and-push.sh base` from `tools/docker` folder

FROM ubuntu:22.04

# ───────────────────────────── system-wide defaults ────────────────────────────
# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Set display variable for X11
ENV DISPLAY=:0

# refresh keyring
RUN apt-get -o Acquire::AllowInsecureRepositories=true update -qq \
 && apt-get install -y --allow-unauthenticated --no-install-recommends \
        ca-certificates gnupg ubuntu-keyring \
 && rm -rf /var/lib/apt/lists/*

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
    wget \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

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

# ─────────────────────────────── fixuid block ─────────────────────────────────
ARG FIXUID_VERSION=0.6.0
ARG CANONICAL_USER=hirschuser
ARG CANONICAL_UID=1000
ARG CANONICAL_GID=${CANONICAL_UID}

# 1. Canonical user / group
RUN groupadd -g ${CANONICAL_GID} ${CANONICAL_USER} && \
    useradd  -u ${CANONICAL_UID} -g ${CANONICAL_USER} -m -s /bin/bash ${CANONICAL_USER}

# 2. Fixuid binary + config
RUN curl -sSL \
        "https://github.com/boxboat/fixuid/releases/download/v${FIXUID_VERSION}/fixuid-${FIXUID_VERSION}-linux-amd64.tar.gz" \
        | tar -xz -C /usr/local/bin && \
    chown root:root /usr/local/bin/fixuid && chmod 4755 /usr/local/bin/fixuid && \
    mkdir -p /etc/fixuid

RUN mkdir -p /home/${CANONICAL_USER}/bazel_plugin_cache && \
    printf 'user: %s\ngroup: %s\npaths:\n  - /home/%s\n  - /home/%s/bazel_plugin_cache\n' \
           "${CANONICAL_USER}" "${CANONICAL_USER}" "${CANONICAL_USER}" "${CANONICAL_USER}" \
           > /etc/fixuid/config.yml

# ensure fixuid always runs even if entrypoint is /bin/sh
RUN REAL_SH=$(readlink -f /bin/sh) && \
    mv /bin/sh /bin/sh.real && \
    printf '#!%s\nexec /usr/local/bin/fixuid -q -- %s "$@"\n' "$REAL_SH" "$REAL_SH" > /bin/sh && \
    chmod 755 /bin/sh

# 3. Default identity & run command
USER ${CANONICAL_USER}:${CANONICAL_USER}
