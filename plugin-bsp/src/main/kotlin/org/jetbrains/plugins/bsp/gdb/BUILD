load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "gdb",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/runnerAction",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/cpp",
    ],
)
