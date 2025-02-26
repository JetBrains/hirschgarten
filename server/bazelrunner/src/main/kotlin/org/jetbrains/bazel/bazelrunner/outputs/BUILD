load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "outputs",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/utils",
        "//server/commons",
        "//server/logger",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
