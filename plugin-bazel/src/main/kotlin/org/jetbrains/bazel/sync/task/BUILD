load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "task",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/performance",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/projectStructure",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/ids",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
