load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BazelReleaseTest",
    size = "small",
    src = "BazelReleaseTest.kt",
    resources = ["//plugin-bazel/bazelrunner/src/test/resources:workspace"],
    deps = [
        "//plugin-bazel/bazelrunner",
    ],
)

kt_junit5_test(
    name = "BazelRunnerBuilderTest",
    size = "small",
    src = "BazelRunnerBuilderTest.kt",
    deps = [
        "//plugin-bazel/bazelrunner",
    ],
)

kt_junit5_test(
    name = "ModuleResolverTest",
    size = "small",
    src = "ModuleResolverTest.kt",
    deps = [
        "//plugin-bazel/bazelrunner",
    ],
)
