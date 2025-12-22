<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.2.10</title>
</head>
<body>
<h1>2025.2.10</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[fix] Pass parent environment variables to run targets to fix GUI applications failing on Linux due to missing DISPLAY/WAYLAND_DISPLAY variables, while keeping tests isolated without inherited environment. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2761">BAZEL-2761</a></li>
    <li>[fix] Corrected bazel_binary path resolution to properly use the workspace-relative bazel wrapper instead of falling back to the system bazel from PATH. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2764">BAZEL-2764</a></li>
    <li>[fix] Correct heuristic that incorrectly treated jars from `@lib`, `@ultimate_lib`, and `@jps_to_bazel` repositories as internal, causing external dependencies to be excluded from indexing and resulting in red code errors. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2754">BAZEL-2754</a> | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2392">BAZEL-2392</a></li>
    <li>[fix] Correctly determine external autoloads when `--incompatible_disable_autoloads_in_main_repo` is set by no longer assuming languages are implicitly present, fixing IntelliJ sync issues. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2768">BAZEL-2768</a></li>
    <li>[fix] Add fallback implementation for `get_import_path` to support older protobuf versions that lack this method in `proto_common`. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2762">BAZEL-2762</a></li>
    <li>cleanup [bazel]: Use `invariantSeparatorsPathString` for consistent path formatting in repo mapping, remove unused import</li>
    <li>refactor [bazel]: Improve target deduplication from O(n^2) to O(n) during wildcard expansion</li>
    <li>[feature] Add option to run tests via runfiles instead of Bazel, enabling parallel test execution while Bazel build runs. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2604">BAZEL-2604</a></li>
    <li>[feature] Added more integration tests and fixed debugging when using &quot;Test with Bazel&quot; mode, supporting parallel test execution via runfiles instead of direct Bazel invocation. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2604">BAZEL-2604</a></li>
    <li>[feature] Changed default test execution to use runfiles via --script_path instead of `bazel test`, enabling parallel test runs; added a registry option to revert to Bazel test execution for caching benefits. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2604">BAZEL-2604</a></li>
    <li>[fix] Added ANSI color highlighting support for debug output when using --script_path configurations. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2751">BAZEL-2751</a></li>
    <li>[fix] Remove left-over WORKSPACE files in the aspect directory to prevent sync failures caused by directories being added to `--deleted_packages` in `.bazelrc`. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2753">BAZEL-2753</a></li>
    <li>[fix] Forward missing test filter to runWithScriptPath so that test filters are no longer ignored in debug Java run configurations. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2752">BAZEL-2752</a></li>
    <li>[fix] Implement missing `annotateExternallyModCommand` method in BazelExternalAnnotationsManager to restore the &#x27;Annotate&#x27; action functionality in both Bazel and non-Bazel projects. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2125">BAZEL-2125</a></li>
    <li>[maintenance] Rename the run configuration handler to &quot;Run/Test&quot; in the UI. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2697">BAZEL-2697</a></li>
    <li>[fix] Change the default value for `import_depth` from `1` to `-1` to match the documented behavior. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2737">BAZEL-2737</a></li>
    <li>[fix] Non-module and non-executable targets such as `jvm_library` are now correctly imported and display build gutters. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2699">BAZEL-2699</a></li>
    <li>[fix] Improved error reporting when using the aspect directory, addressing analysis invalidation issues caused by repository injection for the bazel plugin. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2677">BAZEL-2677</a></li>
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
