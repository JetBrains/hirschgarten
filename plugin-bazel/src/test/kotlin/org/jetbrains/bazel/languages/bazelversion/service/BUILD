load("//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BazeliskrcParserTest",
    src = "BazeliskrcParserTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src:plugin-bazel-lib",
    ],
)

kt_junit5_test(
    name = "BazelVersionWorkspaceResolverTest",
    src = "BazelVersionWorkspaceResolverTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/bazelversion/version_resolve_bazelisk",
        "//plugin-bazel/src/test/testData/bazelversion/version_resolve_bazelversion",
    ],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
