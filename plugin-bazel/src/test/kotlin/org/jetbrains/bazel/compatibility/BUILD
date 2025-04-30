load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "DisabledKotlinPluginTest",
    src = "DisabledKotlinPluginTest.kt",
    classname = "org.jetbrains.bazel.compatibility.DisabledKotlinPluginTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)

ide_starter_test(
    name = "PyCharmTest",
    src = "PyCharmTest.kt",
    classname = "org.jetbrains.bazel.compatibility.PyCharmTest",
    ide_ids = [IDE_ID.PY],
    deps = [
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
