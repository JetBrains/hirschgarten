load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "coroutineDebug",
    src = "CoroutineDebugTest.kt",
    ide_ids = [IDE_ID.IC],
    deps = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config"],
)
