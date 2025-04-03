load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "ProjectViewLexerTest",
    src = "ProjectViewLexerTest.kt",
    deps = [
        "//plugin-bazel/src:plugin-bazel-lib",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)
