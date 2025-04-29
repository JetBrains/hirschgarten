load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "DisabledKotlinPluginTest",
    size = "large",
    src = "DisabledKotlinPluginTest.kt",
    classname = "org.jetbrains.bazel.compatibility.DisabledKotlinPluginTest",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)

ide_starter_test(
    name = "PyCharmTest",
    size = "large",
    src = "PyCharmTest.kt",
    classname = "org.jetbrains.bazel.compatibility.PyCharmTest",
    product_build_number = "251.23774.444",
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
