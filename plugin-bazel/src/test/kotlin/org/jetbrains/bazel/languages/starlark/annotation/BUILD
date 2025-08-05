load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkFunctionAnnotatorTest",
    src = "StarlarkFunctionAnnotatorTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/starlark/annotation:FunctionAnnotatorTestData.bzl",
        "//plugin-bazel/src/test/testData/starlark/annotation:UnresolvedNamedArgumentsAnnotatorTestData.bzl",
    ],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkStringAnnotatorTest",
    src = "StarlarkStringAnnotatorTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/annotation:StringAnnotatorTestData.bzl"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkUnusedDeclarationTest",
    src = "StarlarkUnusedDeclarationTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/annotation:UnusedDeclarationTestData.bzl"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "BazelGlobalFunctionAnnotatorTest",
    src = "BazelGlobalFunctionAnnotatorTest.kt",
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkSrcsAnnotatorTest",
    src = "StarlarkSrcsAnnotatorTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/starlark/annotation:EmptyGlobAnnotatorTestData.bzl",
        "//plugin-bazel/src/test/testData/starlark/annotation:MultipleUnresolvedSrcsAnnotatorTestData.bzl",
        "//plugin-bazel/src/test/testData/starlark/annotation:SomeUnresolvedKtSrcsAnnotatorTestData.bzl",
        "//plugin-bazel/src/test/testData/starlark/annotation:SomeUnresolvedSrcsAnnotatorTestData.bzl",
        "//plugin-bazel/src/test/testData/starlark/annotation:UnresolvedGlobAnnotatorTestData.bzl",
        "//plugin-bazel/src/test/testData/starlark/annotation:UnresolvedSrcsAnnotatorTestData.bzl",
    ],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
