# NativeLink local RBE server for Bazel plugin testing.
# To build and start: ./tools/scripts/start-local-rbe.sh
# To push to registry: ../build-and-push.sh nativelink
#
# NativeLink is amd64 only — on Apple Silicon this runs via Rosetta/QEMU.

FROM --platform=linux/amd64 ghcr.io/tracemachina/nativelink:v1.0.0-rc2 AS nativelink-bin

FROM --platform=linux/amd64 ubuntu:22.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash coreutils ca-certificates curl git \
    gcc g++ make \
    python3 python-is-python3 \
    unzip zip \
    && rm -rf /var/lib/apt/lists/*

# The official image is Nix-built; the binary lives at /nix/store/<hash>/bin/nativelink.
# Use a glob to avoid hardcoding the Nix store hash.
RUN --mount=from=nativelink-bin,source=/,target=/nativelink-img \
    cp /nativelink-img/nix/store/*/bin/nativelink /usr/local/bin/nativelink

COPY nativelink-local.json5 /config/nativelink.json5
COPY certs/ /config/certs/

VOLUME /data
EXPOSE 50051 50061

ENTRYPOINT ["nativelink", "/config/nativelink.json5"]
