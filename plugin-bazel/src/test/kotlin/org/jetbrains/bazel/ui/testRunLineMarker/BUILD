load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "testRunLineMarker",
    src = "RunLineMarkerTest.kt",
    classname = "org.jetbrains.bazel.ui.testRunLineMarker.RunLineMarkerTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
