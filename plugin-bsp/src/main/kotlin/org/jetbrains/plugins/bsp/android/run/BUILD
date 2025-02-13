load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "run",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/action",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/tasks",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
    ],
)
