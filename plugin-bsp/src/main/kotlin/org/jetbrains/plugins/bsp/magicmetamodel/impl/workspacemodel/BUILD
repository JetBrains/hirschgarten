load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacemodel",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bsp:__subpackages__"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
