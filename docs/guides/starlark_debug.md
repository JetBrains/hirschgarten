# Starlark Debug
Starlark Debug functionality makes it possible to debug Bazel's analysis phase.
It does not complete the build, only the analysis phase is performed.

## How to use
1. Place a breakpoint in a `BUILD` file or in a macro (`.bzl` file)
2. In the tool window, use `Starlark Debug` action in a target context menu

## Troubleshooting
### Target has been simply built, without stopping at my breakpoint
The Starlark Debug is executed during the analysis phase of the build.
If build caches for the chosen target are up to date, this phase does not trigger at all.
Performing `bazel clean` should help in that case.
Alternatively, the chosen target might have not used the line where a breakpoint was placed.

## Limitations
The Starlark Debug functionality supports almost all the IDE's debug functions
(start/pause/stop, step into/over/out). However, it currently has some limitations:
- expression evaluation can be used only on the top-level values (variables in other scopes may not be visible)
- value setting is not supported
