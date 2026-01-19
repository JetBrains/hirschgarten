<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.2.11</title>
</head>
<body>
<h1>2025.2.11</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>Non-module targets now respect the `import_depth` setting in project view, preventing extra entries from appearing in the project view tree when `import_depth: 0` is configured. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2763">BAZEL-2763</a></li>
    <li>Accept and ignore non-target &#x27;srcs&#x27; attributes to prevent crashes when rules use string-based srcs instead of label-based srcs. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2767">BAZEL-2767</a></li>
    <li>Combine invocations of `bazel mod show_repo` into a single call to reduce wall-clock time. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2780">BAZEL-2780</a></li>
    <li>Move temporary target-pattern file outside the .bazelbsp directory to prevent unnecessary regeneration and avoid invalidating Bazel&#x27;s analysis cache. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2773">BAZEL-2773</a></li>
    <li>Avoid unnecessary regeneration of BUILD file in .bazelbsp/aspects directory to prevent Bazel from invalidating its analysis cache unnecessarily. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2773">BAZEL-2773</a></li>
    <li>Pass parent environment variables to run targets to fix GUI applications failing on Linux due to missing DISPLAY/WAYLAND_DISPLAY variables, while keeping tests isolated without inherited environment. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2761">BAZEL-2761</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
    <li>Add diagnostic logging to help identify why debug gutter icons sometimes disappear while run icons remain visible, logging target kind, language classes, and rule type when debug is unavailable. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2292">BAZEL-2292</a></li>
</ul>
</body>
</html>
