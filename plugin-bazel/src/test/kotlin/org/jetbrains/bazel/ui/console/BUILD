load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelBuildTargetConsoleFilterTest",
    src = "BazelBuildTargetConsoleFilterTest.kt",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/bazel",
    ],
)
