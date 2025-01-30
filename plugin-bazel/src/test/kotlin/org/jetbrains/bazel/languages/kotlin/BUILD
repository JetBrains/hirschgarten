load("//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:tests.bzl", "ide_starter_test")

ide_starter_test(
    name = "kotlin_strict_deps_test",
    size = "large",
    src = "KotlinStrictDepsTest.kt",
    classname = "org.jetbrains.bazel.languages.kotlin.KotlinStrictDepsTest",
    tags = ["manual"],
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
