<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.3.4</title>
</head>
<body>
<h1>2025.3.4</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[fix] Bazel run/debug gutter actions now only appear for Python files defined as main in py_binary or py_test targets, preserving standard IntelliJ run/debug options for other Python files in Bazel projects. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-83488">BAZEL-83488</a></li>
    <li>[fix] Resolve NullPointerException that crashed PyCharm when importing an empty Bazel project. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2840">BAZEL-2840</a></li>
    <li>[fix] Register Scala&#x27;s dependency on Java to resolve sync failures in Bazel 9 where JavaInfo was not defined in the generated scala_info.bzl file. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2846">BAZEL-2846</a></li>
    <li>[maintenance] Support simultaneous queries for bazel mod dump_repo_mapping, avoiding unnecessary calls when rules_kotlin is used alongside rules_java. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2785">BAZEL-2785</a></li>
    <li>[fix] Ensure that JavaInfo is properly imported in generated scala_info.bzl to fix Bazel 9 sync failures for Scala repositories. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2846">BAZEL-2846</a></li>
    <li>[fix] Bazel `run` and `build` commands now support graceful termination, preserving the Bazel server and analysis cache when stopping processes via the UI instead of forcefully killing the server. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2825">BAZEL-2825</a></li>
    <li>[fix] Avoid unnecessary calls to dump_repo_mapping to address incorrect representation of repo-mapping data structure where apparent-to-canonical repository name mappings should be repository-dependent. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2787">BAZEL-2787</a></li>
    <li>[fix] Update location regex in IntelliJRunnerOutputParser to correctly parse Kotlin/Java error diagnostics after file location format changed from `/path (line:col)` to `/path:line:col`. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2742">BAZEL-2742</a></li>
    <li>[fix] Correct for-loop variable scoping in reference resolution to properly recognize nested Starlark tuples. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2811">BAZEL-2811</a></li>
    <li>[fix] Nested Starlark tuples are now properly recognized in Bazel. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2811">BAZEL-2811</a></li>
    <li>[maintenance] Refactor Starlark&#x27;s globbing mechanism to use Kotlin coroutines instead of the legacy concurrency model with manually managed Runnable tasks, simplifying the code and improving reliability. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2794">BAZEL-2794</a></li>
    <li>[fix] Remove redundant `bazel version` call during sync to reduce unnecessary overhead before the main build command. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2413">BAZEL-2413</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
</ul>
</body>
</html>
