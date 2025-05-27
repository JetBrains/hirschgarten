load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "io",
    srcs = glob(["*.kt", "*.java"], allow_empty = True),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
