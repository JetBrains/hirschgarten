load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BepDiagnosticsTest",
    src = "BepDiagnosticsTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bep",
    ],
)

kt_junit5_test(
    name = "TestXmlParserTest",
    size = "small",
    src = "TestXmlParserTest.kt",
    deps = [
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bep",
    ],
)
