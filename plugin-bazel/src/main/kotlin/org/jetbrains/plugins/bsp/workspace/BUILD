load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspace",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bazel/workspacemodel/src:workspacemodel",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
