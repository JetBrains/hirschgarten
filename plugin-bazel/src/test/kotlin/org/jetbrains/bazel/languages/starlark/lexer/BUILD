load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkLexerTest",
    src = "StarlarkLexerTest.kt",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)
