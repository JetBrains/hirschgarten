load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelBuildTargetConsoleFilterTest",
    src = "BazelBuildTargetConsoleFilterTest.kt",
    associates = ["//plugin-bazel/src:intellij-bazel"],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src:test_fixtures",
    ],
)
