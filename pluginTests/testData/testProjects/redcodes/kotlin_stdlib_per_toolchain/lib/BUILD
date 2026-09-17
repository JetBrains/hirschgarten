# `//kotlin:jvm.bzl` skips the Android rules, which do not load under Bazel 9
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "lib",
    srcs = select({
        "//settings:mode_a": ["a/Lib.kt"],
        "//settings:mode_b": ["b/Lib.kt"],
    }),
    visibility = ["//visibility:public"],
)
