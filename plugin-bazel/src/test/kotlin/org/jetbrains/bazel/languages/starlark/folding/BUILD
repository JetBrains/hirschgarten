load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkFoldingTest",
    src = "StarlarkFoldingTest.kt",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = ["//plugin-bazel:intellij-bazel"],
)
