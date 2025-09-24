load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "ProjectViewLexerTest",
    src = "ProjectViewLexerTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/projectview",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/fixtures",
    ],
)
