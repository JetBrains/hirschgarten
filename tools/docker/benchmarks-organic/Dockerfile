# ═══════════════════════════════════════════════════════════════════════════════
# Hirschgarten Benchmarks Organic Docker Image
# ═══════════════════════════════════════════════════════════════════════════════
# https://jetbrains.team/p/bazel/packages/container/docker-private/hirschgarten-benchmarks-organic
# To build and push this image locally run `build-and-push.sh benchmarks-organic private %space.git.credentials%` from `tools/docker` folder

# ───────────────────────────────── Base Image ─────────────────────────────────
FROM registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest

# ───────────────────────────── User Configuration ──────────────────────────────
# Create a non-root user with UID/GID 1000
ARG CANONICAL_USER=hirschuser
ARG CANONICAL_UID=1000
ARG CANONICAL_GID=${CANONICAL_UID}

RUN groupadd -g ${CANONICAL_GID} ${CANONICAL_USER} && \
    useradd -u ${CANONICAL_UID} -g ${CANONICAL_USER} -m -s /bin/bash ${CANONICAL_USER}

# ────────────────────────── Benchmark Projects ─────────────────────────────
# Switch to the non-root user
USER ${CANONICAL_USER}
WORKDIR /home/${CANONICAL_USER}

# Clone projects for benchmarking
RUN --mount=type=secret,id=space_git_credentials,uid=${CANONICAL_UID} \
    cd /home/${CANONICAL_USER} \
    && git clone --depth 1 https://$(cat /run/secrets/space_git_credentials)@git.jetbrains.team/dl/Datalore.git project_Datalore \
    && git clone --depth 1 https://github.com/JetBrains/hirschgarten.git project_Hirschgarten \
    && git clone --depth 1 https://github.com/bazelbuild/bazel.git project_Bazel \
    && git clone --depth 1 https://github.com/grpc/grpc-java.git project_gRPC-Java \
    && git clone --depth 1 https://github.com/JetBrains/bazel-bsp.git project_Bazel-BSP

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
