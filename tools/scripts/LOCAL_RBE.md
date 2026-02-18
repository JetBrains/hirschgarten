# Local Remote Build Execution with NativeLink

Run a local NativeLink server in Docker to test the Bazel plugin with remote execution enabled.

## Prerequisites

- Docker (Docker Desktop, OrbStack, or similar)
- ~25 GB free disk space for CAS + action cache

## Quick Start

```bash
# From the ultimate root:

# 1. Start NativeLink
plugins/bazel/tools/scripts/start-local-rbe.sh

# 2. Configure inner Bazel to use RBE
cp plugins/bazel/tools/scripts/bazelrc-local-rbe.template .bazelrc-user.bazelrc

# 3. Open Ultimate in IDE with Bazel plugin, trigger sync

# 4. Watch NativeLink processing actions
docker logs -f bazel-rbe-local

# 5. When done — stop and revert
plugins/bazel/tools/scripts/stop-local-rbe.sh
rm .bazelrc-user.bazelrc
```

## What It Does

The start script builds a Docker image with NativeLink (a Bazel Remote Execution API v2 server) and runs it locally. Inner Bazel sends build actions to `grpc://localhost:50051` instead of executing them on the host.

```
IDE (IntelliJ + Bazel plugin)
└── Inner Bazel (sync / aspects / build)
    └── --remote_executor=grpc://localhost:50051
        └── Docker: NativeLink (port 50051)
            ├── CAS (filesystem, 20 GB)
            ├── Action Cache (5 GB)
            └── Local Worker (executes actions inside container)
```

## File Overview

| File | Purpose |
|------|---------|
| `scripts/start-local-rbe.sh` | Build image and start container |
| `scripts/stop-local-rbe.sh` | Stop and remove container |
| `scripts/bazelrc-local-rbe.template` | Bazel flags to copy into `.bazelrc-user.bazelrc` |
| `docker/nativelink/Dockerfile` | NativeLink + Ubuntu build tools image |
| `docker/nativelink/nativelink-local.json5` | NativeLink server configuration |

## Notes

- NativeLink image is **amd64 only**. On Apple Silicon it runs via Rosetta/QEMU — expect slower action execution compared to native builds.
- CAS data persists at `~/.cache/nativelink/` across container restarts. Delete this directory to clear the cache.
- The `.bazelrc-user.bazelrc` file overrides the project's HTTP remote cache (`--remote_cache=`). **Remove it when switching back to normal builds**, otherwise you'll get: `ERROR: Cannot combine gRPC based remote execution with HTTP-based caching`.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `UNAVAILABLE: connection refused` | Start NativeLink: `start-local-rbe.sh` |
| `Cannot combine gRPC remote execution with HTTP-based caching` | Remove `.bazelrc-user.bazelrc` |
| Actions fail with missing tool | Add the dependency to the Dockerfile, rebuild |
| CAS disk full | `rm -rf ~/.cache/nativelink/` and restart |
