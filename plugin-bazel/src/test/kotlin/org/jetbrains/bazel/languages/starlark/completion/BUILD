load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelGlobalFunctionCompletionContributorTest",
    src = "BazelGlobalFunctionCompletionContributorTest.kt",
    deps = ["//plugin-bazel"],
)

kt_intellij_junit4_test(
    name = "BazelNativeRulesCompletionContributorTest",
    src = "BazelNativeRulesCompletionContributorTest.kt",
    deps = ["//plugin-bazel"],
)

kt_intellij_junit4_test(
    name = "BazelNativeRulesArgumentCompletionContributorTest",
    src = "BazelNativeRulesArgumentCompletionContributorTest.kt",
    deps = ["//plugin-bazel"],
)

kt_intellij_junit4_test(
    name = "StarlarkParamsCompletionContributorTest",
    src = "StarlarkParamsCompletionContributorTest.kt",
    deps = [
        "//plugin-bazel",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkLoadedSymbolsCompletionTest",
    src = "StarlarkLoadedSymbolsCompletionTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/completion"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkArgumentCompletionTest",
    src = "StarlarkArgumentCompletionTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/completion"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
