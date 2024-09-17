load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelGlobalFunctionCompletionContributorTest",
    src = "BazelGlobalFunctionCompletionContributorTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = ["//plugin-bazel:intellij-bazel"],
)

kt_intellij_junit4_test(
    name = "StarlarkParamsCompletionContributorTest",
    src = "StarlarkParamsCompletionContributorTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = ["//plugin-bazel:intellij-bazel"],
)

kt_intellij_junit4_test(
    name = "StarlarkLoadedSymbolsCompletionTest",
    src = "StarlarkLoadedSymbolsCompletionTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/completion"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkArgumentCompletionTest",
    src = "StarlarkArgumentCompletionTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/completion"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
