load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_plugin.bzl", "java_plugin")
load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

java_library(
    name = "java_lib",
    srcs = ["JavaLib.java"],
    plugins = ["//helper:processor"],
    deps = ["//helper:lib"],
)

kt_jvm_library(
    name = "kt_lib",
    srcs = ["KtLib.kt"],
    plugins = ["//helper:processor"],
    deps = ["//helper:lib"],
)
