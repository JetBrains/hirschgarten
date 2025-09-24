load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkListLiteralTest",
    src = "StarlarkListLiteralTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/psi"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)
