<!DOCTYPE html>
<html lang="en">
<body>
<h1>Bazel Plugin 2024.3.10</h1>
<p>Throughout the 2025.1 release cycle, we&#39;ve published changes to the marketplace every couple of weeks. Here is an overview of the enhancements over the past 4 months. Read the full update on the <a href="https://blog.jetbrains.com/?post_type=idea&amp;p=559002">IntelliJ IDEA blog</a></p>
<h2 id="spotlight-features-bazel-8-faster-sync-python-and-scala">Spotlight features: Bazel 8, faster sync, Python, and Scala</h2>
<ul>
    <li>Full Bazel 8 and nested modules support</li>
    <li>Python and Scala support</li>
    <li>Experimental phased sync (enable it under <em>Settings | Build, Execution, Deployment | Build Tools | Bazel | Advanced Settings</em>)</li>
</ul>
<h2 id="get-started-faster">Get started faster</h2>
<ul>
    <li>Quick start project templates (2025.1 only)</li>
</ul>
<h2 id="better-integration">Better integration</h2>
<ul>
    <li>Improved JVM project structure handling</li>
    <li>Debugging and coverage for Bazel tests</li>
    <li>More informative JUnit 5 output</li>
</ul>
<h2 id="faster-flow">Faster flow</h2>
<ul>
    <li>Add dependencies directly from code</li>
    <li>Create Bazel packages from project tree context menu</li>
    <li>Navigate to <code>BUILD</code> files from source file context menu</li>
    <li>Bazel targets in <em>Search Everywhere</em></li>
    <li>Run multiple tests easily</li>
    <li>Clickable targets in the Sync console</li>
</ul>
<h2 id="starlark">Starlark</h2>
<ul>
    <li>In-editor quick documentation</li>
    <li>Comprehensive code completion</li>
    <li><code>glob</code> function expansion</li>
</ul>
<h2 id="-bazelproject-editor">.bazelproject editor</h2>
<ul>
    <li>Better highlighting</li>
    <li>Commenting shortcut</li>
    <li>Code style config</li>
</ul>
<h2 id="project-admin-tools">Project admin tools</h2>
<ul>
    <li>Shard sync for large projects</li>
    <li>Managed .bazelproject</li>
    <li>Import shared run configurations</li>
</ul>
<p>Please file any issue on our <a href="https://youtrack.jetbrains.com/issues/BAZEL">YouTrack</a> or reach us directly on slack <a href="https://bazelbuild.slack.com/archives/C025SBYFC4E">#intellij @ slack.bazel.build</a></p>
<h1>Changelog since 2024.3.10</h1>
<h3>Features</h3>
<ul>
    <li>[feature] Added Bazel target actions to the file tab context menu, allowing users to copy target IDs and jump to BUILD files directly from tab headers. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1801">BAZEL-1801</a></li>
    <li>[feature] Added ability to navigate directly to Java classes when referenced by name in BUILD files | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1796">BAZEL-1796</a></li>
    <li>[feature] Added auto-completion support for library names in BUILD files to improve developer productivity. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1868">BAZEL-1868</a></li>
    <li>[feature] Added autocomplete functionality for required Bazel rule parameters that have default values | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1857">BAZEL-1857</a></li>
    <li>[feature] Added code completion support for visibility specifications in BUILD files to help users specify package access permissions more easily | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1841">BAZEL-1841</a></li>
    <li>[feature] Added glob pattern expansion functionality to Starlark, allowing users to expand file patterns with command+click interaction | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1581">BAZEL-1581</a></li>
    <li>[feature] Added intelligent file path autocompletion in Starlark build files for source file attributes like 'src' and 'srcs', suggesting valid files from the current Bazel package. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1848">BAZEL-1848</a></li>
    <li>[feature] Added phased synchronization as an experimental user setting to provide more control over sync behavior | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1881">BAZEL-1881</a></li>
    <li>[feature] Added support for finding Bazel targets through the "Search everywhere" functionality | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1808">BAZEL-1808</a></li>
    <li>[feature] Added validation to flag missing required arguments in Bazel native rules | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1563">BAZEL-1563</a></li>
    <li>[feature] Enable Python support by default</li>
    <li>[feature] Enhanced JUnit 5 test output to include complete multi-line exception messages for better error diagnosis | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1872">BAZEL-1872</a></li>
    <li>[feature] Enhanced ProjectView editor with code styling, comment shortcuts, and smart indentation handling | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1566">BAZEL-1566</a></li>
</ul>
<h3>Bug Fixes</h3>
<ul>
    <li>[fix] Added "Jump to BUILD file" option to target widget context menu for improved file navigation accessibility | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1801">BAZEL-1801</a></li>
    <li>[fix] Disabled the "Add dependency" functionality in non-Bazel projects to prevent incorrect usage | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1890">BAZEL-1890</a></li>
    <li>[fix] Disabled the "New Bazel Package" action in non-Bazel projects to prevent incorrect usage | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1769">BAZEL-1769</a></li>
    <li>[fix] Ensured consistent base directory calculation during phased synchronization by making the baseDirectory parameter non-nullable | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-635">BAZEL-635</a></li>
    <li>[fix] Filter out interface jars (best effort) out of runtime classpath for junit configuration</li>
    <li>[fix] Fixed inconsistent display of "Run all tests" option across different project directories and locations | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1878">BAZEL-1878</a></li>
    <li>[fix] Fixed synchronization order between JVM and Bazel repository mapping hooks to ensure proper target completion functionality. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-176">BAZEL-176</a></li>
    <li>[fix] Improved the user interface for creating new Bazel packages to better align with standard patterns and workflows | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-177">BAZEL-177</a></li>
    <li>[fix] Prevent stopping other enter handlers from executing their logic</li>
    <li>[fix] Prevented the .bazelproject file from being excluded from the project view to ensure it remains accessible | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1876">BAZEL-1876</a></li>
    <li>[fix] Standardized BUILD file naming convention across project wizard and new Bazel package creation | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1874">BAZEL-1874</a></li>
    <li>[fix] Updated BazelRunner to terminate processes in a way that works across different operating systems, including Windows. | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1847">BAZEL-1847</a></li>
    <li>[fix] Updated code to properly obtain workspace name using ctx.workspace_name instead of hardcoding it as 'main' | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1836">BAZEL-1836</a></li>
    <li>[fix] Updated file path handling to prevent exceptions when processing Git diff files by using a more robust path conversion method | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1812">BAZEL-1812</a></li>
    <li>[fix] Expand envs and expand make variables for jvm flags</li>
    <li>[fix] Wrap `PsiElement#parent` in `runReadAction` as it is read-intended</li>
    <li>[fix] Don't expand make variables during sync by default | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1812">BAZEL-1894</a></li>
    <li>[fix] Fix read access exception with LabelSearchEverywhereContributor | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1812">BAZEL-1808</a></li>
</ul>
<h3>Maintenance</h3>
<ul>
    <li>[maintenance] Move hotswap functionality to experimental settings while a better user experience is being developed | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1891">BAZEL-1891</a></li>
    <li>[maintenance] Reorganized experimental settings into a separate dedicated panel to improve settings organization and clarity | #<a href="https://youtrack.jetbrains.com/issue/BAZEL-1863">BAZEL-1863</a></li>
</ul>
</body>
</html>