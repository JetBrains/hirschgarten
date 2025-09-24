load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcCommenterTest",
    src = "BazelrcCommenterTest.kt",
    data = ["//plugin-bazel/src/test/testData/bazelrc/commenter"],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
