# Description: Indirections for SDK changes to the underlying platform library.

load("@rules_java//java:java_library.bzl", "java_library")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

licenses(["notice"])

java_library(
    name = "sdkcompat",
    visibility = ["//visibility:public"],
    exports = select_for_plugin_api({
        "intellij-2025.1": ["//sdkcompat/v251"],
        "intellij-2025.2": ["//sdkcompat/v252"],
    }),
)
