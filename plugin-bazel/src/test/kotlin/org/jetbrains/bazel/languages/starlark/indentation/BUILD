load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkEnterBetweenBracesTest",
    src = "StarlarkEnterBetweenBracesTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = ["//plugin-bazel:intellij-bazel"],
)

kt_intellij_junit4_test(
    name = "StarlarkLineIndentProviderTest",
    src = "StarlarkLineIndentProviderTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/indentation"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
