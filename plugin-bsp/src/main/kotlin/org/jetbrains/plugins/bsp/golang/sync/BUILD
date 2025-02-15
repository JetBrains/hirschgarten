load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sync",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters/transformers",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/projectStructure/workspaceModel",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/go",
    ],
)
