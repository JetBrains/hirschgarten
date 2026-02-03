"""
load("@contrib_rules_jvm//java/private:create_jvm_test_suite.bzl", "create_jvm_test_suite")
load("@contrib_rules_jvm//java/private:junit5.bzl", "java_junit5_test")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")
load("//rules/testing/stamper:stamper_rules.bzl", "jvm_test_stamper")
load("//rules_intellij/build_defs:build_defs.bzl", "api_version_txt")

KOTEST_DEPS = [
    "@maven//:io_kotest_kotest_assertions_api_jvm",
    "@maven//:io_kotest_kotest_assertions_core_jvm",
    "@maven//:io_kotest_kotest_assertions_shared_jvm",
    "@maven//:io_kotest_kotest_common_jvm",
]

JUNIT_DEPS = [
    "@maven//:org_junit_jupiter_junit_jupiter",
    "@maven//:org_junit_jupiter_junit_jupiter_api",
    "@maven//:org_junit_jupiter_junit_jupiter_params",
    "@maven//:org_junit_platform_junit_platform_console",
    "@maven//:org_mockito_mockito_core",
]

PKGS = [
    "java.base/java.io",
    "java.base/java.lang",
    "java.base/java.lang.reflect",
    "java.base/java.net",
    "java.base/java.nio",
    "java.base/java.nio.charset",
    "java.base/java.text",
    "java.base/java.time",
    "java.base/java.util",
    "java.base/java.util.concurrent",
    "java.base/java.util.concurrent.atomic",
    "java.base/jdk.internal.vm",
    "java.base/sun.nio.ch",
    "java.base/sun.nio.fs",
    "java.base/sun.security.ssl",
    "java.base/sun.security.util",
    "java.desktop/com.apple.eawt",
    "java.desktop/com.apple.eawt.event",
    "java.desktop/com.apple.laf",
    "java.desktop/java.awt",
    "java.desktop/java.awt.dnd.peer",
    "java.desktop/java.awt.event",
    "java.desktop/java.awt.image",
    "java.desktop/java.awt.peer",
    "java.desktop/java.awt.font",
    "java.desktop/javax.swing",
    "java.desktop/javax.swing.plaf.basic",
    "java.desktop/javax.swing.text.html",
    "java.desktop/javax.swing.text.html.parser",
    "java.desktop/sun.awt.datatransfer",
    "java.desktop/sun.awt.image",
    "java.desktop/sun.awt",
    "java.desktop/sun.font",
    "java.desktop/sun.java2d",
    "java.desktop/sun.lwawt",
    "java.desktop/sun.lwawt.macosx",
    "java.desktop/sun.swing",
    "jdk.attach/sun.tools.attach",
    "jdk.compiler/com.sun.tools.javac.api",
    "jdk.internal.jvmstat/sun.jvmstat.monitor",
    "jdk.jdi/com.sun.tools.jdi",
]

ADD_OPENS_FLAGS = ["--add-opens=" + pkg + "=ALL-UNNAMED" for pkg in PKGS]

INTELLIJ_DEPS = [
    "//rules_intellij/testing:lib",
    "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
    "//rules_intellij/intellij_platform_sdk:java_for_tests",
    "//rules_intellij/intellij_platform_sdk:kotlin_for_tests",
    "//rules_intellij/third_party/code_with_me:code_with_me_for_tests",
    "//rules_intellij/third_party/go:go_for_tests",
    "//rules_intellij/third_party/python:python_for_tests",
    "//rules_intellij/third_party/terminal:terminal_for_tests",
    # Usually, we'd get this from the JetBrains SDK, but the bundled one not aware of Bazel platforms,
    # so it fails on certain setups.
    "@jna//jar",
]

INTELLIJ_RUNTIME_DEPS = [
    "//rules_intellij/intellij_platform_sdk:bundled_plugins",
    "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
    "//rules_intellij/intellij_platform_sdk:kotlin_for_tests",
]

INTELLIJ_JVM_FLAGS = [
    "-Didea.classpath.index.enabled=false",
    "-Djava.awt.headless=true",
    "-Djunit.jupiter.extensions.autodetection.enabled=true",
    "-Didea.force.use.core.classloader=true",
    "-Djava.system.class.loader=com.intellij.util.lang.PathClassLoader",
]

def kt_test_lib(name, srcs = [], deps = [], **kwargs):
    kt_jvm_library(
        name = name,
        srcs = srcs,
        deps = deps + KOTEST_DEPS + JUNIT_DEPS,
        **kwargs
    )

def _kt_define_library(name, **kwargs):
    kt_jvm_library(
        name = name,
        **kwargs
    )
    return name

def _kt_define_test(name, **kwargs):
    _attr_size = kwargs.pop("size", None)
    _attr_test_class = kwargs.pop("test_class", None)
    _attr_jvm_flags = kwargs.pop("jvm_flags", [])

    lib_name = "_{}".format(name)
    kt_test_lib(
        name = lib_name,
        testonly = True,
        **kwargs
    )

    stamper_name = "stamper__{}".format(name)
    jvm_test_stamper(
        name = stamper_name,
        targets = [
            ":{}".format(lib_name),
        ],
        testonly = True,
        tags = ["no-ide"],
    )

    api_version_txt_name = name + "_api_version"
    api_version_txt(name = api_version_txt_name, check_eap = False)
    data = kwargs.pop("data", [])
    data.append(api_version_txt_name)

    kt_jvm_test(
        data = data,
        name = name,
        srcs = [],
        tags = ["exclusive"],
        main_class = "org.jetbrains.bazel.test.runner.TestRunner",
        jvm_flags = [
            "-Dblaze.idea.api.version.file=$(location %s)" % api_version_txt_name,
        ] + ADD_OPENS_FLAGS + INTELLIJ_JVM_FLAGS + _attr_jvm_flags,
        runtime_deps = [
            ":{}".format(stamper_name),
            ":{}".format(lib_name),
            "//rules/testing/runner:test_runner",
        ] + INTELLIJ_RUNTIME_DEPS,
    )

    return name

def kt_test_suite(name, srcs, test_suffixes = ["Test.kt"], deps = [], runtime_deps = [], size = "large", **kwargs):
    create_jvm_test_suite(
        name,
        srcs = srcs,
        package = None,
        test_suffixes = test_suffixes,
        define_library = _kt_define_library,
        define_test = _kt_define_test,
        deps = deps + KOTEST_DEPS + JUNIT_DEPS,
        runtime_deps = runtime_deps,
        size = size,
        **kwargs
    )
"""
