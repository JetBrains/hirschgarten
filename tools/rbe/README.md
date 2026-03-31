# Local Single-Node Remote Execution

This directory contains a Docker-free local remote execution setup for the Bazel plugin and the
`ultimate` monorepo.

It intentionally uses Bazel's own in-tree remote worker instead of a clustered remote execution
stack. The worker is built from a pinned upstream Bazel source checkout derived automatically from
the target workspace's `.bazelversion`.

## Quick start

From the workspace you want to enable, or by passing its path explicitly:

```bash
./plugins/bazel/tools/rbe/setup.sh
./plugins/bazel/tools/rbe/setup.sh /path/to/workspace
```

That one command:

1. Finds the target workspace and reads its `.bazelversion`.
2. Derives the upstream Bazel version from that value.
3. Clones that exact `bazelbuild/bazel` tag into the user cache, unless it is already there.
4. Starts Bazel's in-tree remote worker on `grpc://127.0.0.1:9092`.
5. Writes ignored user-local bazelrc files that enable `--config=rbe-local` automatically for:
   - `<workspace>/.bazelrc-user.bazelrc`
   - `<workspace>/plugins/bazel/.user.bazelrc` when that subproject exists

After that, regular `bazel build`, `bazel test`, IDE syncs, and IDE-started Bazel invocations in
that workspace will go through the local single-node RE backend and use the same worker as a local
remote cache.

## Scripts

- `setup.sh` starts the worker and writes the ignored user-local bazelrc files.
- `start.sh` starts the worker without touching the bazelrc files.
- `stop.sh` stops the worker.
- `status.sh` prints the current worker state, cache paths, and generated bazelrc paths.

## What gets committed vs generated

Committed:

- The helper scripts in this directory.

Generated locally and ignored by git:

- [`ultimate/.bazelrc-user.bazelrc`](../../../../.bazelrc-user.bazelrc)
- [`ultimate/plugins/bazel/.user.bazelrc`](../../.user.bazelrc)

Delete those generated files if you want to disable this setup completely.

## State location

The worker state lives outside the repo so the checkout stays clean.

macOS:

```text
~/Library/Caches/JetBrains/bazel-plugin/rbe/<bazel-tag>/
```

Linux:

```text
${XDG_CACHE_HOME:-~/.cache}/JetBrains/bazel-plugin/rbe/<bazel-tag>/
```

That directory contains:

- the cached upstream Bazel source checkout used to launch the worker
- CAS data
- work files
- `remote-worker.log`
- pid files

## Optional environment variables

- `RBE_PORT=<port>` changes the worker port. The setup script writes matching overrides into the
  generated user-local bazelrc files.
- `RBE_USE_LINUX_SANDBOX=1` enables Bazel worker sandboxing on Linux. This is off by default so
  the setup is robust on ordinary developer machines.
- `RBE_BAZEL_SOURCE_DIR=/path/to/bazel` reuses an existing Bazel source checkout instead of
  cloning into the cache. Once provided successfully, it is remembered in the local RBE state and
  reused by later `setup.sh`, `start.sh`, and `status.sh` runs.
- `RBE_UPSTREAM_BAZEL_GIT_URL=https://github.com/<owner>/bazel.git` overrides the upstream Bazel
  source repo URL used for the automatic checkout.
- `RBE_UPSTREAM_BAZEL_VERSION=<version>` overrides the derived upstream Bazel version.

Examples:

```bash
RBE_PORT=9191 ./plugins/bazel/tools/rbe/setup.sh
RBE_USE_LINUX_SANDBOX=1 ./plugins/bazel/tools/rbe/setup.sh
RBE_BAZEL_SOURCE_DIR=../bazel ./plugins/bazel/tools/rbe/setup.sh
./plugins/bazel/tools/rbe/setup.sh /tmp/some-bazel-workspace
```

## Notes

- The generated user-local bazelrc files are self-contained, so installation into another workspace
  does not depend on that workspace containing `plugins/bazel/tools/rbe/rbe-local.bazelrc`.
- The generated config points both `--remote_executor` and `--remote_cache` at the local worker,
  plus execution-strategy overrides needed to make `ultimate` builds work with it.
- The profile sets `--remote_cache_compression=false` because Bazel's in-tree worker does not
  advertise remote cache compression support.
- `ultimate/build/remote_cache.bazelrc` normally sets `--remote_upload_local_results=false` for the
  shared HTTP cache. The local profile overrides that to `true` so locally-fallbacked actions can
  still populate the worker-backed cache.
- For `.bazelversion` values like `JetBrains/8.5.1-jb_20260304_104`, the automatic derivation uses
  the upstream base version `8.5.1`. For normal `.bazelversion` values like `8.5.1`, it uses that
  value directly.
- If you want to skip the automatic pinned checkout, point `RBE_BAZEL_SOURCE_DIR` at an existing
  source checkout once and the scripts will keep using it.
- If the target workspace's `.bazelrc` does not import `.bazelrc-user.bazelrc`, `setup.sh` prints
  the line you need to add.
- This setup is meant for local testing and debugging. It is not a replacement for EngFlow or any
  other multi-machine production RE deployment.

## Upstream references

- Bazel in-tree remote worker:
  <https://github.com/bazelbuild/bazel/blob/master/src/tools/remote/README.md>
- Bazel shell tests that launch the same worker:
  <https://github.com/bazelbuild/bazel/blob/master/src/test/shell/bazel/remote/remote_utils.sh>
