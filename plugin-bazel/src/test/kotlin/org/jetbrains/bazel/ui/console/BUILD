load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelBuildTargetConsoleFilterTest",
    src = "BazelBuildTargetConsoleFilterTest.kt",
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/starlark/repomapping"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
    ],
)
