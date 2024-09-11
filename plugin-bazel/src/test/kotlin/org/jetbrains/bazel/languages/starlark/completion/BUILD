load("@//plugin-bazel/rules/kotlin:junit4.bzl", "junit4_kt_test")

junit4_kt_test(
    name = "BazelGlobalFunctionCompletionContributorTest",
    srcs = ["BazelGlobalFunctionCompletionContributorTest.kt"],
)

junit4_kt_test(
    name = "StarlarkParamsCompletionContributorTest",
    srcs = ["StarlarkParamsCompletionContributorTest.kt"],
)

junit4_kt_test(
    name = "StarlarkLoadedSymbolsCompletionTest",
    srcs = ["StarlarkLoadedSymbolsCompletionTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/completion"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)

junit4_kt_test(
    name = "StarlarkArgumentCompletionTest",
    srcs = ["StarlarkArgumentCompletionTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/completion"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)
