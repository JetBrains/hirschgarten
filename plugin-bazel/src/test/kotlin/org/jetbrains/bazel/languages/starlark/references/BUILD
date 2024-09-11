load("@//plugin-bazel/rules/kotlin:junit4.bzl", "junit4_kt_test")

junit4_kt_test(
    name = "StarlarkArgumentReferenceTest",
    srcs = ["StarlarkArgumentReferenceTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/references"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)
