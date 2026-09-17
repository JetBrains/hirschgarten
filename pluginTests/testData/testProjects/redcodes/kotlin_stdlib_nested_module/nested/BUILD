# `//kotlin:jvm.bzl` skips the Android rules, which do not load under Bazel 9
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "lib",
    srcs = ["Lib.kt"],
    visibility = ["//visibility:public"],
)
