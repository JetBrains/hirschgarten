load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sync",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/jpsCompilation/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel/impl/updaters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//rules_intellij/intellij_platform_sdk:kotlin",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
