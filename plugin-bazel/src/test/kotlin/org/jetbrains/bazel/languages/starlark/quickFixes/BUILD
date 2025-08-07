load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkGlobAllowEmptyQuickFixTest",
    src = "StarlarkGlobAllowEmptyQuickFixTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/starlark/quickFixes:StarlarkEmptyGlobAllowEmptyQuickFixTestData",
        "//plugin-bazel/src/test/testData/starlark/quickFixes:StarlarkEmptyGlobNotAllowedAllowEmptyQuickFixTestData",
        "//plugin-bazel/src/test/testData/starlark/quickFixes:StarlarkUnresolvedGlobAllowEmptyQuickFixTestData",
    ],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
