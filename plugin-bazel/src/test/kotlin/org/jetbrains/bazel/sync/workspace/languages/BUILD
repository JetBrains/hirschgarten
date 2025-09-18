load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")
load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "JVMLanguagePluginParserTest",
    size = "small",
    src = "JVMLanguagePluginParserTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
    ],
)

kt_junit5_test(
    name = "LanguagePluginServiceTest",
    size = "small",
    src = "LanguagePluginServiceTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//server/bazelrunner",
    ],
)
