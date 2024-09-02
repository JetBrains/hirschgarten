load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "taskEvents",
    srcs = glob([
        "**/*.java",
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//protocol",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)
