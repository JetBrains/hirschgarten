load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "logger",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    visibility = ["//server/logger:__subpackages__"],
    deps = [
        "//protocol",
        "//server/commons",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
