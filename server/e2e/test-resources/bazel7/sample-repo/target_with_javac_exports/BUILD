load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "java_library",
    srcs = ["JavaLibrary.java"],
    add_exports = [
        "java.base/jdk.internal.misc",
        "jdk.compiler/com.sun.tools.javac.comp",
    ],
    add_opens = ["java.base/java.lang.invoke"],
    javacopts = ["-XepDisableAllChecks"],
    visibility = ["//visibility:public"],
)
