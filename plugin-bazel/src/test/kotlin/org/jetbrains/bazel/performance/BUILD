load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "performance",
    size = "large",
    src = "PerformanceTest.kt",
    classname = "org.jetbrains.bazel.performance.PerformanceTest",
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
