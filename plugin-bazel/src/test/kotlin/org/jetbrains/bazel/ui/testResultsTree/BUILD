load("//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:tests.bzl", "ide_starter_test")

ide_starter_test(
    name = "testResultsTree",
    size = "large",
    src = "TestTargetActionResultsTreeTest.kt",
    classname = "org.jetbrains.bazel.ui.testResultsTree.TestTargetActionResultsTreeTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
