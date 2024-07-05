load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacemodel",
    srcs = glob([
        "main/gen/**/*.kt",
        "main/kotlin/**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@maven//:com_google_code_gson_gson",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)