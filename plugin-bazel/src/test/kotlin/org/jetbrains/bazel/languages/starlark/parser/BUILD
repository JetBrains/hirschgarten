load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "StarlarkParsingBasicRulesTest",
    src = "StarlarkParsingBasicRulesTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/basicRules"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingComplexTest",
    src = "StarlarkParsingComplexTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/complex"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingExpressionTest",
    src = "StarlarkParsingExpressionTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/expression"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingStatementTest",
    src = "StarlarkParsingStatementTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/statement"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "StarlarkParsingStmtTest",
    src = "StarlarkParsingStmtTest.kt",
    data = ["//plugin-bazel/src/test/testData/starlark/parser/stmt"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/fixtures",
    ],
)
