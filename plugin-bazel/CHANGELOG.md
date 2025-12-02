<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bazel Plugin 2025.3.1</title>
</head>
<body>
<h1>2025.3.1</h1>
<p>RELEASE NOTES</p>
<p>Please consider switching to Nightly channel in <code>Settings -> Build, Execution, Deployment -> Build Tools -> Bazel -> Advanced Settings</code><br>
Switching to nightly you'll get newest features and fixes faster!</p>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h2>Changelog:</h2>
<h3>Features</h3>
<ul>
    <li>[fix] Corrected aspect label reference in Bazel plugin&#x27;s Scala aspect to resolve sync failures on Scala projects | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2716">BAZEL-2716</a></li>
    <li>[fix] Corrected Windows path escaping in generated utils.bzl to prevent invalid escape sequence errors during import | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2710">BAZEL-2710</a></li>
    <li>[feature] Enabled --script_path for all JVM debug configurations and added build error parsing for &#x27;bazel run&#x27; commands. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2575">BAZEL-2575</a></li>
    <li>[fix] Improved script path handling for environment variables and removed working directory configuration to prevent timeouts across all debug configurations | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2575">BAZEL-2575</a></li>
    <li>[fix] Added kotlin-stdlib dependency to Java modules that depend on Kotlin modules to resolve missing search scope access for Kotlin standard library elements | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2642">BAZEL-2642</a></li>
    <li>[fix] Added error handling to detect when .bazelbsp directory is listed in .bazelignore to prevent analysis invalidation issues | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2677">BAZEL-2677</a></li>
    <li>[fix] Removed --inject_repository flag to prevent analysis invalidation when building from command line | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2677">BAZEL-2677</a></li>
    <li>[fix] Fixed Bazel project root detection when historicalProjectBasePath is not a direct parent of the project path | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[maintenance] Extracted format parameter to improve code readability | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[fix] Resolved open-source build failure caused by missing BUILD file in server/resources/aspects directory | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2698">BAZEL-2698</a></li>
    <li>[fix] Removed duplicated error diagnostics from sync console output | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2610">BAZEL-2610</a></li>
    <li>[fix] Removed intellij.bazel.server conversion from ignore list | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2663">BAZEL-2663</a></li>
    <li>[maintenance] Cleaned Bazel plugin distribution by removing unnecessary dependencies except k2 from packaged build | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2663">BAZEL-2663</a></li>
    <li>[maintenance] Cleaned Bazel plugin distribution by removing unnecessary dependencies except k2 from packaged build | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2663">BAZEL-2663</a></li>
    <li>[fix] Fixed IDE error when importing run configurations with excluded targets that were not imported | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2686">BAZEL-2686</a></li>
    <li>[fix] Resolved race condition in InitProjectActivity that caused multiple EditorTabs instances to be created, resulting in unclickable editor UI | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2684">BAZEL-2684</a></li>
    <li>[feature] Parse system-out in test cases and add tests for parsing to improve test output consistency | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2255">BAZEL-2255</a></li>
    <li>[fix] Added exception stacktraces and adjusted logic to mark only failed tests as failed instead of all tests | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2255">BAZEL-2255</a></li>
    <li>[maintenance] Inlined method to simplify code structure in Bazel project handling | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[feature] Added Path extension utility methods hasNameOf and hasExtensionOf for improved file path operations. | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[maintenance] Replaced VirtualFiles operations with pure NIO operations to improve project root calculation and file handling in Bazel projects | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[maintenance] Inlined Kotlin extension functions for improved code clarity | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[feature] Added label completion support for use_extension in Bazel module files | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1830">BAZEL-1830</a></li>
    <li>[fix] Preserved run configuration behavior for 252 build | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2469">BAZEL-2469</a></li>
    <li>[maintenance] Updated plugin run configuration to support plugin model v2 | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2469">BAZEL-2469</a></li>
    <li>[maintenance] Removed granularity to align module structure with autogenerated BUILD files, added plugin v2 model support, and bumped version to 253 | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2469">BAZEL-2469</a></li>
    <li>[fix] Prevented run configuration changes from being applied immediately after serialization | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2651">BAZEL-2651</a></li>
    <li>[feature] Added references for use_extension in Bazel module files to improve code navigation and intelligence | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1830">BAZEL-1830</a></li>
    <li>PY-57566 Support PEP 660 editable installs.</li>
    <li>[bazel] Fix non-test gutters after revision 2a24ea341968a6d019ae51a33ef1461dbd182ecd</li>
    <li>[fix] Removed compile dependency from aggregator module to resolve classloading issue causing PyCharm to fail when importing projects | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2641">BAZEL-2641</a></li>
    <li>[fix] Resolved missing gRPC runtime dependency causing ClassNotFoundException when loading Bazel plugin in PyCharm | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2641">BAZEL-2641</a></li>
    <li>[fix] Prevented IllegalStateException when formatting Starlark files before Bazel project root directory is initialized | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2626">BAZEL-2626</a></li>
    <li>[fix] Added missing isBazelProject check to prevent IllegalStateException when building non-Bazel projects | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2634">BAZEL-2634</a></li>
    <li>[fix] Fixed import_depth configuration for Go and Python, and added documentation to default .bazelproject file to make shallow import option more discoverable | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2655">BAZEL-2655</a></li>
    <li>[fix] Catch NoSuchFileException in JavaModuleToDummyJavaModulesTransformerHACK to prevent sync failures when file tree walking encounters missing directories. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1908">BAZEL-1908</a></li>
    <li>[fix] Restored marking of generated sources after regression that caused performance issues by iterating through bazel-out directory | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2657">BAZEL-2657</a></li>
    <li>[feature] Added support for @rules_java//toolchains:incompatible_language_version_bootclasspath flag to ensure Java toolchain info takes precedence over runtime info when determining project language level | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2521">BAZEL-2521</a></li>
    <li>[fix] Corrected run configurations serialization to prevent infinite duplication of parameter entries | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2640">BAZEL-2640</a></li>
    <li>[fix] Added bazel convenience symlinks to .gitignore to prevent unclean repository state in newly created projects | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2633">BAZEL-2633</a></li>
    <li>[fix] Properly indent sample code in wizard-generated Java Bazel projects to avoid immediate formatting changes | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2633">BAZEL-2633</a></li>
    <li>[bazel] Fix spelling in bazel query notifications</li>
    <li>[feature] Implemented find usages functionality for files and folders in glob functions | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2048">BAZEL-2048</a></li>
    <li>[bazel] Fix spelling in Starlark glob error notification</li>
    <li>[fix] Configured Java language version and toolchain in generated Bazel projects to ensure they build successfully out of the box | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2627">BAZEL-2627</a></li>
    <li>[fix] Ensured project check is performed first to properly calculate project root when opening Bazel projects via file selection instead of directory selection. | #<a href="https://youtrack.jetbrains.com/issue/IJPL-216121">IJPL-216121</a></li>
    <li>[fix] Fixed Git VCS mapping registration after project import by updating BazelProjectStorePathCustomizer to consider BUILD files | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2589">BAZEL-2589</a> | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2528">BAZEL-2528</a></li>
    <li>[fix] Fixed glob validation to respect Bazel version, preventing false empty glob errors on versions prior to 8.0 where allow_empty defaults to true | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-2645">BAZEL-2645</a></li>
    <li>[fix] Fixed unresponsive &quot;Build target&quot; field when manually creating Bazel run configurations | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1104">BAZEL-1104</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
</ul>
<h3>Maintenance</h3>
<ul>
</ul>
</body>
</html>
