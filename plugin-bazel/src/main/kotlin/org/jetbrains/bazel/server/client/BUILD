load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "client",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/logger",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/taskEvents",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/ids",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
