# Description: Indirections for SDK changes to the underlying platform library.

load("@rules_intellij//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

licenses(["notice"])

java_library(
    name = "sdkcompat",
    visibility = ["//visibility:public"],
    exports = select_for_plugin_api({
        "intellij-2024.2": ["//sdkcompat/v242"],
        "intellij-2024.3": ["//sdkcompat/v243"],
    }),
)
