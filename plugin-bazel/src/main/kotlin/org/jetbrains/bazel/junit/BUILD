load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "junit",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/runfiles",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/runnerAction",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/settings/bazel",
        "//rules_intellij/intellij_platform_sdk:junit",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
