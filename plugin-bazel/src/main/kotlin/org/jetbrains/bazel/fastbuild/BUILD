load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "fastbuild",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow/sync/bazelPaths",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/tasks",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/android",
        "//rules_intellij/third_party/code_with_me",
        "//rules_intellij/third_party/devkit",
        "//rules_intellij/third_party/go",
        "//rules_intellij/third_party/performance",
        "//rules_intellij/third_party/python",
        "//sdkcompat",
    ],
)
