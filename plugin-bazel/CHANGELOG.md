<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.3.2</title>
</head>
<body>
<h1>2025.3.2</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[feature] Add changelog support and improve plugin builder to produce plugin.zip from sources with version stamping in plugin.xml, enabling faster release cycles in release branches. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2663">BAZEL-2663</a></li>
    <li>[fix] Always fall back to default project view when the project view file cannot be found, preventing IllegalStateException during configuration context creation. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2566">BAZEL-2566</a></li>
    <li>[feature] Add run configuration to build Bazel plugin standalone, enabling production of plugin.zip from sources with version stamping equivalent to JPS build output. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2663">BAZEL-2663</a></li>
    <li>[fix] Pass parent environment variables to run targets to fix GUI applications failing on Linux due to missing DISPLAY/WAYLAND_DISPLAY variables, while keeping tests isolated without inherited environment. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2761">BAZEL-2761</a></li>
    <li>[fix] Relative path resolution for bazel_binary now correctly uses the workspace root bazel wrapper instead of falling back to the system bazel from PATH. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2764">BAZEL-2764</a></li>
    <li>[fix] Correct heuristic that incorrectly treated jars from `@lib`, `@ultimate_lib`, and `@jps_to_bazel` repositories as internal, causing external dependencies to be excluded from indexing and resulting in red code errors. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2754">BAZEL-2754</a> | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2392">BAZEL-2392</a></li>
    <li>[fix] Correctly determine external autoloads when `--incompatible_disable_autoloads_in_main_repo` is set by no longer assuming languages are implicitly present, fixing IntelliJ sync issues. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2768">BAZEL-2768</a></li>
    <li>[fix] Add fallback implementation for `get_import_path` to support older protobuf versions that lack the `proto_common.get_import_path` method. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2762">BAZEL-2762</a></li>
    <li>[fix] Broken &quot;Link Bazel Project&quot; action that failed to properly import projects and show the plugin icon, replaced with a new &quot;Load Bazel Project&quot; action that creates and imports a new project instead of modifying current project state. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2638">BAZEL-2638</a></li>
    <li>[feature] Add option to run tests via runfiles instead of Bazel, enabling parallel test execution while Bazel build runs concurrently. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2604">BAZEL-2604</a></li>
    <li>[fix] Prefer class jar over source jar when both are duplicated for external libraries, resolving unresolved reference issues in monorepo imports. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2611">BAZEL-2611</a></li>
    <li>[feature] Add option to run tests via runfiles instead of Bazel, enabling parallel test execution and debugging; includes additional integration tests and a registry key to control the behavior. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2604">BAZEL-2604</a></li>
    <li>[feature] Run tests via runfiles instead of `bazel test` by default to enable parallel test execution, with an option to revert to Bazel test for caching benefits. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2604">BAZEL-2604</a></li>
    <li>[fix] Remove left-over WORKSPACE files in the aspect directory to prevent sync failures caused by directories being added to `--deleted_packages` in `.bazelrc`. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2753">BAZEL-2753</a></li>
    <li>[fix] Proper solution for linking Bazel projects in headless mode, enabling auto-loading of double build system projects like Ultimate for Qodana 253+. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2734">BAZEL-2734</a></li>
    <li>refactor [bazel]: Improve target deduplication from O(n^2) to O(n) during wildcard expansion</li>
    <li>[fix] Bazel project auto-open now correctly loads projects in headless mode and triggers sync, restoring support for double build system projects in Qodana 253+. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2734">BAZEL-2734</a></li>
    <li>[fix] Only the first import now triggers sync on project startup, preventing repeated reimports for Go projects. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2656">BAZEL-2656</a></li>
    <li>[fix] Added ANSI color highlighting support for debug output when using --script_path configurations. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2751">BAZEL-2751</a></li>
    <li>[fix] Forward missing test filter to runWithScriptPath so that test filters are no longer ignored in debug Java run configurations. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2752">BAZEL-2752</a></li>
    <li>[feature] Support `.bazeliskversion` file to enable hermetic workflows by downloading the specified Bazelisk version instead of using locally installed Bazel executables. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2573">BAZEL-2573</a></li>
    <li>[fix] Implement missing `annotateExternallyModCommand` method in BazelExternalAnnotationsManager to restore the &#x27;Annotate&#x27; action functionality in both Bazel and non-Bazel projects. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2125">BAZEL-2125</a></li>
    <li>[maintenance] Renamed the run configuration handler to &quot;Run/Test&quot; in the UI for clarity. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2697">BAZEL-2697</a></li>
    <li>[fix] Automatically link newly created Java/Kotlin template projects in IntelliJ IDEA so the Bazel plugin recognizes them without requiring a manual reopen. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2731">BAZEL-2731</a></li>
    <li>[fix] Change the default value for `import_depth` from `1` to `-1` to match the documented behavior. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2737">BAZEL-2737</a></li>
    <li>[fix] Non-module and non-executable targets (such as certain `jvm_library` targets) are now correctly imported, resolving missing build gutters in the root BUILD.bazel file. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2699">BAZEL-2699</a></li>
    <li>[fix] Optimize package root calculation during sync by using path pattern matching to predict directory structures, reducing precalculation time from 30-40 seconds to under a second for large monorepos. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2551">BAZEL-2551</a></li>
    <li>[fix] Improved error reporting when using aspect directories in Bazel to help diagnose analysis invalidation issues caused by repository injection. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2677">BAZEL-2677</a></li>
    <li>[bazel] Re-enable patronus checks for Bazel plugin with 2 attempts in 253 branch</li>
    <li>[fix] Fall back to the parent directory when the root path is a file that is no longer recognized, preventing IDEA from hanging when opening a Bazel project after the plugin has been uninstalled. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2681">BAZEL-2681</a></li>
    <li>[fix] Async stack trace now correctly appears in debug mode for Kotlin coroutines. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2660">BAZEL-2660</a></li>
    <li>[fix] Corrected aspect label reference in the Bazel plugin&#x27;s Scala aspect, resolving sync failures on Scala projects. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2716">BAZEL-2716</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
</ul>
</body>
</html>
