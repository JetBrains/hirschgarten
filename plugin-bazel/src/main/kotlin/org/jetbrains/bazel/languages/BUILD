load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "languages",
    srcs = glob([
        "**/*.kt",
        "**/*.java",
    ]),
    visibility = [
        "//plugin-bazel:__subpackages__",
        "//tools/starlark_data_generation:__subpackages__",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/bazelrc",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/starlark/repomapping",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/terminal",
    ],
)
