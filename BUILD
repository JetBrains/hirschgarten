load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "2.0",
    #     we should turn it on one day
    #     experimental_strict_kotlin_deps = "warn",
    jvm_target = "17",
    language_version = "2.0",
)

alias(
    name = "format",
    actual = "//tools/format",
)

npm_link_all_packages(name = "node_modules")
