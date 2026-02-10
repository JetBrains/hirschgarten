<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.2.12</title>
</head>
<body>
<h1>2025.2.12</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[fix] Correct for-loop variable scoping in reference resolution to properly recognize nested Starlark tuples. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2811">BAZEL-2811</a></li>
    <li>[fix] Updated test data to properly detect unused nested Starlark tuples. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2811">BAZEL-2811</a></li>
    <li>[fix] Nested Starlark tuples are now properly recognized in Bazel. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2811">BAZEL-2811</a></li>
    <li>[fix] Remove redundant `bazel version` call during sync to reduce unnecessary overhead before the main build command. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2413">BAZEL-2413</a></li>
    <li>[fix] Non-module targets now respect the `import_depth` setting in project view, preventing extra entries from appearing in the project tree when `import_depth: 0` is configured. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2763">BAZEL-2763</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
</ul>
</body>
</html>
