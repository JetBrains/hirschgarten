load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")
load("@rules_java//java:java_import.bzl", "java_import")

java_import(
    name = "compiler_plugin_jar",
    jars = ["compiler_plugin.jar"],
)

kt_compiler_plugin(
    name = "compiler_plugin",
    id = "com.example.plugin",
    deps = [":compiler_plugin_jar"],
)

kt_jvm_library(
    name = "lib",
    srcs = ["Lib.kt"],
    plugins = [":compiler_plugin"],
)
