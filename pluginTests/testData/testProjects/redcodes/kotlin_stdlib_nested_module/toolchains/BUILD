load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

java_import(
    name = "kotlin_stdlib_1_9_25",
    jars = ["@kotlin_stdlib_1_9_25//file"],
)

# An older Kotlin stdlib version instead of the one bundled with rules_kotlin.
# `jvm_runtime` is merged into the toolchain stdlibs, so it must not keep the bundled default
define_kt_toolchain(
    name = "kt",
    jvm_runtime = [":kotlin_stdlib_1_9_25"],
    jvm_stdlibs = [
        "@rules_kotlin//kotlin/compiler:annotations",
        ":kotlin_stdlib_1_9_25",
    ],
)
