load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "model",
    visibility = ["//visibility:public"],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
)

java_library(
    name = "generator",
    visibility = ["//visibility:public"],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/generator",
    ],
)

java_library(
    name = "parser",
    visibility = ["//visibility:public"],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser",
    ],
)
