load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkArgumentReferenceTest",
    src = "StarlarkArgumentReferenceTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/references"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
