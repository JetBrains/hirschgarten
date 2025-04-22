load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "run",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/jpsCompilation/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/runnerAction",
        "//protocol",
        "//rules_intellij/intellij_platform_sdk:java",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
