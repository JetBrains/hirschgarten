load(":commons.bzl", "kt_test")

INTELLIJ_DEPS = [
    "@rules_intellij//testing:lib",
    "@rules_intellij//intellij_platform_sdk:plugin_api_for_tests",
    # Usually, we'd get this from the JetBrains SDK, but the bundled one not aware of Bazel platforms,
    # so it fails on certain setups.
    "@jna//jar",
]

INTELLIJ_RUNTIME_DEPS = [
    "@rules_intellij//intellij_platform_sdk:bundled_plugins",
    "@rules_intellij//intellij_platform_sdk:plugin_api_for_tests",
]

JVM_FLAGS = [
    "-Didea.classpath.index.enabled=false",
    "-Djava.awt.headless=true",
    "-Djunit.jupiter.extensions.autodetection.enabled=true",
    "-Didea.force.use.core.classloader=true",
    "-Djava.system.class.loader=com.intellij.util.lang.PathClassLoader",
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

def kt_intellij_junit4_test(deps = [], runtime_deps = [], jvm_flags = [], **kwargs):
    """Test macro which adds all the necessary things to run the test with IntelliJ IDEA fixtures.
    All the test fixtures defined for IntelliJ IDEA support only Junit4, so if you want to use one of them
    (e.g. UsefulTestCase) please use this macro.
    """
    kt_test(
        deps = INTELLIJ_DEPS + deps,
        runtime_deps = INTELLIJ_RUNTIME_DEPS + runtime_deps,
        jvm_flags = JVM_FLAGS + ADD_OPENS_FLAGS + jvm_flags,
        **kwargs
    )
