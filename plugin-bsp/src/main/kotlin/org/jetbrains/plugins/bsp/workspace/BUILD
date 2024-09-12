load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspace",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/impl",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
