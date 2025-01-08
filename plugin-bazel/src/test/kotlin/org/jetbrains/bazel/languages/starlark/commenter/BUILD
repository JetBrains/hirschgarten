load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkCommenterTest",
    src = "StarlarkCommenterTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/commenter"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = ["//plugin-bazel:intellij-bazel"],
)
