# C/C++ Support of the Bazel Plugin

| Module | Kind | Why it exists |
|---|---|---|
| `intellij.bazel.clion.common` | backend | The sync code. The Bazel language server can use it, because it does not depend on the IDE features of `intellij.bazel.core`. |
| `intellij.bazel.clion.core` | backend | The IDE specific features, such as run and debug. `LanguageServerProjectStructureTest` does not let the language server depend on it. |
| `intellij.bazel.clion.shared` | shared | A default split mode module for RPC interfaces and DTOs. |
| `intellij.bazel.clion.frontend` | frontend | A default split mode module for the UI. |
| `intellij.bazel.clion.test.catch` | backend | The Catch2 support. Catch2 is an optional CLion plugin, and the user can disable it. |
| `intellij.bazel.clion.test.google` | backend | The GoogleTest support, for the same reason as `test.catch`. |
