load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "projectTree",
    srcs = glob([
        "*.kt",
        "**/*.kt",
    ]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/runnerAction",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
