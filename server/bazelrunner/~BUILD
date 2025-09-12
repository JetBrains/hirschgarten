load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "params",
    visibility = ["//visibility:public"],
    exports = [
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/params",
    ],
)

java_library(
    name = "bazelrunner",
    visibility = ["//visibility:public"],
    exports = [
        "//server/bazelrunner:params",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner",
    ],
)
