load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "hotswap",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src:intellij-bazel-unpackaged"],
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
