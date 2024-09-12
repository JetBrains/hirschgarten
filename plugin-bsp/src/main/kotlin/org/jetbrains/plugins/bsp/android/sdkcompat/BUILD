load("@rules_intellij//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

# v243: it is no longer possible to inject the Project parameter into an implementation of AndroidProjectSystemProvider.
# This has to be a separate target from the main sdkcompat because there have to be two separate implementations
# with the same name for different versions of the IDE - it is not possible to have a single implementation that
# works in both versions.

java_library(
    name = "sdkcompat",
    visibility = ["//visibility:public"],
    exports = select_for_plugin_api({
        "intellij-2024.2": ["//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/android/sdkcompat/v242"],
        "intellij-2024.3": ["//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/android/sdkcompat/v243"],
    }),
)
