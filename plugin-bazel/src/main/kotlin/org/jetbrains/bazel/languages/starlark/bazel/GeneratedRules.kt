package org.jetbrains.bazel.languages.starlark.bazel
val RULES = listOf(setOf(BazelNativeRule("cc_tool","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("src","""None""",false,"""The underlying binary that this tool represents.

Usually just a single prebuilt (eg. @toolchain//:bin/clang), but may be any
executable label."""),BazelNativeRuleArgument("data","""[]""",false,"""Additional files that are required for this tool to run.

Frequently, clang and gcc require additional files to execute as they often shell out to
other binaries (e.g. <code>cc1</code>)."""),BazelNativeRuleArgument("allowlist_include_directories","""[]""",false,"""Include paths implied by using this tool.

Compilers may include a set of built-in headers that are implicitly available
unless flags like <code>-nostdinc</code> are provided. Bazel checks that all included
headers are properly provided by a dependency or allowlisted through this
mechanism.

As a rule of thumb, only use this if Bazel is complaining about absolute paths in your
toolchain and you've ensured that the toolchain is compiling with the <code>-no-canonical-prefixes</code>
and/or <code>-fno-canonical-system-headers</code> arguments.

This can help work around errors like:
<code>the source file 'main.c' includes the following non-builtin files with absolute paths
(if these are builtin files, make sure these paths are in your toolchain)</code>."""),BazelNativeRuleArgument("capabilities","""[]""",false,"""Declares that a tool is capable of doing something.

For example, <code>@rules_cc//cc/toolchains/capabilities:supports_pic</code>."""),),"""Declares a tool for use by toolchain actions.

<code>cc_tool</code> rules are used in a <code>cc_tool_map</code> rule to ensure all files and
metadata required to run a tool are available when constructing a <code>cc_toolchain</code>.

In general, include all files that are always required to run a tool (e.g. libexec/** and
cross-referenced tools in bin/*) in the data attribute. If some files are only
required when certain flags are passed to the tool, consider using a <code>cc_args</code> rule to
bind the files to the flags that require them. This reduces the overhead required to properly
enumerate a sandbox with all the files required to run a tool, and ensures that there isn't
unintentional leakage across configurations and actions.

Example:
<code>
load("//cc/toolchains:tool.bzl", "cc_tool")

cc_tool(
    name = "clang_tool",
    src = "@llvm_toolchain//:bin/clang",
    # Suppose clang needs libc to run.
    data = ["@llvm_toolchain//:lib/x86_64-linux-gnu/libc.so.6"]
    tags = ["requires-network"],
    capabilities = ["//cc/toolchains/capabilities:supports_pic"],
)
</code>"""),BazelNativeRule("cc_tool_capability","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("feature_name","""""""",false,"""The name of the feature to generate for this capability"""),),"""A capability is an optional feature that a tool supports.

For example, not all compilers support PIC, so to handle this, we write:

<code>
cc_tool(
    name = "clang",
    src = "@host_tools/bin/clang",
    capabilities = [
        "//cc/toolchains/capabilities:supports_pic",
    ],
)

cc_args(
    name = "pic",
    requires = [
        "//cc/toolchains/capabilities:supports_pic"
    ],
    args = ["-fPIC"],
)
</code>

This ensures that <code>-fPIC</code> is added to the command-line only when we are using a
tool that supports PIC."""),BazelNativeRule("cc_args_list","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("args","""[]""",false,"""(ordered) cc_args to include in this list."""),),"""An ordered list of cc_args.

This is a convenience rule to allow you to group a set of multiple <code>cc_args</code> into a
single list. This particularly useful for toolchain behaviors that require different flags for
different actions.

Note: The order of the arguments in <code>args</code> is preserved to support order-sensitive flags.

Example usage:
<code>
load("//cc/toolchains:cc_args.bzl", "cc_args")
load("//cc/toolchains:args_list.bzl", "cc_args_list")

cc_args(
    name = "gc_sections",
    actions = [
        "//cc/toolchains/actions:link_actions",
    ],
    args = ["-Wl,--gc-sections"],
)

cc_args(
    name = "function_sections",
    actions = [
        "//cc/toolchains/actions:compile_actions",
        "//cc/toolchains/actions:link_actions",
    ],
    args = ["-ffunction-sections"],
)

cc_args_list(
    name = "gc_functions",
    args = [
        ":function_sections",
        ":gc_sections",
    ],
)
</code>"""),BazelNativeRule("cc_action_type","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("action_name","""""""",true,""""""),),"""A type of action (eg. c_compile, assemble, strip).

<code>cc_action_type</code> rules are used to associate arguments and tools together to
perform a specific action. Bazel prescribes a set of known action types that are used to drive
typical C/C++/ObjC actions like compiling, linking, and archiving. The set of well-known action
types can be found in @rules_cc//cc/toolchains/actions:BUILD.

It's possible to create project-specific action types for use in toolchains. Be careful when
doing this, because every toolchain that encounters the action will need to be configured to
support the custom action type. If your project is a library, avoid creating new action types as
it will reduce compatibility with existing toolchains and increase setup complexity for users.

Example:
<code>
load("//cc:action_names.bzl", "ACTION_NAMES")
load("//cc/toolchains:actions.bzl", "cc_action_type")

cc_action_type(
    name = "cpp_compile",
    action_name =  = ACTION_NAMES.cpp_compile,
)
</code>"""),BazelNativeRule("cc_action_type_set","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("actions","""""""",true,"""A list of cc_action_type or cc_action_type_set"""),BazelNativeRuleArgument("allow_empty","""False""",false,""""""),),"""Represents a set of actions.

This is a convenience rule to allow for more compact representation of a group of action types.
Use this anywhere a <code>cc_action_type</code> is accepted.

Example:
<code>
load("//cc/toolchains:actions.bzl", "cc_action_type_set")

cc_action_type_set(
    name = "link_executable_actions",
    actions = [
        "//cc/toolchains/actions:cpp_link_executable",
        "//cc/toolchains/actions:lto_index_for_executable",
    ],
)
</code>"""),BazelNativeRule("cc_feature","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("feature_name","""""""",false,"""The name of the feature that this rule implements.

The feature name is a string that will be used in the <code>features</code> attribute of
rules to enable them (eg. <code>cc_binary(..., features = ["opt"])</code>.

While two features with the same <code>feature_name</code> may not be bound to the same
toolchain, they can happily live alongside each other in the same BUILD file.

Example:
<code>
cc_feature(
    name = "sysroot_macos",
    feature_name = "sysroot",
    ...
)

cc_feature(
    name = "sysroot_linux",
    feature_name = "sysroot",
    ...
)
</code>"""),BazelNativeRuleArgument("args","""[]""",false,"""A list of <code>cc_args</code> or <code>cc_args_list</code> labels that are expanded when this feature is enabled."""),BazelNativeRuleArgument("requires_any_of","""[]""",false,"""A list of feature sets that define toolchain compatibility.

If *at least one* of the listed <code>cc_feature_set</code>s are fully satisfied (all
features exist in the toolchain AND are currently enabled), this feature is
deemed compatible and may be enabled.

Note: Even if <code>cc_feature.requires_any_of</code> is satisfied, a feature is not
enabled unless another mechanism (e.g. command-line flags, <code>cc_feature.implies</code>,
<code>cc_toolchain_config.enabled_features</code>) signals that the feature should actually
be enabled."""),BazelNativeRuleArgument("implies","""[]""",false,"""List of features enabled along with this feature.

Warning: If any of the features cannot be enabled, this feature is
silently disabled."""),BazelNativeRuleArgument("mutually_exclusive","""[]""",false,"""A list of things that this feature is mutually exclusive with.

It can be either:
* A feature, in which case the two features are mutually exclusive.
* A <code>cc_mutually_exclusive_category</code>, in which case all features that write
    <code>mutually_exclusive = [":category"]</code> are mutually exclusive with each other.

If this feature has a side-effect of implementing another feature, it can be
useful to list that feature here to ensure they aren't enabled at the same time."""),BazelNativeRuleArgument("overrides","""None""",false,"""A declaration that this feature overrides a known feature.

In the example below, if you missed the "overrides" attribute, it would complain
that the feature "opt" was defined twice.

Example:
<code>
load("//cc/toolchains:feature.bzl", "cc_feature")

cc_feature(
    name = "opt",
    feature_name = "opt",
    args = [":size_optimized"],
    overrides = "//cc/toolchains/features:opt",
)
</code>"""),),"""A dynamic set of toolchain flags that create a singular feature definition.

A feature is basically a dynamically toggleable <code>cc_args_list</code>. There are a variety of
dependencies and compatibility requirements that must be satisfied to enable a
<code>cc_feature</code>. Once those conditions are met, the arguments in <code>cc_feature.args</code>
are expanded and added to the command-line.

A feature may be enabled or disabled through the following mechanisms:
* Via command-line flags, or a <code>.bazelrc</code> file via the
  <code>--features</code> flag
* Through inter-feature relationships (via <code>cc_feature.implies</code>) where one
  feature may implicitly enable another.
* Individual rules (e.g. <code>cc_library</code>) or <code>package</code> definitions may elect to manually enable or
  disable features through the
  <code>features</code> attribute.

Note that a feature may alternate between enabled and disabled dynamically over the course of a
build. Because of their toggleable nature, it's generally best to avoid adding arguments to a
<code>cc_toolchain</code> as a <code>cc_feature</code> unless strictly necessary. Instead, prefer to express arguments
via <code>cc_toolchain.args</code> whenever possible.

You should use a <code>cc_feature</code> when any of the following apply:
* You need the flags to be dynamically toggled over the course of a build.
* You want build files to be able to configure the flags in question. For example, a
  binary might specify <code>features = ["optimize_for_size"]</code> to create a small
  binary instead of optimizing for performance.
* You need to carry forward Starlark toolchain behaviors. If you're migrating a
  complex Starlark-based toolchain definition to these rules, many of the
  workflows and flags were likely based on features.

If you only need to configure flags via the Bazel command-line, instead
consider adding a
<code>bool_flag</code>
paired with a <code>config_setting</code>
and then make your <code>cc_args</code> rule <code>select</code> on the <code>config_setting</code>.

For more details about how Bazel handles features, see the official Bazel
documentation at
https://bazel.build/docs/cc-toolchain-config-reference#features.

Example:
<code>
load("//cc/toolchains:feature.bzl", "cc_feature")

# A feature that enables LTO, which may be incompatible when doing interop with various
# languages (e.g. rust, go), or may need to be disabled for particular <code>cc_binary</code> rules
# for various reasons.
cc_feature(
    name = "lto",
    feature_name = "lto",
    args = [":lto_args"],
)
</code>"""),BazelNativeRule("cc_feature_constraint","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("all_of","""[]""",false,""""""),BazelNativeRuleArgument("none_of","""[]""",false,""""""),),"""Defines a compound relationship between features.

This rule can be used with <code>cc_args.require_any_of</code> to specify that a set
of arguments are only enabled when a constraint is met. Both <code>all_of</code> and <code>none_of</code> must be
satisfied simultaneously.

This is basically a <code>cc_feature_set</code> that supports <code>none_of</code> expressions. This extra flexibility
is why this rule may only be used by <code>cc_args.require_any_of</code>.

Example:
<code>
load("//cc/toolchains:feature_constraint.bzl", "cc_feature_constraint")

# A constraint that requires a <code>linker_supports_thinlto</code> feature to be enabled,
# AND a <code>no_optimization</code> to be disabled.
cc_feature_constraint(
    name = "thinlto_constraint",
    all_of = [":linker_supports_thinlto"],
    none_of = [":no_optimization"],
)
</code>"""),BazelNativeRule("cc_feature_set","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("all_of","""[]""",false,"""A set of features"""),),"""Defines a set of features.

This may be used by both <code>cc_feature</code> and <code>cc_args</code> rules, and is effectively a way to express
a logical <code>AND</code> operation across multiple required features.

Example:
<code>
load("//cc/toolchains:feature_set.bzl", "cc_feature_set")

cc_feature_set(
    name = "thin_lto_requirements",
    all_of = [
        ":thin_lto",
        ":opt",
    ],
)
</code>"""),BazelNativeRule("cc_mutually_exclusive_category","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),),"""A rule used to categorize <code>cc_feature</code> definitions for which only one can be enabled.

This is used by <code>cc_feature.mutually_exclusive</code> to express groups
of <code>cc_feature</code> definitions that are inherently incompatible with each other and must be treated as
mutually exclusive.

Warning: These groups are keyed by name, so two <code>cc_mutually_exclusive_category</code> definitions of the
same name in different packages will resolve to the same logical group.

Example:
<code>
load("//cc/toolchains:feature.bzl", "cc_feature")
load("//cc/toolchains:mutually_exclusive_category.bzl", "cc_mutually_exclusive_category")

cc_mutually_exclusive_category(
    name = "opt_level",
)

cc_feature(
    name = "speed_optimized",
    mutually_exclusive = [":opt_level"],
)

cc_feature(
    name = "size_optimized",
    mutually_exclusive = [":opt_level"],
)

cc_feature(
    name = "unoptimized",
    mutually_exclusive = [":opt_level"],
)
</code>"""),BazelNativeRule("cc_external_feature","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("feature_name","""""""",true,"""The name of the feature"""),BazelNativeRuleArgument("overridable","""""""",true,"""Whether the feature can be overridden"""),),"""A declaration that a feature with this name is defined elsewhere.

This rule communicates that a feature has been defined externally to make it possible to reference
features that live outside the rule-based cc toolchain ecosystem. This allows various toolchain
rules to reference the external feature without accidentally re-defining said feature.

This rule is currently considered a private API of the toolchain rules to encourage the Bazel
ecosystem to migrate to properly defining their features as rules.

Example:
<code>
load("//cc/toolchains:external_feature.bzl", "cc_external_feature")

# rules_rust defines a feature that is disabled whenever rust artifacts are being linked using
# the cc toolchain to signal that incompatible flags should be disabled as well.
cc_external_feature(
    name = "rules_rust_unsupported_feature",
    feature_name = "rules_rust_unsupported_feature",
    overridable = False,
)
</code>"""),),setOf(BazelNativeRule("java_binary","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("srcs","""[]""",false,"""The list of source files that are processed to create the target.
This attribute is almost always required; see exceptions below.
<p>
Source files of type <code>.java</code> are compiled. In case of generated
<code>.java</code> files it is generally advisable to put the generating rule's name
here instead of the name of the file itself. This not only improves readability but
makes the rule more resilient to future changes: if the generating rule generates
different files in the future, you only need to fix one place: the <code>outs</code> of
the generating rule. You should not list the generating rule in <code>deps</code>
because it is a no-op.
</p>
<p>
Source files of type <code>.srcjar</code> are unpacked and compiled. (This is useful if
you need to generate a set of <code>.java</code> files with a genrule.)
</p>
<p>
Rules: if the rule (typically <code>genrule</code> or <code>filegroup</code>) generates
any of the files listed above, they will be used the same way as described for source
files.
</p>

<p>
This argument is almost always required, except if a
<code>main_class</code> attribute specifies a
class on the runtime classpath or you specify the <code>runtime_deps</code> argument.
</p>"""),BazelNativeRuleArgument("deps","""[]""",false,"""The list of other libraries to be linked in to the target.
See general comments about <code>deps</code> at
Typical attributes defined by
most build rules."""),BazelNativeRuleArgument("resources","""[]""",false,"""A list of data files to include in a Java jar.

<p>
Resources may be source files or generated files.
</p>

<p>
If resources are specified, they will be bundled in the jar along with the usual
<code>.class</code> files produced by compilation. The location of the resources inside
of the jar file is determined by the project structure. Bazel first looks for Maven's
standard directory layout,
(a "src" directory followed by a "resources" directory grandchild). If that is not
found, Bazel then looks for the topmost directory named "java" or "javatests" (so, for
example, if a resource is at <code>&lt;workspace root&gt;/x/java/y/java/z</code>, the
path of the resource will be <code>y/java/z</code>. This heuristic cannot be overridden,
however, the <code>resource_strip_prefix</code> attribute can be used to specify a
specific alternative directory for resource files."""),BazelNativeRuleArgument("runtime_deps","""[]""",false,"""Libraries to make available to the final binary or test at runtime only.
Like ordinary <code>deps</code>, these will appear on the runtime classpath, but unlike
them, not on the compile-time classpath. Dependencies needed only at runtime should be
listed here. Dependency-analysis tools should ignore targets that appear in both
<code>runtime_deps</code> and <code>deps</code>."""),BazelNativeRuleArgument("data","""[]""",false,"""The list of files needed by this library at runtime.
See general comments about <code>data</code>
at Typical attributes defined by
most build rules."""),BazelNativeRuleArgument("plugins","""[]""",false,"""Java compiler plugins to run at compile-time.
Every <code>java_plugin</code> specified in this attribute will be run whenever this rule
is built. A library may also inherit plugins from dependencies that use
<code>exported_plugins</code>. Resources
generated by the plugin will be included in the resulting jar of this rule."""),BazelNativeRuleArgument("deploy_env","""[]""",false,"""A list of other <code>java_binary</code> targets which represent the deployment
environment for this binary.
Set this attribute when building a plugin which will be loaded by another
<code>java_binary</code>.<br/> Setting this attribute excludes all dependencies from
the runtime classpath (and the deploy jar) of this binary that are shared between this
binary and the targets specified in <code>deploy_env</code>."""),BazelNativeRuleArgument("launcher","""None""",false,"""Specify a binary that will be used to run your Java program instead of the
normal <code>bin/java</code> program included with the JDK.
The target must be a <code>cc_binary</code>. Any <code>cc_binary</code> that
implements the

Java Invocation API can be specified as a value for this attribute.

<p>By default, Bazel will use the normal JDK launcher (bin/java or java.exe).</p>

<p>The related <code>
--java_launcher</code> Bazel flag affects only those
<code>java_binary</code> and <code>java_test</code> targets that have
<i>not</i> specified a <code>launcher</code> attribute.</p>

<p>Note that your native (C++, SWIG, JNI) dependencies will be built differently
depending on whether you are using the JDK launcher or another launcher:</p>

<ul>
<li>If you are using the normal JDK launcher (the default), native dependencies are
built as a shared library named <code>{name}_nativedeps.so</code>, where
<code>{name}</code> is the <code>name</code> attribute of this java_binary rule.
Unused code is <em>not</em> removed by the linker in this configuration.</li>

<li>If you are using any other launcher, native (C++) dependencies are statically
linked into a binary named <code>{name}_nativedeps</code>, where <code>{name}</code>
is the <code>name</code> attribute of this java_binary rule. In this case,
the linker will remove any code it thinks is unused from the resulting binary,
which means any C++ code accessed only via JNI may not be linked in unless
that <code>cc_library</code> target specifies <code>alwayslink = True</code>.</li>
</ul>

<p>When using any launcher other than the default JDK launcher, the format
of the <code>*_deploy.jar</code> output changes. See the main
java_binary docs for details.</p>"""),BazelNativeRuleArgument("bootclasspath","""None""",false,"""Restricted API, do not use!"""),BazelNativeRuleArgument("neverlink","""False""",false,""""""),BazelNativeRuleArgument("javacopts","""[]""",false,"""Extra compiler options for this binary.
Subject to "Make variable" substitution and
Bourne shell tokenization.
<p>These compiler options are passed to javac after the global compiler options.</p>"""),BazelNativeRuleArgument("add_exports","""[]""",false,"""Allow this library to access the given <code>module</code> or <code>package</code>.
<p>
This corresponds to the javac and JVM --add-exports= flags."""),BazelNativeRuleArgument("add_opens","""[]""",false,"""Allow this library to reflectively access the given <code>module</code> or
<code>package</code>.
<p>
This corresponds to the javac and JVM --add-opens= flags."""),BazelNativeRuleArgument("main_class","""""""",false,"""Name of class with <code>main()</code> method to use as entry point.
If a rule uses this option, it does not need a <code>srcs=[...]</code> list.
Thus, with this attribute one can make an executable from a Java library that already
contains one or more <code>main()</code> methods.
<p>
The value of this attribute is a class name, not a source file. The class must be
available at runtime: it may be compiled by this rule (from <code>srcs</code>) or
provided by direct or transitive dependencies (through <code>runtime_deps</code> or
<code>deps</code>). If the class is unavailable, the binary will fail at runtime; there
is no build-time check.
</p>"""),BazelNativeRuleArgument("jvm_flags","""[]""",false,"""A list of flags to embed in the wrapper script generated for running this binary.
Subject to $(location) and
"Make variable" substitution, and
Bourne shell tokenization.

<p>The wrapper script for a Java binary includes a CLASSPATH definition
(to find all the dependent jars) and invokes the right Java interpreter.
The command line generated by the wrapper script includes the name of
the main class followed by a <code>"$@"</code> so you can pass along other
arguments after the classname.  However, arguments intended for parsing
by the JVM must be specified <i>before</i> the classname on the command
line.  The contents of <code>jvm_flags</code> are added to the wrapper
script before the classname is listed.</p>

<p>Note that this attribute has <em>no effect</em> on <code>*_deploy.jar</code>
outputs.</p>"""),BazelNativeRuleArgument("deploy_manifest_lines","""[]""",false,"""A list of lines to add to the <code>META-INF/manifest.mf</code> file generated for the
<code>*_deploy.jar</code> target. The contents of this attribute are <em>not</em> subject
to "Make variable" substitution."""),BazelNativeRuleArgument("stamp","""-1""",false,"""Whether to encode build information into the binary. Possible values:
<ul>
<li>
  <code>stamp = 1</code>: Always stamp the build information into the binary, even in
  <code>--nostamp</code> builds. <b>This
  setting should be avoided</b>, since it potentially kills remote caching for the
  binary and any downstream actions that depend on it.
</li>
<li>
  <code>stamp = 0</code>: Always replace build information by constant values. This
  gives good build result caching.
</li>
<li>
  <code>stamp = -1</code>: Embedding of build information is controlled by the
  <code>--[no]stamp</code> flag.
</li>
</ul>
<p>Stamped binaries are <em>not</em> rebuilt unless their dependencies change.</p>"""),BazelNativeRuleArgument("use_testrunner","""False""",false,"""Use the test runner (by default
<code>com.google.testing.junit.runner.BazelTestRunner</code>) class as the
main entry point for a Java program, and provide the test class
to the test runner as a value of <code>bazel.test_suite</code>
system property.

<br/>
You can use this to override the default
behavior, which is to use test runner for
<code>java_test</code> rules,
and not use it for <code>java_binary</code> rules.  It is unlikely
you will want to do this.  One use is for <code>AllTest</code>
rules that are invoked by another rule (to set up a database
before running the tests, for example).  The <code>AllTest</code>
rule must be declared as a <code>java_binary</code>, but should
still use the test runner as its main entry point.

The name of a test runner class can be overridden with <code>main_class</code> attribute."""),BazelNativeRuleArgument("use_launcher","""True""",false,"""Whether the binary should use a custom launcher.

<p>If this attribute is set to false, the
launcher attribute  and the related
<code>--java_launcher</code> flag
will be ignored for this target."""),BazelNativeRuleArgument("env","""{}""",false,""""""),BazelNativeRuleArgument("classpath_resources","""[]""",false,"""<em class="harmful">DO NOT USE THIS OPTION UNLESS THERE IS NO OTHER WAY)</em>
<p>
A list of resources that must be located at the root of the java tree. This attribute's
only purpose is to support third-party libraries that require that their resources be
found on the classpath as exactly <code>"myconfig.xml"</code>. It is only allowed on
binaries and not libraries, due to the danger of namespace conflicts.
</p>"""),BazelNativeRuleArgument("licenses","""[]""",false,""""""),BazelNativeRuleArgument("create_executable","""True""",false,"""Deprecated, use <code>java_single_jar</code> instead."""),BazelNativeRuleArgument("resource_strip_prefix","""""""",false,"""The path prefix to strip from Java resources.
<p>
If specified, this path prefix is stripped from every file in the <code>resources</code>
attribute. It is an error for a resource file not to be under this directory. If not
specified (the default), the path of resource file is determined according to the same
logic as the Java package of source files. For example, a source file at
<code>stuff/java/foo/bar/a.txt</code> will be located at <code>foo/bar/a.txt</code>.
</p>"""),),"""<p>
  Builds a Java archive ("jar file"), plus a wrapper shell script with the same name as the rule.
  The wrapper shell script uses a classpath that includes, among other things, a jar file for each
  library on which the binary depends. When running the wrapper shell script, any nonempty
  <code>JAVABIN</code> environment variable will take precedence over the version specified via
  Bazel's <code>--java_runtime_version</code> flag.
</p>
<p>
  The wrapper script accepts several unique flags. Refer to
  <code>java_stub_template.txt</code>
  for a list of configurable flags and environment variables accepted by the wrapper.
</p>

<h4 id="java_binary_implicit_outputs">Implicit output targets</h4>
<ul>
  <li><code><var>name</var>.jar</code>: A Java archive, containing the class files and other
    resources corresponding to the binary's direct dependencies.</li>
  <li><code><var>name</var>-src.jar</code>: An archive containing the sources ("source
    jar").</li>
  <li><code><var>name</var>_deploy.jar</code>: A Java archive suitable for deployment (only
    built if explicitly requested).
    <p>
      Building the <code>&lt;<var>name</var>&gt;_deploy.jar</code> target for your rule
      creates a self-contained jar file with a manifest that allows it to be run with the
      <code>java -jar</code> command or with the wrapper script's <code>--singlejar</code>
      option. Using the wrapper script is preferred to <code>java -jar</code> because it
      also passes the JVM flags and the options
      to load native libraries.
    </p>
    <p>
      The deploy jar contains all the classes that would be found by a classloader that
      searched the classpath from the binary's wrapper script from beginning to end. It also
      contains the native libraries needed for dependencies. These are automatically loaded
      into the JVM at runtime.
    </p>
    <p>If your target specifies a launcher
      attribute, then instead of being a normal JAR file, the _deploy.jar will be a
      native binary. This will contain the launcher plus any native (C++) dependencies of
      your rule, all linked into a static binary. The actual jar file's bytes will be
      appended to that native binary, creating a single binary blob containing both the
      executable and the Java code. You can execute the resulting jar file directly
      like you would execute any native binary.</p>
  </li>
  <li><code><var>name</var>_deploy-src.jar</code>: An archive containing the sources
    collected from the transitive closure of the target. These will match the classes in the
    <code>deploy.jar</code> except where jars have no matching source jar.</li>
</ul>

<p>
It is good practice to use the name of the source file that is the main entry point of the
application (minus the extension). For example, if your entry point is called
<code>Main.java</code>, then your name could be <code>Main</code>.
</p>

<p>
  A <code>deps</code> attribute is not allowed in a <code>java_binary</code> rule without
  <code>srcs</code>; such a rule requires a
  <code>main_class</code> provided by
  <code>runtime_deps</code>.
</p>

<p>The following code snippet illustrates a common mistake:</p>

<pre class="code">
<code class="lang-starlark">
java_binary(
    name = "DontDoThis",
    srcs = [
        <var>...</var>,
        <code class="deprecated">"GeneratedJavaFile.java"</code>,  # a generated .java file
    ],
    deps = [<code class="deprecated">":generating_rule",</code>],  # rule that generates that file
)
</code>
</pre>

<p>Do this instead:</p>

<pre class="code">
<code class="lang-starlark">
java_binary(
    name = "DoThisInstead",
    srcs = [
        <var>...</var>,
        ":generating_rule",
    ],
)
</code>
</pre>"""),BazelNativeRule("java_import","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("data","""[]""",false,"""The list of files needed by this rule at runtime."""),BazelNativeRuleArgument("deps","""[]""",false,"""The list of other libraries to be linked in to the target.
See java_library.deps."""),BazelNativeRuleArgument("exports","""[]""",false,"""Targets to make available to users of this rule.
See java_library.exports."""),BazelNativeRuleArgument("runtime_deps","""[]""",false,"""Libraries to make available to the final binary or test at runtime only.
See java_library.runtime_deps."""),BazelNativeRuleArgument("jars","""""""",true,"""The list of JAR files provided to Java targets that depend on this target."""),BazelNativeRuleArgument("srcjar","""None""",false,"""A JAR file that contains source code for the compiled JAR files."""),BazelNativeRuleArgument("neverlink","""False""",false,"""Only use this library for compilation and not at runtime.
Useful if the library will be provided by the runtime environment
during execution. Examples of libraries like this are IDE APIs
for IDE plug-ins or <code>tools.jar</code> for anything running on
a standard JDK."""),BazelNativeRuleArgument("constraints","""[]""",false,"""Extra constraints imposed on this rule as a Java library."""),BazelNativeRuleArgument("proguard_specs","""[]""",false,"""Files to be used as Proguard specification.
These will describe the set of specifications to be used by Proguard. If specified,
they will be added to any <code>android_binary</code> target depending on this library.

The files included here must only have idempotent rules, namely -dontnote, -dontwarn,
assumenosideeffects, and rules that start with -keep. Other options can only appear in
<code>android_binary</code>'s proguard_specs, to ensure non-tautological merges."""),BazelNativeRuleArgument("add_exports","""[]""",false,"""Allow this library to access the given <code>module</code> or <code>package</code>.
<p>
This corresponds to the javac and JVM --add-exports= flags."""),BazelNativeRuleArgument("add_opens","""[]""",false,"""Allow this library to reflectively access the given <code>module</code> or
<code>package</code>.
<p>
This corresponds to the javac and JVM --add-opens= flags."""),BazelNativeRuleArgument("licenses","""[]""",false,""""""),),"""<p>
  This rule allows the use of precompiled <code>.jar</code> files as
  libraries for <code>java_library</code> and
  <code>java_binary</code> rules.
</p>

<h4 id="java_import_examples">Examples</h4>

<pre class="code">
<code class="lang-starlark">
    java_import(
        name = "maven_model",
        jars = [
            "maven_model/maven-aether-provider-3.2.3.jar",
            "maven_model/maven-model-3.2.3.jar",
            "maven_model/maven-model-builder-3.2.3.jar",
        ],
    )
</code>
</pre>"""),BazelNativeRule("java_library","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("srcs","""[]""",false,"""The list of source files that are processed to create the target.
This attribute is almost always required; see exceptions below.
<p>
Source files of type <code>.java</code> are compiled. In case of generated
<code>.java</code> files it is generally advisable to put the generating rule's name
here instead of the name of the file itself. This not only improves readability but
makes the rule more resilient to future changes: if the generating rule generates
different files in the future, you only need to fix one place: the <code>outs</code> of
the generating rule. You should not list the generating rule in <code>deps</code>
because it is a no-op.
</p>
<p>
Source files of type <code>.srcjar</code> are unpacked and compiled. (This is useful if
you need to generate a set of <code>.java</code> files with a genrule.)
</p>
<p>
Rules: if the rule (typically <code>genrule</code> or <code>filegroup</code>) generates
any of the files listed above, they will be used the same way as described for source
files.
</p>
<p>
Source files of type <code>.properties</code> are treated as resources.
</p>

<p>All other files are ignored, as long as there is at least one file of a
file type described above. Otherwise an error is raised.</p>

<p>
This argument is almost always required, except if you specify the <code>runtime_deps</code> argument.
</p>"""),BazelNativeRuleArgument("data","""[]""",false,"""The list of files needed by this library at runtime.
See general comments about <code>data</code> at
Typical attributes defined by
most build rules.
<p>
  When building a <code>java_library</code>, Bazel doesn't put these files anywhere; if the
  <code>data</code> files are generated files then Bazel generates them. When building a
  test that depends on this <code>java_library</code> Bazel copies or links the
  <code>data</code> files into the runfiles area.
</p>"""),BazelNativeRuleArgument("resources","""[]""",false,"""A list of data files to include in a Java jar.
<p>
Resources may be source files or generated files.
</p>

<p>
If resources are specified, they will be bundled in the jar along with the usual
<code>.class</code> files produced by compilation. The location of the resources inside
of the jar file is determined by the project structure. Bazel first looks for Maven's
standard directory layout,
(a "src" directory followed by a "resources" directory grandchild). If that is not
found, Bazel then looks for the topmost directory named "java" or "javatests" (so, for
example, if a resource is at <code>&lt;workspace root&gt;/x/java/y/java/z</code>, the
path of the resource will be <code>y/java/z</code>. This heuristic cannot be overridden,
however, the <code>resource_strip_prefix</code> attribute can be used to specify a
specific alternative directory for resource files."""),BazelNativeRuleArgument("plugins","""[]""",false,"""Java compiler plugins to run at compile-time.
Every <code>java_plugin</code> specified in this attribute will be run whenever this rule
is built. A library may also inherit plugins from dependencies that use
<code>exported_plugins</code>. Resources
generated by the plugin will be included in the resulting jar of this rule."""),BazelNativeRuleArgument("deps","""[]""",false,"""The list of libraries to link into this library.
See general comments about <code>deps</code> at
Typical attributes defined by
most build rules.
<p>
  The jars built by <code>java_library</code> rules listed in <code>deps</code> will be on
  the compile-time classpath of this rule. Furthermore the transitive closure of their
  <code>deps</code>, <code>runtime_deps</code> and <code>exports</code> will be on the
  runtime classpath.
</p>
<p>
  By contrast, targets in the <code>data</code> attribute are included in the runfiles but
  on neither the compile-time nor runtime classpath.
</p>"""),BazelNativeRuleArgument("runtime_deps","""[]""",false,"""Libraries to make available to the final binary or test at runtime only.
Like ordinary <code>deps</code>, these will appear on the runtime classpath, but unlike
them, not on the compile-time classpath. Dependencies needed only at runtime should be
listed here. Dependency-analysis tools should ignore targets that appear in both
<code>runtime_deps</code> and <code>deps</code>."""),BazelNativeRuleArgument("exports","""[]""",false,"""Exported libraries.
<p>
  Listing rules here will make them available to parent rules, as if the parents explicitly
  depended on these rules. This is not true for regular (non-exported) <code>deps</code>.
</p>
<p>
  Summary: a rule <i>X</i> can access the code in <i>Y</i> if there exists a dependency
  path between them that begins with a <code>deps</code> edge followed by zero or more
  <code>exports</code> edges. Let's see some examples to illustrate this.
</p>
<p>
  Assume <i>A</i> depends on <i>B</i> and <i>B</i> depends on <i>C</i>. In this case
  C is a <em>transitive</em> dependency of A, so changing C's sources and rebuilding A will
  correctly rebuild everything. However A will not be able to use classes in C. To allow
  that, either A has to declare C in its <code>deps</code>, or B can make it easier for A
  (and anything that may depend on A) by declaring C in its (B's) <code>exports</code>
  attribute.
</p>
<p>
  The closure of exported libraries is available to all direct parent rules. Take a slightly
  different example: A depends on B, B depends on C and D, and also exports C but not D.
  Now A has access to C but not to D. Now, if C and D exported some libraries, C' and D'
  respectively, A could only access C' but not D'.
</p>
<p>
  Important: an exported rule is not a regular dependency. Sticking to the previous example,
  if B exports C and wants to also use C, it has to also list it in its own
  <code>deps</code>.
</p>"""),BazelNativeRuleArgument("exported_plugins","""[]""",false,"""The list of <code>java_plugin</code>s (e.g. annotation
processors) to export to libraries that directly depend on this library.
<p>
  The specified list of <code>java_plugin</code>s will be applied to any library which
  directly depends on this library, just as if that library had explicitly declared these
  labels in <code>plugins</code>.
</p>"""),BazelNativeRuleArgument("bootclasspath","""None""",false,"""Restricted API, do not use!"""),BazelNativeRuleArgument("javabuilder_jvm_flags","""[]""",false,"""Restricted API, do not use!"""),BazelNativeRuleArgument("javacopts","""[]""",false,"""Extra compiler options for this library.
Subject to "Make variable" substitution and
Bourne shell tokenization.
<p>These compiler options are passed to javac after the global compiler options.</p>"""),BazelNativeRuleArgument("neverlink","""False""",false,"""Whether this library should only be used for compilation and not at runtime.
Useful if the library will be provided by the runtime environment during execution. Examples
of such libraries are the IDE APIs for IDE plug-ins or <code>tools.jar</code> for anything
running on a standard JDK.
<p>
  Note that <code>neverlink = True</code> does not prevent the compiler from inlining material
  from this library into compilation targets that depend on it, as permitted by the Java
  Language Specification (e.g., <code>static final</code> constants of <code>String</code>
  or of primitive types). The preferred use case is therefore when the runtime library is
  identical to the compilation library.
</p>
<p>
  If the runtime library differs from the compilation library then you must ensure that it
  differs only in places that the JLS forbids compilers to inline (and that must hold for
  all future versions of the JLS).
</p>"""),BazelNativeRuleArgument("resource_strip_prefix","""""""",false,"""The path prefix to strip from Java resources.
<p>
If specified, this path prefix is stripped from every file in the <code>resources</code>
attribute. It is an error for a resource file not to be under this directory. If not
specified (the default), the path of resource file is determined according to the same
logic as the Java package of source files. For example, a source file at
<code>stuff/java/foo/bar/a.txt</code> will be located at <code>foo/bar/a.txt</code>.
</p>"""),BazelNativeRuleArgument("proguard_specs","""[]""",false,"""Files to be used as Proguard specification.
These will describe the set of specifications to be used by Proguard. If specified,
they will be added to any <code>android_binary</code> target depending on this library.

The files included here must only have idempotent rules, namely -dontnote, -dontwarn,
assumenosideeffects, and rules that start with -keep. Other options can only appear in
<code>android_binary</code>'s proguard_specs, to ensure non-tautological merges."""),BazelNativeRuleArgument("add_exports","""[]""",false,"""Allow this library to access the given <code>module</code> or <code>package</code>.
<p>
This corresponds to the javac and JVM --add-exports= flags."""),BazelNativeRuleArgument("add_opens","""[]""",false,"""Allow this library to reflectively access the given <code>module</code> or
<code>package</code>.
<p>
This corresponds to the javac and JVM --add-opens= flags."""),BazelNativeRuleArgument("licenses","""[]""",false,""""""),),"""<p>This rule compiles and links sources into a <code>.jar</code> file.</p>

<h4>Implicit outputs</h4>
<ul>
  <li><code>lib<var>name</var>.jar</code>: A Java archive containing the class files.</li>
  <li><code>lib<var>name</var>-src.jar</code>: An archive containing the sources ("source
    jar").</li>
</ul>"""),BazelNativeRule("java_plugin","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("srcs","""[]""",false,"""The list of source files that are processed to create the target.
This attribute is almost always required; see exceptions below.
<p>
Source files of type <code>.java</code> are compiled. In case of generated
<code>.java</code> files it is generally advisable to put the generating rule's name
here instead of the name of the file itself. This not only improves readability but
makes the rule more resilient to future changes: if the generating rule generates
different files in the future, you only need to fix one place: the <code>outs</code> of
the generating rule. You should not list the generating rule in <code>deps</code>
because it is a no-op.
</p>
<p>
Source files of type <code>.srcjar</code> are unpacked and compiled. (This is useful if
you need to generate a set of <code>.java</code> files with a genrule.)
</p>
<p>
Rules: if the rule (typically <code>genrule</code> or <code>filegroup</code>) generates
any of the files listed above, they will be used the same way as described for source
files.
</p>
<p>
Source files of type <code>.properties</code> are treated as resources.
</p>

<p>All other files are ignored, as long as there is at least one file of a
file type described above. Otherwise an error is raised.</p>

<p>
This argument is almost always required, except if you specify the <code>runtime_deps</code> argument.
</p>"""),BazelNativeRuleArgument("data","""[]""",false,"""The list of files needed by this library at runtime.
See general comments about <code>data</code> at
Typical attributes defined by
most build rules.
<p>
  When building a <code>java_library</code>, Bazel doesn't put these files anywhere; if the
  <code>data</code> files are generated files then Bazel generates them. When building a
  test that depends on this <code>java_library</code> Bazel copies or links the
  <code>data</code> files into the runfiles area.
</p>"""),BazelNativeRuleArgument("resources","""[]""",false,"""A list of data files to include in a Java jar.
<p>
Resources may be source files or generated files.
</p>

<p>
If resources are specified, they will be bundled in the jar along with the usual
<code>.class</code> files produced by compilation. The location of the resources inside
of the jar file is determined by the project structure. Bazel first looks for Maven's
standard directory layout,
(a "src" directory followed by a "resources" directory grandchild). If that is not
found, Bazel then looks for the topmost directory named "java" or "javatests" (so, for
example, if a resource is at <code>&lt;workspace root&gt;/x/java/y/java/z</code>, the
path of the resource will be <code>y/java/z</code>. This heuristic cannot be overridden,
however, the <code>resource_strip_prefix</code> attribute can be used to specify a
specific alternative directory for resource files."""),BazelNativeRuleArgument("plugins","""[]""",false,"""Java compiler plugins to run at compile-time.
Every <code>java_plugin</code> specified in this attribute will be run whenever this rule
is built. A library may also inherit plugins from dependencies that use
<code>exported_plugins</code>. Resources
generated by the plugin will be included in the resulting jar of this rule."""),BazelNativeRuleArgument("deps","""[]""",false,"""The list of libraries to link into this library.
See general comments about <code>deps</code> at
Typical attributes defined by
most build rules.
<p>
  The jars built by <code>java_library</code> rules listed in <code>deps</code> will be on
  the compile-time classpath of this rule. Furthermore the transitive closure of their
  <code>deps</code>, <code>runtime_deps</code> and <code>exports</code> will be on the
  runtime classpath.
</p>
<p>
  By contrast, targets in the <code>data</code> attribute are included in the runfiles but
  on neither the compile-time nor runtime classpath.
</p>"""),BazelNativeRuleArgument("bootclasspath","""None""",false,"""Restricted API, do not use!"""),BazelNativeRuleArgument("javabuilder_jvm_flags","""[]""",false,"""Restricted API, do not use!"""),BazelNativeRuleArgument("javacopts","""[]""",false,"""Extra compiler options for this library.
Subject to "Make variable" substitution and
Bourne shell tokenization.
<p>These compiler options are passed to javac after the global compiler options.</p>"""),BazelNativeRuleArgument("neverlink","""False""",false,"""Whether this library should only be used for compilation and not at runtime.
Useful if the library will be provided by the runtime environment during execution. Examples
of such libraries are the IDE APIs for IDE plug-ins or <code>tools.jar</code> for anything
running on a standard JDK.
<p>
  Note that <code>neverlink = True</code> does not prevent the compiler from inlining material
  from this library into compilation targets that depend on it, as permitted by the Java
  Language Specification (e.g., <code>static final</code> constants of <code>String</code>
  or of primitive types). The preferred use case is therefore when the runtime library is
  identical to the compilation library.
</p>
<p>
  If the runtime library differs from the compilation library then you must ensure that it
  differs only in places that the JLS forbids compilers to inline (and that must hold for
  all future versions of the JLS).
</p>"""),BazelNativeRuleArgument("resource_strip_prefix","""""""",false,"""The path prefix to strip from Java resources.
<p>
If specified, this path prefix is stripped from every file in the <code>resources</code>
attribute. It is an error for a resource file not to be under this directory. If not
specified (the default), the path of resource file is determined according to the same
logic as the Java package of source files. For example, a source file at
<code>stuff/java/foo/bar/a.txt</code> will be located at <code>foo/bar/a.txt</code>.
</p>"""),BazelNativeRuleArgument("proguard_specs","""[]""",false,"""Files to be used as Proguard specification.
These will describe the set of specifications to be used by Proguard. If specified,
they will be added to any <code>android_binary</code> target depending on this library.

The files included here must only have idempotent rules, namely -dontnote, -dontwarn,
assumenosideeffects, and rules that start with -keep. Other options can only appear in
<code>android_binary</code>'s proguard_specs, to ensure non-tautological merges."""),BazelNativeRuleArgument("add_exports","""[]""",false,"""Allow this library to access the given <code>module</code> or <code>package</code>.
<p>
This corresponds to the javac and JVM --add-exports= flags."""),BazelNativeRuleArgument("add_opens","""[]""",false,"""Allow this library to reflectively access the given <code>module</code> or
<code>package</code>.
<p>
This corresponds to the javac and JVM --add-opens= flags."""),BazelNativeRuleArgument("licenses","""[]""",false,""""""),BazelNativeRuleArgument("generates_api","""False""",false,"""This attribute marks annotation processors that generate API code.
<p>If a rule uses an API-generating annotation processor, other rules
depending on it can refer to the generated code only if their
compilation actions are scheduled after the generating rule. This
attribute instructs Bazel to introduce scheduling constraints when
--java_header_compilation is enabled.
<p><em class="harmful">WARNING: This attribute affects build
performance, use it only if necessary.</em></p>"""),BazelNativeRuleArgument("processor_class","""""""",false,"""The processor class is the fully qualified type of the class that the Java compiler should
use as entry point to the annotation processor. If not specified, this rule will not
contribute an annotation processor to the Java compiler's annotation processing, but its
runtime classpath will still be included on the compiler's annotation processor path. (This
is primarily intended for use by
Error Prone plugins, which are loaded
from the annotation processor path using

java.util.ServiceLoader.)"""),BazelNativeRuleArgument("output_licenses","""[]""",false,""""""),),"""<p>
  <code>java_plugin</code> defines plugins for the Java compiler run by Bazel. The
  only supported kind of plugins are annotation processors. A <code>java_library</code> or
  <code>java_binary</code> rule can run plugins by depending on them via the <code>plugins</code>
  attribute. A <code>java_library</code> can also automatically export plugins to libraries that
  directly depend on it using
  <code>exported_plugins</code>.
</p>

<h4 id="java_plugin_implicit_outputs">Implicit output targets</h4>
    <ul>
      <li><code><var>libname</var>.jar</code>: A Java archive.</li>
    </ul>

<p>
  Arguments are identical to <code>java_library</code>, except
  for the addition of the <code>processor_class</code> argument.
</p>"""),BazelNativeRule("java_test","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("test_class","""""""",false,"""The Java class to be loaded by the test runner.<br/>
<p>
  By default, if this argument is not defined then the legacy mode is used and the
  test arguments are used instead. Set the <code>--nolegacy_bazel_java_test</code> flag
  to not fallback on the first argument.
</p>
<p>
  This attribute specifies the name of a Java class to be run by
  this test. It is rare to need to set this. If this argument is omitted,
  it will be inferred using the target's <code>name</code> and its
  source-root-relative path. If the test is located outside a known
  source root, Bazel will report an error if <code>test_class</code>
  is unset.
</p>
<p>
  For JUnit3, the test class needs to either be a subclass of
  <code>junit.framework.TestCase</code> or it needs to have a public
  static <code>suite()</code> method that returns a
  <code>junit.framework.Test</code> (or a subclass of <code>Test</code>).
  For JUnit4, the class needs to be annotated with
  <code>org.junit.runner.RunWith</code>.
</p>
<p>
  This attribute allows several <code>java_test</code> rules to
  share the same <code>Test</code>
  (<code>TestCase</code>, <code>TestSuite</code>, ...).  Typically
  additional information is passed to it
  (e.g. via <code>jvm_flags=['-Dkey=value']</code>) so that its
  behavior differs in each case, such as running a different
  subset of the tests.  This attribute also enables the use of
  Java tests outside the <code>javatests</code> tree.
</p>"""),BazelNativeRuleArgument("env_inherit","""[]""",false,""""""),BazelNativeRuleArgument("srcs","""[]""",false,"""The list of source files that are processed to create the target.
This attribute is almost always required; see exceptions below.
<p>
Source files of type <code>.java</code> are compiled. In case of generated
<code>.java</code> files it is generally advisable to put the generating rule's name
here instead of the name of the file itself. This not only improves readability but
makes the rule more resilient to future changes: if the generating rule generates
different files in the future, you only need to fix one place: the <code>outs</code> of
the generating rule. You should not list the generating rule in <code>deps</code>
because it is a no-op.
</p>
<p>
Source files of type <code>.srcjar</code> are unpacked and compiled. (This is useful if
you need to generate a set of <code>.java</code> files with a genrule.)
</p>
<p>
Rules: if the rule (typically <code>genrule</code> or <code>filegroup</code>) generates
any of the files listed above, they will be used the same way as described for source
files.
</p>

<p>
This argument is almost always required, except if a
<code>main_class</code> attribute specifies a
class on the runtime classpath or you specify the <code>runtime_deps</code> argument.
</p>"""),BazelNativeRuleArgument("deps","""[]""",false,"""The list of other libraries to be linked in to the target.
See general comments about <code>deps</code> at
Typical attributes defined by
most build rules."""),BazelNativeRuleArgument("resources","""[]""",false,"""A list of data files to include in a Java jar.

<p>
Resources may be source files or generated files.
</p>

<p>
If resources are specified, they will be bundled in the jar along with the usual
<code>.class</code> files produced by compilation. The location of the resources inside
of the jar file is determined by the project structure. Bazel first looks for Maven's
standard directory layout,
(a "src" directory followed by a "resources" directory grandchild). If that is not
found, Bazel then looks for the topmost directory named "java" or "javatests" (so, for
example, if a resource is at <code>&lt;workspace root&gt;/x/java/y/java/z</code>, the
path of the resource will be <code>y/java/z</code>. This heuristic cannot be overridden,
however, the <code>resource_strip_prefix</code> attribute can be used to specify a
specific alternative directory for resource files."""),BazelNativeRuleArgument("runtime_deps","""[]""",false,"""Libraries to make available to the final binary or test at runtime only.
Like ordinary <code>deps</code>, these will appear on the runtime classpath, but unlike
them, not on the compile-time classpath. Dependencies needed only at runtime should be
listed here. Dependency-analysis tools should ignore targets that appear in both
<code>runtime_deps</code> and <code>deps</code>."""),BazelNativeRuleArgument("data","""[]""",false,"""The list of files needed by this library at runtime.
See general comments about <code>data</code>
at Typical attributes defined by
most build rules."""),BazelNativeRuleArgument("plugins","""[]""",false,"""Java compiler plugins to run at compile-time.
Every <code>java_plugin</code> specified in this attribute will be run whenever this rule
is built. A library may also inherit plugins from dependencies that use
<code>exported_plugins</code>. Resources
generated by the plugin will be included in the resulting jar of this rule."""),BazelNativeRuleArgument("launcher","""None""",false,"""Specify a binary that will be used to run your Java program instead of the
normal <code>bin/java</code> program included with the JDK.
The target must be a <code>cc_binary</code>. Any <code>cc_binary</code> that
implements the

Java Invocation API can be specified as a value for this attribute.

<p>By default, Bazel will use the normal JDK launcher (bin/java or java.exe).</p>

<p>The related <code>
--java_launcher</code> Bazel flag affects only those
<code>java_binary</code> and <code>java_test</code> targets that have
<i>not</i> specified a <code>launcher</code> attribute.</p>

<p>Note that your native (C++, SWIG, JNI) dependencies will be built differently
depending on whether you are using the JDK launcher or another launcher:</p>

<ul>
<li>If you are using the normal JDK launcher (the default), native dependencies are
built as a shared library named <code>{name}_nativedeps.so</code>, where
<code>{name}</code> is the <code>name</code> attribute of this java_binary rule.
Unused code is <em>not</em> removed by the linker in this configuration.</li>

<li>If you are using any other launcher, native (C++) dependencies are statically
linked into a binary named <code>{name}_nativedeps</code>, where <code>{name}</code>
is the <code>name</code> attribute of this java_binary rule. In this case,
the linker will remove any code it thinks is unused from the resulting binary,
which means any C++ code accessed only via JNI may not be linked in unless
that <code>cc_library</code> target specifies <code>alwayslink = True</code>.</li>
</ul>

<p>When using any launcher other than the default JDK launcher, the format
of the <code>*_deploy.jar</code> output changes. See the main
java_binary docs for details.</p>"""),BazelNativeRuleArgument("bootclasspath","""None""",false,"""Restricted API, do not use!"""),BazelNativeRuleArgument("neverlink","""False""",false,""""""),BazelNativeRuleArgument("javacopts","""[]""",false,"""Extra compiler options for this binary.
Subject to "Make variable" substitution and
Bourne shell tokenization.
<p>These compiler options are passed to javac after the global compiler options.</p>"""),BazelNativeRuleArgument("add_exports","""[]""",false,"""Allow this library to access the given <code>module</code> or <code>package</code>.
<p>
This corresponds to the javac and JVM --add-exports= flags."""),BazelNativeRuleArgument("add_opens","""[]""",false,"""Allow this library to reflectively access the given <code>module</code> or
<code>package</code>.
<p>
This corresponds to the javac and JVM --add-opens= flags."""),BazelNativeRuleArgument("main_class","""""""",false,"""Name of class with <code>main()</code> method to use as entry point.
If a rule uses this option, it does not need a <code>srcs=[...]</code> list.
Thus, with this attribute one can make an executable from a Java library that already
contains one or more <code>main()</code> methods.
<p>
The value of this attribute is a class name, not a source file. The class must be
available at runtime: it may be compiled by this rule (from <code>srcs</code>) or
provided by direct or transitive dependencies (through <code>runtime_deps</code> or
<code>deps</code>). If the class is unavailable, the binary will fail at runtime; there
is no build-time check.
</p>"""),BazelNativeRuleArgument("jvm_flags","""[]""",false,"""A list of flags to embed in the wrapper script generated for running this binary.
Subject to $(location) and
"Make variable" substitution, and
Bourne shell tokenization.

<p>The wrapper script for a Java binary includes a CLASSPATH definition
(to find all the dependent jars) and invokes the right Java interpreter.
The command line generated by the wrapper script includes the name of
the main class followed by a <code>"$@"</code> so you can pass along other
arguments after the classname.  However, arguments intended for parsing
by the JVM must be specified <i>before</i> the classname on the command
line.  The contents of <code>jvm_flags</code> are added to the wrapper
script before the classname is listed.</p>

<p>Note that this attribute has <em>no effect</em> on <code>*_deploy.jar</code>
outputs.</p>"""),BazelNativeRuleArgument("deploy_manifest_lines","""[]""",false,"""A list of lines to add to the <code>META-INF/manifest.mf</code> file generated for the
<code>*_deploy.jar</code> target. The contents of this attribute are <em>not</em> subject
to "Make variable" substitution."""),BazelNativeRuleArgument("stamp","""0""",false,"""Whether to encode build information into the binary. Possible values:
<ul>
<li>
  <code>stamp = 1</code>: Always stamp the build information into the binary, even in
  <code>--nostamp</code> builds. <b>This
  setting should be avoided</b>, since it potentially kills remote caching for the
  binary and any downstream actions that depend on it.
</li>
<li>
  <code>stamp = 0</code>: Always replace build information by constant values. This
  gives good build result caching.
</li>
<li>
  <code>stamp = -1</code>: Embedding of build information is controlled by the
  <code>--[no]stamp</code> flag.
</li>
</ul>
<p>Stamped binaries are <em>not</em> rebuilt unless their dependencies change.</p>"""),BazelNativeRuleArgument("use_testrunner","""True""",false,"""Use the test runner (by default
<code>com.google.testing.junit.runner.BazelTestRunner</code>) class as the
main entry point for a Java program, and provide the test class
to the test runner as a value of <code>bazel.test_suite</code>
system property.

<br/>
You can use this to override the default
behavior, which is to use test runner for
<code>java_test</code> rules,
and not use it for <code>java_binary</code> rules.  It is unlikely
you will want to do this.  One use is for <code>AllTest</code>
rules that are invoked by another rule (to set up a database
before running the tests, for example).  The <code>AllTest</code>
rule must be declared as a <code>java_binary</code>, but should
still use the test runner as its main entry point.

The name of a test runner class can be overridden with <code>main_class</code> attribute."""),BazelNativeRuleArgument("use_launcher","""True""",false,"""Whether the binary should use a custom launcher.

<p>If this attribute is set to false, the
launcher attribute  and the related
<code>--java_launcher</code> flag
will be ignored for this target."""),BazelNativeRuleArgument("env","""{}""",false,""""""),BazelNativeRuleArgument("classpath_resources","""[]""",false,"""<em class="harmful">DO NOT USE THIS OPTION UNLESS THERE IS NO OTHER WAY)</em>
<p>
A list of resources that must be located at the root of the java tree. This attribute's
only purpose is to support third-party libraries that require that their resources be
found on the classpath as exactly <code>"myconfig.xml"</code>. It is only allowed on
binaries and not libraries, due to the danger of namespace conflicts.
</p>"""),BazelNativeRuleArgument("licenses","""[]""",false,""""""),BazelNativeRuleArgument("create_executable","""True""",false,"""Deprecated, use <code>java_single_jar</code> instead."""),BazelNativeRuleArgument("resource_strip_prefix","""""""",false,"""The path prefix to strip from Java resources.
<p>
If specified, this path prefix is stripped from every file in the <code>resources</code>
attribute. It is an error for a resource file not to be under this directory. If not
specified (the default), the path of resource file is determined according to the same
logic as the Java package of source files. For example, a source file at
<code>stuff/java/foo/bar/a.txt</code> will be located at <code>foo/bar/a.txt</code>.
</p>"""),),"""<p>
A <code>java_test()</code> rule compiles a Java test. A test is a binary wrapper around your
test code. The test runner's main method is invoked instead of the main class being compiled.
</p>

<h4 id="java_test_implicit_outputs">Implicit output targets</h4>
<ul>
  <li><code><var>name</var>.jar</code>: A Java archive.</li>
  <li><code><var>name</var>_deploy.jar</code>: A Java archive suitable
    for deployment. (Only built if explicitly requested.) See the description of the
    <code><var>name</var>_deploy.jar</code> output from
    java_binary for more details.</li>
</ul>

<p>
See the section on <code>java_binary()</code> arguments. This rule also
supports all attributes common
to all test rules (*_test).
</p>

<h4 id="java_test_examples">Examples</h4>

<pre class="code">
<code class="lang-starlark">

java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/foo/base:testResources",
        "//java/com/foo/testing/util",
    ],
)

java_test(
    name = "AllTests",
    size = "small",
    runtime_deps = [
        ":tests",
        "//util/mysql",
    ],
)
</code>
</pre>"""),BazelNativeRule("java_package_configuration","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("packages","""[]""",false,"""The set of <code>package_group</code>s
the configuration should be applied to."""),BazelNativeRuleArgument("javacopts","""[]""",false,"""Java compiler flags."""),BazelNativeRuleArgument("data","""[]""",false,"""The list of files needed by this configuration at runtime."""),BazelNativeRuleArgument("system","""None""",false,"""Corresponds to javac's --system flag."""),BazelNativeRuleArgument("output_licenses","""[]""",false,""""""),),"""<p>
Configuration to apply to a set of packages.
Configurations can be added to
<code>java_toolchain.javacopts</code>s.
</p>

<h4 id="java_package_configuration_example">Example:</h4>

<pre class="code">
<code class="lang-starlark">

java_package_configuration(
    name = "my_configuration",
    packages = [":my_packages"],
    javacopts = ["-Werror"],
)

package_group(
    name = "my_packages",
    packages = [
        "//com/my/project/...",
        "-//com/my/project/testing/...",
    ],
)

java_toolchain(
    ...,
    package_configuration = [
        ":my_configuration",
    ]
)

</code>
</pre>"""),BazelNativeRule("java_runtime","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("default_cds","""None""",false,"""Default CDS archive for hermetic <code>java_runtime</code>. When hermetic
is enabled for a <code>java_binary</code> target the <code>java_runtime</code>
default CDS is packaged in the hermetic deploy JAR."""),BazelNativeRuleArgument("hermetic_srcs","""[]""",false,"""Files in the runtime needed for hermetic deployments."""),BazelNativeRuleArgument("hermetic_static_libs","""[]""",false,"""The libraries that are statically linked with the launcher for hermetic deployments"""),BazelNativeRuleArgument("java","""None""",false,"""The path to the java executable."""),BazelNativeRuleArgument("java_home","""""""",false,"""The path to the root of the runtime.
Subject to "Make" variable substitution.
If this path is absolute, the rule denotes a non-hermetic Java runtime with a well-known
path. In that case, the <code>srcs</code> and <code>java</code> attributes must be empty."""),BazelNativeRuleArgument("lib_ct_sym","""None""",false,"""The lib/ct.sym file needed for compilation with <code>--release</code>. If not specified and
there is exactly one file in <code>srcs</code> whose path ends with
<code>/lib/ct.sym</code>, that file is used."""),BazelNativeRuleArgument("lib_modules","""None""",false,"""The lib/modules file needed for hermetic deployments."""),BazelNativeRuleArgument("srcs","""[]""",false,"""All files in the runtime."""),BazelNativeRuleArgument("version","""0""",false,"""The feature version of the Java runtime. I.e., the integer returned by
<code>Runtime.version().feature()</code>."""),BazelNativeRuleArgument("output_licenses","""[]""",false,""""""),),"""<p>
Specifies the configuration for a Java runtime.
</p>

<h4 id="java_runtime_example">Example:</h4>

<pre class="code">
<code class="lang-starlark">

java_runtime(
    name = "jdk-9-ea+153",
    srcs = glob(["jdk9-ea+153/**"]),
    java_home = "jdk9-ea+153",
)

</code>
</pre>"""),BazelNativeRule("java_toolchain","",setOf(BazelNativeRuleArgument("name","""""""",true,"""A unique name for this target."""),BazelNativeRuleArgument("android_lint_data","""[]""",false,"""Labels of tools available for label-expansion in android_lint_jvm_opts."""),BazelNativeRuleArgument("android_lint_opts","""[]""",false,"""The list of Android Lint arguments."""),BazelNativeRuleArgument("android_lint_jvm_opts","""[]""",false,"""The list of arguments for the JVM when invoking Android Lint."""),BazelNativeRuleArgument("android_lint_package_configuration","""[]""",false,"""Android Lint Configuration that should be applied to the specified package groups."""),BazelNativeRuleArgument("android_lint_runner","""None""",false,"""Label of the Android Lint runner, if any."""),BazelNativeRuleArgument("bootclasspath","""[]""",false,"""The Java target bootclasspath entries. Corresponds to javac's -bootclasspath flag."""),BazelNativeRuleArgument("compatible_javacopts","""{}""",false,"""Internal API, do not use!"""),BazelNativeRuleArgument("deps_checker","""None""",false,"""Label of the ImportDepsChecker deploy jar."""),BazelNativeRuleArgument("forcibly_disable_header_compilation","""False""",false,"""Overrides --java_header_compilation to disable header compilation on platforms that do not
support it, e.g. JDK 7 Bazel."""),BazelNativeRuleArgument("genclass","""None""",false,"""Label of the GenClass deploy jar."""),BazelNativeRuleArgument("header_compiler","""None""",false,"""Label of the header compiler. Required if --java_header_compilation is enabled."""),BazelNativeRuleArgument("header_compiler_direct","""None""",false,"""Optional label of the header compiler to use for direct classpath actions that do not
include any API-generating annotation processors.

<p>This tool does not support annotation processing."""),BazelNativeRuleArgument("header_compiler_builtin_processors","""[]""",false,"""Internal API, do not use!"""),BazelNativeRuleArgument("ijar","""None""",false,"""Label of the ijar executable."""),BazelNativeRuleArgument("jacocorunner","""None""",false,"""Label of the JacocoCoverageRunner deploy jar."""),BazelNativeRuleArgument("javabuilder","""None""",false,"""Label of the JavaBuilder deploy jar."""),BazelNativeRuleArgument("javabuilder_data","""[]""",false,"""Labels of data available for label-expansion in javabuilder_jvm_opts."""),BazelNativeRuleArgument("javabuilder_jvm_opts","""[]""",false,"""The list of arguments for the JVM when invoking JavaBuilder."""),BazelNativeRuleArgument("java_runtime","""None""",false,"""The java_runtime to use with this toolchain. It defaults to java_runtime
in execution configuration."""),BazelNativeRuleArgument("javac_supports_workers","""True""",false,"""True if JavaBuilder supports running as a persistent worker, false if it doesn't."""),BazelNativeRuleArgument("javac_supports_multiplex_workers","""True""",false,"""True if JavaBuilder supports running as a multiplex persistent worker, false if it doesn't."""),BazelNativeRuleArgument("javac_supports_worker_cancellation","""True""",false,"""True if JavaBuilder supports cancellation of persistent workers, false if it doesn't."""),BazelNativeRuleArgument("javac_supports_worker_multiplex_sandboxing","""False""",false,"""True if JavaBuilder supports running as a multiplex persistent worker with sandboxing, false if it doesn't."""),BazelNativeRuleArgument("javacopts","""[]""",false,"""The list of extra arguments for the Java compiler. Please refer to the Java compiler
documentation for the extensive list of possible Java compiler flags."""),BazelNativeRuleArgument("jspecify_implicit_deps","""None""",false,"""Experimental, do not use!"""),BazelNativeRuleArgument("jspecify_javacopts","""[]""",false,"""Experimental, do not use!"""),BazelNativeRuleArgument("jspecify_packages","""[]""",false,"""Experimental, do not use!"""),BazelNativeRuleArgument("jspecify_processor","""None""",false,"""Experimental, do not use!"""),BazelNativeRuleArgument("jspecify_processor_class","""""""",false,"""Experimental, do not use!"""),BazelNativeRuleArgument("jspecify_stubs","""[]""",false,"""Experimental, do not use!"""),BazelNativeRuleArgument("jvm_opts","""[]""",false,"""The list of arguments for the JVM when invoking the Java compiler. Please refer to the Java
virtual machine documentation for the extensive list of possible flags for this option."""),BazelNativeRuleArgument("misc","""[]""",false,"""Deprecated: use javacopts instead"""),BazelNativeRuleArgument("oneversion","""None""",false,"""Label of the one-version enforcement binary."""),BazelNativeRuleArgument("oneversion_whitelist","""None""",false,"""Deprecated: use oneversion_allowlist instead"""),BazelNativeRuleArgument("oneversion_allowlist","""None""",false,"""Label of the one-version allowlist."""),BazelNativeRuleArgument("oneversion_allowlist_for_tests","""None""",false,"""Label of the one-version allowlist for tests."""),BazelNativeRuleArgument("package_configuration","""[]""",false,"""Configuration that should be applied to the specified package groups."""),BazelNativeRuleArgument("proguard_allowlister",""""@bazel_tools//tools/jdk:proguard_whitelister"""",false,"""Label of the Proguard allowlister."""),BazelNativeRuleArgument("reduced_classpath_incompatible_processors","""[]""",false,"""Internal API, do not use!"""),BazelNativeRuleArgument("singlejar","""None""",false,"""Label of the SingleJar deploy jar."""),BazelNativeRuleArgument("source_version","""""""",false,"""The Java source version (e.g., '6' or '7'). It specifies which set of code structures
are allowed in the Java source code."""),BazelNativeRuleArgument("target_version","""""""",false,"""The Java target version (e.g., '6' or '7'). It specifies for which Java runtime the class
should be build."""),BazelNativeRuleArgument("timezone_data","""None""",false,"""Label of a resource jar containing timezone data. If set, the timezone data is added as an
implicitly runtime dependency of all java_binary rules."""),BazelNativeRuleArgument("tools","""[]""",false,"""Labels of tools available for label-expansion in jvm_opts."""),BazelNativeRuleArgument("turbine_data","""[]""",false,"""Labels of data available for label-expansion in turbine_jvm_opts."""),BazelNativeRuleArgument("turbine_jvm_opts","""[]""",false,"""The list of arguments for the JVM when invoking turbine."""),BazelNativeRuleArgument("xlint","""[]""",false,"""The list of warning to add or removes from default list. Precedes it with a dash to
removes it. Please see the Javac documentation on the -Xlint options for more information."""),BazelNativeRuleArgument("licenses","""[]""",false,""""""),),"""<p>
Specifies the configuration for the Java compiler. Which toolchain to be used can be changed through
the --java_toolchain argument. Normally you should not write those kind of rules unless you want to
tune your Java compiler.
</p>

<h4>Examples</h4>

<p>A simple example would be:
</p>

<pre class="code">
<code class="lang-starlark">

java_toolchain(
    name = "toolchain",
    source_version = "7",
    target_version = "7",
    bootclasspath = ["//tools/jdk:bootclasspath"],
    xlint = [ "classfile", "divzero", "empty", "options", "path" ],
    javacopts = [ "-g" ],
    javabuilder = ":JavaBuilder_deploy.jar",
)
</code>
</pre>"""),),)
