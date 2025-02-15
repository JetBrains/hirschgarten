load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "widgets",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/runnerAction",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/services",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/action",
        "//plugin-bazel/workspacemodel/src:workspacemodel",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
