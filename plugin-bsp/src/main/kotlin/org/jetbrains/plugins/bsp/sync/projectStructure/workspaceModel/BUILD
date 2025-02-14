load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspaceModel",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/performance",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/projectStructure",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
