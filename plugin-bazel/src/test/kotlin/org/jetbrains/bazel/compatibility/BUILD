load("//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:tests.bzl", "ide_starter_test")

ide_starter_test(
    name = "DisabledKotlinPluginTest",
    size = "large",
    src = "DisabledKotlinPluginTest.kt",
    classname = "org.jetbrains.bazel.compatibility.DisabledKotlinPluginTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)

ide_starter_test(
    name = "clion",
    size = "large",
    src = "CLionStarterTest.kt",
    classname = "org.jetbrains.bazel.compatibility.CLionStarterTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
