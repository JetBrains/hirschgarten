load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "2.0",
    jvm_target = "17",
    language_version = "2.0",
)

alias(
    name = "format",
    actual = "//tools/format",
)
