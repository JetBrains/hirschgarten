load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_compiler_plugin")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "2.1",
    #     we should turn it on one day
    #     experimental_strict_kotlin_deps = "warn",
    jvm_target = "17",
    language_version = "2.1",
)

kt_compiler_plugin(
    name = "serialization_plugin",
    compile_phase = True,
    id = "org.jetbrains.kotlin.serialization",
    stubs_phase = True,
    deps = [
        "@rules_kotlin//kotlin/compiler:kotlinx-serialization-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "kotlin_serialization",
    srcs = [],
    exported_compiler_plugins = [":serialization_plugin"],
    visibility = ["//visibility:public"],
    exports = [
        #         "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_core_jvm",
        #         "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_json",
    ],
)

alias(
    name = "format",
    actual = "//tools/format",
)
