load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelGlobalFunctionDocumentationTargetTest",
    src = "BazelGlobalFunctionDocumentationTargetTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "BazelNativeRuleArgumentDocumentationTargetTest",
    src = "BazelNativeRuleArgumentDocumentationTargetTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
