load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "widgets",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/assets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/runnerAction",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/services",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/action",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
