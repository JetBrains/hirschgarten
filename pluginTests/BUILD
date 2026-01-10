"""
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:commons.bzl", "INTELLIJ_DEPS", "INTELLIJ_RUNTIME_DEPS", "JUNIT_DEPS", "KOTEST_DEPS", "kt_test_suite")

kt_jvm_library(
    name = "test_fixtures",
    testonly = True,
    srcs = glob(["testFixtures/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel",
        "//pluginTests/compat/hirschgarten",
    ] + JUNIT_DEPS + KOTEST_DEPS + INTELLIJ_DEPS,
)

kt_test_suite(
    name = "plugin-tests",
    srcs = glob(["test/**/*.kt"]),
    data = [
        "//pluginTests/testData:test_data",
    ],
    resources = [
        "//plugin-bazel",
        "//plugin-bazel:plugin-bazel_zip",
    ],
    test_suffixes_excludes = ["BaseTest.kt"],
    runtime_deps = INTELLIJ_RUNTIME_DEPS,
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//plugin-bazel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/golang/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel/impl",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel/impl/updaters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/settings",
        "//pluginTests:test_fixtures",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    ] + INTELLIJ_DEPS,
)
"""