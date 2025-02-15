load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "notifications",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
