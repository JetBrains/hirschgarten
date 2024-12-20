load("//:versions.bzl", "IDE_STARTER_TEST_VERSION")
load("//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:tests.bzl", "ide_starter_test")
load("//plugin-bsp/rules/kotlin:junit5.bzl", "kt_test")

ide_starter_test(
    name = "hotswap",
    size = "large",
    src = "HotSwapTest.kt",
    classname = "org.jetbrains.bazel.hotswap.HotSwapTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
