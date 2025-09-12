load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "performance",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/performance",
    ],
)
