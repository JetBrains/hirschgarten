load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "console",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/action",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/assets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console/ids",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
