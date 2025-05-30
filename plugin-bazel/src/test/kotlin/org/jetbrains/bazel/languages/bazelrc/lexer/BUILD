load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcLexerTest",
    src = "BazelrcLexerTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src:plugin-bazel-lib",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "BazelrcQuotingLexerTest",
    src = "BazelrcQuotingLexerTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src:plugin-bazel-lib",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)
