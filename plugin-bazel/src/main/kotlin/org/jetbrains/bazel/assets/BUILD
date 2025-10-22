load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "assets",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = ["//rules_intellij/intellij_platform_sdk:plugin_api"],
)
