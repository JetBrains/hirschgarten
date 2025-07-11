load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "legacy",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/projectStructure/workspaceModel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
    ],
)
