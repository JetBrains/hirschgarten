#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
# Description:
#
# Common test utilities for IntelliJ plugins.

load(
    "//:build-visibility.bzl",
    "INTELLIJ_PLUGINS_VISIBILITY",
)
load(
    "//intellij_platform_sdk:build_defs.bzl",
    "select_for_ide",
    "select_for_plugin_api",
)

package(default_visibility = INTELLIJ_PLUGINS_VISIBILITY)

licenses(["notice"])

java_library(
    name = "lib",
    testonly = 1,
    srcs = glob(["src/**/*.java"]),
    exports = ["//testing/src/com/google/idea/testing/runfiles"],
    deps = [
        "//intellij_platform_sdk:plugin_api_for_tests",
        "@rules_intellij_maven//:org_junit_jupiter_junit_jupiter",
        "@rules_intellij_maven//:org_junit_jupiter_junit_jupiter_api",
        "@rules_intellij_maven//:org_junit_platform_junit_platform_console",
        "@rules_intellij_maven//:org_junit_platform_junit_platform_engine",
        "@rules_intellij_maven//:org_junit_platform_junit_platform_launcher",
        "@rules_intellij_maven//:org_junit_platform_junit_platform_suite_api",
        "@rules_intellij_maven//:org_junit_platform_junit_platform_suite_engine",
        "@rules_intellij_maven//:org_junit_vintage_junit_vintage_engine",
    ],
)

java_library(
    name = "runfiles",
    testonly = 1,
    exports = ["//testing/src/com/google/idea/testing/runfiles"],
)
