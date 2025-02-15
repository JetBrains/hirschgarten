load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "updaters",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters/transformers"],
    visibility = ["//plugin-bsp:__subpackages__"],
    exports = ["//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters/transformers"],
    deps = [
        "//plugin-bsp/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/action",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "//sdkcompat",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
