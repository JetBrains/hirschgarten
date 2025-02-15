load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "startup",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bsp:__subpackages__"],
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets/fileTargets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
