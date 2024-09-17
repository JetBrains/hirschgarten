load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkFunctionAnnotatorTest",
    src = "StarlarkFunctionAnnotatorTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/annotation:FunctionAnnotatorTestData.bzl"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkStringAnnotatorTest",
    src = "StarlarkStringAnnotatorTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/annotation:StringAnnotatorTestData.bzl"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
