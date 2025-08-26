load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

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
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests"
    ],
)

java_binary(
    name = "resolver_sanity_runner",
    main_class = "org.jetbrains.bazel.sync.workspace.mapper.normal.ResolverSanityRunnerKt",
    visibility = ["//visibility:public"],
    runtime_deps = [":resolver_sanity_runner_lib"],
)

# Test data filegroups
filegroup(
    name = "python_testdata",
    srcs = glob(["testData/python/*.textproto"]),
)

filegroup(
    name = "samplerepo_testdata", 
    srcs = glob(["testData/samplerepo/*.textproto"]),
)

# Generate outputs by running the resolver_sanity_runner
genrule(
    name = "python_output",
    srcs = [":python_testdata"],
    outs = ["python_output.txt"],
    cmd = "$(location :resolver_sanity_runner) $(locations :python_testdata) > $@",
    tools = [":resolver_sanity_runner"],
)

genrule(
    name = "samplerepo_output",
    srcs = [":samplerepo_testdata"],
    outs = ["samplerepo_output.txt"], 
    cmd = "$(location :resolver_sanity_runner) $(locations :samplerepo_testdata) > $@",
    tools = [":resolver_sanity_runner"],
)

# Diff tests comparing actual vs expected outputs
diff_test(
    name = "python_diff_test",
    file1 = ":python_output",
    file2 = "testData/python/expected_output.txt",
)

diff_test(
    name = "samplerepo_diff_test",
    file1 = ":samplerepo_output",
    file2 = "testData/samplerepo/expected_output.txt"
)
