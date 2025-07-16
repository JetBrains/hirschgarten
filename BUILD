load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_kotlinc_options")

package(default_visibility = ["//visibility:public"])

kt_kotlinc_options(
    name = "kotlinc_options",
    include_stdlibs = "none",
    jvm_target = "17",
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "2.0",
    jvm_target = "17",
    kotlinc_options = ":kotlinc_options",
    language_version = "2.0",
)

alias(
    name = "format",
    actual = "//tools/format",
)

npm_link_all_packages(name = "node_modules")
