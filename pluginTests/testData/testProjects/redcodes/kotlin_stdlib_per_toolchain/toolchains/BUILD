load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

# Two Kotlin toolchains with different stdlib versions, selected by `//settings:mode`

# the stdlib bundled with rules_kotlin
define_kt_toolchain(
    name = "kt_a",
    target_settings = ["//settings:mode_a"],
)

java_import(
    name = "kotlin_stdlib_1_9_25",
    jars = ["@kotlin_stdlib_1_9_25//file"],
)

# An older Kotlin stdlib version instead of the one bundled with rules_kotlin.
# `jvm_runtime` is merged into the toolchain stdlibs, so it must not keep the bundled default
define_kt_toolchain(
    name = "kt_b",
    jvm_runtime = [":kotlin_stdlib_1_9_25"],
    jvm_stdlibs = [
        "@rules_kotlin//kotlin/compiler:annotations",
        ":kotlin_stdlib_1_9_25",
    ],
    target_settings = ["//settings:mode_b"],
)
