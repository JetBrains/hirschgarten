load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "widgets",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/debug",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/bazelquery",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/runnerAction",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/bazel",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/bazelrunner",
    ],
)
