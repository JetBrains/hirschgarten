load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "generator",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview:__pkg__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bazel/projectview/generator:__pkg__",
    ],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
    deps = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/generator/sections",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model",
    ],
)
