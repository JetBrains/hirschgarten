load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "splitter",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser:__subpackages__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter:__pkg__",
    ],
)
