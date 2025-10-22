load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "updaters",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/jpsCompilation/utils",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow/sync/bazelPaths",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/scala",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
        "@maven//:com_google_code_gson_gson",
    ],
)
