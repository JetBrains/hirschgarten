load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkParsingBasicRulesTest",
    src = "StarlarkParsingBasicRulesTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/basicRules"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingComplexTest",
    src = "StarlarkParsingComplexTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/complex"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingExpressionTest",
    src = "StarlarkParsingExpressionTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/expression"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingStatementTest",
    src = "StarlarkParsingStatementTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/statement"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingStmtTest",
    src = "StarlarkParsingStmtTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/stmt"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
