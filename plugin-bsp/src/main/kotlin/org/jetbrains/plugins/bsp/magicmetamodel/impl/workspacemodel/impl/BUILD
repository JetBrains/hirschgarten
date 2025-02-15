load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "impl",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters"],
    visibility = ["//plugin-bsp:__subpackages__"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
