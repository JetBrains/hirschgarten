load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bazelrc",
    srcs = glob(["**/*.kt", "**/*.java"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
