load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcLexerTest",
    src = "BazelrcLexerTest.kt",
    deps = [
        "//plugin-bazel/src:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "BazelrcQuotingLexerTest",
    src = "BazelrcQuotingLexerTest.kt",
    deps = [
        "//plugin-bazel/src:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)
