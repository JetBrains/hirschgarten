<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.2.7</title>
</head>
<body>
<h1>2025.2.7</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[feature] Added support for @rules_java//toolchains:incompatible_language_version_bootclasspath flag to ensure Java toolchain info takes precedence over runtime info when determining project language level | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2521">BAZEL-2521</a></li>
    <li>[fix] Fixed &quot;Navigate to source&quot; functionality for JVM tests in the test runner | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1898">BAZEL-1898</a></li>
    <li>[bazel] Fix spelling in Starlark glob error notification</li>
    <li>[bazel] Fix spelling in bazel query notifications</li>
    <li>[feature] Added navigation to test cases from test runner and test status icons | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1898">BAZEL-1898</a> | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1938">BAZEL-1938</a></li>
    <li>[fix] Resolved NoSuchElementException when adding directories to project view with an empty directories section | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2263">BAZEL-2263</a></li>
    <li>[fleet] update kotlinx.coroutines</li>
    <li>[fix] Added missing runnable, sourceless targets to workspace to enable debug build execution from hirschgarten | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2553">BAZEL-2553</a></li>
    <li>[fix] Added output jar libraries for custom kinds without sources to ensure generated code is properly recognized | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2609">BAZEL-2609</a></li>
    <li>[fix] Corrected project language level to use Java toolchain source version instead of runtime JDK version | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2521">BAZEL-2521</a></li>
    <li>[fix] Properly handle project view flags to prevent mangling of recognized flags and filtering of unrecognized flags in sync and build commands | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2613">BAZEL-2613</a></li>
    <li>[bazel][maintenance] update test repository URL and commit hash in ProtobufResolveTest configuration</li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
</ul>
</body>
</html>
