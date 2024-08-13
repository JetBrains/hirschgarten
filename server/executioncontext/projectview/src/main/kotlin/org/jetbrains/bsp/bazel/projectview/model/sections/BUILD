load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model:__pkg__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/model/sections:__pkg__",
    ],
    exports = [
        "@maven//:ch_epfl_scala_bsp4j",
    ],
    deps = [
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
