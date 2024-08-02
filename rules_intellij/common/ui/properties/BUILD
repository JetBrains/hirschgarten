#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#

load(
    "//:build-visibility.bzl",
    "COMMON_PLUGINS_VISIBILITY",
    "DEFAULT_TEST_VISIBILITY",
)
load(
    "//testing:test_defs.bzl",
    "intellij_unit_test_suite",
)

licenses(["notice"])

java_library(
    name = "properties",
    srcs = glob(["src/**/*.java"]),
    visibility = COMMON_PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
    ],
)

intellij_unit_test_suite(
    name = "unit_tests",
    srcs = glob(["tests/unittests/**/*.java"]),
    test_package_root = "com.google.idea.common.ui.properties",
    visibility = DEFAULT_TEST_VISIBILITY,
    deps = [
        ":properties",
        "//intellij_platform_sdk:test_libs",
        "@junit//jar",
    ],
)
