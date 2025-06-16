# ═══════════════════════════════════════════════════════════════════════════════
# Hirschgarten E2E Docker Image
# ═══════════════════════════════════════════════════════════════════════════════
# https://jetbrains.team/p/bazel/packages/container/docker/hirschgarten-e2e
# To build and push this image locally run `build-and-push.sh e2e` from `tools/docker` folder

# ───────────────────────────────── Base Image ─────────────────────────────────
FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# ─────────────────────────────── Dependencies ────────────────────────────────
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

# ───────────────────────────── User Configuration ──────────────────────────────
ARG CANONICAL_USER=hirschuser
ARG CANONICAL_UID=1000
ARG CANONICAL_GID=${CANONICAL_UID}

# Create user and group
RUN groupadd -g ${CANONICAL_GID} ${CANONICAL_USER} && \
    useradd  -u ${CANONICAL_UID} -g ${CANONICAL_USER} -m -s /bin/bash ${CANONICAL_USER}

USER ${CANONICAL_USER}
WORKDIR /home/${CANONICAL_USER}

# ────────────────────────── Benchmark Generation ─────────────────────────────
# Clone repo and generate synthetic benchmark
RUN git clone --depth 1 https://github.com/JetBrains/hirschgarten.git /home/${CANONICAL_USER}/hirschgarten && \
    cd /home/${CANONICAL_USER}/hirschgarten && \
    bazel run //server/bspcli:generator -- /home/${CANONICAL_USER}/project_10 10 --targetssize 1 && \
    cd /home/${CANONICAL_USER} && rm -rf /home/${CANONICAL_USER}/hirschgarten

# ─────────────────────────────── Fixuid Setup ─────────────────────────────────
USER root

ARG FIXUID_VERSION=0.6.0

# Install fixuid binary
RUN curl -sSL \
        "https://github.com/boxboat/fixuid/releases/download/v${FIXUID_VERSION}/fixuid-${FIXUID_VERSION}-linux-amd64.tar.gz" \
        | tar -xz -C /usr/local/bin && \
    chown root:root /usr/local/bin/fixuid && chmod 4755 /usr/local/bin/fixuid && \
    mkdir -p /etc/fixuid

# Configure fixuid
RUN mkdir -p /home/${CANONICAL_USER}/bazel_plugin_cache && \
    printf 'user: %s\ngroup: %s\npaths:\n  - /home/%s\n  - /home/%s/bazel_plugin_cache\n' \
           "${CANONICAL_USER}" "${CANONICAL_USER}" "${CANONICAL_USER}" "${CANONICAL_USER}" \
           > /etc/fixuid/config.yml

# Ensure fixuid always runs even if entrypoint is /bin/sh
RUN REAL_SH=$(readlink -f /bin/sh) && \
    mv /bin/sh /bin/sh.real && \
    printf '#!%s\nexec /usr/local/bin/fixuid -q -- %s "$@"\n' "$REAL_SH" "$REAL_SH" > /bin/sh && \
    chmod 755 /bin/sh

# ───────────────────────────── Final Configuration ─────────────────────────────
USER ${CANONICAL_USER}:${CANONICAL_USER}
