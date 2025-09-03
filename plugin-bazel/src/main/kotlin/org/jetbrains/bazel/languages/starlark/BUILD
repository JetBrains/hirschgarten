load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "starlark",
    srcs = glob([
        "**/*.kt",
        "**/*.java",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/bazelrc",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/starlark/repomapping",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
