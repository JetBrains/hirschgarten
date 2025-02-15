load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/taskEvents",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console/ids",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
