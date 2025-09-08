load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")
load("//rules/testing:commons.bzl", "kt_test")
load("//rules/testing:intellij.bzl", "INTELLIJ_DEPS")
load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "TargetTagsResolverTest",
    srcs = ["TargetTagsResolverTest.kt"],
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains.bazel.sync.workspace.mapper.normal",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)

intellij_integration_test_suite(
    name = "ResolverSanityTest",
    srcs = ["ResolverSanityTest.kt"],
    data = ["//plugin-bazel/src/test/testData/sync/sanity"],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains.bazel.sync.workspace.mapper.normal",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
