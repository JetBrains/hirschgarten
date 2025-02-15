load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/flow/open/wizard",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/chunking",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/client",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console/ids",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
