load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "hotswap",
    size = "large",
    src = "HotSwapTest.kt",
    classname = "org.jetbrains.bazel.hotswap.HotSwapTest",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
