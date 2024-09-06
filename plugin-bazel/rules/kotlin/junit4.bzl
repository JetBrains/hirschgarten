load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

DEPS = [
    "@maven//:io_kotest_kotest_assertions_api_jvm",
    "@maven//:io_kotest_kotest_assertions_core_jvm",
    "@maven//:io_kotest_kotest_assertions_shared_jvm",
    "@maven//:io_kotest_kotest_common_jvm",
    "//plugin-bazel/src:intellij-bazel",
    "//plugin-bsp/src:intellij-bsp",
    "@rules_intellij//testing:lib",
    "@rules_intellij//intellij_platform_sdk:plugin_api_for_tests",
    "@jna//jar",
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

def junit4_kt_test(name, srcs, classname = "", deps = [], runtime_deps = [], **kwargs):
    if (classname == ""):
        classname = _guess_classname(name)

    kt_jvm_test(
        name = name,
        args = ["--select-class=" + classname, "--fail-if-no-tests"],
        test_class = classname,
        srcs = srcs,
        deps = deps + DEPS,
        runtime_deps = runtime_deps + [
            "@rules_intellij//intellij_platform_sdk:bundled_plugins",
            "@rules_intellij//intellij_platform_sdk:plugin_api_for_tests",
            "//plugin-bazel:intellij-bazel",
            "//plugin-bsp:intellij-bsp",
        ],
        jvm_flags = JVM_FLAGS + ADD_OPENS_FLAGS,
        **kwargs
    )

def _guess_classname(name):
    package = _guess_class_package()

    return package + "." + name

def _guess_class_package():
    package_name = native.package_name()
    package_name_without_last_slash = package_name.rstrip("/")
    _, _, package_name_without_org = package_name_without_last_slash.partition("/org/")
    real_package_name_with_slashes = "org/" + package_name_without_org
    package_name_with_dots = real_package_name_with_slashes.replace("/", ".")

    return package_name_with_dots
