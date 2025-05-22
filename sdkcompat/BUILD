# Description: Indirections for SDK changes to the underlying platform library.

load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

licenses(["notice"])

kt_jvm_library(
    name = "sdkcompat",
    visibility = ["//visibility:public"],
    exports = select_for_plugin_api({
        "intellij-2024.3": ["//sdkcompat/v243"],
        "intellij-2025.1": ["//sdkcompat/v251"],
        "intellij-2025.2": ["//sdkcompat/v252"],
    }),
)
