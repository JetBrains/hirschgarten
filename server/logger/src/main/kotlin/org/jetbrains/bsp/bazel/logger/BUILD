load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "logger",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    visibility = ["//server/logger:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/protocol",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
