load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "android",
    srcs = glob(["*.kt"]),
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/utils",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:commons_io_commons_io",
    ],
)
