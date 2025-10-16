load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "gutter",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/gutters",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/python",
    ],
)
