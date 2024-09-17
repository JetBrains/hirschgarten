load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BazelReleaseTest",
    size = "small",
    src = "BazelReleaseTest.kt",
    resources = ["//server/bazelrunner/src/test/resources:workspace"],
    deps = [
        "//server/bazelrunner",
    ],
)

kt_junit5_test(
    name = "BazelRunnerBuilderTest",
    size = "small",
    src = "BazelRunnerBuilderTest.kt",
    deps = [
        "//server/bazelrunner",
    ],
)
