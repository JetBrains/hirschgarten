load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkGlobAllowEmptyQuickFixTest",
    src = "StarlarkGlobAllowEmptyQuickFixTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/starlark/quickFixes",
    ],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
