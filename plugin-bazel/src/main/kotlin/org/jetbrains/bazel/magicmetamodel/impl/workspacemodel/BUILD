load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacemodel",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
