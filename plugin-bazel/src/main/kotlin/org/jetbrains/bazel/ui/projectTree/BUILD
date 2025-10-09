load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "projectTree",
    srcs = glob([
        "*.kt",
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
    ],
)
