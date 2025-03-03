load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelqueryParsingTest",
    src = "BazelqueryParsingTest.kt",
    data = ["//plugin-bazel/src/test/testData/bazelquery/parser"],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelquery/fixtures",
    ],
)
