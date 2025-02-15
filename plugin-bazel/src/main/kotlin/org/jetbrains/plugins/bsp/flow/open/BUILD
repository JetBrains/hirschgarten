load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "open",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters/transformers"],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//protocol",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
