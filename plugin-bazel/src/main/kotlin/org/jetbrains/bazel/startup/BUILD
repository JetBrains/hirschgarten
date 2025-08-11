load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "startup",
    srcs = glob(["*.kt"]),
    visibility = [
        "//commons:__subpackages__",
        "//plugin-bazel:__subpackages__",
        "//server:__subpackages__",
    ],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow/sync/bazelPaths",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/settings",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/widgets/fileTargets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
