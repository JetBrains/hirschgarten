load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "startup",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/projectStructure/legacy"],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup/utils",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/go",
    ],
)
