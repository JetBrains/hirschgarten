load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")

#
# Description: Blaze plugin for various IntelliJ products.
#
load(
    "//:build-visibility.bzl",
    "BAZEL_PLUGIN_SUBPACKAGES",
    "DEFAULT_TEST_VISIBILITY",
    "create_plugin_packages_group",
)

licenses(["notice"])

create_plugin_packages_group()

default_java_toolchain(
    name = "custom_java_17_toolchain",
    configuration = dict(),
    java_runtime = "@rules_java//toolchains:remotejdk_17",
    source_version = "17",
    target_version = "17",
)
