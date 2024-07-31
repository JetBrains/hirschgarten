load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "performance_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2024.1": ["@performance_2024_1//jar"],
        "intellij-ue-2024.1": ["@performance_2024_1//jar"],
        "intellij-2024.2": ["@performance_2024_2//jar"],
        "intellij-ue-2024.2": ["@performance_2024_2//jar"],
        "clion-2024.1": ["@performance_2024_1//jar"],
        "clion-2024.2": ["@performance_2024_2//jar"],
    }),
)

java_library(
    name = "performance_for_tests",
    testonly = 1,
    exports = [":performance_internal"],
)

java_library(
    name = "performance",
    neverlink = 1,
    exports = [":performance_internal"],
)
