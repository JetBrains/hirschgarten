load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "android",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/startup",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
        "//sdkcompat",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
    ],
)
