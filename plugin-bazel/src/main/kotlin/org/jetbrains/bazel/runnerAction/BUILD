load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "runnerAction",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/tasks",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace/languages/java",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
