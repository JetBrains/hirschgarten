load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")
load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load(":defs.bzl", "resolver_test")

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

kt_jvm_library(
    name = "resolver_sanity_runner_lib",
    srcs = ["ResolverSanityRunner.kt"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_junit_jupiter_junit_jupiter_engine",
        "@maven//:org_junit_platform_junit_platform_console",
        "@maven//:org_junit_platform_junit_platform_suite_api",
        "@maven//:org_junit_platform_junit_platform_suite_engine",
    ],
)

java_binary(
    name = "resolver_sanity_runner",
    main_class = "org.jetbrains.bazel.sync.workspace.mapper.normal.ResolverSanityRunnerKt",
    visibility = ["//visibility:public"],
    runtime_deps = [":resolver_sanity_runner_lib"],
)

resolver_test(name = "python")

resolver_test(name = "samplerepo")

resolver_test(name = "go")
