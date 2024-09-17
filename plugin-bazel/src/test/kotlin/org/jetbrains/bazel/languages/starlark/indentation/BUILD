load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkEnterBetweenBracesTest",
    src = "StarlarkEnterBetweenBracesTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = ["//plugin-bazel:intellij-bazel"],
)
