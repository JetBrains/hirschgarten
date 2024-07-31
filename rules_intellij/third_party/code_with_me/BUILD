load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "code_with_me_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2024.2": ["@code_with_me_2024_2//jar"],
        "intellij-ue-2024.2": ["@code_with_me_2024_2//jar"],
        "default": [],
    }),
)

java_library(
    name = "code_with_me",
    neverlink = 1,
    exports = [":code_with_me_internal"],
)

java_library(
    name = "code_with_me_for_tests",
    testonly = 1,
    exports = [":code_with_me_internal"],
)
