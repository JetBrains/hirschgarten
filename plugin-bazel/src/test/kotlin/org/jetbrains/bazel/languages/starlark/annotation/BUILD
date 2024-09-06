load("@//plugin-bazel/rules/kotlin:junit4.bzl", "junit4_kt_test")

junit4_kt_test(
    name = "StarlarkFunctionAnnotatorTest",
    srcs = ["StarlarkFunctionAnnotatorTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/annotation:FunctionAnnotatorTestData.bzl"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)

junit4_kt_test(
    name = "StarlarkStringAnnotatorTest",
    srcs = ["StarlarkStringAnnotatorTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/annotation:StringAnnotatorTestData.bzl"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)
