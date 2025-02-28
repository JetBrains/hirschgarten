# Phased sync

Phased sync is an experimental feature that syncs the Bazel project in 2 phases:

1. query phase - runs a Bazel query to determine project structure: targets, sources, dependencies between targets.
2. aspect phase - runs Bazel aspects to determine the full set of dependencies and generated sources for all targets. 

The query phase is typically 10x-100x faster than the aspect phase and is sufficient to enable you to get working with the code - rudimentary highlighting, as well as completions and navigation from and to project sources is enabled. Once the query phase completes, your project is ready for editing.

The aspect phase enables the full set of IDE features, including highlighting of problems with dependencies, and navigating to dependency sources. It runs once the query phase is completed.

## Enabling phased sync

There are 2 registry keys:

- `bsp.use.phased.sync` - enable the phased sync feature
- `bsp.execute.second.phase.on.sync` - instead of running the aspect phase after the query phase, remain in query-synced mode. This is useful to test functionality before running aspects or saving your computer from working through the whole build.