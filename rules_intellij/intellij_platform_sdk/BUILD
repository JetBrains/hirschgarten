load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
# Copyright 2011 Google Inc.  All rights reserved.
#
# Description:
#   Defines a package group that restricts access to the JetBrains
#   plugin apis to known packages that build plugins. Only packages
#   listed here may depend on these libraries.
load(
    ":build_defs.bzl",
    "select_from_plugin_api_directory",
)

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

config_setting(
    name = "intellij-latest",
    values = {
        "define": "ij_product=intellij-latest",
    },
)

config_setting(
    name = "intellij-2025.1",
    values = {"define": "ij_product=intellij-2025.1"},
)

config_setting(
    name = "intellij-2025.2",
    values = {"define": "ij_product=intellij-2025.2"},
)

config_setting(
    name = "intellij-2025.3",
    values = {"define": "ij_product=intellij-2025.3"},
)

# The purpose of this rule is to hide the versioning
# complexity from users of this api.
# There will be additional versions added in the future
java_library(
    name = "plugin_api_internal",
    data = [
        ":application_info_json",
    ],
    visibility = ["//visibility:private"],
    exports = select_from_plugin_api_directory(
        android_studio = [
        ],
        clion = [
        ],
        intellij = [
            ":sdk",
            ":platform_images",
        ],
    ),
)

# The outward facing plugin api
java_library(
    name = "plugin_api",
    neverlink = 1,
    exports = [":plugin_api_internal"],
)

# For tests, we need the IJ API at runtime, so we can't use the neverlink rule.
java_library(
    name = "plugin_api_for_tests",
    data = select_from_plugin_api_directory(
        android_studio = [
            ":kotlinc_version",
        ],
        clion = [],
        # From 2022.1 forward, the version of kotlinc will be obtained
        # by KotlinPluginLayout from plugins/kotlin/kotlinc/build.txt
        # so we need to provide this to our tests
        intellij = [":kotlinc_version"],
    ),
    exports = [":plugin_api_internal"],
)

# IntelliJ Kotlin plugin
java_library(
    name = "kotlin",
    neverlink = 1,
    visibility = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/kotlin:__subpackages__",
        "//sdkcompat:__subpackages__",
    ],
    exports = select_from_plugin_api_directory(
        android_studio = [":kotlin"],
        clion = [],
        intellij = [":kotlin"],
    ),
)

# IntelliJ Kotlin plugin
java_library(
    name = "kotlin_for_tests",
    testonly = 1,
    exports = select_from_plugin_api_directory(
        android_studio = [":kotlin"],
        clion = [],
        intellij = [":kotlin"],
    ),
)

# IntelliJ Java plugin
java_library(
    name = "java",
    neverlink = 1,
    visibility = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow/modify/ideStarter:__pkg__",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/java:__subpackages__",
        "//sdkcompat:__subpackages__",
    ],
    exports = select_from_plugin_api_directory(
        android_studio = [":java"],
        clion = [],
        intellij = [":java"],
    ),
)

# IntelliJ Java plugin
java_library(
    name = "java_for_tests",
    testonly = 1,
    exports = select_from_plugin_api_directory(
        android_studio = [":java"],
        clion = [],
        intellij = [":java"],
    ),
)

# IntelliJ JUnit plugin
java_library(
    name = "junit",
    neverlink = 1,
    visibility = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/junit:__subpackages__"],
    exports = select_from_plugin_api_directory(
        android_studio = [],
        clion = [],
        intellij = [":junit"],
    ),
)

# IntelliJ JUnit plugin
java_library(
    name = "junit_for_tests",
    testonly = 1,
    exports = select_from_plugin_api_directory(
        android_studio = [],
        clion = [],
        intellij = [":junit"],
    ),
)

# Bundled plugins required by integration tests
java_library(
    name = "bundled_plugins",
    testonly = 1,
    runtime_deps = select_from_plugin_api_directory(
        android_studio = [":bundled_plugins"],
        clion = [],
        intellij = [":bundled_plugins"],
    ),
)

filegroup(
    name = "application_info_json",
    srcs = select_from_plugin_api_directory(
        android_studio = [":application_info_json"],
        clion = [],
        intellij = [":application_info_json"],
    ),
)
