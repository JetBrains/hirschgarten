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

kt_junit5_test(
    name = "ModuleResolverTest",
    size = "small",
    src = "ModuleResolverTest.kt",
    deps = [
        "//server/bazelrunner",
    ],
)

kt_junit5_test(
    name = "OutputProcessorTest",
    size = "small",
    src = "OutputProcessorTest.kt",
    deps = [
        "//bsp-testkit2/bsp-testkit/client",
        "//server/bazelrunner",
        #"//rules_intellij/intellij_platform_sdk:plugin_api_for_tests"
    ],
)
