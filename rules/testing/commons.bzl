"""Shared constants for running the IntelliJ plugin tests from this open-source checkout.
"""

KOTEST_DEPS = [
    "@maven//:io_kotest_kotest_assertions_api_jvm",
    "@maven//:io_kotest_kotest_assertions_core_jvm",
    "@maven//:io_kotest_kotest_assertions_shared_jvm",
    "@maven//:io_kotest_kotest_common_jvm",
]

JUNIT_DEPS = [
    "@maven//:junit_junit",
    "@maven//:org_junit_jupiter_junit_jupiter",
    "@maven//:org_junit_jupiter_junit_jupiter_api",
    "@maven//:org_junit_jupiter_junit_jupiter_params",
    "@maven//:org_junit_platform_junit_platform_commons",
    "@maven//:org_junit_platform_junit_platform_launcher",
    "@maven//:org_mockito_mockito_core",
]

ENGINE_DEPS = [
    "@maven//:org_junit_vintage_junit_vintage_engine",
    "@maven//:net_java_dev_jna_jna",
    "@maven//:org_jetbrains_pty4j_pty4j",
]

PKGS = [
    "java.base/java.io",
    "java.base/java.lang",
    "java.base/java.lang.ref",
    "java.base/java.lang.reflect",
    "java.base/java.net",
    "java.base/java.nio",
    "java.base/java.nio.charset",
    "java.base/java.text",
    "java.base/java.time",
    "java.base/java.util",
    "java.base/java.util.concurrent",
    "java.base/java.util.concurrent.atomic",
    "java.base/jdk.internal.ref",
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
    "java.desktop/javax.swing.text",
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
    "java.desktop/sun.swing.text",
    "jdk.attach/sun.tools.attach",
    "jdk.compiler/com.sun.tools.javac.api",
    "jdk.internal.jvmstat/sun.jvmstat.monitor",
    "jdk.jdi/com.sun.tools.jdi",
]

ADD_OPENS_FLAGS = ["--add-opens=" + pkg + "=ALL-UNNAMED" for pkg in PKGS]

INTELLIJ_DEPS = [
    "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
    "//rules_intellij/intellij_platform_sdk:java_for_tests",
    "//rules_intellij/intellij_platform_sdk:kotlin_for_tests",
    "//rules_intellij/third_party/code_with_me:code_with_me_for_tests",
    "//rules_intellij/third_party/go:go_for_tests",
    "//rules_intellij/third_party/python:python_for_tests",
    "//rules_intellij/third_party/terminal:terminal_for_tests",
    "//rules_intellij/intellij_platform_sdk:test_framework",
    "//rules_intellij/intellij_platform_sdk:bundled_plugins_for_tests",
    "//rules_intellij/intellij_platform_sdk:bytecode_viewer_for_tests",
    "//rules_intellij/intellij_platform_sdk:junit_for_tests",
    "//rules_intellij/intellij_platform_sdk:testrunner_for_tests",
    "//rules_intellij/third_party/devkit:devkit_for_tests",
    "//rules_intellij/third_party/performance:performance_for_tests",
    "//rules_intellij/third_party/protobuf:protoedit_for_tests",
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
    "-Didea.reset.classpath.from.manifest=true",
    "-Dintellij.build.use.compiled.classes=false",
    "-Djava.util.zip.use.nio.for.zip.file.access=true",
    "-ea",
]

TEST_DEPS = KOTEST_DEPS + JUNIT_DEPS + INTELLIJ_DEPS + [
    "@maven//:org_jetbrains_kotlin_kotlin_test",
    "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    "@maven//:org_assertj_assertj_core",
]
