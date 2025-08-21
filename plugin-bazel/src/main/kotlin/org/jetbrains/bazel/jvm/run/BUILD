load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "run",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/hotswap",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/taskEvents",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/notifications",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
    ],
)
