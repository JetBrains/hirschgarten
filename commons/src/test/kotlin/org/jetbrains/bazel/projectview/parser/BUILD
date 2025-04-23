load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_junit5_test(
    name = "DefaultProjectViewParserTest",
    size = "small",
    src = "DefaultProjectViewParserTest.kt",
    resources = ["//commons/src/test/resources:projectview"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser",
        "//commons/src/test/kotlin/org/jetbrains/bazel/projectview/parser:parser_test_mock",
    ],
)

kt_jvm_library(
    name = "parser_test_mock",
    testonly = 1,
    srcs = ["ProjectViewParserTestMock.kt"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser",
    ],
)
