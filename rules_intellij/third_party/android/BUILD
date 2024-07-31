load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "android_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2024.1": ["@android_2024_1//:android"],
        "intellij-ue-2024.1": ["@android_2024_1//:android"],
        "intellij-2024.2": ["@android_2024_2//:android"],
        "intellij-ue-2024.2": ["@android_2024_2//:android"],
        "default": [],
    }),
)

java_library(
    name = "android_for_tests",
    testonly = 1,
    exports = [":android_internal"],
)

java_library(
    name = "android",
    neverlink = 1,
    exports = [":android_internal"],
)
