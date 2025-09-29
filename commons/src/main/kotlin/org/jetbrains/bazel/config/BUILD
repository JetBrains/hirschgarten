load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "config",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacemodel/entities",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
