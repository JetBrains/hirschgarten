load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "hotswap",
    src = "HotSwapTest.kt",
    classname = "org.jetbrains.bazel.hotswap.HotSwapTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
    ],
)
