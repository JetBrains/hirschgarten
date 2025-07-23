load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")
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
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
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

kt_intellij_junit4_test(
    name = "OutputProcessorTest",
    size = "small",
    src = "outputs/OutputProcessorTest.kt",
    test_class = "org.jetbrains.bazel.bazelrunner.outputs.OutputProcessorTest",
    deps = [
        "//bsp-testkit2/bsp-testkit/client",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/outputs",
    ],
)
