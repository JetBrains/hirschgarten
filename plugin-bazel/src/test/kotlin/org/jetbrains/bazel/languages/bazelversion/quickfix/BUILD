load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelVersionQuickFixTest",
    src = "BazelVersionQuickFixTest.kt",
    data = ["//plugin-bazel/src/test/testData/bazelversion/quickfix"],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
