load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "targetKind",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//commons/src/main/kotlin/org/jetbrains/bsp/info",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/projectStructure",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/scope",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
