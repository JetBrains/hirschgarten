#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
# Description:
#
# Common test utilities for IntelliJ plugins.

load(
    "//rules_intellij/intellij_platform_sdk:build_defs.bzl",
    "select_for_ide",
    "select_for_plugin_api",
)

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

exports_files(["test_defs_kt.bzl"])

java_library(
    name = "lib",
    testonly = 1,
    srcs = glob(["src/**/*.java"]),
    exports = ["//rules_intellij/testing/src/com/google/idea/testing/runfiles"],
    deps = [
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@maven//:org_junit_platform_junit_platform_console",
        "@maven//:org_junit_platform_junit_platform_engine",
        "@maven//:org_junit_platform_junit_platform_launcher",
        "@maven//:org_junit_platform_junit_platform_suite_api",
        "@maven//:org_junit_platform_junit_platform_suite_engine",
        "@maven//:org_junit_vintage_junit_vintage_engine",
        "@maven//:junit_junit",
    ],
)

java_library(
    name = "runfiles",
    testonly = 1,
    exports = ["//rules_intellij/testing/src/com/google/idea/testing/runfiles"],
)
