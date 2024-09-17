load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcCommandCompletionTest",
    src = "BazelrcCommandCompletionTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelrc/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "BazelrcConfigCompletionTest",
    src = "BazelrcConfigCompletionTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelrc/fixtures",
    ],
)
