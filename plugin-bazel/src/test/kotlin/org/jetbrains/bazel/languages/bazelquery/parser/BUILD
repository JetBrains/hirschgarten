load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelQueryParsingTest",
    src = "BazelQueryParsingTest.kt",
    data = ["//plugin-bazel/src/test/testData/bazelquery/parser"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelquery/fixtures",
    ],
)
