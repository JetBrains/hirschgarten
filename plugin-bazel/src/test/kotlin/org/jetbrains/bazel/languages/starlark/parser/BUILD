load("@//plugin-bazel/rules/kotlin:junit4.bzl", "junit4_kt_test")

junit4_kt_test(
    name = "StarlarkParsingBasicRulesTest",
    srcs = ["StarlarkParsingBasicRulesTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/parser/basicRules"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)

junit4_kt_test(
    name = "StarlarkParsingComplexTest",
    srcs = ["StarlarkParsingComplexTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/parser/complex"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)

junit4_kt_test(
    name = "StarlarkParsingExpressionTest",
    srcs = ["StarlarkParsingExpressionTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/parser/expression"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)

junit4_kt_test(
    name = "StarlarkParsingStatementTest",
    srcs = ["StarlarkParsingStatementTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/parser/statement"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)

junit4_kt_test(
    name = "StarlarkParsingStmtTest",
    srcs = ["StarlarkParsingStmtTest.kt"],
    data = ["//plugin-bazel/src/test/testData/starlark/parser/stmt"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures"],
)
