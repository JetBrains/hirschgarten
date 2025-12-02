"""
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:commons.bzl", "INTELLIJ_DEPS", "INTELLIJ_RUNTIME_DEPS", "JUNIT_DEPS", "KOTEST_DEPS", "kt_test_suite")
load("//rules/testing:ide_starter.bzl", "kt_integration_test_suite")

kt_integration_test_suite(
    name = "integration-tests",
    srcs = glob(["test/**/*.kt"]),
    resources = [
        "//plugin-bazel:plugin-bazel_zip",
    ],
    tags = [
        "ide-starter-test",
        "manual",
    ],
    test_suffixes_excludes = ["BaseProjectTest.kt"],
    runtime_deps = INTELLIJ_RUNTIME_DEPS,
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//integrationTests/compat/hirschgarten",
        "//plugin-bazel",
        "//pluginTests:test_fixtures",
    ] + INTELLIJ_DEPS,
)
"""