load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelQueryCommandCompletionTest",
    src = "BazelQueryCommandCompletionTest.kt",
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelquery/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "BazelQueryOperationCompletionTest",
    src = "BazelQueryOperationCompletionTest.kt",
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelquery/fixtures",
    ],
)
