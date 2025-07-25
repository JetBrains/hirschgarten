import com.google.devtools.build.lib.starlarkdocextract.StardocOutputProtos
import common.serializeFunctionsTo
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.Environment
import java.io.FileInputStream
import kotlin.system.exitProcess

object ProtobufReader {
  private val commonParams =
    listOf(
      BazelGlobalFunctionParameter(
        name = "aspect_hints",
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>; default is <code translate="no" dir="ltr">[]</code></p>

          <p>A list of arbitrary labels which is exposed to <a href="/extending/aspects">aspects</a> (in
          particular - aspects invoked by this rule's reverse dependencies), but isn't exposed to this rule's
          own implementation. Consult documentation for language-specific rule sets for details about what
          effect a particular aspect hint would have.</p>

          <p>You could think of an aspect hint as a richer alternative to a <a href="#common.tags">tag</a>:
          while a tag conveys only a boolean state (the tag is either present or absent in the
          <code translate="no" dir="ltr">tags</code> list), an aspect hint can convey arbitrary structured information in its
          <a href="/extending/rules#providers">providers</a>.</p>

          <p>In practice, aspect hints are used for interoperability between different language-specific
          rule sets. For example, imagine you have a <code translate="no" dir="ltr">mylang_binary</code> target which needs to depend
          on an <code translate="no" dir="ltr">otherlang_library</code> target. The MyLang-specific logic needs some additional
          information about the OtherLang target in order to use it, but <code translate="no" dir="ltr">otherlang_library</code>
          doesn't provide this information because it knows nothing about MyLang. One solution might be for
          the MyLang rule set to define a <code translate="no" dir="ltr">mylang_hint</code> rule which can be used to encode that
          additional information; the user can add the hint to their <code translate="no" dir="ltr">otherlang_library</code>'s
          <code translate="no" dir="ltr">aspect_hints</code>, and <code translate="no" dir="ltr">mylang_binary</code> can use an aspect to collect the
          additional information from a MyLang-specific provider in the <code translate="no" dir="ltr">mylang_hint</code>.</p>

          <p>For a concrete example, see
          <a href="https://github.com/bazelbuild/rules_swift/blob/master/doc/rules.md#swift_interop_hint"><code translate="no" dir="ltr">swift_interop_hint</code></a>
          and <a href="https://github.com/bazelbuild/rules_swift/blob/master/doc/rules.md#swift_overlay"><code translate="no" dir="ltr">swift_overlay</code></a>
          in <code translate="no" dir="ltr">rules_swift</code>.</p>

          <p>Best practices:</p>
          <ul>
            <li>Targets listed in <code translate="no" dir="ltr">aspect_hints</code> should be lightweight and minimal.</li>
            <li>Language-specific logic should consider only aspect hints having providers relevant to that
              language, and should ignore any other aspect hints.</li>
          </ul>
          """.trimIndent(),
        required = false,
        defaultValue = "[]",
        positional = false,
        named = true,
      ),
      BazelGlobalFunctionParameter(
        name = "compatible_with",
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>;
            <a href="#configurable-attributes">nonconfigurable</a>; default is <code translate="no" dir="ltr">[]</code></p>

          <p>
          The list of environments this target can be built for, in addition to
          default-supported environments.
          </p>

          <p>
          This is part of Bazel's constraint system, which lets users declare which
          targets can and cannot depend on each other. For example, externally deployable
          binaries shouldn't depend on libraries with company-secret code. See
          <a href="https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/analysis/constraints/ConstraintSemantics.java#L46">
          ConstraintSemantics</a> for details.
          </p>
          """.trimIndent(),
        required = false,
        positional = false,
        named = true,
        defaultValue = "[]",
      ),
      BazelGlobalFunctionParameter(
        name = "deprecation",
        doc =
          """
          <p>String; <a href="#configurable-attributes">nonconfigurable</a>; default is <code translate="no" dir="ltr">None</code></p>

          <p>
          An explanatory warning message associated with this target.
          Typically this is used to notify users that a target has become obsolete,
          or has become superseded by another rule, is private to a package, or is
          perhaps considered harmful for some reason. It is a good idea to include
          some reference (like a webpage, a bug number or example migration CLs) so
          that one can easily find out what changes are required to avoid the message.
          If there is a new target that can be used as a drop in replacement, it is a
          good idea to just migrate all users of the old target.
          </p>

          <p>
          This attribute has no effect on the way things are built, but it
          may affect a build tool's diagnostic output.  The build tool issues a
          warning when a rule with a <code translate="no" dir="ltr">deprecation</code> attribute is
          depended upon by a target in another package.
          </p>

          <p>
          Intra-package dependencies are exempt from this warning, so that,
          for example, building the tests of a deprecated rule does not
          encounter a warning.
          </p>

          <p>
          If a deprecated target depends on another deprecated target, no warning
          message is issued.
          </p>

          <p>
          Once people have stopped using it, the target can be removed.
          </p>
          """.trimIndent(),
        required = false,
        positional = false,
        named = true,
        defaultValue = "None",
      ),
      BazelGlobalFunctionParameter(
        name = "exec_compatible_with",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>;
            <a href="#configurable-attributes">nonconfigurable</a>; default is <code translate="no" dir="ltr">[]</code>
          </p>

          <p>
          A list of
          <code translate="no" dir="ltr"><a href="/reference/be/platforms-and-toolchains#constraint_value">constraint_values</a></code>
          that must be present in the execution platform of this target's default exec
          group. This is in addition to any constraints already set by the rule type.
          Constraints are used to restrict the list of available execution platforms.

          For more details, see
          the description of
            <a href="/docs/toolchains#toolchain-resolution">toolchain resolution</a>.
            and
            <a href="/extending/exec-groups">exec groups</a>

          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "exec_group_compatible_with",
        required = false,
        named = true,
        positional = false,
        defaultValue = "{}",
        doc =
          """
          <p>
          A dictionary of exec group names to lists of
          <code translate="no" dir="ltr"><a href="/reference/be/platforms-and-toolchains#constraint_value">constraint_values</a></code>
          that must be present in the execution platform for the given exec group. This
          is in addition to any constraints already set on the exec group's definition.
          Constraints are used to restrict the list of available execution platforms.

          For more details, see
          the description of
            <a href="/docs/toolchains#toolchain-resolution">toolchain resolution</a>.
            and
            <a href="/extending/exec-groups">exec groups</a>

          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "exec_properties",
        required = false,
        named = true,
        positional = false,
        defaultValue = "{}",
        doc =
          """
          <p>Dictionary of strings; default is <code translate="no" dir="ltr">{}</code></p>

          <p> A dictionary of strings that will be added to the <code translate="no" dir="ltr">exec_properties</code> of a platform selected for this target. See <code translate="no" dir="ltr">exec_properties</code> of the <a href="/reference/be/platforms-and-toolchains#platform">platform</a> rule.</p>

          <p>If a key is present in both the platform and target-level properties, the value will be taken from the target.</p>

          <p>Keys can be prefixed with the name of an execution group followed by a <code translate="no" dir="ltr">.</code> to apply them only to that particular exec group.</p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "features",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>List of <i>feature</i> strings; default is <code translate="no" dir="ltr">[]</code></p>

          <p>A feature is string tag that can be enabled or disabled on a target. The
            meaning of a feature depends on the rule itself.</p>

          <p>This <code translate="no" dir="ltr">features</code> attribute is combined with the <a href="/reference/be/functions#package">
          package</a> level <code translate="no" dir="ltr">features</code> attribute. For example, if
          the features ["a", "b"] are enabled on the package level, and a target's
          <code translate="no" dir="ltr">features</code> attribute contains ["-a", "c"], the features enabled for the
          rule will be "b" and "c".
            <a href="https://github.com/bazelbuild/examples/blob/main/rules/features/BUILD">
              See example</a>.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "package_metadata",
        required = false,
        named = true,
        positional = false,
        defaultValue = null,
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>;
            <a href="#configurable-attributes">nonconfigurable</a>; default is the package's
            <code translate="no" dir="ltr"><a href="/reference/be/functions#package.default_package_metadata">default_package_metadata</a></code>
          </p>

          <p>
          A list of labels that are associated metadata about this target.
          Typically, the labels are simple rules that return a provider of
          constant values. Rules and aspects may use these labels to perform some
          additional analysis on the build graph.
          </p>

          <p>
          The canonical use case is that of
          <a href="https://github.com/bazelbuild/rules_license">rules_license</a>.
          For that use case, <code translate="no" dir="ltr">package_metadata</code> and
          <code translate="no" dir="ltr">default_package_metadata</code> is used to attach information
          about a package's licence or version to targets. An aspect applied
          to a top-level binary can be used to gather those and produce
          compliance reports.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "restricted_to",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>;
            <a href="#configurable-attributes">nonconfigurable</a>; default is <code translate="no" dir="ltr">[]</code></p>

          <p>
          The list of environments this target can be built for, <i>instead</i> of
          default-supported environments.
          </p>

          <p>
          This is part of Bazel's constraint system. See
          <code translate="no" dir="ltr"><a href="#common.compatible_with">compatible_with</a></code>
          for details.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "tags",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>
            List of strings; <a href="#configurable-attributes">nonconfigurable</a>;
            default is <code translate="no" dir="ltr">[]</code>
          </p>

          <p>
            <i>Tags</i> can be used on any rule. <i>Tags</i> on test and
            <code translate="no" dir="ltr">test_suite</code> rules are useful for categorizing the tests.
            <i>Tags</i> on non-test targets are used to control sandboxed execution of
            <code translate="no" dir="ltr">genrule</code>s and

          <a href="/rules/concepts">Starlark</a>
            actions, and for parsing by humans and/or external tools.
          </p>

          <p>
            Bazel modifies the behavior of its sandboxing code if it finds the following
            keywords in the <code translate="no" dir="ltr">tags</code> attribute of any test or <code translate="no" dir="ltr">genrule</code>
            target, or the keys of <code translate="no" dir="ltr">execution_requirements</code> for any Starlark
            action.
          </p>

          <ul>
              <li><code translate="no" dir="ltr">no-sandbox</code> keyword results in the action or test never being
              sandboxed; it can still be cached or run remotely - use <code translate="no" dir="ltr">no-cache</code>
              or <code translate="no" dir="ltr">no-remote</code> to prevent either or both of those.
            </li>
            <li><code translate="no" dir="ltr">no-cache</code> keyword results in the action or test never being
              cached (locally or remotely). Note: for the purposes of this tag, the disk cache
              is considered a local cache, whereas the HTTP and gRPC caches are considered
              remote. Other caches, such as Skyframe or the persistent action cache, are not
              affected.
            </li>
              <li><code translate="no" dir="ltr">no-remote-cache</code> keyword results in the action or test never being
              cached remotely (but it may be cached locally; it may also be executed remotely).
              Note: for the purposes of this tag, the disk cache is considered a local cache,
              whereas the HTTP and gRPC caches are considered remote. Other caches, such as
              Skyframe or the persistent action cache, are not affected.
              If a combination of local disk cache and remote cache are used (combined cache),
              it's treated as a remote cache and disabled entirely unless <code translate="no" dir="ltr">--incompatible_remote_results_ignore_disk</code>
              is set in which case the local components will be used.
            </li>
              <li><code translate="no" dir="ltr">no-remote-exec</code> keyword results in the action or test never being
              executed remotely (but it may be cached remotely).
            </li>

            <li><code translate="no" dir="ltr">no-remote</code> keyword prevents the action or test from being executed remotely or
              cached remotely. This is equivalent to using both
              <code translate="no" dir="ltr">no-remote-cache</code> and <code translate="no" dir="ltr">no-remote-exec</code>.
                </li>
             <li><code translate="no" dir="ltr">no-remote-cache-upload</code> keyword disables upload part of remote caching of a spawn.
               it does not disable remote execution.
            </li>
              <li><code translate="no" dir="ltr">local</code> keyword precludes the action or test from being remotely cached,
              remotely executed, or run inside the sandbox.
              For genrules and tests, marking the rule with the <code translate="no" dir="ltr">local = True</code>
              attribute has the same effect.
            </li>

              <li><code translate="no" dir="ltr">requires-network</code> keyword allows access to the external
              network from inside the sandbox.  This tag only has an effect if sandboxing
              is enabled.
            </li>

            <li><code translate="no" dir="ltr">block-network</code> keyword blocks access to the external
              network from inside the sandbox. In this case, only communication
              with localhost is allowed. This tag only has an effect if sandboxing is
              enabled.
            </li>

            <li><code translate="no" dir="ltr">requires-fakeroot</code> runs the test or action as uid and gid 0 (i.e., the root
              user). This is only supported on Linux. This tag takes precedence over the
              <code class="flag" translate="no" dir="ltr">--sandbox_fake_username</code> command-line option.
            </li>
          </ul>

          <p>
            <i>Tags</i> on tests are generally used to annotate a test's role in your
            debug and release process.  Typically, tags are most useful for C++ and Python
            tests, which lack any runtime annotation ability.  The use of tags and size
            elements gives flexibility in assembling suites of tests based around codebase
            check-in policy.
          </p>

          <p>
            Bazel modifies test running behavior if it finds the following keywords in the
            <code translate="no" dir="ltr">tags</code> attribute of the test rule:
          </p>

          <ul>
            <li><code translate="no" dir="ltr">exclusive</code> will force the test to be run in the
              "exclusive" mode, ensuring that no other tests are running at the
              same time. Such tests will be executed in serial fashion after all build
              activity and non-exclusive tests have been completed. Remote execution is
              disabled for such tests because Bazel doesn't have control over what's
              running on a remote machine.
            </li>

            <li><code translate="no" dir="ltr">exclusive-if-local</code> will force the test to be run in the
              "exclusive" mode if it is executed locally, but will run the test in parallel if it's
              executed remotely.
            </li>

            <li><code translate="no" dir="ltr">manual</code> keyword will exclude the target from expansion of target pattern wildcards
              (<code translate="no" dir="ltr">...</code>, <code translate="no" dir="ltr">:*</code>, <code translate="no" dir="ltr">:all</code>, etc.) and <code translate="no" dir="ltr">test_suite</code> rules
              which do not list the test explicitly when computing the set of top-level targets to build/run
              for the <code translate="no" dir="ltr">build</code>, <code translate="no" dir="ltr">test</code>, and <code translate="no" dir="ltr">coverage</code> commands. It does not
              affect target wildcard or test suite expansion in other contexts, including the
              <code translate="no" dir="ltr">query</code> command. Note that <code translate="no" dir="ltr">manual</code> does not imply that a target should
              not be built/run automatically by continuous build/test systems. For example, it may be
              desirable to exclude a target from <code translate="no" dir="ltr">bazel test ...</code> because it requires specific
              Bazel flags, but still have it included in properly-configured presubmit or continuous test
              runs.

                </li>

            <li><code translate="no" dir="ltr">external</code> keyword will force test to be unconditionally
              executed (regardless of <code class="flag" translate="no" dir="ltr">--cache_test_results</code>
              value).
            </li>

            </ul>

          See
          <a href="/reference/test-encyclopedia#tag-conventions">Tag Conventions</a>
           in the Test Encyclopedia for more conventions on tags attached to test targets.
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "target_compatible_with",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <td><p>
          List of <a href="/concepts/labels">labels</a>; default is <code translate="no" dir="ltr">[]</code>
          </p>

          <p>
          A list of
          <code translate="no" dir="ltr"><a href="/reference/be/platforms-and-toolchains#constraint_value">constraint_value</a></code>s
          that must be present in the target platform for this target to be considered
          <em>compatible</em>. This is in addition to any constraints already set by the
          rule type. If the target platform does not satisfy all listed constraints then
          the target is considered <em>incompatible</em>. Incompatible targets are
          skipped for building and testing when the target pattern is expanded
          (e.g. <code translate="no" dir="ltr">//...</code>, <code translate="no" dir="ltr">:all</code>). When explicitly specified on the
          command line, incompatible targets cause Bazel to print an error and cause a
          build or test failure.
          </p>

          <p>
          Targets that transitively depend on incompatible targets are themselves
          considered incompatible. They are also skipped for building and testing.
          </p>

          <p>
          An empty list (which is the default) signifies that the target is compatible
          with all platforms.
          </p><p>

          </p><p>
          All rules other than <a href="/reference/be/workspace">Workspace Rules</a> support this
          attribute.
          For some rules this attribute has no effect. For example, specifying
          <code translate="no" dir="ltr">target_compatible_with</code> for a
          <code translate="no" dir="ltr"><a href="/reference/be/c-cpp#cc_toolchain">cc_toolchain</a></code> is not useful.
          </p><p>

          </p><p>
          See the
          <a href="/docs/platforms#skipping-incompatible-targets">Platforms</a>
          page for more information about incompatible target skipping.
          </p>
          </td>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "testonly",
        required = false,
        named = true,
        positional = false,
        defaultValue = "False",
        doc =
          """
          <p>Boolean; <a href="#configurable-attributes">nonconfigurable</a>; default is <code translate="no" dir="ltr">False</code>
            except for test and test suite targets</p>

          <p>
          If <code translate="no" dir="ltr">True</code>, only testonly targets (such as tests) can depend on this target.
          </p>

          <p>
          Equivalently, a rule that is not <code translate="no" dir="ltr">testonly</code> is not allowed to
          depend on any rule that is <code translate="no" dir="ltr">testonly</code>.
          </p>

          <p>
          Tests (<code translate="no" dir="ltr">*_test</code> rules)
          and test suites (<a href="/reference/be/general#test_suite">test_suite</a> rules)
          are <code translate="no" dir="ltr">testonly</code> by default.
          </p>

          <p>
          This attribute is intended to mean that the target should not be
          contained in binaries that are released to production.
          </p>

          <p>
          Because testonly is enforced at build time, not run time, and propagates
          virally through the dependency tree, it should be applied judiciously. For
          example, stubs and fakes that
          are useful for unit tests may also be useful for integration tests
          involving the same binaries that will be released to production, and
          therefore should probably not be marked testonly. Conversely, rules that
          are dangerous to even link in, perhaps because they unconditionally
          override normal behavior, should definitely be marked testonly.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "toolchains",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>;
            <a href="#configurable-attributes">nonconfigurable</a>; default is <code translate="no" dir="ltr">[]</code></p>

          <p>
            The set of targets whose <a href="/reference/be/make-variables">Make variables</a> this target is
            allowed to access. These targets are either instances of rules that provide
            <code translate="no" dir="ltr">TemplateVariableInfo</code> or special targets for toolchain types built into Bazel. These
            include:

          </p><ul>
              <li><code translate="no" dir="ltr">@bazel_tools//tools/cpp:toolchain_type</code>
            </li><li><code translate="no" dir="ltr">@rules_java//toolchains:current_java_runtime</code>
            </li></ul>

          <p>
            Note that this is distinct from the concept of
              <a href="/docs/toolchains#toolchain-resolution">toolchain resolution</a>
              that is used by rule implementations for platform-dependent configuration. You cannot use this
            attribute to determine which specific <code translate="no" dir="ltr">cc_toolchain</code> or <code translate="no" dir="ltr">java_toolchain</code> a
            target will use.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "visibility",
        required = false,
        named = true,
        positional = false,
        defaultValue = null,
        doc =
          """
          <p>List of <a href="/concepts/labels">labels</a>;
            <a href="#configurable-attributes">nonconfigurable</a>;
            default varies
          </p>

          <p>
            The <code translate="no" dir="ltr">visibility</code> attribute controls whether the target can be
            depended on by targets in other locations. See the documentation for
            <a href="/concepts/visibility">visibility</a>.
          </p>

          <p>
            For targets declared directly in a BUILD file or in legacy macros called from
            a BUILD file, the default value is the package's
            <code translate="no" dir="ltr"><a href="/reference/be/functions#package.default_visibility">default_visibility</a></code>
            if specified, or else <code translate="no" dir="ltr">["//visibility:private"]</code>. For targets
            declared in one or more symbolic macros, the default value is always just
            <code translate="no" dir="ltr">["//visibility:private"]</code> (which makes it useable only within the
            package containing the macro's code).
          </p>
          """.trimIndent(),
      ),
    )

  private val testParams =
    listOf(
      BazelGlobalFunctionParameter(
        name = "args",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>List of strings; subject to
          <a href="/reference/be/make-variables#predefined_label_variables">$(location)</a> and
          <a href="/reference/be/make-variables">"Make variable"</a> substitution, and
          <a href="#sh-tokenization">Bourne shell tokenization</a>; default is <code translate="no" dir="ltr">[]</code></p>

          <p>Command line arguments that Bazel passes to the target when it is
          executed with <code translate="no" dir="ltr">bazel test</code>.

          </p><p>
          These arguments are passed before any <code translate="no" dir="ltr">--test_arg</code> values
          specified on the <code translate="no" dir="ltr">bazel test</code> command line.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "env",
        required = false,
        named = true,
        positional = false,
        defaultValue = "{}",
        doc =
          """
          <p>
            Dictionary of strings; values are subject to
            <a href="/reference/be/make-variables#predefined_label_variables">$(location)</a> and
            <a href="/reference/be/make-variables">"Make variable"</a> substitution; default is <code translate="no" dir="ltr">{}</code>
          </p>

          <p>
            Specifies additional environment variables to set when the test is executed by
            <code translate="no" dir="ltr">bazel test</code>.
          </p>

          <p>
            This attribute only applies to native rules, like <code translate="no" dir="ltr">cc_test</code>,
            <code translate="no" dir="ltr">py_test</code>, and <code translate="no" dir="ltr">sh_test</code>. It does not apply to
            Starlark-defined test rules. For your own Starlark rules, you can add an "env"
            attribute and use it to populate a

              <a href="/rules/lib/providers/RunEnvironmentInfo">RunEnvironmentInfo</a>
            Provider.
          </p>
              <a href="/rules/lib/toplevel/testing#TestEnvironment">TestEnvironment</a>

            Provider.
          <p></p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "env_inherit",
        required = false,
        named = true,
        positional = false,
        defaultValue = "[]",
        doc =
          """
          <p>List of strings; default is <code translate="no" dir="ltr">[]</code></p>

          <p>Specifies additional environment variables to inherit from the
            external environment when the test is executed by <code translate="no" dir="ltr">bazel test</code>.
          </p>

          <p>
            This attribute only applies to native rules, like <code translate="no" dir="ltr">cc_test</code>, <code translate="no" dir="ltr">py_test</code>,
            and <code translate="no" dir="ltr">sh_test</code>.  It does not apply to Starlark-defined test rules.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "size",
        required = false,
        named = true,
        positional = false,
        defaultValue = "medium",
        doc =
          """
          <p>String <code translate="no" dir="ltr">"enormous"</code>, <code translate="no" dir="ltr">"large"</code>, <code translate="no" dir="ltr">"medium"</code>, or
            <code translate="no" dir="ltr">"small"</code>; <a href="#configurable-attributes">nonconfigurable</a>;
            default is <code translate="no" dir="ltr">"medium"</code></p>

          <p>Specifies a test target's "heaviness": how much time/resources it needs to run.</p>

          <p>Unit tests are considered "small", integration tests "medium", and end-to-end tests "large" or
          "enormous". Bazel uses the size to determine a default timeout, which can be overridden using the
          <code translate="no" dir="ltr">timeout</code> attribute. The timeout is for all tests in the BUILD target, not for each
          individual test. When the test is run locally, the <code translate="no" dir="ltr">size</code> is additionally used for
          scheduling purposes: Bazel tries to respect <code translate="no" dir="ltr">--local_{ram,cpu}_resources</code> and not
          overwhelm the local machine by running lots of heavy tests at the same time.</p>

          <p>Test sizes correspond to the following default timeouts and assumed peak local resource
          usages:</p>

          <div class="devsite-table-wrapper"><table style="width: 100%">
            <tbody><tr>
              <th>Size</th>
              <th>RAM (in MB)</th>
              <th>CPU (in CPU cores)</th>
              <th>Default timeout</th>
            </tr>
            <tr>
              <td>small</td>
              <td>20</td>
              <td>1</td>
              <td>short (1 minute)</td>
            </tr>
            <tr>
              <td>medium</td>
              <td>100</td>
              <td>1</td>
              <td>moderate (5 minutes)</td>
            </tr>
            <tr>
              <td>large</td>
              <td>300</td>
              <td>1</td>
              <td>long (15 minutes)</td>
            </tr>
            <tr>
              <td>enormous</td>
              <td>800</td>
              <td>1</td>
              <td>eternal (60 minutes)</td>
            </tr>
          </tbody></table></div>

          <p>
            The environment variable
            <code translate="no" dir="ltr"><a href="/reference/test-encyclopedia#initial-conditions">TEST_SIZE</a></code> will be set to
            the value of this attribute when spawning the test.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "timeout",
        required = false,
        named = true,
        positional = false,
        defaultValue = null,
        doc =
          """
          <p>String <code translate="no" dir="ltr">"short"</code>, <code translate="no" dir="ltr">"moderate"</code>, <code translate="no" dir="ltr">"long"</code>, or
            <code translate="no" dir="ltr">"eternal"</code>; <a href="#configurable-attributes">nonconfigurable</a>; default is derived
            from the test's <code translate="no" dir="ltr">size</code> attribute</p>

          <p>
          How long the test is expected to run before returning.
          </p>

          <p>
          While a test's size attribute controls resource estimation, a test's
          timeout may be set independently.  If not explicitly specified, the
          timeout is based on the <a href="#test.size">test's size</a>. The test
          timeout can be overridden with the <code translate="no" dir="ltr">--test_timeout</code> flag, e.g. for
          running under certain conditions which are known to be slow. Test timeout values
          correspond to the following time periods:
          </p>

          <div class="devsite-table-wrapper"><table style="width: 100%">
            <tbody><tr>
              <th>Timeout Value</th>
              <th>Time Period</th>
            </tr>
            <tr>
              <td>short</td>
              <td>1 minute</td>
            </tr>
            <tr>
              <td>moderate</td>
              <td>5 minutes</td>
            </tr>
            <tr>
              <td>long</td>
              <td>15 minutes</td>
            </tr>
            <tr>
              <td>eternal</td>
              <td>60 minutes</td>
            </tr>
          </tbody></table></div>

          <p>
          For times other than the above, the test timeout can be overridden with the
          <code translate="no" dir="ltr">--test_timeout</code> bazel flag, e.g. for manually running under
          conditions which are known to be slow. The <code translate="no" dir="ltr">--test_timeout</code> values
          are in seconds. For example <code translate="no" dir="ltr">--test_timeout=120</code> will set the test
          timeout to two minutes.
          </p>

          <p>
            The environment variable
            <code translate="no" dir="ltr"><a href="/reference/test-encyclopedia#initial-conditions">TEST_TIMEOUT</a></code> will be set
            to the test timeout (in seconds) when spawning the test.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "flaky",
        required = false,
        named = true,
        positional = false,
        defaultValue = "False",
        doc =
          """
          <p>Boolean; <a href="#configurable-attributes">nonconfigurable</a>;
            default is <code translate="no" dir="ltr">False</code></p>

          <p>
          Marks test as flaky.
          </p>

          <p>
          If set, executes the test up to three times, marking it as failed only if it
          fails each time. By default, this attribute is set to False and the test is
          executed only once. Note, that use of this attribute is generally discouraged -
          tests should pass reliably when their assertions are upheld.
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "shard_count",
        required = false,
        named = true,
        positional = false,
        defaultValue = "-1",
        doc =
          """
          <p>Non-negative integer less than or equal to 50; default is <code translate="no" dir="ltr">-1</code></p>

          <p>Specifies the number of parallel shards
          to use to run the test.</p>

          <p>If set, this value will override any heuristics used to determine the number of
          parallel shards with which to run the test. Note that for some test
          rules, this parameter may be required to enable sharding
          in the first place. Also see <code translate="no" dir="ltr">--test_sharding_strategy</code>.</p>

          <p>If test sharding is enabled, the environment variable <code translate="no" dir="ltr">
          <a href="/reference/test-encyclopedia#initial-conditions">TEST_TOTAL_SHARDS</a>
          </code> will be set to this value when spawning the test.</p>

          <p>Sharding requires the test runner to support the test sharding protocol.
          If it does not, then it will most likely run every test in every shard, which
          is not what you want.</p>

          <p>See
          <a href="/reference/test-encyclopedia#test-sharding">Test Sharding</a>
          in the Test Encyclopedia for details on sharding.</p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "local",
        required = false,
        named = true,
        positional = false,
        defaultValue = "False",
        doc =
          """
          <p>Boolean; <a href="#configurable-attributes">nonconfigurable</a>;
            default is <code translate="no" dir="ltr">False</code></p>

          <p>Forces the test to be run locally, without sandboxing.</p>

          <p>Setting this to True is equivalent to providing "local" as a tag
          (<code translate="no" dir="ltr">tags=["local"]</code>).</p>
          """.trimIndent(),
      ),
    )

  val binaryParams =
    listOf(
      BazelGlobalFunctionParameter(
        name = "args",
        required = false,
        positional = false,
        named = true,
        defaultValue = "[]",
        doc =
          """
          <p>
            List of strings; subject to
            <a href="/reference/be/make-variables#predefined_label_variables">$(location)</a> and
            <a href="/reference/be/make-variables">"Make variable"</a> substitution, and
            <a href="#sh-tokenization">Bourne shell tokenization</a>;
            <a href="#configurable-attributes">nonconfigurable</a>;
            default is <code translate="no" dir="ltr">[]</code>
          </p>

          <p>
          Command line arguments that Bazel will pass to the target when it is executed
          either by the <code translate="no" dir="ltr">run</code> command or as a test. These arguments are
          passed before the ones that are specified on the <code translate="no" dir="ltr">bazel run</code> or
          <code translate="no" dir="ltr">bazel test</code> command line.
          </p>

          <p>
          <em class="harmful">NOTE: The arguments are not passed when you run the target
          outside of Bazel (for example, by manually executing the binary in
          <code translate="no" dir="ltr">bazel-bin/</code>).</em>
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "env",
        required = false,
        positional = false,
        named = true,
        defaultValue = "{}",
        doc =
          """
          <p>Dictionary of strings; values are subject to
          <a href="/reference/be/make-variables#predefined_label_variables">$(location)</a> and
          <a href="/reference/be/make-variables">"Make variable"</a> substitution; default is <code translate="no" dir="ltr">{}</code></p>

          <p>Specifies additional environment variables to set when the target is
            executed by <code translate="no" dir="ltr">bazel run</code>.
          </p>

          <p>
            This attribute only applies to native rules, like <code translate="no" dir="ltr">cc_binary</code>, <code translate="no" dir="ltr">py_binary</code>,
            and <code translate="no" dir="ltr">sh_binary</code>.  It does not apply to Starlark-defined executable rules. For your own
            Starlark rules, you can add an "env" attribute and use it to populate a

              <a href="/rules/lib/providers/RunEnvironmentInfo">RunEnvironmentInfo</a>

            Provider.
          </p>

          <p>
          <em class="harmful">NOTE: The environment variables are not set when you run the target
          outside of Bazel (for example, by manually executing the binary in
          <code translate="no" dir="ltr">bazel-bin/</code>).</em>
          </p>
          """.trimIndent(),
      ),
      BazelGlobalFunctionParameter(
        name = "output_licenses",
        required = false,
        positional = false,
        named = true,
        defaultValue = "[]",
        doc =
          """
          <p>List of strings; default is <code translate="no" dir="ltr">[]</code></p>

          <p>
          The licenses of the output files that this binary generates.

          This is part of a deprecated licensing API that Bazel no longer uses. Don't
          use this.
          </p>
          """.trimIndent(),
      ),
    )

  private fun removeLinks(input: String): String {
    var result = input.replace(Regex("<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL), "$1")
    result = result.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
    return result
  }

  private fun replaceTicks(input: String): String {
    var result = input.replace(Regex("```(.*)```", RegexOption.DOT_MATCHES_ALL), "<code>$1</code>")
    result = result.replace(Regex("`(.*?)`", RegexOption.DOT_MATCHES_ALL), "<code>$1</code>")
    return result
  }

  private fun defaultNameOrNull(name: String): String? = if (name == "") null else name

  private fun unwrapName(name: String): String =
    if (name.contains(".")) {
      name.split(".")[1]
    } else {
      name
    }

  private fun attributeInfoToData(attrInfo: StardocOutputProtos.AttributeInfo): BazelGlobalFunctionParameter =
    BazelGlobalFunctionParameter(
      name = attrInfo.name,
      doc = if (attrInfo.docString.isEmpty()) null else replaceTicks(removeLinks(attrInfo.docString)),
      required = attrInfo.mandatory,
      defaultValue = defaultNameOrNull(attrInfo.defaultValue),
      positional = false,
      named = true,
    )

  private fun joinParamLists(
    a: List<BazelGlobalFunctionParameter>,
    b: List<BazelGlobalFunctionParameter>,
  ): List<BazelGlobalFunctionParameter> {
    val res = mutableListOf<BazelGlobalFunctionParameter>()

    for (rule in a) {
      val corresponding = b.firstOrNull { it.name == rule.name }
      if (corresponding == null) {
        res.add(rule)
      } else if (rule.doc == null) {
        res.add(rule.copy(doc = corresponding.doc))
      } else {
        res.add(rule)
      }
    }

    return res
  }

  private fun ruleInfoToData(ruleInfo: StardocOutputProtos.RuleInfo): BazelGlobalFunction {
    var attributes = ruleInfo.attributeList.map { attributeInfoToData(it) }
    val name = unwrapName(ruleInfo.ruleName)
    attributes = joinParamLists(attributes, commonParams)

    attributes =
      if (name.endsWith("_test")) {
        joinParamLists(attributes, testParams)
      } else if (name.endsWith("_binary")) {
        joinParamLists(attributes, binaryParams)
      } else {
        attributes
      }

    return BazelGlobalFunction(
      name = name,
      doc = replaceTicks(removeLinks(ruleInfo.docString)),
      environment = listOf(Environment.BUILD),
      params = attributes,
    )
  }

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.isEmpty()) {
      println("Usage: reader <input_file>")
      exitProcess(1)
    }

    val allRules = mutableListOf<BazelGlobalFunction>()

    for (inputFilePath in args) {
      val moduleInfo =
        FileInputStream(inputFilePath).use { input ->
          StardocOutputProtos.ModuleInfo.parseFrom(input)
        }

      for (rule in moduleInfo.ruleInfoList) {
        val ruleData = ruleInfoToData(rule)
        allRules.add(ruleData)
      }
    }

    println(serializeFunctionsTo(allRules))
  }
}
