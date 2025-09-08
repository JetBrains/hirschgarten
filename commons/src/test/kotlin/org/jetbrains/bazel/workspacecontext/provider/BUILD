load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "DefaultWorkspaceContextProviderTest",
    srcs = ["DefaultWorkspaceContextProviderTest.kt"],
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
    test_package_root = "org.jetbrains.bazel.workspacecontext",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
    ],
)