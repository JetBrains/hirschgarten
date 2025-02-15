load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "startup",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets/fileTargets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
