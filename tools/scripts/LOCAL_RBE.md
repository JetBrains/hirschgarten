# Local Remote Build Execution with NativeLink

Run a local NativeLink server in Docker to test the Bazel plugin with remote execution and mTLS authentication.

## Prerequisites

- Docker (Docker Desktop, OrbStack, or similar)
- ~25 GB free disk space for CAS + action cache

## Quick Start

```bash
# From the ultimate root:

# 1. Start NativeLink (builds image, writes .bazelrc-user.bazelrc)
plugins/bazel/tools/scripts/start-local-rbe.sh

# 2. Open Ultimate in IDE with Bazel plugin, trigger sync

# 3. Watch NativeLink processing actions
docker logs -f bazel-rbe-local

# 4. When done — stop server and remove .bazelrc-user.bazelrc
plugins/bazel/tools/scripts/stop-local-rbe.sh
```

## What It Does

The start script builds a Docker image with NativeLink (a Bazel Remote Execution API v2 server), starts it with mTLS enabled, and writes `.bazelrc-user.bazelrc` pointing to the bundled certificates. Inner Bazel authenticates via mTLS and sends build actions over `grpcs://localhost:50051`.

```
IDE (IntelliJ + Bazel plugin)
└── Inner Bazel (sync / aspects / build)
    └── --remote_executor=grpcs://localhost:50051
        │   --tls_certificate=ca.pem
        │   --tls_client_certificate=client.pem
        │   --tls_client_key=client-key.pem
        └── Docker: NativeLink (port 50051, mTLS)
            ├── Server cert: server.pem (signed by CA)
            ├── Client CA:   ca.pem (verifies client certs)
            ├── CAS (filesystem, 20 GB)
            ├── Action Cache (5 GB)
            └── Local Worker (executes actions inside container)
```

## File Overview

| File | Purpose |
|------|---------|
| `scripts/start-local-rbe.sh` | Build image, start container, write `.bazelrc-user.bazelrc` |
| `scripts/stop-local-rbe.sh` | Stop container and remove `.bazelrc-user.bazelrc` |
| `scripts/bazelrc-local-rbe.template` | Reference template (start script generates the actual file) |
| `docker/nativelink/Dockerfile` | NativeLink + Ubuntu build tools image |
| `docker/nativelink/nativelink-local.json5` | NativeLink server config with mTLS on port 50051 |
| `docker/nativelink/certs/` | Static mTLS certificates (CA, server, client) |

## Certificates

Pre-generated self-signed certificates bundled in `docker/nativelink/certs/`, valid for 10 years. These are for local testing only — not production secrets.

| File | Purpose |
|------|---------|
| `ca.pem` / `ca-key.pem` | Self-signed CA (signs both server and client certs) |
| `server.pem` / `server-key.pem` | Server cert (SAN: localhost, 127.0.0.1) |
| `client.pem` / `client-key.pem` | Client cert (CN: bazel-client) |

## Notes

- NativeLink image is **amd64 only**. On Apple Silicon it runs via Rosetta/QEMU.
- CAS data persists at `~/.cache/nativelink/` across container restarts.
- The start script writes `.bazelrc-user.bazelrc` automatically; the stop script removes it.
- **Always run `stop-local-rbe.sh` before switching branches** — the bazelrc overrides conflict with master's HTTP remote cache.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `UNAVAILABLE: connection refused` | Start NativeLink: `start-local-rbe.sh` |
| `Cannot combine gRPC remote execution with HTTP-based caching` | Run `stop-local-rbe.sh` or `rm .bazelrc-user.bazelrc` |
| `TLS handshake failed` / certificate errors | Rebuild image: `docker build -t nativelink-local docker/nativelink/` |
| Actions fail with missing tool | Add the dependency to the Dockerfile, rebuild |
| CAS disk full | `rm -rf ~/.cache/nativelink/` and restart |
