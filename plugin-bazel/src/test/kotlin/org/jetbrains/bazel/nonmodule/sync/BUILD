load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "NonModuleTargetsTest",
    src = "NonModuleTargetsTest.kt",
    classname = "org.jetbrains.bazel.nonmodule.sync.NonModuleTargetsTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
