load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcQuickFixTest",
    src = "BazelrcQuickFixTest.kt",
    data = ["//plugin-bazel/src/test/testData/bazelrc/quickfix"],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
