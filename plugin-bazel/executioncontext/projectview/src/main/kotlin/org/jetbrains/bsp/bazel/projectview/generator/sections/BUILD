load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/generator:__pkg__",
        "//plugin-bazel/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/generator/sections:__pkg__",
    ],
    exports = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
    ],
    deps = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
    ],
)
