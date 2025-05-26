load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "SampleTest",
    src = "SampleTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/findusages"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)

kt_intellij_junit4_test(
    name = "GlobFindUsagesTest",
    src = "GlobFindUsagesTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/findusages"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkFunctionFindUsagesTest",
    src = "StarlarkFunctionFindUsagesTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/findusages"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
