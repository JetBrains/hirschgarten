load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "projectAware",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/buildTask",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
