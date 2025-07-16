load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_kotlinc_options")

kt_kotlinc_options(
    name = "kotlinc_options",
    include_stdlibs = "none",
    jvm_target = "17",
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "2.0",
    experimental_multiplex_workers = True,
    jvm_target = "17",
    kotlinc_options = ":kotlinc_options",
    language_version = "2.0",
)

alias(
    name = "format",
    actual = "//tools/format",
)
