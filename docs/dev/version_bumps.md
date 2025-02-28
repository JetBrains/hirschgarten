# Version bumps

If you want to bump the version (for example, before a release), you should do this in the following places:

- `central-sync/VERSION`
- `maven_coordinates` in `server/src/main/kotlin/org/jetbrains/bazel/BUILD`
- `VERSION` in `commons/src/main/kotlin/org/jetbrains/bazel/commons/Constants.java`
