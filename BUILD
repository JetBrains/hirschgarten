load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_kotlinc_options")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

kt_kotlinc_options(
    name = "kotlinc_options",
    include_stdlibs = "none",
    jvm_target = select_for_plugin_api({
        "intellij-2025.2": "17",
        "intellij-2025.3": "21",
    }),
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = select_for_plugin_api({
        "intellij-2025.2": "2.0",
        "intellij-2025.3": "2.3",
    }),
    experimental_multiplex_workers = True,
    jvm_target = select_for_plugin_api({
        "intellij-2025.2": "17",
        "intellij-2025.3": "21",
    }),
    kotlinc_options = ":kotlinc_options",
    language_version = select_for_plugin_api({
        "intellij-2025.2": "2.0",
        "intellij-2025.3": "2.3",
    }),
)

alias(
    name = "format",
    actual = "//tools/format",
)

npm_link_all_packages(name = "node_modules")
