load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "starter",
    size = "large",
    src = "BazelProjectOpenProcessorStarterTest.kt",
    classname = "org.jetbrains.bazel.flow.starter.BazelProjectOpenProcessorStarterTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
