load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "label",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = ["@maven//:ch_epfl_scala_bsp4j"],
)
