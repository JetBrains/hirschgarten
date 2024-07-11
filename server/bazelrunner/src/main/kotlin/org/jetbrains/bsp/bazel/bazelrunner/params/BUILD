load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "params",
    srcs = glob(["*.kt"]),
    visibility = ["//server/bazelrunner:__subpackages__"],
    deps = [
        "//server/commons",
    ],
)
