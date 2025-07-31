load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "performance",
    src = "PerformanceTest.kt",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/performance",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
    ],
)
