load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "DisabledKotlinPluginTest",
    src = "DisabledKotlinPluginTest.kt",
    ide_ids = [IDE_ID.IC],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
    ],
)

ide_starter_test(
    name = "PyCharmTest",
    src = "PyCharmTest.kt",
    ide_ids = [IDE_ID.PY],
)
