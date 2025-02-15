load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "diagnostics",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel/server:__subpackages__"],
    deps = [
        "//plugin-bazel/bazelrunner",
        "//plugin-bazel/commons",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
