load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "starter",
    src = "BazelProjectOpenProcessorStarterTest.kt",
    classname = "org.jetbrains.bazel.flow.starter.BazelProjectOpenProcessorStarterTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
