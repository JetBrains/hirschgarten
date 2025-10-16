load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "impl",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel/impl/updaters"],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
    ],
)
