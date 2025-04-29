load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "NonModuleTargetsTest",
    size = "large",
    src = "NonModuleTargetsTest.kt",
    classname = "org.jetbrains.bazel.nonmodule.sync.NonModuleTargetsTest",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
