load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "coroutines",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
