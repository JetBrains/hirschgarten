load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "ImportRunConfigurationsSyncHookTest",
    src = "ImportRunConfigurationsSyncHookTest.kt",
    classname = "org.jetbrains.bazel.run.import.ImportRunConfigurationsSyncHookTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
