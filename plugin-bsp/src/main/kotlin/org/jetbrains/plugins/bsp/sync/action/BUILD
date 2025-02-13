load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "action",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/jps-compilation/src:jps-compilation",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/action",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
