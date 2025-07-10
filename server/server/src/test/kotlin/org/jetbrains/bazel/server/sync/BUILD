load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "TargetTagsResolverTest",
    srcs = ["TargetTagsResolverTest.kt"],
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains.bazel.server.sync",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
