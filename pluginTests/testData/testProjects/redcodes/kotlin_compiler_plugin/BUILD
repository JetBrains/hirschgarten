load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")

kt_compiler_plugin(
    name = "serialization_plugin",
    id = "org.jetbrains.kotlin.serialization",
    deps = ["@maven//:org_jetbrains_kotlin_kotlin_serialization_compiler_plugin_embeddable"],
)

kt_jvm_library(
    name = "lib",
    srcs = ["Lib.kt"],
    plugins = [":serialization_plugin"],
    deps = ["@maven//:org_jetbrains_kotlinx_kotlinx_serialization_core_jvm"],
)
