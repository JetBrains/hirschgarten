load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "BazelCoverageTest",
    size = "large",
    src = "BazelCoverageTest.kt",
    classname = "org.jetbrains.bazel.run.coverage.BazelCoverageTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
