#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
"""Custom rule for creating IntelliJ plugin tests.
"""

load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
load(
    "//rules_intellij/build_defs:build_defs.bzl",
    "api_version_txt",
)

ADD_OPENS = [
    "--add-opens=%s=ALL-UNNAMED" % x
    for x in [
        # keep sorted
        "java.base/java.io",
        "java.base/java.lang",
        "java.base/java.nio",
        "java.base/java.util",
        "java.base/java.util.concurrent",
        "java.base/jdk.internal.vm",
        "java.base/sun.nio.fs",
        "java.desktop/java.awt",
        "java.desktop/java.awt.event",
        "java.desktop/javax.swing",
        "java.desktop/javax.swing.plaf.basic",
        "java.desktop/javax.swing.text",
        "java.desktop/javax.swing.text.html",
        "java.desktop/javax.swing.text.html.parser",
        "java.desktop/sun.awt",
        "java.desktop/sun.awt.image",
        "java.desktop/sun.font",
    ]
]

def _generate_test_suite_impl(ctx):
    """Generates a JUnit5 test suite pulling in all the referenced classes.

    Args:
      ctx: the rule context
    """
    suite_class_name = ctx.label.name
    lines = []
    lines.append("package %s;" % ctx.attr.test_package_root)
    lines.append("")
    test_srcs = _get_test_srcs(ctx.attr.srcs)
    test_classes = [_get_test_class(test_src, ctx.attr.test_package_root) for test_src in test_srcs]
    class_rules = ctx.attr.class_rules
    if (class_rules):
        lines.append("import org.junit.jupiter.api.extension.RegisterExtension;")
    lines.append("import org.junit.platform.suite.api.SelectClasses;")
    lines.append("import org.junit.platform.suite.api.Suite;")
    lines.append("import org.junit.runner.RunWith;")
    lines.append("import org.junit.jupiter.api.extension.ExtendWith;")
    lines.append("import org.junit.jupiter.api.BeforeAll;")
    lines.append("import com.google.idea.testing.BlazeTestSystemProperties;")
    lines.append("")
    for test_class in test_classes:
        lines.append("import %s;" % test_class)
    lines.append("")
    lines.append("@Suite")
    lines.append("@SelectClasses({")
    for test_class in test_classes:
        lines.append("    %s.class," % test_class.split(".")[-1])
    lines.append("})")
    lines.append("public class %s {" % suite_class_name)
    lines.append("")

    i = 1
    for class_rule in class_rules:
        lines.append("@RegisterExtension")
        lines.append("public static %s setupRule_%d = new %s();" % (class_rule, i, class_rule))
        i += 1

    lines.append("}")

    contents = "\n".join(lines)
    ctx.actions.write(
        output = ctx.outputs.out,
        content = contents,
    )

_generate_test_suite = rule(
    implementation = _generate_test_suite_impl,
    attrs = {
        # srcs for the test classes included in the suite (only keep those ending in Test.java)
        "srcs": attr.label_list(allow_files = True, mandatory = True),
        # the package string of the output test suite.
        "test_package_root": attr.string(mandatory = True),
        # optional list of classes to instantiate as a @ClassRule in the test suite.
        "class_rules": attr.string_list(),
        "run_with": attr.string(default = "com.google.idea.testing.IntellijJunit5Extension"),
    },
    outputs = {"out": "%{name}.java"},
)

def intellij_unit_test_suite(
        name,
        srcs,
        test_package_root,
        class_rules = [],
        size = "medium",
        **kwargs):
    """Creates a java_test rule composed of all valid test classes in the specified srcs.

    The resulting environment is minimal and any interactions with IntelliJ need additional dependencies.

    Notes:
      Only classes ending in "Test.java" will be recognized.

    Args:
      name: name of this rule.
      srcs: the test classes.
      test_package_root: only tests under this package root will be run.
      class_rules: JUnit class rules to apply to these tests.
      size: the test size.
      **kwargs: Any other args to be passed to the java_test.
    """
    suite_class_name = name + "TestSuite"
    suite_class = test_package_root + "." + suite_class_name

    api_version_txt_name = name + "_api_version"
    api_version_txt(name = api_version_txt_name, check_eap = False)
    data = kwargs.pop("data", [])
    data.append(api_version_txt_name)

    deps = kwargs.pop("deps", [])
    deps.extend([
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_junit_platform_junit_platform_console",
        "@maven//:org_junit_platform_junit_platform_suite_engine",
        "@maven//:org_junit_platform_junit_platform_suite_api",
    ])

    jvm_flags = list(kwargs.pop("jvm_flags", []))
    jvm_flags.extend([
        "-Didea.classpath.index.enabled=false",
        "-Djava.awt.headless=true",
        "-Dblaze.idea.api.version.file=$(location %s)" % api_version_txt_name,
    ])
    jvm_flags.extend(ADD_OPENS)

    _generate_test_suite(
        name = suite_class_name,
        srcs = srcs,
        test_package_root = test_package_root,
        class_rules = class_rules,
    )
    kt_jvm_test(
        name = name,
        size = size,
        srcs = srcs + [suite_class_name],
        data = data,
        deps = deps,
        args = ["--select-class=" + test_package_root + "." + suite_class_name, "--fail-if-no-tests", "-e", "junit-platform-suite"],
        jvm_flags = jvm_flags,
        test_class = suite_class,
        **kwargs
    )

def _generate_directory(ctx):
    dir = ctx.actions.declare_directory(ctx.attr.dir)
    ctx.actions.run_shell(
        outputs = [dir],
        command = "mkdir -p %s" % ctx.attr.dir,
    )
    return DefaultInfo(
        files = depset([dir]),
    )

generate_directory = rule(
    implementation = _generate_directory,
    attrs = {
        "dir": attr.string(),
    },
)

def intellij_integration_test_suite(
        name,
        srcs,
        test_package_root,
        deps,
        additional_class_rules = [],
        size = "medium",
        jvm_flags = [],
        runtime_deps = [],
        required_plugins = None,
        **kwargs):
    """Creates a java_test rule composed of all valid test classes in the specified srcs and specifically tailored for intellij-ide integration tests.

    The resulting testing environment runs IntelliJ in headless mode.
    The IntelliJ instance has system properties set,
    folders set up, and will contain bundled plugins and testing libraries.
    The tests can depend on other plugins via the required_plugins parameter
    and have runtime dependencies on (e.g., Protobuf libraries) via runtime_deps.

    The additional scaffolding makes tests slower to execute
    and more prone to breakage by upstream changes,
    hence, intellij_unit_test_suite should be preferred.

    Notes:
      Only classes ending in "Test.java" will be recognized.
      All test classes must be located in the blaze package calling this function.

    Args:
      name: name of this rule.
      srcs: the test classes.
      test_package_root: only tests under this package root will be run.
      deps: the required deps.
      size: the test size.
      jvm_flags: extra flags to be passed to the test vm.
      runtime_deps: the required runtime dependencies, (e.g., intellij_plugin targets).
      required_plugins: optional comma-separated list of plugin IDs. Integration tests will fail if
          these plugins aren't loaded at runtime.
      **kwargs: Any other args to be passed to the java_test.
    """
    suite_class_name = name + "TestSuite"
    suite_class = test_package_root + "." + suite_class_name
    _generate_test_suite(
        name = suite_class_name,
        srcs = srcs,
        test_package_root = test_package_root,
        class_rules = additional_class_rules,
        run_with = "com.google.idea.testing.IntellijJunit5Extension",
    )

    api_version_txt_name = name + "_api_version"
    api_version_txt(name = api_version_txt_name, check_eap = False)
    data = kwargs.pop("data", [])
    data.append(api_version_txt_name)

    deps = list(deps)
    deps.extend([
        "//rules_intellij/testing:lib",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_junit_jupiter_junit_jupiter_engine",
        "@maven//:org_junit_platform_junit_platform_console",
        "@maven//:org_junit_platform_junit_platform_suite_engine",
        "@maven//:org_junit_platform_junit_platform_suite_api",
        # Usually, we'd get this from the JetBrains SDK, but the bundled one not aware of Bazel platforms,
        # so it fails on certain setups.
        "@jna//jar",
    ])
    runtime_deps = list(runtime_deps)
    runtime_deps.extend([
        "//rules_intellij/intellij_platform_sdk:bundled_plugins",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
    ])

    resources = kwargs.pop("resources", [])

    prefs_name = name + "_prefs"

    generate_directory(
        name = prefs_name,
        dir = prefs_name + "_dir",
    )

    data.append(prefs_name)

    jvm_flags = list(jvm_flags)
    jvm_flags.extend([
        "-Didea.classpath.index.enabled=false",
        "-Djava.awt.headless=true",
        "-Djunit.jupiter.extensions.autodetection.enabled=true",
        "-Djava.util.prefs.userRoot=$(location %s)" % prefs_name,
        "-Didea.force.use.core.classloader=true",
        "-Djava.system.class.loader=com.intellij.util.lang.PathClassLoader",
        "-Dblaze.idea.api.version.file=$(location %s)" % api_version_txt_name,
    ])
    jvm_flags.extend(ADD_OPENS)

    if required_plugins:
        jvm_flags.append("-Didea.required.plugins.id=" + required_plugins)

    main_class = kwargs.pop("main_class", "org.junit.platform.console.ConsoleLauncher")

    args = kwargs.pop("args", [])
    args.append("--disable-banner")
    args.append("--select-class=" + test_package_root + "." + suite_class_name)
    args.append("--fail-if-no-tests")
    args.append("-e")
    args.append("junit-platform-suite")
    kt_jvm_test(
        name = name,
        size = size,
        srcs = srcs + [suite_class_name],
        data = data,
        args = args,
        resources = resources,
        jvm_flags = jvm_flags,
        test_class = suite_class,
        runtime_deps = runtime_deps,
        deps = deps,
        main_class = main_class,
        **kwargs
    )

def _get_test_class(test_src, test_package_root):
    """Returns the package string of the test class, beginning with the given root."""
    test_path = test_src.short_path
    temp = test_path[:-len(".kt")]
    temp = temp.replace("/", ".")
    i = temp.rfind(test_package_root)
    if i < 0:
        fail("Test source '%s' not under package root '%s'" % (test_path, test_package_root))
    test_class = temp[i:]
    return test_class

def _get_test_srcs(targets):
    """Returns all files of the given targets that end with Test.java."""
    files = depset()
    for target in targets:
        files = depset(transitive = [files, target.files])
    return [f for f in files.to_list() if f.basename.endswith("Test.kt")]
