load("@//plugin-bazel/rules/kotlin:junit4.bzl", "junit4_kt_test")

junit4_kt_test(
    name = "StarlarkFunctionFindUsagesTest",
    srcs = ["StarlarkFunctionFindUsagesTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/findusages"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)
