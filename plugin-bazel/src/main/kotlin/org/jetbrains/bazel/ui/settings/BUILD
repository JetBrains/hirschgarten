load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "settings",
    srcs = glob(["*.kt"]),
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/settings/bazel",
    ],
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
