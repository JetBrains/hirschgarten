load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/generator:__pkg__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/generator/sections:__pkg__",
    ],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
    ],
    deps = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
    ],
)
