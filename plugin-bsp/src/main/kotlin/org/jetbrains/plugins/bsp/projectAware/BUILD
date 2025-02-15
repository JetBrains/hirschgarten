load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "projectAware",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/buildTask",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
