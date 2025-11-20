<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.2.8</title>
</head>
<body>
<h1>2025.2.8</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[fix] Removed accidental cherry-pick to plugin.xml preventing extension creation exception after IDE restart | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2664">BAZEL-2664</a></li>
    <li>[fix] Catch NoSuchFileException in JavaModuleToDummyJavaModulesTransformerHACK to prevent sync failures when file tree walking encounters missing directories. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1908">BAZEL-1908</a></li>
    <li>[fix] Fixed import_depth configuration for Go and Python, and added documentation to default .bazelproject file to make shallow import option more discoverable | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2655">BAZEL-2655</a></li>
    <li>[fix] Restored marking of generated sources after regression that caused performance issues by iterating through bazel-out directory | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2657">BAZEL-2657</a></li>
    <li>[fix] Properly indent sample code in wizard-generated Java Bazel projects to avoid immediate formatting changes | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2633">BAZEL-2633</a></li>
    <li>[fix] Configured Java language version and toolchain in generated Bazel projects to ensure they build successfully out of the box | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2627">BAZEL-2627</a></li>
    <li>[fix] Fixed unresponsive &quot;Build target&quot; field when manually creating Bazel run configurations | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1104">BAZEL-1104</a></li>
    <li>[fix] Fixed glob validation to respect Bazel version, preventing false empty glob errors on versions prior to 8.0 where allow_empty defaults to true | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2645">BAZEL-2645</a></li>
    <li>[fix] Resolved JavaTestLocator error preventing Bazel py_test targets from running in PyCharm by correcting extension point configuration in withJava.xml | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2649">BAZEL-2649</a></li>
    <li>[feature] Added support for @rules_java//toolchains:incompatible_language_version_bootclasspath flag to ensure Java toolchain info takes precedence over runtime info when determining project language level | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2521">BAZEL-2521</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
</ul>
</body>
</html>
