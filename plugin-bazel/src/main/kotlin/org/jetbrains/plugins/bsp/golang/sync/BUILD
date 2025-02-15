load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sync",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters/transformers",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/projectStructure/workspaceModel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bazel/workspacemodel/src:workspacemodel",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/go",
    ],
)
